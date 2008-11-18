// VOClass.java

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

package net.rujel.vseobuch;

import net.rujel.interfaces.*;
import com.webobjects.foundation.*;
import com.webobjects.eocontrol.*;
import com.webobjects.eoaccess.EOUtilities;

public class VOClass extends _VOClass implements EduGroup
{
    public VOClass() {
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
	public String title() {
		String name = name();
		return name.substring(name.indexOf('-') + 1);
	}

	public boolean isInGroup(PersonLink who) {
		if(_list == null)
			return list().contains(who);
		else
			return _list.contains(who);
	}
	
	public int count() {
		if(_list == null)
			return list().count();
		else
			return _list.count();
	}

	private transient NSArray _list;
	
	public NSArray list() {
		return list(false);
	}
	
	public NSArray list(boolean refresh) {
		if(refresh || _list == null) {
			NSMutableArray result = new NSMutableArray();
			java.util.Enumeration en = grouping().objectEnumerator();
			EOEnterpriseObject curr;
			while(en.hasMoreElements()) {
				curr = (EOEnterpriseObject)en.nextElement();
				Integer arch = (Integer)curr.valueForKey("isArhive");
				if(arch != 1) {
					result.addObject(curr.storedValueForKey("student"));
				}
			}
			_list = EOSortOrdering.sortedArrayUsingKeyOrderArray(result,Person.sorter);//result.immutableClone();
		}
		return _list;
	}
	
	public static final NSArray sorter = new NSArray(new Object[] {
		EOSortOrdering.sortOrderingWithKey("grade",EOSortOrdering.CompareAscending),
		EOSortOrdering.sortOrderingWithKey("name",EOSortOrdering.CompareAscending)
	});
	
	public static NSArray listGroups(NSTimestamp date, EOEditingContext ec) {
		Integer year = net.rujel.base.MyUtility.eduYearForDate(date);
/*		EOEditingContext ec = EOSharedEditingContext.defaultSharedEditingContext();
		EOQualifier qual = new EOKeyValueQualifier("eduYear",EOQualifier.QualifierOperatorEqual,year);
		EOFetchSpecification fspec = new EOFetchSpecification("VOClass",qual,sorter);
		return ec.objectsWithFetchSpecification(fspec);*/
		NSArray list = EOUtilities.objectsMatchingKeyAndValue(ec,"VOClass","eduYear",year);
		return EOSortOrdering.sortedArrayUsingKeyOrderArray(list,sorter);
/*		NSDictionary dict = new NSDictionary(year,"year");
        return EOUtilities.objectsWithFetchSpecificationAndBindings(ec,"VOClass","AllClassesInYear",dict);*/
    }
	

}
