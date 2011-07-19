// SchedulePopup.java: Class file for WO Component 'SchedulePopup'

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

import java.util.logging.Logger;

import net.rujel.interfaces.EduCourse;
import net.rujel.interfaces.EduGroup;
import net.rujel.interfaces.Student;
import net.rujel.interfaces.Teacher;
import net.rujel.ui.RedirectPopup;

import com.webobjects.appserver.WOActionResults;
import com.webobjects.appserver.WOComponent;
import com.webobjects.appserver.WOContext;
import com.webobjects.appserver.WOResponse;
import com.webobjects.eocontrol.EOAndQualifier;
import com.webobjects.eocontrol.EOFetchSpecification;
import com.webobjects.eocontrol.EOKeyValueQualifier;
import com.webobjects.eocontrol.EOQualifier;
import com.webobjects.foundation.NSArray;

public class SchedulePopup extends WOComponent {
	public static final Logger logger = Logger.getLogger("rujel.schedule");
	public WOComponent returnPage;
    public EduCourse course;
    public Teacher forTeacher;
    public EduGroup forClass;
    public Student forStudent;
    public NSArray courses;
    
    public SchedulePopup(WOContext context) {
        super(context);
    }
    
    public void appendToResponse(WOResponse aResponse,WOContext aContext) {
    	if(courses == null && (forClass != null || forTeacher != null)) {
    		EOQualifier[] quals = new EOQualifier[2];
    		quals[0] = new EOKeyValueQualifier("eduYear", EOQualifier.QualifierOperatorEqual,
    				session().valueForKey("eduYear"));
    		if(forTeacher != null)
    			quals[1] = new EOKeyValueQualifier("teacher", 
    					EOQualifier.QualifierOperatorEqual, forTeacher);
    		else
    			quals[1] = new EOKeyValueQualifier("eduGroup", 
    					EOQualifier.QualifierOperatorEqual, forClass);
    		quals[0] = new EOAndQualifier(new NSArray(quals));
    		EOFetchSpecification fs = new EOFetchSpecification(EduCourse.entityName,quals[0],null);
    		courses = course.editingContext().objectsWithFetchSpecification(fs);
    	}
    	super.appendToResponse(aResponse, aContext);
    }
    

    public WOActionResults noAction() {
		return RedirectPopup.getRedirect(context(), returnPage);
    }
    
	public String groupClass() {
		if(forClass != null)
			return "selection";
		return "grey";
	}
    
	public String subjectClass() {
		if(forClass == null && forTeacher == null)
			return "selection";
		return "grey";
	}
	
	public String teacherClass() {
		if(forTeacher != null)
			return "selection";
		return "grey";
	}

	public WOActionResults useCourse() {
		forClass = null;
		forTeacher = null;
		courses = null;
		return null;
	}

	public WOActionResults useGroup() {
		forClass = course.eduGroup();
		forTeacher = null;
		courses = null;
		return null;
	}

	public WOActionResults useTeacher() {
		forClass = null;
		forTeacher = course.teacher();
		courses = null;
		return null;
	}
}