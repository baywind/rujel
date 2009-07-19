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
import net.rujel.auth.*;
import com.webobjects.foundation.*;
import com.webobjects.appserver.WOMessage;
import com.webobjects.eocontrol.*;
import com.webobjects.eoaccess.EOUtilities;
import java.util.Enumeration;

public class BaseCourse extends EOGenericRecord implements EduCourse, UseAccess
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

	public static final NSArray accessKeys = new NSArray (new String[] {
		"read","create","edit","delete","openCourses","createNewEduPlanCourses","editSubgroups"});

	private transient NamedFlags _access;
	public NamedFlags access() {
		if(_access == null) {
			_access = StaticImplementation.access(this,accessKeys);
		}
		return _access.immutableClone();
	}
	/*
	private transient NSMutableDictionary _schemeCache;
	public NamedFlags schemeAccess(String schemePath) {
		NamedFlags result = null;
		if(_schemeCache == null) {
			_schemeCache = new NSMutableDictionary();
		} else {
			result = (NamedFlags)_schemeCache.objectForKey(schemePath);
		}
		if(result == null) {
			result = StaticImplementation.schemeAccess(this,schemePath);
			if(result == null) {
				result = DegenerateFlags.ALL_FALSE;
			}
			_schemeCache.setObjectForKey(result,schemePath);
		}
		if(result == DegenerateFlags.ALL_FALSE)
			return null;
		else return result;
	}*/
	
	public boolean isOwned() {
		return StaticImplementation.isOwned(this);
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
	
    public String comment() {
        return (String)storedValueForKey("comment");
    }
	
    public void setComment(String aValue) {
        takeStoredValueForKey(aValue, "comment");
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
	
	public Integer eduYear() {
        return (Integer)storedValueForKey("eduYear");
    }
	
    public void setEduYear(Integer year) {
        takeStoredValueForKey(year, "eduYear");
    }
	
    public NSArray lessons() {
        return (NSArray)storedValueForKey("lessons");
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
			//audience.takeValuesFromDictionary(dict);
			addObjectToBothSidesOfRelationshipWithKey(audience,"audience");
			audience.addObjectToBothSidesOfRelationshipWithKey((EOEnterpriseObject)dict.objectForKey("student"),"student");
//			if(_subgroup != null)
//				_subgroup.addObject(dict.objectForKey("student"));
		}
		
    }
	
    public void removeFromSubgroup(Student object) {
		NSDictionary dict = audienceDictForStudent(object);
		NSArray aud = EOUtilities.objectsMatchingValues(editingContext(),"CourseAudience",dict);
		if(aud != null && aud.count() > 0) {
			EOEnterpriseObject audience = (EOEnterpriseObject)aud.lastObject();
			removeObjectFromBothSidesOfRelationshipWithKey(audience,"audience");
//			if(_subgroup != null)
//				_subgroup.removeObject(dict.objectForKey("student"));
		}
    }
	
	/*
	public NSArray audience() {
		_subgroup = null;
        return (NSArray)storedValueForKey("audience");
    }
	
    public void setAudience(NSArray aValue) {
        takeStoredValueForKey(aValue, "audience");
		_subgroup = null;
   }
	
    public void addToAudience(EOEnterpriseObject object) {
		if(audience() == null || !audience().containsObject(object)) {
			includeObjectIntoPropertyWithKey(object, "audience");
			Object st = object.storedValueForKey("student");
			if(_subgroup != null && st != null)_subgroup.addObject(st);
		}
    }
	
    public void removeFromAudience(EOEnterpriseObject object) {
		excludeObjectFromPropertyWithKey(object, "audience");
		Object st = object.storedValueForKey("student");
		if(_subgroup != null && st != null)_subgroup.removeObject(st);
   }
*/
	public void setSubgroup(NSArray newList) {
		if(newList == null || newList.count() == 0) {
			takeStoredValueForKey(new NSArray(),"audience");
			return;
		}
		NSMutableSet subSet = new NSMutableSet(newList);
		NSSet stuSet = new NSSet(eduGroup().list());
		if (stuSet.setBySubtractingSet(subSet).count() == 0) {
			takeStoredValueForKey(new NSArray(),"audience");
			return;
		}
		NSArray audience = (NSArray)storedValueForKey("audience");
		EOEnterpriseObject aud;
		if(audience != null && audience.count() > 0) {
			Enumeration enumerator = audience.objectEnumerator();
			NSMutableArray tmp = new NSMutableArray(audience.count());
			while (enumerator.hasMoreElements()) {
				aud = (EOEnterpriseObject)enumerator.nextElement();
				Object stu = aud.storedValueForKey("student");
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
		NSArray studentsList = eduGroup().list();
		NSArray audience = (NSArray)storedValueForKey("audience");
		if(audience == null || audience.count() == 0) {
			return studentsList;
		}
		NSMutableArray tmp = new NSMutableArray();
		Enumeration enumerator = audience.objectEnumerator();
		EOEnterpriseObject aud;
		Object stu;
		while (enumerator.hasMoreElements()) {
			aud = (EOEnterpriseObject)enumerator.nextElement();
			stu = aud.storedValueForKey("student");
			if(studentsList.containsObject(stu))
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
}
