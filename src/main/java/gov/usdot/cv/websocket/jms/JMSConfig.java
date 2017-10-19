package gov.usdot.cv.websocket.jms;


/**
 * Parameters for connecting to a JMS Server.
 */
public class JMSConfig {

	public String systemName;
	public String userName;
	public String password;
	public String brokerURL;
	public String truststoreFile;
	public String truststorePassword;
	public String topic;
	
	public JMSConfig() {
		super();
	}

	@Override
	public String toString() {
		return "JMSConfig [systemName=" + systemName + ", userName=" + userName
				+ ", password=" + password + ", brokerURL=" + brokerURL
				+ ", truststoreFile=" + truststoreFile
				+ ", truststorePassword=" + truststorePassword + ", topic="
				+ topic + "]";
	}

}
