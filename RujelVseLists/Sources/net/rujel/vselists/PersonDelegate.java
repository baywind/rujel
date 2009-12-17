// PersonDelegate.java

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

package net.rujel.vselists;

import com.webobjects.eoaccess.EOUtilities;
import com.webobjects.eocontrol.EOEditingContext;
import com.webobjects.eocontrol.EOEnterpriseObject;
import com.webobjects.eocontrol.EOFetchSpecification;
import com.webobjects.eocontrol.EOKeyValueQualifier;
import com.webobjects.eocontrol.EOSortOrdering;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSKeyValueCoding;

import net.rujel.interfaces.Person;
import net.rujel.interfaces.PersonLink;
import net.rujel.reusables.SettingsReader;

public class PersonDelegate {
	
	public static final NSArray sorter = new NSArray(
			new EOSortOrdering(VseStudent.DELO_KEY,EOSortOrdering.CompareAscending));

	public static Object fullName(PersonLink person, boolean startWithLastName,
			int lastNameDisplay,int firstNameDisplay,int secondNameDisplay) {
		if(person instanceof VseStudent || person instanceof VseTeacher) {
			Person p = person.person();
			if(p != null) {
				String lastName = p.lastName();
				if(lastName == null || lastName.length() == 0 || lastName.equals("?"))
					p = null;
			}
			if(p == null) {
				return NSKeyValueCoding.Utility.valueForKey(person, VseStudent.DELO_KEY);
			}
		}
		return null;
	}
	
	public static NSArray search(EOEditingContext ec,String entity, String searchString) {
		if(entity == null || searchString == null || searchString.length() == 0)
			return null;
		if(entity.equals(VseStudent.ENTITY_NAME) || entity.equals(VseTeacher.ENTITY_NAME)) {
			StringBuilder buf = new StringBuilder();
			buf.append('*').append(searchString).append('*');
			EOKeyValueQualifier qual = new EOKeyValueQualifier(VseTeacher.DELO_KEY,
					EOKeyValueQualifier.QualifierOperatorCaseInsensitiveLike,buf.toString());
			EOFetchSpecification fs = new EOFetchSpecification(entity,qual,sorter);
			fs.setRefreshesRefetchedObjects(true);
			NSArray result = ec.objectsWithFetchSpecification(fs);
			if(result != null && result.count() > 0)
				return result;
		}
		return null;
	}
	
	public static Object create(EOEditingContext ec,String entity, String initString) {
		if(entity.equals(VseStudent.ENTITY_NAME) || entity.equals(VseTeacher.ENTITY_NAME)) {
			String personEntity = SettingsReader.stringForKeyPath("interfaces.Person", 
					VsePerson.ENTITY_NAME);
			PersonLink person = Person.Utility.create(ec, personEntity, initString);
			if(person == null)
				return NSKeyValueCoding.NullValue;
			EOEnterpriseObject result = EOUtilities.createAndInsertInstance(ec, entity);
			result.takeValueForKey(person, VseStudent.PERSON_KEY);
			return result;
		}
		return null;
	}
}
