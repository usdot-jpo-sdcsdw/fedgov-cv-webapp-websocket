package gov.usdot.cv.websocket.jms.connection;

import gov.usdot.cv.websocket.jms.JMSConfig;
import gov.usdot.cv.websocket.jms.filter.InvalidFilterException;
import gov.usdot.cv.websocket.jms.router.MessageRouter;

import javax.jms.BytesMessage;
import javax.jms.Message;
import javax.jms.MessageListener;

import net.sf.json.JSONObject;

import org.apache.log4j.Logger;

/**
 * Subscribes to the JMS Server for messages and processes them as they come in. 
 */
public class MessageSubscriber {

	private static Logger logger = Logger.getLogger(MessageSubscriber.class);
	private JMSConnection jmsConn;
	private JMSConfig config;
	private MessageRouter messageRouter = new MessageRouter();
	
	public MessageSubscriber(JMSConfig config) {
		this.config = config;
	}
	
	public void start() {
		boolean connected = false;
		int attempts = 0;
		while (!connected && attempts < 3) {
			attempts++;
			try {
				jmsConn = new JMSConnection(config);
				jmsConn.openConnection();
				jmsConn.addTopicListener(config.topic, new VehSitDataMessageListener());
				jmsConn.startConnection();
				connected = true;
			} catch (Exception e) {
				logger.error("Error starting MessageSubscriber, retrying in 10 seconds", e);
				try { Thread.sleep(10000); } catch (InterruptedException ignore) { }
			}
		}
		if (!connected) {
			logger.error("Failed to connect to " + config);
		}
	}
	
	public void stop() {
		if (jmsConn != null) {
			jmsConn.closeConnection();
		}
	}
	
	private class VehSitDataMessageListener implements MessageListener {
		public void onMessage(Message message) {
			try {
				if (message instanceof BytesMessage) {
					BytesMessage bytesMessage = (BytesMessage)message;
					byte[] bytes = new byte[(int)bytesMessage.getBodyLength()];
					bytesMessage.readBytes(bytes);
					
					messageRouter.routeMessage(bytes);
				} else {
					logger.error("Unexpected message type " + message.getClass().getName());
				}
			} catch (Exception e) {
				logger.error("Error in VehSitDataMessageListener", e);
			}
		}
	}
	
	public void addFilter(String websocketID, JSONObject json, String message)
			throws InvalidFilterException {
		messageRouter.addFilter(websocketID, json, message);
	}

	public void removeFilter(String websocketID) {
		messageRouter.removeFilter(websocketID);
	}
}
