// PrintReprimands.java: Class file for WO Component 'PrintReprimands'

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

import java.util.Date;

import net.rujel.interfaces.EduCourse;
import net.rujel.reusables.Period;

import com.webobjects.appserver.*;
import com.webobjects.eocontrol.EOAndQualifier;
import com.webobjects.eocontrol.EOFetchSpecification;
import com.webobjects.eocontrol.EOKeyValueQualifier;
import com.webobjects.eocontrol.EOQualifier;
import com.webobjects.eocontrol.EOSortOrdering;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSMutableArray;
import com.webobjects.foundation.NSTimestamp;

// Generated by the WOLips Templateengine Plug-in at Jun 27, 2009 2:42:35 PM
public class PrintReprimands extends com.webobjects.appserver.WOComponent {

	public EduCourse course;
	public Period period;
	public NSArray list;
	public Reprimand item;
    
	public PrintReprimands(WOContext context) {
        super(context);
    }
	   public NSArray list() {
	    	if(list != null)
	    		return list;
	    	EOQualifier qual = new EOKeyValueQualifier("course",
	    			EOQualifier.QualifierOperatorEqual,course);
	    	if(period != null) {
	    		NSMutableArray quals = new NSMutableArray(qual);
	    		Date date = period.begin();
	    		if(!(date instanceof NSTimestamp))
	    			date = new NSTimestamp(date);
	    		qual = new EOKeyValueQualifier(Reprimand.RELIEF_KEY,
	    				EOQualifier.QualifierOperatorGreaterThanOrEqualTo,date);
	    		quals.addObject(qual);
	    		date = period.end();
	    		if(!(date instanceof NSTimestamp))
	    			date = new NSTimestamp(date);
	    		qual = new EOKeyValueQualifier(Reprimand.RAISED_KEY,
	    				EOQualifier.QualifierOperatorLessThanOrEqualTo,date);
	    		quals.addObject(qual);
	    		qual = new EOAndQualifier(quals);
	    	}
			EOSortOrdering so = new EOSortOrdering(Reprimand.RAISED_KEY, 
					EOSortOrdering.CompareAscending);
	    	EOFetchSpecification fs = new EOFetchSpecification
	    						(Reprimand.ENTITY_NAME,qual,new NSArray(so));
	    	list = course.editingContext().objectsWithFetchSpecification(fs);
	    	if(list == null)
	    		list = NSArray.EmptyArray;
	    	return list;
	   }
}