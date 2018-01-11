package gov.dot.its.jpo.sdcsdw.websocketsfragment.mongo;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;

import com.mongodb.BasicDBObject;
import com.mongodb.MongoException;
import com.mongodb.MongoOptions;
import com.mongodb.WriteResult;

import gov.dot.its.jpo.sdcsdw.websocketsfragment.deposit.DepositException;
import gov.dot.its.jpo.sdcsdw.websocketsfragment.mongo.model.DataModel;
import gov.dot.its.jpo.sdcsdw.websocketsfragment.service.AsdCompleteXerParser;
import gov.dot.its.jpo.sdcsdw.websocketsfragment.service.GeoJsonBuilder;
import gov.dot.its.jpo.sdcsdw.websocketsfragment.service.xerjsonparser.XerJsonParserException;
import net.sf.json.JSONObject;

@SuppressWarnings("deprecation")
public class MongoDepositor
{
    private static final Logger logger = Logger.getLogger(MongoDepositor.class
                                                          .getName());
    
    private static final String ENCODED_MSG = "encodedMsg";
    
    private static final Map<Integer, String> collectionLookup = new HashMap<Integer, String>();
    private static final List<String> dateOperators = new ArrayList<String>();
    private static final List<String> resultEncodings = new ArrayList<String>();
    
    private static final String GEOJSON_FIELD_NAME = "region";
    
    
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
    
    private CloseableInsertSitDataDao dao;
    
    public MongoDepositor(MongoConfig config) {
        this.config = config;
    }
    
    public void connect() {
        try {
            MongoOptions options = new MongoOptionsBuilder().setAutoConnectRetry(config.autoConnectRetry).setConnectTimeoutMs(config.connectionTimeoutMs).build();
            dao = CloseableInsertSitDataDao.newInstance(config.host, config.port, options, config.database);
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
    
    public boolean deposit(JSONObject json, Document xer) throws DepositException {
        int retries = 3;
        while (retries >= 0) {
            try {
                try {
                    AsdCompleteXerParser.unpackAsdXer(json, xer);
                } catch (XerJsonParserException ex) {
                    throw new DepositException(ex);
                }
                
                json.put(GEOJSON_FIELD_NAME, GeoJsonBuilder.buildGeoJson(json));
                
                
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
                    logger.info(result.getN() + " records affected by update");
                } else {
                    result = this.dao.insert(config.collectionName, doc);
                    logger.info(result.getN() + " records affected by insert");
                }
                
                return true;
                
            } catch (DepositException ex) {
                throw ex;
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
