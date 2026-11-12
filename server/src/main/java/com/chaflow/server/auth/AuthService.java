package com.chaflow.server.auth;

import com.chaflow.server.db.Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public final class AuthService {
    public record Account(long id, String phone, String username, String displayName) {}

    public Account register(String phone, String username, String displayName, String password) throws SQLException {
        phone = required(phone, "Phone number");
        username = required(username, "Username").toLowerCase();
        displayName = required(displayName, "Display name");
        validatePassword(password);
        if (!phone.matches("\\+?[0-9]{8,15}")) throw new IllegalArgumentException("Enter a valid phone number.");
        if (!username.matches("[a-z0-9_]{3,32}")) throw new IllegalArgumentException("Username must be 3–32 characters (letters, numbers, underscore).");
        if (displayName.length() > 80) throw new IllegalArgumentException("Display name is too long.");

        String sql = "INSERT INTO chatflow.utilisateur (numero_telephone,mot_de_passe,pseudo,nom_affiche) VALUES (?,?,?,?) RETURNING id_utilisateur,numero_telephone,pseudo,nom_affiche";
        try (Connection c = Database.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, phone);
            ps.setString(2, PasswordHasher.hash(password));
            ps.setString(3, username);
            ps.setString(4, displayName);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return new Account(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4));
            }
        } catch (SQLException e) {
            if ("23505".equals(e.getSQLState())) throw new IllegalArgumentException("Phone number or username is already registered.");
            throw e;
        }
    }

    public Account login(String phone, String password) throws SQLException {
        phone = required(phone, "Phone number");
        if (password == null || password.isBlank()) throw new IllegalArgumentException("Enter your password.");
        String sql = "SELECT id_utilisateur,numero_telephone,pseudo,nom_affiche,mot_de_passe FROM chatflow.utilisateur WHERE numero_telephone = ?";
        try (Connection c = Database.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, phone);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next() || !PasswordHasher.verify(password, rs.getString("mot_de_passe")))
                    throw new IllegalArgumentException("Incorrect phone number or password.");
                long id = rs.getLong("id_utilisateur");
                try (PreparedStatement update = c.prepareStatement("UPDATE chatflow.utilisateur SET statut='EN_LIGNE', derniere_connexion=CURRENT_TIMESTAMP WHERE id_utilisateur=?")) {
                    update.setLong(1, id); update.executeUpdate();
                }
                return new Account(id, rs.getString("numero_telephone"), rs.getString("pseudo"), rs.getString("nom_affiche"));
            }
        }
    }

    private static String required(String value, String label) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(label + " is required.");
        return value.trim();
    }
    private static void validatePassword(String password) {
        if (password == null || password.length() < 8) throw new IllegalArgumentException("Password must contain at least 8 characters.");
    }
}
