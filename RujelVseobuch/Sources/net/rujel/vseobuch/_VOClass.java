// _VOClass.java

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

public abstract class _VOClass extends EOGenericRecord {

    public _VOClass() {
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

    public Number eduYear() {
        return (Number)storedValueForKey("eduYear");
    }

    public void setEduYear(Number aValue) {
        takeStoredValueForKey(aValue, "eduYear");
    }

    public Integer grade() {
        return (Integer)storedValueForKey("grade");
    }

    public void setGrade(Integer aValue) {
        takeStoredValueForKey(aValue, "grade");
    }

    public String name() {
        return (String)storedValueForKey("name");
    }

    public void setName(String aValue) {
        takeStoredValueForKey(aValue, "name");
    }

    public NSArray fullList() {
        return (NSArray)storedValueForKey("fullList");
    }

    public void setFullList(NSArray aValue) {
        takeStoredValueForKey(aValue, "fullList");
    }

    public void addToFullList(net.rujel.vseobuch.VOStudent object) {
	includeObjectIntoPropertyWithKey(object, "fullList");
    }

    public void removeFromFullList(net.rujel.vseobuch.VOStudent object) {
	excludeObjectFromPropertyWithKey(object, "fullList");
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
