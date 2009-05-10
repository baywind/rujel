//VariationsPlugin.java: Class file for WO Component 'VariationsPlugin'

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

package net.rujel.curriculum;

import java.util.Enumeration;

import net.rujel.base.MyUtility;
import net.rujel.eduplan.PlanCycle;
import net.rujel.eduresults.EduPeriod;
import net.rujel.eduresults.PeriodType;
import net.rujel.interfaces.EduCourse;

import com.webobjects.appserver.*;
import com.webobjects.eocontrol.EOAndQualifier;
import com.webobjects.eocontrol.EOEditingContext;
import com.webobjects.eocontrol.EOFetchSpecification;
import com.webobjects.eocontrol.EOKeyValueQualifier;
import com.webobjects.eocontrol.EOQualifier;
import com.webobjects.foundation.*;

// Generated by the WOLips Templateengine Plug-in at Feb 10, 2009 3:17:25 PM
public class VariationsPlugin extends com.webobjects.appserver.WOComponent {
    protected EduCourse _course;
	public NSDictionary planFact;

	public VariationsPlugin(WOContext context) {
        super(context);
    }

/*	public EduCourse course() {
		if(_course == null) {
			_course = (EduCourse)valueForBinding("course");
		}
		return _course;
	}*/
	
    public void appendToResponse(WOResponse aResponse, WOContext aContext) {
    	NSTimestamp today = (NSTimestamp)session().valueForKey("today");
    	EduCourse course = (EduCourse)valueForBinding("course");
    	
    	planFact = planFact(course, today);
    	super.appendToResponse(aResponse, aContext);
    }
	

	
	public WOActionResults popup() {
		WOComponent popup = pageWithName("VariationsList");
		popup.takeValueForKey(valueForBinding("course"), "course");
		popup.takeValueForKey(context().page(), "returnPage");
//		popup.takeValueForKey(valueForBinding("currLesson"), "currLesson");
//		popup.takeValueForKey(valueForBinding("currTab"), "currTab");
		//popup.takeValueForKey(planFact, "planFact");
		return popup;
	}

	public boolean isStateless() {
		return true;
	}
	
	public boolean synchronizesVariablesWithBindings() {
		return false;
	}
	
	public void reset() {
		planFact = null;
		_course = null;	
		super.reset();
	}
	
	public static NSDictionary planFact(EduCourse course, NSTimestamp date) {
		EOEditingContext ec = course.editingContext();
		int plan = 0;
		int hours = 0;
		NSMutableDictionary result = new NSMutableDictionary();
		NSArray list = PeriodType.periodTypesForCourse(course);
		if(list != null && list.count() > 0) {
			PeriodType perType = (PeriodType)list.objectAtIndex(0);
			EOQualifier[] quals = new EOQualifier[2];
			quals[0] = new EOKeyValueQualifier(EduPeriod.EDU_YEAR_KEY,
					EOQualifier.QualifierOperatorEqual, course.eduYear());
			quals[1] = new EOKeyValueQualifier(EduPeriod.PERIOD_TYPE_KEY,
					EOQualifier.QualifierOperatorEqual,perType);
			EOFetchSpecification fs = new EOFetchSpecification(EduPeriod.ENTITY_NAME,
					new EOAndQualifier(new NSArray(quals)),MyUtility.numSorter);
			NSArray periods = ec.objectsWithFetchSpecification(fs);
			if(periods != null && periods.count() > 0) {
				Enumeration enu = periods.objectEnumerator();
				int days = 0;
				int totalWeeks = 0;
				while (enu.hasMoreElements()) {
					EduPeriod period = (EduPeriod) enu.nextElement();
					if(date.compare(period.begin()) < 0)
						break;
					hours = PlanCycle.planHoursForCourseAndPeriod(course, period);
					if(hours == 0)
						continue;
					days += period.daysInPeriod(date);
					int weeks = days / 7;
					totalWeeks += weeks;
					days = days%7;
					plan += weeks*hours;
					if(date.compare(period.end()) > 0) {
						hours = 0;
					}
				}
				result.takeValueForKey(new Integer(plan), "planPre");
				result.takeValueForKey(new Integer(hours), "maxDeviation");
				result.takeValueForKey(new Integer(days), "extraDays");
				result.takeValueForKey(new Integer(totalWeeks), "weeks");
//				if(days >= 4) {
//					result.takeValueForKey(Boolean.TRUE, "weekend");
//				}
			}
		}
		if(plan == 0)
			return result;
		list = Variation.variations(course, null, date, null);
		int plus = 0;
		int minus = 0;
		if(list != null && list.count() > 0) {
			Enumeration enu = list.objectEnumerator();
			while (enu.hasMoreElements()) {
				Variation var = (Variation) enu.nextElement();
				int value = var.value().intValue();
				if(var.isExternal()) {
					plan += value;
				} else {
					if (value > 0)
						plus += value;
					else
						minus -= value;
				}
			}
			result.takeValueForKey(new Integer(plus), "plus");
			result.takeValueForKey(new Integer(minus), "minus");
		}
		result.takeValueForKey(new Integer(plan), "plan");
		
		int fact = factOnDate(course, date);
		if(fact >= 0) {
			result.takeValueForKey(new Integer(fact), "fact");
		}
		if(fact < 0)
			return result;
		int deviation = fact - (plan + plus - minus);
		result.takeValueForKey(new Integer(deviation), "deviation");
		if(deviation < 0) {
			result.takeValueForKey(new Integer(deviation), "result");
			result.takeValueForKey("warning", "styleClass");
		} else if(deviation > hours) {
			result.takeValueForKey(new Integer(deviation - hours), "result");
			result.takeValueForKey("highlight2", "styleClass");
		} else {
			result.takeValueForKey(new Integer(0), "result");
			result.takeValueForKey("gerade", "styleClass");
		}
//		if(result.valueForKey("weekend") == Boolean.TRUE)
//			result.takeValueForKey(new Integer(plan + hours), "nextPlan");
		return result;
	}
	
	public static int factOnDate(EduCourse course, NSTimestamp date) {
		EOQualifier dateQual = new EOKeyValueQualifier("date",
				EOQualifier.QualifierOperatorLessThanOrEqualTo,date); 
		NSArray list = course.lessons();
		if(list != null && list.count() > 0) {
			list = EOQualifier.filteredArrayWithQualifier(list, dateQual);
			if(list != null) {
				return list.count();
			}
		} else {
			return -1;
		}
		return 0;
	}
}