// ChooseRegime.java: Class file for WO Component 'ChooseRegime'

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

import net.rujel.reusables.*;

import com.webobjects.eocontrol.EOSortOrdering;
import com.webobjects.foundation.*;
import com.webobjects.appserver.*;
//import java.util.prefs.Preferences;
import java.util.Enumeration;
import java.util.logging.Logger;
import net.rujel.reusables.WOLogLevel;

public class ChooseRegime extends WOComponent {
	protected static Logger logger = Logger.getLogger("rujel");
//    public String regime;
    
	public WOComponent returnPage;
    public NSDictionary grpItem;
    public NSKeyValueCoding regItem;
//    public EOEditingContext ec = new SessionedEditingContext(session());
//    public Object eduGroup;
    public WOComponent srcMark;
    protected Number eduYear;

    public ChooseRegime(WOContext context) {
        super(context);
    }
	
    public void awake() {
    	super.awake();
    	eduYear = (Number)session().valueForKey("eduYear");
    }
    
    public boolean hideJournales() {
    	if(returnPage == null)
    		return false;
    	if(returnPage == srcMark)
    		return true;
    	if (returnPage.name().endsWith("SrcMark")) {
    		srcMark = returnPage;
    		return true;
    	}
    	return false;
    }
    
    public String onClick() {
    	if(returnPage != null && returnPage.name().endsWith((String)regItem.valueForKey("component")))
    		return "closePopup();";
    	return (String)session().valueForKey("tryLoad");
    }
    
    protected WOComponent cleanPathStack(String componentName) {
    	NSMutableArray pathStack = (NSMutableArray)session().valueForKey("pathStack");
    	WOComponent component = null;
    	if(pathStack != null && pathStack.count() > 0) {
    		if (componentName != null) {
				component = (WOComponent) pathStack.objectAtIndex(0);
				if (component.name().endsWith(componentName))
					component.ensureAwakeInContext(context());
				else
					component = null;
			}
			pathStack.removeAllObjects();
    	}
    	return component;
    }
    
    public WOComponent choose() {
		//session().takeValueForKey(this,"pushComponent");
    	String componentName = (String)regItem.valueForKey("component");
		String regTitle = (String)regItem.valueForKey("title");
		logger.log(WOLogLevel.FINER,"Opening regime "+ regTitle,session());
		WOComponent resultPage = cleanPathStack(componentName);
		if(resultPage == null)
			resultPage = pageWithName(componentName);
		try {
			resultPage.takeValueForKey(regTitle, "title");
		} finally {
			return resultPage;
		}
		//return pageWithName(prefs.get(regime,regime));
    }
	
    public WOComponent chooseJournal() {
		if(srcMark == null) {
			srcMark = cleanPathStack("SrcMark");
			if(srcMark == null)
				srcMark = pageWithName("SrcMark");
		} else {
			cleanPathStack(null);
    		srcMark.ensureAwakeInContext(context());
		}
		/*		
		session().takeValueForKey(this,"pushComponent");
		logger.log(WOLogLevel.FINER,"Opening regime "+ srcMark.valueForKey("title"),session());
		if(eduGroup != null) {
    		srcMark.takeValueForKey(eduGroup, "currClass");
        	eduGroup = null;  	
        	WOActionResults  result = (WOActionResults)srcMark.valueForKey("selectClass");
    		if(result != null)
    			return result;
    	}
*/    	return srcMark;
    }
    
    public WOActionResults changeDate() {
    	Number currYear = (Number)session().valueForKey("eduYear");
    	if(returnPage == null || currYear.intValue() != eduYear.intValue()) {
    		srcMark = null;
    		returnPage = chooseJournal();
    		returnPage.takeValueForKey(null, "currClass");
    		WOActionResults result = (WOActionResults)returnPage.valueForKey("selectClass");
    		if(result != null && result instanceof WOComponent)
    			returnPage = (WOComponent)result;
    	} else {
    		returnPage.ensureAwakeInContext(context());
    	}
    	return returnPage;
    }
    
    public WOActionResults flush() {
    	StringStorage str = (StringStorage)application().valueForKey("strings");
    	str.flush();
    	returnPage.ensureAwakeInContext(context());
    	return returnPage;
    }
	/*
	public static NSArray allRegimes() {
		try {
			return new NSArray(prefs.keys());
		} catch (java.util.prefs.BackingStoreException bex) {
			return null;
		}
	}
	
	public static NSDictionary allowedRegimes(UserPresentation user) {
		try {
			NSMutableDictionary result = new NSMutableDictionary();
			SettingsReader prefs = SettingsReader.settingsForPath("ui.regime",true);
			java.util.Enumeration enu = prefs.keyEnumerator();
			while(enu.hasMoreElements()) {
				String name = (String)enu.nextElement();
				String cmpnt = prefs.get(name,name);
				try {
					if(user.accessLevel(cmpnt) != 0)
						result.setObjectForKey(cmpnt,name);
				} catch (AccessHandler.UnlistedModuleException ex) {
					result.setObjectForKey(cmpnt,name);
				}
			}
			return result.immutableClone();
		} catch (Exception ex) {
			logger.logp(WOLogLevel.SEVERE,"ChooseRegime","allowedRegimes",
					"Can't get list of regimes preferences",ex);
			return null;
		}
	}
	
    public String title() {
        String result = (String)application().valueForKeyPath("strings.Strings.ChooseRegime.title");
        	if(result == null)
        		result = "Choose Regime";
        return result;
    }*/
    
    protected NSArray _regimeGroups;
    public NSArray regimeGroups() {
    	if(_regimeGroups != null)
    		return _regimeGroups;
    	_regimeGroups = (NSArray)application().valueForKeyPath(
    			"strings.Strings.ChooseRegime.regimeGroups");
    	NSKeyValueCodingAdditions readAccess = (NSKeyValueCodingAdditions)
    							session().valueForKey("readAccess");
    	NSMutableArray result = PlistReader.cloneArray(_regimeGroups, true);
/*    	if(Various.boolForObject(readAccess.valueForKeyPath("_read.Overview"))) {
    		NSMutableDictionary edu = (NSMutableDictionary)result.objectAtIndex(0);
    		if(!"edu".equals(edu.valueForKey("id"))) {
    			for (int i = 1; i < result.count(); i++) {
    				edu = (NSMutableDictionary)result.objectAtIndex(i);
    				if("edu".equals(edu.valueForKey("id")))
    					break;
    				else
    					edu = null;
    			}
    		}
    		if(edu != null) {
    			edu.removeObjectForKey("regimes");
    		}
    	}*/
    	_regimeGroups = (NSArray)session().valueForKeyPath("modules.regimeGroups");
    	if(_regimeGroups != null && _regimeGroups.count() > 0)
    		result.addObjectsFromArray(PlistReader.cloneArray(_regimeGroups, true));
    	NSDictionary grpsByID = new NSDictionary(result,(NSArray)result.valueForKey("id"));
    	NSArray allRegimes = (NSArray)application().valueForKeyPath(
    		"strings.Strings.ChooseRegime.defaultRegimes");
    	_regimeGroups = (NSArray)session().valueForKeyPath("modules.regimes");
    	if(_regimeGroups != null && regimeGroups().count() > 0) {
    		allRegimes = allRegimes.arrayByAddingObjectsFromArray(_regimeGroups);
    	}
    	Enumeration enu = allRegimes.objectEnumerator();
    	while (enu.hasMoreElements()) {
    		NSKeyValueCoding reg = (NSKeyValueCoding) enu.nextElement();
    		String checkAccess = (String) reg.valueForKey("checkAccess");
    		if(checkAccess == null)
    			checkAccess = (String) reg.valueForKey("component");
    		if(Various.boolForObject(readAccess.valueForKeyPath("_read." + checkAccess)))
    			continue;
    		NSMutableDictionary grp = (NSMutableDictionary)grpsByID.valueForKey(
    				(String)reg.valueForKey("group"));
    		if(grp == null)
    			grp = (NSMutableDictionary)grpsByID.valueForKey("other");
    		NSMutableArray regimes = (NSMutableArray)grp.valueForKey("regimes");
    		if(regimes == null) {
    			regimes = new NSMutableArray(reg);
    			grp.takeValueForKey(regimes, "regimes");
    		} else {
    			regimes.addObject(reg);
    			EOSortOrdering.sortArrayUsingKeyOrderArray(regimes, ModulesInitialiser.sorter);
    		}
    	} // place regimes to groups
    	enu = result.objectEnumerator();
    	result = new NSMutableArray(result.count());
    	while (enu.hasMoreElements()) {
    		NSMutableDictionary rGrp = (NSMutableDictionary) enu.nextElement();
    		if(Various.boolForObject(rGrp.valueForKeyPath("regimes.count")))
    			result.addObject(rGrp);
    	}
    	if(result.count() > 0)
    		EOSortOrdering.sortArrayUsingKeyOrderArray(result, ModulesInitialiser.sorter);
    	_regimeGroups = result;

    	return _regimeGroups;
    }
}
