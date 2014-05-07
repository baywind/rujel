package net.rujel.ui;

import com.webobjects.appserver.WOContext;
import com.webobjects.appserver.WOComponent;
import com.webobjects.appserver.WOMessage;
import com.webobjects.appserver.WOResponse;
import com.webobjects.eoaccess.EOAdaptor;
import com.webobjects.eoaccess.EOAdaptorChannel;
import com.webobjects.eoaccess.EOAttribute;
import com.webobjects.eoaccess.EODatabaseChannel;
import com.webobjects.eoaccess.EODatabaseContext;
import com.webobjects.eoaccess.EOModel;
import com.webobjects.eoaccess.EOModelGroup;
import com.webobjects.eoaccess.EOSQLExpression;
import com.webobjects.eoaccess.EOSQLExpressionFactory;
import com.webobjects.eoaccess.EOUtilities;
import com.webobjects.eocontrol.EOEditingContext;
import com.webobjects.eocontrol.EOEnterpriseObject;
import com.webobjects.eocontrol.EOSortOrdering;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSData;
import com.webobjects.foundation.NSDictionary;
import com.webobjects.foundation.NSForwardException;
import com.webobjects.foundation.NSKeyValueCoding;
import com.webobjects.foundation.NSMutableArray;
import com.webobjects.foundation.NSMutableDictionary;
import com.webobjects.foundation.NSPropertyListSerialization;

import java.io.InputStream;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Enumeration;

import net.rujel.base.MyUtility;
import net.rujel.reusables.SessionedEditingContext;
import net.rujel.reusables.SettingsReader;

import com.webobjects.appserver.WOActionResults;

public class QueryPage extends WOComponent {
    public NSDictionary plist;
    public NSDictionary query;
	public NSMutableArray<NSDictionary> result;
	public NSArray cols;
	public Object colItem;
	public NSDictionary rowItem;
	public String message;
	public EOEditingContext ec;
	public NSArray models;
	public EOModel currentModel;
	public NSMutableDictionary params;

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
			throw new NSForwardException(e,"Error reading Overview.pist");
		}
        ec = new SessionedEditingContext(context.session());
        EOSortOrdering so = new EOSortOrdering("name",
        		EOSortOrdering.CompareCaseInsensitiveAscending);
        models = EOSortOrdering.sortedArrayUsingKeyOrderArray
        		(EOModelGroup.defaultGroup().models(), new NSArray(so));
        params = new NSMutableDictionary();
        query = new NSMutableDictionary();
    }

	public String colName() {
		if(colItem == null)
			return null;
		if(colItem instanceof String)
			return (String)colItem;
		else
			return (String)NSKeyValueCoding.Utility.valueForKey(colItem, "name");
	}
	
	public Object value() {
		if(rowItem == null || colItem == null)
			return null;
		Object value = rowItem.valueForKey(colName());
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

	public WOActionResults performQuery() {
		String sql = (String)query.valueForKey("sql");
		if(sql.indexOf('$') > 0) {
			StringBuilder buf = new StringBuilder(sql.length());
			int idx = sql.indexOf('$');
			int idx2 = -1;
			while (idx > 0 && idx < sql.length()) {
				buf.append(sql.substring(idx2 +1, idx));
				idx2 = sql.indexOf('$', idx +1);
				if(idx2 < 0) {
					message = "wrong sql format!";
					return null;
				}
				String keyPath = sql.substring(idx +1, idx2);
				Object value = params.valueForKeyPath(keyPath);
				if(value == null) {
					message = "Parameter $" + keyPath + "$ is null!";
					return null;
				}
				if(value instanceof EOEnterpriseObject)
					value = MyUtility.getID((EOEnterpriseObject)value);
				else if (value instanceof java.util.Date)
					buf.append(timestampFmt.format(value));
				buf.append(value);
				idx = sql.indexOf('$', idx2 +1);
			}
			buf.append(sql.substring(idx2+1));
			sql = buf.toString();
		}
		query.takeValueForKey(currentModel.name(), "model");
		EODatabaseContext dctx = EODatabaseContext.registeredDatabaseContextForModel(
				currentModel, ec);
		try {
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
			result = new NSMutableArray<NSDictionary>();
			while (true) {
				NSMutableDictionary row = acnl.fetchRow();
				if(row == null)
					break;
				result.addObject(row);
			}
			message = "Found: " + result.count();
		} catch (Exception e) {
			message = e.getLocalizedMessage();
		} finally {
			dctx.unlock();
		}
		if(result == null || result.count() == 0)
			return null;
		NSMutableDictionary extInfo = (NSMutableDictionary)query.valueForKey("extInfo");
		if(extInfo == null || extInfo.count() == 0)
			return null;
		Enumeration enu = cols.objectEnumerator();
		while (enu.hasMoreElements()) {
			EOAttribute col = (EOAttribute) enu.nextElement();
			NSDictionary dict = (NSDictionary)extInfo.valueForKey(col.name());
			if(dict != null)
				applyExtInfo(dict, col.name());
		}
		return null;
	}
	
	public WOActionResults saveQuery() {
		String data = NSPropertyListSerialization.xmlStringFromPropertyList(query);
		WOResponse response = application().createResponseInContext(context()); 
		response.setHeader("application/xml","Content-Type");
		response.setContent(data);
		response.setHeader("attachment; filename=\"query.plist\"","Content-Disposition");
		response.disableClientCaching();
		return response;
	}
	
	public void setQueryPlist(NSData data) {
		try {
			query = (NSDictionary)NSPropertyListSerialization.propertyListFromData(data,"utf8");
			String modelName = (String)query.valueForKey("model");
			currentModel = EOModelGroup.defaultGroup().modelNamed(modelName);
		} catch (Exception e) {
			message = e.toString();
		}
	}
	
	public NSData queryPlist() {
		return null;
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
		NSMutableArray queryParams = (NSMutableArray)query.valueForKey("parameters");
		if(queryParams == null) {
			queryParams = new NSMutableArray(dict);
			query.takeValueForKey(queryParams, "parameters");
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
		NSMutableArray qplist = (NSMutableArray)query.valueForKey("parameters");
		if(qplist == null)
			return null;
		qplist.removeObject(rowItem);
		return null;
	}
	
	public NSDictionary extInfo() {
		if(colItem == null)
			return null;
		NSMutableDictionary extInfo = (NSMutableDictionary)query.valueForKey("extInfo");
		if(extInfo == null)
			return null;
		return (NSDictionary)extInfo.valueForKey(colName());
	}
	
	public void setExtInfo(NSDictionary dict) {
		NSMutableDictionary extInfo = (NSMutableDictionary)query.valueForKey("extInfo");
		if(extInfo == null) {
			extInfo = new NSMutableDictionary(dict,colName());
			query.takeValueForKey(extInfo, "extInfo");
		} else {
			extInfo.takeValueForKey(dict, colName());
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
		applyExtInfo(extInfo(),colName());
		return null;
	}

	public void applyExtInfo(NSDictionary dict, String colName) {
		if(result == null || result.count() == 0)
			return;
		String extCol =  colName + "_Ext";
		if(dict == null) {
			result.takeValueForKey(null,extCol);
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
		for (int i = 0; i < result.count(); i++) {
			NSDictionary row = result.objectAtIndex(i);
			Object id = row.valueForKey(colName);
			if(id == null) {
				if(row.valueForKey(extCol) != null)
					row.takeValueForKey(null, extCol);
				continue;
			}
			if(!(row instanceof NSMutableDictionary)) {
				row = row.mutableClone();
				result.replaceObjectAtIndex(row, i);
			}
			EOEnterpriseObject eo=null;
			try {
				eo = EOUtilities.faultWithPrimaryKeyValue(ec, entity, id);
			} catch (Exception e) {}
			row.takeValueForKey(eo, extCol);
		}
	}
	
	public Object extValue() {
		if(rowItem == null || colItem == null)
			return null;
		return rowItem.valueForKey(colName() + "_Ext");
	}
}