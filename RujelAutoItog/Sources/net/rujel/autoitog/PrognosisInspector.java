// PrognosisInspector.java: Class file for WO Component 'PrognosisInspector'

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

import java.math.BigDecimal;
import java.util.Enumeration;

import net.rujel.base.MyUtility;
import net.rujel.criterial.BorderSet;
import net.rujel.interfaces.EduCourse;
import net.rujel.reusables.Various;
import net.rujel.reusables.WOLogLevel;

import com.webobjects.appserver.*;
import com.webobjects.eoaccess.EOUtilities;
import com.webobjects.eocontrol.EOEditingContext;
import com.webobjects.eocontrol.EOEnterpriseObject;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSDictionary;
import com.webobjects.foundation.NSMutableArray;
import com.webobjects.foundation.NSMutableDictionary;
import com.webobjects.foundation.NSTimestamp;

// Generated by the WOLips Templateengine Plug-in at Oct 26, 2009 2:03:35 PM
public class PrognosisInspector extends com.webobjects.appserver.WOComponent {

    public WOComponent returnPage;
    public AutoItog autoItog;
    public EduCourse course;
    public PrognosesAddOn addOn;
    public NSDictionary calcDict;
    public NSTimestamp fireDate;
    protected NSArray collected;
    public Object item;

	public PrognosisInspector(WOContext context) {
        super(context);
    }
    
	public void setAutoItog(AutoItog ai) {
		autoItog = ai;
		if(ai == null) {
			calcDict = NSDictionary.EmptyDictionary;
			fireDate = null;
			return;
		}
		fireDate = autoItog.fireDate();
		CourseTimeout cto = CourseTimeout.getTimeoutForCourseAndPeriod(course, 
				autoItog.itogContainer());
		if(cto != null)
			fireDate = cto.fireDate();

		String pattern = autoItog.calculatorName();
    	if(pattern != null) { // resolve className to readable title
    		NSArray calculators = (NSArray)session().valueForKeyPath(
    							"strings.RujelAutoItog_AutoItog.calculators");
    		if(calculators != null && calculators.count() > 0) {
    			for (int i = 0; i < calculators.count(); i++) {
    				calcDict = (NSDictionary)calculators.objectAtIndex(i);
    				if(pattern.equals(calcDict.valueForKey("className"))) {
    					return;
    				}
    			}
    		}
    	}
    	calcDict = NSDictionary.EmptyDictionary;
	}

	public String bSetDetials() {
		BorderSet bSet = (BorderSet)valueForKeyPath("autoItog.borderSet");
		if(bSet == null)
			return null;
		StringBuilder buf = new StringBuilder("0% : ");
		buf.append(bSet.zeroValue());
		NSArray borders = bSet.sortedBorders();
		if(borders != null && borders.count() > 0) {
			Enumeration enu = borders.objectEnumerator();
			while (enu.hasMoreElements()) {
				EOEnterpriseObject bd = (EOEnterpriseObject) enu.nextElement();
				BigDecimal value = (BigDecimal)bd.valueForKey("least");
				/*value = value.stripTrailingZeros();
				if(value.scale() < 0)
					value = value.setScale(0);*/
				buf.append('\r').append(MyUtility.formatDecimal(value)).append("% : ");
				buf.append(bd.valueForKey("title"));
			}
		}
		return WOMessage.stringByEscapingHTMLAttributeValue(buf.toString());
	}
	
	public NSArray collected() {
		if(collected != null)
			return collected;
//		collected = autoItog.relatedForCourse(course);
		Calculator calc = autoItog.calculator();
		if(calc == null)
			return null;
		collected = autoItog.relKeysForCourse(course);
		NSMutableArray related = (collected == null || collected.count() == 0)?
				new NSMutableArray():
			((NSArray)collected.valueForKey("relKey")).mutableClone();
		collected = calc.collectRelated(course, autoItog,
				!autoItog.namedFlags().flagForKey("runningTotal"), false);
		NSMutableArray result = new NSMutableArray();
		if(collected != null && collected.count() > 0) {
			Enumeration enu = collected.objectEnumerator();
			while (enu.hasMoreElements()) {
				Object object = enu.nextElement();
				NSMutableDictionary dict = calc.describeObject(object);
				dict.takeValueForKey(object, "object");
				Integer relKey = calc.relKeyForObject(object);
				dict.takeValueForKey(relKey, "relKey");
				if(related.removeObject(relKey)) {
					dict.takeValueForKey(Boolean.TRUE, "related");
					dict.takeValueForKey("gerade", "styleClass");
				} else {
					dict.takeValueForKey("grey", "styleClass");
				}
				result.addObject(dict);
			}
		}
		if(related.count() > 0) {
			Enumeration enu = related.objectEnumerator();
			EOEditingContext ec = autoItog.editingContext();
			String entName = calc.reliesOnEntity();
			while (enu.hasMoreElements()) {
				Integer relKey = (Integer)enu.nextElement();
				Object object = null;
				try {
					object = EOUtilities.objectWithPrimaryKeyValue(ec, entName, relKey);
				} catch (RuntimeException e) {
					object = null;
				}
				if(course != calc.courseForObject(object))
					object = null;
				NSMutableDictionary dict = calc.describeObject(object);
				dict.takeValueForKey(object, "object");
				dict.takeValueForKey(relKey, "relKey");
				dict.takeValueForKey(Boolean.valueOf(object != null), "related");
				dict.takeValueForKey("warning", "styleClass");
				result.addObject(dict);
			}
		}
		collected = result;
		return collected;
	}
	
	public WOActionResults save() {
		returnPage.ensureAwakeInContext(context());
		if(collected.count() == 0)
			return returnPage;
		Enumeration enu = collected.objectEnumerator();
		EOEditingContext ec = autoItog.editingContext();
		ec.lock();
		try {
			int count = 0;
			while (enu.hasMoreElements()) {
				NSMutableDictionary dict = (NSMutableDictionary) enu.nextElement();
				Object object = dict.valueForKey("relKey");
				if(object == null) 
					object = dict.valueForKey("object");
				if(Various.boolForObject(dict.valueForKey("related"))) {
					autoItog.addRelatedObject(object, course);
					count++;
				} else {
					autoItog.removeRelatedObject(object, course);
				}
			}
			if(count == 0) {
				NSArray found = Prognosis.prognosesArrayForCourseAndPeriod(course, 
						autoItog.itogContainer(), false);
				if(found.count() > 0) {
					enu = found.objectEnumerator();
					while (enu.hasMoreElements()) {
						EOEnterpriseObject progn = (EOEnterpriseObject) enu.nextElement();
						ec.deleteObject(progn);
					}
				}
			}
			if(ec.hasChanges())
				ec.saveChanges();
			if(count > 0) {
				addOn.calculate();
				AutoItogModule.logger.log(WOLogLevel.EDITING,"Changed related list",
						new Object[] {session(),autoItog,course});
			} else {
				addOn.reset();
				AutoItogModule.logger.log(WOLogLevel.EDITING,
						"Removed all objects from autoItog, prognoses deleted",
						new Object[] {session(),autoItog, course});
			}
		} catch (Exception e) {
			session().takeValueForKey(e.getMessage(), "message");
			AutoItogModule.logger.log(WOLogLevel.WARNING,"Error saving related objects",
					new Object[] {session(),autoItog,e});
			if(ec.hasChanges())
				ec.revert();
		} finally {
			ec.unlock();
		}
		return returnPage;
	}
}