package net.rujel.curriculum;

import net.rujel.interfaces.EduCourse;
import net.rujel.interfaces.EduLesson;
import net.rujel.interfaces.Person;
import net.rujel.reusables.SettingsReader;
import net.rujel.reusables.WOLogLevel;

import com.webobjects.appserver.WOActionResults;
import com.webobjects.appserver.WOContext;
import com.webobjects.appserver.WOComponent;
import com.webobjects.eoaccess.EOUtilities;
import com.webobjects.eocontrol.*;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSTimestamp;

public class EditVarSub extends WOComponent {
	
	public NSTimestamp date;
    public WOComponent returnPage;
    public EduCourse toCourse;
    public EduCourse fromCourse;
    protected EduLesson lesson;
//    public Integer value = new Integer(1);
    public NSArray courses;
    public EduCourse item;
    public Reason reason;
//    protected NSArray backVars;
    protected Variation variation;
    
    public EditVarSub(WOContext context) {
        super(context);
    }
    
    public void setLesson(EduLesson lesson) {
    	this.lesson = lesson;
    	if(lesson == null)
    		return;
    	date = lesson.date();
    	setToCourse(lesson.course());
    	EOQualifier[] quals = new EOQualifier[4];
    	quals[0] = new EOKeyValueQualifier(Variation.DATE_KEY,
    			EOQualifier.QualifierOperatorEqual, date);
    	quals[1] = new EOKeyValueQualifier(Variation.VALUE_KEY,
    		EOQualifier.QualifierOperatorGreaterThan, new Integer(0));
       	quals[2] = new EOKeyValueQualifier("course",
    			EOQualifier.QualifierOperatorEqual, toCourse);
       	quals[3] = new EOKeyValueQualifier("relatedLesson",
    			EOQualifier.QualifierOperatorEqual, lesson);
       	quals[0] = new EOAndQualifier(new NSArray(quals));
       	EOFetchSpecification fs = new EOFetchSpecification(
       			Variation.ENTITY_NAME,quals[0],null);
       	NSArray found = lesson.editingContext().objectsWithFetchSpecification(fs);
       	if(found != null && found.count() > 0) {
       		variation = (Variation)found.objectAtIndex(0);
			reason = variation.reason();
//			value = variation.value();
       		Variation back = variation.getPaired();
			if(back != null)
				fromCourse = back.course();
       	}
    }
    
    public void setToCourse(EduCourse course) {
    	toCourse = course;
    	if(course == null)
    		return;
    	EOQualifier[] quals = new EOQualifier[3];
    	quals[0] = new EOKeyValueQualifier("eduYear",
    			EOQualifier.QualifierOperatorEqual, course.eduYear());
    	quals[1] = new EOKeyValueQualifier("eduGroup",
    			EOQualifier.QualifierOperatorEqual, course.eduGroup());
    	quals[2] = new EOKeyValueQualifier("cycle",
    			EOQualifier.QualifierOperatorNotEqual, course.cycle());
    	quals[0] = new EOAndQualifier(new NSArray(quals));
    	EOFetchSpecification fs = new EOFetchSpecification(EduCourse.entityName
    			, quals[0], null);
    	courses = course.editingContext().objectsWithFetchSpecification(fs);
    	quals[1] = new EOKeyValueQualifier("cycle.school",
    			EOQualifier.QualifierOperatorEqual, session().valueForKey("school"));
    	courses = EOQualifier.filteredArrayWithQualifier(courses, quals[1]);
    	courses = EOSortOrdering.sortedArrayUsingKeyOrderArray(courses, EduCourse.sorter);
    }

    public void selectCourse() {
    	fromCourse = item;
    	EOQualifier[] quals = new EOQualifier[3];
    	quals[0] = new EOKeyValueQualifier("course",
    			EOQualifier.QualifierOperatorEqual, item);
    	quals[1] = new EOKeyValueQualifier(Variation.DATE_KEY,
    			EOQualifier.QualifierOperatorEqual, date);
    	quals[2] = new EOKeyValueQualifier(Variation.VALUE_KEY,
    			EOQualifier.QualifierOperatorLessThan, new Integer(0));
    	quals[1] = new EOAndQualifier(new NSArray(quals));
    	EOFetchSpecification fs = new EOFetchSpecification(
    			Variation.ENTITY_NAME,quals[1],null);
    	NSArray found = fromCourse.editingContext().objectsWithFetchSpecification(fs);
    	reason = null;
    	if(found != null && found.count() > 0) {
    		for (int i = 0; i < found.count(); i++) {
        		Variation var = (Variation)found.objectAtIndex(0);
        		if(var.relatedLesson() == null || var.relatedLesson().course() == toCourse) {
        			reason = var.reason();
        			if(var.relatedLesson() != null)
        				break;
        		}
			}
    	}
    }
    
	public WOActionResults save() {
		EOEditingContext ec = toCourse.editingContext();
		returnPage.ensureAwakeInContext(context());
		if(reason == null) {
			ec.revert();
			return returnPage;
		}

		if(variation == null)
			variation = (Variation)EOUtilities.createAndInsertInstance(ec, Variation.ENTITY_NAME);
		variation.setRelatedLesson(lesson);
		variation.addObjectToBothSidesOfRelationshipWithKey(reason, Variation.REASON_KEY);
//		variation.setDate(date);
//		variation.setValue(new Integer(-1));
		boolean noRelieve = Boolean.getBoolean("PlanFactCheck.disable")
				|| SettingsReader.boolForKeyPath("edu.disablePlanFactCheck", false);
		try {
			ec.saveChanges();
	       	Curriculum.logger.log(WOLogLevel.EDITING,"VarSub Variation saved",
	       			new Object[] {session(),variation});
			if(!noRelieve) {
				String usr = (String)session().valueForKeyPath("user.present");
				if(usr == null)
					usr = "??" + Person.Utility.fullName(
							toCourse.teacher(), true, 2, 1, 1);
				Reprimand.autoRelieve(toCourse, date, usr);
			}
		} catch (Exception e) {
			ec.revert();
			session().takeValueForKey(e.getMessage(), "message");
			Curriculum.logger.log(WOLogLevel.WARNING,"Error saving VarSub Variation",
					new Object[] {session(),variation,toCourse});
			return returnPage;
		}
    	EOQualifier[] quals = new EOQualifier[3];
       	quals[0] = new EOKeyValueQualifier("course",
    			EOQualifier.QualifierOperatorEqual, fromCourse);
    	quals[1] = new EOKeyValueQualifier(Variation.DATE_KEY,
    			EOQualifier.QualifierOperatorEqual, date);
    	quals[2] = new EOKeyValueQualifier(Variation.VALUE_KEY,
    		EOQualifier.QualifierOperatorLessThan, new Integer(0));
//    	quals[3] = new EOKeyValueQualifier(Variation.REASON_KEY,
//    			EOQualifier.QualifierOperatorEqual, reason);
       	quals[1] = new EOAndQualifier(new NSArray(quals));
       	EOFetchSpecification fs = new EOFetchSpecification(
       			Variation.ENTITY_NAME,quals[1],null);
       	NSArray found = fromCourse.editingContext().objectsWithFetchSpecification(fs);
       	Variation back = null;
       	if(found == null || found.count() == 0) {
       		back = (Variation)EOUtilities.createAndInsertInstance(ec, Variation.ENTITY_NAME);
    		back.addObjectToBothSidesOfRelationshipWithKey(fromCourse, "course");
    		back.addObjectToBothSidesOfRelationshipWithKey(reason, Variation.REASON_KEY);
       	} else {
       		int lvl = 0;
       		for (int i = 0; i < found.count(); i++) {
				Variation var = (Variation)found.objectAtIndex(i);
				if(var.relatedLesson() == lesson) {
			       	session().removeObjectForKey("lessonProperies");
					return returnPage;
				}
				if(var.reason() == reason && lvl < 4) {
					lvl = 3;
					back = var;
					if(var.value().intValue() == -1)
						lvl++;
				} else if(var.value().intValue() == -1 && lvl < 2) {
					lvl = 2;
					back = var;
				} else if(lvl == 0) {
					lvl = 1;
					back = var;
				}
			}
       		if(back.value().intValue() < -1) {
       			back.setValue(new Integer(back.value().intValue() +1));
           		back = (Variation)EOUtilities.createAndInsertInstance(ec, Variation.ENTITY_NAME);
        		back.addObjectToBothSidesOfRelationshipWithKey(fromCourse, "course");
        		back.addObjectToBothSidesOfRelationshipWithKey(reason, Variation.REASON_KEY);
       		}
       	}
       	back.setRelatedLesson(lesson);
       	if(ec.hasChanges()) {
    		try {
    			ec.saveChanges();
    	       	Curriculum.logger.log(WOLogLevel.EDITING,
    	       			"VarSub reverse Variation created",
    	       			new Object[] {session(),back});
    			if(!noRelieve) {
    				String usr = (String)session().valueForKeyPath("user.present");
    				if(usr == null)
    					usr = "??" + Person.Utility.fullName(
    							toCourse.teacher(), true, 2, 1, 1);
    				Reprimand.autoRelieve(fromCourse, date, usr);
    			}
    		} catch (Exception e) {
    			ec.revert();
    			session().takeValueForKey(e.getMessage(), "message");
    			Curriculum.logger.log(WOLogLevel.WARNING,
    					"Error saving reverse VarSub Variation",
    					new Object[] {session(),back,variation});
    			return returnPage;
    		}
       	}
       	session().removeObjectForKey("lessonProperies");
		return returnPage;
	}
	
	public String listStyle() {
		if(fromCourse == null)
			return null;
		return "display:none;";
	}
	
	public String rowOnClick() {
		if(item == fromCourse)
			return "hideObj('selectCourse');showObj('curCourse');fitWindow();";
		return (String)session().valueForKey("ajaxPopupNoPos");
	}
	
	public String rowClass() {
		if(item == fromCourse)
			return "selection";
		return "green";
	}
}