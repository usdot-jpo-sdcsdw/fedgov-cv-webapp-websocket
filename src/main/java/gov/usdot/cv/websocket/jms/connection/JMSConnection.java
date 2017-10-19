package gov.usdot.cv.websocket.jms.connection;

import gov.usdot.cv.resources.PrivateResourceLoader;
import gov.usdot.cv.websocket.jms.JMSConfig;
import gov.usdot.cv.websocket.jms.connection.ActiveMQSslTransportFactory;
import gov.usdot.cv.websocket.jms.connection.JMSConnection;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.Session;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.log4j.Logger;

/**
 * Utility class for encapsulating the work of creating a JMS Connection and adding a topic subscriber.
 */
public class JMSConnection {

	private static Logger logger = Logger.getLogger(JMSConnection.class);
	
	private Connection connection;
	private Session session;
	private MessageConsumer consumer;
	
	private JMSConfig config;
	
	public JMSConnection(JMSConfig config) {
		this.config = config;
	}
	
	public void openConnection() throws JMSException, 
		KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException, UnrecoverableKeyException, URISyntaxException {
		
		String userPassword;
		if(PrivateResourceLoader.isPrivateResource(config.password)) {
			userPassword = PrivateResourceLoader.getProperty(config.password);
		}
		else {
			userPassword = config.password;
		}
		ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory(config.userName, 
				userPassword, config.brokerURL);
		
		if (config.truststoreFile != null) {
			ActiveMQSslTransportFactory transportFactory = new ActiveMQSslTransportFactory();
			transportFactory.initialize(config);
		}
		connection = factory.createConnection();
		
		logger.info("JMS connection opened to " + config.systemName + " " + config.brokerURL);
	}
	
	public void closeConnection() {
		try {consumer.close();} catch (Exception ignore) { }
		try {session.close();} catch (Exception ignore) { }
		try {connection.close();} catch (Exception ignore) { }
		consumer = null;
		connection = null;
		session = null;
		logger.info("JMS connection closed to " + config.brokerURL);
	}
	
	public void addTopicListener(String topic, MessageListener listener) throws JMSException {
		if (connection != null) {
			if (session == null) {
				session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
			}
			Destination dest = session.createTopic(topic);
			consumer = session.createConsumer(dest, null, false);
			consumer.setMessageListener(listener);
			logger.info("Listener added to topic " + config.topic);
		}
	}
	
	public void startConnection() throws JMSException {
		if (connection != null) {
			connection.start();
			logger.info("JMS connection started");
		}
	}
}
