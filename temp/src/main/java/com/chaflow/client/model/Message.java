package com.chaflow.client.model;
public record Message(long id,long conversationId,long senderId,String content,String sentAt,String editedAt) {}
