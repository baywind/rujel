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
import net.rujel.reusables.WOLogLevel;

import com.webobjects.appserver.WOActionResults;
import com.webobjects.appserver.WOContext;
import com.webobjects.appserver.WOComponent;
import com.webobjects.appserver.WOResponse;
import com.webobjects.eocontrol.EOEditingContext;
import com.webobjects.eocontrol.EOEnterpriseObject;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSMutableArray;

public class SettingsByCourse extends WOComponent {
	protected NSArray _byCourse;
	public EOEnterpriseObject item;
//	public EOEditingContext ec;
	protected Object selector;
	
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
		String sel = (String)valueForBinding("selector");
		if(sel != null) {
			Object val = valueForBinding(sel);
	    	if(_byCourse != null && ((val == null)?selector == null : val.equals(selector)))
	    		return _byCourse;
			selector = val;
			SettingsBase base = base();
	    	_byCourse = base.settingUsage(sel, val, session().valueForKey("eduYear"));
	    	if(sel.equals(SettingsBase.TEXT_VALUE_KEY))
	    		sel = SettingsBase.NUMERIC_VALUE_KEY;
	    	else
	    		sel = SettingsBase.TEXT_VALUE_KEY;	
	    	if(canSetValueForBinding(sel)) {
	    		if(_byCourse.count() > 0) {
	    			EOEnterpriseObject bc = (EOEnterpriseObject)_byCourse.objectAtIndex(0);
	    			val = bc.valueForKey(sel);
	    		} else {
	    			val = null;
	    		}
    			setValueForBinding(val, sel);
	    	}
		}
		if(hasBinding("editList")) {
			if(sel == null)
				_byCourse = (NSArray)valueForBinding("editList");
			else
				setValueForBinding(_byCourse, "editList");
		}
		if(_byCourse == null) {
			SettingsBase base = base();
			if(base == null)
				_byCourse = new NSMutableArray();
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
    			return "<td colspan = \"2\"></td>";
    		else
    			return "<td/>";
    	}
    	return null;
	}
	
    public WOActionResults addByCourse() {
    	WOComponent editor = pageWithName("ByCourseEditor");
    	editor.takeValueForKey(context().page(), "returnPage");
    	editor.takeValueForKey(_byCourse, "editList");
    	editor.takeValueForKey(base(), "base");
    	if(hasBinding("textValue")) {
    		Object value = valueForBinding("textValue");
    		if(value == null)
    			value = NullValue;
    		editor.takeValueForKeyPath(value, "byCourse.textValue");
    	}
    	if(hasBinding("numericValue")) {
    		Object value = valueForBinding("numericValue");
    		if(value == null)
    			value = NullValue;
    		editor.takeValueForKeyPath(value, "byCourse.numericValue");
    	}
    	if(hasBinding("pushByCourse")) {
    		editor.takeValueForKey(this, "resultGetter");
    		editor.takeValueForKey("^pushByCourse", "pushToKeyPath");
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

	public Boolean cantSetBase() {
		if(!Various.boolForObject(valueForBinding("canSetBase")))
			return Boolean.TRUE;
		if(Various.boolForObject(valueForBinding("readOnly")))
			return Boolean.TRUE;
		NamedFlags access = (NamedFlags)valueForBinding("access");
		if(access != null && !access.flagForKey("edit"))
			return Boolean.TRUE;
		String sel = (String)valueForBinding("selector");
		if(sel == null)
			return Boolean.TRUE;
		Object val = valueForBinding(sel);
		Object bs = base().valueForKey(sel);
		if((val==null)?bs==null:val.equals(bs))
			return Boolean.TRUE;
		return (Boolean)session().valueForKeyPath("readAccess._edit.SettingsBase");
	}

	public WOActionResults makeBase() {
//		String sel = (String)valueForBinding("selector");
//		base().takeValueForKey(valueForBinding(sel), sel);
    	if(hasBinding("textValue"))
    		base().takeValueForKey(valueForBinding("textValue"), "textValue");
    	if(hasBinding("numericValue"))
    		base().takeValueForKey(valueForBinding("numericValue"), "numericValue");
		EOEditingContext ec = base().editingContext();
		try {
			ec.saveChanges();
			((NSMutableArray)_byCourse).insertObjectAtIndex(base(), 0);
			ByCourseEditor.logger.log(WOLogLevel.COREDATA_EDITING,"Changed BaseSettings",
					new Object[] {session(),base()});
		} catch (Exception e) {
			ec.revert();
			session().takeValueForKey(e.getMessage(), "message");
			ByCourseEditor.logger.log(WOLogLevel.WARNING,"Error modifying BaseSettings",
					new Object[] {session(),base(),e});
		}
		return null;
	}

    public boolean synchronizesVariablesWithBindings() {
        return false;
	}
	
	public boolean isStateless() {
		return false;
	}
	
	public void appendToResponse(WOResponse aResponse, WOContext aContext) {
		if(Various.boolForObject(valueForBinding("readOnly")))
			_byCourse = null;
		super.appendToResponse(aResponse, aContext);
	}
	
	public void reset() {
		super.reset();
//		base = null;
		_byCourse = null;
		item = null;
		setValueForBinding(null, "item");
	}
}