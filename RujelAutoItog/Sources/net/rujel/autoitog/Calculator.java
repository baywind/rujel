// Calculator.java

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

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Enumeration;
import java.util.logging.Logger;

import net.rujel.interfaces.*;
import net.rujel.reusables.Various;
import net.rujel.reusables.WOLogLevel;

import com.webobjects.eocontrol.*;
import com.webobjects.foundation.*;

public abstract class Calculator {

	public static Calculator calculatorForName(String calcName) {
		if(calcName == null || calcName.length() == 0 || calcName.equalsIgnoreCase("none"))
			return null;
		try {
			Class calculatorClass = Class.forName(calcName);
			Field[] cf = calculatorClass.getFields();
			if(cf.length > 0) {
				for (int i = 0; i < cf.length; i++) {
					if(cf[i].getName().equals("sharedInstance"))
						return (Calculator)cf[i].get(null);
				}
			}
			Constructor<Calculator> calConstr = calculatorClass.getConstructor((Class[])null);
			return calConstr.newInstance((Object[])null);
		} catch (Exception e) {
			Logger.getLogger("rujel.autoitog").log(WOLogLevel.WARNING,
						"Could not get Claculator for name calcName",e);
			return null;
		}
		//return BachalaureatCalculator.sharedInstance;
	}
	
	protected static NSArray marksForStudentAndWorks(Student student, NSArray works) {
		NSMutableArray quals = new NSMutableArray(Various.getEOInQualifier("work",works));
		quals.addObject(new EOKeyValueQualifier("student",EOQualifier.QualifierOperatorEqual,student));
		
		EOFetchSpecification fs = new EOFetchSpecification("Mark",new EOAndQualifier(quals),null);
		fs.setRefreshesRefetchedObjects(true);
		return student.editingContext().objectsWithFetchSpecification(fs);
	}

	public abstract Prognosis calculateForStudent(Student student, EduCourse course, AutoItog itog);

	public PerPersonLink calculatePrognoses(EduCourse course, AutoItog itog) {
		Enumeration enu = course.groupList().objectEnumerator();
		NSMutableDictionary result = new NSMutableDictionary();
		while (enu.hasMoreElements()) {
			Student student = (Student)enu.nextElement();
			Prognosis progn = calculateForStudent(student, course, itog);
			if(progn != null)
				result.setObjectForKey(progn,student);
		}
		return new PerPersonLink.Dictionary(result);
	}
	
	protected static final NSArray reliesOn = new NSArray(new String[] {"Work","Mark"});
	public NSArray reliesOn() {
		return reliesOn;
	}
	
	public String reliesOnEntity() {
		return "Work";
	}
}
