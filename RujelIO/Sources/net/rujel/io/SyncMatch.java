//  SyncMatch.java

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

import net.rujel.base.EntityIndex;
import net.rujel.base.MyUtility;

import com.webobjects.eocontrol.*;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSMutableArray;
import com.webobjects.foundation.NSMutableDictionary;
import com.webobjects.foundation.NSMutableSet;

public class SyncMatch extends _SyncMatch {

	public static void init() {
	}

	public void awakeFromInsertion(EOEditingContext ec) {
		super.awakeFromInsertion(ec);
	}

	public void turnIntoFault(EOFaultHandler handler) {
		super.turnIntoFault(handler);
	}
	
	public String entity() {
		return entityIndex().entName();
	}
	
	public void setEntity(String entity) {
		EntityIndex ei = null;
		ExtSystem sys = extSystem();
		if(sys.entIdxes != null) {
			ei = (EntityIndex)sys.entIdxes.valueForKey(entity);
			if(ei != null && ei.editingContext() != editingContext())
				ei = null;
		}
		if(ei == null) {
			ei = EntityIndex.indexForEntityName(editingContext(),entity, false);
			if(sys.entIdxes != null)
				sys.entIdxes.takeValueForKey(ei, entity);
		}
		setEntityIndex(ei);
		if(ei.isYearly())
			setEduYear(MyUtility.eduYear(editingContext()));
	}
	
	public static EOQualifier matchQualifier(ExtSystem sys, ExtBase base, 
			EntityIndex ei, Integer objectID, Integer eduYear) {
		NSMutableArray quals = new NSMutableArray();
		EOEditingContext ec = null;
		quals.addObject(new EOKeyValueQualifier(EXT_BASE_KEY, 
				EOQualifier.QualifierOperatorEqual, base));
		if(base != null) {
			quals.addObject(new EOKeyValueQualifier(EXT_BASE_KEY, 
				EOQualifier.QualifierOperatorEqual, NullValue));
			EOQualifier qual = new EOOrQualifier(quals);
			quals.removeAllObjects();
			quals.addObject(qual);
			quals.addObject(new EOKeyValueQualifier(EXT_SYSTEM_KEY, 
					EOQualifier.QualifierOperatorEqual, base.extSystem()));
			ec = base.editingContext();
		} else if(sys != null) {
			quals.addObject(new EOKeyValueQualifier(EXT_SYSTEM_KEY, 
				EOQualifier.QualifierOperatorEqual, sys));
			ec = sys.editingContext();
		}
		if(ei != null) {
			quals.addObject(new EOKeyValueQualifier(ENTITY_INDEX_KEY, 
					EOQualifier.QualifierOperatorEqual, ei));
			if(ec == null)
				ec = ei.editingContext();
		}
		if(objectID != null) {
			quals.addObject(new EOKeyValueQualifier(OBJ_ID_KEY, 
					EOQualifier.QualifierOperatorEqual, objectID));
		}
		if(eduYear == null)
			eduYear = MyUtility.eduYear(ec);
		{
			EOQualifier[] qual = new EOQualifier[2];
			qual[0] = new EOKeyValueQualifier(EDU_YEAR_KEY, 
					EOQualifier.QualifierOperatorEqual, eduYear);
			qual[1] = new EOKeyValueQualifier(EDU_YEAR_KEY, 
					EOQualifier.QualifierOperatorEqual, NullValue);
			quals.addObject(new EOOrQualifier(new NSArray(qual)));
		}
		return new EOAndQualifier(quals);
	}
	
	public static SyncMatch matchForSystemAndObject(ExtSystem sys, ExtBase base, 
			EOKeyGlobalID gid) {
		return getMatch(sys, base, gid.entityName(), (Integer)gid.keyValues()[0]);
	}
	
	public static SyncMatch getMatch(ExtSystem sys, ExtBase base, String entity, Integer objID) {
		if(sys == null)
			sys = base.extSystem();
		EOEditingContext ec = sys.editingContext();
		EntityIndex ei = null;
		if(sys.entIdxes != null) {
			ei = (EntityIndex)sys.entIdxes.valueForKey(entity);
			if(ei != null && ei.editingContext() != ec)
				ei = null;
		}
		if(ei == null) {
			ei = EntityIndex.indexForEntityName(ec, entity, false);
			if(sys.entIdxes != null)
				sys.entIdxes.takeValueForKey(ei, entity);
		}
		return getMatch(sys, base, ei, objID);
	}
	
	public static SyncMatch getMatch(ExtSystem sys, ExtBase base, EntityIndex ei, Integer objID) {
		EOQualifier qual = matchQualifier(sys, base, ei, objID, null);
		EOFetchSpecification fs = new EOFetchSpecification(ENTITY_NAME,qual,null);
		NSArray found = sys.editingContext().objectsWithFetchSpecification(fs);
		if(found == null || found.count() == 0)
			return null;
		if(found.count() > 1) {
			for (int i = 0; i < found.count(); i++) {
				SyncMatch m = (SyncMatch)found.objectAtIndex(i);
				if(m.eduYear() != null)
					return m;
			}
		}
		return (SyncMatch)found.objectAtIndex(0);
	}
	
	public static NSMutableDictionary dictForEntity(String entityName, ExtSystem sys, ExtBase base) {
		EOEditingContext ec = sys.editingContext();
		EntityIndex ei = null;
		if(sys.entIdxes != null) {
			ei = (EntityIndex)sys.entIdxes.valueForKey(entityName);
			if(ei != null && ei.editingContext() != ec)
				ei = null;
		}
		if(ei == null) {
			ei = EntityIndex.indexForEntityName(ec, entityName, false);
			if(sys.entIdxes != null)
				sys.entIdxes.takeValueForKey(ei, entityName);
		}
		EOQualifier qual = matchQualifier(sys, base, ei, null, null);
		EOFetchSpecification fs = new EOFetchSpecification(ENTITY_NAME,qual,null);
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

}
