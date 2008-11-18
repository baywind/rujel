// _Contact.java
// $LastChangedRevision: 4733 $ DO NOT EDIT.  Make changes to Contact.java instead.

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

package net.rujel.contacts;

import com.webobjects.eocontrol.*;
import com.webobjects.foundation.*;
import java.math.BigDecimal;

@SuppressWarnings("all")
public abstract class _Contact extends EOGenericRecord {
	public static final String ENTITY_NAME = "Contact";

	// Attributes
	public static final String CONTACT_KEY = "contact";
	public static final String FLAGS_KEY = "flags";
	public static final String KIND_KEY = "kind";
	public static final String PERS_ID_KEY = "persID";

	// Relationships
	public static final String PERSON_ENTITY_KEY = "personEntity";
	public static final String TYPE_KEY = "type";

  public String contact() {
    return (String) storedValueForKey(CONTACT_KEY);
  }

  public void setContact(String value) {
    takeStoredValueForKey(value, CONTACT_KEY);
  }

  public Integer flags() {
    return (Integer) storedValueForKey(FLAGS_KEY);
  }

  public void setFlags(Integer value) {
    takeStoredValueForKey(value, FLAGS_KEY);
  }

  public String kind() {
    return (String) storedValueForKey(KIND_KEY);
  }

  public void setKind(String value) {
    takeStoredValueForKey(value, KIND_KEY);
  }

  public Integer persID() {
    return (Integer) storedValueForKey(PERS_ID_KEY);
  }

  public void setPersID(Integer value) {
    takeStoredValueForKey(value, PERS_ID_KEY);
  }

  public net.rujel.base.EntityIndex personEntity() {
    return (net.rujel.base.EntityIndex)storedValueForKey(PERSON_ENTITY_KEY);
  }

  public void setPersonEntity(EOEnterpriseObject value) {
    	takeStoredValueForKey(value, PERSON_ENTITY_KEY);
  }
  
  public EOGenericRecord type() {
    return (EOGenericRecord)storedValueForKey(TYPE_KEY);
  }

  public void setType(EOEnterpriseObject value) {
    	takeStoredValueForKey(value, TYPE_KEY);
  }
  
}
