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

import java.lang.reflect.Method;
import java.util.Calendar;
import java.util.Enumeration;

import net.rujel.base.SettingsBase;
import net.rujel.reusables.WOLogLevel;

import com.webobjects.appserver.*;
import com.webobjects.eoaccess.EOUtilities;
import com.webobjects.eocontrol.EOEditingContext;
import com.webobjects.eocontrol.EOEnterpriseObject;
import com.webobjects.eocontrol.EOFetchSpecification;
import com.webobjects.eocontrol.EOKeyValueQualifier;
import com.webobjects.eocontrol.EOQualifier;
import com.webobjects.eocontrol.EOSortOrdering;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSMutableDictionary;
import com.webobjects.foundation.NSTimestamp;

public class EduPeriodSelector extends com.webobjects.appserver.WOComponent {
	
	public EduPeriod perItem;
	public NSTimestamp end;
	protected EduPeriod[] periods;
	public EOEnterpriseObject type;
	
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
   /*
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
	}*/
	
	protected NSArray _types;
	public NSArray types() {
		if(_types != null)
			return types();
		EOQualifier qual = new EOKeyValueQualifier("inYearCount",EOQualifier.QualifierOperatorGreaterThan,0);
		NSArray so = new NSArray(new EOSortOrdering("inYearCount", EOSortOrdering.CompareDescending));
		EOFetchSpecification fs = new EOFetchSpecification("ItogType", qual, so);
		_types = ec().objectsWithFetchSpecification(fs);
		return _types;
	}
	
	protected NSArray<NSMutableDictionary> _list;
    public NSArray list() {
    	if(_list == null) {
    		if(type==null) {
    			if(periods == null)
    				type = fetchPeriods();
    			if(type==null)
    				type = (EOEnterpriseObject)types().lastObject();
    		}
    		generateList();
    	}
    	return _list;
    }
    
    protected NSArray itogsForType(EOEnterpriseObject type) {
		Integer count = (Integer)type.valueForKey("inYearCount");
		Integer eduYear = (Integer)session().valueForKey("eduYear");
		NSArray args = new NSArray(new Object[]{eduYear,type});
		EOQualifier qual=EOQualifier.qualifierWithQualifierFormat(
				"eduYear=%d and itogType=%@", args); 
		NSArray so = new NSArray(new EOSortOrdering ("num",EOSortOrdering.CompareAscending));
		EOFetchSpecification fs = new EOFetchSpecification("ItogContainer",qual,so);
		NSArray itogs=ec().objectsWithFetchSpecification(fs);
		if(itogs.count() < count) {
			try {
				Class cl = Class.forName("ItogType");
				Method gen = cl.getMethod("generateItogsInYear", Integer.class);
				itogs=(NSArray)gen.invoke(type, eduYear);
				ec().saveChanges();
				EduPlan.logger.log(WOLogLevel.INFO, 
						"Autogenerated itogs for type",new Object[]{session(),type});
			} catch (Exception e) {
				EduPlan.logger.log(WOLogLevel.WARNING, 
						"Falied to generate itogs for type",new Object[]{e,session(),type});
			}
		}
		return itogs;
    }
       
	protected void generateList() {
		NSArray itogs = itogsForType(type);
    	if (_list != null && _list.count() == itogs.count()) {
    		for (int i = 0; i < _list.count(); i++) {
				NSMutableDictionary dict = _list.objectAtIndex(i);
				dict.takeValueForKey(itogs.objectAtIndex(i), "relatedItog");
			}
    		return;
    	}
    	NSMutableDictionary[] arr = new NSMutableDictionary[itogs.count()];
    	if(periods != null && itogs.count() == (Integer)periods[1].valueForKeyPath(
    			"relatedItog.itogType.inYearCount")) {
    		for (int i = 0; i < itogs.count(); i++) {
    			arr[i] = new NSMutableDictionary(itogs.objectAtIndex(i), "relatedItog");
    			arr[i].takeValueForKey(periods[i+1].begin(), EduPeriod.BEGIN_KEY);
    		}
    	} else {
    		NSTimestamp begin = null;
    		if(_list != null) {
    			NSMutableDictionary dict = _list.objectAtIndex(0);
    			begin = (NSTimestamp)dict.valueForKey(EduPeriod.BEGIN_KEY);
    		}
    		if(begin == null && periods != null && periods[1] != null)
    			begin =periods[1].begin();
    		if(end == null && periods != null && periods[0] != null)
    			end = periods[0].begin();
    		if(begin == null || end == null) {
    			Integer eduYear = (Integer)session().valueForKey("eduYear");
    			NSTimestamp[] dates = EduPeriod.defaultYearDates(eduYear.intValue());
    			if(begin==null)
    				begin=dates[0];
    			if(end==null)
    				end = dates[1];
    		}
    		Calendar cal = Calendar.getInstance();
    		cal.setTime(end);
    		int days = cal.get(Calendar.DAY_OF_YEAR);
    		cal.setTime(begin);
    		days -= cal.get(Calendar.DAY_OF_YEAR);
    		if(days < 0)
    			days+=cal.getActualMaximum(Calendar.DAY_OF_YEAR);
    		int step = days/itogs.count();
    		arr[0] = new NSMutableDictionary(itogs.objectAtIndex(0), "relatedItog");
    		arr[0].takeValueForKey(begin, EduPeriod.BEGIN_KEY);
    		for (int i = 1; i < arr.length; i++) {
    			arr[i] = new NSMutableDictionary(itogs.objectAtIndex(i), "relatedItog");
    			cal.add(Calendar.DATE, step);
    			arr[i].takeValueForKey(new NSTimestamp(cal.getTimeInMillis()), EduPeriod.BEGIN_KEY);
    		}
    	}
    	_list = new NSArray(arr);
	}
	
	protected EOEnterpriseObject fetchPeriods() {
		EOEnterpriseObject type = null;
    	NSArray found = EOUtilities.objectsMatchingKeyAndValue(ec(), 
    			EduPeriod.ENTITY_NAME, EduPeriod.LIST_NAME_KEY, listName); 
		periods = new EduPeriod[found.count() +1];
		Enumeration enu = found.objectEnumerator();
		while (enu.hasMoreElements()) {
			EduPeriod per = (EduPeriod) enu.nextElement();
			EOEnterpriseObject itog = per.relatedItog();
			if(itog == null) {
				periods[0]=per;
			} else {
				if(type == null)
					type = (EOEnterpriseObject)itog.valueForKey("itogType");
				Integer num = (Integer)itog.valueForKey("num");
				if(num >= periods.length) {
					EduPeriod[] prev = periods;
					periods = new EduPeriod[num+1];
					for (int i = 0; i < prev.length; i++) {
						periods[i]=prev[i];
					}
				}
				periods[num.intValue()]=per;
			}
		}
    	return type;
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
    
    public void setType(EOEnterpriseObject newType) {
    	type = newType;
    	generateList();
    }
    
    public WOActionResults save() {
    	if(_list== null || list().count() ==0)
    		return null;
    	if(periods == null)
    		fetchPeriods();
    	if(periods.length < _list.count()+1) {
    		EduPeriod[] prev = periods;
    		periods = new EduPeriod[_list.count()+1];
    		for (int i = 0; i < prev.length; i++) {
				periods[i]=prev[i];
			}
    	}
    	EOEditingContext ec = ec();
    	
    	for (int i = 0; i < periods.length; i++) {
        	if(periods[i] == null) {
        		periods[i] = (EduPeriod)EOUtilities.createAndInsertInstance(
        				ec, EduPeriod.ENTITY_NAME);
        		periods[i].setListName(listName);
        	}
        	if(i==0) {
            	if(!end.equals(periods[0].begin()))
            		periods[0].setBegin(end);
            	continue;
        	}
        	if(_list.count() < i) {
        		if (periods[i] != null) {
        			ec.deleteObject(periods[i]);
        			periods[i]=null;
        		}
        		continue;
        	}
    		NSMutableDictionary dict = _list.objectAtIndex(i -1);
    		Object val = dict.valueForKey("relatedItog");
    		if(periods[i].relatedItog() != val)
    			periods[i].addObjectToBothSidesOfRelationshipWithKey(
    					(EOEnterpriseObject)val, "relatedItog");
    		val = dict.valueForKey(EduPeriod.BEGIN_KEY);
    		if(!val.equals(periods[i].begin()))
    			periods[i].setBegin((NSTimestamp)val);
		}
    	try {
			ec.saveChanges();
			EduPlan.logger.log(WOLogLevel.COREDATA_EDITING,
					"Saved EduPeriod settings for listName " + listName,session());
		} catch (Exception e) {
			EduPlan.logger.log(WOLogLevel.COREDATA_EDITING,
					"Failed to save EduPeriod settings for listName " + listName,
					new Object[]{session(),e});
			ec.revert();
			periods = null;
		}
    	return null;
    }
    
/*    
    public WOActionResults save() {
    	EOEditingContext ec = ec();
    	EduPeriod per = currPeriod();
    	String act = "edit";
    	if(per == null) {
    		act = "creat";
    		per = (EduPeriod)EOUtilities.createAndInsertInstance(ec, EduPeriod.ENTITY_NAME);
        	if(listName != null) {
        		per.addToList(listName);
         	}
        	per.setEduYear((Integer)session().valueForKey("eduYear"));
    	}
    	if(begin != null && !begin.equals(per.begin()))
    		per.setBegin(begin);
    	if(end != null && !end.equals(per.end()))
    		per.setEnd(end);
    	if(title != null && !title.equals(per.title()))
    		per.setTitle(title);
    	if(fullName != null && !fullName.equals(per.fullName()))
    		per.setFullName(fullName);
    	if(!ec.hasChanges()) {
    		_currPeriod = null;
    		if(hasBinding("currPeriod"))
    			setValueForBinding(_currPeriod, "currPeriod");
			begin = null;
			end = null;
			title = null;
			fullName = null;
    		setValueForBinding(per, "selected");
    		return (WOActionResults)valueForBinding("selectAction");
    	}
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
*/
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
    		_ec = null;
    		_list = null;
    		periods = null;
    		type =  null;
    		end = null;
    		perItem = null;
    		weekDays = 7;
    	}
    	super.appendToResponse(aResponse, aContext);
    }

}