// CourseTimeout.java

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

import net.rujel.interfaces.*;
import net.rujel.eduresults.EduPeriod;

import com.webobjects.foundation.*;
import com.webobjects.eoaccess.EOUtilities;
import com.webobjects.eocontrol.*;

import java.util.logging.Logger;
import java.util.Enumeration;

import net.rujel.reusables.NamedFlags;
import net.rujel.reusables.Various;
import net.rujel.reusables.WOLogLevel;

public class CourseTimeout extends _CourseTimeout  implements Timeout {
	protected static Logger logger = Logger.getLogger("rujel.autoitog");
	
    public CourseTimeout() {
        super();
    }

	public static void init() {
		EOInitialiser.initialiseRelationship("CourseTimeout","eduCourse",false,"courseID","EduCourse").anyInverseRelationship().setPropagatesPrimaryKey(true);
		
		EOInitialiser.initialiseRelationship("CourseTimeout","cycle",false,"eduCycleID","EduCycle").anyInverseRelationship().setPropagatesPrimaryKey(true);
		EOInitialiser.initialiseRelationship("CourseTimeout","teacher",false,"teacherID","Teacher").anyInverseRelationship().setPropagatesPrimaryKey(true);
		EOInitialiser.initialiseRelationship("CourseTimeout","eduGroup",false,"eduGroupID","EduGroup").anyInverseRelationship().setPropagatesPrimaryKey(true);
		
		//EOInitialiser.initialiseRelationship("CourseTimeout","eduPeriod",false,"periodID","EduPeriod").anyInverseRelationship().setPropagatesPrimaryKey(true);
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
/*
    public EduPeriod eduPeriod() {
        return (EduPeriod)storedValueForKey("eduPeriod");
    }
	
    public void setEduPeriod(EduPeriod aValue) {
        takeStoredValueForKey(aValue, "eduPeriod");
    }*/
	
    public EduCourse eduCourse() {
        return (EduCourse)storedValueForKey("eduCourse");
    }
	
    public void awakeFromInsertion(EOEditingContext ec) {
    	super.awakeFromInsertion(ec);
    	setFlags(new Integer(0));
    }

    public void setEduCourse(EduCourse aValue) {
        takeStoredValueForKey(aValue, "eduCourse");
    }
	
	public EduCycle cycle() {
        return (EduCycle)storedValueForKey("cycle");
    }
	
    public void setCycle(EduCycle aValue) {
        takeStoredValueForKey(aValue, "cycle");
    }
	
	
    public Teacher teacher() {
        return (Teacher)storedValueForKey("teacher");
    }
	
    public void setTeacher(Teacher aValue) {
        takeStoredValueForKey(aValue, "teacher");
    }
	
    public EduGroup eduGroup() {
        return (EduGroup)storedValueForKey("eduGroup");
    }
	
    public void setEduGroup(EduGroup aValue) {
        takeStoredValueForKey(aValue, "eduGroup");
    }
    /*
    public void setDueDate(NSTimestamp newDate) {
    	NSTimestamp oldDate = dueDate();
    	super.setDueDate(newDate);
    	if(oldDate == null || oldDate.compare(newDate) > 0) {
    		updatePrognoses();
    	} else {
    		relatedPrognoses().takeValueForKey(newDate, "laterFireDate");
    }*/
    
    private NamedFlags _flags;
    public NamedFlags namedFlags() {
    	if(_flags==null) {
    		_flags = new NamedFlags(flags().intValue(),flagNames);
    		try{
    		_flags.setSyncParams(this, getClass().getMethod("setNamedFlags", NamedFlags.class));
    		} catch (Exception e) {
    			Logger.getLogger("rujel.autoitog").log(WOLogLevel.WARNING,
						"Could not get syncMethod for CourseTimeout flags",e);
			}
    	}
    	return _flags;
    }
    
    public void setNamedFlags(NamedFlags flags) {
    	_flags = flags;
    	setFlags(flags.toInteger());
    }

    public static CourseTimeout getTimeoutForCourseAndPeriod(EduCourse course, EduPeriod period) {
    	if(course == null || period == null)
    		return null;
    	NSDictionary dict = new NSDictionary (new Object[] {course,period},
    			new String[] {"eduCourse","eduPeriod"});
		NSArray timeouts = EOUtilities.objectsMatchingValues(course.editingContext(), "CourseTimeout", dict);
		//objectsMatchingKeyAndValue(course.editingContext(), "CourseTimeout", "eduCourse", course);
		if(timeouts.count() > 0) {
			if(timeouts.count() > 1) {
				logger.log(WOLogLevel.WARNING,"Multple timeouts found for course",course);
			}
			return (CourseTimeout)timeouts.objectAtIndex(0);
		}
		timeouts = CourseTimeout.timeoutsForCourseAndPeriod(course,period);
		if(timeouts == null || timeouts.count() == 0) {
			return null;
		}
		CourseTimeout _courseTimeout = null;
		if(timeouts.count() == 1) {
			_courseTimeout = (CourseTimeout)timeouts.objectAtIndex(0);
		} else {
			Enumeration enu = timeouts.objectEnumerator();
			NSTimestamp maxDate = null;
			NSTimestamp minDate = null;
			CourseTimeout maxT = null;
			CourseTimeout minT = null;
			while (enu.hasMoreElements()) {
				CourseTimeout curT = (CourseTimeout) enu.nextElement();
				NSTimestamp curDate = curT.dueDate();
				if(curT.namedFlags().flagForKey("negative")) {
					if(minDate == null || curDate.compare(minDate) < 0) {
						minT = curT;
						minDate = curDate;
					}
				} else {
					if(maxDate == null || curDate.compare(maxDate) > 0) {
						maxT = curT;
						maxDate = curDate;
					}
				}
			}
			if(minT == null) {
				_courseTimeout = maxT;
			} else if (maxT == null) {
				_courseTimeout = minT;
			} else {
				if(minT.namedFlags().flagForKey("priority")) {
					_courseTimeout = minT;
				} else {
					_courseTimeout = maxT;
				}
			}
		}
		return _courseTimeout;
	}

	public NSArray relatedPrognoses() {
		if(eduCourse() != null) {
	    	NSDictionary dict = new NSDictionary(
	    			new Object[] {eduCourse(),eduPeriod()},
	    			new String[] {"eduCourse","eduPeriod"});
	    	return EOUtilities.objectsMatchingValues(editingContext(), "Prognosis", dict);
		}
		NSArray relatedCourses = relatedCourses();
		if(relatedCourses == null || relatedCourses.count() == 0)
			return null;
		NSMutableArray quals = new NSMutableArray
				(new EOKeyValueQualifier("eduPeriod",EOQualifier.QualifierOperatorEqual,eduPeriod()));
		quals.addObject(Various.getEOInQualifier("eduCourse", relatedCourses));
		EOQualifier qual = new EOAndQualifier(quals);
		EOFetchSpecification fs = new EOFetchSpecification("Prognosis",qual,null);
		return editingContext().objectsWithFetchSpecification(fs);
	}
	
	public NSArray relatedCourses() {
		if(eduCourse() != null)
			return new NSArray(eduCourse());
		NSMutableDictionary dict = new NSMutableDictionary
				(eduPeriod().eduYear(),"eduYear");
		dict.takeValueForKey(cycle(), "cycle");
		dict.takeValueForKey(eduGroup(), "eduGroup");
		dict.takeValueForKey(teacher(), "teacher");
    	NSArray courses = EOUtilities.objectsMatchingValues(editingContext(), EduCourse.entityName, dict);
    	Enumeration enu = courses.objectEnumerator();
    	NSMutableArray result = new NSMutableArray();
    	while (enu.hasMoreElements()) {
			EduCourse course = (EduCourse) enu.nextElement();
			CourseTimeout curTimeout = CourseTimeout.getTimeoutForCourseAndPeriod(course, eduPeriod());
			if(curTimeout == this)
				result.addObject(course);
		}
    	return result;
	}

	public static NSArray timeoutsForCourseAndPeriod(EduCourse course, EduPeriod period) {
		EOQualifier qual = qualifierForCourseAndPeriod(course,period);
		EOFetchSpecification fs = new EOFetchSpecification("CourseTimeout",qual,null);
		return course.editingContext().objectsWithFetchSpecification(fs);
	}
	
	public static EOQualifier qualifierForCourseAndPeriod(EduCourse course, EduPeriod period) {
		EOQualifier qual = new EOKeyValueQualifier("eduCourse", EOQualifier.QualifierOperatorEqual , null);
		NSMutableArray allQuals = new NSMutableArray(qual);
		NSMutableArray quals = new NSMutableArray();
		
		quals.addObject(new EOKeyValueQualifier("cycle",EOQualifier.QualifierOperatorEqual,null));
		quals.addObject(new EOKeyValueQualifier("cycle",EOQualifier.QualifierOperatorEqual,course.cycle()));
		allQuals.addObject(new EOOrQualifier(quals));
		quals.removeAllObjects();
		
		quals.addObject(new EOKeyValueQualifier("teacher",EOQualifier.QualifierOperatorEqual,null));
		quals.addObject(new EOKeyValueQualifier("teacher",EOQualifier.QualifierOperatorEqual,course.teacher()));
		allQuals.addObject(new EOOrQualifier(quals));
		quals.removeAllObjects();
		
		quals.addObject(new EOKeyValueQualifier("eduGroup",EOQualifier.QualifierOperatorEqual,null));
		quals.addObject(new EOKeyValueQualifier("eduGroup",EOQualifier.QualifierOperatorEqual,course.eduGroup()));
		allQuals.addObject(new EOOrQualifier(quals));
		quals.removeAllObjects();
		
		//allQuals.addObject(new EOKeyValueQualifier("eduCourse", EOQualifier.QualifierOperatorEqual , null));
		/*
		qual = new EOAndQualifier(allQuals);
		//cycle(or null) and teacher(or null) and eduGroup(or null) and course = null
		allQuals.removeAllObjects();
		
		quals.addObject(qual);
		quals.addObject(new EOKeyValueQualifier("eduCourse", EOQualifier.QualifierOperatorEqual , course));
		qual = new EOOrQualifier(quals);*/
		// or course
		if(period != null) {
			//return qual;
		//allQuals.addObject(qual);
		qual = new EOKeyValueQualifier("eduPeriod", EOQualifier.QualifierOperatorEqual , period);
		allQuals.addObject(qual);
		}
		return new EOAndQualifier(allQuals);
		// and eduPeriod
	}
}
