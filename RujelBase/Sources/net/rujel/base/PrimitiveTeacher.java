// PrimitiveTeacher.java

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

package net.rujel.base;

import net.rujel.interfaces.*;
import net.rujel.reusables.*;
import net.rujel.auth.UseAccess;
import com.webobjects.foundation.*;

public class PrimitiveTeacher extends _PrimitiveTeacher implements Teacher, Person, UseAccess
{
    public PrimitiveTeacher() {
        super();
    }
	
	public static final int MaxNameLength = 28;

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
	
	private transient NamedFlags _access;
	public NamedFlags access() {
		if(_access == null) {
			_access = StaticImplementation.access(this,accessKeys);
		}
		return _access.immutableClone();
	}
/*	
	private transient NSMutableDictionary _schemeCache;
	public NamedFlags schemeAccess(String schemePath) {
		NamedFlags result = null;
		if(_schemeCache == null) {
			_schemeCache = new NSMutableDictionary();
		} else {
			result = (NamedFlags)_schemeCache.objectForKey(schemePath);
		}
		if(result == null) {
			result = StaticImplementation.schemeAccess(this,schemePath);
			if(result == null) {
				result = DegenerateFlags.ALL_FALSE;
			}
			_schemeCache.setObjectForKey(result,schemePath);
		}
		if(result == DegenerateFlags.ALL_FALSE)
			return null;
		else return result;
	}
*/
	
	public boolean isOwned() {
		return StaticImplementation.isOwned(this);
	}
	

	public PrimitiveTeacher person() {
		return this;
	}
	
	public String initials() {
		return Person.Utility.initials(this);//new String (new char[] {firstName().charAt(0),'.',' ',secondName().charAt(0),'.'});
	}
	
	public Object validateValueForKey(Object value,String key) throws NSValidation.ValidationException  {
		if(key.endsWith("Name")) {
			return MyUtility.validateAttributeValue("Person." + key,value,String.class,!key.startsWith("second"),MaxNameLength);
		}
		
		return super.validateValueForKey(value,key);
	}
	
	public Object validateTakeValueForKeyPath(Object value,String keyPath) throws NSValidation.ValidationException {
		if(keyPath.indexOf('.') < 0) {
			takeStoredValueForKey(value,keyPath);
		}
		return super.validateTakeValueForKeyPath(value,keyPath);
	}
	/*
	public String validateLastName(Object n) {
		return (String)MyUtility.validateAttributeValue("Person.lastName",n,String.class,true,MaxNameLength);
	}
	
	public String validateFirstName(Object n) {
		return (String)MyUtility.validateAttributeValue("Person.firstName",n,String.class,true,MaxNameLength);
	}
	
	public String validateSecondName(Object n) {
		return (String)MyUtility.validateAttributeValue("Person.secondName",n,String.class,false,MaxNameLength);
	} */
	
}
