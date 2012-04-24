// XMLGenerator.java

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

package net.rujel.io;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.logging.Logger;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.URIResolver;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import com.webobjects.appserver.WOSession;
import com.webobjects.eoaccess.EOUtilities;
import com.webobjects.eocontrol.EOEditingContext;
import com.webobjects.eocontrol.EOEnterpriseObject;
import com.webobjects.eocontrol.EOObjectStore;
import com.webobjects.eocontrol.EOObjectStoreCoordinator;
import com.webobjects.eocontrol.EOSortOrdering;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSDictionary;
import com.webobjects.foundation.NSMutableArray;
import com.webobjects.foundation.NSMutableDictionary;
import com.webobjects.foundation.NSTimestamp;

import net.rujel.base.BaseCourse;
import net.rujel.base.MyUtility;
import net.rujel.interfaces.*;
import net.rujel.reusables.DataBaseConnector;
import net.rujel.reusables.SessionedEditingContext;
import net.rujel.reusables.SettingsReader;
import net.rujel.reusables.Various;
import net.rujel.reusables.WOLogLevel;
import net.rujel.reusables.xml.AbstractObjectReader;
import net.rujel.reusables.xml.GeneratorModule;
import net.rujel.reusables.xml.Resolver;
import net.rujel.reusables.xml.TransormationErrorListener;

public class XMLGenerator extends AbstractObjectReader {

	public static class RujelInputSource extends InputSource {
		private WOSession ses;
		private NSDictionary options;
		public RujelInputSource(WOSession session, NSDictionary options) {
			super();
			ses = session;
			this.options = options;
		}
	}
	
	public static Transformer getTransformer(WOSession session,NSDictionary reporter,
			InputSource input) throws TransformerException {
		
		TransformerFactory factory = TransformerFactory.newInstance();
		factory.setErrorListener(new TransormationErrorListener(session));
		Transformer transformer = null;
		String transName = (reporter == null)?null:(String)reporter.valueForKey("transform");
		File sourceDir = null;
		if(transName != null) {
			String root = SettingsReader.stringForKeyPath("reportsDir", "CONFIGDIR/RujelReports");
			root = Various.convertFilePath(root);
			sourceDir = new File(root,"StudentReport");
			File file = new File(sourceDir,transName);
			if(file.exists())
				transformer = factory.newTransformer(new StreamSource(file));
		}
		if(transformer == null) {
			transformer = factory.newTransformer();
    		transformer.setOutputProperty("indent", "yes");
    		transformer.setOutputProperty("{http://xml.apache.org/xalan}indent-amount", "4");
    		return transformer;
		}
		NSArray list = (NSArray)reporter.valueForKey("sources");
		if(list != null && list.count() > 0) {
			Enumeration enu = list.objectEnumerator();
			NSMutableDictionary sources = new NSMutableDictionary();
			while (enu.hasMoreElements()) {
				String srcName = (String) enu.nextElement();
				if("Persdata".equals(srcName)) {
			        Source src = new SAXSource(new Persdata(),input);
					sources.takeValueForKey(src,"persdata.xml");
				} else if("Options".equals(srcName)) {
			        Source src = new SAXSource(new Options(),input);
					sources.takeValueForKey(src,"options.xml");
				} else {
					File file = new File(sourceDir,srcName);
					if(file.exists())
						sources.takeValueForKey(new StreamSource(file),srcName);
				}
			}
			if(sources.count() > 0) {
				URIResolver resolver = new Resolver(sources);
				transformer.setURIResolver(resolver);
			}
		}
		return transformer;
	}
	
	public static byte[] generate(WOSession session, NSDictionary options)
									throws TransformerException {
		
		InputSource input =  new RujelInputSource(session,options);
		Transformer transformer = getTransformer(session, 
				(NSDictionary)options.valueForKey("reporter"), input);
		XMLReader reader = null;
		String srcName = (String)options.valueForKeyPath("reporter.mainSource");
		if("Persdata".equals(srcName)) {
			reader = new Persdata();
		} else if("Options".equals(srcName)) {
			reader = new Options();
		} else {
			reader = new XMLGenerator();
		}
        Source src = new SAXSource(reader,input);
    	ByteArrayOutputStream out = new ByteArrayOutputStream();
    	Result res = new StreamResult(out);
      	transformer.transform(src, res);
    	return out.toByteArray();
	}
	
	public static byte[] transformData(WOSession session, NSDictionary options, byte[] data)
										throws TransformerException {
		if(options.valueForKeyPath("reporter.transform") == null)
			return data;
		InputSource input =  new RujelInputSource(session,options);
		Transformer transformer = getTransformer(session, 
				(NSDictionary)options.valueForKey("reporter"), input);
        Source src = new StreamSource(new ByteArrayInputStream(data));
    	ByteArrayOutputStream out = new ByteArrayOutputStream();
    	Result res = new StreamResult(out);
      	transformer.transform(src, res);
    	return out.toByteArray();
	}
	
	public void parse(InputSource input) throws IOException, SAXException {
        if (input instanceof RujelInputSource) {
            parse((RujelInputSource)input);
        } else {
            throw new SAXException("Unsupported InputSource specified. "
                    + "Must be a ProjectTeamInputSource");
        }
	}

	public void parse(RujelInputSource in)  throws IOException, SAXException {
        if (handler == null) {
            throw new IllegalStateException("ContentHandler not set");
        }
        
        handler.startDocument();
        handler.prepareAttribute("product", "Rujel");
        String tmp = System.getProperty("RujelVersion");
        if(tmp != null)
        	handler.prepareAttribute("version", tmp);
        Integer eduYear = (Integer)in.ses.valueForKey("eduYear");
        {
        	EOObjectStore os = DataBaseConnector.objectStoreForTag(eduYear.toString());
        	if(os == null)
        		os = EOObjectStoreCoordinator.defaultCoordinator();
			EOEditingContext ec = (EOEditingContext)in.options.valueForKey("ec");
			if(ec == null) {
				ec = new SessionedEditingContext(os, in.ses);
				in.options.takeValueForKey(ec,"ec");
			}
        	if(in.options.valueForKeyPath("reporter.sync") != null) {
        		in.options.takeValueForKey(new SyncGenerator(in.options), "sync");
        	}
        	if (ExtBase.localBaseID() == null)
        		ExtBase.localBase(ec);
        	handler.prepareAttribute("base", ExtBase.localBaseID());
        	tmp = eduYear.toString();
        }
        handler.prepareAttribute("eduYear", tmp);
		handler.startElement("ejdata");
		tmp = SettingsReader.stringForKeyPath("schoolName", null);
		if(tmp != null) {
			handler.prepareAttribute("title", tmp);
			handler.element("school", null);
		}
		
		NSArray groups = prepareGroups(in);

		in.ses.setObjectForKey(in.options,"xmlGeneration");
		NSArray generators = (NSArray)in.ses.valueForKeyPath("modules.xmlGeneration");
		in.ses.removeObjectForKey("xmlGeneration");
		
		useGenerators(generators, null);
		{
			NSDictionary opt = (NSDictionary)in.options.valueForKeyPath(
					"reporter.settings.courses");
			if(opt != null && !Various.boolForObject(opt.valueForKey("active"))) {
				handler.endElement("ejdata");
				handler.endDocument();
				return;
			}
		}
		NSArray courses = (NSArray)in.options.valueForKey("courses");
		if(courses != null)
			groups = null;
		handler.startElement("courses");
		if(courses == null) {
			Enumeration enu = groups.objectEnumerator();
			NSMutableArray grades = new NSMutableArray();
			while (enu.hasMoreElements()) {
				EduGroup gr = (EduGroup) enu.nextElement();
				courses = EOUtilities.objectsMatchingKeyAndValue(gr.editingContext(),
						EduCourse.entityName, "eduGroup", gr);
				processCourses(courses, generators, in);
				if(!grades.containsObject(gr.grade())) {
					grades.addObject(gr.grade());
				}
			}
			//TODO: add groupless grade courses
		} else {
			processCourses(courses, generators, in);
		}
		courses = (NSArray)in.options.valueForKey("extraCourses");
		if(courses instanceof NSMutableArray) {
			EOSortOrdering.sortArrayUsingKeyOrderArray((NSMutableArray)courses, EduCourse.sorter);
			processCourses(courses, generators, in);
		}
		handler.endElement("courses");
		handler.endElement("ejdata");
		handler.endDocument();
//		EOEditingContext ec = (EOEditingContext)in.options.valueForKey("ec");
//		ec.unlock();
//		ec.dispose();
	}
	
	private NSArray prepareGroups(RujelInputSource in) throws SAXException {
		NSArray groups = (NSArray)in.options.valueForKey("eduGroups");
		if(groups == null) {
			EduGroup gr = (EduGroup)in.options.valueForKey("eduGroup");
			if(gr != null)
				groups = new NSArray(gr);
		}
		NSArray students = (NSArray)in.options.valueForKey("students");
		if(students == null) {
			Student stu = (Student)in.options.valueForKey("student");
			if(stu != null) {
				students = new NSArray(stu);
				if(groups == null) {
					EduGroup gr = stu.recentMainEduGroup();
					if(gr != null)
						groups = new NSArray(gr);
				}
			}
		} else if(groups == null) {
			Enumeration enu = students.objectEnumerator();
			NSMutableArray tmp = new NSMutableArray();
			while (enu.hasMoreElements()) {
				Student stu = (Student) enu.nextElement();
				EduGroup gr = stu.recentMainEduGroup();
				if(gr != null && !tmp.containsObject(gr))
					tmp.addObject(gr);
			}
			if(tmp.count() > 1)
				EOSortOrdering.sortArrayUsingKeyOrderArray(tmp, EduGroup.sorter);
			if(tmp.count() > 0)
				groups = tmp;
		}
		if(groups == null) {
			NSTimestamp date = (NSTimestamp)in.ses.valueForKey("today");
			EOEditingContext ec = (EOEditingContext)in.options.valueForKey("ec");
			groups = EduGroup.Lister.listGroups(date, ec);
		}
		{
			NSDictionary opt = (NSDictionary)in.options.valueForKeyPath(
					"reporter.settings.eduGroups");
			if(opt != null && !Various.boolForObject(opt.valueForKey("active")))
				return groups;
		}
		GeneratorModule sync = (GeneratorModule)in.options.valueForKey("sync");
		if(sync != null)
			sync.preload("eduGroup", groups);
		handler.startElement("eduGroups");
		Enumeration enu = groups.objectEnumerator();
		boolean nolist = Various.boolForObject(
				in.options.valueForKeyPath("reporter.settings.eduGroups.nolist"));
		boolean tutors = Various.boolForObject(
				in.options.valueForKeyPath("reporter.settings.eduGroups.tutors"));
		while (enu.hasMoreElements()) {
			EduGroup gr = (EduGroup) enu.nextElement();
			handler.prepareAttribute("id", MyUtility.getID(gr));
			handler.prepareAttribute("name", gr.name());
			handler.prepareAttribute("grade", gr.grade().toString());
			handler.prepareAttribute("title", gr.title());
			if(Various.boolForObject(in.ses.valueForKeyPath("strings.sections.hasSections"))) {
				try {
					Integer sect = (Integer)gr.valueForKey("section");
					if(sect != null)
						handler.prepareAttribute("section", sect.toString());
				} catch (Exception e) {}
			}
			handler.startElement("eduGroup");
			if(sync != null)
				sync.generateFor(gr, handler);
			if(tutors) try {
				NSArray list = (NSArray)gr.valueForKey("tutors");
				if(list != null) {
					for (int i = 0; i < list.count(); i++) {
						EOEnterpriseObject tutor = (EOEnterpriseObject)list.objectAtIndex(i);
						Teacher teacher = (Teacher)tutor.valueForKey("teacher");
						if(teacher != null) {
							handler.prepareAttribute("id", MyUtility.getID(teacher));
							handler.element("teacher",
									Person.Utility.fullName(teacher, true, 2, 1, 1));
						}
					}
				}
			} catch (Exception e) {
				Logger.getLogger("rujel.xml").log(WOLogLevel.WARNING, 
						"Failed to list tutors on group", new Object[]{gr,e});
			}
			if(nolist) {
				handler.prepareEnumAttribute("type", "full");
				handler.endElement("eduGroup");
				continue;
			}
			if(students == null) { // all students
				handler.prepareEnumAttribute("type", "full");
				NSArray list = gr.list();
				if(list != null && list.count() > 0) {
					Enumeration stenu = list.objectEnumerator();
					while (stenu.hasMoreElements()) {
						Student stu = (Student) stenu.nextElement();
						handler.prepareAttribute("id", MyUtility.getID(stu));
//						handler.prepareAttribute("name", Person.Utility.fullName(stu, true, 2, 2, 0));
						handler.element("student",null);
					}
				}
			} else if(students.count() == 0) { // just list groups
				handler.prepareEnumAttribute("type", "mixed");
			} else { // selected students
				handler.prepareEnumAttribute("type", "sub");
				Enumeration stenu = students.objectEnumerator();
				while (stenu.hasMoreElements()) {
					Student stu = (Student) stenu.nextElement();
					if(!gr.isInGroup(stu)) {
						continue;
					}
					handler.prepareAttribute("id", MyUtility.getID(stu));
//					handler.prepareAttribute("name", Person.Utility.fullName(stu, true, 2, 2, 0));
					handler.element("student",null);
				} // students enumeration
			} // selected students
			handler.endElement("eduGroup");
		} // group enumeration
		handler.endElement("eduGroups");
		if(sync != null)
			sync.unload("eduGroup");
		return groups;
	}
	
	private void processCourses(NSArray courses, NSArray generators, RujelInputSource in)
			throws SAXException {
		if(courses == null || courses.count() == 0)
			return;
		Student stu = (Student)in.options.valueForKey("student");
		Enumeration enu = courses.objectEnumerator();
		GeneratorModule sync = (GeneratorModule)in.options.valueForKey("sync");
		if(sync != null)
			sync.preload("course", courses);
		while (enu.hasMoreElements()) {
			EduCourse crs = (EduCourse) enu.nextElement();
			if(crs instanceof BaseCourse) {
				if(stu != null && !((BaseCourse)crs).isInSubgroup(stu))
					continue;
			}
			writeCourse(crs, generators,in);
		}
		if(sync != null)
			sync.unload("course");
	}
	
	private void writeCourse(EduCourse course,NSArray generators, RujelInputSource in)
			throws SAXException {
		handler.prepareAttribute("id", MyUtility.getID(course));
		handler.prepareAttribute("cycle", MyUtility.getID(course.cycle()));
		handler.prepareAttribute("subject", course.cycle().subject());
		handler.startElement("course");
		GeneratorModule sync = (GeneratorModule)in.options.valueForKey("sync");
		if(sync != null)
			sync.generateFor(course, handler);
		Teacher teacher = course.teacher();
		if(teacher != null) {
			handler.prepareAttribute("id", MyUtility.getID(teacher));
			handler.element("teacher", Person.Utility.fullName(teacher, true, 2, 1, 1));
		}
		EduGroup gr = course.eduGroup();
		handler.prepareAttribute("id", (gr==null)?"0":MyUtility.getID(gr));
		handler.prepareAttribute("name", (gr==null)?"":gr.name());
		try {
			if(Various.boolForObject(course.valueForKeyPath("namedFlags.mixedGroup"))) {
				handler.prepareEnumAttribute("type", "mixed");
				gr = null;
			} else if(Various.boolForObject(course.valueForKeyPath("audience.count"))) {
				handler.prepareEnumAttribute("type", "sub");
				gr = null;
			} else {
				handler.prepareEnumAttribute("type", "full");
			}
		} catch (Exception e) {
			handler.prepareEnumAttribute("type", "full");
		}
		handler.startElement("eduGroup");
		if(gr == null && !Various.boolForObject(
				in.options.valueForKeyPath("reporter.settings.eduGroups.nolist"))) {
			NSArray list = course.groupList();
			NSArray students = (NSArray)in.options.valueForKey("students");
			if(list != null && list.count() > 0) {
				Enumeration enu = list.objectEnumerator();
				while (enu.hasMoreElements()) {
					Student stu = (Student) enu.nextElement();
					if(students != null && !students.containsObject(stu))
						continue;
					handler.prepareAttribute("id", MyUtility.getID(stu));
					handler.element("student",null);
				}
			}
		}
		handler.endElement("eduGroup");
		if(course.comment() != null)
			handler.element("comment", course.comment());
		if(!Various.boolForObject(in.options.valueForKeyPath(
				"reporter.generation.omitCourseContent")))
			useGenerators(generators, course);
		handler.endElement("course");
	}
	
	protected void useGenerators(NSArray generators, Object object) throws SAXException {
		if(generators != null && generators.count() > 0) {
			Enumeration enu = generators.objectEnumerator();
			while (enu.hasMoreElements()) {
				GeneratorModule gen = (GeneratorModule) enu.nextElement();
				gen.generateFor(object, handler);
			}
		}
	}
	
	public static class Persdata extends AbstractObjectReader {

		public void parse(InputSource input) throws IOException, SAXException {
	        if (input instanceof RujelInputSource) {
	            parse((RujelInputSource)input);
	        } else {
	            throw new SAXException("Unsupported InputSource specified. "
	                    + "Must be a ProjectTeamInputSource");
	        }
		}

		public void parse(RujelInputSource in)  throws IOException, SAXException {
	        if (handler == null) {
	            throw new IllegalStateException("ContentHandler not set");
	        }
			handler.startDocument();
			handler.prepareAttribute("product", "Rujel");
			String tmp = System.getProperty("RujelVersion");
			if(tmp != null)
				handler.prepareAttribute("version", tmp);
			handler.prepareAttribute("base", ExtBase.localBaseID());
			tmp = in.ses.valueForKey("eduYear").toString();
			handler.prepareAttribute("eduYear", tmp);
			handler.startElement("persdata");
			GeneratorModule sync = (GeneratorModule)in.options.valueForKey("sync");
			Object stu = in.options.valueForKey("student");
			if(stu != null) {
				generateForPersonLink((Student)stu, "student", sync);
			} else {
				stu = (NSArray)in.options.valueForKey("students");
				if(stu != null && ((NSArray)stu).count() > 0) {
					Enumeration enu = ((NSArray)stu).objectEnumerator();
					if(sync != null)
						sync.preload("person", ((NSArray)stu));
					while (enu.hasMoreElements()) {
						generateForPersonLink((Student)enu.nextElement(),"student", sync);
					}
					if(sync != null)
						sync.unload("person");
				} else {
					stu = null;
				}
			}
			if(stu == null) {
				stu = in.options.valueForKey("eduGroup");
				if(stu instanceof EduGroup) {
					NSArray list = ((EduGroup)stu).list();
					if(list != null && list.count() > 0) {
						if(sync != null)
							sync.preload("person", list);
						Enumeration enu = list.objectEnumerator();
						while (enu.hasMoreElements()) {
							generateForPersonLink((Student)enu.nextElement(),"student", sync);
						}
						if(sync != null)
							sync.unload("person");
					}
				}
			}
			NSArray courses = (NSArray)in.options.valueForKey("courses");
			if(courses != null && courses.count() > 0) {
				Enumeration enu = courses.objectEnumerator();
				NSMutableArray teachers = new NSMutableArray();
				while (enu.hasMoreElements()) {
					EduCourse crs = (EduCourse) enu.nextElement();
					Teacher teacher = crs.teacher();
					if(!teachers.containsObject(teacher))
						teachers.addObject(teacher);
				}
				if(teachers.count() > 0) {
					EOSortOrdering.sortArrayUsingKeyOrderArray(teachers, Person.sorter);
					
					if(sync != null)
						sync.preload("person", teachers);
					enu = teachers.objectEnumerator();
					while (enu.hasMoreElements()) {
						Teacher teacher = (Teacher) enu.nextElement();
						generateForPersonLink(teacher, "teacher",sync);
					}
					if(sync != null)
						sync.unload("person");
				}
			}
			handler.endElement("persdata");
			handler.endDocument();
		}
		
		protected void generateForPersonLink(PersonLink plink, String type, GeneratorModule sync)
					throws SAXException {
			handler.prepareEnumAttribute("type", type);
			handler.prepareAttribute("id", MyUtility.getID((EOEnterpriseObject)plink));
			Person pers = plink.person();
			handler.prepareEnumAttribute("sex", (pers.sex())?"male":"female");
			if(plink instanceof Student) {
				Integer absGrade = null;
				try {
					absGrade = (Integer)((Student)plink).valueForKey("absGrade");
				} catch (Exception e) {}
				if(absGrade == null) {
					try {
						absGrade = ((Student)plink).recentMainEduGroup().grade();
					} catch (Exception e) {}
				}
				if(absGrade != null)
					handler.prepareAttribute("absGrade", absGrade.toString());
			}
			handler.startElement("person");
			if(pers != plink && pers instanceof EOEnterpriseObject) {
				handler.startElement("syncdata");
				handler.prepareAttribute("product", "Rujel");
				handler.prepareAttribute("base", ExtBase.localBaseID());
				handler.prepareAttribute("entity", ((EOEnterpriseObject)pers).entityName());
				handler.element("extid", MyUtility.getID((EOEnterpriseObject)pers));
				if(sync != null)
					sync.generateFor(plink, handler);
				handler.endElement("syncdata");
			} else if(sync != null) {
				sync.generateFor(plink, handler);
			}
			handler.prepareEnumAttribute("type", "last");
			handler.element("name", pers.lastName());
			handler.prepareEnumAttribute("type", "first");
			handler.element("name", pers.firstName());
			handler.prepareEnumAttribute("type", "second");
			handler.element("name", pers.secondName());
			if(pers.birthDate() != null) {
				handler.prepareAttribute("type","birth");
				handler.element("date", MyUtility.formatXMLDate(pers.birthDate()));
			}
			handler.endElement("person");
		}
	}
	
	public static class Options extends AbstractObjectReader {

		public void parse(InputSource input) throws IOException, SAXException {
	        if (input instanceof RujelInputSource) {
	            parse((RujelInputSource)input);
	        } else {
	            throw new SAXException("Unsupported InputSource specified. "
	                    + "Must be a ProjectTeamInputSource");
	        }
		}
		
		public void parse(RujelInputSource in)  throws IOException, SAXException {
	        if (handler == null) {
	            throw new IllegalStateException("ContentHandler not set");
	        }
	        NSDictionary settings = null;
	        if(!Various.boolForObject(in.options.valueForKeyPath("reporter.hideSettings")))
	        	settings = (NSDictionary)in.options.valueForKeyPath("reporter.settings");
	        NSDictionary info = (NSDictionary)in.options.valueForKey("info");
	        if((settings == null || settings.count() == 0) && info == null)
	        	return;
			handler.startDocument();
			handler.startElement("options");
			if(settings != null)
				writeDict(settings);
			if(info != null) {
				handler.startElement("info");
				writeDict(info);
				handler.endElement("info");
			}
			Student student = (Student)in.options.valueForKey("student");
			if(student != null) {
				String id = MyUtility.getID(student);
				handler.element("studentID", id);
			}
			handler.endElement("options");
			handler.endDocument();
		}
		
		public void writeDict(NSDictionary dict) throws SAXException {
	        if(dict == null || dict.count() == 0)
	        	return;
			Enumeration enu = dict.keyEnumerator();
			while (enu.hasMoreElements()) {
				String key = (String) enu.nextElement();
				Object value = dict.valueForKey(key);
				if(value instanceof NSDictionary) {
					handler.startElement(key);
					writeDict((NSDictionary)value);
					handler.endElement(key);
				} else {
					handler.element(key, value.toString());
				}
			}
		}
	}
}
