// _MarkArchive.java

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
// $LastChangedRevision: 4733 $ DO NOT EDIT.  Make changes to MarkArchive.java instead.

package net.rujel.markarchive;

import com.webobjects.eocontrol.*;
import com.webobjects.foundation.*;
import java.math.BigDecimal;

@SuppressWarnings("all")
public abstract class _MarkArchive extends EOGenericRecord {
	public static final String ENTITY_NAME = "MarkArchive";

	// Attributes
	public static final String ACTION_TYPE_KEY = "actionType";
	public static final String DATA_KEY = "data";
	public static final String KEY1_KEY = "key1";
	public static final String KEY2_KEY = "key2";
	public static final String KEY3_KEY = "key3";
	public static final String TIMESTAMP_KEY = "timestamp";
	public static final String USER_KEY = "user";
	public static final String WOSID_KEY = "wosid";

	// Relationships
	public static final String USED_ENTITY_KEY = "usedEntity";

  public Integer actionType() {
    return (Integer) storedValueForKey(ACTION_TYPE_KEY);
  }

  public void setActionType(Integer value) {
    takeStoredValueForKey(value, ACTION_TYPE_KEY);
  }

  public String data() {
    return (String) storedValueForKey(DATA_KEY);
  }

  public void setData(String value) {
    takeStoredValueForKey(value, DATA_KEY);
  }

  public Integer key1() {
    return (Integer) storedValueForKey(KEY1_KEY);
  }

  public void setKey1(Integer value) {
    takeStoredValueForKey(value, KEY1_KEY);
  }

  public Integer key2() {
    return (Integer) storedValueForKey(KEY2_KEY);
  }

  public void setKey2(Integer value) {
    takeStoredValueForKey(value, KEY2_KEY);
  }

  public Integer key3() {
    return (Integer) storedValueForKey(KEY3_KEY);
  }

  public void setKey3(Integer value) {
    takeStoredValueForKey(value, KEY3_KEY);
  }

  public NSTimestamp timestamp() {
    return (NSTimestamp) storedValueForKey(TIMESTAMP_KEY);
  }

  public void setTimestamp(NSTimestamp value) {
    takeStoredValueForKey(value, TIMESTAMP_KEY);
  }

  public String user() {
    return (String) storedValueForKey(USER_KEY);
  }

  public void setUser(String value) {
    takeStoredValueForKey(value, USER_KEY);
  }

  public String wosid() {
    return (String) storedValueForKey(WOSID_KEY);
  }

  public void setWosid(String value) {
    takeStoredValueForKey(value, WOSID_KEY);
  }

  public EOGenericRecord usedEntity() {
    return (EOGenericRecord)storedValueForKey(USED_ENTITY_KEY);
  }

  public void setUsedEntity(EOEnterpriseObject value) {
    	takeStoredValueForKey(value, USED_ENTITY_KEY);
  }
  
}
