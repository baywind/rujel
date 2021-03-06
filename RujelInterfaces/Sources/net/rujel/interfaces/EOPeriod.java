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

import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;

import com.webobjects.eocontrol.EOEnterpriseObject;
import com.webobjects.eocontrol.EOSortOrdering;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSLocking;
import com.webobjects.foundation.NSTimestamp;

import net.rujel.reusables.Period;
import net.rujel.reusables.SettingsReader;

public interface EOPeriod extends Period,EOEnterpriseObject {
 	public static final NSArray sorter = new NSArray(new Object[] {
 			EOSortOrdering.sortOrderingWithKey("begin",EOSortOrdering.CompareAscending),
 			EOSortOrdering.sortOrderingWithKey("end",EOSortOrdering.CompareDescending)});
 	
	public NSTimestamp begin();
	
	public NSTimestamp end();
	
	public String name();
	
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

	
	public static class Utility {
		public static boolean contains(Period period, Date date) {
			if(date == null) return false;
			boolean begin = period.begin().compareTo(date) <= 0;
			if(!begin && period.begin().getTime() - date.getTime() > NSLocking.OneDay)
				return false;
			boolean end = period.end().compareTo(date) >= 0;
			if(!end && date.getTime() - period.end().getTime() > NSLocking.OneDay)
				return false;
			if(begin && end)
				return true;
			Calendar cal = Calendar.getInstance();
			if(!begin) {
				cal.setTime(period.begin());
				cal.set(Calendar.HOUR_OF_DAY, 0);
				cal.set(Calendar.MINUTE, 0);
				cal.set(Calendar.SECOND, 0);
				cal.set(Calendar.MILLISECOND, 0);
				begin = cal.getTimeInMillis() < date.getTime();
			}
			if(!begin)
				return false;
			if(!end) {
				cal.setTime(period.end());
				cal.set(Calendar.HOUR_OF_DAY, 23);
				cal.set(Calendar.MINUTE, 59);
				cal.set(Calendar.SECOND, 59);
				cal.set(Calendar.MILLISECOND, 999);
				end = cal.getTimeInMillis() > date.getTime();
			}
			return begin && end;
		}
		
		public static int countDays(Date begin, Date end) {
			Calendar cal1 = Calendar.getInstance();
			if(begin != null)
				cal1.setTime(begin);
			else if(cal1.get(Calendar.MILLISECOND) > 0 && cal1.get(Calendar.HOUR_OF_DAY) < 
					SettingsReader.intForKeyPath("edu.midnightHour", 5))
				cal1.add(Calendar.DATE, -1);
			
			Calendar cal2 = Calendar.getInstance();
			if(end != null)
				cal2.setTime(end);
			else if(cal2.get(Calendar.MILLISECOND) > 0 && cal2.get(Calendar.HOUR_OF_DAY) < 
					SettingsReader.intForKeyPath("edu.midnightHour", 5))
				cal2.add(Calendar.DATE, -1);
			
			int days = cal2.get(Calendar.DAY_OF_YEAR) - cal1.get(Calendar.DAY_OF_YEAR);
			while (cal1.get(Calendar.YEAR) != cal2.get(Calendar.YEAR)) {
				if(cal1.getTimeInMillis() > cal2.getTimeInMillis()) {
					days -= cal2.getActualMaximum(Calendar.DAY_OF_YEAR);
					cal2.add(Calendar.YEAR, 1);
				} else {
					days += cal1.getActualMaximum(Calendar.DAY_OF_YEAR);
					cal1.add(Calendar.YEAR, 1);
				}
			}
			return days +1;
		}
		
		public static int compareDates(Date first, Date second) {
			long firstLong = (first == null)?System.currentTimeMillis():first.getTime();
			long secondLong = (second == null)?System.currentTimeMillis():second.getTime();
			return compareDates(firstLong, first == null, secondLong, second == null);
		}
		public static int compareDates(long firstLong, long secondLong) {
			return compareDates(firstLong, false, secondLong, false);
		}
		public static int compareDates(long firstLong, boolean firstNull,
				long secondLong, boolean secondNull) {
			long a = firstLong - secondLong;
			if(a==0)
				return 0;
			int v = (a>0)? 1: -1;
			if(Math.abs(a) > 2*NSLocking.OneDay)
				return v;
			Calendar cal = Calendar.getInstance();
			cal.setTimeInMillis(firstLong);
			int day = cal.get(Calendar.DAY_OF_YEAR);
			if(firstNull && cal.get(Calendar.HOUR_OF_DAY) < 
					SettingsReader.intForKeyPath("edu.midnightHour", 5))
				day--;
			cal.setTimeInMillis(secondLong);
			if(secondNull && cal.get(Calendar.HOUR_OF_DAY) < 
					SettingsReader.intForKeyPath("edu.midnightHour", 5))
				day++;
			if(day == cal.get(Calendar.DAY_OF_YEAR))
				return 0;
			return v;
		}

		public static int intersect (Date since, Date to, Period per) {
			Date begin = per.begin();
			if(begin.compareTo(since) < 0)
				begin = since;
			Date end = per.end();
			if(end.compareTo(to) > 0)
				end = to;
			if(begin.compareTo(end) > 0)
				return 0;
			int result = countDays(begin, end);
//			if(result < 0)
//				result = 0;
			return result;
		}

		public static int verifyList(NSArray list) {
			if(list == null || list.count() < 2)
				return 0;
			list = EOSortOrdering.sortedArrayUsingKeyOrderArray(list, EOPeriod.sorter);
			Enumeration enu = list.objectEnumerator();
			NSTimestamp lastEnd = null;
			int result = 0;
			while (enu.hasMoreElements()) {
				EOPeriod per = (EOPeriod) enu.nextElement();
				if(lastEnd != null && lastEnd.compare(per.begin()) >= 0)
					result += EOPeriod.Utility.countDays(per.begin(), lastEnd);
				lastEnd = per.end();
			}
			return result;
		}
	}

	public static class ByDates implements Period {
		private NSTimestamp begin;
		private NSTimestamp end;
		
		public ByDates(NSTimestamp begin, NSTimestamp end) {
			this.begin = begin;
			this.end = end;
		}
		
		private static final String typeID = "EOPeriod.ByDates";
		public String typeID() {
			return typeID;
		}
		
		public NSTimestamp begin() {
			return begin;
		}
		public NSTimestamp end() {
			return end;
		}

		public boolean contains(Date date) {
			return Utility.contains(this, date);
		}
		
		public int countInYear() {
			return 0;
		}
		public EOPeriod nextPeriod() {
			return null;
		}
	}
}
