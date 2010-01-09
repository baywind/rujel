package net.rujel.base;

import java.util.Calendar;

import net.rujel.interfaces.EduCourse;
import net.rujel.interfaces.EduLesson;
import net.rujel.interfaces.Person;

import com.webobjects.appserver.*;
import com.webobjects.eocontrol.*;
import com.webobjects.foundation.*;

public class LessonsDiary extends WOComponent {
	public static final NSArray monthDate = (NSArray)WOApplication.application().
		valueForKeyPath("strings.Reusables_Strings.presets.monthDate");
	public static final NSArray weekdayShort = (NSArray)WOApplication.application().
		valueForKeyPath("strings.Reusables_Strings.presets.weekdayShort");

	public Object item;    public LessonsDiary(WOContext context) {
		super(context);
	}

	public NSArray courses;

    public Object section;
    protected EOEditingContext ec;
    protected EOQualifier[] quals;
	public NSArray list;

	public void appendToResponse(WOResponse aResponse, WOContext aContext) {
		courses = (NSArray)valueForBinding("courses");
		if(courses == null) {
			aResponse.appendContentString("No courses defined");
			return;
		}
		NSTimestamp date = (NSTimestamp)valueForBinding("date");
		NSTimestamp to = (date == null)?new NSTimestamp():date;
		NSTimestamp since = (NSTimestamp)valueForBinding("since");
		if(since == null) {
			since = to.timestampByAddingGregorianUnits(0, 0, -7, 0, 0, 0);
		}
		
		Integer year = (date == null)?(Integer)application().valueForKey("year"):
			MyUtility.eduYearForDate(date);
		ec = (EOEditingContext)application().valueForKeyPath(
				"ecForYear." + year.toString());
		quals = new EOQualifier[3];
		quals[1] = new EOKeyValueQualifier("date", 
				EOQualifier.QualifierOperatorGreaterThanOrEqualTo, since);
		quals[2] = new EOKeyValueQualifier("date", 
				EOQualifier.QualifierOperatorLessThanOrEqualTo, to);
		super.appendToResponse(aResponse, aContext);
	}
    
	public void setSection(Object nextSection) {
		section = nextSection;
		if(section instanceof EduCourse) {
			quals[0] = new EOKeyValueQualifier("course", 
					EOQualifier.QualifierOperatorEqual, section);
			quals[0] = new EOAndQualifier(new NSArray(quals));
			EOFetchSpecification fs = new EOFetchSpecification(EduLesson.entityName,
					quals[0],EduLesson.sorter);
			fs.setRefreshesRefetchedObjects(true);
			list = ec.objectsWithFetchSpecification(fs);
		} else {
			list = null;
		}
	}
	

	
    public String formatSection() {
		StringBuilder result = new StringBuilder(52);
		if (section instanceof EduCourse) {
			EduCourse course = (EduCourse) section;
			result.append(course.cycle().subject());
			if(course.comment() != null) {
				result.append(" <i>(").append(course.comment()).append(")</i>");
			}
			if(course.teacher() != null) {
				result.append(" <span style=\"font-weight:normal;font-size:medium;\">- ");
				result.append(Person.Utility.fullName(course.teacher(), true, 2, 2, 2));
				result.append("</span>");
			}
		} else { // unknown section type
			return section.toString();
		}
		return result.toString();
	}
	
	public String itemLabel() {
		if(item == null)
			return null;
		EduLesson lesson = (EduLesson)this.item;
		if(lesson.title() != null)
			return lesson.title();
		StringBuffer result = new StringBuffer(16);
		Calendar cal = Calendar.getInstance();
		cal.setTime(lesson.date());
		result.append(cal.get(Calendar.DATE)).append(' ');
		result.append(monthDate.objectAtIndex(cal.get(Calendar.MONTH)));
		result.append(" (");
		result.append(weekdayShort.
				objectAtIndex(cal.get(Calendar.DAY_OF_WEEK) -1)).append(')');
		//MyUtility.dateFormat().format(item.date(), result, new FieldPosition(0));
		result.append(' ');
		return result.toString();
	}
	
	public boolean synchronizesVariablesWithBindings() {
        return false;
	}
	public boolean isStateless() {
		return true;
	}
	
	public void reset() {
		courses = null;
		section = null;
		ec = null;
		quals = null;
		item = null;
		super.reset();
	}
}