// EMailBroadcast.java

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

package net.rujel.contacts;

import net.rujel.reports.StudentReports;
import net.rujel.reusables.*;
import net.rujel.interfaces.*;
import net.rujel.io.XMLGenerator;
import net.rujel.base.MyUtility;
import net.rujel.base.Setting;
import net.rujel.base.SettingsBase;
import net.rujel.reusables.WOLogLevel;
import net.rujel.eduplan.*;
import net.rujel.email.Mailer;

import com.webobjects.foundation.*;
import com.webobjects.eocontrol.*;
import com.webobjects.eoaccess.EOUtilities;
import com.webobjects.appserver.*;

import java.lang.ref.WeakReference;
import java.text.FieldPosition;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.logging.Logger;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.TimerTask;

import javax.mail.internet.InternetAddress;


public class EMailBroadcast implements Runnable{
	public static final Logger logger = Logger.getLogger("rujel.contacts");

	public static Object init(Object obj, WOContext ctx) {
		if (obj == null || obj.equals("init")) {
			try {
				Object access = PlistReader.readPlist("access.plist", "RujelContacts", null);
				WOApplication.application().takeValueForKey(access, "defaultAccess");
			} catch (NSKeyValueCoding.UnknownKeyException e) {
				// default access not supported
			}
		} else if(obj.equals("scheduleTask")) {
			RequestMail.forgetSent();
			boolean dontBcast = Boolean.getBoolean("EMailBroadcast.disableWeekly");
			if(dontBcast)
				return null;
			int weekday = SettingsReader.intForKeyPath("mail.broadcastWeekday", -1);
			Calendar cal = Calendar.getInstance();
			if(cal.get(Calendar.DAY_OF_WEEK) != weekday)
				return null;
			String time = SettingsReader.stringForKeyPath("mail.broadcastTime", "5:30");
			TimerTask task = new TimerTask() {
				public void run() {
					broadcastMarksForPeriod(null, null);
				}
			};
			if(MyUtility.scheduleTaskOnTime(task, time))
				logger.log(WOLogLevel.FINE,"EMailBroadcast scheduled on " + time);
		} else if(obj.equals("overviewAction")) {
			return overviewAction(ctx);
		} else if(obj.equals("regimes")) {
			if(Various.boolForObject(ctx.session().valueForKeyPath("readAccess._read.Contacts")))
				return null;
			return WOApplication.application().valueForKeyPath(
					"strings.RujelContacts_Contacts.contactsRegime");
		} else if("journalPlugins".equals(obj)) {
			if(Various.boolForObject(ctx.session().valueForKeyPath(
					"readAccess._read.SendMailForm")))
				return null;
			return ctx.session().valueForKeyPath("strings.RujelContacts_Contacts.dashboard");
		}
		return null;
	}
	
	public static NSArray overviewAction(WOContext ctx) {
		NSMutableArray result = new NSMutableArray();
		NamedFlags access = (NamedFlags)ctx.session().valueForKeyPath(
				"readAccess.FLAGS.SendMailForm");
		if(access.getFlag(0)) {
			result.addObject(ctx.session().valueForKeyPath(
					"strings.RujelContacts_Contacts.sendmailAction"));
		}
		if(result.count() > 0)
			return result;
		return null;
	}
	
	public static void broadcastMarksForPeriod(Period period, NSDictionary reporter) {
//		WOContext ctx = MyUtility.dummyContext(null);
//		WOSession ses = ctx.session();
		NSTimestamp moment = null;
		Integer eduYear = (Integer)WOApplication.application().valueForKey("year");
		String defaultDate = SettingsReader.stringForKeyPath("ui.defaultDate", null);
		if(defaultDate != null) {
			try {
				moment = (NSTimestamp)MyUtility.dateFormat().parseObject(defaultDate);
			} catch (Exception e) {
				logger.log(WOLogLevel.WARNING, "Failed parsing default date " + 
						defaultDate + ". Using today.",e);
			}
		}
		if(moment == null)
			moment = MyUtility.dayInEduYear(eduYear.intValue());

		if(period != null && !period.contains(moment)) {
			java.util.Date fin = period.end();
			if(fin instanceof NSTimestamp)
				moment = (NSTimestamp)fin;
			else
				moment = new NSTimestamp(fin);
		}
		MyUtility.eduYearForDate(moment);
		EOEditingContext ec = null;
		if(period instanceof EOEnterpriseObject) {
			ec = ((EOEnterpriseObject)period).editingContext();
		} else if(SettingsReader.boolForKeyPath("dbConnection.yearTag", false)) {
			EOObjectStore os = DataBaseConnector.objectStoreForTag(eduYear.toString());
			ec = new EOEditingContext(os);
		} else {
			ec = new EOEditingContext();
		}
		/*
		ses.takeValueForKey(moment, "today");
		if(ec == null) {
			ec = ses.defaultEditingContext();
		}*/
		ec.lock();
		try {
			SettingsBase listBase = SettingsBase.baseForKey(EduPeriod.ENTITY_NAME, ec, false);
			NSMutableDictionary periodsByList = null;
		if(period == null) {
			String listName = listBase.textValue();
			period = EduPeriod.getCurrentPeriod(moment, listName, ec);
			if(!listBase.isSingle()) {
				if(period == null) {
					periodsByList = new NSMutableDictionary(NSKeyValueCoding.NullValue,listName);
				} else {
					periodsByList = new NSMutableDictionary(period,listName);
				}
			} else if (period == null) {
				return;
			}
		} else { // period != null
			if(period instanceof EOEnterpriseObject) {
				period = (Period)EOUtilities.localInstanceOfObject(ec,(EOEnterpriseObject)period);
			}
			
		} //end period selection
		logger.log(WOLogLevel.INFO,"Starting mailing for period",period);		
		//cycle groups and send mails
		
		Enumeration eduGroups = EduGroup.Lister.listGroups(moment,ec).objectEnumerator();
//		WOApplication app = WOApplication.application();
		
		NSMutableDictionary dict = new NSMutableDictionary(eduYear,"eduYear");
		
		if(reporter == null) {
			WOSession ses = null;
			if(ec instanceof SessionedEditingContext) 
				ses = ((SessionedEditingContext)ec).session();
			StudentReports reports = new StudentReports(ses);
			reporter = (NSDictionary)reports.defaultReporter();
		} // get default reporter
//		ec.unlock();
		idx = -1;
		SettingsBase reportCourses = SettingsBase.baseForKey("reportCourses", ec, false);
gr:		while (eduGroups.hasMoreElements()) {
			EduGroup eduGroup = (EduGroup)eduGroups.nextElement();
			if(reportCourses != null) {
				NSDictionary crs = SettingsBase.courseDict(eduGroup, eduYear);
				Integer num = reportCourses.forCourse(crs).numericValue();
				if(num != null && num.intValue() == 0)
					continue gr;
			}
//			ec.lock();
			NSArray students = eduGroup.list();
			if(students == null || students.count() == 0)
				continue gr;
			dict.setObjectForKey(eduGroup,"eduGroup");
			NSArray existingCourses = EOUtilities.objectsMatchingValues(ec,
					EduCourse.entityName,dict);
//			if(existingCourses == null || existingCourses.count() == 0)
//				continue gr;
			NSMutableDictionary params = new NSMutableDictionary();
			params.takeValueForKey(eduGroup, "eduGroup");
			params.takeValueForKey(students,"students");
			params.takeValueForKey(existingCourses,"courses");
			if(periodsByList != null) {
				Setting bc = listBase.forCourse(
						SettingsBase.courseDict(eduGroup, eduYear));
				String listName = (bc == null)?null:bc.textValue();
				if(listName != null) {
					Object per = periodsByList.objectForKey(listName);
					if(per == null) {
						per = EduPeriod.getCurrentPeriod(moment, listName, ec);
						if(per == null)
							per = NSKeyValueCoding.NullValue;
						periodsByList.setObjectForKey(per, listName);
					}
					if(per == NSKeyValueCoding.NullValue)
						continue gr;
					params.takeValueForKey(per,"period");
				} else {
					params.takeValueForKey(period,"period");
				}
			} else {
				params.takeValueForKey(period,"period");
			}
			params.takeValueForKey(reporter,"reporter");
			params.takeValueForKey(eduGroup.name(),"groupName");
			params.takeValueForKey("Finished mailing for eduGroup","logMessage");
			params.takeValueForKey(eduGroup,"logParam");
			params.takeValueForKey(moment, "date");
			params.takeValueForKey(Boolean.TRUE, "needData");
			if(SettingsReader.stringForKeyPath("ui.diaryURL", null) != null)
				params.takeValueForKey(Boolean.TRUE, "diaryLink");
//			params.takeValueForKey(ctx,"ctx");
//			params.takeValueForKey(ec,"editingContext");
//			params.takeValueForKey(ses,"tmpsession");
//			ses.sleep();
//			broadcastMarks(params);
			if(queue == null) {
				queue = new NSMutableArray(params);
			} else
			synchronized (queue) {
				queue.addObject(params);
			}
		} // EduGroup
		} catch (Exception ex) {
			logger.log(WOLogLevel.WARNING,"Error broadcasting mail for period",
					new Object[] {period,ex});
		} finally {
			ec.unlock();
			idx = 0;
			if(queue != null && queue.count() > 0) {
				Thread t = new Thread(new EMailBroadcast(),"EMailBroadcast");
				t.setPriority(Thread.MIN_PRIORITY + 1);
				t.start();
			}
		}
		logger.log(WOLogLevel.INFO,"Mailing queued",period);
	}
	
		/*
	public static void broadcastMarksToList(NSArray students, Period period, 
			NSDictionary reporter, String groupName, NSArray existingCourses, WOContext ctx) {
		NSMutableDictionary params = new NSMutableDictionary();
		params.takeValueForKey(students,"students");
		params.takeValueForKey(period,"period");
		params.takeValueForKey(reporter,"reporter");
		params.takeValueForKey(groupName,"groupName");
		params.takeValueForKey(existingCourses,"courses");
		params.takeValueForKey(ctx,"ctx");
		broadcastMarks(params);
	}
	*/
	//volatile protected static EMailBroadcast running = null;
	protected static volatile NSMutableArray queue = null;
	protected static volatile int idx = 0;
	
	public static synchronized void broadcastMarks(NSDictionary params) {
		if(queue == null) {
			queue = new NSMutableArray(params);
			if(idx < 0)
				return;
			synchronized (queue) {
				logger.log(WOLogLevel.FINEST, Thread.currentThread().getName() + '(' +
						+ Thread.currentThread().getId() + ") Starting queue",
						params.valueForKey("logParam"));
				Thread t = new Thread(new EMailBroadcast(),"EMailBroadcast");
				t.setPriority(Thread.MIN_PRIORITY + 1);
				t.start();
			}
		} else {
			synchronized (queue) {
				logger.log(WOLogLevel.FINEST, Thread.currentThread().getName() + '(' +
					+ Thread.currentThread().getId() + ") Adding to queue",
					params.valueForKey("logParam"));

				queue.addObject(params);
			}
		}
	}

	protected static synchronized NSDictionary nextTask() {
		if(queue == null)
			return null;
		synchronized(queue) {
			NSDictionary result = null;
			if(queue.count() <= idx) {
				queue = null;
				idx = 0;
				logger.log(WOLogLevel.FINEST, Thread.currentThread().getName() + '(' +
						+ Thread.currentThread().getId() + ") Closing queue");
				return null;
			} else {
				result = (NSDictionary)queue.objectAtIndex(idx);
				idx++;
				return result;
			}
		}
	}

	public void run() {
		try {
			//int tasksLeft = queue.count() - idx;
			NSDictionary params = nextTask();
			/*if(params != null) {
				ses = (WOSession)params.valueForKey("tmpsession");
				ses.awake();
			}*/
			while(params != null) {
				//NSDictionary params = (NSDictionary)queue.objectAtIndex(idx);
				doBroadcast(params);
				params = nextTask();
			}
			/*synchronized (running) {
				idx++;
				tasksLeft = queue.count() - idx;
			}
		}
		queue.removeAllObjects();
		idx = 0;
		queue = null;*/
		} catch (Throwable ex) {
			logger.log(WOLogLevel.WARNING,"Error in mail broadcasting",new Object[] {ses,ex});
			queue = null;
			idx = 0;
			/*
			Thread t = new Thread(new EMailBroadcast(),"EMailBroadcast");
			t.setPriority(Thread.MIN_PRIORITY + 1);
			t.start();*/
		}
		if(ses != null) {
//			ses.sleep();
			Object user = ses.valueForKey("user");
			if(user == null || user.toString().startsWith("DummyUser")) {
				//logger.log(WOLogLevel.SESSION, "Terminating Dummy mailing session", ses);
				ses.terminate();
			}
			ses = null;
		}
	}
	
	protected WOSession ses;
//	protected WOContext context;
	protected EOEditingContext ec;
	protected void doBroadcast(NSDictionary params) {
		NSArray students = (NSArray)params.valueForKey("students");
		if(students == null || students.count() == 0)
			return;
		logger.log(WOLogLevel.FINEST, Thread.currentThread().getName() + '(' +
				+ Thread.currentThread().getId() + ") Starting broadcast for",
				new Object[] {ses, params.valueForKey("logParam")});
		NSArray existingCourses = (NSArray)params.valueForKey("courses");

		Period period = (Period)params.valueForKey("period");
		WOContext ctx = (WOContext)params.valueForKey("ctx");
		if(ctx == null) {
			if(ctx == null) {
				ctx = MyUtility.dummyContext(ses);
			}
			if(ses == null) {
				ses = ctx.session();
				NSTimestamp date = (NSTimestamp)params.valueForKey("date");
				if(date == null)
					date = (NSTimestamp)params.valueForKey("to");
				if(date == null && period != null)
					date = new NSTimestamp(period.end());
				if(date != null)
					ses.takeValueForKey(date, "today");
//				ses.takeValueForKey(Boolean.TRUE,"dummyUser");
			} else {
				ctx._setSession(ses);
			}
			WeakReference sesRef = (WeakReference)params.valueForKey("callerSession");
			WOSession callerSession = (sesRef == null)?null:(WOSession)sesRef.get();
			if(callerSession != null) {
				Object reportSettings = callerSession.objectForKey("reportSettingsForStudent");
				if(reportSettings != null)
					ses.setObjectForKey(reportSettings, "reportSettingsForStudent");
			}
		}
		
		if(ec==null)
			ec = (EOEditingContext)params.valueForKey("editingContext");
		if(ec !=null && !(ec instanceof SessionedEditingContext))
			ec = null;
		if(ec instanceof SessionedEditingContext) {
			Object user = ((SessionedEditingContext)ec).session().valueForKey("user");
			if(user == null || !user.toString().startsWith("DummyUser"))
				ec = null;
		}
		if(ec == null) {
			EOObjectStore os = (EOObjectStore)ses.objectForKey("objectStore"); 
			os = EOObjectStoreCoordinator.defaultCoordinator();
			ec = new SessionedEditingContext(os,ctx.session());
			params.takeValueForKey(ec, "editingContext");
		}
		students = EOUtilities.localInstancesOfObjects(ec,students);
		params.takeValueForKey(students, "students");

		existingCourses = EOUtilities.localInstancesOfObjects(ec,existingCourses);

		if(period instanceof EOEnterpriseObject) {
			period = (Period)EOUtilities.localInstanceOfObject(ec,(EOEnterpriseObject)period);
		}
		PerPersonLink contacts = Contact.getContactsForList(students,
				EMailUtiliser.conType(ec),Boolean.TRUE);
		if(contacts == null) {
//			ec.unlock();
//			ses.sleep();
			logger.log(WOLogLevel.FINEST, Thread.currentThread().getName() + '(' +
					+ Thread.currentThread().getId() + ") No contacts found",
					new Object[] {ses, params.valueForKey("logParam")});
			return;
		}
		
		NSDictionary reporter = (NSDictionary)params.valueForKey("reporter");
		if(reporter != null && reporter.count() == 0) {
			StudentReports reports = new StudentReports(ses);
			reporter = (NSDictionary)reports.defaultReporter();
		}
		String groupName = (String)params.valueForKey("groupName");
		
		long timeout = 0;
		try {
			String lag = SettingsReader.stringForKeyPath("mail.messageLag",null);
			if(lag != null)
				timeout = Long.parseLong(lag);
		} catch (Exception ex) {
			logger.log(WOLogLevel.INFO,"Failed to define message lag",new Object[] {ses,ex});
			timeout = 0;
		}
		
		Mailer mailer = (Mailer)params.valueForKey("mailer");
		if(mailer == null) {
			mailer = new Mailer();
			logger.log(WOLogLevel.FINEST,"Mailer instantiated",ses);
		} else {
			mailer.extraHeaders.removeAllObjects();
		}
		mailer.extraHeaders.takeValueForKey(params.valueForKey("user"), "X-rujel-user");
		StringBuffer textBuf = new StringBuffer();
		boolean allowRequest = false;
		if(reporter != null) {
			String text = (String)reporter.valueForKey("winTitle");
			if(text == null)
				text = (String)reporter.valueForKey("title");
			if(text == null)
				text = (String)reporter.valueForKey("component");
			if(text != null)
				textBuf.append(text);
			textBuf.append(' ');
			textBuf.append(WOApplication.application().valueForKeyPath(
					"strings.RujelContacts_Contacts.isInAttachment"));
			textBuf.append("\n    ---------------\n\n");
			allowRequest = !SettingsReader.boolForKeyPath("mail.denyRequesting", false);
		}
		String messageText = (String)params.valueForKey("messageText");
		if(messageText == null)
			messageText = mailer.defaultMessageText();
		if(messageText != null)
			textBuf.append(messageText.replaceAll("%", "%%"));
		messageText = (String)params.valueForKey("sign");
		if(messageText != null) {
			textBuf.append("\n\n---\n").append(messageText);
		}
		WOApplication app = WOApplication.application();
		EduGroup eduGroup = (EduGroup)params.valueForKey("eduGroup");
		eduGroup = (EduGroup)EOUtilities.localInstanceOfObject(ec, eduGroup);
		if(Various.boolForObject(params.valueForKey("diaryLink"))) {
			textBuf.append("\n\n");
			String text = (String)app.valueForKeyPath(
				"strings.RujelContacts_Contacts.diaryLink");
			textBuf.append(text);
			textBuf.append(":\n").append(app.valueForKey("diaryUrl"));
			EOKeyGlobalID gid = (EOKeyGlobalID)ec.globalIDForObject(eduGroup);
			textBuf.append("?grID=").append(gid.keyValues()[0]).append("&studentID=%1$d");
		}
		if(allowRequest) {
//			String auto = ctx.completeApplicationURLPrefix(false, 0);
			textBuf.append("\n\n");
			textBuf.append(app.valueForKeyPath(
					"strings.RujelContacts_Contacts.MailRequest.title"));
			textBuf.append(":\n").append(app.valueForKey("serverUrl"));
			textBuf.append(app.valueForKey("urlPrefix")).append('/');
			textBuf.append(app.directActionRequestHandlerKey()).append('/');
			textBuf.append("RequestMail/%1$d");
		}
		
		int textLength = textBuf.length();
		logger.log(WOLogLevel.FINEST,"Message text prepared",ses);
		ses.setObjectForKey(params, "broadcastAdditions");
		NSArray extensions = (NSArray)ses.valueForKeyPath("modules.broadcastAdditions");
		ses.removeObjectForKey("broadcastAdditions");
		logger.log(WOLogLevel.FINEST,"Found " + extensions.count() + 
				" broadcastAdditions",ses);
		if(extensions == null || extensions.count() == 0)
			textBuf.append("\n.\n");
		NSMutableDictionary settings = new NSMutableDictionary();
		settings.takeValueForKey(eduGroup, "eduGroup");
		settings.takeValueForKey(existingCourses,"courses");
		settings.takeValueForKey(period,"period");
		Object since = params.valueForKey("since");
		settings.takeValueForKey(since,"since");
		Object to = params.valueForKey("to");
		settings.takeValueForKey(to,"to");
		boolean xml = (reporter != null && reporter.valueForKey("component") == null);
		if(xml && !Various.boolForObject(reporter.valueForKeyPath("settings.courses.hidden"))) {
			SettingsBase reportCourses = SettingsBase.baseForKey("reportCourses", ec, false);
			settings.takeValueForKey(reportCourses, "reportCourses");
		}
		byte[] xmlData = null;
		if(xml && Various.boolForObject(reporter.valueForKey("studentInOptions"))) {
			settings.takeValueForKey(students,"students");
			NSMutableDictionary counters = new NSMutableDictionary();
			settings.takeValueForKey(counters, "counters");
			try {
				xmlData = XMLGenerator.generate(ses, settings);
				if(counters.count() == 0)
					return;
				/*
				//testfile
				String mailDir = SettingsReader.stringForKeyPath("mail.writeFileDir", null);
				mailDir = Various.convertFilePath(mailDir);
				StringBuilder fileName = new StringBuilder("_data_");
				if(eduGroup == null)
					fileName.append("noGroup");
				else
					fileName.append(eduGroup.name());
				fileName.append(".xml");
				File messageFile = new File(mailDir,fileName.toString());
				FileOutputStream fos = new FileOutputStream(messageFile);
				fos.write(xmlData);
				fos.close(); */
			} catch (Exception e) {
				logger.log(WOLogLevel.WARNING,"Failed to generate group xmlData", 
						new Object[] {ses,settings,e});
			}
			settings.takeValueForKey(null, "students");
			NSMutableDictionary info = new NSMutableDictionary(MyUtility.presentEduYear(
					(Integer)ses.valueForKey("eduYear")), "eduYear");
			if(period instanceof EOPeriod)
				info.takeValueForKey(((EOPeriod)period).valueForKey("name"), "period");
			if(since != null || to != null) {
				Format dateFormat = MyUtility.dateFormat();
				StringBuffer buf = new StringBuffer();
				FieldPosition fp = new FieldPosition(0);
				if(since != null)
					dateFormat.format(since, buf, fp);
				else
					buf.append("...");
				buf.append(" - "); 
				if(to != null)
					dateFormat.format(to, buf, fp);
				else
					buf.append("...");
				info.takeValueForKey(buf.substring(idx), "dates");
			}
			settings.takeValueForKey(info, "info");
		}
		settings.takeValueForKey(reporter,"reporter");
		NSSet adrSet = (NSSet)params.valueForKey("adrSet");
		Enumeration stEnu = students.objectEnumerator();
st:		while (stEnu.hasMoreElements()) {
			long startTime = System.currentTimeMillis();
			Student student = (Student)stEnu.nextElement();
			NSArray stContacts = (NSArray)contacts.forPersonLink(student);
			stContacts = contactsInSet(stContacts, adrSet);
			if(stContacts == null || stContacts.count() == 0) {
				logger.log(WOLogLevel.FINER,
						"Skipping mail to student as no contacts found",new Object[] {ses,student});
				continue st;
			}
			Enumeration cenu = stContacts.objectEnumerator();
			boolean zip = false;
			while (cenu.hasMoreElements()) {
				Contact cnt = (Contact) cenu.nextElement();
				Integer flags = cnt.flags();
				if(flags == null)
					continue;
				int fl = flags.intValue();
				if(adrSet == null) {
					if((fl & 1) == 0 || (fl & 32) != 0)
						continue;					
				}
				zip = ((fl & 2) > 0);
				if(zip)
					break;
			}
			cenu = stContacts.objectEnumerator();
			InternetAddress[] toAddr = EMailUtiliser.toAdressesFromContacts(cenu,adrSet != null);
			if(toAddr == null || toAddr.length == 0) {
				logger.log(WOLogLevel.FINER,
						"Skipping mail to student as no active adresses found",
						new Object[] {ses,student});
				continue st;
			}
			synchronized (mailer) {
				try {
					NSData attach = null;
					String attName = null;
					if(reporter != null) {
						ctx.setUserInfoForKey(params.valueForKey("needData"), "needData");
						settings.takeValueForKey(student,"student");
						SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
						attName = sdf.format(new Date());
						if(xml) {
							try {
								if(xmlData == null) {
									NSMutableDictionary counters = new NSMutableDictionary();
									settings.takeValueForKey(counters, "counters");
									byte[] res = XMLGenerator.generate(ses, settings);
									attach = new NSData(res);
									Counter cnt = (Counter)counters.objectForKey(student);
									if(cnt == null || cnt.intValue() == 0)
										continue;
								} else {
									NSDictionary counters = (NSDictionary)
												settings.valueForKey("counters");
									Counter cnt = (Counter)counters.objectForKey(student);
									if(cnt == null || cnt.intValue() == 0)
										continue;
									byte[] res = XMLGenerator.transformData(ses, settings, xmlData);
									attach = new NSData(res);
								}
								String ext = (String)reporter.valueForKey("filext");
								if(ext == null)
									ext = ".html";
								if(ext.charAt(0) == '.')
									attName = attName + ext;
								else
									attName = attName + '.' + ext;
							} catch (Exception e) {
								logger.log(WOLogLevel.WARNING,"Failed to generate attachment", 
										new Object[] {ses,settings,e});
							}
						} else {
							WOComponent reportPage = app.pageWithName("PrintReport",ctx);
							reportPage.takeValueForKey(settings, "settings");
							attach = reportPage.generateResponse().content();
							if(ctx.userInfoForKey("needData") != null) {
								attach = null;
								if(!Various.boolForObject(params.valueForKey("sendEmpty"))) {
									logger.log(WOLogLevel.FINER,
											"Skipping mail to student as no data fond",
											new Object[] {ses,student});
									continue;
								}
							}
							attName = attName + ".html";
						}
					}
					StringBuffer subject = new StringBuffer("RUJEL: ");
					if(groupName != null) {
						subject.append(groupName).append(" : ");
					}
					subject.append(Person.Utility.fullName(student,true,2,2,0));
					String subj = (String)params.valueForKey("subject");
					if(subj != null && subj.length() > 0)
						subject.append(" : ").append(subj);
					EOKeyGlobalID stID = (EOKeyGlobalID)ec.globalIDForObject(student);
					if(extensions != null && extensions.count() > 0) { // append extensions
						textBuf.delete(textLength, textBuf.length());
						Enumeration exts = extensions.objectEnumerator();
						while (exts.hasMoreElements()) {
							NSDictionary ext = (NSDictionary) exts.nextElement();
							String text = (String)ext.objectForKey(stID);
							if(text == null)
								text = (String)ext.valueForKey("text");
							if(text == null)
								continue;
							textBuf.append('\n').append('\n').append(text);
						}
						textBuf.append("\n.\n");
					}
					String message = String.format(textBuf.toString(), stID.keyValues()[0],
							Person.Utility.fullName(student, false, 2, 2, 0));

					if(attach != null) {
						if(zip) {
							try {
								attach = Mailer.zip(attach,attName);
							} catch (Exception e) {
								logger.log(WOLogLevel.WARNING,"Error zipping message",
										new Object[] {ses,subject,e});
							}
							attName = attName + ".zip";
						}
						mailer.sendMessage(subject.toString(), message, toAddr, attach, attName);
					} else {
						mailer.sendTextMessage(subject.toString(), message, toAddr);
					}
					logger.log(WOLogLevel.FINEST,"Mail sent \"" + subject + '"',ses);
				} catch (Exception ex) {
					logger.log(WOLogLevel.WARNING,"Failed to send email for student",
							new Object[] {ses,student,ex});
					WeakReference sesRef = (WeakReference)params.valueForKey("callerSession");
					WOSession callerSession = (sesRef == null)?null:(WOSession)sesRef.get();
					if(callerSession != null) {
						StringBuffer message = new StringBuffer((String)WOApplication.application().
								valueForKeyPath("strings.RujelContacts_Contacts.failedMailing"));
						message.append(Person.Utility.fullName(student, false, 2, 2, 0));
						callerSession.takeValueForKey(message.toString(), "message");
					}
				}

				if(timeout > 0) {
					long fin = startTime + timeout;
					long towait = fin - System.currentTimeMillis();
					try {
						Thread t = Thread.currentThread();
						int pr = t.getPriority();
						t.setPriority(Thread.MIN_PRIORITY);
						while(towait > 0) {
							logger.log(WOLogLevel.FINEST,"Waiting timeout " + towait,ses);
							mailer.wait(towait);
							towait = fin - System.currentTimeMillis();
						}
						t.setPriority(pr);
					} catch (Exception ex) {
						logger.log(WOLogLevel.FINER,"Interrupted timeout between mails",
								new Object[] {ses,ex});
					}
				}
			}
		} // Student
		String logMessage = (String)params.valueForKey("logMessage");
		Object logParam = params.valueForKey("logParam");
		if(logMessage != null)
			logger.log(WOLogLevel.FINE,logMessage,new Object[] {ses,logParam});
//		ec.unlock();
//		ses.sleep();
	}

	protected NSArray contactsInSet(NSArray contacts, NSSet set) {
		if(contacts == null || contacts.count() == 0 || set == null)
			return contacts;
		if(set.count() == 0)
			return NSArray.EmptyArray;
		NSMutableArray result = contacts.mutableClone();
		Enumeration enu = contacts.objectEnumerator();
		while (enu.hasMoreElements()) {
			Contact obj = (Contact) enu.nextElement();
			if(!set.containsObject(ec.globalIDForObject(obj)))
				result.removeObject(obj);
		}
		return result;
	}

}
