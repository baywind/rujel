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
import net.rujel.reusables.WOLogLevel;
import net.rujel.eduresults.*;
//import er.javamail.*;

import com.webobjects.foundation.*;
import com.webobjects.eocontrol.*;
import com.webobjects.eoaccess.EOUtilities;
import com.webobjects.appserver.*;

import java.lang.reflect.Method;
import java.util.logging.Logger;
import java.util.Enumeration;

import javax.mail.internet.InternetAddress;


public class EMailBroadcast implements Runnable{
	protected static Logger logger = Logger.getLogger("rujel.contacts");

	public static Object init(Object obj, WOContext ctx) {
		if (obj == null) {
			return null;
		} else if(obj.equals("scheduleTask")) {
			boolean dontBcast = Boolean.getBoolean("EMailBroadcast.disableWeekly");
			if(dontBcast)
				return null;
			try {
				Method method = EMailBroadcast.class.getMethod("broadcastMarks",(Class[])null);
				Scheduler.sharedInstance().registerTask(Scheduler.WEEKLY,method,
						null,null,"EMailBroadcast.weekly");
			} catch (Exception ex) {
				logger.log(WOLogLevel.WARNING,"Failed to schedule mail broadcast",ex);
			}
/*		} else if (obj instanceof Period.Week) {
			EOEditingContext ec = new EOEditingContext();
			NSArray pertypes = PeriodType.allPeriodTypes(ec,new Integer(2007));
			PeriodType prt = null;
			Enumeration enu = pertypes.objectEnumerator();
			while (enu.hasMoreElements()) {
				prt = (PeriodType)enu.nextElement();
				if(prt.inYearCount().intValue() == 3) {
					break;
				}
			}
			EduPeriod per = prt.currentPeriod(new NSTimestamp(((Period)obj).begin()));
			broadcastMarksForPeriod(per,null);
*/		} else if(obj.equals("overviewAction")) {
			return overviewAction(ctx);
		} else if(obj.equals("regimes")) {
			if(Various.boolForObject(ctx.session().valueForKeyPath("readAccess._read.Contacts")))
				return null;
			return WOApplication.application().valueForKeyPath(
					"strings.RujelContacts_Contacts.contactsRegime");
		}
		return null;
	}
	
	public static NSArray overviewAction(WOContext ctx) {
		NSMutableArray result = new NSMutableArray();
		NamedFlags access = (NamedFlags)ctx.session().valueForKeyPath("readAccess.FLAGS.SendMailForm");
		if(access.getFlag(0)) {
			result.addObject(WOApplication.application().valueForKeyPath(
					"strings.RujelContacts_Contacts.sendmailAction"));
		}
		if(result.count() > 0)
			return result;
		return null;
	}

	public static void broadcastMarks() {
		broadcastMarksForPeriod(null,null,(EOEditingContext)null);
	}
	
	public static void broadcastMarksForPeriod(Period period, NSDictionary reporter,
			EOEditingContext ec) {
		//EOEditingContext ec = period.editingContext();//new EOEditingContext();
		logger.log(WOLogLevel.INFO,"Starting mailing for period",period);
	/*	
		long timeout = 0;
		try {
			String lag = SettingsReader.stringForKeyPath("mail.messageLag",null);
			if(lag != null)
				timeout = Long.parseLong(lag);
		} catch (Exception ex) {
			logger.log(WOLogLevel.INFO,"Failed to define message lag",ex);
			timeout = 0;
		}
		
		ERMailDeliveryHTML mail = new ERMailDeliveryHTML ();*/
		WOContext ctx = dummyContext(null);
		WOSession ses = null;
		if(ec == null) {
			ses = ctx.session(); 
			if(ses.valueForKey("user") == null)
			   ses.takeValueForKey(Boolean.TRUE,"dummyUser");
			ec = new SessionedEditingContext(ses);
		}
		ec.lock();

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
		Number eduYear = MyUtility.eduYearForDate(moment);

		// select period 
		
		Period defaultPeriod = null;
		NSMutableDictionary periodsByGroup = null;
		if(period == null) {
			NSArray typeUsage = EOUtilities.objectsWithQualifierFormat(ec,"PeriodTypeUsage",
					"(eduYear = %d OR eduYear = 0) AND eduGroup = nil AND course = nil",
								new NSArray(eduYear));
			if(typeUsage == null || typeUsage.count() == 0) {
				defaultPeriod = new Period.ByDates(MyUtility.yearStart(eduYear.intValue()),moment);
				
			} else {
				PeriodType pertype = null;
				if(typeUsage.count() > 1) {
					typeUsage = PeriodType.filterTypeUsageArray(typeUsage,eduYear);
					typeUsage = (NSArray)typeUsage.valueForKey("periodType");
					NSMutableArray res = (typeUsage instanceof NSMutableArray)?(NSMutableArray)typeUsage:typeUsage.mutableClone();
					EOSortOrdering so = EOSortOrdering.sortOrderingWithKey("inYearCount",EOSortOrdering.CompareDescending);
					EOSortOrdering.sortArrayUsingKeyOrderArray(res,new NSArray(so));
					typeUsage = res;
					pertype = (PeriodType)typeUsage.objectAtIndex(0);
				} else {
					pertype = (PeriodType)((EOEnterpriseObject)typeUsage.objectAtIndex(0)).valueForKey("periodType");
				}
				defaultPeriod = pertype.currentPeriod(moment);
			}
			typeUsage = EOUtilities.objectsWithQualifierFormat(ec,"PeriodTypeUsage", "(eduYear = %d OR eduYear = 0) AND eduGroup != nil AND course = nil",new NSArray(eduYear));
			if(typeUsage != null && typeUsage.count() > 0) {
				typeUsage = PeriodType.filterTypeUsageArray(typeUsage,eduYear);
				periodsByGroup = new NSMutableDictionary();
				Enumeration enu = typeUsage.objectEnumerator();
				while (enu.hasMoreElements()) {
					EOEnterpriseObject cur = (EOEnterpriseObject)enu.nextElement();
					EduGroup gr = (EduGroup)cur.valueForKey("eduGroup");
					PeriodType pt = (PeriodType)cur.valueForKey("periodType");
					EduPeriod per = (EduPeriod)periodsByGroup.objectForKey(gr);
					if(per != null) {
						if(per.periodType().inYearCount().intValue() > pt.inYearCount().intValue())
							continue;
					}
					per = pt.currentPeriod(moment);
					periodsByGroup.setObjectForKey(per,gr);
				}
			}
		} else { // period != null
			if(period instanceof EOEnterpriseObject) {
				period = (Period)EOUtilities.localInstanceOfObject(ec,(EOEnterpriseObject)period);
			}
			
		} //end period selection
		
		//cycle groups and send mails
		
		Enumeration eduGroups = EduGroup.Lister.listGroups(moment,ec).objectEnumerator();
		WOApplication app = WOApplication.application();
		
		NSMutableDictionary dict = new NSMutableDictionary(eduYear,"eduYear");
		
		if(reporter == null)
				reporter = (NSDictionary)app.valueForKeyPath("strings.Strings.Overview.defaultReporter");
		ec.unlock();
gr:		while (eduGroups.hasMoreElements()) {
			EduGroup eduGroup = (EduGroup)eduGroups.nextElement();
			ec.lock();
			NSArray students = eduGroup.list();
			if(students == null || students.count() == 0)
				continue gr;
			/*
			PerPersonLink contacts = Contact.getContactsForList(students,EMailUtiliser.conType(ec));
			if(contacts == null)
				continue gr;*/
			dict.setObjectForKey(eduGroup,"eduGroup");
			NSArray existingCourses = EOUtilities.objectsMatchingValues(ec,EduCourse.entityName,dict);
			
			if(defaultPeriod != null) {
				if(periodsByGroup != null) {
					period = (Period)periodsByGroup.objectForKey(eduGroup);
				}
				if(period == null) {
					period = defaultPeriod;
				}
			}
//			broadcastMarksToList(students,period,reporter,eduGroup.name(),existingCourses,ctx);
			NSMutableDictionary params = new NSMutableDictionary();
			params.takeValueForKey(students,"students");
			params.takeValueForKey(period,"period");
			params.takeValueForKey(reporter,"reporter");
			params.takeValueForKey(eduGroup.name(),"groupName");
			params.takeValueForKey(existingCourses,"courses");
			params.takeValueForKey(ctx,"ctx");
			params.takeValueForKey("Finished mailing for eduGroup","logMessage");
			params.takeValueForKey(eduGroup,"logParam");
			params.takeValueForKey(ec,"editingContext");
			params.takeValueForKey(ses,"tmpsession");
			ec.unlock();
			broadcastMarks(params);
			/*
			Enumeration stEnu = students.objectEnumerator();
st:			while (stEnu.hasMoreElements()) {
				long startTime = System.currentTimeMillis();
				Student student = (Student)stEnu.nextElement();
				NSArray stContacts = (NSArray)contacts.forPersonLink(student);
				if(stContacts == null || stContacts.count() == 0)
					continue st;
				stContacts = EMailUtiliser.toAdressesFromContacts(stContacts,false);
				if(stContacts == null || stContacts.count() == 0)
					continue st;
				//mail.setHiddenPlainTextContent(text);
				
				//prepare HTML page for email
				WOComponent reportPage = app.pageWithName("PrintReport",ctx);
				reportPage.takeValueForKey(reporter,"reporter");
				reportPage.takeValueForKey(existingCourses,"courses");
				reportPage.takeValueForKey(new NSArray(student),"students");
				reportPage.takeValueForKey(period,"period");
				int mailsCount = 0;
				synchronized (mail) {
					mail.newMail();
					try {
						mail.setComponent(reportPage);
						String subj = "RUJEL: " + eduGroup.name() + " : " + Person.Utility.fullName(student.person(),true,2,2,0) + " - " + reportPage.valueForKey("title");
						mail.setSubject(subj);	
						mailsCount = EMailUtiliser.sendToContactList(mail,stContacts);					
						logger.finer("Mail sent \"" + subj + '"');
					} catch (Exception ex) {
						logger.log(WOLogLevel.WARNING,"Failed to send email for student",new Object[] {student,ex});
					}
					
					if(timeout > 0 && mailsCount > 0) {
						long fin = startTime + timeout;
						long towait = fin - System.currentTimeMillis();
						while(towait > 0) {
							try {
								logger.finest("Waiting timeout " + towait);
								mail.wait(towait);
							} catch (Exception ex) {
								logger.logp(WOLogLevel.FINER,EMailBroadcast.class.getName(),"broadcastMarksForPeriod","Interrupted timeout between mails",ex);
							}
							towait = fin - System.currentTimeMillis();
						}
					}
				}
			} // Student
			
			logger.log(WOLogLevel.FINE,"Finished mailing for eduGroup",eduGroup);*/
		} // EduGroup
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
			if(params != null) {
				ses = (WOSession)params.valueForKey("tmpsession");
			}
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
			if(ses != null) {
				Object user = ses.valueForKey("user");
				if(user == null || user.toString().startsWith("DummyUser")) {
					//logger.log(WOLogLevel.SESSION, "Terminating Dummy mailing session", ses);
					ses.terminate();
				}
				ses = null;
			}
		} catch (Throwable ex) {
			logger.log(WOLogLevel.WARNING,"Error in mail broadcasting",ex);
			queue = null;
			idx = 0;
			/*
			Thread t = new Thread(new EMailBroadcast(),"EMailBroadcast");
			t.setPriority(Thread.MIN_PRIORITY + 1);
			t.start();*/
		}
	}
	
	protected WOSession ses;
	protected WOContext context;
	protected EOEditingContext ec;
	protected void doBroadcast(NSDictionary params) {
		NSArray students = (NSArray)params.valueForKey("students");
		NSArray existingCourses = (NSArray)params.valueForKey("courses");
		if(students == null || students.count() == 0)
			return;

		WOContext ctx = (WOContext)params.valueForKey("ctx");
		if(ctx == null) {
			if(context == null)
				context = dummyContext(ses);
			ctx = context;
			if(ses == null) {
				ses = ctx.session();
				ses.takeValueForKey(Boolean.TRUE,"dummyUser");
			}
		}
		
		if(ec==null)
			ec = (EOEditingContext)params.valueForKey("editingContext");
		if(ec==null) {
			ec = new SessionedEditingContext(ctx.session());//((EOEnterpriseObject)students.objectAtIndex(0)).editingContext();
		}
		
		ec.lock();
		
		students = EOUtilities.localInstancesOfObjects(ec,students);
		existingCourses = EOUtilities.localInstancesOfObjects(ec,existingCourses);

		Period period = (Period)params.valueForKey("period");
		if(period instanceof EOEnterpriseObject) {
			period = (Period)EOUtilities.localInstanceOfObject(ec,(EOEnterpriseObject)period);
		}
		PerPersonLink contacts = Contact.getContactsForList(students,EMailUtiliser.conType(ec));
		if(contacts == null) {
			ec.unlock();
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
			InternetAddress[] to = EMailUtiliser.toAdressesFromContacts(stContacts,adrSet != null);
			if(to == null || to.length == 0)
				continue st;
			synchronized (mailer) {
				try {
					StringBuffer subject = new StringBuffer("RUJEL: ");
					if(groupName != null) {
						subject.append(groupName).append(" : ");
					}
					subject.append(Person.Utility.fullName(student.person(),true,2,2,0));
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
						mailer.sendPage(subject.toString(), text, reportPage, to);
					} else {
						mailer.sendTextMessage(subject.toString(), text, to);
					}
					logger.finer("Mail sent \"" + subject + '"');
				} catch (Exception ex) {
					logger.log(WOLogLevel.WARNING,"Failed to send email for student",new Object[] {student,ex});
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
		ec.unlock();
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

	public static WOContext dummyContext(WOSession ses) {
		WOApplication app = WOApplication.application();
		String dummyUrl = app.cgiAdaptorURL() + "/" + app.name() + ".woa/wa/dummy";
		if(ses != null) {
			dummyUrl = dummyUrl + "?wosid=" + ses.sessionID();
		}
		WORequest request = app.createRequest( "GET", dummyUrl, "HTTP/1.0", null, null, null);
		WOContext context = app.createContextForRequest (request);
		//context._generateCompleteURLs ();
		return context;
	}


}
