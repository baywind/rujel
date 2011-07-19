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

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.logging.Logger;

import com.webobjects.appserver.WOActionResults;
import com.webobjects.appserver.WOContext;
import com.webobjects.appserver.WOElement;
import com.webobjects.appserver.WOMessage;
import com.webobjects.eoaccess.EOUtilities;
import com.webobjects.eocontrol.EOAndQualifier;
import com.webobjects.eocontrol.EOEditingContext;
import com.webobjects.eocontrol.EOEnterpriseObject;
import com.webobjects.eocontrol.EOFetchSpecification;
import com.webobjects.eocontrol.EOKeyValueQualifier;
import com.webobjects.eocontrol.EOQualifier;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSMutableArray;
import com.webobjects.foundation.NSMutableSet;
import com.webobjects.foundation.NSTimestamp;

import net.rujel.base.MyUtility;
import net.rujel.base.SettingsBase;
import net.rujel.interfaces.EduCourse;
import net.rujel.interfaces.EduCycle;
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
    	if(list == null)
    		list = sequence();
    	return super.template();
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
    		quals[0] = new EOKeyValueQualifier("course",
    				EOQualifier.QualifierOperatorEqual,course);
    	} else {
    		if(course != null && !courses.containsObject(course))
    			courses = courses.arrayByAddingObject(course);
    		quals[0] = Various.getEOInQualifier("course", courses);
    	}
    	quals[1] = ScheduleEntry.onDate((NSTimestamp)session().valueForKey("today"));
    	quals[0] = new EOAndQualifier(new NSArray(quals));
    	fs = new EOFetchSpecification(ScheduleEntry.ENTITY_NAME,quals[0],ScheduleEntry.tableSorter);
    	found = ec.objectsWithFetchSpecification(fs);
    	if(found == null || found.count() == 0)
    		return result;
    	Enumeration enu = found.objectEnumerator();
    	Object[] row = null;
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
			if(row[num] == null)
				row[num] = new NSMutableArray(crs);
			else if(!((NSMutableArray)row[num]).containsObject(crs))
				((NSMutableArray)row[num]).addObject(crs);
		}
    	return result;
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
    		if(!showGroupName && !showSubject)
    			return "###";
    		NSMutableArray array = (NSMutableArray)value;
    		Enumeration enu = array.objectEnumerator();
    		StringBuilder buf = new StringBuilder();
    		int[] counter = null;
    		NSMutableArray cycles = null;
    		while (enu.hasMoreElements()) {
				EduCourse crs = (EduCourse) enu.nextElement();
	    		if(showGroupName) { // show group names
					if(buf.length() > 0)
						buf.append(" / ");
	    			buf.append(WOMessage.stringByEscapingHTMLString(crs.eduGroup().name()));
	    			if(crs.comment() != null) {
	    				buf.append(" <em>(");
	    				buf.append(WOMessage.stringByEscapingHTMLString(crs.comment()));
	    				buf.append(")</em>");
	    			}
	    		}
	    		if(showSubject) {  // show subjects
	    			EduCycle cycle = crs.cycle();
	    			if(cycles == null) {
	    				cycles = new NSMutableArray(cycle);
	    				counter = new int[array.count()];
	    				counter[0] = 1;
	    			} else {
	    				int idx = cycles.indexOfObject(cycle);
	    				if(idx < 0) {
	    					counter[cycles.count()] = 1;
	    					cycles.addObject(cycle);
	    				} else {
	    					counter[idx]++;
	    				}
	    			}
	    		} // end show subject
			} // courses enumeration
    		if(cycles != null) {
    			for (int i = 0; i < cycles.count(); i++) {
    				if(buf.length() > 0)
    					buf.append(" / ");
					EduCycle cycle = (EduCycle)cycles.objectAtIndex(i);
					buf.append(WOMessage.stringByEscapingHTMLString(cycle.subject()));
					if(counter[i] > 1)
						buf.append('*').append(counter[i]);
				}
    		}
    		return buf.toString();
    	}
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
    		if(course != null && array.containsObject(course))
    			return "selection";
    		EduCourse crs = (EduCourse)array.objectAtIndex(0);
    		NSArray audience = (NSArray)crs.valueForKey("audience");
    		boolean subgroup = (audience != null && audience.count() > 0);
    		for (int i = 1; i < array.count(); i++) {
    			crs = (EduCourse)array.objectAtIndex(i);
    			if(!subgroup)
    				return "highlight";
    			audience = (NSArray)crs.valueForKey("audience");
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
    		return Boolean.valueOf(array.containsObject(course));
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
    		if(array.containsObject(course)) {
    			if(!checked.booleanValue())
    				toDelete.addObject(id);
    		} else if(checked.booleanValue()) {
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
		NSTimestamp today = (NSTimestamp)session().valueForKey("today");
    	if(toDelete.count() > 0) {
    		EOQualifier quals[] = new EOQualifier[2];
    		quals[0] = new EOKeyValueQualifier("course",EOQualifier.QualifierOperatorEqual,course);
    		quals[1] = ScheduleEntry.onDate(today);
    		quals[0] = new EOAndQualifier(new NSArray(quals));
    		EOFetchSpecification fs = new EOFetchSpecification(ScheduleEntry.ENTITY_NAME,
    				quals[0],ScheduleEntry.tableSorter);
    		NSArray found = ec.objectsWithFetchSpecification(fs);
    		Enumeration enu = found.objectEnumerator();
    		NSTimestamp yesterday = today.timestampByAddingGregorianUnits(0, 0, -1, 0, 0, 0);
    		while (enu.hasMoreElements()) {
				ScheduleEntry sdl = (ScheduleEntry) enu.nextElement();
				Integer index = new Integer((sdl.weekdayNum().intValue() - weekStart +1)*100 
						+ sdl.num().intValue());
				if(!toDelete.contains(index))
					continue;
				if(sdl.isTemporary() ||
						( sdl.validSince() != null && sdl.validSince().after(yesterday))) {
					if(pool == null)
						pool = new NSMutableArray(sdl);
					else
						pool.addObject(sdl);
				} else {
					sdl.setValidTo(yesterday);
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
				sdl.setValidSince(today);
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