// ItogPopup.java

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

package net.rujel.eduresults;


import net.rujel.reusables.*;
import net.rujel.base.MyUtility;
import net.rujel.interfaces.*;
//import net.rujel.eduresults.*;

import com.webobjects.foundation.*;
import com.webobjects.appserver.*;
import com.webobjects.eocontrol.*;
import com.webobjects.eoaccess.*;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.logging.Logger;
import net.rujel.reusables.WOLogLevel;

public class ItogPopup extends WOComponent {
	protected static Logger logger = Logger.getLogger("rujel.itog");
	public ItogMark itog;

    public EduPeriod eduPeriod;
	//public EduCycle cycle;
    public Student student;
    public String mark;
	public NSKeyValueCoding addOn;
	public WOComponent returnPage;
	public String changeReason;
	public final boolean ifArchive = SettingsReader.boolForKeyPath("markarchive.ItogMark", false);

	public ItogPopup(WOContext context) {
		super(context);       
	}

	public String mark() {
		if(itog != null) {
			mark = itog.mark();
		} else {
			mark = null;
		}
		return mark;
	}

	public NSArray flaglist() {
		if(mark == null) return null;
		return ItogMark.flagKeys;
	}

	public static final NSDictionary flagNames = (NSDictionary)WOApplication.application().valueForKeyPath("strings.RujelEduResults_EduResults.properties.ItogMark.flags");
	public Object item;

	public boolean flagStatus () {
		return itog.readFlags().flagForKey(item);
	}
	public void setFlagStatus(boolean status) {
		itog.readFlags().setFlagForKey(status,item);
	}

	public String flagName() {
		return (String)flagNames.valueForKey(item.toString());
	}

	public NamedFlags access() {
		if(itog != null)
			return (NamedFlags)session().valueForKeyPath("readAccess.FLAGS.itog");
		return (NamedFlags)addOn.valueForKey("access");
	}

	public WOComponent save() {
		//WOComponent returnPage = (WOComponent)addOn.valueForKey("returnPage");
		returnPage.ensureAwakeInContext(context());
		if(itog == null && mark == null)
			return returnPage;
		if(mark == null)
			return delete();

		if(!access().flagForKey("edit")) {
			session().takeValueForKey(valueForKeyPath("application.strings.Strings.messages.noAccess"),"message");
			return returnPage;
		}
		boolean newItog = (itog == null);
		boolean same = (!newItog && mark.equals(itog.mark()));
		if(newItog || !same || itog.readFlags().flagForKey("constituents")) {
			EduCourse eduCourse = (addOn == null)?itog.assumeCourse():
				(EduCourse)addOn.valueForKey("eduCourse");
			EOEditingContext ec = (eduCourse==null)?itog.editingContext():eduCourse.editingContext();
			ec.lock();
			EOEnterpriseObject prognosis = null;
			try {
				Class prClass = Class.forName("net.rujel.autoitog.Prognosis");
				Method meth = prClass.getMethod("getPrognosis", Student.class,EduCourse.class, EduPeriod.class, Boolean.TYPE);
				prognosis = (EOEnterpriseObject)meth.invoke(null, student,eduCourse,eduPeriod,Boolean.FALSE);
			} catch (Exception e) {
				// cant get corresponding prognosis
			}
			try {
				/*boolean enableArchive = SettingsReader.boolForKeyPath("markarchive.enable", false);
			if(mark == null) {
				if(enableArchive) {
					EOEnterpriseObject archive = EOUtilities.createAndInsertInstance(ec,"MarkArchive");
					archive.takeValueForKey(itog, "objectIdentifier");
					archive.takeValueForKey(".", '@' + "mark");
				}
				ec.deleteObject(itog);
			} else {*/
				if(newItog) {
					itog = (ItogMark)EOUtilities.createAndInsertInstance(ec,"ItogMark");
					itog.addObjectToBothSidesOfRelationshipWithKey(eduPeriod,"eduPeriod");
					itog.addObjectToBothSidesOfRelationshipWithKey(student,"student");
					itog.addObjectToBothSidesOfRelationshipWithKey(eduCourse.cycle(),"cycle");
					if(addOn != null)
						addOn.takeValueForKey(null,"agregate");
				}
				if(!same) {
					if(!newItog)
						itog.readFlags().setFlagForKey(true,"changed");
					itog.setMark(mark);
					itog.readFlags().setFlagForKey(true,"manual");
				}
				itog.readFlags().setFlagForKey(false,"constituents");
				if(prognosis != null) {
					boolean flag = true;
					BigDecimal value = (BigDecimal)prognosis.valueForKey("value");
					itog.setValue(value);
					if(value != null) {
						String fromValue = (String)prognosis.valueForKey("markFromValue");
						if(fromValue != null)
							flag = !mark.equals(fromValue);
					}
					itog.readFlags().setFlagForKey(flag,"forced");
					flag = !Various.boolForObject(prognosis.valueForKey("isComplete"));
					itog.readFlags().setFlagForKey(flag,"incomplete");
//				} else {
//					itog.readFlags().setFlagForKey(false,"forced");
//					itog.readFlags().setFlagForKey(false,"incomplete");
//					itog.setValue(null);
				}
				if(ifArchive) {
					EOEnterpriseObject archive = EOUtilities.createAndInsertInstance(ec,"MarkArchive");
					archive.takeValueForKey(itog, "object");
					if(changeReason == null && same)
						archive.takeValueForKey(flagNames.valueForKey("constituents"),"reason");
					else
						archive.takeValueForKey(changeReason, "reason");
					if(!same && !itog.readFlags().flagForKey("changed")) {
						Integer count = (Integer)archive.valueForKey("archivesCount");
						itog.readFlags().setFlagForKey(count.intValue() > 0,"changed");
					}
				}
				//}

				ec.saveChanges();
				String message = (newItog)?"New Itog created":"Itog is changed";
				logger.logp(WOLogLevel.UNOWNED_EDITING,getClass().getName(),"save",message,new Object[] {session(),itog});
				if (!same) {
					stat(eduCourse, ec);
				}
			} catch (Exception ex) {
				logger.logp(WOLogLevel.WARNING,getClass().getName(),"save","Failed to save itog",new Object[] {session(),itog,ex});
				session().takeValueForKey(ex.getMessage(),"message");
			} finally {
				ec.unlock();
			}
		}
		return returnPage;
	}
	
	private void stat(EduCourse eduCourse, EOEditingContext ec) {
		EOEnterpriseObject grouping = ModuleInit.getStatsGrouping(eduCourse, eduPeriod);
		if (grouping != null) {
//			EOEditingContext ec = eduCourse.editingContext();
			NSArray itogs = ItogMark.getItogMarks(eduCourse.cycle(), eduPeriod, null, ec);
			itogs = MyUtility.filterByGroup(itogs, "student", eduCourse.groupList(), true);
			grouping.takeValueForKey(itogs, "array");
//			NSDictionary stats = ModuleInit.statCourse(eduCourse,eduPeriod);
//			grouping.takeValueForKey(stats, "dict");
			try {
				ec.saveChanges();
			} catch (Exception e) {
				logger.log(WOLogLevel.WARNING,"Failed to save itog Stats for course",
						new Object[] { eduCourse, e });
				ec.revert();
			}
		}
	}
	
	public WOComponent delete() {
		//WOComponent returnPage = (WOComponent)addOn.valueForKey("returnPage");
		returnPage.ensureAwakeInContext(context());
		if(itog == null)
			return returnPage;
		if(!access().flagForKey("delete")) {
			session().takeValueForKey(valueForKeyPath("application.strings.Strings.messages.noAccess"),"message");
			return returnPage;
		}
		EOEditingContext ec = student.editingContext();
		ec.lock();
		try {
			NSDictionary pKey = EOUtilities.primaryKeyForObject(ec,itog);
			if(ifArchive) {
				EOEnterpriseObject archive = EOUtilities.createAndInsertInstance(ec,"MarkArchive");
				archive.takeValueForKey(itog, "objectIdentifier");
				archive.takeValueForKey(".", '@' + "mark");
				archive.takeValueForKey(changeReason, "reason");
			}
			ec.deleteObject(itog);
			ec.saveChanges();
			logger.logp(WOLogLevel.UNOWNED_EDITING,getClass().getName(),"delete","Itog is deleted",new Object[] {session(),pKey});
			EduCourse eduCourse = (addOn == null)?itog.assumeCourse():
				(EduCourse)addOn.valueForKey("eduCourse");
			stat(eduCourse, ec);
		} catch (Exception ex) {
			logger.logp(WOLogLevel.WARNING,getClass().getName(),"delete","Failed to delete itog",new Object[] {session(),itog,ex});
			session().takeValueForKey(ex.getMessage(),"message");
			ec.revert();
		} finally {
			ec.unlock();
		}
		if(addOn != null)
			addOn.takeValueForKey(null,"agregate");
		return returnPage;
	}

	private transient Boolean readOnly;

	public boolean readOnly() {
		if(readOnly != null) return readOnly.booleanValue();
		if(access() == null || access().flagForKey("edit")) {
			readOnly = Boolean.FALSE;
			return false;
		} 
		if(access().flagForKey("create") && itog==null) {
			readOnly = Boolean.FALSE;
			return false;
		}
		readOnly = Boolean.TRUE;
		return true;
	}
	
	public NSMutableDictionary identifierDictionary() {
		EduCourse eduCourse = (addOn == null)?itog.assumeCourse():
				(EduCourse)addOn.valueForKey("eduCourse");
		if(student == null || eduPeriod == null || eduCourse == null)
    		return null;
		NSMutableDictionary ident = new NSMutableDictionary("ItogMark","entityName");
		ident.takeValueForKey(eduPeriod,"period");
		ident.takeValueForKey(student, "student");
		ident.takeValueForKey(eduCourse.cycle(),"eduCycle");
		ident.takeValueForKey(eduCourse.editingContext(), "editingContext");
		return ident;
    }

	public String onkeypress() {
		if(!ifArchive)
			return null;
		return "showObj('itogChangeReason');form.changeReason.onkeypress();fitWindow();";
	}
}
