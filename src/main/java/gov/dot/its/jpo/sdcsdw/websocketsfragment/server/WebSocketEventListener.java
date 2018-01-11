package gov.dot.its.jpo.sdcsdw.websocketsfragment.server;

public interface WebSocketEventListener {
	
	public void onOpen(String websocketID);
	public void onClose(String websocketID);
	public void onMessage(String websocketID, String message);
	
}
