package net.rujel.reports;

import com.webobjects.appserver.WOActionResults;
import com.webobjects.appserver.WOContext;
import com.webobjects.appserver.WOComponent;
import com.webobjects.appserver.WOResponse;
import com.webobjects.eocontrol.EOEditingContext;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSKeyValueCoding;
import com.webobjects.foundation.NSMutableArray;

import net.rujel.interfaces.EduGroup;
import net.rujel.reusables.PlistReader;
import net.rujel.reusables.SessionedEditingContext;

public class GroupReport extends WOComponent {
	public EOEditingContext ec;
	public NSMutableArray reports;
	public NSMutableArray display;
    public EduGroup currClass;
    public NSArray students;
	public NSKeyValueCoding item;
	public NSKeyValueCoding subItem;

	public GroupReport(WOContext context) {
        super(context);
        ec = new SessionedEditingContext(context.session());
    }

	public void setCurrClass(EduGroup group) {
		currClass = group;
		if(currClass != null)
		students = currClass.list();
	}
	
    public NSMutableArray prepareDisplay() {
    	return new NSMutableArray(session().valueForKeyPath(
    			"strings.RujelReports_Reports.GroupReport.defaultDisplay"));
    }
	
	public void modifyList() {
		display = prepareDisplay();
		display.addObjectsFromArray(PropSelector.prepareActiveList(reports));
	}

    public String title() {
		return (String)application().valueForKeyPath(
				"strings.RujelReports_Reports.GroupReport.title");
	}
    
    public void appendToResponse(WOResponse aResponse, WOContext aContext) {
    	if(reports == null) {
    		NSArray availableReports = (NSArray)session().valueForKeyPath(
    				"modules.groupReport");
    		reports = PlistReader.cloneArray(availableReports, true);
    		NSArray dirReports = ReportsModule.reportsFromDir(
    				"GroupReport",aContext.session());
    		if(dirReports != null && dirReports.count() > 0)
    			reports.addObjectsFromArray(dirReports);
    	}
    	super.appendToResponse(aResponse, aContext);
    }
    
	public String reportStyle() {
		if(display != null && display.count() > 1)
			return "display:none;";
		return null;
	}
	
	public WOActionResults export() {
		WOComponent exportPage = pageWithName("ReportTable");
		exportPage.takeValueForKey(currClass, "item");
		exportPage.takeValueForKey(students, "list");
		exportPage.takeValueForKey(display, "properties");
 		exportPage.takeValueForKey("'GroupReport'yyMMdd", "filenameFormatter");
		return exportPage;
	}
		
	public WOActionResults clear() {
		currClass = null;
		students = null;
		return null;
	}
	public void sectionChanged() {
		reports = null;
	}
}