package wasdi.shared.data;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.bson.Document;
import org.bson.conversions.Bson;

import com.mongodb.Block;
import com.mongodb.client.FindIterable;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;

import wasdi.shared.business.Processor;

public class ProcessorRepository extends  MongoRepository {
	
    public boolean insertProcessor(Processor oProcessor) {

        try {
            String sJSON = s_oMapper.writeValueAsString(oProcessor);
            getCollection("processors").insertOne(Document.parse(sJSON));

            return true;

        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return false;
    }

    public Processor getProcessor(String sProcessorId) {

        try {
            Document oWSDocument = getCollection("processors").find(new Document("processorId", sProcessorId)).first();

            String sJSON = oWSDocument.toJson();

            Processor oProcessor = s_oMapper.readValue(sJSON,Processor.class);

            return oProcessor;
        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return  null;
    }

    public Processor getProcessorByName(String sName) {

        try {
            Document oWSDocument = getCollection("processors").find(new Document("name", sName)).first();

            String sJSON = oWSDocument.toJson();

            Processor oProcessor = s_oMapper.readValue(sJSON,Processor.class);

            return oProcessor;
        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return  null;
    }

    
    public boolean updateProcessor(Processor oProcessor) {
        try {
            String sJSON = s_oMapper.writeValueAsString(oProcessor);
            
            Bson oFilter = new Document("processorId", oProcessor.getProcessorId());
            Bson oUpdateOperationDocument = new Document("$set", new Document(Document.parse(sJSON)));
            
            UpdateResult oResult = getCollection("processors").updateOne(oFilter, oUpdateOperationDocument);

            if (oResult.getModifiedCount()==1) return  true;
        }
        catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return  false;
    }

    public List<Processor> getProcessorByUser(String sUserId) {

        final ArrayList<Processor> aoReturnList = new ArrayList<Processor>();
        try {

            FindIterable<Document> oWSDocuments = getCollection("processors").find(new Document("userId", sUserId));

            oWSDocuments.forEach(new Block<Document>() {
                public void apply(Document document) {
                    String sJSON = document.toJson();
                    Processor oProcesor = null;
                    try {
                        oProcesor = s_oMapper.readValue(sJSON,Processor.class);
                        aoReturnList.add(oProcesor);
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
    
    public int getNextProcessorPort() {
    	
    	int iPort = -1;

        try {
        	Document oWSDocument = getCollection("processors").find().sort(new Document("port", -1)).first();
            String sJSON = oWSDocument.toJson();
            Processor oProcessor = s_oMapper.readValue(sJSON,Processor.class);
            iPort = oProcessor.getPort();
        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        if (iPort == -1) iPort = 5000;
        else iPort++;
        
        return iPort;
    }

    public boolean deleteProcessor(String sProcessorId) {

        try {

            DeleteResult oDeleteResult = getCollection("processors").deleteOne(new Document("processorId", sProcessorId));

            if (oDeleteResult != null)
            {
                if (oDeleteResult.getDeletedCount() == 1 )
                {
                    return  true;
                }
            }

        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return  false;
    }

    public int deleteProcessorByUser(String sUserId) {

        try {

            DeleteResult oDeleteResult = getCollection("processors").deleteMany(new Document("userId", sUserId));

            if (oDeleteResult != null)
            {
                return  (int) oDeleteResult.getDeletedCount();
            }

        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return 0;
    }
    
    
    public List<Processor> getDeployedProcessors() {

        final ArrayList<Processor> aoReturnList = new ArrayList<Processor>();
        try {

            //FindIterable<Document> oWSDocuments = getCollection("processors").find(new Document("port", new Document("$gt", 4999)));
        	FindIterable<Document> oWSDocuments = getCollection("processors").find();

            oWSDocuments.forEach(new Block<Document>() {
                public void apply(Document document) {
                    String sJSON = document.toJson();
                    Processor oWorkflow = null;
                    try {
                        oWorkflow = s_oMapper.readValue(sJSON,Processor.class);
                        aoReturnList.add(oWorkflow);
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

}
