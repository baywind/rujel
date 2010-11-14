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

import java.util.Enumeration;

import net.rujel.base.MyUtility;
import net.rujel.interfaces.Person;
import net.rujel.interfaces.PersonLink;
import net.rujel.interfaces.Student;
import net.rujel.reusables.Counter;
import net.rujel.reusables.SettingsReader;

import com.webobjects.eoaccess.EOUtilities;
import com.webobjects.eocontrol.*;
import com.webobjects.foundation.*;

public class VseStudent extends _VseStudent implements Student {

	public static final EOQualifier notInGroup = new EOKeyValueQualifier(
			"recentMainEduGroup",EOQualifier.QualifierOperatorEqual,NullValue);
	
	public static void init() {
		EOSortOrdering.ComparisonSupport.setSupportForClass(
				new Person.ComparisonSupport(), VsePerson.class);
		EOSortOrdering.ComparisonSupport.setSupportForClass(
				new PersonLink.ComparisonSupport(), VseStudent.class);
	}

	public void awakeFromInsertion(EOEditingContext ec) {
		super.awakeFromInsertion(ec);
		setAbsGrade(new Integer(0));
	}

	protected static final NSArray flagsSorter = new NSArray(
			new EOSortOrdering("eduGroup.flags",EOSortOrdering.CompareAscending));
	public VseEduGroup recentMainEduGroup() {
		NSArray lists = lists();
		if(lists == null || lists.count() == 0)
			return null;
		NSTimestamp date = MyUtility.date(editingContext());
		NSArray args = new NSArray(new Object[] {date,date});
		EOQualifier qual = EOQualifier.qualifierWithQualifierFormat(
				"(enter = nil OR enter <= %@) AND (leave = nil OR leave >= %@)", args);
		lists = EOQualifier.filteredArrayWithQualifier(lists, qual);
		if(lists == null || lists.count() == 0)
			return null;
		int year = MyUtility.eduYearForDate(date);
		if(lists.count() > 1) {
			lists = EOSortOrdering.sortedArrayUsingKeyOrderArray(lists, flagsSorter);
		}
		Enumeration enu = lists.objectEnumerator();
		while (enu.hasMoreElements()) {
			VseList vl = (VseList) enu.nextElement();
			VseEduGroup grp = vl.eduGroup();
			if(grp.firstYear().intValue() <= year && grp.lastYear().intValue() >= year)
				return grp;
		}
		return null;	
	}
	
	public static VseStudent studentForPerson(Person person, NSTimestamp date) {
		EOEditingContext ec = person.editingContext();
		NSArray students = EOUtilities.objectsMatchingKeyAndValue(ec, ENTITY_NAME,
				PERSON_KEY, person);
		if(students == null || students.count() == 0)
			return null;
		if(date != null) {
			NSArray args = new NSArray(new Object[] {date,date});
			EOQualifier qual = EOQualifier.qualifierWithQualifierFormat(
					"(enter = nil OR enter <= %@) AND (leave = nil OR leave >= %@)", args);
			students = EOQualifier.filteredArrayWithQualifier(students, qual);
			if(students == null || students.count() == 0)
				return null;
		}
		return (VseStudent)students.objectAtIndex(0);
	}

	public static VseStudent studentForPerson(Person person, NSTimestamp date,
			boolean create) {
		VseStudent student = studentForPerson(person, date);
		if(create && student == null) {
			EOEditingContext ec = person.editingContext();
			student = (VseStudent)EOUtilities.createAndInsertInstance(ec, ENTITY_NAME);
			student.addObjectToBothSidesOfRelationshipWithKey(person, PERSON_KEY);
			student.setEnter(date);
		}
		return student;
	}
	
	public int currGrade() {
		Integer absGrade = absGrade();
		if(absGrade == null || absGrade.intValue() == 0)
			return 0;
		Integer year = MyUtility.eduYear(editingContext());
		if(year == null)
			return -1;
		return year.intValue() - absGrade.intValue();
	}
	/*
	public static NSMutableDictionary studentsAgregate(
			EOEditingContext ec, NSTimestamp date) {
		EOFetchSpecification fs = new EOFetchSpecification(ENTITY_NAME,null,null);
		fs.setRefreshesRefetchedObjects(true);
		fs.setPrefetchingRelationshipKeyPaths(new NSArray("person"));
		if(date != null) {
			NSArray args = new NSArray(new Object[] {date,date});
			EOQualifier qual = EOQualifier.qualifierWithQualifierFormat(
					"(enter = nil OR enter <= %@) AND (leave = nil OR leave >= %@)", args);
			fs.setQualifier(qual);
		}
		NSArray list = ec.objectsWithFetchSpecification(fs);
		NSMutableDictionary agregate = new NSMutableDictionary();
		int maxGrade = SettingsReader.intForKeyPath("edu.maxGrade", 11);
		NSMutableArray keys = new NSMutableArray();
		for (int i = SettingsReader.intForKeyPath("edu.minGrade", 1); i <= maxGrade; i++) {
			Integer grade = new Integer(i);
			keys.addObject(grade);
			agregate.setObjectForKey(new NSMutableArray(), grade);
		}
		agregate.takeValueForKey(keys, "list");
		if(list == null || list.count() == 0)
			return agregate;
		Enumeration enu = list.objectEnumerator();
		NSArray aSorter = new NSArray(new EOSortOrdering
				("person",EOSortOrdering.CompareAscending));
		while (enu.hasMoreElements()) {
			VseStudent st = (VseStudent) enu.nextElement();
			if(st.recentMainEduGroup() != null)
				continue;
			Integer grade = new Integer(st.currGrade());
			NSMutableArray byGrade = (NSMutableArray)agregate.objectForKey(grade);
			if(byGrade == null) {
				byGrade = new NSMutableArray(st);
				agregate.setObjectForKey(byGrade, grade);
//				if(!keys.containsObject(grade))
					keys.addObject(grade);
			} else {
				byGrade.addObject(st);
				EOSortOrdering.sortArrayUsingKeyOrderArray(byGrade, aSorter);
			}
		}
		if (agregate.count() > 0) {
			try {
				keys.sortUsingComparator(NSComparator.AscendingNumberComparator);
			} catch (ComparisonException e) {
				e.printStackTrace();
			}
			agregate.takeValueForKey(keys, "list");
		}
		return agregate;
	} */
	
	public static NSArray agregatedList(EOEditingContext ec, NSTimestamp date) {
		int maxGrade = SettingsReader.intForKeyPath("edu.maxGrade", 11);
		NSMutableArray result = new NSMutableArray();
		NSArray groups = VseEduGroup.listGroups(date, ec);
		Enumeration grenu = groups.objectEnumerator();
		VseEduGroup grp = (grenu.hasMoreElements())?(VseEduGroup)grenu.nextElement():null;
		int eduYear = MyUtility.eduYearForDate(date).intValue();
		EOQualifier quals[] = new EOQualifier[2];
		NSArray args = new NSArray(new Object[] {date,date});
		quals[0] = EOQualifier.qualifierWithQualifierFormat(
				"(enter = nil OR enter <= %@) AND (leave = nil OR leave >= %@)", args);
		EOFetchSpecification fs = new EOFetchSpecification(ENTITY_NAME,null,null);
		fs.setRefreshesRefetchedObjects(true);
		for (int i = SettingsReader.intForKeyPath("edu.minGrade", 1); i <= maxGrade; i++) {
			while(grp != null && grp.grade().intValue() < i) {
				result.addObject(grp);
				grp = (grenu.hasMoreElements())?(VseEduGroup)grenu.nextElement():null;
			}
			NSMutableDictionary dict = dict(Integer.toString(i), 
					new Integer(eduYear - i), quals);
			result.addObject(dict);
			fs.setQualifier(quals[1]);
			NSArray list = ec.objectsWithFetchSpecification(fs);
			if(list == null || list.count() == 0)
				continue;
			Enumeration enu = list.objectEnumerator();
			while (enu.hasMoreElements()) {
				VseStudent st = (VseStudent) enu.nextElement();
				if(st.recentMainEduGroup() == null) {
					dict.valueForKeyPath("allCount.raise");
					if(quals[0].evaluateWithObject(st))
						dict.valueForKeyPath("currCount.raise");
//					break;
				}
			}
		} // list all grades
		while(grp != null && grp.grade().intValue() == maxGrade ) {
			result.addObject(grp);
			grp = (grenu.hasMoreElements())?(VseEduGroup)grenu.nextElement():null;
		}
		quals[1] = new EOKeyValueQualifier(ABS_GRADE_KEY,
				EOQualifier.QualifierOperatorLessThan,new Integer(eduYear - maxGrade));
		fs.setQualifier(quals[1]);
		EOSortOrdering so = new EOSortOrdering(ABS_GRADE_KEY,EOSortOrdering.CompareDescending);
		args = new NSArray(so);
		fs.setSortOrderings(args);
		NSArray list = ec.objectsWithFetchSpecification(fs);
		NSMutableDictionary currdict = null;
		if(list != null && list.count() > 0) {
			Enumeration stenu = list.objectEnumerator();
			eduYear = eduYear - maxGrade;
			while (stenu.hasMoreElements()) {
				VseStudent st = (VseStudent) stenu.nextElement();
				if(st.absGrade().intValue() < eduYear) {
					eduYear = st.absGrade().intValue();
					while(grp != null && grp.absGrade().intValue() < eduYear) {
						NSMutableDictionary dict = dict(
								grp.grade().toString(), grp.absGrade(), quals) ;
						result.addObject(dict);
						int absGrade = grp.absGrade().intValue();
						while(grp != null && grp.absGrade().intValue() == absGrade ) {
							result.addObject(grp);
							grp = (grenu.hasMoreElements())?(VseEduGroup)grenu.nextElement():null;
						}
					}
					currdict = dict(
							Integer.toString(st.currGrade()), st.absGrade(),quals);
//					currdict.takeValueForKey(Boolean.TRUE, "hide");
					result.addObject(currdict);
					while(grp != null && grp.absGrade().intValue() == eduYear ) {
						result.addObject(grp);
						grp = (grenu.hasMoreElements())?(VseEduGroup)grenu.nextElement():null;
//						currdict.takeValueForKey(Boolean.FALSE, "hide");
					}
				} // next grade
				if(st.recentMainEduGroup() == null) {
//						currdict.takeValueForKey(Boolean.FALSE, "hide");
					currdict.valueForKeyPath("allCount.raise");
					if(quals[0].evaluateWithObject(st))
						currdict.valueForKeyPath("currCount.raise");
				}
			}
		}
		while(grp != null) {
			NSMutableDictionary dict = dict(
					grp.grade().toString(), grp.absGrade(), quals) ;
			result.addObject(dict);
			int absGrade = grp.absGrade().intValue();
			while(grp != null && grp.absGrade().intValue() == absGrade ) {
				result.addObject(grp);
				grp = (grenu.hasMoreElements())?(VseEduGroup)grenu.nextElement():null;
			}
		}
		return result;
	}

	private static NSMutableDictionary dict(String name,Integer absGrade,EOQualifier[] quals) {
		NSMutableDictionary dict = new NSMutableDictionary(name,"name");
		dict.takeValueForKey("grey","styleClass");
		dict.takeValueForKey(absGrade, "absGrade");
		quals[1] = new EOKeyValueQualifier(ABS_GRADE_KEY,
				EOQualifier.QualifierOperatorEqual,absGrade);
//		dict.takeValueForKey(quals[1], "allQual");
//		quals[1] = new EOAndQualifier(new NSArray(quals));
		dict.takeValueForKey(quals[1], "qualifier");
		dict.takeValueForKey(new Counter(), "allCount");
		dict.takeValueForKey(new Counter(), "currCount");
		return dict;
	}
	
	public void validateForSave() {
		super.validateForSave();
		VseList.validateDates(enter(), leave());
	}
}
