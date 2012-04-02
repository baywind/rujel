// ImportExport.java

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

package net.rujel.io;

import java.util.Enumeration;

import com.webobjects.appserver.WOActionResults;
import com.webobjects.appserver.WOComponent;
import com.webobjects.appserver.WOContext;
import com.webobjects.appserver.WOResponse;
import com.webobjects.eocontrol.EOEditingContext;
import com.webobjects.eocontrol.EOKeyValueQualifier;
import com.webobjects.eocontrol.EOQualifier;
import com.webobjects.eocontrol.EOSortOrdering;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSDictionary;
import com.webobjects.foundation.NSKeyValueCoding;
import com.webobjects.foundation.NSMutableArray;
import com.webobjects.foundation.NSMutableDictionary;

import net.rujel.base.MyUtility;
import net.rujel.base.XMLGenerator;
import net.rujel.reports.ReporterSetup;
import net.rujel.reports.ReportsModule;
import net.rujel.reusables.SessionedEditingContext;
import net.rujel.reusables.Various;
import net.rujel.reusables.WOLogFormatter;
import net.rujel.ui.LessonList;

public class ImportExport extends LessonList {

	public NSMutableArray importList;
    public NSMutableArray exportList;
    public NSKeyValueCoding item;
    public NSMutableDictionary reporter;
    public EOEditingContext ec;
    
    public ImportExport(WOContext context) {
        super(context);
		EOQualifier qual = new EOKeyValueQualifier("id",
				EOQualifier.QualifierOperatorNotEqual, null);
        NSArray reports = ReportsModule.reportsFromDir("ImportExport", context.session(), qual);
        if(reports == null || reports.count() == 0)
        	return;
        importList = new NSMutableArray();
        exportList = new NSMutableArray();
        Enumeration enu = reports.objectEnumerator();
        while (enu.hasMoreElements()) {
			NSDictionary dict = (NSDictionary) enu.nextElement();
			String type = (String)dict.valueForKey("type");
			if("import".equals(type))
				Various.addToSortedList(dict, importList, "sort", EOSortOrdering.CompareAscending);
			else if("export".equals(type))
				Various.addToSortedList(dict, exportList, "sort", EOSortOrdering.CompareAscending);
		}
        ec = new SessionedEditingContext(context.session());
    }

//    public boolean isCurrent() {
//    	return item == current;
//    }
    
    public WOActionResults useItem() {
		WOComponent result = pageWithName("ReporterSetup");
		result.takeValueForKey(this, "returnPage");
		result.takeValueForKey(item, "reporter");
		return result;
    }
    
    public WOActionResults export() {
    	NSMutableDictionary reportDict = new NSMutableDictionary();
		reportDict.takeValueForKey(reporter,"reporter");
		NSMutableDictionary info = new NSMutableDictionary(MyUtility.presentEduYear(
				(Integer)session().valueForKey("eduYear")), "eduYear");
		reportDict.takeValueForKey(info, "info");
		info = (NSMutableDictionary)reporter.valueForKey("settings");
		if(info == null) {
			info = ReporterSetup.getDefaultSettings((NSDictionary)reporter,
					ReportsModule.reportsFolder("StudentReports"));
			reporter.takeValueForKey(info, "settings");
		}
		byte[] result = null;
		try {
			result = XMLGenerator.generate(session(), (NSDictionary)reportDict);
		} catch (Exception e) {
			result = WOLogFormatter.formatTrowable(e).getBytes();
		}
		WOResponse response = application().createResponseInContext(context());
		response.setContent(result);
		String contentType = (String)reporter.valueForKey("ContentType");
		if(contentType == null)
			contentType = "application/octet-stream";
		response.setHeader(contentType,"Content-Type");
		StringBuilder buf = new StringBuilder("attachment; filename=\"");
		buf.append("filename");
		contentType = (String)reporter.valueForKey("filext");
		if(contentType != null) {
			if(contentType.charAt(0) != '.')
				buf.append('.');
			buf.append(contentType);
		}
		buf.append('"');
		response.setHeader(buf.toString(),"Content-Disposition");
		reporter = null;
		return response;
    }
    
    public String onLoad() {
    	if(reporter != null)
    		return "if(tryLoad())window.open('globalActionUrl')";
    	return null;
    }
}