package com.chaflow.server.repository;

import com.chaflow.server.dao.ChatDAO;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** JDBC repository adapter. SQL is confined to DAO classes. */
public final class JdbcChatRepository implements ChatRepository {
    private final ChatDAO dao;
    public JdbcChatRepository(ChatDAO dao) { this.dao = Objects.requireNonNull(dao); }
    public List<Map<String,Object>> searchUsers(long me,String q)throws SQLException{return dao.searchUsers(me,q);}
    public Map<String,Object> sendFriendRequest(long me,long target)throws SQLException{return dao.sendFriendRequest(me,target);}
    public void respondFriendRequest(long me,long id,boolean accept)throws SQLException{dao.respondFriendRequest(me,id,accept);}
    public List<Map<String,Object>> friendRequests(long me)throws SQLException{return dao.friendRequests(me);}
    public void removeFriend(long me,long other)throws SQLException{dao.removeFriend(me,other);}
    public List<Map<String,Object>> friends(long me)throws SQLException{return dao.friends(me);}
    public long openDirectConversation(long me,long other)throws SQLException{return dao.openDirectConversation(me,other);}
    public List<Map<String,Object>> conversations(long me)throws SQLException{return dao.conversations(me);}
    public List<Map<String,Object>> messages(long me,long conversation,int limit)throws SQLException{return dao.messages(me,conversation,limit);}
    public Map<String,Object> sendMessage(long me,long conversation,String content)throws SQLException{return dao.sendMessage(me,conversation,content);}
    public List<Long> activeParticipantIds(long me,long conversation)throws SQLException{return dao.activeParticipantIds(me,conversation);}
}
