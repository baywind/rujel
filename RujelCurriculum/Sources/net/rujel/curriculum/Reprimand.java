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

import java.text.DateFormat;
import java.text.FieldPosition;
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
import net.rujel.reusables.Various;
import net.rujel.reusables.WOLogFormatter;
import net.rujel.reusables.WOLogLevel;

import com.webobjects.foundation.*;
import com.webobjects.appserver.WOApplication;
import com.webobjects.appserver.WOMessage;
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
		String defaultDate = SettingsReader.stringForKeyPath("ui.defaultDate", null);
		planFactCheck(MyUtility.parseDate(defaultDate));
	}
	public static void planFactCheck(NSTimestamp onDate) {
		Calendar cal = Calendar.getInstance();
		if(onDate != null) {
			cal.setTime(onDate);
		} else {
			onDate = new NSTimestamp(cal.getTimeInMillis());
		}
		cal.set(Calendar.HOUR_OF_DAY, 0);
		cal.add(Calendar.DATE, -SettingsReader.intForKeyPath("edu.planFactLagDays", 0));
		
		NSTimestamp now = new NSTimestamp(cal.getTimeInMillis());
		Integer eduYear = MyUtility.eduYearForDate(now);

		EOEditingContext ec = new EOEditingContext();
		ec.lock();
		try {
			SettingsBase weekStart = SettingsBase.baseForKey("weekStart", ec, false);
			int testDay = Calendar.MONDAY;
			if(weekStart != null && 
					(weekStart.byCourse() == null || weekStart.byCourse().count() == 0)) {
				Integer num = weekStart.numericValue();
				if(num != null)
					testDay = num.intValue();
				weekStart = null;
			}
			if(cal.get(Calendar.DAY_OF_WEEK) != testDay)
				return;
			
			NSMutableDictionary byListName = new NSMutableDictionary();
			
			NSArray list = EOUtilities.objectsMatchingKeyAndValue(ec,
					EduCourse.entityName, "eduYear", eduYear);
			if (list == null || list.count() == 0) {
//				ec.unlock();
				return;
			}
			SettingsBase listSettings = SettingsBase.baseForKey(
					EduPeriod.ENTITY_NAME, ec, false);
			SettingsBase requireVerification = SettingsBase.baseForKey(
					"ignoreUnverifiedReasons",ec,false);
			FieldPosition fp = new FieldPosition(DateFormat.DATE_FIELD);
			NSArray weekdays = (NSArray)WOApplication.application().valueForKeyPath(
					"strings.Reusables_Strings.presets.weekdayShort");
			Enumeration enu = list.objectEnumerator();
			int year = cal.get(Calendar.YEAR);
			while (enu.hasMoreElements()) {
				EduCourse course = (EduCourse) enu.nextElement();
				NSArray lessons = course.lessons();
				if(lessons == null || lessons.count() == 0)
					continue;
				if(weekStart != null) {
					EOEnterpriseObject bc = weekStart.forCourse(course);
					Integer num = (Integer)bc.valueForKey(SettingsBase.NUMERIC_VALUE_KEY);
					if(num != null)
						testDay = num.intValue();
					if(cal.get(Calendar.DAY_OF_WEEK) != testDay)
						continue;
				}
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
					if(eduPeriod == null) {
						eduPeriod = EduPeriod.getCurrentPeriod(now, listName, ec);
						if(eduPeriod != null && 
							now.getTime() - eduPeriod.begin().getTime() > NSLocking.OneDay) {
//							EOGlobalID gid = ec.globalIDForObject(eduPeriod);
							String key = WOLogFormatter.formatEO(eduPeriod);
							NSDictionary values = new NSDictionary(
									new Object[] {key, new Integer(1)},
							new String[] {Reason.VERIFICATION_KEY,Reason.FLAGS_KEY});
							NSArray found = EOUtilities.objectsMatchingValues(ec, Reason.ENTITY_NAME, values);
							if(found == null || found.count() == 0) {
								Reason result = (Reason)EOUtilities.createAndInsertInstance(
										ec, Reason.ENTITY_NAME);
								result.takeValuesFromDictionary(values);
								result.setBegin(prevDate);
								result.setEnd(eduPeriod.begin());
								result.setReason((String)WOApplication.application().
										valueForKeyPath(
								"strings.RujelCurriculum_Curriculum.titles.periodStart") +
								' ' + eduPeriod.name());
								ec.saveChanges();
							}
							dict.takeValueForKey(Boolean.TRUE, "periodEdge");
						}
						continue;
					} else if (now.getTime() - eduPeriod.end().getTime() > NSLocking.OneDay) {
						dict.takeValueForKey(Boolean.TRUE, "periodEdge");
					}
					dict.takeValueForKey(eduPeriod, "eduPeriod");
					EOQualifier[] quals = new  EOQualifier[3];
					quals[0] = new EOKeyValueQualifier("date",
							EOQualifier.QualifierOperatorLessThan,now);
					quals[1] = new EOKeyValueQualifier("date",
							EOQualifier.QualifierOperatorGreaterThanOrEqualTo,prevDate);
					dict.takeValueForKey(quals, "weekQualifier");
					NSArray holidays = Holiday.holidaysInDates(prevDate, now,ec,listName);
					if(holidays == null) {
						holidays = NSArray.EmptyArray;
					} else  {
						NSTimestamp day = prevDate;
						for (int i = 0; i < holidays.count(); i++) {
						Holiday hd = (Holiday)holidays.objectAtIndex(i);
						if(hd.begin().getTime() > day.getTime())
							break;
						day = hd.end();
						}
						if(day.getTime() >= now.getTime() - NSLocking.OneDay) {
							// whole week in holidays
							dict.takeValueForKey(null, "eduPeriod");
							continue;
						}
					}
					dict.takeValueForKey(holidays, "holidays");
					NSTimestamp prevStart = prevDate;
					do {
						if(holidays != null && holidays.count() > 0) {
							Holiday hd = (Holiday)holidays.objectAtIndex(0);
							if (prevStart.after(hd.begin())) {
								cal.setTime(hd.begin());
							} else {
								cal.setTime(prevStart);
							}
							while (cal.get(Calendar.DAY_OF_WEEK) != testDay)
								cal.add(Calendar.DATE, -1);
							prevDate = new NSTimestamp(cal.getTimeInMillis());
						}
						prevStart = prevDate.timestampByAddingGregorianUnits(
								0, 0,-week, 0, 0, 0);
						holidays = Holiday.holidaysInDates(
								prevStart, prevDate, ec, listName);
					} while (holidays != null && holidays.count() > 0);
					eduPeriod = EduPeriod.getCurrentPeriod(prevStart, listName, ec);
					if(eduPeriod == null)
						prevStart = null;
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
				boolean verifiedOnly = (requireVerification != null &&
						Various.boolForObject(requireVerification.forCourse(course)
								.valueForKey(SettingsBase.NUMERIC_VALUE_KEY)));
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
					Enumeration venu = variations.objectEnumerator();
					while (venu.hasMoreElements()) {
						Variation var = (Variation) venu.nextElement();
						if(!(verifiedOnly && var.reason().unverified()))
							fact -= var.value().intValue();
					}
				}
				if(fact == plan)
					continue;
//				if(holidays.count() > 0 || 
//						Various.boolForObject(dict.valueForKey("periodEdge"))) {
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
							if(verifiedOnly && var.reason().unverified())
								continue;
							cal.setTime(var.date());
							int idx = cal.get(Calendar.DAY_OF_YEAR) - ref;
							if(idx < 0)
								idx += year;
							currWeek[idx] -= var.value().intValue();
						}
					} // place variations to currWeek
					
					// compare to previous week
					quals = (EOQualifier[])dict.valueForKey("prevQualifier");
					StringBuffer buf = new StringBuffer();
					Integer singleDev = new Integer(0);
					if(quals != null) {
					ref = ((Integer)dict.valueForKey("prevRef")).intValue();
					
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
							if(verifiedOnly && var.reason().unverified())
								continue;
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
					cal.setTime(eduPeriod.end());
					cal.set(Calendar.HOUR_OF_DAY, 23);
					cal.set(Calendar.MINUTE, 59);
					cal.set(Calendar.SECOND, 59);
					cal.set(Calendar.MILLISECOND, 999);
					long periodEnd = cal.getTimeInMillis();
					cal.setTime(now);
					cal.add(Calendar.DATE, -week);
					int autoVars = 0;
					Reason periodEndReason = null;
					NSArray holidays = (NSArray) dict.valueForKey("holidays");
					for (int i = 0; i < currWeek.length; i++) {
						NSTimestamp date = new NSTimestamp(cal.getTimeInMillis());
						if(currWeek[i] < 0) {
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
								currWeek[i] = 0;
								break;
							} // holiday variation
							if(currWeek[i] < 0 && date.getTime() > periodEnd &&
								EduPeriod.getCurrentPeriod(date, listName, ec) == null) {
								if(periodEndReason == null) {
//									EOGlobalID gid = ec.globalIDForObject(eduPeriod);
									String key = WOLogFormatter.formatEO(eduPeriod);
									NSDictionary values = new NSDictionary(
											new Object[] {key, new Integer(1)},
											new String[] {Reason.VERIFICATION_KEY,Reason.FLAGS_KEY});
									NSArray found = EOUtilities.objectsMatchingValues(ec, ENTITY_NAME, values);
									if(found == null || found.count() == 0) {
										periodEndReason = (Reason)EOUtilities.createAndInsertInstance(
												ec, ENTITY_NAME);
										periodEndReason.takeValuesFromDictionary(values);
										periodEndReason.setBegin(eduPeriod.end().
												timestampByAddingGregorianUnits(0, 0, 1, 0, 0, 0));
										periodEndReason.setEnd(now);
										periodEndReason.setReason((String)WOApplication.application().
												valueForKeyPath(
												"strings.RujelCurriculum_Curriculum.titles.periodEnd") +
												' ' + eduPeriod.name());
									} else {
										periodEndReason = (Reason)found.objectAtIndex(0);
									}
								} // init periodEndReason
								Variation var = (Variation)EOUtilities.
								createAndInsertInstance(ec, Variation.ENTITY_NAME);
								var.addObjectToBothSidesOfRelationshipWithKey(
										course, "course");
								var.addObjectToBothSidesOfRelationshipWithKey(
										periodEndReason, "reason");
								var.setDate(date);
								var.setValue(new Integer(currWeek[i]));
								autoVars -= currWeek[i];
							}// period end variation
						}
						if(currWeek[i] != 0) {
							if(buf.length() > 0)
								buf.append("\n");
							if(currWeek[i] > 0)
								buf.append('+');
							buf.append(currWeek[i]);
							buf.append(" : ");
							buf.append(weekdays.objectAtIndex(
									cal.get(Calendar.DAY_OF_WEEK) -1));
							buf.append(',').append(' ');
							MyUtility.dateFormat().format(date, buf, fp);
							if(singleDev != null) {
								if(singleDev.intValue() == 0)
									singleDev = new Integer(currWeek[i]);
								else
									singleDev = null;
							}
						}
						if (ec.hasChanges())
							ec.saveChanges();
						cal.add(Calendar.DATE, 1);
					} // review week day by day
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
					}
//				} // processing holidays
				if(plan == fact)
					continue;
				int deviation = fact - plan;
				Reprimand rpr = (Reprimand) EOUtilities
						.createAndInsertInstance(ec, ENTITY_NAME);
				rpr.setCourse(course);
				if(buf.length() > 0) {
					if(singleDev == null || singleDev.intValue() != deviation) {
						buf.append("\n").append(WOApplication.application().valueForKeyPath(
						"strings.Reusables_Strings.dataTypes.total")).append(" : ");
						if(deviation > 0)
							buf.append('+');
						buf.append(deviation);
					}
				} else {
					if(deviation > 0)
						buf.append('+');
					buf.append(deviation);
				}
				rpr.setContent(buf.toString());
				rpr.setAuthor("AutoCheck");
				if(onDate != null)
					rpr.setRaised(onDate);
				logger.log(WOLogLevel.FINER,"Creating Reprimand",course);
				if (ec.hasChanges()) {
					ec.saveChanges();
				}
			} // iterating courses
			// check previous reprimands
			EOQualifier[] quals = new EOQualifier[3];
			quals[0] = new EOKeyValueQualifier(Reprimand.RAISED_KEY,
					EOQualifier.QualifierOperatorLessThan, now);
			quals[1] = new EOKeyValueQualifier(Reprimand.RELIEF_KEY,
					EOQualifier.QualifierOperatorEqual,NullValue);
			quals[2] = new EOKeyValueQualifier(Reprimand.AUTHOR_KEY,
					EOQualifier.QualifierOperatorEqual,"AutoCheck");
			quals[0] = new EOAndQualifier(new NSArray(quals));			
			EOFetchSpecification fs = new EOFetchSpecification(Reprimand.ENTITY_NAME,quals[0],null);
			list = ec.objectsWithFetchSpecification(fs);
			if(list != null && list.count() > 0) {
				enu = list.objectEnumerator();
				while (enu.hasMoreElements()) {
					Reprimand rpr = (Reprimand) enu.nextElement();
					EduCourse course = rpr.course();
					EOEnterpriseObject setting = listSettings.forCourse(course);
					
					String listName = (String)setting.valueForKey(SettingsBase.TEXT_VALUE_KEY);
					Integer weekDays = (Integer)setting.valueForKey(
							SettingsBase.NUMERIC_VALUE_KEY);
					int week = (weekDays == null)? 7 : weekDays.intValue();
					cal.setTime(rpr.raised());
					cal.set(Calendar.HOUR_OF_DAY, 0);
					cal.add(Calendar.DATE, -SettingsReader.intForKeyPath("edu.planFactLagDays", 0));
					
					NSTimestamp end = new NSTimestamp(cal.getTimeInMillis());
					cal.add(Calendar.DATE, -week);
					NSTimestamp begin = new NSTimestamp(cal.getTimeInMillis());

					EduPeriod eduPeriod = EduPeriod.getCurrentPeriod(begin,listName,ec);
					int plan = PlanCycle.planHoursForCourseAndPeriod(course, eduPeriod);

					quals[0] = new EOKeyValueQualifier("date",
							EOQualifier.QualifierOperatorLessThan,end);
					quals[1] = new EOKeyValueQualifier("date",
							EOQualifier.QualifierOperatorGreaterThanOrEqualTo,begin);
					quals[2] = new EOKeyValueQualifier("course",
							EOQualifier.QualifierOperatorEqual,course);
					quals[2] = new EOAndQualifier(new NSArray(quals));
					fs = new EOFetchSpecification(EduLesson.entityName,
							quals[2],MyUtility.dateSorter);
					NSArray lessons = ec.objectsWithFetchSpecification(fs);
					int fact = (lessons == null)? 0 : lessons.count();
					fs.setEntityName(Variation.ENTITY_NAME);
					NSArray variations = ec.objectsWithFetchSpecification(fs);
					if(variations != null && variations.count() > 0) {
						boolean verifiedOnly = (requireVerification != null &&
								Various.boolForObject(requireVerification.forCourse(course)
										.valueForKey(SettingsBase.NUMERIC_VALUE_KEY)));
						Enumeration venu = variations.objectEnumerator();
						while (venu.hasMoreElements()) {
							Variation var = (Variation) venu.nextElement();
							if(!(verifiedOnly && var.reason().unverified()))
								fact -= var.value().intValue();
						}
					}
					if(fact == plan)
						rpr.setRelief(onDate);
				} // enumerate previous reprimands
				if(ec.hasChanges())
					ec.saveChanges();
			}
			logger.log(WOLogLevel.FINE,"Automatic PlanFactCheck finished");
		} catch (Exception e) {
			logger.log(WOLogLevel.WARNING,"Error autochecking planFact",e);
			ec.revert();
		} finally {
			ec.unlock();
		}
	}
	
	public String formattedContent() {
	   	String result = WOMessage.stringByEscapingHTMLString(content());
	    result = result.replaceAll("\n", "<br/>\n");
	    return result;
	}
}
