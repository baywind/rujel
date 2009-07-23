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

	protected static final String[] keys = new String[] {"grade","eduGroup","cycle","teacher"};
	
	public static void init() {
		EOInitialiser.initialiseRelationship("SettingByCourse","course",false,"courseID","EduCourse");
		EOInitialiser.initialiseRelationship("SettingByCourse","cycle",false,"cycleID","EduCycle");
		EOInitialiser.initialiseRelationship("SettingByCourse","eduGroup",false,"groupID","EduGroup");
		EOInitialiser.initialiseRelationship("SettingByCourse","teacher",false,"teacherID","Teacher");
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
			if(bc.valueForKey(keys[i]) == null)
				continue;
			if (bc.valueForKey(keys[i]) == course.valueForKey(keys[i]))
				match += 1<<i;
			else
				return 0;
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
		Object[] vals = new Object[keys.length];
		if(value instanceof Number) {
			vals[0] = value;
		} else if(value instanceof EduCycle) {
			vals[1] = value;
			vals[0] = ((EduCycle)value).grade();
		} else if(value instanceof EduGroup) {
			vals[2] = value;
			vals[0] = ((EduGroup)value).grade();
		} else if(value instanceof Teacher) {
			vals[3] = value;
		} else {
			int count = 0;
			for (int i = 0; i < keys.length; i++) {
				try {
					vals[i] = NSKeyValueCoding.Utility.valueForKey(value, keys[i]);
				} catch (Exception e) {
					;
				}
				if(vals[i] != null) {
					count++;
					if(vals[0] == null && (i==1 || i==2))
						vals[0] = NSKeyValueCoding.Utility.valueForKey(vals[i], keys[0]);
				}
			}
			if(count == 0)
				return null;
			if(eduYear == null) {
				try {
					eduYear = (Integer)NSKeyValueCoding.Utility.valueForKey(value, "eduYear");
				} catch (Exception e) {
					;
				}
			}
		}
		Enumeration en = byCourse.objectEnumerator();
		EOEnterpriseObject result = this;
		int count = 0;
		loop:
		while (en.hasMoreElements()) {
			EOEnterpriseObject bc = (EOEnterpriseObject) en.nextElement();
			if(bc.valueForKey("course") != null)
				continue;
			Integer year = (Integer)bc.valueForKey("eduYear");
			if(year != null && !year.equals(eduYear))
				continue;
			int bcCount = 0;
			for (int i = 1; i < keys.length; i++) {
				if(bc.valueForKey(keys[i]) != null) {
					if(!bc.valueForKey(keys[i]).equals(vals[i]))
						continue loop;
					else
						bcCount += 1<<i;
				}
				if(bcCount > count) {
					result = bc;
					count = 0;
				}
			}
		}
		return result;
	}
	
	public void updateNumValuesForText(String textValue, Integer numValue) {
		if(textValue.equals(textValue()))
			setNumericValue(numValue);
		NSDictionary values = new NSDictionary(new Object[] {this,textValue},
				new String[] {"settingsBase",TEXT_VALUE_KEY});
		NSArray subs = EOUtilities.objectsMatchingValues(
				editingContext(), "SettingByCourse", values);
		if(subs != null && subs.count() > 0)
			subs.takeValueForKey(numValue, NUMERIC_VALUE_KEY);
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
//		try {
			SettingsBase sb = (SettingsBase)EOUtilities.objectMatchingKeyAndValue(ec, 
					ENTITY_NAME, KEY_KEY, key);
			return sb.forCourse(course);
//		} catch (Exception e) {
//			return null;
//		}
	}
	
	public static SettingsBase baseForKey(String key, EOEditingContext ec, boolean create) {
		SettingsBase sb = null;
		try {
			sb = (SettingsBase)EOUtilities.objectMatchingKeyAndValue(ec,
					ENTITY_NAME, KEY_KEY, key);
		} catch (EOObjectNotAvailableException e) {
			sb = (SettingsBase)EOUtilities.createAndInsertInstance(ec, ENTITY_NAME);
			sb.setKey(key);
		}
		return sb;
	}
	
	public static SettingsBase createBaseForKey(String key, EOEditingContext ec, 
			String stringValue, Integer numericValue) {
		SettingsBase sb = baseForKey(key, ec, true);
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
	
	public static class Comparator extends NSComparator {
		public int compare(Object arg0, Object arg1) throws ComparisonException {
			if(arg0 == null && arg1 == null)
				return OrderedSame;
			try {
				NSKeyValueCoding l = (NSKeyValueCoding)arg0;
				NSKeyValueCoding r = (NSKeyValueCoding)arg1;
				if(l == null)
					return OrderedDescending;
				else if(r == null)
					return OrderedAscending;
				if(l instanceof SettingsBase) {
					if(r instanceof SettingsBase)
						return OrderedSame;
					return OrderedAscending;
				} else if(r instanceof SettingsBase) {
					return OrderedDescending;
				}
				int result = compareKeys(l, r, "course");
				if(result != OrderedSame)
					return result;
				int lCount = 0;
				int rCount = 0;
				int order = 0;
				for (int i = 0; i < keys.length; i++) {
					if(l.valueForKey(keys[i]) != null)
						lCount += 1<<i;
					if(r.valueForKey(keys[i]) != null)
						rCount += 1<<i;
					if(lCount == rCount) {
						order += compareKeys(l, r, keys[i])<<(keys.length -i);
					}
				}
				if(lCount == 0)
					lCount = 7;
				if(rCount == 0)
					rCount = 7;
				if(lCount < rCount)
					return OrderedAscending;
				if(lCount > rCount)
					return OrderedDescending;
				if(order > 0)
					return OrderedDescending;
				if(order < 0)
					return OrderedAscending;
			} catch (RuntimeException e) {
				throw new ComparisonException("Illegal arguments to compare");
			}
			return OrderedSame;
		}
		
		protected int compareKeys(NSKeyValueCoding l, NSKeyValueCoding r, String key) {
			Object lo = l.valueForKey(key);
			Object ro = r.valueForKey(key);
			if(lo != null) {
				if(ro == null) {
					return OrderedDescending;
				} else {
					return EOSortOrdering.ComparisonSupport.compareValues
										(lo, ro, EOSortOrdering.CompareAscending);
//					EOSortOrdering.ComparisonSupport support = 
//						EOSortOrdering.ComparisonSupport.supportForClass(lo.getClass());
//					return support.compareAscending(lo, ro);
				}
			} else {
				if(ro == null)
					return OrderedSame;
				else
					return OrderedAscending;
			}
		}
		
	}
}
