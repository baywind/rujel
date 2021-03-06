// NotesPage.java: Class file for WO Component 'NotesPage'

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

import net.rujel.interfaces.*;
import net.rujel.reusables.*;

import com.webobjects.foundation.*;
import com.webobjects.appserver.*;
import com.webobjects.eocontrol.*;

import java.lang.ref.WeakReference;
import java.util.Enumeration;

public class NotesPage extends WOComponent {
    public PerPersonLink lessonItem;
    public Student studentItem;

    public NSMutableDictionary presenterCache = new NSMutableDictionary();
	
    public NotesPage(WOContext context) {
        super(context);
    }
	
	protected Boolean _single;
	public Boolean single() {
		if(_single == null) {
			Object tmp = valueForBinding("single");
			if(tmp != null && tmp instanceof Boolean) {
				_single = (Boolean)tmp;
			} else {
				_single = new Boolean(Various.boolForObject(tmp));
			}
		}
		return _single;
	}
	/*
	public void setSingle(Object val) {
		single = Various.boolForObject(val);
	}*/
						  
	public NSArray lessonsListing() {
		if(valueForBinding("selectStudent") instanceof Student)
			return null;
		if(single()) {
			if(currLesson() != null)
				return new NSArray(currLesson());
			else
				return null;
		} 
		return (NSArray)valueForBinding("lessonsList");
		//return lessonsList;
	}
	
	protected PerPersonLink _currLesson;
	public PerPersonLink currLesson() {
		if(_currLesson == null) {
			_currLesson = (PerPersonLink)valueForBinding("currLesson");
		}
		return _currLesson;
	}

	protected NamedFlags _access;
	public NamedFlags access() {
		if(_access == null) {
			NSKeyValueCoding present = (NSKeyValueCoding)valueForBinding("present");
			String checkAccess = (String)present.valueForKey("checkAccess");
			if(checkAccess == null)
				checkAccess = (String)present.valueForKey("entityName");
			_access = (NamedFlags)session().valueForKeyPath("readAccess.FLAGS." + checkAccess);
			if(_access == null)
				_access = DegenerateFlags.ALL_TRUE;
		}	
		return _access;
	}
	
	protected String _currPresenter;
	public String presenter() {
		if(_currPresenter == null) {
			NSKeyValueCoding present = (NSKeyValueCoding)valueForBinding("present");
			if(present == null) {
				_currPresenter = "NotePresenter";
				return "NotePresenter";
			}
			_currPresenter = (String)present.valueForKey("presenter");
			if(currLesson() != null) {
				String entName = (String)present.valueForKey("entityName");
				String currEnt = null;
				if(currLesson() instanceof EOEnterpriseObject) {
					currEnt = ((EOEnterpriseObject)currLesson()).entityName();
				} else {
					currEnt = currLesson().getClass().getName();
					currEnt = currEnt.substring(currEnt.lastIndexOf('.') + 1);
				}
				if(!currEnt.equals(entName)) {
					entName = (String)present.valueForKey(currEnt);
					if(entName == null)
						entName = (String)present.valueForKey("tmpPresenter");
					if(entName != null)
						_currPresenter = entName;
				}
			}
			if(_currPresenter == null)
				_currPresenter = "NotePresenter";
		}
		return _currPresenter;
	}

	public String studentStyle() {
		Object selectStudent = valueForBinding("selectStudent");
		if(studentItem == null) {
			if(selectStudent != null && selectStudent == lessonItem && !single())
				return "selection";
			else
				return "grey";
		}
		if(selectStudent != null && selectStudent.equals(studentItem))
			return "selection";
		Boolean sex = (Boolean)valueForKeyPath("studentItem.person.sex");
		if(sex == null) return "grey";
		if (sex.booleanValue())
			return "male";
		else
			return "female";
	}

	public boolean cantSelectStudent() {
		return (!hasBinding("selectStudentAction"));
	}
	
	public String studentNotes() {
		if(studentItem == null || currLesson() == null ||
				!(valueForBinding("selectStudent") instanceof Student))
			return null;
		StringBuilder buf = new StringBuilder(
				"<span class=\"fsc\">");
		Object forStudent = currLesson().forPersonLink(studentItem);
		boolean none = (forStudent == null || forStudent.equals(NullValue));
//		if(none)
//			buf.append("color:#cc3333;\">");
//		else
//			buf.append("color:#999999;\">");
		if(none)
			buf.append('0');
		else if(forStudent instanceof Object[]) {
			Object[] arr = (Object[])forStudent;
			int count = 0;
			for (int i = 0; i < arr.length; i++) {
				if(arr[i] != null && !arr[i].equals(NullValue))
					count++;
			}
			buf.append(count);
		} else if(forStudent instanceof NSArray)
			buf.append(((NSArray) forStudent).count());
		else
			buf.append('1');
		buf.append("</span>");
		return buf.toString();
	}
    
	public NSArray unmentionedStudents() {
		NSArray lessonsList = (NSArray)valueForBinding("lessonsList");
		NSArray studentsList = (NSArray)valueForBinding("studentsList");

		if(lessonsList == null || lessonsList.count() == 0 || 
				!(lessonsList.objectAtIndex(0) instanceof EduLesson))
			return null;
		NSMutableSet unmentionedSet = new NSMutableSet();
		NSSet mentioned = new NSSet(studentsList);
		Enumeration lessEnum = lessonsList.objectEnumerator();
		EduLesson les;
		//Enumeration noteEnum;
		//EOEnterpriseObject note;
		//Object stu;
		while (lessEnum.hasMoreElements()) {
			les = (EduLesson)lessEnum.nextElement();
			unmentionedSet.addObjectsFromArray(les.students());
			/*noteEnum = les.notes().objectEnumerator();
			while (noteEnum.hasMoreElements()) {
				note = (EOEnterpriseObject)noteEnum.nextElement();
				stu = note.storedValueForKey("student");
				if(!studentsList.containsObject(stu))
					unmentionedSet.addObject(stu);
			}*/
		}
		unmentionedSet.subtractSet(mentioned);
		if(unmentionedSet.count() < 1)
			return null;
		NSArray tmp = unmentionedSet.allObjects();
		if(tmp.count() == 1)
			return tmp;
		
		return EOSortOrdering.sortedArrayUsingKeyOrderArray(tmp,Person.sorter);
	}
	
//	public Object selectStudent;
	
    public Integer idx;

    /** @TypeInfo com.webobjects.foundation.NSKeyValueCoding */
    public NSArray allAddOns;
    public NSKeyValueCoding addOnItem;
    /** @TypeInfo java.lang.String */
    public NSMutableArray activeAddOns;
//    public String activeAddOnItem;
	
	public String number() {
		if(idx == null)return null;
		return String.valueOf(idx.intValue() + 1);
	}
	
	public WOActionResults selectLesson() {
		_currLesson = lessonItem;
		if(lessonItem instanceof EOEnterpriseObject) {
			EOEditingContext ec = ((EOEnterpriseObject)lessonItem).editingContext();
			if(ec == null) {
				_currLesson = null;
			} else {
				if (ec.hasChanges())
					ec.revert();
			}
		}
		setValueForBinding(_currLesson,"currLesson");
		setValueForBinding(_currLesson,"selectStudent");
		context().setUserInfoForKey(studentItem, "activeStudent");
/*		//selectStudent = studentItem;
		if(hasBinding("selectStudent")) {
			if(studentItem == null)
				setValueForBinding(_currLesson,"selectStudent");
			else
				setValueForBinding(studentItem,"selectStudent");
		}*/
		return context().page();
    }
	
	public WOActionResults studentSelection() {
		//selectStudent = studentItem;
		if(hasBinding("selectStudent")) {
			if(studentItem != valueForBinding("selectStudent"))
				setValueForBinding(studentItem,"selectStudent");
			else
				setValueForBinding(currLesson(),"selectStudent");
		}
		EOEditingContext ec = studentItem.editingContext();
		if (ec.hasChanges()) ec.revert();
		return (WOActionResults)valueForBinding("selectStudentAction");
		//performParentAction(selectStudentAction);//
	}
	
	public String cellID () {
		Object selectStudent = context().userInfoForKey("activeStudent");
		// valueForBinding("selectStudent");
		if(selectStudent == null || studentItem == null || !selectStudent.equals(studentItem))
			return null;
		else
			return "focus";
	}
	
	public void reset() {
		_currLesson = null;
		_currPresenter = null;
		lessonItem = null;
		studentItem = null;
		_single = null;
		addOnItem = null;
		allAddOns = null;
		activeAddOns = null;
		_access = null;
		if(presenterCache == null)
			presenterCache = new NSMutableDictionary();
		else
			presenterCache.removeAllObjects();
	}
	
	public boolean isStateless() {
		return true;
	}
	
	public boolean synchronizesVariablesWithBindings() {
		return false;
	}
	
	public boolean canSave() {
		return (currLesson() != null && access().flagForKey("edit"));
	}
	
    public WOActionResults save() {
        WOActionResults result = (WOActionResults)parent().valueForKey("save");
		_currLesson = (EduLesson)valueForBinding("currLesson");
		return result; 
    }
	
	public boolean isSelected() {
		return (currLesson() != null && lessonItem == currLesson());
	}
	
	public void setStudentItem(Object item) {
		if(item instanceof Student) {
			studentItem = (Student)item;
		} else {
			studentItem = null;
			
		}
	}
	
	public NSArray allAddOns() {
		if((single() && currLesson() != null) || session()==null) return null;
		
		if(allAddOns != null)
			return allAddOns;
		//allAddOns = (NSArray)valueForBinding("allAddOns");
		allAddOns = (NSArray)session().objectForKey("notesAddOns");
		if(allAddOns == null) {
			allAddOns = (NSArray)session().valueForKeyPath("modules.notesAddOns");
			if(allAddOns == null)
				allAddOns = NSArray.EmptyArray;
			session().setObjectForKey(allAddOns,"notesAddOns");
			//setValueForBinding(allAddOns,"allAddOns");
		}
		WeakReference courseRef = new WeakReference(valueForBinding("course"));
		allAddOns.takeValueForKey(courseRef, "course");
		return allAddOns;
	}

	protected static final EOQualifier inSingle = new EOKeyValueQualifier("inSingle", 
			EOQualifier.QualifierOperatorEqual, Boolean.TRUE);
	public NSMutableArray activeAddOns() {
		if(activeAddOns != null)
			return activeAddOns;
		//activeAddOns = (NSMutableArray)valueForBinding("activeAddOns");
		activeAddOns = (NSMutableArray)session().objectForKey("activeAddOns");
		if(activeAddOns == null) {
			activeAddOns = new NSMutableArray();
			if(allAddOns() != null && allAddOns.count() > 0) {
				Enumeration en  = allAddOns.objectEnumerator();
				while (en.hasMoreElements()) {
					NSKeyValueCoding curr = (NSKeyValueCoding)en.nextElement();
					if(Various.boolForObject(curr.valueForKey("defaultOn"))) {
						activeAddOns.addObject(curr);
					}
				}
			}
			if(activeAddOns != null) {
				session().setObjectForKey(activeAddOns, "activeAddOns");
			}
		}
		if(activeAddOns != null && activeAddOns.count() > 0) {
			WeakReference courseRef = new WeakReference(valueForBinding("course"));
			activeAddOns.takeValueForKey(courseRef, "course");
			if(single() && currLesson() != null)
				EOQualifier.filterArrayWithQualifier(activeAddOns, inSingle);
		}
		return activeAddOns;
	}
	
	public static void resetAddons(WOSession ses) {
		NSMutableArray addOns = (NSMutableArray)ses.objectForKey("notesAddOns");
		if(addOns != null && addOns.count() > 0)
			addOns.valueForKey("reset");
		ses.removeObjectForKey("lessonProperties");
	}

	/*
	public boolean single() {
		return (lessonsList != null && lessonsList.count() == 1);
	}*/
}
