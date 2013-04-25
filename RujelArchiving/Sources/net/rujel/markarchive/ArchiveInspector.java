package net.rujel.markarchive;

import java.util.Date;
import java.util.Enumeration;

import net.rujel.base.MyUtility;
import net.rujel.reusables.DisplayAny;
import net.rujel.reusables.Various;

import com.webobjects.appserver.WOActionResults;
import com.webobjects.appserver.WOContext;
import com.webobjects.appserver.WOComponent;
import com.webobjects.appserver.WOMessage;
import com.webobjects.eoaccess.EOModelGroup;
import com.webobjects.eoaccess.EOObjectNotAvailableException;
import com.webobjects.eoaccess.EOUtilities;
import com.webobjects.eocontrol.EOAndQualifier;
import com.webobjects.eocontrol.EOEnterpriseObject;
import com.webobjects.eocontrol.EOFetchSpecification;
import com.webobjects.eocontrol.EOKeyValueQualifier;
import com.webobjects.eocontrol.EOQualifier;
import com.webobjects.eocontrol.EOSortOrdering;
import com.webobjects.foundation.*;

public class ArchiveInspector extends WOComponent {
	
	public NSMutableDictionary dict;
	public ReadArchives returnPage;
	public NSKeyValueCoding grouping;
	public Object initData;
	protected MarkArchive ma;

	public NSMutableArray params;
	public Object item;
	public Object item2;
	public NSMutableDictionary children;
	public boolean inChildren;
	
	public NSArray list;
	public NSKeyValueCodingAdditions valueOf = new DisplayAny.ValueReader(this);
	
    public ArchiveInspector(WOContext context) {
        super(context);
    }
    
	public void setDict(NSMutableDictionary dict) {
		this.dict = dict;
		Object arch = dict.valueForKey("arch");
		NSDictionary ue = (NSDictionary)dict.valueForKey("usedEntity");
		String groupingKey = (String)ue.valueForKey("grouping");
		if(arch instanceof MarkArchive) {
			ma = (MarkArchive)arch;
			grouping = null;
			list = prehistory((MarkArchive)arch);
			if(ma.actionType().intValue() >= 3)
				prepareChildren();
			groupingKey = null;
		} else if(arch instanceof NSArray) {
			ma = (MarkArchive)((NSArray)arch).objectAtIndex(0);
			if(groupingKey == null)
				list = prehistory(ma);
			else
				list = (NSArray)arch;
		}
		NSArray pDicts = (NSArray)ue.valueForKey("params");
		if(pDicts == null)
			return;
		initData = DisplayAny.ValueReader.evaluateValue(ue.valueForKey("initData"), ma, this);
		params = new NSMutableArray();
		for (int i = 0; i < pDicts.count(); i++) {
			NSDictionary pd = (NSDictionary)pDicts.objectAtIndex(i);
			String key = (String)pd.valueForKey("key");
			if(key == null)
				continue;
			if(key.equals(groupingKey)) {
				grouping = pd;
				continue;
			}
			NSMutableDictionary param = new NSMutableDictionary();
			param.takeValueForKey(key, "key");
			param.takeValueForKey(pd.valueForKey("title"), "title");
			params.addObject(param);
			param.takeValueForKey(pd.valueForKey("displayDict"), "displayDict");
			Object value = getParam(ma, pd, this);
			if(value instanceof StringBuilder) {
				NSArray values = (NSArray)pd.valueForKey("parentValues");
				if(values != null && parentObject() != null) {
					StringBuilder buf = new StringBuilder("<span class = \"dimtext\">");
					for (int j = 0; j < values.count(); j++) {
						if(j > 0)
							buf.append(" : ");
						Object val = values.objectAtIndex(j);
						val = DisplayAny.ValueReader.evaluateValue(val, parentObject(), this);
						if(val instanceof Date)
							val = MyUtility.dateFormat().format(val);
						else if(val == null)
							val = "&lt;?null?&gt;";
						else
							val = WOMessage.stringByEscapingHTMLString(val.toString());
						buf.append(val);
					}
					buf.append("</span>");
					value = buf.toString();
				}
			}
			param.takeValueForKey(value, "value");
		}
	}

	protected static Object getParam(MarkArchive ma,NSKeyValueCoding dict,WOComponent page) {
		if(dict == null)
			return null;
		Integer id = ma.getKeyValue((String)dict.valueForKey("key"));
		String entityName = (String)dict.valueForKey("entityName");
		if(EOModelGroup.defaultGroup().entityNamed(entityName) == null) {
			try {
				Class tIf = Class.forName("net.rujel.interfaces." + entityName);
				entityName = (String)tIf.getDeclaredField("entityName").get(null);
			} catch (Exception e) {
				StringBuilder buf = new StringBuilder();
				buf.append("&lt;?").append(entityName).append("?:").append(id).append("&gt;");
				return buf;
			}
		}
		try {
			EOEnterpriseObject eo = EOUtilities.objectWithPrimaryKeyValue(
					ma.editingContext(), entityName, id);
			Object value = eo;
			String key = (String)dict.valueForKey("keyPath");
			if(key != null)
				value = eo.valueForKeyPath(key);
			NSArray values = (NSArray)dict.valueForKey("subValues");
			if(values == null)
				return value;
			StringBuilder buf = new StringBuilder();
			for (int j = 0; j < values.count(); j++) {
				if(j > 0)
					buf.append(" : ");
				Object val = values.objectAtIndex(j);
				val = DisplayAny.ValueReader.evaluateValue(val, value, page);
				if(val instanceof Date)
					val = MyUtility.dateFormat().format(val);
				buf.append(val);
			}
			return WOMessage.stringByEscapingHTMLString(buf.toString());
		} catch (EOObjectNotAvailableException e) {
			StringBuilder buf = new StringBuilder();
			buf.append("&lt;").append(entityName).append(':').append(id).append("&gt;");
			return buf;
		} catch (Exception e) {
			return WOMessage.stringByEscapingHTMLString(e.toString());
		}
	}

	public Object groupingValue() {
		return getParam((MarkArchive)item, grouping, this);
	}

	public static NSArray prehistory(MarkArchive arch) {
		NSDictionary params = arch.valuesForKeys(MarkArchive.idKeys);
		EOQualifier qual = EOQualifier.qualifierToMatchAllValues(params);
		NSArray sorter = new NSArray(
				new EOSortOrdering(MarkArchive.TIMESTAMP_KEY, EOSortOrdering.CompareAscending));
		EOFetchSpecification fs = new EOFetchSpecification(MarkArchive.ENTITY_NAME,qual,sorter);
		return arch.editingContext().objectsWithFetchSpecification(fs);
	}

	public String rowClass() {
		if(!(item instanceof MarkArchive))
			return null;
		return ReadArchives.rowClass((MarkArchive)item);
	}
	
	protected MarkArchive chooseParent() {
		for (int i = list.count() -1; i >= 0; i--) {
			MarkArchive test = (MarkArchive)list.objectAtIndex(i);
			if(test.actionType().intValue() < 3) {
				return test;
			}
		}
		return (MarkArchive)list.lastObject();
	}
	
	protected MarkArchive _parent;
	public MarkArchive parentObject() {
		if(inChildren)
			return chooseParent();
		if(_parent != null)
			return _parent;
		NSDictionary pDict = (NSDictionary)dict.valueForKeyPath("usedEntity.parent");
		if(pDict == null)
			return null;
		String pKey = (String)pDict.valueForKey("parentKey");
		Integer id = ma.getKeyValue(pKey);
		pKey = (String)pDict.valueForKey("searchKey");
		String entityName = (String)pDict.valueForKey("entityName");
		EOQualifier archQual = MarkArchive.archiveQualifier(entityName,
				new NSDictionary(id,pKey), ma.editingContext());
		NSMutableArray quals = null;
		if(archQual instanceof EOAndQualifier) {
			quals = ((EOAndQualifier)archQual).qualifiers().mutableClone();
		} else {
			quals = new NSMutableArray(archQual);
		}
		quals.addObject(new EOKeyValueQualifier(MarkArchive.TIMESTAMP_KEY, 
				EOQualifier.QualifierOperatorLessThanOrEqualTo,
				dict.valueForKey(MarkArchive.TIMESTAMP_KEY)));
		quals.addObject(new EOKeyValueQualifier(MarkArchive.ACTION_TYPE_KEY,
					EOQualifier.QualifierOperatorLessThan, new Integer(3)));
		archQual = new EOAndQualifier(quals);
		EOFetchSpecification fs = new EOFetchSpecification(MarkArchive.ENTITY_NAME,
				archQual,MarkArchive.backSorter);
		NSArray found = ma.editingContext().objectsWithFetchSpecification(fs);
		if(found == null || found.count() == 0)
			return null;
		_parent = (MarkArchive)found.objectAtIndex(0);
		return _parent;
	}
	
	protected void prepareChildren() {
		children = new NSMutableDictionary();
		NSDictionary pDict = (NSDictionary)dict.valueForKeyPath("usedEntity.children");
		if(pDict == null)
			return;
		String pKey = (String)pDict.valueForKey("searchKey");
		Integer id = ma.getKeyValue(pKey);
		pKey = (String)pDict.valueForKey("parentKey");
		String entityName = (String)pDict.valueForKey("entityName");
		EOQualifier archQual = MarkArchive.archiveQualifier(entityName,
				new NSDictionary(id,pKey), ma.editingContext());
		NSArray sort = new NSArray(new EOSortOrdering[] {
				new EOSortOrdering(MarkArchive.KEY1_KEY,EOSortOrdering.CompareAscending),
				new EOSortOrdering(MarkArchive.KEY2_KEY,EOSortOrdering.CompareAscending),
				new EOSortOrdering(MarkArchive.KEY3_KEY,EOSortOrdering.CompareAscending),
				new EOSortOrdering(MarkArchive.TIMESTAMP_KEY,EOSortOrdering.CompareDescending)
		});
		EOFetchSpecification fs = new EOFetchSpecification(MarkArchive.ENTITY_NAME,archQual,sort);
		NSArray found = ma.editingContext().objectsWithFetchSpecification(fs);
		if(found == null || found.count() == 0)
			return;
		NSMutableArray result = new NSMutableArray();
		MarkArchive curr = null;
		Enumeration enu = found.objectEnumerator();
		NSDictionary entDict = null;
		NSDictionary cGrouping = null;
		NSMutableDictionary grValues = new NSMutableDictionary();
		while (enu.hasMoreElements()) {
			MarkArchive arch = (MarkArchive) enu.nextElement();
			if(entDict == null) {
				entDict = returnPage.entDict(arch.usedEntity());
				NSArray cParams = (NSArray)entDict.valueForKey("params");
				String grParam = (String)entDict.valueForKey("grouping");
				if(cParams != null && grParam != null) {
					for (int i = 0; i < cParams.count(); i++) {
						NSDictionary param = (NSDictionary)cParams.objectAtIndex(i);
						if(grParam.equals(param.valueForKey("key"))) {
							cGrouping = param;
							break;
						}
					}
				}
				children.takeValueForKey(entDict, "usedEntity");
				children.takeValueForKey(cGrouping, "grouping");
			} // if(entDict == null)
			if(curr != null && curr.sameIdentifier(arch))
				continue;
			curr = arch;
			NSMutableDictionary cDict = new NSMutableDictionary(arch,"arch");
			Object grValue = getParam(arch, cGrouping, this);
			if(grValue != null) {
				grValues.setObjectForKey(grValue, arch);
				cDict.takeValueForKey(grValue, "grouping");
			}
			result.addObject(cDict);
		} // found.objectEnumerator();
		if(result.count() > 1) {
			NSArray sorter = new NSArray(
					new EOSortOrdering("grouping",EOSortOrdering.CompareAscending));
			EOSortOrdering.sortArrayUsingKeyOrderArray(result, sorter);
		} else if (result.count() == 0) {
			return;
		}
		if(grValues.count() > 0)
			children.takeValueForKey(grValues, "grValues");
		children.takeValueForKey(Boolean.TRUE, "hasChildren");
		NSArray res = (NSArray)result.valueForKey("arch");
		children.takeValueForKey(res, "list");
	}
	
	public WOActionResults expandGrouped() {
		NSMutableDictionary newDict = null;
		if(inChildren) {
			newDict = new NSMutableDictionary(item,"arch");
			newDict.takeValueForKey(children.valueForKey("usedEntity"),"usedEntity");
			newDict.takeValueForKey(valueForKeyPath("item.timestamp"), MarkArchive.TIMESTAMP_KEY);
			newDict.takeValueForKey(dict.valueForKey("course"), "course");
		} else {
			newDict = dict.mutableClone();
		}
		newDict.removeObjectForKey("multiplier");
		newDict.takeValueForKey(item, "arch");
		newDict.takeValueForKey(this, "returnPage");
		String reason = ((MarkArchive)item).reason();
		if(reason != null)
			reason = WOMessage.stringByEscapingHTMLString(reason);
		newDict.takeValueForKey(reason, "reason");
		WOComponent inspector = pageWithName("ArchiveInspector");
		inspector.takeValueForKey(newDict, "dict");
		inChildren = false;
		return inspector;
	}
	
	public Boolean noReturn() {
		if(item == null || dict.valueForKey("returnPage") == null)
			return Boolean.TRUE;
		String groupingKey = (String)dict.valueForKeyPath("usedEntity.grouping");
		if(groupingKey == null)
			return Boolean.TRUE;
		if(groupingKey.equals(NSKeyValueCoding.Utility.valueForKey(item, "key")))
			return Boolean.FALSE;
		return Boolean.TRUE;
	}
	
	public Object childGroupingValue() {
		if(item == null || children == null)
			return null;
		NSMutableDictionary grValues = (NSMutableDictionary)children.valueForKey("grValues");
		if(grValues == null)
			return null;
		return grValues.objectForKey(item);
	}
	
	public Object initData() {
		if(inChildren) {
			MarkArchive mItem = (MarkArchive)item;
			Object childInit = valueForKeyPath("children.usedEntity.initData");
			if(childInit == null)
				return null;
			return DisplayAny.ValueReader.evaluateValue(childInit, mItem, this);
		}
		return initData;
	}
	
	public Object enterChildren() {
		Object has = valueForKeyPath("children.hasChildren");
		inChildren = Various.boolForObject(has);
		return has;
	}
	
	public String leaveChildren() {
		inChildren = false;
		return null;
	}
	
	public WOActionResults openCourse() {
		WOComponent result = pageWithName("LessonNoteEditor");
		result.takeValueForKey(dict.valueForKey("course"), "course");
		session().takeValueForKey(returnPage,"pushComponent");
		return result;
	}
}