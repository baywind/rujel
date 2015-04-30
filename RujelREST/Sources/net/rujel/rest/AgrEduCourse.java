package net.rujel.rest;

import java.util.Enumeration;

import net.rujel.base.BaseCourse;
import net.rujel.base.MyUtility;
import net.rujel.base.SettingsBase;
import net.rujel.eduplan.PlanCycle;
import net.rujel.eduplan.Subject;
import net.rujel.eduresults.ItogMark;
import net.rujel.eduresults.ItogType;
import net.rujel.interfaces.EduCourse;
import net.rujel.interfaces.EduGroup;
import net.rujel.interfaces.Person;
import net.rujel.interfaces.Teacher;
import net.rujel.rest.Agregator.ParseError;
import net.rujel.reusables.Various;

import com.webobjects.appserver.WOApplication;
import com.webobjects.eocontrol.EOAndQualifier;
import com.webobjects.eocontrol.EOEditingContext;
import com.webobjects.eocontrol.EOEnterpriseObject;
import com.webobjects.eocontrol.EOFetchSpecification;
import com.webobjects.eocontrol.EOKeyValueQualifier;
import com.webobjects.eocontrol.EOOrQualifier;
import com.webobjects.eocontrol.EOQualifier;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSDictionary;
import com.webobjects.foundation.NSMutableArray;
import com.webobjects.foundation.NSMutableDictionary;
import com.webobjects.foundation.NSSelector;

public class AgrEduCourse extends AgrEntity {

	private NSMutableDictionary<String, Integer> resultsBylist = new NSMutableDictionary();
	
	private static final String[] attributes = new String[] {
		"eduYear","subject","grade","form","teacher","results"};

	public String entityName() {
		return EduCourse.entityName;
	}

	public NSArray attributes() {
		return new NSArray(attributes);
	}

	@Override
	public Enumeration getObjectsEnumeration(NSDictionary params)
			throws ParseError {
		NSMutableArray quals = new NSMutableArray();
		String txt = (String)params.valueForKey("eduYear");
		if(txt == null) {
			Integer year = (Integer)WOApplication.application().valueForKey("year");
			if(year == null)
				year = MyUtility.eduYearForDate(null);
			quals.addObject(new EOKeyValueQualifier(BaseCourse.EDU_YEAR_KEY, 
					EOQualifier.QualifierOperatorEqual, year));
			params.takeValueForKey(year.toString(), "eduYear");
		} else {
			addIntToQuals(quals, BaseCourse.EDU_YEAR_KEY, txt);
		}
		txt = (String)params.valueForKey("form");
		final EOQualifier formQual = getStringQual("form", txt);
		if(formQual != null && params.valueForKey("grade") == null) {
			NSArray list = groupsForForm(txt, ec);
			if(list == null)
				return null;
			quals.addObject(Various.getEOInQualifier("eduGroup", list));
			list = (NSArray)list.valueForKey("grade");
			params.takeValueForKey(list.toString(), "grade");
		}
		NSArray cycles = cyclesForParams(params,ec);
		if(cycles == null || cycles.count() == 0)
			return null;
//		NSMutableDictionary iterate = new NSMutableDictionary(cycles,"cycle");
		
		// TODO: qualifier for Teacher

		txt = (String)params.valueForKey("results");
		final Object[] snv = selectorAndValue(txt);
		return new RowsEnum(this, quals, "cycle",cycles) {
			protected boolean qualifies(Wrapper obj) {
				if(formQual != null && !formQual.evaluateWithObject(obj))
					return false;
				if(snv != null) {
					Integer res = getResults((EduCourse)obj.obj);
					if(snv[0] == EOQualifier.QualifierOperatorEqual) {
						for (int i = 1; i < snv.length; i++) {
							if(res.intValue() == Integer.parseInt((String)snv[i]))
								return true;
						}
						return false;
					}
					if(!EOQualifier.ComparisonSupport.compareValues(res,new Integer((String)snv[1]), 
							(NSSelector)snv[0]))
						return false;
//					EOKeyValueQualifier qual = new EOKeyValueQualifier("results", 
//							(NSSelector)snv[0], new Integer((String)snv[1]));
//					if(!qual.evaluateWithObject(obj))
//						return false;
					if(snv.length >= 4) {
//						qual = new EOKeyValueQualifier("results", 
//								(NSSelector)snv[2], new Integer((String)snv[3]));
//						if(!qual.evaluateWithObject(obj))
//							return false;
						return EOQualifier.ComparisonSupport.compareValues(res,
								new Integer((String)snv[3]), (NSSelector)snv[2]);
					}
				}
				return true;
			}
		};
	}
	
	public static NSArray groupsForForm(String form, EOEditingContext ec)  throws ParseError {
		if(form == null)
			return null;
		NSArray list = EduGroup.Lister.listGroups(MyUtility.date(ec), ec);
		if(list == null || list.count() == 0)
			return null;
		EOQualifier qual = getStringQual("name", form);
		if(qual == null)
			return null;
		list = EOQualifier.filteredArrayWithQualifier(list, qual);
		if(list == null || list.count() == 0)
			return null;
		return list;
	}

	public static NSArray cyclesForParams(NSDictionary params, EOEditingContext ec) throws ParseError {
		String txt;
		NSMutableArray cycleQuals = new NSMutableArray();
		txt = (String)params.valueForKey("grade");
		addIntToQuals(cycleQuals, PlanCycle.GRADE_KEY, txt);
		txt = (String)params.valueForKey("subject");
		if(txt != null && txt.length() > 0) {
			EOQualifier squal;
			if(txt.charAt(0) == '{') {
				squal = getStringQual(Subject.SUBJECT_KEY, txt);
			} else {
				squal = new EOOrQualifier(new NSArray(new EOKeyValueQualifier[] {
						new EOKeyValueQualifier(Subject.SUBJECT_KEY,
								EOQualifier.QualifierOperatorCaseInsensitiveLike,txt),
								new EOKeyValueQualifier(Subject.FULL_NAME_KEY,
										EOQualifier.QualifierOperatorCaseInsensitiveLike,txt)					
				}));
			}
			EOFetchSpecification fs = new EOFetchSpecification(Subject.ENTITY_NAME,squal,null);
			NSArray subjects = ec.objectsWithFetchSpecification(fs);
			if(subjects == null || subjects.count() == 0)
				return null;
			cycleQuals.addObject(Various.getEOInQualifier(PlanCycle.SUBJECT_EO_KEY, subjects));
		}
		EOQualifier cqual = (cycleQuals.count() == 0)? null : (cycleQuals.count() == 1)? 
			 (EOQualifier)cycleQuals.objectAtIndex(0): new EOAndQualifier(cycleQuals);
		EOFetchSpecification fs = new EOFetchSpecification(PlanCycle.ENTITY_NAME,cqual,null);
		NSArray found = ec.objectsWithFetchSpecification(fs);
		return found;
	}

	@Override
	public Object getValue(EOEnterpriseObject obj, String attribute) {
		if(obj instanceof Teacher) {
			if(attribute.equals("name"))
				return Person.Utility.fullName((Teacher)obj, true, 2, 2, 2);
		}
		if(!(obj instanceof EduCourse))
			return "???";
		EduCourse course = (EduCourse)obj;
		if(attribute.equals("eduYear"))
			return course.eduYear();
		if(attribute.equals("subject"))
			return course.cycle().subject();
		if(attribute.equals("grade"))
			return course.cycle().grade();
		if(attribute.equals("form"))
			return course.valueForKeyPath("eduGroup.name");
		if(attribute.equals("teacher")) {
			if(course.teacher() == null)
				return null;
			return new Wrapper(course.teacher());
		}
		if(attribute.equals("results")) {
			return getResults(course);
		}
		return null;
	}

	public Integer getResults(EduCourse course) {
		String listName = SettingsBase.stringSettingForCourse(ItogMark.ENTITY_NAME, course, ec);
		Integer res = resultsBylist.objectForKey(listName);
		if(res == null) {
			NSArray types = ItogType.typesForList(listName, course.eduYear(), ec);
			if(types == null || types.count() == 0) {
				res = new Integer(0);
			} else {
				res = (Integer)types.valueForKeyPath("@max.inYearCount");
			}
			if(res == null)
				res = new Integer(0);
			else if(res.intValue() == 0)
				res = new Integer(1);
			resultsBylist.setObjectForKey(res, listName);
		}
		return res;
	}

	public boolean qualifies(EOEnterpriseObject obj) {
		return true;
	}
}
