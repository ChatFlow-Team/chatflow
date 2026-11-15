package com.chaflow.server.dao;

import com.chaflow.server.model.Conversation;
import com.chaflow.server.model.NatureConversation;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.Optional;

public class ConversationDAO {

    private static final String FIND_BY_ID_SQL = """
            SELECT id_conversation, nature, cree_le, derniere_activite
            FROM chatflow.conversation
            WHERE id_conversation = ?
            """;

    private static final String INSERT_SQL = """
            INSERT INTO chatflow.conversation (nature)
            VALUES (?)
            RETURNING id_conversation, nature, cree_le, derniere_activite
            """;

    private final DataSource dataSource;

    public ConversationDAO(DataSource dataSource) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource");
    }

    public Optional<Conversation> findById(long id) throws SQLException {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(FIND_BY_ID_SQL)) {

            statement.setLong(1, id);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? Optional.of(mapConversation(rs)) : Optional.empty();
            }
        }
    }

    public Conversation save(Conversation conversation) throws SQLException {
        Objects.requireNonNull(conversation, "conversation");
        if (conversation.idConversation() != 0) {
            throw new IllegalArgumentException("Une nouvelle conversation doit avoir idConversation = 0");
        }
        Objects.requireNonNull(conversation.nature(), "nature");

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(INSERT_SQL)) {

            statement.setString(1, conversation.nature().name());
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return mapConversation(rs);
                }
                throw new SQLException("PostgreSQL n'a pas retourne la conversation creee");
            }
        }
    }

    private static Conversation mapConversation(ResultSet rs) throws SQLException {
        return new Conversation(
                rs.getLong("id_conversation"),
                NatureConversation.valueOf(rs.getString("nature")),
                rs.getObject("cree_le", OffsetDateTime.class),
                rs.getObject("derniere_activite", OffsetDateTime.class)
        );
    }
}
