// _Grouping.java

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
// $LastChangedRevision: 4733 $ DO NOT EDIT.  Make changes to Grouping.java instead.

package net.rujel.stats;

import com.webobjects.eocontrol.*;
import com.webobjects.foundation.*;
import java.math.BigDecimal;

@SuppressWarnings("all")
public abstract class _Grouping extends EOGenericRecord {
	public static final String ENTITY_NAME = "Grouping";

	// Attributes
	public static final String GID1_KEY = "gid1";
	public static final String GID2_KEY = "gid2";
	public static final String TOTAL_KEY = "total";

	// Relationships
	public static final String DESCRIPTION_KEY = "description";
	public static final String STAT_ENTRIES_KEY = "statEntries";

  public Integer gid1() {
    return (Integer) storedValueForKey(GID1_KEY);
  }

  public void setGid1(Integer value) {
    takeStoredValueForKey(value, GID1_KEY);
  }

  public Integer gid2() {
    return (Integer) storedValueForKey(GID2_KEY);
  }

  public void setGid2(Integer value) {
    takeStoredValueForKey(value, GID2_KEY);
  }

  public Integer total() {
    return (Integer) storedValueForKey(TOTAL_KEY);
  }

  public void setTotal(Integer value) {
    takeStoredValueForKey(value, TOTAL_KEY);
  }

  public net.rujel.stats.Description description() {
    return (net.rujel.stats.Description)storedValueForKey(DESCRIPTION_KEY);
  }

  public void setDescription(EOEnterpriseObject value) {
    	takeStoredValueForKey(value, DESCRIPTION_KEY);
  }
  
  public NSArray statEntries() {
    return (NSArray)storedValueForKey(STAT_ENTRIES_KEY);
  }
 
  public void setStatEntries(NSArray value) {
    takeStoredValueForKey(value, STAT_ENTRIES_KEY);
  }
  
  public void addToStatEntries(EOEnterpriseObject object) {
    includeObjectIntoPropertyWithKey(object, STAT_ENTRIES_KEY);
  }

  public void removeFromStatEntries(EOEnterpriseObject object) {
    excludeObjectFromPropertyWithKey(object, STAT_ENTRIES_KEY);
  }

}
