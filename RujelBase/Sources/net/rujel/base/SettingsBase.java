//  SettingsBase.java

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

package net.rujel.base;

import java.util.Enumeration;

import net.rujel.interfaces.EOInitialiser;
import net.rujel.interfaces.EduCourse;
import net.rujel.interfaces.EduCycle;
import net.rujel.interfaces.EduGroup;
import net.rujel.interfaces.Teacher;

import com.webobjects.foundation.*;
import com.webobjects.eoaccess.EOObjectNotAvailableException;
import com.webobjects.eoaccess.EOUtilities;
import com.webobjects.eocontrol.*;

public class SettingsBase extends _SettingsBase {

	protected static final String[] keys = new String[] {"grade","cycle","eduGroup","teacher"};
	
	public static void init() {
		EOInitialiser.initialiseRelationship("SettingsByCourse","course",false,"courseID","EduCourse");
		EOInitialiser.initialiseRelationship("SettingsByCourse","cycle",false,"cycleID","EduCycle");
		EOInitialiser.initialiseRelationship("SettingsByCourse","eduGroup",false,"groupID","EduGroup");
		EOInitialiser.initialiseRelationship("SettingsByCourse","teacher",false,"teacherID","Teacher");
	}

	public void awakeFromInsertion(EOEditingContext ec) {
		super.awakeFromInsertion(ec);
	}

	public EOEnterpriseObject forCourse(EduCourse course) {
		if(course == null)
			return this;
		NSArray byCourse = byCourse();
		if(byCourse == null || byCourse.count() == 0)
			return this;
		EOEnterpriseObject result = this;
		Enumeration en = byCourse.objectEnumerator();
		int matches = 0;
		Integer eduYear = course.eduYear();
		while (en.hasMoreElements()) {
			EOEnterpriseObject bc = (EOEnterpriseObject) en.nextElement();
			Integer year = (Integer)bc.valueForKey("eduYear");
			if(year != null && !year.equals(eduYear))
				continue;
			if(bc.valueForKey("course") == course)
				return bc;
			int match = match(bc, course);
			if(match > matches) {
				matches = match;
				result = bc;
			}
		}
		return result;
	}
	
	protected int match(EOEnterpriseObject bc, EduCourse course) {
		int match = 0;
		for (int i = 1; i < keys.length; i++) {
			if (bc.valueForKey(keys[i]) == course.valueForKey(keys[i]))
				match++;
		}
		Integer grade = (Integer)bc.valueForKey(keys[0]);
		if (grade != null && grade.equals(course.cycle().grade()))
			match++;
		return match;
	}
	
	public EOEnterpriseObject forValue(Object value, Integer eduYear) {
		if(value == null || value == NullValue)
			return this;
		if(value instanceof EduCourse)
			return forCourse((EduCourse)value);
		NSArray byCourse = byCourse();
		if(byCourse == null || byCourse.count() == 0)
			return this;
		int idx = -1;
		Number grade = null;
		if(value instanceof Number) {
			idx = 0;
//			grade = (Number)value;
		} else if(value instanceof EduCycle) {
			idx = 1;
			grade = ((EduCycle)value).grade();
		} else if(value instanceof EduGroup) {
			idx = 2;
			grade = ((EduGroup)value).grade();
		} else if(value instanceof Teacher) {
			idx = 3;
		} else {
			return null;
		}
		Enumeration en = byCourse.objectEnumerator();
		EOEnterpriseObject forGrade = this;
		loop:
		while (en.hasMoreElements()) {
			EOEnterpriseObject bc = (EOEnterpriseObject) en.nextElement();
			Integer year = (Integer)bc.valueForKey("eduYear");
			if(year != null && !year.equals(eduYear))
				continue;
			if(!value.equals(bc.valueForKey(keys[idx]))) {
				if(grade != null && grade.equals(bc.valueForKey(keys[0]))) {
					for (int i = 1; i < keys.length; i++) {
						if(bc.valueForKey(keys[i]) != null)
							continue loop;
					}
					forGrade = bc;
				}
				continue;				
			}
			if(bc.valueForKey("course") != null)
				continue;
			for (int i = 1; i < keys.length; i++) {
				if(i == idx)
					continue;
				if(bc.valueForKey(keys[i]) != null)
					continue loop;
			}
			Object grd = bc.valueForKey(keys[0]); 
			if(grd == null || grd.equals(grade))
				return bc;
		}
		return forGrade;
	}
	

	public static EOEnterpriseObject settingForValue(String key, Object value, 
			Integer eduYear, EOEditingContext ec) {
		try {
			SettingsBase sb = (SettingsBase)EOUtilities.objectMatchingKeyAndValue(ec, 
					ENTITY_NAME, KEY_KEY, key);
			return sb.forValue(value, eduYear);
		} catch (Exception e) {
			return null;
		}
	}
	
	public static EOEnterpriseObject settingForCourse(String key, EduCourse course, 
			EOEditingContext ec) {
		if(ec == null && course != null)
			ec = course.editingContext();
		try {
			SettingsBase sb = (SettingsBase)EOUtilities.objectMatchingKeyAndValue(ec, 
					ENTITY_NAME, KEY_KEY, key);
			return sb.forCourse(course);
		} catch (Exception e) {
			return null;
		}
	}
	
	public static SettingsBase createBaseForKey(String key, EOEditingContext ec, 
			String stringValue, Integer numericValue) {
		SettingsBase sb = null;
		try {
			sb = (SettingsBase)EOUtilities.objectMatchingKeyAndValue(ec,
					ENTITY_NAME, KEY_KEY, key);
		} catch (EOObjectNotAvailableException e) {
			sb = (SettingsBase)EOUtilities.createAndInsertInstance(ec, ENTITY_NAME);
			sb.setKey(key);
		}
		sb.setNumericValue(numericValue);
		sb.setTextValue(stringValue);
		return sb;
	}
	
	public static int numericSettingForCourse(String key, EduCourse course, 
			EOEditingContext ec, int defaultValue) {
		EOEnterpriseObject eo = settingForCourse(key, course, ec);
		if (eo==null)
			return defaultValue;
		return ((Integer)eo.valueForKey(NUMERIC_VALUE_KEY)).intValue();
	}

	public static Integer numericSettingForCourse(String key, EduCourse course, 
			EOEditingContext ec) {
		EOEnterpriseObject eo = settingForCourse(key, course, ec);
		return (eo==null)?null:(Integer)eo.valueForKey(NUMERIC_VALUE_KEY);
	}
	
	public static String stringSettingForCourse(String key, EduCourse course, 
			EOEditingContext ec) {
		EOEnterpriseObject eo = settingForCourse(key, course, ec);
		return (eo==null)?null:(String)eo.valueForKey(TEXT_VALUE_KEY);
	}
}
