// PrognosUsage.java

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

package net.rujel.autoitog;


import com.webobjects.foundation.*;
import com.webobjects.eocontrol.*;

import java.util.logging.Logger;

import net.rujel.base.MyUtility;
import net.rujel.eduresults.PeriodType;
import net.rujel.interfaces.*;
import net.rujel.reusables.NamedFlags;
import net.rujel.reusables.WOLogLevel;

public class PrognosUsage extends _PrognosUsage
{
	public static final NSArray prognosUsageFlagNames = new NSArray(new String[]
	        {"active","manual","noTimeouts","priority"});

	public static void init() {
		EOInitialiser.initialiseRelationship("PrognosUsage","eduCourse",false,"courseID","EduCourse").anyInverseRelationship().setPropagatesPrimaryKey(true);
		EOInitialiser.initialiseRelationship("PrognosUsage","eduGroup",false,"eduGroupID","EduGroup").anyInverseRelationship().setPropagatesPrimaryKey(true);
		EOInitialiser.initialiseRelationship("PrognosUsage","eduCycle",false,"eduCycleID","EduCycle").anyInverseRelationship().setPropagatesPrimaryKey(true);
		//EOInitialiser.initialiseRelationship("PrognosUsage","periodType",false,"perTypeID","PeriodType").anyInverseRelationship().setPropagatesPrimaryKey(true);
	}

    public PrognosUsage() {
        super();
    }
    
    public void awakeFromInsertion(EOEditingContext ec) {
    	super.awakeFromInsertion(ec);
    	setFlags(new Byte((byte)0));
    	setCalculatorName("");
    	setEduYear(MyUtility.eduYearForDate(new NSTimestamp()));
    }

    public EduCourse eduCourse() {
        return (EduCourse)storedValueForKey("eduCourse");
    }	
    public void setEduCourse(EduCourse aValue) {
        takeStoredValueForKey(aValue, "eduCourse");
    }

    public EduGroup eduGroup() {
        return (EduGroup)storedValueForKey("eduGroup");
    }	
    public void setEduGroup(EduGroup aValue) {
        takeStoredValueForKey(aValue, "eduGroup");
    }

    public EduCycle eduCycle() {
        return (EduCycle)storedValueForKey("eduCycle");
    }	
    public void setEduCycle(EduCycle aValue) {
        takeStoredValueForKey(aValue, "eduCycle");
    }

    private NamedFlags _flags;
    public NamedFlags namedFlags() {
    	if(_flags==null) {
    		_flags = new NamedFlags(flags().intValue(),prognosUsageFlagNames);
    		try{
    		_flags.setSyncParams(this, getClass().getMethod("setNamedFlags", NamedFlags.class));
    		} catch (Exception e) {
    			Logger.getLogger("rujel.autoitog").log(WOLogLevel.WARNING,
						"Could not get syncMethod for PrognosUsage flags",e);
			}
    	}
    	return _flags;
    }
    
    public void setNamedFlags(NamedFlags flags) {
    	_flags = flags;
    	setFlags(new Byte(flags.byteValue()));
    }
    
    private int _priority = -1;
    public int priority() {
    	if(_priority < 0) {
    		if(eduCourse() != null) {
    			_priority = 100;
    		} else {
    			_priority = 0;
				if(eduCourse() != null)
					_priority += 4;
				if(eduCycle() != null)
					_priority += 3;
				if(namedFlags().flagForKey("priority"))
					_priority += 2;
    		}
    	}
    	return _priority;
    }
    
    protected Calculator _calculator;
    public Calculator calculator() {
    	if(_calculator == null) {
    		_calculator = Calculator.calculatorForName(calculatorName());
    	}
    	return _calculator;
    }
    
    public boolean noCalculator() {
    	String calcName = calculatorName();
		if(calcName == null || calcName.length() == 0 || calcName.equalsIgnoreCase("none"))
			return true;
		return false;
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
	public static PrognosUsage prognosUsage(EduCourse course, PeriodType perType) {
		EOQualifier qual1 = new EOKeyValueQualifier("periodType",EOQualifier.QualifierOperatorEqual,perType);
		EOQualifier qual2 = new EOKeyValueQualifier("eduCourse",EOQualifier.QualifierOperatorEqual,course);
		NSMutableArray quals = new NSMutableArray(new EOQualifier[] {qual1,qual2});
		EOQualifier qual = new EOAndQualifier(quals);
		EOFetchSpecification fs = new EOFetchSpecification("PrognosUsage",qual,null);
		EOEditingContext ec = course.editingContext();
		NSArray result = ec.objectsWithFetchSpecification(fs);
		if(result != null && result.count() > 0) {
			return (PrognosUsage)result.objectAtIndex(0);
		}
		
		NSArray args = new NSArray(new Object[]{course.eduYear(),course.cycle(),course.eduGroup()});
		String qualString = "(eduYear = %d OR eduYear = 0) AND (eduCycle = %@ OR eduCycle = nil) AND (eduGroup = %@ OR eduGroup = nil) AND eduCourse = nil";
		qual2 = EOQualifier.qualifierWithQualifierFormat(qualString,args);

		quals.replaceObjectAtIndex(qual2, 1);
		qual = new EOAndQualifier(quals);
		fs.setQualifier(qual);
		result = ec.objectsWithFetchSpecification(fs);
		if(result == null || result.count() == 0)
			return null;
		int idx = 0;
		int count = result.count();
		if(count > 1) {
			int prior = -1;
			for (int i = 0; i < count; i++) {
				PrognosUsage cur = (PrognosUsage)result.objectAtIndex(i);
				/*int cp = 0;
				if(cur.eduCourse() != null)
					cp += 4;
				if(cur.eduCycle() != null)
					cp += 3;
				if((cp<prior) && ((prior-cp) < 2)) {
					if(cur.namedFlags().flagForKey("priority"))
						cp += 2;
				}*/
				if(cur.priority() > prior) {
					idx = i;
					prior = cur.priority();
				}				
			}
		}
		return (PrognosUsage)result.objectAtIndex(idx);
	}
	
	public void turnIntoFault(EOFaultHandler handler) {
		super.turnIntoFault(handler);
		_priority = -1;
		_flags = null;
		_calculator = null;
	}
}
