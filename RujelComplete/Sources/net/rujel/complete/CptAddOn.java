package net.rujel.complete;

import java.util.Enumeration;

import net.rujel.base.SettingsBase;
import net.rujel.interfaces.EduCourse;
import net.rujel.interfaces.Person;
import net.rujel.interfaces.Student;
import net.rujel.reusables.NamedFlags;

import com.webobjects.appserver.WOSession;
import com.webobjects.eoaccess.EOUtilities;
import com.webobjects.eocontrol.EOEditingContext;
import com.webobjects.eocontrol.EOKeyGlobalID;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSDictionary;
import com.webobjects.foundation.NSKeyValueCoding;
import com.webobjects.foundation.NSMutableArray;
import com.webobjects.foundation.NSMutableDictionary;

public class CptAddOn implements NSKeyValueCoding, NSKeyValueCoding.ErrorHandling {

	protected NSDictionary dict;
	protected NamedFlags access;
	protected EduCourse course;
	protected boolean active;
	protected NSMutableDictionary agregate;
	
	public CptAddOn(WOSession ses) {
		dict = (NSDictionary)ses.valueForKeyPath("strings.RujelComplete_Complete.addOn");
		access = (NamedFlags)ses.valueForKeyPath("readAccess.FLAGS.Completion");
	}
	
	public NamedFlags access() {
		return access;
	}
	
	public void setCourse(EduCourse crs) {
		if(crs == course)
			return;
		course = crs;
		agregate = null;
		if(crs == null) {
			active = false;
			return;
		}
		String closing = SettingsBase.stringSettingForCourse(
				Completion.SETTINGS_BASE, course, course.editingContext());
		active = Boolean.parseBoolean(closing);
	}
	
	protected void getAgregate() {
		EOEditingContext ec = course.editingContext();
		NSMutableArray courseList = course.groupList().mutableClone();
		Completion all = null;
		NSMutableDictionary param = new NSMutableDictionary(course,"course");
		param.takeValueForKey("student", Completion.ASPECT_KEY);
		NSArray found = EOUtilities.objectsMatchingValues(ec, Completion.ENTITY_NAME, param);
		agregate = new NSMutableDictionary();
		if(found != null && found.count() >= 0) {
			Enumeration enu = found.objectEnumerator();
			while (enu.hasMoreElements()) {
				Completion cpt = (Completion) enu.nextElement();
				Student student = cpt.student();
				if(student == null) {
					all = cpt;
					continue;
				}
				NSMutableDictionary _dict = new NSMutableDictionary(cpt,Completion.ENTITY_NAME);
				boolean closed = cpt.closeDate() != null;
				_dict.takeValueForKey(Boolean.valueOf(closed), "closed");
				_dict.takeValueForKey(cpt.present(), "hover");
				_dict.takeValueForKey(Person.Utility.fullName(student, true, 2, 2, 0), "title");
				EOKeyGlobalID gid = (EOKeyGlobalID)ec.globalIDForObject(student);
				_dict.takeValueForKey("cpt" + gid.keyValues()[0], "id");
				courseList.removeObject(student);
				agregate.setObjectForKey(_dict, student);
			}
		}
		if(courseList.count() > 0) {
			NSDictionary _dict;
			if(all != null) {
				_dict = new NSMutableDictionary(all,Completion.ENTITY_NAME);
				boolean closed = all.closeDate() != null;
				_dict.takeValueForKey(Boolean.valueOf(closed), "closed");
				_dict.takeValueForKey(all.present(), "hover");
			} else {
				_dict = new NSDictionary(Boolean.FALSE,"closed");
			}
			Enumeration enu = courseList.objectEnumerator();
			while (enu.hasMoreElements()) {
				Student student = (Student) enu.nextElement();
				NSMutableDictionary dic = _dict.mutableClone();
				dic.takeValueForKey(Person.Utility.fullName(student, true, 2, 2, 0), "title");
				EOKeyGlobalID gid = (EOKeyGlobalID)ec.globalIDForObject(student);
				dic.takeValueForKey("cpt" + gid.keyValues()[0], "id");
				agregate.setObjectForKey(dic, student);
			}
		}
	}
	
	public NSMutableDictionary dictForStudent(Student student) {
		if(agregate == null) getAgregate();
		return (NSMutableDictionary)agregate.objectForKey(student);
	}
	
	public Boolean hide() {
		return Boolean.valueOf(!active);
	}
	
	public void takeValueForKey(Object arg0, String arg1) {
		NSKeyValueCoding.DefaultImplementation.takeValueForKey(this, arg0, arg1);
	}

	public Object valueForKey(String arg0) {
		return NSKeyValueCoding.DefaultImplementation.valueForKey(this, arg0);
	}

	public Object handleQueryWithUnboundKey(String arg0) {
		return dict.valueForKey(arg0);
	}

	public void handleTakeValueForUnboundKey(Object arg0, String arg1) {
		NSKeyValueCoding.DefaultImplementation.handleTakeValueForUnboundKey(this, arg0, arg1);
	}

	public void unableToSetNullForKey(String arg0) {
		NSKeyValueCoding.DefaultImplementation.unableToSetNullForKey(this, arg0);
	}

}
