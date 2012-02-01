// ReporterSetup.java: Class file for WO Component 'ReporterSetup'

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

package net.rujel.ui;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.logging.Logger;

import net.rujel.reports.ReportsModule;
import net.rujel.reports.StudentReports;
import net.rujel.reusables.DisplayAny;
import net.rujel.reusables.ModulesInitialiser;
import net.rujel.reusables.PlistReader;
import net.rujel.reusables.SettingsReader;
import net.rujel.reusables.Various;
import net.rujel.reusables.WOLogFormatter;
import net.rujel.reusables.WOLogLevel;

import com.webobjects.appserver.WOActionResults;
import com.webobjects.appserver.WOContext;
import com.webobjects.appserver.WOComponent;
import com.webobjects.appserver.WOSession;
import com.webobjects.eocontrol.*;
import com.webobjects.foundation.*;

public class ReporterSetup extends WOComponent {
//	public static final String SETTINGS = "reportSettingsForStudent";
	
    public WOComponent returnPage;
    public NSDictionary reporter;
    public NSArray reports;
	public NSKeyValueCoding item;
	public NSKeyValueCoding subItem;
	public NSKeyValueCoding optItem;
	public NSMutableArray presets;
	public String presetName;
//	protected boolean showPresets = false;

    public ReporterSetup(WOContext context) {
        super(context);
        if(Various.boolForObject(session().valueForKeyPath("readAccess.edit.ReporterSetup")))
        	presetName = (String)context.session().valueForKeyPath(
        				"strings.Strings.PrintReport.defaultSettings");
        else
        	presetName = null;
    }
    
	public String subStyle() {
		if(item == null || Various.boolForObject(item.valueForKey("active")))
			return null;
		return "display:none;";
	}
	
	public void setReporter(NSDictionary dict) {
//		NSArray defaults = (NSArray)session().valueForKeyPath("modules." + settingsName);
//		reports = PlistReader.cloneArray(defaults, true);
		reporter = dict;
        EOQualifier[] quals = new EOQualifier[2];
        quals[0] = new EOKeyValueQualifier("reporterID", EOQualifier.QualifierOperatorEqual, 
        		reporter.valueForKey("id"));
        if("default".equals(reporter.valueForKey("id"))) {
        	quals[1] = new EOKeyValueQualifier("reporterID",
        			EOQualifier.QualifierOperatorEqual, null);
        	quals[0] = new EOOrQualifier(new NSArray(quals));
        }
        quals[1] = new EOKeyValueQualifier("id", EOQualifier.QualifierOperatorEqual, null);
    	quals[0] = new EOAndQualifier(new NSArray(quals));
//		presets = ReportsModule.reportsFromDir("StudentReport", session(), quals[0]);
    	File reportsDir = new File(ReportsModule.reportsFolder(), "StudentReport");
    	File[] files = reportsDir.listFiles(PlistReader.Filter);
    	presets = new NSMutableArray();
    	for (int i = 0; i < files.length; i++) {
    		try {
    			FileInputStream fis = new FileInputStream(files[i]);
    			NSData data = new NSData(fis, fis.available());
    			fis.close();
    			String encoding = System.getProperty("PlistReader.encoding", "utf8");
    			Object plist = NSPropertyListSerialization.propertyListFromData(data, encoding);
    			if(!(plist instanceof NSDictionary))
    				continue;
    			NSDictionary preset = (NSDictionary)plist;
    			if(preset.valueForKey("id") != null)
    				continue;
    			String id = (String)reporter.valueForKey("id");
    			if(id == null) id = "default";
    			if(!id.equals(preset.valueForKey("reporterID")) && 
    					(preset.valueForKey("reporterID") != null || !id.equals("default")))
    				continue;
    			if(!(preset instanceof NSMutableDictionary))
    				preset = preset.mutableClone();
    			preset.takeValueForKey(files[i], "file");
    			if(files[i].getName().equals("defaultSettings.plist")) {
    				preset.takeValueForKey("gerade", "style");
    				presets.insertObjectAtIndex(preset, 0);
    			} else {
    				preset.takeValueForKey("ungerade", "style");
    				presets.addObject(preset);
    			}
    		} catch (Exception e) {
    			Object [] args = new Object[] {session(),e,files[i]};
    			Logger.getLogger("rujel.reports").log(WOLogLevel.WARNING,
    					"Error reading CoursesReport plist",args);
    		}
    	}
    	reports = (NSArray)dict.valueForKey("options");
		NSMutableDictionary settings = (NSMutableDictionary)dict.valueForKey("settings");
//		if(settings == null)
//			settings = PlistReader.cloneDictionary(defaultSettings, true);
//		else if(Various.boolForObject(session().valueForKeyPath("readAccess.edit.ReporterSetup")))
//        	presetName = (String)settings.valueForKey("title");
		if(settings != null)
			settings = StudentReports.synchronizeReportSettings(settings, reporter, false, true);
	}
	
	public WOComponent submit() {
//		EOSortOrdering.sortArrayUsingKeyOrderArray(reports, ModulesInitialiser.sorter);
		NSMutableDictionary settings = (NSMutableDictionary)reporter.objectForKey("settings");
		settings = StudentReports.synchronizeReportSettings(settings, reporter,true,false);
		reporter.takeValueForKey(settings, "settings");
//		settings.takeValueForKey(presetName, "title");
//		session().setObjectForKey(settings, SETTINGS);
		returnPage.ensureAwakeInContext(context());
//		showPresets = false;
		return returnPage;
	}
	
	public WOComponent savePreset() {
		NSMutableDictionary settings = (NSMutableDictionary)reporter.objectForKey("settings");
		settings = StudentReports.synchronizeReportSettings(settings, reporter,true,false);
		String defaultName = (String)session().valueForKeyPath(
				"strings.Strings.PrintReport.defaultSettings");
		File file = null;
		boolean cantEdit = !Various.boolForObject(session().valueForKeyPath(
														"readAccess.edit.ReporterSetup"));
		if(presetName == null || presetName.equals(defaultName)) {
	        if(cantEdit) {
	        	StudentReports.synchronizeReportSettings(getDefaultSettings(reporter), reporter, false, true);
	        	presetName = null;
	        	return null;
	        }
//			defaultSettings = settings.immutableClone();
			presetName = defaultName;
			file = new File(ReportsModule.reportsFolder(),"/StudentReport/defaultSettings.plist");
			
			if(presets == null || presets.count() == 0) {
				presets = new NSMutableArray(settings);
			} else {
				NSDictionary first = (NSDictionary)presets.objectAtIndex(0);
				if(file.equals(first.valueForKey("file"))) {
					presets.replaceObjectAtIndex(settings, 0);
				} else {
					presets.insertObjectAtIndex(settings, 0);
				}
			}
		} else if(presets != null) {
			for(int i = 0; i < presets.count(); i++) {
				NSDictionary pre = (NSDictionary) presets.objectAtIndex(i);
				if(presetName.equals(pre.valueForKey("title"))) {
					if(cantEdit && !Various.boolForObject(pre.valueForKey("isNew"))) {
						settings = PlistReader.cloneDictionary(pre, true);
						StudentReports.synchronizeReportSettings(settings, reporter, false, true);
						presetName = null;
			        	return null;
					}
					file = (File)pre.valueForKey("file");
					settings.takeValueForKey(new Integer(i), "index");
					settings.takeValueForKey(pre.valueForKey("isNew"), "isNew");
					break;
				}
			}
		}
		if(file == null) {
	        String path = SettingsReader.stringForKeyPath("reportsDir", "CONFIGDIR/RujelReports");
	        path = path + "/StudentReport";
	        File folder = new File(Various.convertFilePath(path));
	        Calendar cal = Calendar.getInstance();
	        StringBuilder buf = new StringBuilder(10);
	        buf.append(cal.get(Calendar.YEAR));
	        int len = cal.get(Calendar.MONTH) + 1;
	        if(len < 10)
	        	buf.append('0');
	        buf.append(len);
	        len = cal.get(Calendar.DATE);
	        if(len < 10)
	        	buf.append('0');
	        buf.append(len);
	        len = buf.length();
        	buf.append(".plist");
	        file = new File(folder,buf.toString());
	        if(file.exists()) {
	        	char idx = 'a';
	        	buf.insert(len, idx);
	        	while (idx <= 'z') {
					file = new File(folder,buf.toString());
					if(!file.exists())
						break;
					idx++;
					buf.setCharAt(len, idx);
				}
	        	if(idx > 'z')
	        		throw new IllegalStateException("Too many presets");
	        }
		}
		try {
			if(!file.exists())
				file.getParentFile().mkdirs();
	        settings.takeValueForKey(presetName, "title");
	        settings.takeValueForKey(reporter.valueForKey("id"), "reporterID");
			Integer index = (Integer)settings.removeObjectForKey("index");
			String xml = NSPropertyListSerialization.xmlStringFromPropertyList(settings);
			FileOutputStream out = new FileOutputStream(file);
			OutputStreamWriter writer = new OutputStreamWriter(out, "utf8");
			writer.write(xml);
			writer.close();
			out.close();
			settings.takeValueForKey(file, "file");
			settings.takeValueForKey((presetName == defaultName)?"gerade":"ungerade","style");
			if(presetName == defaultName)
				return null;
			if(index == null && cantEdit) 
				settings.takeValueForKey(Boolean.TRUE, "isNew");
			if(index != null)
				presets.replaceObjectAtIndex(settings, index.intValue());
			else if(presets == null)
				presets = new NSMutableArray(settings);
			else
				presets.addObject(settings);
		} catch (Exception e) {
			Logger.getLogger("rujel.reports").log(WOLogLevel.WARNING,
					"Error saving StudentReport preset " + presetName,
					new Object[] {session(), file, e});
		}
		return null;
	}
	
	public WOActionResults deletePreset() {
		if(presets != null && item != null && presets.removeObject(item)) {
			File file = (File)item.valueForKey("file");
			if(file != null && file.exists())
				file.delete();
		}
//		showPresets = true;
		return this;
	}
	
	public String deletePresetOnClick() {
		StringBuilder buf = new StringBuilder("if(confirm('");
		String message = (String)session().valueForKeyPath(
				"strings.Strings.messages.areYouShure");
		String del = (String)session().valueForKeyPath(
				"strings.Reusables_Strings.uiElements.Delete");
		message = String.format(message, del);
		buf.append(message);
		buf.append("'))ajaxPopupAction('");
		buf.append(context().componentActionURL()).append("');");
		return buf.toString();
	}
	
	public WOActionResults usePreset() {
		NSMutableDictionary settings = (item == null)?getDefaultSettings(reporter):
				PlistReader.cloneDictionary((NSDictionary)item, true);
		boolean canEdit = Various.boolForObject(session().valueForKeyPath(
				"readAccess.edit.ReporterSetup"));
		if(item == null) {
				presetName = (!canEdit)?null:(String)session().valueForKeyPath(
							"strings.Strings.PrintReport.defaultSettings");
		} else {
        	presetName = (canEdit || Various.boolForObject(item.valueForKey("isNew")))?
        		(String)settings.valueForKey("title"):null;
        }
		StudentReports.synchronizeReportSettings(settings, reporter, false, true);
//		showPresets = true;
		return this;
	}
	
/*	public static NSMutableDictionary synchronizeReportSettings(NSMutableDictionary settings,
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
*/	
	public static NSMutableDictionary getDefaultSettings(NSDictionary reporter) {
		NSMutableDictionary result = null;
		String path = SettingsReader.stringForKeyPath("reportsDir","CONFIGDIR/RujelReports");
		path = path + "/StudentReport/" + reporter.valueForKey("id") + "_defaults.plist";
		Object plist = PlistReader.readPlist(path, null);
		if(plist instanceof NSDictionary) {
			result = PlistReader.cloneDictionary((NSDictionary)plist, true);
			result = StudentReports.synchronizeReportSettings(result, reporter, false, false);
		} else {
	/*		result = (NSMutableDictionary)reporter.valueForKey("settings");
			if(result != null)
				result = PlistReader.cloneDictionary(result, true);*/
			result = StudentReports.synchronizeReportSettings(result, reporter, true, false);
		}
		return result;
	}

	public static NSArray prepareReports(
			WOSession ses, NSMutableDictionary reportSettings) {
		NSMutableDictionary settings = (NSMutableDictionary)reportSettings.valueForKeyPath(
				"reporter.settings");
		if(settings == null) {
			NSMutableDictionary reporter = (NSMutableDictionary)reportSettings.valueForKey("reporter");
			settings = getDefaultSettings(reporter);
			settings.takeValueForKey(ses.valueForKeyPath(
				"strings.Strings.PrintReport.defaultSettings"), "title");
			reporter.takeValueForKey(settings, "settings");
		}
		ses.setObjectForKey(reportSettings,"reportForStudent");
		NSArray reports = (NSArray)ses.valueForKeyPath("modules.reportForStudent");
		ses.removeObjectForKey("reportForStudent");
		return reports;
	}
	
/*	public String presetsStyle() {
		if(showPresets)
			return "border-left:1px #666666 solid;";
		return "display:none;border-left:1px #666666 solid;";
	}*/
	
	public String savePresetOnClick() {
		String result = "enumerate(form,'sorting',1);ajaxPost(this);";
		if(!Various.boolForObject(session().valueForKeyPath("readAccess.edit.ReporterSetup"))) {
			return "f=form.presetName;if(f.value.length){" +
			result + "}else{disabled=true;f.focus();}return false;";
		}
		return result + "return false;";
	}
	
	public NSArray options() {
		return (NSArray)DisplayAny.ValueReader.evaluateValue(
				subItem.valueForKey("options"), item, this);
	}
	
	public Object selection() {
		Object obj = subItem.valueForKey("active");
		if(obj instanceof String) {
			try {
				EOEditingContext ec = (EOEditingContext)returnPage.valueForKey("ec");
				obj = Various.parseEO((String)obj, ec);
			} catch (Exception e) {
				;
			}
		}
		return obj;
	}
	
	public void setSelection(Object sel) {
		subItem.takeValueForKey(sel, "active");
	}
	
	public String optTitle() {
		String key = (String)subItem.valueForKey("displayString");
		return (String)optItem.valueForKey(key);
	}
}