package net.rujel.curriculum;

import net.rujel.interfaces.EduCourse;
import net.rujel.interfaces.EduLesson;
import net.rujel.interfaces.Person;
import net.rujel.reusables.NamedFlags;
import net.rujel.reusables.SettingsReader;
import net.rujel.reusables.WOLogLevel;
import net.rujel.ui.RedirectPopup;

import com.webobjects.appserver.WOActionResults;
import com.webobjects.appserver.WOContext;
import com.webobjects.appserver.WOComponent;
import com.webobjects.eoaccess.EOUtilities;
import com.webobjects.eocontrol.*;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSKeyValueCoding;
import com.webobjects.foundation.NSTimestamp;

public class EditVarSub extends WOComponent {
	
	public NSTimestamp date;
    public WOComponent returnPage;
    public EduCourse toCourse;
    public EduCourse fromCourse;
    protected EduLesson lesson;
    public NSArray courses;
    public EduCourse item;
    public Reason reason;
    protected Variation variation;
    public Boolean cantSave = Boolean.TRUE;
	public NamedFlags access;
    
    public EditVarSub(WOContext context) {
        super(context);
    }
    
    public void setLesson(EduLesson lesson) {
    	this.lesson = lesson;
    	if(lesson == null)
    		return;
    	date = lesson.date();
    	toCourse = lesson.course();
    	EOQualifier[] quals = new EOQualifier[3];
       	quals[0] = new EOKeyValueQualifier("relatedLesson",
    			EOQualifier.QualifierOperatorEqual, lesson);
    	quals[1] = new EOKeyValueQualifier(Variation.VALUE_KEY,
    		EOQualifier.QualifierOperatorGreaterThan, new Integer(0));
       	quals[2] = new EOKeyValueQualifier("course",
    			EOQualifier.QualifierOperatorEqual, toCourse);
       	quals[0] = new EOAndQualifier(new NSArray(quals));
       	EOFetchSpecification fs = new EOFetchSpecification(
       			Variation.ENTITY_NAME,quals[0],null);
       	NSArray found = lesson.editingContext().objectsWithFetchSpecification(fs);
       	if(found != null && found.count() > 0) {
       		setVariation((Variation)found.objectAtIndex(0));
       	} else {
       		access = (NamedFlags)session().valueForKeyPath("readAccess.FLAGS.Variation");
       		access = access.mutableClone();
       		access.setFlagForKey(false, "delete");
       		access.setFlagForKey(access.flagForKey("create"), "edit");
       	}
    }
    
    public void setVariation(Object var) {
    	if(var instanceof Variation) {
    		variation = (Variation)var;
    	} else {
    		setLesson((EduLesson)NSKeyValueCoding.Utility.valueForKey(var,"lesson"));
    		cantSave = Boolean.FALSE;
    		return;
    	}
		reason = variation.reason();
   		Variation back = variation.getPaired();
		if(back != null) {
			fromCourse = back.course();
		}
		if(lesson == null) {
			lesson = variation.relatedLesson();
			toCourse = variation.course();
			date = variation.date();
		}
		cantSave = Boolean.FALSE;
		session().setObjectForKey(variation, "readAccess");
		access = (NamedFlags)session().valueForKeyPath("readAccess.FLAGS.session");
		session().removeObjectForKey("readAccess");
    }
    
    public void prepareCourses() {
    	if(toCourse == null)
    		return;
    	EOQualifier[] quals = new EOQualifier[3];
    	quals[0] = new EOKeyValueQualifier("eduYear",
    			EOQualifier.QualifierOperatorEqual, toCourse.eduYear());
    	quals[1] = new EOKeyValueQualifier("eduGroup",
    			EOQualifier.QualifierOperatorEqual, toCourse.eduGroup());
    	quals[2] = new EOKeyValueQualifier("cycle",
    			EOQualifier.QualifierOperatorNotEqual, toCourse.cycle());
    	quals[0] = new EOAndQualifier(new NSArray(quals));
    	EOFetchSpecification fs = new EOFetchSpecification(EduCourse.entityName
    			, quals[0], null);
    	courses = toCourse.editingContext().objectsWithFetchSpecification(fs);
//    	quals[1] = new EOKeyValueQualifier("cycle.school",
//    			EOQualifier.QualifierOperatorEqual, session().valueForKey("school"));
//    	courses = EOQualifier.filteredArrayWithQualifier(courses, quals[1]);
    	courses = EOSortOrdering.sortedArrayUsingKeyOrderArray(courses, EduCourse.sorter);
    	if(fromCourse != null && cantSave != null && !cantSave.booleanValue()) {
    		cantSave = null;
    	} else if(variation != null && fromCourse == null
    			&& !access.flagForKey("edit") && access.flagForKey("create")) {
    		access = access.mutableClone();
    		access.setFlagForKey(true, "edit");
    	}
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
    	Variation back = chooseBack(found);
    	if(back != null)
    		reason = back.reason();
    	cantSave = Boolean.FALSE;
    }
    
    public void selectNoCourse() {
    	fromCourse = null;
    	reason = null;
    	cantSave = null;
    }
    
    public String onDelete() {
		String href = context().componentActionURL();
		return "if(confirmAction(this.value,event))ajaxPopupAction('" + href + "');";
    }

    public WOActionResults done() {
    	return done(false);
    }

    public WOActionResults done(boolean hasChanges) {
    	if(hasChanges)
			session().removeObjectForKey("lessonProperties");
    	if(returnPage instanceof VariationsList) {
    		returnPage.ensureAwakeInContext(context());
    		if(hasChanges) {
    			returnPage.takeValueForKey(Boolean.TRUE,"hasChanges");
    			returnPage.takeValueForKey(null,"planFact");
    		}
    		return returnPage;
    	}
       	return RedirectPopup.getRedirect(context(), returnPage);
    }
    
    public WeekFootprint weekFootprint() {
		WeekFootprint footprint = null;
		try {
			footprint = (WeekFootprint)returnPage.valueForKey("weekFootprint");
		} catch (Exception e) {
		}
		return footprint;
    }

    public WOActionResults save() {
		if(reason == null) {
    		session().takeValueForKey(application().valueForKeyPath(
    			"strings.RujelCurriculum_Curriculum.messages.reasonRequired"), "message");
    		return this;
		}
		EOEditingContext ec = toCourse.editingContext();
		if(variation == null)
			variation = (Variation)EOUtilities.createAndInsertInstance(ec, Variation.ENTITY_NAME);
		variation.setRelatedLesson(lesson);
		variation.addObjectToBothSidesOfRelationshipWithKey(reason, Variation.REASON_KEY);
//		variation.setDate(date);
		variation.setValue(new Integer(1));
		boolean noRelieve = SettingsReader.boolForKeyPath("edu.disablePlanFactCheck", false);
		WeekFootprint weekFootprint = weekFootprint();
		boolean changed = ec.hasChanges();
		if(changed) {
		try {
			ec.saveChanges();
			if(weekFootprint != null) weekFootprint.reset();
	       	Curriculum.logger.log(WOLogLevel.EDITING,"Positive Variation saved",
	       			new Object[] {session(),variation});
			if(!noRelieve) {
				String usr = (String)session().valueForKeyPath("user.present");
				if(usr == null)
					usr = "??" + Person.Utility.fullName(
							toCourse.teacher(), true, 2, 1, 1);
				Reprimand.autoRelieve(toCourse, date, usr, weekFootprint);
			}
		} catch (Exception e) {
			ec.revert();
			session().takeValueForKey(e.getMessage(), "message");
			Curriculum.logger.log(WOLogLevel.WARNING,"Error saving positive Variation",
					new Object[] {session(),variation,toCourse});
			return done(true);
		}
		}
    	EOQualifier[] quals = new EOQualifier[3];
       	quals[0] = new EOKeyValueQualifier("relatedLesson",
    			EOQualifier.QualifierOperatorEqual, lesson);
    	quals[1] = new EOKeyValueQualifier(Variation.DATE_KEY,
    			EOQualifier.QualifierOperatorEqual, date);
    	quals[2] = new EOKeyValueQualifier(Variation.VALUE_KEY,
    		EOQualifier.QualifierOperatorLessThan, new Integer(0));
//    	quals[3] = new EOKeyValueQualifier(Variation.REASON_KEY,
//    			EOQualifier.QualifierOperatorEqual, reason);
       	quals[0] = new EOAndQualifier(new NSArray(quals));
       	EOFetchSpecification fs = new EOFetchSpecification(
       			Variation.ENTITY_NAME,quals[0],null);
       	NSArray found = ec.objectsWithFetchSpecification(fs);
       	if(lesson != null && found != null && found.count() > 0) {
   			boolean gotcha = false;
       		for (int i = 0; i < found.count(); i++) {
				Variation var = (Variation)found.objectAtIndex(i);
				if(var.course() != fromCourse) {
					var.setRelatedLesson(null);
					Curriculum.logger.log(WOLogLevel.EDITING,"Detaching variation from lesson",
							new Object[] {session(),var});
				} else {
					gotcha = true;
				}
       		}
       		if(gotcha) {
       			if(ec.hasChanges()) {
       				try {
						ec.saveChanges();
					} catch (Exception e) {
		    			ec.revert();
		    			session().takeValueForKey(e.getMessage(), "message");
		    			Curriculum.logger.log(WOLogLevel.WARNING,
		    					"Error Detaching Variations from lesson",
		    					new Object[] {session(),lesson});
		    			return done(changed);
					}
       			}
       			return done(changed);
       		}
       	}
		if(fromCourse == null)
			return done(changed);
       	quals[0] = new EOKeyValueQualifier("course",
    			EOQualifier.QualifierOperatorEqual, fromCourse);
       	fs.setQualifier(new EOAndQualifier(new NSArray(quals)));
       	found = ec.objectsWithFetchSpecification(fs);
       	Variation back = chooseBack(found);
       	if(back == null) {
       		back = (Variation)EOUtilities.createAndInsertInstance(ec, Variation.ENTITY_NAME);
    		back.addObjectToBothSidesOfRelationshipWithKey(fromCourse, "course");
    		back.addObjectToBothSidesOfRelationshipWithKey(reason, Variation.REASON_KEY);
    		back.setValue(new Integer(-1));
       	} else {
       		if(back.relatedLesson() == lesson)
       			return done(changed);
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
    	       	Curriculum.logger.log(WOLogLevel.EDITING,"VarSub reverse Variation created",
    	       			new Object[] {session(),back});
    	       	if(weekFootprint != null) weekFootprint.reset();
    			if(!noRelieve) {
    				String usr = (String)session().valueForKeyPath("user.present");
    				if(usr == null)
    					usr = "??" + Person.Utility.fullName(
    							toCourse.teacher(), true, 2, 1, 1);
    				Reprimand.autoRelieve(fromCourse, date, usr, null);
    			}
    		} catch (Exception e) {
    			ec.revert();
    			session().takeValueForKey(e.getMessage(), "message");
    			Curriculum.logger.log(WOLogLevel.WARNING, "Error saving reverse VarSub Variation",
    					new Object[] {session(),back,variation});
    			return done(changed);
    		}
       	}
       	session().removeObjectForKey("lessonProperties");
		return done(changed);
	}
	
    protected Variation chooseBack(NSArray found) {
    	if(found == null || found.count() == 0)
    		return null;
   		int lvl = 0;
   		Variation back = null;
   		for (int i = 0; i < found.count(); i++) {
			Variation var = (Variation)found.objectAtIndex(i);
			EduLesson lsn = var.relatedLesson(); 
			if(lsn == lesson) {
		       	session().removeObjectForKey("lessonProperties");
				return var;
			} else if(lsn != null && lsn.date() != null) {
				continue;
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
   		return back;
    }
    
	public String listStyle() {
		if(courses != null && (fromCourse == null ^ cantSave == null))
			return null;
		return "display:none;";
	}
	
	public String selectOnClick() {
		if(courses == null)
			return (String)session().valueForKey("ajaxPopupNoPos");
		return "hideObj('curCourse');showObj('selectCourse');fitWindow();";
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
	
	public EduCourse reasonCourse() {
		if(fromCourse == null)
			return toCourse;
		return fromCourse;
	}
	
    public WOActionResults goToRelated() {
		WOComponent nextPage = pageWithName("LessonNoteEditor");
		nextPage.takeValueForKey(fromCourse,"course");
		WOComponent toPush = returnPage;
		if(returnPage instanceof VariationsList)
			toPush = (WOComponent)returnPage.valueForKey("returnPage");
		session().takeValueForKey(toPush,"pushComponent");
		return nextPage;
    }
    
    public WOActionResults delete() {
    	EOEditingContext ec = toCourse.editingContext();
    	if(lesson != null && lesson.editingContext() != null) {
    	EOQualifier[] quals = new EOQualifier[2];
       	quals[0] = new EOKeyValueQualifier("relatedLesson",
    			EOQualifier.QualifierOperatorEqual, lesson);
    	quals[1] = new EOKeyValueQualifier(Variation.VALUE_KEY,
    		EOQualifier.QualifierOperatorLessThan, new Integer(0));
       	quals[0] = new EOAndQualifier(new NSArray(quals));
       	EOFetchSpecification fs = new EOFetchSpecification(
       			Variation.ENTITY_NAME,quals[0],null);
       	NSArray found = ec.objectsWithFetchSpecification(fs);
       	if(found != null && found.count() > 0) {
       		for (int i = 0; i < found.count(); i++) {
				Variation var = (Variation)found.objectAtIndex(i);
				var.setRelatedLesson(null);
				Curriculum.logger.log(WOLogLevel.EDITING,"Detaching variation from lesson",
						new Object[] {session(),var});
       		}
       	}
    	}
    	ec.deleteObject(variation);
    	try {
			ec.saveChanges();
	       	Curriculum.logger.log(WOLogLevel.EDITING,"Positive Variation deleted",
	       			new Object[] {session(),lesson});
			WeekFootprint weekFootprint = weekFootprint();
			if(weekFootprint != null) weekFootprint.reset();
			boolean noRelieve = SettingsReader.boolForKeyPath("edu.disablePlanFactCheck", false);
			if(!noRelieve) {
				String usr = (String)session().valueForKeyPath("user.present");
				if(usr == null)
					usr = "??" + Person.Utility.fullName(
							toCourse.teacher(), true, 2, 1, 1);
				Reprimand.autoRelieve(toCourse, date, usr, weekFootprint);
			}
		} catch (Exception e) {
			ec.revert();
			session().takeValueForKey(e.getMessage(), "message");
			Curriculum.logger.log(WOLogLevel.WARNING,"Error deleting positive Variation",
					new Object[] {session(),variation,toCourse});
			return done(true);
		}
    	return done(true);
    }
    
}