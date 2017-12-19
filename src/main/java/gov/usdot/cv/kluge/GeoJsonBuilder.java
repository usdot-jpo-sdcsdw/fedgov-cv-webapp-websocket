package gov.usdot.cv.kluge;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

public class GeoJsonBuilder
{
    public static JSONObject buildGeoJson(JSONObject src)
    {
        double north = src.getDouble(AsdCompleteXerParser.JsonFields.NW_LON);
        double west = src.getDouble(AsdCompleteXerParser.JsonFields.NW_LAT);
        double south = src.getDouble(AsdCompleteXerParser.JsonFields.SE_LON);
        double east = src.getDouble(AsdCompleteXerParser.JsonFields.SE_LAT);
        
        
        JSONArray coordinates = new JSONArray();
        coordinates.add(makeCoordinate(north, west));
        coordinates.add(makeCoordinate(north, east));
        coordinates.add(makeCoordinate(south, east));
        coordinates.add(makeCoordinate(south, west));
        coordinates.add(makeCoordinate(north, west));
        
        JSONArray coordinatesWrapper = new JSONArray();
        coordinatesWrapper.add(coordinates);
         
       JSONObject obj = new JSONObject();
        
        obj.put("type", "Polygon");
        obj.put("coordinates", coordinatesWrapper);
        
        return obj;
    }
    
    private static JSONArray makeCoordinate(double lon, double lat)
    {
        JSONArray point = new JSONArray();
        point.add(lon);
        point.add(lat);
                
        return point;
    }
}
