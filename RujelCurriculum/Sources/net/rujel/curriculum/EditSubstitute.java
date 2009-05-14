// EditSubstitute.java: Class file for WO Component 'EditSubstitute'

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

import java.math.BigDecimal;
import java.util.Enumeration;
import java.util.logging.Logger;

import net.rujel.interfaces.*;

import com.webobjects.appserver.*;
import com.webobjects.eoaccess.EOUtilities;
import com.webobjects.eocontrol.EOEditingContext;
import com.webobjects.eocontrol.EOGlobalID;
import com.webobjects.foundation.*;
import net.rujel.interfaces.Teacher;
import net.rujel.reusables.WOLogLevel;

// Generated by the WOLips Templateengine Plug-in at Oct 1, 2008 1:23:43 PM
public class EditSubstitute extends com.webobjects.appserver.WOComponent {
	protected static Logger logger = Logger.getLogger("rujel.curriculum");

	public EditSubstitute(WOContext context) {
        super(context);
    }
    
    public WOActionResults returnPage;
    public Substitute substitute;
    public EduLesson lesson;
    public NSArray forcedList;
	public Teacher teacher;
	//public boolean join = false;
	public BigDecimal factor = BigDecimal.ONE;
	//public String comment;
	public Reason reason;
	public Boolean cantSelect;
	public Boolean cantEdit = Boolean.TRUE;
	public Boolean canDelete = Boolean.FALSE;
   
    public void setLesson(EduLesson aLesson) {
    	lesson = aLesson;
    	EOEditingContext ec = lesson.editingContext();
    	EOGlobalID userGID = (EOGlobalID)session().valueForKey("userPersonGID");
    	if(userGID != null) {
       	   	PersonLink userPerson = (PersonLink)ec.objectForGlobalID(userGID);
       	   	if(userPerson instanceof Teacher && !(userPerson == lesson.course().teacher())) {
       	   		forcedList = new NSArray(userPerson);
       	   		teacher = (Teacher)userPerson;
       	   	}
    	}
    }
    
    public void setSubstitute(Substitute sub) {
    	substitute = sub;
    	if(sub != null) {
    		teacher = sub.teacher();
    		if(forcedList == null || !forcedList.contains(teacher)) {
    			if(forcedList == null)
    				forcedList = new NSArray(teacher);
    			else
    				forcedList = forcedList.arrayByAddingObject(teacher);
    		}
    		reason = substitute.reason();
    		//comment = (String)substitute.valueForKeyPath("reason.reason");
    		//join = sub.sFlags().flagForKey("join");
    		factor = sub.factor().stripTrailingZeros();
    		if(factor.scale() < 0)
    			factor.setScale(0);
    		// TODO: think on more specific access reading
    		String obj = (context().page() == this)?"substitute":"Substitute";
    		cantEdit = (Boolean)session().valueForKeyPath("readAccess._edit." + obj);
    		cantSelect = cantEdit;
    		canDelete = (Boolean)session().valueForKeyPath("readAccess.delete." + obj);
    	} else {
    		cantSelect = (Boolean)session().valueForKeyPath("readAccess._edit.Substitute");
    	}
		if(cantSelect.booleanValue() && teacher == null) {
			cantSelect = Boolean.FALSE;
		}
    }

    public void setTeacher(Teacher aTeacher) {
    	teacher = (Teacher)EOUtilities.localInstanceOfObject(lesson.editingContext(), aTeacher);
    	if(aTeacher != null) {
    		String path = "readAccess." + ((substitute==null)?"_create.S":"_edit.s") +"ubstitute";
    		cantEdit = (Boolean)session().valueForKeyPath(path);
    	} else {
    		cantEdit = Boolean.TRUE;
    	}
    }
    
	public WOActionResults save() {
		EOEditingContext ec = lesson.editingContext(); 
		NSArray others = (NSArray)lesson.valueForKey("substitutes");
		if(others != null && others.count() > 0) {
			Enumeration enu = others.objectEnumerator();
			while (enu.hasMoreElements()) {
				Substitute sub = (Substitute) enu.nextElement();
				if(sub != substitute && sub.teacher() == teacher) {
					reason = null;
					session().takeValueForKey(application().valueForKeyPath(
						"strings.RujelCurriculum_Curriculum.messages.duplicateTeacher"), "message");
					session().removeObjectForKey("lessonProperies");
					break;
				}
			}
		}
		if(reason == null) {
			if (returnPage instanceof WOComponent) 
				((WOComponent)returnPage).ensureAwakeInContext(context());
			if(ec.hasChanges())
				ec.revert();
			return returnPage;
		}
		String action = "saved";
		ec.lock();
		if(substitute == null) {
			substitute = (Substitute)EOUtilities.createAndInsertInstance(ec, Substitute.ENTITY_NAME);
			substitute.addObjectToBothSidesOfRelationshipWithKey(lesson, "lesson");
			action = "created";
		}
		substitute.addObjectToBothSidesOfRelationshipWithKey(teacher,"teacher");
		//substitute.sFlags().setFlagForKey(join, "join");
		substitute.setFactor(factor);
		substitute.addObjectToBothSidesOfRelationshipWithKey(reason, "reason");
/*		if(comment == null) {
			EOEnterpriseObject reason = substitute.reason();
			if(reason != null) {
				NSArray subs = (NSArray)reason.valueForKey("substitutes");
				if(subs == null || subs.count() < 2) {
					substitute.removeObjectFromBothSidesOfRelationshipWithKey(reason, "reason");
					ec.deleteObject(reason);
				}
			}
		} else {
			EOEnterpriseObject reason = EOUtilities.createAndInsertInstance(ec, "Reason");
			reason.takeValueForKey(comment, "reason");
			substitute.addObjectToBothSidesOfRelationshipWithKey(reason, "reason");
		}*/
		return done(action);
	}
	
	public WOActionResults delete() {
		EOEditingContext ec = lesson.editingContext(); 
		ec.lock();
		if(substitute != null && substitute.editingContext() != null) {
/*			reason = substitute.reason();
			if(reason != null && 
					(reason.substitutes() == null || reason.substitutes().count() == 0) &&
					(reason.variations() == null || reason.variations().count() == 0)) {			
				logger.log(WOLogLevel.UNOWNED_EDITING,"Deleting reason left unused",reason);
				ec.deleteObject(reason);
			}*/
			logger.log(WOLogLevel.UNOWNED_EDITING,"Deleting substitute",substitute);
			ec.deleteObject(substitute);
		}
		return done("deleted");
	}
	
	protected WOActionResults done(String action) {
		EOEditingContext ec = lesson.editingContext();
		try {
			ec.saveChanges();
			Object[] args = new Object[] {session(),lesson};
			logger.log(WOLogLevel.UNOWNED_EDITING,"Substitute for lesson " + action,args);
		} catch (NSValidation.ValidationException vex) {
			ec.revert();
			String message = vex.getMessage();
			session().takeValueForKey(vex.getMessage(), "message");
			Object[] args = new Object[] {session(),lesson,message};
			logger.log(WOLogLevel.FINE,"Failed to save "+ action + " Substitute for lesson ",args);
		} catch (Exception e) {
			ec.revert();
			session().takeValueForKey(e.getMessage(), "message");
			Object[] args = new Object[] {session(),lesson,e};
			logger.log(WOLogLevel.WARNING,"Failed to save "+ action + " Substitute for lesson ",args);
		} finally {
			ec.unlock();
		}
		session().removeObjectForKey("lessonProperies");
		if (returnPage instanceof WOComponent) 
			((WOComponent)returnPage).ensureAwakeInContext(context());
		return returnPage;
	}
 }