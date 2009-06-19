// StudentMarks.java: Class file for WO Component 'StudentMarks'

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

package net.rujel.criterial;

import net.rujel.interfaces.*;
import net.rujel.reusables.*;
import net.rujel.base.MyUtility;

import com.webobjects.foundation.*;
import com.webobjects.appserver.*;
import com.webobjects.eocontrol.*;

import java.text.Format;
import java.util.Enumeration;
import java.util.logging.Logger;
import java.math.BigDecimal;

public class StudentMarks extends WOComponent {	
	public static final String workIntegral = SettingsReader.stringForKeyPath("criterial.workIntegral","%");
/*
	public Student student;
	public NSTimestamp since;
	public NSTimestamp to;
	public Period period;
	public NSKeyValueCoding reporter;
*/
//	protected NSMutableArray courses;
//	public NSMutableArray coursePresent;
//    public NSMutableDictionary courseItem;
    public NSKeyValueCoding workItem;
    public Object critItem;
    
    protected static final NSArray FLAGS = new NSArray(new Object[] {
    		"marked","markable","all"}); 
	
	protected static Format dateFormat = MyUtility.dateFormat();
	
	public NSDictionary courseItem() {
		return (NSDictionary)valueForBinding("value");
	}
	
	public void awake() {
		super.awake();
		synchronized (dateFormat) {
			dateFormat = MyUtility.dateFormat();
		}
	}
	
	protected static NSTimestamp date2timestamp(java.util.Date date) {
		if(date instanceof NSTimestamp)
			return (NSTimestamp)date;
		else
			return new NSTimestamp(date);
	}
	
	public static NSDictionary reportForStudent(NSDictionary settings) {
		NamedFlags options = (NamedFlags)settings.valueForKey("marks");	
		if(options == null)
			return null;
		
		Student student = (Student)settings.valueForKey("student");
		EOEditingContext ec = student.editingContext();
		
		NSMutableDictionary result = ((NSDictionary)WOApplication.application()
				.valueForKeyPath("strings.RujelCriterial_Strings.marksReport")).mutableClone();
		int count = 0;
		NSTimestamp since = (NSTimestamp)settings.valueForKey("since");
		NSTimestamp to = (NSTimestamp)settings.valueForKey("to");
		Period period = (Period)settings.valueForKey("period");
		if(!(period instanceof EOEnterpriseObject))
			period = null;

		NSArray courses = (NSArray)settings.valueForKey("courses");
		if(courses == null)
			return null;
		NSMutableArray[] allWorks = new NSMutableArray[courses.count()];
		
		//Enumeration enu = courses.objectEnumerator();
		NSMutableArray args = new NSMutableArray();
		if(since != null)
			args.addObject(new EOKeyValueQualifier(Work.DATE_KEY,
					EOQualifier.QualifierOperatorGreaterThanOrEqualTo,since));
		if(to != null)
			args.addObject(new EOKeyValueQualifier(Work.DATE_KEY,
					EOQualifier.QualifierOperatorLessThanOrEqualTo,to));
//		EOQualifier qual = EOQualifier.qualifierWithQualifierFormat("date >= %@ AND date <= %@",args);
		for(int i = 0; i < courses.count(); i++) { //get works for courses;
			EduCourse c = (EduCourse)courses.objectAtIndex(i);
			NSMutableArray quals = args.mutableClone();//new NSMutableArray(qual);
			quals.add(new EOKeyValueQualifier("course",EOQualifier.QualifierOperatorEqual,c));
			if(!options.flagForKey("all")) {
				if(options.flagForKey("markable")) {
					/*NSArray qs = new NSArray(new Object[] {
							new EOKeyValueQualifier(Work.WEIGHT_KEY,
									EOQualifier.QualifierOperatorGreaterThan,BigDecimal.ZERO),
							new EOKeyValueQualifier(Work.CRITER_MASK_KEY,
									EOQualifier.QualifierOperatorNotEqual,NullValue)
					});
					quals.addObject(new EOOrQualifier(qs));*/
					quals.addObject(new EOKeyValueQualifier(Work.CRITER_MASK_KEY,
									EOQualifier.QualifierOperatorNotEqual,NullValue));
				} else {
					quals.addObject(new EOKeyValueQualifier(Work.WEIGHT_KEY,
							EOQualifier.QualifierOperatorGreaterThan,BigDecimal.ZERO));
				}
				if(options.flagForKey("marked")) {
					quals.addObject(new EOKeyValueQualifier(Work.TYPE_KEY,
							EOQualifier.QualifierOperatorNotEqual,new Integer(Work.OPTIONAL)));
				}
			}
			
			EOFetchSpecification fs = new EOFetchSpecification("Work",
					new EOAndQualifier(quals),EduLesson.sorter);
			fs.setRefreshesRefetchedObjects(true);
			NSArray works = ec.objectsWithFetchSpecification(fs);
			if(works != null && works.count() > 0) {
				allWorks[i] = works.mutableClone();
				
				if(!(options.flagForKey("all") || options.flagForKey("marked"))) {
					EOQualifier q = new EOKeyValueQualifier(Work.TYPE_KEY,
							EOQualifier.QualifierOperatorEqual,new Integer(Work.OPTIONAL));
					NSArray optional = EOQualifier.filteredArrayWithQualifier(works, q);
					if(optional != null && optional.count() > 0) {
						Enumeration enu = optional.objectEnumerator();
						while (enu.hasMoreElements()) {
							Work w = (Work) enu.nextElement();
							if(w.forPersonLink(student) == null)
								allWorks[i].removeObject(w);
						}
					}
				} // removing not estimated optional works
			} // add result to allWorks
		}
		
		NSMutableArray extraWorks = new NSMutableArray();
		if(options.flagForKey("marked")) { //add works with marks
			args.removeAllObjects();
//			args.addObjects(new Object[] { student,since,to,since,to });
//			String qualifierFormat = "student = %@ AND ";
			args.addObject(new EOKeyValueQualifier("student",
					EOQualifier.QualifierOperatorEqual,student));
			if(period != null) {
				args.addObject(new EOKeyValueQualifier("work.date",
						EOQualifier.QualifierOperatorGreaterThanOrEqualTo,period.begin()));
				args.addObject(new EOKeyValueQualifier("work.date",
						EOQualifier.QualifierOperatorLessThanOrEqualTo,period.end()));
//				qualifierFormat = qualifierFormat + "(work.date >= %@ AND work.date <= %@)";
			} else if(since != null || to != null) {
				EOQualifier[] or = new EOQualifier[2];
				if(since != null) {
					or[0] = new EOKeyValueQualifier("work.date",
							EOQualifier.QualifierOperatorGreaterThanOrEqualTo,since);
					or[1] = new EOKeyValueQualifier(Mark.DATE_SET_KEY,
							EOQualifier.QualifierOperatorGreaterThanOrEqualTo,since);
					args.addObject(new EOOrQualifier(new NSArray(or)));
				}
				if(to != null) {
					or[0] = new EOKeyValueQualifier("work.date",
							EOQualifier.QualifierOperatorLessThanOrEqualTo,to);
					or[1] = new EOKeyValueQualifier(Mark.DATE_SET_KEY,
							EOQualifier.QualifierOperatorLessThanOrEqualTo,to);
					args.addObject(new EOOrQualifier(new NSArray(or)));
				}
//				qualifierFormat = qualifierFormat + "((dateSet >= %@ AND dateSet <= %@) OR (work.date >= %@ AND work.date <= %@))";
			}
//			EOQualifier qual = EOQualifier.qualifierWithQualifierFormat(qualifierFormat,args);
			EOFetchSpecification fs = new EOFetchSpecification("Mark",
					new EOAndQualifier(args),null);
			fs.setRefreshesRefetchedObjects(true);
			NSArray allMarks = ec.objectsWithFetchSpecification(fs);
			//insert WorkNotes
			fs.setEntityName("WorkNote");
			if(period == null) {
				while (args.count() > 1) {
					args.removeLastObject();
				}
				if(since != null)
					args.addObject(new EOKeyValueQualifier("work.date",
							EOQualifier.QualifierOperatorGreaterThanOrEqualTo,since));
				if(to != null)
					args.addObject(new EOKeyValueQualifier("work.date",
							EOQualifier.QualifierOperatorLessThanOrEqualTo,to));
//				EOQualifier qual = EOQualifier.qualifierWithQualifierFormat(
//						"student = %@ AND work.date >= %@ AND work.date <= %@",args);
				fs.setQualifier(new EOAndQualifier(args));
			}
			NSArray workNotes = ec.objectsWithFetchSpecification(fs);
			if(workNotes != null && workNotes.count() > 0)
				allMarks = allMarks.arrayByAddingObjectsFromArray(workNotes);
			
			Enumeration enu = allMarks.objectEnumerator();
			NSMutableArray extraCourses = new NSMutableArray();
			while (enu.hasMoreElements()) {
				EOEnterpriseObject m = (EOEnterpriseObject)enu.nextElement();
				EduCourse course = (EduCourse)m.valueForKeyPath("work.course");
				if(course == null) {
					Logger.getLogger("rujel.criterial").log(WOLogLevel.INFO,"Dangling mark found",m);
					continue;
				}
				Work w = (Work)m.valueForKey("work");
				int idx = courses.indexOfIdenticalObject(course);
				if(idx == NSArray.NotFound) {
					idx = courses.count();
					extraCourses.addObject(course);
					courses = courses.arrayByAddingObject(course);
					extraWorks.addObject(new NSMutableArray(w));
				} else {
					NSMutableArray works = null;//(NSMutableArray)allWorks.objectAtIndex(i);
					if(idx < allWorks.length)
						works = allWorks[idx];
					else
						works = (NSMutableArray)extraWorks.objectAtIndex(idx - allWorks.length);
					if(works != null) {
					//NSMutableArray cWorks = (NSMutableArray)allWorks.objectAtIndex(idx);
						if(works.indexOfIdenticalObject(w) == NSArray.NotFound) {
							works.addObject(w);
						}
					} else {
						if(idx < allWorks.length)
							allWorks[idx] = new NSMutableArray(w);
					}
				}
			}
			if(extraCourses.count() > 0) {
				result.takeValueForKey(extraCourses, "extraCourses");
			}
		}

		for (int i = 0; i < courses.count(); i++) {
			NSMutableArray works = null;//(NSMutableArray)allWorks.objectAtIndex(i);
			if(i < allWorks.length)
				works = allWorks[i];
			else
				works = (NSMutableArray)extraWorks.objectAtIndex(i - allWorks.length);
			if(works != null && works.count() > 0) {
				EduCourse c = (EduCourse)courses.objectAtIndex(i);
				NSMutableDictionary courseItem = formatCourse(c);
				NSArray criteria = (NSArray)courseItem.valueForKeyPath("criteria.title");
				synchronized (dateFormat) {
					if(works != null && works.count() > 0) {
						courseItem.setObjectForKey(formatWorks(works,criteria),"works");
					}
				}
				result.setObjectForKey(courseItem, c);
				count++;
			}
		}
		if(count == 0)
			return null;
		return result;
	}
/*
	public void appendToResponse(WOResponse aResponse,WOContext aContext) {
		student = (Student)valueForBinding("student");
		super.appendToResponse(aResponse, aContext);
	}
		since = (NSTimestamp)valueForBinding("since");
		to = (NSTimestamp)valueForBinding("to");
		period = (Period)valueForBinding("period");
		if(period != null) {
			if(since == null)
				since = date2timestamp(period.begin());
			if(to == null)
				to = date2timestamp(period.end());
		}
		courses = ((NSArray)valueForBinding("courses")).mutableClone();
		
		EOEditingContext ec = student.editingContext();
		
		NSMutableDictionary printStudentResults = new NSMutableDictionary(student,"student");
		printStudentResults.takeValueForKey(since,"since");
		printStudentResults.takeValueForKey(to,"to");
		if(period != null)
			printStudentResults.takeValueForKey(period,"period");

		WOSession ses = aContext.session();
		ses.setObjectForKey(printStudentResults,"printStudentResults");
		NSArray resultsForCourse = (NSArray)ses.valueForKeyPath("modules.printStudentResults");
		if(resultsForCourse != null && resultsForCourse.count() > 0) {
			printStudentResults = (NSMutableDictionary)resultsForCourse.objectAtIndex(0);
		}
		ses.removeObjectForKey("printStudentResults");

		NSMutableArray args = new NSMutableArray(new Object[] { student,since,to,since,to });
		String qualifierFormat =
		"student = %@ AND ";
		if(period != null && period instanceof EOEnterpriseObject) {
			qualifierFormat = qualifierFormat + "(work.date >= %@ AND work.date <= %@)";
		} else {
			qualifierFormat = qualifierFormat + "((dateSet >= %@ AND dateSet <= %@) OR (work.date >= %@ AND work.date <= %@))";
		}
		EOQualifier q = EOQualifier.qualifierWithQualifierFormat(qualifierFormat,args);
		EOFetchSpecification fs = new EOFetchSpecification("Mark",q,null);
		fs.setRefreshesRefetchedObjects(true);
		NSArray allMarks = ec.objectsWithFetchSpecification(fs);
		//EOUtilities.objectsWithQualifierFormat(ec,"Mark",qualifierFormat,args);
		
		NSMutableArray quals = new NSMutableArray();
		q = new EOKeyValueQualifier("eduYear",EOQualifier.QualifierOperatorEqual,session().valueForKey("eduYear"));
		quals.addObject(q);

		q = new EOKeyValueQualifier("groupList",EOQualifier.QualifierOperatorContains,student);
		quals.addObject(q);

		q = new EOAndQualifier(quals);
		EOQualifier.filterArrayWithQualifier(courses,q);
		args.removeAllObjects();
		args.addObject(EOSortOrdering.sortOrderingWithKey("cycle.subject",EOSortOrdering.CompareCaseInsensitiveAscending));
		EOSortOrdering.sortArrayUsingKeyOrderArray(courses,args);
				
		NSMutableArray allWorks = new NSMutableArray();
		
		Enumeration enu = courses.objectEnumerator();
		while(enu.hasMoreElements()) {
			EduCourse c = (EduCourse)enu.nextElement();
			NSMutableArray works = null;
			NSArray cWorks = c.lessons();
			if(cWorks != null && cWorks.count() > 0) {
				works = cWorks.mutableClone();
				if(since != null || to != null) {
					args.removeAllObjects();
					args.addObjects(new Object[] { since,to });
					EOQualifier qual = EOQualifier.qualifierWithQualifierFormat("date >= %@ AND date <= %@",args);
					EOQualifier.filterArrayWithQualifier(works,qual);
				}
			} else {
				works = new NSMutableArray();
			}
			allWorks.addObject(works);
		}
		
		enu = allMarks.objectEnumerator();
		while (enu.hasMoreElements()) {
			Mark m = (Mark)enu.nextElement();
			Work w = m.work();
			int idx = courses.indexOfIdenticalObject(w.course());
			if(idx == NSArray.NotFound) {
				idx = courses.count();
				courses.addObject(w.course());
				allWorks.addObject(new NSMutableArray(w));
				
			} else {
				NSMutableArray cWorks = (NSMutableArray)allWorks.objectAtIndex(idx);
				if(cWorks.indexOfIdenticalObject(w) == NSArray.NotFound) {
					cWorks.addObject(w);
				}
			}
		}
		
		
		coursePresent = new NSMutableArray();
		for (int i = 0; i < courses.count(); i++) {
			NSMutableArray works = (NSMutableArray)allWorks.objectAtIndex(i);
			if(works != null && works.count() > 0) {
				EduCourse c = (EduCourse)courses.objectAtIndex(i);
				courseItem = formatCourse(c);
				Object res = printStudentResults.objectForKey("title");
				if(res != null)
					courseItem.setObjectForKey(res,"resultTitle");
				res = printStudentResults.objectForKey(c);
				if(res != null)
					courseItem.setObjectForKey(res,"result");
				
				NSArray criteria = (NSArray)courseItem.valueForKeyPath("criteria.title");
				synchronized (dateFormat) {
					courseItem.setObjectForKey(formatWorks(works,criteria),"works");
				}
				coursePresent.addObject(courseItem);
			}
		}
				
		super.appendToResponse(aResponse,aContext);
	}*/
	
    public StudentMarks(WOContext context) {
        super(context);
    }
	
	public boolean isMax() {
		if(workItem == null) return false;
		return "max".equals(workItem.valueForKey("kind"));
	}


	public boolean isStateless() {
		return true;
	}
	
	public boolean synchronizesVariablesWithBindings() {
        return false;
	}
	/*
	public void reset() {
		student = null;
		since = null;
		to = null;
		period = null;
	}
	*/
	public static NSMutableDictionary formatCourse(EduCourse course) {
		NSMutableDictionary result = new NSMutableDictionary();
		/*
		result.setObjectForKey(course.cycle().subject(),"subject");
		if(course.comment() != null) {
			result.setObjectForKey(course.comment(),"comment");
		}
		result.setObjectForKey(Person.Utility.fullName(course.teacher().person(),true,2,2,2), "teacher");
		result.setObjectForKey(course.eduGroup().name(),"eduGroup");
		*/
		String integral = SettingsReader.stringForKeyPath("edu.presenters.workIntegral","~");
		result.takeValueForKey(integral, "integral");

		NSArray criteria = CriteriaSet.criteriaForCycle(course.cycle());
		/*WOApplication app  = WOApplication.application();
		String title = workIntegral;
		String comment = app.valueForKeyPath("strings.RujelCriterial_Strings.integral");
		
		NSDictionary specCrit = new NSDictionary(new Object[] {title,comment},keys.objects());*/
		NSMutableArray critDicts = new NSMutableArray();
		if(criteria != null && criteria.count() > 0) {
			Enumeration en = criteria.objectEnumerator();
			while (en.hasMoreElements()) {
				EOEnterpriseObject criter = (EOEnterpriseObject)en.nextElement();
				NSMutableDictionary critDict = new NSMutableDictionary(
						criter.valueForKey("title"),"title");
				critDict.takeValueForKey(criter.valueForKey("comment"), "comment");
				critDicts.addObject(critDict);
			}
		}
		/*
		title = app.valueForKeyPath("strings.RujelCriterial_Strings.text");
		comment = app.valueForKeyPath("strings.RujelCriterial_Strings.comments");
		specCrit = new NSDictionary(new Object[] {title,comment},keys.objects());
		critDicts.addObject(specCrit);*/
		result.setObjectForKey(critDicts,"criteria");
		
		return result;
	}
	
	public static NSArray formatWorks(NSArray works,NSArray criteria) {
		if(works == null || works.count() ==0) return null;
		NSArray blanc = null;
		if(criteria != null && criteria.count() > 0) {
			Object[] arr = new Object[criteria.count()];
			String empty = "";
			for (int i = 0; i < criteria.count(); i++) {
				arr[i] = empty;
			}
			blanc = new NSArray(arr);
		}
		NSMutableArray maxValues = null;
		NSMutableArray result = new NSMutableArray();
		Enumeration enu = EOSortOrdering.sortedArrayUsingKeyOrderArray(works,Work.sorter).objectEnumerator();
		while(enu.hasMoreElements()) {
			Work currWork = (Work)enu.nextElement();
			NSMutableDictionary curr = new NSMutableDictionary(currWork,"work");
			if(blanc != null) { // prepare criter list
				NSArray currMask = currWork.criterMask();
				if(currMask != null && currMask.count() > 0) {
					NSMutableArray critMask = blanc.mutableClone();
					int status = (maxValues==null)?2:0;
					Enumeration critEnum = currMask.objectEnumerator();
					while(critEnum.hasMoreElements()) {
						EOEnterpriseObject mask = (EOEnterpriseObject)critEnum.nextElement();
						int idx = criteria.indexOfObject(mask.valueForKeyPath("criterion.title"));
						if(idx == NSArray.NotFound)
							throw new IllegalArgumentException("Work employs undescribed criterion");
						Number max = (Number)mask.valueForKey("max");
						critMask.replaceObjectAtIndex(max,idx);
						if(status != 2) {
							Object titleMax = maxValues.objectAtIndex(idx);
							if(titleMax instanceof Number) {
								if(max.intValue() != ((Number)titleMax).intValue()){
									status = 2;
								}
							} else {
								status = 1;
							}
						}
					}
					if(status == 2) {
						maxValues = critMask;
						NSMutableDictionary maxRow = new NSMutableDictionary("max","kind");
						maxRow.setObjectForKey(critMask,"values");
						result.addObject(maxRow);
					} else if(status == 1) {
						for (int i = 0; i < critMask.count(); i++) {
							Object value = critMask.objectAtIndex(i);
							if(value instanceof Number)
								maxValues.replaceObjectAtIndex(value,i);
						}
					}
				}
			} // prepare criter list
			curr.setObjectForKey(dateFormat.format(currWork.announce()),"announce");
			curr.setObjectForKey(dateFormat.format(currWork.date()),"date");
			String theme = currWork.theme();
			if(theme != null && theme.length() > 0)
				curr.setObjectForKey(theme,"theme");
			curr.setObjectForKey(currWork.workType(), "type");
			if(BigDecimal.ZERO.compareTo(currWork.weight()) == 0) {
				curr.setObjectForKey("noWeight","kind");
				curr.setObjectForKey("color:#333333;","style");
			} else
				curr.setObjectForKey("withWeight","kind");
			
			result.addObject(curr);
			
		}
		return result;
	}
}
