package com.chaflow.server.model;

import java.time.OffsetDateTime;

/**
 * Ligne de la table {@code chatflow.conversation}.
 * L'identifiant est généré par PostgreSQL ; 0 désigne une nouvelle conversation.
 * Les dates obligatoires sont renseignées par les valeurs par défaut SQL à l'insertion.
 */
public record Conversation(
        long idConversation,
        NatureConversation nature,
        OffsetDateTime creeLe,
        OffsetDateTime derniereActivite
) {
}
