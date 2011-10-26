package net.rujel.criterial;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Enumeration;

import org.xml.sax.SAXException;

import com.webobjects.eoaccess.EOUtilities;
import com.webobjects.eocontrol.EOAndQualifier;
import com.webobjects.eocontrol.EOEditingContext;
import com.webobjects.eocontrol.EOEnterpriseObject;
import com.webobjects.eocontrol.EOFetchSpecification;
import com.webobjects.eocontrol.EOKeyValueQualifier;
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
import net.rujel.reusables.xml.EasyGenerationContentHandlerProxy;
import net.rujel.reusables.xml.GeneratorModule;

public class CriterialXML extends GeneratorModule {

	public CriterialXML(NSDictionary options) {
		super(options);
	}
	
	public Integer sort() {
		return new Integer(20);
	}
	
	protected ForCourse forCourse;
	
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
		} else {
			throw new SAXException("Can only generate for EduCourse or Work");
		}
	}
	
	public void generateFor(EduCourse course,
				EasyGenerationContentHandlerProxy handler) throws SAXException {
		if(!handler.recentElement().equals("course"))
			throw new SAXException("Should generate within course");
		Date since = (NSTimestamp)settings.valueForKey("since");
		Date to = (NSTimestamp)settings.valueForKey("to");		
		Period period = (Period)settings.valueForKey("period");
		if(period != null) {
			if(since == null) {
				since = period.begin();
				if(!(since instanceof NSTimestamp))
					since = new NSTimestamp(since);
			}
			if(to == null) {
				to = period.begin();
				if(!(to instanceof NSTimestamp))
					to = new NSTimestamp(to);
			}
		}
		NSMutableArray quals = new NSMutableArray(new EOKeyValueQualifier(
				"course",EOQualifier.QualifierOperatorEqual,course));
		if(since != null)
			quals.addObject(new EOKeyValueQualifier(Work.DATE_KEY,
					EOQualifier.QualifierOperatorGreaterThanOrEqualTo,since));
		if(to != null)
			quals.addObject(new EOKeyValueQualifier(Work.DATE_KEY,
					EOQualifier.QualifierOperatorLessThanOrEqualTo,to));
		EOFetchSpecification fs = new EOFetchSpecification(Work.ENTITY_NAME,
				new EOAndQualifier(quals),EduLesson.sorter);
		fs.setRefreshesRefetchedObjects(true);
		NSArray works = course.editingContext().objectsWithFetchSpecification(fs);
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
		NSArray mask = work.criterMask();
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
			NSArray marks = work.marks();
			NSArray notes = work.notes();
			Student student = (Student)settings.valueForKey("student");
			NSArray students = (NSArray)settings.valueForKey("students");
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
					handler.prepareAttribute("student", XMLGenerator.getID(st));
					if(mask == null) { // non criterial mark
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
				}
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
					handler.prepareAttribute("student", XMLGenerator.getID(st));
					handler.startElement("mark");
					handler.element("comment", tmp.toString());
					handler.endElement("mark");
				}
				handler.endElement("marks");
			}
		}
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
