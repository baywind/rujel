//  ScheduleEntry.java

/*
 * Copyright (c) 2008, Gennady & Michael Kushnir
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 * 
 * 	o	Redistributions of source code must retain the above copyright notice, this
 * 		list of conditions and the following disclaimer.
 * 	o	Redistributions in binary form must reproduce the above copyright notice,
 * 		this list of conditions and the following disclaimer in the documentation
 * 		and/or other materials provided with the distribution.
 * 	o	Neither the name of the RUJEL nor the names of its contributors may be used
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

import java.util.Calendar;
import java.util.logging.Logger;

import net.rujel.base.SettingsBase;
import net.rujel.interfaces.EOInitialiser;
import net.rujel.interfaces.EduCourse;
import net.rujel.reusables.NamedFlags;
import net.rujel.reusables.WOLogLevel;

import com.webobjects.eocontrol.*;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSTimestamp;

public class ScheduleEntry extends _ScheduleEntry {
	
	public static final NSArray tableSorter = new NSArray(new EOSortOrdering[] {
			new EOSortOrdering(NUM_KEY, EOSortOrdering.CompareAscending),
			new EOSortOrdering(WEEKDAY_NUM_KEY, EOSortOrdering.CompareAscending)
	});

	public static void init() {
		EOInitialiser.initialiseRelationship("ScheduleEntry","otherTeacher",false,"teacherID","Teacher");
		EOInitialiser.initialiseRelationship("ScheduleEntry","course",false,"courseID","EduCourse");
	}

	public EduCourse course() {
		return (EduCourse)storedValueForKey("course");
	}

	public void setCourse(EduCourse course) {
		takeStoredValueForKey(course, "course");
	}

	public void awakeFromInsertion(EOEditingContext ec) {
		super.awakeFromInsertion(ec);
		Integer zero = new Integer(0);
		setFlags(zero);
	}
	
	public int index() {
		int weekStart = SettingsBase.numericSettingForCourse(
				"weekStart", course(), editingContext(), Calendar.MONDAY);
		return (weekdayNum().intValue() - weekStart +1)*100 + num().intValue();
	}
	
	public void setIndex(int index) {
		int weekStart = SettingsBase.numericSettingForCourse(
				"weekStart", course(), editingContext(), Calendar.MONDAY);
		setWeekdayNum(new Integer(index/100 + weekStart -1));
		setNum(new Integer(index%100));
	}

	public static final NSArray flagNames = new NSArray (new String[] {
			"-1-","-2-","-4-","temporary"});

	private NamedFlags _flags;
	public NamedFlags namedFlags() {
		if(_flags==null) {
			_flags = new NamedFlags(flags().intValue(),flagNames);
			try{
				_flags.setSyncParams(this, getClass().getMethod("setNamedFlags",
						NamedFlags.class));
			} catch (Exception e) {
				Logger.getLogger("rujel.schedule").log(WOLogLevel.WARNING,
						"Could not get syncMethod for ScheduleEntry flags",e);
			}
		}
		return _flags;
	}

	public void setNamedFlags(NamedFlags flags) {
		if(flags != null)
			setFlags(flags.toInteger());
		_flags = flags;
	}

	public void setFlags(Integer value) {
		_flags = null;
		super.setFlags(value);
	}

	public boolean isTemporary() {
		return ((flags().intValue() & 8) > 0);
	}

	public static EOQualifier onDate(NSTimestamp date) {
		EOQualifier[] quals = new EOQualifier[2];
		quals[0] = new EOKeyValueQualifier(VALID_SINCE_KEY,
				EOQualifier.QualifierOperatorEqual,null);
		quals[1] = new EOKeyValueQualifier(VALID_SINCE_KEY,
				EOQualifier.QualifierOperatorLessThanOrEqualTo,date);
		EOQualifier since = new EOOrQualifier(new NSArray(quals));
		quals[0] = new EOKeyValueQualifier(VALID_TO_KEY,
				EOQualifier.QualifierOperatorEqual,null);
		quals[1] = new EOKeyValueQualifier(VALID_TO_KEY,
				EOQualifier.QualifierOperatorGreaterThanOrEqualTo,date);
		quals[1] = new EOOrQualifier(new NSArray(quals));
		quals[0] = since;
		return new EOAndQualifier(new NSArray(quals));
	}
}