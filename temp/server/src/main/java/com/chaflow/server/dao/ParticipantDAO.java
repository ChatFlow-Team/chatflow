package com.chaflow.server.dao;

import com.chaflow.server.model.Participant;
import com.chaflow.server.model.RoleParticipant;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class ParticipantDAO {

    private static final String INSERT_SQL = """
            INSERT INTO chatflow.participant (id_utilisateur, id_conversation, role)
            VALUES (?, ?, ?)
            RETURNING id_utilisateur, id_conversation, role, date_adhesion
            """;

    private static final String FIND_BY_ID_SQL = """
            SELECT id_utilisateur, id_conversation, role, date_adhesion
            FROM chatflow.participant
            WHERE id_utilisateur = ? AND id_conversation = ?
            """;

    private static final String FIND_BY_CONVERSATION_SQL = """
            SELECT id_utilisateur, id_conversation, role, date_adhesion
            FROM chatflow.participant
            WHERE id_conversation = ? AND date_sortie IS NULL
            ORDER BY id_utilisateur
            """;

    private static final String IS_ACTIVE_MEMBER_SQL = """
            SELECT 1 FROM chatflow.participant
            WHERE id_utilisateur = ? AND id_conversation = ? AND date_sortie IS NULL
            """;

    private static final String LEAVE_SQL = """
            UPDATE chatflow.participant
            SET date_sortie = CURRENT_TIMESTAMP
            WHERE id_utilisateur = ? AND id_conversation = ? AND date_sortie IS NULL
            """;

    private final DataSource dataSource;

    public ParticipantDAO(DataSource dataSource) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource");
    }

    public Participant save(Participant participant) throws SQLException {
        Objects.requireNonNull(participant, "participant");
        Objects.requireNonNull(participant.role(), "role");

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(INSERT_SQL)) {

            statement.setLong(1, participant.idUtilisateur());
            statement.setLong(2, participant.idConversation());
            statement.setString(3, participant.role().name());

            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return mapParticipant(rs);
                }
                throw new SQLException("PostgreSQL n'a pas retourne le participant cree");
            }
        }
    }

    public Optional<Participant> findById(long idUtilisateur, long idConversation) throws SQLException {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(FIND_BY_ID_SQL)) {

            statement.setLong(1, idUtilisateur);
            statement.setLong(2, idConversation);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? Optional.of(mapParticipant(rs)) : Optional.empty();
            }
        }
    }

    public List<Participant> findByConversation(long idConversation) throws SQLException {
        List<Participant> participants = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(FIND_BY_CONVERSATION_SQL)) {

            statement.setLong(1, idConversation);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    participants.add(mapParticipant(rs));
                }
            }
        }
        return participants;
    }

    public boolean isActiveMember(long idUtilisateur, long idConversation) throws SQLException {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(IS_ACTIVE_MEMBER_SQL)) {

            statement.setLong(1, idUtilisateur);
            statement.setLong(2, idConversation);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next();
            }
        }
    }

    public boolean leaveConversation(long idUtilisateur, long idConversation) throws SQLException {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(LEAVE_SQL)) {

            statement.setLong(1, idUtilisateur);
            statement.setLong(2, idConversation);
            return statement.executeUpdate() > 0;
        }
    }

    private static Participant mapParticipant(ResultSet rs) throws SQLException {
        return new Participant(
                rs.getLong("id_utilisateur"),
                rs.getLong("id_conversation"),
                RoleParticipant.valueOf(rs.getString("role")),
                rs.getObject("date_adhesion", OffsetDateTime.class)
        );
    }
}
