package gov.usdot.cv.kluge;

public class XerJsonParserPathMissingException extends XerJsonParserException
{
    public XerJsonParserPathMissingException(String msg)
    {
        super(msg);
    }
    
    public XerJsonParserPathMissingException(String msg, Exception cause)
    {
        super(msg, cause);
    }
}
