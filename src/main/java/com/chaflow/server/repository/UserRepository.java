package com.chaflow.server.repository;

import com.chaflow.server.model.Utilisateur;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Optional;

/** User persistence contract used by authentication services. */
public interface UserRepository {
    Optional<Utilisateur> findByNumeroTelephone(String phone) throws SQLException;
    boolean existsByNumeroTelephone(String phone) throws SQLException;
    Optional<Utilisateur> findByPseudo(String username) throws SQLException;
    Utilisateur insert(Utilisateur user) throws SQLException;
    void updateStatut(long userId,String status) throws SQLException;
    void updateDerniereConnexion(long userId,Instant when) throws SQLException;
}
