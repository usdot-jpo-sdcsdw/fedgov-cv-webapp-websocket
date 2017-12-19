package gov.usdot.cv.kluge;

import javax.xml.xpath.XPathExpressionException;

import org.w3c.dom.Document;

import net.sf.json.JSONObject;

public class XerJsonExtractor
{
    public XerJsonExtractor(XerJsonParser parser, String field, String path)
    {
        this.parser = parser;
        this.field = field;
        this.path = path;
    }
    
    public void extract(JSONObject obj, Document doc) throws XerJsonParserException
    {
        parser.parseXer(obj, field, doc, path);
    }
    
    private final XerJsonParser parser;
    private final String field;
    private final String path;
}
