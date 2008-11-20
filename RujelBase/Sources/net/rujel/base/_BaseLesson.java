// _BaseLesson.java

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
// $LastChangedRevision: 4733 $ DO NOT EDIT.  Make changes to BaseLesson.java instead.

package net.rujel.base;

import com.webobjects.eocontrol.*;
import com.webobjects.foundation.*;
import java.math.BigDecimal;

@SuppressWarnings("all")
public abstract class _BaseLesson extends EOGenericRecord {
	public static final String ENTITY_NAME = "BaseLesson";

	// Attributes
	public static final String DATE_KEY = "date";
	public static final String NUMBER_KEY = "number";
	public static final String TASK_ID_KEY = "taskID";
	public static final String THEME_KEY = "theme";
	public static final String TITLE_KEY = "title";

	// Relationships
	public static final String COURSE_KEY = "course";
	public static final String NOTES_KEY = "notes";
	public static final String TASK_TEXT_KEY = "taskText";

  public NSTimestamp date() {
    return (NSTimestamp) storedValueForKey(DATE_KEY);
  }

  public void setDate(NSTimestamp value) {
    takeStoredValueForKey(value, DATE_KEY);
  }

  public Integer number() {
    return (Integer) storedValueForKey(NUMBER_KEY);
  }

  public void setNumber(Integer value) {
    takeStoredValueForKey(value, NUMBER_KEY);
  }

  public Integer taskID() {
    return (Integer) storedValueForKey(TASK_ID_KEY);
  }

  public void setTaskID(Integer value) {
    takeStoredValueForKey(value, TASK_ID_KEY);
  }

  public String theme() {
    return (String) storedValueForKey(THEME_KEY);
  }

  public void setTheme(String value) {
    takeStoredValueForKey(value, THEME_KEY);
  }

  public String title() {
    return (String) storedValueForKey(TITLE_KEY);
  }

  public void setTitle(String value) {
    takeStoredValueForKey(value, TITLE_KEY);
  }

  public net.rujel.base.BaseCourse course() {
    return (net.rujel.base.BaseCourse)storedValueForKey(COURSE_KEY);
  }

  public void setCourse(EOEnterpriseObject value) {
    	takeStoredValueForKey(value, COURSE_KEY);
  }
  
  public EOGenericRecord taskText() {
    return (EOGenericRecord)storedValueForKey(TASK_TEXT_KEY);
  }

  public void setTaskText(EOEnterpriseObject value) {
    	takeStoredValueForKey(value, TASK_TEXT_KEY);
  }
  
  public NSArray notes() {
    return (NSArray)storedValueForKey(NOTES_KEY);
  }
 
  public void setNotes(NSArray value) {
    takeStoredValueForKey(value, NOTES_KEY);
  }
  
  public void addToNotes(EOEnterpriseObject object) {
    includeObjectIntoPropertyWithKey(object, NOTES_KEY);
  }

  public void removeFromNotes(EOEnterpriseObject object) {
    excludeObjectFromPropertyWithKey(object, NOTES_KEY);
  }

}
