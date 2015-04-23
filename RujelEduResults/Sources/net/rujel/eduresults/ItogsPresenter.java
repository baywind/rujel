// ItogsPresenter.java: Class file for WO Component 'ItogsPresenter'

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

import net.rujel.reusables.*;
import net.rujel.ui.AddOnPresenter;

import com.webobjects.foundation.*;
import com.webobjects.appserver.*;
import com.webobjects.eocontrol.*;
import java.util.Enumeration;
import java.util.logging.Logger;

public class ItogsPresenter extends AddOnPresenter {

	private NSArray _periods;
	private ItogMark[] _arr;
	
	public ItogContainer periodItem;
	
    public ItogsPresenter(WOContext context) {
        super(context);
    }
	
	public void reset() {
		super.reset();
		_periods = null;
		_arr = null;
		_pertype = null;
		_styleClass = null;
	}
	
	public NSArray periods() {
		if(_periods == null) {
			if(currAddOn().course() == course()) {
				_periods = (NSArray)currAddOn().valueForKey("periods");
			}
			if(_periods == null) {
				currAddOn().setCourse(course());
				_periods = ItogContainer.itogsForCourse(course());
				if(_periods == null)
					_periods = NSArray.EmptyArray;
				currAddOn().takeValueForKey(_periods,"periods");
				currAddOn().agregate = null;
			}
		}
		return _periods;
	}
	
	public NSDictionary itogs() {
		if(currAddOn().agregate == null) {
				NSArray periods = periods();
				if(periods == null || periods.count() == 0)
					return NSDictionary.EmptyDictionary;
				EOQualifier qual = Various.getEOInQualifier(ItogMark.CONTAINER_KEY,periods);
				NSMutableArray quals = new NSMutableArray(qual);
				qual = new EOKeyValueQualifier("cycle",EOQualifier.QualifierOperatorEqual,course().cycle());
				quals.addObject(qual);
				qual = new EOAndQualifier(quals);
				EOFetchSpecification fspec = new EOFetchSpecification("ItogMark",qual,null);
				NSArray allItogs = course().editingContext().objectsWithFetchSpecification(fspec);

				if(allItogs != null && allItogs.count() > 0) {
					NSMutableDictionary agregate = new NSMutableDictionary();
					Enumeration enu = allItogs.objectEnumerator();
					while (enu.hasMoreElements()) {
						ItogMark curr = (ItogMark)enu.nextElement();
						ItogMark[] arr = (ItogMark[])agregate.objectForKey(curr.student());
						if(arr == null) {
							arr = new ItogMark[periods.count()];
							agregate.setObjectForKey(arr,curr.student());
						}
						int idx = periods.indexOfIdenticalObject(curr.container());
						if(idx < 0) {
							NSMutableDictionary args = new NSMutableDictionary(session(),"session");
							args.takeValueForKey(curr, "mark");
							args.takeValueForKey(course(), "course");
							args.takeValueForKey(curr.container(), "period");
							//Object[] args = new Object[] {session(),curr,course(),curr.eduPeriod()};
							Logger.getLogger("rujel.eduresults").log(WOLogLevel.WARNING,
									"Found ItogMark for wrong period",args);
							continue;
						}
						arr[idx] = curr;
					}
					currAddOn().agregate = agregate;
				} else {
					currAddOn().agregate = new NSMutableDictionary();
				}
		}
		return currAddOn().agregate;
	}
	
	public ItogMark itog() {
		if(student() == null)
			return null;
		if(_arr == null)
			_arr = (ItogMark[])itogs().objectForKey(student());
		if(_arr == null) return null;
		return _arr[periods().indexOfIdenticalObject(periodItem)];
	}
	
	public String star() {
		EOEnterpriseObject commentEO = ItogMark.getItogComment(
				course().cycle(), periodItem, student(), false);
		if(commentEO == null)
			return null;
		NSDictionary dict = ItogMark.commentsDict(commentEO);
		if(dict.valueForKey(ItogMark.MANUAL) == null)
			return null;
		return "<sup style=\"font-size:smaller;\">*</sup>";
	}
	
    public WOComponent moreInfo() {
		course().editingContext().revert();
        WOComponent nextPage = pageWithName("ItogPopup");
		nextPage.takeValueForKey(currAddOn(),"addOn");
		nextPage.takeValueForKey(periodItem,"itogContainer");
		nextPage.takeValueForKey(student(),"student");
		nextPage.takeValueForKey(itog(),"itog");
//		nextPage.takeValueForKey(course(),"course");
		nextPage.takeValueForKey(context().page(),"returnPage");
        return nextPage;
    }

	protected ItogType _pertype;
    protected String _styleClass;
	public String styleClass() {
		if(_pertype != periodItem.itogType()) {
			if(_styleClass == null || _styleClass.startsWith("un"))
				_styleClass = "gerade";
			else
				_styleClass = "ungerade";
			_pertype = periodItem.itogType();
		}
		if(Various.boolForObject(valueForKeyPath("itog.readFlags.flagged")))
			return "orange";
		return _styleClass;
    }
}
