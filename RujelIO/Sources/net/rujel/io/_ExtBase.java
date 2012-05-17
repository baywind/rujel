// _ExtBase.java

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
// $LastChangedRevision: 4733 $ DO NOT EDIT.  Make changes to ExtBase.java instead.

package net.rujel.io;

import com.webobjects.eocontrol.*;
import com.webobjects.foundation.*;
import java.math.BigDecimal;

@SuppressWarnings("all")
public abstract class _ExtBase extends EOGenericRecord {
	public static final String ENTITY_NAME = "ExtBase";

	// Attributes
	public static final String BASE_ID_KEY = "baseID";
	public static final String TITLE_KEY = "title";

	// Relationships
	public static final String EXT_DATA_KEY = "extData";
	public static final String EXT_SYSTEM_KEY = "extSystem";
	public static final String SYNC_INDEXES_KEY = "syncIndexes";

  public String baseID() {
    return (String) storedValueForKey(BASE_ID_KEY);
  }

  public void setBaseID(String value) {
    takeStoredValueForKey(value, BASE_ID_KEY);
  }

  public String title() {
    return (String) storedValueForKey(TITLE_KEY);
  }

  public void setTitle(String value) {
    takeStoredValueForKey(value, TITLE_KEY);
  }

  public net.rujel.io.ExtSystem extSystem() {
    return (net.rujel.io.ExtSystem)storedValueForKey(EXT_SYSTEM_KEY);
  }

  public void setExtSystem(EOEnterpriseObject value) {
    	takeStoredValueForKey(value, EXT_SYSTEM_KEY);
  }
  
  public NSArray extData() {
    return (NSArray)storedValueForKey(EXT_DATA_KEY);
  }
 
  public void setExtData(NSArray value) {
    takeStoredValueForKey(value, EXT_DATA_KEY);
  }
  
  public void addToExtData(EOEnterpriseObject object) {
    includeObjectIntoPropertyWithKey(object, EXT_DATA_KEY);
  }

  public void removeFromExtData(EOEnterpriseObject object) {
    excludeObjectFromPropertyWithKey(object, EXT_DATA_KEY);
  }

  public NSArray syncIndexes() {
    return (NSArray)storedValueForKey(SYNC_INDEXES_KEY);
  }
 
  public void setSyncIndexes(NSArray value) {
    takeStoredValueForKey(value, SYNC_INDEXES_KEY);
  }
  
  public void addToSyncIndexes(EOEnterpriseObject object) {
    includeObjectIntoPropertyWithKey(object, SYNC_INDEXES_KEY);
  }

  public void removeFromSyncIndexes(EOEnterpriseObject object) {
    excludeObjectFromPropertyWithKey(object, SYNC_INDEXES_KEY);
  }

}
