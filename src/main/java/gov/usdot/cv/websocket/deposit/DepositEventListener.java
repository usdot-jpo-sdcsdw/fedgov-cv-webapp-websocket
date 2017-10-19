package gov.usdot.cv.websocket.deposit;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import org.apache.log4j.Logger;
import org.eclipse.jetty.util.ssl.SslContextFactory;

import com.oss.asn1.Coder;
import com.oss.asn1.DecodeFailedException;
import com.oss.asn1.DecodeNotSupportedException;

import gov.usdot.asn1.generated.j2735.J2735;
import gov.usdot.asn1.j2735.J2735Util;
import gov.usdot.cv.websocket.WebSocketClient;
import gov.usdot.cv.websocket.WebSocketSSLHelper;
import gov.usdot.cv.websocket.server.WebSocketEventListener;
import gov.usdot.cv.websocket.server.WebSocketServer;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;

public class DepositEventListener implements WebSocketEventListener {

	private static final Logger logger = Logger.getLogger(DepositEventListener.class
			.getName());
	private final static String DEPOSIT_TAG = "DEPOSIT:";
	private final static String SYSTEM_NAME = "systemDepositName";
	private final static String ENCODE_TYPE = "encodeType";
	private final static String ENCODED_MSG = "encodedMsg";
	private final static String ENCODE_TYPE_HEX = "hex";
	private final static String ENCODE_TYPE_BASE64 = "base64";
	private final static String ENCODE_TYPE_UPER = "uper";
	
	private List<DepositConfig> depositConfigs;
	private Map<String, WebSocketClient> depositClientMap = new HashMap<String, WebSocketClient>();
	private Coder coder;
	
	public DepositEventListener(List<DepositConfig> depositConfigs) {
		this.depositConfigs = depositConfigs;
	}
	
	public void connect() {
		try {
			J2735.initialize();
			coder = J2735.getPERUnalignedCoder();
		} catch (Exception e) {
			logger.error("Failed initialize J2735 environment: ", e);
		}
		for (DepositConfig config: depositConfigs) {
			try {
				SslContextFactory sslContextFactory = null;
				if (config.websocketURL.startsWith("wss")) {
					sslContextFactory = WebSocketSSLHelper.buildClientSslContextFactory(config.keystoreFile, config.keystorePassword);
				}
				WebSocketClient depositClient = new WebSocketClient(config.websocketURL, sslContextFactory);
				
				logger.info("Opening WebSocket connection to: " + config.websocketURL);
				depositClient.connect();
				depositClientMap.put(config.systemName, depositClient);
			} catch (Exception e) {
				logger.error("Failed to connect WebSocket Server, config: " + config, e);
			}
		}
	}
	
	public void close() {
		for (WebSocketClient depositClient: depositClientMap.values()) {
			depositClient.close();
		}
		depositClientMap.clear();
		J2735.deinitialize();
	}
	
	public void onMessage(String websocketID, String message) {
		if (message.startsWith(DEPOSIT_TAG)) {
			logger.debug("Received deposit message " + message + " from websocket " + websocketID);
			message = message.substring(DEPOSIT_TAG.length()).trim();
			JSONObject json = (JSONObject)JSONSerializer.toJSON(message);
			try {
				validateMessage(json);
				String systemName = json.getString(SYSTEM_NAME);
				WebSocketClient wsClient = depositClientMap.get(systemName);
				if (wsClient != null) {
					wsClient.send(message);
					// TODO need to wait from response from server?
					WebSocketServer.sendMessage(websocketID, "DEPOSITED:1");
				} else {
					// validateMessage should always catch this, but just in case
					logger.error("No WebSocketClient for systemDepositName: " + systemName);
				}
			} catch (DepositException de) {
				logger.error("Invalid deposit message ", de);
				WebSocketServer.sendMessage(websocketID, "ERROR: " + de.getMessage());
			} catch (Exception e) {
				logger.error("Unexpected error depositing message ", e);
				WebSocketServer.sendMessage(websocketID, "ERROR: " + e.getMessage());
			}
		}
	}

	public void onOpen(String websocketID) {
		// do nothing
	}

	public void onClose(String websocketID) {
		// do nothing
	}
	
	public void validateMessage(JSONObject json) throws DepositException {
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
					J2735Util.decode(coder, bytes);
				} catch (DecodeFailedException e) {
					errorMsg.append("Failed to decode message: " + e.toString());
				} catch (DecodeNotSupportedException e) {
					errorMsg.append("Failed to decode message: " + e.toString());
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
		
		if (errorMsg.length() > 0) {
			throw new DepositException(errorMsg.toString());
		}
	}
}
