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
import java.util.logging.Logger;
import net.rujel.reusables.WOLogLevel;
import java.util.GregorianCalendar;

public class AutoItogModule {
	protected static Logger logger = Logger.getLogger("rujel.autoitog");
	protected static NSArray addOns = (NSArray)WOApplication.application().
			valueForKeyPath("strings.RujelAutoItog_AutoItog.addOns");
	protected static NSDictionary reportSettings = (NSDictionary)WOApplication.application().
			valueForKeyPath("strings.RujelAutoItog_AutoItog.reportSettings");
	/*
	public static Object init(Object obj) {
		if("notesAddOns".equals(obj)) {
			NSMutableDictionary prognosAddOn = (NSMutableDictionary)GenericAddOn.addonForID(addOns,"prognosis");
			if(prognosAddOn != null) {
				prognosAddOn.removeObjectForKey("prognoses");
			}
			return addOns;
		}
		return null;
	}*/
	
	public static Object init(Object obj, WOContext ctx) {
		if(obj == null || obj.equals("init")) {
			Prognosis.init();
			PrognosUsage.init();
			StudentTimeout.init();
			CourseTimeout.init();
			Bonus.init();
		} else if("scheduleTask".equals(obj)) {
			return scheduleTask(ctx);
		} else if("notesAddOns".equals(obj)) {
			return notesAddOns(ctx);
		} else if("objectSaved".equals(obj)) {
			return objectSaved(ctx);
		} else if("printStudentResults".equals(obj)) {
			return printStudentResults(ctx);
		} else if("reportForStudent".equals(obj)) {
			NSDictionary settings = (NSDictionary)ctx.session().objectForKey("reportForStudent");
			return PrognosReport.reportForStudent(settings);
		} else if("reportSettingsForStudent".equals(obj)) {
			return PlistReader.cloneDictionary(reportSettings, true);
		} else if("extItog".equals(obj)) {
			return extItog(ctx);
		} else if("statCourseReport".equals(obj)) {
			return statCourseReport(ctx);
		}
		return null;
	}
	
	public static Object scheduleTask(WOContext ctx) {
		boolean disable = Boolean.getBoolean("AutoItog.disable")
				|| SettingsReader.boolForKeyPath("edu.disableAutoItog", false);
		if(disable)
			return null;
		Scheduler sched = Scheduler.sharedInstance();
		java.lang.reflect.Method method = null;
		try {
			method = AutoItogModule.class.getMethod("daily",(Class[])null);
		} catch (Exception ex) {
			throw new RuntimeException("Could not get method to schedule",ex);
		}
		sched.registerTask(Scheduler.DAILY,method,null,null,"AutoItog");
		return null;
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
		if(!(period instanceof EduPeriod)) {
			NSTimestamp since = (NSTimestamp)dict.removeObjectForKey("since");
			NSTimestamp to = (NSTimestamp)dict.removeObjectForKey("to");
			qual = EOQualifier.qualifierWithQualifierFormat("begin <= %@ AND end >= %@",new NSArray(new Object[]{since,to}));
			fs = new EOFetchSpecification("EduPeriod",qual,EduPeriod.sorter);
			results = ec.objectsWithFetchSpecification(fs);
			if(results == null || results.count() == 0)
				return null;
			period = (EduPeriod)results.objectAtIndex(0);
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
	
	public static void daily() {
		EOEditingContext ec = new EOEditingContext();
		NSTimestamp day = new NSTimestamp();
		int lag = SettingsReader.intForKeyPath("edu.autoItogLagDays", 0);
		if(lag != 0) {
			GregorianCalendar cal = new GregorianCalendar();
			cal.setTime(day);
			cal.add(GregorianCalendar.DATE, -lag);
			day = new NSTimestamp(cal.getTime());
		}
		EduPeriod period = null;
		NSArray courses = null;
		// EduPeriod end
		EOQualifier qual = new EOKeyValueQualifier("end",EOQualifier.QualifierOperatorEqual,day);
		EOFetchSpecification fs = new EOFetchSpecification("EduPeriod",qual,EduPeriod.sorter);
		fs.setRefreshesRefetchedObjects(true);
		ec.lock();
		try {
		NSArray found = ec.objectsWithFetchSpecification(fs);
		if(found != null && found.count() > 0) {
			Enumeration enu = found.objectEnumerator();
			while (enu.hasMoreElements()) {
				EduPeriod per = (EduPeriod) enu.nextElement();
				if(per != period) {
					period = per;
					courses = coursesForPeriod(period);
				}
				if(courses == null || courses.count() == 0)
					continue;
				logger.log(WOLogLevel.INFO,"EduPeriod " + per.name() +  "ends today.",per);
				automateItogForPeriod(per,courses);
			}
		}
		// CourseTimeout firedate
		NSArray args = new NSArray(day);
		fs.setEntityName("CourseTimeout");
		EOSortOrdering so = new EOSortOrdering("dueDate",EOSortOrdering.CompareAscending);
		NSMutableArray sorter = new NSMutableArray(so);
		so = new EOSortOrdering("periodID",EOSortOrdering.CompareAscending);
		sorter.add(so);
		fs.setSortOrderings(sorter);
		qual = EOQualifier.qualifierWithQualifierFormat("dueDate <= %@ AND flags < 64", args);
		fs.setQualifier(qual);
		found = ec.objectsWithFetchSpecification(fs);
		/*found = EOUtilities.objectsWithQualifierFormat(ec,
				"CourseTimeout","dueDate <= %@ AND flags < 64",args);*/
		if(found != null && found.count() > 0) {
			logger.log(WOLogLevel.INFO,"Automating course timeouts: " + found.count());
			Enumeration enu = found.objectEnumerator();
			while (enu.hasMoreElements()) {
				CourseTimeout cto = (CourseTimeout) enu.nextElement();
				if(cto.eduPeriod() != period) {
					period = cto.eduPeriod();
					courses = coursesForPeriod(period);
				}
				if(courses == null || courses.count() == 0)
					continue;
				automateCourseTimeout(cto, courses);
			}
		}
		// Timed out prognoses automation
		fs.setEntityName("Prognosis");
		fs.setSortOrderings(null);
		qual = new EOKeyValueQualifier("fireDate",EOQualifier.QualifierOperatorLessThanOrEqualTo,day);
		int ignoreAfter = SettingsReader.intForKeyPath("edu.ignorePrognosesAfterDays", 0);
		if(ignoreAfter > 0) {
			NSMutableArray quals = new NSMutableArray(qual);
			NSTimestamp since = day.timestampByAddingGregorianUnits(0, 0, - ignoreAfter, 0, 0, 0);
			qual = new EOKeyValueQualifier("fireDate",
					EOQualifier.QualifierOperatorGreaterThanOrEqualTo,since);
			quals.addObject(qual);
			qual = new EOAndQualifier(quals);
		}
		fs.setQualifier(qual);
		found = ec.objectsWithFetchSpecification(fs);
		if(found != null && found.count() > 0)
			automateTimedOutPrognoses(ec,found);
		/*} catch (Exception e) {
			logger.log(WOLogLevel.WARNING,"Error while converting prognoses to ItogMarks",e);*/
		} finally {
			ec.unlock();
		}
		logger.log(WOLogLevel.FINE,"AutoItog completed");
	}
	
	private static void message(CharSequence text) {
		logger.log(WOLogLevel.INFO, text.toString());
	}
	
	protected static void automateTimedOutPrognoses(EOEditingContext ec, NSArray prognoses) {
		logger.log(WOLogLevel.INFO,"Automating timed out prognoses: " + prognoses.count());
		StringBuffer buf = new StringBuffer("Timed out prognoses:\n");
		EOSortOrdering so = new EOSortOrdering ("eduPeriod.eduYear", EOSortOrdering.CompareAscending);
		NSMutableArray sorter = new NSMutableArray(so);
		so = new EOSortOrdering ("eduPeriod.countInYear", EOSortOrdering.CompareDescending);
		sorter.addObject(so);
		so = new EOSortOrdering ("eduPeriod.begin", EOSortOrdering.CompareAscending);
		sorter.addObject(so);
		so = new EOSortOrdering ("eduCourse.eduGroup", EOSortOrdering.CompareAscending);
		sorter.addObject(so);
//		so = new EOSortOrdering ("eduCourse.cycle", EOSortOrdering.CompareAscending);
//		sorter.addObject(so);
		so = new EOSortOrdering ("eduCourse", EOSortOrdering.CompareAscending);
		sorter.addObject(so);
		prognoses = EOSortOrdering.sortedArrayUsingKeyOrderArray(prognoses, sorter);
//		int last = 0;
		EduPeriod period = null;
		//EduGroup group = null;
		//EduCycle cycle = null;
		EduCourse course = null;
		boolean enableArchive = SettingsReader.boolForKeyPath("markarchive.ItogMark", false);
		boolean overwrite = SettingsReader.boolForKeyPath("edu.overwriteItogsScheduled", false);
		int inCourse = 0;
		for (int i = 0; i < prognoses.count(); i++) {
			Prognosis prognos = (Prognosis)prognoses.objectAtIndex(i);
			if(prognos.eduPeriod() != period) {
				if(course != null) {
					savePrognoses(ec, course, period, buf);
					buf.append("-- ").append(inCourse).append(" --\n");
				}
				period = prognos.eduPeriod();
				buf.append("\n-- ").append(period.name()).append(' ');
				buf.append(period.presentEduYear()).append('\n').append('\n');
				course = null;
			}
			if(prognos.eduCourse() != course) {
				if(course != null) {
					savePrognoses(ec, course, period, buf);
					buf.append("-- ").append(inCourse).append(" --\n");
				}
				course = prognos.eduCourse();
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
				archive.takeValueForKey(itog, "object");
				archive.takeValueForKey("scheduled", "wosid");
				archive.takeValueForKey("AutoItog", "user");
			}
		}
		if(course != null)
			savePrognoses(ec, course, period, buf);
		message(buf);
	}
	
	private static void savePrognoses(EOEditingContext ec, EduCourse course, 
			EduPeriod period, StringBuffer buf) {
		try {
			ec.saveChanges();
		}  catch (Exception ex) {
			logger.log(WOLogLevel.WARNING,"Failed to save timed out prognoses for course", 
					new Object[] {course, ex});
			buf.append("Failed to save timed out prognoses");
			ec.revert();
		}
		ModuleInit.prepareStats(course, period, true);
	}
		
	protected static NSArray coursesForPeriod (EduPeriod period) {
		EOEditingContext ec = period.editingContext();
		
		NSArray periodsUsage = EOUtilities.objectsWithQualifierFormat(ec,
				"PeriodTypeUsage", "(eduYear = %d OR eduYear = 0) AND periodType = %@",
				new NSArray(new Object[] {period.eduYear(),period.periodType()}));
		if(periodsUsage == null || periodsUsage.count() == 0)
			return null;
		period = (EduPeriod)EOUtilities.localInstanceOfObject(ec,period);
		
		NSMutableArray courses = new NSMutableArray();
		Enumeration enu = periodsUsage.objectEnumerator();
		while (enu.hasMoreElements()) {
			EOEnterpriseObject pu = (EOEnterpriseObject)enu.nextElement();
			Object cur = pu.valueForKey("course");
			if(cur != null) {
				courses.addObject(cur);
				continue;
			}
			cur = pu.valueForKey("eduGroup");
			if(cur != null) {
				NSArray args = new NSArray(new Object[] {period.eduYear(),cur});
				NSArray existingCourses = EOUtilities.objectsWithQualifierFormat(ec,EduCourse.entityName,"eduYear = %d AND eduGroup = %@",args);
				courses.addObjectsFromArray(existingCourses);
				continue;
			} else {
				courses.setArray(EOUtilities.objectsMatchingKeyAndValue(ec,EduCourse.entityName,"eduYear",period.eduYear()));
				break;
			}
		}
		if(courses.count() > 0) {
			NSMutableArray invalid = new NSMutableArray();
			enu = courses.objectEnumerator();
			while (enu.hasMoreElements()) {
				EduCourse cur = (EduCourse) enu.nextElement();
				NSArray types = PeriodType.pertypesForCourseFromUsageArray(cur, periodsUsage);
				if(types == null || !types.contains(period.periodType())) {
					invalid.addObject(cur);
				}
			}
			if(invalid.count() > 0)
				courses.removeObjectsInArray(invalid);
			EOSortOrdering so = new EOSortOrdering ("eduGroup", EOSortOrdering.CompareAscending);
			NSMutableArray sorter = new NSMutableArray(so);
			so = new EOSortOrdering ("cycle", EOSortOrdering.CompareAscending);
			sorter.addObject(so);
			EOSortOrdering.sortArrayUsingKeyOrderArray(courses, sorter);
		}
		return courses;
	}
	
	//protected static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
	
	protected static void automateCourseTimeout(CourseTimeout cto, NSArray courses) {
		//EOEditingContext ec = cto.editingContext();
		EduPeriod period = cto.eduPeriod();
		NSMutableSet toProcess = new NSMutableSet(cto.relatedCourses());
		StringBuffer buf = new StringBuffer("Automating CourseTimeout: ");
		buf.append(cto.eduPeriod().name()).append(':');
		
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");//DateFormat.getDateInstance(DateFormat.MEDIUM);
		dateFormat.format(cto.dueDate(), buf, new FieldPosition(DateFormat.DATE_FIELD));
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
		if(cto.eduCourse() != null) {
			buf.append("for single EduCourse");
		}
		buf.append(")\n");

		toProcess.intersectSet(new NSSet(courses));
		Enumeration enu = toProcess.objectEnumerator();
cycleCourses:
		while (enu.hasMoreElements()) {
			EduCourse cur = (EduCourse) enu.nextElement();
			if(cto != CourseTimeout.getTimeoutForCourseAndPeriod(cur, period)) {
				continue cycleCourses;
			}
			cto.namedFlags().setFlagForKey(true, "passed");
			Prognosis.convertPrognosesForCourseAndPeriod(cur,period,cto.dueDate(),buf);
		}
		message(buf);
	}
	
	protected static void automateItogForPeriod(EduPeriod period, NSArray courses) {
		EOEditingContext ec = period.editingContext();
		
		StringBuffer buf = new StringBuffer("Automating end of EduPeriod");
		buf.append(period.name()).append('\n');

		NSArray courseTimeouts = EOUtilities.objectsMatchingKeyAndValue(ec,"CourseTimeout","eduPeriod",period);
		
		Enumeration courseEnum = courses.objectEnumerator();
cycleCourses:
		while (courseEnum.hasMoreElements()) {
			EduCourse course = (EduCourse)courseEnum.nextElement();
			NSArray tmpArray = null;//PeriodType.periodTypesForCourse(course);
			/*if(!tmpArray.containsObject(period.periodType()))
				continue cycleCourses;*/
			NSDictionary dict = new NSDictionary (new Object[] {course,period},
					new String[] {"eduCourse","eduPeriod"});
			tmpArray = EOUtilities.objectsMatchingValues(course.editingContext(), "CourseTimeout", dict);
			if(tmpArray == null || tmpArray.count() == 0) {
				EOQualifier qual = CourseTimeout.qualifierForCourseAndPeriod(course,period);
				tmpArray = EOQualifier.filteredArrayWithQualifier(courseTimeouts,qual);
			}
			if(tmpArray != null && tmpArray.count() > 0) {
				logger.logp(WOLogLevel.INFO,AutoItogModule.class.getName(),"automateItogForPeriod","Itog timed out for course",course);
				continue cycleCourses;
			}
			//single timouts are checked inside convertPrognosesForCourseAndPeriod()
			Prognosis.convertPrognosesForCourseAndPeriod(course,period,period.end(),buf);
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

		EduPeriod period = null;
		Enumeration enu = list.objectEnumerator();
		NSMutableDictionary byCycle = new NSMutableDictionary();
		while (enu.hasMoreElements()) {
			CourseTimeout cto = (CourseTimeout) enu.nextElement();
			if(period != cto.eduPeriod()) {
				if(period != null)
					append(byCycle, result);
				period = cto.eduPeriod();
			}
			EduCourse course = cto.eduCourse();
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

/*		Enumeration enu = courses.objectEnumerator();
		NSArray sort = new NSArray(new Object[] {
				new EOSortOrdering("eduPeriod",EOSortOrdering.CompareAscending),
				EOSortOrdering.sortOrderingWithKey("dueDate",EOSortOrdering.CompareDescending),
		});
		while (enu.hasMoreElements()) {
			EduCourse course = (EduCourse) enu.nextElement();
			qual = CourseTimeout.qualifierForCourseAndPeriod(course,null);
			NSMutableArray tos = timeouts.mutableClone();
			EOQualifier.filterArrayWithQualifier(tos,qual);
			if(tos.count() == 0)
				continue;
			if(tos.count() > 1)
				EOSortOrdering.sortArrayUsingKeyOrderArray(tos, sort);
			// TODOs: more accurate CourseTimeout selection 
			EduPeriod per = null;
			for (int i = 0; i < tos.count(); i++) {
				CourseTimeout ct = (CourseTimeout)tos.objectAtIndex(i);
				if(ct.eduPeriod() == per)
					continue;
				per = ct.eduPeriod();
				NSMutableDictionary dict = ct.extItog();
				dict.takeValueForKey(course.cycle(), "cycle");
				result.addObject(dict);
			}
		}*/

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
	
	public static Object statCourseReport(WOContext ctx) {
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
	}
}
