package com.chaflow.server.repository;

import com.chaflow.server.dao.UtilisateurDAO;
import com.chaflow.server.model.Utilisateur;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/** JDBC adapter around the existing user DAO. */
public final class JdbcUserRepository implements UserRepository {
    private final UtilisateurDAO dao;
    public JdbcUserRepository(UtilisateurDAO dao){this.dao=Objects.requireNonNull(dao);}
    public Optional<Utilisateur> findByNumeroTelephone(String phone)throws SQLException{return dao.findByNumeroTelephone(phone);}
    public boolean existsByNumeroTelephone(String phone)throws SQLException{return dao.existsByNumeroTelephone(phone);}
    public Optional<Utilisateur> findByPseudo(String username)throws SQLException{return dao.findByPseudo(username);}
    public Utilisateur insert(Utilisateur user)throws SQLException{return dao.insert(user);}
    public void updateStatut(long id,String status)throws SQLException{dao.updateStatut(id,status);}
    public void updateDerniereConnexion(long id,Instant when)throws SQLException{dao.updateDerniereConnexion(id,when);}
}
