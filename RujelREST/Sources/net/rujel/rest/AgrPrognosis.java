package net.rujel.rest;

import java.math.BigDecimal;
import java.util.Enumeration;

import net.rujel.autoitog.AutoItog;
import net.rujel.autoitog.Prognosis;
import net.rujel.base.MyUtility;
import net.rujel.base.SettingsBase;
import net.rujel.eduresults.ItogContainer;
import net.rujel.eduresults.ItogMark;
import net.rujel.eduresults.ItogType;
import net.rujel.interfaces.EduCourse;
import net.rujel.rest.Agregator.ParseError;
import net.rujel.reusables.SettingsReader;

import com.webobjects.eocontrol.EOAndQualifier;
import com.webobjects.eocontrol.EOEnterpriseObject;
import com.webobjects.eocontrol.EOFetchSpecification;
import com.webobjects.eocontrol.EOKeyValueQualifier;
import com.webobjects.eocontrol.EOQualifier;
import com.webobjects.eocontrol.EOSortOrdering;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSDictionary;
import com.webobjects.foundation.NSMutableArray;
import com.webobjects.foundation.NSMutableDictionary;
import com.webobjects.foundation.NSTimestamp;

public class AgrPrognosis extends AgrEntity {
	
	protected static final String[] attributes = new String[] {"eduYear","perCount","perNum",
		"grade","subject","course","student","mark","state","value","form"};
	
	protected static final NSArray itogSorter = new NSArray(new EOSortOrdering[] {
			new EOSortOrdering(ItogContainer.EDU_YEAR_KEY,EOSortOrdering.CompareAscending),
			new EOSortOrdering("itogType.inYearCount",EOSortOrdering.CompareDescending),
			new EOSortOrdering(ItogContainer.NUM_KEY,EOSortOrdering.CompareAscending),
			new EOSortOrdering("itogType.sort",EOSortOrdering.CompareDescending)
	});

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
		if(courses == null || !courses.hasMoreElements())
			return null;
		
		String txt = (String)params.valueForKey("eduYear");
		final Integer eduYear = new Integer(txt);
		quals.addObject(new EOKeyValueQualifier(ItogContainer.EDU_YEAR_KEY,
				EOQualifier.QualifierOperatorEqual, eduYear));
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
		quals.addObject(NSArray.EmptyArray);
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
		
		return new RowsEnum(this, quals,Prognosis.ITOG_CONTAINER_KEY,NSArray.EmptyArray) {
			private NSMutableDictionary<String, NSArray> itogByListName = new NSMutableDictionary();
			private SettingsBase settings = SettingsBase.baseForKey(ItogMark.ENTITY_NAME, ec, false);
			
			protected boolean nextIteration() {
				if(super.nextIteration())
					return true;
				restart();
				NSArray itogs = null;
				while (courses.hasMoreElements() && (itogs == null || itogs.count() == 0)) {
					Wrapper wrapped = (Wrapper)courses.nextElement();
					EduCourse course = (EduCourse) wrapped.obj;
					quals.replaceObjectAtIndex(new EOKeyValueQualifier("course", 
							EOQualifier.QualifierOperatorEqual,course),0);
			    	String listName = settings.forCourse(course).textValue();
			    	itogs = itogByListName.objectForKey(listName);
			    	if(itogs != null)
			    		continue;
					if(getDefault) {
				    	EOQualifier[] qs = new EOQualifier[3];
				    	qs[0] = new EOKeyValueQualifier(AutoItog.LIST_NAME_KEY,
				    			EOQualifier.QualifierOperatorEqual,listName);
				    	qs[1] = new EOKeyValueQualifier(AutoItog.FIRE_DATE_KEY,
				    			EOQualifier.QualifierOperatorGreaterThanOrEqualTo, today);
				    	qs[2] = new EOKeyValueQualifier(AutoItog.FLAGS_KEY,
				    			EOQualifier.QualifierOperatorLessThan, new Integer(32));
				    	qs[0] = new EOAndQualifier(new NSArray(qs));
				    	EOFetchSpecification fs = new EOFetchSpecification(AutoItog.ENTITY_NAME,
				    			qs[0],null);
				    	NSArray found = ec.objectsWithFetchSpecification(fs);
				    	itogs = (NSMutableArray)found.valueForKey(AutoItog.ITOG_CONTAINER_KEY);
					} else {
						NSArray types = ItogType.typesForList(listName, eduYear, ec);
						itogs = ItogType.itogsForTypeList(types,eduYear);
					}
					if(itogs == null || itogs.count() == 0) {
						itogs = NSArray.EmptyArray;
					} else {
						if(!(itogs instanceof NSMutableArray))
							itogs = itogs.mutableClone();
						EOQualifier.filterArrayWithQualifier((NSMutableArray)itogs, itogQual);
						EOSortOrdering.sortArrayUsingKeyOrderArray(
								(NSMutableArray)itogs, itogSorter);
			    	}
					itogByListName.setObjectForKey(itogs, listName);
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
