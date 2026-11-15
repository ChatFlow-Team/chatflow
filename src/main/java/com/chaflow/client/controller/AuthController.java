package com.chaflow.client.controller;

import com.chaflow.client.SocketClient;
import com.chaflow.client.service.AuthService;

/** Handles sign-in/sign-up UI actions. */
public final class AuthController {
    private final AuthService service;
    public AuthController(AuthService service){this.service=service;}
    public SocketClient.AuthResult login(String phone,String password){return service.login(phone,password);}
    public SocketClient.AuthResult register(String phone,String username,String name,String password){return service.register(phone,username,name,password);}
    public void logout(String token){service.logout(token);}
}
