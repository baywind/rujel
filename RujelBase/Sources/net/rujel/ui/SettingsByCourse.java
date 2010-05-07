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
    	editor.takeValueForKeyPath(valueForBinding("defaultText"), "byCourse.textValue");
    	editor.takeValueForKeyPath(valueForBinding("defaultNumeric"), "byCourse.numericValue");
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