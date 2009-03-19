// CurriculumModule.java

/*
 * Copyright (c) 2008, Gennady & Michael Kushnir
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 * 
 * 	�	Redistributions of source code must retain the above copyright notice, this
 * 		list of conditions and the following disclaimer.
 * 	�	Redistributions in binary form must reproduce the above copyright notice,
 * 		this list of conditions and the following disclaimer in the documentation
 * 		and/or other materials provided with the distribution.
 * 	�	Neither the name of the RUJEL nor the names of its contributors may be used
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

package net.rujel.curriculum;

import java.util.Enumeration;
import java.util.logging.Logger;

import net.rujel.interfaces.EduLesson;
import net.rujel.interfaces.Person;
import net.rujel.reusables.*;

import com.webobjects.appserver.WOApplication;
import com.webobjects.appserver.WOContext;
import com.webobjects.eoaccess.EOUtilities;
import com.webobjects.eocontrol.EOEditingContext;
import com.webobjects.foundation.*;

public class CurriculumModule {
	
	public static NSArray journalPlugins = (NSArray)WOApplication.application().valueForKeyPath(
			"strings.RujelCurriculum_Curriculum.plugins");

	
	public static Object init(Object obj, WOContext ctx) {
		if(obj == null || obj.equals("init")) {
			Substitute.init();
			Reason.init();
			Variation.init();
		} else if ("extendLesson".equals(obj)) {
			return extendLesson(ctx);
		} else if ("lessonProperies".equals(obj)) {
			return lessonProperies(ctx);
		} else if ("journalPlugins".equals(obj)) {
			return journalPlugins(ctx);
		} else if(obj.equals("regimes")) {
			if(Various.boolForObject(ctx.session().valueForKeyPath("readAccess._read.Curriculum")))
				return null;
			return WOApplication.application().valueForKeyPath(
					"strings.RujelCurriculum_Curriculum.curriculumRegime");
		}
		return null;
	}

	public static NSKeyValueCoding extendLesson(WOContext ctx) {
		//EduLesson lesson = (EduLesson)ctx.session().objectForKey("currentLesson");
		//Substitute sub = Substitute.substituteForLesson(lesson);
		if(Various.boolForObject(ctx.session().valueForKeyPath("readAccess._read.Substitute")))
			return null;
		NSMutableDictionary result = new NSMutableDictionary("ShowSubstitute","component");
		//result.takeValueForKey(sub,"substitute");
		result.takeValueForKey("20","sort");
		return result;
	}
	
	public static NSDictionary lessonProperies(WOContext ctx) {
		if(!Various.boolForObject("readAccess.read.Substitute"))
			return null;
		NSArray lessonsList = (NSArray)ctx.session().objectForKey("lessonsList");
		if(lessonsList == null || lessonsList.count() == 0)
			return null;
		EOEditingContext ec = null;
		NSMutableDictionary result = new NSMutableDictionary();
		Enumeration enu = lessonsList.objectEnumerator();
		while (enu.hasMoreElements()) {
			EduLesson lesson = (EduLesson) enu.nextElement();
			NSArray subs = (NSArray)lesson.valueForKey("substitutes");
			if(subs == null || subs.count() == 0)
				continue;
			NSMutableDictionary props = new NSMutableDictionary("highlight2","class");
			Enumeration senu = subs.objectEnumerator();
			StringBuffer title = new StringBuffer();
			String sTitle = null;
			while(senu.hasMoreElements()) {
				Substitute sub = (Substitute)senu.nextElement();
				if(!sub.title().equals(sTitle)) {
					if(sTitle != null)
						title.append(';').append(' ');
					sTitle = sub.title();
						title.append(sTitle).append(" : ");
				} else {
					title.append(',').append(' ');
				}
				title.append(Person.Utility.fullName(sub.teacher(), true, 2, 1, 1));
				if(lesson.date() != null && !lesson.date().equals(sub.date())) {
					if(ec == null)
						ec = new EOEditingContext();
					sub = (Substitute)EOUtilities.localInstanceOfObject(ec, sub);
					sub.setDate(lesson.date());
					Logger.getLogger("rujel.curriculum").log(WOLogLevel.OWNED_EDITING,
							"Correcting substitute date", new Object[] {ctx.session(),sub});
				}
			} // Enumeration senu = subs.objectEnumerator();
			props.setObjectForKey(title.toString(),"title");
			result.setObjectForKey(props, lesson);
		}
		if(result.count() == 0)
			return null;
		if(ec != null) {
			try {
				ec.saveChanges();
			} catch (Exception e) {
				Logger.getLogger("rujel.curriculum").log(WOLogLevel.WARNING,
						"Error saving substitute corrections",  new Object[] {ctx.session(),e});
			}
		}
		return result;
	}
	
	public static NSArray journalPlugins(WOContext ctx) {
		if(Various.boolForObject(ctx.session().valueForKeyPath("readAccess._read.Variation")))
			return null;
		return journalPlugins;
	}
}