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

import net.rujel.base.BaseCourse;
import net.rujel.base.MyUtility;
import net.rujel.interfaces.EduCourse;
import net.rujel.interfaces.EduGroup;
import net.rujel.interfaces.Student;
import net.rujel.reusables.ModulesInitialiser;
import net.rujel.reusables.SettingsReader;
import net.rujel.reusables.Various;

import com.webobjects.appserver.*;
import com.webobjects.eoaccess.EOUtilities;
import com.webobjects.eocontrol.EOEditingContext;
import com.webobjects.eocontrol.EOKeyGlobalID;
import com.webobjects.foundation.*;

public class Main extends WOComponent {
	protected static Logger logger = Logger.getLogger("rujel.diary");
	
	public Main(WOContext context) {
		super(context);
	}

	public NSTimestamp date;
	public NSTimestamp since;
/*	public String dateString;
	public String sinceString;
	public String dateClass;
	public String sinceClass;
*/	
	protected NSArray groupList;
	public NSArray tabs;
	public NSKeyValueCoding currTab;
	//public Integer tabIndex;
	public Number currGr;
	public Number studentID;
	public String grName;

	public NSKeyValueCoding item;
	public NSKeyValueCoding item2;
	public NSKeyValueCoding item3;
	
	public NSArray courses;
	
	public void appendToResponse(WOResponse aResponse, WOContext aContext) {
		WORequest req = context().request();
		// date and groupList
		
		String dateString = req.stringFormValueForKey("date");
		date = (NSTimestamp)MyUtility.dateFormat().parseObject(
				dateString, new java.text.ParsePosition(0));
		Integer year = (date == null)?(Integer)application().valueForKey("year"):
			MyUtility.eduYearForDate(date);
		EOEditingContext ec = (EOEditingContext)application().valueForKeyPath(
				"ecForYear." + year.toString());
		if(ec == null) {
			year = (Integer)application().valueForKey("year");
			date = (NSTimestamp)application().valueForKey("today");
			ec = (EOEditingContext)application().valueForKeyPath(
					"ecForYear." + year.toString());
		}
//		NSNotificationCenter nc = NSNotificationCenter.defaultCenter();
//		nc.postNotification("EOSharedEditingContextInitializedObjectsNotification",ec,null);
		groupList = groupListForDate(date);
//		nc.postNotification("groupList prepared", groupList);
		/*	
		sinceString = req.stringFormValueForKey("since");
		since = (NSTimestamp)MyUtility.dateFormat().parseObject(
				sinceString, new java.text.ParsePosition(0));
		groupList = groupListForDate(since);
		if(since == null && Various.boolForObject(valueForKeyPath("currTab.period"))) {
			since = (date ==null)?new NSTimestamp():date;
			since = date.timestampByAddingGregorianUnits(0, 0, -7, 0, 0, 0);
			sinceString = MyUtility.dateFormat().format(since);
			sinceClass = "default";
		}
	*/	
		currGr = context().request().numericFormValueForKey("grID",
				new NSNumberFormatter("#"));
		studentID = context().request().numericFormValueForKey("studentID",
				new NSNumberFormatter("#"));
		EduGroup eduGroup = null;
		if(currGr != null) {
			try {
				 eduGroup = (EduGroup) EOUtilities.objectWithPrimaryKeyValue(
							ec, EduGroup.entityName,currGr);
				 grName = eduGroup.name();
			} catch (Exception e) {
				logger.log(Level.INFO,"Failed to get eduGroup: " + currGr,e);
			}
			if(eduGroup == null)
				currGr = null;
		}
		
		// display tabs
		tabs = ModulesInitialiser.useModules(context(), "diary");
		if(tabs != null) {
			String txt = req.stringFormValueForKey("regime");
			if(txt != null) {
				for(int i = tabs.count() -1;i>=0;i--) {
					currTab = (NSKeyValueCoding)tabs.objectAtIndex(i);
					if(txt.equals(currTab.valueForKey("id"))) {
						//tabIndex = new Integer(i);
						break;
					}
				}
			} else {
				currTab = (NSKeyValueCoding)tabs.objectAtIndex(0);
			}
		}

		//courses
		if(eduGroup != null) {
			String tmp = req.stringFormValueForKey("courses");
			if(tmp != null) {
				String[] cids = tmp.split(";");
				EduCourse[] crs = new EduCourse[cids.length];
				for (int i = 0; i < cids.length; i++) {
					try {
						Integer cid = new Integer(cids[i]);
						crs[i] = (EduCourse) EOUtilities.objectWithPrimaryKeyValue(
								ec, EduCourse.entityName, cid);
//						if(grName == null)
//							grName = crs[i].eduGroup().name();
					} catch (Exception e) {
						logger.log(Level.INFO,"Failed to get course for id: " + cids[i],e);
					}
				}
				courses = new NSArray(crs);
			}
			if(courses == null) {
				try {
					NSDictionary dict = new NSDictionary(new Object[] {year,eduGroup},
							new String[] {"eduYear","eduGroup"});
					courses = EOUtilities.objectsMatchingValues(ec,EduCourse.entityName, dict);
					if(courses != null && courses.count() > 0 && studentID != null) {
						Student student = (Student)EOUtilities.objectWithPrimaryKeyValue(
								ec, Student.entityName,studentID);
						if(eduGroup.isInGroup(student))
							courses = BaseCourse.coursesForStudent(courses, student);
						else
							studentID = null;
					}
				} catch (Exception e) {
					logger.log(Level.INFO,"Failed to get courses for eduGroup: " + currGr,e);
				}
				if(courses != null && courses.count() > 0 && studentID != null) {
					try {
					} catch (Exception e) {
						logger.log(Level.INFO,"Failed to get courses for studentID: "
								+ studentID,e);
					}
				}
			}
		}

		super.appendToResponse(aResponse, aContext);
	}
	
	public NSArray groupList() {
		if(groupList == null)
			groupList = groupListForDate(date);
		return groupList;
	}
	
	public boolean showTitles() {
		return (groupList().count() > 1);
	}
	
	protected NSArray groupListForDate(NSTimestamp aDate) {
		Integer year = (aDate == null)?(Integer)application().valueForKey("year"):
			MyUtility.eduYearForDate(aDate);
		NSMutableDictionary<Number, NSArray> groupsForYear = 
			(NSMutableDictionary<Number, NSArray>)valueForKeyPath("application.groupsForYear");
		NSArray result = groupsForYear.objectForKey(year);
		if(result != null)
			return result;
		EOEditingContext ec = (EOEditingContext)application().valueForKeyPath(
				"ecForYear." + year.toString());
		if(aDate == null || ec == null) // {
			return groupListForDate((NSTimestamp)application().valueForKey("today"));
//		} else {
			NSArray groups = EduGroup.Lister.listGroups(aDate,ec);
			int maxIndex = 0;
			{
				NSArray list = (NSArray) WOApplication.application().valueForKeyPath(
						"strings.sections.list");
				if(list != null && list.count() > 1) {
					Number max = (Number)list.valueForKeyPath("@max.idx");
					maxIndex = max.intValue();
				}
			}
			if(groups == null || groups.count() == 0) {
				return NSArray.EmptyArray;
			} else {
				NSMutableArray[] rows = new NSMutableArray[maxIndex + 1];
				NSMutableArray[] grps = new NSMutableArray[maxIndex + 1];
				result = new NSMutableArray();
				Enumeration enu = groups.objectEnumerator();
				int maxCnt = 0;
				while (enu.hasMoreElements()) {
					EduGroup gr = (EduGroup) enu.nextElement();
					NSMutableDictionary grDict = new NSMutableDictionary();
					Integer grYear = gr.eduYear();
					Integer grade = gr.grade();
					int s = 0;
					if(maxIndex > 0) {
						try {
							Integer sect = (Integer)gr.valueForKey("section");
							s = sect.intValue();
							if(s > maxIndex)
								s = 0;
						} catch(Exception e) {}
					}
					if(rows[s] == null)
						rows[s] = new NSMutableArray();
					if(grps[s] == null)
						grps[s] = new NSMutableArray();
					grps[s].addObject(grDict);
					
					NSMutableDictionary row = (NSMutableDictionary)rows[s].lastObject();
					boolean gerade = true;
					if(row != null) {
						Integer currGrade = (Integer)row.valueForKey("grade");
						gerade = Various.boolForObject(row.valueForKey("gerade"));
						if(grade == null) {
							if(currGrade != null) {
								row = null;
								currGrade = null;
							}
						} else if(!grade.equals(currGrade)) {
							row = null;
						}
					}
					if(row == null) {
						gerade = !gerade;
						row = new NSMutableDictionary(Boolean.valueOf(gerade),"gerade");
						rows[s].addObject(row);
						row.takeValueForKey(grade, "grade");
					}
					
					NSMutableArray gradeGroups = (NSMutableArray)row.valueForKey("groups");
					if(gradeGroups == null) {
						gradeGroups = new NSMutableArray(grDict);
						row.takeValueForKey(gradeGroups, "groups");
					} else {
						gradeGroups.addObject(grDict);
					}
					if(gradeGroups.count() > maxCnt)
						maxCnt = gradeGroups.count();
					
					if(grYear == null || grade == null || grYear.equals(year)) {
						grDict.takeValueForKey(gr.name(),"name");
					} else {
						grade = new Integer(grade.intValue() + 
								year.intValue() - grYear.intValue());
						StringBuilder name = new StringBuilder(6);
						name.append(grade).append(' ').append(gr.title());
						grDict.takeValueForKey(name.toString(),"name");
					}
					grDict.takeValueForKey(grade, "grade");
					grDict.takeValueForKey(gr.title(), "title");
					EOKeyGlobalID gid = (EOKeyGlobalID)gr.editingContext().globalIDForObject(gr);
					grDict.takeValueForKey(gid.keyValues()[0], "grID");
					StringBuffer onclick = new StringBuffer("takeValueForKey(");
					onclick.append(gid.keyValues()[0]).append(",'grID',true);");
					grDict.takeValueForKey(onclick.toString(), "onclick");
					grDict.takeValueForKey(Boolean.valueOf(gerade),"gerade");
//					((NSMutableArray)result).addObject(grDict);
				} // groups enumeration
				if(maxIndex > 0) {
					NSArray sections = (NSArray) WOApplication.application().valueForKeyPath(
						"strings.sections.list");
					enu = sections.objectEnumerator();
					result = new NSMutableArray();
					while (enu.hasMoreElements()) {
						NSKeyValueCoding sect = (NSKeyValueCoding) enu.nextElement();
						Number idx = (Number)sect.valueForKey("idx");
						NSMutableDictionary dict = new NSMutableDictionary(idx,"section");
						dict.takeValueForKey(sect.valueForKey("value"), "title");
						int i = idx.intValue();
						if(grps[i] == null)
							continue;
						if(grps[i].count() > rows[i].count() && (maxCnt > 3 || grps[i].count() > 
								SettingsReader.intForKeyPath("ui.maxListLength", 22))) {
							dict.takeValueForKey(rows[i], "rows");
						} else {
							dict.takeValueForKey(grps[i], "rows");
						}
						grps[i] = null;
						((NSMutableArray)result).addObject(dict);
					} // sections
					for (int i = 0; i < grps.length; i++) {
						if(grps[i] == null)
							continue;
						NSMutableDictionary dict = new NSMutableDictionary(
								new Integer(i),"section");
						dict.takeValueForKey("-=? " + i + " ?=-", "title");
						if(grps[i].count() > rows[i].count() && (maxCnt > 3 || grps[i].count() > 
						SettingsReader.intForKeyPath("ui.maxListLength", 22))) {
							dict.takeValueForKey(rows[i], "rows");
						} else {
							dict.takeValueForKey(grps[i], "rows");
						}
						((NSMutableArray)result).addObject(dict);
					}
				} else {
					NSDictionary dict = null;
					if(grps[0].count() > rows[0].count() && (maxCnt > 3 || grps[0].count() > 
							SettingsReader.intForKeyPath("ui.maxListLength", 22))) {
						dict = new NSDictionary(rows[0], "rows");
					} else {
						dict = new NSDictionary(grps[0], "rows");
					}
					result = new NSArray(dict);
				}
			} // has groups
//		}
		groupsForYear.setObjectForKey(result, year);
		return result;
	}
	
	
	
	public boolean isCurrent() {
		if(item2 == null || currGr == null)
			return false;
		Number id = (Number)((item3 == null)?item2.valueForKey("grID"):item3.valueForKey("grID"));
		return (id != null && currGr.intValue() == id.intValue());
	}
	
	public String grClass() {
		if(item2 == null)
			return null;
		if(isCurrent())
			return "selection";
		if(item3 != null)
			return "ungerade";
		if(item2.valueForKey("onclick") == null ||
				Various.boolForObject(item2.valueForKey("gerade")))
			return "gerade";
		return "ungerade";
	}

}
