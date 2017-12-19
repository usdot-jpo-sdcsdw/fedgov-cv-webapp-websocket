package gov.usdot.cv.kluge;

public class XerJsonParserBadTypeException extends XerJsonParserException
{
    public XerJsonParserBadTypeException(String msg)
    {
        super(msg);
    }
    
    public XerJsonParserBadTypeException(String msg, Exception cause)
    {
        super(msg, cause);
    }
}
