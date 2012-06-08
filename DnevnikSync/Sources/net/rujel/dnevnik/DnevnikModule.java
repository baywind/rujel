package net.rujel.dnevnik;


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
		}
		return null;
	}

}
