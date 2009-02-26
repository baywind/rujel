//  Reason.java

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

package net.rujel.curriculum;

import java.util.Enumeration;
import java.util.logging.Logger;

import com.webobjects.foundation.*;
import com.webobjects.eocontrol.*;

import net.rujel.interfaces.*;
import net.rujel.reusables.*;

public class Reason extends _Reason {
	public static NSArray flagNames = new NSArray(new String[] {
			"external","-2-","-4","-8-","forEduGroup","forTeacher"});

	public static void init() {
		EOInitialiser.initialiseRelationship(ENTITY_NAME,"eduGroup",false,"eduGroupID","EduGroup");
		EOInitialiser.initialiseRelationship(ENTITY_NAME,"teacher",false,"teacherID","Teacher");
	}

	public void awakeFromInsertion(EOEditingContext ec) {
		super.awakeFromInsertion(ec);
		Integer school = null;
		if(editingContext() instanceof SessionedEditingContext)
			school = (Integer)valueForKeyPath("editingContext.session.school");
		if(school == null)
			school = new Integer(SettingsReader.intForKeyPath("schoolNumber", 0));
		setSchool(school);
		setFlags(new Integer(0));
	}

	public EduGroup eduGroup() {
		if(namedFlags().flagForKey("forEduGroup"))
			return (EduGroup)storedValueForKey("eduGroup");
		else
			return null;
	}
	
	public void setEduGroup(EduGroup gr) {
		namedFlags().setFlagForKey((gr != null), "forEduGroup");
		takeStoredValueForKey(gr, "eduGroup");
	}
	
	public Teacher teacher() {
		if(namedFlags().flagForKey("forTeacher"))
			return (Teacher)storedValueForKey("teacher");
		else
			return null;
	}
	
	public void setTeacher(Teacher newTeacher) {
		namedFlags().setFlagForKey((newTeacher != null), "forTeacher");
		takeStoredValueForKey(newTeacher, "teacher");
	}
	
	public String title() {
		if(teacher() == null && eduGroup() == null)
			return reason();
		StringBuilder result = new StringBuilder(reason());
		result.append(' ').append('(');
		exts(result);
		result.append(')');
		return result.toString();
	}
	
    private NamedFlags _flags;
    public NamedFlags namedFlags() {
    	if(_flags==null) {
    		_flags = new NamedFlags(flags().intValue(),flagNames);
    		try{
    		_flags.setSyncParams(this, getClass().getMethod("setNamedFlags", NamedFlags.class));
    		} catch (Exception e) {
    			Logger.getLogger("rujel.curriculum").log(WOLogLevel.WARNING,
						"Could not get syncMethod for Reason flags",e);
			}
    	}
    	return _flags;
    }
    
    public void setNamedFlags(NamedFlags flags) {
    	_flags = flags;
    	setFlags(flags.toInteger());
    }

	public boolean external() {
		return namedFlags().getFlag(0);//flagForKey("external");
	}
	
	protected void exts(StringBuilder result) {
		if(teacher() != null) {
			Person t = teacher().person();
			result.append(t.lastName()).append(' ');
			if(t.firstName() != null) {
				result.append(t.firstName().charAt(0)).append('.');
				if(t.secondName() != null)
				result.append(' ').append(t.secondName().charAt(0)).append('.');
			}
		}
		if(eduGroup() != null) {
			if(teacher() != null)
				result.append(',').append(' ');
			result.append(eduGroup().name());
		}		
	}
	
	public String extToString() {
		if(teacher() == null && eduGroup() == null)
			return null;
		StringBuilder sb = new StringBuilder(12);
		exts(sb);
		return sb.toString();
	}
	
	public boolean unverified() {
		return (verification() == null || verification().length() == 0);
	}
	
    public String styleClass() {
    	if(external())
    		return "grey";
    	if(unverified())
    		return "ungerade";
    	return "gerade";
    }
	
	public static NSArray reasons (NSTimestamp date, EduCourse course, boolean hideExternal) {
		EOQualifier qual = null;
		NSMutableArray quals = new NSMutableArray();
		if(course.teacher() == null) {
			qual = new EOKeyValueQualifier(FLAGS_KEY,
					EOQualifier.QualifierOperatorGreaterThanOrEqualTo,new Integer(32));
			quals.addObject(qual);
			qual = new EOKeyValueQualifier("teacher",
					EOQualifier.QualifierOperatorEqual,NullValue);
			quals.addObject(qual);
			qual = new EOAndQualifier(quals);
			quals.removeAllObjects();
		} else {
			qual = new EOKeyValueQualifier("teacher",
					EOQualifier.QualifierOperatorEqual,course.teacher());
		}
		quals.addObject(qual);
		qual = new EOKeyValueQualifier("eduGroup",
				EOQualifier.QualifierOperatorEqual,course.eduGroup());
		quals.addObject(qual);
		qual = new EOKeyValueQualifier(FLAGS_KEY,
				EOQualifier.QualifierOperatorLessThan,new Integer(16));
		quals.addObject(qual);
		qual = new EOOrQualifier(quals);
		quals.removeAllObjects();
		quals.addObject(qual);
		
		qual = new EOKeyValueQualifier(SCHOOL_KEY,
				EOQualifier.QualifierOperatorEqual,course.cycle().school());
		quals.addObject(qual);
		qual = new EOKeyValueQualifier(BEGIN_KEY,
				EOQualifier.QualifierOperatorLessThanOrEqualTo,date);
		quals.addObject(qual);
		EOQualifier[] ors = new EOQualifier[2];
		ors[0] = new EOKeyValueQualifier(END_KEY,
				EOQualifier.QualifierOperatorGreaterThanOrEqualTo,date);
		ors[1] = new EOKeyValueQualifier(END_KEY,
				EOQualifier.QualifierOperatorEqual,NullValue);
		qual = new EOOrQualifier(new NSArray(ors));
		quals.addObject(qual);

		qual = new EOAndQualifier(quals);
		EOFetchSpecification fs = new EOFetchSpecification(
				ENTITY_NAME,qual,EOPeriod.sorter);
		NSArray found = course.editingContext().objectsWithFetchSpecification(fs);
		if(found != null && found.count() > 0) {
			Enumeration enu = found.objectEnumerator();
			NSMutableArray result = new NSMutableArray();
			while (enu.hasMoreElements()) {
				Reason r = (Reason) enu.nextElement();
				if(hideExternal && r.external())
					continue;
				if(r.namedFlags().flagForKey("forEduGroup") && r.eduGroup() != course.eduGroup())
					continue;
				else if(r.namedFlags().flagForKey("forTeacher") && r.teacher() != course.teacher())
					continue;
				else
					result.addObject(r);
			}
			return result;
		}
		return found;
	}
}
