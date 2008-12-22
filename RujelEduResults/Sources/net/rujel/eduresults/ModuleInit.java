// ModuleInit.java

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

package net.rujel.eduresults;

import net.rujel.reusables.*;
import net.rujel.auth.*;
import net.rujel.interfaces.EduCourse;
import net.rujel.interfaces.EduLesson;

import com.webobjects.foundation.*;
import com.webobjects.eocontrol.*;
import com.webobjects.eoaccess.EOUtilities;
import com.webobjects.appserver.WOApplication;
import com.webobjects.appserver.WOContext;
import java.util.Enumeration;

public class ModuleInit {
	protected static final NSDictionary tab = (NSDictionary)WOApplication.application().valueForKeyPath("extStrings.RujelEduResults-EduResults.itogTab");
	protected static final NSDictionary addOn = (NSDictionary)WOApplication.application().valueForKeyPath("extStrings.RujelEduResults-EduResults.itogAddOn");
	protected static final NSDictionary studentReporter = (NSDictionary)WOApplication.application().valueForKeyPath("extStrings.RujelEduResults-EduResults.studentReporter");
	
	public static Object init(Object obj, WOContext ctx) {
		if(obj == null || obj.equals("init")) {
			ItogMark.init();
			PeriodType.init();
			EOSortOrdering.ComparisonSupport.setSupportForClass(
					new EduPeriod.ComparisonSupport(), Period.class);
			EOSortOrdering.ComparisonSupport.setSupportForClass(
					new EduPeriod.ComparisonSupport(), EduPeriod.class);
		} else if("init2".equals(obj)) {
			return init2(ctx);
		} else if("notesAddOns".equals(obj)) {
			return notesAddOns(ctx);
		} else if("studentReporter".equals(obj)) {
			return studentReporter(ctx);
		} else if("periods".equals(obj)) {
			return periods(ctx);
		} else if("lessonTabs".equals(obj)) {
			return lessonTabs(ctx);
		}
		return null;
	}
	
	public static Object init2(WOContext ctx) {
		EOEditingContext ec = new EOEditingContext();
		Integer year = net.rujel.base.MyUtility.eduYearForDate(new NSTimestamp());
		NSArray starterPeriods = EOUtilities.objectsWithQualifierFormat(ec,"EduPeriod","num = 1 AND eduYear = %@",new NSArray(year));
		
		//NSArray periodTypes = PeriodType.allPeriodTypes(ec);
		Enumeration enu = starterPeriods.objectEnumerator();
		//NSTimestamp today = new NSTimestamp();
		while (enu.hasMoreElements()) {
			//PeriodType perType = (PeriodType)enu.nextElement();
			EduPeriod period = (EduPeriod)enu.nextElement();//perType.currentPeriod(today);
			if(period == null) continue;
			Scheduler.sharedInstance().registerPeriod(period);
		}
		return null;
	}		
	
	public static NamedFlags moduleAccess(WOContext ctx,Object key) {
		UserPresentation user = (UserPresentation)ctx.session().valueForKey("user");
		NamedFlags access = null;
		if(user != null) {
			try {
				int lvl = user.accessLevel(key);
				access = new ImmutableNamedFlags(lvl,UseAccess.accessKeys);
			}  catch (AccessHandler.UnlistedModuleException e) {
				access = DegenerateFlags.ALL_TRUE;
			}
		}
		if(access == null)
			access = DegenerateFlags.ALL_TRUE;
		return access;
	}
	
	public static NSMutableDictionary notesAddOns(WOContext ctx) {
		NSMutableDictionary itogAddOn = addOn.mutableClone();
		
		NamedFlags access = moduleAccess(ctx,"ItogMark");//itogAddOn.valueForKey("id"));
			/*null;
		UserPresentation user = (UserPresentation)ctx.session().valueForKey("user");
		if(user != null) {
			try {
				int lvl = user.accessLevel(itogAddOn.valueForKey("id"));
				access = new ImmutableNamedFlags(lvl,UseAccess.accessKeys);
			}  catch (AccessHandler.UnlistedModuleException e) {
				access = DegenerateFlags.ALL_TRUE;
			}
		}
		if(access == null)
			access = DegenerateFlags.ALL_TRUE;*/
		if(access.getFlag(0)) {
			itogAddOn.takeValueForKey(access,"access");
			return itogAddOn;
		}
		return null;
	}
	/* else if(ctx == null && "presentTabs".equals(obj)) {
		return tab;
	} else if(ctx != null && "presentTabs".equals(obj)) {
		return new ItogsTab((EduCourse)ctx.page().valueForKey("course"),tab);
	}*/
	
	public static NSDictionary studentReporter(WOContext ctx) {
		NamedFlags access = moduleAccess(ctx,"ItogMark");
		if(!access.getFlag(0))
				return null;
		NSMutableDictionary result = studentReporter.mutableClone();
		result.takeValueForKey(access,"access");
		return result;
	}
	
	public static NSArray periods(WOContext ctx) {
		Object eduYear = ctx.session().valueForKey("eduYear");
		NSArray result = EOUtilities.objectsMatchingKeyAndValue(ctx.session().defaultEditingContext()
				,"EduPeriod","eduYear",eduYear);
		return result;
		//return EOSortOrdering.sortedArrayUsingKeyOrderArray(result,EduPeriod.sorter);
	}

	protected static class PeriodTab implements Tabs.GenericTab {
		protected String title;
		protected String hover;
		protected EOQualifier qual;
		protected boolean current;
		protected int code;
		
		public PeriodTab(EduPeriod period, boolean isCurrent) {
			title = period.title();
			code = period.code();
			current = isCurrent;
			NSMutableArray quals = new NSMutableArray();
			quals.addObject(new EOKeyValueQualifier
					("date",EOQualifier.QualifierOperatorGreaterThanOrEqualTo,period.begin()));
			quals.addObject(new EOKeyValueQualifier
					("date",EOQualifier.QualifierOperatorLessThanOrEqualTo,period.end()));
			qual = new EOAndQualifier(quals);
			hover = period.name();
		}
		public boolean defaultCurrent() {
			return current;
		}

		public String title() {
			return title;
		}
		public String hover() {
			return hover;
		}		
		public EOQualifier qualifier() {
			return qual;
		}
		
		public boolean equals(Object obj) {
			if (obj instanceof PeriodTab) {
				PeriodTab aTab = (PeriodTab) obj;
				return (this.code == aTab.code);
			}
			return false;
		}

		public int hashCode() {
			return code;
		}

	}

	public static NSArray lessonTabs(WOContext ctx) {
		EduCourse course = (EduCourse)ctx.session().objectForKey("courseForlessons");
		NSTimestamp currDate = (NSTimestamp)ctx.session().objectForKey("recentDate");
		if(currDate == null) {
		EduLesson currLesson = (EduLesson)ctx.session().objectForKey("selectedLesson");
		currDate = (currLesson != null)?currLesson.date():
			(NSTimestamp)ctx.session().valueForKey("today");
		}
		NSArray periods = EduPeriod.periodsForCourse(course);
		if(periods == null || periods.count() == 0)
			return null;
		Enumeration enu = periods.objectEnumerator();
		NSMutableArray result = new NSMutableArray();
		while (enu.hasMoreElements()) {
			EduPeriod per = (EduPeriod) enu.nextElement();
			boolean isCurrent = per.contains(currDate);
			result.addObject(new PeriodTab(per,isCurrent));
		}
		return new NSArray((Object)result);
	}
}
