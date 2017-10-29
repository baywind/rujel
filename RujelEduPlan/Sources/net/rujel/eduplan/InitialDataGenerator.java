// InitialDataGenerator.java

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

import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.TimeZone;

import net.rujel.base.SettingsBase;
import net.rujel.interfaces.EOPeriod;
import net.rujel.interfaces.EduCourse;
import net.rujel.reusables.DataBaseUtility;
import net.rujel.reusables.SettingsReader;
import net.rujel.reusables.WOLogLevel;

import com.webobjects.appserver.WOApplication;
import com.webobjects.appserver.WOContext;
import com.webobjects.eoaccess.EOUtilities;
import com.webobjects.eocontrol.EOEditingContext;
import com.webobjects.eocontrol.EOEnterpriseObject;
import com.webobjects.eocontrol.EOFetchSpecification;
import com.webobjects.eocontrol.EOKeyGlobalID;
import com.webobjects.eocontrol.EOKeyValueQualifier;
import com.webobjects.eocontrol.EOObjectStoreCoordinator;
import com.webobjects.eocontrol.EOQualifier;
import com.webobjects.eocontrol.EOSortOrdering;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSDictionary;
import com.webobjects.foundation.NSForwardException;
import com.webobjects.foundation.NSMutableArray;
import com.webobjects.foundation.NSMutableDictionary;
import com.webobjects.foundation.NSTimestamp;

public class InitialDataGenerator {

	public static Object initialData(WOContext ctx) {
		EOEditingContext prevEc = (EOEditingContext)ctx.userInfoForKey("prevEc");
		EOEditingContext ec = (EOEditingContext)ctx.userInfoForKey("ec");
		if(prevEc == null) { // load predefined data
			try {
				EOObjectStoreCoordinator os = EOObjectStoreCoordinator.defaultCoordinator();
				InputStream data = WOApplication.application().resourceManager().
				inputStreamForResourceNamed("dataEduPlanModel.sql", "RujelEduPlan", null);
				DataBaseUtility.executeScript(os, "EduPlanModel", data);
				/*
				int num = SettingsReader.intForKeyPath("schoolNumber", 0);
				if(num != 0) {
					StringBuilder sql = new StringBuilder("update PL_CYCLE set SCHOOL_NUM = ");
					sql.append(num).append(';');
					EOUtilities.rawRowsForSQL(ec, "EduPlanModel", sql.toString(), null);
				}
				*/
				data = WOApplication.application().resourceManager().
				inputStreamForResourceNamed("dataEduPlanYearly.sql", "RujelEduPlan", null);
				DataBaseUtility.executeScript(os, "EduPlanYearly", data);
				EduPlan.logger.log(WOLogLevel.INFO, "Loaded inital EduPlan");
				Integer eduYear = (Integer)ctx.userInfoForKey("eduYear");
				NSArray holidays = EOUtilities.objectsForEntityNamed(ec, Holiday.ENTITY_NAME);
				if(holidays == null || holidays.count() == 0) {
					generateHolidays(ec, eduYear);
				}
				ec.saveChanges();
				EduPlan.logger.log(WOLogLevel.INFO, "Loaded inital Holidays for EduPlan models");
			} catch (Exception e) {
				ec.revert();
				EduPlan.logger.log(WOLogLevel.WARNING,
						"Failed to load inital data for EduPlan models",e);
			}
		} else { // copy from previous year
			try {
				if(copyPlanHours(prevEc, ec)) {
					ec.saveChanges();
					EduPlan.logger.log(WOLogLevel.INFO, 
							"Copied PlanHours data from previous year");
				}
			} catch (Exception e) {
				ec.revert();
				EduPlan.logger.log(WOLogLevel.WARNING, 
						"Failed to copy PlanHours data from previous year",e);
			}
			Integer eduYear = (Integer)ctx.userInfoForKey("eduYear");
			try {
				if(copyPeriods(ec, eduYear, prevEc)) {
					ec.saveChanges();
					EduPlan.logger.log(WOLogLevel.INFO,"Copied EduPeriods from previous year");
				}
			} catch (Exception e) {
				ec.revert();
				EduPlan.logger.log(WOLogLevel.WARNING, 
						"Failed to copy EduPeriods from previous year",e);
			}
			try {
				if(copyHolidays(ec, prevEc) || generateHolidays(ec, eduYear)) {
					ec.saveChanges();
					EduPlan.logger.log(WOLogLevel.INFO, "Copied Holidays from previous year");
				}
			} catch (Exception e) {
				ec.revert();
				EduPlan.logger.log(WOLogLevel.WARNING, 
						"Failed to copy Holidays from previous year",e);
			}
		}
		return null;
	}

	protected static boolean copyHolidays(EOEditingContext ec, EOEditingContext prevEc) {
		NSArray rows = EOUtilities.objectsForEntityNamed(prevEc, Holiday.ENTITY_NAME);
		if(rows == null || rows.count() == 0)
			return false;
		Enumeration enu = rows.objectEnumerator();
		while (enu.hasMoreElements()) {
			Holiday o = (Holiday) enu.nextElement();
			Holiday newO = (Holiday)EOUtilities.createAndInsertInstance
				(ec, Holiday.ENTITY_NAME);
			newO.setName(o.name());
			newO.setListName(o.listName());
			newO.setBegin(
					o.begin().timestampByAddingGregorianUnits(1, 0, 0, 0, 0, 0));
			newO.setEnd(
					o.end().timestampByAddingGregorianUnits(1, 0, 0, 0, 0, 0));
		}
		return true;
	}

	protected static boolean copyPeriods(EOEditingContext ec, Integer eduYear, EOEditingContext prevEc) {
		NSArray nContainers = EOUtilities.objectsMatchingKeyAndValue(ec, "ItogContainer", "eduYear", eduYear);

		SettingsBase setting = SettingsBase.baseForKey(EduPeriod.ENTITY_NAME, prevEc, false);
		if(setting == null || setting.textValue()==null)
			return false;
		NSArray lists=setting.availableValues(eduYear, SettingsBase.TEXT_VALUE_KEY);
		if(lists.count() ==0)
			return false;
		Enumeration enu = lists.objectEnumerator();
		Method gen = null;

		while (enu.hasMoreElements()) {
			String listName = (String) enu.nextElement();
			NSArray periods = EOUtilities.objectsMatchingKeyAndValue(prevEc, 
					EduPeriod.ENTITY_NAME, EduPeriod.LIST_NAME_KEY, listName);
			if(periods == null || periods.count() == 0)
				continue;
			Enumeration penu = periods.objectEnumerator();
			NSArray forType = null;
			while (penu.hasMoreElements()) {
				EduPeriod per = (EduPeriod) penu.nextElement();
				EOEnterpriseObject type = (EOEnterpriseObject)per.valueForKeyPath("relatedItog.itogType");
				if(type == null) {
					EduPeriod nPer = (EduPeriod)EOUtilities.createAndInsertInstance(prevEc, EduPeriod.ENTITY_NAME);
					nPer.setListName(listName);
					nPer.setBegin(per.begin().timestampByAddingGregorianUnits(1, 0, 0, 0, 0, 0));
					continue;
				}
				type = EOUtilities.localInstanceOfObject(ec, type);
				if(forType == null || ((EOEnterpriseObject)forType.objectAtIndex(0))
						.valueForKey("itogType") != type) {
					EOQualifier qual = new EOKeyValueQualifier("itogType", 
							EOQualifier.QualifierOperatorEqual, type);
					forType = EOQualifier.filteredArrayWithQualifier(nContainers, qual);
				} //if(forType == null
				if(forType.count() == 0) {
					if(gen == null) {
						try {
							Class cl = Class.forName("ItogType");
							gen = cl.getMethod("generateItogsInYear", Integer.class);
						} catch (Exception e) {
							throw new NSForwardException(e, 
									"Failed to get itogs generation method");
						}
					}
					try {
						forType=(NSArray)gen.invoke(type, eduYear);
					} catch (Exception e) {
						throw new NSForwardException(e, 
								"Failed to generate itogs for next year");
					}
				} //(forType.count() == 0)
				EOQualifier qual = new EOKeyValueQualifier("num", EOQualifier.QualifierOperatorEqual, 
						per.valueForKey("num"));
				NSArray found = EOQualifier.filteredArrayWithQualifier(forType, qual);
				if(found.count() == 0)
					continue;
				EduPeriod nPer = (EduPeriod)EOUtilities.createAndInsertInstance(prevEc, EduPeriod.ENTITY_NAME);
				nPer.setListName(listName);
				nPer.addObjectToBothSidesOfRelationshipWithKey(
						(EOEnterpriseObject)found.objectAtIndex(0), "relatedItog");
				nPer.setBegin(per.begin().timestampByAddingGregorianUnits(1, 0, 0, 0, 0, 0));
			} //periods.objectEnumerator();
//			ec.saveChanges();
		} //lists.objectEnumerator();
		return true;
	}
		/*
		NSArray rows = EOUtilities.objectsForEntityNamed(prevEc, "PeriodList");
		if(rows == null || rows.count() == 0)
			return false;
		Enumeration enu = rows.objectEnumerator();
		NSMutableDictionary pers = new NSMutableDictionary();
		while (enu.hasMoreElements()) {
			EOEnterpriseObject o = (EOEnterpriseObject) enu.nextElement();
			EOEnterpriseObject newO = EOUtilities.createAndInsertInstance
				(ec, o.entityName());
			newO.takeValueForKey(o.valueForKey("listName"), "listName");
			EduPeriod per = (EduPeriod)o.valueForKey("period");
			EduPeriod newPer = (EduPeriod)pers.objectForKey(per);
			if(newPer == null) {
				newPer = (EduPeriod)EOUtilities.createAndInsertInstance(
						ec, EduPeriod.ENTITY_NAME);
				newPer.setTitle(per.title());
				newPer.setFullName(per.fullName());
				newPer.setEduYear(eduYear);
				newPer.setBegin(
						per.begin().timestampByAddingGregorianUnits(1, 0, 0, 0, 0, 0));
				newPer.setEnd(
						per.end().timestampByAddingGregorianUnits(1, 0, 0, 0, 0, 0));
				pers.setObjectForKey(newPer, per);
			}
			newO.addObjectToBothSidesOfRelationshipWithKey(newPer, "period");
		}
		return true;
	}*/

	protected static boolean copyPlanHours(EOEditingContext prevEc,
			EOEditingContext ec) {
		NSArray rows = EOUtilities.objectsForEntityNamed(prevEc, PlanHours.ENTITY_NAME);
		if(rows == null || rows.count() == 0)
			return false;
		Enumeration enu = rows.objectEnumerator();
		while (enu.hasMoreElements()) {
			PlanHours ph = (PlanHours) enu.nextElement();
			PlanHours newPh = (PlanHours)EOUtilities.createAndInsertInstance
			(ec, "PlanHours");
			newPh.takeValueForKey(ph.totalHours(), "totalHours");
			newPh.takeValueForKey(ph.weeklyHours(), "weeklyHours");
			EOEnterpriseObject rel = EOUtilities.localInstanceOfObject(ec, ph.planCycle());
			newPh.addObjectToBothSidesOfRelationshipWithKey(rel, "planCycle");
			rel = EOUtilities.localInstanceOfObject(ec, ph.section());
			newPh.setSection(rel);
			newPh.setEduSubject(ph.eduSubject());
			newPh.setGrade(ph.grade());
		}
		return true;
	}

	protected static boolean generateHolidays(EOEditingContext ec, Integer eduYear) {
		NSArray presets = (NSArray)WOApplication.application().valueForKeyPath(
		"strings.RujelEduPlan_EduPlan.defaultHolidays");
		if(presets == null || presets.count() == 0)
			return false;
		Enumeration enu = presets.objectEnumerator();
		int newYearMonth = SettingsReader.intForKeyPath("edu.newYearMonth",Calendar.JULY);
		TimeZone tz = TimeZone.getDefault();
		while (enu.hasMoreElements()) {
			NSDictionary dict = (NSDictionary) enu.nextElement();
			Holiday holiday = (Holiday)EOUtilities.createAndInsertInstance(
					ec, Holiday.ENTITY_NAME);
			holiday.setName((String)dict.valueForKey("title"));
			int year = eduYear;
			Number month = (Number)dict.valueForKey("startMonth");
			Number day = (Number)dict.valueForKey("startDay");
			if(month.intValue() < newYearMonth)
				year++;
			NSTimestamp date = new NSTimestamp(year, month.intValue(), day.intValue(),
					0, 0, 0, tz);
			holiday.setBegin(date);
			year = eduYear;
			month = (Number)dict.valueForKey("endMonth");
			day = (Number)dict.valueForKey("endDay");
			if(month.intValue() < newYearMonth)
				year++;
			date = new NSTimestamp(year, month.intValue(), day.intValue(),0, 0, 0, tz);
			holiday.setEnd(date);
		}
		return true;
	}

    public static NSArray itogsForType(EOEnterpriseObject type,Integer eduYear,boolean generate) {
		Integer count = (Integer)type.valueForKey("inYearCount");
		NSArray args = new NSArray(new Object[]{eduYear,type});
		EOQualifier qual=EOQualifier.qualifierWithQualifierFormat(
				"eduYear=%d and itogType=%@", args); 
		NSArray so = new NSArray(new EOSortOrdering ("num",EOSortOrdering.CompareAscending));
		EOFetchSpecification fs = new EOFetchSpecification("ItogContainer",qual,so);
		EOEditingContext ec = type.editingContext();
		NSArray itogs=ec.objectsWithFetchSpecification(fs);
		if(itogs.count() < count) {
			try {
				Class cl = Class.forName("ItogType");
				Method gen = cl.getMethod("generateItogsInYear", Integer.class);
				itogs=(NSArray)gen.invoke(type, eduYear);
				ec.saveChanges();
				EduPlan.logger.log(WOLogLevel.INFO, "Autogenerated itogs for type",type);
			} catch (Exception e) {
				EduPlan.logger.log(WOLogLevel.WARNING, 
						"Falied to generate itogs for type",new Object[]{e,type});
			}
		}
		return itogs;
    }
	
    public static void updatePeriodStructure(EOEditingContext ec) {
/*    	NSArray perlist=EOUtilities.objectsForEntityNamed(ec, EduPeriod.ENTITY_NAME);
    	if(perlist.count() > 0)
    		return; */
    	EOKeyValueQualifier qual = new EOKeyValueQualifier("newPeriod", 
    			EOQualifier.QualifierOperatorEqual, null);
    	NSArray perlist = new NSArray(new EOSortOrdering("listName",EOSortOrdering.CompareAscending));
    	EOFetchSpecification fs = new EOFetchSpecification("PeriodList",qual,perlist);
    	fs.setPrefetchingRelationshipKeyPaths(new NSArray("oldPeriod"));
    	perlist = ec.objectsWithFetchSpecification(fs);
    	if(perlist == null || perlist.count() ==0)
    		return;
    	String listName = (String)((EOEnterpriseObject)perlist.objectAtIndex(0)).valueForKey("listName");
    	Enumeration enu = perlist.objectEnumerator();
    	perlist = EOUtilities.objectsForEntityNamed(ec, "PlanDetail");
    	NSMutableDictionary details = null;
    	if(perlist != null && perlist.count() > 0) { // prepare PlanDetail agregate
    		details = new NSMutableDictionary();
			SettingsBase sb = SettingsBase.baseForKey(EduPeriod.ENTITY_NAME, ec, false);
    		Enumeration denu = perlist.objectEnumerator();
    		while (denu.hasMoreElements()) {
				EOEnterpriseObject det = (EOEnterpriseObject) denu.nextElement();
				EduCourse course = (EduCourse)det.valueForKey("course");
				String ln = (sb==null)?listName:
					sb.forCourse(course).textValue();
				NSMutableDictionary forLN = (NSMutableDictionary)details.objectForKey(ln);
				if(forLN == null) {
					forLN = new NSMutableDictionary();
					details.setObjectForKey(forLN, ln);
				}
				EOEnterpriseObject per = (EOEnterpriseObject)det.valueForKey("eduPeriod");
				if(ln.equals(per.valueForKey(EduPeriod.LIST_NAME_KEY)))
					continue;
				EOKeyGlobalID gid = (EOKeyGlobalID)ec.globalIDForObject(per);
				Object id = gid.keyValues()[0];
				NSMutableArray toUpdate = (NSMutableArray)forLN.objectForKey(id);
				if(toUpdate == null) {
					toUpdate = new NSMutableArray(det);
					forLN.setObjectForKey(toUpdate, id);
				} else {
					toUpdate.addObject(det);
				}
			} // details.objectEnumerator();
    	} // prepare PlanDetail agregate
    	
    	NSMutableArray list = new NSMutableArray();
    	while (enu.hasMoreElements()) {
			EOEnterpriseObject pl = (EOEnterpriseObject) enu.nextElement();
			if(!pl.valueForKey("listName").equals(listName)) {
				generatePeriodList(list, listName, ec,details);
				listName = (String)pl.valueForKey("listName");
				list.removeAllObjects();
			}
			list.addObject(pl);
		}
    	generatePeriodList(list, listName, ec,details);
    }
    
    private static void generatePeriodList(NSMutableArray list,String listName,EOEditingContext ec,
    		NSMutableDictionary details) {
    	int count = list.count();
    	if(listName == null || count == 0)
    		return;
    	NSArray types = EOUtilities.objectsMatchingKeyAndValue(ec,
    			"ItogType", "inYearCount", Integer.valueOf(count));
    	if(types == null) {
			EduPlan.logger.log(WOLogLevel.WARNING,"Failed to get ItogType with count="+
					count + " for listName " + listName);
			return;
    	}
    	NSArray sorter = new NSArray(new EOSortOrdering("oldPeriod",EOSortOrdering.CompareAscending));
    	EOSortOrdering.sortArrayUsingKeyOrderArray(list, sorter);
    	Integer eduYear = (Integer)((EOEnterpriseObject)list.objectAtIndex(0)).valueForKeyPath("oldPeriod.eduYear");
    	EOEnterpriseObject type = (EOEnterpriseObject)types.objectAtIndex(0);
    	NSArray itogs = itogsForType(type, eduYear, types.count() == 1);
    	if(itogs.count() != count) {
    		for (int i = 1; i < types.count(); i++) {
    			EOEnterpriseObject t = (EOEnterpriseObject)types.objectAtIndex(i); 
    			itogs = itogsForType(t, eduYear, false);
    			if(itogs.count() == count){
    				type = t;
    				break;
    			}
    		}
    		if(itogs.count() != count)
    			itogs = itogsForType(type, eduYear, true);
    	}
    	if(details != null)
    		details = (NSMutableDictionary)details.valueForKey(listName);
    	EOEnterpriseObject oldPer=null;
    	EduPeriod[] perlist = new EduPeriod[count+1];
    	for (int i = 0; i < count; i++) {
    		EOEnterpriseObject pl = (EOEnterpriseObject)list.objectAtIndex(i);
			oldPer = (EOEnterpriseObject)pl.valueForKey("oldPeriod");
			EOEnterpriseObject itog = (EOEnterpriseObject)itogs.objectAtIndex(i);
			EduPeriod per = (EduPeriod)EOUtilities.createAndInsertInstance(ec, EduPeriod.ENTITY_NAME);
			per.setListName(listName);
			NSTimestamp begin = (NSTimestamp)oldPer.valueForKey(EduPeriod.BEGIN_KEY);
			per.takeValueForKey(begin,EduPeriod.BEGIN_KEY);
			per.addObjectToBothSidesOfRelationshipWithKey(itog, "relatedItog");
			pl.addObjectToBothSidesOfRelationshipWithKey(per, "newPeriod");
			perlist[i+1]=per;
			per._perlist=perlist;
			if(details == null)
				continue;
			EOKeyGlobalID gid = (EOKeyGlobalID)ec.globalIDForObject(oldPer);
			NSArray toUpdate = (NSArray)details.removeObjectForKey(gid.keyValues()[0]);
			if(toUpdate != null) {
				toUpdate.takeValueForKey(per, "eduPeriod");
//				Enumeration enu = toUpdate.objectEnumerator();
//				while (enu.hasMoreElements()) {
//					EOEnterpriseObject pd = (EOEnterpriseObject) enu.nextElement();
//					pd.addObjectToBothSidesOfRelationshipWithKey(per, "eduPeriod");
//				}
			}
		}
		EduPeriod per = (EduPeriod)EOUtilities.createAndInsertInstance(ec, EduPeriod.ENTITY_NAME);
		per.setListName(listName);
		NSTimestamp lastDay = (NSTimestamp)oldPer.valueForKey("end");
		lastDay = lastDay.timestampByAddingGregorianUnits(0, 0, 1, 0, 0, 0);
		per.takeValueForKey(lastDay,EduPeriod.BEGIN_KEY);
		perlist[0]=per;
		per._perlist=perlist;
    	if(details != null && details.count() > 0) { // has unbound PlanDetails
    		Enumeration enu = details.keyEnumerator();
    		while (enu.hasMoreElements()) {
				Object pk = (Object) enu.nextElement();
				NSArray toUpdate = (NSArray)details.objectForKey(pk);
				oldPer = EOUtilities.objectWithPrimaryKeyValue(ec, "OldEduPeriod", pk);
				NSTimestamp end = (NSTimestamp)oldPer.valueForKey("end");
				if(EOPeriod.Utility.countDays(end, perlist[0].begin()) < 10) {
					toUpdate.takeValueForKey(perlist[count], "eduPeriod");
					continue;
				}
				NSTimestamp begin = (NSTimestamp)oldPer.valueForKey("begin");
				for (int i = 1; i < perlist.length; i++) {
					if(EOPeriod.Utility.countDays(perlist[i].begin(),begin) < 10) {
						toUpdate.takeValueForKey(perlist[i], "eduPeriod");
						break;
					}
				}
			} // details.keyEnumerator();
    	} // has unbound PlanDetails

		try {
			ec.saveChanges();
			EduPlan.logger.log(WOLogLevel.INFO, "Autogenerated periods for listName: " + listName,type);
		} catch (Exception e) {
			ec.revert();
			EduPlan.logger.log(WOLogLevel.WARNING, 
					"Falied to generate periods for listName: " + listName,new Object[]{e,type});
		}
    }
}
