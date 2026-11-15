package com.chaflow.server.model;

import java.time.OffsetDateTime;

/**
 * Ligne de la table {@code chatflow.participant}.
 * Les deux identifiants constituent la clé primaire, sans identifiant généré.
 * {@code rejointLe} est obligatoire en base et possède une valeur par défaut SQL.
 */
public record Participant(
        long idUtilisateur,
        long idConversation,
        RoleParticipant role,
        OffsetDateTime rejointLe
) {
    public ParticipantId id() {
        return new ParticipantId(idUtilisateur, idConversation);
    }
}
