// _WorkType.java

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
// $LastChangedRevision: 4733 $ DO NOT EDIT.  Make changes to WorkType.java instead.

package net.rujel.criterial;

import com.webobjects.eocontrol.*;
import com.webobjects.foundation.*;
import java.math.BigDecimal;

@SuppressWarnings("all")
public abstract class _WorkType extends EOGenericRecord {
	public static final String ENTITY_NAME = "WorkType";

	// Attributes
	public static final String COLOR_NO_WEIGHT_KEY = "colorNoWeight";
	public static final String COLOR_WEIGHT_KEY = "colorWeight";
	public static final String DFLT_FLAGS_KEY = "dfltFlags";
	public static final String DFLT_WEIGHT_KEY = "dfltWeight";
	public static final String SORT_KEY = "sort";
	public static final String TYPE_NAME_KEY = "typeName";

	// Relationships
	public static final String CRITERIA_SET_KEY = "criteriaSet";

  public String colorNoWeight() {
    return (String) storedValueForKey(COLOR_NO_WEIGHT_KEY);
  }

  public void setColorNoWeight(String value) {
    takeStoredValueForKey(value, COLOR_NO_WEIGHT_KEY);
  }

  public String colorWeight() {
    return (String) storedValueForKey(COLOR_WEIGHT_KEY);
  }

  public void setColorWeight(String value) {
    takeStoredValueForKey(value, COLOR_WEIGHT_KEY);
  }

  public Integer dfltFlags() {
    return (Integer) storedValueForKey(DFLT_FLAGS_KEY);
  }

  public void setDfltFlags(Integer value) {
    takeStoredValueForKey(value, DFLT_FLAGS_KEY);
  }

  public java.math.BigDecimal dfltWeight() {
    return (java.math.BigDecimal) storedValueForKey(DFLT_WEIGHT_KEY);
  }

  public void setDfltWeight(java.math.BigDecimal value) {
    takeStoredValueForKey(value, DFLT_WEIGHT_KEY);
  }

  public Integer sort() {
    return (Integer) storedValueForKey(SORT_KEY);
  }

  public void setSort(Integer value) {
    takeStoredValueForKey(value, SORT_KEY);
  }

  public String typeName() {
    return (String) storedValueForKey(TYPE_NAME_KEY);
  }

  public void setTypeName(String value) {
    takeStoredValueForKey(value, TYPE_NAME_KEY);
  }

  public net.rujel.criterial.CriteriaSet criteriaSet() {
    return (net.rujel.criterial.CriteriaSet)storedValueForKey(CRITERIA_SET_KEY);
  }

  public void setCriteriaSet(EOEnterpriseObject value) {
    	takeStoredValueForKey(value, CRITERIA_SET_KEY);
  }
  
}
