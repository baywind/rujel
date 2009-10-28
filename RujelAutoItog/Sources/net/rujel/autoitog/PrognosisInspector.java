package net.rujel.autoitog;

import java.math.BigDecimal;
import java.util.Enumeration;

import net.rujel.criterial.BorderSet;
import net.rujel.interfaces.EduCourse;
import net.rujel.reusables.Various;
import net.rujel.reusables.WOLogLevel;

import com.webobjects.appserver.*;
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
//    public Calculator calculator;
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
				value = value.stripTrailingZeros();
				if(value.scale() < 0)
					value = value.setScale(0);
				buf.append('\r').append(value).append("% : ");
				buf.append(bd.valueForKey("title"));
			}
		}
		return WOMessage.stringByEscapingHTMLAttributeValue(buf.toString());
	}
	
	public NSArray collected() {
		if(collected != null)
			return collected;
		collected = autoItog.relatedForCourse(course);
		NSMutableArray related = (collected == null)?new NSMutableArray():
			collected.mutableClone();
		Calculator calc = autoItog.calculator();
		collected = calc.collectRelated(course, autoItog,
				!autoItog.namedFlags().flagForKey("runningTotal"), false);
		NSMutableArray result = new NSMutableArray();
		if(collected != null && collected.count() > 0) {
			Enumeration enu = collected.objectEnumerator();
			while (enu.hasMoreElements()) {
				Object object = enu.nextElement();
				NSMutableDictionary dict = calc.describeObject(object);
				dict.takeValueForKey(object, "object");
				if(related.removeObject(object)) {
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
			while (enu.hasMoreElements()) {
				Object object = enu.nextElement();
				NSMutableDictionary dict = calc.describeObject(object);
				dict.takeValueForKey(object, "object");
				dict.takeValueForKey(Boolean.TRUE, "related");
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
			while (enu.hasMoreElements()) {
				NSMutableDictionary dict = (NSMutableDictionary) enu.nextElement();
				Object object = dict.valueForKey("object");
				if(Various.boolForObject(dict.valueForKey("related"))) {
					autoItog.addRelatedObject(object, course);
				} else {
					autoItog.removeRelatedObject(object, course);
				}
			}
			if(ec.hasChanges())
				ec.saveChanges();
			addOn.calculate();
			AutoItogModule.logger.log(WOLogLevel.UNOWNED_EDITING,"Changed related list",
					new Object[] {session(),autoItog});
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