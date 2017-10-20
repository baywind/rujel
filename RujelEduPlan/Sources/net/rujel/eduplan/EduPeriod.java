// EduPeriod.java

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

package net.rujel.eduplan;

import net.rujel.reusables.*;
import net.rujel.interfaces.*;
import net.rujel.base.SettingsBase;

import com.webobjects.foundation.*;
import com.webobjects.eocontrol.*;
import com.webobjects.appserver.WOContext;

import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import com.webobjects.eoaccess.EOUtilities;

public class EduPeriod extends _EduPeriod implements EOPeriod
{

 	public static final NSArray sorter = new NSArray(
 			EOSortOrdering.sortOrderingWithKey("begin",EOSortOrdering.CompareAscending));

 	public EduPeriod() {
        super();
    }
	
	/*
	 // If you add instance variables to store property values you
	 // should add empty implementions of the Serialization methods
	 // to avoid unnecessary overhead (the properties will be
	 // serialized for you in the superclass).
	 private void writeObject(java.io.ObjectOutputStream out) throws java.io.IOException {
	 }
	 
	 private void readObject(java.io.ObjectInputStream in) throws java.io.IOException, java.lang.ClassNotFoundException {
	 }
	 */
	
	public boolean contains(Date date) {
		return EOPeriod.Utility.contains(this, date);
	}
	
	public EOEnterpriseObject addToList(String listName) {
		NSDictionary dict = new NSDictionary(new Object[] {this,listName},
				new String[] {"period","listName"});
		EOEditingContext ec = editingContext();
		NSArray list = EOUtilities.objectsMatchingValues(ec, "PeriodList", dict);
		if(list != null && list.count() > 0)
			return (EOEnterpriseObject)list.objectAtIndex(0);
		EOEnterpriseObject pl = EOUtilities.createAndInsertInstance(ec, "PeriodList");
		pl.takeValuesFromDictionary(dict);
		return pl;
	}
	
	public boolean removeFromList(String listName) {
		NSDictionary dict = new NSDictionary(new Object[] {this,listName},
				new String[] {"period","listName"});
		EOEditingContext ec = editingContext();
		NSArray list = EOUtilities.objectsMatchingValues(ec, "PeriodList", dict);
		if(list == null || list.count() == 0)
			return false;
		Enumeration enu = list.objectEnumerator();
		while (enu.hasMoreElements()) {
			EOEnterpriseObject pl = (EOEnterpriseObject) enu.nextElement();
			pl.removeObjectFromBothSidesOfRelationshipWithKey(this, "period");
			ec.deleteObject(pl);
		}
		return true;
	}

	public static NSArray sortedPeriods(EOEditingContext ec, String listName) {
		EOQualifier qual = new EOKeyValueQualifier(LIST_NAME_KEY,
				EOQualifier.QualifierOperatorEqual,listName);
		EOFetchSpecification fs = new EOFetchSpecification(ENTITY_NAME, qual, sorter);
		return ec.objectsWithFetchSpecification(fs);
	}
	
	public static EduPeriod[] fetchPeriods(EOEditingContext ec, String listName) {
    	NSArray found = EOUtilities.objectsMatchingKeyAndValue(ec, 
    			EduPeriod.ENTITY_NAME, EduPeriod.LIST_NAME_KEY, listName);
    	return arrangePeriods(found);
	}
    private static EduPeriod[] arrangePeriods(NSArray found) {
    	EduPeriod[] periods = new EduPeriod[found.count() +1];
		Enumeration enu = found.objectEnumerator();
		while (enu.hasMoreElements()) {
			EduPeriod per = (EduPeriod) enu.nextElement();
			EOEnterpriseObject itog = per.relatedItog();
			if(itog == null) {
				periods[0]=per;
			} else {
				Integer num = (Integer)itog.valueForKey("num");
				if(num >= periods.length) { //make larger array
					EduPeriod[] prev = periods;
					periods = new EduPeriod[num+1];
					for (int i = 0; i < prev.length; i++) {
						if(prev[i] != null) {
							periods[i]=prev[i];
						}
					}
				}
				periods[num.intValue()]=per;
			}
		}
		EduPeriod next=periods[0];
		if(next == null) {
			next = (EduPeriod)found.objectAtIndex(0);
			EduPlan.logger.log(WOLogLevel.WARNING, "No end EduPeriod found for listName "
						+ next.listName(),next.valueForKeyPath("relatedItog.itogType"));
			return periods;
		}
		next._next=next;
		next._perlist=periods;
		for (int i = periods.length-1; i > 0 ; i--) {
			if(periods[i]!=null) {
				periods[i]._next=next;
				periods[i]._perlist=periods;
				next = periods[i];
			}
		}
    	return periods;
	}
	
	public static NSArray periodsInList(String listName,EOEditingContext ec) {
		return periodsInList(listName, ec, true);
	}
	public static NSArray periodsInList(String listName,EOEditingContext ec, boolean noEnd) {
		if(listName == null)
			return null;
    	NSArray found = EOUtilities.objectsMatchingKeyAndValue(ec, 
    			EduPeriod.ENTITY_NAME, EduPeriod.LIST_NAME_KEY, listName);
    	if(found == null || found.count() == 0)
    		return null;
    	EduPeriod tmp = (EduPeriod)found.objectAtIndex(0);
		EduPeriod[] periods = tmp._perlist;
		if (periods == null)
			periods = arrangePeriods(found);
		if(noEnd) {
			tmp = periods[0];
			periods[0] = null;
			NSArray result = new NSArray(periods);
			periods[0]=tmp;
			return result;
		} else {
			return new NSArray(periods);
		}
	}
		
	public static NSArray defaultPeriods(EOEditingContext ec) {
		String listName = SettingsBase.stringSettingForCourse(ENTITY_NAME, null, ec);
		return periodsInList(listName, ec);
	}
	
	public static NSArray periodsForCourse(EduCourse course) {
		EOEditingContext ec = course.editingContext();
		String listName = SettingsBase.stringSettingForCourse(ENTITY_NAME, course, ec);
		NSArray list = periodsInList(listName, ec); 
		if(list == null || list.count() == 0)
			list = defaultPeriods(ec); 
		return list;
	}
	
	public static final NSArray<EOSortOrdering> grouper = new NSArray(new EOSortOrdering[]{
			new EOSortOrdering(LIST_NAME_KEY, EOSortOrdering.CompareAscending),
			new EOSortOrdering(BEGIN_KEY, EOSortOrdering.CompareAscending)});
	public static NSArray periods(WOContext ctx) {
//		Integer eduYear = (Integer)ctx.session().valueForKey("eduYear");
		EOEditingContext ec = null;
		try {
			ec = (EOEditingContext)ctx.page().valueForKey("ec");
		} catch (Exception e) {
			;
		}
		if(ec == null) {
			ec = (EOEditingContext)ctx.session().defaultEditingContext();
		}
		EOFetchSpecification fs = new EOFetchSpecification(ENTITY_NAME, null, grouper);
		return ec.objectsWithFetchSpecification(fs);
	}
	/*
	public static NSArray periodsInYear(Number eduYear, EOEditingContext ec) {
		NSArray result = EOUtilities.objectsMatchingKeyAndValue(ec, 
				ENTITY_NAME, EDU_YEAR_KEY, eduYear);
		if(result == null || result.count() < 2)
			return result;
		return EOSortOrdering.sortedArrayUsingKeyOrderArray(result,sorter);
	}*/
	
	public static EduPeriod getCurrentPeriod(NSTimestamp date, String listName,
			EOEditingContext ec) {
		if(listName == null)
			listName = SettingsBase.stringSettingForCourse(ENTITY_NAME, null, ec);
		EOQualifier[] quals = new EOQualifier[3];
		quals[0] = new EOKeyValueQualifier("period.begin",
				EOQualifier.QualifierOperatorLessThanOrEqualTo,date);
		quals[1] = new EOKeyValueQualifier("period.end",
				EOQualifier.QualifierOperatorGreaterThanOrEqualTo,date);
		quals[2] = new EOKeyValueQualifier("listName",
				EOQualifier.QualifierOperatorEqual,listName);
		quals[0] = new EOAndQualifier(new NSArray(quals));
		EOFetchSpecification fs = new EOFetchSpecification("PeriodList",quals[0],null);
		NSArray list = ec.objectsWithFetchSpecification(fs);
		if(list == null || list.count() == 0)
			return null;
		EOEnterpriseObject pl = (EOEnterpriseObject)list.objectAtIndex(0);
		return (EduPeriod)pl.valueForKey("period");
	}
	
	public EOEnterpriseObject relatedItog() {
		return (EOEnterpriseObject)storedValueForKey("relatedItog");
	}
	
	public String name() {
		return (String)valueForKeyPath("relatedItog.name");
	}

	public String title() {
		return (String)valueForKeyPath("relatedItog.title");
	}
	
	public Number sort() {
		return new Integer(code());
	}
	
	public int code() {
		Calendar cal = Calendar.getInstance();
		cal.setTime(begin());
		int result = cal.get(Calendar.YEAR);
		result -= 2000;
		result = result*10000;
		result += cal.get(Calendar.MONTH)*100;
		result += cal.get(Calendar.DAY_OF_MONTH);
		result = (result+1)*1000;
		result -= EOPeriod.Utility.countDays(begin(), end());
		return result;
	}
	/*
	public String presentEduYear() {
		if(eduYear() == null)
			return null;
		int year = eduYear().intValue();
		return MyUtility.presentEduYear(year);
	}*/
	
	public int daysInPeriod(NSTimestamp toDate) {
		NSTimestamp begin = begin();
		NSTimestamp end = end();
		if(toDate != null){
			if(toDate.compare(begin) < 0)
				return 0;
			if(toDate.compare(end) < 0)
				end = toDate;
		}
		int days = EOPeriod.Utility.countDays(begin, end);
		days -= Holiday.freeDaysInDates(begin, end, editingContext(), listName());
		return days;
	}
	
	public static int activeDaysInDates(NSTimestamp begin, NSTimestamp end, String listName, EOEditingContext ec) {
		NSArray periods = sortedPeriods(ec, listName);
		NSArray holidays = Holiday.holidaysInDates(begin, end, ec, listName);
		if(periods == null || periods.count() == 0) {
			String tmpListName = SettingsBase.stringSettingForCourse(ENTITY_NAME, null, ec);
			periods = sortedPeriods(ec, tmpListName);
		}
		if(periods == null || periods.count() == 0)
			return 0;
		EduPeriod first = (EduPeriod)periods.objectAtIndex(0);
		EduPeriod last = (EduPeriod)periods.lastObject();
		NSTimestamp perlast = last.begin().timestampByAddingGregorianUnits(0, 0, -1, 0, 0, 0);
		if(begin == null || begin.before(first.begin()))
			begin = first.begin();
		if(end == null || end.after(perlast))
			end = perlast;
		return EOPeriod.Utility.countDays(begin, end) 
				- Holiday.freeDaysInDates(begin, end, holidays);
	}
	
/*	public int[] weeksAndDays(NSTimestamp toDate, EduCourse course) {
		EOEditingContext ec = editingContext();
		String listName = SettingsBase.stringSettingForCourse(ENTITY_NAME, course, ec);
		int days = daysInPeriod(toDate, listName);
		int weekDays = SettingsBase.numericSettingForCourse("weekDays", course, ec, 7);
		int[] result = new int[2];
		result[0] = days/weekDays;
		result[1] = days%weekDays;
		return result;
	}*/
		
	public static int daysForList(String listName, NSTimestamp toDate, EOEditingContext ec) {
		NSArray periods = sortedPeriods(ec, listName);
		if(periods == null || periods.count() == 0)
			periods = defaultPeriods(ec); 
		if(periods == null || periods.count() == 0)
			return 0;
		EduPeriod per = (EduPeriod)periods.objectAtIndex(0);
		NSTimestamp begin = per.begin();
		per = (EduPeriod)periods.lastObject();
		NSTimestamp end = per.begin();
		int days=-1;
		if(toDate != null){
			if(toDate.compare(begin) < 0)
				return 0;
			if(toDate.compare(end) < 0) {
				end = toDate;
				days =0;
			}
		}
		days += EOPeriod.Utility.countDays(begin, end);
		days -= Holiday.freeDaysInDates(begin, end, ec, listName);
		return days;
	}

	
	public static int dayState(NSTimestamp date, EOEditingContext ec, String listName) {
		EduPeriod current = getCurrentPeriod(date, listName, ec);
		if(current == null)
			return 2;
		NSArray list = Holiday.holidaysInDates(date, date, ec, listName);
		if(list != null && list.count() > 0)
			return 1;
		return 0;
	}

	protected static class PeriodTab implements Tabs.GenericTab {
		protected String title;
		protected String hover;
		protected EOQualifier qual;
		protected boolean current;
		protected int code;
		protected Period per;
		
		public PeriodTab(EduPeriod period, boolean isCurrent) {
			title = (String)period.valueForKeyPath("relatedItog.title");
			code = period.code();
/*			try {
				EOKeyGlobalID gid = (EOKeyGlobalID)period.editingContext().
						globalIDForObject(period);
				Integer key = (Integer)gid.keyValues()[0];
				code = key.intValue();
			} catch (Exception e) {
				code = 0;
			}*/
			current = isCurrent;
			NSMutableArray quals = new NSMutableArray();
			quals.addObject(new EOKeyValueQualifier
					("date",EOQualifier.QualifierOperatorGreaterThanOrEqualTo,period.begin()));
			quals.addObject(new EOKeyValueQualifier
					("date",EOQualifier.QualifierOperatorLessThanOrEqualTo,period.end()));
			qual = new EOAndQualifier(quals);
			hover = period.name();
			per = new EOPeriod.ByDates(period.begin(), period.end());
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
		public Period period() {
			return per;
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

	protected EduPeriod[] _perlist;
	protected EduPeriod _next;
	public NSArray allInList() {
		if(_perlist == null) {
			_perlist = fetchPeriods(editingContext(),listName());
		}
		return new NSArray(_perlist);
	}
	
	public EduPeriod next() {
		if(_next==null)
			fetchPeriods(editingContext(), listName());
		return _next;
		/*
		Integer num = (Integer)valueForKeyPath("relatedItog.num");
		if(num == null)
			return this;
		if(_perlist==null)
			_perlist = fetchPeriods(editingContext(), listName());
		int idx=num+1;
		EduPeriod next = null;
		while (next == null && _perlist.length > idx) {
			next = _perlist[idx];
			idx++;
		}
		if(next == null)
			next = _perlist[0];
		if(next == null)
			next = this;
		return next;*/
	}
	
	public NSTimestamp end() {
		return next().begin().timestampByAddingGregorianUnits(0, 0, -1, 0, 0, 0);
	}
	
	public static NSTimestamp[] defaultYearDates(int eduYear) {
		String start = SettingsReader.stringForKeyPath("edu.yearStart", null);
		String end = SettingsReader.stringForKeyPath("edu.yearEnd", null);
		Calendar cal = Calendar.getInstance();
		NSTimestamp[] result = new NSTimestamp[2];
		if(start == null) {
			cal.set(Calendar.MONTH, Calendar.SEPTEMBER);
			cal.set(Calendar.DAY_OF_MONTH, 1);
			cal.set(Calendar.YEAR, eduYear);
			result[0] = new NSTimestamp(cal.getTimeInMillis());
			if(end == null) {
				cal.set(Calendar.MONTH, Calendar.MAY);
				cal.set(Calendar.DAY_OF_MONTH, 31);
				cal.add(Calendar.YEAR, 1);
				result[1] = new NSTimestamp(cal.getTimeInMillis());
				return result;
			}
		}
		Format df = new SimpleDateFormat(
				SettingsReader.stringForKeyPath("ui.shortDateFormat","MM/dd"));
		if(start != null) {
			Date aDate = (Date)df.parseObject(start, new java.text.ParsePosition(0));
			cal.setTime(aDate);
			cal.set(Calendar.YEAR, eduYear);
			result[0] = new NSTimestamp(cal.getTimeInMillis());			
		}
		int startDay = cal.get(Calendar.DAY_OF_YEAR);
		if(end == null) {
			cal.set(Calendar.MONTH, Calendar.MAY);
			cal.set(Calendar.DAY_OF_MONTH, 31);
		} else {
			Date aDate = (Date)df.parseObject(start, new java.text.ParsePosition(0));
			cal.setTime(aDate);
			cal.set(Calendar.YEAR, eduYear);			
		}
		if(cal.get(Calendar.DAY_OF_YEAR) < startDay)
			cal.add(Calendar.YEAR, 1);
		result[1] = new NSTimestamp(cal.getTimeInMillis());
		return result;
	}
	
	public void turnIntoFault(EOFaultHandler handler) {
		_perlist=null;
		_next=null;
		super.turnIntoFault(handler);
	}
}
