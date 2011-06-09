package net.rujel.ui;

import net.rujel.base.BaseLesson;
import net.rujel.interfaces.EduLesson;
import net.rujel.interfaces.PerPersonLink;
import net.rujel.reusables.Various;

import com.webobjects.appserver.WOActionResults;
import com.webobjects.appserver.WOContext;
import com.webobjects.eocontrol.EOKeyValueQualifier;
import com.webobjects.eocontrol.EOQualifier;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSKeyValueCoding;
import com.webobjects.foundation.NSMutableDictionary;

public class ConsolidatedList extends LessonList {
	
	public PerPersonLink.Dictionary dateItem;
	
    public ConsolidatedList(WOContext context) {
        super(context);
    }
    
    public EduLesson lastLessonItem() {
    	if(dateItem == null)
    		return null;
    	NSArray lessons = (NSArray)dateItem.valueForKey("lessons");
    	if(lessons == null || lessons.count() == 0)
    		return null;
    	return (EduLesson)lessons.lastObject();
    }
    
	public String showHomeTaskOnClick() {
		if(dateItem == null)
			return null;
		if(BaseLesson.getTaskDelegate().hasPopup())
			return (String)session().valueForKey("ajaxPopup");
		if(Various.boolForObject(valueForBinding("wide")))
			return null;
		if(dateItem != valueForBinding("currLesson"))
			return (String)session().valueForKey("checkRun");
		return "returnField=document.getElementById('homeTask');myPrompt(htTitle,null,this);";
	}
	
	public WOActionResults popupHomeTask() {
		if(!BaseLesson.getTaskDelegate().hasPopup()) {
			return selectLesson();
		}
		return BaseLesson.getTaskDelegate().homeWorkPopupForLesson(context(), lastLessonItem());
	}

	public boolean isSelector() {
		return (dateItem != null && dateItem == valueForBinding("selector"));
	}
	
	public boolean isCurrent() {
		return (dateItem != null && dateItem == valueForBinding("currLesson"));
	}

	public WOActionResults selectLesson() {
		if(hasBinding("selector"))
			setValueForBinding(dateItem,"selector");
		if(hasBinding("currLesson"))
			setValueForBinding(dateItem,"currLesson");
		return null;
    }

	public Object propForLesson(String prop, EduLesson lesson) {
		if(dateItem == null)
			return null;
		Object val = dateItem.valueForKey(prop);
		if(lesson == null)
			return val;
		NSMutableDictionary bl = (NSMutableDictionary)dateItem.valueForKey("byLesson");
		if(bl == null) return val;
		bl = (NSMutableDictionary)bl.objectForKey(lesson);
		if(bl == null) return val;
		Object add = bl.valueForKey(prop);
		if(add == null) return val;
		if(val == null) return add;
		StringBuilder buf = new StringBuilder(val.toString());
		if(prop.equals("class"))
			buf.append(' ');
		else
			buf.append('\n');
		buf.append(add);
		return buf;
	}
	
	public NSKeyValueCoding lessonProperties() {
		if(lessonProperties == null) {
			lessonProperties = new NSKeyValueCoding() {
				public Object valueForKey(String key) {
					return propForLesson(key,lessonItem);
				}
				public void takeValueForKey(Object arg0, String arg1) {
					; //do nothing
				}
			};
		}
		return lessonProperties;
	}
	
	public EduLesson oneLesson() {
		if(dateItem == null)
			return null;
		NSArray lessons = (NSArray)dateItem.valueForKey("lessons");
		if(lessons != null && lessons.count() == 1)
			return (EduLesson)lessons.objectAtIndex(0);
		return null;
	}
	
	public String rowClass() {
		if(dateItem == null)
			return null;
		EduLesson lesson = lessonItem;
		NSArray lessons = (NSArray)dateItem.valueForKey("lessons");
		if(lesson == null) {
			if(dateItem == valueForBinding("currLesson")) return "selection";
			if(lessons != null && lessons.count() == 1)
				lesson = (EduLesson)lessons.objectAtIndex(0);
		} else if(lessons != null && lessons.count() == 1) {
			return null;
		}
		Object fromProps = propForLesson("class", lesson);
		if(fromProps != null)
			return fromProps.toString();
		if(lesson == null)
			return "backfield2";
		return "ungerade";
	}
	
	public NSArray extentions() {
		if(dateItem == null) return null;
		NSArray list = super.extentions();
		if(list == null) return null;
		if(lessonItem == null) {
			if(oneLesson() != null)
				return list;
			EOQualifier qual = new EOKeyValueQualifier("lessonRequired",
					EOQualifier.QualifierOperatorNotEqual, Boolean.TRUE);
			return EOQualifier.filteredArrayWithQualifier(list, qual);
		} else {
			EOQualifier qual = new EOKeyValueQualifier("lessonRequired",
					EOQualifier.QualifierOperatorEqual, Boolean.TRUE);
			return EOQualifier.filteredArrayWithQualifier(list, qual);
		}
	}

}