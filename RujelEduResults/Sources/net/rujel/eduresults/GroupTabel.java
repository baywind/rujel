package net.rujel.eduresults;

import java.util.Enumeration;

import net.rujel.base.MyUtility;
import net.rujel.eduplan.PlanCycle;
import net.rujel.eduplan.Subject;
import net.rujel.interfaces.EduCourse;
import net.rujel.interfaces.EduCycle;
import net.rujel.interfaces.Student;
import net.rujel.reusables.Various;

import com.webobjects.appserver.WOContext;
import com.webobjects.appserver.WOComponent;
import com.webobjects.eoaccess.EOUtilities;
import com.webobjects.eocontrol.EOAndQualifier;
import com.webobjects.eocontrol.EOEditingContext;
import com.webobjects.eocontrol.EOFetchSpecification;
import com.webobjects.eocontrol.EOQualifier;
import com.webobjects.eocontrol.EOSortOrdering;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSMutableArray;
import com.webobjects.foundation.NSMutableDictionary;

public class GroupTabel extends WOComponent {
    public GroupTabel(WOContext context) {
        super(context);
    }
    
	public static final NSArray cycleSorter = new NSArray(new EOSortOrdering[]{
			new EOSortOrdering(PlanCycle.GRADE_KEY, EOSortOrdering.CompareAscending),
			new EOSortOrdering(PlanCycle.SUBJECT_EO_KEY, EOSortOrdering.CompareAscending)});
    public EduCourse course;
    public NSMutableArray containers;
    public NSArray students;
    protected NSArray found;
    public Object item;
    public Student student;
	public NSMutableArray years;
	public Object eduYear;
	public String subject;
    
    public void setCourse(EduCourse crs) {
    	course = crs;
    	if (crs == null)
    		return;
    	EOEditingContext ec = crs.editingContext();
    	students = crs.groupList();
    	if(students == null || students.count() == 0)
    		return;
    	EOQualifier[] quals = new EOQualifier[2];
    	quals[0] = Various.getEOInQualifier(ItogMark.STUDENT_KEY, students);
    	EduCycle cycle = crs.cycle();
    	subject = cycle.subject();
    	NSArray cycles;
    	try {
        	Subject subjEO = (Subject)cycle.valueForKey("subjectEO");
        	cycles = EOUtilities.objectsMatchingKeyAndValue(
        			ec, EduCycle.entityName, "subjectEO.subjectGroup", subjEO.subjectGroup());
    	} catch (Exception e) {
    		cycles = EOUtilities.objectsMatchingKeyAndValue(
        			ec, EduCycle.entityName, "subject",subject);
		}
    	if(cycles.count() > 1)
    		cycles = EOSortOrdering.sortedArrayUsingKeyOrderArray(cycles, cycleSorter);
    	quals[1] = Various.getEOInQualifier(ItogMark.CYCLE_KEY, cycles);
    	quals[0] = new EOAndQualifier(new NSArray(quals));
    	EOFetchSpecification fs = new EOFetchSpecification(ItogMark.ENTITY_NAME, quals[0], null);
    	found = ec.objectsWithFetchSpecification(fs);
    	if(found == null || found.count() == 0)
    		return;
    	containers = new NSMutableArray();
    	NSMutableDictionary<String, NSMutableDictionary> yCounter = new NSMutableDictionary();
//    	String subj = course.cycle().subject();
    	for (int i = 0; i < found.count(); i++) {
			ItogMark mark = (ItogMark)found.objectAtIndex(i);
			ItogContainer itog = mark.container();
			StringBuilder buf = new StringBuilder();
			buf.append(MyUtility.presentEduYear(itog.eduYear().intValue()));
			EduCycle c = mark.cycle();
			buf.append('\n').append('(').append(c.grade()).append(')');
			buf.append(' ').append(c.subject());
			String key=buf.toString();
			NSMutableDictionary yDict = yCounter.objectForKey(key);
			if(yDict == null) {
				yDict = new NSMutableDictionary(key,"title");
				yDict.setObjectForKey(new NSMutableDictionary(), "itogs");
				yDict.setObjectForKey(itog.eduYear(), "eduYear");
				yDict.setObjectForKey(MyUtility.presentEduYear(itog.eduYear().intValue()),
						"presentYear");
				yDict.setObjectForKey(c.grade(),"grade");
				yDict.setObjectForKey(c.subject(),"subject");
				yCounter.setObjectForKey(yDict, key);
				if(subject != null && !subject.equals(c.subject()))
					subject = null;
			}
			NSMutableDictionary itogs = (NSMutableDictionary)yDict.valueForKey("itogs");
			NSMutableDictionary container = (NSMutableDictionary)itogs.objectForKey(itog);
			if(container == null) {
				container = new NSMutableDictionary(itog,"itog");
				container.setObjectForKey(key, "eduYear");
				itogs.setObjectForKey(container, itog);
			}
			Student st = mark.student();
			Object obj = container.objectForKey(st);
			if(obj instanceof NSMutableArray) {
				((NSMutableArray)obj).addObject(mark);
			} else if(obj instanceof ItogMark) {
				container.setObjectForKey(new NSMutableArray(new Object[]{obj,mark}),st);
			} else {
				container.setObjectForKey(mark,st);
			}
		}
//    	itog = null;
    	years = new NSMutableArray(yCounter.count());
    	Enumeration enu = yCounter.objectEnumerator();
    	while (enu.hasMoreElements()) {
 			Various.addToSortedList(enu.nextElement(), years, "title", null);	
		}
    	enu = years.objectEnumerator();
    	containers = new NSMutableArray();
    	NSArray sort = new NSArray(new EOSortOrdering("itog", EOSortOrdering.CompareAscending));
    	while (enu.hasMoreElements()) {
    		NSMutableDictionary yDict = (NSMutableDictionary) enu.nextElement();
			NSMutableDictionary itogs = (NSMutableDictionary)yDict.valueForKey("itogs");
			NSArray sorted = EOSortOrdering.sortedArrayUsingKeyOrderArray(itogs.allValues(), sort);
			containers.addObjectsFromArray(sorted);
		}
    }
        
    public Object mark() {
    	if(item instanceof NSMutableDictionary && student != null) {
    		Object m = ((NSMutableDictionary)item).objectForKey(student);
    		if(m instanceof ItogMark) {
    			return ((ItogMark)m).mark();
    		} else if(m instanceof NSMutableArray) {
    			StringBuilder buf = new StringBuilder();
    			Enumeration enu = ((NSMutableArray)m).objectEnumerator();
    			while (enu.hasMoreElements()) {
					ItogMark mark = (ItogMark) enu.nextElement();
					buf.append(mark.mark()).append(' ');
				}
    		}
    	}
    	return null;
    }
    
	public String cellStyle() {
		if(item == null && student == null)
			return "width:2em;";
		NSMutableDictionary cnt = (NSMutableDictionary)item;
		if(eduYear != null && eduYear.equals(cnt.objectForKey("eduYear")))
			return (student == null)?"width:2em;":null;
		eduYear = cnt.objectForKey("eduYear");
		if(student == null)
			return "width:2em;border-left:double 3px;";
		return "border-left:double 3px;";
	}

}