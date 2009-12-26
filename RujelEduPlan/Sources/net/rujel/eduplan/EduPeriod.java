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
import net.rujel.base.MyUtility;
import net.rujel.base.SettingsBase;

import com.webobjects.foundation.*;
import com.webobjects.eocontrol.*;
import com.webobjects.appserver.WOApplication;
import com.webobjects.appserver.WOContext;

import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import com.webobjects.eoaccess.EOUtilities;

public class EduPeriod extends _EduPeriod implements EOPeriod
{

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
	
	public void validateForSave() throws NSValidation.ValidationException {
		if(begin() == null || end() == null) {
			String message = (String)WOApplication.application().valueForKeyPath(
					"strings.RujelEduPlan_EduPlan.messages.emptyDate");
			throw new NSValidation.ValidationException(message);
		}
		if(!eduYear().equals(MyUtility.eduYearForDate(begin())) ||
				!eduYear().equals(MyUtility.eduYearForDate(end()))) {
			String message = (String)WOApplication.application().valueForKeyPath(
				"strings.RujelEduPlan_EduPlan.messages.datesNotInYear");
			throw new NSValidation.ValidationException(message);
		}
		if(begin().compare(end()) >= 0) {
			String message = (String)WOApplication.application().valueForKeyPath(
					"strings.RujelEduPlan_EduPlan.messages.beginEndPeriod");
			throw new NSValidation.ValidationException(message);
		}
		String title = title();
		if(title == null) {
			String message = (String)WOApplication.application().valueForKeyPath(
					"strings.RujelEduPlan_EduPlan.messages.titleRequired");
			throw new NSValidation.ValidationException(message);
		}
		if(title.length() > 4) {
			int idx = title.indexOf(' ');
			if(idx < 0 || idx > 4 || (title.length() - idx) > 5) {
				String message = (String)WOApplication.application().valueForKeyPath(
					"strings.RujelEduPlan_EduPlan.messages.titleTooLong");
				throw new NSValidation.ValidationException(message);
			}
		}
		super.validateForSave();
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
	
	public static NSArray periodsInList(String listName,EOEditingContext ec) {
		if(listName == null)
			return null;
		NSArray list = EOUtilities.objectsMatchingKeyAndValue(ec, 
				"PeriodList", "listName", listName);
		if(list != null && list.count() > 0) {
			list = (NSArray)list.valueForKey("period");
			list = EOSortOrdering.sortedArrayUsingKeyOrderArray(list, sorter);
		}
		return list;
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
	
	public static NSArray periods(WOContext ctx) {
		Integer eduYear = (Integer)ctx.session().valueForKey("eduYear");
		EOEditingContext ec = null;
		try {
			ec = (EOEditingContext)ctx.page().valueForKey("ec");
		} catch (Exception e) {
			;
		}
		if(ec == null) {
			ec = (EOEditingContext)ctx.session().defaultEditingContext();
		}
		return periodsInYear(eduYear, ec);
	}
	
	public static NSArray periodsInYear(Number eduYear, EOEditingContext ec) {
		NSArray result = EOUtilities.objectsMatchingKeyAndValue(ec, 
				ENTITY_NAME, EDU_YEAR_KEY, eduYear);
		if(result == null || result.count() < 2)
			return result;
		return EOSortOrdering.sortedArrayUsingKeyOrderArray(result,sorter);
	}
	
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
		
	public String name() {
		if(fullName() == null)
			return title();
		return fullName();
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
	
	public String presentEduYear() {
		if(eduYear() == null)
			return null;
		int year = eduYear().intValue();
		return MyUtility.presentEduYear(year);
	}
	
	public int daysInPeriod(NSTimestamp toDate) {
		return daysInPeriod(toDate,null);
	}
	public int daysInPeriod(NSTimestamp toDate, String listName) {
		NSTimestamp begin = begin();
		NSTimestamp end = end();
		if(toDate != null){
			if(toDate.compare(begin) < 0)
				return 0;
			if(toDate.compare(end) < 0)
				end = toDate;
		}
		int days = EOPeriod.Utility.countDays(begin, end);
		if(listName == null) {
			NSArray list = EOUtilities.objectsMatchingKeyAndValue(editingContext(), 
					"PeriodList", "period", this);
			if(list != null && list.count() == 1) {
				EOEnterpriseObject pl = (EOEnterpriseObject)list.objectAtIndex(0);
				listName = (String)pl.valueForKey("listName");
			}
		}
		days -= Holiday.freeDaysInDates(begin, end, editingContext(), listName);
		return days;
	}
		
	public static int verifyList(NSArray list) {
		if(list == null || list.count() < 2)
			return 0;
		list = EOSortOrdering.sortedArrayUsingKeyOrderArray(list, EOPeriod.sorter);
		Enumeration enu = list.objectEnumerator();
		NSTimestamp lastEnd = null;
		int result = 0;
		while (enu.hasMoreElements()) {
			EOPeriod per = (EOPeriod) enu.nextElement();
			if(lastEnd != null && lastEnd.compare(per.begin()) >= 0)
				result += EOPeriod.Utility.countDays(per.begin(), lastEnd);
			lastEnd = per.end();
		}
		return result;
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
		
	public static int daysForList(String listName, NSTimestamp date, EOEditingContext ec) {
		NSArray periods = periodsInList(listName, ec);
		if(periods == null || periods.count() == 0)
			periods = defaultPeriods(ec); 
		if(periods == null || periods.count() == 0)
			return 0;
		return daysForList(listName, date, periods);
	}
	public static int daysForList(String listName, NSTimestamp date, NSArray list) {
		int sumDays = 0;
		Enumeration enu = list.objectEnumerator();
		while (enu.hasMoreElements()) {
			EduPeriod per = (EduPeriod) enu.nextElement();
			sumDays += per.daysInPeriod(date, listName);
		}
		return sumDays;
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
		
		public PeriodTab(EduPeriod period, boolean isCurrent) {
			title = period.title();
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
}
