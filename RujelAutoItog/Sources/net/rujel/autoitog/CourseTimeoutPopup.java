// CourseTimeoutPopup.java: Class file for WO Component 'CourseTimeoutPopup'

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

package net.rujel.autoitog;

import net.rujel.reusables.*;
import net.rujel.interfaces.*;
import net.rujel.eduresults.*;

import com.webobjects.foundation.*;
import com.webobjects.appserver.*;
import com.webobjects.eocontrol.*;
import com.webobjects.eoaccess.*;
import java.util.logging.Logger;
import java.util.GregorianCalendar;
import net.rujel.reusables.WOLogLevel;

public class CourseTimeoutPopup extends WOComponent {
	protected static Logger logger = Logger.getLogger("rujel.autoitog");

	public WOComponent returnPage;
	public EduCourse course;
	//public EduPeriod eduPeriod;
	
	public NSTimestamp dueDate;
	public String reason;
	
	public boolean forCourse = true;
	public boolean forTeacher = false;
	public boolean forCycle = false;
	public boolean forEduGroup =false;
	
	public boolean readOnly = false;
	
	public NSKeyValueCoding addOn;
	protected CourseTimeout timeout;
	
    public CourseTimeoutPopup(WOContext context) {
        super(context);
    }
	
	protected EduPeriod _period;
	public EduPeriod eduPeriod() {
		if(_period == null) {
			_period = (EduPeriod)addOn.valueForKey("eduPeriod");
			if(_period == null) {
				NSArray pertypes = PeriodType.periodTypesForCourse(course);
				if(pertypes == null) return null;
				PeriodType pertype = (PeriodType)pertypes.objectAtIndex(0);
				_period = pertype.currentPeriod();
			}
		}
		return _period;
	}

	public void setTimeout (CourseTimeout value) {
		timeout = value;
		NamedFlags access = (NamedFlags)session().valueForKeyPath("readAccess.FLAGS.CourseTimeout");
		if(timeout != null) {
			_period = timeout.eduPeriod();
			forCourse = (timeout.eduCourse() != null);
			forTeacher = (timeout.teacher() != null);
			forCycle = (timeout.cycle() != null);
			forEduGroup = (timeout.eduGroup() != null);
			dueDate = timeout.dueDate();
			reason = timeout.reason();
			readOnly = !access.flagForKey("edit");
		} else {
			readOnly = !access.flagForKey("create");
			GregorianCalendar cal = new GregorianCalendar();
			cal.setTime(eduPeriod().end());
			cal.add(GregorianCalendar.DATE,7);
			dueDate = new NSTimestamp(cal.getTime());
		}
	}
	
	public WOComponent save() {
		EOEditingContext ec = course.editingContext();
		ec.lock();
		if(timeout == null || (forCourse && timeout.eduCourse() == null)) {
			timeout = (CourseTimeout)EOUtilities.createAndInsertInstance(ec,"CourseTimeout");
			timeout.setEduPeriod(eduPeriod());
		}
		
			timeout.setDueDate(dueDate);
			timeout.setReason(reason);
			if(forCourse) {
				timeout.setEduCourse(course);
			} else {
				timeout.setEduCourse(null);
				timeout.setCycle((forCycle)?course.cycle():null);
				timeout.setEduGroup((forEduGroup)?course.eduGroup():null);
				timeout.setTeacher((forTeacher)?course.teacher():null);
			}
			
		try {
			ec.saveChanges();
			logger.logp(WOLogLevel.UNOWNED_EDITING,getClass().getName(),"save","CourseTimeout is changed",new Object[] {session(),timeout});
		} catch (Exception ex) {
			logger.logp(WOLogLevel.WARNING,getClass().getName(),"save","Failed to save CourseTimeout",new Object[] {session(),timeout,ex});
			session().takeValueForKey(ex.getMessage(),"message");
		} finally {
			ec.unlock();
		}
		addOn.takeValueForKey(timeout,"courseTimeout");
		returnPage.ensureAwakeInContext(context());
		return returnPage;
	}
	
	public WOComponent delete() {
		if(timeout != null) {
			EOEditingContext ec = timeout.editingContext();
			ec.lock();
			ec.deleteObject(timeout);
			try {
				logger.logp(WOLogLevel.UNOWNED_EDITING,getClass().getName(),"delete","Deleting CourseTimeout",new Object[] {session(),timeout});
				ec.saveChanges();
			} catch (Exception ex) {
				logger.logp(WOLogLevel.WARNING,getClass().getName(),"delete","Failed to delete CourseTimeout",new Object[] {session(),timeout,ex});
				session().takeValueForKey(ex.getMessage(),"message");
			} finally {
				ec.unlock();
			}
			addOn.takeValueForKey(null,"courseTimeout");
		}
		returnPage.ensureAwakeInContext(context());
		return returnPage;		
	}
	
    public String teacherName() {
        return Person.Utility.fullName(course.teacher().person(),true,2,1,1);
    }
}
