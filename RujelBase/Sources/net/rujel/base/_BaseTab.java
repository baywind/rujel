// _BaseTab.java

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
// $LastChangedRevision: 4733 $ DO NOT EDIT.  Make changes to BaseTab.java instead.

package net.rujel.base;

import com.webobjects.eocontrol.*;
import com.webobjects.foundation.*;
import java.math.BigDecimal;

@SuppressWarnings("all")
public abstract class _BaseTab extends EOGenericRecord {
	public static final String ENTITY_NAME = "BaseTab";

	// Attributes
	public static final String FIRST_LESSON_NUMBER_KEY = "firstLessonNumber";

	// Relationships
	public static final String COURSE_KEY = "course";
	public static final String FOR_ENTITY_KEY = "forEntity";

  public Integer firstLessonNumber() {
    return (Integer) storedValueForKey(FIRST_LESSON_NUMBER_KEY);
  }

  public void setFirstLessonNumber(Integer value) {
    takeStoredValueForKey(value, FIRST_LESSON_NUMBER_KEY);
  }

  public net.rujel.base.BaseCourse course() {
    return (net.rujel.base.BaseCourse)storedValueForKey(COURSE_KEY);
  }

  public void setCourse(EOEnterpriseObject value) {
    	takeStoredValueForKey(value, COURSE_KEY);
  }
  
  public net.rujel.base.EntityIndex forEntity() {
    return (net.rujel.base.EntityIndex)storedValueForKey(FOR_ENTITY_KEY);
  }

  public void setForEntity(EOEnterpriseObject value) {
    	takeStoredValueForKey(value, FOR_ENTITY_KEY);
  }
  
}
