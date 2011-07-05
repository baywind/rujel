// ModuleInit.java

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

import java.math.BigDecimal;
import java.util.Enumeration;
import java.util.logging.Logger;

import net.rujel.base.BaseLesson;
import net.rujel.base.MyUtility;
import net.rujel.interfaces.EOInitialiser;
import net.rujel.interfaces.EduCourse;
import net.rujel.interfaces.EduLesson;
import net.rujel.interfaces.Student;
import net.rujel.reusables.PlistReader;
import net.rujel.reusables.Various;
import net.rujel.reusables.WOLogLevel;
import net.rujel.ui.DateAgregate;
import net.rujel.ui.MarksPresenter;

import com.webobjects.eoaccess.EOJoin;
import com.webobjects.eoaccess.EORelationship;
import com.webobjects.eoaccess.EOUtilities;
import com.webobjects.eocontrol.*;
import com.webobjects.foundation.*;
import com.webobjects.appserver.WOApplication;
import com.webobjects.appserver.WOContext;
import com.webobjects.appserver.WOMessage;
import com.webobjects.appserver.WOSession;

public class ModuleInit {

	public static Object init(Object obj, WOContext ctx) {
		if(obj == null || obj.equals("init")) {
			try {
				Object access = PlistReader.readPlist("access.plist", "RujelCriterial", null);
				WOApplication.application().takeValueForKey(access, "defaultAccess");
			} catch (NSKeyValueCoding.UnknownKeyException e) {
				// default access not supported
			}
			init();
//		} else if("init2".equals(obj)) {
//			Work.initTypes();
		} else if("presentTabs".equals(obj)) {
			NSDictionary worksTab = (NSDictionary)WOApplication.application().
				valueForKeyPath("strings.RujelCriterial_Strings.worksTab");
			return PlistReader.cloneDictionary(worksTab, true);
		} else if ("extendLesson".equals(obj)) {
			return extendLesson(ctx);
		} else if("reportForStudent".equals(obj)) {
			NSDictionary settings = (NSDictionary)ctx.session().objectForKey("reportForStudent");
			return StudentMarks.reportForStudent(settings);
		} else if("reportSettingsForStudent".equals(obj)) {
			NSDictionary reportSettings = (NSDictionary)WOApplication.application().
				valueForKeyPath("strings.RujelCriterial_Strings.reportSettings");
			return PlistReader.cloneDictionary(reportSettings, true);
		} else if ("lessonProperties".equals(obj)) {
			return lessonProperties(ctx);
		} else if ("dateAgregate".equals(obj)) {
			return dateAgregate(ctx);
		} else if ("diary".equals(obj)) {
			NSArray diaryTabs = (NSArray)WOApplication.application().
					valueForKeyPath("strings.RujelCriterial_Strings.diaryTabs");
			return PlistReader.cloneArray(diaryTabs, true);
		} else if ("courseComplete".equals(obj)) {
			return WOApplication.application().
					valueForKeyPath("strings.RujelCriterial_Strings.courseComplete");
		} else if("deleteCourse".equals(obj)) {
			return deleteCourse(ctx);
		} else if("completionLock".equals(obj)) {
			return new NSArray(new NSDictionary[] {
					new NSDictionary(new String[] {"WorkNote","work.course","student"},
							new String[] {"entity","coursePath","studentPath"}),
					new NSDictionary(new String[] {"Mark","work.course","student"},
							new String[] {"entity","coursePath","studentPath"}) });
		} else if("adminModules".equals(obj)) {
			return adminModules(ctx);
		} else if("deleteStudents".equals(obj)) {
			return deleteStudents(ctx);
		}
		return null;
	}
	
	public static void init() {
		EOInitialiser.initialiseRelationship("WorkNote","student",false,"studentID","Student").
						anyInverseRelationship().setPropagatesPrimaryKey(true);

		EORelationship relationship = EOInitialiser.initialiseRelationship("Work","course",false,
				"courseID","EduCourse");
		if(EduLesson.entityName.equals("Work")) {
			EORelationship backrel = relationship.destinationEntity().relationshipNamed("lessons");
			EOJoin join = (EOJoin)backrel.joins().objectAtIndex(0);
			backrel.removeJoin(join);
			join = (EOJoin)relationship.joins().objectAtIndex(0);
			join = new EOJoin(join.destinationAttribute(),join.sourceAttribute());
			backrel.addJoin(join);
		} else if (EduLesson.entityName.equals("BaseLesson")) {
			BaseLesson.setTaskDelegate(new HomeWorkDelegate());
		}

		Mark.init();
	}
	
	public static NSKeyValueCoding extendLesson(WOContext ctx) {
		NSMutableDictionary result = new NSMutableDictionary("05", "sort");
		result.takeValueForKey("WorksOnDate", "component");
		return result;
	}
	
	public static NSDictionary lessonProperties(WOContext ctx) {
		NSArray lessonsList = (NSArray)ctx.session().objectForKey("lessonsList");
		if(lessonsList == null || lessonsList.count() == 0)
			return null;
		NSMutableDictionary result = new NSMutableDictionary();
		EduLesson lesson = (EduLesson)lessonsList.objectAtIndex(0);
		EOEditingContext ec = lesson.editingContext();
		if(ec == null) {
			Logger.getLogger("rujel.criterial").log(WOLogLevel.WARNING,
					"Lesson not in EditingContext",ctx.session());
			return null;
		}
		EOQualifier qual = new EOKeyValueQualifier(
				"course",EOQualifier.QualifierOperatorEqual,lesson.course());
		NSMutableArray quals = new NSMutableArray();
		quals.addObject(qual);
		qual = new EOKeyValueQualifier(
				"weight",EOQualifier.QualifierOperatorGreaterThan,BigDecimal.ZERO);
		quals.addObject(qual);
		qual = new EOKeyValueQualifier("date",EOQualifier.QualifierOperatorLessThanOrEqualTo
				,lessonsList.valueForKey("@max.date"));
		quals.addObject(qual);
		qual = new EOKeyValueQualifier("date",EOQualifier.QualifierOperatorGreaterThanOrEqualTo
				,lessonsList.valueForKey("@min.date"));
		quals.addObject(qual);
		qual = new EOAndQualifier(quals);
		EOFetchSpecification fs = new EOFetchSpecification(Work.ENTITY_NAME,qual,null);
		NSArray works = ec.objectsWithFetchSpecification(fs);
		NSDictionary props = new NSDictionary("font-weight:bold;","style");
		Enumeration enu = lessonsList.objectEnumerator();
		while (enu.hasMoreElements()) {
			lesson = (EduLesson) enu.nextElement();
			qual = new EOKeyValueQualifier("date",EOQualifier.QualifierOperatorEqual,lesson.date());
			NSArray w = EOQualifier.filteredArrayWithQualifier(works, qual);
			if(w != null && w.count() > 0)
				result.setObjectForKey(props, lesson);
		}
		if(result.count() == 0)
			return null;
		return result;
	}

	public static Object deleteCourse(WOContext ctx) {
		EduCourse course = (EduCourse)ctx.session().objectForKey("deleteCourse");
		EOEditingContext ec = course.editingContext();
		NSArray list = EOUtilities.objectsMatchingKeyAndValue
					(ec, Work.ENTITY_NAME, "course", course);
		if(list == null || list.count() == 0)
			return null;
		String message = (String)WOApplication.application().valueForKeyPath(
				"strings.RujelCriterial_Strings.messages.courseHasWorks");
		ctx.session().takeValueForKey(message, "message");
		return message;
	}
	
	public static Object adminModules(WOContext ctx) {
		WOSession ses = ctx.session();
		NSDictionary setup = (NSDictionary)ses.valueForKeyPath(
			"strings.RujelCriterial_Strings.setup");
		NSMutableArray result = new NSMutableArray();
		Enumeration enu = setup.keyEnumerator();
		while (enu.hasMoreElements()) {
			String key = (String) enu.nextElement();
			if(Various.boolForObject(ses.valueForKeyPath("readAccess.read." + key)))
				result.addObject(setup.valueForKey(key));
		}
		return result;
	}
	
	public static Object deleteStudents(WOContext ctx) {
		NSArray students = (NSArray)ctx.session().objectForKey("deleteStudents");
		if(students == null || students.count() == 0)
			return null;
		EOQualifier qual = Various.getEOInQualifier("student", students);
		EOFetchSpecification fs = new EOFetchSpecification("Mark",qual,null);
		fs.setFetchLimit(1);
		EOEnterpriseObject student = (EOEnterpriseObject)students.objectAtIndex(0);
		NSArray found = student.editingContext().objectsWithFetchSpecification(fs);
		if(found != null && found.count() > 0) {
			return ctx.session().valueForKeyPath(
					"strings.RujelCriterial_Strings.messages.relatedMarksFound");
		}
		return null;
	}
	
	public static Object dateAgregate(WOContext ctx) {
		DateAgregate agr = (DateAgregate)ctx.session().objectForKey("dateAgregate");
		if (agr == null)
			return null;
		EOQualifier qual = new EOKeyValueQualifier("course",
				EOQualifier.QualifierOperatorEqual,agr.course());
		if(agr.begin != null || agr.end != null) {
			NSMutableArray quals = new NSMutableArray(qual);
			if(agr.begin != null) {
				quals.addObject(new EOKeyValueQualifier(Work.DATE_KEY, 
						EOQualifier.QualifierOperatorGreaterThanOrEqualTo, agr.begin));
			}
			if(agr.end != null) {
				quals.addObject(new EOKeyValueQualifier(Work.DATE_KEY, 
						EOQualifier.QualifierOperatorLessThanOrEqualTo, agr.end));
			}
			if(quals.count() > 1)
				qual = new EOAndQualifier(quals);
		}
		EOFetchSpecification fs = new EOFetchSpecification(
				Work.ENTITY_NAME,qual,MyUtility.dateSorter);
		EOEditingContext ec = agr.course().editingContext();
		NSArray works = ec.objectsWithFetchSpecification(fs);
		if(works == null || works.count() == 0) {
			return null;
		} else {
			Enumeration enu = works.objectEnumerator();
			NSMutableArray dates = new NSMutableArray(works.count());
			int lDate = 0;
			NSMutableArray tmp = null;
			NSMutableArray worksOnDate = null;
			while (enu.hasMoreElements()) {
				Work work = (Work) enu.nextElement();
				boolean skip = (work.isOptional() &&
						(work.marks() == null || work.marks().count() == 0 ) &&
						(work.notes() == null || work.notes().count() == 0 ));
				int wDate = agr.dateIndex(work.date());
				if(tmp == null || wDate != lDate) {
					if(tmp != null && tmp.count() > 0) {
						if(tmp.count() > 1)
							EOSortOrdering.sortArrayUsingKeyOrderArray(tmp, Work.sorter);
						dates.addObject(tmp.toArray(new Work[tmp.count()]));
					}
					tmp = new NSMutableArray();
					worksOnDate = new NSMutableArray(workRow(work));
					if(agr.getArray()[wDate] == null) {
						agr.getArray()[wDate] = new NSMutableDictionary(worksOnDate,"works");
					} else {
						agr.getArray()[wDate].takeValueForKey(worksOnDate, "works");
					}
					lDate = wDate;
				} else {
					worksOnDate.addObject(workRow(work));
				}
				if(!skip) {
					tmp.addObject(work);
				}
			}
			if(tmp != null && tmp.count() > 0)
				dates.addObject(tmp.toArray(new Work[tmp.count()]));
			works = dates.immutableClone();
		}
		Enumeration enu = works.objectEnumerator();
		NSMutableDictionary presenterCache = new NSMutableDictionary();
		NSArray students = agr.course().groupList();
		while (enu.hasMoreElements()) {
			Work[] work = (Work[]) enu.nextElement();
			NSMutableDictionary wDict = agr.getOnDate(work[0].date());
		/*	if(wDict == null) {
				wDict = new NSMutableDictionary();
				agr.setOnDate(wDict, work[0].date());
			}*/
			for (int i = 0; i < work.length; i++) {
				boolean optional = work[i].isOptional();
				boolean hasWeight = work[i].hasWeight();
				if(hasWeight) {
					DateAgregate.appendValueToKeyInDict("font-weight:bold;","rowStyle",wDict,null);
				}
				//prepare titleCells
				NSMutableDictionary cells[] = 
					(NSMutableDictionary[])wDict.valueForKey(Work.ENTITY_NAME);
				if(cells == null) {
					cells = new NSMutableDictionary[work.length];
					wDict.takeValueForKey(cells, Work.ENTITY_NAME);
				}
				cells[i] = new NSMutableDictionary(Work.ENTITY_NAME,"id");
				cells[i].takeValueForKey(work[i].theme(), "hover");
				cells[i].takeValueForKey("background-color:" + WorkType.color(work[i]), "style");
//				cells[i].takeValueForKey(work[i],"object");
				//byStudent
				Enumeration stEnu = students.objectEnumerator();
				while (stEnu.hasMoreElements()) {
					Student student = (Student) stEnu.nextElement();
					NSMutableDictionary stDict = (NSMutableDictionary)wDict.objectForKey(student);
					if(stDict == null) {
						stDict = new NSMutableDictionary();
						wDict.setObjectForKey(stDict, student);
					}
					cells = (NSMutableDictionary[])stDict.valueForKey(Work.ENTITY_NAME);
					if(cells == null) {
						cells = new NSMutableDictionary[work.length];
						stDict.takeValueForKey(cells, Work.ENTITY_NAME);
					}
					cells[i] = new NSMutableDictionary(Work.ENTITY_NAME,"id");
					if(hasWeight)
						cells[i].takeValueForKey("font-weight:bold;", "style");
					BigDecimal integral = work[i].integralForStudent(student);
					String note = work[i].noteForStudent(student);
					if(integral != null) {
						String key = (hasWeight)?"integralColor":"weightlessColor";
						FractionPresenter pres = (FractionPresenter)presenterCache.valueForKey(key);
						if(pres == null) {
							pres = BorderSet.presenterForCourse(agr.course(), key);
							if(pres == null)
								pres = BorderSet.fractionPresenterForTitle(ec, "color");
							if(pres == null)
								pres = FractionPresenter.NONE;
							presenterCache.takeValueForKey(pres, key);
						}
						if(pres != FractionPresenter.NONE) {
							StringBuilder style = new StringBuilder();
							key = pres.presentFraction(integral);
							style.append("color:").append(key).append(';');
							if(hasWeight)
								style.append("font-weight:bold;");
							cells[i].takeValueForKey(style.toString(), "style");
						}
						key = (hasWeight)?"workIntegral":"weightless";
						pres = (FractionPresenter)presenterCache.valueForKey(key);
						if(pres == null) {
							pres = BorderSet.presenterForCourse(agr.course(), key);
							if(pres == null) {
								pres = FractionPresenter.NONE;
							}
							presenterCache.takeValueForKey(pres, key);
						}
						if(pres == FractionPresenter.NONE) {
							Mark[] marks = work[i].forPersonLink(student);
							if(marks.length == 1 && note == null) {
								cells[i].takeValueForKey(marks[i].present(),"value");
							} else {
								StringBuilder buf = new StringBuilder();
								for (int j = 0; j < marks.length; j++) {
									buf.append(marks[j].present());
									if(j < marks.length -1)
										buf.append('/');
								}
								if(note != null)
									buf.append('*');
								cells[i].takeValueForKey(buf.toString(),"value");
							}
						} else {
							String value = pres.presentFraction(integral);
							if(note != null)
								value = value + '*';
							cells[i].takeValueForKey(value,"value");
						}
					} // if(integral != null)
					else if(!optional) {
						if(note == null)
							cells[i].takeValueForKey(".","value");
						else
							cells[i].takeValueForKey(".*","value");
					} else if(note != null) {
						String link = MarksPresenter.linkFromNote(note);
						String img = (link==null)?"text.png":"link.png";
						String value = (String)presenterCache.valueForKey(img);
						if(value == null) {
							img = WOApplication.application().resourceManager().
							urlForResourceNamed(img,"RujelBase",null,ctx.request());
							value = "<img src=\"" + img + 
								"\" alt=\"txt\" height=\"16\" width=\"16\">";
							presenterCache.takeValueForKey(value, img);
						}
						if(link != null) {
							StringBuilder buf = new StringBuilder("<a href=\"");
							buf.append(link).append("\" target = \"_blank\">");
							buf.append(value).append("</a>");
							value = buf.toString();
						}
						cells[i].takeValueForKey(value,"value");
					}
					if(note != null) {
						cells[i].takeValueForKey(
								WOMessage.stringByEscapingHTMLAttributeValue(note), "hover");
					}
				} // students Enumerations
			} // works on date
		}
		return ctx.session().valueForKeyPath("strings.RujelCriterial_Strings.consolidatedView");
	}
	
	private static NSMutableDictionary workRow(Work work) {
		if(work == null)
			return null;
		NSMutableDictionary row = new NSMutableDictionary(work,"object");
		row.takeValueForKey(Boolean.TRUE, "skipDate");
		row.takeValueForKey(work.date(), "date");
		row.takeValueForKey(work.workType().typeName(), "rowHover");
		row.takeValueForKey(work.workType().typeName(), "title");
		row.takeValueForKey(work.theme(), "theme");
		row.takeValueForKey(work.announce(), "otherDate");
		row.takeValueForKey(work.trimmedWeight().toString(), "extShort");
		if(work.isCompulsory())
			row.takeValueForKey("font-weight:bold;", "extStyle");
		StringBuilder buf = new StringBuilder("background-color:");
		buf.append(work.color()).append(';');
		if(!work.isOptional())
			buf.append("font-weight:bold;");
		row.takeValueForKey(buf.toString(), "cellStyle");
		row.takeValueForKey(work.color(), "rowColor");
		row.takeValueForKey(new Integer(work.usedCriteria().count() +2), "editorSpan");
		return row;
	}
}
