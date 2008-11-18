// _PrimitiveEduCycle.java
// $LastChangedRevision: 4733 $ DO NOT EDIT.  Make changes to PrimitiveEduCycle.java instead.

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

package net.rujel.base;

import com.webobjects.eocontrol.*;
import com.webobjects.foundation.*;
import java.math.BigDecimal;

@SuppressWarnings("all")
public abstract class _PrimitiveEduCycle extends EOGenericRecord {
	public static final String ENTITY_NAME = "PrimitiveEduCycle";

	// Attributes
	public static final String GRADE_KEY = "grade";
	public static final String SUBGROUPS_KEY = "subgroups";
	public static final String SUBJECT_KEY = "subject";

	// Relationships
	public static final String COURSES_KEY = "courses";

  public Integer grade() {
    return (Integer) storedValueForKey(GRADE_KEY);
  }

  public void setGrade(Integer value) {
    takeStoredValueForKey(value, GRADE_KEY);
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

  public NSArray courses() {
    return (NSArray)storedValueForKey(COURSES_KEY);
  }
 
  public void setCourses(NSArray value) {
    takeStoredValueForKey(value, COURSES_KEY);
  }
  
  public void addToCourses(EOEnterpriseObject object) {
    includeObjectIntoPropertyWithKey(object, COURSES_KEY);
  }

  public void removeFromCourses(EOEnterpriseObject object) {
    excludeObjectFromPropertyWithKey(object, COURSES_KEY);
  }

}
