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
		} else if("deleteCourse".equals(obj)) {
			return deleteCourse(ctx);
		} else if("assumeNextLesson".equals(obj)) {
			return assumeNextLesson(ctx);
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
						ec = new EOEditingContext(lesson.
								editingContext().parentObjectStore());
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
		EduCourse course = (EduCourse)ctx.session().objectForKey("assumeNextLesson");
		if(course == null)
			return null;
		NSArray sorter = new NSArray(new EOSortOrdering(
				"date",EOSortOrdering.CompareDescending));
		EOQualifier qual = new EOKeyValueQualifier ("course",
				EOQualifier.QualifierOperatorEqual,course);
		EOFetchSpecification fs = new EOFetchSpecification(
				EduLesson.entityName,qual,sorter);
		EOEditingContext ec = course.editingContext();
		NSArray list = ec.objectsWithFetchSpecification(fs);
		if(list == null || list.count() == 0)
			return null;
		Enumeration lenu = list.objectEnumerator();
		EduLesson lesson = (EduLesson)lenu.nextElement();
		Calendar cal = Calendar.getInstance();
		cal.setTime(lesson.date());
		fs.setEntityName(Variation.ENTITY_NAME);
		list = ec.objectsWithFetchSpecification(fs);
		if(list != null && list.count() > 0) {
			Enumeration venu = list.objectEnumerator();
			while (venu.hasMoreElements()) {
				Variation var = (Variation) venu.nextElement();
				if(var.reason().namedFlags().flagForKey("external"))
					continue;
				int lday = cal.get(Calendar.DAY_OF_YEAR);
				int lyear = cal.get(Calendar.YEAR);
				cal.setTime(var.date());
				int value = var.value().intValue();
				if(cal.get(Calendar.YEAR) == lyear &&
						cal.get(Calendar.DAY_OF_YEAR) == lday) {
					if(value > 0) {
						value--;
						while(value > 0) {
							if(!lenu.hasMoreElements())
								return new NSDictionary(
										new Object[] {sort,var.date()},
										new Object[] {"sort","date"});
							lesson = (EduLesson)lenu.nextElement();
							cal.setTime(lesson.date());
							if(cal.get(Calendar.YEAR) == lyear &&
									cal.get(Calendar.DAY_OF_YEAR) == lday) {
								value--;
							} else {
								return new NSDictionary(
										new Object[] {sort,var.date()},
										new Object[] {"sort","date"});
							}
						}
					} else {
						lesson = null;
						break;
					}
				} else {
					if(cal.get(Calendar.YEAR) > lyear || (cal.get(Calendar.YEAR) == lyear 
							&& cal.get(Calendar.DAY_OF_YEAR) > lday)) {
						if(value > 0) {
							return new NSDictionary(
								new Object[] {sort,var.date()},
								new Object[] {"sort","date"});
						}
						lesson = null;
					}
					break;
				}
			}
			if(lesson != null)
				cal.setTime(lesson.date());
		} // choose last date from lessons and variations
		int weekStart = SettingsBase.numericSettingForCourse(
				"weekStart", course, ec, Calendar.MONDAY);
		int year = cal.getActualMaximum(Calendar.YEAR);
		cal.set(Calendar.HOUR_OF_DAY, 0);
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
		if(fact == plan) {
			for (int i = 0; i < currWeek.length; i++) {
				if(currWeek[i] > 0) {
					cal.add(Calendar.DATE,i);
					date = new NSTimestamp(cal.getTimeInMillis());
					break;
				}
			}
		} else {
			quals = (EOQualifier[])dict.valueForKey("prevQualifier");
			if(quals == null)
				return null;
			quals[2] = new EOKeyValueQualifier("course",
					EOQualifier.QualifierOperatorEqual,course);
			qual = new EOAndQualifier(new NSArray(quals));
			fs = new EOFetchSpecification(EduLesson.entityName, qual,MyUtility.dateSorter);
			list = ec.objectsWithFetchSpecification(fs);
			ref = ((Integer)dict.valueForKey("prevRef")).intValue();
			int[] prevWeek = new int[week];
			Reprimand.putLessons(list, ref, prevWeek, 1);
			fs.setEntityName(Variation.ENTITY_NAME);
			list = ec.objectsWithFetchSpecification(fs);
			Reprimand.putVariations(list, ref, prevWeek,verifiedOnly > 0, 1);
			cal.add(Calendar.DATE, - week);
			year = cal.getActualMaximum(Calendar.YEAR);
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
		if(EduPeriod.getCurrentPeriod(date, listName, ec) == null)
			return null;
		list = Holiday.holidaysInDates(date, date, ec, listName);
		while (list != null && list.count() > 0) {
			Holiday hd = (Holiday)list.lastObject();
			cal.setTime(hd.end());
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
			if(EduPeriod.getCurrentPeriod(date, listName, ec) == null)
				return null;
			list = Holiday.holidaysInDates(date, date, ec, listName);
		}
		return new NSDictionary(
				new Object[] {sort,date},
				new Object[] {"sort","date"});
	}
}