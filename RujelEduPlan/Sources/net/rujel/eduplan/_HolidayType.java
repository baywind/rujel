// _HolidayType.java

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

// Created by eogenerator
// $LastChangedRevision: 4733 $ DO NOT EDIT.  Make changes to HolidayType.java instead.

package net.rujel.eduplan;

import com.webobjects.eocontrol.*;
import com.webobjects.foundation.*;
import java.math.BigDecimal;

@SuppressWarnings("all")
public abstract class _HolidayType extends EOGenericRecord {
	public static final String ENTITY_NAME = "HolidayType";

	// Attributes
	public static final String BEGIN_DAY_KEY = "beginDay";
	public static final String BEGIN_MONTH_KEY = "beginMonth";
	public static final String END_DAY_KEY = "endDay";
	public static final String END_MONTH_KEY = "endMonth";
	public static final String NAME_KEY = "name";

	// Relationships
	public static final String HOLIDAYS_KEY = "holidays";

  public Integer beginDay() {
    return (Integer) storedValueForKey(BEGIN_DAY_KEY);
  }

  public void setBeginDay(Integer value) {
    takeStoredValueForKey(value, BEGIN_DAY_KEY);
  }

  public Integer beginMonth() {
    return (Integer) storedValueForKey(BEGIN_MONTH_KEY);
  }

  public void setBeginMonth(Integer value) {
    takeStoredValueForKey(value, BEGIN_MONTH_KEY);
  }

  public Integer endDay() {
    return (Integer) storedValueForKey(END_DAY_KEY);
  }

  public void setEndDay(Integer value) {
    takeStoredValueForKey(value, END_DAY_KEY);
  }

  public Integer endMonth() {
    return (Integer) storedValueForKey(END_MONTH_KEY);
  }

  public void setEndMonth(Integer value) {
    takeStoredValueForKey(value, END_MONTH_KEY);
  }

  public String name() {
    return (String) storedValueForKey(NAME_KEY);
  }

  public void setName(String value) {
    takeStoredValueForKey(value, NAME_KEY);
  }

  public NSArray holidays() {
    return (NSArray)storedValueForKey(HOLIDAYS_KEY);
  }
 
  public void setHolidays(NSArray value) {
    takeStoredValueForKey(value, HOLIDAYS_KEY);
  }
  
  public void addToHolidays(EOEnterpriseObject object) {
    includeObjectIntoPropertyWithKey(object, HOLIDAYS_KEY);
  }

  public void removeFromHolidays(EOEnterpriseObject object) {
    excludeObjectFromPropertyWithKey(object, HOLIDAYS_KEY);
  }

}
