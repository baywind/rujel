// _Substitute.java

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
// $LastChangedRevision: 4733 $ DO NOT EDIT.  Make changes to Substitute.java instead.

package net.rujel.curriculum;

import com.webobjects.eocontrol.*;
import com.webobjects.foundation.*;
import java.math.BigDecimal;

@SuppressWarnings("all")
public abstract class _Substitute extends EOGenericRecord {
	public static final String ENTITY_NAME = "Substitute";

	// Attributes
	public static final String DATE_KEY = "date";
	public static final String FACTOR_KEY = "factor";

	// Relationships
	public static final String REASON_KEY = "reason";

  public NSTimestamp date() {
    return (NSTimestamp) storedValueForKey(DATE_KEY);
  }

  public void setDate(NSTimestamp value) {
    takeStoredValueForKey(value, DATE_KEY);
  }

  public java.math.BigDecimal factor() {
    return (java.math.BigDecimal) storedValueForKey(FACTOR_KEY);
  }

  public void setFactor(java.math.BigDecimal value) {
    takeStoredValueForKey(value, FACTOR_KEY);
  }

  public net.rujel.curriculum.Reason reason() {
    return (net.rujel.curriculum.Reason)storedValueForKey(REASON_KEY);
  }

  public void setReason(EOEnterpriseObject value) {
    	takeStoredValueForKey(value, REASON_KEY);
  }
  
}
