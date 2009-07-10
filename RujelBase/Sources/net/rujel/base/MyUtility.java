//MyUtility.java

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

package net.rujel.base;

import com.webobjects.eoaccess.EOUtilities;
import com.webobjects.eocontrol.EOEditingContext;
import com.webobjects.eocontrol.EOSortOrdering;
import com.webobjects.foundation.*;
import com.webobjects.appserver.WOApplication;
import com.webobjects.appserver.WOContext;
import com.webobjects.appserver.WORequest;
import com.webobjects.appserver.WOSession;

import java.text.Format;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.GregorianCalendar;

import net.rujel.interfaces.EduLesson;
import net.rujel.reusables.SettingsReader;

public class MyUtility {
	public static NSArray numSorter = new NSArray(
			new EOSortOrdering("num",EOSortOrdering.CompareAscending));
	public static NSArray dateSorter = new NSArray(EOSortOrdering.sortOrderingWithKey(
			"date", EOSortOrdering.CompareAscending));

	// TODO : replace NSTimestampFormatter with java.text.SimpleDateFormat

	@SuppressWarnings("deprecation")
	public static Format dateFormat() {
		return new NSTimestampFormatter(SettingsReader.stringForKeyPath("ui.dateFormat","%Y-%m-%d"));
	}
	
	public static NSTimestamp parseDate(String dateString) {
		if(dateString == null)
			return null;
		try {
			Object result = dateFormat().parseObject(dateString);
			if(result instanceof NSTimestamp)
				return (NSTimestamp)result;
			if(result instanceof Date)
				return new NSTimestamp((Date)result);
		} catch (ParseException e) {
			return null;
		}
		return null;
	}

	public static Integer eduYearForSession(WOSession session,String dateKey) {
		NSTimestamp today = null;
		if(dateKey != null)
			today = (NSTimestamp)session.valueForKey(dateKey);
		if(today == null) today = new NSTimestamp();
		return eduYearForDate(today);
	}

	public static Integer eduYearForDate(Date date) {
		if (date == null) return null;
		GregorianCalendar gcal = new GregorianCalendar();
		gcal.setTime(date);
		int year = gcal.get(GregorianCalendar.YEAR);
		int month = gcal.get(GregorianCalendar.MONTH);
		int newYearMonth = SettingsReader.intForKeyPath("edu.newYearMonth",GregorianCalendar.JULY);
		if(month < newYearMonth) {
			 year--;
		} else if (month == newYearMonth){
			int newYearDay = SettingsReader.intForKeyPath("edu.newYearDay",1);
			if (gcal.get(GregorianCalendar.DAY_OF_MONTH) < newYearDay)
				year--;
		}
		return new Integer(year);
	}

	public static Date dateToEduYear(Date date, Integer eduYear) {
		if(!eduYear.equals(eduYearForDate(date))) {
			GregorianCalendar cal = new GregorianCalendar();
			cal.setTime(date);
			int year = eduYear.intValue();
			int month = cal.get(GregorianCalendar.MONTH);
			int newYearMonth = SettingsReader.intForKeyPath("edu.newYearMonth",GregorianCalendar.JULY);
			if(month < newYearMonth) {
				year++;
			} else if (month == newYearMonth) {
				int newYearDay = SettingsReader.intForKeyPath("edu.newYearDay",1);
				int day = cal.get(GregorianCalendar.DAY_OF_MONTH);
				if(day < newYearDay)
					year++;
			}
			cal.set(GregorianCalendar.YEAR, year);
			return cal.getTime();
		}
		return date;
	}

	public static String presentEduYear(int year) {
		StringBuffer buf = new StringBuffer(Integer.toString(year));
		buf.append('/');
		year = (year % 100) + 1;
		if(year < 10)
			buf.append('0');
		buf.append(year);
		return buf.toString();
	}

	public static java.util.Date yearStart(int eduYear) {
		int newYearMonth = SettingsReader.intForKeyPath("edu.newYearMonth",GregorianCalendar.JULY);
		int newYearDay = SettingsReader.intForKeyPath("edu.newYearDay",1);
		GregorianCalendar cal = new GregorianCalendar(eduYear,newYearMonth,newYearDay);
		return cal.getTime();
	}

	public static NSTimestamp validateDateInEduYear(Object aDate, Integer eduYear, String key) 
	throws NSValidation.ValidationException {
		NSTimestamp date = null;
		if(aDate instanceof NSTimestamp) {
			date = (NSTimestamp)aDate;
		} else if(aDate instanceof Date) {
			date = new NSTimestamp((Date)aDate);
		} else if(aDate instanceof String) {
			try {
				date = (NSTimestamp)MyUtility.dateFormat().parseObject(
						(String)aDate, new java.text.ParsePosition(0));
			} catch (Exception e) {
				throw new NSValidation.ValidationException(
						"Could not parse string to date",aDate,key);
			}
		}
		if(date == null)
			throw new NSValidation.ValidationException(
					"Null value or could not coerce",aDate,key);
		if(eduYear != null && !eduYear.equals(MyUtility.eduYearForDate(date))) {
			String message = (String)WOApplication.application().valueForKeyPath(
			"strings.RujelBase_Base.notInEduYear");
			if(message == null)
				message = "Date is not in a eduYear";
			throw new NSValidation.ValidationException(message,aDate,key);
		}
		return date;
	}

	public static String stringForPath(String path) {
		return (String)WOApplication.application().valueForKeyPath("strings." + path);
	}

	public static Object validateAttributeValue(String attr,Object value,
			Class valueType,boolean notNull,int maxLenth) 
	throws NSValidation.ValidationException {
		// TODO: review validation localisation
		//String attributeName = attr.substring(attr.lastIndexOf('.') + 1);
		if(value == null) {
			if(notNull)
				throw new NSValidation.ValidationException(String.format(
						stringForPath("Strings.messages.nullProhibit"),
						stringForPath("RujelInterfaces_Names.properties." + attr)),value,attr);
			else
				return value;
		}
		if(valueType != null && !(valueType.isInstance(value)))
			throw new NSValidation.ValidationException(String.format(
					stringForPath("Strings.messages.invalidValue"),
					stringForPath("RujelInterfaces_Names.properties." + attr)),value,attr);

		if(maxLenth > 0 && ((String)value).length() > maxLenth)
			throw new NSValidation.ValidationException(String.format(
					stringForPath("Strings.messages.longString"),
					stringForPath("RujelInterfaces_Names.properties." + attr),maxLenth),value,attr);

		return value;
	}

	public static Integer setNumberToNewLesson(EduLesson currLesson) {
		EOEditingContext ec = currLesson.editingContext();
		NSMutableArray allLessons = EOUtilities.
		objectsMatchingKeyAndValue(ec, currLesson.entityName(),
				"course", currLesson.course()).mutableClone();
		if(allLessons != null && allLessons.count() > 0) {
			allLessons.removeIdenticalObject(currLesson);
			EOSortOrdering.sortArrayUsingKeyOrderArray(allLessons, EduLesson.sorter);
		}
		if(allLessons == null || allLessons.count() == 0) {
			Integer num = new Integer(1);
			currLesson.setNumber(num);
			return num;
		}
		int idx = allLessons.count() + 1;
		boolean inProgress = true;
		for (int i = idx-2; i >= 0; i--) {
			EduLesson lesson = (EduLesson)allLessons.objectAtIndex(i);
			if(inProgress && lesson.date().compare(currLesson.date()) <= 0) {
				currLesson.setNumber(idx);
				inProgress = false;
				idx--;
			}
			if(lesson.number().intValue() != idx)
				lesson.setNumber(idx);
			idx--;
		}
		Integer num = new Integer(idx);
		if(inProgress) 
			currLesson.setNumber(num);
		return num;
	}
	
	public static NSArray filterByGroup(NSArray list, String key, 
													NSArray group, boolean addTotal) {
		if(list == null || list.count() == 0)
			return list;
		int total = group.count();
		NSMutableArray result = new NSMutableArray();
		Enumeration enu = list.objectEnumerator();
		while (enu.hasMoreElements()) {
			Object obj = enu.nextElement();
			if(!group.containsObject(NSKeyValueCoding.Utility.valueForKey(obj, key)))
				continue;
			result.addObject(obj);
			total--;
		}
		if(total > 0)
			result.addObject(new Integer(total));
		return result;
	}
	
	public static WOContext dummyContext(WOSession ses) {
		WOApplication app = WOApplication.application();
		String dummyUrl = app.cgiAdaptorURL() + "/" + app.name() + ".woa/wa/dummy";
		if(ses != null) {
			dummyUrl = dummyUrl + "?wosid=" + ses.sessionID();
		}
		WORequest request = app.createRequest( "GET", dummyUrl, "HTTP/1.0", null, null, null);
		WOContext context = app.createContextForRequest (request);
		if(ses == null) {
			ses = context.session();
			ses.takeValueForKey(Boolean.TRUE,"dummyUser");
		}
		context.generateCompleteURLs();
		return context;
	}
	
	public static int countDays(Date begin, Date end) {
		Calendar cal1 = Calendar.getInstance();
		cal1.setTime(begin);
		Calendar cal2 = Calendar.getInstance();
		cal2.setTime(end);
		int days = cal2.get(Calendar.DAY_OF_YEAR) - cal1.get(Calendar.DAY_OF_YEAR);
		while (cal1.get(Calendar.YEAR) < cal2.get(Calendar.YEAR)) {
			days += cal1.getActualMaximum(Calendar.DAY_OF_YEAR);
			cal1.add(Calendar.YEAR, 1);
		}
		return days +1;
	}
}
