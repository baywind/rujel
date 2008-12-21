// Overview.java: Class file for WO Component 'Overview'

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

import com.webobjects.foundation.*;
import com.webobjects.appserver.*;
import com.webobjects.eocontrol.*;
import com.webobjects.eoaccess.*;

import java.text.Format;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.logging.Logger;
import net.rujel.reusables.WOLogLevel;
import net.rujel.base.MyUtility;

public class Overview extends WOComponent {
	protected static Logger logger = Logger.getLogger("rujel.journal");
 	
	public Number currentMode;
	
	public static final NSArray accessKeys = new NSArray(new Object[] {"open","subjects","students"});
	
	protected NamedFlags _access;
	public NamedFlags access() {
		if(_access == null) {
			UserPresentation user = (UserPresentation)session().valueForKey("user");
			if(user != null) {
				try {
					int lvl = user.accessLevel("Overview");
					_access = new ImmutableNamedFlags(lvl,accessKeys);
				}  catch (AccessHandler.UnlistedModuleException e) {
					_access = DegenerateFlags.ALL_TRUE;
				}
			}
			if(_access == null)
				_access = DegenerateFlags.ALL_TRUE;
		}
		return _access;
	}
	
	public EOEditingContext ec;
    public EduGroup currClass;
	
    /** @TypeInfo com.webobjects.foundation.NSDictionary */
    public NSMutableArray subjects;
	public NSArray existingCourses;
    public NSMutableDictionary currSubject;
    public NSDictionary subjItem;
	
    public NSTimestamp since;
    public NSTimestamp to;
	
	public NSArray periods = (NSArray)session().valueForKeyPath("modules.periods");;
	public Period period;
    public Period perItem;

	public void setPeriod(Period newPeriod) {
		period = newPeriod;
		if(newPeriod == null)
			return;
		java.util.Date date = newPeriod.begin();
		if(date instanceof NSTimestamp) {
			since = (NSTimestamp)date;
		} else {
			since = new NSTimestamp(date);
		}
		date = newPeriod.end();
		if(date instanceof NSTimestamp) {
			to = (NSTimestamp)date;
		} else {
			to = new NSTimestamp(date);
		}
	}
	
    /** @TypeInfo com.webobjects.foundation.NSMutableDictionary */
    public NSArray courses;
    public NSMutableDictionary courseItem;
	
    public Overview(WOContext context) {
        super(context);
		ec = new SessionedEditingContext(session());
		ec.lock();
		ec.setSharedEditingContext(EOSharedEditingContext.defaultSharedEditingContext());
		ec.unlock();
		GregorianCalendar cal = new GregorianCalendar();
		cal.setTime((NSTimestamp)session().valueForKey("today"));
		cal.set(GregorianCalendar.HOUR_OF_DAY, 0);
		cal.set(GregorianCalendar.MINUTE, 0);
		cal.set(GregorianCalendar.SECOND, 0);
		cal.set(GregorianCalendar.MILLISECOND, 0);
		to = new NSTimestamp(cal.getTime());
		cal.add(GregorianCalendar.DATE, -7);
		since = new NSTimestamp(cal.getTime());//to.timestampByAddingGregorianUnits(0, 0, -7, 0, 0, 0);
		int mode = (access().flagForKey("students") && !access().flagForKey("subjects"))?1:0;
		currentMode = new Integer(mode);
		session().savePageInPermanentCache(this);
    }
	
    public WOComponent selectClass() {
		unselect();
		existingCourses = null;
		NSArray cycles = EduCycle.Lister.cyclesForEduGroup(currClass);
		//EOUtilities.objectsMatchingKeyAndValue(ec,EduCycle.entityName,"grade",currClass.grade());
		
		NSArray args = new NSArray(new Object[] {session().valueForKey("eduYear") , currClass });
		existingCourses = EOUtilities.objectsWithQualifierFormat(ec,EduCourse.entityName,"eduYear = %d AND eduGroup = %@",args);
		subjects = new NSMutableArray();
		
		Enumeration enumerator = cycles.objectEnumerator();
		while (enumerator.hasMoreElements()) { //prepare subjects listing
			EduCycle currCycle = (EduCycle)enumerator.nextElement();
			EOQualifier qual = new EOKeyValueQualifier("cycle",EOQualifier.QualifierOperatorEqual,currCycle);
			NSArray matches = EOQualifier.filteredArrayWithQualifier(existingCourses,qual);
			currSubject = new NSMutableDictionary(currCycle,"cycle");
			currSubject.takeValueForKey(currCycle.subject(),"subject");
			if(matches != null && matches.count() > 0) {
				currSubject.takeValueForKey(new Integer(matches.count()),"count");
				currSubject.takeValueForKey(matches,"courses");
				currSubject.takeValueForKey("course","style");
			} else {
				currSubject.takeValueForKey(new Integer(0),"count");
				currSubject.takeValueForKey("eduPlan","style");
			}
			subjects.addObject(currSubject);
		}
		currSubject = null;
		return null;
    }
    
    public boolean showSelector() {
    	if(currSubject == null)
    		return false;
    	if(present() == null)
    		return false;
    	return (present().valueForKey("selector") != null);
    }
	
	protected void unselect() {
		selectedStudents.removeAllObjects();
		courses = null;
		currStudent = null;
		reporters = null;
		if(currSubject == null) return;
		Number count = (Number)currSubject.objectForKey("count");
		if(count != null && count.intValue() > 0)
			currSubject.setObjectForKey("course","style");
		else
			currSubject.setObjectForKey("eduPlan","style");
		currSubject = null;
/*		if(activeNotesAddOns != null && activeNotesAddOns.count() > 0) {
			Object obj = activeNotesAddOns.objectAtIndex(0);
			if(obj instanceof NSKeyValueCoding)
				activeNotesAddOns = (NSMutableArray)activeNotesAddOns.valueForKey("id");
			else if(!(obj instanceof String))
				activeNotesAddOns = null;
		}
		notesAddOns = null;*/
	}
	
	public void updateLessonLists() {
		if(courses == null || courses.count() == 0) return;
		Enumeration enu = courses.objectEnumerator();
		NSArray args = new NSArray(new Object[] { since,to });
		EOQualifier qual = EOQualifier.qualifierWithQualifierFormat("date >= %@ AND date <= %@",args);
		NSKeyValueCoding present = present();
		while(enu.hasMoreElements()) {
			courseItem = (NSMutableDictionary)enu.nextElement();
			//NSArray tmp = (NSArray)courseItem.valueForKeyPath("course.lessons");
			EduCourse course = (EduCourse)courseItem.valueForKey("course");
			NSMutableArray qualifiers = new NSMutableArray(qual);
			NSArray lessons = LessonNoteEditor.lessonListForCourseAndPresent(
					course, present, qualifiers);
			courseItem.setObjectForKey(lessons,"lessonsList");
		}
		/*NSArray notesAddOns = (NSArray)session().objectForKey("notesAddOns");
		if(notesAddOns != null)
			notesAddOns.takeValueForKey((period instanceof EOEnterpriseObject)?
					period:null, "eduPeriod");*/
		setCurrTab(null);
	}
	
    public void selectSubject() {
		unselect();
		if(subjItem instanceof NSMutableDictionary)
			currSubject = (NSMutableDictionary)subjItem;
		else
			currSubject = subjItem.mutableClone();
		currSubject.setObjectForKey("selection","style");
		
		courses = (NSArray)currSubject.valueForKey("courses");
		if(courses == null || courses.count() == 0) return;
		Enumeration enu = courses.objectEnumerator();
		NSArray args = new NSArray(new Object[] { since,to });
		EOQualifier qual = EOQualifier.qualifierWithQualifierFormat("date >= %@ AND date <= %@",args);
		courses = new NSMutableArray();
		NSKeyValueCoding present = present();
		while(enu.hasMoreElements()) {
			EduCourse currCourse = (EduCourse)enu.nextElement();
			courseItem = new NSMutableDictionary();
			courseItem.setObjectForKey(currCourse,"course");
			/*NSArray tablist = (NSArray)currCourse.valueForKey("sortedTabs");
			if(tablist != null && tablist.count() > 0) {
				courseItem.setObjectForKey(tablist,"tablist");
				courseItem.setObjectForKey(Boolean.TRUE,"hasTabs");
			} else {
				courseItem.setObjectForKey(Boolean.FALSE,"hasTabs");
			}*/
			NSMutableArray qualifiers = new NSMutableArray(qual);
			NSArray lessons = LessonNoteEditor.lessonListForCourseAndPresent(
					currCourse, present, qualifiers);
			courseItem.setObjectForKey(lessons,"lessonsList");
			((NSMutableArray)courses).addObject(courseItem);
		}
 		logger.logp(WOLogLevel.READING,"Overview","selectSubject","Opening subject '" + currSubject.objectForKey("subject") + "' (" + currSubject.objectForKey("count") + ") courses",session());
	}
	
	public EduCourse course() {
		if(courseItem == null) return null;
		return (EduCourse) courseItem.valueForKey("course");
	}
    /** @TypeInfo BaseTab */
    public EOEnterpriseObject currTab()  {
        if(courseItem == null) return null;
		return (EOEnterpriseObject) courseItem.valueForKey("currTab");
    }
    public void setCurrTab(EOEnterpriseObject newCurrTab) {
        if(newCurrTab == null) {
			courseItem.removeObjectForKey("currTab");
		} else {
			courseItem.setObjectForKey(newCurrTab,"currTab");
			courseItem.setObjectForKey(newCurrTab.valueForKey("lessonsInTab"),"lessonsList");
			since = (NSTimestamp)newCurrTab.valueForKeyPath("lessonsInTab.@min.date");
			to = (NSTimestamp)newCurrTab.valueForKeyPath("lessonsInTab.@max.date");
		}
    }
	
	public NSMutableSet selectedStudents = new NSMutableSet();
    public Student currStudent;
	/*public Student studentItem;

	public boolean studentSelected() {
        return selectedStudents.containsObject(studentItem);
    }
	
    public void setStudentSelected(boolean newStudentSelected) {
		if(newStudentSelected)
			selectedStudents.addObject(studentItem);
		else
			selectedStudents.removeObject(studentItem);
    }
	public String studentStyle() {
		if(currStudent == studentItem) return "selection";
		Boolean sex = studentItem.person().sex();
		if(sex == null) return "grey";
		return (sex.booleanValue())?"male":"female";
	}
	*/
	public WOActionResults selectStudent() {
		//currStudent = studentItem;
 		logger.logp(WOLogLevel.READING,"Overview","selectStudent","Opening marks for student",new Object[] {session(),currStudent});
		return null;
		/*NSMutableSet tmp = selectedStudents.mutableClone();
		selectedStudents.removeAllObjects();
		selectedStudents.addObject(studentItem);
		WOActionResults result = printSelectedStudents();
		selectedStudents = tmp;
		return result;*/
	}
	
	public WOActionResults printSelectedStudents() {
		NSMutableArray studentsToReport = selectedStudents.allObjects().mutableClone();
		EOSortOrdering.sortArrayUsingKeyOrderArray(studentsToReport,Person.sorter);
		WOComponent reportPage = pageWithName("PrintReport");
		reportPage.takeValueForKey(reporter,"reporter");
		reportPage.takeValueForKey(existingCourses,"courses");
		reportPage.takeValueForKey(studentsToReport,"students");
		reportPage.takeValueForKey(since,"since");
		reportPage.takeValueForKey(to,"to");
		reportPage.takeValueForKey(period,"period");
		reportPage.takeValueForKey(currClass, "eduGroup");
		Format dateFormat = MyUtility.dateFormat();
 		logger.logp(WOLogLevel.MASS_READING,"Overview","selectStudent",
 				"Printing marks for multiple (" + studentsToReport.count() + ") students ("
 				+ dateFormat.format(since) + " - " + dateFormat.format(to) +')',
 				new Object[] {session(),currClass});
		return reportPage;
	}
	
	//public static final String reporter = SettingsReader.stringForKeyPath("ui.presenter.report","StudentMarks");
	public NSKeyValueCoding reporter;
	public NSKeyValueCoding reporterItem;
	
	protected NSMutableArray reporters;
	public NSArray reporterList() {
		if(reporters == null) {
			reporters = (NSMutableArray)session().valueForKeyPath("modules.studentReporter");
			Object title = null;
			if(reporter != null && reporters != null && reporters.count() > 0) {
				title = reporter.valueForKey("title");
			}
			reporter = (NSDictionary)application().valueForKeyPath("extStrings.Overview.defaultReporter");
			reporters.insertObjectAtIndex(reporter,0);				
			if(title != null) {
				if(!title.equals(reporter.valueForKey("title"))) {
					Enumeration enu = reporters.objectEnumerator();
					while (enu.hasMoreElements()) {
						reporter = (NSKeyValueCoding)enu.nextElement();
						if(title.equals(reporter.valueForKey("title")))
							return reporters;
					}
					reporter = (NSKeyValueCoding)reporters.objectAtIndex(0);
				}
			}
		}
		return reporters;
	}
	public void selectReporter() {
		reporter = reporterItem;
	}
	public boolean showReporterSelector() {
		return (reporterList().count() > 1);
	}
	public String reporterItemStyle() {
		if(reporterItem == reporter) {
			return "cursor:pointer;font-weight:bold;";
		} else {
			return "cursor:pointer;color:blue;";
		}
	}
	/*
	 public static void testTask() {
		 logger.info("Test task executed successfully");
	 }*/
	
//	public NSKeyValueCoding present;
	public NSArray presentTabs;
	public NSArray presentTabs() {
		if(presentTabs == null) {
			presentTabs = (NSArray)session().valueForKeyPath("modules.presentTabs");
			if(presentTabs != null && presentTabs.count() > 0)
				_present = (NSKeyValueCoding)presentTabs.objectAtIndex(0);
		}
		return presentTabs;
	}
	
	public void setPresent(NSKeyValueCoding pres) {
		_present = pres;
		updateLessonLists();
	}
	
/*	public NSArray notesAddOns;
	
	public void setNotesAddOns(NSArray addons) {
		notesAddOns = addons;
		if(notesAddOns != null && period instanceof EOEnterpriseObject)
			notesAddOns.takeValueForKey(period, "eduPeriod");
	}
	
	public NSMutableArray activeNotesAddOns;
*/ 
	protected NSKeyValueCoding _present;
 
	public NSKeyValueCoding present() {
		if(_present == null) {
			NSArray presTabs = presentTabs();
			if(presTabs != null && presTabs.count() > 0)
				_present = (NSKeyValueCoding)presTabs.objectAtIndex(0);
			else
				_present = NSDictionary.EmptyDictionary;
		}
		return _present;
	}
	/*
	public WOComponent sendMails() {
		net.rujel.contacts.EMailBroadcast.broadastMarksForPeriod(period,null);
		return this;
	}*/
	
	public WOActionResults overviewAction() {
		NSMutableArray studentsToReport = selectedStudents.allObjects().mutableClone();
		EOSortOrdering.sortArrayUsingKeyOrderArray(studentsToReport,Person.sorter);

		WOComponent nextPage = pageWithName((String)reporterItem.valueForKey("component"));
		NSMutableDictionary dict = new NSMutableDictionary();
		dict.takeValueForKey(reporter,"reporter");
		dict.takeValueForKey(existingCourses,"courses");
		dict.takeValueForKey(studentsToReport,"students");
		dict.takeValueForKey(since,"since");
		dict.takeValueForKey(to,"to");
		dict.takeValueForKey(period,"period");
		//dict.takeValueForKey(ec,"editingContext");
		dict.takeValueForKey(currClass,"eduGroup");
		
		nextPage.takeValueForKey(dict,"dict");
		return nextPage;
	}
	
	public WOActionResults showThemes() {
		WOComponent nextPage = pageWithName("PrintLessons");
		nextPage.takeValueForKey(courseItem.valueForKey("course"), "course");
		Period per = period;
		if(per == null) {
			per = new Period.ByDates(since,to);
		}
		nextPage.takeValueForKey(per, "period");
		return nextPage;
	}
	
	protected WOComponent subsReport;
	public WOComponent subsReport() {
		if(subsReport == null) {
			subsReport = pageWithName("SubsReport");
		}
		subsReport.ensureAwakeInContext(context());
		subsReport.takeValueForKey(since, "begin");
		subsReport.takeValueForKey(to, "end");
		return subsReport;
	}
	
	public String subTitle() {
		if(!Various.boolForObject(session().valueForKeyPath("readAccess.read.SubsReport")))
			return null;
		String result = (String)application().valueForKeyPath(
				"extStrings.RujelCurriculum_Curriculum.subsReport");
		return String.format(result, subsReport().valueForKeyPath("substitutes.count"));
	}
}
