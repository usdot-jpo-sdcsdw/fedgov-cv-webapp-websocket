package gov.dot.its.jpo.sdcsdw.websocketsfragment.mongo.dao;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

import org.apache.commons.lang.math.NumberUtils;
import org.apache.log4j.Logger;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.Mongo;

@SuppressWarnings("deprecation")
public abstract class AbstractMongoDbDao {
    private final Logger logger = Logger.getLogger(getClass());
    
    protected static final String EXPIRE_AFTER_SECONDS_FIELD    = "expireAfterSeconds";
    protected static final String GEOSPATIAL_INDEX_VALUE        = "2dsphere";
    protected static final DBObject indexOptions = new BasicDBObject("background", true);
    protected static final String CREATED_AT_SORT_INDEX_NAME = "createdAt_1";
    
    private Mongo   mongo;
    private DB      database;
    
    private final ReadLock readLock;
    private final WriteLock writeLock;
    private HashMap<String, DBCollection> collections 
        = new HashMap<String, DBCollection>();

    protected AbstractMongoDbDao(Mongo mongo, String dbname) {
        this.mongo = mongo;
        this.database = this.mongo.getDB(dbname);
        
        ReentrantReadWriteLock semaphore = new ReentrantReadWriteLock();
        this.readLock = semaphore.readLock();
        this.writeLock = semaphore.writeLock();
    }
    
    public Set<String> getCollectionNames() {
        Set<String> names = this.database.getCollectionNames();
        if (names != null) return names;
        return Collections.emptySet();
    }
    
    public List<DBObject> getIndexes(String collectionName) {
        DBCollection collection = get(collectionName);
        List<DBObject> result =  collection.getIndexInfo();
        if (result != null) return result;
        return Collections.emptyList();
    }
    
    protected DBCollection get(String collectionName) {
        DBCollection collection = internalGet(collectionName);
        if (collection != null) return collection;
        return internalSet(collectionName);
    }
    
    protected DBCollection internalGet(String collectionName) {
        readLock.lock();
        try {
            return this.collections.get(collectionName);
        } finally {
            readLock.unlock();
        }
    }
    
    protected DBCollection internalSet(String collectionName) {
        writeLock.lock();
        try {
            if (this.collections.containsKey(collectionName)) {
                return this.collections.get(collectionName);
            }
            
            DBCollection collection = this.database.getCollection(collectionName);
            this.collections.put(collectionName, collection);
            return collection;
        } finally {
            writeLock.unlock();
        }
    }
    
    /**
     * Creates the indexes defined by the indexDefinitionList on the given collection.
     * Indexes in indexDefinitionList are separated by commas, field names and values are
     * separated by ":", and for compound indexes use a space between name:value pairs, for example
     * "region:2dsphere createdAt:1, requestId:1 createdAt:1, createdAt:1"
     * @param collectionName
     * @param indexDefinitionList
     */
    public void createIndexes(String collectionName, String indexDefinitionList) {
        //String indexDefinitionList = "region:2dsphere createdAt:1, requestId:1 createdAt:1, createdAt:1";
        if (indexDefinitionList != null) {
            String[] indexes = indexDefinitionList.split(",");
            for (String index: indexes) {
                BasicDBObject dbObj = buildIndexObject(index.trim());
                DBCollection collection = get(collectionName);
                logger.info("Ensuring index " + dbObj + " for collection " + collectionName);
                collection.createIndex(dbObj, indexOptions);
            }
        }
    }
    
    private BasicDBObject buildIndexObject(String indexString) {
        logger.info("Parsing index string " + indexString); 
        BasicDBObject dbObj = new BasicDBObject();
        String[] nameValues = indexString.split(" ");
        for (String nameValue: nameValues) {
            String name = nameValue.substring(0, nameValue.indexOf(":"));
            String value = nameValue.substring(nameValue.indexOf(":")+1);
            dbObj.put(name, guessType(value));
        }
        return dbObj;
    }
    
    private Object guessType(String value) {
        if (value.startsWith("\"") && value.endsWith("\""))
            return value.substring(1, value.length()-1);
        else if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false"))
            return Boolean.valueOf(value);
        else if (NumberUtils.isNumber(value))
            return Integer.parseInt(value);
        else
            return value;
    }
}