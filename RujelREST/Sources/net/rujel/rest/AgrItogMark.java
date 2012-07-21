package net.rujel.rest;

import java.util.Enumeration;

import net.rujel.base.MyUtility;
import net.rujel.eduresults.ItogContainer;
import net.rujel.eduresults.ItogMark;
import net.rujel.reusables.SettingsReader;

import com.webobjects.appserver.WOApplication;
import com.webobjects.appserver.WORequest;
import com.webobjects.eocontrol.EOAndQualifier;
import com.webobjects.eocontrol.EOEnterpriseObject;
import com.webobjects.eocontrol.EOFetchSpecification;
import com.webobjects.eocontrol.EOKeyValueQualifier;
import com.webobjects.eocontrol.EOQualifier;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSMutableArray;
import com.webobjects.foundation.NSMutableDictionary;

public class AgrItogMark extends AgrEntity {
	
	private static final String[] attributes = new String[] {
		"student","grade"};
	
	public String entityName() {
		return ItogMark.ENTITY_NAME;
	}
	
	public NSArray attributes() {
		return new NSArray(attributes);
	}

	public Enumeration rawRowsEnumeration(WORequest req) {
		NSMutableDictionary params = new NSMutableDictionary();
		NSMutableArray quals = new NSMutableArray();
		String txt = req.stringFormValueForKey("eduYear");
		if(txt == null) {
			Integer year = (Integer)WOApplication.application().valueForKey("year");
			if(year == null)
				year = MyUtility.eduYearForDate(null);
			quals.addObject(new EOKeyValueQualifier(ItogContainer.EDU_YEAR_KEY, 
					EOQualifier.QualifierOperatorEqual, year));
			params.takeValueForKey(year, "eduYear");
		} else {
			addIntToQuals(quals, ItogContainer.EDU_YEAR_KEY, txt);
			params.takeValueForKey(txt, "eduYear");
		}
		txt = req.stringFormValueForKey("perCount");
		params.takeValueForKey(txt, "perCount");
		addIntToQuals(quals, "itogType.inYearCount", txt);
		txt = req.stringFormValueForKey("perNum");
		params.takeValueForKey(txt, "perNum");
		addIntToQuals(quals, ItogContainer.NUM_KEY, txt);
		EOQualifier qual = new EOAndQualifier(quals);
		quals.removeAllObjects();
		EOFetchSpecification fs = new EOFetchSpecification(ItogContainer.ENTITY_NAME,qual,null);
		NSArray found = ec.objectsWithFetchSpecification(fs);
		if(found == null || found.count() == 0)
			return null;
		NSMutableDictionary iterate = new NSMutableDictionary();
		if(found.count() > 1) {
			iterate.takeValueForKey(found, ItogMark.CONTAINER_KEY);
		} else {
			quals.addObject(new EOKeyValueQualifier(ItogMark.CONTAINER_KEY, 
					EOQualifier.QualifierOperatorEqual, found.objectAtIndex(0)));
		}
		txt = req.stringFormValueForKey("grade");
		if(txt == null) {
			int minGrade = SettingsReader.intForKeyPath("edu.minGrade", 1);
			int maxGrade = SettingsReader.intForKeyPath("edu.maxGrade", 11);
			Integer[] list = new Integer[maxGrade - minGrade +1];
			for (int i = 0; i < list.length; i++) {
				list[i] = new Integer(minGrade + i);
			}
			iterate.takeValueForKey(new NSArray(list), "cycle.grade");
		} else {
			params.takeValueForKey(txt, "grade");
			Object[] snv = selectorAndValue(txt);
			if(snv[0] == EOQualifier.QualifierOperatorEqual) {
				if(snv.length == 2) {
					quals.addObject(new EOKeyValueQualifier("cycle.grade", 
							EOQualifier.QualifierOperatorEqual, new Integer(txt)));
				} else {
					Integer[] list = new Integer[snv.length -1];
					for (int i = 0; i < list.length; i++) {
						list[i] = new Integer((String)snv[i +1]);
					}
					iterate.takeValueForKey(new NSArray(list), "cycle.grade");
				}
			} else {
				int minGrade = SettingsReader.intForKeyPath("edu.minGrade", 1);
				int maxGrade = SettingsReader.intForKeyPath("edu.maxGrade", 11);
				if(snv[0] == EOQualifier.QualifierOperatorLessThan)
					maxGrade = Integer.parseInt((String)snv[1]) -1;
				else if(snv[0] == EOQualifier.QualifierOperatorLessThanOrEqualTo)
					maxGrade = Integer.parseInt((String)snv[1]);
				else if(snv[0] == EOQualifier.QualifierOperatorGreaterThan)
					minGrade = Integer.parseInt((String)snv[1]) +1;
				else if(snv[0] == EOQualifier.QualifierOperatorGreaterThanOrEqualTo)
					minGrade = Integer.parseInt((String)snv[1]);
 				if(snv.length >= 4) {
					if(snv[2] == EOQualifier.QualifierOperatorLessThan)
						maxGrade = Integer.parseInt((String)snv[3]) -1;
					else if(snv[2] == EOQualifier.QualifierOperatorLessThanOrEqualTo)
						maxGrade = Integer.parseInt((String)snv[3]);
				}
				Integer[] list = new Integer[maxGrade - minGrade +1];
				for (int i = 0; i < list.length; i++) {
					list[i] = new Integer(minGrade + i);
				}
				iterate.takeValueForKey(new NSArray(list), "cycle.grade");
			}
		} // iterator for grade
		txt = req.stringFormValueForKey("mark");
		if(txt != null) {
			params.takeValueForKey(txt, "mark");
			quals.addObject(new EOKeyValueQualifier(ItogMark.MARK_KEY, 
					EOQualifier.QualifierOperatorEqual, txt));
		}
		txt = req.stringFormValueForKey("value");
		params.takeValueForKey(txt, "value");
		addDecToQuals(quals, ItogMark.VALUE_KEY, txt);
		
		req.context().setUserInfoForKey(params, "params");
		
		//TODO: qualifiers for subject, student and course
		
		return new RowsEnum(this, quals, iterate);
	}
	
	public Object getValue(EOEnterpriseObject obj, String attribute) {
		ItogMark mark = (ItogMark)obj;
		if(attribute.equals("subject"))
			return mark.cycle().subject();
		if(attribute.equals("grade"))
			return mark.cycle().grade();
		if(attribute.equals("eduYear"))
			return mark.container().eduYear();
		if(attribute.equals("course")) //wrap
			return mark.assumeCourse();
		if(attribute.equals("student")) //wrap
			return mark.student();
		if(attribute.equals("perNum"))
			return mark.container().num();
		if(attribute.equals("perCount"))
			return mark.container().itogType().inYearCount();
		if(attribute.equals("mark"))
			return mark.mark();
		if(attribute.equals("value"))
			return mark.value();
		throw new IllegalArgumentException("Unknown attribute '" + attribute + '\'');
	}
}
