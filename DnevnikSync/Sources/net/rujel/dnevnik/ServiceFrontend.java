package net.rujel.dnevnik;

import java.io.File;
import java.io.FileInputStream;
import java.net.URL;
//import java.rmi.RemoteException;
//import java.util.Calendar;
import java.util.Enumeration;
import java.util.logging.LogManager;
import java.util.logging.Logger;

//import org.apache.axis.types.UnsignedByte;

import ru.mos.dnevnik.*;

import net.rujel.base.EntityIndex;
import net.rujel.base.MyUtility;
//import net.rujel.base.SettingsBase;
import net.rujel.contacts.Contact;
//import net.rujel.eduplan.EduPeriod;
import net.rujel.interfaces.EduGroup;
import net.rujel.interfaces.PerPersonLink;
import net.rujel.interfaces.Student;
import net.rujel.io.*;
import net.rujel.reusables.PlistReader;
import net.rujel.reusables.SettingsReader;
import net.rujel.reusables.WOLogFormatter;
import net.rujel.reusables.WOLogLevel;
import net.rujel.ui.Progress;
import net.rujel.ui.RedirectPopup;

import com.webobjects.appserver.WOActionResults;
import com.webobjects.appserver.WOContext;
import com.webobjects.appserver.WOComponent;
import com.webobjects.appserver.WOResponse;
import com.webobjects.eoaccess.EOUtilities;
import com.webobjects.eocontrol.*;
import com.webobjects.foundation.*;

public class ServiceFrontend extends WOComponent {
	
	
	public EOEditingContext ec;
	public ExtSystem sync;
	public ImportServiceSoap soap;
	public String schoolGuid;
	public Integer year;

//	public NSArray perGroups;
//	public ReportingPeriodGroup pgr;
	public Object item;
	public Object item2;
	public NSArray errors;
	
	public NSArray events;
	public NSTimestamp since;
	public NSTimestamp to;
	public Integer code = new Integer(33);
	public boolean exportAll;
	protected int timeShift = SettingsReader.intForKeyPath("dnevnik.timeZone", 4);
	protected PerPersonLink ppl;
	public Boolean sendAll;
	public String syncTime = SettingsReader.stringForKeyPath("dnevnik.syncTime", null);

	public Object active;

    public ServiceFrontend(WOContext context) {
        super(context);
        ec = new net.rujel.reusables.SessionedEditingContext(context.session());
        sync = (ExtSystem)ExtSystem.extSystemNamed("oejd.moscow", ec, false);
        if (sync == null) {
        	item = "OEJD not initialized";
        	return;
        }
        schoolGuid = sync.extDataForKey("schoolGUID", null);
        if (schoolGuid == null) {
        	item = "School GUID not defined";
        	return;
        }
        year = (Integer)context.session().valueForKey("eduYear");
        sendAll = SettingsReader.boolForKeyPath("dnevnik.sendAll", false);
        exportAll = sendAll;
        try {
        	String tmp = SettingsReader.stringForKeyPath("dnevnik.serviceURL", null);
        	URL serviceURL = new URL(tmp);
        	ImportServiceLocator locator = new  ImportServiceLocator();
        	soap = locator.getImportServiceSoap12(serviceURL);
//        	perGroups = new NSArray(
//        			soap.getReportingPeriodGroupCollection(schoolGuid, year.intValue()));
        } catch (Exception e) {
        	soap = null;
        	item = e;
//			throw new NSForwardException(e);
		}
        if(sendAll) {
        	active = "Оценки";
        	select();
        }
    }
    
    public WOActionResults select() {
    	if(ec.hasChanges())
    		ec.revert();
    	if(active == null) {
    		errors = null;
    		events = null;
    	} else if(active.equals("Оценки")) {
            events = SyncEvent.eventsForSystem(sync, null, 5, "marks");
            if(events == null || events.count() == 0) {
            	to =  new NSTimestamp();
            	since = to.timestampByAddingGregorianUnits(0, 0, -1, 0, 0, 0);
            } else {
            	SyncEvent last = (SyncEvent)events.objectAtIndex(0);
            	since = last.execTime();
            	to = since.timestampByAddingGregorianUnits(0, 0, 1, 0, 0, 0);
            }
/*    	} else if(active.equals("Периоды")) {
            events = SyncEvent.eventsForSystem(sync, null, 10, "!marks");
            if(soap != null) {
            	try {
            		perGroups = new NSArray(
            				soap.getReportingPeriodGroupCollection(schoolGuid, year.intValue()));
            	} catch (Exception e) {
            		Sychroniser.logger.log(WOLogLevel.WARNING,
            				"Error retrieving ReportingPeriodGroupCollection", 
            				new Object[] {session(), e});
            	}
            } */
    	} else if (active instanceof EduGroup) {
    		errors = null;
			events = ((EduGroup)active).list();
			EOEnterpriseObject contype = Contact.getType(ec, 
					OEJDUtiliser.class.getCanonicalName(), true);
			if(ec.globalIDForObject(contype).isTemporary()) {
				contype.takeValueForKey("ОЭЖД", "type");
				ec.saveChanges();
			}
			ppl = Contact.getContactsForList(events ,contype, Boolean.FALSE);
		}
    	return null;
    }
    
    public void clear() {
//    	if(active instanceof EduGroup) {
    		active = null;
    		errors = null;
    		events = null;
//    	}
    }
    
    public boolean isGroup() {
    	return (active instanceof EduGroup);
    }
    
    public String noTabClass() {
    	if(active == null)
    		return "selection";
    	else
    		return "grey";
    }
    /*
    public String periods() {
    	if(pgr == null)
    		return null;
    	ReportingPeriod[] pers = pgr.getReportingPeriods();
    	StringBuilder buf = new StringBuilder();
    	for (int i = 0; i < pers.length; i++) {
    		buf.append(pers[i].getNumber()).append(") ");
    		writeCal(pers[i].getDateStart(), buf);
    		buf.append(" - ");
    		writeCal(pers[i].getDateFinish(), buf);
    		buf.append("<br/>");
 		}
    	return buf.toString();
    }
    
    private void writeCal (Calendar cal, StringBuilder buf) {
    	if(cal == null) {
    		buf.append("?.?,?");
    		return;
    	}
    	buf.append(cal.get(Calendar.DATE)).append('.');
    	buf.append(cal.get(Calendar.MONTH) +1).append('.');
    	buf.append(cal.get(Calendar.YEAR));
    }
    
    private static final String[] PGnames = new String[] {
    	"Модуль", "Год", "Полугодие", "Триместр", "Четверть"};
    private static final EduReportingPeriodType[] PGtypes = new EduReportingPeriodType[] {
    	EduReportingPeriodType.Module, EduReportingPeriodType.Year, EduReportingPeriodType.HalfYear,
    	EduReportingPeriodType.Trimester, EduReportingPeriodType.Quarter};

    public WOActionResults syncPeriods() {
    	SettingsBase pb =  SettingsBase.baseForKey(EduPeriod.ENTITY_NAME, ec, false);
    	if(pb == null)
    		return null;
    	NSArray regimes = pb.availableValues(year, SettingsBase.TEXT_VALUE_KEY);
    	ReportingPeriodGroup[] rpgs = null;
    	try {
    		rpgs = soap.getReportingPeriodGroupCollection(
    				schoolGuid, year.intValue());
    		if(rpgs == null)
    			return null;
    	} catch (Exception e) {
    		throw new NSForwardException(e);
    	}
    	Enumeration enu = regimes.objectEnumerator();
    	NSMutableDictionary prepared = new NSMutableDictionary();
    	while (enu.hasMoreElements()) {
    		String listName = (String) enu.nextElement();
    		NSArray periods = EduPeriod.periodsInList(listName, ec);
    		if(periods == null || periods.count() == 0)
    			continue;
    		int count = periods.count();
    		String periodName = (count < 5)?PGnames[count]:PGnames[0];
    		ReportingPeriodGroup pg = (ReportingPeriodGroup)prepared.objectForKey(periodName);
    		if(pg != null) {
    			ReportingPeriod rp = pg.getReportingPeriods()[0];
    			EduPeriod per = (EduPeriod)periods.objectAtIndex(0);
    			Calendar cal = Calendar.getInstance();
    			cal.setTime(per.begin());
    			cal.add(Calendar.HOUR_OF_DAY, timeShift);
    			if(cal.before(rp.getDateStart())) {
    				rp.setDateStart(cal);
    				cal = Calendar.getInstance();
    			}
    			rp = pg.getReportingPeriods()[pg.getReportingPeriods().length -1];
    			per = (EduPeriod)periods.lastObject();
    			cal.setTime(per.end());
    			if(cal.after(rp.getDateFinish()))
    				rp.setDateFinish(cal);
    			continue;
    		} // extend periods if required
    		for (int i = 0; i < rpgs.length; i++) {
    			if(periodName.equalsIgnoreCase(rpgs[i].getName())) {	
    				pg = rpgs[i];
    				break;
    			}
    		}
    		ReportingPeriod[] pers;
    		if(pg == null) {
    			pers = new ReportingPeriod[(count > 4)?8:count];
    		} else {
    			pers = pg.getReportingPeriods();
    		}
			long fin = 0;
			for (int i = 0; i < pers.length; i++) {
				EduPeriod per = (i < count)?(EduPeriod)periods.objectAtIndex(i):null;
				Calendar dateStart;
				if(pers[i] != null)
					dateStart = pers[i].getDateStart();
				else
					dateStart = Calendar.getInstance();
				if(per == null) {
					dateStart.setTimeInMillis(fin);
					dateStart.add(Calendar.DATE, 1);
					fin = dateStart.getTimeInMillis();
				} else {
					dateStart.setTime(per.begin());
					dateStart.set(Calendar.HOUR_OF_DAY, timeShift);
				}
				Calendar dateFinish;
				if(pers[i] != null)
					dateFinish = pers[i].getDateFinish();
				else
					dateFinish = Calendar.getInstance();
				if(per == null) {
					dateFinish.setTimeInMillis(fin);
					dateFinish.add(Calendar.DATE, 1);
				} else {
					if(i == 7 && count > 8)
						per = (EduPeriod)periods.lastObject();
					dateFinish.setTime(per.end());
					dateFinish.set(Calendar.HOUR_OF_DAY, timeShift);
				}
				fin = dateFinish.getTimeInMillis();
				if(pers[i] == null) {
					UnsignedByte number = new UnsignedByte(i);
					pers[i] = new ReportingPeriod(0, number, dateStart, dateFinish);
				}
			}
			if(pg == null) {
				EduReportingPeriodType type = (count < 5)?PGtypes[count]:PGtypes[0];
    			pg = new ReportingPeriodGroup(0, periodName, type, pers);
			}
    		prepared.setObjectForKey(pg, periodName);
    	} // regimes.objectEnumerator()
    	enu = prepared.objectEnumerator();
    	while (enu.hasMoreElements()) {
    		ReportingPeriodGroup pg = (ReportingPeriodGroup) enu.nextElement();
    		try {
				ReportingPeriod[] pers = pg.getReportingPeriods();
    			if(pg.getID() == 0) {
    				pg.setID(soap.insertReportingPeriodGroup(
    						schoolGuid, pg.getName(), pg.getType(), year));
    				for (int i = 0; i < pers.length; i++) {
						pers[i].setID(soap.insertReportingPeriod(
								pg.getID(), pg.getName(), pers[i].getNumber(),
								pers[i].getDateStart(), pers[i].getDateFinish()));
					}
    			} else {
    				for (int i = 0; i < pers.length; i++) {
    					soap.updateReportingPeriod(pers[i].getID(), 
    							pers[i].getDateStart(), pers[i].getDateFinish());
    				}    				
    			}
    		} catch (Exception e) {
    			throw new NSForwardException(e);
    		}
    	}
    	try {
    		perGroups = new NSArray(
    				soap.getReportingPeriodGroupCollection(schoolGuid, year.intValue()));
        	SyncEvent.addEvent(sync, "periods");
    	} catch (Exception e) {
    		throw new NSForwardException(e);
    	}
    	return null;
    }
	
	public WOActionResults syncGroups() {
     	SettingsBase pb =  SettingsBase.baseForKey(EduPeriod.ENTITY_NAME, ec, false);
    	if(pb == null)
    		return null;
    	ReportingPeriodGroup[] rpgs;
        try {
        	rpgs = soap.getReportingPeriodGroupCollection(schoolGuid, year.intValue());
        } catch (Exception e) {
			return null;
		}

    	Object section = session().valueForKeyPath("state.section");
    	if(section != null)
    		session().takeValueForKeyPath(null,"state.section");
    	NSArray groups = EduGroup.Lister.listGroups(null, ec);
    	if(section != null)
    		session().takeValueForKeyPath(section,"state.section");
    	ExtBase base = ExtBase.localBase(ec);
    	NSDictionary dict = base.extSystem().dictForObjects(groups, base);
    	Enumeration enu = groups.objectEnumerator();
    	String timetable = sync.extidForObject("ScheduleRing", new Integer(0), null);
    	//TODO:require timetable here
    	Long ttID = (timetable == null)?null:new Long(timetable);
    	while (enu.hasMoreElements()) {
    		EduGroup gr = (EduGroup) enu.nextElement();
    		String groupGuid = (String)dict.objectForKey(gr);
    		if(groupGuid == null)
    			continue;
    		long pgrp = 0;
    		String listName = pb.forObject(gr).textValue();
    		NSArray periods = EduPeriod.periodsInList(listName, ec);
    		int count = (periods == null)?4:periods.count();
    		String periodName = (count < 5)?PGnames[count]:PGnames[0];
    		if(listName != null) {
    			for (int i = 0; i < rpgs.length; i++) {
					if(periodName.equalsIgnoreCase(rpgs[i].getName())) {
						pgrp = rpgs[i].getID();
						break;
					}
				}
    		}
    		try {
    			soap.updateGroup(groupGuid, schoolGuid, gr.name(), 
    					new UnsignedByte(gr.grade()), year, pgrp, null, ttID);
    		} catch (RemoteException e) {
    			throw new NSForwardException(gr.name() + ' ' + groupGuid, e);
    		}
    	}
    	events = events.arrayByAddingObject(SyncEvent.addEvent(sync, "groups"));
    	return null;
	}
    
	public WOActionResults syncTimetable() {
		NSArray list = new NSArray(new EOSortOrdering[] {
				new EOSortOrdering("timeScheme", EOSortOrdering.CompareAscending),
				new EOSortOrdering("num", EOSortOrdering.CompareAscending)
		});
		EOFetchSpecification fs = new EOFetchSpecification("ScheduleRing", null, list);
		list = ec.objectsWithFetchSpecification(fs);
		if(list == null || list.count() == 0)
			return null;
		Integer timeScheme = null;
		Enumeration enu = list.objectEnumerator();
		
		NSMutableArray<DayTimeTableItem> lessons = new NSMutableArray<DayTimeTableItem>();
		Calendar cal = Calendar.getInstance();
		while (enu.hasMoreElements()) {
			EOEnterpriseObject entry = (EOEnterpriseObject) enu.nextElement();
			if(!entry.valueForKey("timeScheme").equals(timeScheme)) {
				if(lessons.count() != 0 ) {
					DayTimeTable dtt = new DayTimeTable(lessons.objects());
					DayTimeTable[] week = new DayTimeTable[] {dtt,dtt,dtt,dtt,dtt,dtt};
					String extid = sync.extidForObject("TimeScheme", timeScheme, null);
					try{
						String name = "Расписание " + timeScheme;
						if(extid == null) {
							long id = soap.insertTimeTable(schoolGuid, name, week);
							extid = Long.toString(id);
							sync.addMatch("TimeScheme", timeScheme, null).setExtID(extid);
							ec.saveChanges();
						} else {
							soap.updateTimeTable(Long.parseLong(extid),name, week);
						}
					} catch (RemoteException e) {
						throw new NSForwardException(e);
					}
					lessons.removeAllObjects();
				}
				timeScheme = (Integer)entry.valueForKey("timeScheme");
			}
			int startHours, startMinutes, finishHours, finishMinutes;
			cal.setTime((NSTimestamp)entry.valueForKey("startTime"));
			startHours = cal.get(Calendar.HOUR_OF_DAY);
			startMinutes = cal.get(Calendar.MINUTE);
			cal.setTime((NSTimestamp)entry.valueForKey("endTime"));
			finishHours = cal.get(Calendar.HOUR_OF_DAY);
			finishMinutes = cal.get(Calendar.MINUTE);
			UnsignedByte lessonNumber = new UnsignedByte((Integer)entry.valueForKey("num"));
			DayTimeTableItem lesson = new DayTimeTableItem(lessonNumber, 
					startHours, startMinutes, finishHours, finishMinutes);
			lessons.addObject(lesson);
		}
		if(lessons.count() != 0 ) {
			DayTimeTableItem[] dtis = lessons.toArray(new DayTimeTableItem[lessons.count()]);
			DayTimeTable dtt = new DayTimeTable(dtis);
			DayTimeTable[] week = new DayTimeTable[] {dtt,dtt,dtt,dtt,dtt,dtt};
			String extid = sync.extidForObject("ScheduleRing", timeScheme, null);
			try{
				String name = "Расписание " + timeScheme;
				if(extid == null) {
					long id = soap.insertTimeTable(schoolGuid, name, week);
					extid = Long.toString(id);
					sync.addMatch("ScheduleRing", timeScheme, null).setExtID(extid);
					ec.saveChanges();
				} else {
					soap.updateTimeTable(Long.parseLong(extid),name, week);
				}
			} catch (RemoteException e) {
				throw new NSForwardException(e);
			}
		}
		SyncEvent.addEvent(sync, "timetable");
		ec.saveChanges();
//        events = SyncEvent.eventsForSystem(sync, null, 20, "marks");
		return null;
	} */

	public WOActionResults dateFromEvent() {
		if(!(item instanceof SyncEvent))
			return null;
		SyncEvent event = (SyncEvent)item;
		since = event.execTime();
		return null;
	}
	
	public WOActionResults syncMarks() {
		ec.saveChanges();
		Sychroniser sychroniser = new Sychroniser(ec, year);
		sychroniser.system = sync;
		sychroniser.schoolGuid = schoolGuid;
		sychroniser.soap = soap;
		sychroniser.since = since;
		sychroniser.to = to;
		sychroniser.state = new Progress.State();
//		sychroniser.syncChanges(since, to, limit);
//        events = SyncEvent.eventsForSystem(sync, null, 10); 
//		return null;
		Thread thread = new Thread(sychroniser,"OEJD_Sync");
		thread.setPriority(Thread.MIN_PRIORITY + 1);
		thread.start();

		Progress progress = (Progress)pageWithName("Progress");
		progress.returnPage = this;
		progress.resultPath = "errors";
		progress.title = "Загрузка данных в ОЭЖД";
		progress.state = sychroniser.state;
		
		return progress.refresh();
	}

	public void setErrors(NSArray errors) {
		this.errors = errors;
        events = SyncEvent.eventsForSystem(sync, null, 20, "marks");
	}
	
	public NSArray contacts() {
		if(item instanceof Student) {
			NSArray contacts = (ppl==null)?null:(NSArray)ppl.forPersonLink((Student)item);
			if(contacts == null || contacts.count() == 0)
				contacts = new NSArray(new NSMutableDictionary());
			return contacts;
		}
		return null;
	}

	public Boolean studentActive() {
		if(item2 instanceof Contact) {
			return Boolean.valueOf(((Contact)item2).flags() > 0);
		} else if (item2 instanceof NSDictionary) {
			return Boolean.FALSE;
		}
		return null;
	}

	public void setStudentActive(Boolean studentActive) {
		if(item2 instanceof Contact) {
			Contact cnt = (Contact)item2;
			boolean was = cnt.flags() > 0;
			if(studentActive != was) {
				cnt.setFlags(new Integer((studentActive)?1:0));
			}
			if(!studentActive) {
				NSArray contacts = (ppl==null)?null:(NSArray)ppl.forPersonLink((Student)item);
				if(contacts != null && contacts.count() > 1)
					ec.deleteObject(cnt);
			}
		} else if (item2 instanceof NSDictionary) {
			NSMutableDictionary dict = (NSMutableDictionary)item2;
			dict.takeValueForKey(item, "person");
			dict.takeValueForKey(new Integer(1), "flags");
			if(errors == null)
				errors = new NSMutableArray(dict);
			else
				((NSMutableArray)errors).addObject(dict);
		}
	}
	
	public String fieldStyle() {
		if(studentActive() == Boolean.TRUE)
			return null;
		return "visibility:hidden;";
	}
	
	public WOActionResults saveContacts() {
		EOEnterpriseObject contype = Contact.getType(ec, 
				OEJDUtiliser.class.getCanonicalName(), true);
		if(errors != null && errors.count() > 0) {
			Enumeration enu = errors.objectEnumerator();
			while (enu.hasMoreElements()) {
				NSMutableDictionary dict = (NSMutableDictionary) enu.nextElement();
				Contact cnt = (Contact)EOUtilities.createAndInsertInstance(ec, Contact.ENTITY_NAME);
				cnt.setType(contype);
				cnt.takeValuesFromDictionary(dict);
			}
			errors = null;
		}
		if(!ec.hasChanges())
			return null;
		try {
			ec.saveChanges();
			ppl = Contact.getContactsForList(events ,contype, Boolean.FALSE);
		} catch (Exception e) {
			Logger.getLogger("rujel.dnevnik").log(WOLogLevel.WARNING,
					"Failed to save EOJD contacts",new Object[] {session(),active,e});
			session().takeValueForKey(e.getMessage(), "message");
			ec.revert();
		}
		return null;
	}

	public WOActionResults export() {
		NSDictionary reporter = (NSDictionary)PlistReader.readPlist(
				"oejdCSV.plist", "DnevnikSync", null);
		reporter.takeValueForKeyPath(code, "settings.byContact");
		if(!exportAll)
			reporter.takeValueForKeyPath(Boolean.TRUE, "settings.limitExport");
		if(active == null) {
    		WOComponent resultp = pageWithName("ExportParams");
    		if(!sendAll) {
				Enumeration enu = activeContacts();
				if(enu != null) {
					NSMutableDictionary preload = new NSMutableDictionary(Boolean.TRUE,"PRELOADED");
					preload.takeValueForKey("contactFlags", "paramKey");
					while (enu.hasMoreElements()) {
						Contact cnt = (Contact) enu.nextElement();
						Student student = (Student)cnt.person();
						int flags = cnt.flags().intValue();
						if(cnt.contact() != null)
							flags += 32;
						preload.setObjectForKey(flags, student);
					}
					preload(preload, reporter);
				}
    		}
    		reporter.takeValueForKey("item", "resultPath");
    		resultp.takeValueForKey(this, "returnPage");
    		resultp.takeValueForKey(reporter, "reporter");
    		return resultp;
		}
 		if(ec.hasChanges()) {
 			try {
 				ec.saveChanges();
 			} catch (Exception e) {
 				session().takeValueForKey(e.getMessage(), "message");
 				return RedirectPopup.getRedirect(context(), this);
 			}
 		}
	   	NSMutableDictionary reportDict = new NSMutableDictionary();
		reportDict.takeValueForKey(reporter,"reporter");
		reportDict.takeValueForKey(session().valueForKeyPath("state.section"), "section");
		reportDict.takeValueForKey(ec,"ec");
		reportDict.takeValueForKey(sync.getIndexes(null), "indexes");
		reportDict.takeValueForKey("ImportExport", "reportDir");
		reportDict.takeValueForKey(session().valueForKey("today"), "today");

		NSMutableDictionary info = new NSMutableDictionary(MyUtility.presentEduYear(
				(Integer)session().valueForKey("eduYear")), "eduYear");
		info.takeValueForKey(sync.getDataDict(null), "extraData");
		reportDict.takeValueForKey(info, "info");
		
		NSMutableDictionary preload = new NSMutableDictionary(Boolean.TRUE,"PRELOADED");
		preload.takeValueForKey("contactFlags", "paramKey");
		if(active instanceof EduGroup) {
			reportDict.takeValueForKey(active, "eduGroup");
			Enumeration enu = events.objectEnumerator();
			NSArray students = (exportAll)?events:new NSMutableArray();
			while (enu.hasMoreElements()) {
				Student student = (Student) enu.nextElement();
				NSArray contacts = (ppl==null)?null:(NSArray)ppl.forPersonLink(student);
				if(contacts != null && contacts.count() > 0) {
					Contact cnt = (Contact)contacts.objectAtIndex(0);
					int flags = cnt.flags().intValue();
					if(!exportAll && flags != 0)
						((NSMutableArray)students).addObject(student);
					if(cnt.contact() != null)
						flags += 32;
					preload.setObjectForKey(flags, student);
				}
			}
			reportDict.takeValueForKey(students, "students");
		} else  {
			Enumeration enu = activeContacts();
			if(enu != null) {
				NSMutableArray students = (exportAll)?null:new NSMutableArray();
				while (enu.hasMoreElements()) {
					Contact cnt = (Contact) enu.nextElement();
					Student student = (Student)cnt.person();
					int flags = cnt.flags().intValue();
					if(!exportAll && flags != 0 && !students.containsObject(student))
						((NSMutableArray)students).addObject(student);
					if(cnt.contact() != null)
						flags += 32;
					preload.setObjectForKey(flags, student);
				}
				reportDict.takeValueForKey(students, "students");
			}
		}
		if(preload.count() > 1)
			preload(preload, reporter);
		
		Progress progress = (Progress)pageWithName("Progress");
		progress.returnPage = this;
		progress.resultPath = "item";
		progress.title = (String)reporter.valueForKey("title");
		progress.state = XMLGenerator.backgroundGenerate(reportDict);
		return progress.refresh();
	}
	
	public String onLoad() {
		if(item instanceof byte[]) {
			session().setObjectForKey(item, "download");
			return "window.location=globalActionUrl;";
		}
		return null;
	}
	
	public WOActionResults download() {
		item = session().objectForKey("download");
		if(!(item instanceof byte[]))
			return null;
		WOResponse response = application().createResponseInContext(context());
		response.setContent((byte[])item);
		response.setHeader("application/octet-stream","Content-Type");
		if(active instanceof EduGroup) {
			StringBuilder buf = new StringBuilder("attachment; filename=\"persdata");
			EduGroup gr = (EduGroup)active;
			buf.append(gr.grade()).append('-').append(gr.title()).append(".csv\"");
			response.setHeader(buf.toString(),"Content-Disposition");
		} else {
			response.setHeader("attachment; filename=\"persdata.csv\"","Content-Disposition");
		}
		item = null;
		session().removeObjectForKey("download");
		return response;
	}
	
	protected Enumeration activeContacts() {
		EOEnterpriseObject contype = Contact.getType(ec, 
				OEJDUtiliser.class.getCanonicalName(), false);
		EntityIndex ent = EntityIndex.indexForEntityName(ec, Student.entityName, false);
		if(contype == null)
			return null;
		NSArray contacts = EOUtilities.objectsWithQualifierFormat(ec, Contact.ENTITY_NAME, 
				"type = %@ AND flags >= 1 AND personEntity = %@", 
				new NSArray(new Object[] {contype, ent}));
		if(contacts == null || contacts.count() == 0)
			return null;
		return contacts.objectEnumerator();
		/*
		Enumeration enu = contacts.objectEnumerator();
		NSMutableArray students = new NSMutableArray();
		while (enu.hasMoreElements()) {
			Contact cnt = (Contact) enu.nextElement();
			Student student = (Student)cnt.person();
			if(!students.containsObject(student))
				students.addObject(student);
		}
		return students;*/
	}
	
	protected void preload(NSMutableDictionary preload, NSDictionary reporter) {
		NSMutableDictionary sdict = (NSMutableDictionary)reporter.valueForKey("sync");
		if(sdict == null) {
			sdict = new NSMutableDictionary();
			reporter.takeValueForKey(sdict, "sync");
		}
		Object forTag = sdict.valueForKey("person");
		if(forTag == null) {
			forTag = preload;
		} else {
			if (forTag instanceof NSArray) {
				if(!(forTag instanceof NSMutableArray))
					forTag = ((NSMutableArray)forTag).mutableClone();
			} else {
				forTag = new NSMutableArray(forTag);
			}
			((NSMutableArray)forTag).addObject(preload);
		}
		sdict.takeValueForKey(forTag, "person");
	}
	
	public WOActionResults logFile() {
		String logPath = System.getProperty("WOOutputPath");
		if(logPath == null)
			logPath = LogManager.getLogManager().getProperty(
					"java.util.logging.FileHandler.pattern");
		logPath = NSPathUtilities.stringByDeletingLastPathComponent(logPath);
		String filename = SettingsReader.stringForKeyPath("dnevnik.logFile", "OEJDsync.log");
		File log = new File(logPath, filename);
		WOResponse response = application().createResponseInContext(context());
		response.setHeader("application/octet-stream","Content-Type");
		response.setHeader("attachment; filename=\"" + filename + "\"","Content-Disposition");

		try {
			response.setContentStream(new FileInputStream(log),4096,log.length());
		} catch (Exception e) {
			response.setContent(WOLogFormatter.formatTrowable(e));
		}
		response.disableClientCaching();
		return response;
	}
}