package net.rujel.dnevnik;

import net.rujel.io.ExtSystem;
import net.rujel.io.SyncEvent;
import net.rujel.reusables.MultiECLockManager;

import com.webobjects.appserver.WOComponent;
import com.webobjects.appserver.WOContext;
import com.webobjects.eocontrol.EOEditingContext;
import com.webobjects.foundation.NSTimestamp;
import com.webobjects.woextensions.WOLongResponsePage;

public class SyncWaiter extends WOLongResponsePage {
    public SyncWaiter(WOContext context) {
        super(context);
        setRefreshInterval(2.0);
    }
    
	public NSTimestamp since;
	public NSTimestamp to;
	public Integer limit;
    public Sychroniser sychroniser;
    public WOComponent returnPage;

	@Override
	public Object performAction() {
		sychroniser.waiter = this;
		Object errors =  sychroniser.syncChanges(since, to, limit);
		return errors;
	}
	
	public WOComponent pageForResult(Object aResult) {
		MultiECLockManager lm = (MultiECLockManager)session().valueForKey("ecLockManager");
		if(lm != null)
			lm.registerEditingContext((EOEditingContext)returnPage.valueForKey("ec"));

		returnPage.ensureAwakeInContext(context());
		ExtSystem sync = (ExtSystem)returnPage.valueForKey("sync");
		returnPage.takeValueForKey(SyncEvent.eventsForSystem(sync, null, 20,"marks"), "events");
		returnPage.takeValueForKey(aResult,"errors");
		return returnPage;
	}
	
}