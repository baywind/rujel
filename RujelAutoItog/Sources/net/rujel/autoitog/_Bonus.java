// _Bonus.java

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
// $LastChangedRevision: 4733 $ DO NOT EDIT.  Make changes to Bonus.java instead.

package net.rujel.autoitog;

import com.webobjects.eocontrol.*;
import com.webobjects.foundation.*;
import java.math.BigDecimal;

@SuppressWarnings("all")
public abstract class _Bonus extends EOGenericRecord {
	public static final String ENTITY_NAME = "Bonus";

	// Attributes
	public static final String MARK_KEY = "mark";
	public static final String REASON_KEY = "reason";
	public static final String VALUE_KEY = "value";

	// Relationships
	public static final String EDU_PERIOD_KEY = "eduPeriod";
	public static final String PROGNOSES_KEY = "prognoses";

  public String mark() {
    return (String) storedValueForKey(MARK_KEY);
  }

  public void setMark(String value) {
    takeStoredValueForKey(value, MARK_KEY);
  }

  public String reason() {
    return (String) storedValueForKey(REASON_KEY);
  }

  public void setReason(String value) {
    takeStoredValueForKey(value, REASON_KEY);
  }

  public BigDecimal value() {
    return (BigDecimal) storedValueForKey(VALUE_KEY);
  }

  public void setValue(BigDecimal value) {
    takeStoredValueForKey(value, VALUE_KEY);
  }

  public net.rujel.eduresults.EduPeriod eduPeriod() {
    return (net.rujel.eduresults.EduPeriod)storedValueForKey(EDU_PERIOD_KEY);
  }

  public void setEduPeriod(EOEnterpriseObject value) {
    	takeStoredValueForKey(value, EDU_PERIOD_KEY);
  }
  
  public NSArray prognoses() {
    return (NSArray)storedValueForKey(PROGNOSES_KEY);
  }
 
  public void setPrognoses(NSArray value) {
    takeStoredValueForKey(value, PROGNOSES_KEY);
  }
  
  public void addToPrognoses(EOEnterpriseObject object) {
    includeObjectIntoPropertyWithKey(object, PROGNOSES_KEY);
  }

  public void removeFromPrognoses(EOEnterpriseObject object) {
    excludeObjectFromPropertyWithKey(object, PROGNOSES_KEY);
  }

}
