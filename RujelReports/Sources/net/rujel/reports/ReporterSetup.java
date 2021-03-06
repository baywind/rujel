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

package net.rujel.reports;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.logging.Logger;

import net.rujel.reusables.DisplayAny;
import net.rujel.reusables.ModulesInitialiser;
import net.rujel.reusables.PlistReader;
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
	
	public static final String NULL = "<Null>";
    public WOComponent returnPage;
    public NSDictionary reporter;
    public NSArray reports;
	public NSKeyValueCoding item;
	public NSKeyValueCoding subItem;
	public NSKeyValueCoding optItem;
	public NSMutableArray presets;
	public String presetName;
	public String submitPath;
	protected File reportsDir = ReportsModule.reportsFolder("StudentReport");

    public ReporterSetup(WOContext context) {
        super(context);
        if(Various.boolForObject(session().valueForKeyPath("readAccess.edit.ReporterSetup")))
        	presetName = (String)context.session().valueForKeyPath(
        				"strings.Strings.PrintReport.defaultSettings");
        else
        	presetName = null;
    }
    
    public void setDir(String dir) {
    	reportsDir = ReportsModule.reportsFolder(dir);
    }
    
	public String subStyle() {
		if(item == null || Various.boolForObject(item.valueForKey("active")))
			return null;
		return "display:none;";
	}
	
	public void setReporter(NSDictionary dict) {
		reporter = dict;
		String id = (String)reporter.valueForKey("id");
		if(id == null) id = "default";
        EOQualifier[] quals = new EOQualifier[2];
        quals[0] = new EOKeyValueQualifier("reporterID", EOQualifier.QualifierOperatorEqual,id);
        if("default".equals(id)) {
        	quals[1] = new EOKeyValueQualifier("reporterID",
        			EOQualifier.QualifierOperatorEqual, null);
        	quals[0] = new EOOrQualifier(new NSArray(quals));
        }
        quals[1] = new EOKeyValueQualifier("id", EOQualifier.QualifierOperatorEqual, null);
    	quals[0] = new EOAndQualifier(new NSArray(quals));
//		presets = ReportsModule.reportsFromDir("StudentReport", session(), quals[0]);
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
		if(settings != null)
			settings = synchronizeReportSettings(settings, reporter, false, true);
	}
	
	public WOComponent submit() {
		NSMutableDictionary settings = (NSMutableDictionary)reporter.objectForKey("settings");
		settings = synchronizeReportSettings(settings, reporter,true,false);
		if(settings != null)
			reporter.takeValueForKey(settings, "settings");
		returnPage.ensureAwakeInContext(context());
		context().setUserInfoForKey(reporter, "submittedReporter");
		if(submitPath != null)
			returnPage.takeValueForKey(reporter, submitPath);
		return returnPage;
	}
	
	public WOComponent savePreset() {
		NSMutableDictionary settings = (NSMutableDictionary)reporter.objectForKey("settings");
		settings = synchronizeReportSettings(settings, reporter,true,false);
		String defaultName = (String)session().valueForKeyPath(
				"strings.Strings.PrintReport.defaultSettings");
		File file = null;
		boolean cantEdit = !Various.boolForObject(session().valueForKeyPath(
														"readAccess.edit.ReporterSetup"));
		if(presetName == null || presetName.equals(defaultName)) {
	        if(cantEdit) {
	        	synchronizeReportSettings(getDefaultSettings(reporter, reportsDir),
	        			reporter, false, true);
	        	presetName = null;
	        	return null;
	        }
			presetName = defaultName;
			file = new File(reportsDir,"defaultSettings.plist");
			
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
						synchronizeReportSettings(settings, reporter, false, true);
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
//	        String path = SettingsReader.stringForKeyPath("reportsDir", "CONFIGDIR/RujelReports");
//	        path = path + "/StudentReport";
//	        File folder = new File(Various.convertFilePath(path));
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
	        file = new File(reportsDir,buf.toString());
	        if(file.exists()) {
	        	char idx = 'a';
	        	buf.insert(len, idx);
	        	while (idx <= 'z') {
					file = new File(reportsDir,buf.toString());
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
					"Error saving Report preset " + presetName,
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
		NSMutableDictionary settings = (item == null)?getDefaultSettings(reporter, reportsDir):
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
		synchronizeReportSettings(settings, reporter, false, true);
//		showPresets = true;
		return this;
	}
	
	public static NSMutableDictionary getDefaultSettings(NSDictionary reporter, File dir) {
		NSMutableDictionary result = null;
		File file = new File(dir, reporter.valueForKey("id") + "_defaults.plist");
		Object plist = null;
		if(file.exists()) {
			try {
				FileInputStream fis = new FileInputStream(file);
				plist = PlistReader.readPlist(fis, null);
			} catch (java.io.FileNotFoundException e) {
				return null;
			} catch (Exception ioex) {
				Logger.getLogger("rujel.reports").log(WOLogLevel.WARNING,
						"Error reading default settings plist", new Object[] {file, ioex});
				return null;
			}
		}
			if(plist instanceof NSDictionary) {
				result = PlistReader.cloneDictionary((NSDictionary)plist, true);
				result = synchronizeReportSettings(result, reporter, false, false);
			} else {
				result = synchronizeReportSettings(result, reporter, true, false);
			}
		return result;
	}

	public static NSArray prepareReports(
			WOSession ses, NSMutableDictionary reportSettings, File dir) {
		NSMutableDictionary settings = (NSMutableDictionary)reportSettings.valueForKeyPath(
				"reporter.settings");
		if(settings == null) {
			NSMutableDictionary reporter = (NSMutableDictionary)reportSettings.valueForKey("reporter");
			settings = getDefaultSettings(reporter, dir);
			if(settings != null)
			settings.takeValueForKey(ses.valueForKeyPath(
				"strings.Strings.PrintReport.defaultSettings"), "title");
			reporter.takeValueForKey(settings, "settings");
		}
		ses.setObjectForKey(reportSettings,"reportForStudent");
		NSArray reports = (NSArray)ses.valueForKeyPath("modules.reportForStudent");
		ses.removeObjectForKey("reportForStudent");
		return reports;
	}
	
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
			if(obj.equals(NULL))
				return null;
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
		if(sel == null)
			sel = NULL;
		subItem.takeValueForKey(sel, "active");
	}
	
	public String optTitle() {
		String key = (String)subItem.valueForKey("displayString");
		return (String)optItem.valueForKey(key);
	}

	public static NSMutableDictionary synchronizeReportSettings(NSMutableDictionary settings,
			NSKeyValueCoding reporter, boolean updSettings, boolean updReports) {
		NSArray reports = (NSArray)reporter.valueForKey("options");
		if(reports == null)
			return settings;
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
			boolean updSubs = updSettings || subs == null;
			if(subs == null) {
				subs = (preSubs == null)? new NSMutableDictionary() : preSubs.mutableClone();
				settings.setObjectForKey(subs, key);
			} else if(keys !=null) {
				keys.removeObject(key);
				skeys = subs.allKeys().mutableClone();
				skeys.removeObject("active");
				skeys.removeObject("sort");
			}
			if(updSubs) {
				Object value = rp.valueForKey("active");
				if(NULL.equals(value))
					value = null;
//				if(value instanceof EOEnterpriseObject)
//					value = WOLogFormatter.formatEO((EOEnterpriseObject)value);
				subs.takeValueForKey(value,"active");
			} else if(updReports) {
				rp.takeValueForKey(subs.valueForKey("active"), "active");
			}
			if(updSubs)
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
					if(updSubs) {
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
	
	public String submitTitle() {
		String title = (String)reporter.valueForKey("submitTitle");
		if(title == null) {
			NSKeyValueCodingAdditions source = (context().hasSession())? session() :application();
			title = (String)source.valueForKeyPath("strings.Reusables_Strings.uiElements.Submit");
		}
		return title;
	}
}