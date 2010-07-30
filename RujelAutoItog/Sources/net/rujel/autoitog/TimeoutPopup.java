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
import net.rujel.base.SettingsBase;
import net.rujel.eduresults.ItogContainer;
import net.rujel.eduresults.ItogMark;
import net.rujel.interfaces.*;

import com.webobjects.foundation.*;
import com.webobjects.appserver.*;
import com.webobjects.eoaccess.EOUtilities;
import com.webobjects.eocontrol.*;

import java.util.logging.Logger;
import net.rujel.reusables.WOLogLevel;

public class TimeoutPopup extends WOComponent {
	protected static Logger logger = Logger.getLogger("rujel.autoitog");

	public PrognosesAddOn addOn;
	public EduCourse course;
	public ItogContainer eduPeriod;
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

    public void appendToResponse(WOResponse aResponse, WOContext aContext) {
    	if(dueDate == null) {
    		if(addOn != null && addOn.periodItem != null &&
    				addOn.periodItem.itogContainer() == eduPeriod) {
    			dueDate = addOn.periodItem.fireDate();
    		} else {
    			String listName = SettingsBase.stringSettingForCourse(
    					ItogMark.ENTITY_NAME, course, course.editingContext());
    			AutoItog ai = AutoItog.forListName(listName, eduPeriod);
    			dueDate = ai.fireDate();
    		}
			dueDate = dueDate.timestampByAddingGregorianUnits(0, 0, 7, 0, 0, 0);
    	}
    	super.appendToResponse(aResponse, aContext);
    }
    
	public void setTimeout (Timeout value) {
		timeout = value;
		String toClass = null;
		if(timeout == null) {
			toClass = (prognosis == null)?"CourseTimeout":"StudentTimeout";
		} else {
			if(timeout instanceof StudentTimeout) {
				toClass = "StudentTimeout";
				if(prognosis == null) {
					if(course != null) {
						prognosis = Prognosis.getPrognosis(
								((StudentTimeout)timeout).student(), course, 
								eduPeriod, false);
					} else {
						NSArray prognoses = timeout.relatedPrognoses();
						if(prognoses != null && prognoses.count() > 0)
							prognosis = (Prognosis)prognoses.objectAtIndex(0);
					}
				}
				if(course == null && prognosis != null)
					course = prognosis.course();
			} else {
				toClass = "CourseTimeout";
			}
		}
		NamedFlags access = (NamedFlags)session().valueForKeyPath("readAccess.FLAGS." + toClass);
		if(timeout != null) {
//			eduPeriod = timeout.eduPeriod();
			forCourse = (timeout.course() != null);
			if(timeout instanceof CourseTimeout) {
				CourseTimeout cto = (CourseTimeout)timeout;
				forTeacher = (cto.teacher() != null);
				forCycle = (cto.cycle() != null);
				forEduGroup = (cto.eduGroup() != null);
			}
			dueDate = timeout.fireDate();
			reason = timeout.reason();
			flags.setFlags(timeout.flags().intValue());
			readOnly = !access.flagForKey("edit");
		} else {
			readOnly = !access.flagForKey("create");
		}
	}
	
	public void setEduPeriod(Object period) {
		if(period instanceof ItogContainer) {
			eduPeriod = (ItogContainer)period;
		} else if (period instanceof AutoItog) {
			eduPeriod = ((AutoItog)period).itogContainer();
			dueDate = ((AutoItog)period).fireDate();
			dueDate = dueDate.timestampByAddingGregorianUnits(0, 0, 7, 0, 0, 0);
		}
	}

    public String teacherName() {
        return Person.Utility.fullName(course.teacher().person(),true,2,1,1);
    }

    public String title() {
    	if(prognosis == null)
    		return (String)valueForKeyPath("application.strings.RujelAutoItog_AutoItog.properties.CourseTimeout.this");
    	StringBuffer buf = new StringBuffer((String)valueForKeyPath("application.strings.RujelAutoItog_AutoItog.properties.Timeout.this"));
    	buf.append(" - ");
    	buf.append(course.cycle().subject());
       	buf.append(" - ");
       	buf.append(Person.Utility.fullName(prognosis.student(),true,2,1,1));
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
					if(timeout == null || (forCourse && timeout.course() == null)) {
						String entity = (prognosis==null)?"CourseTimeout":"StudentTimeout";
						if(timeout == null || !timeout.fireDate().equals(dueDate)) {
							timeout = (Timeout)EOUtilities.createAndInsertInstance(ec, entity);
							timeout.addObjectToBothSidesOfRelationshipWithKey(
									eduPeriod, "itogContainer");
							related = null;
						}
						timeout.setCourse((forCourse)?course:null);
						if(prognosis != null) {
							timeout.takeValueForKey(prognosis.student(),"student");
							//prognosis.addObjectToPropertyWithKey(timeout,(forCourse)?"timeout":"generalTimeout");
						}
					} else	if(!forCourse) {
						timeout.setCourse(null);
						/*
						if(prognosis != null && timeout.eduCourse() != null) {
							prognosis.removeObjectFromBothSidesOfRelationshipWithKey(timeout, "timeout");
							prognosis.addObjectToPropertyWithKey(timeout,"generalTimeout");
						}*/
					}
					timeout.setFireDate(dueDate);
					timeout.setReason(reason);
					timeout.namedFlags().setFlags(flags.intValue());
					if(prognosis == null) {
						timeout.takeValueForKey((forTeacher)?course.teacher():null,"teacher");
						timeout.takeValueForKey((forCycle)?course.cycle():null,"cycle");
						timeout.takeValueForKey((forEduGroup)?course.eduGroup():null,"eduGroup");
					}
	    			String listName = SettingsBase.stringSettingForCourse(
	    					ItogMark.ENTITY_NAME, course, ec);
	    			AutoItog ai = AutoItog.forListName(listName, eduPeriod);
					dueDate = AutoItog.combineDateAndTime(dueDate, ai.fireTime());
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
				logger.logp(WOLogLevel.EDITING,getClass().getName(),"save","Timeout is changed",new Object[] {session(),timeout});
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
