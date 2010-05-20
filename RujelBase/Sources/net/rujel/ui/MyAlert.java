// MyAlert.java: Class file for WO Component 'MyAlert'

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

import com.webobjects.appserver.WOActionResults;
import com.webobjects.appserver.WOContext;
import com.webobjects.appserver.WOComponent;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSForwardException;
import com.webobjects.foundation.NSKeyValueCoding;
import com.webobjects.foundation.NSMutableArray;
import com.webobjects.foundation.NSSelector;

public class MyAlert extends WOComponent {
	
	public String message;
	public WOComponent returnPage;
	public Boolean cantCancel;
	public boolean ajax = false;
	public NSArray actions;
	public NSKeyValueCoding item;
	
    public MyAlert(WOContext context) {
        super(context);
    }
    
    public void setAddAction(NSKeyValueCoding action) {
    	if(actions == null) {
    		actions = new NSMutableArray(action);
    		return;
    	}
    	if(!(actions instanceof NSMutableArray))
    		actions = actions.mutableClone();
    	((NSMutableArray)actions).addObject(action);
    }
    
    public WOActionResults run() {
    	returnPage.ensureAwakeInContext(context());
    	if(item == null)
    		return returnPage;
    	Object obj = item.valueForKey("object");
    	if(obj == null)
    		obj = returnPage;
    	String key = (String)item.valueForKey("getKey");
    	if(key != null) {
    		obj = NSKeyValueCoding.Utility.valueForKey(obj, key);
    		if(obj instanceof WOActionResults)
    			return (WOActionResults)obj;
    		else
    			return returnPage;
    	}
    	key = (String)item.valueForKey("setKey");
    	if(key != null) {
    		Object value = item.valueForKey("setValue");
    		NSKeyValueCoding.Utility.takeValueForKey(obj, value, key);
    		return returnPage;
    	}
    	NSSelector sel = (NSSelector)item.valueForKey("selector");
    	if(sel != null) {
    		Object[] params = (Object[])item.valueForKey("parameters");
    		try {
				sel.invoke(obj, params);
			} catch (Exception e) {
				throw new NSForwardException("Error perfoming selector", e);
			}
    	}
    	return returnPage;
    }
    
    public String cancelTitle() {
    	if(actions == null || actions.count() == 0) {
        	return (String)session().valueForKeyPath("strings.Reusables_Strings.uiElements.OK");
    	}
    	return (String)session().valueForKeyPath("strings.Reusables_Strings.uiElements.Cancel");
    }
    
    public String onClick() {
    	if(!ajax && item == null)
    		return "closePopup();";
    	StringBuilder buf = new StringBuilder();
    	if(ajax)
    		buf.append("ajaxPopupAction('");
    	else
    		buf.append("if(tryLoad())window.location = '");
    	buf.append(context().componentActionURL()).append('\'');
    	if(ajax)
    		buf.append(')');
    	buf.append(';');
    	return buf.toString();
    }
}