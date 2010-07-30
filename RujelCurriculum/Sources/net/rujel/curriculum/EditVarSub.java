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
import com.webobjects.eocontrol.EOAndQualifier;
import com.webobjects.eocontrol.EOEditingContext;
import com.webobjects.eocontrol.EOFetchSpecification;
import com.webobjects.eocontrol.EOKeyValueQualifier;
import com.webobjects.eocontrol.EOQualifier;
import com.webobjects.eocontrol.EOSortOrdering;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSTimestamp;

public class EditVarSub extends WOComponent {
	
	public NSTimestamp date;
    public WOComponent returnPage;
    public EduCourse toCourse;
    public EduCourse fromCourse;
    public Integer value = new Integer(1);
    public NSArray courses;
    public EduCourse item;
    public Reason reason;
//    protected NSArray backVars;
    protected Variation variation;
    
    public EditVarSub(WOContext context) {
        super(context);
    }
    
    public void setLesson(EduLesson lesson) {
    	if(lesson == null)
    		return;
    	date = lesson.date();
    	setToCourse(lesson.course());
    	EOQualifier[] quals = new EOQualifier[3];
    	quals[0] = new EOKeyValueQualifier(Variation.DATE_KEY,
    			EOQualifier.QualifierOperatorEqual, date);
    	quals[1] = new EOKeyValueQualifier(Variation.VALUE_KEY,
    		EOQualifier.QualifierOperatorGreaterThan, new Integer(0));
       	quals[2] = new EOKeyValueQualifier("course",
    			EOQualifier.QualifierOperatorEqual, toCourse);
       	quals[0] = new EOAndQualifier(new NSArray(quals));
       	EOFetchSpecification fs = new EOFetchSpecification(
       			Variation.ENTITY_NAME,quals[0],null);
       	NSArray found = lesson.editingContext().objectsWithFetchSpecification(fs);
       	if(found != null && found.count() > 0) {
       		for (int i = 0; i < found.count(); i++) {
				Variation var = (Variation)found.objectAtIndex(i);
				NSArray backVars = var.getAllPaired(true);
				if(backVars != null) {
					if(backVars.count() == 0) {
						backVars = null;
						continue;
					}
					variation = var;
					var = (Variation)backVars.objectAtIndex(0);
					fromCourse = var.course();
					reason = var.reason();
					value = variation.value();
					break;
				}
			}
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
    	/*
       	if(variation != null && backVars != null) {
       		backVars = EOQualifier.filteredArrayWithQualifier(backVars, quals[0]);
       		if(backVars == null || backVars.count() == 0)
       			variation = null;
       		else
       			return;
       	}*/
    	variation = null;
    	quals[1] = new EOKeyValueQualifier(Variation.DATE_KEY,
    			EOQualifier.QualifierOperatorEqual, date);
    	quals[2] = new EOKeyValueQualifier(Variation.VALUE_KEY,
    			EOQualifier.QualifierOperatorLessThan, new Integer(0));
    	quals[1] = new EOAndQualifier(new NSArray(quals));
    	EOFetchSpecification fs = new EOFetchSpecification(
    			Variation.ENTITY_NAME,quals[1],null);
    	NSArray found = fromCourse.editingContext().objectsWithFetchSpecification(fs);
    	if(found != null && found.count() > 0) {
//    		backVars = found;
    		Variation var = (Variation)found.objectAtIndex(0);
    		reason = var.reason();
    	} else {
//    		backVars = null;
    		reason = null;
    	}
    }
    
	public WOActionResults save() {
		EOEditingContext ec = toCourse.editingContext();
		returnPage.ensureAwakeInContext(context());
		if(reason == null) {
			ec.revert();
			return returnPage;
		}
		int val = (value == null)?1:value.intValue();
		if(val < 0) {
			val = -val;
			value = new Integer(val); 
		}
		if(variation == null)
		variation = (Variation)EOUtilities.createAndInsertInstance(
				ec, Variation.ENTITY_NAME);
		variation.addObjectToBothSidesOfRelationshipWithKey(toCourse, "course");
		variation.addObjectToBothSidesOfRelationshipWithKey(reason, Variation.REASON_KEY);
		variation.setDate(date);
		variation.setValue(value);
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
    	EOQualifier[] quals = new EOQualifier[4];
       	quals[0] = new EOKeyValueQualifier("course",
    			EOQualifier.QualifierOperatorEqual, fromCourse);
    	quals[1] = new EOKeyValueQualifier(Variation.DATE_KEY,
    			EOQualifier.QualifierOperatorEqual, date);
    	quals[2] = new EOKeyValueQualifier(Variation.VALUE_KEY,
    		EOQualifier.QualifierOperatorLessThan, new Integer(0));
    	quals[3] = new EOKeyValueQualifier(Variation.REASON_KEY,
    			EOQualifier.QualifierOperatorEqual, reason);
       	quals[1] = new EOAndQualifier(new NSArray(quals));
       	EOFetchSpecification fs = new EOFetchSpecification(
       			Variation.ENTITY_NAME,quals[1],null);
       	NSArray found = fromCourse.editingContext().objectsWithFetchSpecification(fs);
       	if(found == null || found.count() == 0) {
       		Variation var = (Variation)EOUtilities.createAndInsertInstance(
    				ec, Variation.ENTITY_NAME);
    		var.addObjectToBothSidesOfRelationshipWithKey(fromCourse, "course");
    		var.addObjectToBothSidesOfRelationshipWithKey(reason, Variation.REASON_KEY);
    		var.setDate(date);
    		var.setValue(new Integer(-val));
    		try {
    			ec.saveChanges();
    	       	Curriculum.logger.log(WOLogLevel.EDITING,
    	       			"VarSub reverse Variation created",
    	       			new Object[] {session(),var});
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
    					new Object[] {session(),var,variation});
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