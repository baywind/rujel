package net.rujel.vselists;

import java.util.Enumeration;
import java.util.logging.Logger;

import net.rujel.reusables.NamedFlags;
import net.rujel.reusables.PlistReader;
import net.rujel.reusables.SessionedEditingContext;
import net.rujel.reusables.WOLogLevel;

import com.webobjects.appserver.*;
import com.webobjects.eoaccess.EOUtilities;
import com.webobjects.eocontrol.EOEditingContext;
import com.webobjects.eocontrol.EOEnterpriseObject;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSDictionary;
import com.webobjects.foundation.NSKeyValueCodingAdditions;
import com.webobjects.foundation.NSMutableArray;
import com.webobjects.foundation.NSTimestamp;

// Generated by the WOLips Templateengine Plug-in at Aug 27, 2009 11:13:10 PM
public class ListsEditor extends com.webobjects.appserver.WOComponent {
	public static Logger logger = Logger.getLogger("rujel.vselists");
    
	public static Object init(Object obj, WOContext ctx) {
		if(obj == null || obj.equals("init")) {
			//
		} else if(obj.equals("regimes")) {
			if(ctx != null && ctx.hasSession())
				return ctx.session().valueForKeyPath(
					"strings.RujelVseLists_VseStrings.listRegime");
			return WOApplication.application().valueForKeyPath(
					"strings.RujelVseLists_VseStrings.listRegime");
		} else if(obj.equals("personInspector")) {
			return personInspector(ctx);
		}
		return null;
	}
	
	public EOEditingContext ec;
    public VseEduGroup group;
    public boolean showAll = true;
    public NSKeyValueCodingAdditions item;
    public NamedFlags access;
    
    public ListsEditor(WOContext context) {
        super(context);
    	ec = new SessionedEditingContext(context.session());
    	access = (NamedFlags)context.session().valueForKeyPath(
				"readAccess.FLAGS.VseList");
    }

    public NSArray list() {
    	if(group == null)
    		return null;
    	if(showAll)
    		return group.lists();
    	else
    		return group.vseList();
    }
    
    public WOActionResults createGroup() {
    	WOComponent result = pageWithName("VseGroupInspector");
    	result.takeValueForKey(this, "returnPage");
    	result.takeValueForKey(ec, "ec");
    	return result;
    }
    
    public WOActionResults editGroup() {
    	WOComponent result = pageWithName("VseGroupInspector");
    	result.takeValueForKey(this, "returnPage");
    	result.takeValueForKey(group, "currGroup");
    	return result;
    }
    
	public String rowClass() {
		if (showAll) {
			NSTimestamp enter = (NSTimestamp) item.valueForKey("enter");
			NSTimestamp leave = (NSTimestamp) item.valueForKey("leave");
			if (enter != null || leave != null) {
				NSTimestamp today = (NSTimestamp) session()
						.valueForKey("today");
				if (leave != null && leave.compare(today) < 0)
					return "grey";
				if (enter != null && enter.compare(today) > 0)
					return "grey";
			}
		}
		Boolean sex = (Boolean)item.valueForKeyPath("student.person.sex");
		if(sex == null) return null;
		return (sex.booleanValue())?"male":"female";
	}
	
	public WOActionResults editPerson() {
		WOComponent popup = pageWithName("PersonInspector");
		popup.takeValueForKey(this, "returnPage");
		popup.takeValueForKey(item.valueForKey("student"), "personLink");
		return popup;
	}
	
	public WOActionResults addPerson() {
		WOComponent popup = pageWithName("SelectorPopup");
		popup.takeValueForKey(this, "returnPage");
		popup.takeValueForKey(session().valueForKeyPath(
				"strings.RujelVseLists_VseStrings.selectPerson"), "dict");
		popup.takeValueForKey("newPerson", "resultPath");
		return popup;
	}
	
	public void setNewPerson(VsePerson person) {
		if(group == null)
			return;
		if(person == null) {
			ec.revert();
			return;
		}
		person = (VsePerson)EOUtilities.localInstanceOfObject(ec, person);
		Enumeration enu = group.vseList().objectEnumerator();
		while (enu.hasMoreElements()) {
			EOEnterpriseObject vl = (EOEnterpriseObject) enu.nextElement();
			if(vl.valueForKeyPath("student.person") == person) {
				session().takeValueForKey(session().valueForKeyPath(
					"strings.RujelVseLists_VseStrings.duplicateEntry"), "message");
				return;
			}
		}
		ec.lock();
		try {
			NSTimestamp date = (NSTimestamp)session().valueForKey("today");
			VseStudent student = VseStudent.studentForPerson(person, date, true);
			student.setAbsGrade(group.absStart());
			EOEnterpriseObject newEntry = EOUtilities.createAndInsertInstance(ec, "VseList");
			newEntry.addObjectToBothSidesOfRelationshipWithKey(group, "eduGroup");
			newEntry.addObjectToBothSidesOfRelationshipWithKey(student, "student");
			newEntry.takeValueForKey(date, "enter");
			ec.saveChanges();
			logger.log(WOLogLevel.UNOWNED_EDITING, "Added person to group",
					new Object[] {session(),person,group});
		} catch (RuntimeException e) {
			logger.log(WOLogLevel.WARNING, "Error adding person to group",
					new Object[] {session(),person,group,e});
			session().takeValueForKey(e.getMessage(), "message");
			ec.revert();
		} finally {
			ec.unlock();
		}
		
	}
	
	public void save() {
		ec.lock();
		try {
			ec.saveChanges();
			logger.log(WOLogLevel.UNOWNED_EDITING, "Changed dates in group list",
					new Object[] {session(),group});
		} catch (Exception e) {
			logger.log(WOLogLevel.WARNING, "Error saving changes in group list",
					new Object[] {session(),group,e});
			session().takeValueForKey(e.getMessage(), "message");
			ec.revert();
		} finally {
			ec.unlock();
		}
	}
	
	public Boolean hideAddButton() {
		if(group == null)
			return Boolean.TRUE;
		return (Boolean)session().valueForKeyPath("readAccess._create.VseList");
	}
	
	public static Object personInspector(WOContext ctx) {
		WOSession ses = ctx.session();
		NSMutableArray result = new NSMutableArray();
		NamedFlags access = (NamedFlags)ses.valueForKeyPath(
				"readAccess.FLAGS.VsePerson");
		if(access.getFlag(0)) {
			NSDictionary dict = (NSDictionary)ses.valueForKeyPath(
					"strings.RujelVseLists_VseStrings.inspectors.Person");
			dict = PlistReader.cloneDictionary(dict, true);
			dict.takeValueForKey(access, "access");
			result.addObject(dict);
		}
		VsePerson person = (VsePerson)ses.objectForKey("PersonInspector");
		access = (NamedFlags)ses.valueForKeyPath(
			"readAccess.FLAGS.VseStudent");
		if(access.getFlag(0) 
				&& VseStudent.studentForPerson(person, null) != null) {
			NSDictionary dict = (NSDictionary)ses.valueForKeyPath(
					"strings.RujelVseLists_VseStrings.inspectors.Student");
			dict = PlistReader.cloneDictionary(dict, true);
			dict.takeValueForKey(access, "access");
			result.addObject(dict);
		}
		if(result.count() == 0)
			return null;
		return result;
	}
}