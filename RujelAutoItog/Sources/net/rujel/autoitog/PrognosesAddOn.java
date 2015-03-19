// PrognosesAddOn.java

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

import java.lang.reflect.Method;
import java.util.Enumeration;
import java.util.logging.Logger;

import com.webobjects.appserver.WOApplication;
import com.webobjects.appserver.WOSession;
import com.webobjects.eocontrol.*;
import com.webobjects.foundation.*;

import net.rujel.base.MyUtility;
import net.rujel.eduresults.*;
import net.rujel.interfaces.*;
import net.rujel.reusables.*;
import net.rujel.ui.AddOnPresenter;

public class PrognosesAddOn extends AddOnPresenter.AddOn {

	protected NSArray _periods;
	public AutoItog periodItem;
	protected Student _student;
	protected NSDictionary _prognosesForStudent;
	protected NSMutableDictionary _courseTimeouts;
	private Period _dates;

	public PrognosesAddOn(WOSession ses) {
		super((NSDictionary)WOApplication.application().
				valueForKeyPath("strings.RujelAutoItog_AutoItog.PrognosesAddOn"),
				(NamedFlags)ses.valueForKeyPath("readAccess.FLAGS.Prognosis"));
		session = ses;
		_courseTimeouts = new NSMutableDictionary();
	}
	protected WOSession session;

	private NamedFlags _accessTimeout;
	public NamedFlags accessTimeout() {
		if(_accessTimeout == null)
			_accessTimeout = (NamedFlags)session.valueForKeyPath(
					"readAccess.FLAGS.StudentTimeout");
		return _accessTimeout;
	}
	private NamedFlags _accessCourseTimeout;
	public NamedFlags accessCourseTimeout() {
		if(_accessCourseTimeout == null)
			_accessCourseTimeout = (NamedFlags)session.valueForKeyPath(
			"readAccess.FLAGS.StudentTimeout");
		return _accessCourseTimeout;
	}

	public void reset() {
		periodItem = null;
		_periods = null;
		super.reset();
		_courseTimeouts.removeAllObjects();
		_student = null;
		_prognosesForStudent = null;
		_timeout = null;
		_prognosis = null;
		_dates = null;
	}

	public void update(EduCourse crs) {
		NSTimestamp minDate = (NSTimestamp)session.objectForKey("minDate");
		NSTimestamp maxDate = (NSTimestamp)session.objectForKey("maxDate");
		if(minDate != null && maxDate != null) {
			setCourse(crs, minDate, maxDate);
			return;
		}
		NSTimestamp date = (NSTimestamp)session.objectForKey("recentDate");
		if(date == null)
			date = (NSTimestamp)session.valueForKey("today");
		setCourse(crs, date, date);
	}

	public void setCourse(EduCourse newCourse, NSTimestamp minDate, NSTimestamp maxDate) {
		if(newCourse != _course)
			reset();
		if(newCourse == null) {
			return;
		}
		_course = newCourse;
		if(_periods == null || _dates == null || 
				!(_dates.contains(minDate) && _dates.contains(maxDate))) {
			_periods = AutoItog.currentAutoItogsForCourse(_course, minDate, maxDate);
			agregate = null;
			_prognosesForStudent = null;
//			_prognosis = null;
			_courseTimeouts.removeAllObjects();
//			_timeout =null;
		}
		_dates = new Period.ByDates(minDate, maxDate);
		if(_periods == null)
			return;
		Enumeration enu = _periods.objectEnumerator();
		while (enu.hasMoreElements()) {
			AutoItog ai = (AutoItog) enu.nextElement();
			CourseTimeout cto = CourseTimeout.getTimeoutForCourseAndPeriod(_course, 
					ai.itogContainer());
			_courseTimeouts.setObjectForKey((cto==null)?NSKeyValueCoding.NullValue:
							cto, ai.itogContainer());
		}
	}

	public NSArray periods() {
		//if(_forcedPeriod == null)
			return _periods;
		//else
		//	return new NSArray(_forcedPeriod);
	}
	
	public void setPeriods(NSArray periods) {
		if(periods == null || periods.count() == 0 || !periods.equals(_periods)) {
			reset();
			_periods = periods;
		}
	}

	protected Prognosis _prognosis;
	public Prognosis prognosis() {
		if(_student == null)
			return null;
		if(_prognosis != null)
			return _prognosis;
		if(_prognosesForStudent != null) {
			_prognosis = (Prognosis)_prognosesForStudent.objectForKey(
					periodItem.itogContainer());
			return _prognosis;
		}
		if(agregate == null) {
			agregate = new NSMutableDictionary();
			Enumeration enu = _periods.objectEnumerator();
			while (enu.hasMoreElements()) {
				AutoItog eduPeriod = (AutoItog) enu.nextElement();
				NSArray prognoses = Prognosis.prognosesArrayForCourseAndPeriod(
						_course, eduPeriod);
				if(prognoses == null || prognoses.count() == 0)
					continue;
				Enumeration penu = prognoses.objectEnumerator();
				while (penu.hasMoreElements()) {
					Prognosis cpr = (Prognosis) penu.nextElement();
					Student student = cpr.student();
					NSMutableDictionary forStudent = (NSMutableDictionary)
					agregate.objectForKey(student);
					if(forStudent == null) {
						forStudent = new NSMutableDictionary(cpr,eduPeriod.itogContainer());
						agregate.setObjectForKey(forStudent, student);
					} else {
						forStudent.setObjectForKey(cpr, eduPeriod.itogContainer());
					}
					if(student == _student && eduPeriod == periodItem) {
						_prognosis = cpr;
						_prognosesForStudent = forStudent;
					}
				} // enumerating prognoses
			} // enumerating periods
			if(_prognosis != null)
				return _prognosis;
		} // _prognosesMatrix == null
		_prognosesForStudent = (NSMutableDictionary)agregate.objectForKey(_student);
		if(_prognosesForStudent == null)
			_prognosesForStudent = NSDictionary.EmptyDictionary;
		_prognosis = (Prognosis)_prognosesForStudent.objectForKey(
				periodItem.itogContainer());

		return _prognosis;
	}

	public void setPrognosis(Prognosis prognosis) {
		if(_prognosis == prognosis)
			return;
		_prognosis = prognosis;
		if(agregate == null)
			return;
		if(prognosis != null && !_periods.contains(prognosis.autoItog())) {
			PerPersonLink forPeriod = (PerPersonLink)
					agregate.objectForKey(prognosis.itogContainer());
			if(forPeriod == null ||
			 		forPeriod.forPersonLink(prognosis.student()) == prognosis)
				return;
			agregate.removeObjectForKey(prognosis.itogContainer());
			return;
		}
		if(prognosis != null)
			_student = prognosis.student();
		NSMutableDictionary forStudent = (NSMutableDictionary)agregate.objectForKey(_student);
		if(prognosis != null) {
			if(periodItem == null ||
					periodItem.itogContainer() != prognosis.itogContainer())
			periodItem = prognosis.autoItog();
			if(forStudent == null) {
				forStudent = new NSMutableDictionary(prognosis,periodItem.itogContainer());
				agregate.setObjectForKey(forStudent, _student);
			} else {
				forStudent.setObjectForKey(prognosis, periodItem.itogContainer());
			}
		} else {
			if(periodItem != null && forStudent != null) {
				forStudent.removeObjectForKey(periodItem.itogContainer());
			}
		}
	}
	
	public void setStudent(Student student) {
		if(student != _student)
			_prognosesForStudent = null;
		_student = student;
	}

	protected Timeout _timeout;
	public Timeout timeout() {
		if(_timeout == null) {
			if(periodItem == null)
				return null;
			if(_student == null) {
				_timeout = courseTimeout();
			} else {
				_timeout = (prognosis()==null)?null:prognosis().getStudentTimeout();
			}
		}
		return _timeout;
	}

	public boolean timeOuts = false;

	public void toggleTimeOuts() {
		timeOuts = !timeOuts;
	}

	public void setPeriodItem(AutoItog period) {
		_prognosis = null;
		_timeout = null;
		inTimeout = false;
		periodItem = period;
	}

	public boolean inTimeout = false;

	public boolean showTimeout() {
		if(!timeOuts)
			return false;
		inTimeout = (!periodItem.namedFlags().flagForKey("noTimeouts"));
		return inTimeout;
	}

	public CourseTimeout courseTimeout() {
		Object result = _courseTimeouts.objectForKey(periodItem.itogContainer());
		if(result==null) { // && _forcedPeriod != null) {
			result = CourseTimeout.getTimeoutForCourseAndPeriod(
					_course, periodItem.itogContainer());
			if(result==null)
				result = NSKeyValueCoding.NullValue;
			_courseTimeouts.setObjectForKey(result, periodItem.itogContainer());
		}
		return (result==NSKeyValueCoding.NullValue)?null:(CourseTimeout)result;
	}

	public void setCourseTimeout(CourseTimeout timeout) {
		if(timeout==null) {
			Enumeration enu = (periodItem == null)?_courseTimeouts.keyEnumerator():
				new NSArray(periodItem.itogContainer()).objectEnumerator();
			while (enu.hasMoreElements()) {
				ItogContainer eduPeriod = (ItogContainer) enu.nextElement();
				CourseTimeout co = CourseTimeout.getTimeoutForCourseAndPeriod(
						_course,eduPeriod);
				_courseTimeouts.setObjectForKey((co==null)?
						NSKeyValueCoding.NullValue:co, eduPeriod);
			}
		} else {
			_courseTimeouts.setObjectForKey(timeout, timeout.itogContainer());
		}
	}

	public static boolean canAccessFieldsDirectly() {
		return true;
	}

	/*	public void setSaveLesson(EduLesson lesson) {
		NSTimestamp date = lesson.date();
//		if(_forcedPeriod != null && !_forcedPeriod.contains(date))
//			_forcedPeriod = null;
		if(_course == lesson.course() && _periods != null) {
			listPeriodsForDate(date);
		} else {// _periods != null
			reset();
			setCourse(lesson.course(),date);
		}
		if(_periods == null)
			return;
		Enumeration enu = _periods.objectEnumerator();
		while (enu.hasMoreElements()) {
			periodItem = (EduPeriod) enu.nextElement();
			calculate();
		}
		setPeriodItem(null);
	}
*/
	public String fireDate() {
		if(prognosis()==null)
			return null;
		NSTimestamp fireDate = prognosis().fireDate();
		if(fireDate == null || fireDate.equals(periodItem.fireDate()))
			return null;
		CourseTimeout courseTimeout = courseTimeout();
		String result = MyUtility.dateFormat().format(fireDate);
		if(courseTimeout != null && fireDate.equals(courseTimeout.fireDate())) {
			result = "<span class=\"dimtext\">" + result + "</span>";
		}
		return result;
	}

	public void calculate() {
		EOEditingContext ec = _course.editingContext();
		Calculator calc = periodItem.calculator();
		if(calc == null)
			return;
		try {
			ec.lock();
			PerPersonLink prognoses = calc.calculatePrognoses(_course,periodItem);
			boolean um = (agregate!=null && _periods.containsObject(periodItem));
			Enumeration enu = _course.groupList().objectEnumerator();
			boolean ifArchive = (SettingsReader.boolForKeyPath("markarchive.Prognosis", 
					SettingsReader.boolForKeyPath("markarchive.archiveAll", false))
					&& periodItem.namedFlags().flagForKey("manual"));
			while (enu.hasMoreElements()) {
				Student student = (Student) enu.nextElement();
				NSMutableDictionary forStudent = (um)?
						(NSMutableDictionary)agregate.objectForKey(student):null;
				Prognosis prognosis = (prognoses==null)?null:
					(Prognosis)prognoses.forPersonLink(student);
				if(prognosis != null) {
//					if(prognosis.fireDate() == null)
//						prognosis.setFireDate(periodItem.fireDate());
					//if(prognosis.fireDate() != null && prognosis.valueChanged())
						prognosis.updateFireDate(courseTimeout());
					if(ifArchive)
						AutoItogModule.archivePrognosisChange(prognosis);
					if(um){
						if(forStudent == null) {
							forStudent = new NSMutableDictionary(prognosis,
									periodItem.itogContainer());
							agregate.setObjectForKey(forStudent, student);
						} else {
							forStudent.setObjectForKey(prognosis, 
									periodItem.itogContainer());
						}
					}
				} else {
					if(um && forStudent != null) {
						forStudent.removeObjectForKey(periodItem.itogContainer());
					}
				}
			}
			if(!um && agregate != null && prognoses != null) {
				agregate.setObjectForKey(prognoses, periodItem);
			}
			EOEnterpriseObject grouping = PrognosesAddOn.getStatsGrouping(_course,
					periodItem.itogContainer());
			if(grouping != null) {
//			NSDictionary stats = PrognosesAddOn.statCourse(_course, prognoses.allValues());
//				grouping.takeValueForKey(stats, "dict");
				NSArray list = (prognoses == null)?null:MyUtility.filterByGroup(
						prognoses.allValues(),"student", _course.groupList(), true);
				grouping.takeValueForKey(list, "array");
			}
			ec.saveChanges();
		} catch (RuntimeException ex) {
			ec.revert();
			agregate = null;
			Logger.getLogger("rujel.autoitog").log(WOLogLevel.WARNING,
					"Error saving calculated prognoses",ex);
			session.takeValueForKey(ex.getMessage(), "message");
		} finally {
			ec.unlock();
		}
	}
	
	public static NSDictionary statCourse(EduCourse course, ItogContainer period) {
		NSArray prognoses = Prognosis.prognosesArrayForCourseAndPeriod(course, period, false);
		if(prognoses == null || prognoses.count() == 0)
			return NSDictionary.EmptyDictionary;
		if(prognoses.count() > 1) {
			EOSortOrdering so = new EOSortOrdering(Prognosis.MARK_KEY,
					EOSortOrdering.CompareCaseInsensitiveAscending);
			prognoses = EOSortOrdering.sortedArrayUsingKeyOrderArray(
					prognoses, new NSArray(so));
		}
		NSMutableArray keys = new NSMutableArray();
		NSArray group = course.groupList();
		NSMutableDictionary result = new NSMutableDictionary(keys, "keys");
		int total = group.count();
		result.setObjectForKey(new Integer(total), "total");
		Enumeration enu = prognoses.objectEnumerator();
		String currKey = null;
		int currCount = 0;
		while (enu.hasMoreElements()) {
			Prognosis pr = (Prognosis) enu.nextElement();
			if(!group.containsObject(pr.student()))
				continue;
			if((currKey==null)?pr.mark()==null:currKey.equalsIgnoreCase(pr.mark())) {
				currCount++;
			} else {
				if(currCount > 0)
					result.setObjectForKey(new Integer(currCount),
							(currKey==null)?" ":currKey);
				currKey = pr.mark();
				keys.addObject((currKey==null)?" ":currKey);
				currCount = 1;
			}
			total--;
		}
		if(currCount > 0)
			result.setObjectForKey(new Integer(currCount), currKey);
		if(total > 0) {
			result.setObjectForKey(new Integer(total), "");
			keys.addObject("");
		}
		return result;
	}
	
	public static EOEnterpriseObject getStatsGrouping (EduCourse course,
			ItogContainer period) {
		EOEnterpriseObject grouping = null;
		try {
			Class descClass = Class.forName("net.rujel.stats.Description");
			Method method = descClass.getMethod("getGrouping", String.class, String.class,
					EOEnterpriseObject.class, EOEnterpriseObject.class, Boolean.TYPE);
			grouping = (EOEnterpriseObject)method.invoke(null, Prognosis.ENTITY_NAME, 
					Prognosis.MARK_KEY, course, period, Boolean.TRUE);
			if(grouping.valueForKeyPath("description.description") == null) {
				String prName = (String)WOApplication.application().valueForKeyPath(
					"strings.RujelAutoItog_AutoItog.properties.Prognosis.this");
				grouping.takeValueForKeyPath(prName,"description.description");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return grouping;
	}
}
