// LessonList.java: Class file for WO Component 'LessonList'

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

import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.logging.Logger;

import net.rujel.base.BaseLesson;
import net.rujel.base.MyUtility;
import net.rujel.interfaces.*;
import net.rujel.reusables.NamedFlags;
import net.rujel.reusables.SettingsReader;
import net.rujel.reusables.Various;
import net.rujel.reusables.WOLogLevel;

import com.webobjects.foundation.*;
import com.webobjects.appserver.*;
import com.webobjects.eocontrol.*;

public class LessonList extends WOComponent {
    public EduLesson lessonItem;
	public NSKeyValueCoding extItem;
	
    protected String _studentPresenter;
	public String studentPresenter() {
		if(_studentPresenter == null)
			_studentPresenter = (String)parent().valueForKeyPath("present.presenter");
		if (_studentPresenter == null)
			_studentPresenter = "NotePresenter";
		//SettingsReader.stringForKeyPath("ui.presenter.note","NotePresenter");
		return _studentPresenter;
	}
    public LessonList(WOContext context) {
        super(context);
    }
	
	public void selectLesson() {
		if(hasBinding("currLesson"))
			setValueForBinding(lessonItem,"currLesson");
		if(hasBinding("selector"))
			setValueForBinding(lessonItem,"selector");
		if(lessonItem != null) {
			EOEditingContext ec = lessonItem.editingContext();
			if (ec.hasChanges()) ec.revert();
			_access = (NamedFlags)session().valueForKeyPath("readAccess.FLAGS.lessonItem");
		}
    }	
	
	public String rowClass() {
		if(lessonItem == valueForBinding("currLesson")) return "selection";
		Object fromProps = lessonProperties().valueForKey("class");
		if(fromProps != null)
			return fromProps.toString();
		if(lessonItem.title() != null) return "gerade";
		else return "ungerade";
	}
	
	public Boolean canEdit() {
		if(lessonItem == valueForBinding("currLesson")) {
			Boolean acc = (Boolean)access().valueForKey("edit");
//				(Boolean)session().valueForKeyPath("readAccess.edit.currLesson");
			return acc;
		} else return Boolean.FALSE;
	}
	
	public String dateFieldID() {
		if(valueForBinding("selector") == null)
			return "focus";
		else
			return null;
	}
	
	public String themeFieldID() {
		Object selector = valueForBinding("selector");
		if(selector != null && selector.equals(lessonItem))
			return "focus";
		else
			return null;
	}
	
	protected static Format _dateFormat = 
		new SimpleDateFormat(SettingsReader.stringForKeyPath("ui.dateFormat","yyyy-MM-dd"));
	protected static Format shortDateFormat = 
		new SimpleDateFormat(SettingsReader.stringForKeyPath("ui.shortDateFormat","MM/dd"));
	public Format dateFormat() {
		if(Various.boolForObject(valueForBinding("wide")))
			return _dateFormat;
		else
			return shortDateFormat;	
 	}
	
	public String lessonTitle() {
		if(lessonItem == null) return null;
        String result = lessonItem.title();
		if(result != null)
			return result;
		return dateFormat().format(lessonItem.date());
    }
	
	public void setLessonTitle(String aValue) {
		String newTitle = null;
		if(aValue != null) {
			Date aDate = (Date)dateFormat().parseObject(
					aValue, new java.text.ParsePosition(0));
			if(aDate == null) {
				if(aValue.length() > 10)
					newTitle = aValue.substring(0,10);
				else
					newTitle = aValue;
				makeDateFromNum(lessonItem);
			} else {
				Integer eduYear = (Integer)session().valueForKey("eduYear");
				aDate = MyUtility.dateToEduYear(aDate, eduYear);
				lessonItem.setDate(new NSTimestamp(aDate));
			}
		}
		lessonItem.setTitle(newTitle);
    }
	
	protected static void makeDateFromNum(EduLesson les) {
		NSArray args = new NSArray(new Object[] {les.course(),les.number()});
		EOQualifier qual = EOQualifier.qualifierWithQualifierFormat("" +
				"course = %@ AND number < %@ AND title = nil",args);
		NSArray sort = new NSArray(new EOSortOrdering ("number",
				EOSortOrdering.CompareDescending));
		EOFetchSpecification fs = new EOFetchSpecification(EduLesson.entityName,qual,sort);
		fs.setFetchLimit(1);
		NSArray found = les.editingContext().objectsWithFetchSpecification(fs);
		if(found == null || found.count() == 0) {
			qual = EOQualifier.qualifierWithQualifierFormat(
					"course = %@ AND number > %@ AND title = nil",args);
			fs.setQualifier(qual);
			sort = new NSArray(new EOSortOrdering ("number",EOSortOrdering.CompareAscending));
			fs.setSortOrderings(sort);
			found = les.editingContext().objectsWithFetchSpecification(fs);
			if(found == null || found.count() == 0) return;
		}
		EduLesson ajacent = (EduLesson)found.objectAtIndex(0);
		les.setDate(ajacent.date());
	}
	
	public boolean canMove () {
		return (hasBinding("up") && hasBinding("down") && hasBinding("splitTab"));
	}
	
	public boolean isStateless() {
		return true;
	}
	
	public boolean synchronizesVariablesWithBindings() {
		return false;
	}
	
	public void reset() {
		super.reset();
		_studentPresenter = null;
		_extentions = null;
		extItem = null;
		lessonProperies = null;
		_access = null;
	}

	protected NSArray _extentions;
	public NSArray extentions() {
		if(lessonItem == null || lessonItem != valueForBinding("selector") ||
				Various.boolForObject(valueForBinding("wide")))
			return null;
		if(_extentions == null) {
			session().setObjectForKey(lessonItem, "currentLesson");
			_extentions = (NSArray)session().valueForKeyPath("modules.extendLesson");
			session().removeObjectForKey("currentLesson");
			if(_extentions == null)
				_extentions = NSArray.EmptyArray;
		}
		if(_extentions.count() == 0)
			return null;
		return _extentions;
	}
	
	
	protected boolean expiredDate(Date date) {
		if(date == null)
			return false;
		NSTimestamp today = (NSTimestamp)session().valueForKey("today");
		if(today == null)
			return false;
		GregorianCalendar cur = new GregorianCalendar();
		cur.setTime(today);
		GregorianCalendar test = new GregorianCalendar();
		test.setTime(date);
		int diff = test.get(GregorianCalendar.YEAR) - cur.get(GregorianCalendar.YEAR);
		if(diff > 0)
			return false;
		else if (diff < 0)
			return true;
		return (test.get(GregorianCalendar.DAY_OF_YEAR) < 
						cur.get(GregorianCalendar.DAY_OF_YEAR));
	}

	public String dateCellStyle() {
		if(lessonItem == valueForBinding("selector"))
			return "selection";
		if(expiredDate(lessonItem.date()))
			return "grey";
		return null;
	}
	
	protected NSMutableDictionary lessonProperies;
	public NSDictionary lessonProperties() {
		if(lessonProperies == null)
			lessonProperies = (NSMutableDictionary)session().objectForKey("lessonProperies");
		NSDictionary lProps = null;
		if(lessonProperies != null)
			lProps = (NSDictionary)lessonProperies.objectForKey(lessonItem);
		if(lProps != null)
			return lProps;
		NSArray lessonsList = (NSArray)valueForBinding("lessonsList");
		if(lessonsList == null || lessonsList.count() == 0)
			return NSDictionary.EmptyDictionary;
		session().setObjectForKey(lessonsList, "lessonsList");
		NSArray propertiesList = (NSArray)session().valueForKeyPath(
				"modules.lessonProperies");
		session().removeObjectForKey("lessonsList");
		lessonProperies = new NSMutableDictionary();
		if(propertiesList == null || propertiesList.count() == 0) 
			return NSDictionary.EmptyDictionary;
		Enumeration lessons = lessonsList.objectEnumerator();
		while (lessons.hasMoreElements()) {
			EduLesson lesson = (EduLesson) lessons.nextElement();
			NSMutableDictionary currProperties = new NSMutableDictionary();
			Enumeration prEnu = propertiesList.objectEnumerator();
			while (prEnu.hasMoreElements()) {
				NSDictionary mProps = (NSDictionary) prEnu.nextElement();
				NSDictionary lm = (NSDictionary)mProps.objectForKey(lesson); 
				if(lm == null)
					continue;
				Enumeration lmEnu = lm.keyEnumerator();
				while (lmEnu.hasMoreElements()) {
					String key = (String) lmEnu.nextElement();
					Object value = lm.objectForKey(key);
					if(key.equals("image")) {
						NSMutableArray images = (NSMutableArray)
						currProperties.valueForKey("images");
						if(images == null) {
							images = new NSMutableArray(value);
							currProperties.setObjectForKey(images, "images");
						} else {
							images.addObject(value);
						}
					} else { // !key.equals("image")
						StringBuffer buf = (StringBuffer)currProperties.valueForKey(key);
						if(buf == null) {
							buf = new StringBuffer(value.toString());
							currProperties.setObjectForKey(buf, key);
						} else {
							buf.append(' ').append(value.toString());
						}
					}
				}// lm.keyEnumerator();
			} // propertiesList.objectEnumerator();
			lessonProperies.setObjectForKey((currProperties.count() > 0)?
					currProperties:NSDictionary.EmptyDictionary, lesson);
			if(lesson == lessonItem)
				lProps = currProperties;
		}// lessonsList.objectEnumerator();
		session().setObjectForKey(lessonProperies, "lessonProperies");
		if(lProps == null) {
			Logger.getLogger("rujel.ui").log(WOLogLevel.WARNING,
					"Something wrong reading properties",session());
			lProps = NSDictionary.EmptyDictionary;
		}
		return lProps;
	}
	
	public WOActionResults newLessonPopup() {
		valueForBinding("save");
		WOComponent popup = (WOComponent)session().objectForKey("LessonInspector");
		if(popup != null) {
			popup.ensureAwakeInContext(context());
			session().removeObjectForKey("LessonInspector");
		} else {
			popup = pageWithName("LessonInspector");
		}
		popup.takeValueForKey(context().page(), "returnPage");
		session().setObjectForKey(valueForBinding("course"), "assumeNextLesson");
		NSArray ls = (NSArray)session().valueForKeyPath("modules.assumeNextLesson");
		session().removeObjectForKey("assumeNextLesson");
		NSTimestamp date = null;
		String title = null;
		if(ls != null && ls.count() > 0) {
			Enumeration en = ls.objectEnumerator();
			String theme = null;
			while (en.hasMoreElements() && 
					(date == null || theme == null || title == null)) {
				NSKeyValueCoding la = (NSKeyValueCoding) en.nextElement();
				if(date == null)
					date = (NSTimestamp)la.valueForKey("date");
				if(title == null)
					title = (String)la.valueForKey("theme");
				if(title == null)
					title = (String)la.valueForKey("title");
			}
			if(theme != null)
				popup.takeValueForKey(theme, "newTheme");
		}
		if(date == null)
			date = (NSTimestamp)session().valueForKey("today");
		popup.takeValueForKey(date, "newDate");
		if(title == null)
			title = MyUtility.dateFormat().format(date);
		popup.takeValueForKey(title, "newTitle");
		return popup;
	}
	
	public String showHomeTaskOnClick() {
		if(lessonItem == null || lessonItem != valueForBinding("currLesson"))
			return null;
		if(BaseLesson.getTaskDelegate().hasPopup() && lessonItem.homeTask() == null)
			return (String)session().valueForKey("ajaxPopup");
		if(Various.boolForObject(valueForBinding("wide")))
			return null;
		return "returnField=document.getElementById('homeTask');myPrompt(htTitle,null,this);";
	}
	
	public WOActionResults popupHomeTask() {
		if(!BaseLesson.getTaskDelegate().hasPopup())
			return null;
		return BaseLesson.getTaskDelegate().homeWorkPopupForLesson(context(), lessonItem);
	}
	
	protected NamedFlags _access;
	public NamedFlags access() {
		if(_access == null) {
			if(valueForBinding("currLesson") == null) {
				String ent = EduLesson.entityName; 
				try {
					ent = (String)parent().valueForKeyPath("present.entityName");
					if(ent == null)
						ent = EduLesson.entityName;
				} catch (Exception e) {
					;
				}
				_access = (NamedFlags)session().valueForKeyPath("readAccess.FLAGS." + ent);
			} else {
				_access = (NamedFlags)session().valueForKeyPath(
						"readAccess.FLAGS.currLesson");
			}
		}
		return _access;
	}
}
