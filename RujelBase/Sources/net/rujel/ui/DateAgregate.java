// DateAgregate.java

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

package net.rujel.ui;

import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;

import net.rujel.interfaces.EduCourse;
import net.rujel.interfaces.EduLesson;
import net.rujel.interfaces.PerPersonLink;
import net.rujel.reusables.SettingsReader;

import com.webobjects.eocontrol.EOSortOrdering;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSMutableArray;
import com.webobjects.foundation.NSMutableDictionary;
import com.webobjects.foundation.NSTimestamp;

public class DateAgregate {

	protected NSMutableDictionary[] array;
	protected boolean[] initialized;
	protected NSTimestamp beginDate;
	protected int beginDay;
	protected EduCourse forCourse;
	public Date begin;
	public Date end;
	
	public DateAgregate(EduCourse course) {
		forCourse = course;
		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.YEAR, course.eduYear().intValue());
		cal.set(Calendar.MONTH,SettingsReader.intForKeyPath("edu.newYearMonth",Calendar.JULY));
		cal.set(Calendar.DAY_OF_MONTH, SettingsReader.intForKeyPath("edu.newYearDay",1));
		beginDate = new NSTimestamp(cal.getTimeInMillis());
		beginDay = cal.get(Calendar.DAY_OF_YEAR);
		array = new NSMutableDictionary[cal.getActualMaximum(Calendar.DAY_OF_YEAR)];
		initialized = new boolean[array.length];
	}
	
	public int dateIndex(Date date) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		int result = cal.get(Calendar.DAY_OF_YEAR) - beginDay;
//		if(result >= array.length)
//			return array.length -1;
		if(result < 0)
			result += array.length;
//		if(result < 0)
//			return 0;
		return result;
	}
	
	public NSArray listInDates() {
		int fin = (end == null)?array.length -1: dateIndex(end);
		NSMutableArray result = new NSMutableArray();
		for (int i = (begin==null)?0:dateIndex(begin); i <= fin; i++) {
    		if(!initialized[i])
    			return null;
			if(array[i] != null)
				result.addObject(new PerPersonLink.Dictionary(array[i].mutableClone()));
		}
		return result;
	}
	
	public NSArray listForMask(NSArray lessons, NSArray views) {
    	Enumeration enu = lessons.objectEnumerator();
    	PerPersonLink.Dictionary[] out = new PerPersonLink.Dictionary[array.length];
    	while (enu.hasMoreElements()) {
    		EduLesson lesson = (EduLesson) enu.nextElement();
    		int day = dateIndex(lesson.date());
    		if(!initialized[day])
    			return null;
			if(out[day] == null) {
				if(array[day] == null) {
					out[day] = new PerPersonLink.Dictionary(
						new NSMutableDictionary(lesson.date(),"date"));
				} else {
					out[day] = new PerPersonLink.Dictionary(array[day].mutableClone());
					out[day].takeValueForKey(lesson.date(), "date");
					if(views != null && views.count() > 0) {
						Enumeration venu = views.objectEnumerator();
						int span = 0;
						while (venu.hasMoreElements()) {
							String key = (String) venu.nextElement();
							Object view = array[day].valueForKey(key);
							if(view instanceof Object[])
								span += ((Object[])view).length;
						}
						if(span > 0)
							out[day].takeValueForKey(new Integer(span), "span");
					}
				}
				out[day].takeValueForKey(new NSMutableArray(lesson), "lessons");
			} else {
				NSMutableArray related = (NSMutableArray)out[day].valueForKey("lessons");
				if(related.containsObject(lesson))
					continue;
				related.addObject(lesson);
				EOSortOrdering.sortArrayUsingKeyOrderArray(related, EduLesson.sorter);
			}
		}
    	return new NSArray(out);
	}

	public NSMutableDictionary getOnDate(Date date) {
		return array[dateIndex(date)];
	}

	public NSMutableDictionary getOrCreateOnDate(Date date) {
		if(array[dateIndex(date)] == null)
			array[dateIndex(date)] = new NSMutableDictionary();
		return array[dateIndex(date)];
	}
	
	public void setOnDate(NSMutableDictionary dict, Date date) {
		array[dateIndex(date)] = dict;
	}
	
	public NSMutableDictionary[] getArray() {
		return array;
	}

	public NSTimestamp getBeginDate() {
		return beginDate;
	}

	public EduCourse course() {
		return forCourse;
	}
	
	public boolean isInitialized() {
		int fin = (end == null)?initialized.length -1: dateIndex(end);
		for (int i = (begin==null)?0:dateIndex(begin); i <= fin; i++) {
			if(!initialized[i])
				return false;
		}
		return true;
	}
	
	public void setInitialized(boolean set) {
		int fin = (end == null)?initialized.length -1: dateIndex(end);
		for (int i = (begin==null)?0:dateIndex(begin); i <= fin; i++) {
			initialized[i] = set;
			if(!set)
				array[i] = null;
		}
	}
	
	public static StringBuilder appendValueToKeyInDict(String value, String key, 
			NSMutableDictionary dict, String separator) {
		StringBuilder buf = (StringBuilder)dict.valueForKey(key);
		if(buf == null) {
			buf = new StringBuilder();
			dict.takeValueForKey(buf, key);
		} else {
			if(buf.indexOf(value) >= 0)
				return buf;
			if(separator != null)
				buf.append(separator);
		}
		buf.append(value);
		return buf;
	}
}
