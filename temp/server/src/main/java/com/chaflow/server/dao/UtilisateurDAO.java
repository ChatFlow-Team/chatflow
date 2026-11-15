package com.chaflow.server.dao;

import com.chaflow.server.model.Utilisateur;

import java.sql.SQLException;
import java.time.Instant;
import java.util.Optional;

public interface UtilisateurDAO {

    Optional<Utilisateur> findById(long idUtilisateur) throws SQLException;

    Optional<Utilisateur> findByNumeroTelephone(String numeroTelephone) throws SQLException;
    boolean existsByNumeroTelephone(String numeroTelephone) throws SQLException;
    Optional<Utilisateur> findByPseudo(String pseudo) throws SQLException;

    /** Inserts a new user (idUtilisateur is ignored) and returns it with the generated id and creeLe. */
    Utilisateur insert(Utilisateur utilisateur) throws SQLException;

    void updateProfil(long idUtilisateur, String nomAffiche, String photoProfil) throws SQLException;

    /** statut must be one of: EN_LIGNE, HORS_LIGNE, ABSENT, OCCUPE (SQL CHECK constraint). */
    void updateStatut(long idUtilisateur, String statut) throws SQLException;

    void updateDerniereConnexion(long idUtilisateur, Instant quand) throws SQLException;

    boolean delete(long idUtilisateur) throws SQLException;
}
