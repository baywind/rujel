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

package net.rujel.criterial;

import java.math.BigDecimal;
import java.util.Enumeration;
import java.util.logging.Logger;

import net.rujel.base.BaseLesson;
import net.rujel.interfaces.EOInitialiser;
import net.rujel.interfaces.EduLesson;
import net.rujel.reusables.PlistReader;
import net.rujel.reusables.WOLogLevel;

import com.webobjects.eoaccess.EOJoin;
import com.webobjects.eoaccess.EORelationship;
import com.webobjects.eocontrol.*;
import com.webobjects.foundation.*;
import com.webobjects.appserver.WOApplication;
import com.webobjects.appserver.WOContext;

public class ModuleInit {
	protected static NSArray tabs = (NSArray)WOApplication.application().valueForKeyPath("extStrings.RujelCriterial_Strings.tabs");
	protected static NSDictionary worksTab = (NSDictionary)WOApplication.application().valueForKeyPath("extStrings.RujelCriterial_Strings.worksTab");
	protected static NSDictionary reportSettings = (NSDictionary)WOApplication.application().valueForKeyPath("extStrings.RujelCriterial_Strings.reportSettings");

	public static Object init(Object obj) {
		if("presentTabs".equals(obj)) {
			return worksTab;
		}
		return null;
	}
	
	public static Object init(Object obj, WOContext ctx) {
		if(obj == null || obj.equals("init")) {
			init();
//		} else if("init2".equals(obj)) {
			Work.initTypes();
		} else if("presentTabs".equals(obj)) {
			return worksTab.mutableClone();
		} else if ("extendLesson".equals(obj)) {
			return extendLesson(ctx);
		} else if("reportForStudent".equals(obj)) {
			NSDictionary settings = (NSDictionary)ctx.session().objectForKey("reportForStudent");
			return StudentMarks.reportForStudent(settings);
		} else if("reportSettingsForStudent".equals(obj)) {
			return PlistReader.cloneDictionary(reportSettings, true);
		} else if ("lessonProperies".equals(obj)) {
			return lessonProperies(ctx);
		}
		return null;
	}
	
	public static void init() {
		EOInitialiser.initialiseRelationship("WorkNote","student",false,"studentID","Student").
						anyInverseRelationship().setPropagatesPrimaryKey(true);

		EORelationship relationship = EOInitialiser.initialiseRelationship
									("CycleCritSet","cycle",false,"cycleID","EduCycle");
		/*
		EOJoin join = (EOJoin)relationship.joins().objectAtIndex(0);
		join = new EOJoin(join.destinationAttribute(),join.sourceAttribute());
		EORelationship backrel = new EORelationship();
		backrel.setName("cycleCritSets");
		relationship.destinationEntity().addRelationship(backrel);
		backrel.addJoin(join);
		backrel.setToMany(true);
		backrel.setJoinSemantic(EORelationship.InnerJoin);
		backrel.setPropagatesPrimaryKey(true);
		backrel.setOwnsDestination(true);
		backrel.setDeleteRule(EOClassDescription.DeleteRuleCascade);

		relationship = EOInitialiser.initialiseRelationship("Work","taskText",false,"workID","TaskText");
		relationship.setOwnsDestination(true);
		relationship.setPropagatesPrimaryKey(true);
		relationship.setDeleteRule(EOClassDescription.DeleteRuleCascade);
		relationship.setIsMandatory(false);
		*/
		relationship = EOInitialiser.initialiseRelationship("Work","course",false,"courseID","EduCourse");
		if(EduLesson.entityName.equals("Work")) {
			EORelationship backrel = relationship.destinationEntity().relationshipNamed("lessons");
			EOJoin join = (EOJoin)backrel.joins().objectAtIndex(0);
			backrel.removeJoin(join);
			join = (EOJoin)relationship.joins().objectAtIndex(0);
			join = new EOJoin(join.destinationAttribute(),join.sourceAttribute());
			backrel.addJoin(join);
		} else if (EduLesson.entityName.equals("BaseLesson")) {
			BaseLesson.setTaskDelegate(new HomeWorkDelegate());
		}

		Mark.init();
	}
	
	public static NSKeyValueCoding extendLesson(WOContext ctx) {
		EduLesson lesson = (EduLesson)ctx.session().objectForKey("currentLesson");
		EOEditingContext ec = lesson.editingContext();
		EOSortOrdering so = new EOSortOrdering("type",EOSortOrdering.CompareAscending);
		NSMutableArray sorter = new NSMutableArray(so);
		so = new EOSortOrdering("announce",EOSortOrdering.CompareDescending);
		sorter.addObject(so);
		so = new EOSortOrdering("date",EOSortOrdering.CompareAscending);
		sorter.addObject(so);
		EOQualifier qual = new EOKeyValueQualifier("course",EOQualifier.QualifierOperatorEqual,lesson.course());
		NSMutableArray quals = new NSMutableArray(qual);
		NSTimestamp date = lesson.date();
		qual = new EOKeyValueQualifier("date",EOQualifier.QualifierOperatorGreaterThanOrEqualTo,date);
		quals.addObject(qual);
		qual = new EOKeyValueQualifier("announce",EOQualifier.QualifierOperatorLessThanOrEqualTo,date);
		quals.addObject(qual);
		qual = new EOAndQualifier(quals);
		EOFetchSpecification fs = new EOFetchSpecification("Work",qual,sorter);
		NSArray related = ec.objectsWithFetchSpecification(fs);
		/*if(related == null || related.count() == 0)
			return null;*/
		NSMutableDictionary result = new NSMutableDictionary("05", "sort");
		result.takeValueForKey(related,"works");
		result.takeValueForKey("WorksOnDate", "component");
		return result;
	}
	
	public static NSDictionary lessonProperies(WOContext ctx) {
		NSArray lessonsList = (NSArray)ctx.session().objectForKey("lessonsList");
		if(lessonsList == null || lessonsList.count() == 0)
			return null;
		NSMutableDictionary result = new NSMutableDictionary();
		EduLesson lesson = (EduLesson)lessonsList.objectAtIndex(0);
		EOEditingContext ec = lesson.editingContext();
		if(ec == null) {
			Logger.getLogger("rujel.criterial").log(WOLogLevel.WARNING,
					"Lesson not in EditingContext",ctx.session());
			return null;
		}
		EOQualifier qual = new EOKeyValueQualifier(
				"course",EOQualifier.QualifierOperatorEqual,lesson.course());
		NSMutableArray quals = new NSMutableArray(NSKeyValueCoding.NullValue);
		quals.addObject(qual);
		qual = new EOKeyValueQualifier(
				"weight",EOQualifier.QualifierOperatorGreaterThan,BigDecimal.ZERO);
		quals.addObject(qual);
		EOFetchSpecification fs = new EOFetchSpecification("Work",qual,null);
		NSDictionary props = new NSDictionary("font-weight:bold;","style");
		Enumeration enu = lessonsList.objectEnumerator();
		while (enu.hasMoreElements()) {
			lesson = (EduLesson) enu.nextElement();
			qual = new EOKeyValueQualifier("date",EOQualifier.QualifierOperatorEqual,lesson.date());
			quals.replaceObjectAtIndex(qual, 0);
			qual = new EOAndQualifier(quals);
			fs.setQualifier(qual);
			NSArray w = ec.objectsWithFetchSpecification(fs);
			if(w != null && w.count() > 0)
				result.setObjectForKey(props, lesson);
		}
		if(result.count() == 0)
			return null;
		return result;
	}

}

