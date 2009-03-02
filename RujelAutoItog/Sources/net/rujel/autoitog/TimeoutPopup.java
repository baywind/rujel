// TimeoutPopup.java: Class file for WO Component 'TimeoutPopup'

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
import net.rujel.eduresults.EduPeriod;
import net.rujel.interfaces.*;

import com.webobjects.foundation.*;
import com.webobjects.appserver.*;
import com.webobjects.eoaccess.EOUtilities;
import com.webobjects.eocontrol.*;

import java.util.GregorianCalendar;
import java.util.logging.Logger;
import net.rujel.reusables.WOLogLevel;

public class TimeoutPopup extends WOComponent {
	protected static Logger logger = Logger.getLogger("rujel.autoitog");

	public PrognosesAddOn addOn;
	public EduCourse course;
	public EduPeriod eduPeriod;
	public Timeout timeout;
	public Prognosis prognosis;
//	public Student student;
	
	public NSTimestamp dueDate;
	public String reason;
	
	public boolean forCourse = true;
	public boolean forTeacher = false;
	public boolean forCycle = false;
	public boolean forEduGroup =false;
	public NamedFlags flags = new NamedFlags(Timeout.flagNames);
	
	public boolean readOnly = false;
	
 	
//	public WOComponent caller;
	public WOComponent returnPage;

    public TimeoutPopup(WOContext context) {
        super(context);
    }
	
	public void setTimeout (Timeout value) {
		timeout = value;
		String toClass = (prognosis == null)?"CourseTimeout":"StudentTimeout";
		NamedFlags access = (NamedFlags)session().valueForKeyPath("readAccess.FLAGS." + toClass);
		if(timeout != null) {
			eduPeriod = timeout.eduPeriod();
			forCourse = (timeout.eduCourse() != null);
			if(timeout instanceof CourseTimeout) {
				CourseTimeout cto = (CourseTimeout)timeout;
				forTeacher = (cto.teacher() != null);
				forCycle = (cto.cycle() != null);
				forEduGroup = (cto.eduGroup() != null);
			}
			dueDate = timeout.dueDate();
			reason = timeout.reason();
			flags.setFlags(timeout.flags().intValue());
			readOnly = !access.flagForKey("edit");
		} else {
			readOnly = !access.flagForKey("create");
		}
	}
	
	public void setEduPeriod(EduPeriod period) {
		eduPeriod = period;
		GregorianCalendar cal = new GregorianCalendar();
		cal.setTime(eduPeriod.end());
		cal.add(GregorianCalendar.DATE,7);
		dueDate = new NSTimestamp(cal.getTime());
	}

    public String teacherName() {
        return Person.Utility.fullName(course.teacher().person(),true,2,1,1);
    }

    public String title() {
    	if(prognosis == null)
    		return (String)valueForKeyPath("application.strings.RujelAutoItog_AutoItog.properties.CourseTimeout.this");
    	Person student = prognosis.student().person();
    	StringBuffer buf = new StringBuffer((String)valueForKeyPath("application.strings.RujelAutoItog_AutoItog.properties.Timeout.this"));
    	buf.append(" - ");
    	buf.append(course.cycle().subject());
       	buf.append(" - ");
       	buf.append(Person.Utility.fullName(student,true,2,1,1));
       	return  buf.toString();
    }
	
	public WOComponent save() {
		if(timeout != null || dueDate != null) {
			EOEditingContext ec = course.editingContext();
			ec.lock();
			try {
				NSArray related = (timeout == null)?null:timeout.relatedPrognoses();
				if(dueDate == null) {
					//prognosis.removeObjectFromBothSidesOfRelationshipWithKey(timeout,(forCourse)?"timeout":"generalTimeout");
					ec.deleteObject(timeout);
				} else {
					if(timeout == null || (forCourse && timeout.eduCourse() == null)) {
						String entity = (prognosis==null)?"CourseTimeout":"StudentTimeout";
						if(timeout == null || !timeout.dueDate().equals(dueDate)) {
							timeout = (Timeout)EOUtilities.createAndInsertInstance(ec, entity);
							timeout.takeValueForKey(eduPeriod, "eduPeriod");
							related = null;
						}
						timeout.setEduCourse((forCourse)?course:null);
						if(prognosis != null) {
							timeout.takeValueForKey(prognosis.student(),"student");
							//prognosis.addObjectToPropertyWithKey(timeout,(forCourse)?"timeout":"generalTimeout");
						}
					} else	if(!forCourse) {
						timeout.setEduCourse(null);
						/*
						if(prognosis != null && timeout.eduCourse() != null) {
							prognosis.removeObjectFromBothSidesOfRelationshipWithKey(timeout, "timeout");
							prognosis.addObjectToPropertyWithKey(timeout,"generalTimeout");
						}*/
					}
					timeout.setDueDate(dueDate);
					timeout.setReason(reason);
					timeout.namedFlags().setFlags(flags.intValue());
					if(prognosis == null) {
						timeout.takeValueForKey((forTeacher)?course.teacher():null,"teacher");
						timeout.takeValueForKey((forCycle)?course.cycle():null,"cycle");
						timeout.takeValueForKey((forEduGroup)?course.eduGroup():null,"eduGroup");
					}
				}
						//NSArray params = new NSArray(new Object[] 
						//							 {timeout.valueForKey("eduPeriod"),timeout.valueForKey("student")});
				ec.saveChanges();
				if (dueDate != null && dueDate.compare(new NSTimestamp()) < 0) {
					logger.log(WOLogLevel.INFO, "FireDate is set earlier than current date", 
							new Object [] {session(),timeout});
				}
				if(prognosis == null)
					addOn.setCourseTimeout((dueDate==null)?null:(CourseTimeout)timeout);
				NSArray newRelated = (dueDate==null || timeout == null)? null 
						: timeout.relatedPrognoses();
				if(related == null && prognosis != null)
					related = timeout.relatedPrognoses();
				if(newRelated==null || newRelated.count() == 0) {
					if(related != null && related.count() > 0) {
						related.takeValueForKey(null,"updateWithCourseTimeout");
					}
				} else {
					if(timeout instanceof CourseTimeout)
						newRelated.takeValueForKey(timeout,"updateWithCourseTimeout");
					else 
						newRelated.takeValueForKey(null,"updateWithCourseTimeout");
					if(related != null && related.count() > 0) {
						NSMutableArray tmp = related.mutableClone();
						tmp.removeObjectsInArray(newRelated);
						if(tmp.count() > 0)
							tmp.takeValueForKey(null,"updateWithCourseTimeout");
					}
				}
				ec.saveChanges();
				logger.logp(WOLogLevel.UNOWNED_EDITING,getClass().getName(),"save","Timeout is changed",new Object[] {session(),timeout});
			} catch (Exception ex) {
				logger.logp(WOLogLevel.WARNING,getClass().getName(),"save","Failed to save timeout",new Object[] {session(),timeout,ex});
				session().takeValueForKey(ex.getMessage(),"message");
			} finally {
				ec.unlock();
			}
			//addOn.removeObjectForKey("timeouts");
		}
		returnPage.ensureAwakeInContext(context());
		return returnPage;
	}
	
	public WOComponent delete() {
		dueDate = null;
		return save();
	}
}
