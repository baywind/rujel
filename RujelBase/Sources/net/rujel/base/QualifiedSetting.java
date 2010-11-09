//  QualifiedSetting.java

/*
 * Copyright (c) 2008, Gennady & Michael Kushnir
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 * 
 * 	o	Redistributions of source code must retain the above copyright notice, this
 * 		list of conditions and the following disclaimer.
 * 	o	Redistributions in binary form must reproduce the above copyright notice,
 * 		this list of conditions and the following disclaimer in the documentation
 * 		and/or other materials provided with the distribution.
 * 	o	Neither the name of the RUJEL nor the names of its contributors may be used
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

package net.rujel.base;

import net.rujel.interfaces.EduCourse;
import net.rujel.reusables.Various;
import net.rujel.reusables.WOLogFormatter;

import com.webobjects.eoaccess.EOUtilities;
import com.webobjects.eocontrol.*;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSKeyValueCodingAdditions;
import com.webobjects.foundation.NSMutableArray;
import com.webobjects.foundation.NSPropertyListSerialization;

public class QualifiedSetting extends _QualifiedSetting {

	protected EOQualifier qualifier;
	protected NSArray courses;
	
	public void setQualifierString(String string) {
		nullufy();
		super.setQualifierString(string);
	}
	
	public void setArgumentsString(String string) {
		nullufy();
		super.setArgumentsString(string);
	}
	
	public void setQualifier(EOQualifier qual) {
		StringBuilder buf = new StringBuilder();
		NSMutableArray args = new NSMutableArray();
		Various.formatQualifier(qual, buf, args);
		setQualifierString(buf.toString());
		setArgumentsString(NSPropertyListSerialization.stringFromPropertyList(args,false));
		qualifier = qual;
	}
	
	public void setCourse(EduCourse course) {
		String format = WOLogFormatter.formatEO(course);
		setQualifierString("IS");
		setArgumentsString(format);
		courses = new NSArray(EOUtilities.localInstanceOfObject(editingContext(), course));
	}
	
	public void setCourses(NSArray newCourses) {
		setQualifierString("IN");
		setArgumentsString(Various.stringFromArguments(newCourses));
		courses = EOUtilities.localInstancesOfObjects(editingContext(), newCourses);
	}

	public void addCourse(EduCourse course) {
		course = (EduCourse)EOUtilities.localInstanceOfObject(editingContext(), course);
		if(courses == null)
			courses = new NSArray(course);
		else
			courses = courses.arrayByAddingObject(course);
		super.setQualifierString("IN");
		super.setArgumentsString(Various.stringFromArguments(courses));
		qualifier = null;
	}
	
	public EOQualifier getQualifier() {
		if(courses == null && qualifier == null)
			read();
		return qualifier;
	}
	
	public NSArray getCourses() {
		if(courses == null && qualifier == null)
			read();
		return (courses == null)?null:courses.immutableClone();
	}
	
	protected void read() {
		String qualifierString = qualifierString();
		if(qualifierString.equals("IS")) {
			courses = new NSArray(Various.parseEO(qualifierString, editingContext()));
			qualifier = null;
		} else {
			courses = Various.argumentsFromString(argumentsString(), editingContext());
			if(qualifierString.equals("IN")) {
				qualifier = null;
			} else {
				qualifier = EOQualifier.qualifierWithQualifierFormat(qualifierString,courses);
				courses = null;
			}
		}
	}
	
	public boolean evaluateWithObject(NSKeyValueCodingAdditions object) {
		if(courses == null && qualifier == null) {
			read();
		}
		if(object instanceof EduCourse) {
			object = EOUtilities.localInstanceOfObject(editingContext(),
					(EOEnterpriseObject)object);
			if(eduYear() != null && !eduYear().equals(((EduCourse)object).eduYear()))
				return false;
		}
		if(courses != null)
			return courses.containsObject(object);
		else if (qualifier != null)
			return qualifier.evaluateWithObject(object);
		else
			return false;
	}

	public void awakeFromInsertion(EOEditingContext ec) {
		super.awakeFromInsertion(ec);
		setSort(new Integer(1));
	}

	public void nullufy() {
		courses = null;
		qualifier = null;
	}
	public void turnIntoFault(EOFaultHandler handler) {
		nullufy();
		super.turnIntoFault(handler);
	}
	/*
	public int compare(NSKeyValueCoding other) {
		return 0; // TODO: write comparison
	}*/
}
