package net.rujel.eduplan;

import java.util.Enumeration;

import net.rujel.base.IndexRow;
import net.rujel.base.Indexer;
import net.rujel.base.MyUtility;
import net.rujel.base.SchoolSection;
import net.rujel.reusables.ModulesInitialiser;
import net.rujel.reusables.NamedFlags;
import net.rujel.reusables.SettingsReader;
import net.rujel.reusables.Various;
import net.rujel.reusables.WOLogLevel;

import com.webobjects.appserver.WOActionResults;
import com.webobjects.appserver.WOContext;
import com.webobjects.appserver.WOComponent;
import com.webobjects.appserver.WOSession;
import com.webobjects.eoaccess.EOUtilities;
import com.webobjects.eocontrol.EOEditingContext;
import com.webobjects.eocontrol.EOSortOrdering;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSDictionary;
import com.webobjects.foundation.NSKeyValueCoding;
import com.webobjects.foundation.NSMutableArray;
import com.webobjects.foundation.NSMutableDictionary;
import com.webobjects.foundation.NSNotification;
import com.webobjects.foundation.NSNotificationCenter;
import com.webobjects.foundation.NSPropertyListSerialization;

public class SectionsSetup extends WOComponent {
//    public Indexer sIndex;
    public Object item;
    public WOComponent returnPage;
	public NSArray sections;
	public Object currSection;
	public NSMutableDictionary newDict = new NSMutableDictionary(
			new NamedFlags(SchoolSection.flagNames),"namedFlags");
	public NSArray grades;
    
    public SectionsSetup(WOContext context) {
        super(context);
    	EOEditingContext ec = context.session().defaultEditingContext();
    	sections = SchoolSection.listSections(ec, true);
		int minGrade = SettingsReader.intForKeyPath("edu.minGrade", 1);
		int maxGrade = SettingsReader.intForKeyPath("edu.maxGrade", 11);
		Integer[] grds = new Integer[maxGrade - minGrade + 1];
		for (int i = 0; i < grds.length; i++) {
			grds[i] = new Integer(minGrade + i);
		}
		grades = new NSArray(grds);
    	newDict.takeValueForKey(minGrade, "minGrade");
    	newDict.takeValueForKey(maxGrade, "maxGrade");
    }
    
/*
    public void setEc(EOEditingContext ec) {
    	sIndex = Indexer.getIndexer(ec, "eduSections",(String)null, true);
		if(ec.globalIDForObject(sIndex).isTemporary()) {
			Logger logger = Logger.getLogger("rujel.eduplan");
			try {
				ec.saveChanges();
				logger.log(WOLogLevel.COREDATA_EDITING,"autocreating eduSections indexer",sIndex);
			} catch (Exception e) {
				logger.log(WOLogLevel.WARNING,"Error autocreating eduSections indexer",
						new Object[] {session(),e});
				ec.revert();
			}
		}
    } */
    
    public WOActionResults close() {
    	returnPage.ensureAwakeInContext(context());
    	try {
			returnPage.takeValueForKey(Boolean.TRUE, "shouldReset");
		} catch (Exception e) {
		}
    	return returnPage;
    }
    
    public static NSArray updateSession(WOSession ses) {
    	Indexer sIndex = Indexer.getIndexer(ses.defaultEditingContext(),
    			"eduSections",(String)null, false);
		NSKeyValueCoding strings = (NSKeyValueCoding)ses.valueForKey("strings");
    	if(sIndex == null) {
			strings.takeValueForKey(NSDictionary.EmptyDictionary, "sections");
			return null;
    	}
    	NSArray sections = sIndex.sortedIndex();
		if(sections == null || sections.count() == 0) {
			strings.takeValueForKey(NSDictionary.EmptyDictionary, "sections");
			return null;
		}
		NSMutableArray list = new NSMutableArray(sections.count());
		Enumeration enu = sections.objectEnumerator();
		while (enu.hasMoreElements()) {
			IndexRow section = (IndexRow) enu.nextElement();
			NSMutableDictionary dict = new NSMutableDictionary(3);
			dict.takeValueForKey(section.idx(), IndexRow.IDX_KEY);
			dict.takeValueForKey(section.value(), IndexRow.VALUE_KEY);
			String comment = section.comment();
			if(comment != null) {
				try {
					Object data = NSPropertyListSerialization.propertyListFromString(comment);
					dict.takeValueForKey(data, "dict");
				} catch (Exception e) {
					EduPlan.logger.log(WOLogLevel.INFO,"Error parsing section dict: "
							+ section.value(), e);
				}
			}
			list.addObject(dict);
		}
		NSDictionary dict = (NSDictionary)strings.valueForKey("sections");
		SesNotifier observer = (dict == null)?null:(SesNotifier)dict.valueForKey("observer");
		if(observer == null)
			observer = new SesNotifier(ses);
		else
			NSNotificationCenter.defaultCenter().removeObserver(observer);
		dict = new NSMutableDictionary(3);
		dict.takeValueForKey(list, "list");
		dict.takeValueForKey(Boolean.valueOf(list.count() > 1), "hasSections");
		strings.takeValueForKey(dict, "sections");
		Integer year = (Integer)ses.valueForKey("eduYear");
		dict.takeValueForKey(observer, "observer");
		NSNotificationCenter.defaultCenter().addObserver(observer,
				MyUtility.notify,"sectionsChanged", year);
		return list;
    }
    
    public WOActionResults update() {
    	NSArray list = updateSession(session());
    	NSNotificationCenter.defaultCenter().postNotification("sectionsChanged",
    			session().valueForKey("eduYear"),new NSDictionary(list,"list"));
    	return null;
    }
    
    public static class SesNotifier {
    	private WOSession ses;
    	public SesNotifier(WOSession session) {
    		ses = session;
    	}

    	public void notify(NSNotification ntf) {
    		NSArray list = (NSArray)ntf.userInfo().valueForKey("list");
    		NSKeyValueCoding strings = (NSKeyValueCoding)ses.valueForKey("strings");
    		if(strings == null)
    			return;
    		if(list == null || list.count() == 0) {
    			strings.takeValueForKey(new NSDictionary(this, "observer"), "sections");
    			return;
    		}
    		NSMutableDictionary dict = new NSMutableDictionary(3);
    		dict.takeValueForKey(list, "list");
    		dict.takeValueForKey(Boolean.valueOf(list.count() > 1), "hasSections");
    		dict.takeValueForKey(this, "observer");
    		strings.takeValueForKey(dict, "sections");

    		Integer state =  (Integer)ses.valueForKeyPath("state.section.idx");
    		if(state != null) {
    			Enumeration enu = list.objectEnumerator();
    			while (enu.hasMoreElements()) {
    				NSDictionary sect = (NSDictionary) enu.nextElement();
    				if(state.equals(sect.valueForKey(IndexRow.IDX_KEY)))
    					return;
    			}
    			NSDictionary sect = (NSDictionary)list.objectAtIndex(0);
    			ses.takeValueForKeyPath(sect, "state.section");
    		}
    	}
    }

	public WOActionResults save() {
		EOEditingContext ec = session().defaultEditingContext();
		SchoolSection created = null;
		if(currSection == null && newDict.containsKey("name")) {
			created = (SchoolSection)EOUtilities.createAndInsertInstance(
					ec, SchoolSection.ENTITY_NAME);
			created.takeValuesFromDictionary(newDict);
			if(sections.count() < 2) {
				SchoolSection first = (SchoolSection)sections.lastObject();
				if(first == null || first.sectionID() <= 0)
					created.takeStoredValueForKey(Integer.valueOf(1), "sID");				
			}
			Integer sort = (Integer)sections.valueForKeyPath("@max.sort");
			if(sort == null)
				sort = Integer.valueOf(sections.count());
			else
				sort = new Integer(sort.intValue() +1);
			created.setSort(sort);
		}
		try {
			ec.saveChanges();
			if(created != null) {
				EduPlan.logger.log(WOLogLevel.COREDATA_EDITING,"Created section",
						new Object[] {created});
				session().takeValueForKey(SchoolSection.forSession(session()), "sections");
				sections = sections.arrayByAddingObject(created);
			} else {
				EduPlan.logger.log(WOLogLevel.COREDATA_EDITING,"Modified section",
						new Object[] {currSection});
				sections = EOSortOrdering.sortedArrayUsingKeyOrderArray(
						sections, ModulesInitialiser.sorter);
			}
			if(newDict.containsKey("name"))
				newDict = new NSMutableDictionary(
						new NamedFlags(SchoolSection.flagNames),"namedFlags");
		} catch (Exception e) {
			ec.revert();
			EduPlan.logger.log(WOLogLevel.WARNING,"Error saving changes to section",
					new Object[] {currSection,e});
			session().takeValueForKey(e.toString(), "message");
		}
		currSection = null;
		return null;
	}
	
	public String sectionClass() {
		if(Various.boolForObject(valueForKeyPath("item.namedFlags.disabled")))
			return "grey";
		else
			return "ungerade";
	}

	public WOActionResults selectSection() {
		currSection = item;
		return null;
	}

	public boolean isSelected() {
		return item == currSection;
	}
}