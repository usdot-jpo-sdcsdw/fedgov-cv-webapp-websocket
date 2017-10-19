package gov.usdot.cv.websocket.jms.format;

import java.math.BigDecimal;
import java.nio.ByteBuffer;

import org.apache.commons.codec.binary.Hex;
import org.apache.log4j.Logger;

import gov.usdot.asn1.generated.j2735.dsrc.Position3D;
import gov.usdot.asn1.generated.j2735.semi.VehSitDataMessage;
import gov.usdot.asn1.generated.j2735.semi.VehSitRecord;
import gov.usdot.asn1.j2735.J2735Util;
import gov.usdot.cv.common.asn1.TransmissionAndSpeedHelper;
import net.sf.json.JSONObject;

public class VehicleFormatter {

	private static Logger logger = Logger.getLogger(VehicleFormatter.class);
	
	private static final double MAX_LAT = 90.0;
	private static final double MIN_LAT = -90.0;
	private static final double MAX_LON = 180.0;
	private static final double MIN_LON = -180.0;
	
	private static final String REQUEST_ID		= "requestId";
	private static final String TEMP_ID			= "tempId";
	private static final String GROUP_ID		= "groupId";
	private static final String LAT 			= "lat";
	private static final String LON 			= "long";
	private static final String SPEED 			= "speed";
	private static final String HEADING 		= "heading";
	
	public static JSONObject parseMessage(VehSitDataMessage vehSitData) {
		JSONObject jsonMessage = new JSONObject();
		jsonMessage.element(REQUEST_ID, String.valueOf(ByteBuffer.wrap(vehSitData.getRequestID().byteArrayValue()).getInt()));
		jsonMessage.element(TEMP_ID, Hex.encodeHexString(vehSitData.getBundle().get(0).getTempID().byteArrayValue()));
		jsonMessage.element(GROUP_ID, Hex.encodeHexString(vehSitData.getGroupID().byteArrayValue()));
				
		if (vehSitData.getBundle() != null && vehSitData.getBundle().getSize() > 0) {
			VehSitRecord vsr = vehSitData.getBundle().get(0);
			Position3D pos = vsr.getPos();
			Double lat = J2735Util.convertGeoCoordinateToDouble(pos.getLat().intValue());
			Double lon = J2735Util.convertGeoCoordinateToDouble(pos.get_long().intValue());
			String errorString = "";
			if (lat < MIN_LAT || lat > MAX_LAT) {
				errorString+= "Invalid lat value: " + lat;
			}
			if (lon < MIN_LON || lon > MAX_LON) {
				errorString+= " Invalid lon value: " + lon;
			}
			if (errorString.length() > 0) {
				logger.info(errorString);
				return null;
			}
			jsonMessage.element(LAT, lat);
			jsonMessage.element(LON, lon);
			
			BigDecimal speedMPH = new BigDecimal(TransmissionAndSpeedHelper.getSpeedMph(vsr.getFundamental().getSpeed()));
			speedMPH = speedMPH.setScale(2, BigDecimal.ROUND_HALF_UP);
			jsonMessage.element(SPEED, speedMPH);
			
			// From asn1 spec for DSRC.Heading -- LSB of 0.0125 degrees
			BigDecimal heading = new BigDecimal(vsr.getFundamental().getHeading().intValue() * 0.0125);
			heading = heading.setScale(2, BigDecimal.ROUND_HALF_UP);
			jsonMessage.element(HEADING, heading);
		} else {
			logger.error("Message does not contain a single VehSitRecord element.");
			return null;
		}
		return jsonMessage;
	}
}
