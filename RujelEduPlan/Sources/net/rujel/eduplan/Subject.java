// Subject.java

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

package net.rujel.eduplan;

import com.webobjects.foundation.*;
import com.webobjects.foundation.NSComparator.ComparisonException;
//import com.webobjects.eoaccess.EOUtilities;
import com.webobjects.eocontrol.EOEditingContext;
import com.webobjects.eocontrol.EOEnterpriseObject;
import com.webobjects.eocontrol.EOFetchSpecification;
import com.webobjects.eocontrol.EOKeyValueQualifier;
import com.webobjects.eocontrol.EOQualifier;
import com.webobjects.eocontrol.EOSortOrdering;

public class Subject extends _Subject implements EOSortOrdering.Comparison {

	public static final NSArray<EOSortOrdering> numSorter = new NSArray<EOSortOrdering>
		(new EOSortOrdering ("num",EOSortOrdering.CompareAscending));
	public static final NSArray<EOSortOrdering> sorter = new NSArray<EOSortOrdering> (new EOSortOrdering[] {
			new EOSortOrdering ("area.num",EOSortOrdering.CompareAscending),
			new EOSortOrdering ("num",EOSortOrdering.CompareAscending)});
	
	public static final SubjectComparator comparator = new SubjectComparator();
	
	public void awakeFromInsertion(EOEditingContext ec) {
		super.awakeFromInsertion(ec);
		Integer zero = new Integer(0);
		setNormalGroup(zero);
		setNum(zero);
		setSubgroups(zero);
	}
	
	public static NSArray subjectsForArea(EOEnterpriseObject area) {
		if(area == null)
			return null;
    	EOQualifier qual = new EOKeyValueQualifier("area",EOQualifier.QualifierOperatorEqual,area);
    	EOFetchSpecification fs = new EOFetchSpecification("Subject",qual,Subject.numSorter);
    	return area.editingContext().objectsWithFetchSpecification(fs);
	}
	
	public void setArea(EOEnterpriseObject area) {
		if(area == area())
			return;
		super.setArea(area);
		NSArray subjs = subjectsForArea(area);
		if(subjs != null && subjs.count() > 0) {
			Subject last = (Subject)subjs.lastObject();
			int num = last.num().intValue() +1;
			setNum(new Integer(num));
		}
	}
	
	public int compareAscending(Object arg0) {
		try {
			return comparator.compare(this, arg0);
		} catch  (ComparisonException ex) {
			throw new NSForwardException(ex,"Error comparing");
		}
	}	
	public int compareCaseInsensitiveAscending(Object arg0) {
		return compareAscending(arg0);
	}
	
	public int compareDescending(Object arg0) {
		try {
			return comparator.compare(arg0,this);
		} catch  (ComparisonException ex) {
			throw new NSForwardException(ex,"Error comparing");
		}
	}
	public int compareCaseInsensitiveDescending(Object arg0) {
		return compareDescending(arg0);
	}
	
	public void setSubgroupsPresent(Integer value) {
		if(value == null)
			value = new Integer(0);
		super.setSubgroups(value);
	}
	
	public void setNormalGroupPresent(Integer value) {
		if(value == null)
			value = new Integer(0);
		super.setNormalGroup(value);
	}
	
	public Integer subgroupsPresent() {
		Integer result = super.subgroups();
		if(result == null || result.intValue() == 0)
			return null;
		return result;
	}
	public Integer normalGroupPresent() {
		Integer result = super.normalGroup();
		if(result == null || result.intValue() == 0)
			return null;
		return result;
	}
}
