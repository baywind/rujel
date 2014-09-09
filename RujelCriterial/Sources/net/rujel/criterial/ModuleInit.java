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

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.Enumeration;
import java.util.logging.Logger;

import net.rujel.base.BaseLesson;
import net.rujel.base.BaseModule;
import net.rujel.base.Indexer;
import net.rujel.base.SettingsBase;
import net.rujel.interfaces.EOInitialiser;
import net.rujel.interfaces.EduCourse;
import net.rujel.interfaces.EduLesson;
import net.rujel.reusables.DataBaseUtility;
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
		} else if("usedModels".equals(obj)) {
			return "Criterial";
		} else if("initialData".equals(obj)) {
			return initialData(ctx);
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
			if(Various.boolForObject(ses.valueForKeyPath("readAccess.read." + key))) {
				Object module = setup.valueForKey(key);
				if(module instanceof NSArray)
					result.addObjectsFromArray((NSArray)module);
				else
					result.addObject(module);
			}
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

	public static Object initialData(WOContext ctx) {
		EOEditingContext prevEc = (EOEditingContext)ctx.userInfoForKey("prevEc");
		Logger logger = Logger.getLogger("rujel.criterial");
		if(prevEc == null) { // load predefined data
			try {
				InputStream data = WOApplication.application().resourceManager().
				inputStreamForResourceNamed("dataCriterial.sql", "RujelCriterial", null);
				//			EOEditingContext ec = (EOEditingContext)ctx.userInfoForKey("ec");
				EOObjectStoreCoordinator os = EOObjectStoreCoordinator.defaultCoordinator();
				DataBaseUtility.executeScript(os, "Criterial", data);
			} catch (Exception e) {
				logger.log(WOLogLevel.WARNING, "Failed to load inital data for Criterial model",e);
			}
		} else { // copy from previous year
			EOEditingContext ec = (EOEditingContext)ctx.userInfoForKey("ec");
			NSArray found = EOUtilities.objectsForEntityNamed(prevEc, WorkType.ENTITY_NAME);
			if(found != null && found.count() > 0) try {
				Enumeration enu = found.objectEnumerator();
				NSMutableDictionary idMatch = new NSMutableDictionary();
				while (enu.hasMoreElements()) {
					EOEnterpriseObject wt = (EOEnterpriseObject) enu.nextElement();
					EOEnterpriseObject newT = EOUtilities.createAndInsertInstance(
							ec, WorkType.ENTITY_NAME);
					newT.updateFromSnapshot(wt.snapshot());
					{
						EOKeyGlobalID gid = (EOKeyGlobalID)prevEc.globalIDForObject(wt);
						idMatch.setObjectForKey(newT, gid.keyValues()[0]);
					}
				}
				ec.saveChanges();
				logger.log(WOLogLevel.INFO,"Copied WorkTypes from previous year");
				SettingsBase base = SettingsBase.baseForKey("defaultWorkType", prevEc, false);
				if(base != null) {
					BaseModule.copySetting(base, ec, idMatch);
					ec.saveChanges();
					logger.log(WOLogLevel.INFO,"Copied WorkType settings from previous year");
				}
			} catch (Exception e) {
				logger.log(WOLogLevel.WARNING,"Failed to copy WorkTypes from previous year", e);
				ec.revert();
			}

			found = EOUtilities.objectsForEntityNamed(prevEc, BorderSet.ENTITY_NAME);
			if(found != null && found.count() > 0) try { // Borders
				Enumeration enu = found.objectEnumerator();
				NSMutableDictionary idMatch = new NSMutableDictionary(found.count());
				NSArray keys = new NSArray(new String[] {BorderSet.TITLE_KEY,
						BorderSet.ZERO_VALUE_KEY,BorderSet.FORMAT_STRING_KEY,
						BorderSet.VALUE_TYPE_KEY, BorderSet.USE_CLASS_KEY, BorderSet.EXCLUDE_KEY});
				while (enu.hasMoreElements()) {
					BorderSet bs = (BorderSet) enu.nextElement();
					BorderSet newT = (BorderSet)EOUtilities.createAndInsertInstance(
							ec, BorderSet.ENTITY_NAME);
					newT.takeValuesFromDictionary(bs.valuesForKeys(keys));
					{
						EOKeyGlobalID gid = (EOKeyGlobalID)prevEc.globalIDForObject(bs);
						idMatch.setObjectForKey(newT, gid.keyValues()[0]);
					}
					NSArray borders = bs.borders();
					if(borders != null && borders.count() > 0) {
						Enumeration benu = borders.objectEnumerator();
						while (benu.hasMoreElements()) {
							EOEnterpriseObject b = (EOEnterpriseObject) benu.nextElement();
							EOEnterpriseObject newB = EOUtilities.createAndInsertInstance(
									ec, b.entityName());
							newT.addObjectToBothSidesOfRelationshipWithKey(
									newB, BorderSet.BORDERS_KEY);
							newB.takeValueForKey(b.valueForKey("title"), "title");
							newB.takeValueForKey(b.valueForKey("least"), "least");
						}
					}
				}
				ec.saveChanges();
				logger.log(WOLogLevel.INFO,"Copied BorderSets from previous year");
				NSArray presenters = EOUtilities.objectsWithQualifierFormat(prevEc, 
						SettingsBase.ENTITY_NAME, "key like 'presenters.*'", null);
				if(presenters != null && presenters.count() > 0) {
					enu = presenters.objectEnumerator();
					while (enu.hasMoreElements()) {
						SettingsBase base = (SettingsBase) enu.nextElement();
						BaseModule.copySetting(base, ec, idMatch);
					}
				}
			} catch (Exception e) {
				logger.log(WOLogLevel.WARNING,"Failed to copy BorderSets from previous year", e);
				ec.revert();
			}
			
			found = EOUtilities.objectsForEntityNamed(prevEc, CriteriaSet.ENTITY_NAME);
			if(found != null && found.count() > 0) try { // CriteriaSet
				Enumeration enu = found.objectEnumerator();
				NSMutableDictionary indexes = (NSMutableDictionary)
						ctx.userInfoForKey("doneIndexers");
				NSArray keys = new NSArray( new String[] {"criterion","title",
						"dfltMax","dfltWeight","comment","flags"});
				NSMutableDictionary idMatch = new NSMutableDictionary();
				while (enu.hasMoreElements()) {
					CriteriaSet bs = (CriteriaSet) enu.nextElement();
					CriteriaSet newT = (CriteriaSet)EOUtilities.createAndInsertInstance(
							ec, CriteriaSet.ENTITY_NAME);
					newT.setSetName(bs.setName());
					newT.setComment(bs.comment());
					newT.setFlags(bs.flags());
					{
						EOKeyGlobalID gid = (EOKeyGlobalID)prevEc.globalIDForObject(bs);
						idMatch.setObjectForKey(newT, gid.keyValues()[0]);
					}
					NSArray crits = bs.criteria();
					if(crits != null && crits.count() > 0) {
						Enumeration cenu = crits.objectEnumerator();
						while (cenu.hasMoreElements()) {
							EOEnterpriseObject b = (EOEnterpriseObject) cenu.nextElement();
							EOEnterpriseObject newB = EOUtilities.createAndInsertInstance(
									ec, b.entityName());
							newT.addObjectToBothSidesOfRelationshipWithKey(
									newB, CriteriaSet.CRITERIA_KEY);
							newB.takeValuesFromDictionary(b.valuesForKeys(keys));
							Indexer idx = (Indexer)b.valueForKey("indexer");
							if(idx != null) {
								EOGlobalID gid = prevEc.globalIDForObject(idx);
								if(indexes == null) {
									indexes = new NSMutableDictionary();
									ctx.setUserInfoForKey(indexes, "doneIndexers");
								}
								Object val = indexes.objectForKey(gid);
								if(val instanceof Indexer) {
									idx = (Indexer)val;
									if(idx.editingContext() != ec)
										idx = (Indexer)EOUtilities.localInstanceOfObject(ec, idx);
								} else if(val instanceof EOGlobalID) {
									idx = (Indexer)ec.faultForGlobalID((EOGlobalID)val, ec);
								} else {
									idx = BaseModule.copyIndexer(ec, idx);
									indexes.setObjectForKey(idx, gid);
								}
								newB.addObjectToBothSidesOfRelationshipWithKey(idx, "indexer");
							}
						}
					}
				}
				ec.saveChanges();
				logger.log(WOLogLevel.INFO,"Copied CriteriaSets from previous year");
				enu = indexes.keyEnumerator();
				while (enu.hasMoreElements()) {
					EOGlobalID gid = (EOGlobalID) enu.nextElement();
					Object value = indexes.objectForKey(gid);
					if(value instanceof EOEnterpriseObject) {
						EOGlobalID newGid = ec.globalIDForObject((EOEnterpriseObject)value);
						indexes.setObjectForKey(newGid, gid);
					}
				}
				SettingsBase oldBase = SettingsBase.baseForKey(
						CriteriaSet.ENTITY_NAME, prevEc, false);
				if(oldBase != null) {
					BaseModule.copySetting(oldBase, ec, idMatch);
					ec.saveChanges();
					logger.log(WOLogLevel.INFO,"Copied CriteriaSet settings from previous year");
				}
			} catch (Exception e) {
				logger.log(WOLogLevel.WARNING,"Failed to copy CriteriaSets from previous year", e);
				ec.revert();
			}
		}
		return null;
	}
}
