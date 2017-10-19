package gov.usdot.cv.websocket.jms.filter;

import gov.usdot.cv.common.util.Syslogger;
import gov.usdot.cv.websocket.jms.JMSConfig;
import gov.usdot.cv.websocket.jms.connection.MessageSubscriber;
import gov.usdot.cv.websocket.server.WebSocketEventListener;
import gov.usdot.cv.websocket.server.WebSocketServer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;

import org.apache.log4j.Logger;

public class FilterEventListener implements WebSocketEventListener {

	private final static String SYS_LOG_ID = "WebSocket SubscriptionProcessor";
	
	private static Logger logger = Logger.getLogger(FilterEventListener.class);
	private final static String SUBSCRIBE_TAG = "SUBSCRIBE:";
	private final static String SYSTEM_NAME = "systemSubName";
	
	private List<JMSConfig> jmsConfigs;
	private Map<String, MessageSubscriber> messageSubscriberMap = new HashMap<String, MessageSubscriber>();
	
	public FilterEventListener(List<JMSConfig> jmsConfigs) {
		this.jmsConfigs = jmsConfigs;
	}
	
	public void connect() {
		for (JMSConfig config: jmsConfigs) {
			MessageSubscriber messageSubscriber = new MessageSubscriber(config);
			messageSubscriber.start();
			messageSubscriberMap.put(config.systemName, messageSubscriber);
		}
	}
	
	public void close() {
		for (MessageSubscriber messageSubscriber: messageSubscriberMap.values()) {
			messageSubscriber.stop();
		}
		messageSubscriberMap.clear();
	}
	
	public void onMessage(String websocketID, String message) {
		try {
			if (message.startsWith(SUBSCRIBE_TAG)) {
				logger.info("Received subscription message: " + message + " from websocket " + websocketID);
				message = message.substring(SUBSCRIBE_TAG.length()).trim();
				JSONObject json = (JSONObject)JSONSerializer.toJSON(message);
				if (json.containsKey(SYSTEM_NAME)) {
					String systemName = json.getString(SYSTEM_NAME);
					if (messageSubscriberMap.containsKey(systemName)) {
						removeExistingFilter(websocketID);
						WebSocketServer.sendMessage(websocketID, "START: " + message);
						messageSubscriberMap.get(systemName).addFilter(websocketID, json, message);
						Syslogger.getInstance().log(SYS_LOG_ID, 
							String.format("Added subscription for websocketID %s, subscription %s", 
									websocketID, json.toString()));
					} else {
						String errorMsg = "Invalid systemSubName: " + systemName + 
								", not one of the supported systemSubName: " + messageSubscriberMap.keySet().toString();
						logger.error(errorMsg);
						WebSocketServer.sendMessage(websocketID, "ERROR: " + errorMsg);
					}
				} else {
					String errorMsg = "Subscribe message missing required systemSubName field";
					logger.error(errorMsg);
					WebSocketServer.sendMessage(websocketID, "ERROR: " + errorMsg);
				}
			}
		} catch (InvalidFilterException e) {
			String errorMsg = "Invalid Subscription message " + e.toString();
			logger.error(errorMsg);
			WebSocketServer.sendMessage(websocketID, "ERROR: " + errorMsg);
		}
	}

	public void onOpen(String websocketID) {
		// do nothing
	}

	public void onClose(String websocketID) {
		removeExistingFilter(websocketID);
		Syslogger.getInstance().log(SYS_LOG_ID, 
			String.format("Cancelled subscription for websocketID %s", websocketID));
	}
	
	private void removeExistingFilter(String websocketID) {
		for (MessageSubscriber messageSubscriber: messageSubscriberMap.values()) {
			messageSubscriber.removeFilter(websocketID);
		}
	}

}
