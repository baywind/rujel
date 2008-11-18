// ItogsTab.java

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

package net.rujel.eduresults;

import net.rujel.interfaces.EduCourse;
import com.webobjects.foundation.*;

public class ItogsTab implements NSKeyValueCoding {
	protected NSDictionary dict;
	protected EduCourse curs;
	//protected NSArray _periods;
	
	public ItogsTab(EduCourse course, NSDictionary dictionary) {
		curs = course;
		dict = dictionary;
	}
	
	public void takeValueForKey(Object value,String key) {
		dict.takeValueForKey(value,key);
	}
	
	public Object valueForKey(String key) {
		if(key == null)return null;
		if(key.equals("list")) {
			return periods();
		}
		return dict.valueForKey(key);
	}
	
	public NSArray periods() {
//		if(_periods == null) {
			NSArray _periods = EduPeriod.periodsForCourse(curs);
			if(_periods == null || _periods.count() == 0) return NSArray.EmptyArray;
			for (int i = 0; i < _periods.count(); i++) {
				EduPeriod per = (EduPeriod)_periods.objectAtIndex(i);
				per.setCourse(curs);
//				per.setNumber(new Integer(i+1));
			}
//		}
		return _periods;
	}
}
