package com.chaflow.server;

import com.chaflow.server.service.AuthService;
import com.chaflow.server.service.ChatService;
import com.chaflow.server.controller.AuthController;
import com.chaflow.server.controller.ChatController;
import com.chaflow.server.dao.ChatDAO;
import com.chaflow.server.dao.UtilisateurDaoJdbc;
import com.chaflow.server.db.Database;
import com.chaflow.server.repository.JdbcChatRepository;
import com.chaflow.server.repository.JdbcUserRepository;
import com.chaflow.server.config.ServerConfig;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;

public final class SocketServer {
    private static final int PORT = ServerConfig.port();
    private static final AuthController AUTH = new AuthController(
            new AuthService(new JdbcUserRepository(new UtilisateurDaoJdbc(Database.getDataSource()))));
    private static final ChatController CHAT = new ChatController(
            new ChatService(new JdbcChatRepository(new ChatDAO(Database.getDataSource()))));
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Base64.Encoder ENC = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DEC = Base64.getUrlDecoder();
    private static final ConcurrentMap<String, Long> SESSIONS = new ConcurrentHashMap<>();

    public static void main(String[] args) throws Exception {
        Database.migrate();
        System.out.println("Flyway migrations completed. ChaFlow database is ready.");
        ExecutorService clients = Executors.newCachedThreadPool();
        try (ServerSocket server = new ServerSocket(PORT)) {
            System.out.println("ChaFlow socket server listening on port " + PORT);
            while (true) { Socket socket = server.accept(); clients.submit(() -> handle(socket)); }
        } finally { clients.shutdown(); }
    }

    private static void handle(Socket socket) {
        try (socket;
             BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8))) {
            socket.setSoTimeout(15000);
            String response = process(reader.readLine());
            writer.write(response); writer.newLine(); writer.flush();
        } catch (Exception e) { System.err.println("Client request failed: " + e.getMessage()); }
    }

    private static String process(String request) {
        try {
            if (request == null || request.isBlank()) throw new IllegalArgumentException("Empty request.");
            String[] f = request.split("\\t", -1);
            String cmd=f[0]; Object result;
            if ("LOGIN".equals(cmd) && f.length==3) {
                AuthService.Account a=AUTH.login(dec(f[1]),dec(f[2])); result=account(a);
            } else if ("REGISTER".equals(cmd) && f.length==5) {
                AuthService.Account a=AUTH.register(dec(f[1]),dec(f[2]),dec(f[3]),dec(f[4])); result=account(a);
            } else {
                if(f.length<2)throw new IllegalArgumentException("Authentication required.");
                Long me=SESSIONS.get(dec(f[1])); if(me==null)throw new IllegalArgumentException("Session expired. Please sign in again.");
                result=switch(cmd){
                    case "SEARCH_USERS" -> { require(f,3); yield CHAT.searchUsers(me,dec(f[2])); }
                    case "FRIEND_REQUEST" -> { require(f,3); yield CHAT.sendFriendRequest(me,Long.parseLong(dec(f[2]))); }
                    case "FRIEND_RESPOND" -> { require(f,4); CHAT.respondFriendRequest(me,Long.parseLong(dec(f[2])),Boolean.parseBoolean(dec(f[3]))); yield Map.of("message","Friend request updated."); }
                    case "FRIEND_REQUESTS" -> CHAT.friendRequests(me);
                    case "FRIENDS" -> CHAT.friends(me);
                    case "FRIEND_REMOVE" -> { require(f,3); CHAT.removeFriend(me,Long.parseLong(dec(f[2]))); yield Map.of("message","Friend removed."); }
                    case "OPEN_DIRECT" -> { require(f,3); yield Map.of("conversationId",CHAT.openDirectConversation(me,Long.parseLong(dec(f[2])))); }
                    case "CONVERSATIONS" -> CHAT.conversations(me);
                    case "MESSAGES" -> { require(f,3); yield CHAT.messages(me,Long.parseLong(dec(f[2])),200); }
                    case "SEND_MESSAGE" -> { require(f,4); yield CHAT.sendMessage(me,Long.parseLong(dec(f[2])),dec(f[3])); }
                    case "LOGOUT" -> { SESSIONS.remove(dec(f[1])); AUTH.setOnlineStatus(me,"HORS_LIGNE"); yield Map.of("message","Signed out."); }
                    default -> throw new IllegalArgumentException("Unknown request.");
                };
            }
            return "OK\t"+enc(JSON.writeValueAsString(result));
        } catch (IllegalArgumentException e) { return "ERROR\t"+enc(e.getMessage()==null?"Invalid request.":e.getMessage()); }
        catch (Exception e) { e.printStackTrace(); return "ERROR\t"+enc("Server error. Check the server logs."); }
    }

    private static Map<String,Object> account(AuthService.Account a){try{AUTH.setOnlineStatus(a.id(),"EN_LIGNE");}catch(Exception e){throw new IllegalStateException("Could not update account status.",e);}String token=UUID.randomUUID().toString();SESSIONS.put(token,a.id());Map<String,Object> m=new LinkedHashMap<>();m.put("id",a.id());m.put("phone",a.phone());m.put("username",a.username());m.put("displayName",a.displayName());m.put("token",token);return m;}
    private static void require(String[] f,int n){if(f.length!=n)throw new IllegalArgumentException("Invalid request format.");}
    private static String dec(String s){return new String(DEC.decode(s),StandardCharsets.UTF_8);}
    private static String enc(String s){return ENC.encodeToString(s.getBytes(StandardCharsets.UTF_8));}
}
