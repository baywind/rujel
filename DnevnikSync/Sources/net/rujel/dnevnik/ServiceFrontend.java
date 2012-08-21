package net.rujel.dnevnik;

import java.net.URL;
import java.rmi.RemoteException;
import java.util.Calendar;
import java.util.Enumeration;

import org.apache.axis.types.UnsignedByte;

import ru.mos.dnevnik.*;

import net.rujel.base.SettingsBase;
import net.rujel.eduplan.EduPeriod;
import net.rujel.interfaces.EduGroup;
import net.rujel.io.ExtBase;
import net.rujel.io.ExtSystem;
import net.rujel.io.SyncEvent;
import net.rujel.reusables.SettingsReader;
import net.rujel.ui.Progress;

import com.webobjects.appserver.WOActionResults;
import com.webobjects.appserver.WOContext;
import com.webobjects.appserver.WOComponent;
import com.webobjects.eoaccess.EOUtilities;
import com.webobjects.eocontrol.EOEditingContext;
import com.webobjects.eocontrol.EOEnterpriseObject;
import com.webobjects.eocontrol.EOFetchSpecification;
import com.webobjects.eocontrol.EOSortOrdering;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSDictionary;
import com.webobjects.foundation.NSForwardException;
import com.webobjects.foundation.NSMutableArray;
import com.webobjects.foundation.NSMutableDictionary;
import com.webobjects.foundation.NSTimestamp;

public class ServiceFrontend extends WOComponent {
	
	
	public EOEditingContext ec;
	public ExtSystem sync;
	public ImportServiceSoap soap;
	public String schoolGuid;
	public Integer year;

	public NSArray perGroups;
	public ReportingPeriodGroup pgr;
	public Object item;
	public NSArray errors;
	
	public NSArray events;
	public NSTimestamp since;
	public NSTimestamp to;
	public Integer limit;
	protected int timeShift = SettingsReader.intForKeyPath("dnevnik.timeZone", 4);

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
        events = SyncEvent.eventsForSystem(sync, null, 20, "marks");
        try {
        	String tmp = SettingsReader.stringForKeyPath("dnevnik.serviceURL", null);
        	URL serviceURL = new URL(tmp);
        	ImportServiceLocator locator = new  ImportServiceLocator();
        	soap = locator.getImportServiceSoap12(serviceURL);
        	perGroups = new NSArray(
        			soap.getReportingPeriodGroupCollection(schoolGuid, year.intValue()));
        } catch (Exception e) {
        	soap = null;
        	item = e;
//			throw new NSForwardException(e);
		}
    }
    
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
    	long ttID = Long.parseLong(timetable);
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
    	SyncEvent event = (SyncEvent)EOUtilities.createAndInsertInstance(
    			ec, SyncEvent.ENTITY_NAME);
    	event.setExtSystem(sync);
    	event.setSyncEntity("groups");
    	ec.saveChanges();
    	events = events.arrayByAddingObject(event);
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
		SyncEvent event = (SyncEvent)EOUtilities.createAndInsertInstance(
				ec, SyncEvent.ENTITY_NAME);
		event.setExtSystem(sync);
		event.setSyncEntity("timetable");
		ec.saveChanges();
//        events = SyncEvent.eventsForSystem(sync, null, 20, "marks");
		return null;
	}

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
		Thread thread = new Thread(sychroniser,"XMLGenerator");
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
}