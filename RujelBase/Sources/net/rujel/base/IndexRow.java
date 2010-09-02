//  IndexRow.java

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

package net.rujel.base;

import com.webobjects.eoaccess.EOUtilities;
import com.webobjects.eocontrol.EOEnterpriseObject;
import com.webobjects.eocontrol.EOFaultHandler;

public class IndexRow extends _IndexRow {

	public static void init() {
	}
	private int idx = Integer.MIN_VALUE;
	private String comment;
	private String formatted;

	public void nullify() {
		idx = Integer.MIN_VALUE;
		formatted = null;
		comment = null;
	}
	public void turnIntoFault(EOFaultHandler handler) {
		nullify();
		super.turnIntoFault(handler);
	}
	
	public int num() {
		if(idx == Integer.MIN_VALUE) {
			idx = idx().intValue();
		}
		return idx;
	}
	
	public String comment() {
		if(comment == null)
			comment = (String)valueForKeyPath("commentEO.storedText");
		return comment;
	}
	
	public void setIdx(Integer idx) {
		if (idx() != null && !editingContext().globalIDForObject(this).isTemporary())
			throw new UnsupportedOperationException(
					"Idx is primary key for IndexRow and may not be changed");
		super.setIdx(idx);
	}

	public void setComment(String cmnt) {
		EOEnterpriseObject text = commentEO();
		if(cmnt == null) {
			if(text != null)
				removeObjectFromBothSidesOfRelationshipWithKey(text, COMMENT_EO_KEY);
 		} else {
 			if(text == null) {
 				text = EOUtilities.createAndInsertInstance(editingContext(), "TextStore");
 				text.takeValueForKey(EntityIndex.indexForObject(this), "entityIndex");
 			}
 			text.takeValueForKey(cmnt, "storedText");
		}
		comment = cmnt;
	}
	
	public String formatted() {
		if(formatted == null) {
			String format = (String)valueForKeyPath("indexer.formatString");
			if(format != null)
				formatted = String.format(format, idx(), value(), comment());
			else
				formatted = value();
		}
		return formatted;
	}
}
