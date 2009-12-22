//  VsePerson.java

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

package net.rujel.vselists;

import java.util.Enumeration;

import net.rujel.interfaces.Person;
import net.rujel.interfaces.PersonLink;

import com.webobjects.eocontrol.*;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSKeyValueCodingAdditions;
import com.webobjects.foundation.NSMutableArray;
import com.webobjects.foundation.NSMutableDictionary;

public class VsePerson extends _VsePerson implements Person {

	public static void init() {
	}

	public void awakeFromInsertion(EOEditingContext ec) {
		super.awakeFromInsertion(ec);
		setSex(Boolean.FALSE);
	}

	public String initials() {
		return Person.Utility.initials(this);
	}

	public VsePerson person() {
		return this;
	}

	public void setPerson(EOEnterpriseObject pers) {
		if(pers != this)
			throw new UnsupportedOperationException("Person attribute can't be changed as it should always return this");
	}
	
	public static NSMutableDictionary agregateByLetter(NSArray list) {
		if(list == null)
			return null;
		NSMutableDictionary agregate = new NSMutableDictionary();
		NSArray aSorter = new NSArray(new EOSortOrdering
				("person",EOSortOrdering.CompareAscending));
		Enumeration enu = list.objectEnumerator();
		while (enu.hasMoreElements()) {
			PersonLink plink = (PersonLink) enu.nextElement();
			String ln = (String)NSKeyValueCodingAdditions.Utility.valueForKeyPath(
					plink, "person.lastName");
			String letter = (ln==null)?"?":ln.substring(0,1);
			NSMutableArray byLetter = (NSMutableArray)agregate.valueForKey(letter);
			if(byLetter == null) {
				byLetter = new NSMutableArray(plink);
				agregate.takeValueForKey(byLetter, letter);
			} else {
				byLetter.addObject(plink);
				EOSortOrdering.sortArrayUsingKeyOrderArray(byLetter, aSorter);
			}
		}
		return agregate;
	}
}
