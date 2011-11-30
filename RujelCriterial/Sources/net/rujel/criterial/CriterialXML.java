// CriterialXML.java

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
import java.util.Date;
import java.util.Enumeration;
import java.util.logging.Logger;

import org.xml.sax.SAXException;

import com.webobjects.eoaccess.EOUtilities;
import com.webobjects.eocontrol.EOAndQualifier;
import com.webobjects.eocontrol.EOEditingContext;
import com.webobjects.eocontrol.EOEnterpriseObject;
import com.webobjects.eocontrol.EOFetchSpecification;
import com.webobjects.eocontrol.EOKeyValueQualifier;
import com.webobjects.eocontrol.EOOrQualifier;
import com.webobjects.eocontrol.EOQualifier;
import com.webobjects.eocontrol.EOSortOrdering;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSDictionary;
import com.webobjects.foundation.NSMutableArray;
import com.webobjects.foundation.NSTimestamp;

import net.rujel.base.Indexer;
import net.rujel.base.SettingsBase;
import net.rujel.base.XMLGenerator;
import net.rujel.interfaces.EduCourse;
import net.rujel.interfaces.EduLesson;
import net.rujel.interfaces.Student;
import net.rujel.reusables.Period;
import net.rujel.reusables.Various;
import net.rujel.reusables.WOLogLevel;
import net.rujel.reusables.xml.EasyGenerationContentHandlerProxy;
import net.rujel.reusables.xml.GeneratorModule;

public class CriterialXML extends GeneratorModule {

	public CriterialXML(NSDictionary options) {
		super(options);
		Student student = (Student)settings.valueForKey("student");
		NSArray students = (NSArray)settings.valueForKey("students");
		if(student != null || (students != null && students.count() > 0)) {
			if(student == null)
				student = (Student)students.objectAtIndex(0);
			EOEditingContext ec = student.editingContext();
			if(students == null || students.count() == 0) {
				students = new NSArray(student);
			} else if(!students.containsObject(student)) {
				students = students.arrayByAddingObject(student);
			}
			student = null;
			Date since = (NSTimestamp)settings.valueForKey("since");
			Date to = (NSTimestamp)settings.valueForKey("to");		
			if(since == null || to == null) {
				Period period = (Period)settings.valueForKey("period");
				if(period != null) {
					if(since == null) {
						since = period.begin();
						if(!(since instanceof NSTimestamp))
							since = new NSTimestamp(since);
					}
					if(to == null) {
						to = period.end();
						if(!(to instanceof NSTimestamp))
							to = new NSTimestamp(to);
					}
				}
			}
			Object workType = settings.valueForKeyPath("reporter.settings.marks.workType");
			if(workType instanceof String) {
				try {
					workType = Various.parseEO((String)workType, ec);
				} catch (Exception e) {
					Logger.getLogger("rujel.criterial").log(WOLogLevel.WARNING,
							"Error parsing workType in reportSettings",e);
					workType = null;
				}
			}
			boolean dateSet = Various.boolForObject(options.valueForKeyPath(
					"reporter.settings.marks.dateSet"));
			NSMutableArray quals = new NSMutableArray(Various.getEOInQualifier("student", students));
			if(workType != null)
				quals.addObject(new EOKeyValueQualifier("work.workType",
					EOQualifier.QualifierOperatorEqual,workType));
			if(since != null) {
				EOQualifier[] or = new EOQualifier[2];
				if(dateSet) {
					or[0] = new EOKeyValueQualifier("work.date",
							EOQualifier.QualifierOperatorGreaterThanOrEqualTo,since);
					or[1] = new EOKeyValueQualifier(Mark.DATE_SET_KEY,
							EOQualifier.QualifierOperatorGreaterThanOrEqualTo,since);
					quals.addObject(new EOOrQualifier(new NSArray(or)));
				} else {
					quals.addObject(new EOKeyValueQualifier("work.date",
							EOQualifier.QualifierOperatorGreaterThanOrEqualTo,since));
				}
			}
			if(to != null) {
				EOQualifier[] or = new EOQualifier[2];
				if(dateSet) {
					or[0] = new EOKeyValueQualifier("work.date",
							EOQualifier.QualifierOperatorLessThanOrEqualTo,to);
					or[1] = new EOKeyValueQualifier(Mark.DATE_SET_KEY,
							EOQualifier.QualifierOperatorLessThanOrEqualTo,to);
					quals.addObject(new EOOrQualifier(new NSArray(or)));
				} else {
					quals.addObject(new EOKeyValueQualifier("work.date",
							EOQualifier.QualifierOperatorLessThanOrEqualTo,to));
				}
			}
			EOFetchSpecification fs = new EOFetchSpecification(Mark.ENTITY_NAME,
					new EOAndQualifier(quals),null);
			fs.setRefreshesRefetchedObjects(true);
			NSArray allMarks = ec.objectsWithFetchSpecification(fs);
			extractWorks(allMarks);
			fs.setEntityName("WorkNote");
			if(dateSet) {
				int idx = (workType == null)?1:2;
				if(since != null) {
					quals.replaceObjectAtIndex(new EOKeyValueQualifier("work.date",
							EOQualifier.QualifierOperatorGreaterThanOrEqualTo,since), idx);
					idx++;
				}
				if(to != null)
					quals.replaceObjectAtIndex(new EOKeyValueQualifier("work.date",
							EOQualifier.QualifierOperatorLessThanOrEqualTo,to),idx);
				fs.setQualifier(new EOAndQualifier(quals));
			}
			allMarks = ec.objectsWithFetchSpecification(fs);
			extractWorks(allMarks);
		}
	}
	
	protected void extractWorks(NSArray list) {
		if(list == null || list.count() == 0)
			return;
		if(preloadWorks == null)
			preloadWorks = new NSMutableArray();
		Enumeration enu = list.objectEnumerator();
		NSArray courses = (NSArray)settings.valueForKey("courses");
		NSMutableArray extraCourses = (NSMutableArray)settings.valueForKey("extraCourses");
		while (enu.hasMoreElements()) {
			EOEnterpriseObject m = (EOEnterpriseObject) enu.nextElement();
			Work work = (Work)m.valueForKey(Mark.WORK_KEY);
			if(work == null || preloadWorks.containsObject(work))
				continue;
			preloadWorks.addObject(work);
			if(courses == null)
				continue;
			EduCourse course = work.course();
			if(!courses.containsObject(course)) {
				if(extraCourses == null) {
					extraCourses = new NSMutableArray(course);
					settings.takeValueForKey(extraCourses, "extraCourses");
				} else if(!extraCourses.contains(course)) {
					extraCourses.addObject(course);
				}
			}
		}
	}
	
	
	public Integer sort() {
		return new Integer(20);
	}
	
	protected ForCourse forCourse;
	protected NSMutableArray preloadWorks;
	
	protected static class ForCourse {
		protected EduCourse course;
		protected FractionPresenter integralPresenter;
		protected FractionPresenter weightlessPresenter;
		protected FractionPresenter integralColor;
		protected FractionPresenter weightlessColor;
		protected CriteriaSet critSet;
		
		public ForCourse(EduCourse aCourse) {
			course = aCourse;
			critSet = CriteriaSet.critSetForCourse(course);
			EOEditingContext ec = course.editingContext();
			EOEnterpriseObject setting = SettingsBase.settingForCourse("presenters.workIntegral", 
					course, ec);
			if(setting != null)
				integralPresenter = presenterForSetting(setting, ec);
			if(integralPresenter == null)
				integralPresenter = FractionPresenter.PERCENTAGE;
			setting = SettingsBase.settingForCourse("presenters.weightless",course, ec);
			if(setting != null)
				weightlessPresenter = presenterForSetting(setting, ec);
			setting = SettingsBase.settingForCourse("presenters.integralColor",course, ec);
			if(setting != null)
				integralColor = presenterForSetting(setting, ec);
			if(integralColor == null)
				integralColor = BorderSet.fractionPresenterForTitle(ec, "color");
			setting = SettingsBase.settingForCourse("presenters.weightlessColor",course, ec);
			if(setting != null)
				weightlessColor = presenterForSetting(setting, ec);
			if(weightlessColor == null)
				weightlessColor = BorderSet.fractionPresenterForTitle(ec, "color");
		}
		
		protected FractionPresenter presenterForSetting(EOEnterpriseObject setting,
				EOEditingContext ec) {
			Integer pKey = (Integer)setting.valueForKey(SettingsBase.NUMERIC_VALUE_KEY);
			String key = (String)setting.valueForKeyPath(SettingsBase.TEXT_VALUE_KEY);
			if (pKey != null) {
				return (BorderSet)EOUtilities.objectWithPrimaryKeyValue(
						ec, BorderSet.ENTITY_NAME, pKey);
			} else if(key != null && !key.startsWith("none")) {
				return BorderSet.fractionPresenterForTitle(ec, key);
			}
			return null;
		}
	}
	
	public void generateFor(Object object,
			EasyGenerationContentHandlerProxy handler) throws SAXException {
		if(object instanceof EduCourse) {
			generateFor((EduCourse)object,handler);
		} else if(object instanceof Work) {
			generateFor((Work)object,handler);
		}
	}
	
	public void generateFor(EduCourse course,
				EasyGenerationContentHandlerProxy handler) throws SAXException {
		if(!handler.recentElement().equals("course"))
			throw new SAXException("Should generate within course");
		{
			NSDictionary opt = (NSDictionary)settings.valueForKeyPath("reporter.settings");
			if(opt != null && !Various.boolForObject(opt.valueForKeyPath("marks.active")))
				return;
		}
		Date since = (NSTimestamp)settings.valueForKey("since");
		Date to = (NSTimestamp)settings.valueForKey("to");		
		if(since == null || to == null) {
			Period period = (Period)settings.valueForKey("period");
			if(period != null) {
				if(since == null) {
					since = period.begin();
					if(!(since instanceof NSTimestamp))
						since = new NSTimestamp(since);
				}
				if(to == null) {
					to = period.end();
					if(!(to instanceof NSTimestamp))
						to = new NSTimestamp(to);
				}
			}
		}
		EOEditingContext ec = course.editingContext();
		Object workType = settings.valueForKeyPath("reporter.settings.marks.workType");
		if(workType instanceof String) {
			try {
				workType = Various.parseEO((String)workType, ec);
			} catch (Exception e) {
				Logger.getLogger("rujel.criterial").log(WOLogLevel.WARNING,
						"Error parsing workType in reportSettings",e);
				workType = null;
			}
		}
		EOQualifier qual = new EOKeyValueQualifier("course",
				EOQualifier.QualifierOperatorEqual,course);
		NSMutableArray quals = new NSMutableArray(qual);
		if(since != null)
			quals.addObject(new EOKeyValueQualifier(Work.DATE_KEY,
					EOQualifier.QualifierOperatorGreaterThanOrEqualTo,since));
		if(to != null)
			quals.addObject(new EOKeyValueQualifier(Work.DATE_KEY,
					EOQualifier.QualifierOperatorLessThanOrEqualTo,to));
		if(workType != null)
			quals.addObject(new EOKeyValueQualifier(Work.WORK_TYPE_KEY,
					EOQualifier.QualifierOperatorEqual,workType));
		NSArray preloaded = (preloadWorks== null)?null:
			EOQualifier.filteredArrayWithQualifier(preloadWorks, qual);
		qual = new EOAndQualifier(quals);
		EOFetchSpecification fs = new EOFetchSpecification(Work.ENTITY_NAME,qual,EduLesson.sorter);
		fs.setRefreshesRefetchedObjects(true);
		NSArray works = ec.objectsWithFetchSpecification(fs);
		if(preloaded != null && preloaded.count() > 0) {
			Enumeration enu = preloaded.objectEnumerator();
			NSMutableArray toAdd = null;
			while (enu.hasMoreElements()) {
				Work work = (Work) enu.nextElement();
				if(works.containsObject(work))
					continue;
				if(toAdd == null)
					toAdd = new NSMutableArray(work);
				else
					toAdd.addObject(work);
			}
			if(toAdd != null) {
				toAdd.addObjectsFromArray(works);
				works = EOSortOrdering.sortedArrayUsingKeyOrderArray(toAdd, EduLesson.sorter);
			}
		}
		if(works != null && works.count() > 0) {
			handler.prepareEnumAttribute("type","work");
			handler.startElement("containers");
			Enumeration lenu = works.objectEnumerator();
			while (lenu.hasMoreElements()) {
				Work work = (Work) lenu.nextElement();
				generateFor(work, handler);
			}
			handler.endElement("containers");
		}
	}
	
	public void generateFor(Work work,
			EasyGenerationContentHandlerProxy handler) throws SAXException {
		if(!handler.recentElement().equals("containers"))
			throw new SAXException("Should generate within 'containers'");
		Number lvl = (Number)settings.valueForKeyPath("reporter.settings.marks.level");
		if(lvl != null && lvl.intValue() > 2)
			lvl = null;
		NSArray mask = work.criterMask();
		if(lvl != null) {
//			if(mask == null || mask.count() == 0)
//				return;
			if(lvl.intValue() == 2 && (mask != null && mask.count() > 0))
				lvl = null;
			else if(work.isOptional()) {
				if(lvl.intValue() == 0)
					return;
			} else {
				lvl = null;
			}
		}
		Student student = (Student)settings.valueForKey("student");
		NSArray students = (NSArray)settings.valueForKey("students");
		if(student == null && students == null)
			lvl = null;
		if(lvl != null && student != null) {
			if((work.forPersonLink(student)) != null || work.noteForStudent(student) != null) {
				lvl = null;
				raiseCounterForObject(student);
			}
		}
		if(lvl != null && students != null) {
			Enumeration enu = students.objectEnumerator();
			while (lvl != null && enu.hasMoreElements()) {
				Student st = (Student) enu.nextElement();
				if((work.forPersonLink(st)) != null || work.noteForStudent(st) != null)
					lvl = null;
			}
		}
		if(lvl != null)
			return;
		if(forCourse == null || forCourse.course != work.course())
			forCourse = new ForCourse(work.course());
		if(work._critSet == null)
			work._critSet = forCourse.critSet;
		if(work._critSet == null)
			work._critSet = NullValue;
		handler.prepareAttribute("id", XMLGenerator.getID(work));
		Object tmp = work.number();
		if(tmp != null)
			handler.prepareAttribute("num", tmp.toString());
		handler.prepareAttribute("date", XMLGenerator.formatDate(work.date()));
//		handler.prepareAttribute("title", work.title());
		handler.prepareAttribute("type", work.workType().typeName());
		handler.startElement("container");
		handler.element("content", work.theme());
		tmp = work.homeTask();
		if(tmp != null)
			handler.element("task", tmp.toString());
		if(mask != null && mask.count() > 0) {
			if(mask.count() > 1) {
				mask = EOSortOrdering.sortedArrayUsingKeyOrderArray(mask, CriteriaSet.sorter);
			} else {
				EOEnterpriseObject cr = (EOEnterpriseObject) mask.objectAtIndex(0);
				tmp = cr.valueForKey("criterion");
				if(((Integer)tmp).intValue() == 0) {
					tmp = cr.valueForKey("max");
					if(((Integer)tmp).intValue() == 5)
						mask = null;
				}
			}
			if(mask != null) {
				handler.startElement("criteria");
				Enumeration enu = mask.objectEnumerator();
				while (enu.hasMoreElements()) {
					EOEnterpriseObject cr = (EOEnterpriseObject) enu.nextElement();
					tmp = cr.valueForKey("criterion");
					handler.prepareAttribute("idx", tmp.toString());
					handler.prepareAttribute("title",
							CriteriaSet.critNameForNum((Integer)tmp, forCourse.critSet));
					if(forCourse.critSet != null) {
						Indexer idx = forCourse.critSet.indexerForCriter((Integer)tmp);
						tmp = null;
						if(idx != null) {
							Integer mIndex = idx.maxIndex();
							if(mIndex != null)
								tmp = idx.valueForIndex(mIndex.intValue(), null);
						}
						if(tmp == null)
							tmp = cr.valueForKey("max");
					} else {
						tmp = cr.valueForKey("max");
					}
					handler.prepareAttribute("max", tmp.toString());
					tmp = cr.valueForKey("weight");
					if(tmp != null)
						handler.prepareAttribute("weight", tmp.toString());
					handler.element("criterion", null);
				}
				handler.endElement("criteria");
			}
			if(work.hasWeight()) {
				handler.prepareAttribute("weight", work.trimmedWeight().toString());
				handler.prepareEnumAttribute("compulsory", Boolean.toString(work.isCompulsory()));
				handler.element("calc", null);
			}
		}
			NSArray marks = work.marks();
			NSArray notes = work.notes();
			if(marks != null && marks.count() > 0) {
				handler.startElement("marks");
				Enumeration enu = work.students().objectEnumerator();
				while (enu.hasMoreElements()) {
					Student st = (Student) enu.nextElement();
					if(student != null) {
						if(st != student)
							continue;
					} else if(students != null && !students.containsObject(st))
						continue;
					Mark[] mrk = work.forPersonLink(st);
					String note = work.noteForStudent(st);
					if(mrk == null && note == null)
						continue;
					raiseCounterForObject(st);
					handler.prepareAttribute("student", XMLGenerator.getID(st));
					if(mask == null) { // non criterial mark (that is strange)
						if(mrk != null && mrk[0] != null) {
							handler.prepareAttribute("value", mrk[0].present());
						}
						handler.startElement("mark");
					} else { // criterial mark
						tmp = work.integralForStudent(st);
						if(tmp != null) {
							if(work.hasWeight()) {
								if(forCourse.integralPresenter != null)
									handler.prepareAttribute("value", 
											forCourse.integralPresenter.presentFraction(
													(BigDecimal)tmp));
								handler.startElement("mark");
								handler.prepareEnumAttribute("type", "color");
								handler.element("present", 
										forCourse.integralColor.presentFraction((BigDecimal)tmp));
							} else {
								if(forCourse.weightlessPresenter != null)
									handler.prepareAttribute("value", 
											forCourse.weightlessPresenter.presentFraction(
													(BigDecimal)tmp));
								handler.startElement("mark");
								handler.prepareEnumAttribute("type", "color");
								handler.element("present", 
										forCourse.weightlessColor.presentFraction((BigDecimal)tmp));
							}
							handler.prepareEnumAttribute("type", "inner");
							handler.element("present", tmp.toString());
							if(mrk != null) {
								for (int i = 0; i < mrk.length; i++) {
									if(mrk[i] == null)
										continue;
									handler.prepareAttribute("criter", 
											mrk[i].criterion().toString());
									handler.prepareAttribute("value", mrk[i].present());
									handler.startElement("crmark");
									if(mrk[i].indexer() != null) {
										handler.prepareEnumAttribute("type", "inner");
										handler.element("present", mrk[i].value().toString());
									}
									handler.endElement("crmark");
								}
							}
						} else {
							handler.startElement("mark");
						}
					} // criterial marks
					if(note != null) {
						if(((String)note).startsWith("http"))
							handler.element("weblink", note.toString());
						else
							handler.element("comment", note.toString());
					}
					handler.endElement("mark");
				} // work.students enumeration
				handler.endElement("marks");
			} else if(notes != null && notes.count() > 0) {
				handler.startElement("marks");
				Enumeration enu = notes.objectEnumerator();
				while (enu.hasMoreElements()) {
					EOEnterpriseObject nt = (EOEnterpriseObject) enu.nextElement();
					Student st = (Student)nt.valueForKey("student");
					if(student != null) {
						if(st != student)
							continue;
					} else if(students != null && !students.containsObject(st))
						continue;
					tmp = nt.valueForKey("note");
					if(tmp == null)
						continue;
					raiseCounterForObject(st);
					handler.prepareAttribute("student", XMLGenerator.getID(st));
					handler.startElement("mark");
					handler.element("comment", tmp.toString());
					handler.endElement("mark");
				}
				handler.endElement("marks");
			}
//		} // has mask
		if(work.isHometask() != work.workType().namedFlags().flagForKey("hometask")) {
			handler.prepareAttribute("key", "hometask");
			handler.element("param", Boolean.toString(work.isHometask()));
		}
		tmp = work.announce();
		if(tmp != null && !work.date().equals(tmp)) {
			handler.prepareAttribute("key", "announce");
			handler.element("param", XMLGenerator.formatDate((NSTimestamp)tmp));
		}
		tmp = work.load();
		if(tmp != null && ((Integer)tmp).intValue() > 0) {
			handler.prepareAttribute("key", "load");
			handler.element("param", tmp.toString());
		}
		handler.endElement("container");
}
}
