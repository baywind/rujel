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


import java.util.Enumeration;

import net.rujel.interfaces.EduCourse;
import net.rujel.interfaces.Person;
import net.rujel.interfaces.Student;
import net.rujel.reusables.Various;

import com.webobjects.appserver.WOActionResults;
import com.webobjects.appserver.WOContext;
import com.webobjects.appserver.WOComponent;
import com.webobjects.appserver.WORequest;
import com.webobjects.foundation.NSMutableArray;
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
			return (!currAddOn().access().flagForKey("create"));
		} else {
			boolean closed = Various.boolForObject(dict().valueForKey("closed"));
			if(closed) {
				return !currAddOn().access().flagForKey("edit");
			} else {
				return true;//!access.flagForKey("create");
			}
		}
	}
	
	public WOActionResults stamp() {
		WOComponent alert = pageWithName("MyAlert");
		alert.takeValueForKey(context().page(), "returnPage");
		NSMutableDictionary action = new NSMutableDictionary("closeDict","setKey");
		NSMutableDictionary cd = new NSMutableDictionary(course(),"course");
		cd.takeValueForKey(session().valueForKeyPath("user.present"), "user");
		action.takeValueForKey(cd, "setValue");
		action.takeValueForKey(currAddOn(), "object");
		if(student() != null) { // release student
			StringBuilder buf = new StringBuilder((String)session().valueForKeyPath(
					"strings.RujelComplete_Complete.messages.release"));
			buf.append(":<br/>\n").append(Person.Utility.fullName(student(), true, 2, 2, 0));
			cd.setObjectForKey(student(),"releaseStudent");
			action.takeValueForKey(session().valueForKeyPath(
					"strings.RujelComplete_Complete.release"), "title");
			alert.takeValueForKey(action, "addAction");
			alert.takeValueForKey(buf.toString(), "message");
			return alert;
		}
		StringBuilder buf = currAddOn().checkRequirements(course());
		if(buf != null) {
			buf.insert(0, ":\n");
			buf.insert(0,session().valueForKeyPath(
				"strings.RujelComplete_Complete.messages.preceding"));
			alert.takeValueForKey(buf.toString(), "message");
			return alert;
		}
		WORequest req = context().request();
//		NSArray keys = req.formValueKeys();
//		if(keys != null && keys.count() > 0) {
			buf = new StringBuilder((String)session().valueForKeyPath(
				"strings.RujelComplete_Complete.messages.closing1"));
			buf.append("<ul>");
			Enumeration enu = course().groupList().objectEnumerator();//keys.objectEnumerator();
//			EOEditingContext ec = course().editingContext();
			NSMutableArray toClose = new NSMutableArray();
			boolean all = true;
			while (enu.hasMoreElements()) {
				Student student = (Student)enu.nextElement();
				NSMutableDictionary dic = currAddOn().dictForStudent(student);
				if(Various.boolForObject(dic.valueForKey("complete"))) {
					all = false;
					continue;
				}
				String key = (String)dic.valueForKey("id");
//				if(!key.startsWith("cpt"))
//					continue;
				boolean checked = false;
				key = req.stringFormValueForKey(key);
				if(key != null)
					checked = Boolean.valueOf(key);
				else
					checked = Various.boolForObject(dic.valueForKey("checked"));
				if(!checked) {
					all = false;
					continue;
				}
//				Integer stID = Integer.valueOf(key.substring(3));
//				Student student = (Student)EOUtilities.objectWithPrimaryKeyValue(ec,
//						Student.entityName, stID);
				buf.append("<li>");
				buf.append(Person.Utility.fullName(student, true, 2, 2, 0));
				buf.append("</li>\n");
				toClose.addObject(student);
			}
			if(toClose.count() > 0) {
				if(all) {
					cd.takeValueForKey(Boolean.TRUE, "closeAll");
					alert.takeValueForKey(session().valueForKeyPath(
					"strings.RujelComplete_Complete.messages.closingAll"), "message");
				} else {
					cd.takeValueForKey(toClose, "toClose");
					buf.append("</ul>").append(session().valueForKeyPath(
						"strings.RujelComplete_Complete.messages.closing2"));
					alert.takeValueForKey(buf.toString(), "message");
				}
				action.takeValueForKey(session().valueForKeyPath(
						"strings.RujelComplete_Complete.closeTitle"), "title");
				alert.takeValueForKey(action, "addAction");
			} else {
				alert.takeValueForKey(session().valueForKeyPath(
						"strings.RujelComplete_Complete.messages.noneSelected"), "message");
			}
		/*} else {
			alert.takeValueForKey(session().valueForKeyPath(
				"strings.RujelComplete_Complete.messages.noneSelected"), "message");
		}*/
		return alert;
	}
	
	public String onClick() {
		if(student() != null)
			return (String)session().valueForKey("ajaxPopup");
		StringBuilder buf = new StringBuilder(
				"var m = '';for(var a in returnField){m = m.concat(a,'=',returnField[a],'&');}");
		buf.append("getAjaxPopup(event,'");
		buf.append(context().componentActionURL()).append("',m);");
		return buf.toString();
	}
}