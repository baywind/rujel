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
import net.rujel.reusables.WOLogLevel;

import com.webobjects.appserver.WOSession;
import com.webobjects.eocontrol.EOKeyValueQualifier;
import com.webobjects.eocontrol.EOQualifier;
import com.webobjects.eocontrol.EOSortOrdering;
import com.webobjects.foundation.*;

public class StudentReports {
	
	protected NSMutableArray reporters;
	protected WOSession ses;
	protected String defaultID;
	protected NSMutableDictionary<String, NSKeyValueCoding> dict=
			new NSMutableDictionary<String, NSKeyValueCoding>();
	protected NSMutableDictionary<String, NSDictionary> settings;
	
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
		dict.removeAllObjects();
	}

	public static NSMutableArray reporterList(WOSession ses) {
		NSMutableArray reporters = new NSMutableArray();
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
		return reporters;
	}
	
	public NSArray reporterList() {
		if(reporters == null) {
			reporters = reporterList(ses);
			Enumeration enu = reporters.objectEnumerator();
			while (enu.hasMoreElements()) {
				NSKeyValueCoding rep = (NSKeyValueCoding) enu.nextElement();
				String key = (String)rep.valueForKey("id");
				if(key != null)
					dict.takeValueForKey(rep, key);
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

	public void initSettingsDict() {
		settings = new NSMutableDictionary<String, NSDictionary>();
		reporterList();
		NSArray presets = getPresets(false,ses);
		Enumeration enu = presets.objectEnumerator();
		while (enu.hasMoreElements()) {
			NSMutableDictionary preset = (NSMutableDictionary) enu.nextElement();
			String reporterID =  (String)preset.valueForKey("reporterID");
			if (dict.containsKey(reporterID))
				settings.setObjectForKey(preset, preset.valueForKey("title"));
		}
	}	
	public static NSArray getPresets(boolean excludeDefault, WOSession ses) {	
		File reportsDir = new File(ReportsModule.reportsFolder(),"StudentReport");
    	File[] files = reportsDir.listFiles(PlistReader.Filter);
    	NSMutableArray result = new NSMutableArray();
    	for (int i = 0; i < files.length; i++) {
    		try {
    			if(excludeDefault && files[i].getName().equals("defaultSettings.plist"))
    				continue;
    			FileInputStream fis = new FileInputStream(files[i]);
    			NSData data = new NSData(fis, fis.available());
    			fis.close();
    			String encoding = System.getProperty("PlistReader.encoding", "utf8");
    			Object plist = NSPropertyListSerialization.propertyListFromData(data, encoding);
    			if(!(plist instanceof NSDictionary))
    				continue;
    			NSDictionary preset = (NSDictionary)plist;
    			String reporterID =  (String)preset.valueForKey("reporterID");
    			if(reporterID == null)
    				continue;
    			//TODO: filter by section access
    			if(!(preset instanceof NSMutableDictionary))
    				preset = preset.mutableClone();
    			preset.takeValueForKey(files[i], "file");
    			preset.takeValueForKey(files[i].getName(), "filename");
    			result.addObject(preset);
    		} catch (Exception e) {
    			Object [] args = new Object[] {ses,e,files[i]};
    			Logger.getLogger("rujel.reports").log(WOLogLevel.WARNING,
    					"Error reading report settings plist",args);
    		}
    	}
    	return result;
	}
	
	public NSDictionary settingsNamed(String title) {
		if(settings == null) {
			initSettingsDict();
		}
		return (NSDictionary) settings.valueForKey(title);
	}
	
	public NSArray settingsAvailable() {
		if(settings == null) {
			initSettingsDict();
		}
		return settings.allKeys();
	}
	
	public static NSArray settingsAvailable(WOSession ses) {
		StudentReports reports = new StudentReports(ses);
		return reports.settingsAvailable();
	}
	
	public NSKeyValueCoding getReporter (String title) {
//		Enumeration enu = reporterList().objectEnumerator();
		if(title == null)
			title = defaultID;
		if(dict.count() == 0)
			reporterList();
		return (NSKeyValueCoding)dict.valueForKey(title);
/*		while (enu.hasMoreElements()) {
			NSKeyValueCoding reporter = (NSKeyValueCoding)enu.nextElement();
			if(title.equals(reporter.valueForKey("id")))
				return reporter;
		}
		return null;*/
	}
	
	public NSKeyValueCoding getReportForFilename(String filename) {
		if(filename == null)
			return null;
		File reportsDir = new File(ReportsModule.reportsFolder(),"StudentReport");
		File reportFile = new File(reportsDir,filename);
		if(!reportFile.exists())
			return null;
		try {
			FileInputStream fis = new FileInputStream(reportFile);
			NSData data = new NSData(fis, fis.available());
			fis.close();
			NSDictionary settings = (NSDictionary)NSPropertyListSerialization.
					propertyListFromData(data, "utf8");
			settings = settings.mutableClone();
			settings.takeValueForKey(reportFile, "file");
			settings.takeValueForKey(filename, "filename");
			return getReport((NSMutableDictionary)settings);
		} catch (IOException e) {
			Logger.getLogger("rujel.reports").log(WOLogLevel.INFO,
					"Error reading settings for StudentReport from file",
					new Object[] {ses,reportFile,e});
			return null;
		}
	}
	
	public NSKeyValueCoding getReportByName(String settingsTitle) {
		NSDictionary settings = settingsNamed(settingsTitle);
		if(settings == null)
			return null;
		return getReport(settings.mutableClone());
	}
	public NSMutableDictionary getReport(NSMutableDictionary settings) {
		String reporterID = (String)settings.valueForKey("reporterID");
		if(reporterID == null)
			throw new IllegalStateException("Named report settings '"+ settings.valueForKey("title") + 
					"' does not have required 'reporterID' key.");
		NSKeyValueCoding reporter = getReporter(reporterID);
		if(reporter == null)
			throw new IllegalStateException("Failed to find reporter with id '" +reporterID+ 
					"' for named report settings '"+ settings.valueForKey("title") + '\'');
		NSMutableDictionary result = ((NSDictionary)reporter).mutableClone();
		settings = ReporterSetup.synchronizeReportSettings(
				(NSMutableDictionary)settings, result, false, true);
		result.takeValueForKey(settings, "settings");
		return result;
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
				dict.takeValueForKey(result, defaultID);
			} catch (Exception e) {}
		}
		if(result == null)
			result = (NSKeyValueCoding)reporterList().objectAtIndex(0);
		
		if(settings != null) {
			settings = ReporterSetup.synchronizeReportSettings(
					(NSMutableDictionary)settings, result, false, true);
			result.takeValueForKey(settings, "settings");
		}
		return result;
	}
/*	
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
				if(NULL.equals(value))
					value = null;
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
*/
}
