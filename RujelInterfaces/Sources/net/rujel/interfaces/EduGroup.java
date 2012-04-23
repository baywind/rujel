// EduGroup.java

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

//import java.util.Collection;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import com.webobjects.foundation.NSComparator;
import com.webobjects.foundation.NSTimestamp;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSForwardException;
import com.webobjects.eocontrol.EOEditingContext;
import com.webobjects.eocontrol.EOSortOrdering;

/** Should implement static method <tt>public static NSArray listGroups(NSTimestamp date, EOEditingContext ec)</tt>  */

public interface EduGroup extends PersonGroup {
	
	public static final String className = net.rujel.reusables.SettingsReader.stringForKeyPath(
			"interfaces.EduGroup",null);
	public static final String entityName = className.substring(1 + className.lastIndexOf('.'));
	public static final NSArray sorter = new NSArray(new EOSortOrdering[] {
			new EOSortOrdering("grade",EOSortOrdering.CompareAscending),
			new EOSortOrdering("title",EOSortOrdering.CompareAscending)});
	
	public Integer grade();
	public String title();
	
	/** If this eduGroup is valid only for specific eduYear. Otherwise null */
	public Integer eduYear();
	
	//public NSArray studentsList();
	//public Collection<Student> studentsList();

	//
	public static class Lister {
		protected static Method method;
		public static NSArray listGroups(NSTimestamp date,EOEditingContext ec) {
			try {
				if(method == null) {
					Class aClass = Class.forName(EduGroup.className);
					method = aClass.getMethod("listGroups",NSTimestamp.class,EOEditingContext.class);
				}
				return (NSArray)method.invoke(null,date,ec);
			} catch (Exception ex) {
				throw new NSForwardException(ex, "Could not initialise EduGroup listing method");
			}
		}
		
		protected static NSArray aSorter;
		public static NSArray sorter() {
			if(aSorter == null) {
				try {
					Class aClass = Class.forName(EduGroup.className);
					Field field = aClass.getDeclaredField("sorter");
					aSorter = (NSArray)field.get(null);
				} catch (Exception ex) {
				}
				if(aSorter == null)
					aSorter = sorter;
			}
			return aSorter;
		}
	}
	
	public static class ComparisonSupport extends EOSortOrdering.ComparisonSupport {

		public int compareAscending(Object left, Object right) {
			try {
				EduGroup leftGroup = (EduGroup)left;
				EduGroup rightGroup = (EduGroup)right;
				int result = compareValues(leftGroup.grade(), rightGroup.grade(),
						EOSortOrdering.CompareAscending);
				if(result != NSComparator.OrderedSame)
					return result;
				result = compareValues(leftGroup.title(), rightGroup.title(),
						EOSortOrdering.CompareAscending);
				return result;
			} catch (Exception e) {
				return super.compareAscending(left, right);
			}
		}

		public int compareCaseInsensitiveAscending(Object left, Object right) {
			try {
				EduGroup leftGroup = (EduGroup)left;
				EduGroup rightGroup = (EduGroup)right;
				int result = compareValues(leftGroup.grade(), rightGroup.grade(),
						EOSortOrdering.CompareCaseInsensitiveAscending);
				if(result != NSComparator.OrderedSame)
					return result;
				result = compareValues(leftGroup.title(), rightGroup.title(),
						EOSortOrdering.CompareCaseInsensitiveAscending);
				return result;
			} catch (Exception e) {
				return super.compareCaseInsensitiveAscending(left, right);
			}
		}

		public int compareDescending(Object left, Object right) {
			return compareAscending(right, left);
		}

		public int compareCaseInsensitiveDescending(Object left, Object right) {
			return compareCaseInsensitiveAscending(right, left);
		}
	}
}