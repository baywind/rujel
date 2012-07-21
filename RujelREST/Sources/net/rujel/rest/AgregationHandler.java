// AgregationHandler.java

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

package net.rujel.rest;

import java.util.Enumeration;

import net.rujel.base.MyUtility;
import net.rujel.reusables.Counter;
import net.rujel.reusables.DataBaseConnector;

import com.webobjects.appserver.WOApplication;
import com.webobjects.appserver.WOContext;
import com.webobjects.appserver.WORequest;
import com.webobjects.appserver.WORequestHandler;
import com.webobjects.appserver.WOResponse;
import com.webobjects.eocontrol.EOEditingContext;
import com.webobjects.foundation.NSDictionary;
import com.webobjects.foundation.NSKeyValueCoding;
import com.webobjects.foundation.NSMutableArray;
import com.webobjects.foundation.NSMutableDictionary;

public class AgregationHandler extends WORequestHandler {

	public static final String handlerKey = "agr";
	
	public WOResponse handleRequest(WORequest req) {
		WOApplication app = WOApplication.application();
		WOContext context = app.createContextForRequest(req);
		String txt = req.stringFormValueForKey("eduYear");
		Integer eduYear = (txt == null)?(Integer)WOApplication.application().valueForKey("year"):
			new Integer(txt);
		if(eduYear == null)
			eduYear = MyUtility.eduYearForDate(null);
		context.setUserInfoForKey(eduYear, "eduYear");
		if(txt == null)
			txt = eduYear.toString();
		ReportSource res = new ReportSource();
		EOEditingContext ec = new EOEditingContext(DataBaseConnector.objectStoreForTag(txt));
		res.entity = req.requestHandlerPath();
		AgrEntity entity = AgrEntity.forName(res.entity,ec);
		context.setUserInfoForKey(entity, res.entity);
		Enumeration enu = entity.rawRowsEnumeration(req);
		res.attributes = (NSDictionary)context.userInfoForKey("params");
		int lvl = 0;
		NSMutableArray agrList = null;
		while (true) {
			txt = req.stringFormValueForKey("_grp" + (lvl +1));
			if(txt == null)
				break;
			lvl++;
			String[] grp = txt.split(",");
			for (int i = 0; i < grp.length; i++) {
				grp[i] = grp[i].trim();
			}
			txt = req.stringFormValueForKey("_agr" + lvl);
			String[] agr = txt.split(",");
			for (int i = 0; i < agr.length; i++) {
				agr[i] = agr[i].trim();
			}
			agrList = new NSMutableArray();
			NSMutableDictionary agrDict = new NSMutableDictionary();
			while (enu.hasMoreElements()) {
				NSKeyValueCoding row = (NSKeyValueCoding) enu.nextElement();
				if(res.agregates != null) {
					for (int i = 0; i < res.agregates.length; i++) {
						Agregator agregator = (Agregator)row.valueForKey(res.agregates[i]);
						if(agregator != null)
							row.takeValueForKey(agregator.getResult(), res.agregates[i]);
					}
				}
				NSMutableDictionary dict = agrDict;
				for (int i = 0; i < grp.length; i++) {
					Object key = row.valueForKey(grp[i]);
					if(key == null)
						key = NSKeyValueCoding.NullValue;
					NSMutableDictionary found = (NSMutableDictionary)dict.objectForKey(key);
					if(found == null) {
						found = new NSMutableDictionary();
						dict.setObjectForKey(found, key);
					}
					dict = found;
				}
				if(dict.count() == 0) {
					for (int i = 0; i < grp.length; i++) {
						dict.takeValueForKey(row.valueForKey(grp[i]), grp[i]);
					}
					agrList.addObject(dict);
					dict.setObjectForKey(new Counter(1), "_count_");
					if(agr == null) continue;
					for (int i = 0; i < agr.length; i++) {
						String source = req.stringFormValueForKey(agr[i]);
						Agregator agregator = Agregator.parceAgregator(source);
						dict.setObjectForKey(agregator, agr[i]);
						agregator.scan(row);
					}
				} else {
					dict.valueForKeyPath("_count_.raise");
					if(agr == null) continue;
					for (int i = 0; i < agr.length; i++) {
						Agregator agregator = (Agregator)dict.valueForKey(agr[i]);
						agregator.scan(row);
					}
				}
			} // rows enumeration
			res.agregates = agr;
			res.groupings = grp;
			enu = agrList.objectEnumerator();
		} // argegation levels
		res.level = lvl;
		res.rows = agrList.immutableClone();
		WOResponse response = app.createResponseInContext(context);
		try {
			response.setContent(ResponseXML.generate(res));
			response.setHeader("text/xml","Content-Type");
		} catch (Exception e) {
			response.appendContentString("Oops!!");
		}
		/*
		for (int i = 0; i < res.groupings.length; i++) {
			response.appendContentHTMLString(res.groupings[i]);
			response.appendContentCharacter(';');
		}
		for (int i = 0; i < res.agregates.length; i++) {
			response.appendContentHTMLString(res.agregates[i]);
			response.appendContentCharacter(';');
		}
		response.appendContentCharacter('\n');
		while (enu.hasMoreElements()) {
			NSMutableDictionary row = (NSMutableDictionary) enu.nextElement();
			for (int i = 0; i < res.groupings.length; i++) {
				String key = res.groupings[i];
				Object value = row.valueForKey(key);
				if(value != null)
					response.appendContentHTMLString(value.toString());
				response.appendContentCharacter(';');
			}
			for (int i = 0; i < res.agregates.length; i++) {
				String key = res.agregates[i];
				Object value = row.valueForKey(key);
				if(value != null)
					response.appendContentHTMLString(value.toString());
				response.appendContentCharacter(';');
			}
			response.appendContentCharacter('\n');
		} */
		response.disableClientCaching();
		return response;
	}

}
