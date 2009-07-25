// TeacherSelector.java: Class file for WO Component 'TeacherSelector'

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

package net.rujel.ui;

import java.util.Enumeration;
import java.util.logging.Logger;

import net.rujel.interfaces.*;
import net.rujel.reusables.PlistReader;
import net.rujel.reusables.Various;
import net.rujel.reusables.WOLogLevel;

import com.webobjects.appserver.*;
import com.webobjects.eoaccess.EOUtilities;
import com.webobjects.eocontrol.EOEditingContext;
import com.webobjects.eocontrol.EOEnterpriseObject;
import com.webobjects.eocontrol.EOSortOrdering;
import com.webobjects.foundation.*;

// Generated by the WOLips Templateengine Plug-in at Apr 8, 2009 3:51:12 PM
public class TeacherSelector extends com.webobjects.appserver.WOComponent {
	protected static final Logger logger = Logger.getLogger("rujel.base");
	public EOEditingContext editingContext;
    public NSArray subjects;
    public Object selection;
    protected NSMutableDictionary dict;
    public String searchString;
    public String searchMessage;
    protected NSArray found;
    
    public Object item;
    public String currSubject;

	public TeacherSelector(WOContext context) {
        super(context);
      }
	
	public void appendToResponse(WOResponse aResponse, WOContext aContext) {
		selection = valueForBinding("selection");
		item = null;
		super.appendToResponse(aResponse, aContext);
		searchMessage = null;
	}

	public void awake() {
		super.awake();
		selection = valueForBinding("selection");
		if(editingContext == null) {
			editingContext = (EOEditingContext)valueForBinding("editingContext");
			populate();
		}
	}
	
	public boolean synchronizesVariablesWithBindings() {
        return false;
	}

	public void populate() {
		dict = new NSMutableDictionary();
		Integer eduYear = (Integer)session().valueForKey("eduYear");
		NSArray allCourses = EOUtilities.objectsMatchingKeyAndValue
					(editingContext, EduCourse.entityName, "eduYear", eduYear);
		if(allCourses == null || allCourses.count() == 0)
			return;
		NSMutableSet all = new NSMutableSet();
		NSMutableSet subjectSet = new NSMutableSet();
		// collecting teachers on subjects
		Enumeration enu = allCourses.objectEnumerator();
		boolean smartEduPlan = (EduCycle.className.equals("net.rujel.eduplan.PlanCycle"));
		while (enu.hasMoreElements()) {
			EduCourse course = (EduCourse) enu.nextElement();
			Object subject = course.cycle().subject();
			Teacher teacher = course.teacher();
			if(teacher != null) {
				all.addObject(teacher);
				NSMutableSet bySubj = (NSMutableSet)dict.objectForKey(subject);
				if(bySubj == null) {
					bySubj = new NSMutableSet(teacher);
					dict.setObjectForKey(bySubj, subject);
				} else {
					bySubj.addObject(teacher);
				}
			}
			if(smartEduPlan) {
				subject = course.valueForKeyPath("cycle.subjectEO");
			}
			subjectSet.addObject(subject);
		} // allCourses.objectEnumerator
		dict.setObjectForKey(all, "All");
		// sorting teacher lists
		enu = dict.keyEnumerator();
		while (enu.hasMoreElements()) {
			Object subject = enu.nextElement();
			NSMutableSet bySubj = (NSMutableSet)dict.objectForKey(subject);
			NSArray sorted = EOSortOrdering.sortedArrayUsingKeyOrderArray(
					bySubj.allObjects(), Person.sorter);
			dict.setObjectForKey(sorted, subject);
		}
		NSComparator comparator = NSComparator.AscendingStringComparator;
		subjects = subjectSet.allObjects();
		try {
			if (smartEduPlan) {
				Class compClass = Class.forName("net.rujel.eduplan.SubjectComparator");
				comparator = (NSComparator)compClass.getConstructor((Class[])null)
															.newInstance((Object[])null);
			}
			subjects = subjects.sortedArrayUsingComparator(comparator);
		} catch (Exception e) {
			Object[] args = new Object[] {session(),e};
			logger.log(WOLogLevel.WARNING,"Error sorting subjects list",args);
		}
		if(smartEduPlan) {
			subjects = (NSArray)subjects.valueForKey("subject");
		}
	}
	
	public WOActionResults search() {
		currSubject = null;
		if(searchString == null)
			return null;
		found = Person.Utility.search(session().defaultEditingContext(),
				Teacher.entityName, searchString);
		if(found == null || found.count() == 0) {
			searchMessage = (String)application().valueForKeyPath(
					"strings.Strings.messages.nothingFound");
			return null;
		}
		NSMutableArray fullList = (NSMutableArray)session().valueForKey("personList");
		NSMutableArray tmp = found.mutableClone();
		tmp.removeObjectsInArray(fullList);
		fullList.addObjectsFromArray(tmp);
		return null;
	}
	
	public String subjectClass() {
		if(item == null) {
			if("All".equals(currSubject))
				return "selection";
			return "gerade";
		}
		if(item.equals(currSubject))
			return "selection";
		return "ungerade";
	}
	
	public String teacherClass() {
		if(item == null) {
			if(selection == NullValue)
				return "selection";
			return "orange";
		}
		if(item == selection)
			return "selection";
		Boolean sex = (Boolean)NSKeyValueCoding.Utility.valueForKey(item, "sex");
		if(sex == null)
			return "grey";
		
		if(found != null && found.containsObject(item)) {
			if (sex.booleanValue())
				return "foundMale";
			else
				return "foundFemale";
		} else {
			if (sex.booleanValue())
				return "male";
			else
				return "female";
		}
	}
	
	public WOActionResults select() {
		if(item == null) {
			currSubject = "All";
			return context().page();
		}
		if(item instanceof String) {
			currSubject = (String)item;
			return context().page();
		}
		if(item instanceof Teacher) {
			selection = (Teacher)item;
			setValueForBinding(selection, "selection");
			return (WOActionResults)valueForBinding("selectAction");
		}
		return null;
	}
	
	public WOActionResults selectVacant() {
		setValueForBinding(NullValue, "selection");
		return (WOActionResults)valueForBinding("selectAction");
	}
	
	public WOActionResults selectDelete() {
		setValueForBinding(null, "selection");
		return (WOActionResults)valueForBinding("selectAction");
	}

	public NSArray list() {
		if(currSubject != null)
			return (NSArray)dict.valueForKey(currSubject);
		NSMutableArray result = new NSMutableArray();
		NSArray personList = (NSArray)session().valueForKey("personList");
		if(personList != null && personList.count() > 0) {
			Enumeration enu = personList.objectEnumerator();
			while (enu.hasMoreElements()) {
				EOEnterpriseObject pers = (EOEnterpriseObject) enu.nextElement();
				if(pers instanceof Teacher)
					result.addObject(EOUtilities.localInstanceOfObject(editingContext, pers));
			}
		}
		if(selection instanceof Teacher && !result.contains(selection))
			result.insertObjectAtIndex(selection, 0);
		return result;
	}
	
	public String act() {
		if(Various.boolForObject(valueForBinding("useAjaxPost"))) {
			String href = context().componentActionURL();
			String result = "ajaxPopupAction('" + href + "');";
			return result;
		}
		return (String)session().valueForKey("tryLoad");
	}

	public String onSubmit() {
		if(Various.boolForObject(valueForBinding("useAjaxPost")))
			return "return ajaxPost(this);";
		return "return tryLoad(true);";
	}

	public static WOActionResults selectorPopup(WOComponent returnPage, 
			String resultPath, EOEditingContext ec) {
		WOComponent selector = returnPage.pageWithName("SelectorPopup");
		selector.takeValueForKey(returnPage, "returnPage");
		selector.takeValueForKey(resultPath, "resultPath");
		Object teacher = returnPage.valueForKeyPath(resultPath);
		selector.takeValueForKey(teacher, "value");
		NSDictionary dict = (NSDictionary)WOApplication.application().valueForKeyPath(
				"strings.RujelBase_Base.selectTeacher");
		dict = PlistReader.cloneDictionary(dict, true);
		if(teacher != null) {
			dict.takeValueForKeyPath(new NSArray(teacher), "presenterBindings.forcedList");
		} else {
			dict.takeValueForKeyPath(null, "presenterBindings.forcedList");
		}
		dict.takeValueForKeyPath(ec, "presenterBindings.editingContext");
		selector.takeValueForKey(dict, "dict");
		return selector;
	}
}
