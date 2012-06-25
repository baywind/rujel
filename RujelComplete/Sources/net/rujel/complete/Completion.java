//  Completion.java

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

package net.rujel.complete;

import java.text.FieldPosition;
import java.util.Enumeration;
import java.util.logging.Logger;

import net.rujel.base.MyUtility;
import net.rujel.base.SettingsBase;
import net.rujel.interfaces.*;
import net.rujel.reusables.SessionedEditingContext;
import net.rujel.reusables.Various;
import net.rujel.reusables.WOLogLevel;

import com.webobjects.foundation.*;
import com.webobjects.appserver.WOMessage;
import com.webobjects.appserver.WOSession;
import com.webobjects.eoaccess.EOUtilities;
import com.webobjects.eocontrol.*;

public class Completion extends _Completion {
	public static String SETTINGS_BASE = "CompletionActive";

	public static void init() {
		EOInitialiser.initialiseRelationship(ENTITY_NAME,"student",false,"studentID",
				"Student").anyInverseRelationship().setPropagatesPrimaryKey(true);
		EOInitialiser.initialiseRelationship(ENTITY_NAME,"course",false,"courseID","EduCourse");
	}
	
	public void close(String who) {
		setWhoClosed(who);
		setCloseDate(new NSTimestamp());
	}
	
	public void unclose() {
		setWhoClosed(null);
		setCloseDate(null);
	}
	
	public boolean isNotClosed() {
		return (closeDate() == null);
	}

	public EduCourse course() {
		return (EduCourse)storedValueForKey("course");
	}
	
	public void setCourse(EduCourse newCourse) {
		takeStoredValueForKey(newCourse,"course");
	}
	
	public Student student() {
        return (Student)storedValueForKey("student");
    }
	
    public void setStudent(EOEnterpriseObject aValue) {
        takeStoredValueForKey(aValue, "student");
    }
    
	public static NSMutableDictionary localisation(WOSession ses) {
		NSMutableDictionary result = new NSMutableDictionary(ses.valueForKeyPath(
				"strings.RujelComplete_Complete.StudentCatalog"), "student");
		NSArray modules = (NSArray)ses.valueForKeyPath("modules.courseComplete");
		if(modules != null && modules.count() > 0) {
			Enumeration enu = modules.objectEnumerator();
			while (enu.hasMoreElements()) {
				NSKeyValueCoding mod = (NSKeyValueCoding) enu.nextElement();
				result.setObjectForKey(mod.valueForKey("title"), mod.valueForKey("id"));
			}
		}
		return result;
	}
	
	public String title(NSDictionary localisation) {
		if(localisation == null)
			return aspect();
		String result = (String)localisation.valueForKey(aspect());
		if(result == null)
			return aspect();
		return result;
	}
	
	public String title(WOSession ses) {
		if(ses == null)
			return aspect();
		return title(localisation(ses));
	}

	public String title() {
		return title(((SessionedEditingContext)editingContext()).session());
	}
	
	private static final FieldPosition pos = new FieldPosition(0);
	public String present() {
		StringBuffer buf = new StringBuffer();
		if(closeDate() != null)
			MyUtility.dateFormat().format(closeDate(), buf, pos);
		else
			return whoClosed();
		buf.append(" : ");
		if(whoClosed() != null)
			buf.append(whoClosed());
		return buf.toString();
	}
	
	public void awakeFromInsertion(EOEditingContext ec) {
		super.awakeFromInsertion(ec);
	}

	public void turnIntoFault(EOFaultHandler handler) {
		super.turnIntoFault(handler);
	}
	
	public static boolean closingActive(EduCourse course) {
		EOQualifier qual = new EOKeyValueQualifier("course",
				EOQualifier.QualifierOperatorEqual, course);
		EOFetchSpecification fs = new EOFetchSpecification(ENTITY_NAME,qual,null);
		fs.setFetchLimit(1);
		NSArray found = course.editingContext().objectsWithFetchSpecification(fs);
		return (found == null || found.count() == 0);
	}
/*
	public static boolean closingActive(EOEditingContext ec) {
		EOFetchSpecification fs = new EOFetchSpecification(ENTITY_NAME,null,null);
		fs.setFetchLimit(1);
		NSArray found = ec.objectsWithFetchSpecification(fs);
		return (found == null || found.count() == 0);
	}*/
	
	public static void activateClosing(NSArray courses, NSArray modules) {
		EduCourse course = (EduCourse)courses.objectAtIndex(0);
		EOEditingContext ec = course.editingContext();
		Enumeration cenu = courses.objectEnumerator();
		while (cenu.hasMoreElements()) {
			course = (EduCourse) cenu.nextElement();
			activateForCourse(course, modules);
			try {
				if(ec.hasChanges())
					ec.saveChanges();
			} catch (Exception e) {
				Logger.getLogger("rujel.complete").log(WOLogLevel.WARNING,
					"Error generating Completion records for course", new Object[] {course,e});
				ec.revert();
			}
		}
	}
	
	public static void activateForCourse(EduCourse course, NSArray modules) {
		EOEditingContext ec = course.editingContext();
		EOQualifier qual = new EOKeyValueQualifier("course",
				EOQualifier.QualifierOperatorEqual, course);
		EOFetchSpecification fs = new EOFetchSpecification(ENTITY_NAME,qual,null);
		NSArray found = ec.objectsWithFetchSpecification(fs);
		if(found != null && found.count() > 0) {
			found = (NSArray)found.valueForKey(ASPECT_KEY);
		} else {
			found = null;
		}
		Completion cmpl = null;
		if (found == null || !found.containsObject("student")) {
			cmpl = (Completion) EOUtilities.createAndInsertInstance(ec, ENTITY_NAME);
			cmpl.setCourse(course);
			cmpl.setAspect("student");
		}
/*		if (found == null || !found.containsObject("lessons")) {
			cmpl = (Completion) EOUtilities.createAndInsertInstance(ec, ENTITY_NAME);
			cmpl.setCourse(course);
			cmpl.setAspect("lessons");
		}*/
		Enumeration menu = modules.objectEnumerator();
		while (menu.hasMoreElements()) {
			NSKeyValueCoding module = (NSKeyValueCoding) menu.nextElement();
			if(!Various.boolForObject(module.valueForKey("manual")))
				continue;
			String id = (String)module.valueForKey("id");
			if(found != null && found.containsObject(id))
				continue;
			cmpl = (Completion)EOUtilities.createAndInsertInstance(ec, ENTITY_NAME);
			cmpl.setCourse(course);
			cmpl.setAspect(id);
		}
	}
	
	public static int[] countReady(NSArray completions) {
		int[] result = new int[] {0,0,0};
		if(completions == null || completions.count() == 0)
			return null;
		result[2] = completions.count();
		Enumeration enu = completions.objectEnumerator();
		while (enu.hasMoreElements()) {
			Completion cpt = (Completion) enu.nextElement();
			if(cpt.closeDate() == null)
				result[0]++;
			else
				result[1]++;
		}
		return result;
	}
	
	public static NSArray findCompletions(Object course, Object student,
			String aspect, Boolean closed, EOEditingContext ec) {
		NSMutableArray quals = new NSMutableArray();
		if(course instanceof NSArray)  {
			if(((NSArray)course).count() == 0)
				return null;
			quals.addObject(Various.getEOInQualifier("course", (NSArray)course));
		} else if(course != null)
			quals.addObject(new EOKeyValueQualifier("course",
					EOQualifier.QualifierOperatorEqual,course));
		if(student instanceof NSArray)  {
			if(((NSArray)student).count() == 0)
				return null;
			quals.addObject(Various.getEOInQualifier("student", (NSArray)student));
		} else if(student != null)
			quals.addObject(new EOKeyValueQualifier("student",
					EOQualifier.QualifierOperatorEqual,student));
		if(aspect != null)
			quals.addObject(new EOKeyValueQualifier(ASPECT_KEY,
					EOQualifier.QualifierOperatorEqual,aspect));
		if(closed != null)
			quals.addObject(new EOKeyValueQualifier(CLOSE_DATE_KEY,(closed.booleanValue())?
	EOQualifier.QualifierOperatorNotEqual:EOQualifier.QualifierOperatorEqual,NullValue));
		EOFetchSpecification fs = new EOFetchSpecification(ENTITY_NAME,
				new EOAndQualifier(quals), null);
		NSArray result = ec.objectsWithFetchSpecification(fs);
		if(result == null)
			result = NSArray.EmptyArray;
		return result; // course open Completion
	}
	
	public static NSMutableDictionary courseCompletion(EduCourse course,
			NSArray modules, SettingsBase settings) {
		if(!Various.boolForObject(settings.forCourse(course).textValue())) {
			return null;
		}
		NSArray list = EOUtilities.objectsMatchingKeyAndValue(course.editingContext(),
				ENTITY_NAME, "course", course);
		if(list == null || list.count() == 0)
			return null;
		Enumeration enu = list.objectEnumerator();
		int closed = 0;
		int students = 0;
		int totalStudents = 0;
		NSMutableDictionary result = new NSMutableDictionary();
		while (enu.hasMoreElements()) {
			Completion cpt = (Completion) enu.nextElement();
			String aspect = cpt.aspect();
			if(aspect.equals("student")) {
				totalStudents++;
				if(cpt.closeDate() != null)
					students++;
			} else if(modules.containsObject(aspect)) {
				if(cpt.closeDate() != null) {
					closed++;
					result.takeValueForKey(cpt.closeDate(), aspect);
					result.takeValueForKey("gerade", aspect + "Class");
				} else {
					result.takeValueForKey("ungerade", aspect + "Class");
				}
			}
		}
		if(closed == 0) {
			result.takeValueForKey("X", "integral");
			result.takeValueForKey("grey", "integralClass");
		} else if(closed < modules.count()) {
			result.takeValueForKey("~", "integral");
			result.takeValueForKey("ungerade", "integralClass");
		} else if(students < totalStudents) {
			result.takeValueForKey("S", "integral");
			result.takeValueForKey("gerade", "integralClass");
		} else {
			result.takeValueForKey("V", "integral");
			result.takeValueForKey("green", "integralClass");
		}
		if(totalStudents == 0) {
			result.takeValueForKey("???", "presentStudent");
		} else {
			if(students < totalStudents) {
				result.takeValueForKey("highlight", "studentClass");
			} else {
				result.takeValueForKey("green", "studentClass");
			}
			if(totalStudents > 1) {
				StringBuffer buf = new StringBuffer(7);
				buf.append(students).append(" / ").append(totalStudents);
				result.takeValueForKey(buf.toString(), "presentStudent");
			} else {
				result.takeValueForKey((students < totalStudents)? "0" : "V",
						"presentStudent");
			}
			if(students == 0)
				result.takeValueForKey("grey", "studentClass");
		}
		return result;
	}
	
	public static boolean studentIsReady(Student student, EduGroup gr, Integer year) {
		EOEditingContext ec = student.editingContext();
		NSArray found = EOUtilities.objectsWithQualifierFormat(ec, ENTITY_NAME,
				"student = %@ AND closeDate = nil", new NSArray(student));
		if(found != null && found.count() > 0) { // individual open Completion
			Enumeration enu = found.objectEnumerator();
			while (enu.hasMoreElements()) {
				Completion cpt = (Completion) enu.nextElement();
				if(year.equals(cpt.course().eduYear()))
					return false;
			}
		}
		NSMutableArray courses = new NSMutableArray();
		found = EOUtilities.objectsMatchingKeyAndValue(ec, "CourseAudience", "student", student);
		if(found != null && found.count() > 0) { //subgroup courses
			Enumeration enu = found.objectEnumerator();
			while (enu.hasMoreElements()) {
				EOEnterpriseObject aud = (EOEnterpriseObject)enu.nextElement();
				EduCourse crs = (EduCourse) aud.valueForKey("course");
				if(crs.eduYear().equals(year))
					courses.addObject(crs);
			}
		}
		if(gr == null)
			gr = student.recentMainEduGroup();
		if(gr != null) { //wide courses
			found = EOUtilities.objectsWithQualifierFormat(ec, EduCourse.entityName,
					"eduGroup = %@ AND eduYear = %d", new NSArray(new Object[] {gr,year}));
			if(found != null && found.count() > 0) {
				Enumeration enu = found.objectEnumerator();
				while (enu.hasMoreElements()) {
					EduCourse crs = (EduCourse) enu.nextElement();
					if(!courses.contains(crs) && crs.groupList().contains(student))
						courses.addObject(crs);
				}
			}
		}
		found = findCompletions(courses, NullValue, "student", Boolean.FALSE, ec);
		return (found == null || found.count() == 0);
	}
	
	public static NSMutableDictionary completionsForGroup(EduGroup gr,Integer year) {
		if(gr == null)
			return null;
		EOEditingContext ec = gr.editingContext();
		NSArray students = gr.list();
		
		EOQualifier[] quals = new EOQualifier[3];
		quals[0] = new EOKeyValueQualifier("eduYear", EOQualifier.QualifierOperatorEqual, year);
		quals[1] = new EOKeyValueQualifier("eduGroup", EOQualifier.QualifierOperatorEqual, gr);
		quals[1] = new EOAndQualifier(new NSArray(quals));
		EOFetchSpecification fs = new EOFetchSpecification(EduCourse.entityName,quals[1],null);
		NSArray courses = ec.objectsWithFetchSpecification(fs);
		
		quals[0] = new EOKeyValueQualifier("course.eduYear",
				EOQualifier.QualifierOperatorEqual, year);
		quals[1] = new EOKeyValueQualifier("course.eduGroup"
				, EOQualifier.QualifierOperatorNotEqual, gr);
		quals[2] = Various.getEOInQualifier("student", students);
		quals[1] = new EOAndQualifier(new NSArray(quals));
		fs = new EOFetchSpecification("CourseAudience",quals[1],null);
		NSArray found = ec.objectsWithFetchSpecification(fs);
		if(found != null && found.count() > 0) {
			if(courses == null)
				courses = found;
			else
				courses = courses.arrayByAddingObjectsFromArray(
						(NSArray)found.valueForKey("course"));
		}
		found = findCompletions(courses, NullValue, "student", Boolean.TRUE, ec);
		NSMutableDictionary result = new NSMutableDictionary();
		Enumeration enu = students.objectEnumerator();
		while (enu.hasMoreElements()) {
			Student student = (Student) enu.nextElement();
			NSMutableDictionary dict = new NSMutableDictionary(4);
			NSMutableArray closed = new NSMutableArray();
			dict.takeValueForKey(closed, "closed");
			result.setObjectForKey(dict, student);
			if(found != null && found.count() > 0) {
				Enumeration cEnu = found.objectEnumerator();
				while (cEnu.hasMoreElements()) {
					Completion cpt = (Completion) cEnu.nextElement();
					EduCourse crs = cpt.course();
					if(crs.groupList().containsObject(student))
						closed.addObject(crs);
				}
			}
			dict.takeValueForKey(new NSMutableArray(), "left");
		}
		found = findCompletions(null, students, "student", null, ec);
		if(found != null && found.count() > 0) {
			enu = found.objectEnumerator();
			while (enu.hasMoreElements()) {
				Completion cpt = (Completion) enu.nextElement();
				NSMutableDictionary dict = (NSMutableDictionary)result.objectForKey(cpt.student());
				NSMutableArray list = (NSMutableArray)dict.valueForKey(
						(cpt.closeDate() == null)?"left":"closed");
				list.addObject(cpt.course());
			}
		}
		enu = result.keyEnumerator();
		while (enu.hasMoreElements()) {
			Student student = (Student)enu.nextElement();
			NSMutableDictionary dict = (NSMutableDictionary)result.objectForKey(student);
			NSMutableArray closed = (NSMutableArray)dict.valueForKey("closed");
			dict.takeValueForKey(new Integer(closed.count()), "closedCount");
			dict.takeValueForKey(coursesList(closed, gr), "closed");
			NSMutableArray left = (NSMutableArray)dict.valueForKey("left");
			Enumeration cEnu = courses.objectEnumerator();
			while (cEnu.hasMoreElements()) {
				EduCourse crs = (EduCourse)cEnu.nextElement();
				if(!closed.containsObject(crs) && !left.containsObject(crs) &&
						crs.groupList().containsObject(student))
					left.addObject(crs);
			}
			dict.takeValueForKey(new Integer(left.count()), "leftCount");
			dict.takeValueForKey(coursesList(left, gr), "left");
		}		
		return result;
	}
	
	private static String coursesList(NSMutableArray list, EduGroup gr) {
		if(list.count() == 0)
			return null;
		EOSortOrdering.sortArrayUsingKeyOrderArray(list, EduCourse.sorter);
		StringBuilder buf = new StringBuilder();
		Enumeration enu = list.objectEnumerator();
		while (enu.hasMoreElements()) {
			EduCourse crs = (EduCourse) enu.nextElement();
			if(gr != crs.eduGroup())
				buf.append('[').append(crs.eduGroup().name()).append(']').append(' ');
			buf.append(crs.cycle().subject());
			if(crs.comment() != null && crs.comment().length() < 10) {
				buf.append(' ').append('(');
				buf.append(WOMessage.stringByEscapingHTMLAttributeValue(crs.comment()));
				buf.append(')');
			}
			if(enu.hasMoreElements())
				buf.append(',').append('\n');
		}
		return buf.toString();
	}
}
