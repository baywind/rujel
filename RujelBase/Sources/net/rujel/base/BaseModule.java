// BaseModule.java

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

package net.rujel.base;


import java.util.Enumeration;
import java.util.logging.Logger;

import net.rujel.interfaces.EduCourse;
import net.rujel.reusables.PlistReader;
import net.rujel.reusables.Various;
import net.rujel.reusables.WOLogLevel;

import com.webobjects.appserver.WOApplication;
import com.webobjects.appserver.WOContext;
import com.webobjects.eoaccess.EOUtilities;
import com.webobjects.eocontrol.EOAndQualifier;
import com.webobjects.eocontrol.EOEditingContext;
import com.webobjects.eocontrol.EOEnterpriseObject;
import com.webobjects.eocontrol.EOFetchSpecification;
import com.webobjects.eocontrol.EOGlobalID;
import com.webobjects.eocontrol.EOKeyGlobalID;
import com.webobjects.eocontrol.EOKeyValueQualifier;
import com.webobjects.eocontrol.EONotQualifier;
import com.webobjects.eocontrol.EOQualifier;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSDictionary;
import com.webobjects.foundation.NSKeyValueCoding;
import com.webobjects.foundation.NSMutableDictionary;

public class BaseModule {

	public static Object init(Object obj, WOContext ctx) {
		if(obj == null || obj.equals("init")) {
			try {
				Object access = PlistReader.readPlist("access.plist", "RujelBase", null);
				WOApplication.application().takeValueForKey(access, "defaultAccess");
			} catch (NSKeyValueCoding.UnknownKeyException e) {
				// default access not supported
			}
			init();
		} else if("presentTabs".equals(obj)) {
			NSDictionary lessonsTab = (NSDictionary)ctx.session().valueForKeyPath(
					"strings.RujelBase_Base.lessonsTab");
			if(lessonsTab == null)
				return new NSMutableDictionary();
			return PlistReader.cloneDictionary(lessonsTab, true);
		} else if("reportForStudent".equals(obj)) {
			NSDictionary settings = (NSDictionary)ctx.session().objectForKey("reportForStudent");
			return LessonReport.reportForStudent(settings);
		} else if("reportSettingsForStudent".equals(obj)) {
			NSDictionary reportSettings = (NSDictionary)WOApplication.application().
					valueForKeyPath("strings.RujelBase_Base.reportSettings");
			return PlistReader.cloneDictionary(reportSettings, true);
		} else if("deleteCourse".equals(obj)) {
			return deleteCourse(ctx);
		} else if("courseComplete".equals(obj)) {
			return ctx.session().valueForKeyPath("strings.RujelBase_Base.courseComplete");
		} else if("completionLock".equals(obj)) {
			return new NSDictionary(new String[] {"BaseNote","lesson.course","student"},
					new String[] {"entity","coursePath","studentPath"});
		} else if ("diary".equals(obj)) {
			NSDictionary diaryTabs = (NSDictionary)WOApplication.application().
					valueForKeyPath("strings.RujelBase_Base.diaryTab");
			return PlistReader.cloneDictionary(diaryTabs, true);
		} else if("deleteStudents".equals(obj)) {
			return deleteStudents(ctx);
		} else if("xmlGeneration".equals(obj)) {
			NSDictionary options = (NSDictionary)ctx.session().objectForKey("xmlGeneration");
			return new LessonsXML(options);
		} else if("archiveType".equals(obj)) {
			return ctx.session().valueForKeyPath("strings.RujelBase_Base.archiveType");
		} else if("usedModels".equals(obj)) {
			return new NSArray(new String[] {"BaseStatic","BaseYearly"});
		} else if("initialData".equals(obj)) {
			return initialData(ctx);
		}
		return null;
	}

	public static void init() {
/*		EOEditingContext ec = new EOEditingContext();
		ec.lock();
		try {
			NSArray list = EOUtilities.objectsMatchingKeyAndValue(ec, 
					Indexer.ENTITY_NAME, Indexer.TYPE_KEY, new Integer((int)Short.MIN_VALUE));
			EOEnterpriseObject typeIndex = null;
			if(list == null || list.count() == 0) {
				typeIndex = EOUtilities.createAndInsertInstance(ec,Indexer.ENTITY_NAME);
				typeIndex.takeValueForKey(new Integer((int)Short.MIN_VALUE), Indexer.TYPE_KEY);
				typeIndex.takeValueForKey("index types",Indexer.TITLE_KEY);
				ec.saveChanges();
				Logger.getLogger("rujel.base").log(WOLogLevel.COREDATA_EDITING,
						"Automatically generated type index");
			} else {
				typeIndex = (EOEnterpriseObject)list.objectAtIndex(0);
			}
			Indexer.setTipesGID(ec.globalIDForObject(typeIndex));
		} catch (Exception e) {
			Logger.getLogger("rujel.base").log(WOLogLevel.WARNING,
					"Error autogenerating type index",e);
		} finally {
			ec.unlock();
		} */
	}
	
	public static Object deleteCourse(WOContext ctx) {
		EduCourse course = (EduCourse)ctx.session().objectForKey("deleteCourse");
		if(course.lessons() == null || course.lessons().count() == 0)
			return null;
		String message = (String)WOApplication.application().valueForKeyPath(
				"strings.RujelBase_Base.courseHasLessons");
		ctx.session().takeValueForKey(message, "message");
		return message;
	}
	
	public static Object deleteStudents(WOContext ctx) {
		NSArray students = (NSArray)ctx.session().objectForKey("deleteStudents");
		if(students == null || students.count() == 0)
			return null;
		EOQualifier qual = Various.getEOInQualifier("student", students);
		EOFetchSpecification fs = new EOFetchSpecification("BaseNote",qual,null);
		fs.setFetchLimit(1);
		EOEnterpriseObject student = (EOEnterpriseObject)students.objectAtIndex(0);
		NSArray found = student.editingContext().objectsWithFetchSpecification(fs);
		if(found != null && found.count() > 0) {
			return ctx.session().valueForKeyPath(
					"strings.RujelBase_Base.relatedNotesFound");
		}
		return null;
	}
	
	public static Object initialData(WOContext ctx) {
		EOEditingContext prevEc = (EOEditingContext)ctx.userInfoForKey("prevEc");
		if(prevEc == null)
			return null;
		Logger logger = Logger.getLogger("rujel.base");
		EOEditingContext ec = (EOEditingContext)ctx.userInfoForKey("ec");
		EOQualifier[] quals = new EOQualifier[] {
				new EOKeyValueQualifier(SettingsBase.KEY_KEY,
						EOQualifier.QualifierOperatorNotEqual, "CompletionActive"),
				new EOKeyValueQualifier(SettingsBase.KEY_KEY,
								EOQualifier.QualifierOperatorNotEqual, "coursesDone"),
				new EOKeyValueQualifier(SettingsBase.KEY_KEY,
								EOQualifier.QualifierOperatorNotEqual, "defaultWorkType"),
				new EONotQualifier(new EOKeyValueQualifier(SettingsBase.KEY_KEY,
								EOQualifier.QualifierOperatorLike, "presenters.*")),
				new EOKeyValueQualifier(SettingsBase.KEY_KEY,
						EOQualifier.QualifierOperatorNotEqual, "CriteriaSet")
		};
		quals[0] = new EOAndQualifier(new NSArray(quals));
		EOFetchSpecification fs = new EOFetchSpecification(SettingsBase.ENTITY_NAME,quals[0],null);
		NSArray found = prevEc.objectsWithFetchSpecification(fs);
		if(found != null && found.count() > 0) try {
			Enumeration enu = found.objectEnumerator();
			while (enu.hasMoreElements()) {
				SettingsBase sb = (SettingsBase) enu.nextElement();
				copySetting(sb, ec, null);
			}
			ec.saveChanges();
			logger.log(WOLogLevel.INFO,"Copied SettingsBase from previous year", found.valueForKey(SettingsBase.KEY_KEY));
		} catch (Exception e) {
			logger.log(WOLogLevel.WARNING,"Failed to copy SettingsBase from previous year", e);
			ec.revert();
		} // found SettingsBase
		
		found = EOUtilities.objectsForEntityNamed(prevEc, Indexer.ENTITY_NAME);
		if(found != null && found.count() > 0) try {
			Enumeration enu = found.objectEnumerator();
			NSMutableDictionary done = (NSMutableDictionary)ctx.userInfoForKey("doneIndexers");
			if(done == null) {
				done = new NSMutableDictionary();
				ctx.setUserInfoForKey(done, "doneIndexers");
			}
			while (enu.hasMoreElements()) {
				Indexer idx = (Indexer) enu.nextElement();
				EOGlobalID gid = prevEc.globalIDForObject(idx);
				if(done != null && done.containsKey(gid))
					continue;
				idx = copyIndexer(ec, idx);
				done.setObjectForKey(idx, gid);
			}
			ec.saveChanges();
			enu = done.keyEnumerator();
			while (enu.hasMoreElements()) {
				EOGlobalID gid = (EOGlobalID) enu.nextElement();
				Object value = done.objectForKey(gid);
				if(value instanceof EOEnterpriseObject)
					done.setObjectForKey(ec.globalIDForObject((EOEnterpriseObject)value), gid);
			}
			logger.log(WOLogLevel.INFO,"Copied Indexers from previous year");
		} catch (Exception e) {
			logger.log(WOLogLevel.WARNING,"Failed to copy Indexers from previous year", e);
			ec.revert();
		} // found Indexer
		return null;
	}

	public static void copySetting(SettingsBase oldBase, EOEditingContext newEC,
			NSMutableDictionary idMatch) {
		SettingsBase newBase = SettingsBase.baseForKey(oldBase.key(), newEC, true);
		newBase.setTextValue(oldBase.textValue());
		Integer num = oldBase.numericValue();
		if(num != null) {
			if(idMatch == null) {
				newBase.setNumericValue(num);
			} else {
				EOEnterpriseObject newCS = (EOEnterpriseObject)idMatch.objectForKey(num);
				EOKeyGlobalID gid = (EOKeyGlobalID)newEC.globalIDForObject(newCS);
				newBase.setNumericValue((Integer)gid.keyValues()[0]);
			}
		}
		NSArray qslist = oldBase.qualifiedSettings();
		if(qslist != null && qslist.count() > 0) {
			Enumeration enu = qslist.objectEnumerator();
			Integer year = (Integer)newEC.userInfoForKey("eduYear");
			while (enu.hasMoreElements()) {
				QualifiedSetting qs = (QualifiedSetting) enu.nextElement();
				if(qs.eduYear() != null && !qs.eduYear().equals(year))
					continue;
				QualifiedSetting newQ = (QualifiedSetting)EOUtilities.
				createAndInsertInstance(newEC, QualifiedSetting.ENTITY_NAME);
				newQ.addObjectToBothSidesOfRelationshipWithKey(newBase,
						QualifiedSetting.SETTINGS_BASE_KEY);
				if(qs.section() != null)
					newQ.setSection(EOUtilities.localInstanceOfObject(newEC, qs.section()));
				newQ.setSort(qs.sort());
				newQ.setQualifierString(qs.qualifierString());
				newQ.setArgumentsString(qs.argumentsString());
				newQ.setTextValue(qs.textValue());
				num = qs.numericValue();
				if(num == null)
					continue;
				if(idMatch == null) {
					newQ.setNumericValue(num);
				} else {
					EOEnterpriseObject newCS = (EOEnterpriseObject)idMatch.objectForKey(num);
					EOKeyGlobalID gid = (EOKeyGlobalID)newEC.globalIDForObject(newCS);
					newQ.setNumericValue((Integer)gid.keyValues()[0]);
				}
			}
		}
	}

	
	public static Indexer copyIndexer(EOEditingContext ec, Indexer idx) {
		Indexer newI = (Indexer)EOUtilities.createAndInsertInstance(
				ec, Indexer.ENTITY_NAME);
		newI.setTitle(idx.title());
		newI.setType(idx.type());
		newI.setDefaultValue(idx.defaultValue());
		newI.setFormatString(idx.formatString());
		newI.setComment(idx.comment());
		NSArray index = idx.indexRows();
		if(index == null || index.count() == 0)
			return newI;
		Enumeration ienu = index.objectEnumerator();
		while (ienu.hasMoreElements()) {
			IndexRow ir = (IndexRow) ienu.nextElement();
			IndexRow newR = (IndexRow)EOUtilities.createAndInsertInstance(
					ec, IndexRow.ENTITY_NAME);
			newR.addObjectToBothSidesOfRelationshipWithKey(newI, IndexRow.INDEXER_KEY);
			newR.setIdx(ir.idx());
			newR.setValue(ir.value());
			newR.setComment(ir.comment());
		}
		return newI;
	}
}
