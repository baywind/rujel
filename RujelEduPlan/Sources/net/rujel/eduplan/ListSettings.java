// ListSettings.java: Class file for WO Component 'ListSettings'

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

import java.util.Enumeration;

import net.rujel.base.SettingsBase;

import com.webobjects.appserver.*;
import com.webobjects.eocontrol.EOEditingContext;
import com.webobjects.eocontrol.EOEnterpriseObject;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSKeyValueCoding;
import com.webobjects.foundation.NSMutableArray;
import com.webobjects.foundation.NSComparator.ComparisonException;

// Generated by the WOLips Templateengine Plug-in at Jul 17, 2009 12:26:14 PM
public class ListSettings extends com.webobjects.appserver.WOComponent {
	
	protected NSMutableArray _lists;
	protected SettingsBase base;
	public NSMutableArray byCourse;
	public EOEditingContext ec;
	public Object currList;
	public Integer currNum;
	public NSMutableArray usage;
	public Object item;
	
    public ListSettings(WOContext context) {
        super(context);
    }
    
    public SettingsBase base() {
    	if (base == null) {
    		base = (SettingsBase) valueForBinding("base");
    		if(base == null) {
    			String key = (String) valueForBinding("key");
    			if(ec == null)
    				ec = (EOEditingContext)valueForBinding("ec");
    			base = SettingsBase.baseForKey(key, ec, false);
    		}
			NSArray baseByCourse = base.byCourse();
			byCourse = new NSMutableArray();
	    	if(baseByCourse != null && baseByCourse.count() > 0) {
	    		Enumeration enu = baseByCourse.objectEnumerator();
	    		Object eduYear = session().valueForKey("eduYear");
	    		while (enu.hasMoreElements()) {
	    			EOEnterpriseObject bc = (EOEnterpriseObject) enu.nextElement();
	    			if(bc.valueForKey("eduYear") == null || 
	    					eduYear.equals(bc.valueForKey("eduYear")))
	    				byCourse.addObject(bc);
	    		}
	    	}
    		if(byCourse.count() > 1) {
    			try {
					byCourse.sortUsingComparator(new SettingsBase.Comparator());
				} catch (ComparisonException e) {
					e.printStackTrace();
				}
    		}
        	if(hasBinding("currNum")) {
        		currNum = base.numericValue();
        		setValueForBinding(currNum, "currNum");
        	}
		}
		return base;
    }
    
    public NSMutableArray lists() {
    	if(_lists == null) {
    		_lists = new NSMutableArray(base().textValue());
    		if(byCourse == null || byCourse.count() == 0)
    			return _lists;
    		Enumeration enu = byCourse.objectEnumerator();
    		while (enu.hasMoreElements()) {
				EOEnterpriseObject bc = (EOEnterpriseObject) enu.nextElement();
				String listName = (String)bc.valueForKey(SettingsBase.TEXT_VALUE_KEY);
				if(!_lists.containsObject(listName))
					_lists.addObject(listName);
			}
    		NSArray extraLists = (NSArray)valueForBinding("extraLists");
    		if(extraLists != null && extraLists.count() > 0) {
    			String listKey = (String)valueForBinding("listKey");
    			enu = extraLists.objectEnumerator();
    			while (enu.hasMoreElements()) {
					Object listName = enu.nextElement();
					if(listKey == null && listName instanceof NSKeyValueCoding)
						listKey = "listName";
					if(listKey != null)
						listName = NSKeyValueCoding.Utility.valueForKey(listName, listKey);
					if(!_lists.containsObject(listName))
						_lists.addObject(listName);
				}
    		}
    	}
    	return _lists;
    }

    public void setCurrList(String list) {
    	if(list == null || list.equals(currList))
    		return;
    	currList = list;
    	setValueForBinding(currList, "currList");
    	if(hasBinding("currNum")) {
    		updateUsage();
    		if(usage == null || usage.count() == 0) {
    			currNum = null;
    		} else {
    			EOEnterpriseObject bc = (EOEnterpriseObject)usage.objectAtIndex(0);
    			currNum = (Integer)bc.valueForKey(SettingsBase.NUMERIC_VALUE_KEY);
    		}
    		setValueForBinding(currNum,"currNum");
    	}
    }
    
    public void updateUsage() {
    	if(usage == null)
    		usage = new NSMutableArray();
    	else
    		usage.removeAllObjects();
    	if(currList == null) 
    		return;
    	if(currList.equals(base().textValue()))
    		usage.addObject(base);
    	if(byCourse != null && byCourse.count() > 0) {
    		Enumeration enu = byCourse.objectEnumerator();
//    		Object eduYear = session().valueForKey("eduYear");
    		while (enu.hasMoreElements()) {
    			EOEnterpriseObject bc = (EOEnterpriseObject) enu.nextElement();
//    			if(bc.valueForKey("eduYear") != null && 
//    					!eduYear.equals(bc.valueForKey("eduYear")))
//    				continue;
    			if(currList.equals(bc.valueForKey(SettingsBase.TEXT_VALUE_KEY)))
    				usage.addObject(bc);
    		}
    	}
    }
    
    public void createList() {
    	if(!lists().contains(currList)) {
    		lists().addObject(currList);
    	}
    }
    
    public WOActionResults addByCourse() {
    	WOComponent editor = pageWithName("ByCourseEditor");
    	editor.takeValueForKey(context().page(), "returnPage");
    	editor.takeValueForKey(byCourse, "baseByCourse");
    	editor.takeValueForKey(base, "base");
    	editor.takeValueForKeyPath(currList, "byCourse.textValue");
    	editor.takeValueForKeyPath(currNum, "byCourse.numericValue");
    	return editor;
    }
    
    public void appendToResponse(WOResponse aResponse, WOContext aContext) {
    	Object cl = valueForBinding("currList");
    	if(currList == null || !currList.equals(cl)) {
    		currList = cl;
    		if(currList == null) {
    			currList = base().textValue();
    		}
    	}
    	updateUsage();
    	super.appendToResponse(aResponse, aContext);
    }
    
    public boolean synchronizesVariablesWithBindings() {
        return false;
	}
}