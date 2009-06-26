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
import net.rujel.auth.*;
import net.rujel.base.BaseLesson;
import net.rujel.base.MyUtility;
import net.rujel.base.BaseLesson.TaskDelegate;
import net.rujel.interfaces.*;

import com.webobjects.foundation.*;
import com.webobjects.eocontrol.*;
import com.webobjects.eoaccess.*;
import com.webobjects.appserver.WOSession;
import com.webobjects.appserver.WOApplication;
import java.math.*;
import java.util.Enumeration;
import java.util.logging.Logger;

public class Work extends _Work implements UseAccessScheme,EduLesson {	// EOObserving
	public transient FractionPresenter _integralPresenter;

	public Work() {
		super();
	}

	public void awakeFromInsertion(EOEditingContext ctx) {
		super.awakeFromInsertion(ctx);
		super.setWeight(BigDecimal.ZERO);
		Integer zero = new Integer(0);
		setLoad(zero);
		setType(zero);
		setAnnounce(new NSTimestamp());
	}
	
	public FractionPresenter integralPresenter() {
		if(editingContext() instanceof SessionedEditingContext) {
			WOSession ses = ((SessionedEditingContext)editingContext()).session();
			FractionPresenter result = (FractionPresenter)ses.objectForKey("integralPresenter");
			return (result==null)?_integralPresenter:result;
		} else 
			return _integralPresenter;
	}
	
	public void setIntegralPresenterKey(String key) {
		NSTimestamp today = null;
		if(editingContext() instanceof SessionedEditingContext) {
			WOSession ses = ((SessionedEditingContext)editingContext()).session();
			today = (NSTimestamp)ses.valueForKey("today");
		}
		if(today == null)
			today = new NSTimestamp();
		_integralPresenter = BorderSet.fractionPresenterForTitleAndDate(editingContext(),key,today);
	}
	/*
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
	public static final NSArray accessKeys = new NSArray (new String[] {
			"read","create","edit","delete","setMarks","changeCritSet"});
	
	private transient NamedFlags _access;
	public NamedFlags access() {
		if(_access == null) {
			_access = StaticImplementation.access(this,accessKeys);
		}
		return _access.immutableClone();
	}
	
	public boolean isOwned() {
		return StaticImplementation.isOwned(this);
	}
	
	private transient NSArray _usedCriteria;
	public NSArray usedCriteria() {
		if(_usedCriteria == null) {
			if(criterMask() == null || criterMask().count() == 0) return NSArray.EmptyArray;
			NSMutableArray result = ((NSArray)criterMask().valueForKey("criterion")).mutableClone();
			EOSortOrdering so = EOSortOrdering.sortOrderingWithKey(
					"sort",EOSortOrdering.CompareAscending);
			EOSortOrdering.sortArrayUsingKeyOrderArray(result,new NSArray(so));
			//result = ((NSArray)result.valueForKey("title")).mutableClone();
			_usedCriteria = (NSArray)result.valueForKey("title");//result.immutableClone();
		}
		return _usedCriteria;
	}
	
	private transient NSArray _allCriteria;
	public NSArray allCriteria() {
		if(_allCriteria == null) {
			_allCriteria = CriteriaSet.criteriaForCycle(course().cycle());
		}
		return _allCriteria;
	}
	
	
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
	
	protected boolean specCriterion(String criterion) {
		if(criterion == null) return true;
		return (criterion.equals(integralPresenter().title()) || "text".equals(criterion));
	}
	/*
	public void objectWillChange(Object object) {
		if(object == criteriaSet()) {
			nullify();
			EOObserverCenter.removeObserver(this,object);
		}
	}*/
	public NSArray marksForStudentOrCriterion(Student student,EOEnterpriseObject criterion) {
		if(criterion != null)
			return marksForStudentOrCriterion(student,(String)criterion.valueForKey("title"));
		else
			return marksForStudentOrCriterion(student,(String)null);
	}
	
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
	
	public Mark markForStudentWithoutCriterion(Student student) {
		if(student == null || (_marksIndex != null && _uninitialisedMarks == null)) return null;
		EOQualifier qual = EOQualifier.qualifierWithQualifierFormat(
				"criterion = nil AND student = %@",new NSArray(student));
		NSArray toFilter = (_uninitialisedMarks != null)?_uninitialisedMarks:marks();
		NSArray result = EOQualifier.filteredArrayWithQualifier(toFilter,qual);
		if(result == null || result.count() == 0)
			return null;
		return (Mark)result.objectAtIndex(0);
	}
	
	public Mark markForStudentAndCriterion(Student student,EOEnterpriseObject criterion) {
		return markForStudentAndCriterion(student,(String)criterion.valueForKey("title"));
	}
	
	public Mark markForStudentAndCriterion(Student student,String criterion) {
		if(student == null)
			throw new NullPointerException ("Parameter 'student' must be non null");
		if("text".equals(criterion))
			throw new IllegalArgumentException("Special marks are not accessible by this method");

		if(criterion == null || 
				(integralPresenter() != null && criterion.equals(integralPresenter().title()))) {
			return markForStudentWithoutCriterion(student);
		}
		int idx = usedCriteria().indexOf(criterion);
		Mark[] marks = forPersonLink(student);//(Mark[])marksIndex().objectForKey(student);
		if(idx == NSArray.NotFound) {
			if(_oddMarksIndex==null) return null;
			NSDictionary oddMarks = (NSDictionary)_oddMarksIndex.objectForKey(student);
			if(oddMarks==null) return null;
			return (Mark)oddMarks.objectForKey(criterion);
		} else {
			if(marks == null) return null;
			return marks[idx];
		}
		/*NSArray result = marksForStudentOrCriterion(student,criterion);
		if(result == null || result.count() == 0)
			return null;
		if(result.count() > 1)
			throw new IllegalStateException("Multiple marks found");
		return (Mark)result.objectAtIndex(0); */
	}
	
//	private NSMutableDictionary _integrals;
	public BigDecimal integralForStudent(Student student) {
		BigDecimal decimalWeightSum = null;
		BigDecimal result = null;
//		if(_integrals == null) {
			Number weightSum = (Number)criterMask().valueForKeyPath("@sum.weight");
			if(weightSum == null)
				throw new IllegalStateException("Can't get sum wieght for integral calculation");
			decimalWeightSum = new BigDecimal(weightSum.intValue());
/*			 _integrals = new NSMutableDictionary(decimalWeightSum,"weightSum");
		} else {
			result = (BigDecimal)_integrals.objectForKey(student);
			if(result == null) {
				decimalWeightSum = (BigDecimal)_integrals.objectForKey("weightSum");
			} else {
				return result;
			}
		}*/
		Mark[] marks = forPersonLink(student);// (Mark[])marksIndex().objectForKey(student);
		if(marks == null) return null;
		/*if(weight() == null || weight().compareTo(BigDecimal.ZERO) == 0) {
			if(SettingsReader.boolForKeyPath("edu.noWeightlessIntegral", false))
				return BigDecimal.ZERO;
		}*/
			
		//MathContext mc = new MathContext(4,RoundingMode.CEILING);
		BigDecimal sum = BigDecimal.ZERO;
		
		for (int i = 0; i < marks.length; i++) {
			if(marks[i] != null) {
				BigDecimal value = new BigDecimal(marks[i].value().intValue());
				Number maxNum = (Number)marks[i].valueForKeyPath("criterMask.max");
				if(maxNum == null || maxNum.intValue() == 0) {
					Logger.getLogger("rujel.criterial").log(
							WOLogLevel.WARNING,"Found zero max for mark",marks[i]);
					return BigDecimal.ZERO;
				}
				BigDecimal max = new BigDecimal(maxNum.intValue());
				BigDecimal weight = new BigDecimal(
						((Number)marks[i].criterMask().valueForKey("weight")).intValue());
				value = (value.multiply(weight)).divide(max,6,BigDecimal.ROUND_CEILING);
				sum = sum.add(value);
			}
		}
		result = sum.divide(decimalWeightSum,4,BigDecimal.ROUND_HALF_UP);
//		_integrals.setObjectForKey(result,student);
		return result;
	}
	
	
	public String integralForStudent(Student student, FractionPresenter pres) {
		return pres.presentFraction(integralForStudent(student));
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
	private transient NSMutableArray _oddMarks;
	private transient NSMutableArray _uninitialisedMarks;
	protected NSDictionary marksIndex() {
		if(_marksIndex == null) {
			_marksIndex = new NSMutableDictionary();
			Enumeration en = marks().objectEnumerator();
			while (en.hasMoreElements()) {
				Mark curr = (Mark)en.nextElement();
				placeToIndex(curr);
				
			}
		}
		if(_uninitialisedMarks != null && _uninitialisedMarks.count() > 0) { 
			//check if any mark got initialised
			for (int i = _uninitialisedMarks.count() -1; i >= 0; i--) {
				Mark curr = (Mark)_uninitialisedMarks.objectAtIndex(i);
				if(curr.student() != null && curr.criterion() != null) {
					placeToIndex(curr);
					_uninitialisedMarks.removeObjectAtIndex(i);
				}
			}
		}
		return _marksIndex;
	}
	
	protected void placeToIndex(Mark curr) {
		if(_marksIndex == null) marksIndex();
		if(curr.criterion() == null || curr.student() == null) { //mark is not initialised yet
			if(_uninitialisedMarks == null) {
				_uninitialisedMarks = new NSMutableArray(curr);
			} else {
				_uninitialisedMarks.addObject(curr);
			}
			return;
		}	//uninitialised mark
			int idx = usedCriteria().indexOfObject(curr.criterion().valueForKey("title"));
		if(idx == NSArray.NotFound) { //mark for wrong criterion
			if(_oddMarks == null) {
				_oddMarks = new NSMutableArray(curr);
			} else {
				_oddMarks.addObject(curr);
			}
			NSMutableDictionary marks = null;
			if(_oddMarksIndex == null) {
				marks = new NSMutableDictionary(curr,curr.criterion().valueForKey("title"));
				_oddMarksIndex = new NSMutableDictionary(marks,curr.student());
			} else {
				marks = (NSMutableDictionary)_oddMarksIndex.objectForKey(curr.student());
				if(marks == null) {
					marks = new NSMutableDictionary(curr,curr.criterion());
					_oddMarksIndex.setObjectForKey(marks,curr.student());
				} else {
					marks.setObjectForKey(curr,curr.criterion());
				}
			}
			return;
		} // wrong criterion
		Mark[] marks = (Mark[])_marksIndex.objectForKey(curr.student());
		if (marks == null) {
			marks = new Mark[usedCriteria().count()];
			_marksIndex.setObjectForKey(marks,curr.student());
		}
		marks[idx] = curr;
	}
	
	public void turnIntoFault(EOFaultHandler handler) {
		super.turnIntoFault(handler);
		nullify();
	}
	public void nullify() {
		//_mask = null;
		_usedCriteria = null;
        _marksIndex = null;
		_oddMarksIndex = null;
		_oddMarks = null;
		_uninitialisedMarks = null;
		//_studentMarkSets = null;
		//_editableMarksForStudent = null;
		_access = null;
		_allCriteria=null;
//		_integrals = null;
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
	
    public void removeFromMarks(net.rujel.criterial.Mark object) {
		super.removeFromMarks(object);
		if(_marksIndex == null) return;
		if(_uninitialisedMarks!=null && _uninitialisedMarks.removeObject(object)) return;
		int idx = usedCriteria().indexOfObject(object.criterion().valueForKey("title"));
		if(idx == NSArray.NotFound) { //mark for wrong criterion
			if(_oddMarks!= null)_oddMarks.removeObject(object);
			if(_oddMarksIndex != null) {
				NSMutableDictionary oddMarks = (NSMutableDictionary)
							_oddMarksIndex.objectForKey(object.student());
				if(oddMarks==null) return;
				if(object.equals(oddMarks.removeObjectForKey(object.criterion()))) {
					if(oddMarks.count() == 0) {
						_oddMarksIndex.removeObjectForKey(object.student());
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
    /*
	public Teacher substitute() {
		return (Teacher)storedValueForKey("substitute");
	}
	
	public void setSubstitute(Teacher teacher) {
		takeStoredValueForKey(teacher, "substitute");
	}*/
	
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
		EOEnterpriseObject note = EOUtilities.createAndInsertInstance(editingContext(),"WorkNote");
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
		NSArray criterMask = criterMask();
		if(BigDecimal.ZERO.compareTo(weight()) != 0) {
			String message = (String)WOApplication.application().valueForKeyPath(
			"strings.RujelCriterial_Strings.messages.critersRequired");
			if(criterMask == null || criterMask.count() == 0)
				throw new NSValidation.ValidationException(message);
			Integer maxWeight = (Integer)criterMask.valueForKeyPath("@max.weight");
			if(maxWeight == null || maxWeight.intValue() == 0)
				throw new NSValidation.ValidationException(message);
		}
		if(criterMask == null || criterMask.count() == 0)
			return;
		Enumeration en = criterMask().objectEnumerator();
		while (en.hasMoreElements()) {
			EOEnterpriseObject mask = (EOEnterpriseObject)en.nextElement();
			Number critMax = (Number)mask.valueForKey("max");
			Number markMax = (Number)mask.valueForKeyPath("marks.@max.value");
			if(critMax == null || critMax.intValue() <= 0) {
				String message = (String)WOApplication.application().valueForKeyPath(
						"strings.RujelCriterial_Strings.messages.maxShouldBeOverZero");
				message = String.format(message,mask.valueForKeyPath("criterion.title"));
				throw new NSValidation.ValidationException(message);
			}
			if(markMax != null && markMax.intValue() > critMax.intValue()) {
				String message = (String)WOApplication.application().valueForKeyPath(
						"strings.RujelCriterial_Strings.messages.markValueOverMax");
				message = String.format(message,mask.valueForKeyPath("criterion.title"));
				throw new NSValidation.ValidationException(message);
			}
		}
	}
	
	public AccessSchemeAssistant assistantForAttribute(String attribute, NSArray useAccessKeys) {
		return null;
	}
	
	public NamedFlags accessForAttribute (String attribute, NSArray useAccessKeys) {
		if(editingContext() == null) return DegenerateFlags.ALL_FALSE;
		UserPresentation user = (UserPresentation)valueForKeyPath("editingContext.session.user");
		if(user == null)
			throw new IllegalStateException ("Can't get user to determine access");
		if(useAccessKeys == null) useAccessKeys = UseAccess.accessKeys;
		String request = "Work." + attribute;
		if("marks".equals(attribute))
			request = "Mark";
		else if("notes".equals(attribute))
			request = "BaseNote";
		try {
			int level = user.accessLevel(request);
			NamedFlags result = new ImmutableNamedFlags(level,useAccessKeys);
			return result;
		} catch (AccessHandler.UnlistedModuleException e) {
//			Logger.getLogger("auth").logp(Level.WARNING,
//					"UseAccess.StaticImplementation","access",
//					"Undefined access to module : returning full access",
//					new Object[] {valueForKeyPath("editingContext.session"),request});
			return  null;
		}
	}
	
	public void setWeight(BigDecimal aValue) {
       if(weight().compareTo(aValue) != 0)
		   super.setWeight(aValue);
    }
	
	public BigDecimal weight() {
		BigDecimal weight = super.weight();
		if(weight == null) return null;
		if(weight.compareTo(BigDecimal.ZERO) == 0)
			return BigDecimal.ZERO;
		weight = weight.stripTrailingZeros();
		if(weight.scale() < 0)
			weight.setScale(0);
		return weight;
	}
	
	public static final int CLASSWORK = 0;
	public static final int HOMEWORK = 1;
	public static final int PROJECT = 2;
	public static final int OPTIONAL = 3;
	protected static NSArray workTypes = new NSArray (
			new String[] {"classwork","homework","project","optional"});
	
	public static NSArray workTypes() {
		return workTypes;
	}
	
	public static void initTypes() {
		NSDictionary types = (NSDictionary)WOApplication.application().valueForKeyPath(
				"strings.RujelCriterial_Strings.workTypes");
		if(types == null || types.count() == 0)
			return;
		NSMutableArray result = new NSMutableArray();
		Enumeration enu = workTypes.objectEnumerator();
		while (enu.hasMoreElements()) {
			String type = (String) enu.nextElement();
			String value = (String)types.objectForKey(type);
			result.addObject((value == null)?type:value);
		}
		workTypes = result.immutableClone();
	}
	
	public String workType() {
		return (String)workTypes.objectAtIndex(type().intValue());
	}
	public void setWorkType(String workType) {
		int value = 0;
		if(workType != null)
			value = workTypes.indexOfObject(workType);
		if(value < 0)
			value = 0;
		setType(new Integer(value));
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
	
	public String styleClass() {
		if(type() == null)
			return null;
		switch (type().intValue()) {
		case CLASSWORK:
			if(weight() != null && weight().compareTo(BigDecimal.ZERO) != 0)
				return "classWeight";
			else
				return "classPlain";
		case HOMEWORK:
			if(weight() != null && weight().compareTo(BigDecimal.ZERO) != 0)
				return "homeWeight";
			else
				return "homework";
		case PROJECT:
			return "project";
		case OPTIONAL:
			return "optional";
		default:
			return null;
		}
	}
}