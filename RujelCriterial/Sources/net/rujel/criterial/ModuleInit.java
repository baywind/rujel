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
import net.rujel.reusables.WOLogLevel;

import com.webobjects.eoaccess.EOJoin;
import com.webobjects.eoaccess.EORelationship;
import com.webobjects.eoaccess.EOUtilities;
import com.webobjects.eocontrol.*;
import com.webobjects.foundation.*;
import com.webobjects.appserver.WOApplication;
import com.webobjects.appserver.WOContext;

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
			init();
//		} else if("init2".equals(obj)) {
//			Work.initTypes();
		} else if("presentTabs".equals(obj)) {
			NSDictionary worksTab = (NSDictionary)WOApplication.application().
				valueForKeyPath("strings.RujelCriterial_Strings.worksTab");
			return worksTab.mutableClone();
		} else if ("extendLesson".equals(obj)) {
			return extendLesson(ctx);
		} else if("reportForStudent".equals(obj)) {
			NSDictionary settings = (NSDictionary)ctx.session().objectForKey("reportForStudent");
			return StudentMarks.reportForStudent(settings);
		} else if("reportSettingsForStudent".equals(obj)) {
			NSDictionary reportSettings = (NSDictionary)WOApplication.application().
				valueForKeyPath("strings.RujelCriterial_Strings.reportSettings");
			return PlistReader.cloneDictionary(reportSettings, true);
		} else if ("lessonProperies".equals(obj)) {
			return lessonProperies(ctx);
		} else if ("diary".equals(obj)) {
			NSArray diaryTabs = (NSArray)WOApplication.application().
					valueForKeyPath("strings.RujelCriterial_Strings.diaryTabs");
			return PlistReader.cloneArray(diaryTabs, true);
		} else if ("courseComplete".equals(obj)) {
			return WOApplication.application().
					valueForKeyPath("strings.RujelCriterial_Strings.courseComplete");
		} else if("deleteCourse".equals(obj)) {
			return deleteCourse(ctx);
		} else if("adminModules".equals(obj)) {
			return WOApplication.application().valueForKeyPath(
					"strings.RujelCriterial_Strings.setup.WorkType");
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
		EOQualifier qual = new EOKeyValueQualifier("course",EOQualifier.QualifierOperatorEqual,lesson.course());
		NSMutableArray quals = new NSMutableArray(qual);
		NSTimestamp date = lesson.date();
		qual = new EOKeyValueQualifier("date",EOQualifier.QualifierOperatorGreaterThanOrEqualTo,date);
		quals.addObject(qual);
		qual = new EOKeyValueQualifier("announce",EOQualifier.QualifierOperatorLessThanOrEqualTo,date);
		quals.addObject(qual);
		qual = new EOAndQualifier(quals);
		EOFetchSpecification fs = new EOFetchSpecification("Work",qual,null);
		NSArray related = ec.objectsWithFetchSpecification(fs);
		if(related != null && related.count() > 1) {
			EOSortOrdering so = new EOSortOrdering("workType.sort",EOSortOrdering.CompareAscending);
			NSMutableArray sorter = new NSMutableArray(so);
			so = new EOSortOrdering("announce",EOSortOrdering.CompareDescending);
			sorter.addObject(so);
			so = new EOSortOrdering("date",EOSortOrdering.CompareAscending);
			sorter.addObject(so);
			related = EOSortOrdering.sortedArrayUsingKeyOrderArray(related, sorter);
		}
		NSMutableDictionary result = new NSMutableDictionary("05", "sort");
		result.takeValueForKey(related,"works");
		result.takeValueForKey("WorksOnDate", "component");
		return result;
	}
	
	public static NSDictionary lessonProperies(WOContext ctx) {
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
		NSMutableArray quals = new NSMutableArray(NSKeyValueCoding.NullValue);
		quals.addObject(qual);
		qual = new EOKeyValueQualifier(
				"weight",EOQualifier.QualifierOperatorGreaterThan,BigDecimal.ZERO);
		quals.addObject(qual);
		EOFetchSpecification fs = new EOFetchSpecification("Work",qual,null);
		NSDictionary props = new NSDictionary("font-weight:bold;","style");
		Enumeration enu = lessonsList.objectEnumerator();
		while (enu.hasMoreElements()) {
			lesson = (EduLesson) enu.nextElement();
			qual = new EOKeyValueQualifier("date",EOQualifier.QualifierOperatorEqual,lesson.date());
			quals.replaceObjectAtIndex(qual, 0);
			qual = new EOAndQualifier(quals);
			fs.setQualifier(qual);
			NSArray w = ec.objectsWithFetchSpecification(fs);
			if(w != null && w.count() > 0)
				result.setObjectForKey(props, lesson);
		}
		if(result.count() == 0)
			return null;
		return result;
	}
	
/*	public NSDictionary diaryOnPeriod(WOContext ctx) {
		WORequest req = ctx.request();
		String tmp = req.stringFormValueForKey("to");
		NSTimestamp to = (tmp == null)?null:(NSTimestamp)MyUtility.dateFormat().parseObject(
				tmp, new java.text.ParsePosition(0));
		if(to==null) {
			tmp = req.stringFormValueForKey("date");
			to = (tmp == null)?null:(NSTimestamp)MyUtility.dateFormat().parseObject(
					tmp, new java.text.ParsePosition(0));
		}
		if(to == null) {
			to = new NSTimestamp();
		}
		tmp = req.stringFormValueForKey("since");
		NSTimestamp since = (tmp == null)?null:(NSTimestamp)MyUtility.dateFormat().parseObject(
				tmp, new java.text.ParsePosition(0));
		if(since == null) {
			Calendar cal = Calendar.getInstance();
			cal.setTime(to);
			cal.set(Calendar.HOUR,0);
			cal.add(Calendar.WEEK_OF_YEAR, -1);
			since = new NSTimestamp(cal.getTimeInMillis());
		}
		
		EOSortOrdering so = new EOSortOrdering("course.cycle",EOSortOrdering.CompareAscending);
		NSMutableArray sorter = new NSMutableArray(so); 
		so = new EOSortOrdering(Work.DATE_KEY,EOSortOrdering.CompareAscending);
		sorter.addObject(so);
		
		NSArray found = diary(req, since, to, sorter);
		NSMutableDictionary result = new NSMutableDictionary(new Integer(50),"sort");
		result.takeValueForKey(found, "results");
		return result;
	}

	public NSDictionary diaryOnDay(WOContext ctx) {
		WORequest req = ctx.request();
		String tmp = req.stringFormValueForKey("date");
		NSTimestamp date = (tmp == null)?null:(NSTimestamp)MyUtility.dateFormat().parseObject(
				tmp, new java.text.ParsePosition(0));
		if(date == null)
			date = new NSTimestamp(); 
		EOSortOrdering so = new EOSortOrdering(Work.DATE_KEY,EOSortOrdering.CompareAscending);
		NSArray sorter = new NSArray(so);
		NSArray found = diary(req,date,date,sorter);
		NSMutableDictionary result = new NSMutableDictionary(new Integer(50),"sort");
		result.takeValueForKey(found, "results");
		return result;
		
	}
	
	public NSArray diary (WOContext ctx) {
		WORequest req = ctx.request();
		NSTimestamp to = null;
		String tmp = req.stringFormValueForKey("date");
		NSTimestamp date = (tmp == null)?null:(NSTimestamp)MyUtility.dateFormat().parseObject(
				tmp, new java.text.ParsePosition(0));
		if(date == null)
			date = new NSTimestamp();
		else
			to = date;
		EOEditingContext ec = (EOEditingContext)WOApplication.application().valueForKeyPath(
				"ecForYear." + MyUtility.eduYearForDate(date));
		NSArray courses = null;
		tmp = req.stringFormValueForKey("courses");
		if(tmp != null) {
			String[] cids = tmp.split(";");
			EduCourse[] crs = new EduCourse[cids.length];
			for (int i = 0; i < cids.length; i++) {
				try {
					Integer cid = new Integer(cids[i]);
					crs[i] = (EduCourse) EOUtilities.objectWithPrimaryKeyValue(
							ec, EduCourse.entityName, cid);
				} catch (Exception e) {
					//TODO: log failure;
				}
			}
			courses = new NSArray(crs);
		}
		if(courses == null) {
			try {
				Number classID = req.numericFormValueForKey("eduGroup",
						new NSNumberFormatter("#"));
				EduGroup eduGroup = (EduGroup) EOUtilities
						.objectWithPrimaryKeyValue(ec, EduGroup.entityName,
								classID);
				courses = EOUtilities.objectsMatchingKeyAndValue(ec,
						EduCourse.entityName, "eduGroup", eduGroup);
			} catch (Exception e) {
				// TODO: handle exception
			}
		}
		NSArray diaryTabs = (NSArray)WOApplication.application().
				valueForKeyPath("strings.RujelCriterial_Strings.diaryTabs");
		NSArray result = PlistReader.cloneArray(diaryTabs, true);
		String regime = req.stringFormValueForKey("regime");
		if(courses == null && regime == null)
			return result;
		Enumeration enu = result.objectEnumerator();
		NSMutableDictionary tab = null;
		while (enu.hasMoreElements()) {
			tab = (NSMutableDictionary) enu.nextElement();
			if(regime.equals(tab.valueForKey("id")))
				break;
			else
				tab = null;
		}
		if(tab == null)
			return result;

		tab.takeValueForKey("true", "selected");
		NSTimestamp since = null;
		NSArray sorter = null;
		
		if(Various.boolForObject(tab.valueForKey("period"))) {
			tmp = req.stringFormValueForKey("since");
			since = (tmp == null)?null:(NSTimestamp)MyUtility.dateFormat().parseObject(
					tmp, new java.text.ParsePosition(0));
			if(since == null) {
				Calendar cal = Calendar.getInstance();
				cal.setTime(date);
				cal.set(Calendar.HOUR,0);
				cal.add(Calendar.WEEK_OF_YEAR, -1);
				since = new NSTimestamp(cal.getTimeInMillis());
			}
		} else { // if tab is for period
			since = date;
			EOSortOrdering so = new EOSortOrdering(Work.DATE_KEY,EOSortOrdering.CompareAscending);
			sorter = new NSArray(so);
		} // if tab is for day

		EOQualifier qual = Various.getEOInQualifier("course", courses);
		NSMutableArray quals = new NSMutableArray(qual);
		if(to != null) {
			qual = new EOKeyValueQualifier(Work.ANNOUNCE_KEY,
					EOQualifier.QualifierOperatorLessThanOrEqualTo,to);
			quals.addObject(qual);
		}
		qual = new EOKeyValueQualifier(Work.DATE_KEY,
				EOQualifier.QualifierOperatorGreaterThanOrEqualTo,since);
		quals.addObject(qual);
		qual = new EOKeyComparisonQualifier(Work.ANNOUNCE_KEY,
				EOQualifier.QualifierOperatorNotEqual,Work.DATE_KEY);
		quals.addObject(qual);
		qual = new EOAndQualifier(quals);
		EOFetchSpecification fs = new EOFetchSpecification(Work.ENTITY_NAME,qual,sorter);
		fs.setRefreshesRefetchedObjects(true);
		NSArray found =  ec.objectsWithFetchSpecification(fs);
		//return found;
		tab.takeValueForKey(found, "list");
		return result;
	}
	*/

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
}
