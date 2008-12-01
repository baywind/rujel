//  Bonus.java

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

package net.rujel.autoitog;

import java.math.BigDecimal;
import java.util.logging.Logger;

import com.webobjects.appserver.WOApplication;
import com.webobjects.eocontrol.*;
import com.webobjects.foundation.NSMutableDictionary;

import net.rujel.interfaces.*;
import net.rujel.reusables.*;

public class Bonus extends _Bonus {

	public static void init() {
		EOInitialiser.initialiseRelationship("Bonus","cycle",false,"cycleID","EduCycle")
			.anyInverseRelationship().setPropagatesPrimaryKey(true);
		EOInitialiser.initialiseRelationship("Bonus","student",false,"studentID","Student")
			.anyInverseRelationship().setPropagatesPrimaryKey(true);
	}

	public Student student() {
        return (Student)storedValueForKey("student");
    }
	
    public void setStudent(Student aValue) {
        takeStoredValueForKey(aValue, "student");
    }
	
	public EduCycle cycle() {
        return (EduCycle)storedValueForKey("cycle");
    }
	
    public void setCycle(EduCycle aValue) {
        takeStoredValueForKey(aValue, "cycle");
    }

    public void awakeFromInsertion(EOEditingContext ec) {
		super.awakeFromInsertion(ec);
		setValue(BigDecimal.ZERO);
		setMark(" ");
	}

    public void zeroBonus() {
    	BigDecimal bonus = value();
   		if(bonus != null && bonus.compareTo(BigDecimal.ZERO) != 0)
			setValue(BigDecimal.ZERO);
   		else
   			calculateBonus(prognosis(), this, false);
   		//namedFlags().setFlagForKey(false, "keepBonus");
    }
    
    public Prognosis prognosis() {
    	try {
    		return (Prognosis)prognoses().objectAtIndex(0);
    	} catch (Exception e) {
    		Object[] args = new Object[] {this,e};
    		Logger.getLogger("rujel.autoitog").log(WOLogLevel.FINE,
    				"Could not get Prognosis for Bonus", args);   		
    		return null;
    	}
    }

    public void initBonus(Prognosis prognosis, boolean setValue) {
    	addObjectToBothSidesOfRelationshipWithKey(prognosis.student(), "student");
    	addObjectToBothSidesOfRelationshipWithKey(prognosis.eduPeriod(), EDU_PERIOD_KEY);
    	addObjectToBothSidesOfRelationshipWithKey(prognosis.eduCourse().cycle(), "cycle");
    	addObjectToBothSidesOfRelationshipWithKey(prognosis,PROGNOSES_KEY);
    	calculateBonus(prognosis, this, setValue);
    }
    
    public BigDecimal calculateValue(Prognosis prognosis, boolean update) {
    	try {
    		BigDecimal topValue = prognosis.prognosUsage().borderSet().borderForKey(mark());
    		topValue = topValue.movePointLeft(2);
    		BigDecimal prognos = prognosis.value();
    		BigDecimal bonus = (topValue.compareTo(prognos) < 0)?
    				BigDecimal.ZERO : topValue.subtract(prognos);
    		if(update)
    			setValue(bonus);
    		return bonus;
    	} catch (Exception e) {
    		Object[] args = new Object[] {prognosis,e};
    		Logger.getLogger("rujel.autoitog").log(WOLogLevel.FINE,
    				"Bonus not applicable for this prognosis", args);
    		return null;
    	}
    }
    
    public boolean submitted() {
    	return (value().compareTo(calculateValue(prognosis(), false)) == 0);
    }
    
    public static BigDecimal calculateBonus(Prognosis prognosis,Bonus toUpdate,boolean setValue) {
    	try {
    		EOEnterpriseObject border = prognosis.prognosUsage().borderSet().
    					borderForFraction(prognosis.value(), true); 
    		if(border == null)
    			return null;
    		BigDecimal topValue = (BigDecimal)border.valueForKey("least");
    		topValue = topValue.movePointLeft(2);
    		BigDecimal prognos = prognosis.value();
    		if(topValue.compareTo(prognos) < 0)
    			return null;
    		BigDecimal bonus = topValue.subtract(prognos);
    		if(toUpdate != null) {
    			toUpdate.setMark((String)border.valueForKey("title"));
    			if(setValue)
    				toUpdate.setValue(bonus);
    		}
    		return bonus;
    	} catch (Exception e) {
    		Object[] args = new Object[] {prognosis,e};
    		Logger.getLogger("rujel.autoitog").log(WOLogLevel.FINE,
    				"Bonus not applicable for this prognosis", args);
    		return null;
    	}
    }
    
	public NSMutableDictionary extItog() {
		NSMutableDictionary result = new NSMutableDictionary(eduPeriod(),"eduPeriod");
		result.takeValueForKey(cycle(), "cycle");
		StringBuffer buf = new StringBuffer((String)WOApplication.application()
				.valueForKeyPath("extStrings.RujelAutoItog_AutoItog.ui.markHasBonus"));
		buf.append('.').append(' ').append((String)WOApplication.application()
				.valueForKeyPath("extStrings.RujelAutoItog_AutoItog.properties.Timeout.reason"));
		buf.append(": <em>").append(reason()).append("</em>");
		result.takeValueForKey(buf.toString(), "text");
		return result;
	}
}
