// ImportList.java: Class file for WO Component 'ImportList'

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

package net.rujel.vselists;

import java.util.Calendar;
import java.util.Enumeration;

import com.webobjects.appserver.WOActionResults;
import com.webobjects.appserver.WOComponent;
import com.webobjects.appserver.WOContext;
import com.webobjects.appserver.WOMessage;
import com.webobjects.appserver.WOResponse;
import com.webobjects.eoaccess.EOUtilities;
import com.webobjects.eocontrol.EOAndQualifier;
import com.webobjects.eocontrol.EOEditingContext;
import com.webobjects.eocontrol.EOEnterpriseObject;
import com.webobjects.eocontrol.EOGlobalID;
import com.webobjects.eocontrol.EOKeyValueQualifier;
import com.webobjects.eocontrol.EOQualifier;
import com.webobjects.eocontrol.EOSortOrdering;
import com.webobjects.foundation.*;

import net.rujel.base.MyUtility;
import net.rujel.interfaces.Person;
import net.rujel.interfaces.Student;
import net.rujel.reusables.SessionedEditingContext;
import net.rujel.reusables.Various;
import net.rujel.reusables.WOLogLevel;

public class ImportList extends WOComponent {
    public ImportList(WOContext context) {
        super(context);
    }
    
    public VseEduGroup targetGroup;
    public Object item;
    public Object item2;
    public int index;
    public NSMutableArray omit = new NSMutableArray();
    public NSMutableArray toAdd;
    public NSMutableArray toExclude;
    public NSMutableArray toUpdate;
    public NSMutableArray toStay;
    public NSMutableDictionary params = new NSMutableDictionary();
    public NSTimestamp onDate;
    public NSMutableArray resultingList;
    public NSMutableArray leavers;

    public void appendToResponse(WOResponse aResponse, WOContext aContext) {
    	if(targetGroup == null)
    		targetGroup = (VseEduGroup)valueForBinding("params");
    	if(!Various.boolForObject(valueForBinding("interpreted"))) {
    		toAdd = null;
    		toExclude = null;
    		toUpdate = null;
    		toStay = null;
    		resultingList = null;
    		leavers = null;
    		params.removeAllObjects();
    	}
    	if(toAdd == null && toExclude == null && toUpdate == null && toStay == null) {
    		compareWithGroup();
    		if(canSetValueForBinding("interpreted"))
    			setValueForBinding(Boolean.TRUE, "interpreted");
    	}
    	super.appendToResponse(aResponse, aContext);
    }
    
    public void compareWithGroup() {
    	NSArray list = (NSArray)valueForBinding("list");
    	if(list == null)
    		return;
    	NSArray grList = targetGroup.lists();
    	Enumeration enu = list.objectEnumerator();
    	while (enu.hasMoreElements()) {
			NSMutableDictionary dict = (NSMutableDictionary) enu.nextElement();
			if(dict.valueForKey("lastName") == null && dict.valueForKey("firstName") == null)
				continue;
			VseList vseList = findEntry(dict, grList); // find in target group
			if(vseList == null) {
				if(toAdd == null)
					toAdd = new NSMutableArray(dict);
				else
					toAdd.addObject(dict);
				// find existing student by name
				NSMutableArray quals = new NSMutableArray();
		    	String name = (String)dict.valueForKey("lastName");
		    	if(name != null)
		    		quals.addObject(new EOKeyValueQualifier(VsePerson.LAST_NAME_KEY,
		    				EOQualifier.QualifierOperatorEqual, name));
		    	name = (String)dict.valueForKey("firstName");
		    	if(name != null)
		    		quals.addObject(new EOKeyValueQualifier(VsePerson.FIRST_NAME_KEY,
		    				EOQualifier.QualifierOperatorEqual, name));
		    	name = (String)dict.valueForKey("secondName");
		    	if(name != null)
		    		quals.addObject(new EOKeyValueQualifier(VsePerson.SECOND_NAME_KEY,
		    				EOQualifier.QualifierOperatorEqual, name));
		    	NSArray found = (quals.count() < 2)? null :Person.Utility.search(
		    		targetGroup.editingContext(),new EOAndQualifier(quals), Student.entityName);
		    	if(found != null && found.count() > 0) {
		    		Enumeration fenu = found.objectEnumerator();
		    		int grade = targetGroup.absGrade().intValue();
		    		boolean unchanged = true;
		    		while (fenu.hasMoreElements()) { // exclude students from distant grades
						VseStudent stu = (VseStudent) fenu.nextElement();
						int stGrade = stu.absGrade().intValue();
						if(Math.abs(stGrade - grade) > 2) {
							if(unchanged) {
								found = found.mutableClone();
								unchanged = false;
							}
							((NSMutableArray)found).removeIdenticalObject(stu);
						}
					}
		    		if(found.count() > 0) {
		    			dict.takeValueForKey(found, "found");
		    			dict.takeValueForKey(found.objectAtIndex(0), "student");
		    			params.takeValueForKey(Boolean.TRUE, "showFound");
		    		}
	    		} else {  // found student for name else
	    			Integer matches = (Integer)dict.valueForKey("matches");
	    			if(matches.intValue() < 2)
	    				omit.addObject(dict);
		    	}
		    	if(dict.valueForKey("sex") == null) {
		    		if(found != null && found.count() > 0) {
		    			VseStudent stu = (VseStudent)found.objectAtIndex(0);
		    			dict.takeValueForKey(stu.valueForKeyPath("person.sex"), "sex");
		    		} else {
		    			int idx = omit.indexOfIdenticalObject(dict);
		    			if(idx < 0)
		    				omit.addObject(dict);
		    		}
		    	}
			} else {
				if(toExclude == null)
					toExclude = grList.mutableClone();
				toExclude.removeIdenticalObject(vseList);
			}
			if(!params.containsKey("showDate") && dict.valueForKey("birthDate") != null)
				params.takeValueForKey(Boolean.TRUE, "showDate");
		} // list enumeration
    	NSArray sorter = new NSArray(new EOSortOrdering[] {
    			EOSortOrdering.sortOrderingWithKey("lastName", EOSortOrdering.CompareAscending),
    			EOSortOrdering.sortOrderingWithKey("firstName", EOSortOrdering.CompareAscending),
    			EOSortOrdering.sortOrderingWithKey("secondName", EOSortOrdering.CompareAscending),
    			EOSortOrdering.sortOrderingWithKey("birthDate", EOSortOrdering.CompareAscending)});
    	if(toAdd != null && toAdd.count() > 1)
    		EOSortOrdering.sortArrayUsingKeyOrderArray(toAdd, sorter);
    	if(toUpdate != null && toUpdate.count() > 1)
    		EOSortOrdering.sortArrayUsingKeyOrderArray(toUpdate, sorter);
    	if(toStay != null && toStay.count() > 1)
    		EOSortOrdering.sortArrayUsingKeyOrderArray(toStay, sorter);
    	
    	if(toExclude != null && toExclude.count() > 0) {
    		enu = toExclude.immutableClone().objectEnumerator();
    		NSTimestamp today = (NSTimestamp)session().valueForKey("today");
    		while (enu.hasMoreElements()) {
				VseList vseList = (VseList) enu.nextElement();
				if(!VseList.isActual(vseList,today.getTime()))
					toExclude.removeIdenticalObject(vseList);
			}
    		if(toExclude.count() > 1) {
    	    	EOSortOrdering.sortArrayUsingKeyOrderArray(toExclude, VseList.sorter);
    		}
    	}
    	if(toExclude != null && toExclude.count() == 0) {
    		toExclude = null;
    	}
    	onDate = (NSTimestamp)session().valueForKey("today");
    }
    
    private VseList findEntry(NSMutableDictionary dict, NSArray lists) {
    	String last = (String)dict.valueForKey("lastName");
    	String first = (String)dict.valueForKey("firstName");
    	Enumeration enu = lists.objectEnumerator();
    	while (enu.hasMoreElements()) {
    		VseList vseList = (VseList) enu.nextElement();
    		Person person = vseList.student().person();
			if(compareValue(person, "lastName", last) &&
					compareValue(person, "firstName", first)) {
				dict.takeValueForKey(vseList, "existing");
				Object value = dict.valueForKey("secondName");
				boolean update = (value != null && !compareValue(person,"secondName",value));
				if(update) {
					dict.takeValueForKey("highlight","sNameClass");
					String sName = person.secondName();
					if(sName != null)
						dict.takeValueForKey(strikeout(sName), "oldSName");
				}
				value = dict.valueForKey("birthDate");
				if(value != null && !compareValue(person, "birthDate", value)) {
					update = true;
					dict.takeValueForKey("highlight","bDateClass");
					NSTimestamp bDate = person.birthDate();
					if(bDate != null) {
						String birth = strikeout(MyUtility.dateFormat().format(bDate));
						dict.takeValueForKey(birth, "oldBDate");
					}
				}
				if(update) {
					dict.takeValueForKey(Boolean.TRUE, "update");
					if(toUpdate == null)
						toUpdate = new NSMutableArray(dict);
					else
						toUpdate.addObject(dict);
				} else {
					if(toStay == null)
						toStay = new NSMutableArray(dict);
					else
						toStay.addObject(dict);
				}
				return vseList;
			}
		}
    	return null;
    }
    
    private boolean compareValue (NSKeyValueCodingAdditions dict,String key, Object value) {
    	Object check = dict.valueForKeyPath(key);
    	if(value == null || value.equals(""))
    		return (check == null || check.equals(""));
    	return value.equals(check);
    }
    
    private String strikeout(String string) {
    	StringBuilder buf = new StringBuilder(
    			"<span style = \"text-decoration:line-through;color:#666666;\">");
    	buf.append(WOMessage.stringByEscapingHTMLString(string)).append("</span><br/>");
    	return buf.toString();
    }
    
    public String num() {
    	return Integer.toString(index +1);
    }
    
    public Boolean check() {
		return Boolean.valueOf(omit.indexOfIdenticalObject(item) < 0);
	}
    
    public void setCheck(Boolean check) {
    	int idx = omit.indexOfIdenticalObject(item);
    	if(check.booleanValue()) {
    		if(idx >= 0)
    			omit.removeObjectAtIndex(idx);
    	} else if(idx < 0) {
    			omit.addObject(item);
    	}
    }
    
    public WOActionResults apply() {
    	EOEditingContext ec = targetGroup.editingContext();
    	if(toAdd != null) {
    		Enumeration enu = toAdd.objectEnumerator();
    		boolean initial = (targetGroup.lists() == null || targetGroup.lists().count() == 0);
    		if(initial) {
    	    	Integer eduYear = MyUtility.eduYearForDate(onDate);
    	    	initial = (targetGroup.firstYear().intValue() >= eduYear.intValue());
    		}
    		while (enu.hasMoreElements()) {
				NSMutableDictionary dict = (NSMutableDictionary) enu.nextElement();
				if(omit.indexOfIdenticalObject(dict) >= 0)
					continue;
				VseStudent student = (VseStudent)dict.valueForKey("student");
				if(student == null) {
					student = (VseStudent)Person.Utility.create(ec, VseStudent.ENTITY_NAME, null);
					Person person = student.person();
					person.setLastName((String)dict.valueForKey("lastName"));
					person.setFirstName((String)dict.valueForKey("firstName"));
					person.setSecondName((String)dict.valueForKey("secondName"));
					person.setBirthDate((NSTimestamp)dict.valueForKey("birthDate"));
					Boolean sex = (Boolean)dict.valueForKey("sex");
					if(sex == null)
						sex = Boolean.FALSE;
					person.setSex(sex);
					student.setEnter(onDate);
					student.setAbsGrade(targetGroup.absGrade());
				}
				VseList vseList = (VseList)EOUtilities.createAndInsertInstance(
						ec, VseList.ENTITY_NAME);
				vseList.addObjectToBothSidesOfRelationshipWithKey(student, VseList.STUDENT_KEY);
				targetGroup.addObjectToBothSidesOfRelationshipWithKey(
						vseList, VseEduGroup.LISTS_KEY);
				if(!initial)
					vseList.setEnter(onDate);
			} // toAdd enumeration
    	} // add new
    	if(toUpdate != null) {
    		Enumeration enu = toUpdate.objectEnumerator();
        	NSMutableArray changed = (NSMutableArray)params.valueForKey("changed");
        	if(changed == null) {
        		changed = new NSMutableArray();
        		params.takeValueForKey(changed, "changed");
        	}
    		while (enu.hasMoreElements()) {
				NSMutableDictionary dict = (NSMutableDictionary) enu.nextElement();
				if(omit.indexOfIdenticalObject(dict) >= 0)
					continue;
				Person person = (Person)dict.valueForKeyPath("existing.student.person");
				if(person == null)
					continue;
				if(dict.valueForKey("sNameClass") != null)
					person.setSecondName((String)dict.valueForKey("secondName"));
				if(dict.valueForKey("bDateClass") != null)
					person.setBirthDate((NSTimestamp)dict.valueForKey("birthDate"));
				changed.addObject(dict.valueForKey("existing"));
    		}
    	}
    	if(toExclude != null) {
    		Enumeration enu = toExclude.objectEnumerator();
    		NSTimestamp leaveDate = null;
        	NSMutableArray changed = (NSMutableArray)params.valueForKey("changed");
    		if(onDate != null) {
    			leaveDate= onDate.timestampByAddingGregorianUnits(0, 0, -1, 0, 0, 0);
            	if(changed == null) {
            		changed = new NSMutableArray();
            		params.takeValueForKey(changed, "changed");
            	}
    		}
    		while (enu.hasMoreElements()) {
				VseList vseList = (VseList) enu.nextElement();
				if(omit.indexOfIdenticalObject(vseList) >= 0)
					continue;
				if(onDate == null) {
					targetGroup.removeObjectFromBothSidesOfRelationshipWithKey(
							vseList, VseEduGroup.LISTS_KEY);
					ec.deleteObject(vseList);
				} else {
					vseList.setLeave(leaveDate);
					changed.addObject(vseList);
				}
    		}
    	}
    	if(!ec.hasChanges())
    		return (WOComponent)session().valueForKey("pullComponent");
    	// prepare results
		Enumeration enu = targetGroup.lists().objectEnumerator();
		resultingList = new NSMutableArray();
		leavers = new NSMutableArray();
		Calendar cal = Calendar.getInstance();
		long now = onDate.getTime();
		while (enu.hasMoreElements()) {
			VseList l = (VseList) enu.nextElement();
			NSTimestamp border = l.leave();
			if(border != null) {
				if(border.getTime() < now - NSLocking.OneDay) {
					leavers.addObject(l);
					continue;
				} else {
					cal.setTime(border);
					cal.set(Calendar.HOUR, 23);
					cal.set(Calendar.MINUTE, 59);
					long time = cal.getTimeInMillis(); 
					if(time < now) {
						leavers.addObject(l);
						continue;
					}
				}
			}
			border = l.enter();
			if(border != null) {
				if(border.getTime() > now) {
					leavers.addObject(l);
					continue;
				}
			}
			resultingList.addObject(l);
		} // targetGroup.lists enumeration
		if(resultingList.count() > 1)
			EOSortOrdering.sortArrayUsingKeyOrderArray(resultingList, VseList.sorter);
		if(leavers.count() > 1)
			EOSortOrdering.sortArrayUsingKeyOrderArray(leavers, VseList.sorter);
    	return null;
    }
    
    public String rowClass() {
    	Boolean sex = (Boolean)valueForKeyPath((item instanceof VseList)?
    			"item.student.person.sex":"item.sex");
    	if(sex == null) return "grey";
    	if(item instanceof EOEnterpriseObject) {
    		EOGlobalID gid = targetGroup.editingContext().globalIDForObject(
    				(EOEnterpriseObject)item);
    		if(gid.isTemporary()) {
    			return (sex.booleanValue())?"foundMale":"foundFemale";    		
    		}
    	}
    	return (sex.booleanValue())?"male":"female";
	}
    
    public String rowStyle() {
    	NSMutableArray changed = (NSMutableArray)params.valueForKey("changed");
    	if(changed == null)
    		return null;
    	if(changed.indexOfIdenticalObject(item) < 0)
    		return null;
    	return "font-weight:bold;";
    }
	
	public WOActionResults editPerson() {
		EOEnterpriseObject student = (EOEnterpriseObject)valueForKeyPath("item.student");
		if(student == null)
			return null;
		EOEditingContext ec = new SessionedEditingContext(targetGroup.editingContext(), session());
		student = EOUtilities.localInstanceOfObject(ec, student);
		WOComponent popup = pageWithName("PersonInspector");
		popup.takeValueForKey(context().page(), "returnPage");
		popup.takeValueForKey(student, "personLink");
		return popup;
	}

	public WOActionResults revert() {
		targetGroup.editingContext().revert();
		resultingList = null;
		leavers = null;
		return null;
	}
	
	public String radioName() {
		return "selector" + index;
	}
	
	public String presentFound() {
		if(!(item2 instanceof VseStudent))
			return null;
		VseStudent st = (VseStudent)item2;
		VseList member = st.memberOf(onDate);
		if(member == null)
			return "???";
		String grName = WOMessage.stringByEscapingHTMLAttributeValue(member.eduGroup().name());
		if(member.isActual()) {
			String message = (String)session().valueForKeyPath(
				"strings.RujelVseLists_VseStrings.import.isIn");
			return String.format(message,grName);
		} else {
			NSTimestamp date = member.leave();
			Integer year = (date == null)?member.eduGroup().lastYear():null;
			if((date==null)?year.intValue() < MyUtility.eduYearForDate(onDate).intValue() :
				date.before(onDate)) {
				String message = (String)session().valueForKeyPath(
						"strings.RujelVseLists_VseStrings.import.wasIn");
				String border = (date==null)?MyUtility.presentEduYear(year.intValue()):
					MyUtility.dateFormat().format(date);
				return String.format(message, grName,border);
			}
			date = member.enter();
			year = (date == null)?member.eduGroup().firstYear():null;
			String message = (String)session().valueForKeyPath(
					"strings.RujelVseLists_VseStrings.import.willBe");
			String border = (date==null)?MyUtility.presentEduYear(year.intValue()):
				MyUtility.dateFormat().format(date);
			return String.format(message, grName,border);
		}
	}
    
	public WOActionResults save() {
		try {
			targetGroup.editingContext().saveChanges();
			ListsEditor.logger.log(WOLogLevel.MASS_EDITING,"Imported students for group",
					new Object[] {session(),targetGroup});
		} catch (Exception e) {
			ListsEditor.logger.log(WOLogLevel.WARNING,"Failed to imported students for group",
					new Object[] {session(),targetGroup,e});
			session().takeValueForKey(e.getMessage(), "message");
		}
		WOComponent result = (WOComponent)session().valueForKey("pullComponent");
		result.ensureAwakeInContext(context());
		result.takeValueForKey(targetGroup, "selection");
		return result;
	}
	
    public boolean isStateless() {
		return false;
	}
	public boolean synchronizesVariablesWithBindings() {
        return false;
	}
}