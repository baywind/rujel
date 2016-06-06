// RingsSetup.java: Class file for WO Component 'RingsSetup'

/*
 * Copyright (c) 2008, Gennady & Michael Kushnir
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 * 
 * 	•	Redistributions of source code must retain the above copyright notice, this
 * 		list of conditions and the following disclaimer.
 * 	•	Redistributions in binary form must reproduce the above copyright notice,
 * 		this list of conditions and the following disclaimer in the documentation
 * 		and/or other materials provided with the distribution.
 * 	•	Neither the name of the RUJEL nor the names of its contributors may be used
 * 		to endorse or promote products derived from this software without specific 
 * 		prior written permission.
 * 		
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT
 * SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
 * TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
 * BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY
 * WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package net.rujel.schedule;

import java.text.SimpleDateFormat;
import java.util.Date;
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
	public Boolean useGlobal;
	public String title;
    
    public RingsSetup(WOContext context) {
        super(context);
    }
    
	public void appendToResponse(WOResponse aResponse, WOContext aContext) {
		if(ec == null || Various.boolForObject(valueForBinding("shouldReset"))) {
	        ec = (EOEditingContext)aContext.page().valueForKey("ec");
			updateSection();
			setValueForBinding(Boolean.FALSE, "shouldReset");
		}
		super.appendToResponse(aResponse, aContext);
	}
	        
	public WOActionResults toggleEdit() {
    	noEdit = (Boolean)session().valueForKeyPath("readAccess._edit.ScheduleRing");
    	if(!noEdit && useGlobal) {
    		list = NSArray.EmptyArray;
    		useGlobal = Boolean.FALSE;
    		if((Integer)session().valueForKeyPath("state.section.sID") > 0) {
    			title = (String)session().valueForKeyPath(
				"strings.RujelSchedule_Schedule.makeLocal") 
				+ " \"" + session().valueForKeyPath("state.section.name") + '"';
    		}
    	}
    	return null;
    }
    
	public void updateSection() {
		Integer currSection = (Integer)session().valueForKeyPath("state.section.sID");
    	EOQualifier qual = new EOKeyValueQualifier("timeScheme", 
    			EOQualifier.QualifierOperatorEqual, currSection);
    	EOFetchSpecification fs = new EOFetchSpecification("ScheduleRing",qual,MyUtility.numSorter);
    	list = ec.objectsWithFetchSpecification(fs);
    	if((list == null || list.count() == 0) && currSection.intValue() > 0) {
        	qual = new EOKeyValueQualifier("timeScheme", 
        			EOQualifier.QualifierOperatorEqual, new Integer(0));
        	fs.setQualifier(qual);
        	list = ec.objectsWithFetchSpecification(fs);
        	useGlobal = Boolean.valueOf(list != null && list.count() > 0);
        	noEdit = useGlobal;
        	if(useGlobal)
        		title = (String)session().valueForKeyPath(
        				"strings.RujelSchedule_Schedule.useGlobal");
        	else
        		title = null;
    	} else {
    		useGlobal = Boolean.FALSE;
        	noEdit = Boolean.TRUE;
        	if(currSection.intValue() == 0)
        		title = (String)session().valueForKeyPath(
					"strings.RujelSchedule_Schedule.isGlobal");
        	else
        		title = (String)session().valueForKeyPath(
					"strings.RujelSchedule_Schedule.isLocal") 
					+ " \"" + session().valueForKeyPath("state.section.name") + '"';
    	}
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
		SimpleDateFormat format = new SimpleDateFormat("HH:mm");
		boolean fail = false;
		NSMutableArray newList = list.mutableClone();
    	while(true) {
    		try {
    			String newVal = req.stringFormValueForKey("start" + idx);
    			if(newVal == null)
    				break;
    			Date newStart = (Date)format.parseObject(newVal);
    			newVal = req.stringFormValueForKey("end" + idx);
    			if(newVal == null)
    				break;
    			Date newEnd = (Date)format.parseObject(newVal);
    			EOEnterpriseObject ring = EOUtilities.createAndInsertInstance(ec, "ScheduleRing");
    			ring.takeValueForKey((Integer)session().valueForKeyPath("state.section.sID"),
    					"timeScheme");
    			ring.takeValueForKey(new NSTimestamp(newStart), "startTime");
    			ring.takeValueForKey(new NSTimestamp(newEnd), "endTime");
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
			if(newList.count() == 0) {
				updateSection();
			} else {
				list = newList;
				useGlobal = Boolean.FALSE;
				Integer currSection = (Integer)session().valueForKeyPath("state.section.sID");
	        	if(currSection.intValue() > 0)
	        		title = (String)session().valueForKeyPath(
						"strings.RujelSchedule_Schedule.isLocal");
			}
    	} else {
			noEdit = Boolean.TRUE;
    	}
    	return null;
    }
    
    public boolean synchronizesVariablesWithBindings() {
        return false;
	}
}