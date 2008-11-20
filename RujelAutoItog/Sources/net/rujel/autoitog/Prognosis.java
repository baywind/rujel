// Prognosis.java

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

package net.rujel.autoitog;

import net.rujel.reusables.*;
import net.rujel.interfaces.*;
import net.rujel.eduresults.*;
import net.rujel.base.EntityIndex;
import net.rujel.criterial.BorderSet;

import com.webobjects.foundation.*;
import com.webobjects.eocontrol.*;
import com.webobjects.eoaccess.*;

import java.math.*;
import java.util.Enumeration;
import java.util.logging.Logger;

public class Prognosis extends _Prognosis {
    public static final NSArray flagNames = new NSArray(new String[]
                                                       {"disable","keep","keepBonus"});
	protected static Logger logger = Logger.getLogger("rujel.autoitog");
	//protected static final String presenterKey = SettingsReader.stringForKeyPath("edu.prognosisPresenterKey","/5");
	//protected static final String calculator = prefs.get("edu.prognosisCalculator","OldFormulaStatsCalculator");
	
    public Prognosis() {
        super();
    }
	
	public static void init() {
		//		EOInitialiser.initialiseRelationship("ItogMark","teacher",false,"teacherID","Teacher");
		EOInitialiser.initialiseRelationship("Prognosis","eduCourse",false,"eduCourseID","EduCourse").anyInverseRelationship().setPropagatesPrimaryKey(true);
		EOInitialiser.initialiseRelationship("Prognosis","student",false,"studentID","Student").anyInverseRelationship().setPropagatesPrimaryKey(true);
		//EOInitialiser.initialiseRelationship("Prognosis","eduPeriod",false,"periodID","EduPeriod").anyInverseRelationship().setPropagatesPrimaryKey(true);
		//EOModelGroup.defaultGroup().entityNamed("Prognosis").relationshipNamed("eduPeriod").anyInverseRelationship().setPropagatesPrimaryKey(true);
	}
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
	
    public Student student() {
        return (Student)storedValueForKey("student");
    }
	
    public void awakeFromInsertion(EOEditingContext ec) {
    	super.awakeFromInsertion(ec);
    	setFlags(new Integer(0));
    	setComplete(BigDecimal.ZERO);
    	setBonus(BigDecimal.ZERO);
    	super.setValue(BigDecimal.ZERO);
    }
    
    protected void zeroBonus() {
   		if(!BigDecimal.ZERO.equals(bonus()))
			setBonus(BigDecimal.ZERO);
    }

    public void setStudent(Student aValue) {
        takeStoredValueForKey(aValue, "student");
    }

    public EduCourse eduCourse() {
        return (EduCourse)storedValueForKey("eduCourse");
    }
	
    public void setEduCourse(EduCourse aValue) {
        takeStoredValueForKey(aValue, "eduCourse");
    }
    
    public void setValue(BigDecimal aValue) {
    	/*if(aValue != null && aValue.equals(value()))
    		return;*/
    	super.setValue(aValue);
    	//_bonus = null;
    	if(aValue == null)
    		return;
    	if(namedFlags().flagForKey("keep"))
    		return;
    	PrognosUsage pu = prognosUsage();
    	if(pu == null)
    		return;
    	BorderSet presenter = pu.borderSet();
    	if(presenter == null)
    		return;
    	if(namedFlags().flagForKey("keepBonus")) {
    		aValue.add(bonus());
    		/*EOEnterpriseObject border = presenter.borderForFraction(aValue, true);
    		super.setMark((String)border.valueForKey("title"));
    		BigDecimal topValue = (BigDecimal)border.valueForKey("least");
    		_bonus = topValue.subtract(aValue);*/
    	} else {
    		zeroBonus();
    	}
		super.setMark(presenter.presentFraction(aValue));
     }
    /*
    public void setMark(String mark) {
    	super.setMark(mark);
    	_bonus = null;
    }
    
    protected BigDecimal _bonus;
    */
    public BigDecimal calculateBonus() {
    	return calculateBonus(false);
    }
    
    public BigDecimal calculateBonus(boolean update) {
    	try {
    		EOEnterpriseObject border = prognosUsage().borderSet().
    		borderForFraction(value(), true); 
    		BigDecimal topValue = (BigDecimal)border.valueForKey("least");
    		BigDecimal bonus = topValue.subtract(value());
    		if(update) {
    			setBonus(bonus);
    			super.setMark((String)border.valueForKey("title"));
    		}
    		return bonus;
    	} catch (Exception e) {
    		logger.log(WOLogLevel.FINER, "Bonus not applicable for this prognosis", this);
    		return null;
    	}
    }
    
    public void addBonus() {
    	setBonus(calculateBonus(true));   	
    }
    
	public String bonusText() {
		return (String)valueForKeyPath("bonusTextEO.storedText");
	}
	
	public void setBonusText(String bonusText) {
		EOEnterpriseObject bonusTextEO = bonusTextEO();
		if(bonusText == null) {
			if(bonusTextEO != null)
				removeObjectFromBothSidesOfRelationshipWithKey(bonusTextEO, BONUS_TEXT_EO_KEY);
		} else {
			if(bonusTextEO == null) {
				bonusTextEO = EOUtilities.createAndInsertInstance(editingContext(), "TextStore");
				bonusTextEO.takeValueForKey(EntityIndex.indexForObject(this), "entityIndex");
				addObjectToBothSidesOfRelationshipWithKey(bonusTextEO, BONUS_TEXT_EO_KEY);
			}
			bonusTextEO.takeValueForKey(bonusText, "storedText");
		}		
	}


    public void setEduPeriod(EduPeriod period) {
    	super.setEduPeriod(period);
    	if(fireDate() == null)
    		setFireDate(period.end());
    }
    
    private NamedFlags _flags;
    public NamedFlags namedFlags() {
    	if(_flags==null) {
    		_flags = new NamedFlags(flags().intValue(),flagNames);
    		try{
    		_flags.setSyncParams(this, getClass().getMethod("setNamedFlags", NamedFlags.class));
    		} catch (Exception e) {
    			Logger.getLogger("rujel.autoitog").log(WOLogLevel.WARNING,
						"Could not get syncMethod for StudentTimeout flags",e);
			}
    	}
    	return _flags;
    }
    
    public void setNamedFlags(NamedFlags flags) {
    	_flags = flags;
    	setFlags(flags.toInteger());
    }

    
/*	
    public EduPeriod eduPeriod() {
        return (EduPeriod)storedValueForKey("eduPeriod");
    }
	
    public void setEduPeriod(EduPeriod aValue) {
        takeStoredValueForKey(aValue, "eduPeriod");
    }
*/
    public boolean isComplete() {
    	BigDecimal complete = complete();
    	if(complete == null) return false;
    	if(complete.equals(BigDecimal.ZERO) && prognosUsage().noCalculator())
    		return true;
    	return (complete.compareTo(BigDecimal.ONE) == 0);
    }

	public void turnIntoFault(EOFaultHandler handler) {
		super.turnIntoFault(handler);
		//presenter = null;
		_timeout = null;
		_usage = null;
		_flags = null;
		//_bonus = null;
		//_calc = null;
	}
	/*
	protected transient FractionPresenter presenter;
	public String presentValue() {
		BigDecimal value = value();
		if(value == null) return null;
		if(presenter == null) {
			if(eduPeriod() == null) {
				return value.toString();
			}
			String presenterKey = SettingsReader.stringForKeyPath("edu.prognosisPresenterKey","/5");
			presenter = BorderSet.fractionPresenterForTitleAndDate(editingContext(),presenterKey,eduPeriod().end());
			if(presenter == null) return value.toString();
		}
		return presenter.presentFraction(value);
	}*/
	
	protected transient Object _timeout;
	public StudentTimeout getStudentTimeout() {
		if(_timeout == null) {
			_timeout = StudentTimeout.timeoutForStudentCourseAndPeriod(student(), eduCourse(), eduPeriod());
			if(_timeout == null)
				_timeout = NullValue;
		}
		
		//if(_timeout != null) {
			if(_timeout==NullValue)
				return null;
			else
				return (StudentTimeout)_timeout;
		/*}
		NSArray timeouts = StudentTimeout.timeoutsForCourseAndPeriod(eduCourse(), eduPeriod());
		if(timeouts == null || timeouts.count() == 0) {
			_timeout = NullValue;
			return null;
		}
		if(timeouts.count() > 1) {
			Enumeration enu = timeouts.objectEnumerator();
			while (enu.hasMoreElements()) {
				StudentTimeout tout = (StudentTimeout) enu.nextElement();
				if(tout.student() == student()) {
					_timeout = tout;
					return tout;
				}
			}
		}
		_timeout = timeouts.objectAtIndex(0);
		return (StudentTimeout)_timeout;*/
	}
	
	public NSTimestamp updateFireDate() {
		CourseTimeout courseTimeout = CourseTimeout.getTimeoutForCourseAndPeriod(eduCourse(), eduPeriod());
		return updateFireDate(courseTimeout);
	}

	public void setUpdateWithCourseTimeout(CourseTimeout courseTimeout) {
		if(courseTimeout == null)
			updateFireDate();
		else
			updateFireDate(courseTimeout);
	}

	public NSTimestamp updateFireDate(CourseTimeout courseTimeout) {
		NSTimestamp result = null;
		_timeout = null;
		StudentTimeout studentTimeout = getStudentTimeout();
		if(courseTimeout != null) {
			if(studentTimeout != null)
				result = chooseDate(studentTimeout, courseTimeout);
			else
				result = courseTimeout.dueDate();
		} else {
			if(studentTimeout != null)
				 result = studentTimeout.dueDate();
			else
				result = eduPeriod().end();
		}
		setFireDate(result);
		return result;
	}
	
	public static NSTimestamp chooseDate(StudentTimeout studentTimeout,
			CourseTimeout courseTimeout) {
		NSTimestamp stDate = studentTimeout.dueDate();
		NSTimestamp crDate = courseTimeout.dueDate();
		if(courseTimeout.namedFlags().flagForKey("negative")) {
			if(studentTimeout.namedFlags().flagForKey("negative")) {
				if(stDate.compare(crDate) > 0) {
					return crDate;
				} else {
					return stDate;
				}
			}
		} else {
			if(!studentTimeout.namedFlags().flagForKey("negative")) {
				if(stDate.compare(crDate) > 0) {
					return stDate;
				} else {
					return crDate;
				}
			}
		}
		if(studentTimeout.eduCourse() != null)
			return stDate;
		if(courseTimeout.namedFlags().flagForKey("priority") && 
				!studentTimeout.namedFlags().flagForKey("priority"))
			return crDate;
		return stDate;
	}
	
	public void setLaterFireDate(NSTimestamp newDate) {
		if(fireDate() != null && newDate.compare(fireDate()) > 0)
			setFireDate(newDate);
	}

/*
	protected static NSArray filteredCalculatorUsage(NSArray cu) {
		Enumeration enu = cu.objectEnumerator();
		NSMutableArray result = new NSMutableArray();
		while (enu.hasMoreElements()) {
			EOEnterpriseObject cur = (EOEnterpriseObject) enu.nextElement();
			
		}
		return result;
	}
*/	
	public void _setPrognosUsage(PrognosUsage prognosUsage) {
		_usage = prognosUsage;
	}

	private PrognosUsage _usage;
	public PrognosUsage prognosUsage() {
		if(_usage == null)
			_usage = PrognosUsage.prognosUsage(eduCourse(), eduPeriod().periodType());
		return _usage;
	}
	/*
	public static PerPersonLink calculatePrognoses(EduCourse course, EduPeriod period) {
		PrognosUsage prognosUsage = PrognosUsage.prognosUsage(course, period.periodType());
		Calculator calculator = prognosUsage.calculator();
		if(calculator == null)
			return null;
		return calculator.calculatePrognoses(course, period);
	}*/
	
	public static Prognosis getPrognosis(
			Student student, EduCourse course, EduPeriod period, boolean create) {
		NSMutableDictionary dict = new NSMutableDictionary(period,"eduPeriod");
		dict.setObjectForKey(course,"eduCourse");
		dict.setObjectForKey(student,"student");
		Prognosis progn = null;
		EOEditingContext ec = course.editingContext();
		try {
			progn = (Prognosis)EOUtilities.objectMatchingValues(ec,"Prognosis",dict);
		} catch (EOObjectNotAvailableException onaex) {
			if(!create)
				return null;
			progn = (Prognosis)EOUtilities.createAndInsertInstance(ec,"Prognosis");
			progn.takeValuesFromDictionary(dict);
		} catch (EOUtilities.MoreThanOneException mtoex) {
			Logger.getLogger("rujel.autoitog").log(WOLogLevel.WARNING,
					"Multiple prognoses found for dictionary",dict);
			return null;
		}
		return progn;
	}

	public static PerPersonLink prognosesForCourseAndPeriod(EduCourse course, EduPeriod period) {
		NSArray args = new NSArray(new Object[] {period,course});
		NSArray found = EOUtilities.objectsWithQualifierFormat(
				course.editingContext(),"Prognosis","eduPeriod = %@ AND eduCourse = %@",args);
		Enumeration enu = found.objectEnumerator();
		PrognosUsage usage = PrognosUsage.prognosUsage(course, period.periodType());
		while (enu.hasMoreElements()) {
			Prognosis pr = (Prognosis) enu.nextElement();
			pr._usage = usage;
		}
		NSDictionary result = new NSDictionary(found,(NSArray)found.valueForKey("student"));
		return new PerPersonLink.Dictionary(result);
	}
	
	/*
	public static class Prognoses implements PerPersonLink {
		private NSDictionary storage;
		
		public Prognoses(NSDictionary dict) {
			storage = dict;
		}
		
		public NSArray allValues() {
			return storage.allValues();
		}
		
		public Object forPersonLink(PersonLink pers) {
			return storage.objectForKey(pers);
		}
	}*/
	public ItogMark convertToItogMark(NSArray itogs, boolean overwrite) {
		return convertToItogMark(itogs, overwrite, null);
	}
	
	protected static void report(String report, Object obj, StringBuffer buf) {
		if(buf == null) {
			logger.log(WOLogLevel.INFO,report,obj);
		} else {
			PersonLink student = null;
			if(obj instanceof PersonLink)
				student = (PersonLink)obj;
			else if (obj instanceof NSKeyValueCoding)
				student = (PersonLink)((NSKeyValueCoding)obj).valueForKey("student");
			buf.append(Person.Utility.fullName(student.person(), false, 2, 1, 0));
			buf.append(" : ").append(report).append('\n');
		}
	}
	
	public ItogMark convertToItogMark(NSArray itogs, boolean overwrite, StringBuffer buf) {
		if(namedFlags().flagForKey("disable")) {
			report("This prognosis was disabled",this, buf);
			return null;
			// TODO: should reset fireDate?
		}

		ItogMark itog = null;
		if(itogs != null) {
			itog = ItogMark.getItogMark(eduCourse().cycle(),eduPeriod(),student(),itogs);
		} else {
			itog = ItogMark.getItogMark(eduCourse().cycle(),eduPeriod(),student(),editingContext());
		}

		if(itog == null) {
			itog = (ItogMark)EOUtilities.createAndInsertInstance(editingContext(),"ItogMark");
			itog.addObjectToBothSidesOfRelationshipWithKey(eduPeriod(),"eduPeriod");
			itog.addObjectToBothSidesOfRelationshipWithKey(student(),"student");
			itog.addObjectToBothSidesOfRelationshipWithKey(eduCourse().cycle(),"cycle");
			setFireDate(null);
		} else {
			setFireDate(null);
			if(!overwrite) {
				report("Itog already exists",this, buf);
				return null;
			}
			itog.readFlags().setFlagForKey(true,"changed");
		}
		itog.setValue(value());
		itog.setMark(mark());
		//itog.readFlags().setFlagForKey(true,"calculated");
		itog.readFlags().setFlagForKey(false,"constituents");
		if(!isComplete()) {
			itog.readFlags().setFlagForKey(true,"incomplete");
		}
		return itog;
	}
	
	public static void convertPrognosesForCourseAndPeriod(
			EduCourse course, EduPeriod period, java.util.Date scheduled) {
		convertPrognosesForCourseAndPeriod(course, period, scheduled, null);
	}
	
	
	public static void convertPrognosesForCourseAndPeriod(
			EduCourse course, EduPeriod period, java.util.Date scheduled, StringBuffer buffer) {
		EOEditingContext ec = course.editingContext();
		NSArray args = new NSArray(new Object[] {period,course,scheduled});
		StringBuffer buf = (buffer == null)?new StringBuffer():buffer;
		buf.append(course.eduGroup().name()).append(" : ");
		buf.append(course.cycle().subject()).append(" - ");
		buf.append(Person.Utility.fullName(course.teacher().person(), false, 2, 1, 0));
		buf.append('\n');
		
		NSArray prognoses = EOUtilities.objectsWithQualifierFormat(ec,
				"Prognosis","eduPeriod = %@ AND eduCourse = %@ AND fireDate <= %@",args);
		if(prognoses == null || prognoses.count() == 0) {
			buf.append("No prognoses found for course.\n");
			if(buffer == null) {
				logger.log(WOLogLevel.INFO, buf.toString(),course);
			}
			return;
		}
		NSMutableArray students = course.groupList().mutableClone();
		
		Enumeration penu = prognoses.objectEnumerator();
		NSArray itogs = ItogMark.getItogMarks(course.cycle(),period,null,ec);
		boolean overwrite = (scheduled == null || SettingsReader.boolForKeyPath("edu.overwriteItogsScheduled", false));
		boolean enableArchive = SettingsReader.boolForKeyPath("markarchive.ItogMark", false);
cycleStudents:
			while (penu.hasMoreElements()) {
				Prognosis prognos = (Prognosis)penu.nextElement();
				int idx = students.indexOf(prognos.student());
				if(idx >= 0) {
					students.removeObjectAtIndex(idx);
				}
				// check timeout
				if(prognos.fireDate() == null) {
					//prognos.getStudentTimeout() != null  && scheduled.compareTo(prognos.getStudentTimeout().dueDate()) < 0)
					report("Prognosis is already fired", prognos, buf);
					continue cycleStudents;
				}
				if(scheduled != null && scheduled.compareTo(prognos.fireDate()) < 0) {
					report("Prognosis is timed out", prognos, buf);
					continue cycleStudents;
				}
				if(idx < 0) {
					args = new NSArray(new Object[] {period,prognos.student()});
					NSArray found = EOUtilities.objectsWithQualifierFormat(course.editingContext(),"Prognosis","eduPeriod = %@ AND student = %@",args);
					EOQualifier qual = new EOKeyValueQualifier("eduCourse.cycle",EOQualifier.QualifierOperatorEqual,course.cycle());
					found = EOQualifier.filteredArrayWithQualifier(found, qual);
					if(found.count() > 1) {
						Enumeration enu = found.objectEnumerator();
						while (enu.hasMoreElements()) {
							Prognosis crp = (Prognosis) enu.nextElement();
							if(crp.eduCourse().groupList().contains(crp.student())) {
								report("Skipping prognosis for student not in group - found another prognosis", prognos, buf);
								continue cycleStudents;
							}
						}
					}
					report("Setting mark to student not in group", prognos, buf);
				}
				ItogMark itog = prognos.convertToItogMark(itogs,overwrite, buf);
				if(itog != null && (overwrite || !itogs.containsObject(itog))) {
					itog.readFlags().setFlagForKey(scheduled != null,"scheduled");
					if(enableArchive) {
						EOEnterpriseObject archive = EOUtilities.createAndInsertInstance(ec,"MarkArchive");
						archive.takeValueForKey(itog, "object");
						archive.takeValueForKey("scheduled", "wosid");
						if(scheduled != null)
							archive.takeValueForKey("AutoItog", "user");
					}
				}
			} // cycleStudents
		if(students.count() > 0) {
			Enumeration enu = students.objectEnumerator();
			buf.append("No prognoses found for students:\n");
			while (enu.hasMoreElements()) {
				Student stu = (Student) enu.nextElement();
				buf.append(Person.Utility.fullName(stu.person(), true, 2, 1, 0)).append(", ");
			}
			buf.append('\n');
		}
		if(ec.hasChanges()) {
			try {
				ec.saveChanges();
				logger.logp(WOLogLevel.FINE,Prognosis.class.getName(), "convertPrognosesForCourseAndPeriod","Saved itogs based on prognoses for course",course);
			}  catch (Exception ex) {
				logger.logp(WOLogLevel.WARNING,Prognosis.class.getName(), 
						"convertPrognosesForCourseAndPeriod","Failed to save itogs based on prognoses for course", new Object[] {course,ex});
				buf.append("Failed to save itogs based on prognoses for course");
				ec.revert();
			}
		} else { //ec.hasChanges()
			logger.logp(WOLogLevel.FINE,Prognosis.class.getName(), "convertPrognosesForCourseAndPeriod","No itogs to save for course",course);
		}
		if(buffer == null) {		
			logger.log(WOLogLevel.INFO, buf.toString(),course);
		}
	}
}
