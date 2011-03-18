package net.rujel.eduplan;

import java.util.logging.Logger;

import net.rujel.base.IndexRow;
import net.rujel.base.Indexer;
import net.rujel.reusables.WOLogLevel;

import com.webobjects.appserver.WOActionResults;
import com.webobjects.appserver.WOContext;
import com.webobjects.appserver.WOComponent;
import com.webobjects.eocontrol.EOEditingContext;

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
}