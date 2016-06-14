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

import java.util.Enumeration;
import java.util.logging.Logger;

import net.rujel.base.ReadAccess;
import net.rujel.base.SchoolSection;
import net.rujel.reusables.SessionedEditingContext;
import net.rujel.reusables.SettingsReader;
import net.rujel.reusables.WOLogLevel;
import net.rujel.ui.TeacherSelector;

import com.webobjects.appserver.WOActionResults;
import com.webobjects.appserver.WOComponent;
import com.webobjects.appserver.WOContext;
import com.webobjects.appserver.WODisplayGroup;
import com.webobjects.appserver.WOSession;
import com.webobjects.eoaccess.EOUtilities;
import com.webobjects.eocontrol.EOEditingContext;
import com.webobjects.eocontrol.EOEnterpriseObject;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSDictionary;
import com.webobjects.foundation.NSKeyValueCoding;
import com.webobjects.foundation.NSMutableArray;
import com.webobjects.foundation.NSMutableDictionary;

public class ManageUsers extends WOComponent {
	protected static Logger logger = Logger.getLogger("rujel.user");

	public ManageUsers(WOContext aContext) {
		super(aContext);
		populateGroups(aContext.session());
	}
	
	public void populateGroups(WOSession ses) {
		NSDictionary locale = (NSDictionary)ses.valueForKeyPath(
				"strings.RujelUsers_UserStrings.accessGroups");
		NSMutableArray array = ((NSArray)locale.valueForKey("array")).mutableClone();
		if(mask)
			array.insertObjectAtIndex("@", 0);
		Enumeration enu = array.objectEnumerator();
		accGroups = new NSMutableArray();
		while (enu.hasMoreElements()) {
			String grId = (String) enu.nextElement();
			NSMutableDictionary dict = new NSMutableDictionary(grId,"groupName");
			dict.takeValueForKey(locale.valueForKey(grId), "title");
			accGroups.addObject(dict);
		}
		if(ec == null)
			ec = new SessionedEditingContext(ses);
		NSArray found = EOUtilities.objectsForEntityNamed(ec, "UserGroup");
		if(found == null)
			return;
		enu = found.objectEnumerator();
		while (enu.hasMoreElements()) {
			EOEnterpriseObject gr = (EOEnterpriseObject) enu.nextElement();
			String name = (String)gr.valueForKey("groupName");
			int idx = array.indexOf(name);
			NSMutableDictionary dict = null;
			if(idx < 0) {
				dict = new NSMutableDictionary(name,"groupName");
				dict.takeValueForKey(locale.valueForKey(name), "title");
				accGroups.addObject(dict);
				array.addObject(name);
			} else {
				dict = (NSMutableDictionary)accGroups.objectAtIndex(idx);
			}
			SchoolSection section = (SchoolSection)gr.valueForKey("section");
			if(section == null)
				dict.takeValueForKey(gr, "global");
			else
				dict.setObjectForKey(gr, section.sectionID());
		}
	}
	
	protected EOEditingContext ec;
	public WODisplayGroup usersList;
	public NSArray sections;
	public NSMutableArray accGroups;
	public AutUser userItem;
	public EOEnterpriseObject item;
	public NSKeyValueCoding item2;
	public NSKeyValueCoding currGroup;
	public String parentHandler = SettingsReader.stringForKeyPath("auth.parentLoginHandler", null);
	public String passw1;
	public String passw2;
	protected boolean readFromParent = parentHandler != null && 
		SettingsReader.boolForKeyPath("auth.readFromParent", false);
	public boolean mask = SettingsReader.boolForKeyPath("auth.maskGlobalAccess", false);
	
	public EOEditingContext _ec() {
		if(ec == null)
			ec = new SessionedEditingContext(session());
		return ec;
	}
	
	public WOActionResults saveGroup() {
		if(currGroup == null)
			return null;
		EOEnterpriseObject group = (EOEnterpriseObject)currGroup.valueForKey("group");
		NSMutableDictionary row = null;
		SchoolSection section = (SchoolSection)currGroup.valueForKey("section");
		if(group == null) {
			group = EOUtilities.createAndInsertInstance(ec, "UserGroup");
			group.takeValueForKey(currGroup.valueForKey("groupName"), "groupName");
			group.takeValueForKey(section, "section");
			row = (NSMutableDictionary)currGroup.valueForKey("row");
		}
		group.takeValueForKey(currGroup.valueForKey("externalEquivalent"), "externalEquivalent");
		try {
			ec.saveChanges();
			logger.log(WOLogLevel.CONFIG,"Saved group changes",group);
			if(row != null) {
				if(section == null)
					row.setObjectForKey(group, "global");
				else
					row.setObjectForKey(group, section.sectionID());
			}
		} catch (Exception e) {
			logger.log(WOLogLevel.WARNING,"Error saving group changes",
					new Object[] {session(),group,e});
			session().takeValueForKey(e.getMessage(), "message");
		}
		return null;
	}
	
	public WOActionResults submit() {
		if(usersList.selectedObject() == null)
			return search();
		else
			return save();
	}
	
	public WOActionResults search() {
		String query = context().request().stringFormValueForKey("userName");
		usersList.queryMatch().takeValueForKey(query, "userName");
		usersList.qualifyDisplayGroup();
		usersList.setCurrentBatchIndex(0);
		return null;
	}
	
	public WOActionResults save() {
		boolean match = true;
		try {/*
			if(groupsList.selectedObject() != null) {
				list = groupsList;
			} else*/ if(usersList.selectedObject() != null) {
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
				TableLoginHandler.flush();
			} else {
				return null;
			}
			ec.saveChanges();
			logger.log(WOLogLevel.EDITING,"User changes saved",
					new Object[] {session(),usersList.selectedObject()});
			if(match) {
				usersList.clearSelection();
				passw1 = null;
				passw2 = null;
			}
		} catch (Exception e) {
			logger.log(WOLogLevel.WARNING,"Error changing User ",
					new Object[] {session(),usersList.selectedObject(),e});
			ec.revert();
			populateGroups(session());
			session().takeValueForKey(e.getMessage(), "message");
		}
		return null;
	}
	
	public WOActionResults delete() {
		WODisplayGroup list = null;
		/*
		if(groupsList.selectedObject() != null) {
			list = groupsList;
			logger.log(WOLogLevel.EDITING,"Deleting group " + NSKeyValueCoding.Utility.
					valueForKey(list.selectedObject(), "groupName"),
					new Object[] {session(),list.selectedObject()});
		} else */
		if(usersList.selectedObject() != null) {
			list = usersList;
			logger.log(WOLogLevel.EDITING,"Deleting user " + NSKeyValueCoding.Utility.
					valueForKey(list.selectedObject(), AutUser.USER_NAME_KEY),
					new Object[] {session(),list.selectedObject()});
			TableLoginHandler.flush();
		} else {
			return null;
		}
		try {
			list.delete();
			ec.saveChanges();
			passw1 = null;
			passw2 = null;
			list.clearSelection();
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
		if(usersList.selectedObject() == userItem) {
			usersList.clearSelection();
			passw1 = null;
			passw2 = null;
		} else {
			usersList.selectObject(userItem);
			passw1 = (userItem.credential() == null)? null :"password";
			passw2 = (userItem.hasParent())? "parent" : null;
		}
		currGroup = null;
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
//		selector.takeValueForKeyPath(valueForKeyPath("usersList.selectedObject.present")
//				, "dict.presenterBindings.selection");
		return selector;
	}

	public WOActionResults addGroup() {
		if(passw2 == null)
			return null;
		item2 = new NSMutableDictionary(passw2,"groupName");
		NSDictionary locale = (NSDictionary)session().valueForKeyPath(
				"strings.RujelUsers_UserStrings.accessGroups");
		item2.takeValueForKey(locale.valueForKey(passw2), "title");
		accGroups.addObject(item2);
		selectGroup();
		return null;
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
	
	public NSArray sections() {
		if(sections != null)
			return sections;
		sections = SchoolSection.listSections(ec, true);
		return sections;
	}
	
	protected EOEnterpriseObject group() {
		if(item2 == null)
			return null;
		Object section = (item == null)?"global":item.valueForKey("sectionID");
		return (EOEnterpriseObject)((NSMutableDictionary)item2).objectForKey(section);
	}
	
	public boolean isInGroup() {
		EOEnterpriseObject group = group();
		EOEnterpriseObject global = (EOEnterpriseObject)item2.valueForKey("global");
		if(group == null && global == null)
			return false;
		NSArray groups = (NSArray)valueForKeyPath("usersList.selectedObject.groups");
		if(groups == null || groups.count() == 0)
			return false;
		if (group != null && groups.containsObject(group))
			return true;
		if(global != null && groups.containsObject(global)) {
			if(mask) {
				NSMutableDictionary maskRow = (NSMutableDictionary)accGroups.objectAtIndex(0);
				if(item2 == maskRow && item != null)
					return true;
				Object key = maskRow.valueForKey("global");
				if(key != null && groups.containsObject(key))
					return true;
				if(item != null) {
					key = item.valueForKey("sectionID");
					key = maskRow.objectForKey(key);
					return (key != null && groups.containsObject(key));
				}
			} else {
				return true;
			}
		}
		return false;
	}
	
	public Boolean cantEditGroup() {
		if(usersList.selectedObject() != null)
			return Boolean.TRUE;
		return (Boolean)session().valueForKeyPath("readAccess._edit.UserGroup");
	}
	
	public Boolean disableGroup() {
		EOEnterpriseObject group = group();
		AutUser au = (AutUser)usersList.selectedObject();
		if(readFromParent && au.hasParent() &&
				(group != null && group.valueForKey("externalEquivalent") != null))
			return Boolean.TRUE;
		ReadAccess readAccess = (ReadAccess)session().valueForKey("readAccess");
		Integer section = (Integer)valueForKeyPath("item.sectionID");
		if(!readAccess.cachedAccessForObject("ManageUsers@" + 
				item2.valueForKey("groupName"), section).flagForKey("edit"))
			return Boolean.TRUE;
		if(item != null) {
			EOEnterpriseObject global = (EOEnterpriseObject)item2.valueForKey("global");
			if(au.isInGroup(global)) {
				if(mask && !"@".equals(global.valueForKey("groupName"))) {
					NSMutableDictionary maskRow = (NSMutableDictionary)accGroups.objectAtIndex(0);
					Object key = maskRow.objectForKey("global");
					if(key != null && au.isInGroup((EOEnterpriseObject)key))
						return Boolean.TRUE;
					key = maskRow.objectForKey(section);
					if(key == null || !au.isInGroup((EOEnterpriseObject)key))
						return Boolean.FALSE;
				}
				return Boolean.TRUE;
			}
		}
		return Boolean.FALSE;
				//(Boolean)session().valueForKeyPath("readAccess._edit.ManageUsers.@" + 
				//		item2.valueForKey("groupName"));
	}
	
	public void setIsInGroup(boolean is) {
		AutUser au = (AutUser)usersList.selectedObject();
		EOEnterpriseObject group = group();
		EOEnterpriseObject global = (EOEnterpriseObject)item2.valueForKey("global");
		if(is && au.isInGroup(global))
			return;
		if(is && group == null) {
			group = EOUtilities.createAndInsertInstance(ec, "UserGroup");
			group.takeValueForKey(item2.valueForKey("groupName"), "groupName");
			if(item != null) {
//				Object section = item.valueForKey(IndexRow.IDX_KEY);
				group.takeValueForKey(item, "section");
				((NSMutableDictionary)item2).setObjectForKey(group, item.valueForKey("sectionID"));
			} else {
				item2.takeValueForKey(group, "global");
			}
		}
		if(is) {
			if(!au.isInGroup(group))
				au.addObjectToBothSidesOfRelationshipWithKey(group,AutUser.GROUPS_KEY);
		} else {
			if(au.isInGroup(group))
				au.removeObjectFromBothSidesOfRelationshipWithKey(group,AutUser.GROUPS_KEY);
		}
	}
	
	public WOActionResults selectGroup() {
		if(ec.hasChanges()) {
			ec.revert();
			valueForKeyPath("usersList.selectedObject.flushPlink");
		}
		if(item2 == null)
			return null;
		currGroup = new NSMutableDictionary();
		currGroup.takeValueForKey(item2.valueForKey("groupName"), "groupName");
		currGroup.takeValueForKey(item2.valueForKey("title"), "title");
		currGroup.takeValueForKey(item2, "row");
		if(item != null) {
			Object sectionID = item.valueForKey("sectionID");
			currGroup.takeValueForKey(item, "section");
			currGroup.takeValueForKey(item.valueForKey(SchoolSection.NAME_KEY), "sectionName");
			if(currGroup.valueForKey("sectionName") == null && sectionID != null)
				currGroup.takeValueForKey(sectionID.toString(), "sectionName");
		} else {
			currGroup.takeValueForKey("...", "sectionName");
		}
		EOEnterpriseObject group = group();
		if(group != null) {
			currGroup.takeValueForKey(group, "group");
			currGroup.takeValueForKey(
					group.valueForKey("externalEquivalent"), "externalEquivalent");
		}
		usersList.clearSelection();
		passw1 = null;
		passw2 = null;
		return null;
	}
	
	public String groupClass() {
		if(currGroup != null && currGroup.valueForKey("row") == item2 && 
				currGroup.valueForKey("section") == item)
			return "selection";
		EOEnterpriseObject group = group();
		if(mask && "@".equals(item2.valueForKey("groupName"))) {
			if(group!= null && group.valueForKey("externalEquivalent") != null)
				return "highlight";
			return "highlight2";
		}
		if(group == null)
			return "grey";
		if(!readFromParent)
			return null;
		if(group.valueForKey("externalEquivalent") != null)
			return "gerade";
		return "ungerade";
	}
	/*
	public Boolean noAddGroup() {
		if(usersList.selectedObject() != null)
			return Boolean.TRUE;
		return (Boolean)session().valueForKeyPath("readAccess._create.UserGroup");
	}*/

	public Boolean showBatch() {
		return Boolean.valueOf(usersList.batchCount() > 1);
	}

	public boolean showSelector() {
		return usersList.allObjects().count() > usersList.numberOfObjectsPerBatch();
	}
	
	public boolean synchronizesVariablesWithBindings() {
        return false;
	}
	
	public boolean isStateless() {
		return false;
	}

}
