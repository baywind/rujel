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
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.transform.TransformerException;

import org.json.JSONObject;

import net.rujel.base.MyUtility;
import net.rujel.rest.Agregator.ParseError;
import net.rujel.reusables.DataBaseConnector;
import net.rujel.reusables.SettingsReader;
import net.rujel.reusables.Various;
import net.rujel.reusables.WOLogFormatter;

import com.webobjects.appserver.WOApplication;
import com.webobjects.appserver.WOContext;
import com.webobjects.appserver.WORequest;
import com.webobjects.appserver.WORequestHandler;
import com.webobjects.appserver.WOResponse;
import com.webobjects.eocontrol.EOEditingContext;
import com.webobjects.eocontrol.EOObjectStore;
import com.webobjects.foundation.NSMutableDictionary;

public class AgregationHandler extends WORequestHandler {

	public static final String handlerKey = "agr";
	public static final Logger logger = Logger.getLogger("rujel.rest");
	
	public WOResponse handleRequest(WORequest req) {
		WOApplication app = WOApplication.application();
		WOContext context = app.createContextForRequest(req);
		String txt = req.requestHandlerPath();
		if(txt == null || txt.length() == 0) {
			WOResponse response = app.createResponseInContext(context); 
			response.setHeader("text/html; charset=UTF-8","Content-Type");
			response.setContent(app.resourceManager().bytesForResourceNamed(
					"prepareRequest.html", "RujelREST", null));
			return response;
		}
		if("jsonQuery".equals(txt)) {
			JSONObject json = new JSONObject(req.contentString());
			ReportProcessor processor = new ReportProcessor(json);
			processor.schoolID = req.stringFormValueForKey("schoolID");
			Thread t = new Thread(processor,"ReportProcessor");
			t.setPriority(Thread.MIN_PRIORITY + 1);
			t.start();

			WOResponse response = app.createResponseInContext(context); 
			response.setHeader("text/plain; charset=UTF-8","Content-Type");
			response.setContent("OK");
			return response;
		}
		logger.log(Level.FINER, "Agregation request for entity '" + txt + '\'',
				new Object[] {Various.clientIdentity(req), req.formValues()});
		ReportSource res = new ReportSource();
		res.entity = txt;
		try {
			byte[] result = prepareXML(req, context, res);
			WOResponse response = app.createResponseInContext(context);
			response.setContent(result);
			response.setHeader("text/xml; charset=UTF-8","Content-Type");
			response.disableClientCaching();
			return response;
		} catch (ParseError e) {
			return parseError(e, context);
		} catch (Exception e) {
			return error(e, context);
		}
	}

	public byte[] prepareXML(WORequest req, WOContext context, ReportSource res)
			throws ParseError, TransformerException {
		String txt = req.stringFormValueForKey("eduYear");
		Integer eduYear = (txt == null)?(Integer)WOApplication.application().valueForKey("year"):
			new Integer(txt);
		if(eduYear == null)
			eduYear = MyUtility.eduYearForDate(null);
		context.setUserInfoForKey(eduYear, "eduYear");
		EOEditingContext ec;
		if(SettingsReader.boolForKeyPath("dbConnection.yearTag", false)) {
			if(txt == null)
				txt = eduYear.toString();
			EOObjectStore os = DataBaseConnector.objectStoreForTag(txt);
			if(os == null) {
				WOResponse response =  WOApplication.application().createResponseInContext(context);
				response.setHeader("text/plain; charset=UTF-8","Content-Type");
				response.setStatus(WOResponse.HTTP_STATUS_INTERNAL_ERROR);
				response.appendContentString("Requested eduYear '");
				response.appendContentString(txt);
				response.appendContentString("' was not found in database.");
			}
			ec = new EOEditingContext(os);
		} else {
			ec = new EOEditingContext();
		}
		ec.setUserInfoForKey(eduYear, "eduYear");
		ec.lock();
		byte[] result;
		try {
			AgrEntity entity = AgrEntity.forName(res.entity,ec);
			context.setUserInfoForKey(entity, res.entity);
			NSMutableDictionary params = new NSMutableDictionary();
			Enumeration enu = entity.attributes().objectEnumerator();
			while (enu.hasMoreElements()) {
				String key = (String) enu.nextElement();
				params.takeValueForKey(req.stringFormValueForKey(key), key);
			}
			res.attributes = params;
			enu = entity.getObjectsEnumeration(params);
			int lvl = 1;
			while (enu != null) {
				txt = req.stringFormValueForKey("_grp" + lvl);
				if(txt == null)
					break;
				res.groupings = txt.split(",");
				for (int i = 0; i < res.groupings.length; i++) {
					res.groupings[i] = res.groupings[i].trim();
				}
				txt = req.stringFormValueForKey("_agr" + lvl);
				String[] prevAgr = res.agregates;
				Agregator[] agregators;
				if(txt != null && txt.length() > 0) {
					res.agregates = txt.split(",");
					agregators = new Agregator[res.agregates.length];
					for (int i = 0; i < res.agregates.length; i++) {
						String key = res.agregates[i].trim();
						res.agregates[i] = key;
						String source = req.stringFormValueForKey(key);
						if(source == null)
							continue;
						agregators[i] = Agregator.parceAgregator(source);
						agregators[i].name = key;
					}
				} else {
					agregators = null;
					res.agregates = null;
				}
				txt = req.stringFormValueForKey("_list" + lvl);
				if(txt != null)
					res.lists = txt.split(",");
				res.agregate(agregators, enu, prevAgr);
				res.level = new Integer(lvl);
				enu = (res.rows == null || res.rows.count() == 0)? null : res.rows.objectEnumerator();
				lvl++;
			} // argegation levels
			result = ResponseXML.generate(res);
		} finally {
			ec.unlock();
			ec.dispose();
		}
		return result;
	}
	
	public WOResponse error(Throwable error, WOContext context) {
		WOResponse response =  WOApplication.application().createResponseInContext(context);
		response.setHeader("text/plain; charset=UTF-8","Content-Type");
		if(error instanceof IllegalArgumentException && error.getMessage().contains("entity")) {
			response.appendContentString(error.getMessage());
			response.setStatus(WOResponse.HTTP_STATUS_NOT_FOUND);
		} else {
			logger.log(Level.WARNING,"Error in service response",error);
			response.setStatus(WOResponse.HTTP_STATUS_INTERNAL_ERROR);
			response.appendContentString("Error occured when generating response.\n\n");
			response.appendContentString(WOLogFormatter.formatTrowable(error));
		}
		return response;
	}

	public WOResponse parseError(ParseError error, WOContext context) {
		WOResponse response = WOApplication.application().createResponseInContext(context);
		response.setHeader("text/plain; charset=UTF-8","Content-Type");
		response.appendContentString("An error occured when parsing request parameter:\n\n");
		response.appendContentString(error.getParsingString());
		response.appendContentCharacter('\n');
		response.appendContentString(error.showPosition());
		response.appendContentCharacter('\n');
		response.appendContentString(error.getMessage());
		response.appendContentCharacter('\n');
		response.setStatus(WOResponse.HTTP_STATUS_INTERNAL_ERROR);
		return response;
	}
	
}
