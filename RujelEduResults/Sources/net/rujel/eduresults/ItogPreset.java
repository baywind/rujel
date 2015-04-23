//  ItogPreset.java

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

package net.rujel.eduresults;

import java.math.BigDecimal;
import java.util.Enumeration;

import net.rujel.base.SettingsBase;

import com.webobjects.eocontrol.*;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSKeyValueCodingAdditions;
import com.webobjects.foundation.NSMutableArray;
import com.webobjects.foundation.NSMutableDictionary;

public class ItogPreset extends _ItogPreset {
	
	public static NSArray sorter = new NSArray( new EOSortOrdering[] {
			new EOSortOrdering(PRESET_GROUP_KEY, EOSortOrdering.CompareAscending),
			new EOSortOrdering(STATE_KEY, EOSortOrdering.CompareDescending),
			new EOSortOrdering(VALUE_KEY, EOSortOrdering.CompareDescending)});
	
	public static NSArray listPresetGroup(EOEditingContext ec, Integer grNum) {
		EOQualifier qual = new EOKeyValueQualifier(PRESET_GROUP_KEY,
				EOQualifier.QualifierOperatorEqual, grNum);
		EOFetchSpecification fs = new EOFetchSpecification(ENTITY_NAME, qual, sorter);
		return ec.objectsWithFetchSpecification(fs);
	}
	
	public static ItogPreset presetForMark(String mark, NSArray presets) {
		EOQualifier qual = new EOKeyValueQualifier(MARK_KEY, 
				EOQualifier.QualifierOperatorEqual, mark);
		NSArray found = EOQualifier.filteredArrayWithQualifier(presets, qual);
		if(found == null || found.count() == 0)
			return null;
		return (ItogPreset)found.objectAtIndex(0);
	}
	
	public static ItogPreset presetForMark(String mark, Integer grNum, EOEditingContext ec) {
		if(grNum == null)
			return null;
		EOQualifier[] quals = new EOQualifier[2];
		quals[0] = new EOKeyValueQualifier(PRESET_GROUP_KEY,
				EOQualifier.QualifierOperatorEqual, grNum);
		quals[1] = new EOKeyValueQualifier(MARK_KEY, 
				EOQualifier.QualifierOperatorEqual, mark);
		quals[0] = new EOAndQualifier(new NSArray(quals));
		EOFetchSpecification fs = new EOFetchSpecification(ENTITY_NAME, quals[0], null);
		NSArray found = ec.objectsWithFetchSpecification(fs);
		if(found == null || found.count() == 0)
			return null;
		return (ItogPreset)found.objectAtIndex(0);
	}

	public static ItogPreset lowMark(NSArray group) {
		ItogPreset[] result = new ItogPreset[2];
		for (int i = group.count() -1; i > 0 ; i--) {
			ItogPreset pr = (ItogPreset)group.objectAtIndex(i);
			if(pr.state().intValue() == 0)
				continue;
			if(result[0] == null)
				result[0] = pr;
			if(pr.value() == null)
				continue;
			if(result[1] == null)
				result[1] = pr;
			if(!BigDecimal.ZERO.equals(pr.value()))
				return pr;
		}
		if(result[1] != null) return result[1];
		if(result[0] != null) return result[0];
		return (ItogPreset)group.lastObject();
	}
	
	public static String nameForGroup(NSArray group) {
		StringBuilder buf = new StringBuilder(15);
		ItogPreset pr = (ItogPreset)group.objectAtIndex(0);
		buf.append(pr.mark());
		ItogPreset pr1 = lowMark(group);
		if(pr1 != null && pr1 != pr)
			buf.append("-").append(pr1.mark());
		return buf.toString();
	}
	
	public static Integer getPresetGroup(String listName, Integer eduYear,
			ItogType itogType) {
		EOQualifier[] quals = new EOQualifier[3];
		quals[0] = new EOKeyValueQualifier("eduYear", EOQualifier.QualifierOperatorEqual, eduYear);
		quals[1] = new EOKeyValueQualifier("eduYear",
				EOQualifier.QualifierOperatorEqual, new Integer(0));
		quals[1] = new EOOrQualifier(new NSArray(quals));
		quals[0] = new EOKeyValueQualifier("listName",
				EOQualifier.QualifierOperatorEqual,listName);
		quals[2] = new EOKeyValueQualifier("itogType",
				EOQualifier.QualifierOperatorEqual, itogType);
		quals[0] = new EOAndQualifier(new NSArray(quals));
		
		EOFetchSpecification fs = new EOFetchSpecification("ItogTypeList",quals[0],null);
		NSArray found = itogType.editingContext().objectsWithFetchSpecification(fs);
		if(found == null || found.count() == 0)
			return null;
		Integer pr = null;
		for (int i = 0; i < found.count(); i++) {
			EOEnterpriseObject itl = (EOEnterpriseObject)found.objectAtIndex(i);
			if(pr == null || eduYear.equals(itl.valueForKey("eduYear")))
				pr = (Integer)itl.valueForKey(PRESET_GROUP_KEY);
		}
		return pr;
	}
	
	public static Integer getPresetGroup(ItogContainer itog, NSKeyValueCodingAdditions course) {
		String listName = SettingsBase.stringSettingForCourse(ItogMark.ENTITY_NAME, course,
				itog.editingContext());
		return getPresetGroup(listName, itog.eduYear(), itog.itogType());
	}

	public static NSMutableArray allPresets(EOEditingContext ec) {
		EOFetchSpecification fs = new EOFetchSpecification(ENTITY_NAME, null, sorter);
		NSArray found = ec.objectsWithFetchSpecification(fs);
		if(found == null || found.count() == 0)
			return null;
		NSMutableArray result = new NSMutableArray();
		Enumeration enu = found.objectEnumerator();
		NSMutableDictionary dict = null;
		NSMutableArray currList = null;
		Integer curr = null;
		while (enu.hasMoreElements()) {
			ItogPreset preset = (ItogPreset) enu.nextElement();
			if(curr == null || !curr.equals(preset.presetGroup())) {
				if(dict != null) {
					dict.takeValueForKey(nameForGroup(currList), "fullName");
				}
				curr = preset.presetGroup();
				dict = new NSMutableDictionary(curr,PRESET_GROUP_KEY);
				currList = new NSMutableArray(preset);
				dict.takeValueForKey(currList,"list");
				dict.takeValueForKey(preset.mark(), "max");
				result.addObject(dict);
			} else {
				currList.addObject(preset);
			}
		}
		dict.takeValueForKey(nameForGroup(currList), "fullName");
		return result;
	}

	public static void init() {
	}
	
    public void awakeFromInsertion(EOEditingContext ec) {
    	super.awakeFromInsertion(ec);
    	setState(new Integer(0));
    }

	public void turnIntoFault(EOFaultHandler handler) {
		super.turnIntoFault(handler);
	}
}
