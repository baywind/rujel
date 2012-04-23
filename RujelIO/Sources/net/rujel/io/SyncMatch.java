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

import net.rujel.base.EntityIndex;
import net.rujel.base.MyUtility;

import com.webobjects.eocontrol.*;
import com.webobjects.foundation.NSArray;

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
		EntityIndex ei = EntityIndex.indexForEntityName(editingContext(), entity, true);
		setEntityIndex(ei);
		if(ei.isYearly())
			setEduYear(MyUtility.eduYear(editingContext()));
	}
	
	public static SyncMatch matchForSystemAndObject(ExtSystem sys, EOKeyGlobalID gid) {
		EOEditingContext ec = sys.editingContext();
		EntityIndex ei = EntityIndex.indexForEntityName(ec, gid.entityName(), false);
		Integer eduYear = MyUtility.eduYear(ec);
		EOQualifier[] qual = new EOQualifier[4];
		qual[0] = new EOKeyValueQualifier(EDU_YEAR_KEY, 
				EOQualifier.QualifierOperatorEqual, eduYear);
		qual[1] = new EOKeyValueQualifier(EDU_YEAR_KEY, 
				EOQualifier.QualifierOperatorEqual, NullValue);
		qual[3] = new EOOrQualifier(new NSArray(qual));
		
		qual[0] = new EOKeyValueQualifier(EXT_SYSTEM_KEY, 
				EOQualifier.QualifierOperatorEqual, sys);
		qual[1] = new EOKeyValueQualifier(ENTITY_INDEX_KEY, 
				EOQualifier.QualifierOperatorEqual, ei);
		qual[2] = new EOKeyValueQualifier(OBJ_ID_KEY, 
				EOQualifier.QualifierOperatorEqual, gid.keyValues()[0]);
		qual[3] = new EOAndQualifier(new NSArray(qual));
		EOFetchSpecification fs = new EOFetchSpecification(ENTITY_NAME,qual[3],null);
		NSArray found = ec.objectsWithFetchSpecification(fs);
		if(found == null || found.count() == 0)
			return null;
		if(found.count() > 1) {
			for (int i = 0; i < qual.length; i++) {
				SyncMatch m = (SyncMatch)found.objectAtIndex(i);
				if(m.eduYear() != null)
					return m;
			}
		}
		return (SyncMatch)found.objectAtIndex(0);
	}
}
