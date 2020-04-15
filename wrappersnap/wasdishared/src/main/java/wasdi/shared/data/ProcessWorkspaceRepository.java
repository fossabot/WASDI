package wasdi.shared.data;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;

import com.mongodb.BasicDBObject;
import com.mongodb.Block;
import com.mongodb.client.FindIterable;
import com.mongodb.client.model.Filters;

import wasdi.shared.LauncherOperations;
import wasdi.shared.business.ProcessStatus;
import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.utils.Utils;

/**
 * Created by s.adamo on 31/01/2017.
 */
/**
 * @author s.adamo
 * @author c.nattero
 *
 */
public class ProcessWorkspaceRepository extends MongoRepository {

	/**
	 * Insert a new Process Workspace 
	 * @param oProcessWorkspace Process Workpsace to insert
	 * @return Mongo obj id
	 */
    public String insertProcessWorkspace(ProcessWorkspace oProcessWorkspace) {

        try {
        	
        	Utils.debugLog("Inserting Process " + oProcessWorkspace.getProcessObjId() + " - status: " + oProcessWorkspace.getStatus());
        	
        	// Initialize the Last State Change Date
        	oProcessWorkspace.setLastStateChangeDate(Utils.GetFormatDate(new Date()));
        	
            String sJSON = s_oMapper.writeValueAsString(oProcessWorkspace);
            Document oDocument = Document.parse(sJSON);
            getCollection("processworkpsace").insertOne(oDocument);
            return oDocument.getObjectId("_id").toHexString();

        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return "";
    }

    /**
     * Delete an Entity by Mongo Id
     * @param sId Process Workspace Mongo Id
     * @return
     */
    public boolean deleteProcessWorkspace(String sId) {

        try {
            getCollection("processworkpsace").deleteOne(new Document("_id", new ObjectId(sId)));

            return true;

        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return false;
    }

    /**
     * Delete Process Workspace by PID
     * @param iPid
     * @return
     */
    public boolean deleteProcessWorkspaceByPid(int iPid) {

        try {
            getCollection("processworkpsace").deleteOne(new Document("pid", iPid));

            return true;

        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return false;
    }

    /**
     * Delete Process Workspace by WASDI ID
     * @param sProcessObjId
     * @return
     */
    public boolean deleteProcessWorkspaceByProcessObjId(String sProcessObjId) {

        try {
            getCollection("processworkpsace").deleteOne(new Document("processObjId", sProcessObjId));

            return true;

        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return false;
    }    

    /**
     * Get List of Process Workspaces in a Workspace
     * @param sWorkspaceId unique id of the workspace
     * @param eStatus the status of the process
     * @param eOperation the type of Launcher Operation
     * @param oDateFRom starting date (included)
     * @param oDateTo ending date (included)
     * @return list of results
     */
    public List<ProcessWorkspace> getProcessByWorkspace(String sWorkspaceId) {

        return getProcessByWorkspace(sWorkspaceId, null, null, null, null, null);
    }
    
    /**
     * Get List of Process Workspaces in a Workspace
     * @param sWorkspaceId
     * @param eStatus
     * @return list of results
     */
    public List<ProcessWorkspace> getProcessByWorkspace(String sWorkspaceId, ProcessStatus eStatus, LauncherOperations eOperation, String sProductNameSubstring, Instant oDateFRom, Instant oDateTo) {

        final ArrayList<ProcessWorkspace> aoReturnList = new ArrayList<ProcessWorkspace>();
        try {

        	Bson oFilter = buildFilter(sWorkspaceId, eStatus, eOperation, sProductNameSubstring, oDateFRom, oDateTo);
        	FindIterable<Document> oWSDocuments = getCollection("processworkpsace").find(oFilter)
            		.sort(new Document("operationDate", -1));
            fillList(aoReturnList, oWSDocuments);

        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return aoReturnList;
    }
    
    /**
     * Get paginated list of process workspace in a workspace
     * @param sWorkspaceId unique id of the workspace
     * @param iStartIndex start index for pagination
     * @param iEndIndex end index for pagination
     * @return list of results
     */
    public List<ProcessWorkspace> getProcessByWorkspace(String sWorkspaceId, int iStartIndex, int iEndIndex) {

        return getProcessByWorkspace(sWorkspaceId, null, null, null, null, null, iStartIndex, iEndIndex);
    }
    
    /**
     * Get paginated list of process workspace in a workspace
     * @param sWorkspaceId unique id of the workspace
     * @param eStatus the status of the process
     * @param eOperation the type of Launcher Operation
     * @param oDateFRom starting date (included)
     * @param oDateTo ending date (included)
     * @param iStartIndex start index for pagination
     * @param iEndIndex end index for pagination
     * @return list of results
     */
    public List<ProcessWorkspace> getProcessByWorkspace(String sWorkspaceId, ProcessStatus eStatus, LauncherOperations eOperation, String sProductNameSubstring, Instant oDateFRom, Instant oDateTo, int iStartIndex, int iEndIndex) {

        final ArrayList<ProcessWorkspace> aoReturnList = new ArrayList<ProcessWorkspace>();
        try {

        	Bson oFilter = buildFilter(sWorkspaceId, eStatus, eOperation, sProductNameSubstring, oDateFRom, oDateTo);
        	
        	FindIterable<Document> oWSDocuments = getCollection("processworkpsace").find(oFilter)
            		.sort(new Document("operationDate", -1))
            		.skip(iStartIndex)
            		.limit(iEndIndex-iStartIndex);
        	
            fillList(aoReturnList, oWSDocuments);
        	

        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return aoReturnList;
    }
    

	/**
	 * @param sWorkspaceId
	 * @param eStatus
	 * @param eOperation
	 * @param oDateFRom
	 * @param oDateTo
	 * @return
	 */
	private Bson buildFilter(String sWorkspaceId, ProcessStatus eStatus, LauncherOperations eOperation,
			String sProductNameSubstring,
			Instant oDateFRom, Instant oDateTo) {
		Bson oFilter = Filters.eq("workspaceId", sWorkspaceId);
		if(null!=eStatus) {
			Bson oCond = Filters.eq("status", eStatus.name());
			oFilter = Filters.and(oFilter, oCond);
		}
		if(null!=eOperation) {
			Bson oCond = Filters.eq("operationType", eOperation.name());
			oFilter = Filters.and(oFilter, oCond);
		}
		if(!Utils.isNullOrEmpty(sProductNameSubstring)) {
			//Bson oCond = Filters.regex("productName", Pattern.quote(sProductNameSubstring));
			//Bson oCond = Filters.regex("productName", sProductNameSubstring);
			//Bson oCond = Filters.eq("productName", sProductNameSubstring);

			Pattern regex = Pattern.compile(sProductNameSubstring);
			Bson oCond = Filters.eq("productName", regex);
			oFilter = Filters.and(oFilter, oCond);
		}
		if(null!=oDateFRom) {
			Bson oCond = Filters.gte("operationDate", oDateFRom);
			oFilter = Filters.and(oFilter, oCond);
		}
		if(null!=oDateTo) {
			Bson oCond = Filters.lte("operationDate", oDateFRom);
			oFilter = Filters.and(oFilter, oCond);
		}
		return oFilter;
	}
    
	
	
	/**
	 * @param sProcessId the process id
	 * @return process status as a string, null if the process is not found
	 */
	public String getProcessStatusFromId(String sProcessId) {
		Document oDocument = getCollection("processworkpsace").find(Filters.eq("processObjId", sProcessId)).first();
		String sJson = oDocument.toJson();
		try {
			ProcessWorkspace oProcessWorkspace = s_oMapper.readValue(sJson, ProcessWorkspace.class);
			return oProcessWorkspace.getStatus();
		} catch (Exception oE) {
			Utils.debugLog("ProcessWorkspaceRepository.getProcessStatusFromId( " + sProcessId + " ): " + oE );
		}
		return null;
	}
	
    /**
     * Get the total count of pw in a workspace
     * @param sWorkspaceId
     * @return
     */
    public long countByWorkspace(String sWorkspaceId) {
    	try {
    		long lCount = getCollection("processworkpsace").count(new Document("workspaceId", sWorkspaceId));
    		return lCount;
    	}
    	catch (Exception oEx) {
    		oEx.printStackTrace();
		}
    	
    	return 0;
    }
    
    /**
     * Get the list of process workspace for a user
     * @param sUserId
     * @return
     */
    public List<ProcessWorkspace> getProcessByUser(String sUserId) {

        final ArrayList<ProcessWorkspace> aoReturnList = new ArrayList<ProcessWorkspace>();
        try {

            FindIterable<Document> oWSDocuments = getCollection("processworkpsace").find(new Document("userId", sUserId)).sort(new Document("operationDate", -1));
            fillList(aoReturnList, oWSDocuments);

        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return aoReturnList;
    }

    
    /**
     * Get the list of created processes NOT Download or IDL
     * @return
     */
    public List<ProcessWorkspace> getCreatedProcesses() {

        final ArrayList<ProcessWorkspace> aoReturnList = new ArrayList<ProcessWorkspace>();
        try {

            FindIterable<Document> oWSDocuments = getCollection("processworkpsace").find(
            		Filters.and(
            				Filters.eq("status", ProcessStatus.CREATED.name()),
            				Filters.not(Filters.eq("operationType", LauncherOperations.DOWNLOAD.name())),
            				Filters.not(Filters.eq("operationType", LauncherOperations.RUNIDL.name()))
            				)
            		)
            		.sort(new Document("operationDate", -1));
            fillList(aoReturnList, oWSDocuments);

        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return aoReturnList;
    }
    
    /**
     * Get the list of created processes NOT Download or IDL in a NODE
     * @param sComputingNodeCode
     * @return
     */
    public List<ProcessWorkspace> getCreatedProcessesByNode(String sComputingNodeCode) {

        final ArrayList<ProcessWorkspace> aoReturnList = new ArrayList<ProcessWorkspace>();
        try {

            FindIterable<Document> oWSDocuments = getCollection("processworkpsace").find(
            		Filters.and(
            				Filters.eq("status", ProcessStatus.CREATED.name()),
            				Filters.not(Filters.eq("operationType", LauncherOperations.DOWNLOAD.name())),
            				Filters.not(Filters.eq("operationType", LauncherOperations.RUNIDL.name())),
            				Filters.eq("nodeCode", sComputingNodeCode)
            				)
            		)
            		.sort(new Document("operationDate", -1));
            fillList(aoReturnList, oWSDocuments);

        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return aoReturnList;
    }
    
    /**
     * Get the list of processes not Download or IDL in ready state for a specific node
     * @param sComputingNodeCode
     * @return
     */
    public List<ProcessWorkspace> getReadyProcessesByNode(String sComputingNodeCode) {

        final ArrayList<ProcessWorkspace> aoReturnList = new ArrayList<ProcessWorkspace>();
        try {

            FindIterable<Document> oWSDocuments = getCollection("processworkpsace").find(
            		Filters.and(
            				Filters.eq("status", ProcessStatus.READY.name()),
            				Filters.not(Filters.eq("operationType", LauncherOperations.DOWNLOAD.name())),
            				Filters.not(Filters.eq("operationType", LauncherOperations.RUNIDL.name())),
            				Filters.eq("nodeCode", sComputingNodeCode)
            				)
            		)
            		.sort(new Document("lastStateChangeDate", -1));
            fillList(aoReturnList, oWSDocuments);

        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return aoReturnList;
    }
    
    /**
     * Get the list of processes not download or idl in running state
     * @return
     */
    public List<ProcessWorkspace> getRunningProcesses() {

        final ArrayList<ProcessWorkspace> aoReturnList = new ArrayList<ProcessWorkspace>();
        try {

            FindIterable<Document> oWSDocuments = getCollection("processworkpsace").find(
            		Filters.and(
            				Filters.eq("status", ProcessStatus.RUNNING.name()),
            				Filters.not(Filters.eq("operationType", LauncherOperations.DOWNLOAD.name())),
            				Filters.not(Filters.eq("operationType", LauncherOperations.RUNIDL.name()))
            				)
            		)
            		.sort(new Document("operationDate", -1));
            fillList(aoReturnList, oWSDocuments);

        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return aoReturnList;
    }
    
    /**
     * Get the list of processes not download or idl in running state in a specific node
     * @param sComputingNodeCode
     * @return
     */
    public List<ProcessWorkspace> getRunningProcessesByNode(String sComputingNodeCode) {

        final ArrayList<ProcessWorkspace> aoReturnList = new ArrayList<ProcessWorkspace>();
        try {

            FindIterable<Document> oWSDocuments = getCollection("processworkpsace").find(
            		Filters.and(
            				Filters.eq("status", ProcessStatus.RUNNING.name()),
            				Filters.not(Filters.eq("operationType", LauncherOperations.DOWNLOAD.name())),
            				Filters.not(Filters.eq("operationType", LauncherOperations.RUNIDL.name())),
            				Filters.eq("nodeCode", sComputingNodeCode)
            				)
            		)
            		.sort(new Document("operationDate", -1));
            fillList(aoReturnList, oWSDocuments);

        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return aoReturnList;
    }    
    
    /**
     * Get the list of created download processes
     * @return
     */
    public List<ProcessWorkspace> getCreatedDownloads() {

        final ArrayList<ProcessWorkspace> aoReturnList = new ArrayList<ProcessWorkspace>();
        try {

            FindIterable<Document> oWSDocuments = getCollection("processworkpsace").find(
            		Filters.and(
            				Filters.eq("status", ProcessStatus.CREATED.name()),
            				Filters.eq("operationType", LauncherOperations.DOWNLOAD.name())
            				)
            		)
            		.sort(new Document("operationDate", -1));
            fillList(aoReturnList, oWSDocuments);

        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return aoReturnList;
    }
    
    /**
     * Get the list of created download processes in specific node
     * @param sComputingNodeCode
     * @return
     */
    public List<ProcessWorkspace> getCreatedDownloadsByNode(String sComputingNodeCode) {

        final ArrayList<ProcessWorkspace> aoReturnList = new ArrayList<ProcessWorkspace>();
        try {

            FindIterable<Document> oWSDocuments = getCollection("processworkpsace").find(
            		Filters.and(
            				Filters.eq("status", ProcessStatus.CREATED.name()),
            				Filters.eq("operationType", LauncherOperations.DOWNLOAD.name()),
            				Filters.eq("nodeCode", sComputingNodeCode)
            				)
            		)
            		.sort(new Document("operationDate", -1));
            fillList(aoReturnList, oWSDocuments);

        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return aoReturnList;
    }
    
    /**
     * Get the list of created idl processes
     * @return
     */
    public List<ProcessWorkspace> getCreatedIDL() {

        final ArrayList<ProcessWorkspace> aoReturnList = new ArrayList<ProcessWorkspace>();
        try {

            FindIterable<Document> oWSDocuments = getCollection("processworkpsace").find(
            		Filters.and(
            				Filters.eq("status", ProcessStatus.CREATED.name()),
            				Filters.eq("operationType", LauncherOperations.RUNIDL.name())
            				)
            		)
            		.sort(new Document("operationDate", -1));
            fillList(aoReturnList, oWSDocuments);

        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return aoReturnList;
    }

    /**
     * Get the list of created idl processes in a specific node
     * @param sComputingNode
     * @return
     */
    public List<ProcessWorkspace> getCreatedIDLByNode(String sComputingNode) {

        final ArrayList<ProcessWorkspace> aoReturnList = new ArrayList<ProcessWorkspace>();
        try {

            FindIterable<Document> oWSDocuments = getCollection("processworkpsace").find(
            		Filters.and(
            				Filters.eq("status", ProcessStatus.CREATED.name()),
            				Filters.eq("operationType", LauncherOperations.RUNIDL.name()),
            				Filters.eq("nodeCode", sComputingNode)
            				)
            		)
            		.sort(new Document("operationDate", -1));
            fillList(aoReturnList, oWSDocuments);

        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return aoReturnList;
    }
    
    /**
     * Get the list of running download processes
     * @return
     */
    public List<ProcessWorkspace> getRunningDownloads() {

        final ArrayList<ProcessWorkspace> aoReturnList = new ArrayList<ProcessWorkspace>();
        try {

            FindIterable<Document> oWSDocuments = getCollection("processworkpsace").find(
            		Filters.and(
            				Filters.eq("status", ProcessStatus.RUNNING.name()),
            				Filters.eq("operationType", LauncherOperations.DOWNLOAD.name())
            				)
            		)
            		.sort(new Document("operationDate", -1));
            fillList(aoReturnList, oWSDocuments);

        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return aoReturnList;
    }
    
    /**
     * Get the list of running download processes in a specific node
     * @param sComputingNode
     * @return
     */
    public List<ProcessWorkspace> getRunningDownloadsByNode(String sComputingNode) {

        final ArrayList<ProcessWorkspace> aoReturnList = new ArrayList<ProcessWorkspace>();
        try {

            FindIterable<Document> oWSDocuments = getCollection("processworkpsace").find(
            		Filters.and(
            				Filters.eq("status", ProcessStatus.RUNNING.name()),
            				Filters.eq("operationType", LauncherOperations.DOWNLOAD.name()),
            				Filters.eq("nodeCode", sComputingNode)
            				)
            		)
            		.sort(new Document("operationDate", -1));
            fillList(aoReturnList, oWSDocuments);

        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return aoReturnList;
    }
    
    /**
     * Get the list of ready download processes
     * @return
     */
    public List<ProcessWorkspace> getReadyDownloads() {

        final ArrayList<ProcessWorkspace> aoReturnList = new ArrayList<ProcessWorkspace>();
        try {

            FindIterable<Document> oWSDocuments = getCollection("processworkpsace").find(
            		Filters.and(
            				Filters.eq("status", ProcessStatus.READY.name()),
            				Filters.eq("operationType", LauncherOperations.DOWNLOAD.name())
            				)
            		)
            		.sort(new Document("lastStateChangeDate", -1));
            fillList(aoReturnList, oWSDocuments);

        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return aoReturnList;
    }
    
    /**
     * Get the list of ready download processes in a specific node
     * @param sComputingNode
     * @return
     */
    public List<ProcessWorkspace> getReadyDownloadsByNode(String sComputingNode) {

        final ArrayList<ProcessWorkspace> aoReturnList = new ArrayList<ProcessWorkspace>();
        try {

            FindIterable<Document> oWSDocuments = getCollection("processworkpsace").find(
            		Filters.and(
            				Filters.eq("status", ProcessStatus.READY.name()),
            				Filters.eq("operationType", LauncherOperations.DOWNLOAD.name()),
            				Filters.eq("nodeCode", sComputingNode)
            				)
            		)
            		.sort(new Document("lastStateChangeDate", -1));
            fillList(aoReturnList, oWSDocuments);

        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return aoReturnList;
    }
    
    
    /**
     * Get the list of running IDL
     * @return
     */
    public List<ProcessWorkspace> getRunningIDL() {

        final ArrayList<ProcessWorkspace> aoReturnList = new ArrayList<ProcessWorkspace>();
        try {

            FindIterable<Document> oWSDocuments = getCollection("processworkpsace").find(
            		Filters.and(
            				Filters.eq("status", ProcessStatus.RUNNING.name()),
            				Filters.eq("operationType", LauncherOperations.RUNIDL.name())
            				)
            		)
            		.sort(new Document("operationDate", -1));
            fillList(aoReturnList, oWSDocuments);

        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return aoReturnList;
    }
    
    /**
     * Get the list of running IDL in a specific node
     * @param sComputingNode
     * @return
     */
    public List<ProcessWorkspace> getRunningIDLByNode(String sComputingNode) {

        final ArrayList<ProcessWorkspace> aoReturnList = new ArrayList<ProcessWorkspace>();
        try {

            FindIterable<Document> oWSDocuments = getCollection("processworkpsace").find(
            		Filters.and(
            				Filters.eq("status", ProcessStatus.RUNNING.name()),
            				Filters.eq("operationType", LauncherOperations.RUNIDL.name()),
            				Filters.eq("nodeCode", sComputingNode)
            				)
            		)
            		.sort(new Document("operationDate", -1));
            fillList(aoReturnList, oWSDocuments);

        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return aoReturnList;
    }

    /**
     * Get the list of ready IDL
     * @return
     */
    public List<ProcessWorkspace> getReadyIDL() {

        final ArrayList<ProcessWorkspace> aoReturnList = new ArrayList<ProcessWorkspace>();
        try {

            FindIterable<Document> oWSDocuments = getCollection("processworkpsace").find(
            		Filters.and(
            				Filters.eq("status", ProcessStatus.READY.name()),
            				Filters.eq("operationType", LauncherOperations.RUNIDL.name())
            				)
            		)
            		.sort(new Document("lastStateChangeDate", -1));
            fillList(aoReturnList, oWSDocuments);

        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return aoReturnList;
    }
    
    /**
     * Get the list of ready IDL in a specific node
     * @param sComputingNode
     * @return
     */
    public List<ProcessWorkspace> getReadyIDLByNode(String sComputingNode) {

        final ArrayList<ProcessWorkspace> aoReturnList = new ArrayList<ProcessWorkspace>();
        try {

            FindIterable<Document> oWSDocuments = getCollection("processworkpsace").find(
            		Filters.and(
            				Filters.eq("status", ProcessStatus.READY.name()),
            				Filters.eq("operationType", LauncherOperations.RUNIDL.name()),
            				Filters.eq("nodeCode", sComputingNode)
            				)
            		)
            		.sort(new Document("lastStateChangeDate", -1));
            fillList(aoReturnList, oWSDocuments);

        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return aoReturnList;
    }
    
    
    /**
     * Get the list of processes in a specific state in a specific node
     * @param sProcessStatus status of the process
     * @param sComputingNodeCode computing node
     * @return
     */    
    public List<ProcessWorkspace> getProcessesByStateNode(String sProcessStatus, String sComputingNodeCode) {
    	return getProcessesByStateNode(sProcessStatus, sComputingNodeCode, "operationDate");
    }
    
    
    
    /**
     * Get the list of processes in a specific state in a specific node
     * @param sProcessStatus status of the process
     * @param sComputingNodeCode computing node
     * @return
     */        
    public List<ProcessWorkspace> getProcessesByStateNode(String sProcessStatus, String sComputingNodeCode, String sOrderBy) {

        final ArrayList<ProcessWorkspace> aoReturnList = new ArrayList<ProcessWorkspace>();
        try {

            FindIterable<Document> oWSDocuments = getCollection("processworkpsace").find(
            		Filters.and(
            				Filters.eq("status", sProcessStatus),
            				Filters.eq("nodeCode", sComputingNodeCode)
            				)
            		)
            		.sort(new Document(sOrderBy, -1));
            fillList(aoReturnList, oWSDocuments);

        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return aoReturnList;
    }    
    
    
    
    
	private void fillList(final ArrayList<ProcessWorkspace> aoReturnList, FindIterable<Document> oWSDocuments) {
		oWSDocuments.forEach(new Block<Document>() {
		    public void apply(Document document) {
		        String sJSON = document.toJson();
		        ProcessWorkspace oProcessWorkspace = null;
		        try {
		            oProcessWorkspace = s_oMapper.readValue(sJSON,ProcessWorkspace.class);
		            aoReturnList.add(oProcessWorkspace);
		        } catch (IOException e) {
		            e.printStackTrace();
		        }

		    }
		});
	}

    public List<ProcessWorkspace> getLastProcessByWorkspace(String sWorkspaceId) {

        final ArrayList<ProcessWorkspace> aoReturnList = new ArrayList<ProcessWorkspace>();
        try {

            //FindIterable<Document> oWSDocuments = getCollection("processworkpsace").find(new Document("workspaceId", sWorkspaceId)).sort(new Document("_id", -1)).limit(5);
        	FindIterable<Document> oWSDocuments = getCollection("processworkpsace").find(new Document("workspaceId", sWorkspaceId)).sort(new Document("operationDate", -1)).limit(5);
            fillList(aoReturnList, oWSDocuments);

        } catch (Exception oEx) {
            oEx.printStackTrace();
        }
        
        // The client expects the most recent last
        Collections.reverse(aoReturnList);

        return aoReturnList;
    }

    public List<ProcessWorkspace> getLastProcessByUser(String sUserId) {

        final ArrayList<ProcessWorkspace> aoReturnList = new ArrayList<ProcessWorkspace>();
        try {

            FindIterable<Document> oWSDocuments = getCollection("processworkpsace").find(new Document("userId", sUserId)).sort(new Document("operationDate", -1)).limit(5);
            fillList(aoReturnList, oWSDocuments);

        } catch (Exception oEx) {
            oEx.printStackTrace();
        }
        
        // The client expects the most recent last
        Collections.reverse(aoReturnList);

        return aoReturnList;
    }
    
    
    public ProcessWorkspace getProcessByProductName(String sProductName) {
        ProcessWorkspace oProcessWorkspace = null;
        try {

            Document oWSDocument = getCollection("processworkpsace").find(new Document("productName", sProductName)).first();

            if (oWSDocument==null) return  null;

            String sJSON = oWSDocument.toJson();
            try {
                oProcessWorkspace = s_oMapper.readValue(sJSON, ProcessWorkspace.class);

            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return oProcessWorkspace;
    }

    public List<ProcessWorkspace> getProcessByProductNameAndWorkspace(String sProductName, String sWorkspace) {

        final ArrayList<ProcessWorkspace> aoReturnList = new ArrayList<ProcessWorkspace>();
        try {

            FindIterable<Document> oWSDocuments = getCollection("processworkpsace").find(
            		Filters.and(
            				Filters.eq("productName", sProductName),
            				Filters.eq("workspaceId", sWorkspace)
            				)
            		)
            		.sort(new Document("operationDate", -1));
            fillList(aoReturnList, oWSDocuments);

        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return aoReturnList;
    }
    
    public ProcessWorkspace getProcessByProcessObjId(String sProcessObjId) {
        ProcessWorkspace oProcessWorkspace = null;
        try {

            Document oWSDocument = getCollection("processworkpsace").find(new Document("processObjId", sProcessObjId)).first();

            if (oWSDocument==null) return  null;

            String sJSON = oWSDocument.toJson();
            try {
                oProcessWorkspace = s_oMapper.readValue(sJSON, ProcessWorkspace.class);

            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return oProcessWorkspace;
    }

    public ArrayList<String> getProcessesStatusByProcessObjId(ArrayList<String> asProcessesObjId) {
    	
    	ArrayList<String> asReturnStatus = new ArrayList<>();
    	
    	final ArrayList<ProcessWorkspace> aoProcessesList = new ArrayList<ProcessWorkspace>();
        try {
        	
        	BasicDBObject oInQuery = new BasicDBObject();
        	
            oInQuery.put("processObjId", new BasicDBObject("$in", asProcessesObjId));

            FindIterable<Document> oWSDocuments = getCollection("processworkpsace").find(oInQuery);
            fillList(aoProcessesList, oWSDocuments);
            
            for (int i=0; i<aoProcessesList.size(); i++) {
            	asReturnStatus.add(aoProcessesList.get(i).getStatus());
            }
        } 
        catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return asReturnStatus;
    }
    
    /**
     * Update a process Workspace. If there is a state change, the system will update also update lastStateChangeDate 
     * @param oProcessWorkspace Process Workpsace to update
     * @return
     */
    public boolean updateProcess(ProcessWorkspace oProcessWorkspace) {

        try {
        	
        	ProcessWorkspace oOriginal = getProcessByProcessObjId(oProcessWorkspace.getProcessObjId());
        	
        	if (oOriginal.getStatus().equals(oProcessWorkspace.getStatus()) == false) {
        		oProcessWorkspace.setLastStateChangeDate(Utils.GetFormatDate(new Date()));
        	}
        	
        	Utils.debugLog("Updating Process " + oProcessWorkspace.getProcessObjId() + " - status: " + oProcessWorkspace.getStatus());
        	
            String sJSON = s_oMapper.writeValueAsString(oProcessWorkspace);
            Document filter = new Document("processObjId", oProcessWorkspace.getProcessObjId());
			Document update = new Document("$set", new Document(Document.parse(sJSON)));
			getCollection("processworkpsace").updateOne(filter, update);

            return true;

        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return false;
    }
    
    public boolean cleanQueue() {

        try {
        	
        	Utils.debugLog("Cleaning ProcessWorkspace Queue");
        	String sJSON = "{\"status\":\"ERROR\"}";
            Document oFilter = new Document("status", "CREATED");
			Document oUpdate = new Document("$set", new Document(Document.parse(sJSON)));
			getCollection("processworkpsace").updateMany(oFilter, oUpdate);

            return true;

        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return false;
    }


    public boolean existsPidProcessWorkspace(Integer iPid) {

        final ArrayList<ProcessWorkspace> aoReturnList = new ArrayList<ProcessWorkspace>();
        boolean bExists = false;
        try {

            FindIterable<Document> oWSDocuments = getCollection("processworkpsace").find(Filters.and(Filters.eq("pid", iPid)));

            oWSDocuments.forEach(new Block<Document>() {
                public void apply(Document document) {
                    String sJSON = document.toJson();
                    ProcessWorkspace oProcessWorkspace = null;
                    try {
                        oProcessWorkspace = s_oMapper.readValue(sJSON,ProcessWorkspace.class);
                        aoReturnList.add(oProcessWorkspace);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }
            });

        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        if (aoReturnList.size() > 0)
            bExists = true;

        return bExists;
    }

    
    public List<ProcessWorkspace> getRunningSummary() {

        final ArrayList<ProcessWorkspace> aoReturnList = new ArrayList<ProcessWorkspace>();
        try {

            FindIterable<Document> oWSDocuments = getCollection("processworkpsace").find(
            		Filters.or(
            				Filters.eq("status", ProcessStatus.RUNNING.name()),
            				Filters.eq("status", ProcessStatus.WAITING.name()),
            				Filters.eq("status", ProcessStatus.READY.name())
            				)
            		)
            		.sort(new Document("operationDate", -1));
            fillList(aoReturnList, oWSDocuments);

        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return aoReturnList;
    }

    public List<ProcessWorkspace> getWaitingSummary() {

        final ArrayList<ProcessWorkspace> aoReturnList = new ArrayList<ProcessWorkspace>();
        try {

            FindIterable<Document> oWSDocuments = getCollection("processworkpsace").find(
            			Filters.eq("status", ProcessStatus.CREATED.name())
            		)
            		.sort(new Document("operationDate", -1));
            fillList(aoReturnList, oWSDocuments);

        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return aoReturnList;
    }
    
    /**
     * Get the list of all processes
     * @return
     */
    public List<ProcessWorkspace> getList() {

        final ArrayList<ProcessWorkspace> aoReturnList = new ArrayList<ProcessWorkspace>();
        try {
            FindIterable<Document> oWSDocuments = getCollection("processworkpsace").find();
            fillList(aoReturnList, oWSDocuments);

        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return aoReturnList;
    }

	/**
	 * @param sUserId a valid user id
	 * @param sProcessObjId a valid process obj id
	 * @return true if the user launched the process, false otherwise
	 */
	public boolean isProcessOwnedByUser(String sUserId, String sProcessObjId) {
		try {
			Document oDoc = getCollection("processworkpsace").find(Filters.and(
					Filters.eq("userId", sUserId),
					Filters.eq("processObjId", sProcessObjId)
					)).first();
			if(null!=oDoc) {
				return true;
			}
		} catch (Exception oE) {
			Utils.debugLog("ProcessWorkspaceRepository.isProcessOwnedByUser( " + sUserId + ", " + sProcessObjId + " ): " + oE);
		}
		return false;
	}
	
	/**
	 * @param sProcessObjId a valid process obj id
	 * @return the corresponding workspace id, if found, null otherwise
	 */
	public String getWorkspaceByProcessObjId(String sProcessObjId) {
		try {
			Document oDoc = getCollection("processworkpsace").find(
					Filters.eq("processObjId", sProcessObjId)
					).first();
			if(null==oDoc) {
				Utils.debugLog("ProcessWorkspaceRepository.getWorkspaceByProcessObjId: " + sProcessObjId + " is not a valid process obj id, aborting");
				return null;
			}
			if(oDoc.containsKey("workspaceId")) {
				String sWorkspaceId = oDoc.getString("workspaceId");
				return sWorkspaceId;
			}
		} catch (Exception oE) {
			Utils.debugLog("ProcessWorkspaceRepository.getWorkspaceByProcessObjId( " + sProcessObjId + " ): " + oE);
		}
		return null;
	}

	
	/**
	 * @param sProcessObjId a valid process obj id
	 * @return the corresponding payload
	 */
	public String getPayload(String sProcessObjId) {
		try {
			Document oDoc = getCollection("processworkpsace").find(
					Filters.eq("processObjId", sProcessObjId)
					).first();
			if(null==oDoc) {
				Utils.debugLog("ProcessWorkspaceRepository.getPayload: " + sProcessObjId + " is not a valid process obj id, aborting");
				return null;
			}
			if(oDoc.containsKey("payload")) {
				String sPayload = oDoc.getString("payload");
				return sPayload;
			}
		} catch (Exception oE) {
			Utils.debugLog("ProcessWorkspaceRepository.getPayload( " + sProcessObjId + " ): " + oE);
		}
		return null;
	}
}
