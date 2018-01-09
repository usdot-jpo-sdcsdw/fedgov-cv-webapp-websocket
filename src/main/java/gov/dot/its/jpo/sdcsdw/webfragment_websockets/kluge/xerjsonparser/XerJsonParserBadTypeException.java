package gov.dot.its.jpo.sdcsdw.webfragment_websockets.kluge.xerjsonparser;

/** Exception thrown when a field could not be parsed when extracting JSON from XER
 * 
 * @author amm30955
 *
 */
public class XerJsonParserBadTypeException extends XerJsonParserException
{
    /**
     * 
     */
    private static final long serialVersionUID = 1801895544695239337L;

    /** Create a new exception
     * 
     * @param msg Message
     */
    public XerJsonParserBadTypeException(String msg)
    {
        super(msg);
    }
    
    /** Create a new exception
     * 
     * @param msg Message
     * @param cause Underlying exception
     */
    public XerJsonParserBadTypeException(String msg, Exception cause)
    {
        super(msg, cause);
    }
}
