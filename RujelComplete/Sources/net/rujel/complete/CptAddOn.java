package net.rujel.complete;

import java.util.Enumeration;

import net.rujel.base.SettingsBase;
import net.rujel.interfaces.EduCourse;
import net.rujel.interfaces.Student;
import net.rujel.reusables.NamedFlags;
import net.rujel.reusables.Various;
import net.rujel.reusables.WOLogLevel;

import com.webobjects.appserver.WOSession;
import com.webobjects.eoaccess.EOUtilities;
import com.webobjects.eocontrol.*;
import com.webobjects.foundation.*;

public class CptAddOn implements NSKeyValueCoding, NSKeyValueCoding.ErrorHandling {

	protected WOSession session;
	protected NSDictionary dict;
	protected Completion global;
	protected NamedFlags access;
	protected EduCourse course;
	protected boolean active;
	protected boolean dim = false;
	protected NSMutableDictionary agregate;
	protected NSArray requires;
	protected ClosingLock closingLock;
	
	public CptAddOn(WOSession ses) {
		session = ses;
		dict = (NSDictionary)ses.valueForKeyPath("strings.RujelComplete_Complete.addOn");
		access = (NamedFlags)ses.valueForKeyPath("readAccess.FLAGS.Completion");
		NSArray modules = (NSArray)ses.valueForKeyPath("modules.courseComplete");
		EOQualifier qual = new EOKeyValueQualifier("precedes",
				EOQualifier.QualifierOperatorContains,"student");
		requires = EOQualifier.filteredArrayWithQualifier(modules, qual);
		closingLock = (ClosingLock)ses.valueForKeyPath("readAccess.modifier.ClosingLock");
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
		NSMutableArray courseList = course.groupList().mutableClone();
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
				Boolean closed = Boolean.valueOf(cpt.closeDate() != null);
				_dict.takeValueForKey(closed, "closed");
				_dict.takeValueForKey(cpt.present(), "hover");
//				_dict.takeValueForKey(Person.Utility.fullName(student, true, 2, 2, 0), "title");
				EOKeyGlobalID gid = (EOKeyGlobalID)ec.globalIDForObject(student);
				_dict.takeValueForKey("cpt" + gid.keyValues()[0], "id");
				courseList.removeObject(student);
				agregate.setObjectForKey(_dict, student);
			}
		}
		dim = !access().flagForKey("create");
		if(!dim)
			dim = (checkRequirements(course) != null);
		if(courseList.count() > 0) {
			NSMutableDictionary preset = new NSMutableDictionary();
			boolean closed = false;
			if(global != null) {
				preset.takeValueForKey(global, Completion.ENTITY_NAME);
				closed = (global.closeDate() != null);
				preset.takeValueForKey(global.present(), "hover");
			}
			preset.takeValueForKey(Boolean.valueOf(closed), "closed");
			if(closed) {
				preset.takeValueForKey(Boolean.FALSE, "checked");
			} else if(dim) {
				preset.takeValueForKey(Boolean.FALSE, "checked");
				preset.takeValueForKey(Boolean.TRUE, "disable");
			} else {
				preset.takeValueForKey(Boolean.TRUE, "checked");
			}
			Enumeration enu = courseList.objectEnumerator();
			while (enu.hasMoreElements()) {
				Student student = (Student) enu.nextElement();
				NSMutableDictionary dic = preset.mutableClone();
				EOKeyGlobalID gid = (EOKeyGlobalID)student.
				editingContext().globalIDForObject(student);
				dic.takeValueForKey("cpt" + gid.keyValues()[0], "id");
				agregate.setObjectForKey(dic, student);
			}
		}
		if(dim)
			return;
		session.setObjectForKey(course, "preventComplete");
		found = (NSArray)session.valueForKeyPath("modules.preventComplete");
		session.removeObjectForKey("preventComplete");
		if(found != null && found.count() > 0) {
			Enumeration enu = found.objectEnumerator();
			while (enu.hasMoreElements()) {
				NSKeyValueCoding lock = (NSKeyValueCoding) enu.nextElement();
				Student student = (Student)lock.valueForKey("student");
				NSMutableDictionary dic = (NSMutableDictionary)agregate.objectForKey(student);
				if(dic == null)
					continue;
				dic.takeValueForKey(Boolean.FALSE, "checked");
				if(Various.boolForObject(lock.valueForKey("disable"))) {
					dic.takeValueForKey(Boolean.TRUE, "disable");
					if(Various.boolForObject(dic.valueForKey("closed")))
						dic.takeValueForKey("warning", "styleClass");
					else
						dic.takeValueForKey("grey", "styleClass");
				} else if(Various.boolForObject(dic.valueForKey("closed")))
					dic.takeValueForKey("highlight", "styleClass");
				String title = (String)lock.valueForKey("title");
				if(title != null) {
					Object hover = dic.valueForKey("hover");
					if(hover instanceof StringBuilder) {
						((StringBuilder)hover).append(';').append(' ').append(title);
					} else {
						hover = new StringBuilder(title);
						dic.takeValueForKey(hover, "hover");
					}
				}
			}
		}
	}
	/*
	protected NSMutableDictionary prepareDict(Student student) {
		NSMutableDictionary dic = new NSMutableDictionary();
		if(global != null) {
			dic.takeValueForKey(global, Completion.ENTITY_NAME);
			dic.takeValueForKey(Boolean.valueOf(global.closeDate() != null), "closed");
			dic.takeValueForKey(global.present(), "hover");
		} else {
			dic.takeValueForKey(Boolean.FALSE, "closed");
		}
		dic.takeValueForKey(Boolean.TRUE, "checked");
		dic.takeValueForKey(access.valueForKey("_create"), "disable");
//		dic.takeValueForKey(Person.Utility.fullName(student, true, 2, 2, 0), "title");
		EOKeyGlobalID gid = (EOKeyGlobalID)student.
				editingContext().globalIDForObject(student);
		dic.takeValueForKey("cpt" + gid.keyValues()[0], "id");
		agregate.setObjectForKey(dic, student);
		return dic;
	}*/
	
	public NSMutableDictionary dictForStudent(Student student) {
		if(agregate == null) getAgregate();
		NSMutableDictionary dic = (NSMutableDictionary)agregate.objectForKey(student);
/*		if(dic == null) {
			if(course.groupList().containsObject(student))
				prepareDict(student);
		}*/
		return dic;
	}
	
	public Boolean hide() {
		return Boolean.valueOf(!active);
	}
	
	public String filename() {
		if(agregate == null) getAgregate();
		if(dim)
			return "stamp_gr.gif";
		else
			return "stamp.gif";
	}
	
	public void setCloseDict(NSMutableDictionary cd) {
		setCourse((EduCourse)cd.objectForKey("course"));
		String user = (String)cd.objectForKey("user");
		EOEditingContext ec = course.editingContext();
		NSTimestamp date = new NSTimestamp();
		NSArray toClose = (NSArray)cd.valueForKey("toClose");
		boolean closeAll = Various.boolForObject(cd.valueForKey("closeAll"));
		if(closeAll) {
			if(global == null) {
				global = (Completion)EOUtilities.createAndInsertInstance(ec,Completion.ENTITY_NAME);
				global.setCourse(course);
				global.setAspect("student");
				toClose = course.groupList();
			} else if(agregate != null)
				((NSArray)agregate.allValues()).takeValueForKey(Boolean.TRUE, "closed");
			global.setCloseDate(date);
			global.setWhoClosed(user);
		} else if(toClose == null) {
			Student releaseStudent = (Student)cd.valueForKey("releaseStudent");
			if(releaseStudent == null)
				return;
			NSMutableDictionary dic = dictForStudent(releaseStudent);
			Completion cpt = (Completion)dic.valueForKey(Completion.ENTITY_NAME);
			if(cpt != null && cpt.student() == releaseStudent) {
				cpt.setCloseDate(null);
				cpt.setWhoClosed(user);
//				toClose = new NSArray(releaseStudent);
				dic.takeValueForKey(Boolean.FALSE, "closed");
				dic.takeValueForKey(Boolean.FALSE, "checked");
				dic.takeValueForKey(user, "hover");
			} else { 
//				toClose = new NSArray(releaseStudent);
				toClose = course.groupList().mutableClone();
				((NSMutableArray)toClose).removeObject(releaseStudent);
			}
		}
		if(toClose != null) {
			Enumeration enu = course.groupList().objectEnumerator();//cd.keyEnumerator();
			while (enu.hasMoreElements()) {
				Student student = (Student) enu.nextElement();
				NSMutableDictionary dic = dictForStudent(student);
				boolean close = toClose.containsObject(student);
//				if(close && Various.boolForObject(dic.valueForKey("closed")))
//					continue;
				Completion cpt = (Completion)dic.valueForKey(Completion.ENTITY_NAME);
				if(cpt == null || student != cpt.student()) {
					cpt = (Completion)EOUtilities.createAndInsertInstance(ec,Completion.ENTITY_NAME);
					cpt.setStudent(student);
					cpt.setCourse(course);
					cpt.setAspect("student");
					dic.takeValueForKey(cpt, Completion.ENTITY_NAME);
					if(global != null) {
						cpt.setCloseDate(global.closeDate());
						cpt.setWhoClosed(global.whoClosed());
					}				
				} else if(closeAll) {
					ec.deleteObject(cpt);
					dic.takeValueForKey(global, Completion.ENTITY_NAME);
					dic.takeValueForKey(Boolean.TRUE, "closed");
					continue;
				} else if (cpt.closeDate() != null) {
					continue;
				}
				if(close) {
					if(cpt.closeDate() == null) {
						cpt.setCloseDate(date);
						cpt.setWhoClosed(user);
						dic.takeValueForKey(cpt.present(), "hover");
						dic.takeValueForKey(Boolean.TRUE, "closed");
					}
				} else {
					if(cpt.closeDate() != null) {
						cpt.setCloseDate(null);
						cpt.setWhoClosed(user);
						dic.takeValueForKey(Boolean.FALSE, "closed");
					}
					dic.takeValueForKey(Boolean.FALSE, "checked");
					if(!(dic.valueForKey("hover") instanceof StringBuilder))
						dic.takeValueForKey(cpt.present(), "hover");
				}
			}
			if(global != null && !closeAll)
				ec.deleteObject(global);
		}
		try {
			ec.saveChanges();
			if(global != null && global.editingContext() != ec)
				global = null;
			CompletePopup.logger.log(WOLogLevel.EDITING,"Student Completions saved",
					new Object[] {course,cd.allKeys()});
		} catch (Exception e) {
			ec.revert();
			agregate = null;
			CompletePopup.logger.log(WOLogLevel.WARNING,"Error saving student Completions",
					new Object[] {course,cd.allKeys(),e});
		}
		if(closingLock != null)
			closingLock.setCourse(null);
		Executor.Task executor = new Executor.Task();
		executor.date = session.valueForKey("eduYear");
		executor.setCourse(course);
		if(cd.valueForKey("toClose") == null) {
			Object releaseStudent = cd.valueForKey("releaseStudent");
			if(releaseStudent == null)
				toClose = course.groupList();
			else
				toClose = new NSArray(releaseStudent);
		}
		executor.setStudents(toClose);
		Executor.exec(executor);
	}
	
	public Object dropCompletionAgregate() {
		agregate = null;
		if(closingLock != null)
			closingLock.setCourse(null);
		return null;
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
