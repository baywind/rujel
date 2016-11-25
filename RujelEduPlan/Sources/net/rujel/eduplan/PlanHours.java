//  PlanHours.java

/*
 * Copyright (c) 2008, Gennady & Michael Kushnir
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 * 
 * 	o	Redistributions of source code must retain the above copyright notice, this
 * 		list of conditions and the following disclaimer.
 * 	o	Redistributions in binary form must reproduce the above copyright notice,
 * 		this list of conditions and the following disclaimer in the documentation
 * 		and/or other materials provided with the distribution.
 * 	o	Neither the name of the RUJEL nor the names of its contributors may be used
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
import java.util.logging.Logger;

import com.webobjects.eoaccess.EOUtilities;
import com.webobjects.eocontrol.*;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSDictionary;
import com.webobjects.foundation.NSTimestamp;

import net.rujel.base.Setting;
import net.rujel.base.SettingsBase;
import net.rujel.interfaces.EduCourse;
import net.rujel.reusables.WOLogLevel;

public class PlanHours extends _PlanHours {

	public static void updatePlanHours(EOEditingContext ec) {
		NSArray<PlanHours> toUpdate = EOUtilities.objectsMatchingKeyAndValue(ec, ENTITY_NAME, GRADE_KEY, -1);
		if(toUpdate == null || toUpdate.count() == 0)
			return;
		Enumeration<PlanHours> enu = toUpdate.objectEnumerator();
		while (enu.hasMoreElements()) {
			PlanHours ph = (PlanHours) enu.nextElement();
			@SuppressWarnings("deprecation")
			PlanCycle cycle = ph.planCycle();
			if(cycle == null)
				continue;
			ph.setSection(cycle.section());
			ph.setEduSubject(cycle.subjectEO());
			ph.setGrade(cycle.grade());
			ec.saveChanges();
		}
		Logger.getLogger("rujel.base").log(WOLogLevel.INFO, 
				"Automatically updated PlanHours for new databse structure",toUpdate.count());
	}
	
	public static int planHoursForCourseAndDate(EduCourse course, NSTimestamp date) {
		EOEditingContext ec = course.editingContext();
		NSArray planDetails = (date == null)?null:EOUtilities.objectsMatchingKeyAndValue(
				ec,"PlanDetail","course", course);
		if(planDetails != null && planDetails.count() > 0) {
			Enumeration enu = planDetails.objectEnumerator();
			while (enu.hasMoreElements()) {
				EOEnterpriseObject pd = (EOEnterpriseObject) enu.nextElement();
				EduPeriod per = (EduPeriod)pd.valueForKey("eduPeriod");
				if(per.contains(date)) {
					Number hours = (Number)pd.valueForKey("weekly");
					return Math.abs(hours.intValue());
				}
			}
			return 0;
		}
		if (course.cycle() instanceof PlanCycle) {
			PlanCycle cycle = (PlanCycle) course.cycle();
			EOEnterpriseObject planHours = cycle.planHours(course.eduGroup());
			if(planHours == null)
				return 0;
			Integer w = (Integer)planHours.valueForKey("weeklyHours");
			if(w == null)
				w = cycle.weeklyHours(course)[0];
			return w;
		}
		return 0;
	}

	public static int planHoursForCourseAndPeriod(EduCourse course, EduPeriod period) {
		EOEditingContext ec = course.editingContext();
		NSArray planDetails = (period == null)?null:EOUtilities.objectsMatchingKeyAndValue(
				ec,"PlanDetail","course", course);
		if(planDetails != null && planDetails.count() > 0) {
			EOQualifier qual = new EOKeyValueQualifier("eduPeriod",
					EOQualifier.QualifierOperatorEqual,period);
			planDetails = EOQualifier.filteredArrayWithQualifier(planDetails, qual);
			if(planDetails == null || planDetails.count() == 0)
				return 0;
			EOEnterpriseObject result = (EOEnterpriseObject)planDetails.objectAtIndex(0);
			Number hours = (Number)result.valueForKey("weekly");
			return hours.intValue();
		}
		if (course.cycle() instanceof PlanCycle) {
			PlanCycle cycle = (PlanCycle) course.cycle();
			EOEnterpriseObject planHours = cycle.planHours(course.eduGroup());
			if(planHours == null)
				return 0;
			Integer w = (Integer)planHours.valueForKey("weeklyHours");
			if(w == null)
				w = cycle.weeklyHours(course)[0];
			return w;
		}
		return 0;
	}

	public static void init() {
//		EOInitialiser.initialiseRelationship("PlanHours","specClass",false,"classID","EduGroup")
//		.anyInverseRelationship().setPropagatesPrimaryKey(true);
	}
	
	@SuppressWarnings("deprecation")
	public static void _update_1_2(EOEditingContext ec) {
		NSArray toUpdate = EOUtilities.objectsMatchingKeyAndValue(ec, ENTITY_NAME, 
				GRADE_KEY, Integer.valueOf(-1));
		if(toUpdate == null || toUpdate.count() == 0)
			return;
		Enumeration enu = toUpdate.objectEnumerator();
		while (enu.hasMoreElements()) {
			PlanHours ph = (PlanHours) enu.nextElement();
			PlanCycle cycle = ph.planCycle();
			ph.setSection(cycle.section());
			ph.setEduSubject(cycle.subjectEO());
			ph.setGrade(cycle.grade());
			ec.saveChanges();
		}
	}

	@SuppressWarnings("deprecation")
	public void setPlanCycle(EOEnterpriseObject value) {
		PlanCycle cycle = (PlanCycle)value;
		setSection(cycle.section());
		setGrade(cycle.grade());
		setEduSubject(cycle.subjectEO());
		super.setPlanCycle(cycle);
	}

	public void awakeFromInsertion(EOEditingContext ec) {
		super.awakeFromInsertion(ec);
	}

	public void turnIntoFault(EOFaultHandler handler) {
		super.turnIntoFault(handler);
	}
	
	public int[] weeksAndDays(Integer eduYear) {
		EOEditingContext ec = editingContext();
		int days = 0;
		int weekDays = 7;
		NSDictionary crs = (eduYear == null)? SettingsBase.courseDict(planCycle()):
			SettingsBase.courseDict(planCycle(),eduYear);
		Setting setting = SettingsBase.settingForCourse(EduPeriod.ENTITY_NAME,crs, ec);
		if(setting != null) {
			String listName = setting.textValue();
			days = EduPeriod.daysForList(listName, null, ec);
			Integer h = setting.numericValue();
			if(h != null && h.intValue() > 0)
				weekDays = h.intValue();
		}
		if(days <= 0) {
			setting = SettingsBase.settingForCourse("defaultWeeks", crs, ec);
			if(setting == null) {
				days = 34;
			} else {
				Integer h = setting.numericValue();
				days = (h==null)?34:h.intValue();
			}
			return new int[] {days,0,7};
		} else {
			return new int[] {days/weekDays,days%weekDays,weekDays};
		}
	}

}
