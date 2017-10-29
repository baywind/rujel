package net.rujel.contacts;

import net.rujel.base.MyUtility;
import net.rujel.base.Setting;
import net.rujel.base.SettingsBase;
import net.rujel.eduplan.EduPeriod;
import net.rujel.interfaces.EduGroup;
import net.rujel.interfaces.Student;
import net.rujel.reusables.SettingsReader;

import com.webobjects.appserver.WOContext;
import com.webobjects.appserver.WOComponent;
import com.webobjects.eoaccess.EOUtilities;
import com.webobjects.eocontrol.EOEditingContext;
import com.webobjects.eocontrol.EOKeyGlobalID;
import com.webobjects.foundation.*;

public class MailRequest extends WOComponent {
    public MailRequest(WOContext context) {
        super(context);
    }
    
    public String code;
    public NSArray periods;
    public Object current;
    public Object selected;
    public Object item;
    public NSTimestamp since;
    public NSTimestamp to;
    
    public void setStudentID(Integer studentID) {
		{
			String defaultDate = SettingsReader.stringForKeyPath("ui.defaultDate", null);
			if(defaultDate != null)
				to = MyUtility.parseDate(defaultDate);
		}
		if(to == null)
			to = new NSTimestamp();
		EOEditingContext ec = new EOEditingContext();
		ec.lock();
		try {
			Student student = (Student)EOUtilities.objectWithPrimaryKeyValue(ec, 
					Student.entityName, studentID);
			EduGroup gr = student.recentMainEduGroup();
			NSDictionary cdict = SettingsBase.courseDict(gr, MyUtility.eduYearForDate(to));
			Setting setting = SettingsBase.settingForCourse(EduPeriod.ENTITY_NAME, cdict, ec);
			String listName = (setting == null)?null:setting.textValue();
			periods = EduPeriod.periodsInList(listName, ec);
			NSMutableArray dicts = new NSMutableArray();
			for (int i = 0; i < periods.count(); i++) {
				EduPeriod per = (EduPeriod) periods.objectAtIndex(i);
				NSMutableDictionary dict = new NSMutableDictionary();
				dict.takeValueForKey(per.name(), "name");
				EOKeyGlobalID gid = (EOKeyGlobalID)ec.globalIDForObject(per);
				dict.takeValueForKey(gid.keyValues()[0], "id");
				dicts.addObject(dict);
				if(current == null && per.contains(to)) {
					since = per.begin();
					to = per.end();
					current = dict;
					selected = gid.keyValues()[0];
				}
			}
			periods = dicts;
//		} catch (Exception e) {
//			EMailBroadcast.logger.log(WOLogLevel.WARNING,
//					"Error preparing periods for MailRequest",e);
		} finally {
			ec.unlock();
		}
		if(since == null) {
			since = to.timestampByAddingGregorianUnits(0, 0, -7, 0, 0, 0);
		}
    }
}