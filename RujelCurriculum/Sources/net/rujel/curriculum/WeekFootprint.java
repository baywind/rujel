//WeekFootprint.java

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

import java.text.FieldPosition;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Enumeration;

import net.rujel.base.BaseLesson;
import net.rujel.base.MyUtility;
import net.rujel.base.SettingsBase;
import net.rujel.eduplan.EduPeriod;
import net.rujel.eduplan.Holiday;
import net.rujel.eduplan.PlanCycle;
import net.rujel.interfaces.EOPeriod;
import net.rujel.interfaces.EduCourse;
import net.rujel.interfaces.EduLesson;
import net.rujel.reusables.SettingsReader;
import net.rujel.schedule.ScheduleEntry;

import com.webobjects.appserver.WOApplication;
import com.webobjects.eocontrol.*;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSComparator;
import com.webobjects.foundation.NSDictionary;
import com.webobjects.foundation.NSLocking;
import com.webobjects.foundation.NSMutableArray;
import com.webobjects.foundation.NSMutableDictionary;
import com.webobjects.foundation.NSMutableSet;
import com.webobjects.foundation.NSTimestamp;

public class WeekFootprint {

	public static NSArray backSort = new NSArray(
			new EOSortOrdering("date", EOSortOrdering.CompareDescending));
	
	protected EduCourse course;
	protected EOEditingContext ec;
	protected NSTimestamp begin;
	protected NSTimestamp end;
	protected NSMutableArray[] assumed;
	protected NSMutableArray[] real;
	protected boolean[] active;
	protected EOQualifier quals[] = new EOQualifier[3];
	protected int plan;
	protected int sched = 0;
	protected int weekStart;
	
	public WeekFootprint (EduCourse forCourse) {
		super();
		course = forCourse;
		ec = forCourse.editingContext();
		quals[0] = new EOKeyValueQualifier("course", EOQualifier.QualifierOperatorEqual, course);
		weekStart = SettingsBase.numericSettingForCourse(
				"weekStart", course, ec, Calendar.MONDAY);
	}
	
	public void reset() {
		begin = null;
		end = null;
		assumed = null;
		real = null;
		active = null;
	}
	
	public void setDate(NSTimestamp date) {
		if(begin != null && EOPeriod.Utility.compareDates(begin, date) <= 0 &&
				end != null && EOPeriod.Utility.compareDates(end, date) >= 0)
			return;
    	Calendar cal = Calendar.getInstance();
    	if(date != null)
    		cal.setTime(date);
    	int week = SettingsBase.numericSettingForCourse("EduPeriod", course, ec,7);
		int day = ScheduleEntry.weekday(cal, week) - weekStart;
		if(day < 0)
			day += week;
		cal.add(Calendar.DATE, -day);
		begin = new NSTimestamp(cal.getTimeInMillis());
		cal.add(Calendar.DATE, week -1);
		end = new NSTimestamp(cal.getTimeInMillis());
		cal.add(Calendar.DATE, 1 - week);
		plan = PlanCycle.planHoursForCourseAndDate(course, date);

		assumed = new NSMutableArray[week];
		real = new NSMutableArray[week];
		NSArray list = ScheduleEntry.entriesForPeriod(course, begin, end);
		sched = 0;
		if (list != null && list.count() > 0) {
			Enumeration enu = list.objectEnumerator();
			while (enu.hasMoreElements()) {
				ScheduleEntry sdl = (ScheduleEntry) enu.nextElement();
				day = sdl.weekdayNum().intValue() - weekStart;
				date = sdl.validSince();
				if(date != null && date.after(begin) &&
					EOPeriod.Utility.countDays(begin, date) > day +1)
						continue;
				date = sdl.validTo();
				if(date != null && date.before(end) &&
						EOPeriod.Utility.countDays(begin, date) < day +1)
					continue;
				sched++;
				if(assumed[day] == null) {
					assumed[day] = new NSMutableArray((Object)new NSMutableArray(sdl));
				} else {
					NSMutableArray arr = (NSMutableArray)assumed[day].objectAtIndex(0);
					arr.addObject(sdl);
				}
			}
		} // schedule
		quals[1] = new EOKeyValueQualifier("date",
				EOQualifier.QualifierOperatorLessThanOrEqualTo, end);
		if(sched >= plan)
			quals[2] = new EOKeyValueQualifier("date",
					EOQualifier.QualifierOperatorGreaterThanOrEqualTo, begin);
		else
			quals[2] = null;
		quals[2] = new EOAndQualifier(new NSArray(quals));
		EOFetchSpecification fs = new EOFetchSpecification(BaseLesson.ENTITY_NAME,
				quals[2], backSort);
		if(sched < plan)
			fs.setFetchLimit(plan*3);
		list = ec.objectsWithFetchSpecification(fs);
		addFromList(list, cal);
		fs.setEntityName(Variation.ENTITY_NAME);
		list = ec.objectsWithFetchSpecification(fs);
		addFromList(list, cal);
		cal.setTime(begin);
		int total = 0;
		long[] dates = new long[week];
		for (int i = 0; i < assumed.length; i++) { // analyse assumed
			if(assumed[i] == null) {
				cal.add(Calendar.DATE, 1);
				continue;
			}
			NSMutableSet nums = new NSMutableSet();
			int count = 0;
			for (int j = 0; j < assumed[i].count(); j++) {
				list = (NSArray)assumed[i].objectAtIndex(j);
				if(list.count() > count)
					count = list.count();
				Enumeration enu = list.objectEnumerator();
				while (enu.hasMoreElements()) {
					EOEnterpriseObject obj = (EOEnterpriseObject) enu.nextElement();
					if(obj instanceof ScheduleEntry) {
						nums.addObject(((ScheduleEntry)obj).num());
						dates[i] = System.currentTimeMillis();
					} else if(obj instanceof BaseLesson) {
						nums.addObject(((BaseLesson)obj).number());
						if(dates[i] == 0)
							dates[i] = ((BaseLesson)obj).date().getTime();
					} else if(obj instanceof Variation) {
						int value = ((Variation)obj).value().intValue();
						count -= (value + 1);
						if(value > 0) {
							EduLesson related = ((Variation)obj).relatedLesson();
							if(related != null && related.course() == course)
								nums.removeObject(related.number());
						}
						if(dates[i] == 0)
							dates[i] = ((Variation)obj).date().getTime();
					}
				}
				if(count <= 0) {
					dates[i] = 0;
					continue;
				}
				if(nums.count() >= count)
					break;
			} // cycle prev weeks
			if(count <= 0) {
				assumed[i] = null;
			} else {
				total += count;
				if(nums.count() > 0)
					assumed[i] = new NSMutableArray(nums.allObjects());
				else
					assumed[i] = new NSMutableArray();
				if(assumed[i].count() > 1) {
					try {
						assumed[i].sortUsingComparator(NSComparator.AscendingNumberComparator);
					} catch (Exception e) {}
				}
				if(nums.count() < count) {
					Integer last = (Integer)assumed[i].lastObject();
					int next = (last == null)? 1 : last.intValue() +1;
					while (assumed[i].count() < count) {
						assumed[i].addObject(new Integer(next));
						next++;
					}
				}
			}
//			NSTimestamp adate = new NSTimestamp(cal.getTimeInMillis());
//			cal.add(Calendar.DATE, 1);
		} // process assumptions
		while (total > plan) {
			long latest = System.currentTimeMillis();
			int idx = week;
			for (int i = 0; i < week; i++) {
				if(dates[i] > 0 && dates[i] < latest) {
					latest = dates[i];
					idx = i;
				}
			}
			if(idx < week) {
				int count = assumed[idx].count();
				if(plan <= (total - count)) {
					assumed[idx] = null;
					dates[idx] = 0;
					total -= count;
				} else {
					break;
				}
			}
		}
		
		String listName = SettingsBase.stringSettingForCourse(EduPeriod.ENTITY_NAME, course, ec);
		NSArray periods = EduPeriod.periodsInList(listName, ec); 
		if(periods == null || periods.count() == 0)
			periods = EduPeriod.defaultPeriods(ec);
		if(periods != null && periods.count() > 0) {
			NSArray holidays = Holiday.holidaysInDates(begin, end, ec, listName);
			Enumeration penu = periods.objectEnumerator();
			Enumeration henu = (holidays == null || holidays.count() == 0)? null :
				holidays.objectEnumerator();
			cal.setTime(begin);
			active = new boolean[week];
			EduPeriod per = (EduPeriod)penu.nextElement();
			Holiday hd = (henu == null)?null:(Holiday)henu.nextElement();
			for (int i = 0; i < week; i++) {
				while (penu != null && EOPeriod.Utility.compareDates(
						per.end().getTime(), cal.getTimeInMillis()) < 0) {
					if(penu.hasMoreElements()) {
						per = (EduPeriod)penu.nextElement();
					} else {
						penu = null;
					}
				}
				active[i] = true;
				if(penu == null) {
					active[i] = false;
					if(real[i] == null)
						real[i] = new NSMutableArray(per);
					else
						real[i].addObject(per);
					cal.add(Calendar.DATE, 1);
					continue;
				}
				if(hd != null) {
					while(EOPeriod.Utility.compareDates(
							hd.end().getTime(),cal.getTimeInMillis()) < 0) {
						if(henu.hasMoreElements()) {
							hd = (Holiday)henu.nextElement();
						} else {
							henu = null;
							hd = null;
							break;
						}
					}
					if(hd != null && EOPeriod.Utility.compareDates(
							hd.begin().getTime(),cal.getTimeInMillis()) <= 0) {
						active[i] = false;
						if(real[i] == null)
							real[i] = new NSMutableArray(hd);
						else
							real[i].addObject(hd);
					}
				}
				cal.add(Calendar.DATE, 1);
			}
		}
	}
	
	public NSDictionary assumeNextLesson(EduLesson lesson) {
		Calendar cal = Calendar.getInstance();
		long fin = System.currentTimeMillis() + NSLocking.OneYear;
		if(lesson == null) { // assume start date
			cal.setTime(MyUtility.date(ec));
			NSArray periods = EduPeriod.periodsForCourse(course);
	    	if(periods != null && periods.count() > 0) {
	    		EduPeriod period = null;
	    		for (int i = 0; i < periods.count(); i++) {
	    			EduPeriod per = (EduPeriod)periods.objectAtIndex(i);
	    			if(EOPeriod.Utility.compareDates(
	    					per.begin().getTime(), cal.getTimeInMillis()) > 0)
	    				break;
	    			period = per;
	    			if(EOPeriod.Utility.compareDates(
	    					per.end().getTime(), cal.getTimeInMillis()) > 0)
	    				break;
				}
	    		if(period != null)
	    			cal.setTime(period.begin());
	    		period = (EduPeriod)periods.lastObject();
	    		fin = period.end().getTime();
	    	}
			setDate(new NSTimestamp(cal.getTimeInMillis()));
		} else {
			cal.setTime(lesson.date());
			setDate(lesson.date());
		}
		int day = ScheduleEntry.weekday(cal, assumed.length) - weekStart;
		if(lesson != null) {
			if(assumed[day] != null &&
					assumed[day].count() > real[day].count()) {
				Integer number = lesson.number();
				Enumeration aenu = assumed[day].objectEnumerator();
				while (aenu.hasMoreElements()) {
					Integer num = (Integer) aenu.nextElement();
					if(num.compareTo(number) > 0)
						return assumption(cal, 0, num);
				}
				
			}
			day++;
			cal.add(Calendar.DATE, 1);
		}
		if(day >= assumed.length) {
			day = 0;
			setDate(new NSTimestamp(cal.getTimeInMillis()));
		}
		while (EOPeriod.Utility.compareDates(cal.getTimeInMillis(), fin) <= 0) {
			for (int i = day; i < assumed.length; i++) {
				if(!active[i] || assumed[i] == null || assumed[i].count() == 0)
					continue;
//				if(real[i] != null && real[i].count() >= assumed[i].count())
//					continue;
				if(real[i] == null) {
					Integer num = (Integer)assumed[i].objectAtIndex(0);
					return assumption(cal, i - day, num);
				}
				NSMutableArray as = assumed[i].mutableClone();
				for (int j = 0; j < real[i].count(); j++) {
					EOEnterpriseObject rl = (EOEnterpriseObject)real[i].objectAtIndex(j);
					EduLesson lsn = null;
					if(rl instanceof EduLesson) {
						lsn = (EduLesson)rl;
					} else if(rl instanceof Variation) {
						lsn = ((Variation)rl).relatedLesson();
					}
					if(lsn != null)
						as.removeObject(lsn.number());
				}
				if(as.count() > 0)
					return assumption(cal, i - day, (Integer)as.objectAtIndex(0));
			} // cycle week
			cal.add(Calendar.DATE, active.length - day);
			day = 0;
			setDate(new NSTimestamp(cal.getTimeInMillis()));
		} // cycle weeks
		return null;
	}

	public static Integer sort = new Integer(30);
	private NSDictionary assumption(Calendar cal, int add, Integer num) {
		if(add > 0)
			cal.add(Calendar.DATE, add);
		return new NSDictionary(new Object[] {sort,
				new NSTimestamp(cal.getTimeInMillis()),num},
				new String[] {"sort","date","number"});
	}
	
	public int assumedTillDate (NSTimestamp date) {
		setDate(date);
		int count = EOPeriod.Utility.countDays(begin, date);
		int result = 0;
		for (int i = 0; i < count; i++) {
			if(active[i] && assumed[i] != null)
				result += assumed[i].count();
		}
		return result;
	}
	
	public NSMutableArray suggestVars() {
		NSMutableArray result = new NSMutableArray();
		for (int i = 0; i < active.length; i++) {
			int count = countReal(real[i]);
			if (active[i] && assumed[i] != null)
				count -= assumed[i].count();
			if(count != 0) {
				NSMutableDictionary sg = new NSMutableDictionary();
				sg.takeValueForKey(begin.timestampByAddingGregorianUnits(0, 0, i, 0, 0, 0),"date");
				sg.takeValueForKey(new Integer(count),"value");
				if(count > 0)
					sg.takeValueForKey(Boolean.TRUE, "positive");
				result.addObject(sg);
			}
		}
		return (result.count() == 0) ? null : result;
	}
	
	private static int countReal(NSArray list) {
		if(list == null || list.count() == 0)
			return 0;
		int count = 0;
		Enumeration enu = list.objectEnumerator();
		while (enu.hasMoreElements()) {
			EOEnterpriseObject obj = (EOEnterpriseObject) enu.nextElement();
			if(obj instanceof BaseLesson) {
				count++;
			} else if(obj instanceof Variation) {
				int value = ((Variation)obj).value().intValue();
				count -= value;
			}
		}
		return count;
	}
	
	public Integer checkWeek(StringBuffer buf) {
		int sum = 0;
		int known = 0;
		NSArray weekdays = (buf == null)?null:(NSArray)WOApplication.application().valueForKeyPath(
				"strings.Reusables_Strings.presets.weekdayShort");
		FieldPosition fp = (buf == null)?null: new FieldPosition(SimpleDateFormat.DATE_FIELD);
		Calendar cal = (buf == null)?null: Calendar.getInstance();
		if(cal != null) cal.setTime(begin);
		boolean hasDev = false;
		for (int i = 0; i < active.length; i++) {
			int count = countReal(real[i]);
			sum += count;
			if(buf == null)
				continue;
			if (active[i] && assumed[i] != null)
				count -= assumed[i].count();
			if(count != 0) {
				hasDev = true;
				if(buf.length() > 0)
					buf.append("\n");
				if(count > 0)
					buf.append('+');
				buf.append(count);
				buf.append(" : ");
				buf.append(weekdays.objectAtIndex(cal.get(Calendar.DAY_OF_WEEK) -1));
				buf.append(',').append(' ');
				MyUtility.dateFormat().format(new NSTimestamp(cal.getTimeInMillis()), buf, fp);
			}
			known += count;
			cal.add(Calendar.DATE, 1);
		}
		if(buf != null && known != sum - plan) {
			if(buf.length() > 0)
				buf.append("\n");
			if(sum > plan)
				buf.append('+');
			buf.append(sum - plan - known);
			buf.append(" : [");
			SimpleDateFormat fmt = new SimpleDateFormat(SettingsReader.stringForKeyPath(
					"ui.shortDateFormat","MM-dd"));
			fmt.format(begin, buf, fp);
			buf.append(" - ");
			fmt.format(end, buf, fp);
			buf.append(']');
		}
		if(sum != plan)
			return new Integer(sum - plan);
		if(hasDev)
			return new Integer(0);
		return null;
	}
	
	private NSTimestamp addFromList(NSArray list, Calendar cal) {
		if(list == null || list.count() == 0)
			return null;
		if(cal == null)
			cal = Calendar.getInstance();
		Enumeration enu = list.objectEnumerator();
		NSTimestamp date = null;
		while (enu.hasMoreElements()) {
			EOEnterpriseObject obj = (EOEnterpriseObject) enu.nextElement();
			date = (NSTimestamp)obj.valueForKey("date");
			cal.setTime(date);
			addOnDate(obj, cal);
		}
		return date;
	}
	
	private void addOnDate(EOEnterpriseObject obj, Calendar cal) {
		int day = ScheduleEntry.weekday(cal, assumed.length) - weekStart;
		if(day < 0)
			day += assumed.length;
		boolean isA = EOPeriod.Utility.compareDates(cal.getTimeInMillis(), begin.getTime()) < 0;
		NSMutableArray[] arr = (isA) ? assumed : real;
		if(arr[day] == null) {
			if(isA)
				arr[day] = new NSMutableArray((Object)new NSMutableArray(obj));
			else
				arr[day] = new NSMutableArray(obj);
		} else if(isA) {
			try {
				NSTimestamp date = (NSTimestamp)obj.valueForKey("date");
				for(int i = 0; i < arr[day].count(); i++) {
					NSMutableArray list = (NSMutableArray) arr[day].objectAtIndex(i);
					EOEnterpriseObject exist = (EOEnterpriseObject)list.objectAtIndex(0);
					if(exist instanceof ScheduleEntry)
						continue;
					try {
						NSTimestamp exDate = (NSTimestamp)exist.valueForKey("date");
						if(EOPeriod.Utility.compareDates(date, exDate) < 0)
							continue;
						if(EOPeriod.Utility.compareDates(date, exDate) > 0) {
							list = new NSMutableArray(obj);
							arr[day].insertObjectAtIndex(list, i);
						} else {
							list.addObject(obj);
						}
						return;
					} catch (Exception e) {}
				} // cycle exDates 
				arr[day].addObject(new NSMutableArray(obj));
			} catch (Exception e) {
				NSMutableArray array = (NSMutableArray)assumed[day].objectAtIndex(0);
				array.addObject(obj);
			}
		} else if(!arr[day].containsObject(obj)) {
			arr[day].addObject(obj);
		}
	}
	
	public void addObject(EOEnterpriseObject obj) {
		try {
			NSTimestamp date = (NSTimestamp)obj.valueForKey("date");
			if(date == null || EOPeriod.Utility.compareDates(date, begin) < 0 ||
					EOPeriod.Utility.compareDates(date, end) > 0)
				return;
			Calendar cal = Calendar.getInstance();
			cal.setTime(date);
			addOnDate(obj, cal);
		} catch (Exception e) {
		}
	}
}
