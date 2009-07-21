// _VOClass.java

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
// $LastChangedRevision: 4733 $ DO NOT EDIT.  Make changes to VOClass.java instead.

package net.rujel.vseobuch;

import com.webobjects.eocontrol.*;
import com.webobjects.foundation.*;
import java.math.BigDecimal;

@SuppressWarnings("all")
public abstract class _VOClass extends EOGenericRecord {
	public static final String ENTITY_NAME = "VOClass";

	// Attributes
	public static final String EDU_YEAR_KEY = "eduYear";
	public static final String GRADE_KEY = "grade";
	public static final String NAME_KEY = "name";

	// Relationships
	public static final String FULL_LIST_KEY = "fullList";
	public static final String GROUPING_KEY = "grouping";

  public Integer eduYear() {
    return (Integer) storedValueForKey(EDU_YEAR_KEY);
  }

  public void setEduYear(Integer value) {
    takeStoredValueForKey(value, EDU_YEAR_KEY);
  }

  public Integer grade() {
    return (Integer) storedValueForKey(GRADE_KEY);
  }

  public void setGrade(Integer value) {
    takeStoredValueForKey(value, GRADE_KEY);
  }

  public String name() {
    return (String) storedValueForKey(NAME_KEY);
  }

  public void setName(String value) {
    takeStoredValueForKey(value, NAME_KEY);
  }

  public NSArray fullList() {
    return (NSArray)storedValueForKey(FULL_LIST_KEY);
  }
 
  public void setFullList(NSArray value) {
    takeStoredValueForKey(value, FULL_LIST_KEY);
  }
  
  public void addToFullList(EOEnterpriseObject object) {
    includeObjectIntoPropertyWithKey(object, FULL_LIST_KEY);
  }

  public void removeFromFullList(EOEnterpriseObject object) {
    excludeObjectFromPropertyWithKey(object, FULL_LIST_KEY);
  }

  public NSArray grouping() {
    return (NSArray)storedValueForKey(GROUPING_KEY);
  }
 
  public void setGrouping(NSArray value) {
    takeStoredValueForKey(value, GROUPING_KEY);
  }
  
  public void addToGrouping(EOEnterpriseObject object) {
    includeObjectIntoPropertyWithKey(object, GROUPING_KEY);
  }

  public void removeFromGrouping(EOEnterpriseObject object) {
    excludeObjectFromPropertyWithKey(object, GROUPING_KEY);
  }

}
