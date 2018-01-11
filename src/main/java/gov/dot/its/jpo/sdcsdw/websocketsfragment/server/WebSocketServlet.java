/** LEGACY CODE
 * 
 * This was salvaged in part or in whole from the Legacy System. It will be heavily refactored or removed.
 */
package gov.dot.its.jpo.sdcsdw.websocketsfragment.server;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeRequest;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeResponse;
import org.eclipse.jetty.websocket.servlet.WebSocketCreator;

import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;

import gov.dot.its.jpo.sdcsdw.websocketsfragment.server.utils.TestEventListener;

public class WebSocketServlet extends org.eclipse.jetty.websocket.servlet.WebSocketServlet implements WebSocketCreator {

	private static final long serialVersionUID = 6777605077044031611L;
	
	private static final Logger logger = Logger.getLogger(WebSocketServlet.class
			.getName());

	@Override
	public void configure(WebSocketServletFactory factory) {
		factory.setCreator(this);
		
		logger.info("THIS TEXT SHOULD BE SEEN");
		
		TestEventListener listener = new TestEventListener();
		WebSocketServer.registerEventListener(listener);
	}

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		getServletContext().getNamedDispatcher("default").forward(request, response);
	}
	
	public Object createWebSocket(ServletUpgradeRequest req, ServletUpgradeResponse resp) {
		return WebSocketServer.buildWebSocket();
	}
}
