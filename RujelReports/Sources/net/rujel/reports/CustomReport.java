// CustomReport.java: Class file for WO Component 'CustomReport'

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

package net.rujel.reports;

import java.util.Enumeration;
import java.util.logging.Logger;

import net.rujel.reusables.SessionedEditingContext;
import net.rujel.reusables.Various;
import net.rujel.reusables.WOLogLevel;

import com.webobjects.appserver.*;
import com.webobjects.eoaccess.EOEntity;
import com.webobjects.eoaccess.EORelationship;
import com.webobjects.eoaccess.EOUtilities;
import com.webobjects.eocontrol.*;
import com.webobjects.foundation.*;

// Generated by the WOLips Templateengine Plug-in at Apr 28, 2009 12:10:12 PM
public class CustomReport extends com.webobjects.appserver.WOComponent {
	protected static final Logger logger = Logger.getLogger("rujel.reports");
	
	public EOEditingContext ec;
	public NSMutableArray reports;
	public NSMutableArray display;
	public NSArray list;
	public NSMutableDictionary currReport;
	public NSMutableDictionary params;

	public NSKeyValueCoding item;
	public Integer index;
	protected NSArray fullList;
	public NSMutableArray countList;

	public CustomReport(WOContext context) {
        super(context);
        reports = ReportsModule.reportsFromDir("CustomReport", context);
        params = new NSMutableDictionary();
        ec = new SessionedEditingContext(context.session());
    }
    
    public String title() {
/*    	try {
			Class bd = Class.forName("java.math.BigDecimal");
			java.lang.reflect.Method[] meths = bd.getMethods();
			int count = 0;
			for (int i = 0; i < meths.length; i++) {
				Class[] prms = meths[i].getParameterTypes();
				count += prms.length;
			}
		} catch (Exception e) {
			System.out.println("Error");
		}*/
		return (String)application().valueForKeyPath(
				"strings.RujelReports_Reports.CustomReport.title");
	}

    public void go() {
    	NSMutableArray quals = new NSMutableArray();
    	String entityName = (String)currReport.valueForKey("entity");
   	
    	NSMutableDictionary inQuals = new NSMutableDictionary();
    	NSArray paramDicts = (NSArray)currReport.valueForKey("params");
    	Enumeration enu = paramDicts.objectEnumerator();
    	while (enu.hasMoreElements()) {
			NSKeyValueCoding dict = (NSKeyValueCoding) enu.nextElement();
			if(!Various.boolForObject(dict.valueForKey("active")))
				continue;
			EOQualifier qual = Parameter.qualForParam(dict, params,this);
			if(qual != null) {
				NSKeyValueCoding in = (NSKeyValueCoding)dict.valueForKey("in");
				if(in == null) {
					quals.addObject(qual);
				} else {
					String rel = (String)in.valueForKey("relationship");
					NSMutableDictionary inDict = (NSMutableDictionary)
							inQuals.valueForKey(rel);
					if(inDict == null) {
						String inEntity = (String)in.valueForKey("entity");
						if(inEntity == null) {
							try {
								EOEntity ent = EOUtilities.entityNamed(ec, entityName);
								EORelationship relat = ent.relationshipNamed(rel);
								ent = relat.destinationEntity();
								inEntity = ent.name();
							} catch (RuntimeException e) {
								Object[] args = new Object[] { session(),e,dict };
								logger.log(WOLogLevel.WARNING,"Could not get entityName",args);
								continue;
							}
						}
						inDict = new NSMutableDictionary(inEntity,"entity");
						NSMutableArray attrQuals = new NSMutableArray(qual);
						qual = Parameter.qualForParam(in, params,this);
						if(qual != null) {
							attrQuals.addObject(qual);
						}
						inDict.setObjectForKey(attrQuals, "attrQuals");
						inQuals.takeValueForKey(inDict, rel);
					} else {
						NSMutableArray attrQuals = (NSMutableArray)
								inDict.valueForKey("attrQuals");
						attrQuals.addObject(qual);
					}
				}
			} else if(!Various.boolForObject(dict.valueForKey("hidden"))) { // has qual
				dict.takeValueForKey(null, "active");
			}
		}
    	EOQualifier qual = null;
    	if(inQuals != null && inQuals.count() > 0) {
    		enu = inQuals.keyEnumerator();
    		while (enu.hasMoreElements()) {
				String rel = (String) enu.nextElement();
				NSMutableDictionary inDict = (NSMutableDictionary)inQuals.objectForKey(rel);
				NSArray attrQuals = (NSArray)inDict.valueForKey("attrQuals");
				qual = new EOAndQualifier(attrQuals);
				String inEntity = (String)inDict.valueForKey("entity");
				EOFetchSpecification fs = new EOFetchSpecification(inEntity,qual,null);
				NSArray inList = ec.objectsWithFetchSpecification(fs);
				qual = Various.getEOInQualifier(rel, inList);
				if(qual != null)
					quals.addObject(qual);
			}
    	} // add IN qualifiers
    	if(quals.count() > 0) {
    		if(quals.count() > 1)
    			qual = new EOAndQualifier(quals);
    		else
    			qual = (EOQualifier)quals.objectAtIndex(0);
    	}
    	EOFetchSpecification fs = new EOFetchSpecification(entityName,qual,null);
    	fs.setRefreshesRefetchedObjects(true);
    	NSArray args = (NSArray)currReport.valueForKey("prefetch");
    	if(args != null && args.count() > 0)
    		fs.setPrefetchingRelationshipKeyPaths(args);
    	fullList = ec.objectsWithFetchSpecification(fs);
    	if(fullList == null || fullList.count() == 0) {
    		session().takeValueForKey(session().valueForKeyPath(
    				"strings.Strings.messages.nothingFound"), "message");
    	} else {
    		setupDisplay();
        	logger.log(WOLogLevel.MASS_READING,"Generating report '" + 
        			currReport.valueForKey("title") + "' found: " + fullList.count(),session());
    	}
    }
    
    public void setupDisplay() {
    	display = PropSelector.prepareActiveList(
    			(NSMutableArray)currReport.valueForKey("properties"));
		countList = null;
    	if(fullList != null && fullList.count() > 1) {
    		boolean sortAll = Various.boolForObject(currReport.valueForKey("sortAll"));
    		list = sort(fullList,display,sortAll);
    		Object[] prop = display.objects();
    		params.removeObjectForKey("currObject");
    		if(!sortAll) {
    			for (int i = 0; i < prop.length; i++) {
					if(sortingProp((NSKeyValueCoding)prop[i],sortAll) == null)
						return;
				}
    		}
    		NSMutableArray newList = new NSMutableArray(list.objectAtIndex(0));
    		countList = new NSMutableArray();
    		Enumeration listEnu = list.objectEnumerator();
    		
    		Object[] ref = new Object[prop.length];
    		int count = 0;
    		int maxCount = 0;
    		while (listEnu.hasMoreElements()) {
    			item = (NSKeyValueCoding) listEnu.nextElement();
    			for (int i = 0; i < prop.length; i++) {
					Object param = ReportTable.valueFromDict((NSDictionary)prop[i], item, this);
					if(count == 0) {
						ref[i] = param;
					} else if (param != ref[i]) {
						NSSelector sorter = sortingProp((NSKeyValueCoding)prop[i], sortAll);
						int dev = EOSortOrdering.ComparisonSupport.compareValues(param, 
								ref[i],sorter);
						if(dev != 0) {
							countList.addObject(new Integer(count));
							if(count > maxCount)
								maxCount = count;
							count = 0;
							newList.addObject(item);
							ref[i] = param;

						}
					}
				}
    			count++;
			}
			if(count > maxCount)
				maxCount = count;
    		if(maxCount > 1) {
    			countList.addObject(new Integer(count));
    			list = newList;
    		} else {
    			countList = null;
    		}
    	} else {
    		list = fullList;
    	}
    }
    
    public String showCount() {
    	if(countList == null)
    		return null;
    	StringBuilder result = new StringBuilder();
    	if(index == null) {
    		result.append("<th class = \"backfield2\">");
    		result.append(application().valueForKeyPath(
    				"strings.RujelReports_Reports.CustomReport.countTitle"));
    		result.append("</th>");
    	} else {
    		result.append("<td class = \"backfield1\">");
    	try {
			Integer count = (Integer) countList.objectAtIndex(index.intValue());
			result.append(count);
		} catch (Exception e) {
			result.append('?');
		}
		result.append("</td>");
    	}
    	return result.toString();
    }
    
    public void newQuery() {
    	list = null;
    }
    
    protected static NSSelector sortingProp(NSKeyValueCoding prop,boolean all) {
		NSSelector selector = (NSSelector)prop.valueForKey("sortingSelector");
		if(selector != null)
			return selector;
		String key = (String)prop.valueForKey("comparison");
		if(key == null && !all)
			return null;
		selector = EOSortOrdering.CompareAscending;
		if(key != null) {
			try {
				selector = (NSSelector)EOSortOrdering.class.getField(key).get(null);
			} catch (Exception e) {
				Object[] args = new Object[] {e,prop};
				logger.log(WOLogLevel.WARNING,"Error parsing comparison for property "
						+ prop.valueForKey("title"),args);
				selector = EOSortOrdering.CompareAscending;
			};
		}
		try {
			prop.takeValueForKey(selector, "sortingSelector");
		} finally {
			return selector;
		}
    }
    
    public static NSArray sort(NSArray list, NSArray properties,boolean all) {
    	if(list == null || list.count() == 0)
    		return list;
    	if(properties == null || properties.count() == 0)
    		return list;
    	NSMutableArray sorter = new NSMutableArray();
    	Enumeration enu = properties.objectEnumerator();
    	while (enu.hasMoreElements()) {
			NSKeyValueCoding prop = (NSKeyValueCoding) enu.nextElement();
			NSSelector selector = sortingProp(prop, all);
			if(selector == null)
				continue;
			String key = (String)prop.valueForKey("sortKey");
			if(key == null)
				key = (String)prop.valueForKey("keyPath");
			if(key == null)
				continue;
			EOSortOrdering so = EOSortOrdering.sortOrderingWithKey(key, selector);
			sorter.addObject(so);
		}
    	return EOSortOrdering.sortedArrayUsingKeyOrderArray(list, sorter);
    }
    
	public WOActionResults export() {
		WOComponent exportPage = pageWithName("ReportTable");
		exportPage.takeValueForKey(list, "list");
		exportPage.takeValueForKey(display, "properties");
 		exportPage.takeValueForKey(currReport.valueForKey("entity"), "filenameFormatter");
 		if(countList != null) {
 			NSDictionary extra = new NSDictionary( new Object[] {
 					application().valueForKeyPath(
 							"strings.RujelReports_Reports.CustomReport.countTitle"),
 					"$nextValue",countList.objectEnumerator()
 			},new String[] {
 					"title","value","valuesEnumeration"
 			});
 			exportPage.takeValueForKey(extra, "extra");
 		}
		return exportPage;
	}
	
	public String paramCellStyle() {
		if(Various.boolForObject(item.valueForKey("active")))
			return "visibility:visible;";
		return "visibility:hidden;";
	}
	
	public WOActionResults alert() {
		WOResponse response = WOApplication.application().createResponseInContext(context());
		response.appendContentString(
	"<div id = \"ajaxPopup\" class=\"warning\" style=\"cursor:pointer;\" onclick=\"closePopup()\">");
		response.appendContentString("OOPS!");
		NSDictionary formValues = context().request().formValues();
		response.appendContentString(WOResponse.stringByEscapingHTMLString(formValues.toString()));
		response.appendContentString("</div>");
		return response;
	}
}