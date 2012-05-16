//  ExtSystem.java

/*
 * Copyright (c) 2008, Gennady & Michael Kushnir
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 * 
 * 	o	Redistributions of source code must retain the above copyright notice, this
 * 		list of conditions and the following disclaimer.
 * 	o	Redistributions in binary form must reproduce the above copyright notice,
 * 		this list of conditions and the following disclaimer in the documentation
 * 		and/or other materials provided with the distribution.
 * 	o	Neither the name of the RUJEL nor the names of its contributors may be used
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

import java.util.Enumeration;
import java.util.UUID;

import net.rujel.base.EntityIndex;
import net.rujel.base.MyUtility;
import net.rujel.reusables.PlistReader;
import net.rujel.reusables.Various;

import com.webobjects.eoaccess.EOUtilities;
import com.webobjects.eocontrol.*;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSDictionary;
import com.webobjects.foundation.NSMutableArray;
import com.webobjects.foundation.NSMutableDictionary;
import com.webobjects.foundation.NSMutableSet;

public class ExtSystem extends _ExtSystem {

	public void awakeFromInsertion(EOEditingContext ec) {
		super.awakeFromInsertion(ec);
	}

	public void turnIntoFault(EOFaultHandler handler) {
		super.turnIntoFault(handler);
	}
	
	public String extidForObject(EOEnterpriseObject eo, ExtBase base) {
		if(eo == null)
			return null;
		EOKeyGlobalID gid = (EOKeyGlobalID)eo.editingContext().globalIDForObject(eo);
		SyncMatch match = SyncMatch.matchForSystemAndObject(this, base, gid);
		if(match == null) {
			if(base != null && base.isLocalBase()) {
				match = addMatch(gid,base);
				String guid = UUID.randomUUID().toString();
				match.setEntity(gid.entityName());
				match.setExtID(guid);
//				editingContext().saveChanges();
				return guid;
			}
			return null;
		}
		return match.extID();
	}
	
	public SyncMatch addMatch(EOKeyGlobalID gid, ExtBase base) {
		SyncMatch match = (SyncMatch)EOUtilities.createAndInsertInstance(editingContext(),
				SyncMatch.ENTITY_NAME);
		match.setExtSystem(this);
		if(base != null)
		match.setExtBase(base);
		match.setObjID((Integer)gid.keyValues()[0]);
		return match;
	}
	
	public NSMutableDictionary dictForObjects(NSArray list, ExtBase base) {
		if(list == null || list.count() == 0)
			return null;
		EOEditingContext ec = editingContext();
		NSMutableDictionary dict = new NSMutableDictionary();
		String entityName = null;
		NSMutableArray ids = new NSMutableArray(list.count());
		Enumeration enu = list.objectEnumerator();
		int last = 0;
		while (enu.hasMoreElements()) {
			EOEnterpriseObject obj = (EOEnterpriseObject) enu.nextElement();
			if(entityName == null) {
				entityName = obj.entityName();
			} else if(!entityName.equals(obj.entityName())) {
				matchDict(entityName, ids, dict, list, last, base);
				entityName = obj.entityName();
				last += ids.count();
				ids.removeAllObjects();
			}
			EOKeyGlobalID gid = (EOKeyGlobalID)obj.editingContext().globalIDForObject(obj);
			ids.addObject(gid.keyValues()[0]);
		}
		matchDict(entityName, ids, dict, list, last, base);
		if(ec.hasChanges())
			ec.saveChanges();
		return dict;
	}
	
	private void matchDict(String entityName, NSMutableArray ids, NSMutableDictionary dict,
			NSArray list, int last, ExtBase base) {
		EntityIndex ei = EntityIndex.indexForEntityName(editingContext(), entityName, false);
		EOQualifier[] qual = new EOQualifier[2];
		qual[0] = SyncMatch.matchQualifier(this, base, ei, null, null);
		qual[1] = Various.getEOInQualifier(SyncMatch.OBJ_ID_KEY, ids);
		qual[1] = new EOAndQualifier(new NSArray(qual));
		EOFetchSpecification fs = new EOFetchSpecification(SyncMatch.ENTITY_NAME,qual[1],null);
		NSArray found = editingContext().objectsWithFetchSpecification(fs);
		SyncMatch[] ml = null;
		if(base.isLocalBase())
			ml = new SyncMatch[ids.count()];
		else if(found == null || found.count() == 0)
			return;
		Enumeration enu = found.objectEnumerator();
		NSMutableSet used = new NSMutableSet();
		while (enu.hasMoreElements()) {
			SyncMatch match = (SyncMatch) enu.nextElement();
			Integer id = match.objID();
			if(used.containsObject(id))
				continue;
			int idx = ids.indexOf(id);
			String extID = match.extID();
			if(extID == null) extID = "";
			if(idx < 0) {
				dict.setObjectForKey(extID, id);
				continue;
			}
			if(ml != null)
				ml[idx] = match;
			EOEnterpriseObject eo = (EOEnterpriseObject)list.objectAtIndex(last + idx);
			dict.setObjectForKey(extID, eo);
			if(match.eduYear() != null)
				used.addObject(id);
		}
		if(ml != null) {
			Integer eduYear = (ei.isYearly())?eduYear = MyUtility.eduYear(editingContext()):null;
			for (int i = 0; i < ml.length; i++) {
				if(ml[i] != null)
					continue;
				EOEnterpriseObject eo = (EOEnterpriseObject)list.objectAtIndex(last + i);
				ml[i] = (SyncMatch)EOUtilities.createAndInsertInstance(editingContext(),
						SyncMatch.ENTITY_NAME);
				ml[i].setExtSystem(this);
				ml[i].setExtBase(base);
				ml[i].setObjID((Integer)ids.objectAtIndex(i));
				ml[i].setEntityIndex(ei);
				ml[i].setEduYear(eduYear);
				String guid = UUID.randomUUID().toString();
				ml[i].setExtID(guid);
				dict.setObjectForKey(guid, eo);
			}
		}
	}

	public static ExtSystem extSystemNamed(String name, EOEditingContext ec, boolean create) {
		NSArray rjls = EOUtilities.objectsMatchingKeyAndValue(ec, ENTITY_NAME, 
				PRODUCT_NAME_KEY, name);
		if(rjls == null || rjls.count() == 0) {
			if(!create)
				return null;
			ExtSystem result = (ExtSystem)EOUtilities.createAndInsertInstance(ec, ENTITY_NAME);
			result.setProductName(name);
			return result;
		}
		return (ExtSystem)rjls.objectAtIndex(0);
	}
	
	public ExtBase getBase(String base, boolean create) {
		NSArray bases = extBases();
		if(bases != null && bases.count() > 0) {
			for (int i = 0; i < bases.count(); i++) {
				ExtBase bs = (ExtBase)bases.objectAtIndex(i);
				if(base.equals(bs.baseID()))
					return bs;
			}
		}
		if(!create)
			return null;
		ExtBase bs = (ExtBase)EOUtilities.createAndInsertInstance(
				editingContext(), ExtBase.ENTITY_NAME);
		bs.setBaseID(base);
		addObjectToBothSidesOfRelationshipWithKey(bs, EXT_BASES_KEY);
//		editingContext().saveChanges();
		return bs;
	}

	public NSMutableDictionary getDataDict(ExtBase base) {
		NSArray data = extData();
		if(data == null || data.count() == 0)
			return null;
		NSMutableDictionary dict = new NSMutableDictionary();
		Enumeration enu = data.objectEnumerator();
		while (enu.hasMoreElements()) {
			EOEnterpriseObject dataRow = (EOEnterpriseObject) enu.nextElement();
			ExtBase rb = (ExtBase)dataRow.valueForKey("extBase");
			if(rb != null && rb != base)
				continue;
			String key = (String)dataRow.valueForKey("key");
			String value = (String)dataRow.valueForKey("value");
			dict.takeValueForKey(value, key);
		}
		return dict;
	}
	
	public NSMutableDictionary getIndexes(ExtBase base) {
		NSArray data = syncIndexes();
		if(data == null || data.count() == 0)
			return null;
		Integer eduYear = MyUtility.eduYear(editingContext());
		NSMutableDictionary dict = new NSMutableDictionary();
		Enumeration enu = data.objectEnumerator();
		while (enu.hasMoreElements()) {
			SyncIndex ind = (SyncIndex) enu.nextElement();
			if(ind.extBase() != null && ind.extBase() != base)
				continue;
			if(ind.eduYear() != null && !ind.eduYear().equals(eduYear))
				continue;
			dict.takeValueForKey(ind.getDict(), ind.indexName());
		}
		return dict;
	}
	
	public void setIndexes(NSDictionary indexes, ExtBase base) {
		NSArray data = syncIndexes();
		NSMutableArray toChangeInd = null;
		NSMutableArray toChangeRows = new NSMutableArray();
		if(data != null && data.count() > 0) {
			indexes = PlistReader.cloneDictionary(indexes, true);
			Integer eduYear = MyUtility.eduYear(editingContext());
			Enumeration enu = data.objectEnumerator();
			while (enu.hasMoreElements()) {
				SyncIndex ind = (SyncIndex) enu.nextElement();
				if(ind.extBase() != null && ind.extBase() != base)
					continue;
				if(ind.eduYear() != null && !ind.eduYear().equals(eduYear))
					continue;
				NSMutableDictionary dict = (NSMutableDictionary)
					indexes.valueForKey(ind.indexName());
				if(dict == null) {
					if(toChangeInd == null)
						toChangeInd = new NSMutableArray(ind);
					else
						toChangeInd.addObject(ind);
					toChangeRows.addObjectsFromArray(ind.indexMatches());
					continue;
				}
				NSArray matches = ind.indexMatches();
				if(matches == null || matches.count() == 0)
					continue;
				Enumeration menu = matches.objectEnumerator();
				while (menu.hasMoreElements()) {
					EOEnterpriseObject match = (EOEnterpriseObject) menu.nextElement();
					String local = (String)match.valueForKey("localValue");
					String value = (String)dict.removeObjectForKey(local);
					if(value == null) {
						toChangeRows.addObject(match);
					} else if(!value.equals(match.valueForKey("extValue"))) {
						match.takeValueForKey(value, "extValue");
					}
				} // index rows enumeration
				if(dict.count() == 0)
					indexes.remove(ind.indexName());
			} // indexes enumeration
		} // if Sys has indexes
		EOEditingContext ec = editingContext();
		if(indexes != null && indexes.count() > 0) {
			Enumeration enu = indexes.keyEnumerator();
			while (enu.hasMoreElements()) {
				String indexName = (String)enu.nextElement();
				NSMutableDictionary dict = (NSMutableDictionary) indexes.valueForKey(indexName);
				if(dict.count() == 0)
					continue;
				SyncIndex ind = (toChangeInd == null)? null :
					(SyncIndex)toChangeInd.removeLastObject();
				if(ind == null) {
					ind = (SyncIndex)EOUtilities.createAndInsertInstance(ec, SyncIndex.ENTITY_NAME);
					addObjectToBothSidesOfRelationshipWithKey(ind, SYNC_INDEXES_KEY);
				}
				ind.setIndexName(indexName);
				ind.setExtBase(base);
				Enumeration medu = dict.keyEnumerator();
				while (medu.hasMoreElements()) {
					String local = (String) medu.nextElement();
					EOEnterpriseObject match = (EOEnterpriseObject)toChangeRows.removeLastObject();
					if(match == null)
						match = EOUtilities.createAndInsertInstance(ec,"IndexMatch");
					if(match.valueForKey("syncIndex") != ind)
						ind.addObjectToBothSidesOfRelationshipWithKey(
								match, SyncIndex.INDEX_MATCHES_KEY);
					match.takeValueForKey(local, "localValue");
					match.takeValueForKey(dict.valueForKey(local), "extValue");
				} // index matches creation
			} // indexes creation
		} // if additional index data
		while (toChangeRows.count() > 0) {
			EOEnterpriseObject match = (EOEnterpriseObject)toChangeRows.removeLastObject();
			ec.deleteObject(match);
		}
		if(toChangeInd != null) {
			while (toChangeInd.count() > 0) {
				SyncIndex ind = (SyncIndex)toChangeInd.removeLastObject();
				ec.deleteObject(ind);
			}
		}
	}
	
	public SyncIndex getIndexNamed(String name, ExtBase base, boolean create) {
		NSArray data = syncIndexes();
		if(data == null || data.count() == 0)
			return null;
		Integer eduYear = MyUtility.eduYear(editingContext());
		Enumeration enu = data.objectEnumerator();
		while (enu.hasMoreElements()) {
			SyncIndex ind = (SyncIndex) enu.nextElement();
			if(!name.equals(ind.indexName()))
				continue;
			if(ind.extBase() != null && ind.extBase() != base)
				continue;
			if(ind.eduYear() == null || ind.eduYear().equals(eduYear))
				return ind;
		}
		if(!create)
			return null;
		SyncIndex ind = (SyncIndex)EOUtilities.createAndInsertInstance(editingContext(), 
				SyncIndex.ENTITY_NAME);
		ind.setIndexName(name);
		addObjectToBothSidesOfRelationshipWithKey(ind, SYNC_INDEXES_KEY);
		//setBase
		return ind;
	}
	
	
	
	public EOEnterpriseObject getDataRow(String key, ExtBase base, boolean create) {
		NSArray data = extData();
		if(data != null && data.count() > 0) {
			for (int i = 0; i < data.count(); i++) {
				EOEnterpriseObject row = (EOEnterpriseObject)data.objectAtIndex(i);
				if(!row.valueForKey("key").equals(key))
					continue;
				if(row.valueForKey("extBase") == null || base == row.valueForKey("extBase"))
					return row;
			}
		}
		if(!create)
			return null;
		EOEnterpriseObject row = EOUtilities.createAndInsertInstance(editingContext(), "ExtData");
		row.takeValueForKey(key, "key");
		row.takeValueForKey(base, "extBase");
		addObjectToBothSidesOfRelationshipWithKey(row, EXT_DATA_KEY);
		return row;
	}
	
	public String extDataForKey(String key, ExtBase base) {
		EOEnterpriseObject row = getDataRow(key, base, false);
		if(row == null)
			return null;
		return (String)row.valueForKey("value");
	}
	
	public void setDataForKey(String value, String key, ExtBase base) {
		EOEnterpriseObject row = getDataRow(key, base, value != null);
		if(value == null) {
			if(row != null) {
				removeObjectFromBothSidesOfRelationshipWithKey(row, EXT_DATA_KEY);
				editingContext().deleteObject(row);
			}
		} else if(!value.equals(row.valueForKey("value"))) {
			row.takeValueForKey(value, "value");
		}
	}
	
	public void setDataDict(NSDictionary dict, ExtBase base) {
		NSMutableDictionary source = dict.mutableClone();
		NSMutableArray toChange = new NSMutableArray();
		NSArray data = extData();
		if(data != null && data.count() > 0) {
			for (int i = 0; i < data.count(); i++) {
				EOEnterpriseObject row = (EOEnterpriseObject)data.objectAtIndex(i);
				if(row.valueForKey("extBase") != null && 
						(base == null || base != row.valueForKey("extBase")))
					continue;
				String key = (String)row.valueForKey("key");
				String value = (String)source.removeObjectForKey(key);
				if(value == null) {
					toChange.addObject(row);
				} else if(!value.equals(row.valueForKey("value"))) {
					row.takeValueForKey(value, "value");
				}
			}
		}
		if(source.count() > 0) {
			Enumeration enu = source.keyEnumerator();
			while (enu.hasMoreElements()) {
				String key = (String) enu.nextElement();
				EOEnterpriseObject row = (EOEnterpriseObject)toChange.removeLastObject();
				if(row == null) {
					row = EOUtilities.createAndInsertInstance(editingContext(), "ExtData");
					addObjectToBothSidesOfRelationshipWithKey(row, EXT_DATA_KEY);
				}
				row.takeValueForKey(base, "extBase");
				row.takeValueForKey(key, "key");
				row.takeValueForKey(source.valueForKey(key), "value");
			}
		}
		while (toChange.count() > 0) {
			EOEnterpriseObject row = (EOEnterpriseObject)toChange.removeLastObject();
			removeObjectFromBothSidesOfRelationshipWithKey(row, EXT_DATA_KEY);
			editingContext().deleteObject(row);
		}
	}
}
