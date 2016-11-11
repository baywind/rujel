package net.rujel.ui;

import com.webobjects.appserver.WOContext;
import com.webobjects.appserver.WOComponent;
import com.webobjects.appserver.WOMessage;
import com.webobjects.appserver.WOResponse;
import com.webobjects.eoaccess.EOAdaptor;
import com.webobjects.eoaccess.EOAdaptorChannel;
import com.webobjects.eoaccess.EODatabaseChannel;
import com.webobjects.eoaccess.EODatabaseContext;
import com.webobjects.eoaccess.EOModel;
import com.webobjects.eoaccess.EOModelGroup;
import com.webobjects.eoaccess.EOSQLExpression;
import com.webobjects.eoaccess.EOSQLExpressionFactory;
import com.webobjects.eoaccess.EOUtilities;
import com.webobjects.eocontrol.EOEditingContext;
import com.webobjects.eocontrol.EOEnterpriseObject;
import com.webobjects.eocontrol.EOObjectStore;
import com.webobjects.eocontrol.EOSortOrdering;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSData;
import com.webobjects.foundation.NSDictionary;
import com.webobjects.foundation.NSForwardException;
import com.webobjects.foundation.NSMutableArray;
import com.webobjects.foundation.NSMutableDictionary;
import com.webobjects.foundation.NSPropertyListSerialization;

import java.io.InputStream;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Enumeration;

import net.rujel.base.MyUtility;
import net.rujel.reusables.DataBaseConnector;
import net.rujel.reusables.DisplayAny;
import net.rujel.reusables.Export;
import net.rujel.reusables.ExportCSV;
import net.rujel.reusables.SessionedEditingContext;
import net.rujel.reusables.SettingsReader;

import com.webobjects.appserver.WOActionResults;

public class QueryPage extends WOComponent {
    public NSDictionary plist;
    public NSMutableDictionary rootQuery;
    
    public NSMutableArray iterations;
    public NSMutableDictionary iter;
    public NSMutableDictionary newIter;
    
    public NSMutableDictionary subQuery;
//	public NSArray cols;
	public Object colItem;
	public NSDictionary rowItem;
//	public String message;
//	public EOEditingContext ec;
	public NSArray models;
	public NSMutableDictionary params;
	public NSMutableDictionary ecs;

	public static final SimpleDateFormat timestampFmt = 
		new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	public QueryPage(WOContext context) {
        super(context);
        try {
			InputStream pstream = application().resourceManager().inputStreamForResourceNamed(
					"QueryPage.plist", "app", null);
			NSData pdata = new NSData(pstream, pstream.available());
			plist = (NSDictionary)NSPropertyListSerialization.propertyListFromData(pdata, "utf8");
		} catch (Exception e) {
			throw new NSForwardException(e,"Error reading QueryPage.plist");
		}
		{
			ecs = new NSMutableDictionary();
			EOEditingContext ec = new SessionedEditingContext(context.session());
			Integer year = (Integer)session().valueForKey("eduYear");
			ecs.takeValueForKey(ec, "curr");
			ecs.takeValueForKey(ec, year.toString());
		}
        EOSortOrdering so = new EOSortOrdering("name",
        		EOSortOrdering.CompareCaseInsensitiveAscending);
        models = EOSortOrdering.sortedArrayUsingKeyOrderArray
        		(EOModelGroup.defaultGroup().models(), new NSArray(so));
        models = (NSArray)models.valueForKey("name");
        params = new NSMutableDictionary();
        rootQuery = new NSMutableDictionary();
		newIter = new NSMutableDictionary(new NSMutableDictionary(), "query");
		iterations = new NSMutableArray();
    }

		
	private static String formatValue(Object value) {
		if(value == null || value == NullValue)
			return "<em class=\"dimtext\">NULL</em>";
		else if(value instanceof java.util.Date) {
			Calendar cal = Calendar.getInstance();
			cal.setTime((java.util.Date)value);
			Format dateFormat = timestampFmt;
			if(cal.get(Calendar.HOUR_OF_DAY) == 0 && cal.get(Calendar.MINUTE) == 0 &&
					 cal.get(Calendar.SECOND) == 0)
				dateFormat = MyUtility.dateFormat();
			return dateFormat.format(value);
		} else {
			return WOMessage.stringByEscapingHTMLString(value.toString());
		}
	}
	
	public Object value() {
		if(rowItem == null || colItem == null)
			return null;
		String key = null;
		if(colItem instanceof String)
			key = (String)colItem;
		else if(colItem instanceof NSDictionary)
			key = (String)((NSDictionary)colItem).valueForKey("key");
		Object value = rowItem.valueForKey(key);
		if(colItem instanceof NSDictionary) {
			if(value == null)
				return "...";
			if(value instanceof String)
				return value;
			NSArray cols = (NSArray)((NSDictionary)colItem).valueForKey("cols");
			StringBuilder buf = new StringBuilder(
"<table style=\"border-color:#666666;border-collapse:collapse;text-align:center;\" cellspacing=\"0\" border=\"1\" align=\"center\">");
				Enumeration rows = ((NSArray)value).objectEnumerator();
				while (rows.hasMoreElements()) {
					NSDictionary dict = (NSDictionary) rows.nextElement();
					Enumeration enu = cols.objectEnumerator();
					buf.append("<tr>");
					while (enu.hasMoreElements()) {
						String col = (String) enu.nextElement();
						buf.append("<td>");
						buf.append(formatValue(dict.valueForKey(col))).append("</td>");
					}
					buf.append("</tr>");
			}
			buf.append("</table>");
			return buf.toString();
		} else {
			return formatValue(value);
		}
	}

	public WOActionResults performQuery() {
		NSArray cols;
		boolean addIter = (iter == null);
		if(addIter)
			iter = this.newIter;
		NSMutableArray result = (NSMutableArray)iter.valueForKey("result");
		if(result == null) {
			result = new NSMutableArray();
		} else {
			result.removeAllObjects();
		}
		NSMutableDictionary query = (NSMutableDictionary)iter.valueForKey("query");
		try {
			cols = performQuery(query,result);
		} catch (Exception e) {
			iter.takeValueForKey(e.getLocalizedMessage(), "message");
			iter = null;
			return null;
		}
		iter.takeValueForKey(cols, "cols");
		iter.takeValueForKey(result, "result");
		if(result.count() == 0 && (cols == null || cols.count() == 0)) {
			String sql = (String)query.valueForKey("sql");
			int space = sql.indexOf(' ');
			if(space > 0) {
				String message = sql.substring(0,space) + ": OK";
				iter.takeValueForKey(message, "message");
			} else {
				iter.takeValueForKey("Query OK", "message");
			}
		} else {
			String message = "Found: " + result.count();
			iter.takeValueForKey(message, "message");
		}
		if(addIter) {
			Number index = (Number)query.valueForKey("index");
			if(index == null) {
				index = new Integer(iterations.count());
				query.takeValueForKey(index, "index");
				NSMutableArray queries = (NSMutableArray)rootQuery.valueForKey("queries");
				if(queries == null) {
					queries = new NSMutableArray(query);
					rootQuery.takeValueForKey(queries, "queries");
				} else {
					queries.addObject(query);
				}
			}
			iter.takeValueForKey(index, "index");
			params.takeValueForKey(result, "res" + index);
			iterations.insertObjectAtIndex(iter, 0);
			iter.takeValueForKey(Boolean.TRUE, "recent");
			this.newIter = null;
		}
		if(result.count() == 0) {
			iter = null;
			return null;
		}
		NSMutableDictionary extInfo = (NSMutableDictionary)query.valueForKey("extInfo");
		if(extInfo != null && extInfo.count() > 0) {
			Enumeration enu = cols.objectEnumerator();
			while (enu.hasMoreElements()) {
				String col = (String) enu.nextElement();
				NSDictionary dict = (NSDictionary)extInfo.valueForKey(col);
				if(dict != null)
					applyExtInfo(result,dict, col);
			}
		}
		iter = null;
		return null;
	}
	
	public WOActionResults addIteration() {
		NSMutableArray queries = (NSMutableArray)rootQuery.valueForKey("queries");
		NSMutableDictionary query;
		if(queries == null || queries.count() <= iterations.count()) {
			query = new NSMutableDictionary();
		} else {
			query = (NSMutableDictionary)queries.objectAtIndex(iterations.count());
		}
		newIter = new NSMutableDictionary(query, "query");
		iter.takeValueForKey(null, "recent");
		iter = null;
		return null;
	}
	
	public WOActionResults cancelIteration() {
		if(iterations.count() == 0)
			return null;
		newIter = (NSMutableDictionary) iterations.objectAtIndex(0);
		newIter.takeValueForKey(Boolean.TRUE, "recent");
		newIter = null;
		return null;
	}
	
	public Boolean noIterations() {
		return iterations.count() == 0;
	}
	
	private static void formatParam (Object value, StringBuilder buf) {
		formatParam(value, buf, null);
	}
	private static boolean formatParam (Object value, StringBuilder buf, NSMutableArray set) {
		if(value instanceof NSArray) {
			if(set == null)
				set = new NSMutableArray();
			Enumeration enu = ((NSArray)value).objectEnumerator();
			while (enu.hasMoreElements()) {
				Object val = (Object) enu.nextElement();
				if(val instanceof NSDictionary) {
					NSDictionary dict = (NSDictionary)val;
					if(dict.count() == 1)
						val = dict.allValues().objectAtIndex(0);
				}
				if(formatParam(val,buf,set) && enu.hasMoreElements())
					buf.append(',').append(' ');
			}
			return false;
		}
		int idx = buf.length();
		if(value instanceof EOEnterpriseObject) {
			buf.append(MyUtility.getID((EOEnterpriseObject)value));
		} else if (value instanceof java.util.Date) {
			buf.append('\'').append(timestampFmt.format(value)).append('\'');
		} else if (value instanceof Number) {
			buf.append(value);
		} else {
			buf.append('\'').append(value).append('\'');
		}
		if(set == null)
			return true;
		String val = buf.substring(idx);
		if(set.containsObject(val)) {
			buf.delete(idx, buf.length());
			return false;
		} else {
			set.addObject(val);
			return true;
		}
	}
	
	private EOEditingContext ec(NSDictionary query) {
		String yearTag = (query==null)?null:(String)query.valueForKey("year");
		return ec(yearTag);
	}
	private EOEditingContext ec(String yearTag) {
		if(yearTag == null)
			return (EOEditingContext)ecs.valueForKey("curr");
		Object ec = ecs.valueForKey(yearTag);
		if(ec == null) {
			if(yearTag.equals("prev")) {
				ec = ec("-1");
			} else if(yearTag.equals("next")) {
				ec = ec("+1");
			} else {
				if(yearTag.charAt(0) == '+')
					yearTag = yearTag.substring(1);
				int year = Integer.parseInt(yearTag);
				if(year < 1000) {
					Integer curr = (Integer)session().valueForKey("eduYear");
					year+=curr.intValue();
				}
				yearTag = Integer.toString(year);
				ec = ecs.valueForKey(yearTag);
				if(ec == null) {
					EOObjectStore os = DataBaseConnector.objectStoreForTag(yearTag);
					if(os == null) {
						String msg = (String)session().valueForKeyPath(
							"strings.Strings.messages.unavailableYearlyDb");
						ec = String.format(msg,yearTag);
					} else {
						ec = new SessionedEditingContext(os, session());
					}
				}
			}
			ecs.takeValueForKey(ec, yearTag);			
		} // ecs.valueForKey(yearTag) == null
		if(ec instanceof String)
			throw new IllegalArgumentException((String)ec);
		if(ec instanceof EOEditingContext)
			return (EOEditingContext)ec;
		return null;
	}
	
	public NSArray performQuery(NSDictionary query,NSMutableArray result) {
		String sql = (String)query.valueForKey("sql");
		if(sql.indexOf('$') > 0) {
			StringBuilder buf = new StringBuilder(sql.length());
			int idx = sql.indexOf('$');
			int idx2 = -1;
			while (idx > 0 && idx < sql.length()) {
				buf.append(sql.substring(idx2 +1, idx));
				idx2 = sql.indexOf('$', idx +1);
				if(idx2 < 0)
					throw new IllegalArgumentException("Wrong sql format!");
				String keyPath = sql.substring(idx +1, idx2);
				Object value = params.valueForKeyPath(keyPath);
				if(value == null)
					throw new IllegalArgumentException("Parameter $" + keyPath + "$ is null!");
				formatParam(value,buf);
				idx = sql.indexOf('$', idx2 +1);
			}
			buf.append(sql.substring(idx2+1));
			sql = buf.toString();
		}
		EODatabaseContext dctx = null;
		NSArray cols = null;
		try {
			String modelName = (String)query.valueForKey("model");
			EOModel currentModel = EOModelGroup.defaultGroup().modelNamed(modelName);
			dctx = EODatabaseContext.registeredDatabaseContextForModel(currentModel, ec(query));
			dctx.lock();
			EODatabaseChannel dcnl = dctx.availableChannel();
			EOAdaptorChannel acnl = dcnl.adaptorChannel();
			if(!acnl.isOpen()) {
				acnl = dctx.adaptorContext().createAdaptorChannel();
				acnl.openChannel();
			}
			EOAdaptor adaptor = acnl.adaptorContext().adaptor();
			EOSQLExpressionFactory sqlFactory = adaptor.expressionFactory();
			EOSQLExpression expr = sqlFactory.expressionForString(sql);
			acnl.evaluateExpression(expr);
			cols = acnl.describeResults();
			if(cols != null && cols.count() > 0)
				cols = (NSArray)cols.valueForKey("name");
			while (true) {
				NSMutableDictionary row = acnl.fetchRow();
				if(row == null)
					break;
				result.addObject(row);
			}
		} finally {
			if(dctx != null)
				dctx.unlock();
		}
		return cols;
	}
	
	public WOActionResults saveQuery() {
		NSMutableDictionary toSave = rootQuery;
		NSArray parameters = (NSArray)rootQuery.valueForKey("parameters");
		if(parameters != null && parameters.count() > 0) {
			NSMutableDictionary defaults = new NSMutableDictionary(parameters.count());
			Enumeration enu = parameters.objectEnumerator();
			while (enu.hasMoreElements()) {
				NSDictionary param = (NSDictionary) enu.nextElement();
				String attribute = (String)param.valueForKey("attribute");
				defaults.takeValueForKey(params.valueForKey(attribute), attribute);
			}
			toSave = rootQuery.mutableClone();
			toSave.takeValueForKey(defaults, "defaults");
		}
		String data = NSPropertyListSerialization.xmlStringFromPropertyList(toSave);
		WOResponse response = application().createResponseInContext(context()); 
		response.setHeader("application/xml","Content-Type");
		response.setContent(data);
		response.setHeader("attachment; filename=\"query.plist\"","Content-Disposition");
		response.disableClientCaching();
		return response;
	}
	
	public void setQueryPlist(NSData data) {
		try {
			rootQuery = (NSMutableDictionary)NSPropertyListSerialization.propertyListFromData(data,"utf8");
		} catch (Exception e) {
			session().takeValueForKey(e.toString(), "message");
			return;
		}
		iterations.removeAllObjects();
		iter = null;
		if(rootQuery == null) {
			rootQuery = new NSMutableDictionary();
		} else {
			NSMutableArray queries = (NSMutableArray)rootQuery.valueForKey("queries");
			NSMutableDictionary query;
			if(queries != null && queries.count() > 0) {
				query = (NSMutableDictionary)queries.objectAtIndex(0);
			} else {
				query = new NSMutableDictionary();
				query.takeValueForKey(rootQuery.valueForKey("model"),"model");
				query.takeValueForKey(rootQuery.valueForKey("sql"),"sql");
				query.takeValueForKey(rootQuery.valueForKey("extInfo"),"extInfo");
				query.takeValueForKey(rootQuery.valueForKey("extQueries"),"extQueries");
			}
			newIter = new NSMutableDictionary(query, "query");
		}
		NSDictionary defaults = (NSDictionary)rootQuery.valueForKey("defaults");
		if(defaults != null && defaults.count() > 0) {
			Enumeration enu = defaults.keyEnumerator();
			while (enu.hasMoreElements()) {
				String key = (String) enu.nextElement();
				Object value = defaults.valueForKey(key);
				value = DisplayAny.ValueReader.evaluateValue(value, null, this);
				params.takeValueForKey(value, key);
			}
		}
	}
	
	public NSData queryPlist() {
		String data = NSPropertyListSerialization.xmlStringFromPropertyList(rootQuery);
		return new NSData(data, "utf8");
	}
	
    public boolean synchronizesVariablesWithBindings() {
        return false;
	}
	
	public boolean isStateless() {
		return false;
	}

	public WOActionResults addNewParam() {
		String newParamName = (String)params.valueForKey("newParamName");
		NSDictionary newParamType = (NSDictionary)params.valueForKey("newParamType");
//		message = (String)params.removeObjectForKey("uploadFilename");
		if(newParamType == null ||
				(newParamName == null && newParamType.valueForKey("attribute") == null))
			return null;
		NSDictionary dict = newParamType.mutableClone();
		if(newParamName != null)
			dict.takeValueForKey(newParamName, "attribute");
		NSMutableArray queryParams = (NSMutableArray)rootQuery.valueForKey("parameters");
		if(queryParams == null) {
			queryParams = new NSMutableArray(dict);
			rootQuery.takeValueForKey(queryParams, "parameters");
		} else {
			queryParams.addObject(dict);
		}
		params.takeValueForKey(null, "newParamName");
		params.takeValueForKey(null, "newParamType");
		return null;
	}
	
	public WOActionResults deleteParam() {
		if(rowItem == null)
			return null;
		NSMutableArray qplist = (NSMutableArray)rootQuery.valueForKey("parameters");
		if(qplist == null)
			return null;
		qplist.removeObject(rowItem);
		return null;
	}
	
	public NSDictionary extInfo() {
		if(colItem == null)
			return null;
		NSMutableDictionary extInfo = (NSMutableDictionary)iter.valueForKeyPath("query.extInfo");
		if(extInfo == null)
			return null;
		return (NSDictionary)extInfo.valueForKey((String)colItem);
	}
	
	public void setExtInfo(NSDictionary dict) {
		NSMutableDictionary query = (NSMutableDictionary)iter.valueForKey("query");
		NSMutableDictionary extInfo = (NSMutableDictionary)query.valueForKey("extInfo");
		if(extInfo == null) {
			extInfo = new NSMutableDictionary(dict,colItem);
			query.takeValueForKey(extInfo, "extInfo");
		} else {
			extInfo.takeValueForKey(dict, (String)colItem);
		}
//		applyExtInfo(dict, colName());
	}
	
	
	public NSArray extoptions() {
		NSArray list = (NSArray) plist.valueForKey("extoptions");
		NSDictionary dict = extInfo();
		if(dict == null || list.containsObject(dict))
			return list;
		else
			return list.arrayByAddingObject(dict);
	}
	
	public WOActionResults applyExtInfo() {
		NSMutableArray result = (NSMutableArray)iter.valueForKey("result");
		applyExtInfo(result,extInfo(),(String)colItem);
		return null;
	}

	public void applyExtInfo(NSMutableArray<NSDictionary> rows, NSDictionary dict, String colName) {
		if(rows == null || rows.count() == 0)
			return;
		String extCol =  colName + "_Ext";
		if(dict == null) {
			rows.takeValueForKey(null,extCol);
			return;
		}
		String entity = (String)dict.valueForKey("interface");
		if(entity != null) {
			entity = SettingsReader.stringForKeyPath("interfaces." + entity,null);
			if(entity != null)
				entity = entity.substring(1 + entity.lastIndexOf('.'));
		}
		if(entity == null)
			entity = (String)dict.valueForKey("entity");
		for (int i = 0; i < rows.count(); i++) {
			NSDictionary row = rows.objectAtIndex(i);
			Object id = row.valueForKey(colName);
			if(id == null) {
				if(row.valueForKey(extCol) != null)
					row.takeValueForKey(null, extCol);
				continue;
			}
			if(!(row instanceof NSMutableDictionary)) {
				row = row.mutableClone();
				rows.replaceObjectAtIndex(row, i);
			}
			EOEnterpriseObject eo=null;
			try {
				NSDictionary query = (NSDictionary)iter.valueForKey("query");
				eo = EOUtilities.faultWithPrimaryKeyValue(ec(query), entity, id);
			} catch (Exception e) {}
			row.takeValueForKey(eo, extCol);
		}
	}
	
	public Object extValue() {
		if(rowItem == null || colItem == null)
			return null;
		return rowItem.valueForKey(colItem + "_Ext");
	}

	public WOActionResults performExtQuery() {
		iter.takeValueForKey(null, "message");
		NSMutableDictionary extQuery = (NSMutableDictionary)colItem;
		String key = (extQuery == null)?null:(String)extQuery.valueForKey("key");
		if(key == null) {
			String sql = (String)iter.removeObjectForKey("extSql");
			if(sql == null)
				return null;
			extQuery = new NSMutableDictionary(3);
			extQuery.takeValueForKey(sql, "sql");
			extQuery.takeValueForKey(iter.removeObjectForKey(("extModel")), "model");
			extQuery.takeValueForKey(iter.removeObjectForKey(("extYear")), "year");
			NSMutableDictionary query = (NSMutableDictionary)iter.valueForKey("query");
			NSMutableArray extlist = (NSMutableArray)query.valueForKey("extQueries");
			if(extlist == null) {
				extlist = new NSMutableArray(extQuery);
				query.takeValueForKey(extlist, "extQueries");
			} else {
				extlist.addObject(extQuery);
			}
			key = "ext" + extlist.count();
			extQuery.takeValueForKey(key, "key");
		}
		NSArray found = (NSArray)iter.valueForKey("result");
		if(found == null || found.count() == 0)
			return null;
		Enumeration enu = found.objectEnumerator();
		while (enu.hasMoreElements()) {
			NSMutableDictionary row = (NSMutableDictionary) enu.nextElement();
			NSMutableArray result = new NSMutableArray();
			Object value = null;
			params.takeValueForKey(row, "row");
			try {
				NSArray cols = performQuery(extQuery,result);
				extQuery.takeValueForKey(cols, "cols");
				if(cols.count() > 0) {
					if(result.count() > 0)
						value = result;
					else
						value = "&oslash;";
				} else {
					value = "<em>OK</em>";
				}
				if(cols.count() == 1) {
					String title = (String)cols.objectAtIndex(0);
					extQuery.takeValueForKey(title, "title");
					if(result.count() == 1) {
						NSDictionary dict = (NSDictionary)result.objectAtIndex(0);
						value = dict.valueForKey(title);
						value = formatValue(value);
					}
				} else if (cols.count() > 1) {
					StringBuilder buf = new StringBuilder(
"<table style=\"border-color:#999999;border-collapse:collapse;text-align:center;\" cellspacing=\"0\" border=\"1\" align=\"center\"><tr>");
					Enumeration cenu = cols.objectEnumerator();
					while (cenu.hasMoreElements()) {
						String col = (String) cenu.nextElement();
						buf.append("<td>").append(col).append("</td>");
					}
					buf.append("</tr></table>");
					extQuery.takeValueForKey(buf.toString(), "title");
				}
			} catch (IllegalArgumentException e) {
				iter.takeValueForKey(e.getLocalizedMessage(), "message");
				found.takeValueForKey(null, key);
				return null;
			} catch (Exception e) {
				value = e.getLocalizedMessage();
			}
			row.takeValueForKey(value, key);
		} // rows enumeration
		params.takeValueForKey(null, "row");
		return null;
	}
	
	public WOComponent modelReport() {
		WOComponent page = pageWithName("ModelGroupReport");
		page.takeValueForKey(ecs.valueForKey("curr"), "ec");
		return page;
	}
	
	public WOActionResults exportCSV() {
		Export export = new ExportCSV(context(), (String)iter.valueForKeyPath("query.model"));
		NSArray cols = (NSArray)iter.valueForKey("cols");
		export.beginTitleRow();
		for (int i = 0; i < cols.count(); i++) {
			String col = (String)cols.objectAtIndex(i);
			export.addValue(col);
		}
		NSArray extCols = (NSArray)iter.valueForKeyPath("query.extQueries");
		if(extCols != null) {
			for (int i = 0; i < extCols.count(); i++) {
				NSMutableDictionary extQuery = (NSMutableDictionary)extCols.objectAtIndex(i);
				NSArray ecols = (NSArray)extQuery.valueForKey("cols");
				if(ecols.count() == 1) {
					export.addValue(ecols.objectAtIndex(0));
				} else {
					export.addValue(extQuery.valueForKey("key"));
				}
			}
		}
		export.endRow();
		NSArray result = (NSArray)iter.valueForKey("result");
		Enumeration enu = result.objectEnumerator();
		while (enu.hasMoreElements()) {
			NSMutableDictionary row = (NSMutableDictionary) enu.nextElement();
			export.beginRow();
			for (int i = 0; i < cols.count(); i++) {
				String col = (String)cols.objectAtIndex(i);
				Object value = row.valueForKey(col);
				if(value == null || value == NullValue)
					value = "NULL";
				else
					value = formatValue(value);
				Object ext = row.valueForKey(col + "_Ext");
				if(ext != null) {
					if(ext instanceof EOEnterpriseObject) {
						String format = (String)iter.valueForKeyPath(
								"query.extInfo." + col + ".displayDict.format");
						
						if(format == null) {
							Object inPlist = iter.valueForKeyPath(
									"query.extInfo." + col + ".displayDict.value");
							if(inPlist == null)
								ext = null;
							else
								ext = DisplayAny.ValueReader.evaluateValue(inPlist,ext, this);
						} else {
							ext = DisplayAny.formatObject(format, ext);
						}
					} else {
						ext = formatValue(ext);
					}
					if(ext != null) {
						StringBuilder buf = new StringBuilder(value.toString());
						buf.append(' ').append('(').append(ext).append(')');
						value = buf.toString();
					}
				}
				export.addValue(value);
			}
			if(extCols == null)
				continue;
			for (int i = 0; i < extCols.count(); i++) {
				NSMutableDictionary extQuery = (NSMutableDictionary)extCols.objectAtIndex(i);
				String key = (String)extQuery.valueForKey("key");
				Object val = row.valueForKey(key);
				if(val instanceof NSArray) {
					NSArray ecols = (NSArray)extQuery.valueForKey("cols");
					if(ecols.count() == 1)
						val = ((NSArray)val).valueForKey((String)ecols.objectAtIndex(0));
					if(((NSArray)val).count() == 1)
						val = ((NSArray)val).objectAtIndex(0);
				}
				export.addValue(val);
			}
			export.endRow();
		}
		return export;

	}
	
}