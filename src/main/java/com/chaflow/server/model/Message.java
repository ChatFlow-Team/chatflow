package com.chaflow.server.model;

import com.chaflow.server.enums.TypeMessage;

import java.time.OffsetDateTime;

/**
 * Ligne de la table {@code chatflow.message}.
 * L'identifiant est généré par PostgreSQL ; 0 désigne un nouveau message.
 * {@code contenu} et {@code modifieLe} sont facultatifs (null autorisé en base).
 * {@code dateEnvoi} est obligatoire en base et possède une valeur par défaut SQL.
 * L'expéditeur doit être participant de la conversation : la clé étrangère porte
 * sur le couple (idExpediteur, idConversation).
 * Les statuts et dates de réception/lecture appartiennent à
 * {@code message_destinataire}, séparément pour chaque destinataire.
 */
public record Message(
        long idMessage,
        long idConversation,
        long idExpediteur,
        String contenu,
        TypeMessage typeMessage,
        OffsetDateTime dateEnvoi,
        OffsetDateTime modifieLe,
        boolean supprime
) {
}
