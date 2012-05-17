// _ExtSystem.java

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
// $LastChangedRevision: 4733 $ DO NOT EDIT.  Make changes to ExtSystem.java instead.

package net.rujel.io;

import com.webobjects.eocontrol.*;
import com.webobjects.foundation.*;
import java.math.BigDecimal;

@SuppressWarnings("all")
public abstract class _ExtSystem extends EOGenericRecord {
	public static final String ENTITY_NAME = "ExtSystem";

	// Attributes
	public static final String DESCRIPTION_KEY = "description";
	public static final String PRODUCT_NAME_KEY = "productName";

	// Relationships
	public static final String EXT_BASES_KEY = "extBases";
	public static final String EXT_DATA_KEY = "extData";
	public static final String SYNC_INDEXES_KEY = "syncIndexes";

  public String description() {
    return (String) storedValueForKey(DESCRIPTION_KEY);
  }

  public void setDescription(String value) {
    takeStoredValueForKey(value, DESCRIPTION_KEY);
  }

  public String productName() {
    return (String) storedValueForKey(PRODUCT_NAME_KEY);
  }

  public void setProductName(String value) {
    takeStoredValueForKey(value, PRODUCT_NAME_KEY);
  }

  public NSArray extBases() {
    return (NSArray)storedValueForKey(EXT_BASES_KEY);
  }
 
  public void setExtBases(NSArray value) {
    takeStoredValueForKey(value, EXT_BASES_KEY);
  }
  
  public void addToExtBases(EOEnterpriseObject object) {
    includeObjectIntoPropertyWithKey(object, EXT_BASES_KEY);
  }

  public void removeFromExtBases(EOEnterpriseObject object) {
    excludeObjectFromPropertyWithKey(object, EXT_BASES_KEY);
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
