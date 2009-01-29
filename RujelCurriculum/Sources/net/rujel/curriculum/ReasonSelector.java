// ReasonSelector.java: Class file for WO Component 'ReasonSelector'

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

package net.rujel.curriculum;

import net.rujel.interfaces.*;

import com.webobjects.appserver.*;
import com.webobjects.foundation.*;

// Generated by the WOLips Templateengine Plug-in at Jan 29, 2009 4:14:28 PM
public class ReasonSelector extends com.webobjects.appserver.WOComponent {
//	public NSArray reasons;
//	public Reason reason;
	protected NSTimestamp _date;
	protected EduCourse _course;
	public Reason rItem;
	
    public ReasonSelector(WOContext context) {
        super(context);
    }
/*
    public void appendToResponse(WOResponse aResponse, WOContext aContext) {
    	super.appendToResponse(aResponse, aContext);
    }*/
    
    public EduCourse course() {
    	if(_course == null) {
    		_course = (EduCourse)valueForBinding("course");
    	}
    	return _course;
    }
    
    public NSTimestamp date() {
    	if(_date == null) {
    		_date = (NSTimestamp)valueForBinding("date");
    	}
    	return _date;
    }
    
    public NSArray reasons() {
    	return Reason.reasons(date(), course());
    }
    
    public String slyleClass() {
    	if(rItem == null)
    		return "grey";
    	if(rItem.equals(valueForBinding("reason")))
    		return "selection";
    	if(rItem.unverified())
    		return "ungerade";
    	return "gerade";
    }
    
	public boolean isStateless() {
		return true;
	}
	
	public boolean synchronizesVariablesWithBindings() {
        return false;
	}

	public void reset() {
		//_attribs = null;
	}
}