// BaseCourse.java

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

package net.rujel.base;

import net.rujel.interfaces.*;
import net.rujel.reusables.*;

import com.webobjects.foundation.*;
import com.webobjects.appserver.WOMessage;
import com.webobjects.eocontrol.*;
import com.webobjects.eoaccess.EOUtilities;
import java.util.Enumeration;
import java.util.logging.Logger;

public class BaseCourse extends _BaseCourse implements EduCourse
{
    public BaseCourse() {
        super();
    }

	public static void init() {
		//EOEntity courseEntity = 
		EOInitialiser.initialiseRelationship("BaseCourse","eduGroup",false,"groupID","EduGroup").entity();
		EOInitialiser.initialiseRelationship("BaseCourse","teacher",false,"teacherID","Teacher");
		EOInitialiser.initialiseRelationship("BaseCourse","cycle",false,"cycleID","EduCycle");
		
		EOInitialiser.initialiseRelationship("CourseAudience","student",false,"studentID","Student");
		EOInitialiser.initialiseRelationship("TeacherChange","teacher",false,"teacherID","Teacher");

		/*
		EORelationship subgroupRelationship = new EORelationship();
		subgroupRelationship.setEntity(courseEntity);
		subgroupRelationship.setName("subgroup");
		subgroupRelationship.setDefinition("audience.student");
		subgroupRelationship.setIsMandatory(false);
		courseEntity.addRelationship(subgroupRelationship); */
		EOSortOrdering.ComparisonSupport.setSupportForClass(
				new BaseCourse.ComparisonSupport(), BaseCourse.class);
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
	public Teacher teacher() {
		return (Teacher)storedValueForKey("teacher");
	}
	public void setTeacher(Teacher newTeacher) {
		if(newTeacher != null && newTeacher.editingContext() != editingContext())
			newTeacher = (Teacher)EOUtilities.localInstanceOfObject(editingContext(),newTeacher);
		takeStoredValueForKey(newTeacher, "teacher");
	}
	
	public Teacher teacher(NSTimestamp onDate) {
		EOEnterpriseObject tc = teacherChange(onDate);
		if(tc == null)
			return teacher();
		return (Teacher)tc.valueForKey("teacher");
	}
	
	public EOEnterpriseObject teacherChange(NSTimestamp onDate) {
		return teacherChange(onDate,null);
	}
	public EOEnterpriseObject teacherChange(NSTimestamp onDate, NSTimestamp[] dates) {
		if(onDate == null || flags().intValue() == 0 || teacherChanges().count() == 0) {
			if(dates != null) {
				dates[0] = null;
				dates[1] = null;
			}
			return null;
		}
		EOEnterpriseObject result = null;
		Enumeration enu = teacherChanges().objectEnumerator();
		if(dates == null)
			dates = new NSTimestamp[2];
		while (enu.hasMoreElements()) {
			EOEnterpriseObject tc = (EOEnterpriseObject) enu.nextElement();
			NSTimestamp date = (NSTimestamp)tc.valueForKey("date");
			if(date.compare(onDate) <= 0) {
				if(dates[0] == null || dates[0].compare(date) < 0)
					dates[0] = date;
				continue;
			}
			if(dates[1] != null && dates[1].compare(date) < 0)
				continue;
			dates[1] = date;
			result = tc;
		}
		return result;
	}
	
	public EduGroup eduGroup() {
		return (EduGroup)storedValueForKey("eduGroup");
	}
	public void setEduGroup(EduGroup newEduGroup) {
		takeStoredValueForKey(newEduGroup,"eduGroup");
	}
	public EduCycle cycle() {
        return (EduCycle)storedValueForKey("cycle");
    }
	
    public void setCycle(EduCycle aValue) {
        takeStoredValueForKey(aValue, "cycle");
    }
    
    public void awakeFromInsertion(EOEditingContext ec) {
    	super.awakeFromInsertion(ec);
		setFlags(new Integer(0));
    }

	public void validateForSave() throws NSValidation.ValidationException {
		super.validateForSave();
		if(cycle() == null)
			throw new NSValidation.ValidationException("You should specify eduCycle");
		if(eduGroup() == null)
			throw new NSValidation.ValidationException("You should specify eduGroup");
		Number cg = cycle().grade();
		Integer eg = eduGroup().grade();
		if(cg != null && eg != null && !cg.equals(eg))
			throw new NSValidation.ValidationException("Grade of cycle does not match grade of edu group");
	}
	
	public NSArray sortedLessons() {
		NSArray less = lessons();
		if(less == null || less.count() == 0) return null;
		return EOSortOrdering.sortedArrayUsingKeyOrderArray(less,EduLesson.sorter);
	}
	/*
	private transient NSMutableArray _subgroup;
	
	public NSArray subgroup() {
		if(_subgroup == null) {
			_subgroup = new NSMutableArray();
			NSArray audience = (NSArray)storedValueForKey("audience");
			if(audience == null || audience.count() == 0) {
				return _subgroup.immutableClone();
			}
			Enumeration enumerator = audience.objectEnumerator();
			while (enumerator.hasMoreElements()) {
				EOEnterpriseObject anObject = (EOEnterpriseObject)enumerator.nextElement();
				_subgroup.addObject(anObject.storedValueForKey("student"));
			}
		//	_subgroup = tmp.immutableClone();
		}
		return _subgroup.immutableClone();
	}
	
	public void setSubgroup(NSArray aValue) {
		if(aValue == null || aValue.count() == 0) {
			_subgroup = new NSMutableArray();
			takeStoredValueForKey(_subgroup,"audience");
			return;
		}
		if(_subgroup == null) subgroup();
		
		NSSet input = new NSSet(aValue);
		NSSet recent = new NSSet(_subgroup);
		
		Student st;
		EOEnterpriseObject audience;
		Enumeration enumerator = input.setBySubtractingSet(recent).objectEnumerator();
		while (enumerator.hasMoreElements()) {
			st = (Student)enumerator.nextElement();
			audience = EOUtilities.createAndInsertInstance(editingContext(),"CourseAudience");
			audience.takeStoredValueForKey(st,"student");
			audience.takeStoredValueForKey(this,"course");
			
			addToAudience(audience);
		}
		
		enumerator = recent.setBySubtractingSet(input).objectEnumerator();
		while (enumerator.hasMoreElements()) {
			removeFromSubgroup((Student)enumerator.nextElement());
		}
		
    }
	*/
	
	protected NSDictionary audienceDictForStudent(Student student) {
		return new NSDictionary(new Object[] {
			this, EOUtilities.localInstanceOfObject(editingContext(),student)
		}, new Object[] {
			"course","student"
		});
	}
	
	public boolean isInSubgroup (Student student) {
		NSDictionary dict = audienceDictForStudent(student);
			NSArray aud = EOUtilities.objectsMatchingValues(editingContext(),"CourseAudience",dict);
			if(aud == null || aud.count() == 0)
				return false;
			else
				return true;
	}
	
	public void setIsInSubgroup(Student student, boolean is) {
		if(is)
			addToSubgroup(student);
		else
			removeFromSubgroup(student);
	}
	
    public void addToSubgroup(Student object) {
		NSDictionary dict = audienceDictForStudent(object);
		NSArray aud = EOUtilities.objectsMatchingValues(editingContext(),"CourseAudience",dict);
		if(aud == null || aud.count() == 0) {
			EOEnterpriseObject audience = EOUtilities.createAndInsertInstance(editingContext(),"CourseAudience");
			addObjectToBothSidesOfRelationshipWithKey(audience,"audience");
			audience.addObjectToBothSidesOfRelationshipWithKey((EOEnterpriseObject)dict.objectForKey("student"),"student");
		}
		
    }
	
    public void removeFromSubgroup(Student object) {
		NSDictionary dict = audienceDictForStudent(object);
		NSArray aud = EOUtilities.objectsMatchingValues(editingContext(),"CourseAudience",dict);
		if(aud != null && aud.count() > 0) {
			EOEnterpriseObject audience = (EOEnterpriseObject)aud.lastObject();
			removeObjectFromBothSidesOfRelationshipWithKey(audience,"audience");
		}
    }
	
	public void setSubgroup(NSArray newList) {
		if(newList == null || newList.count() == 0) {
			setAudience(NSArray.EmptyArray);
			return;
		}
		NSMutableSet subSet = new NSMutableSet(newList);
		NSSet stuSet = new NSSet(eduGroup().list());
		if (!namedFlags().flagForKey("mixedGroup") && 
				stuSet.setBySubtractingSet(subSet).count() == 0) {
			setAudience(NSArray.EmptyArray);
			return;
		}
		NSArray audience = audience();
		EOEnterpriseObject aud;
		if(audience != null && audience.count() > 0) {
			Enumeration enumerator = audience.objectEnumerator();
			NSMutableArray tmp = new NSMutableArray(audience.count());
			while (enumerator.hasMoreElements()) {
				aud = (EOEnterpriseObject)enumerator.nextElement();
				Object stu = aud.valueForKey("student");
				if(subSet.containsObject(stu))
					subSet.removeObject(stu);
				else
					tmp.addObject(aud);
			}
			if(tmp.count() > 0) {
				enumerator = tmp.objectEnumerator();
				while (enumerator.hasMoreElements()) {
					aud = (EOEnterpriseObject)enumerator.nextElement();
					removeObjectFromBothSidesOfRelationshipWithKey(aud,"audience");
				}
			}
		}
		if(subSet.count() > 0) {
			Enumeration enumerator = subSet.objectEnumerator();
			while (enumerator.hasMoreElements()) {
				EOEnterpriseObject stu = (EOEnterpriseObject)enumerator.nextElement();
				aud = EOUtilities.createAndInsertInstance(editingContext(),"CourseAudience");
				addObjectToBothSidesOfRelationshipWithKey(aud,"audience");
				aud.addObjectToBothSidesOfRelationshipWithKey(stu,"student");	
			}
		}
//		NSMutableArray tmp = new NSMutableArray();
	}

	public NSArray groupList() {
		NSArray studentsList = (namedFlags().flagForKey("mixedGroup"))?null
				:eduGroup().list();
		NSArray audience = (NSArray)storedValueForKey("audience");
		if(audience == null || audience.count() == 0) {
			return (studentsList==null)?NSArray.EmptyArray:studentsList;
		}
		NSMutableArray tmp = new NSMutableArray();
		Enumeration enumerator = audience.objectEnumerator();
		EOEnterpriseObject aud;
		Object stu;
		while (enumerator.hasMoreElements()) {
			aud = (EOEnterpriseObject)enumerator.nextElement();
			stu = aud.valueForKey("student");
			if(studentsList == null || studentsList.containsObject(stu))
				tmp.addObject(stu);
		}
		return EOSortOrdering.sortedArrayUsingKeyOrderArray(tmp,Person.sorter);
	}

/*	public NSArray sortedTabs() {
		NSArray tabSorter = new NSArray(EOSortOrdering.sortOrderingWithKey("firstLessonNumber",EOSortOrdering.CompareAscending));
		NSArray tabs = (NSArray)storedValueForKey("lessonTabs");
		if(tabs == null || tabs.count() == 0) return null;
		return EOSortOrdering.sortedArrayUsingKeyOrderArray(tabs,tabSorter);
	}*/
	
	public static class ComparisonSupport extends EOSortOrdering.ComparisonSupport {

		public int compareAscending(Object left, Object right) {
			try{
				EduCourse leftCourse = (EduCourse)left;
				EduCourse rightCourse = (EduCourse)right;
				int result = compareValues(leftCourse.eduGroup(), rightCourse.eduGroup(), 
						EOSortOrdering.CompareAscending);
				if(result != NSComparator.OrderedSame)
					return result;
				result = compareValues(leftCourse.cycle(), rightCourse.cycle(), 
						EOSortOrdering.CompareAscending);
				if(result != NSComparator.OrderedSame)
					return result;
				result = compareValues(leftCourse.comment(), rightCourse.comment(), 
						EOSortOrdering.CompareAscending);
				if(result != NSComparator.OrderedSame)
					return result;
				result = compareValues(leftCourse.valueForKeyPath("teacher.person"), 
						rightCourse.valueForKeyPath("teacher.person")
						, EOSortOrdering.CompareAscending);
				return result;
				
			} catch (Exception e) {
				return super.compareAscending(left, right);
			}
			//return NSComparator.OrderedSame;
		}

		public int compareCaseInsensitiveAscending(Object left, Object right) {
			try{
				EduCourse leftCourse = (EduCourse)left;
				EduCourse rightCourse = (EduCourse)right;
				int result = compareValues(leftCourse.cycle(), rightCourse.cycle(), 
						EOSortOrdering.CompareCaseInsensitiveAscending);
				if(result != NSComparator.OrderedSame)
					return result;
				result = compareValues(leftCourse.comment(), rightCourse.comment(), 
						EOSortOrdering.CompareCaseInsensitiveAscending);
				if(result != NSComparator.OrderedSame)
					return result;
				result = compareValues(leftCourse.valueForKeyPath("teacher.person"), 
						rightCourse.valueForKeyPath("teacher.person")
						, EOSortOrdering.CompareCaseInsensitiveAscending);
				return result;
				
			} catch (Exception e) {
				return super.compareCaseInsensitiveAscending(left, right);
			}
		}

		public int compareDescending(Object left, Object right) {
			return compareAscending(right, left);
		}

		public int compareCaseInsensitiveDescending(Object left, Object right) {
			return compareCaseInsensitiveAscending(right, left);
		}
	}

	public String subjectWithComment() {
		if(comment() == null)
			return cycle().subject();
		StringBuilder result = new StringBuilder(WOMessage.stringByEscapingHTMLString(
				cycle().subject()));
		result.append(" <span style = \"font-style:italic;\">(");
		result.append(WOMessage.stringByEscapingHTMLString(comment())).append(")</span>");
		return result.toString();
	}

	public static final NSArray flagNames = new NSArray (new String[] 
	      {"teacherChanged","-2-","-4-","-8-","mixedGroup"});

	private NamedFlags _flags;
	public NamedFlags namedFlags() {
		if(_flags==null) {
			_flags = new NamedFlags(flags().intValue(),flagNames);
			try{
				_flags.setSyncParams(this, getClass().getMethod("setNamedFlags",
						NamedFlags.class));
			} catch (Exception e) {
				Logger.getLogger("rujel.base").log(WOLogLevel.WARNING,
						"Could not get syncMethod for BaseCourse flags",e);
			}
		}
		return _flags;
	}

	public void setNamedFlags(NamedFlags flags) {
		if(flags != null)
			setFlags(flags.toInteger());
		_flags = flags;
	}

	public void setFlags(Integer value) {
		_flags = null;
		super.setFlags(value);
	}
	
	public static NSArray coursesForStudent(NSArray initialCourses, Student student) {
		NSMutableArray result = new NSMutableArray();
		Enumeration enu = initialCourses.objectEnumerator();
		Integer eduYear = null;
		while (enu.hasMoreElements()) {
			EduCourse crs = (EduCourse) enu.nextElement();
			if(eduYear==null)
				eduYear = crs.eduYear();
			NSArray aud = (NSArray)crs.valueForKey("audience");
			if(aud == null || aud.count() == 0)
				result.addObject(crs);
		}
		EOQualifier qual = new EOKeyValueQualifier("student",
				EOQualifier.QualifierOperatorEqual,student);
		EOFetchSpecification fs = new EOFetchSpecification("CourseAudience",qual,null);
		NSArray found = student.editingContext().objectsWithFetchSpecification(fs);
		if(found != null && found.count() > 0) {
			enu = found.objectEnumerator();
			while (enu.hasMoreElements()) {
				EOEnterpriseObject aud = (EOEnterpriseObject) enu.nextElement();
				EduCourse crs = (EduCourse)aud.valueForKey("course");
				if(initialCourses.contains(crs)) {
					result.addObject(crs);
					continue;
				}
				if(crs.eduYear().equals(eduYear) &&
						Various.boolForObject(crs.valueForKeyPath("namedFlags.mixedGroup")))
					result.addObject(crs);
			}
		}
		EOSortOrdering.sortArrayUsingKeyOrderArray(result,new NSArray(
				new EOSortOrdering("cycle",EOSortOrdering.CompareCaseInsensitiveAscending)));
		return result;
	}
}
