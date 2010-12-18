package net.rujel.curriculum;

import java.util.Enumeration;

import net.rujel.base.MyUtility;
import net.rujel.interfaces.EduCourse;
import net.rujel.interfaces.EduLesson;
import net.rujel.interfaces.Person;
import net.rujel.interfaces.Teacher;
import net.rujel.reusables.Export;
import net.rujel.reusables.ExportCSV;

import com.webobjects.appserver.WOActionResults;
import com.webobjects.appserver.WOApplication;
import com.webobjects.appserver.WOContext;
import com.webobjects.appserver.WOComponent;
import com.webobjects.appserver.WOResponse;
import com.webobjects.appserver.WOSession;
import com.webobjects.eocontrol.EOSortOrdering;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSDictionary;
import com.webobjects.foundation.NSMutableArray;
import com.webobjects.foundation.NSMutableDictionary;
import com.webobjects.foundation.NSMutableSet;

public class JournalZPU extends WOComponent {
	
	public NSArray journalZPU;
	public NSDictionary currMonth;
	public NSDictionary item;
	public Object currTeacher;
	
    public JournalZPU(WOContext context) {
        super(context);
    }

	protected static NSArray prepareJournalZPU(Enumeration substitutes, Enumeration variations) {
		NSMutableArray result = new NSMutableArray();
		NSMutableSet lessons = new NSMutableSet();
		if(variations != null) {
			while (variations.hasMoreElements()) {
				Variation var = (Variation) variations.nextElement();
				EduLesson lesson = var.relatedLesson();
				if(lesson != null) {
					if(lessons.containsObject(lesson))
						continue;
					lessons.addObject(lesson);
				}
				NSMutableDictionary dict = convertEvent(var);
				NSArray multiply = (NSArray)dict.valueForKey("multiply");
				if(multiply != null) {
					Enumeration mul = multiply.objectEnumerator();
					while (mul.hasMoreElements()) {
						Substitute sub = (Substitute) mul.nextElement();
						NSMutableDictionary clon = dict.mutableClone();
						clon.takeValueForKey(sub.teacher(), "plusTeacher");
						clon.takeValueForKey(sub.value(), "value");
						result.addObject(clon);
					}
				} else {
					result.addObject(dict);
				}
			}
		}
		if(substitutes != null) {
			while (substitutes.hasMoreElements()) {
				Substitute sub = (Substitute) substitutes.nextElement();
				if(lessons.containsObject(sub.lesson()))
						continue;
//				lessons.addObject(sub.lesson());
				NSMutableDictionary dict = convertEvent(sub);
				result.addObject(dict);
			}
		}
		NSArray sorter = new NSArray(new EOSortOrdering[] {
				new EOSortOrdering("date",EOSortOrdering.CompareAscending),
				new EOSortOrdering("minusTeacher",EOSortOrdering.CompareAscending),
				new EOSortOrdering("grade",EOSortOrdering.CompareAscending),
				new EOSortOrdering("eduGroup",EOSortOrdering.CompareAscending),
				new EOSortOrdering("plusTeacher",EOSortOrdering.CompareAscending)
		});
		EOSortOrdering.sortArrayUsingKeyOrderArray(result, sorter);
		return result;
	}
	
	protected static NSMutableDictionary convertEvent(Reason.Event event) {
		NSMutableDictionary dict = new NSMutableDictionary(event.reason(),"reason");
		if(event.reason().verification() == null)
			dict.takeValueForKey("grey", "cellClass");
		dict.takeValueForKey(event.date(), "date");
		EduCourse course = event.course();
		dict.takeValueForKey(course.eduGroup(), "eduGroup");
		dict.takeValueForKey(course.cycle().grade(), "grade");
		Substitute sub = null;
		if (event instanceof Variation) {
			Variation var = (Variation) event;
			dict.takeValueForKey(var.relatedLesson(), "lesson");
			Variation back = var.getPaired();
			NSArray subs = (NSArray)var.valueForKeyPath("relatedLesson.substitutes");
			if(subs != null && subs.count() > 0) {
				if(subs.count() == 1) {
					sub = (Substitute)subs.objectAtIndex(0);
				} else {
					dict.takeValueForKey(subs, "multiply");
				}
			}
			if(var.value().intValue() > 0) {
				dict.takeValueForKey(var.course(), "plusCourse");
				if(back != null) {
					dict.takeValueForKey(back.course(), "minusCourse");
					dict.takeValueForKey(back.course().teacher(), "minusTeacher");
				}
				if(sub == null) {
					dict.takeValueForKey(var.value(), "value");
					dict.takeValueForKey(var.course().teacher(), "plusTeacher");
				}
			} else {
				dict.takeValueForKey(var.course(), "minusCourse");
				dict.takeValueForKey(var.course().teacher(), "minusTeacher");
				if(back != null) {
					dict.takeValueForKey(back.course(), "plusCourse");
					if(sub == null) {
						dict.takeValueForKey(back.course().teacher(), "plusTeacher");
						dict.takeValueForKey(back.value(), "value");
					}
				} else if(sub == null) {
					dict.takeValueForKey(new Integer(-var.value().intValue()), "value");
				}
			}
		} else if(event instanceof Substitute) {
			sub = (Substitute)event;
			dict.takeValueForKey(sub.course(), "minusCourse");
			dict.takeValueForKey(sub.course(), "plusCourse");
			dict.takeValueForKey(sub.course().teacher(), "minusTeacher");
			dict.takeValueForKey(sub.lesson(), "lesson");
		}
		if(sub != null) {
			dict.takeValueForKey(sub.teacher(), "plusTeacher");
			dict.takeValueForKey(sub.value(), "value");
		}
		return dict;
	}
	
	public String jrowClass() {
		if(currTeacher != null) {
			if(item.valueForKey("minusTeacher") == currTeacher)
				return "female";
			if(item.valueForKey("plusTeacher") == currTeacher)
				return "male";
		} else {
			if(item.valueForKey("minusTeacher") == null)
				return "male";
			if(item.valueForKey("plusTeacher") == null)
				return "female";
			return "green";
		}
		if(item.valueForKey("minusTeacher") == null || item.valueForKey("plusTeacher") == null)
			return "gerade";
		return "ungerade";
	}

	public static WOActionResults exportJournalZPU(NSArray journal,
			WOContext context,String filename) {
		WOSession ses = context.session();
		if(journal == null || journal.count() == 0) {
			WOResponse response = WOApplication.application().createResponseInContext(context);
    		response.appendContentString((String)ses.valueForKeyPath(
				"strings.RujelCurriculum_Curriculum.Tabel.noData"));
        	response.setHeader("application/octet-stream","Content-Type");
        	response.setHeader("attachment; filename=\"noData.txt\"","Content-Disposition");
        	return response;
		}
		Export export = new ExportCSV(context,filename);
		export.beginRow();
		export.addValue(ses.valueForKeyPath("strings.Reusables_Strings.dataTypes.Date"));
		export.addValue(ses.valueForKeyPath("strings.RujelCurriculum_Curriculum.OrigTeacher"));
		export.addValue(ses.valueForKeyPath("strings.RujelInterfaces_Names.EduCycle.subject"));
		export.addValue(ses.valueForKeyPath("strings.RujelInterfaces_Names.EduGroup.this"));
		export.addValue(ses.valueForKeyPath("strings.RujelCurriculum_Curriculum.Reason.Reason"));
		export.addValue(ses.valueForKeyPath(
				"strings.RujelCurriculum_Curriculum.Reason.verification"));
		export.addValue(ses.valueForKeyPath(
				"strings.RujelCurriculum_Curriculum.Substitute.Substitutor"));
		export.addValue(ses.valueForKeyPath("strings.RujelInterfaces_Names.EduCycle.subject"));
		export.addValue(ses.valueForKeyPath(
				"strings.RujelCurriculum_Curriculum.Substitute.factor"));
		Enumeration enu = journal.objectEnumerator();
		StringBuilder buf = new StringBuilder();
		while (enu.hasMoreElements()) {
			NSDictionary dict = (NSDictionary) enu.nextElement();
			export.beginRow();
			export.addValue(MyUtility.dateFormat().format(dict.valueForKey("date")));
			EduCourse course = (EduCourse)dict.valueForKey("minusCourse");
			if(course != null) {
				Teacher teacher = (Teacher)dict.valueForKey("minusTeacher");
				if(teacher != null)
					export.addValue(Person.Utility.fullName(teacher, true, 2, 1, 1));
				else
					export.addValue(ses.valueForKeyPath("strings.RujelBase_Base.vacant"));
				if(course.comment() == null) {
					export.addValue(course.cycle().subject());
				} else {
					buf.delete(0, buf.length());
					buf.append(course.cycle().subject());
					buf.append(' ').append('(').append(course.comment()).append(')');
					export.addValue(buf.toString());
				}
			} else {
				export.addValue(null);
				export.addValue(null);
			}
			if(dict.valueForKey("eduGroup") != null)
				export.addValue(dict.valueForKeyPath("eduGroup.name"));
			else
				export.addValue(dict.valueForKey("grade"));
			export.addValue(dict.valueForKeyPath("reason.title"));
			export.addValue(dict.valueForKeyPath("reason.verification"));
			course = (EduCourse)dict.valueForKey("plusCourse");
			if(course != null) {
				Teacher teacher = (Teacher)dict.valueForKey("plusTeacher");
				export.addValue(Person.Utility.fullName(teacher, true, 2, 1, 1));
				if(course.comment() == null) {
					export.addValue(course.cycle().subject());
				} else {
					buf.delete(0, buf.length());
					buf.append(course.cycle().subject());
					buf.append(' ').append('(').append(course.comment()).append(')');
					export.addValue(buf.toString());
				}
			} else {
				export.addValue(null);
				export.addValue(null);
			}
			export.addValue(dict.valueForKey("value"));
		}
		return export;
	}
	
	public WOActionResults exportZPU() {
    	StringBuilder buf = new StringBuilder("journal");
    	buf.append(currMonth.valueForKey("year"));
    	int month = ((Integer)currMonth.valueForKey("month")).intValue();
    	month++;
    	if(month < 10)
    		buf.append('0');
    	buf.append(month);
		return exportJournalZPU(journalZPU,context(),buf.toString());
	}
	
	public boolean synchronizesVariablesWithBindings() {
        return true;
	}
	
	public boolean isStateless() {
		return true;
	}
	
	public void reset() {
		currMonth = null;
		currTeacher = null;
		journalZPU = null;
		item = null;
		super.reset();
	}
}