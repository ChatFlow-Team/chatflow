package com.chaflow.server.repository;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/** Persistence port used by the chat application service. */
public interface ChatRepository {
    List<Map<String,Object>> searchUsers(long me, String query) throws SQLException;
    Map<String,Object> sendFriendRequest(long me, long target) throws SQLException;
    void respondFriendRequest(long me, long requestId, boolean accept) throws SQLException;
    List<Map<String,Object>> friendRequests(long me) throws SQLException;
    void removeFriend(long me, long other) throws SQLException;
    List<Map<String,Object>> friends(long me) throws SQLException;
    long openDirectConversation(long me, long other) throws SQLException;
    List<Map<String,Object>> conversations(long me) throws SQLException;
    List<Map<String,Object>> messages(long me, long conversation, int limit) throws SQLException;
    Map<String,Object> sendMessage(long me, long conversation, String content) throws SQLException;
}
