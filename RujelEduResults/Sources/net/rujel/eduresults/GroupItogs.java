package net.rujel.eduresults;

import java.lang.reflect.Method;
import java.util.Enumeration;

import net.rujel.base.MyUtility;
import net.rujel.base.SettingsBase;
import net.rujel.interfaces.EduCourse;
import net.rujel.interfaces.EduCycle;
import net.rujel.interfaces.EduGroup;
import net.rujel.interfaces.Student;
import net.rujel.reusables.AdaptingComparator;
import net.rujel.reusables.Various;

import com.webobjects.appserver.WOContext;
import com.webobjects.appserver.WOComponent;
import com.webobjects.eocontrol.EOAndQualifier;
import com.webobjects.eocontrol.EOEditingContext;
import com.webobjects.eocontrol.EOEnterpriseObject;
import com.webobjects.eocontrol.EOFetchSpecification;
import com.webobjects.eocontrol.EOKeyValueQualifier;
import com.webobjects.eocontrol.EOQualifier;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSComparator.ComparisonException;
import com.webobjects.foundation.NSDictionary;
import com.webobjects.foundation.NSKeyValueCoding;
import com.webobjects.foundation.NSMutableArray;
import com.webobjects.foundation.NSMutableDictionary;

public class GroupItogs extends WOComponent {
	
	public EduGroup eduGroup;
	public NSMutableDictionary stItem;
	public Object item;
	public NSMutableArray students;
	public NSMutableArray cycles;
	protected NSMutableDictionary cDict;
	public NSArray[] byCycle;
	public int index;
	public int stIndex;
	public NSArray complete;
	public NSDictionary pedsovet;
	
    public GroupItogs(WOContext context) {
        super(context);
    }

	public String eduYear() {
		Integer year = (Integer)session().valueForKey("eduYear");
		return MyUtility.presentEduYear(year);
	}
	
	public void setEduGroup(EduGroup group) {
		eduGroup = group;
		NSArray list = group.list();
		if(list == null || list.count() == 0)
			return;
		EOEditingContext ec = group.editingContext();
		Integer year = (Integer)session().valueForKey("eduYear");
		NSArray containers = ItogContainer.itogsInYear(year, ec);
		if(containers == null)
			return;
		EOQualifier[] quals = new EOQualifier[2];
		quals[0] = Various.getEOInQualifier(ItogMark.CONTAINER_KEY, containers);
		quals[1] = Various.getEOInQualifier(ItogMark.STUDENT_KEY, list);
		quals[0] = new EOAndQualifier(new NSArray(quals));
		EOFetchSpecification fs = new EOFetchSpecification(ItogMark.ENTITY_NAME,quals[0],null);
		NSArray itogs = ec.objectsWithFetchSpecification(fs);
		if(itogs == null || itogs.count() == 0)
			return;
		students = new NSMutableArray(list.count());
		Enumeration enu = list.objectEnumerator();
		list = EduCycle.Lister.cyclesForEduGroup(group);
		cycles = (list==null)?new NSMutableArray():list.mutableClone();
		byCycle = new NSArray[cycles.count() + 10];
		NSMutableDictionary forListName = new NSMutableDictionary();
		fs = new EOFetchSpecification("CourseAudience",quals[1],null);
		list = ec.objectsWithFetchSpecification(fs);
		if(list != null && list.count() == 0)
			list = null;
		tryComplete();
		if(complete != null) {
			quals[0] = new EOKeyValueQualifier(ItogMark.STUDENT_KEY,
					EOQualifier.QualifierOperatorEqual,null);
			containers = EOQualifier.filteredArrayWithQualifier(complete, quals[0]);
			if(containers != null && containers.count() == 0)
				containers = null;
			else
				containers = (NSArray)containers.valueForKey("course");
		} else {
			containers = null;
		}
		while (enu.hasMoreElements()) {  // course group members
			Student st = (Student) enu.nextElement();
			NSMutableDictionary dict = new NSMutableDictionary(st,"student");
			students.addObject(dict);
			quals[1] = new EOKeyValueQualifier(ItogMark.STUDENT_KEY,
					EOQualifier.QualifierOperatorEqual,st);
			NSArray filtered = (list == null)?null:
				EOQualifier.filteredArrayWithQualifier(list, quals[1]);
			if(filtered != null && filtered.count() > 0) {
				Enumeration fenu = filtered.objectEnumerator();
				while (fenu.hasMoreElements()) {
					EOEnterpriseObject aud = (EOEnterpriseObject) fenu.nextElement();
					EduCourse crs = (EduCourse)aud.valueForKey("course");
					boolean isComplete = (containers != null && containers.containsObject(crs));
					prepareGrid(crs, ec, forListName, dict, isComplete);
				}
			}
			filtered = EOQualifier.filteredArrayWithQualifier(itogs, quals[1]);
			if(filtered != null && filtered.count() > 0) { // put ItogMark on place
				Enumeration fenu = filtered.objectEnumerator();
				while (fenu.hasMoreElements()) {
					ItogMark itog = (ItogMark) fenu.nextElement();
					EduCycle cycle = itog.cycle();
					index = cycles.indexOf(cycle);
					if(index < 0) {
						index = cycles.count();
						cycles.addObject(cycle);
						if(byCycle.length <= index) {
							NSArray[] tmp = byCycle;
							byCycle = new NSArray[index + 10];
							for (int i = 0; i < tmp.length; i++) {
								byCycle[i] = tmp[i];
							}
						}
						
					}
					if(byCycle[index] == null) {
						byCycle[index] = new NSArray(itog.container());
					} else if(!byCycle[index].containsObject(itog.container())) {
						NSMutableArray tmp = (byCycle[index] instanceof NSMutableArray)?
								(NSMutableArray)byCycle[index] : byCycle[index].mutableClone();
						tmp.addObject(itog.container());
						try {
							tmp.sortUsingComparator(new AdaptingComparator(ItogContainer.class));
						} catch (ComparisonException e) {}
//						EOSortOrdering.sortArrayUsingKeyOrderArray(tmp, ItogContainer.sorter);
						byCycle[index] = tmp;
					}
					index = 0;
					NSMutableDictionary md = (NSMutableDictionary)dict.objectForKey(cycle);
					if(md == null) {
						md = new NSMutableDictionary(itog.mark(),itog.container());
						dict.setObjectForKey(md,cycle);
					} else {
						md.setObjectForKey(itog.mark(),itog.container());
					}
				}
			} // put ItogMark on place
			if(complete == null)
				continue;
			filtered = EOQualifier.filteredArrayWithQualifier(complete, quals[1]);
			if(filtered != null && filtered.count() > 0) {
				Enumeration fenu = filtered.objectEnumerator();
				while (fenu.hasMoreElements()) {
					EOEnterpriseObject cpt = (EOEnterpriseObject) fenu.nextElement();
					EduCycle cycle = (EduCycle)cpt.valueForKeyPath("course.cycle");
					NSMutableDictionary md = (NSMutableDictionary)dict.objectForKey(cycle);
					if(md == null) {
						md = new NSMutableDictionary(Boolean.TRUE,"complete");
						dict.setObjectForKey(md,cycle);
					} else {
						md.setObjectForKey(Boolean.TRUE,"complete");
					}
				}
			}
			if(pedsovet != null)
				dict.takeValueForKey(pedsovet.objectForKey(st), "pedsovet");
		}  // course group members
		quals[0] = new EOKeyValueQualifier("eduGroup", EOQualifier.QualifierOperatorEqual, group);
		quals[1] = new EOKeyValueQualifier("eduYear",EOQualifier.QualifierOperatorEqual,year);
		quals[0] = new EOAndQualifier(new NSArray(quals));
		fs = new EOFetchSpecification(EduCourse.entityName,quals[0],null);
		list = ec.objectsWithFetchSpecification(fs);
		if(list != null && list.count() > 0) {
			enu = list.objectEnumerator();
			while (enu.hasMoreElements()) {
				EduCourse crs = (EduCourse) enu.nextElement();
				boolean isComplete = (containers != null && containers.containsObject(crs));
				prepareGrid(crs, ec, forListName, null, isComplete);
			}
		}
	}
	
	
	
	protected NSMutableDictionary dots(NSArray itogs, NSMutableDictionary dict) {
		if(dict == null) {
			Object[] keys = itogs.objects();
			String[] dots = new String[keys.length];
			for (int i = 0; i < dots.length; i++) {
				dots[i] = ".";
			}
			return new NSMutableDictionary(dots,keys);
		} else {
			for (int i = 0; i < itogs.count(); i++) {
				Object itog = itogs.objectAtIndex(i);
				if(dict.objectForKey(itog) == null)
					dict.setObjectForKey(".", itog);
			}
			return dict;
		}
	}
	
	protected void prepareGrid(EduCourse crs, EOEditingContext ec,
			NSMutableDictionary forListName, NSMutableDictionary stDict, boolean isComplete) {
		String listName = SettingsBase.stringSettingForCourse(ItogMark.ENTITY_NAME,crs,ec);
		NSArray itogs = (NSArray)forListName.valueForKey(listName);
		if(itogs == null) {
			itogs = ItogType.typesForList(listName, crs.eduYear(), ec);
			itogs = ItogType.itogsForTypeList(itogs,crs.eduYear());
			forListName.takeValueForKey(itogs, listName);
		}
		if(itogs == null || itogs.count() == 0)
			return;
		EduCycle cycle = crs.cycle();
		if(complete != null) { // put dots
		if(stDict == null) {
			NSArray list = (NSArray)crs.valueForKey("audience");
			if(list == null || list.count() == 0) { //prepare dots for students
				Enumeration enu = students.objectEnumerator();
				while (enu.hasMoreElements()) {
					NSMutableDictionary dict = (NSMutableDictionary) enu.nextElement();
					NSMutableDictionary dots = (NSMutableDictionary)dict.objectForKey(cycle);
					if(dots == null) {
						dots = dots(itogs,dots);
						dict.setObjectForKey(dots, cycle);
					} else {
						dots(itogs,dots);
					}
					if(isComplete)
						dots.takeValueForKey(Boolean.TRUE, "complete");
				}
			}
		} else {
			NSMutableDictionary dots = (NSMutableDictionary)stDict.objectForKey(cycle);
			if(dots == null) {
				dots = dots(itogs,dots);
				stDict.setObjectForKey(dots, cycle);
			} else {
				dots = dots(itogs,dots);
			}
			if(isComplete)
				dots.takeValueForKey(Boolean.TRUE, "complete");
		}
		}
		index = cycles.indexOfObject(cycle);
		if(index < 0) {
			index = cycles.count();
			cycles.addObject(cycle);
			if(byCycle.length <= index) {
				NSArray[] tmp = byCycle;
				byCycle = new NSArray[index + 10];
				for (int i = 0; i < tmp.length; i++) {
					byCycle[i] = tmp[i];
				}
			}
		}
		if(byCycle[index] == itogs)
			return;
		if(byCycle[index] != null) {
			NSMutableArray toAdd = new NSMutableArray();
			for (int i = 0; i < itogs.count(); i++) {
				Object cnt = itogs.objectAtIndex(i);
				if(!byCycle[index].containsObject(cnt))
					toAdd.addObject(cnt);
			}
			if(toAdd.count() == 0) {
				return;
			}
			toAdd.addObjectsFromArray(byCycle[index]);
			try {
				toAdd.sortUsingComparator(new AdaptingComparator(ItogContainer.class));
			} catch (ComparisonException e) {}
			itogs = toAdd.immutableClone();
		}
		byCycle[index] = itogs;
	}
	
	
	
	public void setCycle(EduCycle cycle) {
//		item = cycle;
		if(cycle == null || stItem == null) {
			cDict = null;
		} else {
			cDict = (NSMutableDictionary)stItem.objectForKey(cycle);
		}
	}
	
	public NSArray itogsList() {
		return byCycle[index];
	}
	
	public EduCycle cycle() {
		return (EduCycle)cycles.objectAtIndex(index);
	}
	public String cycleTitle() {
		EduCycle cycle = cycle();
		if(cycle.grade().equals(eduGroup.grade()))
			return cycle().subject();
		StringBuilder buf = new StringBuilder(cycle().subject());
		buf.append(' ').append('(').append(cycle.grade()).append(')');
		return buf.toString();
	}
	
	public String itog() {
		if(cDict == null)
			return null;
		return (String)cDict.objectForKey(item);
	}
	
	public String cellClass() {
		boolean incomplete = (complete != null && cDict != null && 
				cDict.objectForKey("complete") == null);
		if(item == byCycle[index].objectAtIndex(0))
			return (incomplete)?"incomplete leftCol":"leftCol";
		return (incomplete)?"incomplete":null;
	}
	
	protected void tryComplete() {
		if(pedsovet == null) {
			try {
				Method m = Class.forName("net.rujel.complete.PedDecision").getMethod(
						"dictForGroup", EduGroup.class);
				pedsovet = (NSDictionary)m.invoke(null,eduGroup);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		if(complete != null)
			return;
		NSDictionary dict = SettingsBase.courseDict(eduGroup);
		EOEditingContext ec = eduGroup.editingContext(); 
		if(!Various.boolForObject(SettingsBase.stringSettingForCourse("CompletionActive",dict,ec)))
			return;
		try {
			Method m = Class.forName("net.rujel.complete.Completion").getMethod(
					"findCompletions", Object.class, Object.class,
					String.class, Boolean .class, EOEditingContext.class);
			complete = (NSArray)m.invoke(null,null,eduGroup.list(),"student",Boolean.TRUE,ec);
			NSArray more = (NSArray)m.invoke(null,null,
					NSKeyValueCoding.NullValue, "student", Boolean.TRUE, ec);
			if(complete == null || complete.count() == 0) {
				complete = more;
			} else if(more != null && more.count() > 0) {
				complete = complete.arrayByAddingObjectsFromArray(more);
			}
		} catch (Exception e) {}
	}

    public String number() {
    	return Integer.toString(stIndex +1);
    }
}