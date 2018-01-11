package gov.dot.its.jpo.sdcsdw.websocketsfragment.mongo.model;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import com.mongodb.BasicDBObject;
import com.mongodb.util.JSON;

import net.sf.json.JSONObject;

public class DataModel {
    public static final String TIMESTAMP_FORMAT     = "yyyy-MM-dd'T'HH:mm:ss";
    public static final String TIMESTAMP_KEY        = "timestamp";
    public static final String TIME_TO_LIVE_KEY     = "timeToLive";
    public static final String REQUEST_ID_KEY       = "requestId";
    public static final String RECORD_ID_KEY        = "recordId";
    public static final String CREATED_AT_KEY       = "createdAt";

    private JSONObject record;
    
    private BasicDBObject query;
    private BasicDBObject doc;
    
    public DataModel(
            JSONObject record, 
            String expirationFieldName,
            boolean ignoreMessageTTL,
            int ttlValue,
            String ttlUnit) throws ParseException {
        this.record = record;
        this.doc = (BasicDBObject) JSON.parse(record.toString());
        if (record.has(RECORD_ID_KEY)) {
            this.query = new BasicDBObject();
            this.query.put(RECORD_ID_KEY, record.getInt(RECORD_ID_KEY));
        }
        if (record.has(TIMESTAMP_KEY)) {
            String timestamp = record.getString(TIMESTAMP_KEY);
            SimpleDateFormat df = new SimpleDateFormat(TIMESTAMP_FORMAT);
            df.setTimeZone(TimeZone.getTimeZone("UTC"));
            this.doc.put(TIMESTAMP_KEY, df.parse(timestamp));
        }
        setTimeToLive(expirationFieldName, ignoreMessageTTL, ttlValue, ttlUnit);
    }
    
    public BasicDBObject getQuery() {
        return this.query;
    }
    
    public BasicDBObject getDoc() {
        return this.doc;
    }
    
    protected Integer getTimeToLive() {
        if (record.has(TIME_TO_LIVE_KEY)) {
            return record.getInt(TIME_TO_LIVE_KEY);
        }
        return null;
    }
    
    private void setTimeToLive(
        String expirationFieldName, 
        boolean ignoreMessageTTL,
        int ttlValue, 
        String ttlUnit) {
        Date now = new Date();
        Date expiration = null;
        
        if (! ignoreMessageTTL) {
            // Ignore message ttl not set and ttl exists in the
            // record than we uses the one in the record to 
            // calculate when the message will expire.
            
            if (getTimeToLive() != null) {
                TimeToLive ttl = TimeToLive.fromCode(getTimeToLive());
                if (ttl != null) {
                    expiration = ttl.getExpiration();
                }
            }
        }
        
        if (expiration == null) {
            // The ignore message ttl is set or the ttl value doesn't
            // exist in the message. If this is the case, we uses the
            // default ttl.
            expiration = TimeToLive.getExpiration(ttlValue, ttlUnit);
        }
        
        this.doc.put(CREATED_AT_KEY, now);
        this.doc.put(expirationFieldName, expiration);
    }
    
    public String toString() {
        String result = (this.record != null) ? this.record.toString() : null;
        return result;
    }
}