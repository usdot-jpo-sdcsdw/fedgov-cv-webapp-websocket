package gov.usdot.cv.websocket.mongo;

import java.net.UnknownHostException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBObject;
import com.mongodb.Mongo;
import com.mongodb.MongoException;
import com.mongodb.MongoOptions;
import com.mongodb.WriteResult;
import com.mongodb.util.JSON;

import gov.usdot.cv.common.database.mongodb.MongoOptionsBuilder;
import gov.usdot.cv.mongodb.datasink.model.DataModel;
import net.sf.json.JSONObject;

public class MongoDepositor
{
    private static final Logger logger = Logger.getLogger(MongoDepositor.class
                                                          .getName());
                                                  
    private static final Object LOCK = new Object();
    private static final String TTL_UNITS = "^(minute|day|week|month|year)$";
    private static final String ENCODED_MSG = "encodedMsg";
    
    private static final Map<Integer, String> collectionLookup = new HashMap<Integer, String>();
    private static final List<String> dateOperators = new ArrayList<String>();
    private static final List<String> resultEncodings = new ArrayList<String>();
    private static final int MAX_CONCURRENT_QUERIES = 5;
  
    // these must match the index names on each MongoDB collection
    //in the mongo shell, run db.<collectionName>.getIndexes() to list the indexes
    private static final String NO_SORT_INDEX_NAME = "region_2dsphere_createdAt_1";
    private static final String CREATED_AT_SORT_INDEX_NAME = "createdAt_1";
    private static final String REQUEST_ID_SORT_INDEX_NAME = "requestId_1_createdAt_1";
 
    
    static {
        collectionLookup.put(154, "vehSitDataMessage");
        collectionLookup.put(156, "travelerInformation");
        collectionLookup.put(162, "intersectionSitData");
      
        dateOperators.add("GT");
        dateOperators.add("GTE");
        dateOperators.add("LT");
        dateOperators.add("LTE");
      
        resultEncodings.add("hex");
        resultEncodings.add("base64");
        resultEncodings.add("full");
    }
  
    private MongoConfig config;
    private boolean connected = false;
    private DateFormat sdfNoMillis = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
    private DateFormat sdfMillis = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
    
    private CloseableInsertSitDataDao dao;
    
    public MongoDepositor(MongoConfig config) {
        this.config = config;
    }
    
    public void connect() {
        try {
            MongoOptions options = new MongoOptionsBuilder().setAutoConnectRetry(config.autoConnectRetry).setConnectTimeoutMs(config.connectionTimeoutMs).build();
            dao = CloseableInsertSitDataDao.newInstance(config.host, config.port, options, config.database);
            connected = true;
            logger.info("Connected to the " + config.systemName + " MongoDB " + config.host + ":" + config.port);
        } catch (UnknownHostException e) {
            logger.error("Failed to connect to MongoDB", e);
        } catch (MongoException e) {
            logger.error("Failed to connect to MongoDB", e);
        }
    }
    
    public void close() {
        dao.close();
    }
    
    public boolean deposit(JSONObject json) {
        int retries = 3;
        while (retries >= 0) {
            try {
                DataModel model = new DataModel(
                    json,
                    config.ttlFieldName, 
                    config.ignoreMessageTTL,
                    config.ttlValue, 
                    config.ttlUnit);
                
                BasicDBObject query = model.getQuery();
                BasicDBObject doc = model.getDoc();
                
                if (!doc.containsField(ENCODED_MSG)) {
                    logger.error("Missing " + ENCODED_MSG + " in record " + json);
                    return false;
                }
                
                WriteResult result = null;
                if (model.getQuery() != null) {
                    result = this.dao.upsert(config.collectionName, query, doc);
                } else {
                    result = this.dao.insert(config.collectionName, doc);
                }
                
                return true;
                
            } catch (Exception ex) {
                logger.error(String.format("Failed to store record into MongoDB. Message: %s", ex.toString()), ex);
            } finally {
                retries--;
            }
            
            try { Thread.sleep(10); } catch (Exception ignore) {}
        }
        
        logger.error("Failed to store record into MongoDB, retries exhausted. Record: " + json.toString());
        
        return false;
    }
}
