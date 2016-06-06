// _QualifiedSetting.java

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
// $LastChangedRevision: 4733 $ DO NOT EDIT.  Make changes to QualifiedSetting.java instead.

package net.rujel.base;

import com.webobjects.eocontrol.*;
import com.webobjects.foundation.*;
import java.math.BigDecimal;

@SuppressWarnings("all")
public abstract class _QualifiedSetting extends EOGenericRecord {
	public static final String ENTITY_NAME = "QualifiedSetting";

	// Attributes
	public static final String ARGUMENTS_STRING_KEY = "argumentsString";
	public static final String EDU_YEAR_KEY = "eduYear";
	public static final String NUMERIC_VALUE_KEY = "numericValue";
	public static final String QUALIFIER_STRING_KEY = "qualifierString";
	public static final String SORT_KEY = "sort";
	public static final String TEXT_VALUE_KEY = "textValue";

	// Relationships
	public static final String SECTION_KEY = "section";
	public static final String SETTINGS_BASE_KEY = "settingsBase";

  public String argumentsString() {
    return (String) storedValueForKey(ARGUMENTS_STRING_KEY);
  }

  public void setArgumentsString(String value) {
    takeStoredValueForKey(value, ARGUMENTS_STRING_KEY);
  }

  public Integer eduYear() {
    return (Integer) storedValueForKey(EDU_YEAR_KEY);
  }

  public void setEduYear(Integer value) {
    takeStoredValueForKey(value, EDU_YEAR_KEY);
  }

  public Integer numericValue() {
    return (Integer) storedValueForKey(NUMERIC_VALUE_KEY);
  }

  public void setNumericValue(Integer value) {
    takeStoredValueForKey(value, NUMERIC_VALUE_KEY);
  }

  public String qualifierString() {
    return (String) storedValueForKey(QUALIFIER_STRING_KEY);
  }

  public void setQualifierString(String value) {
    takeStoredValueForKey(value, QUALIFIER_STRING_KEY);
  }

  public Integer sort() {
    return (Integer) storedValueForKey(SORT_KEY);
  }

  public void setSort(Integer value) {
    takeStoredValueForKey(value, SORT_KEY);
  }

  public String textValue() {
    return (String) storedValueForKey(TEXT_VALUE_KEY);
  }

  public void setTextValue(String value) {
    takeStoredValueForKey(value, TEXT_VALUE_KEY);
  }

  public net.rujel.base.SchoolSection section() {
    return (net.rujel.base.SchoolSection)storedValueForKey(SECTION_KEY);
  }

  public void setSection(EOEnterpriseObject value) {
    	takeStoredValueForKey(value, SECTION_KEY);
  }
  
  public net.rujel.base.SettingsBase settingsBase() {
    return (net.rujel.base.SettingsBase)storedValueForKey(SETTINGS_BASE_KEY);
  }

  public void setSettingsBase(EOEnterpriseObject value) {
    	takeStoredValueForKey(value, SETTINGS_BASE_KEY);
  }
  
}
