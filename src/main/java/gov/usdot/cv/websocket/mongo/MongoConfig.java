package gov.usdot.cv.websocket.mongo;


public class MongoConfig {

	public String systemName;
	public String host;
	public int port;
	public String database;
	
	public MongoConfig() {
		super();
	}

	@Override
	public String toString() {
		return "MongoConfig [systemName=" + systemName + ", host=" + host + ", port="
				+ port + ", database=" + database + "]";
	}
}
