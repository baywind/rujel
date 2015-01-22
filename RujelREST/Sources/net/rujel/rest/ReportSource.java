// ReportSource.java

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

package net.rujel.rest;

import java.util.Enumeration;

import net.rujel.reusables.Counter;

import org.xml.sax.InputSource;

import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSDictionary;
import com.webobjects.foundation.NSKeyValueCoding;
import com.webobjects.foundation.NSKeyValueCodingAdditions;
import com.webobjects.foundation.NSMutableArray;
import com.webobjects.foundation.NSMutableDictionary;
import com.webobjects.foundation.NSMutableSet;
import com.webobjects.foundation.NSSet;

public class ReportSource extends InputSource {

	public String entity;
	public NSDictionary attributes;
	public String[] groupings;
	public String[] agregates;
	public String[] lists;
	public NSArray rows;
	public Integer level;

	public void agregate(Agregator[] agregators, Enumeration enu, String[] prevAgr) {
		NSMutableArray agrList = new NSMutableArray();
		NSMutableDictionary agrDict = new NSMutableDictionary();
		while (enu.hasMoreElements()) {
			NSKeyValueCoding row = (NSKeyValueCoding) enu.nextElement();
			if(prevAgr != null) {
				for (int i = 0; i < prevAgr.length; i++) {
					Agregator agregator = (Agregator)row.valueForKey(prevAgr[i]);
					if(agregator != null)
						row.takeValueForKey(agregator.getResult(), prevAgr[i]);
				}
			}
			NSMutableDictionary dict = agrDict;
			for (int i = 0; i < groupings.length; i++) {
				Object key = NSKeyValueCodingAdditions.Utility.valueForKeyPath(row,groupings[i]);
				if(key == null)
					key = NSKeyValueCoding.NullValue;
				NSMutableDictionary found = (NSMutableDictionary)dict.objectForKey(key);
				if(found == null) {
					found = new NSMutableDictionary();
					dict.setObjectForKey(found, key);
				}
				dict = found;
			}
			if(dict.count() == 0) {
				for (int i = 0; i < groupings.length; i++) {
					Object val = NSKeyValueCodingAdditions.Utility.valueForKeyPath(
							row,groupings[i]);
					dict.takeValueForKey(val, groupings[i]);
				}
				agrList.addObject(dict);
				dict.setObjectForKey(new Counter(1), "_count_");
				if(agregators != null) {
					for (int i = 0; i < agregators.length; i++) {
						Agregator agr = agregators[i];
						if(agr == null)
							continue;
						else
							agr = agr.emptyClone();
						dict.setObjectForKey(agr, agregators[i].name);
						agr.scan(row);
					}
				}
			} else {
				dict.valueForKeyPath("_count_.raise");
				if(agregates != null) {
					for (int i = 0; i < agregates.length; i++) {
						Agregator agregator = (Agregator)dict.valueForKey(agregates[i]);
						if(agregator != null)
							agregator.scan(row);
					}
				}
			}
			if(lists != null && lists.length > 0) {
				for (int i = 0; i < lists.length; i++) {
					Object value = NSKeyValueCodingAdditions.Utility.valueForKeyPath(row,lists[i]);
					if(value == null)
						continue;
					NSMutableSet list = (NSMutableSet)dict.valueForKey(lists[i]);
					if(list == null) {
						list = new NSMutableSet();
						dict.setObjectForKey(list, lists[i]);
					}
					if(value instanceof NSArray) {
						list.addObjectsFromArray((NSArray)value);
					} else if (value instanceof NSSet) {
						list.unionSet((NSSet)value);
					} else {
						list.addObject(value);
					}
				}
			}
		} // rows enumeration
		rows = agrList.immutableClone();
	}
}
