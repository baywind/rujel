//CustomTab.java: Class file for WO Component 'CustomTab'

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

import net.rujel.reusables.StringStorage;
import net.rujel.reusables.Tabs;
import net.rujel.reusables.Various;

import com.webobjects.appserver.WOApplication;
import com.webobjects.appserver.WOContext;
import com.webobjects.appserver.WOComponent;
import com.webobjects.eocontrol.EOAndQualifier;
import com.webobjects.eocontrol.EOEditingContext;
import com.webobjects.eocontrol.EOKeyValueQualifier;
import com.webobjects.eocontrol.EOQualifier;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSKeyValueCoding;
import com.webobjects.foundation.NSMutableArray;
import com.webobjects.foundation.NSMutableDictionary;
import com.webobjects.foundation.NSSelector;

public class CustomTab extends WOComponent {
	
	public NSKeyValueCoding present;
	public EOEditingContext ec;
    public WOComponent returnPage;
    public String resultPath = "currTab";
    public NSMutableDictionary params;

	public static class Tab implements Tabs.GenericTab {

		public EOQualifier qual;
		public NSMutableDictionary params;
		
		public String title() {
			return (String)StringStorage.appStringStorage.valueForKeyPath(
					"Strings.LessonNoteEditor.manTab");
		}

		public String hover() {
			return (String)StringStorage.appStringStorage.valueForKeyPath(
					"Strings.LessonNoteEditor.manualTab");
		}

		public EOQualifier qualifier() {
			return qual;
		}

		public boolean defaultCurrent() {
			return false;
		}
		
	}
	
    public CustomTab(WOContext context) {
        super(context);
    }
        
    public WOComponent submit() {
    	returnPage.ensureAwakeInContext(context());
    	Tab tab = new Tab();
    	tab.params = params;
    	NSMutableArray quals = new NSMutableArray();
    	if(Various.boolForObject(params.valueForKey("useSuper"))) {
    		tab.qual = (EOQualifier)params.valueForKeyPath("parentTab.qualifier");
    		if(tab.qual != null)
    			quals.addObject(tab.qual);
    	}
    	/*
    	Object value = params.objectForKey("since");
    	if(value != null)
    		quals.addObject(new EOKeyValueQualifier("date",
    				EOQualifier.QualifierOperatorGreaterThanOrEqualTo,value));
    	value = params.objectForKey("to");
    	if(value != null)
    		quals.addObject(new EOKeyValueQualifier("date",
    				EOQualifier.QualifierOperatorLessThanOrEqualTo,value));
    	*/
    	tab.qual = QueryParams.paramsToQual(params, (NSArray)present.valueForKey("params"),
    			(String)present.valueForKey("entityName"), this, ec, quals);
    	returnPage.takeValueForKeyPath(tab, resultPath);
    	return returnPage;
    }
    
    public static CustomTab getPopup(WOComponent returnPage, String resultPath) {
    	CustomTab page = (CustomTab)WOApplication.application().pageWithName(
    			"CustomTab", returnPage.context());
    	page.returnPage = returnPage;
    	if(resultPath != null)
    		page.resultPath = resultPath;
    	Object current = returnPage.valueForKeyPath(page.resultPath);
    	if(current instanceof Tab) {
    		page.params = ((Tab) current).params;
    	}
    	if(page.params == null) {
    		page.params = new NSMutableDictionary();
    		if (current instanceof Tabs.GenericTab) {
    			EOQualifier qual = ((Tabs.GenericTab) current).qualifier();
    			analyseQualifyer(qual, page.params);
    			page.params.takeValueForKey(current, "parentTab");
    		}
    	}
    	return page;
    }
    
    protected static void analyseQualifyer(EOQualifier qual, NSMutableDictionary params) {
    	if(qual instanceof EOKeyValueQualifier &&
    			"date".equals(((EOKeyValueQualifier) qual).key())) {
    		NSSelector sel = ((EOKeyValueQualifier) qual).selector();
    		Object value = ((EOKeyValueQualifier) qual).value();
    		if(sel == EOQualifier.QualifierOperatorEqual) {
    			params.takeValueForKey(value,"min_date");
    			params.takeValueForKey(value,"max_date");
    		} else if(sel == EOQualifier.QualifierOperatorGreaterThan || 
    				sel == EOQualifier.QualifierOperatorGreaterThanOrEqualTo) {
    			params.takeValueForKey(value,"min_date");
    		} else if(sel == EOQualifier.QualifierOperatorLessThan || 
    				sel == EOQualifier.QualifierOperatorLessThanOrEqualTo) {
    			params.takeValueForKey(value,"max_date");
    		}
    	} else if (qual instanceof EOAndQualifier) {
    		NSArray list = ((EOAndQualifier) qual).qualifiers();
    		Enumeration enu = list.objectEnumerator();
    		while (enu.hasMoreElements()) {
				EOQualifier qq = (EOQualifier) enu.nextElement();
				analyseQualifyer(qq, params);
			}
    	}
    }
}