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
import net.rujel.auth.*;
//import net.rujel.base.MyUtility;

import com.webobjects.foundation.*;
import com.webobjects.appserver.*;
import com.webobjects.eocontrol.*;
import com.webobjects.eoaccess.EOUtilities;
import java.util.Enumeration;
import java.util.logging.Logger;

public class SrcMark extends WOComponent {
	protected static Logger logger = Logger.getLogger("rujel.journal");
	protected NamedFlags _access;
	public static final NSArray accessKeys = new NSArray(new Object[] {
		"read","create","edit","delete","openCourses","createNewEduPlanCourses","editSubgroups"});

	private EOEditingContext ec;
	
	private boolean newCourse = false;
	
	public void appendToResponse(WOResponse aResponse, WOContext aContext) {
		newCourse = (aCourse != null && ec.insertedObjects().containsObject(aCourse));
		if(currIndex < 0 || courses.count() <= currIndex || !(courses.objectAtIndex(currIndex) instanceof EduCycle))
			teacherName = null;
		super.appendToResponse(aResponse,aContext);
	}
	
	public NamedFlags access() {
		if (_access == null) return DegenerateFlags.ALL_TRUE;
		return (NamedFlags)_access.immutableClone();
	}
	
	public EduGroup currClass;
	public Integer currGrade;
	public Teacher currTeacher;
	public NSArray courses;
	public EduCourse aCourse;
	
	public EOEnterpriseObject item;
	public NSArray gradeCycles;
//	public EduCycle cycleItem;
//	public EduCycle aCycle;
    public String subject;
	
	public EOEditingContext ec() {
		return ec;
	}
	
	
    public SrcMark(WOContext context) {
        super(context);
		ec = new SessionedEditingContext(session());
		ec.lock();
		ec.setSharedEditingContext(EOSharedEditingContext.defaultSharedEditingContext());
		UserPresentation user = (UserPresentation)session().valueForKey("user");
		try {
			int acc = user.accessLevel(EduCourse.entityName);
			if (acc == 0) throw new AccessHandler.UnlistedModuleException("Zero access");
			_access = new NamedFlags(acc,accessKeys);
		} catch (AccessHandler.UnlistedModuleException e) {
			try {
				_access = new NamedFlags(user.accessLevel("SrcMark"),accessKeys);
			} catch (AccessHandler.UnlistedModuleException e1) {
				logger.logp(WOLogLevel.CONFIG,"SrcMark","<init>","Can't get accessLevel",session());
				_access = DegenerateFlags.ALL_TRUE;
			}
		}
		EOGlobalID pLink = (EOGlobalID)session().valueForKey("userPersonGID");
		if(pLink instanceof Teacher) {
			currTeacher = (Teacher)ec.objectForGlobalID(pLink);
			courses = coursesForTeacher(currTeacher);
			//teacherName = Person.Utility.fullName(currTeacher.person(),true,2,1,1);
			byClass = false;
		}
		ec.unlock();
    }
	
	public void setCurrClass(EduGroup newClass) {
		if(newClass != null && newClass.editingContext() != ec) {
			currClass = (EduGroup)EOUtilities.localInstanceOfObject(ec, newClass);
			currGrade = currClass.grade();
		} else {
			currClass = newClass;
			currGrade = null;
		}
//		courses = coursesForClass(currClass);
	}

	public NSArray coursesForTeacher(Teacher teacher) {
		if(teacher == null) return null;
/*		NSTimestamp today = (NSTimestamp)session().valueForKey("today");
		if(today == null) today = new NSTimestamp();*/
		NSArray args = new NSArray(new Object[] {
			session().valueForKey("eduYear")/*MyUtility.eduYearForSession(session(),"today")*/,teacher
		});
		return EOUtilities.objectsWithQualifierFormat(ec,EduCourse.entityName,"eduYear = %d AND teacher = %@",args);
	}
	
	public NSArray coursesForClass(EduGroup aClass) {
		if(aClass == null)
			return null;
		NSMutableArray cycles = EduCycle.Lister.cyclesForEduGroup(aClass).mutableClone();

		NSArray args = new NSArray(new Object[] { session().valueForKey("eduYear") , aClass });
		NSArray existingCourses = EOUtilities.objectsWithQualifierFormat(ec,EduCourse.entityName,"eduYear = %d AND eduGroup = %@",args);
		//filter cycles
		
		NSMutableArray result = new NSMutableArray();
		Enumeration enumerator = cycles.objectEnumerator();
		EduCycle currCycle;
		EOQualifier qual;
		NSArray matches;
		int count = 0;
		NSMutableSet coursesSet = new NSMutableSet(existingCourses);
		while (enumerator.hasMoreElements()) {
			currCycle = (EduCycle)enumerator.nextElement();
			qual = new EOKeyValueQualifier("cycle",EOQualifier.QualifierOperatorEqual,currCycle);
			matches = EOQualifier.filteredArrayWithQualifier(existingCourses,qual);
			if(matches != null && matches.count() > 0) {
				count = matches.count();
				result.addObjectsFromArray(matches);
				coursesSet.subtractSet(new NSSet(matches));
			} else {
				count = 0;
			}
			Number subs = currCycle.subgroups();
			if(subs == null)
				subs = new Integer(1);
			count = subs.intValue() - count;
			if(count > 0) {
				for (int i = 0; i < count; i++) {
					result.addObject(currCycle);
				}
			}
		}
		if(coursesSet.count() > 0) {
			result.addObjectsFromArray(coursesSet.allObjects());
		}
		return result.immutableClone();
		//old implementation
		/*
		NSMutableDictionary cycleCourse = new NSMutableDictionary();
		Enumeration enumerator = existingCourses.objectEnumerator();
		EduCourse currCourse;
		EduCycle currCycle;
		int idx;
		while (enumerator.hasMoreElements()) {
			currCourse = (EduCourse)enumerator.nextElement();
			currCycle = currCourse.cycle();
			idx = cycles.indexOfObject(currCycle);
			if(idx >= 0) {
				currCycle = (EduCycle)cycles.replaceObjectAtIndex(currCourse,idx);
			} else {
				Object tmpCourse = cycleCourse.objectForKey(currCycle.subject());
				if(tmpCourse != null) {
					idx = cycles.indexOfObject(tmpCourse) + 1;
					cycles.insertObjectAtIndex(currCourse,idx);
				} else {
					cycles.addObject(currCourse);
				}
			}
			cycleCourse.setObjectForKey(currCourse,currCycle.subject());
		}
		return cycles.immutableClone();//existingCourses.arrayByAddingObjectsFromArray(cycles);
			*/
	}
	
    public boolean isEduPlan() {
        return (item instanceof EduCycle);
    }
	
    public WOComponent selectClass() {
		courses = coursesForClass(currClass);
		currIndex = -1;
		if(currClass != null)
			gradeCycles = EduCycle.Lister.cyclesForEduGroup(currClass);
		else
			gradeCycles = null;
		if(newCourse && currClass != null) {
			EduCycle cle = aCourse.cycle();
			if(cle != null) {
				subject = cle.subject();
				/*if(!currClass.grade().equals(cle.grade())) {
					EOQualifier qual = new EOKeyValueQualifier("subject",EOQualifier.QualifierOperatorLike,subject);
					NSArray like = EOQualifier.filteredArrayWithQualifier(gradeCycles,qual);
					
					if(like.count() == 1)
						aCycle = (EduCycle)like.objectAtIndex(0);
					else
						aCycle = null;
					
				}*/
			}
			aCourse.setEduGroup(currClass);
		} else {
			undoCreation();
	//		aCycle = null;
		}
		byClass = true;
        return null;
    }

    public WOComponent selectTeacher() {
    	if(currTeacher != null && currTeacher.editingContext() != ec)
    		currTeacher = (Teacher)EOUtilities.localInstanceOfObject(ec, currTeacher);
		if(teacherName != null) {
			teacherName = Person.Utility.fullName(currTeacher.person(),true,2,2,2);
			return null;
		} else {
			currIndex = -1;
		}
		if(newCourse)
			aCourse.setTeacher(currTeacher);
		else {
			undoCreation();
			courses = coursesForTeacher(currTeacher);
			gradeCycles = null;
			byClass = false;
			//teacherName = Person.Utility.fullName(currTeacher.person(),true,2,1,1);
	//		aCycle = null;
		}
        return null;
    }

    public WOComponent addCourse() {
		ec.lock();
		aCourse = (EduCourse)EOUtilities.createAndInsertInstance(ec,EduCourse.entityName);
		aCourse.setEduYear((Integer)session().valueForKey("eduYear"));//(MyUtility.eduYearForSession(session(),"today"));
		aCourse.setEduGroup(currClass);
		aCourse.setTeacher(currTeacher);
		if(currIndex >= 0) {
			Object cur = courses.objectAtIndex(currIndex);
			if(cur instanceof EduCycle)
				aCourse.setCycle((EduCycle)cur);
		}
	//	aCourse.setCycle(aCycle);
		ec.unlock();
        return null;
    }
	
	public String onClick() {
		String href = context().componentActionURL();
		if(!newCourse || (item instanceof EduCourse))
			return "return checkRun('" + href + "');";
		else
			return "if(tryLoad())window.location = '" + href +"';";
	}
	
    public int cursIndex;
	protected int currIndex = -1;
	
	public WOComponent select() {
		if(item instanceof EduCourse)
			return openCourse();
		if(item instanceof EduCycle)
			return selectCycle();
		return null;
	}
	
    public WOComponent selectCourse() {
		undoCreation();
		currIndex = cursIndex;
        aCourse = (EduCourse)item;
		currGrade = aCourse.eduGroup().grade();
		gradeCycles = EduCycle.Lister.cyclesForEduGroup(aCourse.eduGroup());
//		aCycle = aCourse.cycle();
		return null;
    }

    public WOComponent selectCycle() {
//		aCycle = (EduCycle)item;
		currIndex = cursIndex;
		/*if(ec.hasChanges()) {
			ec.lock();
			if(newCourse) {
				aCourse.setCycle((EduCycle)item);//(aCycle);
				subject = ((EduCycle)item).subject();//aCycle.subject();
			} else {
				ec.revert();
				aCourse = null;
			}
			ec.unlock();
		} else {
			aCourse = null;
		}*/
		if(currTeacher != null)
		teacherName = Person.Utility.fullName(currTeacher.person(),true,2,2,2);
        return null;
    }
	
    public WOComponent openCourse() {
		undoCreation();
		currIndex = -1;
		if(!item.equals(aCourse)) aCourse = null;
//		if(!((EduCourse)item).cycle().equals(aCycle)) aCycle = null;
		
//		Session ses = (Session)session();
		/*int acc = ses.accessLevel(item);
		if(acc < 0 && access.flagForKey("openCourses")) acc = 1;
		UserPresentation user = (UserPresentation)session().valueForKey("user");
		try {
			acc = user.accessLevel(item);
		} catch (AccessHandler.UnlistedModuleException e) {
			logger.logp(WOLogLevel.CONFIG,"SrcMark","<init>","Can't get accessLevel",session());
			acc = (access.flagForKey("openCourses"))?1:0;
		}*/
		
		Boolean canRead = (Boolean)session().valueForKeyPath("readAccess.read.item");
		if((canRead == null || !canRead.booleanValue()) && !access().flagForKey("openCourses")) {
			session().takeValueForKey(valueForKeyPath("application.strings.Strings.messages.noAccess"),"message");
			logger.log(WOLogLevel.READING,"Denied access to course",new Object[] {session(),item});
			return null;
		}
//		String pageName = SettingsReader.stringForKeyPath("ui.subRegime.openCourse","LessonNoteEditor");
		WOComponent nextPage = pageWithName("LessonNoteEditor");
		nextPage.takeValueForKey(item,"course");
//		nextPage.takeValueForKeyPath(item,"accessSchemeAssistant.lessons");
//		session().setObjectForKey(this,"SrcCourseComponent");
		session().takeValueForKey(this,"pushComponent");
		return nextPage;
    }
	
    public WOComponent saveCourse() {
		if(aCourse.eduGroup() == null) {
			String message = String.format((String)valueForKeyPath("application.strings.Strings.messages.parameterRequired"),(String)valueForKeyPath("application.strings.RujelInterfaces_Names.EduGroup.this"));
			session().takeValueForKey(message,"message");
			return null;
		}
		if(subject == null && aCourse.cycle() == null) {
			String message = String.format((String)valueForKeyPath("application.strings.Strings.messages.parameterRequired"),(String)valueForKeyPath("application.strings.RujelInterfaces_Names.EduCycle.subject"));
			session().takeValueForKey(message,"message");
			return null;
		}
		//boolean newCycle = false;
		ec.lock();
		if(subject != null && aCourse.cycle() == null) { //EduCycle from string
			NSDictionary dict = new NSDictionary(new Object[] {subject,aCourse.eduGroup().grade()},
												 new Object[] {"subject","grade"});
			EduCycle aCycle;
			try {
				aCycle = (EduCycle)EOUtilities.objectMatchingValues(ec,EduCycle.entityName,dict);
			} catch (com.webobjects.eoaccess.EOObjectNotAvailableException ex) {
				if(!access().flagForKey("createNewEduPlanCourses")) {
					session().takeValueForKey(valueForKeyPath("application.strings.Strings.messages.noAccess"),"message");
					logger.logp(WOLogLevel.OWNED_EDITING,"SrcMark","saveCourse","Denied to create new cycle for new course",session());
					return null;
				}
				aCycle = (EduCycle)EOUtilities.createAndInsertInstance(ec,EduCycle.entityName);
				aCycle.takeValuesFromDictionary(dict);
				/*aCycle.setSubject(subject);
				aCycle.setGrade(currGrade);*/
				if(aCourse.eduGroup().grade().equals(currGrade)) {
					if(gradeCycles == null)
						gradeCycles = new NSArray(aCycle);
					else
						gradeCycles = gradeCycles.arrayByAddingObject(aCycle);
				}
				//newCycle = true;
			}
			aCourse.setCycle(aCycle);
		} else {
//			aCycle = aCourse.cycle();
		}
		save();
		if(!ec.hasChanges() && !courses.containsObject(aCourse)) { //add new course to list
			int idx = courses.indexOfObject(aCourse.cycle());
			if(idx >= 0) {
				NSMutableArray tmp = courses.mutableClone();
				tmp.replaceObjectAtIndex(aCourse,idx);//aCycle = (EduCycle)
					courses = tmp.immutableClone();
			} else {
				courses = courses.arrayByAddingObject(aCourse);
			}
		}
		
		/*
		 if(currClass != null)
		 courses = coursesForClass(currClass);
		 if(currTeacher != null)
		 courses = coursesForTeacher(currTeacher); */
		ec.unlock();
        return null;
    } //sveCourse
	
	protected void save() {
		if(ec.hasChanges()) {
			if(newCourse) {
				if(!access().flagForKey("create")) {
					session().takeValueForKey(valueForKeyPath("application.strings.Strings.messages.noAccess"),"message");
					logger.logp(WOLogLevel.OWNED_EDITING,"SrcMark","save","Denied to create new course",session());
					return;
				}
			} else {
				Boolean allowEdit = (Boolean)aCourse.valueForKeyPath("access.edit");
				if(!((allowEdit == null && access().flagForKey("edit")) || (allowEdit != null && allowEdit.booleanValue()))) {
					session().takeValueForKey(valueForKeyPath("application.strings.Strings.messages.noAccess"),"message");
					logger.logp(WOLogLevel.OWNED_EDITING,"SrcMark","save","Denied course editing",session());
					return;
				}
			}
			try {
				ec.saveChanges();
				if(newCourse) { //log creation
					logger.logp(WOLogLevel.UNOWNED_EDITING,"SrcMark","save","Created new course",new Object[] {session(),aCourse});
					NSNotificationCenter.defaultCenter().postNotification(net.rujel.auth.AccessHandler.ownNotificationName,session().valueForKey("user"),new NSDictionary(aCourse,"EO"));
				} else { //log change
					WOLogLevel level = WOLogLevel.UNOWNED_EDITING;
					if(aCourse instanceof UseAccess && ((UseAccess)aCourse).isOwned())
						level = WOLogLevel.OWNED_EDITING;
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
			subject = null;
			aCourse = null;
		} catch (NSValidation.ValidationException vex) {
			session().takeValueForKey(vex.getMessage(),"message");
		}
		ec.unlock();
        //return null;
    }

    public WOComponent delete() {
		ec.lock();
		WOLogLevel level = WOLogLevel.UNOWNED_EDITING;
		if(aCourse.lessons().count() > 0)
			level = WOLogLevel.MASS_EDITING;
		else if(aCourse instanceof UseAccess && ((UseAccess)aCourse).isOwned())
			level = WOLogLevel.OWNED_EDITING;
 		try {
			//EOGlobalID id = ec.globalIDForObject(aCourse);
			if(newCourse)
				ec.revert();
			else {
				aCourse.validateForDelete();
				logger.logp(level,"SrcMark","delete","Deleting EduCourse",new Object[] {session(),aCourse});
				ec.deleteObject(aCourse);
				ec.saveChanges();
			}
			subject = null;
		} catch (NSValidation.ValidationException vex) {
			logger.logp(level,"SrcMark","delete","Deletion failed: ",new Object[] {session(),aCourse,vex});
			session().takeValueForKey(vex.getMessage(),"message");
		}
		if(courses != null & courses.count() > 0) {
			NSMutableArray editedCourses = courses.mutableClone();
			editedCourses.removeObject(aCourse);
			courses = editedCourses.immutableClone();
		}
		aCourse = null;
//		courses = coursesForClass(currClass);
		ec.unlock();
		return null;
    }
/*
    public boolean canDelete() {
        return access.flagForKey("delete");
    }
*/	
	public String getSubject() {
		if(aCourse != null && aCourse.cycle() != null && subject == null)
			return aCourse.cycle().subject();
		else
			return subject;
	}
	
	public boolean isSelected() {
		return (cursIndex == currIndex && courses.objectAtIndex(cursIndex) instanceof EduCycle && access().flagForKey("create"));
	}
	
    public String rowStyle() {
        if(cursIndex == currIndex)//(item.equals(aCourse) || item.equals(aCycle))
			return "selection";
		
		if(isEduPlan())
			return "eduPlan";
		else
			return "course";
    }
	
	
    /** @TypeInfo Teacher */
    public NSArray teachers() {
		NSArray sesList = (NSArray)session().valueForKey("sortedPersList");
		NSMutableArray result = new NSMutableArray();
        Enumeration enumerator = sesList.objectEnumerator();
		Object curr;
		while(enumerator.hasMoreElements()) {
			curr = enumerator.nextElement();
			if(curr instanceof Teacher)
				result.addObject(EOUtilities.localInstanceOfObject(ec,(EOEnterpriseObject)curr));
		}
		if (aCourse != null) {
			Teacher t = aCourse.teacher();
			if(t != null && !result.containsObject(t)) {
				result.insertObjectAtIndex(t,0);
			}
		}
		return result;
    }
	
	public NSArray classes() {
		return ClassListing.listGroups((NSTimestamp)session().valueForKey("today"),ec);
	}
	
	public String itemToString() {
		if(item instanceof Person) {
			return Person.Utility.fullName((Person)item,true,2,1,1);
		}
		return item.toString();
	}
	
    public boolean canCreate() {
		if (aCourse != null)
			return false;
        if (access().flagForKey("createNewEduPlanCourses"))
			return true;
/*		if (aCycle != null & currClass != null & access.flagForKey("create"))
			return true; */
		return false;
    }
	
//	public NSArray tabs = new NSArray(new String[] {"Классы","Учителя"});
    public WOComponent editSubgroup() {
        WOComponent nextPage = saveCourse();
		if(session().valueForKey("message") != null)
			return nextPage;
//		String pageName = SettingsReader.stringForKeyPath("ui.subRegime.editCourseSubgroup","SubgroupEditor");
		nextPage = pageWithName("SubgroupEditor");

		nextPage.takeValueForKey(aCourse,"course");
//		session().setObjectForKey(this,"SrcCourseComponent");
		session().takeValueForKey(this,"pushComponent");

        return nextPage;
    }
	
	public boolean byClass = true;
	
	public NSArray tablist = (NSArray)valueForKeyPath("application.strings.Strings.SrcMark.tabs");
	public int tabindex = 0;
    public String teacherName;
	
	public String tabSelected() {
		if(tabindex == NSArray.NotFound) return null;
		try {
			return (String)tablist.objectAtIndex(tabindex);
		} catch (Exception ex) {
			return null;
		}
	}
		
	public void setTabSelected(String tabName) {
		tabindex = tablist.indexOfObject(tabName);
		currIndex = -1;
	}
	
	public String title() {
		return (String)valueForKeyPath("application.strings.Strings.SrcMark.title");
		// return "Выбор Курса";
    }

    public void submitTeacher() {
        if(teacherName == null || teacherName.length() == 0) {
			currIndex = -1;
			if(ec.hasChanges()) {
				ec.lock();
				ec.revert();
				ec.unlock();
			}
			return;
		}
		if(currTeacher != null && teacherName.equals(Person.Utility.fullName(currTeacher.person(),true,2,2,2))) {
			addCourse();
			newCourse = true;
			ec.lock();
			save();
			if(ec.hasChanges()) {
				ec.revert();
				aCourse = null;
			}
			ec.unlock();
			if(aCourse != null) {
				NSMutableArray tmp = courses.mutableClone();
				tmp.replaceObjectAtIndex(aCourse,currIndex);
				courses = tmp.immutableClone();
				teacherName = null;
				aCourse = null;
			}
		} else {
			tabindex = 1;
		}
    }

}
