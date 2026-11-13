package com.chaflow.server.service;
 
import com.chaflow.server.db.Database;
 
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public final class ConversationService {
 
    /** @param created true if a brand-new conversation was created, false if an existing one was reused. */
    public record ConversationResult(long conversationId, boolean created) {}
 
    /**
     * @param requesterId the id of the user who is opening the conversation (the caller).
     *        TODO FOR YOU: SocketServer has no session/auth-token concept yet — every
     *        request is one tab-delimited line on a brand-new socket (see AuthService /
     *        SocketServer.process()). That means the caller's id currently has to be sent
     *        by the client itself inside the request (see the OPEN_CONVERSATION case added
     *        to SocketServer.java). That is fine for coursework, but it means any client
     *        could claim to be any requesterId — there's no real authentication binding a
     *        socket to a logged-in user. If/when the team adds real sessions or tokens,
     *        replace this parameter with whatever resolves the authenticated user from the
     *        session, instead of trusting a value the client sent.
     * @param targetIdOrPhone the other user's numeric id_utilisateur OR their phone number
     *        (numero_telephone), exactly as the caller typed it.
     */
    public ConversationResult openPrivateConversation(long requesterId, String targetIdOrPhone) throws SQLException {
        if (targetIdOrPhone == null || targetIdOrPhone.isBlank()) {
            throw new IllegalArgumentException("Provide the other user's id or phone number.");
        }
        String query = targetIdOrPhone.trim();
 
        try (Connection connection = Database.getConnection()) {
            // Acceptance criterion: "the connected user cannot create a conversation
            // with a user that does not exist" — resolve the target first and fail
            // fast if it doesn't resolve to a real account.
            long targetId = resolveTargetId(connection, query)
                    .orElseThrow(() -> new IllegalArgumentException("That user does not exist."));
 
            if (targetId == requesterId) {
                throw new IllegalArgumentException("You cannot open a conversation with yourself.");
            }
 
            // Acceptance criterion: "the server checks rights before returning the
            // conversation" — confirm the requester is a real account too. (Today that's
            // the whole "rights" check we can do without a session system — see the TODO
            // on requesterId above.)
            if (!userExists(connection, requesterId)) {
                throw new IllegalArgumentException("Unknown requesting user.");
            }
 
            // Acceptance criterion: "an existing conversation between the same pair is reused".
            Long existingId = findExistingPrivateConversation(connection, requesterId, targetId);
            if (existingId != null) {
                return new ConversationResult(existingId, false);
            }
 
            // No existing private conversation: create one, in a transaction, so that
            // two near-simultaneous "open conversation" requests between the same pair
            // can't each pass the check above and both create a duplicate conversation.
            // TODO FOR YOU: this reduces the race but doesn't fully close it without a
            // DB-level constraint (e.g. a unique index on a normalized participant pair).
            // Flagging it rather than adding that constraint myself since it touches the
            // schema/migrations, which the team owns.
            boolean previousAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                long conversationId = createConversation(connection);
                addParticipant(connection, conversationId, requesterId);
                addParticipant(connection, conversationId, targetId);
                connection.commit();
                return new ConversationResult(conversationId, true);
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(previousAutoCommit);
            }
        }
    }
 
    private Optional<Long> resolveTargetId(Connection connection, String idOrPhone) throws SQLException {
        if (idOrPhone.matches("\\d+")) {
            String sql = "SELECT id_utilisateur FROM chatflow.utilisateur WHERE id_utilisateur = ?";
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setLong(1, Long.parseLong(idOrPhone));
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? Optional.of(rs.getLong(1)) : Optional.empty();
                }
            }
        }
 
        String sql = "SELECT id_utilisateur FROM chatflow.utilisateur WHERE numero_telephone = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, idOrPhone);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(rs.getLong(1)) : Optional.empty();
            }
        }
    }
 
    private boolean userExists(Connection connection, long userId) throws SQLException {
        String sql = "SELECT 1 FROM chatflow.utilisateur WHERE id_utilisateur = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }
 
    /**
     * Returns the id of a PRIVEE conversation whose exactly-two active
     * participants are userA and userB, or null if no such conversation exists.
     */
    private Long findExistingPrivateConversation(Connection connection, long userA, long userB) throws SQLException {
        String sql = """
                SELECT c.id_conversation
                FROM chatflow.conversation c
                WHERE c.nature = 'PRIVEE'
                  AND EXISTS (
                        SELECT 1 FROM chatflow.participant p
                        WHERE p.id_conversation = c.id_conversation
                          AND p.id_utilisateur = ?
                          AND p.date_sortie IS NULL)
                  AND EXISTS (
                        SELECT 1 FROM chatflow.participant p
                        WHERE p.id_conversation = c.id_conversation
                          AND p.id_utilisateur = ?
                          AND p.date_sortie IS NULL)
                  AND (SELECT COUNT(*) FROM chatflow.participant p
                       WHERE p.id_conversation = c.id_conversation
                         AND p.date_sortie IS NULL) = 2
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, userA);
            ps.setLong(2, userB);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getLong(1) : null;
            }
        }
    }
 
    private long createConversation(Connection connection) throws SQLException {
        String sql = "INSERT INTO chatflow.conversation (nature) VALUES ('PRIVEE') RETURNING id_conversation";
        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getLong(1);
        }
    }
 
    private void addParticipant(Connection connection, long conversationId, long userId) throws SQLException {
        String sql = "INSERT INTO chatflow.participant (id_utilisateur, id_conversation, role) VALUES (?, ?, 'MEMBRE')";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setLong(2, conversationId);
            ps.executeUpdate();
        }
    }
}
