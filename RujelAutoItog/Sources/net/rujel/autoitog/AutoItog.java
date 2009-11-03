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

import net.rujel.base.SettingsBase;
import net.rujel.eduresults.ItogContainer;
import net.rujel.eduresults.ItogMark;
import net.rujel.interfaces.EOInitialiser;
import net.rujel.interfaces.EduCourse;
import net.rujel.reusables.NamedFlags;
import net.rujel.reusables.SettingsReader;
import net.rujel.reusables.WOLogLevel;

import com.webobjects.foundation.*;
import com.webobjects.eoaccess.EOObjectNotAvailableException;
import com.webobjects.eoaccess.EOUtilities;
import com.webobjects.eocontrol.*;

public class AutoItog extends _AutoItog {
	public static final NSArray flagNames = new NSArray(new String[]
	               {"noTimeouts","manual","runningTotal","-8-","hideInReport","inactive"});
	
	public static final NSArray dateTimeSorter = new NSArray( new EOSortOrdering[] {
		EOSortOrdering.sortOrderingWithKey(FIRE_DATE_KEY,EOSortOrdering.CompareAscending),
		EOSortOrdering.sortOrderingWithKey(FIRE_TIME_KEY,EOSortOrdering.CompareAscending)
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
    	EOEditingContext ec = course.editingContext();
    	String listName = SettingsBase.stringSettingForCourse(ItogMark.ENTITY_NAME, course, ec);
    	EOQualifier[] quals = new EOQualifier[3];
    	quals[0] = new EOKeyValueQualifier(LIST_NAME_KEY,
    			EOQualifier.QualifierOperatorEqual,listName);
    	quals[1] = new EOKeyValueQualifier(FIRE_DATE_KEY,
    			EOQualifier.QualifierOperatorGreaterThanOrEqualTo, date);
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
    	if(found != null && found.count() > 0) {
    		Enumeration enu = found.objectEnumerator();
    		while (enu.hasMoreElements()) {
				CourseTimeout cto = (CourseTimeout) enu.nextElement();
				ItogContainer itog = cto.itogContainer();
				if(types.containsObject(itog))
					continue;
				EOQualifier.filterArrayWithQualifier(result, new EOKeyValueQualifier(
						ITOG_CONTAINER_KEY, EOQualifier.QualifierOperatorNotEqual,itog));
				result.addObject(CourseTimeout.getTimeoutForCourseAndPeriod(course, itog));
			}
    		types.removeAllObjects();
     	}
    	if (result.count() > 1) {
     		EOSortOrdering.sortArrayUsingKeyOrderArray(result, typeSorter);
		}
    	Enumeration enu = result.objectEnumerator();
    	result.removeAllObjects();// = new NSMutableArray();
    	while (enu.hasMoreElements()) {
    		EOEnterpriseObject obj = (EOEnterpriseObject) enu.nextElement();
			Object type = obj.valueForKeyPath("itogContainer.itogType");
			if(types.containsObject(type))
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
			if(fire.compare(date) < 0)
				continue;
			types.addObject(type);
			result.addObject(ai);
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
    	NSDictionary values = new NSDictionary(new Object[] {this,course},
    			new String[] {"autoItog","course"});
    	NSArray found = EOUtilities.objectsMatchingValues(editingContext(), 
    			"ItogRelated", values);
    	if(found == null)
    		found = NSArray.EmptyArray;
    	return found;
    }
    public NSArray relatedForCourse(EduCourse course) {
    	NSArray found = relKeysForCourse(course);
    	if(found == null || found.count() == 0) {
    		NSArray result = null;
    		EOEditingContext ec = editingContext();
    		boolean cache = ec.hasChanges();
    		try {
    			if(cache) {
					ec = new EOEditingContext();
	    			ec.lock();
					AutoItog ai = (AutoItog)EOUtilities.localInstanceOfObject(ec, this);
					course = (EduCourse)EOUtilities.localInstanceOfObject(ec, course);
//					NSDictionary snapshot = snapshot();
//					ai.updateFromSnapshot(snapshot);
					result = calculator().collectRelated(course, ai, 
							!namedFlags().flagForKey("runningTotal"),true);
				} else {
	    			ec.lock();
					result = calculator().collectRelated(course, this, 
							!namedFlags().flagForKey("runningTotal"),true);
				}
				ec.saveChanges();
				if(cache)
					result = EOUtilities.localInstancesOfObjects(editingContext(), result);
			} catch (RuntimeException e) {
				AutoItogModule.logger.log(WOLogLevel.WARNING,"Error collecting related",
						new Object[] {this,e});
			} finally {
				ec.unlock();
			}
    		return result;
    	}
    	NSMutableArray related = new NSMutableArray();
    	String entName = calculator().reliesOnEntity();
    	Enumeration enu = found.objectEnumerator();
    	while (enu.hasMoreElements()) {
			EOEnterpriseObject ir = (EOEnterpriseObject) enu.nextElement();
			Integer relKey = (Integer)ir.valueForKey("relKey");
			try {
				related.addObject(EOUtilities.objectWithPrimaryKeyValue(
						editingContext(), entName, relKey));
			} catch (RuntimeException e) {
				AutoItogModule.logger.log(WOLogLevel.WARNING,
						"Could not get related object: " + entName + ':' + relKey
						,new Object[] {this,e});
			}
		}
    	return related.immutableClone();
    }
    
    public boolean evening() {
    	Calendar cal = Calendar.getInstance();
    	cal.setTime(fireTime());
    	int eveningHour = SettingsReader.intForKeyPath("edu.eveningHour", 17);
    	return (cal.get(Calendar.HOUR_OF_DAY) >= eveningHour);
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
			NSDictionary values = new NSDictionary(new Object[] {course,ai,relKey},
					new String[] {"course","autoItog","relKey"});
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
		NSDictionary values = new NSDictionary(new Object[] {course,this,relKey},
				new String[] {"course","autoItog","relKey"});
		EOEditingContext ec = course.editingContext();
		NSArray found = EOUtilities.objectsMatchingValues(ec, "ItogRelated", values);
		boolean result = (found == null || found.count() == 0);
		if(result) {
			EOEnterpriseObject rel = EOUtilities.createAndInsertInstance(ec, "ItogRelated");
			rel.addObjectToBothSidesOfRelationshipWithKey(this, "autoItog");
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
		NSDictionary values = new NSDictionary(new Object[] {course,this,relKey},
				new String[] {"course","autoItog","relKey"});
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
}
