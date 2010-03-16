package net.rujel.ui;

import java.util.Enumeration;

import net.rujel.reusables.ModulesInitialiser;
import net.rujel.reusables.PlistReader;
import net.rujel.reusables.Various;

import com.webobjects.appserver.WOContext;
import com.webobjects.appserver.WOComponent;
import com.webobjects.appserver.WOSession;
import com.webobjects.eocontrol.EOSortOrdering;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSKeyValueCoding;
import com.webobjects.foundation.NSMutableArray;
import com.webobjects.foundation.NSMutableDictionary;

public class ReporterSetup extends WOComponent {
	public static final String SETTINGS = "reportSettingsForStudent";
	
    public WOComponent returnPage;
    public NSMutableArray reports;
	public NSKeyValueCoding item;
	public NSKeyValueCoding subItem;

    public ReporterSetup(WOContext context) {
        super(context);
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
		settings = synchronizeReportSettings(settings, reports, false, true);
	}
	
	public WOComponent submit() {
//		EOSortOrdering.sortArrayUsingKeyOrderArray(reports, ModulesInitialiser.sorter);
		NSMutableDictionary settings = (NSMutableDictionary)session().objectForKey(SETTINGS);
		settings = synchronizeReportSettings(settings, reports,true,false);
		session().setObjectForKey(settings, SETTINGS);
		returnPage.ensureAwakeInContext(context());
		return returnPage;
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
				if(updSettings || subs.objectForKey("id") == null)
					subs.setObjectForKey(opt.valueForKey("active"),key);
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
	
	public static NSArray prepareReports(
			WOSession ses, NSMutableDictionary reportSettings) {
		NSMutableDictionary settings = (NSMutableDictionary)ses.objectForKey(SETTINGS);
		if(settings == null) {
			NSArray reports = (NSArray)ses.valueForKeyPath("modules.reportSettingsForStudent");
			settings = synchronizeReportSettings(settings, reports, true, false);
			ses.setObjectForKey(settings,SETTINGS);
		}
		reportSettings.takeValueForKey(settings, "settings");
		ses.setObjectForKey(reportSettings,"reportForStudent");
		NSArray reports = (NSArray)ses.valueForKeyPath("modules.reportForStudent");
		ses.removeObjectForKey("reportForStudent");
		return reports;
	}
}