package gov.usdot.cv.kluge;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import gov.usdot.cv.mongodb.datasink.model.TimeToLive;

public class TimeExtractor
{

    public static final String START_TIME = "startTime";
    public static final String STOP_TIME = "stopTime";
    
    public static Date extractDateTime(Document doc, String subPath)
    {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        try {
            cal.set(getTimePart(doc, subPath, YEAR_PART),
                    getTimePart(doc, subPath, MONTH_PART)-1,
                    getTimePart(doc, subPath, DAY_PART),
                    getTimePart(doc, subPath, HOUR_PART),
                    getTimePart(doc, subPath, MINUTE_PART),
                    0);
            cal.set(Calendar.MILLISECOND, 0);
            
            return cal.getTime();
        } catch (XPathExpressionException e) {
            return null;
        }
    }
    
    public static TimeToLive extractTimeToLiveCode(Document doc)
    {
        try {
            Node node = (Node)xPath.evaluate(TTL_PATH, doc.getDocumentElement(), XPathConstants.NODE);
            if (node.getFirstChild() != null && node.getFirstChild().getNodeType() == node.ELEMENT_NODE) {
                Element element = (Element)(node.getFirstChild());
                return TimeToLive.fromString(element.getTagName());
            } else {
                return null;
            }
        } catch (XPathExpressionException e) {
            return null;
        }
    }
    
    private static final XPath xPath = XPathFactory.newInstance().newXPath();
    
    private static final String ASD_PATH = "/AdvisorySituationData";
    
    
    private static final String TTL_PATH = ASD_PATH + "/timeToLive";
    
    private static final String ASDM_DETAILS_PATH = ASD_PATH + "/asdmDetails"; 
    private static final String YEAR_PART = "year";
    private static final String MONTH_PART = "month";
    private static final String DAY_PART = "day";
    private static final String HOUR_PART = "hour";
    private static final String MINUTE_PART = "minute";
    private static String MAKE_TIME_PART_PATH(String subPath, String partPath)
    {
        return  ASDM_DETAILS_PATH + "/" + subPath + "/" + partPath;
    }  
    
    
    private static int getTimePart(Document doc, String subPath, String partPath) throws XPathExpressionException
    {
        Node node = (Node)xPath.evaluate(MAKE_TIME_PART_PATH(subPath, partPath), doc.getDocumentElement(), XPathConstants.NODE);
        return Integer.parseInt(node.getTextContent());
    }
    
    private TimeExtractor() { }
}
