package gov.dot.its.jpo.sdcsdw.webfragment_websockets.kluge.xerjsonparser;

/** Exception thrown when a required field is not present when extracting JSON from XER 
 * 
 * @author amm30955
 *
 */
public class XerJsonParserPathMissingException extends XerJsonParserException
{
    /**
     * 
     */
    private static final long serialVersionUID = 8935906984882591131L;

    /** Create a new exception
     * 
     * @param msg Message
     */
    public XerJsonParserPathMissingException(String msg)
    {
        super(msg);
    }
    
    /** Create a new exception
     * 
     * @param msg Message
     * @param cause Underlying exception
     */
    public XerJsonParserPathMissingException(String msg, Exception cause)
    {
        super(msg, cause);
    }
}
