//  AutoItog.java

/*
 * Copyright (c) 2008, Gennady & Michael Kushnir
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 * 
 * 	o	Redistributions of source code must retain the above copyright notice, this
 * 		list of conditions and the following disclaimer.
 * 	o	Redistributions in binary form must reproduce the above copyright notice,
 * 		this list of conditions and the following disclaimer in the documentation
 * 		and/or other materials provided with the distribution.
 * 	o	Neither the name of the RUJEL nor the names of its contributors may be used
 * 		to endorse or promote products derived from this software without specific
 * 		prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT
 * SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
 * TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
 * BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY
 * WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package net.rujel.autoitog;

import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.logging.Logger;

import net.rujel.base.MyUtility;
import net.rujel.base.SettingsBase;
import net.rujel.eduresults.ItogContainer;
import net.rujel.eduresults.ItogMark;
import net.rujel.eduresults.ItogType;
import net.rujel.interfaces.EOInitialiser;
import net.rujel.interfaces.EOPeriod;
import net.rujel.interfaces.EduCourse;
import net.rujel.reusables.NamedFlags;
import net.rujel.reusables.WOLogLevel;

import com.webobjects.foundation.*;
import com.webobjects.eoaccess.EOObjectNotAvailableException;
import com.webobjects.eoaccess.EOUtilities;
import com.webobjects.eocontrol.*;

public class AutoItog extends _AutoItog {
	public static final NSArray flagNames = new NSArray(new String[]
	               {"noTimeouts","manual","runningTotal","-8-","hideInReport","inactive"});
	
	protected static final NSArray aiSorter = new NSArray( new EOSortOrdering[] {
		EOSortOrdering.sortOrderingWithKey(FIRE_DATE_KEY,EOSortOrdering.CompareAscending),
		EOSortOrdering.sortOrderingWithKey(FIRE_TIME_KEY,EOSortOrdering.CompareAscending),
		EOSortOrdering.sortOrderingWithKey("itogContainer.itogType.sort",EOSortOrdering.CompareAscending)
		});
	public static final NSArray typeSorter = new NSArray(
			new EOSortOrdering(ITOG_CONTAINER_KEY,EOSortOrdering.CompareAscending));

	public static void init() {
		EOInitialiser.initialiseRelationship("ItogRelated","course",false,
				"courseID","EduCourse");
		ComparisonSupport.setSupportForClass(new ComparisonSupport(), AutoItog.class);
	}

	public static NSTimestamp defaultFireDateTime() {
		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.HOUR_OF_DAY, 4);
		cal.set(Calendar.MINUTE, 30);
		cal.add(Calendar.MONTH, 2);
		cal.set(Calendar.DAY_OF_MONTH, 1);
		return new NSTimestamp(cal.getTimeInMillis());
	}
	
	public void awakeFromInsertion(EOEditingContext ec) {
		super.awakeFromInsertion(ec);
		setFlags(new Integer(2));
		setFireDateTime(defaultFireDateTime());
	}
	
	public static NSTimestamp combineDateAndTime(Date date, Date time) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		int year = cal.get(Calendar.YEAR);
		int day = cal.get(Calendar.DAY_OF_YEAR);
		cal.setTime(time);
		cal.set(Calendar.YEAR, year);
		cal.set(Calendar.DAY_OF_YEAR, day);
		return new NSTimestamp(cal.getTimeInMillis());
	}
	
	public void setFireDateTime(NSTimestamp dateTime) {
		setFireDate(dateTime);
		setFireTime(dateTime);
	}
	
	public NSTimestamp fireDateTime() {
		NSTimestamp date = combineDateAndTime(fireDate(), fireTime());
		return date;
	}
	
    public boolean noCalculator() {
    	String calcName = calculatorName();
    	if(calcName == null || calcName.length() == 0 || calcName.equalsIgnoreCase("none"))
    		return true;
    	return false;
    }

    public static AutoItog forListName(String listName, ItogContainer container) {
    	EOEditingContext ec = container.editingContext();
    	NSDictionary values = new NSDictionary(new Object[] {listName,container},
    			new String[] {LIST_NAME_KEY,ITOG_CONTAINER_KEY});
    	try {
    		return (AutoItog)EOUtilities.objectMatchingValues(ec, ENTITY_NAME, values);
    	} catch (EOObjectNotAvailableException e) {
    		return null;
    	}
    }
    
    public static NSArray currentAutoItogsForCourse(EduCourse course,NSTimestamp date) {
    	return currentAutoItogsForCourse(course, date, date);
    }
        public static NSArray currentAutoItogsForCourse(EduCourse course,
        		NSTimestamp minDate, NSTimestamp maxDate) {
    	EOEditingContext ec = course.editingContext();
    	String listName = SettingsBase.stringSettingForCourse(ItogMark.ENTITY_NAME, course, ec);
    	EOQualifier[] quals = new EOQualifier[3];
    	quals[0] = new EOKeyValueQualifier(LIST_NAME_KEY,
    			EOQualifier.QualifierOperatorEqual,listName);
    	quals[1] = new EOKeyValueQualifier(FIRE_DATE_KEY,
    			EOQualifier.QualifierOperatorGreaterThanOrEqualTo, minDate);
    	quals[2] = new EOKeyValueQualifier(FLAGS_KEY,
    			EOQualifier.QualifierOperatorLessThan, new Integer(32));
    	quals[0] = new EOAndQualifier(new NSArray(quals));
    	EOFetchSpecification fs = new EOFetchSpecification(ENTITY_NAME,quals[0],null);
    	NSArray found = ec.objectsWithFetchSpecification(fs);
    	NSMutableArray result = (found==null)?new NSMutableArray():found.mutableClone();    	
    	quals[0] = CourseTimeout.qualifierForCourseAndPeriod(course, null);
    	quals[2] = null; 
    	quals[0] = new EOAndQualifier(new NSArray(quals));
    	fs.setEntityName(CourseTimeout.ENTITY_NAME);
    	fs.setQualifier(quals[0]);
    	fs.setSortOrderings(null);
    	found = ec.objectsWithFetchSpecification(fs);
    	NSMutableSet types = new NSMutableSet();
    	if(found != null && found.count() > 0) { // CourseTimeouts
    		Enumeration enu = found.objectEnumerator();
    		while (enu.hasMoreElements()) {
				CourseTimeout cto = (CourseTimeout) enu.nextElement();
				ItogContainer itog = cto.itogContainer();
				if(types.containsObject(itog))
					continue;
				EOQualifier.filterArrayWithQualifier(result, new EOKeyValueQualifier(
						ITOG_CONTAINER_KEY, EOQualifier.QualifierOperatorNotEqual,itog));
				result.addObject(CourseTimeout.getTimeoutForCourseAndPeriod(course, itog));
				types.addObject(itog);
			}
    		types.removeAllObjects();
     	}
    	if (result.count() > 1) {
     		EOSortOrdering.sortArrayUsingKeyOrderArray(result, aiSorter);
		}
    	NSArray allowedTypes = ItogType.typesForList(listName, course.eduYear(), ec);
    	Enumeration enu = result.objectEnumerator();
    	result = new NSMutableArray();
    	while (enu.hasMoreElements()) {
    		EOEnterpriseObject obj = (EOEnterpriseObject) enu.nextElement();
			Object type = obj.valueForKeyPath("itogContainer.itogType");
			if(type == null || types.containsObject(type) || !allowedTypes.contains(type))
				continue;
			AutoItog ai = null;
			if(obj instanceof AutoItog) {
				ai = (AutoItog)obj;
			} else {
				ItogContainer itog = (ItogContainer)obj.valueForKey(ITOG_CONTAINER_KEY);
				ai = forListName(listName, itog);
			}
			if(ai.namedFlags().flagForKey("inactive"))
				continue;
			NSTimestamp fire = (NSTimestamp)obj.valueForKey(FIRE_DATE_KEY);
			fire = combineDateAndTime(fire, ai.fireTime());
			int compare = EOPeriod.Utility.compareDates(fire, minDate);
			if(compare <= 0)
				continue;
			result.addObject(ai);
			compare = EOPeriod.Utility.compareDates(fire, maxDate);
			if(compare > 0 || (compare == 0 && MyUtility.isEvening(fire)))
				types.addObject(type);
		}
    	return result;
    }
    
    private NamedFlags _flags;
    public NamedFlags namedFlags() {
    	if(_flags==null) {
    		_flags = new NamedFlags(flags().intValue(),flagNames);
    		try{
    			_flags.setSyncParams(this, getClass().getMethod(
    					"setNamedFlags", NamedFlags.class));
    		} catch (Exception e) {
    			Logger.getLogger("rujel.autoitog").log(WOLogLevel.WARNING,
						"Could not get syncMethod for AutoItog flags",e);
			}
    	}
    	return _flags;
    }

    public void setNamedFlags(NamedFlags flags) {
    	_flags = flags;
    	setFlags(flags.toInteger());
    }

    protected Calculator _calculator;
    public Calculator calculator() {
    	if(_calculator == null) {
    		_calculator = Calculator.calculatorForName(calculatorName());
    	}
    	return _calculator;
    }

    public NSArray relKeysForCourse(EduCourse course) {
    	return relKeysForCourse(course, itogContainer());
    }
    
    public static NSArray relKeysForCourse(EduCourse course,ItogContainer itogContainer) {
    	NSDictionary values = new NSDictionary(new Object[] {itogContainer,course},
    			new String[] {"itogContainer","course"});
    	NSArray found = EOUtilities.objectsMatchingValues(course.editingContext(), 
    			"ItogRelated", values);
    	if(found == null)
    		found = NSArray.EmptyArray;
    	return found;
    }
    
    public NSArray relatedForCourse(EduCourse course) {
    	if(noCalculator())
    		return null;
    	boolean cache = editingContext().hasChanges();
    	EOEditingContext tmpEc = (cache)? new EOEditingContext(
    			editingContext().parentObjectStore()) : editingContext();
    	if(cache) {
    		tmpEc.lock();
    		course = (EduCourse)EOUtilities.localInstanceOfObject(tmpEc, course);
    	}
    	AutoItog ai = (cache)?(AutoItog)EOUtilities.localInstanceOfObject(tmpEc, this):this;
       	NSArray found = relKeysForCourse(course,ai.itogContainer());
       	try {
    		if(found != null && found.count() > 0) {
    			NSMutableArray related = new NSMutableArray();
    			String entName = calculator().reliesOnEntity();
    			Enumeration enu = found.objectEnumerator();
    			while (enu.hasMoreElements()) {
    				EOEnterpriseObject ir = (EOEnterpriseObject) enu.nextElement();
    				Integer relKey = (Integer)ir.valueForKey("relKey");
    				try {
    					EOEnterpriseObject object = EOUtilities.objectWithPrimaryKeyValue(
    							tmpEc, entName, relKey);
    					EduCourse crs = calculator().courseForObject(object);
    					if(crs == null) {
    						tmpEc.deleteObject(ir);
    						AutoItogModule.logger.log(WOLogLevel.INFO,
    								"Related object with null course found. deleting:" +
    								relKey, new Object[] {this,course});
    						continue;
    					} else if(course != crs) {
    						AutoItogModule.logger.log(WOLogLevel.WARNING,
    								"Related object with wrong course found:" + relKey,
    								new Object[] {this,course,crs});
    						related = null;
    						Enumeration denu = found.objectEnumerator();
    						while (denu.hasMoreElements()) {
    							ir = (EOEnterpriseObject) denu.nextElement();
    							tmpEc.deleteObject(ir);
    						}
    						break;
    					}
    					related.addObject(object);
    				} catch (RuntimeException e) {
    					AutoItogModule.logger.log(WOLogLevel.WARNING,
    							"Could not get related object: " + entName + ':' + relKey
    							,new Object[] {this,e});
    					tmpEc.deleteObject(ir);
    				}
    			}
    			found = (related == null)? null : related.immutableClone();
    		} else {
    			found = null;
    		}
    		if(found == null) {
    			found = calculator().collectRelated(course, ai, 
    					!namedFlags().flagForKey("runningTotal"),true);
    		}
    		if(tmpEc.hasChanges())
    			tmpEc.saveChanges();
			if(cache && found != null)
				found = EOUtilities.localInstancesOfObjects(editingContext(), found);
    	} catch (RuntimeException e) {
    		AutoItogModule.logger.log(WOLogLevel.WARNING,"Error collecting related",
    				new Object[] {this,e});
    	} finally {
    		if(cache)
    			tmpEc.unlock();
    	}
    	return found;
    }
    
    public boolean evening() {
    	return MyUtility.isEvening(fireTime());
    }
    
    public NSTimestamp fireDateForCourse(EduCourse course) {
    	CourseTimeout cto = CourseTimeout.getTimeoutForCourseAndPeriod(
    			course, itogContainer());
    	if(cto == null)
    		return fireDate();
    	else
    		return cto.fireDate();
    }

    public static class ComparisonSupport extends EOSortOrdering.ComparisonSupport {

		public int compareAscending(Object left, Object right)  {
			if(!(left instanceof AutoItog))
				return NSComparator.OrderedAscending;
			AutoItog l = (AutoItog)left;
			if(!(right instanceof AutoItog))
				return NSComparator.OrderedDescending;
			AutoItog r = (AutoItog)right;
			int result = compareValues(l.fireDateTime(), r.fireDateTime(),
					EOSortOrdering.CompareAscending);
			if(result == NSComparator.OrderedSame)
			result = compareValues(l.itogContainer(),r.itogContainer(),
					EOSortOrdering.CompareAscending);
			return result;
		}

		public int compareCaseInsensitiveAscending(Object left, Object right)  {
			return compareAscending(left, right) ;
		}
		public int compareDescending(Object left, Object right)  {
			return compareAscending(right, left) ;
		}
		public int compareCaseInsensitiveDescending(Object left, Object right)  {
			return compareAscending(right, left) ;
		}
	}
    
    public static NSArray relatedToObject(Object object, EduCourse course) {
    	EOEditingContext ec = course.editingContext();
    	String listName = SettingsBase.stringSettingForCourse(
    			ItogMark.ENTITY_NAME, course, ec);
    	EOQualifier[] quals = new EOQualifier[2];
    	quals[0] = new EOKeyValueQualifier(LIST_NAME_KEY,
    			EOQualifier.QualifierOperatorEqual,listName);
    	quals[1] = new EOKeyValueQualifier(FLAGS_KEY,
    			EOQualifier.QualifierOperatorLessThan, new Integer(32));
    	quals[0] = new EOAndQualifier(new NSArray(quals));
    	EOFetchSpecification fs = new EOFetchSpecification(ENTITY_NAME,quals[0],null);
    	NSArray found = ec.objectsWithFetchSpecification(fs);
//    	NSArray all = EOUtilities.objectsMatchingKeyAndValue(ec,
//    			ENTITY_NAME, listName, LIST_NAME_KEY);
    	if(found == null || found.count() == 0)
    		return null;
    	Enumeration enu = found.objectEnumerator();
    	Integer eduYear = course.eduYear();
    	NSMutableDictionary forCalc = new NSMutableDictionary();
    	NSMutableArray result = new NSMutableArray();
    	while (enu.hasMoreElements()) {
			AutoItog ai = (AutoItog) enu.nextElement();
			if(ai.noCalculator() || !eduYear.equals(ai.itogContainer().eduYear()))
				continue;
			String calc = ai.calculatorName();
			Integer relKey = (Integer)forCalc.objectForKey(calc);
			if(relKey == null) {
				relKey = ai.calculator().relKeyForObject(object);
				if(relKey == null)
					relKey = new Integer(0);
				forCalc.setObjectForKey(relKey, calc);
			}
			if(relKey.intValue() == 0)
				continue;
			NSDictionary values = new NSDictionary(
					new Object[] {course,ai.itogContainer(),relKey},
					new String[] {"course","itogContainer","relKey"});
			found = EOUtilities.objectsMatchingValues(ec, "ItogRelated", values);
			if(found != null && found.count() > 0)
				result.addObject(ai);
		} // all AIs
    	if(result.count() > 1) {
    		EOSortOrdering.sortArrayUsingKeyOrderArray(result, typeSorter);
    	}
    	return result;
    }
    
    public boolean addRelatedObject(Object object, EduCourse course) {
    	Integer relKey = (object instanceof Integer)?(Integer)object:
    			calculator().relKeyForObject(object);
    	if(relKey == null)
    		throw new IllegalArgumentException(
    				"Provided object is not supported by defined calculator");
		NSDictionary values = new NSDictionary(new Object[] {course,itogContainer(),relKey},
				new String[] {"course","itogContainer","relKey"});
		EOEditingContext ec = course.editingContext();
		NSArray found = EOUtilities.objectsMatchingValues(ec, "ItogRelated", values);
		boolean result = (found == null || found.count() == 0);
		if(result) {
			EOEnterpriseObject rel = EOUtilities.createAndInsertInstance(ec, "ItogRelated");
			rel.addObjectToBothSidesOfRelationshipWithKey(itogContainer(), "itogContainer");
			rel.addObjectToBothSidesOfRelationshipWithKey(course, "course");
			rel.takeValueForKey(relKey, "relKey");
		}
		return result;
    }
    
    public boolean removeRelatedObject(Object object, EduCourse course) {
    	Integer relKey = (object instanceof Integer)?(Integer)object:
    			calculator().relKeyForObject(object);
    	if(relKey == null)
    		throw new IllegalArgumentException(
    				"Provided object is not supported by defined calculator");
		NSDictionary values = new NSDictionary(new Object[] {course,itogContainer(),relKey},
				new String[] {"course","itogContainer","relKey"});
		EOEditingContext ec = course.editingContext();
		NSArray found = EOUtilities.objectsMatchingValues(ec, "ItogRelated", values);
		boolean result = (found != null && found.count() > 0);
		if(result) {
			for (int i = 0; i < found.count(); i++) {
				EOEnterpriseObject rel = (EOEnterpriseObject)found.objectAtIndex(i);
				ec.deleteObject(rel);
			}
		}
		return result;
    }
    
    public boolean inactive() {
    	if(flags().intValue() >= 32)
    		return true;
    	Object type = itogContainer().itogType();
    	Integer eduYear = itogContainer().eduYear();
    	if(type == null || eduYear == null)
    		return true;
    	NSArray list = new NSArray(new EOQualifier[] {
        	new EOKeyValueQualifier("eduYear", EOQualifier.QualifierOperatorEqual, new Integer(0)),
        	new EOKeyValueQualifier("eduYear", EOQualifier.QualifierOperatorEqual, eduYear)});
    	list = new NSArray(new EOQualifier[] {
    		new EOKeyValueQualifier("itogType", EOQualifier.QualifierOperatorEqual, type),
    		new EOKeyValueQualifier("listName", EOQualifier.QualifierOperatorEqual, listName()),
    		new EOOrQualifier(list)});
    	EOFetchSpecification fs = new EOFetchSpecification("ItogTypeList",
    			new EOAndQualifier(list),null);
    	list = editingContext().objectsWithFetchSpecification(fs);
    	return (list == null || list.count() == 0);
    }
}
