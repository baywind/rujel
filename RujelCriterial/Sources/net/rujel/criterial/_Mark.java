// _Mark.java

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
// $LastChangedRevision: 4733 $ DO NOT EDIT.  Make changes to Mark.java instead.

package net.rujel.criterial;

import com.webobjects.eocontrol.*;
import com.webobjects.foundation.*;
import java.math.BigDecimal;

@SuppressWarnings("all")
public abstract class _Mark extends EOGenericRecord {
	public static final String ENTITY_NAME = "Mark";

	// Attributes
	public static final String CRITERION_KEY = "criterion";
	public static final String DATE_SET_KEY = "dateSet";
	public static final String VALUE_KEY = "value";

	// Relationships
	public static final String WORK_KEY = "work";

  public Integer criterion() {
    return (Integer) storedValueForKey(CRITERION_KEY);
  }

  public void setCriterion(Integer value) {
    takeStoredValueForKey(value, CRITERION_KEY);
  }

  public NSTimestamp dateSet() {
    return (NSTimestamp) storedValueForKey(DATE_SET_KEY);
  }

  public void setDateSet(NSTimestamp value) {
    takeStoredValueForKey(value, DATE_SET_KEY);
  }

  public Integer value() {
    return (Integer) storedValueForKey(VALUE_KEY);
  }

  public void setValue(Integer value) {
    takeStoredValueForKey(value, VALUE_KEY);
  }

  public net.rujel.criterial.Work work() {
    return (net.rujel.criterial.Work)storedValueForKey(WORK_KEY);
  }

  public void setWork(EOEnterpriseObject value) {
    	takeStoredValueForKey(value, WORK_KEY);
  }
  
}
