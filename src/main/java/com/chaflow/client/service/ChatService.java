package com.chaflow.client.service;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import com.chaflow.client.SocketClient;

/** Client chat and social use cases; transport details stay in SocketService. */
public final class ChatService {
    private final SocketService socket;
    public ChatService(SocketService socket){this.socket=socket;}
    public List<Map<String,Object>> search(String token,String q)throws IOException{return socket.search(token,q);}
    public Map<String,Object> friendRequest(String token,long id)throws IOException{return socket.friendRequest(token,id);}
    public Map<String,Object> respond(String token,long id,boolean accept)throws IOException{return socket.respond(token,id,accept);}
    public List<Map<String,Object>> friendRequests(String token)throws IOException{return socket.friendRequests(token);}
    public List<Map<String,Object>> friends(String token)throws IOException{return socket.friends(token);}
    public Map<String,Object> removeFriend(String token,long id)throws IOException{return socket.removeFriend(token,id);}
    public List<Map<String,Object>> conversations(String token)throws IOException{return socket.conversations(token);}
    public long openDirect(String token,long id)throws IOException{return socket.openDirect(token,id);}
    public List<Map<String,Object>> messages(String token,long id)throws IOException{return socket.messages(token,id);}
    public Map<String,Object> sendMessage(String token,long id,String body)throws IOException{return socket.sendMessage(token,id,body);}
    public SocketClient.EventConnection listenEvents(String token, Consumer<Map<String,Object>> onEvent, Consumer<Exception> onError){return socket.listenEvents(token,onEvent,onError);}
}
