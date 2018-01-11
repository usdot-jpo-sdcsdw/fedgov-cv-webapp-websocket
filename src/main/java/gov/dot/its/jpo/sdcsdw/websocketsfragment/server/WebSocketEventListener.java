/** LEGACY CODE
 * 
 * This was salvaged in part or in whole from the Legacy System. It will be heavily refactored or removed.
 */
package gov.dot.its.jpo.sdcsdw.websocketsfragment.server;

public interface WebSocketEventListener {
	
	public void onOpen(String websocketID);
	public void onClose(String websocketID);
	public void onMessage(String websocketID, String message);
	
}
