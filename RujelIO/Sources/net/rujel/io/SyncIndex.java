//  SyncIndex.java

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

import com.webobjects.eocontrol.*;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSMutableDictionary;

public class SyncIndex extends _SyncIndex {

	public static void init() {
	}

	public void awakeFromInsertion(EOEditingContext ec) {
		super.awakeFromInsertion(ec);
	}

	public void turnIntoFault(EOFaultHandler handler) {
		super.turnIntoFault(handler);
	}
	
	public NSMutableDictionary getDict() {
		NSArray matches = indexMatches();
		if(matches == null || matches.count() == 0)
			return null;
		NSMutableDictionary dict = new NSMutableDictionary();
		Enumeration enu = matches.objectEnumerator();
		while (enu.hasMoreElements()) {
			EOEnterpriseObject match = (EOEnterpriseObject) enu.nextElement();
			String local = (String)match.valueForKey("localValue");
			String ext = (String)match.valueForKey("extValue");
			dict.takeValueForKey(ext, local);
		}
		return dict;
	}
	
	public String extForLocal(String local) {
		NSArray matches = indexMatches();
		if(matches == null || matches.count() == 0)
			return null;
		Enumeration enu = matches.objectEnumerator();
		while (enu.hasMoreElements()) {
			EOEnterpriseObject match = (EOEnterpriseObject) enu.nextElement();
			if(local.equals(match.valueForKey("localValue")))
				return (String)match.valueForKey("extValue");
		}
		return null;
	}
}
