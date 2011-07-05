package net.rujel.ui;

import java.util.Calendar;
import java.util.Enumeration;

import net.rujel.base.BaseLesson;
import net.rujel.interfaces.EduLesson;
import net.rujel.interfaces.PerPersonLink;
import net.rujel.reusables.DegenerateFlags;
import net.rujel.reusables.NamedFlags;
import net.rujel.reusables.Various;

import com.webobjects.appserver.WOActionResults;
import com.webobjects.appserver.WOComponent;
import com.webobjects.appserver.WOContext;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSDictionary;
import com.webobjects.foundation.NSKeyValueCoding;
import com.webobjects.foundation.NSMutableArray;
import com.webobjects.foundation.NSMutableDictionary;
import com.webobjects.foundation.NSTimestamp;

public class ConsolidatedList extends WOComponent {
	
//	public PerPersonLink.Dictionary dateItem;
	public NSMutableArray rows;
	public NSKeyValueCoding rowItem;
	public Boolean activeRow;
	
    public ConsolidatedList(WOContext context) {
        super(context);
    }
    
    public void reset() {
    	super.reset();
    	rows = null;
    	rowItem = null;
    	_access = null;
    	activeRow = null;
    }
    
	public String showHomeTaskOnClick() {
		if(BaseLesson.getTaskDelegate().hasPopup())
			return (String)session().valueForKey("ajaxPopup");
		if(Various.boolForObject(valueForBinding("wide")))
			return null;
//		if(dateItem != valueForBinding("currLesson"))
//			return (String)session().valueForKey("checkRun");
		return "returnField=document.getElementById('homeTask');myPrompt(htTitle,null,this);";
	}
	
	public WOActionResults popupHomeTask() {
		if(!BaseLesson.getTaskDelegate().hasPopup()) {
			return selectLesson();
		}
		return BaseLesson.getTaskDelegate().homeWorkPopupForLesson(context(), 
				(EduLesson)rowItem.valueForKey("lesson"));
	}

	public WOActionResults selectLesson() {
		EduLesson lesson = (EduLesson)rowItem.valueForKey("object");
		NSTimestamp date = lesson.date();
		NSKeyValueCoding curr = (NSKeyValueCoding)valueForBinding("currLesson");
		if(curr != null && date.equals(curr.valueForKey("date"))) {
			setValueForBinding(null,"currLesson");
			return null;
		}
		NSArray lessonsList = (NSArray)valueForBinding("lessonsList");
		Enumeration enu = lessonsList.objectEnumerator();
		while (enu.hasMoreElements()) {
			PerPersonLink.Dictionary plink = (PerPersonLink.Dictionary) enu.nextElement();
			if(date.equals(plink.valueForKey("date"))) {
				if(hasBinding("selector"))
					setValueForBinding(plink,"selector");
				if(hasBinding("currLesson"))
					setValueForBinding(plink,"currLesson");
				return null;
			}
		}
		return null;
    }
	
	public NSArray rows() {
		if(rows == null)
			prepareRows();
		return rows;
	}
	
	public void prepareRows() {
		NSArray lessonsList = (NSArray)valueForBinding("lessonsList");
		if(lessonsList == null || lessonsList.count() == 0)
			return;
		rows = new NSMutableArray();
		Enumeration lenu = lessonsList.objectEnumerator();
		int length = 0;
		PerPersonLink.Dictionary item = null;
		NSKeyValueCoding current = (NSKeyValueCoding)valueForBinding("currLesson");
		NSTimestamp currDate = (current == null)? null : (NSTimestamp)current.valueForKey("date");
		NSArray views = (NSArray)valueForBinding("views");
		NSArray activeViews = (NSArray)session().valueForKeyPath("state.consolidatedView");
		NSTimestamp today = (NSTimestamp)session().valueForKey("today");
		{
			Calendar cal = Calendar.getInstance();
			cal.setTime(today);
			cal.add(Calendar.DATE, -1);
			cal.set(Calendar.HOUR_OF_DAY, 23);
			cal.set(Calendar.MINUTE, 59);
			today = new NSTimestamp(cal.getTimeInMillis());
		}
		while (lenu.hasMoreElements()) {
			length = separator(length, item == current, today);
			item = (PerPersonLink.Dictionary) lenu.nextElement();
			NSArray list = (NSArray)item.valueForKey("lessons");
			if(list != null && list.count() > 0) { // has lessons
				Enumeration enu = list.objectEnumerator();
				NSMutableDictionary byLesson = (NSMutableDictionary)item.valueForKey("byLesson");
				boolean byDate = BaseLesson.getTaskDelegate().byDate();
				while (enu.hasMoreElements()) {
					EduLesson lesson = (EduLesson) enu.nextElement();
					NSMutableDictionary dict = new NSMutableDictionary(lesson,"object");
					dict.takeValueForKey(lesson.date(), "date");
					dict.takeValueForKey(lesson.theme(), "theme");
					dict.takeValueForKey("ungerade", "rowClass");
					dict.takeValueForKey("ungerade", "cellClass");
					if(!byDate || rows.count() == length) {
						String homeTask = lesson.homeTask();
						if(homeTask != null) {
							dict.takeValueForKey(homeTask, "extHover");
							dict.takeValueForKey(session().valueForKeyPath(
								"strings.RujelInterfaces_Names.EduLesson.ht"), "extShort");
							dict.takeValueForKey("hasText", "extClass");
						}
					}
					if(rows.count() == length) {
						if(byDate)
							dict.takeValueForKey(new Integer(list.count()), "extSpan");
					} else {
						dict.takeValueForKey(Boolean.TRUE, "skipDate");
						if(byDate)
							dict.takeValueForKey(Boolean.TRUE, "skipExt");
					}
					NSMutableDictionary bl = (byLesson == null)? null : 
						(NSMutableDictionary)byLesson.objectForKey(lesson);
					CharSequence val = (CharSequence)item.valueForKey("rowClass");
					val = DateAgregate.appendFromDict(val, bl, "rowClass", ' ');
					if(item == current)
						val = DateAgregate.appendFromDict(val, null, "selectionBorder", ' ');
					if(val != null)
						dict.takeValueForKey(val, "cellClass");
					val = (CharSequence)item.valueForKey("rowStyle");
					val = DateAgregate.appendFromDict(val, bl, "rowStyle", null);
					dict.takeValueForKey(val, "cellStyle");
					if(bl != null)
						dict.takeValueForKey(bl.valueForKey("rowHover"), "rowHover");
					rows.addObject(dict);
				} // lessons enumeration
			} // has lessons 
			if(current == null)
				continue;
			if(views == null || views.count() == 0)
				continue;
			if(activeViews == null || activeViews.count() == 0)
				continue;
			Enumeration enu = views.objectEnumerator();
			while (enu.hasMoreElements()) {
				NSKeyValueCoding view = (NSKeyValueCoding) enu.nextElement();
				Object val = view.valueForKey("id");
				if(val == null || !activeViews.containsObject(val))
					continue; // dont show inactive
				val = view.valueForKey("rows");
				if(val == null)
					continue;
				NSArray addRows = (NSArray)item.valueForKey((String)val);
				if(addRows == null || addRows.count() == 0)
					continue;
				if(current == item) {
					if(rows.count() > 0) {
						NSDictionary separatorRow = new NSDictionary(
								new Object[] {"selection","2","3",Boolean.TRUE,Boolean.TRUE},
								new String[] {"cellClass","colspan","height","skipExt","skipDate"});
						rows.addObject(separatorRow);
					}
					rows.addObjectsFromArray(addRows);
					continue;
				} else if(view.valueForKey("otherDate") == null) {
					continue;
				} else {
					val = item.valueForKey("date");
					if(currDate.after((NSTimestamp)val))
						continue;
				}
				Enumeration renu = addRows.objectEnumerator();
				NSDictionary separatorRow = new NSDictionary(
						new Object[] {"grey","2","2",Boolean.TRUE,Boolean.TRUE},
						new String[] {"cellClass","colspan","height","skipExt","skipDate"});
				while (renu.hasMoreElements()) {
					NSKeyValueCoding row = (NSKeyValueCoding) renu.nextElement();
					val = row.valueForKey("otherDate");
					if(currDate.before((NSTimestamp)val))
						continue;
					if(separatorRow != null) {
						rows.add(separatorRow);
						separatorRow = null;
					}
					rows.addObject(row);
				} // 
			} // views enumeration
		} // lessonsList enumeration
		separator(length, item == current, today);
	}
	
	private int separator(int length,boolean isCurrent, NSTimestamp today) {
		if(rows.count() > length) {
			NSMutableDictionary initRow = (NSMutableDictionary)rows.objectAtIndex(length);
			length = rows.count() - length;
			if(length > 1)
				initRow.takeValueForKey(new Integer(length), "dateSpan");
			if(isCurrent) {
				initRow.takeValueForKey("selection", "dateClass");
				initRow.takeValueForKey("selection", "separatorClass");
				activeRow = Boolean.TRUE;
			} else {
				NSTimestamp date = (NSTimestamp)initRow.valueForKey("date");
				initRow.takeValueForKey((today.after(date))?"grey":"ungerade", "dateClass");
				if(activeRow == Boolean.TRUE) {
					initRow.takeValueForKey("selection", "separatorClass");
					activeRow = null;
				}
			}
		}
		return rows.count();
	}

	protected NamedFlags _access;
	public NamedFlags access() {
		if(_access == null) {
			// TODO: get access
			_access = DegenerateFlags.ALL_TRUE;
		}
		return _access;
	}

	
	public boolean isStateless() {
		return true;
	}

	public boolean synchronizesVariablesWithBindings() {
		return false;
	}
}