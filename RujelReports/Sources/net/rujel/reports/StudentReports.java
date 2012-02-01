// StudentReports.java

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
import java.io.IOException;
import java.util.Enumeration;
import java.util.logging.Logger;

import net.rujel.reusables.ModulesInitialiser;
import net.rujel.reusables.PlistReader;
import net.rujel.reusables.WOLogFormatter;
import net.rujel.reusables.WOLogLevel;

import com.webobjects.appserver.WOSession;
import com.webobjects.eocontrol.EOEnterpriseObject;
import com.webobjects.eocontrol.EOKeyValueQualifier;
import com.webobjects.eocontrol.EOQualifier;
import com.webobjects.eocontrol.EOSortOrdering;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSData;
import com.webobjects.foundation.NSDictionary;
import com.webobjects.foundation.NSKeyValueCoding;
import com.webobjects.foundation.NSMutableArray;
import com.webobjects.foundation.NSMutableDictionary;
import com.webobjects.foundation.NSPropertyListSerialization;

public class StudentReports {
	
	protected NSMutableArray reporters;
	protected WOSession ses;
	protected String defaultID;
	
	public StudentReports(WOSession session) {
		super();
		ses = session;
	}
	
	public String defaultID() {
		if(defaultID == null)
			defaultSettings();
		return defaultID;
	}
	
	public void reset() {
		reporters = null;
		defaultID = null;
	}

	public NSArray reporterList() {
		if(reporters == null) {
			reporters = new NSMutableArray();
			EOQualifier qual = new EOKeyValueQualifier("id",
					EOQualifier.QualifierOperatorNotEqual, null);
			NSArray list = ReportsModule.reportsFromDir("StudentReport", ses, qual);
			if(list != null && list.count() > 0)
				reporters.addObjectsFromArray(list);
			if(ses == null)
				list = ModulesInitialiser.useModules(null, "studentReporter");
			else
				list = (NSArray)ses.valueForKeyPath("modules.studentReporter");
			if(list != null && list.count() > 0)
				reporters.addObjectsFromArray(list);
			if(reporters.count() > 1) {
	    		EOSortOrdering.sortArrayUsingKeyOrderArray(reporters, ModulesInitialiser.sorter);
			}
		}
		return reporters;
	}
	
	public NSDictionary defaultSettings() {
		NSDictionary settings = null;
		if(defaultID == null) {
			File file = new File(ReportsModule.reportsFolder(),
					"StudentReport/defaultSettings.plist");
			if(file.exists()) {
				try {
					FileInputStream fis = new FileInputStream(file);
					NSData data = new NSData(fis, fis.available());
					fis.close();
					settings = (NSDictionary)NSPropertyListSerialization.propertyListFromData(
							data, "utf8");
					defaultID = (String)settings.valueForKey("reporterID");
				} catch (IOException e) {
					Logger.getLogger("rujel.reports").log(WOLogLevel.WARNING,
							"Error reading defaultSettings for StudentReport",
							new Object[] {ses,file,e});
				}
			}
		}
		if(defaultID == null)
			defaultID = "default";
		return settings;
	}
	
	public NSKeyValueCoding getReporter (String title) {
		Enumeration enu = reporterList().objectEnumerator();
		if(title == null)
			title = defaultID;
		while (enu.hasMoreElements()) {
			NSKeyValueCoding reporter = (NSKeyValueCoding)enu.nextElement();
			if(title.equals(reporter.valueForKey("id")))
				return reporter;
		}
		return null;
	}
	
	public NSKeyValueCoding defaultReporter() {
		NSDictionary settings = defaultSettings();
		NSKeyValueCoding result = null;
		if(reporters != null) {
			result = getReporter(null);
			if(result == null)
				result = (NSKeyValueCoding)reporters.objectAtIndex(0);
		} else {
			EOQualifier qual = new EOKeyValueQualifier("id",
					EOQualifier.QualifierOperatorEqual, defaultID);
			NSArray list = ReportsModule.reportsFromDir("StudentReport", ses, qual);
			if(list != null && list.count() > 0) {
				result = (NSKeyValueCoding)list.objectAtIndex(0);
			} else {
				if(ses == null)
					list = ModulesInitialiser.useModules(null, "studentReporter");
				else
					list = (NSArray)ses.valueForKeyPath("modules.studentReporter");
				if(list != null && list.count() > 0) {
					Enumeration enu = list.objectEnumerator();
					while (enu.hasMoreElements()) {
						NSKeyValueCoding reporter = (NSKeyValueCoding)enu.nextElement();
						if(defaultID.equals(reporter.valueForKey("id"))) {
							result = reporter;
							break;
						}
					}
				}
			}
		}
		if(result == null) {
			File file = new File(ReportsModule.reportsFolder(),
					"StudentReport/DefaultReporter.plist");
			try {
				result = (NSDictionary)PlistReader.readPlist(new FileInputStream(file), null);
			} catch (Exception e) {}
		}
		if(result == null)
			result = (NSKeyValueCoding)reporterList().objectAtIndex(0);
		
		if(settings != null) {
			settings = synchronizeReportSettings(
					(NSMutableDictionary)settings, result, false, true);
			result.takeValueForKey(settings, "settings");
		}
		return result;
	}
	
	public static NSMutableDictionary synchronizeReportSettings(NSMutableDictionary settings,
			NSKeyValueCoding reporter, boolean updSettings, boolean updReports) {
		NSArray reports = (NSArray)reporter.valueForKey("options");
		if(reports == null)
			return null;
		NSMutableArray keys = null;
		NSDictionary preSettings = (NSDictionary)reporter.valueForKey("settings");
		if(settings == null) {
			settings = (preSettings == null)?new NSMutableDictionary():
				PlistReader.cloneDictionary(preSettings, true);
			updSettings = true;
		} else {
			keys = settings.allKeys().mutableClone();
		}
		Enumeration enu = reports.objectEnumerator();
		while (enu.hasMoreElements()) {
			NSMutableDictionary rp = (NSMutableDictionary) enu.nextElement();
			Object key = rp.valueForKey("id");
			NSMutableDictionary subs = (NSMutableDictionary)settings.objectForKey(key);
			NSDictionary preSubs = (preSettings == null) ? null :
				(NSDictionary)preSettings.objectForKey(key);
//			NSDictionary defaults = (defaultSettings==null)?null:
//				(NSDictionary)defaultSettings.objectForKey(key);
			NSMutableArray skeys = null;
			if(subs == null) {
				subs = (preSubs == null)? new NSMutableDictionary() : preSubs.mutableClone();
				settings.setObjectForKey(subs, key);
			} else if(keys !=null) {
				keys.removeObject(key);
				skeys = subs.allKeys().mutableClone();
				skeys.removeObject("active");
				skeys.removeObject("sort");
			}
			if(updSettings || subs.valueForKey("active") == null) {
				Object value = rp.valueForKey("active");
//				if(value instanceof EOEnterpriseObject)
//					value = WOLogFormatter.formatEO((EOEnterpriseObject)value);
				subs.takeValueForKey(value,"active");
			} else if(updReports) {
				rp.takeValueForKey(subs.valueForKey("active"), "active");
			}
			if(updSettings || subs.valueForKey("sort") == null)
				subs.takeValueForKey(rp.valueForKey("sort"),"sort");
			else if(updReports)
				rp.takeValueForKey(subs.valueForKey("sort"), "sort");
			NSArray list = (NSArray)rp.valueForKey("options");
			if(list != null) {
			Enumeration options = list.objectEnumerator();
			while (options.hasMoreElements()) {
				NSMutableDictionary opt = (NSMutableDictionary) options.nextElement();
				key = opt.valueForKey("id");
				if(subs.objectForKey(key) == null) {
					subs.takeValueForKey(opt.objectForKey(key),(String)key);
				}
				if(updSettings) {
					Object value = opt.valueForKey("active");
					if(value instanceof EOEnterpriseObject)
						value = WOLogFormatter.formatEO((EOEnterpriseObject)value);
					if(value == null && preSubs != null)
						value = preSubs.objectForKey(key);
					subs.takeValueForKey(value,(String)key);
				} else if(updReports) {
					opt.takeValueForKey(subs.objectForKey(key),"active");
				}
				if(skeys != null)
					skeys.removeObject(key);
			} // options enumeration
			}
			if(skeys != null && skeys.count() > 0) {
				Enumeration senu = skeys.objectEnumerator();
				while (senu.hasMoreElements()) {
					key = senu.nextElement();
					subs.removeObjectForKey(key);
				}
			}
		} // reports enumeration
		if(keys != null && keys.count() > 0) {
			enu = keys.objectEnumerator();
			while (enu.hasMoreElements()) {
				Object key = enu.nextElement();
				settings.removeObjectForKey(key);
			}
		}
		if(preSettings != null) {
			enu = preSettings.keyEnumerator();
			while (enu.hasMoreElements()) {
				String key = (String) enu.nextElement();
				if(settings.valueForKey(key) == null)
					settings.takeValueForKey(preSettings.valueForKey(key), key);
			}
		}
		if(updReports) {
			EOSortOrdering.sortArrayUsingKeyOrderArray(
					(NSMutableArray)reports, ModulesInitialiser.sorter);
			reporter.takeValueForKey(settings, "settings");
		}
		return settings;
	}
}
