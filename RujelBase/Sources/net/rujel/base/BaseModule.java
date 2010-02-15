// BaseModule.java

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

package net.rujel.base;

import net.rujel.interfaces.EduCourse;
import net.rujel.reusables.PlistReader;

import com.webobjects.appserver.WOApplication;
import com.webobjects.appserver.WOContext;
import com.webobjects.foundation.NSDictionary;

public class BaseModule {
	protected static NSDictionary lessonsTab;

	public static Object init(Object obj, WOContext ctx) {
		if(obj == null || obj.equals("init")) {
			init();
		} else if("presentTabs".equals(obj)) {
			if(lessonsTab == null) lessonsTab = (NSDictionary)WOApplication.application().
			valueForKeyPath("strings.RujelBase_Base.lessonsTab");
			if(lessonsTab == null) lessonsTab = NSDictionary.EmptyDictionary;
			return lessonsTab.mutableClone();
		} else if("reportForStudent".equals(obj)) {
			NSDictionary settings = (NSDictionary)ctx.session().objectForKey("reportForStudent");
			return LessonReport.reportForStudent(settings);
		} else if("reportSettingsForStudent".equals(obj)) {
			NSDictionary reportSettings = (NSDictionary)WOApplication.application().
					valueForKeyPath("strings.RujelBase_Base.reportSettings");
			return PlistReader.cloneDictionary(reportSettings, true);
		} else if("deleteCourse".equals(obj)) {
			return deleteCourse(ctx);
		} else if ("diary".equals(obj)) {
			NSDictionary diaryTabs = (NSDictionary)WOApplication.application().
					valueForKeyPath("strings.RujelBase_Base.diaryTab");
			return PlistReader.cloneDictionary(diaryTabs, true);
		}
		return null;
	}

	public static void init() {
		;
	}
	
	public static Object deleteCourse(WOContext ctx) {
		EduCourse course = (EduCourse)ctx.session().objectForKey("deleteCourse");
		if(course.lessons() == null || course.lessons().count() == 0)
			return null;
		String message = (String)WOApplication.application().valueForKeyPath(
				"strings.RujelBase_Base.courseHasLessons");
		ctx.session().takeValueForKey(message, "message");
		return message;
	}
}
