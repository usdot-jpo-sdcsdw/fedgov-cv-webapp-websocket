package gov.usdot.cv.common.database.mongodb;

import com.mongodb.MongoOptions;

@SuppressWarnings("deprecation")
public class MongoOptionsBuilder {
    
    private boolean autoConnectRetry = true;
    private int connectTimeoutMs = 0;
    
    public MongoOptionsBuilder setAutoConnectRetry(boolean autoConnectRetry) {
        this.autoConnectRetry = autoConnectRetry;
        return this;
    }
    
    public MongoOptionsBuilder setConnectTimeoutMs(int connectTimeoutMs) {
        this.connectTimeoutMs = connectTimeoutMs;
        return this;
    }
    
    public MongoOptions build() {
        MongoOptions options = new MongoOptions();
        //options.autoConnectRetry = this.autoConnectRetry;
        options.connectTimeout = this.connectTimeoutMs;
        return options;
    }
    
}