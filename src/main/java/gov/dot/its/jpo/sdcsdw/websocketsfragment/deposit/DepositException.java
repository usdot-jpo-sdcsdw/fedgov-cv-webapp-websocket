/** LEGACY CODE
 * 
 * This was salvaged in part or in whole from the Legacy System. It will be heavily refactored or removed.
 */
package gov.dot.its.jpo.sdcsdw.websocketsfragment.deposit;

public class DepositException extends Exception {

	private static final long serialVersionUID = 7250633998326302977L;

	public DepositException(String message) {
		super(message);
    }
	
	public DepositException(Throwable cause) {
		super(cause);
    }

    public DepositException(String message, Throwable cause) {
        super(message, cause);
    }
}
