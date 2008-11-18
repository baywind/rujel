// PeriodsList.java: Class file for WO Component 'PeriodsList'

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

import net.rujel.interfaces.*;
import com.webobjects.foundation.*;
import com.webobjects.appserver.*;
import com.webobjects.eocontrol.*;
import com.webobjects.eoaccess.EOUtilities;
import java.util.logging.Logger;
import net.rujel.reusables.WOLogLevel;

public class PeriodsList extends WOComponent {
	protected static Logger logger = Logger.getLogger("rujel.eduresults");

     public EduPeriod perItem;

    /** @TypeInfo PeriodType */
    public PeriodType pertypeItem;
	
    public PeriodsList(WOContext context) {
        super(context);
    }
	
	EduCourse _course;
	public EduCourse course() {
		if(_course == null) {
			_course = (EduCourse)valueForBinding("course");
		}
		return _course;
	}
	
	public String rowClass() {
		if(perItem == null) return null;
        Object curr = valueForBinding("currLesson");
		if(curr != null && curr.equals(perItem))
			return "selection";
		String result = perItem.periodType().color();
		if(result == null) result = "gerade";
		return result;
    }
	
	protected NSArray _pertypes;
    /** @TypeInfo PeriodType */
    public NSArray periodTypes() {
		if(_pertypes == null) {
			_pertypes = PeriodType.allPeriodTypes(course().editingContext(),course().eduYear());
		}
        return _pertypes; 
    }
	
	protected NSArray _usedTypes;
    public boolean usesPerType() {
        if(_usedTypes == null) {
			_usedTypes = PeriodType.periodTypesForCourse(course());
		}
		return (_usedTypes != null && _usedTypes.indexOfIdenticalObject(pertypeItem) != NSArray.NotFound);
    }
	
	protected NSMutableArray newtypes;
    public void setUsesPerType(boolean newUsesPerType) {
        if(newUsesPerType != usesPerType()) {
			if(newtypes == null) newtypes = new NSMutableArray();
			//NSDictionary dict = new NSDictionary(new Object[]{course(),pertypeItem},new Object[]{"course","periodType"});
			if(newUsesPerType) {
				EOEnterpriseObject cpt = EOUtilities.createAndInsertInstance(course().editingContext(),"CoursePeriodType");
				cpt.addObjectToBothSidesOfRelationshipWithKey(course(),"course");
				cpt.addObjectToBothSidesOfRelationshipWithKey(pertypeItem,"periodType");
				newtypes.addObject(pertypeItem.name() + " +");
			} else {
				NSArray cpts = EOUtilities.objectsWithQualifierFormat(course().editingContext(),"CoursePeriodType","course = %@ AND periodType = %@",new NSArray(new Object[]{course(),pertypeItem}));
				//objectsMatchingValues(course().editingContext(),"CoursePeriodType",dict);
				for (int i = 0; i < cpts.count(); i++) {
					course().editingContext().deleteObject((EOEnterpriseObject)cpts.objectAtIndex(i));
					newtypes.addObject(pertypeItem.name() + " -");
				}
			}
		}
    }

    public void usePerTypes() {
		EOEditingContext ec = course().editingContext();
		if(ec.hasChanges()) {
			try {
				ec.saveChanges();
				logger.logp(WOLogLevel.OWNED_EDITING,"PeriodsList","usePerTypes","Bound periods: " + newtypes,new Object[] {session(),course()});
			} catch (Exception ex) {
				session().takeValueForKey(ex.getMessage(),"message");
				logger.logp(WOLogLevel.OWNED_EDITING,"PeriodsList","usePerTypes","Failed to bind periods: " + newtypes,new Object[] {session(),course(),ex});
			}
		}
		_usedTypes = PeriodType.periodTypesForCourse(course());
		performParentAction("updateLessonList");
    }

    public void selectPeriod() {
		if(hasBinding("currLesson"))
			setValueForBinding(perItem,"currLesson");
		EOEditingContext ec = course().editingContext();
		if (ec.hasChanges()) ec.revert();
    }
	
	public void generatePeriods() {
		pertypeItem.generatePeriodsFromTemplates(course().eduYear());
		usePerTypes();
	}
	
	public boolean synchronizesVariablesWithBindings() {
        return false;
	}

		public boolean isStateless() {
		return true;
	}
	
	public void reset() {
		_course = null;
		_pertypes = null;
		_usedTypes = null;
	}
}
