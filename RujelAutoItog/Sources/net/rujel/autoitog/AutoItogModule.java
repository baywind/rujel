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
import net.rujel.eduresults.*;
import net.rujel.reusables.*;

import com.webobjects.foundation.*;
import com.webobjects.eocontrol.*;
import com.webobjects.appserver.*;
import com.webobjects.eoaccess.EOUtilities;

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
			valueForKeyPath("extStrings.RujelAutoItog_AutoItog.addOns");
	protected static NSDictionary reportSettings = (NSDictionary)WOApplication.application().
			valueForKeyPath("extStrings.RujelAutoItog_AutoItog.reportSettings");
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
		} else if("init3".equals(obj)) {
			return init3(ctx);
		} else if("notesAddOns".equals(obj)) {
			return notesAddOns(ctx);
		} else if("saveLesson".equals(obj)) {
			return saveLesson(ctx);
		} else if("printStudentResults".equals(obj)) {
			return printStudentResults(ctx);
		} else if("reportForStudent".equals(obj)) {
			NSDictionary settings = (NSDictionary)ctx.session().objectForKey("reportForStudent");
			return PrognosReport.reportForStudent(settings);
		} else if("reportSettingsForStudent".equals(obj)) {
			return PlistReader.cloneDictionary(reportSettings, true);
		}
		return null;
	}
	
	public static Object init3(WOContext ctx) {
		boolean disable = Boolean.getBoolean("AutoItog.disable");
		if(disable)
			return null;
		Scheduler sched = Scheduler.sharedInstance();
		/*EOEditingContext ec = new EOEditingContext();
		NSTimestamp today = new NSTimestamp();
		Integer eduYear = MyUtility.eduYearForDate(today);
		NSArray periodTypes = EOUtilities.objectsWithQualifierFormat(ec,
				"PeriodTypeUsage","eduYear = 0 OR eduYear = %@",new NSArray(eduYear));
		periodTypes = (NSArray)periodTypes.valueForKey("periodType");
		NSSet set = new NSSet(periodTypes);
		
		Enumeration enu = set.objectEnumerator();*/
		java.lang.reflect.Method method = null;
		try {
			method = AutoItogModule.class.getMethod("daily",(Class[])null);
		} catch (Exception ex) {
			throw new RuntimeException("Could not get method to schedule",ex);
		}
		sched.registerTask(Scheduler.DAILY,method,null,null,"AutoItog");
		/*
		int perNum = -1;
		while (enu.hasMoreElements()) {
			PeriodType perType = (PeriodType)enu.nextElement();
			//EduPeriod period = perType.currentPeriod(today);
			String perTypeId = "EduPeriod." + perType.name();
			perNum = sched.numForID(perTypeId);//numForPeriod(period);
			if(perNum == -1) {
				logger.logp(WOLogLevel.INFO,AutoItogModule.class.getName(),"init3",
						"Error setting AutoItog task for period - no such period defined in scheduler",perTypeId);
				continue;
			}
			sched.registerTask(perNum,method,null,Scheduler.THIS_PERIOD,"autoItog");
		}*/

		/*
		Period timeoutPer = TimeoutPeriod.getRecentPeriod();
		if(timeoutPer != null) {
			perNum = sched.registerPeriod(timeoutPer);
			
			try {
				method = TimeoutPeriod.class.getMethod("automateTimedOutPeriod", new Class[]{Period.class});
				sched.registerTask(perNum,method,null,Scheduler.THIS_PERIOD,timeoutPer.typeID());
			} catch (Exception ex) {
				throw new RuntimeException("Could not get method to schedule",ex);
			}
		}*/
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
		/*
		Enumeration enu = addOns.objectEnumerator();
		NSMutableArray result = new NSMutableArray();
		while (enu.hasMoreElements()) {
			NSDictionary dic = (NSDictionary)enu.nextElement();
			//access
			UserPresentation user = (UserPresentation)ctx.session().valueForKey("user");
			NamedFlags access = null;
			if(user != null) {
				try {
					int lvl = user.accessLevel(dic.valueForKey("accessKey"));
					access = new ImmutableNamedFlags(lvl,UseAccess.accessKeys);
				}  catch (AccessHandler.UnlistedModuleException e) {
					access = DegenerateFlags.ALL_TRUE;
				}
			}
			if(access == null)
				access = DegenerateFlags.ALL_TRUE;
			if(access.getFlag(0)) {
				dic = dic.mutableClone();
				dic.takeValueForKey(access,"access");
				result.addObject(dic);
			}

		}
		return result;
		*/
	}

	public static Object saveLesson(WOContext ctx) {
		WOSession ses = ctx.session();
		String key = "AutoItog.PrognosesAddOn";
		PrognosesAddOn addOn = (PrognosesAddOn)ses.objectForKey(key);
		if(addOn == null)
			return null;
		EduLesson currLesson = (EduLesson)ctx.page().valueForKey("currLesson");
		if(currLesson == null) return null;
		if(currLesson.date().compareTo(new NSTimestamp()) > 0)
			return null;
		addOn.setSaveLesson(currLesson);
		return null;
	/*	EduCourse course = currLesson.course();
		//(EduCourse)ctx.page().valueForKey("course");
		NSArray pertypes = PeriodType.periodTypesForCourse(course);
		if(pertypes == null) return null;
		PeriodType pertype = (PeriodType)pertypes.objectAtIndex(0);
		EduPeriod period = pertype.currentPeriod(currLesson.date());
//		if(period.contains(currLesson.date())) {

		course.editingContext().lock();
		try {
			Calculator calc = PrognosUsage.prognosUsage(course, period.periodType()).calculator();
			PerPersonLink result = calc.calculatePrognoses(course,period);
			course.editingContext().saveChanges();
			NSKeyValueCoding prognosAddOn = null;
			NSArray usedAddOns = (NSArray)ctx.page().valueForKey("activeNotesAddOns");
			if(usedAddOns != null) {
				prognosAddOn = GenericAddOn.addonForID(usedAddOns,"prognosis");
			}
			if(prognosAddOn == null) {
				usedAddOns = (NSArray)ctx.page().valueForKey("notesAddOns");
				if(usedAddOns != null) {
					prognosAddOn = GenericAddOn.addonForID(usedAddOns,"prognosis");
				}
			}
			if(prognosAddOn != null) {
				prognosAddOn.takeValueForKey(result,"prognoses");
				prognosAddOn.takeValueForKey(period,"eduPeriod");
			}
		} catch (RuntimeException ex) {
			course.editingContext().revert();
			throw ex;
		} finally {
			course.editingContext().unlock();				
		}
//		}
		return null;*/
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
		String title = (String)WOApplication.application().valueForKeyPath("extStrings.RujelAutoItog-AutoItog.prognosis");
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
	}
	
	private static void message(CharSequence text) {
		logger.log(WOLogLevel.INFO, text.toString());
	}
	
	protected static void automateTimedOutPrognoses(EOEditingContext ec, NSArray prognoses) {
		StringBuffer buf = new StringBuffer("Timed out prognoses: ");
		EOSortOrdering so = new EOSortOrdering ("eduPeriod.countInYear", EOSortOrdering.CompareDescending);
		NSMutableArray sorter = new NSMutableArray(so);
		so = new EOSortOrdering ("eduPeriod.begin", EOSortOrdering.CompareAscending);
		sorter.addObject(so);
		so = new EOSortOrdering ("eduCourse.eduGroup", EOSortOrdering.CompareAscending);
		sorter.addObject(so);
		so = new EOSortOrdering ("eduCourse.cycle", EOSortOrdering.CompareAscending);
		sorter.addObject(so);
		so = new EOSortOrdering ("eduCourse", EOSortOrdering.CompareAscending);
		sorter.addObject(so);
		prognoses = EOSortOrdering.sortedArrayUsingKeyOrderArray(prognoses, sorter);
		int last = 0;
		EduPeriod period = null;
		EduGroup group = null;
		EduCycle cycle = null;
		EduCourse course = null;
		boolean enableArchive = SettingsReader.boolForKeyPath("markarchive.ItogMark", false);
		boolean overwrite = SettingsReader.boolForKeyPath("edu.overwriteItogsScheduled", false);
		for (int i = 0; i < prognoses.count(); i++) {
			Prognosis prognos = (Prognosis)prognoses.objectAtIndex(i);
			if(prognos.eduPeriod() != period) {
				period = prognos.eduPeriod();
				buf.append(period.name()).append('\n');
			}
			if(prognos.eduCourse() != course) {
				course = prognos.eduCourse();
				if(course.eduGroup() != group) {
					group = course.eduGroup();
					buf.append(group.name()).append(':');
				}
				if(course.cycle() != cycle) {
					cycle = course.cycle();
					buf.append(cycle.subject()).append(':');
				}
				buf.append('\n');
				if((i - last) > 12) {
					try {
						last = i;
						ec.saveChanges();
					}  catch (Exception ex) {
						logger.log(WOLogLevel.WARNING,
								"Failed to save timed out prognoses", ex);
						buf.append("Failed to save timed out prognoses");
						ec.revert();
					}
					buf.append("--\n");
				}
			}
			ItogMark itog = prognos.convertToItogMark(null, overwrite, buf);
			if(enableArchive && itog != null) {
				EOEnterpriseObject archive = EOUtilities.createAndInsertInstance(ec,"MarkArchive");
				archive.takeValueForKey(itog, "object");
				archive.takeValueForKey("scheduled", "wosid");
				archive.takeValueForKey("AutoItog", "user");
			}
		}
		try {
			ec.saveChanges();
		}  catch (Exception ex) {
			logger.log(WOLogLevel.WARNING,
					"Failed to save timed out prognoses", ex);
			buf.append("Failed to save timed out prognoses");
			ec.revert();
		}
		message(buf);
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
			/*
			Enumeration studEnum = course.groupList().objectEnumerator();
			PerPersonLink prognoses = Prognosis.prognosesForCourseAndPeriod(course,period);
			NSArray itogs = ItogMark.getItogMarks(course.cycle(),period,null,ec);
cycleStudents:
			while (studEnum.hasMoreElements()) {
				Student student = (Student)studEnum.nextElement();
				Prognosis prognos = (Prognosis)prognoses.forPersonLink(student);
				if(prognos == null || prognos.getTimeout() != null)
					continue cycleStudents;
				prognos.convertToItogMark(itogs);
				
				ItogMark itog = ItogMark.getItogMark(null,null,student,itogs);
				if(itog == null) {
					itog = (ItogMark)EOUtilities.createAndInsertInstance(ec,"ItogMark");
					itog.addObjectToBothSidesOfRelationshipWithKey(period,"eduPeriod");
					itog.addObjectToBothSidesOfRelationshipWithKey(student,"student");
					itog.addObjectToBothSidesOfRelationshipWithKey(course.cycle(),"cycle");
				} else {
					itog.readFlags().setFlagForKey(true,"changed");
				}
				itog.setValue(prognos.value());
				itog.setMark(prognos.presentValue());
				itog.readFlags().setFlagForKey(true,"calculated");
				itog.readFlags().setFlagForKey(true,"scheduled");
				if(BigDecimal.ONE.compareTo(prognos.complete()) > 0) {
					itog.readFlags().setFlagForKey(true,"incomplete");
				}
			} // cycleStudents
			
			if(ec.hasChanges()) {
				try {
					ec.saveChanges();
					logger.logp(WOLogLevel.INFO,AutoItogModule.class.getName(),"automateItogForPeriod","Saved itogs based on prognoses for course",course);
				}  catch (Exception ex) {
					logger.logp(WOLogLevel.WARNING,AutoItogModule.class.getName(),"automateItogForPeriod","Failed to save itogs based on prognoses for course", new Object[] {course,ex});
					ec.revert();
				}
			} else { //ec.hasChanges()
				logger.logp(WOLogLevel.INFO,AutoItogModule.class.getName(),"automateItogForPeriod","No itogs to save for course",course);
			}
		} // cycleCourses
		
		
	}*/
}
