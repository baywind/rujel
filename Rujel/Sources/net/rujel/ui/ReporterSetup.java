package net.rujel.ui;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.logging.Logger;

import net.rujel.reusables.ModulesInitialiser;
import net.rujel.reusables.PlistReader;
import net.rujel.reusables.SettingsReader;
import net.rujel.reusables.Various;
import net.rujel.reusables.WOLogLevel;

import com.webobjects.appserver.WOActionResults;
import com.webobjects.appserver.WOContext;
import com.webobjects.appserver.WOComponent;
import com.webobjects.appserver.WOSession;
import com.webobjects.eocontrol.EOSortOrdering;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSDictionary;
import com.webobjects.foundation.NSKeyValueCoding;
import com.webobjects.foundation.NSMutableArray;
import com.webobjects.foundation.NSMutableDictionary;
import com.webobjects.foundation.NSPropertyListSerialization;

public class ReporterSetup extends WOComponent {
	public static final String SETTINGS = "reportSettingsForStudent";
	
	protected static NSDictionary defaultSettings;
	
    public WOComponent returnPage;
    public NSMutableArray reports;
	public NSKeyValueCoding item;
	public NSKeyValueCoding subItem;
	public NSKeyValueCoding optItem;
	public NSMutableArray presets;
	public String presetName;
	protected boolean showPresets = false;

    public ReporterSetup(WOContext context) {
        super(context);
        String path = SettingsReader.stringForKeyPath("reportsDir", "CONFIGDIR/RujelReports");
        path = path + "/StudentReport";
        File folder = new File(Various.convertFilePath(path));
        if(folder.exists()) {
        	File[] files = folder.listFiles(PlistReader.Filter);
        	if(files != null && files.length > 0) {
        		for (int i = 0; i < files.length; i++) {
					try {
						FileInputStream in = new FileInputStream(files[i]);
						Object plist = PlistReader.readPlist(in, null);
						if(plist instanceof NSDictionary) {
							if(files[i].getName().equals("defaultSettings.plist")) {
								NSArray rps = (NSMutableArray)context.session().valueForKeyPath(
											"modules.reportSettingsForStudent");
								NSMutableDictionary preset = PlistReader.cloneDictionary(
										(NSDictionary)plist, true);
								synchronizeReportSettings(preset, rps, false, false);
								defaultSettings = preset.immutableClone();
							} else {
								NSMutableDictionary preset = (
										(NSDictionary)plist).mutableClone();
								preset.takeValueForKey(files[i], "file");
								if(presets == null)
									presets = new NSMutableArray(preset);
								else
									presets.addObject(preset);
							}
						}
					} catch (Exception e) {
						Logger.getLogger("rujel.reports").log(WOLogLevel.WARNING,
								"Error reading StudentReport preset",
								new Object[] {context.session(), files[i], e});
					}
				}
        	}
        }
        if(defaultSettings == null)
        	defaultSettings = NSDictionary.EmptyDictionary;
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
	
	public void setSettingName(String settingsName) {
		NSArray defaults = (NSArray)session().valueForKeyPath("modules." + settingsName);
		reports = PlistReader.cloneArray(defaults, true);
		NSMutableDictionary settings = (NSMutableDictionary)session().objectForKey(settingsName);
		if(settings == null)
			settings = PlistReader.cloneDictionary(defaultSettings, true);
		else if(Various.boolForObject(session().valueForKeyPath("readAccess.edit.ReporterSetup")))
        	presetName = (String)settings.valueForKey("title");
		settings = synchronizeReportSettings(settings, reports, false, true);
	}
	
	public WOComponent submit() {
//		EOSortOrdering.sortArrayUsingKeyOrderArray(reports, ModulesInitialiser.sorter);
		NSMutableDictionary settings = (NSMutableDictionary)session().objectForKey(SETTINGS);
		settings = synchronizeReportSettings(settings, reports,true,false);
		settings.takeValueForKey(presetName, "title");
		session().setObjectForKey(settings, SETTINGS);
		returnPage.ensureAwakeInContext(context());
		showPresets = false;
		return returnPage;
	}
	
	public WOComponent savePreset() {
		showPresets = true;
		NSMutableDictionary settings = synchronizeReportSettings(null, reports,true,false);
		String defaultName = (String)session().valueForKeyPath(
				"strings.Strings.PrintReport.defaultSettings");
		File file = null;
		boolean cantEdit = !Various.boolForObject(session().valueForKeyPath(
														"readAccess.edit.ReporterSetup"));
		if(presetName == null || presetName.equals(defaultName)) {
	        if(cantEdit) {
	        	synchronizeReportSettings(getDefaultSettings(session()), reports, false, true);
	        	presetName = null;
	        	return null;
	        }
			defaultSettings = settings.immutableClone();
			presetName = defaultName;
			String path = SettingsReader.stringForKeyPath("reportsDir",
			"CONFIGDIR/RujelReports");
			path = path + "/StudentReport/defaultSettings.plist";
			file = new File(Various.convertFilePath(path));
		} else if(presets != null) {
			for(int i = 0; i < presets.count(); i++) {
				NSDictionary pre = (NSDictionary) presets.objectAtIndex(i);
				if(presetName.equals(pre.valueForKey("title"))) {
					if(cantEdit && !Various.boolForObject(pre.valueForKey("isNew"))) {
						settings = PlistReader.cloneDictionary(pre, true);
						synchronizeReportSettings(settings, reports, false, true);
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
			Integer index = (Integer)settings.removeObjectForKey("index");
			String xml = NSPropertyListSerialization.xmlStringFromPropertyList(settings);
			FileOutputStream out = new FileOutputStream(file);
			OutputStreamWriter writer = new OutputStreamWriter(out, "utf8");
			writer.write(xml);
			writer.close();
			out.close();
			if(presetName == defaultName)
				return null;
			settings.takeValueForKey(file, "file");
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
		showPresets = true;
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
		NSMutableDictionary settings = (item == null)?getDefaultSettings(session()):
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
		synchronizeReportSettings(settings, reports, false, true);
		showPresets = true;
		return this;
	}
	
	protected static NSMutableDictionary synchronizeReportSettings(NSMutableDictionary settings,
			 NSArray reports, boolean updSettings, boolean updReports) {
		NSMutableArray keys = null;
		if(settings == null) {
			settings = new NSMutableDictionary();
			updSettings = true;
		} else {
			keys = settings.allKeys().mutableClone();
		}
		Enumeration enu = reports.objectEnumerator();
		while (enu.hasMoreElements()) {
			NSMutableDictionary rp = (NSMutableDictionary) enu.nextElement();
			Object key = rp.valueForKey("id");
			NSMutableDictionary subs = (NSMutableDictionary)settings.objectForKey(key);
			NSMutableArray skeys = null;
			if(subs == null) {
				subs = new NSMutableDictionary();
				settings.setObjectForKey(subs, key);
			} else if(keys !=null) {
				keys.removeObject(key);
				skeys = subs.allKeys().mutableClone();
				skeys.removeObject("active");
				skeys.removeObject("sort");
			}
			if(updSettings || subs.valueForKey("active") == null)
				subs.takeValueForKey(rp.valueForKey("active"),"active");
			else if(updReports)
				rp.takeValueForKey(subs.valueForKey("active"), "active");
			if(updSettings || subs.valueForKey("sort") == null)
				subs.takeValueForKey(rp.valueForKey("sort"),"sort");
			else if(updReports)
				rp.takeValueForKey(subs.valueForKey("sort"), "sort");
			Enumeration options = ((NSArray)rp.valueForKey("options")).objectEnumerator();
			while (options.hasMoreElements()) {
				NSMutableDictionary opt = (NSMutableDictionary) options.nextElement();
				key = opt.valueForKey("id");
				if(updSettings || subs.objectForKey(key) == null)
					subs.takeValueForKey(opt.valueForKey("active"),(String)key);
				else if(updReports)
					opt.setObjectForKey(subs.objectForKey(key),"active");
				if(skeys != null)
					skeys.removeObject(key);
			}
			if(skeys != null && skeys.count() > 0) {
				options = skeys.objectEnumerator();
				while (options.hasMoreElements()) {
					key = options.nextElement();
					subs.removeObjectForKey(key);
				}
			}
		}
		if(keys != null && keys.count() > 0) {
			enu = keys.objectEnumerator();
			while (enu.hasMoreElements()) {
				Object key = enu.nextElement();
				settings.removeObjectForKey(key);
			}
		}
		if(updReports)
			EOSortOrdering.sortArrayUsingKeyOrderArray(
					(NSMutableArray)reports, ModulesInitialiser.sorter);
		return settings;
	}
	
	public static NSMutableDictionary getDefaultSettings(WOSession ses) {
		NSMutableDictionary result = null;
		if(defaultSettings == null) {
			String path = SettingsReader.stringForKeyPath("reportsDir",
					"CONFIGDIR/RujelReports");
			path = path + "/StudentReport/defaultSettings.plist";
			Object plist = PlistReader.readPlist(path, null);
			if(plist instanceof NSDictionary) {
				defaultSettings = (NSDictionary)plist;
				result = PlistReader.cloneDictionary(defaultSettings, true);
				NSArray reports = (NSArray)ses.valueForKeyPath(
						"modules.reportSettingsForStudent");
				result = synchronizeReportSettings(result, reports, false, false);
				defaultSettings = result.immutableClone();
			} else {
				defaultSettings = NSDictionary.EmptyDictionary;
			}
		}
		if(defaultSettings.count() > 0) {
			result = PlistReader.cloneDictionary(defaultSettings, true);
		} else {
			NSArray reports = (NSArray)ses.valueForKeyPath("modules.reportSettingsForStudent");
			result = synchronizeReportSettings(result, reports, true, false);
		}
		return result;
	}
	
	public static NSArray prepareReports(
			WOSession ses, NSMutableDictionary reportSettings) {
		NSMutableDictionary settings = (NSMutableDictionary)ses.objectForKey(SETTINGS);
		if(settings == null) {
			settings = getDefaultSettings(ses);
			settings.takeValueForKey(ses.valueForKeyPath(
			"strings.Strings.PrintReport.defaultSettings"), "title");
			ses.setObjectForKey(settings,SETTINGS);
		}
		reportSettings.takeValueForKey(settings, "settings");
		ses.setObjectForKey(reportSettings,"reportForStudent");
		NSArray reports = (NSArray)ses.valueForKeyPath("modules.reportForStudent");
		ses.removeObjectForKey("reportForStudent");
		return reports;
	}
	
	public String presetsStyle() {
		if(showPresets)
			return "border-left:1px #666666 solid;";
		return "display:none;border-left:1px #666666 solid;";
	}
	
	public String savePresetOnClick() {
		String result = "enumerate(form,'sorting',1);ajaxPost(this);";
		if(!Various.boolForObject(session().valueForKeyPath("readAccess.edit.ReporterSetup"))) {
			return "f=form.presetName;if(f.value.length){" +
			result + "}else{disabled=true;f.focus();}return false;";
		}
		return result + "return false;";
	}
}