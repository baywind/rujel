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
import com.webobjects.appserver.WOSession;
import com.webobjects.eoaccess.EOUtilities;
import com.webobjects.eocontrol.*;
import com.webobjects.foundation.*;

import net.rujel.base.MyUtility;
import net.rujel.base.BaseLesson.TaskDelegate;
import net.rujel.interfaces.EduLesson;
import net.rujel.reusables.ModulesInitialiser;
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
//    	tmpEc.lock();
    	lesson = (EduLesson)EOUtilities.localInstanceOfObject(tmpEc, lesson);
    	nextPage.takeValueForKey(tmpEc, "tmpEC");
    	nextPage.takeValueForKey(homeWorkForLesson(lesson,true), "work");
//    	tmpEc.unlock();
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
		EOQualifier[] quals = new EOQualifier[3];
		quals[0] = new EOKeyValueQualifier("course",
				EOQualifier.QualifierOperatorEqual,lesson.course());
		quals[1] = new EOKeyValueQualifier(Work.FLAGS_KEY,
				EOQualifier.QualifierOperatorGreaterThanOrEqualTo, new Integer(16));
		NSTimestamp date = lesson.date();
		quals[2] = new EOKeyValueQualifier(Work.ANNOUNCE_KEY,
				EOQualifier.QualifierOperatorEqual,date);
		quals[2] = new EOAndQualifier(new NSArray(quals));
		EOFetchSpecification fs = new EOFetchSpecification(Work.ENTITY_NAME,
				quals[2],EduLesson.sorter);
		NSArray found = ec.objectsWithFetchSpecification(fs);
		if(found != null && found.count() > 0) {
			Work work = (Work)found.objectAtIndex(0);
			if(found.count() > 1) {
				for (int i = 0; i < found.count(); i++) {
					work = (Work)found.objectAtIndex(i);
					if(work.namedFlags().flagForKey("fixHometask"))
						break;
					work = null;
				}
				if(work == null)
					work = (Work)found.objectAtIndex(0);
			}
			return work;
		}
		if(!create)
			return null;
		Work result = (Work)EOUtilities.createAndInsertInstance(ec, Work.ENTITY_NAME);
		result.addObjectToBothSidesOfRelationshipWithKey(lesson.course(), "course");
		result.setAnnounce(date);
		quals[1] = new EOKeyValueQualifier("date",
				EOQualifier.QualifierOperatorGreaterThan,date);
		quals[2] = null;
		quals[2] = new EOAndQualifier(new NSArray(quals));
		fs = new EOFetchSpecification(EduLesson.entityName,quals[2],EduLesson.sorter);
		found = ec.objectsWithFetchSpecification(fs);
		date = date.timestampByAddingGregorianUnits(0, 0, 1, 0, 0, 0);
		if(found != null && found.count() > 0) {
			date = ((EduLesson)found.objectAtIndex(0)).date();
		} else  if (ec instanceof SessionedEditingContext) {
			WOSession ses = ((SessionedEditingContext)ec).session();
			ses.setObjectForKey(lesson.course(), "assumeNextLesson");
			found = (NSArray)ses.valueForKeyPath("modules.assumeNextLesson");
			ses.removeObjectForKey("assumeNextLesson");
			if(found != null && found.count() > 0) {
				for (int i = 0; i < found.count(); i++) {
					NSKeyValueCoding dict = (NSKeyValueCoding)found.objectAtIndex(i);
					NSTimestamp assume = (NSTimestamp)dict.valueForKey("date");
					if(assume != null) {
						date = assume;
						break;
					}
				}
			}
		}
		result.setDate(date);
		EOQualifier qual = EOQualifier.qualifierWithQualifierFormat(
				"dfltFlags >= 16 and dfltFlags < 64", null);
		fs = new EOFetchSpecification("WorkType",qual,ModulesInitialiser.sorter);
		found = ec.objectsWithFetchSpecification(fs);
		if(found != null && found.count() > 0) {
			EOEnterpriseObject type = (EOEnterpriseObject)found.objectAtIndex(0);
			if(found.count() > 1) {
				for (int i = 0; i < found.count(); i++) {
					type = (EOEnterpriseObject)found.objectAtIndex(i);
					Integer flags = (Integer) type.valueForKey("dfltFlags"); 
					if((flags.intValue() & 4) == 4)
						break;
					else
						type = null;
				}
				if(type == null)
					type = (EOEnterpriseObject)found.objectAtIndex(0);
			}
			result.setWorkType(type);
		} else {
			result.namedFlags().setFlagForKey(true, "hometask");
		}
		MyUtility.setNumberToNewLesson(result);
		//result.setNumber(new Integer(0));
		return result;
	}
}
