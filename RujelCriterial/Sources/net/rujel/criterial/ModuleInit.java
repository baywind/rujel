// ModuleInit.java

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

package net.rujel.criterial;

import java.math.BigDecimal;
import java.util.Enumeration;
import java.util.logging.Logger;

import net.rujel.base.BaseLesson;
import net.rujel.interfaces.EOInitialiser;
import net.rujel.interfaces.EduCourse;
import net.rujel.interfaces.EduLesson;
import net.rujel.reusables.PlistReader;
import net.rujel.reusables.Various;
import net.rujel.reusables.WOLogLevel;

import com.webobjects.eoaccess.EOJoin;
import com.webobjects.eoaccess.EORelationship;
import com.webobjects.eoaccess.EOUtilities;
import com.webobjects.eocontrol.*;
import com.webobjects.foundation.*;
import com.webobjects.appserver.WOApplication;
import com.webobjects.appserver.WOContext;
import com.webobjects.appserver.WOSession;

public class ModuleInit {

/*	public static Object init(Object obj) {
		if("presentTabs".equals(obj)) {
			return init(obj, null);
		}
		return null;
	}
*/	
	public static Object init(Object obj, WOContext ctx) {
		if(obj == null || obj.equals("init")) {
			try {
				Object access = PlistReader.readPlist("access.plist", "RujelCriterial", null);
				WOApplication.application().takeValueForKey(access, "defaultAccess");
			} catch (NSKeyValueCoding.UnknownKeyException e) {
				// default access not supported
			}
			init();
//		} else if("init2".equals(obj)) {
//			Work.initTypes();
		} else if("presentTabs".equals(obj)) {
			NSDictionary worksTab = (NSDictionary)WOApplication.application().
				valueForKeyPath("strings.RujelCriterial_Strings.worksTab");
			return PlistReader.cloneDictionary(worksTab, true);
		} else if ("extendLesson".equals(obj)) {
			return extendLesson(ctx);
		} else if("reportForStudent".equals(obj)) {
			NSDictionary settings = (NSDictionary)ctx.session().objectForKey("reportForStudent");
			return StudentMarks.reportForStudent(settings);
		} else if("reportSettingsForStudent".equals(obj)) {
			NSDictionary reportSettings = (NSDictionary)WOApplication.application().
				valueForKeyPath("strings.RujelCriterial_Strings.reportSettings");
			return PlistReader.cloneDictionary(reportSettings, true);
		} else if ("lessonProperties".equals(obj)) {
			return lessonProperties(ctx);
		} else if ("diary".equals(obj)) {
			NSArray diaryTabs = (NSArray)WOApplication.application().
					valueForKeyPath("strings.RujelCriterial_Strings.diaryTabs");
			return PlistReader.cloneArray(diaryTabs, true);
		} else if ("courseComplete".equals(obj)) {
			return WOApplication.application().
					valueForKeyPath("strings.RujelCriterial_Strings.courseComplete");
		} else if("deleteCourse".equals(obj)) {
			return deleteCourse(ctx);
		} else if("completionLock".equals(obj)) {
			return new NSArray(new NSDictionary[] {
					new NSDictionary(new String[] {"WorkNote","work.course","student"},
							new String[] {"entity","coursePath","studentPath"}),
					new NSDictionary(new String[] {"Mark","work.course","student"},
							new String[] {"entity","coursePath","studentPath"}) });
		} else if("adminModules".equals(obj)) {
			return adminModules(ctx);
		} else if("deleteStudents".equals(obj)) {
			return deleteStudents(ctx);
		} else if("xmlGeneration".equals(obj)) {
			return xmlGeneration(ctx);
		} else if("archiveType".equals(obj)) {
			return ctx.session().valueForKeyPath("strings.RujelCriterial_Strings.archiveType");
		}
		return null;
	}
	
	public static void init() {
		EOInitialiser.initialiseRelationship("WorkNote","student",false,"studentID","Student").
						anyInverseRelationship().setPropagatesPrimaryKey(true);

		EORelationship relationship = EOInitialiser.initialiseRelationship("Work","course",false,
				"courseID","EduCourse");
		if(EduLesson.entityName.equals("Work")) {
			EORelationship backrel = relationship.destinationEntity().relationshipNamed("lessons");
			EOJoin join = (EOJoin)backrel.joins().objectAtIndex(0);
			backrel.removeJoin(join);
			join = (EOJoin)relationship.joins().objectAtIndex(0);
			join = new EOJoin(join.destinationAttribute(),join.sourceAttribute());
			backrel.addJoin(join);
		} else if (EduLesson.entityName.equals("BaseLesson")) {
			BaseLesson.setTaskDelegate(new HomeWorkDelegate());
		}

		Mark.init();
	}
	
	public static NSKeyValueCoding extendLesson(WOContext ctx) {
		EduLesson lesson = (EduLesson)ctx.session().objectForKey("currentLesson");
		EOEditingContext ec = lesson.editingContext();
		if(ec == null) return null;
		EOQualifier qual = new EOKeyValueQualifier("course",EOQualifier.QualifierOperatorEqual,lesson.course());
		NSMutableArray quals = new NSMutableArray(qual);
		NSTimestamp date = lesson.date();
		qual = new EOKeyValueQualifier("date",EOQualifier.QualifierOperatorGreaterThanOrEqualTo,date);
		quals.addObject(qual);
		qual = new EOKeyValueQualifier("announce",EOQualifier.QualifierOperatorLessThanOrEqualTo,date);
		quals.addObject(qual);
		qual = new EOAndQualifier(quals);
		EOFetchSpecification fs = new EOFetchSpecification("Work",qual,EduLesson.sorter);
		NSArray related = ec.objectsWithFetchSpecification(fs);
	/*	if(related != null && related.count() > 1) {
			EOSortOrdering so = new EOSortOrdering("workType.sort",EOSortOrdering.CompareAscending);
			NSMutableArray sorter = new NSMutableArray(so);
			so = new EOSortOrdering("announce",EOSortOrdering.CompareDescending);
			sorter.addObject(so);
			so = new EOSortOrdering("date",EOSortOrdering.CompareAscending);
			sorter.addObject(so);
			related = EOSortOrdering.sortedArrayUsingKeyOrderArray(related, sorter);
		}*/
		NSMutableDictionary result = new NSMutableDictionary("05", "sort");
		result.takeValueForKey(related,"works");
		result.takeValueForKey("WorksOnDate", "component");
		return result;
	}
	
	public static NSDictionary lessonProperties(WOContext ctx) {
		NSArray lessonsList = (NSArray)ctx.session().objectForKey("lessonsList");
		if(lessonsList == null || lessonsList.count() == 0)
			return null;
		NSMutableDictionary result = new NSMutableDictionary();
		EduLesson lesson = (EduLesson)lessonsList.objectAtIndex(0);
		EOEditingContext ec = lesson.editingContext();
		if(ec == null) {
			Logger.getLogger("rujel.criterial").log(WOLogLevel.WARNING,
					"Lesson not in EditingContext",ctx.session());
			return null;
		}
		EOQualifier qual = new EOKeyValueQualifier(
				"course",EOQualifier.QualifierOperatorEqual,lesson.course());
		NSMutableArray quals = new NSMutableArray();
		quals.addObject(qual);
		qual = new EOKeyValueQualifier(
				"weight",EOQualifier.QualifierOperatorGreaterThan,BigDecimal.ZERO);
		quals.addObject(qual);
		qual = new EOKeyValueQualifier("date",EOQualifier.QualifierOperatorLessThanOrEqualTo
				,lessonsList.valueForKey("@max.date"));
		quals.addObject(qual);
		qual = new EOKeyValueQualifier("date",EOQualifier.QualifierOperatorGreaterThanOrEqualTo
				,lessonsList.valueForKey("@min.date"));
		quals.addObject(qual);
		qual = new EOAndQualifier(quals);
		EOFetchSpecification fs = new EOFetchSpecification(Work.ENTITY_NAME,qual,null);
		NSArray works = ec.objectsWithFetchSpecification(fs);
		NSDictionary props = new NSDictionary("font-weight:bold;","style");
		Enumeration enu = lessonsList.objectEnumerator();
		while (enu.hasMoreElements()) {
			lesson = (EduLesson) enu.nextElement();
			qual = new EOKeyValueQualifier("date",EOQualifier.QualifierOperatorEqual,lesson.date());
			NSArray w = EOQualifier.filteredArrayWithQualifier(works, qual);
			if(w != null && w.count() > 0)
				result.setObjectForKey(props, lesson);
		}
		if(result.count() == 0)
			return null;
		return result;
	}

	public static Object deleteCourse(WOContext ctx) {
		EduCourse course = (EduCourse)ctx.session().objectForKey("deleteCourse");
		EOEditingContext ec = course.editingContext();
		NSArray list = EOUtilities.objectsMatchingKeyAndValue
					(ec, Work.ENTITY_NAME, "course", course);
		if(list == null || list.count() == 0)
			return null;
		String message = (String)WOApplication.application().valueForKeyPath(
				"strings.RujelCriterial_Strings.messages.courseHasWorks");
		ctx.session().takeValueForKey(message, "message");
		return message;
	}
	
	public static Object adminModules(WOContext ctx) {
		WOSession ses = ctx.session();
		NSDictionary setup = (NSDictionary)ses.valueForKeyPath(
			"strings.RujelCriterial_Strings.setup");
		NSMutableArray result = new NSMutableArray();
		Enumeration enu = setup.keyEnumerator();
		while (enu.hasMoreElements()) {
			String key = (String) enu.nextElement();
			if(Various.boolForObject(ses.valueForKeyPath("readAccess.read." + key)))
				result.addObject(setup.valueForKey(key));
		}
		return result;
	}
	
	public static Object deleteStudents(WOContext ctx) {
		NSArray students = (NSArray)ctx.session().objectForKey("deleteStudents");
		if(students == null || students.count() == 0)
			return null;
		EOQualifier qual = Various.getEOInQualifier("student", students);
		EOFetchSpecification fs = new EOFetchSpecification("Mark",qual,null);
		fs.setFetchLimit(1);
		EOEnterpriseObject student = (EOEnterpriseObject)students.objectAtIndex(0);
		NSArray found = student.editingContext().objectsWithFetchSpecification(fs);
		if(found != null && found.count() > 0) {
			return ctx.session().valueForKeyPath(
					"strings.RujelCriterial_Strings.messages.relatedMarksFound");
		}
		return null;
	}
	
	public static Object xmlGeneration(WOContext ctx) {
		NSDictionary options = (NSDictionary)ctx.session().objectForKey("xmlGeneration");
		{
			NSDictionary settings = (NSDictionary)options.valueForKeyPath("reporter.settings");
		if(settings != null && !Various.boolForObject(settings.valueForKeyPath("marks.active")))
			return null;
		}
		return new CriterialXML(options);
	}
}
