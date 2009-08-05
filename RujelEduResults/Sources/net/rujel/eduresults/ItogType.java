//  ItogType.java

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

import java.util.Enumeration;

import net.rujel.base.SettingsBase;
import net.rujel.interfaces.EduCourse;

import com.webobjects.foundation.*;
import com.webobjects.eoaccess.EOUtilities;
import com.webobjects.eocontrol.*;

public class ItogType extends _ItogType {

	public static void init() {
	}

	public void awakeFromInsertion(EOEditingContext ec) {
		super.awakeFromInsertion(ec);
		setInYearCount(new Integer(0));
		setSort(new Integer(127));
	}
	
	public NSArray itogsInYear(Integer eduYear) {
		NSDictionary qual = new NSDictionary(new Object[] {
				eduYear, this}, new Object []
				{ItogContainer.EDU_YEAR_KEY, ItogContainer.ITOG_TYPE_KEY});
		NSArray result = EOUtilities.objectsMatchingValues(editingContext(),
				ItogContainer.ENTITY_NAME, qual);
		if(result != null && result.count() > 1)
			result = EOSortOrdering.sortedArrayUsingKeyOrderArray(result,ItogContainer.sorter);
		return result;
	}
	
	public static NSArray typesForCourse(EduCourse course) {
		EOEditingContext ec = course.editingContext();
		String listName = SettingsBase.stringSettingForCourse(ItogMark.ENTITY_NAME, course, ec);
		return typesForList(listName, ec);
	}
	public static NSArray typesForList(String listName, EOEditingContext ec) {
		EOQualifier qual = new EOKeyValueQualifier("listName",
				EOQualifier.QualifierOperatorEqual,listName);
		NSArray sorter = new NSArray(new EOSortOrdering("itogType.sort",
				EOSortOrdering.CompareAscending));
		EOFetchSpecification fs = new EOFetchSpecification("ItogTypeList",qual,sorter);
		NSArray list = ec.objectsWithFetchSpecification(fs);
		if(list == null || list.count() == 0) {
			listName = SettingsBase.stringSettingForCourse(ItogMark.ENTITY_NAME, null, ec);
			qual = new EOKeyValueQualifier("listName",
					EOQualifier.QualifierOperatorEqual,listName);
			fs.setQualifier(qual);
			list = ec.objectsWithFetchSpecification(fs);
			// log this
		}
		if(list != null && list.count() > 0)
			list = (NSArray)list.valueForKey("itogType");
		return list;
	}
	
	public static NSArray itogsForTypeList(NSArray list, Integer eduYear) {
		if(list == null || list.count() == 0)
			return null;
		Enumeration enu = list.objectEnumerator();
		NSMutableArray result = new NSMutableArray();
		while (enu.hasMoreElements()) {
			ItogType type = (ItogType) enu.nextElement();
			result.addObjectsFromArray(type.itogsInYear(eduYear));
		}
		return result;
	}
}
