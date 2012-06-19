// _SyncEvent.java

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
// $LastChangedRevision: 4733 $ DO NOT EDIT.  Make changes to SyncEvent.java instead.

package net.rujel.io;

import com.webobjects.eocontrol.*;
import com.webobjects.foundation.*;
import java.math.BigDecimal;

@SuppressWarnings("all")
public abstract class _SyncEvent extends EOGenericRecord {
	public static final String ENTITY_NAME = "SyncEvent";

	// Attributes
	public static final String EXEC_TIME_KEY = "execTime";
	public static final String RESULT_KEY = "result";
	public static final String SYNC_ENTITY_KEY = "syncEntity";

	// Relationships
	public static final String EXT_BASE_KEY = "extBase";
	public static final String EXT_SYSTEM_KEY = "extSystem";

  public NSTimestamp execTime() {
    return (NSTimestamp) storedValueForKey(EXEC_TIME_KEY);
  }

  public void setExecTime(NSTimestamp value) {
    takeStoredValueForKey(value, EXEC_TIME_KEY);
  }

  public Integer result() {
    return (Integer) storedValueForKey(RESULT_KEY);
  }

  public void setResult(Integer value) {
    takeStoredValueForKey(value, RESULT_KEY);
  }

  public String syncEntity() {
    return (String) storedValueForKey(SYNC_ENTITY_KEY);
  }

  public void setSyncEntity(String value) {
    takeStoredValueForKey(value, SYNC_ENTITY_KEY);
  }

  public net.rujel.io.ExtBase extBase() {
    return (net.rujel.io.ExtBase)storedValueForKey(EXT_BASE_KEY);
  }

  public void setExtBase(EOEnterpriseObject value) {
    	takeStoredValueForKey(value, EXT_BASE_KEY);
  }
  
  public net.rujel.io.ExtSystem extSystem() {
    return (net.rujel.io.ExtSystem)storedValueForKey(EXT_SYSTEM_KEY);
  }

  public void setExtSystem(EOEnterpriseObject value) {
    	takeStoredValueForKey(value, EXT_SYSTEM_KEY);
  }
  
}
