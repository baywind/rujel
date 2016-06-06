//  SchoolSection.java

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

import java.util.Enumeration;
import java.util.logging.Logger;

import com.webobjects.appserver.WOSession;
import com.webobjects.eoaccess.EOUtilities;
import com.webobjects.eocontrol.*;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSMutableArray;
import com.webobjects.foundation.NSMutableDictionary;

import net.rujel.auth.UserPresentation;
import net.rujel.reusables.ModulesInitialiser;
import net.rujel.reusables.NamedFlags;
import net.rujel.reusables.SettingsReader;
import net.rujel.reusables.WOLogLevel;

public class SchoolSection extends _SchoolSection {

	public static void init() {
	}

	public static NSArray flagNames = new NSArray(new String[] {
			"-1-","-2-","-4-","-8-","-16-","disabled"});

	private NamedFlags _flags;
    public NamedFlags namedFlags() {
    	if(_flags==null) {
    		_flags = new NamedFlags(flags().intValue(),flagNames);
    		try{
    			_flags.setSyncParams(this, getClass().getMethod("setNamedFlags",
    					NamedFlags.class));
    		} catch (Exception e) {
    			Logger.getLogger("rujel.curriculum").log(WOLogLevel.WARNING,
						"Could not get syncMethod for SchoolSection flags",e);
			}
    	}
    	return _flags;
    }
    
    public void setNamedFlags(NamedFlags flags) {
    	if(flags != null)
    		setFlags(flags.toInteger());
    	_flags = flags;
    }
    
    public void setFlags(Integer value) {
    	_flags = null;
    	super.setFlags(value);
    }
    
    public void awakeFromInsertion(EOEditingContext ec) {
		super.awakeFromInsertion(ec);
		setFlags(new Integer(0));
		setMinGrade(SettingsReader.intForKeyPath("edu.minGrade", 1));
		setMaxGrade(SettingsReader.intForKeyPath("edu.maxGrade", 11));
	}

	public void turnIntoFault(EOFaultHandler handler) {
		super.turnIntoFault(handler);
		_flags = null;
	}

	public Integer sectionID() {
		EOGlobalID gid = editingContext().globalIDForObject(this);
		if(gid.isTemporary())
			return null;
		return (Integer) ((EOKeyGlobalID)gid).keyValues()[0];
	}
	
	public static NSArray listSections(EOEditingContext ec, boolean includeHidden) {
		EOQualifier qual = (includeHidden)?null: new EOKeyValueQualifier(FLAGS_KEY,
				EOQualifier.QualifierOperatorLessThan, new Integer(32));
		EOFetchSpecification fs = new EOFetchSpecification(
				ENTITY_NAME,qual,ModulesInitialiser.sorter);
		NSArray list = ec.objectsWithFetchSpecification(fs);
		if(includeHidden && list == null || list.count() == 0) {
	    	Indexer sIndex = Indexer.getIndexer(ec,"eduSections",(String)null, false);
    		NSArray rows = (sIndex == null)?null:sIndex.indexRows();
	    	if(rows == null || rows.count() == 0) {
	    		SchoolSection section = (SchoolSection)EOUtilities.createAndInsertInstance(
	    				ec, ENTITY_NAME);
	    		section.takeStoredValueForKey(new Integer(0), "sID");
	    		section.setSort(0);
	    		section.setName(" ");
				list = new NSArray(section);
	    	} else {
	    		Enumeration enu = rows.objectEnumerator();
	    		list = new NSMutableArray();
	    		while (enu.hasMoreElements()) {
					IndexRow row = (IndexRow) enu.nextElement();
		    		SchoolSection section = (SchoolSection)EOUtilities.createAndInsertInstance(
		    				ec, ENTITY_NAME);
		    		section.takeStoredValueForKey(row.idx(), "sID");
		    		section.setSort(row.idx());
		    		section.setName(row.value());
		    		list.add(section);
				}
	    	}
			ec.saveChanges();
		}
		return list;
	}
	
	public static NSMutableDictionary forSession(WOSession ses) {
		NSMutableDictionary dict = new NSMutableDictionary();
		NSArray list = listSections(ses.defaultEditingContext(), false);
		dict.takeValueForKey(list, "list");
		if(list.count() == 1) {
			dict.takeValueForKey(Boolean.FALSE, "hasSections");
			dict.takeValueForKey(list.objectAtIndex(0), "defaultSection");
//			dict.takeValueForKey(list.objectAtIndex(0), "currSection");
			return dict;
		} else {
			dict.takeValueForKey(Boolean.TRUE, "hasSections");
			UserPresentation user = (UserPresentation)ses.valueForKey("user");
			Enumeration enu = list.objectEnumerator();
			try {
				NSMutableArray filtered = new NSMutableArray(list.count());
				NSMutableArray others = new NSMutableArray(list.count());
				while (enu.hasMoreElements()) {
					SchoolSection sect = (SchoolSection) enu.nextElement();
					if(user.isInGroup(null, sect.sectionID()))
						filtered.addObject(sect);
					else
						others.addObject(sect);
				}
				dict.takeValueForKey(filtered.immutableClone(), "available");
				filtered.addObjectsFromArray(others);
				dict.takeValueForKey(filtered.immutableClone(), "list");
				dict.takeValueForKey(others.immutableClone(), "others");
				dict.takeValueForKey(filtered.objectAtIndex(0), "defaultSection");
			} catch (Exception e) {
				e.printStackTrace(); //TODO
				//failed to filter sections
			}
		}
		return dict;
	}

	public static SchoolSection stateSection(WOSession session, EOEditingContext ec) {
		SchoolSection section = (SchoolSection)session.valueForKeyPath("state.section");
		if(section == null || ec == null)
			return section;
		return (SchoolSection)EOUtilities.localInstanceOfObject(ec, section);
	}

}
