// DiarySchedule.java : Class file for WO Component 'DiarySchedule'

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

package net.rujel.schedule;

import java.text.Format;
import java.util.Enumeration;

import com.webobjects.appserver.WOContext;
import com.webobjects.appserver.WOResponse;
import com.webobjects.eocontrol.EOAndQualifier;
import com.webobjects.eocontrol.EOEditingContext;
import com.webobjects.eocontrol.EOFetchSpecification;
import com.webobjects.eocontrol.EOKeyValueQualifier;
import com.webobjects.eocontrol.EOQualifier;
import com.webobjects.eocontrol.EOSortOrdering;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSKeyValueCoding;
import com.webobjects.foundation.NSMutableArray;
import com.webobjects.foundation.NSMutableDictionary;
import com.webobjects.foundation.NSTimestamp;

import net.rujel.base.MyUtility;
import net.rujel.base.SettingsBase;
import net.rujel.reusables.Various;
import net.rujel.ui.LessonList;

public class DiarySchedule extends LessonList {

	public NSArray courses;
	public EOEditingContext ec;
	public NSMutableArray coming;
	public NSKeyValueCoding item;
	public NSTimestamp date;
	
	public DiarySchedule(WOContext context) {
        super(context);
    }

	public void appendToResponse(WOResponse aResponse, WOContext aContext) {
		courses = (NSArray)valueForBinding("courses");
		if(courses == null) {
			aResponse.appendContentString("No courses defined");
			return;
		}
		date = (NSTimestamp)valueForBinding("date");
		Integer year = (date == null)?(Integer)application().valueForKey("year"):
			MyUtility.eduYearForDate(date);
		ec = (EOEditingContext)application().valueForKeyPath(
				"ecForYear." + year.toString());
		EOQualifier[] quals = new EOQualifier[3];
		quals[0] = Various.getEOInQualifier("course", courses);
		quals[1] = new EOKeyValueQualifier(ScheduleEntry.VALID_SINCE_KEY, 
				EOQualifier.QualifierOperatorGreaterThan, date);
		quals[2] = new EOKeyValueQualifier(ScheduleEntry.FLAGS_KEY, 
				EOQualifier.QualifierOperatorEqual, new Integer(0));
		quals[0] = new EOAndQualifier(new NSArray(quals));
		NSArray list = new NSArray(new EOSortOrdering (
				ScheduleEntry.VALID_SINCE_KEY,EOSortOrdering.CompareAscending));
		EOFetchSpecification fs = new EOFetchSpecification(ScheduleEntry.ENTITY_NAME,
				quals[0],list);
		list = ec.objectsWithFetchSpecification(fs);
		if(list != null && list.count() > 0) {
	    	int week = SettingsBase.numericSettingForCourse("EduPeriod", null, ec,7);
			ScheduleEntry sdl = (ScheduleEntry)list.objectAtIndex(0);
			NSTimestamp min = sdl.validSince();
			NSMutableDictionary dict = new NSMutableDictionary( new Object[] {min,min},
					new String[] {"since","date"});
	    	coming = new NSMutableArray(dict);
			if(list.count() > 1) {
				NSTimestamp late = min.timestampByAddingGregorianUnits(0, 0, week, 0, 0, 0);
				Enumeration enu = list.objectEnumerator();
				while (enu.hasMoreElements()) {
					sdl = (ScheduleEntry) enu.nextElement();
					NSTimestamp max = sdl.validSince();
					if(max.after(late)) {
						min = max;
						late = min.timestampByAddingGregorianUnits(0, 0, week, 0, 0, 0);
						dict = new NSMutableDictionary( new Object[] {min,min},
								new String[] {"since","date"});
						coming.addObject(dict);
					} else {
						dict.takeValueForKey(max, "date");
					}
				}
			}
		}
		super.appendToResponse(aResponse, aContext);
	}
	
	public Format formatter() {
		return MyUtility.dateFormat();
	}

	public boolean synchronizesVariablesWithBindings() {
        return false;
	}
	public boolean isStateless() {
		return true;
	}
	
	public void reset() {
		courses = null;
		ec = null;
		date = null;
		coming = null;
		item = null;
		super.reset();
	}
}