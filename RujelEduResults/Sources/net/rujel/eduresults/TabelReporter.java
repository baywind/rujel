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
import net.rujel.base.BaseCourse;
import net.rujel.base.MyUtility;
import net.rujel.base.SettingsBase;
import net.rujel.interfaces.*;
import com.webobjects.foundation.*;
import com.webobjects.foundation.NSComparator.ComparisonException;
import com.webobjects.appserver.*;
import com.webobjects.eocontrol.*;
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
		cycles = null;
		marksAgregate = null;
		eduYear = null;
		comments = null;
		years = null;
		reportCourses = null;
	}
	public Student student;
	public Number eduYear;
	
	public NSMutableArray perlist;
    public ItogContainer perItem;
    public NSKeyValueCoding item;
	
	public NSMutableArray cycles;
	public Object cycleItem;
	protected NSMutableDictionary forCycle;
	
	protected NSMutableDictionary marksAgregate;
	public NSMutableArray comments;
	public NSMutableArray years;
	protected Object reportCourses;
	
	public boolean courseIsActive(NSKeyValueCodingAdditions course) {
		if(reportCourses == null) {
			EOEditingContext ec = null;
			if(course instanceof EOEnterpriseObject)  {
				ec = ((EOEnterpriseObject)course).editingContext();
			} else if(course instanceof NSDictionary) {
				Enumeration enu = ((NSDictionary)course).objectEnumerator();
				while (enu.hasMoreElements()) {
					Object obj = enu.nextElement();
					if(obj instanceof EOEnterpriseObject) {
						ec = ((EOEnterpriseObject)obj).editingContext();
						break;
					}
				}
			}
			if(ec == null)
				throw new IllegalArgumentException(
						"Course should be a EO itself or dictionary containig at leasr one EO");
			reportCourses = SettingsBase.baseForKey("reportCourses", ec, false);
			if(reportCourses == null)
				reportCourses = NullValue;
		}
		if(reportCourses == NullValue)
			return true;
		Integer number = ((SettingsBase)reportCourses).forCourse(course).numericValue();
		return Various.boolForObject(number);
	}
	
	public void setCourses(NSArray list) {
		cycles = new NSMutableArray();
		if(list == null || list.count() == 0) {
	        if(context().hasSession())
	        	eduYear = (Integer)session().valueForKey("eduYear");
	        else
	        	eduYear = MyUtility.eduYearForDate(null);
			return;
		}
			EduCourse course = (EduCourse)list.objectAtIndex(0);
			eduYear = course.eduYear();
			list = BaseCourse.coursesForStudent(list, student);
//			if(list.count() > 0) {
				NSMutableSet itogs = new NSMutableSet();
				Enumeration enu = list.objectEnumerator();
				while (enu.hasMoreElements()) {
					course = (EduCourse) enu.nextElement();
					if(!courseIsActive(course))
						continue;
					itogs.addObjectsFromArray(ItogContainer.itogsForCourse(course));
					cycles.addObject(course.cycle());
				}
				perlist = itogs.allObjects().mutableClone();
//			}
	}
	
	public void appendToResponse(WOResponse aResponse,WOContext aContext) {
		student = (Student)valueForBinding("student");
		EOEditingContext ec = student.editingContext();
		
		item = (NSKeyValueCoding)valueForBinding("reporter");
		if(Various.boolForObject(item.valueForKey("noYear"))) {
			eduYear = null;
			cycles = new NSMutableArray();
		} else {
			setCourses((NSArray)valueForBinding("courses"));
		}

		EOQualifier qual = new EOKeyValueQualifier(ItogMark.STUDENT_KEY,EOQualifier.QualifierOperatorEqual,student);
		if(eduYear != null) {
			EOQualifier[] quals = new EOQualifier[2];
			quals[0] = qual;
			quals[1] = new EOKeyValueQualifier("container.eduYear",EOQualifier.QualifierOperatorEqual,eduYear);
			qual = new EOAndQualifier(new NSArray(quals));
		}
		EOFetchSpecification fs = new EOFetchSpecification(ItogMark.ENTITY_NAME,qual,null);
		NSArray allMarks = ec.objectsWithFetchSpecification(fs);
//		if(allMarks == null || allMarks.count() == 0)
//			return;
		marksAgregate = new NSMutableDictionary();
		if(perlist == null)
			perlist = new NSMutableArray();
		if(allMarks != null && allMarks.count() > 0) {
		Enumeration enu = allMarks.objectEnumerator();
		while (enu.hasMoreElements()) { //agregate Marks
			ItogMark currMark = (ItogMark)enu.nextElement();
			EduCycle cycle = currMark.cycle();
			perItem = currMark.container();
			if(perItem.eduYear() == null)
				continue;
			if(reportCourses != NullValue) {
				NSDictionary course = SettingsBase.courseDict(cycle, perItem.eduYear());
				if(!courseIsActive(course))
					continue;
			}
			forCycle = (NSMutableDictionary)marksAgregate.objectForKey(cycle.subject());
			if(forCycle == null) {
				forCycle = new NSMutableDictionary();
				marksAgregate.setObjectForKey(forCycle, cycle.subject());
			}
			item = new NSMutableDictionary(currMark.mark(),"mark");
			forCycle.setObjectForKey(item, perItem);
			if(!cycles.containsObject(cycle))
				cycles.addObject(cycle);
			if(!perlist.containsObject(perItem))
				perlist.addObject(perItem);
		} //agregate Marks
		}
		fs.setEntityName("ItogComment");
		allMarks = ec.objectsWithFetchSpecification(fs);

		if(allMarks != null && allMarks.count() > 0) { //prepare comments
			NSArray sorter = new NSArray(new EOSortOrdering[] {
					new EOSortOrdering(ItogMark.CONTAINER_KEY,EOSortOrdering.CompareAscending),
					new EOSortOrdering(ItogMark.CYCLE_KEY,EOSortOrdering.CompareAscending)
			});
			allMarks = EOSortOrdering.sortedArrayUsingKeyOrderArray(allMarks, sorter);
			comments = new NSMutableArray();
			Enumeration enu = allMarks.objectEnumerator();
			int i = 1;
			while (enu.hasMoreElements()) {
				EOEnterpriseObject comment = (EOEnterpriseObject) enu.nextElement();
				perItem = (ItogContainer)comment.valueForKey(ItogMark.CONTAINER_KEY);
				if(perItem.eduYear() == null)
					continue;
				EduCycle cycle = (EduCycle)comment.valueForKey(ItogMark.CYCLE_KEY);
				if(reportCourses != NullValue) {
					NSDictionary course = SettingsBase.courseDict(cycle, perItem.eduYear());
					if(!courseIsActive(course))
						continue;
				}
				item = ItogMark.commentsDict(comment);
				if(eduYear == null && item.valueForKey(ItogMark.MANUAL)==null)
					continue;
				String alias = '*' + Integer.toString(i);
				i++;
				item.takeValueForKey(alias, "alias");
				item.takeValueForKey(cycle.subject(), "subject");
				item.takeValueForKey(perItem, ItogMark.CONTAINER_KEY);
				comments.addObject(item);
				forCycle = (NSMutableDictionary)marksAgregate.objectForKey(cycle.subject());
				if(forCycle == null) {
					forCycle = new NSMutableDictionary();
					marksAgregate.setObjectForKey(forCycle, cycle.subject());
				}
				item = (NSMutableDictionary)forCycle.objectForKey(perItem);
				if(item == null) {
					item = new NSMutableDictionary(alias, "alias");
					forCycle.setObjectForKey(item, perItem);
				} else {
					item.takeValueForKey(alias, "alias");
				}
				if(!cycles.containsObject(cycle))
					cycles.addObject(cycle);
				if(!perlist.containsObject(perItem))
					perlist.addObject(perItem);
			}
		} //prepare comments
		try {
			cycles.sortUsingComparator(new AdaptingComparator());
			perlist.sortUsingComparator(new AdaptingComparator(ItogContainer.class));
//			EOSortOrdering.sortArrayUsingKeyOrderArray(perlist, ItogContainer.sorter);
		} catch (ComparisonException e) {
			;
		}
		// convert cycles list to subjects list
		Enumeration enu = cycles.objectEnumerator();
		cycles = new NSMutableArray();
		while (enu.hasMoreElements()) {
			EduCycle cycle = (EduCycle) enu.nextElement();
			if(!cycles.containsObject(cycle.subject()))
				cycles.addObject(cycle.subject());
		}
		if(eduYear == null && perlist.count() > 0) { // prepare years head
			int year = 0;
			years = new NSMutableArray();
			int colspan = 0;
			enu = perlist.objectEnumerator();
			item = null;
			while (enu.hasMoreElements()) {
				ItogContainer itog = (ItogContainer) enu.nextElement();
				if(itog.eduYear() == null) continue;
				if(itog.eduYear().intValue() != year) {
					year = itog.eduYear().intValue();
					if(item != null) {
						item.takeValueForKey(new Integer(colspan), "colspan");
						years.addObject(item);
					}
					colspan = 0;
					item = new NSMutableDictionary(MyUtility.presentEduYear(year),"text");
				}
				colspan++;
			}
			item.takeValueForKey(new Integer(colspan), "colspan");
			years.addObject(item);
		}
		forCycle = null;
		super.appendToResponse(aResponse,aContext);
	}
	
	public void setCycleItem(Object cItem) {
		cycleItem = cItem;
		forCycle = (NSMutableDictionary)((marksAgregate==null)?null:
						marksAgregate.objectForKey(cycleItem)); 
		if(years != null)
			eduYear = null;
	}
	
	public void setPerItem(ItogContainer pItem) {
		perItem = pItem;
		item = (NSKeyValueCoding)((forCycle==null)?null:
			forCycle.objectForKey(perItem));
	}
	
	public String mark() {
		if(item == null)
			return null;
		String mark = (String)item.valueForKey("mark");
		String alias = (String)item.valueForKey("alias");
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
	
	public boolean commentPeriod() {
		if(perItem == item.valueForKey(ItogMark.CONTAINER_KEY))
			return false;
		perItem = (ItogContainer)item.valueForKey(ItogMark.CONTAINER_KEY);
		return true;
	}
	
	public String periodTitle() {
		/*NSDictionary perAlias = (NSDictionary)marksAgregate.objectForKey("none");
		String alias = (perAlias == null)?null:(String)perAlias.objectForKey(perItem); */
		StringBuffer buf = new StringBuffer();
		if(perItem.itogType().inYearCount().intValue() > 1) {
			/*if(alias != null)
				buf.append("<span style=\"white-space:nowrap;\">");*/
			buf.append(Various.makeRoman(perItem.num().intValue()));
			/*if(alias != null)
				buf.append(" <sup class=\"sup\">(").append(alias).append("</sup>").append("</span>");*/
			buf.append("<br />\n<small>");
			buf.append(perItem.itogType().title());
			buf.append("</small>");
		} else {
			buf.append(perItem.itogType().title());
			/*if(alias != null)
				buf.append("<sup>").append(alias).append("</sup>");*/
		}
		return buf.toString();
	}

	public String commentText() {
		StringBuilder buf = new StringBuilder();
		String comment = (String)item.valueForKey(ItogMark.MANUAL);
		if(comment != null) {
			buf.append("<div style = \"margin-bottom:3px;\"><strong>");
			buf.append(application().valueForKeyPath(
					"strings.Reusables_Strings.dataTypes.comment"));
			buf.append(":</strong> ").append(comment).append("</div>\n");
		}
		Enumeration enu = ((NSDictionary)item).keyEnumerator();
		while (enu.hasMoreElements()) {
			String key = (String) enu.nextElement();
			if(key.equals("alias") || key.equals(ItogMark.MANUAL) ||
					key.equals(ItogMark.CONTAINER_KEY) || key.equals("subject"))
			continue;
				buf.append("<div style = \"margin-bottom:3px;\">");
			comment = (String)item.valueForKey(key);
			buf.append("<strong>").append(key);
				buf.append(":</strong> ").append(comment).append("</div>\n");
		}
		return buf.toString();
	}
	
	public String cellStyle() {
		if(perItem == null || perItem.eduYear().equals(eduYear))
			return (forCycle == null)?"width:2em;":null;
		eduYear = perItem.eduYear();
		if(forCycle == null)
			return "width:2em;border-left:double 3px;";
		return "border-left:double 3px;";
	}
}
