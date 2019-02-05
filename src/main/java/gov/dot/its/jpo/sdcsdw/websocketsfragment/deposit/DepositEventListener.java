/** LEGACY CODE
 * 
 * This was salvaged in part or in whole from the Legacy System. It will be heavily refactored or removed.
 */
package gov.dot.its.jpo.sdcsdw.websocketsfragment.deposit;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import gov.dot.its.jpo.sdcsdw.asn1.perxercodec.Asn1Types;
import gov.dot.its.jpo.sdcsdw.asn1.perxercodec.PerXerCodec;
import gov.dot.its.jpo.sdcsdw.asn1.perxercodec.exception.CodecException;
import gov.dot.its.jpo.sdcsdw.asn1.perxercodec.per.RawPerData;
import gov.dot.its.jpo.sdcsdw.asn1.perxercodec.xer.DocumentXerData;
import gov.dot.its.jpo.sdcsdw.websocketsfragment.mongo.MongoConfig;
import gov.dot.its.jpo.sdcsdw.websocketsfragment.mongo.MongoDepositor;
import gov.dot.its.jpo.sdcsdw.websocketsfragment.server.WebSocketEventListener;
import gov.dot.its.jpo.sdcsdw.websocketsfragment.server.WebSocketServer;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;

public class DepositEventListener implements WebSocketEventListener {

	private static final Logger logger = LoggerFactory.getLogger(DepositEventListener.class
			.getName());
	private final static String DEPOSIT_TAG = "DEPOSIT:";
	private final static String SYSTEM_NAME = "systemDepositName";
	private final static String ENCODE_TYPE = "encodeType";
	private final static String ENCODED_MSG = "encodedMsg";
	private final static String ENCODE_TYPE_HEX = "hex";
	private final static String ENCODE_TYPE_BASE64 = "base64";
	private final static String ENCODE_TYPE_UPER = "uper";
	
	private List<MongoConfig> depositConfigs;
	private Map<String, MongoDepositor> depositClientMap = new HashMap<String, MongoDepositor>();
	
	public DepositEventListener(List<MongoConfig> depositConfigs) {
		this.depositConfigs = depositConfigs;
	}
	
	public void connect() {
		for (MongoConfig config: depositConfigs) {
		    MongoDepositor depositor = new MongoDepositor(config);
		    depositor.connect();
			depositClientMap.put(config.systemName, depositor);
		}
	}
	
	public void close() {
	    logger.debug("Depositor is closing");
		for (MongoDepositor depositClient: depositClientMap.values()) {
			depositClient.close();
		}
		depositClientMap.clear();
	}
	
	public void onMessage(String websocketID, String message) {
		if (message.startsWith(DEPOSIT_TAG)) {
			logger.debug("Received deposit message " + message + " from websocket " + websocketID);
			message = message.substring(DEPOSIT_TAG.length()).trim();
			JSONObject json = (JSONObject)JSONSerializer.toJSON(message);
			try {
				Document xer = validateMessage(json);
				String systemName = json.getString(SYSTEM_NAME);
				MongoDepositor wsClient = depositClientMap.get(systemName);
				if (wsClient != null) {
				    wsClient.deposit(json, xer);
				    WebSocketServer.sendMessage(websocketID, "DEPOSITED:1");
				} else {
					// validateMessage should always catch this, but just in case
					logger.error("No MongoDepositor for systemDepositName: " + systemName);
				}
			} catch (DepositException de) {
				logger.error("Invalid deposit message ", de);
				WebSocketServer.sendMessage(websocketID, "ERROR: " + de.getMessage());
			} catch (Exception e) {
				logger.error("Unexpected error depositing message ", e);
				WebSocketServer.sendMessage(websocketID, "ERROR: " + e.getMessage());
			}
		} else {
		    logger.debug("Deposit listner ignoring: " + message);
		}
	}

	public void onOpen(String websocketID) {
		// do nothing
	}

	public void onClose(String websocketID) {
		// do nothing
	}
	
	public Document validateMessage(JSONObject json) throws DepositException {
		StringBuilder errorMsg = new StringBuilder();
		if (json.containsKey(SYSTEM_NAME) && json.containsKey(ENCODE_TYPE) && json.containsKey(ENCODED_MSG)) {
			String systemName = json.getString(SYSTEM_NAME);
			String encodeType = json.getString(ENCODE_TYPE);
			String encodedMsg = json.getString(ENCODED_MSG);
			if (!depositClientMap.containsKey(systemName)) {
				errorMsg.append("Invalid systemDepositName: ").append(systemName).
					append(", not one of the supported systemDepositName: ").append(depositClientMap.keySet().toString());
			}
			if (!encodeType.equalsIgnoreCase(ENCODE_TYPE_HEX) && !encodeType.equalsIgnoreCase(ENCODE_TYPE_BASE64) 
					&& !encodeType.equalsIgnoreCase(ENCODE_TYPE_UPER)) {
				errorMsg.append("Invalid encodeType: ").append(encodeType).
					append(", not one of the supported encodeType: ").append(ENCODE_TYPE_HEX).
					append(", ").append(ENCODE_TYPE_BASE64).append(", ").append(ENCODE_TYPE_UPER);
			}
			
			byte[] bytes = null;
			if (encodeType.equalsIgnoreCase(ENCODE_TYPE_HEX) || encodeType.equalsIgnoreCase(ENCODE_TYPE_UPER)) {
				try {
					bytes = Hex.decodeHex(encodedMsg.toCharArray());
				} catch (DecoderException e) {
					errorMsg.append("Hex to bytes decoding failed: " + e.toString());
				}
			} else if (encodeType.equalsIgnoreCase(ENCODE_TYPE_BASE64)) {
				bytes = Base64.decodeBase64(encodedMsg);
			}

			if (bytes != null) {
				try {
					//PerXerCodec.guessPerToXer(Asn1Types.getAllTypes(), bytes, RawPerData.unformatter, RawXerData.formatter);
					return PerXerCodec.perToXer(Asn1Types.AdvisorySituationDataType, bytes, RawPerData.unformatter, DocumentXerData.formatter);
				} catch (CodecException e) {
					errorMsg.append("Failed to decode message: " + e);
				}
			}
			
		} else {
			errorMsg.append("Deposit message missing required field(s): ");
			if (!json.containsKey(SYSTEM_NAME))
				errorMsg.append(SYSTEM_NAME).append(" ");
			if (!json.containsKey(ENCODE_TYPE))
				errorMsg.append(ENCODE_TYPE).append(" ");
			if (!json.containsKey(ENCODED_MSG))
				errorMsg.append(ENCODED_MSG).append(" ");
		}
		
		throw new DepositException(errorMsg.toString());
	}
}
