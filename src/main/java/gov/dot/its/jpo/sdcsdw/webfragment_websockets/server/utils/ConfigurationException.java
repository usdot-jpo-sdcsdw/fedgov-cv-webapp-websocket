package gov.dot.its.jpo.sdcsdw.webfragment_websockets.server.utils;

public class ConfigurationException extends Exception {

	private static final long serialVersionUID = -1011704637837676718L;

	public ConfigurationException(String message) {
		super(message);
    }
	
	public ConfigurationException(Throwable cause) {
		super(cause);
    }

    public ConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
}
