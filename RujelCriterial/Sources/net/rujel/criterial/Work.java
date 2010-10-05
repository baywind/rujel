// Work.java

/*
 * Copyright (c) 2008, Gennady & Michael Kushnir
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 * 
 * 	•	Redistributions of source code must retain the above copyright notice, this
 * 		list of conditions and the following disclaimer.
 * 	•	Redistributions in binary form must reproduce the above copyright notice,
 * 		this list of conditions and the following disclaimer in the documentation
 * 		and/or other materials provided with the distribution.
 * 	•	Neither the name of the RUJEL nor the names of its contributors may be used
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

package net.rujel.criterial;

import net.rujel.reusables.*;
import net.rujel.base.BaseLesson;
import net.rujel.base.Indexer;
import net.rujel.base.MyUtility;
import net.rujel.base.SettingsBase;
import net.rujel.base.BaseLesson.TaskDelegate;
import net.rujel.interfaces.*;

import com.webobjects.foundation.*;
import com.webobjects.eocontrol.*;
import com.webobjects.eoaccess.*;
import com.webobjects.appserver.WOApplication;
import java.math.*;
import java.util.Enumeration;
import java.util.logging.Logger;

public class Work extends _Work implements EduLesson {	// EOObserving
	public transient FractionPresenter _integralPresenter;

	public Work() {
		super();
	}

	protected static EOGlobalID defaultType;
	public void awakeFromInsertion(EOEditingContext ctx) {
		super.awakeFromInsertion(ctx);
		super.setWeight(BigDecimal.ZERO);
		Integer zero = new Integer(0);
		setLoad(zero);
		setFlags(zero);
		setAnnounce(new NSTimestamp());
		setWorkType(WorkType.defaultType(ctx));
	}
	
	public FractionPresenter integralPresenter() {
		if(_integralPresenter == null) {
			boolean weightless = !hasWeight();//(weight().compareTo(BigDecimal.ZERO) == 0);
			String key = (weightless)?"presenters.weightless":"presenters.workIntegral";
			EOEditingContext ec = editingContext();
			EOEnterpriseObject setting = SettingsBase.settingForCourse(key, course(), ec);
			if(setting != null) {
				Integer pKey = (Integer)setting.valueForKey(SettingsBase.NUMERIC_VALUE_KEY);
				key = (String)setting.valueForKeyPath(SettingsBase.TEXT_VALUE_KEY);
				if (pKey != null) {
					_integralPresenter = (BorderSet)EOUtilities.objectWithPrimaryKeyValue(
							ec, BorderSet.ENTITY_NAME, pKey);
				} else if(key != null) {
					_integralPresenter = BorderSet.fractionPresenterForTitle(ec, key);
				}
			}
			if(_integralPresenter == null) {
				if(weightless)
					_integralPresenter = new FractionPresenter.None("#"); 
						//FractionPresenter.NONE;
				else
					_integralPresenter = FractionPresenter.PERCENTAGE;
			}
		}
		return _integralPresenter;
	}
	
	/*
	public void setIntegralPresenterKey(String key) {
		_integralPresenter = BorderSet.fractionPresenterForTitle(editingContext(),key);
	}
	public void setIntegralPresenterDate(NSTimestamp date) {
		if(_activeCriterion == null)
			throw new IllegalStateException("Could not determine, required presenter key");
		_integralPresenter = BorderSet.fractionPresenterForTitleAndDate(editingContext(),_activeCriterion,date);
	}*/

/*
    // If you add instance variables to store property values you
    // should add empty implementions of the Serialization methods
    // to avoid unnecessary overhead (the properties will be
    // serialized for you in the superclass).
    private void writeObject(java.io.ObjectOutputStream out) throws java.io.IOException {
    }

    private void readObject(java.io.ObjectInputStream in) throws java.io.IOException, java.lang.ClassNotFoundException {
    }
*/

	
	private transient NSArray _usedCriteria;
	public NSArray usedCriteria() {
		if(_usedCriteria == null) {
			if(criterMask() == null || criterMask().count() == 0) return NSArray.EmptyArray;
			NSMutableArray result = criterMask().mutableClone();
			EOSortOrdering.sortArrayUsingKeyOrderArray(result,CriteriaSet.sorter);
			_usedCriteria = (NSArray)result.valueForKey("criterion");
		}
		return _usedCriteria;
	}
	
	public boolean noCriteria() {
		if(usedCriteria().count() == 0)
			return true;
		if(usedCriteria().count() > 1)
			return false;
		Integer crit = (Integer)usedCriteria().objectAtIndex(0);
		return (crit.intValue() == 0);
	}
	
	private transient NSArray _allCriteria;
	public NSArray allCriteria() {
		if(_allCriteria == null) {
			_allCriteria = allCriteria(CriteriaSet.maxCriterionForCourse(course()));
		}
		return _allCriteria;
	}
	
	public static NSArray allCriteria(int max) {
		if(max <= 0)
			return new NSArray(new Integer(0));
		Integer[] result = new Integer[max];
		for (int i = 0; i < max; i++) {
			result[i] = new Integer(i + 1);
		}
		return new NSArray(result);
	}
	
	private transient Object _critSet;
	public CriteriaSet critSet() {
		if(_critSet == null) {
			_critSet = CriteriaSet.critSetForCourse(course());
			if(_critSet == null)
				_critSet = NullValue;
		}
		return (_critSet == NullValue)?null:(CriteriaSet)_critSet;
	}
	
	@Deprecated
	public EOEnterpriseObject criterMaskNamed(String critName) {
		NSArray mask = criterMask();
		if(mask == null || mask.count() == 0)
			return null;
		Enumeration enu = mask.objectEnumerator();
		while (enu.hasMoreElements()) {
			EOEnterpriseObject crit = (EOEnterpriseObject) enu.nextElement();
			if(critName.equals(crit.valueForKeyPath("criterion.title")))
				return crit;
		}
		return null;
	}
	
	public EOEnterpriseObject getCriterMask(Integer criter) {
		NSArray mask = criterMask();
		if(mask == null || mask.count() == 0)
			return null;
		Enumeration enu = mask.objectEnumerator();
		while (enu.hasMoreElements()) {
			EOEnterpriseObject crit = (EOEnterpriseObject) enu.nextElement();
			if(criter.equals(crit.valueForKeyPath("criterion")))
				return crit;
		}
		return null;
	}
	
	public Object maxForCriter(Integer criter) {
		EOEnterpriseObject mask = getCriterMask(criter);
		if(mask == null)
			return null;
		Integer max = (Integer)mask.valueForKey("max");
		if(critSet() == null)
			return max;
		Indexer indexer = critSet().indexerForCriter(criter);
		if(indexer == null)
			return max;
		String value = indexer.valueForIndex(max.intValue(), null);
		if(value == null)
			return max;
		if(value.length() <= 3 || 
				(value.charAt(0) == '&' && value.length() < 10))
			return value;
		return "&bull;";
	}
	
	/*
	public void objectWillChange(Object object) {
		if(object == criteriaSet()) {
			nullify();
			EOObserverCenter.removeObserver(this,object);
		}
	}*/
	
	@Deprecated
	protected boolean specCriterion(String criterion) {
		if(criterion == null) return true;
		return (criterion.equals(integralPresenter().title()) || "text".equals(criterion));
	}

	@Deprecated
	public NSArray marksForStudentOrCriterion(Student student,String criterion) {
		if(student == null && criterion == null)
			return marks();
		int idx = (criterion == null)?NSArray.NotFound:usedCriteria().indexOfObject(criterion);
		if ((criterion == null)?_marksIndex==null:
					(!specCriterion(criterion)&&(idx==NSArray.NotFound || _marksIndex==null))) {
			NSMutableArray qa = new NSMutableArray();
			if(student != null)
				qa.addObject(new EOKeyValueQualifier(
						"student",EOQualifier.QualifierOperatorEqual,student));
			if(criterion != null)
				qa.addObject(new EOKeyValueQualifier(
						"criterion",EOQualifier.QualifierOperatorEqual,criterion));
			EOQualifier qual = new EOAndQualifier(qa);
			return EOQualifier.filteredArrayWithQualifier(marks(),qual);
		} else {
			if(criterion == null) 
				return new NSArray((Object[])forPersonLink(student));
				//return new NSArray((Object[])marksIndex().objectForKey(student));
			if("text".equals(criterion)) {
				NSArray comments = (NSArray)storedValueForKey("comments");
				if(student == null) return comments;
				EOQualifier q = new EOKeyValueQualifier(
						"student",EOQualifier.QualifierOperatorEqual,student);
				comments = EOQualifier.filteredArrayWithQualifier(comments,q);
				//if(comments == null || comments.count() == 0) return null;
				return comments;
			}
			if(integralPresenter() != null && integralPresenter().title().equals(criterion)) {
				if(student != null)
					return new NSArray(integralForStudent(student,integralPresenter()));
				Enumeration ken = marksIndex().keyEnumerator();
				NSMutableArray result = new NSMutableArray();
				//String[] keys = new String[] {"student","value"};
				while (ken.hasMoreElements()) {
					Object ckey = ken.nextElement();
					result.addObject(integralForStudent((Student)ckey,integralPresenter()));
				}
				return result;
			}
			Enumeration en = marksIndex().objectEnumerator();
			Object[] cur = null;
			NSMutableArray result = new NSMutableArray();
			while (en.hasMoreElements()) {
				cur = (Object[])en.nextElement();
				result.addObject(cur[idx]);
			}
			return result.immutableClone();
		}
	}

	public String criterName(Integer criter) {
		return CriteriaSet.critNameForNum(criter, critSet());
	}
	
	public Integer critForName(String critName) {
		if(critName == null || critName.length() == 0)
			return new Integer(0);
		if(critSet() != null)
			return critSet().criterionForName(critName);
		if(critName.length() > 0)
			return null;
		char ch = critName.charAt(0);
		ch = Character.toUpperCase(ch);
		return new Integer(1 + (int)(ch - 'A'));
	}
	
	public Mark markForStudentAndCriterion(Student student,String criterion) {
		if(student == null)
			throw new NullPointerException ("Parameter 'student' must be non null");
		if("text".equals(criterion))
			throw new IllegalArgumentException("Special marks are not accessible by this method");

		if(criterion == null || 
				(integralPresenter() != null && criterion.equals(integralPresenter().title()))) {
			return markForStudentAndCriterion(student,new Integer(0));
		}
		Integer crit = critForName(criterion);
		return markForStudentAndCriterion(student, crit);
	}

	public Mark markForStudentAndCriterion(Student student,Integer criterion) {
		int idx = usedCriteria().indexOf(criterion);
		if(idx == NSArray.NotFound) {
			if(_oddMarksIndex==null) return null;
			NSDictionary oddMarks = (NSDictionary)_oddMarksIndex.objectForKey(criterion);
			if(oddMarks==null) return null;
			return (Mark)oddMarks.objectForKey(student);
		} else {
			Mark[] marks = forPersonLink(student);
			if(marks == null) return null;
			return marks[idx];
		}
	}
	
	protected Boolean weightToMax; 
	public BigDecimal integralForStudent(Student student) {
		NSArray criterMask = criterMask();
		if(criterMask == null || criterMask.count() == 0)
			return null;
		Mark[] marks = forPersonLink(student);// (Mark[])marksIndex().objectForKey(student);
		if(marks == null)
			return null;
		criterMask = EOSortOrdering.sortedArrayUsingKeyOrderArray(criterMask, CriteriaSet.sorter);
//		Number weightSum = (Number)criterMask.valueForKeyPath("@sum.weight");
//		if(weightSum == null)
//			throw new IllegalStateException("Can't get sum weight for integral calculation");
		BigDecimal decimalWeightSum = BigDecimal.ZERO;//new BigDecimal(weightSum.intValue());
		BigDecimal sum = BigDecimal.ZERO;
		BigDecimal result = null;
		for (int i = 0; i < marks.length; i++) {
			EOEnterpriseObject mask = (EOEnterpriseObject)criterMask.objectAtIndex(i);
			Number num = (Number)mask.valueForKey("max");
			if(num == null || num.intValue() == 0) {
				Logger.getLogger("rujel.criterial").log(
						WOLogLevel.WARNING,"Found zero max",mask);
				return BigDecimal.ZERO;
			}
			BigDecimal max = new BigDecimal(num.intValue());
			num = (Number)mask.valueForKey("weight");
			if(num == null) {
				if(critSet() != null) {
					EOEnterpriseObject criterion = critSet().criterionForNum(
							(Integer)mask.valueForKey("criterion"));
					if(criterion != null)
						num = (Integer)criterion.valueForKey("dfltWeight");
				}
				if(num == null && weightToMax == null) {
					weightToMax = new Boolean(SettingsBase.numericSettingForCourse(
							"weightToMax", course(), editingContext(), 1) > 0);
				}
			}
			BigDecimal weight = (num == null)?((weightToMax.booleanValue())?
					max : BigDecimal.ONE) : new BigDecimal(num.intValue());
			if(marks[i] != null) {
				Number val = marks[i].value();
				if(val == null || val.intValue() > max.intValue())
					return null;
				BigDecimal value = new BigDecimal(val.intValue());
				value = (value.multiply(weight)).divide(max,6,BigDecimal.ROUND_CEILING);
				sum = sum.add(value);
			}
			decimalWeightSum = decimalWeightSum.add(weight);
		}
		if(decimalWeightSum.intValue() == 0)
			return null;
		result = sum.divide(decimalWeightSum,4,BigDecimal.ROUND_HALF_UP);
		return result;
	}
	
	
	public String integralForStudent(Student student, FractionPresenter pres) {
		BigDecimal integral = integralForStudent(student);
		if(integral == null)
			return null;
		return pres.presentFraction(integral);
	}
	
	public NSArray allValues() {
		return marks();
	}
	public int count() {
		return marksIndex().count();
	}

	public Mark[] forPersonLink(PersonLink pers) {
		return (Mark[])marksIndex().objectForKey(pers);
	}
	
	
	private transient NSMutableDictionary _marksIndex;
	private transient NSMutableDictionary _oddMarksIndex;
//	private transient NSMutableArray _oddMarks;
//	private transient NSMutableArray _uninitialisedMarks;
	protected NSDictionary marksIndex() {
		if(_marksIndex == null) {
			_marksIndex = new NSMutableDictionary();
			Enumeration en = marks().objectEnumerator();
			while (en.hasMoreElements()) {
				Mark curr = (Mark)en.nextElement();
				placeToIndex(curr);
				
			}
		}
/*		if(_uninitialisedMarks != null && _uninitialisedMarks.count() > 0) { 
			//check if any mark got initialised
			for (int i = _uninitialisedMarks.count() -1; i >= 0; i--) {
				Mark curr = (Mark)_uninitialisedMarks.objectAtIndex(i);
				if(curr.student() != null && curr.criterion() != null) {
					placeToIndex(curr);
					_uninitialisedMarks.removeObjectAtIndex(i);
				}
			}
		}
*/		return _marksIndex;
	}
	
	protected void placeToIndex(Mark mark) {
		if(_marksIndex == null) marksIndex();
		if(mark.criterion() == null || mark.student() == null) { //mark is not initialised yet
/*			if(_uninitialisedMarks == null) {
				_uninitialisedMarks = new NSMutableArray(mark);
			} else {
				_uninitialisedMarks.addObject(mark);
			}
*/			return;
		}	//uninitialised mark
			int idx = usedCriteria().indexOfObject(mark.criterion());
		if(idx == NSArray.NotFound) { //mark for wrong criterion
/*			if(_oddMarks == null) {
				_oddMarks = new NSMutableArray(mark);
			} else {
				_oddMarks.addObject(mark);
			}*/
			NSMutableDictionary marks = null;
			if(_oddMarksIndex == null) {
				marks = new NSMutableDictionary(mark,mark.student());
				_oddMarksIndex = new NSMutableDictionary(marks,mark.criterion());
			} else {
				marks = (NSMutableDictionary)_oddMarksIndex.objectForKey(mark.criterion());
				if(marks == null) {
					marks = new NSMutableDictionary(mark,mark.student());
					_oddMarksIndex.setObjectForKey(marks,mark.criterion());
				} else {
					marks.setObjectForKey(mark,mark.student());
				}
			}
			return;
		} // wrong criterion
		Mark[] marks = (Mark[])_marksIndex.objectForKey(mark.student());
		if (marks == null) {
			marks = new Mark[usedCriteria().count()];
			_marksIndex.setObjectForKey(marks,mark.student());
		}
		marks[idx] = mark;
	}
	
	public void turnIntoFault(EOFaultHandler handler) {
		super.turnIntoFault(handler);
		nullify();
	}
	public void nullify() {
		_usedCriteria = null;
        _marksIndex = null;
		_oddMarksIndex = null;
//		_oddMarks = null;
//		_uninitialisedMarks = null;
//		_access = null;
		_allCriteria=null;
		_integralPresenter = null;
		weightToMax = null;
	}
	
	public void setMarks(NSArray aValue) {
		nullify();
		super.setMarks(aValue);
    }
	
    public void addToMarks(Mark object) {
		super.addToMarks(object);
		if(_marksIndex != null)
			placeToIndex(object);
    }
	
    public void removeFromMarks(Mark object) {
		super.removeFromMarks(object);
		if(_marksIndex == null) return;
//		if(_uninitialisedMarks!=null && _uninitialisedMarks.removeObject(object)) return;
		int idx = usedCriteria().indexOfObject(object.criterion());
		if(idx == NSArray.NotFound) { //mark for wrong criterion
//			if(_oddMarks!= null)_oddMarks.removeObject(object);
			if(_oddMarksIndex != null) {
				NSMutableDictionary oddMarks = (NSMutableDictionary)
							_oddMarksIndex.objectForKey(object.criterion());
				if(oddMarks==null) return;
				if(object.equals(oddMarks.removeObjectForKey(object.student()))) {
					if(oddMarks.count() == 0) {
						_oddMarksIndex.removeObjectForKey(object.criterion());
						//					if(_oddMarksIndex.count() == 0) _oddMarksIndex = null;
					}
				}
			}
			return;
		} //mark for wrong criterion
		Mark[] marks = (Mark[])_marksIndex.objectForKey(object.student());
		if (marks != null)
		marks[idx] = null;
    }
	
    public void setCriterMask(NSArray aValue) {
        nullify();
		super.setCriterMask(aValue);
    }
	
    public void addToCriterMask(EOEnterpriseObject object) {
		nullify();
		super.addToCriterMask(object);
    }
	
    public void removeFromCriterMask(EOEnterpriseObject object) {
		nullify();
		super.removeFromCriterMask(object);
    }
	
	public EduCourse course() {
		return (EduCourse)storedValueForKey("course");
	}
	public void setCourse(EduCourse newCourse) {
		takeStoredValueForKey(newCourse,"course");
		if(newCourse != null &&(workType() == null || course() == null)) {
			EOEditingContext ec = editingContext();
			EOEnterpriseObject type = SettingsBase.settingForCourse("defaultWorkType",
					newCourse, ec);
			if(type != null) {
				Integer typeID = (Integer)type.valueForKey(SettingsBase.NUMERIC_VALUE_KEY);
				type = EOUtilities.objectWithPrimaryKeyValue(ec, "WorkType", typeID);
				setWorkType(type);
			}
		}
	}
	

	public NSArray students() {
		NSArray marked = marksIndex().allKeys();
		NSArray notes = notes();
		if(notes == null || notes.count() == 0)
			return marked;
		NSArray noted = (NSArray)notes.valueForKey("student");
		if(marked == null || marked.count() == 0)
			return noted;
		NSMutableSet result = new NSMutableSet(marked);
		result.addObjectsFromArray(noted);
		return result.allObjects();
	}
	
	public String noteForStudent(Student student) {
		return BaseLesson.noteForStudent(this, student);
	}
	public EOEnterpriseObject _newNote() {
		EOEnterpriseObject note = EOUtilities.createAndInsertInstance(
				editingContext(),"WorkNote");
		note.addObjectToBothSidesOfRelationshipWithKey(this, "work");
		return note;
	}
	public void setNoteForStudent(String newNote, Student student) {
		BaseLesson.setNoteForStudent(this, newNote, student);
	}
	
	protected static TaskDelegate taskDelegate = new TaskDelegate();
	public static void setTaskDelegate(TaskDelegate delegate) {
		taskDelegate = delegate;
	}
	
	public String taskUrl() {
		String result = homeTask();
		if(!(result == null || result.charAt(0) == '/' || result.contains("://")))
			result = "http://" + result;
		return result;
	}
	
	public String homeTask() {
		return taskDelegate.homeTaskForLesson(this);
	}
	public void setHomeTask(String newTask) {
		taskDelegate.setHomeTaskForLesson(newTask, this);
	}
	
/*	public NSTimestamp validateDate(Object aDate) throws NSValidation.ValidationException {
		return MyUtility.validateDateInEduYear(aDate,course().eduYear(),DATE_KEY);
	}
	
	public NSTimestamp validateAnnounce(Object aDate) {
		return MyUtility.validateDateInEduYear(aDate, course().eduYear(), ANNOUNCE_KEY);
	}*/
	
	public void validateForSave() throws NSValidation.ValidationException {
		super.validateForSave();
		if(announce() == null || date() == null) {
			String message = (String)WOApplication.application().valueForKeyPath(
			"strings.RujelCriterial_Strings.messages.nullDate");
			throw new NSValidation.ValidationException(message);
		}
		if(theme() != null && theme().length() > 255) {
			String message = (String)WOApplication.application().valueForKeyPath(
					"strings.RujelCriterial_Strings.messages.longText");
			message = String.format(message,new Integer(theme().length()));
			throw new NSValidation.ValidationException(message);
		}
		MyUtility.validateDateInEduYear(date(),course().eduYear(),DATE_KEY);
		MyUtility.validateDateInEduYear(announce(), course().eduYear(), ANNOUNCE_KEY);
		if(announce().compare(date()) > 0) {
			String message = (String)WOApplication.application().valueForKeyPath(
				"strings.RujelCriterial_Strings.messages.lateAnnounce");
			throw new NSValidation.ValidationException(message);
		}
		BigDecimal weight = weight();
		if(weight.compareTo(BigDecimal.ZERO) < 0 ||
				weight.compareTo(new BigDecimal(100)) >= 0) {
			String message = (String)WOApplication.application().valueForKeyPath(
				"strings.RujelCriterial_Strings.messages.illegalWeight");
			throw new NSValidation.ValidationException(message);
		}
		if(weight.scale() > 2) {
			String message = (String)WOApplication.application().valueForKeyPath(
			"strings.RujelCriterial_Strings.messages.illegalScale");
			throw new NSValidation.ValidationException(message);
		}
		NSArray criterMask = criterMask();
		if(criterMask == null || criterMask.count() == 0) {
			if(weight.compareTo(BigDecimal.ZERO) > 0) {
				String message = (String)WOApplication.application().valueForKeyPath(
				"strings.RujelCriterial_Strings.messages.critersRequired");
				throw new NSValidation.ValidationException(message);
			}
			return;
		}
		criterMask = EOSortOrdering.sortedArrayUsingKeyOrderArray
												(criterMask,CriteriaSet.sorter);
		int[] maxs = new int[criterMask.count()];
		int maxWeight = 0;
		for (int i = 0; i < maxs.length; i++) {
			EOEnterpriseObject cm = (EOEnterpriseObject)criterMask.objectAtIndex(i);
			Number max = (Number)cm.valueForKey("max");
			Integer criterion = (Integer)cm.valueForKey("criterion");
			if(criterion.intValue() == 0 && maxs.length > 1) {
				String message = (String)WOApplication.application().valueForKeyPath(
						"strings.RujelCriterial_Strings.messages.noCriteriaWithCriteria");
				throw new NSValidation.ValidationException(message);
			}
			if(max == null || max.intValue() <= 0) {
				String message = (String)WOApplication.application().valueForKeyPath(
						"strings.RujelCriterial_Strings.messages.maxShouldBeOverZero");
				message = String.format(message,criterName(criterion));
				throw new NSValidation.ValidationException(message);
			}
			maxs[i] = max.intValue();
			if(maxWeight == 0) {
				max = (Number)cm.valueForKey("weight");
				maxWeight = (max==null)?maxs[i]:max.intValue();
			}
		}
		if(maxWeight == 0) {
			String message = (String)WOApplication.application().valueForKeyPath(
					"strings.RujelCriterial_Strings.messages.critersRequired");
					throw new NSValidation.ValidationException(message);
		}
		if(_oddMarksIndex != null && _oddMarksIndex.count() > 0) {
			Enumeration en = _oddMarksIndex.keyEnumerator();
			StringBuilder buf = new StringBuilder();
			buf.append(WOApplication.application().valueForKeyPath(
				"strings.RujelCriterial_Strings.messages.oddCriteria"));
			while (en.hasMoreElements()) {
				Integer criterion = (Integer) en.nextElement();
				buf.append(' ').append('\'').append(criterName(criterion)).append('\'');
				if(en.hasMoreElements())
					buf.append(',');
			}
			throw new NSValidation.ValidationException(buf.toString());
		}
		if(marksIndex().count() == 0)
			return;
		Enumeration en = marksIndex().objectEnumerator();
		while (en.hasMoreElements()) {
			Mark[] marks = (Mark[])en.nextElement();
			for (int i = 0; i < maxs.length; i++) {
				if(marks[i] == null)
					continue;
				if(marks[i].value() == null) {
					String message = (String)WOApplication.application().valueForKeyPath(
								"strings.RujelCriterial_Strings.messages.illegalMark");
					message = String.format(message, criterName(marks[i].criterion()));
					throw new NSValidation.ValidationException(message);
				}
				if(marks[i].value().intValue() > maxs[i]) {
					String message = (String)WOApplication.application().valueForKeyPath(
								"strings.RujelCriterial_Strings.messages.markValueOverMax");
					message = String.format(message,criterName(marks[i].criterion()));
					throw new NSValidation.ValidationException(message);
				}
			}
		}
	}
	
	public boolean hasWeight() {
		return (super.weight().compareTo(BigDecimal.ZERO) != 0);
	}
	
	public void setTrimmedWeight(BigDecimal aValue) {
		if(Various.boolForObject(valueForKeyPath("workType.namedFlags.fixWeight")))
			return;
		if(weight().compareTo(aValue) != 0) {
			if(trimmedWeight().compareTo(BigDecimal.ZERO) == 0 
					|| aValue.compareTo(BigDecimal.ZERO) == 0)
				_integralPresenter = null;
			super.setWeight(aValue);
		}
	}
	
	public BigDecimal trimmedWeight() {
		BigDecimal weight = super.weight();
		if(weight == null) return null;
		if(weight.compareTo(BigDecimal.ZERO) == 0)
			return BigDecimal.ZERO;
		weight = weight.stripTrailingZeros();
		if(weight.scale() < 0)
			return weight.setScale(0);
		return weight;
	}
	
	public boolean isFlag(int flag) {
		Integer flags = flags();
		if(flags == null)
			return false;
		return ((flags.intValue() & flag) > 0);
	}
	
	protected void setIsFlag(boolean is,int flag) {
		Integer flags = flags();
		if(flags == null) {
			setFlags(new Integer(flag));
		} else {
			int newFlags = flags.intValue();
			if(is) {
				newFlags = newFlags | flag;
			} else {
				newFlags = newFlags - (newFlags & flag);
			}
			if(newFlags != flags.intValue())
				setFlags(new Integer(newFlags));
		}
	}
	
	public boolean isHometask() {
		return isFlag(16);
	}
	
	public void setIsHometask(boolean is) {
		if(Various.boolForObject(valueForKeyPath("workType.namedFlags._fixHometask")))
			setIsFlag(is,16);
	}

	public boolean isCompulsory() {
		return isFlag(8);
	}
		
	public void setIsCompulsory(boolean is) {
		if(Various.boolForObject(valueForKeyPath("workType.namedFlags._fixCompulsory")))
			setIsFlag(is,8);
	}
	
	public boolean isOptional() {
		if(!isCompulsory())
			return true;
		if(EOPeriod.Utility.compareDates(date(), null) > 0)
			return true;
		if(marks() == null || marks().count() == 0) {
			Integer lag = SettingsBase.numericSettingForCourse("countMarklessWorkAfter", 
					course(), editingContext());
			if(lag != null) {
				return (EOPeriod.Utility.countDays(date(), null) <= lag.intValue());
			}
			return true;
		}
		return false;
	}
/*
	private NamedFlags _flags;
    public NamedFlags namedFlags() {
    	if(_flags==null) {
    		_flags = new NamedFlags(flags().intValue(),WorkType.flagNames);
    		try{
    			_flags.setSyncParams(this, getClass().getMethod("setNamedFlags",
    					NamedFlags.class));
    		} catch (Exception e) {
    			Logger.getLogger("rujel.criterial").log(WOLogLevel.WARNING,
						"Could not get syncMethod for Work flags",e);
			}
    	}
    	return _flags;
    }
    
    public void setNamedFlags(NamedFlags flags) {
    	if(flags != null)
    		super.setFlags(flags.toInteger());
    	_flags = flags;
    }
    
    public void setFlags(Integer value) {
    	_flags = null;
    	super.setFlags(value);
    }*/

	public void setWorkType(EOEnterpriseObject workType) {
		EOEnterpriseObject prevType = workType();
		if(workType == prevType)
			return;
		super.setWorkType(workType);
		if(workType != null) {
			Integer flags = (Integer)workType.valueForKey("dfltFlags");
			if(flags == null) {
				flags = new Integer(0);
			} else {
				flags = new Integer(flags.intValue() & 24);
			}
			setFlags(flags);
			setWeight((BigDecimal)workType.valueForKey("dfltWeight"));
		}
	}
    
    public String presentLoad() {
		if(load() == null)
			return null;
		int minutes = load().intValue();
		int hours = minutes / 60;
		minutes = minutes % 60;
		StringBuffer result = new StringBuffer(5);
		if(hours < 10)
			result.append('0');
		result.append(hours).append(':');
		if(minutes < 10)
			result.append('0');
		result.append(minutes);
		return result.toString();
	}

    public String color() {
    	String result = null;
    	if(Various.boolForObject(valueForKeyPath("workType.namedFlags.unused")))
    		return "#999999";
    	if(hasWeight()) {
    		result = (String)valueForKeyPath("workType.colorWeight");
    		if(result == null)
    			return "#ff9966";
    	} else {
    		result = (String)valueForKeyPath("workType.colorNoWeight");
    		if(result == null)
    			return "#ffcc66";
    	}
    	return result;
    }
}