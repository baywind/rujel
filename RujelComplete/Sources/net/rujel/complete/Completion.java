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
import net.rujel.interfaces.EOInitialiser;
import net.rujel.interfaces.EduCourse;
import net.rujel.interfaces.Student;
import net.rujel.reusables.SessionedEditingContext;
import net.rujel.reusables.Various;
import net.rujel.reusables.WOLogLevel;

import com.webobjects.foundation.*;
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
		buf.append(" : ").append(whoClosed());
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
}
