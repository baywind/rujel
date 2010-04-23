package net.rujel.curriculum;

import java.util.Enumeration;

import com.webobjects.appserver.WOApplication;
import com.webobjects.appserver.WOContext;
import com.webobjects.eocontrol.EOEditingContext;
import com.webobjects.eocontrol.EOEnterpriseObject;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSDictionary;
import com.webobjects.foundation.NSTimestamp;

import net.rujel.auth.ReadAccess;
import net.rujel.base.SettingsBase;
import net.rujel.interfaces.EOPeriod;
import net.rujel.interfaces.EduLesson;

public class OldLessonLock implements ReadAccess.Modifier {
	public static final Integer SORT = new Integer(80);
	protected SettingsBase sb;
	
	public OldLessonLock(SettingsBase settingsBase) {
		sb = settingsBase;
	}
	
	protected EduLesson lesson(Object obj, String subPath, WOContext ctx) {
		EduLesson lesson = null;
		if(obj instanceof EduLesson) {
			lesson = (EduLesson)obj;
		} else if(EduLesson.entityName.equals(obj)
				&& ctx.page().name().endsWith("LessonNoteEditor")) {
			lesson = (EduLesson)ctx.page().valueForKey("currLesson");
		}
		return lesson;
	}
	
	public String interpret(Object obj, String subPath, WOContext ctx) {
		EduLesson lesson = lesson(obj, subPath, ctx);
		if(lesson == null)
			return null;
		EOEnterpriseObject s = sb.forCourse(lesson.course());
		Integer days = (Integer)s.valueForKey(SettingsBase.NUMERIC_VALUE_KEY);
		if(days == null)
			return null;
		boolean save = "save".equals(subPath);
		EOEditingContext ec = lesson.editingContext();
		boolean isNew = (ec == null || ec.globalIDForObject(lesson).isTemporary());
		NSDictionary snapshot = (isNew)?NSDictionary.EmptyDictionary:
			ec.committedSnapshotForObject(lesson);
		NSTimestamp date = (NSTimestamp)snapshot.valueForKey("date");
		if (isNew || EOPeriod.Utility.countDays(date, null) -1 <= days.intValue()) {
			if(!save)
				return null;
			date = lesson.date();
			if (EOPeriod.Utility.countDays(date, null) -1 <= days.intValue())
				return null;
		}
		if(save) {
			if(isNew)
				return "oldLesson";
			snapshot = lesson.changesFromSnapshot(snapshot);
			if(snapshot == null || snapshot.count() == 0)
				return null;
			Enumeration enu = snapshot.objectEnumerator();
			while (enu.hasMoreElements()) {
				Object val = enu.nextElement();
				if(!(val instanceof NSArray))
					return "oldLesson";
			}
		} else {
			return "oldLesson";
		}
		return null;
	}
	
/*	public String validate(Object obj, String subPath, WOContext ctx) {
		EduLesson lesson = lesson(obj, subPath, ctx);
		if(lesson == null)
			return null;
		EOEditingContext ec = lesson.editingContext();
		if(invalid(lesson)) {
			String message = "oldLesson";
			if(ec == null || ec.globalIDForObject(lesson).isTemporary())
				return message;
			NSDictionary snapshot = ec.committedSnapshotForObject(lesson);
			snapshot = lesson.changesFromSnapshot(snapshot);
			if(snapshot == null || snapshot.count() == 0)
				return null;
			Enumeration enu = snapshot.objectEnumerator();
			while (enu.hasMoreElements()) {
				Object val = enu.nextElement();
				if(!(val instanceof NSArray))
					return message;
			}
		}
		return null;
	}*/
	
	public Number sort() {
		return SORT;
	}
	
	public String message() {
		return (String)WOApplication.application().valueForKeyPath(
				"strings.RujelCurriculum_Curriculum.messages.oldLesson");
	}
}
