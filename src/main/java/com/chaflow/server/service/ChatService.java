package com.chaflow.server.service;

import com.chaflow.server.repository.ChatRepository;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Application service: validates use cases and delegates persistence to a repository. */
public final class ChatService {
    private final ChatRepository repository;
    public ChatService(ChatRepository repository) { this.repository = Objects.requireNonNull(repository); }

    public List<Map<String,Object>> searchUsers(long me,String query)throws SQLException {
        if(query==null || query.trim().length()<2) throw new IllegalArgumentException("Enter at least 2 characters to search.");
        return repository.searchUsers(me,query.trim());
    }
    public Map<String,Object> sendFriendRequest(long me,long target)throws SQLException {
        if(target<=0 || target==me) throw new IllegalArgumentException("You cannot add yourself.");
        return repository.sendFriendRequest(me,target);
    }
    public void respondFriendRequest(long me,long requestId,boolean accept)throws SQLException {
        if(requestId<=0) throw new IllegalArgumentException("Invalid friend request.");
        repository.respondFriendRequest(me,requestId,accept);
    }
    public List<Map<String,Object>> friendRequests(long me)throws SQLException{return repository.friendRequests(me);}
    public void removeFriend(long me,long other)throws SQLException {
        if(other<=0 || other==me) throw new IllegalArgumentException("Invalid friend.");
        repository.removeFriend(me,other);
    }
    public List<Map<String,Object>> friends(long me)throws SQLException{return repository.friends(me);}
    public long openDirectConversation(long me,long other)throws SQLException {
        if(other<=0 || other==me) throw new IllegalArgumentException("You cannot start a conversation with yourself.");
        return repository.openDirectConversation(me,other);
    }
    public List<Map<String,Object>> conversations(long me)throws SQLException{return repository.conversations(me);}
    public List<Map<String,Object>> messages(long me,long conversation,int limit)throws SQLException {
        if(conversation<=0) throw new IllegalArgumentException("Invalid conversation.");
        return repository.messages(me,conversation,limit);
    }
    public Map<String,Object> sendMessage(long me,long conversation,String content)throws SQLException {
        if(conversation<=0) throw new IllegalArgumentException("Invalid conversation.");
        if(content==null || content.trim().isEmpty() || content.trim().length()>4000)
            throw new IllegalArgumentException("Message must contain 1–4000 characters.");
        return repository.sendMessage(me,conversation,content);
    }
}
