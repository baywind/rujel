// _Description.java

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
// $LastChangedRevision: 4733 $ DO NOT EDIT.  Make changes to Description.java instead.

package net.rujel.stats;

import com.webobjects.eocontrol.*;
import com.webobjects.foundation.*;
import java.math.BigDecimal;

@SuppressWarnings("all")
public abstract class _Description extends EOGenericRecord {
	public static final String ENTITY_NAME = "Description";

	// Attributes
	public static final String DESCRIPTION_KEY = "description";
	public static final String ENT_NAME_KEY = "entName";
	public static final String GROUPING1_KEY = "grouping1";
	public static final String GROUPING2_KEY = "grouping2";
	public static final String STAT_FIELD_KEY = "statField";

	// Relationships
	public static final String BORDER_SET_KEY = "borderSet";

  public String description() {
    return (String) storedValueForKey(DESCRIPTION_KEY);
  }

  public void setDescription(String value) {
    takeStoredValueForKey(value, DESCRIPTION_KEY);
  }

  public String entName() {
    return (String) storedValueForKey(ENT_NAME_KEY);
  }

  public void setEntName(String value) {
    takeStoredValueForKey(value, ENT_NAME_KEY);
  }

  public String grouping1() {
    return (String) storedValueForKey(GROUPING1_KEY);
  }

  public void setGrouping1(String value) {
    takeStoredValueForKey(value, GROUPING1_KEY);
  }

  public String grouping2() {
    return (String) storedValueForKey(GROUPING2_KEY);
  }

  public void setGrouping2(String value) {
    takeStoredValueForKey(value, GROUPING2_KEY);
  }

  public String statField() {
    return (String) storedValueForKey(STAT_FIELD_KEY);
  }

  public void setStatField(String value) {
    takeStoredValueForKey(value, STAT_FIELD_KEY);
  }

  public net.rujel.criterial.BorderSet borderSet() {
    return (net.rujel.criterial.BorderSet)storedValueForKey(BORDER_SET_KEY);
  }

  public void setBorderSet(EOEnterpriseObject value) {
    	takeStoredValueForKey(value, BORDER_SET_KEY);
  }
  
}
