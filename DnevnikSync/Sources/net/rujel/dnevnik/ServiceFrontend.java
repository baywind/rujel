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
import net.rujel.reusables.Various;

import com.webobjects.appserver.WOActionResults;
import com.webobjects.appserver.WOContext;
import com.webobjects.appserver.WOComponent;
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
	
	public NSArray events;
	
    public ServiceFrontend(WOContext context) {
        super(context);
        ec = new net.rujel.reusables.SessionedEditingContext(context.session());
        sync = (ExtSystem)ExtSystem.extSystemNamed("oejd.moscow", ec, false);
        schoolGuid = sync.extDataForKey("schoolGUID", null);
        year = (Integer)context.session().valueForKey("eduYear");
        events = SyncEvent.eventsForSystem(sync, null, 10);
        try {
        	String tmp = SettingsReader.stringForKeyPath("dnevnik.serviceURL", null);
        	URL serviceURL = new URL(tmp);
        	ImportServiceLocator locator = new  ImportServiceLocator();
        	soap = locator.getImportServiceSoap12(serviceURL);
        	perGroups = new NSArray(
        			soap.getReportingPeriodGroupCollection(schoolGuid, year.intValue()));
        } catch (Exception e) {
			throw new NSForwardException(e);
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
    	buf.append(cal.get(Calendar.DATE)).append('.');
    	buf.append(cal.get(Calendar.MONTH) +1).append('.');
    	buf.append(cal.get(Calendar.YEAR));
    }
    
	public WOActionResults syncPeriods() {
    	SettingsBase pb =  SettingsBase.baseForKey(EduPeriod.ENTITY_NAME, ec, false);
    	if(pb == null)
    		return null;
    	NSMutableArray regimes = pb.availableValues(
    			year, SettingsBase.TEXT_VALUE_KEY).mutableClone();
        try {
        	ReportingPeriodGroup[] rpgs = soap.getReportingPeriodGroupCollection(
        			schoolGuid, year.intValue());
        	if(rpgs == null)
        		return null;
        	NSMutableDictionary byName = new NSMutableDictionary();
        	NSMutableArray toDelete = null;
        	int timeShift = SettingsReader.intForKeyPath("ui.timeZone", 4);
        	for (int i = 0; i < rpgs.length; i++) {
        		String listName = rpgs[i].getName();
        		if(!regimes.removeObject(listName)) {
//        			soap.deleteReportingPeriodGroup(rpgs[i].getID());
        			if(toDelete == null)
        				toDelete = new NSMutableArray(new Long(rpgs[i].getID()));
        			else
        				toDelete.addObject(new Long(rpgs[i].getID()));
        			continue;
        		}
        		NSArray periods = EduPeriod.periodsInList(listName, ec);
/*        		int days = 0;
        		int min = 0;
        		int max = 0;
        		NSMutableDictionary byPer = new NSMutableDictionary();
        		Enumeration enu = periods.objectEnumerator();
        		while (enu.hasMoreElements()) {
					EduPeriod per = (EduPeriod) enu.nextElement();
					int length = EOPeriod.Utility.countDays(per.begin(), per.end());
					days += length;
					if(length < min || min == 0)
						min = length;
					if(length > max)
						max = length;
					byPer.setObjectForKey(new Integer(length), per);
				}*/
				ReportingPeriod[] pers = rpgs[i].getReportingPeriods();
        		if (pers.length != periods.count()) {
        			regimes.addObject(listName);
//        			soap.deleteReportingPeriodGroup(rpgs[i].getID());
        			soap.updateReportingPeriodGroup(rpgs[i].getID(), listName.concat(" (устар.)"));
        			if(toDelete == null)
        				toDelete = new NSMutableArray(new Long(rpgs[i].getID()));
        			else
        				toDelete.addObject(new Long(rpgs[i].getID()));
        			continue;
        		}
				Calendar dateStart = Calendar.getInstance();
				Calendar dateFinish = Calendar.getInstance();
				for (int j = 0; j < pers.length; j++) {
					EduPeriod per = (EduPeriod)periods.objectAtIndex(j);
					dateStart.setTime(per.begin());
					dateStart.set(Calendar.HOUR_OF_DAY, timeShift);
					dateFinish.setTime(per.end());
					dateFinish.set(Calendar.HOUR_OF_DAY, timeShift);
					soap.updateReportingPeriod(pers[j].getID(), dateStart, dateFinish);
				}
				byName.takeValueForKey(new Long(rpgs[i].getID()), listName);
			} // update or delete existing
        	if(regimes.count() > 0) {
				Calendar dateStart = Calendar.getInstance();
				Calendar dateFinish = Calendar.getInstance();
        		Enumeration enu = regimes.objectEnumerator();
        		while (enu.hasMoreElements()) {
					String listName = (String) enu.nextElement();
	        		NSArray periods = EduPeriod.periodsInList(listName, ec);
	        		EduReportingPeriodType type;
	        		switch (periods.count()) {
					case 4:
						type = EduReportingPeriodType.Quarter;
						break;
					case 3:
						type = EduReportingPeriodType.Trimester;
						break;
					case 2:
						EduPeriod per = (EduPeriod)periods.objectAtIndex(0);
						String name = per.name();
						int sem = Various.correlation(name, "семестр");
						int half = Various.correlation(name, "полугодие");
						if(sem > half)
							type = EduReportingPeriodType.Semester;
						else
							type = EduReportingPeriodType.HalfYear;
						break;
					case 1:
						type = EduReportingPeriodType.Year;
						break;
					default:
						type = EduReportingPeriodType.Module;
						break;
					}
	        		long pgrp = soap.insertReportingPeriodGroup(
	        				schoolGuid, listName, type, year);
	        		for (int j = 0; j < periods.count(); j++) {
						EduPeriod per = (EduPeriod) periods.objectAtIndex(j);
						dateStart.setTime(per.begin());
						dateStart.set(Calendar.HOUR, timeShift);
						dateFinish.setTime(per.end());
						dateFinish.set(Calendar.HOUR, timeShift);
						soap.insertReportingPeriod(pgrp, per.name(), new UnsignedByte(j),
								dateStart, dateFinish);
					}
					byName.takeValueForKey(new Long(pgrp), listName);
				} // regimes.objectEnumerator()
//        	}{
        		//sync groups
        		NSArray groups = EduGroup.Lister.listGroups(null, ec);
        		ExtBase base = ExtBase.localBase(ec);
        		NSDictionary dict = base.extSystem().dictForObjects(groups, base);
        		enu = groups.objectEnumerator();
        		String timetable = sync.extidForObject("ScheduleRing", new Integer(0), null);
        		Long ttID = (timetable == null)? null: new Long(timetable);
        		while (enu.hasMoreElements()) {
					EduGroup gr = (EduGroup) enu.nextElement();
					String groupGuid = (String)dict.objectForKey(gr);
					if(groupGuid == null)
						continue;
					String listName = pb.forObject(gr).textValue();
					Long pgrp = (Long)byName.objectForKey(listName);
					soap.updateGroup(groupGuid, schoolGuid, gr.name(), 
							new UnsignedByte(gr.grade()), year, pgrp, null, ttID);
				}
        	} // create regimes
        	if(toDelete != null) {
        		Enumeration enu = toDelete.objectEnumerator();
        		while (enu.hasMoreElements()) {
					Long id = (Long) enu.nextElement();
					soap.deleteReportingPeriodGroup(id.longValue());
				}
        	}
        	perGroups = new NSArray(
        			soap.getReportingPeriodGroupCollection(schoolGuid, year.intValue()));
        } catch (Exception e) {
			throw new NSForwardException(e);
		}
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
		return null;
	}
}