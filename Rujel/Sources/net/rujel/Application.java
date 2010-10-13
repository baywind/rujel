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

import net.rujel.auth.ReadAccess;
import net.rujel.base.MyUtility;
import net.rujel.base.SettingsBase;
import net.rujel.reusables.*;

import com.apress.practicalwo.practicalutilities.WORequestAdditions;
import com.webobjects.eoaccess.EODatabaseContext;
import com.webobjects.eocontrol.EOObjectStoreCoordinator;
import com.webobjects.foundation.*;
import com.webobjects.appserver.*;

import java.util.Calendar;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Logger;

public class Application extends UTF8Application {
	protected StringStorage _strings;
	protected static Logger logger = Logger.getLogger("rujel");
	protected Timer timer;
	public Integer year;
	protected String serverUrl;
	
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
		ModulesInitialiser.readModules(SettingsReader.rootSettings(), "modules");
		propertiesPath = SettingsReader.stringForKeyPath("ui.localisationFolder", null);
		if(propertiesPath != null) {
			_strings = new StringStorage(propertiesPath,null);
		} else {
			propertiesPath = SettingsReader.stringForKeyPath("ui.defaultLocalisation", null);
			if(propertiesPath != null)
				propertiesPath = SettingsReader.stringForKeyPath(
						"ui.localisations." + propertiesPath, null);
			if(propertiesPath == null)
				_strings = StringStorage.appStringStorage;
			else
				_strings = new StringStorage(propertiesPath,null);
		}
		EODatabaseContext.setDefaultDelegate(new CompoundPKeyGenerator());
		if(SettingsReader.boolForKeyPath("dbConnection.yearTag", false)) {
			NSTimestamp today = null;
			String defaultDate = SettingsReader.stringForKeyPath("ui.defaultDate", null);
			if(defaultDate == null) {
				today = new NSTimestamp();
			} else {
				try {
					today = (NSTimestamp)MyUtility.dateFormat().parseObject(defaultDate);
				} catch (Exception e) {
					logger.log(WOLogLevel.WARNING, "Failed parsing default date " + 
							defaultDate + ". Using today.",e);
					today = new NSTimestamp();
				}
			}
			year = MyUtility.eduYearForDate(today);
			if(!DataBaseConnector.makeConnections(
					EOObjectStoreCoordinator.defaultCoordinator(), year.toString())) {
				year = new Integer(year.intValue() -1);
				logger.log(WOLogLevel.INFO,"Trying to connect to previous year database:" + year);
				if(!DataBaseConnector.makeConnections(
						EOObjectStoreCoordinator.defaultCoordinator(), year.toString())) {
					logger.log(WOLogLevel.SEVERE,"Could not connect to database!");
					System.err.println("Could not connect to database!");
					terminate();
					return;
				}
			}
		} else {
			if(!DataBaseConnector.makeConnections()) {
				logger.log(WOLogLevel.SEVERE,"Could not connect to database!");
				System.err.println("Could not connect to database!");
				terminate();
				return;
			}
		}
		NSDictionary access = (NSDictionary)PlistReader.readPlist("access.plist", null, null);
		ReadAccess.mergeDefaultAccess(access);
		
		SettingsBase.init();
		net.rujel.interfaces.EOInitialiser.initAll();
//		SettingsReader node = SettingsReader.settingsForPath("modules",true);
		ModulesInitialiser.initModules("init");
		
		int cacheSize = SettingsReader.intForKeyPath("ui.keyValueCacheSize", 0);
		if(cacheSize > 0) {
			keyValueCache = new KeyValueCache(cacheSize);
		}
		
		if (!Boolean.getBoolean("DisableScheduledTasks")) {
			timer = new Timer(true);
			TimerTask task = new TimerTask() {
				public void run() {
					ModulesInitialiser.useModules(null, "scheduleTask");
				}
			};
			Calendar cal = Calendar.getInstance();
			cal.set(Calendar.HOUR_OF_DAY, 0);
			cal.set(Calendar.MINUTE, 0);
			cal.set(Calendar.SECOND, 1);
			if (cal.getTimeInMillis() < System.currentTimeMillis())
				cal.add(Calendar.DATE, 1);
			timer.scheduleAtFixedRate(task, cal.getTime(), NSLocking.OneDay);
			ModulesInitialiser.useModules(null, "scheduleTask");
		}
		serverUrl = SettingsReader.stringForKeyPath("ui.serverURL", null);
		if(serverUrl == null) {
			SettingsReader reader = SettingsReader.settingsForPath(
					"auth.customURL", false);
			if(reader != null) {
				serverUrl = (String)reader.valueForKeyPath("insecure.default");
				if(serverUrl == null)
					serverUrl = (String)reader.valueForKeyPath("secure.default");
			}
		}
		logger.log(WOLogLevel.INFO,"Rujel started. Version:"
				+ System.getProperty("RujelVersion"), webserverConnectURL());
	}
	
	public Timer timer() {
		return timer;
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
	
	public void refuseNewSessions(boolean aVal) {
		if(aVal) {
			logger.info("Application is refusing new sessions");
		} else {
			logger.info("Application is accepting sessions again");
		}
		super.refuseNewSessions(aVal);
	}
	public void terminate() {
		if(timer != null)
			timer.cancel();
		logger.info("Application is terminating");
		super.terminate();
	}

	public String serverUrl() {
		if(serverUrl.charAt(0) == '?')
			return serverUrl.substring(1);
		return serverUrl;
	}
	
	public WORequest createRequest(String aMethod, String aURL, String anHTTPVersion, 
			Map someHeaders, NSData aContent, Map someInfo) {
		WORequest result = super.createRequest(
				aMethod, aURL, anHTTPVersion, someHeaders, aContent, someInfo);
		if(serverUrl == null || serverUrl.charAt(0) == '?') {
			String url = WORequestAdditions.hostName(result);
            if(url != null) {
            	if(!url.startsWith("http"))
            		url = "http://" + url;
            	if(url.contains("//localhost") || url.contains("//127.0.0.1") ||
            			url.contains("//192.168.") || url.contains("//10.")) {
            		serverUrl = '?' + url;
            	} else {
            		serverUrl = url;
            	}
            }
		}
		return result;
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
	
	public void setDefaultAccess(NSDictionary toMerge) {
		ReadAccess.mergeDefaultAccess(toMerge);
	}
}
