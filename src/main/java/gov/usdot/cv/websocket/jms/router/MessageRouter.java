package gov.usdot.cv.websocket.jms.router;

import gov.usdot.cv.websocket.jms.filter.Filter;
import gov.usdot.cv.websocket.jms.filter.FilterBuilder;
import gov.usdot.cv.websocket.jms.filter.InvalidFilterException;
import gov.usdot.cv.websocket.jms.format.MessageFormatter;
import gov.usdot.cv.websocket.server.WebSocketServer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.sf.json.JSONObject;

import com.oss.asn1.DecodeFailedException;
import com.oss.asn1.DecodeNotSupportedException;

public class MessageRouter {

	private Map<String, Filter> filterMap = new ConcurrentHashMap<String, Filter>(
			16, 0.9f, 1);
	private Map<String, DelayedMessageSender> delayedSenderMap = new ConcurrentHashMap<String, DelayedMessageSender>(
			16, 0.9f, 1);
	
	public void addFilter(String websocketID, JSONObject json, String message)
			throws InvalidFilterException {
		Filter filter = FilterBuilder.buildFilter(json, message);
		filterMap.put(websocketID, filter);
		int messageDelay = json.optInt("messageDelay", 0);
		if (messageDelay != 0) {
			DelayedMessageSender delayedSender = new DelayedMessageSender(websocketID, messageDelay);
			delayedSender.start();
			delayedSenderMap.put(websocketID, delayedSender);
		}
	}

	public void removeFilter(String websocketID) {
		filterMap.remove(websocketID);
		DelayedMessageSender delayedSender = delayedSenderMap.remove(websocketID);
		if (delayedSender != null) {
			delayedSender.stop();
		}
	}

	public void routeMessage(byte[] message) throws DecodeFailedException, DecodeNotSupportedException, InterruptedException {
		for (Map.Entry<String, Filter> entry : filterMap.entrySet()) {
			String websocketID = entry.getKey();
			Filter filter = entry.getValue();

			RoutableMessage routableMessage = new RoutableMessage(message);
			if (routableMessage.matches(filter)) {
				String formattedMessage = MessageFormatter.formatMessage(routableMessage, filter.getResultEncoding());
				
				if (formattedMessage != null) {
					DelayedMessageSender delayedSender = delayedSenderMap.get(websocketID);
					if (delayedSender == null) {
						WebSocketServer.sendMessage(websocketID, formattedMessage);
					} else {
						delayedSender.sendMessageWithDelay(formattedMessage, routableMessage.getTimestamp());
					}
				}
			}
		}
	}
	
}
