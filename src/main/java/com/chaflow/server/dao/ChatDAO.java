package com.chaflow.server.dao;

import javax.sql.DataSource;

import java.sql.*;
import java.util.*;

/** Transactional application operations for discovery, friends and direct messaging. */
public final class ChatDAO {
    private final DataSource dataSource;
    public ChatDAO(DataSource dataSource) { this.dataSource = Objects.requireNonNull(dataSource, "dataSource"); }
    public List<Map<String,Object>> searchUsers(long me, String query) throws SQLException {
        String q = query == null ? "" : query.trim();
        if (q.length() < 2) throw new IllegalArgumentException("Enter at least 2 characters to search.");
        String sql = """
            SELECT u.id_utilisateur,u.pseudo,u.nom_affiche,u.numero_telephone,u.statut,
                   EXISTS(SELECT 1 FROM chatflow.friendship f WHERE f.user_low=LEAST(?,u.id_utilisateur) AND f.user_high=GREATEST(?,u.id_utilisateur)) AS is_friend,
                   (SELECT fr.status FROM chatflow.friend_request fr WHERE fr.user_low=LEAST(?,u.id_utilisateur) AND fr.user_high=GREATEST(?,u.id_utilisateur) AND fr.status='PENDING' LIMIT 1) AS pending_status,
                   (SELECT fr.requester_id FROM chatflow.friend_request fr WHERE fr.user_low=LEAST(?,u.id_utilisateur) AND fr.user_high=GREATEST(?,u.id_utilisateur) AND fr.status='PENDING' LIMIT 1) AS pending_requester
            FROM chatflow.utilisateur u
            WHERE u.id_utilisateur<>? AND (LOWER(u.pseudo) LIKE LOWER(?) OR LOWER(u.nom_affiche) LIKE LOWER(?) OR u.numero_telephone LIKE ?)
            ORDER BY u.nom_affiche LIMIT 30
            """;
        List<Map<String,Object>> out = new ArrayList<>();
        try (Connection c=dataSource.getConnection(); PreparedStatement p=c.prepareStatement(sql)) {
            for(int i=1;i<=6;i++) p.setLong(i,me); p.setLong(7,me);
            String like="%"+q+"%"; p.setString(8,like); p.setString(9,like); p.setString(10,like);
            try(ResultSet r=p.executeQuery()){while(r.next()){
                Map<String,Object> m=user(r); m.put("isFriend",r.getBoolean("is_friend"));
                m.put("pendingStatus",r.getString("pending_status"));
                long requester=r.getLong("pending_requester"); m.put("pendingRequester",r.wasNull()?null:requester); out.add(m);
            }}
        } return out;
    }

    public Map<String,Object> sendFriendRequest(long me,long target) throws SQLException {
        if(me==target) throw new IllegalArgumentException("You cannot add yourself.");
        try(Connection c=dataSource.getConnection()){
            c.setAutoCommit(false);
            try {
                if(friendshipExists(c,me,target)) throw new IllegalArgumentException("You are already friends.");
                try(PreparedStatement p=c.prepareStatement("SELECT id_request,requester_id,recipient_id FROM chatflow.friend_request WHERE user_low=LEAST(?,?) AND user_high=GREATEST(?,?) AND status='PENDING' FOR UPDATE")){
                    p.setLong(1,me);p.setLong(2,target);p.setLong(3,me);p.setLong(4,target);
                    try(ResultSet r=p.executeQuery()){if(r.next()){
                        long requester=r.getLong("requester_id"), id=r.getLong("id_request");
                        if(requester==target){acceptFriendRequest(c,id,me);c.commit();return Map.of("status","ACCEPTED","message","Incoming request accepted.");}
                        throw new IllegalArgumentException("A friend request is already pending.");
                    }}
                }
                try(PreparedStatement p=c.prepareStatement("INSERT INTO chatflow.friend_request(requester_id,recipient_id) VALUES(?,?) RETURNING id_request")){
                    p.setLong(1,me);p.setLong(2,target);try(ResultSet r=p.executeQuery()){r.next();long id=r.getLong(1);c.commit();return Map.of("status","PENDING","requestId",id,"message","Friend request sent.");}
                }
            }catch(Exception e){c.rollback();if(e instanceof SQLException s)throw s;if(e instanceof RuntimeException re)throw re;throw new SQLException(e);}
        }
    }

    public void respondFriendRequest(long me,long requestId,boolean accept) throws SQLException {
        try(Connection c=dataSource.getConnection()){
            c.setAutoCommit(false);
            try{acceptOrReject(c,me,requestId,accept);c.commit();}
            catch(Exception e){c.rollback();if(e instanceof SQLException s)throw s;if(e instanceof RuntimeException re)throw re;throw new SQLException(e);}
        }
    }
    private void acceptOrReject(Connection c,long me,long id,boolean accept)throws SQLException{
        try(PreparedStatement p=c.prepareStatement("SELECT requester_id,recipient_id FROM chatflow.friend_request WHERE id_request=? AND status='PENDING' FOR UPDATE")){
            p.setLong(1,id);try(ResultSet r=p.executeQuery()){
                if(!r.next()||r.getLong("recipient_id")!=me)throw new IllegalArgumentException("Friend request not found.");
                long from=r.getLong("requester_id");
                try(PreparedStatement u=c.prepareStatement("UPDATE chatflow.friend_request SET status=?,responded_at=CURRENT_TIMESTAMP WHERE id_request=?")){u.setString(1,accept?"ACCEPTED":"REJECTED");u.setLong(2,id);u.executeUpdate();}
                if(accept)try(PreparedStatement f=c.prepareStatement("INSERT INTO chatflow.friendship(user_low,user_high) VALUES(LEAST(?,?),GREATEST(?,?)) ON CONFLICT DO NOTHING")){f.setLong(1,me);f.setLong(2,from);f.setLong(3,me);f.setLong(4,from);f.executeUpdate();}
            }
        }
    }
    private void acceptFriendRequest(Connection c,long id,long me)throws SQLException{acceptOrReject(c,me,id,true);}

    public List<Map<String,Object>> friendRequests(long me) throws SQLException {
        String sql="SELECT fr.id_request,u.id_utilisateur,u.pseudo,u.nom_affiche,u.numero_telephone,fr.created_at FROM chatflow.friend_request fr JOIN chatflow.utilisateur u ON u.id_utilisateur=fr.requester_id WHERE fr.recipient_id=? AND fr.status='PENDING' ORDER BY fr.created_at DESC";
        List<Map<String,Object>> out=new ArrayList<>();try(Connection c=dataSource.getConnection();PreparedStatement p=c.prepareStatement(sql)){p.setLong(1,me);try(ResultSet r=p.executeQuery()){while(r.next()){Map<String,Object> m=new LinkedHashMap<>();m.put("requestId",r.getLong(1));m.put("id",r.getLong(2));m.put("username",r.getString(3));m.put("displayName",r.getString(4));m.put("phone",r.getString(5));m.put("createdAt",r.getTimestamp(6).toInstant().toString());out.add(m);}}}return out;
    }
    public void removeFriend(long me,long other) throws SQLException {
        if(me==other) throw new IllegalArgumentException("Invalid friend.");
        try(Connection c=dataSource.getConnection(); PreparedStatement p=c.prepareStatement(
                "DELETE FROM chatflow.friendship WHERE user_low=LEAST(?,?) AND user_high=GREATEST(?,?)")) {
            p.setLong(1,me);p.setLong(2,other);p.setLong(3,me);p.setLong(4,other);
            if(p.executeUpdate()==0) throw new IllegalArgumentException("Friendship not found.");
        }
    }

    public List<Map<String,Object>> friends(long me) throws SQLException {
        String sql="SELECT u.id_utilisateur,u.pseudo,u.nom_affiche,u.numero_telephone,u.statut FROM chatflow.friendship f JOIN chatflow.utilisateur u ON u.id_utilisateur=CASE WHEN f.user_low=? THEN f.user_high ELSE f.user_low END WHERE f.user_low=? OR f.user_high=? ORDER BY u.nom_affiche";
        List<Map<String,Object>> out=new ArrayList<>();try(Connection c=dataSource.getConnection();PreparedStatement p=c.prepareStatement(sql)){p.setLong(1,me);p.setLong(2,me);p.setLong(3,me);try(ResultSet r=p.executeQuery()){while(r.next())out.add(user(r));}}return out;
    }

    public long openDirectConversation(long me,long other) throws SQLException {
        if(me==other)throw new IllegalArgumentException("You cannot start a conversation with yourself.");
        try(Connection c=dataSource.getConnection()){
            c.setAutoCommit(false);
            try{
                try(PreparedStatement p=c.prepareStatement("SELECT 1 FROM chatflow.utilisateur WHERE id_utilisateur=?")){p.setLong(1,other);try(ResultSet r=p.executeQuery()){if(!r.next())throw new IllegalArgumentException("This ChatFlow account does not exist.");}}
                long low=Math.min(me,other),high=Math.max(me,other),conversationId;
                try(PreparedStatement p=c.prepareStatement("INSERT INTO chatflow.conversation AS existing(nature,direct_user_low,direct_user_high) VALUES('PRIVEE',?,?) ON CONFLICT (direct_user_low,direct_user_high) WHERE nature='PRIVEE' AND direct_user_low IS NOT NULL AND direct_user_high IS NOT NULL DO UPDATE SET derniere_activite=existing.derniere_activite RETURNING id_conversation")){
                    p.setLong(1,low);p.setLong(2,high);try(ResultSet r=p.executeQuery()){r.next();conversationId=r.getLong(1);}
                }
                addParticipant(c,me,conversationId);addParticipant(c,other,conversationId);c.commit();return conversationId;
            }catch(Exception e){c.rollback();if(e instanceof SQLException s)throw s;if(e instanceof RuntimeException re)throw re;throw new SQLException(e);}
        }
    }
    private void addParticipant(Connection c,long user,long conversation)throws SQLException{
        try(PreparedStatement p=c.prepareStatement("INSERT INTO chatflow.participant(id_utilisateur,id_conversation) VALUES(?,?) ON CONFLICT(id_utilisateur,id_conversation) DO UPDATE SET date_sortie=NULL")){p.setLong(1,user);p.setLong(2,conversation);p.executeUpdate();}
    }
    public List<Map<String,Object>> conversations(long me)throws SQLException{
        String sql="""
          SELECT c.id_conversation,c.nature,c.derniere_activite,u.id_utilisateur AS other_id,u.pseudo,u.nom_affiche,u.numero_telephone,
                 (SELECT m.contenu FROM chatflow.message m WHERE m.id_conversation=c.id_conversation AND m.supprime_le IS NULL ORDER BY m.envoye_le DESC,m.id_message DESC LIMIT 1) AS last_message,
                 (SELECT COUNT(*) FROM chatflow.message_destinataire md JOIN chatflow.message um ON um.id_message=md.id_message
                  WHERE md.id_destinataire=? AND um.id_conversation=c.id_conversation AND md.statut <> 'LU') AS unread_count
          FROM chatflow.participant mine JOIN chatflow.conversation c ON c.id_conversation=mine.id_conversation
          LEFT JOIN chatflow.participant op ON op.id_conversation=c.id_conversation AND op.id_utilisateur<>? AND op.date_sortie IS NULL
          LEFT JOIN chatflow.utilisateur u ON u.id_utilisateur=op.id_utilisateur
          WHERE mine.id_utilisateur=? AND mine.date_sortie IS NULL AND c.nature='PRIVEE'
          ORDER BY c.derniere_activite DESC,c.id_conversation DESC
          """;
        List<Map<String,Object>> out=new ArrayList<>();try(Connection c=dataSource.getConnection();PreparedStatement p=c.prepareStatement(sql)){p.setLong(1,me);p.setLong(2,me);p.setLong(3,me);try(ResultSet r=p.executeQuery()){while(r.next()){Map<String,Object> m=new LinkedHashMap<>();m.put("id",r.getLong("id_conversation"));m.put("nature",r.getString("nature"));m.put("lastActivity",r.getTimestamp("derniere_activite").toInstant().toString());m.put("otherId",r.getObject("other_id")==null?null:r.getLong("other_id"));m.put("username",r.getString("pseudo"));m.put("displayName",r.getString("nom_affiche"));m.put("phone",r.getString("numero_telephone"));m.put("lastMessage",r.getString("last_message"));m.put("unreadCount",r.getLong("unread_count"));out.add(m);}}}return out;
    }
    public List<Map<String,Object>> messages(long me,long conversation,int limit)throws SQLException{
        requireMember(me,conversation);
        try(Connection c=dataSource.getConnection();PreparedStatement p=c.prepareStatement("UPDATE chatflow.message_destinataire md SET statut='LU', recu_le=COALESCE(md.recu_le,CURRENT_TIMESTAMP), lu_le=CURRENT_TIMESTAMP FROM chatflow.message m WHERE md.id_message=m.id_message AND md.id_destinataire=? AND m.id_conversation=? AND md.statut<>'LU'")){p.setLong(1,me);p.setLong(2,conversation);p.executeUpdate();}
        String sql="SELECT m.id_message,m.id_expediteur,m.contenu,m.envoye_le,m.modifie_le,u.pseudo,u.nom_affiche FROM chatflow.message m JOIN chatflow.utilisateur u ON u.id_utilisateur=m.id_expediteur WHERE m.id_conversation=? AND m.supprime_le IS NULL ORDER BY m.envoye_le DESC,m.id_message DESC LIMIT ?";
        List<Map<String,Object>> out=new ArrayList<>();try(Connection c=dataSource.getConnection();PreparedStatement p=c.prepareStatement(sql)){p.setLong(1,conversation);p.setInt(2,Math.max(1,Math.min(limit,200)));try(ResultSet r=p.executeQuery()){while(r.next()){Map<String,Object> m=new LinkedHashMap<>();m.put("id",r.getLong(1));m.put("senderId",r.getLong(2));m.put("content",r.getString(3));m.put("sentAt",r.getTimestamp(4).toInstant().toString());m.put("editedAt",r.getTimestamp(5)==null?null:r.getTimestamp(5).toInstant().toString());m.put("username",r.getString(6));m.put("displayName",r.getString(7));out.add(m);}}}Collections.reverse(out);return out;
    }
    public Map<String,Object> sendMessage(long me,long conversation,String content)throws SQLException{
        String body=content==null?"":content.trim();if(body.isEmpty()||body.length()>4000)throw new IllegalArgumentException("Message must contain 1–4000 characters.");
        try(Connection c=dataSource.getConnection()){c.setAutoCommit(false);try{requireMember(c,me,conversation);long id;String sent;
            try(PreparedStatement p=c.prepareStatement("INSERT INTO chatflow.message(id_conversation,id_expediteur,contenu,type_message) VALUES(?,?,?,'TEXTE') RETURNING id_message,envoye_le")){p.setLong(1,conversation);p.setLong(2,me);p.setString(3,body);try(ResultSet r=p.executeQuery()){r.next();id=r.getLong(1);sent=r.getTimestamp(2).toInstant().toString();}}
            try(PreparedStatement p=c.prepareStatement("INSERT INTO chatflow.message_destinataire(id_message,id_destinataire) SELECT ?,id_utilisateur FROM chatflow.participant WHERE id_conversation=? AND id_utilisateur<>? AND date_sortie IS NULL")){p.setLong(1,id);p.setLong(2,conversation);p.setLong(3,me);p.executeUpdate();}
            try(PreparedStatement p=c.prepareStatement("UPDATE chatflow.conversation SET derniere_activite=CURRENT_TIMESTAMP WHERE id_conversation=?")){p.setLong(1,conversation);p.executeUpdate();}
            c.commit();return Map.of("id",id,"senderId",me,"content",body,"sentAt",sent);
        }catch(Exception e){c.rollback();if(e instanceof SQLException s)throw s;if(e instanceof RuntimeException re)throw re;throw new SQLException(e);}}}
    /** Returns only active conversation members, excluding the authenticated sender. */
    public List<Long> activeParticipantIds(long me, long conversation) throws SQLException {
        requireMember(me, conversation);
        String sql = "SELECT id_utilisateur FROM chatflow.participant WHERE id_conversation=? AND date_sortie IS NULL AND id_utilisateur<>? ORDER BY id_utilisateur";
        List<Long> ids = new ArrayList<>();
        try (Connection c = dataSource.getConnection(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setLong(1, conversation);
            p.setLong(2, me);
            try (ResultSet r = p.executeQuery()) {
                while (r.next()) ids.add(r.getLong(1));
            }
        }
        return ids;
    }

    private void requireMember(long me,long conv)throws SQLException{try(Connection c=dataSource.getConnection()){requireMember(c,me,conv);}}
    private void requireMember(Connection c,long me,long conv)throws SQLException{try(PreparedStatement p=c.prepareStatement("SELECT 1 FROM chatflow.participant WHERE id_utilisateur=? AND id_conversation=? AND date_sortie IS NULL")){p.setLong(1,me);p.setLong(2,conv);try(ResultSet r=p.executeQuery()){if(!r.next())throw new IllegalArgumentException("You are not a member of this conversation.");}}}
    private boolean friendshipExists(Connection c,long a,long b)throws SQLException{try(PreparedStatement p=c.prepareStatement("SELECT 1 FROM chatflow.friendship WHERE user_low=LEAST(?,?) AND user_high=GREATEST(?,?)")){p.setLong(1,a);p.setLong(2,b);p.setLong(3,a);p.setLong(4,b);try(ResultSet r=p.executeQuery()){return r.next();}}}
    private Map<String,Object> user(ResultSet r)throws SQLException{Map<String,Object> m=new LinkedHashMap<>();m.put("id",r.getLong("id_utilisateur"));m.put("username",r.getString("pseudo"));m.put("displayName",r.getString("nom_affiche"));m.put("phone",r.getString("numero_telephone"));m.put("status",r.getString("statut"));return m;}
}
