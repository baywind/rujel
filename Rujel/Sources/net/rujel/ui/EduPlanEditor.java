// EduPlanEditor.java: Class file for WO Component 'EduPlanEditor'

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

import net.rujel.interfaces.*;
import net.rujel.reusables.*;
import net.rujel.auth.*;

import com.webobjects.foundation.*;
import com.webobjects.appserver.*;
import com.webobjects.eocontrol.*;
import com.webobjects.eoaccess.EOUtilities;
import java.util.logging.Logger;
import net.rujel.reusables.WOLogLevel;

public class EduPlanEditor extends WOComponent {
	protected static Logger logger = Logger.getLogger("rujel.journal");
	
	public Number gradeItem;
	public Integer currGrade;
	/** @TypeInfo java.lang.Number */
	public NSArray gradeList;
	
	public EOEditingContext ec;

    /** @TypeInfo EduCycle */
    public NSArray cycles;
	public EduCycle cycleItem;
	public EduCycle currCycle;
	
	protected NamedFlags _access;
	public static final NSArray accessKeys = new NSArray(new Object[] {
		"read","create","edit","delete"});
	
	public NamedFlags access() {
		if (_access == null) return DegenerateFlags.ALL_TRUE;
		return (NamedFlags)_access.immutableClone();
	}
	
    public EduPlanEditor(WOContext context) {
        super(context);
		NSArray gradeSorter = new NSArray(EOSortOrdering.sortOrderingWithKey("grade",EOSortOrdering.CompareAscending));
		NSArray attribList = new NSArray("grade");
		
		UserPresentation user = (UserPresentation)session().valueForKey("user");
		try {
			int acc = user.accessLevel(EduCycle.entityName);
			if (acc == 0) throw new AccessHandler.UnlistedModuleException("Zero access");
			_access = new NamedFlags(acc,accessKeys);
		} catch (AccessHandler.UnlistedModuleException e) {
			try {
				_access = new NamedFlags(user.accessLevel("EduPlanEditor"),accessKeys);
			} catch (AccessHandler.UnlistedModuleException e1) {
				logger.logp(WOLogLevel.CONFIG,"EduPlanEditor","<init>","Can't get accessLevel",session());
				_access = DegenerateFlags.ALL_TRUE;
			}
		}
		
		ec = new SessionedEditingContext(session());
		ec.lock();
		EOFetchSpecification fspec = new EOFetchSpecification(EduCycle.entityName,null,gradeSorter);
		fspec.setRawRowKeyPaths(attribList);
		fspec.setUsesDistinct(true);
		ec.unlock();
		NSArray tmp = ec.objectsWithFetchSpecification(fspec);
		gradeList = (NSArray)tmp.valueForKey("grade");
		gradeItem = (Number)gradeList.objectAtIndex(0);
		selectGrade();
    }
	
    public String title() {
        return (String)valueForKeyPath("application.strings.RujelInterfaces_Names.EduCycle.this");
    }	
	
	public void selectGrade() {
		currGrade = (gradeItem instanceof Integer)?(Integer)gradeItem:new Integer(gradeItem.intValue());
		currCycle = null;
		cycles = EOUtilities.objectsMatchingKeyAndValue(ec,EduCycle.entityName,"grade",currGrade);
	}

    public Integer newGrade;
	
    public void makeNewGrade() {
		if(newGrade == null) return;
		Number max = (Number)gradeList.valueForKeyPath("@max.intValue");
		Number min = (Number)gradeList.valueForKeyPath("@min.intValue");
//		gradeItem = null;
		if(newGrade.intValue() >= min.intValue() && newGrade.intValue() <= max.intValue()) {
			java.util.Enumeration enumerator = gradeList.objectEnumerator();
			while (enumerator.hasMoreElements()) {
				gradeItem = (Number)enumerator.nextElement();
				if (gradeItem.intValue() == newGrade.intValue()) {
					selectGrade();
					newGrade = null;
					return;
				}
			}
			
		}
//		if(gradeItem == null) {
			gradeList = gradeList.arrayByAddingObject(newGrade);
			currGrade = newGrade;
			cycles = new NSArray();
//		}
		newGrade = null;
    }
	
	public void selectCycle () {
		if(access().flagForKey("edit"))
			currCycle = cycleItem;
		else
			session().takeValueForKey(valueForKeyPath("application.strings.Strings.messages.noAccess"),"message");
	}
	
	public String subject;
	public void addCycle() {
		if(subject == null) return;
		ec.lock();
		currCycle = (EduCycle)EOUtilities.createAndInsertInstance(ec,EduCycle.entityName);
		currCycle.setGrade(currGrade);
		currCycle.setSubject(subject);
		cycles = cycles.arrayByAddingObject(currCycle);
		ec.unlock();
		subject = null;
	}

    public void save() {
		if(ec.hasChanges()) {
			boolean newCycle = ec.insertedObjects().containsObject(currCycle);
			if(newCycle) {
				if(!access().flagForKey("create")) {
					session().takeValueForKey(valueForKeyPath("application.strings.Strings.messages.noAccess"),"message");
					logger.logp(WOLogLevel.OWNED_EDITING,"EduPlanEditor","save","Denied to create new cycle",session());
					return;
				}
			} else {
				Boolean allowEdit = (Boolean)currCycle.valueForKeyPath("access.edit");
				if(!((allowEdit == null && access().flagForKey("edit")) || (allowEdit != null && allowEdit.booleanValue()))) {
					session().takeValueForKey(valueForKeyPath("application.strings.Strings.messages.noAccess"),"message");
					logger.logp(WOLogLevel.OWNED_EDITING,"EduPlanEditor","save","Denied cycle editing",new Object[] {session(),currCycle});
					return;
				}
			}				
			ec.lock();
			try {
				ec.saveChanges();
				if(newCycle) { //log creation
					logger.logp(WOLogLevel.UNOWNED_EDITING,"EduPlanEditor","save","Created new cycle",new Object[] {session(),currCycle});
					NSNotificationCenter.defaultCenter().postNotification(net.rujel.auth.AccessHandler.ownNotificationName,session().valueForKey("user"),new NSDictionary(currCycle,"EO"));
				} else { //log change
					WOLogLevel level = WOLogLevel.UNOWNED_EDITING;
					if(currCycle instanceof UseAccess && ((UseAccess)currCycle).isOwned())
						level = WOLogLevel.OWNED_EDITING;
					logger.logp(level,"EduPlanEditor","save","Saved changes in cycle",new Object[] {session(),currCycle});
				}
				currCycle = null;
			} catch (NSValidation.ValidationException vex) {
				session().takeValueForKey(vex.getMessage(),"message");
				logger.logp(WOLogLevel.FINER,"EduPlanEditor","save","Falied save cycle",new Object[] {session(),currCycle,vex});
			}
			ec.unlock();
		}
	}
	
	public void delete() {
		ec.lock();
		ec.deleteObject(currCycle);
		try {
			if(ec.hasChanges()) {
				ec.saveChanges();
			}
			currCycle = null;
		} catch (NSValidation.ValidationException vex) {
			session().takeValueForKey(vex.getMessage(),"message");
		}
		cycles = EOUtilities.objectsMatchingKeyAndValue(ec,EduCycle.entityName,"grade",currGrade);
		ec.unlock();
	}
	
	public String gradeRowStyle() {
		int grade = gradeItem.intValue();
		if(currGrade != null && grade == currGrade.intValue())
			return "selection";
		if(grade%2==0)
			return "gerade";
		else
			return "ungerade";
	}
	/*
	public String cycleRowStyle() {
		if(cycleItem.equals(currCycle)) {
			return "selection";
		}
		return "grey";
	}
	*/
}