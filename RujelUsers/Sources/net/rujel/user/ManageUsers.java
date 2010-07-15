// ManageUsers.java: Class file for WO Component 'ManageUsers'

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

import java.util.logging.Logger;

import net.rujel.reusables.SessionedEditingContext;
import net.rujel.reusables.SettingsReader;
import net.rujel.reusables.WOLogLevel;
import net.rujel.ui.TeacherSelector;

import com.webobjects.appserver.WOActionResults;
import com.webobjects.appserver.WOComponent;
import com.webobjects.appserver.WOContext;
import com.webobjects.appserver.WODisplayGroup;
import com.webobjects.eocontrol.EOEditingContext;
import com.webobjects.eocontrol.EOEnterpriseObject;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSKeyValueCoding;

public class ManageUsers extends WOComponent {
	protected static Logger logger = Logger.getLogger("rujel.user");

	public ManageUsers(WOContext aContext) {
		super(aContext);
	}
	
	protected EOEditingContext ec;
	public WODisplayGroup usersList;
	public WODisplayGroup groupsList;
	public AutUser userItem;
	public EOEnterpriseObject item;
	public String parentHandler = SettingsReader.stringForKeyPath(
			"auth.parentLoginHandler", null);
	public String passw1;
	public String passw2;
	
	public EOEditingContext _ec() {
		if(ec == null)
			ec = new SessionedEditingContext(session());
		return ec;
	}
	
	public WOActionResults save() {
		boolean match = true;
		WODisplayGroup list = null;
		try {
			if(groupsList.selectedObject() != null) {
				list = groupsList;
			} else if(usersList.selectedObject() != null) {
				list = usersList;
				if(passw1 == passw2 || passw1 != null && !passw1.equals("password")) {
					match = (passw1 == null || passw1.equals(passw2));
					if(match)
						takeValueForKeyPath(passw1, "usersList.selectedObject.password");
					else
						session().takeValueForKey(session().valueForKeyPath(
								"strings.RujelUsers_UserStrings.messages.noMatch"), "message");
				} else if(passw2 != null && passw2.equals("parent")) {
					match = (valueForKeyPath("usersList.selectedObject.credential") != null);
					if(!match) {
						session().takeValueForKey(session().valueForKeyPath(
						"strings.RujelUsers_UserStrings.messages.enterPassword"), "message");
						passw1 = null;
						passw2 = null;
					}
				}
			} else {
				return null;
			}
			ec.saveChanges();
			logger.log(WOLogLevel.UNOWNED_EDITING,"User changes saved",
					new Object[] {session(),list.selectedObject()});
			if(match)
				list.clearSelection();
		} catch (Exception e) {
			logger.log(WOLogLevel.WARNING,"Error changing User ",
					new Object[] {session(),list.selectedObject(),e});
			session().takeValueForKey(e.getMessage(), "message");
		}
		return null;
	}
	
	public WOActionResults delete() {
		WODisplayGroup list = null;
		if(groupsList.selectedObject() != null) {
			list = groupsList;
			logger.log(WOLogLevel.UNOWNED_EDITING,"Deleting group " + NSKeyValueCoding.Utility.
					valueForKey(list.selectedObject(), "groupName"),
					new Object[] {session(),list.selectedObject()});
		} else if(usersList.selectedObject() != null) {
			list = usersList;
			logger.log(WOLogLevel.UNOWNED_EDITING,"Deleting user " + NSKeyValueCoding.Utility.
					valueForKey(list.selectedObject(), AutUser.USER_NAME_KEY),
					new Object[] {session(),list.selectedObject()});
		} else {
			return null;
		}
		try {
			list.delete();
			ec.saveChanges();
		} catch (Exception e) {
			logger.log(WOLogLevel.WARNING,"Error deleting",
					new Object[] {session(),list.selectedObject(),e});
			session().takeValueForKey(e.getMessage(), "message");
		}
		return null;
	}
	
	public WOActionResults selectUser() {
		if(ec.hasChanges()) {
			ec.revert();
			valueForKeyPath("usersList.selectedObject.flushPlink");
		}
		usersList.selectObject(userItem);
		passw1 = (userItem.credential() == null)? null :"password";
		passw2 = (userItem.hasParent())? "parent" : null;
		groupsList.clearSelection();
		return null;
	}
	
	public boolean userIsSelected() {
		return (userItem == usersList.selectedObject()); 
	}
	
	public WOActionResults chooseUserPerson() {
		WOComponent selector = TeacherSelector.selectorPopup(context().page(),usersList,
				"selectedObject.personLink", ec);
		selector.takeValueForKeyPath(Boolean.TRUE, "dict.presenterBindings.hideVacant");
		selector.takeValueForKeyPath(Boolean.TRUE, "dict.presenterBindings.allowDelete");
		return selector;
	}

	public WOActionResults attachParent() {
		AutUser au = (AutUser)usersList.selectedObject();
		if(au != null) {
			au.setCredential(au.userName());
			passw2 = "parent";
			passw1 = null;
		}
		return null;
	}
	
	public String userClass() {
		if(userItem == null)
			return "grey";
		if(userItem == usersList.selectedObject())
			return "selection";
		if(userItem.hasParent())
			return "gerade";
		return "ungerade";
	}
	
	public boolean isInGroup() {
		if(item == null) return false;
		NSArray groups = (NSArray)valueForKeyPath("usersList.selectedObject.groups");
		if(groups == null) return false;
		return groups.containsObject(item);
	}
	
	public void setIsInGroup(boolean is) {
		AutUser au = (AutUser)usersList.selectedObject();
		if(item == null || au == null)
			return;
		if(is) {
			if(!au.groups().containsObject(item))
				au.addObjectToBothSidesOfRelationshipWithKey(
						(EOEnterpriseObject)item,AutUser.GROUPS_KEY);
		} else {
			if(au.groups().containsObject(item))
				au.removeObjectFromBothSidesOfRelationshipWithKey(
						(EOEnterpriseObject)item,AutUser.GROUPS_KEY);
		}
	}
	
	public WOActionResults selectGroup() {
		if(ec.hasChanges()) {
			ec.revert();
			valueForKeyPath("usersList.selectedObject.flushPlink");
		}
		groupsList.selectObject(item);
		usersList.clearSelection();
		return null;
	}
	
	public String groupClass() {
		if(item == null)
			return "grey";
		if(item == groupsList.selectedObject())
			return "selection";
		if(item.valueForKey("externalEquivalent") != null)
			return "gerade";
		return "ungerade";
	}
	
	public Boolean noAddGroup() {
		if(usersList.selectedObject() != null || groupsList.selectedObject() != null)
			return Boolean.TRUE;
		return (Boolean)session().valueForKeyPath("readAccess._create.UserGroup");
	}

	public Boolean noBatch() {
		if(usersList.selectedObject() != null || groupsList.selectedObject() != null)
			return Boolean.TRUE;
		return Boolean.valueOf(usersList.batchCount() < 2);
	}

	public boolean synchronizesVariablesWithBindings() {
        return false;
	}
	
	public boolean isStateless() {
		return false;
	}

}
