package net.rujel.autoitog;

import java.math.BigDecimal;
import java.util.Enumeration;

import net.rujel.base.MyUtility;
import net.rujel.criterial.Mark;
import net.rujel.criterial.Work;
import net.rujel.eduresults.ItogContainer;
import net.rujel.eduresults.ItogType;
import net.rujel.interfaces.EduCourse;
import net.rujel.interfaces.EduLesson;
import net.rujel.interfaces.Student;
import net.rujel.reusables.Various;

import com.webobjects.eoaccess.EOUtilities;
import com.webobjects.eocontrol.*;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSDictionary;
import com.webobjects.foundation.NSMutableArray;
import com.webobjects.foundation.NSMutableDictionary;
import com.webobjects.foundation.NSTimestamp;

public abstract class WorkCalculator extends Calculator {

	protected static final NSArray reliesOn = new NSArray(new String[] {"Work","Mark"});

	public WorkCalculator() {
		super();
	}

/*	public NSArray reliesOn() {
		return reliesOn;
	}*/

	public String reliesOnEntity() {
		return "Work";
	}

	protected static NSArray marksForStudentAndWorks(Student student, NSArray works) {
		NSMutableArray quals = new NSMutableArray(Various.getEOInQualifier("work",works));
		quals.addObject(new EOKeyValueQualifier("student",EOQualifier.QualifierOperatorEqual,student));
		
		EOFetchSpecification fs = new EOFetchSpecification("Mark",new EOAndQualifier(quals),null);
		fs.setRefreshesRefetchedObjects(true);
		return student.editingContext().objectsWithFetchSpecification(fs);
	}
	
	public NSArray collectRelated(EduCourse course, AutoItog autoItog, 
			boolean omitMentioned, boolean prepareEc) {
		if(!autoItog.calculatorName().equals(this.getClass().getName()))
			throw new IllegalStateException("Should be applied to AutoItog related to this Calculator");
		EOEditingContext ec = autoItog.editingContext();
		NSTimestamp fireDate = autoItog.fireDate();
		CourseTimeout cto = CourseTimeout.getTimeoutForCourseAndPeriod(course, 
				autoItog.itogContainer());
		if(cto != null)
			fireDate = cto.fireDate();
		EOQualifier[] quals = new EOQualifier[2];
		quals[0] = new EOKeyValueQualifier(Work.DATE_KEY,(autoItog.evening())?
				EOQualifier.QualifierOperatorLessThanOrEqualTo:
					EOQualifier.QualifierOperatorLessThan,fireDate);
		quals[1] = new EOKeyValueQualifier("course",
				EOQualifier.QualifierOperatorEqual,course);
//		if(checkWeight) {
			NSMutableArray allQuals = new NSMutableArray(quals);
			allQuals.addObject(new EOKeyValueQualifier(Work.WEIGHT_KEY,
					EOQualifier.QualifierOperatorGreaterThan,BigDecimal.ZERO));
			quals[0] = new EOAndQualifier(allQuals); 
//		} else {
//			quals[0] = new EOAndQualifier(new NSArray(quals));
//		}
		EOFetchSpecification fs = new EOFetchSpecification(Work.ENTITY_NAME,quals[0],null);
		NSArray works = ec.objectsWithFetchSpecification(fs);
		if(works == null || works.count() == 0)
			return null;
		NSMutableArray mentioned = null;
		if(omitMentioned) {
			mentioned = new NSMutableArray();
			NSArray aiList = EOUtilities.objectsMatchingKeyAndValue(ec,
					AutoItog.ENTITY_NAME, AutoItog.LIST_NAME_KEY, autoItog.listName());
			if(aiList != null && aiList.count() > 0) {
				Enumeration enu = aiList.objectEnumerator();
				ItogContainer itog = autoItog.itogContainer();
				ItogType type = itog.itogType();
				Integer eduYear = itog.eduYear();
				while (enu.hasMoreElements()) {
					AutoItog ai = (AutoItog) enu.nextElement();
					if(ai == autoItog || ai.itogContainer().itogType() != type 
							|| !ai.itogContainer().eduYear().equals(eduYear))
						continue;
					if(!getClass().getName().equals(ai.calculatorName()) && 
							!ai.calculator().reliesOnEntity().equals(reliesOnEntity()))
						continue;
					NSArray found = ai.relKeysForCourse(course);
					if(found != null && found.count() > 0)
						mentioned.addObjectsFromArray((NSArray)found.valueForKey("relKey"));
				}
			}
		}
			/*
		quals[0] = new EOKeyValueQualifier("itogContainer.itogType",
				EOQualifier.QualifierOperatorEqual,autoItog.itogContainer().itogType());
		EOQualifier.filterArrayWithQualifier(aiList, quals[0]);
		aiList.removeObject(autoItog);
//		EOSortOrdering.sortArrayUsingKeyOrderArray(aiList, AutoItog.sorter);
		quals[0] = Various.getEOInQualifier("autoItog", aiList);
		allQuals.removeAllObjects();
		allQuals.addObjects(quals);
		quals[0] = new EOAndQualifier(allQuals);
		fs.setEntityName("ItogRelated");
		fs.setQualifier(quals[0]);
		NSArray mentioned = ec.objectsWithFetchSpecification(fs);
		if(mentioned != null && mentioned.count() > 0)
			mentioned = (NSArray)mentioned.valueForKey("relKey");
		else
			mentioned = null; */
		quals[0] = new EOKeyValueQualifier("autoItog",EOQualifier.QualifierOperatorEqual,
				autoItog);
		allQuals.removeAllObjects();
		allQuals.addObjects(quals);
		quals[0] = new EOAndQualifier(allQuals);
		fs.setEntityName("ItogRelated");
		fs.setQualifier(quals[0]);
		NSMutableDictionary forNum = null;
		if(prepareEc) {
			NSArray already = ec.objectsWithFetchSpecification(fs);
			if(already != null && already.count() > 0)
				forNum = new NSMutableDictionary(already,
						(NSArray)already.valueForKey("relKey"));
		}
		Enumeration enu = works.objectEnumerator();
		NSMutableArray result = new NSMutableArray();
		while (enu.hasMoreElements()) {
			Work work = (Work) enu.nextElement();
			if(work.marks() == null || work.marks().count() == 0)
				continue;
			EOKeyGlobalID gid = (EOKeyGlobalID)ec.globalIDForObject(work);
			Object key = gid.keyValues()[0];
			if(omitMentioned && mentioned.containsObject(key))
				continue;
			result.addObject(work);
			if(!prepareEc)
				continue;
			if(forNum == null || forNum.removeObjectForKey(key) == null) {
				EOEnterpriseObject ir = EOUtilities.createAndInsertInstance(ec, "ItogRelated");
				ir.addObjectToBothSidesOfRelationshipWithKey(autoItog, "autoItog");
				ir.addObjectToBothSidesOfRelationshipWithKey(course, "course");
				ir.takeValueForKey(key, "relKey");
			}
		}
		if(prepareEc && forNum != null && forNum.count() > 0) {
			enu = forNum.objectEnumerator();
			while (enu.hasMoreElements()) {
				EOEnterpriseObject ir = (EOEnterpriseObject) enu.nextElement();
				ec.deleteObject(ir);
			}
		}
		return result;
	}
	
	public Integer relKeyForObject(Object object) {
		if(object instanceof Integer)
			return (Integer)object;
		Work work = null;
		if (object instanceof Work) {
			work = (Work) object;
		} else if (object instanceof Mark) {
			work = ((Mark)object).work();
		} else if(object instanceof NSDictionary) {
			NSDictionary dict = (NSDictionary)object;
			EduLesson lesson = (EduLesson)dict.valueForKey("lesson");
			if(lesson instanceof Work) {
				work = (Work)lesson;
			} else if(lesson == null) {
				String entityName = (String)dict.valueForKey("entityName");
				if(!Work.ENTITY_NAME.equals(entityName))
					return null;
				NSDictionary pKey = (NSDictionary)dict.valueForKey("pKey");
				return (pKey == null)?null:
					(Integer)pKey.allValues().objectAtIndex(0);
			} else {
				return null;
			}
		} else {
			return null;
		}
		try {
			if(work.weight().compareTo(BigDecimal.ZERO) == 0)
				return null;
			EOKeyGlobalID gid = (EOKeyGlobalID)work.editingContext().
								globalIDForObject(work);
			return (Integer)gid.keyValues()[0];
		} catch (Exception e) {
			return null;
		}
	}
	
	public boolean skipAutoAdd(Integer relKey, EOEditingContext ec) {
		if(relKey == null || ec == null)
			return true;
		try {
			Work work = (Work)EOUtilities.objectWithPrimaryKeyValue(ec,
					Work.ENTITY_NAME, relKey);
			if(work.marks().count() == 0 ||
					work.weight().compareTo(BigDecimal.ZERO) == 0)
				return true;
		} catch (Exception e) {
			return true;
		}
		return false;
	}

	public NSMutableDictionary describeObject(Object object) {
		if(object == null)
			return null;
		Work work = null;
		if(object instanceof Work) {
			work = (Work)object;
		} else if (object instanceof Mark) {
			work = ((Mark)object).work();
		} else if(object instanceof NSDictionary) {
			NSDictionary dict = (NSDictionary)object;
			EduLesson lesson = (EduLesson)dict.valueForKey("lesson");
			if(lesson instanceof Work) {
				work = (Work)lesson;
			} else if(lesson == null) {
				String entityName = (String)dict.valueForKey("entityName");
				if(!Work.ENTITY_NAME.equals(entityName))
					return null;
				NSDictionary pKey = (NSDictionary)dict.valueForKey("pKey");
				work = (Work)EOUtilities.objectWithPrimaryKey(
						new EOEditingContext(), entityName, pKey);
			}
		} else if (object instanceof Integer) {
			work = (Work)EOUtilities.objectWithPrimaryKeyValue(
					new EOEditingContext(), Work.ENTITY_NAME, object);
		} else {
			return null;
		}
		String title = work.title();
		if(title == null)
			title = MyUtility.dateFormat().format(work.date());
		NSMutableDictionary result = new NSMutableDictionary(title,"title");
		result.takeValueForKey(work.theme(), "description");
		result.takeValueForKey(work.color(), "color");
		result.takeValueForKey(work.valueForKeyPath("workType.typeName"), "hover");
		return result;
	}
}