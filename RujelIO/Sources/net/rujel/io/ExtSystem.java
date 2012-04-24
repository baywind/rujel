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
import net.rujel.reusables.Various;

import com.webobjects.eoaccess.EOUtilities;
import com.webobjects.eocontrol.*;
import com.webobjects.foundation.NSArray;
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
	
	public static ExtSystem localSystem(EOEditingContext ec) {
		NSArray rjls = EOUtilities.objectsMatchingKeyAndValue(ec, ENTITY_NAME, 
				PRODUCT_NAME_KEY, "Rujel");
		if(rjls == null || rjls.count() == 0) {
			ExtSystem result = (ExtSystem)EOUtilities.createAndInsertInstance(ec, ENTITY_NAME);
			result.setProductName("Rujel");
			return result;
		}
		return (ExtSystem)rjls.objectAtIndex(0);
	}
}
