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

package net.rujel.base;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Enumeration;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerException;
import javax.xml.transform.Source;
import javax.xml.transform.Result;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.webobjects.appserver.WOApplication;
import com.webobjects.appserver.WOSession;
import com.webobjects.eocontrol.EOEditingContext;
import com.webobjects.eocontrol.EOEnterpriseObject;
import com.webobjects.eocontrol.EOGlobalID;
import com.webobjects.eocontrol.EOKeyGlobalID;
import com.webobjects.eocontrol.EOObjectStore;
import com.webobjects.eocontrol.EOObjectStoreCoordinator;
import com.webobjects.eocontrol.EOSortOrdering;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSDictionary;
import com.webobjects.foundation.NSMutableArray;
import com.webobjects.foundation.NSTimestamp;

import net.rujel.interfaces.EduGroup;
import net.rujel.interfaces.Person;
import net.rujel.interfaces.Student;
import net.rujel.reusables.DataBaseConnector;
import net.rujel.reusables.SessionedEditingContext;
import net.rujel.reusables.SettingsReader;
import net.rujel.reusables.xml.AbstractObjectReader;

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
	
	public static byte[] generate(WOSession session, NSDictionary options)
									throws IOException, TransformerException {
		TransformerFactory factory = TransformerFactory.newInstance();
        Transformer transformer = factory.newTransformer();
        
        Source src = new SAXSource(new XMLGenerator(),
                new RujelInputSource(session,options));

    	ByteArrayOutputStream out = new ByteArrayOutputStream();
    	Result res = new StreamResult(out);
    	transformer.setOutputProperty("indent", "yes");
    	transformer.setOutputProperty("{http://xml.apache.org/xalan}indent-amount", "4");
      	transformer.transform(src, res);
    	return out.toByteArray();
	}
	
	@Override
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
		tmp = WOApplication.application().host() +
				WOApplication.application().valueForKey("urlPrefix");
		handler.prepareAttribute("base", tmp);
		tmp = in.ses.valueForKey("eduYear").toString();
		handler.prepareAttribute("eduYear", tmp);
		handler.startElement("ejdata");
		tmp = SettingsReader.stringForKeyPath("schoolName", null);
		if(tmp != null) {
			handler.prepareAttribute("title", tmp);
			handler.element("school", null);
		}
		prepareGroups(in);
		
		handler.endElement("ejdata");
		handler.endDocument();
	}
	
	private void prepareGroups(RujelInputSource in) throws SAXException {
		NSArray eduGroups = (NSArray)in.options.valueForKey("eduGroups");
		if(eduGroups == null) {
			EduGroup gr = (EduGroup)in.options.valueForKey("eduGroup");
			if(gr != null)
				eduGroups = new NSArray(gr);
		}
		NSArray students = (NSArray)in.options.valueForKey("students");
		if(students == null) {
			Student stu = (Student)in.options.valueForKey("student");
			if(stu != null) {
				students = new NSArray(stu);
				if(eduGroups == null) {
					EduGroup gr = stu.recentMainEduGroup();
					if(gr != null)
						eduGroups = new NSArray(gr);
				}
			}
		} else if(eduGroups == null) {
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
				eduGroups = tmp;
		}
		if(eduGroups == null) {
			NSTimestamp date = (NSTimestamp)in.ses.valueForKey("today");
			Integer eduYear = (Integer)in.ses.valueForKey("eduYear");
			EOObjectStore os = DataBaseConnector.objectStoreForTag(eduYear.toString());
			if(os == null)
				os = EOObjectStoreCoordinator.defaultCoordinator();
			EOEditingContext ec = new SessionedEditingContext(os, in.ses);
			eduGroups = EduGroup.Lister.listGroups(date, ec);
		}
		handler.startElement("eduGroups");
		Enumeration enu = eduGroups.objectEnumerator();
		while (enu.hasMoreElements()) {
			EduGroup gr = (EduGroup) enu.nextElement();
			handler.prepareAttribute("id", getID(gr));
			handler.prepareAttribute("name", gr.name());
			handler.prepareAttribute("grade", gr.grade().toString());
			handler.prepareAttribute("title", gr.title());
			try {
				Integer abs = (Integer)gr.valueForKey("absGrade");
				if(abs != null)
					handler.prepareAttribute("absGrade", abs.toString());
			} catch (Exception e) {}
			if(students == null) { // all students
				handler.prepareAttribute("type", "NMTOKEN", "full");
				handler.startElement("eduGroup");
				NSArray list = gr.list();
				if(list != null && list.count() > 0) {
					Enumeration stenu = list.objectEnumerator();
					while (stenu.hasMoreElements()) {
						Student stu = (Student) stenu.nextElement();
						handler.prepareAttribute("id", getID(stu));
						handler.element("student", Person.Utility.fullName(stu, true, 2, 1, 0));
					}
				}
			} else { // selected students
				handler.prepareAttribute("type", "NMTOKEN", "sub");
				boolean skip = true;
				Enumeration stenu = students.objectEnumerator();
				while (stenu.hasMoreElements()) {
					Student stu = (Student) stenu.nextElement();
					if(gr.isInGroup(stu)) {
						if(skip) {
							handler.startElement("eduGroup");
							skip = false;
						}
					} else {
						continue;
					}
					handler.prepareAttribute("id", getID(stu));
					handler.element("student", Person.Utility.fullName(stu, true, 2, 1, 0));
				} // students enumeration
				if(skip) {
					handler.dropAttributes();
					continue;
				}
			} // selected students
			handler.endElement("eduGroup");
		} // groupe enumeration
		handler.endElement("eduGroups");
	}
	
	public static String getID (EOEnterpriseObject eo) {
		EOEditingContext ec = eo.editingContext();
		if(ec == null)
			return null;
		EOGlobalID gid = ec.globalIDForObject(eo);
		if(gid.isTemporary())
			return null;
		EOKeyGlobalID kGid = (EOKeyGlobalID)gid;
		if(kGid.keyCount() > 1) {
			return kGid.keyValuesArray().toString();
//			NSDictionary pKey = com.webobjects.eoaccess.EOUtilities.primaryKeyForObject(ec, eo);
//			return pKey.toString();
		}
		return kGid.keyValues()[0].toString();
	}
}
