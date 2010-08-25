// CriteriaSet.java

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

package net.rujel.criterial;


import java.util.Enumeration;
import java.util.logging.Logger;

import net.rujel.base.Indexer;
import net.rujel.base.SettingsBase;
import net.rujel.interfaces.EduCourse;
import net.rujel.reusables.NamedFlags;
import net.rujel.reusables.WOLogLevel;

import com.webobjects.foundation.*;
import com.webobjects.appserver.WOApplication;
import com.webobjects.eocontrol.*;
import com.webobjects.eoaccess.EOUtilities;

public class CriteriaSet extends _CriteriaSet
{
	public static final NSArray sorter = new NSArray(EOSortOrdering.sortOrderingWithKey("criterion",EOSortOrdering.CompareAscending));
    public CriteriaSet() {
        super();
    }
	
    public static final NSArray flagNames = new NSArray(
    		new String[] {"fixMax","fixWeight","fixList","onlyCriter"});
    
	/*
	 // If you add instance variables to store property values you
	 // should add empty implementions of the Serialization methods
	 // to avoid unnecessary overhead (the properties will be
	 // serialized for you in the superclass).
	 private void writeObject(java.io.ObjectOutputStream out) throws java.io.IOException {
	 }
	 
	 private void readObject(java.io.ObjectInputStream in) throws java.io.IOException, java.lang.ClassNotFoundException {
	 }
	 */
    
    public void awakeFromInsertion(EOEditingContext ec) {
    	setFlags(new Integer(0));
    }
	
	public NSArray sortedCriteria() {
		return EOSortOrdering.sortedArrayUsingKeyOrderArray(criteria(),sorter);
	}
	
	public EOEnterpriseObject criterionNamed(String critName) {
		if(criteria() == null || criteria().count() == 0)
			return null;
		Enumeration enu = criteria().objectEnumerator();
		while (enu.hasMoreElements()) {
			EOEnterpriseObject crit = (EOEnterpriseObject) enu.nextElement();
			if(critName.equals(crit.valueForKey("title")))
				return crit;
		}
		return null;
	}

	public static String critNameForNum(Integer criter, CriteriaSet set) {
		if(criter.intValue() == 0)
			return "#";
		if(set == null)
			return Character.toString((char)('A' + criter.intValue() -1));
		return set.critNameForNum(criter);
	}

	public String critNameForNum(Integer criterion) {
		EOEnterpriseObject criter = criterionForNum(criterion);
		return (criter==null)?null:(String)criter.valueForKey("title");
	}
	
	public EOEnterpriseObject criterionForNum(Integer criterion) {
		if(criteria() == null || criteria().count() == 0)
			return null;
		Enumeration enu = criteria().objectEnumerator();
		while (enu.hasMoreElements()) {
			EOEnterpriseObject crit = (EOEnterpriseObject) enu.nextElement();
			if(criterion.equals(crit.valueForKey("criterion")))
				return crit;
		}
		return null;
	}
	
	public Integer criterionForName(String name) {
		if(name == null)
			return new Integer(0);
		EOEnterpriseObject crit = criterionNamed(name);
		return (crit == null)?null:(Integer)crit.valueForKey("criterion");
	}
	
	public EOEnterpriseObject addCriterion() {
		EOEnterpriseObject criterion = EOUtilities.createAndInsertInstance(
				editingContext(),"Criterion");
		NSArray criteria = criteria();
		Number num = null;
		if(criteria == null || criteria.count() == 0) {
			num = new Integer(1);
		} else {
			num = (Number)criteria().valueForKeyPath("@max.criterion");
			num = new Integer(num.intValue() + 1);
		}
		criterion.takeValueForKey(num,"criterion");
		addObjectToBothSidesOfRelationshipWithKey(criterion,"criteria");
		return criterion;
	}
	
	public static CriteriaSet critSetForCourse(EduCourse course) {
		EOEditingContext ec = course.editingContext();
		Integer set = SettingsBase.numericSettingForCourse(ENTITY_NAME, course,ec);
		if(set == null || set.intValue() == 0)
			return null;
		return (CriteriaSet)EOUtilities.objectWithPrimaryKeyValue(ec, ENTITY_NAME, set);
	}
	
	public static NSArray criteriaForCourse(EduCourse course) {
		CriteriaSet set = critSetForCourse(course);
		if (set!=null)
			return set.sortedCriteria();
		int maxCriter = maxCriterionForCourse(course);
		return criteriaForMax(maxCriter);
	}
	
	public static NSArray criteriaForMax(int maxCriter) {
		if(maxCriter == 0)
			return NSArray.EmptyArray;
		char first = 'A';
		NSDictionary[] result = new NSDictionary[maxCriter];
		for (int i = 0; i < maxCriter; i++) {
			String title = Character.toString((char)(first + i));
			NSDictionary critDict = new NSDictionary( new Object[]
					{title, new Integer(i + 1)} , new String[] {"title","criterion"});
			result[i] = critDict;
		}
		return new NSArray(result);
	}
	
	public static String titleForCriterion(int criterion) {
		if(criterion == 0)
			return "#";
		return Character.toString((char)('A' + criterion -1));
	}
	
	public static int maxCriterionForCourse(EduCourse course) {
		CriteriaSet set = critSetForCourse(course);
		if(set != null) {
			Integer max = (Integer)set.criteria().valueForKey("@max.criterion");
			return (max == null)?0:max.intValue();
		}
		NSArray works = EOUtilities.objectsMatchingKeyAndValue(course.editingContext(), 
				Work.ENTITY_NAME, "course", course);
		return maxCriterionInWorks(works);
	}
	
	public static int maxCriterionInWorks (NSArray works) {
		if(works == null || works.count() == 0)
			return 0;
		int max = 0;
		Enumeration enu = works.objectEnumerator();
		while (enu.hasMoreElements()) {
			Work work = (Work) enu.nextElement();
			NSArray mask = work.criterMask();
			if(mask != null && mask.count() > 0) {
				Integer wMax = (Integer)mask.valueForKeyPath("@max.criterion");
				if(wMax.intValue() > max)
					max = wMax.intValue();
			}
		}
		return max;
	}
	
	private NamedFlags _flags;
    public NamedFlags namedFlags() {
    	if(_flags==null) {
    		_flags = new NamedFlags(flags().intValue(),flagNames);
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
    		super.setFlags(flags.toInteger());
    	_flags = flags;
    }

    public void setFlags(Integer flags) {
    	_flags = null;
    	super.setFlags(flags);
    }
    
    /** Automatically sets dfltMax field of every criterion in CriteriaSet to
     * maxIndex of the attached Indexer (if exists)
     * 
     *  @return true if any changes were made */
	public boolean setMaxes() {
		NSArray rows = criteria();
		if(rows == null || rows.count() == 0)
			return false;
		boolean result = false;
		Enumeration enu = rows.objectEnumerator();
		while (enu.hasMoreElements()) {
			EOEnterpriseObject cr = (EOEnterpriseObject) enu.nextElement();
			Indexer idx = (Indexer)cr.valueForKey("indexer");
			if(idx == null || idx.maxIndex() == null)
				continue;
			if(idx.maxIndex().equals(cr.valueForKey("dfltMax")))
				continue;
			cr.takeValueForKey(idx.maxIndex(), "dfltMax");
			result = true;
		}
		return result;
	}
    
    public void validateForSave() {
    	super.validateForSave();
    	NSArray criteria = criteria();
    	if(namedFlags().flagForKey("fixList") || namedFlags().flagForKey("onlyCriter")) {
    		if(criteria == null || criteria.count() == 0)
    			throw new ValidationException((String)
    					WOApplication.application().valueForKeyPath(
    					"strings.RujelCriterial_Strings.messages.criteriaRequired"));
    	}
    	if(namedFlags().flagForKey("fixMax") || namedFlags().flagForKey("fixWeight")) {
    		Enumeration enu = criteria.objectEnumerator();
    		while (enu.hasMoreElements()) {
				EOEnterpriseObject cr = (EOEnterpriseObject) enu.nextElement();
				if(namedFlags().flagForKey("fixMax") && 
						cr.valueForKey("dfltMax") == null)
	    			throw new ValidationException((String)
	    					WOApplication.application().valueForKeyPath(
	    					"strings.RujelCriterial_Strings.messages.maxsRequired"));
				if(namedFlags().flagForKey("fixWeight") &&
						cr.valueForKey("dfltWeight") == null)
	    			throw new ValidationException((String)
	    					WOApplication.application().valueForKeyPath(
	    					"strings.RujelCriterial_Strings.messages.weightsRequired"));
			}
    	}
    }
}
