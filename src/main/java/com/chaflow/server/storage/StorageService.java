package com.chaflow.server.storage;

import javax.sql.DataSource;
import java.nio.file.*;
import java.security.MessageDigest;
import java.sql.*;
import java.util.*;

/**
 * Rails Active Storage-inspired disk service: PostgreSQL stores blob/attachment
 * metadata while the original bytes live under storage/<key-prefix>/<key>.
 */
public final class StorageService {
    public static final long MAX_FILE_SIZE = 12L * 1024 * 1024;
    private final DataSource dataSource;
    private final Path root;

    public StorageService(DataSource dataSource) {
        this(dataSource, Paths.get(System.getenv().getOrDefault("CHAFLOW_STORAGE_ROOT", "storage")));
    }

    public StorageService(DataSource dataSource, Path root) {
        this.dataSource = Objects.requireNonNull(dataSource);
        this.root = Objects.requireNonNull(root).toAbsolutePath().normalize();
    }

    public Map<String,Object> upload(long userId, long conversationId, String filename,
                                     String contentType, byte[] bytes) throws Exception {
        if (userId <= 0 || conversationId <= 0) throw new IllegalArgumentException("Invalid conversation.");
        if (filename == null || filename.isBlank() || filename.length() > 255)
            throw new IllegalArgumentException("Invalid filename.");
        if (bytes == null || bytes.length == 0 || bytes.length > MAX_FILE_SIZE)
            throw new IllegalArgumentException("Files must be between 1 byte and 12 MB.");
        String mime = contentType == null ? "application/octet-stream" : contentType.toLowerCase(Locale.ROOT);
        if (mime.isBlank() || mime.length() > 255 || mime.indexOf('\r') >= 0 || mime.indexOf('\n') >= 0)
            throw new IllegalArgumentException("Invalid file content type.");
        String safeName = Paths.get(filename.replace('\\', '/')).getFileName().toString();
        if (safeName.isBlank() || safeName.contains("..")) throw new IllegalArgumentException("Invalid filename.");

        String key = UUID.randomUUID().toString().replace("-", "");
        Path target = pathFor(key);
        Files.createDirectories(target.getParent());
        Files.write(target, bytes, StandardOpenOption.CREATE_NEW);
        boolean committed = false;
        try (Connection c = dataSource.getConnection()) {
            c.setAutoCommit(false);
            try {
                requireMember(c, userId, conversationId);
                long blobId;
                try (PreparedStatement p = c.prepareStatement("""
                    INSERT INTO chatflow.active_storage_blobs(key,filename,content_type,byte_size,checksum)
                    VALUES(?,?,?,?,?) RETURNING id
                    """)) {
                    p.setString(1,key); p.setString(2,safeName); p.setString(3,mime);
                    p.setLong(4,bytes.length); p.setString(5,sha256(bytes));
                    try (ResultSet r=p.executeQuery()) { r.next(); blobId=r.getLong(1); }
                }
                long messageId;
                String sentAt;
                String messageType = mime.startsWith("image/") ? "IMAGE" : "FICHIER";
                try (PreparedStatement p = c.prepareStatement("""
                    INSERT INTO chatflow.message(id_conversation,id_expediteur,contenu,type_message)
                    VALUES(?,?,?,?) RETURNING id_message,envoye_le
                    """)) {
                    p.setLong(1,conversationId); p.setLong(2,userId); p.setString(3,safeName); p.setString(4,messageType);
                    try (ResultSet r=p.executeQuery()) { r.next(); messageId=r.getLong(1); sentAt=r.getTimestamp(2).toInstant().toString(); }
                }
                try (PreparedStatement p = c.prepareStatement("""
                    INSERT INTO chatflow.active_storage_attachments(name,record_type,record_id,blob_id)
                    VALUES('file','Message',?,?)
                    """)) { p.setLong(1,messageId); p.setLong(2,blobId); p.executeUpdate(); }
                try (PreparedStatement p = c.prepareStatement("""
                    INSERT INTO chatflow.message_destinataire(id_message,id_destinataire)
                    SELECT ?,id_utilisateur FROM chatflow.participant
                    WHERE id_conversation=? AND id_utilisateur<>? AND date_sortie IS NULL
                    """)) { p.setLong(1,messageId); p.setLong(2,conversationId); p.setLong(3,userId); p.executeUpdate(); }
                try (PreparedStatement p = c.prepareStatement("UPDATE chatflow.conversation SET derniere_activite=CURRENT_TIMESTAMP WHERE id_conversation=?")) {
                    p.setLong(1,conversationId); p.executeUpdate();
                }
                c.commit(); committed=true;
                Map<String,Object> out = new LinkedHashMap<>();
                out.put("id",messageId); out.put("senderId",userId); out.put("content",safeName);
                out.put("sentAt",sentAt); out.put("type",messageType); out.put("attachment",Map.of(
                        "id",blobId,"filename",safeName,"contentType",mime,"byteSize",bytes.length));
                return out;
            } catch (Exception e) { c.rollback(); throw e; }
        } finally {
            if (!committed) Files.deleteIfExists(target);
        }
    }

    public Map<String,Object> download(long userId, long blobId) throws Exception {
        String key, filename, mime;
        try (Connection c=dataSource.getConnection(); PreparedStatement p=c.prepareStatement("""
            SELECT b.key,b.filename,b.content_type
            FROM chatflow.active_storage_blobs b
            JOIN chatflow.active_storage_attachments a ON a.blob_id=b.id
            JOIN chatflow.message m ON m.id_message=a.record_id
            JOIN chatflow.participant pm ON pm.id_conversation=m.id_conversation
            WHERE b.id=? AND a.record_type='Message' AND pm.id_utilisateur=? AND pm.date_sortie IS NULL
            LIMIT 1
            """)) {
            p.setLong(1,blobId); p.setLong(2,userId);
            try(ResultSet r=p.executeQuery()) {
                if(!r.next()) throw new IllegalArgumentException("File not found or access denied.");
                key=r.getString(1); filename=r.getString(2); mime=r.getString(3);
            }
        }
        byte[] data=Files.readAllBytes(pathFor(key));
        return Map.of("id",blobId,"filename",filename,"contentType",mime,"data",Base64.getEncoder().encodeToString(data));
    }

    private Path pathFor(String key) {
        if (key == null || !key.matches("[a-f0-9]{32}")) throw new IllegalArgumentException("Invalid storage key.");
        Path path=root.resolve(key.substring(0,2)).resolve(key.substring(2,4)).resolve(key).normalize();
        if(!path.startsWith(root)) throw new IllegalArgumentException("Invalid storage path.");
        return path;
    }
    private static void requireMember(Connection c,long user,long conversation)throws SQLException {
        try(PreparedStatement p=c.prepareStatement("SELECT 1 FROM chatflow.participant WHERE id_utilisateur=? AND id_conversation=? AND date_sortie IS NULL")) {
            p.setLong(1,user); p.setLong(2,conversation);
            try(ResultSet r=p.executeQuery()) { if(!r.next()) throw new IllegalArgumentException("You are not a member of this conversation."); }
        }
    }
    private static String sha256(byte[] bytes)throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }
}
