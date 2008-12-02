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
		perlist = null;
		perItem = null;
		item = null;
		cyclesForPerlists = null;
		marksAgregate = null;
		perTypeUsage = null;
		eduPeriods = null;
		eduYear = null;
		comments = null;
	}
	public Student student;
	protected Number eduYear;
	
	public NSArray perlist;
    public EduPeriod perItem;
    public NSKeyValueCoding item;
	public int perIdx;
	
	public NSMutableDictionary cyclesForPerlists;
	protected NSMutableDictionary marksAgregate;
	public NSMutableArray comments;
		
	protected NSArray perTypeUsage;
	protected NSArray eduPeriods;
	
	public void initialiseReporter(NSKeyValueCoding reporter, EOEditingContext ec) {
		
		perTypeUsage = (NSArray)reporter.valueForKey("perTypeUsage");
		if(perTypeUsage == null) {
			perTypeUsage = EOUtilities.objectsWithQualifierFormat(ec,"PeriodTypeUsage", "(eduYear = %d OR eduYear = 0)",new NSArray(eduYear));

			//pertypes = PeriodType.allPeriodTypes(ec,eduYear);
			reporter.takeValueForKey(perTypeUsage,"perTypeUsage");
		} else {
			perTypeUsage = EOUtilities.localInstancesOfObjects(ec,perTypeUsage);
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
		Object tmp = valueForBinding("period");
		if(tmp instanceof EduPeriod) {
			eduYear = ((EduPeriod)tmp).eduYear();
		} 
		if(eduYear == null) {
			tmp = valueForBinding("to");
			if(tmp == null)
				tmp = valueForBinding("since");
			if(tmp instanceof NSTimestamp)
				eduYear = net.rujel.base.MyUtility.eduYearForDate((NSTimestamp)tmp);
		}
		if(eduYear == null) {
			eduYear = (Integer)session().valueForKey("eduYear");
		}
		NSMutableArray courses = ((NSArray)valueForBinding("courses")).mutableClone();


		EOEditingContext ec = student.editingContext();
		NSKeyValueCoding reporter = (NSKeyValueCoding)valueForBinding("reporter");
		initialiseReporter(reporter, ec);
		reporter.takeValueForKey(eduYear, "eduYear");

		EOQualifier qual = new EOKeyValueQualifier("groupList",EOQualifier.QualifierOperatorContains,student);
		EOQualifier.filterArrayWithQualifier(courses,qual);
		EOSortOrdering.sortArrayUsingKeyOrderArray(courses, 
				new NSArray<EOSortOrdering>(new EOSortOrdering("cycle",EOSortOrdering.CompareAscending)));
		
		NSMutableArray params = new NSMutableArray(student);
		params.addObject(eduYear);
		
		NSArray allMarks = EOUtilities.objectsWithQualifierFormat(ec,"ItogMark","student = %@ AND eduPeriod.eduYear = %@",params);

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
					NSMutableDictionary[] agregate = (NSMutableDictionary[])marksAgregate.objectForKey(currMark.cycle());
					if(agregate == null) {
						agregate = new NSMutableDictionary[curPerList.count()];
						marksAgregate.setObjectForKey(agregate,currMark.cycle());
					}
					agregate[perIdx] = new NSMutableDictionary(currMark.mark(),"mark");
					break getgroup;
				}
			}
		} //agregate Marks

		//prepare comments
		reporter.takeValueForKey(student,"student");
		reporter.takeValueForKey(courses, "courses");
		session().setObjectForKey(reporter, "itogReporter");
		comments = (NSMutableArray)session().valueForKeyPath("modules.extItog");
		session().removeObjectForKey("itogReporter");
		if(comments != null && comments.count() > 0) {
			NSArray sorter = new NSArray(new EOSortOrdering[] {
					new EOSortOrdering("eduPeriod",EOSortOrdering.CompareAscending),
					new EOSortOrdering("cycle",EOSortOrdering.CompareAscending)
			});
			EOSortOrdering.sortArrayUsingKeyOrderArray(comments, sorter);
			for (int i = 0; i < comments.count(); i++) {
				NSDictionary comment = (NSDictionary)comments.objectAtIndex(i);
				String alias = '*' + Integer.toString(1 + i);
					//Character.toString((char)('a' + i));
				Object cycle = comment.valueForKey("cycle");
				if(cycle == null) {
					NSMutableDictionary perAlias = (NSMutableDictionary)
							marksAgregate.valueForKey("none");
					if(perAlias == null) {
						perAlias = new NSMutableDictionary();
						marksAgregate.setObjectForKey(perAlias, "none");
					}
					perAlias.setObjectForKey(alias, comment.valueForKey("eduPeriod"));
					continue;
				}
				comment.takeValueForKey(alias, "alias");
				Enumeration pen = cyclesForPerlists.keyEnumerator();
				while (pen.hasMoreElements()) {
					NSArray curPerList = (NSArray)pen.nextElement();
					perIdx = curPerList.indexOfIdenticalObject(comment.valueForKey("eduPeriod"));
					if(perIdx == NSArray.NotFound)
						continue;
					NSArray cycles = (NSArray)cyclesForPerlists.objectForKey(curPerList);
					if(cycles.containsObject(cycle)) {
						NSMutableDictionary[] agregate = (NSMutableDictionary[])
						marksAgregate.objectForKey(cycle);
						if(agregate == null) {
							agregate = new NSMutableDictionary[curPerList.count()];
							marksAgregate.setObjectForKey(agregate,cycle);
						}
						if(agregate[perIdx] == null)
							agregate[perIdx] = new NSMutableDictionary(alias,"alias");
						else
							agregate[perIdx].takeValueForKey(alias,"alias");
						//break;
					}
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
	
	public String mark() {
		if(item == null || perItem == null)
			return null;
		NSMutableDictionary[] agregate = (NSMutableDictionary[])marksAgregate.objectForKey(item);
		NSDictionary agr = (agregate==null)?null:agregate[perIdx];
		if(agr == null)
			return null;
		String mark = (String)agr.valueForKey("mark");
		String alias = (String)agr.valueForKey("alias");
		if(alias == null)
			return mark;
		StringBuffer buf = new StringBuffer(15);
		buf.append("&nbsp;&nbsp;");
		if(mark != null) {
			//buf.append("<strong>").append(mark).append("</strong>");
			buf.append(mark);
		}
		buf.append("<sup class=\"sup\">").append(alias).append("</sup>");
		return buf.toString();
	}
	
	public String commentSubj() {
		Object cycle = valueForKeyPath("item.cycle");
		if (cycle instanceof EduCycle) {
			EduCycle c = (EduCycle) cycle;
			return c.subject();
		}
		if (cycle == null || "none".equals(cycle))
			return (String)application().valueForKeyPath(
					"extStrings.RujelEduResults_EduResults.allCycles");
		return cycle.toString();
	}
	
	public boolean commentPeriod() {
		if(perItem == item.valueForKey("eduPeriod"))
			return false;
		perItem = (EduPeriod)item.valueForKey("eduPeriod");
		return true;
	}
	
	public String periodTitle() {
		/*NSDictionary perAlias = (NSDictionary)marksAgregate.objectForKey("none");
		String alias = (perAlias == null)?null:(String)perAlias.objectForKey(perItem); */
		StringBuffer buf = new StringBuffer();
		if(perItem.countInYear() > 1) {
			/*if(alias != null)
				buf.append("<span style=\"white-space:nowrap;\">");*/
			buf.append(Various.makeRoman(perItem.num().intValue()));
			/*if(alias != null)
				buf.append(" <sup class=\"sup\">(").append(alias).append("</sup>").append("</span>");*/
			buf.append("<br>\n<small>");
			buf.append(perItem.periodType().title());
			buf.append("</small>");
		} else {
			buf.append(perItem.periodType().title());
			/*if(alias != null)
				buf.append("<sup>").append(alias).append("</sup>");*/
		}
		return buf.toString();
	}

	public String liStyle() {
		Object cycle = valueForKeyPath("item.cycle");
		if (cycle == null || "none".equals(cycle))
			return "list-style-type:none;line-height:150%";
		return null;
	}
	
	public String num() {
		return Integer.toString(perIdx + 1);
	}
}
