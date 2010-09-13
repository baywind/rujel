// Mark.java

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

import net.rujel.base.Indexer;
import net.rujel.base.SettingsBase;
import net.rujel.interfaces.*;
import net.rujel.reusables.SessionedEditingContext;
import com.webobjects.foundation.*;
import com.webobjects.eoaccess.EOUtilities;
import com.webobjects.eocontrol.*;
import com.webobjects.appserver.WOApplication;

public class Mark extends _Mark {
	
	public static void init() {
		EOInitialiser.initialiseRelationship("Mark","student",false,"studentID","Student")
						.anyInverseRelationship().setPropagatesPrimaryKey(true);
	}
	
    public Mark() {
        super();
    }

/*
    // If you add instance variables to store property values you
    // should add empty implementions of the Serialization methods
    // to avoid unnecessary overhead (the properties will be
    // serialized for you in the superclass).
    private void writeObject(java.io.ObjectOutputStream out) throws java.io.IOException {
    }

    private void readObject(java.io.ObjectInputStream in) throws java.io.IOException, 
    				java.lang.ClassNotFoundException {
    }
*/

	public double weightedFraction() {
		EOEnterpriseObject mask = work().getCriterMask(criterion());
		if(mask == null)
			throw new IllegalStateException("This mark was not properly initialised");
		Number max = (Number)mask.valueForKey("max");
		Number weight = (Number)mask.valueForKey("weight");
		if(max == null || weight == null || value() == null)
			throw new IllegalStateException("This mark was not properly initialised");
		
		return value().doubleValue()*weight.doubleValue()/max.doubleValue();
	}
	
	public Student student() {
        return (Student)storedValueForKey("student");
    }
	
    public void setStudent(EOEnterpriseObject aValue) {
        takeStoredValueForKey(aValue, "student");
    }
	
	public void awakeFromInsertion(EOEditingContext ec) {
		super.awakeFromInsertion(ec);
		NSTimestamp now = null;
		if(ec instanceof SessionedEditingContext) {
			now = (NSTimestamp)((SessionedEditingContext)ec).session().valueForKey("today");
		}
		if(now == null)
			now = new NSTimestamp();
		setDateSet(now);
	}
	
/*	public void setCriterionName(String criterionName) {
		if(work() == null)
			throw new IllegalStateException("Work should be set first");
		EOEnterpriseObject mask = work().criterMaskNamed(criterionName);
		if(mask == null)
			throw new IllegalArgumentException("The criterion not defined for this work");
		EOEnterpriseObject crit = (EOEnterpriseObject)mask.valueForKey("criterion");
		setCriterMask(mask);
		setCriterion(crit);
	}*/
	
	public Integer validateValue(Number aValue) throws NSValidation.ValidationException {
		if (aValue == null) {
			String message = (String)WOApplication.application().valueForKeyPath(
					"strings.RujelCriterial_Strings.messages.illegalMark");
					message = String.format(message, work().criterName(criterion()));
			throw new NSValidation.ValidationException(message);
		}
		EOEnterpriseObject criterMask = work().getCriterMask(criterion());
		String message = null;
		if(criterMask == null) {
			message = (String)WOApplication.application().valueForKeyPath
				("strings.RujelCriterial_Strings.messages.unavailbleCriterion");
			throw new NSValidation.ValidationException(message);
		}
		Number max = (Number)criterMask.valueForKey("max");
		if(message == null && aValue.intValue() > max.intValue()) {
			message = "markValueOverMax";
		}
		if(message == null && aValue.intValue() < 0) {
			if(SettingsBase.numericSettingForCourse("allowMarkLowerZero", 
					work().course(), editingContext(), 0) > 0) {
				if(aValue.intValue() < -max.intValue()) {
					message = "markValueOverMax";
				}
			} else {
				message = "markValueLowerZero";
			}
		}
		if (message != null) {
			String criterion = work().criterName(criterion());
			message = (String)WOApplication.application().valueForKeyPath(
					"strings.RujelCriterial_Strings.messages." + message);
			if(criterion == null)
				criterion = criterion().toString();
			message = String.format(message,criterion);
			throw new NSValidation.ValidationException(message);
		}
		if(aValue instanceof Integer)
			return (Integer)aValue;
		return new Integer(aValue.intValue());
	}
	
	public void setValue(Integer aValue) {
        if(value() == null || aValue == null || value().intValue() != aValue.intValue()) {
			NSTimestamp today = null;
			if(editingContext() instanceof SessionedEditingContext) {
				today = (NSTimestamp)((SessionedEditingContext)
						editingContext()).session().valueForKey("today");
			}
			if(today == null)
				today = new NSTimestamp();
			super.setValue(aValue);
			setDateSet(today);
		}
    }
	
	public Indexer indexer() {
		CriteriaSet cset = work().critSet();
		if(cset == null)
			return null;
		return cset.indexerForCriter(criterion());
	}
	
	public String present() {
		Indexer idx = indexer();
		Integer value = value();
		if(value == null)
			return "?";
		if(idx == null)
			return value.toString();
		String result = idx.formattedForIndex(value.intValue(), null);
		if(result == null)
			return value.toString();
		return result;
	}
	
	public Boolean setPresent(String present) {
		if(present == null) {
			setValue(null);
			return null;
		}
		Integer value = null;
		Indexer idx = indexer();
		if(idx != null) {
			value = idx.indexForValue(present, true);
			if(value == null && !work().critSet().namedFlags().flagForKey("allowNumbers")) {
				setValue(null);
				return null;
			}
		}
		if(value == null) {
			try {
				value = new Integer(present);
			} catch (NumberFormatException e) {
				setValue(null);
				return null;
			}
		}
		if(value != null && value.equals(value()))
			return Boolean.FALSE;
		setValue(value);
		return Boolean.TRUE;
	}
	
	@Deprecated
	public Object handleQueryWithUnboundKey(String key) {
		if(key.equals(work().criterName(criterion())))
			return value();
		if(key.equals("text"))
			return work().noteForStudent(student());
		if(!work().usedCriteria().contains(key)) {
			if(key.equals(valueForKeyPath("work.integralPresenter.title")))
				return work().integralForStudent(student());
			return super.handleQueryWithUnboundKey(key);
		}
		Mark mark = work().markForStudentAndCriterion(student(), key);
		if(mark != null)
			return mark.value();
		else
			return null;
	}
	
	@Deprecated
	public void handleTakeValueForUnboundKey(Object value, String key) {
		if(key.equals(valueForKeyPath("criterion.title"))) {
			if(value != null) {
			setValue((Integer)value);
			} else {
				work().removeObjectFromBothSidesOfRelationshipWithKey(this,"marks");
				editingContext().deleteObject(this);
			}
			return;
		}
		if(key.equals("text")) {
			work().setNoteForStudent((String)value, student());
			return;
		}
		Integer criterion = work().critForName(key);
		if(criterion == null) {
			super.handleTakeValueForUnboundKey(value, key);
			return;
		}
		Mark mark = work().markForStudentAndCriterion(student(), key);
		if(value != null) {
			if(mark == null) {
				mark = (Mark)EOUtilities.createAndInsertInstance(work().editingContext(),"Mark");
				work().addObjectToBothSidesOfRelationshipWithKey(mark,"marks");
				mark.setStudent(student());
				mark.setCriterion(criterion);
			}
			if (mark.value() == null || ((Integer)value).intValue() != mark.value().intValue()) {
				mark.setValue((Integer)value);
			}
		} else {
			work().removeObjectFromBothSidesOfRelationshipWithKey(mark,"marks");
			editingContext().deleteObject(mark);
		} 
	}

}
