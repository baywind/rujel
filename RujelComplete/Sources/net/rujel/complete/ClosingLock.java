// ClosingLock.java

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

package net.rujel.complete;

import java.util.Enumeration;

import com.webobjects.appserver.WOApplication;
import com.webobjects.appserver.WOComponent;
import com.webobjects.appserver.WOContext;
import com.webobjects.appserver.WOSession;
import com.webobjects.eoaccess.EOUtilities;
import com.webobjects.eocontrol.EOEnterpriseObject;
import com.webobjects.foundation.*;

import net.rujel.auth.ReadAccess;
import net.rujel.interfaces.EduCourse;
import net.rujel.interfaces.Student;
import net.rujel.reusables.Various;
import net.rujel.reusables.WOLogLevel;

public class ClosingLock implements ReadAccess.Modifier {
	public static final Integer SORT = new Integer(50);
	public static final String[] lockKeys = new String[] {"entity","coursePath","studentPath"};

	protected EduCourse course;
	protected NSMutableDictionary locks;
	protected NSSet aspects;
	protected NSArray diffStudents;
	protected Boolean allStudents;
	protected NSArray modules;
	
	public ClosingLock (WOSession ses) {
		locks = new NSMutableDictionary();
		modules = (NSArray)ses.valueForKeyPath("modules.completionLock");
		if(modules != null && modules.count() > 0) {
			Enumeration enu = modules.objectEnumerator();
			while (enu.hasMoreElements()) {
				NSKeyValueCoding mod = (NSKeyValueCoding) enu.nextElement();
				Object key = mod.valueForKey("entity");
				if(key != null)
					locks.setObjectForKey(mod, key);
			}
		}
		modules = (NSArray)ses.valueForKeyPath("modules.courseComplete");
		if(modules != null && modules.count() > 0) {
			Enumeration enu = modules.objectEnumerator();
			String[] keys = new String[] {"aspect","coursePath"};
			while (enu.hasMoreElements()) {
				NSKeyValueCoding mod = (NSKeyValueCoding) enu.nextElement();
				Object key = mod.valueForKey("lockEntity");
				if(key == null) continue;
				String value = (String)mod.valueForKey("id");
				String[] values = new String[] {value,"course"};
				value = (String)mod.valueForKey("coursePath");
				if(value != null)
					values[1] = value;
				NSDictionary dict = new NSDictionary(values,keys);
				if(key instanceof NSArray) {
					Enumeration entEnu = ((NSArray)key).objectEnumerator();
					while (entEnu.hasMoreElements()) {
						key = entEnu.nextElement();
						locks.setObjectForKey(dict, key);
					}
				} else {
					locks.setObjectForKey(dict, key);
				}
			}
		}
	}
	
	public String interpret(Object obj, String subPath, WOContext ctx) {
		String entity = null;
		if(obj instanceof EOEnterpriseObject)
			entity = ((EOEnterpriseObject)obj).entityName();
		else if(obj instanceof String)
			entity = (String)obj;
		else
			return null;
		NSDictionary lock = (NSDictionary)locks.valueForKey(entity);
		if(lock == null) {
			return null;
		} else if (obj instanceof String) {
			try {
				EduCourse crs = (EduCourse)ctx.page().valueForKey("course");
				setCourse(crs);
			} catch (Exception e) {
				obj = ctx.session().objectForKey("readAccess");
				if(obj == null) {
					CompletePopup.logger.log(WOLogLevel.FINER,"String request to ClosingLock: "
							+ entity, new Object[] {ctx.session(),e});
					return null;
				}
			}
		} 
		if(obj instanceof NSKeyValueCodingAdditions) {
			String key = null;
			key = (String)lock.valueForKey("coursePath");
			if(key == null) {
				key = (String)lock.valueForKey("checkCourse");
				Object checkCourse = (course==null)?null:course.valueForKeyPath(key);
				key = (String)lock.valueForKey("checkPath");
				Object checkPath = ((NSKeyValueCodingAdditions)obj).valueForKeyPath(key);
				if((checkCourse==null)?checkPath!=null:!checkCourse.equals(checkPath)) {
					key = (String)lock.valueForKey("assumeCourse");
					if(key == null) key = "assumeCourse";
					checkCourse = ((NSKeyValueCodingAdditions)obj).valueForKeyPath(key);
					setCourse((EduCourse)checkCourse);
				}
			} else {
				setCourse((EduCourse)((NSKeyValueCodingAdditions)obj).valueForKeyPath(key));
			}
		}
		if(course == null || (aspects == null && allStudents == null && diffStudents == null))
			return null;
		String key = (String)lock.valueForKey("aspect");
		if(key != null) {
			if(aspects != null && aspects.containsObject(key))
				return entity + "@completionClosed";
			return null;
		}
		Object student = null;
		if(obj instanceof String) {
			WOComponent component = ctx.component();
			try {
				student = component.valueForKey("student");
			} catch (NSKeyValueCoding.UnknownKeyException e) {
				student = component.valueForBinding("student");
			}
			if(student == null)
				return null;
		}
		key = (String)lock.valueForKey("studentPath");
		if(key == null) {
			CompletePopup.logger.log(WOLogLevel.WARNING,
					"Undefined aspect/studentPath for completion Lock " + entity, lock);
			return null;
		}
		boolean closed = (allStudents != null && allStudents.booleanValue());
		if(diffStudents != null) {
			if(student == null)
				student = ((NSKeyValueCodingAdditions)obj).valueForKeyPath(key);
			if(diffStudents.containsObject(student))
				closed = !closed;
		}
		if(closed)
			return entity + "@completionClosed";
		return null;
	}
	
	public void setCourse(EduCourse crs) {
		if(crs == course)
			return;
		course = crs;
		aspects = null;
		allStudents = null;
		diffStudents = null;
		if(crs == null)
			return;
		NSArray completions = EOUtilities.objectsMatchingKeyAndValue(crs.editingContext(),
				Completion.ENTITY_NAME, "course", crs);
		if(completions == null || completions.count() == 0)
			return;
		NSMutableArray closedStudents = new NSMutableArray();
		NSMutableArray openStudents = new NSMutableArray();
		aspects = new NSMutableSet();
		Enumeration enu = completions.objectEnumerator();
		NSMutableDictionary readyModules = new NSMutableDictionary(modules.count() + 1);
		while (enu.hasMoreElements()) {
			Completion cpt = (Completion) enu.nextElement();
			Student student = cpt.student();
			if(student != null) {
				if(cpt.closeDate() == null)
					openStudents.addObject(student);
				else
					closedStudents.addObject(student);
			} else {
				String aspect = cpt.aspect();
				if(aspect.equals("student")) {
					allStudents = Boolean.valueOf(cpt.closeDate() != null);
				} else if (cpt.closeDate() != null) {
					((NSMutableSet)aspects).addObject(aspect);
					readyModules.takeValueForKey(Boolean.TRUE, aspect);
				} else {
					readyModules.takeValueForKey(Boolean.FALSE, aspect);
				}
			}
		}
		if(allStudents == null) {
			if(openStudents.count() >= closedStudents.count()) {
				if(closedStudents.count() > 0)
					diffStudents = closedStudents.immutableClone();
			} else {
				allStudents = Boolean.TRUE;
				if(openStudents.count() > 0)
					diffStudents = openStudents.immutableClone();
			}
		} else if (openStudents.count() + closedStudents.count() > 0){
			CompletePopup.logger.log(WOLogLevel.WARNING,
					"Found completions both for all and individual students", course);
			if(allStudents.booleanValue())
				diffStudents = openStudents;
			else
				diffStudents = closedStudents;
			if(diffStudents.count() == 0)
				diffStudents = null;
			else
				diffStudents = diffStudents.immutableClone();
		}
		if(diffStudents == null && allStudents != null)
			readyModules.takeValueForKey(allStudents, "student");
		else
			readyModules.takeValueForKey(Boolean.FALSE, "student");
		if(CoursePage.accountDependencies(readyModules, modules) > 0) {
			enu = readyModules.keyEnumerator();
			while (enu.hasMoreElements()) {
				String aspect = (String) enu.nextElement();
				if(Various.boolForObject(readyModules.objectForKey(aspect)))
					((NSMutableSet)aspects).addObject(aspect);
			}
		}
		if(aspects.count() == 0)
			aspects = null;
		else
			aspects = aspects.immutableClone();

	}

	public Number sort() {
		return SORT;
	}
	
	public String message() {
		return (String)WOApplication.application().valueForKeyPath(
				"strings.RujelComplete_Complete.messages.locked");
	}
}
