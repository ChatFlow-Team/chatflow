package com.chaflow.client.controller;

import com.chaflow.client.service.ChatService;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/** Coordinates people search, friends and conversation-list actions. */
public final class MessengerController {
    private final ChatService service;
    public MessengerController(ChatService service){this.service=service;}
    public List<Map<String,Object>> search(String token,String q)throws IOException{return service.search(token,q);}
    public Map<String,Object> friendRequest(String token,long id)throws IOException{return service.friendRequest(token,id);}
    public Map<String,Object> respond(String token,long id,boolean accept)throws IOException{return service.respond(token,id,accept);}
    public List<Map<String,Object>> friendRequests(String token)throws IOException{return service.friendRequests(token);}
    public List<Map<String,Object>> friends(String token)throws IOException{return service.friends(token);}
    public Map<String,Object> removeFriend(String token,long id)throws IOException{return service.removeFriend(token,id);}
    public List<Map<String,Object>> conversations(String token)throws IOException{return service.conversations(token);}
}
