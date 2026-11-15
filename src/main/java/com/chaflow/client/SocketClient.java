package com.chaflow.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class SocketClient {
    private static final String HOST=System.getProperty("chaflow.host","127.0.0.1");
    private static final int PORT=Integer.getInteger("chaflow.port",5050);
    private static final Base64.Encoder ENC=Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DEC=Base64.getUrlDecoder();
    private static final ObjectMapper JSON=new ObjectMapper();

    public AuthResult login(String phone,String password){return auth("LOGIN",phone,password);}
    public AuthResult register(String phone,String username,String displayName,String password){return auth("REGISTER",phone,username,displayName,password);}
    private AuthResult auth(String cmd,String... values){try{Map<String,Object> m=JSON.readValue(raw(cmd,values),new TypeReference<>(){});return new AuthResult(true,num(m,"id"),str(m,"phone"),str(m,"username"),str(m,"displayName"),str(m,"token"),"Welcome, "+str(m,"displayName")+"!");}catch(Exception e){return AuthResult.failure(message(e));}}
    public List<Map<String,Object>> search(String token,String query)throws IOException{return list("SEARCH_USERS",token,query);}
    public Map<String,Object> friendRequest(String token,long target)throws IOException{return object("FRIEND_REQUEST",token,Long.toString(target));}
    public Map<String,Object> respond(String token,long requestId,boolean accept)throws IOException{return object("FRIEND_RESPOND",token,Long.toString(requestId),Boolean.toString(accept));}
    public List<Map<String,Object>> friendRequests(String token)throws IOException{return list("FRIEND_REQUESTS",token);}
    public List<Map<String,Object>> friends(String token)throws IOException{return list("FRIENDS",token);}
    public Map<String,Object> removeFriend(String token,long userId)throws IOException{return object("FRIEND_REMOVE",token,Long.toString(userId));}
    public long openDirect(String token,long target)throws IOException{return num(object("OPEN_DIRECT",token,Long.toString(target)),"conversationId");}
    public List<Map<String,Object>> conversations(String token)throws IOException{return list("CONVERSATIONS",token);}
    public List<Map<String,Object>> messages(String token,long conversation)throws IOException{return list("MESSAGES",token,Long.toString(conversation));}
    public Map<String,Object> sendMessage(String token,long conversation,String content)throws IOException{return object("SEND_MESSAGE",token,Long.toString(conversation),content);}
    public void logout(String token){try{raw("LOGOUT",token);}catch(Exception ignored){}}

    private List<Map<String,Object>> list(String command,String token,String... values)throws IOException{return JSON.convertValue(JSON.readTree(raw(command,prepend(token,values))),new TypeReference<>(){});}
    private Map<String,Object> object(String command,String token,String... values)throws IOException{return JSON.readValue(raw(command,prepend(token,values)),new TypeReference<>(){});}
    private static String[] prepend(String token,String[] values){String[] all=new String[values.length+1];all[0]=token;System.arraycopy(values,0,all,1,values.length);return all;}
    private String raw(String command,String... values)throws IOException{
        StringBuilder request=new StringBuilder(command);for(String v:values)request.append('\t').append(ENC.encodeToString((v==null?"":v).getBytes(StandardCharsets.UTF_8)));
        try(Socket socket=new Socket()){socket.connect(new InetSocketAddress(HOST,PORT),4000);socket.setSoTimeout(12000);
            try(BufferedWriter w=new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(),StandardCharsets.UTF_8));BufferedReader r=new BufferedReader(new InputStreamReader(socket.getInputStream(),StandardCharsets.UTF_8))){w.write(request.toString());w.newLine();w.flush();String response=r.readLine();if(response==null)throw new IOException("Server closed the connection.");String[] parts=response.split("\\t",-1);if(parts.length!=2)throw new IOException("Unexpected server response.");String payload=new String(DEC.decode(parts[1]),StandardCharsets.UTF_8);if("ERROR".equals(parts[0]))throw new IOException(payload);if(!"OK".equals(parts[0]))throw new IOException("Unexpected server response.");return payload;}}
    }
    private static String message(Exception e){String m=e.getMessage();return m==null?"Cannot reach ChaFlow server.":m;}
    private static String str(Map<String,Object> m,String k){return Objects.toString(m.get(k),"");}
    private static long num(Map<String,Object> m,String k){return ((Number)m.get(k)).longValue();}
    public record AuthResult(boolean successful,long userId,String phone,String username,String displayName,String token,String message){static AuthResult failure(String message){return new AuthResult(false,-1,"","","","",message);}}
}
