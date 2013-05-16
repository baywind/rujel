package net.rujel.base;


import java.util.logging.Logger;

import net.rujel.interfaces.EduCycle;
import net.rujel.interfaces.Teacher;
import net.rujel.reusables.Various;
import net.rujel.reusables.WOLogLevel;
import net.rujel.ui.TeacherSelector;

import com.webobjects.appserver.WOContext;
import com.webobjects.appserver.WOComponent;
import com.webobjects.appserver.WOMessage;
import com.webobjects.eoaccess.EOUtilities;
import com.webobjects.eocontrol.EOEditingContext;
import com.webobjects.eocontrol.EOEnterpriseObject;
import com.webobjects.eocontrol.EOSortOrdering;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSTimestamp;
import com.webobjects.appserver.WOActionResults;

public class CourseInspector extends WOComponent {
	protected static Logger logger = Logger.getLogger("rujel.journal");
	public WOComponent returnPage;
	public BaseCourse course;
	public NSArray changes;
	public EOEnterpriseObject changeItem;
	public NSTimestamp nextDate;
	public String nextComment;
	public Updater updater;
	
    public CourseInspector(WOContext context) {
        super(context);
        nextDate = (NSTimestamp)context.session().valueForKey("today");
    }
    
    public void setCourse(BaseCourse crs) {
    	course = crs;
    	if(Various.boolForObject(session().valueForKeyPath("readAccess._read.TeacherChange"))) {
    		changes = null;
    		return;
    	}
    	changes = course.teacherChanges();
    	if(changes != null && changes.count() > 1)
    		changes = EOSortOrdering.sortedArrayUsingKeyOrderArray(changes, MyUtility.dateSorter);
    }

	public String title() {
		if(course == null)
			return "???";
		StringBuilder buf = new StringBuilder();
		buf.append(WOMessage.stringByEscapingHTMLString(course.eduGroup().name())).append(" : ");
		buf.append(WOMessage.stringByEscapingHTMLString(course.cycle().subject()));
		/*
		if(course.comment() != null) {
			buf.append(" <span style = \"font-style:italic;\">(");
			buf.append(WOMessage.stringByEscapingHTMLString(course.comment())).append(")</span>");
		} */
		return buf.toString();
	}
	
	public void setNextTeacher(Object teacher) {
		if(teacher == null || teacher == course.teacher())
			return;
		EOEnterpriseObject tc = EOUtilities.createAndInsertInstance(course.editingContext(),
				"TeacherChange");
		tc.takeValueForKey(nextDate, "date");
		tc.takeValueForKey(nextComment, "comment");
		tc.takeValueForKey(course.teacher(), "teacher");
        nextDate = (NSTimestamp)session().valueForKey("today");
        nextComment = null;
        course.addObjectToBothSidesOfRelationshipWithKey(tc, BaseCourse.TEACHER_CHANGES_KEY);
		if(teacher instanceof Teacher) {
			course.setTeacher((Teacher)teacher);
		} else {
			course.setTeacher(null);
		}
    	changes = course.teacherChanges();
    	if(changes != null && changes.count() > 1)
    		changes = EOSortOrdering.sortedArrayUsingKeyOrderArray(changes, MyUtility.dateSorter);
	}
	
	public WOComponent chooseCourseTeacher() {
		WOComponent result = TeacherSelector.selectorPopup(this, "teacher",
				course.editingContext());
		result.takeValueForKeyPath("ajaxPopup", "dict.onCancel");
//		result.takeValueForKey(course.teacher(), "value");
		result.takeValueForKeyPath(Boolean.TRUE, "dict.presenterBindings.ajaxReturn");
		return result;
	}
	
	public WOComponent chooseNextTeacher() {
		WOComponent result = TeacherSelector.selectorPopup(this, "nextTeacher",
				course.editingContext());
		result.takeValueForKeyPath("ajaxPopup", "dict.onCancel");
		result.takeValueForKeyPath(Boolean.TRUE, "dict.presenterBindings.ajaxReturn");
		return result;
	}
	
	public WOComponent chooseChangeTeacher() {
		WOComponent result = TeacherSelector.selectorPopup(this, "teacher",
				course.editingContext());
//		result.takeValueForKey(changeItem.valueForKey("teacher"), "value");
		result.takeValueForKey(new Getter(changeItem), "resultGetter");
		result.takeValueForKeyPath("ajaxPopup", "dict.onCancel");
		result.takeValueForKeyPath(Boolean.TRUE, "dict.presenterBindings.ajaxReturn");
		result.takeValueForKeyPath(session().valueForKeyPath("readAccess.delete.TeacherChange")
				, "dict.presenterBindings.allowDelete");
		return result;
	}
	
	public String changeTeacherClass() {
		EOEnterpriseObject obj = (changeItem==null)?course:changeItem;
		Boolean sex = (Boolean)obj.valueForKeyPath("teacher.person.sex");
		if(sex == null)
			return "grey";
		if(sex.booleanValue())
			return "male";
		else
			return "female";
	}

	public WOActionResults save() {
		course.namedFlags().setFlagForKey((changes.count() > 0), "teacherChanged");
		if(course.editingContext().hasChanges()) {
			try {
				course.editingContext().saveChanges();
				logger.log(WOLogLevel.EDITING,"Saved changes in course",
						new Object[] {session(),course});
			} catch (Exception e) {
				logger.log(WOLogLevel.WARNING,"Error saving course changes",
						new Object[] {session(),course,e});
				session().takeValueForKey(e.getMessage(), "message");
				course.editingContext().revert();
			}
			if(returnPage.name().endsWith("LessonNoteEditor"))
				returnPage.valueForKey("refresh");
		}
		returnPage.ensureAwakeInContext(context());
		return returnPage;
	}
	
	public WOActionResults cancel() {
		course.editingContext().revert();
		returnPage.ensureAwakeInContext(context());
		return returnPage;
	}
	
	public Object cantDelete() {
		if(updater == null)
			return Boolean.TRUE;
		return session().valueForKeyPath("readAccess._delete.course");
	}
	
	public WOActionResults delete() {
		session().setObjectForKey(course, "deleteCourse");
		NSArray modules = (NSArray)session().valueForKeyPath("modules.deleteCourse");
		EOEditingContext ec =course.editingContext();
		returnPage.ensureAwakeInContext(context());
		if(modules != null && modules.count() > 0) {
			logger.log(WOLogLevel.INFO,"Could not delete EduCourse",
					new Object[] {session(),course,modules});
		} else {
			try {
				EduCycle cycle = course.cycle();
				ec.deleteObject(course);
				ec.saveChanges();
				updater.update();
				logger.log(WOLogLevel.COREDATA_EDITING,"Deleted EduCourse",
						new Object[] {session(),cycle});
			} catch (Exception e) {
				ec.revert();
				logger.log(WOLogLevel.WARNING,"Error deleting EduCourse",
						new Object[] {session(),course,e});
				session().takeValueForKey(e.getMessage(), "message");
			}
		}
		session().removeObjectForKey("deleteCourse");
		return returnPage;
	}	
	
	public String crossOnclick() {
		if(course.editingContext().hasChanges()) {
			StringBuilder buf = new StringBuilder("changed=true;checkRun('");
			buf.append(context().componentActionURL()).append("');");
			return buf.toString();
		}	
		return "closePopup();";
	}
	
	public String tbodyStyle() {
		if(changes == null || changes.count() == 0)
			return "display:none;";
		return null;
	}
	
	public Object teacher() {
		EOEnterpriseObject obj = (changeItem==null)?course:changeItem;
		obj = (EOEnterpriseObject)obj.valueForKey("teacher");
		if(obj == null)
			return NullValue;
		return obj;
	}
	
	public void setTeacher(Object teacher) {
		if(teacher instanceof Null)
			teacher = null;
		if(teacher instanceof Teacher) {
			if(course.teacher() != teacher)
				course.setTeacher((Teacher)teacher);
		} else {
			course.setTeacher(null);
		}
	}
	
	protected class Getter {
		protected EOEnterpriseObject change;
		public Getter(EOEnterpriseObject ch) {
			change = ch;
		}
		
		public void setTeacher(Object teacher) {
			if(teacher == null) {
				course.removeObjectFromBothSidesOfRelationshipWithKey(change,
						BaseCourse.TEACHER_CHANGES_KEY);
				change.editingContext().deleteObject(change);
				changes = course.teacherChanges();
				if(changes != null && changes.count() > 1)
					changes = EOSortOrdering.sortedArrayUsingKeyOrderArray(
							changes,MyUtility.dateSorter);
				return;
			}
			if(teacher instanceof Null)
				teacher = null;
			if(teacher != change.valueForKey("teacher"))
				change.takeValueForKey(teacher, "teacher");
		}
		
		public Object teacher() {
			Object teacher = change.valueForKey("teacher");
			if(teacher == null)
				teacher = NullValue;
			return teacher;
		}
	}
	
	public static interface Updater {
		public void update();
	}
 }