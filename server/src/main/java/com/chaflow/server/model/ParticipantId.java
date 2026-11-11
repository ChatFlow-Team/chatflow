package com.chaflow.server.model;

/** Clé composée de {@code participant}, dans l'ordre défini par le SQL. */
public record ParticipantId(
        long idUtilisateur,
        long idConversation
) {
}
