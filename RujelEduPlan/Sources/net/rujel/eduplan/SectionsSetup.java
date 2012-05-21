package net.rujel.eduplan;

import java.util.Enumeration;
import java.util.logging.Logger;

import net.rujel.base.IndexRow;
import net.rujel.base.Indexer;
import net.rujel.base.MyUtility;
import net.rujel.reusables.WOLogLevel;

import com.webobjects.appserver.WOActionResults;
import com.webobjects.appserver.WOContext;
import com.webobjects.appserver.WOComponent;
import com.webobjects.appserver.WOSession;
import com.webobjects.eocontrol.EOEditingContext;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSDictionary;
import com.webobjects.foundation.NSKeyValueCoding;
import com.webobjects.foundation.NSMutableArray;
import com.webobjects.foundation.NSMutableDictionary;
import com.webobjects.foundation.NSNotification;
import com.webobjects.foundation.NSNotificationCenter;
import com.webobjects.foundation.NSPropertyListSerialization;

public class SectionsSetup extends WOComponent {
    public Indexer sIndex;
    public IndexRow item;
    public WOComponent returnPage;
    
    public SectionsSetup(WOContext context) {
        super(context);
    }

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
    }
    
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
			strings.takeValueForKey(null, "sections");
			return null;
    	}
    	NSArray sections = sIndex.sortedIndex();
		if(sections == null || sections.count() == 0) {
			strings.takeValueForKey(null, "sections");
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
		SesNotifier observer = (SesNotifier)ses.valueForKeyPath("strings.sections.observer");
		if(observer == null)
			observer = new SesNotifier(ses);
		else
			NSNotificationCenter.defaultCenter().removeObserver(observer);
		NSMutableDictionary dict = new NSMutableDictionary(3);
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
    	NSArray sections = updateSession(session());
    	NSNotificationCenter.defaultCenter().postNotification("sectionsChanged",
    			session().valueForKey("eduYear"),new NSDictionary(sections,"list"));
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
}