package com.chaflow.server.model;

import java.time.Instant;

public record Utilisateur(
        long idUtilisateur,
        String numeroTelephone,
        String motDePasse,
        String pseudo,
        String nomAffiche,
        String photoProfil,
        String statut,
        Instant derniereConnexion,
        Instant creeLe
) {
}