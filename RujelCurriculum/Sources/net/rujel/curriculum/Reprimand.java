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
import net.rujel.interfaces.EOInitialiser;
import net.rujel.interfaces.EduCourse;
import net.rujel.interfaces.EduLesson;
import net.rujel.reusables.DataBaseConnector;
import net.rujel.reusables.SettingsReader;
import net.rujel.reusables.WOLogLevel;

import com.webobjects.foundation.*;
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
//			String checkTime = SettingsReader.stringForKeyPath(
//					"edu.planFactCheckTime", "0:59");
//			MyUtility.setTime(cal, checkTime);
		} else {
			onDate = new NSTimestamp(cal.getTimeInMillis());
		}
		cal.set(Calendar.HOUR_OF_DAY, 12);
		cal.add(Calendar.DATE, -SettingsReader.intForKeyPath("edu.planFactLagDays", 0));
		
		NSTimestamp now = new NSTimestamp(cal.getTimeInMillis());
		Integer eduYear = MyUtility.eduYearForDate(now);

		EOObjectStore os = EOObjectStoreCoordinator.defaultCoordinator();
		if(SettingsReader.boolForKeyPath("dbConnection.yearTag", false)) {
			os = DataBaseConnector.objectStoreForTag(eduYear.toString());
		}
		EOEditingContext ec = new EOEditingContext(os);
		ec.lock();
		try {
			SettingsBase weekStart = SettingsBase.baseForKey("weekStart", ec, false);
			int testDay = Calendar.MONDAY;
			if(weekStart != null) {
				Integer num = weekStart.numericValue();
				if(num != null)
					testDay = num.intValue();
				if(weekStart.isSingle())
					weekStart = null;
			}
			if(cal.get(Calendar.DAY_OF_WEEK) != testDay) {
				if(weekStart == null)
					return;
				Integer day = new Integer(cal.get(Calendar.DAY_OF_WEEK));
				Enumeration enu = weekStart.qualifiedSettings().objectEnumerator();
				while (enu.hasMoreElements()) {
					EOEnterpriseObject qs = (EOEnterpriseObject) enu.nextElement();
					if(day.equals(qs.valueForKey(SettingsBase.NUMERIC_VALUE_KEY))) {
						day = null;
						break;
					}
				}
				if(day != null)
					return;
			}
			NSArray list = EOUtilities.objectsMatchingKeyAndValue(ec,
					EduCourse.entityName, "eduYear", eduYear);
			if (list == null || list.count() == 0) {
//				ec.unlock();
				return;
			}
			String author; {
				StringBuilder buf = new StringBuilder("AutoCheck");
				int weekOfYear = cal.get(Calendar.WEEK_OF_YEAR);
				if(weekOfYear < 10) {
					buf.append('0');
				}
				buf.append(weekOfYear);
				author = buf.toString();
			}
			cal.add(Calendar.DATE, -2);
			NSTimestamp day = new NSTimestamp(cal.getTimeInMillis());
			SettingsBase devForRpr = SettingsBase.baseForKey("deviationForReprimand", ec, false);
			SettingsBase widget = SettingsBase.baseForKey("PlanFactWidget", ec, false);
			Enumeration enu = list.objectEnumerator();
			while (enu.hasMoreElements()) { // all courses
				EduCourse course = (EduCourse) enu.nextElement();
				if(widget != null) {
					EOEnterpriseObject setting = widget.forCourse(course);
					if("hide".equals(setting.valueForKey(SettingsBase.TEXT_VALUE_KEY)))
						continue;
				}
				cal.setTime(now);
				if(weekStart != null) {
					EOEnterpriseObject bc = weekStart.forCourse(course);
					Integer num = (Integer)bc.valueForKey(SettingsBase.NUMERIC_VALUE_KEY);
					if(num != null)
						testDay = num.intValue();
					if(cal.get(Calendar.DAY_OF_WEEK) != testDay)
						continue;
				}

				NSArray lessons = course.lessons();
				if(lessons == null || lessons.count() == 0)
					continue;
				
				int minDev = 1;
				if(devForRpr != null) {
					Integer num = (Integer)devForRpr.forCourse(course).
									valueForKey(SettingsBase.NUMERIC_VALUE_KEY);
					if(num != null)
						minDev = num.intValue();
					if(minDev < 0)
						continue;
				}

				WeekFootprint weekFootprint = new WeekFootprint(course);
				weekFootprint.setDate(day);
				StringBuffer buf = new StringBuffer();
				Integer deviation = weekFootprint.checkWeek(buf);
				if(deviation == null || Math.abs(deviation.intValue()) < minDev)
					continue;
				EOQualifier[] quals = new EOQualifier[3];
				quals[0] = new EOKeyValueQualifier("course",
						EOQualifier.QualifierOperatorEqual,course);
				quals[1] = new EOKeyValueQualifier(Reprimand.AUTHOR_KEY,
						EOQualifier.QualifierOperatorEqual,author);
				quals[2] = new EOKeyValueQualifier(Reprimand.RELIEF_KEY,
						EOQualifier.QualifierOperatorEqual,NullValue);
				quals[2] = new EOAndQualifier(new NSArray(quals));			
				EOFetchSpecification fs = new EOFetchSpecification(
						Reprimand.ENTITY_NAME,quals[2],null);
				list = ec.objectsWithFetchSpecification(fs);
				Reprimand rpr = null;
				if(list != null && list.count() > 0) {
					rpr = (Reprimand)list.objectAtIndex(0);
				} else {
					rpr = (Reprimand) EOUtilities.createAndInsertInstance(ec, ENTITY_NAME);
					rpr.setCourse(course);
					rpr.setAuthor(author);
				}
				rpr.setContent(buf.toString());
				if(onDate != null)
					rpr.setRaised(onDate);
				logger.log(WOLogLevel.FINER,"Creating Reprimand",course);
				if (ec.hasChanges()) {
					try {
						ec.saveChanges();
					} catch (Exception e) {
						logger.log(WOLogLevel.WARNING,"Error saving reprimand",
								new Object[] {course,now,e});
						ec.revert();
					}
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
					String weekName = rpr.author();
					weekName = weekName.substring(9);
					int weekNum = Integer.parseInt(weekName);
					EduCourse course = rpr.course();
					if(weekStart != null) {
						EOEnterpriseObject bc = weekStart.forCourse(course);
						Integer num = (Integer)bc.valueForKey(SettingsBase.NUMERIC_VALUE_KEY);
						if(num != null)
							testDay = num.intValue();
						else
							testDay = weekStart.numericValue().intValue();
					}
					cal.setTime(onDate);
					if(weekNum > cal.get(Calendar.WEEK_OF_YEAR))
						cal.add(Calendar.YEAR, -1);
					cal.set(Calendar.WEEK_OF_YEAR, weekNum -1);
					cal.set(Calendar.DAY_OF_WEEK, testDay);
					now = new NSTimestamp(cal.getTimeInMillis());
					WeekFootprint weekFootprint = new WeekFootprint(course);
					weekFootprint.setDate(now);
					int minDev = 1;
					if(devForRpr != null) {
						Integer num = (Integer)devForRpr.forCourse(course).
										valueForKey(SettingsBase.NUMERIC_VALUE_KEY);
						if(num != null)
							minDev = num.intValue();
						if(minDev < 0)
							continue;
					}
					Integer deviation = weekFootprint.checkWeek(null);
					if(deviation == null || Math.abs(deviation.intValue()) < minDev)
						rpr.setRelief(onDate);
				} // enumerate previous reprimands
				if (ec.hasChanges()) {
					try {
						ec.saveChanges();
					} catch (Exception e) {
						logger.log(WOLogLevel.WARNING,"Error saving after planFact check",
								new Object[] {onDate,e});
						ec.revert();
					}
				}
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
	
	public static void autoRelieve(EduCourse course, NSTimestamp date, String author, 
			WeekFootprint weekFootprint) {
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
			String auto; {
				StringBuilder buf = new StringBuilder("AutoCheck");
				int weekOfYear = cal.get(Calendar.WEEK_OF_YEAR);
				if(weekOfYear < 10) {
					buf.append('0');
				}
				buf.append(weekOfYear);
				auto = buf.toString();
			}
			NSTimestamp now = new NSTimestamp(cal.getTimeInMillis());
			EOQualifier[] quals = new EOQualifier[3];
			quals[0] = new EOKeyValueQualifier("course",
					EOQualifier.QualifierOperatorEqual,course);
			quals[1] = new EOKeyValueQualifier("date",
					EOQualifier.QualifierOperatorGreaterThanOrEqualTo,now);
			quals[2] = new EOAndQualifier(new NSArray(quals));			
			EOFetchSpecification fs = new EOFetchSpecification(EduLesson.entityName
					,quals[2],null);
			fs.setFetchLimit(1);
			NSArray list = ec.objectsWithFetchSpecification(fs);
			final boolean lastWeek = (list == null || list.count() == 0);
			if(lastWeek) {
				fs.setQualifier(quals[0]);
				list = ec.objectsWithFetchSpecification(fs);
				if(list == null || list.count() == 0)
					return;
				list = null;
			}
			if(weekFootprint == null)
				weekFootprint = new WeekFootprint(course);
			weekFootprint.setDate(date);
			int week = SettingsBase.numericSettingForCourse(EduPeriod.ENTITY_NAME,
					course, ec, 7);
			String listName = SettingsBase.stringSettingForCourse(EduPeriod.ENTITY_NAME,
					course, ec);
			quals[2] = new EOKeyValueQualifier(Reprimand.RELIEF_KEY,
					EOQualifier.QualifierOperatorEqual,NullValue);
			Reprimand rpr = null;
			quals[1] = new EOKeyValueQualifier(Reprimand.AUTHOR_KEY,
					EOQualifier.QualifierOperatorEqual,auto);
			quals[1] = new EOAndQualifier(new NSArray(quals));			
			fs = new EOFetchSpecification(Reprimand.ENTITY_NAME,quals[1],null);
			list = ec.objectsWithFetchSpecification(fs);
			if(list != null && list.count() > 0) {
				rpr = (Reprimand)list.objectAtIndex(0);
				if(lastWeek) {
					Integer deviation = weekFootprint.checkWeek(null);
					if(deviation == null || Math.abs(deviation.intValue()) < minDev) {
						rpr.setRelief(new NSTimestamp());
						rpr.setAuthor(rpr.author() + " / " + author);
						logger.log(WOLogLevel.FINE,
								"Automatically relieving reprimand",rpr);
					}
					rpr = null;
				}
			}
			if(lastWeek) { // check previous week
				cal.add(Calendar.DATE, -week);
				 while(true) {
					StringBuilder buf = new StringBuilder("AutoCheck");
					int weekOfYear = cal.get(Calendar.WEEK_OF_YEAR);
					if(weekOfYear < 10) {
						buf.append('0');
					}
					buf.append(weekOfYear);
					auto = buf.toString();
					
					cal.add(Calendar.DATE, -1);
					NSTimestamp to = new NSTimestamp(cal.getTimeInMillis());
					cal.add(Calendar.DATE, 1 -week);
					NSTimestamp since = new NSTimestamp(cal.getTimeInMillis());
					now = since;
					if(Holiday.freeDaysInDates(since, to, ec,listName) < week)
						break;
				}
				quals[1] = new EOKeyValueQualifier(Reprimand.AUTHOR_KEY,
						EOQualifier.QualifierOperatorEqual,auto);
				quals[1] = new EOAndQualifier(new NSArray(quals));			
				fs = new EOFetchSpecification(Reprimand.ENTITY_NAME,quals[1],null);
				list = ec.objectsWithFetchSpecification(fs);
				if(list != null && list.count() > 0) {
					rpr = (Reprimand)list.objectAtIndex(0);
				}
				weekFootprint.setDate(now);
			}
			StringBuffer buf = new StringBuffer();
			Integer deviation = weekFootprint.checkWeek(buf);
			if(deviation == null || Math.abs(deviation.intValue()) < minDev) {
				if(rpr != null) {
					rpr.setRelief(new NSTimestamp());
					rpr.setAuthor(rpr.author() + " / " + author);
					logger.log(WOLogLevel.FINE,"Automatically relieving reprimand",rpr);
					rpr = null;
				}
			} else {
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
						logger.log(WOLogLevel.FINE,"Automatically recreating reprimand",rpr);
					}
					rpr = (Reprimand) EOUtilities.createAndInsertInstance(ec, ENTITY_NAME);
					rpr.setCourse(course);
					rpr.setContent(newMessage);
					rpr.setAuthor(auto);
				}
			}
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
