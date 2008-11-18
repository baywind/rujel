// PrognosPresenter.java: Class file for WO Component 'PrognosPresenter'

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

//import net.rujel.interfaces.*;
import net.rujel.reusables.NamedFlags;
import net.rujel.ui.AddOnPresenter;

import com.webobjects.appserver.*;

public class PrognosPresenter extends AddOnPresenter {

    public PrognosPresenter(WOContext context) {
        super(context);
    }
    /*
	public void reset() {
		super.reset();
	} */

	public void appendToResponse(WOResponse aResponse, WOContext aContext) {
    	currAddOn().setCourse(course());
    	currAddOn().setStudent(student());
    	super.appendToResponse(aResponse, aContext);
    }
	
	public WOActionResults invokeAction(WORequest aRequest, WOContext aContext) {
    	currAddOn().setCourse(course());
    	currAddOn().setStudent(student());
    	return super.invokeAction(aRequest, aContext);
	}
	public void takeValuesFromRequest(WORequest aRequest, WOContext aContext) {
    	currAddOn().setCourse(course());
    	currAddOn().setStudent(student());
    	super.takeValuesFromRequest(aRequest, aContext);
	}
	
	public PrognosesAddOn currAddOn() {
		return (PrognosesAddOn) super.currAddOn();
	}
	
	public String style() {
		String result = "border-style:none double;border-width:0px 3px;";
		Prognosis prognosis = currAddOn().prognosis();
		if(prognosis == null)
			return result;
		if(!prognosis.isComplete())
			result = result + "text-decoration:underline;";
		if(prognosis.namedFlags().flagForKey("keep"))
			result = result + "font-weight:bold;";
		return result;
	}

	public String styleClass() {
		Prognosis prognosis = currAddOn().prognosis();
		if(prognosis != null && prognosis.namedFlags().flagForKey("disable"))
			return "grey";
		if(currAddOn().timeout() != null) {
			if(currAddOn().timeout().namedFlags().flagForKey("negative"))
				return "highlight2";
			else
				return "highlight";
		}
		if(!isActive()) return null;
		Boolean sex = (Boolean)valueForKeyPath("student.person.sex");
		if(sex == null || (currAddOn().prognosis() == null && currAddOn().inTimeout)) return "grey";
		if (sex.booleanValue())
			return "male";
		else
			return "female";
	}

	public NamedFlags access() {
		if(currAddOn().inTimeout)
			return currAddOn().accessTimeout();
		else
			return currAddOn().access();
	}
	
	protected boolean isActive() {
		if(currAddOn().inTimeout) {
			String flag = (currAddOn().timeout()==null)?"create":"read";
			if(student() != null)
				return (currAddOn().prognosis() != null && currAddOn().accessTimeout().flagForKey(flag));
			else
				return currAddOn().accessCourseTimeout().flagForKey(flag);
		} else {
			String flag = (currAddOn().prognosis()==null)?"create":"read";
			return currAddOn().access().flagForKey(flag);
		}
	}
	
	public WOActionResults editPrognosis() {
		Prognosis prognosis = currAddOn().prognosis();
		if(prognosis == null && !access().flagForKey("create")) {
			String message = (String)valueForKeyPath("application.strings.messages.noAccess");
			return messagePopup(message);
		}
		WOComponent popup = pageWithName("PrognosisPopup");
		popup.takeValueForKey(prognosis,"prognosis");
		popup.takeValueForKey(currAddOn(),"addOn");
		popup.takeValueForKey(course(),"course");
		popup.takeValueForKey(student(),"student");
		popup.takeValueForKey(currAddOn().periodItem,"eduPerod");
		popup.takeValueForKey(context().page(),"returnPage");
		return popup;
	}

	public WOActionResults editTimeout() {
		if(currAddOn().timeout() == null && !access().flagForKey("create")) {
			String message = (String)valueForKeyPath("application.strings.messages.noAccess");
			return messagePopup(message);
		}
		WOComponent popup = pageWithName("TimeoutPopup");
		popup.takeValueForKey(currAddOn(),"addOn");
		popup.takeValueForKey(course(),"course");
		popup.takeValueForKey(currAddOn().periodItem,"eduPeriod");
		//popup.takeValueForKey(student(),"student");
		if(student() != null)
			popup.takeValueForKey(currAddOn().prognosis(),"prognosis");
		popup.takeValueForKey(currAddOn().timeout(),"timeout");
		popup.takeValueForKey(context().page(),"returnPage");
		return popup;
	}

}