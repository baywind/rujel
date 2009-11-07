// SubgroupEditor.java: Class file for WO Component 'SubgroupEditor'

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

package net.rujel.ui;

import net.rujel.interfaces.*;
import net.rujel.base.*;

import com.webobjects.foundation.*;
import com.webobjects.appserver.*;
import com.webobjects.eocontrol.*;
import java.util.logging.Logger;
import net.rujel.reusables.WOLogLevel;

public class SubgroupEditor extends WOComponent {
	protected static Logger logger = Logger.getLogger("rujel.journal");
    public BaseCourse course;
    public Student studentItem;
	private NSMutableSet subgroup;
	public NSArray studentsList;
	
    public SubgroupEditor(WOContext context) {
        super(context);
    }
	
	public void setCourse(BaseCourse aCourse) {
		if (aCourse == null) {
			course = null;
			return;
		}
		course = aCourse;
		studentsList = course.eduGroup().list();
		subgroup = new NSMutableSet(course.groupList());
	}
	
    public boolean isInSubgroup() {
		return subgroup.containsObject(studentItem);
    }
	
    public void setIsInSubgroup(boolean newIsInSubgroup) {
        if(newIsInSubgroup)
			subgroup.addObject(studentItem);
		else
			subgroup.removeObject(studentItem);
    }
	
	public WOActionResults save() {
		NSArray ls = course.groupList();
		if(ls != null && subgroup.equals(new NSMutableSet(ls))) 
			return (WOComponent)session().valueForKey("pullComponent");
		
		WOActionResults nextPage = null;
		EOEditingContext ec = course.editingContext();
//		if(ec.hasChanges()) {
			ec.lock();
			course.setSubgroup(subgroup.allObjects());
			WOLogLevel level = WOLogLevel.UNOWNED_EDITING;
			try {
				ec.saveChanges();
//				if(course instanceof UseAccess && ((UseAccess)course).isOwned())
//					level = WOLogLevel.OWNED_EDITING;
				logger.logp(level,"SubgroupEditor","save","Subgroup changes saved",new Object[] {session(),course});
				session().takeValueForKey(Boolean.TRUE,"prolong");
				nextPage = (WOComponent)session().valueForKey("pullComponent");
			} catch (Exception ex) {
				logger.logp(level,"SubgroupEditor","save","Failed to save changes in subgroup",new Object[] {session(),course,ex});
				session().takeValueForKey(ex.toString(),"message");
			}
			ec.unlock();
			
//		}
		return nextPage;
	}
	
	public String studentStyle() {
		Boolean sex = (Boolean)valueForKeyPath("studentItem.person.sex");
		if(!isInSubgroup()) return "grey";
		if(sex == null) return "gerade"; 
		if (sex.booleanValue())
			return "male";
		else
			return "female";
	}
	
	
    public String onClick() {
		StringBuilder sb = new StringBuilder(50).append("modifyRowClass(this,'");
		Boolean sex = (Boolean)valueForKeyPath("studentItem.person.sex");
		if(sex != null) {
			if(isInSubgroup()) {
				if (sex.booleanValue())
					sb.append("male");
				else
					sb.append("female");
			} else {
				sb.append("found");
				if (sex.booleanValue())
					sb.append("Male");
				else
					sb.append("Female");
			}
		} else {
			if(!isInSubgroup())
				sb.append("un");
			sb.append("gerade");
		}
		sb.append("','grey");
		sb.append("');");
		return sb.toString();
    }
	
    public String title() {
        return (String)valueForKeyPath("application.strings.RujelInterfaces_Names.EduCourse.subgroup");
    }
    public Number total() {
        return subgroup.count();
    }
	
    public void setTotal(Number newTotal) {
        if(newTotal.intValue() != subgroup.count())
			logger.logp(WOLogLevel.INFO,"SubgroupEditor","setTotal","Incorrect subgroup.count calculation",new Object[] {session(),course});
    }

	private int idx = 0;
	public int idx() {
		return idx + 1;
	}
	public void setIdx(Number nextIdx) {
		idx = (nextIdx == null)?0:nextIdx.intValue();
	}
}
