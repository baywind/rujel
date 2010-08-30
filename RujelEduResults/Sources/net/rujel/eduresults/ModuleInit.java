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

package net.rujel.eduresults;

import net.rujel.reusables.*;
import net.rujel.base.MyUtility;
import net.rujel.interfaces.EduCourse;

import com.webobjects.foundation.*;
import com.webobjects.eocontrol.*;
import com.webobjects.appserver.WOApplication;
import com.webobjects.appserver.WOContext;

import java.lang.reflect.Method;
import java.util.Enumeration;
import java.util.logging.Logger;

public class ModuleInit {
	
	public static Object init(Object obj, WOContext ctx) {
		if(obj == null || obj.equals("init")) {
			try {
				Object access = PlistReader.readPlist("access.plist",
						"RujelEduResults", null);
				WOApplication.application().takeValueForKey(access, "defaultAccess");
			} catch (NSKeyValueCoding.UnknownKeyException e) {
				// default access not supported
			}
			ItogMark.init();
			ItogType.init();
			EOSortOrdering.ComparisonSupport.setSupportForClass(
					new ItogContainer.ComparisonSupport(), Period.class);
			EOSortOrdering.ComparisonSupport.setSupportForClass(
					new ItogContainer.ComparisonSupport(), ItogContainer.class);
		} else if("notesAddOns".equals(obj)) {
			return notesAddOns(ctx);
		} else if("studentReporter".equals(obj)) {
			return studentReporter(ctx);
		} else if("statCourseReport".equals(obj)) {
			return statCourseReport(ctx);
		} else if("planTabs".equals(obj)) {
			return WOApplication.application().valueForKeyPath(
				"strings.RujelEduResults_EduResults.planTab");
		} else if("completionLock".equals(obj)) {
			return new NSDictionary(
					new String[] {ItogMark.ENTITY_NAME,"student","cycle","cycle"},
					new String[] {"entity","studentPath","checkPath","checkCourse"});
		} else if("deleteStudents".equals(obj)) {
			return deleteStudents(ctx);
		}
		return null;
	}
	
	public static NSMutableDictionary notesAddOns(WOContext ctx) {
		NamedFlags access = (NamedFlags)ctx.session().valueForKeyPath("readAccess.FLAGS.ItogMark");
		if(access.getFlag(0)) {
			NSMutableDictionary itogAddOn = ((NSDictionary)WOApplication.application().
					valueForKeyPath("strings.RujelEduResults_EduResults.itogAddOn")).mutableClone();
			itogAddOn.takeValueForKey(access,"access");
			return itogAddOn;
		}
		return null;
	}
	
	public static Object studentReporter(WOContext ctx) {
		NamedFlags access = (NamedFlags)ctx.session().valueForKeyPath("readAccess.FLAGS.ItogMark");
		//NamedFlags access = moduleAccess(ctx,"ItogMark");
		if(!access.getFlag(0))
				return null;
		Object result = WOApplication.application().valueForKeyPath(
				"strings.RujelEduResults_EduResults.studentReporter");
//		result = result.mutableClone();
//		result.takeValueForKey(access,"access");
		return result;
	}

	public static NSDictionary statCourse(EduCourse course, ItogContainer container) {
		NSArray itogs = ItogMark.getItogMarks(course.cycle(), container, null);
//		itogs = MyUtility.filterByGroup(itogs, "student", course.groupList(), true);
		if(itogs == null || itogs.count() == 0)
			return NSDictionary.EmptyDictionary;
		if(itogs.count() > 1) {
			EOSortOrdering so = new EOSortOrdering(ItogMark.MARK_KEY,
					EOSortOrdering.CompareCaseInsensitiveAscending);
			itogs = EOSortOrdering.sortedArrayUsingKeyOrderArray(itogs, new NSArray(so));
		}
		NSMutableArray keys = new NSMutableArray();
		NSArray group = course.groupList();
		NSMutableDictionary result = new NSMutableDictionary(keys, "keys");
		int total = group.count();
		result.setObjectForKey(new Integer(total), "total");
		Enumeration enu = itogs.objectEnumerator();
		String currKey = null;
		int currCount = 0;
		while (enu.hasMoreElements()) {
			ItogMark itog = (ItogMark) enu.nextElement();
			if(!group.containsObject(itog.student()))
				continue;
			if((currKey==null)?itog.mark()==null:currKey.equalsIgnoreCase(itog.mark())) {
				currCount++;
			} else {
				if(currCount > 0)
					result.setObjectForKey(new Integer(currCount), (currKey==null)?" ":currKey);
				currKey = itog.mark();
				keys.addObject((currKey==null)?" ":currKey);
				currCount = 1;
			}
			total--;
		}
		if(currCount > 0)
			result.setObjectForKey(new Integer(currCount), currKey);
		if(total > 0) {
			result.setObjectForKey(new Integer(total), "");
			keys.addObject("");
		}
		return result;
	}
	
	public static EOEnterpriseObject getStatsGrouping (EduCourse course, ItogContainer period) {
		EOEnterpriseObject grouping = null;
		try {
			Class descClass = Class.forName("net.rujel.stats.Description");
			Method method = descClass.getMethod("getGrouping", String.class, String.class,
					EOEnterpriseObject.class, EOEnterpriseObject.class, Boolean.TYPE);
			grouping = (EOEnterpriseObject)method.invoke(null, ItogMark.ENTITY_NAME, 
					ItogMark.MARK_KEY, course, period, Boolean.TRUE);
			if(grouping.valueForKeyPath("description.description") == null) {
				String prName = (String)WOApplication.application().valueForKeyPath(
					"strings.RujelEduResults_EduResults.properties.ItogMark.this");
				grouping.takeValueForKeyPath(prName,"description.description");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return grouping;
	}
	
	public static EOEnterpriseObject prepareStats(EduCourse course,
			ItogContainer period, boolean save) {
		EOEnterpriseObject grouping = getStatsGrouping(course, period);
		if(grouping != null) {
			NSArray itogs = ItogMark.getItogMarks(course.cycle(), period, null);
			itogs = MyUtility.filterByGroup(itogs, "student", course.groupList(), true);
			grouping.takeValueForKey(itogs, "array");
//			NSDictionary stats = ModuleInit.statCourse(course, period);
//			grouping.takeValueForKey(stats, "dict");
			if (save) {
				EOEditingContext ec = grouping.editingContext();
				try {
					ec.saveChanges();
				} catch (Exception e) {
					Logger.getLogger("rujel.eduresults").log(WOLogLevel.WARNING,
							"Failed to save itog Stats for course", new Object[] {course,e});
					ec.revert();
				}
			}
		}
		return grouping;
	}

	
	public static NSArray marksPreset() {
		return ((NSArray)WOApplication.application().valueForKeyPath(
				"strings.RujelEduResults_EduResults.marksPreset")).immutableClone();
	}
	
	public static Object statCourseReport(WOContext ctx) {
		if(Various.boolForObject(ctx.session().valueForKeyPath("readAccess._read.ItogMark")))
			return null;
		EOEditingContext ec = null;
		EduCourse course = (EduCourse)ctx.session().objectForKey("statCourseReport");
		NSArray list = null;
		if(course != null) {
			ec = course.editingContext();
			list = ItogContainer.itogsForCourse(course);
		} else {
			try {
				ec = (EOEditingContext)ctx.page().valueForKey("ec");
			} catch (Exception e) {
				ec = new SessionedEditingContext(ctx.session());
			}
//			ec.lock();
			Integer year = (Integer)ctx.session().valueForKey("eduYear");
			EOQualifier qual = new EOKeyValueQualifier(ItogContainer.EDU_YEAR_KEY,
					EOQualifier.QualifierOperatorEqual,year);
			EOFetchSpecification fs = new EOFetchSpecification(ItogContainer.ENTITY_NAME,
					qual,ItogContainer.sorter);
			list = ec.objectsWithFetchSpecification(fs);
		}
		if(list == null || list.count() == 0) {
//			ec.unlock();
			return null;
		}
		NSMutableArray result = new NSMutableArray();
		
		Enumeration enu = list.objectEnumerator();
		NSMutableDictionary template = new NSMutableDictionary(ItogMark.ENTITY_NAME,"entName");
		template.setObjectForKey(ItogMark.MARK_KEY, "statField");
		template.setObjectForKey(marksPreset(),"keys");
		
		try {
			Method method = ModuleInit.class.getMethod("statCourse",
					EduCourse.class, ItogContainer.class);
			template.setObjectForKey(method,"ifEmpty");
		} catch (Exception e) {
			e.printStackTrace();
		}

		String title = (String)WOApplication.application().valueForKeyPath(
				"strings.RujelEduResults_EduResults.itogAddOn.title");
		int sort = 30;
		EOFetchSpecification fs = new EOFetchSpecification(ItogMark.ENTITY_NAME,null,null);
		fs.setFetchLimit(1);
		while (enu.hasMoreElements()) {
			ItogContainer itog = (ItogContainer) enu.nextElement();
			EOQualifier qual = new EOKeyValueQualifier(ItogMark.CONTAINER_KEY,
					EOQualifier.QualifierOperatorEqual,itog);
			fs.setQualifier(qual);
			list = ec.objectsWithFetchSpecification(fs);
			if(list == null || list.count() == 0)
				continue;
			NSMutableDictionary dict = template.mutableClone();
			dict.setObjectForKey(itog.title(),"title");
			dict.setObjectForKey(title,"description");
			dict.setObjectForKey(itog,"param2");
			dict.setObjectForKey(String.valueOf(sort),"sort");
			dict.setObjectForKey(Boolean.TRUE, "addCalculations");
			result.addObject(dict);
			sort++;
		}
//		ec.unlock();
		return result;
	}
	
	public static Object deleteStudents(WOContext ctx) {
		NSArray students = (NSArray)ctx.session().objectForKey("deleteStudents");
		if(students == null || students.count() == 0)
			return null;
		EOQualifier qual = Various.getEOInQualifier("student", students);
		EOFetchSpecification fs = new EOFetchSpecification("ItogMark",qual,null);
		fs.setFetchLimit(1);
		EOEnterpriseObject student = (EOEnterpriseObject)students.objectAtIndex(0);
		NSArray found = student.editingContext().objectsWithFetchSpecification(fs);
		if(found != null && found.count() > 0) {
			Object result = ctx.session().valueForKeyPath(
					"strings.RujelEduResults_EduResults.relatedItogsFound");
			if(result == null)
				result = "Related itogs found";
			return result;
		}
		return null;
	}
}
