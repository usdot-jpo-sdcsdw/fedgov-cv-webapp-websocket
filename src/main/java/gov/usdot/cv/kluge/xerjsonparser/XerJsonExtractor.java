package gov.usdot.cv.kluge.xerjsonparser;

import org.w3c.dom.Document;

import net.sf.json.JSONObject;

/** Extracts a value from a specific path in XER into a specific key in JSON 
 * 
 * @author amm30955
 *
 */
public class XerJsonExtractor
{
    /** Create a new extractor
     * 
     * @param parser Function to use to extract
     * @param field JSON key to store into
     * @param path XER path to extract
     */
    public XerJsonExtractor(XerJsonParser parser, String field, String path)
    {
        this.parser = parser;
        this.field = field;
        this.path = path;
    }
    
    /** Create a new extractor 
     * 
     * @param parser Function to use to extract
     * @param pair Pair of JSON key and XER path to extract
     */
    public XerJsonExtractor(XerJsonParser parser, FieldPathPair pair)
    {
        this(parser, pair.getField(), pair.getPath());
    }
    
    /** Run the extractor
     * 
     * @param obj JSON object to extract into
     * @param doc XER document to extract from
     * @throws XerJsonParserException XER path was not found or the value could not be parsed
     */
    public void extract(JSONObject obj, Document doc) throws XerJsonParserException
    {
        parser.parseXer(obj, field, doc, path);
    }
    
    /** Function to use to extract */
    private final XerJsonParser parser;
    /** JSON key to store into */
    private final String field;
    /** XER path to extract */
    private final String path;
}
