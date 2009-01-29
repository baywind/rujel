// _Reason.java

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
// $LastChangedRevision: 4733 $ DO NOT EDIT.  Make changes to Reason.java instead.

package net.rujel.curriculum;

import com.webobjects.eocontrol.*;
import com.webobjects.foundation.*;
import java.math.BigDecimal;

@SuppressWarnings("all")
public abstract class _Reason extends EOGenericRecord {
	public static final String ENTITY_NAME = "Reason";

	// Attributes
	public static final String BEGIN_KEY = "begin";
	public static final String END_KEY = "end";
	public static final String REASON_KEY = "reason";
	public static final String SCHOOL_KEY = "school";
	public static final String VERIFICATION_KEY = "verification";

	// Relationships
	public static final String SUBSTITUTES_KEY = "substitutes";
	public static final String VARIATIONS_KEY = "variations";

  public NSTimestamp begin() {
    return (NSTimestamp) storedValueForKey(BEGIN_KEY);
  }

  public void setBegin(NSTimestamp value) {
    takeStoredValueForKey(value, BEGIN_KEY);
  }

  public NSTimestamp end() {
    return (NSTimestamp) storedValueForKey(END_KEY);
  }

  public void setEnd(NSTimestamp value) {
    takeStoredValueForKey(value, END_KEY);
  }

  public String reason() {
    return (String) storedValueForKey(REASON_KEY);
  }

  public void setReason(String value) {
    takeStoredValueForKey(value, REASON_KEY);
  }

  public Integer school() {
    return (Integer) storedValueForKey(SCHOOL_KEY);
  }

  public void setSchool(Integer value) {
    takeStoredValueForKey(value, SCHOOL_KEY);
  }

  public String verification() {
    return (String) storedValueForKey(VERIFICATION_KEY);
  }

  public void setVerification(String value) {
    takeStoredValueForKey(value, VERIFICATION_KEY);
  }

  public NSArray substitutes() {
    return (NSArray)storedValueForKey(SUBSTITUTES_KEY);
  }
 
  public void setSubstitutes(NSArray value) {
    takeStoredValueForKey(value, SUBSTITUTES_KEY);
  }
  
  public void addToSubstitutes(EOEnterpriseObject object) {
    includeObjectIntoPropertyWithKey(object, SUBSTITUTES_KEY);
  }

  public void removeFromSubstitutes(EOEnterpriseObject object) {
    excludeObjectFromPropertyWithKey(object, SUBSTITUTES_KEY);
  }

  public NSArray variations() {
    return (NSArray)storedValueForKey(VARIATIONS_KEY);
  }
 
  public void setVariations(NSArray value) {
    takeStoredValueForKey(value, VARIATIONS_KEY);
  }
  
  public void addToVariations(EOEnterpriseObject object) {
    includeObjectIntoPropertyWithKey(object, VARIATIONS_KEY);
  }

  public void removeFromVariations(EOEnterpriseObject object) {
    excludeObjectFromPropertyWithKey(object, VARIATIONS_KEY);
  }

}
