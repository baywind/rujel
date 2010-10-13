// PrintWorks.java: Class file for WO Component 'PrintWorks'

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

package net.rujel.criterial;

import java.util.Date;

import net.rujel.interfaces.EduCourse;
import net.rujel.reusables.Period;

import com.webobjects.appserver.*;
import com.webobjects.eocontrol.EOAndQualifier;
import com.webobjects.eocontrol.EOFetchSpecification;
import com.webobjects.eocontrol.EOKeyValueQualifier;
import com.webobjects.eocontrol.EOQualifier;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSKeyValueCoding;
import com.webobjects.foundation.NSMutableArray;
import com.webobjects.foundation.NSTimestamp;

// Generated by the WOLips Templateengine Plug-in at Jun 26, 2009 3:09:09 PM
public class PrintWorks extends com.webobjects.appserver.WOComponent {
	
	public EduCourse course;
	public Period period;
	public NSArray list;
	public Work work;
	public Object critItem;
	
    public PrintWorks(WOContext context) {
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
    		qual = new EOKeyValueQualifier(Work.DATE_KEY,
    				EOQualifier.QualifierOperatorGreaterThanOrEqualTo,date);
    		quals.addObject(qual);
    		date = period.end();
    		if(!(date instanceof NSTimestamp))
    			date = new NSTimestamp(date);
    		qual = new EOKeyValueQualifier(Work.ANNOUNCE_KEY,
    				EOQualifier.QualifierOperatorLessThanOrEqualTo,date);
    		quals.addObject(qual);
    		qual = new EOAndQualifier(quals);
    	}
    	EOFetchSpecification fs = new EOFetchSpecification(Work.ENTITY_NAME,qual,Work.sorter);
    	fs.setRefreshesRefetchedObjects(true);
    	list = course.editingContext().objectsWithFetchSpecification(fs);
    	if(list == null)
    		list = NSArray.EmptyArray;
    	return list;
    }
    
	private NSArray _criteria;
    public NSArray criteria() {
		if(_criteria == null) {
			_criteria = CriteriaSet.criteriaForCourse(course);
			//criteriaForCycle(course.cycle());
			if(_criteria == null)
				_criteria = NSArray.EmptyArray;
		}
		return _criteria;
    }

	public Object critMax() {
		if(work == null)
			return null;
		if(critItem == null)
			critItem = new Integer(0);
		else if(critItem instanceof NSKeyValueCoding)
			critItem = ((NSKeyValueCoding)critItem).valueForKey("criterion");
		return work.maxForCriter((Integer)critItem);
	}

	private String colspan;
	public String colspan() {
		if(colspan == null)
			colspan = Integer.toString(3 + criteria().count());
		return colspan;
	}
	
	public String rowspan() {
		if(work.homeTask() == null)
			return null;
		return " rowspan=\"2\"";
	}
	
	public String style() {
		if(work.hasWeight())
			return "font-weight:bold;";
		return null;
	}
	
	public String criterlessMax() {
		if(work == null)
			return null;
		if(!work.noCriteria())
			return null;
		StringBuilder buf = new StringBuilder("<td align = \"center\"");
		if(criteria() != null && criteria().count() > 1)
			buf.append(" colspan = \"").append(criteria().count()).append('"');
		else
			buf.append(" style = \"font-weight:bold;\"");
		buf.append('>');
		Object max = work.maxForCriter(new Integer(0));
		if(max == null)
			buf.append('-');
		else
			buf.append(max);
		buf.append("</td>");
		return buf.toString();
	}
}