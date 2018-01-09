package gov.dot.its.jpo.sdcsdw.webfragment_websockets.mongo;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import net.sf.json.JSONObject;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import org.apache.log4j.Logger;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.Mongo;
import com.mongodb.MongoException;

import gov.dot.its.jpo.sdcsdw.webfragment_websockets.server.WebSocketServer;

@SuppressWarnings("deprecation")
public class MongoQueryRunner {

    private static final Logger logger = Logger.getLogger(MongoQueryRunner.class
                                                          .getName());
                                                  
    private static final Map<Integer, String> collectionLookup = new HashMap<Integer, String>();
    private static final List<String> dateOperators = new ArrayList<String>();
    private static final List<String> resultEncodings = new ArrayList<String>();
    private static final int MAX_CONCURRENT_QUERIES = 5;
  
    // these must match the index names on each MongoDB collection
    // in the mongo shell, run db.<collectionName>.getIndexes() to list the indexes
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
	private Mongo mongoClient;
	private DB database;
	private boolean connected = false;
	private DateFormat sdfNoMillis = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
	private DateFormat sdfMillis = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
	private AtomicInteger queryCount = new AtomicInteger();
	
	public MongoQueryRunner(MongoConfig config) {
		this.config = config;
	}
	
	public void connect() {
		try {
			mongoClient = new Mongo(config.host, config.port);
			database = mongoClient.getDB(config.database);
			connected = true;
			logger.info("Connected to the " + config.systemName + " MongoDB " + config.host + ":" + config.port);
		} /*catch (UnknownHostException e) {
			logger.error("Failed to connect to MongoDB", e);
		} */ catch (MongoException e) {
			logger.error("Failed to connect to MongoDB", e);
		}
	}
	
	public void close() {
		mongoClient.close();
	}
	
	public void runQuery(String websocketID, JSONObject json, String message) {
		if (connected) {
			DBCursor cursor = null;
			try {
				if (queryCount.get() < MAX_CONCURRENT_QUERIES) {
					int startCount = queryCount.incrementAndGet();
					validateMessage(json, message);
					BasicDBObject query = buildQuery(json);
					logger.info("Total concurrent query count for " + config.systemName + " is: " + startCount);
					cursor = buildCursor(query, json);
					cursor.addSpecial("$comment", websocketID);
					WebSocketServer.sendMessage(websocketID, "START: " + message);
					
					int recordCount = 0;
					// the call to hasNext() actually invokes the query against Mongo
					if (cursor.hasNext()) {
						while (cursor.hasNext()) {
							if (!WebSocketServer.isWebSocketOpen(websocketID)) {
								logger.info("WebSocket closed while processing query");
								break;
							}
							DBObject dbObj = cursor.next();
							String record = encodeResponse(dbObj, json);
							if (record != null) {
								WebSocketServer.sendMessage(websocketID, record);
								String connectionID = json.optString("connectionID", null);
								if (connectionID != null && WebSocketServer.isWebSocketOpen(connectionID)) {
									WebSocketServer.sendMessage(connectionID, record);
								}
								recordCount++;
							}
						}
					}
					WebSocketServer.sendMessage(websocketID, "STOP: recordCount=" + recordCount);
				} else {
					logger.warn("The maximum number of concurrent queries (" + MAX_CONCURRENT_QUERIES + 
							") has been reached for " + config.systemName);
					logger.warn("Dropping the query for websocketID " + websocketID);
					WebSocketServer.sendMessage(websocketID, "ERROR: The maximum number of concurrent queries on " +
							config.systemName + " has been reached.  Please try your query again shortly.");
				}
			} catch (InvalidQueryException e) {
				logger.error("InvalidQueryException", e);
				WebSocketServer.sendMessage(websocketID, "ERROR: " + e.toString());
			} catch (Exception e) {
				logger.error("Unexpected Exception", e);
				WebSocketServer.sendMessage(websocketID, "ERROR: " + e.toString());
			} finally {
				int endCount = queryCount.decrementAndGet();
				logger.info("Finished executing query on " + config.systemName + ", total concurrent query count is: " + endCount);
				if (cursor != null) {
					cursor.close();
				}
			}
		} else {
			logger.warn("Not connected to MongoDB");
		}
	}
	
	public void killRunningQueries(String websocketID) {
		DBObject queryID = new BasicDBObject();
		queryID.put("query.$comment", websocketID);
		DBCursor cursor = database.getCollection("$cmd.sys.inprog").find(queryID);
		
		try {
			while(cursor.hasNext()) {
				DBObject dbObj = cursor.next();
				BasicDBList queryList = (BasicDBList)dbObj.get("inprog");
				for (Object obj: queryList) {
					DBObject query = (DBObject)obj;
					Integer opid = Integer.parseInt(query.get("opid").toString());
					logger.info("Killing query with opid " + opid);
					
					BasicDBObject killOp = new BasicDBObject("op", opid);
					database.getCollection("$cmd.sys.killop").findOne(killOp);
				}
			}
		} finally {
			cursor.close();
		}
	}
	
	private String encodeResponse(DBObject dbObj, JSONObject json) {
		String resultEncoding = json.optString("resultEncoding", "hex");
		String record = null;
		if (dbObj.containsField("encodedMsg")) {
			if (resultEncoding.equalsIgnoreCase("full")) {
				record = dbObj.toString();
			} else if (resultEncoding.equalsIgnoreCase("base64")) {
				record = dbObj.get("encodedMsg").toString();
			} else if (resultEncoding.equalsIgnoreCase("hex")) {
				record = Hex.encodeHexString(Base64.decodeBase64(dbObj.get("encodedMsg").toString()));
			}
		} else {
			logger.error("Missing field encodedMsg for message " + dbObj);
		}
		return record;
	}
	
	private JSONObject validateMessage(JSONObject json, String message) throws InvalidQueryException {
//		{
//		    "dialogID": 154,
//		    "startDate": "2014-06-25T17:05:36.757Z",
//		    "startDateOperator": "GTE",
//		    "endDate": "2014-09-26T17:05:36.757Z",
//		    "endDateOperator": "LTE",
//		    "nwLat": 42.398998,
//		    "nwLon": -83.194134,
//		    "seLat": 42.306898,
//		    "seLon": -82.953121,
//		    "orderByField": "createdAt",
//		    "orderByOrder": -1,
//		    "skip": 0,
//		    "limit": 10,
//		    "resultEncoding": "hex"
//		}
	
		try {
			if (json.containsKey("dialogID")) {
				if (!collectionLookup.containsKey(json.getInt("dialogID"))) {
					throw new InvalidQueryException("Invalid dialogID: " + json.getInt("dialogID") + 
							", not one of the supported dialogIDs: " + collectionLookup.keySet().toString());
				}
			} else {
				throw new InvalidQueryException("Missing dialogID in message: " + message);
			}
			
			if (json.containsKey("startDate")) {
				try {
					sdfNoMillis.parse(json.getString("startDate"));
				} catch (ParseException e) {
					try {
						sdfMillis.parse(json.getString("startDate"));
					} catch (ParseException e2) {
						throw new InvalidQueryException("Invalid startDate: " + json.getString("startDate") + 
							", must match format yyyy-MM-dd'T'HH:mm:ss or yyyy-MM-dd'T'HH:mm:ss.SSS");
					}
				}
			}
			
			if (json.containsKey("startDateOperator")) {
				if (!dateOperators.contains(json.getString("startDateOperator"))) {
					throw new InvalidQueryException("Invalid startDateOperator: " + json.getString("startDateOperator") + 
							", not one of the supported startDateOperator: " + dateOperators.toString());
				}
			}
			
			if (json.containsKey("endDate")) {
				try {
					sdfNoMillis.parse(json.getString("endDate"));
				} catch (ParseException e) {
					try {
						sdfMillis.parse(json.getString("endDate"));
					} catch (ParseException e2) {
						throw new InvalidQueryException("Invalid endDate: " + json.getString("endDate") + 
							", must match format yyyy-MM-dd'T'HH:mm:ss or yyyy-MM-dd'T'HH:mm:ss.SSS");
					}
				}
			}
			
			if (json.containsKey("endDateOperator")) {
				if (!dateOperators.contains(json.getString("endDateOperator"))) {
					throw new InvalidQueryException("Invalid endDateOperator: " + json.getString("endDateOperator") + 
							", not one of the supported endDateOperator: " + dateOperators.toString());
				}
			}
			
			if (json.containsKey("nwLat") || json.containsKey("nwLon") || json.containsKey("seLat") || json.containsKey("seLon")) {
				if (!json.containsKey("nwLat"))
					throw new InvalidQueryException("The field nwLat is required");
				if (!json.containsKey("nwLon"))
					throw new InvalidQueryException("The field nwLon is required");
				if (!json.containsKey("seLat"))
					throw new InvalidQueryException("The field seLat is required");
				if (!json.containsKey("seLon"))
					throw new InvalidQueryException("The field seLon is required");
			}
			
			if (json.containsKey("orderByOrder")) {
				if (json.getInt("orderByOrder") != 1 && json.getInt("orderByOrder") != -1) {
					throw new InvalidQueryException("Invalid orderByOrder: " + json.getInt("orderByOrder") + 
							", must be either 1 (Ascending) or -1 (Descending)");
				}
			}
			
			if (json.containsKey("skip")) {
				// just make sure its a number
				json.getInt("skip");
			}
			
			if (json.containsKey("limit")) {
				// just make sure its a number
				json.getInt("limit");
			}
			
			if (json.containsKey("resultEncoding")) {
				String resultEncoding = json.getString("resultEncoding");
				boolean valid = false;
				for (String validEncoding: resultEncodings) {
					if (resultEncoding.equalsIgnoreCase(validEncoding)) {
						valid = true;
						break;
					}
				}
				if (!valid) {
					throw new InvalidQueryException("Invalid resultEncoding: " + resultEncoding + 
							", not one of the supported resultEncodinga: " + resultEncodings);
				}
			}
			return json;
			
		} catch (Exception e) {
			logger.error(e);
			throw new InvalidQueryException(e);
		}
	}
	
	private BasicDBObject buildQuery(JSONObject json) throws InvalidQueryException {
		BasicDBObject query = new BasicDBObject();
		if (json.containsKey("nwLat") && json.containsKey("nwLon") &&
				json.containsKey("seLat") && json.containsKey("seLon")) {
			
			double nwLat = json.getDouble("nwLat");
			double nwLon = json.getDouble("nwLon");
			
			double seLat = json.getDouble("seLat");
			double seLon = json.getDouble("seLon");
			
			double neLat = nwLat;
			double neLon = seLon;
			
			double swLat = seLat;
			double swLon = nwLon;
			
			List<double[]> coordinates = new ArrayList<double[]>();
			coordinates.add(new double[] { nwLon, nwLat });
			coordinates.add(new double[] { neLon, neLat });
			coordinates.add(new double[] { seLon, seLat });
			coordinates.add(new double[] { swLon, swLat});
			coordinates.add(new double[] { nwLon, nwLat });
			// have to wrap the coords in another list to get the triple "[[[" that mongo wants
			List<List<double[]>> coordinatesList = new ArrayList<List<double[]>>();
			coordinatesList.add(coordinates);
			
			DBObject geoWithin = BasicDBObjectBuilder.start()
				.push("$geoIntersects")
					.push("$geometry")
				    	.add("type", "Polygon")
				        .add("coordinates", coordinatesList).get();
			query.append("region", geoWithin);
		}
		
		if (json.containsKey("startDate") || json.containsKey("endDate")) {
			Date startDate = null;
			Date endDate = null;
			
			if (json.containsKey("startDate")) {
				try {
					startDate = sdfNoMillis.parse(json.getString("startDate"));
				} catch (ParseException e) {
					try {
						sdfMillis.parse(json.getString("startDate"));
					} catch (ParseException e2) {
						throw new InvalidQueryException("Invalid startDate: " + json.getString("startDate") + 
							", must match format yyyy-MM-dd'T'HH:mm:ss or yyyy-MM-dd'T'HH:mm:ss.SSS");
					}
				}
			}
			if (json.containsKey("endDate")) {
				try {
					endDate = sdfNoMillis.parse(json.getString("endDate"));
				} catch (ParseException e) {
					try {
						sdfMillis.parse(json.getString("endDate"));
					} catch (ParseException e2) {
						throw new InvalidQueryException("Invalid endDate: " + json.getString("endDate") + 
							", must match format yyyy-MM-dd'T'HH:mm:ss or yyyy-MM-dd'T'HH:mm:ss.SSS");
					}
				}
			}
			
			DBObject dateRange = new BasicDBObject();
			if (startDate != null) {
				String startDateOperator = json.optString("startDateOperator", "GTE");
				dateRange.put("$" + startDateOperator.toLowerCase(), startDate);
			}
			if (endDate != null) {
				String endDateOperator = json.optString("endDateOperator", "LTE");
				dateRange.put("$" + endDateOperator.toLowerCase(), endDate);
			}
			query.append("createdAt", dateRange);
		}
		return query;
	}
	
	private DBCursor buildCursor(BasicDBObject query, JSONObject json) throws InvalidQueryException {
		StringBuilder queryParams = new StringBuilder();
		DBObject fieldNames = null;
		
		String resultEncoding = json.optString("resultEncoding", "hex");
		if (!resultEncoding.equals("full")) {
			fieldNames = new BasicDBObject(2);
			fieldNames.put("encodedMsg", 1);
			fieldNames.put("_id", 0);
			queryParams.append(" fieldNames: ").append(fieldNames);
		}
		
		//String collectionName = collectionLookup.get(json.getInt("dialogID"));
		String collectionName = config.collectionName;
		DBCollection collection = database.getCollection(collectionName);
		DBCursor cursor = (fieldNames == null) ? collection.find(query) : collection.find(query, fieldNames);
		
		if (json.containsKey("orderByField")) {
			String orderByField = json.getString("orderByField");
			BasicDBObject orderBy = new BasicDBObject(orderByField, 
					json.optInt("orderByOrder", -1));
			queryParams.append(" orderBy: ").append(orderBy);
			cursor.sort(orderBy);
			
			if (orderByField.equals("createdAt"))
				cursor.hint(CREATED_AT_SORT_INDEX_NAME);
			else if (orderByField.equals("requestId"))
				cursor.hint(REQUEST_ID_SORT_INDEX_NAME);
		} else {
			cursor.hint(NO_SORT_INDEX_NAME);
		}
		
		int skipNumber = json.optInt("skip", 0);
		if (skipNumber > 0) {
			cursor.skip(skipNumber);
			queryParams.append(" skip: ").append(skipNumber);
		}
		
		int limitNumber = json.optInt("limit", 0);
		if (limitNumber > 0) {
			cursor.limit(limitNumber);
			queryParams.append(" limit: ").append(limitNumber);
		}
		
		logger.info("Executing query on " + config.systemName + " " + 
				collectionName + " : " + query + " , " + queryParams);
		return cursor;
	}
	
}
