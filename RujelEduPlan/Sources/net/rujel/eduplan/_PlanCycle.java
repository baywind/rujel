// _PlanCycle.java

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
// $LastChangedRevision: 4733 $ DO NOT EDIT.  Make changes to PlanCycle.java instead.

package net.rujel.eduplan;

import com.webobjects.eocontrol.*;
import com.webobjects.foundation.*;
import java.math.BigDecimal;

@SuppressWarnings("all")
public abstract class _PlanCycle extends EOGenericRecord {
	public static final String ENTITY_NAME = "PlanCycle";

	// Attributes
	public static final String GRADE_KEY = "grade";
	public static final String SCHOOL_KEY = "school";
	public static final String SECTION_KEY = "section";

	// Relationships
	public static final String PLAN_HOURS_KEY = "planHours";
	public static final String SUBJECT_EO_KEY = "subjectEO";

  public Integer grade() {
    return (Integer) storedValueForKey(GRADE_KEY);
  }

  public void setGrade(Integer value) {
    takeStoredValueForKey(value, GRADE_KEY);
  }

  public Integer school() {
    return (Integer) storedValueForKey(SCHOOL_KEY);
  }

  public void setSchool(Integer value) {
    takeStoredValueForKey(value, SCHOOL_KEY);
  }

  public Integer section() {
    return (Integer) storedValueForKey(SECTION_KEY);
  }

  public void setSection(Integer value) {
    takeStoredValueForKey(value, SECTION_KEY);
  }

  public net.rujel.eduplan.Subject subjectEO() {
    return (net.rujel.eduplan.Subject)storedValueForKey(SUBJECT_EO_KEY);
  }

  public void setSubjectEO(EOEnterpriseObject value) {
    	takeStoredValueForKey(value, SUBJECT_EO_KEY);
  }
  
  public NSArray planHours() {
    return (NSArray)storedValueForKey(PLAN_HOURS_KEY);
  }
 
  public void setPlanHours(NSArray value) {
    takeStoredValueForKey(value, PLAN_HOURS_KEY);
  }
  
  public void addToPlanHours(EOEnterpriseObject object) {
    includeObjectIntoPropertyWithKey(object, PLAN_HOURS_KEY);
  }

  public void removeFromPlanHours(EOEnterpriseObject object) {
    excludeObjectFromPropertyWithKey(object, PLAN_HOURS_KEY);
  }

}
