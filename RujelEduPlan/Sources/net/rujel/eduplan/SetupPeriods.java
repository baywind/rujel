// SetupPeriods.java: Class file for WO Component 'SetupPeriods'

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
import net.rujel.reusables.PlistReader;
import net.rujel.reusables.WOLogLevel;

import com.webobjects.appserver.*;
import com.webobjects.eoaccess.EOUtilities;
import com.webobjects.eocontrol.*;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSDictionary;
import com.webobjects.foundation.NSMutableArray;
import com.webobjects.appserver.WOActionResults;

// Generated by the WOLips Templateengine Plug-in at Jul 14, 2009 6:43:49 PM
public class SetupPeriods extends com.webobjects.appserver.WOComponent {

 	public static final NSArray listSorter = new NSArray(new Object[] {
 		EOSortOrdering.sortOrderingWithKey("period.begin",EOSortOrdering.CompareAscending),
 		EOSortOrdering.sortOrderingWithKey("period.end",EOSortOrdering.CompareDescending)});

 	public EOEditingContext ec;
	public String listName;
	public SettingsBase base;
	public EduPeriod currPeriod;
	public Boolean details;
	public NSArray perList;
	public Object item;
	public int weekDays = 7;
	public NSArray extraLists;
    
	public SetupPeriods(WOContext context) {
        super(context);
        setEc((EOEditingContext)context.page().valueForKey("ec"));
    }
	
	public void setEc(EOEditingContext newEc) {
		ec = newEc;
		ec.lock();
		try {
			base = SettingsBase.baseForKey(EduPeriod.ENTITY_NAME, ec, true);
			setListName(base.textValue());
			if(listName == null) {
				setListName((String)application().valueForKeyPath(
					"strings.RujelEduPlan_EduPlan.SetupPeriods.defaultListName"));
				base.setTextValue(listName);
				ec.saveChanges();
				EduPlan.logger.log(WOLogLevel.SETTINGS_EDITING,
						"Created default EduPeriod ListName setting: " + listName,
						new Object[] {session(), base});
				details = Boolean.FALSE;
			} else {
				if(perList == null || perList.count() == 0) {
					details = Boolean.FALSE;	
				} else {
					EOQualifier qual = new EOKeyValueQualifier("listName",
							EOQualifier.QualifierOperatorNotEqual,listName);
					EOFetchSpecification fs = new EOFetchSpecification("PeriodList",qual,null);
					extraLists = ec.objectsWithFetchSpecification(fs);
					if(extraLists != null && extraLists.count() > 0)
						details = Boolean.TRUE;
					else
						details = Boolean.FALSE;
					qual = new EOKeyValueQualifier(Holiday.LIST_NAME_KEY,
							EOQualifier.QualifierOperatorNotEqual,NullValue);
					fs.setEntityName(Holiday.ENTITY_NAME);
					fs.setQualifier(qual);
					NSArray holidays = ec.objectsWithFetchSpecification(fs);
					if(holidays != null && holidays.count() > 0) {
						if(extraLists == null || extraLists.count() == 0)
							extraLists = holidays;
						else
							extraLists = extraLists.arrayByAddingObjectsFromArray(holidays);
					}
				}
			}
//			weekDays = SettingsBase.numericSettingForCourse("weekDays", null, ec, 7);
		} catch (Exception e) {
			EduPlan.logger.log(WOLogLevel.WARNING,
					"Error creating default EduPeriod ListName setting",
					new Object[] {session(), e});
		} finally {
			ec.unlock();
		}
	}
	
    public boolean synchronizesVariablesWithBindings() {
        return false;
	}

    public void setWeekDays(Integer wd) {
    	if(wd == null || wd.intValue() <= 0)
    		weekDays = 7;
    	else
    		weekDays = wd.intValue();
    }
    
    public void saveWeekDays() {
    	ec.lock();
    	try {
			SettingsBase sb = SettingsBase.baseForKey(EduPeriod.ENTITY_NAME, ec, false);
			if(sb == null) return;
			sb.updateNumValuesForText(listName, new Integer(weekDays));
			ec.saveChanges();
			EduPlan.logger.log(WOLogLevel.SETTINGS_EDITING, "Changed weekDays for listName '"
					+ listName + "' : " + weekDays, session());
		} catch (Exception e) {
			EduPlan.logger.log(WOLogLevel.WARNING,"Error changing weekDays for listName '"
					+ listName + '\'', new Object[] {session(),e});
			session().takeValueForKey(e.getMessage(), "message");
			ec.revert();
			weekDays = 0;
		} finally {
			ec.unlock();
		}
    }
    
    public void showDetails() {
		if(ec.hasChanges()) {
			ec.lock();
			ec.revert();
			ec.unlock();
		}
		details = Boolean.TRUE;
	}
	
	public void setListName(String name) {
		listName = name;
		if(listName != null) {
			perList =  EOUtilities.objectsMatchingKeyAndValue(ec, 
					"PeriodList", "listName", listName);
			if(perList != null && perList.count() > 0) {
				perList = EOSortOrdering.sortedArrayUsingKeyOrderArray(perList, listSorter);
			}
		} else {
			perList = null;
		}
		item = null;
	}

    public String weeksDays() {
    	int days = 0;
    	if(item instanceof EOEnterpriseObject) {
    		EduPeriod per = (EduPeriod)((EOEnterpriseObject)item).valueForKey("period");
    		if(per == null)
    			return "0";
    		days = per.daysInPeriod(null, listName);
    	} else {
    		days = EduPeriod.daysForList(listName,null,(NSArray)perList.valueForKey("period"));
    	}
    	StringBuilder result = new StringBuilder(10);
    	if(weekDays > 0) {
    		result.append(days/weekDays).append(' ');
    		result.append('(').append(days%weekDays).append(')');
    	} else {
    		result.append('(').append(days).append(')');
    	}
    	return result.toString();
    }
    
	public WOActionResults addPeriodToList() {
		WOComponent selector = pageWithName("SelectorPopup");
		selector.takeValueForKey(context().page(), "returnPage");
		selector.takeValueForKey("addPeriod", "resultPath");
		selector.takeValueForKey(this, "resultGetter");
		NSDictionary dict = (NSDictionary)application().valueForKeyPath(
				"strings.RujelEduPlan_EduPlan.SetupPeriods.choosePeriod");
		dict = PlistReader.cloneDictionary(dict, true);
		dict.takeValueForKeyPath(ec, "presenterBindings.ec");
		selector.takeValueForKey(dict, "dict");
		return selector;
	}

	public void setAddPeriod(EduPeriod per) {
		ec.lock();
		if(per == null || perList == null)
			perList =  EOUtilities.objectsMatchingKeyAndValue(ec, 
					"PeriodList", "listName", listName);
		if(per != null && perList != null && perList.count() > 0) {
			Enumeration enu = perList.objectEnumerator();
			NSMutableArray testList = new NSMutableArray(per);
			while (enu.hasMoreElements()) {
				EOEnterpriseObject pl = (EOEnterpriseObject) enu.nextElement();
				EduPeriod lp = (EduPeriod)pl.valueForKey("period");
				if(lp == per) {
					per = null;
					break;
				}
				testList.addObject(lp);
			}
			if(per != null && EduPeriod.verifyList(testList) > 0) {
        		session().takeValueForKey(application().valueForKeyPath(
    				"strings.RujelEduPlan_EduPlan.messages.periodsIntersect"), "message");
        		per = null;
			}
		}
		if(per != null) {
			EOEnterpriseObject pl = per.addToList(listName);
			try {
				ec.saveChanges();
				EduPlan.logger.log(WOLogLevel.COREDATA_EDITING,"Added period to list "
						+ listName,  new Object[] {session(),per});
				if(perList == null) {
					perList = new NSMutableArray(pl);
				} else {
					NSMutableArray resultList = null;
					if(perList instanceof NSMutableArray)
						resultList = (NSMutableArray)perList;
					else 
						resultList = perList.mutableClone();
					resultList.addObject(pl);
					EOSortOrdering.sortArrayUsingKeyOrderArray(resultList, listSorter);
					perList = resultList;
				}
			} catch (Exception e) {
				EduPlan.logger.log(WOLogLevel.INFO,"Error adding period to list "
						+ listName,  new Object[] {session(),per,e});
				ec.revert();
				session().takeValueForKey(e.getMessage(), "message");
			}
		} else {
			if(perList instanceof NSMutableArray)
				EOSortOrdering.sortArrayUsingKeyOrderArray((NSMutableArray)perList,listSorter);
			else 
				perList = EOSortOrdering.sortedArrayUsingKeyOrderArray(perList, listSorter);
		}
		ec.unlock();
	}
	
	public WOActionResults editPeriod() {
		WOComponent selector = pageWithName("SelectorPopup");
		selector.takeValueForKey(context().page(), "returnPage");
		selector.takeValueForKey("addPeriod", "resultPath");
		selector.takeValueForKey(this, "resultGetter");
		NSDictionary dict = (NSDictionary)application().valueForKeyPath(
				"strings.RujelEduPlan_EduPlan.SetupPeriods.choosePeriod");
		dict = PlistReader.cloneDictionary(dict, true);
		dict.takeValueForKeyPath(ec, "presenterBindings.ec");
		dict.takeValueForKeyPath(".", "presenterBindings.currPeriod");
		dict.takeValueForKeyPath(null, "presenterBindings.selected");
		dict.takeValueForKeyPath(listName, "presenterBindings.listName");
		dict.takeValueForKeyPath(Boolean.TRUE, "presenterBindings.cantSelect");
		dict.takeValueForKeyPath(Boolean.TRUE, "presenterBindings.global");
		selector.takeValueForKey(dict, "dict");
		selector.takeValueForKey(((EOEnterpriseObject)item).valueForKey("period"), "value");
		return selector;
	}

	public void deletePeriod() {
		if(!(item instanceof EOEnterpriseObject))
				return;
		ec.lock();
		EduPeriod per = (EduPeriod)((EOEnterpriseObject)item).valueForKey("period");
		try {
			EduPlan.logger.log(WOLogLevel.COREDATA_EDITING,"Removing period from list "
					+ listName, new Object[] {session(),per});
			ec.deleteObject((EOEnterpriseObject)item);
			ec.saveChanges();
			if(!(perList instanceof NSMutableArray))
				perList = perList.mutableClone();
			((NSMutableArray)perList).removeObject(item);
		} catch (Exception e) {
			EduPlan.logger.log(WOLogLevel.INFO,"Error removing period from list "
					+ listName,  
					new Object[] {session(),per,e});
			ec.revert();
			session().takeValueForKey(e.getMessage(), "message");
		} finally {
			ec.unlock();
		}
	}

	public void reset() {
		super.reset();
		currPeriod = null;
		perList = null;
		extraLists = null;
		item = null;
		details = null;
		listName = null;
		weekDays = 7;
	}
}
