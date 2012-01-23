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

import java.util.Calendar;
import java.util.Enumeration;
import java.util.TimerTask;
import java.util.logging.Logger;

import net.rujel.base.MyUtility;
import net.rujel.base.SettingsBase;
import net.rujel.eduplan.EduPeriod;
import net.rujel.eduplan.Holiday;
import net.rujel.eduplan.PlanCycle;
import net.rujel.interfaces.EduCourse;
import net.rujel.interfaces.EduLesson;
import net.rujel.interfaces.Person;
import net.rujel.reusables.*;

import com.webobjects.appserver.WOApplication;
import com.webobjects.appserver.WOContext;
import com.webobjects.eoaccess.EOUtilities;
import com.webobjects.eocontrol.EOAndQualifier;
import com.webobjects.eocontrol.EOEditingContext;
import com.webobjects.eocontrol.EOFetchSpecification;
import com.webobjects.eocontrol.EOKeyValueQualifier;
import com.webobjects.eocontrol.EOQualifier;
import com.webobjects.eocontrol.EOSortOrdering;
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
		} else if("assumeNextLesson".equals(obj)) {
			return assumeNextLesson(ctx);
		} else if("objectSaved".equals(obj)) {
			return objectSaved(ctx);
		} else if("accessModifier".equals(obj)) {
			return accessModifier(ctx);
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
		MyUtility.scheduleTaskOnTime(task,checkTime);
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
	
	private static final Integer sort = new Integer(30);
	public static NSDictionary assumeNextLesson(WOContext ctx) {
		Object obj = ctx.session().objectForKey("assumeNextLesson");
		if(obj instanceof EduCourse)
			return null;
		EduCourse course = ((EduLesson)obj).course();
		EOEditingContext ec = course.editingContext();
		Calendar cal = Calendar.getInstance();
		cal.setTime(((EduLesson)obj).date());
		NSArray sorter = new NSArray(new EOSortOrdering(
				"date",EOSortOrdering.CompareDescending));
		EOQualifier qual = new EOKeyValueQualifier ("course",
				EOQualifier.QualifierOperatorEqual,course);
		EOFetchSpecification fs = new EOFetchSpecification(Variation.ENTITY_NAME,qual,sorter);
		NSArray list = ec.objectsWithFetchSpecification(fs);
		if(list != null && list.count() > 0) {
				Variation var = (Variation)list.objectAtIndex(0);
				if(var.date().getTime() > cal.getTimeInMillis() &&
						!var.reason().namedFlags().flagForKey("external")) {
					Calendar vcal = Calendar.getInstance();
					vcal.setTime(var.date());
					if(vcal.get(Calendar.YEAR) > cal.get(Calendar.YEAR) ||
							(vcal.get(Calendar.YEAR) == cal.get(Calendar.YEAR) 
						&& vcal.get(Calendar.DAY_OF_YEAR) > cal.get(Calendar.DAY_OF_YEAR))) {
						
						if(var.value().intValue() > 0) {
							return new NSDictionary(
								new Object[] {sort,var.date()},
								new Object[] {"sort","date"});
						}
						cal = vcal;
					}
				}
		} // choose last date from lessons and variations
		int weekStart = SettingsBase.numericSettingForCourse(
				"weekStart", course, ec, Calendar.MONDAY);
		cal.set(Calendar.HOUR_OF_DAY, 12);
		long last = cal.getTimeInMillis();
		cal.add(Calendar.DATE, 1);
		while(cal.get(Calendar.DAY_OF_WEEK) != weekStart)
			cal.add(Calendar.DATE, 1);
		NSTimestamp date = new NSTimestamp(cal.getTimeInMillis());
		int plan = PlanCycle.planHoursForCourseAndDate(course, date);
		if(plan <= 0)
			return null;
		int week = SettingsBase.numericSettingForCourse(
				EduPeriod.ENTITY_NAME, course, ec,7);
		int year = cal.getActualMaximum(Calendar.DAY_OF_YEAR);

		String listName = SettingsBase.stringSettingForCourse(EduPeriod.ENTITY_NAME, course, ec);
		NSDictionary dict = Reprimand.prepareDict(date, listName, ec, week, weekStart);
		if(dict == null)
			return null;
		EOQualifier[] quals = (EOQualifier[])dict.valueForKey("weekQualifier");
		quals[2] = new EOKeyValueQualifier("course",
				EOQualifier.QualifierOperatorEqual,course);
		qual = new EOAndQualifier(new NSArray(quals));
		fs = new EOFetchSpecification(EduLesson.entityName, qual,MyUtility.dateSorter);
		list = ec.objectsWithFetchSpecification(fs);
		int ref = ((Integer)dict.valueForKey("refDay")).intValue();
		int[] currWeek = new int[week];
		int fact = Reprimand.putLessons(list, ref, currWeek, 1);
		fs.setEntityName(Variation.ENTITY_NAME);
		list = ec.objectsWithFetchSpecification(fs);
		int verifiedOnly = SettingsBase.numericSettingForCourse(
				"ignoreUnverifiedReasons", course, ec, 0);
		fact += Reprimand.putVariations(list, ref, currWeek,verifiedOnly > 0, 1);
		if(fact >= plan) { // get first lesson of next week
			for (int i = 0; i < currWeek.length; i++) {
				if(currWeek[i] > 0) {
					cal.add(Calendar.DATE,i);
					date = new NSTimestamp(cal.getTimeInMillis());
					break;
				}
			}
		} else { // compare with previous week
			cal.add(Calendar.DATE, - week);
			quals = (EOQualifier[])dict.valueForKey("prevQualifier");
			if(quals == null) {
				quals = new EOQualifier[3];
				cal.add(Calendar.DATE, - week);
				ref = cal.get(Calendar.DAY_OF_YEAR);
				NSTimestamp tmpDate = new NSTimestamp(cal.getTimeInMillis());
				quals[0] = new EOKeyValueQualifier("date",
						EOQualifier.QualifierOperatorGreaterThanOrEqualTo,tmpDate);
				cal.add(Calendar.DATE, week);
				tmpDate = new NSTimestamp(cal.getTimeInMillis());
				quals[1] = new EOKeyValueQualifier("date",
						EOQualifier.QualifierOperatorLessThan,tmpDate);
			} else {
				ref = ((Integer)dict.valueForKey("prevRef")).intValue();
			}
			quals[2] = new EOKeyValueQualifier("course",
					EOQualifier.QualifierOperatorEqual,course);
			qual = new EOAndQualifier(new NSArray(quals));
			fs = new EOFetchSpecification(EduLesson.entityName, qual,MyUtility.dateSorter);
			list = ec.objectsWithFetchSpecification(fs);
			int[] prevWeek = new int[week];
			Reprimand.putLessons(list, ref, prevWeek, 1);
			fs.setEntityName(Variation.ENTITY_NAME);
			list = ec.objectsWithFetchSpecification(fs);
			Reprimand.putVariations(list, ref, prevWeek,verifiedOnly > 0, 1);
			year = cal.getActualMaximum(Calendar.DAY_OF_YEAR);
			cal.set(Calendar.HOUR_OF_DAY,23);
			boolean none = true;
			for (int i = 0; i < currWeek.length; i++) {
				if(currWeek[i] < prevWeek[i] && cal.getTimeInMillis() >= last) {
					none = false;
					break;
				}
				cal.add(Calendar.DATE,1);
			}
			currWeek = prevWeek;
			if(none) {
				for (int i = 0; i < currWeek.length; i++) {
					if(currWeek[i] > 0) {
						none = false;
						break;
					}
					cal.add(Calendar.DATE,1);
				}
			}
			if(none) {
				return null;
			} else {
				date = new NSTimestamp(cal.getTimeInMillis());
			}
		}
		EduPeriod per = null;
		Enumeration enu = null;
		list = EduPeriod.periodsInList(listName, ec);
		if(list != null && list.count() > 0) {
			enu = list.objectEnumerator();
		}
		do {
			NSTimestamp fromDate = null;
			if(enu != null && (per == null || !per.contains(date))) {
				while (enu.hasMoreElements()) {
					per = (EduPeriod) enu.nextElement();
					if(per.contains(date)) {
						break;
					}
					if(per.begin().after(date)) {
						fromDate = per.begin();
						break;
					}
					per = null;
				}
				if(per == null)
					return null;
			}
			if(fromDate == null)
				list = Holiday.holidaysInDates(date, date, ec, listName);
			else
				list = Holiday.holidaysInDates(fromDate, fromDate, ec, listName);
			if (list != null && list.count() > 0) {
				Holiday hd = (Holiday)list.lastObject();
				fromDate = hd.end();
			}
			if(fromDate == null)
				break;
			cal.setTime(fromDate);
			cal.add(Calendar.DATE, 1);
			int days = cal.get(Calendar.DAY_OF_YEAR) - ref;
			if(days < 0)
				days += year;
			boolean none = true;
			for (int i = days % week; i < currWeek.length; i++) {
				if(currWeek[i] > 0) {
					none = false;
					break;
				}
				cal.add(Calendar.DATE,1);
			}
			if(none) {
				for (int i = 0; i < currWeek.length; i++) {
					if(currWeek[i] > 0) {
						none = false;
						break;
					}
					cal.add(Calendar.DATE,1);
				}
			}
			if(none) {
				return null;
			} else {
				date = new NSTimestamp(cal.getTimeInMillis());
			}
		} while(true);
		return new NSDictionary(
				new Object[] {sort,date},
				new Object[] {"sort","date"});
	}
	
	public static Object objectSaved(WOContext ctx) {
		boolean disable = Boolean.getBoolean("PlanFactCheck.disable")
				|| SettingsReader.boolForKeyPath("edu.disablePlanFactCheck", false);
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
			String widget = SettingsBase.stringSettingForCourse(
						"PlanFactWidget", course, course.editingContext());
			if("hide".equals(widget))
				return null;
			date = lesson.date();
			NSArray related = EOUtilities.objectsMatchingKeyAndValue(lesson.editingContext(), 
					Variation.ENTITY_NAME, "relatedLesson", lesson);
			if(related != null && related.count() > 0) {
				related.takeValueForKey(lesson.date(), Variation.DATE_KEY);
			}
		}
		if(course != null) {
			String usr = (String)ctx.session().valueForKeyPath("user.present");
			if(usr == null)
				usr = "??" + Person.Utility.fullName(course.teacher(), true, 2, 1, 1);
			Reprimand.autoRelieve(course, date, usr);
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