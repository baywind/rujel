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
import net.rujel.base.MyUtility;

import com.webobjects.foundation.*;
import com.webobjects.appserver.WOApplication;
import com.webobjects.eoaccess.EOUtilities;
import com.webobjects.eocontrol.*;

import java.text.DateFormat;
import java.text.FieldPosition;
import java.text.Format;
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
		EOInitialiser.initialiseRelationship("CourseTimeout","course",false,"courseID","EduCourse").anyInverseRelationship().setPropagatesPrimaryKey(true);
		
		EOInitialiser.initialiseRelationship("CourseTimeout","cycle",false,"cycleID","EduCycle").anyInverseRelationship().setPropagatesPrimaryKey(true);
		EOInitialiser.initialiseRelationship("CourseTimeout","teacher",false,"teacherID","Teacher").anyInverseRelationship().setPropagatesPrimaryKey(true);
		EOInitialiser.initialiseRelationship("CourseTimeout","eduGroup",false,"eduGroupID","EduGroup").anyInverseRelationship().setPropagatesPrimaryKey(true);
		
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
	
    public EduCourse course() {
        return (EduCourse)storedValueForKey("course");
    }
	
    public void awakeFromInsertion(EOEditingContext ec) {
    	super.awakeFromInsertion(ec);
    	setFlags(new Integer(0));
    }

    public void setCourse(EduCourse aValue) {
        takeStoredValueForKey(aValue, "course");
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

    public static CourseTimeout getTimeoutForCourseAndPeriod(EduCourse course, AutoItog period) {
    	if(course == null || period == null)
    		return null;
    	NSDictionary dict = new NSDictionary (new Object[] {course,period},
    			new String[] {"course",AUTO_ITOG_KEY});
		NSArray timeouts = EOUtilities.objectsMatchingValues(course.editingContext(), ENTITY_NAME, dict);
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
		return chooseOne(timeouts);
	}

    public static CourseTimeout chooseOne(NSArray timeouts) {
    	if(timeouts == null || timeouts.count() == 0)
    		return null;
    	if(timeouts.count() == 1)
    		return (CourseTimeout)timeouts.objectAtIndex(0);
		Enumeration enu = timeouts.objectEnumerator();
		NSTimestamp maxDate = null;
		NSTimestamp minDate = null;
		CourseTimeout maxT = null;
		CourseTimeout minT = null;
		while (enu.hasMoreElements()) {
			CourseTimeout curT = (CourseTimeout) enu.nextElement();
			NSTimestamp curDate = curT.fireDate();
			if(curT.namedFlags().flagForKey("negative")) {
				if(minDate == null || curDate.compare(minDate) < 0
						|| curT.namedFlags().flagForKey("priority")) {
					minT = curT;
					minDate = curDate;
				}
			} else {
				if(maxDate == null || curDate.compare(maxDate) > 0
						|| curT.namedFlags().flagForKey("priority")) {
					maxT = curT;
					maxDate = curDate;
				}
			}
		}
		if(minT == null) {
			return maxT;
		}
		if (maxT == null) {
			return minT;
		}
		if(minT.namedFlags().flagForKey("priority")) {
			return minT;
		} else {
			return maxT;
		}
    }
    
	public NSArray relatedPrognoses() {
		if(course() != null) {
	    	NSDictionary dict = new NSDictionary(
	    			new Object[] {course(),autoItog()},
	    			new String[] {"course",AUTO_ITOG_KEY});
	    	return EOUtilities.objectsMatchingValues(editingContext(), "Prognosis", dict);
		}
		NSArray relatedCourses = relatedCourses();
		if(relatedCourses == null || relatedCourses.count() == 0)
			return null;
		NSMutableArray quals = new NSMutableArray
				(new EOKeyValueQualifier(AUTO_ITOG_KEY,EOQualifier.QualifierOperatorEqual,autoItog()));
		quals.addObject(Various.getEOInQualifier("course", relatedCourses));
		EOQualifier qual = new EOAndQualifier(quals);
		EOFetchSpecification fs = new EOFetchSpecification("Prognosis",qual,null);
		return editingContext().objectsWithFetchSpecification(fs);
	}
	
	public EOQualifier courseQualifier() {
		if(course() != null)
			return EOUtilities.qualifierForEnterpriseObject(editingContext(), course());
		NSMutableArray quals = new NSMutableArray(new EOKeyValueQualifier("eduYear",
				EOQualifier.QualifierOperatorEqual,autoItog().itogContainer().eduYear()));
		if(cycle() != null)
			quals.addObject(new EOKeyValueQualifier("cycle",
				EOQualifier.QualifierOperatorEqual,cycle()));
		if(eduGroup() != null)
			quals.addObject(new EOKeyValueQualifier("eduGroup",
				EOQualifier.QualifierOperatorEqual,eduGroup()));
		if(teacher() != null)
			quals.addObject(new EOKeyValueQualifier("teacher",
				EOQualifier.QualifierOperatorEqual,teacher()));
		return new EOAndQualifier(quals);
	}
	
	public NSArray relatedCourses() {
		if(course() != null)
			return new NSArray(course());
		EOQualifier qual = courseQualifier();
		EOFetchSpecification fs = new EOFetchSpecification(EduCourse.entityName,qual,null);
    	NSArray courses = editingContext().objectsWithFetchSpecification(fs);
    	Enumeration enu = courses.objectEnumerator();
    	NSMutableArray result = new NSMutableArray();
    	while (enu.hasMoreElements()) {
			EduCourse course = (EduCourse) enu.nextElement();
			CourseTimeout curTimeout = CourseTimeout.getTimeoutForCourseAndPeriod(course, autoItog());
			if(curTimeout == this)
				result.addObject(course);
		}
    	return result;
	}

	public static NSArray timeoutsForCourseAndPeriod(EduCourse course, AutoItog period) {
		EOQualifier qual = qualifierForCourseAndPeriod(course,period);
		EOFetchSpecification fs = new EOFetchSpecification("CourseTimeout",qual,null);
		return course.editingContext().objectsWithFetchSpecification(fs);
	}
	
	public static EOQualifier qualifierForCourseAndPeriod(EduCourse course, AutoItog period) {
		EOQualifier qual = new EOKeyValueQualifier("course", EOQualifier.QualifierOperatorEqual , null);
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
		
		// or course
		qual = new EOKeyValueQualifier("course", EOQualifier.QualifierOperatorEqual , course);
		quals.addObject(qual);
		quals.addObject(new EOAndQualifier(allQuals));
		qual = new EOOrQualifier(quals);
		
		// and AutoItog
		if(period != null) {
			allQuals.removeAllObjects();
			allQuals.addObject(qual);
			qual = new EOKeyValueQualifier(AUTO_ITOG_KEY, EOQualifier.QualifierOperatorEqual , period);
			allQuals.addObject(qual);
			qual = new EOAndQualifier(allQuals);
		}
		return qual;
	}
	
	public NSMutableDictionary extItog(EduCycle cycle) {
		NSMutableDictionary result = new NSMutableDictionary(autoItog(),AUTO_ITOG_KEY);
		StringBuffer buf = new StringBuffer((String)WOApplication.application()
				.valueForKeyPath("strings.RujelAutoItog_AutoItog.ui.generalTimeout"));
		buf.append(' ').append((String)WOApplication.application()
				.valueForKeyPath("strings.RujelAutoItog_AutoItog.ui.upTo"));
		buf.append(' ');
		Format df = MyUtility.dateFormat();
		df.format(fireDate(), buf, new FieldPosition(DateFormat.DATE_FIELD));
		buf.append(" : <em>").append(reason()).append("</em>");
		if(course() == null) {
//			if(cycle() != null)
//				buf.append(" (").append(cycle().subject()).append(')');
			if(eduGroup() != null)
				buf.append(" (").append(eduGroup().name()).append(')');
			if(teacher() != null)
				buf.append(" <span style=\"white-space:nowrap;\">(").append(Person.Utility.fullName(teacher(), true, 2, 1, 1)).append(")</span>");
		}
		result.takeValueForKey(buf.toString(), "text");
		result.takeValueForKey(cycle, "cycle");
		return result;
	}
}
