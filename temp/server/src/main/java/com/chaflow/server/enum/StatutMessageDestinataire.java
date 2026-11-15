package com.chaflow.server.model;

/**
 * Valeurs SQL de {@code message_destinataire.statut}, et non de {@code message}.
 * La valeur par défaut SQL est ENVOYE.
 */
public enum StatutMessageDestinataire {
    ENVOYE,
    RECU,
    LU
}
