// LessonsXML.java

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
		}
	}
	
	public void generateFor(EduCourse course,
				EasyGenerationContentHandlerProxy handler) throws SAXException {
		if(!handler.recentElement().equals("course"))
			throw new SAXException("Should generate within course");
		{
			NSDictionary opt = (NSDictionary)settings.valueForKeyPath("reporter.settings");
			if(opt != null && opt.valueForKey("lessons") == null)
				return;
		}
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
		EOFetchSpecification fs = new EOFetchSpecification(BaseLesson.ENTITY_NAME,
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
		tmp = lesson.homeTask();
		if(tmp != null)
			handler.element("task", (String)tmp);
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
