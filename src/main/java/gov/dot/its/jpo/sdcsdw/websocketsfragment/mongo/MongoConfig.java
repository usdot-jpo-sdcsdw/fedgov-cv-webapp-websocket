package gov.dot.its.jpo.sdcsdw.websocketsfragment.mongo;


public class MongoConfig {

	public String systemName;
	public String host;
	public int port;
	public String database;
	public boolean autoConnectRetry;
	public int connectionTimeoutMs;
	public String ttlUnit;
    public int ttlValue;
    public String ttlFieldName = "expireAt";
    public boolean ignoreMessageTTL;
    public String collectionName;
    
	public MongoConfig() {
		super();
	}

	@Override
	public String toString() {
	    
		return new StringBuilder().append("MongoConfig")
		                          .append(" [")
		                          .append("systemName=")
		                          .append(systemName)
		                          .append(", ")
		                          .append("host=")
		                          .append(host)
		                          .append(", ")
		                          .append("port=")
		                          .append(port)
		                          .append(", ")
		                          .append("database=")
		                          .append(database)
		                          .append(", ")
		                          .append("collectionName=")
		                          .append(collectionName)
		                          .append(", ")
		                          .append("autoConnectRetry=")
		                          .append(autoConnectRetry)
		                          .append(", ")
		                          .append("connectionTimeoutMs=")
		                          .append(connectionTimeoutMs)
		                          .append(", ")
		                          .append("ttlUnit=")
		                          .append(ttlUnit)
		                          .append(", ")
		                          .append("ttlValue=")
		                          .append(ttlValue)
		                          .append(", ")
		                          .append("ttlFieldName=")
		                          .append(ttlFieldName)
		                          .append(", ")
		                          .append("ignoreMessageTTL=")
		                          .append(ignoreMessageTTL)
		                          .append("]")
		                          .toString();
	}
}
