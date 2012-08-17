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
import net.rujel.base.Setting;
import net.rujel.base.SettingsBase;

import com.webobjects.foundation.*;
import com.webobjects.appserver.*;
import com.webobjects.eoaccess.EOUtilities;
import com.webobjects.eocontrol.*;

import java.text.Format;
import java.util.Enumeration;
import java.util.logging.Logger;
import java.math.BigDecimal;

public class StudentMarks extends WOComponent {	
	public static final String workIntegral = SettingsReader.stringForKeyPath(
			"criterial.workIntegral","%");
	public static final NSArray noCriteria = new NSArray(new NSDictionary("#","title"));

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
		NSDictionary options = (NSDictionary)settings.valueForKeyPath("reporter.settings.marks");	
		if(options == null || !Various.boolForObject(options.valueForKey("active")))
			return null;
		
		Student student = (Student)settings.valueForKey("student");
		EOEditingContext ec = student.editingContext();
		
		NSMutableDictionary result = new NSMutableDictionary("marks","id");
		result.takeValueForKey("StudentMarks", "component");
		result.takeValueForKey(options.valueForKey("sort"), "sort");
		NSTimestamp since = (NSTimestamp)settings.valueForKey("since");
		NSTimestamp to = (NSTimestamp)settings.valueForKey("to");
		Period period = (Period)settings.valueForKey("period");
		if(!(period instanceof EOEnterpriseObject))
			period = null;
		Object workType = options.valueForKey("workType");
		if(workType instanceof String) {
			if(workType.equals("<Null>"))
				workType = null;
			else try {
				workType = Various.parseEO((String)workType, ec);
			} catch (Exception e) {
				Logger.getLogger("rujel.criterial").log(WOLogLevel.WARNING,
						"Error parsing workType in reportSettings",e);
				workType = null;
				settings.takeValueForKey(null, "workType");
			}
		}
		NSArray courses = (NSArray)settings.valueForKey("courses");
		if(courses == null)
			return null;
		NSMutableArray[] allWorks = new NSMutableArray[courses.count()];
		SettingsBase reportCourses = (SettingsBase)settings.valueForKey("reportCourses");

		//Enumeration enu = courses.objectEnumerator();
		NSMutableArray args = new NSMutableArray();
		if(since != null)
			args.addObject(new EOKeyValueQualifier(Work.DATE_KEY,
					EOQualifier.QualifierOperatorGreaterThanOrEqualTo,since));
		if(to != null)
			args.addObject(new EOKeyValueQualifier(Work.DATE_KEY,
					EOQualifier.QualifierOperatorLessThanOrEqualTo,to));
		Number lvl = (Number)options.valueForKey("level");
		int level = (lvl == null)?1:lvl.intValue();
//		boolean all = Various.boolForObject(options.valueForKey("all"));
//		boolean marked = Various.boolForObject(options.valueForKey("marked"));
		for(int i = 0; i < courses.count(); i++) { //get works for courses;
			EduCourse c = (EduCourse)courses.objectAtIndex(i);
			if(reportCourses != null) {
				Integer num = reportCourses.forCourse(c).numericValue();
				if(!Various.boolForObject(num))
					continue;
			}
			NSMutableArray quals = args.mutableClone();//new NSMutableArray(qual);
			quals.add(new EOKeyValueQualifier("course",EOQualifier.QualifierOperatorEqual,c));
			if(level < 2) {
				quals.addObject(EOQualifier.qualifierWithQualifierFormat(
						"flags >= 24 OR (flags >= 8 and flags < 16) ", null));
				quals.addObject(new EOKeyValueQualifier(Work.WEIGHT_KEY,
						EOQualifier.QualifierOperatorGreaterThan,BigDecimal.ZERO));
			}
			if(workType != null) {
				quals.addObject(new EOKeyValueQualifier(Work.WORK_TYPE_KEY,
						EOQualifier.QualifierOperatorEqual,workType));
			}
			EOFetchSpecification fs = new EOFetchSpecification(Work.ENTITY_NAME,
					new EOAndQualifier(quals),EduLesson.sorter);
			fs.setRefreshesRefetchedObjects(true);
			NSArray works = ec.objectsWithFetchSpecification(fs);
			if(works != null && works.count() > 0) {
				if(level < 5) {
					allWorks[i] = new NSMutableArray(works.count());
					Enumeration enu = works.objectEnumerator();
					while (enu.hasMoreElements()) {
						Work w = (Work) enu.nextElement();
						if(level < 2) {
							int flags = w.flags().intValue();
							if(((flags & 8) == 0 || !w.hasWeight()) 
									&& w.forPersonLink(student) == null)
								continue;
						} else {
							NSArray mask = w.criterMask();
							if(mask == null || mask.count() == 0)
								continue;
						}
						allWorks[i].addObject(w);
					}
				} else {
					allWorks[i] = works.mutableClone();
				}
			} // add result to allWorks
		}
		
		NSMutableArray extraWorks = new NSMutableArray();
		if(level >= 1) { //add works with marks
			boolean dateSet = Various.boolForObject(options.valueForKey("dateSet"));
			args.removeAllObjects();
			args.addObject(new EOKeyValueQualifier("student",
					EOQualifier.QualifierOperatorEqual,student));
			if(workType != null)
				args.addObject(new EOKeyValueQualifier("work.workType",
					EOQualifier.QualifierOperatorEqual,workType));
			if(period != null) {
				args.addObject(new EOKeyValueQualifier("work.date",
						EOQualifier.QualifierOperatorGreaterThanOrEqualTo,period.begin()));
				args.addObject(new EOKeyValueQualifier("work.date",
						EOQualifier.QualifierOperatorLessThanOrEqualTo,period.end()));
			} else if(since != null || to != null) {
				EOQualifier[] or = new EOQualifier[2];
				if(since != null) {
					if(dateSet) {
						or[0] = new EOKeyValueQualifier("work.date",
								EOQualifier.QualifierOperatorGreaterThanOrEqualTo,since);
						or[1] = new EOKeyValueQualifier(Mark.DATE_SET_KEY,
								EOQualifier.QualifierOperatorGreaterThanOrEqualTo,since);
						args.addObject(new EOOrQualifier(new NSArray(or)));
					} else {
						args.addObject(new EOKeyValueQualifier("work.date",
								EOQualifier.QualifierOperatorGreaterThanOrEqualTo,since));
					}
				}
				if(to != null) {
					if(dateSet) {
						or[0] = new EOKeyValueQualifier("work.date",
								EOQualifier.QualifierOperatorLessThanOrEqualTo,to);
						or[1] = new EOKeyValueQualifier(Mark.DATE_SET_KEY,
								EOQualifier.QualifierOperatorLessThanOrEqualTo,to);
						args.addObject(new EOOrQualifier(new NSArray(or)));
					} else {
						args.addObject(new EOKeyValueQualifier("work.date",
								EOQualifier.QualifierOperatorLessThanOrEqualTo,to));
					}
				}
			}
			EOFetchSpecification fs = new EOFetchSpecification(Mark.ENTITY_NAME,
					new EOAndQualifier(args),null);
			fs.setRefreshesRefetchedObjects(true);
			NSArray allMarks = ec.objectsWithFetchSpecification(fs);
			//insert WorkNotes
			fs.setEntityName("WorkNote");
			if(period == null && dateSet) {
				for (int i = args.count() -1; i >= 0; i--) {
					if(args.objectAtIndex(i) instanceof EOOrQualifier) {
						args.removeObjectAtIndex(i);
					}
				}
				if(since != null)
					args.addObject(new EOKeyValueQualifier("work.date",
							EOQualifier.QualifierOperatorGreaterThanOrEqualTo,since));
				if(to != null)
					args.addObject(new EOKeyValueQualifier("work.date",
							EOQualifier.QualifierOperatorLessThanOrEqualTo,to));
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
					Logger.getLogger("rujel.criterial").log(WOLogLevel.INFO,
							"Dangling mark found",m);
					continue;
				}
				if(reportCourses != null) {
					Integer num = reportCourses.forCourse(course).numericValue();
					if(!Various.boolForObject(num))
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
		level = 0;
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
					boolean hideMax = Various.boolForObject(options.valueForKey("hideMax"));
					courseItem.setObjectForKey(formatWorks(works,criteria, hideMax),"works");
				}
				result.setObjectForKey(courseItem, c);
				level++;
			}
		}
		if(level == 0)
			return null;
		else
			settings.takeValueForKey(null, "needData");
		return result;
	}
	

	public static NSMutableDictionary formatCourse(EduCourse course) {
		NSMutableDictionary result = new NSMutableDictionary();
		EOEditingContext ec = course.editingContext();
		{
			Setting integral = SettingsBase.settingForCourse("presenters.workIntegral", course, ec);
			//SettingsReader.stringForKeyPath("edu.presenters.workIntegral","~");
			String title = null;
			if(integral != null) {
				Integer pKey = integral.numericValue();
				if(pKey != null) {
					try {
						BorderSet bset = (BorderSet)EOUtilities.objectWithPrimaryKeyValue(
								ec, BorderSet.ENTITY_NAME, pKey);
						title = bset.title();
					} catch (Exception e) {
						;
					}
				}
				if(title == null)
					title = integral.textValue();
			}
			if(title == null)
				title = "%";
			result.takeValueForKey(title, "integral");
		}
		NSArray criteria = CriteriaSet.criteriaForCourse(course);
/*		NSMutableArray critDicts = new NSMutableArray();
		if(criteria != null && criteria.count() > 0) {
			Enumeration en = criteria.objectEnumerator();
			while (en.hasMoreElements()) {
				NSKeyValueCoding criter = (NSKeyValueCoding)en.nextElement();
				NSMutableDictionary critDict = new NSMutableDictionary(
						criter.valueForKey("title"),"title");
				critDict.takeValueForKey(criter.valueForKey("comment"), "comment");
				critDicts.addObject(critDict);
			}
		} else {
			int maxCriter = CriteriaSet.maxCriterionForCourse(course);
			char first = 'A';
			for (int i = 0; i < maxCriter; i++) {
				String title = Character.toString((char)(first + i));
				NSDictionary critDict = new NSDictionary(title,"title");
				critDicts.addObject(critDict);
			}
		}*/
		if(criteria == null || criteria.count() == 0)
			criteria = noCriteria;
		result.setObjectForKey(criteria,"criteria");
		
		return result;
	}
	
	public static NSArray formatWorks(NSArray works,NSArray criteria, boolean hideMax) {
		if(works == null || works.count() ==0) return null;
		NSArray blanc = null;
		final String empty = "";
		if(criteria != null && criteria.count() > 0) {
			Object[] arr = new Object[criteria.count()];
			for (int i = 0; i < arr.length; i++) {
				arr[i] = empty;
			}
			blanc = new NSArray(arr);
		}
		NSMutableArray maxValues = null;
		Object bcMax = null;
		NSMutableArray result = new NSMutableArray();
		Enumeration enu = EOSortOrdering.sortedArrayUsingKeyOrderArray(
				works,Work.sorter).objectEnumerator();
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
						Integer idx = (Integer)mask.valueForKey("criterion");
						if(idx.intValue() < 0 || idx.intValue() > critMask.count())
							continue;
						Object max = currWork.maxForCriter(idx);
						if(max == null)
							continue;
						if(idx.intValue() == 0) {
							if(bcMax == null || !bcMax.equals(max)) {
								NSMutableDictionary maxRow = new NSMutableDictionary("max","kind");
								bcMax = max;
								maxRow.setObjectForKey(max,"title");
								result.addObject(maxRow);
							}
							status = 0;
							maxValues = null;
							break;
						} else {
							bcMax = null;
						}
						critMask.replaceObjectAtIndex(max,idx.intValue() -1);
						if(status != 2) {
							Object titleMax = maxValues.objectAtIndex(idx.intValue() -1);
							if(titleMax != empty) {
								if(!max.equals(titleMax)){
									status = 2;
								}
							} else {
								status = 1;
							}
						}
					} // criteria enumeration
					if(status == 2 && !hideMax) {
						maxValues = critMask;
						NSMutableDictionary maxRow = new NSMutableDictionary("max","kind");
						maxRow.setObjectForKey(maxValues,"values");
						result.addObject(maxRow);
					} else if(status == 1) {
						for (int i = 0; i < critMask.count(); i++) {
							Object value = critMask.objectAtIndex(i);
							if(value != empty)
								maxValues.replaceObjectAtIndex(value,i);
						}
					}
				}
			} // prepare criter list
			curr.takeValueForKey(dateFormat.format(currWork.announce()),"announce");
			curr.takeValueForKey(dateFormat.format(currWork.date()),"date");
			curr.takeValueForKey(currWork.valueForKeyPath("workType.typeName"), "type");
			String theme  = currWork.taskUrl();
			if(theme != null) {
				if(theme.charAt(0) == '/') {
					String host = (String)WOApplication.application().valueForKey(
							"serverURL");
					if(host != null)
						theme = host + theme;
				}
				curr.takeValueForKey(theme, "url");
			}
			theme = currWork.theme();
			if(theme == null) {
				theme = "- - -";
			} else {
				theme = WOMessage.stringByEscapingHTMLString(theme);
			}
			BigDecimal weight = currWork.trimmedWeight();
			curr.setObjectForKey(weight,"weight");
//			if(BigDecimal.ZERO.compareTo(currWork.weight()) == 0) {
			if(weight.equals(BigDecimal.ZERO)) {
				curr.setObjectForKey("noWeight","kind");
				curr.setObjectForKey("color:#333333;" + currWork.font(),"style");
				curr.takeValueForKey(theme,"theme");
			} else {
				curr.setObjectForKey("withWeight","kind");
				curr.setObjectForKey(currWork.font(),"style");
				StringBuilder buf = new StringBuilder("<strong>");
				buf.append(theme).append("</strong>");
//				if(BigDecimal.ONE.compareTo(currWork.weight()) != 0) {
				curr.takeValueForKey(buf.toString(),"theme");
				if(!weight.equals(BigDecimal.ONE)) {
					buf.delete(1, buf.length());
					buf.append(WOApplication.application().valueForKeyPath(
							"strings.RujelCriterial_Strings.weight"));
					buf.append(':').append(' ').append(weight).append('>');
					curr.takeValueForKey(buf.toString(),"weightStr");
				}
			}
			result.addObject(curr);
		}
		return result;
	}
	
    public StudentMarks(WOContext context) {
        super(context);
    }
	
	public void appendToResponse(WOResponse aResponse, WOContext aContext) {
		WOSession ses = aContext.session();
		NSDictionary settings = (NSDictionary)valueForBinding("settings");
		//ses.objectForKey("reportSettingsForStudent");
		if(!Various.boolForObject(settings.valueForKeyPath("marks.short"))) {
			super.appendToResponse(aResponse, aContext);
			return;
		}
		NSKeyValueCoding item = (NSKeyValueCoding)valueForBinding("value");
		NSArray works = (NSArray)item.valueForKey("works");
		if(works == null || works.count() == 0)
			return;
		Student student = (Student)valueForBinding("student");
		boolean hideMax = Various.boolForObject(settings.valueForKeyPath("marks.hideMax"));
		aResponse.appendContentString("<strong style = \"font-size:110%;\">");
		aResponse.appendContentString((String)ses.valueForKeyPath(
				"strings.RujelCriterial_Strings.works"));
		aResponse.appendContentString(":</strong> ");
		Enumeration enu = works.objectEnumerator();
		while (enu.hasMoreElements()) {
			workItem = (NSKeyValueCoding) enu.nextElement();
			Work work = (Work)workItem.valueForKey("work");
			if(work == null)
				continue;
			BigDecimal weight = (BigDecimal)workItem.valueForKey("weight");
			aResponse.appendContentString("<span");
			aResponse.appendContentString(" style = \"");
			aResponse.appendContentString(work.font());
			String tmp = (String)workItem.valueForKey("url");
			if(tmp != null)
				aResponse.appendContentString("cursor:pointer;color:blue;");
			if(weight.equals(BigDecimal.ZERO))
				aResponse.appendContentString("color:#666666;");
			if(tmp != null) {
				aResponse.appendContentString("\" onclick = \"window.open('");
				aResponse.appendContentString(tmp);
				aResponse.appendContentString("','_blank');");
			}
 			tmp = (String)workItem.valueForKey("kind");
			if(tmp != null) {
				aResponse.appendContentString("\" class = \"");
				aResponse.appendContentString(tmp);
			}
			aResponse.appendContentString("\" title = \"");
			tmp = (String)workItem.valueForKey("date");
			aResponse.appendContentHTMLAttributeValue(tmp);
			aResponse.appendContentHTMLAttributeValue(": ");
			aResponse.appendContentHTMLAttributeValue(work.theme());
			tmp = (String)workItem.valueForKey("weightStr");
			if(tmp != null) {
				aResponse.appendContentCharacter(' ');
				aResponse.appendContentHTMLAttributeValue(tmp);
			}
			aResponse.appendContentCharacter(' ');
			aResponse.appendContentCharacter('(');
			tmp = (String)workItem.valueForKey("type");
			aResponse.appendContentHTMLAttributeValue(tmp);
			aResponse.appendContentString(")\">");
			NSArray criteria = work.criterMask();
			if(criteria == null)
				criteria = NSArray.EmptyArray;
			boolean crits = (criteria.count() > 1);
			tmp = work.noteForStudent(student);
			if(crits) {
				criteria = EOSortOrdering.sortedArrayUsingKeyOrderArray(
						criteria, CriteriaSet.sorter);
			} else if (criteria.count() == 1) {
				EOEnterpriseObject crMask = (EOEnterpriseObject)criteria.objectAtIndex(0);
				Integer cr = (Integer)crMask.valueForKey("criterion");
				crits = (cr.intValue() != 0 || tmp != null);
			}
			if(crits)
				aResponse.appendContentCharacter('[');
			Enumeration cenu = criteria.objectEnumerator();
			while (cenu.hasMoreElements()) {
				EOEnterpriseObject crMask = (EOEnterpriseObject)cenu.nextElement();
				Integer cr = (Integer)crMask.valueForKey("criterion");
				if(cr.intValue() != 0) {
					aResponse.appendContentHTMLString(work.criterName(cr));
					aResponse.appendContentCharacter(':');
				}
				Mark mark = work.markForStudentAndCriterion(student, cr);
				if(mark == null || mark.value() == null)
					aResponse.appendContentString("&oslash;");
				else
					aResponse.appendContentString(mark.present());
				if(!hideMax) {
					aResponse.appendContentString(
							"<sub style = \"font-size:60%;color:#999999\">");
					aResponse.appendContentString(work.maxForCriter(cr).toString());
					aResponse.appendContentString("</sub>");
				}
				if(cenu.hasMoreElements())
					aResponse.appendContentString(",&nbsp;");
			}
			if(tmp != null) {
				if(crits)
					aResponse.appendContentCharacter(',');
				int idx = tmp.indexOf("http");
				if(idx < 0) {
					aResponse.appendContentCharacter(' ');
					aResponse.appendContentHTMLString(tmp);
				} else {
					if(idx > 0)
						aResponse.appendContentHTMLString(tmp.substring(0,idx));
					aResponse.appendContentString(" <a href = \"");
					int idx2 = tmp.indexOf(' ', idx);
					if(idx2 < 0) {
						aResponse.appendContentString(tmp.substring(idx));
					} else {
						aResponse.appendContentString(tmp.substring(idx,idx2));
					}
					aResponse.appendContentString("\" target = \"_blank\">&gt;&gt;</a>");
					if(idx2 > 0)
						aResponse.appendContentHTMLString(tmp.substring(idx2));
				}
			}
			if(crits)
				aResponse.appendContentCharacter(']');
			aResponse.appendContentString("</span>; ");
		}
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
}

