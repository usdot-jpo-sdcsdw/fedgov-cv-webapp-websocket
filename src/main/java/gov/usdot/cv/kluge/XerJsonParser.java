package gov.usdot.cv.kluge;

import org.w3c.dom.Document;

import net.sf.json.JSONObject;

public interface XerJsonParser
{
    void parseXer(JSONObject obj, String field, Document doc, String path) throws XerJsonParserException;
}
