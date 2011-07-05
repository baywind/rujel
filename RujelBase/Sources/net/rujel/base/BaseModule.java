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

import net.rujel.interfaces.EduCourse;
import net.rujel.interfaces.Student;
import net.rujel.reusables.PlistReader;
import net.rujel.reusables.Various;
import net.rujel.ui.DateAgregate;

import com.webobjects.appserver.WOApplication;
import com.webobjects.appserver.WOContext;
import com.webobjects.appserver.WOMessage;
import com.webobjects.eocontrol.EOAndQualifier;
import com.webobjects.eocontrol.EOEditingContext;
import com.webobjects.eocontrol.EOEnterpriseObject;
import com.webobjects.eocontrol.EOFetchSpecification;
import com.webobjects.eocontrol.EOKeyValueQualifier;
import com.webobjects.eocontrol.EOQualifier;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSDictionary;
import com.webobjects.foundation.NSKeyValueCoding;
import com.webobjects.foundation.NSMutableArray;
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
		} else if ("dateAgregate".equals(obj)) {
			return dateAgregate(ctx);
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
	
	public static Object dateAgregate(WOContext ctx) {
		DateAgregate agr = (DateAgregate)ctx.session().objectForKey("dateAgregate");
		if (agr == null)
			return null;
		EOQualifier qual = new EOKeyValueQualifier("course",
				EOQualifier.QualifierOperatorEqual,agr.course());
		if(agr.begin != null || agr.end != null) {
			NSMutableArray quals = new NSMutableArray(qual);
			if(agr.begin != null) {
				quals.addObject(new EOKeyValueQualifier(BaseLesson.DATE_KEY, 
						EOQualifier.QualifierOperatorGreaterThanOrEqualTo, agr.begin));
			}
			if(agr.end != null) {
				quals.addObject(new EOKeyValueQualifier(BaseLesson.DATE_KEY, 
						EOQualifier.QualifierOperatorLessThanOrEqualTo, agr.end));
			}
			if(quals.count() > 1)
				qual = new EOAndQualifier(quals);
		}
		EOFetchSpecification fs = new EOFetchSpecification(
				BaseLesson.ENTITY_NAME,qual,MyUtility.dateSorter);
		EOEditingContext ec = agr.course().editingContext();
		NSArray lessons = ec.objectsWithFetchSpecification(fs);
		if(lessons == null || lessons.count() == 0) {
			return null;
		} else {
			Enumeration enu = lessons.objectEnumerator();
			NSMutableArray dates = new NSMutableArray(lessons.count());
			int lDate = 0;
			NSMutableArray array = null;
			while (enu.hasMoreElements()) {
				BaseLesson lesson = (BaseLesson) enu.nextElement();
				int wDate = agr.dateIndex(lesson.date());
				if(array == null || wDate != lDate) {
					if(array != null) {
						dates.addObject(array.toArray(new BaseLesson[array.count()]));
					}
					array = new NSMutableArray(lesson);
				} else {
					array.addObject(lesson);
				}
			}
			if(array != null)
				dates.addObject(array.toArray(new BaseLesson[array.count()]));
			lessons = dates.immutableClone();
		}
		Enumeration enu = lessons.objectEnumerator();
		String txt = null;
		while (enu.hasMoreElements()) {
			BaseLesson[] lesson = (BaseLesson[]) enu.nextElement();
			NSMutableDictionary lDict = agr.getOrCreateOnDate(lesson[0].date());
			boolean comments = false;
			for (int i = 0; i < lesson.length; i++) {
				NSArray notes = lesson[i].notes();
				if(notes == null || notes.count() == 0)
					continue;
				Enumeration nEnu = notes.objectEnumerator();
				NSDictionary skip = new NSDictionary("grey","styleClass");
				while (nEnu.hasMoreElements()) {
					EOEnterpriseObject note = (EOEnterpriseObject) nEnu.nextElement();
					String value = (String)note.valueForKey("note");
					if(value == null || value.length() == 0)
						continue;
					Student student = (Student)note.valueForKey("student");
					NSMutableDictionary stDict = (NSMutableDictionary)lDict.objectForKey(student);
					if(stDict == null) {
						stDict = new NSMutableDictionary();
						lDict.setObjectForKey(stDict, student);
					}
					int idx = BaseLesson.isSkip(value);
					if(idx > 0) {
						stDict.takeValueForKey(skip, "baseAttendance");
						if(value.length() > idx+1)
							value = value.substring(idx +1);
						else
							continue;
					}
					comments = true;
					if(txt == null) {
						txt = WOApplication.application().resourceManager().
						urlForResourceNamed("text.png","RujelBase",null,ctx.request());
						txt = "<img src=\"" + txt + 
							"\" alt=\"txt\" height=\"16\" width=\"16\">";
					}
					NSMutableDictionary[] nts = (NSMutableDictionary[])
						stDict.valueForKey("BaseNote");
					if(nts == null) {
						nts = new NSMutableDictionary[] {new NSMutableDictionary(txt,"value")};
						stDict.takeValueForKey(nts, "BaseNote");
					}
					value = WOMessage.stringByEscapingHTMLAttributeValue(value);
					DateAgregate.appendValueToKeyInDict(value, "hover", nts[0], "\n");
				} // notes in lesson
				if(comments) {
					lDict.takeValueForKey(new NSMutableDictionary[] {
						new NSMutableDictionary(lesson[0],"object") } , "BaseNote");
				}
			} // lessons in date
		}
		return ctx.session().valueForKeyPath("strings.RujelBase_Base.consolidatedView");
	}
}
