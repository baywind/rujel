// PlanCycle.java

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

import net.rujel.base.Indexer;
import net.rujel.base.SettingsBase;
import net.rujel.interfaces.*;
import net.rujel.reusables.*;

import com.webobjects.foundation.*;
import com.webobjects.foundation.NSComparator.ComparisonException;
import com.webobjects.appserver.WOContext;
import com.webobjects.appserver.WOSession;
import com.webobjects.eoaccess.EORelationship;
import com.webobjects.eoaccess.EOUtilities;
import com.webobjects.eocontrol.*;

public class PlanCycle extends _PlanCycle implements EduCycle
{
	@Deprecated
	public static Object init(Object obj, WOContext ctx) {
		return EduPlan.init(obj, ctx);
	}
	
	public static void init() {
		EOInitialiser.initialiseRelationship("PlanHours","specClass",false,"classID","EduGroup")
				.anyInverseRelationship().setPropagatesPrimaryKey(true);
		EORelationship back = EOInitialiser.initialiseRelationship(
				"PlanDetail","course",false,"courseID","EduCourse").anyInverseRelationship();
		back.setPropagatesPrimaryKey(true);
		back.setDeleteRule(EOClassDescription.DeleteRuleCascade);
	}
	
    public PlanCycle() {
        super();
    }

	public Integer subgroups() {
		Integer value = (Integer)valueForKeyPath("subjectEO.subgroups");
		if(value == null)
			return value = new Integer(0);
		return value;
	}

	public void awakeFromInsertion(EOEditingContext ec) {
		super.awakeFromInsertion(ec);
		Integer zero = new Integer(0);
		setGrade(zero);
		setLevel(zero);
		setSchool(school(ec));
	}
	
	protected static Integer school(EOEditingContext ec) {
		Integer school = null;
		if(ec instanceof SessionedEditingContext) {
			WOSession ses = (WOSession)((SessionedEditingContext)ec).session();
			school = (Integer)ses.valueForKey("school");
		}
		if(school == null) {
			school = new Integer(SettingsReader.intForKeyPath("schoolNumber", 0));
		}
		return school;
	}
	
	protected WOSession session() {
		EOEditingContext ec = editingContext();
		WOSession ses = null;
		if(ec instanceof SessionedEditingContext) {
			ses = (WOSession)((SessionedEditingContext)ec).session();
		}
		return ses;
	}
	
	protected Object sesValueForKey(String key) {
		WOSession ses = session();
		if(ses == null)
			return null;
		return ses.valueForKey(key);
	}
	protected Object sesValueForKey(String key, Object dflt) {
		Object result = sesValueForKey(key);
		return (result == null)?dflt:result;
	}
/*
    // If you add instance variables to store property values you
    // should add empty implementions of the Serialization methods
    // to avoid unnecessary overhead (the properties will be
    // serialized for you in the superclass).
    private void writeObject(java.io.ObjectOutputStream out) throws java.io.IOException {
    }

    private void readObject(java.io.ObjectInputStream in) 
    		throws java.io.IOException, java.lang.ClassNotFoundException {
    }
*/

	public static NSArray cyclesForSubject(Subject subject) {
		EOEditingContext ec = subject.editingContext();
		NSArray found = allCyclesFor(null, subject, school(ec), ec);
		if(found == null || found.count() == 0)
			return NSArray.EmptyArray;
		NSMutableArray result = new NSMutableArray();
		Enumeration enu = found.objectEnumerator();
		while (enu.hasMoreElements()) {
			PlanCycle cycle = (PlanCycle) enu.nextElement();
			NSArray hours = cycle.planHours();
			if(hours != null && hours.count() > 0)
				result.addObject(cycle);
		}
		try {
			result.sortUsingComparator(AdaptingComparator.sharedInstance);
		} catch (ComparisonException e) {
			e.printStackTrace();
		}
		return result;
	}
	
	public static NSArray allSubjects(EOEditingContext ec) {
		NSArray cycles = EOUtilities.objectsMatchingKeyAndValue(ec,
				ENTITY_NAME, SCHOOL_KEY, school(ec));
		if(cycles == null || cycles.count() == 0)
			return null;
		NSMutableArray result = new NSMutableArray();
		Enumeration enu = cycles.objectEnumerator();
		while (enu.hasMoreElements()) {
			PlanCycle cycle = (PlanCycle) enu.nextElement();
			NSArray hours = cycle.planHours();
			if(hours == null || hours.count() == 0)
				continue;
			if(!result.containsObject(cycle.subjectEO()))
				result.addObject(cycle.subjectEO());
		}
		try {
			SubjectComparator comparator = new SubjectComparator();
			result.sortUsingComparator(comparator);
		} catch (ComparisonException e) {
			e.printStackTrace();
		}
		return result;
	}

	public static NSArray allCyclesFor(Integer grade, Subject subject, 
				Integer school, EOEditingContext ec) {
		NSMutableArray  quals = new NSMutableArray();
		if(school != null)
			quals.addObject(new EOKeyValueQualifier(SCHOOL_KEY,
					EOQualifier.QualifierOperatorEqual,school));
		if(grade != null)
			quals.addObject(new EOKeyValueQualifier(GRADE_KEY,
					EOQualifier.QualifierOperatorEqual,grade));
		if(subject != null)
			quals.addObject(new EOKeyValueQualifier(SUBJECT_EO_KEY,
					EOQualifier.QualifierOperatorEqual,subject));
		EOFetchSpecification fs = new EOFetchSpecification("PlanCycle",
				new EOAndQualifier(quals),null);
		return ec.objectsWithFetchSpecification(fs);		
	}
	
	public static NSArray cyclesForGrade(Integer grade, EOEditingContext ec) {
		NSArray found = allCyclesFor(grade,null, school(ec), ec);
		if(found == null || found.count() == 0)
			return NSArray.EmptyArray;
		Enumeration enu = found.objectEnumerator();
		NSMutableArray result = new NSMutableArray();
		while (enu.hasMoreElements()) {
			PlanCycle cycle = (PlanCycle) enu.nextElement();
			if(cycle.planHours(null) != null)
				result.addObject(cycle);
		}
		EOSortOrdering.sortArrayUsingKeyOrderArray(result,SubjectComparator.sorter);
		return result;
	}
	
	public static NSArray cyclesForEduGroup(EduGroup group) {
		EOEditingContext ec = group.editingContext();
		NSArray found = allCyclesFor(group.grade(),null, school(ec), ec);
		if(found == null || found.count() == 0)
			return NSArray.EmptyArray;
		Enumeration enu = found.objectEnumerator();
		NSMutableArray result = new NSMutableArray();
		while (enu.hasMoreElements()) {
			PlanCycle cycle = (PlanCycle) enu.nextElement();
			if(cycle.planHours(group) != null)
				result.addObject(cycle);
		}
		EOSortOrdering.sortArrayUsingKeyOrderArray(result,SubjectComparator.sorter);
		return result;
	}
	
	public static class ComparisonSupport extends EOSortOrdering.ComparisonSupport {
		SubjectComparator comparator = new SubjectComparator();
				
		public int compareAscending(Object left, Object right) {
			if(left == null || left instanceof NSKeyValueCoding.Null)
				return NSComparator.OrderedAscending;
			if(right == null || right instanceof NSKeyValueCoding.Null)
				return NSComparator.OrderedDescending;
			try {
				PlanCycle l = (PlanCycle)left;
				PlanCycle r = (PlanCycle)right;
				return comparator.compare(l.subjectEO(), r.subjectEO());
			} catch  (ComparisonException ex) {
				throw new NSForwardException(ex,"Error comparing");
			} catch (ClassCastException ce) {
				if(left instanceof PlanCycle)
					return NSComparator.OrderedAscending;
				else
					return NSComparator.OrderedDescending;
			}
		}	
		public int compareCaseInsensitiveAscending(Object left, Object right)  {
			return compareAscending(left, right) ;
		}
		
		public int compareDescending(Object left, Object right)  {
			return compareAscending(right, left);
		}
		public int compareCaseInsensitiveDescending(Object left, Object right)  {
			return compareDescending(left, right);
		}

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
					Number hours = (Number)pd.valueForKey("hours");
					return hours.intValue();
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
			Number hours = (Number)result.valueForKey("hours");
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
	
	public EOEnterpriseObject planHours(EduGroup grp, boolean strict) {
		NSArray hrs = planHours();
		if(hrs == null || hrs.count() == 0)
			return null;
		Enumeration enu = hrs.objectEnumerator();
		EOEnterpriseObject result = null;
		while (enu.hasMoreElements()) {
			EOEnterpriseObject hr = (EOEnterpriseObject) enu.nextElement();
			EduGroup gr = (EduGroup)hr.valueForKey("specClass");
			if(gr == grp)
				return hr;
			if(!strict && gr == null)
				result = hr;
		}
		return result;

	}
	
	public EOEnterpriseObject planHours(EduGroup grp) {
		return planHours(grp, false);
	}
	
	public EOEnterpriseObject createPlanHours(EduGroup grp) {
		EOEnterpriseObject hr = planHours(grp, true); 
		if(hr != null)
			return hr;
		hr = EOUtilities.createAndInsertInstance(editingContext(), "PlanHours");
		Integer zero = new Integer(0);
		hr.takeValueForKey(zero, "weeklyHours");
		hr.takeValueForKey(zero, "totalHours");
		hr.addObjectToBothSidesOfRelationshipWithKey(grp, "specClass");
		addObjectToBothSidesOfRelationshipWithKey(hr, PLAN_HOURS_KEY);
		return hr;
	}
	
	public boolean deletePlanHours(EduGroup grp) {
		EOEnterpriseObject hr = planHours(grp, true); 
		if(hr == null)
			return false;
		removeObjectFromBothSidesOfRelationshipWithKey(hr, PLAN_HOURS_KEY);
		editingContext().deleteObject(hr);
		return true;
	}

	public int[] weeksAndDays(Integer eduYear) {
		EOEditingContext ec = editingContext();
		int days = 0;
		int weekDays = 7;
		NSDictionary crs = new NSDictionary(
				new Object[] {this,eduYear},
				new String[] {"cycle","eduYear"});
		EOEnterpriseObject setting = SettingsBase.settingForCourse(EduPeriod.ENTITY_NAME,crs, ec);
		if(setting != null) {
			String listName = (String)setting.valueForKey(SettingsBase.TEXT_VALUE_KEY);
			days = EduPeriod.daysForList(listName, null, ec);
			Integer h = (Integer)setting.valueForKey(SettingsBase.NUMERIC_VALUE_KEY);
			if(h != null && h.intValue() > 0)
				weekDays = h.intValue();
		}
		if(days <= 0) {
			setting = SettingsBase.settingForCourse("defaultWeeks", crs, ec);
			if(setting == null) {
				days = 34;
			} else {
				Integer h = (Integer)setting.valueForKey(SettingsBase.NUMERIC_VALUE_KEY);
				days = (h==null)?34:h.intValue();
			}
			return new int[] {days,0,7};
		} else {
			return new int[] {days/weekDays,days%weekDays,weekDays};
		}
	}
	
	protected int[] weeklyHours(EduCourse course) {
		int hours = 0;
		{
			EOEnterpriseObject ph = planHours(course.eduGroup());
			if(ph != null) {
				Integer hrs = (Integer)ph.valueForKey("totalHours");
				if(hrs != null)
					hours = hrs.intValue();
			} else {
				return new int[] {0, 0};
			}
		}
		int[] weeksAndDays = weeksAndDays(course.eduYear());
		if(weeksAndDays[0] == 0)
			return new int[] {0, hours};
		int[] weeklyHours = new int[2];
		weeklyHours[0] = hours / weeksAndDays[0];
		weeklyHours[1] = hours % weeksAndDays[0];
		if(weeklyHours[1] > weeklyHours[0]) {
			weeklyHours[0]++;
			weeklyHours[1] = hours - weeksAndDays[0]*weeklyHours[0];
		}
		return weeklyHours;
	}

	public int weekly(EduCourse course) {
		EOEnterpriseObject ph = planHours(course.eduGroup());
		if(ph != null) {
			Integer hrs = (Integer)ph.valueForKey("weeklyHours");
			if(hrs != null)
				return hrs.intValue();
		}
		return weeklyHours(course)[0];
	}
	
	public boolean calculatedTotal(EduGroup group) {
		EOEnterpriseObject ph = planHours(group);
		if(ph == null)
			return false;
		Integer t = (Integer)ph.valueForKey("totalHours");
		return (t == null || t.intValue() <= 0);
	}

	public Integer hours(NSKeyValueCoding course) {
		EOEnterpriseObject ph = planHours((course==null)?null:
			(EduGroup)course.valueForKey("eduGroup"));
		Integer h = null;
		if(ph != null) {
			h = (Integer)ph.valueForKey("totalHours");
			if(h != null && h.intValue() > 0)
				return h;
			h = (Integer)ph.valueForKey("weeklyHours");
			if(h == null || h.intValue() <= 0)
				return new Integer(0);
		}
		int count = h.intValue();
		int[] wd = weeksAndDays((course==null)?null:
			(Integer)course.valueForKey("eduYear"));
		if(count > 1) {
			count = wd[0]*count + count*wd[1]/wd[2];
		} else {
			count = wd[0];
		}
		return new Integer(count);
	}
	
	public void setSubjectEO(EOEnterpriseObject value) {
		super.setSubjectEO(value);
		setSubject((value==null)?null:(String)value.valueForKey(Subject.SUBJECT_KEY));
	}
	
	public String extraInfo() {
		NSArray list = Indexer.indexersOfType(editingContext(), "eduLevel");
		if(list == null || list.count() == 0)
			throw new UnknownKeyException("eduLevel not defined can't return extraInfo",
					this,"extraInfo");
		Indexer idx = (Indexer)list.objectAtIndex(0);
		return idx.formattedForIndex(level().intValue(), null);
	}
}
