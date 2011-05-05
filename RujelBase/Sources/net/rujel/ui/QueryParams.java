//QueryParams.java: Class file for WO Component 'QueryParams'

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

import java.util.Enumeration;
import java.util.logging.Logger;

import net.rujel.reusables.DisplayAny;
import net.rujel.reusables.Various;
import net.rujel.reusables.WOLogLevel;

import com.webobjects.appserver.WOContext;
import com.webobjects.appserver.WOComponent;
import com.webobjects.appserver.WOSession;
import com.webobjects.eoaccess.EOEntity;
import com.webobjects.eoaccess.EORelationship;
import com.webobjects.eoaccess.EOUtilities;
import com.webobjects.eocontrol.EOAndQualifier;
import com.webobjects.eocontrol.EOEditingContext;
import com.webobjects.eocontrol.EOFetchSpecification;
import com.webobjects.eocontrol.EOQualifier;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSKeyValueCoding;
import com.webobjects.foundation.NSMutableArray;
import com.webobjects.foundation.NSMutableDictionary;

public class QueryParams extends WOComponent {
	
	public NSKeyValueCoding item;
//	public NSArray list;
//	public NSMutableDictionary params;
	
    public QueryParams(WOContext context) {
        super(context);
    }

	public String paramCellStyle() {
		if(Various.boolForObject(item.valueForKey("active")))
			return "visibility:visible;white-space:nowrap;";
		return "visibility:hidden;white-space:nowrap;";
	}
	
	public boolean isStateless() {
		return true;
	}
	
	public boolean synchronizesVariablesWithBindings() {
        return false;
	}
	
	public void reset() {
		super.reset();
		item = null;
	}

	public static EOQualifier paramsToQual(NSMutableDictionary params, NSArray paramDicts, 
			String entityName,WOComponent onpage, EOEditingContext ec, NSMutableArray quals) {
		if(quals == null)
			quals = new NSMutableArray();
    	NSMutableDictionary inQuals = new NSMutableDictionary();
    	WOSession ses = onpage.session();
    	Logger logger = Logger.getLogger("rujel.reports");

    	Enumeration enu = paramDicts.objectEnumerator();
    	while (enu.hasMoreElements()) {
			NSKeyValueCoding dict = (NSKeyValueCoding) enu.nextElement();
			if(!Various.boolForObject(dict.valueForKey("active")))
				continue;
			EOQualifier qual = Parameter.qualForParam(dict, params,onpage);
			if(qual != null) {
				NSKeyValueCoding in = (NSKeyValueCoding)dict.valueForKey("in");
				if(in == null) {
					quals.addObject(qual);
				} else {
					String rel = (String)in.valueForKey("relationship");
					try {
						Object inList = in.valueForKey("list");
						if(inList != null) {
							inList = DisplayAny.ValueReader.evaluateValue(
									inList, params, onpage);
							if(inList != null && ((NSArray)inList).count() > 0) {
								quals.addObject(Various.getEOInQualifier(rel, (NSArray)inList));
								continue;
							} else {
								String message = (String)ses.valueForKeyPath(
									"strings.RujelReports_Reports.CustomReport.restrictingParam");
								if(message == null)
									message = (String)dict.valueForKey("title");
								else
									message = String.format(message,dict.valueForKey("title"));
								ses.takeValueForKey(message, "message");
								return null;
							}
						}
					} catch (Exception e) {
						logger.log(WOLogLevel.WARNING,
								"Error reading allowed value list for relationship \"" + rel +
								'"' + "in report on '" + entityName + '\'',
								new Object[] {ses,e});
					}
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
								Object[] args = new Object[] { ses,e,dict };
								logger.log(WOLogLevel.WARNING,"Could not get entityName",args);
								continue;
							}
						}
						inDict = new NSMutableDictionary(inEntity,"entity");
						NSMutableArray attrQuals = new NSMutableArray(qual);
						qual = Parameter.qualForParam(in, params,onpage);
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
    	if(quals.count() == 0)
    		return null;
    	if(quals.count() == 1)
    		return (EOQualifier) quals.objectAtIndex(0);
    	return new EOAndQualifier(quals);
	}
}