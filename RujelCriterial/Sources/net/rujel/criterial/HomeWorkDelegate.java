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

import net.rujel.base.BaseLesson.NoteDelegate;
import net.rujel.base.BaseLesson.TaskDelegate;
import net.rujel.base.SettingsBase;
import net.rujel.interfaces.EduLesson;
import net.rujel.reusables.ModulesInitialiser;
import net.rujel.reusables.NamedFlags;
import net.rujel.reusables.SessionedEditingContext;
import net.rujel.reusables.WOLogLevel;

public class HomeWorkDelegate extends TaskDelegate {
	
	public String homeTaskForLesson(EduLesson lesson) {
		Work work = homeWorkForLesson(lesson);
		if(work == null)
			return null;
		return work.theme();
	}
	
	public void setHomeTaskForLesson(String newTask, EduLesson lesson) {
		 if(newTask == null) {
			 Work work = homeWorkForLesson(lesson);
			 if(work != null) {
				 work.removeObjectFromBothSidesOfRelationshipWithKey(lesson.course(),"course");
				 work.editingContext().deleteObject(work);
			 }
		 } else {
			 Work work = homeWorkForLesson(lesson);
			 if(work == null) {
				 work = (Work)EOUtilities.createAndInsertInstance(
						 lesson.editingContext(), Work.ENTITY_NAME);
				 work.addObjectToBothSidesOfRelationshipWithKey(lesson.course(), "course");
				 work.takeValuesFromDictionary(newDictForLesson(lesson));
//				 MyUtility.setNumberToNewLesson(work);
			 }
			 work.setNumber(Integer.valueOf(lesson.number().intValue() - 100));
			 work.setTheme(newTask);
		 }
	}
	
	public WOComponent homeWorkPopupForLesson(WOContext context, EduLesson lesson) {
    	WOComponent nextPage = WOApplication.application().pageWithName("WorkInspector", context);
    	nextPage.takeValueForKey(context.page(), "returnPage");
    	Object init = homeWorkForLesson(lesson);
    	if(init != null) {
    		nextPage.takeValueForKey(init, "work");
    	} else {
    		init = newDictForLesson(lesson);
    		nextPage.takeValueForKey(init, "dict");
    		if(((NSDictionary)init).valueForKey(Work.WORK_TYPE_KEY) == null)
    			nextPage.takeValueForKey(new NamedFlags(16,WorkType.flagNames), "namedFlags");
    	}
    	nextPage.takeValueForKey(lesson, "lesson");
    	return nextPage;
	}
	
	public boolean hasPopup() {
		return true;
	}
	
	public void updateNumber(EduLesson lesson, Integer newNumber) {
		Work work = homeWorkForLesson(lesson);
		if(work == null)
			return;
		if(newNumber == null)
			work.setNumber(Integer.valueOf(1));
		else
			work.setNumber(newNumber.intValue() -100);
	}
	
	protected Work homeWorkForLesson(EduLesson lesson) {
		EOEditingContext ec = lesson.editingContext();
		if(ec == null) {
			Logger.getLogger("rujel.criterial").log(WOLogLevel.WARNING,
					"Lesson not in EditingContext");
			return null;
		}
		if(ec.globalIDForObject(lesson).isTemporary()) {
			return null;
		}
		EOQualifier[] quals = new EOQualifier[3];
		quals[0] = new EOKeyValueQualifier("course",
				EOQualifier.QualifierOperatorEqual,lesson.course());
		quals[1] = new EOKeyValueQualifier(Work.FLAGS_KEY,
				EOQualifier.QualifierOperatorGreaterThanOrEqualTo, Integer.valueOf(16));
		NSDictionary stored = ec.committedSnapshotForObject(lesson);
		NSTimestamp date = (NSTimestamp)stored.valueForKey("date");//lesson.date();
		if(!date.equals(lesson.date()) && ec instanceof SessionedEditingContext) {
			WOSession ses = ((SessionedEditingContext)ec).session();
			ses.takeValueForKey(ses.valueForKeyPath(
					"strings.RujelCriterial_Strings.messages.checkWorks"), "message");
		}
		quals[2] = new EOKeyValueQualifier(Work.ANNOUNCE_KEY,
				EOQualifier.QualifierOperatorEqual,date);
		quals[2] = new EOAndQualifier(new NSArray(quals));
		EOFetchSpecification fs = new EOFetchSpecification(Work.ENTITY_NAME,
				quals[2],EduLesson.sorter);
		NSArray found = ec.objectsWithFetchSpecification(fs);
		if(found != null && found.count() > 0) {
			Work work = null;
			int num = lesson.number().intValue() - 100;
			for (int i = 0; i < found.count(); i++) {
				Work w = (Work)found.objectAtIndex(i);
				int wNum = w.number().intValue();
				if(wNum == num)
					return w;
				if(wNum < 0)
					continue;
				if(work == null || w.workType().namedFlags().flagForKey("fixHometask"))
					work = w;
			}
			return work;
		}
		return null;
	}
	
	public NSMutableDictionary newDictForLesson(EduLesson lesson) {
		EOEditingContext ec = lesson.editingContext();
		NSMutableDictionary result = new NSMutableDictionary();
		NSTimestamp date = lesson.date();
		result.takeValueForKey(date, Work.ANNOUNCE_KEY);
		
		EOQualifier[] quals = new EOQualifier[3];
		quals[0] = new EOKeyValueQualifier("course",
				EOQualifier.QualifierOperatorEqual,lesson.course());
		quals[1] = new EOKeyValueQualifier("date",
				EOQualifier.QualifierOperatorGreaterThan,date);
		quals[2] = null;
		quals[2] = new EOAndQualifier(new NSArray(quals));
		EOFetchSpecification fs = new EOFetchSpecification(EduLesson.entityName,
				quals[2],EduLesson.sorter);
		NSArray found = ec.objectsWithFetchSpecification(fs);
		date = date.timestampByAddingGregorianUnits(0, 0, 1, 0, 0, 0);
		if(found != null && found.count() > 0) {
			date = ((EduLesson)found.objectAtIndex(0)).date();
		} else  if (ec instanceof SessionedEditingContext) {
			WOSession ses = ((SessionedEditingContext)ec).session();
			ses.setObjectForKey(lesson, "assumeNextLesson");
			found = (NSArray)ses.valueForKeyPath("modules.assumeNextLesson");
			ses.removeObjectForKey("assumeNextLesson");
			if(found != null && found.count() > 0) {
				for (int i = 0; i < found.count(); i++) {
					NSKeyValueCoding dict = (NSKeyValueCoding)found.objectAtIndex(i);
					NSTimestamp assume = (NSTimestamp)dict.valueForKey("date");
					if(assume != null && assume.after(date)) {
						date = assume;
						break;
					}
				}
			}
		}
		result.takeValueForKey(date, Work.DATE_KEY);
		EOQualifier qual = EOQualifier.qualifierWithQualifierFormat(
				"dfltFlags >= 16 and dfltFlags < 64 and (dfltFlags < 32 or dfltFlags >= 48)", null);
		fs = new EOFetchSpecification(WorkType.ENTITY_NAME,qual,ModulesInitialiser.sorter);
		found = ec.objectsWithFetchSpecification(fs);
		if(found != null && found.count() > 0) {
			WorkType type = (WorkType)found.objectAtIndex(0);
			if(found.count() > 1) {
				for (int i = 0; i < found.count(); i++) {
					type = (WorkType)found.objectAtIndex(i);
					Integer flags = (Integer) type.valueForKey("dfltFlags"); 
					if((flags.intValue() & 4) == 4)
						break;
					else
						type = null;
				}
				if(type == null)
					type = (WorkType)found.objectAtIndex(0);
			}
			result.takeValueForKey(type,Work.WORK_TYPE_KEY);
/*			result.takeValueForKey(type.namedFlags(), "namedFlags");
		} else {
			NamedFlags namedFlags = new NamedFlags(WorkType.flagNames);
			namedFlags.setFlagForKey(true, "hometask");
			namedFlags.setFlagForKey(true, "fixHometask");
			result.takeValueForKey(namedFlags, "namedFlags");*/
		}
		String defaultHometask = SettingsBase.stringSettingForCourse("defaultHometask",
				lesson.course(), ec);
		if(defaultHometask != null)
			result.takeValueForKey(defaultHometask, Work.THEME_KEY);
		return result;
	}
	
	public NoteDelegate getNoteDelegateForLesson(EduLesson lesson) {
		EOEditingContext ec = lesson.editingContext();
		if(ec == null) {
			Logger.getLogger("rujel.criterial").log(WOLogLevel.WARNING,
					"Lesson not in EditingContext");
			return null;
		}
		return new WorkNoteDelegate(lesson);
	}
	
}
