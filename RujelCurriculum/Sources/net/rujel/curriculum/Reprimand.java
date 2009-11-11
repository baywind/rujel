//  Reprimand.java

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

package net.rujel.curriculum;

import java.util.Calendar;
import java.util.Enumeration;
import java.util.logging.Logger;

import net.rujel.base.MyUtility;
import net.rujel.base.SettingsBase;
import net.rujel.eduplan.EduPeriod;
import net.rujel.eduplan.Holiday;
import net.rujel.eduplan.PlanCycle;
import net.rujel.interfaces.EOInitialiser;
import net.rujel.interfaces.EduCourse;
import net.rujel.interfaces.EduLesson;
import net.rujel.reusables.SettingsReader;
import net.rujel.reusables.WOLogLevel;

import com.webobjects.foundation.*;
import com.webobjects.eoaccess.EOUtilities;
import com.webobjects.eocontrol.*;

public class Reprimand extends _Reprimand {
	protected static Logger logger = Logger.getLogger("rujel.curriculum");

	public static void init() {
		EOInitialiser.initialiseRelationship(ENTITY_NAME,
				"course",false,"courseID","EduCourse");
	}

	public void awakeFromInsertion(EOEditingContext ec) {
		super.awakeFromInsertion(ec);
		setRaised(new NSTimestamp());
		setStatus(new Integer(0));
		setAuthor("anonymous");
	}
	
	public EduCourse course() {
		return (EduCourse)storedValueForKey("course");
	}
	
	public void setCourse(EduCourse course) {
		takeStoredValueForKey(course, "course");
	}

	public void relieve() {
		setRelief(new NSTimestamp());
	}
	
	public static void planFactCheck() {
		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.HOUR_OF_DAY, 0);
		cal.add(Calendar.DATE, -SettingsReader.intForKeyPath("edu.planFactLagDays", 0));
		
		NSTimestamp now = new NSTimestamp(cal.getTimeInMillis());
		
		Integer eduYear = MyUtility.eduYearForDate(now);

		EOEditingContext ec = new EOEditingContext();
		ec.lock();
		try {
			SettingsBase weekStart = SettingsBase.baseForKey("weekStart", ec, false);
			int testDay = Calendar.MONDAY;
			if(weekStart != null) {
				if(weekStart.byCourse() != null && weekStart.byCourse().count() > 0) {
					testDay = -1;
				} else {
					Integer num = weekStart.numericValue();
					if(num != null)
						testDay = num.intValue();
				}
			}
			if(testDay >= 0 &&
					cal.get(Calendar.DAY_OF_WEEK) != testDay) {
				return;
			}
			
			NSMutableDictionary byListName = new NSMutableDictionary();
			
			NSArray courses = EOUtilities.objectsMatchingKeyAndValue(ec,
					EduCourse.entityName, "eduYear", eduYear);
			if (courses == null || courses.count() == 0) {
				ec.unlock();
				return;
			}
			SettingsBase listSettings = SettingsBase.baseForKey(
					EduPeriod.ENTITY_NAME, ec, false);
			Enumeration enu = courses.objectEnumerator();
			int year = cal.get(Calendar.YEAR);
			while (enu.hasMoreElements()) {
				EduCourse course = (EduCourse) enu.nextElement();
				NSArray lessons = course.lessons();
				if(lessons == null || lessons.count() == 0)
					continue;
				EOEnterpriseObject setting = listSettings.forCourse(course);
				
				String listName = (String)setting.valueForKey(SettingsBase.TEXT_VALUE_KEY);
				Integer weekDays = (Integer)setting.valueForKey(
						SettingsBase.NUMERIC_VALUE_KEY);
				int week = (weekDays == null)? 7 : weekDays.intValue();
				
				NSDictionary dict = (NSDictionary)byListName.objectForKey(listName);
				if(dict == null) {
					dict = new NSMutableDictionary();
					byListName.setObjectForKey(dict, listName);
					cal.setTime(now);
					cal.add(Calendar.DATE, -week);
					NSTimestamp prevDate = new NSTimestamp(cal.getTimeInMillis());
					dict.takeValueForKey(
							new Integer(cal.get(Calendar.DAY_OF_YEAR)), "refDay");
					if(year > 2000 && year != cal.get(Calendar.YEAR)) {
						year = cal.getActualMaximum(Calendar.DAY_OF_YEAR);
						dict.takeValueForKey(new Integer(year), "year");
					}
					EduPeriod eduPeriod = EduPeriod.getCurrentPeriod(prevDate,listName,ec);
					if(eduPeriod == null)
						continue;
					dict.takeValueForKey(eduPeriod, "eduPeriod");
					EOQualifier[] quals = new  EOQualifier[3];
					quals[0] = new EOKeyValueQualifier("date",
							EOQualifier.QualifierOperatorLessThan,now);
					quals[1] = new EOKeyValueQualifier("date",
							EOQualifier.QualifierOperatorGreaterThanOrEqualTo,prevDate);
					dict.takeValueForKey(quals, "weekQualifier");
					NSArray holidays = Holiday.holidaysInDates(prevDate, now,ec,listName);
					if(holidays == null)
						holidays = NSArray.EmptyArray;
					else if(holidays.count() == 1) {
						Holiday hd = (Holiday)holidays.objectAtIndex(0);
						if(hd.begin().getTime() <= prevDate.getTime() &&
								hd.end().getTime() >= now.getTime() - NSLocking.OneDay) {
							// whole week in holidays
							dict.takeValueForKey(null, "eduPeriod");
							continue;
						}
					}
					dict.takeValueForKey(holidays, "holidays");
					NSTimestamp prevStart = null;
					while (holidays != null && holidays.count() > 0) {
						if(prevStart != null)
							prevDate = prevStart;
						Holiday hd = (Holiday)holidays.objectAtIndex(0);
						if (prevDate.after(hd.begin())) {
							cal.setTime(hd.begin());
							while (cal.get(Calendar.DAY_OF_WEEK) != testDay)
								cal.add(Calendar.DATE, -1);
							prevDate = new NSTimestamp(cal.getTimeInMillis());
						}
						prevStart = prevDate.timestampByAddingGregorianUnits(
								0, 0,-week, 0, 0, 0);
						holidays = Holiday.holidaysInDates(
								prevStart, prevDate, ec, listName);
					}
					if(prevStart != null) {
						quals = new  EOQualifier[3];
						quals[0] = new EOKeyValueQualifier("date",
								EOQualifier.QualifierOperatorLessThan,prevDate);
						quals[1] = new EOKeyValueQualifier("date",EOQualifier.
								QualifierOperatorGreaterThanOrEqualTo,prevStart);
						dict.takeValueForKey(quals, "prevQualifier");
						Long prevDiff = new Long(now.getTime() - prevDate.getTime());
						dict.takeValueForKey(prevDiff, "prevDiff");
						cal.setTime(prevStart);
						dict.takeValueForKey(
								new Integer(cal.get(Calendar.DAY_OF_YEAR)), "prevRef");						
						if(year > 2000 && year != cal.get(Calendar.YEAR)) {
							year = cal.getActualMaximum(Calendar.DAY_OF_YEAR);
							dict.takeValueForKey(new Integer(year), "year");
						}
					}
//					dict.takeValueForKey(prevDate, "prevDate");
				} // init dict by listName
				EduPeriod eduPeriod = (EduPeriod)dict.valueForKey("eduPeriod");
				if(eduPeriod == null)
					continue;
				int plan = PlanCycle.planHoursForCourseAndPeriod(course, eduPeriod);
				if(plan == 0)
					continue;
				EOQualifier[] quals = (EOQualifier[])dict.valueForKey("weekQualifier");
				quals[2] = new EOKeyValueQualifier("course",
						EOQualifier.QualifierOperatorEqual,course);
				quals[2] = new EOAndQualifier(new NSArray(quals));
				EOFetchSpecification fs = new EOFetchSpecification(EduLesson.entityName,
						quals[2],MyUtility.dateSorter);
				lessons = ec.objectsWithFetchSpecification(fs);
				int fact = (lessons == null)? 0 : lessons.count();
				fs.setEntityName(Variation.ENTITY_NAME);
				NSArray variations = ec.objectsWithFetchSpecification(fs);
				if(variations != null && variations.count() > 0) {
					Integer varSum = (Integer)variations.valueForKeyPath("@sum.value");
					fact -= varSum.intValue();
				}
				if(fact == plan)
					continue;
				NSArray holidays = (NSArray) dict.valueForKey("holidays");
				if(holidays.count() > 0) {
					int[] currWeek = new int[week];
					int ref = ((Integer)dict.valueForKey("refDay")).intValue();
					if(lessons != null && lessons.count() > 0) {
						Enumeration lenu = lessons.objectEnumerator();
						while (lenu.hasMoreElements()) {
							EduLesson lesson = (EduLesson) lenu.nextElement();
							cal.setTime(lesson.date());
							int idx = cal.get(Calendar.DAY_OF_YEAR) - ref;
							if(idx < 0)
								idx += year;
							currWeek[idx]++;
						}
					} // place lessons to currWeek
					if(variations != null && variations.count() > 0) {
						Enumeration venu = variations.objectEnumerator();
						while (venu.hasMoreElements()) {
							Variation var = (Variation) venu.nextElement();
							cal.setTime(var.date());
							int idx = cal.get(Calendar.DAY_OF_YEAR) - ref;
							if(idx < 0)
								idx += year;
							currWeek[idx] -= var.value().intValue();
						}
					} // place variations to currWeek
					
					ref = ((Integer)dict.valueForKey("prevRef")).intValue();
					
					quals = (EOQualifier[])dict.valueForKey("prevQualifier");
					quals[2] = new EOKeyValueQualifier("course",
							EOQualifier.QualifierOperatorEqual,course);
					quals[2] = new EOAndQualifier(new NSArray(quals));
					fs.setQualifier(quals[2]);
					NSArray prevVariations = ec.objectsWithFetchSpecification(fs);
					fs.setEntityName(EduLesson.entityName);
					NSArray prevLessons = ec.objectsWithFetchSpecification(fs);
					if(prevVariations != null && prevVariations.count() > 0) {
						Enumeration venu = prevVariations.objectEnumerator();
						while (venu.hasMoreElements()) {
							Variation var = (Variation) venu.nextElement();
							cal.setTime(var.date());
							int idx = cal.get(Calendar.DAY_OF_YEAR) - ref;
							if(idx < 0)
								idx += year;
							int value = var.value().intValue();
							currWeek[idx] += value;
						}
					}
					if(prevLessons != null && prevLessons.count() > 0) {
						Enumeration lenu = prevLessons.objectEnumerator();
						while (lenu.hasMoreElements()) {
							EduLesson lesson = (EduLesson) lenu.nextElement();
							cal.setTime(lesson.date());
							int idx = cal.get(Calendar.DAY_OF_YEAR) - ref;
							if(idx < 0)
								idx += year;
							currWeek[idx]--;
						}
					}
					cal.setTime(now);
					cal.add(Calendar.DATE, -week);
					int autoVars = 0;
					for (int i = 0; i < currWeek.length; i++) {
						if(currWeek[i] < 0) {
							NSTimestamp date = new NSTimestamp(cal.getTimeInMillis());
							Enumeration henu = holidays.objectEnumerator();
							while (henu.hasMoreElements()) {
								Holiday hd = (Holiday) henu.nextElement();
								if(!hd.contains(date))
									continue;
								Reason reason = Reason.reasonForHoliday(hd, true);
								Variation var = (Variation)EOUtilities.
									createAndInsertInstance(ec, Variation.ENTITY_NAME);
								var.addObjectToBothSidesOfRelationshipWithKey(
										course, "course");
								var.addObjectToBothSidesOfRelationshipWithKey(
										reason, "reason");
								var.setDate(date);
								var.setValue(new Integer(currWeek[i]));
								autoVars -= currWeek[i];
								break;
							}
						}
						cal.add(Calendar.DATE, 1);
					} // create variations for holidays
					/*if(ec.hasChanges()) {
						try {
							ec.saveChanges();
							logger.log(WOLogLevel.FINE, "Automatically created " + autoVars
									 + " variations for course", course);
						} catch (Exception e) {
							logger.log(WOLogLevel.WARNING, "Error autocreating variations",
									new Object[] {course,e});
							ec.revert();
						}
					}*/
					plan -= autoVars;
				} // processing holidays
				if(plan == fact)
					continue;
				int deviation = fact - plan;
				Reprimand rpr = (Reprimand) EOUtilities
						.createAndInsertInstance(ec, ENTITY_NAME);
				rpr.setCourse(course);
				rpr.setContent((deviation > 0) ? "+" + deviation
						: Integer.toString(deviation));
				rpr.setAuthor("PlanFactCheck Daemon");
				logger.log(WOLogLevel.FINER,"Creating Reprimand",course);
			} // iterating courses
			if (ec.hasChanges()) {
				ec.saveChanges();
			}
		} catch (Exception e) {
			logger.log(WOLogLevel.WARNING,"Error autochecking planFact",e);
			ec.revert();
		} finally {
			ec.unlock();
		}
		logger.log(WOLogLevel.FINE,"Automatic PlanFactCheck finished");
	}
}
