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

import net.rujel.criterial.*;
import net.rujel.interfaces.EduCourse;
import net.rujel.interfaces.EduCycle;
import net.rujel.reusables.SettingsReader;
import com.webobjects.foundation.*;
import com.webobjects.appserver.*;
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
	
	private String _selection;
	protected String selection() {
		if(_selection == null) {
			if(hasBinding("selection"))
				_selection = (String)valueForBinding("selection");
			else
				_selection = (String)session().objectForKey("activeCriterion");
			if(_selection == null) {
				_selection = integral();
				session().setObjectForKey(_selection,"activeCriterion");
			}
				
		}
		return _selection;//.toString();
	}
	
	/** @TypeInfo java.lang.EOEnterpriseObject */
	private NSArray _criteria;
	public NSArray criteria() {
		if(_criteria == null && (hasBinding("cycle") || hasBinding("course"))) {
			EduCycle cycle = (EduCycle)valueForBinding("cycle");
			if(cycle == null) {
				EduCourse course = (EduCourse)valueForBinding("course");
				cycle = course.cycle();
			}
			NSArray critSets = CriteriaSet.critSetsForCycle(cycle);
			_criteria = CriteriaSet.criteriaForSets(critSets);
		}
		return _criteria;
	}
	/*
    public static NSArray criteriaForCourse(EOEnterpriseObject course) {
		NSArray sets = (NSArray)course.valueForKeyPath("cycle.cycleCritSets.criteriaSet");
		if(sets != null && sets.count() > 0) {
			CriteriaSet set = (CriteriaSet)sets.objectAtIndex(0);
			if(sets.count() > 1) {
				NSMutableArray result = set.sortedCriteria().mutableClone();
				for (int i = 1; i < sets.count(); i++) {
					set = (CriteriaSet)sets.objectAtIndex(i);
					result.addObjectsFromArray(set.sortedCriteria());
				}
				return result.immutableClone();
			} else {
				return set.sortedCriteria();
			}
		}
		return null;
	}*/
	
    public String crClass() {
		if(selection().equals(critItem.valueForKey("title")))
			return "selection";
		return "orange";
    }
    private String _integral;
    public String integral() {
    	if(_integral == null) {
    		_integral = (String)valueForBinding("integralPresenter");
    		if(_integral == null) {
    			FractionPresenter fs = (FractionPresenter)session().objectForKey("integralPresenter");
    			if(fs == null) { // ) 
    				_integral = SettingsReader.stringForKeyPath("edu.presenters.workIntegral","%");
    				EOEditingContext ec = EOSharedEditingContext.defaultSharedEditingContext();
    				//((EOEnterpriseObject)valueForBinding("cycle")).editingContext();
    				NSTimestamp today = (NSTimestamp)session().valueForKey("today");
    				fs = BorderSet.fractionPresenterForTitleAndDate(ec,_integral,today);
    				session().setObjectForKey(fs,"integralPresenter");
    			}
    			_integral = fs.title();
    		}
    	}
    	return _integral;
    }

    public String intClass() {
		if(selection().equals(integral()))
			return "selection";
		return "orange";
    }
	
    public String textClass() {
		if(selection().equals("text"))
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
		return select(integral());
	}
	
	public WOActionResults selectCriter() {
		return select((String)critItem.valueForKey("title"));
	}
	
	public WOActionResults selectText() {
		return select("text");
	}
	
	public WOActionResults select(String sel) {
		if(hasBinding("selection")) {
			setValueForBinding(sel,"selection");
		} else {
			session().setObjectForKey(sel,"activeCriterion");
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
