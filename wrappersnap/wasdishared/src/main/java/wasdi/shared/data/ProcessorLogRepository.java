package wasdi.shared.data;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.bson.Document;
import org.bson.types.ObjectId;

import com.mongodb.BasicDBObject;
import com.mongodb.Block;
import com.mongodb.DBObject;
import com.mongodb.QueryBuilder;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;

import wasdi.shared.business.ProcessorLog;
import wasdi.shared.utils.Utils;

/**
 * User Processors Log repository
 * @author p.campanella
 *
 */
public class ProcessorLogRepository extends MongoRepository {
	
	public ProcessorLogRepository() {
		m_sThisCollection = "processorlog";
		m_sRepoDb = "local";
	}
	
	/**
	 * Insert a new log row
	 * @param oProcessLog row to add
	 * @return Mongo Obj Id
	 */
    public String insertProcessLog(ProcessorLog oProcessLog) {
        try {
        	if(null == oProcessLog) {
        		Utils.debugLog("ProcessorLogRepository.InsertProcessLog: oProcessorLog is null");
        		return null;
        	}
        	CounterRepository oCounterRepo = new CounterRepository();
        	
        	oProcessLog.setRowNumber(oCounterRepo.getNextValue(oProcessLog.getProcessWorkspaceId()));
        	
            String sJSON = s_oMapper.writeValueAsString(oProcessLog);
            Document oDocument = Document.parse(sJSON);
            
            getCollection(m_sThisCollection).insertOne(oDocument);
            return oDocument.getObjectId("_id").toHexString();

        } catch (Exception oEx) {
            Utils.debugLog("ProcessorLogRepository.InsertProcessLog: "+oEx);
        }
        return "";
    }
    
	/**
	 * Insert a new log row
	 * @param aoProcessLogs a list of rows to add
	 */
    public void insertProcessLogList(List<ProcessorLog> aoProcessLogs) {
        try {
        	if(null == aoProcessLogs) {
        		Utils.debugLog("ProcessorLogRepository.InsertProcessLogList: aoProcessorLog is null");
        		return;
        	}
        	
        	List<Document> aoDocs = new ArrayList<>();
        	for (ProcessorLog oProcessorLog: aoProcessLogs) {
        		if(null!=oProcessorLog) {
        			String sJSON = s_oMapper.writeValueAsString(oProcessorLog);
                    Document oDocument = Document.parse(sJSON);
                    aoDocs.add(oDocument);
        		}
			}
        	getCollection(m_sThisCollection).insertMany(aoDocs);

        } catch (Exception oEx) {
            Utils.debugLog("ProcessorLogRepository.InsertProcessLog: "+oEx);
        }
        return;
    }
    
    /**
     * Delete a Log row by Mongo Id
     * @param sId Mongo Obj Id
     * @return True or False
     */
    public boolean deleteProcessorLog(String sId) {
        try {
            getCollection(m_sThisCollection).deleteOne(new Document("_id", new ObjectId(sId)));

            return true;

        } catch (Exception oEx) {
        	Utils.debugLog("ProcessorLogRepository.DeleteProcessorLog( "+sId+" )" +oEx);
        }

        return false;
    }
    
    /**
     * Get the logs of a ProcessWorkspace
     * @param sProcessWorkspaceId Id of the process
     * @return List of all the log rows
     */
    public List<ProcessorLog> getLogsByProcessWorkspaceId(String sProcessWorkspaceId) {

        final ArrayList<ProcessorLog> aoReturnList = new ArrayList<ProcessorLog>();
        if(!Utils.isNullOrEmpty(sProcessWorkspaceId)) {
	        try {
	            FindIterable<Document> oWSDocuments = getCollection(m_sThisCollection).find(new Document("processWorkspaceId", sProcessWorkspaceId));
	            if(oWSDocuments!=null) {
	            	fillList(aoReturnList, oWSDocuments);
	            }
	        } catch (Exception oEx) {
	        	Utils.debugLog("ProcessorLogRepository.GetLogsByProcessWorkspaceId( " + sProcessWorkspaceId + " )" +oEx);
	        }
        }
        return aoReturnList;
    }
    
    /**
     * Get all the logs of an array of ProcessWorkspaceId
     * @param asProcessWorkspaceId
     * @return
     */
    public List<ProcessorLog> getLogsByArrayProcessWorkspaceId(List<String> asProcessWorkspaceId) {
    	
    	/*
        ObjectId[] aoObjarray = new ObjectId[asProcessWorkspaceId.size()];

        for(int i=0;i<asProcessWorkspaceId.size();i++)
        {
            aoObjarray[i] = new ObjectId(asProcessWorkspaceId.get(i));
        }
        */

        BasicDBObject oInQuery = new BasicDBObject("$in", asProcessWorkspaceId);
        BasicDBObject oQuery = new BasicDBObject("processWorkspaceId", oInQuery);

        final ArrayList<ProcessorLog> aoReturnList = new ArrayList<ProcessorLog>();
        
        try {
            FindIterable<Document> oWSDocuments = getCollection(m_sThisCollection).find(oQuery);
            if(oWSDocuments!=null) {
            	fillList(aoReturnList, oWSDocuments);
            }
        } catch (Exception oEx) {
        	Utils.debugLog("ProcessorLogRepository.getLogsByArrayProcessWorkspaceId" +oEx);
        }
        return aoReturnList;
    }
    

    
    /**
     * Delete all the logs of a Process Workspace
     * @param sProcessWorkspaceId Id of the process
     * @return True or False in case of error
     */
    public boolean deleteLogsByProcessWorkspaceId(String sProcessWorkspaceId) {
        if(!Utils.isNullOrEmpty(sProcessWorkspaceId)) {
        	
	        try {
	            getCollection(m_sThisCollection).deleteMany(new Document("processWorkspaceId", sProcessWorkspaceId));
	            return true;
	        } catch (Exception oEx) {
	        	Utils.debugLog("ProcessorLogRepository.GetLogsByProcessWorkspaceId( " + sProcessWorkspaceId + " )" +oEx);
	        }
        }
        return false;
    }
    
    /**
     * Get all the log rows containing a specified text
     * @param sLogText Text to search for
     * @return List of log rows containing the text
     */
    public List<ProcessorLog> getLogRowsByText(String sLogText) {

        final ArrayList<ProcessorLog> aoReturnList = new ArrayList<ProcessorLog>();
        if(!Utils.isNullOrEmpty(sLogText)) {
	        try {
	        	
	        	BasicDBObject oRegexQuery = new BasicDBObject();
	        	oRegexQuery.put("logRow", new BasicDBObject("$regex", sLogText));
	        	
	            FindIterable<Document> oWSDocuments = getCollection(m_sThisCollection).find(oRegexQuery);
	            if(oWSDocuments!=null) {
	            	fillList(aoReturnList, oWSDocuments);
	            }
	        } catch (Exception oEx) {
	        	Utils.debugLog("ProcessorLogRepository.GetLogRowsByText( " + sLogText + " )" +oEx);
	        }
        }
        return aoReturnList;
    }
    
    /**
     * Get all the log rows of a ProcessWorkpsace containing a specified text
     * @param sLogText sLogText Text to search for
     * @param sProcessWorkspaceId WASDI Id of the Process Workspace
     * @return List of found rows
     */
    public List<ProcessorLog> getLogRowsByTextAndProcessId(String sLogText, String sProcessWorkspaceId) {

        final ArrayList<ProcessorLog> aoReturnList = new ArrayList<ProcessorLog>();
        if(!Utils.isNullOrEmpty(sLogText)) {
	        try {
	        	
	        	BasicDBObject oRegexQuery = new BasicDBObject();
	        	oRegexQuery.put("logRow", new BasicDBObject("$regex", sLogText));

	        	List<BasicDBObject> aoFilters = new ArrayList<BasicDBObject>();
	        	aoFilters.add(oRegexQuery);
	        	aoFilters.add(new BasicDBObject("processWorkspaceId", sProcessWorkspaceId));
	        	
	        	BasicDBObject oAndQuery = new BasicDBObject();
	        	oAndQuery.put("$and", aoFilters);
	        	
	        	
	            FindIterable<Document> oWSDocuments = getCollection(m_sThisCollection).find(oAndQuery);
	            if(oWSDocuments!=null) {
	            	fillList(aoReturnList, oWSDocuments);
	            }
	        } catch (Exception oEx) {
	        	Utils.debugLog("ProcessorLogRepository.GetLogRowsByText( " + sLogText + " )" +oEx);
	        }
        }
        return aoReturnList;
    }
    
	/**
	 * Get a list of logs row in a range
	 * iLo and iUp are included
	 * @param sProcessWorkspaceId Process Workpsace Id
	 * @param iLo Lower Bound
	 * @param iUp Upper Bound
	 * @return List of Log Rows in the range
	 */
    public List<ProcessorLog> getLogsByWorkspaceIdInRange(String sProcessWorkspaceId, Integer iLo, Integer iUp) {
    	
		if(null == sProcessWorkspaceId || iLo == null || iUp == null) {
			throw new NullPointerException("ProcessorLogRepository.getLogsByWorkspaceIdInRange( " + sProcessWorkspaceId + ", " + iLo + ", " + iUp + " ): null argument passed");
		}
		if(iLo < 0 || iLo >iUp) {
			throw new IllegalArgumentException("ProcessorLogRepository.getLogsByWorkspaceIdInRange: 0 <= "+iLo+" <= "+iUp+" is unverified");
		}
	
        final ArrayList<ProcessorLog> aoReturnList = new ArrayList<ProcessorLog>();
        
        try {
			//MongoDB query is:
    		//db.getCollection('processorlog').find({ "rowNumber":{$gte: iLo, $lte: iUp}, processWorkspaceId: sProceessWorkspaceId })
        	DBObject oQuery = QueryBuilder.start().and(
        					QueryBuilder.start().put("processWorkspaceId").is(sProcessWorkspaceId).get(),
        					QueryBuilder.start().and(
        						QueryBuilder.start().put("rowNumber").greaterThanEquals(iLo).get(),
        						QueryBuilder.start().put("rowNumber").lessThanEquals(iUp).get()
        					).get()
        		    ).get();
        	BasicDBObject oDocument = new BasicDBObject();
        	oDocument.putAll(oQuery);
        	MongoCollection<Document> aoProcessorLogCollection =  getCollection(m_sThisCollection);
        	FindIterable<Document> oWSDocuments = aoProcessorLogCollection.find(oDocument);
        	if(oWSDocuments != null) {
        		fillList(aoReturnList, oWSDocuments);
        	}
        } catch (Exception oEx) {
        	Utils.debugLog("ProcessorLogRepository.getLogsByWorkspaceIdInRange" + oEx);
        }

        return aoReturnList;

	}
 
    /**
     * Fill a list of ProcessorLog Entities
     * @param aoReturnList
     * @param oWSDocuments
     */
	private void fillList(final ArrayList<ProcessorLog> aoReturnList, FindIterable<Document> oWSDocuments) {
		oWSDocuments.forEach(new Block<Document>() {
		    public void apply(Document document) {
		        String sJSON = document.toJson();
		        ProcessorLog oProcessorLog= null;
		        try {
		        	oProcessorLog = s_oMapper.readValue(sJSON,ProcessorLog.class);
		            aoReturnList.add(oProcessorLog);
		        } catch (IOException oEx) {
		        	Utils.debugLog("ProcessorLogRepository.fillList: "+oEx);
		        }

		    }
		});
	}

}
