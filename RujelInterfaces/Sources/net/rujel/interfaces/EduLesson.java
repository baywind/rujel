// EduLesson.java

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

package net.rujel.interfaces;

//import net.rujel.auth.*;
import com.webobjects.foundation.NSTimestamp;//java.util.Date;
import com.webobjects.foundation.NSArray;
import com.webobjects.eocontrol.EOEnterpriseObject;
import com.webobjects.eocontrol.EOSortOrdering;

public interface EduLesson extends PerPersonLink,EOEnterpriseObject {
	public static final String className = net.rujel.reusables.SettingsReader.
			stringForKeyPath("interfaces.EduLesson",null);
	public static final String entityName = className.substring(1
			+ className.lastIndexOf('.'));

	public static final NSArray sorter = new NSArray(new EOSortOrdering[] {
		EOSortOrdering.sortOrderingWithKey("number", EOSortOrdering.CompareAscending),
		EOSortOrdering.sortOrderingWithKey("date", EOSortOrdering.CompareAscending),
	});

	public EduCourse course();
	public void setCourse(EduCourse newCourse);
	
	public Integer number();
	public void setNumber(Integer newNumber);
	
	public NSTimestamp date();
	public void setDate(NSTimestamp newDate);

	public String title();
	public void setTitle(String newTitle);
	
	public String theme();
	public void setTheme(String newTheme);
	
	public String homeTask();
	public void setHomeTask(String newTask);

	public NSArray notes();
	public NSArray students();
	/*
	public Teacher substitute();
	public void setSubstitute(Teacher teacher);
	*/
	public String noteForStudent(Student student);
	public void setNoteForStudent(String note, Student student);
}