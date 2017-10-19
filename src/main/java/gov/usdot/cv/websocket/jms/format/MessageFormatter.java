package gov.usdot.cv.websocket.jms.format;

import gov.usdot.asn1.generated.j2735.semi.IntersectionSituationData;
import gov.usdot.asn1.generated.j2735.semi.SemiDialogID;
import gov.usdot.asn1.generated.j2735.semi.VehSitDataMessage;
import gov.usdot.cv.websocket.jms.format.IntersectionFormatter;
import gov.usdot.cv.websocket.jms.format.VehicleFormatter;
import gov.usdot.cv.websocket.jms.router.RoutableMessage;
import net.sf.json.JSONObject;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;

/**
 * Parsers VehSitDataMessages and extracts key values for mapping UI. 
 */
public class MessageFormatter {

	public static String formatMessage(RoutableMessage message, String resultEncoding) {
		byte[] bytes = message.getRawMessage();
		if (resultEncoding.equalsIgnoreCase("hex")) {
			return Hex.encodeHexString(bytes);
		} else if (resultEncoding.equalsIgnoreCase("base64")) {
			return Base64.encodeBase64String(bytes);
		} else if (resultEncoding.equalsIgnoreCase("full")) {
			if (message.getDialogId() == SemiDialogID.vehSitData.longValue()) {
				JSONObject json = VehicleFormatter.parseMessage((VehSitDataMessage)message.getMessage());
				return json != null ? json.toString(): null;
			} else if (message.getDialogId() == SemiDialogID.intersectionSitDataDep.longValue()) {
				JSONObject json = IntersectionFormatter.formatMessage((IntersectionSituationData)message.getMessage());
				return json != null ? json.toString(): null;
			} else {
				return null;
			}
		} else {
			return null;
		}
	}
	
}
