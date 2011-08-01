//  VseList.java

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

import net.rujel.base.MyUtility;

import com.webobjects.appserver.WOApplication;
import com.webobjects.eocontrol.EOEnterpriseObject;
import com.webobjects.eocontrol.EOSortOrdering;
import com.webobjects.foundation.*;

public class VseList extends _VseList {

	public static final NSArray sorter = new NSArray(
			new EOSortOrdering(STUDENT_KEY,EOSortOrdering.CompareAscending));

	public void setEnter(NSTimestamp value) {
		super.setEnter(value);
		eduGroup().nullify();
		if(value != null && student().enter() != null && student().enter().after(value))
			student().setEnter(value);
	}

	public void setLeave(NSTimestamp value) {
		super.setLeave(value);
		eduGroup().nullify();
		if(value != null && student().leave() != null && student().leave().before(value))
			student().setLeave(value);
	}
	
	public void validateForSave() {
		super.validateForSave();
		validateDates(enter(), leave());
	}
	
	public static boolean isActual(EOEnterpriseObject l, long now) {
		NSTimestamp border = (NSTimestamp)l.valueForKey("leave");
		if(border != null) {
			if(border.getTime() < now - NSLocking.OneDay) {
				return false;
			} else {
				Calendar cal = Calendar.getInstance();
				cal.setTime(border);
				cal.set(Calendar.HOUR, 23);
				cal.set(Calendar.MINUTE, 59);
				if(cal.getTimeInMillis() < now)
					return false;
			}
		}
		border = (NSTimestamp)l.valueForKey("enter");
		if(border != null) {
			if(border.getTime() > now) {
				return false;
			} else if(border.getTime() - now < NSLocking.OneDay){
				Calendar cal = Calendar.getInstance();
				cal.setTime(border);
				cal.set(Calendar.HOUR, 0);
				cal.set(Calendar.MINUTE, 0);
				return (cal.getTimeInMillis() <= now);
			}
		}
		return true;
	}
	
	public boolean isActual(NSTimestamp onDate) {
		int eduYear = MyUtility.eduYearForDate(onDate).intValue();
		return eduGroup().isActual(eduYear) && isActual(this, onDate.getTime());
	}
	
	public boolean isActual() {
		return isActual(MyUtility.date(editingContext()));
	}
	
	public static void validateDates(NSTimestamp enter, NSTimestamp leave) {
		if(enter == null || leave == null)
			return;
		if(enter.getTime() > leave.getTime()) {
			throw new NSValidation.ValidationException((String)WOApplication.application()
					.valueForKeyPath("strings.RujelVseLists_VseStrings.enterLaterLeave"));
		}
	}
}
