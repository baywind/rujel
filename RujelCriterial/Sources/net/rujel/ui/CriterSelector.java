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

import java.util.logging.Logger;

import net.rujel.base.Setting;
import net.rujel.base.SettingsBase;
import net.rujel.criterial.*;
import net.rujel.interfaces.EduCourse;
import net.rujel.interfaces.EduCycle;
import net.rujel.reusables.WOLogLevel;

import com.webobjects.foundation.*;
import com.webobjects.appserver.*;
import com.webobjects.eoaccess.EOUtilities;
import com.webobjects.eocontrol.*;

public class CriterSelector extends WOComponent {
	/*public static String workIntegral() {
		return SettingsReader.stringForKeyPath("edu.presenters.workIntegral","%");
	}*/

    public NSKeyValueCoding critItem;
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
			if(course != null) {
				_criteria = CriteriaSet.criteriaForCourse(course);//criteriaForSets(critSets);
			} else {
				EduCycle cycle = (EduCycle)valueForBinding("cycle");
				if(cycle != null) {
					Integer eduYear = (Integer)session().valueForKey("eduYear");
					EOEditingContext ec = cycle.editingContext();
					NSDictionary crs = SettingsBase.courseDict(cycle,eduYear);
					Setting setting = SettingsBase.settingForCourse(
							CriteriaSet.ENTITY_NAME, crs, ec);
					if(setting != null) {
						Integer set = setting.numericValue();
						if(set != null && set.intValue() > 0) {
							CriteriaSet critSet = (CriteriaSet)EOUtilities.
								objectWithPrimaryKeyValue(ec,CriteriaSet.ENTITY_NAME, set);
							_criteria = critSet.sortedCriteria();
						}
					}
				}
    			if(_criteria != null)
    				return _criteria;
				NSArray allLessons = (NSArray)valueForBinding("allLessons");
				if(allLessons != null) {
					try {
						int maxCrit = CriteriaSet.maxCriterionInWorks(allLessons);
						_criteria = CriteriaSet.criteriaForMax(maxCrit);
					} catch (RuntimeException e) {
						Logger.getLogger("rujel.criterial").log(WOLogLevel.WARNING,
								"Could not get criteria from works (lessons)",
								new Object[] {session(),e});
					}
				}
				if(_criteria == null)
					_criteria = NSArray.EmptyArray;
			}
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
    			NSKeyValueCodingAdditions c = (EduCourse)valueForBinding("course");
    			EOEditingContext ec = null;
    			if(c == null) {
    				EduCycle cycle = (EduCycle)valueForBinding("cycle");
    				Integer eduYear = (Integer)session().valueForKey("eduYear");
    				ec = cycle.editingContext();
       				c = SettingsBase.courseDict(cycle,eduYear);
    			} else {
        			ec = ((EduCourse)c).editingContext();
    			}
    			Setting setting = SettingsBase.settingForCourse(
    					"presenters.workIntegral", c, ec);
    			if(setting != null) {
    				_integral = setting.textValue();
    				if(_integral != null)
    					return _integral;
    				Integer pKey = setting.numericValue();
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
		return select(Integer.valueOf(-1));
	}
	
	public WOActionResults select(Integer sel) {
		Integer currSel = (Integer)valueForBinding("selection");
		if(hasBinding("selection")) {
			setValueForBinding(sel,"selection");
		} else {
			currSel = (Integer)session().objectForKey("activeCriterion");
			if(sel != null)
				session().setObjectForKey(sel,"activeCriterion");
			else
				session().removeObjectForKey("activeCriterion");
		}
		if((sel==null)?currSel==null:sel.equals(currSel)) {
			Boolean hide = (Boolean)session().objectForKey("hideMarkless");
			if(hide == null) {
				hide = new Boolean(sel != null && sel.intValue() < 0);
			} else {
				hide = new Boolean(!hide.booleanValue());
			}
			session().setObjectForKey(hide,"hideMarkless");
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
