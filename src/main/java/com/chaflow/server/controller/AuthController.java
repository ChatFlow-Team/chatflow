package com.chaflow.server.controller;

import com.chaflow.server.service.AuthService;
import java.sql.SQLException;
import java.util.Objects;

/** Socket-facing authentication controller. */
public final class AuthController {
    private final AuthService service;
    public AuthController(AuthService service){this.service=Objects.requireNonNull(service);}
    public AuthService.Account login(String phone,String password)throws SQLException{return service.login(phone,password);}
    public AuthService.Account register(String phone,String username,String displayName,String password)throws SQLException{return service.register(phone,username,displayName,password);}
    public void setOnlineStatus(long userId,String status)throws SQLException{service.setOnlineStatus(userId,status);}
}
