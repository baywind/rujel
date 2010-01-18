//  WorkType.java

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

package net.rujel.criterial;

import java.math.BigDecimal;
import java.util.logging.Logger;

import net.rujel.reusables.ModulesInitialiser;
import net.rujel.reusables.NamedFlags;
import net.rujel.reusables.WOLogLevel;

import com.webobjects.foundation.*;
import com.webobjects.eocontrol.*;

public class WorkType extends _WorkType {

	public static final NSArray flagNames = new NSArray (new String[] {
			"fixWeight","fixCompulsory","fixHometask","compulsory","hometask","-32-","unused"});

	public static EOQualifier activeQualifier = new EOKeyValueQualifier(DFLT_FLAGS_KEY,
			EOQualifier.QualifierOperatorLessThan,new Integer(64));
	
	protected static int maxNum = 0;
	
	public static Integer nextSortNum(EOEditingContext ec) {
		NSArray sorter = new NSArray(new EOSortOrdering(
				SORT_KEY,EOSortOrdering.CompareDescending));
		EOFetchSpecification fs = new EOFetchSpecification(ENTITY_NAME,activeQualifier,sorter);
		fs.setFetchLimit(1);
		NSArray list = ec.objectsWithFetchSpecification(fs);
		if(list != null && list.count() > 0) {
			WorkType wt = (WorkType)list.objectAtIndex(0);
			maxNum = wt.sort().intValue();
		}
		return new Integer(maxNum +1);
	}
	
	protected static EOGlobalID defaultType;
	public static WorkType defaultType(EOEditingContext ctx) {
		if(defaultType != null)
			return (WorkType)ctx.faultForGlobalID(defaultType, ctx);
		EOQualifier qual = new EOKeyValueQualifier("dfltFlags",
				EOQualifier.QualifierOperatorLessThan, new Integer(16));
		EOFetchSpecification fs = new EOFetchSpecification(ENTITY_NAME,qual,
				ModulesInitialiser.sorter);
		fs.setFetchLimit(1);
		NSArray found = ctx.objectsWithFetchSpecification(fs);
		if(found != null && found.count() > 0) {
			WorkType type = (WorkType)found.objectAtIndex(0);
			defaultType = ctx.globalIDForObject(type);
			return type;
		}
		return null;
	}

	private NamedFlags _flags;
    public NamedFlags namedFlags() {
    	if(_flags==null) {
    		_flags = new NamedFlags(dfltFlags().intValue(),WorkType.flagNames);
    		try{
    			_flags.setSyncParams(this, getClass().getMethod("setNamedFlags",
    					NamedFlags.class));
    		} catch (Exception e) {
    			Logger.getLogger("rujel.criterial").log(WOLogLevel.WARNING,
						"Could not get syncMethod for Work flags",e);
			}
    	}
    	return _flags;
    }
    
    public void setNamedFlags(NamedFlags flags) {
    	if(flags != null)
    		super.setDfltFlags(flags.toInteger());
    	_flags = flags;
    }
    
    public void setDfltFlags(Integer value) {
    	_flags = null;
    	super.setDfltFlags(value);
    }

	
	public void awakeFromInsertion(EOEditingContext ec) {
		super.awakeFromInsertion(ec);
		super.setDfltFlags(new Integer(0));
		setDfltWeight(BigDecimal.ZERO);
		setUseCount(new Integer(0));
		if(maxNum <= 0)
			setSort(nextSortNum(ec));
	}
	
	public void awakeFromFetch(EOEditingContext ec) {
		super.awakeFromFetch(ec);
		if(sort().intValue() > maxNum)
			maxNum = sort().intValue();
		NSDictionary snapshot = ec.committedSnapshotForObject(this);
		if(snapshot == null || snapshot.count() == 0) {
			Logger.getLogger("rujel.criterial").log(WOLogLevel.WARNING,
					"Empty snapshot for fetched object",
					new Object[] {this, new IllegalStateException()});
		}
	}
	
	public void setSort(Integer sort) {
		super.setSort(sort);
		if(sort.intValue() > maxNum)
			maxNum = sort.intValue();
	}
	
	public void turnIntoFault(EOFaultHandler handler) {
		super.turnIntoFault(handler);
		_flags = null;
	}
}
