// SubjectComparator.java

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

package net.rujel.eduplan;

import com.webobjects.eocontrol.EOEnterpriseObject;
import com.webobjects.eocontrol.EOSortOrdering;
import com.webobjects.foundation.NSComparator;
import com.webobjects.foundation.NSForwardException;
import com.webobjects.foundation.NSKeyValueCoding;

public class SubjectComparator extends NSComparator {
	private static final String SUBJECT = "Subject";
	private static final String AREA = "SubjectArea";
	private static final String num = "num";
	private static final String areaNum = "area.num";
		
	public int compare(Object arg0, Object arg1) throws ComparisonException {
		if(arg1 == arg0)
			return OrderedSame;
		if(arg0 == null || arg0 instanceof NSKeyValueCoding.Null)
			return OrderedAscending;
		if(arg1 == null || arg1 instanceof NSKeyValueCoding.Null)
			return OrderedDescending;
		if(!(arg0 instanceof EOEnterpriseObject && arg1 instanceof EOEnterpriseObject))
			throw new ComparisonException("Can only compare EOEnterpriseObjects");
		EOEnterpriseObject left = (EOEnterpriseObject)arg0;
		EOEnterpriseObject right = (EOEnterpriseObject)arg1;
		return compare(left, right);
	}
	
	public static int compare(EOEnterpriseObject left, EOEnterpriseObject right) throws ComparisonException {
		int result = 0;
		try {
			Number num1 = null;
			Number num2 = null;
			if(left.entityName().equals(SUBJECT)) {
				if(right.entityName().equals(SUBJECT)) {
					if(((Subject)left).area() != ((Subject)right).area()) {
						num1 = (Number)left.valueForKeyPath(areaNum);
						num2 = (Number)right.valueForKeyPath(areaNum);
					} else {
						SubjectGroup lg = ((Subject)left).subjectGroup();
						SubjectGroup rg = ((Subject)right).subjectGroup();
						if(lg == rg) {
							num1 = (Number)left.valueForKey(num);
							num2 = (Number)right.valueForKey(num);
						} else {
							SubjectGroup ag = (SubjectGroup)left.valueForKeyPath("area.subjectGroup");
							if(ag == null)
								return SubjectGroupComparator.compare(lg,rg);
							if(lg.path().containsObject(ag)) {
								if(rg.path().containsObject(ag))
									return SubjectGroupComparator.compare(lg,rg);
								else
									return OrderedAscending;
							} else {
								if(rg.path().containsObject(ag))
									return OrderedDescending;
								else
									return SubjectGroupComparator.compare(lg,rg);
							}
						}
					}
				} else if(right.entityName().equals(AREA)) {
					num1 = (Number)left.valueForKeyPath(areaNum);
					num2 = (Number)right.valueForKey(num);
				} else {
					return OrderedAscending;
				}
			} else if (right.entityName().equals(SUBJECT)) {
				if(left.entityName().equals(AREA)) {
					num1 = (Number)left.valueForKey(num);
					num2 = (Number)right.valueForKeyPath(areaNum);
				} else {
					return OrderedDescending;
				}
			}
			if(num1 == null || num2 == null || num1.equals(num2)) {
				num1 = (Number)left.valueForKey(num);
				num2 = (Number)right.valueForKey(num);
			}
			result = AscendingNumberComparator.compare(num1,num2);
		} catch (Exception e) {
			ComparisonException ce = new ComparisonException("Error comparing given objects: " + e);
			ce.initCause(e);
			throw ce;
		}
		return result;
	}

	public static class ComparisonSupport extends EOSortOrdering.ComparisonSupport {
		SubjectComparator comparator = new SubjectComparator();
				
		public int compareAscending(Object left, Object right)  {
			try {
				return comparator.compare(left, right);
			} catch  (ComparisonException ex) {
				throw new NSForwardException(ex,"Error comparing");
			}
		}	
		public int compareCaseInsensitiveAscending(Object left, Object right)  {
			return compareAscending(left, right) ;
		}
		
		public int compareDescending(Object left, Object right)  {
			try {
				return comparator.compare(right,left);
			} catch  (ComparisonException ex) {
				throw new NSForwardException(ex,"Error comparing");
			}
		}
		public int compareCaseInsensitiveDescending(Object left, Object right)  {
			return compareDescending(left, right);
		}

	}
}
