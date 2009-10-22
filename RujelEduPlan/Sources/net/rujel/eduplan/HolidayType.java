//  HolidayType.java

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

import java.util.Calendar;
import java.util.GregorianCalendar;

import com.webobjects.foundation.*;
import com.webobjects.appserver.WOApplication;
import com.webobjects.eoaccess.EOUtilities;
import com.webobjects.eocontrol.*;

public class HolidayType extends _HolidayType {

	public static NSArray sorter = new NSArray(new EOSortOrdering[] {
			new EOSortOrdering(BEGIN_MONTH_KEY,EOSortOrdering.CompareAscending),
			new EOSortOrdering(BEGIN_DAY_KEY,EOSortOrdering.CompareAscending)});
	
	public void awakeFromInsertion(EOEditingContext ec) {
		super.awakeFromInsertion(ec);
	}
	
	public void validateForSave() throws NSValidation.ValidationException {
		super.validateForSave();
		int dif = endMonth().intValue() - beginMonth().intValue();
		if(dif == 0) dif = endDay().intValue() - beginDay().intValue();
		if(dif < 0) {
			String message = (String)WOApplication.application().valueForKeyPath(
					"strings.RujelEduPlan_EduPlan.messages.beginEndPeriod");
			throw new NSValidation.ValidationException(message);
		}
		int day = beginDay().intValue();
		int month = beginMonth().intValue();
		if(month > 12)
			month -= 12;
		GregorianCalendar cal = new GregorianCalendar();
		cal.set(GregorianCalendar.MONTH,month);
//		2004,month,1);
		if(day > cal.getActualMaximum(GregorianCalendar.DAY_OF_MONTH)) {
			String message = (String)WOApplication.application().valueForKeyPath(
					"strings.RujelEduPlan_EduPlan.messages.invalidDateInMonth");
			throw new NSValidation.ValidationException(message);
		}
		day = endDay().intValue();
		month = endMonth().intValue();
		if(month > 12)
			month -= 12;
		cal.set(GregorianCalendar.MONTH,month);
		if(day > cal.getActualMaximum(GregorianCalendar.DAY_OF_MONTH)) {
			String message = (String)WOApplication.application().valueForKeyPath(
					"strings.RujelEduPlan_EduPlan.messages.invalidDateInMonth");
			throw new NSValidation.ValidationException(message);
		}
	}
	
	public static NSTimestamp dateFromPreset(Integer eduYear, Integer aMonth, Integer aDay) {
		int year = eduYear.intValue();
		int month = aMonth.intValue();
		if(month > 12) {
			month -= 12;
			year++;
		}
		int day =  aDay.intValue();
		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.YEAR, year);
		cal.set(Calendar.MONDAY, month);
		cal.set(Calendar.DATE, day);
		return new NSTimestamp(cal.getTimeInMillis());
	}
	
	public Holiday makeHoliday(Integer eduYear, String listName) {
		Holiday hd = (Holiday)EOUtilities.createAndInsertInstance(editingContext(),
				Holiday.ENTITY_NAME);
		hd.addObjectToBothSidesOfRelationshipWithKey(this, Holiday.HOLIDAY_TYPE_KEY);
/*		int year = eduYear.intValue();
		int month = beginMonth().intValue();
		if(month > 12) {
			month -= 12;
			year++;
		}
		int day =  beginDay().intValue();
		NSTimestamp datum = new NSTimestamp(year,month,day,0,0,0,TimeZone.getDefault());
		hd.setBegin(datum);
		year = eduYear;
		month = beginMonth().intValue();
		if(month > 12) {
			month -= 12;
			year++;
		}
		day =  beginDay().intValue();
		datum = new NSTimestamp(year,month,day,0,0,0,TimeZone.getDefault());
		hd.setEnd(datum);
*/		
		hd.setBegin(dateFromPreset(eduYear, beginDay(), beginMonth()));
		hd.setEnd(dateFromPreset(eduYear, endMonth(), endDay()));
		hd.setListName(listName);
		return hd;
	}
}
