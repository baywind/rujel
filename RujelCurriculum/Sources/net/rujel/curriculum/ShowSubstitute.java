// ShowSubstitute.java

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

package net.rujel.curriculum;

import net.rujel.interfaces.*;
import net.rujel.reusables.Various;

import com.webobjects.appserver.*;
import com.webobjects.eocontrol.EOFetchSpecification;
import com.webobjects.eocontrol.EOQualifier;
import com.webobjects.foundation.NSArray;
import com.webobjects.appserver.WOActionResults;

// Generated by the WOLips Templateengine Plug-in at Sep 30, 2008 12:18:15 PM
public class ShowSubstitute extends com.webobjects.appserver.WOComponent {
    public ShowSubstitute(WOContext context) {
        super(context);
    }
    
//    protected NSKeyValueCoding _dict;
//    protected EduCourse _course;
    public NSArray subsList;
    public Substitute substitute;
    public Boolean cantCreate;
    public NSArray joins;
    
/*    public NSKeyValueCoding dict() {
    	if(_dict == null)
    		_dict = (NSKeyValueCoding)valueForBinding("extention");
    	return _dict;
    }*/
    
    public NSArray subsList() {
    	if(subsList == null) {
       		EduLesson lesson = (EduLesson)valueForBinding("lesson");
    		subsList = (NSArray)lesson.valueForKey("substitutes");
    		if(subsList == null)
    			subsList = NSArray.EmptyArray;
    	}
    	return subsList;
    }

    public NSArray joins() {
    	if(joins == null) {
       		EduLesson lesson = (EduLesson)valueForBinding("lesson");
//     		joins = EOUtilities.objectsMatchingKeyAndValue(lesson.editingContext(), 
//    				Substitute.ENTITY_NAME, "fromLesson",lesson);
       		joins = (NSArray)lesson.valueForKey("joins");
     		if(joins == null)
     			joins = NSArray.EmptyArray;
    	}
    	return joins;
    }
    
    public Boolean canAdd() {
    	if(cantCreate().booleanValue())
    		return Boolean.FALSE;
    	if(subsList().count() > 0 || joins().count() > 0)
    		return Boolean.TRUE;
		return Boolean.FALSE;
    }
    
    public String descJoin() {
    	EduCourse course = (EduCourse)valueForKeyPath("substitute.lesson.course");
    	if(course == null)
    		return null;
    	StringBuilder buf = new StringBuilder("<span style = \"white-space:nowrap;\">");
    	buf.append(course.eduGroup().name()).append("</span> ");
    	Teacher teacher = course.teacher(substitute.date());
    	if(teacher == null)
    		buf.append(session().valueForKeyPath("strings.RujelBase_Base.vacant"));
    	else {
    		buf.append("<span style = \"white-space:nowrap;\">");
    		buf.append(Person.Utility.fullName(teacher, true, 2, 1, 1));
    		buf.append("</span>");
    	}
    	return buf.toString();
    }
    
    public WOActionResults openJointLesson() {
    	EduCourse course = (EduCourse)valueForKeyPath("substitute.lesson.course");
    	if(course == null)
    		return null;
		WOComponent nextPage = pageWithName("LessonNoteEditor");
		nextPage.takeValueForKey(substitute.lesson(),"currLesson");
		nextPage.takeValueForKey(course,"course");
		nextPage.takeValueForKey(nextPage.valueForKey("currLesson"),"selector");
		session().takeValueForKey(context().page(),"pushComponent");
		return nextPage;

    }
/*    public EduCourse eduCourse() {
    	if(_course == null) {
    		EduLesson lesson = (EduLesson)valueForBinding("lesson");
    		_course = lesson.course();
    	}
    	return _course;
    }*/
    
    public Boolean cantCreate() {
    	if(cantCreate==null)
    		cantCreate = (Boolean)session().valueForKeyPath("readAccess._create.Substitute");
    	return cantCreate;
    }
    
    public boolean show() {
    	return (!cantCreate().booleanValue() || !empty() || variation() != null ||
    			Various.boolForObject(session().valueForKeyPath("readAccess.create.Variation")));
    }
    
    public boolean empty() {
    	return ((joins() == null || joins().count() == 0) &&
    			(subsList() == null || subsList().count() == 0));
    }
    
   /*
    public String substitutor() {
    	Substitute sub = substitute();
    	if(sub == null || sub.teacher() == eduCourse().teacher())
    		return null;
    	return Person.Utility.fullName(sub.teacher(), true, 2, 1, 1);
    }
    
	public String insteadof() {
    	Substitute sub = substitute();
    	if(sub == null || sub.eduCourse() == eduCourse())
    		return null;
		StringBuffer buf = new StringBuffer((String)
				application().valueForKeyPath("strings.RujelCurriculum_Curriculum.Instead"));
		buf.append(':').append(' ');
		if(sub.eduCourse() == null)
			buf.append(application().valueForKeyPath("strings.RujelCurriculum_Curriculum.noSubject"));
		else
			buf.append(sub.eduCourse().cycle().subject());
		return buf.toString();
	}*/
   
	public boolean isStateless() {
		return true;
	}
	
	public boolean synchronizesVariablesWithBindings() {
		return false;
	}
	
	public void reset() {
		super.reset();
//		_dict = null;
//		_course = null;
		substitute = null;
		subsList = null;
		cantCreate = null;
		_variation = null;
		joins = null;
	}
	/*
	public Boolean cantEdit() {
		if(substitute == null)
			return (Boolean)session().valueForKeyPath("readAccess._create.Substitute");
		else
			return (Boolean)session().valueForKeyPath("readAccess._edit.substitute");
	} */

	public WOActionResults edit() {
		String pageName = "SubsTypeSelector";
		Object lesson = valueForBinding("lesson");
		if(substitute != null) {
//			if((substitute.fromLesson() == lesson))
//				pageName = "EditJoin";
//			else
				pageName = "EditSubstitute";
		} else if(joins() != null && joins().count() > 0) {
			pageName = "EditJoin";
		} else if(subsList() != null && subsList().count() > 0) {
			pageName = "EditSubstitute";
		}
		WOComponent editor = pageWithName(pageName);
		editor.takeValueForKey(context().page(), "returnPage");
		editor.takeValueForKey(lesson, "lesson");
		if(substitute != null)
			editor.takeValueForKey(substitute, "substitute");
		if(substitute == null && pageName.equals("EditSubstitute")) {
			Substitute sub = (Substitute) subsList().objectAtIndex(0);
			if(sub.fromLesson() != null)
				editor.takeValueForKey(new Integer(1), "idx");
		}
		return editor;
	}
	/*
	public WOActionResults addJoin() {
		WOComponent editor = pageWithName("SubsTypeSelector");
		editor.takeValueForKey(context().page(), "returnPage");
		editor.takeValueForKey(valueForBinding("lesson"), "lesson");	
		return editor;
	}*/

/*	
	public String cellClass() {
		if(substitute == null)
			return "grey";
		else
			return "gerade";
	}

	public String subsTitle() {
		if(Various.boolForObject(valueForKeyPath("substitute.sFlags.join")))
			return (String)application().valueForKeyPath("strings.RujelCurriculum_Curriculum.Join");
		return (String)application().valueForKeyPath("strings.RujelCurriculum_Curriculum.Substitute");
	}*/
	
	protected Object _variation;
	public Variation variation() {
		if(_variation == null) {
   		EduLesson lesson = (EduLesson)valueForBinding("lesson");
		NSArray args = new NSArray(new Object[] {lesson.course(),lesson.date()});
		EOQualifier qual = EOQualifier.qualifierWithQualifierFormat(
				"course = %@ AND date = %@ AND value >= 1", args);
		EOFetchSpecification fs = new EOFetchSpecification(Variation.ENTITY_NAME,qual,null);
		NSArray found = lesson.editingContext().objectsWithFetchSpecification(fs);
		if(found == null || found.count() == 0)
			_variation = NullValue;
		else
			_variation = found.objectAtIndex(0);
		}
		if(_variation == NullValue)
			return null;
		else
			return (Variation)_variation;
	}
	
	public String isVarStyle() {
		if(Various.boolForObject(session().valueForKeyPath("readAccess._read.Variation")))
			return "background-color:#cccccc;color:#cccccc;";
		if(variation() == null)
			return "background-color:#999999;color:#cccccc;";
		return "font-weight:bold;color:#339966;background-color:#99ff99;";
	}
	
	public WOComponent addedLesson() {
		WOComponent nextPage = pageWithName("EditVariation");
		nextPage.takeValueForKey(Boolean.TRUE, "returnNormaly");
		nextPage.takeValueForKey(Boolean.TRUE, "onlyChooseReason");
   		EduLesson lesson = (EduLesson)valueForBinding("lesson");
   		nextPage.takeValueForKey(context().page(), "returnPage");
    	nextPage.takeValueForKey(lesson.course(), "course");
		if(variation() != null) {
			nextPage.takeValueForKey(variation(), "variation");
		} else {
	   		nextPage.takeValueForKey(lesson.date(),"date");
	   		nextPage.takeValueForKey(new Integer(1),"value");
		}
		return nextPage;
	}
}