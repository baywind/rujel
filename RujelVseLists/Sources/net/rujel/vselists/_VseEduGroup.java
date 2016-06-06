// _VseEduGroup.java

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
// $LastChangedRevision: 4733 $ DO NOT EDIT.  Make changes to VseEduGroup.java instead.

package net.rujel.vselists;

import com.webobjects.eocontrol.*;
import com.webobjects.foundation.*;
import java.math.BigDecimal;

@SuppressWarnings("all")
public abstract class _VseEduGroup extends EOGenericRecord {
	public static final String ENTITY_NAME = "VseEduGroup";

	// Attributes
	public static final String ABS_GRADE_KEY = "absGrade";
	public static final String FIRST_YEAR_KEY = "firstYear";
	public static final String FLAGS_KEY = "flags";
	public static final String LAST_YEAR_KEY = "lastYear";
	public static final String TITLE_KEY = "title";

	// Relationships
	public static final String LISTS_KEY = "lists";
	public static final String SECTION_KEY = "section";
	public static final String VSE_TUTORS_KEY = "vseTutors";

  public Integer absGrade() {
    return (Integer) storedValueForKey(ABS_GRADE_KEY);
  }

  public void setAbsGrade(Integer value) {
    takeStoredValueForKey(value, ABS_GRADE_KEY);
  }

  public Integer firstYear() {
    return (Integer) storedValueForKey(FIRST_YEAR_KEY);
  }

  public void setFirstYear(Integer value) {
    takeStoredValueForKey(value, FIRST_YEAR_KEY);
  }

  public Integer flags() {
    return (Integer) storedValueForKey(FLAGS_KEY);
  }

  public void setFlags(Integer value) {
    takeStoredValueForKey(value, FLAGS_KEY);
  }

  public Integer lastYear() {
    return (Integer) storedValueForKey(LAST_YEAR_KEY);
  }

  public void setLastYear(Integer value) {
    takeStoredValueForKey(value, LAST_YEAR_KEY);
  }

  public String title() {
    return (String) storedValueForKey(TITLE_KEY);
  }

  public void setTitle(String value) {
    takeStoredValueForKey(value, TITLE_KEY);
  }

  public net.rujel.base.SchoolSection section() {
    return (net.rujel.base.SchoolSection)storedValueForKey(SECTION_KEY);
  }

  public void setSection(EOEnterpriseObject value) {
    	takeStoredValueForKey(value, SECTION_KEY);
  }
  
  public NSArray lists() {
    return (NSArray)storedValueForKey(LISTS_KEY);
  }
 
  public void setLists(NSArray value) {
    takeStoredValueForKey(value, LISTS_KEY);
  }
  
  public void addToLists(EOEnterpriseObject object) {
    includeObjectIntoPropertyWithKey(object, LISTS_KEY);
  }

  public void removeFromLists(EOEnterpriseObject object) {
    excludeObjectFromPropertyWithKey(object, LISTS_KEY);
  }

  public NSArray vseTutors() {
    return (NSArray)storedValueForKey(VSE_TUTORS_KEY);
  }
 
  public void setVseTutors(NSArray value) {
    takeStoredValueForKey(value, VSE_TUTORS_KEY);
  }
  
  public void addToVseTutors(EOEnterpriseObject object) {
    includeObjectIntoPropertyWithKey(object, VSE_TUTORS_KEY);
  }

  public void removeFromVseTutors(EOEnterpriseObject object) {
    excludeObjectFromPropertyWithKey(object, VSE_TUTORS_KEY);
  }

}
