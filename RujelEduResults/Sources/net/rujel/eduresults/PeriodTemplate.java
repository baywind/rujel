// PeriodTemplate.java

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

package net.rujel.eduresults;


import com.webobjects.foundation.*;
import com.webobjects.eocontrol.*;
import com.webobjects.appserver.WOApplication;
import com.webobjects.eoaccess.EOUtilities;
import java.util.TimeZone;
import java.util.GregorianCalendar;

public class PeriodTemplate extends _PeriodTemplate
{
    public PeriodTemplate() {
        super();
    }

/*
    // If you add instance variables to store property values you
    // should add empty implementions of the Serialization methods
    // to avoid unnecessary overhead (the properties will be
    // serialized for you in the superclass).
    private void writeObject(java.io.ObjectOutputStream out) throws java.io.IOException {
    }

    private void readObject(java.io.ObjectInputStream in) throws java.io.IOException, java.lang.ClassNotFoundException {
    }
*/
	private transient boolean beginSecondYear;
	private transient int beginMonthInYear = 0;
	private transient boolean endSecondYear;
	private transient int endMonthInYear = 0;
	
	public void awakeFromFetch(EOEditingContext ec) {
		super.awakeFromFetch(ec);
		beginSecondYear = ((beginMonthInYear =  beginMonth().intValue()) > 12);
		if(beginSecondYear) {
			beginMonthInYear = beginMonthInYear - 12;
		}
		endSecondYear = ((endMonthInYear =  endMonth().intValue()) > 12);
		if(endSecondYear) {
			endMonthInYear = endMonthInYear - 12;
		}
	}
	
	public boolean beginSecondYear() {
		return beginSecondYear;
	}
	
	public void setBeginSecondYear(boolean aValue) {
		if(aValue != beginSecondYear) {
			beginSecondYear = aValue;
			super.setBeginMonth(new Integer(beginMonthInYear + ((beginSecondYear)?12:0)));
		}
	}
	
	public int beginMonthInYear() {
		return beginMonthInYear;
	}
	
	public void setBeginMonthInYear(int aValue) {
		if(aValue != beginMonthInYear) {
			beginMonthInYear = aValue;
			super.setBeginMonth(new Integer(beginMonthInYear + ((beginSecondYear)?12:0)));
		}
	}
	///end
	public boolean endSecondYear() {
		return endSecondYear;
	}
	
	public void setEndSecondYear(boolean aValue) {
		if(aValue != endSecondYear) {
			endSecondYear = aValue;
			super.setEndMonth(new Integer(endMonthInYear + ((endSecondYear)?12:0)));
		}
	}
	
	public int endMonthInYear() {
		return endMonthInYear;
	}
	
	public void setEndMonthInYear(int aValue) {
		if(aValue != endMonthInYear) {
			endMonthInYear = aValue;
			super.setEndMonth(new Integer(endMonthInYear + ((endSecondYear)?12:0)));
		}
	}
///
	public void setBeginMonth(Number aValue) {
		beginMonthInYear = 1;
		beginSecondYear = (aValue != null && (beginMonthInYear =  aValue.intValue()) > 12);
		if(beginSecondYear) {
			beginMonthInYear = beginMonthInYear - 12;
		}
        super.setBeginMonth(aValue);
    }
	
    public void setEndMonth(Number aValue) {
		endMonthInYear = 1;
		endSecondYear = (aValue != null && (endMonthInYear =  aValue.intValue()) > 12);
		if(endSecondYear) {
			endMonthInYear = endMonthInYear - 12;
		}
		super.setEndMonth(aValue);
    }

	public Number validatePerNum(Object aValue) throws NSValidation.ValidationException {
		Integer numberValue;
		if (aValue instanceof String) {
			// Convert the String to an Integer.
			try {
				numberValue = new Integer((String)aValue);
			} catch (NumberFormatException numberFormatException) {
				throw new NSValidation.ValidationException("Validation exception: Unable to convert the String " + aValue + " to an Integer");
			}
		} else if (aValue instanceof Number) {
			numberValue = new Integer(((Number)aValue).intValue());
		} else {
			throw new NSValidation.ValidationException("Validation exception: Unable to convert the Object " + aValue + " to an Integer");
		}
		
		int num = numberValue.intValue();
		Number count = periodType().inYearCount();
		if (num <= 0 || num > count.intValue()) {
			String message = String.format((String)WOApplication.application().valueForKeyPath("extStrings.RujelEduResults-EduResults.invalidPeriodNum"),count);
			throw new NSValidation.ValidationException(message, this, "num");
		}
		return numberValue;
	}
	
	public EduPeriod makeEduPeriod(int eduYear) {
		EduPeriod per = (EduPeriod)EOUtilities.createAndInsertInstance(editingContext(),"EduPeriod");
		per.setPeriodType(periodType());
		per.setNum(perNum());
		per.setEduYear(new Integer(eduYear));
		
		int year = (beginSecondYear)?eduYear + 1:eduYear;
		NSTimestamp datum = new NSTimestamp(year,beginMonthInYear,beginDay().intValue(),0,0,0,TimeZone.getDefault());
		per.setBegin(datum);
		
		year = (endSecondYear)?eduYear + 1:eduYear;
		datum = new NSTimestamp(year,endMonthInYear,endDay().intValue(),0,0,0,TimeZone.getDefault());
		per.setEnd(datum);
		
		return per;
	}
	
	public void validateForSave() throws NSValidation.ValidationException {
		super.validateForSave();
		int dif = endMonth().intValue() - beginMonth().intValue();
		if(dif == 0) dif = endDay().intValue() - beginDay().intValue();
		if(dif <= 0) {
			String message = (String)WOApplication.application().valueForKeyPath("extStrings.RujelEduResults-EduResults.beginEndPeriod");
			throw new NSValidation.ValidationException(message);
		}
		int day = beginDay().intValue();
		GregorianCalendar cal = new GregorianCalendar(2004,beginMonthInYear,1);
		if(day > cal.getActualMaximum(GregorianCalendar.DAY_OF_MONTH)) {
			String message = (String)WOApplication.application().valueForKeyPath("extStrings.RujelEduResults-EduResults.invalidDateInMonth");
			throw new NSValidation.ValidationException(message);
		}
		day = endDay().intValue();
		cal.set(GregorianCalendar.MONTH,endMonthInYear);
		if(day > cal.getActualMaximum(GregorianCalendar.DAY_OF_MONTH)) {
			String message = (String)WOApplication.application().valueForKeyPath("extStrings.RujelEduResults-EduResults.invalidDateInMonth");
			throw new NSValidation.ValidationException(message);
		}
	}
}
