// ItogMark.java

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

package net.rujel.eduresults;

import java.util.Enumeration;

import net.rujel.reusables.*;
import net.rujel.interfaces.*;

import com.webobjects.foundation.*;
import com.webobjects.appserver.WOApplication;
import com.webobjects.eocontrol.*;
import com.webobjects.eoaccess.EOUtilities;

public class ItogMark extends _ItogMark
{
	public static NSArray flagKeys = new NSArray(new Object[] {"changed","forced","incomplete","manual","constituents"});
	
	public static NSArray localisedFlagKeys() {
		NSDictionary localisation = (NSDictionary)WOApplication.application().valueForKeyPath(
				"strings.RujelEduResults_EduResults.properties.ItogMark.flags");
		if(localisation == null || localisation.count() == 0)
			return flagKeys;
		NSMutableArray result = new NSMutableArray();
		Enumeration enu = flagKeys.objectEnumerator();
		while (enu.hasMoreElements()) {
			String key = (String) enu.nextElement();
			String lKey = (String)localisation.valueForKey(key);
			if(lKey == null)
				lKey = key;
			result.addObject(lKey);
		}
		return result;
	}
	
	public static void init() {
//		EOInitialiser.initialiseRelationship("ItogMark","teacher",false,"teacherID","Teacher");
		EOInitialiser.initialiseRelationship("ItogMark","cycle",false,"eduCycleID","EduCycle").anyInverseRelationship().setPropagatesPrimaryKey(true);
		EOInitialiser.initialiseRelationship("ItogMark","student",false,"studentID","Student").anyInverseRelationship().setPropagatesPrimaryKey(true);
	}
	
    public ItogMark() {
        super();
    }

/*
    // If you add instance variables to store property values you
    // should add empty implementions of the Serialization methods
    // to avoid unnecessary overhead (the properties will be
    // serialized for you in the superclass).
    private void writeObject(java.io.ObjectOutputStream out) throws java.io.IOException {
    }

    private void readObject(java.io.ObjectInputStream in) throws java.io.IOException, java.lang.ClassNotFoundException {
    }
 */

    public void awakeFromInsertion(EOEditingContext ec) {
    	super.awakeFromInsertion(ec);
    	setFlags(new Integer(0));
    	setMark("?");
    }

    public Student student() {
    	return (Student)storedValueForKey("student");
    }

    public void setStudent(Student aValue) {
    	takeStoredValueForKey(aValue, "student");
    }
	
	public EduCycle cycle() {
        return (EduCycle)storedValueForKey("cycle");
    }
	
    public void setCycle(EduCycle aValue) {
        takeStoredValueForKey(aValue, "cycle");
    }

	public void setMark(String aValue) {
		if(aValue == null && value() == null/* && comment() == null*/) {
			editingContext().deleteObject(this);
		} else {
			super.setMark(aValue);
		}
	}
	/*
	public void setComment(String aValue) {
		if(aValue == null && value() == null && mark() == null) {
			editingContext().deleteObject(this);
		} else {
			super.setComment(aValue);
		}
	}*/
	
	private transient NamedFlags _namedFlags;
	public NamedFlags readFlags() {
		if(_namedFlags == null) {
			try {
//				int flags = 0;
//				if(flags() != null) flags = flags().intValue();
				_namedFlags = new NamedFlags(flags().intValue(),flagKeys);
				java.lang.reflect.Method sync = this.getClass().getMethod
					("_syncFlags",new Class[] {Flags.class});
				_namedFlags.setSyncParams(this,sync);
			} catch (Exception ex) {
				throw new NSForwardException(ex,"Error readingflags");
			}
		}
		return _namedFlags;
	}

	public void turnIntoFault(EOFaultHandler handler){
		_namedFlags = null;
		super.turnIntoFault(handler);
	}
	
	public void _syncFlags(Flags toSync) {
		setFlags(new Integer(toSync.intValue()));
	}

	public static EOQualifier qualifyItogMark(EduCycle cycle, EduPeriod period, Student student) {
		EOQualifier qual= null;
		NSMutableArray quals = new NSMutableArray();
		if(cycle != null) {
			qual = new EOKeyValueQualifier("cycle",EOQualifier.QualifierOperatorEqual,cycle);
			if(period == null && student == null)
				return qual;
			quals.addObject(qual);
		}
		if(period != null) {
			qual = new EOKeyValueQualifier("eduPeriod",EOQualifier.QualifierOperatorEqual,period);
			if(cycle == null && student == null)
				return qual;
			quals.addObject(qual);
		}
		if(student != null) {
			qual = new EOKeyValueQualifier("student",EOQualifier.QualifierOperatorEqual,student);
			if(period == null && cycle == null)
				return qual;
			quals.addObject(qual);
		}
		if(quals == null || quals.count() == 0)
			return null;
		qual = new EOAndQualifier(quals);
		return qual;
	}
	
	public static NSArray getItogMarks(EduCycle cycle, EduPeriod period, Student student) {
		EOEditingContext ec = null;
		if(student != null)
			ec = student.editingContext();
		else if(cycle != null)
			ec = cycle.editingContext();
		else if(period != null)
			ec = period.editingContext();
		else
			throw new NullPointerException("At least one of parameters should have value");
		return getItogMarks(cycle,period,student,ec);
	}
		
	public static NSArray getItogMarks(EduCycle cycle, EduPeriod period, Student student, EOEditingContext ec) {
		EOQualifier qual = qualifyItogMark(cycle,period,student);
		if(qual == null)
			throw new NullPointerException("No parameters specified");
		EOFetchSpecification fspec = new EOFetchSpecification("ItogMark",qual,null);
		return ec.objectsWithFetchSpecification(fspec);
	}
	
	public static ItogMark getItogMark(EduCycle cycle, EduPeriod period, Student student, EOEditingContext ec) {
		NSArray result = getItogMarks(cycle,period,student,ec);
		if(result == null || result.count() == 0) return null;
		if(result.count() > 1) {
			throw new EOUtilities.MoreThanOneException("Multiple ItogMarks found");
		}
		return (ItogMark)result.objectAtIndex(0);
	}
	
	public static ItogMark getItogMark(EduCycle cycle, EduPeriod period, Student student, NSArray list) {
		NSArray result = EOQualifier.filteredArrayWithQualifier(list,qualifyItogMark(cycle,period,student));
		if(result == null || result.count() == 0) return null;
		if(result.count() > 1) {
			throw new EOUtilities.MoreThanOneException("Multiple ItogMarks found");
		}
		return (ItogMark)result.objectAtIndex(0);
	}
	
	public EduCourse assumeCourse() {
		EOQualifier[] quals = new EOQualifier[2];
		quals[0] = new EOKeyValueQualifier("eduYear",
				EOQualifier.QualifierOperatorEqual,eduPeriod().eduYear());
		quals[1]  = new EOKeyValueQualifier("cycle",
				EOQualifier.QualifierOperatorEqual,cycle());
		EOQualifier qual = new EOAndQualifier(new NSArray(quals));
		EOFetchSpecification fs = new EOFetchSpecification(EduCourse.entityName,qual,null);
		NSArray found = editingContext().objectsWithFetchSpecification(fs);
		if(found == null || found.count() == 0)
			return null;
		EduCourse result = (EduCourse)found.objectAtIndex(0);
		if(found.count() == 1)
			return result;
		qual = new EOKeyValueQualifier("groupList",
				EOQualifier.QualifierOperatorContains,student());
		found = EOQualifier.filteredArrayWithQualifier(found, qual);
		if(found.count() > 0)
			result = (EduCourse)found.objectAtIndex(0);
		return result;
	}
}