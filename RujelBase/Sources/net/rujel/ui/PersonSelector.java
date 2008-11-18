// PersonSelector.java: Class file for WO Component 'PersonSelector'

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

import net.rujel.interfaces.*;

import com.webobjects.foundation.*;
import com.webobjects.appserver.*;

public class PersonSelector extends WOComponent {
    public PersonLink item;

    public PersonSelector(WOContext context) {
        super(context);
    }
	public String rowStyle() {
		PersonLink curr = (PersonLink)valueForBinding("selectedItem");
		if(curr == item) return "selection";
		Boolean sex = item.person().sex();
		if(sex == null) return "grey";
		return (sex.booleanValue())?"male":"female";
	}
	/*
	protected int readChar(char c) {
		switch (c) {
			case '0':
				return 0;
			case '1':
				return 1;
			case '2':
				return 2;
		}
		return 3;
	}*/
	
	public String name() {
		boolean startWithLastName = true;
		int ln = 2;
		int fn = 2;
		int sn = 0;
		String nameFormat = (String)valueForBinding("nameFormat");
		if(nameFormat != null && nameFormat.length() > 0) {
			if(nameFormat.length() >= 3) {
				ln = (int)(nameFormat.charAt(0) -'0');
				fn = (int)(nameFormat.charAt(1) -'0');
				sn = (int)(nameFormat.charAt(2) -'0');
			}
			char c = nameFormat.charAt(nameFormat.length() - 1);
			startWithLastName = (c != '-');
		}
		return Person.Utility.fullName(item.person(),startWithLastName,ln,fn,sn);
	}
	
	public boolean checked() {
		NSSet selection = (NSSet)valueForBinding("selection");
		return (selection != null && selection.containsObject(item));
	}
	
	public String onClick() {
		if(hasBinding("selectedItem")) {
			String result = (String)valueForBinding("onClick");
			if(result == null)
				result = (String)session().valueForKey("tryLoad");
			return result;
		}
		return null;
	}
	
	public String onMouseOver() {
		if(hasBinding("selectedItem"))
			return "dim(this);";
		return null;
	}
	public String onMouseOut() {
		if(hasBinding("selectedItem"))
			return "unDim(this);";
		return null;
	}
	
	public WOActionResults select() {
		setValueForBinding(item,"selectedItem");
		return (WOActionResults)valueForBinding("selectAction");
	}
	
	public void setChecked(boolean val) {
		NSSet selection = (NSSet)valueForBinding("selection");
		if(selection instanceof NSMutableSet) {
			if(val)
				((NSMutableSet)selection).addObject(item);
			else
				((NSMutableSet)selection).removeObject(item);
		} else {
			NSSet tmp = new NSSet(item);
			if(val) {
				selection = selection.setByUnioningSet(tmp);
			} else {
				selection = selection.setBySubtractingSet(tmp);
			}
		}
		setValueForBinding(selection,"selection");
	}
	
	public boolean isStateless() {
		return true;
	}
	
	public boolean synchronizesVariablesWithBindings() {
        return false;
	}
	
	public void reset() {
		allAddOns = null;
		addOnItem = null;
		item = null;
	}	
	
	public NSArray allAddOns() {
		if(allAddOns == null) {
			allAddOns = valueForBinding("addOns");
		}
		if(allAddOns == null)
			return null;
		if(allAddOns instanceof NSArray)
		return (NSArray)allAddOns;
		if(allAddOns instanceof PerPersonLink)
			return (NSArray)((PerPersonLink)allAddOns).forPersonLink(item);
		throw new IllegalArgumentException("Required NSArra or PerPersonLink as addOns list, but found " + addOnItem.getClass().getName());
	}
	
    public String cellStyle() {
		StringBuffer result = new StringBuffer("white-space:nowrap;");
		if(allAddOns() != null)
			result.append("border-right:black 3px double;");
		Object width = valueForBinding("nameWidth");
		if(width != null)
			result.append("width:").append(width).append(';');
		return result.toString();
	}
	
    private Object allAddOns;
	public Object addOnItem;
	
    public NSKeyValueCoding currAddOn() {
		if(addOnItem instanceof NSKeyValueCoding)
			return (NSKeyValueCoding)addOnItem;
		if(addOnItem instanceof PerPersonLink)
			return (NSKeyValueCoding)((PerPersonLink)addOnItem).forPersonLink(item);
		throw new IllegalArgumentException("Required NSKeyValueCoding or PerPersonLink as addOn, but found " + addOnItem.getClass().getName());
	}
	
    public boolean ticks() {
        return hasBinding("selection");
    }
}
