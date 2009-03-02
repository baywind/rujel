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
import net.rujel.interfaces.*;
//import net.rujel.eduresults.*;

import com.webobjects.foundation.*;
import com.webobjects.appserver.*;
import com.webobjects.eocontrol.*;
import com.webobjects.eoaccess.*;

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
		if(itog == null || !mark.equals(itog.mark())) {
			EduCourse eduCourse = (EduCourse)addOn.valueForKey("eduCourse");
			EOEditingContext ec = eduCourse.editingContext();
			ec.lock();
			try {
				boolean newItog = (itog == null);
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
					addOn.takeValueForKey(null,"agregate");
				} else {
					itog.readFlags().setFlagForKey(true,"changed");
					//itog.setValue(null);
				}
				itog.setMark(mark);
				if(ifArchive) {
					EOEnterpriseObject archive = EOUtilities.createAndInsertInstance(ec,"MarkArchive");
					archive.takeValueForKey(itog, "object");
					archive.takeValueForKey(changeReason, "reason");
				}
				//}

				ec.saveChanges();
				String message = (newItog)?"New Itog created":"Itog is changed";
				logger.logp(WOLogLevel.UNOWNED_EDITING,getClass().getName(),"save",message,new Object[] {session(),itog});
			} catch (Exception ex) {
				logger.logp(WOLogLevel.WARNING,getClass().getName(),"save","Failed to save itog",new Object[] {session(),itog,ex});
				session().takeValueForKey(ex.getMessage(),"message");
			} finally {
				ec.unlock();
			}
		}
		return returnPage;
	}
	
	public WOComponent delete() {
		//WOComponent returnPage = (WOComponent)addOn.valueForKey("returnPage");
		returnPage.ensureAwakeInContext(context());
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
		} catch (Exception ex) {
			logger.logp(WOLogLevel.WARNING,getClass().getName(),"delete","Failed to delete itog",new Object[] {session(),itog,ex});
			session().takeValueForKey(ex.getMessage(),"message");
			ec.revert();
		} finally {
			ec.unlock();
		}
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
		EduCourse eduCourse = (EduCourse)addOn.valueForKey("eduCourse");
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
