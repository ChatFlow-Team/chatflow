package com.chaflow.client.controller;

import com.chaflow.client.service.ChatService;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import com.chaflow.client.SocketClient;

/** Coordinates opening a direct chat, loading history and sending messages. */
public final class ChatController {
    private final ChatService service;
    public ChatController(ChatService service){this.service=service;}
    public long openDirect(String token,long userId)throws IOException{return service.openDirect(token,userId);}
    public List<Map<String,Object>> messages(String token,long conversationId)throws IOException{return service.messages(token,conversationId);}
    public Map<String,Object> sendMessage(String token,long conversationId,String body)throws IOException{return service.sendMessage(token,conversationId,body);}
    /** Uploads a file attachment to a conversation through the chat service. */
    public Map<String,Object> uploadAttachment(String token, long conversationId, String filename,
                                               String contentType, String base64Data) throws IOException {
        return service.uploadAttachment(token, conversationId, filename, contentType, base64Data);
    }
    /** Downloads an attachment after the server has checked conversation membership. */
    public Map<String,Object> downloadAttachment(String token, long blobId) throws IOException {
        return service.downloadAttachment(token, blobId);
    }
    public SocketClient.EventConnection listenEvents(String token, Consumer<Map<String,Object>> onEvent, Consumer<Exception> onError){return service.listenEvents(token,onEvent,onError);}
}
