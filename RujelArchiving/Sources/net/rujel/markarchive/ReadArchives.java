package net.rujel.markarchive;

import java.util.Enumeration;

import net.rujel.base.MyUtility;
import net.rujel.interfaces.EduCourse;
import net.rujel.interfaces.EduCycle;
import net.rujel.interfaces.EduGroup;
import net.rujel.interfaces.Person;
import net.rujel.interfaces.Teacher;
import net.rujel.reusables.SessionedEditingContext;
import net.rujel.reusables.Various;

import com.webobjects.appserver.WOContext;
import com.webobjects.appserver.WOComponent;
import com.webobjects.appserver.WOMessage;
import com.webobjects.eoaccess.EOUtilities;
import com.webobjects.eocontrol.*;
import com.webobjects.foundation.*;
import com.webobjects.appserver.WOActionResults;

public class ReadArchives extends WOComponent {
	
	protected NSMutableArray records;
	public NSMutableArray list;
	public Object item;
	public NSMutableArray entities;
	protected NSMutableDictionary byEnt; 
	
	public EOEditingContext ec;
	public NSMutableDictionary params;
	protected NSMutableArray sorter = MarkArchive.backSorter.mutableClone();
	public NSDictionary sortStyle;
	
    public ReadArchives(WOContext context) {
        super(context);
		ec = new SessionedEditingContext(context.session());
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
			entDict(ue);
		}
		records = new NSMutableArray();
		if(!Various.boolForObject(session().valueForKeyPath("readAccess.edit.ReadArchives"))) {
			String username = (String)context.session().valueForKeyPath("user.present");
			if(username != null)
				setFilterUser(username);
		}
		select();
		session().savePageInPermanentCache(this);
    }
    
    public String title() {
    	return (String)session().valueForKeyPath(
    			"strings.RujelArchiving_Archive.ReadArchives.title");
    }
    
    public NSDictionary entDict(EOEnterpriseObject usedEntity) {
		String entityName = (String)usedEntity.valueForKey("usedEntity");
    	NSMutableDictionary ent = (NSMutableDictionary)byEnt.valueForKey(entityName);
    	if(ent == null) {
    		ent = new NSMutableDictionary(entityName,"entityName");
    		ent.takeValueForKey(usedEntity, "usedEntity");
    		ent.takeValueForKey(entityName,"title");
    		for (int j = 0; j < MarkArchive.keys.length; j++) {
    			String test = (String)usedEntity.valueForKey(MarkArchive.keys[j]);
    			if(test == null)
    				continue;
    			if(test.equals("course") || test.equals("courseID")) {
    				ent.takeValueForKey("course", "course");
    				break;
    			}
    		}
    		byEnt.takeValueForKey(ent, entityName);
    		entities.addObject(ent);
    	}
		return ent;
    }
    
    private NSTimestamp dateBatch(NSTimestamp startDate, NSMutableArray quals) {
    	if(startDate == null)
    		startDate = (NSTimestamp)params.valueForKey("since");
    	if(startDate == null) {
    		Integer year = (Integer)session().valueForKey("eduYear");
    		startDate = MyUtility.dayInEduYear(year.intValue() -1);
    		params.takeValueForKey(startDate, "since");
    	}
    	long start = startDate.getTime();
    	EOQualifier qual = new EOKeyValueQualifier(MarkArchive.TIMESTAMP_KEY, 
				EOQualifier.QualifierOperatorGreaterThanOrEqualTo,startDate);
    	NSTimestamp finDate = (NSTimestamp)params.valueForKey("to");
    	long fin = (finDate == null)?System.currentTimeMillis():finDate.getTime();
    	if((fin - start) > NSLocking.OneWeek) {
    		finDate = startDate.timestampByAddingGregorianUnits(0, 0, 7, 0, 0, 0);
    	}
    	if(finDate != null) {
        	EOQualifier qual1 = new EOKeyValueQualifier(MarkArchive.TIMESTAMP_KEY, 
    				EOQualifier.QualifierOperatorLessThan,finDate);
        	qual = new EOAndQualifier(new NSArray(new EOQualifier[] {qual,qual1}));
    	}
    	if(quals.count() == 0)
    		quals.addObject(qual);
    	else
    		quals.replaceObjectAtIndex(qual, 0);
    	if ((fin - start) <= NSLocking.OneWeek)
    		return null;
    	return finDate;
    }
    
    public void select() {
    	records.removeAllObjects();
    	list = null;
    	NSMutableArray quals = new NSMutableArray();
    	NSTimestamp finDate = dateBatch((NSTimestamp)params.valueForKey("since"), quals);
		quals.addObject(new EOKeyValueQualifier(MarkArchive.ACTION_TYPE_KEY, 
				EOQualifier.QualifierOperatorGreaterThanOrEqualTo, params.valueForKey("level")));
		if(params.valueForKey("usedEntity") != null) {
			quals.addObject(new EOKeyValueQualifier(MarkArchive.USED_ENTITY_KEY, 
					EOQualifier.QualifierOperatorEqual,
					params.valueForKeyPath("usedEntity.usedEntity")));
		}
		NSArray found = (NSMutableArray)params.valueForKey("extraQuals");
		if(found != null && found.count() > 0) {
			Enumeration enu = found.objectEnumerator();
			while (enu.hasMoreElements()) {
				NSDictionary qd = (NSDictionary) enu.nextElement();
				EOQualifier qual = (EOQualifier)qd.valueForKey("qualifier");
				if(qual == null)
					continue;
				quals.addObject(qual);
				qd.takeValueForKey(Boolean.TRUE, "used");
			}
		}
		NSArray recordsSorter = new NSArray(new EOSortOrdering[] {
				new EOSortOrdering(MarkArchive.WOSID_KEY,EOSortOrdering.CompareAscending),
				new EOSortOrdering(MarkArchive.KEY1_KEY,EOSortOrdering.CompareAscending),
				new EOSortOrdering(MarkArchive.KEY2_KEY,EOSortOrdering.CompareAscending),
				new EOSortOrdering(MarkArchive.KEY3_KEY,EOSortOrdering.CompareAscending),
				new EOSortOrdering(MarkArchive.TIMESTAMP_KEY, EOSortOrdering.CompareAscending)});
		EOFetchSpecification fs = new EOFetchSpecification(MarkArchive.ENTITY_NAME,
				new EOAndQualifier(quals),recordsSorter);
		found = ec.objectsWithFetchSpecification(fs);
		params.removeObjectForKey("warnings");
		if(found == null || found.count() == 0) {
			while(finDate != null) {
				NSTimestamp startDate = finDate;
				finDate = dateBatch(startDate, quals);
				fs.setQualifier(new EOAndQualifier(quals));
				found = ec.objectsWithFetchSpecification(fs);
				if(found != null && found.count() > 0) {
					params.takeValueForKey(startDate, "since");
					break;
				}
			}
			if(found == null || found.count() == 0)
				return;
		}
//		if(found.count() > 1000) {
//			session().takeValueForKey(found.count() + " — That's too much!", "message");
//			return;
//		}
/*		boolean checkUser;
		{
			Object user = session().valueForKey("user"); 
			if(user != null) {
				String name = user.getClass().getName();
				checkUser = "net.rujel.user.TableUser".equals(name);
			} else {
				checkUser = Boolean.getBoolean("ReadArchives.forceCheckUser");
			}
		} */
		Enumeration enu = found.objectEnumerator();
		NSMutableDictionary dict = null;
		int noWarn = 0;
		NSArray actionTypes = (NSArray)session().valueForKeyPath(
				"strings.RujelArchiving_Archive.actionTypes");
		while (enu.hasMoreElements() || finDate != null) {
			if(!enu.hasMoreElements()) {
				if(records.count() > 500) {
					params.takeValueForKey(finDate, "to");
					session().takeValueForKey(session().valueForKeyPath(
						"strings.RujelArchiving_Archive.ReadArchives.limitedSelection"), "message");
					break;
				}
				while(finDate != null) {
					finDate = dateBatch(finDate, quals);
					fs.setQualifier(new EOAndQualifier(quals));
					found = ec.objectsWithFetchSpecification(fs);
					if(found != null && found.count() > 0)
						break;
				}
				if(found == null || found.count() == 0)
					break;
				else
					enu = found.objectEnumerator();
			}
			MarkArchive arch = (MarkArchive) enu.nextElement();
			ifsame: // grouping
				if (dict != null) { // && arch.actionType().intValue() == 1 &&
//						"green".equals(dict.valueForKey("rowClass"))) {
					String grouping = (String)dict.valueForKeyPath("usedEntity.grouping");
					if(grouping == null)
						break ifsame;
					Object prev = dict.valueForKey("arch");
					MarkArchive prevMA;
					if(prev instanceof MarkArchive)
						prevMA = (MarkArchive)prev;
					else if(prev instanceof NSMutableArray)
						prevMA = (MarkArchive)((NSArray)prev).objectAtIndex(0);
					else
						break ifsame;
					if (!(arch.wosid().equals(prevMA.wosid()) && 
							arch.usedEntity().equals(prevMA.usedEntity()) &&
							arch.actionType().equals(prevMA.actionType())))
						break ifsame;
					EOEnterpriseObject ue = arch.usedEntity(); 
					for (int j = 0; j < MarkArchive.keys.length; j++) {
						String test = (String)ue.valueForKey(MarkArchive.keys[j]);
						if(test == null || test.equals(grouping) || test.equals(grouping + "ID"))
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
					Various.addToSortedList(arch, (NSMutableArray)prev, MarkArchive.TIMESTAMP_KEY,
							EOSortOrdering.CompareAscending);
//					((NSMutableArray)prev).addObject(arch);
					if(arch.reason() != null) {
						String reason = (String)dict.valueForKey("reason");
						String aRsn = WOMessage.stringByEscapingHTMLAttributeValue(arch.reason());
						if(reason == null) {
							dict.takeValueForKey("font-style:italic;", "actStyle");
							dict.takeValueForKey(aRsn, "reason");
						} else if(!reason.contains(aRsn)) {
							reason = reason + "; " + aRsn;
							dict.takeValueForKey(reason, "reason");
						}
					}
					dict.takeValueForKey("*" + ((NSMutableArray)prev).count(),"multiplier");
					continue;
				} // ifsame

			dict = new NSMutableDictionary(arch,"arch");
			dict.takeValueForKey(rowClass(arch), "rowClass");
			dict.takeValueForKey(arch.timestamp(), MarkArchive.TIMESTAMP_KEY);
			dict.takeValueForKey(arch.user(), MarkArchive.USER_KEY);
			dict.takeValueForKey(arch.wosid(), MarkArchive.WOSID_KEY);
			if(arch.reason() != null) {
				dict.takeValueForKey("font-style:italic;", "actStyle");
				dict.takeValueForKey(
						WOMessage.stringByEscapingHTMLAttributeValue(arch.reason()), "reason");
			}
			if(actionTypes == null || actionTypes.count() < 4) {
				dict.takeValueForKey(arch.actionType(), "actionType");
			} else {
				Integer at = arch.actionType();
				if(at == null || at < 1 || at > 3)
					at = 0;
				dict.takeValueForKey(actionTypes.objectAtIndex(at), "actionType");
			}
			records.addObject(dict);
			NSDictionary entDict = entDict(arch.usedEntity());
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
					Integer grade = cycle.grade();
					dict.takeValueForKey(grade, "grade");
					dict.takeValueForKey(grade.toString(), "group");
					dict.takeValueForKey(cycle, "cycle");
				} catch (Exception e) {
					continue;
				}
			} else {
				dict.takeValueForKey(course.subjectWithComment(), "subject");
				dict.takeValueForKey(course.cycle(), "cycle");
				dict.takeValueForKey(course.cycle().grade(), "grade");
				if(course.eduGroup() == null) {
					Integer grade = course.cycle().grade();
					dict.takeValueForKey(grade.toString(), "group");
					dict.takeValueForKey(grade, "grade");
				} else {
					EduGroup gr = course.eduGroup();
					dict.takeValueForKey(gr.name(), "group");
					dict.takeValueForKey(gr.grade(), "grade");
				}
				Teacher teacher = course.teacher(arch.timestamp());
				String teacherName = (teacher == null)? 
						(String)session().valueForKeyPath("strings.RujelBase_Base.vacant") :
							Person.Utility.fullName(teacher,true, 2, 2, 2);
				dict.takeValueForKey(teacherName, "teacherName");
				if(teacher != null && !teacherName.equals(arch.user())) {
					dict.takeValueForKey("warning", "teacherClass");
				} else {
					noWarn++;
				}
			}
		} // found.objectEnumerator();
		if(noWarn == 0)
			records.takeValueForKey(null, "teacherClass");
		else if(noWarn < records.count())
			params.takeValueForKey("grey", "warnings");
		if(sortStyle != null && params.valueForKey("usedEntity") != null) {
			if((Various.boolForObject(params.valueForKeyPath("usedEntity.noTeacher"))
					&& sortStyle.valueForKey("teacher") != null) ||
			(Various.boolForObject(params.valueForKeyPath("usedEntity.noCourse"))
					&& (sortStyle.valueForKey("teacher") != null ||
							sortStyle.valueForKey("group") != null ||
							sortStyle.valueForKey("subject") != null))) {
				sorter = MarkArchive.backSorter.mutableClone();
				sortStyle = null;
			}
		}
		prepareList(null);
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
    
    public void sortByDate() {
    	EOSortOrdering so = (EOSortOrdering)sorter.lastObject();
    	sortStyle = null;
    	if(so == null || !so.key().equals(MarkArchive.TIMESTAMP_KEY)) {
    		so = (EOSortOrdering)MarkArchive.backSorter.objectAtIndex(0);
    	} else if(sorter.count() < 2) {
    		if(so.selector() == EOSortOrdering.CompareDescending) {
    			so = new EOSortOrdering(MarkArchive.TIMESTAMP_KEY,EOSortOrdering.CompareAscending);
    			sortStyle = new NSDictionary("selection","date");
    		} else {
    			so = (EOSortOrdering)MarkArchive.backSorter.objectAtIndex(0);
    		}
    	} else if(so.selector() != EOSortOrdering.CompareDescending) {
    		sortStyle = new NSDictionary("selection","date");
    	}
    	sorter.removeAllObjects();
    	sorter.addObject(so);
    	EOSortOrdering.sortArrayUsingKeyOrderArray(list, sorter);
    }
    
    public void sortBySubject() {
    	EOSortOrdering so = (EOSortOrdering)sorter.lastObject();
    	if(so == null || !so.key().equals(MarkArchive.TIMESTAMP_KEY))
    		so = (EOSortOrdering)MarkArchive.backSorter.objectAtIndex(0);
    	sorter.removeAllObjects();
    	if(sortStyle == null || sortStyle.valueForKey("subject") == null) {
    		sorter.addObject(new EOSortOrdering("cycle",EOSortOrdering.CompareAscending));
    		sorter.addObject(new EOSortOrdering("course",EOSortOrdering.CompareAscending));
    		sortStyle = new NSDictionary("selection","subject");
    	} else {
    		so = (EOSortOrdering)MarkArchive.backSorter.objectAtIndex(0);
    		sortStyle = null;
    	}
    	sorter.addObject(so);
    	EOSortOrdering.sortArrayUsingKeyOrderArray(list, sorter);
    }
    
    public void sortByGroup() {
    	EOSortOrdering so = (EOSortOrdering)sorter.lastObject();
    	if(so == null || !so.key().equals(MarkArchive.TIMESTAMP_KEY))
    		so = (EOSortOrdering)MarkArchive.backSorter.objectAtIndex(0);
    	sorter.removeAllObjects();
    	if(sortStyle == null || sortStyle.valueForKey("group") == null) {
    		sorter.addObject(new EOSortOrdering("grade",EOSortOrdering.CompareAscending));
    		sorter.addObject(new EOSortOrdering("group",EOSortOrdering.CompareCaseInsensitiveAscending));
    		sortStyle = new NSDictionary("selection","group");
    	} else {
    		so = (EOSortOrdering)MarkArchive.backSorter.objectAtIndex(0);
    		sortStyle = null;
    	}
    	sorter.addObject(so);
    	EOSortOrdering.sortArrayUsingKeyOrderArray(list, sorter);
    }

    public void sortByTeacher() {
    	EOSortOrdering so = (EOSortOrdering)sorter.lastObject();
    	if(so == null || !so.key().equals(MarkArchive.TIMESTAMP_KEY))
    		so = (EOSortOrdering)MarkArchive.backSorter.objectAtIndex(0);
    	sorter.removeAllObjects();
    	if(sortStyle == null || sortStyle.valueForKey("teacher") == null) {
    		sorter.addObject(new EOSortOrdering("teacherName",
    				EOSortOrdering.CompareCaseInsensitiveAscending));
    		sortStyle = new NSDictionary("selection","teacher");
    	} else {
    		so = (EOSortOrdering)MarkArchive.backSorter.objectAtIndex(0);
    		sortStyle = null;
    	}
    	sorter.addObject(so);
    	EOSortOrdering.sortArrayUsingKeyOrderArray(list, sorter);
    }

    public void sortByUser() {
    	EOSortOrdering so = (EOSortOrdering)sorter.lastObject();
    	if(so == null || !so.key().equals(MarkArchive.TIMESTAMP_KEY))
    		so = (EOSortOrdering)MarkArchive.backSorter.objectAtIndex(0);
    	sorter.removeAllObjects();
    	if(sortStyle == null || sortStyle.valueForKey("user") == null) {
    		sorter.addObject(new EOSortOrdering(MarkArchive.USER_KEY,
    				EOSortOrdering.CompareCaseInsensitiveAscending));
    		sortStyle = new NSDictionary("selection","user");
    	} else {
    		so = (EOSortOrdering)MarkArchive.backSorter.objectAtIndex(0);
    		sortStyle = null;
    	}
    	sorter.addObject(so);
    	EOSortOrdering.sortArrayUsingKeyOrderArray(list, sorter);
    }
    
    protected void prepareList(NSArray extraQuals) {
    	if(records.count() == 0) {
    		list = null;
    		return;
    	}
    	if(extraQuals == null)
    		extraQuals = (NSArray)params.valueForKey("extraQuals");
    	NSMutableArray quals = new NSMutableArray();
    	if(extraQuals != null && extraQuals.count() > 0) {
    		Enumeration enu = extraQuals.objectEnumerator();
    		while (enu.hasMoreElements()) {
    			NSDictionary qd = (NSDictionary) enu.nextElement();
    			if(Various.boolForObject(qd.valueForKey("used")))
    					continue;
    			EOQualifier qual = (EOQualifier)qd.valueForKey("filter");
    			if(qual != null)
    				quals.addObject(qual);
    		}
    	}
    	list = records.mutableClone();
    	if("selection".equals(params.valueForKey("warnings")))
    		quals.addObject(new EOKeyValueQualifier("teacherClass",
    				EOQualifier.QualifierOperatorEqual,"warning"));
    	if(quals.count() > 0) {
    		EOQualifier qual;
    		if(quals.count() == 1)
    			qual = (EOQualifier)quals.objectAtIndex(0);
    		else
    			qual = new EOAndQualifier(quals);
    		EOQualifier.filterArrayWithQualifier(list, qual);
    	}
    	EOSortOrdering.sortArrayUsingKeyOrderArray(list, sorter);
    }
    
    public void filterUser() {
    	Object value = valueForKeyPath("item.user");
    	setFilterUser(value);
    }
    
    public void setFilterUser(Object value) {
    	NSMutableDictionary byKey = (NSMutableDictionary)params.valueForKey("byKey");
    	if(byKey == null) {
    		byKey = new NSMutableDictionary();
    		params.takeValueForKey(byKey, "byKey");
    	}
    	NSMutableDictionary dict = (NSMutableDictionary)byKey.valueForKey(MarkArchive.USER_KEY);
    	if(dict == null) {
    		dict = new NSMutableDictionary(MarkArchive.USER_KEY,"attribute");
    		byKey.takeValueForKey(dict, MarkArchive.USER_KEY);
        	dict.takeValueForKey(
        			session().valueForKeyPath("strings.RujelArchiving_Archive.author"), "title");
        	dict.takeValueForKey("ungerade", "styleClass");
    	}
    	dict.takeValueForKey(value, "value");
    	EOQualifier qual = new EOKeyValueQualifier(MarkArchive.USER_KEY, 
    			EOQualifier.QualifierOperatorEqual, value);
    	dict.takeValueForKey(qual, "qualifier");
    	dict.takeValueForKey(qual, "filter");
    	if(valueForKeyPath("sortStyle.user") != null) {
        	EOSortOrdering so = (EOSortOrdering)sorter.lastObject();
        	if(so == null || !so.key().equals(MarkArchive.TIMESTAMP_KEY))
        		so = (EOSortOrdering)MarkArchive.backSorter.objectAtIndex(0);
        	sorter.removeAllObjects();
        	sorter.addObject(so);
    		sortStyle = null;
    	}
    	NSMutableArray extraQuals = (NSMutableArray)params.valueForKey("extraQuals");
    	if(extraQuals == null) {
    		extraQuals = new NSMutableArray(dict);
    		params.takeValueForKey(extraQuals, "extraQuals");
    	} else {
    		for (int i = 0; i < extraQuals.count(); i++) {
    			NSMutableDictionary exist = (NSMutableDictionary)extraQuals.objectAtIndex(i);
				if(MarkArchive.USER_KEY.equals(exist.valueForKey("attribute"))) {
					if(value.equals(exist.valueForKey("value")))
						return;
					extraQuals.replaceObjectAtIndex(dict, i);
			    	prepareList(extraQuals);
					return;
				}
			}
			extraQuals.addObject(dict);
    	}
    	prepareList(extraQuals);
    }

    public void filterCourse() {
    	EduCourse course = (EduCourse)valueForKeyPath("item.course");
    	setFilterCourse(course);
    }
    public void setFilterCourse(EduCourse course) {
    	NSMutableDictionary byKey = (NSMutableDictionary)params.valueForKey("byKey");
    	if(byKey == null) {
    		byKey = new NSMutableDictionary();
    		params.takeValueForKey(byKey, "byKey");
    	}
    	NSMutableDictionary dict = (NSMutableDictionary)byKey.valueForKey("course");
    	if(dict == null) {
    		dict = new NSMutableDictionary("course","attribute");
    		byKey.takeValueForKey(dict, "course");
        	dict.takeValueForKey(session().valueForKeyPath(
        			"strings.RujelInterfaces_Names.EduCourse.this"), "title");
        	dict.takeValueForKey("gerade", "styleClass");
    	}
    	StringBuilder value = new StringBuilder();
    	value.append(course.eduGroup().name()).append(" ; ");
    	value.append(course.subjectWithComment()).append(" ; ");
    	if(course.teacher() == null)
    		value.append("<em>").append(
    				session().valueForKeyPath("strings.RujelBase_Base.vacant")).append("</em>");
    	else
    		value.append(Person.Utility.fullName(course.teacher(), true, 2, 1, 1));
    	dict.takeValueForKey(value, "value");
    	EOQualifier qual = new EOKeyValueQualifier("course", 
    			EOQualifier.QualifierOperatorEqual, course);
    	dict.takeValueForKey(qual, "filter");
    	if(valueForKeyPath("sortStyle.subject") != null) {
        	EOSortOrdering so = (EOSortOrdering)sorter.lastObject();
        	if(so == null || !so.key().equals(MarkArchive.TIMESTAMP_KEY))
        		so = (EOSortOrdering)MarkArchive.backSorter.objectAtIndex(0);
        	sorter.removeAllObjects();
        	sorter.addObject(so);
    		sortStyle = null;
    	}
    	NSMutableArray extraQuals = (NSMutableArray)params.valueForKey("extraQuals");
    	if(extraQuals == null) {
    		extraQuals = new NSMutableArray(dict);
    		params.takeValueForKey(extraQuals, "extraQuals");
    	} else {
    		for (int i = 0; i < extraQuals.count(); i++) {
    			NSMutableDictionary exist = (NSMutableDictionary)extraQuals.objectAtIndex(i);
				if("course".equals(exist.valueForKey("attribute"))) {
					if(value.equals(exist.valueForKey("value")))
						return;
					extraQuals.replaceObjectAtIndex(dict, i);
			    	prepareList(extraQuals);
					return;
				}
			}
			extraQuals.addObject(dict);
    	}
    	prepareList(extraQuals);
    }
    
    public void removeExtra() {
    	NSMutableDictionary byKey = (NSMutableDictionary)params.valueForKey("byKey");
    	if(byKey != null) {
    		String key = (String)valueForKeyPath("item.attribute");
    		byKey.takeValueForKey(null, key);
    	}
    	NSMutableArray extraQuals = (NSMutableArray)params.valueForKey("extraQuals");
    	if(extraQuals == null)
    		return;
    	extraQuals.removeIdenticalObject(item);
    	if(Various.boolForObject(valueForKeyPath("item.used"))) {
    		list = null;
        	records.removeAllObjects();
    	} else {
    		prepareList(extraQuals);
    	}
    }
    
    public void toggleWarnings() {
    	if("selection".equals(params.valueForKey("warnings")))
    		params.takeValueForKey("grey", "warnings");
    	else
    		params.takeValueForKey("selection", "warnings");
    	prepareList(null);
    }

	public WOActionResults expand() {
		WOComponent inspector = pageWithName("ArchiveInspector");
		inspector.takeValueForKey(this, "returnPage");
		inspector.takeValueForKey(item, "dict");
		return inspector;
	}
	
	public boolean cantFilterCourse() {
		return (valueForKeyPath("item.course") == null ||
				valueForKeyPath("params.byKey.course") != null);
	}
}