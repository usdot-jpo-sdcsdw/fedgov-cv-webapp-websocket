package gov.usdot.cv.kluge;

import net.sf.json.JSON;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;


public class GeoJsonExtractor
{
    public static JSONObject buildRegionFromAsdXml(Document doc)
    {
        try {
            JSONArray nw = extractCorner(doc, NW_CORNER);
            JSONArray se = extractCorner(doc, SE_CORNER);
            JSONArray ne = joinCorners(nw, se);
            JSONArray sw = joinCorners(se, nw);
            
            JSONArray coordinates = new JSONArray();
            coordinates.add(nw);
            coordinates.add(ne);
            coordinates.add(se);
            coordinates.add(sw);
            coordinates.add(nw);
            
            JSONArray coordinatesWrapper = new JSONArray();
            coordinatesWrapper.add(coordinates);
             
           JSONObject obj = new JSONObject();
            
            obj.put("type", "Polygon");
            obj.put("coordinates", coordinatesWrapper);
            
            return obj;
        } catch(XPathExpressionException ex) {
            return null;
        }
    }
    
    private static final String SERVICE_REGION_PATH = "/AdvisorySituationData/serviceRegion/";
    private static final String NW_CORNER = "nwCorner";
    private static final String NE_CORNER = "neCorner";
    private static final String SW_CORNER = "swCorner";
    private static final String SE_CORNER = "seCorner";
    private static final String LAT = "lat";
    private static final String LONG = "long";
    private static double INVERSE_SCALING_FACTOR = 1e7;
    
    
    private static final String MAKE_COORDINATE_PATH(String corner, String coord)
    {
        return SERVICE_REGION_PATH + corner + "/" + coord;
    }
    
    private static final XPath xPath = XPathFactory.newInstance().newXPath();
    
    private static JSONArray extractCorner(Document doc, String corner) throws XPathExpressionException
    {
        double coordLong = extractCoordinate(doc, corner, LONG);
        double coordLat = extractCoordinate(doc, corner, LAT);
        JSONArray coordinate = new JSONArray();
        coordinate.add(coordLong);
        coordinate.add(coordLat);
           
        return coordinate;
    }
    
    private static JSONArray joinCorners(JSONArray corner1, JSONArray corner2)
    {
        JSONArray outCorner = new JSONArray();
        outCorner.add(corner1.get(0));
        outCorner.add(corner2.get(1));
        
        return outCorner;
    }
      
    private static double extractCoordinate(Document doc, String corner, String coord) throws XPathExpressionException
    {
        Node node = (Node)xPath.evaluate(MAKE_COORDINATE_PATH(corner, coord), doc.getDocumentElement(), XPathConstants.NODE);
        return Double.parseDouble(node.getTextContent()) / INVERSE_SCALING_FACTOR;
    }
}