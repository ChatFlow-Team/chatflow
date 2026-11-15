package com.chaflow.server.config;

/** Centralized server runtime configuration. */
public final class ServerConfig {
    private ServerConfig(){}
    public static int port(){
        int value=Integer.parseInt(System.getenv().getOrDefault("CHAFLOW_PORT","5050"));
        if(value<1||value>65535) throw new IllegalArgumentException("CHAFLOW_PORT must be between 1 and 65535.");
        return value;
    }
}
