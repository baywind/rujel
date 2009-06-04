//  Description.java

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

import net.rujel.criterial.BorderSet;

import com.webobjects.foundation.*;
import com.webobjects.eoaccess.EOObjectNotAvailableException;
import com.webobjects.eoaccess.EOUtilities;
import com.webobjects.eocontrol.*;

public class Description extends _Description {

	public static String UNSTORED = "unstored";

	public void awakeFromInsertion(EOEditingContext ec) {
		super.awakeFromInsertion(ec);
	}
	
	public NSDictionary calculate(NSArray values) {
		return calculate(values, statField(), borderSet());
	}

	public static NSDictionary calculate(NSArray values, String attribute, BorderSet bSet) {
		if(values == null || values.count() == 0)
			return NSDictionary.EmptyDictionary;
		NSMutableDictionary result = new NSMutableDictionary();
		NSMutableArray list = values.mutableClone();
		if(attribute != null && list.lastObject() instanceof Number) {
			Number num = (Number)list.removeLastObject();
			result.setObjectForKey(num, "");
			num = new Integer(num.intValue() + list.count());
			result.setObjectForKey(num,Grouping.TOTAL_KEY);
		} else {
			result.setObjectForKey(new Integer(values.count()),Grouping.TOTAL_KEY);
		}
		if(list.count() > 1) {
			EOSortOrdering so = new EOSortOrdering(attribute,
					EOSortOrdering.CompareCaseInsensitiveAscending);
			EOSortOrdering.sortArrayUsingKeyOrderArray(list, new NSArray(so));
		}
		NSMutableArray keys = null;
		if(bSet == null) {
			keys = new NSMutableArray();
			result.setObjectForKey(keys, "keys");
		} else {
			result.setObjectForKey(bSet.sortedTitles(), "keys");
		}
		Enumeration enu = list.objectEnumerator();
		String currKey = null;
		int currCount = 0;
		while (enu.hasMoreElements()) {
			Object value  = enu.nextElement();
			if(attribute != null)
				value = NSKeyValueCoding.Utility.valueForKey(value,attribute);
			if(bSet != null) {
				value = bSet.presentFraction((Number)value);
			}
			if(value == null)
				value = "";
			if(((String)value).equalsIgnoreCase(currKey)) {
				currCount++;
			} else {
				if(currKey != null)
					result.setObjectForKey(new Integer(currCount), currKey);
				currCount = 1;
				currKey = (String)value;
				if(keys != null)
					keys.addObject(value);
			}
		}
		if(currKey != null)
			result.setObjectForKey(new Integer(currCount), currKey);
		return result;
	}
	
	public Grouping getGrouping(EOEnterpriseObject param1, EOEnterpriseObject param2,
																		boolean create) {
		if(UNSTORED.equalsIgnoreCase(grouping1()))
			return null;
		Object gid1 = NullValue;
		if(param1 != null) {
			if(!param1.entityName().equals(grouping1()))
				throw new IllegalArgumentException("Param1 is of wrong entity");
			EOKeyGlobalID gid = (EOKeyGlobalID)editingContext().globalIDForObject(param1);
			gid1 = gid.keyValues()[0];
		}
		Object gid2 = NullValue;
		if(param2 != null) {
			if(!param2.entityName().equals(grouping2()))
				throw new IllegalArgumentException("Param2 is of wrong entity");
			EOKeyGlobalID gid = (EOKeyGlobalID)editingContext().globalIDForObject(param2);
			gid2 = gid.keyValues()[0];
		}
		NSDictionary params = new NSDictionary(new Object[] {this,gid1,gid2},
				new String[] {Grouping.DESCRIPTION_KEY,Grouping.GID1_KEY,Grouping.GID2_KEY});
		try {
			return (Grouping)EOUtilities.objectMatchingValues(editingContext(),
					Grouping.ENTITY_NAME, params);
		} catch (EOObjectNotAvailableException e) {
			if(create) {
				Grouping grouping = (Grouping)EOUtilities.createAndInsertInstance(
							editingContext(), Grouping.ENTITY_NAME);
				grouping.takeValuesFromDictionary(params);
				return grouping;
			}
		}
		return null;
	}
	
	public static Description getDescription(String entName, String statField, 
			String ent1, String ent2, EOEditingContext ec, boolean create) {
		NSMutableArray quals = new NSMutableArray(new EOKeyValueQualifier(ENT_NAME_KEY,
				EOQualifier.QualifierOperatorEqual,entName));
		if(ent1 != null)
		quals.addObject(new EOKeyValueQualifier(GROUPING1_KEY,
				EOQualifier.QualifierOperatorEqual,ent1));
		if(ent2 != null)
		quals.addObject(new EOKeyValueQualifier(GROUPING2_KEY,
				EOQualifier.QualifierOperatorEqual,ent2));
		if(statField != null) {
			quals.addObject(new EOKeyValueQualifier(STAT_FIELD_KEY,
					EOQualifier.QualifierOperatorEqual,statField));
		}
		EOQualifier qual = new EOAndQualifier(quals);
		EOFetchSpecification fs = new EOFetchSpecification(ENTITY_NAME,qual,null);
		NSArray found = ec.objectsWithFetchSpecification(fs);
		if((found == null || found.count() == 0) && (ent2 != ent1)) {
			if(ent2 != null)
			quals.replaceObjectAtIndex(new EOKeyValueQualifier(GROUPING1_KEY,
					EOQualifier.QualifierOperatorEqual,ent2), 1);
			if(ent1 != null)
			quals.replaceObjectAtIndex(new EOKeyValueQualifier(GROUPING2_KEY,
					EOQualifier.QualifierOperatorEqual,ent1), (ent2==null)?1:2);
			qual = new EOAndQualifier(quals);
			fs.setQualifier(qual);
			found = ec.objectsWithFetchSpecification(fs);
		}
		Description desc = null;
		if(found != null && found.count() > 0) {
			desc = (Description)found.objectAtIndex(0);
		} else {
			if(!create)
				return null;
			desc = (Description)EOUtilities.createAndInsertInstance(ec, ENTITY_NAME);
			desc.setEntName(entName);
			desc.setStatField(statField);
			desc.setGrouping1(ent1);
			desc.setGrouping2(ent2);
		}
		return desc;
	}
	
	public static Grouping getGrouping(String entName, String statField, 
			EOEnterpriseObject param1, EOEnterpriseObject param2, boolean create) {
		if(entName == null)
			throw new IllegalArgumentException("Entity name required");
		EOEditingContext ec = null;
		if(param1 != null)
			ec = param1.editingContext();
		else if(param2 != null)
			ec = param2.editingContext();
		else
			throw new IllegalArgumentException("At least one grouping param is required");
		String ent1 = (param1 == null)?null:param1.entityName();
		String ent2 = (param2 == null)?null:param2.entityName();
		Description desc = getDescription(entName, statField, ent1, ent2, ec, create);
		return (desc == null)?null:desc.getGrouping(param1, param2, create);	
	}
}
