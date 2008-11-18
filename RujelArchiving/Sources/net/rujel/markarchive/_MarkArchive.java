// _MarkArchive.java

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

package net.rujel.markarchive;


import com.webobjects.foundation.*;
import com.webobjects.eocontrol.*;

public abstract class _MarkArchive extends EOGenericRecord {

    public _MarkArchive() {
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

    public String data() {
        return (String)storedValueForKey("data");
    }

    public void setData(String aValue) {
        takeStoredValueForKey(aValue, "data");
    }

    public Number key1() {
        return (Number)storedValueForKey("key1");
    }

    public void setKey1(Number aValue) {
        takeStoredValueForKey(aValue, "key1");
    }

    public Number key2() {
        return (Number)storedValueForKey("key2");
    }

    public void setKey2(Number aValue) {
        takeStoredValueForKey(aValue, "key2");
    }

    public Number key3() {
        return (Number)storedValueForKey("key3");
    }

    public void setKey3(Number aValue) {
        takeStoredValueForKey(aValue, "key3");
    }

    public NSTimestamp timestamp() {
        return (NSTimestamp)storedValueForKey("timestamp");
    }

    public void setTimestamp(NSTimestamp aValue) {
        takeStoredValueForKey(aValue, "timestamp");
    }

    public String user() {
        return (String)storedValueForKey("user");
    }

    public void setUser(String aValue) {
        takeStoredValueForKey(aValue, "user");
    }

    public String wosid() {
        return (String)storedValueForKey("wosid");
    }

    public void setWosid(String aValue) {
        takeStoredValueForKey(aValue, "wosid");
    }

    public EOEnterpriseObject usedEntity() {
        return (EOEnterpriseObject)storedValueForKey("usedEntity");
    }

    public void setUsedEntity(EOEnterpriseObject aValue) {
        takeStoredValueForKey(aValue, "usedEntity");
    }
}
