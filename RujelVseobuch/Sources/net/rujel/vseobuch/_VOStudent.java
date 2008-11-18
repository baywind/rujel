// _VOStudent.java

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

package net.rujel.vseobuch;


import com.webobjects.foundation.*;
import com.webobjects.eocontrol.*;

public abstract class _VOStudent extends EOGenericRecord {

    public _VOStudent() {
        super();
    }

/*
    // If you add instance variables to store property values you
    // should add empty implementions of the Serialization methods
    // to avoid unnecessary overhead (the properties will be
    // serialized for you in the superclass).
    private void writeObject(java.io.ObjectOutputStream out) throws java.io.IOException {
    }

    private void readObject(java.io.ObjectInputStream in) throws java.io.IOException, java.lang.ClassNotFoundException {
    }
*/

    public NSTimestamp birthDate() {
        return (NSTimestamp)storedValueForKey("birthDate");
    }

    public void setBirthDate(NSTimestamp aValue) {
        takeStoredValueForKey(aValue, "birthDate");
    }

    public String firstName() {
        return (String)storedValueForKey("firstName");
    }

    public void setFirstName(String aValue) {
        takeStoredValueForKey(aValue, "firstName");
    }

    public String lastName() {
        return (String)storedValueForKey("lastName");
    }

    public void setLastName(String aValue) {
        takeStoredValueForKey(aValue, "lastName");
    }

    public String secondName() {
        return (String)storedValueForKey("secondName");
    }

    public void setSecondName(String aValue) {
        takeStoredValueForKey(aValue, "secondName");
    }

    public Boolean sex() {
        return (Boolean)storedValueForKey("sex");
    }

    public void setSex(Boolean aValue) {
        takeStoredValueForKey(aValue, "sex");
    }

    public net.rujel.vseobuch.VOStudent person() {
        return (net.rujel.vseobuch.VOStudent)storedValueForKey("person");
    }

    public void setPerson(net.rujel.vseobuch.VOStudent aValue) {
        takeStoredValueForKey(aValue, "person");
    }

    public NSArray grouping() {
        return (NSArray)storedValueForKey("grouping");
    }

    public void setGrouping(NSArray aValue) {
        takeStoredValueForKey(aValue, "grouping");
    }

    public void addToGrouping(EOEnterpriseObject object) {
	includeObjectIntoPropertyWithKey(object, "grouping");
    }

    public void removeFromGrouping(EOEnterpriseObject object) {
	excludeObjectFromPropertyWithKey(object, "grouping");
    }
}
