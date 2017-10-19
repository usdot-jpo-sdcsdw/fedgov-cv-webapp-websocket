package gov.usdot.cv.websocket.jms.filter;

import java.util.ArrayList;
import java.util.List;

import net.sf.json.JSONObject;

import com.spatial4j.core.context.SpatialContext;
import com.spatial4j.core.shape.Rectangle;

public class FilterBuilder {

	private final static SpatialContext ctx = SpatialContext.GEO;
	private static List<Integer> dialogIDs = new ArrayList<Integer>();
	private static List<String> resultEncodings = new ArrayList<String>();

	static {
		dialogIDs.add(-1);
		dialogIDs.add(154);
		dialogIDs.add(162);
		
		resultEncodings.add("hex");
		resultEncodings.add("base64");
		resultEncodings.add("full");
	}
	
	public static Filter buildFilter(JSONObject json, String message) throws InvalidFilterException {
		validateFilterMessage(json, message);
		int dialogId = json.getInt("dialogID");
		int vsmType = json.containsKey("vsmType") ? json.getInt("vsmType") : 0;
		
		Rectangle boundingBox = null;
		if (json.containsKey("nwLat") && json.containsKey("nwLon") &&
			json.containsKey("seLat") && json.containsKey("seLon")) {
			double nwLat = json.getDouble("nwLat");
			double nwLon = json.getDouble("nwLon");
			double seLat = json.getDouble("seLat");
			double seLon = json.getDouble("seLon");
			boundingBox = ctx.makeRectangle(nwLon, seLon, seLat, nwLat);
		}
		
		String resultEncoding = json.optString("resultEncoding", "hex");
		return new Filter(dialogId, vsmType, boundingBox, resultEncoding);
	}
	
	private static JSONObject validateFilterMessage(JSONObject json, String message) throws InvalidFilterException {
		if (json.containsKey("dialogID")) {
			if (!dialogIDs.contains(json.getInt("dialogID"))) {
				throw new InvalidFilterException("Invalid dialogID: " + json.getInt("dialogID") + 
						", not one of the supported dialogIDs: " + dialogIDs);
			}
		} else {
			throw new InvalidFilterException("Missing dialogID in message: " + message);
		}
		
		if (json.containsKey("nwLat") || json.containsKey("nwLon") || json.containsKey("seLat") || json.containsKey("seLon")) {
			if (!json.containsKey("nwLat"))
				throw new InvalidFilterException("The field nwLat is required");
			if (!json.containsKey("nwLon"))
				throw new InvalidFilterException("The field nwLon is required");
			if (!json.containsKey("seLat"))
				throw new InvalidFilterException("The field seLat is required");
			if (!json.containsKey("seLon"))
				throw new InvalidFilterException("The field seLon is required");
		}
		
		if (json.containsKey("resultEncoding")) {
			String resultEncoding = json.getString("resultEncoding");
			boolean valid = false;
			for (String validEncoding: resultEncodings) {
				if (resultEncoding.equalsIgnoreCase(validEncoding)) {
					valid = true;
					break;
				}
			}
			if (!valid) {
				throw new InvalidFilterException("Invalid resultEncoding: " + resultEncoding + 
						", not one of the supported resultEncodinga: " + resultEncodings);
			}
		}
		
		return json;
	}
}
