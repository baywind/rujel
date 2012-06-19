package net.rujel.dnevnik;

import java.net.URL;

import ru.mos.dnevnik.*;

import net.rujel.base.SettingsBase;
import net.rujel.eduplan.EduPeriod;
import net.rujel.io.ExtSystem;
import net.rujel.io.SyncEvent;
import net.rujel.io.SyncIndex;
import net.rujel.reusables.SettingsReader;

import com.webobjects.appserver.WOActionResults;
import com.webobjects.appserver.WOContext;
import com.webobjects.appserver.WOComponent;
import com.webobjects.eocontrol.EOEditingContext;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSForwardException;
import com.webobjects.foundation.NSMutableDictionary;

public class ServiceFrontend extends WOComponent {
	
	
	public EOEditingContext ec;
	public ExtSystem sync;
	public ImportServiceSoap soap;
	public String schoolGuid;
	public Integer year;

	public NSArray perGroups;
	public NSArray periods;
	public ReportingPeriodGroup pgr;
	public Object item;
	
	public NSArray events;
	
    public ServiceFrontend(WOContext context) {
        super(context);
        ec = new net.rujel.reusables.SessionedEditingContext(context.session());
        sync = (ExtSystem)ExtSystem.extSystemNamed("oejd.moscow", ec, false);
        schoolGuid = sync.extDataForKey("schoolGUID", null);
        year = (Integer)context.session().valueForKey("eduYear");
        events = SyncEvent.eventsForSystem(sync, null, 10);
        try {
        	String tmp = SettingsReader.stringForKeyPath("dnevnik.serviceURL", null);
        	URL serviceURL = new URL(tmp);
        	ImportServiceLocator locator = new  ImportServiceLocator();
        	soap = locator.getImportServiceSoap12(serviceURL);
        	perGroups = new NSArray(
        			soap.getReportingPeriodGroupCollection(schoolGuid, year.intValue()));
        } catch (Exception e) {
			throw new NSForwardException(e);
		}
    }
    
    public void setPgr(ReportingPeriodGroup set) {
    	pgr = set;
    	if(set != null)
    		periods = new NSArray(set.getReportingPeriods());
    }
    
    @SuppressWarnings("unused")
	public WOActionResults syncPeriods() {
    	SettingsBase pb =  SettingsBase.baseForKey(EduPeriod.ENTITY_NAME, ec, false);
    	if(pb == null)
    		return null;
    	NSArray regimes = pb.availableValues(year, SettingsBase.TEXT_VALUE_KEY);
        try {
        	ReportingPeriodGroup[] rpgs = soap.getReportingPeriodGroupCollection(
        			schoolGuid, year.intValue());
        	if(rpgs == null)
        		return null;
        	SyncIndex index = sync.getIndexNamed("periods", null, true);
        	NSMutableDictionary dict = index.getDict();
        	for (int i = 0; i < rpgs.length; i++) {
        		NSArray local = dict.allKeysForObject(rpgs[i].getName());
				ReportingPeriod[] pers = rpgs[i].getReportingPeriods();
				
			}
        } catch (Exception e) {
			throw new NSForwardException(e);
		}
        return null;
    }
    
}