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


import net.rujel.interfaces.EduCourse;
import net.rujel.reusables.PlistReader;
import net.rujel.reusables.Various;

import com.webobjects.appserver.WOApplication;
import com.webobjects.appserver.WOContext;
import com.webobjects.eocontrol.EOEnterpriseObject;
import com.webobjects.eocontrol.EOFetchSpecification;
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
}
