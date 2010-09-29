package net.rujel.curriculum;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.GregorianCalendar;

import net.rujel.base.BaseCourse;
import net.rujel.eduplan.PlanCycle;
import net.rujel.interfaces.EduCourse;
import net.rujel.interfaces.EduLesson;
import net.rujel.interfaces.Person;
import net.rujel.interfaces.Teacher;
import net.rujel.reusables.Counter;
import net.rujel.reusables.SessionedEditingContext;
import net.rujel.reusables.SettingsReader;
import net.rujel.reusables.Export;
import net.rujel.ui.TeacherSelector;

import com.webobjects.appserver.*;
import com.webobjects.eocontrol.*;
import com.webobjects.foundation.*;

// Generated by the WOLips Templateengine Plug-in at May 13, 2009 2:56:05 PM
public class Tabel extends com.webobjects.appserver.WOComponent {
	
	public EOEditingContext ec;
	public NSArray months;
	public NSDictionary currMonth;
	public NSDictionary item;
	public Teacher currTeacher;
	public NSMutableArray details;
	public Integer index;
	public Boolean cantSelect;
	
    public Tabel(WOContext context) {
        super(context);
        ec = new SessionedEditingContext(context.session());
        Calendar cal = Calendar.getInstance();
        cal.setTime((NSTimestamp)session().valueForKey("today"));
//        currMonth = new NSMutableDictionary(cal,"cal");
        int cmi = cal.get(Calendar.MONTH);
        NSArray monthNames = (NSArray)application().valueForKeyPath(
        		"strings.Reusables_Strings.presets.monthLong");
//        currMonth.takeValueForKey(monthNames.objectAtIndex(cmi), "name");
        cal.set(Calendar.YEAR, ((Number)session().valueForKey("eduYear")).intValue());
        cal.set(Calendar.MONTH,
        		SettingsReader.intForKeyPath("edu.newYearMonth",GregorianCalendar.JULY));
        NSDictionary[] mns = new NSDictionary[12 + 
                  ((SettingsReader.intForKeyPath("edu.newYearDay",1) > 1)?1:0)];
        for (int i = 0; i < mns.length; i++) {
        	int m = cal.get(Calendar.MONTH);
			mns[i] = new NSDictionary(new Object[] {
					new Integer(cal.get(Calendar.YEAR)), new Integer(m),
						monthNames.objectAtIndex(m),
						new Integer(cal.getActualMaximum(Calendar.DAY_OF_MONTH))}
			,new Object[] { "year", "month", "name","days" });
			if(m == cmi)
				currMonth = mns[i];
			cal.add(Calendar.MONTH, 1);
		}
        months = new NSArray(mns);
        EOKeyGlobalID gid = (EOKeyGlobalID)context.session().valueForKey("userPersonGID");
        if(gid != null && gid.entityName().equals(Teacher.entityName)) {
        	setCurrTeacher((Teacher)ec.faultForGlobalID(gid, ec));
        	cantSelect = (Boolean)context.session().valueForKeyPath("readAccess._edit.Tabel");
        }
    }
    
    public String title() {
    	return (String)application().valueForKeyPath(
    			"strings.RujelCurriculum_Curriculum.Tabel.title");
    }
    
    protected static void addHoursToKey(NSMutableDictionary dict, 
    		BigDecimal hours, Calendar cal, Object key) {
    	BigDecimal[] byTeacher = (BigDecimal[])((dict==null)?key:dict.objectForKey(key));
    	if(byTeacher ==  null) {
    		byTeacher = new BigDecimal[cal.getActualMaximum(Calendar.DAY_OF_MONTH) +1];
    		dict.setObjectForKey(byTeacher, key);
    	}    	
    	int day = cal.get(Calendar.DAY_OF_MONTH);
    	if(byTeacher[day] == null) {
    		byTeacher[day] = hours;
    	} else {
    		byTeacher[day] = byTeacher[day].add(hours);
    	}
    	if(byTeacher[0] == null) {
    		byTeacher[0] = hours;
    	} else {
    		byTeacher[0] = byTeacher[0].add(hours);
    	}
    }
    
    protected EOQualifier monthQual(Calendar cal) {
    	EOQualifier quals[] = monthQuals(((Integer)currMonth.valueForKey("year")).intValue()
    			, ((Integer)currMonth.valueForKey("month")).intValue(), "date",cal);
    	return new EOAndQualifier(new NSArray(quals));
    }
    
    public static EOQualifier[] monthQuals(int year, int month, String key, Calendar cal) {
    	if(cal == null)
    		cal = Calendar.getInstance();
    	cal.set(Calendar.YEAR, year);
    	cal.set(Calendar.MONTH, month);
    	cal.set(Calendar.DAY_OF_MONTH, 1);
    	EOQualifier quals[] = new EOQualifier[2];
    	quals[0] = new EOKeyValueQualifier("date",
    			EOQualifier.QualifierOperatorGreaterThanOrEqualTo,
    			new NSTimestamp(cal.getTimeInMillis()));
    	cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
    	quals[1] = new EOKeyValueQualifier("date",
    			EOQualifier.QualifierOperatorLessThanOrEqualTo,
    			new NSTimestamp(cal.getTimeInMillis()));
    	return quals;
    }
    
    public WOActionResults export() {
    	Calendar cal = Calendar.getInstance();
    	EOFetchSpecification fs = new EOFetchSpecification(
    			Substitute.ENTITY_NAME,monthQual(cal),null);
    	fs.setRefreshesRefetchedObjects(true);
    	ec.objectsWithFetchSpecification(fs);
    	fs.setEntityName(EduLesson.entityName);
//    	NSArray list = new NSArray(new String[] {"substitutes.teacher","course.teacher"});
//    	fs.setPrefetchingRelationshipKeyPaths(list);
    	NSArray list = ec.objectsWithFetchSpecification(fs);
    	if(list == null || list.count() == 0) {
			WOResponse response = WOApplication.application().createResponseInContext(context());
    		response.appendContentString((String)session().valueForKeyPath(
				"strings.RujelCurriculum_Curriculum.Tabel.noData"));
        	response.setHeader("application/octet-stream","Content-Type");
        	response.setHeader("attachment; filename=\"noData.txt\"","Content-Disposition");
        	return response;
    	}
    	NSMutableDictionary byTeacher = new NSMutableDictionary();
    	NSMutableDictionary subsByTeacher = new NSMutableDictionary();
    	NSMutableDictionary plusByTeacher = new NSMutableDictionary();
    	NSMutableDictionary byCourse = new NSMutableDictionary();
    	Enumeration enu = list.objectEnumerator();
    	while (enu.hasMoreElements()) {   // lessons
			EduLesson lesson = (EduLesson) enu.nextElement();
			cal.setTime(lesson.date());
			NSArray subs = (NSArray)lesson.valueForKey("substitutes");
			if(subs != null && subs.count() > 0) {
				int[] sbt = (int[])byCourse.objectForKey(lesson.course());
				if(sbt == null) {
					sbt = new int[cal.getActualMaximum(Calendar.DAY_OF_MONTH) +1];
					byCourse.setObjectForKey(sbt, lesson.course());
				}
				sbt[cal.get(Calendar.DATE)]--;
				Enumeration sEnu = subs.objectEnumerator();
				while (sEnu.hasMoreElements()) {
					Substitute sub = (Substitute) sEnu.nextElement();
					Teacher teacher = sub.teacher();
					addHoursToKey(byTeacher, sub.factor(), cal, teacher);
					BigDecimal bySub = (BigDecimal)subsByTeacher.objectForKey(teacher);
					if(bySub == null) {
						subsByTeacher.setObjectForKey(sub.factor(), teacher);
					} else {
						subsByTeacher.setObjectForKey(bySub.add(sub.factor()), teacher);
					}
				}
			} else {
				Object teacher = lesson.course().teacher(lesson.date());
				if(teacher == null)
					teacher = NSDictionary.EmptyDictionary;
				addHoursToKey(byTeacher, BigDecimal.ONE, cal,teacher);
			}
		} // lessons
    	list = new NSArray(EOSortOrdering.sortOrderingWithKey(Variation.VALUE_KEY, 
    			EOSortOrdering.CompareDescending));
    	fs.setSortOrderings(list);
    	fs.setEntityName(Variation.ENTITY_NAME);
    	list = ec.objectsWithFetchSpecification(fs);
    	if(list != null && list.count() > 0) {
    		Enumeration vEnu = list.objectEnumerator();
    		while (vEnu.hasMoreElements()) { // variations
				Variation var = (Variation) vEnu.nextElement();
				int val = var.value().intValue();
				int[] sbt = (int[])byCourse.objectForKey(var.course());
				if(sbt == null) {
					if(val < 0)
						continue;
					sbt = new int[cal.getActualMaximum(Calendar.DATE)];
					byCourse.setObjectForKey(sbt, var.course());
				}
				cal.setTime(var.date());
				int day = cal.get(Calendar.DAY_OF_MONTH);
				if(val < 0) {  // negative
					if(sbt[day] > 0) {
						Teacher teacher = var.course().teacher(var.date());
						Counter cnt = (Counter)plusByTeacher.objectForKey(teacher);
						if(cnt != null) {
							cnt.add(val);
							val = cnt.intValue();
							if(val <= 0)
								plusByTeacher.removeObjectForKey(teacher);
						}
						if(val < 0) {
							BigDecimal bySub = (BigDecimal)subsByTeacher.objectForKey(teacher);
							if(bySub == null)
								continue;
							bySub = bySub.add(new BigDecimal(val));
							if(bySub.intValue() <= 0)
								subsByTeacher.removeObjectForKey(teacher);
							else
								subsByTeacher.setObjectForKey(bySub, teacher);
						}
					}
					sbt[day] += val;
				} else {  // val > 0 positive
					sbt[day] += val;
					if(sbt[day] < 0)
						continue;
					if(sbt[day] < val)
						val = sbt[day];
					Object teacher = var.course().teacher();
					if(teacher == null) teacher = NSDictionary.EmptyDictionary;
					NSArray paired = var.getAllPaired(true);
					if(paired != null) {
						if(paired.count() > 1) {
							list = new NSArray(new EOSortOrdering(Variation.VALUE_KEY,
									EOSortOrdering.CompareAscending));
							paired = EOSortOrdering.sortedArrayUsingKeyOrderArray(
									paired, list);
						}
						Enumeration pEnu = paired.objectEnumerator();
						while (pEnu.hasMoreElements() && val > 0) {
							Variation pr = (Variation) pEnu.nextElement();
							int pv = -pr.value().intValue();
							BigDecimal cnt = new BigDecimal(pv);
							BigDecimal bySub = (BigDecimal)subsByTeacher.objectForKey(teacher);
							if(bySub == null) {
								subsByTeacher.setObjectForKey(cnt, teacher);
							} else {
								subsByTeacher.setObjectForKey(bySub.add(cnt), teacher);
							}
							val -= pv;
						} // paired enumeration
					} // paired != null 
					if(val > 0) {
						Counter cnt = (Counter)plusByTeacher.objectForKey(teacher);
						if(cnt == null)
							plusByTeacher.setObjectForKey(new Counter(val), teacher);
						else
							cnt.add(val);
					}
				} //val > 0
			} // variations enumeration
    	} // have variatins
    	list = EOSortOrdering.sortedArrayUsingKeyOrderArray(
    			byTeacher.allKeys(),Person.sorter);
    	StringBuilder buf = new StringBuilder("tabel");
    	buf.append(currMonth.valueForKey("year"));
    	int month = ((Integer)currMonth.valueForKey("month")).intValue();
    	month++;
    	if(month < 10)
    		buf.append('0');
    	buf.append(month);
    	Export exportPage = new Export(context(),buf.toString());
    	
    	exportPage.beginRow();
    	exportPage.addValue(currMonth.valueForKey("name"));
    	int days = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
		for (int i = 1; i <= days; i++) {
			exportPage.addValue(Integer.toString(i));
		}
		exportPage.addValue(application().valueForKeyPath(
				"strings.RujelCurriculum_Curriculum.Tabel.total"));
		if(subsByTeacher.count() > 0)
			exportPage.addValue(application().valueForKeyPath(
					"strings.RujelCurriculum_Curriculum.Tabel.bySubs"));
		if(plusByTeacher.count() > 0)
			exportPage.addValue(application().valueForKeyPath(
					"strings.RujelCurriculum_Curriculum.Tabel.extraLessons.subject"));
		exportPage.endRow();
    	
    	NSNumberFormatter formatter = new NSNumberFormatter();
    	formatter.setDecimalSeparator(",");
    	formatter.setThousandSeparator(" ");
    	enu = list.objectEnumerator();
    	while (enu.hasMoreElements()) {
			Object teacher = enu.nextElement();
			exportPage.beginRow();
			if(teacher instanceof Teacher)
				exportPage.addValue(Person.Utility.fullName(
						(Teacher)teacher, true, 2, 1, 1));
			else
				exportPage.addValue(session().valueForKeyPath(
						"strings.RujelBase_Base.vacant"));
			BigDecimal[] allHours = (BigDecimal[]) byTeacher.objectForKey(teacher); 
			for (int i = 0; i <= days; i++) {
				BigDecimal value = allHours[(i==days)?0:i + 1];
				if(value != null) {
					value = value.stripTrailingZeros();
					if(value.scale() < 0)
						value = value.setScale(0);
				}
				exportPage.addValue(formatter.format(value));
			}
			if(subsByTeacher.count() > 0) {
				BigDecimal value = (BigDecimal)subsByTeacher.objectForKey(teacher);
				if(value != null) {
					value = value.stripTrailingZeros();
					if(value.scale() < 0)
						value = value.setScale(0);
				}
				exportPage.addValue(formatter.format(value));
			}
			if(plusByTeacher.count() > 0)
				exportPage.addValue(plusByTeacher.objectForKey(teacher));
			exportPage.endRow();
		}
    	return exportPage;
    }
    
	public WOActionResults exportDetails() {
		if(details == null || details.count() == 0) {
			WOResponse response = WOApplication.application().createResponseInContext(context());
    		response.appendContentString((String)session().valueForKeyPath(
				"strings.RujelCurriculum_Curriculum.Tabel.noData"));
        	response.setHeader("application/octet-stream","Content-Type");
        	response.setHeader("attachment; filename=\"noData.txt\"","Content-Disposition");
        	return response;
		}
    	StringBuilder buf = new StringBuilder("details");
    	buf.append(currMonth.valueForKey("year"));
    	int month = ((Integer)currMonth.valueForKey("month")).intValue();
    	month++;
    	if(month < 10)
    		buf.append('0');
    	buf.append(month);
		Export export = new Export(context(),buf.toString());

		int days = ((Integer)currMonth.valueForKey("days")).intValue();
		export.beginRow();
//		export.addValue(currMonth.valueForKey("name"));
		export.addValue(Person.Utility.fullName(currTeacher, true, 2, 2, 2));
		export.addValue(null);
		for (int i = 1; i <= days; i++) {
			export.addValue(Integer.toString(i));
		}
		export.addValue(application().valueForKeyPath(
				"strings.RujelCurriculum_Curriculum.Tabel.total"));
		export.endRow();

		Enumeration enu = details.objectEnumerator();
    	NSNumberFormatter formatter = new NSNumberFormatter();
    	formatter.setDecimalSeparator(",");
    	formatter.setThousandSeparator(" ");
		while (enu.hasMoreElements()) {
			NSDictionary row = (NSDictionary) enu.nextElement();
			export.beginRow();
			export.addValue(row.valueForKey("eduGroup"));
			export.addValue(row.valueForKey("subject"));
			BigDecimal[] values = (BigDecimal[])row.valueForKey("values");
			NSMutableArray[] info = (NSMutableArray[])row.valueForKey("info");
			for (int i = 0; i <= days; i++) {
				BigDecimal value = values[(i==days)?0:i + 1];
				if(value != null) {
					value = value.stripTrailingZeros();
					if(value.scale() < 0)
						value = value.setScale(0);
				}
				String val = formatter.format(value);
				if(i < days && info != null && info[i+1] != null && info[i+1].count() > 0)
					val = val + '*';
				export.addValue(val);
			}
			export.endRow();
		}
		return export;
	}
    
    public void setCurrTeacher(Teacher teacher) {
    	currTeacher = teacher;
    	go();
    }
    
    public void setCurrMonth(NSDictionary month) {
		currMonth = month;
		go();
	}
    
    public void go() {
    	if(currTeacher == null || currMonth == null)
    		return;
    	Calendar cal = Calendar.getInstance();
    	cal.set(Calendar.YEAR,((Integer)currMonth.valueForKey("year")).intValue());
    	cal.set(Calendar.MONTH, ((Integer)currMonth.valueForKey("month")).intValue());
    	cal.set(Calendar.DAY_OF_MONTH, 1);
    	NSTimestamp firstDay = new NSTimestamp(cal.getTimeInMillis());
    	int days = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
    	cal.set(Calendar.DAY_OF_MONTH, days);
    	NSTimestamp lastDay = new NSTimestamp(cal.getTimeInMillis());

    	// courses for teacher
    	EOQualifier[] quals = new EOQualifier[2];//NSMutableArray();
    	quals[0] = new EOKeyValueQualifier("date",
    			EOQualifier.QualifierOperatorGreaterThanOrEqualTo,firstDay);
    	quals[1] = new EOKeyValueQualifier("date",
    			EOQualifier.QualifierOperatorLessThanOrEqualTo,lastDay);
    	EOQualifier monthQual = new EOAndQualifier(new NSArray(quals));
    	EOQualifier teacherQual =  new EOKeyValueQualifier("teacher",
    			EOQualifier.QualifierOperatorEqual,currTeacher);
    	quals[0] = teacherQual;
    	quals[1] = new EOKeyValueQualifier("eduYear",EOQualifier.QualifierOperatorEqual,
    			session().valueForKey("eduYear"));
    	quals[1] = new EOAndQualifier(new NSArray(quals));
    	EOFetchSpecification fs = new EOFetchSpecification(
    			EduCourse.entityName,quals[1],null);
    	fs.setRefreshesRefetchedObjects(true);
    	NSArray list = ec.objectsWithFetchSpecification(fs);
       	NSTimestamp[] dates = new NSTimestamp[2];
       	int weekly = 0;
		details = new NSMutableArray();
		final NSMutableArray clear = new NSMutableArray();
    	if(list != null && list.count() > 0) {
	    	quals[1] = new EOKeyValueQualifier("date",
	    			EOQualifier.QualifierOperatorLessThanOrEqualTo,lastDay);
    		Enumeration enu = list.objectEnumerator();
    		while (enu.hasMoreElements()) {
				BaseCourse course = (BaseCourse) enu.nextElement();
				EOEnterpriseObject ct = course.teacherChange(lastDay,dates);
				if(ct != null)
					continue;
				PlanCycle cycle = (PlanCycle)course.cycle();
				int hours = cycle.weekly(course);
				weekly += hours;
				NSMutableDictionary dict = new NSMutableDictionary(course,"course");
				dict.takeValueForKey(new Integer(hours), "hours");
				if(dates[0] != null && dates[0].compare(firstDay) > 0) {
			    	quals[0] = new EOKeyValueQualifier("date",
			    			EOQualifier.QualifierOperatorGreaterThanOrEqualTo,dates[0]);
			    	dict.takeValueForKey(new EOAndQualifier(new NSArray(quals)),"qual");
			    	NSMutableArray[] info = new NSMutableArray[days +1];
			    	cal.setTime(dates[0]);
			    	for (int i = cal.get(Calendar.DAY_OF_MONTH) -1; i > 0; i--) {
						info[i] = clear;
					}
			    	dict.takeValueForKey(info, "info");
				} else {
			    	dict.takeValueForKey(monthQual,"qual");
				}
		    	details.addObject(dict);
    		}
    	}
    	quals[0] = teacherQual;
    	quals[1] = new EOKeyValueQualifier("date",
    			EOQualifier.QualifierOperatorGreaterThan,firstDay);
    	quals[0] = new EOAndQualifier(new NSArray(quals));
    	fs.setQualifier(quals[0]);
    	fs.setEntityName("TeacherChange");
    	list = ec.objectsWithFetchSpecification(fs);
     	
    	if(list != null && list.count() > 0) {
    		Enumeration enu = list.objectEnumerator();
    		while (enu.hasMoreElements()) {
				EOEnterpriseObject ct = (EOEnterpriseObject) enu.nextElement();
				NSTimestamp end = (NSTimestamp)ct.valueForKey("date");
				end = end.timestampByAddingGregorianUnits(0, 0, -1, 0, 0, 0);
				BaseCourse course = (BaseCourse) ct.valueForKey("course");
				ct = course.teacherChange(end, dates);
				if(ct == null)
					continue;
				NSTimestamp begin = dates[0];
				if(begin != null && begin.compare(lastDay) > 0)
					continue;
		    	NSMutableArray[] info = null;
				if(begin == null || begin.compare(firstDay) < 0) {
					begin = firstDay;
				} else {
					info = new NSMutableArray[days +1];
			    	cal.setTime(begin);
			    	for (int i = cal.get(Calendar.DAY_OF_MONTH) -1; i > 0; i--) {
						info[i] = clear;
					}
				}
				if(end.compare(lastDay) > 0) {
					end = lastDay;
				} else {
					if(info == null)
						info = new NSMutableArray[days +1];
			    	cal.setTime(end);
			    	for (int i = cal.get(Calendar.DAY_OF_MONTH) +1; i < info.length; i++) {
						info[i] = clear;
					}
				}
				PlanCycle cycle = (PlanCycle)course.cycle();
				int hours = cycle.weekly(course);
				NSMutableDictionary dict = new NSMutableDictionary(course,"course");
		    	dict.takeValueForKey(info, "info");
				dict.takeValueForKey(new Integer(hours), "hours");
		    	quals[0] = new EOKeyValueQualifier("date",
		    			EOQualifier.QualifierOperatorGreaterThanOrEqualTo,begin);
		    	quals[1] = new EOKeyValueQualifier("date",
		    			EOQualifier.QualifierOperatorLessThanOrEqualTo,end);
		    	dict.takeValueForKey(new EOAndQualifier(new NSArray(quals)),"qual");
		    	details.addObject(dict);
			}
    	}

    	if(details.count() > 1) {
           	NSArray sorter = new NSArray(new EOSortOrdering[] {
        			new EOSortOrdering("course.cycle",EOSortOrdering.CompareAscending),
        			new EOSortOrdering("course.eduGroup",EOSortOrdering.CompareAscending),
        			new EOSortOrdering("course.comment",EOSortOrdering.CompareAscending)});
           	EOSortOrdering.sortArrayUsingKeyOrderArray(details, sorter);
    	}
    	list = details.immutableClone();
    	BigDecimal[] totals = new BigDecimal[days + 1];
    	BigDecimal[] subsTotals = new BigDecimal[days + 1];
		BigDecimal[] factorCounts = null;
    	BigDecimal[] extras = null;
    	NSMutableDictionary row = ((NSDictionary)application().valueForKeyPath(
    			"strings.RujelCurriculum_Curriculum.Tabel.grandTotal")).mutableClone();
    	row.setObjectForKey(totals, "values");
    	details = new NSMutableArray(row);
		NSMutableDictionary byCourse = new NSMutableDictionary();
// main courses
    	if(list.count() > 0) {
    		BigDecimal[] mainTotals = new BigDecimal[days + 1];
    		row = ((NSDictionary)application().valueForKeyPath(
				"strings.RujelCurriculum_Curriculum.Tabel.mainTotal")).mutableClone();
    		row.setObjectForKey(mainTotals, "values");
    		details.addObject(row);
    		String itemClass = (String)row.valueForKey("itemClass");
    		Enumeration coursEnu = list.objectEnumerator();
			NSArray sorter = new NSArray(new EOSortOrdering(Variation.VALUE_KEY,
					EOSortOrdering.CompareAscending));
    		while (coursEnu.hasMoreElements()) {
    			NSDictionary dict = (NSDictionary)coursEnu.nextElement();
				EduCourse course = (EduCourse) dict.valueForKey("course");
				row = new NSMutableDictionary(course,"course");
				row.takeValueForKey(course.eduGroup().name(), "eduGroup");
				row.takeValueForKey(course.subjectWithComment(),"subject");
				row.takeValueForKey(itemClass, "class");
				row.takeValueForKey(dict.valueForKey("hours"), "hours");
				BigDecimal[] allHours = new BigDecimal[days + 1];
	        	NSMutableArray[] info = (NSMutableArray[])dict.valueForKey("info");
				row.setObjectForKey(allHours, "values");
				quals[0] = (EOQualifier)dict.valueForKey("qual");
				quals[1] = new EOKeyValueQualifier("course",
						EOQualifier.QualifierOperatorEqual,course);
	    		fs.setEntityName(EduLesson.entityName);
				fs.setQualifier(new EOAndQualifier(new NSArray(quals)));
				fs.setSortOrderings(null);
				list = ec.objectsWithFetchSpecification(fs);
				if(list != null && list.count() > 0) {  //lessons
					Enumeration lEnu = list.objectEnumerator();
					while (lEnu.hasMoreElements()) {
						EduLesson lesson = (EduLesson) lEnu.nextElement();
						cal.setTime(lesson.date());
						list = (NSArray)lesson.valueForKey("substitutes");
						if(list != null && list.count() > 0) {
							if(info == null)
								info = new NSMutableArray[days +1];
							int day = cal.get(Calendar.DAY_OF_MONTH);
							if(info[day] == null)
								info[day] = new NSMutableArray(lesson);
							else
								info[day].addObject(lesson);
						} else {
							addHoursToKey(null, BigDecimal.ONE, cal, allHours);
							addHoursToKey(null, BigDecimal.ONE, cal, mainTotals);
							addHoursToKey(null, BigDecimal.ONE, cal, totals);
						}
					}
				} //lessons
				fs.setSortOrderings(sorter);
				fs.setEntityName(Variation.ENTITY_NAME);
				list = ec.objectsWithFetchSpecification(fs);
				if(list != null && list.count() > 0) {  //variations
					if(info == null)
						info = new NSMutableArray[days +1];
					Enumeration vEnu = list.objectEnumerator();
					while (vEnu.hasMoreElements()) {
						Variation var = (Variation) vEnu.nextElement();
						int val = var.value().intValue();
						cal.setTime(var.date());
						int day = cal.get(Calendar.DAY_OF_MONTH);
						if(info[day] == null) {
							info[day] = new NSMutableArray(var);
						} else {
							Enumeration dEnu = info[day].objectEnumerator();
							while (dEnu.hasMoreElements()) {
								Object obj = dEnu.nextElement();
								if(obj instanceof EduLesson)
									val--;
								else if (obj instanceof Variation)
									val += ((Variation)obj).value().intValue();
							}
							info[day].addObject(var);
						}
						if(val <= 0)
							continue;
						addHoursToKey(null, new BigDecimal(-val), cal, mainTotals);
						NSArray paired = var.getAllPaired(true);
						if(paired != null) {
							if(paired.count() > 1) {
//								list = new NSArray(new EOSortOrdering(Variation.VALUE_KEY,
//										EOSortOrdering.CompareAscending));
								paired = EOSortOrdering.sortedArrayUsingKeyOrderArray(
										paired, fs.sortOrderings());
							}
							Enumeration pEnu = paired.objectEnumerator();
							while (pEnu.hasMoreElements() && val > 0) {
								Variation pr = (Variation) pEnu.nextElement();
								int pv = -pr.value().intValue();
								BigDecimal cnt = new BigDecimal(pv);
								addHoursToKey(byCourse, cnt, cal, pr.course());
								if(factorCounts == null)
									factorCounts = new BigDecimal[days + 1];
								addHoursToKey(null, cnt, cal, factorCounts);
								addHoursToKey(null, cnt, cal, subsTotals);
								val -= pv;
							}
						}
						if(val > 0) {
							if(extras == null)
								extras = new BigDecimal[days + 1];
							addHoursToKey(null, new BigDecimal(val), cal, extras);
						}
					}
				} // variations
				row.takeValueForKey(info, "info");
	    		details.addObject(row);
			}
    	} // main courses
    	if(extras != null) {
    		row = ((NSDictionary)application().valueForKeyPath(
					"strings.RujelCurriculum_Curriculum.Tabel.extraLessons")).mutableClone();
			row.takeValueForKey(extras,"values");
			details.insertObjectAtIndex(row, 2);
    	}
		row = ((NSDictionary)application().valueForKeyPath(
				"strings.RujelCurriculum_Curriculum.Tabel.subsTotal")).mutableClone();
		row.setObjectForKey(subsTotals, "values");
		String itemClass = (String)row.valueForKey("itemClass");
    	if(factorCounts != null) {
    		details.addObject(row);
    		row = ((NSDictionary)application().valueForKeyPath(
				"strings.RujelCurriculum_Curriculum.Tabel.varSub")).mutableClone();
			row.takeValueForKey(factorCounts,"values");
			details.addObject(row);
			appendCourses(byCourse, null, itemClass);
    	}
//    	quals.removeAllObjects();
//    	quals.addObject(qual);
    	quals[0] = teacherQual;
    	quals[1] = monthQual;
//    	quals.addObject(qual);
    	quals[1] = new EOAndQualifier(new NSArray(quals));
    	fs.setEntityName(Substitute.ENTITY_NAME);
    	fs.setQualifier(quals[1]);
    	list = new NSArray(new EOSortOrdering(
    			Substitute.FACTOR_KEY,EOSortOrdering.CompareDescending));
    	fs.setSortOrderings(list);
    	list = ec.objectsWithFetchSpecification(fs);
    	if(list == null || list.count() == 0)
    		return;
    	else if(factorCounts == null)
    		details.addObject(row);
		Enumeration enu = list.objectEnumerator();
		int month = ((Integer)currMonth.valueForKey("month")).intValue();
		BigDecimal currFactor = null;
       	NSArray sorter = new NSArray(new EOSortOrdering[] {
    			new EOSortOrdering("cycle",EOSortOrdering.CompareAscending),
    			new EOSortOrdering("eduGroup",EOSortOrdering.CompareAscending),
    			new EOSortOrdering("teacher.person",EOSortOrdering.CompareAscending),
    			new EOSortOrdering("comment",EOSortOrdering.CompareAscending)});
       	while (enu.hasMoreElements()) { //
			Substitute sub = (Substitute) enu.nextElement();
			BigDecimal factor = sub.factor();
			if(factor.compareTo(BigDecimal.ZERO) == 0)
				continue;
			cal.setTime(sub.lesson().date());
			if(month != cal.get(Calendar.MONTH))
				continue;
			if(!factor.equals(currFactor)) {
				if(byCourse.count() > 0)
					appendCourses(byCourse, sorter, itemClass);
				currFactor = factor;
				factorCounts = new BigDecimal[days + 1];
				row = new NSMutableDictionary("2","colspan");
				row.takeValueForKey("backfield2","class");
				row.takeValueForKey(sub.title() + " - " + factor.stripTrailingZeros()
						, "subject");
				row.takeValueForKey(factorCounts,"values");
				details.addObject(row);
			}
			addHoursToKey(byCourse, BigDecimal.ONE, cal, sub.lesson().course());
			addHoursToKey(null, BigDecimal.ONE, cal, factorCounts);
			addHoursToKey(null, factor, cal, subsTotals);
			addHoursToKey(null, factor, cal, totals);
		} // substitutes
		if(byCourse.count() > 0)
			appendCourses(byCourse, sorter, itemClass);
    }
    
    private void appendCourses(NSMutableDictionary byCourse,
    		NSArray sorter,String itemClass) {
    	NSArray list = byCourse.allKeys();
    	if(sorter == null)
    		list = EOSortOrdering.sortedArrayUsingKeyOrderArray(list, EduCourse.sorter);
    	else
    		list = EOSortOrdering.sortedArrayUsingKeyOrderArray(list, sorter);
    	Enumeration enu = list.objectEnumerator();
    	while (enu.hasMoreElements()) {
			EduCourse course = (EduCourse) enu.nextElement();
			NSMutableDictionary row = new NSMutableDictionary(course,"course");
			row.takeValueForKey(course.eduGroup().name(), "eduGroup");
			row.takeValueForKey(Person.Utility.fullName(course.teacher(), true, 2, 1, 1),
					(sorter==null)?"hover":"subject");
			if(sorter == null)
				row.takeValueForKey(course.subjectWithComment(),"subject");
			else
				row.takeValueForKey(course.cycle().subject(),"hover");
//			row.takeValueForKey(course.cycle().subject(),"hover");
			row.takeValueForKey(itemClass, "class");
    		row.setObjectForKey(byCourse.objectForKey(course), "values");
    		details.addObject(row);
		}
    	byCourse.removeAllObjects();
    }
    
	public WOActionResults selectTeacher() {
		WOComponent selector = TeacherSelector.selectorPopup(this, "currTeacher", ec);
		selector.takeValueForKeyPath(Boolean.TRUE, "dict.presenterBindings.hideVacant");
		return selector;
	}
	
	public String value() {
		if(item == null) {
			if(index!=null)
				return Integer.toString(index.intValue() + 1);
		}
		BigDecimal[] values = (BigDecimal[])((NSDictionary)item).valueForKey("values");
		if(values == null)
			return Integer.toString(index.intValue() + 1);
		BigDecimal value = values[(index == null)?0:index.intValue() +1];
		NSArray[] infos = (NSArray[])((NSDictionary)item).valueForKey("info");
		boolean info = (index != null && infos != null && infos[index.intValue() +1] != null
				&& infos[index.intValue() +1].count() > 0);
		if(value == null)
			return (info)?"*":null;
		if(value != null) {
			value = value.stripTrailingZeros();
			if(value.scale() < 0)
				value = value.setScale(0);
		}
		return (info)? value.toString() + '*' : value.toString();
	}
	
	public String cellHover() {
		if(item == null || index == null)
			return null;
		NSArray[] values = (NSArray[])((NSDictionary)item).valueForKey("info");
		if(values == null || values[index.intValue() +1] == null ||
				values[index.intValue() +1].count() == 0)
			return null;
		StringBuilder buf = new StringBuilder();
		Enumeration enu = values[index.intValue() +1].objectEnumerator();
		while (enu.hasMoreElements()) {
			Object obj = enu.nextElement();
			if(obj instanceof EduLesson)
				buf.append(Substitute.subsTitleForLesson((EduLesson)obj)).append('\n');
			else if(obj instanceof Variation) {
				Variation var = (Variation)obj;
				if(var.value().intValue() > 0)
					buf.append('+');
				buf.append(var.value()).append(" : ").append(var.reason().title()).append('\n');
			}
		}
		return WOMessage.stringByEscapingHTMLAttributeValue(buf.toString());
	}
	
	public String cellClass() {
		if(item == null || index == null)
			return null;
		NSArray[] values = (NSArray[])((NSDictionary)item).valueForKey("info");
		if(values != null && values[index.intValue() +1] != null &&
				values[index.intValue() +1].count() == 0)
			return "green";
		return null;
	}
	
	public String sum() {
		index = null;
		return value();
	}
	
	public WOActionResults openCourse() {
		Object course = item.valueForKey("course");
		if(course == null)
			return null;
		WOComponent page = pageWithName("LessonNoteEditor");
		page.takeValueForKey(course, "course");
		session().takeValueForKey(this, "pushComponent");
		return page;
	}
}