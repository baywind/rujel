// ScheduleXML.java

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

import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;

import org.xml.sax.SAXException;

import com.webobjects.eocontrol.EOAndQualifier;
import com.webobjects.eocontrol.EOEditingContext;
import com.webobjects.eocontrol.EOFetchSpecification;
import com.webobjects.eocontrol.EOKeyValueQualifier;
import com.webobjects.eocontrol.EOOrQualifier;
import com.webobjects.eocontrol.EOQualifier;
import com.webobjects.eocontrol.EOSortOrdering;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSDictionary;
import com.webobjects.foundation.NSMutableArray;
import com.webobjects.foundation.NSTimestamp;

import net.rujel.base.MyUtility;
import net.rujel.base.SettingsBase;
import net.rujel.interfaces.EduCourse;
import net.rujel.reusables.Period;
import net.rujel.reusables.Various;
import net.rujel.reusables.xml.EasyGenerationContentHandlerProxy;
import net.rujel.reusables.xml.GeneratorModule;

public class ScheduleXML extends GeneratorModule {

	public ScheduleXML(NSDictionary options) {
		super(options);
	}
	
	public Integer sort() {
		return new Integer(5);
	}
	

	public void generateFor(Object object,
			EasyGenerationContentHandlerProxy handler) throws SAXException {
		if(object instanceof EduCourse)
			generateFor((EduCourse)object,handler);
	}

	public void generateFor(EduCourse course,
			EasyGenerationContentHandlerProxy handler) throws SAXException {
		if(!handler.recentElement().equals("course"))
			throw new SAXException("Should generate within course");
		{
			NSDictionary opt = (NSDictionary)settings.valueForKeyPath("reporter.settings");
			if(opt != null && !Various.boolForObject(opt.valueForKeyPath("schedule.active")))
				return;
		}
		Date since = (NSTimestamp)settings.valueForKey("since");
		Date to = (NSTimestamp)settings.valueForKey("to");
		if(since == null || to == null) {
			Period period = (Period)settings.valueForKey("period");
			if(period != null) {
				if(since == null) {
					since = period.begin();
					if(!(since instanceof NSTimestamp))
						since = new NSTimestamp(since);
				}
				if(to == null) {
					to = period.end();
					if(!(to instanceof NSTimestamp))
						to = new NSTimestamp(to);
				}
			}
		}
		boolean changes = Various.boolForObject(settings.valueForKeyPath(
				"reporter.settings.schedule.changes"));
		EOQualifier qual = new EOKeyValueQualifier("course",
				EOQualifier.QualifierOperatorEqual,course);
		EOEditingContext ec = course.editingContext();
		if(!changes) {
			NSMutableArray and = new NSMutableArray(qual);
			EOQualifier[] quals = new EOQualifier[2];
			NSTimestamp date = (NSTimestamp)to;
			if(date == null)
				date = (NSTimestamp)since;
			date = MyUtility.date(ec);
			quals[0] = new EOKeyValueQualifier(ScheduleEntry.VALID_SINCE_KEY,
					EOQualifier.QualifierOperatorEqual,null);
			quals[1] = new EOKeyValueQualifier(ScheduleEntry.VALID_SINCE_KEY,
					EOQualifier.QualifierOperatorLessThanOrEqualTo,date);
			and.addObject(new EOOrQualifier(new NSArray(quals)));
			if(since != null)
				date = (NSTimestamp)since;
			quals[0] = new EOKeyValueQualifier(ScheduleEntry.VALID_TO_KEY,
					EOQualifier.QualifierOperatorEqual,null);
			quals[1] = new EOKeyValueQualifier(ScheduleEntry.VALID_TO_KEY,
					EOQualifier.QualifierOperatorGreaterThanOrEqualTo,date);
			and.addObject(new EOOrQualifier(new NSArray(quals)));
			and.addObject(new EOKeyValueQualifier(ScheduleEntry.FLAGS_KEY, 
				EOQualifier.QualifierOperatorEqual, new Integer(0)));
			qual = new EOAndQualifier(and);
		}
		NSArray sorter = new NSArray(new EOSortOrdering[] {
				new EOSortOrdering(ScheduleEntry.WEEKDAY_NUM_KEY, EOSortOrdering.CompareAscending),
				new EOSortOrdering(ScheduleEntry.NUM_KEY, EOSortOrdering.CompareAscending)
		});
		EOFetchSpecification fs = new EOFetchSpecification(ScheduleEntry.ENTITY_NAME,qual,sorter);
		NSArray list = ec.objectsWithFetchSpecification(fs);
		if(list == null || list.count() == 0)
			return;
		handler.startElement("schedule");
		Enumeration enu = list.objectEnumerator();
		int weekStart = SettingsBase.numericSettingForCourse(
				"weekStart", course, ec, Calendar.MONDAY) -1;
		while (enu.hasMoreElements()) {
			ScheduleEntry entry = (ScheduleEntry) enu.nextElement();
			int weekday = entry.weekdayNum().intValue() - weekStart;
			handler.prepareAttribute("weekday", Integer.toString(weekday));
			handler.prepareAttribute("number", entry.num().toString());
			NSTimestamp date = entry.validSince();
			if(date != null && (changes || (to != null && date.before(to) && date.after(since))))
				handler.prepareAttribute("validSince", MyUtility.formatXMLDate(date));
			date = entry.validTo();
			if(date != null && (changes || (since != null && date.after(since) && date.before(to))))
				handler.prepareAttribute("validTo", MyUtility.formatXMLDate(date));
			if(entry.isTemporary())
				handler.prepareAttribute("flags", "temporary");
			handler.element("timeslot", null);
		}
		handler.endElement("schedule");
	}
}