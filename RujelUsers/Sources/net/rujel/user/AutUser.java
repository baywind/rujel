//  AutUser.java

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

package net.rujel.user;

import net.rujel.auth.LoginProcessor;
import net.rujel.base.EntityIndex;
import net.rujel.interfaces.PersonLink;

import com.webobjects.eoaccess.EOUtilities;
import com.webobjects.eocontrol.*;

public class AutUser extends _AutUser {

	protected Object plink;
	public PersonLink personLink() {
		if(plink != null)
			return (plink == NullValue)?null:(PersonLink)plink;
		if(personID() == null || personEntity() == null) {
			plink = NullValue;
			return null;
		}
		try {
			plink = (PersonLink)EOUtilities.objectWithPrimaryKeyValue(
					editingContext(), personEntity().entName(), personID());
		} catch (Exception e) {
			plink = NullValue;
			return null;
		}
		return (PersonLink)plink;
	}
	
	public void setPersonLink(PersonLink pl) {
		if(pl == null) {
			plink = NullValue;
			setPersonEntity(null);
			setPersonID(null);
			return;
		}
		EOEnterpriseObject pln = EOUtilities.localInstanceOfObject(
				editingContext(), (EOEnterpriseObject)pl);
		setPersonEntity(EntityIndex.indexForObject(pln, true));
		EOKeyGlobalID gid = (EOKeyGlobalID)editingContext().globalIDForObject(pln);
		takeValueForKey(gid.keyValues()[0], PERSON_ID_KEY);
		plink = pln;
	}
	
	public String setPassword(String password) {
		String hash = TableLoginHandler.HASH_PREFIX + LoginProcessor.getPasswordDigest(password);
		setCredential(hash);
		return hash;
	}
	
	public boolean hasParent() {
		return !(credential() == null || credential().startsWith(TableLoginHandler.HASH_PREFIX));
	}

	public void turnIntoFault(EOFaultHandler handler) {
		plink = null;
		super.turnIntoFault(handler);
	}
	
	public void _flushPlink() {
		plink = null;
	}
}
