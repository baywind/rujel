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
import net.rujel.interfaces.EOInitialiser;
import net.rujel.interfaces.EduCourse;
import net.rujel.reusables.SettingsReader;
import net.rujel.reusables.Various;
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
		setRaised(new NSTimestamp());
		setStatus(new Integer(0));
		setAuthor("anonymous");
		super.awakeFromInsertion(ec);
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
		EOEditingContext ec = new EOEditingContext();
		ec.lock();
		try {
			Calendar cal = Calendar.getInstance();
			cal.set(Calendar.HOUR_OF_DAY, 0);
			cal.add(Calendar.DATE, -SettingsReader.intForKeyPath("edu.planFactLagDays", 0));
			NSTimestamp now = new NSTimestamp(cal.getTimeInMillis());
			Integer eduYear = MyUtility.eduYearForDate(now);
			NSArray details = EduPeriod.periodsInYear(eduYear, ec);
			EOQualifier qual = Various.getEOInQualifier("eduPeriod", details);
			EOFetchSpecification fs = new EOFetchSpecification("PlanDetail",
					qual, null);
			details = ec.objectsWithFetchSpecification(fs);
			EOQualifier[] quals = null;
			boolean norm = (details == null || details.count() == 0);
			if (!norm) {
				quals = new EOQualifier[] {
						new EOKeyValueQualifier("hours",
								EOQualifier.QualifierOperatorEqual,
								new Integer(0)), null };
			}
			NSArray courses = EOUtilities.objectsMatchingKeyAndValue(ec,
					EduCourse.entityName, "eduYear", eduYear);
			if (courses == null || courses.count() == 0) {
				ec.unlock();
				return;
			}
			Enumeration enu = courses.objectEnumerator();
//			NSArray pertypeUsages = PeriodType.usagesForYear(eduYear, ec);
			NSMutableDictionary dayForListName = new NSMutableDictionary();
			while (enu.hasMoreElements()) {
				EduCourse course = (EduCourse) enu.nextElement();
				if (quals != null) {
					quals[1] = new EOKeyValueQualifier("course",
							EOQualifier.QualifierOperatorEqual, course);
					qual = new EOAndQualifier(new NSArray(quals));
					norm = (EOQualifier.filteredArrayWithQualifier(details,
							qual).count() == 0);
				}
				String listName = SettingsBase.stringSettingForCourse
										(EduPeriod.ENTITY_NAME, course, ec);
				if (norm && dayForListName.count() > 0) {
					Integer day = (Integer) dayForListName.valueForKey(listName);
					if (day != null && day.intValue() > 0) {
						logger.log(WOLogLevel.FINEST,"Skipping " + listName,course);
						continue;
					}
				}
				NSDictionary planFact = VariationsPlugin.planFact(course, now);
				Integer day = (Integer) planFact.valueForKey("extraDays");
				if (norm) {
					dayForListName.setObjectForKey(day, listName);
				}
				if (day == null || day.intValue() > 0) {
					logger.log(WOLogLevel.FINER,"Skipping " + listName + 
							": day is " + day,course);
					continue;
				}
				Integer deviation = (Integer) planFact.valueForKey("deviation");
				if (deviation == null || deviation.intValue() == 0) {
					logger.log(WOLogLevel.FINER,"Skipping: no deviation",course);
					continue;
				}
				Reprimand rpr = (Reprimand) EOUtilities
						.createAndInsertInstance(ec, ENTITY_NAME);
				rpr.setCourse(course);
				rpr.setContent((deviation.intValue() > 0) ? "+" + deviation
						: deviation.toString());
				rpr.setAuthor("PlanFactCheck Daemon");
				logger.log(WOLogLevel.FINER,"Creating Reprimand",course);
			}
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
