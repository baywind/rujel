// AutoItogPresenter.java

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

package net.rujel.autoitog;

import java.text.SimpleDateFormat;
import java.util.Enumeration;

import net.rujel.base.SettingsBase;
import net.rujel.eduresults.ItogContainer;
import net.rujel.eduresults.ItogMark;
import net.rujel.interfaces.EduCourse;
import net.rujel.reusables.ExtDynamicElement;
import net.rujel.reusables.NamedFlags;
import net.rujel.reusables.SettingsReader;
import net.rujel.reusables.Various;
import net.rujel.reusables.WOLogLevel;

import com.webobjects.appserver.*;
import com.webobjects.eocontrol.EOEditingContext;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSDictionary;

// Generated by the WOLips Templateengine Plug-in at Sep 11, 2009 11:25:13 AM
public class AutoItogPresenter extends ExtDynamicElement {

	public AutoItogPresenter(String name, NSDictionary associations, WOElement template) {
		super(name, associations, template);
	}

	public void appendToResponse(WOResponse aResponse, WOContext aContext) {
		ItogContainer itog = (ItogContainer)valueForBinding("value",aContext);
		if(itog == null) {
			aResponse.appendContentString((String)aContext.session().valueForKeyPath(
					"strings.RujelAutoItog_AutoItog.properties.AutoItog.this"));
			NamedFlags access = (NamedFlags)aContext.session().valueForKeyPath(
					"readAccess.FLAGS.AutoItog");
			if(access.intValue() < 16 || valueForBinding("itogsList", aContext) == null)
				return;
			aResponse.appendContentString(" <span onclick = \"if(tryLoad())window.location = '");
			aResponse.appendContentString(aContext.componentActionURL());
			aResponse.appendContentString(
					"';\" onmouseover = \"dim(this);\" onmouseout = \"unDim(this)\">");
			aResponse.appendContentString("#</span>");
			return;
		}
		String listName = (String)valueForBinding("listName", aContext);
		AutoItog autoItog = AutoItog.forListName(listName, itog);
		aResponse.appendContentString("<span");
		if(autoItog != null || Various.boolForObject(aContext.session().valueForKeyPath(
				"readAccess.create.AutoItog"))) {
			aResponse.appendContentString(" onclick = \"ajaxPopupAction('");
			aResponse.appendContentString(aContext.componentActionURL());
			aResponse.appendContentString(
				"',event);\" onmouseover = \"dim(this);\" onmouseout = \"unDim(this)\"");
		}
		if(autoItog == null) {
			aResponse.appendContentString(" style = \"font-style:italic;\"");
			aResponse.appendContentCharacter('>');
			aResponse.appendContentString((String)aContext.session().valueForKeyPath(
					"strings.RujelAutoItog_AutoItog.none"));
//			return;
		} else {
			boolean disabled = (autoItog.namedFlags().flagForKey("inactive"));
			if(disabled)
				aResponse.appendContentString(" class = \"dimtext\"");
			aResponse.appendContentCharacter('>');
			String pattern = SettingsReader.stringForKeyPath("ui.shortDateFormat","MMM-dd");
			SimpleDateFormat format = new SimpleDateFormat(pattern);
			aResponse.appendContentString(format.format(autoItog.fireDate()));
			aResponse.appendContentCharacter(' ');
			format = new SimpleDateFormat("HH:mm");
			aResponse.appendContentString(format.format(autoItog.fireTime()));
			aResponse.appendContentCharacter(':');
			aResponse.appendContentCharacter(' ');
			pattern = autoItog.calculatorName();
			if(pattern != null) { // resolve className to readable title
				NSArray calculators = (NSArray)aContext.session().valueForKeyPath(
					"strings.RujelAutoItog_AutoItog.calculators");
				for (int i = 0; i < calculators.count(); i++) {
					NSDictionary calc = (NSDictionary)calculators.objectAtIndex(i);
					if(pattern.equals(calc.valueForKey("className"))) {
						aResponse.appendContentString((String)calc.valueForKey("title"));
						break;
					}
				}
			}
			if(autoItog.namedFlags().flagForKey("manual")) {
				if(pattern != null)
					aResponse.appendContentCharacter('/');
				aResponse.appendContentString((String)aContext.session().valueForKeyPath(
					"strings.RujelAutoItog_AutoItog.manual"));
			}
		} // autoItog != null
		aResponse.appendContentString("</span>");
	}

	protected WOActionResults action(WOContext aContext) {
		ItogContainer itog = (ItogContainer)valueForBinding("value",aContext);
		if(itog == null) {
			try {
				updateDates(aContext);
			} catch (Exception e) {
				aContext.session().takeValueForKey(e.getMessage(), "message");
				AutoItogEditor.logger.log(WOLogLevel.WARNING, "Failed updating dates",
						new Object[] {aContext.session(),e});
			}
			return super.action(aContext);
		}
		WOComponent popup = WOApplication.application().pageWithName(
				"AutoItogEditor", aContext);
		popup.takeValueForKey(aContext.page(), "returnPage");
		String listName = (String)valueForBinding("listName", aContext);
		popup.takeValueForKey(listName, "listName");
		popup.takeValueForKey(itog, "itog");
		AutoItog autoItog = AutoItog.forListName(listName, itog);
//		if(autoItog != null) {
			popup.takeValueForKey(autoItog, "autoItog");
//		}
		return popup;
	}
	
	private void updateDates(WOContext aContext) {
		NSArray itogsList = (NSArray)valueForBinding("itogsList", aContext);
//			EOUtilities.objectsMatchingKeyAndValue(ec, 
//				ItogContainer.ENTITY_NAME, ItogContainer.EDU_YEAR_KEY, eduYear);
		if(itogsList == null || itogsList.count() == 0)
			return;
		String listName = (String)valueForBinding("listName", aContext);
		EOEditingContext ec = (EOEditingContext)valueForBinding("ec", aContext);
		Integer eduYear = (Integer)aContext.session().valueForKey("eduYear");
		SettingsBase base = SettingsBase.baseForKey(ItogMark.ENTITY_NAME, ec, false);
		NSArray courses = base.coursesForSetting(listName, null,eduYear);
		if(courses == null || courses.count() == 0)
			return;
		Enumeration enu = courses.objectEnumerator();
		while (enu.hasMoreElements()) {
			EduCourse course = (EduCourse) enu.nextElement();
			for (int i = 0; i < itogsList.count(); i++) {
				ItogContainer itog = (ItogContainer)itogsList.objectAtIndex(i);
				AutoItog ai = AutoItog.forListName(listName, itog);
				NSArray prognoses = Prognosis.prognosesArrayForCourseAndPeriod(course, ai);
				if(prognoses == null || prognoses.count() == 0)
					continue;
				CourseTimeout cto = CourseTimeout.getTimeoutForCourseAndPeriod(course, itog);
				prognoses.takeValueForKey(cto, "updateWithCourseTimeout");
				ec.saveChanges();
			}
		}
	}
}