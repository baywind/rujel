// BachalaureatCalculator.java

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

import net.rujel.criterial.*;
import net.rujel.eduresults.EduPeriod;
import net.rujel.interfaces.EduCourse;
import net.rujel.interfaces.PerPersonLink;
import net.rujel.interfaces.Student;
import net.rujel.reusables.Various;
import net.rujel.reusables.WOLogLevel;

import com.webobjects.foundation.*;
import com.webobjects.eoaccess.EOObjectNotAvailableException;
import com.webobjects.eoaccess.EOUtilities;
import com.webobjects.eocontrol.*;
//import com.webobjects.eoaccess.EOUtilities;

import java.math.*;
import java.util.Enumeration;
import java.util.logging.Logger;

public class BachalaureatCalculator extends Calculator {
	public static final BachalaureatCalculator sharedInstance = new BachalaureatCalculator();
	
//	protected Student _student;
	protected static final int WEIGHT = 0;
	protected static final int VALUE = 1;
	protected static final int MAX = 2;
	/*
	public OldFormulaStatsCalculator(Student student) {
		_student = student;
	}*/
	/*
	public double integralForCourseAndPeriod(EduCourse course, Period period) {
	
		
		EOEditingContext ec = course.editingContext();
		
		NSMutableArray args = new NSMutableArray(new Object[] {student,course.cycle(),period.begin(),period.end()});
		String qualifierFormat = "student = %@ AND work.course.cycle = %@ AND (work.date >= %@ AND work.date <= %@)";
		NSArray allMarks = EOUtilities.objectsWithQualifierFormat(ec,"Mark",qualifierFormat,args);
	}*/
	
	
	public NSDictionary agregateMarks(NSArray allMarks) {
		if(allMarks == null || allMarks.count() == 0)
			return null;
		NSMutableDictionary dict = new NSMutableDictionary();
		NSMutableSet optWorks = new NSMutableSet();
		Enumeration en = allMarks.objectEnumerator();
		while (en.hasMoreElements()) {
			Mark mark = (Mark)en.nextElement();
			BigDecimal weightValue = mark.work().weight();
			Integer critWeight = (Integer)mark.valueForKeyPath("criterMask.weight");
			if(weightValue == null || weightValue.compareTo(BigDecimal.ZERO) == 0)
				continue;
			if(critWeight != null && critWeight.intValue() == 0)
				continue;
			EOEnterpriseObject crit = mark.criterion();
			if(crit == null)
				continue;
			if(mark.work().type().intValue() == Work.OPTIONAL) {
				optWorks.addObject(mark.work());
			}
			BigDecimal[] agregator = (BigDecimal[])dict.objectForKey(crit);
			if(agregator == null) {
				agregator = new BigDecimal[] {BigDecimal.ZERO,BigDecimal.ZERO,BigDecimal.ZERO};
				dict.setObjectForKey(agregator,crit);
			}
			//double weight = weightValue.doubleValue();
			Number value = mark.value();
			Number max = (Number)mark.valueForKeyPath("criterMask.max");
			if(value == null || max == null)
				continue;
			agregator[WEIGHT] = agregator[WEIGHT].add(weightValue);
			agregator[VALUE] = agregator[VALUE].add(weightValue.multiply(new BigDecimal(value.intValue())));
			agregator[MAX] = agregator[MAX].add(weightValue.multiply(new BigDecimal(max.intValue())));
		}
		if(optWorks.count() > 0) {
			dict.setObjectForKey(agregateWorks(optWorks.allObjects(), false), "optionalWorks");
		}
		return dict;
	}
	
	public NSDictionary agregateWorks(NSArray works) {
		return agregateWorks(works, true);
	}

	protected NSDictionary agregateWorks(NSArray works,boolean filter) {
		if(works == null || works.count() == 0)
			return null;
		NSMutableDictionary dict = new NSMutableDictionary();
		Enumeration en = works.objectEnumerator();
		while (en.hasMoreElements()) { // works
			Work work = (Work)en.nextElement();
			BigDecimal weightValue = work.weight();
			if(filter) {
				if(work.type().intValue() == Work.OPTIONAL)
					continue;
				if(weightValue == null || weightValue.compareTo(BigDecimal.ZERO) == 0)
					continue;
			}
			Enumeration masks = work.criterMask().objectEnumerator();
			while(masks.hasMoreElements()) { //criters
				EOEnterpriseObject currMask = (EOEnterpriseObject)masks.nextElement();
				EOEnterpriseObject crit = (EOEnterpriseObject)currMask.valueForKey("criterion");

				BigDecimal[] agregator = (BigDecimal[])dict.objectForKey(crit);
				if(agregator == null) {
					agregator = new BigDecimal[] {BigDecimal.ZERO,BigDecimal.ZERO,BigDecimal.ZERO};
					dict.setObjectForKey(agregator,crit);
				}
				//Number value = (Number)currMask.valueForKey("weight");
				Number max = (Number)currMask.valueForKey("max");
				if(max == null)
					continue;
				agregator[WEIGHT] = agregator[WEIGHT].add(weightValue);
				//agregator[VALUE] = agregator[VALUE].add(weightValue.multiply(new BigDecimal(value.intValue())));
				agregator[MAX] = agregator[MAX].add(weightValue.multiply(new BigDecimal(max.intValue())));
				
/*
				BigDecimal agregator = (BigDecimal)dict.objectForKey(crit);
				if(agregator == null) {
					agregator = work.weight();
				} else {
					agregator = agregator.add(work.weight());
				}
				dict.setObjectForKey(agregator,crit);*/
			}
		}
		return dict;
	}
	
	
	
	public double getIntegral(NSArray allMarks) {
		return getIntegral(agregateMarks(allMarks));
	}
	
	public double getIntegral(NSDictionary agregatedMarks) {
		if(agregatedMarks == null || agregatedMarks.count() == 0)
			return 0;
		Enumeration en = agregatedMarks.objectEnumerator();
		double valSum = 0;
		double maxSum = 0;
		while (en.hasMoreElements()) {
			BigDecimal[] agregator = (BigDecimal[])en.nextElement();
			valSum = valSum + (agregator[VALUE].doubleValue()/agregator[WEIGHT].doubleValue());
			maxSum = maxSum + (agregator[MAX].doubleValue()/agregator[WEIGHT].doubleValue());
		}
		return valSum/maxSum;
	}
	/*
	public double getIntegral(Object agregatedMarks, Object agregatedWorks) {
		return getIntegral((NSDictionary)agregatedMarks, (NSDictionary)agregatedWorks);
	}*/
	
	public double getIntegral(NSDictionary agregatedMarks, NSDictionary agregatedWorks) {
		if(agregatedWorks == null) return 0;
		if(agregatedMarks == null) return 0;
		double valSum = 0;
		double maxSum = 0;
		NSDictionary optWorks = (NSDictionary)agregatedMarks.valueForKey("optionalWorks");
		Enumeration enu = agregatedMarks.keyEnumerator();
		while (enu.hasMoreElements()) {
			EOEnterpriseObject crit = (EOEnterpriseObject)enu.nextElement();
			BigDecimal[] wagr = (BigDecimal[])agregatedWorks.objectForKey(crit);
			BigDecimal[] optAgr = (optWorks == null)?null:(BigDecimal[])optWorks.objectForKey(crit);
			if(wagr == null || BigDecimal.ZERO.compareTo(wagr[WEIGHT]) == 0 || BigDecimal.ZERO.compareTo(wagr[MAX]) == 0) {
				if(optAgr == null || BigDecimal.ZERO.compareTo(optAgr[WEIGHT].add(wagr[WEIGHT])) == 0
						|| BigDecimal.ZERO.compareTo(optAgr[MAX].add(wagr[MAX])) == 0)
				continue;
			}
			double sumWeight = wagr[WEIGHT].doubleValue();
			if(optAgr != null && optAgr[WEIGHT] != null)
				sumWeight = sumWeight + optAgr[WEIGHT].doubleValue();
			double sumMax = wagr[MAX].doubleValue();
			if(optAgr != null && optAgr[MAX] != null)
				sumMax = sumMax + optAgr[MAX].doubleValue();
			BigDecimal[] magr = (BigDecimal[])agregatedMarks.objectForKey(crit);
			double sumVal = (magr==null)?0:magr[VALUE].doubleValue();
			
			valSum = valSum + (sumVal/sumWeight);
			maxSum = maxSum + (sumMax/sumWeight);
		}
		return valSum/maxSum;
	}
	/*
	public BigDecimal getComplete(Object agregatedMarks, Object agregatedWorks) {
		return getComplete((NSDictionary)agregatedMarks, (NSDictionary)agregatedWorks);
	}*/
	
	public BigDecimal getComplete(NSDictionary agregatedMarks, NSDictionary agregatedWorks) {
		if(agregatedWorks == null) return null;
		if(agregatedMarks == null) return BigDecimal.ZERO;
		BigDecimal sumMarks = null;
		BigDecimal sumWorks = null;
		boolean equals = true;
		Enumeration enu = agregatedWorks.keyEnumerator();
		while (enu.hasMoreElements()) {
			EOEnterpriseObject crit = (EOEnterpriseObject)enu.nextElement();
			BigDecimal[] wagr = (BigDecimal[])agregatedWorks.objectForKey(crit);
			BigDecimal val = (wagr==null)?null:wagr[WEIGHT];
			if(val != null) {
				sumWorks = (sumWorks == null)?val:sumWorks.add(val);
			}
			BigDecimal[] magr = (BigDecimal[])agregatedMarks.objectForKey(crit);
			val = (magr==null)?null:magr[WEIGHT];
			if(val != null) {
				sumMarks = (sumMarks == null)?val:sumMarks.add(val);
			}
			if(equals && (sumMarks == null || sumWorks.compareTo(sumMarks) != 0)) {
				equals = false;
			}
		}
		if(sumWorks == null) return null;
		if(sumMarks == null) return BigDecimal.ZERO;
		
		BigDecimal complete = sumMarks.divide(sumWorks,4,BigDecimal.ROUND_HALF_UP);
		if(equals) {
			complete = BigDecimal.ONE;
		} else {
			if(complete.compareTo(BigDecimal.ONE) == 0) {
				complete = complete.negate();
			}
		}
		return complete;
	}
	/*
	public void updatePrognosis(Prognosis prognosis, NSDictionary agregatedMarks, NSDictionary agregatedWorks) {
		double integral = getIntegral(agregatedMarks,agregatedWorks);
		BigDecimal value = new BigDecimal(integral,new MathContext(4));
		if(prognosis.value() == null || prognosis.value().compareTo(value) != 0){
			prognosis.setValue(value);
		}
		value = getComplete(agregatedMarks,agregatedWorks);
		if(prognosis.complete() == null || prognosis.complete().compareTo(value) != 0) {
			prognosis.setComplete(value);
		}		
	}*/
	
	
	public PerPersonLink calculatePrognoses(EduCourse course, EduPeriod period) {
			//return prognosesForCourseAndPeriod(course,period);
		NSMutableDictionary dict = new NSMutableDictionary(period,"eduPeriod");
		dict.setObjectForKey(course,"eduCourse");
		EOEditingContext ec = course.editingContext();
		
		NSArray works = works(course, period);
		if(works.count() == 0) {
			return null;
		}
		NSDictionary agregatedWorks = agregateWorks(works);
		
		NSMutableArray quals = new NSMutableArray(Various.getEOInQualifier("work",works));//allWorks));
		quals.addObject(NSKeyValueCoding.NullValue);
		
		EOFetchSpecification fs = new EOFetchSpecification("Mark",null,null);
		fs.setRefreshesRefetchedObjects(true);
		Enumeration enu = course.groupList().objectEnumerator();
		NSMutableDictionary result = new NSMutableDictionary();
		while (enu.hasMoreElements()) {
			Student student = (Student)enu.nextElement();
			
			quals.replaceObjectAtIndex(new EOKeyValueQualifier("student",EOQualifier.QualifierOperatorEqual,student),1);
			fs.setQualifier(new EOAndQualifier(quals));
			NSArray allMarks = ec.objectsWithFetchSpecification(fs);
			NSDictionary agregatedMarks = agregateMarks(allMarks);
			
			dict.setObjectForKey(student,"student");
			Prognosis progn = prognosisForStudent(dict, ec, agregatedMarks, agregatedWorks);
			/*try {
				progn = (Prognosis)EOUtilities.objectMatchingValues(ec,"Prognosis",dict);
			} catch (EOObjectNotAvailableException onaex) {
				progn = (Prognosis)EOUtilities.createAndInsertInstance(ec,"Prognosis");
				progn.takeValuesFromDictionary(dict);
			} catch (EOUtilities.MoreThanOneException mtoex) {
				Logger.getLogger("rujel.autoitog").log(WOLogLevel.WARNING,
						"Multiple prognoses found for dictionary",dict);
				continue;
			}
//			calculator.updatePrognosis(progn,agregatedMarks,agregatedWorks);
			double integral = getIntegral(agregatedMarks,agregatedWorks);
			long rounded = (long)(integral*10000);
			BigDecimal value = BigDecimal.valueOf(rounded,4);//new BigDecimal(integral,new MathContext(4));
			//if(progn.value() == null || progn.value().compareTo(value) != 0) {
				progn.setValue(value);
			//}
			value = getComplete(agregatedMarks,agregatedWorks);
			if(progn.complete() == null || progn.complete().compareTo(value) != 0) {
				progn.setComplete(value);
			}*/
			if(progn != null)
				result.setObjectForKey(progn,student);
		}
		return new PerPersonLink.Dictionary(result);
	}
	
	protected Prognosis prognosisForStudent(NSDictionary matchingValues,EOEditingContext ec,
			NSDictionary agregatedMarks, NSDictionary agregatedWorks) {
		Prognosis progn = null;
		try {
			progn = (Prognosis)EOUtilities.objectMatchingValues(ec,"Prognosis",matchingValues);
		} catch (EOObjectNotAvailableException onaex) {
			progn = (Prognosis)EOUtilities.createAndInsertInstance(ec,"Prognosis");
			progn.takeValuesFromDictionary(matchingValues);
		} catch (EOUtilities.MoreThanOneException mtoex) {
			Logger.getLogger("rujel.autoitog").log(WOLogLevel.WARNING,
					"Multiple prognoses found for dictionary",matchingValues);
			return null;
		}
		//calculator.updatePrognosis(progn,agregatedMarks,agregatedWorks);
		double integral = getIntegral(agregatedMarks,agregatedWorks);
		long rounded = (long)(integral*10000);
		BigDecimal value = BigDecimal.valueOf(rounded,4);//new BigDecimal(integral,new MathContext(4));
		//if(progn.value() == null || progn.value().compareTo(value) != 0) {
		progn.setValue(value);
		//}
		value = getComplete(agregatedMarks,agregatedWorks);
		if(progn.complete() == null || progn.complete().compareTo(value) != 0) {
			progn.setComplete(value);
		}
		return progn;
	}

	public Prognosis calculateForStudent(Student student, EduCourse course, EduPeriod period) {
		NSMutableDictionary dict = new NSMutableDictionary(period,"eduPeriod");
		dict.setObjectForKey(course,"eduCourse");
		dict.setObjectForKey(student,"student");
		EOEditingContext ec = course.editingContext();
		
		NSArray works = works(course, period);
		if(works.count() == 0) {
			return null;
		}
		NSDictionary agregatedWorks = agregateWorks(works);
		
		NSMutableArray quals = new NSMutableArray(Various.getEOInQualifier("work",works));//allWorks));
		quals.addObject(new EOKeyValueQualifier("student",EOQualifier.QualifierOperatorEqual,student));
		
		EOFetchSpecification fs = new EOFetchSpecification("Mark",new EOAndQualifier(quals),null);
		fs.setRefreshesRefetchedObjects(true);
		NSArray allMarks = ec.objectsWithFetchSpecification(fs);
		NSDictionary agregatedMarks = agregateMarks(allMarks);
		
		return prognosisForStudent(dict, ec, agregatedMarks, agregatedWorks);
	}
}
