// PrintReport.java: Class file for WO Component 'PrintReport'

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

package net.rujel.ui;

import net.rujel.base.MyUtility;
import net.rujel.interfaces.*;
import net.rujel.reusables.*;
import com.webobjects.foundation.*;
import com.webobjects.appserver.*;

public class PrintReport extends WOComponent {
	//public static final String reporter = SettingsReader.stringForKeyPath("ui.presenter.report","StudentMarks");
	
	public NSKeyValueCoding reporter; /*() {
		return reporter;
	}*/

    public PrintReport(WOContext context) {
        super(context);
    }

	public NSArray students;
	public NSTimestamp since;
	public NSTimestamp to;
    public Student studentItem;
	public NSArray courses;
	public Period period;
	public EduGroup eduGroup;
	
	public void setPeriod(Period newPeriod) {
		period = newPeriod;
		if(newPeriod == null)
			return;
		java.util.Date date = newPeriod.begin();
		if(date instanceof NSTimestamp) {
			since = (NSTimestamp)date;
		} else {
			since = new NSTimestamp(date);
		}
		date = newPeriod.end();
		if(date instanceof NSTimestamp) {
			to = (NSTimestamp)date;
		} else {
			to = new NSTimestamp(date);
		}
	}
	
	public String title() {
		StringBuffer result = new StringBuffer();
		String value = (String)reporter.valueForKey("winTitle");
		if(value != null && value.length() > 0) {
			result.append(value);
		} else {
			value = (String)reporter.valueForKey("title");
			if(value == null)
				result.append(application().valueForKeyPath("strings.Strings.PrintReport.title"));
			else {
				result.append(value).append(' ');
				result.append(application().valueForKeyPath("strings.Strings.PrintReport.marks"));
			}
		}
		if(period != null && period instanceof NSKeyValueCoding && 
				!Various.boolForObject(reporter.valueForKey("hideDates"))) {
			value = (String)((NSKeyValueCoding)period).valueForKey("name");
			if(result.length() > 0)
				result.append(" : ");
			result.append(value);
		}
		return result.toString();
	}
	
	public String eduYear() {
		Integer year = (Integer)session().valueForKey("eduYear");
		return MyUtility.presentEduYear(year);
	}

	public void appendToResponse(WOResponse aResponse, WOContext aContext) {
//		Thread t = Thread.currentThread();
//		int priority = t.getPriority();
//		t.setPriority(priority -1);
		super.appendToResponse(aResponse, aContext);
//		t.setPriority(priority);
	}
	
	public boolean allowAjax() {
		return (students.count() > 1 &&
				Various.boolForObject(reporter.valueForKey("allowAjax")));
	}
	
	public String stDivStyle() {
		StringBuilder result = new StringBuilder();
		if(students.count() > 1) {
			result.append("page-break-after:always;");
			if(Various.boolForObject(reporter.valueForKey("allowAjax")))
				result.append("display:none;");
		}
		return (result.length() == 0)?null:result.toString();
	}
	
	public String onLoad() {
		if(allowAjax())
			return "onLoad(this);";
		return null;
	}
	
	public WOActionResults loadStudent() {
		WOComponent result = pageWithName((String)reporter.valueForKey("component"));
		try {
			result.takeValueForKey(studentItem, "student");
		} catch (Exception e) {
			;
		}
		try {
			result.takeValueForKey(courses, "courses");
		} catch (Exception e) {
			;
		}
		try {
			result.takeValueForKey(since, "since");
		} catch (Exception e) {
			;
		}
		try {
			result.takeValueForKey(to, "to");
		} catch (Exception e) {
			;
		}
		try {
			result.takeValueForKey(period, "period");
		} catch (Exception e) {
			;
		}
		try {
			result.takeValueForKey(reporter, "reporter");
		} catch (Exception e) {
			;
		}
		return result;
	}
}
