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
import net.rujel.reports.ReportsModule;
import net.rujel.reusables.*;

import com.webobjects.foundation.*;
import com.webobjects.appserver.*;
import com.webobjects.eocontrol.*;
import com.webobjects.eoaccess.*;

import java.text.FieldPosition;
import java.text.Format;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.logging.Logger;
import net.rujel.reusables.WOLogLevel;
import net.rujel.base.BaseCourse;
import net.rujel.base.MyUtility;
import net.rujel.base.XMLGenerator;

public class Overview extends WOComponent {
	protected static Logger logger = Logger.getLogger("rujel.journal");
 	
	public Number currentMode;
	
	public static final NSArray accessKeys = new NSArray(new Object[] {"open","subjects","students"});
	
	protected NamedFlags _access;
	public NamedFlags access() {
		if(_access == null) {
			_access = (NamedFlags)session().valueForKeyPath("readAccess.FLAGS.Overview");
			_access.setKeys(accessKeys);
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
	
	public Period period;
    public NSTimestamp since;
    public NSTimestamp to;
    /*	
	public NSArray periods = (NSArray)session().valueForKeyPath("modules.periods");;
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
*/	
    /** @TypeInfo com.webobjects.foundation.NSMutableDictionary */
    public NSArray courses;
    public NSMutableDictionary courseItem;
	
    public Overview(WOContext context) {
        super(context);
		ec = new SessionedEditingContext(session());
		GregorianCalendar cal = new GregorianCalendar();
		cal.setTime((NSTimestamp)session().valueForKey("today"));
		cal.set(GregorianCalendar.HOUR_OF_DAY, 0);
		cal.set(GregorianCalendar.MINUTE, 0);
		cal.set(GregorianCalendar.SECOND, 0);
		cal.set(GregorianCalendar.MILLISECOND, 0);
		to = new NSTimestamp(cal.getTime());
		session().setObjectForKey(to, "recentDate");
		cal.add(GregorianCalendar.DATE, -7);
		since = new NSTimestamp(cal.getTime());//to.timestampByAddingGregorianUnits(0, 0, -7, 0, 0, 0);
		int mode = (access().flagForKey("students") && !access().flagForKey("subjects"))?1:0;
		currentMode = new Integer(mode);
//		session().savePageInPermanentCache(this);
    }
	
    public WOComponent selectClass() {
		unselect();
		existingCourses = null;
		NSArray cycles = EduCycle.Lister.cyclesForEduGroup(currClass);
		//EOUtilities.objectsMatchingKeyAndValue(ec,EduCycle.entityName,"grade",currClass.grade());
		
		NSArray args = new NSArray(new Object[] {session().valueForKey("eduYear") , currClass });
		existingCourses = EOUtilities.objectsWithQualifierFormat(ec,
				EduCourse.entityName,"eduYear = %d AND eduGroup = %@",args);
		if(existingCourses != null && existingCourses.count() > 1)
			existingCourses = EOSortOrdering.sortedArrayUsingKeyOrderArray(
					existingCourses, EduCourse.sorter);
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
				currSubject.takeValueForKey("green","style");
			} else {
				currSubject.takeValueForKey(new Integer(0),"count");
				currSubject.takeValueForKey("grey","style");
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
    
    public WOActionResults resetSelections() {
    	unselect();
		existingCourses = null;
    	currClass = null;
    	subjects = null;
    	return null;
    }
	
	protected void unselect() {
		selectedStudents.removeAllObjects();
		courses = null;
		currStudent = null;
		reporters = null;
		if(currSubject == null) return;
		Number count = (Number)currSubject.objectForKey("count");
		if(count != null && count.intValue() > 0)
			currSubject.setObjectForKey("green","style");
		else
			currSubject.setObjectForKey("grey","style");
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
	
	protected EOQualifier periodQualifier(String field) {
		if(since == null) {
			if(to == null)
				return null;
			else
				return new EOKeyValueQualifier(field,
						EOQualifier.QualifierOperatorLessThanOrEqualTo,to);
		} else if(to == null) {
			return new EOKeyValueQualifier(field,
					EOQualifier.QualifierOperatorGreaterThanOrEqualTo,since);
		}
		EOQualifier[] quals = new EOQualifier[2];
		quals[0] = new EOKeyValueQualifier(field,
				EOQualifier.QualifierOperatorGreaterThanOrEqualTo,since);
		quals[1] = new EOKeyValueQualifier(field,
				EOQualifier.QualifierOperatorLessThanOrEqualTo,to);
		return new EOAndQualifier(new NSArray(quals));
	}
	
	public void updateLessonLists() {
		if(courses == null || courses.count() == 0) return;
		Enumeration enu = courses.objectEnumerator();
		if(to == null) {
			session().removeObjectForKey("recentDate");	
		}
		EOQualifier qual = periodQualifier("date");
		NSKeyValueCoding present = present();
		while(enu.hasMoreElements()) {
			courseItem = (NSMutableDictionary)enu.nextElement();
			//NSArray tmp = (NSArray)courseItem.valueForKeyPath("course.lessons");
			EduCourse course = (EduCourse)courseItem.valueForKey("course");
			NSArray lessons = LessonNoteEditor.lessonListForCourseAndPresent(
					course, present, qual);
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
		EOQualifier qual = periodQualifier("date");
		courses = new NSMutableArray();
		NSKeyValueCoding present = present();
		while(enu.hasMoreElements()) {
			EduCourse currCourse = (EduCourse)enu.nextElement();
			courseItem = new NSMutableDictionary();
			courseItem.setObjectForKey(currCourse,"course");
			NSArray lessons = LessonNoteEditor.lessonListForCourseAndPresent(
					currCourse, present, qual);
			courseItem.setObjectForKey(lessons,"lessonsList");
			((NSMutableArray)courses).addObject(courseItem);
		}
		EOSortOrdering.sortArrayUsingKeyOrderArray(((NSMutableArray)courses), EduCourse.sorter);
 		logger.log(WOLogLevel.READING,"Opening subject '" + currSubject.objectForKey("subject") +
 				"' (" + currSubject.objectForKey("count") + ") courses",session());
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
	
	public WOActionResults genarateXML() {
		NSMutableDictionary reportSettings = new NSMutableDictionary();
		reportSettings.takeValueForKey(existingCourses,"courses");
//		NSMutableArray studentsToReport = selectedStudents.allObjects().mutableClone();
//		reportSettings.takeValueForKey(studentsToReport,"students");
		reportSettings.takeValueForKey(since,"since");
		reportSettings.takeValueForKey(to,"to");
		reportSettings.takeValueForKey(period,"period");
		reportSettings.takeValueForKey(currClass, "eduGroup");

		/*
		NSMutableDictionary settings = (NSMutableDictionary)session().objectForKey(
				ReporterSetup.SETTINGS);
		if(settings == null) {
			settings = ReporterSetup.getDefaultSettings(session());
			settings.takeValueForKey(session().valueForKeyPath(
			"strings.Strings.PrintReport.defaultSettings"), "title");
			session().setObjectForKey(settings,ReporterSetup.SETTINGS);
		}
		reportSettings.takeValueForKey(settings, "settings");
		*/
		byte[] result = null;
		try {
			result = XMLGenerator.generate(session(), reportSettings);
		} catch (Exception e) {
			result = WOLogFormatter.formatTrowable(e).getBytes();
		}
		WOResponse response = application().createResponseInContext(context());
		response.setContent(result);
		response.setHeader("application/xml","Content-Type");
		return response;
	}
	
	public WOActionResults printCurrStudent() {
		return printSelectedStudents(new NSArray(currStudent));
	}

	public WOActionResults printSelectedStudents() {
		NSMutableArray studentsToReport = selectedStudents.allObjects().mutableClone();
		EOSortOrdering.sortArrayUsingKeyOrderArray(studentsToReport,Person.sorter);
		return printSelectedStudents(studentsToReport);
	}
	
	public WOActionResults printSelectedStudents(NSArray studentsToReport) {
		NSKeyValueCoding reportPage = null;
		boolean xml = (reporter.valueForKey("component") == null);
		if(xml)
			reportPage = new NSMutableDictionary();
		else
			reportPage = pageWithName("PrintReport");
		reportPage.takeValueForKey(reporter,"reporter");
		reportPage.takeValueForKey(BaseCourse.coursesForStudent(existingCourses, studentsToReport)
				,"courses");
		reportPage.takeValueForKey(studentsToReport,"students");
		reportPage.takeValueForKey(since,"since");
		reportPage.takeValueForKey(to,"to");
		reportPage.takeValueForKey(period,"period");
		reportPage.takeValueForKey(currClass, "eduGroup");
		StringBuffer buf = new StringBuffer("Printing marks for ");
		if(studentsToReport.count() > 1) {
			buf.append("multiple (");
			buf.append(studentsToReport.count()).append(") students (");
		} else if(studentsToReport.count() == 1){
			Student st = (Student)studentsToReport.objectAtIndex(0);
			buf.append(WOLogFormatter.formatEO(st)).append(' ').append('(');
		}
		NSMutableDictionary info = (xml)?new NSMutableDictionary(MyUtility.presentEduYear(
					(Integer)session().valueForKey("eduYear")), "eduYear"):null;
		if(period instanceof EOPeriod) {
			String pername = (String)((EOPeriod)period).valueForKey("title");
			buf.append(pername);
			if(xml)
				info.takeValueForKey(((EOPeriod)period).valueForKey("name"), "period");
		}
		if(since != null || to != null) {
			if(period != null)
				buf.append(':').append(' ');
			int idx = buf.length();
			Format dateFormat = MyUtility.dateFormat();
			FieldPosition fp = new FieldPosition(0);
			if(since != null)
				dateFormat.format(since, buf, fp);
			else
				buf.append("...");
			buf.append(" - "); 
			if(to != null)
				dateFormat.format(to, buf, fp);
			else
				buf.append("...");
			if(xml)
				info.takeValueForKey(buf.substring(idx), "dates");
		}
		buf.append(')');
 		logger.log((studentsToReport.count() > 1)?WOLogLevel.MASS_READING:WOLogLevel.READING,
 				buf.toString(), new Object[] {session(),currClass});
 		if(xml) {
 			reportPage.takeValueForKey(info, "info");
 			byte[] result = null;
 			try {
 				result = XMLGenerator.generate(session(), (NSDictionary)reportPage);
 			} catch (Exception e) {
 				result = WOLogFormatter.formatTrowable(e).getBytes();
 			}
 			WOResponse response = application().createResponseInContext(context());
 			response.setContent(result);
 			String contentType = (String)reporter.valueForKey("ContentType");
 			if(contentType != null)
 				response.setHeader(contentType,"Content-Type");
 			else if (reporter.valueForKey("transform") == null)
 				response.setHeader("application/xml","Content-Type");
 			return response;

 		} else {
 			return (WOComponent)reportPage;
 		}
	}
	
	//public static final String reporter = SettingsReader.stringForKeyPath("ui.presenter.report","StudentMarks");
	public NSKeyValueCoding reporter;
	public NSKeyValueCoding reporterItem;
	
	protected NSMutableArray reporters;
	public NSArray reporterList() {
		if(reporters == null) {
			Object title = null;
			if(reporter != null) {
				title = reporter.valueForKey("title");
			}
//			reporter = (NSDictionary)session().valueForKeyPath(
//					"strings.Strings.Overview.defaultReporter");
			reporters = new NSMutableArray();
			NSArray list = (NSArray)session().valueForKeyPath("modules.studentReporter");
			if(list != null && list.count() > 0)
				reporters.addObjectsFromArray(list);
			EOQualifier qual = new EOKeyValueQualifier("id",
					EOQualifier.QualifierOperatorNotEqual, null);
			list = ReportsModule.reportsFromDir("StudentReport", session(), qual);
			if(list != null && list.count() > 0)
				reporters.addObjectsFromArray(list);
			if(reporters.count() > 1) {
	    		EOSortOrdering.sortArrayUsingKeyOrderArray(reporters, ModulesInitialiser.sorter);
			}
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
	
	public boolean reporterIsCurr() {
		return (reporter == reporterItem);
	}

	public WOActionResults selectReporter() {
		reporter = reporterItem;
		return null;
	}
	public WOComponent editReporter() {
		WOComponent result = pageWithName("ReporterSetup");
		result.takeValueForKey(this, "returnPage");
		result.takeValueForKey(reporter, "reporter");
		return result;
	}
	public Boolean hideReporterEdit() {
		if(reporter != reporterItem || reporter.valueForKey("options") == null)
			return Boolean.TRUE;
		return (Boolean)session().valueForKeyPath("readAccess._read.ReporterSetup");
	}
	public boolean showReporterSelector() {
		boolean result = (reporterList().count() > 1);
		if(!result)
			reporterItem = reporter;
		return result;
	}
	public String reporterItemStyle() {
		if(reporterItem == reporter) {
			return "font-weight:bold;padding:2px 1.3ex;";
		} else {
			return "cursor:pointer;color:blue;padding:2px 1.3ex;";
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
		if(Various.boolForObject(reporterItem.valueForKey("redirect")))
			return RedirectPopup.getRedirect(context(), nextPage,
					(String)reporterItem.valueForKey("target"));
		try {
			nextPage.takeValueForKey(this, "returnPage");
		} catch (Exception e) {
			// return not supported
		}
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
	
	public NSArray allLessons() {
		if(courses == null || courses.count() == 0)
			return null;
		NSMutableArray result = new NSMutableArray();
		Enumeration enu = courses.objectEnumerator();
		while (enu.hasMoreElements()) {
			NSDictionary crs = (NSDictionary) enu.nextElement();
			NSArray cl = (NSArray)crs.valueForKey("lessonsList");
			if(cl != null && cl.count() > 0)
				result.addObjectsFromArray(cl);
		}
		return result;
	}

	public void setPeriod(Period period) {
		this.period = period;
	}

	public void setSince(NSTimestamp since) {
		this.since = since;
	}

	public void setTo(NSTimestamp to) {
		this.to = to;
	}
	
	public boolean showGroupReports() {
		return (currClass != null && currStudent == null);
	}
	
	public WOActionResults groupReport() {
		String name = (String)reporterItem.valueForKey("component");
		WOComponent page = pageWithName(name);
		page.takeValueForKey(currClass,"eduGroup");
		return page;
	}
}
