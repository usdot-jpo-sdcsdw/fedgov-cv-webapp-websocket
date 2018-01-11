package gov.dot.its.jpo.sdcsdw.websocketsfragment.service.xerjsonparser;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.function.Function;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import net.sf.json.JSONObject;

/** Collection of parsers and utility methods for extracting JSON from XER
 * 
 * @author amm30955
 *
 */
public class XerJsonParsers
{
    /** Parser for string fields */
    public static final XerJsonParser StringXerJsonParser = (JSONObject obj, String field, Document doc, String path) ->
    {
        obj.put(field, getXerString(path, doc));
    };
    
    /** Parser for base-10 long fields */
    public static final XerJsonParser LongXerJsonParser = (JSONObject obj, String field, Document doc, String path) ->
    {
        obj.put(field, getXerLong(path, doc));
    };
    
    /** Parser for base-16 int fields */
    public static final XerJsonParser HexIntXerJsonParser = (JSONObject obj, String field, Document doc, String path) ->
    {
        obj.put(field, getXerInt(path, doc, 16));
    };
    
    /** Parser for base-2 int fields */
    public static final XerJsonParser BitIntXerJsonParser = (JSONObject obj, String field, Document doc, String path) ->
    {
        obj.put(field, getXerInt(path, doc, 2));
    };
    
    /** Parser for double fields */
    public static final XerJsonParser DoubleXerJsonParser = (JSONObject obj, String field, Document doc, String path) ->
    {
        obj.put(field, getXerDouble(path, doc));
    };
    
    /** Parser for coordinates with a scaling factor of 1e7 */
    public static final XerJsonParser CoordinateXerJsonParser = (JSONObject obj, String field, Document doc, String path) ->
    {
        obj.put(field, getXerCoordinate(path, doc));
    };
    
    /** Parser for DFullTime fields */
    public static final XerJsonParser DateXerJsonParser = (JSONObject obj, String field, Document doc, String path) ->
    {
        obj.put(field, getXerDate(path, doc));
    };
    
    /** Parser for enum fields which are not 1-to-1
     * 
     * @param nameMap Map from string values to enum values
     * @param intMap Map from enum values to int values
     * @return Parser
     */
    public static final <E extends Enum<E>> XerJsonParser EnumXerJsonParser(Function<String, E> nameMap, Function<E, Integer> intMap)
    {
        return (JSONObject obj, String field, Document doc, String path) ->
        {
            obj.put(field, intMap.apply(nameMap.apply(getXerEnum(path, doc))));
        };
    }
    
    /** Parser for enum fields which are 1-to-1
     * 
     * @param nameMap Map from string values to enum values
     * @return Parser
     */
    public static final <E extends Enum<E>> XerJsonParser EnumXerJsonParser(Function<String, E> nameMap)
    {
        return (JSONObject obj, String field, Document doc, String path) ->
        {
            E enumValue = nameMap.apply(getXerEnum(path, doc));
            
            if (enumValue == null) {
                throw new XerJsonParserBadTypeException("Not a valid enum, was not any of the possible values");
            }
            
            obj.put(field, enumValue.ordinal());
        };
    }
    
    /** Make a parser optional so that it will not fail if the field is missing, but still fail if it can't be parsed
     * 
     * @param parser Parser to make optional
     * @return Optional version of the parser
     */
    public static XerJsonParser optional(XerJsonParser parser)
    {
        return new XerJsonParser() {

            @Override
            public void parseXer(JSONObject obj,
                                 String field,
                                 Document src,
                                 String path) throws XerJsonParserException
            {
                try {
                    parser.parseXer(obj, field, src, path);
                } catch (XerJsonParserPathMissingException ex) {
                    // Do nothing
                }
            }
        };
    }
    
    /** Reciprocal of the scaling factor for coordinates */
    private static double INVERSE_SCALING_FACTOR = 1e7;
    /** XPath instance for extracting XER fields */
    private static final XPath xPath = XPathFactory.newInstance().newXPath();
    
    /** Extract a string from XER
     * 
     * @param path Path to extract
     * @param doc Document to extract from
     * @return String at that path in the document
     * @throws XerJsonParserException If the path could not be found
     */
    private static final String getXerString(String path, Document doc) throws XerJsonParserException
    {
        try {
            Node node = (Node)xPath.evaluate(path, doc.getDocumentElement(), XPathConstants.NODE);
            if (node == null) {
                throw new XerJsonParserPathMissingException("No element at path: " + path);
            } else {
                return node.getTextContent();
            }
        } catch (XPathExpressionException ex) {
            throw new RuntimeException("Internal error: Bad XPath", ex);
        }
    }
    
    /** Extract an enum from XER
     * 
     * @param path Path to extract
     * @param doc Document to extract from
     * @return Enum string at that path in the document
     * @throws XerJsonParserException If the path could not be found
     */
    private static final String getXerEnum(String path, Document doc) throws XerJsonParserException
    {
        try {
            Node node = (Node)xPath.evaluate(path, doc.getDocumentElement(), XPathConstants.NODE);
            if (node == null) {
                throw new XerJsonParserPathMissingException("No element at path: " + path);
            } else if (node.getFirstChild() == null) {
                throw new XerJsonParserBadTypeException("Not a valid enum");
            } else if (node.getFirstChild().getNodeType() == Node.ELEMENT_NODE) {
                Element element = (Element)(node.getFirstChild());
                return element.getTagName();
            } else {
                throw new XerJsonParserBadTypeException("Node at path " + path + " was not an Enum node");
            }
        } catch (XPathExpressionException ex) {
            throw new RuntimeException("Internal error: Bad XPath", ex);
        }
    }
    
    /** Extract a base-10 int from XER
     * 
     * @param path Path to extract
     * @param doc Document to extract from
     * @return Integer at that path in the document
     * @throws XerJsonParserException If the path could not be found
     */
    private static final int getXerInt(String path, Document doc) throws XerJsonParserException
    {
        String stringValue = getXerString(path, doc);
        try {
            return Integer.parseInt(stringValue);
        } catch (NumberFormatException ex) {
            throw new XerJsonParserBadTypeException("Could not parse " + stringValue + " at path " + path + " as an Integer", ex);
        }
    }
    
    /** Extract an int from XER with an arbitrary base
     * 
     * @param path Path to extract
     * @param doc Document to extract from
     * @param radix Base of the integer to extract
     * @return Integer at that path in the document
     * @throws XerJsonParserException If the path could not be found
     */
    private static final int getXerInt(String path, Document doc, int radix) throws XerJsonParserException
    {
        String stringValue = getXerString(path, doc);
        try {
            return Integer.parseUnsignedInt(stringValue, radix);
        } catch (NumberFormatException ex) {
            throw new XerJsonParserBadTypeException("Could not parse " + stringValue + " at path " + path + " as a base-" + radix + " Integer", ex);
        }
    }
    
    /** Extract a long from XER
     * 
     * @param path Path to extract
     * @param doc Document to extract from
     * @return Long at that path in the document
     * @throws XerJsonParserException If the path could not be found
     */
    private static final long getXerLong(String path, Document doc) throws XerJsonParserException
    {
        String stringValue = getXerString(path, doc);
        try {
            return Long.parseLong(stringValue);
        } catch (NumberFormatException ex) {
            throw new XerJsonParserBadTypeException("Could not parse " + stringValue + " at path " + path + " as a Long", ex);
        }
    }
    
    /** Extract a double from XER
     * 
     * @param path Path to extract
     * @param doc Document to extract from
     * @return Double at that path in the document
     * @throws XerJsonParserException If the path could not be found
     */
    private static final double getXerDouble(String path, Document doc) throws XerJsonParserException
    {
        String stringValue = getXerString(path, doc);
        try {
            return Double.parseDouble(stringValue);
        } catch (NumberFormatException ex) {
            throw new XerJsonParserBadTypeException("Could not parse " + stringValue + " at path " + path + " as a Double", ex);
        }
    }
    
    /** Extract a Coordinate from XER
     * 
     * @param path Path to extract
     * @param doc Document to extract from
     * @return Coordinate at that path in the document
     * @throws XerJsonParserException If the path could not be found
     */
    private static final double getXerCoordinate(String path, Document doc) throws XerJsonParserException
    {
        return getXerDouble(path, doc) / INVERSE_SCALING_FACTOR;
    }
    
    private static final String YEAR_PART = "year";
    private static final String MONTH_PART = "month";
    private static final String DAY_PART = "day";
    private static final String HOUR_PART = "hour";
    private static final String MINUTE_PART = "minute"; 
    
    private static final String DATE_PATTERN = "yyyy-MM-dd'T'HH:mm:ss";
    private static final DateFormat DATE_FORMAT = new SimpleDateFormat(DATE_PATTERN);
    
    static {
        DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
    }
    
    /** Extract a Date from XER
     * 
     * @param path Path to extract
     * @param doc Document to extract from
     * @return Date at that path in the document
     * @throws XerJsonParserException If the path could not be found
     */
    private static final String getXerDate(String path, Document doc) throws XerJsonParserException
    {
        int year;
        int month;
        int day;
        int hour;
        int minute;
        
        try {
            year = getXerInt(path + "/" + YEAR_PART, doc);
            month = getXerInt(path + "/" + MONTH_PART, doc);
            day = getXerInt(path + "/" + DAY_PART, doc);
            hour = getXerInt(path + "/" + HOUR_PART, doc);
            minute = getXerInt(path + "/" + MINUTE_PART, doc);
        }
        catch (XerJsonParserPathMissingException ex) {
            throw new XerJsonParserBadTypeException("Not a valid date", ex);
        }
        catch (XerJsonParserBadTypeException ex) {
            throw new XerJsonParserBadTypeException("Not a valid date", ex);
        }
        
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        
        cal.set(year,
                month-1,
                day,
                hour,
                minute,
                0);
        cal.set(Calendar.MILLISECOND, 0);
        
        return DATE_FORMAT.format(cal.getTime());
    }
}
