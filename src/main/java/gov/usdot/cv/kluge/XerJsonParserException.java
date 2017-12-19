package gov.usdot.cv.kluge;

public class XerJsonParserException extends Exception
{
    public XerJsonParserException(String msg)
    {
        super(msg);
    }
    
    public XerJsonParserException(String msg, Exception cause)
    {
        super(msg, cause);
    }
}
