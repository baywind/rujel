//  Reason.java

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

package net.rujel.curriculum;

import java.util.Calendar;
import java.util.Enumeration;
import java.util.logging.Logger;

import com.webobjects.foundation.*;
import com.webobjects.appserver.WOApplication;
import com.webobjects.eoaccess.EOUtilities;
import com.webobjects.eocontrol.*;

import net.rujel.base.MyUtility;
import net.rujel.base.SchoolSection;
import net.rujel.eduplan.Holiday;
import net.rujel.eduplan.PlanCycle;
import net.rujel.interfaces.*;
import net.rujel.reusables.*;

public class Reason extends _Reason {

	public static void init() {
		EOInitialiser.initialiseRelationship(ENTITY_NAME,"eduGroup",
				false,"eduGroupID","EduGroup");
		EOInitialiser.initialiseRelationship(ENTITY_NAME,"teacher",
				false,"teacherID","Teacher");
	}

	public void awakeFromInsertion(EOEditingContext ec) {
		super.awakeFromInsertion(ec);
		setFlags(new Integer(0));
	}

	public EduGroup eduGroup() {
		if(namedFlags().flagForKey("forEduGroup"))
			return (EduGroup)storedValueForKey("eduGroup");
		else
			return null;
	}
	
	public void setEduGroup(EduGroup gr) {
		namedFlags().setFlagForKey((gr != null), "forEduGroup");
		takeStoredValueForKey(gr, "eduGroup");
	}
	
	public Teacher teacher() {
		if(namedFlags().flagForKey("forTeacher"))
			return (Teacher)storedValueForKey("teacher");
		else
			return null;
	}
	
	public void setTeacher(Object newTeacher) {
		namedFlags().setFlagForKey((newTeacher != null), "forTeacher");
		takeStoredValueForKey((newTeacher==NullValue)?null:newTeacher, "teacher");
	}
	
	public String title() {
		if(teacher() == null && eduGroup() == null)
			return reason();
		StringBuilder result = new StringBuilder(reason());
		result.append(' ').append('(');
		exts(result);
		result.append(')');
		return result.toString();
	}
	
	public static NSArray flagNames = new NSArray(new String[] {
			"external","forSkip","forAdd","toReturn","forEduGroup","forTeacher"});

	private NamedFlags _flags;
    public NamedFlags namedFlags() {
    	if(_flags==null) {
    		_flags = new NamedFlags(flags().intValue(),flagNames);
    		try{
    			_flags.setSyncParams(this, getClass().getMethod("setNamedFlags",
    					NamedFlags.class));
    		} catch (Exception e) {
    			Logger.getLogger("rujel.curriculum").log(WOLogLevel.WARNING,
						"Could not get syncMethod for Reason flags",e);
			}
    	}
    	return _flags;
    }
    
    public void setNamedFlags(NamedFlags flags) {
    	if(flags != null)
    		setFlags(flags.toInteger());
    	_flags = flags;
    }
    
    public void setFlags(Integer value) {
    	_flags = null;
    	super.setFlags(value);
    }

    protected void exts(StringBuilder result) {
    	boolean added = false;
    	if(namedFlags().flagForKey("forTeacher")) {
    		if(teacher() != null) {
    			result.append(Person.Utility.fullName(teacher(), true, 2, 1, 1));
    		} else {
    			result.append(WOApplication.application().valueForKeyPath(
    			"strings.RujelBase_Base.vacant"));
    		}
    		added = true;
    	}
    	if(eduGroup() != null) {
    		if(added)
				result.append(',').append(' ');
			result.append(eduGroup().name());
			added = true;
		} else if(grade() != null) {
    		if(added)
				result.append(',').append(' ');
			result.append(grade());
			added = true;
		}
    	if(section() != null) {
    		if(added) {
    			if(editingContext().userInfoForKey("activeSection") == section()) {
    				result.append('*');
    				return;
    			}
				result.append(',').append(' ');
    		}
    		result.append(section().name());
    	}
	}
/*
	public static Object sectionForNum(Integer num, EOEditingContext ec) {
		Object section = num;
		if(ec instanceof SessionedEditingContext) {
			NSArray sections = (NSArray)((SessionedEditingContext)ec
					).session().valueForKeyPath("strings.sections.list");
			Enumeration enu = sections.objectEnumerator();
			while (enu.hasMoreElements()) {
				NSDictionary sect = (NSDictionary) enu.nextElement();
				if(num.equals(sect.valueForKey(IndexRow.IDX_KEY))) {
					return sect.valueForKey(IndexRow.VALUE_KEY);
				}
			}
		} else {
			Indexer sIndex = Indexer.getIndexer(ec,"eduSections",(String)null, false);
			if(sIndex != null) {
//    				sIndex.valueForIndex((Integer)section,null);
				try {
					IndexRow idx = (IndexRow)EOUtilities.objectMatchingValues(
							ec,IndexRow.ENTITY_NAME, new NSDictionary(new Object[] { sIndex,num },
									new String[] {IndexRow.INDEXER_KEY,IndexRow.IDX_KEY }));
					section = idx.valueForKey(IndexRow.VALUE_KEY);
				} catch (Exception e) {
				}
			}
		}
		return section;
	} */
	
	public boolean noLimits() {
		return (!namedFlags().flagForKey("forTeacher") && !namedFlags().flagForKey("forEduGroup")
				&& grade() == null && section() == null);
	}
	
	public String extToString() {
		if(noLimits())
			return null;
		StringBuilder sb = new StringBuilder(12);
		exts(sb);
		return sb.toString();
	}
	
	public boolean unverified() {
		return (verification() == null || verification().length() == 0);
	}
	
    public String styleClass() {
    	if(flags().intValue() == 1)
    		return "grey";
    	if(unverified())
    		return "ungerade";
    	return "gerade";
    }
	
	public static NSArray reasons (NSTimestamp date, EduCourse course) {
		Props props = new Props(course,date);
		props.begin = date;
		props.end = date;
		return reasons(props);
	}
	
	public static Props propsFromEvents(NSArray events) {
		if(events == null || events.count() == 0)
			return null;
		Props props = null;
		Enumeration enu = events.objectEnumerator();
		while (enu.hasMoreElements()) {
			Event event = (Event) enu.nextElement();
			EduCourse course = event.course();
			boolean others = (event instanceof Variation && 
					((Variation)event).value().intValue() > 0);
			if(others) {
				Variation paired = ((Variation)event).getPaired();
				if(paired != null) {
					course = paired.course();
					others = false;
				}
			}
			NSTimestamp date = event.date();
			if(props == null) {
				props = new Props(course,date);
				props.otherTeachers = others;
			} else {
				if(props.ec != event.editingContext())
					throw new IllegalArgumentException(
							"Given events belong to different editing contexts");
				if(props.eduGroup != null && props.eduGroup != course.eduGroup())
					props.eduGroup = null;
				if(!others) {
					if(props.teacher != null && course.teacher() != props.teacher()) {
						if(props.otherTeachers)
							props.teacher = course.teacher();
						else
							props.teacher = null;
					}
					props.otherTeachers = false;
				}
				PlanCycle cycle = (PlanCycle)course.cycle();
				if(props.grade != null && !props.grade.equals(cycle.grade()))
					props.grade = null;
				if(props.section != null && !props.section.equals(cycle.section()))
					props.section = null;
			}
			if(props.begin == null || props.begin.compare(date) > 0)
				props.begin = date;
			if(props.end == null || props.end.compare(date) < 0)
				props.end = date;
		} // cycle events
		return props;
	}
	
	public static NSArray reasons (Props props) {
		EOQualifier qual = null;
		NSMutableArray quals = new NSMutableArray();
		if(props.otherTeachers) {
			qual = new EOKeyValueQualifier(FLAGS_KEY,
					EOQualifier.QualifierOperatorGreaterThanOrEqualTo, new Integer(32));
			quals.addObject(qual);
		} else if (props.teacher != null) {
			qual = new EOKeyValueQualifier("teacher",
					EOQualifier.QualifierOperatorEqual, props.teacher);
			if (props.teacher == NullValue) {
				quals.addObject(qual);
				qual = new EOKeyValueQualifier(FLAGS_KEY,
						EOQualifier.QualifierOperatorGreaterThanOrEqualTo, new Integer(32));
				quals.addObject(qual);
				qual = new EOAndQualifier(quals);
				quals.removeAllObjects();
			}
			quals.addObject(qual);
		} // teacher is set
		if (props.eduGroup != null) {
			qual = new EOKeyValueQualifier("eduGroup",
					EOQualifier.QualifierOperatorEqual, props.eduGroup);
			quals.addObject(qual);
		} // group is set
		qual = new EOKeyValueQualifier(FLAGS_KEY,
				EOQualifier.QualifierOperatorLessThan,new Integer(16));
		
		if (quals.count() > 0) {
			quals.addObject(qual);
			qual = new EOOrQualifier(quals);
			quals.removeAllObjects();
		}
		quals.addObject(qual);
		
		qual = new EOKeyValueQualifier(BEGIN_KEY,
				EOQualifier.QualifierOperatorLessThanOrEqualTo,props.begin);
		quals.addObject(qual);
		
		EOQualifier[] ors = new EOQualifier[2];
		ors[0] = new EOKeyValueQualifier(END_KEY,
				EOQualifier.QualifierOperatorGreaterThanOrEqualTo,props.end);
		ors[1] = new EOKeyValueQualifier(END_KEY,EOQualifier.QualifierOperatorEqual,NullValue);
		qual = new EOOrQualifier(new NSArray(ors));
		quals.addObject(qual);

		if (props.grade == null) {
			qual = new EOKeyValueQualifier("grade",
					EOQualifier.QualifierOperatorEqual, NullValue);
		} else {
			ors[0] = new EOKeyValueQualifier("grade",
					EOQualifier.QualifierOperatorEqual, props.grade);
			ors[1] = new EOKeyValueQualifier("grade",
					EOQualifier.QualifierOperatorEqual, NullValue);
			qual = new EOOrQualifier(new NSArray(ors));
		} // grade
		quals.addObject(qual);

		if (props.section == null) {
			qual = new EOKeyValueQualifier("section",
					EOQualifier.QualifierOperatorEqual, NullValue);
		} else {
			ors[0] = new EOKeyValueQualifier("section",
					EOQualifier.QualifierOperatorEqual, props.section);
			ors[1] = new EOKeyValueQualifier("section",
					EOQualifier.QualifierOperatorEqual, NullValue);
			qual = new EOOrQualifier(new NSArray(ors));
		} // section
		quals.addObject(qual);
				
		qual = new EOAndQualifier(quals);
		EOFetchSpecification fs = new EOFetchSpecification(
				ENTITY_NAME,qual,EOPeriod.sorter);
		NSArray found = props.ec.objectsWithFetchSpecification(fs);
		if(found != null && found.count() > 0) {
			Enumeration enu = found.objectEnumerator();
			NSMutableArray result = new NSMutableArray();
			while (enu.hasMoreElements()) {
				Reason r = (Reason) enu.nextElement();
				if(r.namedFlags().flagForKey("forEduGroup") 
						&& r.eduGroup() != props.eduGroup)
					continue;
				else if(!props.otherTeachers && r.namedFlags().flagForKey("forTeacher")
						&& r.teacher() != props.teacher())
					continue;
				else
					result.addObject(r);
			} // results verification
			return result;
		}
		return found;
	}
	
	public static Reason reasonForHoliday(Holiday holiday, boolean create) {
		if(holiday == null)
			return null;
		EOEditingContext ec = holiday.editingContext();
//		EOGlobalID gid = ec.globalIDForObject(holiday);
		String key = WOLogFormatter.formatEO(holiday);
		NSDictionary values = new NSDictionary( new Object[] {key, new Integer(1)},
				new String[] {VERIFICATION_KEY,FLAGS_KEY});
		NSArray found = EOUtilities.objectsMatchingValues(ec, ENTITY_NAME, values);
		if(found != null) {
			for (int i = 0; i < found.count(); i++) {
				Reason reason = (Reason)found.objectAtIndex(i);
				if(EOPeriod.Utility.compareDates(reason.end(), holiday.begin()) >= 0 &&
						EOPeriod.Utility.compareDates(reason.begin(), holiday.end()) <= 0) {
					if(EOPeriod.Utility.compareDates(reason.begin(), holiday.begin()) > 0)
						reason.setBegin(holiday.begin());
					if(EOPeriod.Utility.compareDates(reason.end(), holiday.end()) < 0)
						reason.setEnd(holiday.end());
					return reason;
				}
			}
		}
		if(!create)
			return null;
		Reason result = (Reason)EOUtilities.createAndInsertInstance(ec, ENTITY_NAME);
		result.takeValuesFromDictionary(values);
		result.setBegin(holiday.begin());
		result.setEnd(holiday.end());
		if(holiday.listName() == null) {
			result.setReason(holiday.name());
		} else {
			StringBuilder buf = new StringBuilder(holiday.name());
			buf.append(' ').append('(').append(holiday.listName()).append(')');
			result.setReason(buf.toString());
		}
		return result;
	}

	public void validateForSave() {
		super.validateForSave();
		StringBuilder buf = new StringBuilder();
		buf.append(reason()).append(':').append(' ');
		long end = Long.MAX_VALUE;
		if(end() != null) {
			Calendar cal = Calendar.getInstance();
			cal.setTime(end());
			cal.set(Calendar.HOUR_OF_DAY, 23);
			cal.set(Calendar.MINUTE, 59);
			cal.set(Calendar.SECOND, 59);
			cal.set(Calendar.MILLISECOND, 999);
			end = cal.getTimeInMillis();
		}
		if(end() != null && begin().getTime() > end) {
			buf.append(WOApplication.application().valueForKeyPath(
					"strings.RujelCurriculum_Curriculum.messages.invalidPeriod"));
			throw new NSValidation.ValidationException(buf.toString());
		}
		boolean checkTeacher = namedFlags().flagForKey("forTeacher");
		boolean checkGroup = namedFlags().flagForKey("forEduGroup");
		EduGroup eduGroup = eduGroup();
		Teacher teacher = teacher();
		NSArray list = substitutes();
		if(list != null && list.count() > 0) {
			Enumeration enu =  list.objectEnumerator();
			while (enu.hasMoreElements()) {
				Substitute sub = (Substitute) enu.nextElement();
				if(sub.editingContext() == null)
					continue;
				EduCourse course = (EduCourse)sub.valueForKeyPath("lesson.course");
				if(course == null)
					continue;
				if(checkTeacher && (teacher != course.teacher(sub.date()))) {
					buf.append(WOApplication.application().valueForKeyPath(
						"strings.RujelCurriculum_Curriculum.messages.cantSetTeacher"));
					buf.append(" (").append(Person.Utility.fullName(course.teacher(sub.date())
							, true, 2, 1, 1)).append(')');
					throw new NSValidation.ValidationException(buf.toString(),sub,"teacher");
				}
				if(checkGroup && (eduGroup != course.eduGroup())) {
					buf.append(WOApplication.application().valueForKeyPath(
						"strings.RujelCurriculum_Curriculum.messages.cantSetEduGroup"));
					buf.append(" (").append(course.eduGroup().name()).append(')');
					throw new NSValidation.ValidationException(buf.toString(), sub,"eduGroup");
				}
				if(grade() != null && !grade().equals(course.cycle().grade())) {
					buf.append(WOApplication.application().valueForKeyPath(
						"strings.RujelCurriculum_Curriculum.messages.cantSetGrade"));
					buf.append(" (").append(course.cycle().grade()).append(')');
					throw new NSValidation.ValidationException(buf.toString(), sub,"eduGroup");
				}
				if(section() != null && section() != ((PlanCycle)course.cycle()).section()) {
					buf.append(WOApplication.application().valueForKeyPath(
						"strings.RujelCurriculum_Curriculum.messages.cantSetSection"));
					buf.append(" (").append(
							((PlanCycle)course.cycle()).section().name()).append(')');
					throw new NSValidation.ValidationException(buf.toString(), sub,"eduGroup");
				}
				if(EOPeriod.Utility.compareDates(begin(),sub.date()) > 0 || 
						(sub.date().getTime() > end)) {
					buf.append(WOApplication.application().valueForKeyPath(
						"strings.RujelCurriculum_Curriculum.messages.cantSetDates"));
					buf.append(" (").append(
							MyUtility.dateFormat().format(sub.date())).append(')');
					if(EOPeriod.Utility.compareDates(begin(),sub.date()) > 0)
						throw new NSValidation.ValidationException(buf.toString(),sub,BEGIN_KEY);
					else
						throw new NSValidation.ValidationException(buf.toString(),sub,END_KEY);
				}
			} // enu substitutes
		}
		list = variations();
		if(list != null && list.count() > 0) {
			Enumeration enu =  list.objectEnumerator();
			while (enu.hasMoreElements()) {
				Variation var = (Variation) enu.nextElement();
				EduCourse course = var.course();
				if(checkTeacher && (teacher != course.teacher(var.date()))
						&& var.value().intValue() <= 0) {
					buf.append(WOApplication.application().valueForKeyPath(
							"strings.RujelCurriculum_Curriculum.messages.cantSetTeacher"));
					buf.append(" (").append(Person.Utility.fullName(course.teacher(var.date())
							, true, 2, 1, 1)).append(')');
					throw new NSValidation.ValidationException(buf.toString(),var,"teacher");
				}
				if(checkGroup && (eduGroup != course.eduGroup())) {
					buf.append(WOApplication.application().valueForKeyPath(
						"strings.RujelCurriculum_Curriculum.messages.cantSetEduGroup"));
					buf.append(" (").append(course.eduGroup().name()).append(')');
					throw new NSValidation.ValidationException(buf.toString()
							,var,"eduGroup");
				}
				if(grade() != null && !grade().equals(course.cycle().grade())) {
					buf.append(WOApplication.application().valueForKeyPath(
						"strings.RujelCurriculum_Curriculum.messages.cantSetGrade"));
					buf.append(" (").append(course.cycle().grade()).append(')');
					throw new NSValidation.ValidationException(buf.toString(), var,"eduGroup");
				}
				if(section() != null && section() != ((PlanCycle)course.cycle()).section()) {
					buf.append(WOApplication.application().valueForKeyPath(
						"strings.RujelCurriculum_Curriculum.messages.cantSetSection"));
					buf.append(" (").append(
							((PlanCycle)course.cycle()).section().name()).append(')');
					throw new NSValidation.ValidationException(buf.toString(), var,"eduGroup");
				}
				if(EOPeriod.Utility.compareDates(begin(), var.date())  > 0 || 
						(var.date().getTime() > end)) {
					buf.append(WOApplication.application().valueForKeyPath(
						"strings.RujelCurriculum_Curriculum.messages.cantSetDates"));
					buf.append(" (").append(
							MyUtility.dateFormat().format(var.date())).append(')');
					if(EOPeriod.Utility.compareDates(begin(),var.date()) > 0)
						throw new NSValidation.ValidationException(
								buf.toString(),var,BEGIN_KEY);
					else
						throw new NSValidation.ValidationException(
								buf.toString(),var,END_KEY);
					}
			} // enu variations
		}
	}
	
	public void turnIntoFault(EOFaultHandler handler) {
		super.turnIntoFault(handler);
		_flags = null;
	}
	
	public static interface Event extends EOEnterpriseObject {
		public NSTimestamp date();
		public EduCourse course();
		public Reason reason();
		public Number value();
	}
	
	public static class Props {
		public EOEditingContext ec;
		
		public NSTimestamp begin;
		public NSTimestamp end;
		public Object teacher;
		public EduGroup eduGroup;
		public Integer grade;
		public SchoolSection section;
		public boolean otherTeachers = false;
		
		public Props() {
			super();
		}
		
		public Props(EduCourse course, NSTimestamp onDate) {
			super();
			ec = course.editingContext();
			eduGroup = course.eduGroup();
			teacher = course.teacher(onDate);
			if(teacher == null)
				teacher = NullValue;
			PlanCycle cycle = (PlanCycle)course.cycle();
			grade = cycle.grade();
			section = cycle.section();
		}
		
		public Teacher teacher() {
			if(teacher instanceof Teacher)
				return (Teacher)teacher;
			return null;
		}
		
		public Reason newReason() {
			Reason reason = (Reason)EOUtilities.createAndInsertInstance(ec, ENTITY_NAME);
			if(begin != null)
				reason.setBegin(begin);
			reason.setEnd(end);
			if(teacher != null)
				reason.setTeacher(teacher);
			else if(eduGroup != null)
				reason.setEduGroup(eduGroup);
			else if(grade != null)
				reason.setGrade(grade);
			if(section != null)
				reason.setSection(section);
			return reason;
		}
		
		public String present() {
			StringBuilder result = new StringBuilder();
			boolean added = false;
			if(teacher != null) {
				if(teacher instanceof Teacher) {
					result.append(Person.Utility.fullName(teacher(), true, 2, 1, 1));
				} else {
					result.append(WOApplication.application().valueForKeyPath(
					"strings.RujelBase_Base.vacant"));
				}
				added = true;
			}
	    	if(eduGroup != null) {
	    		if(added)
					result.append(',').append(' ');
				result.append(eduGroup.name());
				added = true;
			} else if(grade != null) {
	    		if(added)
					result.append(',').append(' ');
				result.append(grade);
				added = true;
			}
	    	if(section != null) {
	    		if(added)
					result.append(',').append(' ');
				result.append(section.name());
	    	}
	    	return result.toString();
		}
	}
	
	public void setSection(Object section) {
		if(section instanceof SchoolSection) {
			if(((SchoolSection)section).editingContext() != this.editingContext())
				section = EOUtilities.localInstanceOfObject(
						editingContext(), (SchoolSection)section);
			super.setSection((SchoolSection)section);
		} else {
			super.setSection(null);
		}
	}
}
