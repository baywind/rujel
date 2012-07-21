package net.rujel.rest;

import com.webobjects.appserver.WOApplication;
import com.webobjects.appserver.WOContext;
import com.webobjects.appserver.WORequestHandler;

public class RestModule {
	public static Object init(Object obj, WOContext ctx) {
		if(obj == null || obj.equals("init")) {
			init();
		}
		return null;
	}
	
	public static void init() {
		WOApplication app = WOApplication.application();
		WORequestHandler rh = app.requestHandlerForKey(AgregationHandler.handlerKey);
		if(rh == null)
			app.registerRequestHandler(new AgregationHandler(), AgregationHandler.handlerKey);
	}

}
