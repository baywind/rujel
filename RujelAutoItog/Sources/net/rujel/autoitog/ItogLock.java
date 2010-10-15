package net.rujel.autoitog;

import com.webobjects.appserver.WOApplication;
import com.webobjects.appserver.WOContext;
import com.webobjects.eocontrol.EOEnterpriseObject;
import com.webobjects.foundation.NSDictionary;

import net.rujel.auth.ReadAccess.Modifier;
import net.rujel.base.SettingsBase;
import net.rujel.eduresults.ItogContainer;
import net.rujel.eduresults.ItogMark;
import net.rujel.interfaces.EduCourse;

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
			EduCourse course = (EduCourse)ctx.page().valueForKeyPath("addOn.eduCourse");
			listName = SettingsBase.stringSettingForCourse(ItogMark.ENTITY_NAME,
					course, course.editingContext());
		} catch (Exception e) {
			SettingsBase settings = SettingsBase.baseForKey(ItogMark.ENTITY_NAME,
					container.editingContext(),false);
			if(obj instanceof ItogMark) {
				Object val = ((ItogMark)obj).cycle(); // assumeCourse();
				NSDictionary dict = new NSDictionary(
						new Object[] {val,container.eduYear()},
						new String[] {"cycle","eduYear"});
				EOEnterpriseObject bc = settings.forCourse(dict);
				listName = (String)bc.valueForKey(SettingsBase.TEXT_VALUE_KEY);
			} else {
				listName = settings.textValue();
			}
		}
		if(listName == null)
			return null;
		AutoItog ai = AutoItog.forListName(listName, container);
		if(ai != null)
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
