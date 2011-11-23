// EduPlanXML.java

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

package net.rujel.eduplan;

import java.util.Enumeration;

import org.xml.sax.SAXException;

import com.webobjects.appserver.WOApplication;
import com.webobjects.eocontrol.EOEnterpriseObject;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSDictionary;

import net.rujel.base.XMLGenerator;
import net.rujel.interfaces.EduGroup;
import net.rujel.reusables.Various;
import net.rujel.reusables.xml.EasyGenerationContentHandlerProxy;
import net.rujel.reusables.xml.GeneratorModule;

public class EduPlanXML extends GeneratorModule {

	public EduPlanXML(NSDictionary options) {
		super(options);
	}

	@Override
	public void generateFor(Object object,
			EasyGenerationContentHandlerProxy handler) throws SAXException {
		if(!handler.recentElement().equals("ejdata"))
			return;
		{
			NSDictionary opt = (NSDictionary)settings.valueForKeyPath("reporter.settings");
			if(opt != null && opt.valueForKey("eduPlan") == null)
				return;
		}
		EduGroup gr = (EduGroup)settings.valueForKey("eduGroup");
		handler.startElement("eduPlan");
		if(gr == null) {
			
		} else {
			NSArray cycles = PlanCycle.cyclesForEduGroup(gr);
			if(cycles == null || cycles.count() == 0)
				return;
			Enumeration enu = cycles.objectEnumerator();
			Subject subj = null;
			while (enu.hasMoreElements()) {
				PlanCycle cycle = (PlanCycle) enu.nextElement();
				if(subj != cycle.subjectEO()) {
					if(subj != null)
						handler.endElement("subject");
					subj = cycle.subjectEO();
					handler.prepareAttribute("title", subj.subject());
					handler.startElement("subject");
					if(subj.fullName() != null)
						handler.element("content", subj.fullName());
					handler.prepareAttribute("key", "area");
					handler.element("param", (String)subj.valueForKeyPath("area.areaName"));
				}
				handler.prepareAttribute("id", XMLGenerator.getID(cycle));
				handler.prepareAttribute("grade", cycle.grade().toString());
				if(Various.boolForObject(WOApplication.application().valueForKeyPath(
						"strings.sections.hasSections")))
					handler.prepareAttribute("section", cycle.section().toString());
				handler.startElement("cycle");
				EOEnterpriseObject hours = cycle.planHours(gr, false);
				if(hours != null) {
					Integer hrs = (Integer)hours.valueForKey("weeklyHours");
					if(hrs == null) {
						handler.prepareEnumAttribute("type", "total");
						hrs = (Integer)hours.valueForKey("totalHours");
					} else {
						handler.prepareEnumAttribute("type", "weekly");
					}
					if(hrs == null)
						handler.dropAttributes();
					else
						handler.element("hours", hrs.toString());
				}
				handler.endElement("cycle");
			} // cycles enumeration
			if(subj != null)
				handler.endElement("subject");
		}
		handler.endElement("eduPlan");
	}

}
