// EntityIndex.java

/*
 * Copyright (c) 2008, Gennady & Michael Kushnir
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 * 
 * 	•	Redistributions of source code must retain the above copyright notice, this
 * 		list of conditions and the following disclaimer.
 * 	•	Redistributions in binary form must reproduce the above copyright notice,
 * 		this list of conditions and the following disclaimer in the documentation
 * 		and/or other materials provided with the distribution.
 * 	•	Neither the name of the RUJEL nor the names of its contributors may be used
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

package net.rujel.base;

import java.util.logging.Logger;

import com.webobjects.foundation.*;
import com.webobjects.eoaccess.EOEntity;
import com.webobjects.eoaccess.EOModelGroup;
import com.webobjects.eoaccess.EOUtilities;
import com.webobjects.eocontrol.*;

import net.rujel.reusables.*;

public class EntityIndex extends _EntityIndex {
	public static EntityIndex indexForEntityName(EOEditingContext ec, String entName) {
		return indexForEntityName(ec, entName,true);
	}
	public static EntityIndex indexForEntityName(EOEditingContext ec, String entName, boolean create) {
		NSArray found = EOUtilities.objectsMatchingKeyAndValue(ec, "EntityIndex", "entName", entName);
		EntityIndex result = null;
		if(found == null || found.count() == 0) {
			result = (EntityIndex)EOUtilities.createAndInsertInstance(ec, "EntityIndex");
			result.setEntName(entName);
			EOEntity entity = EOModelGroup.defaultGroup().entityNamed(entName);
			String path = entity.model().pathURL().getPath();
			result.setSqlTable(path + '.' + entity.externalName());
			Logger.getLogger("rujel.base").log(WOLogLevel.COREDATA_EDITING,"Autocreating EnityIndex for entity " + entName,result);
		} else {
			result = (EntityIndex)found.objectAtIndex(0);
		}
		return result;
	}
	public static EntityIndex indexForObject(EOEnterpriseObject eo) {
		return indexForObject(eo, true); 
	}
	
	public static EntityIndex indexForObject(EOEnterpriseObject eo, boolean create) {
		EOEditingContext ec = eo.editingContext();
		String entName = eo.entityName();
		return indexForEntityName(ec, entName);
	}
}
