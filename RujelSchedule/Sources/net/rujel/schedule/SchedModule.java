// SchedModule.java

/*
 * Copyright (c) 2008, Gennady & Michael Kushnir
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 * 
 * 	o	Redistributions of source code must retain the above copyright notice, this
 * 		list of conditions and the following disclaimer.
 * 	o	Redistributions in binary form must reproduce the above copyright notice,
 * 		this list of conditions and the following disclaimer in the documentation
 * 		and/or other materials provided with the distribution.
 * 	o	Neither the name of the RUJEL nor the names of its contributors may be used
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

package net.rujel.schedule;

import java.util.Enumeration;

import net.rujel.interfaces.EduCourse;
import net.rujel.interfaces.EduGroup;
import net.rujel.interfaces.Student;
import net.rujel.reusables.PlistReader;
import net.rujel.reusables.SettingsReader;
import net.rujel.reusables.Various;

import com.webobjects.appserver.WOApplication;
import com.webobjects.appserver.WOContext;
import com.webobjects.eoaccess.EOUtilities;
import com.webobjects.eocontrol.EOAndQualifier;
import com.webobjects.eocontrol.EOEditingContext;
import com.webobjects.eocontrol.EOFetchSpecification;
import com.webobjects.eocontrol.EOKeyGlobalID;
import com.webobjects.eocontrol.EOKeyValueQualifier;
import com.webobjects.eocontrol.EOQualifier;
import com.webobjects.eocontrol.EOSortOrdering;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSDictionary;
import com.webobjects.foundation.NSKeyValueCoding;
import com.webobjects.foundation.NSMutableDictionary;
import com.webobjects.foundation.NSMutableSet;
import com.webobjects.foundation.NSTimestamp;

public class SchedModule {
	public static Object init(Object obj, WOContext ctx) {
		if(obj == null || obj.equals("init")) {
			try {
				Object access = PlistReader.readPlist("access.plist", "RujelSchedule", null);
				WOApplication.application().takeValueForKey(access, "defaultAccess");
			} catch (NSKeyValueCoding.UnknownKeyException e) {
				// default access not supported
			}
			ScheduleEntry.init();
		} else if ("journalPlugins".equals(obj)) {
			return ctx.session().valueForKeyPath("strings.RujelSchedule_Schedule.dashboard");
		} else if("planTabs".equals(obj)) {
			if(Various.boolForObject(ctx.session().valueForKeyPath("readAccess.read.ScheduleRing")))
				return ctx.session().valueForKeyPath(
					"strings.RujelSchedule_Schedule.planTab");
			return null;
		} else if ("diary".equals(obj)) {
			NSDictionary diaryTab = (NSDictionary)WOApplication.application().
					valueForKeyPath("strings.RujelSchedule_Schedule.diaryTab");
			return PlistReader.cloneDictionary(diaryTab, true);
		} else if ("broadcastAdditions".equals(obj)) {
			return broadcastAdditions(ctx);
		}
		return null;
	}
	
	public static Object broadcastAdditions(WOContext ctx) {
		if(SettingsReader.boolForKeyPath("mail.dontBroadcastScheduleChanges", false))
			return null;
		NSDictionary params = (NSDictionary)ctx.session().objectForKey("broadcastAdditions");
		if(params == null)
			return null;
		if(!Various.boolForObject(params.valueForKey("diaryLink")))
			return null;
		NSArray students = (NSArray)params.valueForKey("students");
		if(students == null)
			return null;
		Student student = (Student)students.objectAtIndex(0);
		EOEditingContext ec = (EOEditingContext)params.valueForKey("editingContext");
		if(ec == null)
			ec = student.editingContext();
		EduGroup eduGroup = (EduGroup)params.valueForKey("eduGroup");
		if(eduGroup == null)
			eduGroup = student.recentMainEduGroup();
		else
			eduGroup = (EduGroup)EOUtilities.localInstanceOfObject(ec, eduGroup);
		NSArray courses = (NSArray)params.valueForKey("courses");
		EOQualifier[] quals = new EOQualifier[2];
		if(courses == null) {
			courses = EOUtilities.objectsMatchingKeyAndValue(ec, EduCourse.entityName,
					"eduGroup", eduGroup); // TODO : smarter selection

			quals[0] = new EOKeyValueQualifier("eduYear", EOQualifier.QualifierOperatorEqual,
					ctx.session().valueForKey("eduYear"));
			quals[1] = new EOKeyValueQualifier("eduGroup", 
					EOQualifier.QualifierOperatorEqual, eduGroup);
			quals[0] = new EOAndQualifier(new NSArray(quals));
			EOFetchSpecification fs = new EOFetchSpecification(EduCourse.entityName,quals[0],null);
			courses = ec.objectsWithFetchSpecification(fs);
		} else {
			courses = EOUtilities.localInstancesOfObjects(ec,courses);
		}
		NSTimestamp date = (NSTimestamp)ctx.session().valueForKey("today");//new NSTimestamp();
		NSTimestamp lately = date.timestampByAddingGregorianUnits(0, 0, -7, -12, 0, 0);
		quals[0] = Various.getEOInQualifier("course", courses);
		quals[1] = new EOKeyValueQualifier(ScheduleEntry.VALID_SINCE_KEY, 
				EOQualifier.QualifierOperatorGreaterThan, lately);
		quals[0] = new EOAndQualifier(new NSArray(quals));
		NSArray list = new NSArray(new EOSortOrdering (
				ScheduleEntry.VALID_SINCE_KEY,EOSortOrdering.CompareAscending));
		EOFetchSpecification fs = new EOFetchSpecification(ScheduleEntry.ENTITY_NAME,
				quals[0],list);
		list = ec.objectsWithFetchSpecification(fs);
		if(list == null || list.count() == 0)
			return null;
		StringBuilder buf = new StringBuilder();
		buf.append(WOApplication.application().valueForKeyPath(
				"strings.RujelSchedule_Schedule.messageAddition"));
		buf.append(':').append('\n').append(WOApplication.application().valueForKey("diaryUrl"));
		EOKeyGlobalID gid = (EOKeyGlobalID)ec.globalIDForObject(eduGroup);
		buf.append("?regime=schedule&grID=").append(gid.keyValues()[0]).append("&studentID=%1$d");
		String text = buf.toString();

		NSMutableDictionary dict = new NSMutableDictionary("schedule","id");
		dict.takeValueForKey("20", "sort");
		Enumeration enu = list.objectEnumerator();
		NSMutableSet include = new NSMutableSet();
		while (enu.hasMoreElements()) {
			ScheduleEntry sdl = (ScheduleEntry) enu.nextElement();
			NSArray aud = (NSArray)sdl.valueForKeyPath("course.audience");
			if(aud == null || aud.count() == 0) {
				dict.takeValueForKey(text, "text");
				return dict;
			}
			aud = (NSArray)sdl.valueForKeyPath("course.groupList");
			include.addObjectsFromArray(aud);
		}
		NSMutableSet exclude = new NSMutableSet(students);
		include.intersectSet(exclude);
		if(include.count() == 0)
			return null;
		exclude.subtractSet(include);
		if(exclude.count() == 0) {
			dict.takeValueForKey(text, "text");
			return dict;
		}
		enu = include.objectEnumerator();
		while (enu.hasMoreElements()) {
			student = (Student) enu.nextElement();
			dict.setObjectForKey(text, ec.globalIDForObject(student));
		}
		return dict;
	}
}
