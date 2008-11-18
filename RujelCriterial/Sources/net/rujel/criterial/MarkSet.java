// MarkSet.java

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

import net.rujel.interfaces.Student;
import com.webobjects.foundation.*;
import com.webobjects.eocontrol.*;
import com.webobjects.eoaccess.EOUtilities;
import java.util.Enumeration;

public class MarkSet {
	//TODO: what is it?
	protected NSArray _criters;
	protected NSArray _marks;
	protected NSArray _mutators;
	private Student student;
	private Work work;
	private boolean valid = true;
	
	public MarkSet (Work work,Student student) {
		this.work = work;
		this.student = student;
	}
	
	public MarkSet (Work work,Student student, Mark[] marks) {
		this.work = work;
		this.student = student;
		_marks = new NSArray(marks);
		NSMutableArray tmpMutator = new NSMutableArray();
		for (int i = 0; i < marks.length; i++) {
			if(valid)
				tmpMutator.addObject(marks[i]);
		}
	}
	

	protected void init() {
		NSArray args = new NSArray(new Object[] {work,student});
		NSMutableArray allMarks = EOUtilities.objectsWithQualifierFormat(work.editingContext(),"Mark","work = @% AND student = @%",args).mutableClone();
		_criters = work.usedCriteria();
		NSMutableArray tmp = _criters.mutableClone();
		int pos = allMarks.count() -1;
		Mark currMark = null;
		for (int i = pos; i >=0 ; i--) {
			currMark = (Mark)allMarks.objectAtIndex(i);
			pos = _criters.indexOfObject(currMark.criterion());
			if(pos >= 0) {
				tmp.replaceObjectAtIndex(currMark,pos);
				allMarks.removeObjectAtIndex(i);
			}
		}
		for (int i = 0; i < tmp.count(); i++) {
			if(!(tmp.objectAtIndex(i) instanceof Mark)) {
				tmp.replaceObjectAtIndex(NSKeyValueCoding.NullValue,i);
			}
		}
		if(allMarks.count() > 0) {
			valid = false;
			EOSortOrdering sorter = EOSortOrdering.sortOrderingWithKey("criterion.sort",EOSortOrdering.CompareAscending);
			EOSortOrdering.sortArrayUsingKeyOrderArray(allMarks,new NSArray(sorter));
			NSArray criteria = (NSArray)allMarks.valueForKey("criterion");
			_criters = _criters.arrayByAddingObjectsFromArray(criteria);
			tmp.addObjectsFromArray(allMarks);
		} else {
			valid = true;
		}
		_marks = tmp.immutableClone();
	}
	
	public NSArray marks() {
		if(_marks == null || _criters == null) {
			init();
		}
		return _marks;
	}
	
	public Mark markForCriterion(EOEnterpriseObject criterion) {
		NSArray args = new NSArray(new Object[] {work,student,criterion});
		NSMutableArray result = EOUtilities.objectsWithQualifierFormat(work.editingContext(),"Mark","work = @% AND student = @% AND criterion = @%",args).mutableClone();
		if(result == null || result.count() == 0)
			return null;
		if(result.count() > 1)
			throw new IllegalStateException("Multiple marks found");
		return (Mark)result.objectAtIndex(0);
	}
	
	public double integral() {
		if(_marks == null || _criters == null) {
			init();
		}
		Number weightSum = (Number)_criters.valueForKeyPath("@sum.weight");
		if(weightSum == null)
			throw new IllegalStateException("Can't get sum wieght for integral calculation");
		double result = (double)0;
		Enumeration en = _marks.objectEnumerator();
		Object mark = null;
		while (en.hasMoreElements()) {
			mark = en.nextElement();
			if(mark instanceof Mark) {
				result = result + ((Mark)mark).weightedFraction();
			}
		}
		result = result / weightSum.doubleValue();
		return result;
	}
	/*
	public static class MarkMutator implements NSKeyValueCoding {
		private Work _work;
		private Student _student;
		private EOEnterpriseObject _criterion;
		private Mark _mark;
	}*/
}
