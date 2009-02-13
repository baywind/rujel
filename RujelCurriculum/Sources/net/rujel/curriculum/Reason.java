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

import com.webobjects.foundation.*;
import com.webobjects.eocontrol.*;

import net.rujel.interfaces.*;
import net.rujel.reusables.*;

public class Reason extends _Reason {

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
		setExternal(Boolean.FALSE);
	}

	public EduGroup eduGroup() {
		return (EduGroup)storedValueForKey("eduGroup");
	}
	
	public void setEduGroup(EduGroup gr) {
		takeStoredValueForKey(gr, "eduGroup");
	}
	
	public Teacher teacher() {
		return (Teacher)storedValueForKey("teacher");
	}
	
	public void setTeacher(Teacher newTeacher) {
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
    	if(external().booleanValue())
    		return "grey";
    	if(unverified())
    		return "ungerade";
    	return "gerade";
    }
	
	public static NSArray reasons (NSTimestamp date, EduCourse course, boolean hideExternal) {
		EOQualifier qual = new EOKeyValueQualifier(SCHOOL_KEY,
				EOQualifier.QualifierOperatorEqual,course.cycle().school());
		NSMutableArray quals = new NSMutableArray(qual);
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
		// specs
		ors[0] = new EOKeyValueQualifier("teacher",
				EOQualifier.QualifierOperatorEqual,course.teacher());
		ors[1] = new EOKeyValueQualifier("teacher",
				EOQualifier.QualifierOperatorEqual,NullValue);
		qual = new EOOrQualifier(new NSArray(ors));
		quals.addObject(qual);
		ors[0] = new EOKeyValueQualifier("eduGroup",
				EOQualifier.QualifierOperatorEqual,course.eduGroup());
		ors[1] = new EOKeyValueQualifier("eduGroup",
				EOQualifier.QualifierOperatorEqual,NullValue);
		qual = new EOOrQualifier(new NSArray(ors));
		quals.addObject(qual);
		if(hideExternal) {
			qual = new EOKeyValueQualifier(EXTERNAL_KEY,
					EOQualifier.QualifierOperatorEqual,Boolean.FALSE);
			quals.add(qual);
		}
		qual = new EOAndQualifier(quals);
		EOFetchSpecification fs = new EOFetchSpecification(
				ENTITY_NAME,qual,EOPeriod.sorter);
		return course.editingContext().objectsWithFetchSpecification(fs);
	}
}
