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
import com.webobjects.eoaccess.EOUtilities;
import com.webobjects.eocontrol.EOEditingContext;
import com.webobjects.appserver.WOComponent;
import com.webobjects.appserver.WOActionResults;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSMutableArray;
import net.rujel.eduplan.SubjectGroup;

public class SubjectGroupLine extends WOComponent {
	
	public Object _subjectGroup;
	public Object _selection;
	
	public Object item;
	
    public SubjectGroupLine(WOContext context) {
        super(context);
    }
    
    public boolean synchronizesVariablesWithBindings() {
        return false;
	}
	
	public boolean isStateless() {
		return true;
	}
	
	public void reset() {
		_subjectGroup = null;
		_selection = null;
		item = null;
	}
	
	public SubjectGroup subjectGroup() {
		if(_subjectGroup == null)
			_subjectGroup = (SubjectGroup)valueForBinding("subjectGroup");
		return (SubjectGroup)_subjectGroup;
	}
	public SubjectGroup selection() {
		if(_selection == null) {
			_selection = valueForBinding("selection");
			if(_selection == null)
				_selection = NullValue;
		}
		return (SubjectGroup)((_selection==NullValue)?null:_selection);
	}
	
	public boolean shouldShow() {
		if(selection() == null)
			return true;
		if(subjectGroup() == selection() || selection().path().containsObject(subjectGroup()) 
				|| subjectGroup().parent() == selection().parent())
			return true;
		else
			return false;
	}
	
	public boolean isSelected() {
		if(selection() == null)
			return false;
		return (subjectGroup() == selection());
	}
	
	public boolean showChildren() {
		if(subjectGroup().children() == null || subjectGroup().children().count() == 0)
			return false;
		return (subjectGroup() != selection());
	}
	
	public boolean showNumerator() {
		if(selection() == null)
			return false;
		return (subjectGroup().parent() == selection().parent());
	}
	
	public boolean showSorter() {
		if(selection() == null)
			return false;
		return (selection().parent() == subjectGroup() && subjectGroup().children().count() > 1);
	}

	public WOActionResults select() {
		_selection = subjectGroup();
		setValueForBinding(selection(), "selection");
		return null;
	}

	public NSArray parentList() {
		EOEditingContext ec = subjectGroup().editingContext();
		NSArray found = EOUtilities.objectsForEntityNamed(ec, SubjectGroup.ENTITY_NAME);
		if(found == null || found.count() == 0)
			return null;
		NSMutableArray result = new NSMutableArray(found.count());
		for (int i = 0; i < found.count(); i++) {
			SubjectGroup sg = (SubjectGroup)found.objectAtIndex(i);
			if(sg != subjectGroup())
				sg.addToSortedList(result);
		}
		return result;
	}

	public String selname() {
		return (String)valueForKeyPath("selection.name");
	}

	public void setSelname(String name) {
		SubjectGroup selection = selection();
		if(selection == null)
			return;
		if(name == null) {
			NSArray children = selection.children();
			if(children == null || children.count() == 0)
				selection.editingContext().deleteObject(selection);
		} else {
			selection.setName(name);
		}
	}
}