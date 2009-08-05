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

package net.rujel.diary;

import java.util.logging.Logger;

import net.rujel.base.MyUtility;
import net.rujel.reusables.*;

import com.webobjects.appserver.WOApplication;
import com.webobjects.appserver.WOComponent;
import com.webobjects.appserver.WOContext;
import com.webobjects.appserver.WORequestHandler;
import com.webobjects.appserver.WOResponse;
import com.webobjects.eocontrol.EOObjectStore;
import com.webobjects.eocontrol.EOObjectStoreCoordinator;
import com.webobjects.eocontrol.EOSharedEditingContext;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSKeyValueCoding;
import com.webobjects.foundation.NSMutableDictionary;
import com.webobjects.foundation.NSTimestamp;

public class Application extends UTF8Application {
	protected static Logger logger = Logger.getLogger("rujel");
	protected StringStorage _strings;

	public NSMutableDictionary<Number, NSArray> groupsForYear 
			= new NSMutableDictionary<Number, NSArray>();
	public NSMutableDictionary<Number, NSArray> coursesForGroup 
			= new NSMutableDictionary<Number, NSArray>();
	
	public static void main(String[] argv) {
		WOApplication.main(argv, Application.class);
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
		//EODatabaseContext.setDefaultDelegate(new CompoundPKeyGenerator());
		_year = MyUtility.eduYearForDate(today());
		DataBaseConnector.makeConnections(
				EOObjectStoreCoordinator.defaultCoordinator(), _year.toString());
		
		ecForYear.takeValueForKey(EOSharedEditingContext.defaultSharedEditingContext(),
				_year.toString());

		net.rujel.interfaces.EOInitialiser.initAll();
		SettingsReader node = SettingsReader.settingsForPath("modules",true);
		ModulesInitialiser.initModules(node,"init");
				
/*		int cacheSize = SettingsReader.intForKeyPath("ui.keyValueCacheSize", 0);
		if(cacheSize > 0) {
			keyValueCache = new KeyValueCache(cacheSize);
		}*/
		
		logger.logp(WOLogLevel.INFO,"Application","<init>","RujelMarkbook started " + webserverConnectURL());
	}
	
	protected Integer _year;
	public Integer year() {
		return _year;
	}
	
	protected Object _today;
	public NSTimestamp today() {
		if(_today == NullValue)
			return new NSTimestamp();
		if(_today == null) {
			String defaultDate = SettingsReader.stringForKeyPath("ui.defaultDate", null);
			if(defaultDate == null) {
				_today = NullValue;
				return new NSTimestamp();
			} else {
				try {
					_today = (NSTimestamp)MyUtility.dateFormat().parseObject(defaultDate);
				} catch (Exception e) {
					logger.log(WOLogLevel.WARNING, "Failed parsing default date " + 
							defaultDate + ". Using today.",e);
					_today = NullValue;
					return new NSTimestamp();
				}
			}
		}
		return (NSTimestamp)_today;
	}
	
	public NSKeyValueCoding strings() {
		return _strings;
	}
	
	public WOResponse handleException(Exception anException,  WOContext aContext) {
		if(aContext == null) {
			logger.log(WOLogLevel.UNCOUGHT_EXCEPTION,"Exception occured with no WOContext",anException);
			return super.handleException(anException,aContext);
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
		logger.log(WOLogLevel.UNCOUGHT_EXCEPTION,msg.toString(),anException);

		WOComponent errorPage = pageWithName("ErrorPage", aContext);
		errorPage.takeValueForKey(anException, "throwable");
		return errorPage.generateResponse();
		
		//return super.handleException(anException,aContext);
	}

	public void setFlush(String what) {
		if(what == null)
			return;
		if(what.equals("cache")) {
			groupsForYear.removeAllObjects();
			coursesForGroup.removeAllObjects();
		} else if(what.equals("strings")) {
			_strings.flush();
		} else if(what.equals("ec")) {
			EOSharedEditingContext.defaultSharedEditingContext().invalidateAllObjects();
		}
			
	}

	public NSKeyValueCoding ecForYear = new NSKeyValueCoding() {
		private NSMutableDictionary ecDict = new NSMutableDictionary();

		public void takeValueForKey(Object value, String key) {
			ecDict.takeValueForKey(value, key);
		}

		public Object valueForKey(String key) {
			EOSharedEditingContext ec = (EOSharedEditingContext)ecDict.valueForKey(key);
			if(ec != null)
				return ec;
			EOObjectStore os = DataBaseConnector.objectStoreForTag(key);
			if(os == null)
				return null;
			ec = new EOSharedEditingContext(os);
			ecDict.takeValueForKey(ec, key);
			return ec;
		}
	};
}
