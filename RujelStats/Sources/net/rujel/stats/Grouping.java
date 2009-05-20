//  Grouping.java

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

package net.rujel.stats;

import java.util.Enumeration;

import net.rujel.reusables.Counter;

import com.webobjects.foundation.*;
import com.webobjects.foundation.NSComparator.ComparisonException;
import com.webobjects.eoaccess.EOUtilities;
import com.webobjects.eocontrol.*;

public class Grouping extends _Grouping {

	public static void init() {
	}

	public void awakeFromInsertion(EOEditingContext ec) {
		super.awakeFromInsertion(ec);
	}
 
	protected NSDictionary _dict;
	protected NSArray _keys;
	
	public NSDictionary dict() {
		if(_dict != null)
			return _dict;
		NSArray list = statEntries();
		if(list == null || list.count() == 0)
			return null;
		if(list.count() > 1) {
			EOSortOrdering so = new EOSortOrdering("statKey"
					,EOSortOrdering.CompareCaseInsensitiveAscending);
			list = EOSortOrdering.sortedArrayUsingKeyOrderArray(list, new NSArray(so));
		}
		_keys = (NSArray)list.valueForKey("statKey");
		list = (NSArray)list.valueForKey("keyCount");
		_dict = new NSDictionary(list,_keys);
		return _dict;
	}
	
	public NSArray keys() {
		if(_keys == null) {
			dict();
		}
		return _keys.immutableClone();
	}
	
	public Integer countForKey(String key) {
		return (Integer)dict().objectForKey(key);
	}
	
	public void setCountForKey(Integer count, String key) {
		_dict = null;
		_keys = null;
		NSArray list = statEntries();
		if(list != null && list.count() > 0) {
			EOQualifier qual = new EOKeyValueQualifier("statKey",
					EOQualifier.QualifierOperatorEqual,key);
			list = EOQualifier.filteredArrayWithQualifier(list, qual);
		}
		EOEnterpriseObject entry = null;
		if(list == null || list.count() == 0) {
			if(count == null)
				return;
			entry = EOUtilities.createAndInsertInstance(editingContext(), "StatEntry");
			addObjectToBothSidesOfRelationshipWithKey(entry, STAT_ENTRIES_KEY);
		} else {
			for (int i = list.count() -1; i >= 0; i--) {
				entry = (EOEnterpriseObject) list.objectAtIndex(i);
				if(count == null || i > 0)
					editingContext().deleteObject(entry);
			}
			if(count == null)
				return;
		}
		entry.takeValueForKey(count, "keyCount");
	}
	
	public void turnIntoFault(EOFaultHandler handler) {
		_dict = null;
		_keys = null;
		super.turnIntoFault(handler);
	}
	
	public void recalculate(NSArray values) {
		
	}
	
	public static NSDictionary multyStats(NSArray groupings) {
		if(groupings == null || groupings.count() == 0)
			return NSDictionary.EmptyDictionary;
		if(groupings.count() == 1) {
			return ((Grouping)groupings.objectAtIndex(0)).dict();
		}
		Counter total = new Counter();
		NSMutableArray keys = new NSMutableArray();
		NSMutableDictionary result = new NSMutableDictionary(TOTAL_KEY,total);
		result.setObjectForKey(keys, "keys");
		Enumeration enu = groupings.objectEnumerator();
		while (enu.hasMoreElements()) {
			Grouping gr = (Grouping) enu.nextElement();
			total.setAdd(gr.total());
			NSArray list = gr.statEntries();
			if(list == null || list.count() == 0)
				continue;
			Enumeration entries = list.objectEnumerator();
			while (entries.hasMoreElements()) {
				EOEnterpriseObject entry = (EOEnterpriseObject) entries.nextElement();
				String key = (String)entry.valueForKey("statKey");
				Integer value = (Integer)entry.valueForKey("keyCount");
				Counter count = (Counter)result.objectForKey(key);
				if(count == null) {
					count = new Counter(value);
					result.setObjectForKey(count, key);
					keys.addObject(key);
				} else {
					count.setAdd(value);
				}
			}
		}
		if(keys.count() > 1) {
			try {
				keys.sortUsingComparator(NSComparator.AscendingCaseInsensitiveStringComparator);
			} catch (ComparisonException e) {
				;
			}
		}
		return result;
	}
}
