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

import java.text.FieldPosition;
import java.text.SimpleDateFormat;
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
			SettingsBase devForRpr = SettingsBase.baseForKey(
					"deviationForReprimand", ec, false);
/*
			SettingsBase requireVerification = SettingsBase.baseForKey(
					"ignoreUnverifiedReasons",ec,false);
			FieldPosition fp = new FieldPosition(SimpleDateFormat.DATE_FIELD);
			NSArray weekdays = (NSArray)WOApplication.application().valueForKeyPath(
					"strings.Reusables_Strings.presets.weekdayShort");
			*/
			Enumeration enu = list.objectEnumerator();
			while (enu.hasMoreElements()) {
				EduCourse course = (EduCourse) enu.nextElement();
				NSArray lessons = course.lessons();
				if(lessons == null || lessons.count() == 0)
					continue;
				cal.setTime(now);
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
					dict = prepareDict(now, listName, ec, week, testDay);
					if(dict == null)
						continue;
					byListName.setObjectForKey(dict, listName);
				} // init dict by listName
				int minDev = 1;
				if(devForRpr != null) {
					Integer num = (Integer)devForRpr.forCourse(course).
									valueForKey(SettingsBase.NUMERIC_VALUE_KEY);
					if(num != null)
						minDev = num.intValue();
					if(minDev < 0)
						continue;
				}
				/*
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
				if(fact == plan && minDev > 0)
					continue;
//				if(holidays.count() > 0 || 
//						Various.boolForObject(dict.valueForKey("periodEdge"))) {
					int[] currWeek = new int[week];
					int ref = ((Integer)dict.valueForKey("refDay")).intValue();
					putLessons(lessons, ref, currWeek, 1);
					putVariations(variations, ref, currWeek, verifiedOnly, 1);
					
					// compare to previous week
					quals = (EOQualifier[])dict.valueForKey("prevQualifier");
					StringBuffer buf = new StringBuffer();
					int deviation = fact - plan;
					if(quals != null) {
						ref = ((Integer)dict.valueForKey("prevRef")).intValue();

						quals[2] = new EOKeyValueQualifier("course",
								EOQualifier.QualifierOperatorEqual,course);
						quals[2] = new EOAndQualifier(new NSArray(quals));
						fs.setQualifier(quals[2]);
						NSArray prevVariations = ec.objectsWithFetchSpecification(fs);
						fs.setEntityName(EduLesson.entityName);
						NSArray prevLessons = ec.objectsWithFetchSpecification(fs);
						putVariations(prevVariations, ref, currWeek, verifiedOnly, -1);
						putLessons(prevLessons, ref, currWeek, -1);

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
								currWeek[i] = 0;
							}// period end variation
						}
						if(currWeek[i] != 0) {
							if(buf.length() > 0)
								buf.append("\n");
							if(currWeek[i] > 0)
								buf.append('+');
							buf.append(currWeek[i]);
							deviation -= currWeek[i];
							buf.append(" : ");
							buf.append(weekdays.objectAtIndex(
									cal.get(Calendar.DAY_OF_WEEK) -1));
							buf.append(',').append(' ');
							MyUtility.dateFormat().format(date, buf, fp);
						}
						if (ec.hasChanges())
							ec.saveChanges();
						cal.add(Calendar.DATE, 1);
					} // review week day by day
					plan -= autoVars;
					deviation += autoVars;
					}
//				} // processing holidays
				if((deviation == 0 && buf.length() == 0)
						|| Math.abs(plan - fact) < minDev)
					continue; */
				StringBuffer buf = new StringBuffer();
				Integer deviation = checkWeekByDays(course, dict, minDev, buf);
				if(deviation == null)
					continue;
				Reprimand rpr = (Reprimand) EOUtilities
						.createAndInsertInstance(ec, ENTITY_NAME);
				rpr.setCourse(course);
				if(deviation.intValue() != 0){
					if(buf.length() > 0)
						buf.append("\n");
					if(deviation.intValue() > 0)
						buf.append('+');
					buf.append(deviation);
					buf.append(" : ");
					buf.append(dict.valueForKey("weekString"));
				}
				rpr.setContent(buf.toString());
				rpr.setAuthor((String)dict.valueForKey("author"));
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
					EOQualifier.QualifierOperatorLike,"AutoCheck??");
			quals[0] = new EOAndQualifier(new NSArray(quals));			
			EOFetchSpecification fs = new EOFetchSpecification(Reprimand.ENTITY_NAME,quals[0],null);
			list = ec.objectsWithFetchSpecification(fs);
			if(list != null && list.count() > 0) {
				enu = list.objectEnumerator();
				while (enu.hasMoreElements()) {
					Reprimand rpr = (Reprimand) enu.nextElement();
					/*
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
					*/
					if(shouldRelieve(rpr));
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
	
	protected static Integer checkWeekByDays(EduCourse course, NSDictionary dict,
			int minDev, StringBuffer buf) {
		EduPeriod eduPeriod = (EduPeriod)dict.valueForKey("eduPeriod");
		if(eduPeriod == null)
			return null;
		int plan = PlanCycle.planHoursForCourseAndPeriod(course, eduPeriod);
		if(plan == 0)
			return null;
		EOEditingContext ec = course.editingContext();
		boolean verifiedOnly = (SettingsBase.numericSettingForCourse(
				"ignoreUnverifiedReasons", course, ec,1) > 0);
		EOQualifier[] quals = (EOQualifier[])dict.valueForKey("weekQualifier");
		quals[2] = new EOKeyValueQualifier("course",
				EOQualifier.QualifierOperatorEqual,course);
		quals[2] = new EOAndQualifier(new NSArray(quals));
		EOFetchSpecification fs = new EOFetchSpecification(EduLesson.entityName,
				quals[2],MyUtility.dateSorter);
		NSArray lessons = ec.objectsWithFetchSpecification(fs);
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
		if(fact == plan && minDev > 0)
			return null;
		int week = ((Integer)dict.valueForKey("week")).intValue();
		int[] currWeek = new int[week];
		int ref = ((Integer)dict.valueForKey("refDay")).intValue();
		putLessons(lessons, ref, currWeek, 1);
		putVariations(variations, ref, currWeek, verifiedOnly, 1);
		
		// compare to previous week
		quals = (EOQualifier[])dict.valueForKey("prevQualifier");
		int deviation = fact - plan;
		if(quals != null) {
			ref = ((Integer)dict.valueForKey("prevRef")).intValue();

			quals[2] = new EOKeyValueQualifier("course",
					EOQualifier.QualifierOperatorEqual,course);
			quals[2] = new EOAndQualifier(new NSArray(quals));
			fs.setQualifier(quals[2]);
			NSArray prevVariations = ec.objectsWithFetchSpecification(fs);
			fs.setEntityName(EduLesson.entityName);
			NSArray prevLessons = ec.objectsWithFetchSpecification(fs);
			putVariations(prevVariations, ref, currWeek, verifiedOnly, -1);
			putLessons(prevLessons, ref, currWeek, -1);
			Calendar cal = Calendar.getInstance();
			cal.setTime(eduPeriod.end());
			cal.set(Calendar.HOUR_OF_DAY, 23);
			cal.set(Calendar.MINUTE, 59);
			cal.set(Calendar.SECOND, 59);
			cal.set(Calendar.MILLISECOND, 999);
			long periodEnd = cal.getTimeInMillis();
			NSTimestamp now = (NSTimestamp)dict.valueForKey("date");
			cal.setTime(now);
			cal.add(Calendar.DATE, -week);
			int autoVars = 0;
			Reason periodEndReason = null;
			String listName = (String)dict.valueForKey("listName");
			NSArray holidays = (NSArray) dict.valueForKey("holidays");
			FieldPosition fp = new FieldPosition(SimpleDateFormat.DATE_FIELD);
			NSArray weekdays = (NSArray)WOApplication.application().valueForKeyPath(
				"strings.Reusables_Strings.presets.weekdayShort");
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
//						EOGlobalID gid = ec.globalIDForObject(eduPeriod);
						String key = WOLogFormatter.formatEO(eduPeriod);
						NSDictionary values = new NSDictionary(
								new Object[] {key, new Integer(1)},
								new String[] {Reason.VERIFICATION_KEY,Reason.FLAGS_KEY});
						NSArray found = EOUtilities.objectsMatchingValues(ec,
								ENTITY_NAME, values);
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
					currWeek[i] = 0;
				}// period end variation
			}
			if(currWeek[i] != 0) {
				if(buf.length() > 0)
					buf.append("\n");
				if(currWeek[i] > 0)
					buf.append('+');
				buf.append(currWeek[i]);
				deviation -= currWeek[i];
				buf.append(" : ");
				buf.append(weekdays.objectAtIndex(
						cal.get(Calendar.DAY_OF_WEEK) -1));
				buf.append(',').append(' ');
				MyUtility.dateFormat().format(date, buf, fp);
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
		deviation += autoVars;
		}
		if((deviation == 0 && buf.length() == 0)
				|| Math.abs(plan - fact) < minDev)
			return null;
		return new Integer(deviation);
	}
	
	protected static boolean shouldRelieve(Reprimand rpr) {
		EduCourse course = rpr.course();
		EOEditingContext ec = rpr.editingContext();
		EOEnterpriseObject setting = SettingsBase.settingForCourse(
				EduPeriod.ENTITY_NAME, course, ec);
		
		String listName = (String)setting.valueForKey(SettingsBase.TEXT_VALUE_KEY);
		Integer weekDays = (Integer)setting.valueForKey(
				SettingsBase.NUMERIC_VALUE_KEY);
		int week = (weekDays == null)? 7 : weekDays.intValue();
		Calendar cal = Calendar.getInstance();
		cal.setTime(rpr.raised());
		cal.set(Calendar.HOUR_OF_DAY, 0);
		cal.add(Calendar.DATE, -SettingsReader.intForKeyPath("edu.planFactLagDays", 0));
		
		NSTimestamp end = new NSTimestamp(cal.getTimeInMillis());
		cal.add(Calendar.DATE, -week);
		NSTimestamp begin = new NSTimestamp(cal.getTimeInMillis());

		EduPeriod eduPeriod = EduPeriod.getCurrentPeriod(begin,listName,ec);
		int plan = PlanCycle.planHoursForCourseAndPeriod(course, eduPeriod);
		
		EOQualifier[] quals = new EOQualifier[3];
		quals[0] = new EOKeyValueQualifier("date",
				EOQualifier.QualifierOperatorLessThan,end);
		quals[1] = new EOKeyValueQualifier("date",
				EOQualifier.QualifierOperatorGreaterThanOrEqualTo,begin);
		quals[2] = new EOKeyValueQualifier("course",
				EOQualifier.QualifierOperatorEqual,course);
		quals[2] = new EOAndQualifier(new NSArray(quals));
		EOFetchSpecification fs = new EOFetchSpecification(EduLesson.entityName,
				quals[2],MyUtility.dateSorter);
		NSArray lessons = ec.objectsWithFetchSpecification(fs);
		int fact = (lessons == null)? 0 : lessons.count();
		fs.setEntityName(Variation.ENTITY_NAME);
		NSArray variations = ec.objectsWithFetchSpecification(fs);
		if(variations != null && variations.count() > 0) {
			boolean verifiedOnly = (SettingsBase.numericSettingForCourse(
					"ignoreUnverifiedReasons", course, ec,1) > 0);
			Enumeration venu = variations.objectEnumerator();
			while (venu.hasMoreElements()) {
				Variation var = (Variation) venu.nextElement();
				if(!(verifiedOnly && var.reason().unverified()))
					fact -= var.value().intValue();
			}
		}
		return (fact == plan);
	}

	protected static Reason createPeriodStartReason(
			EduPeriod eduPeriod, NSTimestamp prevDate) {
		String key = WOLogFormatter.formatEO(eduPeriod);
		NSDictionary values = new NSDictionary(
				new Object[] {key, new Integer(1)},
				new String[] {Reason.VERIFICATION_KEY,Reason.FLAGS_KEY});
		EOEditingContext ec = eduPeriod.editingContext();
		NSArray found = EOUtilities.objectsMatchingValues(ec, Reason.ENTITY_NAME, values);
		if(found != null && found.count() > 0)
			return (Reason)found.objectAtIndex(0);

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
		return result;
	}

	public static NSDictionary prepareDict(NSTimestamp now, String listName,
			EOEditingContext ec, int week, int testDay) {
		Calendar cal = Calendar.getInstance();
		NSMutableDictionary dict = new NSMutableDictionary();
		dict.takeValueForKey(now, "date");
		dict.takeValueForKey(listName, "listName");
		dict.takeValueForKey(new Integer(week), "week");
		cal.setTime(now);
		if(cal.get(Calendar.DAY_OF_WEEK) != testDay) {
			return null;
		} else {
			StringBuilder buf = new StringBuilder("AutoCheck");
			int weekOfYear = cal.get(Calendar.WEEK_OF_YEAR);
			if(weekOfYear < 10) {
				buf.append('0');
			}
			buf.append(weekOfYear);
			dict.takeValueForKey(buf.toString(), "author");
		}
		cal.add(Calendar.DATE, -week);
		NSTimestamp prevDate = new NSTimestamp(cal.getTimeInMillis());
		if(true) {
			StringBuffer buf = new StringBuffer(15);
			buf.append('[');
			FieldPosition fp = new FieldPosition(SimpleDateFormat.DATE_FIELD);
			SimpleDateFormat fmt = new SimpleDateFormat(SettingsReader.stringForKeyPath(
					"ui.shortDateFormat","MM-dd"));
			fmt.format(prevDate, buf, fp);
			buf.append(" - ");
			fmt.format(now.timestampByAddingGregorianUnits(0, 0, -1, 0, 0, 0), buf, fp);
			buf.append(']');
			dict.takeValueForKey(buf.toString(),"weekString");
		}
		EOQualifier[] quals = new  EOQualifier[3];
		quals[0] = new EOKeyValueQualifier("date",
				EOQualifier.QualifierOperatorLessThan,now);
		quals[1] = new EOKeyValueQualifier("date",
				EOQualifier.QualifierOperatorGreaterThanOrEqualTo,prevDate);
		dict.takeValueForKey(quals, "weekQualifier");
		dict.takeValueForKey(
				new Integer(cal.get(Calendar.DAY_OF_YEAR)), "refDay");
		EduPeriod eduPeriod = EduPeriod.getCurrentPeriod(prevDate,listName,ec);
		if(eduPeriod == null) {
			eduPeriod = EduPeriod.getCurrentPeriod(now, listName, ec);
			if(eduPeriod != null && 
					now.getTime() - eduPeriod.begin().getTime() > NSLocking.OneDay) {
//				EOGlobalID gid = ec.globalIDForObject(eduPeriod);
				createPeriodStartReason(eduPeriod, prevDate);
				dict.takeValueForKey(Boolean.TRUE, "periodEdge");
			}
			return dict;
		} else if (now.getTime() - eduPeriod.end().getTime() > NSLocking.OneDay) {
			dict.takeValueForKey(Boolean.TRUE, "periodEdge");
		}
		dict.takeValueForKey(eduPeriod, "eduPeriod");
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
				return dict;
			}
		}
		dict.takeValueForKey(holidays, "holidays");
		NSTimestamp prevStart = prevDate;
		do {
			prevDate = prevStart;
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
//			Long prevDiff = new Long(now.getTime() - prevDate.getTime());
//			dict.takeValueForKey(prevDiff, "prevDiff");
			cal.setTime(prevStart);
			dict.takeValueForKey(
					new Integer(cal.get(Calendar.DAY_OF_YEAR)), "prevRef");						
		}
		return dict;
	}
	
	public static int[] currWeek(EduCourse course,NSDictionary dict, NSArray lessons,
			NSArray variations, int week, int year, boolean verifiedOnly) {
		int[] currWeek = new int[week];
		int ref = ((Integer)dict.valueForKey("refDay")).intValue();
		putLessons(lessons, ref, currWeek, 1);
		putVariations(variations, ref, currWeek, verifiedOnly, 1);
		
		// compare to previous week
		EOQualifier[] quals = (EOQualifier[])dict.valueForKey("prevQualifier");
		if(quals != null) {
			ref = ((Integer)dict.valueForKey("prevRef")).intValue();

			quals[2] = new EOKeyValueQualifier("course",
					EOQualifier.QualifierOperatorEqual,course);
			quals[2] = new EOAndQualifier(new NSArray(quals));
			EOFetchSpecification fs = new EOFetchSpecification(
					Variation.ENTITY_NAME, quals[2], MyUtility.dateSorter);
			EOEditingContext ec = course.editingContext();
			NSArray prevVariations = ec.objectsWithFetchSpecification(fs);
			fs.setEntityName(EduLesson.entityName);
			NSArray prevLessons = ec.objectsWithFetchSpecification(fs);
			putVariations(prevVariations, ref, currWeek, verifiedOnly, -1);
			putLessons(prevLessons, ref, currWeek, -1);
		}
		return currWeek;
	}

	public static int putLessons(NSArray lessons, int ref, int[] currWeek, int factor) {
		if(lessons == null || lessons.count() == 0)
			return 0;
		Enumeration lenu = lessons.objectEnumerator();
		Calendar cal = Calendar.getInstance();
		int year = 0;
		int sum = 0;
		while (lenu.hasMoreElements()) {
			EduLesson lesson = (EduLesson) lenu.nextElement();
			cal.setTime(lesson.date());
			int idx = cal.get(Calendar.DAY_OF_YEAR) - ref;
			if(idx < 0) {
				if(year == 0) {
					cal.add(Calendar.YEAR, -1);
					year = cal.getActualMaximum(Calendar.DAY_OF_YEAR);
				}
				idx += year;
			}
			if(idx >= currWeek.length)
				continue;
			currWeek[idx] += factor;
			sum += factor;
		}
		return sum;
	}

	public static int putVariations(NSArray variations, int ref, int[] currWeek, 
			boolean verifiedOnly, int factor) {
		if(variations == null || variations.count() == 0)
			return 0;
		Calendar cal = Calendar.getInstance();
		int year = 0;
		Enumeration venu = variations.objectEnumerator();
		int sum = 0;
		while (venu.hasMoreElements()) {
			Variation var = (Variation) venu.nextElement();
			if(verifiedOnly && var.reason().unverified())
				continue;
			cal.setTime(var.date());
			int idx = cal.get(Calendar.DAY_OF_YEAR) - ref;
			if(idx < 0) {
				if(year == 0) {
					cal.add(Calendar.YEAR, -1);
					year = cal.getActualMaximum(Calendar.DAY_OF_YEAR);
				}
				idx += year;
			}
			int value = var.value().intValue();
			if(idx >= currWeek.length)
				continue;
			currWeek[idx] -= value * factor;
			sum -= value * factor;
		}
		return sum;
	}
	
	public String formattedContent() {
	   	String result = WOMessage.stringByEscapingHTMLString(content());
	    result = result.replaceAll("\n", "<br/>\n");
	    return result;
	}
	
	public static void autoRelieve(EduCourse course, NSTimestamp date, String author) {
		EOEditingContext ec = course.editingContext();
		ec.lock();
		try {
			int minDev = SettingsBase.numericSettingForCourse("deviationForReprimand",
					course, ec, 1);
			if(minDev < 0)
				return;
			int weekStart = SettingsBase.numericSettingForCourse("weekStart",
					course, ec, Calendar.MONDAY);
			Calendar cal = Calendar.getInstance();
			cal.setTime(date);
			do {
				cal.add(Calendar.DATE, 1);
			} while(cal.get(Calendar.DAY_OF_WEEK) != weekStart);
			NSTimestamp now = new NSTimestamp(cal.getTimeInMillis());
			EOQualifier[] quals = new EOQualifier[5];
			quals[0] = new EOKeyValueQualifier("course",
					EOQualifier.QualifierOperatorEqual,course);
			quals[1] = new EOKeyValueQualifier("date",
					EOQualifier.QualifierOperatorGreaterThanOrEqualTo,now);
			quals[2] = new EOAndQualifier(new NSArray(quals));			
			EOFetchSpecification fs = new EOFetchSpecification(EduLesson.entityName
					,quals[2],null);
			fs.setFetchLimit(1);
			NSArray list = ec.objectsWithFetchSpecification(fs);
			if(list == null || list.count() == 0)
				return;
			
			int week = SettingsBase.numericSettingForCourse(EduPeriod.ENTITY_NAME,
					course, ec, 7);
			String listName = SettingsBase.stringSettingForCourse(EduPeriod.ENTITY_NAME,
					course, ec);
			NSDictionary dict = prepareDict(now, listName, ec, week, weekStart);
//			week += SettingsReader.intForKeyPath("edu.planFactLagDays", 0);
			quals[1] = new EOKeyValueQualifier(Reprimand.AUTHOR_KEY,
					EOQualifier.QualifierOperatorEqual,dict.valueForKey("author"));
			quals[2] = new EOKeyValueQualifier(Reprimand.RELIEF_KEY,
					EOQualifier.QualifierOperatorEqual,NullValue);
			/*
			quals[2] = new EOKeyValueQualifier(Reprimand.RAISED_KEY,
					EOQualifier.QualifierOperatorGreaterThan, date);
			quals[3] = new EOKeyValueQualifier(Reprimand.RAISED_KEY,
					EOQualifier.QualifierOperatorLessThanOrEqualTo,
					date.timestampByAddingGregorianUnits(0, 0, week, 0, 0, 0));
			*/
			quals[2] = new EOAndQualifier(new NSArray(quals));			
			fs = new EOFetchSpecification(Reprimand.ENTITY_NAME,quals[2],null);
			list = ec.objectsWithFetchSpecification(fs);
			Reprimand rpr = null;
			if(list != null && list.count() > 0)
				rpr = (Reprimand)list.objectAtIndex(0);
			StringBuffer buf = new StringBuffer();
			Integer deviation = checkWeekByDays(course, dict, minDev, buf);
			if(deviation == null) {
				if(rpr != null) {
					rpr.setRelief(new NSTimestamp());
					rpr.setAuthor(rpr.author() + " / " + author);
					logger.log(WOLogLevel.FINE,"Automatically relieving reprimand",rpr);
					rpr = null;
				}
			} else {
				if(deviation.intValue() != 0){
					if(buf.length() > 0)
						buf.append("\n");
					if(deviation.intValue() > 0)
						buf.append('+');
					buf.append(deviation);
					buf.append(" : ");
					buf.append(dict.valueForKey("weekString"));
				}
				String newMessage = buf.toString();
				int compare = countRows(newMessage);
				if(rpr != null)
					compare -= countRows(rpr.content());
				if(compare == 0) {
					if(rpr != null && newMessage.equals(rpr.content()))
						compare--;
					else
						compare++;
				}
				if(compare > 0) {
					if(rpr != null) {
						rpr.setRelief(new NSTimestamp());
						rpr.setAuthor(rpr.author() + " / " + author);
						logger.log(WOLogLevel.FINE,"Automatically recreating reprimand",rpr);				}
					}
					rpr = (Reprimand) EOUtilities
					.createAndInsertInstance(ec, ENTITY_NAME);
					rpr.setCourse(course);
					rpr.setContent(newMessage);
					rpr.setAuthor((String)dict.valueForKey("author"));
			}
			/*
			if(list == null || list.count() == 0)
				return;
			Enumeration enu = list.objectEnumerator();
			while (enu.hasMoreElements()) {
				Reprimand rpr = (Reprimand) enu.nextElement();
				if(shouldRelieve(rpr)) {
					rpr.setRelief(new NSTimestamp());
					rpr.setAuthor(rpr.author() + " / " + author);
					logger.log(WOLogLevel.FINE,"Automatically relieving reprimand",rpr);
				}
			}*/
			if(ec.hasChanges())
				ec.saveChanges();
			if(rpr != null)
				logger.log(WOLogLevel.FINE,"Autogenerating Reprimand",rpr);
		} catch (RuntimeException e) {
			logger.log(WOLogLevel.WARNING,"Error autorelieving reprimands",
					new Object[] {course, e});
			ec.revert();
		} finally {
			ec.unlock();
		}
	}
	
	protected static int countRows(String string) {
		int result = 0;
		int idx = string.indexOf('\n');
		while(idx > 0) {
			result++;
			idx = string.indexOf('\n', idx +1);
		}
		return result;
	}
}
