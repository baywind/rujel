//  ExtBase.java

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

import net.rujel.reusables.SettingsReader;

import com.webobjects.appserver.WOApplication;
import com.webobjects.eoaccess.EOUtilities;
import com.webobjects.eocontrol.*;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSMutableDictionary;

public class ExtBase extends _ExtBase {
	
	protected static EOGlobalID localBaseGID;
	protected static String localBase;

	public static String localBaseID() {
		return localBase;
	}

	public static void init() {
	}

	public void awakeFromInsertion(EOEditingContext ec) {
		super.awakeFromInsertion(ec);
	}

	public void turnIntoFault(EOFaultHandler handler) {
		super.turnIntoFault(handler);
	}
	
	public boolean isLocalBase() {
		if(localBaseGID == null)
			return (this == localBase(editingContext()));
		EOGlobalID gid = editingContext().globalIDForObject(this);
		return gid.equals(localBaseGID);
	}
	
	public static ExtBase localBase(EOEditingContext ec) {
		return localBase(ec,false);
	}
	public static ExtBase localBase(EOEditingContext ec, boolean yearly) {
		if(localBaseGID != null)
			return (ExtBase)ec.faultForGlobalID(localBaseGID, ec);
		ExtSystem localSystem = ExtSystem.extSystemNamed("Rujel", ec, true);
		NSArray rjls = localSystem.extBases();
		if(rjls == null || rjls.count() == 0) {
			localBase = SettingsReader.stringForKeyPath("baseName", null);
			if(localBase == null) {
				WOApplication app = WOApplication.application();
				localBase = app.name() + '.' + app.host();
			}
			ec.saveChanges();
			ExtBase result = (ExtBase)EOUtilities.createAndInsertInstance(ec, ENTITY_NAME);
			result.setBaseID(localBase);
			result.setTitle("local");
			result.addObjectToBothSidesOfRelationshipWithKey(localSystem, EXT_SYSTEM_KEY);
			ec.saveChanges();
			localBaseGID = ec.globalIDForObject(result);
		}
		ExtBase result = (ExtBase)rjls.objectAtIndex(0);
		if(rjls.count() > 1) {
			EOKeyGlobalID gid = (EOKeyGlobalID)ec.globalIDForObject(result);
			int min = (Integer)gid.keyValues()[0];
			for (int i = 1; i < rjls.count(); i++) {
				ExtBase es = (ExtBase) rjls.objectAtIndex(i);
				gid = (EOKeyGlobalID)ec.globalIDForObject(es);
				if(min > ((Integer)gid.keyValues()[0])) {
					result = es;
					min = (Integer)gid.keyValues()[0];
				}
			}
		}
		if(localBaseGID == null) {
			localBaseGID = ec.globalIDForObject(result);
			localBase = result.baseID();
		}
		return result;
	}
	
	public SyncMatch addMatch(EOKeyGlobalID gid) {
		SyncMatch match = (SyncMatch)EOUtilities.createAndInsertInstance(editingContext(),
				SyncMatch.ENTITY_NAME);
		match.setExtSystem(extSystem());
		match.setExtBase(this);
		match.setObjID((Integer)gid.keyValues()[0]);
		return match;
	}

	public String extidForObject(EOEnterpriseObject eo) {
		return extSystem().extidForObject(eo, this);
	}
	
	public NSMutableDictionary dictForObjects(NSArray list) {
		return extSystem().dictForObjects(list, this);
	}
}
