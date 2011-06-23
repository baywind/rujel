// ConsolidatedCell: Class file for WO Component 'ConsolidatedCell'

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

import java.util.Enumeration;
import java.util.GregorianCalendar;

import net.rujel.interfaces.PerPersonLink;
import net.rujel.interfaces.Student;

import com.webobjects.appserver.WOApplication;
import com.webobjects.appserver.WOContext;
import com.webobjects.appserver.WOComponent;
import com.webobjects.appserver.WOMessage;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSDictionary;
import com.webobjects.foundation.NSKeyValueCoding;
import com.webobjects.foundation.NSMutableArray;
import com.webobjects.foundation.NSMutableDictionary;
import com.webobjects.foundation.NSTimestamp;

public class ConsolidatedCell extends WOComponent {
    public ConsolidatedCell(WOContext context) {
        super(context);
    }
/*
    public static NSArray prepareList(NSArray lessons, WOSession ses) {
    	EduLesson lesson1 = (EduLesson)lessons.objectAtIndex(0);
    	EduLesson lesson2  = (EduLesson)lessons.lastObject();
    	EduCourse course = lesson1.course();
    	int eduYear = course.eduYear().intValue();
    	ses.setObjectForKey(new Period.ByDates(lesson1.date(),lesson2.date()), "compoundPeriod");
    	ses.setObjectForKey(lesson1.course(), "compoundCell");
    	NSArray list = (NSArray)ses.valueForKeyPath("modules.compoundCell");
    	if(list == null || list.count() == 0)
    	return null;
    	final int day0 = MyUtility.dayOfEduYear(lesson1.date(), eduYear);
    	int day = MyUtility.dayOfEduYear(lesson2.date(), eduYear);
    	NSMutableDictionary[] arr = new NSMutableDictionary[day - day0 +1];
    	Enumeration enu = lessons.objectEnumerator();
    	while (enu.hasMoreElements()) {
			lesson1 = (EduLesson) enu.nextElement();
			day = MyUtility.dayOfEduYear(lesson1.date(), eduYear);
			day -= day0;
			if(arr[day] == null) {
				arr[day] = new NSMutableDictionary(lesson1.date(),"date");
				
			} else {
				Integer colspan = (Integer)arr[day].valueForKey("colspan");
				if(colspan == null) {
					arr[day].takeValueForKey(new Integer(2), "colspan");
				} else {
					colspan = new Integer(colspan.intValue() +1);
					arr[day].takeValueForKey(colspan,"colspan");
				}
			}
		}
    	enu = list.objectEnumerator();
    	while (enu.hasMoreElements()) {
			NSMutableDictionary mDict = (NSMutableDictionary) enu.nextElement();
			
		}
    	
    	NSMutableArray result = new NSMutableArray(lessons.count());
    	for (int i = 0; i < arr.length; i++) {
			if(arr[i] != null)
				result.addObject(new PerPersonLink.Dictionary(arr[i]));
		}
    	return new NSArray(result);
    }
    
    protected DateAgregate dateAgregate() {
    	DateAgregate agr = (DateAgregate)valueForBinding("dateAgregate");
    	if(agr == null) {
    		agr = new DateAgregate((EduCourse)valueForBinding("course"));
    		setValueForBinding(agr, "dateAgregate");
    	}
    	return agr;
    }*/
    
    public Object item;
    
    protected NSArray views;
    protected Enumeration viewsEnu() {
    	if(views == null)
    		views = (NSArray)session().valueForKeyPath("state.consolidatedView");
    	return views.objectEnumerator();
    }
    
    public PerPersonLink.Dictionary datePlink() {
    	return (PerPersonLink.Dictionary)valueForBinding("lesson");
    }
    
    public Student student() {
    	return (Student)valueForBinding("student");
    }
    
    public Boolean showTitle() {
    	return (student() == null && valueForBinding("titleRow") == null);
    }
    
    protected NSKeyValueCoding _dict;
    
    public NSKeyValueCoding dict() {
    	if(_dict == null) {
    		if(student() == null)
    			_dict = datePlink();
    		else
    			_dict = (NSMutableDictionary)datePlink().forPersonLink(student());
    		if(_dict == null)
    			_dict = NSDictionary.EmptyDictionary;
    	}
    	return _dict;
    }
    
    public NSArray cells() {
    	NSMutableArray result = new NSMutableArray();
    	PerPersonLink.Dictionary dict = (student()==null)?null:datePlink();
    	CharSequence style = null;
    	CharSequence styleClass = null;
    	Enumeration enu = viewsEnu();
    	while (enu.hasMoreElements()) {
    		String view = (String)enu.nextElement();
			Object local = dict().valueForKey(view);
			Object global = (dict==null)?null:dict.valueForKey(view);
			if(local == null) {
				if(global instanceof NSDictionary[]) {
					for (int i = 0; i < ((NSDictionary[])global).length; i++) {
						result.addObject(NSDictionary.EmptyDictionary);
					}
				}
				continue;
			}
			if(local instanceof NSDictionary[]) {
				NSDictionary[] found = (NSDictionary[])local;
				int length = (global instanceof NSDictionary[])?((NSDictionary[])global).length:
					found.length;
				for (int i = 0; i < length; i++) {
					if(i >= found.length || found[i] == null) {
						result.addObject(NSDictionary.EmptyDictionary);
					} else {
						result.addObject(found[i].mutableClone());
					}
				}
			} else if(local instanceof NSDictionary) {
				NSDictionary found = (NSDictionary)local;
				if(found.count() == 0)
					continue;
				style = appendFromDict(style, found, "style");
				styleClass = appendFromDict(styleClass, found, "styleClass");
			}
		}
    	if(result.count() <= 1) {
    		if(styleClass == null)
    			styleClass = "lbd rbd";
    		else
    			styleClass = styleClass + " lbd rbd";
    	}
    	NSDictionary generic = NSDictionary.EmptyDictionary;
    	if(style != null || styleClass != null) {
    		generic = new NSMutableDictionary();
    		generic.takeValueForKey(style, "style");
    		generic.takeValueForKey(styleClass, "styleClass");
    		generic = generic.immutableClone();
    	}
    	if(result.count() == 0) {
    		return new NSArray(generic);
    	} else if(result.count() <= 1) {
    		NSDictionary rd = (NSDictionary)result.objectAtIndex(0);
    		if(rd == NSDictionary.EmptyDictionary)
    			return new NSArray(generic);
//    		if(!(rd instanceof NSMutableDictionary))
//				rd = rd.mutableClone();
    		rd.takeValueForKey(appendFromDict(styleClass, rd, "styleClass"), "styleClass");
			if(style != null)
				rd.takeValueForKey(appendFromDict(style, rd, "style"), "style");
    		return new NSArray(rd);
    	} else {
    		for (int i = 0; i < result.count(); i++) {
				String val = null;
    			if(i == 0)
    				val = "lbd";
    			else if (i == result.count() -1)
    				val = "rbd";
    			else if (generic == NSDictionary.EmptyDictionary)
    				continue;
				NSDictionary rd = (NSDictionary)result.objectAtIndex(i);
				if(rd == NSDictionary.EmptyDictionary) {
					if(val == null) {
						rd = generic;
					} else {
						if(styleClass != null)
							val = val + ' ' + styleClass;
						if(style == null) {
							rd = new NSDictionary(val,"styleClass");
						} else {
							rd = generic.mutableClone();
							rd.takeValueForKey(val, "styleClass");
						}
					}
					result.replaceObjectAtIndex(rd, i);
				} else {
//					if(!(rd instanceof NSMutableDictionary)) {
//						rd = rd.mutableClone();
//						result.replaceObjectAtIndex(rd, i);
//					}
					if(styleClass != null) {
						if(val == null)
							val = styleClass.toString();
						else 
							val = val + ' ' + styleClass;
					}
					if(val != null)
						rd.takeValueForKey(appendFromDict(val, rd, "styleClass"), "styleClass");
					if(style != null)
						rd.takeValueForKey(appendFromDict(style, rd, "style"), "style");
				}
			}
    	}
    	// add borders
    	return result;
    }
    
    private CharSequence appendFromDict(CharSequence initial, NSDictionary dict, String key) {
    	if(initial == null)
    		return (CharSequence)dict.valueForKey(key);
    	CharSequence value = (CharSequence)dict.valueForKey(key);
    	if(value == null)
    		return initial;
		StringBuilder buf = new StringBuilder();
		buf.append(initial).append(' ').append(value);
		return buf;
    }
    
	public String lessonTitle() {
		NSKeyValueCoding lesson = datePlink();
		if(lesson==null)return null;
		GregorianCalendar cal = new GregorianCalendar();
		cal.setTime((NSTimestamp)lesson.valueForKey("date"));
		StringBuilder buf = new StringBuilder("<strong>");
		buf.append(cal.get(GregorianCalendar.DAY_OF_MONTH)).append("</strong>");
		try {
			NSArray days = (NSArray)WOApplication.application().valueForKeyPath(
			"strings.Reusables_Strings.presets.weekdayShort");
			int day = cal.get(GregorianCalendar.DAY_OF_WEEK);
			String weekday = (String)days.objectAtIndex(day);
			buf.append("<br/><small>").append(weekday).append("</small>");
		} catch (Exception e) {
			; //failed to get weekday
		}
		NSArray lessons = (NSArray)lesson.valueForKey("lessons");
		if(lessons != null && lessons.count() > 1) {
			String result = buf.toString();
			buf.delete(0, buf.length());
			buf.append("<table cellspacing = \"0\" width = \"100%\"><tr>\n\t");
			for (int i = 0; i < lessons.count(); i++) {
				buf.append("<th>").append(result).append("</th>\n\t");
			}
			buf.append("\n</tr></table>");
		} else {
			buf.insert(0,"<div style = \"width:2em\">");
			buf.append("</div>");
		}
		return buf.toString();
	}

	public String value() {
		NSKeyValueCoding dict = dict();
		Object result = dict.valueForKey("prefix");
		if(result != null)
			result = result.toString();
		result = concatValues(result, dict.valueForKey("value"));
		result = concatValues(result, dict.valueForKey("suffix"));
		if(result == null)
			result = dict.valueForKey("ifEmpty");
		if(dict.valueForKey("hover") != null) {
			result = concatValues(result, "*");
		}
		if(result == null)
			return null;
		return result.toString();
	}
	
	private static Object concatValues(Object result, Object value) {
		if(result == null)
			return (value == null)?null:value.toString();
		if(value == null)
			return result;
		if(!(result instanceof StringBuilder))
			result = new StringBuilder(result.toString());
		((StringBuilder)result).append(' ').append(value);
		return result;
	}
    
	public String titleHover() {
		Object hover = datePlink().valueForKey("hover");
		if(hover == null)
			return null;
		return WOMessage.stringByEscapingHTMLAttributeValue(hover.toString());
	}
	
	public boolean isStateless() {
		return true;
	}

	public boolean synchronizesVariablesWithBindings() {
        return false;
	}
	
	public void reset() {
		_dict = null;
		views = null;
		super.reset();
	}
}
