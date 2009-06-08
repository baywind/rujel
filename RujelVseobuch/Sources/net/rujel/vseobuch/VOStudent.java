// VOStudent.java

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

package net.rujel.vseobuch;

import net.rujel.interfaces.*;
import com.webobjects.foundation.*;
import com.webobjects.eocontrol.*;

public class VOStudent extends _VOStudent implements Student, Person
{
	public static void init() {
		EOSortOrdering.ComparisonSupport.setSupportForClass(
				new Person.ComparisonSupport(), VOStudent.class);
	}

	public VOStudent() {
        super();
    }

/*
    // If you add instance variables to store property values you
    // should add empty implementions of the Serialization methods
    // to avoid unnecessary overhead (the properties will be
    // serialized for you in the superclass).
    private void writeObject(java.io.ObjectOutputStream out) throws java.io.IOException {
    }

    private void readObject(java.io.ObjectInputStream in) throws java.io.IOException, java.lang.ClassNotFoundException {
    }
*/

	public EduGroup recentMainEduGroup() {
		EOQualifier qual1 = new EOKeyValueQualifier("isArhive",EOQualifier.QualifierOperatorNotEqual,new Integer(1));
		EOQualifier qual = new EOKeyValueQualifier("student",EOQualifier.QualifierOperatorEqual,this);
		qual = new EOAndQualifier(new NSArray(new Object[] {qual1, qual }));
		EOFetchSpecification fs = new EOFetchSpecification("VOList",qual,null);
		NSArray groups = editingContext().objectsWithFetchSpecification(fs);
		if (groups == null || groups.count() == 0) return null;
//		NSTimestamp today = (NSTimestamp)valueForKeyPath("editingContext.session.today");
		Integer year = (Integer)valueForKeyPath("editingContext.session.eduYear");
			//net.rujel.base.MyUtility.eduYearForDate(today);
		if(year == null)
			throw new IllegalStateException("Could not get current year");
		java.util.Enumeration en = groups.objectEnumerator();
		VOClass cl = null;
		while(en.hasMoreElements()) {
			cl = (VOClass)((EOEnterpriseObject)en.nextElement()).valueForKey("eduClass");
			if(year.equals(cl.eduYear()))
				return cl;
		}
		return null;
	}
	
	public String initials() {
		return Person.Utility.initials(this);//new String (new char[] {firstName().charAt(0),'.',' ',secondName().charAt(0),'.'});
	}
	
	public VOStudent person() {
		return this;
	}
	
	public void setPerson(Person pers) {
		if(pers != this)
			throw new UnsupportedOperationException("Person attribute can't be changed as it should always return this");
	}
}
