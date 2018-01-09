package gov.dot.its.jpo.sdcsdw.webfragment_websockets.server;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;

public class WebSocketServer {
	
	private static Logger logger = Logger.getLogger(WebSocketServer.class);
	
	private static final List<WebSocketEventListener> eventListeners = new CopyOnWriteArrayList<WebSocketEventListener>();
	private static final Map<String, CVWebSocket> webSocketMap = new ConcurrentHashMap<String, CVWebSocket>(16, 0.9f, 1);
	
	public static CVWebSocket buildWebSocket() {
		return new WebSocketServer().new CVWebSocket();
	}
	
	public static void sendMessage(String message) {
		if (message != null) {
			for (CVWebSocket socket : webSocketMap.values()) {
				if (socket.isOpen()) {
					try {
						socket.sendMessage(message);
					} catch (IOException e) {
						logger.error("Error sending message: " + message + " to session: " + socket.session, e);
					}
				}
			}
		}
	}
	
	public static void sendMessage(String webSocketID, String message) {
		if (message != null) {
			CVWebSocket socket = webSocketMap.get(webSocketID);
			if (socket != null) {
				try {
					socket.sendMessage(message);
				} catch (IOException e) {
					logger.error("Error sending message: " + message + " to session: " + socket.session, e);
				}
			} else {
				logger.warn("No WebSocket found with ID: " + webSocketID);
			}
		}
	}
	
	public static void registerEventListener(WebSocketEventListener listener) {
		eventListeners.add(listener);
	}
	
	public static boolean isWebSocketOpen(String webSocketID) {
		return webSocketMap.containsKey(webSocketID);
	}
	
	@WebSocket
	public class CVWebSocket {
		
		private Session session;
		private String webSocketID;
		private ThreadedMessageProcessor messageProcessor;
		private ThreadedMessageSender messageSender;
		
		@OnWebSocketConnect
		public void onOpen(Session session) {
			this.webSocketID =
					String.format("l(%s)<->r(%s)", 
							session.getLocalAddress().toString().split("/")[1],
							session.getRemoteAddress().toString().split("/")[1]);
			this.session = session;
			this.session.setIdleTimeout(0);		// Don't timeout
			
			webSocketMap.put(this.webSocketID, this);
			logger.info("WebSocket " + webSocketID + " session opened");
			logger.info("WebSocket session count is " + webSocketMap.size());
			
			for (WebSocketEventListener listener: eventListeners) {
				try {
					listener.onOpen(this.webSocketID);
				} catch (Exception e) {
					logger.error("onOpen eventListener call failed", e);
				}
			}
			
			messageProcessor = new ThreadedMessageProcessor(this.session, this.webSocketID);
			messageSender = new ThreadedMessageSender(this.session, this.webSocketID);
			messageProcessor.start();
			messageSender.start();
			
			try {
				sendMessage("CONNECTED: sessionID=" + this.webSocketID);
			} catch (IOException e) {
				logger.error("Failed to send sessionID on socket open", e);
			}
		}
		
		public boolean isOpen() {
			return session.isOpen();
		}
		
		public void sendMessage(String data) throws IOException {
			messageSender.sendMessage(data);
		}
		
		@OnWebSocketMessage
		public void onMessage(String message) {
			messageProcessor.processMessage(message);
		}
		
		@OnWebSocketClose
		public void onClose(int statusCode, String reason) {
			messageProcessor.stop();
			messageSender.stop();
			
			webSocketMap.remove(this.webSocketID);
			logger.info("WebSocket " + webSocketID + " session closed");
			logger.info("WebSocket session count is " + webSocketMap.size());
			
			for (WebSocketEventListener listener: eventListeners) {
				try {
					listener.onClose(this.webSocketID);
				} catch (Exception e) {
					logger.error("onClose eventListener call failed", e);
				}
			}
		}
	}
	
	class ThreadedMessageProcessor {

		private Session session;
		private String webSocketID;
		private BlockingQueue<String> messageQueue = new LinkedBlockingQueue<String>();
		private boolean stop = false;
		
		public ThreadedMessageProcessor(Session session, String webSocketID) {
			this.session = session;
			this.webSocketID = webSocketID;
		}
		
		public void start() {
			new Thread(new SenderThread()).start();
		}
		
		public void stop() {
			stop = true;
		}
		
		public void processMessage(String message) {
			messageQueue.add(message);
		}
		
		private class SenderThread implements Runnable {
			public void run() {
				while (!stop) {
					String message = null;
					try {
						message = messageQueue.poll(1000, TimeUnit.MILLISECONDS);
					} catch (InterruptedException e) {
						logger.error("Interrupted while waiting for message ", e);
					}
					if (message != null) {
						for (WebSocketEventListener listener: eventListeners) {
							try {
								if (!stop) listener.onMessage(webSocketID, message);
							} catch (Exception e) {
								String errorMsg = "Unexpected Exception while processing incoming message " + e;
								logger.error(errorMsg, e);
								try {
									// If messages are attempted to be sent by multiple threads(for example, multiple clients)
									// to the same RemoteEndpoint, it can lead to blocking and throws the error:
									//     java.lang.IllegalStateException: Blocking message pending 10000 for BLOCKING
									// To alleviate this, use asynchronous, non-blocking methods that require us to check
									// if the send was successful.
									// https://bugs.eclipse.org/bugs/show_bug.cgi?id=474488
									Future<Void> sendFuture = session.getRemote().sendStringByFuture("ERROR: " + errorMsg);
									sendFuture.get(3, TimeUnit.SECONDS);	// Wait for completion
								} catch (Exception e1) {
									logger.error("Failed to send error message ", e1);
								}
							}
						}
					}
				}
			}
		}
	}
	
	class ThreadedMessageSender {

		private static final int CAPACITY = 5000;
		private Session session;
		private String webSocketID;
		private BlockingQueue<String> messageQueue = new LinkedBlockingQueue<String>(CAPACITY);
		private boolean stop = false;
		
		public ThreadedMessageSender(Session session, String webSocketID) {
			this.session = session;
			this.webSocketID = webSocketID;
		}
		
		public void start() {
			new Thread(new SenderThread()).start();
		}
		
		public void stop() {
			stop = true;
			messageQueue.clear();
		}
		
		public void sendMessage(String message) {
			boolean added = messageQueue.offer(message);
			if (!added) {
				logger.error("WebSocket: " + webSocketID + " Send Queue reached capacity: " + CAPACITY);
				logger.error("Closing session to protect against memory failure");
				this.session.close();
			}
		}
		
		private class SenderThread implements Runnable {
			public void run() {
				while (!stop) {
					String message = null;
					try {
						message = messageQueue.poll(1000, TimeUnit.MILLISECONDS);
					} catch (InterruptedException e) {
						logger.error("Interrupted while waiting for message ", e);
					}
					if (message != null) {
						try {
							// If messages are attempted to be sent by multiple threads(for example, multiple clients)
							// to the same RemoteEndpoint, it can lead to blocking and throws the error:
							//     java.lang.IllegalStateException: Blocking message pending 10000 for BLOCKING
							// To alleviate this, use asynchronous, non-blocking methods that require us to check
							// if the send was successful.
							// https://bugs.eclipse.org/bugs/show_bug.cgi?id=474488
							Future<Void> sendFuture = session.getRemote().sendStringByFuture(message);
							sendFuture.get(3, TimeUnit.SECONDS);	// Wait for completion
						} catch (Exception e) {
							logger.error("Failed to send message to session: " + 
									webSocketID + " error: " + e , e);
						}
					}
				}
			}
		}
	}
}
