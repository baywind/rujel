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
	
	public static final int MANUAL_FLAG = 8;
	
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
		if(gr == null)
			return null;
		EOEditingContext ec = gr.editingContext();
		NSMutableDictionary dict = new NSMutableDictionary();
		NSDictionary crs = SettingsBase.courseDict(gr);
		dict.takeValueForKey(
				SettingsBase.stringSettingForCourse("pedsovetTitle",crs, ec), "title");
		EOQualifier qual = Various.getEOInQualifier("student", gr.list());
		EOFetchSpecification fs = new EOFetchSpecification(ENTITY_NAME,qual,null);
		NSArray found = ec.objectsWithFetchSpecification(fs);
		String dflt = defaultDecision(gr);
		for (int i = 0; i < found.count(); i++) {
			PedDecision dec = (PedDecision)found.objectAtIndex(i);
			String decision = dec.specDecision();
			if(decision == null)
				decision = formatDefault(dflt, dec.student());
			else if(decision.length() == 0)
				continue;
			dict.setObjectForKey(decision, dec.student());
		}
		return dict;
	}
	
	public static String defaultDecision(EduGroup gr) {
		NSDictionary crs = SettingsBase.courseDict(gr);
		String dflt = SettingsBase.stringSettingForCourse(
				"pedsovetDecision",crs, gr.editingContext());
		if(dflt == null || gr.grade() == null)
			return dflt;
		int next = gr.grade().intValue();
		next++;
		return String.format(dflt, new Integer(next));
	}
	
	public static String formatDefault(String result, Student stu) {
		int idx1 = result.indexOf('[');
		if(idx1 < 0)
			return result;
		int idx2 = result.indexOf(']', idx1);
		if(idx2 < 0)
			return result;
		StringBuilder buf = new StringBuilder(result.length());
		buf.append(result.substring(0, idx1));
		while (idx1 >= 0) {
			int idx3 = result.indexOf('|', idx1);
			if(idx3 > idx1 && idx3 < idx2) {
				if(stu.person().sex()) {
					if(idx3 > idx1 +1)
						buf.append(result.substring(idx1 +1, idx3));
				} else {
					if(idx2 > idx3 +1)
						buf.append(result.substring(idx3 +1, idx2));
				}
			}
			idx1 = result.indexOf('[',idx2);
			idx3 = result.indexOf(']',idx3);
			if(idx1 > idx2 && idx3 > 1) {
				buf.append(result.substring(idx2 +1, idx1));
				idx2 = idx3;
			}
		} // while (idx1 >= 0)
		buf.append(result.substring(idx2 +1));
		return buf.toString();
	}
}
