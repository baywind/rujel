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
import net.rujel.interfaces.EduCourse;
import net.rujel.interfaces.PerPersonLink;
import net.rujel.interfaces.Student;
import net.rujel.reusables.Various;

import com.webobjects.foundation.*;
import com.webobjects.eocontrol.*;
//import com.webobjects.eoaccess.EOUtilities;

import java.math.*;
import java.util.Enumeration;

public class BachalaureatCalculator extends WorkCalculator {
	public static final BachalaureatCalculator sharedInstance = new BachalaureatCalculator();
	
//	protected Student _student;
	protected static final int WEIGHT = 0;
	protected static final int VALUE = 1;
	protected static final int MAX = 2;
	
	protected BigDecimal[] newAgregate() {
		return new BigDecimal[] {BigDecimal.ZERO,BigDecimal.ZERO,BigDecimal.ZERO};
	}
	
	public NSDictionary agregateMarks(NSArray allMarks) {
		if(allMarks == null || allMarks.count() == 0)
			return null;
		NSMutableDictionary dict = new NSMutableDictionary();
		NSMutableSet optWorks = new NSMutableSet();
		Enumeration en = allMarks.objectEnumerator();
		while (en.hasMoreElements()) {
			Mark mark = (Mark)en.nextElement();
			Work work = mark.work();
			BigDecimal weightValue = work.weight();
			Integer crit = mark.criterion();
			EOEnterpriseObject mask = work.getCriterMask(crit);
			Integer critWeight = (Integer)mask.valueForKeyPath("weight");
			if(weightValue == null || weightValue.compareTo(BigDecimal.ZERO) == 0)
				continue;
			if(critWeight != null && critWeight.intValue() == 0)
				continue;
			if(crit == null)
				continue;
			if(work.isOptional()) {
				optWorks.addObject(work);
			}
			BigDecimal[] agregator = (BigDecimal[])dict.objectForKey(crit);
			if(agregator == null) {
				agregator = newAgregate();
				dict.setObjectForKey(agregator,crit);
			}
			//double weight = weightValue.doubleValue();
			Number value = mark.value();
			Number max = (Number)mask.valueForKeyPath("max");
			if(value == null || max == null)
				continue;
			agregator[WEIGHT] = agregator[WEIGHT].add(weightValue);
			agregator[VALUE] = agregator[VALUE].add(
					weightValue.multiply(new BigDecimal(value.intValue())));
			agregator[MAX] = agregator[MAX].add(
					weightValue.multiply(new BigDecimal(max.intValue())));
		}
		if(optWorks.count() > 0) {
			NSDictionary optionalWorks = agregateWorks(optWorks.allObjects(), false);
			optionalWorks.takeValueForKey(optWorks, "list");
			dict.setObjectForKey(optionalWorks, "optionalWorks");
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
				if(work.isOptional())
					continue;
				if(weightValue == null || weightValue.compareTo(BigDecimal.ZERO) == 0)
					continue;
			}
			Enumeration masks = work.criterMask().objectEnumerator();
			while(masks.hasMoreElements()) { //criters
				EOEnterpriseObject currMask = (EOEnterpriseObject)masks.nextElement();
				Integer crit = (Integer)currMask.valueForKey("criterion");

				BigDecimal[] agregator = (BigDecimal[])dict.objectForKey(crit);
				if(agregator == null) {
					agregator = newAgregate();
					dict.setObjectForKey(agregator,crit);
				}
				//Number value = (Number)currMask.valueForKey("weight");
				Number max = (Number)currMask.valueForKey("max");
				if(max == null)
					continue;
				agregator[WEIGHT] = agregator[WEIGHT].add(weightValue);
				agregator[MAX] = agregator[MAX].add(weightValue.multiply(
						new BigDecimal(max.intValue())));
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
		if(agregatedMarks == null) return 0;
		NSDictionary optWorks = (NSDictionary)agregatedMarks.valueForKey("optionalWorks");
		if(agregatedWorks == null && optWorks == null) return 0;
		double valSum = 0;
		double maxSum = 0;
		Enumeration enu = agregatedMarks.keyEnumerator();
		while (enu.hasMoreElements()) {
			Object crit = enu.nextElement();
			BigDecimal[] wagr = (BigDecimal[])agregatedWorks.objectForKey(crit);
			BigDecimal[] optAgr = (optWorks == null)?null:(BigDecimal[])optWorks.objectForKey(crit);
			if(wagr == null
					|| BigDecimal.ZERO.compareTo(wagr[WEIGHT]) == 0
					|| BigDecimal.ZERO.compareTo(wagr[MAX]) == 0) {
				if(optAgr == null)
					continue;
				if(wagr == null)
					wagr = newAgregate();
				if(BigDecimal.ZERO.compareTo(optAgr[WEIGHT].add(wagr[WEIGHT])) == 0
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
		if(agregatedMarks == null) return BigDecimal.ZERO;
		NSDictionary optWorks = (NSDictionary)agregatedMarks.valueForKey("optionalWorks");
		if(agregatedWorks == null) return (optWorks == null)?null:BigDecimal.ONE;;
		BigDecimal sumMarks = null;
		BigDecimal sumWorks = null;
		boolean equals = true;
		Enumeration enu = agregatedWorks.keyEnumerator();
		while (enu.hasMoreElements()) {
			Object crit = enu.nextElement();
			BigDecimal[] wagr = (BigDecimal[])agregatedWorks.objectForKey(crit);
			BigDecimal val = (wagr==null)?null:wagr[WEIGHT];
			if(val != null) {
				sumWorks = (sumWorks == null)?val:sumWorks.add(val);
			}
			BigDecimal[] magr = (BigDecimal[])agregatedMarks.objectForKey(crit);
			val = (magr==null)?null:magr[WEIGHT];
			if(val != null) {
				sumMarks = (sumMarks == null)?val:sumMarks.add(val);
				if(optWorks != null) {
					wagr = (BigDecimal[])optWorks.objectForKey(crit);
					val = (wagr==null)?null:wagr[WEIGHT];
					if(val != null)
						sumMarks = sumMarks.subtract(val);
				}
			}
			if(equals && (sumMarks == null || sumWorks.compareTo(sumMarks) != 0)) {
				equals = false;
			}
		}
		if(sumWorks == null) return (optWorks == null)?null:BigDecimal.ONE;
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
	
	public PerPersonLink calculatePrognoses(EduCourse course, AutoItog period) {
			//return prognosesForCourseAndPeriod(course,period);
		//NSMutableDictionary dict = new NSMutableDictionary(period,"eduPeriod");
		//dict.setObjectForKey(course,"eduCourse");
		EOEditingContext ec = course.editingContext();
		
		NSArray works = period.relatedForCourse(course);//works(course, period);

		NSDictionary agregatedWorks = agregateWorks(works);
		boolean noWorks = (works == null || works.count() == 0);
		
		EOQualifier[] quals = new EOQualifier[2];
		if(!noWorks)
			quals[0] = Various.getEOInQualifier("work",works);
		
		EOFetchSpecification fs = new EOFetchSpecification("Mark",null,null);
		fs.setRefreshesRefetchedObjects(true);
		Enumeration enu = course.groupList().objectEnumerator();
		NSMutableDictionary result = new NSMutableDictionary();
		while (enu.hasMoreElements()) {
			Student student = (Student)enu.nextElement();
			Prognosis progn = Prognosis.getPrognosis(student, course, 
					period.itogContainer(), !noWorks);
			if(noWorks) {
				if(progn != null)
					ec.deleteObject(progn);
				continue;
			}
			
			quals[1] = new EOKeyValueQualifier("student",
					EOQualifier.QualifierOperatorEqual,student);
			fs.setQualifier(new EOAndQualifier(new NSArray(quals)));
			NSArray allMarks = ec.objectsWithFetchSpecification(fs);
			NSDictionary agregatedMarks = agregateMarks(allMarks);
			if(progn == null) {
				if(agregatedMarks == null || agregatedMarks.valueForKey("optionalWorks") == null)
					continue;
				else
					progn = Prognosis.getPrognosis(student, course, period.itogContainer(), true);
			}
			progn.setAutoItog(period);
			initPrognosis(progn, ec, agregatedMarks, agregatedWorks);
			result.setObjectForKey(progn,student);
		}
		return new PerPersonLink.Dictionary(result);
	}
	
	protected void initPrognosis(Prognosis progn,EOEditingContext ec,
			NSDictionary agregatedMarks, NSDictionary agregatedWorks) {
		/*Prognosis progn = null;
		try {
			progn = (Prognosis)EOUtilities.objectMatchingValues(ec,"Prognosis",matchingValues);
		} catch (EOObjectNotAvailableException onaex) {
			progn = (Prognosis)EOUtilities.createAndInsertInstance(ec,"Prognosis");
			progn.takeValuesFromDictionary(matchingValues);
		} catch (EOUtilities.MoreThanOneException mtoex) {
			Logger.getLogger("rujel.autoitog").log(WOLogLevel.WARNING,
					"Multiple prognoses found for dictionary",matchingValues);
			return null;
		}*/
		//calculator.updatePrognosis(progn,agregatedMarks,agregatedWorks);
		double integral = getIntegral(agregatedMarks,agregatedWorks);
//		long rounded = (long)(integral*10000);
		BigDecimal value = new BigDecimal(integral);
		value = value.setScale(4,BigDecimal.ROUND_HALF_UP);
		//if(progn.value() == null || progn.value().compareTo(value) != 0) {
		progn.setValue(value);
		//}
		value = getComplete(agregatedMarks,agregatedWorks);
		if(value == null)
			value = BigDecimal.ZERO;
		if(progn.complete() == null || progn.complete().compareTo(value) != 0) {
			progn.setComplete(value);
		}
		//return progn;
	}

	public Prognosis calculateForStudent(Student student, EduCourse course, 
			AutoItog period, NSArray works) {
		if(works == null || works.count() == 0) {
			return null;
		}
		EOEditingContext ec = course.editingContext();
		NSDictionary agregatedWorks = agregateWorks(works);
		boolean noWorks = (works == null || works.count() == 0);
		Prognosis progn = Prognosis.getPrognosis(student, course, period.itogContainer(), !noWorks);
		if(noWorks) {
			if(progn != null)
				ec.deleteObject(progn);
			return null;
		}
		EOQualifier[] quals = new EOQualifier[] { Various.getEOInQualifier("work",works),
				new EOKeyValueQualifier("student",EOQualifier.QualifierOperatorEqual,student) };
		EOFetchSpecification fs = new EOFetchSpecification("Mark",
				new EOAndQualifier(new NSArray(quals)),null);
		fs.setRefreshesRefetchedObjects(true);
		NSArray allMarks = ec.objectsWithFetchSpecification(fs);
		NSDictionary agregatedMarks = agregateMarks(allMarks);

		progn.setAutoItog(period);
		initPrognosis(progn, ec, agregatedMarks, agregatedWorks);
		return progn;
	}

}
