// _Indexer.java

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
// $LastChangedRevision: 4733 $ DO NOT EDIT.  Make changes to Indexer.java instead.

package net.rujel.base;

import com.webobjects.eocontrol.*;
import com.webobjects.foundation.*;
import java.math.BigDecimal;

@SuppressWarnings("all")
public abstract class _Indexer extends EOGenericRecord {
	public static final String ENTITY_NAME = "Indexer";

	// Attributes
	public static final String COMMON_STRING_KEY = "commonString";
	public static final String DEFAULT_VALUE_KEY = "defaultValue";
	public static final String TITLE_KEY = "title";
	public static final String TYPE_KEY = "type";

	// Relationships
	public static final String COMMENT_EO_KEY = "commentEO";
	public static final String INDEX_ROWS_KEY = "indexRows";

  public String commonString() {
    return (String) storedValueForKey(COMMON_STRING_KEY);
  }

  public void setCommonString(String value) {
    takeStoredValueForKey(value, COMMON_STRING_KEY);
  }

  public String defaultValue() {
    return (String) storedValueForKey(DEFAULT_VALUE_KEY);
  }

  public void setDefaultValue(String value) {
    takeStoredValueForKey(value, DEFAULT_VALUE_KEY);
  }

  public String title() {
    return (String) storedValueForKey(TITLE_KEY);
  }

  public void setTitle(String value) {
    takeStoredValueForKey(value, TITLE_KEY);
  }

  public Integer type() {
    return (Integer) storedValueForKey(TYPE_KEY);
  }

  public void setType(Integer value) {
    takeStoredValueForKey(value, TYPE_KEY);
  }

  public EOGenericRecord commentEO() {
    return (EOGenericRecord)storedValueForKey(COMMENT_EO_KEY);
  }

  public void setCommentEO(EOEnterpriseObject value) {
    	takeStoredValueForKey(value, COMMENT_EO_KEY);
  }
  
  public NSArray indexRows() {
    return (NSArray)storedValueForKey(INDEX_ROWS_KEY);
  }
 
  public void setIndexRows(NSArray value) {
    takeStoredValueForKey(value, INDEX_ROWS_KEY);
  }
  
  public void addToIndexRows(EOEnterpriseObject object) {
    includeObjectIntoPropertyWithKey(object, INDEX_ROWS_KEY);
  }

  public void removeFromIndexRows(EOEnterpriseObject object) {
    excludeObjectFromPropertyWithKey(object, INDEX_ROWS_KEY);
  }

}
