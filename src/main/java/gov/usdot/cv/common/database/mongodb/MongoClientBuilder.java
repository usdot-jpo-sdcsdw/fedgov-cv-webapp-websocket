package gov.usdot.cv.common.database.mongodb;

import java.net.UnknownHostException;

import com.mongodb.Mongo;
import com.mongodb.MongoOptions;
import com.mongodb.ServerAddress;

public class MongoClientBuilder {
    private static final String DEFAULT_HOST = "localhost";
    private static final int    DEFAULT_PORT = 27017;
    
    private String          host = DEFAULT_HOST;
    private Integer         port = DEFAULT_PORT;
    private MongoOptions    options = null;
    
    public MongoClientBuilder setHost(String host) {
        this.host = host;
        return this;
    }
    
    public MongoClientBuilder setPort(Integer port) {
        this.port = port;
        return this;
    }
    
    public MongoClientBuilder setMongoOptions(MongoOptions options) {
        this.options = options;
        return this;
    }
    
    public Mongo build() throws UnknownHostException {
        if (this.options == null) {
            return new Mongo(new ServerAddress(this.host, this.port));
        } else {
            return new Mongo(new ServerAddress(this.host, this.port), this.options);
        }
    }
}