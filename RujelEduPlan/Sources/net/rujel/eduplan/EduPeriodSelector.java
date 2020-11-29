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

import java.util.Calendar;
import java.util.Enumeration;

import net.rujel.base.SettingsBase;
import net.rujel.interfaces.EOPeriod;
import net.rujel.reusables.Various;
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
	
//	public EduPeriod perItem;
	public Object item;
	protected NSTimestamp end;
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
	public String totalDays;
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
	
	public NSTimestamp lastDay() {
		return end.timestampByAddingGregorianUnits(0, 0, -1, 0, 0, 0);
	}
	public void setLastDay(NSTimestamp lastDay) {
		end = lastDay.timestampByAddingGregorianUnits(0, 0, +1, 0, 0, 0);
	}
	
	public NSArray types;
	public NSArray fetchTypes() {
		EOQualifier qual = new EOKeyValueQualifier("inYearCount",EOQualifier.QualifierOperatorGreaterThan,0);
		NSArray so = new NSArray(new EOSortOrdering("inYearCount", EOSortOrdering.CompareDescending));
		EOFetchSpecification fs = new EOFetchSpecification("ItogType", qual, so);
		types = ec().objectsWithFetchSpecification(fs);
		return types;
	}
	
	public String noType() {
		if(type == null)
			return (String)session().valueForKeyPath(
					"strings.RujelEduPlan_EduPlan.SetupPeriods.chooseType");
		else
			return null;
	}
	
	protected NSArray<NSMutableDictionary> _list;
    public NSArray list() {
    	if(_list == null && type != null) {
    		generateList();
    		calculateDays();
    	}
    	return _list;
    }
           
	public void generateList() {
		Integer eduYear = (Integer)session().valueForKey("eduYear");
		NSArray itogs = InitialDataGenerator.itogsForType(type,eduYear,true);
		int cnt = itogs.count();
    	if (_list != null && _list.count() == cnt) { // use same dates
    		for (int i = 0; i < cnt; i++) {
				NSMutableDictionary dict = _list.objectAtIndex(i);
				dict.takeValueForKey(itogs.objectAtIndex(i), "relatedItog");
			}
    		calculateDays();
    		return;
    	}
    	NSMutableDictionary[] arr = new NSMutableDictionary[cnt];
		for (int i = 0; i < cnt; i++) {
			arr[i] = new NSMutableDictionary(itogs.objectAtIndex(i), "relatedItog");
		}
    	if(periods != null && periods[1] != null && cnt == (Integer)periods[1].valueForKeyPath(
    			"relatedItog.itogType.inYearCount")) { // take dates from periods
    		for (int i = 0; i < cnt; i++) {
    			arr[i].takeValueForKey(periods[i+1].begin(), EduPeriod.BEGIN_KEY);
    		}
			_list = new NSArray(arr);
			calculateDays();
			return;
    	}// take dates from periods
    	
    	NSArray found = EOUtilities.objectsMatchingKeyAndValue(ec(),
    			EduPeriod.ENTITY_NAME, "relatedItog", itogs.objectAtIndex(0));
    	if(found.count() > 0) { // copy existing periods from other listName
    		EduPeriod per = (EduPeriod)found.objectAtIndex(0);
    		found = EOUtilities.objectsMatchingKeyAndValue(ec(),
    				EduPeriod.ENTITY_NAME, EduPeriod.LIST_NAME_KEY, per.listName());
    		Enumeration enu = found.objectEnumerator();
    		while (enu.hasMoreElements()) {
    			per = (EduPeriod) enu.nextElement();
    			Integer num=(Integer)per.valueForKeyPath("relatedItog.num");
    			if(num == null) {
    				end = per.begin();
    			} else {
    				arr[num-1].takeValueForKey(per.begin(), EduPeriod.BEGIN_KEY);
    			}
    		} //found.objectEnumerator();
    		_list = new NSArray(arr);
    		calculateDays();
    		return;
    	} // copy existing periods from other listName

    	// generate dates based on default settings
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
    		NSTimestamp[] dates = EduPeriod.defaultYearDates(eduYear.intValue());
    		if(begin==null)
    			begin=dates[0];
    		if(end==null)
    			setLastDay(dates[1]);
    	}
    	Calendar cal = Calendar.getInstance();
    	cal.setTime(end);
    	int days = cal.get(Calendar.DAY_OF_YEAR);
    	cal.setTime(begin);
    	days -= cal.get(Calendar.DAY_OF_YEAR);
    	if(days < 0)
    		days+=cal.getActualMaximum(Calendar.DAY_OF_YEAR);
    	int step = days/cnt;
    	arr[0] = new NSMutableDictionary(itogs.objectAtIndex(0), "relatedItog");
    	arr[0].takeValueForKey(begin, EduPeriod.BEGIN_KEY);
    	for (int i = 1; i < arr.length; i++) {
    		cal.add(Calendar.DATE, step);
    		arr[i].takeValueForKey(new NSTimestamp(cal.getTimeInMillis()), EduPeriod.BEGIN_KEY);
    	}
    	_list = new NSArray(arr);
    	calculateDays();
	}
    
	public void calculateDays() {
		NSArray holidays = Holiday.holidaysForList(listName, ec());
		NSTimestamp fin = end;
		for (int i = _list.count()-1; i >=0; i--) {
			NSMutableDictionary dict = (NSMutableDictionary)_list.objectAtIndex(i);
			NSTimestamp begin = (NSTimestamp)dict.valueForKey(EduPeriod.BEGIN_KEY);
			int days = EOPeriod.Utility.countDays(begin, fin) -1;
			days -= Holiday.freeDaysInDates(begin, fin, holidays);
			StringBuilder buf = new StringBuilder(10);
			buf.append(days/weekDays).append(' ');
			buf.append('(').append(days%weekDays).append(')');
			dict.takeValueForKey(buf.toString(), "weekDays");
			fin = begin;
		}
		int days = EOPeriod.Utility.countDays(fin, end) -1;
		days -= Holiday.freeDaysInDates(fin,end, holidays);
		StringBuilder buf = new StringBuilder(10);
		buf.append(days/weekDays).append(' ');
		buf.append('(').append(days%weekDays).append(')');
		totalDays=buf.toString();
	}
    
    public WOActionResults save() {
    	if(_list== null || _list.count() ==0)
    		return null;
    	calculateDays();
    	if(periods == null) {
    		periods = new EduPeriod[_list.count()+1];
    	} else if(periods.length < _list.count()+1) {
    		EduPeriod[] prev = periods;
    		periods = new EduPeriod[_list.count()+1];
    		for (int i = 0; i < prev.length; i++) {
				periods[i]=prev[i];
			}
    	}
    	EOEditingContext ec = ec();
    	
    	for (int i = _list.count()+1; i < periods.length; i++) {
    		if (periods[i] != null) {
    			ec.deleteObject(periods[i]);
    			periods[i]=null;
    		}
		} // delete extra periods

    	if(periods[0]==null) {
    		periods[0] = (EduPeriod)EOUtilities.createAndInsertInstance(
    				ec, EduPeriod.ENTITY_NAME);
    		periods[0].setListName(listName);
    	}
		periods[0].setBegin(end);
    	EduPeriod next = periods[0];
    	next._next=next;
    	next._perlist=periods;
    	
    	for (int i = _list.count(); i > 0; i--) { // create needed periods, save dates
        	if(periods[i] == null) {
        		periods[i] = (EduPeriod)EOUtilities.createAndInsertInstance(
        				ec, EduPeriod.ENTITY_NAME);
        		periods[i].setListName(listName);
        	}
    		NSMutableDictionary dict = _list.objectAtIndex(i -1);
    		Object val = dict.valueForKey("relatedItog");
    		if(periods[i].relatedItog() != val)
    			periods[i].addObjectToBothSidesOfRelationshipWithKey(
    					(EOEnterpriseObject)val, "relatedItog");
    		val = dict.valueForKey(EduPeriod.BEGIN_KEY);
    		if(!val.equals(periods[i].begin()))
    			periods[i].setBegin((NSTimestamp)val);
    		periods[i]._next=next;
    		periods[i]._perlist=periods;
    		next = periods[i];
		} // create needed periods, save dates
    	try {
			ec.saveChanges();
			EduPlan.logger.log(WOLogLevel.COREDATA_EDITING,
					"Saved EduPeriod settings for listName " + listName,session());
		} catch (Exception e) {
			EduPlan.logger.log(WOLogLevel.WARNING,
					"Failed to save EduPeriod settings for listName " + listName,
					new Object[]{session(),e});
			ec.revert();
    		periods=EduPeriod.fetchPeriods(ec(),listName);
		}
    	return null;
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
    
    protected EOEnterpriseObject typeForListName(String listName) {
    	if(listName == null)
    		return null;
		Integer eduYear = (Integer)session().valueForKey("eduYear");
		NSArray ltypes = EOUtilities.objectsMatchingKeyAndValue(ec(), "ItogTypeList", "listName", listName);
		if (ltypes == null || ltypes.count() == 0)
			return null;
		EOEnterpriseObject best = null;
		int cnt = 0;
		Enumeration enu = ltypes.objectEnumerator();
		while (enu.hasMoreElements()) {
			EOEnterpriseObject tl = (EOEnterpriseObject) enu.nextElement();
			Integer val = (Integer)tl.valueForKey("eduYear");
			if(val != null && val.intValue() > 0 && !val.equals(eduYear))
				continue;
			EOEnterpriseObject t = (EOEnterpriseObject)tl.valueForKey("itogType");
			val = (Integer)t.valueForKey("inYearCount");
			if(val.intValue() > cnt) {
				cnt = val.intValue();
				best = t;
			}
		}
		return best;
    }
    
    public void appendToResponse(WOResponse aResponse, WOContext aContext) {
    	Object ln = valueForBinding("listName");
    	if((ln==null)?listName!=null:!ln.equals(listName) ||
    			Various.boolForObject(valueForBinding("shouldReset"))) {
    		listName = (String)ln;
    		_ec = null;
    		_list = null;
    		weekDays = 7;
    		fetchTypes();
    		periods=EduPeriod.fetchPeriods(ec(),listName);
    		if(periods == null) {
    			type = typeForListName(listName);
    			if (type == null) {
    				String itogList = SettingsBase.stringSettingForCourse("ItogMark", null, ec());
    				type = typeForListName(itogList);
    			}
    			if (type == null)
    				type = (EOEnterpriseObject)types.objectAtIndex(0);
    		} else {
    		if(periods.length > 1 && periods[1] != null)
    			type=(EOEnterpriseObject)periods[1].valueForKeyPath("relatedItog.itogType");
    		if(periods[0] != null)
    			end=periods[0].begin();
    		}
    		if(type != null)
    			generateList();
//    		perItem = null;
    	}
    	super.appendToResponse(aResponse, aContext);
    }

    public boolean hasChanges() {
    	if(type == null || _list == null)
    		return false;
    	if(periods==null)
    		return true;
    	int cnt = _list.count();
    	if(cnt >= periods.length)
    		return true;
    	if (periods[0]==null)
    			return true;
    	for (int i = 0; i < cnt; i++) {
			NSMutableDictionary dict = _list.objectAtIndex(i);
			EduPeriod per = periods[i+1];
			if (per == null || 
					per.relatedItog() != dict.valueForKey("relatedItog"))
				return true;
			if (0 != EOPeriod.Utility.compareDates(per.begin(), 
					(NSTimestamp)dict.valueForKey(EduPeriod.BEGIN_KEY)))
				return true;
		}
		return (0 != EOPeriod.Utility.compareDates(periods[0].begin(),end));
    }
   
}