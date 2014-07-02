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
import net.rujel.ui.AddOnPresenter;
import net.rujel.base.MyUtility;
import net.rujel.base.SettingsBase;
import net.rujel.interfaces.EduCourse;

import com.webobjects.foundation.*;
import com.webobjects.eoaccess.EOUtilities;
import com.webobjects.eocontrol.*;
import com.webobjects.appserver.WOApplication;
import com.webobjects.appserver.WOContext;
import com.webobjects.appserver.WOSession;

import java.io.InputStream;
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
			if(Various.boolForObject(ctx.session().valueForKeyPath("readAccess.read.SetupItogs")))
				return WOApplication.application().valueForKeyPath(
					"strings.RujelEduResults_EduResults.planTab");
			return null;
		} else if("groupComplete".equals(obj)) {
//			return null; 
			return ctx.session().valueForKeyPath("strings.RujelEduResults_EduResults.groupItogs");
		} else if("completionLock".equals(obj)) {
			return new NSDictionary(
					new String[] {ItogMark.ENTITY_NAME,"student","cycle","cycle"},
					new String[] {"entity","studentPath","checkPath","checkCourse"});
		} else if("deleteStudents".equals(obj)) {
			return deleteStudents(ctx);
		} else if("archiveType".equals(obj)) {
			return ctx.session().valueForKeyPath("strings.RujelEduResults_EduResults.archiveType");
		} else if("journalPlugins".equals(obj)) {
			return ctx.session().valueForKeyPath("strings.RujelEduResults_EduResults.groupTabel");
		} else if("usedModels".equals(obj)) {
			return "EduResults";
		} else if("initialData".equals(obj)) {
			return initialData(ctx);
		}
		return null;
	}
	
	public static NSKeyValueCoding notesAddOns(WOContext ctx) {
		NamedFlags access = (NamedFlags)ctx.session().valueForKeyPath("readAccess.FLAGS.ItogMark");
		if(access.getFlag(0)) {
			NSDictionary itogAddOn = (NSDictionary)WOApplication.application().
					valueForKeyPath("strings.RujelEduResults_EduResults.itogAddOn");
			itogAddOn.takeValueForKey(access,"access");
			return new AddOnPresenter.AddOn(itogAddOn, access);
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
			String listName = sectionListName(ctx.session(), ec);
			Integer year = (Integer)ctx.session().valueForKey("eduYear");
			list = ItogType.itogsForTypeList(ItogType.typesForList(listName, year, ec), year);
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
			Logger.getLogger("rujel.itog").log(WOLogLevel.WARNING,
					"Error getting statCourse method",e);
		}

		template.takeValueForKey(WOApplication.application().valueForKeyPath(
				"strings.RujelEduResults_EduResults.itogAddOn.title"), "description");
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
			dict.setObjectForKey(itog,"param2");
			dict.setObjectForKey(String.valueOf(sort),"sort");
			dict.setObjectForKey(Boolean.TRUE, "addCalculations");
			result.addObject(dict);
			sort++;
		}
//		ec.unlock();
		return result;
	}
	
	public static String sectionListName(WOSession session, EOEditingContext ec) {
		NSMutableDictionary courseDict = null;
		Integer section = (Integer)session.valueForKeyPath("state.section.idx");
		if(section != null) {
			NSDictionary sectDict = new NSDictionary(section,"section");
			courseDict = new NSMutableDictionary(3);
			courseDict.takeValueForKey(session.valueForKeyPath("eduYear"),"eduYear");
			courseDict.takeValueForKey(sectDict, "eduGroup");
			courseDict.takeValueForKey(sectDict, "cycle");
		}
		String listName = SettingsBase.stringSettingForCourse(
				ItogMark.ENTITY_NAME, courseDict, ec);
		return listName;
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

	public static Object initialData(WOContext ctx) {
		EOEditingContext prevEc = (EOEditingContext)ctx.userInfoForKey("prevEc");
		EOEditingContext ec = (EOEditingContext)ctx.userInfoForKey("ec");
		Logger logger = Logger.getLogger("rujel.eduresults");
		if(prevEc == null) { // load predefined data
			try {
				EOObjectStoreCoordinator os = EOObjectStoreCoordinator.defaultCoordinator();
				InputStream data = WOApplication.application().resourceManager().
				inputStreamForResourceNamed("dataEduResults.sql", "RujelEduResults", null);
				DataBaseUtility.executeScript(os, "EduResults", data);
				logger.log(WOLogLevel.INFO, "Loaded inital EduResults data");
			} catch (Exception e) {
				ec.revert();
				logger.log(WOLogLevel.WARNING,"Failed to load inital EduResults data  ",e);
			}
		} else { // copy ItogTypeList from previous year
			Integer eduYear = (Integer)ctx.userInfoForKey("eduYear");
			NSArray lists = EOUtilities.objectsMatchingKeyAndValue(ec,
					"ItogTypeList", "eduYear", eduYear);
			if(lists != null && lists.count() > 0)
				return null;
			Integer prevYear = (Integer)ctx.userInfoForKey("prevYear");
			SettingsBase base = SettingsBase.baseForKey(ItogMark.ENTITY_NAME, prevEc, true);
			lists = base.availableValues(null, SettingsBase.TEXT_VALUE_KEY);
			Enumeration enu = lists.objectEnumerator();
			NSMutableArray types = new NSMutableArray();
			while (enu.hasMoreElements()) {
				String listName = (String) enu.nextElement();
				try {
					NSArray typeList = ItogType.getTypeList(listName, prevYear, prevEc);
					if(typeList == null || typeList.count() == 0)
						continue;
					Enumeration tenu = typeList.objectEnumerator();
					while (tenu.hasMoreElements()) {
						EOEnterpriseObject tl = (EOEnterpriseObject) tenu.nextElement();
						Integer year = (Integer)tl.valueForKey("eduYear");
						if(year == null || year.intValue() == 0)
							tl.takeValueForKey(prevYear, "eduYear");
						ItogType type = (ItogType)tl.valueForKey("itogType");
						type = (ItogType)EOUtilities.localInstanceOfObject(ec, type);
						if(!types.containsObject(type)) {
							if(type.inYearCount() > 0)
								type.generateItogsInYear(eduYear);
							types.addObject(type);
						}
						EOEnterpriseObject newTL = EOUtilities.createAndInsertInstance(
								ec, "ItogTypeList");
						newTL.takeValueForKey(listName, "listName");
						newTL.takeValueForKey(eduYear, "eduYear");
						newTL.addObjectToBothSidesOfRelationshipWithKey(type, "itogType");
					} // typeList.objectEnumerator();
					ec.saveChanges();
					if(prevEc.hasChanges())
						prevEc.saveChanges();
				} catch (Exception e) {
					ec.revert();
					logger.log(WOLogLevel.WARNING,"Failed to copy ItogTypeList for listName: "
							+ listName,e);
				}
			} //lists.objectEnumerator();
		}
		logger.log(WOLogLevel.INFO, "Copied ItogTypeList from previous year");
		return null;
	}
}
