// PerPersonLink.java

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

package net.rujel.interfaces;

import java.util.Enumeration;

import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSDictionary;
import com.webobjects.foundation.NSKeyValueCoding;
import com.webobjects.foundation.NSMutableArray;

public interface PerPersonLink {

	public NSArray allValues();
	
	public Object forPersonLink(PersonLink pers);
	
	public int count();
	
	public static class Dictionary implements PerPersonLink , NSKeyValueCoding {
		protected NSDictionary storage;
		protected int count;
		protected NSArray values;
		
		public Dictionary(NSDictionary dict) {
			storage = dict;
			Enumeration enu = dict.keyEnumerator();
			count = 0;
			while (enu.hasMoreElements()) {
				Object key = (Object) enu.nextElement();
				if(key instanceof PersonLink)
					count++;
			}
		}
		
		public NSArray allValues() {
			if(values != null)
				return values;
			if(count == 0)
				return NSArray.EmptyArray;
			if(count == storage.count())
				return storage.allValues();
			NSMutableArray result = new NSMutableArray();
			Enumeration enu = storage.keyEnumerator();
			while (enu.hasMoreElements()) {
				Object key = (Object) enu.nextElement();
				if(key instanceof PersonLink)
					result.addObject(storage.objectForKey(key));
			}
			values = result.immutableClone();
			return values;
		}
		
		public Object forPersonLink(PersonLink pers) {
			return storage.objectForKey(pers);
		}
		
		public int count() {
			return count;
		}

		public void takeValueForKey(Object value, String key) {
			storage.takeValueForKey(value, key);
		}

		public Object valueForKey(String key) {
			return storage.valueForKey(key);
		}
	}
}
