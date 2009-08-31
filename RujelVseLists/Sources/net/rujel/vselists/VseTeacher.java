//  VseTeacher.java

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

package net.rujel.vselists;

import com.webobjects.eoaccess.EOUtilities;
import com.webobjects.eocontrol.EOEditingContext;
import com.webobjects.eocontrol.EOQualifier;
import com.webobjects.eocontrol.EOSortOrdering;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSTimestamp;

import net.rujel.interfaces.Person;
import net.rujel.interfaces.PersonLink;
import net.rujel.interfaces.Teacher;

public class VseTeacher extends _VseTeacher implements Teacher{

	public static void init() {
		EOSortOrdering.ComparisonSupport.setSupportForClass(
				new Person.ComparisonSupport(), VsePerson.class);
		EOSortOrdering.ComparisonSupport.setSupportForClass(
				new PersonLink.ComparisonSupport(), VseTeacher.class);
	}

	public static VseTeacher teacherForPerson(VsePerson person, NSTimestamp date) {
		EOEditingContext ec = person.editingContext();
		NSArray list = EOUtilities.objectsMatchingKeyAndValue(ec, ENTITY_NAME,
				PERSON_KEY, person);
		if(list == null || list.count() == 0)
			return null;
		if(date != null) {
			NSArray args = new NSArray(new Object[] {date,date});
			EOQualifier qual = EOQualifier.qualifierWithQualifierFormat(
					"(enter = nil OR enter <= %@) AND (leave = nil OR leave >= %@)", args);
			list = EOQualifier.filteredArrayWithQualifier(list, qual);
			if(list == null || list.count() == 0)
				return null;
		}
		return (VseTeacher)list.objectAtIndex(0);
	}

	public static VseTeacher teacherForPerson(VsePerson person, NSTimestamp date,
			boolean create) {
		VseTeacher teacher = teacherForPerson(person, date);
		if(create && teacher == null) {
			EOEditingContext ec = person.editingContext();
			teacher = (VseTeacher)EOUtilities.createAndInsertInstance(ec, ENTITY_NAME);
			teacher.addObjectToBothSidesOfRelationshipWithKey(person, PERSON_KEY);
			teacher.setEnter(date);
		}
		return teacher;
	}}
