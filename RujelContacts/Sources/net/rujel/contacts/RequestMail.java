package net.rujel.contacts;

import net.rujel.base.MyUtility;
import net.rujel.base.SettingsBase;
import net.rujel.eduplan.EduPeriod;
import net.rujel.interfaces.EduCourse;
import net.rujel.interfaces.EduGroup;
import net.rujel.interfaces.Student;
import net.rujel.reusables.Period;
import net.rujel.reusables.SettingsReader;
import net.rujel.reusables.WOLogLevel;

import com.webobjects.appserver.*;
import com.webobjects.eoaccess.EOObjectNotAvailableException;
import com.webobjects.eoaccess.EOUtilities;
import com.webobjects.eocontrol.EOEditingContext;
import com.webobjects.eocontrol.EOEnterpriseObject;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSDictionary;
import com.webobjects.foundation.NSMutableDictionary;
import com.webobjects.foundation.NSMutableSet;
import com.webobjects.foundation.NSTimestamp;

public class RequestMail extends WODirectAction {

	public RequestMail (WORequest aRequest) {
		super(aRequest);
	}
	
	protected static NSMutableSet sent;
	public static void forgetSent() {
		sent = new NSMutableSet();
	}

	public WOActionResults performActionNamed(String anActionName) {
		if(sent == null || SettingsReader.boolForKeyPath("mail.denyRequesting", false))
			return error("denied");
		if(anActionName.equals("default"))
			anActionName = request().stringFormValueForKey("code");
		if(anActionName == null)
			return pageWithName("MailRequest");
		Integer studentID = null;
		try {
			studentID = Integer.valueOf(anActionName, 10);
		} catch (NumberFormatException e) {
			return error("wrongCode");
		}
		String mail = request().stringFormValueForKey("addr");
		if(mail == null) {
			WOComponent page = pageWithName("MailRequest");
			page.takeValueForKey(anActionName, "code");
			return page;
		}
		String id = mail.toLowerCase() + studentID;
		if(sent.containsObject(id)) {
			return error("duplicate");
		}
		
		NSMutableDictionary params = new NSMutableDictionary();
		NSTimestamp date = new NSTimestamp();
		params.takeValueForKey(date, "date");
		params.takeValueForKey(WOApplication.application().valueForKeyPath(
						"strings.Strings.Overview.defaultReporter"), "reporter");
		
		EOEditingContext ec = new EOEditingContext();
		ec.lock();
		try {
			Student student = (Student)EOUtilities.objectWithPrimaryKeyValue(ec, 
					Student.entityName, studentID);
			params.takeValueForKey(ec, "editingContext");
			params.takeValueForKey(new NSArray(student), "students");
			EduGroup gr = student.recentMainEduGroup();
			{
				NSMutableDictionary dict = new NSMutableDictionary();
				dict.takeValueForKey(MyUtility.eduYearForDate(date), "eduYear");
				if(gr != null) {
					params.takeValueForKey(gr.name(), "groupName");
					dict.takeValueForKey(gr,"eduGroup");
					params.takeValueForKey(EOUtilities.objectsMatchingValues(ec,
							EduCourse.entityName, dict), "courses");
					dict.takeValueForKey(new NSDictionary(gr.grade(),"grade"),"cycle");
				}
				EOEnterpriseObject setting = SettingsBase.settingForCourse(
						EduPeriod.ENTITY_NAME, dict, ec);
				String listName = (setting == null)?null:
					(String)setting.valueForKey(SettingsBase.TEXT_VALUE_KEY);
				Period period = EduPeriod.getCurrentPeriod(date, listName, ec);;
				params.takeValueForKey(period, "period");
			}
			NSArray mails = EOUtilities.objectsWithQualifierFormat(ec, Contact.ENTITY_NAME,
					"persID = %d AND ( contact caseInsensitiveLike '*<" + mail +
					">' OR contact caseInsensitiveLike '" + mail + "')",
					new NSArray(Contact.idForPerson(student.person())));
			if(mails == null || mails.count() == 0)
				return error("illegalMail");
			NSMutableSet set = new NSMutableSet();
			for (int i = 0; i < mails.count(); i++) {
				Contact cnt = (Contact)mails.objectAtIndex(i);
				set.addObject(ec.globalIDForObject(cnt));
			}
			params.takeValueForKey(set, "adrSet");
			params.takeValueForKey(student,"logParam");
			EMailBroadcast.logger.log(WOLogLevel.INFO, "Requested mail queued for: " + mail,
					new Object[] {MyUtility.clientIdentity(context().request()),student});
		} catch (EOObjectNotAvailableException e) {
			return error("wrongCode");
		} catch (Exception e) {
			EMailBroadcast.logger.log(WOLogLevel.WARNING,"Error preparing requested mail " +
					studentID,new Object[] {e});
			return error("preparationError");
		} finally {
			ec.unlock();
		}
//		params.takeValueForKey("Requested mail message text", "messageText");
		params.takeValueForKey("Done requested mailing for student","logMessage");
		EMailBroadcast.broadcastMarks(params);
		sent.addObject(id);
		WOComponent result = pageWithName("MessagePage");
		result.takeValueForKey(WOApplication.application().valueForKeyPath(
				"strings.RujelContacts_Contacts.MailRequest.initiated"), "message");
		return result;
	}
	
	protected WOActionResults error(String amessage) {
		String message = "strings.RujelContacts_Contacts.MailRequest." + amessage;
		message = (String)WOApplication.application().valueForKeyPath(message);
		if(message == null) message = amessage;
		NSMutableDictionary dict = MyUtility.clientIdentity(context().request());
		dict.takeValueForKey(request().requestHandlerPath(), "requested");
		dict.addEntriesFromDictionary(request().formValues());
		EMailBroadcast.logger.log(WOLogLevel.INFO,"Failed to send requested mail: " + message,dict);
		WOComponent result = pageWithName("MessagePage");
		result.takeValueForKey(message, "message");
		return result;
	}
}
