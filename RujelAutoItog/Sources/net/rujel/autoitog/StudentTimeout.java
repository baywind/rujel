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


import java.util.Enumeration;
import java.util.logging.Logger;

import com.webobjects.foundation.*;
import com.webobjects.eoaccess.EOUtilities;
import com.webobjects.eocontrol.*;

import net.rujel.eduresults.EduPeriod;
import net.rujel.interfaces.*;
import net.rujel.reusables.*;

public class StudentTimeout extends _StudentTimeout implements Timeout
{

	public static void init() {
		EOInitialiser.initialiseRelationship("StudentTimeout","eduCourse",false,"eduCourseID","EduCourse");//.anyInverseRelationship().setPropagatesPrimaryKey(true);
		EOInitialiser.initialiseRelationship("StudentTimeout","student",false,"studentID","Student");//.anyInverseRelationship().setPropagatesPrimaryKey(true);
		/*
		EOInitialiser.initialiseRelationship("GeneralTimeout","eduCourse",false,"eduCourseID","EduCourse");//.anyInverseRelationship().setPropagatesPrimaryKey(true);
		EOInitialiser.initialiseRelationship("GeneralTimeout","student",false,"studentID","Student");//.anyInverseRelationship().setPropagatesPrimaryKey(true);
		*/
		//EOInitialiser.initialiseRelationship("Timeout","eduPeriod",false,"periodID","EduPeriod").anyInverseRelationship().setPropagatesPrimaryKey(true);
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
	
    public EduCourse eduCourse() {
        return (EduCourse)storedValueForKey("eduCourse");
    }
	
    public void setEduCourse(EduCourse aValue) {
        takeStoredValueForKey(aValue, "eduCourse");
    }
	/*
    public void setDueDate(NSTimestamp newDate) {
    	super.setDueDate(newDate);
    	NSDictionary snapshot = editingContext().committedSnapshotForObject(this);
    	NSTimestamp oldDate = null;
    	if(snapshot != null)
    		oldDate = (NSTimestamp)snapshot.valueForKey("dueDate");
    	if(oldDate == null || oldDate.compare(newDate) > 0) {
       		relatedPrognoses().valueForKey("updateFireDate");
    	} else {
    		relatedPrognoses().takeValueForKey(newDate, "laterFireDate");
    	}
    }*/
 
    /*
    public EduPeriod eduPeriod() {
        return (EduPeriod)storedValueForKey("eduPeriod");
    }
	
    public void setEduPeriod(EduPeriod aValue) {
        takeStoredValueForKey(aValue, "eduPeriod");
    }*/
    
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
    	/*
    	if(eduCourse() != null)
    		return prognosis();*/
    	EduCourse c = eduCourse();
    	NSDictionary dict = new NSMutableDictionary(
    			new Object[] {student(),eduPeriod()},
    			new String[] {"student","eduPeriod"});
    	/*if(c==null)
    		dict.takeValueForKey(NullValue, "timeout");
    	else*/
    		dict.takeValueForKey(c, "eduCourse");
    	return EOUtilities.objectsMatchingValues(editingContext(), "Prognosis", dict);
    }

	
	public boolean allCycles() {
		return (eduCourse() == null);
	}
	
	public static NSArray timeoutsForCourseAndPeriod(Student student,EduCourse course,EduPeriod period) {
		EOQualifier qual = new EOKeyValueQualifier("eduCourse",EOQualifier.QualifierOperatorEqual,course);
		NSMutableArray quals = new NSMutableArray(qual);
		qual = new EOKeyValueQualifier("eduCourse",EOQualifier.QualifierOperatorEqual,null);
		quals.addObject(qual);
		qual = new EOOrQualifier(quals);
		quals.removeAllObjects();
		quals.addObject(qual);
		qual = new EOKeyValueQualifier("eduPeriod",EOQualifier.QualifierOperatorEqual,period);
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
	
	public static StudentTimeout timeoutForStudentCourseAndPeriod(Student student,EduCourse course,EduPeriod period) {
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

}
