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
import net.rujel.base.SettingsBase;
import net.rujel.interfaces.EduCourse;
import net.rujel.interfaces.EduLesson;
import net.rujel.interfaces.Person;
import net.rujel.reusables.*;

import com.webobjects.appserver.WOApplication;
import com.webobjects.appserver.WOContext;
import com.webobjects.eoaccess.EOUtilities;
import com.webobjects.eocontrol.EOEditingContext;
import com.webobjects.eocontrol.EOFetchSpecification;
import com.webobjects.eocontrol.EOKeyValueQualifier;
import com.webobjects.eocontrol.EOQualifier;
import com.webobjects.foundation.*;

public class CurriculumModule {
		
	public static boolean isAvailable(NSArray active) {
		boolean res = (active.containsObject("net.rujel.base.BaseModule") &&
				(active.containsObject("net.rujel.eduplan.EduPlan") ||
					active.containsObject("net.rujel.eduplan.PlanCycle")));
		if(!res)
			Logger.getLogger("rujel.curriculum").log(WOLogLevel.INFO,
					"Curriculum module requires EduPlan and Base modules");
		return res;
	}
	
	public static Object init(Object obj, WOContext ctx) {
		if(obj == null || obj.equals("init")) {
			try {
				Object access = PlistReader.readPlist("access.plist",
						"RujelCurriculum", null);
				WOApplication.application().takeValueForKey(access, "defaultAccess");
			} catch (NSKeyValueCoding.UnknownKeyException e) {
				// default access not supported
			}
			Substitute.init();
			Reason.init();
			Variation.init();
			Reprimand.init();
		} else if ("extendLesson".equals(obj)) {
			return extendLesson(ctx);
		} else if ("lessonProperties".equals(obj)) {
			return lessonProperties(ctx);
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
		} else if("deleteCourse".equals(obj)) {
			return deleteCourse(ctx);
		} else if("deleteLesson".equals(obj)) {
			return deleteLesson(ctx);
		} else if("assumeNextLesson".equals(obj)) {
			return assumeNextLesson(ctx);
		} else if("objectSaved".equals(obj)) {
			return objectSaved(ctx);
		} else if("accessModifier".equals(obj)) {
			return accessModifier(ctx);
		} else if("archiveType".equals(obj)) {
			return ctx.session().valueForKeyPath("strings.RujelCurriculum_Curriculum.archiveType");
		} else if("adminModules".equals(obj)) {
			return ctx.session().valueForKeyPath("strings.RujelCurriculum_Curriculum.adminModules");
		} else if("usedModels".equals(obj)) {
			return "Curriculum";
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
	
	public static NSDictionary lessonProperties(WOContext ctx) {
		boolean showSubs = Various.boolForObject("readAccess.read.Substitute");
		boolean showVars = Various.boolForObject("readAccess.read.Variation");
		if(!showSubs && !showVars)
			return null;
		NSArray lessonsList = (NSArray)ctx.session().objectForKey("lessonsList");
		if(lessonsList == null || lessonsList.count() == 0)
			return null;
		NSMutableDictionary result = new NSMutableDictionary();
		NSArray vars = null;
		if(showVars) {
			EduLesson lesson = (EduLesson)lessonsList.objectAtIndex(0);
			NSArray args = new NSArray(lesson.course());
			vars = EOUtilities.objectsWithQualifierFormat(lesson.editingContext(),
					Variation.ENTITY_NAME, "course = %@ AND value >= 1 ", args);
			showVars = (vars != null && vars.count() > 0);
		}
		if(showSubs) {
			EOQualifier qual = Various.getEOInQualifier("lesson", lessonsList);
			EOFetchSpecification fs = new EOFetchSpecification(Substitute.ENTITY_NAME
					,qual,null);
			EduLesson lesson = (EduLesson)lessonsList.objectAtIndex(0);
			NSArray subs = lesson.editingContext().objectsWithFetchSpecification(fs);
			showSubs = (subs != null && subs.count() > 0);
		}
		Enumeration enu = lessonsList.objectEnumerator();
		while (enu.hasMoreElements()) {
			EduLesson lesson = (EduLesson) enu.nextElement();
			NSMutableDictionary props = null;
			if(showVars) {
				EOQualifier qual = new EOKeyValueQualifier("relatedLesson",
						EOQualifier.QualifierOperatorEqual,lesson);
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
			String sTitle = Substitute.subsTitleForLesson(lesson);
			if(sTitle == null)
				continue;
			if(props == null)
				props = new NSMutableDictionary("highlight2","class");
			else
				props.takeValueForKey("highlight2","class");
			String title = (String)props.valueForKey("title");
			if(title != null)
				sTitle = sTitle + " -+- " + title;
			props.setObjectForKey(sTitle,"title");
			result.setObjectForKey(props, lesson);
		}
		if(result.count() == 0)
			return null;
		return result;
	}
	
	public static Object journalPlugins(WOContext ctx) {
		Object result = null;
		if(Various.boolForObject(ctx.session().valueForKeyPath("readAccess.read.Variation"))) {
			EduCourse course = (EduCourse)ctx.session().objectForKey("editorCourse");
			String state = (course == null) ? null : SettingsBase.stringSettingForCourse(
					"PlanFactWidget", course, course.editingContext());
			result = ("hide".equals(state))?null:WOApplication.application().valueForKeyPath(
				"strings.RujelCurriculum_Curriculum.varsPlugin");
		}
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
		if(MyUtility.scheduleTaskOnTime(task,checkTime))
			Curriculum.logger.log(WOLogLevel.FINE,"PlanFactCheck scheduled on " + checkTime);
		return null;
	}

	public static Object deleteCourse(WOContext ctx) {
		EduCourse course = (EduCourse)ctx.session().objectForKey("deleteCourse");
		EOEditingContext ec = course.editingContext();
		NSArray list = EOUtilities.objectsMatchingKeyAndValue
					(ec, Variation.ENTITY_NAME, "course", course);
		if(list == null || list.count() == 0)
			return null;
		String message = (String)WOApplication.application().valueForKeyPath(
				"strings.RujelCurriculum_Curriculum.messages.courseHasVariations");
		ctx.session().takeValueForKey(message, "message");
		return message;
	}
	
	public static Object deleteLesson(WOContext ctx) {
		EduLesson lesson = (EduLesson)ctx.userInfoForKey("deleteLesson");
		if(lesson == null || !EduLesson.entityName.equals(lesson.entityName()))
			return null;
		EOEditingContext ec = lesson.editingContext();
		NSArray vars = EOUtilities.objectsMatchingKeyAndValue(ec, 
				Variation.ENTITY_NAME, "relatedLesson", lesson);
		if(vars != null && vars.count() > 0) {
			Enumeration enu = vars.objectEnumerator();
			while (enu.hasMoreElements()) {
				Variation var = (Variation) enu.nextElement();
//				if(var.value().intValue() > 0)
					ec.deleteObject(var);
//				else
//					var.setRelatedLesson(null);
			}
		}
		return null;
	}
	
	public static NSDictionary assumeNextLesson(WOContext ctx) {
		Object obj = ctx.session().objectForKey("assumeNextLesson");
		EduCourse course = null;
		EduLesson lesson = null;
		if(obj instanceof EduCourse) {
			course = (EduCourse)obj;
		} else if(obj instanceof EduLesson) {
			lesson = (EduLesson)obj;
			course = lesson.course();
		}
		if(course == null)
			return null;
		WeekFootprint footprint = null;
		try {
			footprint = (WeekFootprint)ctx.page().valueForKey("weekFootprint");
		} catch (Exception e) {
			footprint = new WeekFootprint(course);
		}
		return footprint.assumeNextLesson(lesson);

	}
	
	public static Object objectSaved(WOContext ctx) {
		boolean disable = SettingsReader.boolForKeyPath("edu.disablePlanFactCheck", false);
		if(disable) {
			return null;
		}
		Object saved = ctx.session().objectForKey("objectSaved");
		EduCourse course = null;
		NSTimestamp date = null;
		if (saved instanceof NSDictionary) {
			NSDictionary dict = (NSDictionary) saved;
			saved = dict.valueForKey("lesson");
			if(saved == null &&
					EduLesson.entityName.equals(dict.valueForKey("entityName"))) {
				course = (EduCourse)dict.valueForKey("course");
				date = (NSTimestamp)dict.valueForKey("date");
			}
		}
		if (saved instanceof EduLesson) {
			EduLesson lesson = (EduLesson) saved;
			if(!lesson.entityName().equals(EduLesson.entityName))
				return null;
			course = lesson.course();
			date = lesson.date();
			NSArray related = EOUtilities.objectsMatchingKeyAndValue(lesson.editingContext(), 
					Variation.ENTITY_NAME, "relatedLesson", lesson);
			if(related != null && related.count() > 0) {
				related.takeValueForKey(lesson.date(), Variation.DATE_KEY);
			}
		}
		if(course != null) {
			String widget = SettingsBase.stringSettingForCourse(
					"PlanFactWidget", course, course.editingContext());
			if("hide".equals(widget))
				return null;
			String usr = (String)ctx.session().valueForKeyPath("user.present");
			if(usr == null)
				usr = "??" + Person.Utility.fullName(course.teacher(), true, 2, 1, 1);
			WeekFootprint footprint = null;
			if(ctx.page().name().equals("LessonNoteEditor")) {
				footprint = (WeekFootprint)ctx.page().valueForKey("weekFootprint");
			} else {
				footprint = new WeekFootprint(course);
			}
			Reprimand.autoRelieve(course, date, usr, footprint);
		}
		return null;
	}
	
	public static OldLessonLock accessModifier(WOContext ctx) {
		SettingsBase sb = SettingsBase.baseForKey("OldLessonLock",
				ctx.session().defaultEditingContext(), false);
		if(sb == null)
			return null;
		return new OldLessonLock(sb);
	}
}