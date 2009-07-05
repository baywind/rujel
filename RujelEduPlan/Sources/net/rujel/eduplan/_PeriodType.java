// _PeriodType.java

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
// $LastChangedRevision: 4733 $ DO NOT EDIT.  Make changes to PeriodType.java instead.

package net.rujel.eduresults;

import com.webobjects.eocontrol.*;
import com.webobjects.foundation.*;
import java.math.BigDecimal;

@SuppressWarnings("all")
public abstract class _PeriodType extends EOGenericRecord {
	public static final String ENTITY_NAME = "PeriodType";

	// Attributes
	public static final String ARCHIVE_SINCE_KEY = "archiveSince";
	public static final String COLOR_KEY = "color";
	public static final String IN_YEAR_COUNT_KEY = "inYearCount";
	public static final String NAME_KEY = "name";
	public static final String TITLE_KEY = "title";

	// Relationships
	public static final String TEMPLATES_KEY = "templates";

  public Integer archiveSince() {
    return (Integer) storedValueForKey(ARCHIVE_SINCE_KEY);
  }

  public void setArchiveSince(Integer value) {
    takeStoredValueForKey(value, ARCHIVE_SINCE_KEY);
  }

  public String color() {
    return (String) storedValueForKey(COLOR_KEY);
  }

  public void setColor(String value) {
    takeStoredValueForKey(value, COLOR_KEY);
  }

  public Integer inYearCount() {
    return (Integer) storedValueForKey(IN_YEAR_COUNT_KEY);
  }

  public void setInYearCount(Integer value) {
    takeStoredValueForKey(value, IN_YEAR_COUNT_KEY);
  }

  public String name() {
    return (String) storedValueForKey(NAME_KEY);
  }

  public void setName(String value) {
    takeStoredValueForKey(value, NAME_KEY);
  }

  public String title() {
    return (String) storedValueForKey(TITLE_KEY);
  }

  public void setTitle(String value) {
    takeStoredValueForKey(value, TITLE_KEY);
  }

  public NSArray templates() {
    return (NSArray)storedValueForKey(TEMPLATES_KEY);
  }
 
  public void setTemplates(NSArray value) {
    takeStoredValueForKey(value, TEMPLATES_KEY);
  }
  
  public void addToTemplates(EOEnterpriseObject object) {
    includeObjectIntoPropertyWithKey(object, TEMPLATES_KEY);
  }

  public void removeFromTemplates(EOEnterpriseObject object) {
    excludeObjectFromPropertyWithKey(object, TEMPLATES_KEY);
  }

}
