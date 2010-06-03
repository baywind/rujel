// SettingsByCourse.java: Class file for WO Component 'SettingsByCourse'

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

import net.rujel.base.SettingsBase;
import net.rujel.reusables.NamedFlags;
import net.rujel.reusables.Various;

import com.webobjects.appserver.WOActionResults;
import com.webobjects.appserver.WOContext;
import com.webobjects.appserver.WOComponent;
import com.webobjects.eocontrol.EOEditingContext;
import com.webobjects.eocontrol.EOEnterpriseObject;
import com.webobjects.foundation.NSArray;

public class SettingsByCourse extends WOComponent {
	protected NSArray _byCourse;
	public EOEnterpriseObject item;
//	public EOEditingContext ec;
	
    public SettingsByCourse(WOContext context) {
        super(context);
    }
/*
    public String key() {
    	if(base == null)
    		return null;
    	return base.key();
    }
    
    public void setKey(String key) {
    	if(base == null || ! base.key().equals(key)) {
    		if(ec == null)
    			ec = (EOEditingContext)parent().valueForKey("ec");
    		base = SettingsBase.baseForKey(key, ec, false);
    	}
    }*/
    
    public SettingsBase base() {
    	SettingsBase base = (SettingsBase)valueForBinding("base");
    	if(base == null && hasBinding("key")) {
    		String key = (String)valueForBinding("key");
    		EOEditingContext ec = (EOEditingContext)valueForBinding("ec");
    		if(ec == null)
    			ec = (EOEditingContext)parent().valueForKey("ec");
    		base = SettingsBase.baseForKey(key, ec, false);
    	}
    	return base;
    }
    
	public NSArray byCourse() {
		if(_byCourse == null) {
			SettingsBase base = base();
			if(base == null)
				_byCourse = NSArray.EmptyArray;
			else
				_byCourse = base.byCourse((Integer)session().valueForKey("eduYear"));
		}
		return _byCourse;
	}
    
	public Boolean hideDetails() {
		if(!Various.boolForObject(valueForBinding("hideEmptyDetails")))
			return Boolean.FALSE;
		return Boolean.valueOf(byCourse().count() <= 1);
	}
	
	public String editorHead() {
    	if(Various.boolForObject(valueForBinding("readOnly")))
    		return null;
    	NamedFlags access = (NamedFlags)valueForBinding("access");
    	if(access == null)
    		access = (NamedFlags)session().valueForKeyPath("readAccess.FLAGS.SettingByCourse");
    	if(access.flagForKey("edit") || access.flagForKey("delete")) {
    		if(access.flagForKey("edit") && access.flagForKey("delete"))
    			return "<td colspan = \"2\"/>";
    		else
    			return "<td/>";
    	}
    	return null;
	}
	
    public WOActionResults addByCourse() {
    	WOComponent editor = pageWithName("ByCourseEditor");
    	editor.takeValueForKey(context().page(), "returnPage");
//    	editor.takeValueForKey(byCourse, "baseByCourse");
    	editor.takeValueForKey(base(), "base");
    	if(hasBinding("defaultText")) {
    		Object value = valueForBinding("defaultText");
    		if(value == null)
    			value = NullValue;
    		editor.takeValueForKeyPath(value, "byCourse.textValue");
    	}
    	if(hasBinding("defaultNumeric")) {
    		Object value = valueForBinding("defaultNumeric");
    		if(value == null)
    			value = NullValue;
    		editor.takeValueForKeyPath(value, "byCourse.numericValue");
    	}
    	if(hasBinding("changedByCourse")) {
    		editor.takeValueForKey("^changedByCourse", "pushToKeyPath");
    		editor.takeValueForKey(this, "resultGetter");
    	}
    	return editor;
    }
    
    public void setItem(EOEnterpriseObject obj) {
    	item = obj;
    	setValueForBinding(item, "item");
    }
    
    public Boolean canEdit() {
    	if(Various.boolForObject(valueForBinding("readOnly")))
    		return Boolean.FALSE;
    	NamedFlags access = (NamedFlags)valueForBinding("access");
    	if(access != null)
    		return Boolean.valueOf(access.flagForKey("create"));
    	else
    		return (Boolean)session().valueForKeyPath("readAccess.create.SettingByCourse");
    }
	
    public boolean synchronizesVariablesWithBindings() {
        return false;
	}
	
	public boolean isStateless() {
		return true;
	}
	
	public void reset() {
		super.reset();
//		base = null;
		_byCourse = null;
		item = null;
		setValueForBinding(null, "item");
	}
}