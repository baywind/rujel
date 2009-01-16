// Main.java : Class file for WO Component 'Main'

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

package net.rujel.diary.ui;

import java.util.Enumeration;

import net.rujel.base.MyUtility;
import net.rujel.interfaces.EduGroup;

import com.webobjects.appserver.WOComponent;
import com.webobjects.appserver.WOContext;
import com.webobjects.appserver.WORequest;
import com.webobjects.eocontrol.EOKeyGlobalID;
import com.webobjects.eocontrol.EOSharedEditingContext;
import com.webobjects.foundation.*;

public class Main extends WOComponent {
	private static final long serialVersionUID = 1L;

	public Main(WOContext context) {
		super(context);
	}
	
	public NSKeyValueCoding item;
	
	public NSArray groupList() {
		WORequest req = context().request();
		String dateTxt = req.stringFormValueForKey("date");
		if(dateTxt == null)
			dateTxt = req.stringFormValueForKey("to");
		if(dateTxt == null)
			dateTxt = req.stringFormValueForKey("since");
		if(dateTxt == null)
			return groupListForDate(null);
		NSTimestamp date = (NSTimestamp)MyUtility.dateFormat().parseObject(
				dateTxt, new java.text.ParsePosition(0));
		return groupListForDate(date);
	}
	
	protected NSArray groupListForDate(NSTimestamp date) {
		Integer year = (date == null)?new Integer(0):MyUtility.eduYearForDate(date);
		NSMutableDictionary<Number, NSArray> groupsForYear = 
			(NSMutableDictionary<Number, NSArray>)valueForKeyPath("application.groupsForYear");
		NSArray result = groupsForYear.objectForKey(year);
		if(result != null)
			return result;
		if(date == null) {
			result = groupListForDate(new NSTimestamp());
		} else {
			NSArray groups = EduGroup.Lister.listGroups(date, 
					EOSharedEditingContext.defaultSharedEditingContext());
			if(groups == null || groups.count() == 0) {
				result = NSArray.EmptyArray;
			} else {
				result = new NSMutableArray();
				Enumeration<EduGroup> enu = groups.objectEnumerator();
				while (enu.hasMoreElements()) {
					EduGroup gr = (EduGroup) enu
							.nextElement();
					NSMutableDictionary grDict = new NSMutableDictionary(gr.name(),"name");
					grDict.takeValueForKey(gr.grade(), "grade");
					grDict.takeValueForKey(gr.title(), "title");
					EOKeyGlobalID gid = (EOKeyGlobalID)gr.editingContext().globalIDForObject(gr);
					grDict.takeValueForKey(gid.keyValues()[0], "grID");
					StringBuffer onclick = new StringBuffer("takeValueForKey(");
					onclick.append(gid.keyValues()[0]).append(",'grID',true);");
					grDict.takeValueForKey(onclick.toString(), "onclick");
					((NSMutableArray)result).addObject(grDict);
				}
			}
		}
		groupsForYear.setObjectForKey(result, year);
		return result;
	}
}
