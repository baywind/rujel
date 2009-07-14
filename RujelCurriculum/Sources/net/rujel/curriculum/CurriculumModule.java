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
import java.util.TimerTask;
import java.util.logging.Logger;

import net.rujel.base.MyUtility;
import net.rujel.interfaces.EduLesson;
import net.rujel.interfaces.Person;
import net.rujel.reusables.*;

import com.webobjects.appserver.WOApplication;
import com.webobjects.appserver.WOContext;
import com.webobjects.eoaccess.EOUtilities;
import com.webobjects.eocontrol.EOEditingContext;
import com.webobjects.eocontrol.EOKeyValueQualifier;
import com.webobjects.eocontrol.EOQualifier;
import com.webobjects.foundation.*;

public class CurriculumModule {
		
	public static Object init(Object obj, WOContext ctx) {
		if(obj == null || obj.equals("init")) {
			Substitute.init();
			Reason.init();
			Variation.init();
			Reprimand.init();
		} else if ("extendLesson".equals(obj)) {
			return extendLesson(ctx);
		} else if ("lessonProperies".equals(obj)) {
			return lessonProperies(ctx);
		} else if ("journalPlugins".equals(obj)) {
			return journalPlugins(ctx);
		} else if("scheduleTask".equals(obj)) {
			return scheduleTask(ctx);
		} else if(obj.equals("regimes")) {
			return WOApplication.application().valueForKeyPath(
					"strings.RujelCurriculum_Curriculum.regimes");
		} else if(obj.equals("courseComplete")) {
			return WOApplication.application().valueForKeyPath(
					"strings.RujelCurriculum_Curriculum.courseComplete");
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
		boolean showSubs = Various.boolForObject("readAccess.read.Substitute");
		boolean showVars = Various.boolForObject("readAccess.read.Variation");
		if(!showSubs && !showVars)
			return null;
		NSArray lessonsList = (NSArray)ctx.session().objectForKey("lessonsList");
		if(lessonsList == null || lessonsList.count() == 0)
			return null;
		EOEditingContext ec = null;
		NSMutableDictionary result = new NSMutableDictionary();
		NSArray vars = null;
		if(showVars) {
			EduLesson lesson = (EduLesson)lessonsList.objectAtIndex(0);
			NSArray args = new NSArray(lesson.course());
			vars = EOUtilities.objectsWithQualifierFormat(lesson.editingContext(),
					Variation.ENTITY_NAME, "course = %@ AND value >= 1", args);
			showVars = (vars != null && vars.count() > 0);
		}
		Enumeration enu = lessonsList.objectEnumerator();
		while (enu.hasMoreElements()) {
			EduLesson lesson = (EduLesson) enu.nextElement();
			NSMutableDictionary props = null;
			if(showVars) {
				EOQualifier qual = new EOKeyValueQualifier(Variation.DATE_KEY,
						EOQualifier.QualifierOperatorEqual,lesson.date());
				NSArray filtered = EOQualifier.filteredArrayWithQualifier(vars, qual);
				if(filtered != null && filtered.count() > 0) {
					props = new NSMutableDictionary("color:#006600;","style");
					props.setObjectForKey(WOApplication.application().valueForKeyPath(
							"strings.RujelCurriculum_Curriculum.Variation.plus"),"title");
					result.setObjectForKey(props, lesson);
				}
			}
			if(!showSubs)
				continue;
			NSArray subs = (NSArray)lesson.valueForKey("substitutes");
			if(subs == null || subs.count() == 0)
				continue;
			if(props == null)
				props = new NSMutableDictionary("highlight2","class");
			else
				props.takeValueForKey("highlight2","class");
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
			sTitle = (String)props.valueForKey("title");
			if(sTitle != null) {
				title.append(" -+- ").append(sTitle);
			}
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
	
	public static Object journalPlugins(WOContext ctx) {
		Object result = null;
		if(Various.boolForObject(ctx.session().valueForKeyPath("readAccess.read.Variation")))
				result = WOApplication.application().valueForKeyPath(
						"strings.RujelCurriculum_Curriculum.varsPlugin");
		if(Various.boolForObject(ctx.session().valueForKeyPath("readAccess.read.Reprimand"))) {
			NSDictionary rp = (NSDictionary)WOApplication.application().valueForKeyPath(
				"strings.RujelCurriculum_Curriculum.reprPlugin");
			if(result==null)
				result = rp;
			else
				result = new NSArray(new Object[] {result,rp});
		}
		return result;
	}
	
	public static Object scheduleTask(WOContext ctx) {
		boolean disable = Boolean.getBoolean("PlanFactCheck.disable")
				|| SettingsReader.boolForKeyPath("edu.disablePlanFactCheck", false);
		if(disable)
			return null;
		String checkTime = SettingsReader.stringForKeyPath("edu.planFactCheckTime", "0:59");
		TimerTask task = new TimerTask() {
			public void run() {
				Reprimand.planFactCheck();
			}
		};
		MyUtility.scheduleTaskOnTime(task,checkTime);
		return null;
	}
}