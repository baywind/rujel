package net.rujel.autoitog;

import com.webobjects.appserver.WOApplication;
import com.webobjects.appserver.WOContext;
import com.webobjects.eocontrol.EOAndQualifier;
import com.webobjects.eocontrol.EOFetchSpecification;
import com.webobjects.eocontrol.EOKeyValueQualifier;
import com.webobjects.eocontrol.EOQualifier;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSDictionary;

import net.rujel.base.SettingsBase;
import net.rujel.base.ReadAccess.Modifier;
import net.rujel.eduresults.ItogContainer;
import net.rujel.eduresults.ItogMark;
import net.rujel.interfaces.EduCourse;
import net.rujel.interfaces.EduCycle;

public class ItogLock implements Modifier {
	public static final Integer SORT = new Integer(70);
	
//	protected SettingsBase settings;
	
	public ItogLock () {
		super();
//		settings = SettingsBase.baseForKey(ItogMark.ENTITY_NAME, ec, false);
	}

	public String interpret(Object obj, String subPath, WOContext ctx) {
//		if(settings == null)
//			return null;
		if(obj instanceof String) {
			if(!ItogMark.ENTITY_NAME.equals(obj))
				return null;
		} else if(!(obj instanceof ItogMark)) {
			return null;
		}
		ItogContainer container = null;
		String listName = null;
		if(obj instanceof ItogMark) {
			container = ((ItogMark)obj).container();
		} else {
			try {
				container = (ItogContainer)ctx.page().valueForKey("itogContainer");
			} catch (Exception e) {
				return null;
			}
		}
		try {
			EduCourse course = (EduCourse)ctx.page().valueForKeyPath("addOn.course");
			listName = SettingsBase.stringSettingForCourse(ItogMark.ENTITY_NAME,
					course, course.editingContext());
		} catch (Exception e) {
			SettingsBase settings = SettingsBase.baseForKey(ItogMark.ENTITY_NAME,
					container.editingContext(),false);
			if(obj instanceof ItogMark) {
				Object val = ((ItogMark)obj).cycle(); // assumeCourse();
				NSDictionary dict = SettingsBase.courseDict((EduCycle)val,container.eduYear());
				listName = settings.forCourse(dict).textValue();
			} else {
				listName = settings.textValue();
			}
		}
		if(listName == null)
			return null;
		EOQualifier quals[] = new EOQualifier[3];
		quals[0] = new EOKeyValueQualifier(AutoItog.LIST_NAME_KEY,
				EOQualifier.QualifierOperatorEqual, listName);
		quals[1] = new EOKeyValueQualifier(AutoItog.ITOG_CONTAINER_KEY,
				EOQualifier.QualifierOperatorEqual, container);
		quals[2] = new EOKeyValueQualifier(AutoItog.FLAGS_KEY,
				EOQualifier.QualifierOperatorLessThan, new Integer(32));
		quals[0] = new EOAndQualifier(new NSArray (quals));
		EOFetchSpecification fs = new EOFetchSpecification(AutoItog.ENTITY_NAME,quals[0],null);
		NSArray found = container.editingContext().objectsWithFetchSpecification(fs);
		if(found != null && found.count() > 0)
			return ItogMark.ENTITY_NAME.concat("@prognosed");
		return null;
	}

	public String message() {
		return (String)WOApplication.application().valueForKeyPath(
			"strings.RujelAutoItog_AutoItog.ui.ItogLock");
	}

	public Number sort() {
		return SORT;
	}

}
