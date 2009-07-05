//  ItogContainer.java

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

package net.rujel.eduresults;

import net.rujel.reusables.SessionedEditingContext;
import net.rujel.reusables.Various;

import com.webobjects.foundation.*;
import com.webobjects.eocontrol.*;

public class ItogContainer extends _ItogContainer {
	
	public static NSArray sorter = new NSArray(new EOSortOrdering[] {
			new EOSortOrdering(EDU_YEAR_KEY,EOSortOrdering.CompareAscending),
			new EOSortOrdering("itogType.sort",EOSortOrdering.CompareAscending),
			new EOSortOrdering(NUM_KEY,EOSortOrdering.CompareAscending)
	}); 

	public static void init() {
	}

	public void awakeFromInsertion(EOEditingContext ec) {
		super.awakeFromInsertion(ec);
		setNum(new Integer(0));
		if(editingContext() instanceof SessionedEditingContext) {
			Integer year = (Integer)((SessionedEditingContext)
					editingContext()).session().valueForKey("eduYear");
			if(year != null)
				setEduYear(year);
		}
	}
	
	public String title() {
		if(num() > 0) {
			return Various.makeRoman(num().intValue()) + ' ' + itogType().title();
		}
		return itogType().title();
	}
	
	public String name() {
		if(num() > 0) {
			return Various.makeRoman(num().intValue()) + ' ' + itogType().name();
		}
		return itogType().name();
	}
	
	public static class ComparisonSupport extends EOSortOrdering.ComparisonSupport {
		
		public int compareAscending(Object left, Object right)  {
			if(!(left instanceof ItogContainer))
				return NSComparator.OrderedAscending;
			ItogContainer l = (ItogContainer)left;
			if(!(right instanceof ItogContainer))
				return NSComparator.OrderedDescending;
			ItogContainer r = (ItogContainer)right;
			int result = compareValues(l.eduYear(), r.eduYear(),
					EOSortOrdering.CompareAscending);
			if(result == NSComparator.OrderedSame)
			result = compareValues(l.valueForKey("itogType.sort"), 
					r.valueForKey("itogType.sort"), EOSortOrdering.CompareAscending);
			if(result == NSComparator.OrderedSame)
				result = compareValues(l.num(), r.num(), EOSortOrdering.CompareAscending);
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
