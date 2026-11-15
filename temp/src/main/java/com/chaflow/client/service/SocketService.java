package com.chaflow.client.service;

import com.chaflow.client.SocketClient;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Sole client-side transport adapter. It speaks the existing socket protocol. */
public final class SocketService {
    private final SocketClient transport;
    public SocketService(){this(new SocketClient());}
    public SocketService(SocketClient transport){this.transport=Objects.requireNonNull(transport);}
    public SocketClient.AuthResult login(String phone,String password){return transport.login(phone,password);}
    public SocketClient.AuthResult register(String phone,String username,String name,String password){return transport.register(phone,username,name,password);}
    public void logout(String token){transport.logout(token);}
    public List<Map<String,Object>> search(String token,String q)throws IOException{return transport.search(token,q);}
    public Map<String,Object> friendRequest(String token,long id)throws IOException{return transport.friendRequest(token,id);}
    public Map<String,Object> respond(String token,long id,boolean accept)throws IOException{return transport.respond(token,id,accept);}
    public List<Map<String,Object>> friendRequests(String token)throws IOException{return transport.friendRequests(token);}
    public List<Map<String,Object>> friends(String token)throws IOException{return transport.friends(token);}
    public Map<String,Object> removeFriend(String token,long id)throws IOException{return transport.removeFriend(token,id);}
    public long openDirect(String token,long id)throws IOException{return transport.openDirect(token,id);}
    public List<Map<String,Object>> conversations(String token)throws IOException{return transport.conversations(token);}
    public List<Map<String,Object>> messages(String token,long id)throws IOException{return transport.messages(token,id);}
    public Map<String,Object> sendMessage(String token,long id,String body)throws IOException{return transport.sendMessage(token,id,body);}
}
