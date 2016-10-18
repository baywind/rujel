//  SubjectGroup.java

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

package net.rujel.eduplan;

import com.webobjects.eocontrol.*;
import com.webobjects.foundation.NSArray;

public class SubjectGroup extends _SubjectGroup {

	public static void init() {
	}
	
	protected NSArray<SubjectGroup> _path;
	protected String _padding;
		
	public void _updatePath() {
		SubjectGroup parent = parent();
		if(parent == null) {
			_path = new NSArray(this);
			_padding = "";
		} else {
			_path = parent.path().arrayByAddingObject(this);
			_padding = parent.padding() + "-&nbsp;";
		}
		NSArray children = children();
		if(children != null && children.count() > 0)
			children.valueForKey("updatePath");
	}
	
	public NSArray<SubjectGroup> path() {
		if(_path == null)
			_updatePath();
		return _path;
	}

	public int depth() {
		return path().count() -1;
	}
	
	public String padding() {
		if(_padding == null)
			_updatePath();
		return _padding;
	}

	public void awakeFromInsertion(EOEditingContext ec) {
		super.awakeFromInsertion(ec);
	}

	public void turnIntoFault(EOFaultHandler handler) {
		_padding = null;
		_path = null;
		super.turnIntoFault(handler);
	}
}
