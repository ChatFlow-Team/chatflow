package com.chaflow.server.service;

import com.chaflow.server.repository.UserRepository;
import com.chaflow.server.auth.PasswordHasher;
import com.chaflow.server.model.Utilisateur;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Objects;

/** Authentication use cases. Password hashing and user persistence are separate concerns. */
public final class AuthService {
    public record Account(long id, String phone, String username, String displayName) {}
    private final UserRepository users;

    public AuthService(UserRepository users) { this.users = Objects.requireNonNull(users); }

    public Account register(String phone,String username,String displayName,String password)throws SQLException {
        phone=required(phone,"Phone number");
        username=required(username,"Username").toLowerCase();
        displayName=required(displayName,"Display name");
        validatePassword(password);
        if(!phone.matches("\\+?[0-9]{8,15}")) throw new IllegalArgumentException("Enter a valid phone number.");
        if(!username.matches("[a-z0-9_]{3,32}")) throw new IllegalArgumentException("Username must be 3–32 characters (letters, numbers, underscore).");
        if(displayName.length()>80) throw new IllegalArgumentException("Display name is too long.");
        if(users.existsByNumeroTelephone(phone) || users.findByPseudo(username).isPresent())
            throw new IllegalArgumentException("Phone number or username is already registered.");
        try {
            Utilisateur created=users.insert(new Utilisateur(0,phone,PasswordHasher.hash(password),username,displayName,null,"HORS_LIGNE",null,null));
            return account(created);
        } catch(SQLException e) {
            if("23505".equals(e.getSQLState())) throw new IllegalArgumentException("Phone number or username is already registered.");
            throw e;
        }
    }

    public Account login(String phone,String password)throws SQLException {
        phone=required(phone,"Phone number");
        if(password==null || password.isBlank()) throw new IllegalArgumentException("Enter your password.");
        Utilisateur user=users.findByNumeroTelephone(phone)
                .filter(u->PasswordHasher.verify(password,u.motDePasse()))
                .orElseThrow(()->new IllegalArgumentException("Incorrect phone number or password."));
        users.updateStatut(user.idUtilisateur(),"EN_LIGNE");
        users.updateDerniereConnexion(user.idUtilisateur(),Instant.now());
        return account(user);
    }

    public void setOnlineStatus(long userId,String status)throws SQLException {
        if(!"EN_LIGNE".equals(status) && !"HORS_LIGNE".equals(status)) throw new IllegalArgumentException("Invalid status.");
        users.updateStatut(userId,status);
        if("HORS_LIGNE".equals(status)) users.updateDerniereConnexion(userId,Instant.now());
    }

    private static Account account(Utilisateur u){return new Account(u.idUtilisateur(),u.numeroTelephone(),u.pseudo(),u.nomAffiche());}
    private static String required(String v,String label){if(v==null||v.isBlank())throw new IllegalArgumentException(label+" is required.");return v.trim();}
    private static void validatePassword(String p){if(p==null||p.length()<8)throw new IllegalArgumentException("Password must contain at least 8 characters.");}
}
