// _BorderSet.java

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
// $LastChangedRevision: 4733 $ DO NOT EDIT.  Make changes to BorderSet.java instead.

package net.rujel.criterial;

import com.webobjects.eocontrol.*;
import com.webobjects.foundation.*;
import java.math.BigDecimal;

@SuppressWarnings("all")
public abstract class _BorderSet extends EOGenericRecord {
	public static final String ENTITY_NAME = "BorderSet";

	// Attributes
	public static final String EXCLUDE_KEY = "exclude";
	public static final String FORMAT_STRING_KEY = "formatString";
	public static final String TITLE_KEY = "title";
	public static final String USE_CLASS_KEY = "useClass";
	public static final String ZERO_VALUE_KEY = "zeroValue";

	// Relationships
	public static final String BORDERS_KEY = "borders";

  public Boolean exclude() {
    return (Boolean) storedValueForKey(EXCLUDE_KEY);
  }

  public void setExclude(Boolean value) {
    takeStoredValueForKey(value, EXCLUDE_KEY);
  }

  public String formatString() {
    return (String) storedValueForKey(FORMAT_STRING_KEY);
  }

  public void setFormatString(String value) {
    takeStoredValueForKey(value, FORMAT_STRING_KEY);
  }

  public String title() {
    return (String) storedValueForKey(TITLE_KEY);
  }

  public void setTitle(String value) {
    takeStoredValueForKey(value, TITLE_KEY);
  }

  public String useClass() {
    return (String) storedValueForKey(USE_CLASS_KEY);
  }

  public void setUseClass(String value) {
    takeStoredValueForKey(value, USE_CLASS_KEY);
  }

  public String zeroValue() {
    return (String) storedValueForKey(ZERO_VALUE_KEY);
  }

  public void setZeroValue(String value) {
    takeStoredValueForKey(value, ZERO_VALUE_KEY);
  }

  public NSArray borders() {
    return (NSArray)storedValueForKey(BORDERS_KEY);
  }
 
  public void setBorders(NSArray value) {
    takeStoredValueForKey(value, BORDERS_KEY);
  }
  
  public void addToBorders(EOEnterpriseObject object) {
    includeObjectIntoPropertyWithKey(object, BORDERS_KEY);
  }

  public void removeFromBorders(EOEnterpriseObject object) {
    excludeObjectFromPropertyWithKey(object, BORDERS_KEY);
  }

}
