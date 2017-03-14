// GlobalPlan.java: Class file for WO Component 'SubjectGroupEditor'

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

package net.rujel.eduplan;

import com.webobjects.appserver.WOContext;
import com.webobjects.appserver.WOResponse;
import com.webobjects.eoaccess.EOUtilities;
import com.webobjects.eocontrol.EOEditingContext;
import com.webobjects.eocontrol.EOKeyValueQualifier;
import com.webobjects.eocontrol.EOQualifier;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSDictionary;
import com.webobjects.foundation.NSMutableDictionary;

import net.rujel.reusables.Various;
import net.rujel.reusables.WOLogLevel;

import com.webobjects.appserver.WOComponent;

import net.rujel.base.ReadAccess;
import net.rujel.eduplan.SubjectGroup;

public class SubjectGroupEditor extends WOComponent {
	
	public EOEditingContext ec;
	public NSArray topGroups;
	public Object item;
	public NSArray allGroups;
	public SubjectGroup selection;
	public NSMutableDictionary newDict = new NSMutableDictionary();
	public ReadAccess access;
	
    public SubjectGroupEditor(WOContext context) {
        super(context);
    }

    public boolean synchronizesVariablesWithBindings() {
        return false;
	}
    
	public void appendToResponse(WOResponse aResponse, WOContext aContext) {
		if(Various.boolForObject(valueForBinding("shouldReset"))) {
			ec = (EOEditingContext)context().page().valueForKey("ec");
			newDict.removeAllObjects();
			NSDictionary dict = (NSDictionary)valueForBinding("dict");
			access = (ReadAccess)dict.valueForKey("access");
			allGroups = SubjectGroup.listSubjectGroups(ec);
			EOQualifier qual = new EOKeyValueQualifier(SubjectGroup.PARENT_KEY,
					EOQualifier.QualifierOperatorEqual,NullValue);
			topGroups = EOQualifier.filteredArrayWithQualifier(allGroups, qual);
			setValueForBinding(Boolean.FALSE, "shouldReset");
			selection = null;
		}
		super.appendToResponse(aResponse, aContext);
	}
	
	public void save() {
		setValueForBinding(Boolean.TRUE, "shouldReset");
		if(newDict.count() > 0) {
			String name = (String)newDict.valueForKey(SubjectGroup.NAME_KEY);
			if(name != null) {
				selection = (SubjectGroup)EOUtilities.createAndInsertInstance(ec, 
						SubjectGroup.ENTITY_NAME);
				selection.setName(name);
				name = (String)newDict.valueForKey(SubjectGroup.FULL_NAME_KEY);
				if(name != null)
					selection.setFullName(name);
			}
			Object newParent = newDict.valueForKey("parent");
			if(selection != null && newParent != null) {
				int maxsort = 0;
				if(newParent == NullValue) {
					SubjectGroup parent = selection.parent();
					parent.removeObjectFromBothSidesOfRelationshipWithKey(selection, 
							SubjectGroup.CHILDREN_KEY);
					selection.setParent(null);
					maxsort=(Integer)topGroups.valueForKeyPath("@max.sort");
				} else if(newParent instanceof SubjectGroup) {
					SubjectGroup parent = selection.parent();
					if(parent != null)
						parent.removeObjectFromPropertyWithKey(selection,SubjectGroup.CHILDREN_KEY);
					selection.addObjectToBothSidesOfRelationshipWithKey((SubjectGroup)newParent, 
							SubjectGroup.PARENT_KEY);
					NSArray children = ((SubjectGroup)newParent).children();
					if(children != null && children.count() > 0)
						maxsort=(Integer)children.valueForKeyPath("@max.sort");
				}
				selection.setSort(maxsort + 1);
				selection._path = null;
				selection._padding = null;
			}
		}
		if(!ec.hasChanges())
			return;
	   	ec.lock();
    	try {
			ec.saveChanges();
			EduPlan.logger.log(WOLogLevel.COREDATA_EDITING, "Saved changes in SubjectGroup", 
					new Object[] {session(),selection});
		} catch (Exception e) {
			EduPlan.logger.log(WOLogLevel.INFO,"Error saving changes is SubjectGroup",
					new Object[] {session(),selection,e});
			session().takeValueForKey(e.getMessage(), "message");
//			ec.revert();
		} finally {
			ec.unlock();
			newDict.removeAllObjects();
		}
	}

	public boolean showSorter() {
		if(selection == null)
			return false;
		return (selection.parent() == null);
	}

	public SubjectGroup selectionParent() {
		if(selection == null)
			return null;
		return selection.parent();
	}

	public void setSelectionParent(SubjectGroup newParent) {
		if(selection == null)
			return;
		if(newParent != selection.parent())
			newDict.takeValueForKey((newParent==null)?NullValue:newParent, "parent");
	}

}