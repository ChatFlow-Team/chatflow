package com.chaflow.client.service;

import com.chaflow.client.SocketClient;

/** Client authentication use cases. */
public final class AuthService {
    private final SocketService socket;
    public AuthService(SocketService socket){this.socket=socket;}
    public SocketClient.AuthResult login(String phone,String password){return socket.login(phone,password);}
    public SocketClient.AuthResult register(String phone,String username,String name,String password){return socket.register(phone,username,name,password);}
    public void logout(String token){socket.logout(token);}
}
