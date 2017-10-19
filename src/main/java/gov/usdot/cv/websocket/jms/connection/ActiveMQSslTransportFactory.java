package gov.usdot.cv.websocket.jms.connection;

import gov.usdot.cv.resources.PrivateResourceLoader;
import gov.usdot.cv.websocket.jms.JMSConfig;
import gov.usdot.cv.websocket.server.utils.ConfigUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import javax.net.ServerSocketFactory;
import javax.net.SocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import org.apache.activemq.broker.SslContext;
import org.apache.activemq.transport.TransportFactory;
import org.apache.activemq.transport.tcp.SslTransportFactory;
import org.apache.activemq.util.IOExceptionSupport;

/**
 * Assists in setting up the SSL connection to ActiveMQ. 
 *
 */
public class ActiveMQSslTransportFactory extends SslTransportFactory {

    private SslContext sslContext = null;
    
    public ActiveMQSslTransportFactory() {
    	super();
    }
    
    public void initialize(JMSConfig config) throws URISyntaxException, KeyStoreException, 
    	NoSuchAlgorithmException, CertificateException, IOException, UnrecoverableKeyException  {
    	InputStream truststore;
    	if(PrivateResourceLoader.isPrivateResource(config.truststoreFile)) {
    		truststore = PrivateResourceLoader.getFileAsStream(config.truststoreFile);
		}
		else {
			truststore = ConfigUtils.getFileAsStream(config.truststoreFile);
		}
    	String truststorePassword;
    	if(PrivateResourceLoader.isPrivateResource(config.truststorePassword)) {
    		truststorePassword = PrivateResourceLoader.getProperty(config.truststorePassword);
		}
		else {
			truststorePassword = config.truststorePassword;
		}
		TrustManager[] trustManagers = getTrustManagers(truststore, truststorePassword);
		SecureRandom secureRandom = new SecureRandom();
		
		sslContext = new SslContext(null, trustManagers, secureRandom);
        TransportFactory.registerTransportFactory("ssl", this);
    }
    
    @Override
    protected ServerSocketFactory createServerSocketFactory() throws IOException {
    	
        if (null != sslContext) {
            try {
                return sslContext.getSSLContext().getServerSocketFactory();
            } catch (Exception e) {
                throw IOExceptionSupport.create(e);
            }
        }
        
        return super.createServerSocketFactory();
        
    }

    @Override
    protected SocketFactory createSocketFactory() throws IOException {
    	
        if (null != sslContext) {
            try {
                return sslContext.getSSLContext().getSocketFactory();
            } catch (Exception e) {
                throw IOExceptionSupport.create(e);
            }
        }
        
        return super.createSocketFactory();
        
    }

	private TrustManager[] getTrustManagers(InputStream trustStoreStream,
			String trustStorePassword) throws IOException, KeyStoreException,
			NoSuchAlgorithmException, CertificateException {

		KeyStore trustStore = KeyStore.getInstance("JKS");
		char[] trustStorePwd = (trustStorePassword != null) ? trustStorePassword
				.toCharArray() : null;
		trustStore.load(trustStoreStream, trustStorePwd);
		TrustManagerFactory trustManagerFactory = TrustManagerFactory
				.getInstance(TrustManagerFactory.getDefaultAlgorithm());
		trustManagerFactory.init(trustStore);

		return trustManagerFactory.getTrustManagers();

	}
}