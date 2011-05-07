//  QualifiedSetting.java

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

import net.rujel.interfaces.EduCourse;
import net.rujel.reusables.ModulesInitialiser;
import net.rujel.reusables.PlistReader;
import net.rujel.reusables.Various;
import net.rujel.reusables.WOLogFormatter;
import net.rujel.ui.Parameter;

import com.webobjects.appserver.WOSession;
import com.webobjects.eoaccess.EOUtilities;
import com.webobjects.eocontrol.*;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSKeyValueCodingAdditions;
import com.webobjects.foundation.NSMutableArray;
import com.webobjects.foundation.NSMutableDictionary;
import com.webobjects.foundation.NSPropertyListSerialization;

public class QualifiedSetting extends _QualifiedSetting {

	protected EOQualifier qualifier;
	protected NSArray courses;
	
	public void setQualifierString(String string) {
		nullufy();
		super.setQualifierString(string);
	}
	
	public void setArgumentsString(String string) {
		nullufy();
		super.setArgumentsString(string);
	}
	
	public void setQualifier(EOQualifier qual) {
		StringBuilder buf = new StringBuilder();
		NSMutableArray args = new NSMutableArray();
		Various.formatQualifier(qual, buf, args);
		setQualifierString(buf.toString());
		setArgumentsString(NSPropertyListSerialization.stringFromPropertyList(args,false));
		qualifier = qual;
	}
	
	public void setCourse(EduCourse course) {
		String format = WOLogFormatter.formatEO(course);
		setQualifierString("IS");
		setArgumentsString(format);
		courses = new NSArray(EOUtilities.localInstanceOfObject(editingContext(), course));
	}
	
	public void setCourses(NSArray newCourses) {
		setQualifierString("IN");
		setArgumentsString(Various.stringFromArguments(newCourses));
		courses = EOUtilities.localInstancesOfObjects(editingContext(), newCourses);
	}

	public void addCourse(EduCourse course) {
		course = (EduCourse)EOUtilities.localInstanceOfObject(editingContext(), course);
		if(courses == null)
			courses = new NSArray(course);
		else
			courses = courses.arrayByAddingObject(course);
		super.setQualifierString("IN");
		super.setArgumentsString(Various.stringFromArguments(courses));
		qualifier = null;
	}
	
	public EOQualifier getQualifier() {
		if(courses == null && qualifier == null)
			read();
		return qualifier;
	}
	
	public NSArray getCourses() {
		if(courses == null && qualifier == null)
			read();
		return (courses == null)?null:courses.immutableClone();
	}
	
	protected void read() {
		String qualifierString = qualifierString();
		if(qualifierString.equals("IS")) {
			courses = new NSArray(Various.parseEO(argumentsString(), editingContext()));
			qualifier = null;
		} else {
			courses = Various.argumentsFromString(argumentsString(), editingContext());
			if(qualifierString.equals("IN")) {
				qualifier = null;
			} else {
				qualifier = EOQualifier.qualifierWithQualifierFormat(qualifierString,courses);
				courses = null;
			}
		}
	}
	
	public boolean evaluateWithObject(NSKeyValueCodingAdditions object) {
		if(courses == null && qualifier == null) {
			read();
		}
		if(eduYear() != null && object instanceof EduCourse) {
			object = EOUtilities.localInstanceOfObject(editingContext(),
					(EOEnterpriseObject)object);
			if(!eduYear().equals(((EduCourse)object).eduYear()))
				return false;
		}
		if(courses != null)
			return courses.containsObject(object);
		else if (qualifier != null)
			return qualifier.evaluateWithObject(object);
		else
			return false;
	}
	
	public String printQualifier() {
		String qualifierString = qualifierString();
		if(qualifierString.equals("IS"))
			return "= " + argumentsString();
		if(qualifierString.equals("IN"))
			return "IN " + argumentsString();
		StringBuilder buf = new StringBuilder(qualifierString);
		NSArray list = NSPropertyListSerialization.arrayForString(argumentsString());
		int idx = buf.indexOf("%");
		int i = 0;
		while (idx >= 0) {
			String val = (String)list.objectAtIndex(i);
			if(buf.charAt(idx +1) != '%') {
				buf.replace(idx, idx+2, val);
				i++;
			}
			idx = buf.indexOf("%", idx+2);
		}
		return buf.toString();
	}

	public void awakeFromInsertion(EOEditingContext ec) {
		super.awakeFromInsertion(ec);
		setSort(new Integer(1));
	}

	public void nullufy() {
		courses = null;
		qualifier = null;
	}
	public void turnIntoFault(EOFaultHandler handler) {
		nullufy();
		super.turnIntoFault(handler);
	}
	/*
	public int compare(NSKeyValueCoding other) {
		return 0; // TODO: write comparison
	}*/
	
	public static NSMutableArray editors(WOSession ses) {
		NSArray list = (NSArray)ses.valueForKeyPath("strings.RujelBase_Base.SettingsBase.editors");
		NSMutableArray editors = PlistReader.cloneArray(list, true);
		list = (NSArray)ses.valueForKeyPath("modules.settingEditors");
		editors.addObjectsFromArray(PlistReader.cloneArray(list, true));
		EOSortOrdering.sortArrayUsingKeyOrderArray(editors, ModulesInitialiser.sorter);
		return editors;
	}
	
	public static class AdvancedQualifierException extends Exception {
		private EOQualifier qual;
		public AdvancedQualifierException(EOQualifier q) {
			super();
			qual = q;
		}
		
		public EOQualifier qualifier() {
			return qual;
		}
	}

	public static void analyseQual(EOQualifier qual, NSMutableDictionary params, 
			NSArray editors) throws AdvancedQualifierException {
		if(qual instanceof EOOrQualifier) {
			analyseOR((EOOrQualifier)qual,params,editors);
		} else if (qual instanceof EOKeyValueQualifier) {
			analyseKeyValue((EOKeyValueQualifier)qual, params, editors);
		} else if (qual instanceof EOAndQualifier) {
			analyseAND((EOAndQualifier)qual,params,editors);
		} else {
			throw new AdvancedQualifierException(qual);
		}
	}

	protected static void analyseAND(EOAndQualifier and, NSMutableDictionary params, 
			NSArray editors) throws AdvancedQualifierException {
		Enumeration enu = and.qualifiers().objectEnumerator();
		while (enu.hasMoreElements()) {
			EOQualifier qual = (EOQualifier) enu.nextElement();
			analyseQual(qual,params,editors);
		}
	}

	protected static NSMutableDictionary analyseOR(EOOrQualifier or, NSMutableDictionary params, 
			NSArray editors) throws AdvancedQualifierException {
		NSMutableDictionary dict = null;
		Enumeration enu = or.qualifiers().objectEnumerator();
		String keyPath = null;
		NSMutableArray values = null;
		while (enu.hasMoreElements()) {
			EOQualifier qual = (EOQualifier) enu.nextElement();
			if(qual instanceof EOKeyValueQualifier) {
				EOKeyValueQualifier kq = (EOKeyValueQualifier)qual;
				if (kq.selector() != EOQualifier.QualifierOperatorEqual)
					throw new AdvancedQualifierException(qual);
				if(dict == null) {
					dict = analyseKeyValue(kq,params,editors);
					if(dict == null || !Various.boolForObject(dict.valueForKey("or")))
						throw new AdvancedQualifierException(qual);
					values = (NSMutableArray)params.valueForKey(Parameter.attribute(dict));
					keyPath = (String)dict.valueForKey("attribute");
					continue;
				}
				if (!checkKeyPath(kq.key(), keyPath))
					throw new AdvancedQualifierException(qual);
				values.addObject(kq.value());
			} else {
				throw new AdvancedQualifierException(qual);
			}
		}
		return dict;
	}

	protected static boolean checkKeyPath(String key, String pattern) {
		if(key == null || pattern == null)
			return false;
		if(pattern.charAt(pattern.length() -1) == '*') {
			pattern = pattern.substring(0,pattern.length() -1);
			return key.startsWith(pattern);
		} else {
			return key.equals(pattern);
		}
	}

	protected static NSMutableDictionary analyseKeyValue(EOKeyValueQualifier qual, 
			NSMutableDictionary params, NSArray editors) throws AdvancedQualifierException {
		Enumeration enu = editors.objectEnumerator();
		while (enu.hasMoreElements()) {
			NSMutableDictionary ed = (NSMutableDictionary) enu.nextElement();
			String keyPath = (String)ed.valueForKey("attribute");
			if(checkKeyPath(qual.key(), keyPath)) {
				keyPath = Parameter.attribute(ed);
				if(qual.selector() == EOQualifier.QualifierOperatorEqual ||
						(EOQualifier.stringForOperatorSelector(qual.selector()).equalsIgnoreCase(
								(String)ed.valueForKey("qualifierSelector")))) {
					if(Various.boolForObject(ed.valueForKey("or"))) {
						NSMutableArray values = (NSMutableArray)params.valueForKey(keyPath);
						if(values == null)
							params.takeValueForKey(new NSMutableArray(qual.value()), keyPath);						
						else
							values.addObject(qual.value());
					} else {
						params.takeValueForKey(qual.value(), keyPath);
					}
					if(Various.boolForObject(ed.valueForKey("range"))) {
						ed.takeValueForKey("=", "qualifierSelector");
						ed.takeValueForKey(null, "secondSelector");
					}
				} else if(qual.selector() == EOQualifier.QualifierOperatorGreaterThanOrEqualTo ||
						qual.selector() == EOQualifier.QualifierOperatorGreaterThan) {
					params.takeValueForKey(qual.value(), "min_" + keyPath);
					if(Various.boolForObject(ed.valueForKey("range")))
						ed.takeValueForKey(">=", "qualifierSelector");
				} else if(qual.selector() == EOQualifier.QualifierOperatorLessThanOrEqualTo ||
						qual.selector() == EOQualifier.QualifierOperatorLessThan) {
					params.takeValueForKey(qual.value(), "max_" + keyPath);
					if(Various.boolForObject(ed.valueForKey("range")))
						ed.takeValueForKey("<=", "secondSelector");
				} else {
					throw new AdvancedQualifierException(qual);
				}
				ed.takeValueForKey(Boolean.TRUE, "active");
				return ed;
			}
		}
		return null;
	}
}
