//  VseStudent.java

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

import net.rujel.interfaces.EduGroup;
import net.rujel.interfaces.Person;
import net.rujel.interfaces.PersonLink;
import net.rujel.interfaces.Student;
import net.rujel.reusables.SessionedEditingContext;

import com.webobjects.appserver.WOSession;
import com.webobjects.eocontrol.*;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSTimestamp;

public class VseStudent extends _VseStudent implements Student {

	public static void init() {
		EOSortOrdering.ComparisonSupport.setSupportForClass(
				new Person.ComparisonSupport(), VsePerson.class);
		EOSortOrdering.ComparisonSupport.setSupportForClass(
				new PersonLink.ComparisonSupport(), VseStudent.class);
	}

	public void awakeFromInsertion(EOEditingContext ec) {
		super.awakeFromInsertion(ec);
	}

	protected NSTimestamp date() {
		NSTimestamp date = null;
		if (editingContext() instanceof SessionedEditingContext) {
			WOSession ses = ((SessionedEditingContext)editingContext()).session();
			date = (NSTimestamp)ses.valueForKey("today");
		}
		if(date == null)
			date = new NSTimestamp();
		return date;
	}

	protected static final NSArray flagsSorter = new NSArray(
			new EOSortOrdering(VseEduGroup.FLAGS_KEY,EOSortOrdering.CompareAscending));
	public EduGroup recentMainEduGroup() {
		NSArray lists = lists();
		if(lists == null || lists.count() == 0)
			return null;
		NSTimestamp date = date();
		NSArray args = new NSArray(new Object[] {date,date});
		EOQualifier qual = EOQualifier.qualifierWithQualifierFormat(
				"(enter = nil OR enter <= %@) AND (leave = nil OR leave >= %@)", args);
		lists = EOQualifier.filteredArrayWithQualifier(lists, qual);
		if(lists == null || lists.count() == 0)
			return null;
		if(lists.count() > 1) {
			lists = EOSortOrdering.sortedArrayUsingKeyOrderArray(lists, flagsSorter);
		}
		return (EduGroup)((EOEnterpriseObject)lists.objectAtIndex(0)).valueForKey("eduGroup");		
	}
}
