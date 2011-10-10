//  Variation.java

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

import com.webobjects.eoaccess.EOUtilities;
import com.webobjects.eocontrol.*;
import com.webobjects.foundation.*;

import net.rujel.base.MyUtility;
import net.rujel.interfaces.*;

public class Variation extends _Variation implements Reason.Event {

	public static void init() {
		EOInitialiser.initialiseRelationship(ENTITY_NAME,"course",false,"courseID","EduCourse");
		EOInitialiser.initialiseRelationship(ENTITY_NAME,"relatedLesson",false,
				"lessonID","EduLesson");
	}

	public void awakeFromInsertion(EOEditingContext ec) {
		super.awakeFromInsertion(ec);
		setValue(new Integer(0));
	}
 
	public EduCourse course() {
		return (EduCourse)storedValueForKey("course");
	}
	
	public void setCourse(EduCourse newCourse) {
		takeStoredValueForKey(newCourse, "course");
	}
	
	public EduLesson relatedLesson() {
		return (EduLesson)storedValueForKey("relatedLesson");
	}
	
	public void setRelatedLesson(EduLesson lesson) {
		takeStoredValueForKey(lesson, "relatedLesson");
		if(lesson == null)
			return;
		setDate(lesson.date());
		if(course() == null) {
			setCourse(lesson.course());
			setValue(new Integer(1));
		} else {
			setValue(new Integer((course() == lesson.course())?1:-1));
		}
	}

	public static NSArray variations(EduCourse course, 
			NSTimestamp begin, NSTimestamp end) {
		EOQualifier qual = new EOKeyValueQualifier("course",
				EOQualifier.QualifierOperatorEqual, course);
		NSMutableArray quals = new NSMutableArray(qual);
		if(begin != null) {
			qual = new EOKeyValueQualifier(DATE_KEY,
					EOQualifier.QualifierOperatorGreaterThanOrEqualTo,begin);
			quals.addObject(qual);
		}
		if(end != null) {
			qual = new EOKeyValueQualifier(DATE_KEY,
					EOQualifier.QualifierOperatorLessThanOrEqualTo,end);
			quals.addObject(qual);
		}
		qual = new EOAndQualifier(quals);
		EOFetchSpecification fs = new EOFetchSpecification(ENTITY_NAME,qual,MyUtility.dateSorter);
		fs.setRefreshesRefetchedObjects(true);
		NSArray vars = course.editingContext().objectsWithFetchSpecification(fs);
//		if(vars == null)
//			vars = NSArray.EmptyArray;
		return vars;
	}
	
	public static int totalVariations(EduCourse course, NSTimestamp begin, NSTimestamp end) {
		NSArray vars = variations(course, begin, end);
		if(vars == null || vars.count() == 0)
			return 0;
		Number result = (Number)vars.valueForKeyPath("@sum.value");
		return (result == null)?0:result.intValue();
	}
	
/*	public static int planWithVariations(EduCourse course, EduPeriod period, NSTimestamp toDate) {
		int planOnly =  PlanCycle.planHoursForCourseAndPeriod(course, period);
		if(planOnly == 0)
			return 0;
		planOnly = PlanCycle.planLessonsInPeriod(period, planOnly, toDate);
		if(planOnly == 0)
			return 0;		
		NSArray vars = variations(course, period.begin(), period.end(), Boolean.TRUE);
		if(vars == null || vars.count() == 0)
			return planOnly;
		Number shift = (Number)vars.valueForKeyPath("@sum.value");
		if(shift != null)
			planOnly += shift.intValue();
		return planOnly;
	}
	
	public static int yearPlanWithVariations(EduCourse course, 
			PeriodType perType, NSTimestamp toDate) {
		int planOnly =  PlanCycle.wholeYearPlanLessons(course, perType, toDate);
		if(planOnly == 0)
			return 0;		
		NSArray vars = variations(course, null, toDate, Boolean.TRUE);
		if(vars == null || vars.count() == 0)
			return planOnly;
		Number shift = (Number)vars.valueForKeyPath("@sum.value");
		if(shift != null)
			planOnly += shift.intValue();
		return planOnly;
	}*/
	
    public String valueStyle() {
    	if(reason().flags().intValue() == 1)
    		return "text-align:center;color:#333333;";
    	if(value().intValue() > 0) {
    		return "font-weight:bold;text-align:left;color:#009933;";
    	}
    	if(value().intValue() < 0) {
       		return "font-weight:bold;text-align:right;color:#cc3333;";
    	}
    	return "text-align:center;";
    }
    /*
    public NSArray getAllPaired(boolean byReason) {
    	Integer value = value();
    	if(value == null || value.intValue() == 0)
    		return null;
    	EOQualifier[] quals = new EOQualifier[4];
    	if(byReason)
    		quals[0] = new EOKeyValueQualifier(REASON_KEY,
    				EOQualifier.QualifierOperatorEqual, reason());
    	quals[1] = new EOKeyValueQualifier(DATE_KEY,
    			EOQualifier.QualifierOperatorEqual, date());
    	quals[2] = new EOKeyValueQualifier(VALUE_KEY,(value.intValue() < 0)?
    		EOQualifier.QualifierOperatorGreaterThan:EOQualifier.QualifierOperatorLessThan
    			, new Integer(0));
    	quals[3] = new EOKeyValueQualifier("course",
    			EOQualifier.QualifierOperatorNotEqual, course());
    	quals[0] = new EOAndQualifier(new NSArray(quals));
    	EOFetchSpecification fs = new EOFetchSpecification(ENTITY_NAME,quals[0],null);
    	NSArray found = editingContext().objectsWithFetchSpecification(fs);
    	if(found == null || found.count() == 0)
    		return null;
    	quals[1] = new EOKeyValueQualifier("course.eduGroup",
    			EOQualifier.QualifierOperatorEqual,course().eduGroup());
    	found = EOQualifier.filteredArrayWithQualifier(found, quals[1]);
       	if(found == null || found.count() == 0)
       		return null;
       	return found;
    }*/
    
    public Variation getPaired() {
    	EduCourse course = course();
    	EduLesson lesson = relatedLesson();
    	if(course == null || lesson == null || lesson.editingContext() == null)
    		return null;
    	NSArray args = new NSArray(new Object[] {lesson,course});
    	NSArray found = EOUtilities.objectsWithQualifierFormat(editingContext(), ENTITY_NAME, 
    			"relatedLesson = %@ and course != %@", args);
    	//getAllPaired(true);
       	if(found == null || found.count() == 0)
       		return null;
       	if(found.count() > 0) {
       		int val = -value().intValue();
       		for (int i = 0; i < found.count(); i++) {
       			Variation var = (Variation)found.objectAtIndex(i);
       			if(var.value().intValue() == val)
       				return var;
       		}
       	}
       	return (Variation)found.objectAtIndex(0);
     }
}
