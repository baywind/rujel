// PeriodType.java

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

package net.rujel.eduplan;

import net.rujel.base.MyUtility;
import net.rujel.base.SettingsBase;
import net.rujel.interfaces.*;
import net.rujel.reusables.SessionedEditingContext;
import net.rujel.reusables.SettingsReader;
import net.rujel.reusables.WOLogLevel;

import com.webobjects.foundation.*;
import com.webobjects.eocontrol.*;

import java.util.*;
import java.util.logging.*;
import com.webobjects.eoaccess.EOUtilities;

public class PeriodType extends _PeriodType  {
	
	public static NSArray sorter = new NSArray(EOSortOrdering.sortOrderingWithKey("inYearCount",EOSortOrdering.CompareDescending));
	
	public static void init() {
		EOInitialiser.initialiseRelationship("PeriodTypeUsage","course",false,"eduCourseID","EduCourse").setIsMandatory(false);//.anyInverseRelationship().setPropagatesPrimaryKey(true);
		EOInitialiser.initialiseRelationship("PeriodTypeUsage","eduGroup",false,"eduGroupID","EduGroup").setIsMandatory(false);
	}
	
	public PeriodType() {
        super();
    }

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
		super.awakeFromInsertion(ec);
		setArchiveSince(new Integer(0));
	}
	
	public static NSArray allPeriodTypes(EOEditingContext ec) {
		return allPeriodTypes(ec,null);
	}
	
	public static NSArray allPeriodTypes(EOEditingContext ec,Number eduYear) {
		EOQualifier qual = new EOKeyValueQualifier("archiveSince",EOQualifier.QualifierOperatorEqual,new Integer(0));
		if(eduYear != null) {
			NSMutableArray quals = new NSMutableArray(qual);
			qual = new EOKeyValueQualifier("archiveSince",EOQualifier.QualifierOperatorGreaterThan,eduYear);
			quals.addObject(qual);
			qual = new EOOrQualifier(quals);
		}
		
		EOFetchSpecification fs = new EOFetchSpecification("PeriodType",qual,sorter);
		return ec.objectsWithFetchSpecification(fs);
	}
	
	public NSArray generatePeriodsFromTemplates(int eduYear) {
		NSArray templates = templates();
		Logger logger =Logger.getLogger("rujel.PeriodType");
		if(templates == null || templates.count() == 0) {
			logger.logp(Level.INFO,getClass().getName(),"generatePeriodsFromTemplates","Could not generate periods from templates in year " + eduYear + " because no period templates specified for this period type",this);
			throw new IllegalStateException("No period templates specified for this period type");
		}
		Enumeration en = templates.objectEnumerator();
		NSMutableArray result = new NSMutableArray();
		while (en.hasMoreElements()) {
			PeriodTemplate curr = (PeriodTemplate)en.nextElement();
			/*
			EduPeriod per = (EduPeriod)EOUtilities.createAndInsertInstance(editingContext(),"EduPeriod");
			per.setPeriodType(this);
			per.setEduYear(new Integer(eduYear));
			per.takeValueForKey(curr.valueForKey("perNum"),"num");

			Number date = (Number)curr.valueForKey("beginMonth");
			int month = (date==null)?0:date.intValue();
			int year = (month <= 12)?eduYear:eduYear + 1;
			if(month > 12) month = month - 12;
			date = (Number)curr.valueForKey("beginDay");
			int day = (date==null)?0:date.intValue();
			NSTimestamp datum = new NSTimestamp(year,month,day,0,0,0,TimeZone.getDefault());
			per.setBegin(datum);

			date = (Number)curr.valueForKey("endMonth");
			month = (date==null)?0:date.intValue();
			year = (month <= 12)?eduYear:eduYear + 1;
			if(month > 12) month = month - 12;
			date = (Number)curr.valueForKey("endDay");
			day = (date==null)?0:date.intValue();
			datum = new NSTimestamp(year,month,day,0,0,0,TimeZone.getDefault());
			per.setEnd(datum);
			
			result.addObject(per);*/
			result.addObject(curr.makeEduPeriod(eduYear));
		}
		EOSortOrdering.sortArrayUsingKeyOrderArray(result,MyUtility.numSorter);
		logger.logp(Level.INFO,getClass().getName(),"generatePeriodsFromTemplates","Generated periods from templates in year " + eduYear,this);
		return result;
	}
	
	public EduPeriod currentPeriod(NSTimestamp date) {
		if(date == null) {
			if(editingContext() instanceof SessionedEditingContext) {
				date = (NSTimestamp)((SessionedEditingContext)editingContext()).session().valueForKey("today");
			}
			if(date == null) date = new NSTimestamp();
		}
		EOQualifier qual = new EOKeyValueQualifier("periodType",EOQualifier.QualifierOperatorEqual,this);
		NSMutableArray quals = new NSMutableArray(qual);
		qual = new EOKeyValueQualifier("begin",EOQualifier.QualifierOperatorLessThanOrEqualTo,date);
		quals.addObject(qual);
		qual = new EOKeyValueQualifier("end",EOQualifier.QualifierOperatorGreaterThanOrEqualTo,date);
		quals.addObject(qual);
		qual = new EOAndQualifier(quals);
		EOFetchSpecification fspec = new EOFetchSpecification("EduPeriod",qual,null);
		NSArray result = editingContext().objectsWithFetchSpecification(fspec);
		if(result == null || result.count() == 0) {
			if(!editingContext().hasChanges()) {
				Number year = net.rujel.base.MyUtility.eduYearForDate(date);
				if(!SettingsReader.boolForKeyPath("edu.autogenerateEduPeriods", true))
					return null;
				NSArray periodsForYear = EOUtilities.objectsMatchingKeyAndValue(editingContext(), "EduPeriod", "eduYear", year);
				if(periodsForYear != null && periodsForYear.count() > 0)
					return null;
				try {
					editingContext().lock();
					result = generatePeriodsFromTemplates(year.intValue());
					editingContext().saveChanges();
				} catch (Exception ex) {
					Logger.getLogger("rujel.eduresults").log(WOLogLevel.WARNING,"Failed to generate eduPeriods for edu year",year);
					editingContext().revert();
				} finally {
					editingContext().unlock();
				}
			}
			if(result == null || result.count() == 0)
				return null;
			result = EOQualifier.filteredArrayWithQualifier(result,qual);
		} 
		if(result.count() > 1) {
			Logger.getLogger("rujel.eduresults").log(WOLogLevel.WARNING,"Several EduPeriods found overlapping selected date",date);
			//throw new EOUtilities.MoreThanOneException("Several EduPeriods found overlapping selected date");
		}
		if(result == null || result.count() == 0)
			return null;
		return (EduPeriod)result.objectAtIndex(0);
	}
	
	public EduPeriod currentPeriod() {
		/*
		 * NSTimestamp today = null;
		if(editingContext() instanceof SessionedEditingContext) {
			today = (NSTimestamp)((SessionedEditingContext)editingContext()).session().valueForKey("today");
		}
		if(today == null) today = new NSTimestamp();*/
		return currentPeriod(null);
	}

	public NSArray periodsForYear(Integer year) {
		EOQualifier[] quals = new EOQualifier[2];
		quals[0] = new EOKeyValueQualifier(EduPeriod.EDU_YEAR_KEY,EOQualifier.QualifierOperatorEqual,year);
		quals[1] = new EOKeyValueQualifier(EduPeriod.PERIOD_TYPE_KEY,EOQualifier.QualifierOperatorEqual,this);
		quals[0] = new EOAndQualifier(new NSArray(quals));
		NSArray ps = new NSArray(new EOSortOrdering(EduPeriod.NUM_KEY,EOSortOrdering.CompareAscending));
		EOFetchSpecification fs = new EOFetchSpecification(EduPeriod.ENTITY_NAME,quals[0],ps);
		return editingContext().objectsWithFetchSpecification(fs);
	}
}
