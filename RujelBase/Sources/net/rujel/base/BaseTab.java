// BaseTab.java

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

import java.util.Enumeration;

import net.rujel.interfaces.*;
import net.rujel.reusables.*;

import com.webobjects.eoaccess.EOUtilities;
import com.webobjects.eocontrol.*;
import com.webobjects.foundation.*;

public class BaseTab extends _BaseTab {
	public static final NSArray sorter = new NSArray(EOSortOrdering.sortOrderingWithKey(
			FIRST_LESSON_NUMBER_KEY, EOSortOrdering.CompareAscending));

    public BaseTab() {
        super();
    }

/*
    // If you add instance variables to store property values you
    // should add empty implementions of the Serialization methods
    // to avoid unnecessary overhead (the properties will be
    // serialized for you in the superclass).
    private void writeObject(java.io.ObjectOutputStream out) throws java.io.IOException {
    }

    private void readObject(java.io.ObjectInputStream in) 
    throws java.io.IOException, java.lang.ClassNotFoundException {
    }
*/
	public static NSArray baseTabsForCourse(EduCourse course, String entityName) {
		EntityIndex entity = EntityIndex.indexForEntityName(course.editingContext(), entityName);
		return baseTabsForCourse(course, entity);
	}
	
	public static NSArray baseTabsForCourse(EduCourse course, EntityIndex entity) {
		NSArray quals = new NSArray(new EOQualifier[] {
				new EOKeyValueQualifier(COURSE_KEY,EOQualifier.QualifierOperatorEqual,course),
				new EOKeyValueQualifier(FOR_ENTITY_KEY,EOQualifier.QualifierOperatorEqual,entity)
		});
		EOFetchSpecification fs = new EOFetchSpecification(
				ENTITY_NAME,new EOAndQualifier(quals),sorter);
		NSArray found = course.editingContext().objectsWithFetchSpecification(fs);
		return found;
	}
	
	public static NSArray tabsForCourse(EduCourse course, String entityName) {
		EntityIndex entity = EntityIndex.indexForEntityName(course.editingContext(), entityName);
		return tabsForCourse(course, entity);
	}
	public static NSArray tabsForCourse(EduCourse course, EntityIndex entity) {
		NSArray found = baseTabsForCourse(course, entity);
		if(found == null || found.count() == 0)
			return null;
		Integer idx = null;
		NSMutableArray result = new NSMutableArray();
		Enumeration enu = found.objectEnumerator();
		while (enu.hasMoreElements()) {
			BaseTab bt = (BaseTab) enu.nextElement();
			Integer num = bt.firstLessonNumber();
			result.addObject(new Tab(idx,num));
			idx = num;
		}
		result.addObject(new Tab(idx,null));
		return result;
	}
	
	public static BaseTab tabForLesson(EduLesson lesson, Integer number, boolean create, int max) {
		EntityIndex ent = EntityIndex.indexForObject(lesson);
		NSMutableArray quals = new NSMutableArray(new EOKeyValueQualifier(
				FOR_ENTITY_KEY,EOQualifier.QualifierOperatorEqual,ent));
		quals.addObject(new EOKeyValueQualifier(
				COURSE_KEY,EOQualifier.QualifierOperatorEqual,lesson.course()));
		quals.addObject(new EOKeyValueQualifier(FIRST_LESSON_NUMBER_KEY,
				EOQualifier.QualifierOperatorLessThanOrEqualTo, number));
		EOQualifier qual = new EOAndQualifier(quals);
		NSArray sort = new NSArray(EOSortOrdering.sortOrderingWithKey(
				FIRST_LESSON_NUMBER_KEY, EOSortOrdering.CompareDescending));
		EOFetchSpecification fs = new EOFetchSpecification(ENTITY_NAME,qual,sort);
		fs.setFetchLimit(1);
		EOEditingContext ec = lesson.editingContext();
		NSArray found = ec.objectsWithFetchSpecification(fs);
		BaseTab result = (found == null || found.count() == 0)? null :
				(BaseTab)found.objectAtIndex(0);
		if(!create)
			return result;
		if(result != null && number.equals(result.firstLessonNumber()))
			return result;
		qual = new EOKeyValueQualifier(FIRST_LESSON_NUMBER_KEY,
				EOQualifier.QualifierOperatorGreaterThan, new Integer(max));
		quals.replaceObjectAtIndex(qual, 2);
		qual = new EOAndQualifier(quals);
		fs.setQualifier(qual);
		found = ec.objectsWithFetchSpecification(fs);
		result = (found == null || found.count() == 0)? null : (BaseTab)found.objectAtIndex(0);
		if(result == null) {
			result = (BaseTab)EOUtilities.createAndInsertInstance(ec, ENTITY_NAME);
			result.addObjectToBothSidesOfRelationshipWithKey(ent, FOR_ENTITY_KEY);
			result.addObjectToBothSidesOfRelationshipWithKey(lesson.course(), COURSE_KEY);
		}
		result.setFirstLessonNumber(number);
		return result;
	}
	
	/*
	public NSArray lessonsInTab() {
		return course().sortedLessons().subarrayWithRange(range());
	}
	
	public String label() {
		NSArray lessons = lessonsInTab();
		NSTimestamp firstDate = (NSTimestamp)lessons.valueForKeyPath("@min.date");
		NSTimestamp lastDate = (NSTimestamp)lessons.valueForKeyPath("@max.date");
//		if (lastDate != null)
			return String.format("%1$te.%1tm - %2$te.%2tm",firstDate,lastDate);
//		else
//			return String.format("%1$te.%1tm - ...",firstDate);
	}*/

	
	public static class Tab extends NSRange implements Tabs.GenericTab {
		private Integer fn;
		private Integer ln;
//		private EOQualifier qual;
		private String hover;
		protected int code = 0;


		public Tab (Integer first, Integer next) {
			if(first != null)
				code += 1000*first.intValue();
			fn = first;
			if(next != null) {
				ln = new Integer(next.intValue() -1);
				code += next.intValue();
			}
			if(first != null) {
//				qual = new EOKeyValueQualifier(
//						"number",EOQualifier.QualifierOperatorGreaterThanOrEqualTo,first);
				StringBuffer toHover = new StringBuffer(first.toString());
				toHover.append(" ... ");
				if(ln != null) {
/*					NSMutableArray qs = new NSMutableArray(qual);
					qs.addObject(new EOKeyValueQualifier(
							"number",EOQualifier.QualifierOperatorLessThan,last));
					qual = new EOAndQualifier(qs);
*/					toHover.append(ln);
				}
				hover = toHover.toString();
			} else {
//				qual = new EOKeyValueQualifier("number",EOQualifier.QualifierOperatorLessThan,last);
				hover = "1 ... " + ln;
			}
		}
		
		public int location() {
			if(fn == null)
				return 0;
			return fn.intValue() -1;
		}
		
		public int length() {
			if(ln == null)
				return Integer.MAX_VALUE - location();
			return ln.intValue() - location();
		}
		
		public int maxRange() {
			if(ln == null)
				return Integer.MAX_VALUE;
			return ln.intValue();
		}
		
		public boolean equals(Object aTab) {
			if(aTab == this)
				return true;
			if (aTab instanceof Tab) {
				Tab test = (Tab) aTab;
				return (test.code == this.code);
			}
			return false;
		}
		public int hashCode() {			
			return code;
		}
		
		public boolean defaultCurrent() {
			return (ln == null);
		}
		
		public String hover() {
			return hover;
		}

		public EOQualifier qualifier() {
			return null;//qual;
		}

		public String title() {
			if(fn == null)
				return "1";
			return fn.toString();
		}

		public Period period() {
			return null;
		}	
	}
}
