// _VseTeacher.java

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
// $LastChangedRevision: 4733 $ DO NOT EDIT.  Make changes to VseTeacher.java instead.

package net.rujel.vselists;

import com.webobjects.eocontrol.*;
import com.webobjects.foundation.*;
import java.math.BigDecimal;

@SuppressWarnings("all")
public abstract class _VseTeacher extends EOGenericRecord {
	public static final String ENTITY_NAME = "VseTeacher";

	// Attributes
	public static final String DELO_KEY = "delo";
	public static final String ENTER_KEY = "enter";
	public static final String LEAVE_KEY = "leave";
	public static final String POSITION_KEY = "position";

	// Relationships
	public static final String PERSON_KEY = "person";

  public String delo() {
    return (String) storedValueForKey(DELO_KEY);
  }

  public void setDelo(String value) {
    takeStoredValueForKey(value, DELO_KEY);
  }

  public NSTimestamp enter() {
    return (NSTimestamp) storedValueForKey(ENTER_KEY);
  }

  public void setEnter(NSTimestamp value) {
    takeStoredValueForKey(value, ENTER_KEY);
  }

  public NSTimestamp leave() {
    return (NSTimestamp) storedValueForKey(LEAVE_KEY);
  }

  public void setLeave(NSTimestamp value) {
    takeStoredValueForKey(value, LEAVE_KEY);
  }

  public String position() {
    return (String) storedValueForKey(POSITION_KEY);
  }

  public void setPosition(String value) {
    takeStoredValueForKey(value, POSITION_KEY);
  }

  public net.rujel.vselists.VsePerson person() {
    return (net.rujel.vselists.VsePerson)storedValueForKey(PERSON_KEY);
  }

  public void setPerson(EOEnterpriseObject value) {
    	takeStoredValueForKey(value, PERSON_KEY);
  }
  
}
