package gov.dot.its.jpo.sdcsdw.webfragment_websockets.mongo;

import gov.dot.its.jpo.sdcsdw.webfragment_websockets.server.WebSocketServer;
import gov.dot.its.jpo.sdcsdw.webfragment_websockets.server.utils.ConfigUtils;

import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import org.apache.log4j.Logger;

public class MongoProviderServlet extends HttpServlet {
	
	private static final Logger logger = Logger.getLogger(MongoProviderServlet.class
			.getName());
	
	private static final long serialVersionUID = -2437352381953355241L;
	
	private MongoQueryEventListener queryListener;

	@Override
	public void init() throws ServletException {
		super.init();
		try {
			String mongoConfigFile = getInitParameter("mongoConfigFile");
			List<MongoConfig> configList = ConfigUtils.loadConfigBeanList(mongoConfigFile, MongoConfig.class);
			logger.info("Using " + configList);
			
			queryListener = new MongoQueryEventListener(configList);
			queryListener.connect();
			WebSocketServer.registerEventListener(queryListener);
			
		} catch (Exception e) {
			logger.error("MongoProviderServlet initialization failed", e);
		}
	}
	
	@Override
	public void destroy() {
		super.destroy();
		if (queryListener != null) {
			queryListener.close();
		}
	}
}
