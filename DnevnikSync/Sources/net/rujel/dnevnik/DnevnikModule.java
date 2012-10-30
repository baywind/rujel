package net.rujel.dnevnik;

import java.util.TimerTask;

import net.rujel.base.MyUtility;
import net.rujel.reusables.SettingsReader;
import net.rujel.reusables.WOLogLevel;

import com.webobjects.appserver.WOApplication;
import com.webobjects.appserver.WOContext;
import com.webobjects.foundation.NSKeyValueCoding;

public class DnevnikModule {
	public static Object init(Object obj, WOContext ctx) {
		if(obj == null || obj.equals("init")) {
			try {
//				Object access = PlistReader.readPlist("access.plist", "DnevnikSync", null);
//				WOApplication.application().takeValueForKey(access, "defaultAccess");
			} catch (NSKeyValueCoding.UnknownKeyException e) {
				// default access not supported
			}
//			init();
		} else if(obj.equals("regimes")) {
			return WOApplication.application().valueForKeyPath(
					"strings.DnevnikSync_Dnevnik.dnevnikRegime");
		} else if(obj.equals("scheduleTask")) {
			String time = SettingsReader.stringForKeyPath("dnevnik.syncTime", null);
			if(time == null)
				return null;
			TimerTask task = new TimerTask() {
				public void run() {
					try {
						Sychroniser.syncToMoment(null);
					} catch (Exception e) {
						Sychroniser.logger.log(WOLogLevel.WARNING,
								"Scheduled Dnevnik synchronisation failed", e);
					}
				}
			};
			if(MyUtility.scheduleTaskOnTime(task, time))
				Sychroniser.logger.log(WOLogLevel.FINE,"Dnevnik sync scheduled on " + time);
		}
		return null;
	}
}
