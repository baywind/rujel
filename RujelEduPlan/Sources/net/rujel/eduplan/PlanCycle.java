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

import java.util.Date;
import java.util.Enumeration;

import net.rujel.base.MyUtility;
import net.rujel.base.SettingsBase;
import net.rujel.interfaces.*;
import net.rujel.reusables.*;

import com.webobjects.foundation.*;
import com.webobjects.foundation.NSComparator.ComparisonException;
import com.webobjects.appserver.WOApplication;
import com.webobjects.appserver.WOContext;
import com.webobjects.appserver.WOSession;
import com.webobjects.eoaccess.EORelationship;
import com.webobjects.eoaccess.EOUtilities;
import com.webobjects.eocontrol.*;

public class PlanCycle extends _PlanCycle implements EduCycle
{
	public static Object init(Object obj, WOContext ctx) {
		if(obj == null || obj.equals("init")) {
			//init();
			EOSortOrdering.ComparisonSupport.setSupportForClass(
					new SubjectComparator.ComparisonSupport(), Subject.class);
			EOSortOrdering.ComparisonSupport.setSupportForClass(
					new PlanCycle.ComparisonSupport(), PlanCycle.class);
		} else if(obj.equals("regimes")) {
//			if(Various.boolForObject(ctx.session().valueForKeyPath("readAccess._read.EduPlan")))
//				return null;
			return WOApplication.application().valueForKeyPath(
					"strings.RujelEduPlan_EduPlan.planRegime");
		} else if("planTabs".equals(obj)) {
			return WOApplication.application().valueForKeyPath(
				"strings.RujelEduPlan_EduPlan.planTabs");
		} else if("lessonTabs".equals(obj)) {
			return EduPeriod.lessonTabs(ctx);
		} else if("periods".equals(obj)) {
			return EduPeriod.periods(ctx);
		}
		return null;
	}
	
	public static void init() {
		EOInitialiser.initialiseRelationship("PlanCycle",SPEC_CLASS_KEY,false,"classID","EduGroup")
				.anyInverseRelationship().setPropagatesPrimaryKey(true);
		EORelationship back = EOInitialiser.initialiseRelationship(
				"PlanDetail","course",false,"courseID","EduCourse").anyInverseRelationship();
		back.setPropagatesPrimaryKey(true);
		back.setDeleteRule(EOClassDescription.DeleteRuleCascade);
	}
	
	public static String SPEC_CLASS_KEY = "specClass";
	
    public PlanCycle() {
        super();
    }

	public Integer subgroups() {
		Integer value = (Integer)valueForKeyPath("subjectEO.subgroups");
		if(value == null)
			return value = new Integer(0);
		return value;
	}
	
	public EduGroup specClass() {
		return (EduGroup)storedValueForKey(SPEC_CLASS_KEY);
	}
	public void setSpecClass(EduGroup specClass) {
		takeStoredValueForKey(specClass,SPEC_CLASS_KEY);
		if(specClass != null) {
			setGrade(specClass.grade());
			/*WOSession ses = session();
			if(ses != null) {
				GlobalPlan.logger.log(WOLogLevel.COREDATA_EDITING,"Adding spec class");
			}*/
		}
	}
	
	public void setYear (Integer year) {
		int val = (year==null)?0:year.intValue();
		if(val > 100 || year == null) {
			val = val%100;
			year = new Integer(val);
		}
		super.setYear(year);
	}
	
	public void setEduYear(Integer eduYear) {
		setYear(eduYear);
	}
	public Integer eduYear() {
		Integer year = year();
		if(year == null || year.intValue() == 0)
			return null;
		return new Integer(2000 + year.intValue());
	}

	public void awakeFromInsertion(EOEditingContext ec) {
		super.awakeFromInsertion(ec);
		Integer zero = new Integer(0);
		setTotalHours(zero);
		setWeeklyHours(zero);
		setLevel(zero);
		setGrade(zero);
		setYear(zero);
		setSchool(school(ec));
	}
	
	protected static Integer school(EOEditingContext ec) {
		Integer school = null;
		if(ec instanceof SessionedEditingContext) {
			WOSession ses = (WOSession)((SessionedEditingContext)ec).session();
			school = (Integer)ses.valueForKey("school");
		} else {
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

	public static NSArray cyclesForSubjectAndYear(EOEditingContext ec, 
			EOEnterpriseObject subject, int eduYear) {
		if (eduYear > 100)
			eduYear = eduYear%100;
		Integer year = new Integer (eduYear);
		EOQualifier qual = null;
		NSMutableArray quals = new NSMutableArray();
		if(eduYear > 0) {
			quals.addObject(new EOKeyValueQualifier(YEAR_KEY,
					EOQualifier.QualifierOperatorEqual,new Integer(0)));
			quals.addObject(new EOKeyValueQualifier(YEAR_KEY,
					EOQualifier.QualifierOperatorGreaterThan,year));
			qual = new EOOrQualifier(quals);
			quals.removeAllObjects();
			quals.addObject(qual);
			quals.addObject(new EOKeyValueQualifier(SPEC_CLASS_KEY,
					EOQualifier.QualifierOperatorEqual,NullValue));
			qual = new EOAndQualifier(quals);
			quals.removeAllObjects();
			quals.addObject(qual);
			quals.addObject(new EOKeyValueQualifier(YEAR_KEY,
					EOQualifier.QualifierOperatorEqual,year));
			qual = new EOOrQualifier(quals);
		} else {
			qual = new EOKeyValueQualifier(YEAR_KEY,
					EOQualifier.QualifierOperatorEqual,year);
		}
		quals.removeAllObjects();
		quals.addObject(qual);
		quals.addObject(new EOKeyValueQualifier("school",
				EOQualifier.QualifierOperatorEqual,school(ec)));
		if(subject != null) {
			quals.addObject(new EOKeyValueQualifier(SUBJECT_EO_KEY,
					EOQualifier.QualifierOperatorEqual,subject));
		}
		qual = new EOAndQualifier(quals);
		EOFetchSpecification fs = new EOFetchSpecification("PlanCycle",qual,null);
		return ec.objectsWithFetchSpecification(fs);
	}
	
	public static NSArray subjectsForYear(EOEditingContext ec, int eduYear) {
		NSArray cycles = cyclesForSubjectAndYear(ec, null, eduYear);
		if(cycles == null || cycles.count() == 0)
			return null;
		NSMutableArray result = new NSMutableArray();
		Enumeration enu = cycles.objectEnumerator();
		while (enu.hasMoreElements()) {
			PlanCycle cycle = (PlanCycle) enu.nextElement();
			if(!result.containsObject(cycle.subjectEO()) && 
					cycle.hours() != null && cycle.hours().intValue() > 0)
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

	public static NSArray cyclesForGrade(Integer grade, EOEditingContext ec) {
		Integer year = null;
		Integer school = null;
		if(ec instanceof SessionedEditingContext) {
			WOSession ses = (WOSession)((SessionedEditingContext)ec).session();
			year = (Integer)ses.valueForKey("eduYear");
			year = new Integer(year.intValue()%100);
			school = (Integer)ses.valueForKey("school");
		} 
		if(school == null)
			school = new Integer(SettingsReader.intForKeyPath("schoolNumber", 0));
		if(year == null) {
			int eduYear = MyUtility.eduYearForDate(new Date());
			year = new Integer(eduYear%100);
		}
		EOQualifier qual = null;
		NSMutableArray quals = new NSMutableArray();
		quals.addObject(new EOKeyValueQualifier(YEAR_KEY,
				EOQualifier.QualifierOperatorEqual,new Integer(0)));
		quals.addObject(new EOKeyValueQualifier(YEAR_KEY,
				EOQualifier.QualifierOperatorGreaterThanOrEqualTo,year));
		qual = new EOOrQualifier(quals);
		quals.removeAllObjects();
		quals.addObject(qual);
		quals.addObject(new EOKeyValueQualifier(SPEC_CLASS_KEY,
				EOQualifier.QualifierOperatorEqual,NullValue));
		quals.addObject(new EOKeyValueQualifier(SCHOOL_KEY,
				EOQualifier.QualifierOperatorEqual,school(ec)));
		quals.addObject(new EOKeyValueQualifier(GRADE_KEY,
				EOQualifier.QualifierOperatorEqual,grade));		
		qual = new EOAndQualifier(quals);
		EOFetchSpecification fs = new EOFetchSpecification("PlanCycle",qual,null);
		NSArray result = ec.objectsWithFetchSpecification(fs);
		return EOSortOrdering.sortedArrayUsingKeyOrderArray(result,SubjectComparator.sorter);
	}
	
	public static NSArray cyclesForEduGroup(EduGroup group) {
		EOEditingContext ec = group.editingContext();
		NSMutableArray result = cyclesForGrade(group.grade(), ec).mutableClone();
		Integer year = null;
		if(ec instanceof SessionedEditingContext) {
			WOSession ses = (WOSession)((SessionedEditingContext)ec).session();
			year = (Integer)ses.valueForKey("eduYear");
			year = new Integer(year.intValue()%100);
		} 
		if(year == null) {
			int eduYear = MyUtility.eduYearForDate(new Date());
			year = new Integer(eduYear%100);
		}

		EOQualifier qual = new EOKeyValueQualifier(SPEC_CLASS_KEY,
				EOQualifier.QualifierOperatorEqual,group);
		NSMutableArray quals = new NSMutableArray(qual);
		quals.addObject(new EOKeyValueQualifier(YEAR_KEY,
				EOQualifier.QualifierOperatorEqual,year));
		qual = new EOAndQualifier(quals);
		EOFetchSpecification fs = new EOFetchSpecification("PlanCycle",qual,null);
		NSArray spec = ec.objectsWithFetchSpecification(fs);
		if(spec != null && spec.count() > 0) {
			quals.removeAllObjects();
			Enumeration enu = spec.objectEnumerator();
			while (enu.hasMoreElements()) {
				PlanCycle sc = (PlanCycle) enu.nextElement();
				quals.addObject(new EOKeyValueQualifier(SUBJECT_EO_KEY,
						EOQualifier.QualifierOperatorNotEqual,sc.subjectEO()));
			}
			qual = new EOAndQualifier(quals);
			EOQualifier.filterArrayWithQualifier(result, qual);
			result.addObjectsFromArray(spec);
			EOSortOrdering.sortArrayUsingKeyOrderArray(result, SubjectComparator.sorter);
		}
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
			Integer w = cycle.weeklyHours();
			if(w == null)
				w = cycle.weeklyHours(MyUtility.eduYearForDate(date))[0];
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
			Integer w = cycle.weeklyHours();
			if(w == null)
				w = cycle.weeklyHours(period.eduYear())[0];
			return w;
		}
		return 0;
	}

	public int[] weeksAndDays(Integer eduYear) {
		EOEditingContext ec = editingContext();
		int days = 0;
		int weekDays = 7;
		EOEnterpriseObject setting = SettingsBase.settingForValue(EduPeriod.ENTITY_NAME,
				this, eduYear, ec);
		if(setting != null) {
			String listName = (String)setting.valueForKey(SettingsBase.TEXT_VALUE_KEY);
			days = EduPeriod.daysForList(listName, null, ec);
			Integer h = (Integer)setting.valueForKey(SettingsBase.NUMERIC_VALUE_KEY);
			if(h != null)
				weekDays = h.intValue();
		}
		if(days <= 0) {
			setting = SettingsBase.settingForValue("defaultWeeks", this, eduYear, ec);
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
	
	
	public int[] weeklyHours(Integer eduYear) {
		int[] weeksAndDays = weeksAndDays(eduYear);
		int[] weeklyHours = new int[2]; 
		int hours = totalHours().intValue();
		if(weeksAndDays[0] == 0)
			return new int[] {0, hours};
		weeklyHours[0] = hours / weeksAndDays[0];
		weeklyHours[1] = hours % weeksAndDays[0];
		if(weeklyHours[1] > weeklyHours[0]) {
			weeklyHours[0]++;
			weeklyHours[1] = hours - weeksAndDays[0]*weeklyHours[0];
		}
		return weeklyHours;
	}

	public Integer weekly() {
		Integer w = weeklyHours();
		if(w != null && w.intValue() > 0)
			return w;
		return new Integer(weeklyHours(null)[0]);
	}
	
	public boolean calculatedTotal() {
		Integer t = totalHours();
		return (t == null || t.intValue() <= 0);
	}

	public Integer hours() {
		Integer h = totalHours();
		if(h != null && h.intValue() > 0)
			return h;
		h = weeklyHours();
		if(h == null || h.intValue() <= 0)
			return new Integer(0);
		int count = h.intValue();
		int[] wd = weeksAndDays(null);
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
}
