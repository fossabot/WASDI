package wasdi.shared.data;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.bson.Document;

import com.mongodb.Block;
import com.mongodb.client.FindIterable;
import com.mongodb.client.model.Filters;
import com.mongodb.client.result.DeleteResult;

import wasdi.shared.business.WorkspaceSharing;
import wasdi.shared.utils.Utils;

/**
 * Created by p.campanella on 25/10/2016.
 */
public class WorkspaceSharingRepository extends  MongoRepository{
	
	public WorkspaceSharingRepository() {
		m_sThisCollection = "workspacessharing";
	}

	/**
	 * Insert a New Workspace sharing
	 * @param oWorkspaceSharing
	 * @return
	 */
    public boolean insertWorkspaceSharing(WorkspaceSharing oWorkspaceSharing) {

        try {
            String sJSON = s_oMapper.writeValueAsString(oWorkspaceSharing);
            getCollection(m_sThisCollection).insertOne(Document.parse(sJSON));

            return true;

        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return false;
    }
    
    /**
     * Get all the workspaces shared by this owner User 
     * @param sUserId
     * @return
     */
    public List<WorkspaceSharing> getWorkspaceSharingByOwner(String sUserId) {

        final ArrayList<WorkspaceSharing> aoReturnList = new ArrayList<WorkspaceSharing>();
        try {

            FindIterable<Document> oWSDocuments = getCollection(m_sThisCollection).find(new Document("ownerId", sUserId));

            oWSDocuments.forEach(new Block<Document>() {
                public void apply(Document document) {
                    String sJSON = document.toJson();
                    WorkspaceSharing oWorkspaceSharing = null;
                    try {
                        oWorkspaceSharing = s_oMapper.readValue(sJSON,WorkspaceSharing.class);
                        aoReturnList.add(oWorkspaceSharing);
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
     * Get all the workspaces shared with this User
     * @param sUserId
     * @return
     */
    public List<WorkspaceSharing> getWorkspaceSharingByUser(String sUserId) {

        final ArrayList<WorkspaceSharing> aoReturnList = new ArrayList<WorkspaceSharing>();
        try {

            FindIterable<Document> oWSDocuments = getCollection(m_sThisCollection).find(new Document("userId", sUserId));

            oWSDocuments.forEach(new Block<Document>() {
                public void apply(Document document) {
                    String sJSON = document.toJson();
                    WorkspaceSharing oWorkspaceSharing = null;
                    try {
                        oWorkspaceSharing = s_oMapper.readValue(sJSON,WorkspaceSharing.class);
                        aoReturnList.add(oWorkspaceSharing);
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
     * Get all the sharings of this workspace
     * @param sWorkspaceId
     * @return
     */
    public List<WorkspaceSharing> getWorkspaceSharingByWorkspace(String sWorkspaceId) {

        final ArrayList<WorkspaceSharing> aoReturnList = new ArrayList<WorkspaceSharing>();
        try {

            FindIterable<Document> oWSDocuments = getCollection(m_sThisCollection).find(new Document("workspaceId", sWorkspaceId));

            oWSDocuments.forEach(new Block<Document>() {
                public void apply(Document document) {
                    String sJSON = document.toJson();
                    WorkspaceSharing oWorkspaceSharing = null;
                    try {
                        oWorkspaceSharing = s_oMapper.readValue(sJSON,WorkspaceSharing.class);
                        aoReturnList.add(oWorkspaceSharing);
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
     * Get all the sharings 
     * @param sWorkspaceId
     * @return
     */
    public List<WorkspaceSharing> getWorkspaceSharings() {

        final ArrayList<WorkspaceSharing> aoReturnList = new ArrayList<WorkspaceSharing>();
        try {

            FindIterable<Document> oWSDocuments = getCollection(m_sThisCollection).find();

            oWSDocuments.forEach(new Block<Document>() {
                public void apply(Document document) {
                    String sJSON = document.toJson();
                    WorkspaceSharing oWorkspaceSharing = null;
                    try {
                        oWorkspaceSharing = s_oMapper.readValue(sJSON,WorkspaceSharing.class);
                        aoReturnList.add(oWorkspaceSharing);
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
     * Delete all the sharings of a specific Workspace
     * @param sWorkspaceId
     * @return
     */
    public int deleteByWorkspaceId(String sWorkspaceId) {

        try {

            DeleteResult oDeleteResult = getCollection(m_sThisCollection).deleteMany(new Document("workspaceId", sWorkspaceId));

            if (oDeleteResult != null)
            {
                return  (int) oDeleteResult.getDeletedCount();
            }

        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return 0;
    }
    
    /**
     * Delete all the sharings with User
     * @param sUserId
     * @return
     */
    public int deleteByUserId(String sUserId) {

        try {

            DeleteResult oDeleteResult = getCollection(m_sThisCollection).deleteMany(new Document("userId", sUserId));

            if (oDeleteResult != null)
            {
                return  (int) oDeleteResult.getDeletedCount();
            }

        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return 0;
    }
    
    /**
     * Delete a specific Sharing of this workspace with this user
     * @param sUserId
     * @param sWorkspaceId
     * @return
     */
    public int deleteByUserIdWorkspaceId(String sUserId, String sWorkspaceId) {
        try {

            DeleteResult oDeleteResult = getCollection(m_sThisCollection).deleteMany(Filters.and(Filters.eq("userId", sUserId), Filters.eq("workspaceId", sWorkspaceId)));

            if (oDeleteResult != null)
            {
                return  (int) oDeleteResult.getDeletedCount();
            }

        } 
        catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return 0;
    }
    
    /**
     * Checks if workspace is shared with user
     * @param sUserId
     * @param sWorkspaceId
     * @return
     */
    public boolean isSharedWithUser(String sUserId, String sWorkspaceId) {
    	try {
    		Document oWSDocument = getCollection(m_sThisCollection).find(
    				Filters.and(
    						Filters.eq("userId", sUserId),
    						Filters.eq("workspaceId", sWorkspaceId)
    						)
    		).first();
    		if(null!=oWSDocument) {
    			return true;
    		}
    		
    	} catch (Exception oE) {
			Utils.debugLog("WorkspaceSharingRepository.isSharedWithUser( " + sUserId + ", " + sWorkspaceId + "): error: " + oE);
		}
    	return false;
    }
}
