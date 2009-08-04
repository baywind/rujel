// PeriodSelector.java: Class file for WO Component 'PeriodSelector'

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

package net.rujel.eduplan;

import java.util.Enumeration;

import net.rujel.base.SettingsBase;
import net.rujel.reusables.Various;
import net.rujel.reusables.WOLogLevel;

import com.webobjects.appserver.*;
import com.webobjects.eoaccess.EOUtilities;
import com.webobjects.eocontrol.EOEditingContext;
import com.webobjects.eocontrol.EOEnterpriseObject;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSMutableSet;
import com.webobjects.foundation.NSTimestamp;

public class EduPeriodSelector extends com.webobjects.appserver.WOComponent {
	
	public EduPeriod perItem;
	public String title;
	public String fullName;
	public NSTimestamp begin;
	public NSTimestamp end;
	
    public EduPeriodSelector(WOContext context) {
        super(context);
    }
    
    protected EOEditingContext _ec;
    protected EOEditingContext ec() {
    	if(_ec == null) {
    		_ec =  (EOEditingContext)valueForBinding("ec");
    		weekDays = SettingsBase.numericSettingForCourse("weekDays", null, _ec, 7);
    	}
    	return _ec;
    }
    public String listName;
	protected int weekDays;
   
	protected Object _currPeriod;
	public EduPeriod currPeriod() {
		if(_currPeriod == null) {
			_currPeriod = valueForBinding("currPeriod");
			if(_currPeriod == null) {
				_currPeriod = NullValue;
				return null;
			} else {
				title = ((EduPeriod)_currPeriod).title();
				fullName = ((EduPeriod)_currPeriod).fullName();
				begin = ((EduPeriod)_currPeriod).begin();
				end = ((EduPeriod)_currPeriod).end();
			}
		} else if ((_currPeriod instanceof EduPeriod) &&
				((EduPeriod)_currPeriod).editingContext() == null) {
			_currPeriod = NullValue;
		}
		return (_currPeriod == NullValue)?null:(EduPeriod)_currPeriod;
	}
	
	protected NSArray _list;
    public NSArray list() {
    	if(_list != null)
    		return _list;
    	_list = (NSArray)valueForBinding("list");
    	if(_list != null)
    		return _list;
    	if(listName != null) {
    		_list = EduPeriod.periodsInList(listName, ec());
    	} else {
    		_list = EduPeriod.periodsInYear((Integer)session().valueForKey("eduYear"), ec());
    	}
    	if(_list == null)
    		_list = NSArray.EmptyArray;
    	return _list;
    }
    
    public String weeksDays() {
    	int days = 0;
    	if(perItem == null)
    		days = EduPeriod.daysForList(listName, null, list());
    	else
    		days = perItem.daysInPeriod(null, listName);
		StringBuilder result = new StringBuilder(10);
		result.append(days/weekDays).append(' ');
		result.append('(').append(days%weekDays).append(')');
		return result.toString();
	}
    
    public WOActionResults save() {
    	EOEditingContext ec = ec();
    	ec.lock();
    	EduPeriod per = currPeriod();
    	String act = "edit";
    	if(per == null) {
    		act = "creat";
    		per = (EduPeriod)EOUtilities.createAndInsertInstance(ec,
    			EduPeriod.ENTITY_NAME);
        	if(listName != null) {
        		per.addToList(listName);
         	}
    	}
    	per.setEduYear((Integer)session().valueForKey("eduYear"));
    	per.setBegin(begin);
    	per.setEnd(end);
    	per.setTitle(title);
    	per.setFullName(fullName);
    	try {
    		ec.saveChanges();
			EduPlan.logger.log(WOLogLevel.COREDATA_EDITING,"EduPeriod " + act + "ed",
					new Object[] {session(),per});
        	if(listName != null &&
        			EduPeriod.verifyList(EduPeriod.periodsInList(listName, ec)) > 0) {
        		session().takeValueForKey(application().valueForKeyPath(
        			"strings.RujelEduPlan_EduPlan.messages.periodsIntersect"), "message");
    			if(hasBinding("currPeriod")) {
    				_currPeriod = per;
            	} else {
            		setValueForBinding(per, "selected");
            		_currPeriod = null;
    			}
        	} else {
        		setValueForBinding(per, "selected");
        		_currPeriod = null;
        	}
    	} catch (Exception e) {
			session().takeValueForKey(e.getMessage(), "message");
			EduPlan.logger.log(WOLogLevel.FINE,"Error " + act + "ing period",
					new Object[] {session(),per,e});
			if(hasBinding("currPeriod"))
				_currPeriod = per;
			else
				ec.revert();
//			if(currPeriod() != null && currPeriod().editingContext() == null)
//				_currPeriod = NullValue;
		} finally {
			ec.unlock();
		}
		if(hasBinding("currPeriod"))
			setValueForBinding(_currPeriod, "currPeriod");
		_list = null;
		if(currPeriod() == null) {
			begin = null;
			end = null;
			title = null;
			fullName = null;
		}
    	WOActionResults result =  (WOActionResults)valueForBinding("selectAction");
    	return result;
    }
    
	public void delete() {
		EOEditingContext ec = ec();
		ec.lock();
		try {
			if(ec().globalIDForObject(currPeriod()).isTemporary()) {
				ec().revert();
			} else {
				if(listName == null || Various.boolForObject(valueForBinding("global"))) {
					NSArray lists = EOUtilities.objectsMatchingKeyAndValue(ec(),
							"PeriodList", "period", _currPeriod);
					if(lists != null && lists.count() > 0) {
						Enumeration enu = lists.objectEnumerator();
						while (enu.hasMoreElements()) {
							EOEnterpriseObject pl = (EOEnterpriseObject) enu.nextElement();
							ec.deleteObject(pl);
						}
					}
					EduPlan.logger.log(WOLogLevel.COREDATA_EDITING,"Deleting EduPeriod",
							new Object[] {session(),_currPeriod});
					ec().deleteObject(currPeriod());
				} else {
					currPeriod().removeFromList(listName);
					EduPlan.logger.log(WOLogLevel.COREDATA_EDITING,
							"Removing EduPeriod from list: " + listName,
							new Object[] {session(),_currPeriod});
				}
				ec().saveChanges();
			}
			_currPeriod = null;
		} catch (Exception e) {
			EduPlan.logger.log(WOLogLevel.WARNING,"Error deleting EduPeriod",
					new Object[] {session(),_currPeriod,e});
			ec().revert();
		} finally {
			ec().unlock();
		}
		if(hasBinding("currPeriod"))
			setValueForBinding(_currPeriod, "currPeriod");
		_list = null;
		if(currPeriod() == null) {
			begin = null;
			end = null;
			title = null;
			fullName = null;
		}
	}

	public WOActionResults select() {
		if(ec().hasChanges()) {
			try {
				ec().lock();
				ec().revert();
			} catch (Exception e) {
				;
			} finally {
				ec().unlock();
			}
		}
    	if(hasBinding("selected"))
    		setValueForBinding(perItem, "selected");
    	if(hasBinding("currPeriod") && Various.boolForObject(
    			session().valueForKeyPath("readAccess.edit.EduPeriod"))) {
    		setValueForBinding(perItem, "currPeriod");
    		_currPeriod = perItem;
			title = perItem.title();
			fullName = perItem.fullName();
			begin = perItem.begin();
			end = perItem.end();
    	}
    	return (WOActionResults)valueForBinding("selectAction");
    }
    
    public boolean isCurrent() {
    	return (perItem == currPeriod());
    }
    
    public Boolean canCreate() {
    	if(currPeriod() != null && !ec().globalIDForObject(currPeriod()).isTemporary())
    		return Boolean.FALSE;
    	return (Boolean)session().valueForKeyPath("readAccess.create.EduPeriod");
    }
    
    public Boolean canDelete() {
    	if(!hasBinding("currPeriod"))
    		return Boolean.FALSE;
    	if(listName == null)
    		return (Boolean)session().valueForKeyPath("readAccess.delete.EduPeriod");
    	else
    		return (Boolean)session().valueForKeyPath("readAccess.delete.PeriodList");
    }
    
    public String confirmDelete() {
    	StringBuilder buf = new StringBuilder("if(confirm('");
    	if(listName == null || Various.boolForObject(valueForBinding("global"))) {
    		String message = (String)application().valueForKeyPath(
    			"strings.RujelEduPlan_EduPlan.messages.confirmDelete");
			NSArray lists = EOUtilities.objectsMatchingKeyAndValue(ec(),
					"PeriodList", "period", _currPeriod);
			int count = (lists==null)?0:lists.count();
			buf.append(String.format(message, count));
			if(count > 0) {
				NSMutableSet set = new NSMutableSet();
				Enumeration enu = lists.objectEnumerator();
				while (enu.hasMoreElements()) {
					EOEnterpriseObject pl = (EOEnterpriseObject) enu.nextElement();
					String ln = (String)pl.valueForKey("listName");
					if(!set.containsObject(ln)) {
						if(set.count() > 0)
							buf.append(',').append(' ');
						set.addObject(ln);
						buf.append(WOMessage.stringByEscapingHTMLAttributeValue(ln));
					}
				}
			}
    	} else {
       		String message = (String)application().valueForKeyPath(
       			"strings.RujelEduPlan_EduPlan.messages.confirmRemove");
			buf.append(String.format(message, listName));
    	}
    	buf.append("') && tryLoad())window.location = '").append(
    			context().componentActionURL()).append("';");
    	return buf.toString();
    }
    
    public Boolean cantSelect() {
    	if(Various.boolForObject(valueForBinding("cantSelect")))
    		return Boolean.TRUE;
    	if(hasBinding("selected"))
    		return Boolean.FALSE;
    	return (Boolean)session().valueForKeyPath("readAccess._edit.EduPeriod");
    }

    public boolean synchronizesVariablesWithBindings() {
        return false;
	}
	
	/*	public boolean isStateless() {
		return true;
	}

	public void reset() {
		super.reset();
		_ec = null;
		_listName = null;
		_currPeriod = null;
		weekDays = 7;
		title = null;
		fullName = null;
		begin = null;
		end = null;
	}*/
    
    public void appendToResponse(WOResponse aResponse, WOContext aContext) {
    	Object ln = valueForBinding("listName");
    	if((ln==null)?listName!=null:!ln.equals(listName)) {
    		listName = (String)ln;
    		_currPeriod = null;
    		_ec = null;
    		_list = null;
    		begin = null;
    		end = null;
    		fullName = null;
    		perItem = null;
    		title = null;
    		weekDays = 7;
    		title = null;
    	}
    	super.appendToResponse(aResponse, aContext);
    }

}