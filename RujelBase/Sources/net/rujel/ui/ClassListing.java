// ClassListing.java: Class file for WO Component 'ClassListing'

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
//import net.rujel.vseobuch.*;

import com.webobjects.foundation.*;
import com.webobjects.appserver.*;
import com.webobjects.eocontrol.*;

public class ClassListing extends WOComponent {
    public transient EduGroup currClass;
	/*protected static java.lang.reflect.Method method;
	static {
		try {
			Class aClass = Class.forName(EduGroup.className);
			method = aClass.getMethod("listGroups",NSTimestamp.class,EOEditingContext.class);
		} catch (Exception ex) {
			throw new NSForwardException(ex, "Could not initialise EduGroup listing method");
		}
		
	}*/

    public ClassListing(WOContext context) {
        super(context);
    }
	
	public void reset() {
		currClass = null;
	}

    /** @TypeInfo EduGroup */
    public static NSArray listGroups(NSTimestamp today,EOEditingContext ec) {
//		NSTimestamp today = (NSTimestamp)session().valueForKey("today");
		if(today == null)
			today = new NSTimestamp();
		return EduGroup.Lister.listGroups(today,ec);
	}
	
	public NSArray groups() {
		Object sect = session().objectForKey("tmpSection");
		Object storedSection = null;
		if(sect != null) {
			storedSection = session().valueForKeyPath("state.section");
			session().takeValueForKeyPath(sect, "state.section");
		}
		NSArray result = listGroups((NSTimestamp)session().valueForKey("today"),
				(EOEditingContext)valueForBinding("editingContext"));
		if(storedSection != null)
			session().takeValueForKeyPath(storedSection, "state.section");
		return result;
	}
	
	public boolean isStateless() {
		return true;
	}
	
	public boolean synchronizesVariablesWithBindings() {
        return false;
	}
    public boolean noAction() {
        boolean noAction = !hasBinding("selectAction");
		return noAction;
    }
    
    public WOActionResults anAction() {
        setValueForBinding(currClass,"selection");
        session().removeObjectForKey("tmpSection");
		return (WOActionResults)valueForBinding("selectAction");
    }

	public String rowTitle() {
		try {
			return (String)currClass.valueForKey("hover");
		} catch (NSKeyValueCoding.UnknownKeyException e) {
			return null;
		}
	}

	public Object currSection() {
		Object sect = session().objectForKey("tmpSection");
		if(sect == null) {
			EduGroup selection = (EduGroup)valueForBinding("selection");
			if(selection != null) {
				try {
					sect = selection.valueForKey("section");
					session().setObjectForKey(sect, "tmpSection");
				} catch (NSKeyValueCoding.UnknownKeyException e) {
				}
			}
		}
		if(sect == null)
			return session().valueForKeyPath("state.section");
		return sect;
	}

	public void setCurrSection(Object currSection) {
		if(currSection == null)
			session().removeObjectForKey("tmpSection");
		else
			session().setObjectForKey(currSection, "tmpSection");
	}
}
