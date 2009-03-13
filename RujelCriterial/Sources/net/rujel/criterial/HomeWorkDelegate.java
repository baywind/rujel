// HomeWorkDelegate.java

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

package net.rujel.criterial;

import java.util.logging.Logger;

import com.webobjects.appserver.WOApplication;
import com.webobjects.appserver.WOComponent;
import com.webobjects.appserver.WOContext;
import com.webobjects.eoaccess.EOUtilities;
import com.webobjects.eocontrol.*;
import com.webobjects.foundation.*;

import net.rujel.base.MyUtility;
import net.rujel.base.BaseLesson.TaskDelegate;
import net.rujel.interfaces.EduLesson;
import net.rujel.reusables.SessionedEditingContext;
import net.rujel.reusables.WOLogLevel;

public class HomeWorkDelegate extends TaskDelegate {
	public String homeTaskForLesson(EduLesson lesson) {
		Work work = homeWorkForLesson(lesson, false);
		if(work == null)
			return null;
		return work.theme();
	}
	
	public void setHomeTaskForLesson(String newTask, EduLesson lesson) {
		 if(newTask == null) {
			 Work work = homeWorkForLesson(lesson, false);
			 if(work != null) {
				 work.removeObjectFromBothSidesOfRelationshipWithKey(lesson.course(),"course");
				 work.editingContext().deleteObject(work);
			 }
		 } else {
			 Work work = homeWorkForLesson(lesson, true);
			 work.setTheme(newTask);
		 }
	}
	
	public WOComponent homeWorkPopupForLesson(WOContext context, EduLesson lesson) {
    	WOComponent nextPage = WOApplication.application().pageWithName("WorkInspector", context);
    	nextPage.takeValueForKey(context.page(), "returnPage");
	   	EOEditingContext tmpEc = new SessionedEditingContext(
	   			lesson.editingContext(),context.session());
    	tmpEc.setSharedEditingContext(EOSharedEditingContext.defaultSharedEditingContext());
    	tmpEc.lock();
    	lesson = (EduLesson)EOUtilities.localInstanceOfObject(tmpEc, lesson);
    	nextPage.takeValueForKey(tmpEc, "tmpEC");
    	nextPage.takeValueForKey(homeWorkForLesson(lesson,true), "work");
    	tmpEc.unlock();
    	return nextPage;
	}
	
	public boolean hasPopup() {
		return true;
	}
	
	protected Work homeWorkForLesson(EduLesson lesson,boolean create) {
		EOEditingContext ec = lesson.editingContext();
		if(ec == null) {
			Logger.getLogger("rujel.criterial").log(WOLogLevel.WARNING,
					"Lesson not in EditingContext");
			return null;
		}
		if(ec.insertedObjects().containsObject(lesson )) {
			if(!create)
				return null;
		}
		EOQualifier qual = new EOKeyValueQualifier("course",
				EOQualifier.QualifierOperatorEqual,lesson.course());
		NSMutableArray quals = new NSMutableArray(qual);
		qual = new EOKeyValueQualifier("type",
				EOQualifier.QualifierOperatorEqual,new Integer(Work.HOMEWORK));
		quals.addObject(qual);
		NSTimestamp date = lesson.date();
		qual = new EOKeyValueQualifier("announce",
				EOQualifier.QualifierOperatorEqual,date);
		quals.addObject(qual);
		qual = new EOAndQualifier(quals);
		EOFetchSpecification fs = new EOFetchSpecification("Work",qual,EduLesson.sorter);
		NSArray found = ec.objectsWithFetchSpecification(fs);
		if(found != null && found.count() > 0)
			return (Work)found.objectAtIndex(0);
		if(!create)
			return null;
		Work result = (Work)EOUtilities.createAndInsertInstance(ec, "Work");
		result.addObjectToBothSidesOfRelationshipWithKey(lesson.course(), "course");
		result.setAnnounce(date);
		result.setDate(date.timestampByAddingGregorianUnits(0, 0, 1, 0, 0, 0));
		result.setType(new Integer(Work.HOMEWORK));
		MyUtility.setNumberToNewLesson(result);
		//result.setNumber(new Integer(0));
		return result;
	}
}
