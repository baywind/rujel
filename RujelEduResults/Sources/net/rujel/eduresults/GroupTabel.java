package net.rujel.eduresults;

import java.util.Enumeration;

import net.rujel.base.MyUtility;
import net.rujel.interfaces.EduCourse;
import net.rujel.interfaces.EduCycle;
import net.rujel.interfaces.Student;
import net.rujel.reusables.Counter;
import net.rujel.reusables.Various;

import com.webobjects.appserver.WOContext;
import com.webobjects.appserver.WOComponent;
import com.webobjects.eoaccess.EOUtilities;
import com.webobjects.eocontrol.EOAndQualifier;
import com.webobjects.eocontrol.EOEditingContext;
import com.webobjects.eocontrol.EOEnterpriseObject;
import com.webobjects.eocontrol.EOFetchSpecification;
import com.webobjects.eocontrol.EOKeyValueQualifier;
import com.webobjects.eocontrol.EOQualifier;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSDictionary;
import com.webobjects.foundation.NSMutableArray;
import com.webobjects.foundation.NSMutableDictionary;

public class GroupTabel extends WOComponent {
    public GroupTabel(WOContext context) {
        super(context);
    }
    
    public EduCourse course;
    public NSMutableArray containers;
    public NSArray students;
    protected NSArray found;
    protected NSMutableDictionary forStudent;
    public Object item;
    public ItogContainer itog;
	public NSMutableArray years;
	public Number eduYear;
    
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
    	try {
        	EOEnterpriseObject subjEO = (EOEnterpriseObject)cycle.valueForKey("subjectEO");
        	NSArray cycles = EOUtilities.objectsMatchingKeyAndValue(
        			ec, EduCycle.entityName, "subjectEO", subjEO);
        	quals[1] = Various.getEOInQualifier(ItogMark.CYCLE_KEY, cycles);
    	} catch (Exception e) {
			quals[1] = new EOKeyValueQualifier("cycle.subject",
					EOQualifier.QualifierOperatorEqual, cycle.subject());
		}
    	quals[0] = new EOAndQualifier(new NSArray(quals));
    	EOFetchSpecification fs = new EOFetchSpecification(ItogMark.ENTITY_NAME, quals[0], null);
    	found = ec.objectsWithFetchSpecification(fs);
    	if(found == null || found.count() == 0)
    		return;
    	containers = new NSMutableArray();
    	NSMutableDictionary yCounter = new NSMutableDictionary();
    	for (int i = 0; i < found.count(); i++) {
			ItogMark mark = (ItogMark)found.objectAtIndex(i);
			itog = mark.container();
			if(!containers.containsObject(itog)) {
				Various.addToSortedList(itog, containers, null, null);
				Counter yCnt = (Counter)yCounter.objectForKey(itog.eduYear());
				if(yCnt == null) {
					yCounter.setObjectForKey(new Counter(1), itog.eduYear());
				} else {
					yCnt.raise();
				}
			}
		}
    	itog = null;
    	years = new NSMutableArray(yCounter.count());
    	Enumeration enu = yCounter.keyEnumerator();
    	while (enu.hasMoreElements()) {
			Number year = (Number) enu.nextElement();
			Counter cnt = (Counter)yCounter.objectForKey(year);
			Integer count = new Integer(cnt.intValue());
			String present = MyUtility.presentEduYear(year.intValue());
			NSDictionary dict = new NSDictionary(new Object[] {year,present, count},
					new String[] {"year","text","colspan"});
			Various.addToSortedList(dict, years, "year", null);	
		}
    	forStudent = new NSMutableDictionary(containers.count());
    }
    
    public void setStudent(Student student) {
    	item = student;
    	forStudent.removeAllObjects();
    	if(student == null)
    		return;
    	EOKeyValueQualifier qual = new EOKeyValueQualifier(ItogMark.STUDENT_KEY, 
    			EOQualifier.QualifierOperatorEqual, student);
    	NSArray filtered = EOQualifier.filteredArrayWithQualifier(found, qual);
    	for (int i = 0; i < filtered.count(); i++) {
			ItogMark mark = (ItogMark)filtered.objectAtIndex(i);
			forStudent.setObjectForKey(mark.mark(), mark.container());
		}
    }
    
    public Student student() {
    	return (Student)item;
    }
    
    public Object mark() {
    	if(itog == null || forStudent.count() == 0)
    		return null;
    	return forStudent.objectForKey(itog);
    }
    
	public String cellStyle() {
		if(itog == null || itog.eduYear().equals(eduYear))
			return (forStudent.count() == 0)?"width:2em;":null;
		eduYear = itog.eduYear();
		if(forStudent.count() == 0)
			return "width:2em;border-left:double 3px;";
		return "border-left:double 3px;";
	}

}