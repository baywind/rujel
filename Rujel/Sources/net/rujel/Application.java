// Application.java

/*
 * Copyright (c) 2008, Gennady & Michael Kushnir
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 * 
 * 	•	Redistributions of source code must retain the above copyright notice, this
 * 		list of conditions and the following disclaimer.
 * 	•	Redistributions in binary form must reproduce the above copyright notice,
 * 		this list of conditions and the following disclaimer in the documentation
 * 		and/or other materials provided with the distribution.
 * 	•	Neither the name of the RUJEL nor the names of its contributors may be used
 * 		to endorse or promote products derived from this software without specific 
 * 		prior written permission.
 * 		
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT
 * SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
 * TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
 * BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY
 * WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package net.rujel;

import net.rujel.reusables.*;

import com.webobjects.eoaccess.EODatabaseContext;
import com.webobjects.foundation.*;
import com.webobjects.appserver.*;

import java.util.logging.Logger;

public class Application extends UTF8Application {
	private StringStorage _strings = new StringStorage(application().resourceManager());
	protected static Logger logger = Logger.getLogger("rujel");
	
	public SettingsReader prefs() {
		return SettingsReader.rootSettings();
	}
	
	public Application() {
        super();
        System.out.println("Welcome to " + this.name() + "!");
		WORequestHandler directActionRequestHandler = requestHandlerForKey("wa");
		setDefaultRequestHandler(directActionRequestHandler);

		String propertiesPath = SettingsReader.stringForKeyPath("loggingProperties", null);
//		InputStream propsIn = (propertiesPath!=null)?null:
//			resourceManager().inputStreamForResourceNamed("logging.properties", "app", null);
		if(propertiesPath != null) {
			LogInitialiser.initLogging(null, propertiesPath, logger);
		}
		
		EODatabaseContext.setDefaultDelegate(new CompoundPKeyGenerator());
		DataBaseConnector.makeConnections();

		net.rujel.interfaces.EOInitialiser.initAll();
		SettingsReader node = SettingsReader.settingsForPath("modules",true);
		ModulesInitialiser.initModules(node,"init");
		net.rujel.reusables.Scheduler.sharedInstance();
		ModulesInitialiser.initModules(node,"schedulePeriod");
		ModulesInitialiser.initModules(node,"scheduleTask");
		
//		byte[] bytes = resourceManager().bytesForResourceNamed("Strings.plist",null,null);
//		NSDictionary strings = (NSDictionary)NSPropertyListSerialization.propertyListFromData(new NSData(bytes),"UTF8");
//		_strings.setObjectForKey(strings,"Strings");
		
		int cacheSize = SettingsReader.intForKeyPath("ui.keyValueCacheSize", 0);
		if(cacheSize > 0) {
			keyValueCache = new KeyValueCache(cacheSize);
		}
		
		/*
		String sesTimeout = System.getProperty("WOSessionTimeOut");
		setSessionTimeOut(Integer.valueOf(sesTimeout));*/
		logger.logp(WOLogLevel.INFO,"Application","<init>","Rujel started " + webserverConnectURL());
	}
	
	protected KeyValueCache keyValueCache;
	public Object valueForKeyPath(String keyPath) {
		if(keyValueCache == null || (keyPath.indexOf('.') < 0))
			return super.valueForKeyPath(keyPath);
		Object result = keyValueCache.valueForKey(keyPath);
		if(result == null) {
			result = super.valueForKeyPath(keyPath);
			keyValueCache.takeValueForKey(result, keyPath);
		} else if(result == NullValue) {
			return null;
		}
		return result;
	}

    public static void main(String argv[]) {
        WOApplication.main(argv, Application.class);
		logger.logp(WOLogLevel.INFO,"Application","main","Rujel ended");
    }
	/*
	public WOSession createSessionForRequest(WORequest aRequest) {
		WOSession ses = super.createSessionForRequest(aRequest);
		logger.log(WOLogLevel.SESSION,"Session created",new Object[] {ses,ses.clientIdentity(aRequest)});
		return ses;
	} */
    
    public WOResponse handleSessionRestorationErrorInContext(WOContext aContext) {
    	WOComponent page = pageWithName("MessagePage", aContext);
    	logger.log(WOLogLevel.FINE, "SessionRestorationErrorInContext",
    			(aContext.hasSession())?aContext.session():aContext.request().sessionID());
    	page.takeValueForKey("sessionTitle", "plistTitle");
    	page.takeValueForKey("SessionRestorationError", "plistMessage");
    	page.takeValueForKey("login", "redirectAction");
    	page.takeValueForKey(new Integer(12), "timeout");
    	return page.generateResponse();
    }
    
    public WOResponse handleSessionCreationErrorInContext(WOContext aContext) {
    	WOComponent page = pageWithName("MessagePage", aContext);
    	Object[] args = new Object[3];
    	args[0] = (aContext.hasSession())?aContext.session():aContext.request().sessionID();
    	args[1] = new Exception("SessionCreationErrorInContext");
    	logger.log(WOLogLevel.WARNING, "SessionCreationErrorInContext : "
    			+ aContext.request().uri(),args);			
    	page.takeValueForKey("sessionTitle", "plistTitle");
    	page.takeValueForKey("SessionCreationError", "plistMessage");
    	return page.generateResponse(); 	
    }
    
    public WOResponse handlePageRestorationErrorInContext(WOContext aContext) {
    	WOComponent page = pageWithName("MessagePage", aContext);
    	logger.log(WOLogLevel.INFO, "Page restoration error",
    			(aContext.hasSession())?aContext.session():null);
    	page.takeValueForKey("Error", "plistTitle");
    	page.takeValueForKey("PageRestorationError", "plistMessage");
    	page.takeValueForKey("report", "redirectAction");
    	return page.generateResponse();
    }
    
	public WOResponse handleException(Exception anException,  WOContext aContext) {
		if(aContext == null) {
			logger.log(WOLogLevel.UNCOUGHT_EXCEPTION,"Exception occured with no WOContext",anException);
			return super.handleException(anException,aContext);
		}
		Object[] param = new Object[] {anException};
		if(aContext.hasSession()) {
			param = new Object[] {anException,aContext.session()};
		} else if (aContext.request().isSessionIDInRequest()) {
			param = new Object[] {anException,aContext.request().sessionID()};
		}
		StringBuffer msg = new StringBuffer("Exception occured executing ");
		msg.append(aContext.request().uri());
		WOComponent page = aContext.page();
		if(page != null) {
			msg.append(" when in component ").append(page.name());
		}
		WOComponent component = aContext.component();
		if(component != null && component != page) {
			msg.append('(').append(component.name()).append(')');
		}
		String elem = aContext.elementID();
		if(elem != null && elem.length() > 0) {
			msg.append(", elementID: ").append(elem);
		}
		logger.log(WOLogLevel.UNCOUGHT_EXCEPTION,msg.toString(),param);

		WOComponent errorPage = pageWithName("ErrorPage", aContext);
		errorPage.takeValueForKey(anException, "throwable");
		return errorPage.generateResponse();
		//return super.handleException(anException,aContext);
	}
	
	public NSKeyValueCoding strings() {
		return _strings;
	}
	
/*	public NSDictionary strings() {
		return (NSDictionary)_strings.objectForKey("Strings");
	}
	
	public NSKeyValueCoding extStrings() {
		return new NSKeyValueCoding() {
			public Object valueForKey(String key) {
				NSDictionary dict = (NSDictionary)_strings.valueForKey(key);
				if(dict == null) {
					String frw = null;
					String res = key + ".plist";
					int idx = key.indexOf('-');
					if(idx < 0) idx = key.indexOf('_');
					if(idx > 0) {
						frw = key.substring(0,idx);
						res = res.substring(idx + 1);
					}
					byte[] bytes = resourceManager().bytesForResourceNamed(res,frw,null);
					if(bytes != null && bytes.length > 0)
						dict = (NSDictionary)NSPropertyListSerialization.propertyListFromData(new NSData(bytes),"UTF8");
					if(dict == null) {
						dict = NSDictionary.EmptyDictionary;
						logger.log(WOLogLevel.WARNING,"Could not load dictionary for resource named " + key);
					}
					_strings.setObjectForKey(dict,key);
				}
				return dict;
			}
			
			public void takeValueForKey(Object value, String key) {
				;
			}
		};
	}*/
	
	public void refuseNewSessions(boolean aVal) {
		super.refuseNewSessions(aVal);
		if(aVal) {
			logger.info("Application is refusing new sessions");
		} else {
			logger.info("Application is accepting sessions again");
		}
	}
	public void terminate() {
		logger.info("Application is terminating");
		super.terminate();
	}
	
	public WOSession createSessionForRequest(WORequest aRequest) {
		WOSession result = super.createSessionForRequest(aRequest);
		if(!(aRequest.method().equals("POST") && aRequest.uri().contains("login"))
				&& !aRequest.uri().contains("dummy")) {
			Exception ex = new Exception("Dangling session creation");
			Object[] args = new Object[] {result, Session.clientIdentity(aRequest),ex};
			logger.log(WOLogLevel.SESSION,
					"Generating session: " + aRequest.method() + ':' + aRequest.uri(), args);
		}
		return result;
	}
}
