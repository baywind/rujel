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
import net.rujel.base.SettingsBase;
import net.rujel.criterial.FractionPresenter;

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
                   {"-1-","keep","keepBonus","-8-","-16-","-32-","disable"});
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
    	setState(new Integer(0));
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
//    	if(aValue != null && aValue.equals(value()))
//    		return;
    	super.setValue(aValue);
    	//_bonus = null;
    	if(aValue == null)
    		return;
     	Bonus bonus = bonus();
    	if(bonus != null) {
    		boolean keep = namedFlags().flagForKey("keepBonus");
    		BigDecimal value = bonus.calculateValue(this,keep);
    		if(value.compareTo(BigDecimal.ZERO) < 0) {
    			removeObjectFromBothSidesOfRelationshipWithKey(bonus, BONUS_KEY);
    			editingContext().deleteObject(bonus);
    			logger.log(WOLogLevel.FINE,"Bonus was overriden. Removing.",this);
    		} else if(!keep) {
    			BigDecimal curValue = bonus.value();
    			if(curValue.compareTo(BigDecimal.ZERO) > 0) {
    				MathContext mc = new MathContext(4);
    				curValue = curValue.round(mc);
    				value = value.round(mc);
    				if(!value.equals(curValue)) {
    					bonus.setValue(BigDecimal.ZERO);
    					logger.log(WOLogLevel.FINE,"Automatically dismissing bonus",bonus);
    				}
    			}
    		}
    	}
    	if(!namedFlags().flagForKey("keep"))
    		updateMarkFromValue();
    }
    
    public String markFromValue() {
    	if(value() == null)
    		return null;
    	FractionPresenter presenter = autoItog().borderSet();
    	if(presenter == null)
    		presenter = FractionPresenter.PERCENTAGE;
    	return presenter.presentFraction(value());
    }
    
    public void updateMarkFromValue() {
    	String mark = markFromValue();
		super.setMark(mark);
		if(mark == null)
			return;
		NSArray presets = autoItog().itogPresets();
		if(presets != null && presets.count() > 0) {
			ItogPreset preset = (ItogPreset)presets.objectAtIndex(0);
			if(preset.mark().charAt(0) == '%') {
				preset = ItogPreset.presetForValue(value(), presets);
				if(preset == null)
					setState(Integer.valueOf(1));
			} else {
				preset = ItogPreset.presetForMark(mark, presets);
			}
		}
		ItogPreset preset = ItogPreset.presetForMark(mark, 
				autoItog().presetGroup(), editingContext());
		if(preset != null)
			setState(preset.state());
    }

    protected Object _autoItog;
    public AutoItog autoItog() {
    	if(_autoItog == null) {
        	String listName = SettingsBase.stringSettingForCourse(ItogMark.ENTITY_NAME,
        			course(), editingContext());
        	_autoItog = AutoItog.forListName(listName, itogContainer());
        	if(_autoItog == null)
        		_autoItog = NullValue;
    	}
    	return (_autoItog == NullValue)?null:(AutoItog)_autoItog;
    }
    
    public void setAutoItog(AutoItog itog) {
    	if(_autoItog != itog && itog != null) {
    		if(itogContainer() != itog.itogContainer())
    			setItogContainer(itog.itogContainer());
    		if(editingContext().globalIDForObject(this).isTemporary())
    			setFireDate(itog.fireDate());
    	}
    	_autoItog = itog;
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
		_autoItog = null;
		_relatedItog = null;
	}
	
	protected Object _relatedItog;
	public ItogMark relatedItog() {
		return relatedItog(null);
	}
	public ItogMark relatedItog(NSArray itogs) {
		if(_relatedItog == null) {
			if(itogs == null) {
				_relatedItog = ItogMark.getItogMark(course().cycle(),itogContainer(),
						student(),editingContext());
			} else {
				_relatedItog = ItogMark.getItogMark(course().cycle(),itogContainer(),
						student(),itogs);
			}
			if(_relatedItog == null) {
				_relatedItog = NullValue;
				return null;
			}
		} else if(_relatedItog == NullValue) {
			return null;
		} else if(((ItogMark)_relatedItog).editingContext() == null) {
			_relatedItog = null;
			return null;
		}
		return (ItogMark)_relatedItog;
	}
	
	public boolean itogDiffers() {
		ItogMark itog = relatedItog();
		if(itog == null)
			return false;
		String mark = mark();
		Bonus bonus = bonus();
		if(bonus != null && bonus.value().compareTo(BigDecimal.ZERO) > 0) {
			mark = bonus.mark();
		}
		if(mark == null)
			return false;
		return !mark.equals(itog.mark());
	}
	
	protected transient Object _timeout;
	public StudentTimeout getStudentTimeout() {
		if(_timeout == null) {
			_timeout = StudentTimeout.timeoutForStudentAndCourse(
					student(), course(), itogContainer());
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
				course(), itogContainer());
		return updateFireDate(courseTimeout);
	}

	public void setUpdateWithCourseTimeout(Object courseTimeout) {
		if(courseTimeout == NullValue)
			updateFireDate();
		else
			updateFireDate((CourseTimeout)courseTimeout);
	}

	public NSTimestamp updateFireDate(CourseTimeout courseTimeout) {
		_timeout = null;
		if(fireDate() == null && !valueChanged() && relatedItog() != null) {
			return null;
		}
		StudentTimeout studentTimeout = getStudentTimeout();
		NSTimestamp result = Timeout.Utility.chooseDate(studentTimeout, courseTimeout);
		if(result == null) {
			result = autoItog().fireDate();
		}
		setFireDate(result);
		return result;
	}

	
/*	public void setLaterFireDate(NSTimestamp newDate) {
		if(fireDate() != null && newDate.compare(fireDate()) > 0)
			setFireDate(newDate);
	}
*/
	public static Prognosis getPrognosis(
			Student student, EduCourse course, ItogContainer itog, boolean create) {
		EOEditingContext ec = course.editingContext();
		if(course == null || itog == null || student == null)
			throw new IllegalArgumentException("All arguments are required");
		EOQualifier qual = new EOKeyValueQualifier("course",
				EOQualifier.QualifierOperatorEqual, course);
		NSMutableArray quals = new NSMutableArray(qual);
		qual = new EOKeyValueQualifier(Prognosis.ITOG_CONTAINER_KEY,
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
			progn.addObjectToBothSidesOfRelationshipWithKey(itog, 
					Prognosis.ITOG_CONTAINER_KEY);
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
		NSArray found = prognosesArrayForCourseAndPeriod(course, period.itogContainer(), true);
		NSDictionary result = new NSDictionary(found,(NSArray)found.valueForKey("student"));
		return new PerPersonLink.Dictionary(result);
	}
	
	public static NSArray prognosesArrayForCourseAndPeriod(
							EduCourse course, AutoItog autoItog) {
		NSArray args = new NSArray(new Object[] {autoItog.itogContainer(),course});
		NSArray found = EOUtilities.objectsWithQualifierFormat(course.editingContext(),
				"Prognosis","itogContainer = %@ AND course = %@",args);
		if(found.count() > 0)
			found.takeValueForKey(autoItog, "autoItog");
		return found;
	}
	
	public static NSArray prognosesArrayForCourseAndPeriod(
								EduCourse course, ItogContainer period, boolean init) {
		NSArray args = new NSArray(new Object[] {period,course});
		NSArray found = EOUtilities.objectsWithQualifierFormat(course.editingContext(),
				"Prognosis","itogContainer = %@ AND course = %@",args);
		if(init && found.count() > 0) {
        	String listName = SettingsBase.stringSettingForCourse(ItogMark.ENTITY_NAME,
        			course, course.editingContext());
        	AutoItog ai = AutoItog.forListName(listName, period);
        	if(ai != null)
        		found.takeValueForKey(ai, "autoItog");
		}
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
			buf.append(Person.Utility.fullName(student, false, 2, 1, 0));
			buf.append(" : ").append(report).append('\n');
		}
	}
	
	public ItogMark convertToItogMark(NSArray itogs, boolean overwrite, StringBuffer buf) {
		//ItogMark itog = null;
		ItogContainer container = itogContainer();
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
		boolean unchanged = false;
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
				return relatedItog();
			}
			if(!this.mark().equals(relatedItog().mark()))
				relatedItog().readFlags().setFlagForKey(true,"changed");
			else
				unchanged = true;
		}
		relatedItog().setValue(value());
		relatedItog().setState(state());
		if(!unchanged)
			relatedItog().setMark((mark() == null)?" ":mark());
		Bonus bonus = bonus();
		String bonusTitle = (String)WOApplication.application().valueForKeyPath(
				"strings.RujelAutoItog_AutoItog.ui.markHasBonus");
		if(bonus != null) {
			if(bonus.value().compareTo(BigDecimal.ZERO) > 0) {
				relatedItog().setMark(bonus.mark());
				NSArray presets = autoItog().itogPresets();
				if(presets != null && presets.count() > 0) {
					ItogPreset preset = (ItogPreset)presets.objectAtIndex(0);
					if(preset.mark().charAt(0) == '%') {
						BigDecimal value = value().add(bonus.value());
						preset = ItogPreset.presetForValue(value, presets);
						if(preset == null)
							relatedItog().setState(Integer.valueOf(1));
					} else {
						preset = ItogPreset.presetForMark(bonus.mark(), presets);
					}
					if(preset != null)
						relatedItog().setState(preset.state());
				}
				relatedItog().comments().takeValueForKey(bonus.reason(), bonusTitle);
				report("Bonus is applied",this, buf);
			} else {
				report("Bonus NOT applied",this, buf);
				relatedItog().comments().takeValueForKey(null, bonusTitle);
			}
			if(buf != null) {
				buf.append(bonus.reason()).append(" : ").append(mark());
				buf.append(" -> ").append(bonus.mark()).append('\n');
			}
		} else {
			relatedItog().comments().takeValueForKey(null, bonusTitle);
		}
		String markFromValue = markFromValue();
		if(unchanged && buf != null)
			buf.append((char)0);
		
		boolean flag = (markFromValue == null || !relatedItog().mark().equals(markFromValue));	
    	relatedItog().readFlags().setFlagForKey(flag,"forced");
		relatedItog().readFlags().setFlagForKey(false,"constituents");
		relatedItog().readFlags().setFlagForKey(!isComplete(),"incomplete");
		return relatedItog();
	}
	
	public static void convertPrognoses(EduCourse course, ItogContainer itog, Date scheduled) {
		convertPrognoses(course, itog, scheduled, null);
	}
	
	
	public static void convertPrognoses(EduCourse course, ItogContainer itog,
			 Date scheduled, StringBuffer buffer) {
		EOEditingContext ec = course.editingContext();
//		NSArray args = new NSArray(new Object[] {itog,course,scheduled});
		StringBuffer buf = (buffer == null)?new StringBuffer():buffer;
		buf.append(course.eduGroup().name()).append(" : ");
		buf.append(course.cycle().subject()).append(" - ");
		buf.append(Person.Utility.fullName(course.teacher(), false, 2, 1, 0));
		buf.append('\n');
		
		NSArray prognoses = prognosesArrayForCourseAndPeriod(course, itog, true);
//			EOUtilities.objectsWithQualifierFormat(ec,Prognosis.ENTITY_NAME,
//				"autoItog = %@ AND course = %@",args);
		if(prognoses == null || prognoses.count() == 0) {
			buf.append("No prognoses found for course.\n");
			if(buffer == null) {
				logger.log(WOLogLevel.INFO, buf.toString(),course);
			}
			return;
		}
		CourseTimeout cto = CourseTimeout.getTimeoutForCourseAndPeriod(course, itog);
		NSMutableArray students = course.groupList().mutableClone();
		
		Enumeration penu = prognoses.objectEnumerator();
		NSArray itogs = ItogMark.getItogMarks(course.cycle(),itog,null,ec);
		boolean overwrite = (scheduled == null || SettingsReader.boolForKeyPath(
				"edu.overwriteItogsScheduled", false));
		boolean enableArchive = SettingsReader.boolForKeyPath("markarchive.ItogMark", 
				SettingsReader.boolForKeyPath("markarchive.archiveAll", false));
cycleStudents:
			while (penu.hasMoreElements()) {
				Prognosis prognos = (Prognosis)penu.nextElement();
				int idx = students.indexOf(prognos.student());
				if(idx >= 0) {
					students.removeObjectAtIndex(idx);
				}
				if(prognos.fireDate() == null) {
//					report("Prognosis is already fired", prognos, buf);
					continue cycleStudents;
				}
				// check timeout
	    		Timeout timeout = Timeout.Utility.chooseTimeout(
	    				prognos.getStudentTimeout(), cto);
				if(scheduled != null && scheduled.compareTo(prognos.fireDate()) < 0) {
					EOEnterpriseObject commentEO = ItogMark.getItogComment(course.cycle(),
							itog, prognos.student(), (timeout != null));
					if(commentEO != null)
						Timeout.Utility.setTimeoutComment(commentEO, timeout);
					// add ItogComment about timeout
					report("Prognosis is timed out", prognos, buf);
					continue cycleStudents;
				}
				if(idx < 0) {
					NSArray args = new NSArray(new Object[] {itog,prognos.student()});
					NSArray found = EOUtilities.objectsWithQualifierFormat(
							course.editingContext(),ENTITY_NAME,
							"itogContainer = %@ AND student = %@",args);
					EOQualifier qual = new EOKeyValueQualifier("course.cycle",
							EOQualifier.QualifierOperatorEqual,course.cycle());
					found = EOQualifier.filteredArrayWithQualifier(found, qual);
					if(found.count() > 1) {
						Enumeration enu = found.objectEnumerator();
						while (enu.hasMoreElements()) {
							Prognosis crp = (Prognosis) enu.nextElement();
							if(crp == prognos)
								continue;
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
				if(itogMark != null) {
					EOEnterpriseObject commentEO = itogMark.commentEO(timeout != null);
					if(commentEO != null)
						Timeout.Utility.setTimeoutComment(commentEO, timeout);
				}
				if(buf.charAt(buf.length() -1) != 0 &&
						itogMark != null && (overwrite || !itogs.containsObject(itogMark))) {
					itogMark.readFlags().setFlagForKey(scheduled == null,"manual");
					if(enableArchive) {
						EOEnterpriseObject archive = EOUtilities.
										createAndInsertInstance(ec,"MarkArchive");
						archive.takeValueForKey(itogMark, "object");
						if(scheduled != null) {
//							logger.log(WOLogLevel.INFO,"AutoItog", new Object[] {
//								itogMark,Thread.currentThread(),new Exception("AutoItog")});
							archive.takeValueForKey("scheduled", "wosid");
							archive.takeValueForKey("AutoItog", "user");
						} else if(!(ec instanceof SessionedEditingContext)) {
							archive.takeValueForKey("???", "wosid");
							archive.takeValueForKey("???","user");
						}
						int actionType = 1;
						if(overwrite && itogs.containsObject(itogMark))
							actionType = 2;
						archive.takeValueForKey(new Integer(actionType), "actionType");
					}
				}
				if(buf.charAt(buf.length() -1) == 0)
					buf.deleteCharAt(buf.length() -1);
			} // cycleStudents
		if(students.count() > 0) {
			Enumeration enu = students.objectEnumerator();
			buf.append("No prognoses found for students:\n");
			while (enu.hasMoreElements()) {
				Student stu = (Student) enu.nextElement();
				buf.append(Person.Utility.fullName(stu, true, 2, 1, 0)).append(", ");
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
			EOEnterpriseObject grouping = ModuleInit.getStatsGrouping(course, itog);
			if(grouping != null) {
				itogs = ItogMark.getItogMarks(course.cycle(), itog, null, ec);
				itogs = MyUtility.filterByGroup(itogs, "student", course.groupList(), true);
				grouping.takeValueForKey(itogs, "array");
//				NSDictionary stats = ModuleInit.statCourse(course, itog.itogContainer());
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
