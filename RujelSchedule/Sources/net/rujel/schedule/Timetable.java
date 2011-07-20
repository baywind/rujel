// Timetable.java: Class file for WO Component 'Timetable'

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

package net.rujel.schedule;

import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.logging.Logger;

import com.webobjects.appserver.WOActionResults;
import com.webobjects.appserver.WOContext;
import com.webobjects.appserver.WOElement;
import com.webobjects.appserver.WOMessage;
import com.webobjects.eoaccess.EOUtilities;
import com.webobjects.eocontrol.*;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSMutableArray;
import com.webobjects.foundation.NSMutableSet;
import com.webobjects.foundation.NSTimestamp;

import net.rujel.base.MyUtility;
import net.rujel.base.SettingsBase;
import net.rujel.interfaces.EduCourse;
import net.rujel.reusables.SettingsReader;
import net.rujel.reusables.Various;
import net.rujel.reusables.WOLogLevel;
import net.rujel.ui.LessonList;

public class Timetable extends LessonList {
	public static final Logger logger = Logger.getLogger("rujel.schedule");
    
    public Object[] rowItem;
    public Integer rowIndex;
    public Integer cellIndex;
    public NSArray list;
    public int cols;
    public EduCourse course;
    protected NSArray courses;
    protected NSMutableSet toAdd = new NSMutableSet();
    protected NSMutableSet toDelete = new NSMutableSet();
    public NSTimestamp date;
    protected NSTimestamp lately;

	
	public Timetable(WOContext context) {
        super(context);
    }

    public WOElement template() {
    	course = (EduCourse)valueForBinding("course");
    	NSArray bound = (NSArray)valueForBinding("courses");
    	if(bound != courses) {
    		courses = bound;
    		list = null;
    	}
    	if(date == null || Various.boolForObject(valueForBinding("readOnly"))) {
    		NSTimestamp newDate = (NSTimestamp)valueForBinding("date");
    		if(newDate == null)
    			newDate = (NSTimestamp)session().valueForKey("today");
    		if(date == null || !date.equals(newDate)) {
    			setDate(newDate);
    			list = null;
    		}
    	}
    	if(list == null)
    		list = sequence();
    	return super.template();
    }
    
    public void setDate(NSTimestamp newDate) {
    	date = newDate;
    	lately = date.timestampByAddingGregorianUnits(0, 0, -cols, 0, 0, 0);
    }

    protected NSArray sequence() {
    	EOEditingContext ec = (EOEditingContext)valueForBinding("ec");
    	int week = SettingsBase.numericSettingForCourse("EduPeriod", course, ec,7);
		int weekStart = SettingsBase.numericSettingForCourse(
				"weekStart", course, ec, Calendar.MONDAY);
    	NSMutableArray result = new NSMutableArray(week);
    	cols = week +1;
		String[] titles = new String[cols];
    	if(week%7 == 0) {
    		NSArray weekDays = (NSArray)session().valueForKeyPath((week > 7)?
    				"strings.Reusables_Strings.presets.weekdayShort":
    					"strings.Reusables_Strings.presets.weekdayLong");
    		for (int i = 1; i < cols; i++) {
    			int widx = (weekStart -2 +i)%7;
    			String weekday = (String)weekDays.objectAtIndex(widx);
    			if(week > 7) {
    				StringBuilder buf = new StringBuilder(weekday);
    				buf.append(' ').append((i-1)/7 +1);
    				weekday = buf.toString();
    			}
    			titles[i] = weekday;
    		}
    	} else {
    		for (int i = 1; i < cols; i++) {
    			titles[i] = Integer.toString(i);
			}
    	}
    	result.addObject(titles);
    	Integer timeScheme = SettingsBase.numericSettingForCourse("timeScheme", course, ec);
    	EOQualifier qual = (timeScheme == null)? null : new EOKeyValueQualifier("timeScheme",
    			EOQualifier.QualifierOperatorEqual, new Integer(week));
    	EOFetchSpecification fs = new EOFetchSpecification(
    			"ScheduleRing", qual, MyUtility.numSorter);
    	NSArray found = ec.objectsWithFetchSpecification(fs);
    	if(found != null && found.count() > 0) {
    		titles[0] = (String)session().valueForKeyPath("strings.RujelSchedule_Schedule.Rings");
    		Enumeration enu = found.objectEnumerator();
    		SimpleDateFormat df = new SimpleDateFormat("HH:mm");
    		while (enu.hasMoreElements()) {
				EOEnterpriseObject ring = (EOEnterpriseObject) enu.nextElement();
				StringBuilder buf = new StringBuilder();
				NSTimestamp time = (NSTimestamp)ring.valueForKey("startTime");
				buf.append(df.format(time)).append(" - ");
				time = (NSTimestamp)ring.valueForKey("endTime");
				buf.append(df.format(time));
				Object[] row = new Object[cols];
				row[0] = buf.toString();
				result.addObject(row);
			}
    		
    	}
    	EOQualifier[] quals = new EOQualifier[2];
    	if(courses == null || courses.count() == 0) {
    		quals[0] = new EOKeyValueQualifier(ScheduleEntry.FLAGS_KEY,
    				EOQualifier.QualifierOperatorEqual,new Integer(0)); // NOT temporary
        	quals[1] = ScheduleEntry.onDate(date); // OR recantly actual
        	quals[1] = new EOOrQualifier(new NSArray(quals));

    		quals[0] = new EOKeyValueQualifier("course",
    				EOQualifier.QualifierOperatorEqual,course); // for current course
        	quals[0] = new EOAndQualifier(new NSArray(quals));
   	} else {
    		if(course != null && !courses.containsObject(course))
    			courses = courses.arrayByAddingObject(course);
    		quals[0] = Various.getEOInQualifier("course", courses); // for courses in list
        	quals[1] = ScheduleEntry.onDate(date); // AND recently actual
        	quals[0] = new EOAndQualifier(new NSArray(quals));
    	}
    	fs = new EOFetchSpecification(ScheduleEntry.ENTITY_NAME,quals[0],ScheduleEntry.tableSorter);
    	found = ec.objectsWithFetchSpecification(fs);
    	if(found == null || found.count() == 0)
    		return result;
    	Enumeration enu = found.objectEnumerator();
    	Object[] row = null;
    	NSArray sorter = null;
    	if(Various.boolForObject(valueForBinding("showSubject")) || 
    			Various.boolForObject(valueForBinding("showGroupName"))) {
    		sorter = new NSArray(new Object[] {
    			new EOSortOrdering ("course.cycle",EOSortOrdering.CompareAscending),
    			new EOSortOrdering ("course.eduGroup",EOSortOrdering.CompareAscending),
    			new EOSortOrdering ("course.comment",EOSortOrdering.CompareAscending)
    		});
    	} else {
    		sorter = new NSArray(new EOSortOrdering (
    				ScheduleEntry.VALID_SINCE_KEY,EOSortOrdering.CompareAscending));
    	}
    	while (enu.hasMoreElements()) {
			ScheduleEntry schdl = (ScheduleEntry) enu.nextElement();
			int num = schdl.num().intValue();
			while (num > result.count() -1) {
				row = new Object[cols];
				result.addObject(row);
			}
			row = (Object[])result.objectAtIndex(num);
			num = schdl.weekdayNum().intValue();
			num = num - weekStart +1;
			if(num < 1)
				num += week;
			while (num > week) {
				num -= week;
			}
			EduCourse crs = schdl.course();
			if(row[num] == null) {
				row[num] = new NSMutableArray(schdl);
			} else if(crs == course || findCourseInArray((NSMutableArray)row[num], crs) == null) {
				((NSMutableArray)row[num]).addObject(schdl);
				EOSortOrdering.sortArrayUsingKeyOrderArray((NSMutableArray)row[num], sorter);
			}
		}
    	return result;
    }
    
    private ScheduleEntry findCourseInArray(NSArray array, EduCourse crs) {
    	if(array == null || array.count() == 0)
    		return null;
    	Enumeration enu = array.objectEnumerator();
    	while (enu.hasMoreElements()) {
			ScheduleEntry sdl = (ScheduleEntry) enu.nextElement();
			if(sdl.course() == crs)
				return sdl;
		}
    	return null;
    }
    
    public Object value() {
    	if(rowItem == null || cellIndex == null ||
    			cellIndex.intValue() < 0 || cellIndex.intValue() >= rowItem.length)
    		return null;
    	return rowItem[cellIndex.intValue()];
    }
    
    public Integer cellID() {
    	if(rowIndex == null || cellIndex == null)
    		return null;
    	int result = cellIndex.intValue()*100 + rowIndex.intValue();
    	return new Integer(result);
    }
    
    public String cellValue() {
    	Object value = value();
    	if(value == null)
    		return null;
    	if(value instanceof String)
    		return (String)value;
    	if(value instanceof NSMutableArray) {
    		boolean showSubject = Various.boolForObject(valueForBinding("showSubject"));
    		boolean showGroupName = Various.boolForObject(valueForBinding("showGroupName"));
    		NSMutableArray array = (NSMutableArray)value;
    		Enumeration enu = array.objectEnumerator();
    		StringBuilder buf = new StringBuilder();
    		EduCourse prev = null;
    		int count = 0;
    		Format format = (courses != null)? null :
    			new SimpleDateFormat(SettingsReader.stringForKeyPath("ui.shortDateFormat","dd.MM"));
//    		boolean bracket = false;
    		while (enu.hasMoreElements()) {
    			ScheduleEntry sdl = (ScheduleEntry)enu.nextElement();
    			if(courses == null) {
    				if(buf.length() > 0)
    					buf.append(',').append(' ');
    				NSTimestamp since = sdl.validSince();
    				NSTimestamp to = sdl.validTo();
    				buf.append("<span");
    				if(since != null || to != null) {
    					buf.append(" style = \"white-space:nowrap;");
    					if(to != null && to.before(date)) {
    						buf.append("color:#666666;");
    					} else if(since != null) {
    						if(since.after(date))
    							buf.append("color:#66cc66;");
    						else if(since.after(lately))
    							buf.append("font-weight:bold;");
    					}
    					buf.append('"');
    				}
    				buf.append('>');
    				if(since != null && to == null) {
    					buf.append(session().valueForKeyPath(
    							"strings.Reusables_Strings.dataTypes.since")).append(' ');
    				}
    				if(since != null) {
    					buf.append(format.format(since));
    					if(to != null)
    						buf.append(' ').append('-');
    				} else {
    					buf.append(session().valueForKeyPath("strings.RujelBase_Base.before"));
    				}
    				if(to != null) {
    					buf.append(' ').append(format.format(to));
    				} else if(since == null) {
    					buf.append("###");
    				}
    				buf.append("</span>");
    				continue;
    			}
    			boolean recent = (sdl.validSince() != null && sdl.validSince().after(lately));
				EduCourse crs = sdl.course();
				if(prev == null || prev.cycle() != crs.cycle()) { // other cycle
					if(count > 1)
						buf.append('*').append(count);
					if(prev != null)
						buf.append(" / ");
					if(showSubject) {  // show subjects
						if(recent)
							buf.append("<strong>");
						buf.append(WOMessage.stringByEscapingHTMLString(crs.cycle().subject()));
						if(recent)
							buf.append("</strong>");
					} // show subjects
					prev = null;
					count = 0;
				}  // other cycle
				if(showGroupName) { // show group names
					if(prev == null || prev.eduGroup() != crs.eduGroup()) {
//						if(bracket)
//		    				buf.append(")</em> ");
	    				if(count > 1)
	    					buf.append('*').append(count);
	    				if(prev == null) {
	    					if(showSubject)
	    						buf.append('-');
	    				} else {
	    					if(showSubject)
	    						buf.append(',');
	    					else
	    						buf.append(" / ");
	    				}
	    				if(recent)
	    					buf.append("<strong>");
		    			buf.append(WOMessage.stringByEscapingHTMLString(crs.eduGroup().name()));
	    				if(recent)
	    					buf.append("</strong>");
		    			count = 1;
					} else {
						count++;
					}
	    		} else {
	    			count++;
	    		}
	    		// end show group names
	    			/*if(crs.comment() != null) {
	    				if(bracket) {
	    					buf.append(',').append(' ');
	    				} else {
	    					bracket = true;
	    					buf.append(" <em>(");
	    				}
	    				buf.append(WOMessage.stringByEscapingHTMLString(crs.comment()));
	    			}*/
	    		prev = crs;
			} // courses enumeration
//			if(bracket)
//				buf.append(")</em> ");
			if(count > 1)
				buf.append('*').append(count);
    		return buf.toString();
    	} // if(value instanceof NSMutableArray)
    	return null;
    }
    
    public String cellClass() {
    	if(cellIndex.intValue() == 0)
    		return "orange";
    	Object value = value();
    	if(value == null)
    		return "grey";
    	if(value instanceof String)
    		return "orange";
    	if(value instanceof NSMutableArray) {
    		NSMutableArray array = (NSMutableArray)value;
    		if(course != null) {
        		Enumeration enu = array.objectEnumerator();
				String result = null;
        		while (enu.hasMoreElements()) {
    				ScheduleEntry sdl = (ScheduleEntry) enu.nextElement();
    				if(sdl.course() == course) {
    					result = "grey";
    					if(sdl.isActual(date))
    						return "selection";
    					if(sdl.validSince() != null && sdl.validSince().after(date))
    						result = "green";
    				}
    			}
        		if(result != null)
        			return result;
    		}
    		ScheduleEntry sdl = (ScheduleEntry)array.objectAtIndex(0);
    		NSArray audience = (NSArray)sdl.valueForKeyPath("course.audience");
    		boolean subgroup = (audience != null && audience.count() > 0);
    		for (int i = 1; i < array.count(); i++) {
    			sdl = (ScheduleEntry)array.objectAtIndex(i);
    			if(!subgroup)
    				return "highlight";
    			audience = (NSArray)sdl.valueForKeyPath("course.audience");
    			if(audience == null || audience.count() == 0)
    				return "highlight";
			}
    		return (subgroup)?"gerade":"ungerade";
    	}
		return "grey";
    }

    public Boolean checked() {
    	Integer id = cellID();
    	if(id == null) return null;
    	if(toAdd.containsObject(id))
    		return Boolean.TRUE;
    	if(toDelete.containsObject(id))
    		return Boolean.FALSE;
    	Object value = value();
    	if(value instanceof NSMutableArray) {
    		NSMutableArray array = (NSMutableArray)value;
    		Enumeration enu = array.objectEnumerator();
    		while (enu.hasMoreElements()) {
				ScheduleEntry sdl = (ScheduleEntry) enu.nextElement();
				if(sdl.course() == course && sdl.isActual(date))
					return Boolean.TRUE;
			}
    	}
    	return Boolean.FALSE;
    }
    
    public void setChecked(Boolean checked) {
    	Integer id = cellID();
    	if(checked.booleanValue()) {
    		toDelete.removeObject(id);
    	} else {
    		toAdd.removeObject(id);
    	}
    	Object value = value();
    	if(value instanceof NSMutableArray) {
    		NSMutableArray array = (NSMutableArray)value;
    		Enumeration enu = array.objectEnumerator();
    		while (enu.hasMoreElements()) {
				ScheduleEntry sdl = (ScheduleEntry) enu.nextElement();
				if(sdl.course() == course && sdl.isActual(date)) {
					if(!checked.booleanValue())
						toDelete.addObject(id);
					return;
				}
			}
    		if(checked.booleanValue()) {
        		toAdd.addObject(id);
        	}
    	} else if(checked.booleanValue()) {
    		toAdd.addObject(id);
    	}
    }
    
    public WOActionResults submit() {
    	NSArray newCells = context().request().formValuesForKey("newCells");
    	if(newCells != null && newCells.count() > 0) {
    		Enumeration enu = newCells.objectEnumerator();
    		while (enu.hasMoreElements()) {
				String idString = (String) enu.nextElement();
				try {
					toAdd.addObject(Integer.parseInt(idString));
				} catch (Exception e) {
					logger.log(WOLogLevel.WARNING,"Error parcing cellId",
							new Object[] {session(),idString,e});
				}
			}
    	}
    	if(toAdd.count() == 0 && toDelete.count() == 0)
    		return (WOActionResults)valueForBinding("noAction");
		int weekStart = SettingsBase.numericSettingForCourse(
				"weekStart", course, course.editingContext(), Calendar.MONDAY);
    	EOEditingContext ec = course.editingContext();
    	NSMutableArray pool = null;
    	{
    		EOQualifier quals[] = new EOQualifier[2];
    		quals[0] = new EOKeyValueQualifier("course",EOQualifier.QualifierOperatorEqual,course);
    		quals[1] = new EOKeyValueQualifier(ScheduleEntry.FLAGS_KEY,
    				EOQualifier.QualifierOperatorEqual,new Integer(0)); // NOT temporary
    		quals[0] = new EOAndQualifier(new NSArray(quals));
    		EOFetchSpecification fs = new EOFetchSpecification(ScheduleEntry.ENTITY_NAME,
    				quals[0],ScheduleEntry.tableSorter);
    		NSArray found = ec.objectsWithFetchSpecification(fs);
    		Enumeration enu = found.objectEnumerator();
    		NSTimestamp yesterday = date.timestampByAddingGregorianUnits(0, 0, -1, 0, 0, 0);
    		while (enu.hasMoreElements()) {
				ScheduleEntry sdl = (ScheduleEntry) enu.nextElement();
				Integer index = new Integer((sdl.weekdayNum().intValue() - weekStart +1)*100 
						+ sdl.num().intValue());
				if(toDelete.containsObject(index) && sdl.isActual(date)) {
					if(sdl.isTemporary() ||
							(sdl.validSince() != null && sdl.validSince().after(yesterday))) {
						if(pool == null)
							pool = new NSMutableArray(sdl);
						else
							pool.addObject(sdl);
					} else {
						sdl.setValidTo(yesterday);
					}
				} else if(toAdd.containsObject(index)){
					NSTimestamp valid = sdl.validSince();
					if(valid != null && valid.after(date)) {
						sdl.setValidSince(date);
						toAdd.removeObject(index);
					} else {
						valid = sdl.validTo();
						if(valid != null && valid.before(date) && valid.after(lately)) {
							sdl.setValidTo(null);
							toAdd.removeObject(index);
						}
					}
				}
			}
    	} // delete
    	if(toAdd.count() > 0) { // add
    		Enumeration enu = toAdd.objectEnumerator();
    		while (enu.hasMoreElements()) {
				Integer index = (Integer) enu.nextElement();
				ScheduleEntry sdl = null;
				if(pool != null) {
					sdl = (ScheduleEntry)pool.removeLastObject();
					if(pool.count() == 0)
						pool = null;
					sdl.setValidTo(null);
					sdl.setFlags(new Integer(0));
				} else {
					sdl = (ScheduleEntry)EOUtilities.createAndInsertInstance(
							ec, ScheduleEntry.ENTITY_NAME);
					sdl.setCourse(course);
				}
				int num = index.intValue()/100 + weekStart -1;
				sdl.setWeekdayNum(new Integer(num));
				num = index.intValue()%100;
				sdl.setNum(new Integer(num));
				sdl.setValidSince(date);
			}
    	}
    	if(pool != null) { // delete from pool
    		Enumeration enu = pool.objectEnumerator();
    		while (enu.hasMoreElements()) {
				ScheduleEntry sdl = (ScheduleEntry) enu.nextElement();
				ec.deleteObject(sdl);
			}
    	}
    	if(ec.hasChanges()) {
    		try {
				ec.saveChanges();
				logger.log(WOLogLevel.EDITING,"Saved schedule for course",
						 new Object[] {session(),course});
			} catch (Exception e) {
				ec.revert();
				logger.log(WOLogLevel.WARNING,"Error saving schedule",
						 new Object[] {session(),course,e});
				session().takeValueForKey(e.getMessage(), "message");
			}
    	}
    	toAdd.removeAllObjects();
    	toDelete.removeAllObjects();
    	list = null;
		return (WOActionResults)valueForBinding("withAction");
    }
    
    public Boolean disabled() {
    	if(Various.boolForObject(valueForBinding("readOnly")))
    		return Boolean.TRUE;
    	if(cellIndex == null || cellIndex < 1)
    		return Boolean.TRUE;
    	Object value = value();
    	if(value instanceof String)
    		return Boolean.TRUE;
    	return Boolean.FALSE;
    }
    
    public boolean isStateless() {
    	return Various.boolForObject(valueForBinding("readOnly"));
    }
    
    public boolean synchronizesVariablesWithBindings() {
        return false;
	}
}