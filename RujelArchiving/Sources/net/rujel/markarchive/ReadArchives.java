package net.rujel.markarchive;

import java.util.Enumeration;

import net.rujel.interfaces.EduCourse;
import net.rujel.interfaces.EduCycle;
import net.rujel.interfaces.Person;
import net.rujel.interfaces.Teacher;
import net.rujel.reusables.SessionedEditingContext;

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
import com.webobjects.foundation.NSKeyValueCoding;
import com.webobjects.foundation.NSLocking;
import com.webobjects.foundation.NSMutableArray;
import com.webobjects.foundation.NSMutableDictionary;
import com.webobjects.foundation.NSTimestamp;

public class ReadArchives extends WOComponent {
	
	public NSMutableArray records;
	public Object item;
	public NSMutableArray entities;
	protected NSMutableDictionary byEnt; 
//	public NSDictionary currEntity;
	
	public EOEditingContext ec;
	public NSMutableDictionary params;
	
    public ReadArchives(WOContext context) {
        super(context);
		ec = new SessionedEditingContext(session());
		params = new NSMutableDictionary();
		{
			NSTimestamp to = (NSTimestamp)session().valueForKey("today");
			params.takeValueForKey(to.timestampByAddingGregorianUnits(0, 0, -7, 0, 0, 0),"since");
			if(System.currentTimeMillis() - to.getTime() < NSLocking.OneDay)
				to = null;
			params.takeValueForKey(to, "to");
		}
		params.takeValueForKey(new Integer(2),"level");
		entities = (NSMutableArray)session().valueForKeyPath("modules.archiveType");
		if(entities == null || entities.count() == 0)
			return;
		NSArray used = EOUtilities.objectsForEntityNamed(ec, "UsedEntity");
		if(used == null || used.count() == 0)
			return;
		byEnt = new NSMutableDictionary();
		for (int j = 0; j < entities.count(); j++) {
			NSDictionary ent = (NSDictionary) entities.objectAtIndex(j);
			String name = (String)ent.valueForKey("entityName");
			if(name == null)
				continue;
			for (int i = 0; i < used.count(); i++) {
				EOEnterpriseObject ue = (EOEnterpriseObject)used.objectAtIndex(i);
				if(name.equals(ue.valueForKey("usedEntity"))) {
					ent = ent.mutableClone();
					ent.takeValueForKey(ue, "usedEntity");
					entities.replaceObjectAtIndex(ent, j);
					break;
				}
			}
			byEnt.takeValueForKey(ent, name);
		}
		for (int i = 0; i < used.count(); i++) {
			EOEnterpriseObject ue = (EOEnterpriseObject)used.objectAtIndex(i);
			String name = (String)ue.valueForKey("usedEntity");
			if(byEnt.valueForKey(name) != null)
				continue;
			NSMutableDictionary ent = new NSMutableDictionary(name,"entityName");
			ent.takeValueForKey(ue, "usedEntity");
			ent.takeValueForKey(name,"title");
			for (int j = 0; j < MarkArchive.keys.length; j++) {
				String test = (String)ue.valueForKey(MarkArchive.keys[j]);
				if(test == null)
					continue;
				if(test.equals("course") || test.equals("courseID")) {
					ent.takeValueForKey("course", "course");
					break;
				}
			}
			byEnt.takeValueForKey(ent, name);
			entities.addObject(ent);
		}
		records = new NSMutableArray();
		select();
    }
    
    public void select() {
    	records.removeAllObjects();
    	NSMutableArray quals = new NSMutableArray();
		quals.addObject(new EOKeyValueQualifier(MarkArchive.ACTION_TYPE_KEY, 
				EOQualifier.QualifierOperatorGreaterThanOrEqualTo, params.valueForKey("level")));
		quals.addObject(new EOKeyValueQualifier(MarkArchive.TIMESTAMP_KEY, 
				EOQualifier.QualifierOperatorGreaterThanOrEqualTo, params.valueForKey("since")));
		if(params.valueForKey("to") != null) {
			quals.addObject(new EOKeyValueQualifier(MarkArchive.TIMESTAMP_KEY, 
					EOQualifier.QualifierOperatorLessThanOrEqualTo, params.valueForKey("to")));
		}
		if(params.valueForKey("usedEntity") != null) {
			quals.addObject(new EOKeyValueQualifier(MarkArchive.USED_ENTITY_KEY, 
					EOQualifier.QualifierOperatorEqual,
					params.valueForKeyPath("usedEntity.usedEntity")));
		}
		EOFetchSpecification fs = new EOFetchSpecification(MarkArchive.ENTITY_NAME,
				new EOAndQualifier(quals),MarkArchive.backSorter);
		NSArray found = ec.objectsWithFetchSpecification(fs);
		if(found == null || found.count() == 0)
			return;
//		if(found.count() > 1000) {
//			session().takeValueForKey(found.count() + " — That's too much!", "message");
//			return;
//		}
		boolean checkUser;
		{
			Object user = session().valueForKey("user"); 
			if(user != null) {
				String name = user.getClass().getName();
				checkUser = "net.rujel.user.TableUser".equals(name);
			} else {
				checkUser = false;
			}
			checkUser = true; // TODO: comment this out
		}
		Enumeration enu = found.objectEnumerator();
		NSMutableDictionary dict = null;
		while (enu.hasMoreElements()) {
			MarkArchive arch = (MarkArchive) enu.nextElement();
			ifsame:
				if (dict != null && arch.actionType().intValue() == 1 &&
						"green".equals(dict.valueForKey("rowClass"))) {
					Object prev = dict.valueForKey("arch");
					MarkArchive prevMA;
					if(prev instanceof MarkArchive)
						prevMA = (MarkArchive)prev;
					else if(prev instanceof NSMutableArray)
						prevMA = (MarkArchive)((NSArray)prev).objectAtIndex(0);
					else
						break ifsame;
					if (!(arch.wosid().equals(prevMA.wosid()) && 
							arch.usedEntity().equals(prevMA.usedEntity())))
						break ifsame;
					EOEnterpriseObject ue = arch.usedEntity(); 
					for (int j = 0; j < MarkArchive.keys.length; j++) {
						String test = (String)ue.valueForKey(MarkArchive.keys[j]);
						if(test == null || test.equals("student") || test.equals("studentID"))
							continue;
						Object val1 = arch.valueForKey(MarkArchive.keys[j]);
						Object val2 = prevMA.valueForKey(MarkArchive.keys[j]);
						if((val1 == null)?val2 != null : !val1.equals(val2))
							break ifsame;
					}
					if(!(prev instanceof NSMutableArray)) {
						prev = new NSMutableArray(prevMA);
						dict.takeValueForKey(prev, "arch");
					}
					((NSMutableArray)prev).addObject(arch);
					dict.takeValueForKey("*" + ((NSMutableArray)prev).count(),"reason");
					continue;
				} // ifsame

			dict = new NSMutableDictionary(arch,"arch");
			dict.takeValueForKey(rowClass(arch), "rowClass");
			dict.takeValueForKey(arch.timestamp(), MarkArchive.TIMESTAMP_KEY);
			dict.takeValueForKey(arch.user(), MarkArchive.USER_KEY);
			dict.takeValueForKey(arch.reason(), "reason");
			{
				NSArray actionTypes = (NSArray)session().valueForKeyPath(
						"strings.RujelArchiving_Archive.actionTypes");
				if(actionTypes == null || actionTypes.count() < 4) {
					dict.takeValueForKey(arch.actionType(), "actionType");
				} else {
					Integer at = arch.actionType();
					if(at == null || at < 1 || at > 3)
						at = 0;
					dict.takeValueForKey(actionTypes.objectAtIndex(at), "actionType");
				}
			}
			records.addObject(dict);
			String ent = (String)arch.valueForKeyPath("usedEntity.usedEntity");
			NSDictionary entDict = (NSDictionary)byEnt.valueForKey(ent);
			if(entDict == null) {
				dict.takeValueForKey(new NSDictionary(ent,"title"), "usedEntity");
				continue;
			}
			dict.takeValueForKey(entDict, "usedEntity");
			Object courseRef = entDict.valueForKey("course");
			EduCourse course = null;
			if(courseRef instanceof CharSequence) {
				Integer cID = arch.getKeyValue(courseRef.toString());
				if(cID != null) {
					try {
						course = (EduCourse)EOUtilities.objectWithPrimaryKeyValue(ec, 
								EduCourse.entityName, cID);
					} catch (Exception e) {
						//
					}
				}
			} else if (courseRef instanceof NSKeyValueCoding) {
				NSKeyValueCoding keyDict = (NSKeyValueCoding)courseRef;
				String key = (String)keyDict.valueForKey("fromKey");
				Integer id = arch.getKeyValue(key);
				key = (String)keyDict.valueForKey("fromEntity");
				try {
					EOEnterpriseObject obj = EOUtilities.objectWithPrimaryKeyValue(ec, key, id);
					course = (EduCourse)obj.valueForKey("course");
				} catch (Exception e) {
					try {
						Class tIf = Class.forName("net.rujel.interfaces." + key);
						key = (String)tIf.getDeclaredField("entityName").get(null);
						EOEnterpriseObject obj = EOUtilities.objectWithPrimaryKeyValue(ec, key, id);
						course = (EduCourse)obj.valueForKey("course");						
					} catch (Exception e2) {
						// oops
					}
				}
			}
			dict.takeValueForKey(course, "course");
			if(course == null) {
				String cycleRef = (String)entDict.valueForKey("cycle");
				if(cycleRef == null)
					continue;
				Integer cID = arch.getKeyValue(cycleRef.toString());
				if(cID == null)
					continue;
				try {
					EduCycle cycle = (EduCycle)EOUtilities.objectWithPrimaryKeyValue(ec, 
							EduCycle.entityName, cID);
					dict.takeValueForKey(cycle.subject(), "subject");
					dict.takeValueForKey(cycle.grade().toString(), "group");
				} catch (Exception e) {
					continue;
				}
			} else {
				dict.takeValueForKey(course.subjectWithComment(), "subject");
				if(course.eduGroup() == null)
					dict.takeValueForKey(course.cycle().grade().toString(), "group");
				else
					dict.takeValueForKey(course.eduGroup().name(), "group");
				Teacher teacher = course.teacher(arch.timestamp());
				String teacherName = (teacher == null)? null :
					Person.Utility.fullName(teacher,true, 2, 2, 2);
				dict.takeValueForKey(teacherName, "teacherName");
				if(checkUser && teacherName != null && !teacherName.equals(arch.user())) {
					dict.takeValueForKey("warning", "teacherClass");
				}
			}
		} // found.objectEnumerator();
    }
    
    public static String rowClass(MarkArchive ma) {
		switch (ma.actionType()) {
		case 1:
			return "green";
		case 2:
			return "gerade";
		case 3:
			return "female";
		default:
			return "grey";
		}
    }
    
    public String rowClass() {
    	if (item instanceof MarkArchive)
			return rowClass((MarkArchive) item);
    	return null;
    }
}