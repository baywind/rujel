// _AutUser.java

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
// $LastChangedRevision: 4733 $ DO NOT EDIT.  Make changes to AutUser.java instead.

package net.rujel.user;

import com.webobjects.eocontrol.*;
import com.webobjects.foundation.*;
import java.math.BigDecimal;

@SuppressWarnings("all")
public abstract class _AutUser extends EOGenericRecord {
	public static final String ENTITY_NAME = "AutUser";

	// Attributes
	public static final String CREDENTIAL_KEY = "credential";
	public static final String PERSON_ID_KEY = "personID";
	public static final String USER_NAME_KEY = "userName";

	// Relationships
	public static final String GROUPS_KEY = "groups";
	public static final String PERSON_ENTITY_KEY = "personEntity";
	public static final String USER_PROPERTIES_KEY = "userProperties";

  public String credential() {
    return (String) storedValueForKey(CREDENTIAL_KEY);
  }

  public void setCredential(String value) {
    takeStoredValueForKey(value, CREDENTIAL_KEY);
  }

  public Integer personID() {
    return (Integer) storedValueForKey(PERSON_ID_KEY);
  }

  public void setPersonID(Integer value) {
    takeStoredValueForKey(value, PERSON_ID_KEY);
  }

  public String userName() {
    return (String) storedValueForKey(USER_NAME_KEY);
  }

  public void setUserName(String value) {
    takeStoredValueForKey(value, USER_NAME_KEY);
  }

  public net.rujel.base.EntityIndex personEntity() {
    return (net.rujel.base.EntityIndex)storedValueForKey(PERSON_ENTITY_KEY);
  }

  public void setPersonEntity(EOEnterpriseObject value) {
    	takeStoredValueForKey(value, PERSON_ENTITY_KEY);
  }
  
  public NSArray groups() {
    return (NSArray)storedValueForKey(GROUPS_KEY);
  }
 
  public void setGroups(NSArray value) {
    takeStoredValueForKey(value, GROUPS_KEY);
  }
  
  public void addToGroups(EOEnterpriseObject object) {
    includeObjectIntoPropertyWithKey(object, GROUPS_KEY);
  }

  public void removeFromGroups(EOEnterpriseObject object) {
    excludeObjectFromPropertyWithKey(object, GROUPS_KEY);
  }

  public NSArray userProperties() {
    return (NSArray)storedValueForKey(USER_PROPERTIES_KEY);
  }
 
  public void setUserProperties(NSArray value) {
    takeStoredValueForKey(value, USER_PROPERTIES_KEY);
  }
  
  public void addToUserProperties(EOEnterpriseObject object) {
    includeObjectIntoPropertyWithKey(object, USER_PROPERTIES_KEY);
  }

  public void removeFromUserProperties(EOEnterpriseObject object) {
    excludeObjectFromPropertyWithKey(object, USER_PROPERTIES_KEY);
  }

}
