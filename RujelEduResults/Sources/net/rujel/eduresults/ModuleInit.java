// ModuleInit.java

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

package net.rujel.eduresults;

import net.rujel.reusables.*;
import net.rujel.base.MyUtility;
import net.rujel.interfaces.EduCourse;
import net.rujel.interfaces.EduLesson;

import com.webobjects.foundation.*;
import com.webobjects.eocontrol.*;
import com.webobjects.eoaccess.EOUtilities;
import com.webobjects.appserver.WOApplication;
import com.webobjects.appserver.WOContext;

import java.lang.reflect.Method;
import java.util.Enumeration;
import java.util.logging.Logger;

public class ModuleInit {
	protected static final NSDictionary tab = (NSDictionary)WOApplication.application().valueForKeyPath("strings.RujelEduResults_EduResults.itogTab");
	protected static final NSDictionary addOn = (NSDictionary)WOApplication.application().valueForKeyPath("strings.RujelEduResults_EduResults.itogAddOn");
	protected static final NSDictionary studentReporter = (NSDictionary)WOApplication.application().valueForKeyPath("strings.RujelEduResults_EduResults.studentReporter");
	protected static final NSArray marksPreset = ((NSArray)WOApplication.application().valueForKeyPath("strings.RujelEduResults_EduResults.marksPreset")).immutableClone();
	
	public static Object init(Object obj, WOContext ctx) {
		if(obj == null || obj.equals("init")) {
			ItogMark.init();
			PeriodType.init();
			EOSortOrdering.ComparisonSupport.setSupportForClass(
					new EduPeriod.ComparisonSupport(), Period.class);
			EOSortOrdering.ComparisonSupport.setSupportForClass(
					new EduPeriod.ComparisonSupport(), EduPeriod.class);
		} else if("schedulePeriod".equals(obj)) {
			return schedulePeriod(ctx);
		} else if("notesAddOns".equals(obj)) {
			return notesAddOns(ctx);
		} else if("studentReporter".equals(obj)) {
			return studentReporter(ctx);
		} else if("periods".equals(obj)) {
			return periods(ctx);
		} else if("lessonTabs".equals(obj)) {
			return lessonTabs(ctx);
		} else if("statCourseReport".equals(obj)) {
			return statCourseReport(ctx);
		}
		return null;
	}
	
	public static Object schedulePeriod(WOContext ctx) {
		EOEditingContext ec = new EOEditingContext();
		Integer year = net.rujel.base.MyUtility.eduYearForDate(new NSTimestamp());
		NSArray starterPeriods = EOUtilities.objectsWithQualifierFormat(ec,"EduPeriod","num = 1 AND eduYear = %@",new NSArray(year));
		
		//NSArray periodTypes = PeriodType.allPeriodTypes(ec);
		Enumeration enu = starterPeriods.objectEnumerator();
		//NSTimestamp today = new NSTimestamp();
		while (enu.hasMoreElements()) {
			//PeriodType perType = (PeriodType)enu.nextElement();
			EduPeriod period = (EduPeriod)enu.nextElement();//perType.currentPeriod(today);
			if(period == null) continue;
			Scheduler.sharedInstance().registerPeriod(period);
		}
		return null;
	}		
	
	public static NSMutableDictionary notesAddOns(WOContext ctx) {
		NamedFlags access = (NamedFlags)ctx.session().valueForKeyPath("readAccess.FLAGS.ItogMark");
		if(access.getFlag(0)) {
			NSMutableDictionary itogAddOn = addOn.mutableClone();
			itogAddOn.takeValueForKey(access,"access");
			return itogAddOn;
		}
		return null;
	}
	
	public static NSDictionary studentReporter(WOContext ctx) {
		NamedFlags access = (NamedFlags)ctx.session().valueForKeyPath("readAccess.FLAGS.ItogMark");
		//NamedFlags access = moduleAccess(ctx,"ItogMark");
		if(!access.getFlag(0))
				return null;
		NSMutableDictionary result = studentReporter.mutableClone();
		result.takeValueForKey(access,"access");
		return result;
	}
	
	public static NSArray periods(WOContext ctx) {
		Object eduYear = ctx.session().valueForKey("eduYear");
		NSArray result = EOUtilities.objectsMatchingKeyAndValue(ctx.session().defaultEditingContext()
				,"EduPeriod","eduYear",eduYear);
		return result;
		//return EOSortOrdering.sortedArrayUsingKeyOrderArray(result,EduPeriod.sorter);
	}

	protected static class PeriodTab implements Tabs.GenericTab {
		protected String title;
		protected String hover;
		protected EOQualifier qual;
		protected boolean current;
		protected int code;
		
		public PeriodTab(EduPeriod period, boolean isCurrent) {
			title = period.title();
			code = period.code();
			current = isCurrent;
			NSMutableArray quals = new NSMutableArray();
			quals.addObject(new EOKeyValueQualifier
					("date",EOQualifier.QualifierOperatorGreaterThanOrEqualTo,period.begin()));
			quals.addObject(new EOKeyValueQualifier
					("date",EOQualifier.QualifierOperatorLessThanOrEqualTo,period.end()));
			qual = new EOAndQualifier(quals);
			hover = period.name();
		}
		public boolean defaultCurrent() {
			return current;
		}

		public String title() {
			return title;
		}
		public String hover() {
			return hover;
		}		
		public EOQualifier qualifier() {
			return qual;
		}
		
		public boolean equals(Object obj) {
			if (obj instanceof PeriodTab) {
				PeriodTab aTab = (PeriodTab) obj;
				return (this.code == aTab.code);
			}
			return false;
		}

		public int hashCode() {
			return code;
		}

	}

	public static NSArray lessonTabs(WOContext ctx) {
		EduCourse course = (EduCourse)ctx.session().objectForKey("courseForlessons");
		NSTimestamp currDate = (NSTimestamp)ctx.session().objectForKey("recentDate");
		if(currDate == null) {
		EduLesson currLesson = (EduLesson)ctx.session().objectForKey("selectedLesson");
		currDate = (currLesson != null)?currLesson.date():
			(NSTimestamp)ctx.session().valueForKey("today");
		}
		NSArray periods = EduPeriod.periodsForCourse(course);
		if(periods == null || periods.count() == 0)
			return null;
		Enumeration enu = periods.objectEnumerator();
		NSMutableArray result = new NSMutableArray();
		while (enu.hasMoreElements()) {
			EduPeriod per = (EduPeriod) enu.nextElement();
			boolean isCurrent = per.contains(currDate);
			result.addObject(new PeriodTab(per,isCurrent));
		}
		return new NSArray((Object)result);
	}

	public static NSDictionary statCourse(EduCourse course, EduPeriod period) {
		NSArray itogs = ItogMark.getItogMarks(course.cycle(), period, null);
//		itogs = MyUtility.filterByGroup(itogs, "student", course.groupList(), true);
		if(itogs == null || itogs.count() == 0)
			return NSDictionary.EmptyDictionary;
		if(itogs.count() > 1) {
			EOSortOrdering so = new EOSortOrdering(ItogMark.MARK_KEY,
					EOSortOrdering.CompareCaseInsensitiveAscending);
			itogs = EOSortOrdering.sortedArrayUsingKeyOrderArray(itogs, new NSArray(so));
		}
		NSMutableArray keys = new NSMutableArray();
		NSArray group = course.groupList();
		NSMutableDictionary result = new NSMutableDictionary(keys, "keys");
		int total = group.count();
		result.setObjectForKey(new Integer(total), "total");
		Enumeration enu = itogs.objectEnumerator();
		String currKey = null;
		int currCount = 0;
		while (enu.hasMoreElements()) {
			ItogMark itog = (ItogMark) enu.nextElement();
			if(!group.containsObject(itog.student()))
				continue;
			if((currKey==null)?itog.mark()==null:currKey.equalsIgnoreCase(itog.mark())) {
				currCount++;
			} else {
				if(currCount > 0)
					result.setObjectForKey(new Integer(currCount), (currKey==null)?" ":currKey);
				currKey = itog.mark();
				keys.addObject((currKey==null)?" ":currKey);
				currCount = 1;
			}
			total--;
		}
		if(currCount > 0)
			result.setObjectForKey(new Integer(currCount), currKey);
		if(total > 0) {
			result.setObjectForKey(new Integer(total), "");
			keys.addObject("");
		}
		return result;
	}
	
	public static EOEnterpriseObject getStatsGrouping (EduCourse course, EduPeriod period) {
		EOEnterpriseObject grouping = null;
		try {
			Class descClass = Class.forName("net.rujel.stats.Description");
			Method method = descClass.getMethod("getGrouping", String.class, String.class,
					EOEnterpriseObject.class, EOEnterpriseObject.class, Boolean.TYPE);
			grouping = (EOEnterpriseObject)method.invoke(null, ItogMark.ENTITY_NAME, 
					ItogMark.MARK_KEY, course, period, Boolean.TRUE);
			if(grouping.valueForKeyPath("description.description") == null) {
				String prName = (String)WOApplication.application().valueForKeyPath(
					"strings.RujelEduResults_EduResults.properties.ItogMark.this");
				grouping.takeValueForKeyPath(prName,"description.description");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return grouping;
	}
	
	public static EOEnterpriseObject prepareStats(EduCourse course, EduPeriod period, boolean save) {
		EOEnterpriseObject grouping = getStatsGrouping(course, period);
		if(grouping != null) {
			NSArray itogs = ItogMark.getItogMarks(course.cycle(), period, null);
			itogs = MyUtility.filterByGroup(itogs, "student", course.groupList(), true);
			grouping.takeValueForKey(itogs, "array");
//			NSDictionary stats = ModuleInit.statCourse(course, period);
//			grouping.takeValueForKey(stats, "dict");
			if (save) {
				EOEditingContext ec = grouping.editingContext();
				try {
					ec.saveChanges();
				} catch (Exception e) {
					Logger.getLogger("rujel.eduresults").log(WOLogLevel.WARNING,
							"Failed to save itog Stats for course", new Object[] {course,e});
					ec.revert();
				}
			}
		}
		return grouping;
	}

	
	public static NSArray marksPreset() {
		return marksPreset;
	}
	
	public static Object statCourseReport(WOContext ctx) {
		EOEditingContext ec = null;
		try {
			ec = (EOEditingContext)ctx.page().valueForKey("ec");
		} catch (Exception e) {
			ec = new SessionedEditingContext(ctx.session());
		}
		Integer year = (Integer)ctx.session().valueForKey("eduYear");
		NSArray list = PeriodType.usagesForYear(year,ec);
		if(list == null || list.count() == 0)
			return null;
		list = (NSArray)list.valueForKey("periodType");
		NSSet pertypes = new NSSet(list);
		NSMutableArray result = new NSMutableArray();
		
		Enumeration enu = EduPeriod.periodsInYear(year, ec).objectEnumerator();
		NSMutableDictionary template = new NSMutableDictionary(ItogMark.ENTITY_NAME,"entName");
		template.setObjectForKey(ItogMark.MARK_KEY, "statField");
		template.setObjectForKey(marksPreset(),"keys");
		
		try {
			Method method = ModuleInit.class.getMethod("statCourse",
					EduCourse.class, EduPeriod.class);
			template.setObjectForKey(method,"ifEmpty");
		} catch (Exception e) {
			e.printStackTrace();
		}

		String title = (String)WOApplication.application().valueForKeyPath(
				"strings.RujelEduResults_EduResults.itogAddOn.title");
		int sort = 50;
		while (enu.hasMoreElements()) {
			EduPeriod period = (EduPeriod) enu.nextElement();
			if(!pertypes.containsObject(period.periodType()))
				continue;
			NSMutableDictionary dict = template.mutableClone();
			dict.setObjectForKey(title + " - " + period.title(),"title");
			dict.setObjectForKey(period,"param2");
			dict.setObjectForKey(String.valueOf(sort),"sort");
			result.addObject(dict);
			sort++;
		}

		return result;
	}
}
