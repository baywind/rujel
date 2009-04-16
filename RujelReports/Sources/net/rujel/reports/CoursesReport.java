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

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.util.Enumeration;
import java.util.logging.Logger;

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

		ec = new SessionedEditingContext(session());
		NSArray availableReports = (NSArray)session().valueForKeyPath("modules.CoursesReport");
		reports = PlistReader.cloneArray(availableReports, true);
        
        String reportsDirPath = SettingsReader.stringForKeyPath("reportsDir",
				"LOCALROOT/Library/WebObjects/Configuration/RujelReports");
        reportsDirPath = Various.convertFilePath(reportsDirPath);
        File reportsDir = new File(reportsDirPath, "CoursesReport");
        if (reportsDir.isDirectory()) {
        	File[] files = reportsDir.listFiles(new FileFilter() {
        		public boolean accept(File file) {
        			return (file.isFile() && file.getName().endsWith(
        			".plist"));
        		}
        	});
        	for (int i = 0; i < files.length; i++) {
        		try {
        			FileInputStream fis = new FileInputStream(files[i]);
        			NSData data = new NSData(fis, fis.available());
        			fis.close();
        			String encoding = System.getProperty(
        					"PlistReader.encoding", "utf8");
        			Object plist = NSPropertyListSerialization
        							.propertyListFromData(data, encoding);
        			if(plist instanceof NSDictionary) {
        				checkInDict((NSDictionary)plist);
        			} else if (plist instanceof NSArray) {
        				Enumeration enu = ((NSArray)plist).objectEnumerator();
        				while (enu.hasMoreElements()) {
							NSDictionary dict = (NSDictionary) enu.nextElement();
							checkInDict(dict);
						}
        			}
        		} catch (Exception e) {
        			Object [] args = new Object[] {session(),e,files[i].getAbsolutePath()};
        			Logger.getLogger("rujel.reports").log(WOLogLevel.WARNING,
        					"Error reading CoursesReport plist",args);
        		}
        	}
        	EOSortOrdering.sortArrayUsingKeyOrderArray(reports, ModulesInitialiser.sorter);
        }
        //prepareDisplay();
        modifyList();
    }
    
    protected void checkInDict(NSDictionary dict) {
		NSArray checkAccess = (NSArray)dict.valueForKey("checkAccess");
		if(checkAccess != null && checkAccess.count() > 0) {
			NSKeyValueCodingAdditions readAccess = 
				(NSKeyValueCodingAdditions)session().valueForKey("readAccess");
			Enumeration enu = checkAccess.objectEnumerator();
			while (enu.hasMoreElements()) {
				String acc = (String) enu.nextElement();
				if(Various.boolForObject(
						readAccess.valueForKeyPath("_read." + acc)))
					return;
			}
		}
		reports.addObject(dict);    	
    }
    
    protected void prepareDisplay() {
        display = new NSMutableArray(defaultDisplay.valueForKey("subject"));
        if((curSource==null)?tabindex == 0:curSource instanceof EduGroup)
        	display.addObject(defaultDisplay.valueForKey("teacher"));
        else
        	display.addObject(defaultDisplay.valueForKey("eduGroup"));
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
		prepareDisplay();
		if(item != null) {
			reports.takeValueForKey(Boolean.FALSE, "active");
			item.takeValueForKey(Boolean.TRUE, "active");
			NSArray subs = (NSArray)item.valueForKey("subParams");
			if(subs != null)
				subs.takeValueForKey(Boolean.TRUE, "active");
			display.addObject(item);
			return;
		}
		EOSortOrdering.sortArrayUsingKeyOrderArray(reports, ModulesInitialiser.sorter);
		Enumeration enu = reports.objectEnumerator();
		while (enu.hasMoreElements()) {
			NSMutableDictionary rDict = (NSMutableDictionary) enu.nextElement();
			NSMutableArray sub = (NSMutableArray) rDict.valueForKey("subParams");
			if(sub != null)
				EOSortOrdering.sortArrayUsingKeyOrderArray(sub, ModulesInitialiser.sorter);
			if(!Various.boolForObject(rDict.valueForKey("active")))
				continue;
			DisplayAny.ValueReader.clearResultCache(rDict, null, true);
			rDict = rDict.mutableClone();
			display.addObject(rDict);
			if(sub != null) {
				Enumeration subEnu = sub.objectEnumerator();
				sub = new NSMutableArray();
				while (subEnu.hasMoreElements()) {
					NSMutableDictionary sDict = (NSMutableDictionary) subEnu.nextElement();
					if(Various.boolForObject(sDict.valueForKey("active")))
						sub.addObject(sDict);
				}
				rDict.takeValueForKey(sub, "subParams");
			}
		}
	}

	public String subRowStyle() {
		if(item == null || Various.boolForObject(item.valueForKey("active")))
			return null;
		return "display:none;";
	}

	public String tableStyle() {
		if(display != null && display.count() > 2)
			return "display:none;";
		return null;
	}
}