package com.chaflow.server.dao;

import com.chaflow.server.dao.UtilisateurDAO;
import com.chaflow.server.model.Utilisateur;

import javax.sql.DataSource;
import java.sql.*;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

public class UtilisateurDaoJdbc implements UtilisateurDAO{

    private static final String COLUMNS =
            "id_utilisateur, numero_telephone, mot_de_passe, pseudo, nom_affiche, " +
            "photo_profil, statut, derniere_connexion, cree_le";

    private final DataSource dataSource;

    public UtilisateurDaoJdbc(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Optional<Utilisateur> findById(long id) throws SQLException {
        return findOne("SELECT " + COLUMNS + " FROM chatflow.utilisateur WHERE id_utilisateur = ?",
                ps -> ps.setLong(1, id));
    }

    @Override
    public Optional<Utilisateur> findByNumeroTelephone(String numero) throws SQLException {
        return findOne("SELECT " + COLUMNS + " FROM chatflow.utilisateur WHERE numero_telephone = ?",
                ps -> ps.setString(1, numero));
    }

    @Override
    public boolean existsByNumeroTelephone(String numeroTelephone) throws SQLException {
        String sql = """
                SELECT EXISTS (
                    SELECT 1
                    FROM chatflow.utilisateur
                    WHERE numero_telephone = ?
                )
                """;

        try (Connection connection = dataSource.getConnection();
            PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, numeroTelephone);

            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getBoolean(1);
            }
        }
    }

    @Override
    public Optional<Utilisateur> findByPseudo(String pseudo) throws SQLException {
        return findOne("SELECT " + COLUMNS + " FROM chatflow.utilisateur WHERE pseudo = ?",
                ps -> ps.setString(1, pseudo));
    }

    @Override
    public Utilisateur insert(Utilisateur u) throws SQLException {
        // statut and cree_le are omitted so PostgreSQL applies its defaults
        String sql = "INSERT INTO chatflow.utilisateur " +
                "(numero_telephone, mot_de_passe, pseudo, nom_affiche, photo_profil) " +
                "VALUES (?, ?, ?, ?, ?) RETURNING " + COLUMNS;
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, u.numeroTelephone());
            ps.setString(2, u.motDePasse());   // must already be a hash
            ps.setString(3, u.pseudo());
            ps.setString(4, u.nomAffiche());
            ps.setString(5, u.photoProfil());
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return map(rs);
            }
        }
    }

    @Override
    public void updateProfil(long id, String nomAffiche, String photoProfil) throws SQLException {
        execute("UPDATE chatflow.utilisateur SET nom_affiche = ?, photo_profil = ? WHERE id_utilisateur = ?",
                ps -> {
                    ps.setString(1, nomAffiche);
                    ps.setString(2, photoProfil);
                    ps.setLong(3, id);
                });
    }

    @Override
    public void updateStatut(long id, String statut) throws SQLException {
        execute("UPDATE chatflow.utilisateur SET statut = ? WHERE id_utilisateur = ?",
                ps -> {
                    ps.setString(1, statut);
                    ps.setLong(2, id);
                });
    }

    @Override
    public void updateDerniereConnexion(long id, Instant quand) throws SQLException {
        execute("UPDATE chatflow.utilisateur SET derniere_connexion = ? WHERE id_utilisateur = ?",
                ps -> {
                    ps.setObject(1, OffsetDateTime.ofInstant(quand, ZoneOffset.UTC));
                    ps.setLong(2, id);
                });
    }

    @Override
    public boolean delete(long id) throws SQLException {
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "DELETE FROM chatflow.utilisateur WHERE id_utilisateur = ?")) {
            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    // ---------- helpers ----------

    @FunctionalInterface
    private interface Binder { void bind(PreparedStatement ps) throws SQLException; }

    private Optional<Utilisateur> findOne(String sql, Binder binder) throws SQLException {
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            binder.bind(ps);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        }
    }

    private void execute(String sql, Binder binder) throws SQLException {
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            binder.bind(ps);
            ps.executeUpdate();
        }
    }

    private Utilisateur map(ResultSet rs) throws SQLException {
        OffsetDateTime derniere = rs.getObject("derniere_connexion", OffsetDateTime.class);
        OffsetDateTime cree = rs.getObject("cree_le", OffsetDateTime.class);
        return new Utilisateur(
                rs.getLong("id_utilisateur"),
                rs.getString("numero_telephone"),
                rs.getString("mot_de_passe"),
                rs.getString("pseudo"),
                rs.getString("nom_affiche"),
                rs.getString("photo_profil"),
                rs.getString("statut"),
                derniere == null ? null : derniere.toInstant(),
                cree == null ? null : cree.toInstant()
        );
    }
}