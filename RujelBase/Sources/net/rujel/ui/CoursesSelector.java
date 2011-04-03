package net.rujel.ui;

import java.util.Enumeration;

import net.rujel.interfaces.EduCourse;
import net.rujel.interfaces.EduCycle;
import net.rujel.interfaces.EduGroup;
import net.rujel.interfaces.Student;
import net.rujel.interfaces.Teacher;
import net.rujel.reusables.Various;

import com.webobjects.appserver.WOContext;
import com.webobjects.appserver.WOComponent;
import com.webobjects.appserver.WOActionResults;
import com.webobjects.appserver.WOResponse;
import com.webobjects.eoaccess.EOUtilities;
import com.webobjects.eocontrol.EOAndQualifier;
import com.webobjects.eocontrol.EOEditingContext;
import com.webobjects.eocontrol.EOEnterpriseObject;
import com.webobjects.eocontrol.EOFetchSpecification;
import com.webobjects.eocontrol.EOGlobalID;
import com.webobjects.eocontrol.EOKeyGlobalID;
import com.webobjects.eocontrol.EOKeyValueQualifier;
import com.webobjects.eocontrol.EOOrQualifier;
import com.webobjects.eocontrol.EOQualifier;
import com.webobjects.eocontrol.EOSortOrdering;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSMutableArray;
import com.webobjects.foundation.NSMutableDictionary;

public class CoursesSelector extends WOComponent {
    public CoursesSelector(WOContext context) {
        super(context);
    }
    
    public static final int CLASS_TAB = 0;
    public static final int SUBJECT_TAB = 1;
    public static final int TEACHER_TAB = 2;
    
    public int currTab = -1;
    public Object selection;

    public void appendToResponse(WOResponse aResponse, WOContext aContext) {
		Integer sesTab = (Integer)session().valueForKeyPath("state.courseSelector");
//		if(currTab < 0) {
			if(sesTab != null) {
				Object newSelection = null;
				if(hasBinding("selection")) {
					newSelection = valueForBinding("selection");
				}
				if(currTab != sesTab.intValue()) {
					newSelection = session().valueForKeyPath("state.coursesSelection");
					if(newSelection instanceof EOGlobalID) {
						String ent = ((EOKeyGlobalID)newSelection).entityName();
						if((sesTab.intValue() == CLASS_TAB)?ent.equals(EduGroup.entityName):
							(sesTab.intValue() == TEACHER_TAB)?ent.equals(Teacher.entityName):
								ent.startsWith("Subject")) {
							EOEditingContext ec = (EOEditingContext)valueForBinding("ec");
							newSelection = ec.faultForGlobalID((EOGlobalID)newSelection, ec);
						} else {
							newSelection = null;
						}
					}
				}
				if(currTab != sesTab.intValue() ||
						((newSelection != null || hasBinding("selection")) 
						&& selection != newSelection)) {
					currTab = sesTab.intValue();
					setSelection(newSelection);
				}
			} else if(currTab < 0) {
				currTab = 0;
				clean();
			}/*
		} else if(sesTab != null && sesTab.intValue() != currTab) {
			currTab = sesTab.intValue();
			clean();
		}*/
		super.appendToResponse(aResponse, aContext);
    }
    
	public WOActionResults clean() {
		if(hasBinding("courses"))
			setValueForBinding(null, "courses");
		selection = null;
		if(hasBinding("selection"))
			setValueForBinding(null,"selection");
		session().takeValueForKeyPath(null, "state.coursesSelection");
		return null;
	}
	
	public void setSelection(Object val) {
		selection = val;
		if(hasBinding("selection"))
			setValueForBinding(selection,"selection");
		if(val == null) {
			if(hasBinding("courses"))
				setValueForBinding(null, "courses");
			session().takeValueForKeyPath(val, "state.coursesSelection");
			return;
		}
		EOEditingContext ec = (EOEditingContext)valueForBinding("ec");
		if(hasBinding("courses")) {
			boolean smartEduPlan = (EduCycle.className.equals("net.rujel.eduplan.PlanCycle"));
			EOQualifier quals[] = new EOQualifier[2];
//			int currTab = 0;
//			Integer tmp = (Integer)session().valueForKeyPath("state.courseSelector");
//			if(tmp != null) currTab = tmp.intValue();
			NSArray cycles = null;
			switch (currTab) {
			case CLASS_TAB:
				if(smartEduPlan) {
					NSMutableDictionary values = new NSMutableDictionary();
					values.takeValueForKey(session().valueForKeyPath("state.section.idx"), 
							"section");
					values.takeValueForKey(session().valueForKey("school"), "school");
					values.takeValueForKey(((EduGroup)selection).grade(), "grade");
					cycles = EOUtilities.objectsMatchingValues(ec, EduCycle.entityName, values);
				} else {
					cycles = EOUtilities.objectsMatchingKeyAndValue(ec, EduCycle.entityName, 
							"grade", ((EduGroup)selection).grade());
				}
				if(cycles != null && cycles.count() > 0) {
					quals[1] = Various.getEOInQualifier("cycle", cycles);
					quals[0] = new EOKeyValueQualifier("eduGroup", 
							EOQualifier.QualifierOperatorEqual, NullValue);
					quals[0] = new EOAndQualifier(new NSArray(quals));
				}
				quals[1] = new EOKeyValueQualifier("eduGroup", 
						EOQualifier.QualifierOperatorEqual, selection);
				if(quals[0] != null)
					quals[1] = new EOOrQualifier(new NSArray(quals));
				break;
			case TEACHER_TAB:
				quals[1] = new EOKeyValueQualifier("teacher", 
						EOQualifier.QualifierOperatorEqual, selection);
				break;
			case SUBJECT_TAB:
				if(smartEduPlan && selection instanceof EOEnterpriseObject) {
					NSMutableDictionary values = new NSMutableDictionary();
					values.takeValueForKey(session().valueForKeyPath("state.section.idx"), 
							"section");
					values.takeValueForKey(session().valueForKey("school"), "school");
					if(((EOEnterpriseObject)selection).entityName().equals("SubjectArea"))
						values.takeValueForKey(selection, "subjectEO.area");
					else
						values.takeValueForKey(selection, "subjectEO");
					cycles = EOUtilities.objectsMatchingValues(ec, EduCycle.entityName, values);
				} else if(selection instanceof String) {
					cycles = EOUtilities.objectsMatchingKeyAndValue(ec,
							EduCycle.entityName, "subject", selection);
				}
				quals[1] = Various.getEOInQualifier("cycle", cycles);
				break;
			default:
				throw new IllegalArgumentException("Unknown tab number");
			}
			quals[0] = new EOKeyValueQualifier("eduYear", EOQualifier.QualifierOperatorEqual, 
					session().valueForKey("eduYear"));
			quals[0] = new EOAndQualifier(new NSArray(quals));
			EOFetchSpecification fs = new EOFetchSpecification(EduCourse.entityName,quals[0],null);
			NSArray courses = ec.objectsWithFetchSpecification(fs);
			if(smartEduPlan && courses != null && courses.count() > 0) {
				quals[0] = new EOKeyValueQualifier("cycle.school",
						EOQualifier.QualifierOperatorEqual, session().valueForKey("school"));
				if(currTab != TEACHER_TAB) {
					quals[1] = new EOKeyValueQualifier("cycle.section",
							EOQualifier.QualifierOperatorEqual, session().valueForKeyPath(
							"state.section.idx"));
					quals[0] = new EOAndQualifier(new NSArray(quals));
				}
				if(currTab == CLASS_TAB) {
					NSMutableArray collect = new NSMutableArray();
					Enumeration enu = courses.objectEnumerator();
					NSArray students = ((EduGroup)selection).list();
					if(students != null && students.count() == 0) students = null;
					while (enu.hasMoreElements()) {
						EduCourse crs = (EduCourse) enu.nextElement();
						if(!quals[0].evaluateWithObject(crs))
							continue;
						if(crs.eduGroup() == selection) {
							collect.addObject(crs);
							continue;
						}
						if(students == null)
							continue;
						Enumeration senu = crs.groupList().objectEnumerator();
						while (senu.hasMoreElements()) {
							Student st = (Student) senu.nextElement();
							if(students.containsObject(st)) {
								collect.addObject(crs);
								break;
							}
						}
					}
				} else {
					courses = EOQualifier.filteredArrayWithQualifier(courses, quals[0]);
				}
			}
			courses = EOSortOrdering.sortedArrayUsingKeyOrderArray(courses, EduCourse.sorter);
			setValueForBinding(courses, "courses");
		}
		if(val instanceof EOEnterpriseObject)
			val = ec.globalIDForObject((EOEnterpriseObject)val);
		session().takeValueForKeyPath(val, "state.coursesSelection");
	}
	
	public boolean showTeachers() {
//		Integer tmp = (Integer)session().valueForKeyPath("state.courseSelector");
//		return (tmp!= null && tmp.intValue() == TEACHER_TAB);
		return (currTab == TEACHER_TAB);
	}

	public boolean showSubjects() {
//		Integer tmp = (Integer)session().valueForKeyPath("state.courseSelector");
//		return (tmp!= null && tmp.intValue() == CLASS_TAB);
		return (currTab == SUBJECT_TAB);
	}
	
	public String subjectSelector() {
		if((EduCycle.className.equals("net.rujel.eduplan.PlanCycle")))
			return "SubjectSelector";
		return null;
	}

	public boolean synchronizesVariablesWithBindings() {
        return false;
	}
	
	public boolean isStateless() {
		return false;
	}
}