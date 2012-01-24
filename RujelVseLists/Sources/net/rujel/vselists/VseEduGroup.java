//  VseEduGroup.java

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

package net.rujel.vselists;

import java.util.Calendar;
import java.util.Enumeration;
import java.util.logging.Logger;

import net.rujel.base.MyUtility;
import net.rujel.interfaces.EduGroup;
import net.rujel.interfaces.Person;
import net.rujel.interfaces.PersonLink;
import net.rujel.reusables.NamedFlags;
import net.rujel.reusables.SessionedEditingContext;
import net.rujel.reusables.SettingsReader;
import net.rujel.reusables.WOLogLevel;

import com.webobjects.foundation.*;
import com.webobjects.appserver.WOApplication;
import com.webobjects.appserver.WOSession;
import com.webobjects.eoaccess.EOUtilities;
import com.webobjects.eocontrol.*;

public class VseEduGroup extends _VseEduGroup implements EduGroup {
	
	public static void init() {
		EOSortOrdering.ComparisonSupport.setSupportForClass(
				new EduGroup.ComparisonSupport(), VseEduGroup.class);
	}
	
	public static final NSArray sorter = new NSArray(new Object[] {
			new EOSortOrdering(ABS_GRADE_KEY, EOSortOrdering.CompareDescending),
//			new EOSortOrdering("eduGroup.flags",EOSortOrdering.CompareAscending),
			new EOSortOrdering(TITLE_KEY, EOSortOrdering.CompareAscending)
	});

	public void awakeFromInsertion(EOEditingContext ec) {
		super.awakeFromInsertion(ec);
		setFlags(new Integer(0));
		int maxGrade = SettingsReader.intForKeyPath("edu.maxGrade", 11);
		int minGrade = SettingsReader.intForKeyPath("edu.minGrade", 1);
		Integer year = eduYear();
		if(year == null)
			return;
		setFirstYear(year);
		setAbsGrade(new Integer(year.intValue()-minGrade));
		year = new Integer(year.intValue() + maxGrade - minGrade);
		setLastYear(year);
	}
	
	public Integer grade() {
		Integer absGrade = absGrade();
		if(absGrade == null)
			return absGrade;
		Integer year = MyUtility.eduYear(editingContext());
		if(year == null)
			year = MyUtility.eduYearForDate(null);
		return new Integer(year.intValue() - absGrade.intValue());
	}
	
	public void setGrade(Number grade) {
		if(grade == null) {
			setAbsGrade(null);
			return;
		}
		Integer year = MyUtility.eduYear(editingContext());
		if(year == null)
			year = MyUtility.eduYearForDate(null);
		Integer absGrade = new Integer(year.intValue() - grade.intValue());
		setAbsGrade(absGrade);
	}
	
	public boolean isActual(int eduYear) {
		if(lastYear() == null || firstYear() == null)
			return false;
		return(eduYear <= lastYear().intValue() &&
				eduYear >= firstYear().intValue());
	}

	public NSArray fullList() {
		if(lists() == null)
			return null;
		return (NSArray)lists().valueForKey("student");
	}

	protected NSArray _list;
	protected NSArray _tutors;
	protected long since = 0;
	protected long to = Long.MAX_VALUE;
	public NSArray list() {
		return list(MyUtility.date(editingContext()));
	}
	public NSArray list(NSTimestamp date) {
		NSArray list = vseList(date);
		if(list != null && list.count() > 0)
			return (NSArray)list.valueForKey("student");
		return NSArray.EmptyArray;
	}
	
	public NSArray vseList() {
		return vseList(MyUtility.date(editingContext()));
	}
	public NSArray vseList(NSTimestamp date) {
		long now = date.getTime();
		if(_list == null || now < since || now > to)
			prepareLists(now);
		return _list;
	}
	
	public NSArray tutors() {
		return tutors(MyUtility.date(editingContext()));
	}
	public NSArray tutors(NSTimestamp date) {
		long now = date.getTime();
		if(_tutors == null || now < since || now > to)
			prepareLists(now);
		return _tutors;
	}
	
	public String hover() {
		NSArray tutors = tutors();
		if(tutors == null || tutors.count() == 0)
			return null;
		Enumeration enu = tutors.objectEnumerator();
		StringBuilder buf = new StringBuilder();
		String title = (String)WOApplication.application().valueForKeyPath(
				"strings.RujelVseLists_VseStrings.tutor");
		if(title != null)
			buf.append(title).append(':').append(' ');
		while (enu.hasMoreElements()) {
			VseTutor tt = (VseTutor) enu.nextElement();
			buf.append(Person.Utility.fullName(tt.teacher(), true, 2, 1, 1));
			if(enu.hasMoreElements())
				buf.append(',').append(' ');
		}
		return buf.toString();
	}
	
	protected void prepareLists(long now) {
		since = 0;
		to = Long.MAX_VALUE;
		_list = processList(lists(), now, VseList.sorter);
		_tutors = processList(vseTutors(), now, VseTutor.sorter);
	}
	
	protected NSArray processList(NSArray source, long now, NSArray lSorter) {
		if(source == null || source.count() == 0)
			return NSArray.EmptyArray;
		Enumeration enu = source.objectEnumerator();
		NSMutableArray list = new NSMutableArray();
		Calendar cal = Calendar.getInstance();
		while (enu.hasMoreElements()) {
			EOEnterpriseObject l = (EOEnterpriseObject) enu.nextElement();
			NSTimestamp border = (NSTimestamp)l.valueForKey("leave");
			if(border != null) {
				if(border.getTime() < since - NSLocking.OneDay)
					continue;
				if(border.getTime() < to) {
					cal.setTime(border);
					cal.set(Calendar.HOUR, 23);
					cal.set(Calendar.MINUTE, 59);
					long time = cal.getTimeInMillis(); 
					if(time < now) {
						if(since < time)
							since = time;
						continue;
					} else if(to > time) {
						to = time;
					}
				}
			}
			border = (NSTimestamp)l.valueForKey("enter");
			if(border != null) {
				if(border.getTime() > to)
					continue;
				if(border.getTime() > since - NSLocking.OneDay) {
					cal.setTime(border);
					cal.set(Calendar.HOUR, 0);
					cal.set(Calendar.MINUTE, 0);
					cal.set(Calendar.SECOND, 0);
					cal.set(Calendar.MILLISECOND, 0);
					long time = cal.getTimeInMillis();
					if(time > now) {
						if(to > time) {
							cal.add(Calendar.MINUTE, -1);
							to = cal.getTimeInMillis();
						}
						continue;
					} else if(since < time) {
						since = time;
					}
				}
			}
			list.addObject(l);
		}
		if(list.count() > 1)
			EOSortOrdering.sortArrayUsingKeyOrderArray(list, lSorter);
		return list.immutableClone();
	}
	
	public VseList addStudent(VseStudent student, NSTimestamp date) {
		if(lists().count() > 0) {
			Enumeration enu = lists().objectEnumerator();
			while (enu.hasMoreElements()) {
				VseList l = (VseList) enu.nextElement();
				if(l.student() != student)
					continue;
				if(date == null) {
					l.setEnter(null);
					l.setLeave(null);
					return l;
				} else if(l.leave() == null || l.leave().after(date)) {
					if(l.enter().after(date))
						l.setEnter(date);
					return l;
				}
			}
		}
		VseList l = (VseList)EOUtilities.createAndInsertInstance(editingContext(),
				VseList.ENTITY_NAME);
		addObjectToBothSidesOfRelationshipWithKey(l, LISTS_KEY);
		l.addObjectToBothSidesOfRelationshipWithKey(student, VseList.STUDENT_KEY);
		l.setEnter(date);
		return l;
	}
	
	public void setLists(NSArray value) {
		nullify();
		super.setLists(value);
	}
	public void addToLists(EOEnterpriseObject object) {
		nullify();
		super.addToLists(object);
	}
	public void removeFromLists(EOEnterpriseObject object) {
		nullify();
		super.removeFromLists(object);
	}

	public void setVseTutors(NSArray value) {
		nullify();
		super.setVseTutors(value);
	}
	public void addToVseTutors(EOEnterpriseObject object) {
		nullify();
		super.addToVseTutors(object);
	}
	public void removeFromVseTutors(EOEnterpriseObject object) {
		nullify();
		super.removeFromVseTutors(object);
	}
	
	public int count() {
		return list().count();
	}

	public boolean isInGroup(PersonLink who) {
		return list().containsObject(who);
	}

	public String name() {
		StringBuilder buf = new StringBuilder();
		buf.append(grade()).append(' ').append(title());
		return buf.toString();
	}

	public Integer eduYear() {
		if(editingContext() instanceof SessionedEditingContext)
			return null;
		return MyUtility.eduYearForDate(null);
	}
	
	public static NSArray flagNames = new NSArray(new String[] {"spec"});
	private NamedFlags _flags;
    public NamedFlags namedFlags() {
    	if(_flags==null) {
    		_flags = new NamedFlags(flags().intValue(),flagNames);
    		try{
    			_flags.setSyncParams(this, getClass().getMethod("setNamedFlags",
    					NamedFlags.class));
    		} catch (Exception e) {
    			Logger.getLogger("rujel.curriculum").log(WOLogLevel.WARNING,
						"Could not get syncMethod for Reason flags",e);
			}
    	}
    	return _flags;
    }
    
    public void setNamedFlags(NamedFlags flags) {
    	if(flags != null)
    		setFlags(flags.toInteger());
    	_flags = flags;
    }
    
    public void setFlags(Integer value) {
    	_flags = null;
    	super.setFlags(value);
    }

    public void nullify() {
		_list = null;
		_tutors = null;
		since = 0;
		to = Long.MAX_VALUE;
		_flags = null;
    }
    
	public void turnIntoFault(EOFaultHandler handler) {
		super.turnIntoFault(handler);
		nullify();
	}

	public static NSArray listGroups(NSTimestamp date, EOEditingContext ec) {
		Object section = null;
		Integer year = MyUtility.eduYearForDate(date);
		try {
			if (ec instanceof SessionedEditingContext) {
				WOSession ses = ((SessionedEditingContext)ec).session();
				if(ses != null) {
					if(date == null) {
						Integer sYear = (Integer)ses.valueForKey("eduYear");
						if(sYear != null) year = sYear;
					}
					section = ses.valueForKeyPath("state.section.idx");
				}
			}
		} catch (Exception e) {
			;
		}
		EOQualifier[] quals = new EOQualifier[3];
		quals[0] = new EOKeyValueQualifier(FIRST_YEAR_KEY,
				EOQualifier.QualifierOperatorLessThanOrEqualTo,year);
		quals[1] = new EOKeyValueQualifier(LAST_YEAR_KEY,
				EOQualifier.QualifierOperatorGreaterThanOrEqualTo,year);
		if(section != null) {
			quals[2] = new EOKeyValueQualifier(SECTION_KEY,
					EOQualifier.QualifierOperatorEqual,section);
		}
		quals[0] = new EOAndQualifier(new NSArray(quals));
		EOFetchSpecification fs = new EOFetchSpecification(ENTITY_NAME,quals[0],sorter);
		NSArray result = ec.objectsWithFetchSpecification(fs);
		if(result == null || result.count() <= 1)
			return result;
		return result;
	}
}
