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
import net.rujel.base.MyUtility;
import net.rujel.criterial.BorderSet;

import com.webobjects.foundation.*;
import com.webobjects.appserver.WOApplication;
import com.webobjects.eocontrol.*;
import com.webobjects.eoaccess.*;

import java.math.*;
import java.util.Date;
import java.util.Enumeration;
import java.util.logging.Logger;

public class Prognosis extends _Prognosis {
    public static final NSArray flagNames = new NSArray(new String[]
                                                       {"disable","keep","keepBonus"});
	protected static Logger logger = Logger.getLogger("rujel.autoitog");
	
    public Prognosis() {
        super();
    }
	
	public static void init() {
		EOInitialiser.initialiseRelationship("Prognosis","course",false,"courseID","EduCourse");
		EOInitialiser.initialiseRelationship("Prognosis","student",false,"studentID","Student");
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
    	super.setValue(BigDecimal.ZERO);
    }
    
    public void setStudent(Student aValue) {
        takeStoredValueForKey(aValue, "student");
    }

    public EduCourse course() {
        return (EduCourse)storedValueForKey("course");
    }
	
    public void setCourse(EduCourse aValue) {
        takeStoredValueForKey(aValue, "course");
    }
    
    public void setValue(BigDecimal aValue) {
    	if(aValue != null && aValue.equals(value()))
    		return;
    	super.setValue(aValue);
    	//_bonus = null;
    	if(aValue == null)
    		return;
    	if(namedFlags().flagForKey("keep"))
    		return;
    	Bonus bonus = bonus();
    	if(bonus != null) {
    		if(namedFlags().flagForKey("keepBonus")) {
    			bonus.calculateValue(this,true);
     		} else if(bonus.value().compareTo(BigDecimal.ZERO) > 0) {
    			bonus.setValue(BigDecimal.ZERO);
    			logger.log(WOLogLevel.FINE,"Automatically dismissing bonus",bonus);
    		}
    	}
    	updateMarkFromValue();
    }
    
    public String markFromValue() {
    	if(value() == null)
    		return null;
    	BorderSet presenter = autoItog().borderSet();
    	if(presenter == null)
    		return null;
    	return presenter.presentFraction(value());
    }
    
    public void updateMarkFromValue() {
		super.setMark(markFromValue());
    }

    public void setAutoItog(AutoItog itog) {
    	super.setAutoItog(itog);
    	if(itog != null && fireDate() == null)
    		setFireDate(itog.fireDate());
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
						"Could not get syncMethod for StudentTimeout flags",e);
			}
    	}
    	return _flags;
    }
    
    public void setNamedFlags(NamedFlags flags) {
    	_flags = flags;
    	if(flags() != null && flags.intValue() != flags().intValue())
    		setFlags(flags.toInteger());
    }

    public boolean isComplete() {
    	BigDecimal complete = complete();
    	if(complete == null) return false;
    	if(complete.compareTo(BigDecimal.ZERO) == 0 && autoItog().noCalculator())
    		return true;
    	return (complete.compareTo(BigDecimal.ONE) == 0);
    }

	public void turnIntoFault(EOFaultHandler handler) {
		super.turnIntoFault(handler);
		//presenter = null;
		_timeout = null;
		_flags = null;
		//_bonus = null;
		//_calc = null;
		_relatedItog = null;
	}
	
	protected Object _relatedItog;
	public ItogMark relatedItog() {
		if(_relatedItog == null) {
			_relatedItog = ItogMark.getItogMark(course().cycle(),autoItog().itogContainer(),
					student(),editingContext());
			if(_relatedItog == null)
				_relatedItog = NullValue;
		}
		if(_relatedItog == NullValue) {
			return null;
		}
		return (ItogMark)_relatedItog;
	}
	
	protected transient Object _timeout;
	public StudentTimeout getStudentTimeout() {
		if(_timeout == null) {
			_timeout = StudentTimeout.timeoutForStudentCourseAndPeriod(
					student(), course(), autoItog().itogContainer());
			if(_timeout == null)
				_timeout = NullValue;
		}
		if(_timeout==NullValue)
			return null;
		else
			return (StudentTimeout)_timeout;
	}
	
	public NSTimestamp updateFireDate() {
		CourseTimeout courseTimeout = CourseTimeout.getTimeoutForCourseAndPeriod(
				course(), autoItog().itogContainer());
		return updateFireDate(courseTimeout);
	}

	public void setUpdateWithCourseTimeout(CourseTimeout courseTimeout) {
		if(courseTimeout == null)
			updateFireDate();
		else
			updateFireDate(courseTimeout);
	}

	public NSTimestamp updateFireDate(CourseTimeout courseTimeout) {
		_timeout = null;
		if(fireDate() == null && !valueChanged() && relatedItog() != null) {
			return null;
		}
		NSTimestamp result = null;
		StudentTimeout studentTimeout = getStudentTimeout();
		if(courseTimeout != null) {
			if(studentTimeout != null)
				result = chooseDate(studentTimeout, courseTimeout);
			else
				result = courseTimeout.fireDate();
		} else {
			if(studentTimeout != null)
				result = studentTimeout.fireDate();
			else
				result = autoItog().fireDate();
		}
		setFireDate(result);
		return result;
	}
	
	public static NSTimestamp chooseDate(StudentTimeout studentTimeout,
			CourseTimeout courseTimeout) {
		NSTimestamp stDate = studentTimeout.fireDate();
		NSTimestamp crDate = courseTimeout.fireDate();
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
		if(studentTimeout.course() != null)
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

	public static Prognosis getPrognosis(
			Student student, EduCourse course, AutoItog itog, boolean create) {
		EOEditingContext ec = course.editingContext();
		if(course == null || itog == null || student == null)
			throw new IllegalArgumentException("All arguments are required");
		EOQualifier qual = new EOKeyValueQualifier("course",
				EOQualifier.QualifierOperatorEqual, course);
		NSMutableArray quals = new NSMutableArray(qual);
		qual = new EOKeyValueQualifier(Prognosis.AUTO_ITOG_KEY,
				EOQualifier.QualifierOperatorEqual, itog);
		quals.addObject(qual);
		qual = new EOKeyValueQualifier("student",
				EOQualifier.QualifierOperatorEqual, student);
		quals.addObject(qual);
		qual = new EOAndQualifier(quals);
		EOFetchSpecification fs = new EOFetchSpecification(Prognosis.ENTITY_NAME,qual,null);
		NSArray found = ec.objectsWithFetchSpecification(fs);
		if(found == null || found.count() == 0) {
			if (!create)
				return null;
			Prognosis progn = (Prognosis)EOUtilities.createAndInsertInstance(ec,"Prognosis");
			progn.addObjectToBothSidesOfRelationshipWithKey(course, "course");
			progn.addObjectToBothSidesOfRelationshipWithKey(itog, Prognosis.AUTO_ITOG_KEY);
			progn.addObjectToBothSidesOfRelationshipWithKey(student, "student");
			return progn;
		} else {
			if(found.count() > 1) {
				Object args = new Object[] {found};
				if (ec instanceof SessionedEditingContext) {
					SessionedEditingContext s_ec = (SessionedEditingContext) ec;
					args = new Object[] {s_ec.session(),found};
				}
				logger.log(WOLogLevel.WARNING,"Multiple prognoses found",args);
			}
			return (Prognosis)found.objectAtIndex(0);
		}
	}

	@Deprecated
	public static PerPersonLink prognosesForCourseAndPeriod(
			EduCourse course, AutoItog period) {
		NSArray found = prognosesArrayForCourseAndPeriod(course, period);
		NSDictionary result = new NSDictionary(found,(NSArray)found.valueForKey("student"));
		return new PerPersonLink.Dictionary(result);
	}
	
	@Deprecated
	public static NSArray prognosesArrayForCourseAndPeriod(
								EduCourse course, AutoItog period) {
		NSArray args = new NSArray(new Object[] {period,course});
		NSArray found = EOUtilities.objectsWithQualifierFormat(
				course.editingContext(),"Prognosis","autoItog = %@ AND course = %@",args);
		return found;
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
	
	public ItogMark convertToItogMark(NSArray itogs, boolean overwrite) {
		return convertToItogMark(itogs, overwrite, null);
	}

	public ItogMark convertToItogMark(NSArray itogs, boolean overwrite, StringBuffer buf) {
		//ItogMark itog = null;
		ItogContainer container = autoItog().itogContainer();
		if(itogs != null) {
			_relatedItog = ItogMark.getItogMark(course().cycle(),container,
					student(),itogs);
		} else {
			_relatedItog = ItogMark.getItogMark(course().cycle(),container,
					student(),editingContext());
		}
		
		if(namedFlags().flagForKey("disable")) {
			report("This prognosis was disabled",this, buf);
			if(_relatedItog != null)
				setFireDate(null);
			return null;
		}

		setFireDate(null);
		if(_relatedItog == null) {
			_relatedItog = EOUtilities.createAndInsertInstance(editingContext(),"ItogMark");
			relatedItog().addObjectToBothSidesOfRelationshipWithKey(
					container,ItogMark.CONTAINER_KEY);
			relatedItog().addObjectToBothSidesOfRelationshipWithKey(student(),"student");
			relatedItog().addObjectToBothSidesOfRelationshipWithKey(course().cycle(),"cycle");
		} else {
			if(!overwrite) {
				report("Itog already exists",this, buf);
				BigDecimal value = relatedItog().value();
				boolean flag = (value != null && !value.equals(this.value()));
				relatedItog().readFlags().setFlagForKey(flag,"constituents");
				return null;
			}
			if(!this.mark().equals(relatedItog().mark()))
				relatedItog().readFlags().setFlagForKey(true,"changed");
		}
		relatedItog().setValue(value());
		relatedItog().setMark((mark() == null)?" ":mark());
		Bonus bonus = bonus();
		if(bonus != null) {
			if(bonus.value().compareTo(BigDecimal.ZERO) > 0) {
				relatedItog().setMark(bonus.mark());
				report("Bonus is applied",this, buf);
			} else {
				report("Bonus not applied",this, buf);
				/*
    			removeObjectFromBothSidesOfRelationshipWithKey(bonus, "bonus");
    			removeObjectFromBothSidesOfRelationshipWithKey(bonus.cycle(), "cycle");
    			editingContext().deleteObject(bonus);*/
			}
			buf.append(bonus.reason()).append(" : ").append(mark());
			buf.append(" -> ").append(bonus.mark()).append('\n');
		}
		String markFromValue = markFromValue();
		
		boolean flag = (markFromValue == null || !relatedItog().mark().equals(markFromValue));	
    	relatedItog().readFlags().setFlagForKey(flag,"forced");
		relatedItog().readFlags().setFlagForKey(false,"constituents");
		relatedItog().readFlags().setFlagForKey(!isComplete(),"incomplete");

		return relatedItog();
	}
	
	public static void convertPrognoses(EduCourse course, AutoItog period, Date scheduled) {
		convertPrognoses(course, period, scheduled, null);
	}
	
	
	public static void convertPrognoses(EduCourse course, AutoItog itog,
			 Date scheduled, StringBuffer buffer) {
		EOEditingContext ec = course.editingContext();
		NSArray args = new NSArray(new Object[] {itog,course,scheduled});
		StringBuffer buf = (buffer == null)?new StringBuffer():buffer;
		buf.append(course.eduGroup().name()).append(" : ");
		buf.append(course.cycle().subject()).append(" - ");
		buf.append(Person.Utility.fullName(course.teacher().person(), false, 2, 1, 0));
		buf.append('\n');
		
		NSArray prognoses = EOUtilities.objectsWithQualifierFormat(ec,
				"Prognosis","autoItog = %@ AND course = %@ AND fireDate <= %@",args);
		if(prognoses == null || prognoses.count() == 0) {
			buf.append("No prognoses found for course.\n");
			if(buffer == null) {
				logger.log(WOLogLevel.INFO, buf.toString(),course);
			}
			return;
		}
		NSMutableArray students = course.groupList().mutableClone();
		
		Enumeration penu = prognoses.objectEnumerator();
		NSArray itogs = ItogMark.getItogMarks(course.cycle(),itog.itogContainer(),null,ec);
		boolean overwrite = (scheduled == null || SettingsReader.boolForKeyPath(
				"edu.overwriteItogsScheduled", false));
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
					report("Prognosis is already fired", prognos, buf);
					continue cycleStudents;
				}
				if(scheduled != null && scheduled.compareTo(prognos.fireDate()) < 0) {
					report("Prognosis is timed out", prognos, buf);
					//continue cycleStudents;
				}
				if(idx < 0) {
					args = new NSArray(new Object[] {itog,prognos.student()});
					NSArray found = EOUtilities.objectsWithQualifierFormat(
							course.editingContext(),"Prognosis",
							"autoItog = %@ AND student = %@",args);
					EOQualifier qual = new EOKeyValueQualifier("course.cycle",
							EOQualifier.QualifierOperatorEqual,course.cycle());
					found = EOQualifier.filteredArrayWithQualifier(found, qual);
					if(found.count() > 1) {
						Enumeration enu = found.objectEnumerator();
						while (enu.hasMoreElements()) {
							Prognosis crp = (Prognosis) enu.nextElement();
							if(crp.course().groupList().contains(crp.student())) {
								report(
"Skipping prognosis for student not in group - found another prognosis", prognos, buf);
								if(prognos.complete().compareTo(BigDecimal.ZERO) == 0)
									ec.deleteObject(prognos);
								else
									prognos.setFireDate(null);
								continue cycleStudents;
							}
						}
					}
					report("Setting mark to student not in group", prognos, buf);
				}
				ItogMark itogMark = prognos.convertToItogMark(itogs,overwrite, buf);
				if(itogMark != null && (overwrite || !itogs.containsObject(itogMark))) {
					itogMark.readFlags().setFlagForKey(scheduled == null,"manual");
					if(enableArchive) {
						EOEnterpriseObject archive = EOUtilities.
										createAndInsertInstance(ec,"MarkArchive");
						archive.takeValueForKey(itogMark, "object");
						if(scheduled != null) {
							logger.log(WOLogLevel.INFO,"AutoItog", new Object[] {
								itogMark,Thread.currentThread(),new Exception("AutoItog")});
							archive.takeValueForKey("scheduled", "wosid");
							archive.takeValueForKey("AutoItog", "user");
						} else if(!(ec instanceof SessionedEditingContext)) {
							archive.takeValueForKey("???", "wosid");
							archive.takeValueForKey("???","user");
						}
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
				logger.log(WOLogLevel.FINE,
						"Saved itogs based on prognoses for course",course);
			}  catch (Exception ex) {
				logger.log(WOLogLevel.WARNING,
						"Failed to save itogs based on prognoses for course",
						new Object[] {course,ex});
				buf.append("Failed to save itogs based on prognoses for course");
				ec.revert();
			}
			EOEnterpriseObject grouping = ModuleInit.getStatsGrouping(
					course, itog.itogContainer());
			if(grouping != null) {
				itogs = ItogMark.getItogMarks(course.cycle(), itog.itogContainer(), null, ec);
				itogs = MyUtility.filterByGroup(itogs, "student", course.groupList(), true);
				grouping.takeValueForKey(itogs, "array");
//				NSDictionary stats = ModuleInit.statCourse(course, period);
//				grouping.takeValueForKey(stats, "dict");
				try {
					ec.saveChanges();
				} catch (Exception e) {
					logger.log(WOLogLevel.WARNING,"Failed to save itog Stats for course",
							new Object[] {course,e});
					ec.revert();
				}
			}
		} else { //ec.hasChanges()
			logger.log(WOLogLevel.FINE,"No itogs to save for course",course);
		}
		if(buffer == null) {		
			logger.log(WOLogLevel.INFO, buf.toString(),course);
		}
	}
	
	public boolean valueChanged() {
		NSDictionary committed = editingContext().committedSnapshotForObject(this);
		if(committed == null || committed.count() == 0)
			return true;
		NSDictionary recent = snapshot();
		if(committed == recent)
			return false;
		String[] keys = new String[] {COMPLETE_KEY,VALUE_KEY,MARK_KEY};
		for (int i = 0; i < keys.length; i++) {
			Object a = committed.valueForKey(keys[i]);
			Object b = recent.valueForKey(keys[i]);
			if((a == null)? b != null : !a.equals(b))
				return true;
		}
		return false;
	}
	
	public static NSArray localisedFlagKeys() {
		NSDictionary localisation = (NSDictionary)WOApplication.application().valueForKeyPath(
				"strings.RujelAutoItog_AutoItog.properties.Prognosis.flags");
		if(localisation == null || localisation.count() == 0)
			return flagNames;
		NSMutableArray result = new NSMutableArray();
		Enumeration enu = flagNames.objectEnumerator();
		while (enu.hasMoreElements()) {
			String key = (String) enu.nextElement();
			String lKey = (String)localisation.valueForKey(key);
			if(lKey == null)
				lKey = key;
			result.addObject(lKey);
		}
		return result;
	}

}
