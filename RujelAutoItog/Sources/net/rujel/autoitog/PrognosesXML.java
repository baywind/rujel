package net.rujel.autoitog;

import java.text.SimpleDateFormat;
import java.util.Enumeration;

import org.xml.sax.SAXException;

import com.webobjects.eocontrol.EOAndQualifier;
import com.webobjects.eocontrol.EOEditingContext;
import com.webobjects.eocontrol.EOFetchSpecification;
import com.webobjects.eocontrol.EOKeyValueQualifier;
import com.webobjects.eocontrol.EOQualifier;
import com.webobjects.eocontrol.EOSortOrdering;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSDictionary;
import com.webobjects.foundation.NSMutableArray;
import com.webobjects.foundation.NSTimestamp;

import net.rujel.base.SettingsBase;
import net.rujel.base.XMLGenerator;
import net.rujel.eduresults.ItogContainer;
import net.rujel.eduresults.ItogMark;
import net.rujel.interfaces.EduCourse;
import net.rujel.reusables.Period;
import net.rujel.reusables.Various;
import net.rujel.reusables.xml.EasyGenerationContentHandlerProxy;
import net.rujel.reusables.xml.GeneratorModule;

public class PrognosesXML extends GeneratorModule {
	
	public static final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");


	public PrognosesXML(NSDictionary options) {
		super(options);
	}
	
	public Integer sort() {
		return new Integer(60);
	}

	public void generateFor(Object object,
			EasyGenerationContentHandlerProxy handler) throws SAXException {
		if(object instanceof EduCourse) {
			generateFor((EduCourse)object,handler);
		} else {
			throw new SAXException("Can only generate for EduCourse");
		}
	}
	
	public void generateFor(EduCourse course,
				EasyGenerationContentHandlerProxy handler) throws SAXException {
		if(!handler.recentElement().equals("course"))
			throw new SAXException("Should generate within course");
		EOEditingContext ec = course.editingContext();
		String listName = SettingsBase.stringSettingForCourse(ItogMark.ENTITY_NAME, course, ec);
		if(listName == null)
			return;
		NSTimestamp date = (NSTimestamp)settings.valueForKey("to");		
		Period period = (Period)settings.valueForKey("period");
		if(period != null && !period.contains(date)) {
			long millis = period.end().getTime()/2 + period.begin().getTime()/2;
			date = new NSTimestamp(millis);
		}
		NSMutableArray quals = new NSMutableArray(3);
		quals.addObject(new EOKeyValueQualifier("course",EOQualifier.QualifierOperatorEqual,course));
		quals.addObject(new EOKeyValueQualifier(Prognosis.FIRE_DATE_KEY
				,EOQualifier.QualifierOperatorGreaterThanOrEqualTo,date));
		if(settings.valueForKey("student") != null) {
			quals.addObject(new EOKeyValueQualifier("student", EOQualifier.QualifierOperatorEqual,
					settings.valueForKey("student")));
		} else {
			NSArray students = (NSArray)settings.valueForKey("students");
			if(students != null)
				quals.addObject(Various.getEOInQualifier("student", students));
		}
		EOFetchSpecification fs = new EOFetchSpecification(Prognosis.ENTITY_NAME,
				new EOAndQualifier(new NSArray(quals)),null);
		NSArray prognoses = ec.objectsWithFetchSpecification(fs);
		if(prognoses == null || prognoses.count() == 0) {
			return;
		} else if(prognoses.count() > 1) {
			NSArray sorter = new NSArray(new EOSortOrdering(Prognosis.ITOG_CONTAINER_KEY,
					EOSortOrdering.CompareAscending));
			prognoses = EOSortOrdering.sortedArrayUsingKeyOrderArray(prognoses, sorter);
		}
		handler.prepareEnumAttribute("type", "prognosis");
		handler.startElement("containers");
		Enumeration enu = prognoses.objectEnumerator();
		AutoItog ai = null;
		while (enu.hasMoreElements()) {
			Prognosis progn = (Prognosis) enu.nextElement();
			if(progn.namedFlags().flagForKey("disabled"))
				continue;
			if(ai == null || ai.itogContainer() != progn.itogContainer()) {
				if(ai != null) {
					handler.endElement("marks");
					handler.endElement("container");
				}
				ItogContainer itog = progn.itogContainer();
				ai = AutoItog.forListName(listName, itog);
				if(ai == null)
					continue;
				handler.prepareAttribute("id", XMLGenerator.getID(ai));
				handler.prepareAttribute("date", XMLGenerator.formatDate(ai.fireDate()));
				if(itog.num() != null)
					handler.prepareAttribute("num", itog.num().toString());
				handler.prepareAttribute("type", itog.itogType().name());
				handler.prepareAttribute("title", itog.title());
				handler.startElement("container");
				handler.element("content",itog.name());
				handler.prepareAttribute("key", "flags");
				handler.element("param", ai.flags().toString());
				handler.prepareAttribute("key", "time");
				handler.element("param", timeFormat.format(ai.fireTime()));
				Timeout timeout = CourseTimeout.getTimeoutForCourseAndPeriod(course, itog);
				if(timeout != null) {
					handler.prepareAttribute("key", "timeout");
					handler.element("param", XMLGenerator.formatDate(timeout.fireDate()));
					handler.prepareAttribute("key", "timeoutFlags");
					handler.element("param", timeout.flags().toString());
					handler.prepareAttribute("key", "timeoutReason");
					handler.element("param", timeout.reason());
				} // timeout
				handler.startElement("marks");
			} // describe container
			handler.prepareAttribute("student", XMLGenerator.getID(progn.student()));
			handler.prepareAttribute("value", progn.mark());
			handler.startElement("mark");
			handler.prepareEnumAttribute("type", "inner");
			handler.element("present", progn.value().toString());
			handler.prepareAttribute("key", "complete");
			handler.element("param", progn.complete().toString());
			handler.prepareAttribute("key", "flags");
			handler.element("param", progn.flags().toString());
			Timeout timeout = StudentTimeout.timeoutForStudentAndCourse(
					progn.student(), course, progn.itogContainer());
			if(timeout != null) {
				handler.prepareAttribute("key", "timeout");
				handler.element("param", XMLGenerator.formatDate(timeout.fireDate()));
				handler.prepareAttribute("key", "timeoutFlags");
				handler.element("param", timeout.flags().toString());
				handler.prepareAttribute("key", "timeoutReason");
				handler.element("param", timeout.reason());
			}
			handler.endElement("mark");
		} // prognosws enumeration
		handler.endElement("marks");
		handler.endElement("container");
		handler.endElement("containers");
	}

}
