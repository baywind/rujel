package net.rujel.dnevnik;

import java.net.URL;

import ru.mos.dnevnik.*;

import net.rujel.io.ExtBase;
import net.rujel.io.ExtSystem;
import net.rujel.reusables.SettingsReader;

import com.webobjects.appserver.WOContext;
import com.webobjects.appserver.WOComponent;
import com.webobjects.eocontrol.EOEditingContext;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSDictionary;
import com.webobjects.foundation.NSForwardException;
import com.webobjects.webservices.client.WOWebServiceClient;

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
	
    public ServiceFrontend(WOContext context) {
        super(context);
        ec = new net.rujel.reusables.SessionedEditingContext(context.session());
        sync = (ExtSystem)ExtSystem.extSystemNamed("oejd.moscow", ec, false);
        schoolGuid = sync.extDataForKey("schoolGUID", null);
        year = (Integer)context.session().valueForKey("eduYear");
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
}