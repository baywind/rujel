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

package net.rujel.diary;

import java.util.Enumeration;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.rujel.base.MyUtility;
import net.rujel.interfaces.EduCourse;
import net.rujel.interfaces.EduGroup;
import net.rujel.reusables.ModulesInitialiser;
import net.rujel.reusables.Various;

import com.webobjects.appserver.*;
import com.webobjects.eoaccess.EOUtilities;
import com.webobjects.eocontrol.EOEditingContext;
import com.webobjects.eocontrol.EOKeyGlobalID;
import com.webobjects.eocontrol.EOSharedEditingContext;
import com.webobjects.foundation.*;

public class Main extends WOComponent {
	protected static Logger logger = Logger.getLogger("rujel.diary");
	
	public Main(WOContext context) {
		super(context);
	}

	public NSTimestamp date;
	public String dateString;
	public NSTimestamp since;
	public String sinceString;
	
	public NSArray groupList;
	public NSArray tabs;
	public NSKeyValueCoding currTab;
	public Number currGr;

	public NSKeyValueCoding item;
	
	public NSArray courses; 
	
	public void appendToResponse(WOResponse aResponse, WOContext aContext) {
		WORequest req = context().request();
		// date and groupList
		dateString = req.stringFormValueForKey("date");
		date = (NSTimestamp)MyUtility.dateFormat().parseObject(
				dateString, new java.text.ParsePosition(0));
		groupList = groupListForDate(date);
		if(date == null) {
			//date = new NSTimestamp();
			dateString = MyUtility.dateFormat().format(new NSTimestamp());
		}
		currGr = context().request().numericFormValueForKey("grID",
				new NSNumberFormatter("#"));
		
		sinceString = req.stringFormValueForKey("since");
		since = (NSTimestamp)MyUtility.dateFormat().parseObject(
				sinceString, new java.text.ParsePosition(0));
		groupList = groupListForDate(since);
		if(since == null && Various.boolForObject(valueForKeyPath("currTab.period"))) {
			since = (date ==null)?new NSTimestamp():date;
			since = date.timestampByAddingGregorianUnits(0, 0, -7, 0, 0, 0);
			sinceString = MyUtility.dateFormat().format(since);
		}
		
		// display tabs
		tabs = ModulesInitialiser.useModules(context(), "diary");
		if(tabs != null) {
			String txt = req.stringFormValueForKey("regime");
			if(txt != null) {
				for(int i = tabs.count() -1;i>=0;i--) {
					currTab = (NSKeyValueCoding)tabs.objectAtIndex(i);
					if(txt.equals(currTab.valueForKey("id")))
						break;
				}
			} else {
				currTab = (NSKeyValueCoding)tabs.objectAtIndex(0);
			}
		}

		//courses
		if(currGr != null) {
			EOEditingContext ec = EOSharedEditingContext.defaultSharedEditingContext();
			String tmp = req.stringFormValueForKey("courses");
			if(tmp != null) {
				String[] cids = tmp.split(";");
				EduCourse[] crs = new EduCourse[cids.length];
				for (int i = 0; i < cids.length; i++) {
					try {
						Integer cid = new Integer(cids[i]);
						crs[i] = (EduCourse) EOUtilities.objectWithPrimaryKeyValue(
								ec, EduCourse.entityName, cid);
					} catch (Exception e) {
						logger.log(Level.INFO,"Failed to get course for id: " + cids[i],e);
					}
				}
				courses = new NSArray(crs);
			}
			if(courses == null) {
				try {
					EduGroup eduGroup = (EduGroup) EOUtilities
					.objectWithPrimaryKeyValue(ec, EduGroup.entityName,
							currGr);
					courses = EOUtilities.objectsMatchingKeyAndValue(ec,
							EduCourse.entityName, "eduGroup", eduGroup);
					//TODO: respect eduYear
				} catch (Exception e) {
					logger.log(Level.INFO,"Failed to get eduGroup for id: " + currGr,e);
				}
			}
		}

		super.appendToResponse(aResponse, aContext);
	}
	
	
	protected NSArray groupListForDate(NSTimestamp aDate) {
		Integer year = (aDate == null)?new Integer(0):MyUtility.eduYearForDate(aDate);
		NSMutableDictionary<Number, NSArray> groupsForYear = 
			(NSMutableDictionary<Number, NSArray>)valueForKeyPath("application.groupsForYear");
		NSArray result = groupsForYear.objectForKey(year);
		if(result != null)
			return result;
		if(aDate == null) {
			result = groupListForDate(new NSTimestamp());
		} else {
			NSArray groups = EduGroup.Lister.listGroups(aDate, 
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
	
	public boolean isSelected() {
		if(currGr == null) {
			//currGr = new Integer(0);
			return false;
		}
		Number curr = (Number)valueForKeyPath("item.grID");
		return (curr != null && curr.intValue() == currGr.intValue());
	}

	public String dateStyle() {
		if(date == null)
			return "color:#999999;";
		return null;
	}
}
