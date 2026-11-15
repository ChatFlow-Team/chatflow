package com.chaflow.server.controller;

import com.chaflow.server.service.ChatService;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Socket-facing controller for user discovery, friends and conversations. */
public final class ChatController {
    private final ChatService service;
    public ChatController(ChatService service){this.service=Objects.requireNonNull(service);}
    public List<Map<String,Object>> searchUsers(long me,String q)throws SQLException{return service.searchUsers(me,q);}
    public Map<String,Object> sendFriendRequest(long me,long target)throws SQLException{return service.sendFriendRequest(me,target);}
    public void respondFriendRequest(long me,long id,boolean accept)throws SQLException{service.respondFriendRequest(me,id,accept);}
    public List<Map<String,Object>> friendRequests(long me)throws SQLException{return service.friendRequests(me);}
    public void removeFriend(long me,long other)throws SQLException{service.removeFriend(me,other);}
    public List<Map<String,Object>> friends(long me)throws SQLException{return service.friends(me);}
    public long openDirectConversation(long me,long other)throws SQLException{return service.openDirectConversation(me,other);}
    public List<Map<String,Object>> conversations(long me)throws SQLException{return service.conversations(me);}
    public List<Map<String,Object>> messages(long me,long conversation,int limit)throws SQLException{return service.messages(me,conversation,limit);}
    public Map<String,Object> sendMessage(long me,long conversation,String content)throws SQLException{return service.sendMessage(me,conversation,content);}
}
