// ByCoursePresenter.java: Class file for WO Component 'ByCoursePresenter'

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

import net.rujel.base.QualifiedSetting;
import net.rujel.base.SettingsBase;
import net.rujel.reusables.Flags;
import net.rujel.reusables.MutableFlags;
import net.rujel.reusables.NamedFlags;
import net.rujel.reusables.Various;
import net.rujel.reusables.WOLogLevel;

import com.webobjects.appserver.*;
import com.webobjects.eocontrol.*;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSKeyValueCoding;
import com.webobjects.foundation.NSMutableArray;
import com.webobjects.foundation.NSMutableDictionary;

// Generated by the WOLips Templateengine Plug-in at Jul 17, 2009 3:54:08 PM
public class ByCoursePresenter extends com.webobjects.appserver.WOComponent {
	
	public static final int OTHER = 0;
	public static final int GROUP = 1;
	public static final int SUBJECT = 2;
	public static final int TEACHER = 3;
	
	protected NSKeyValueCoding _bc;
	public Object[] matrix;
	protected NSMutableDictionary params;
	public NSMutableArray rows; 
	public Integer rowspan;
	public boolean islist;
	public NSArray list;
	public Object item;
	public int index;
//	protected NSArray descriptions;
	public Boolean noEdit;
	
    public ByCoursePresenter(WOContext context) {
        super(context);
    }
    
    public NSKeyValueCoding bc() {
    	if(_bc == null)
    		_bc = (NSKeyValueCoding)valueForBinding("value");
    	return _bc;
    }
    
    public WOElement template() {
    	prepare();
    	if(hasBinding("rowspan"))
    		setValueForBinding(rowspan, "rowspan");
    	return super.template();
    }

    protected void prepare() {
    	_bc = (NSKeyValueCoding)valueForBinding("value");
    	if(_bc == null || _bc instanceof SettingsBase)
    		return;
    	QualifiedSetting qs = (QualifiedSetting)_bc;
    	list = qs.getCourses();
    	if(list != null) {
    		islist = true;
    		if(list.count() > 0)
    			rowspan = new Integer(list.count() +1);
    		return;
    	}
    	EOQualifier qual = qs.getQualifier();
    	
    	NSMutableArray editors = (NSMutableArray)valueForBinding("editors");
    	if(editors == null) {
    		editors = QualifiedSetting.editors(session());
    		if(canSetValueForBinding("editors"))
    			setValueForBinding(editors,"editors");
    	} else {
    		editors.takeValueForKey(null, "active");
    	}
    	params = new NSMutableDictionary();
    	try {
    		QualifiedSetting.analyseQual(qual, params, editors);
    	} catch (QualifiedSetting.AdvancedQualifierException aqe) {
			noEdit = Boolean.TRUE;
			return;
		}
    	Enumeration enu = editors.objectEnumerator();
    	NSMutableArray general = new NSMutableArray();
    	while (enu.hasMoreElements()) {
			NSMutableDictionary ed = (NSMutableDictionary) enu.nextElement();
			if(!Various.boolForObject(ed.valueForKey("active")))
				continue;
			Number mask = (Number)ed.valueForKey("mask");
			if(mask == null || mask.intValue() <= 0) {
				general.addObject(ed);
				continue;
			}
			Object[] row = rowForMask(mask.intValue());
			for (int i = 1; i < 4; i++) {
				if(Flags.getFlag(i, mask.intValue()))
					row[i] = ed;
			}
		}
    	int span = (rows == null)?0:rows.count();
    	if(general.count() > 0) {
    		list = general.immutableClone();
    		span++;
    	}
    	if(span > 1)
    		rowspan = new Integer(span);
    }
    
    protected Object[] rowForMask(int mask) {
    	if(rows != null) {
    		Enumeration enu = rows.objectEnumerator();
    		while (enu.hasMoreElements()) {
    			Object[] row = (Object[]) enu.nextElement();
    			MutableFlags rm = (MutableFlags)row[0];
    			if((mask & rm.intValue()) == 0) {
    				rm.setFlags(rm.intValue() | mask);
    				return row;
    			}
    		}
    	}
		Object[] row = new Object[4];
		row[0] = new MutableFlags(mask);
		if(rows == null)
			rows = new NSMutableArray((Object)row);
		else
			rows.addObject(row);
		return row;
    }
    
    public boolean omitCell() {
    	boolean result = hasBinding("rowspan");
    	return result;
    }

    public NSMutableDictionary dict() {
    	if(matrix == null || index < 0 || index >= 3) {
    		if(item instanceof NSMutableDictionary)
    			return (NSMutableDictionary)item;
    		return null;
    	}
    	return (NSMutableDictionary)matrix[index +1];
    }
    
    public boolean single() {
    	NSMutableDictionary dict = dict();
    	if(dict == null)
    		return true;
    	String attribute = Parameter.attribute(dict);
    	Object value = params.valueForKey(attribute);
    	if(value instanceof NSArray)
    		return (((NSArray)value).count() <= 1);
    	return true;
    }
    
    public Object value() {
    	NSMutableDictionary dict = dict();
    	if(dict == null)
    		return null;
    	String attribute = Parameter.attribute(dict);
    	Object value = params.valueForKey(attribute);
    	if(value == NullValue)
    		return null;
    	if(value != null) {
        	if(value instanceof NSArray) {
        		switch (((NSArray)value).count()) {
				case 0:
					return null;
				case 1:
					return ((NSArray)value).objectAtIndex(0);
				default:
					return value;
				}
        	}
    		return value;
    	} else if(Various.boolForObject(dict().valueForKey("range"))) {
    		StringBuilder buf = new StringBuilder();
    		value = params.valueForKey("min_" + attribute);
    		if(value != null)
    			buf.append(value);
    		value = params.valueForKey("max_" + attribute);
    		if(value == null) {
    			if(buf.length() > 0)
    				buf.insert(0, "&ge; ");
    		} else {
    			buf.append((buf.length() > 0)?" ... ":"&le; ").append(value);
    		}
    		if(buf.length() > 0)
    			return buf.toString();
    	}
    	return value;
    }
    
    public boolean hide() {
    	return (matrix != null && (list != null || rows.objectAtIndex(0) != matrix));
    }
    
    public boolean showCell() {
    	Number mask = (Number)valueForKeyPath("dict.mask");
    	return (mask == null || !Flags.getFlag(index, mask.intValue()));
    }

    public Boolean showOther() {
    	if(matrix == null || matrix[OTHER] == null)
    		return Boolean.FALSE;
    	if(valueForBinding("omit") != null)
    		return Boolean.FALSE;
    	return Boolean.TRUE;
    }
    
    protected NamedFlags _access;
    public NamedFlags access() {
    	if(_access != null)
    		return _access;
    	_access = (NamedFlags)valueForBinding("access");
    	if(_access != null)
    		return _access;
    	_access = (NamedFlags)session().valueForKeyPath("readAccess.FLAGS.QualifiedSetting");
    	return _access;
    }
    
	public String editorHead() {
    	if(Various.boolForObject(valueForBinding("readOnly")))
    		return null;
    	NamedFlags access = (NamedFlags)valueForBinding("access");
    	if(access == null)
    		access = (NamedFlags)session().valueForKeyPath("readAccess.FLAGS.QualifiedSetting");
    	if(access.flagForKey("edit") || access.flagForKey("delete")) {
    		if(access.flagForKey("edit") && access.flagForKey("delete"))
    			return "<td colspan = \"2\"></td>";
    		else
    			return "<td/>";
    	}
    	return null;
	}
	
    public boolean isBase() {
    	return (bc() instanceof SettingsBase);
    }

    public boolean isStateless() {
		return true;
	}
	public boolean synchronizesVariablesWithBindings() {
        return false;
	}
	
	public void reset() {
		islist = false;
		_bc = null;
		_access = null;
//		descriptions = null;
		list = null;
		item = null;
		matrix = null;
		index = -1;
		noEdit = null;
		rowspan = null;
		rows = null;
		super.reset();
	}

	public WOActionResults edit() {
		WOComponent editor = pageWithName("ByCourseEditor");
		editor.takeValueForKey(context().page(), "returnPage");
		editor.takeValueForKey(bc(), "byCourse");
		editor.takeValueForKey(valueForBinding("editList"), "editList");
    	if(hasBinding("pushByCourse")) {
    		editor.takeValueForKey(this, "resultGetter");
    		editor.takeValueForKey("^pushByCourse", "pushToKeyPath");
    	}
		return editor;
	}
	
	public void delete() {
		if(!(bc() instanceof EOEnterpriseObject))
			return;
		EOEnterpriseObject bc = (EOEnterpriseObject)bc();
		EOEditingContext ec = bc.editingContext();
		ec.lock();
		SettingsBase base = null;
		if(bc instanceof SettingsBase)
			base = (SettingsBase)bc;
		else
			base = (SettingsBase)bc.valueForKey("settingsBase");
		try {
			ec.deleteObject(bc);
			ByCourseEditor.logger.log(WOLogLevel.COREDATA_EDITING,"Deleting QualifiedSetting: "
					+ base.key(), new Object[] {session(),bc.valueForKey("settingsBase")});
			String path = (String)valueForBinding("pushByCourse");
			if(path != null) {
				WOComponent getter = parent();
	    		while (path != null && path.charAt(0) == '^') {
	    			path = path.substring(1);
					path = (String)getter.valueForBinding(path);
					getter = getter.parent();
				}
	    		if(path != null)
	    			getter.takeValueForKeyPath(bc, path);
			}
			ec.saveChanges();
			NSMutableArray alist = (NSMutableArray)valueForBinding("editList");
			if(alist != null)
				alist.removeObject(bc);
		} catch (Exception e) {
			ByCourseEditor.logger.log(WOLogLevel.INFO,"Could not delete QualifiedSetting: "
					+ base.key(), new Object[] {session(),bc,e});
			session().takeValueForKey(e.getMessage(), "message");
		} finally {
			ec.unlock();
		}
	}
}