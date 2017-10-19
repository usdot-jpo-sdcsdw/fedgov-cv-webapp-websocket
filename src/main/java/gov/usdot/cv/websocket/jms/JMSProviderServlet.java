package gov.usdot.cv.websocket.jms;

import gov.usdot.cv.websocket.jms.filter.FilterEventListener;
import gov.usdot.cv.websocket.server.WebSocketServer;
import gov.usdot.cv.websocket.server.utils.ConfigUtils;

import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import org.apache.log4j.Logger;

public class JMSProviderServlet extends HttpServlet {
	
	private static final Logger logger = Logger.getLogger(JMSProviderServlet.class
			.getName());
	private static final long serialVersionUID = 552873629811306900L;
	
	private FilterEventListener filterListener;

	@Override
	public void init() throws ServletException {
		super.init();
		try {
			String jmsConfigFile = getInitParameter("jmsConfigFile");
			List<JMSConfig> configList = ConfigUtils.loadConfigBeanList(jmsConfigFile, JMSConfig.class);
			logger.info("Using " + configList);

			filterListener = new FilterEventListener(configList);
			filterListener.connect();
			WebSocketServer.registerEventListener(filterListener);

		} catch (Exception e) {
			logger.error("MongoProviderServlet initialization failed", e);
		}
	}
	
	@Override
	public void destroy() {
		super.destroy();
		if (filterListener != null) {
			filterListener.close();
		}
	}
	
}
