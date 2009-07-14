//  Holiday.java

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

package net.rujel.eduplan;

import java.util.Date;
import java.util.Enumeration;

import net.rujel.base.MyUtility;
import net.rujel.interfaces.EOPeriod;

import com.webobjects.foundation.*;
import com.webobjects.eocontrol.*;

public class Holiday extends _Holiday implements EOPeriod {

	public static void init() {
	}

	public void awakeFromInsertion(EOEditingContext ec) {
		super.awakeFromInsertion(ec);
	}
	
	public void validateForSave() {
		if(end() == null)
			setEnd(begin());
		super.validateForSave();
	}
	
	public int days() {
		return MyUtility.countDays(begin(), end());
	}
	
	public static NSArray holidaysInDates(NSTimestamp since, NSTimestamp to, 
			EOEditingContext ec, String listName) {
		EOQualifier[] quals = new EOQualifier[2];
		quals[0] = new EOKeyValueQualifier(END_KEY,EOQualifier.
				QualifierOperatorGreaterThanOrEqualTo,since);
		quals[1] = new EOKeyValueQualifier(BEGIN_KEY,EOQualifier.
				QualifierOperatorLessThanOrEqualTo,to);
		EOQualifier qual = new EOAndQualifier(new NSArray(quals));
		quals[0] = new EOKeyValueQualifier(LIST_NAME_KEY,
				EOQualifier.QualifierOperatorEqual,NullValue);
		if(listName != null) {
			quals[1] = new EOKeyValueQualifier(LIST_NAME_KEY,
					EOQualifier.QualifierOperatorEqual,listName);
			quals[0] = new EOOrQualifier(new NSArray(quals));
		}
		quals[1] = qual;
		qual = new EOAndQualifier(new NSArray(quals));
		EOFetchSpecification fs = new EOFetchSpecification(ENTITY_NAME,qual,EduPeriod.sorter);
		return ec.objectsWithFetchSpecification(fs);
	}
	
	public static int freeDaysInDates(NSTimestamp since, NSTimestamp to, 
			EOEditingContext ec, String listName) {
		NSArray list = holidaysInDates(since, to, ec, listName);
		if(list == null || list.count() == 0)
			return 0;
		Enumeration enu = list.objectEnumerator();
		int days = 0;
		while (enu.hasMoreElements()) {
			Holiday hd = (Holiday) enu.nextElement();
			NSTimestamp begin = hd.begin();
			if(begin.compare(since) < 0)
				begin = since;
			NSTimestamp end = hd.end();
			if(end.compare(to) > 0)
				end = to;
			days += MyUtility.countDays(begin, end);
		}
		return days;
	}

	public boolean contains(Date date) {
		if (date.compareTo(begin()) >= 0 && date.compareTo(end()) <= 0)
			return true;
		
		return false;
	}

	public String name() {
		return holidayType().name();
	}

}
