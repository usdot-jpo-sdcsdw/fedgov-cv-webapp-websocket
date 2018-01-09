package gov.dot.its.jpo.sdcsdw.webfragment_websockets.kluge;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

/** Static methods for building a GeoJSON polygon from an unpacked ASD  
 * 
 * @author amm30955
 *
 */
public class GeoJsonBuilder
{
    /** Build a GeoJSON polygon from the NW and SE coordinates in an unpacked
     * JSON ASD
     * 
     * @param src JSON object containing an unpacked ASD
     * @return JSON object containing a GeoJSON polygon
     */
    public static JSONObject buildGeoJson(JSONObject src)
    {
        double north = src.getDouble(AsdCompleteXerParser.Fields.NW_LON.getField());
        double west = src.getDouble(AsdCompleteXerParser.Fields.NW_LAT.getField());
        double south = src.getDouble(AsdCompleteXerParser.Fields.SE_LON.getField());
        double east = src.getDouble(AsdCompleteXerParser.Fields.SE_LAT.getField());
        
        
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
    
    /** Make a GeoJSON point from a longitude and latitude 
     * 
     * @param lon Longitude
     * @param lat Latitude
     * @return GeoJSON point
     */
    private static JSONArray makeCoordinate(double lon, double lat)
    {
        JSONArray point = new JSONArray();
        point.add(lon);
        point.add(lat);
                
        return point;
    }
}
