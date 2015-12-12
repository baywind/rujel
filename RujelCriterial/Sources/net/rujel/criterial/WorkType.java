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

import net.rujel.base.Setting;
import net.rujel.base.SettingsBase;
import net.rujel.interfaces.EduCourse;
import net.rujel.reusables.Flags;
import net.rujel.reusables.ModulesInitialiser;
import net.rujel.reusables.NamedFlags;
import net.rujel.reusables.SessionedEditingContext;
import net.rujel.reusables.Various;
import net.rujel.reusables.WOLogLevel;

import com.webobjects.foundation.*;
import com.webobjects.appserver.WOApplication;
import com.webobjects.appserver.WOSession;
import com.webobjects.eoaccess.EOUtilities;
import com.webobjects.eocontrol.*;

public class WorkType extends _WorkType {

	public static final NSArray flagNames = new NSArray (new String[] {
			"fixWeight","fixCompulsory","fixHometask","compulsory","hometask","system",
			"specCriter","-128-","-256-","-512-","-1024-","-2048-","-4096-","unused"});

	public static final EOQualifier activeQualifier = new EOKeyValueQualifier(DFLT_FLAGS_KEY,
			EOQualifier.QualifierOperatorLessThan,Integer.valueOf(8192));
	
	public static final NSDictionary specTypes = new NSDictionary (
			new Integer[] {Integer.valueOf(38)}, new String[] {"onLesson"});

	public static WorkType defaultType(EduCourse course) {
		EOEditingContext ec = course.editingContext();
		Setting typeSetting = SettingsBase.settingForCourse("defaultWorkType",course, ec);
		if(typeSetting != null && typeSetting.numericValue() != null) {
			try {
				return (WorkType)EOUtilities.objectWithPrimaryKeyValue(
						ec, "WorkType", typeSetting.numericValue());
			} catch (Exception e) {
				Logger.getLogger("rujel.criterial").log(
						WOLogLevel.WARNING,"Default WorkType is unknown",
						new Object[] {typeSetting, e});
			}
		}
		return defaultType(ec);
	}

	public static WorkType defaultType(EOEditingContext ec) {
		EOQualifier qual = new EOKeyValueQualifier(DFLT_FLAGS_KEY,
				EOQualifier.QualifierOperatorLessThan, Integer.valueOf(16));
		EOFetchSpecification fs = new EOFetchSpecification(ENTITY_NAME,qual,
				ModulesInitialiser.sorter);
		fs.setFetchLimit(1);
		NSArray found = ec.objectsWithFetchSpecification(fs);
		if(found == null || found.count() == 0) {
			qual = new EOKeyValueQualifier(DFLT_FLAGS_KEY,
					EOQualifier.QualifierOperatorLessThan, Integer.valueOf(64));
			fs.setQualifier(qual);
			found = ec.objectsWithFetchSpecification(fs);
			if(found == null || found.count() == 0)
				return null;
		}
		return (WorkType)found.objectAtIndex(0);
	}
	
	public static WorkType getSpecType(EOEditingContext ec, String type) {
		Integer typeFlags = (Integer)specTypes.valueForKey(type);
		if(typeFlags == null)
			throw new IllegalArgumentException("Unknown type name");
		NSArray found = EOUtilities.objectsMatchingKeyAndValue(ec, ENTITY_NAME,
				DFLT_FLAGS_KEY, typeFlags);
		if(found != null && found.count() > 0)
			return (WorkType)found.objectAtIndex(0);
		NSDictionary typeProps = (NSDictionary)WOApplication.application().valueForKeyPath(
				"strings.RujelCriterial_Strings.spesTypes." + type);
		if(typeProps == null)
			throw new IllegalArgumentException("Could not get system type properties");
		EOEditingContext tmpEc = ec;
		if(ec.hasChanges())
			tmpEc = new EOEditingContext(ec.rootObjectStore());
		NSArray sort = new NSArray(new EOSortOrdering(SORT_KEY,EOSortOrdering.CompareDescending));
		EOFetchSpecification fs = new EOFetchSpecification(ENTITY_NAME,null,sort);
		fs.setFetchLimit(0);
		found = tmpEc.objectsWithFetchSpecification(fs);
		WorkType workType = (WorkType)EOUtilities.createAndInsertInstance(tmpEc, ENTITY_NAME);
		workType.takeValuesFromDictionary(typeProps);
		if(found == null || found.count() == 0) {
			workType.setSort(Integer.valueOf(1));
		} else {
			WorkType oldType = (WorkType)found.objectAtIndex(0);
			workType.setSort(Integer.valueOf(oldType.sort().intValue() +1));
		}
		try {
			tmpEc.saveChanges();
			if(tmpEc != ec)
				workType = (WorkType)EOUtilities.localInstanceOfObject(ec, workType);
			Logger.getLogger("rujel.criterial").log(WOLogLevel.COREDATA_EDITING,
					"Autocreated system WorkType '" + type + '\'',workType);
		} catch (Exception e) {
			if (ec instanceof SessionedEditingContext) {
				WOSession ses = ((SessionedEditingContext)ec).session();
				Logger.getLogger("rujel.criterial").log(WOLogLevel.WARNING,
					"Failed autocreating system WorkType '" + type + '\'',new Object[] {ses,e});
			} else {
				Logger.getLogger("rujel.criterial").log(WOLogLevel.WARNING,
						"Failed autocreating system WorkType '" + type + '\'',e);
			}
			throw new NSForwardException(e, "Failed to autocreate system WorkType");
		}
		return workType;
	}

	public int useCount() {
		NSArray found = EOUtilities.objectsMatchingKeyAndValue(editingContext(),
				Work.ENTITY_NAME, Work.WORK_TYPE_KEY, this);
		if(found == null)
			return 0;
		return found.count();
	}
	
	public boolean isUsed() {
		EOQualifier qual = new EOKeyValueQualifier(Work.WORK_TYPE_KEY, 
				EOQualifier.QualifierOperatorEqual, this);
		EOFetchSpecification fs = new EOFetchSpecification(Work.ENTITY_NAME, qual, null);
		fs.setFetchLimit(1);
		NSArray found = editingContext().objectsWithFetchSpecification(fs);
		return (found != null && found.count() > 0);
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
		super.setDfltFlags(Integer.valueOf(0));
		setDfltWeight(BigDecimal.ZERO);
	}

	
	public void turnIntoFault(EOFaultHandler handler) {
		super.turnIntoFault(handler);
		_flags = null;
	}
	
	public String weightHex() {
		if(colorWeight() == null)
			return null;
		return colorWeight().substring(1).toUpperCase();
	}
	
	public void setWeightHex(String hex) {
		if(hex != null) {
			if(hex.charAt(0) != '#')
				hex = "#" + hex.toLowerCase();
			else
				hex = hex.toLowerCase();
		}
		setColorWeight(hex);
	}

	public String noWeightHex() {
		if(colorNoWeight() == null)
			return null;
		return colorNoWeight().substring(1).toUpperCase();
	}
	
	public void setNoWeightHex(String hex) {
		if(hex != null) {
			if(hex.charAt(0) != '#')
				hex = "#" + hex.toLowerCase();
			else
				hex = hex.toLowerCase();
		}
		setColorNoWeight(hex);
	}
	
	public BigDecimal trimmedWeight() {
		BigDecimal weight = super.dfltWeight();
		if(weight == null) return null;
		if(weight.compareTo(BigDecimal.ZERO) == 0)
			return BigDecimal.ZERO;
		weight = weight.stripTrailingZeros();
		if(weight.scale() < 0)
			return weight.setScale(0);
		return weight;
	}
	
	public static NSArray filteredTypesForCourse(EduCourse course) {
		int mask = SettingsBase.numericSettingForCourse("WorkTypeMask", course, null, 0);
		EOQualifier qual = activeQualifier;
		if(mask != 0) {
			NSMutableArray goes = new NSMutableArray();
			for (int i = 0; i < 32; i++) {
				if(Flags.getFlag(i, mask))
					goes.addObject(new Integer(i));
			}
			qual = Various.getEOInQualifier(SORT_KEY, goes);
			qual = new EOAndQualifier(new NSArray(new EOQualifier[] {activeQualifier,qual}));
		}
		EOFetchSpecification fs = new EOFetchSpecification(ENTITY_NAME,qual,ModulesInitialiser.sorter);
		return course.editingContext().objectsWithFetchSpecification(fs);
	}
}
