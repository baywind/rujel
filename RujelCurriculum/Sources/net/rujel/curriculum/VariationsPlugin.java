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

import net.rujel.base.MyUtility;
import net.rujel.base.SettingsBase;
import net.rujel.eduplan.*;
import net.rujel.interfaces.EOPeriod;
import net.rujel.interfaces.EduCourse;
import net.rujel.interfaces.EduLesson;
import net.rujel.reusables.Period;

import com.webobjects.appserver.*;
import com.webobjects.eoaccess.EOUtilities;
import com.webobjects.eocontrol.EOAndQualifier;
import com.webobjects.eocontrol.EOEditingContext;
import com.webobjects.eocontrol.EOEnterpriseObject;
import com.webobjects.eocontrol.EOFetchSpecification;
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
    	
    	planFact = planFact(course, today);
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
			if(startDate == null) {
				cal.setTime(eduPer.begin());
				if(cal.get(Calendar.DAY_OF_WEEK) != weekStart) {
					cal.add(Calendar.DATE, 1);
					while(cal.get(Calendar.DAY_OF_WEEK) != weekStart)
						cal.add(Calendar.DATE, -1);
				}
				cal.set(Calendar.HOUR_OF_DAY, 0);
				startDate = new NSTimestamp(cal.getTimeInMillis());
//			} else if(eduPer.end().before(startDate)) {
//				continue;
			}
			if(perFin != null && 
					eduPer.begin().getTime() - perFin.getTime() > NSLocking.OneDay) {
				cal.setTime(perFin);
				cal.add(Calendar.DATE, 1);
				cal.set(Calendar.HOUR_OF_DAY, 0);
				perFin = new NSTimestamp(cal.getTimeInMillis());
				cal.setTime(eduPer.begin());
				cal.add(Calendar.DATE, -1);
				cal.set(Calendar.HOUR_OF_DAY, 20);
				if(cal.getTimeInMillis() > perFin.getTime()) {
					Period per = new Period.ByDates(perFin,
							new NSTimestamp(cal.getTimeInMillis()));
					holidays.addObject(per);
				}
			}
			perFin = eduPer.end();
			if(finDate != null && perFin.after(finDate))
				break;
		}
		if(finDate != null) {
			cal.setTime(finDate);
		} else {
			cal.setTime(perFin);
//			cal.add(Calendar.DATE, 1);
			cal.set(Calendar.HOUR_OF_DAY, 1);
			finDate = new NSTimestamp(cal.getTimeInMillis());
		}
		int days = EOPeriod.Utility.countDays(startDate, finDate);
		int result = days / week;
		int left = days % week;
		if(left > 0) {
			if(EOPeriod.Utility.compareDates(perFin, finDate) < 0) {
				result ++;
				cal.add(Calendar.DATE, week);
			}
			cal.add(Calendar.DATE, -left);
			finDate = new NSTimestamp(cal.getTimeInMillis());
		}
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
		int minPlan = 0;
		int plan = 0;
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
		//TODO remove this plug
		NSArray list = EduPeriod.periodsInList(listName, ec);
		if(list != null && list.count() > 0) {
			EduPeriod last = (EduPeriod)list.lastObject();
			if(date == null || date.compare(last.end()) > 0)
				date = last.end();
		}
		//
		if(date != null) {
			cal.setTime(date);
			weeks = weeks(listName, cal, ec, weekDays, weekStart);
			if(date != null)
				extraDays = EOPeriod.Utility.countDays(cal.getTime(), date) -1; 
		} else {
			weeks = weeks(listName, null, ec, weekDays, weekStart);
		}
		list = EOUtilities.objectsMatchingKeyAndValue(ec,
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
				if(date == null || date.compare(per.end()) > 0) {
// TODO 			if(hours.intValue() < 0) { // calculate plan and update hours
//					days += per.daysInPeriod(null,listName);
					plan += Math.abs(hours.intValue());
				} else if(date.compare(per.begin()) >= 0) {
					hours = (Integer)pd.valueForKey("weekly");
					cal.setTime(per.begin());
					while(cal.get(Calendar.DAY_OF_WEEK) != weekStart)
						cal.add(Calendar.DATE, -1);
					beginDate = new NSTimestamp(cal.getTimeInMillis());
					if(hours == null || hours.intValue() == 0)
						continue;
					maxDev = hours.intValue();
					int perDays = EOPeriod.Utility.countDays(beginDate, date);
					int perWeeks = perDays/weekDays;
					
					plan += perWeeks*maxDev;
				}
				/*if(maxDev == 0)
					active = 2;*/
			}
		} else {  // no details
			PlanCycle cl = (PlanCycle)course.cycle();
			maxDev = cl.weekly();
			minPlan = maxDev * weeks;
			plan = minPlan;
			/*
			if(active == 2) {
				EOQualifier[] quals = new EOQualifier[2];
				quals[0] = new EOKeyValueQualifier("period.end",
						EOQualifier.QualifierOperatorGreaterThanOrEqualTo,date);
				quals[1] = new EOKeyValueQualifier("listName",
						EOQualifier.QualifierOperatorEqual,listName);
				quals[0] = new EOAndQualifier(new NSArray(quals));
				EOFetchSpecification fs = new EOFetchSpecification("PeriodList",quals[0],null);
				fs.setFetchLimit(1);
				list = ec.objectsWithFetchSpecification(fs);
				if(list == null || list.count() == 0)
					active = 3;
			}
			if(active < 3) {
				if(active == 0 && !exclude)
					extraDays++;
			} else {
				plan = cl.hours().intValue();
			} */
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
		cal.set(Calendar.HOUR_OF_DAY,20);
		long startDate = cal.getTimeInMillis();
		int verifiedOnly = SettingsBase.numericSettingForCourse("ignoreUnverifiedReasons", course, ec, 0);
		if(list != null && list.count() > 0) {  // accont for variations
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
			plan = minPlan;
		} // accont for variations
		if(extraDays == 0)
			minPlan -= maxDev;
		result.takeValueForKey(new Integer(minPlan), "minPlan");

//		NSTimestamp refDate = new NSTimestamp(cal.getTimeInMillis());
		if(date != null && extraDays > 0) {  // calculate last week 
			cal.add(Calendar.DATE, weekDays +1);
			NSDictionary dict = Reprimand.prepareDict(new NSTimestamp(cal.getTimeInMillis()), 
					listName, ec, weekDays, weekStart);
			if(dict != null && dict.valueForKey("eduPeriod") != null) {
				EOQualifier[] quals = (EOQualifier[])dict.valueForKey("prevQualifier");
				if(quals != null) {
					int[] currWeek = new int[weekDays];
					int ref = ((Integer)dict.valueForKey("prevRef")).intValue();

					quals[2] = new EOKeyValueQualifier("course",
							EOQualifier.QualifierOperatorEqual,course);
					quals[2] = new EOAndQualifier(new NSArray(quals));
					EOFetchSpecification fs = new EOFetchSpecification(EduLesson.entityName,
							quals[2],MyUtility.dateSorter);
					list = ec.objectsWithFetchSpecification(fs);
					Reprimand.putLessons(list, ref, currWeek, -1);
					fs.setEntityName(Variation.ENTITY_NAME);
					list = ec.objectsWithFetchSpecification(fs);
					Reprimand.putVariations(list, ref, currWeek,verifiedOnly > 0, -1);
					NSArray holidays = (NSArray)dict.valueForKey("holidays");
					cal.setTimeInMillis(startDate);
					weekPlan:
					for (int i = 0; i < currWeek.length; i++) {
						if(cal.getTimeInMillis() > date.getTime())
							break;
						cal.add(Calendar.DATE, 1);
						if(currWeek[i] == 0)
							continue;
						if(holidays != null) {
							Enumeration henu = holidays.objectEnumerator();
							while (henu.hasMoreElements()) {
								Holiday hd = (Holiday) henu.nextElement();
								if(hd.contains(cal.getTime()))
									continue weekPlan;
							}

						}
						plan -= currWeek[i];
					}
					
					ref = ((Integer)dict.valueForKey("refDay")).intValue();
					quals = (EOQualifier[])dict.valueForKey("weekQualifier");
					quals[2] = new EOKeyValueQualifier("course",
							EOQualifier.QualifierOperatorEqual,course);
					quals[2] = new EOAndQualifier(new NSArray(quals));
					fs = new EOFetchSpecification(EduLesson.entityName,
							quals[2],MyUtility.dateSorter);
					list = ec.objectsWithFetchSpecification(fs);
					Reprimand.putLessons(list, ref, currWeek, 1);
					fs.setEntityName(Variation.ENTITY_NAME);
					list = ec.objectsWithFetchSpecification(fs);
					Reprimand.putVariations(list, ref, currWeek,verifiedOnly > 0, 1);
					NSMutableArray suggest = new NSMutableArray();
					cal.setTimeInMillis(startDate);
					for (int i = 0; i < currWeek.length; i++) {
						if(cal.getTimeInMillis() > date.getTime())
							break;
						cal.add(Calendar.DATE, 1);
						if(currWeek[i] != 0) {
							NSMutableDictionary sg = new NSMutableDictionary();
							sg.takeValueForKey(
									new NSTimestamp(cal.getTimeInMillis()), "date");
							sg.takeValueForKey(new Integer(currWeek[i]),"value");
							suggest.addObject(sg);
						}
					}
					if(suggest.count() > 0)
						result.takeValueForKey(suggest, "suggest");
				}
			}
		}  // calculate last week 
		result.takeValueForKey(new Integer(plan), "plan");
		fact = fact - (plus - minus);
		
		result.takeValueForKey(new Integer(fact - plan), "deviation");
		if(plan  == fact) {
			result.takeValueForKey("green", "styleClass");
		} else if(fact < minPlan) {
			result.takeValueForKey("warning", "styleClass");
		} else if(fact > minPlan + maxDev) {
			result.takeValueForKey("highlight2", "styleClass");
		} else {
			result.takeValueForKey("gerade", "styleClass");
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