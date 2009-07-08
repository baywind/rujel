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
	
	public int countInYear() {
		PeriodType type = periodType();
		if(type != null && type.inYearCount() != null)
			return type.inYearCount().intValue();
		return 0;
	}
	
	public boolean contains(Date date) {
		return (date.compareTo(begin()) >= 0 && date.compareTo(end()) <= 0);
	}
	
	public String typeID() {
		return "EduPeriod." + periodType().name();
	}
	
	public EduPeriod nextPeriod() {
		Number year = eduYear();
		Number num = num();
		if(year == null || num == null || num.intValue() >= countInYear())
			return null;
		NSMutableDictionary attrs = new NSMutableDictionary(year,"eduYear");
		Integer nextNum = new Integer(num.intValue() + 1);
		attrs.setObjectForKey(nextNum,"num");
		attrs.setObjectForKey(periodType(),"periodType");
		try {
			return (EduPeriod)EOUtilities.objectMatchingValues(editingContext(),"EduPeriod",attrs);
		} catch (Exception ex) {
			if(ex instanceof EOUtilities.MoreThanOneException) {
				/// log
			}
			return null;
		}
	}
	
	public Number validateNum(Object aValue) throws NSValidation.ValidationException {
		Integer numberValue;
		if (aValue instanceof String) {
			// Convert the String to an Integer.
			try {
				numberValue = new Integer((String)aValue);
			} catch (NumberFormatException numberFormatException) {
				throw new NSValidation.ValidationException("Validation exception: Unable to convert the String " + aValue + " to an Integer");
			}
		} else if (aValue instanceof Number) {
			numberValue = new Integer(((Number)aValue).intValue());
		} else {
			throw new NSValidation.ValidationException("Validation exception: Unable to convert the Object " + aValue + " to an Integer");
		}
		
		int num = numberValue.intValue();
		if (num <= 0 || num > countInYear()) {
			String message = String.format((String)WOApplication.application().valueForKeyPath("strings.RujelEduResults_EduResults.invalidPeriodNum"),periodType().inYearCount());
			throw new NSValidation.ValidationException(message, this, "num");
		}
		return numberValue;
	}
	
	public void validateForSave() throws NSValidation.ValidationException {
		super.validateForSave();
		if(begin().compare(end()) >= 0) {
			String message = (String)WOApplication.application().valueForKeyPath("strings.RujelEduResults_EduResults.beginEndPeriod");
			throw new NSValidation.ValidationException(message);
		}
	}
	
	/*
	 public static EduPeriod currentPeriodOfType(PeriodType type, NSTimestamp date, EOEditingContext ec) {
		 EOQualifier qual = new EOKeyValueQualifier("periodType",EOQualifier.QualifierOperatorEqual,type);
		 NSMutableArray quals = new NSMutableArray(qual);
		 qual = new EOKeyValueQualifier("begin",EOQualifier.QualifierOperatorLessThanOrEqualTo,date);
		 quals.addObject(qual);
		 qual = new EOKeyValueQualifier("end",EOQualifier.QualifierOperatorGreaterThanOrEqualTo,date);
		 quals.addObject(qual);
		 qual = new EOAndQualifier(quals);
		 EOFetchSpecification fspec = new EOFetchSpecification("EduPeriod",qual,null);
		 NSArray result = ec.objectsWithFetchSpecification(fspec);
		 if(result == null || result.count() == 0) return null;
		 if(result.count() > 1) {
			 /// log
		 }
		 return (EduPeriod)result.objectAtIndex(0);
	 }
	 public static EduPeriod currentPeriodOfType(PeriodType type, EOEditingContext ec) {
		 NSTimestamp today = null;
		 if(ec instanceof SessionedEditingContext) {
			 today = (NSTimestamp)((SessionedEditingContext)ec).session().valueForKey("today");
		 }
		 if(today == null) today = new NSTimestamp();
		 
		 return currentPeriodOfType(type,today,ec);
	 }*/
	
	public static NSArray periodsInList(String listName,EOEditingContext ec) {
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
		if(ec == null)
			ec = EOSharedEditingContext.defaultSharedEditingContext();
		return periodsInYear(eduYear, ec);
	}
	
	public static NSArray periodsInYear(Number eduYear, EOEditingContext ec) {
		NSArray result = EOUtilities.objectsMatchingKeyAndValue(ec, 
				ENTITY_NAME, EDU_YEAR_KEY, eduYear);
		if(result == null || result.count() < 2)
			return result;
		return EOSortOrdering.sortedArrayUsingKeyOrderArray(result,sorter);
	}
	
	public static EduPeriod getCurrentPeriod(NSTimestamp moment, EduCourse course,
			EOEditingContext ec) {
		NSArray periods = null;
		if(course == null)
			periods = defaultPeriods(ec);
		else
			periods = periodsForCourse(course);
		Enumeration enu = periods.objectEnumerator();
		while (enu.hasMoreElements()) {
			EduPeriod per = (EduPeriod) enu.nextElement();
			if(per.contains(moment))
				return per;
		}
		return null;
	}
	
	public String title() {
		if(countInYear() > 1) {
			return Various.makeRoman(num().intValue()) + ' ' + periodType().title();
		}
		return periodType().title();
	}
	
	public String name() {
		if(countInYear() > 1) {
			return Various.makeRoman(num().intValue()) + ' ' + periodType().name();
		}
		return periodType().name();
	}

	/*
	public Number number() {
		if(lesNum == null)
			return num();
		return lesNum;
	}
	public void setNumber(Number newNumber) {
		lesNum = newNumber;
	}
	
	public NSTimestamp date() {
		return end();
	}
	public void setDate(NSTimestamp newDate) {
		throw new UnsupportedOperationException("You can't change date here");
	}
	
	public void setTitle(String newTitle) {
		throw new UnsupportedOperationException("You can't change title here");
	}	
	
	public String theme() {
		return periodType().name();
	}
	public void setTheme(String newTheme) {
		throw new UnsupportedOperationException("You can't change theme here");
	}
	
	
	public String homeTask() {
		return null;
	}
	public void setHomeTask(String newTask) {
		throw new UnsupportedOperationException("Hometask is unavalable for itogs");
	}	
	
	public NSArray notes() {
		if(itogs == null) {
			if(course == null)
				throw new IllegalStateException("Course is not set");
			itogs = ItogMark.getItogMarks(course.cycle(),this,null);
		}
		return itogs;
	}
	public NSArray students() {
		return course.groupList();
	}
	
	public String noteForStudent(Student student) {
		return itogForStudent(student).comment();
	}
	public void setNoteForStudent(String note, Student student) {
		itogForStudent(student).setComment(note);
	}
	*/
		
	public Number sort() {
		return new Integer (100*(100 - countInYear()) + (num().intValue()));
	}
	
	public int code() {
		return 10000*eduYear().intValue() + sort().intValue();
	}
	
	public String presentEduYear() {
		if(eduYear() == null)
			return null;
		int year = eduYear().intValue();
		return MyUtility.presentEduYear(year);
	}
	
	public int daysInPeriod(NSTimestamp toDate) {
		Calendar begin = Calendar.getInstance();
		begin.setTime(begin());
		Calendar end = Calendar.getInstance();
		end.setTime(end());
		end.add(Calendar.DATE, 1);
		if(toDate != null){
			if(toDate.getTime() < begin.getTimeInMillis())
				return 0;
			if(toDate.getTime() < end.getTimeInMillis())
				end.setTime(toDate);
		}
		int day = end.get(Calendar.DAY_OF_YEAR) - begin.get(Calendar.DAY_OF_YEAR);
		while (begin.get(Calendar.YEAR) < end.get(Calendar.YEAR)) {
			day += begin.getActualMaximum(Calendar.DAY_OF_YEAR);
			begin.add(Calendar.YEAR, 1);
		}
		return day;
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
}
