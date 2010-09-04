// TableUser.java

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

package net.rujel.user;

import java.util.Enumeration;

import com.webobjects.eocontrol.EOEditingContext;
import com.webobjects.eocontrol.EOEnterpriseObject;
import com.webobjects.eocontrol.EOGlobalID;
import com.webobjects.eocontrol.EOKeyGlobalID;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSMutableDictionary;

import net.rujel.auth.UserPresentation;
import net.rujel.auth.UserPresentation.DefaultImplementation;
import net.rujel.interfaces.Person;
import net.rujel.interfaces.PersonLink;
import net.rujel.interfaces.Student;
import net.rujel.interfaces.Teacher;

public class TableUser extends DefaultImplementation implements
		UserPresentation {
	
	protected String username;
	protected EOGlobalID userGID;
	protected EOGlobalID personGID;
	protected String[] groups;
	protected NSMutableDictionary properties;
	protected UserPresentation parent;
	protected String present;
	
	public TableUser (AutUser au, UserPresentation parent) {
		EOEditingContext ec = au.editingContext();
		username = au.userName();
		userGID = ec.globalIDForObject(au);
		if(au.personEntity() != null && au.personID() != null) {
			personGID = EOKeyGlobalID.globalIDWithEntityName(
					au.personEntity().entName(), new Object[] {au.personID()});
			present = Person.Utility.fullName(personLink(ec), true, 2, 2, 2);
		} else {
			present = username;
		}
		NSArray list = au.userProperties(); 
		if(list != null && list.count() > 0) {
			Enumeration enu = list.objectEnumerator();
			properties = new NSMutableDictionary();
			while (enu.hasMoreElements()) {
				EOEnterpriseObject pr = (EOEnterpriseObject) enu.nextElement();
				properties.setObjectForKey(pr.valueForKey("propValue"), pr.valueForKey("propKey"));
			}
		}
		list = au.groups();
		if(list != null && list.count() > 0) {
			groups = new String[list.count()];
			for (int i = 0; i < groups.length; i++) {
				EOEnterpriseObject gr = (EOEnterpriseObject)list.objectAtIndex(i);
				groups[i] = (String)gr.valueForKey("groupName");
			}
		}
		this.parent = parent;
	}

	public AutUser userEO(EOEditingContext ec) {
		return (AutUser)ec.faultForGlobalID(userGID, ec);
	}
	
	public PersonLink personLink(EOEditingContext ec) {
		if(personGID == null) 
			return null;
		return (PersonLink)ec.faultForGlobalID(personGID, ec);
	}
	
	public void _updatePersonLink(EOEnterpriseObject plink) {
		personGID = plink.editingContext().globalIDForObject(plink);
	}
	
	public Object[] listGroups() {
		if(groups == null)
			return null;
		return groups.clone();
	}

	public String present() {
		return present;
	}

	public boolean isInGroup (Object group) {
		return (group.equals(username) ||
				(groups != null && super.isInGroup(group)) ||
				(parent != null && parent.isInGroup(group)));
	}
	
	public Object propertyNamed(String property) {
		if(property.equals("personGID"))
			return personGID;
		if(property.equals("teacherID")) {
			if(personGID != null && personGID instanceof EOKeyGlobalID &&
					((EOKeyGlobalID)personGID).entityName().equals(Teacher.entityName))
				return ((EOKeyGlobalID)personGID).keyValues()[0];
		}
		if(property.equals("studentID")) {
			if(personGID != null && personGID instanceof EOKeyGlobalID &&
					((EOKeyGlobalID)personGID).entityName().equals(Student.entityName))
				return ((EOKeyGlobalID)personGID).keyValues()[0];
		}
			
		Object result = (properties == null)?null:properties.valueForKey(property);
		if(result == null && parent != null)
			result = parent.propertyNamed(property);
		return result;
	}
	
	public String toString() {
		return username;
	}
}
