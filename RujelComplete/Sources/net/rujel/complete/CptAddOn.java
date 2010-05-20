package net.rujel.complete;

import java.util.Enumeration;

import net.rujel.base.SettingsBase;
import net.rujel.interfaces.EduCourse;
import net.rujel.interfaces.Student;
import net.rujel.reusables.NamedFlags;
import net.rujel.reusables.WOLogLevel;

import com.webobjects.appserver.WOSession;
import com.webobjects.eoaccess.EOUtilities;
import com.webobjects.eocontrol.*;
import com.webobjects.foundation.*;

public class CptAddOn implements NSKeyValueCoding, NSKeyValueCoding.ErrorHandling {

	protected NSDictionary dict;
	protected Completion global;
	protected NamedFlags access;
	protected EduCourse course;
	protected boolean active;
	protected NSMutableDictionary agregate;
	protected NSArray requires;
	
	public CptAddOn(WOSession ses) {
		dict = (NSDictionary)ses.valueForKeyPath("strings.RujelComplete_Complete.addOn");
		access = (NamedFlags)ses.valueForKeyPath("readAccess.FLAGS.Completion");
		NSArray modules = (NSArray)ses.valueForKeyPath("modules.courseComplete");
		EOQualifier qual = new EOKeyValueQualifier("precedes",
				EOQualifier.QualifierOperatorContains,"student");
		requires = EOQualifier.filteredArrayWithQualifier(modules, qual);
	}
	
	public NamedFlags access() {
		return access;
	}
	
	public void setCourse(EduCourse crs) {
		if(crs == course)
			return;
		course = crs;
		agregate = null;
		global = null;
		if(crs == null) {
			active = false;
			return;
		}
		String closing = SettingsBase.stringSettingForCourse(
				Completion.SETTINGS_BASE, course, course.editingContext());
		active = Boolean.parseBoolean(closing);
	}
	
	public StringBuilder checkRequirements(EduCourse crs) {
		if(requires == null || requires.count() == 0)
			return null;
		NSMutableArray quals = new NSMutableArray();
		StringBuilder buf = new StringBuilder("<ul>");
		if(requires.count() > 1) {
			Enumeration enu = requires.objectEnumerator();
			while (enu.hasMoreElements()) {
				NSKeyValueCoding mod = (NSKeyValueCoding) enu.nextElement();
				quals.addObject(new EOKeyValueQualifier(Completion.ASPECT_KEY, 
						EOQualifier.QualifierOperatorEqual, mod.valueForKey("id")));
				buf.append("<li>").append(mod.valueForKey("title")).append("</li>\n");
			}
			EOQualifier qual = new EOOrQualifier(quals);
			quals.removeAllObjects();
			quals.addObject(qual);
		} else {
			NSKeyValueCoding mod = (NSKeyValueCoding)requires.objectAtIndex(0);
			quals.addObject(new EOKeyValueQualifier(Completion.ASPECT_KEY, 
					EOQualifier.QualifierOperatorEqual, mod.valueForKey("id")));
			buf.append("<li>").append(mod.valueForKey("title")).append("</li>\n");
		}
		quals.addObject(new EOKeyValueQualifier("course",
				EOQualifier.QualifierOperatorEqual, crs));
		quals.addObject(new EOKeyValueQualifier(Completion.CLOSE_DATE_KEY,
				EOQualifier.QualifierOperatorNotEqual, NullValue));
		EOFetchSpecification fs = new EOFetchSpecification(Completion.ENTITY_NAME,
				new EOAndQualifier(quals),null);
		NSArray found = crs.editingContext().objectsWithFetchSpecification(fs);
		if(found != null && found.count() >= requires.count())
			return null;
		return buf.append("</ul>");
	}
	
	protected void getAgregate() {
		EOEditingContext ec = course.editingContext();
//		NSMutableArray courseList = course.groupList().mutableClone();
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
					global = cpt;
					continue;
				}
				NSMutableDictionary _dict = new NSMutableDictionary(cpt,Completion.ENTITY_NAME);
				boolean closed = cpt.closeDate() != null;
				_dict.takeValueForKey(Boolean.valueOf(closed), "closed");
				_dict.takeValueForKey(cpt.present(), "hover");
//				_dict.takeValueForKey(Person.Utility.fullName(student, true, 2, 2, 0), "title");
				EOKeyGlobalID gid = (EOKeyGlobalID)ec.globalIDForObject(student);
				_dict.takeValueForKey("cpt" + gid.keyValues()[0], "id");
//				courseList.removeObject(student);
				agregate.setObjectForKey(_dict, student);
			}
		}
	}
	
	public NSMutableDictionary dictForStudent(Student student) {
		if(agregate == null) getAgregate();
		NSMutableDictionary dic = (NSMutableDictionary)agregate.objectForKey(student);
		if(dic == null) {
			dic = new NSMutableDictionary();
			if(global != null) {
				dic.takeValueForKey(global, Completion.ENTITY_NAME);
				dic.takeValueForKey(Boolean.valueOf(global.closeDate() != null), "closed");
				dic.takeValueForKey(global.present(), "hover");
			} else {
				dic.takeValueForKey(Boolean.FALSE, "closed");
			}
			dic.takeValueForKey(Boolean.valueOf(global.closeDate() != null), "closed");
//			dic.takeValueForKey(Person.Utility.fullName(student, true, 2, 2, 0), "title");
			EOKeyGlobalID gid = (EOKeyGlobalID)student.
					editingContext().globalIDForObject(student);
			dic.takeValueForKey("cpt" + gid.keyValues()[0], "id");
		}
		return dic;
	}
	
	public Boolean hide() {
		return Boolean.valueOf(!active);
	}
	
	public void setCloseDict(NSMutableDictionary cd) {
		setCourse((EduCourse)cd.removeObjectForKey("course"));
		String user = (String)cd.removeObjectForKey("user");
		if(cd.count() == 0)
			return;
		Enumeration enu = course.groupList().objectEnumerator();//cd.keyEnumerator();
		EOEditingContext ec = course.editingContext();
		while (enu.hasMoreElements()) {
			Student student = (Student) enu.nextElement();
			NSMutableDictionary dic = dictForStudent(student);
			Boolean closed = (Boolean)cd.objectForKey(student);
			dic.takeValueForKey(closed, "closed");
			Completion cpt = (Completion)dic.valueForKey(Completion.ENTITY_NAME);
			if(cpt == null || student != cpt.student()) {
				cpt = (Completion)EOUtilities.createAndInsertInstance(ec,Completion.ENTITY_NAME);
				cpt.setStudent(student);
				cpt.setCourse(course);
				cpt.setAspect("student");
				dic.takeValueForKey(cpt, Completion.ENTITY_NAME);
				agregate.setObjectForKey(dic, student);
				if(global != null && global.closeDate() != null) {
					cpt.setCloseDate(global.closeDate());
					cpt.setWhoClosed(global.whoClosed());
					if(closed != null && closed.booleanValue())
						continue;
				}
			}
			cpt.setCloseDate(closed.booleanValue()? new NSTimestamp() : null);
			if(closed.booleanValue() || cpt.whoClosed() != null)
				cpt.setWhoClosed(user);
			dic.takeValueForKey(cpt.present(), "hover");
		}
		if(global != null)
			ec.deleteObject(global);
		try {
			ec.saveChanges();
			global = null;
			CompletePopup.logger.log(WOLogLevel.UNOWNED_EDITING,"Student Completions saved",
					new Object[] {course,cd.allKeys()});
		} catch (Exception e) {
			ec.revert();
			agregate = null;
			CompletePopup.logger.log(WOLogLevel.WARNING,"Error saving student Completions",
					new Object[] {course,cd.allKeys(),e});
		}
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
