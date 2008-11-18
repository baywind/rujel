// _Indexer.java
// $LastChangedRevision: 4733 $ DO NOT EDIT.  Make changes to Indexer.java instead.

/*
 * Copyright (c) 2008, Gennady & Michael Kushnir
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 * 
 * 	•	Redistributions of source code must retain the above copyright notice, this
 * 		list of conditions and the following disclaimer.
 * 	•	Redistributions in binary form must reproduce the above copyright notice,
 * 		this list of conditions and the following disclaimer in the documentation
 * 		and/or other materials provided with the distribution.
 * 	•	Neither the name of the RUJEL nor the names of its contributors may be used
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

package net.rujel.criterial;

import com.webobjects.eocontrol.*;
import com.webobjects.foundation.*;
import java.math.BigDecimal;

@SuppressWarnings("all")
public abstract class _Indexer extends EOGenericRecord {
	public static final String ENTITY_NAME = "Indexer";

	// Attributes
	public static final String TITLE_KEY = "title";
	public static final String TYPE_KEY = "type";

	// Relationships
	public static final String INDEX_ROW_KEY = "indexRow";

  public String title() {
    return (String) storedValueForKey(TITLE_KEY);
  }

  public void setTitle(String value) {
    takeStoredValueForKey(value, TITLE_KEY);
  }

  public Byte type() {
    return (Byte) storedValueForKey(TYPE_KEY);
  }

  public void setType(Byte value) {
    takeStoredValueForKey(value, TYPE_KEY);
  }

  public NSArray indexRow() {
    return (NSArray)storedValueForKey(INDEX_ROW_KEY);
  }
 
  public void setIndexRow(NSArray value) {
    takeStoredValueForKey(value, INDEX_ROW_KEY);
  }
  
  public void addToIndexRow(EOEnterpriseObject object) {
    includeObjectIntoPropertyWithKey(object, INDEX_ROW_KEY);
  }

  public void removeFromIndexRow(EOEnterpriseObject object) {
    excludeObjectFromPropertyWithKey(object, INDEX_ROW_KEY);
  }

}
