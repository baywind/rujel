// IntegralCalculator.java

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

import java.math.BigDecimal;
import java.util.Enumeration;

import com.webobjects.foundation.NSArray;

import net.rujel.criterial.Mark;
import net.rujel.criterial.Work;
import net.rujel.interfaces.*;

public class IntegralCalculator extends WorkCalculator {

	public Prognosis calculateForStudent(Student student, EduCourse course,
			AutoItog period, NSArray works) {
		double weightSum = 0;
		double integralSum = 0;
		int count = 0;
		int complete = 0;
		if (works != null && works.count() > 0) {
			Enumeration<Work> enu = works.objectEnumerator();
			while (enu.hasMoreElements()) {
				Work work = (Work) enu.nextElement();
				if (BigDecimal.ZERO.compareTo(work.weight()) == 0)
					continue;
				BigDecimal integral = work.integralForStudent(student);
				if (integral == null && !work.isCompulsory())
					continue;
				double weight = work.weight().doubleValue();
				weightSum += weight;
				if (integral != null) {
					integralSum += (weight * integral.doubleValue());
					Mark[] marks = work.forPersonLink(student);
					for (int i = 0; i < marks.length; i++) {
						count++;
						if (marks[i] != null)
							complete++;
					}
				} else {
					count += work.criterMask().count();
				}
			}
		}
		double integral = integralSum / weightSum;
		Prognosis progn = Prognosis.getPrognosis(student, course,
				period.itogContainer(), (count > 0));
		if(count == 0) {
			if(progn != null)
				progn.editingContext().deleteObject(progn);
			return null;
		}
//		long rounded = (long)(integral*10000);
		BigDecimal value = new BigDecimal(integral);
		value = value.setScale(4,BigDecimal.ROUND_HALF_UP);
		progn.setAutoItog(period);
		progn.setValue(value);
//		rounded = ((long)complete*10000)/count;
		value = new BigDecimal((double)complete/count); //BigDecimal.valueOf(rounded,4);
		value = value.setScale(4,BigDecimal.ROUND_HALF_UP);
		if(progn.complete() == null || progn.complete().compareTo(value) != 0) {
			progn.setComplete(value);
		}
		return progn;
	}

}
