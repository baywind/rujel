// AutoItogModule.java

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

package net.rujel.autoitog;

import net.rujel.interfaces.*;
import net.rujel.base.MyUtility;
import net.rujel.base.SettingsBase;
import net.rujel.eduresults.*;
import net.rujel.reusables.*;

import com.webobjects.foundation.*;
import com.webobjects.eocontrol.*;
import com.webobjects.appserver.*;
import com.webobjects.eoaccess.EOUtilities;

import java.text.DateFormat;
import java.text.FieldPosition;
import java.text.SimpleDateFormat;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.Enumeration;
import java.util.NoSuchElementException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Logger;

public class AutoItogModule {
	public static final Logger logger = Logger.getLogger("rujel.autoitog");
	
	public static boolean isAvailable(NSArray active) {
		boolean res = (active.containsObject("net.rujel.eduresults.ModuleInit") &&
				active.containsObject("net.rujel.criterial.ModuleInit"));
		if(!res)
			logger.log(WOLogLevel.INFO,
					"AutoItog module requires EduResults and Criterial modules");
		return res;
	}
	
	public static Object init(Object obj, WOContext ctx) {
		if(obj == null || obj.equals("init")) {
			try {
				Object access = PlistReader.readPlist("access.plist", "RujelAutoItog", null);
				WOApplication.application().takeValueForKey(access, "defaultAccess");
			} catch (NSKeyValueCoding.UnknownKeyException e) {
				// default access not supported
			}
			AutoItog.init();
			Prognosis.init();
			StudentTimeout.init();
			CourseTimeout.init();
		} else if("scheduleTask".equals(obj)) {
			scheduleTask(ctx);
		} else if("notesAddOns".equals(obj)) {
			return notesAddOns(ctx);
		} else if("objectSaved".equals(obj)) {
			return objectSaved(ctx);
		} else if("printStudentResults".equals(obj)) {
			return printStudentResults(ctx);
		} else if("reportForStudent".equals(obj)) {
			return PrognosReport.reportForStudent(ctx.session());
		} else if("reportSettingsForStudent".equals(obj)) {
			NSDictionary reportSettings = (NSDictionary)ctx.session().
						valueForKeyPath("strings.RujelAutoItog_AutoItog.reportSettings");
			return PlistReader.cloneDictionary(reportSettings, true);
		} else if("itogExtensions".equals(obj)) {
			NSDictionary reportSettings = (NSDictionary)ctx.session().
				valueForKeyPath("strings.RujelAutoItog_AutoItog.itogExtension");
			return PlistReader.cloneDictionary(reportSettings, true);
		} else if("extItog".equals(obj)) {
//			return extItog(ctx);
		} else if("statCourseReport".equals(obj)) {
			return statCourseReport(ctx);
		} else if("groupReport".equals(obj)) {
			return groupMarksReport(ctx);
		} else if("accessModifier".equals(obj)) {
			if(SettingsBase.baseForKey(ItogMark.ENTITY_NAME, 
					ctx.session().defaultEditingContext(), false) == null)
				return null;
			return new ItogLock();
		} else if("completionLock".equals(obj)) {
			return new NSDictionary(new String[] {Prognosis.ENTITY_NAME,"course","student"},
					new String[] {"entity","coursePath","studentPath"});
		} else if("deleteItogContainer".equals(obj)) {
			return deleteItogContainer(ctx);
		} else if("xmlGeneration".equals(obj)) {
			return xmlGeneration(ctx);
		} else if("archiveType".equals(obj)) {
			return ctx.session().valueForKeyPath("strings.RujelAutoItog_AutoItog.archiveType");
		} else if(obj.equals("usedModels")) {
			return "AutoItog";
		}
		return null;
	}
	
	public static Object scheduleTask(WOContext ctx) {
		Timer timer = (Timer)WOApplication.application().valueForKey("timer");
		if(timer == null)
			return null;
		boolean disable = Boolean.getBoolean("AutoItog.disable")
				|| SettingsReader.boolForKeyPath("edu.disableAutoItog", false);
		if(disable)
			return null;
		EOEditingContext ec = new EOEditingContext();
		NSTimestamp day = new NSTimestamp();
		NSMutableArray alreadyScheduled = new NSMutableArray();
		ec.lock();
		try {
			// AutoItog FireDate
			NSArray found = EOUtilities.objectsMatchingKeyAndValue(ec, AutoItog.ENTITY_NAME,
					AutoItog.FIRE_DATE_KEY, day);
			if(found != null && found.count() > 0) {
				Enumeration enu = found.objectEnumerator();
				while (enu.hasMoreElements()) {
					final AutoItog ai = (AutoItog) enu.nextElement();
					if(ai.inactive()) {
						alreadyScheduled.addObject(ai);
						continue;
					}
					NSTimestamp fire =  ai.fireDateTime();
					if(fire.getTime() - System.currentTimeMillis() < 10000) {
						automateItog(ai);
					} else {
						timer.schedule(new AutoItogAutomator(ai,false),fire);
						alreadyScheduled.addObject(ai);
					}
				}
			}
		// CourseTimeout firedate
		NSArray args = new NSArray(day);
		EOQualifier qual = EOQualifier.qualifierWithQualifierFormat(
				"fireDate <= %@ AND flags < 64", args);
		Object so = new EOSortOrdering("itogID",EOSortOrdering.CompareAscending);
		so = new NSArray(so);
		EOFetchSpecification fs = new EOFetchSpecification(
				CourseTimeout.ENTITY_NAME,qual,(NSArray)so);
		found = ec.objectsWithFetchSpecification(fs);
		if(found != null && found.count() > 0) {
			logger.log(WOLogLevel.FINER,"Scheduling course timeouts: " + found.count());
			Enumeration enu = found.objectEnumerator();
			NSMutableArray timeouts = new NSMutableArray();
			ItogContainer itog = null;
			while (enu.hasMoreElements()) {
				CourseTimeout cto = (CourseTimeout) enu.nextElement();
				if(itog != cto.itogContainer() && timeouts.count() > 0) {
					scheduleTimeoutsForItog(timeouts, itog, alreadyScheduled,timer);
					timeouts.removeAllObjects();
				}
				itog = cto.itogContainer();
				timeouts.addObject(cto);
			}
			if(timeouts.count() > 0)
				scheduleTimeoutsForItog(timeouts, itog, alreadyScheduled,timer);
		}
		// Timed out prognoses automation
		qual = new EOKeyValueQualifier("fireDate",
				EOQualifier.QualifierOperatorLessThanOrEqualTo,day);
		NSMutableArray quals = new NSMutableArray(qual);
		qual = new EOKeyValueQualifier(Prognosis.FLAGS_KEY,
				EOQualifier.QualifierOperatorLessThan,new Integer(64));
		quals.addObject(qual);
		if(alreadyScheduled.count() > 0) {
			qual = Various.getEOInQualifier(Prognosis.ITOG_CONTAINER_KEY,
					(NSArray)alreadyScheduled.valueForKey(AutoItog.ITOG_CONTAINER_KEY));
			qual = new EONotQualifier(qual);
			quals.addObject(qual);
		}
		int ign = SettingsReader.intForKeyPath("edu.ignorePrognosesAfterDays", 0);
		if(ign > 0) {
			NSTimestamp since = day.timestampByAddingGregorianUnits(0, 0, - ign, 0, 0, 0);
			qual = new EOKeyValueQualifier("fireDate",
					EOQualifier.QualifierOperatorGreaterThanOrEqualTo,since);
			quals.addObject(qual);
		}
		qual = new EOAndQualifier(quals);
		fs.setEntityName(Prognosis.ENTITY_NAME);
//		fs.setSortOrderings(null);
		fs.setQualifier(qual);
		found = ec.objectsWithFetchSpecification(fs);
		if(found != null && found.count() > 0) {
			Enumeration enu = found.objectEnumerator();
			AutoItog ai = null;
			while (enu.hasMoreElements()) {
				Prognosis pr = (Prognosis) enu.nextElement();
				if(ai == pr.autoItog())
					continue;
				ai = pr.autoItog();
				if(ai == null) {
					try {
						pr.setFireDate(null);
						ec.saveChanges();
					} catch (Exception e) {
						logger.log(WOLogLevel.WARNING, "Failed to disable unowned prognosis",
								new Object[] {pr,e});
					} finally {
						continue;
					}
				}
				if(alreadyScheduled.containsObject(ai))
					continue;
				alreadyScheduled.addObject(ai);
				if(ai.inactive())
					continue;
				NSTimestamp fire = AutoItog.combineDateAndTime(day, ai.fireTime());
				if(fire.getTime() - System.currentTimeMillis() < 10000)
					automateTimedOutPrognoses(ai);
				else
					timer.schedule(new AutoItogAutomator(ai,true), fire);
			}
		}
		/*} catch (Exception e) {
			logger.log(WOLogLevel.WARNING,"Error while converting prognoses to ItogMarks",e);*/
		} finally {
			ec.unlock();
		}
		logger.log(WOLogLevel.FINE,"AutoItog scheduled");
		return null;
	}
	protected static void scheduleTimeoutsForItog(NSArray timeouts,ItogContainer itog,
			NSMutableArray alreadyScheduled, Timer timer) {
		EOEditingContext ec = itog.editingContext();
		NSArray ais = EOUtilities.objectsMatchingKeyAndValue(ec,
				AutoItog.ENTITY_NAME, AutoItog.ITOG_CONTAINER_KEY, itog);
		if(ais == null)
			return;
		NSMutableArray toGids = new NSMutableArray();
		Enumeration enu = timeouts.objectEnumerator();
		NSTimestamp date = null;
		while (enu.hasMoreElements()) {
			CourseTimeout cto = (CourseTimeout) enu.nextElement();
			if(date == null || date.compare(cto.fireDate()) < 0)
				date = cto.fireDate();
			toGids.addObject(ec.globalIDForObject(cto));
		}
		enu = ais.objectEnumerator();
		while (enu.hasMoreElements()) {
			AutoItog autoItog = (AutoItog) enu.nextElement();
			if(autoItog.inactive()) {
				if(!alreadyScheduled.containsObject(autoItog))
					alreadyScheduled.addObject(autoItog);
				continue;
			}
			NSTimestamp fire = AutoItog.combineDateAndTime(date, autoItog.fireDateTime());
			if(fire.getTime() - System.currentTimeMillis() < 10000) {
				Enumeration ctos = timeouts.objectEnumerator();
				while (ctos.hasMoreElements()) {
					CourseTimeout cto = (CourseTimeout) ctos.nextElement();
					automateCourseTimeout(cto, autoItog);
				}
			} else {
				timer.schedule(new CourseTimeoutsAutomator(autoItog,toGids),
						fire);
				if(!alreadyScheduled.containsObject(autoItog))
					alreadyScheduled.addObject(autoItog);
			}
		}
	}
	
	public static NSKeyValueCoding notesAddOns(WOContext ctx) {
		WOSession ses = ctx.session();
		if(Various.boolForObject(ses.valueForKeyPath("readAccess._read.Prognosis")))
			return null;
		String key = "AutoItog.PrognosesAddOn";
		PrognosesAddOn addOn = (PrognosesAddOn)ses.objectForKey(key);
		if(addOn == null) {
			addOn = new PrognosesAddOn(ses);
			ses.setObjectForKey(addOn, key);
		}
		return addOn;
	}

	public static Object objectSaved(WOContext ctx) {
		WOSession ses = ctx.session();
		Object obj = ses.objectForKey("objectSaved");
		EduCourse course;
		NSTimestamp date;
		Student student = null;
		if(obj instanceof EduLesson) {
			course = ((EduLesson)obj).course();
			date = ((EduLesson)obj).date();
		} else if(obj instanceof NSDictionary) {
			NSDictionary dict = (NSDictionary)obj;
			course = (EduCourse)dict.valueForKey("course");
			if(course == null)
				course = (EduCourse)dict.valueForKey("eduCourse");
			if(course == null)
				course = (EduCourse)dict.valueForKeyPath("lesson.course");
			date = (NSTimestamp)dict.valueForKey("date");
			if(date == null)
				date = (NSTimestamp)dict.valueForKeyPath("lesson.date");
			if(date == null)
				date = (NSTimestamp)ses.objectForKey("recentDate");
			student = (Student)dict.valueForKey("student");
		} else {
			return null;
		}
		if(course == null)
			return null;
		if(date == null)
			return null;
		EOEditingContext ec = course.editingContext();
		PrognosesAddOn addOn = (PrognosesAddOn)ses.objectForKey("AutoItog.PrognosesAddOn");
		if(student == null && addOn != null) {
			addOn.setCourse(course, date,date);
		}
		boolean canArchive = SettingsReader.boolForKeyPath("markarchive.Prognosis", 
				SettingsReader.boolForKeyPath("markarchive.archiveAll", false));
		NSArray autoItogs = AutoItog.relatedToObject(obj, course);
		try {
		boolean newObj = (autoItogs == null || autoItogs.count() == 0);
		if(newObj) {
//			NSArray marks = (NSArray)dict.valueForKeyPath("lesson.marks");
//			if(marks == null || marks.count() == 0)
//				return null;
			autoItogs = AutoItog.currentAutoItogsForCourse(course, date);
		} else {
			NSDictionary snapshot = (NSDictionary)ses.objectForKey("committedSnapshot");
			if(snapshot != null && EOPeriod.Utility.compareDates(date, 
					(NSTimestamp)snapshot.valueForKey("date")) != 0) {
				NSArray current = AutoItog.currentAutoItogsForCourse(course, date);
				Enumeration enu = autoItogs.objectEnumerator();
				while (enu.hasMoreElements()) {
					AutoItog ai = (AutoItog) enu.nextElement();
					if(!current.containsObject(ai)) {
						if(ai.removeRelatedObject(obj, course)) {
							ec.saveChanges();
							message(ses, ai, false);
							prognosesChange(course, student, addOn,canArchive, ai);
						}
					}
				}
				autoItogs = current;
				newObj = true;
			}
		}
		Enumeration enu = autoItogs.objectEnumerator();
		while (enu.hasMoreElements()) {
			AutoItog ai = (AutoItog) enu.nextElement();
			if(ai.calculator() == null || ai.namedFlags().flagForKey("inactive"))
				continue;
			Integer relKey = ai.calculator().relKeyForObject(obj);
			if(relKey == null)
				continue;
			if(ai.calculator().skipAutoAdd(relKey, ec)) { 
				if(ai.removeRelatedObject(relKey, course)) {
					ec.saveChanges();
					message(ses, ai, false);
				} else continue;
			} else if(newObj) {
				if(ai.relKeysForCourse(course).count() != 0) {
					if(ai.addRelatedObject(relKey, course)) {
						ec.saveChanges();
						message(ses, ai, true);
					} else continue;
				} else {
					message(ses, ai, true);
				}
			}
			prognosesChange(course, student, addOn,canArchive, ai);
		} // autoItogs.objectEnumerator();
		if(ec.hasChanges()) 
			ec.saveChanges();
		} catch (Exception e) {
			logger.log(WOLogLevel.WARNING,"Could not save prognoses update",e);
			ses.takeValueForKey(e.getMessage(), "message");
		}
		if(addOn != null)
			addOn.setPeriods(autoItogs);
		return null;
	}

	private static void message(WOSession ses, AutoItog ai,boolean added) {
		StringBuilder message = new StringBuilder();
		if(added)
			message.append(ses.valueForKeyPath(
					"strings.RujelAutoItog_AutoItog.ui.addedToPrognose"));
		else
			message.append(ses.valueForKeyPath(
					"strings.RujelAutoItog_AutoItog.ui.removedFromPrognose"));
		message.append(' ').append('"');
		message.append(ai.itogContainer().title()).append('"');
		ses.takeValueForKey(message.toString(), "message");
	}

	private static void prognosesChange(EduCourse course, Student student,
			PrognosesAddOn addOn, boolean canArchive, AutoItog ai) {
		boolean ifArchive = (canArchive && ai.namedFlags().flagForKey("manual"));
		if(student == null) { // whole class
			if(addOn != null) {
				addOn.setPeriodItem(ai);
				addOn.calculate();
			} else {
				PerPersonLink prognoses = ai.calculator().calculatePrognoses(
						course, ai);
				CourseTimeout ct = CourseTimeout.getTimeoutForCourseAndPeriod(
						course, ai.itogContainer());
				Enumeration prenu = prognoses.allValues().objectEnumerator();
				while (prenu.hasMoreElements()) {
					Prognosis progn = (Prognosis) prenu.nextElement();
					progn.updateFireDate(ct);
					if(ifArchive) {
						archivePrognosisChange(progn);
					}
				}
				PrognosesAddOn.feedStats(course, ai.itogContainer(), prognoses.allValues());
			}
		} else { // specific student
			Prognosis progn = ai.calculator().calculateForStudent(student, course, ai,
					ai.relatedForCourse(course));
			if(progn != null) {
				progn.updateFireDate();
				if(ifArchive) {
					archivePrognosisChange(progn);
				}
			}
			PrognosesAddOn.feedStats(course, ai.itogContainer(), null);
			if(addOn != null)
				addOn.setPrognosis(progn);
//					addOn.reset();
		}
	}
	
	public static void archivePrognosisChange(Prognosis prognosis) {
		EOEditingContext ec = prognosis.editingContext();
		NSDictionary snapshot = ec.committedSnapshotForObject(prognosis);
		if(snapshot != null && snapshot
				.valueForKey("mark").equals(prognosis.mark()))
			return;
		EOEnterpriseObject archive = EOUtilities.createAndInsertInstance(
					ec,"MarkArchive");
		archive.takeValueForKey(prognosis, "object");
		String calcName = prognosis.autoItog().calculatorName();
		//calcName = calcName.substring(calcName.lastIndexOf('.') +1);
		archive.takeValueForKey(calcName, "reason");
		archive.takeValueForKey(new Integer(
				(ec.globalIDForObject(prognosis).isTemporary())?1:2), "actionType");
	}
	
	public static NSMutableDictionary printStudentResults(WOContext ctx) {
		NSMutableDictionary dict = (NSMutableDictionary)ctx.session().objectForKey(
				"printStudentResults");
		if(dict == null)
			return null;
		Student student = (Student)dict.objectForKey("student");
		EOEditingContext ec = student.editingContext();
		EOQualifier qual = null;
		EOFetchSpecification fs = null;
		NSArray results = null;
		Object period = dict.removeObjectForKey("period");
		if(!(period instanceof EOPeriod)) {
			NSTimestamp since = (NSTimestamp)dict.removeObjectForKey("since");
			NSTimestamp to = (NSTimestamp)dict.removeObjectForKey("to");
			qual = EOQualifier.qualifierWithQualifierFormat("begin <= %@ AND end >= %@",new NSArray(new Object[]{since,to}));
			fs = new EOFetchSpecification("EduPeriod",qual,EOPeriod.sorter);
			results = ec.objectsWithFetchSpecification(fs);
			if(results == null || results.count() == 0)
				return null;
			period = (EOPeriod)results.objectAtIndex(0);
			//qual = Various.getEOInQualifier("eduPeriod",results);
		} //else {
			qual = new EOKeyValueQualifier("eduPeriod",EOQualifier.QualifierOperatorEqual,period);
		//}
		NSMutableArray quals = new NSMutableArray(qual);
		qual = new EOKeyValueQualifier("student",EOQualifier.QualifierOperatorEqual,student);
		quals.addObject(qual);
		qual = new EOAndQualifier(quals);
		fs = new EOFetchSpecification("Prognosis",qual,null);
		
		results = ec.objectsWithFetchSpecification(fs);
		NSMutableDictionary result =  new NSMutableDictionary((NSArray)results.valueForKey("mark"),(NSArray)results.valueForKey("eduCourse"));
		String title = (String)WOApplication.application().valueForKeyPath("strings.RujelAutoItog_AutoItog.properties.Prognosis.this");
		//title.append(' ').append('(').append(')');
		result.setObjectForKey(title,"title");
		result.setObjectForKey("30","sort");
		return result;
	}
	
	private static void message(CharSequence text) {
		logger.log(WOLogLevel.INFO, text.toString());
	}
	
	protected static class AutoItogAutomator extends TimerTask {
		protected EOGlobalID gid;
		protected boolean skip;
		public AutoItogAutomator(AutoItog autoItog,boolean onlyTimeout) {
			super();
			gid = autoItog.editingContext().globalIDForObject(autoItog);
			skip = onlyTimeout;
		}
		public void run() {
			EOEditingContext ec = new EOEditingContext(
					EOObjectStoreCoordinator.defaultCoordinator());
			ec.lock();
			try {
				AutoItog autoItog = (AutoItog)ec.faultForGlobalID(gid,ec);
				if(autoItog == null || autoItog.inactive()) {
					logger.log(WOLogLevel.INFO,
						"Canceled AutoItog automation as it was found inactive",autoItog);
					return;
				}
				NSTimestamp fire = autoItog.fireDate();
				if(fire.getTime() > System.currentTimeMillis()) {
					logger.log(WOLogLevel.INFO,"Cancelling autoItog execution",
							new Object[] {gid, fire});
					return;
				}
				fire = autoItog.fireDateTime();
				if(fire.getTime() > System.currentTimeMillis()) { // reschedule
					Timer timer = (Timer)WOApplication.application().valueForKey("timer");
					timer.schedule(this, fire);
					logger.log(WOLogLevel.INFO,"Postponing autoItog execution",
							new Object[] {gid, fire});
					return;
				}
				if(!skip)
					automateItog(autoItog);
				automateTimedOutPrognoses(autoItog);
			} catch (Exception e) {
				logger.log(WOLogLevel.WARNING,"Error performing scheduled AutoItog",
						new Object[] {gid,e});
			} finally {
				ec.unlock();
			}
		}
	}
	
	protected static void automateTimedOutPrognoses(AutoItog itog) {
		EOEditingContext ec = itog.editingContext();
		EOQualifier[] quals = new EOQualifier[3];
		quals[0] = new EOKeyValueQualifier(Prognosis.FIRE_DATE_KEY,
				EOQualifier.QualifierOperatorLessThanOrEqualTo,new NSTimestamp());
		quals[1] = new EOKeyValueQualifier(Prognosis.ITOG_CONTAINER_KEY,
				EOQualifier.QualifierOperatorEqual,itog.itogContainer());
		quals[2] = new EOKeyValueQualifier(Prognosis.FLAGS_KEY,
				EOQualifier.QualifierOperatorLessThan,new Integer(64));
		quals[0] = new EOAndQualifier(new NSArray(quals));
		EOFetchSpecification fs = new EOFetchSpecification(Prognosis.ENTITY_NAME,quals[0],null);
		NSArray prognoses = ec.objectsWithFetchSpecification(fs);
		if(prognoses == null || prognoses.count() == 0)
			return;
		logger.log(WOLogLevel.INFO,"Automating timed out prognoses: " + prognoses.count(),
				itog);
		StringBuffer buf = new StringBuffer("Timed out prognoses:\n");
		buf.append(itog.itogContainer().name()).append(' ');
		buf.append(MyUtility.presentEduYear(itog.itogContainer().eduYear()));
		if(prognoses.count() > 1) {
			NSArray sorter = new NSArray(new EOSortOrdering ("course", 
					EOSortOrdering.CompareAscending));
			prognoses = EOSortOrdering.sortedArrayUsingKeyOrderArray(prognoses, sorter);
		}
		EduCourse course = null;
		boolean enableArchive = SettingsReader.boolForKeyPath("markarchive.ItogMark", 
				SettingsReader.boolForKeyPath("markarchive.archiveAll", false));
		boolean overwrite = SettingsReader.boolForKeyPath("edu.overwriteItogsScheduled", false);
		SettingsBase sb = SettingsBase.baseForKey(ItogMark.ENTITY_NAME,ec, false);
		String listName = itog.listName();
		CourseTimeout cto = null;
		NSArray groupList = null;
		int inCourse = 0;
		for (int i = 0; i < prognoses.count(); i++) {
			Prognosis prognos = (Prognosis)prognoses.objectAtIndex(i);
			EduCourse crs = prognos.course();
			if(crs == null || crs.eduYear() == null) {
				ec.deleteObject(prognos);
				ec.saveChanges();
				continue;
			}
			if(crs != course) {
				if(inCourse > 0) {
					savePrognoses(ec, course, itog.itogContainer(), buf);
					buf.append("-- ").append(inCourse).append(" --\n");
				}
				course = crs;
				if(!listName.equals(sb.forCourse(course).textValue())) {
					inCourse = -1;
					continue;
				}
				inCourse = 0;
				buf.append(course.eduGroup().name()).append(" : ");
				buf.append(course.cycle().subject());
				if(course.comment() != null)
					buf.append(':');
				buf.append('\n');
				cto = CourseTimeout.getTimeoutForCourseAndPeriod(course,
						itog.itogContainer());
				groupList = course.groupList();
			} else if(inCourse < 0) {
				continue;
			}
			if(!groupList.containsObject(prognos.student())) {
				if(prognos.complete().compareTo(BigDecimal.ZERO) == 0)
					ec.deleteObject(prognos);
				else
					prognos.setFireDate(null);
				buf.append("Student ").append(Person.Utility.fullName(
						prognos.student(), true, 2, 1, 0)).append(" not in group for course\n");
				continue;
			}
			prognos.setAutoItog(itog);
			prognos.updateFireDate(cto);
			Timeout timeout = null;
			if(EOPeriod.Utility.compareDates(prognos.fireDate(), itog.fireDate()) != 0) {
	    		timeout = Timeout.Utility.chooseTimeout(prognos.getStudentTimeout(),cto);
			}
			ItogMark itogMark = prognos.convertToItogMark(null, overwrite, buf);
			EOEnterpriseObject commentEO = itogMark.commentEO(timeout != null);
			if(commentEO != null)
				Timeout.Utility.setTimeoutComment(commentEO, timeout);
			if(itogMark != null)
				inCourse++;
			if(buf.charAt(buf.length() -1) == 0) {
				buf.deleteCharAt(buf.length() -1);
				continue;
			}
			if(enableArchive && itogMark != null) {
				EOEnterpriseObject archive = EOUtilities.createAndInsertInstance(ec,
						"MarkArchive");
//				logger.log(WOLogLevel.INFO,"AutoItog", new Object[] {
//						itogMark,Thread.currentThread(),new Exception("AutoItog")});
				archive.takeValueForKey(itogMark, "object");
				archive.takeValueForKey("scheduled", "wosid");
				archive.takeValueForKey("AutoItog", "user");
				int actionType = 1;
				if(overwrite && !ec.globalIDForObject(itogMark).isTemporary())
					actionType = 2;
				archive.takeValueForKey(new Integer(actionType), "actionType");
			}
		}
		if(inCourse > 0) {
			savePrognoses(ec, course, itog.itogContainer(), buf);
			buf.append("-- ").append(inCourse).append(" --\n");
		}
		message(buf);
	}
	
	private static void savePrognoses(EOEditingContext ec, EduCourse course, 
			ItogContainer itog, StringBuffer buf) {
		try {
			ec.saveChanges();
		}  catch (Exception ex) {
			logger.log(WOLogLevel.WARNING,"Failed to save timed out prognoses for course", 
					new Object[] {course, ex});
			buf.append("Failed to save timed out prognoses");
			ec.revert();
		}
		ModuleInit.prepareStats(course, itog, ItogMark.MARK_KEY, true);
		ModuleInit.prepareStats(course, itog, "stateKey", true);
	}

	//protected static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
	
	protected static class CourseTimeoutsAutomator extends TimerTask {
		protected EOGlobalID aiGID;
		protected NSArray timeouts;
		public CourseTimeoutsAutomator(AutoItog autoItog,NSArray courseTimeouts) {
			super();
			EOEditingContext ec = autoItog.editingContext();
			aiGID = ec.globalIDForObject(autoItog);
			Object obj = courseTimeouts.objectAtIndex(0);
			if(obj instanceof EOGlobalID) {
				timeouts = courseTimeouts;
			} else {
				NSMutableArray toGigs = new NSMutableArray();
				Enumeration enu = courseTimeouts.objectEnumerator();
				while (enu.hasMoreElements()) {
					CourseTimeout cto = (CourseTimeout) enu.nextElement();
					toGigs.addObject(ec.globalIDForObject(cto));
				}
				timeouts = toGigs;
			}
		}
		public void run() {
			EOEditingContext ec = new EOEditingContext(
					EOObjectStoreCoordinator.defaultCoordinator());
			ec.lock();
			try {
				AutoItog autoItog = (AutoItog)ec.faultForGlobalID(aiGID,ec);
				if(autoItog == null || autoItog.inactive()) {
					logger.log(WOLogLevel.INFO,
						"Canceled CourseTimeout automation as AutoItog found inactive",autoItog);
					return;
				}
				Enumeration enu = timeouts.objectEnumerator();
				while (enu.hasMoreElements()) {
					EOGlobalID gid = (EOGlobalID) enu.nextElement();
					CourseTimeout cto = (CourseTimeout)ec.faultForGlobalID(gid,ec);
					automateCourseTimeout(cto,autoItog);
				}
				automateTimedOutPrognoses(autoItog);
			} catch (Exception e) {
				logger.log(WOLogLevel.WARNING,"Error performing scheduled AutoItog",
						new Object[] {aiGID,timeouts,e});
			} finally {
				ec.unlock();
			}
		}
	}
	
	protected static void automateCourseTimeout(CourseTimeout cto, AutoItog ai) {
		//EOEditingContext ec = cto.editingContext();
		ItogContainer container = cto.itogContainer();
//		NSMutableSet toProcess = new NSMutableSet(cto.relatedCourses());
		StringBuffer buf = new StringBuffer("Automating CourseTimeout: ");
		buf.append(cto.itogContainer().name()).append(':');
		
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");//DateFormat.getDateInstance(DateFormat.MEDIUM);
		dateFormat.format(cto.fireDate(), buf, new FieldPosition(DateFormat.DATE_FIELD));
		buf.append(" (");
		if(cto.eduGroup() != null) {
			buf.append(cto.eduGroup().name()).append(' ');
		}
		if(cto.cycle() != null) {
			buf.append(cto.cycle().subject()).append(' ');
		}
		if(cto.teacher() != null) {
			buf.append(Person.Utility.fullName(cto.teacher(), false, 2, 1, 1)).append(' ');
		}
		if(cto.course() != null) {
			buf.append("for single EduCourse");
		}
		buf.append(")\n");

//		toProcess.intersectSet(new NSSet(courses));
		Enumeration enu = cto.relatedCourses().objectEnumerator();
			//toProcess.objectEnumerator();
		SettingsBase sb = SettingsBase.baseForKey(ItogMark.ENTITY_NAME,
				ai.editingContext(), false);
		String listName = ai.listName();
cycleCourses:
		while (enu.hasMoreElements()) {
			EduCourse cur = (EduCourse) enu.nextElement();
			if(!listName.equals(sb.forCourse(cur).textValue()))
				continue cycleCourses;
			if(cto != CourseTimeout.getTimeoutForCourseAndPeriod(cur, container)) {
				continue cycleCourses;
			}
			cto.namedFlags().setFlagForKey(true, "passed");
			Prognosis.convertPrognoses(cur,container,cto.fireDate(),buf);
		}
		message(buf);
	}
	
	protected static void automateItog(AutoItog autoItog) {
		EOEditingContext ec = autoItog.editingContext();
		ItogContainer container = autoItog.itogContainer();
		StringBuffer buf = new StringBuffer("Automating ItogContainer : ");
		buf.append(container.name()).append('\n');

//		NSArray courseTimeouts = EOUtilities.objectsMatchingKeyAndValue(ec,
//				CourseTimeout.ENTITY_NAME,CourseTimeout.ITOG_CONTAINER_KEY,container);
		
		SettingsBase base = SettingsBase.baseForKey(ItogMark.ENTITY_NAME, ec, false);
		NSArray courses = base.coursesForSetting(autoItog.listName(), 
				null, container.eduYear());
		Enumeration courseEnum = courses.objectEnumerator();
cycleCourses:
		while (courseEnum.hasMoreElements()) {
			EduCourse course = (EduCourse)courseEnum.nextElement();
			CourseTimeout ct = CourseTimeout.getTimeoutForCourseAndPeriod(course, container);
			if(ct != null) {
				buf.append(course.eduGroup().name()).append(" : ");
				buf.append(course.cycle().subject()).append(" - ");
				buf.append(Person.Utility.fullName(course.teacher(), false, 2, 1, 0));
				if(ct.namedFlags().flagForKey("negative")) {
					logger.log(WOLogLevel.INFO,
							"Course should have been already automated",course);
					buf.append(" :: should have been already automated.\n");
					continue cycleCourses;
				}
				logger.log(WOLogLevel.INFO,"Itog timed out for course",course);
				buf.append(" :: timed out.\n");
				// add ItogComment about timeout
				Enumeration enu = course.groupList().objectEnumerator();
				while (enu.hasMoreElements()) {
					Student student = (Student) enu.nextElement();
					EOEnterpriseObject commentEO = ItogMark.getItogComment(course.cycle(),
							container, student, true);
					Timeout timeout = StudentTimeout.timeoutForStudentAndCourse(
							student, course, container);
					timeout = Timeout.Utility.chooseTimeout((StudentTimeout)timeout, ct);
					Timeout.Utility.setTimeoutComment(commentEO, timeout);
				}
				continue cycleCourses;
			}
			//single timouts are checked inside convertPrognosesForCourseAndPeriod()
			Prognosis.convertPrognoses(course,
					autoItog.itogContainer(),autoItog.fireDate(),buf);
		} // cycleCourses
		message(buf);
	}
/*
	public static Object extItog(WOContext ctx) {
		NSKeyValueCoding reporter = (NSKeyValueCoding)ctx.session().objectForKey("itogReporter");
		Student student = (Student)reporter.valueForKey("student");
		Integer eduYear = (Integer)reporter.valueForKey("eduYear");
		EOEditingContext ec = student.editingContext();
		
		NSArray list = (NSArray)reporter.valueForKey("eduPeriods");
		if(list == null || list.count() == 0) {
			list = EOUtilities.objectsMatchingKeyAndValue(ec,"EduPeriod", "eduYear",eduYear);
			reporter.takeValueForKey(list,"eduPeriods");
		} else {
			list = EOUtilities.localInstancesOfObjects(ec,list);
		}
		EOQualifier perQual = Various.getEOInQualifier("eduPeriod", list);
		if(perQual == null)
			return null;
		NSMutableArray quals = new NSMutableArray(perQual);
		EOQualifier qual = new EOKeyValueQualifier("student",EOQualifier.QualifierOperatorEqual,student);
		quals.addObject(qual);
		qual = new EOAndQualifier(quals);

		NSMutableArray result = new NSMutableArray();
		
		// StudentTimeout
		
		//list = new NSArray(new EOSortOrdering("eduPeriod",EOSortOrdering.CompareAscending));
		EOFetchSpecification fs = new EOFetchSpecification(
				StudentTimeout.ENTITY_NAME,qual,null);
		list = ec.objectsWithFetchSpecification(fs);
		if(list != null && list.count() > 0) {
			result.addObjectsFromArray((NSArray)list.valueForKey("extItog"));
		}
		
		// Bonus
		qual = new EOKeyValueQualifier(
				"value",EOQualifier.QualifierOperatorGreaterThan,BigDecimal.ZERO);
		quals.addObject(qual);
		qual = new EOAndQualifier(quals);
		fs.setEntityName(Bonus.ENTITY_NAME);
		fs.setQualifier(qual);
		list = ec.objectsWithFetchSpecification(fs);
		if(list != null && list.count() > 0) {
			result.addObjectsFromArray((NSArray)list.valueForKey("extItog"));
		}
		
		
		// CourseTimeout
		NSArray courses = (NSArray)reporter.valueForKey("courses");
		if(courses == null || courses.count() == 0)
			return result;
		
		list = (NSArray)reporter.valueForKey("courseTimeouts");
		if(list == null) {
			fs.setEntityName(CourseTimeout.ENTITY_NAME);
			fs.setQualifier(perQual);
			list = ec.objectsWithFetchSpecification(fs);
			reporter.takeValueForKey(list,"courseTimeouts");
		} else {
			list = EOUtilities.localInstancesOfObjects(ec,list);
		}
		if(list.count() > 1) {
			NSArray tmp = new NSArray(new EOSortOrdering("eduPeriod",EOSortOrdering.CompareAscending));
			list = EOSortOrdering.sortedArrayUsingKeyOrderArray(list, tmp);
		}

		ItogContainer container = null;
		Enumeration enu = list.objectEnumerator();
		NSMutableDictionary byCycle = new NSMutableDictionary();
		while (enu.hasMoreElements()) {
			CourseTimeout cto = (CourseTimeout) enu.nextElement();
			if(container != cto.itogContainer()) {
				if(container != null)
					append(byCycle, result);
				container = cto.itogContainer();
			}
			EduCourse course = cto.course();
			if(course != null) {
				if(courses.contains(course)) {
					register(byCycle, cto, course.cycle());
				}
				continue;
			}
			NSArray related = EOQualifier.filteredArrayWithQualifier
				(courses, cto.courseQualifier());
			if(related == null || related.count() == 0)
				continue;
			if(related.count() == courses.count()) {
				register(byCycle, cto, "none");
				continue;
			}
			Enumeration rEnu = related.objectEnumerator();
			while (rEnu.hasMoreElements()) {
				course = (EduCourse) rEnu.nextElement();
				register(byCycle, cto, course.cycle());
			}
		}
		append(byCycle, result);

		return result;
	}
	
	private static void append(NSMutableDictionary byCycle, NSMutableArray result) {
		NSArray list = (NSArray)byCycle.removeObjectForKey("none");
		CourseTimeout grand = null;
		if(list != null) {
			grand = CourseTimeout.chooseOne(list);
			result.addObject(grand.extItog(null));
		}
		Enumeration enu = byCycle.keyEnumerator();
		while (enu.hasMoreElements()) {
			Object key = enu.nextElement();
			NSMutableArray tmp = (NSMutableArray)byCycle.objectForKey(key);
			if(grand != null)
				tmp.addObject(grand);
			CourseTimeout cto = CourseTimeout.chooseOne(tmp);
			if(cto == grand)
				continue;
			NSMutableDictionary dict = cto.extItog((EduCycle)key);
			result.addObject(dict);
		}
		byCycle.removeAllObjects();
	} 
	
	private static void register (NSMutableDictionary byCycle, Object value, Object key) {
		NSMutableArray tmp = (NSMutableArray) byCycle.objectForKey(key);
		if(tmp == null) {
			tmp = new NSMutableArray(value);
			byCycle.setObjectForKey(tmp, key);
		} else {
			tmp.addObject(value);
		}
	} */
	
	protected static Enumeration aiEnu(EOEditingContext ec, String listName, NSTimestamp date) {
    	EOQualifier[] quals = new EOQualifier[3];
    	quals[0] = new EOKeyValueQualifier(AutoItog.LIST_NAME_KEY,
    			EOQualifier.QualifierOperatorEqual,listName);
    	quals[1] = new EOKeyValueQualifier(AutoItog.FIRE_DATE_KEY,
    			EOQualifier.QualifierOperatorGreaterThanOrEqualTo, date);
    	quals[2] = new EOKeyValueQualifier(AutoItog.FLAGS_KEY,
    			EOQualifier.QualifierOperatorLessThan, new Integer(32));
    	quals[0] = new EOAndQualifier(new NSArray(quals));
    	EOFetchSpecification fs = new EOFetchSpecification(
    			AutoItog.ENTITY_NAME,quals[0],null);
    	NSArray list = ec.objectsWithFetchSpecification(fs);
		if(list == null || list.count() == 0)
			return null;
		list = EOSortOrdering.sortedArrayUsingKeyOrderArray(list,
				AutoItog.typeSorter);
		final Enumeration enu = list.objectEnumerator();
		return new Enumeration() {
			private AutoItog next = null;
			private NSMutableSet types = new NSMutableSet();

			public boolean hasMoreElements() {
				if(next != null)
					return true;
				while (enu.hasMoreElements()) {
					next = (AutoItog) enu.nextElement();
					ItogType type =next.itogContainer().itogType(); 
					if(!types.containsObject(type)) {
						types.addObject(type);
						return true;
					}
				}
				next = null;
				return false;
			}

			public Object nextElement() {
				if(hasMoreElements()) {
					Object result = next;
					next = null;
					return result;
				}
				throw new NoSuchElementException("No more elements");
			}
		};
	}
	
	public static Object statCourseReport(WOContext ctx) {
		EOEditingContext ec = null;
		NSTimestamp date = (NSTimestamp)ctx.session().valueForKey("today");
		if(date == null)
			return null;
		try {
			ec = (EOEditingContext)ctx.page().valueForKey("ec");
		} catch (Exception e) {
			ec = new SessionedEditingContext(ctx.session());
		}
		String listName;
		Enumeration enu = null;
		NSKeyValueCodingAdditions course = (NSKeyValueCodingAdditions)
				ctx.session().objectForKey("statCourseReport");
		if(!(course instanceof EduCourse)) {
			try {
				course = (EduCourse)ctx.page().valueForKey("course");
			} catch (Exception e) {
				
			}
		}
		if(course == null) {
			listName = ModuleInit.sectionListName(ctx.session(), ec);			
		} else {
			listName = SettingsBase.stringSettingForCourse(ItogMark.ENTITY_NAME, course, ec);
		}
		if(course instanceof EduCourse) {
			NSArray list = AutoItog.currentAutoItogsForCourse((EduCourse)course, date);
			if(list != null && list.count() > 0)
				enu = list.objectEnumerator();
		} else {
			enu = aiEnu(ec, listName, date);
		}
		if(enu == null)
			return null;
		/*
		try {
			EduCourse course = (EduCourse)ctx.page().valueForKey("course");
			listName = SettingsBase.stringSettingForCourse(ItogMark.ENTITY_NAME, course, ec);
			NSArray list = AutoItog.currentAutoItogsForCourse(course, date);
			if(list == null || list.count() == 0)
				return null;
			enu = list.objectEnumerator();
		} catch (Exception e) {
			listName = ModuleInit.sectionListName(ctx.session(), ec);
			enu = aiEnu(ec, listName, date);
			if(enu == null)
				return null;
		}*/
		NSMutableDictionary template = new NSMutableDictionary(Prognosis.ENTITY_NAME,"entName");
		template.setObjectForKey(Prognosis.MARK_KEY, "statField");
		template.takeValueForKey(new NSDictionary(listName,ItogMark.ENTITY_NAME),
				SettingsBase.ENTITY_NAME);
		try {
			Method method = PrognosesAddOn.class.getMethod("statCourse",
					EduCourse.class, ItogContainer.class, String.class);
			template.setObjectForKey(method,"ifEmpty");
		} catch (Exception e) {
			logger.log(WOLogLevel.WARNING,"Failed to get stats method",e);
		}
		template.setObjectForKey(ctx.session().valueForKeyPath(
				"strings.RujelAutoItog_AutoItog.prognoses"),"description");
		int sort = 50;
		NSMutableArray result = new NSMutableArray();
		boolean addCalculations = SettingsReader.boolForKeyPath(
				"edu.prognonesStatCalculations", false);
		Integer curGrNum = null;
		NSArray presets = null;
		while (enu.hasMoreElements()) {
			AutoItog perType = (AutoItog) enu.nextElement();
			ItogContainer period = perType.itogContainer();
			if(period == null)
				continue;
			Integer grNum = ItogPreset.getPresetGroup(listName,period.eduYear(),period.itogType());
			if(grNum != null && !grNum.equals(curGrNum)) {
				presets = ItogPreset.listPresetGroup(ec, grNum, true);
				if(presets != null)
					presets = (NSArray)presets.valueForKey(ItogPreset.MARK_KEY);
				if(presets.count() == 0)
					presets = null;
			}
			NSMutableDictionary dict = template.mutableClone();
			String name = period.title();
			dict.setObjectForKey(period,"param2");
			if(presets != null) {
				dict.setObjectForKey(String.valueOf(sort + 10),"sort");
				dict.setObjectForKey(name,"title");
				dict.setObjectForKey(Boolean.valueOf(addCalculations
						&& "5".equals(presets.objectAtIndex(0))),"addCalculations");
				dict.takeValueForKey(presets, "keys");				
				result.addObject(dict);
				dict = dict.mutableClone();
			}
			dict.setObjectForKey("stateKey","statField");
			dict.setObjectForKey(ItogPreset.stateSymbolsDescending, "keys");
			dict.setObjectForKey(Boolean.FALSE, "addCalculations");
			dict.setObjectForKey(String.valueOf(sort),"sort");
			dict.setObjectForKey("~ " + name + " ~","title");
			result.addObject(dict);			
			sort++;
		}
		return result;
	}
	
	public static Object deleteItogContainer(WOContext ctx) {
		ItogContainer itog = (ItogContainer)ctx.session().objectForKey("deleteItogContainer");
		deleteWithContainer(itog, Prognosis.ENTITY_NAME);
		deleteWithContainer(itog, StudentTimeout.ENTITY_NAME);
		deleteWithContainer(itog, CourseTimeout.ENTITY_NAME);
		deleteWithContainer(itog, "ItogRelated");
		deleteWithContainer(itog, AutoItog.ENTITY_NAME);
		return null;
	}
	
	private static void deleteWithContainer (ItogContainer itog, String entity) {
		EOEditingContext ec = itog.editingContext();
		NSArray found = EOUtilities.objectsMatchingKeyAndValue(ec, entity, "itogContainer", itog);
		if(found == null || found.count() == 0)
			return;
		Enumeration enu = found.objectEnumerator();
		while (enu.hasMoreElements()) {
			EOEnterpriseObject obj = (EOEnterpriseObject) enu.nextElement();
			ec.deleteObject(obj);
		}
	}

	public static Object xmlGeneration(WOContext ctx) {
		NSDictionary options = (NSDictionary)ctx.session().objectForKey("xmlGeneration");
		return new PrognosesXML(options);
	}
	
	public static Object groupMarksReport(WOContext ctx) {
		if(Various.boolForObject(ctx.session().valueForKeyPath("readAccess._read.ItogMark")))
			return null;
		EOEditingContext ec = null;
		try {
			ec = (EOEditingContext)ctx.page().valueForKey("ec");
		} catch (Exception e) {
			ec = new SessionedEditingContext(ctx.session());
		}
//		ec.lock();
		String listName;
		NSKeyValueCodingAdditions course = (NSKeyValueCodingAdditions)
				ctx.session().objectForKey("groupReport");
		if(!(course instanceof EduCourse)) {
			try {
				course = (EduCourse)ctx.page().valueForKey("course");
			} catch (Exception e) {
				
			}
		}
		if(course == null) {
			listName = ModuleInit.sectionListName(ctx.session(), ec);			
		} else {
			listName = SettingsBase.stringSettingForCourse(ItogMark.ENTITY_NAME, course, ec);
		}
		NSTimestamp date = (NSTimestamp)ctx.session().valueForKey("today");
		Enumeration aiEnu = aiEnu(ec, listName, date);
		if(aiEnu == null)
			return null;
		NSMutableArray result = new NSMutableArray();
		NSArray subParams = (NSArray)ctx.session().valueForKeyPath(
				"strings.RujelEduResults_EduResults.groupReportSubs");
		String name = (String)ctx.session().valueForKeyPath(
				"strings.RujelAutoItog_AutoItog.prognoses");
		NSDictionary setting = new NSDictionary(listName,ItogMark.ENTITY_NAME);
		while (aiEnu.hasMoreElements()) {
			AutoItog ai = (AutoItog) aiEnu.nextElement();
			ItogContainer itog = ai.itogContainer();
			NSMutableDictionary dict = new NSMutableDictionary(ai,"eo");
			dict.setObjectForKey(name + " - " + itog.title(),"title");
			dict.setObjectForKey("AutoItog" + MyUtility.getID(ai), "id");
			int sort = 50 + itog.itogType().sort()*10 + itog.num();
			dict.setObjectForKey(String.valueOf(sort), "sort");
			dict.setObjectForKey(PlistReader.cloneArray(subParams, true), "subParams");
			dict.takeValueForKey(setting,SettingsBase.ENTITY_NAME);
			try {
				NSMutableDictionary preload = new NSMutableDictionary("preloadMarks","methodName");
				Method method = AutoItogModule.class.getMethod("preloadMarks", 
						EduGroup.class,ItogContainer.class,NSMutableDictionary.class);
				preload.setObjectForKey(method, "parsedMethod");
				preload.takeValueForKey(new NSArray(new Object[] {".",itog,"$itemDict"}),"paramValues");
				preload.takeValueForKey(Boolean.FALSE, "cacheResult");
				dict.takeValueForKey(preload, "preload");
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			result.addObject(dict);
		}
		return result;
	}

	public static NSMutableDictionary preloadMarks(EduGroup group, ItogContainer itog, 
			NSMutableDictionary itemDict) {
		if(group == null)
			return null;
		EOQualifier[] quals = new EOQualifier[2];
		quals[0] = Various.getEOInQualifier("student", group.list());
		quals[1] = new EOKeyValueQualifier(Prognosis.ITOG_CONTAINER_KEY, 
				EOQualifier.QualifierOperatorEqual, itog);
		quals[1] = new EOAndQualifier(new NSArray(quals));
		EOFetchSpecification fs = new EOFetchSpecification(Prognosis.ENTITY_NAME,quals[1],
				new NSArray(new EOSortOrdering("studentID", EOSortOrdering.CompareAscending)));
		EOEditingContext ec = group.editingContext();
		NSArray found = ec.objectsWithFetchSpecification(fs);
		return ModuleInit.preloadMarks(found, group, itog, itemDict);
	}

}
