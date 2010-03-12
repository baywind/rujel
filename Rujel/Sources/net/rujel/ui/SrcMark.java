// SrcMark.java: Class file for WO Component 'SrcMark'

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

import net.rujel.interfaces.*;
import net.rujel.reusables.*;

import com.webobjects.foundation.*;
import com.webobjects.foundation.NSComparator.ComparisonException;
import com.webobjects.appserver.*;
import com.webobjects.eocontrol.*;
import com.webobjects.eoaccess.EOUtilities;
import java.util.Enumeration;
import java.util.logging.Logger;
import com.webobjects.appserver.WOActionResults;

public class SrcMark extends WOComponent {
	protected static Logger logger = Logger.getLogger("rujel.journal");
	protected NamedFlags access;

	public EOEditingContext ec;
	
	public EduGroup currClass;
	public Object currTeacher;
	public NSArray courses;
//	public EduCourse aCourse;
	
	public EOEnterpriseObject item;
    public int cursIndex;
	protected int currIndex = -1;
	public NSMutableArray popupCycles;
	public NSMutableDictionary dict = new NSMutableDictionary();
	
	
    public SrcMark(WOContext context) {
        super(context);
		ec = new SessionedEditingContext(session());
		ec.lock();
		access = (NamedFlags)session().valueForKeyPath("readAccess.FLAGS.SrcMark");
		EOGlobalID pLink = (EOGlobalID)session().valueForKey("userPersonGID");
		if(pLink instanceof Teacher) {
			currTeacher = (Teacher)ec.objectForGlobalID(pLink);
			//teacherName = Person.Utility.fullName(currTeacher,true,2,1,1);
			if(currTeacher != null)
				coursesForTeacher(currTeacher);
		}
		ec.unlock();
    }
	
	public void setCurrClass(EduGroup newClass) {
		if(newClass != null && newClass.editingContext() != ec) {
			currClass = (EduGroup)EOUtilities.localInstanceOfObject(ec, newClass);
		} else {
			currClass = newClass;
		}
	}
	
	public String teacherOnClick() {
		String key = (currTeacher == null || currClass == null)?"ajaxPopup":"checkRun";
		return (String)session().valueForKey(key);
	}

    public WOActionResults selectTeacher() {
    	if(currTeacher == null || currClass == null)
    		return chooseCurrentTeacher();
    	coursesForTeacher(currTeacher);
		return null;
    }
    public void coursesForTeacher(Object teacher) {
    	currIndex = -1;
    	currClass = null;
		popupCycles = null;
		NSArray args = new NSArray(new Object[] {session().valueForKey("eduYear"),teacher});
		NSArray result =  EOUtilities.objectsWithQualifierFormat(ec,EduCourse.entityName,
				"eduYear = %d AND teacher = %@",args);
		EOQualifier qual = new EOKeyValueQualifier("cycle.school",
				EOQualifier.QualifierOperatorEqual, session().valueForKey("school"));
		courses = EOQualifier.filteredArrayWithQualifier(result, qual);
	}
	
    public WOComponent selectClass() {
		dict.removeAllObjects();
		currIndex = -1;
		undoCreation();
		if(currClass == null)
			return null;
		NSMutableArray cycles = EduCycle.Lister.cyclesForEduGroup(currClass).mutableClone();
		dict.takeValueForKey(currClass, "eduGroup");
		dict.takeValueForKey(session().valueForKey("eduYear"), "eduYear");
		NSArray existingCourses = EOUtilities.objectsMatchingValues(ec, EduCourse.entityName, dict);
		EOQualifier qual = new EOKeyValueQualifier("cycle.school",
				EOQualifier.QualifierOperatorEqual, session().valueForKey("school"));
		existingCourses = EOQualifier.filteredArrayWithQualifier(existingCourses, qual);
		//filter cycles
		
		NSMutableArray result = new NSMutableArray();
		Enumeration enumerator = cycles.objectEnumerator();
		NSMutableSet coursesSet = new NSMutableSet(existingCourses);
		popupCycles = new NSMutableArray();
		while (enumerator.hasMoreElements()) {
			EduCycle currCycle = (EduCycle)enumerator.nextElement();
			qual = new EOKeyValueQualifier("cycle",EOQualifier.QualifierOperatorEqual,currCycle);
			NSArray matches = EOQualifier.filteredArrayWithQualifier(existingCourses,qual);
			int count = 0;
			if(matches != null && matches.count() > 0) {
				count = matches.count();
				result.addObjectsFromArray(matches);
				coursesSet.subtractSet(new NSSet(matches));
			}
			Number subs = currCycle.subgroups();
			if(subs == null)
				subs = new Integer(1);
			count = subs.intValue() - count;
			if(count > 0) {
				for (int i = 0; i < count; i++) {
					result.addObject(currCycle);
				}
			} else {
				popupCycles.addObject(currCycle);
			}
		}
		if(coursesSet.count() > 0) {
			result.addObjectsFromArray(coursesSet.allObjects());
		}
		courses = result.immutableClone();
        return null;
	}
	
    public boolean isEduPlan() {
        return (item instanceof EduCycle);
    }

    public void setCurrTeacher(Object teacher) {
		undoCreation();
    	if(teacher instanceof Teacher) {
    		currTeacher = (Teacher)EOUtilities.localInstanceOfObject(ec, (Teacher)teacher);
    	} else {
    		currTeacher = teacher;
    	}
    	if(teacher != null)
    		coursesForTeacher(currTeacher);
    }
    
    public String teacherRowClass() {
    	if(currClass == null && currTeacher != null)
    		return "selection";
    	return "grey";
    }

    public WOComponent addCourse() {
		currIndex = courses.count();
        return null;
    }
    
    public Boolean disableSelect() {
    	if(item instanceof EduCourse)
    		return (Boolean)access.valueForKey("_read");
    	if(item instanceof EduCycle)
    		return (Boolean)access.valueForKey("_create");
    	return Boolean.TRUE;
    }

    public WOComponent select() {
		if(item instanceof EduCourse)
			return openCourse();
		if(item instanceof EduCycle)
			return selectRow();
		return null;
	}

    public WOComponent selectRow() {
    	undoCreation();
		currIndex = cursIndex;
		if(item instanceof EduCycle)
			dict.takeValueForKey(item, "cycle");
        return null;
    }
	
    public WOComponent openCourse() {
		undoCreation();
		currIndex = -1;
		if(Various.boolForObject(session().valueForKeyPath("readAccess._read.item"))) {
			session().takeValueForKey(valueForKeyPath("application.strings.Strings.messages.noAccess"),"message");
			logger.log(WOLogLevel.READING,"Denied access to course",new Object[] {session(),item});
			return null;
		}
		WOComponent nextPage = pageWithName("LessonNoteEditor");
		nextPage.takeValueForKey(item,"course");
		session().takeValueForKey(this,"pushComponent");
		return nextPage;
    }
	
	public void save() {
		EduCourse aCourse = null;
		if(currIndex >= 0 && currIndex < courses.count() && 
				courses.objectAtIndex(currIndex) instanceof EduCourse)
			aCourse = (EduCourse)courses.objectAtIndex(currIndex);
		boolean newCourse = (aCourse == null);
		if(newCourse) {
			if(!access.flagForKey("create")) {
				session().takeValueForKey(valueForKeyPath("application.strings.Strings.messages.noAccess"),"message");
				logger.logp(WOLogLevel.OWNED_EDITING,"SrcMark","save","Denied to create new course",session());
				return;
			}
			aCourse = (EduCourse)EOUtilities.createAndInsertInstance(ec, EduCourse.entityName);
			aCourse.takeValuesFromDictionary(dict);
		} else {
			if(Various.boolForObject(session().valueForKeyPath("readAccess._edit.aCourse"))) {
				session().takeValueForKey(valueForKeyPath("application.strings.Strings.messages.noAccess"),"message");
				logger.logp(WOLogLevel.OWNED_EDITING,"SrcMark","save","Denied course editing",session());
				return;
			}
		}
		if(ec.hasChanges()) {
			try {
				ec.saveChanges();
				if(newCourse) { //log creation
					logger.logp(WOLogLevel.UNOWNED_EDITING,"SrcMark","save","Created new course",new Object[] {session(),aCourse});
					NSNotificationCenter.defaultCenter().postNotification("Own created object",
							session().valueForKey("user"),new NSDictionary(aCourse,"EO"));
					NSMutableArray tmp = courses.mutableClone();
					dict.removeObjectForKey("teacher");
					if (createNew()) {
						boolean found = false;
						EduCycle cycle = aCourse.cycle();
						for(int i = 0; i < courses.count();i++) {
							Object cur = courses.objectAtIndex(i);
							if(cur instanceof EduCourse) {
								if(found && ((EduCourse)cur).cycle() != cycle) {
									tmp.insertObjectAtIndex(aCourse, i);
									currIndex = i;
									break;
								} else {
									found = (((EduCourse)cur).cycle() == cycle);
								}
							}
						}
					} else {
						Object cycle = tmp.replaceObjectAtIndex(aCourse, currIndex);
						if(!tmp.containsObject(cycle)) {
							popupCycles.addObject(cycle);
							try {
								popupCycles.sortUsingComparator(
										AdaptingComparator.sharedInstance);
							} catch (ComparisonException e) {
								logger.log(WOLogLevel.WARNING,"Error sorting cycles",
										new Object[] {session(),cycle,e});
							}
						}
					}
					courses = tmp.immutableClone();
				} else { //log change
					WOLogLevel level = WOLogLevel.UNOWNED_EDITING;
//					if(aCourse instanceof UseAccess && ((UseAccess)aCourse).isOwned())
//						level = WOLogLevel.OWNED_EDITING;
					logger.logp(level,"SrcMark","save","Saved changes in course",new Object[] {session(),aCourse});
				}
			} catch (NSValidation.ValidationException vex) {
				session().takeValueForKey(vex.getMessage(),"message");
				logger.logp(WOLogLevel.FINER,"SrcMark","save","Falied to save course",new Object[] {session(),aCourse,vex});
			}
		}
	} //save

    public void undoCreation() {
		currIndex = -1;
		ec.lock();
		try {
			ec.revert();
		} catch (NSValidation.ValidationException vex) {
			session().takeValueForKey(vex.getMessage(),"message");
		}
		ec.unlock();
        //return null;
    }
	
	public boolean isSelected() {
		return (cursIndex == currIndex && 
				courses.objectAtIndex(cursIndex) instanceof EduCycle &&
				access.flagForKey("create"));
	}
	
    public String rowStyle() {
        if(cursIndex == currIndex)//(item.equals(aCourse) || item.equals(aCycle))
			return "selection";
		
		if(isEduPlan())
			return "grey";
		else
 			return "green";
    }
    
	public String currTeacherName() {
		return teacherName(currTeacher);
	}
	
	public String teacherName() {
		return teacherName(dict.valueForKey("teacher"));
	}
	
	public String teacherName(Object teacher) {
		if(teacher == null)
			return (String)session().valueForKeyPath(
					"strings.Reusables_Strings.uiElements.Select");
		if(teacher == NullValue)
			return (String)session().valueForKeyPath(
					"strings.RujelBase_Base.vacant");
		if(teacher instanceof PersonLink)
			return Person.Utility.fullName((PersonLink)teacher,true,2,1,1);
		
		return "???";
	}
	
    public boolean cantCreate() {
		if (currClass == null || courses == null || currIndex >= courses.count() ||
				popupCycles == null || popupCycles.count() == 0)
			return true;
        if (access.flagForKey("edit"))
			return false;
		return true;
    }
    
    public boolean createNew() {
    	return (courses != null && currIndex >= courses.count());
    }
	
    public WOComponent editSubgroup() {
        WOComponent nextPage = null;//saveCourse();
		if(session().valueForKey("message") != null)
			return nextPage;
		nextPage = pageWithName("SubgroupEditor");

		nextPage.takeValueForKey(courses.objectAtIndex(currIndex),"course");
		session().takeValueForKey(this,"pushComponent");

        return nextPage;
    }
	
	public String title() {
		return (String)valueForKeyPath("application.strings.Strings.SrcMark.title");
    }

	public WOActionResults chooseTeacherForDict() {
		return TeacherSelector.selectorPopup(this, "dict.teacher", ec);
	}
	
	public WOActionResults chooseCurrentTeacher() {
		return TeacherSelector.selectorPopup(this, "currTeacher", ec);
	}

	public WOActionResults inspectCourse() {
		WOComponent result = pageWithName("CourseInspector");
		result.takeValueForKey(this, "returnPage");
		result.takeValueForKey(item, "course");
		return result;
	}
}
