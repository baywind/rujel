// CoursesReport.java: Class file for WO Component 'CoursesReport'

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

package net.rujel.reports;

import net.rujel.interfaces.EduCourse;
import net.rujel.interfaces.EduGroup;
import net.rujel.interfaces.Teacher;
import net.rujel.reusables.*;

import com.webobjects.appserver.*;
import com.webobjects.eoaccess.EOUtilities;
import com.webobjects.eocontrol.EOEditingContext;
import com.webobjects.eocontrol.EOSortOrdering;
import com.webobjects.foundation.*;

// Generated by the WOLips Templateengine Plug-in at Apr 10, 2009 5:37:55 PM
public class CoursesReport extends com.webobjects.appserver.WOComponent {
	public EOEditingContext ec;
	public NSMutableArray reports;
	public NSMutableArray display;
	protected  NSDictionary defaultDisplay = (NSDictionary)application().valueForKeyPath(
		"strings.RujelReports_Reports.CoursesReport.defaultDisplay");

	public NSArray courses;
	public Object curSource;

	public NSArray tablist = (NSArray)valueForKeyPath(
			"application.strings.Strings.SrcMark.tabs");
	public int tabindex = 0;
	
	public NSKeyValueCoding item;
	public NSKeyValueCoding subItem;
	
    public CoursesReport(WOContext context) {
        super(context);

		ec = new SessionedEditingContext(context.session());
		NSArray availableReports = (NSArray)session().valueForKeyPath("modules.CoursesReport");
		reports = PlistReader.cloneArray(availableReports, true);
        
		reports.addObjectsFromArray(ReportsModule.reportsFromDir("CoursesReport",context));
        //prepareDisplay();
        //modifyList();
    }
    
    public NSMutableArray prepareDisplay() {
    	NSMutableArray forceDisplay = new NSMutableArray(defaultDisplay.valueForKey("subject"));
        if((curSource==null)?tabindex == 0:curSource instanceof EduGroup)
        	forceDisplay.addObject(defaultDisplay.valueForKey("teacher"));
        else
        	forceDisplay.addObject(defaultDisplay.valueForKey("eduGroup"));
        return forceDisplay;
    }
    
    public String title() {
		return (String)application().valueForKeyPath(
				"strings.RujelReports_Reports.CoursesReport.title");
	}
    
	public void appendToResponse(WOResponse aResponse, WOContext aContext) {
		super.appendToResponse(aResponse, aContext);
	}

	public String tabSelected() {
		if(tabindex == NSArray.NotFound) return null;
		try {
			return (String)tablist.objectAtIndex(tabindex);
		} catch (Exception ex) {
			return null;
		}
	}
		
	public void setTabSelected(String tabName) {
		tabindex = tablist.indexOfObject(tabName);
		if(curSource == null) {
        	display.replaceObjectAtIndex(defaultDisplay.valueForKey(
        			(tabindex == 0)?"teacher":"eduGroup"),1);
 		}
	}

	public void setCurSource(Object newSource) {
		curSource = newSource;
		NSMutableDictionary values = new NSMutableDictionary(
				session().valueForKey("eduYear"),"eduYear");
		if (curSource instanceof Teacher) {
			values.takeValueForKey(curSource, "teacher");
		} else if(curSource instanceof EduGroup) {
			values.takeValueForKey(curSource, "eduGroup");
		}
		courses = EOUtilities.objectsMatchingValues(ec, EduCourse.entityName, values);
		if(courses != null && courses.count() > 1) {
			courses = EOSortOrdering.sortedArrayUsingKeyOrderArray(courses, EduCourse.sorter);  
		}
		display.replaceObjectAtIndex(defaultDisplay.valueForKey(
				(curSource instanceof EduGroup)?"teacher":"eduGroup"),1);
	}
	
	public void modifyList() {
		display = prepareDisplay();
		display.addObjectsFromArray(PropSelector.prepareActiveList(reports));
	}

	public String reportStyle() {
		if(display != null && display.count() > 2)
			return "display:none;";
		return null;
	}
	
	public WOActionResults export() {
		WOComponent exportPage = pageWithName("ReportTable");
		exportPage.takeValueForKey(courses, "list");
		exportPage.takeValueForKey(display, "properties");
 		exportPage.takeValueForKey("'CoursesReport'yyMMdd", "filenameFormatter");
		return exportPage;
	}
}