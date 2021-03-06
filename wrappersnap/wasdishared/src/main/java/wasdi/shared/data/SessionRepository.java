package wasdi.shared.data;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.bson.Document;

import com.mongodb.Block;
import com.mongodb.client.FindIterable;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.UpdateResult;

import wasdi.shared.business.UserSession;
import wasdi.shared.utils.Utils;

/**
 * Created by p.campanella on 21/10/2016.
 */
public class SessionRepository extends MongoRepository {
	
	public SessionRepository() {
		m_sThisCollection = "sessions";
	}
	
	/**
	 * Create a new session
	 * @param oSession
	 * @return
	 */
    public boolean insertSession(UserSession oSession) {
        try {
            String sJSON = s_oMapper.writeValueAsString(oSession);
            getCollection(m_sThisCollection).insertOne(Document.parse(sJSON));

            return true;

        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return false;
    }
    
    /**
     * Get a session by Id
     * @param sSessionId
     * @return
     */
    public UserSession getSession(String sSessionId) {
        try {
            Document oSessionDocument = getCollection(m_sThisCollection).find(new Document("sessionId", sSessionId)).first();

            if (oSessionDocument != null) {
                String sJSON = oSessionDocument.toJson();

                UserSession oUserSession = s_oMapper.readValue(sJSON, UserSession.class);
                return oUserSession;
            }

        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return  null;
    }
    
    /**
     * Get all the active sessions of a user
     * @param sUserId
     * @return
     */
    public List<UserSession> getAllActiveSessions(String sUserId) {
        final ArrayList<UserSession> aoReturnList = new ArrayList<>();
        try {
            long lNow = new Date().getTime();
            FindIterable<Document> oWSDocuments = getCollection(m_sThisCollection).find(Filters.and(Filters.gte("lastTouch", lNow - 24*60*60*1000), Filters.eq("userId", sUserId)));

            oWSDocuments.forEach(new Block<Document>() {
                public void apply(Document document) {
                    String sJSON = document.toJson();
                    UserSession oUserSession = null;
                    try {
                        oUserSession = s_oMapper.readValue(sJSON, UserSession.class);
                        aoReturnList.add(oUserSession);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }
            });
        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return aoReturnList;
    }
    
    /**
     * Get all the expired sessions of a user
     * @param sUserId
     * @return
     */
    public List<UserSession> getAllExpiredSessions(String sUserId) {
        final ArrayList<UserSession> aoReturnList = new ArrayList<>();
        try {
            long lNow = new Date().getTime();
            FindIterable<Document> oWSDocuments = getCollection(m_sThisCollection).find(Filters.and(Filters.lt("lastTouch", lNow - 24*60*60*1000), Filters.eq("userId", sUserId)));

            oWSDocuments.forEach(new Block<Document>() {
                public void apply(Document document) {
                    String sJSON = document.toJson();
                    UserSession oUserSession = null;
                    try {
                        oUserSession = s_oMapper.readValue(sJSON, UserSession.class);
                        aoReturnList.add(oUserSession);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }
            });
        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return aoReturnList;
    }
    
    /**
     * Refresh a session
     * @param oSession
     * @return
     */
    public boolean touchSession(UserSession oSession) {
        try {
            UpdateResult oResult = getCollection(m_sThisCollection).updateOne(Filters.eq("sessionId",oSession.getSessionId()), Updates.set("lastTouch", (double)new Date().getTime()));

            if (oResult.getModifiedCount()==1) return  true;
        }
        catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return  false;
    }
    
    /**
     * Delete a Session
     * @param oSession
     * @return
     */
    public boolean deleteSession(UserSession oSession) {
        try {
            if (oSession == null || Utils.isNullOrEmpty(oSession.getSessionId()))
                return true;
            getCollection(m_sThisCollection).deleteOne(new Document("sessionId", oSession.getSessionId()));
            return true;

        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return false;
    }
}
