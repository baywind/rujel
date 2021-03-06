//VariationsPlugin.java: Class file for WO Component 'VariationsPlugin'

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

package net.rujel.curriculum;

import java.util.Calendar;
import java.util.Enumeration;

import net.rujel.base.SettingsBase;
import net.rujel.eduplan.*;
import net.rujel.interfaces.EOPeriod;
import net.rujel.interfaces.EduCourse;
import net.rujel.reusables.Period;
import net.rujel.reusables.SettingsReader;

import com.webobjects.appserver.*;
import com.webobjects.eoaccess.EOUtilities;
import com.webobjects.eocontrol.EOEditingContext;
import com.webobjects.eocontrol.EOEnterpriseObject;
import com.webobjects.eocontrol.EOKeyValueQualifier;
import com.webobjects.eocontrol.EOQualifier;
import com.webobjects.eocontrol.EOSortOrdering;
import com.webobjects.foundation.*;

// Generated by the WOLips Templateengine Plug-in at Feb 10, 2009 3:17:25 PM
public class VariationsPlugin extends com.webobjects.appserver.WOComponent {
//    protected EduCourse _course;
	public NSDictionary planFact;

	public VariationsPlugin(WOContext context) {
        super(context);
    }

/*	public EduCourse course() {
		if(_course == null) {
			_course = (EduCourse)valueForBinding("course");
		}
		return _course;
	}*/
	
    public void appendToResponse(WOResponse aResponse, WOContext aContext) {
    	NSTimestamp today = (NSTimestamp)session().valueForKey("today");
    	EduCourse course = (EduCourse)valueForBinding("course");
    	WeekFootprint weekFootprint = (WeekFootprint)valueForBinding("weekFootprint");
    	planFact = planFact(course, today, weekFootprint);
    	super.appendToResponse(aResponse, aContext);
    }

    public String varsTotal() {
    	Integer netChange = (Integer)valueForKeyPath("planFact.netChange");
    	if(netChange == null || netChange.intValue() == 0)
    		return null;
		StringBuilder vars = new StringBuilder(" <span style = \"color:#666666;\">");
		if(netChange.intValue() > 0)
			vars.append('+');
		vars.append(netChange).append("</span>");
		return vars.toString();
    }
	
	public WOActionResults popup() {
		WOComponent popup = pageWithName("VariationsList");
		popup.takeValueForKey(valueForBinding("course"), "course");
		popup.takeValueForKey(context().page(), "returnPage");
		popup.takeValueForKey(valueForBinding("weekFootprint"),"weekFootprint");
//		popup.takeValueForKey(valueForBinding("currLesson"), "currLesson");
//		popup.takeValueForKey(valueForBinding("currTab"), "currTab");
		//popup.takeValueForKey(planFact, "planFact");
		return popup;
	}

	public boolean isStateless() {
		return true;
	}
	
	public boolean synchronizesVariablesWithBindings() {
		return false;
	}
	
	public void reset() {
		planFact = null;
//		_course = null;	
		super.reset();
	}
	
	protected static int weeks(String listName, Calendar cal,
			EOEditingContext ec, int week, int weekStart) {
		NSArray periods = EduPeriod.periodsInList(listName, ec);
		if(periods == null || periods.count() == 0)
			periods = EduPeriod.defaultPeriods(ec); 
		if(periods == null || periods.count() == 0)
			return 0;
		NSTimestamp finDate = null;
		if(cal == null) {
			cal = Calendar.getInstance();
		} else {
//			cal.add(Calendar.DATE, 1);
			cal.set(Calendar.HOUR_OF_DAY,1);
			finDate = new NSTimestamp(cal.getTimeInMillis());
		}
		NSMutableArray holidays = new NSMutableArray();
		Enumeration enu = periods.objectEnumerator();
		NSTimestamp perFin = null;
		/*if(startDate != null) {
			cal.setTime(startDate);
			while(cal.get(Calendar.DAY_OF_WEEK) != weekStart)
				cal.add(Calendar.DATE, -1);
			startDate = new NSTimestamp(cal.getTimeInMillis());
		}*/
		NSTimestamp startDate = null;
		while (enu.hasMoreElements()) {
			EduPeriod eduPer = (EduPeriod) enu.nextElement();
			if(finDate != null && finDate.before(eduPer.begin()))
				break;
			if(startDate == null) {
				cal.setTime(eduPer.begin());
				if(cal.get(Calendar.DAY_OF_WEEK) != weekStart) {
					cal.add(Calendar.DATE, 1);
					while(cal.get(Calendar.DAY_OF_WEEK) != weekStart)
						cal.add(Calendar.DATE, -1);
				}
				cal.set(Calendar.HOUR_OF_DAY, 0);
				startDate = new NSTimestamp(cal.getTimeInMillis());
			}
			if(perFin != null && EOPeriod.Utility.compareDates(perFin, eduPer.begin()) < 0) {
				cal.setTime(eduPer.begin());
				cal.add(Calendar.DATE, -1);
				cal.set(Calendar.HOUR_OF_DAY, 20);
				Period per = new EOPeriod.ByDates(perFin,new NSTimestamp(cal.getTimeInMillis()));
				holidays.addObject(per);
			}
			cal.setTime(eduPer.end());
			cal.add(Calendar.DATE, 1);
			cal.set(Calendar.HOUR_OF_DAY, 0);
			perFin = new NSTimestamp(cal.getTimeInMillis());
		}
		if(startDate == null)
			return 0;
		int days;
		if(finDate == null || EOPeriod.Utility.compareDates(finDate,perFin) >= 0) {
			cal.add(Calendar.DATE, -1);
			cal.set(Calendar.HOUR_OF_DAY, 1);
			days = EOPeriod.Utility.countDays(startDate, cal.getTime());
			int left = days % week;
			if(left > 0) {
				cal.add(Calendar.DATE, week - left);
			}
			if(finDate == null || 
					EOPeriod.Utility.compareDates(finDate.getTime(),cal.getTimeInMillis()) > 0) {
				long millis = cal.getTimeInMillis();
				cal.setTime(finDate);
				finDate = new NSTimestamp(millis);
				days += week -left;
			} else {
				cal.add(Calendar.DATE, -week);
			}
			Period per = new EOPeriod.ByDates(perFin,finDate);
			holidays.addObject(per);
		} else {
			cal.setTime(finDate);
			days = EOPeriod.Utility.countDays(startDate, finDate);
			int left = days % week;
			if(left > 0)
				cal.add(Calendar.DATE, -left);
		}
		int result = days / week;
/*		int left = days % week;
		if(left > 0) {
			cal.add(Calendar.DATE, -left);
			if(ended) {
				result++;
				cal.add(Calendar.DATE, week);
				Period per = new Period.ByDates(perFin,new NSTimestamp(cal.getTimeInMillis()));
				holidays.addObject(per);
			}
			finDate = new NSTimestamp(cal.getTimeInMillis());
		}*/
		NSArray realHolidays = Holiday.holidaysInDates(startDate,finDate, ec, listName);
		if(holidays.count() == 0) {
			holidays.setArray(realHolidays);
		} else {
			holidays.addObjectsFromArray(realHolidays);
			EOSortOrdering.sortArrayUsingKeyOrderArray(holidays, EduPeriod.sorter);
		}
		result -= holidayWeeks(holidays, weekStart, week, startDate, finDate);
//		if(finDate <  perFin.getTime())
//			finDate -= 2 * NSLocking.OneHour;
//		cal.setTime(finDate);
		return result;
	}
	
	public static int holidayWeeks(NSArray holidays, int weekStart, int week, 
			NSTimestamp begin, NSTimestamp end) {
		if(holidays == null || holidays.count() == 0)
			return 0;
		Enumeration enu = holidays.objectEnumerator();
		int result = 0;
		Calendar cal = Calendar.getInstance();
		cal.setTime(begin);
		while (cal.get(Calendar.DAY_OF_WEEK) != weekStart)
			cal.add(Calendar.DATE, 1);
		cal.set(Calendar.HOUR_OF_DAY, 20);
		NSTimestamp prevStartDay = new NSTimestamp(cal.getTimeInMillis());
		NSTimestamp prevFinDay = prevStartDay;
		while (enu.hasMoreElements() && prevFinDay.before(end)) {
			Period per = (Period) enu.nextElement();
			cal.setTime(per.begin());
			cal.set(Calendar.HOUR_OF_DAY, 0);
//			int perBegin = cal.get(Calendar.DAY_OF_YEAR);
			if(cal.getTimeInMillis() > prevFinDay.getTime()) {
				result += (EOPeriod.Utility.countDays(prevStartDay, prevFinDay) -1)/week;
				while (cal.get(Calendar.DAY_OF_WEEK) != weekStart) {
					cal.add(Calendar.DATE, 1);
				}
				prevStartDay = new NSTimestamp(cal.getTimeInMillis());
			}
			
			if(per.end().before(end))
				cal.setTime(per.end());
			else
				cal.setTime(end);
			cal.set(Calendar.HOUR_OF_DAY, 20);
			cal.add(Calendar.DATE, 1);
			if(cal.getTimeInMillis() > prevFinDay.getTime())
				prevFinDay = new NSTimestamp(cal.getTimeInMillis());
		}
		result += (EOPeriod.Utility.countDays(prevStartDay, prevFinDay) -1)/week;
		return result;
	}
	
	public static NSDictionary planFact(EduCourse course, NSTimestamp date) {
		return planFact(course, date, null);
	}
	public static NSDictionary planFact(EduCourse course, NSTimestamp date,
			WeekFootprint weekFootprint) {
		int minPlan = 0;
		int maxDev = 0;
		int weeks = 0;
		int extraDays = 0;
//		int active = 2;
		NSTimestamp beginDate = null;
		EOEditingContext ec = course.editingContext();
		int weekDays = SettingsBase.numericSettingForCourse(
				EduPeriod.ENTITY_NAME, course, ec,7);
		int weekStart = SettingsBase.numericSettingForCourse(
				"weekStart", course, ec, Calendar.MONDAY);

		String listName = SettingsBase.stringSettingForCourse(EduPeriod.ENTITY_NAME, course, ec);
/*		if(date != null) {
			active = EduPeriod.dayState(date, ec, listName);
			if(active==0) {
				Calendar cal = Calendar.getInstance();
				cal.setTime(date);
				exclude = (cal.get(Calendar.HOUR_OF_DAY) == 0);
			}
		}*/
		
		Calendar cal = Calendar.getInstance();
		//
		if(date != null) {
			cal.setTime(date);
			if(cal.get(Calendar.MILLISECOND) > 0 && cal.get(Calendar.HOUR_OF_DAY) 
					< SettingsReader.intForKeyPath("edu.midnightHour", 5)) {
				cal.add(Calendar.DATE, -1);
			}
			cal.set(Calendar.HOUR_OF_DAY, 12);
			date = new NSTimestamp(cal.getTimeInMillis());
		}
		NSArray list = EOUtilities.objectsMatchingKeyAndValue(ec,
				"PlanDetail","course", course);
		if(list != null && list.count() > 0) {  // has details
			Enumeration enu = list.objectEnumerator();
			while (enu.hasMoreElements()) {
				EOEnterpriseObject pd = (EOEnterpriseObject) enu.nextElement();
				Integer hours = (Integer)pd.valueForKey("hours");
				if(hours == null || hours.intValue() == 0)
					continue;
				EduPeriod per = (EduPeriod)pd.valueForKey("eduPeriod");
//				int days = 0;
				if(hours.intValue() > 0 &&
						(date == null || EOPeriod.Utility.compareDates(date,per.end()) >= 0)) {
					minPlan += hours.intValue();
					beginDate = per.end();
					continue;
				}
				if(date != null && EOPeriod.Utility.compareDates(date,per.begin()) < 0)
					continue;
				hours = (Integer)pd.valueForKey("weekly");
				if(hours == null || hours.intValue() == 0)
					continue;
				maxDev = hours.intValue();
				NSMutableArray holidays = new NSMutableArray();
				NSTimestamp start = per.begin();
				cal.setTime(per.begin());
				if(cal.get(Calendar.DAY_OF_WEEK) != weekStart) {
					cal.add(Calendar.DATE, 1);
					while(cal.get(Calendar.DAY_OF_WEEK) != weekStart)
						cal.add(Calendar.DATE, -1);
					start = new NSTimestamp(cal.getTimeInMillis());
					if(start.before(per.begin())) {
						Period pre = new EOPeriod.ByDates(start,per.begin());
						holidays.addObject(pre);
					}
				}
//				if(beginDate == null)
//					beginDate = start;
				NSTimestamp end = per.end();
				boolean fin = (date == null ||
						EOPeriod.Utility.compareDates(date,end) >= 0); 
				if(!fin)
					end = date;
				NSArray realHolidays = Holiday.holidaysInDates(start,end, ec, listName);
				holidays.addObjectsFromArray(realHolidays);
				int perDays = EOPeriod.Utility.countDays(start, end);
				int left = perDays%weekDays;
				int perWeeks = perDays/weekDays;
				cal.setTime(end);
				if(left > 0) {
					if (fin) {
						cal.add(Calendar.DATE, 1);
						NSTimestamp afterEnd = new NSTimestamp(cal.getTimeInMillis());
						cal.add(Calendar.DATE, weekDays - left -1);
						end = new NSTimestamp(cal.getTimeInMillis());
						holidays.addObject(new EOPeriod.ByDates(afterEnd,end));
						perWeeks++;
					} else {
						extraDays = left;
						cal.add(Calendar.DATE, -left);
					}
				}
				perWeeks -= holidayWeeks(holidays, weekStart, weekDays, start, end);
				minPlan += perWeeks*maxDev;
				weeks += perWeeks;
			}
			cal.setTime(date);
			if(cal.get(Calendar.DAY_OF_WEEK) != weekStart) {
				cal.add(Calendar.DATE, 1);
				while(cal.get(Calendar.DAY_OF_WEEK) != weekStart)
					cal.add(Calendar.DATE, -1);
			}
			cal.set(Calendar.HOUR_OF_DAY, 1);
		} else {  // no details
			if(date != null) {
				weeks = weeks(listName, cal, ec, weekDays, weekStart);
				if(cal.getTimeInMillis() != date.getTime())
					extraDays = EOPeriod.Utility.countDays(cal.getTime(), date) -1;
			} else {
				weeks = weeks(listName, null, ec, weekDays, weekStart);
			}
			PlanCycle cl = (PlanCycle)course.cycle();
			maxDev = cl.weekly(course);
			minPlan = maxDev * weeks;
		}

		NSMutableDictionary result = new NSMutableDictionary();
		result.takeValueForKey(new Integer(minPlan), "minPlan");
		result.takeValueForKey(new Integer(maxDev), "maxDeviation");
		result.takeValueForKey(new Integer(weeks), "weeks");

		int fact = factOnDate(course, date,false);
		if(fact < 0)
			return result;
		else
			result.takeValueForKey(new Integer(fact), "fact");

		if(minPlan + extraDays == 0)
			return result;
		result.takeValueForKey(new Integer(extraDays), "extraDays");
		list = Variation.variations(course, beginDate, date);
		int plus = 0;
		int minus = 0;
//		boolean verifiedOnly = SettingsReader.boolForKeyPath("ignoreUnverifiedReasons", false);
//		cal.add(Calendar.DATE, +1);
		cal.set(Calendar.HOUR_OF_DAY,13);
		long startDate = cal.getTimeInMillis();
		int verifiedOnly = SettingsBase.numericSettingForCourse(
				"ignoreUnverifiedReasons", course, ec, 0);
		if(list != null && list.count() > 0) {  // account for variations
			Enumeration enu = list.objectEnumerator();
			while (enu.hasMoreElements()) {
				Variation var = (Variation) enu.nextElement();
				if(verifiedOnly > 0 && var.reason().unverified())
					continue;
				int value = var.value().intValue();
				if(var.reason().namedFlags().flagForKey("external")) {
					if(var.date().getTime() < startDate)
						minPlan += value;
				} else { 
					if (value > 0)
						plus += value;
					else
						minus -= value;
				}
			}
			result.takeValueForKey(new Integer(plus), "plus");
			result.takeValueForKey(new Integer(minus), "minus");
			result.takeValueForKey(new Integer(plus - minus), "netChange");
		} // accont for variations
		int plan = minPlan;
		if(weekFootprint == null)
			weekFootprint = new WeekFootprint(course);
		if(extraDays == 0) {
			minPlan -= maxDev;
			weekFootprint.setDate(date);
			if(EOPeriod.Utility.compareDates(date, null) <= 0)
				weekFootprint.checkWeek(null);
		}
		result.takeValueForKey(new Integer(minPlan), "minPlan");

//		NSTimestamp refDate = new NSTimestamp(cal.getTimeInMillis());
		if(date != null && extraDays > 0) {  // calculate last week
			plan += weekFootprint.assumedTillDate(date);
		}  // calculate last week 
		result.takeValueForKey(new Integer(plan), "plan");
		fact = fact - (plus - minus);
		
		result.takeValueForKey(new Integer(fact - plan), "deviation");
		
		if("hide".equals(SettingsBase.stringSettingForCourse(
				"PlanFactWidget", course, course.editingContext()))) {
			result.takeValueForKey("grey", "styleClass");
		} else {
		if(plan  == fact) {
			result.takeValueForKey("green", "styleClass");
		} else if(fact < minPlan) {
			result.takeValueForKey("warning", "styleClass");
		} else if(fact > minPlan + maxDev) {
			result.takeValueForKey("highlight2", "styleClass");
		} else {
			result.takeValueForKey("gerade", "styleClass");
		}
		}
//		if(result.valueForKey("weekend") == Boolean.TRUE)
//			result.takeValueForKey(new Integer(plan + hours), "nextPlan");
		return result;
	}
	
	public static int factOnDate(EduCourse course, NSTimestamp date,boolean exclude) {
		NSArray list = course.lessons();
		if(list != null && list.count() > 0) {
			if (date != null) {
				if (exclude)
					date = date.timestampByAddingGregorianUnits(0, 0, -1, 0, 0,
							0);
				EOQualifier dateQual = new EOKeyValueQualifier("date",
						EOQualifier.QualifierOperatorLessThanOrEqualTo, date);
				list = EOQualifier.filteredArrayWithQualifier(list, dateQual);
			}
			if(list != null) {
				return list.count();
			}
		} else {
			return -1;
		}
		return 0;
	}
}