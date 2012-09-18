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

import java.lang.ref.WeakReference;
import java.util.Enumeration;
import java.util.logging.Logger;

import net.rujel.interfaces.EduCourse;
import net.rujel.interfaces.EduCycle;
import net.rujel.interfaces.EduGroup;
import net.rujel.reusables.ModulesInitialiser;
import net.rujel.reusables.WOLogLevel;

import com.webobjects.foundation.*;
import com.webobjects.eoaccess.EOObjectNotAvailableException;
import com.webobjects.eoaccess.EOUtilities;
import com.webobjects.eocontrol.*;

public class SettingsBase extends _SettingsBase implements Setting {
	
	protected static final String[] keys = new String[] {"grade","eduGroup","cycle","teacher"};

	public void awakeFromInsertion(EOEditingContext ec) {
		super.awakeFromInsertion(ec);
	}
	
	public Setting forObject(Object obj) {
		Integer eduYear;
		if(obj instanceof EOEnterpriseObject)
			eduYear = MyUtility.eduYear(((EOEnterpriseObject)obj).editingContext());
		else
			eduYear = MyUtility.eduYear(editingContext());
		return forCourse(courseDict(obj,eduYear));
	}

	public Setting forCourse(NSKeyValueCodingAdditions course) {
		if(course == null)
			return this;
		NSArray byCourse = qualifiedSettings();
		if(byCourse == null || byCourse.count() == 0)
			return this;
		if(course instanceof EduCourse)
			course = EOUtilities.localInstanceOfObject(editingContext(), (EduCourse)course);
		Setting result = this;
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
				editingContext(), QualifiedSetting.ENTITY_NAME, values);
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
	
	public static Setting settingForCourse(String key, 
			NSKeyValueCodingAdditions course, EOEditingContext ec) {
		if(ec == null && course instanceof EduCourse)
			ec = ((EduCourse)course).editingContext();
		SettingsBase base = baseForKey(key, ec, false);
		if(base == null)
			return null;
		return base.forCourse(course);
	}
	
	public static NSDictionary courseDict(EduGroup eduGroup) {
		Integer eduYear = MyUtility.eduYear(eduGroup.editingContext());
		return courseDict(eduGroup,eduYear);
	}
	public static NSDictionary courseDict(EduGroup eduGroup,Integer eduYear) {
		NSDictionary dict;
		try {
			Integer section = (Integer)eduGroup.valueForKey("section");
			dict = new NSDictionary(new Integer[] {eduGroup.grade(), section},
					new String[] {"grade","section"});
		} catch (Exception e) {
			dict = new NSDictionary(eduGroup.grade(),"grade");
		}
		return new NSDictionary(new Object[] {dict,eduGroup,eduYear},
				new Object[] {"cycle","eduGroup","eduYear"});
	}
	
	public static NSDictionary courseDict(EduCycle cycle) {
		Integer eduYear = MyUtility.eduYear(cycle.editingContext());
		return courseDict(cycle,eduYear);
	}
	public static NSDictionary courseDict(EduCycle cycle,Integer eduYear) {
		NSDictionary dict = new NSDictionary(cycle.grade(),"grade");
		return new NSDictionary(new Object[] {cycle,dict,eduYear},
				new Object[] {"cycle","eduGroup","eduYear"});
	}
	
	public static NSDictionary courseDict(Integer grade,Integer eduYear) {
		NSDictionary dict = new NSDictionary(grade,"grade");
		if(eduYear == null)
			return new NSDictionary(new Object[] {dict,dict},
					new Object[] {"cycle","eduGroup"});
		else
			return new NSDictionary(new Object[] {dict,dict,eduYear},
					new Object[] {"cycle","eduGroup","eduYear"});
	}
	
	public static NSKeyValueCodingAdditions courseDict(Object obj,Integer eduYear) {
		if(obj == null)
			return null;
		if(obj instanceof EduCourse)
			return (EduCourse)obj;
		if(eduYear == null && obj instanceof EOEnterpriseObject) {
			eduYear = MyUtility.eduYear(((EOEnterpriseObject)obj).editingContext());
		}
		if(obj instanceof EduCycle)
			return courseDict((EduCycle)obj,eduYear);
		if(obj instanceof EduGroup)
			return courseDict((EduGroup)obj,eduYear);
		if(obj instanceof Integer)
			return courseDict((Integer)obj,eduYear);
		throw new IllegalArgumentException(
				"EduCourse, EduCycle, EduGroup or Integer are only accepted. Receiver "
				+ obj.getClass().getName());
	}
		
	public static SettingsBase baseForKey(String key, EOEditingContext ec, boolean create) {
		NSMutableDictionary sbdict = (NSMutableDictionary)ec.userInfoForKey(ENTITY_NAME);
		if(sbdict == null) {
			sbdict = new NSMutableDictionary();
			ec.setUserInfoForKey(sbdict, ENTITY_NAME);
		}
		Object sb = sbdict.valueForKey(key);
		if(sb instanceof WeakReference) {
			sb = ((WeakReference)sb).get();
		}
		if(sb instanceof SettingsBase)
			return (SettingsBase)sb;
		else if(!create && sb == NullValue)
			return null;
		try {
			sb = EOUtilities.objectMatchingKeyAndValue(ec, ENTITY_NAME, KEY_KEY, key);
		} catch (EOObjectNotAvailableException e) {
			if(create) {
				sb = EOUtilities.createAndInsertInstance(ec, ENTITY_NAME);
				((SettingsBase)sb).setKey(key);
			}
		}
		sbdict.takeValueForKey((sb==null)?NullValue:new WeakReference(sb), key);
		return (SettingsBase)sb;
	}
	
	public static SettingsBase createBaseForKey(String key, EOEditingContext ec, 
			String stringValue, Integer numericValue) {
		SettingsBase sb = baseForKey(key, ec, true);
		sb.setNumericValue(numericValue);
		sb.setTextValue(stringValue);
		return sb;
	}
	
	public static int numericSettingForCourse(String key, NSKeyValueCodingAdditions course, 
			EOEditingContext ec, int defaultValue) {
		Setting eo = settingForCourse(key, course, ec);
		if (eo==null || eo.numericValue() == null)
			return defaultValue;
		return ((Integer)eo.numericValue()).intValue();
	}

	public static Integer numericSettingForCourse(String key, NSKeyValueCodingAdditions course, 
			EOEditingContext ec) {
		Setting eo = settingForCourse(key, course, ec);
		return (eo==null)?null:(Integer)eo.numericValue();
	}
	
	public static String stringSettingForCourse(String key, NSKeyValueCodingAdditions course, 
			EOEditingContext ec) {
		Setting eo = settingForCourse(key, course, ec);
		return (eo==null)?null:(String)eo.textValue();
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
    		EOSortOrdering.sortArrayUsingKeyOrderArray(usage, ModulesInitialiser.sorter);
    	}
    	return usage;
	}
	
    public NSMutableArray byCourseSorted(Integer eduYear) {
		NSArray baseByCourse = qualifiedSettings();
		if(baseByCourse == null || baseByCourse.count() == 0)
			return null;
		NSMutableArray byCourse = new NSMutableArray();
		Enumeration enu = baseByCourse.objectEnumerator();
		while (enu.hasMoreElements()) {
			EOEnterpriseObject bc = (EOEnterpriseObject) enu.nextElement();
			if(bc.valueForKey("eduYear") == null || 
					(eduYear != null && eduYear.equals(bc.valueForKey("eduYear"))))
				byCourse.addObject(bc);
		}
		if(byCourse.count() > 1) {
			EOSortOrdering.sortArrayUsingKeyOrderArray(byCourse, ModulesInitialiser.sorter);
		}
    	return byCourse;
    }
	
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
			Setting setting = forCourse(course);
			if(numeric != null && !numeric.equals(setting.numericValue()))
				continue;
			if(text != null && !text.equals(setting.numericValue()))
				continue;
			result.addObject(course);
		}
		return result;
	}
	
	public NSArray availableValues(Integer eduYear, String key) {
		NSMutableArray result = new NSMutableArray();
		Object value = valueForKey(key);
		if(value != null)
			result.addObject(value);
		NSArray byCourse = byCourseSorted(eduYear);
		if(byCourse != null && byCourse.count() > 0) {
			Enumeration enu = byCourse.objectEnumerator();
			while (enu.hasMoreElements()) {
				EOEnterpriseObject bc = (EOEnterpriseObject) enu.nextElement();
				value = bc.valueForKey(key);
				if(value != null && !result.containsObject(value))
					result.addObject(value);
			}
		}
		return result;
	}

	public boolean isSingle() {
		return (qualifiedSettings() == null || qualifiedSettings().count() == 0);
	}
	
	public Integer _sort() {
		return new Integer(0);
	}
	
	public void cleanSBDict() {
		EOEditingContext ec = editingContext();
		if(ec == null) return;
		NSMutableDictionary sbdict = (NSMutableDictionary)ec.userInfoForKey(ENTITY_NAME);
		if(sbdict == null)
			return;
		String key = key();
		if(key != null)
			sbdict.removeObjectForKey(key);
		if(!ec.globalIDForObject(this).isTemporary())
			key = (String)ec.committedSnapshotForObject(this).valueForKey(KEY_KEY);
		if(key != null)
			sbdict.removeObjectForKey(key);
	}
	
	public void validateForSave() {
		super.validateForSave();
		cleanSBDict();
	}
	
	public void validateForDelete() {
		super.validateForDelete();
		cleanSBDict();
	}
	
	public void turnIntoFault(EOFaultHandler handler) {
		super.turnIntoFault(handler);
		cleanSBDict();
	}
}
