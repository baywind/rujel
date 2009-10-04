// Substitute.java

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

package net.rujel.curriculum;

import java.math.BigDecimal;
import java.util.Enumeration;
import java.util.logging.Logger;

import com.webobjects.foundation.*;
import com.webobjects.appserver.WOApplication;
import com.webobjects.eoaccess.EOEntity;
import com.webobjects.eoaccess.EORelationship;
import com.webobjects.eoaccess.EOUtilities;
import com.webobjects.eocontrol.*;

import net.rujel.base.MyUtility;
import net.rujel.interfaces.*;
import net.rujel.reusables.*;

public class Substitute extends _Substitute implements Reason.Event {
	protected static Logger logger = Logger.getLogger("rujel.curriculum");
	//public static NSArray flagKeys = new NSArray("join");
		
	public static void init() {
		EORelationship rel = EOInitialiser.initialiseRelationship(
				ENTITY_NAME,"lesson",false,"lessonID","EduLesson").anyInverseRelationship();
		EOEntity ent = rel.entity();
		//EOEntity dest = rel.destinationEntity();
		//NSArray joins = rel.joins();
		ent.removeRelationship(rel);
		//rel = new EORelationship();
		rel.setName("substitutes");
		rel.setToMany(true);
		//rel.addJoin((EOJoin)joins.objectAtIndex(0));
		rel.setPropagatesPrimaryKey(true);
		rel.setOwnsDestination(true);
		rel.setDeleteRule(EOClassDescription.DeleteRuleCascade);	
		rel.setIsMandatory(false);
		ent.addRelationship(rel);
		EOInitialiser.initialiseRelationship(ENTITY_NAME,"fromLesson",false,"fromID","EduLesson")
			.anyInverseRelationship().setPropagatesPrimaryKey(true);
		EOInitialiser.initialiseRelationship("Substitute","teacher",false,"teacherID","Teacher")
			.anyInverseRelationship().setPropagatesPrimaryKey(true);
		//EOInitialiser.initialiseRelationship("Substitute","eduCourse",false,"courseID","EduCourse").
		//		anyInverseRelationship().setPropagatesPrimaryKey(true);
	}
	
/*	public static Substitute substituteForLesson(EduLesson lesson) {
		EOEditingContext ec = lesson.editingContext();
		NSArray substitutes = EOUtilities.objectsMatchingKeyAndValue(ec, Substitute.ENTITY_NAME, 
				"lesson", lesson);
		NSArray substitutes = (NSArray)lesson.valueForKey("substitutes");
		if(substitutes == null || substitutes.count() == 0)
			return null;
		if(substitutes.count() > 1)
			logger.log(WOLogLevel.WARNING,"Multiple substitutes found for lesson",lesson);
		return (Substitute)substitutes.objectAtIndex(0);
	}*/

	public EduLesson lesson() {
		return (EduLesson)storedValueForKey("lesson");
	}
	
	public void setLesson(EduLesson aValue) {
		takeStoredValueForKey(aValue, "lesson");
		if(aValue != null)
			setDate(aValue.date());
	}

	public EduLesson fromLesson() {
		return (EduLesson)storedValueForKey("fromLesson");
	}
	
	public void setFromLesson(EduLesson aValue) {
		takeStoredValueForKey(aValue, "fromLesson");
		if(aValue != null)
			setDate(aValue.date());
	}

	public Teacher teacher() {
		return (Teacher)storedValueForKey("teacher");
	}
	public void setTeacher(Teacher aValue) {
		takeStoredValueForKey(aValue, "teacher");
	}

   public EduCourse course() {
    	return lesson().course();
    }

	public void validateForSave() throws ValidationException { 
		if(teacher() == null || teacher() == lesson().course().teacher()) {
			String message = (String)WOApplication.application().valueForKeyPath(
					"strings.RujelCurriculum_Curriculum.messages.wrongTeacher");
			throw new ValidationException(message,teacher(),"teacher");
		}
		super.validateForSave();
	}

	
	public void awakeFromInsertion(EOEditingContext ec) {
		super.awakeFromInsertion(ec);
		super.setFactor(BigDecimal.ONE);
	}
	
	public String title() {
//		if(factor() == null)
//			return "?";
//		if(factor().compareTo(BigDecimal.ONE) < 0)
		if(fromLesson() != null)
			return (String)WOApplication.application().valueForKeyPath(
					"strings.RujelCurriculum_Curriculum.Substitute.Join");
		return (String)WOApplication.application().valueForKeyPath(
				"strings.RujelCurriculum_Curriculum.Substitute.Substitute");
	}

	public static int checkSubstitutes(NSArray subs) {
		int s = 0;
		if(subs != null && subs.count() > 0) {
			EOEditingContext ec = null;
			try {
				Enumeration enu = subs.objectEnumerator();
				while (enu.hasMoreElements()) {
					Substitute substitute = (Substitute) enu.nextElement();
					NSTimestamp date = (NSTimestamp)substitute.valueForKeyPath("lesson.date");
					if(date != null && !date.equals(substitute.date())) {
						if(ec == null) {
							ec = new EOEditingContext();
							ec.lock();
						}
						substitute = (Substitute)EOUtilities.localInstanceOfObject(ec, substitute);
						substitute.setDate(date);
						s++;
					}
				}
				if(ec != null) {
					ec.saveChanges();
				}
			} catch (Exception e) {
				logger.log(WOLogLevel.WARNING , "Error refining substitutes" , e);
			} finally {
				if(ec != null)
					ec.unlock();
			}
		}
		return s;
	}
	
    public static NSArray substitutesForPeriod(EOEditingContext ec, NSTimestamp begin,
    		NSTimestamp end, Integer school) {
       	NSMutableArray quals = new NSMutableArray(new EOKeyValueQualifier(
       			"reason.school", EOQualifier.QualifierOperatorEqual,school));
    	quals.addObject(new EOKeyValueQualifier(Substitute.DATE_KEY,
    			EOQualifier.QualifierOperatorGreaterThanOrEqualTo,begin));
    	quals.addObject(new EOKeyValueQualifier(Substitute.DATE_KEY,
    			EOQualifier.QualifierOperatorLessThanOrEqualTo,end));
//    	NSArray sorter = new NSArray(EOSortOrdering.sortOrderingWithKey(
//    			"date", EOSortOrdering.CompareAscending));
    	EOFetchSpecification fs = new EOFetchSpecification(
    			Substitute.ENTITY_NAME,new EOAndQualifier(quals),MyUtility.dateSorter);
    	fs.setRefreshesRefetchedObjects(true);
    	return ec.objectsWithFetchSpecification(fs);
    }
    
    public static Integer countSubstituted(NSArray lessons) {
    	int result = 0;
    	if(lessons == null || lessons.count() == 0)
    		return new Integer(result);
    	Enumeration enu = lessons.objectEnumerator();
    	while (enu.hasMoreElements()) {
			EduLesson les = (EduLesson) enu.nextElement();
			NSArray subs = (NSArray)les.valueForKey("substitutes");
			if(subs != null && subs.count() > 0)
				result++;
		}
		return new Integer(result);    	
    }
}
