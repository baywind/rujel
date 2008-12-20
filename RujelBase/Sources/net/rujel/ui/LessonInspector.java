// LessonInspector.java: Class file for WO Component 'LessonInspector'

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

import java.util.Date;

import net.rujel.base.MyUtility;
import net.rujel.interfaces.EduLesson;

import com.webobjects.appserver.*;
import com.webobjects.foundation.NSTimestamp;

public class LessonInspector extends com.webobjects.appserver.WOComponent {
	public String newDate;
	public String newTheme;
	
	public WOComponent returnPage;
	//protected EOEditingContext ec;

    public LessonInspector(WOContext context) {
        super(context);
    }
    
    public void setReturnPage(WOComponent page) {
    	returnPage = page;
    	//ec = (EOEditingContext)page.valueForKey("ec");
    	newDate = MyUtility.dateFormat().format(session().valueForKey("today"));
    }
    
    public WOActionResults save() {
    	if(newDate == null || newTheme == null) {
    		Object message = application().valueForKeyPath(
    				"extStrings.RujelBase_Base.dateAndThemeRequired");
    		session().takeValueForKey(message, "message");
    		return back();
    	}
    	Date date = (Date)MyUtility.dateFormat().parseObject(
				newDate, new java.text.ParsePosition(0));
		returnPage.ensureAwakeInContext(context());
		returnPage.valueForKey("addLesson");
		EduLesson lesson = (EduLesson)returnPage.valueForKey("currLesson");
		lesson.setTheme(newTheme);
    	if(date == null) {
    		lesson.setTitle(newDate);
    	} else {
    		NSTimestamp aDate = (date instanceof NSTimestamp)?
    				(NSTimestamp)date:new NSTimestamp(date);
    		lesson.setDate(aDate);
    	}
    	session().setObjectForKey(lesson.date(), "recentDate");
    	returnPage.valueForKey("save");
    	returnPage.takeValueForKey(lesson,"currLesson");
    	return returnPage;
    }
    
    public WOActionResults back() {
       	returnPage.ensureAwakeInContext(context());
    	return returnPage;
   }
}