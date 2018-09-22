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
import net.rujel.base.SchoolSection;
import net.rujel.base.SettingsBase;
import net.rujel.eduplan.PlanCycle;
import net.rujel.interfaces.EduCourse;
import net.rujel.interfaces.EduCycle;
import net.rujel.interfaces.EduGroup;
import net.rujel.interfaces.Student;

import com.webobjects.foundation.*;
import com.webobjects.eoaccess.EOUtilities;
import com.webobjects.eocontrol.*;
import com.webobjects.appserver.WOApplication;
import com.webobjects.appserver.WOContext;
import com.webobjects.appserver.WOSession;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.Enumeration;
import java.util.NoSuchElementException;
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
		} else if("groupReport".equals(obj)) {
			return groupMarksReport(ctx);
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
			if(Various.boolForObject(ctx.session().valueForKeyPath("readAccess.read.ItogMark")))
				return ctx.session().valueForKeyPath(
						"strings.RujelEduResults_EduResults.groupTabel");
			else return null;
		} else if("usedModels".equals(obj)) {
			return "EduResults";
		} else if("updateCourseCycle".equals(obj)) {
			return updateCourseCycle(ctx);
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
		if(ctx != null) {
		NamedFlags access = (NamedFlags)ctx.session().valueForKeyPath("readAccess.FLAGS.ItogMark");
		//NamedFlags access = moduleAccess(ctx,"ItogMark");
		if(!access.getFlag(0))
				return null;
		}
		Object result = WOApplication.application().valueForKeyPath(
				"strings.RujelEduResults_EduResults.studentReporter");
//		result = result.mutableClone();
//		result.takeValueForKey(access,"access");
		return result;
	}

	public static NSDictionary statCourse(EduCourse course, ItogContainer container, String key) {
		NSArray itogs = ItogMark.getItogMarks(course.cycle(), container, null);
//		itogs = MyUtility.filterByGroup(itogs, "student", course.groupList(), true);
		if(itogs == null || itogs.count() == 0)
			return NSDictionary.EmptyDictionary;
		if(itogs.count() > 1) {
			EOSortOrdering so = new EOSortOrdering(
					key,EOSortOrdering.CompareCaseInsensitiveAscending);
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
			String mark = (String)itog.valueForKey(key);
			if((currKey==null)?mark==null:currKey.equalsIgnoreCase(mark)) {
				currCount++;
			} else {
				if(currCount > 0)
					result.setObjectForKey(new Integer(currCount), (currKey==null)?" ":currKey);
				currKey = mark;
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
	
	public static EOEnterpriseObject getStatsGrouping (EduCourse course, ItogContainer period,
			String key) {
		EOEnterpriseObject grouping = null;
		try {
			Class descClass = Class.forName("net.rujel.stats.Description");
			Method method = descClass.getMethod("getGrouping", String.class, String.class,
					EOEnterpriseObject.class, EOEnterpriseObject.class, Boolean.TYPE);
			grouping = (EOEnterpriseObject)method.invoke(null, ItogMark.ENTITY_NAME, 
					key, course, period, Boolean.TRUE);
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
			ItogContainer period, String key, boolean save) {
		EOEnterpriseObject grouping = getStatsGrouping(course, period, key);
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
	
	protected static Enumeration itogsEnu(final EOEditingContext ec, String listName, Integer year) {
		NSArray list = ItogType.itogsForTypeList(ItogType.typesForList(listName, year, ec), year);
		if(list == null || list.count() == 0)
			return null;
		final Enumeration enu = list.objectEnumerator();
		return new Enumeration() {
			private Object next = null;
			EOFetchSpecification fs = new EOFetchSpecification(ItogMark.ENTITY_NAME,null,null);

			public boolean hasMoreElements() {
				if(next != null)
					return true;
				while(enu.hasMoreElements()) {
					next = enu.nextElement();
					EOQualifier qual = new EOKeyValueQualifier(ItogMark.CONTAINER_KEY,
							EOQualifier.QualifierOperatorEqual,next);
					fs.setQualifier(qual);
					fs.setFetchLimit(1);
					NSArray found = ec.objectsWithFetchSpecification(fs);
					if(found != null && found.count() > 0)
						return true;
				}
				next = null;
				return false;
			}

			public Object nextElement() {
				if(hasMoreElements()) {
					Object result = next;
					next = null;
					return result;
				}
				throw new NoSuchElementException("No more elements");
			}
		};
	}
	
	public static Object statCourseReport(WOContext ctx) {
		if(Various.boolForObject(ctx.session().valueForKeyPath("readAccess._read.ItogMark")))
			return null;
		EOEditingContext ec = null;
		NSKeyValueCodingAdditions course = (NSKeyValueCodingAdditions)
				ctx.session().objectForKey("statCourseReport");
		String listName;
		if(course != null) {
			ec = (EOEditingContext)course.valueForKey("editingContext");
		}		
		if(ec == null) {
			try {
				ec = (EOEditingContext)ctx.page().valueForKey("ec");
			} catch (Exception e) {
				ec = new SessionedEditingContext(ctx.session());
			}
		}
		if(course == null) {
			listName = sectionListName(ctx.session(), ec);
		} else {
			listName = SettingsBase.stringSettingForCourse(ItogMark.ENTITY_NAME, course, ec);
		}
		Integer year = (Integer)ctx.session().valueForKey("eduYear");
		Enumeration enu = itogsEnu(ec, listName, year);
		if(enu == null)
			return null;

		NSMutableArray result = new NSMutableArray();
		
		NSMutableDictionary template = new NSMutableDictionary(ItogMark.ENTITY_NAME,"entName");
		template.setObjectForKey(ItogMark.MARK_KEY, "statField");
		try {
			Method method = ModuleInit.class.getMethod("statCourse",
					EduCourse.class, ItogContainer.class, String.class);
			template.setObjectForKey(method,"ifEmpty");
		} catch (Exception e) {
			Logger.getLogger("rujel.itog").log(WOLogLevel.WARNING,
					"Error getting statCourse method",e);
		}
		template.takeValueForKey(ctx.session().valueForKeyPath(
				"strings.RujelEduResults_EduResults.itogAddOn.title"), "description");
		template.takeValueForKey(new NSDictionary(listName,ItogMark.ENTITY_NAME),
				SettingsBase.ENTITY_NAME);
		int sort = 30;
		Integer curGrNum = null;
		NSArray presets = null;
		while (enu.hasMoreElements()) {
			ItogContainer itog = (ItogContainer) enu.nextElement();
			Integer grNum = ItogPreset.getPresetGroup(listName, year, itog.itogType());
			if(grNum != null && !grNum.equals(curGrNum)) {
				presets = ItogPreset.listPresetGroup(ec, grNum, true);
				if(presets != null) {
					if(presets.count() == 0)
						presets = null;
					else
						presets = (NSArray)presets.valueForKey(ItogPreset.MARK_KEY);
				}
			}
			String title = itog.title();
			NSMutableDictionary dict = template.mutableClone();
			dict.setObjectForKey(itog,"param2");
			if(presets != null) {
				dict.setObjectForKey(title,"title");
				dict.setObjectForKey(String.valueOf(sort + 10),"sort");
				dict.setObjectForKey(Boolean.valueOf(
						"5".equals(presets.objectAtIndex(0))), "addCalculations");
				dict.takeValueForKey(presets, "keys");				
				result.addObject(dict);
				//			sort++;
				dict = dict.mutableClone();
			}
			dict.setObjectForKey("stateKey","statField");
			dict.setObjectForKey(ItogPreset.stateSymbolsDescending, "keys");
			dict.setObjectForKey(Boolean.FALSE, "addCalculations");
			dict.setObjectForKey(String.valueOf(sort),"sort");
			dict.setObjectForKey("~ " + title + " ~","title");
			result.addObject(dict);			
			sort++;
		}
		return result;
	}
	
	public static Object groupMarksReport(WOContext ctx) {
		if(Various.boolForObject(ctx.session().valueForKeyPath("readAccess._read.ItogMark")))
			return null;
		EOEditingContext ec = null;
		NSKeyValueCodingAdditions course = (NSKeyValueCodingAdditions)
				ctx.session().objectForKey("groupReport");
		String listName;
		if(course != null) {
			ec = (EOEditingContext)course.valueForKey("editingContext");
		}		
		if(ec == null) {
			try {
				ec = (EOEditingContext)ctx.page().valueForKey("ec");
			} catch (Exception e) {
				ec = new SessionedEditingContext(ctx.session());
			}
		}
		if(course == null) {
			listName = sectionListName(ctx.session(), ec);
		} else {
			listName = SettingsBase.stringSettingForCourse(ItogMark.ENTITY_NAME, course, ec);
		}
		NSDictionary settings = new NSDictionary(listName,ItogMark.ENTITY_NAME);
		Integer year = (Integer)ctx.session().valueForKey("eduYear");
		Enumeration itogEnu = itogsEnu(ec, listName, year);
		if(itogEnu == null)
			return null;
		NSMutableArray result = new NSMutableArray();
		NSArray subParams = (NSArray)ctx.session().valueForKeyPath(
				"strings.RujelEduResults_EduResults.groupReportSubs");
		String name = (String)ctx.session().valueForKeyPath(
				"strings.RujelEduResults_EduResults.itogAddOn.title");
		while (itogEnu.hasMoreElements()) {
			ItogContainer itog = (ItogContainer) itogEnu.nextElement();
			NSMutableDictionary dict = new NSMutableDictionary(itog,"eo");
			dict.takeValueForKey(settings, SettingsBase.ENTITY_NAME);
			dict.setObjectForKey(name + " - " + itog.title(),"title");
			dict.setObjectForKey("itog" + MyUtility.getID(itog), "id");
			int sort = itog.itogType().sort()*10 + itog.num();
			dict.setObjectForKey(String.valueOf(sort), "sort");
			dict.setObjectForKey(PlistReader.cloneArray(subParams, true), "subParams");
			try {
				NSMutableDictionary preload = new NSMutableDictionary("preloadMarks","methodName");
				Method method = ModuleInit.class.getMethod("preloadMarks", 
						EduGroup.class,ItogContainer.class,NSMutableDictionary.class);
				preload.setObjectForKey(method, "parsedMethod");
				preload.takeValueForKey(new NSArray(new Object[] {".",itog,"$itemDict"}),"paramValues");
				preload.takeValueForKey(Boolean.FALSE, "cacheResult");
				dict.takeValueForKey(preload, "preload");
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			result.addObject(dict);
		}
		return result;
	}
	
	public static NSMutableDictionary preloadMarks(EduGroup group, ItogContainer itog, 
			NSMutableDictionary itemDict) {
		if(group == null)
			return null;
		EOQualifier[] quals = new EOQualifier[2];
		quals[0] = Various.getEOInQualifier("student", group.list());
		quals[1] = new EOKeyValueQualifier(ItogMark.CONTAINER_KEY, 
				EOQualifier.QualifierOperatorEqual, itog);
		quals[1] = new EOAndQualifier(new NSArray(quals));
		EOFetchSpecification fs = new EOFetchSpecification(ItogMark.ENTITY_NAME,quals[1],
				new NSArray(new EOSortOrdering("studentID", EOSortOrdering.CompareAscending)));
		EOEditingContext ec = group.editingContext(); 
		NSArray found = ec.objectsWithFetchSpecification(fs);
		return preloadMarks(found, group, itog, itemDict);
	}
		
	public static NSMutableDictionary preloadMarks(NSArray marks, EduGroup group,
			ItogContainer itog, NSMutableDictionary itemDict) {
		if(marks == null || marks.count() == 0)
			return null;
		NSMutableArray subParams = (NSMutableArray)itemDict.valueForKey("subParams");
		EOQualifier.filterArrayWithQualifier(subParams, new EOKeyValueQualifier("metaSub", 
				EOQualifier.QualifierOperatorEqual, Boolean.TRUE));

		NSMutableDictionary result = new NSMutableDictionary();
		Enumeration enu = marks.objectEnumerator();
		NSMutableArray cycles = new NSMutableArray();
		NSMutableArray allMarks = new NSMutableArray();
		Student currStudent = null;
		NSMutableDictionary rowDict = null;
		String[] colors = new String[] {null,"#ff6666","#ffff33","#99ff66"};
		String[] states = new String[] {"none","bad","acceptable","good"};
		while (enu.hasMoreElements()) {
			ItogContainer.MarkContainer mark = (ItogContainer.MarkContainer) enu.nextElement();
			if(mark.student() != currStudent) {
				rowDict = new NSMutableDictionary();
				currStudent = mark.student();
				result.setObjectForKey(rowDict, currStudent);
			}
			if(!cycles.containsObject(mark.cycle()))
				Various.addToSortedList(mark.cycle(), cycles,null,EOSortOrdering.CompareAscending);
			if(!allMarks.containsObject(mark.mark()))
				Various.addToSortedList(mark.mark(), allMarks,null,EOSortOrdering.CompareDescending);
			String key = "cycle" + MyUtility.getID(mark.cycle());
			NSMutableDictionary dict = (NSMutableDictionary)rowDict.valueForKey(key);
			if(dict == null) {
				dict = new NSMutableDictionary(mark.mark(),"mark");
				rowDict.takeValueForKey(dict, key);
				if(mark.state() != null) {
					int state = mark.state().intValue();
					if(state >= 0 && state <= 3) {
						dict.takeValueForKey("background-color:" + colors[state], "stateColor");
						key = "stat_" + states[state];
						Counter counter = (Counter)rowDict.valueForKey(key);
						if(counter == null) {
							counter = new Counter(1);
							rowDict.takeValueForKey(counter, key);
						} else
							counter.raise();
					}
				}
			} else {
				String markValue = (String)dict.valueForKey("mark");
				if(!markValue.equals(mark.mark()))
					dict.takeValueForKey(null, "stateColor");
				markValue = markValue + '/' + mark.mark();
				dict.takeValueForKey(markValue, "mark");
			}
//			if(subParams.count() > 0) { // add to stats
				key = "stat" + mark.mark();
				Counter counter = (Counter)rowDict.valueForKey(key);
				if(counter == null) {
					counter = new Counter(1);
					rowDict.takeValueForKey(counter, key);
				} else
					counter.raise();
				
//			}
		} // marks enumeration
		for (int j = 0; j < 3; j++) {
			if(subParams.count() <= j)
				break;
			NSDictionary sub = (NSDictionary)subParams.objectAtIndex(j);
			if(!Various.boolForObject(sub.valueForKey("active")))
				continue;
			if("allMarks".equals(sub.valueForKey("id"))) {
				enu = cycles.objectEnumerator();
				while (enu.hasMoreElements()) {
					PlanCycle cycle = (PlanCycle) enu.nextElement();
					NSMutableDictionary dict = new NSMutableDictionary(Boolean.TRUE,"active");
					String id = "cycle" + MyUtility.getID(cycle);
					dict.takeValueForKey(id,"id");
					String subject = cycle.subject();
					dict.takeValueForKey(subject, "title");
					if(subject.length() > 4)
						dict.takeValueForKey(subject.substring(0,3), "short");
					dict.takeValueForKey(cycle.subjectEO().fullName(), "hover");
					dict.takeValueForKey("vertical_text", "titleClass");
					dict.takeValueForKey(Boolean.TRUE, "hiddenSub");
					dict.takeValueForKey("$preloadedValue." + id + ".mark", "value");
					dict.takeValueForKey("$preloadedValue." + id + ".stateColor", "style");
					subParams.addObject(dict);
				}				
			} // all marks
			else if("markStats".equals(sub.valueForKey("id"))) {
				EOEditingContext ec = group.editingContext();
				String listName = SettingsBase.stringSettingForCourse(ItogMark.ENTITY_NAME,
						SettingsBase.courseDict(group, itog.eduYear()),ec);
				Integer presetGroup = ItogPreset.getPresetGroup(listName, itog.eduYear(), itog.itogType());
				NSArray presets = ItogPreset.listPresetGroup(ec, presetGroup, true);
				boolean usePresets = (presets != null && presets.count() > 0);
				if (usePresets){
					enu = presets.objectEnumerator();
				} else { 
					enu = allMarks.objectEnumerator();
				}
					while (enu.hasMoreElements()) {
						String mark;
						NSMutableDictionary dict = new NSMutableDictionary(Boolean.TRUE,"active");
						if(usePresets) {
							ItogPreset pr = (ItogPreset) enu.nextElement();
							mark = pr.mark();
							if(pr.state() != null && pr.state() > 0)
								dict.takeValueForKey(new NSDictionary("0","valueWhenEmpty"), "presenterBindings");
						} else {
							mark = (String) enu.nextElement();
						}
						String id = "stat" + mark;
						dict.takeValueForKey(id,"id");
						dict.takeValueForKey(mark, "title");
						dict.takeValueForKey(Boolean.TRUE, "hiddenSub");
						dict.takeValueForKey("$preloadedValue." + id, "value");
						subParams.addObject(dict);
					}
			} // mark stats
			else if("successStats".equals(sub.valueForKey("id"))) {
				for (int i = states.length -1; i >= 0 ; i--) {
					String id = "stat_" + states[i];
					NSMutableDictionary dict = new NSMutableDictionary(Boolean.TRUE,"active");
					dict.takeValueForKey(id,"id");
					dict.takeValueForKey(ItogPreset.stateSymbols.objectAtIndex(i), "title");
					NSArray hovers = (NSArray)sub.valueForKey("hovers");
					dict.takeValueForKey(hovers.objectAtIndex(i), "hover");
					dict.takeValueForKey(Boolean.TRUE, "hiddenSub");
					dict.takeValueForKey("$preloadedValue." + id, "value");
					if(i > 0)
						dict.takeValueForKey(new NSDictionary("0","valueWhenEmpty"), "presenterBindings");
					subParams.addObject(dict);
				}
			} // success stats
		} // prepare titles
		return result;
	}
	
	public static String sectionListName(WOSession session, EOEditingContext ec) {
		NSMutableDictionary courseDict = null;
		SchoolSection section = SchoolSection.stateSection(session, ec);
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
						newTL.takeValueForKey(tl.valueForKey("presetGroup"), "presetGroup");
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
	
	public static Object updateCourseCycle(WOContext ctx) {
		EduCourse course = (EduCourse)ctx.userInfoForKey("course");
		EduCycle cycle = (EduCycle)ctx.userInfoForKey("cycle");
		if (course.cycle() == cycle)
			return null;
		EOQualifier[] quals = new EOQualifier[3];
		quals[0] = new EOKeyValueQualifier(ItogMark.CYCLE_KEY, 
				EOQualifier.QualifierOperatorEqual, course.cycle());
		quals[1] = new EOKeyValueQualifier("container.eduYear",
				EOQualifier.QualifierOperatorEqual, course.eduYear());
		quals[2] = Various.getEOInQualifier(ItogMark.STUDENT_KEY, course.groupList());
		quals[0] = new EOAndQualifier(new NSArray(quals));
		EOFetchSpecification fs = new EOFetchSpecification(ItogMark.ENTITY_NAME,quals[0],null);
		NSArray found = cycle.editingContext().objectsWithFetchSpecification(fs);
		if(found != null && found.count() > 0)
			found.takeValueForKey(cycle, ItogMark.CYCLE_KEY);
		return null;
	}
}
