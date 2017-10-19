package gov.usdot.cv.websocket.jms.router;

import gov.usdot.cv.websocket.server.WebSocketServer;

import java.util.Timer;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

public class DelayedMessageSender extends Timer {

	private static Logger logger = Logger.getLogger(DelayedMessageSender.class);
	
	private static final int TEN_SECONDS = 1000 * 10;
	private static final int ONE_HOUR = 1000 * 60 * 60;
	private static final int MAX_DELAY_TIME = TEN_SECONDS;
	private static final int MAX_QUEUED_MESSAGES = 10000;
	
	private String websocketID;
	private int messageDelay;
	private long lastSentTime = 0;
	private long prevMsgTimestamp = 0;
	private long currMsgTimestamp = 0;
	
	private int delayAdjustCounter = 0;
	private int origMessageDelay;
	
	private BlockingQueue<MessageTimestampPair> messageQueue = new LinkedBlockingQueue<MessageTimestampPair>();
	private Thread senderThread;
	private MessageSender sender = new MessageSender();
	
	public DelayedMessageSender(String websocketID, int messageDelay) {
		this.websocketID = websocketID;
		if (messageDelay > MAX_DELAY_TIME) {
			logger.warn("messageDelay " + messageDelay + " is greater than max allowable " 
					+ MAX_DELAY_TIME + " ms");
			messageDelay = MAX_DELAY_TIME;
			logger.warn("Setting messageDelay to " + messageDelay);
		}
		this.messageDelay = messageDelay;
		this.origMessageDelay = messageDelay;
	}
	
	public void start() {
		senderThread = new Thread(sender);
		senderThread.start();
	}
	
	public void stop() {
		sender.stop();
	}
	
	public void sendMessageWithDelay(String message, long timestamp) throws InterruptedException {
		messageQueue.put(new MessageTimestampPair(message, timestamp));
		// adjust the messageDelay if we hit max messages queued
		adjustMessageDelay();
	}
	
	private void adjustMessageDelay() {
		if (origMessageDelay != 0) {
			delayAdjustCounter++;
			if (delayAdjustCounter > (MAX_QUEUED_MESSAGES / 4)) {
				int queueSize = messageQueue.size();
				logger.warn("Websocket " + websocketID + ", number of queued messages " + queueSize
						+ ", max allowed " + MAX_QUEUED_MESSAGES);
				if (queueSize >= MAX_QUEUED_MESSAGES) {
					logger.warn("Websocket " + websocketID + ", temporarily suspending messageDelay because max queue size reached");
					messageDelay = 0;
					if (origMessageDelay > 0) {
						WebSocketServer.sendMessage(websocketID, "ERROR: messageDelay " + messageDelay 
								+ ", is too slow to keep up with the current message rate, temporarily suspending messageDelay");
					} else if (origMessageDelay < 0) {
						WebSocketServer.sendMessage(websocketID, "ERROR: incoming message timestamps are out of sequence, " 
								+ "temporarily suspending messageDelay");
						senderThread.interrupt();
					}
				} else {
					if ((messageDelay == 0) && (queueSize < (MAX_QUEUED_MESSAGES / 2))) {
						messageDelay = origMessageDelay;
						logger.warn("Websocket " + websocketID + ", messageDelay of " + messageDelay 
								+ " has been restored");
					}
				}
				delayAdjustCounter = 0;
			}
		}
	}
	
	private long calculateTimeDelay(long currMsgTimestamp) {
		if (this.messageDelay == 0) {
			// 0 messageDelay means 0 time delay
			return 0;
		} else if (messageDelay > 0) {
			// for positive messageDelay we delay by messageDelay - timeSinceLastSend
			// should give a smooth stream of messages spaced at messageDelay intervals
			long timeSinceLastSend = System.currentTimeMillis() - this.lastSentTime;
			long timeDelay = this.messageDelay - timeSinceLastSend;
			return (timeDelay <= 0 ? 0 : timeDelay);
		} else {
			// for negative messageDelay we try to space messages as they originally arrived
			// by looking at the message timestamp and comparing time between current and previous
			this.prevMsgTimestamp = this.currMsgTimestamp;
			this.currMsgTimestamp = currMsgTimestamp;
			if (this.currMsgTimestamp < this.prevMsgTimestamp) {
				logger.warn("Message received out of time sequence, check timestamps in messages");
			}
			long timeSinceLastSend = System.currentTimeMillis() - this.lastSentTime;
			long timeBetweenMessages = this.currMsgTimestamp - this.prevMsgTimestamp;
			if (timeBetweenMessages > ONE_HOUR) {
				logger.warn("The time between sequential messages was greater than 1 hour, check timestamps in messages");
			}
			long timeDelay = timeBetweenMessages - timeSinceLastSend;
			if (logger.isDebugEnabled()) {
				logger.debug("TimeDelay:" + timeDelay + " = (TimeBetweenMessages:" + timeBetweenMessages 
					+ " - TimeSinceLastSend:" + timeSinceLastSend + ")");
			}
			return (timeDelay <= 0 ? 0 : timeDelay);
		}
	}
	
	private class MessageSender implements Runnable {

		private boolean stop = false;
		
		public void run() {
			while (!stop) {
				MessageTimestampPair msgAndTime = null;
				try {
					msgAndTime = messageQueue.poll(1000, TimeUnit.MILLISECONDS);
				} catch (InterruptedException e) {
					logger.error("Error sending message to websocket " + websocketID, e);
				}
				if (msgAndTime != null) {
					long delay = calculateTimeDelay(msgAndTime.timestamp);
					try { Thread.sleep(delay); } catch (InterruptedException e) {
						logger.error("Sender thread sleep interrupted while sending message to websocket " + websocketID);
						if (origMessageDelay == -1) {
							logger.warn("messageDelay is -1, most possible cause is incoming messages are not properly timestamped");
						}
					}
					WebSocketServer.sendMessage(websocketID, msgAndTime.message);
					lastSentTime = System.currentTimeMillis();
				}
			}
		}
		
		public void stop() {
			stop = true;
		}
	}
	
	private class MessageTimestampPair {
		public final String message;
		public final long timestamp;
		
		public MessageTimestampPair(String message, long timestamp) {
			super();
			this.message = message;
			this.timestamp = timestamp;
		}
	}
}
