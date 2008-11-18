// _HomeTask.java
// $LastChangedRevision: 4733 $ DO NOT EDIT.  Make changes to HomeTask.java instead.

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

package net.rujel.criterial;

import com.webobjects.eocontrol.*;
import com.webobjects.foundation.*;
import java.math.BigDecimal;

@SuppressWarnings("all")
public abstract class _HomeTask extends EOGenericRecord {
	public static final String ENTITY_NAME = "HomeTask";

	// Attributes
	public static final String DATE_PUBLISHED_KEY = "datePublished";
	public static final String DEADLINE_KEY = "deadline";
	public static final String HREF_KEY = "href";
	public static final String MATERIALS_KEY = "materials";
	public static final String TASK_KEY = "task";
	public static final String TIME_REQUIRED_KEY = "timeRequired";

	// Relationships
	public static final String WORK_KEY = "work";

  public NSTimestamp datePublished() {
    return (NSTimestamp) storedValueForKey(DATE_PUBLISHED_KEY);
  }

  public void setDatePublished(NSTimestamp value) {
    takeStoredValueForKey(value, DATE_PUBLISHED_KEY);
  }

  public NSTimestamp deadline() {
    return (NSTimestamp) storedValueForKey(DEADLINE_KEY);
  }

  public void setDeadline(NSTimestamp value) {
    takeStoredValueForKey(value, DEADLINE_KEY);
  }

  public String href() {
    return (String) storedValueForKey(HREF_KEY);
  }

  public void setHref(String value) {
    takeStoredValueForKey(value, HREF_KEY);
  }

  public String materials() {
    return (String) storedValueForKey(MATERIALS_KEY);
  }

  public void setMaterials(String value) {
    takeStoredValueForKey(value, MATERIALS_KEY);
  }

  public String task() {
    return (String) storedValueForKey(TASK_KEY);
  }

  public void setTask(String value) {
    takeStoredValueForKey(value, TASK_KEY);
  }

  public Integer timeRequired() {
    return (Integer) storedValueForKey(TIME_REQUIRED_KEY);
  }

  public void setTimeRequired(Integer value) {
    takeStoredValueForKey(value, TIME_REQUIRED_KEY);
  }

  public net.rujel.criterial.Work work() {
    return (net.rujel.criterial.Work)storedValueForKey(WORK_KEY);
  }

  public void setWork(EOEnterpriseObject value) {
    if (value == null) {
    	net.rujel.criterial.Work oldValue = work();
    	if (oldValue != null) {
    		removeObjectFromBothSidesOfRelationshipWithKey(oldValue, WORK_KEY);
      }
    } else {
    	addObjectToBothSidesOfRelationshipWithKey(value, WORK_KEY);
    }
  }
  
}
