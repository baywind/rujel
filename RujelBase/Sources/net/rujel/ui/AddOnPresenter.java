// AddOnPresenter.java

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

package net.rujel.ui;

import java.lang.ref.WeakReference;

import net.rujel.reusables.*;
import net.rujel.interfaces.*;

import com.webobjects.foundation.*;
import com.webobjects.appserver.*;

public class AddOnPresenter extends WOComponent {

    public AddOnPresenter(WOContext context) {
        super(context);
    }
	
    public static class AddOn implements NSKeyValueCoding,NSKeyValueCoding.ErrorHandling {
    	protected EduCourse _course;
    	protected NSDictionary dict;
    	protected NamedFlags _access;
    	public NSMutableDictionary agregate;
    	private NSMutableDictionary userInfo;
    	
    	public AddOn(NSDictionary init, NamedFlags access) {
    		dict = init;
    		_access = access;
    	}

    	public EduCourse course() {
    		return _course;
    	}

    	public void setCourse(Object aCourse) {
    		EduCourse crs = null;
    		if(aCourse instanceof EduCourse) {
    			crs = (EduCourse)aCourse;
    		} else if (aCourse instanceof WeakReference) {
    			crs = (EduCourse)((WeakReference)aCourse).get();
    		}
    		if(crs == null) {
    			reset();
    		} else {
    			update(crs);
    		}
    	}
    	
    	public void update(EduCourse crs) {
    		if(crs == _course)
    			return;
    		reset();
    		_course = crs;
    	}
    	
    	public void reset() {
    		_course = null;
    		agregate = null;
    		userInfo = null;
    	}

    	public NamedFlags access() {
    		return _access;
    	}
    	
    	public void takeValueForKey(Object value, String key) {
    		NSKeyValueCoding.DefaultImplementation.takeValueForKey(this, value, key);
    	}

    	public Object valueForKey(String key) {
    		Object result = dict.valueForKey(key);
    		if(result != null)
    			return result;
    		result = NSKeyValueCoding.DefaultImplementation.valueForKey(this, key);
    		return result;
    	}

    	public Object handleQueryWithUnboundKey(String key) {
    		if(userInfo == null)
    			return null;
    		return userInfo.valueForKey(key);
    	}

    	public void handleTakeValueForUnboundKey(Object value, String key) {
    		if(userInfo == null) {
    			if(value != null)
    				userInfo = new NSMutableDictionary(value,key);
    		} else
    			userInfo.takeValueForKey(value, key);
    	}

    	public void unableToSetNullForKey(String key) {
    		NSKeyValueCoding.DefaultImplementation.unableToSetNullForKey(this, key);
    		//throw new NullPointerException("unableToSetNullForKey : " + key);
    	}
   }
	
	protected AddOn _currAddOn;
	private EduCourse _course;
//	private EduPeriod _period;
	private Object _student;
	
	public AddOn currAddOn() {
		if(_currAddOn == null) {
			_currAddOn = (AddOn)valueForBinding("currAddOn");
		}
		return _currAddOn;
	}
	
	public NamedFlags access() {
		return (NamedFlags)currAddOn().valueForKey("access");
	}
	
	public EduCourse course() {
		if(_course == null) {
			_course = (EduCourse)valueForBinding("course");
		}
		return _course;
	}
	/*
	public EduPeriod eduPeriod() {
		if(_period == null) {
			_period = (EduPeriod)currAddOn().valueForKey("eduPeriod");
			if(_period == null) {
				NSArray pertypes = PeriodType.periodTypesForCourse(course());
				if(pertypes == null) return null;
				PeriodType pertype = (PeriodType)pertypes.objectAtIndex(0);
				_period = pertype.currentPeriod();
			}
		}
		return _period;
	}*/
	
	public Student student() {
		if(_student == null) {
			_student = (Student)valueForBinding("student");
			if(_student == null)
				_student = NullValue;
		}
		return (_student==NullValue)?null:(Student)_student;
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
		//		_period = null;
		_student = null;
		_noAccess = null;
	}
	
	public WOActionResults messagePopup(String message) {
		WOComponent alert = pageWithName("MyAlert");
		alert.takeValueForKey(context().page(), "returnPage");
		alert.takeValueForKey(message, "message");
		return alert;
/*		WOResponse response = WOApplication.application().createResponseInContext(context());
		response.appendContentString("<div class=\"attention\" style=\"cursor:pointer;\" onclick=\"this.style.display='none';\">");
		response.appendContentString(message);
		response.appendContentString("</div>");
		return response;*/
	}
	
	protected Boolean _noAccess;
	public Boolean noAccess() {
		if(_noAccess == null) {
			NamedFlags access = access();
			if(access == null || access.getFlag(0))
				_noAccess = Boolean.FALSE;
			else
				_noAccess = Boolean.TRUE;
		}
		return _noAccess;
	}	
}
