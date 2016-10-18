// _Subject.java

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
// $LastChangedRevision: 4733 $ DO NOT EDIT.  Make changes to Subject.java instead.

package net.rujel.eduplan;

import com.webobjects.eocontrol.*;
import com.webobjects.foundation.*;
import java.math.BigDecimal;

@SuppressWarnings("all")
public abstract class _Subject extends EOGenericRecord {
	public static final String ENTITY_NAME = "Subject";

	// Attributes
	public static final String EXT_NAME_KEY = "extName";
	public static final String FLAGS_KEY = "flags";
	public static final String FULL_NAME_KEY = "fullName";
	public static final String NORMAL_GROUP_KEY = "normalGroup";
	public static final String NUM_KEY = "num";
	public static final String SUBGROUPS_KEY = "subgroups";
	public static final String SUBJECT_KEY = "subject";

	// Relationships
	public static final String AREA_KEY = "area";
	public static final String PLAN_HOURS_KEY = "planHours";
	public static final String SECTION_KEY = "section";
	public static final String SUBJECT_GROUP_KEY = "subjectGroup";

  public String extName() {
    return (String) storedValueForKey(EXT_NAME_KEY);
  }

  public void setExtName(String value) {
    takeStoredValueForKey(value, EXT_NAME_KEY);
  }

  public Integer flags() {
    return (Integer) storedValueForKey(FLAGS_KEY);
  }

  public void setFlags(Integer value) {
    takeStoredValueForKey(value, FLAGS_KEY);
  }

  public String fullName() {
    return (String) storedValueForKey(FULL_NAME_KEY);
  }

  public void setFullName(String value) {
    takeStoredValueForKey(value, FULL_NAME_KEY);
  }

  public Integer normalGroup() {
    return (Integer) storedValueForKey(NORMAL_GROUP_KEY);
  }

  public void setNormalGroup(Integer value) {
    takeStoredValueForKey(value, NORMAL_GROUP_KEY);
  }

  public Integer num() {
    return (Integer) storedValueForKey(NUM_KEY);
  }

  public void setNum(Integer value) {
    takeStoredValueForKey(value, NUM_KEY);
  }

  public Integer subgroups() {
    return (Integer) storedValueForKey(SUBGROUPS_KEY);
  }

  public void setSubgroups(Integer value) {
    takeStoredValueForKey(value, SUBGROUPS_KEY);
  }

  public String subject() {
    return (String) storedValueForKey(SUBJECT_KEY);
  }

  public void setSubject(String value) {
    takeStoredValueForKey(value, SUBJECT_KEY);
  }

  public EOGenericRecord area() {
    return (EOGenericRecord)storedValueForKey(AREA_KEY);
  }

  public void setArea(EOEnterpriseObject value) {
    	takeStoredValueForKey(value, AREA_KEY);
  }
  
  public net.rujel.base.SchoolSection section() {
    return (net.rujel.base.SchoolSection)storedValueForKey(SECTION_KEY);
  }

  public void setSection(EOEnterpriseObject value) {
    	takeStoredValueForKey(value, SECTION_KEY);
  }
  
  public net.rujel.eduplan.SubjectGroup subjectGroup() {
    return (net.rujel.eduplan.SubjectGroup)storedValueForKey(SUBJECT_GROUP_KEY);
  }

  public void setSubjectGroup(EOEnterpriseObject value) {
    	takeStoredValueForKey(value, SUBJECT_GROUP_KEY);
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
