//  CourseSelector.java: Class file for WO Component 'CourseSelector'

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

package net.rujel.ui;

import net.rujel.interfaces.EduCourse;
import net.rujel.interfaces.EduGroup;
import net.rujel.reusables.PlistReader;
import net.rujel.reusables.Various;

import com.webobjects.appserver.WOActionResults;
import com.webobjects.appserver.WOApplication;
import com.webobjects.appserver.WOContext;
import com.webobjects.appserver.WOComponent;
import com.webobjects.eoaccess.EOUtilities;
import com.webobjects.eocontrol.EOEditingContext;
import com.webobjects.eocontrol.EOSortOrdering;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSDictionary;
import com.webobjects.foundation.NSKeyValueCodingAdditions;

public class CourseSelector extends WOComponent {
    public CourseSelector(WOContext context) {
        super(context);
        selection = (EduCourse)valueForBinding("selection");
        if(selection != null) {
        	setCurrGroup(selection.eduGroup());
        }
    }
    
    public EduCourse selection;
    public EduGroup currGroup;
    public NSArray courses;
    public EduCourse courseItem;
    
    public void setCourseItem(EduCourse course) {
    	courseItem = course;
    	if(canSetValueForBinding("courseItem"))
    		setValueForBinding(courseItem,"courseItem");
    }
    
    public void setCurrGroup(EduGroup group)  {
    	currGroup = group;
    	if(group != null) {
    		courses = EOUtilities.objectsMatchingKeyAndValue(group.editingContext(),
    				EduCourse.entityName, "eduGroup", group);
    		courses = EOSortOrdering.sortedArrayUsingKeyOrderArray(courses, EduCourse.sorter);
    	}
    }
    
    public WOActionResults select() {
    	selection = courseItem;
    	setValueForBinding(selection, "selection");
		session().removeObjectForKey("tmpSection");
    	return (WOActionResults)valueForBinding("selectAction");
    }
    
	public String act() {
		if(Various.boolForObject(valueForBinding("useAjax"))) {
			String href = context().componentActionURL();
			String result = "ajaxPopupAction('" + href + "');";
			return result;
		}
		return (String)session().valueForKey("tryLoad");
	}

	public String onSelect() {
		if(Various.boolForObject(valueForBinding("ajaxReturn"))) {
			String href = context().componentActionURL();
			String result = "ajaxPopupAction('" + href + "');";
			return result;
		}
		return (String)session().valueForKey("tryLoad");
	}

	public boolean isStateless() {
		return false;
	}
	public boolean synchronizesVariablesWithBindings() {
        return false;
	}

	public static WOComponent selectorPopup(WOComponent returnPage, Object resultGetter,
			String resultPath, EOEditingContext ec) {
		WOComponent selector = returnPage.pageWithName("SelectorPopup");
		if(resultGetter == null)
			resultGetter = returnPage;
		else
			selector.takeValueForKey(resultGetter, "resultGetter");
		selector.takeValueForKey(returnPage, "returnPage");
		selector.takeValueForKey(resultPath, "resultPath");
		Object course = null;
		try {
			course = NSKeyValueCodingAdditions.Utility.valueForKeyPath(resultGetter,resultPath);
		} catch (UnknownKeyException e) {
			;
		}
		selector.takeValueForKey(course, "value");
		NSDictionary dict = (NSDictionary)WOApplication.application().valueForKeyPath(
				"strings.RujelBase_Base.selectCourse");
		dict = PlistReader.cloneDictionary(dict, true);
		dict.takeValueForKeyPath(ec, "wrapperBindings.editingContext");
		selector.takeValueForKey(dict, "dict");
		return selector;
	}

}