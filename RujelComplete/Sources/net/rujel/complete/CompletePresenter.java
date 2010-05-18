// CompletePresenter.java: Class file for WO Component 'CompletePresenter'

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

package net.rujel.complete;


import net.rujel.interfaces.EduCourse;
import net.rujel.interfaces.Student;
import net.rujel.reusables.Various;

import com.webobjects.appserver.WOContext;
import com.webobjects.appserver.WOComponent;
import com.webobjects.foundation.NSMutableDictionary;

public class CompletePresenter extends WOComponent {
	protected CptAddOn _currAddOn;
	protected NSMutableDictionary _dict;
	private EduCourse _course;
	private Object _student;

	public CompletePresenter(WOContext context) {
        super(context);
    }
	public boolean synchronizesVariablesWithBindings() {
        return false;
	}
	
	public boolean isStateless() {
		return true;
	}
	
	public void reset() {
		super.reset();
		_currAddOn = null;
		_course = null;
		_student = null;
		_dict = null;
	}
	
	public CptAddOn currAddOn() {
		if(_currAddOn == null) {
			_currAddOn = (CptAddOn)valueForBinding("currAddOn");
			_currAddOn.setCourse(course());
		}
		return _currAddOn;
	}
	
	public NSMutableDictionary dict() {
		if(student() == null)
			return null;
		if(_dict == null) {
			_dict = currAddOn().dictForStudent(student());
		}
		return _dict;
	}

	public EduCourse course() {
		if(_course == null) {
			_course = (EduCourse)valueForBinding("course");
		}
		return _course;
	}
	
	public Student student() {
		if(_student == null) {
			_student = (Student)valueForBinding("student");
			if(_student == null)
				_student = NullValue;
		}
		return (_student==NullValue)?null:(Student)_student;
	}
	
	public boolean disabled() {
		if(student() == null) {
			return (currAddOn().access().intValue() <=1);
		} else {
			boolean closed = Various.boolForObject(dict().valueForKey("closed"));
			if(closed) {
				return !currAddOn().access().flagForKey("edit");
			} else {
				return true;//!access.flagForKey("create");
			}
		}
	}
}