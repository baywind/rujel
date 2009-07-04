// _SettingsBase.java

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
// $LastChangedRevision: 4733 $ DO NOT EDIT.  Make changes to SettingsBase.java instead.

package net.rujel.base;

import com.webobjects.eocontrol.*;
import com.webobjects.foundation.*;
import java.math.BigDecimal;

@SuppressWarnings("all")
public abstract class _SettingsBase extends EOGenericRecord {
	public static final String ENTITY_NAME = "SettingsBase";

	// Attributes
	public static final String KEY_KEY = "key";
	public static final String NUMERIC_VALUE_KEY = "numericValue";
	public static final String TEXT_VALUE_KEY = "textValue";

	// Relationships
	public static final String BY_COURSE_KEY = "byCourse";

  public String key() {
    return (String) storedValueForKey(KEY_KEY);
  }

  public void setKey(String value) {
    takeStoredValueForKey(value, KEY_KEY);
  }

  public Integer numericValue() {
    return (Integer) storedValueForKey(NUMERIC_VALUE_KEY);
  }

  public void setNumericValue(Integer value) {
    takeStoredValueForKey(value, NUMERIC_VALUE_KEY);
  }

  public String textValue() {
    return (String) storedValueForKey(TEXT_VALUE_KEY);
  }

  public void setTextValue(String value) {
    takeStoredValueForKey(value, TEXT_VALUE_KEY);
  }

  public NSArray byCourse() {
    return (NSArray)storedValueForKey(BY_COURSE_KEY);
  }
 
  public void setByCourse(NSArray value) {
    takeStoredValueForKey(value, BY_COURSE_KEY);
  }
  
  public void addToByCourse(EOEnterpriseObject object) {
    includeObjectIntoPropertyWithKey(object, BY_COURSE_KEY);
  }

  public void removeFromByCourse(EOEnterpriseObject object) {
    excludeObjectFromPropertyWithKey(object, BY_COURSE_KEY);
  }

}
