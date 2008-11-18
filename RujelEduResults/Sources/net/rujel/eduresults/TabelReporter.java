// TabelReporter.java: Class file for WO Component 'TabelReporter'

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

package net.rujel.eduresults;

import net.rujel.reusables.*;
import net.rujel.interfaces.*;
import net.rujel.autoitog.*;
// TODO: optional timeouts
import com.webobjects.foundation.*;
import com.webobjects.appserver.*;
import com.webobjects.eocontrol.*;
import com.webobjects.eoaccess.*;
import java.util.Enumeration;

public class TabelReporter extends WOComponent {

    public TabelReporter(WOContext context) {
        super(context);
    }
	
	public boolean synchronizesVariablesWithBindings() {
        return false;
	}
	
	public boolean isStateless() {
		return true;
	}
	
	public void reset() {
		student = null;
		since = null;
		to = null;
		reporter = null;
		courses = null;
		period = null;
		perlist = null;
		perItem = null;
		cycleItem = null;
		timeouts = null;
		cyclesForPerlists = null;
		marksAgregate = null;
		timeoutsDict = null;
		perTypeUsage = null;
		eduPeriods = null;
		courseTimeouts = null;
		eduYear = null;
	}
	public Student student;
	public NSTimestamp since;
	public NSTimestamp to;
	public NSKeyValueCoding reporter;
	protected NSMutableArray courses;
	
	public Period period;
	public NSArray perlist;
    public EduPeriod perItem;
    public NSKeyValueCoding cycleItem;
	public int perIdx;
	
	public NSMutableArray timeouts;
	public NSMutableDictionary cyclesForPerlists;
	protected NSMutableDictionary marksAgregate;
	public NSMutableDictionary timeoutsDict;
		
	protected NSArray perTypeUsage;
	protected NSArray eduPeriods;
	protected NSArray courseTimeouts;
	protected Number eduYear;
	
	public void initialiseReporter(EOEditingContext ec) {
		reporter = (NSKeyValueCoding)valueForBinding("reporter");
		if(to == null && period != null)
			to = new NSTimestamp(period.end());
		if(period instanceof EduPeriod) {
			eduYear = ((EduPeriod)period).eduYear();
		} else {
			eduYear = net.rujel.base.MyUtility.eduYearForDate(to);
		}
		
		perTypeUsage = (NSArray)reporter.valueForKey("perTypeUsage");
		if(perTypeUsage == null) {
			perTypeUsage = EOUtilities.objectsWithQualifierFormat(ec,"PeriodTypeUsage", "(eduYear = %d OR eduYear = 0)",new NSArray(eduYear));

			//pertypes = PeriodType.allPeriodTypes(ec,eduYear);
			reporter.takeValueForKey(perTypeUsage,"perTypeUsage");
		} else {
			perTypeUsage = EOUtilities.localInstancesOfObjects(ec,perTypeUsage);
		}
		if(to == null || !to.equals(reporter.valueForKey("dueDate")))
			courseTimeouts = null;
		else
			courseTimeouts = (NSArray)reporter.valueForKey("courseTimeouts");
		if(courseTimeouts == null) {
			if(period != null && period instanceof EduPeriod) {
				courseTimeouts = EOUtilities.objectsMatchingKeyAndValue(ec,"CourseTimeout","eduPeriod",period);
			} else {
				courseTimeouts = EOUtilities.objectsWithQualifierFormat(ec,"CourseTimeout","dueDate >= %@",new NSArray(to));
			}
			reporter.takeValueForKey(courseTimeouts,"courseTimeouts");
			reporter.takeValueForKey(to,"dueDate");
		} else {
			courseTimeouts = EOUtilities.localInstancesOfObjects(ec,courseTimeouts);
		}
		
		eduPeriods = (NSArray)reporter.valueForKey("eduPeriods");
		if(eduPeriods == null) {
			eduPeriods = EOUtilities.objectsMatchingKeyAndValue(ec,"EduPeriod", "eduYear",eduYear);
			reporter.takeValueForKey(eduPeriods,"eduPeriods");
		} else {
			eduPeriods = EOUtilities.localInstancesOfObjects(ec,eduPeriods);
		}

	}
	
	public void appendToResponse(WOResponse aResponse,WOContext aContext) {
		student = (Student)valueForBinding("student");
		since = (NSTimestamp)valueForBinding("since");
		to = (NSTimestamp)valueForBinding("to");
		period = (Period)valueForBinding("period");
		courses = ((NSArray)valueForBinding("courses")).mutableClone();

		EOEditingContext ec = student.editingContext();
		initialiseReporter(ec);

		EOQualifier qual = new EOKeyValueQualifier("groupList",EOQualifier.QualifierOperatorContains,student);
		EOQualifier.filterArrayWithQualifier(courses,qual);
		EOSortOrdering.sortArrayUsingKeyOrderArray(courses, 
				new NSArray<EOSortOrdering>(new EOSortOrdering("cycle",EOSortOrdering.CompareAscending)));
		
		NSMutableArray params = new NSMutableArray(student);
		params.addObject(eduYear);
		
		NSArray allMarks = EOUtilities.objectsWithQualifierFormat(ec,"ItogMark","student = %@ AND eduPeriod.eduYear = %@",params);

		timeoutsDict = new NSMutableDictionary();
		
		cyclesForPerlists = new NSMutableDictionary();
		NSMutableDictionary list4type = new NSMutableDictionary();
		
		Enumeration enu = courses.objectEnumerator();
		while (enu.hasMoreElements()) { //agregate courses
			//put course
			EduCourse currCourse = (EduCourse)enu.nextElement();
			NSArray coursePertypes = PeriodType.pertypesForCourseFromUsageArray(currCourse,perTypeUsage);
			
			perlist = (NSArray)list4type.objectForKey(coursePertypes);
			if(perlist == null) {
				perlist = list4types(coursePertypes);
				list4type.setObjectForKey(perlist,coursePertypes);
			}
				
			NSMutableArray cyclesForPerlist = (NSMutableArray)cyclesForPerlists.objectForKey(perlist);
			if(cyclesForPerlist == null) {
				cyclesForPerlist = new NSMutableArray(currCourse.cycle());
				cyclesForPerlists.setObjectForKey(cyclesForPerlist,perlist);
			} else {
				cyclesForPerlist.addObject(currCourse.cycle());
			}
			
			//put course timeout
			if(courseTimeouts != null && courseTimeouts.count() > 0) {
				qual = CourseTimeout.qualifierForCourseAndPeriod(currCourse,null);
				NSArray tos = EOQualifier.filteredArrayWithQualifier(courseTimeouts,qual);
				if(tos != null && tos.count() > 0) {
					if(tos.count() > 1) {
						if(tos instanceof NSMutableArray) {
							EOSortOrdering.sortArrayUsingKeyOrderArray((NSMutableArray)tos,Timeout.sorter);
						} else {
							tos = EOSortOrdering.sortedArrayUsingKeyOrderArray(tos,Timeout.sorter);
						}
					}
					timeoutsDict.setObjectForKey(tos.objectAtIndex(0),currCourse.cycle());
				}
			}
		} //agregate courses
		
		marksAgregate = new NSMutableDictionary();
		enu = allMarks.objectEnumerator();
		while (enu.hasMoreElements()) { //agregate Marks
			ItogMark currMark = (ItogMark)enu.nextElement();
			Enumeration pen = cyclesForPerlists.keyEnumerator();
getgroup:	while (pen.hasMoreElements()) {
				NSArray curPerList = (NSArray)pen.nextElement();
				perIdx = curPerList.indexOfIdenticalObject(currMark.eduPeriod());
				if(perIdx == NSArray.NotFound)
					continue getgroup;
				NSArray cycles = (NSArray)cyclesForPerlists.objectForKey(curPerList);
				if(cycles.containsObject(currMark.cycle())) {
					ItogMark[] agregate = (ItogMark[])marksAgregate.objectForKey(currMark.cycle());
					if(agregate == null) {
						agregate = new ItogMark[curPerList.count()];
						marksAgregate.setObjectForKey(agregate,currMark.cycle());
					}
					agregate[perIdx] = currMark;
					break getgroup;
				}
			}
		} //agregate Marks

		//agregate Timeouts
		//NSMutableDictionary dict = new NSMutableDictionary(student,"student");
		params.removeAllObjects();
		params.addObject(student);
		NSArray tos = null;
		if(period != null && period instanceof EduPeriod) {
			params.addObject(period);
			tos = EOUtilities.objectsWithQualifierFormat(ec,"StudentTimeout","student = %@ AND eduPeriod >= %@",params);
			//objectsMatchingKeyAndValue(ec,"Timeout","eduPeriod",period);
			//dict.setObjectForKey(period,"eduPeriod");
		} else {
			params.addObject(to);
			tos = EOUtilities.objectsWithQualifierFormat(ec,"StudentTimeout","student = %@ AND dueDate >= %@",params);
			//dict.setObjectForKey(to,"dueDate");
		}
		//NSArray tos = EOUtilities.objectsMatchingValues(ec,"StudentTimeout",dict);
		enu = tos.objectEnumerator();
		while (enu.hasMoreElements()) {
			Timeout curTo = (Timeout)enu.nextElement();
			if(curTo.eduCourse() != null) {
				timeoutsDict.setObjectForKey(curTo,curTo.eduCourse().cycle());
			} else {
				if(timeouts == null) {
					timeouts = new NSMutableArray(curTo);
				} else {
					timeouts.addObject(curTo);
				}
			}
		}
		super.appendToResponse(aResponse,aContext);
	}
	
	protected NSArray list4types(NSArray types) {
		if(types == null || types.count() == 0) {
			return types;
		}
		NSArray sorter = new NSArray(EOSortOrdering.sortOrderingWithKey("num",EOSortOrdering.CompareAscending));
		NSMutableArray list = new NSMutableArray();
		Enumeration enu = types.objectEnumerator();
		while (enu.hasMoreElements()) {
			PeriodType type = (PeriodType)enu.nextElement();
			NSMutableDictionary dict = new NSMutableDictionary(eduYear,"eduYear");
			dict.setObjectForKey(type,"periodType");
			EOQualifier qual = EOQualifier.qualifierToMatchAllValues(dict);
			//NSArray pers = EOUtilities.objectsMatchingValues(type.editingContext(),"EduPeriod",dict);
			NSArray pers = EOQualifier.filteredArrayWithQualifier(eduPeriods,qual);
			if(pers instanceof NSMutableArray) {
				EOSortOrdering.sortArrayUsingKeyOrderArray((NSMutableArray)pers,sorter);
			} else {
				pers = EOSortOrdering.sortedArrayUsingKeyOrderArray(pers,sorter);
			}
			list.addObjectsFromArray(pers);
		}
		return list;
	}
	
	public NSArray cycles() {
		return (NSArray)cyclesForPerlists.objectForKey(perlist);
	}
	
	public ItogMark mark() {
		ItogMark[] agregate = (ItogMark[])marksAgregate.objectForKey(cycleItem);
		return (agregate==null)?null:agregate[perIdx];
	}
	
	public NSKeyValueCoding timeout() {
		return (NSKeyValueCoding)timeoutsDict.objectForKey(cycleItem);
	}
	
	public String periodTitle() {
		if(perItem.countInYear() > 1) {
			return Various.makeRoman(perItem.num().intValue()) + "<br/>\n<small>" + perItem.periodType().title() + "<small>";
		}
		return perItem.periodType().title();
	}
	
}
