// DiarySchedule.java : Class file for WO Component 'DiarySchedule'

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

package net.rujel.schedule;

import com.webobjects.appserver.WOContext;
import com.webobjects.appserver.WOResponse;
import com.webobjects.eocontrol.EOEditingContext;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSTimestamp;

import net.rujel.base.MyUtility;
import net.rujel.ui.LessonList;

public class DiarySchedule extends LessonList {

	public NSArray courses;
	public EOEditingContext ec;
	
	public DiarySchedule(WOContext context) {
        super(context);
    }

	public void appendToResponse(WOResponse aResponse, WOContext aContext) {
		courses = (NSArray)valueForBinding("courses");
		if(courses == null) {
			aResponse.appendContentString("No courses defined");
			return;
		}
		NSTimestamp date = (NSTimestamp)valueForBinding("date");
		Integer year = (date == null)?(Integer)application().valueForKey("year"):
			MyUtility.eduYearForDate(date);
		ec = (EOEditingContext)application().valueForKeyPath(
				"ecForYear." + year.toString());
		super.appendToResponse(aResponse, aContext);
	}

	public boolean synchronizesVariablesWithBindings() {
        return false;
	}
	public boolean isStateless() {
		return true;
	}
	
	public void reset() {
		courses = null;
		ec = null;
		super.reset();
	}
}