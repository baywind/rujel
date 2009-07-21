// _VOTeacher.java

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
// $LastChangedRevision: 4733 $ DO NOT EDIT.  Make changes to VOTeacher.java instead.

package net.rujel.vseobuch;

import com.webobjects.eocontrol.*;
import com.webobjects.foundation.*;
import java.math.BigDecimal;

@SuppressWarnings("all")
public abstract class _VOTeacher extends EOGenericRecord {
	public static final String ENTITY_NAME = "VOTeacher";

	// Attributes
	public static final String BIRTH_DATE_KEY = "birthDate";
	public static final String FIRST_NAME_KEY = "firstName";
	public static final String LAST_NAME_KEY = "lastName";
	public static final String SECOND_NAME_KEY = "secondName";
	public static final String SEX_LETTER_KEY = "sexLetter";

	// Relationships
	public static final String PERSON_KEY = "person";

  public NSTimestamp birthDate() {
    return (NSTimestamp) storedValueForKey(BIRTH_DATE_KEY);
  }

  public void setBirthDate(NSTimestamp value) {
    takeStoredValueForKey(value, BIRTH_DATE_KEY);
  }

  public String firstName() {
    return (String) storedValueForKey(FIRST_NAME_KEY);
  }

  public void setFirstName(String value) {
    takeStoredValueForKey(value, FIRST_NAME_KEY);
  }

  public String lastName() {
    return (String) storedValueForKey(LAST_NAME_KEY);
  }

  public void setLastName(String value) {
    takeStoredValueForKey(value, LAST_NAME_KEY);
  }

  public String secondName() {
    return (String) storedValueForKey(SECOND_NAME_KEY);
  }

  public void setSecondName(String value) {
    takeStoredValueForKey(value, SECOND_NAME_KEY);
  }

  public String sexLetter() {
    return (String) storedValueForKey(SEX_LETTER_KEY);
  }

  public void setSexLetter(String value) {
    takeStoredValueForKey(value, SEX_LETTER_KEY);
  }

  public net.rujel.vseobuch.VOTeacher person() {
    return (net.rujel.vseobuch.VOTeacher)storedValueForKey(PERSON_KEY);
  }

  public void setPerson(EOEnterpriseObject value) {
    	takeStoredValueForKey(value, PERSON_KEY);
  }
  
}
