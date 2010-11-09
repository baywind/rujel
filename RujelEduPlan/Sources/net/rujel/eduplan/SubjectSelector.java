//  SubjectSelector.java: Class file for WO Component 'SubjectSelector'

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

package net.rujel.eduplan;

import net.rujel.base.MyUtility;

import com.webobjects.appserver.WOActionResults;
import com.webobjects.appserver.WOContext;
import com.webobjects.appserver.WOComponent;
import com.webobjects.eocontrol.EOEditingContext;
import com.webobjects.eocontrol.EOEnterpriseObject;
import com.webobjects.eocontrol.EOFetchSpecification;
import com.webobjects.eocontrol.EOKeyValueQualifier;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSDictionary;
import com.webobjects.foundation.NSMutableDictionary;

public class SubjectSelector extends WOComponent {
    public SubjectSelector(WOContext context) {
        super(context);
    }
    
    protected EOEnterpriseObject _currArea;
    protected EOEnterpriseObject _selection;
    protected NSArray _subjects;
    protected NSArray _areas;
    public EOEnterpriseObject item;
    
    protected EOEnterpriseObject selection() {
    	if(_selection == null) {
    		NSMutableDictionary dict = (NSMutableDictionary)valueForBinding("currDict");
    		if(dict != null) {
    			EOKeyValueQualifier qual = (EOKeyValueQualifier)dict.valueForKey("qualifier");
    			if(qual == null)
    				return null;
    			Object value = qual.value();
    			if(value instanceof PlanCycle)
    				_selection = ((PlanCycle)value).subjectEO();
    			else if (value instanceof EOEnterpriseObject)
    				_selection = (EOEnterpriseObject)value;
    		} else {
    			_selection = (EOEnterpriseObject)valueForBinding("selection");
    		}
    	}
    	return _selection;
    }
    
    public EOEnterpriseObject area() {
    	if(_currArea != null)
    		return _currArea;
    	EOEnterpriseObject selection = selection();
    	if(selection == null)
    		return null;
    	if(selection instanceof Subject)
    		return ((Subject)selection).area();
    	if(selection.entityName().equals("SubjectArea"))
    		return selection;
    	return null;
    }
    
    public WOActionResults openArea() {
    	_currArea = item;
    	_subjects = Subject.subjectsForArea(item);
    	return null;
    }
    
    public NSArray subjects() {
    	if(_subjects == null)
    		_subjects = Subject.subjectsForArea(area());
    	return _subjects;
    }
    
    public NSArray areas() {
    	if(_areas == null) {
    		EOEditingContext ec = (EOEditingContext)valueForBinding("ec");
    		if(ec == null)
    			ec = (EOEditingContext)valueForBinding("editingContext");
        	EOFetchSpecification fs = new EOFetchSpecification(
        			"SubjectArea",null,MyUtility.numSorter);
        	_areas = ec.objectsWithFetchSpecification(fs);
    	}
    	return _areas;
    }
    
    public Subject selectedSubject() {
    	if(selection() instanceof Subject)
    		return (Subject)selection();
    	return null;
    }
    
    public String noneClass() {
    	EOEnterpriseObject selection = selection();
    	if(selection != null && selection.entityName().equals("SubjectArea") &&
    			(_currArea == null || _currArea == selection))
    		return "selection";
    	return "grey";
    }
    
    public WOActionResults selectArea() {
		_selection = area();
    	if(canSetValueForBinding("currDict")) {
    		NSMutableDictionary dict = ((NSDictionary)session().valueForKeyPath(
    				"strings.RujelEduPlan_EduPlan.settingQualifiers.area")).mutableClone();
    		dict.takeValueForKey(_selection.valueForKey("areaName"), "value");
    		EOKeyValueQualifier qual = new EOKeyValueQualifier("cycle.subjectEO.area", 
    				EOKeyValueQualifier.QualifierOperatorEqual, _selection);
    		dict.takeValueForKey(qual, "qualifier");
    		setValueForBinding(dict, "currDict");
    	} else if(canSetValueForBinding("selection")) {
    		setValueForBinding(_selection,"selection");
    	}
    	return null;
    }
    
    public WOActionResults selectSubject() {
		_selection = (Subject)item;
    	if(canSetValueForBinding("currDict")) {
    		NSMutableDictionary dict = ((NSDictionary)session().valueForKeyPath(
    				"strings.RujelEduPlan_EduPlan.settingQualifiers.subject")).mutableClone();
    		dict.takeValueForKey(_selection.valueForKey(Subject.SUBJECT_KEY), "value");
    		EOKeyValueQualifier qual = new EOKeyValueQualifier("cycle.subjectEO", 
    				EOKeyValueQualifier.QualifierOperatorEqual, _selection);
    		dict.takeValueForKey(qual, "qualifier");
    		setValueForBinding(dict, "currDict");
    	} else if(canSetValueForBinding("selection")) {
    		setValueForBinding(_selection,"selection");
    	}
    	return null;
    }

    public boolean isStateless() {
		return false;
	}
	public boolean synchronizesVariablesWithBindings() {
        return false;
	}
	
	public void reset() {
		super.reset();
	}
}