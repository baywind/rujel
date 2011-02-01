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
import java.util.logging.Logger;

import net.rujel.base.MyUtility;
import net.rujel.interfaces.EduCourse;
import net.rujel.interfaces.EduLesson;
import net.rujel.reusables.WOLogLevel;

import com.webobjects.appserver.*;
import com.webobjects.eoaccess.EOUtilities;
import com.webobjects.eocontrol.EOEditingContext;
import com.webobjects.eocontrol.EOQualifier;
import com.webobjects.foundation.NSKeyValueCoding;
import com.webobjects.foundation.NSTimestamp;
import com.webobjects.foundation.NSValidation;

public class LessonInspector extends com.webobjects.appserver.WOComponent {
	public String newTitle;
	public String newTheme;
	public NSTimestamp newDate;
	
	public WOComponent returnPage;
	protected EduLesson lesson;
	//protected EOEditingContext ec;

    public LessonInspector(WOContext context) {
        super(context);
    }
    /*
    public void setReturnPage(WOComponent page) {
    	returnPage = page;
    	//ec = (EOEditingContext)page.valueForKey("ec");
    	newTitle = MyUtility.dateFormat().format(session().valueForKey("today"));
    } */

    public WOActionResults save() {
    	if(newTitle == null || newTheme == null) {
    		appendMessage("strings.RujelBase_Base.dateAndThemeRequired");
    		return this;
    	}
    	Date date = null;
    	try {
    		date = (Date)MyUtility.dateFormat().parseObject(
				newTitle, new java.text.ParsePosition(0));
    	} catch (Exception e) {
    		;
    	}
//		returnPage.ensureAwakeInContext(context());
//		EduLesson lesson = (EduLesson)returnPage.valueForKey("addLesson");
    	
    	if(lesson == null || lesson.editingContext() == null) {
    		EduCourse course = (EduCourse)returnPage.valueForKey("course");
    		lesson = (EduLesson)EOUtilities.createAndInsertInstance(
    				course.editingContext(), EduLesson.entityName);
    		lesson.addObjectToBothSidesOfRelationshipWithKey(course,"course");
    	}
			lesson.setTheme(newTheme);
			if(date != null) {
				newDate = (date instanceof NSTimestamp)?
						(NSTimestamp)date:new NSTimestamp(date);
						newTitle = null;
			} else {
				newDate = (NSTimestamp)session().valueForKey("today");
			}
			lesson.setDate(newDate);
			lesson.setTitle(newTitle);
			MyUtility.setNumberToNewLesson(lesson);
			EOQualifier limits = (EOQualifier)returnPage.valueForKeyPath("currTab.qualifier");
			boolean done = false;
			if(limits != null && !limits.evaluateWithObject(lesson)) {
				session().setObjectForKey(this, "LessonInspector");
//				lesson.editingContext().revert();
//				returnPage.takeValueForKey(null, "currPerPersonLink");
//				returnPage.valueForKey("refresh");
				appendMessage("strings.RujelBase_Base.notInTab");
				newTitle = MyUtility.dateFormat().format(newDate);
				done = true;
				return this;
			}
			try {
				lesson.validateForSave();
		    	returnPage.ensureAwakeInContext(context());
				WOActionResults result = (WOActionResults)returnPage.valueForKey("saveNoreset");
				if(result instanceof WOComponent)
					returnPage = (WOComponent)result;

			} catch (NSValidation.ValidationException ve) {
	    		session().takeValueForKey(ve.getMessage(), "message");
	    	} catch (NSKeyValueCoding.UnknownKeyException e) {
	    		session().takeValueForKey(application().valueForKeyPath
	    				("strings.RujelCriterial_Strings.messages.notSaved"), "message");
	    	} catch (Exception e) {
	    		session().takeValueForKey(e.getMessage(), "message");
	    		Logger.getLogger("rujel.base").log(WOLogLevel.WARNING, "error saving", 
	    				new Object[] {session(),lesson,e});
	    	}
	    	done = (!lesson.editingContext().hasChanges());
	       	if(done) {
//				returnPage.takeValueForKey(lesson, "currPerPersonLink");
				returnPage.valueForKey("updateLessonList");
				return RedirectPopup.getRedirect(context(), returnPage, null);
			}
		return this;
    }

    protected void appendMessage(String keyPath) {
/*    	Object message = session().valueForKey("message");
			if(message != null) {
				StringBuilder buf = new StringBuilder((String)message);
				buf.insert(0, "<p>");
				buf.append("</p>\n<p>");
				buf.append(application().valueForKeyPath(keyPath));
				buf.append("</p>");
				message = buf.toString();
			} else {
*/				Object message = application().valueForKeyPath(keyPath);
				if(message == null)
					message = "!!!";
//			}
			session().takeValueForKey(message, "message");
   }
    
    public EduLesson currLesson() {
    	return (EduLesson)returnPage.valueForKey("currLesson");
    }
    
    public WOActionResults returnPage() {
    	returnPage.ensureAwakeInContext(context());
    	EOEditingContext ec = (EOEditingContext)returnPage.valueForKey("ec");
    	if(ec != null && ec.hasChanges())
    		ec.revert();
    	return returnPage;
    }
    
    public WOActionResults invokeAction(WORequest aRequest, WOContext aContext) {
    	if(aContext.elementID().equals(aContext.senderID()))
    		return returnPage();
		return super.invokeAction(aRequest, aContext);
	}
}