// _PrognosUsage.java

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
// $LastChangedRevision: 4733 $ DO NOT EDIT.  Make changes to PrognosUsage.java instead.

package net.rujel.autoitog;

import com.webobjects.eocontrol.*;
import com.webobjects.foundation.*;
import java.math.BigDecimal;

@SuppressWarnings("all")
@Deprecated
public abstract class _PrognosUsage extends EOGenericRecord {
	public static final String ENTITY_NAME = "PrognosUsage";

	// Attributes
	public static final String CALCULATOR_NAME_KEY = "calculatorName";
	public static final String EDU_YEAR_KEY = "eduYear";
	public static final String FLAGS_KEY = "flags";

	// Relationships
	public static final String BORDER_SET_KEY = "borderSet";
	public static final String PERIOD_TYPE_KEY = "periodType";

  public String calculatorName() {
    return (String) storedValueForKey(CALCULATOR_NAME_KEY);
  }

  public void setCalculatorName(String value) {
    takeStoredValueForKey(value, CALCULATOR_NAME_KEY);
  }

  public Integer eduYear() {
    return (Integer) storedValueForKey(EDU_YEAR_KEY);
  }

  public void setEduYear(Integer value) {
    takeStoredValueForKey(value, EDU_YEAR_KEY);
  }

  public Integer flags() {
    return (Integer) storedValueForKey(FLAGS_KEY);
  }

  public void setFlags(Integer value) {
    takeStoredValueForKey(value, FLAGS_KEY);
  }

  public net.rujel.criterial.BorderSet borderSet() {
    return (net.rujel.criterial.BorderSet)storedValueForKey(BORDER_SET_KEY);
  }

  public void setBorderSet(EOEnterpriseObject value) {
    	takeStoredValueForKey(value, BORDER_SET_KEY);
  }
  
//  public net.rujel.eduresults.PeriodType periodType() {
//    return (net.rujel.eduresults.PeriodType)storedValueForKey(PERIOD_TYPE_KEY);
//  }

  public void setPeriodType(EOEnterpriseObject value) {
    	takeStoredValueForKey(value, PERIOD_TYPE_KEY);
  }
  
}
