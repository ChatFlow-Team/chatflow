package com.chaflow.server.dao;

import com.chaflow.server.model.Message;
import com.chaflow.server.enums.TypeMessage;

import javax.sql.DataSource;
import java.sql.*;
import java.time.OffsetDateTime;
import java.util.*;

public class MessageDAO {

    private final DataSource dataSource;

    private static final String COLUMNS =
            "id_message, id_conversation, id_expediteur, " +
            "contenu, type_message, envoye_le, modifie_le, supprime_le";

    public MessageDAO(DataSource dataSource) {
        this.dataSource = Objects.requireNonNull(dataSource);
    }

    // 1. Enregistrer un message
    public Message save(Message message) throws SQLException {
        Objects.requireNonNull(message);
        Objects.requireNonNull(message.typeMessage());

        String sql = """
                INSERT INTO chatflow.message
                (id_conversation, id_expediteur, contenu, type_message)
                VALUES (?, ?, ?, ?)
                RETURNING id_message, id_conversation, id_expediteur,
                          contenu, type_message, envoye_le,
                          modifie_le, supprime_le
                """;

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setLong(1, message.idConversation());
            statement.setLong(2, message.idExpediteur());
            statement.setString(3, message.contenu());
            statement.setString(4, message.typeMessage().name());

            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return mapMessage(rs);
                }
            }
        }

        throw new SQLException("Impossible de creer le message");
    }

    // 2. Rechercher un message par ID
    public Optional<Message> findById(long id) throws SQLException {
        String sql = "SELECT " + COLUMNS +
                " FROM chatflow.message WHERE id_message = ?";

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setLong(1, id);

            try (ResultSet rs = statement.executeQuery()) {
                return rs.next()
                        ? Optional.of(mapMessage(rs))
                        : Optional.empty();
            }
        }
    }

    // 3. Historique des messages
    public List<Message> findByConversation(
            long idConversation, int limit, int offset)
            throws SQLException {

        if (limit <= 0 || offset < 0) {
            throw new IllegalArgumentException("Pagination invalide");
        }

        String sql = "SELECT " + COLUMNS + " " + """
                 FROM chatflow.message
                 WHERE id_conversation = ?
                   AND supprime_le IS NULL
                 ORDER BY envoye_le DESC, id_message DESC
                 LIMIT ? OFFSET ?
                 """;

        List<Message> messages = new ArrayList<>();

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setLong(1, idConversation);
            statement.setInt(2, limit);
            statement.setInt(3, offset);

            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    messages.add(mapMessage(rs));
                }
            }
        }

        Collections.reverse(messages); // expose oldest-to-newest for chat timelines
        return messages;
    }

    // 4. Modifier le contenu
    public boolean updateContent(long id, String contenu)
            throws SQLException {

        String sql = """
                UPDATE chatflow.message
                SET contenu = ?, modifie_le = CURRENT_TIMESTAMP
                WHERE id_message = ? AND supprime_le IS NULL
                """;

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, contenu);
            statement.setLong(2, id);
            return statement.executeUpdate() > 0;
        }
    }

    // 5. Suppression logique
    public boolean softDelete(long id) throws SQLException {
        String sql = """
                UPDATE chatflow.message
                SET supprime_le = CURRENT_TIMESTAMP
                WHERE id_message = ? AND supprime_le IS NULL
                """;

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setLong(1, id);
            return statement.executeUpdate() > 0;
        }
    }

    // 6. Conversion ResultSet vers Message
    private Message mapMessage(ResultSet rs) throws SQLException {
        OffsetDateTime dateEnvoi =
                rs.getObject("envoye_le", OffsetDateTime.class);

        OffsetDateTime modifieLe =
                rs.getObject("modifie_le", OffsetDateTime.class);

        OffsetDateTime supprimeLe =
                rs.getObject("supprime_le", OffsetDateTime.class);

        return new Message(
                rs.getLong("id_message"),
                rs.getLong("id_conversation"),
                rs.getLong("id_expediteur"),
                rs.getString("contenu"),
                TypeMessage.valueOf(rs.getString("type_message")),
                dateEnvoi,
                modifieLe,
                supprimeLe != null
        );
    }
}
