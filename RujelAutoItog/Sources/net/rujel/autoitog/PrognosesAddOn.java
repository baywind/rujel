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
import com.webobjects.eoaccess.EOUtilities;
import com.webobjects.eocontrol.*;
import com.webobjects.foundation.*;

import net.rujel.auth.AccessHandler;
import net.rujel.auth.UseAccess;
import net.rujel.auth.UserPresentation;
import net.rujel.base.MyUtility;
import net.rujel.eduresults.*;
import net.rujel.interfaces.*;
import net.rujel.reusables.*;

public class PrognosesAddOn implements NSKeyValueCoding, NSKeyValueCoding.ErrorHandling {

	protected static NSDictionary addOn = (NSDictionary)WOApplication.application().
						valueForKeyPath("strings.RujelAutoItog_AutoItog.PrognosesAddOn");

	protected EduCourse _course;
	protected NSArray _periods;
	protected NSMutableDictionary _usagesForPerTypes;
	protected NSMutableDictionary _prognosesMatrix;
	public EduPeriod periodItem;
	protected Student _student;
	protected NSDictionary _prognosesForStudent;
	//protected EduPeriod _forcedPeriod;
	protected NSMutableDictionary _courseTimeouts;

	public PrognosesAddOn(WOSession ses) {
		super();
		session = ses;
		user = (UserPresentation)ses.valueForKey("user");
	}
	protected WOSession session;
	protected UserPresentation user;

	private NamedFlags _access;
	public NamedFlags access() {
		if(user == null)
			_access = DegenerateFlags.ALL_TRUE;
		else {
			try {
				int lvl = user.accessLevel("Prognosis");
				_access = new ImmutableNamedFlags(lvl,UseAccess.accessKeys);
			}  catch (AccessHandler.UnlistedModuleException e) {
				_access = DegenerateFlags.ALL_TRUE;
			}
		}
		return _access;
	}

	private NamedFlags _accessTimeout;
	public NamedFlags accessTimeout() {
		if(user == null)
			_accessTimeout = DegenerateFlags.ALL_TRUE;
		else {
			try {
				int lvl = user.accessLevel("StudentTimeout");
				_accessTimeout = new ImmutableNamedFlags(lvl,UseAccess.accessKeys);
			}  catch (AccessHandler.UnlistedModuleException e) {
				_accessTimeout = DegenerateFlags.ALL_TRUE;
			}
		}
		return _accessTimeout;
	}
	private NamedFlags _accessCourseTimeout;
	public NamedFlags accessCourseTimeout() {
		if(user == null)
			_accessCourseTimeout = DegenerateFlags.ALL_TRUE;
		else {
			try {
				int lvl = user.accessLevel("CourseTimeout");
				_accessCourseTimeout = new ImmutableNamedFlags(lvl,UseAccess.accessKeys);
			}  catch (AccessHandler.UnlistedModuleException e) {
				_accessCourseTimeout = DegenerateFlags.ALL_TRUE;
			}
		}
		return _accessCourseTimeout;
	}

	public void reset() {
		periodItem = null;
		_periods = null;
		_course = null;
		_prognosesMatrix = null;
		_courseTimeouts = null;
		//_forcedPeriod = null;
		_usagesForPerTypes = null;
		_student = null;
		_prognosesForStudent = null;
		_timeout = null;
		_prognosis = null;
		currDate = null;
	}

	public void setCourse(EduCourse newCourse) {
		NSTimestamp date = (NSTimestamp)session.objectForKey("recentDate");
		if(date == null)
			date = (NSTimestamp)session.valueForKey("today");
		setCourse(newCourse, date);
	}

	public void setCourse(EduCourse newCourse, NSTimestamp date) {
		if(newCourse == _course) {
			listPeriodsForDate(date);
			return;
		}
		reset();
		if(newCourse == null) {
			return;
		}
		_course = newCourse;
/*		if(_forcedPeriod != null) {
			_forcedPeriod = (EduPeriod)EOUtilities.localInstanceOfObject(
					_course.editingContext(), _forcedPeriod);
		}
*/		NSArray pertypes = PeriodType.periodTypesForCourse(_course);
		NSMutableArray result = new NSMutableArray();
		_usagesForPerTypes = new NSMutableDictionary();
		_courseTimeouts = new NSMutableDictionary();
		Enumeration enu = pertypes.objectEnumerator();
		while (enu.hasMoreElements()) {
			PeriodType type = (PeriodType) enu.nextElement();
			PrognosUsage usage = PrognosUsage.prognosUsage(_course, type);
			if(usage == null || !usage.namedFlags().flagForKey("active"))
				continue;
			_usagesForPerTypes.setObjectForKey(usage, type);
			EduPeriod eduPeriod = type.currentPeriod(date);
			if(eduPeriod == null)
				continue;
			result.addObject(eduPeriod);
			CourseTimeout co = CourseTimeout.getTimeoutForCourseAndPeriod(_course, eduPeriod);
			_courseTimeouts.setObjectForKey((co==null)?NSKeyValueCoding.NullValue:co, eduPeriod);
		}
		EOSortOrdering.sortArrayUsingKeyOrderArray(result, EduPeriod.sorter);
		_periods = result.immutableClone();
		currDate = date;
	}

/*	public void setEduPeriod(Period period) {
		if(period instanceof EduPeriod) {
			if(_course != null)
				_forcedPeriod = (EduPeriod)EOUtilities.localInstanceOfObject(_course.editingContext(), (EduPeriod)period);
			else
				_forcedPeriod = (EduPeriod)period;
		} else {
			_forcedPeriod = null;
			if(period != null && _course != null && _periods != null) {
				listPeriodsForDate(new NSTimestamp(period.end()));
			}
		}
	}
*/
	public NSArray periods() {
		//if(_forcedPeriod == null)
			return _periods;
		//else
		//	return new NSArray(_forcedPeriod);
	}

	public PrognosUsage usage() {
		if(_usagesForPerTypes == null || periodItem == null)
			return null;
		return (PrognosUsage)_usagesForPerTypes.objectForKey(periodItem.periodType());
	}

	protected NSTimestamp currDate;
	protected void listPeriodsForDate(NSTimestamp date) {
		if(date.equals(currDate))
			return;
		NSMutableArray result = new NSMutableArray();
		boolean justUpdate = (_periods != null && 
				_periods.count() == _usagesForPerTypes.count());

		Enumeration enu = (justUpdate)?_periods.objectEnumerator()
				:_usagesForPerTypes.keyEnumerator();
		PeriodType type = null;
		while (enu.hasMoreElements()) {
			EduPeriod eduPeriod = (justUpdate)?(EduPeriod)enu.nextElement():null;
			if(!justUpdate || !eduPeriod.contains(date)) {
				type = (justUpdate)?eduPeriod.periodType()
						:(PeriodType)enu.nextElement();
				eduPeriod = type.currentPeriod(date);
				if(eduPeriod == null)
					continue;
				CourseTimeout co = CourseTimeout.getTimeoutForCourseAndPeriod(_course, eduPeriod);
				_courseTimeouts.setObjectForKey((co==null)?NSKeyValueCoding.NullValue:co, eduPeriod);
			}
			result.addObject(eduPeriod);
		}
		if(type == null)
			return;
		periodItem = null;
		_prognosesMatrix = null;
		//_courseTimeouts = null;
		_student = null;
		_prognosesForStudent = null;
		_timeout = null;
		_prognosis = null;

		EOSortOrdering.sortArrayUsingKeyOrderArray(result, EduPeriod.sorter);
		_periods = result.immutableClone();
		currDate = date;
	}

	protected Prognosis _prognosis;
	public Prognosis prognosis() {
		if(_student == null)
			return null;
		if(_prognosis != null)
			return _prognosis;
		if(_prognosesForStudent != null) {
			_prognosis = (Prognosis)_prognosesForStudent.objectForKey(periodItem);
			return _prognosis;
		}
		if(_prognosesMatrix == null) {
			_prognosesMatrix = new NSMutableDictionary();
			Enumeration enu = _periods.objectEnumerator();
			while (enu.hasMoreElements()) {
				EduPeriod eduPeriod = (EduPeriod) enu.nextElement();
				EOEditingContext ec = _course.editingContext();
				PrognosUsage usage = (PrognosUsage)_usagesForPerTypes.objectForKey(eduPeriod.periodType());
				if(usage == null)
					continue;
				NSDictionary dict = new NSDictionary(
						new Object[] {_course,eduPeriod},
						new String[] {"eduCourse","eduPeriod"});
				NSArray prognoses = EOUtilities.objectsMatchingValues(ec, "Prognosis", dict);
				if(prognoses == null || prognoses.count() == 0)
					continue;
				Enumeration penu = prognoses.objectEnumerator();
				while (penu.hasMoreElements()) {
					Prognosis cpr = (Prognosis) penu.nextElement();
					cpr._setPrognosUsage(usage);
					Student student = cpr.student();
					NSMutableDictionary forStudent = (NSMutableDictionary)
					_prognosesMatrix.objectForKey(student);
					if(forStudent == null) {
						forStudent = new NSMutableDictionary(cpr,eduPeriod);
						_prognosesMatrix.setObjectForKey(forStudent, student);
					} else {
						forStudent.setObjectForKey(cpr, eduPeriod);
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
		
/*		if(periodItem == _forcedPeriod && !_periods.contains(_forcedPeriod)) {
			PerPersonLink forPeriod = (PerPersonLink)_prognosesMatrix.objectForKey(_forcedPeriod);
			if(forPeriod == null) {
				forPeriod = Prognosis.prognosesForCourseAndPeriod(_course, _forcedPeriod);
				if(forPeriod != null)
					_prognosesMatrix.setObjectForKey(forPeriod, _forcedPeriod);
			}
			_prognosis = (forPeriod==null)?null:(Prognosis)forPeriod.forPersonLink(_student);
			return _prognosis;
		}
*/
		_prognosesForStudent = (NSMutableDictionary)_prognosesMatrix.objectForKey(_student);
		if(_prognosesForStudent == null)
			_prognosesForStudent = NSDictionary.EmptyDictionary;
		_prognosis = (Prognosis)_prognosesForStudent.objectForKey(periodItem);

		return _prognosis;
	}

	public void setPrognosis(Prognosis prognosis) {
		if(_prognosis == prognosis)
			return;
		_prognosis = prognosis;
		if(_prognosesMatrix == null)
			return;
		if(prognosis != null && !_periods.contains(prognosis.eduPeriod())) {
			PerPersonLink forPeriod = (PerPersonLink) _prognosesMatrix.objectForKey(prognosis.eduPeriod());
			if(forPeriod == null || forPeriod.forPersonLink(prognosis.student()) == prognosis)
				return;
			_prognosesMatrix.removeObjectForKey(prognosis.eduPeriod());
			return;
		}
		NSMutableDictionary forStudent = (NSMutableDictionary)_prognosesMatrix.objectForKey(_student);
		if(prognosis != null) {
			if(forStudent == null) {
				forStudent = new NSMutableDictionary(prognosis,periodItem);
				_prognosesMatrix.setObjectForKey(forStudent, _student);
			} else {
				forStudent.setObjectForKey(prognosis, periodItem);
			}
		} else {
			if(forStudent != null) {
				forStudent.removeObjectForKey(periodItem);
			}
/*			if (_forcedPeriod != null) {
				_prognosesMatrix.removeObjectForKey(_forcedPeriod);
			}
*/		}
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

	public void setPeriodItem(EduPeriod period) {
		_prognosis = null;
		_timeout = null;
		inTimeout = false;
		periodItem = period;
	}

	public boolean inTimeout = false;

	public boolean showTimeout() {
		if(!timeOuts)
			return false;
		/*		if(!accessTimeout().flagForKey("read"))
			return false;*/
		PrognosUsage usage = usage();
		if(usage == null)
			return false;
		inTimeout = (!usage.namedFlags().flagForKey("denyTimeout"));
		return inTimeout;
	}

	public CourseTimeout courseTimeout() {
		Object result = _courseTimeouts.objectForKey(periodItem);
		if(result==null) { // && _forcedPeriod != null) {
			result = CourseTimeout.getTimeoutForCourseAndPeriod(_course, periodItem);
			if(result==null)
				result = NSKeyValueCoding.NullValue;
			_courseTimeouts.setObjectForKey(result, periodItem);
		}
		return (result==NSKeyValueCoding.NullValue)?null:(CourseTimeout)result;
	}

	public void setCourseTimeout(CourseTimeout timeout) {
		if(timeout==null) {
			Enumeration enu = (periodItem == null)?_courseTimeouts.keyEnumerator():new NSArray(periodItem).objectEnumerator();
			while (enu.hasMoreElements()) {
				EduPeriod eduPeriod = (EduPeriod) enu.nextElement();
				CourseTimeout co = CourseTimeout.getTimeoutForCourseAndPeriod(_course, eduPeriod);
				_courseTimeouts.setObjectForKey((co==null)?NSKeyValueCoding.NullValue:co, eduPeriod);
			}
		} else {
			_courseTimeouts.setObjectForKey(timeout, timeout.eduPeriod());
		}
	}

	public static boolean canAccessFieldsDirectly() {
		return true;
	}

	public void takeValueForKey(Object value, String key) {
		NSKeyValueCoding.DefaultImplementation.takeValueForKey(this, value, key);
	}

	public Object valueForKey(String key) {
		Object result = addOn.valueForKey(key);
		if(result != null)
			return result;
		result = NSKeyValueCoding.DefaultImplementation.valueForKey(this, key);
		return result;
	}

	protected NSMutableDictionary userInfo;
	public Object handleQueryWithUnboundKey(String key) {
		if(userInfo == null)
			return null;
		return userInfo.valueForKey(key);
	}

	public void handleTakeValueForUnboundKey(Object value, String key) {
		if(userInfo == null) {
			if(value != null)
				userInfo = new NSMutableDictionary(value,key);
		} else
			userInfo.takeValueForKey(value, key);
	}

	public void unableToSetNullForKey(String key) {
		NSKeyValueCoding.DefaultImplementation.unableToSetNullForKey(this, key);
		//throw new NullPointerException("unableToSetNullForKey : " + key);
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
		if(fireDate == null || fireDate.equals(periodItem.end()))
			return null;
		CourseTimeout courseTimeout = courseTimeout();
		String result = MyUtility.dateFormat().format(fireDate);
		if(courseTimeout != null && fireDate.equals(courseTimeout.dueDate())) {
			result = "<span class=\"dimtext\">" + result + "</span>";
		}
		return result;
	}

	public void calculate() {
		EOEditingContext ec = _course.editingContext();
		PrognosUsage usage = usage();
		if(usage == null)
			return;
		Calculator calc = usage.calculator();
		if(calc == null)
			return;
		try {
			ec.lock();
			PerPersonLink prognoses = calc.calculatePrognoses(_course,periodItem);
			boolean um = (_prognosesMatrix!=null && _periods.containsObject(periodItem));
			Enumeration enu = _course.groupList().objectEnumerator();
			boolean ifArchive = (SettingsReader.boolForKeyPath("markarchive.Prognosis", false)
					&& usage.namedFlags().flagForKey("manual"));
			while (enu.hasMoreElements()) {
				Student student = (Student) enu.nextElement();
				NSMutableDictionary forStudent = (um)?(NSMutableDictionary)_prognosesMatrix.objectForKey(student):null;
				Prognosis prognosis = (prognoses==null)?null:(Prognosis)prognoses.forPersonLink(student);
				if(prognosis != null) {
					//if(prognosis.fireDate() != null && prognosis.valueChanged())
						prognosis.updateFireDate(courseTimeout());
					if(ifArchive)
						AutoItogModule.archivePrognosisChange(prognosis, usage);
					if(um){
						if(forStudent == null) {
							forStudent = new NSMutableDictionary(prognosis,periodItem);
							_prognosesMatrix.setObjectForKey(forStudent, student);
						} else {
							forStudent.setObjectForKey(prognosis, periodItem);
						}
					}
				} else {
					if(um && forStudent != null) {
						forStudent.removeObjectForKey(periodItem);
					}
				}
			}
			if(!um && _prognosesMatrix != null && prognoses != null) {
				_prognosesMatrix.setObjectForKey(prognoses, periodItem);
			}
			EOEnterpriseObject grouping = PrognosesAddOn.getStatsGrouping(_course, periodItem);
			if(grouping != null) {
//				NSDictionary stats = PrognosesAddOn.statCourse(_course, prognoses.allValues());
//				grouping.takeValueForKey(stats, "dict");
				NSArray list = MyUtility.filterByGroup(prognoses.allValues(),
						"student", _course.groupList(), true);
				grouping.takeValueForKey(list, "array");
			}
			ec.saveChanges();
		} catch (RuntimeException ex) {
			ec.revert();
			Logger.getLogger("rujel.autoitog").log(WOLogLevel.WARNING,"Error saving calculated prognoses",ex);
			session.takeValueForKey(ex.getMessage(), "message");
		}
		ec.unlock();
	}
	
	public static NSDictionary statCourse(EduCourse course, EduPeriod period) {
		NSArray prognoses = Prognosis.prognosesArrayForCourseAndPeriod(course, period);
		if(prognoses == null || prognoses.count() == 0)
			return NSDictionary.EmptyDictionary;
		if(prognoses.count() > 1) {
			EOSortOrdering so = new EOSortOrdering(Prognosis.MARK_KEY,
					EOSortOrdering.CompareCaseInsensitiveAscending);
			prognoses = EOSortOrdering.sortedArrayUsingKeyOrderArray(prognoses, new NSArray(so));
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
					result.setObjectForKey(new Integer(currCount), (currKey==null)?" ":currKey);
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
	
	public static EOEnterpriseObject getStatsGrouping (EduCourse course, EduPeriod period) {
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
