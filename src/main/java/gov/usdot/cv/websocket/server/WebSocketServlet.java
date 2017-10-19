package gov.usdot.cv.websocket.server;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.websocket.servlet.ServletUpgradeRequest;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeResponse;
import org.eclipse.jetty.websocket.servlet.WebSocketCreator;

import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;

import gov.usdot.cv.websocket.server.utils.TestEventListener;

public class WebSocketServlet extends org.eclipse.jetty.websocket.servlet.WebSocketServlet implements WebSocketCreator {

	private static final long serialVersionUID = 6777605077044031611L;

	@Override
	public void configure(WebSocketServletFactory factory) {
		factory.setCreator(this);
		
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
