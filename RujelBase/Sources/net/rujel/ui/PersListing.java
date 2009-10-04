// PersListing.java: Class file for WO Component 'PersListing'

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

package net.rujel.ui;

import java.util.Enumeration;

import net.rujel.interfaces.*;
import net.rujel.reusables.*;
import net.rujel.auth.UserPresentation;
import net.rujel.auth.AccessHandler;

import com.webobjects.foundation.*;
import com.webobjects.appserver.*;
import com.webobjects.eocontrol.*;
import com.webobjects.eoaccess.*;

public class PersListing extends WOComponent {
	private EOEditingContext ec = session().defaultEditingContext();
	private int firstNameDisplay = 2;
	private int secondNameDisplay = 2;
//	protected String entity;
	protected NamedFlags access;
	public static final NSArray accessKeys = new NSArray(new Object[] {
		"search", "create", "edit","delete"});
	private NSArray found;
//	protected NSMutableDictionary actions = new NSMutableDictionary();
	
	public PersonLink selection;
	public Person item;
    public String searchString;
    public String searchMessage;
    public boolean canCreate = false;
//	public NSMutableDictionary newPerson;
//	private Person onEdit;
    public int index;
/*	public String lastName;
	public String firstName;
	public String secondName;
    public Boolean sex; */

	public void setItem(PersonLink newItem) {
		if(newItem == null) item = null;
		else item = newItem.person();
	}
	
    public PersListing(WOContext context) {
        super(context);		
    }
	
	public NamedFlags access() {
		if(access == null) {
			UserPresentation user = (UserPresentation)session().valueForKey("user");
			try {
//				entity = (String)valueForBinding("entity");
				access = new ImmutableNamedFlags(user.accessLevel(entity()),accessKeys);
			} catch (AccessHandler.UnlistedModuleException e1) {
				try {
					access = new ImmutableNamedFlags(user.accessLevel("Person"),accessKeys);
				} catch (AccessHandler.UnlistedModuleException e2) {
					try {
						access = new ImmutableNamedFlags(user.accessLevel("PersListing"),accessKeys);
					} catch (AccessHandler.UnlistedModuleException e3) {
						access = new ImmutableNamedFlags(1,accessKeys);;
					}
				}
			}
		}
		return access;
	}
	
	public Boolean canEdit() {
		if(Various.boolForObject(valueForBinding("noEdit")))
			return Boolean.FALSE;
		return (Boolean)session().valueForKeyPath("readAccess.edit." +
				((item==null)?entity():"item"));
	}
	
	public boolean synchronizesVariablesWithBindings() {
        return false;
	}
	
	public void appendToResponse(WOResponse aResponse, WOContext aContext) {
		try {
			Number b = (Number)valueForBinding("firstNameDisplay");
			if(b != null)
				firstNameDisplay = b.intValue();
			else
				firstNameDisplay = 2;
			
			b = (Number)valueForBinding("secondNameDisplay");
			if(b != null)
				secondNameDisplay = b.intValue();
			else
				secondNameDisplay = 2;
		
		} catch (ClassCastException cex) {
			throw new IllegalArgumentException("NameDisplay bindings should be integer");
		}
		String request = (String)valueForBinding("searchRequest");
		if(request != null) {
			performSearchRequest(request);
		} else {
			selection = (PersonLink) EOUtilities.localInstanceOfObject (ec,
					(EOEnterpriseObject)valueForBinding("selection"));
//			syncSelection();
		}
		if(selection == null && Various.boolForObject(valueForBinding("showPopup"))) {
			selection = defaultSelectionValue();
			setValueForBinding(selection, "selection");
		}
		super.appendToResponse(aResponse,aContext);
		found = null;
//		searchMessage = null;
	}
	
	public void takeValuesFromRequest(WORequest aRequest,WOContext aContext) {
		super.takeValuesFromRequest(aRequest,aContext);
		if(!Various.boolForObject(valueForBinding("showPopup"))) {
			selection = (PersonLink) EOUtilities.localInstanceOfObject (ec,
					(EOEnterpriseObject)valueForBinding("selection"));
//			syncSelection();
		}
	}
	
	protected void performSearchRequest(String request) {
		searchString = request;
		canCreate = false;
		search();
		while (found == null || found.count() == 0) {
			searchString = searchString.substring(0,searchString.length() - 1);
			if(searchString.length() < 2) {
				searchString = request;
				setValueForBinding(null,"searchRequest");
				return;
			}
			search();
		}
		selection = (PersonLink)found.objectAtIndex(0);
		setValueForBinding(selection,"selection");
		
		setValueForBinding(Person.Utility.fullName(selection,true,2,2,2),"searchRequest");
		valueForBinding("selectAction");
		
	}
	
/*	protected void syncSelection () {
		EOEnterpriseObject sel = EOUtilities.localInstanceOfObject (ec,
				(EOEnterpriseObject)valueForBinding("selection"));
		if(sel == null || sel instanceof Person)
			selection = (Person)sel;
		else {
			if (sel instanceof PersonLink)
				selection = ((PersonLink)sel).person();
			else
				throw new IllegalArgumentException(
						"Selection binding should be of class Person or PersonLink");
		}
	}*/

	public void search() {
		EOQualifier qual = Person.Utility.fullNameQualifier(searchString);
		NSKeyValueCodingAdditions strings = (NSKeyValueCodingAdditions)
						session().valueForKey("strings");
		if(qual == null) {
			String noMore = (String)strings.valueForKeyPath(
					"RujelBase_Base.notMoreXwords");
			StringBuilder buf = new StringBuilder();
			buf.append(strings.valueForKeyPath("Strings.messages.illegalFormat"));
			buf.append(' ');
			buf.append(strings.valueForKeyPath("Reusables_Strings.dataTypes.ofRequest"));
			buf.append(' ');
			buf.append(String.format(noMore,3));
			searchMessage = buf.toString();
			canCreate = false;
			return;
		}
		found = Person.Utility.search(ec, qual, entity());
		canCreate = (access != null && access.flagForKey("create"));
		if(found.count() < 1) {
			searchMessage = (String)session().valueForKeyPath(
					"strings.Strings.messages.nothingFound");
			return;
		}
		NSMutableArray fullList = (NSMutableArray)session().valueForKey("personList");
		NSMutableArray tmp = found.mutableClone();
		tmp.removeObjectsInArray(fullList);
		fullList.addObjectsFromArray(tmp);
		searchMessage = null;
	}
	
	public String style() {
		//Boolean useStyles = (Boolean)valueForBinding("useStyles");
		if(Various.boolForObject(valueForBinding("useStyles"))) return null;
		//(useStyles != null && !useStyles.booleanValue())
		
		if(selection != null && item.equals(selection.person())) return "selection";
		if(item.sex() == null) return "grey";
		
		if(found != null && found.containsObject(item)) {
			if (item.sex().booleanValue())
				return "foundMale";
			else
				return "foundFemale";
		} else {
			if (item.sex().booleanValue())
				return "male";
			else
				return "female";
		}
	}
	
	public String itemName() {
		return Person.Utility.composeName(item,firstNameDisplay,secondNameDisplay);
	}
	public String itemFullName() {
		if ((firstNameDisplay > 1) && (secondNameDisplay > 1)) return null;
		else return Person.Utility.fullName(item,true,2,2,2);
	}
	
	public WOActionResults select() {
/*		if(onEdit != null) {
			if(session().valueForKey("message") != null)
				return null;
			onEdit = null;
		}*/
		selection = (PersonLink)personList().objectAtIndex(index);
		canCreate = false;
		searchString = null;
        setValueForBinding(selection,"selection");
		return (WOActionResults)valueForBinding("selectAction");
    }
		
	public String onClick() {
		if(hasBinding("onClick"))
			return (String)valueForBinding("onClick");
		return (String)session().valueForKey("tryLoad");
	}
	/*
	public WOActionResults invokeAction(WORequest aRequest,WOContext aContext) {
		Object p = actions.objectForKey(context().senderID());
		if(p != null) {
			selection = (Person)p;
			canCreate = false;
			onEdit = null;
			searchString = null;
			setValueForBinding(selection,"selection");
			return (WOActionResults)valueForBinding("selectAction");
		}
		return super.invokeAction(aRequest,aContext);
	} */
	
	public void setSearchString(String newSearchString) {
		if(newSearchString == null || !newSearchString.equals(searchString))
			canCreate = false;
		searchString = newSearchString;
	}
/*	
    public WOComponent edit() {
		selection = null;
		setValueForBinding(selection,"selection");
		onEdit = item;
        return null;
    }
	
	public boolean onEdit() {
		return (item.equals(onEdit));
	}
*/	
	protected String _entity;
	protected String entity() {
		if(_entity != null)
			return _entity;
		String entityName = (String)valueForBinding("entity");
		if(entityName == null)
			return null;
		try {
			EOEntity entity = EOUtilities.entityNamed(ec,entityName);
			if(entity != null) {
				_entity = entityName;
				return entityName;
			}
		} catch (EOObjectNotAvailableException naex) {
			entityName = SettingsReader.stringForKeyPath("interfaces." + entityName,null);
			//interfaces.get(entityName,null);
			int dot = entityName.lastIndexOf('.');
			if(dot > 0) {
				entityName = entityName.substring(dot + 1);
			}
			if(entityName == null)
				return null;
			try {
				EOEntity entity = EOUtilities.entityNamed(ec,entityName);
				if(entity != null) {
					_entity = entityName;
					return entityName;
				}
			} catch (EOObjectNotAvailableException naex2) {
				return null;
			}
		}
		return _entity;
	}
	
    public WOComponent create() {
//		searchMessage = null;
//		canCreate = false;
		selection = null;
		setValueForBinding(selection,"selection");
//		newPerson = new NSMutableDictionary();
//		newPerson.takeValueForKey(searchString,"lastName");
		Person onEdit = (Person)EOUtilities.createAndInsertInstance(ec,entity());
		String[] names = Person.Utility.splitNames(searchString);
		if(names != null) {
			if(names.length > 0)
				onEdit.setLastName(names[0]);
			if(names.length > 1)
				onEdit.setFirstName(names[1]);
			if(names.length > 2)
				onEdit.setSecondName(names[2]);
		}
//		NSMutableArray fullList = (NSMutableArray)session().valueForKey("personList");
//		fullList.addObject(onEdit);
		WOComponent returnPage = context().page();
		WOComponent popup = pageWithName("SelectorPopup");
		if(returnPage instanceof SelectorPopup) {
			SelectorPopup sp = (SelectorPopup)returnPage;
			popup.takeValueForKey(sp.returnPage, "returnPage");
			popup.takeValueForKey(sp.resultPath, "resultPath");
			popup.takeValueForKey(sp.resultGetter, "resultGetter");
			popup.reset();
		} else {
			popup.takeValueForKey(returnPage, "returnPage");
		}
		NSDictionary dict = (NSDictionary) session().valueForKeyPath(
					"strings.RujelBase_Base.newPerson");
//		dict = PlistReader.cloneDictionary(dict, true);
//		dict.takeValueForKeyPath(null, "presenterBindings.")
		popup.takeValueForKey(onEdit, "value");
		popup.takeValueForKey(dict, "dict");
		return popup;
    }

    public void undo() {
		searchMessage = null;
        canCreate = false;
    }
	
	public void cancel() {
		ec.revert();
		//NSDictionary snapshot = ec.committedSnapshotForObject(onEdit);
		if (ec.insertedObjects().contains(item)) {
			NSMutableArray fullList = (NSMutableArray)session().valueForKey("personList");
			fullList.removeObject(item);
		}
//		onEdit = null;
	}

	public void delete() {
		NSMutableArray fullList = (NSMutableArray)session().valueForKey("personList");
		fullList.removeObject(item);
//		onEdit = null;
	}
	
	public String act() {
		if(Various.boolForObject(valueForBinding("useAjaxPost"))) {
			String href = context().componentActionURL();
			String result = "ajaxPopupAction('" + href + "');";
			return result;
		}
		return (String)session().valueForKey("tryLoad");
	}

	public String onSubmit() {
		if(Various.boolForObject(valueForBinding("useAjaxPost")))
			return "return ajaxPost(this);";
		return "return tryLoad(true);";
	}
	
	public NSArray personList() {
		NSArray forcedList = (NSArray)valueForBinding("forcedList");
		NSMutableArray result = (forcedList == null)?new NSMutableArray():
			EOUtilities.localInstancesOfObjects(ec,forcedList).mutableClone();
		NSArray personList = (NSArray)session().valueForKey("personList");
		if(personList != null && personList.count() > 0) {
			Enumeration enu = personList.objectEnumerator();
			while (enu.hasMoreElements()) {
				EOEnterpriseObject pers = (EOEnterpriseObject) enu.nextElement();
				if(entity().equals(pers.entityName()) && !result.contains(pers))
					result.addObject(pers);
			}
		}
		return result;
	}
	
	public boolean listIsEmpty() {
		NSArray list = found;
		if(list != null && list.count() > 0)
			return false;
		list = (NSArray)valueForBinding("forcedList");
		if(list != null && list.count() > 0)
			return false;
		list = personList();
		if(list != null && list.count() > 0)
			return false;
		return true;
	}
	
	public String displayString() {
		return item.lastName() + ' ' + itemName();
	}

	public String onChange() {
		StringBuffer buf = new StringBuffer("if(this.selectedIndex==0){");
		/*String message = (String)strings.valueForKeyPath("specific.searchMessage");
		buf.append("q=prompt('").append(message);
		buf.append("');if(q==null)return false;else form.elements[0].value=q;}");*/
		buf.append("document.getElementById('searchArea').style.display='block';}else{");
		if(Various.boolForObject(valueForBinding("useAjaxPost")))
			buf.append("ajaxPost(this.form);");
		else
			buf.append("form.submit();");
		buf.append('}');
		return buf.toString();
	}

	protected PersonLink defaultSelectionValue() {
		NSArray list = (NSArray)valueForBinding("forcedList");
		if(list != null && list.count() > 0) {
			return (PersonLink) EOUtilities.localInstanceOfObject(
					ec,(EOEnterpriseObject)list.objectAtIndex(0));
		}
		list = (NSArray)session().valueForKey("personList");
		if(list != null && list.count() > 0) {
			Enumeration enu = list.objectEnumerator();
			while (enu.hasMoreElements()) {
				EOEnterpriseObject pers = (EOEnterpriseObject) enu.nextElement();
				if(entity().equals(pers.entityName())) {
					return (PersonLink)pers;
				}
			}
		}
		return null;
	}
	
	public WOActionResults submit() {
		if(selection == null) {
			search();
			if(found != null && found.count() > 0) {
				selection = (PersonLink)found.objectAtIndex(0);
			} else {
				selection = defaultSelectionValue();
			}
		}
		setValueForBinding(selection, "selection");
		return (WOActionResults)valueForBinding("selectAction");
	}
	
	public WOActionResults invokeAction(WORequest aRequest, WOContext aContext) {
		WOActionResults result = super.invokeAction(aRequest, aContext);
		return result;
	}
}
