
package com.chaflow.server.service;

import com.chaflow.server.dao.MessageDAO;
import com.chaflow.server.dao.ParticipantDAO;
import com.chaflow.server.model.Message;
import com.chaflow.server.model.TypeMessage;

import java.sql.SQLException;
import java.util.Objects;

public final class SendMessageService {

    private final MessageDAO messageDAO;
    private final ParticipantDAO participantDAO;

    public SendMessageService(
            MessageDAO messageDAO,
            ParticipantDAO participantDAO) {

        this.messageDAO = Objects.requireNonNull(messageDAO);
        this.participantDAO = Objects.requireNonNull(participantDAO);
    }

    public Message sendText(
            long senderId,
            long conversationId,
            String content) throws SQLException {

        if (senderId <= 0 || conversationId <= 0) {
            throw new IllegalArgumentException("Identifiant invalide");
        }

        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("Message vide");
        }

        if (!participantDAO.isActiveMember(senderId, conversationId)) {
            throw new SecurityException(
                    "Utilisateur non autorisé dans cette conversation");
        }

        Message message = new Message(
                0L,
                conversationId,
                senderId,
                content,
                TypeMessage.TEXTE,
                null,
                null,
                false
        );

        return messageDAO.save(message);
    }
}
