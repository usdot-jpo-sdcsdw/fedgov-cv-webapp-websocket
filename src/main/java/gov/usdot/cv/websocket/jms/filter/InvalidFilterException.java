package gov.usdot.cv.websocket.jms.filter;

public class InvalidFilterException extends Exception {

	private static final long serialVersionUID = 1055350051174393468L;

	public InvalidFilterException(String message) {
		super(message);
    }
	
	public InvalidFilterException(Throwable cause) {
		super(cause);
    }

    public InvalidFilterException(String message, Throwable cause) {
        super(message, cause);
    }
}
