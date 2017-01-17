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
import java.util.Calendar;
import java.util.Enumeration;
import java.util.TimeZone;

import net.rujel.reusables.DataBaseUtility;
import net.rujel.reusables.SettingsReader;
import net.rujel.reusables.WOLogLevel;

import com.webobjects.appserver.WOApplication;
import com.webobjects.appserver.WOContext;
import com.webobjects.eoaccess.EOUtilities;
import com.webobjects.eocontrol.EOEditingContext;
import com.webobjects.eocontrol.EOEnterpriseObject;
import com.webobjects.eocontrol.EOObjectStoreCoordinator;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSDictionary;
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

	protected static boolean copyPeriods(EOEditingContext ec, Integer eduYear, 
													EOEditingContext prevEc) {
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
	}

	protected static boolean copyPlanHours(EOEditingContext prevEc,
			EOEditingContext ec) {
		NSArray rows = EOUtilities.objectsForEntityNamed(prevEc, "PlanHours");
		if(rows == null || rows.count() == 0)
			return false;
		Enumeration enu = rows.objectEnumerator();
		while (enu.hasMoreElements()) {
			EOEnterpriseObject ph = (EOEnterpriseObject) enu.nextElement();
			EOEnterpriseObject newPh = EOUtilities.createAndInsertInstance
			(ec, "PlanHours");
			newPh.takeValueForKey(ph.valueForKey("totalHours"), "totalHours");
			newPh.takeValueForKey(ph.valueForKey("weeklyHours"), "weeklyHours");
			EOEnterpriseObject rel = (PlanCycle)ph.valueForKey("planCycle");
			rel = EOUtilities.localInstanceOfObject(ec, rel);
			newPh.addObjectToBothSidesOfRelationshipWithKey(rel, "planCycle");
//			rel = (PlanCycle)ph.valueForKey("specClass");
//			if(rel != null) {
//				rel = EOUtilities.localInstanceOfObject(ec, rel);
//				newPh.addObjectToBothSidesOfRelationshipWithKey(rel, "specClass");
//			}
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

}
