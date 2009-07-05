//  Period.java

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

import com.webobjects.eocontrol.EOEnterpriseObject;
import com.webobjects.eocontrol.EOSortOrdering;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSTimestamp;

import net.rujel.reusables.Period;

public interface EOPeriod extends Period,EOEnterpriseObject {
 	public static final NSArray sorter = new NSArray(new Object[] {
 			EOSortOrdering.sortOrderingWithKey("end",EOSortOrdering.CompareAscending),
 			EOSortOrdering.sortOrderingWithKey("begin",EOSortOrdering.CompareDescending)});

 	
	public NSTimestamp begin();
	
	public NSTimestamp end();
	
	//public boolean contains(NSTimestamp date);

	public static class ComparisonSupport extends EOSortOrdering.ComparisonSupport {
		
		public int compareAscending(Object left, Object right)  {
			Period l = (Period)left;
			Period r = (Period)right;
			int result = l.end().compareTo(r.end());
			if(result == 0)
				result = r.begin().compareTo(l.begin());
			return result;
		}
		public int compareCaseInsensitiveAscending(Object left, Object right)  {
			return compareAscending(left, right) ;
		}
		
		public int compareDescending(Object left, Object right)  {
			return compareAscending(right, left) ;
		}
		public int compareCaseInsensitiveDescending(Object left, Object right)  {
			return compareAscending(right, left) ;
		}
	}

}
