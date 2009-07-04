// _CriteriaSet.java

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
// $LastChangedRevision: 4733 $ DO NOT EDIT.  Make changes to CriteriaSet.java instead.

package net.rujel.criterial;

import com.webobjects.eocontrol.*;
import com.webobjects.foundation.*;
import java.math.BigDecimal;

@SuppressWarnings("all")
public abstract class _CriteriaSet extends EOGenericRecord {
	public static final String ENTITY_NAME = "CriteriaSet";

	// Attributes
	public static final String COMMENT_KEY = "comment";

	// Relationships
	public static final String CRITERIA_KEY = "criteria";

  public String comment() {
    return (String) storedValueForKey(COMMENT_KEY);
  }

  public void setComment(String value) {
    takeStoredValueForKey(value, COMMENT_KEY);
  }

  public NSArray criteria() {
    return (NSArray)storedValueForKey(CRITERIA_KEY);
  }
 
  public void setCriteria(NSArray value) {
    takeStoredValueForKey(value, CRITERIA_KEY);
  }
  
  public void addToCriteria(EOEnterpriseObject object) {
    includeObjectIntoPropertyWithKey(object, CRITERIA_KEY);
  }

  public void removeFromCriteria(EOEnterpriseObject object) {
    excludeObjectFromPropertyWithKey(object, CRITERIA_KEY);
  }

}
