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

import net.rujel.base.MyUtility;
import net.rujel.interfaces.EduCourse;
import net.rujel.interfaces.EduLesson;
import net.rujel.interfaces.PerPersonLink;
import net.rujel.interfaces.Student;
import net.rujel.reusables.Period;

import com.webobjects.appserver.WOContext;
import com.webobjects.appserver.WOComponent;
import com.webobjects.appserver.WOMessage;
import com.webobjects.appserver.WOSession;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSDictionary;
import com.webobjects.foundation.NSMutableArray;
import com.webobjects.foundation.NSMutableDictionary;

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
    
    public PerPersonLink.Dictionary datePlink() {
    	return (PerPersonLink.Dictionary)valueForBinding("lesson");
    }
    
    public Student student() {
    	return (Student)valueForBinding("student");
    }
    
    
    protected NSDictionary _dict;
    
    public NSDictionary dict() {
    	if(_dict == null) {
    		_dict = (NSMutableDictionary)datePlink().forPersonLink(student());
    		if(_dict == null)
    			_dict = NSDictionary.EmptyDictionary;
    	}
    	return _dict;
    }
    
	public String lessonTitle() {
		return NotePresenter.titleForLesson(datePlink());
	}

	public String value() {
		NSDictionary dict = dict();
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
		super.reset();
	}
}
