//  SettingsBase.java

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

package net.rujel.base;

import java.util.Enumeration;

import net.rujel.interfaces.EOInitialiser;
import net.rujel.interfaces.EduCourse;

import com.webobjects.foundation.*;
import com.webobjects.eocontrol.*;

public class SettingsBase extends _SettingsBase {

	protected static final String[] keys = new String[] {"grade","cycle","eduGroup","teacher"};
	
	public static void init() {
		EOInitialiser.initialiseRelationship("SettingsByCourse","course",false,"courseID","EduCourse");
		EOInitialiser.initialiseRelationship("SettingsByCourse","cycle",false,"cycleID","EduCycle");
		EOInitialiser.initialiseRelationship("SettingsByCourse","eduGroup",false,"groupID","EduGroup");
		EOInitialiser.initialiseRelationship("SettingsByCourse","teacher",false,"teacherID","Teacher");
	}

	public void awakeFromInsertion(EOEditingContext ec) {
		super.awakeFromInsertion(ec);
	}

	public EOEnterpriseObject forCourse(EduCourse course) {
		if(course == null)
			return this;
		NSArray byCourse = byCourse();
		if(byCourse == null || byCourse.count() == 0)
			return this;
		EOEnterpriseObject result = this;
		Enumeration en = byCourse.objectEnumerator();
		NSMutableArray matches = new NSMutableArray();
		bycourse:
		while (en.hasMoreElements()) {
			EOEnterpriseObject bc = (EOEnterpriseObject) en.nextElement();
			if(bc.valueForKey("course") == course)
				return bc;
			if(matches.count() > 0) {
				Enumeration menu = matches.objectEnumerator();
				while (menu.hasMoreElements()) {
					String key = (String) menu.nextElement();
					if(!match(bc, course, key))
						continue bycourse;
				}
			}
			if(matches.count() > 0)
				matches.removeAllObjects();
			for (int i = 0; i < keys.length; i++) {
				if(match(bc, course, keys[i])) {
					matches.addObject(keys[i]);
					result = bc;
				}
			}
		}
		return result;
	}
	
	protected boolean match(EOEnterpriseObject bc, EduCourse course, String key) {
		if(key.equals("grade")) {
			Integer grade = (Integer)bc.valueForKey(key);
			return (grade != null && grade.equals(course.cycle().grade()));
		}
		return (bc.valueForKey(key) == course.valueForKey(key));
	}
}
