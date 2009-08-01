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

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.FieldPosition;
import java.text.SimpleDateFormat;
import java.util.Enumeration;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Logger;
import net.rujel.reusables.WOLogLevel;
import java.util.GregorianCalendar;

public class AutoItogModule {
	protected static Logger logger = Logger.getLogger("rujel.autoitog");
	
	public static Object init(Object obj, WOContext ctx) {
		if(obj == null || obj.equals("init")) {
			AutoItog.init();
			Prognosis.init();
			StudentTimeout.init();
			CourseTimeout.init();
		} else if("scheduleTask".equals(obj)) {
			return scheduleTask(ctx);
		} else if("notesAddOns".equals(obj)) {
			return notesAddOns(ctx);
		} else if("objectSaved".equals(obj)) {
			return objectSaved(ctx);
		} else if("printStudentResults".equals(obj)) {
			return printStudentResults(ctx);
		} else if("reportForStudent".equals(obj)) {
			NSDictionary settings = (NSDictionary)ctx.session().
						objectForKey("reportForStudent");
			return PrognosReport.reportForStudent(settings);
		} else if("reportSettingsForStudent".equals(obj)) {
			NSDictionary reportSettings = (NSDictionary)WOApplication.application().
						valueForKeyPath("strings.RujelAutoItog_AutoItog.reportSettings");
			return PlistReader.cloneDictionary(reportSettings, true);
		} else if("extItog".equals(obj)) {
			return extItog(ctx);
		} else if("statCourseReport".equals(obj)) {
//			return statCourseReport(ctx);
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
					NSTimestamp fire =  ai.fireDateTime();
					if(fire.getTime() - System.currentTimeMillis() < 10000) {
						automateItog(ai);
					} else {
						timer.schedule(new AutoItogAutomator(ai,false),fire);
					}
					alreadyScheduled.addObject(ai);
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
				if(itog != cto.itogContainer()) {
					if(timeouts.count() > 0)
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
		qual = Various.getEOInQualifier(Prognosis.AUTO_ITOG_KEY, alreadyScheduled);
		qual = new EONotQualifier(qual);
		NSMutableArray quals = new NSMutableArray(qual);
		qual = new EOKeyValueQualifier("fireDate",
				EOQualifier.QualifierOperatorLessThanOrEqualTo,day);
		quals.addObject(qual);
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
				if(alreadyScheduled.containsObject(ai))
					continue;
				alreadyScheduled.addObject(ai);
				NSTimestamp fire = ai.fireDateTime();
				if(fire.getTime() - System.currentTimeMillis() < 10000)
					automateTimedOutPrognoses(ai);
				else
					timer.schedule(new AutoItogAutomator(ai,true), ai.fireDateTime());
			}
		}
		/*} catch (Exception e) {
			logger.log(WOLogLevel.WARNING,"Error while converting prognoses to ItogMarks",e);*/
		} finally {
			ec.unlock();
		}
		logger.log(WOLogLevel.FINE,"AutoItog completed");		return null;
	}
	protected static void scheduleTimeoutsForItog(NSArray timeouts,ItogContainer itog,
			NSMutableArray alreadyScheduled, Timer timer) {
		EOEditingContext ec = itog.editingContext();
		NSArray ais = EOUtilities.objectsMatchingKeyAndValue(ec,
				AutoItog.ENTITY_NAME, AutoItog.ITOG_CONTAINER_KEY, itog);
		if(ais == null)
			return;
		NSMutableArray toGigs = new NSMutableArray();
		Enumeration enu = timeouts.objectEnumerator();
		while (enu.hasMoreElements()) {
			CourseTimeout cto = (CourseTimeout) enu.nextElement();
			toGigs.addObject(ec.globalIDForObject(cto));
		}
		enu = ais.objectEnumerator();
		while (enu.hasMoreElements()) {
			AutoItog autoItog = (AutoItog) enu.nextElement();
			NSTimestamp fire = autoItog.fireDateTime();
			if(fire.getTime() - System.currentTimeMillis() < 10000) {
				Enumeration ctos = timeouts.objectEnumerator();
				while (ctos.hasMoreElements()) {
					CourseTimeout cto = (CourseTimeout) ctos.nextElement();
					automateCourseTimeout(cto, autoItog);
				}
			} else {
				timer.schedule(new CourseTimeoutsAutomator(autoItog,toGigs),
						autoItog.fireDateTime());
			}
			
		}
	}
	
	public static NSKeyValueCoding notesAddOns(WOContext ctx) {
		WOSession ses = ctx.session();
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
		NSDictionary dict = (NSDictionary)ses.objectForKey("objectSaved");
		EduCourse course = (EduCourse)dict.valueForKey("course");
		if(course == null)
			course = (EduCourse)dict.valueForKey("eduCourse");
		if(course == null)
			course = (EduCourse)dict.valueForKeyPath("lesson.course");
		if(course == null)
			return null;
		NSTimestamp date = (NSTimestamp)dict.valueForKey("date");
		if(date == null)
			date = (NSTimestamp)dict.valueForKeyPath("lesson.date");
		if(date == null)
			date = (NSTimestamp)ses.objectForKey("recentDate");
		if(date == null)
			return null;
		Student student = (Student)dict.valueForKey("student");
		PrognosesAddOn addOn = (PrognosesAddOn)ses.objectForKey("AutoItog.PrognosesAddOn");;
		if(student == null && addOn != null) {
			addOn.setCourse(course, date);
		}
		boolean canArchive = SettingsReader.boolForKeyPath("markarchive.Prognosis", false);
		NSArray pertypes = PeriodType.periodTypesForCourse(course);
		Enumeration enu = pertypes.objectEnumerator();
		while (enu.hasMoreElements()) {
			PeriodType type = (PeriodType) enu.nextElement();
			PrognosUsage usage = PrognosUsage.prognosUsage(course, type);
			if(usage == null || usage.calculator() == null || !usage.namedFlags().flagForKey("active"))
				continue;
			if(!usage.calculator().reliesOn().contains(dict.valueForKey("entityName")))
				continue;
			EduPeriod eduPeriod = type.currentPeriod(date);
			if(eduPeriod == null)
				continue;
			boolean ifArchive = (canArchive && usage.namedFlags().flagForKey("manual"));
			if(student == null) {
				if(addOn != null) {
					addOn.setPeriodItem(eduPeriod);
					addOn.calculate();
				} else {
					PerPersonLink prognoses = usage.calculator().calculatePrognoses(course, eduPeriod);
					CourseTimeout ct = CourseTimeout.getTimeoutForCourseAndPeriod(course, eduPeriod);
					Enumeration prenu = prognoses.allValues().objectEnumerator();
					while (prenu.hasMoreElements()) {
						Prognosis progn = (Prognosis) prenu.nextElement();
						progn.updateFireDate(ct);
						if(ifArchive) {
							archivePrognosisChange(progn, usage);
						}
					}
					EOEnterpriseObject grouping = PrognosesAddOn.getStatsGrouping(course, eduPeriod);
					if(grouping != null) {
//						NSDictionary stats = PrognosesAddOn.statCourse(course, prognoses.allValues());
//						grouping.takeValueForKey(stats, "dict");
						NSArray list = MyUtility.filterByGroup(prognoses.allValues(),
								"student", course.groupList(), true);
						grouping.takeValueForKey(list, "array");
					}
				}
			} else {
				Prognosis progn = usage.calculator().calculateForStudent(student, course, eduPeriod);
				if(progn != null) {
					progn.updateFireDate();
					if(ifArchive) {
						archivePrognosisChange(progn, usage);
					}
				}
				EOEnterpriseObject grouping = PrognosesAddOn.getStatsGrouping(course, eduPeriod);
				if(grouping != null) {
					NSArray prognoses = Prognosis.prognosesArrayForCourseAndPeriod(
							course, eduPeriod);
					prognoses = MyUtility.filterByGroup(prognoses, "student", 
							course.groupList(), true);
					grouping.takeValueForKey(prognoses, "array");
//					NSDictionary stats = PrognosesAddOn.statCourse(course, eduPeriod);
//					grouping.takeValueForKey(stats, "dict");
				}
				if(addOn != null)
					addOn.reset();
			}
		} // pertypes.objectEnumerator();
		if(course.editingContext().hasChanges()) {
			try {
				course.editingContext().saveChanges();
			} catch (Exception e) {
				logger.log(WOLogLevel.WARNING,"Could not save prognoses update",e);
				ses.takeValueForKey(e.getMessage(), "message");
			}
		}
		return null;
	}
	
	public static void archivePrognosisChange(Prognosis prognosis,PrognosUsage usage) {
		EOEditingContext ec = prognosis.editingContext();
		NSDictionary snapshot = ec.committedSnapshotForObject(prognosis);
		if(snapshot != null && snapshot
				.valueForKey("mark").equals(prognosis.mark()))
			return;
		EOEnterpriseObject archive = EOUtilities.createAndInsertInstance(
					ec,"MarkArchive");
		archive.takeValueForKey(prognosis, "object");
		String calcName = usage.calculatorName();
		//calcName = calcName.substring(calcName.lastIndexOf('.') +1);
		archive.takeValueForKey(calcName, "reason");
	}
	
	public static NSMutableDictionary printStudentResults(WOContext ctx) {
		NSMutableDictionary dict = (NSMutableDictionary)ctx.session().objectForKey("printStudentResults");
		if(dict == null)
			return null;
		Student student = (Student)dict.objectForKey("student");
		EOEditingContext ec = student.editingContext();
		//NSArray periods = EOUtilities.objectsWithQualifierFormat(student.editingContext(),"EduPeriod","begin <= %@ AND end >= %@",new NSArray(new Object[]{since,to}));
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
		boolean skip;
		public AutoItogAutomator(AutoItog autoItog,boolean onlyTimeout) {
			super();
			gid = autoItog.editingContext().globalIDForObject(autoItog);
			skip = onlyTimeout;
		}
		public void run() {
			EOEditingContext ec = new EOEditingContext();
			ec.lock();
			try {
				AutoItog autoItog = (AutoItog)ec.objectForGlobalID(gid);
				if(!skip)
					automateItog(autoItog);
				automateTimedOutPrognoses(autoItog);
			} finally {
				ec.unlock();
			}
		}
	}
	
	protected static void automateTimedOutPrognoses(AutoItog autoItog) {
		EOEditingContext ec = autoItog.editingContext();
		EOQualifier[] quals = new EOQualifier[2];
		quals[0] = new EOKeyValueQualifier(Prognosis.FIRE_DATE_KEY,
				EOQualifier.QualifierOperatorLessThanOrEqualTo,new NSTimestamp());
		quals[1] = new EOKeyValueQualifier(Prognosis.AUTO_ITOG_KEY,
				EOQualifier.QualifierOperatorEqual,autoItog);
		quals[0] = new EOAndQualifier(new NSArray(quals));
		EOFetchSpecification fs = new EOFetchSpecification(Prognosis.ENTITY_NAME,quals[0],null);
		NSArray prognoses = ec.objectsWithFetchSpecification(fs);
		
		logger.log(WOLogLevel.INFO,"Automating timed out prognoses: " + prognoses.count(),
				autoItog);
		StringBuffer buf = new StringBuffer("Timed out prognoses:\n");
		buf.append(autoItog.itogContainer().name()).append(' ');
		buf.append(MyUtility.presentEduYear(autoItog.itogContainer().eduYear()));
		if(prognoses.count() > 1) {
			NSArray sorter = new NSArray(new EOSortOrdering ("course", 
					EOSortOrdering.CompareAscending));
			prognoses = EOSortOrdering.sortedArrayUsingKeyOrderArray(prognoses, sorter);
		}
		EduCourse course = null;
		boolean enableArchive = SettingsReader.boolForKeyPath("markarchive.ItogMark", false);
		boolean overwrite = SettingsReader.boolForKeyPath("edu.overwriteItogsScheduled", false);
		int inCourse = 0;
		for (int i = 0; i < prognoses.count(); i++) {
			Prognosis prognos = (Prognosis)prognoses.objectAtIndex(i);
			if(prognos.course() != course) {
				if(course != null) {
					savePrognoses(ec, course, autoItog, buf);
					buf.append("-- ").append(inCourse).append(" --\n");
				}
				course = prognos.course();
				inCourse = 0;
//				if((i - last) > 12) {
//				if(course.eduGroup() != group) {
//					group = course.eduGroup();
					buf.append(course.eduGroup().name()).append(" : ");
//				}
//				if(course.cycle() != cycle) {
//					cycle = course.cycle();
					buf.append(course.cycle().subject()).append(':');
//				}
				buf.append('\n');
			}
			ItogMark itog = prognos.convertToItogMark(null, overwrite, buf);
			if(itog != null)
				inCourse++;
			if(enableArchive && itog != null) {
				EOEnterpriseObject archive = EOUtilities.createAndInsertInstance(ec,"MarkArchive");
				logger.log(WOLogLevel.INFO,"AutoItog", new Object[] {
						itog,Thread.currentThread(),new Exception("AutoItog")});
				archive.takeValueForKey(itog, "object");
				archive.takeValueForKey("scheduled", "wosid");
				archive.takeValueForKey("AutoItog", "user");
			}
		}
		if(course != null)
			savePrognoses(ec, course, autoItog, buf);
		message(buf);
	}
	
	private static void savePrognoses(EOEditingContext ec, EduCourse course, 
			AutoItog autoItog, StringBuffer buf) {
		try {
			ec.saveChanges();
		}  catch (Exception ex) {
			logger.log(WOLogLevel.WARNING,"Failed to save timed out prognoses for course", 
					new Object[] {course, ex});
			buf.append("Failed to save timed out prognoses");
			ec.revert();
		}
		ModuleInit.prepareStats(course, autoItog.itogContainer(), true);
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
			EOEditingContext ec = new EOEditingContext();
			ec.lock();
			try {
				AutoItog autoItog = (AutoItog)ec.objectForGlobalID(aiGID);
				Enumeration enu = timeouts.objectEnumerator();
				while (enu.hasMoreElements()) {
					EOGlobalID gid = (EOGlobalID) enu.nextElement();
					CourseTimeout cto = (CourseTimeout)ec.objectForGlobalID(gid);
					automateCourseTimeout(cto,autoItog);
				}
				automateTimedOutPrognoses(autoItog);
			} finally {
				ec.unlock();
			}
		}
	}
	
	protected static void automateCourseTimeout(CourseTimeout cto,AutoItog autoItog) {
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
			buf.append(Person.Utility.fullName(cto.teacher().person(), false, 2, 1, 1)).append(' ');
		}
		if(cto.course() != null) {
			buf.append("for single EduCourse");
		}
		buf.append(")\n");

//		toProcess.intersectSet(new NSSet(courses));
		Enumeration enu = cto.relatedCourses().objectEnumerator();
			//toProcess.objectEnumerator();
cycleCourses:
		while (enu.hasMoreElements()) {
			EduCourse cur = (EduCourse) enu.nextElement();
			if(cto != CourseTimeout.getTimeoutForCourseAndPeriod(cur, container)) {
				continue cycleCourses;
			}
			cto.namedFlags().setFlagForKey(true, "passed");
			Prognosis.convertPrognoses(cur,autoItog,cto.fireDate(),buf);
		}
		message(buf);
	}
	
	protected static void automateItog(AutoItog autoItog) {
		EOEditingContext ec = autoItog.editingContext();
		ItogContainer container = autoItog.itogContainer();
		StringBuffer buf = new StringBuffer("Automating end of EduPeriod");
		buf.append(container.name()).append('\n');

		NSArray courseTimeouts = EOUtilities.objectsMatchingKeyAndValue(ec,
				CourseTimeout.ENTITY_NAME,CourseTimeout.ITOG_CONTAINER_KEY,container);
		
		SettingsBase base = SettingsBase.baseForKey(ItogMark.MARK_KEY, ec, false);
		NSArray courses = base.coursesForSetting(autoItog.listName(), 
				null, container.eduYear());
		Enumeration courseEnum = courses.objectEnumerator();
		FieldPosition pos = new FieldPosition(DateFormat.DATE_FIELD);
		String timeoutTitle = (String)WOApplication.application()
				.valueForKeyPath("strings.RujelAutoItog_AutoItog.ui.generalTimeout");
		String upTo = (String)WOApplication.application()
				.valueForKeyPath("strings.RujelAutoItog_AutoItog.ui.upTo");
cycleCourses:
		while (courseEnum.hasMoreElements()) {
			EduCourse course = (EduCourse)courseEnum.nextElement();
			CourseTimeout ct = CourseTimeout.getTimeoutForCourseAndPeriod(course, container);
			if(ct != null) {
				if(ct.namedFlags().flagForKey("negative")) {
					logger.log(WOLogLevel.INFO,
							"Course should have been already automated",course);
					continue cycleCourses;
				}
				logger.log(WOLogLevel.INFO,"Itog timed out for course",course);
				// add ItogComment about timeout
				Enumeration enu = course.groupList().objectEnumerator();
				while (enu.hasMoreElements()) {
					Student student = (Student) enu.nextElement();
					EOEnterpriseObject commentEO = ItogMark.getItogComment(course.cycle(),
							container, student, true);
					Timeout timeout = StudentTimeout.timeoutForStudentAndCourse(
							student, course, container);
					timeout = Timeout.Utility.chooseTimeout((StudentTimeout)timeout, ct);
					StringBuffer comment = new StringBuffer();
					comment.append(upTo).append(' ');
					MyUtility.dateFormat().format(timeout.fireDate(), comment, pos);
					buf.append(" : <em>").append(timeout.reason()).append("</em>");
					ItogMark.setCommentForKey(commentEO, comment.toString(), timeoutTitle);
				}
				continue cycleCourses;
			}
			//single timouts are checked inside convertPrognosesForCourseAndPeriod()
			Prognosis.convertPrognoses(course,autoItog,autoItog.fireDate(),buf);
		}
		message(buf);
	}

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
	}
	
	// TODO : stats support for prognoses
	
/*	public static Object statCourseReport(WOContext ctx) {
		EOEditingContext ec = null;
		try {
			ec = (EOEditingContext)ctx.page().valueForKey("ec");
		} catch (Exception e) {
			ec = new SessionedEditingContext(ctx.session());
		}
		NSArray list = new NSArray(ctx.session().valueForKey("eduYear"));
		list = EOUtilities.objectsWithQualifierFormat(ec, PrognosUsage.ENTITY_NAME, 
				"(eduYear = %d OR eduYear = 0)", list);
			//PeriodType.allPeriodTypes(ec, (Integer)ctx.session().valueForKey("eduYear"));
		if(list == null || list.count() == 0)
			return null;
		Enumeration enu = list.objectEnumerator();
		NSMutableArray result = new NSMutableArray();
		while (enu.hasMoreElements()) {
			PrognosUsage pu = (PrognosUsage) enu.nextElement();
			if(!pu.namedFlags().flagForKey("active"))
				continue;
			if(!result.containsObject(pu.periodType()))
				result.addObject(pu.periodType());
		}
		if(result.count() == 0)
			return null;
		list = EOSortOrdering.sortedArrayUsingKeyOrderArray(result, PeriodType.sorter);
		result.removeAllObjects();
		enu = list.objectEnumerator();
//		Object[] params = new Object[] 
//		              {Prognosis.ENTITY_NAME, Prognosis.MARK_KEY,".",EduPeriod.ENTITY_NAME};
		NSMutableDictionary template = new NSMutableDictionary(Prognosis.ENTITY_NAME,"entName");
		template.setObjectForKey(Prognosis.MARK_KEY, "statField");
		template.setObjectForKey(ModuleInit.marksPreset(),"keys");
		try {
			Method method = PrognosesAddOn.class.getMethod("statCourse",
					EduCourse.class, EduPeriod.class);
			template.setObjectForKey(method,"ifEmpty");
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		NSTimestamp today = (NSTimestamp)ctx.session().valueForKey("today");
		String title = (String)WOApplication.application().valueForKeyPath(
				"strings.RujelAutoItog_AutoItog.prognoses");
		int sort = 50;
		while (enu.hasMoreElements()) {
			PeriodType perType = (PeriodType) enu.nextElement();
			EduPeriod period = perType.currentPeriod(today);
			if(period == null)
				continue;
			NSMutableDictionary dict = template.mutableClone();
			dict.setObjectForKey(String.valueOf(sort),"sort");
			dict.setObjectForKey(period.title(),"title");
			dict.setObjectForKey(title,"description");
			dict.setObjectForKey(period,"param2");
			result.addObject(dict);
			sort++;
		}
		return result;
	}*/
}
