package gov.usdot.cv.kluge;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
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

public class XerJsonParsers
{
    public static final XerJsonParser StringXerJsonParser = (JSONObject obj, String field, Document doc, String path) ->
    {
        obj.put(field, getXerString(path, doc));
    };
    
    public static final XerJsonParser LongXerJsonParser = (JSONObject obj, String field, Document doc, String path) ->
    {
        obj.put(field, getXerLong(path, doc));
    };
    
    public static final XerJsonParser HexIntXerJsonParser = (JSONObject obj, String field, Document doc, String path) ->
    {
        obj.put(field, getXerHexInt(path, doc));
    };
    
    public static final XerJsonParser BitIntXerJsonParser = (JSONObject obj, String field, Document doc, String path) ->
    {
        obj.put(field, getXerBitInt(path, doc));
    };
    
    public static final XerJsonParser DoubleXerJsonParser = (JSONObject obj, String field, Document doc, String path) ->
    {
        obj.put(field, getXerDouble(path, doc));
    };
    
    public static final XerJsonParser CoordinateXerJsonParser = (JSONObject obj, String field, Document doc, String path) ->
    {
        obj.put(field, getXerCoordinate(path, doc));
    };
    
    public static final XerJsonParser DateXerJsonParser = (JSONObject obj, String field, Document doc, String path) ->
    {
        obj.put(field, getXerDate(path, doc));
    };
    
    /*public static final XerJsonParser EnumXerJsonParser(Function<String, Integer> nameMap)
    {
        return (JSONObject obj, String field, Document doc, String path) ->
        {
            obj.put(field, nameMap.apply(getXerEnum(path, doc)));
        };
    }*/
    
    public static final <E extends Enum<E>> XerJsonParser EnumXerJsonParser(Function<String, E> nameMap, Function<E, Integer> intMap)
    {
        return (JSONObject obj, String field, Document doc, String path) ->
        {
            obj.put(field, intMap.apply(nameMap.apply(getXerEnum(path, doc))));
        };
    }
    
    public static final <E extends Enum<E>> XerJsonParser EnumXerJsonParser(Function<String, E> nameMap)
    {
        return (JSONObject obj, String field, Document doc, String path) ->
        {
            obj.put(field, nameMap.apply(getXerEnum(path, doc)).ordinal());
        };
    }
    
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
    
    private static double INVERSE_SCALING_FACTOR = 1e7;
    private static final XPath xPath = XPathFactory.newInstance().newXPath();
    
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
    
    private static final String getXerEnum(String path, Document doc) throws XerJsonParserException
    {
        try {
            Node node = (Node)xPath.evaluate(path, doc.getDocumentElement(), XPathConstants.NODE);
            if (node.getFirstChild() != null && node.getFirstChild().getNodeType() == Node.ELEMENT_NODE) {
                Element element = (Element)(node.getFirstChild());
                return element.getTagName();
            } else {
                return null;
            }
        } catch (XPathExpressionException ex) {
            throw new RuntimeException("Internal error: Bad XPath", ex);
        }
    }
    
    private static final int getXerInt(String path, Document doc) throws XerJsonParserException
    {
        String stringValue = getXerString(path, doc);
        try {
            return Integer.parseInt(stringValue);
        } catch (NumberFormatException ex) {
            throw new XerJsonParserBadTypeException("Could not parse " + stringValue + " at path " + path + " as an Integer", ex);
        }
    }
    
    private static final int getXerHexInt(String path, Document doc) throws XerJsonParserException
    {
        String stringValue = getXerString(path, doc);
        try {
            return Integer.parseUnsignedInt(stringValue, 16);
        } catch (NumberFormatException ex) {
            throw new XerJsonParserBadTypeException("Could not parse " + stringValue + " at path " + path + " as a Hex Integer", ex);
        }
    }
    
    private static final int getXerBitInt(String path, Document doc) throws XerJsonParserException
    {
        String stringValue = getXerString(path, doc);
        try {
            return Integer.parseUnsignedInt(stringValue, 2);
        } catch (NumberFormatException ex) {
            throw new XerJsonParserBadTypeException("Could not parse " + stringValue + " at path " + path + " as a Bit Integer", ex);
        }
    }
    
    private static final long getXerLong(String path, Document doc) throws XerJsonParserException
    {
        String stringValue = getXerString(path, doc);
        try {
            return Long.parseLong(stringValue);
        } catch (NumberFormatException ex) {
            throw new XerJsonParserBadTypeException("Could not parse " + stringValue + " at path " + path + " as a Long", ex);
        }
    }
    
    private static final double getXerDouble(String path, Document doc) throws XerJsonParserException
    {
        String stringValue = getXerString(path, doc);
        try {
            return Double.parseDouble(stringValue);
        } catch (NumberFormatException ex) {
            throw new XerJsonParserBadTypeException("Could not parse " + stringValue + " at path " + path + " as a Double", ex);
        }
    }
    
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
    
    private static final String getXerDate(String path, Document doc) throws XerJsonParserException
    {
        int year = getXerInt(path + "/" + YEAR_PART, doc);
        int month = getXerInt(path + "/" + MONTH_PART, doc);
        int day = getXerInt(path + "/" + DAY_PART, doc);
        int hour = getXerInt(path + "/" + HOUR_PART, doc);
        int minute = getXerInt(path + "/" + MINUTE_PART, doc);
        
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
