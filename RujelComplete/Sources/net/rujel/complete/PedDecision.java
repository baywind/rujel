//  PedDecision.java

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

package net.rujel.complete;

import net.rujel.base.SettingsBase;
import net.rujel.interfaces.EOInitialiser;
import net.rujel.interfaces.EduGroup;
import net.rujel.interfaces.Student;
import net.rujel.reusables.Various;

import com.webobjects.eoaccess.EOObjectNotAvailableException;
import com.webobjects.eoaccess.EOUtilities;
import com.webobjects.eocontrol.*;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSDictionary;
import com.webobjects.foundation.NSMutableDictionary;

public class PedDecision extends _PedDecision {

	public static void init() {
		EOInitialiser.initialiseRelationship(ENTITY_NAME,"student",false,"studentID","Student")
			.anyInverseRelationship().setPropagatesPrimaryKey(true);
	}
	
	public Student student() {
        return (Student)storedValueForKey("student");
    }
	
    public void setStudent(EOEnterpriseObject aValue) {
        takeStoredValueForKey(aValue, "student");
    }
	
	public static String titleSetting = "pedsovetTitle";
	public static String decisionSetting = "pedsovetDecision";

	public void awakeFromInsertion(EOEditingContext ec) {
		super.awakeFromInsertion(ec);
		setFlags(new Integer(0));
	}

	public void turnIntoFault(EOFaultHandler handler) {
		super.turnIntoFault(handler);
	}
	
	public static PedDecision setForStudent(Student student, boolean on) {
		EOEditingContext ec = student.editingContext();
		try {
			PedDecision dec = (PedDecision)EOUtilities.objectMatchingKeyAndValue(
					ec, ENTITY_NAME, "student", student);
			if(on)
				return dec;
			else if (dec.specDecision() == null && dec.flags().intValue() == 0)
				ec.deleteObject(dec);
		} catch (EOObjectNotAvailableException e) {
			if(on) {
				PedDecision dec = (PedDecision)EOUtilities.createAndInsertInstance(ec, ENTITY_NAME);
				dec.setStudent(student);
			}
		}
		return null;
	}
	
	public static NSArray getForStudents(NSArray list, EOEditingContext ec) {
		EOQualifier qual = Various.getEOInQualifier("student", list);
		EOFetchSpecification fs = new EOFetchSpecification(ENTITY_NAME,qual,null);
		NSArray found = ec.objectsWithFetchSpecification(fs);
		return found;
	}
	
	public static NSMutableDictionary dictForGroup(EduGroup gr) {
		EOEditingContext ec = gr.editingContext();
		NSMutableDictionary dict = new NSMutableDictionary();
		NSDictionary crs = SettingsBase.courseDict(gr);
		dict.takeValueForKey(
				SettingsBase.stringSettingForCourse("pedsovetTitle",crs, ec), "title");
		EOQualifier qual = Various.getEOInQualifier("student", gr.list());
		EOFetchSpecification fs = new EOFetchSpecification(ENTITY_NAME,qual,null);
		NSArray found = ec.objectsWithFetchSpecification(fs);
		String dflt = SettingsBase.stringSettingForCourse("pedsovetDecision",crs, ec);
		if(dflt != null && gr.grade() != null) {
			int next = gr.grade().intValue();
			next++;
			dflt = String.format(dflt, new Integer(next));
		}
		for (int i = 0; i < found.count(); i++) {
			PedDecision dec = (PedDecision)found.objectAtIndex(i);
			String decision = dec.specDecision();
			if(decision == null)
				decision = dflt;
			else if(decision.length() == 0)
				continue;
			dict.setObjectForKey(decision, dec.student());
		}
		return dict;
	}
}
