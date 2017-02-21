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

import java.util.Enumeration;

import net.rujel.base.MyUtility;
import net.rujel.base.SchoolSection;
import net.rujel.interfaces.EduCycle;
import net.rujel.reusables.Various;

import com.webobjects.appserver.WOActionResults;
import com.webobjects.appserver.WOContext;
import com.webobjects.appserver.WOComponent;
import com.webobjects.eoaccess.EOUtilities;
import com.webobjects.eocontrol.EOEditingContext;
import com.webobjects.eocontrol.EOEnterpriseObject;
import com.webobjects.eocontrol.EOFetchSpecification;
import com.webobjects.eocontrol.EOKeyValueQualifier;
import com.webobjects.eocontrol.EOSortOrdering;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSDictionary;
import com.webobjects.foundation.NSMutableDictionary;
import com.webobjects.foundation.NSMutableSet;

public class SubjectSelector extends WOComponent {
    public SubjectSelector(WOContext context) {
        super(context);
    }
    
    protected EOEnterpriseObject _currArea;
    protected EOEnterpriseObject _selection;
    protected NSArray _subjects;
    protected NSArray _areas;
    protected NSMutableDictionary agregate;

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
    	return areaForSelection(selection());
    }
    
    public static EOEnterpriseObject areaForSelection(EOEnterpriseObject selection) {
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
    	_subjects = null;
		if(canSetValueForBinding("currDict"))
			setValueForBinding(null, "currDict");
		if(canSetValueForBinding("selection"))
			setValueForBinding(null, "selection");
    	return null;
    }
    
    public NSArray subjects() {
    	if(_subjects == null) {
        	if(agregate == null) {
        		_subjects = Subject.subjectsForArea(_currArea,true);
        	} else {
        		NSMutableSet sSet = (NSMutableSet)agregate.objectForKey(_currArea);
        		if(sSet == null)
        			_subjects = NSArray.EmptyArray;
        		else
        			_subjects = EOSortOrdering.sortedArrayUsingKeyOrderArray(
        					sSet.allObjects(), MyUtility.numSorter);
        	}
    	}
    	return _subjects;
    }
    
    public NSArray areas() {
    	item = null;
    	_selection = null;
		EOEditingContext ec = (EOEditingContext)valueForBinding("ec");
		if(ec == null)
			ec = (EOEditingContext)valueForBinding("editingContext");
   	if(agregate != null) {
    		Object check = agregate.valueForKey("section");
    		if(check != null && !check.equals(SchoolSection.stateSection(session(), ec))) {
    			_areas = null;
//    		} else {
//    			check = agregate.valueForKey("school");
//        		if(check != null && !check.equals(session().valueForKey("school")))
//        			_areas = null;
    		}
    		if(_areas == null) {
    			_subjects = null;
    			_selection = null;
    			if(canSetValueForBinding("currDict"))
    				setValueForBinding(null, "currDict");
    			if(canSetValueForBinding("selection"))
    				setValueForBinding(null, "selection");
    			item = _currArea;
    			_currArea = null;
    		}
    	}
    	if(_areas == null) {
    		if(Various.boolForObject(valueForBinding("existingOnly"))) {
    			agregate = new NSMutableDictionary();
				NSMutableDictionary values = new NSMutableDictionary();
				values.takeValueForKey(SchoolSection.stateSection(session(), ec),"section");
//				values.takeValueForKey(session().valueForKey("school"), "school");
				NSArray cycles = EOUtilities.objectsMatchingValues(ec,EduCycle.entityName, values);
				if(cycles == null || cycles.count() == 0)
					return null;
				Enumeration enu = cycles.objectEnumerator();
				while (enu.hasMoreElements()) {
					PlanCycle cycle = (PlanCycle) enu.nextElement();
					NSArray hrs = cycle.planHours();
					if(hrs == null || hrs.count() == 0)
						continue;
					Subject subj = cycle.subjectEO();
					NSMutableSet sSet = (NSMutableSet)agregate.objectForKey(subj.area());
					if(sSet == null) {
						sSet = new NSMutableSet(subj);
						agregate.setObjectForKey(sSet, subj.area());
					} else {
						sSet.addObject(subj);
					}
				}
				_areas = EOSortOrdering.sortedArrayUsingKeyOrderArray(
						agregate.allKeys(), MyUtility.numSorter);
				agregate.addEntriesFromDictionary(values);
    		} else {
    			EOFetchSpecification fs = new EOFetchSpecification(
    					"SubjectArea",null,MyUtility.numSorter);
    			_areas = ec.objectsWithFetchSpecification(fs);
    		}
    	}
    		EOEnterpriseObject selArea = areaForSelection(selection());
    		if(selArea != null && _areas.contains(selArea)) {
    			_currArea = selArea;
    			_subjects = null;
    			subjects();
    			if(_selection != selArea && 
    					(_subjects == null || !_subjects.containsObject(_selection))) {
        			_selection = null;
        			if(canSetValueForBinding("currDict"))
        				setValueForBinding(null, "currDict");
        			if(canSetValueForBinding("selection"))
        				setValueForBinding(null, "selection");
    			}
    		} else if(_selection != null) {
    			_selection = null;
    			if(canSetValueForBinding("currDict"))
    				setValueForBinding(null, "currDict");
    			if(canSetValueForBinding("selection"))
    				setValueForBinding(null, "selection");
    		}
    	if(item != null && _currArea == null && _areas.contains(item))
    		openArea();
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
    	return (WOActionResults)valueForBinding("selectAction");
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
    	return (WOActionResults)valueForBinding("selectAction");
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