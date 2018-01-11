package gov.dot.its.jpo.sdcsdw.websocketsfragment.service.xerjsonparser;

/** Exception thrown when trying to extract JSON from XER  
 * 
 * @author amm30955
 *
 */
public class XerJsonParserException extends Exception
{
    /**
     * 
     */
    private static final long serialVersionUID = 984515492251910936L;

    /** Create a new exception
     * 
     * @param msg Message
     */
    public XerJsonParserException(String msg)
    {
        super(msg);
    }
    
    /** Create a new exception
     * 
     * @param msg Message
     * @param cause Underlying exception
     */
    public XerJsonParserException(String msg, Exception cause)
    {
        super(msg, cause);
    }
}
