package net.rujel.rest;

import java.math.BigDecimal;
import java.util.Enumeration;
import java.util.TreeSet;

import net.rujel.base.MyUtility;
import net.rujel.eduresults.ItogContainer;
import net.rujel.eduresults.ItogMark;
import net.rujel.rest.Agregator.ParseError;

import com.webobjects.appserver.WOApplication;
import com.webobjects.eocontrol.EOAndQualifier;
import com.webobjects.eocontrol.EOEnterpriseObject;
import com.webobjects.eocontrol.EOFetchSpecification;
import com.webobjects.eocontrol.EOKeyValueQualifier;
import com.webobjects.eocontrol.EOQualifier;
import com.webobjects.eocontrol.EOSortOrdering;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSDictionary;
import com.webobjects.foundation.NSMutableArray;

public class AgrItogMark extends AgrEntity {
	
	private static final String[] attributes = new String[] {
		"eduYear","perCount","perNum","grade","subject","course","student","mark","value"};
	
	public String entityName() {
		return ItogMark.ENTITY_NAME;
	}
	
	public NSArray attributes() {
		return new NSArray(attributes);
	}

	public Enumeration getObjectsEnumeration(NSDictionary params) throws ParseError {
		NSMutableArray quals = new NSMutableArray();
		NSArray found = AgrEduCourse.cyclesForParams(params,ec);
		if(found == null || found.count() == 0)
			return null;
//		NSMutableDictionary iterate = new NSMutableDictionary(found,"cycle");
		NSArray[] itrValues = new NSArray[2];
		itrValues[1] = found;
		
		String txt = (String)params.valueForKey("eduYear");
		if(txt == null) {
			Integer year = (Integer)WOApplication.application().valueForKey("year");
			if(year == null)
				year = MyUtility.eduYearForDate(null);
			quals.addObject(new EOKeyValueQualifier(ItogContainer.EDU_YEAR_KEY, 
					EOQualifier.QualifierOperatorEqual, year));
			params.takeValueForKey(year.toString(), "eduYear");
		} else {
			addIntToQuals(quals, ItogContainer.EDU_YEAR_KEY, txt);
		}
		txt = (String)params.valueForKey("perCount");
		if(txt != null) {
			if(txt.equals("0")) {
				quals.addObject(new EOKeyValueQualifier("itogType.inYearCount", 
					EOQualifier.QualifierOperatorEqual, new Integer(1)));
			} else if(txt.equals("1")) {
				quals.addObject(new EOKeyValueQualifier("itogType.inYearCount", 
						EOQualifier.QualifierOperatorEqual, new Integer(1)));
				quals.addObject(new EOKeyValueQualifier("itogType.title", 
						EOQualifier.QualifierOperatorCaseInsensitiveLike, "*год*"));				
			} else {
				addIntToQuals(quals, "itogType.inYearCount", txt);
			}
		}		
		txt = (String)params.valueForKey("perNum");
		addIntToQuals(quals, ItogContainer.NUM_KEY, txt);
		EOQualifier qual = new EOAndQualifier(quals);
		quals.removeAllObjects();
		NSArray sorter = new NSArray(new EOSortOrdering[] {
				new EOSortOrdering(ItogContainer.EDU_YEAR_KEY,EOSortOrdering.CompareAscending),
				new EOSortOrdering("itogType.inYearCount",EOSortOrdering.CompareDescending),
				new EOSortOrdering(ItogContainer.NUM_KEY,EOSortOrdering.CompareAscending),
				new EOSortOrdering("itogType.sort",EOSortOrdering.CompareDescending)
		});
		EOFetchSpecification fs = new EOFetchSpecification(ItogContainer.ENTITY_NAME,qual,sorter);
		found = ec.objectsWithFetchSpecification(fs);
		if(found == null || found.count() == 0)
			return null;
		itrValues[0] = found;
/*		NSMutableDictionary iterate = new NSMutableDictionary();
		if(found.count() > 1) {
			iterate.takeValueForKey(found, ItogMark.CONTAINER_KEY);
		} else {
			quals.addObject(new EOKeyValueQualifier(ItogMark.CONTAINER_KEY, 
					EOQualifier.QualifierOperatorEqual, found.objectAtIndex(0)));
		}
//
  		txt = (String)params.valueForKey("grade");
		if(txt == null) {
			int minGrade = SettingsReader.intForKeyPath("edu.minGrade", 1);
			int maxGrade = SettingsReader.intForKeyPath("edu.maxGrade", 11);
			Integer[] list = new Integer[maxGrade - minGrade +1];
			for (int i = 0; i < list.length; i++) {
				list[i] = new Integer(minGrade + i);
			}
			iterate.takeValueForKey(new NSArray(list), "cycle.grade");
		} else {
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
		*/
		txt = (String)params.valueForKey("mark");
		if(txt != null) {
			quals.addObject(new EOKeyValueQualifier(ItogMark.MARK_KEY, 
					EOQualifier.QualifierOperatorEqual, txt));
		}
		txt = (String)params.valueForKey("value");
		addDecToQuals(quals, ItogMark.VALUE_KEY, txt);
				
		//TODO: qualifiers for student and course
		
		String[] itrAttr = new String[] {ItogMark.CONTAINER_KEY,ItogMark.CYCLE_KEY};
		return new RowsEnum(this, quals,itrAttr,itrValues) {
			
			private Integer curCycle;
			private Integer curResultCount;
			private Integer curResultNumber;
			private TreeSet<Integer> mentionedStudents = new TreeSet();
			
			protected boolean qualifies(Wrapper obj) {
				Integer val = (Integer)obj.row.valueForKey("eduCycleID");
				if(curCycle == null || !curCycle.equals(val)) {
					curCycle = val;
					mentionedStudents.clear();
				}
				ItogContainer cur = (ItogContainer)iterDict.valueForKey(ItogMark.CONTAINER_KEY);
				val = cur.itogType().inYearCount();
				if(curResultCount == null || !curResultCount.equals(val)) {
					curResultCount = val;
					mentionedStudents.clear();
				}
				val = cur.num();
				if(curResultNumber == null || !curResultNumber.equals(val)) {
					curResultNumber = val;
					mentionedStudents.clear();
				}
				val = (Integer)obj.row.valueForKey("studentID");
				return mentionedStudents.add(val);
			}
		};
	}
	
	public Object getValue(EOEnterpriseObject obj, String attribute) {
		if(!(obj instanceof ItogMark)) {
			return "???";
		}
		ItogMark mark = (ItogMark)obj;
		if(attribute.equals("subject"))
			return mark.cycle().subject();
		if(attribute.equals("grade"))
			return mark.cycle().grade();
		if(attribute.equals("eduYear"))
			return mark.container().eduYear();
		if(attribute.equals("course")) //wrap
			return new AgrEduCourse.Wrapper(mark.assumeCourse());
		if(attribute.equals("student")) //wrap
//			return mark.student();
//			return WOLogFormatter.formatEO(mark.student());
			return new Wrapper(mark.student());
		if(attribute.equals("perNum"))
			return mark.container().num();
		if(attribute.equals("perCount")) {
			Integer inYear = mark.container().itogType().inYearCount();
			if(inYear.intValue() == 1) {
				if(!mark.container().itogType().title().toLowerCase().contains("год"))
					return new Integer(0);
			}
			return inYear;
		}
		if(attribute.equals("mark"))
			return mark.mark();
		if(attribute.equals("value")) {
			BigDecimal val = mark.value();
			if(val == null || mark.readFlags().flagForKey("forced")) {
				String m = mark.mark();
				if(m == null)
					return new Integer(0);
				if(m.equals("5"))
					return new Integer(1);
				if(m.equals("4"))
					return new BigDecimal("0.8");
				if(m.equals("3"))
					return new BigDecimal("0.6");
				if(m.equals("2"))
					return new BigDecimal("0.4");
				if(m.startsWith("осв"))
					return null;
				return new Integer(0);
			}
			if(val.compareTo(BigDecimal.ONE) == 0)
				return new Integer(1);
			if(val.compareTo(BigDecimal.ZERO) == 0)
				return new Integer(0);
			return val;
		}
		throw new IllegalArgumentException("Unknown attribute '" + attribute + '\'');
	}
}
