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
	public static NSArray sorter = new NSArray(new EOSortOrdering[] {
		new EOSortOrdering("param1",EOSortOrdering.CompareAscending),
		new EOSortOrdering("param2",EOSortOrdering.CompareAscending)});

	public static void init() {
	}

	public void awakeFromInsertion(EOEditingContext ec) {
		setTotal(new Integer(0));
		super.awakeFromInsertion(ec);
	}
 
	protected NSDictionary _dict;
	protected NSArray _keys;
	
	public NSDictionary dict() {
		if(_dict != null)
			return _dict;
		_keys = (NSArray)description().valueForKeyPath("borderSet.sortedTitles");
		NSArray list = statEntries();
		if(list == null || list.count() == 0)
			return null;
		if(list.count() > 1 && _keys == null) {
			EOSortOrdering so = new EOSortOrdering("statKey"
					,EOSortOrdering.CompareCaseInsensitiveAscending);
			list = EOSortOrdering.sortedArrayUsingKeyOrderArray(list, new NSArray(so));
		}
		NSArray keys = (NSArray)list.valueForKey("statKey");
		list = (NSArray)list.valueForKey("keyCount");
		_dict = new NSDictionary(list,keys);
		if(_keys == null)
			_keys = keys.immutableClone();
		return _dict;
	}
	
	public NSArray keys() {
		if(_keys == null) {
			dict();
		}
		return _keys;
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
		_param1 = null;
		_param2 = null;
		_dict = null;
		_keys = null;
		
		super.turnIntoFault(handler);
	}
	
	public void setArray(NSArray values) {
		setDict(description().calculate(values));
	}
	
	protected EOEnterpriseObject _param1;
	public EOEnterpriseObject param1() {
		if(_param1 != null)
			return _param1;
		if(gid1() == null)
			return null;
		return EOUtilities.objectWithPrimaryKeyValue(editingContext(),
				description().grouping1(), gid1());
	}
	
	public void setParam1(EOEnterpriseObject param) {
		_param1 = null;
		if(param == null) {
			super.setGid1(null);
			return;
		}
		if(!param.entityName().equals(description().grouping1())) {
			throw new IllegalArgumentException("Parameter entity: " + param.entityName() +
					" instead of " + description().grouping1());
		}
		EOKeyGlobalID gid = (EOKeyGlobalID)editingContext().globalIDForObject(param);
		super.setGid1((Integer)gid.keyValues()[0]);
		_param1 = param;
	}
	
	public void setGid1(Integer value) {
		super.setGid1(value);
		_param1 = null;
	}

	protected EOEnterpriseObject _param2;
	public EOEnterpriseObject param2() {
		if(_param2 != null)
			return _param2;
		if(gid2() == null)
			return null;
		return EOUtilities.objectWithPrimaryKeyValue(editingContext(),
				description().grouping2(), gid2());
	}
	
	public void setParam2(EOEnterpriseObject param) {
		_param2 = null;
		if(param == null) {
			super.setGid2(null);
			return;
		}
		if(!param.entityName().equals(description().grouping2())) {
			throw new IllegalArgumentException("Parameter entity: " + param.entityName() +
					" instead of " + description().grouping2());
		}
		EOKeyGlobalID gid = (EOKeyGlobalID)editingContext().globalIDForObject(param);
		super.setGid2((Integer)gid.keyValues()[0]);
		_param2 = param;
	}

	public void setGid2(Integer value) {
		super.setGid2(value);
		_param2 = null;
	}

	public void setDict(NSDictionary newDict) {
		_dict = null;
		_keys = null;
		NSMutableDictionary dict = (newDict == null)? new NSMutableDictionary():
														newDict.mutableClone();
		Integer total = (Integer)dict.removeObjectForKey(TOTAL_KEY);
		if(dict.objectForKey("keys") instanceof NSArray) {
			_keys = (NSArray)dict.removeObjectForKey("keys");
		}
		_dict = dict.immutableClone();
		int checksum = 0;
		NSArray statEntries = statEntries();
		EOEditingContext ec = editingContext();
		if(statEntries != null && statEntries.count() > 0) {
			Enumeration enu = statEntries.objectEnumerator();
			while (enu.hasMoreElements()) {
				EOEnterpriseObject entry = (EOEnterpriseObject) enu.nextElement();
				Object key = entry.valueForKey("statKey");
				if(key == null)
					key = "";
				Number value = (Number)dict.removeObjectForKey(key);
				if(value == null) {
					removeObjectFromBothSidesOfRelationshipWithKey(entry, STAT_ENTRIES_KEY);
					ec.deleteObject(entry);
				} else {
					if(!(value instanceof Integer))
						value = new Integer(value.intValue());
					entry.takeValueForKey(value, "keyCount");
					checksum += value.intValue();
				}
			}
		}
		if(dict.count() > 0) {
			Enumeration enu = dict.keyEnumerator();
			while (enu.hasMoreElements()) {
				String key = (String) enu.nextElement();
				Number value = (Number)dict.objectForKey(key);
				EOEnterpriseObject entry = EOUtilities.createAndInsertInstance(ec, "StatEntry");
				entry.takeValueForKey(key, "statKey");
				if(!(value instanceof Integer))
					value = new Integer(value.intValue());
				entry.takeValueForKey(value, "keyCount");
				addObjectToBothSidesOfRelationshipWithKey(entry, STAT_ENTRIES_KEY);
				checksum += value.intValue();
			}
		}
		if(total != null && checksum > total.intValue()) {
			EOEnterpriseObject entry = EOUtilities.createAndInsertInstance(ec, "StatEntry");
			entry.takeValueForKey(TOTAL_KEY, "statKey");
			entry.takeValueForKey(total, "keyCount");
			addObjectToBothSidesOfRelationshipWithKey(entry, STAT_ENTRIES_KEY);
			checksum += total.intValue();
			total = null;
		}
		if(total == null)
			total = new Integer(checksum);
		setTotal(total);
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
				if(key == null)
					key = "";
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
		enu = keys.objectEnumerator();
		while (enu.hasMoreElements()) {
			String key = (String) enu.nextElement();
			Counter c = (Counter)result.objectForKey(key);
			result.setObjectForKey(new Integer(c.intValue()), key);
		}
		if(!keys.containsObject(TOTAL_KEY))
			result.setObjectForKey(new Integer(total.intValue()), TOTAL_KEY);
		return result;
	}
}
