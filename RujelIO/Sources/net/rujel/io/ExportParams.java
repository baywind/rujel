// ExportParams.java

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

import com.webobjects.appserver.WOActionResults;
import com.webobjects.appserver.WOComponent;
import com.webobjects.appserver.WOContext;
import com.webobjects.appserver.WOMessage;
import com.webobjects.appserver.WOResponse;
import com.webobjects.eocontrol.EOEditingContext;
import com.webobjects.eocontrol.EOEnterpriseObject;
import com.webobjects.eocontrol.EOFetchSpecification;
import com.webobjects.eocontrol.EOQualifier;
import com.webobjects.eocontrol.EOSortOrdering;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSDictionary;
import com.webobjects.foundation.NSKeyValueCoding;
import com.webobjects.foundation.NSKeyValueCodingAdditions;
import com.webobjects.foundation.NSMutableArray;
import com.webobjects.foundation.NSMutableDictionary;

import net.rujel.base.MyUtility;
import net.rujel.reusables.DisplayAny;
import net.rujel.reusables.SessionedEditingContext;
import net.rujel.reusables.Various;
import net.rujel.reusables.WOLogFormatter;

public class ExportParams extends WOComponent {
	
	public WOComponent returnPage;
	public NSDictionary plist;
	public NSMutableDictionary extraData;
	public NSMutableDictionary indexes;
	public EOEditingContext ec;
	public ExtSystem sys;
	public ExtBase base;
	public NSDictionary indexItem;
	public Object item1;
	public Object item2;
	public NSDictionary section;
	public boolean showSections;
	public NSKeyValueCoding access;
	
	public String dataStyle;
	
    public ExportParams(WOContext context) {
        super(context);
        ec = new SessionedEditingContext(context.session());
    }
    
    public void setReporter(NSDictionary dict) {
    	plist = dict;
    	String systemName = (String)dict.valueForKey("extSystem");
    	sys = ExtSystem.extSystemNamed(systemName, ec, true);
    	systemName = (String)dict.valueForKey("extBase");
    	if(systemName != null)
    		base = sys.getBase(systemName, true);
    	if(ec.hasChanges())
    		ec.saveChanges();
    	NSArray list = (NSArray)plist.valueForKey("extData");
    	if(list != null && list.count() > 0) {
        	extraData = sys.getDataDict(base);
        	if(extraData == null) {
        		extraData = new NSMutableDictionary();
        	} else {
        		dataStyle = "display:none;";
        		for (int i = 0; i < list.count(); i++) {
					NSDictionary param = (NSDictionary)list.objectAtIndex(i);
					if(!Various.boolForObject(param.valueForKey("required")))
						continue;
					String att = (String)param.valueForKey("attribute");
					if(extraData.valueForKey(att) == null) {
						dataStyle = null;
						break;
					}
				}
        	}
    	}
    	list = (NSArray)plist.valueForKey("indexes");
    	if(list != null && list.count() > 0) {
        	indexes = sys.getIndexes(base);
        	if(indexes == null)
        		indexes = new NSMutableDictionary();
    	}
    	showSections = Various.boolForObject(plist.valueForKey("section")) &&
    			Various.boolForObject(session().valueForKeyPath("strings.sections.hasSections"));
    	if(showSections)
    		section = (NSDictionary)session().valueForKeyPath("state.section");
    	String checkAccess = (String) plist.valueForKey("checkAccess");
    		access = (NSKeyValueCoding)session().valueForKeyPath(
    			(checkAccess == null)?"readAccess.FLAGS.Export":"readAccess.FLAGS." + checkAccess);
    }
    
    public WOActionResults save() {
		sys.setDataDict(extraData, base);
		sys.setIndexes(indexes, base);
		try {
			ec.saveChanges();
			session().takeValueForKey("parameters saved", "message");
		} catch (Exception e) {
			session().takeValueForKey(e.getMessage(), "message");
		}
		returnPage.ensureAwakeInContext(context());
		return returnPage;
    }
    
    public WOActionResults export() {
 		sys.setDataDict(extraData, base);
		sys.setIndexes(indexes, base);
 		if(ec.hasChanges()) {
 			try {
 				ec.saveChanges();
 			} catch (Exception e) {
 				session().takeValueForKey(e.getMessage(), "message");
 				returnPage.ensureAwakeInContext(context());
 				return returnPage;
 			}
 		}
	   	NSMutableDictionary reportDict = new NSMutableDictionary();
		reportDict.takeValueForKey(plist,"reporter");
		reportDict.takeValueForKey(section, "section");
		reportDict.takeValueForKey(ec,"ec");
		reportDict.takeValueForKey(indexes, "indexes");
		reportDict.takeValueForKey("ImportExport", "reportDir");

		NSMutableDictionary info = new NSMutableDictionary(MyUtility.presentEduYear(
				(Integer)session().valueForKey("eduYear")), "eduYear");
		info.takeValueForKey(extraData, "extraData");
		NSArray indList = (NSArray)plist.valueForKey("indexes");
		if(indList != null && indList.count() > 0) {
			for (int i = 0; i < indList.count(); i++) {
				NSDictionary ind = (NSDictionary)indList.objectAtIndex(i);
				if(Various.boolForObject(ind.valueForKey("inOptions"))) {
					String indName = (String)ind.valueForKey("name");
					info.takeValueForKey(indexes.valueForKey(indName), indName);
				}
			}
		}
		reportDict.takeValueForKey(info, "info");

		byte[] result = null;
		try {
			result = XMLGenerator.generate(session(), (NSMutableDictionary)reportDict);
		} catch (Exception e) {
			result = WOLogFormatter.formatTrowable(e).getBytes();
		}
		WOResponse response = application().createResponseInContext(context());
		response.setContent(result);
		String contentType = (String)plist.valueForKey("ContentType");
		if(contentType == null)
			contentType = "application/octet-stream";
		response.setHeader(contentType,"Content-Type");
		StringBuilder buf = new StringBuilder("attachment; filename=\"");
		buf.append("filename");
		contentType = (String)plist.valueForKey("filext");
		if(contentType != null) {
			if(contentType.charAt(0) != '.')
				buf.append('.');
			buf.append(contentType);
		}
		buf.append('"');
		response.setHeader(buf.toString(),"Content-Disposition");
		return response;
    }
    
    public NSArray localList() {
    	if(indexItem == null)
    		return null;
    	NSDictionary local = (NSDictionary)indexItem.valueForKey("local");
    	if(local == null)
    		return null;
    	NSArray result = (NSArray)local.valueForKey("list");
    	if(result != null)
    		return result;
    	String entityName = (String)local.valueForKey("entityName");
    	if(entityName != null) {
    		EOQualifier qual = null;
    		String qualifier = (String)local.valueForKey("qualifier");
    		if(qualifier != null) {
    			NSArray args = (NSArray)local.valueForKey("args");
    			if(args != null && args.count() > 0) {
    				Object[] a = new Object[args.count()];
    				for (int i = 0; i < a.length; i++) {
    					a[i] = DisplayAny.ValueReader.evaluateValue(
    							args.objectAtIndex(i), plist, this);
    				}
    				args = new NSArray(a);
    			}
    			qual = EOQualifier.qualifierWithQualifierFormat(qualifier, args);
    		}
    		NSArray sorter = (NSArray)local.valueForKey("sorter");
			if(sorter != null && sorter.count() > 0) {
				Object[] s = new Object[sorter.count()];
				for (int i = 0; i < s.length; i++) {
					Object sort = sorter.objectAtIndex(i);
					if(sort instanceof String) {
						s[i] = new EOSortOrdering((String)sort,EOSortOrdering.CompareAscending);
					} else {
						NSDictionary so = (NSDictionary)sort;
						String key = (String)so.valueForKey("key");
						String order = (String)so.valueForKey("order");
						s[i] = new EOSortOrdering(
								key,EOSortOrdering._operatorSelectorForString(order));
					}
				}
				sorter = new NSArray(s);
			}
			EOFetchSpecification fs = new EOFetchSpecification(entityName,qual,sorter);
			result = ec.objectsWithFetchSpecification(fs);
    	} else if(local.valueForKey("methodName") != null)
    		result = (NSArray)DisplayAny.ValueReader.evaluateDict(local, plist, this);
    	if(result != null) {
    		String uniq = (String)local.valueForKey("uniqueAttribute");
    		if(uniq != null) {
    			NSMutableArray res = new NSMutableArray();
    			for (int i = 0; i < result.count(); i++) {
					Object obj = NSKeyValueCodingAdditions.Utility.valueForKeyPath(
							result.objectAtIndex(i), uniq);
					if(obj instanceof String)
						obj = WOMessage.stringByEscapingHTMLString((String)obj);
					if(!res.containsObject(obj))
						res.addObject(obj);
				}
    			result = res;
    		}
    	}
    	local.takeValueForKey(result, "list");
    	return result;
    }
    
    public String value1() {
    	if(item1 == null)
    		return null;
    	if(item1 instanceof String)
    		return (String)item1;
    	String format = (String)indexItem.valueForKeyPath("local.titlePath");
    	if(format != null)
    		return WOMessage.stringByEscapingHTMLString(
    				(String)NSKeyValueCodingAdditions.Utility.valueForKeyPath(item1, format));
    	format = (String)indexItem.valueForKeyPath("local.format");
    	if(format == null)
    		return item1.toString();
    	NSArray fa = (NSArray)indexItem.valueForKeyPath("local.formatArgs");
    	Object[] args = null;
    	if(fa != null && fa.count() > 0) {
    		args = new Object[fa.count()];
    		for (int i = 0; i < args.length; i++) {
				args[i] = DisplayAny.ValueReader.evaluateValue(fa.objectAtIndex(i), item1, this);
			}
    	}
    	return WOMessage.stringByEscapingHTMLString(String.format(format, args));
    }
    
    public String value2() {
    	if(item2 == null)
    		return null;
    	if(item2 instanceof String)
    		return (String)item2;
    	if(item2 instanceof NSKeyValueCoding)
    		return (String)((NSKeyValueCoding)item2).valueForKey("title");
    	return item2.toString();
    }
    
    private String localValue() {
    	if(item1 == null || item1 instanceof String) {
    		return (String)item1;
    	} else {
    		String path = (String)indexItem.valueForKeyPath("local.valuePath");
    		if(path == null) {
    			return MyUtility.getID((EOEnterpriseObject)item1);
    		} else {
    			return NSKeyValueCodingAdditions.Utility.valueForKeyPath(item1, path).toString();
    		}
    	}
    }
    
    private NSMutableDictionary indexDict(boolean create) {
    	if(indexes == null || indexItem == null)
    		return null;
    	String name = (String)indexItem.valueForKey("name");
    	if(name == null)
    		return null;
    	NSMutableDictionary dict = (NSMutableDictionary)indexes.valueForKey(name);
    	if(create && dict == null) {
    		dict = new NSMutableDictionary();
    		indexes.takeValueForKey(dict, name);
    	}
    	return dict;
    }
    
    public String indexStyle() {
    	NSMutableDictionary dict = indexDict(false);
    	if(dict == null || dict.count() == 0)
    		return null;
    	NSArray list = localList();
    	if(list == null || list.count() == 0)
    		return null;
    	for (int i = 0; i < list.count(); i++) {
			item1 = list.objectAtIndex(i);
			String value = localValue();
			if(value == null)
				continue;
			value = (String)dict.valueForKey(value);
			if(value == null)
				return null;
		}
    	return "display:none;";
    }
    
    public String toggleIndex() {
    	if(indexItem == null)
    		return null;
    	StringBuilder buf = new StringBuilder("toggleObj('");
    	buf.append(indexItem.valueForKey("name")).append("');");
    	return buf.toString();
    }
    
    private boolean inAppend = false;
    public void appendToResponse(WOResponse aResponse,
            WOContext aContext) {
    	inAppend = true;
    	super.appendToResponse(aResponse, aContext);
    	inAppend = false;
    }
    
    private String defaultValue() {
    	if(!inAppend || item1 == null)
    		return null;
    	Object dflt = indexItem.valueForKey("defaultValue");
    	if(dflt == null)
    		return null;
    	if(dflt.equals("ASSUME")) {
    		String str1 = value1();
    		NSArray list = (NSArray)indexItem.valueForKey("external");
    		int best = 0;
    		String result = null;
    		for (int i = 0; i < list.count(); i++) {
				Object value2 = list.objectAtIndex(i);
				String str2;
				if(value2 instanceof NSKeyValueCoding) {
					str2 = (String)((NSKeyValueCoding)value2).valueForKey("title");
					value2 = (String)((NSKeyValueCoding)value2).valueForKey("value");
				} else {
					str2 = value2.toString();
				}
				int cor = Various.correlation(str1, str2);
				if(cor > best && cor > Math.min(str1.length(), str2.length())/2) {
					result = value2.toString();
					best = cor;
				}
			}
    		return result;
    	}
    	return (String)DisplayAny.ValueReader.evaluateValue(dflt, item1, this);
    }
    
    public Object selection() {
    	NSMutableDictionary dict = indexDict(false);
    	String value = null;
    	if(dict != null) {
    		value = localValue();
        	value = (value==null)?null:(String)dict.valueForKey(value);
    	}
    	if(value == null)
    		value = defaultValue();
    	if(value == null)
    		return null;
    	NSArray external = (NSArray)indexItem.valueForKey("external");
    	if(external == null || external.count() == 0)
    		return null;
    	for (int i = 0; i < external.count(); i++) {
			Object ext = external.objectAtIndex(i);
			if(ext instanceof String)
				return value;
			if(value.equals(((NSKeyValueCoding)ext).valueForKey("value")))
				return ext;
		}
    	return null;
    }
    
    public void setSelection(Object selection) {
    	String key = localValue();
    	if(key == null)
    		return;
    	NSMutableDictionary dict = indexDict(true);
    	if(selection instanceof NSKeyValueCoding)
        	selection = ((NSKeyValueCoding)selection).valueForKey("value");
    	dict.takeValueForKey(selection, key);
    }
}