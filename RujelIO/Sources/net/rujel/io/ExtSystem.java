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
import net.rujel.reusables.SettingsReader;
import net.rujel.reusables.Various;

import com.webobjects.appserver.WOApplication;
import com.webobjects.eoaccess.EOUtilities;
import com.webobjects.eocontrol.*;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSMutableArray;
import com.webobjects.foundation.NSMutableDictionary;
import com.webobjects.foundation.NSMutableSet;

public class ExtSystem extends _ExtSystem {
	
	protected static String localBase;
	
	public static String localBaseID() {
		return localBase;
	}
	
	public boolean isLocalBase() {
		return baseID().equals(localBase);
	}

	public void awakeFromInsertion(EOEditingContext ec) {
		super.awakeFromInsertion(ec);
	}

	public void turnIntoFault(EOFaultHandler handler) {
		super.turnIntoFault(handler);
	}
	
	public String extidForObject(EOEnterpriseObject eo) {
		if(eo == null)
			return null;
		EOKeyGlobalID gid = (EOKeyGlobalID)eo.editingContext().globalIDForObject(eo);
		SyncMatch match = SyncMatch.matchForSystemAndObject(this, gid);
		if(match == null) {
			if(isLocalBase()) {
				match = addMatch(gid);
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
	
	public SyncMatch addMatch(EOKeyGlobalID gid) {
		SyncMatch match = (SyncMatch)EOUtilities.createAndInsertInstance(editingContext(),
				SyncMatch.ENTITY_NAME);
		match.setExtSystem(this);
		match.setObjID((Integer)gid.keyValues()[0]);
		return match;
	}
	
	public NSMutableDictionary dictForEntity(String entityName) {
		EOEditingContext ec = editingContext();
		EntityIndex ei = EntityIndex.indexForEntityName(ec, entityName, false);
		Integer eduYear = MyUtility.eduYear(ec);
		EOQualifier[] qual = new EOQualifier[3];
		qual[0] = new EOKeyValueQualifier(SyncMatch.EDU_YEAR_KEY, 
				EOQualifier.QualifierOperatorEqual, eduYear);
		qual[1] = new EOKeyValueQualifier(SyncMatch.EDU_YEAR_KEY, 
				EOQualifier.QualifierOperatorEqual, NullValue);
		qual[2] = new EOOrQualifier(new NSArray(qual));
		
		qual[0] = new EOKeyValueQualifier(SyncMatch.EXT_SYSTEM_KEY, 
				EOQualifier.QualifierOperatorEqual, this);
		qual[1] = new EOKeyValueQualifier(SyncMatch.ENTITY_INDEX_KEY, 
				EOQualifier.QualifierOperatorEqual, ei);
		qual[2] = new EOAndQualifier(new NSArray(qual));
		EOFetchSpecification fs = new EOFetchSpecification(ENTITY_NAME,qual[4],null);
		NSArray found = ec.objectsWithFetchSpecification(fs);
		if(found == null || found.count() == 0)
			return null;
		Enumeration enu = found.objectEnumerator();
		NSMutableDictionary dict = new NSMutableDictionary(found.count());
		NSMutableSet used = new NSMutableSet();
		while (enu.hasMoreElements()) {
			SyncMatch match = (SyncMatch) enu.nextElement();
			Integer id = match.objID();
			if(used.containsObject(id))
				continue;
			String extID = match.extID();
			if(extID == null) extID = "";
			dict.setObjectForKey(extID, id);
			if(match.eduYear() != null)
				used.addObject(id);
		}
		return dict;
	}
	
	public NSMutableDictionary dictForObjects(NSArray list) {
		if(list == null || list.count() == 0)
			return null;
		EOEditingContext ec = editingContext();
		NSMutableDictionary dict = new NSMutableDictionary();
		String entityName = null;
		Integer eduYear = MyUtility.eduYear(ec);
		EOQualifier[] qual = new EOQualifier[4];
		qual[0] = new EOKeyValueQualifier(SyncMatch.EDU_YEAR_KEY, 
				EOQualifier.QualifierOperatorEqual, eduYear);
		qual[1] = new EOKeyValueQualifier(SyncMatch.EDU_YEAR_KEY, 
				EOQualifier.QualifierOperatorEqual, NullValue);
		qual[2] = new EOOrQualifier(new NSArray(qual));
		
		qual[0] = new EOKeyValueQualifier(SyncMatch.EXT_SYSTEM_KEY, 
				EOQualifier.QualifierOperatorEqual, this);
		NSMutableArray ids = new NSMutableArray(list.count());
		Enumeration enu = list.objectEnumerator();
		int last = 0;
		while (enu.hasMoreElements()) {
			EOEnterpriseObject obj = (EOEnterpriseObject) enu.nextElement();
			if(entityName == null) {
				entityName = obj.entityName();
				EntityIndex ei = EntityIndex.indexForEntityName(ec, entityName, false);
				qual[1] = new EOKeyValueQualifier(SyncMatch.ENTITY_INDEX_KEY, 
						EOQualifier.QualifierOperatorEqual, ei);
			} else if(!entityName.equals(obj.entityName())) {
				matchDict(qual, ids, dict, list, last);
				EntityIndex ei = EntityIndex.indexForEntityName(ec, entityName, false);
				qual[1] = new EOKeyValueQualifier(SyncMatch.ENTITY_INDEX_KEY, 
						EOQualifier.QualifierOperatorEqual, ei);
				last += ids.count();
			}
			EOKeyGlobalID gid = (EOKeyGlobalID)obj.editingContext().globalIDForObject(obj);
			ids.addObject(gid.keyValues()[0]);
		}
		matchDict(qual, ids, dict, list, last);
		if(ec.hasChanges())
			ec.saveChanges();
		return dict;
	}
	
	private void matchDict(EOQualifier[] qual, NSMutableArray ids, NSMutableDictionary dict,
			NSArray list, int last) {
		qual[3] = Various.getEOInQualifier(SyncMatch.OBJ_ID_KEY, ids);
		qual[3] = new EOAndQualifier(new NSArray(qual));
		EOFetchSpecification fs = new EOFetchSpecification(ENTITY_NAME,qual[3],null);
		NSArray found = editingContext().objectsWithFetchSpecification(fs);
		SyncMatch[] ml = null;
		if(isLocalBase())
			ml = new SyncMatch[ids.count()];
		if(found == null || found.count() == 0)
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
			EntityIndex ei = null;
			Integer eduYear = null;
			for (int i = 0; i < ml.length; i++) {
				if(ml[i] != null) {
					ei = ml[i].entityIndex();
					eduYear = ml[i].eduYear();
					continue;
				}
				EOEnterpriseObject eo = (EOEnterpriseObject)list.objectAtIndex(last + i);
				if(ei == null) {
					ei = EntityIndex.indexForEntityName(editingContext(), eo.entityName(), true);
					if(ei.isYearly())
						eduYear = MyUtility.eduYear(eo.editingContext());
				}
				ml[i] = (SyncMatch)EOUtilities.createAndInsertInstance(editingContext(),
						SyncMatch.ENTITY_NAME);
				ml[i].setExtSystem(this);
				ml[i].setObjID((Integer)ids.objectAtIndex(i));
				ml[i].setEntityIndex(ei);
				ml[i].setEduYear(eduYear);
				String guid = UUID.randomUUID().toString();
				ml[i].setExtID(guid);
				dict.setObjectForKey(guid, eo);
			}
		}
	}
	
	public static ExtSystem localSystem(EOEditingContext ec) {
		NSArray rjls = EOUtilities.objectsMatchingKeyAndValue(ec, ENTITY_NAME, 
				PRODUCT_NAME_KEY, "Rujel");
		if(rjls == null || rjls.count() == 0) {
			ExtSystem result = (ExtSystem)EOUtilities.createAndInsertInstance(ec, ENTITY_NAME);
			result.setProductName("Rujel");
			localBase = SettingsReader.stringForKeyPath("baseName", null);
			if(localBase == null) {
				WOApplication app = WOApplication.application();
				localBase = app.name() + '.' + app.host();
			}
			result.setBaseID(localBase);
			ec.saveChanges();
			return result;
		}
		ExtSystem result = (ExtSystem)rjls.objectAtIndex(0);
		if(rjls.count() == 1)
			return result;
		EOKeyGlobalID gid = (EOKeyGlobalID)ec.globalIDForObject(result);
		int min = (Integer)gid.keyValues()[0];
		for (int i = 1; i < rjls.count(); i++) {
			ExtSystem es = (ExtSystem) rjls.objectAtIndex(i);
			gid = (EOKeyGlobalID)ec.globalIDForObject(es);
			if(min > ((Integer)gid.keyValues()[0])) {
				result = es;
				min = (Integer)gid.keyValues()[0];
			}
		}
		if(localBase == null)
			localBase = result.baseID();
		return result;
	}
}
