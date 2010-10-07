// BaseLesson.java

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

package net.rujel.base;

import java.util.Calendar;

import net.rujel.interfaces.*;
import net.rujel.reusables.SettingsReader;

import com.webobjects.foundation.*;
import com.webobjects.appserver.WOApplication;
import com.webobjects.appserver.WOComponent;
import com.webobjects.appserver.WOContext;
import com.webobjects.eocontrol.*;
import com.webobjects.eoaccess.EOUtilities;

public class BaseLesson extends _BaseLesson implements EduLesson {
/*	
	public static final NSArray flagNames = new NSArray(new Object[] {
		"notLesson","lastOnPage"});
	protected transient NamedFlags _flags;*/
	
    public BaseLesson() {
        super();
    }

	public static void init() {
		EOInitialiser.initialiseRelationship("BaseLesson","course",false,"courseID","EduCourse");
		//EOInitialiser.initialiseRelationship("BaseLesson","substitute",false,"teacherID","Teacher").anyInverseRelationship().setPropagatesPrimaryKey(true);
		EOInitialiser.initialiseRelationship("BaseNote","student",false,"studentID","Student").anyInverseRelationship().setPropagatesPrimaryKey(true);
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
	public void turnIntoFault(EOFaultHandler handler) {
		super.turnIntoFault(handler);
	}
	
	public NSArray students() {
		if(notes() == null) return null;
		return (NSArray)notes().valueForKey("student");
	}
	
	public NSArray allValues() {
		return notes();
	}
	public int count() {
		return notes().count();
	}
	
	public EOEnterpriseObject forPersonLink(PersonLink student) {
		return lessonNoteforStudent(this,student);
	}
	
	public static EOEnterpriseObject lessonNoteforStudent(EduLesson lesson,PersonLink student) {
		NSArray notes = lesson.notes();
		if(notes == null || notes.count() == 0) return null;
		EOQualifier qual = new EOKeyValueQualifier("student",EOQualifier.QualifierOperatorEqual,
				EOUtilities.localInstanceOfObject(lesson.editingContext(),(Student)student));
		NSArray result = EOQualifier.filteredArrayWithQualifier(notes,qual);
		if(result == null || result.count() == 0) return null;
		if(result.count() > 1)
			throw new RuntimeException("Dulicate entries for note found");
		return (EOEnterpriseObject)result.objectAtIndex(0);
	}

	public static String noteForStudent(EduLesson lesson, Student student) {
		if(student == null) throw new NullPointerException("Can't get note for null student");
		EOEnterpriseObject note = lessonNoteforStudent(lesson,student);
		return (note==null)?null:(String)note.storedValueForKey("note");
	}
	public EOEnterpriseObject _newNote() {
		EOEnterpriseObject note = EOUtilities.createAndInsertInstance(editingContext(),"BaseNote");
		note.addObjectToBothSidesOfRelationshipWithKey(this, "lesson");
		return note;
	}
	public static void setNoteForStudent(EduLesson lesson, String newNote, Student student) {
		EOEnterpriseObject note = lessonNoteforStudent(lesson,student);
		if(newNote != null) {			
			if(note == null) {
				note = (EOEnterpriseObject)lesson.valueForKey("newNote");
				note.addObjectToBothSidesOfRelationshipWithKey(student, "student");
			}
			note.takeValueForKey(newNote, "note");
		} else {
			if(note != null){
				lesson.removeObjectFromBothSidesOfRelationshipWithKey(note,"notes");
				lesson.editingContext().deleteObject(note);
			}
		}
	}
	
	public String noteForStudent(Student student) {
		return noteForStudent(this, student);
	}
	public void setNoteForStudent(String newNote, Student student) {
		setNoteForStudent(this, newNote, student);
	}
	/*
	public Integer number() {
		return (Integer)super.number();
	}
	public void setNumber(Integer newNumber) {
		super.setNumber(newNumber);
	}*/
	
	public void setCourse(EduCourse newCourse) {
         takeStoredValueForKey(newCourse, "course");
	}
	
	public static class TaskDelegate {
		public String homeTaskForLesson(EduLesson lesson) {
			return (String)lesson.valueForKeyPath("taskText.storedText");
		}
		
		public void setHomeTaskForLesson(String newTask, EduLesson lesson) {
			EOEnterpriseObject taskText = (EOEnterpriseObject)lesson.valueForKey(TASK_TEXT_KEY);
			if(newTask == null) {
				if(taskText != null)
					lesson.removeObjectFromBothSidesOfRelationshipWithKey(taskText, TASK_TEXT_KEY);
			} else {
				if(taskText == null) {
					taskText = EOUtilities.createAndInsertInstance(lesson.editingContext(), "TextStore");
					taskText.takeValueForKey(EntityIndex.indexForObject(lesson), "entityIndex");
					lesson.addObjectToBothSidesOfRelationshipWithKey(taskText, TASK_TEXT_KEY);
				}
				taskText.takeValueForKey(newTask, "storedText");
			}		
		}
		
		public boolean hasPopup() {
			return false;
		}
		
		public WOComponent homeWorkPopupForLesson(WOContext context, EduLesson lesson) {
			return null;
		}
	}

	protected static TaskDelegate taskDelegate = new TaskDelegate();
	public static void setTaskDelegate(TaskDelegate delegate) {
		taskDelegate = delegate;
	}
	
	public static TaskDelegate getTaskDelegate() {
		return taskDelegate;
	}
	
	//protected String _homeTask;
	public String homeTask() {
		/*if(_homeTask == null) {
			_homeTask = taskDelegate.homeTaskForLesson(this);
			if(_homeTask == null)
				_homeTask ="";
		}
		return _homeTask;*/
		return taskDelegate.homeTaskForLesson(this);
	}
	public void setHomeTask(String newTask) {
		//_homeTask = newTask;
		taskDelegate.setHomeTaskForLesson(newTask, this);
	}
	
	public NSTimestamp validateDate(Object aDate) throws NSValidation.ValidationException {
		if(course() == null)
			return null;
		Integer lag = SettingsBase.numericSettingForCourse("restrictFutureLessonDays",
				course(), editingContext());
		if(lag != null) {
			Calendar cal = Calendar.getInstance();
			if(cal.get(Calendar.HOUR_OF_DAY) < 
					SettingsReader.intForKeyPath("edu.midnightHour", 5)) {
				cal.add(Calendar.DATE, -1);
			}
			int day = cal.get(Calendar.DAY_OF_YEAR);
			int year = cal.get(Calendar.YEAR);
			cal.setTime((NSTimestamp)aDate);
			year -= cal.get(Calendar.YEAR);
			day -= cal.get(Calendar.DAY_OF_YEAR);
			if(year != 0) {
				day += year*cal.getActualMaximum(Calendar.DAY_OF_YEAR);
			}
			if(day < lag.intValue()) {
				String message = null;
				if(lag.intValue() > 0) {
					message = (String)WOApplication.application().valueForKeyPath(
							"strings.RujelBase_Base.limitedFutureLesson");
					message = String.format(message, lag);
				} else {
					message = (String)WOApplication.application().valueForKeyPath(
							"strings.RujelBase_Base.futureLessonForbidden");
				}
				throw new NSValidation.ValidationException(message);
			}
		}
		return MyUtility.validateDateInEduYear(aDate,course().eduYear(),DATE_KEY);
	}
}
