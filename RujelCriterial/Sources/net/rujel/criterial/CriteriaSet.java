// CriteriaSet.java

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

package net.rujel.criterial;


import java.util.Enumeration;

import net.rujel.base.SettingsBase;
import net.rujel.interfaces.EduCourse;
import net.rujel.interfaces.EduCycle;

import com.webobjects.foundation.*;
import com.webobjects.eocontrol.*;
import com.webobjects.eoaccess.EOUtilities;

public class CriteriaSet extends _CriteriaSet
{
	public static final NSArray sorter = new NSArray(EOSortOrdering.sortOrderingWithKey("sort",EOSortOrdering.CompareAscending));
    public CriteriaSet() {
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
	
	public NSArray sortedCriteria() {
		return EOSortOrdering.sortedArrayUsingKeyOrderArray(criteria(),sorter);
	}
	
	public EOEnterpriseObject criterionNamed(String critName) {
		if(criteria() == null || criteria().count() == 0) return null;
		EOQualifier qual = new EOKeyValueQualifier("title",EOQualifier.QualifierOperatorEqual,critName);
		NSArray result = EOQualifier.filteredArrayWithQualifier(criteria(),qual);
		if(result == null || result.count() == 0)
			return null;
		return (EOEnterpriseObject)result.objectAtIndex(0);
	}
	
	public void addCriterion() {
		EOEnterpriseObject criterion = EOUtilities.createAndInsertInstance(editingContext(),"Criterion");
		addObjectToBothSidesOfRelationshipWithKey(criterion,"criteria");
		Number num = (Number)criteria().valueForKeyPath("@max.sort");
		num = (num==null)?new Integer(1):new Integer(num.intValue() + 1);
		criterion.takeValueForKey(num,"sort");
	}
	
	protected static NSArray defaultSets;
	public static NSArray defaultSets(EOEditingContext ec) {
		NSMutableArray result = new NSMutableArray();
		if(defaultSets == null) {
			String dfltQual = "cycle = nil AND qualifier = nil";
			NSArray defaults = EOUtilities.objectsWithQualifierFormat(ec, "CycleCritSet",
					dfltQual, null);
			if(defaults != null && defaults.count() > 0) {
				defaults = (NSArray)defaults.valueForKey("criteriaSet");
				Enumeration enu = defaults.objectEnumerator();
				while (enu.hasMoreElements()) {
					EOEnterpriseObject set = (EOEnterpriseObject) enu.nextElement();
					result.addObject(ec.globalIDForObject(set));
				}
			}
			defaultSets = result.immutableClone();
			return defaults;
		} else if (defaultSets.count() > 0) {
			Enumeration enu = defaultSets.objectEnumerator();
			while (enu.hasMoreElements()) {
				EOGlobalID gid = (EOGlobalID) enu.nextElement();
				result.addObject(ec.faultForGlobalID(gid,ec));
			}
		}
		
		return result.immutableClone();
	}
	
/*	protected static NSArray qualifiers;
	protected static NSArray relatedSets;
	public static NSArray critSetsForCycle(EduCycle cycle) {
		EOEditingContext ec = cycle.editingContext();
		NSArray found = EOUtilities.objectsMatchingKeyAndValue(ec, "CycleCritSet", "cycle", cycle);
		if(found != null && found.count() >0)
			return (NSArray)found.valueForKey("criteriaSet");
		
		// fill qualifiers
		if(qualifiers == null) {
			String qualString = "cycle = nil AND qualifier != nil";
			found = EOUtilities.objectsWithQualifierFormat(ec, "CycleCritSet",
					qualString, null);
			if(found == null || found.count() == 0) {
				qualifiers = NSArray.EmptyArray;
			} else {
				NSMutableArray quals = new NSMutableArray();
				NSMutableArray sets = new NSMutableArray();
				Enumeration enu = found.objectEnumerator();
				while (enu.hasMoreElements()) {
					EOEnterpriseObject ccs = (EOEnterpriseObject) enu.nextElement();
					quals.addObject(ccs.valueForKey("qualifier"));
					EOEnterpriseObject set = (EOEnterpriseObject)ccs.valueForKey("criteriaSet");
					sets.addObject(ec.globalIDForObject(set));
				}
			}
		}
		
		//check qualifiers
		int count = qualifiers.count();
		NSMutableArray result = new NSMutableArray();
		if(count > 0) {
			for (int i = 0; i < count; i++) {
				String qualString = (String)qualifiers.objectAtIndex(i);
				EOQualifier qual = EOQualifier.qualifierWithQualifierFormat(qualString, null);
				if(qual.evaluateWithObject(cycle)) {
					EOGlobalID gid = (EOGlobalID)relatedSets.objectAtIndex(i);
					result.addObject(ec.faultForGlobalID(gid, ec));
				}
			}
		}
		//return default
		if(result.count() == 0)
			return defaultSets(ec);

		//TODO: select cycles
		return result.immutableClone();
	}*/
	
	public static NSArray criteriaForSets(NSArray sets) {
		if(sets != null && sets.count() > 0) {
			CriteriaSet set = (CriteriaSet)sets.objectAtIndex(0);
			if(sets.count() > 1) {
				NSMutableArray result = set.sortedCriteria().mutableClone();
				int count = sets.count();
				for (int i = 1; i < count; i++) {
					set = (CriteriaSet)sets.objectAtIndex(i);
					result.addObjectsFromArray(set.sortedCriteria());
				}
				return result.immutableClone();
			} else {
				return set.sortedCriteria();
			}
		}
		return null;
	}
/*
	public static NSArray criteriaForCycle(EduCycle cycle) {
		NSArray critSets = critSetsForCycle(cycle);
		return criteriaForSets(critSets);
	}
*/	
	public static CriteriaSet critSetForCourse(EduCourse course) {
		EOEditingContext ec = course.editingContext();
		Integer set = SettingsBase.numericSettingForCourse("CriteriaSet", course,ec);
		return (CriteriaSet)EOUtilities.objectWithPrimaryKeyValue(ec, ENTITY_NAME, set);
	}
	
	public static NSArray criteriaForCourse(EduCourse course) {
		CriteriaSet set = critSetForCourse(course);
		return (set==null)?null:set.sortedCriteria();
	}
}
