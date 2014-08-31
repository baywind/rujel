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

import net.rujel.base.MyUtility;
import net.rujel.base.ReadAccess;
import net.rujel.reusables.*;

import com.apress.practicalwo.practicalutilities.WORequestAdditions;
import com.webobjects.eoaccess.EODatabaseContext;
import com.webobjects.eoaccess.EOModel;
import com.webobjects.eocontrol.EOEditingContext;
import com.webobjects.eocontrol.EOObjectStore;
import com.webobjects.eocontrol.EOObjectStoreCoordinator;
import com.webobjects.foundation.*;
import com.webobjects.appserver.*;

import java.lang.reflect.Constructor;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.Statement;
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
	protected String diaryUrl;
	protected String urlPrefix;
	protected String _errorMessage;
	
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
		BufferHandler handler = new BufferHandler(
				"Sorry!\nRUJEL failed to start.\nPlease review log for details.\r-----\r\r");
		handler.setLevel(WOLogLevel.INFO);
		Logger.getLogger("").addHandler(handler);
		
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
		NSArray usedModels = ModulesInitialiser.useModules(null, "usedModels");
		
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
			NSArray problems = DataBaseConnector.makeConnections(year.toString(), usedModels,false);
			if(problems != null) {
				logger.info("Trying to resolve database problems");
				if(dealWithDbProblems(problems, usedModels)) {
					logger.log(WOLogLevel.SEVERE,"Could not connect to database!");
					System.err.println("Could not connect to database!");
					_errorMessage = handler.toString();
					Logger.getLogger("").removeHandler(handler);
					handler.close();
					return;
				}
			}
			
		NSDictionary access = (NSDictionary)PlistReader.readPlist("access.plist", null, null);
		ReadAccess.mergeDefaultAccess(access);
		
//		SettingsBase.init();
		net.rujel.interfaces.EOInitialiser.initAll();
//		SettingsReader node = SettingsReader.settingsForPath("modules",true);
		ModulesInitialiser.initModules("init");
		
		int cacheSize = SettingsReader.intForKeyPath("ui.keyValueCacheSize", 0);
		if(cacheSize > 0) {
			keyValueCache = new KeyValueCache(cacheSize);
		}
		setPageCacheSize(SettingsReader.intForKeyPath("ui.pageCacheSize", 5));
		setPermanentPageCacheSize(SettingsReader.intForKeyPath("ui.permanentPageCacheSize", 2));
		
		if (defaultDate == null && !Boolean.getBoolean("DisableScheduledTasks")) {
			timer = new Timer(true);
			TimerTask task = new TimerTask() {
				public void run() {
					Application app = (Application)WOApplication.application();
					Integer y = app.year;
					try {
						app.testNextYear();
					} catch (Exception e) {
						app.year = y;
					}
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
		if(serverUrl == null || serverUrl.length() < 5) {
			SettingsReader reader = SettingsReader.settingsForPath(
					"auth.customURL", false);
			if(reader != null) {
				serverUrl = (String)reader.valueForKeyPath("insecure.default");
				if(serverUrl == null)
					serverUrl = (String)reader.valueForKeyPath("secure.default");
			}
			if(serverUrl == null)
				serverUrl = "?" + super.host();
		}
		int slash0 = 0;
		if(serverUrl.startsWith("http")) {
			slash0 = serverUrl.lastIndexOf('/', 10) +1;
		}
		int slash = serverUrl.indexOf('/',slash0);
		if(slash > 0)
			serverUrl = serverUrl.substring(slash0,slash);
		diaryUrl = SettingsReader.stringForKeyPath("ui.diaryURL", null);
		urlPrefix = SettingsReader.stringForKeyPath("ui.urlPrefix", "?/Apps/WebObjects/Rujel.woa");
		Logger.getLogger("").removeHandler(handler);
		handler.close();
		logger.log(WOLogLevel.INFO,"Rujel started. Version:" + System.getProperty("RujelVersion")
				 + ' ' + System.getProperty("RujelRevision"), webserverConnectURL());
	}
	
	private boolean dealWithDbProblems(NSArray problems, NSArray usedModels) {
		if(problems == null)
			return false;
		if(problems == NSArray.EmptyArray)
			return true;
		if(SettingsReader.boolForKeyPath("dbConnection.disableSchemaUpdate", false))
			return true;
		boolean shouldCreateSchema = false;
		Boolean isYear = null;
		for (int i = 0; i < problems.count(); i++) {
			Object pr = problems.objectAtIndex(i);
			if(pr instanceof EOModel) {
				NSDictionary userInfo = ((EOModel)pr).userInfo();
				Number versionFound = (Number)userInfo.valueForKey("versionFound");
				if(versionFound == null)
					return true;
				if(versionFound.intValue() > 0) {
					return true;
				} else {
					shouldCreateSchema = true;
				}
				continue;
			}
			Exception err = (Exception)((NSMutableDictionary)pr).removeObjectForKey("error");
			if(err instanceof NumberFormatException)
				return true;
			String url = (String)((NSDictionary)pr).valueForKey("URL");
			String[] splitUrl = DataBaseUtility.extractDBfromURL(url);
			try {
				NSDictionary plist = (NSDictionary)PlistReader.readPlist(
						DataBaseUtility.dbEngine(url) + ".plist", null, null);
				String user = (String)((NSDictionary)pr).valueForKey("username");
				String password = (String)((NSDictionary)pr).valueForKey("password");
				String driverName = (String)((NSDictionary)pr).valueForKey("driver");
				if(driverName != null) {
					Class driverClass = Class.forName(driverName);
					Constructor driverConstructor = driverClass.getConstructor((Class[])null);
					Driver driver = (Driver)driverConstructor.newInstance((Object[])null);
					DriverManager.registerDriver(driver);
				}
				if(err instanceof java.sql.SQLException) {
					logger.log(WOLogLevel.INFO,"Creating required database " + splitUrl[1]);
					Connection conn = DriverManager.getConnection(splitUrl[0],user,password);
					Statement stmnt = conn.createStatement();
					NSArray lines = (NSArray)plist.valueForKey("createDatabase");
					for (int j = 0; j < lines.count(); j++) {
						String line = (String)lines.objectAtIndex(j);
						line = String.format(line, splitUrl[1]);
						stmnt.executeUpdate(line);
					}
					stmnt.close();
					conn.close();
				}
				logger.log(WOLogLevel.INFO,"Database '" + splitUrl[1] + 
						"' created. Creating system tables.");
				Connection conn = DriverManager.getConnection(url,user,password);
				Statement stmnt = conn.createStatement();
				NSArray lines = (NSArray)plist.valueForKey("systemTables");
				for (int j = 0; j < lines.count(); j++) {
					stmnt.executeUpdate((String)lines.objectAtIndex(j));
				}
				stmnt.close();
				conn.close();
				shouldCreateSchema = true;
				if(isYear == null || isYear.booleanValue()) {
					isYear = Boolean.valueOf(splitUrl[1].contains(year.toString()));
				}
			} catch (Exception e) {
				logger.log(WOLogLevel.SEVERE, "Failed to create database " +
						((NSDictionary)pr).valueForKey("URL"),e);
				return true;
			}
		}
		if(shouldCreateSchema) {
			problems = DataBaseConnector.makeConnections(year.toString(), usedModels,true);
			WORequest request = createRequest("GET","dummy","HTTP/1.0",null,null,null);
			WOContext ctx = new WOContext(request) {
				public boolean shouldNotStorePageInBacktrackCache() {
					return true;
				}
			};
			ctx.setUserInfoForKey(year, "eduYear");
			EOEditingContext ec = new EOEditingContext();
			ec.setUserInfoForKey(year, "eduYear");
			ctx.setUserInfoForKey(ec, "ec");
			EOEditingContext prevEc = null;
			if(problems == null && isYear != null && isYear.booleanValue()) {
				Integer prevYear = new Integer(year -1);
				EOObjectStore os = DataBaseConnector.objectStoreForTag(prevYear.toString());
				if(os != null) {
					ctx.setUserInfoForKey(prevYear, "prevYear");
					prevEc = new EOEditingContext(os);
					prevEc.setUserInfoForKey(prevYear, "eduYear");
					ctx.setUserInfoForKey(prevEc, "prevEc");
				}
			}
			try {
				ec.lock();
				if(prevEc != null)
					prevEc.lock();
				ModulesInitialiser.useModules(ctx, "initialData");
			} catch (Exception e) {
				logger.log(WOLogLevel.WARNING,"Failed to add initial data.",e);
			} finally {
				ec.unlock();
				if(prevEc != null)
					prevEc.unlock();
			}
		} // shouldCreateSchema
		return (problems != null && problems.count() > 0);
	}
	
	public void testNextYear() {
		Integer currYear = MyUtility.eduYearForDate(null);
		if(currYear.equals(year))
			return;
		NSArray usedModels = ModulesInitialiser.useModules(null, "usedModels");
		EOObjectStoreCoordinator old = EOObjectStoreCoordinator.defaultCoordinator();
		EOObjectStoreCoordinator os = new EOObjectStoreCoordinator();
		EOObjectStoreCoordinator.setDefaultCoordinator(os);
		old.dispose();
		year = currYear;
		NSArray problems = DataBaseConnector.makeConnections(year.toString(), usedModels,false);
		if(problems != null) {
			logger.info("Trying to resolve database problems");
			if(dealWithDbProblems(problems, usedModels)) {
				logger.log(WOLogLevel.SEVERE,"Could not connect to next eduYear database!");
				return;
			}
		}
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
    	if(_errorMessage != null) {
    		return errorResponse(aContext, _errorMessage);
    	}
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
    	if(_errorMessage != null) {
    		return errorResponse(aContext, _errorMessage);
    	}
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
    	page.takeValueForKey("resume", "redirectAction");
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
		errorPage.takeValueForKey(msg.toString(), "message");
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

	public String host() {
		if(serverUrl == null)
			return super.host();
		if(serverUrl.charAt(0) == '?')
			return serverUrl.substring(1);
		return serverUrl;
	}
	
	public String serverUrl() {
		StringBuilder buf = new StringBuilder("http");
		if(SettingsReader.boolForKeyPath("auth.sessionSecure", false))
			buf.append('s');
		buf.append("://").append(host());
		return buf.toString();
	}
	
	public String urlPrefix() {
		if(urlPrefix.charAt(0) == '?')
			return urlPrefix.substring(1);
		return urlPrefix;
	}
	
	public String diaryUrl() {
		if(diaryUrl == null) {
			StringBuilder buf = new StringBuilder("http://");
			buf.append(host());
			String prefix = urlPrefix();
			int slash = serverUrl.lastIndexOf('/', 4);
			if(slash > 0)
				prefix = prefix.substring(0,slash);
			else
				prefix = "/Apps/WebObjects/";
			buf.append(prefix).append("RujelDiary.woa");
		}
		return diaryUrl;
	}
	
	public static WOResponse errorResponse(WOContext context, String errorMessage) {
//		String errorMessage = ((Application)application())._errorMessage;
		if(errorMessage == null)
			return null;
		WOResponse response = context.response();
		if(response == null)
			response = application().createResponseInContext(context);
		response.setContent(errorMessage);
		response.setHeader("text/plain; charset=UTF-8","Content-Type");
		return response;
	}
	
	public void appendToResponse(WOResponse aResponse,WOContext aContext) {
		if(_errorMessage == null) {
			super.appendToResponse(aResponse,aContext);
		} else {
			aResponse.setContent(_errorMessage);
			aResponse.setHeader("text/plain; charset=UTF-8","Content-Type");
		}
	}
	
	public WORequest createRequest(String aMethod, String aURL, String anHTTPVersion, 
			Map someHeaders, NSData aContent, Map someInfo) {
		WORequest result = super.createRequest(
				aMethod, aURL, anHTTPVersion, someHeaders, aContent, someInfo);
		if(_errorMessage != null)
			return result;
		if(serverUrl == null || serverUrl.charAt(0) == '?') {
			String url = WORequestAdditions.hostName(result);
	        if(url != null && url.length() > 0 && 
	        		(serverUrl == null || !url.contains(serverUrl.substring(1))))
	        	tryUrl(url);
		}
		return result;
	}
	
	protected void tryUrl(String url) {
		int slash0 = 0;
		if(url.startsWith("http")) {
			slash0 = url.lastIndexOf('/', 10) +1;
		}
		int slash = url.indexOf('/',slash0);
		if(slash > 0)
			url = url.substring(slash0,slash);
		if(url.startsWith("localhost") || url.startsWith("127.0.0.1") ||
				url.startsWith("192.168.") || url.startsWith("10.") ||
				url.endsWith(".local") || url.indexOf('.') < 1) {
			serverUrl = '?' + url;
		} else {
			serverUrl = url;
		}
		logger.log(WOLogLevel.INFO,"Server url assumed: " + serverUrl);
	}
	
	public void _setRequest(WORequest req) {
		if (urlPrefix == null || urlPrefix.charAt(0) == '?') {
			urlPrefix = req.applicationURLPrefix();
			logger.log(WOLogLevel.INFO,"Url prefix assumed: " + urlPrefix, 
					(req.context().hasSession())?req.context().session():null);
		}
	}

	public WOSession createSessionForRequest(WORequest aRequest) {
		WOSession result = super.createSessionForRequest(aRequest);
		if(!Various.boolForObject(aRequest.userInfoForKey("isLogin"))
				&& !aRequest.uri().contains("dummy")) {
			Exception ex = new Exception("Dangling session creation");
			Object[] args = new Object[] {result, Various.clientIdentity(aRequest),ex};
			logger.log(WOLogLevel.SESSION,
					"Generating session: " + aRequest.method() + ':' + aRequest.uri(), args);
		}
		ModulesInitialiser.useModules(aRequest.context(), result);
		return result;
	}
	
	public void setDefaultAccess(NSDictionary toMerge) {
		ReadAccess.mergeDefaultAccess(toMerge);
	}
}
