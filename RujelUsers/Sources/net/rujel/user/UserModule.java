// UserModule.java

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

package net.rujel.user;

import java.util.Enumeration;
import java.util.logging.Logger;

import net.rujel.reusables.PlistReader;
import net.rujel.reusables.SettingsReader;
import net.rujel.reusables.WOLogLevel;

import com.webobjects.appserver.WOApplication;
import com.webobjects.appserver.WOContext;
import com.webobjects.eoaccess.EOUtilities;
import com.webobjects.eocontrol.EOEditingContext;
import com.webobjects.eocontrol.EOEnterpriseObject;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSKeyValueCoding;
import com.webobjects.foundation.NSMutableArray;
import com.webobjects.foundation.NSDictionary;

public class UserModule {
	
	public static final String[] presetGroups = new String[] {
		"root","zavuch","zav_kaf","tutor","teacher"};
 	
	public static Object init(Object obj, WOContext ctx) {
		if(obj == null || obj.equals("init")) {
			try {
				Object access = PlistReader.readPlist("access.plist", "RujelUsers", null);
				WOApplication.application().takeValueForKey(access, "defaultAccess");
			} catch (NSKeyValueCoding.UnknownKeyException e) {
				// default access not supported
			}
			generateGroups();
		} else if("adminModules".equals(obj)) {
			return ctx.session().valueForKeyPath("strings.RujelUsers_UserStrings.adminModule");
		} else if("accessModifier".equals(obj)) {
			return null;
		}
		return null;
	}
	
	public static void generateGroups() {
//		if(mappings == null)
//			return;
		EOEditingContext ec = new EOEditingContext();
		ec.lock();
		try {
			NSMutableArray preset = new NSMutableArray(presetGroups);
			NSArray exists = EOUtilities.objectsForEntityNamed(ec, "UserGroup");
			NSDictionary existing = NSDictionary.EmptyDictionary;
			if(exists != null && exists.count() > 0) {
				if(SettingsReader.stringForKeyPath("auth.parentLoginHandler", null) == null)
					return;
				existing = new NSDictionary(exists,(NSArray)exists.valueForKey("groupName"));
				preset.removeObjectsInArray(existing.allKeys());
			}
			SettingsReader mappings = SettingsReader.settingsForPath("auth.groupMapping", false);
			if(mappings != null) {
				Enumeration enu = mappings.keyEnumerator();
				int count = 0;
				while (enu.hasMoreElements()) {
					String key = (String) enu.nextElement();
					String value = mappings.get(key, null);
					if(value == null || value.equals("*"))
						continue;
					EOEnterpriseObject gr = (EOEnterpriseObject)existing.valueForKey(key);
					if(gr != null) {
						if(gr.valueForKey("externalEquivalent") == null)
							gr.takeValueForKey(value, "externalEquivalent");
						continue;
					}
					count++;
					gr = EOUtilities.createAndInsertInstance(ec, "UserGroup");
					gr.takeValueForKey(key, "groupName");
					gr.takeValueForKey(value, "externalEquivalent");
					preset.removeObject(key);
				}
				if(count > 0) {
					Logger logger = Logger.getLogger("rujel.users");
					ec.saveChanges();
					logger.log(WOLogLevel.COREDATA_EDITING,"Generated UserGroups from mappings");
				}
			}
			if(preset.count() > 0 && (exists == null || exists.count() == 0)) {
				Enumeration enu = preset.objectEnumerator();
				while (enu.hasMoreElements()) {
					String key = (String) enu.nextElement();
					EOEnterpriseObject gr = EOUtilities.createAndInsertInstance(ec, "UserGroup");
					gr.takeValueForKey(key, "groupName");
				}
				Logger logger = Logger.getLogger("rujel.users");
				ec.saveChanges();
				logger.log(WOLogLevel.COREDATA_EDITING,"Generated UserGroups from preset");
			}
		} catch (Exception e) {
			Logger logger = Logger.getLogger("rujel.users");
			logger.log(WOLogLevel.WARNING,"Error generating UserGroups from mappings/preset",e);
		} finally {
			ec.unlock();
		}
	}

}
