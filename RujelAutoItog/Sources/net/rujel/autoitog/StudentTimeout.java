// StudentTimeout.java

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

package net.rujel.autoitog;


import java.text.DateFormat;
import java.text.FieldPosition;
import java.text.Format;
import java.util.Enumeration;
import java.util.logging.Logger;

import com.webobjects.foundation.*;
import com.webobjects.appserver.WOApplication;
import com.webobjects.eoaccess.EOUtilities;
import com.webobjects.eocontrol.*;

import net.rujel.base.MyUtility;
import net.rujel.interfaces.*;
import net.rujel.reusables.*;

public class StudentTimeout extends _StudentTimeout implements Timeout
{

	public static void init() {
		EOInitialiser.initialiseRelationship("StudentTimeout","course",false,"courseID","EduCourse");//.anyInverseRelationship().setPropagatesPrimaryKey(true);
		EOInitialiser.initialiseRelationship("StudentTimeout","student",false,"studentID","Student");//.anyInverseRelationship().setPropagatesPrimaryKey(true);
	}

    public StudentTimeout() {
        super();
    }

    public void awakeFromInsertion(EOEditingContext ec) {
    	super.awakeFromInsertion(ec);
    	setFlags(new Integer(0));
    }

    public Student student() {
        return (Student)storedValueForKey("student");
    }
	
    public void setStudent(Student aValue) {
        takeStoredValueForKey(aValue, "student");
    }
	
    public EduCourse course() {
        return (EduCourse)storedValueForKey("course");
    }
	
    public void setCourse(EduCourse aValue) {
        takeStoredValueForKey(aValue, "course");
    }

    private NamedFlags _flags;
    public NamedFlags namedFlags() {
    	if(_flags==null) {
    		_flags = new NamedFlags(flags().intValue(),flagNames);
    		try{
    		_flags.setSyncParams(this, getClass().getMethod("setNamedFlags", NamedFlags.class));
    		} catch (Exception e) {
    			Logger.getLogger("rujel.autoitog").log(WOLogLevel.WARNING,
						"Could not get syncMethod for StudentTimeout flags",e);
			}
    	}
    	return _flags;
    }
    
    public void setNamedFlags(NamedFlags flags) {
    	_flags = flags;
    	setFlags(flags.toInteger());
    }

    public NSArray relatedPrognoses() {
    	EduCourse c = course();
    	NSDictionary dict = new NSMutableDictionary(
    			new Object[] {student(),autoItog()},
    			new String[] {"student",Prognosis.AUTO_ITOG_KEY});
    		dict.takeValueForKey(c, "course");
    	return EOUtilities.objectsMatchingValues(editingContext(), Prognosis.ENTITY_NAME, dict);
    }

	
	public boolean allCycles() {
		return (course() == null);
	}
	
	public static NSArray timeoutsForCourseAndPeriod(Student student,EduCourse course,AutoItog period) {
		EOQualifier qual = new EOKeyValueQualifier("course",EOQualifier.QualifierOperatorEqual,NullValue);
		NSMutableArray quals = new NSMutableArray(qual);
		if(course != null) {
			qual = new EOKeyValueQualifier("course",EOQualifier.QualifierOperatorEqual,course);
			quals.addObject(qual);
			qual = new EOOrQualifier(quals);
			quals.removeAllObjects();
			quals.addObject(qual);
		}
		qual = new EOKeyValueQualifier(AUTO_ITOG_KEY,EOQualifier.QualifierOperatorEqual,period);
		quals.addObject(qual);
		if(student == null)
			qual = Various.getEOInQualifier("student",course.eduGroup().list());
		else
			qual = new EOKeyValueQualifier("student",EOQualifier.QualifierOperatorEqual,student);
		quals.addObject(qual);
		qual = new EOAndQualifier(quals);
		EOFetchSpecification fs = new EOFetchSpecification("StudentTimeout",qual,null);
		NSArray found = course.editingContext().objectsWithFetchSpecification(fs);
		return found;
		/*NSDictionary result = new NSDictionary(found,(NSArray)found.valueForKey("student"));
		return new PerPersonLink.Dictionary(result);*/
	}
	
	public static StudentTimeout timeoutForStudentCourseAndPeriod(Student student,EduCourse course,AutoItog period) {
		if(student == null || course == null || period == null)
			throw new IllegalArgumentException("Non null arguments required");
		NSArray timeouts = timeoutsForCourseAndPeriod(student,course, period);
		if(timeouts == null || timeouts.count() == 0) {
			return null;
		}
		if(timeouts.count() > 1) {
			Enumeration enu = timeouts.objectEnumerator();
			while (enu.hasMoreElements()) {
				StudentTimeout tout = (StudentTimeout) enu.nextElement();
				if(tout.student() == student) {
					return tout;
				}
			}
		}
		return (StudentTimeout)timeouts.objectAtIndex(0);
	}
	
	public static StudentTimeout activeTimeout(Student student, EduCourse course, NSTimestamp date) {
		EOQualifier qual = new EOKeyValueQualifier("course",EOQualifier.QualifierOperatorEqual,NullValue);
		NSMutableArray quals = new NSMutableArray(qual);
		if(course != null) {
			qual = new EOKeyValueQualifier("course",EOQualifier.QualifierOperatorEqual,course);
			quals.addObject(qual);
			qual = new EOOrQualifier(quals);
			quals.removeAllObjects();
			quals.addObject(qual);
		}
		qual = new EOKeyValueQualifier(FIRE_DATE_KEY
				,EOQualifier.QualifierOperatorGreaterThanOrEqualTo,date);
		quals.addObject(qual);
		if(student == null)
			qual = Various.getEOInQualifier("student",course.eduGroup().list());
		else
			qual = new EOKeyValueQualifier("student",EOQualifier.QualifierOperatorEqual,student);
		quals.addObject(qual);
		qual = new EOAndQualifier(quals);
		NSArray sort = new NSArray(new EOSortOrdering(FIRE_DATE_KEY,EOSortOrdering.CompareAscending));
		EOFetchSpecification fs = new EOFetchSpecification("StudentTimeout",qual,sort);
		NSArray found = course.editingContext().objectsWithFetchSpecification(fs);
		if(found == null || found.count() == 0)
			return null;
		return (StudentTimeout)found.objectAtIndex(0);		
	}

	public NSMutableDictionary extItog() {
		NSMutableDictionary result = new NSMutableDictionary(autoItog(),AUTO_ITOG_KEY);
		result.takeValueForKey(valueForKeyPath("course.cycle"), "cycle");
		StringBuffer buf = new StringBuffer((String)WOApplication.application()
				.valueForKeyPath("strings.RujelAutoItog_AutoItog.ui.generalTimeout"));
		buf.append(' ').append((String)WOApplication.application()
				.valueForKeyPath("strings.RujelAutoItog_AutoItog.ui.upTo"));
		buf.append(' ');
		Format df = MyUtility.dateFormat();
		df.format(fireDate(), buf, new FieldPosition(DateFormat.DATE_FIELD));
		buf.append(" : <em>").append(reason()).append("</em>");
		result.takeValueForKey(buf.toString(), "text");
		return result;
	}
}
