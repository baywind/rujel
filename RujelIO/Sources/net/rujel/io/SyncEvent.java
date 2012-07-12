//  SyncEvent.java

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

import com.webobjects.eocontrol.*;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSTimestamp;

public class SyncEvent extends _SyncEvent {
	
	public static NSArray sorter = new NSArray(
			new EOSortOrdering(EXEC_TIME_KEY,EOSortOrdering.CompareDescending));

	public static void init() {
	}

	public void awakeFromInsertion(EOEditingContext ec) {
		super.awakeFromInsertion(ec);
		setExecTime(new NSTimestamp());
		setResult(new Integer(0));
	}

	public void turnIntoFault(EOFaultHandler handler) {
		super.turnIntoFault(handler);
	}
	
	public static NSArray eventsForSystem(ExtSystem sys, ExtBase base, int limit, String syncEnt) {
		EOEditingContext ec = sys.editingContext();
		EOQualifier qual = new EOKeyValueQualifier(EXT_SYSTEM_KEY, 
				EOQualifier.QualifierOperatorEqual, sys);
		if(syncEnt != null) {
			NSArray quals = new NSArray(new Object[] {
					qual, new EOKeyValueQualifier(SYNC_ENTITY_KEY, 
							EOQualifier.QualifierOperatorEqual,syncEnt)
			});
			qual = new EOAndQualifier(quals);
		}
//			SyncMatch.matchQualifier(sys, base, null, null, null);
		EOFetchSpecification fs = new EOFetchSpecification(ENTITY_NAME,qual,sorter);
		if(limit > 0)
			fs.setFetchLimit(limit);
		return ec.objectsWithFetchSpecification(fs);
	}
	
	public static SyncEvent lastEventForSystem(ExtSystem sys, ExtBase base) {
		NSArray found = eventsForSystem(sys, base, 1, null);
		if(found == null || found.count() == 0)
			return null;
		return (SyncEvent)found.objectAtIndex(0);
	}
}
