package net.rujel.base;

import java.util.Date;
import java.util.Enumeration;

import org.xml.sax.SAXException;

import com.webobjects.eocontrol.EOAndQualifier;
import com.webobjects.eocontrol.EOEnterpriseObject;
import com.webobjects.eocontrol.EOFetchSpecification;
import com.webobjects.eocontrol.EOKeyValueQualifier;
import com.webobjects.eocontrol.EOQualifier;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSDictionary;
import com.webobjects.foundation.NSMutableArray;
import com.webobjects.foundation.NSTimestamp;

import net.rujel.interfaces.EduCourse;
import net.rujel.interfaces.EduLesson;
import net.rujel.interfaces.Student;
import net.rujel.reusables.Period;
import net.rujel.reusables.xml.EasyGenerationContentHandlerProxy;
import net.rujel.reusables.xml.GeneratorModule;

public class LessonsXML extends GeneratorModule {

	public LessonsXML(NSDictionary options) {
		super(options);
	}
	
	public Integer sort() {
		return new Integer(10);
	}
	
	@Override
	public void generateFor(Object object,
			EasyGenerationContentHandlerProxy handler) throws SAXException {
		if(object instanceof EduCourse) {
			generateFor((EduCourse)object,handler);
		} else if(object instanceof BaseLesson) {
			generateFor((BaseLesson)object,handler);
		} else {
			throw new SAXException("Can only generate for EduCourse or BaseLesson");
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
			quals.addObject(new EOKeyValueQualifier(BaseLesson.DATE_KEY,
					EOQualifier.QualifierOperatorGreaterThanOrEqualTo,since));
		if(to != null)
			quals.addObject(new EOKeyValueQualifier(BaseLesson.DATE_KEY,
					EOQualifier.QualifierOperatorLessThanOrEqualTo,to));
		EOFetchSpecification fs = new EOFetchSpecification("BaseLesson",
				new EOAndQualifier(quals),EduLesson.sorter);
		fs.setRefreshesRefetchedObjects(true);
		NSArray lessons = course.editingContext().objectsWithFetchSpecification(fs);
		if(lessons != null && lessons.count() > 0) {
			handler.prepareEnumAttribute("type","lesson");
			handler.startElement("containers");
			Enumeration lenu = lessons.objectEnumerator();
			while (lenu.hasMoreElements()) {
				BaseLesson lesson = (BaseLesson) lenu.nextElement();
				generateFor(lesson, handler);
			}
			handler.endElement("containers");
		}
	}
	
	public void generateFor(BaseLesson lesson,
			EasyGenerationContentHandlerProxy handler) throws SAXException {
		if(!handler.recentElement().equals("containers"))
			throw new SAXException("Should generate within 'containers'");
	
		handler.prepareAttribute("id", XMLGenerator.getID(lesson));
		Object tmp = lesson.number();
		if(tmp != null)
			handler.prepareAttribute("num", tmp.toString());
		handler.prepareAttribute("date", XMLGenerator.formatDate(lesson.date()));
//		handler.prepareAttribute("title", lesson.title());
		handler.startElement("container");
		handler.element("content", lesson.theme());
		handler.element("task", lesson.homeTask());
		NSArray notes = lesson.notes();
		if(notes != null && notes.count() > 0) {
			handler.startElement("marks");
			Enumeration enu = notes.objectEnumerator();
			Student student = (Student)settings.valueForKey("student");
			NSArray students = (NSArray)settings.valueForKey("students");
			while (enu.hasMoreElements()) {
				EOEnterpriseObject nt = (EOEnterpriseObject) enu.nextElement();
				Student st = (Student)nt.valueForKey("student");
				if(student != null) {
					if(st != student)
						continue;
				} else if(students != null && !students.containsObject(st))
					continue;
				String note = (String)nt.valueForKey("note");
				if(note == null)
					continue;
				handler.prepareAttribute("student", XMLGenerator.getID(st));
				if(note.length() <= 5) {
					handler.prepareAttribute("value",note);
					handler.element("mark", null);
				} else {
					handler.startElement("mark");
					handler.element("comment", note);
					handler.endElement("mark");
				}
			}
			handler.endElement("marks");
		}
		handler.endElement("container");		
	}
}
