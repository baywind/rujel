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

import java.math.BigDecimal;
import java.text.Format;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.Timer;
import java.util.TimerTask;

import net.rujel.interfaces.EduLesson;
import net.rujel.reusables.SessionedEditingContext;
import net.rujel.reusables.SettingsReader;

public class MyUtility {
	public static NSArray numSorter = new NSArray(
			new EOSortOrdering("num",EOSortOrdering.CompareAscending));
	public static NSArray dateSorter = new NSArray(EOSortOrdering.sortOrderingWithKey(
			"date", EOSortOrdering.CompareAscending));
	// TODO : replace NSTimestampFormatter with java.text.SimpleDateFormat

	
	protected static NSNumberFormatter _numformat;
	
	public static NSNumberFormatter numberFormat() {
		if(_numformat == null) {
			_numformat = new NSNumberFormatter();
			_numformat.setDecimalSeparator(",");
			_numformat.setThousandSeparator(" ");
		}
		return _numformat;
	}
	
	public static String formatDecimal(BigDecimal value) {
		if(value == null)
			return null;
		value = value.stripTrailingZeros();
		if(value.scale() < 0)
			value = value.setScale(0);
		return numberFormat().format(value);
	}
	
	protected static Format _format;
	@SuppressWarnings("deprecation")
	public static Format dateFormat() {
		if(_format == null)
			_format = new NSTimestampFormatter(SettingsReader.stringForKeyPath(
					"ui.dateFormat","%Y-%m-%d"));
		return _format;
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
/*
	public static Integer eduYearForSession(WOSession session,String dateKey) {
		NSTimestamp today = null;
		if(dateKey != null)
			today = (NSTimestamp)session.valueForKey(dateKey);
		if(today == null) today = new NSTimestamp();
		return eduYearForDate(today);
	}*/

	public static Integer eduYearForDate(Date date) {
		Calendar gcal = Calendar.getInstance();
		if(date != null)
			gcal.setTime(date);
		int year = gcal.get(Calendar.YEAR);
		int month = gcal.get(Calendar.MONTH);
		int newYearMonth = SettingsReader.intForKeyPath("edu.newYearMonth",Calendar.JULY);
		if(month < newYearMonth) {
			 year--;
		} else if (month == newYearMonth){
			int newYearDay = SettingsReader.intForKeyPath("edu.newYearDay",1);
			if (gcal.get(Calendar.DAY_OF_MONTH) < newYearDay)
				year--;
		}
		return new Integer(year);
	}
	
	public static Integer eduYear(EOEditingContext ec) {
		Integer eduYear = null;
		if (ec instanceof SessionedEditingContext) {
			WOSession ses = ((SessionedEditingContext)ec).session();
			eduYear = (Integer)ses.valueForKey("eduYear");
		}
		if(eduYear == null)
			eduYear = eduYearForDate(null);
		return eduYear;
	}
	
	public static NSTimestamp date(EOEditingContext ec) {
		NSTimestamp date = null;
		if (ec instanceof SessionedEditingContext) {
			WOSession ses = ((SessionedEditingContext)ec).session();
			date = (NSTimestamp)ses.valueForKey("today");
			if(date == null) {
				Integer eduYear = (Integer)ses.valueForKey("eduYear");
				if(eduYear != null)
					date = MyUtility.dayInEduYear(eduYear.intValue());
			}
		}
		if(date == null)
			date = new NSTimestamp();
		return date;
	}
	
	public static NSTimestamp dayInEduYear(int eduYear) {
		Calendar cal = Calendar.getInstance();
		int year = cal.get(Calendar.YEAR);
		boolean ok = (year == eduYear  || year == eduYear +1);
		int month = cal.get(Calendar.MONTH);
		int newYearMonth = SettingsReader.intForKeyPath("edu.newYearMonth",Calendar.JULY);
		ok = ok && ((eduYear == year)?month >= newYearMonth : month <= newYearMonth);
		int newYearDay = SettingsReader.intForKeyPath("edu.newYearDay",1);
		if(ok && month == newYearMonth) {
			int day = cal.get(Calendar.DAY_OF_MONTH);
			ok = ((year == eduYear)?day >= newYearDay: day < newYearDay);
		}
		if(!ok) {
			cal.set(Calendar.MONTH, newYearMonth);
			cal.set(Calendar.DAY_OF_MONTH, newYearDay);
			if(year > eduYear) {
				cal.set(Calendar.YEAR, eduYear +1);
				cal.add(Calendar.DATE, -1);
			} else {
				cal.set(Calendar.YEAR, eduYear);
			}
		}
		return new NSTimestamp(cal.getTimeInMillis());
	}

	public static Date dateToEduYear(Date date, Integer eduYear) {
		if(!eduYear.equals(eduYearForDate(date))) {
			Calendar cal = Calendar.getInstance();
			if(date != null)
				cal.setTime(date);
			int year = eduYear.intValue();
			int month = cal.get(Calendar.MONTH);
			int newYearMonth = SettingsReader.intForKeyPath("edu.newYearMonth",Calendar.JULY);
			if(month < newYearMonth) {
				year++;
			} else if (month == newYearMonth) {
				int newYearDay = SettingsReader.intForKeyPath("edu.newYearDay",1);
				int day = cal.get(Calendar.DAY_OF_MONTH);
				if(day < newYearDay)
					year++;
			}
			cal.set(Calendar.YEAR, year);
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
/*
	public static java.util.Date yearStart(int eduYear) {
		int newYearMonth = SettingsReader.intForKeyPath("edu.newYearMonth",Calendar.JULY);
		int newYearDay = SettingsReader.intForKeyPath("edu.newYearDay",1);
		Calendar cal = new java.util.GregorianCalendar(eduYear,newYearMonth,newYearDay);
		return cal.getTime();
	}*/

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

	public static Object validateAttributeValue(String attr,Object value,String namePath,
			Class valueType,boolean notNull,int maxLenth) 
	throws NSValidation.ValidationException {
		if(namePath == null)
			namePath = "RujelInterfaces_Names.properties." + attr;
		// TODO: review validation localisation
		//String attributeName = attr.substring(attr.lastIndexOf('.') + 1);
		if(value == null) {
			if(notNull) {
				throw new NSValidation.ValidationException(String.format(
						stringForPath("Strings.messages.nullProhibit"),
						stringForPath(namePath)),value,attr);
			} else {
				return value;
			}
		}
		if(valueType != null && !(valueType.isInstance(value))){
			throw new NSValidation.ValidationException(String.format(
					stringForPath("Strings.messages.invalidValue"),
					stringForPath(namePath)),value,attr);
		}
		if(maxLenth > 0 && ((String)value).length() > maxLenth) {
			throw new NSValidation.ValidationException(String.format(
					stringForPath("Strings.messages.longString"),
					stringForPath(namePath),maxLenth),value,attr);
		}
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
		if(addTotal && total > 0)
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
		WOContext context = new WOContext(request) {
			public boolean shouldNotStorePageInBacktrackCache() {
				return true;
			}
		};//app.createContextForRequest (request);
		if(ses == null) {
			ses = context.session();
			ses.takeValueForKey(Boolean.TRUE,"dummyUser");
		}
		context.generateCompleteURLs();
		return context;
	}
	
	public static void setTime(Calendar cal, String time) {
		int hour = 0;
		int minute = 0;
		int idx = time.indexOf(':');
		if(idx < 0) {
			hour = Integer.parseInt(time);
		} else {
			hour = Integer.parseInt(time.substring(0,idx));
			time = time.substring(idx+1);
			idx = time.indexOf(':');
			if(idx < 0) {
				minute = Integer.parseInt(time);
				idx = 1;
			} else {
				minute = Integer.parseInt(time.substring(0,idx));
				idx = Integer.parseInt(time.substring(idx+1));
			}
		}
		cal.set(Calendar.HOUR_OF_DAY, hour);
		cal.set(Calendar.MINUTE, minute);
		cal.set(Calendar.SECOND, idx);
	}
		
	public static void scheduleTaskOnTime(TimerTask task, String time) {
		Timer timer = (Timer)WOApplication.application().valueForKey("timer");
		if(timer == null)
			return;
		if(time == null || task == null)
			throw new NullPointerException("Both attributes are required");
		Calendar cal = Calendar.getInstance();
		setTime(cal, time);
//		NSTimestamp moment = new NSTimestamp(cal.getTimeInMillis());
		if(System.currentTimeMillis() < cal.getTimeInMillis())
			timer.schedule(task, cal.getTime());
	}
	
    public static boolean isEvening(Date time) {
    	Calendar cal = Calendar.getInstance();
    	cal.setTime(time);
    	int eveningHour = SettingsReader.intForKeyPath("edu.eveningHour", 17);
    	return (cal.get(Calendar.HOUR_OF_DAY) >= eveningHour);
    }

	protected static final String[] CLIENT_IDENTITY_KEYS = new String[]
	                 {"x-webobjects-remote-addr", "remote_addr","remote_host","user-agent"};

	public static NSMutableDictionary clientIdentity(WORequest request) {
		NSMutableDictionary result = new NSMutableDictionary();
		Object value = null;
		for (int i = 0; i < CLIENT_IDENTITY_KEYS.length; i++) {
			value = request.headerForKey((String)CLIENT_IDENTITY_KEYS[i]);
			if(value != null && (result.count() == 0 || !result.containsValue(value)))
				result.setObjectForKey(value,CLIENT_IDENTITY_KEYS[i]);
		}
		return result;
	}

}
