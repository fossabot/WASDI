package wasdi.shared.data;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.MongoClient;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

import java.util.Arrays;

/**
 * Base Repository Class
 * Created by p.campanella on 21/10/2016.
 */
public class MongoRepository {
	/**
	 * Db Name
	 */
    public static String DB_NAME = "wasdi";
    /**
     * Server Address
     */
    public static String SERVER_ADDRESS = "127.0.0.1";
    /**
     * Server Port
     */
    public static int SERVER_PORT = 27017;
    /**
     * Db User
     */
    public static String DB_USER = "user";
    /**
     * Db Password
     */
    public static String DB_PWD = "password";

    /**
     * Object Mapper
     */
    public static ObjectMapper s_oMapper = new ObjectMapper();

    static  {
        s_oMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    /**
     * Mongo Client
     */
    private static MongoClient s_oMongoClient = null;
    /**
     * Mongo Database
     */
    private static  MongoDatabase s_oMongoDatabase = null;

    /**
     * Get The database Object
     * @return
     */
    public static MongoDatabase getMongoDatabase() {
        if (s_oMongoClient == null) {

            MongoCredential oCredential = MongoCredential.createCredential(DB_USER, DB_NAME, DB_PWD.toCharArray());
            s_oMongoClient = new MongoClient(new ServerAddress(SERVER_ADDRESS, SERVER_PORT), Arrays.asList(oCredential));
            s_oMongoDatabase = s_oMongoClient.getDatabase(DB_NAME);
        }

        return s_oMongoDatabase;
    }

    /**
     * Get a named collection
     * @param sCollection
     * @return
     */
    public MongoCollection<Document> getCollection(String sCollection) {
        return getMongoDatabase().getCollection(sCollection);
    }
    
    /**
     * Shut down the connection
     */
    public static void shutDownConnection() {
    	if (s_oMongoClient != null) {
    		try {
    			s_oMongoClient.close();
    		}
    		catch (Exception e) {
				System.out.println("MongoRepository.shutDownConnection: exception " + e.getMessage());
				e.printStackTrace();
			}
    	}
    }
}
