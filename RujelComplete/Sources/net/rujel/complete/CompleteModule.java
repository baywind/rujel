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


import java.lang.reflect.Method;
import java.util.Enumeration;

import net.rujel.base.SettingsBase;
import net.rujel.interfaces.EduCourse;
import net.rujel.reusables.PlistReader;
import net.rujel.reusables.SettingsReader;
import net.rujel.reusables.Various;

import com.webobjects.appserver.WOApplication;
import com.webobjects.appserver.WOContext;
import com.webobjects.eocontrol.EOEditingContext;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSDictionary;
import com.webobjects.foundation.NSKeyValueCoding;
import com.webobjects.foundation.NSKeyValueCodingAdditions;
import com.webobjects.foundation.NSMutableArray;
import com.webobjects.foundation.NSMutableDictionary;
import com.webobjects.foundation.NSMutableSet;
import com.webobjects.foundation.NSSet;

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
			PedDecision.init();
		} else if("journalPlugins".equals(obj)) {
			return journalPlugins(ctx);
		} else if("notesAddOns".equals(obj)) {
			return notesAddOns(ctx);
		} else if("coursesReport".equals(obj)) {
			return coursesReport(ctx);
//		} else if("groupReport".equals(obj)) {
//			return groupReport(ctx);
		} else if("adminModules".equals(obj)) {
			return ctx.session().valueForKeyPath("strings.RujelComplete_Complete.adminModule");
		} else if("accessModifier".equals(obj)) {
			return new ClosingLock(ctx.session());
		}
		return null;
	}
	
	public static Object journalPlugins(WOContext ctx) {
		if(Various.boolForObject(ctx.session().valueForKeyPath("readAccess._read.Completion")))
			return null;
		EduCourse course = (EduCourse)ctx.session().objectForKey("editorCourse");
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

    public static NSMutableDictionary moduleDependencies(NSArray modules) {
    	NSArray ids = (NSArray)modules.valueForKey("id"); 
    	if(!ids.contains("student")) {
    		ids = ids.arrayByAddingObject("student");
    		modules = modules.arrayByAddingObject(new NSDictionary(Boolean.TRUE,"manual"));
    	}
    	NSSet[] result = new NSSet[modules.count()]; 
    	boolean[] flag = new boolean[modules.count()];
    	for (int i = 0; i < flag.length; i++) {
			processModule(i, modules, ids, result, flag);
			
		}
    	return new NSMutableDictionary(result,ids.objects());
    }
    
    private static NSSet processModule(int idx, NSArray modules,NSArray ids,
    		NSSet[] result,boolean[] flag) {
		if(flag[idx])
			return result[idx];
		NSKeyValueCoding mod = (NSKeyValueCoding)modules.objectAtIndex(idx);
		NSArray requires = (NSArray)mod.valueForKey("requires");
		if(result[idx] == null)
			result[idx] = new NSMutableSet(requires);
		NSMutableSet req = (NSMutableSet)result[idx];
		if(requires != null && requires.count() > 0) {
			Enumeration enu = requires.objectEnumerator();
			while (enu.hasMoreElements()) {
				String aspect = (String) enu.nextElement();
				int ri = ids.indexOfIdenticalObject(aspect);
				if(ri < 0)
					continue;
				req.addObject(aspect);
				req.unionSet(processModule(ri, modules, ids, result, flag));
			}
		}
		requires = (NSArray)mod.valueForKey("precedes");
		if(requires != null && requires.count() > 0) {
			Enumeration enu = requires.objectEnumerator();
			boolean manual = Various.boolForObject(mod.valueForKey("manual"));
			while (enu.hasMoreElements()) {
				String aspect = (String) enu.nextElement();
				int ri = ids.indexOfIdenticalObject(aspect);
				if(ri < 0)
					continue;
				if(result[ri] == null)
					result[ri] = new NSMutableSet(req.count() +1);
				NSMutableSet pre = (NSMutableSet)result[ri];
				pre.addObject(ids.objectAtIndex(idx));
				if(!manual)
					pre.unionSet(req);
			}
		}
		flag[idx] = true;
		return req;
    }
    
	public static Object groupReport(WOContext ctx) {
		NSKeyValueCodingAdditions readAccess = (NSKeyValueCodingAdditions)ctx.
							session().valueForKey("readAccess");
		if(Various.boolForObject(readAccess.valueForKeyPath("_read.PedDecision")))
			return null;
		NSMutableDictionary result = new NSMutableDictionary("decision","id");
		result.takeValueForKey(ctx.session().valueForKeyPath(
			"strings.RujelComplete_Complete.pedsovet"), "title");
		result.takeValueForKey("80","sort");
		result.takeValueForKey(PedDecision.ENTITY_NAME, "entity");
		result.takeValueForKey("$preloaded", "value");
		return result;
	}
	
	public static Object coursesReport(WOContext ctx) {
		NSKeyValueCodingAdditions readAccess = (NSKeyValueCodingAdditions)ctx.
							session().valueForKey("readAccess");
		if(Various.boolForObject(readAccess.valueForKeyPath("_read.Completion")))
			return null;
		NSMutableDictionary result = new NSMutableDictionary("completion","id");
		result.takeValueForKey(ctx.session().valueForKeyPath(
				"strings.RujelComplete_Complete.adminModule.title"), "title");
		result.takeValueForKey("80","sort");
		result.takeValueForKey(Completion.ENTITY_NAME, "entity");
		NSArray list = (NSArray)ctx.session().valueForKeyPath("modules.courseComplete");
		Enumeration enu = list.objectEnumerator();
		NSMutableArray modules = new NSMutableArray();
		NSMutableDictionary dict = new NSMutableDictionary("integral","id");
		dict.takeValueForKey(".integral", "value");
		dict.takeValueForKey(ctx.session().valueForKeyPath(
			"strings.Reusables_Strings.dataTypes.total"), "title");
		dict.takeValueForKey("?", "short");
		dict.takeValueForKey(Boolean.TRUE,"active");
		dict.takeValueForKey(".integralClass", "class");
		dict.takeValueForKey("font-weight:bold;width:1em;", "style");
		dict.takeValueForKey(new NSDictionary(Boolean.FALSE,"escapeHTML"), "presenterBindings");
		NSMutableArray subs = new NSMutableArray(dict);
		NSDictionary dateFormat = new NSDictionary(SettingsReader.stringForKeyPath(
				"ui.shortDateFormat", "dd.MM"),"dateformat");
		while (enu.hasMoreElements()) {
			NSKeyValueCoding mod = (NSKeyValueCoding) enu.nextElement();
			if(!Various.boolForObject(mod.valueForKey("manual")))
				continue;
			String id = (String)mod.valueForKey("id");
			modules.addObject(id);
			dict = new NSMutableDictionary(id,"id");
			StringBuilder buf = new StringBuilder(16);
			buf.append('.').append(id);
			dict.takeValueForKey(buf.toString(), "value");
			dict.takeValueForKey(mod.valueForKey("title"), "title");
			dict.takeValueForKey(mod.valueForKey("sort"), "sort");
			buf.append("Class");
			dict.takeValueForKey(buf.toString(), "class");
			dict.takeValueForKey(dateFormat, "presenterBindings");
			subs.addObject(dict);
		}
		dict = new NSMutableDictionary("student","id");
		dict.takeValueForKey(ctx.session().valueForKeyPath(
				"strings.RujelComplete_Complete.StudentCatalog"), "title");
		dict.takeValueForKey(".presentStudent", "value");
		dict.takeValueForKey(".studentClass", "class");
		dict.takeValueForKey(Boolean.TRUE,"active");
		dict.takeValueForKey("90", "sort");
//		dict.takeValueForKey(escape, "presenterBindings");
		subs.addObject(dict);
		result.takeValueForKey(subs, "subParams");
		try {
			dict = new NSMutableDictionary("courseCompletion","methodName");
			SettingsBase settings = null;
			try {
				EOEditingContext ec = null;
				ec = (EOEditingContext)ctx.page().valueForKey("ec");
				settings = SettingsBase.baseForKey(Completion.SETTINGS_BASE, ec, false);
			} catch (Exception ex) {
			}
			Object[] args = new Object[] {".", modules, settings};
			Method method = Completion.class.getMethod("courseCompletion", 
					EduCourse.class, NSArray.class, SettingsBase.class);
			dict.takeValueForKey(method, "parsedMethod");
			dict.takeValueForKey(new NSArray(args), "paramValues");
			result.takeValueForKey(dict, "itemValue");
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		return result;
	}
}
