// Parameter.java: Class file for WO Component 'Parameter'

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

package net.rujel.ui;

import java.text.Format;
import java.util.Enumeration;

import net.rujel.base.MyUtility;
import net.rujel.reusables.DisplayAny;
import net.rujel.reusables.Various;

import com.webobjects.appserver.*;
import com.webobjects.eocontrol.EOAndQualifier;
import com.webobjects.eocontrol.EOEnterpriseObject;
import com.webobjects.eocontrol.EOKeyValueQualifier;
import com.webobjects.eocontrol.EOOrQualifier;
import com.webobjects.eocontrol.EOQualifier;
import com.webobjects.foundation.*;

// Generated by the WOLips Templateengine Plug-in at Apr 29, 2009 10:31:19 AM
public class Parameter extends com.webobjects.appserver.WOComponent {

	protected NSKeyValueCodingAdditions _itemDict;
	protected NSKeyValueCoding _paramsDict;
    protected String _attrib;
//    protected String _attrParam;
    public Object item;

    public NSKeyValueCodingAdditions valueOf = new DisplayAny.ValueReader(this);
	public Integer index;
	
	public Parameter(WOContext context) {
        super(context);
    }
	
	public NSKeyValueCoding paramsDict() {
		if(_paramsDict == null)
			_paramsDict = (NSKeyValueCoding)valueForBinding("paramsDict");
		return _paramsDict;
	}
	
	public NSKeyValueCodingAdditions itemDict() {
		if(_itemDict == null)
			_itemDict = (NSKeyValueCodingAdditions)valueForBinding("itemDict");
		return _itemDict;
	}
	
	public static String attribute(NSKeyValueCoding dict) {
		String attrib = (String)dict.valueForKey("attributeParam");
		if(attrib == null)
			attrib = (String)dict.valueForKey("attribute");
		return attrib;
	}
    
	public String attribute() {
		if(_attrib != null)
			return _attrib;
		_attrib = attribute(itemDict());
		if(_attrib != null)
			return _attrib;
		_attrib = (String)valueForBinding("attribute");
		return _attrib;
	}

	public String paramStyle() {
		if(value() == null)
			return "font-style:italic;";
		return null;
	}
	
    public Object value() {
    	boolean secondSelector = (itemDict().valueForKey("secondSelector") != null);
    	String attribute = attribute();
    	Object value = paramsDict().valueForKey(attribute);
    	if(value == NullValue)
    		return null;
    	if(value != null) {
    		if(Various.boolForObject(itemDict().valueForKey("or")) && item != null)
    			return item;
    		return value;
    	} else if(Various.boolForObject(itemDict().valueForKey("range"))) {
    		StringBuilder buf = new StringBuilder();
    		value = paramsDict().valueForKey("min_" + attribute);
    		if(value != null)
    			buf.append(value);
    		value = paramsDict().valueForKey("max_" + attribute);
    		if(value == null) {
    			if(buf.length() > 0)
    				buf.insert(0, "&ge; ");
    		} else {
    			buf.append((buf.length() > 0)?" ... ":"&le; ").append(value);
    		}
    		if(buf.length() > 0)
    			return buf.toString();
    	} else if(secondSelector) {
    		attribute = "min_" + attribute;
    		value = paramsDict().valueForKey(attribute);
    		if(value != null)
        		return (value == NullValue)?null:value;
    	}
    	value = valueOf.valueForKeyPath("paramsDict.itemDict.default" + 
    			((secondSelector)?"Min":"Value"));
    	if(value != null)
    		paramsDict().takeValueForKey(value, attribute);
    	return value;
    }
    
    public void setValue(Object value) {
    	if(value == null)
    		value = NullValue;
    	boolean secondSelector = (itemDict().valueForKey("secondSelector") != null);
    	String attribute = attribute();
    	if(secondSelector)
    		attribute = "min_" + attribute;
    	paramsDict().takeValueForKey(value, attribute);
    }
    
    public Object secondValue() {
    	String attribute = "max_" + attribute();
    	Object value = paramsDict().valueForKey(attribute);
    	if(value != null)
    		return (value == NullValue)?null:value;
    	value = valueOf.valueForKeyPath("paramsDict.itemDict.defaultMax");
    	paramsDict().takeValueForKey((value==null)?NullValue:value, attribute);
    	return value;    	
    }

    public void setSecondValue(Object value) {
    	if(value == null)
    		value = NullValue;
    	String attribute = "max_" + attribute();
    	paramsDict().takeValueForKey(value, attribute);
    }

    public Boolean showField() {
		if(itemDict().valueForKey("attribute") == null)
			return Boolean.FALSE;
		if(itemDict().valueForKey("select") != null)
			return Boolean.FALSE;
		if(_itemDict.valueForKey("popup") != null)
			return Boolean.FALSE;
		if(_itemDict.valueForKey("rows") != null)
			return Boolean.FALSE;
		return Boolean.TRUE;
	}
	
	public String onkeypress() {
		String selector = (String)itemDict().valueForKey("qualifierSelector");
		if(selector != null && 
				(selector.equals("like") || selector.equals("caseInsensitiveLike")))
			return null;
		if(itemDict().valueForKey("formatter") instanceof Boolean)
			return null;
		return "return isNumberInput(event,true);";
	}
	
	public Format formatter() {
		Object result = valueOf.valueForKeyPath("paramsDict.itemDict.formatter");
		if(result == null)
			return null;
		if(result instanceof Format)
			return (Format)result;
		if(result.equals("date"))
			return MyUtility.dateFormat();
		if(result instanceof String)
			return new NSNumberFormatter((String)result);
		return null;
	}
	
	public String sign() {
		String selector = (String)itemDict().valueForKey("qualifierSelector");
		if(selector == null || selector.equals(">="))
			return "&le;";
		else if (selector.equals(">"))
			return "&lt;";
		return WOMessage.stringByEscapingHTMLString(selector); 
	}
	public String sign2() {
		String selector = (String)itemDict().valueForKey("secondSelector");
		if(selector == null || selector.equals("<="))
			return "&le;";
		else if (selector.equals("<"))
			return "&lt;";
		return WOMessage.stringByEscapingHTMLString(selector); 
	}
	
	public WOActionResults selectorPopup() {
		if(canSetValueForBinding("editor")) {
			if(Various.boolForObject(itemDict().valueForKey("or")) &&
					index != null && !index.equals(itemDict().valueForKey("selection"))) { 
				itemDict().takeValueForKey(index, "selection");
				if(itemDict() != valueForBinding("editor"))
					setValueForBinding(itemDict(), "editor");
			} else 
			if(itemDict() == valueForBinding("editor")) {
				setValueForBinding(null, "editor");
			} else {
				setValueForBinding(itemDict(), "editor");
			}
			return null;
		}
		WOComponent selector = pageWithName("SelectorPopup");
		selector.takeValueForKey(context().page(), "returnPage");
		selector.takeValueForKey("params." + attribute(), "resultPath");
		Object value = (Various.boolForObject(itemDict().valueForKey("or")))? item : value();
		selector.takeValueForKey(value, "value");
		NSMutableDictionary dict = (NSMutableDictionary)itemDict().valueForKey("popup");
		dict.takeValueForKeyPath(valueForBinding("editingContext"),
				"presenterBindings.editingContext");
		selector.takeValueForKey(dict, "dict");
		return selector;
	}
	
	public String onclick() {
		Boolean ajax = (Boolean)valueForBinding("useAjax");
		if(ajax != null && !ajax.booleanValue())
			return null;
		return "return ajaxPost(this);";
	}
	
	public void clearParam() {
		paramsDict().takeValueForKey(null, attribute());
	}
	    
	public void reset() {
		_paramsDict = null;
		_itemDict = null;
		_attrib = null;
		index = new Integer(-1);
		item = null;
//		_attrParam = null;
		super.reset();
	}
	
	public boolean isStateless() {
		return true;
	}
	
	public boolean synchronizesVariablesWithBindings() {
        return false;
	}
	
	public static EOQualifier qualForParam(
			NSKeyValueCoding itemDict, NSKeyValueCoding params,WOComponent page) {
		if(itemDict == null)
			return null;
		String selectorString = (String)itemDict.valueForKey("qualifierFormat");
		if (selectorString != null) {
			NSArray args = (NSArray) itemDict.valueForKey("args");
			if (args != null && args.count() > 0) {
				Enumeration enu = args.objectEnumerator();
				args = new NSMutableArray();
				while (enu.hasMoreElements()) {
					Object arg = enu.nextElement();
					Object param = DisplayAny.ValueReader.evaluateValue(arg,
							"params", page);
					if (param == null)
						param = NullValue;
					((NSMutableArray) args).addObject(param);
				}
			}
			EOQualifier qual = EOQualifier.qualifierWithQualifierFormat(
					selectorString, args);
			return qual;
		}
		if(params == null)
			return null;
		String secondSelector = (String)itemDict.valueForKey("secondSelector");
		selectorString = (String)itemDict.valueForKey("qualifierSelector");
		String attrib = (String)itemDict.valueForKey("attribute");
		String attrParam = (String)itemDict.valueForKey("attributeParam");
		boolean condFormat = Various.boolForObject(itemDict.valueForKey("condFormat"));
		if(attrParam == null)
			attrParam = attrib;
		if(secondSelector != null) {
			NSSelector sel = (selectorString == null)?
						EOQualifier.QualifierOperatorGreaterThanOrEqualTo:
							EOQualifier.operatorSelectorForString(selectorString);;
				
			Object min = params.valueForKey("min_" + attrParam);
			Object max = params.valueForKey("max_" + attrParam);
			NSMutableArray quals = new NSMutableArray();
			if(min != null && min != NullValue) {
				quals.addObject(new EOKeyValueQualifier(attrib,sel,min));
			}
			if(max != null && max != NullValue) {
				sel = EOQualifier.operatorSelectorForString(secondSelector);
				quals.addObject(new EOKeyValueQualifier(attrib,sel,max));
			}
			switch (quals.count()) {
			case 0:
				return null;
			case 1:
				return (EOQualifier)quals.objectAtIndex(0);
			default:
				return new EOAndQualifier(quals);
			}
		} else if(Various.boolForObject(itemDict.valueForKey("or"))) {
			NSArray list = (NSArray)params.valueForKey(attrParam);
			if(list == null || list.count() == 0)
				return null;
			NSMutableArray quals = new NSMutableArray();
			Enumeration en = list.objectEnumerator();
			while (en.hasMoreElements()) {
				Object val = en.nextElement();
				if(condFormat)
					attrib = condFormat("attribute", val, itemDict);
				quals.addObject(new EOKeyValueQualifier(attrib,
						EOQualifier.QualifierOperatorEqual,val));
			}
			if(quals.count() == 1)
				return (EOQualifier)quals.objectAtIndex(0);
			return new EOOrQualifier(quals);
		} else {
			NSSelector selector = EOQualifier.QualifierOperatorEqual;
			if(selectorString != null)
				selector = EOQualifier.operatorSelectorForString(selectorString);
			Object value = params.valueForKey(attrParam);
			if(value == null || value == NullValue) {
				if(Various.boolForObject(itemDict.valueForKey("respectNull")))
					return new EOKeyValueQualifier(attrib,selector,NullValue);
				else
					return null;
			}
			if(condFormat)
				attrib = condFormat("attribute",value,itemDict);
			return new EOKeyValueQualifier(attrib,selector,value);
		}
	}
	
	private static String condFormat(String param, Object value, NSKeyValueCoding dict) {
		String path = null;
		if(value instanceof EOEnterpriseObject) {
			path = ((EOEnterpriseObject)value).entityName();
		} else {
			path = value.getClass().getName();
			int dot = path.lastIndexOf('.');
			if(dot > 0)
				path = path.substring(dot +1);
		}
		path = (String)dict.valueForKey(param + path);
		if(path == null)
			path = (String)dict.valueForKey(param);
		return path;
	}

	public String paramClass() {
		if(itemDict() == valueForBinding("editor")) {
			if(Various.boolForObject(itemDict().valueForKey("or"))) {
				Number sel = (Number)itemDict().valueForKey("selection");
				if((sel==null)?index==null:sel.equals(index))
					return "selection";
				else
					return "green";
			}
			return "selection";
		}
		return null;
	}
	
	public String plusClass() {
		if(itemDict() == valueForBinding("editor")) {
			NSMutableArray list = (NSMutableArray)value();
			if(list == null || list.count() == 0)
				return "selection";
			Number sel = (Number)itemDict().valueForKey("selection");
			if(sel != null && (sel.intValue() >= list.count() || sel.intValue() < 0))
					return "selection";
		}
		return "green";
	}
	
	public WOActionResults addValue() {
		index = new Integer(-1);
		return selectorPopup();
	}
	
	public WOActionResults deleteValue() {
		NSMutableArray values =  (NSMutableArray)paramsDict().valueForKey(attribute());
		values.removeObject(value());
		return null;
	}
}