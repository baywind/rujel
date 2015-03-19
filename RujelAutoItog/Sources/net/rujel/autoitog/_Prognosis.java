// _Prognosis.java

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
// $LastChangedRevision: 4733 $ DO NOT EDIT.  Make changes to Prognosis.java instead.

package net.rujel.autoitog;

import com.webobjects.eocontrol.*;
import com.webobjects.foundation.*;
import java.math.BigDecimal;

@SuppressWarnings("all")
public abstract class _Prognosis extends EOGenericRecord {
	public static final String ENTITY_NAME = "Prognosis";

	// Attributes
	public static final String COMPLETE_KEY = "complete";
	public static final String FIRE_DATE_KEY = "fireDate";
	public static final String FLAGS_KEY = "flags";
	public static final String MARK_KEY = "mark";
	public static final String STATE_KEY = "state";
	public static final String VALUE_KEY = "value";

	// Relationships
	public static final String BONUS_KEY = "bonus";
	public static final String ITOG_CONTAINER_KEY = "itogContainer";

  public java.math.BigDecimal complete() {
    return (java.math.BigDecimal) storedValueForKey(COMPLETE_KEY);
  }

  public void setComplete(java.math.BigDecimal value) {
    takeStoredValueForKey(value, COMPLETE_KEY);
  }

  public NSTimestamp fireDate() {
    return (NSTimestamp) storedValueForKey(FIRE_DATE_KEY);
  }

  public void setFireDate(NSTimestamp value) {
    takeStoredValueForKey(value, FIRE_DATE_KEY);
  }

  public Integer flags() {
    return (Integer) storedValueForKey(FLAGS_KEY);
  }

  public void setFlags(Integer value) {
    takeStoredValueForKey(value, FLAGS_KEY);
  }

  public String mark() {
    return (String) storedValueForKey(MARK_KEY);
  }

  public void setMark(String value) {
    takeStoredValueForKey(value, MARK_KEY);
  }

  public Integer state() {
    return (Integer) storedValueForKey(STATE_KEY);
  }

  public void setState(Integer value) {
    takeStoredValueForKey(value, STATE_KEY);
  }

  public java.math.BigDecimal value() {
    return (java.math.BigDecimal) storedValueForKey(VALUE_KEY);
  }

  public void setValue(java.math.BigDecimal value) {
    takeStoredValueForKey(value, VALUE_KEY);
  }

  public net.rujel.autoitog.Bonus bonus() {
    return (net.rujel.autoitog.Bonus)storedValueForKey(BONUS_KEY);
  }

  public void setBonus(EOEnterpriseObject value) {
    	takeStoredValueForKey(value, BONUS_KEY);
  }
  
  public net.rujel.eduresults.ItogContainer itogContainer() {
    return (net.rujel.eduresults.ItogContainer)storedValueForKey(ITOG_CONTAINER_KEY);
  }

  public void setItogContainer(EOEnterpriseObject value) {
    	takeStoredValueForKey(value, ITOG_CONTAINER_KEY);
  }
  
}
