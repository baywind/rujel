// _PlanHours.java

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
// $LastChangedRevision: 4733 $ DO NOT EDIT.  Make changes to PlanHours.java instead.

package net.rujel.eduplan;

import com.webobjects.eocontrol.*;
import com.webobjects.foundation.*;
import java.math.BigDecimal;

@SuppressWarnings("all")
public abstract class _PlanHours extends EOGenericRecord {
	public static final String ENTITY_NAME = "PlanHours";

	// Attributes
	public static final String GRADE_KEY = "grade";
	public static final String TOTAL_HOURS_KEY = "totalHours";
	public static final String WEEKLY_HOURS_KEY = "weeklyHours";

	// Relationships
	public static final String EDU_SUBJECT_KEY = "eduSubject";
	public static final String PLAN_CYCLE_KEY = "planCycle";
	public static final String SECTION_KEY = "section";

  public Integer grade() {
    return (Integer) storedValueForKey(GRADE_KEY);
  }

  public void setGrade(Integer value) {
    takeStoredValueForKey(value, GRADE_KEY);
  }

  public Integer totalHours() {
    return (Integer) storedValueForKey(TOTAL_HOURS_KEY);
  }

  public void setTotalHours(Integer value) {
    takeStoredValueForKey(value, TOTAL_HOURS_KEY);
  }

  public Integer weeklyHours() {
    return (Integer) storedValueForKey(WEEKLY_HOURS_KEY);
  }

  public void setWeeklyHours(Integer value) {
    takeStoredValueForKey(value, WEEKLY_HOURS_KEY);
  }

  public net.rujel.eduplan.Subject eduSubject() {
    return (net.rujel.eduplan.Subject)storedValueForKey(EDU_SUBJECT_KEY);
  }

  public void setEduSubject(EOEnterpriseObject value) {
    	takeStoredValueForKey(value, EDU_SUBJECT_KEY);
  }
  
  public net.rujel.eduplan.PlanCycle planCycle() {
    return (net.rujel.eduplan.PlanCycle)storedValueForKey(PLAN_CYCLE_KEY);
  }

  public void setPlanCycle(EOEnterpriseObject value) {
    	takeStoredValueForKey(value, PLAN_CYCLE_KEY);
  }
  
  public net.rujel.base.SchoolSection section() {
    return (net.rujel.base.SchoolSection)storedValueForKey(SECTION_KEY);
  }

  public void setSection(EOEnterpriseObject value) {
    	takeStoredValueForKey(value, SECTION_KEY);
  }
  
}
