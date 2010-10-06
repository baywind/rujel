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

import net.rujel.reusables.*;
import net.rujel.interfaces.*;
import net.rujel.base.MyUtility;
import net.rujel.base.SettingsBase;
import net.rujel.reusables.WOLogLevel;
import net.rujel.eduplan.*;

import com.webobjects.foundation.*;
import com.webobjects.eocontrol.*;
import com.webobjects.eoaccess.EOUtilities;
import com.webobjects.appserver.*;

import java.lang.ref.WeakReference;
import java.util.logging.Logger;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.TimerTask;

import javax.mail.internet.InternetAddress;


public class EMailBroadcast implements Runnable{
	protected static Logger logger = Logger.getLogger("rujel.contacts");

	public static Object init(Object obj, WOContext ctx) {
		if (obj == null || obj.equals("init")) {
			try {
				Object access = PlistReader.readPlist("access.plist", "RujelContacts", null);
				WOApplication.application().takeValueForKey(access, "defaultAccess");
			} catch (NSKeyValueCoding.UnknownKeyException e) {
				// default access not supported
			}
		} else if(obj.equals("scheduleTask")) {
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
			MyUtility.scheduleTaskOnTime(task, time);
		} else if(obj.equals("overviewAction")) {
			return overviewAction(ctx);
		} else if(obj.equals("regimes")) {
			if(Various.boolForObject(ctx.session().valueForKeyPath("readAccess._read.Contacts")))
				return null;
			return WOApplication.application().valueForKeyPath(
					"strings.RujelContacts_Contacts.contactsRegime");
		} else if("journalPlugins".equals(obj)) {
			if(Various.boolForObject(ctx.session().valueForKeyPath("readAccess._read.SendMailForm")))
				return null;
			return ctx.session().valueForKeyPath("strings.RujelContacts_Contacts.dashboard");
		}
		return null;
	}
	
	public static NSArray overviewAction(WOContext ctx) {
		NSMutableArray result = new NSMutableArray();
		NamedFlags access = (NamedFlags)ctx.session().valueForKeyPath("readAccess.FLAGS.SendMailForm");
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
		if(period == null) {
			moment = new NSTimestamp();
		} else {
			java.util.Date fin = period.end();
			if(fin instanceof NSTimestamp)
				moment = (NSTimestamp)fin;
			else
				moment = new NSTimestamp(fin);
		}
		Integer eduYear = MyUtility.eduYearForDate(moment);
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
			if(listBase.byCourse() != null && listBase.byCourse().count() > 0) {
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
		WOApplication app = WOApplication.application();
		
		NSMutableDictionary dict = new NSMutableDictionary(eduYear,"eduYear");
		
		if(reporter == null)
				reporter = (NSDictionary)app.valueForKeyPath("strings.Strings.Overview.defaultReporter");
//		ec.unlock();
gr:		while (eduGroups.hasMoreElements()) {
			EduGroup eduGroup = (EduGroup)eduGroups.nextElement();
//			ec.lock();
			NSArray students = eduGroup.list();
			if(students == null || students.count() == 0)
				continue gr;
			dict.setObjectForKey(eduGroup,"eduGroup");
			NSArray existingCourses = EOUtilities.objectsMatchingValues(ec,EduCourse.entityName,dict);
			if(existingCourses == null || existingCourses.count() == 0)
				continue gr;
			NSMutableDictionary params = new NSMutableDictionary();
			params.takeValueForKey(students,"students");
			params.takeValueForKey(existingCourses,"courses");
			if(periodsByList != null) {
				EOEnterpriseObject bc = listBase.forValue(eduGroup, eduYear);
				String listName = (bc == null)?null:
					(String)bc.valueForKey(SettingsBase.TEXT_VALUE_KEY);
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
//			params.takeValueForKey(ctx,"ctx");
//			params.takeValueForKey(ec,"editingContext");
//			params.takeValueForKey(ses,"tmpsession");
//			ses.sleep();
			broadcastMarks(params);
		} // EduGroup
		} catch (Exception ex) {
			logger.log(WOLogLevel.WARNING,"Error broadcasting mail for period",
					new Object[] {period,ex});
		} finally {
			ec.unlock();
		}
		logger.log(WOLogLevel.INFO,"Mailing queued",period);
	}
	
		/*
	public static void broadcastMarksToList(NSArray students, Period period, NSDictionary reporter, String groupName, NSArray existingCourses, WOContext ctx) {
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
			/*running = new EMailBroadcast();
			synchronized (running) {*/
				queue = new NSMutableArray(params);
				Thread t = new Thread(new EMailBroadcast(),"EMailBroadcast");
				t.setPriority(Thread.MIN_PRIORITY + 1);
				t.start();
			//}
		} else {
			//synchronized (running) {
//			int pr = Thread.currentThread().getPriority();
				queue.addObject(params);
			//}
		}
	}

	protected static synchronized NSDictionary nextTask() {
		NSDictionary result = null;
		if(queue == null || queue.count() <= idx) {
			queue = null;
			idx = 0;
		} else {
			result = (NSDictionary)queue.objectAtIndex(idx);
			idx++;
		}
		return result;
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
			logger.log(WOLogLevel.WARNING,"Error in mail broadcasting",ex);
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
		NSArray existingCourses = (NSArray)params.valueForKey("courses");
		if(students == null || students.count() == 0)
			return;

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
					new NSTimestamp(period.end());
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
		if(ec==null)	
			ec = new SessionedEditingContext(ctx.session());
//		ec.lock();
		
		students = EOUtilities.localInstancesOfObjects(ec,students);
		existingCourses = EOUtilities.localInstancesOfObjects(ec,existingCourses);

		if(period instanceof EOEnterpriseObject) {
			period = (Period)EOUtilities.localInstanceOfObject(ec,(EOEnterpriseObject)period);
		}
		PerPersonLink contacts = Contact.getContactsForList(students,EMailUtiliser.conType(ec));
		if(contacts == null) {
//			ec.unlock();
//			ses.sleep();
			return;
		}
		
		NSDictionary reporter = (NSDictionary)params.valueForKey("reporter");
		String groupName = (String)params.valueForKey("groupName");
		
		long timeout = 0;
		try {
			String lag = SettingsReader.stringForKeyPath("mail.messageLag",null);
			if(lag != null)
				timeout = Long.parseLong(lag);
		} catch (Exception ex) {
			logger.log(WOLogLevel.INFO,"Failed to define message lag",ex);
			timeout = 0;
		}
		
		Mailer mailer = (Mailer)params.valueForKey("mailer");
		if(mailer == null)
			mailer = new Mailer();

		String text = null;
		if(reporter != null) {
			text = (String)reporter.valueForKey("winTitle");
			if(text == null)
				text = (String)reporter.valueForKey("title");
			if(text == null)
				text = (String)reporter.valueForKey("component");
			if(text == null)
				text = "";
		}
		StringBuffer textBuf = null;
		if(reporter != null) {
			textBuf = new StringBuffer(text);
			textBuf.append(' ');
			text = (String)WOApplication.application().valueForKeyPath(
			"strings.RujelContacts_Contacts.isInAttachment");
			textBuf.append(text);
			textBuf.append("\n    ---------------\n\n");
			//subject.append(" - ").append(title);
		}
		text = (String)params.valueForKey("messageText");
		if(text == null)
			text = mailer.defaultMessageText();
		if(reporter != null) {
			textBuf.append(text);
			text = textBuf.toString();
		}
		Object since = params.valueForKey("since");
		Object upTo = params.valueForKey("to");
		NSSet adrSet = (NSSet)params.valueForKey("adrSet");
		Enumeration stEnu = students.objectEnumerator();
st:		while (stEnu.hasMoreElements()) {
			long startTime = System.currentTimeMillis();
			Student student = (Student)stEnu.nextElement();
			NSArray stContacts = (NSArray)contacts.forPersonLink(student);
			stContacts = contactsInSet(stContacts, adrSet);
			if(stContacts == null || stContacts.count() == 0)
				continue st;
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
			InternetAddress[] to = EMailUtiliser.toAdressesFromContacts(cenu,adrSet != null);
			if(to == null || to.length == 0)
				continue st;
			synchronized (mailer) {
				try {
					StringBuffer subject = new StringBuffer("RUJEL: ");
					if(groupName != null) {
						subject.append(groupName).append(" : ");
					}
					subject.append(Person.Utility.fullName(student,true,2,2,0));
					String subj = (String)params.valueForKey("subject");
					if(subj != null && subj.length() > 0)
						subject.append(" : ").append(subj);
					if(reporter != null) {
						WOComponent reportPage = WOApplication.application().pageWithName("PrintReport",ctx);
						reportPage.takeValueForKey(reporter,"reporter");
						reportPage.takeValueForKey(existingCourses,"courses");
						reportPage.takeValueForKey(new NSArray(student),"students");
						reportPage.takeValueForKey(period,"period");
						reportPage.takeValueForKey(since,"since");
						reportPage.takeValueForKey(upTo,"to");
						mailer.sendPage(subject.toString(), text, reportPage, to, zip);
					} else {
						mailer.sendTextMessage(subject.toString(), text, to);
					}
					logger.finer("Mail sent \"" + subject + '"');
				} catch (Exception ex) {
					logger.log(WOLogLevel.WARNING,"Failed to send email for student",new Object[] {student,ex});
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
							logger.finest("Waiting timeout " + towait);
							mailer.wait(towait);
							towait = fin - System.currentTimeMillis();
						}
						t.setPriority(pr);
					} catch (Exception ex) {
						logger.logp(WOLogLevel.FINER,EMailBroadcast.class.getName(),
								"broadcastMarksForPeriod","Interrupted timeout between mails",ex);
					}
				}
			}
		} // Student
		String logMessage = (String)params.valueForKey("logMessage");
		Object logParam = params.valueForKey("logParam");
		if(logMessage != null)
			logger.log(WOLogLevel.FINE,logMessage,logParam);
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
