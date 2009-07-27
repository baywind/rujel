// CriterSelector.java: Class file for WO Component 'CriterSelector'

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

import net.rujel.base.SettingsBase;
import net.rujel.criterial.*;
import net.rujel.interfaces.EduCourse;
import com.webobjects.foundation.*;
import com.webobjects.appserver.*;
import com.webobjects.eoaccess.EOUtilities;
import com.webobjects.eocontrol.*;

public class CriterSelector extends WOComponent {
	/*public static String workIntegral() {
		return SettingsReader.stringForKeyPath("edu.presenters.workIntegral","%");
	}*/

    public EOEnterpriseObject critItem;
    public int idx;

    public CriterSelector(WOContext context) {
        super(context);
    }
	
	private Integer _selection;
	protected Integer selection() {
		if(_selection == null) {
			if(hasBinding("selection"))
				_selection = (Integer)valueForBinding("selection");
			else
				_selection = (Integer)session().objectForKey("activeCriterion");
//			if(_selection == null) {
//				_selection = integral();
//				session().setObjectForKey(_selection,"activeCriterion");
//			}				
		}
		return _selection;//.toString();
	}
	
	/** @TypeInfo java.lang.EOEnterpriseObject */
	private NSArray _criteria;
	public NSArray criteria() {
		if(_criteria == null && (hasBinding("cycle") || hasBinding("course"))) {
				EduCourse course = (EduCourse)valueForBinding("course");
			_criteria = CriteriaSet.criteriaForCourse(course);//criteriaForSets(critSets);
		}
		return _criteria;
	}
	
    public String crClass() {
		if(selection() != null && selection().equals(critItem.valueForKey("criterion")))
			return "selection";
		return "orange";
    }
    private String _integral;
    public String integral() {
    	if(_integral == null) {
    		_integral = (String)valueForBinding("integralPresenter");
    		if(_integral == null) {
    			EduCourse course = (EduCourse)valueForBinding("course");
    			EOEditingContext ec = course.editingContext();
    			EOEnterpriseObject setting = SettingsBase.settingForCourse(
    					"presenters.workIntegral", course, ec);
    			if(setting != null) {
    				_integral = (String)setting.valueForKeyPath(SettingsBase.TEXT_VALUE_KEY);
    				if(_integral != null)
    					return _integral;
    				Integer pKey = (Integer)setting.valueForKey(SettingsBase.NUMERIC_VALUE_KEY);
    				if (pKey != null) {
    					BorderSet bSet = (BorderSet)EOUtilities.objectWithPrimaryKeyValue(
    							ec, BorderSet.ENTITY_NAME, pKey);
    					_integral = bSet.title();
    				}
    			}
    		}
    		if(_integral == null)
    			_integral = "%";
    	}
    	return _integral;
    }

    public String intClass() {
		if(selection() == null)
			return "selection";
		return "orange";
    }
	
    public String textClass() {
		if(selection() != null && selection().intValue() < 0)
			return "selection";
		return "orange";
    }
	/*
    public String fullClass() {
		if(selection().equals("full"))
			return "selection";
		return "orange";
    }*/
	
	public WOActionResults selectIntegral() {
		return select(null);
	}
	
	public WOActionResults selectCriter() {
		return select((Integer)critItem.valueForKey("criterion"));
	}
	
	public WOActionResults selectText() {
		return select(new Integer(-1));
	}
	
	public WOActionResults select(Integer sel) {
		if(hasBinding("selection")) {
			setValueForBinding(sel,"selection");
		} else {
			if(sel != null)
				session().setObjectForKey(sel,"activeCriterion");
			else
				session().removeObjectForKey("activeCriterion");
		}
		_selection = sel;
		return (WOActionResults)valueForBinding("selectAction");
	}
	

	public boolean synchronizesVariablesWithBindings() {
        return false;
	}
	public boolean isStateless() {
		return true;
	}
	
	public void reset() {
		_criteria = null;
		_selection = null;
		_integral = null;
	}
}
