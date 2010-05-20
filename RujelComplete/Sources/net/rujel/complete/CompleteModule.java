// CompleteModule.java

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

package net.rujel.complete;


import net.rujel.base.SettingsBase;
import net.rujel.interfaces.EduCourse;
import net.rujel.reusables.PlistReader;
import net.rujel.reusables.Various;

import com.webobjects.appserver.WOApplication;
import com.webobjects.appserver.WOContext;
import com.webobjects.foundation.NSKeyValueCoding;

public class CompleteModule {
	
	public static Object init(Object obj, WOContext ctx) {
		if(obj == null || obj.equals("init")) {
			try {
				Object access = PlistReader.readPlist("access.plist", "RujelComplete", null);
				WOApplication.application().takeValueForKey(access, "defaultAccess");
			} catch (NSKeyValueCoding.UnknownKeyException e) {
				// default access not supported
			}
			Completion.init();
		} else if("journalPlugins".equals(obj)) {
			return journalPlugins(ctx);
		} else if("notesAddOns".equals(obj)) {
			return notesAddOns(ctx);
		} else if("courseComplete".equals(obj)) {
			return ctx.session().valueForKeyPath("strings.RujelComplete_Complete.courseComplete");
		} else if("adminModules".equals(obj)) {
			return ctx.session().valueForKeyPath("strings.RujelComplete_Complete.adminModule");
		}
		return null;
	}
	
	public static Object journalPlugins(WOContext ctx) {
		if(Various.boolForObject(ctx.session().valueForKeyPath("readAccess._read.Completion")))
			return null;
		EduCourse course = (EduCourse)ctx.page().valueForKey("course");
		if(course == null)
			return null;
		String active = SettingsBase.stringSettingForCourse(
				Completion.SETTINGS_BASE, course, course.editingContext());
		if(Boolean.parseBoolean(active))
			return ctx.session().valueForKeyPath("strings.RujelComplete_Complete.dashboard");
		return null;
	}

	public static NSKeyValueCoding notesAddOns(WOContext ctx) {
		if(Various.boolForObject(ctx.session().valueForKeyPath("readAccess._read.Completion")))
			return null;
		return new CptAddOn(ctx.session());
	}
}
