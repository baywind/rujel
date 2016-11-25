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

import com.webobjects.eoaccess.EOUtilities;
import com.webobjects.eocontrol.*;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSMutableArray;

import net.rujel.reusables.ModulesInitialiser;

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
			_padding = parent.padding() + "- ";
		}
		NSArray children = children();
		if(children != null && children.count() > 0)
			children.valueForKey("updatePath");
	}
	
	public NSArray listWithChildren() {
		NSMutableArray result = new NSMutableArray();
		addToList(result);
		return result;
	}
	
	public void addToList(NSMutableArray list) {
		list.addObject(this);
		NSArray<SubjectGroup> children = children();
		if(children == null || children.count() == 0)
			return;
		if(children.count() > 1) {
			children = EOSortOrdering.sortedArrayUsingKeyOrderArray(children, 
					ModulesInitialiser.sorter);
		}
		for (int i = 0; i < children.count(); i++) {
			SubjectGroup c = children.objectAtIndex(i);
			c.addToList(list);
		}
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
	
	public String paddedName() {
		return padding() + name();
	}

	public void awakeFromInsertion(EOEditingContext ec) {
		super.awakeFromInsertion(ec);
	}

	public void turnIntoFault(EOFaultHandler handler) {
		_padding = null;
		_path = null;
		super.turnIntoFault(handler);
	}

	public void addToSortedList(NSMutableArray<SubjectGroup> list) {
		int level = 0;
		int maxLevel = path().count();
		int sort = path().objectAtIndex(0).sort().intValue();
		for (int i = 0; i < list.count(); i++) {
			NSArray<SubjectGroup> itemPath = list.objectAtIndex(i).path();
			if(itemPath == path())
				return;
			int itemSort = (itemPath.count() <= level)? sort+1 :
					itemPath.objectAtIndex(level).sort().intValue();
			if(sort > itemSort)
				continue;
			while (sort == itemSort && level < maxLevel-1) {
				level++;
				sort = path().objectAtIndex(level).sort().intValue();
				itemSort = (itemPath.count() <= level)? sort-1 :
					itemPath.objectAtIndex(level).sort().intValue();
			}
			if(sort < itemSort) {
				while (level < maxLevel) {
					list.insertObjectAtIndex(path().objectAtIndex(level), i);
					i++;
					level++;
				}
				return;
			}
		} // check existing array members
		while (level < maxLevel) {
			list.addObject(path().objectAtIndex(level));
			level++;
		}
	}
	
	public static NSArray listSubjectGroups(EOEditingContext ec) {
		NSArray<SubjectGroup> found = EOUtilities.objectsForEntityNamed(ec, ENTITY_NAME);
		if(found == null || found.count() == 0)
			return null;
		NSMutableArray result = new NSMutableArray(found.count());
		for (int i = 0; i < found.count(); i++) {
			SubjectGroup sg = found.objectAtIndex(i);
			sg.addToSortedList(result);
		}
		return result;
	}
	
}
