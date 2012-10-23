package net.rujel.dnevnik;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URL;
import java.rmi.RemoteException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.UUID;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import javax.xml.rpc.ServiceException;

import org.apache.axis.types.UnsignedByte;

import ru.mos.dnevnik.*;

import net.rujel.base.BaseCourse;
import net.rujel.base.BaseLesson;
import net.rujel.base.EntityIndex;
import net.rujel.base.IndexRow;
import net.rujel.base.Indexer;
import net.rujel.base.MyUtility;
import net.rujel.base.SettingsBase;
import net.rujel.contacts.Contact;
import net.rujel.criterial.CriteriaSet;
import net.rujel.criterial.Work;
import net.rujel.criterial.WorkType;
import net.rujel.eduplan.Subject;
import net.rujel.eduresults.ItogContainer;
import net.rujel.interfaces.EduCourse;
import net.rujel.interfaces.EduCycle;
import net.rujel.interfaces.EduGroup;
import net.rujel.interfaces.EduLesson;
import net.rujel.interfaces.Person;
import net.rujel.interfaces.Student;
import net.rujel.io.ExtBase;
import net.rujel.io.ExtSystem;
import net.rujel.io.SyncEvent;
import net.rujel.io.SyncIndex;
import net.rujel.io.SyncMatch;
import net.rujel.markarchive.MarkArchive;
import net.rujel.reusables.DataBaseConnector;
import net.rujel.reusables.NamedFlags;
import net.rujel.reusables.SettingsReader;
import net.rujel.reusables.Various;
import net.rujel.reusables.WOLogLevel;
import net.rujel.ui.Progress;

import com.webobjects.appserver.WOApplication;
import com.webobjects.appserver.WOMessage;
import com.webobjects.appserver.WOSession;
import com.webobjects.eoaccess.EOUtilities;
import com.webobjects.eocontrol.*;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSDictionary;
import com.webobjects.foundation.NSLocking;
import com.webobjects.foundation.NSMutableArray;
import com.webobjects.foundation.NSMutableDictionary;
import com.webobjects.foundation.NSPathUtilities;
import com.webobjects.foundation.NSTimestamp;

public class Sychroniser implements Runnable {
	
	public static class AttributedRemoteException extends RemoteException {
		private Object[] att;
		public AttributedRemoteException(RemoteException cause, Object[] attributes) {
			super(cause.getMessage(),cause);
			att = attributes;
		}
		public Object[] attributes() {
			return att;
		}
	}
	
	public static final UnsignedByte ZERO = new UnsignedByte(0);
	public static final UnsignedByte ONE = new UnsignedByte(1);
	public static final Logger logger = Logger.getLogger("rujel.dnevnik");
	
	private EOEditingContext ec;
	public Integer eduYear;
	public NSTimestamp since, to, batch1, batch2;
	public ExtSystem system;
	public ExtBase localBase;
	public String schoolGuid;
	public Progress.State state;
	
	protected NSDictionary workTypes;
	protected NSDictionary itogTypes;
	protected BufferedWriter logWriter;
	
	private EntityIndex lessonEI, workEI, timeslotEI, courseEI;
	
	protected ImportServiceSoap soap;
	
	protected SettingsBase criteriaSettings;
	protected NSMutableDictionary critSetParams;
	protected int timeShift = SettingsReader.intForKeyPath("dnevnik.timeZone", 4);
	protected EOEnterpriseObject contype;
	protected EntityIndex studentEI;
	
	public Sychroniser(EOEditingContext ec, Integer eduYear) {
		super();
		this.ec = ec;
		this.eduYear = eduYear;
		if(!SettingsReader.boolForKeyPath("dnevnik.sendAll", false)) {
			contype = Contact.getType(ec, OEJDUtiliser.class.getCanonicalName(), false);
			studentEI = EntityIndex.indexForEntityName(ec, Student.entityName,false);
		}
	}
	
	public static void syncToMoment(NSTimestamp moment) throws IOException, ServiceException {
		Integer eduYear = (Integer)WOApplication.application().valueForKey("year");
		if(moment == null)
			moment = new NSTimestamp();
		EOEditingContext ec;
		if(SettingsReader.boolForKeyPath("dbConnection.yearTag", false)) {
			EOObjectStore os = DataBaseConnector.objectStoreForTag(eduYear.toString());
			ec = new EOEditingContext(os);
		} else {
			ec = new EOEditingContext();
		}
		Sychroniser sychroniser = new Sychroniser(ec, eduYear);
		sychroniser.system = (ExtSystem)ExtSystem.extSystemNamed("oejd.moscow", ec, false);
        if (sychroniser.system == null)
        	throw new IllegalStateException("OEJD not initialized");
		sychroniser.schoolGuid = sychroniser.system.extDataForKey("schoolGUID", null);
        if (sychroniser.schoolGuid == null)
        	throw new IllegalStateException("School GUID not defined");
    	String tmp = SettingsReader.stringForKeyPath("dnevnik.serviceURL", null);
    	URL serviceURL = new URL(tmp);
    	ImportServiceLocator locator = new  ImportServiceLocator();
    	sychroniser.soap = locator.getImportServiceSoap12(serviceURL);
		sychroniser.to = moment;
        NSArray events = SyncEvent.eventsForSystem(sychroniser.system, null, 1, "marks");
        if(events != null && events.count() > 0) {
        	SyncEvent last = (SyncEvent)events.objectAtIndex(0);
        	sychroniser.since = last.execTime();
        }
		Thread thread = new Thread(sychroniser,"OEJD_Sync");
		thread.setPriority(Thread.MIN_PRIORITY + 1);
		thread.start();
	}
	
	protected static final SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	public void run() {
		WOSession ses = MyUtility.dummyContext(null).session();
		try {
			ec = ses.defaultEditingContext();
			system = (ExtSystem)EOUtilities.localInstanceOfObject(ec, system);
			localBase = null;
			lessonEI = null;
			timeslotEI = null;
			workEI = null;
			critSetParams = new NSMutableDictionary();
			String logPath = System.getProperty("WOOutputPath");
			if(logPath == null)
				logPath = LogManager.getLogManager().getProperty(
						"java.util.logging.FileHandler.pattern");
			logPath = NSPathUtilities.stringByDeletingLastPathComponent(logPath);
			logPath = NSPathUtilities.stringByAppendingPathComponent(logPath, 
					SettingsReader.stringForKeyPath("dnevnik.logFile", "OEJDsync.log"));
			try {
				logWriter = new BufferedWriter(new FileWriter(logPath, true),1024);
				logWriter.newLine();
				logWriter.write("Sync started: ");
				logWriter.write(ses.sessionID());
				logWriter.write(' ');
				logWriter.write(df.format(new Date()));
				logWriter.newLine();
			} catch (Exception e) {
				logger.log(WOLogLevel.INFO,"Failed to initialize OEJD sync log",
						new Object[] {ses,e,logPath});
			}
			NSArray result = syncChanges();
			if(state != null) {
				synchronized (state) {
					state.result = result;
					state.current = state.total;
				}
			}
			ses.terminate();
			logWriter.write("Sync finished: ");
			logWriter.write(ses.sessionID());
			logWriter.write(' ');
			logWriter.write(df.format(new Date()));
			logWriter.newLine();
			logWriter.flush();
		} catch (Exception e) {
			if(state != null) {
				synchronized (state) {
//					state.total = -state.total;
					state.result = e;
				}
			}
			logger.log(WOLogLevel.WARNING, 
					"Dnevnik sync failed", new Object[]{ses,e});
		}
	}

	public static NSArray archSorter = new NSArray(
			new EOSortOrdering(MarkArchive.TIMESTAMP_KEY, EOSortOrdering.CompareAscending));
	public NSArray syncChanges() {
		if(system == null)
			system = (ExtSystem)ExtSystem.extSystemNamed("oejd.moscow", ec, true);
		system.setCachesIndexes(true);
		if(localBase == null)
			localBase = ExtBase.localBase(ec);
		if(schoolGuid == null)
			schoolGuid = localBase.extSystem().extDataForKey("schoolGUID", null);
		localBase.extSystem().setCachesIndexes(true);
		if(ec.hasChanges())
			ec.saveChanges();
		EOQualifier qual = null;
		NSMutableArray errors = new NSMutableArray();
		if(since == null) {
			if (to != null)
				qual = new EOKeyValueQualifier(MarkArchive.TIMESTAMP_KEY, 
					EOQualifier.QualifierOperatorLessThanOrEqualTo, to);
			syncForQual(qual, errors);
		} else {
			batch1 = since;
			long fin = (to == null)?System.currentTimeMillis():to.getTime();
			long per =  (fin - batch1.getTime());
			int count = (int) (per/NSLocking.OneDay);
			if(per%NSLocking.OneDay > NSLocking.OneHour)
				count++;
			while(batch1.getTime() < fin) {
				if(count > 1 && state != null) {
					synchronized (state) {
						if(state.shouldStop()) {
							state.name = "Stopped.";
							return (errors.count() == 0)?null:errors;
						}
						state.total = count;
						state = state.createSub();
					}
				}
				if(fin - batch1.getTime() <= (NSLocking.OneDay + NSLocking.OneHour)) {
					batch2 = to;
				} else {
					batch2 = batch1.timestampByAddingGregorianUnits(0, 0, 1, 0, 0, 0);
				}
				if(batch2 == null)
					qual = new EOKeyValueQualifier(MarkArchive.TIMESTAMP_KEY, 
							EOQualifier.QualifierOperatorGreaterThan, batch1);
				else
					qual = EOQualifier.qualifierWithQualifierFormat(
							"timestamp > %@ AND timestamp <= %@",
							new NSArray(new Object[] {batch1,batch2}));
				syncForQual(qual, errors);
				batch1 = batch2;
				if(count > 1 && state != null) {
					state = state.end();
				}
			}
			if(to == null)
				qual = new EOKeyValueQualifier(MarkArchive.TIMESTAMP_KEY, 
						EOQualifier.QualifierOperatorGreaterThan, since);
		}
//		if(limit != null)
//			fs.setFetchLimit(limit.intValue());
		if(errors.count() == 0)
			return null;
		return errors;
	}
	
	private void syncForQual(EOQualifier qual, NSMutableArray errors) {
		EOFetchSpecification fs = new EOFetchSpecification(MarkArchive.ENTITY_NAME,
				qual,archSorter);
		fs.setPrefetchingRelationshipKeyPaths(new NSArray(MarkArchive.USED_ENTITY_KEY));
		NSArray list = ec.objectsWithFetchSpecification(fs);
		if(list == null || list.count() == 0)
			return;
		if(state != null) {
			synchronized (state) {
				if(state.shouldStop()) {
					state.name = "Stopped.";
					return;
				}
				state.total = list.count();
			}
		}
		NSTimestamp last = null;
		SyncEvent event = null;
		for (int idx = 0; idx < list.count(); idx++) {
			MarkArchive arch = (MarkArchive) list.objectAtIndex(idx);
			if(state != null) {
				synchronized (state) {
					if(state.shouldStop()) {
						state.name = "Stopped.";
						return;
					}
					state.current = idx;
					state.name = df.format(arch.timestamp());
				}
			}
			String entity = (String)arch.valueForKeyPath("usedEntity.usedEntity");
			try {
				if("BaseLesson".equals(entity))
					syncLesson(arch);
				else if("BaseNote".equals(entity))
					syncNote(arch);
				else if("Work".equals(entity))
					syncWork(arch);
				else if("Mark".equals(entity))
					syncMark(arch);
				else if("ItogMark".equals(entity))
					syncItogMark(arch);
				last = arch.timestamp();
				if(event == null) {
					event = (SyncEvent)EOUtilities.createAndInsertInstance(
							ec, SyncEvent.ENTITY_NAME);
					event.setExtSystem(system);
					event.setSyncEntity("marks");
				}
				event.setExecTime(last);
//				event.setResult(1);
				ec.saveChanges();
			} catch (Exception e) {
				Object[] attributes = null;
				if(e instanceof AttributedRemoteException) {
					attributes = ((AttributedRemoteException)e).attributes();
					e = (RemoteException)e.getCause();
				}
				NSMutableDictionary dict = new NSMutableDictionary();
				String id = MyUtility.getID(arch);
				dict.takeValueForKey(id, "archID");
				dict.takeValueForKey(e, "exception");
				StackTraceElement trace[] = e.getStackTrace();
				String method = null;
				for (int i = 0; i < trace.length; i++) {
					if(trace[i].getClassName().contains("ImportServiceSoap")) {
						method = trace[i].getMethodName();
						break;
					}
				}
				if(method == null)
					method = trace[0].getMethodName();
				dict.takeValueForKey(method, "method");
				StringBuilder msg = new StringBuilder(method);
				if(attributes != null) {
					msg.append('(');
					for (int i = 0; i < attributes.length; i++) {
						if(i > 0)
							msg.append(',').append(' ');
						if(attributes[i] instanceof Calendar) {
							Calendar cal = (Calendar)attributes[i];
							NSTimestamp date = new NSTimestamp(cal.getTimeInMillis());
							msg.append(MyUtility.dateFormat().format(date));
						} else {
							msg.append(attributes[i]);
						}
					}
					msg.append(')');
					method = msg.toString();
					dict.takeValueForKey(method, "method");
				}
				msg.append(':').append(' ');
				msg.append(WOMessage.stringByEscapingHTMLString(e.getMessage()));
				if(state != null) {
					synchronized (state) {
						state.addMessage(msg.toString());
					}
				}
				errors.addObject(dict);
				if(e instanceof RemoteException) {
					if(logWriter != null) {
						try {
							logWriter.write(id);
							logWriter.write('|');
							logWriter.write(method);
							logWriter.write(' ');
							logWriter.write(e.toString());
							logWriter.newLine();
							logWriter.flush();
						} catch (Exception ioe) {
						}
					}
				} else {
					logger.log(WOLogLevel.WARNING, "Error during Dnevnik sync.", new Object[]{e});
				}
//			} catch (Exception e) {
//				throw new NSForwardException(e);
			}
		}
		if(ec.hasChanges())
			ec.saveChanges();
	}
	
	private SyncMatch getMatch(EntityIndex ei, Integer objID) {
		EOQualifier qual = SyncMatch.matchQualifier(system, null, ei, objID, eduYear);
		EOFetchSpecification fs = new EOFetchSpecification(SyncMatch.ENTITY_NAME,qual,null);
		NSArray found = ec.objectsWithFetchSpecification(fs);
		if(found != null && found.count() > 0)
			return (SyncMatch)found.objectAtIndex(0);
		return null;
	}
	
	private long subjectID (EduCourse course) throws RemoteException {
		Subject subj = (Subject)course.valueForKeyPath("cycle.subjectEO");
		return subjectID(subj);
	}
	private long subjectID (Subject subj) throws RemoteException {
		String subjExtid = system.extidForObject(subj, null);
		if(subjExtid == null) {
			ru.mos.dnevnik.Subject[] subjs = soap.getSubjectCollection(schoolGuid);

//			NSDictionary dict = (NSDictionary)WOApplication.application().valueForKeyPath(
//					"strings.DnevnikSync_Dnevnik.subjectsDict");
//			Enumeration enu = dict.keyEnumerator();
			float best = 0;
			long preset = 0;
			String subject = subj.subject();
			String full = subj.fullName();
			for (int i = 0; i < subjs.length; i++) {
				String name = subjs[i].getName();
				if(name.equalsIgnoreCase(subject) || name.equalsIgnoreCase(full)) {
					preset = subjs[i].getID();
					best = 1;
					break;
				}
				int correlation = Various.correlation(name, subject);
				float relative = ((float)correlation)/
						(Math.max(name.length(), subject.length()) -1);
				if(relative > best) {
					preset = subjs[i].getID();
					best = relative;
				}
				if(full != null) {
					correlation = Various.correlation(name, full);
					relative = ((float)correlation)/
							(Math.max(name.length(), full.length()) -1);
					if(relative > best) {
						preset = subjs[i].getID();
						best = relative;
					}
				}
			} // test presets
			if(best > 0.75) {
				SyncMatch match = system.addMatch(subj, null);
				match.setExtID(Long.toString(preset));
				ec.saveChanges();
				return preset;
			}
			
			SyncIndex indexer = system.getIndexNamed("eduAreas", null, false);
			String areaName = indexer.extForLocal(MyUtility.getID(subj.area()));
			KnowledgeArea area = (areaName == null) ? KnowledgeArea.Common :
					KnowledgeArea.fromString(areaName);
			if(full == null) full = subject;
			long result = 0;
			try {
				result = soap.insertSubject(schoolGuid, subject, full, area);
			} catch (RemoteException e) {
				if(!e.getMessage().contains("Entity already exists: subject")) {
					throw new AttributedRemoteException(e,
							new Object[] {schoolGuid, subject, full, area});
				}
				NSArray found = EOUtilities.objectsMatchingKeyAndValue(ec,
						Subject.ENTITY_NAME, Subject.SUBJECT_KEY, subject);
				for (int i = 0; i < found.count(); i++) {
					Subject other =  (Subject)found.objectAtIndex(i);
					if(other != subj) {
						result = subjectID(other);
						break;
					}
				}
				if(result == 0)
					throw e;
			}
			SyncMatch match = system.addMatch(subj, null);
			match.setExtID(Long.toString(result));
			ec.saveChanges();
			return result;
		}
		return Long.parseLong(subjExtid); 
	}
	
	private InsertLessonResult insertLesson(String groupGuid, long subjectID, String teacherGuid,
			Calendar cal, int num)  throws RemoteException {
		if(num < 0)
			num = 0;
		try {
			UnsignedByte number = new UnsignedByte(num);
			return soap.insertLesson(groupGuid, subjectID, teacherGuid, cal, number);
		} catch (RemoteException e) {
			if(e.getMessage().contains("Entity already exists: Lesson"))
				return insertLesson(groupGuid, subjectID, teacherGuid, cal, num +1);
			throw new AttributedRemoteException(e,
					new Object[] {groupGuid, subjectID,teacherGuid, cal,num});
		}
	}
	
	private String groupGuidForCourse(Integer courseID) throws RemoteException {
		if(courseEI == null)
			courseEI = EntityIndex.indexForEntityName(ec, EduCourse.entityName, true);
		SyncMatch match = getMatch(courseEI, courseID);
		if(match != null) {
			return match.extID().substring(0,36);
		}
		BaseCourse course = (BaseCourse)EOUtilities.objectWithPrimaryKeyValue(ec,
				BaseCourse.ENTITY_NAME, courseID);
		NSArray audience = EOUtilities.rawRowsMatchingKeyAndValue(ec,
				"CourseAudience", "courseID", courseID);
		if(audience == null || audience.count() == 0)
			return localBase.extidForObject(course.eduGroup());
		String parentGuid = localBase.extidForObject(course.eduGroup());
	/*	if(course.namedFlags().flagForKey("mixedGroup")) {
			Integer grade = course.cycle().grade();
			SyncMatch pmatch = SyncMatch.getMatch(null, localBase, 
					EduGroup.entityName, new Integer(-grade.intValue()));
			if(pmatch == null) {
				parentGuid = UUID.randomUUID().toString();
				
				String perlist = SettingsBase.stringSettingForCourse(ItogMark.ENTITY_NAME, 
						SettingsBase.courseDict(grade, eduYear), ec);
				SyncIndex idx = system.getIndexNamed("regimes", null, false);
				if(idx != null)
					perlist = idx.extForLocal(perlist);
				ReportingPeriodGroup[] rpgs = soap.getReportingPeriodGroupCollection(
						schoolGuid, eduYear.intValue());
	    		long pgrp = 0;
    			for (int i = 0; i < rpgs.length; i++) {
					if(perlist.equalsIgnoreCase(rpgs[i].getName())) {
						pgrp = rpgs[i].getID();
						break;
					}
				}
    	    	String timetable = system.extidForObject("ScheduleRing", new Integer(0), null);
    	    	Long ttID = (timetable == null)?null:new Long(timetable);
				soap.insertGroup(parentGuid, schoolGuid, grade.toString(), 
						new UnsignedByte(grade), eduYear,pgrp, "поток", ttID);
				pmatch = localBase.extSystem().addMatch(EduGroup.entityName, 
						new Integer(-grade.intValue()),localBase);
				pmatch.setExtID(parentGuid);
				ec.saveChanges();
			} else {
				parentGuid = pmatch.extID();
			}
		} else { 
			parentGuid = localBase.extidForObject(course.eduGroup());
		} */
		String extid = UUID.randomUUID().toString();
		StringBuilder title = new StringBuilder(course.cycle().subject());
		title.append(' ').append('(');
		if(course.comment() != null)
			title.append(course.comment());
		else
			title.append(Person.Utility.fullName(course.teacher(), true, 2, 1, 1));
		title.append(')');
		 while (match == null) {
			try {
				soap.insertSubGroup(extid, title.toString(), parentGuid, null);
				match = (SyncMatch)EOUtilities.createAndInsertInstance(ec, SyncMatch.ENTITY_NAME);
			} catch (RemoteException e) {
				if(e.getMessage().contains("Entity already exists: Sub group")) {
					title.append('*');
				} else {
					throw new AttributedRemoteException(e,
							new Object[] {extid, title, parentGuid, null});
				}
			}
		}
		match.setExtSystem(system);
		match.setEntityIndex(courseEI);
		match.setEduYear(eduYear);
		match.setObjID(courseID);
		match.setExtID(extid);
		ec.saveChanges();
		syncGroup(course.groupList(), match, null);
		return extid;
	}
	
	private boolean syncCourse(EduCourse course, SyncMatch personMatch) throws RemoteException {
		if(courseEI == null)
			courseEI = EntityIndex.indexForEntityName(ec, EduCourse.entityName, true);
		EOKeyGlobalID gid = (EOKeyGlobalID)ec.globalIDForObject(course);
		Integer courseID = (Integer)gid.keyValues()[0];
		SyncMatch match = SyncMatch.getMatch(system, null, courseEI, courseID);
		if(match == null)
			return false; // subgroup not uploaded for course
		syncGroup(course.groupList(), match, personMatch);
		return true;
	}
	
	private void syncGroup(NSArray list, SyncMatch match, SyncMatch force) throws RemoteException {
		String ext = match.extID();
		String guid = ext.substring(0,36);
		StringBuilder buf = new StringBuilder(guid);
		buf.append(' ');
		if(ext.length() > 37)
			ext = ext.substring(37);
		else
			ext = null;
		String forcedID = (force == null)?null:force.objID().toString();
		NSMutableArray exist = (ext == null)?null:
			NSMutableArray._mutableComponentsSeparatedByString(ext, ",");
		Enumeration enu = list.objectEnumerator();
		boolean update = false;
		while (enu.hasMoreElements()) {
			Student stu = (Student) enu.nextElement();
			String id = MyUtility.getID(stu);
			if(forcedID != null && id.equals(forcedID))
				forcedID = null;
			if(exist == null || !exist.removeObject(id)) {
				update = true;
				SyncMatch personMatch = SyncMatch.getMatch(localBase.extSystem(), localBase,
						Student.entityName, new Integer(id));
				if(personMatch == null)
					return;
				String personGuid = personMatch.extID();
				try {
					soap.insertGroupMembership(personGuid, guid);
				} catch (RemoteException e) {
					state.addMessage("insertGroupMembership(" + personGuid + ',' + guid + "): " +
							WOMessage.stringByEscapingHTMLString(e.getMessage()));
					continue;
				}
			}
			buf.append(id);
			if(enu.hasMoreElements())
				buf.append(',');
		}
		if(forcedID != null) {
			try {
				soap.insertGroupMembership(force.extID(), guid);
				buf.append(',').append(forcedID);
				if(exist != null)
					exist.removeObject(forcedID);
			} catch (RemoteException e) {
				state.addMessage("insertGroupMembership(" + force.extID() + ',' + guid + "): " +
						WOMessage.stringByEscapingHTMLString(e.getMessage()));
			}
		}
		if(exist != null && exist.count() > 0) {
			update = true;
			enu = exist.objectEnumerator();
			while (enu.hasMoreElements()) {
				String stid = (String) enu.nextElement();
				SyncMatch personMatch = SyncMatch.getMatch(localBase.extSystem(), localBase,
						Student.entityName, new Integer(stid));
				if(personMatch == null)
					continue;
				String personGuid = personMatch.extID();
				soap.deleteGroupMembership(personGuid, guid);
			}
		}
		if(update)
			match.setExtID(buf.toString());
	}
	
	private long syncLesson(MarkArchive arch) throws RemoteException {
		if(lessonEI == null)
			lessonEI = EntityIndex.indexForEntityName(ec, "BaseLesson", false);
		if(timeslotEI == null)
			timeslotEI = EntityIndex.indexForEntityName(ec, "Timeslot", true);
		SyncMatch match = getMatch(lessonEI, arch.getKeyValue("lesson"));
		if(match == null && arch.actionType().intValue() == 3)
			return 0;
		if(arch.actionType().intValue() == 3) {
			long lessonID = Long.parseLong(match.extID());
			if(workEI == null)
				workEI = EntityIndex.indexForEntityName(ec, "Work", false);
			NSArray works = EOUtilities.objectsWithQualifierFormat(ec, SyncMatch.ENTITY_NAME, 
				"extSystem = %@ AND entityIndex = %@ AND (extID like %s'|*' OR extID like %s' *')", 
					new NSArray(new Object[] {system,workEI, match.extID(), match.extID()}));
			if(works == null || works.count() == 0) {
				try {
					soap.deleteLesson(lessonID);
				} catch (RemoteException e) {
					if(!e.getMessage().contains("Entity not found: Lesson")) {
						throw new AttributedRemoteException(e, new Object[] {lessonID});
					}
				}
				works = EOUtilities.objectsWithQualifierFormat(ec, SyncMatch.ENTITY_NAME, 
						"extSystem = %@ AND entityIndex = %@ AND extID like %s' *'",
						new NSArray(new Object[] {system,timeslotEI, match.extID()}));
				if(works != null) {
					for (int i = 0; i < works.count(); i++) {
						ec.deleteObject((EOEnterpriseObject)works.objectAtIndex(i));
					}
				}
			}
			ec.deleteObject(match);
			ec.saveChanges();
			return 0;
		}
		Integer courseID = arch.getKeyValue("course");
		EduCourse course = (EduCourse)EOUtilities.objectWithPrimaryKeyValue(ec,
				EduCourse.entityName, courseID);
		if(!syncCourse(course))
			return 0;
		String groupGuid = groupGuidForCourse(courseID);
		groupGuid = groupGuid.toUpperCase();
		NSTimestamp date = (NSTimestamp)arch.valueForKey("@date_date");
		String teacherGuid = localBase.extidForObject(course.teacher(date));
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		cal.set(Calendar.HOUR_OF_DAY, timeShift);
		int number = Integer.parseInt(arch.getArchiveValueForKey("number"));
		Integer timeslot = new Integer(getTimeslot(course, cal, number));
		cal.setTime(date);
		cal.set(Calendar.HOUR_OF_DAY, timeShift);
		SyncMatch tsMatch = getMatch(timeslotEI, timeslot);
		long subjectID = subjectID(course);
		long[] ids = (tsMatch == null) ? new long[2] : parceIDs(tsMatch.extID());
		if(match == null) {
			if(tsMatch == null) {
				InsertLessonResult result = insertLesson(groupGuid, subjectID, 
						teacherGuid, cal, number);
				ids[0] = result.getLessonID();
				ids[1] = result.getWorkID();
				tsMatch = system.addMatch("Timeslot", timeslot, null);
//					(SyncMatch)EOUtilities.createAndInsertInstance(ec, SyncMatch.ENTITY_NAME);
//				tsMatch.setExtSystem(system);
//				tsMatch.setEntityIndex(timeslotEI);
				tsMatch.setEduYear(eduYear);
//				tsMatch.setObjID(timeslot);
				StringBuilder buf = new StringBuilder();
				buf.append(ids[0]).append(' ').append(ids[1]);
				tsMatch.setExtID(buf.toString());
//			} else {
//				soap.updateLesson(ids[0], subjectID, teacherGuid, cal, null);
			}
			match = system.addMatch("BaseLesson", arch.getKeyValue("lesson"), null);
//			(SyncMatch)EOUtilities.createAndInsertInstance(ec, SyncMatch.ENTITY_NAME);
//			match.setExtSystem(system);
//			match.setEntityIndex(lessonEI);
//			match.setObjID(arch.getKeyValue("lesson"));
			match.setEduYear(eduYear);
			match.setExtID(Long.toString(ids[0]));
			ec.saveChanges();
		} else {
			if(tsMatch == null) { // update timeslot
				NSArray found = EOUtilities.objectsWithQualifierFormat(ec, SyncMatch.ENTITY_NAME, 
						"extSystem = %@ AND entityIndex = %@ AND extID like %s' *'",
						new NSArray(new Object[] {system,timeslotEI, match.extID()}));
				if(found != null && found.count() > 0) {
					tsMatch = (SyncMatch)found.objectAtIndex(0);
					tsMatch.setObjID(timeslot);
					ec.saveChanges();
					ids = parceIDs(tsMatch.extID());
				} else {
					ids[0] = Long.parseLong(match.extID());
				}
				try {
					soap.updateLesson(ids[0], subjectID, teacherGuid, cal, new UnsignedByte(number));
				} catch (RemoteException e) {
					throw new AttributedRemoteException(e,
							new Object[] {ids[0], subjectID, teacherGuid, cal, number});
				}
			}
		}
		if(ids[1] != 0) {
			try {
				soap.updateWork(ids[1], null, null, ONE, null,
						arch.getArchiveValueForKey("theme"));
			} catch (RemoteException e) {
				if(!e.getMessage().contains("Entity not found: Work")) {
					throw new AttributedRemoteException(e,new Object[] {ids[1], "null", "null",
							ONE, "null", arch.getArchiveValueForKey("theme")});
				}
				ids[1] = soap.insertWork(ids[0], EduWorkType.LessonAnswer, EduMarkType.Mark5, 
						ONE, "Работа на уроке", arch.getArchiveValueForKey("theme"));
				StringBuilder buf = new StringBuilder();
				buf.append(ids[0]).append(' ').append(ids[1]);
				tsMatch.setExtID(buf.toString());
				ec.saveChanges(); 
			}
		}
		return ids[0];
	}
	
	private MarkArchive getContainerMA(MarkArchive arch, String entity, String key) {
		EOQualifier[] quals = new EOQualifier[4];
		quals[0] = new EOKeyValueQualifier("usedEntity.usedEntity", 
				EOQualifier.QualifierOperatorEqual, entity);
		quals[1] = new EOKeyValueQualifier(MarkArchive.KEY1_KEY, 
				EOQualifier.QualifierOperatorEqual, arch.getKeyValue(key));
		quals[2] = new EOKeyValueQualifier(MarkArchive.TIMESTAMP_KEY, 
				EOQualifier.QualifierOperatorLessThanOrEqualTo, arch.timestamp());
		quals[3] = new EOKeyValueQualifier(MarkArchive.ACTION_TYPE_KEY, 
				EOQualifier.QualifierOperatorLessThan, new Integer(3));
		quals[0] = new EOAndQualifier(new NSArray(quals));
		NSArray list = new NSArray(new EOSortOrdering(
				MarkArchive.TIMESTAMP_KEY, EOSortOrdering.CompareDescending));
		EOFetchSpecification fs = new EOFetchSpecification(
				MarkArchive.ENTITY_NAME, quals[0],list);
		fs.setFetchLimit(1);
		list = ec.objectsWithFetchSpecification(fs);
		if(list == null || list.count() == 0)
			return null;
		return (MarkArchive)list.objectAtIndex(0);
	}
	
	private boolean skipStudent(Integer id) {
		if(contype == null || studentEI == null)
			return false;
		NSDictionary query = new NSDictionary(new Object[] {contype,studentEI,id, new Integer(1)},
				new String[] {
				Contact.TYPE_KEY,Contact.PERSON_ENTITY_KEY, Contact.PERS_ID_KEY,Contact.FLAGS_KEY});
		NSArray found = EOUtilities.objectsMatchingValues(ec, Contact.ENTITY_NAME, query);
		return (found == null || found.count() == 0);
	}
	private boolean activeStudents(NSArray list) {
		if(contype == null || studentEI == null)
			return true;
		NSMutableArray quals = new NSMutableArray();
		Enumeration enu = list.objectEnumerator();
		while (enu.hasMoreElements()) {
			Student st = (Student) enu.nextElement();
			EOKeyGlobalID gid = (EOKeyGlobalID) ec.globalIDForObject(st);
			quals.addObject(new EOKeyValueQualifier(Contact.PERS_ID_KEY,
					EOQualifier.QualifierOperatorEqual, gid.keyValues()[0]));
		}
		if(quals.count() > 0) {
			EOQualifier qual = new EOOrQualifier(quals);
			quals.removeAllObjects();
			quals.addObject(qual);
		}
		quals.addObject(new EOKeyValueQualifier(Contact.TYPE_KEY,
				EOQualifier.QualifierOperatorEqual, contype));
		quals.addObject(new EOKeyValueQualifier(Contact.PERSON_ENTITY_KEY,
				EOQualifier.QualifierOperatorEqual, studentEI));
		quals.addObject(new EOKeyValueQualifier(Contact.FLAGS_KEY,
				EOQualifier.QualifierOperatorEqual, new Integer(1)));
		EOQualifier qual = new EOAndQualifier(quals);
		EOFetchSpecification fs = new EOFetchSpecification(Contact.ENTITY_NAME,qual,null);
		NSArray found = ec.objectsWithFetchSpecification(fs);
		return (found != null && found.count() > 0);
	}
	
	protected NSMutableDictionary syncGroups = new NSMutableDictionary();
	private boolean syncCourse(EduCourse course) {
		if(((BaseCourse)course).namedFlags().flagForKey("mixedGroup")) {
			return activeStudents(course.groupList());
		} else {
			EduGroup grp = course.eduGroup();
			Boolean b = (Boolean)syncGroups.objectForKey(course);
			if(b == null) {
				b = Boolean.valueOf(activeStudents(grp.list()));
				syncGroups.setObjectForKey(b, grp);
			}
			return b.booleanValue();
		}
	}
	
	private void syncNote(MarkArchive arch) throws RemoteException {
		Integer studentID = arch.getKeyValue("student");
		if(skipStudent(studentID))
			return;
		if(lessonEI == null)
			lessonEI = EntityIndex.indexForEntityName(ec, "BaseLesson", false);
		SyncMatch match = null;
		match = getMatch(lessonEI, arch.getKeyValue("lesson"));
		if(match == null && arch.actionType().intValue() == 3)
			return;
		long lessonID;
		if(match == null) {
			MarkArchive la = getContainerMA(arch, "BaseLesson", "lesson");
			if(la == null)
				return;
			lessonID = syncLesson(la);
		} else {
			lessonID = Long.parseLong(match.extID());
		}
		
		SyncMatch personMatch = SyncMatch.getMatch(localBase.extSystem(), localBase,
				Student.entityName, studentID);
		if(personMatch == null)
			return;
		String personGuid = personMatch.extID();
		personGuid = personGuid.toUpperCase();
		if(arch.actionType().intValue() >= 3) {
			soap.updateLessonLogEntry(lessonID, personGuid, EduLessonLogEntryStatus.Attend);
		} else {
			String note = arch.getArchiveValueForKey("text");
			boolean att = (BaseLesson.isSkip(note) == 0);
			try {
				soap.updateLessonLogEntry(lessonID, personGuid, 
						(att)?EduLessonLogEntryStatus.Attend : EduLessonLogEntryStatus.Absent);
			} catch (RemoteException e) {
				if(e.getMessage().contains("student doesn't have membership in the group")) {
					EduLesson lesson = (EduLesson)EOUtilities.objectWithPrimaryKeyValue(ec, 
							EduLesson.entityName, arch.getKeyValue("lesson"));
					if(lesson == null || lesson.course() == null)
						return;
					if(syncCourse(lesson.course(), personMatch))
						soap.updateLessonLogEntry(lessonID, personGuid, 
							(att)?EduLessonLogEntryStatus.Attend : EduLessonLogEntryStatus.Absent);
					}
			}
		}
	}
	
	private NSDictionary[] syncWork(MarkArchive arch) throws RemoteException {
		if(workEI == null)
			workEI = EntityIndex.indexForEntityName(ec, "Work", false);
		if(timeslotEI == null)
			timeslotEI = EntityIndex.indexForEntityName(ec, "Timeslot", true);
		SyncMatch match = null;
		if(arch.actionType().intValue() != 1) {
			match = getMatch(workEI, arch.getKeyValue("work"));
			if(match == null && arch.actionType().intValue() == 3)
				return null;
		}
		if(arch.actionType().intValue() == 3) {
			String extID = match.extID();
			int idx2 = extID.lastIndexOf(';');
			NSMutableArray toDelete = new NSMutableArray();
			NSMutableArray quals = new NSMutableArray();
			while (idx2 > 0) {
				int idx1 = idx2 -1;
				if(Character.isDigit(extID.charAt(idx1))) {
					idx1 = extID.lastIndexOf(' ',idx1);
					String txtID = extID.substring(idx1 +1,idx2);
					toDelete.addObject(txtID);
					quals.addObject(new EOKeyValueQualifier(SyncMatch.EXT_ID_KEY,
							EOQualifier.QualifierOperatorLike,"* " + txtID));
				}
				idx2 = extID.lastIndexOf(';',idx1);
			}
			if(quals.count() > 1) {
				EOQualifier qual = new EOOrQualifier(quals);
				quals.removeAllObjects();
				quals.addObject(qual);
			}
			quals.addObject(new EOKeyValueQualifier(SyncMatch.EXT_SYSTEM_KEY, 
					EOQualifier.QualifierOperatorEqual, system));
			quals.addObject(new EOKeyValueQualifier(SyncMatch.ENTITY_INDEX_KEY, 
					EOQualifier.QualifierOperatorEqual, timeslotEI));
			EOFetchSpecification fs = new EOFetchSpecification(SyncMatch.ENTITY_NAME,
					new EOAndQualifier(quals),null);
			NSArray found = ec.objectsWithFetchSpecification(fs);
			if(found != null && found.count() > 0) {
				for (int i = 0; i < found.count(); i++) {
					SyncMatch sm = (SyncMatch)found.objectAtIndex(i);
					String wID = sm.extID();
					int idx = wID.indexOf(' ');
					if(idx < 0) continue;
					wID = wID.substring(idx +1);
					toDelete.removeObject(wID);
				}
			}
			if(toDelete.count() > 0) {
				for (int i = 0; i < toDelete.count(); i++) {
					String wID = (String)toDelete.objectAtIndex(0);
					try {
						soap.deleteWork(Long.parseLong(wID));
					} catch (RemoteException e) {
						if(!e.getMessage().contains("Entity not found: Work"))
							throw new AttributedRemoteException(e,new Object[] {wID});					}
				}
			}
			ec.deleteObject(match);
			ec.saveChanges();
			return null;
		}
		Integer courseID = arch.getKeyValue("course");
		EduCourse course = (EduCourse)EOUtilities.objectWithPrimaryKeyValue(ec,
				EduCourse.entityName, courseID);
		if(!syncCourse(course))
			return null;
		String maxVal = arch.getArchiveValueForKey("m0");
		NSMutableDictionary[] criteria = null;
		Integer csID = null;
		if(maxVal == null) {
//			maxVal = "5";
			if(critSetParams == null || critSetParams.count() == 0) {
				criteriaSettings = SettingsBase.baseForKey(CriteriaSet.ENTITY_NAME, ec, false);
				if(critSetParams == null)
					critSetParams = new NSMutableDictionary();
				critSetParams.setObjectForKey("0", "0");
			}
			if(criteriaSettings!=null)
				csID = criteriaSettings.forCourse(course).numericValue();
			if(csID == null || csID.intValue() == 0) {
				NSDictionary arcDict = arch.getArchiveDictionary();
				Enumeration enu = arcDict.keyEnumerator();
				int max = 0;
				NSMutableDictionary crByNum = new NSMutableDictionary();
				while (enu.hasMoreElements()) {
					String key = (String) enu.nextElement();
					if(key.charAt(0) != 'm' || !Character.isDigit(key.charAt(1)))
						continue;
					int crit = Integer.parseInt(key.substring(1));
					if(crit > max)
						max = crit;
					String crMax = (String)arcDict.valueForKey(key);
					NSMutableDictionary dict = new NSMutableDictionary();
					dict.takeValueForKey(new Integer(crMax), "criterMax");
					Integer criter = new Integer(crit);
					dict.takeValueForKey(CriteriaSet.critNameForNum(criter, null), "criterName");
					crByNum.setObjectForKey(dict, criter);
				}
//				crByNum.takeValueForKey(new Integer(max), "maxCriterion");
				criteria = new NSMutableDictionary[max +1];
				Enumeration cenu = crByNum.keyEnumerator();
				while (cenu.hasMoreElements()) {
					Integer cr = (Integer) cenu.nextElement();
					criteria[cr.intValue()] = (NSMutableDictionary)crByNum.objectForKey(cr);
				}
			} else {
				NSDictionary[] preset = analyseCriteriaSet(csID);
				int count = 0;
				criteria = new NSMutableDictionary[preset.length];
				for (int i = 0; i < preset.length; i++) {
					String crMax = arch.getArchiveValueForKey("m" + i);
					if(crMax == null)
						continue;
					count++;
					if(preset[i] == null) {
						criteria[i] = new NSMutableDictionary(
								CriteriaSet.critNameForNum(i, null), "criterName");
						criteria[i].takeValueForKey(new Integer(crMax), "criterMax");
					} else {
						criteria[i] = preset[i].mutableClone();
//						if(!criteria[i].containsKey("criterMax"))
							criteria[i].takeValueForKey(new Integer(crMax), "criterMax");
					}
				}
				if(count <= 0) {
					criteria = null;
					csID = null;
				}
			}
		} // no criterless max found
		if(criteria == null) {
//			extid.append('#').append(' ').append(maxVal).append(' ');
			NSMutableDictionary dict = new NSMutableDictionary("#","criterName");
			if(maxVal == null)
				dict.takeValueForKey(new Integer(5), "criterMax");
			else
				dict.takeValueForKey(new Integer(maxVal), "criterMax");
			criteria = new NSMutableDictionary[] {dict};
		}
		EduWorkType workType = getWorkType(arch);
		String title = arch.getArchiveValueForKey(Work.THEME_KEY);
		String[] existing = null;
		long lessonID;
		if(match == null) {
			String groupGuid = groupGuidForCourse(courseID);
			NSTimestamp date = (NSTimestamp)arch.valueForKey("@date_date");
			String teacherGuid = localBase.extidForObject(course.teacher(date));
			if(teacherGuid == null)
				return null;
			teacherGuid = teacherGuid.toUpperCase();
			Calendar cal = Calendar.getInstance();
			cal.setTime(date);
			cal.set(Calendar.HOUR_OF_DAY, timeShift);
			int number = Integer.parseInt(arch.getArchiveValueForKey("number"));
			Integer timeslot = new Integer(getTimeslot(course, cal, number));
			cal.setTime(date);
			cal.set(Calendar.HOUR_OF_DAY, timeShift);
			SyncMatch tsMatch = getMatch(timeslotEI, timeslot);
			long[] ids;
			if(tsMatch == null) {
				InsertLessonResult result = insertLesson(
						groupGuid, subjectID(course), teacherGuid, cal, number);
				ids = new long[2];
				ids[0] = result.getLessonID();
				ids[1] = result.getWorkID();
				tsMatch = (SyncMatch)EOUtilities.createAndInsertInstance(ec, SyncMatch.ENTITY_NAME);
				tsMatch.setExtSystem(system);
				tsMatch.setEntityIndex(timeslotEI);
				tsMatch.setEduYear(eduYear);
				tsMatch.setObjID(timeslot);
				StringBuilder buf = new StringBuilder();
				buf.append(ids[0]).append(' ').append(ids[1]);
				tsMatch.setExtID(buf.toString());
			} else {
				ids = parceIDs(tsMatch.extID());
			}
			lessonID = ids[0];
			ec.saveChanges();
			if(workType == null && criteria.length == 1) {
				existing = new String[] {"# 5 " + ids[1]};
			} else {
				existing = new String[0];
			}
		} else {
			String extID = match.extID();
			int idx = extID.indexOf('|');
			int idx2 = Math.min(idx, extID.indexOf(' '));
			String lID = extID.substring(0,idx2);
			lessonID = Long.parseLong(lID);
			extID = extID.substring(idx +1);
			existing = extID.split(";",-1);
			if(workType != null && existing[0] != null && existing[0].length() == 1) {
				idx = existing[0].lastIndexOf(' ');
				String tsID = lID + existing[0].substring(idx);
				NSArray args = new NSArray(new Object[] {system, timeslotEI, tsID});
				NSArray found = EOUtilities.objectsWithQualifierFormat(ec, SyncMatch.ENTITY_NAME,
						"extSystem = %@ AND entityIndex = %@ AND extID = %s", args);
				if(found != null && found.count() > 0)
					existing = new String[0];
			}
		}
		for (int i = 0; i < existing.length || i < criteria.length; i++) {
			Long workID = null;
			if(i < existing.length && existing[i] != null && existing[i].length() > 0) {
				int idx = existing[i].lastIndexOf(' ');
				workID = new Long(existing[i].substring(idx +1));
			}
			if(i < criteria.length && criteria[i] != null) {
				String name = title;
				if(i > 0) {
					name = title + " [" + criteria[i].valueForKey("criterName") + ']';
				}
				EduMarkType markType = (EduMarkType)criteria[i].valueForKey("markType");
				if(markType == null) {
					Integer max = (Integer)criteria[i].valueForKey("criterMax");
					markType = typeForMax(max);
					if(markType == null)
						markType = EduMarkType.Mark100;
				}
				if(workID == null) {
					if(workType == null)
						workType = EduWorkType.LessonAnswer;
					try {
						workID = soap.insertWork(lessonID, workType, markType, ONE, name, "");
					} catch (RemoteException e) {
						throw new AttributedRemoteException(e,
								new Object[] {lessonID, workType, markType, ONE, name, ""});
					}
				} else {
					try {
//						EduWorkType type = (workType == EduWorkType.LessonBehavior)? null : workType;
						soap.updateWork(workID.longValue(), workType, markType, ONE, name, null);
					} catch (RemoteException e) {
						if(!e.getMessage().contains("Entity not found: Work"))
							throw new AttributedRemoteException(e,
									new Object[] {workID, workType, markType, ONE, name, null, ""});
						if(workType == null)
							workType = EduWorkType.LessonAnswer;
						workID = soap.insertWork(lessonID, workType, markType, ONE, name, "");
					}
				}
				criteria[i].takeValueForKey(workID, "workID");
			} else if(workID != null) {
				try {
					soap.deleteWork(workID.longValue());
				} catch (RemoteException e) {
					if(!e.getMessage().contains("Entity not found: Work"))
						throw new AttributedRemoteException(e,new Object[] {workID});
				}
			}
		}
		StringBuilder extid = new StringBuilder();
		extid.append(lessonID);
		if(csID != null)
			extid.append(' ').append(csID);
		extid.append('|');
		for (int i = 0; i < criteria.length; i++) {
			if(criteria[i] != null) {
				extid.append(criteria[i].valueForKey("criterName")).append(' ');
				extid.append(criteria[i].valueForKey("criterMax")).append(' ');
				extid.append(criteria[i].valueForKey("workID"));
			}
			extid.append(';');
		}
		if(match == null) {
			match = (SyncMatch)EOUtilities.createAndInsertInstance(ec, SyncMatch.ENTITY_NAME);
			match.setExtSystem(system);
			match.setEntityIndex(workEI);
			match.setEduYear(eduYear);
			match.setObjID(arch.getKeyValue("work"));
		}
		match.setExtID(extid.toString());
		ec.saveChanges();
		return criteria;
	}

	public static final NSDictionary[] emptyCritset = 
		new NSDictionary[] {NSDictionary.EmptyDictionary};
	protected NSDictionary[] analyseCriteriaSet(Integer csID) {
		NSDictionary[] preset = (NSDictionary[])critSetParams.objectForKey(csID);
		if(preset != null)
			return preset;
		CriteriaSet set = (CriteriaSet)EOUtilities.objectWithPrimaryKeyValue(
				ec, CriteriaSet.ENTITY_NAME, csID);
		NSArray crits = set.criteria();
		if(crits == null || crits.count() == 0) {
			critSetParams.setObjectForKey(emptyCritset, csID);			
			return emptyCritset;
		}
		Enumeration enu = crits.objectEnumerator();
		int max = 0;
		NSMutableDictionary byNum = new NSMutableDictionary();
		while (enu.hasMoreElements()) {
			EOEnterpriseObject cr = (EOEnterpriseObject) enu.nextElement();
			String title = (String)cr.valueForKey("title");
			Integer criter = (Integer) cr.valueForKey("criterion");
			if(criter.intValue() > max)
				max = criter.intValue();
			if(title == null) title = CriteriaSet.critNameForNum(criter, null);
			title = title.replace(' ', '_').replace(';', ':');
			NSMutableDictionary dict = new NSMutableDictionary(title,"criterName");
			dict.takeValueForKey(cr.valueForKey("dfltMax"), "criterMax");
			byNum.setObjectForKey(dict, criter);
			Indexer indexer = (Indexer)cr.valueForKey("indexer");
			if(indexer == null)
				continue;
			boolean nums = true;
			boolean amer = true;
			boolean plus = false;
			NSMutableDictionary mapping = new NSMutableDictionary();
			int vMax = 0;
			Enumeration ienu = indexer.indexRows().objectEnumerator();
			while (ienu.hasMoreElements()) {
				IndexRow row = (IndexRow) ienu.nextElement();
				mapping.takeValueForKey(row.idx(), row.value());
				String value = row.value().trim();
				if(value.length() > 1) {
					char fin = value.charAt(value.length() -1);
					if(fin == '+' || fin == '-')
						plus = true;
					value = value.substring(0,value.length() -1).trim();
				}
				if(nums) {
					try {
						int val = Integer.parseInt(value);
						if(val > vMax)
							vMax = val;
						amer = false;
					} catch (Exception e) {
						nums = false;
					}
				}
				if(amer) {
					int val = -1;
					if(value.length() == 1) {
						val = "ABCDEF".indexOf(value.toUpperCase());
					}
					amer = (val >= 0);
				}
			} // index rows
			if(amer) {
				dict.takeValueForKey(EduMarkType.MarkAbcdf, "markType");
				dict.takeValueForKey(new Integer(5), "criterMax");
			} else if(nums) {
				EduMarkType markType = typeForMax(vMax);
				if(markType == null) {
					dict.addEntriesFromDictionary(mapping);
				} else {
					dict.takeValueForKey(markType, "markType");
					dict.takeValueForKey(new Integer(vMax), "criterMax");
				}
			} else {
				dict.addEntriesFromDictionary(mapping);
			}
			if(plus)
				dict.takeValueForKey(Boolean.TRUE, "plusMinus");
		} // set ctiteria enumeration
//		byNum.takeValueForKey(new Integer(max), "maxCriterion");
		preset = new NSDictionary[max +1];
		Enumeration cenu = byNum.keyEnumerator();
		while (cenu.hasMoreElements()) {
			Integer cr = (Integer) cenu.nextElement();
			preset[cr] = (NSMutableDictionary)byNum.objectForKey(cr);
			preset[cr] = preset[cr].immutableClone();
		}
		return preset;
	}
	
	private String onLesson;
	protected EduWorkType getWorkType(MarkArchive arch) {
		String workType = arch.getArchiveValueForKey("workType");
		if(workType == null)
			return null;
		workType = workType.substring(9);
		if(onLesson == null)
			onLesson = MyUtility.getID(WorkType.getSpecType(ec, "onLesson"));
		if(workType.equals(onLesson))
			return null;
		if(workTypes == null) {
			SyncIndex indexer = system.getIndexNamed("workType", null, false);
			workTypes = indexer.getDict();
			if(workTypes == null) {
				workTypes = new NSMutableDictionary();
				NSArray types = EOUtilities.objectsForEntityNamed(ec, WorkType.ENTITY_NAME);
				Enumeration enu = types.objectEnumerator();
				while (enu.hasMoreElements()) {
					WorkType type = (WorkType) enu.nextElement();
//					if(onLesson.equals(type.dfltFlags()))
//						workTypes.takeValueForKey("LessonBehavior", MyUtility.getID(type));
					if(type.namedFlags().flagForKey("hometask"))
						workTypes.takeValueForKey("LessonHomework", MyUtility.getID(type));
				}
			} // generate defaultTypes
		}
		workType = (String)workTypes.valueForKey(workType);
		if(workType == null)
			return EduWorkType.LessonAnswer;
		return EduWorkType.fromString(workType);
	}
	
	private EduMarkType typeForMax(int max) {
		switch (max) {
		case 5:
			return EduMarkType.Mark5;
//		case 6:
//			return EduMarkType.Mark6;
		case 7:
			return EduMarkType.Mark7;
		case 10:
			return EduMarkType.Mark10;
		case 12:
			return EduMarkType.Mark12;
		case 1:
			return EduMarkType.Test;
		case 100:
			return EduMarkType.Mark100;
		default:
			return null;	
		}
	}
	
	private void syncMark(MarkArchive arch) throws RemoteException {
		Integer studentID = arch.getKeyValue("student");
		if(skipStudent(studentID))
			return;
		if(workEI == null)
			workEI = EntityIndex.indexForEntityName(ec, "Work", false);
		SyncMatch match = null;
		match = getMatch(workEI, arch.getKeyValue("work"));
		if(match == null && arch.actionType().intValue() == 3)
			return;
		NSDictionary[] criteria;
		if(match == null) {
			MarkArchive wa = getContainerMA(arch, "Work", "work");
			if(wa == null)
				return;
			criteria = syncWork(wa);
		} else {
			String extID = match.extID();
			int idx = extID.indexOf('|');
			int idx2 = extID.lastIndexOf(' ',idx);
			NSDictionary[] preset = null;
			if(idx2 > 0) {
				Integer csID = new Integer(extID.substring(idx2 +1, idx));
				preset = analyseCriteriaSet(csID);
			}
			String[] exist = extID.substring(idx +1).split(";");
			criteria = new NSMutableDictionary[exist.length];
			for (int i = 0; i < exist.length; i++) {
				if(exist == null || exist[i].length() == 0)
					continue;
				String[] vals = exist[i].split(" ",3);
				if(preset == null || preset[i] == null) {
					criteria[i] = new NSMutableDictionary(vals[0],"criterName");
					criteria[i].takeValueForKey(new Integer(vals[1]), "criterMax");
				} else {
					criteria[i] = preset[i].mutableClone();
					if(!preset[i].containsKey("criterMax"))
						criteria[i].takeValueForKey(new Integer(vals[1]), "criterMax");
				}
				criteria[i].takeValueForKey(new Long(vals[2]), "workID");
			}
		}
		SyncMatch personMatch = SyncMatch.getMatch(localBase.extSystem(), localBase,
				Student.entityName, studentID);
		if(personMatch == null)
			return;
		String personGuid = personMatch.extID();
		personGuid = personGuid.toUpperCase();
		int act = arch.actionType().intValue();
		String text = arch.getArchiveValueForKey("text");
		if(".".equals(text))
			text = "";
		for (int i = 0; i < criteria.length; i++) {
			if(criteria[i] == null)
				continue;
			Long workID = (Long)criteria[i].valueForKey("workID");
			if(act >= 3) {
				try {
					soap.deleteMark(workID.longValue(), personGuid, ZERO);
				} catch (RemoteException e) {
					if(!e.getMessage().contains("Entity not found: Mark"))
						throw new AttributedRemoteException(e,
								new Object[] {workID, personGuid, ZERO});
				}
				continue;
			}
			String criterName = (String)criteria[i].valueForKey("criterName");
			String value = arch.getArchiveValueForKey(criterName);
			if(value == null || ".".equals(value)) {
				try {
					soap.deleteMark(workID.longValue(), personGuid, ZERO);
				} catch (RemoteException e) {
					if(!e.getMessage().contains("Entity not found: Mark"))
						throw new AttributedRemoteException(e,
								new Object[] {workID, personGuid, ZERO});
				}
				continue;
			}
			EduMarkType markType = (EduMarkType)criteria[i].valueForKey("markType");
			MarkBonusType bonus = MarkBonusType.NotSet;
			BigDecimal mark = null;
			if(Various.boolForObject(criteria[i].valueForKey("plusMinus"))) {
				int l = value.length() -1;
				char last = value.charAt(l);
				if(last == '+') {
					bonus = MarkBonusType.Plus;
					value = value.substring(0,l).trim();
				} else if(last == '-') {
					bonus = MarkBonusType.Minus;
					value = value.substring(0,l).trim();
				}
			}
			if(markType == EduMarkType.MarkAbcdf) {
				int idx = " FEDCBA".indexOf(value.toUpperCase());
				mark = (idx >= 0)?new BigDecimal(idx):BigDecimal.ZERO;
			} else if(markType == null) {
				Integer max = (Integer)criteria[i].valueForKey("criterMax");
				markType = typeForMax(max);
				if(markType == null) {
					markType = EduMarkType.Mark100;
					Integer mapped = (Integer)criteria[i].valueForKey(value);
					if(mapped == null)
						mapped = new Integer(value);
					mark = new BigDecimal(mapped * 100);
					mark = mark.divide(new BigDecimal(max),0,BigDecimal.ROUND_HALF_UP);
				} else {
					mark = new BigDecimal(value);
				}
			} else {
				try {
					mark = new BigDecimal(value);
				} catch (Exception e) {
					mark = BigDecimal.ZERO;
				}
			}
			if(act == 1) {
				try {
					soap.insertMark(workID, personGuid, mark, markType, ZERO, bonus, text);
				} catch (RemoteException e) {
					if(e.getMessage().contains("Entity already exists: Mark")) {
						soap.updateMark(workID, personGuid, ZERO, mark, markType, bonus, text);
					} else if(e.getMessage().contains(
							"student doesn't have membership in the group")) {
						Work work = (Work)EOUtilities.objectWithPrimaryKeyValue(ec, 
								Work.ENTITY_NAME, arch.getKeyValue("work"));
						if(work == null || work.course() == null)
							return;
						if(syncCourse(work.course(), personMatch))
							soap.insertMark(workID, personGuid, mark, markType, ZERO, bonus, text);
					} else {
						throw new AttributedRemoteException(e, new Object[] {
								workID, personGuid, mark, markType,ZERO, bonus, text});
					}
				}
			} else {
				try {
					soap.updateMark(workID, personGuid, ZERO, mark, markType, bonus, text);
				} catch (RemoteException e) {
					if(!e.getMessage().contains("Entity not found: Mark"))
						throw new AttributedRemoteException(e, new Object[] {
								workID, personGuid, ZERO, mark, markType, bonus, text});
					soap.insertMark(workID, personGuid, mark, markType, ZERO, bonus, text);
				}
			}
//			text = null;
		} // criteria
	}
	
	private static final String[] defaultTypes = new String[] {
		"Exam","Year","Semester","Trimester","Quarter"};
	private void syncItogMark(MarkArchive arch) throws RemoteException {
		Integer studentID = arch.getKeyValue("student");
		if(skipStudent(studentID))
			return;
		String mark = arch.getArchiveValueForKey("mark");
		if(mark == null)
			return;
		else
			mark = mark.trim();
		MarkBonusType bonus = MarkBonusType.NotSet;
		{
			char c = mark.charAt(mark.length() -1);
			if(c == '+') {
				bonus = MarkBonusType.Plus;
				mark = mark.substring(0, mark.length() -1).trim();
			} else if(c == '-') {
				bonus = MarkBonusType.Minus;
				mark = mark.substring(0, mark.length() -1).trim();
			}
		}
		EduMarkType markType = EduMarkType.Mark5;
		if(mark.matches("[НнH]\\W*[АаA]\\W?")) {
			markType = EduMarkType.MarkNA;
		} else if (mark.toLowerCase().contains("осв")) {
			markType = EduMarkType.MarkDismiss;
		}
		if(mark.length() > 0) {
			StringBuilder buf = new StringBuilder();
			for (int i = 0; i < mark.length(); i++) {
				char c = mark.charAt(i);
				if(Character.isDigit(c))
					buf.append(c);
			}
			mark = buf.toString();
		}
		BigDecimal value = null;
		try {
			value = new BigDecimal(mark);
		} catch (Exception e) {
			return;
		}
		
		EduCycle cycle = (EduCycle)EOUtilities.objectWithPrimaryKeyValue(ec,
				EduCycle.entityName, arch.getKeyValue("eduCycle"));
		Student student = (Student)EOUtilities.objectWithPrimaryKeyValue(ec,
				Student.entityName, studentID);
		EOQualifier[] quals = new EOQualifier[2];
		quals[0] = new EOKeyValueQualifier("eduYear",
				EOQualifier.QualifierOperatorEqual,eduYear);
		quals[1]  = new EOKeyValueQualifier("cycle",
				EOQualifier.QualifierOperatorEqual,cycle);
		quals[0] = new EOAndQualifier(new NSArray(quals));
		EOFetchSpecification fs = new EOFetchSpecification(EduCourse.entityName,quals[0],null);
		NSArray found = ec.objectsWithFetchSpecification(fs);
		if(found == null || found.count() == 0)
			return;
/*		EduGroup group = null;
		Enumeration enu = found.objectEnumerator();
		while (enu.hasMoreElements()) {
			EduCourse course = (EduCourse) enu.nextElement();
			if(course.groupList().containsObject(student)) {
				group = course.eduGroup();
				break;
			}
		}
		if(group == null) */
		EduGroup group = student.recentMainEduGroup();
		String groupGuid = localBase.extidForObject(group);
		if(groupGuid == null) {
			return;
		}
		groupGuid = groupGuid.toUpperCase();
		Subject subj = (Subject)cycle.valueForKey("subjectEO");
		long subjectID = subjectID(subj);
		ItogContainer container = (ItogContainer)EOUtilities.objectWithPrimaryKeyValue(ec,
				ItogContainer.ENTITY_NAME, arch.getKeyValue("container"));
		SyncMatch personMatch = SyncMatch.getMatch(localBase.extSystem(), localBase,
				Student.entityName, studentID);
		if(personMatch == null)
			return;
		String studentGuid = personMatch.extID();
		studentGuid = studentGuid.toUpperCase();
		if(itogTypes == null) {
			SyncIndex indexer = system.getIndexNamed("periods", null, false);
			itogTypes = indexer.getDict();
			if(itogTypes == null)
				itogTypes = NSDictionary.EmptyDictionary;
		}
		String type = (itogTypes.count() == 0)?null:MyUtility.getID(container.itogType());
		if(type != null) {
			type = (String)itogTypes.valueForKey(type);
		}
		if(type == null) {
			int count = container.itogType().inYearCount().intValue();
			if(count < 5)
				type = defaultTypes[count];
			else
				type = "Module";
		}
		int act = arch.actionType().intValue();
		if("Exam".equals(type) || "Final".equals(type)) {
			FinalMarkType ftype = FinalMarkType.fromString(type);
			if(act <= 1) {
				try {
					soap.insertFinalMark(groupGuid, subjectID, studentGuid,
							markType, ftype, value, bonus, null);
				} catch (RemoteException e) {
					if(!e.getMessage().contains("Entity already exists: FinalMark"))
						throw new AttributedRemoteException(e,
								new Object[] {groupGuid, subjectID, studentGuid,
										markType, ftype, value, bonus, "null"});
					soap.updateFinalMark(groupGuid, subjectID, studentGuid, 
							markType, ftype, value, bonus, null);
				}
			} else {
				try {
					soap.updateFinalMark(groupGuid, subjectID, studentGuid, 
							markType, ftype, value, bonus, null);
				} catch (RemoteException e) {
					if(!e.getMessage().contains("Entity not found: FinalMark"))
						throw new AttributedRemoteException(e,
								new Object[] {groupGuid, subjectID, studentGuid,
										markType, ftype, value, bonus, "null"});
					soap.insertFinalMark(groupGuid, subjectID, studentGuid,
							markType, ftype, value, bonus, null);
				}
			}
		} else {
			EduReportingPeriodType periodType = EduReportingPeriodType.fromString(type);
			UnsignedByte periodNumber = new UnsignedByte(container.num().intValue() -1);
			if(act <= 1) {
				try {
					soap.insertPeriodMark(groupGuid, subjectID, studentGuid,
							markType, periodType, periodNumber, value, bonus, null);
				} catch (RemoteException e) {
					if(!e.getMessage().contains("Entity already exists: Mark"))
						throw new AttributedRemoteException(e,
								new Object[] {groupGuid, subjectID, studentGuid, markType,
										periodType, periodNumber, value, bonus, "null"});
					soap.updatePeriodMark(groupGuid, subjectID, studentGuid,
							markType, periodType, periodNumber, value, bonus, null);
				}
			} else {
				try {
					soap.updatePeriodMark(groupGuid, subjectID, studentGuid, 
						markType, periodType, periodNumber, value, bonus, null);
				} catch (RemoteException e) {
					if(!e.getMessage().contains("Entity not found: Mark"))
						throw new AttributedRemoteException(e,
								new Object[] {groupGuid, subjectID, studentGuid, markType,
										periodType, periodNumber, value, bonus, "null"});
					soap.insertPeriodMark(groupGuid, subjectID, studentGuid, 
							markType, periodType, periodNumber, value, bonus, null);
				}
			}
		}
	}
	
	public int getTimeslot(MarkArchive arch) {
		EduCourse course = (EduCourse)EOUtilities.objectWithPrimaryKeyValue(ec,
				EduCourse.entityName, arch.getKeyValue("course"));
		Calendar cal = Calendar.getInstance();
		NSTimestamp date = (NSTimestamp)arch.valueForKey("@date_date");
		cal.setTime(date);
		String number = arch.getArchiveValueForKey("number");
		return getTimeslot(course, cal, Integer.parseInt(number));
	}
	
	public int getTimeslot(EduCourse course, Calendar cal, int num) {
		int result = cal.get(Calendar.DAY_OF_YEAR);
		while(cal.get(Calendar.YEAR) > eduYear.intValue()) {
			cal.add(Calendar.YEAR, -1);
			result += cal.getActualMaximum(Calendar.DAY_OF_YEAR);
		}
		result = (result << 5);
		result += num;
		EOKeyGlobalID gid = (EOKeyGlobalID)ec.globalIDForObject(course);
		Integer key = (Integer)gid.keyValues()[0];
		result += (key.intValue() << 15);
		return result;
	}
	
	public long[] parceIDs(String extid) {
		int idx = extid.indexOf(' ');
		long[] ids = new long[2];
		try {
			ids[0] = Long.parseLong(extid.substring(0,idx));
			ids[1] = Long.parseLong(extid.substring(idx +1));
		} catch (Exception e) {
			throw new IllegalArgumentException("Unsupported extid format: " + extid, e);
		}
		return ids;
	}

	public static class Timeslot implements Comparable {
		public EduCourse course;
		public short year;
		public byte month;
		public byte day;
		public byte number;
		public long lessonID;
		
		public Timeslot(EduCourse course, Calendar cal, UnsignedByte num) {
			this.course = course;
			year = (short)cal.get(Calendar.YEAR);
			month = (byte)cal.get(Calendar.MONTH);
			day = (byte)cal.get(Calendar.DATE);
			number = num.byteValue();
		}

		public boolean equals(Timeslot other) {
			return (course == other.course && number == other.number &&
					day == other.day && month == other.month && year == other.year);
		}

		public static BaseCourse.ComparisonSupport comparator = new BaseCourse.ComparisonSupport();
		public int compareTo(Object arg0) {
			Timeslot other = (Timeslot)arg0;
			if(year > other.year) return 1;
			if(year < other.year) return -1;
			if(month > other.month) return 1;
			if(month < other.month) return -1;
			if(day > other.day) return 1;
			if(day < other.day) return -1;
			if(number > other.number) return 1;
			if(number < other.number) return -1;
			return comparator.compareAscending(course, other.course);
		}
	}
	
	public static String assumeWorkType(WorkType type) {
		if(type.dfltFlags().equals(WorkType.specTypes.valueForKey("onLesson")))
			return "LessonBehavior";
		NamedFlags flags = type.namedFlags();
		if(flags.flagForKey("hometask"))
			return "LessonHomework";
		return null;
	}
}
