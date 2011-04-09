package net.rujel.eduplan;

import java.util.Enumeration;
import java.util.logging.Logger;

import net.rujel.base.IndexRow;
import net.rujel.base.Indexer;
import net.rujel.reusables.WOLogLevel;

import com.webobjects.appserver.WOActionResults;
import com.webobjects.appserver.WOApplication;
import com.webobjects.appserver.WOContext;
import com.webobjects.appserver.WOComponent;
import com.webobjects.eocontrol.EOEditingContext;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSDictionary;
import com.webobjects.foundation.NSKeyValueCoding;
import com.webobjects.foundation.NSMutableArray;
import com.webobjects.foundation.NSMutableDictionary;
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
    
    public static NSArray updateApplication(Indexer sIndex) {
    	NSArray sections = sIndex.sortedIndex();
		if(sections == null || sections.count() == 0)
			return null;
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
		NSMutableDictionary dict = new NSMutableDictionary(2);
		dict.takeValueForKey(list, "list");
		dict.takeValueForKey(Boolean.valueOf(list.count() > 1), "hasSections");
		NSKeyValueCoding strings = (NSKeyValueCoding)WOApplication.
							application().valueForKey("strings");
		strings.takeValueForKey(dict, "sections");
		return list;
    }
    
    public WOActionResults update() {
    	NSArray sections = updateApplication(sIndex);
    	if(sections == null || sections.count() == 0)
    		return null;
		Integer state =  (Integer)session().valueForKeyPath("state.section.idx");
		if(state != null) {
			Enumeration enu = sections.objectEnumerator();
			while (enu.hasMoreElements()) {
				NSDictionary sect = (NSDictionary) enu.nextElement();
				if(state.equals(sect.valueForKey(IndexRow.IDX_KEY)))
					return null;
			}
		}
		NSDictionary sect = (NSDictionary)sections.objectAtIndex(0);
		session().takeValueForKeyPath(sect, "state.section");
    	return null;
    }
}