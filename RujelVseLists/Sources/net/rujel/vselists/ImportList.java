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
import net.rujel.interfaces.EOPeriod;
import net.rujel.interfaces.Person;
import net.rujel.interfaces.PersonLink;
import net.rujel.interfaces.Student;
import net.rujel.interfaces.Teacher;
import net.rujel.reusables.AdaptingComparator;
import net.rujel.reusables.SessionedEditingContext;
import net.rujel.reusables.Various;
import net.rujel.reusables.WOLogLevel;

public class ImportList extends WOComponent {
    public ImportList(WOContext context) {
        super(context);
    }
    
    public VseEduGroup targetGroup;
    public NSArray lists;
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
    protected EOEditingContext ec;

    public void appendToResponse(WOResponse aResponse, WOContext aContext) {
    	if(lists == null) {
    		Object prm = valueForBinding("params");
    		if(prm instanceof VseEduGroup) {
    			targetGroup = (VseEduGroup)prm;
    			ec = targetGroup.editingContext();
    			lists = targetGroup.lists();
    		} else if (prm instanceof NSArray) {
    			lists = (NSArray)prm;
    			EOEnterpriseObject eo = (EOEnterpriseObject)lists.objectAtIndex(0);
    			ec = eo.editingContext();
    		}
    	}
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
    	Enumeration enu = list.objectEnumerator();
    	while (enu.hasMoreElements()) {
			NSMutableDictionary dict = (NSMutableDictionary) enu.nextElement();
			if(dict.valueForKey("lastName") == null && dict.valueForKey("firstName") == null)
				continue;
			if(!params.containsKey("showDate") && dict.valueForKey("birthDate") != null)
				params.takeValueForKey(Boolean.TRUE, "showDate");
			EOEnterpriseObject vseList = findEntry(dict); // find in target group
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
		    	NSArray found = null;
		    	if(quals.count() >= 2) {
			    	EOQualifier qual = new EOAndQualifier(quals);
	    			if(targetGroup == null) {
	    				found = Person.Utility.search(ec,qual,Teacher.entityName);
	    			}
	    			if(found == null || found.count() == 0)
	    				found = Person.Utility.search(ec,qual,Student.entityName);
		    	}
		    	if(found == null || found.count() == 0) {
	    			Integer matches = (Integer)dict.valueForKey("matches");
	    			if(matches.intValue() < 2 || dict.valueForKey("sex") == null) { // no name row
	    				omit.addObject(dict);
	    			}
    				continue;
		    	}
		    	if(found != null && found.count() > 0) {
		    		Enumeration fenu = found.objectEnumerator();
		    		int grade = (targetGroup==null)?0:targetGroup.absGrade().intValue();
		    		boolean unchanged = true;
		    		while (fenu.hasMoreElements()) { // exclude students from distant grades
						PersonLink stu = (PersonLink) fenu.nextElement();
						if(!dict.containsKey("sex"))
			    			dict.takeValueForKey(stu.person().sex(), "sex");
						if(stu instanceof VseStudent) {
							int stGrade = ((VseStudent)stu).absGrade().intValue();
							if(grade > 0 && Math.abs(stGrade - grade) > 2) {
								if(unchanged) {
									found = found.mutableClone();
									unchanged = false;
								}
								((NSMutableArray)found).removeIdenticalObject(stu);
							}
						}
		    		}
		    		if(found.count() > 0) {
		    			dict.takeValueForKey(found, "found");
		    			dict.takeValueForKey(found.objectAtIndex(0), "student");
		    			params.takeValueForKey(Boolean.TRUE, "showFound");
		    		}
		    	}
			} else { // found in group
				if(toExclude == null)
					toExclude = lists.mutableClone();
				toExclude.removeIdenticalObject(vseList);
			}
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
    	
    	if(targetGroup != null && toExclude != null && toExclude.count() > 0) {
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
    	} else if(toExclude != null && toExclude.count() > 1) {
	    	try {
				toExclude.sortUsingComparator(new AdaptingComparator());
			} catch (Exception e) {}
		}
    	if(toExclude != null && toExclude.count() == 0) {
    		toExclude = null;
    	}
    	onDate = (NSTimestamp)session().valueForKey("today");
    }
    
    private EOEnterpriseObject findEntry(NSMutableDictionary dict) {
    	String last = (String)dict.valueForKey("lastName");
    	String first = (String)dict.valueForKey("firstName");
    	Enumeration enu = lists.objectEnumerator();
    	while (enu.hasMoreElements()) {
    		EOEnterpriseObject vseList = (EOEnterpriseObject) enu.nextElement();
    		Person person = null;
    		if(vseList instanceof VseList)
    			person = ((VseList)vseList).student().person();
    		else if(vseList instanceof PersonLink)
    			person = ((PersonLink)vseList).person();
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
    	if(toAdd != null) {
    		Enumeration enu = toAdd.objectEnumerator();
    		boolean initial = (lists == null || lists.count() == 0);
    		if(initial) {
    	    	Integer eduYear = MyUtility.eduYearForDate(onDate);
    	    	initial = (targetGroup.firstYear().intValue() >= eduYear.intValue());
    		}
    		while (enu.hasMoreElements()) {
				NSMutableDictionary dict = (NSMutableDictionary) enu.nextElement();
				if(omit.indexOfIdenticalObject(dict) >= 0)
					continue;
				EOEnterpriseObject student = (EOEnterpriseObject)dict.valueForKey("student");
				if(student == null) {
					String ent = (targetGroup==null)?VseTeacher.ENTITY_NAME:VseStudent.ENTITY_NAME;
					student = (EOEnterpriseObject)Person.Utility.create(ec,ent , null);
					Person person = ((PersonLink)student).person();
					person.setLastName((String)dict.valueForKey("lastName"));
					person.setFirstName((String)dict.valueForKey("firstName"));
					person.setSecondName((String)dict.valueForKey("secondName"));
					person.setBirthDate((NSTimestamp)dict.valueForKey("birthDate"));
					Boolean sex = (Boolean)dict.valueForKey("sex");
					if(sex == null)
						sex = Boolean.FALSE;
					person.setSex(sex);
					student.takeValueForKey(onDate,VseStudent.ENTER_KEY);
					if(targetGroup != null)
						student.takeValueForKey(targetGroup.absGrade(),VseStudent.ABS_GRADE_KEY);
				} else if(targetGroup == null) {
					VsePerson pers = (VsePerson)((PersonLink)student).person();
					student = EOUtilities.createAndInsertInstance(ec, VseTeacher.ENTITY_NAME);
					student.takeValueForKey(onDate,VseStudent.ENTER_KEY);
					student.addObjectToBothSidesOfRelationshipWithKey(pers, VseTeacher.PERSON_KEY);
				}
				if(targetGroup != null) {
					VseList vseList = (VseList)EOUtilities.createAndInsertInstance(
							ec, VseList.ENTITY_NAME);
					vseList.addObjectToBothSidesOfRelationshipWithKey(student,VseList.STUDENT_KEY);
					targetGroup.addObjectToBothSidesOfRelationshipWithKey(
							vseList, VseEduGroup.LISTS_KEY);
					if(!initial)
						vseList.setEnter(onDate);
				}
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
    			leaveDate = onDate.timestampByAddingGregorianUnits(0, 0, -1, 0, 0, 0);
            	if(changed == null) {
            		changed = new NSMutableArray();
            		params.takeValueForKey(changed, "changed");
            	}
    		}
    		while (enu.hasMoreElements()) {
				Object vseList = enu.nextElement();
				if(omit.indexOfIdenticalObject(vseList) >= 0)
					continue;
				if(onDate == null) {
					if(targetGroup != null)
						targetGroup.removeObjectFromBothSidesOfRelationshipWithKey(
								(VseList)vseList, VseEduGroup.LISTS_KEY);
					ec.deleteObject((EOEnterpriseObject)vseList);
				} else {
					((EOEnterpriseObject)vseList).takeValueForKey(leaveDate, VseList.LEAVE_KEY);
					changed.addObject(vseList);
				}
    		}
    	}
    	if(!ec.hasChanges())
    		return (WOComponent)session().valueForKey("pullComponent");
    	// prepare results
		Enumeration enu = lists.objectEnumerator();
		resultingList = new NSMutableArray();
		leavers = new NSMutableArray();
		Calendar cal = Calendar.getInstance();
		long now = (onDate==null)?System.currentTimeMillis() :onDate.getTime();
		while (enu.hasMoreElements()) {
			EOEnterpriseObject l = (EOEnterpriseObject) enu.nextElement();
			NSTimestamp border = (NSTimestamp)l.valueForKey(VseList.LEAVE_KEY);
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
			border = (NSTimestamp)l.valueForKey(VseList.ENTER_KEY);
			if(border != null) {
				if(border.getTime() > now) {
					leavers.addObject(l);
					continue;
				}
			}
			resultingList.addObject(l);
		} // targetGroup.lists enumeration
		AdaptingComparator ac = (targetGroup == null)?new AdaptingComparator():null;
		if(resultingList.count() > 1) {
			if(targetGroup == null) try {
				resultingList.sortUsingComparator(ac);
			} catch (Exception e) {}
			else
				EOSortOrdering.sortArrayUsingKeyOrderArray(resultingList, VseList.sorter);
		}
		if(leavers.count() > 1) {
			if(targetGroup == null) try {
				resultingList.sortUsingComparator(ac);
			} catch (Exception e) {}
			else
				EOSortOrdering.sortArrayUsingKeyOrderArray(leavers, VseList.sorter);
		}
    	return null;
    }
    
    public String rowClass() {
    	Boolean sex = (Boolean)valueForKeyPath((item instanceof VseList)?
    			"item.student.person.sex":"item.person.sex");
    	if(sex == null) return "grey";
    	if(item instanceof EOEnterpriseObject) {
    		EOGlobalID gid = ec.globalIDForObject((EOEnterpriseObject)item);
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
		EOEditingContext nec = new SessionedEditingContext(ec, session());
		student = EOUtilities.localInstanceOfObject(nec, student);
		WOComponent popup = pageWithName("PersonInspector");
		popup.takeValueForKey(context().page(), "returnPage");
		popup.takeValueForKey(student, "personLink");
		return popup;
	}

	public WOActionResults revert() {
		ec.revert();
		resultingList = null;
		leavers = null;
		return null;
	}
	
	public String radioName() {
		return "selector" + index;
	}
	
	public String presentFound() {
		if(item2 instanceof VseTeacher) {
			String message = (String)session().valueForKeyPath(
				"strings.RujelVseLists_VseStrings.import.fired");
			NSTimestamp date = ((VseTeacher)item2).leave();
			if(date == null)
				((VseTeacher)item2).enter();
			message = message + MyUtility.dateFormat().format(date);
			return message;
		}
		if(!(item2 instanceof VseStudent))
			return "???";
		VseStudent st = (VseStudent)item2;
		VseList member = st.memberOf(onDate);
		if(member == null)
			return "???";
		VseEduGroup gr = member.eduGroup();
		if(member.isActual()) {
			String message = (String)session().valueForKeyPath(
				"strings.RujelVseLists_VseStrings.import.isIn");
			return String.format(message,
					WOMessage.stringByEscapingHTMLAttributeValue(gr.name()));
		} else {
			NSTimestamp date = member.leave();
			Integer year = (date == null)?member.eduGroup().lastYear():
				MyUtility.eduYearForDate(date);
			String message;
			if((date==null)?year.intValue() < MyUtility.eduYearForDate(onDate).intValue() :
				EOPeriod.Utility.compareDates(date, onDate) < 0) {
				message = (String)session().valueForKeyPath(
						"strings.RujelVseLists_VseStrings.import.wasIn");
			} else {
				date = member.enter();
				year = (date == null)?member.eduGroup().firstYear():MyUtility.eduYearForDate(date);
				message = (String)session().valueForKeyPath(
						"strings.RujelVseLists_VseStrings.import.willBe");
			}
			String border = (date==null)?MyUtility.presentEduYear(year.intValue()):
				MyUtility.dateFormat().format(date);
			StringBuilder grName = new StringBuilder();
			grName.append(year.intValue() - gr.absGrade().intValue()).append(' ');
			grName.append(WOMessage.stringByEscapingHTMLAttributeValue(gr.title()));
			return String.format(message, grName,border);
		}
	}
    
	public WOActionResults save() {
		try {
			ec.saveChanges();
			ListsEditor.logger.log(WOLogLevel.MASS_EDITING,"Imported students for group",
					new Object[] {session(),targetGroup});
		} catch (Exception e) {
			ListsEditor.logger.log(WOLogLevel.WARNING,"Failed to imported students for group",
					new Object[] {session(),targetGroup,e});
			session().takeValueForKey(e.getMessage(), "message");
		}
		WOComponent result = (WOComponent)session().valueForKey("pullComponent");
		result.ensureAwakeInContext(context());
		result.valueForKey("switchMode");
		result.takeValueForKey(targetGroup, "selection");
		return result;
	}
	
	public PersonLink plinkItem() {
		if (item == null)
			return null;
		Object obj = this.item;
		if(obj instanceof NSDictionary)
			obj = ((NSDictionary)obj).valueForKey("existing");
		if(obj instanceof PersonLink)
			return (PersonLink)obj;
		if(obj instanceof VseList)
			return ((VseList)obj).student();
		return null;
	}
	
    public boolean isStateless() {
		return false;
	}
	public boolean synchronizesVariablesWithBindings() {
        return false;
	}
}