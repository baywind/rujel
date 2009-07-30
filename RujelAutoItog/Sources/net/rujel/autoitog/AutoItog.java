//  AutoItog.java

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

package net.rujel.autoitog;

import java.util.Calendar;

import net.rujel.interfaces.EOInitialiser;

import com.webobjects.foundation.*;
import com.webobjects.eocontrol.*;

public class AutoItog extends _AutoItog {

	public static void init() {
		EOInitialiser.initialiseRelationship("ItogRelated","course",false,"courseID","EduCourse");
	}

	public void awakeFromInsertion(EOEditingContext ec) {
		super.awakeFromInsertion(ec);
		setFlags(new Integer(0));
		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.HOUR_OF_DAY, 4);
		cal.set(Calendar.MINUTE, 20);
		cal.set(Calendar.DAY_OF_MONTH, 27);
		cal.add(Calendar.MONTH, 2);
		NSTimestamp date = new NSTimestamp(cal.getTimeInMillis());
		setFireTime(date);
		setFireTime(date);
	}
	
	public NSTimestamp fireDateTime() {
		Calendar cal = Calendar.getInstance();
		cal.setTime(fireDate());
		int year = cal.get(Calendar.YEAR);
		int day = cal.get(Calendar.DAY_OF_YEAR);
		cal.setTime(fireTime());
		cal.set(Calendar.YEAR, year);
		cal.set(Calendar.DAY_OF_YEAR, day);
		NSTimestamp date = new NSTimestamp(cal.getTimeInMillis());
		return date;
	}
	
    public boolean noCalculator() {
    	String calcName = calculatorName();
		if(calcName == null || calcName.length() == 0 || calcName.equalsIgnoreCase("none"))
			return true;
		return false;
    }

}
