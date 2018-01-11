package gov.dot.its.jpo.sdcsdw.websocketsfragment.server.utils;

import java.util.ArrayList;

import gov.dot.its.jpo.sdcsdw.websocketsfragment.server.WebSocketEventListener;
import gov.dot.its.jpo.sdcsdw.websocketsfragment.server.WebSocketServer;

public class TestEventListener implements WebSocketEventListener {

	private final static String TEST_TAG = "TEST";
	private static final ArrayList<String> testData = new ArrayList<String>();
	
	static {
		testData.add("{\"requestId\":\"536952342\",\"tempId\":\"20013e16\",\"lat\":42.4478319,\"long\":-83.4309084,\"speed\":10000.0,\"heading\":360.00}");
		testData.add("{\"requestId\":\"536952342\",\"tempId\":\"20013e16\",\"lat\":42.4488319,\"long\":-83.4309084,\"speed\":10000.0,\"heading\":360.00}");
		testData.add("{\"requestId\":\"536952342\",\"tempId\":\"20013e16\",\"lat\":42.4498319,\"long\":-83.4309084,\"speed\":10000.0,\"heading\":360.00}");
	}
	
	public void onMessage(String websocketID, String message) {
		if (message.startsWith(TEST_TAG)) {
			for (String response: testData) {
				WebSocketServer.sendMessage(websocketID, response);
			}
		}
	}

	public void onOpen(String websocketID) {
		// do nothing
	}

	public void onClose(String websocketID) {
		// do nothing
	}
	
}
