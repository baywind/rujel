// GenericAddOn.java: Class file for WO Component 'GenericAddOn'

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

package net.rujel.base;

import com.webobjects.foundation.*;
import com.webobjects.appserver.*;

import java.util.Enumeration;

public class GenericAddOn extends WOComponent {

	protected NSKeyValueCoding _addOn;
	protected Boolean _active;
	
    public GenericAddOn(WOContext context) {
        super(context);
    }
	
	public NSKeyValueCoding currAddOn() {
		if(_addOn == null) {
			_addOn = (NSKeyValueCoding)valueForBinding("currAddOn");
		}
		return _addOn;
	}
	
	public boolean active() {
		if(_active != null) return _active.booleanValue();
		NSArray activeList = (NSArray)valueForBinding("activeAddOns");
		if(activeList == null || activeList.count() == 0) {
			_active = Boolean.FALSE;
			return false;
		}
		int idx = getActiveIndex(activeList);
		if(idx == NSArray.NotFound) {
			_active = Boolean.FALSE;
			return false;
		} else {
			_active = Boolean.TRUE;
			return true;
		}
	}
	
	protected int getActiveIndex(NSArray activeList) {

//		Enumeration enu = activeList.objectEnumerator();
//		while (enu.hasMoreElements()) {
		for (int i = 0; i < activeList.count(); i++) {
			NSKeyValueCoding cur = (NSKeyValueCoding)activeList.objectAtIndex(i);
			if(cur == currAddOn() || cur == (NSKeyValueCoding)currAddOn().valueForKey("active")) {
				return i;
			}
			String id = (String)cur.valueForKey("id");
			if(id == null) {
				continue;
			}
			if(id.equals(currAddOn().valueForKey("id"))) {
				return i;
			}
		}
		return NSArray.NotFound;
	}
	
	
	public static NSKeyValueCoding addonForID(NSArray addOnsList,String addOnID) {
		Enumeration enu = addOnsList.objectEnumerator();
		while (enu.hasMoreElements()) {
			NSKeyValueCoding cur = (NSKeyValueCoding)enu.nextElement();
			if(addOnID.equals(cur.valueForKey("id")))
				return cur;
		}
		return null;
	}
	
	/*
	protected String _style;
	public String style() {
		if(_style == null) {
			StringBuffer buf = new StringBuffer("cursor:pointer;");
			if(!active()) {
				buf.append("border:none;");
			}
			_style = buf.toString();
		}
		return _style;
	}*/
	
	public String styleClass() {
		if(active() && currAddOn().valueForKey("imageOn") == null) {
			return "selectionBorder";
		}
		return null;
	}
	protected String _image;
	public String image() {
		if(_image != null) return _image;
		String image = (String)currAddOn().valueForKey("image");
		String imageOn = (String)currAddOn().valueForKey("imageOn");
		String imageOff = (String)currAddOn().valueForKey("imageOff");
		if(imageOn == null && imageOff == null) {
			if(image == null) return "eye.gif";
			return image;
		}
		if(active()) {
			if(imageOn == null) {
				if(image == null) return "eye.gif";
				return image;				
			}
			return imageOn;
		} else {
			if(imageOff == null) {
				if(image == null) return "eye.gif";
				return image;				
			}
			return imageOff;
		}
	}
	
	public String framework() {
		if("eye.gif".equals(image())) {
			return "JavaWOExtensions";
		}
		return (String)currAddOn().valueForKey("framework");
	}
	
	public static NSKeyValueCoding activeAddOn(NSKeyValueCoding addOn) {
		NSKeyValueCoding active = (NSKeyValueCoding)addOn.valueForKey("active");
		if(active == null) {
			String presenter = (String)addOn.valueForKey("activePresenter");
			if(presenter == null) {
				active = addOn;
			} else {
				active = new NSMutableDictionary("activeComponent",presenter);
				active.takeValueForKey(addOn.valueForKey("id"),"id");
			}
		}
		return active;
	} 
	
	public void toggleActive() {
		NSMutableArray activeList = (NSMutableArray)valueForBinding("activeAddOns");
		if(activeList == null) {
			activeList = new NSMutableArray();
			setValueForBinding(activeList,"activeAddOns");
		}
		int idx = getActiveIndex(activeList);
		if(idx == NSArray.NotFound) {
			NSKeyValueCoding active = activeAddOn(currAddOn());
			activeList.addObject(active);
		} else {
			activeList.removeObjectAtIndex(idx);
		}
	}
	
	public boolean synchronizesVariablesWithBindings() {
        return false;
	}
	
	public boolean isStateless() {
		return true;
	}
	
	public void reset() {
		_addOn = null;
//		_style = null;
		_active = null;
		_image = null;
	}
	
}