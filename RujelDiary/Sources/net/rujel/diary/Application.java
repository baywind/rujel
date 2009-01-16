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

import java.io.InputStream;
import java.util.logging.Logger;

import net.rujel.reusables.*;

import com.webobjects.appserver.WOApplication;
import com.webobjects.appserver.WORequestHandler;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSKeyValueCoding;
import com.webobjects.foundation.NSMutableDictionary;

public class Application extends UTF8Application {
	protected static Logger logger = Logger.getLogger("rujel");
	protected StringStorage _strings = new StringStorage(resourceManager());;

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
		InputStream propsIn = (propertiesPath!=null)?null:
			resourceManager().inputStreamForResourceNamed("logging.properties", "app", null);
		LogInitialiser.initLogging(propsIn, propertiesPath, logger);
		
		//EODatabaseContext.setDefaultDelegate(new CompoundPKeyGenerator());
		DataBaseConnector.makeConnections();

		net.rujel.interfaces.EOInitialiser.initAll();
		SettingsReader node = SettingsReader.settingsForPath("modules",true);
		ModulesInitialiser.initModules(node,"init");
				
/*		int cacheSize = SettingsReader.intForKeyPath("ui.keyValueCacheSize", 0);
		if(cacheSize > 0) {
			keyValueCache = new KeyValueCache(cacheSize);
		}*/
		
		logger.logp(WOLogLevel.INFO,"Application","<init>","RujelMarkbook started " + webserverConnectURL());
	}
	
	public NSKeyValueCoding strings() {
		return _strings;
	}
}
