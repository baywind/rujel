package net.rujel.schedule;

import java.util.logging.Logger;

import com.webobjects.appserver.WOActionResults;
import com.webobjects.appserver.WOContext;
import com.webobjects.appserver.WORequest;
import com.webobjects.appserver.WOResponse;
import com.webobjects.eoaccess.EOUtilities;
import com.webobjects.eocontrol.*;
import com.webobjects.foundation.*;

import net.rujel.base.MyUtility;
import net.rujel.reusables.Various;
import net.rujel.reusables.WOLogLevel;
import net.rujel.ui.LessonList;

public class RingsSetup extends LessonList {
	public static final Logger logger = Logger.getLogger("rujel.schedule");
	
	public static final NSArray sorter = new NSArray(
			new EOSortOrdering("startTime",EOSortOrdering.CompareAscending));
	
    public EOEditingContext ec;
	public NSArray list;
	public Object item;
    public Boolean noEdit;
	
    public RingsSetup(WOContext context) {
        super(context);
    }
    
	public void appendToResponse(WOResponse aResponse, WOContext aContext) {
		if(ec == null || Various.boolForObject(valueForBinding("shouldReset"))) {
	        ec = (EOEditingContext)aContext.page().valueForKey("ec");
	        list = null;
	        noEdit = (Boolean)session().valueForKeyPath("readAccess._edit.ScheduleRing");
	        if(!noEdit && list().count() > 0)
	        	noEdit = Boolean.TRUE;
			setValueForBinding(Boolean.FALSE, "shouldReset");
		}
		super.appendToResponse(aResponse, aContext);
	}
	        
	public WOActionResults toggleEdit() {
    	noEdit = Boolean.valueOf(!noEdit.booleanValue());
    	return null;
    }
    
    public NSArray list() {
    	if(list != null)
    		return list;
    	EOQualifier qual = new EOKeyValueQualifier("timeScheme", 
    			EOQualifier.QualifierOperatorEqual, new Integer(0));
    	EOFetchSpecification fs = new EOFetchSpecification("ScheduleRing",qual,MyUtility.numSorter);
    	list = ec.objectsWithFetchSpecification(fs);
    	if(list == null)
    		list = NSArray.EmptyArray;
    	return list;
    }
    
    public WOActionResults save() {
    	int idx = list.count() +1;
    	WORequest req = context().request();
    	final Integer zero = new Integer(0);
		@SuppressWarnings("deprecation")
		NSTimestampFormatter format = new NSTimestampFormatter("HH:mm");
		boolean fail = false;
		NSMutableArray newList = list.mutableClone();
    	while(true) {
    		try {
    			String newVal = req.stringFormValueForKey("start" + idx);
    			if(newVal == null)
    				break;
    			NSTimestamp newStart = (NSTimestamp)format.parseObject(newVal);
    			newVal = req.stringFormValueForKey("end" + idx);
    			if(newVal == null)
    				break;
    			NSTimestamp newEnd = (NSTimestamp)format.parseObject(newVal);
    			EOEnterpriseObject ring = EOUtilities.createAndInsertInstance(ec, "ScheduleRing");
    			ring.takeValueForKey(zero, "timeScheme");
    			ring.takeValueForKey(newStart, "startTime");
    			ring.takeValueForKey(newEnd, "endTime");
    			newList.addObject(ring);
    		} catch (Exception e) {
				fail = true;
			}
    		idx++;
    	}
    	if(fail)
    		session().takeValueForKey(session().valueForKeyPath(
    				"strings.RujelSchedule_Schedule.cantParce"), "message");
    	if(ec.hasChanges()) {
    		EOSortOrdering.sortArrayUsingKeyOrderArray(newList, sorter);
    		NSTimestamp prev = null;
    		for (int i = 0; i < newList.count(); i++) {
    			EOEnterpriseObject ring = (EOEnterpriseObject)newList.objectAtIndex(i);
    			NSTimestamp start = (NSTimestamp)ring.valueForKey("startTime");
    			if(start == null) {
    				ec.deleteObject(ring);
    				newList.removeObjectAtIndex(i);
    				i--;
    				continue;
    			}
    			Integer num = (Integer)ring.valueForKey("num");
    			if(num == null || num.intValue() != i+1)
    				ring.takeValueForKey(new Integer(i+1), "num");
    			if(prev != null && prev.after(start)) {
    	    		session().takeValueForKey(session().valueForKeyPath(
    					"strings.RujelSchedule_Schedule.lessonsOverlap"), "message");
    				fail = true;
    			}
    			prev = (NSTimestamp)ring.valueForKey("endTime");
    			if(prev == null) {
    				prev = start.timestampByAddingGregorianUnits(0, 0, 0, 0, 40, 0);
    				ring.takeValueForKey(prev, "endTime");
    			} else if(!prev.after(start)) {
    	    		session().takeValueForKey(session().valueForKeyPath(
						"strings.RujelSchedule_Schedule.inversedLesson"), "message");
    				fail = true;
    			}
			}
    		if(!fail) {
    			try {
    				ec.saveChanges();
    				noEdit = Boolean.TRUE;
    				logger.log(WOLogLevel.COREDATA_EDITING,"Saved rings Schedule", session());
    			} catch (Exception e) {
    				session().takeValueForKey(e.getMessage(), "message");
    				logger.log(WOLogLevel.WARNING,"Error saving rings Schedule", 
    						new Object[] {session(),e});
    			}
    		}
			list = newList;
    	} else {
			noEdit = Boolean.TRUE;
    	}
    	return null;
    }
    
    public boolean synchronizesVariablesWithBindings() {
        return false;
	}
}