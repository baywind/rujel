package net.rujel.rest;

import java.math.BigDecimal;
import java.util.Enumeration;

import net.rujel.autoitog.AutoItog;
import net.rujel.autoitog.Prognosis;
import net.rujel.base.MyUtility;
import net.rujel.eduresults.ItogContainer;
import net.rujel.interfaces.EduCourse;
import net.rujel.rest.Agregator.ParseError;
import net.rujel.reusables.SettingsReader;

import com.webobjects.eocontrol.EOAndQualifier;
import com.webobjects.eocontrol.EOEnterpriseObject;
import com.webobjects.eocontrol.EOKeyValueQualifier;
import com.webobjects.eocontrol.EOQualifier;
import com.webobjects.eocontrol.EOSortOrdering;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSDictionary;
import com.webobjects.foundation.NSMutableArray;
import com.webobjects.foundation.NSTimestamp;

public class AgrPrognosis extends AgrEntity {
	
	private static final String[] attributes = new String[] {
		"eduYear","perCount","perNum","grade","subject","course","student","mark","state","value","form"};
	
	public String entityName() {
		return Prognosis.ENTITY_NAME;
	}
	
	public NSArray attributes() {
		return new NSArray(attributes);
	}

	public Enumeration getObjectsEnumeration(NSDictionary params) throws ParseError {
		NSMutableArray quals = new NSMutableArray();
		AgrEduCourse courseAgregator = new AgrEduCourse();
		courseAgregator.ec = ec;
		final Enumeration courses = courseAgregator.getObjectsEnumeration(params);
		//eduYear is added to params
		if(courses == null)
			return null;
		
		String txt = (String)params.valueForKey("eduYear");
		addIntToQuals(quals, ItogContainer.EDU_YEAR_KEY, txt);
		boolean defaultNum = true;
		txt = (String)params.valueForKey("perCount");
		if(txt != null) {
			if(txt.equals("0")) {
				defaultNum = false;
				quals.addObject(new EOKeyValueQualifier("itogType.inYearCount", 
					EOQualifier.QualifierOperatorEqual, new Integer(1)));
			} else if(txt.equals("1")) {
				defaultNum = false;
				quals.addObject(new EOKeyValueQualifier("itogType.inYearCount", 
						EOQualifier.QualifierOperatorEqual, new Integer(1)));
				quals.addObject(new EOKeyValueQualifier("itogType.title", 
						EOQualifier.QualifierOperatorCaseInsensitiveLike, "*год*"));				
			} else {
				addIntToQuals(quals, "itogType.inYearCount", txt);
				defaultNum = true; //TODO:  smarter decision
			}
		}
		txt = (String)params.valueForKey("perNum");
		addIntToQuals(quals, ItogContainer.NUM_KEY, txt);
		final boolean getDefault = defaultNum && (txt == null);
		final EOQualifier itogQual = new EOAndQualifier(quals);
		quals.removeAllObjects();
		final NSArray sorter = new NSArray(new EOSortOrdering[] {
				new EOSortOrdering(ItogContainer.EDU_YEAR_KEY,EOSortOrdering.CompareAscending),
				new EOSortOrdering("itogType.inYearCount",EOSortOrdering.CompareDescending),
				new EOSortOrdering(ItogContainer.NUM_KEY,EOSortOrdering.CompareAscending),
				new EOSortOrdering("itogType.sort",EOSortOrdering.CompareDescending)
		});
		final NSTimestamp today;
		String defaultDate = SettingsReader.stringForKeyPath("ui.defaultDate", null);
		if(defaultDate == null) {
			today = new NSTimestamp();
		} else {
			NSTimestamp parced;
			try {
				parced = (NSTimestamp)MyUtility.dateFormat().parseObject(defaultDate);
			} catch (Exception e) {
				parced = new NSTimestamp();
			}
			today = parced;
		}
		NSMutableArray containers = null;
		{
		EduCourse course = null;
		while (courses.hasMoreElements() && (containers == null || containers.count() == 0)) {
			Wrapper wrapped = (Wrapper)courses.nextElement();
			course = (EduCourse) wrapped.obj;
			if(getDefault) {
				NSArray autoItogs = AutoItog.currentAutoItogsForCourse(course, today);
				containers = (NSMutableArray)autoItogs.valueForKey(AutoItog.ITOG_CONTAINER_KEY);
			} else {
				containers = (NSMutableArray)ItogContainer.itogsForCourse(course);
			}
			EOQualifier.filterArrayWithQualifier(containers, itogQual);
			EOSortOrdering.sortArrayUsingKeyOrderArray(containers, sorter);
		}
		if(containers == null || containers.count() == 0)
			return null;
		quals.addObject(new EOKeyValueQualifier("course", 
				EOQualifier.QualifierOperatorEqual,course));
		}
		txt = (String)params.valueForKey("state");
		addIntToQuals(quals, Prognosis.STATE_KEY, txt);

		txt = (String)params.valueForKey("mark");
		if(txt != null) {
			EOQualifier qual = getStringQual(Prognosis.MARK_KEY, txt);
			if(qual != null)
			quals.addObject(qual);
		}
		txt = (String)params.valueForKey("value");
		addDecToQuals(quals, Prognosis.VALUE_KEY, txt);
				
		//TODO: qualifiers for student and course
		
		return new RowsEnum(this, quals,Prognosis.ITOG_CONTAINER_KEY,containers) {
			protected boolean nextIteration() {
				if(super.nextIteration())
					return true;
				restart();
				NSMutableArray itogs = null;
				while (courses.hasMoreElements() && (itogs == null || itogs.count() == 0)) {
					Wrapper wrapped = (Wrapper)courses.nextElement();
					EduCourse course = (EduCourse) wrapped.obj;
					quals.replaceObjectAtIndex(new EOKeyValueQualifier("course", 
							EOQualifier.QualifierOperatorEqual,course),0);
					if(getDefault) {
						NSArray autoItogs = AutoItog.currentAutoItogsForCourse(course, today);
						itogs = (NSMutableArray)autoItogs.valueForKey(
								AutoItog.ITOG_CONTAINER_KEY);
					} else {
						itogs = (NSMutableArray)ItogContainer.itogsForCourse(course);
					}
					EOQualifier.filterArrayWithQualifier(itogs, itogQual);
					EOSortOrdering.sortArrayUsingKeyOrderArray(itogs, sorter);
				}
				if(itogs == null || itogs.count() == 0)
					return false;
				itrValues[0] = itogs;
				return super.nextIteration();
			}
		};
	}
	
	public Object getValue(EOEnterpriseObject obj, String attribute) {
		if(!(obj instanceof Prognosis)) {
			return "???";
		}
		Prognosis pr = (Prognosis)obj;
		if(attribute.equals("subject"))
			return pr.valueForKeyPath("course.cycle.subject");
		if(attribute.equals("grade"))
			return pr.valueForKeyPath("course.cycle.grade");
		if(attribute.equals("eduYear"))
			return pr.course().eduYear();
		if(attribute.equals("course")) //wrap
			return new AgrEduCourse.Wrapper(pr.course());
		if(attribute.equals("student")) //wrap
//			return mark.student();
//			return WOLogFormatter.formatEO(mark.student());
			return new Wrapper(pr.student());
		if(attribute.equals("perNum"))
			return pr.itogContainer().num();
		if(attribute.equals("perCount")) {
			Integer inYear = pr.itogContainer().itogType().inYearCount();
			if(inYear.intValue() == 1) {
				if(!pr.itogContainer().itogType().title().toLowerCase().contains("год"))
					return new Integer(0);
			}
			return inYear;
		}
		if(attribute.equals("mark"))
			return pr.mark();
		if(attribute.equals("state"))
			return pr.state();
		if(attribute.equals("value")) {
			BigDecimal val = pr.value();
			if(val.compareTo(BigDecimal.ONE) == 0)
				return new Integer(1);
			if(val.compareTo(BigDecimal.ZERO) == 0)
				return new Integer(0);
			return val;
		}
		if(attribute.equals("form")) {
			return pr.valueForKeyPath("course.eduGroup.name");
		}
		throw new IllegalArgumentException("Unknown attribute '" + attribute + '\'');
	}
}
