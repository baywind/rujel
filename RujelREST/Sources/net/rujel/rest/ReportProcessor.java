package net.rujel.rest;

import java.net.URL;
import java.util.Enumeration;
import java.util.Iterator;

import org.json.JSONArray;
import org.json.JSONObject;

import com.webobjects.appserver.WOApplication;
import com.webobjects.appserver.WOContext;
import com.webobjects.appserver.WOHTTPConnection;
import com.webobjects.appserver.WORequest;
import com.webobjects.appserver.WOResponse;
import com.webobjects.eocontrol.EOEditingContext;
import com.webobjects.eocontrol.EOObjectStore;
import com.webobjects.foundation.NSDictionary;

import net.rujel.base.MyUtility;
import net.rujel.rest.Agregator.ParseError;
import net.rujel.reusables.DataBaseConnector;
import net.rujel.reusables.SettingsReader;
import net.rujel.reusables.WOLogLevel;

public class ReportProcessor implements Runnable {
	
	protected JSONObject json;
	public String queryID;
	public String schoolID;
	
	public ReportProcessor(JSONObject json) {
		super();
		this.json = json;
	}

	public void run() {
		ReportSource res = new ReportSource();
		res.entity = json.getString("entity");
		String responseURL = json.getString("responseURL");
//		schoolID = json.getString("schoolID");
		queryID = json.getString("queryID");
		if(!responseURL.endsWith(queryID)) {
			StringBuilder buf = new StringBuilder(responseURL);
			if(responseURL.indexOf('?') > 0) {
				buf.append('&');
			} else {
				buf.append('?');
			}
			buf.append("queryID=").append(queryID);
			buf.append("&schoolID=").append(schoolID);

			responseURL = buf.toString();
		}
		res.attributes = JSONObject.Utility.dictFromJSON(json.optJSONObject("queryParams"), false);
		WORequest message = new WORequest(
				"GET", responseURL, "HTTP/1.1", null, null, null);
		Integer eduYear = null;
		if(res.attributes != null) {
			Object year = res.attributes.valueForKey("eduYear");
			if(year instanceof Integer)
				eduYear = (Integer)year;
			else if (year instanceof Number)
				eduYear = new Integer(((Number)year).intValue());
			else if(year instanceof CharSequence)
				eduYear = new Integer(year.toString());
		}
		if(eduYear == null) {
			eduYear = (Integer)WOApplication.application().valueForKey("year");
			if (eduYear == null)
				eduYear = MyUtility.eduYearForDate(null);
		}
		if(res.attributes == null)
			res.attributes = new NSDictionary(eduYear,"eduYear");
		WOContext context = MyUtility.dummyContext(null);
		context.setUserInfoForKey(eduYear, "eduYear");
		
		EOEditingContext ec;
		if(SettingsReader.boolForKeyPath("dbConnection.yearTag", false)) {
			String txt = eduYear.toString();
			EOObjectStore os = DataBaseConnector.objectStoreForTag(txt);
			if(os == null) {
				message.appendContentString("ERROR: Requested eduYear '");
				message.appendContentString(txt);
				message.appendContentString("' was not found in database.");
				sendRequest(message, responseURL);
				return;
			}
			ec = new EOEditingContext(os);
		} else {
			ec = new EOEditingContext();
		}
		ec.setUserInfoForKey(eduYear, "eduYear");
		ec.lock();
		try {
			AgrEntity entity = AgrEntity.forName(res.entity,ec);
			context.setUserInfoForKey(entity, res.entity);
			Enumeration enu = entity.getObjectsEnumeration(res.attributes);
			int lvl = 1;
			JSONArray agregations = json.getJSONArray("agregations");
			Iterator iter = agregations.iterator();
			while (iter.hasNext() && enu != null) {
				JSONObject agregation = (JSONObject) iter.next();
				JSONArray list = agregation.getJSONArray("groupings");
				res.groupings = new String[list.length()];
				for (int i = 0; i < res.groupings.length; i++) {
					res.groupings[i] = list.getString(i).trim();
				}
				String[] prevAgr = res.agregates;
				JSONObject agregates = agregation.optJSONObject("agregates");
				Agregator[] agregators;
				if(agregates == null || agregates.length() == 0) {
					agregators = null;
					res.agregates = null;
				} else {
					res.agregates = new String[agregates.length()];
					agregators = new Agregator[agregates.length()];
					int i = 0;
					Iterator argIter = agregates.keys();
					while (argIter.hasNext()) {
						String key = (String) argIter.next();
						res.agregates[i] = key;
						String source = agregates.optString(key);
						agregators[i] = Agregator.parceAgregator(source);
						agregators[i].name = key;
						i++;
					}
				}
				list = agregation.optJSONArray("list");
				if(list != null) {
					res.lists = new String[list.length()];
					for (int j = 0; j < res.lists.length; j++) {
						res.lists[j] = list.getString(j);
					}
				}
				res.agregate(agregators, enu, prevAgr);
				res.level = new Integer(lvl);
				enu = (res.rows == null || res.rows.count() == 0)? null : res.rows.objectEnumerator();
				lvl++;
			}
			message.setContent(ResponseXML.generate(res));
		} catch (ParseError e) {
			AgregationHandler.logger.log(WOLogLevel.INFO,
					"Error parsing agregation request " + queryID, e);
			message.appendContentString("ERROR parsing request: ");
			message.appendContentString(e.toString());
		} catch (Exception e) {
			AgregationHandler.logger.log(WOLogLevel.WARNING,
					"Error processing agregation request " + queryID, e);
			message.appendContentString("ERROR processing request: ");
			message.appendContentString(e.toString());
		} finally {
			ec.unlock();
			ec.dispose();
		}
		WOResponse response = sendRequest(message, responseURL);
		if(response != null) {
			AgregationHandler.logger.log(WOLogLevel.FINE,
					"Sent response to agregation request " + queryID);
		}
	}
	
	protected WOResponse sendRequest(WORequest req, String urlString) {
		try {
			URL url = new URL(urlString);
			int port = url.getPort();
			if(port < 0)
				port = url.getDefaultPort();
			WOHTTPConnection http = new WOHTTPConnection(url.getHost(), port);
			http.sendRequest(req);
			return http.readResponse();
		} catch (Exception e) {
			AgregationHandler.logger.log(WOLogLevel.INFO,
					"Failed to send response to agregation request " + queryID, e);
			return null;
		}
	}

}
