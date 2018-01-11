/** LEGACY CODE
 * 
 * This was salvaged in part or in whole from the Legacy System. It will be heavily refactored or removed.
 */
package gov.dot.its.jpo.sdcsdw.websocketsfragment.server.utils;

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
