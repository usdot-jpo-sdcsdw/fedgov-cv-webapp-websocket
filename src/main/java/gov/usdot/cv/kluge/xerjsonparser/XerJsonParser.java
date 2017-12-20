package gov.usdot.cv.kluge.xerjsonparser;

import org.w3c.dom.Document;

import net.sf.json.JSONObject;

/** Functional interface for extracting a value at a path in XER into a JSON object 
 * 
 * @author amm30955
 *
 */
public interface XerJsonParser
{
    /** Extract a XER value into a JSON object
     * 
     * @param obj JSON object to extract into
     * @param field JSON key to extract to
     * @param doc XER document to extract from
     * @param path XPath to extract from
     * @throws XerJsonParserException If the value is missing or could not be parsed
     */
    void parseXer(JSONObject obj, String field, Document doc, String path) throws XerJsonParserException;
}
