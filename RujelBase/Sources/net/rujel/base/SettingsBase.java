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
import java.util.logging.Logger;

import net.rujel.interfaces.EduCourse;
import net.rujel.reusables.ModulesInitialiser;
import net.rujel.reusables.WOLogLevel;

import com.webobjects.foundation.*;
import com.webobjects.eoaccess.EOObjectNotAvailableException;
import com.webobjects.eoaccess.EOUtilities;
import com.webobjects.eocontrol.*;

public class SettingsBase extends _SettingsBase {

	protected static final String[] keys = new String[] {"grade","eduGroup","cycle","teacher"};

	public void awakeFromInsertion(EOEditingContext ec) {
		super.awakeFromInsertion(ec);
	}

	public EOEnterpriseObject forCourse(NSKeyValueCodingAdditions course) {
		if(course == null)
			return this;
		NSArray byCourse = qualifiedSettings();
		if(byCourse == null || byCourse.count() == 0)
			return this;
		if(course instanceof EduCourse)
			course = EOUtilities.localInstanceOfObject(editingContext(), (EduCourse)course);
		EOEnterpriseObject result = this;
		Enumeration en = byCourse.objectEnumerator();
		int match = 0;
		while (en.hasMoreElements()) {
			QualifiedSetting bc = (QualifiedSetting) en.nextElement();
			if(match > 0 && match > bc.sort().intValue())
				continue;
			try {
				if(bc.evaluateWithObject(course)) {
					match = bc.sort().intValue();
					result = bc;
				}
			} catch (Exception e) {
				Logger.getLogger("rujel.base").log(WOLogLevel.WARNING,
						"Error reading QualifiedSetting from " + key(), new Object[] {bc,e});
			}
		}
		return result;
	}
	/*
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
		NSArray byCourse = qualifiedSettings();
		if(byCourse == null || byCourse.count() == 0)
			return this;
		if(value instanceof EOEnterpriseObject && 
				((EOEnterpriseObject)value).editingContext() != editingContext()) {
			value = EOUtilities.localInstanceOfObject(editingContext(), (EOEnterpriseObject)value);
		}
		Object[] vals = new Object[keys.length];
		if(value instanceof Number) {
			vals[0] = value;
		} else if(value instanceof EduGroup) {
			vals[1] = value;
			vals[0] = ((EduGroup)value).grade();
		} else if(value instanceof EduCycle) {
			vals[2] = value;
			vals[0] = ((EduCycle)value).grade();
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
					if( i > 0 && 
							((EOEnterpriseObject)vals[i]).editingContext() != editingContext()) {
						vals[i] = EOUtilities.localInstanceOfObject(editingContext(),
								((EOEnterpriseObject)vals[i]));
					}
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
			for (int i = 0; i < keys.length; i++) {
				if(bc.valueForKey(keys[i]) != null) {
					if(!bc.valueForKey(keys[i]).equals(vals[i]))
						continue loop;
					else
						bcCount += 1<<i;
				}
			}
			if(bcCount > count) {
				result = bc;
				count = bcCount;
			}
		}
		return result;
	}
	*/
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
/*
	public static EOEnterpriseObject settingForValue(String key, Object value, 
			Integer eduYear, EOEditingContext ec) {
		try {
			SettingsBase sb = (SettingsBase)EOUtilities.objectMatchingKeyAndValue(ec, 
					ENTITY_NAME, KEY_KEY, key);
			return sb.forValue(value, eduYear);
		} catch (Exception e) {
			return null;
		}
	}*/
	
	public static EOEnterpriseObject settingForCourse(String key, 
			NSKeyValueCodingAdditions course, EOEditingContext ec) {
		if(ec == null && course instanceof EduCourse)
			ec = ((EduCourse)course).editingContext();
		try {
			SettingsBase sb = (SettingsBase)EOUtilities.objectMatchingKeyAndValue(ec, 
					ENTITY_NAME, KEY_KEY, key);
			return sb.forCourse(course);
		} catch (EOObjectNotAvailableException e) {
			return null;
		}
	}
	
	public static SettingsBase baseForKey(String key, EOEditingContext ec, boolean create) {
		SettingsBase sb = null;
		try {
			sb = (SettingsBase)EOUtilities.objectMatchingKeyAndValue(ec,
					ENTITY_NAME, KEY_KEY, key);
		} catch (EOObjectNotAvailableException e) {
			if(create) {
				sb = (SettingsBase)EOUtilities.createAndInsertInstance(ec, ENTITY_NAME);
				sb.setKey(key);
			}
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
		if (eo==null || eo.valueForKey(NUMERIC_VALUE_KEY) == null)
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
	
	public NSArray settingUsage(String selector, Object value, Object eduYear) {
    	NSArray byCourse = qualifiedSettings();
    	NSMutableArray usage = new NSMutableArray();
		Object val = valueForKey(selector);
    	if((val == null)?value == null : val.equals(value))
			usage.addObject(this);
    	if(byCourse != null && byCourse.count() > 0) {
    		Enumeration enu = byCourse.objectEnumerator();
    		while (enu.hasMoreElements()) {
    			EOEnterpriseObject bc = (EOEnterpriseObject) enu.nextElement();
    			if(eduYear != null) {
    				val = bc.valueForKey("eduYear");
    				if(val != null && !val.equals(eduYear))
    					continue;
    			}
    			val = bc.valueForKey(selector);
    	    	if((val == null)?value == null : val.equals(value))
    				usage.addObject(bc);
    		}
    	}
    	return usage;
	}
	
    public NSMutableArray byCourse(Integer eduYear) {
		NSArray baseByCourse = qualifiedSettings();
		NSMutableArray byCourse = new NSMutableArray(this);
		if(baseByCourse == null || baseByCourse.count() == 0)
			return byCourse;
		Enumeration enu = baseByCourse.objectEnumerator();
		if(eduYear == null) {
			byCourse.addObjectsFromArray(baseByCourse);
		} else {
			while (enu.hasMoreElements()) {
				EOEnterpriseObject bc = (EOEnterpriseObject) enu.nextElement();
				if(bc.valueForKey("eduYear") == null || 
						eduYear.equals(bc.valueForKey("eduYear")))
					byCourse.addObject(bc);
			}
		}
		if(byCourse.count() > 2) {
			EOSortOrdering.sortArrayUsingKeyOrderArray(byCourse, ModulesInitialiser.sorter);
		}
    	return byCourse;
    }
	/*
	public static EOQualifier byCourseQualifier(EOEnterpriseObject byCourse) {
		if(!byCourse.entityName().equals("SettingByCourse"))
			return null;
		if(byCourse.valueForKey("course") != null)
			return EOUtilities.qualifierForEnterpriseObject(byCourse.editingContext(),
					byCourse);
		NSMutableArray quals = new NSMutableArray();
		Object param = byCourse.valueForKey("eduGroup"); 
		if(param != null)
			quals.addObject(new EOKeyValueQualifier("eduGroup",
					EOQualifier.QualifierOperatorEqual,param));
		param = byCourse.valueForKey("cycle"); 
		if(param != null)
			quals.addObject(new EOKeyValueQualifier("cycle",
					EOQualifier.QualifierOperatorEqual,param));
		param = byCourse.valueForKey("grade"); 
		if(param != null && quals.count() == 0) {
			NSArray cycles = EduCycle.Lister.cyclesForGrade((Integer)param,
					byCourse.editingContext());
			quals.addObject(Various.getEOInQualifier("cycle", cycles));
		}
		param = byCourse.valueForKey("teacher"); 
		if(param != null || quals.count() == 0) {
			quals.addObject(new EOKeyValueQualifier("teacher",
					EOQualifier.QualifierOperatorEqual,param));
		}
		param = byCourse.valueForKey("eduYear"); 
		if(param != null)
			quals.addObject(new EOKeyValueQualifier("eduYear",
					EOQualifier.QualifierOperatorEqual,param));
		return new EOAndQualifier(quals);
	} */
	
	public NSArray coursesForSetting(String text, Integer numeric, Integer eduYear) {
		NSArray allCourses = EOUtilities.objectsMatchingKeyAndValue(editingContext(),
				EduCourse.entityName, "eduYear", eduYear);
		if(allCourses == null || allCourses.count() == 0)
			return null;
		return coursesForSetting(text, numeric, allCourses);
	}
	
	public NSArray coursesForSetting(String text, Integer numeric, NSArray allCourses) {
		NSMutableArray result = new NSMutableArray();
		Enumeration enu = allCourses.objectEnumerator();
		while (enu.hasMoreElements()) {
			EduCourse course = (EduCourse) enu.nextElement();
			EOEnterpriseObject setting = forCourse(course);
			if(numeric != null && !numeric.equals(setting.valueForKey(NUMERIC_VALUE_KEY)))
				continue;
			if(text != null && !text.equals(setting.valueForKey(TEXT_VALUE_KEY)))
				continue;
			result.addObject(course);
		}
		return result;
	}
	/*
	public static class Comparator extends NSComparator {
		public int compare(Object arg0, Object arg1)
			throws com.webobjects.foundation.NSComparator.ComparisonException {
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
				throw new com.webobjects.foundation.NSComparator.ComparisonException(
				"Illegal arguments to compare");
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
	*/
	public boolean isSingle() {
		return (qualifiedSettings() == null || qualifiedSettings().count() == 0);
	}
	
	public Integer _sort() {
		return new Integer(0);
	}
}
