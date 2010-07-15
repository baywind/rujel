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

package net.rujel.reports;

import java.io.File;
import java.io.FileInputStream;
import java.util.Enumeration;
import java.util.logging.Logger;

import net.rujel.reusables.ModulesInitialiser;
import net.rujel.reusables.PlistReader;
import net.rujel.reusables.SettingsReader;
import net.rujel.reusables.Various;
import net.rujel.reusables.WOLogLevel;

import com.webobjects.appserver.WOApplication;
import com.webobjects.appserver.WOContext;
import com.webobjects.appserver.WOSession;
import com.webobjects.eoaccess.EOModelGroup;
import com.webobjects.eocontrol.EOSortOrdering;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSData;
import com.webobjects.foundation.NSDictionary;
import com.webobjects.foundation.NSKeyValueCoding;
import com.webobjects.foundation.NSKeyValueCodingAdditions;
import com.webobjects.foundation.NSMutableArray;
import com.webobjects.foundation.NSPropertyListSerialization;

public class ReportsModule {
	
	static {
		System.out.println ("Initialising ReportsModule");
	}
	
	protected static File reportsFolder;

	public static Object init(Object obj, WOContext ctx) {
		if(obj == null || obj.equals("init")) {
			try {
				Object access = PlistReader.readPlist("access.plist", "RujelReports", null);
				WOApplication.application().takeValueForKey(access, "defaultAccess");
			} catch (NSKeyValueCoding.UnknownKeyException e) {
				// default access not supported
			}
		} else if(obj.equals("regimeGroups")) {
			return WOApplication.application().valueForKeyPath(
					"strings.RujelReports_Reports.regimeGroup");
		} else if(obj.equals("regimes")) {
//			return regimes(ctx);
			return WOApplication.application().valueForKeyPath(
						"strings.RujelReports_Reports.regimes");
		}
		return null;
	}
	
	public static void merge(NSDictionary dict,SettingsReader settings) {
		String reportsDir = (String)dict.valueForKey("reportsDir");
		if(reportsDir == null)
			reportsDir = settings.get("reportsDir", "CONFIGDIR/RujelReports");
		else
			settings.mergeValueToKeyPath(reportsDir, "reportsDir");
		reportsDir = Various.convertFilePath(reportsDir);
		reportsFolder = new File(reportsDir);
		Object value = dict.objectForKey("auth.access");
		if(value == null)
			return;
		NSDictionary access = null;
		if(value instanceof NSDictionary)
			access = (NSDictionary)value;
		else
			access = (NSDictionary)PlistReader.readPlist(value.toString(), null);
		settings.mergeValueToKeyPath(access, "auth.access");
	}
/*	
	public static Object regimes(WOContext ctx) {
		Object result = null;
		if(Various.boolForObject(ctx.session().valueForKeyPath("readAccess.read.CoursesReport")))
			result = WOApplication.application().valueForKeyPath(
				"strings.RujelReports_Reports.CoursesReport.regime");
		if(Various.boolForObject(ctx.session().valueForKeyPath("readAccess.read.CustomReport"))) {
			Object res2 = WOApplication.application().valueForKeyPath(
					"strings.RujelReports_Reports.CustomReport.regime");
			if(result == null)
				result = res2;
			else
				result = new NSArray(new Object[] {result,res2});
		}
		return result;
	}

*/    public static NSMutableArray reportsFromDir(String dir,WOContext context) {
    	NSMutableArray reports = new NSMutableArray();
    	File reportsDir = new File(reportsFolder, dir);
    	if (reportsDir.isDirectory()) {
    		File[] files = reportsDir.listFiles(PlistReader.Filter);
    		WOSession session = context.session();
  		NSKeyValueCodingAdditions readAccess = 
  					(NSKeyValueCodingAdditions)session.valueForKey("readAccess");
   		for (int i = 0; i < files.length; i++) {
    			try {
    				FileInputStream fis = new FileInputStream(files[i]);
    				NSData data = new NSData(fis, fis.available());
    				fis.close();
    				String encoding = System.getProperty(
    						"PlistReader.encoding", "utf8");
    				Object plist = NSPropertyListSerialization
    				.propertyListFromData(data, encoding);
    				if(plist instanceof NSDictionary) {
    					if(checkInDict((NSDictionary)plist,readAccess))
    						reports.addObject(plist);
    				} else if (plist instanceof NSArray) {
    					Enumeration enu = ((NSArray)plist).objectEnumerator();
    					while (enu.hasMoreElements()) {
    						NSDictionary dict = (NSDictionary) enu.nextElement();
    						if(checkInDict(dict,readAccess))
    							reports.addObject(dict);
    					}
    				}
    			} catch (Exception e) {
    				Object [] args = new Object[] {session,e,files[i].getAbsolutePath()};
    				Logger.getLogger("rujel.reports").log(WOLogLevel.WARNING,
    						"Error reading CoursesReport plist",args);
    			}
    		}
    		EOSortOrdering.sortArrayUsingKeyOrderArray(reports, ModulesInitialiser.sorter);
    	}
    	return reports;
    }

    protected static boolean checkInDict(NSDictionary dict,
    		NSKeyValueCodingAdditions readAccess) {
		String entity = (String)dict.valueForKey("entity");
		if(entity != null) {
			if(EOModelGroup.defaultGroup().entityNamed(entity) == null)
				return false;
		}
    	NSArray checkAccess = (NSArray)dict.valueForKey("checkAccess");
    	if(checkAccess != null && checkAccess.count() > 0) {
//  		NSKeyValueCodingAdditions readAccess = 
//  		(NSKeyValueCodingAdditions)session.valueForKey("readAccess");
    		Enumeration enu = checkAccess.objectEnumerator();
    		while (enu.hasMoreElements()) {
    			String acc = (String) enu.nextElement();
    			if(Various.boolForObject(
    					readAccess.valueForKeyPath("_read." + acc)))
    				return false;
    		}
    	} else if(entity != null) {
    			return Various.boolForObject(readAccess.valueForKeyPath("read." + entity));
    	}
    	//reports.addObject(dict);
    	return true;
    }
}
