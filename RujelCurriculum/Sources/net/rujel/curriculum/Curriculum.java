// Curriculum.java: Class file for WO Component 'Curriculum'

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

import java.io.InputStream;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.logging.Logger;

import net.rujel.base.MyUtility;
import net.rujel.base.ReadAccess;
import net.rujel.base.SchoolSection;
import net.rujel.interfaces.EduCourse;
import net.rujel.interfaces.EduGroup;
import net.rujel.reports.ReportTable;
import net.rujel.reusables.*;
import net.rujel.ui.TeacherSelector;

import com.webobjects.appserver.*;
import com.webobjects.eoaccess.EOUtilities;
import com.webobjects.eocontrol.*;
import com.webobjects.foundation.*;

import net.rujel.interfaces.Teacher;

public class Curriculum extends com.webobjects.appserver.WOComponent {
	
	public static Logger logger = Logger.getLogger("rujel.curriculum"); 
	public boolean ifArchive = SettingsReader.boolForKeyPath("markarchive.Reason", 
			SettingsReader.boolForKeyPath("markarchive.archiveAll", false));
	
	public EOEditingContext ec;
	public NSKeyValueCodingAdditions currTab;
	public NSDictionary plist;
	public NSKeyValueCodingAdditions itemDict;
	public NSMutableDictionary params;
	public NSArray list;
	public Object itemRow;
	public Reason currReason;
	public Object currObject;
	public Object highlight;
	public NSKeyValueCodingAdditions valueOf;
	public Object item;
	public NSArray grades;
	public NSArray sections;
	public SchoolSection activeSection;
	public ReadAccess readAccess;
	
	public NSMutableDictionary tmpDict = new NSMutableDictionary();
	
	public WOActionResults invokeAction(WORequest aRequest, WOContext aContext) {
		item = null;
		itemDict = null;
		itemRow = null;
		return super.invokeAction(aRequest, aContext);
	}
	
    public Curriculum(WOContext context) {
        super(context);
        try {
			InputStream pstream = (InputStream)session().valueForKeyPath(
        			"strings.@RujelCurriculum_Overview.plist");
			NSData pdata = new NSData(pstream, pstream.available());
			plist = (NSDictionary)NSPropertyListSerialization.propertyListFromData(
								pdata, "utf8");
		} catch (Exception e) {
			throw new NSForwardException(e,"Error reading Overview.pist");
		}
		ec = new SessionedEditingContext(session());
		
		params = (NSMutableDictionary)session().objectForKey("curriculumParams");
		if(params == null) {
			params = new NSMutableDictionary();
			NSTimestamp day = (NSTimestamp)session().valueForKey("today");
			Calendar cal = Calendar.getInstance();
			cal.setTime(day);
			cal.set(Calendar.DAY_OF_MONTH, 1);
			day = new NSTimestamp(cal.getTimeInMillis());
			params.takeValueForKey(day,"since");
			cal.add(Calendar.MONTH,1);
			cal.add(Calendar.DATE,-1);
			day = new NSTimestamp(cal.getTimeInMillis());
			params.takeValueForKey(day, "to");
			session().setObjectForKey(params, "curriculumParams");
		}
		NSArray tabs = (NSArray)plist.valueForKey("tabs");
		currTab = (NSKeyValueCodingAdditions)tabs.objectAtIndex(0);
		//tmpDict = new NSMutableDictionary(tabs,(NSArray)tabs.valueForKey("entity"));
		Enumeration enu = tabs.objectEnumerator();
		while (enu.hasMoreElements()) {
			NSKeyValueCodingAdditions tab = (NSKeyValueCodingAdditions) enu.nextElement();
			String key = (String)tab.valueForKey("reasonKey");
			if(key != null)
				tmpDict.takeValueForKey(tab, key);
		}
		readAccess = new ReadAccess(context.session());
		readAccess.relObject = this;
		readAccess.sectionPath = "activeSection";
		if(Various.boolForObject(session().valueForKeyPath("sections.hasSections"))) {
			activeSection = (SchoolSection)context.session().valueForKeyPath("state.section");
			activeSection = (SchoolSection)EOUtilities.localInstanceOfObject(ec, activeSection);
			sections = SchoolSection.sectionsForUser(context.session(), "Curriculum", ec, true);
//			if(sections.count() < 2)
//				sections = null;
		}
		valueOf = new DisplayAny.ValueReader(this);
		setActiveSection(activeSection);
    }
    
    public void setActiveSection(Object section) {
    	if(section instanceof SchoolSection)
    		activeSection = (SchoolSection)section;
    	else
    		activeSection = null;
    	ec.setUserInfoForKey(activeSection, "activeSection");
    	
		int maxGrade = (activeSection == null)?SettingsReader.intForKeyPath("edu.maxGrade", 11):
			activeSection.maxGrade().intValue();
		int minGrade = (activeSection == null)?SettingsReader.intForKeyPath("edu.minGrade", 1):
			activeSection.minGrade().intValue();
		Integer[] grds = new Integer[maxGrade - minGrade + 1];
		for (int i = 0; i < grds.length; i++) {
			grds[i] = new Integer(minGrade + i);
		}
		grades = new NSArray(grds);
		if(tmpDict.valueForKey("inReason") == null)
			search();
    }
    
    public void search() {
    	NSArray args = (NSArray)currTab.valueForKeyPath("qualifier.args");
    	if(args != null && args.count() > 0) {
    		Enumeration enu = args.objectEnumerator();
    		args = new NSMutableArray();
    		while (enu.hasMoreElements()) {
    			String arg = (String) enu.nextElement();
    			Object param = params.valueForKey(arg);
    			if(param == null)
    				param = NullValue;
    			((NSMutableArray)args).addObject(param);
    		}
    	}
    	String qualifierFormat = (String)currTab.valueForKeyPath("qualifier.formatString");
    	EOQualifier qual = EOQualifier.qualifierWithQualifierFormat(qualifierFormat, args);
    	String entityName = (String)currTab.valueForKey("entity");
    	if(activeSection != null) { // add section qualifier
    		EOQualifier[] quals = new EOQualifier[2];
    		String key = (entityName.equals(Reason.ENTITY_NAME))?
    				Reason.SECTION_KEY:"reason.section";
    		quals[0] = new EOKeyValueQualifier(key,EOQualifier.QualifierOperatorEqual, activeSection);
    		quals[1] = new EOKeyValueQualifier(key,EOQualifier.QualifierOperatorEqual, NullValue);
			quals[0] = new EOOrQualifier(new NSArray(quals));
    		quals[1] = qual;
    		qual = new EOAndQualifier(new NSArray(quals));
    	}
    	EOFetchSpecification fs = new EOFetchSpecification(entityName,qual,null);
    	fs.setRefreshesRefetchedObjects(true);
    	args = (NSArray)currTab.valueForKey("prefetch");
    	if(args != null && args.count() > 0)
    		fs.setPrefetchingRelationshipKeyPaths(args);
    	list = ec.objectsWithFetchSpecification(fs);
    	if(activeSection != null && list.count() > 0 && !entityName.equals(Reason.ENTITY_NAME)) {
    		// filter by section
			NSMutableArray newList = new NSMutableArray(list.count());
			Enumeration enu = list.objectEnumerator();
/*    		if(entityName.equals(Reason.ENTITY_NAME)) {
    			while (enu.hasMoreElements()) {
					Reason reas = (Reason) enu.nextElement();
					if(reas.section() == activeSection) {
						newList.addObject(reas);
						continue;
					}
					if(filterEvents(reas.substitutes()) || filterEvents(reas.variations()))
						newList.addObject(reas);
				}
    		} else {*/
    			while (enu.hasMoreElements()) {
					Reason.Event event = (Reason.Event) enu.nextElement();
					if(event.reason().section() == activeSection ||
							event.course().valueForKey("section") == activeSection)
						newList.addObject(event);
				}
//    		}
			list = newList;
    	}
    	if(list != null && list.count() > 1)
    		sort();
    }
    /*
    private boolean filterEvents(NSArray events) {
    	if(events == null || events.count() == 0)
    		return false;
		Enumeration enu = events.objectEnumerator();
		while (enu.hasMoreElements()) {
			Reason.Event event = (Reason.Event) enu.nextElement();
			if(event.course().valueForKey("section") == activeSection)
				return true;
		}
		return false;
    }*/
    
    public void sort() {
    	if(list == null || list.count() == 0)
    		return;
    	NSArray properties = (NSArray)currTab.valueForKey("properties");
    	if(properties == null || properties.count() == 0)
    		return;
    	NSMutableArray sorter = new NSMutableArray();
    	Enumeration enu = properties.objectEnumerator();
    	while (enu.hasMoreElements()) {
			NSKeyValueCoding prop = (NSKeyValueCoding) enu.nextElement();
			String key = (String)prop.valueForKey("keyPath");
			if(key == null)
				continue;
			NSSelector selector = EOSortOrdering.CompareAscending;
			EOSortOrdering so = EOSortOrdering.sortOrderingWithKey(key, selector);
			sorter.addObject(so);
		}
    	list = EOSortOrdering.sortedArrayUsingKeyOrderArray(list, sorter);
    }
    
    protected void revert() {
    	if(ec.hasChanges()) {
    		ec.revert();
    		if(currReason != null && currReason.editingContext() == null)
    			currReason = null;
    	}
    }
    
    public void setCurrTab(NSKeyValueCodingAdditions tab) {
		currObject = null;
		revert();
     	currTab = tab;
    	if(currTab == null)
    		return;
    	cancelMove();
    	tmpDict.removeObjectForKey("selectedObjects");
    	if(Reason.ENTITY_NAME.equals(currTab.valueForKey("entity"))) {
    		currObject = currReason;
    		tmpDict.takeValueForKey(Boolean.FALSE,"allowSelection");
    	} else {
    		tmpDict.takeValueForKey(readAccess.valueForKeyPath(
    				"create.Curriculum"),"allowSelection");
    	}
    	if(tmpDict.valueForKey("inReason") != null) {
			String reasonKey = (String)tab.valueForKey("reasonKey");
    		if(reasonKey == null) {
    			tmpDict.takeValueForKey(null, "inReason");
    			search();
    		} else {
				list = (NSArray)currReason.valueForKey(reasonKey);
				sort();
			}
    	} else {
//    		if(list != null)
    			search();
    	}
    }
    
/*    public void setItemDict(NSKeyValueCodingAdditions newDict) {
    	itemDict = newDict;
    	if(itemRow == null || itemDict == null) {
    		item = null;
    	} else {
    		String keyPath = (String)itemDict.valueForKeyPath("keyPath");
    		if(keyPath == null || keyPath.equals("."))
    			item = itemRow;
    		else
    			item = NSKeyValueCodingAdditions.Utility.valueForKeyPath(itemRow, keyPath);
    	}
    }*/
       
    public String rowClass() {
    	if(currReason == itemRow)
    			return "selection";
    	if(itemRow instanceof Reason)
    		return ((Reason)itemRow).styleClass();
    	if(itemRow instanceof Reason.Event)
    		return ((Reason.Event)itemRow).reason().styleClass();
    	return (String)valueOf.valueForKeyPath("itemRow.currTab.rowClass");
    }
    
    public String cellClass() {
     	if(item != null && item == currReason)
    		return "selection";
    	if((item==null)?(highlight==null):item.equals(highlight))
    		return "selectionBorder";
    	if(highlight instanceof EduCourse) {
    		String coursePath = (String)itemDict.valueForKey("course");
    		if(coursePath!=null && 
    				highlight == NSKeyValueCodingAdditions.Utility.valueForKeyPath(itemRow, coursePath))
    			return "selectionBorder";
    	}
    	if(highlight instanceof Integer && item instanceof EduGroup && 
    			highlight.equals(((EduGroup)item).grade()))
    		return "selectionBorder";
    	if((itemDict.valueForKey("popup") != null))
    		return "canPop";
    	return "pad";
    }
    
    public WOActionResults select() {
    	revert();
    	currObject = itemRow;
    	if(itemDict.valueForKey("popup") != null) {
    		highlight = itemRow;
    		return popup();
    	}
    	highlight = item;
    	if(itemRow instanceof Reason)
    		currReason = (Reason)itemRow;
    	else
    		currReason = (Reason)NSKeyValueCoding.Utility.valueForKey(itemRow, "reason");
    	
    	if(list != null && list.count() > 0 && manipulateSelection()) {
    		Enumeration enu = list.objectEnumerator();
    		NSMutableArray set = (NSMutableArray)tmpDict.valueForKey("selectedObjects");
    		if(set == null) {
    			set = new NSMutableArray();
    			tmpDict.takeValueForKey(set, "selectedObjects");
    		} else {
    			set.removeAllObjects();
    		}
//    		String keyPath = (String)itemDict.valueForKeyPath("keyPath");
			Object compare = null;
    		while (enu.hasMoreElements()) {
				Object row = enu.nextElement();
				/*
	    		if(keyPath == null || keyPath.equals("."))
	    			compare = row;
	    		else
	    			compare = NSKeyValueCodingAdditions.Utility.valueForKeyPath(row, keyPath);
	    		*/
				compare = ReportTable.valueFromDict(itemDict, row, this);
	    		if((item==null)?compare==null:item.equals(compare))
	    			set.addObject(row);
			}
    	}
    	if(ifArchive) {
    		WOComponent archivePopup = archivePopup();
     		tmpDict.takeValueForKey(archivePopup.valueForKeyPath("archives.count")
    				, "archivesCount");
    	}
    	return this;
    }
    
    public WOComponent archivePopup() {
    	WOComponent archivePopup = (WOComponent)tmpDict.valueForKey("archivePopup");
    	if(archivePopup == null) {
    		archivePopup = pageWithName("ArchivePopup");
    		archivePopup.takeValueForKey("ReasonArchivePresenter", "presenter");
    		archivePopup.takeValueForKey(this, "returnPage");
    		archivePopup.takeValueForKey(Boolean.TRUE, "noEdit");
    		tmpDict.takeValueForKey(archivePopup, "archivePopup");
    	} else {
    		archivePopup.ensureAwakeInContext(context());
    	}
		archivePopup.takeValueForKey(currReason, "object");
    	return archivePopup;
    }

/*    public String rowID() {
    	if(currObject == itemRow)
    		return "curr";
    	return null;
    }*/

    public String onClick() {
    	if(itemDict.valueForKey("popup") != null) {
    		StringBuilder result = new StringBuilder("highlight(this);");
    		result.append(session().valueForKey("ajaxPopup"));
    		return result.toString();
    	}
    	return (String)session().valueForKey("checkRun");
    }
    
    protected WOActionResults popup() {
    	String name = (String)valueOf.valueForKeyPath("item.itemDict.popup");
		WOComponent nextPage = pageWithName(name);
		nextPage.takeValueForKey(this, "returnPage");
		NSDictionary popupParams = (NSDictionary)itemDict.valueForKey("popupParams");
		if(popupParams == null || popupParams.count() == 0)
			return nextPage;
		Enumeration enu = popupParams.keyEnumerator();
		while (enu.hasMoreElements()) {
			String key = (String) enu.nextElement();
			Object value = valueOf.valueForKeyPath("item.itemDict.popupParams." + key);
			nextPage.takeValueForKey(value, key);
		}
		return nextPage;
	}
    
    public void save() {
    	currObject = null;
		ec.lock();
		try {
    		if(ifArchive && ec.hasChanges()) {
    			EOEnterpriseObject archive = EOUtilities.createAndInsertInstance(ec,"MarkArchive");
    			archive.takeValueForKey(currReason, "objectIdentifier");
    			archive.takeValueForKey(currReason.reason(),"@reason");
    			archive.takeValueForKey(MyUtility.dateFormat().format(currReason.begin()),"@begin");
    			if(currReason.end() != null)
    				archive.takeValueForKey(MyUtility.dateFormat().format(currReason.end()),"@end");
    			if(currReason.verification() != null)
    				archive.takeValueForKey(currReason.verification(),"@verification");
    			if(!currReason.noLimits())
    				archive.takeValueForKey(currReason.extToString(),"@relation");
/*    			if(currReason.namedFlags().flagForKey("forTeacher"))
    				archive.takeValueForKey(Person.Utility.fullName(
    						currReason.teacher(), true, 2, 1, 1),"@teacher");
    			if(currReason.namedFlags().flagForKey("forEduGroup"))
    				archive.takeValueForKey(currReason.eduGroup().name(),"@eduGroup");*/
    			archive.takeValueForKey(new Integer(
    					(ec.globalIDForObject(currReason).isTemporary())?1:2), "actionType");
    		}
			boolean add = (Reason.ENTITY_NAME.equals(currTab.valueForKey("entity")) &&
					ec.globalIDForObject(currReason).isTemporary());
			ec.saveChanges();
			if(add)
				list = list.arrayByAddingObject(currReason);
			sort();
			Object[] args = new Object[] {session(),currReason};
			logger.log(WOLogLevel.EDITING,"Reason is saved",args);
		} catch (Exception ex) {
			if(!ec.globalIDForObject(currReason).isTemporary())
				ec.revert();
			session().takeValueForKey(ex.getMessage(), "message");
			Object[] args = new Object[] {session(),currReason,ex};
			logger.log(WOLogLevel.FINE,"Failed to save reason",args);
		} finally {
			if(currReason != null)
				currReason.setNamedFlags(null);
			ec.unlock();
		}
    	if(ifArchive) {
    		WOComponent archivePopup = archivePopup();
     		tmpDict.takeValueForKey(archivePopup.valueForKeyPath("archives.count")
    				, "archivesCount");
    	}
    }

	public Teacher reasonTeacher() {
		if(currReason != null && currReason.teacher() != null)
			return currReason.teacher();
		if(highlight instanceof Teacher)
			return (Teacher)highlight;
		if(highlight instanceof EduCourse)
			return ((EduCourse)highlight).teacher();
		if(highlight instanceof Reason.Props)
			return ((Reason.Props)highlight).teacher();
		return null;
	}
	public EduGroup reasonGroup() {
		if(currReason != null && currReason.eduGroup() != null)
			return currReason.eduGroup();
		if(highlight instanceof EduGroup)
			return (EduGroup)highlight;
		if(highlight instanceof EduCourse)
			return ((EduCourse)highlight).eduGroup();
		if(highlight instanceof Reason.Props)
			return ((Reason.Props)highlight).eduGroup;
		return null;
	}
	public Integer reasonGrade() {
		if(highlight instanceof Integer)
			return (Integer)highlight;
		if(currReason != null){
			if(currReason.grade() != null)
				return currReason.grade();
			if(currReason.eduGroup() != null)
				return currReason.eduGroup().grade();
		}
		if(highlight instanceof EduGroup)
			return ((EduGroup)highlight).grade();
		if(highlight instanceof EduCourse)
			return ((EduCourse)highlight).cycle().grade();
		if(highlight instanceof Reason.Props)
			return ((Reason.Props)highlight).grade;
		return null;
	}
	
	public Boolean cantEdit() {
		if (tmpDict.valueForKey("reasonsToMoveIn") != null)
			return Boolean.TRUE;
		return (Boolean)readAccess.valueForKeyPath("_edit.currReason");
	}
	public Boolean noEduGroup() {
		if (reasonGroup() == null)
			return Boolean.TRUE;
		return cantEdit();
	}
	
	public WOActionResults selectTeacher() {
		return TeacherSelector.selectorPopup(this, "reasonTeacher", ec);
	}
	
	public WOActionResults selectEduGroup() {
		WOComponent selector = pageWithName("SelectorPopup");
		selector.takeValueForKey(this, "returnPage");
		selector.takeValueForKey("reasonGroup", "resultPath");
		selector.takeValueForKey(reasonGroup(), "value");
		NSMutableDictionary dict = (NSMutableDictionary)plist.valueForKey("selectEduGroup");
		dict.takeValueForKeyPath(ec, "presenterBindings.editingContext");
		selector.takeValueForKey(dict, "dict");
		return selector;
	}
	
	public void setReasonTeacher(Object newTeacher) {
		currObject = null;
		currReason.namedFlags().setFlagForKey((newTeacher != null), "forTeacher");
		if(newTeacher != null) {
			currReason.namedFlags().setFlagForKey(false, "forEduGroup");
			currReason.setEduGroup(null);
			currReason.setGrade(null);
		}
		if(newTeacher instanceof Teacher) {
			highlight = EOUtilities.localInstanceOfObject(ec, (EOEnterpriseObject)newTeacher);
			currReason.setTeacher((Teacher)highlight);
			if(Reason.ENTITY_NAME.equals(currTab.valueForKey("entity")))
				highlight = currReason.extToString();
		} else {
			currReason.setTeacher(null);
			highlight = null;
		}
	}
	
	public void setReasonGroup(Object newGroup) {
		currObject = null;
		currReason.namedFlags().setFlagForKey((newGroup != null), "forEduGroup");
		if(newGroup != null) {
			currReason.namedFlags().setFlagForKey(false, "forTeacher");
			currReason.setTeacher(null);
			currReason.setGrade(null);
		}
		if(newGroup instanceof EduGroup) {
			highlight = EOUtilities.localInstanceOfObject(ec, (EOEnterpriseObject)newGroup);
			currReason.setEduGroup((EduGroup)highlight);
			if(Reason.ENTITY_NAME.equals(currTab.valueForKey("entity")))
				highlight = currReason.extToString();
		} else {
			currReason.setEduGroup(null);
			highlight = null;
		}
	}
	
	public void setReasonGrade(Integer newGrade) {
		currObject = null;
//		currReason.setGrade(newGrade);
		highlight = newGrade;
	}
	
	public void setReasonSection(SchoolSection section) {
		if(section == null)
			currReason.setSection(null);
		else
			currReason.setSection((SchoolSection)EOUtilities.localInstanceOfObject(ec, section));
	}
	
	public SchoolSection reasonSection() {
		if(currReason == null)
			return null;
		SchoolSection sect = currReason.section();
		if(sect == null)
			return null;
		return (SchoolSection)EOUtilities.localInstanceOfObject(
				session().defaultEditingContext(), sect);
	}

	public int relation() {
		if(currReason == null)
			return -1;
		if(currReason.namedFlags().flagForKey("forTeacher"))
			return 1;
		if(currReason.namedFlags().flagForKey("forEduGroup"))
			return 2;
		if(currReason.grade() != null)
			return 3;
		return 0;
	}

	public void setRelation(int relation) {
		currReason.setTeacher((relation == 1)?reasonTeacher():null);
		currReason.namedFlags().setFlagForKey((relation == 1), "forTeacher");
		currReason.setEduGroup((relation == 2)?reasonGroup():null);
		currReason.namedFlags().setFlagForKey((relation == 2), "forEduGroup");
		if(relation == 3) {
			String gradeString = context().request().stringFormValueForKey("gradePopup");
			try {
				int idx = Integer.parseInt(gradeString);
				Integer grade = (Integer)grades.objectAtIndex(idx);//new Integer(gradeString);
				if(!grade.equals(currReason.grade()))
					currReason.setGrade(grade);
			} catch (Exception e) {
				logger.log(WOLogLevel.INFO,"Can't decode grade for reason: " + gradeString,
						new Object[] {session(),currReason});
			}
		} else {
			currReason.setGrade(null);
		}
	}
	
	public void showSubstitutes() {
		currObject = null;
		revert();
		if(currReason == null)
			return;
		currTab = (NSKeyValueCodingAdditions) tmpDict.valueForKey(Reason.SUBSTITUTES_KEY);
		tmpDict.takeValueForKey(Boolean.TRUE, "inReason");
		list = currReason.substitutes();
		sort();
		if(Various.boolForObject(readAccess.valueForKeyPath("create.Curriculum"))) {
			tmpDict.takeValueForKey(Boolean.TRUE,"allowSelection");
			tmpDict.removeObjectForKey("selectedObjects");
			tmpDict.removeObjectForKey("reasonsToMoveIn");
		}
	}
	
	public void showVariations() {
		currObject = null;
		revert();
		if(currReason == null)
			return;
		currTab = (NSKeyValueCodingAdditions) tmpDict.valueForKey(Reason.VARIATIONS_KEY);
		tmpDict.takeValueForKey(Boolean.TRUE, "inReason");
		list = currReason.variations();
		sort();
		if(Various.boolForObject(readAccess.valueForKeyPath("create.Curriculum"))) {
			tmpDict.takeValueForKey(Boolean.TRUE,"allowSelection");
			tmpDict.removeObjectForKey("selectedObjects");
			tmpDict.removeObjectForKey("reasonsToMoveIn");
		}
	}
	
	public void createReason() {
		if(ec.hasChanges()) {
			ec.revert();
		}
		currReason = (Reason)EOUtilities.createAndInsertInstance(ec, Reason.ENTITY_NAME);
		currReason.takeValueForKey(session().valueForKey("today"), Reason.BEGIN_KEY);
		if(Various.boolForObject(currTab.valueForKey("external"))) {
			currReason.setFlags(new Integer(1));
			currReason.setReason((String)currTab.valueForKey("title"));
		} else {
			currReason.setReason((String)plist.valueForKey("newReason"));
		}
		currReason.setSection(activeSection);
		currObject = currReason;
		tmpDict.takeValueForKey(null, "archivesCount");
	}
	
	public boolean canDelete() {
		if(currReason == null)
			return false;
		if(Various.boolForObject(readAccess.valueForKeyPath("_delete.currReason")))
			return false;
		return (currReason.substitutes().count() + currReason.variations().count() == 0);
	}
	
	public void delete() {
		currObject = null;
		ec.lock();
		try {
			if (ec.hasChanges()) {
				ec.revert();
			}
			if(currReason.editingContext() != null) {
				Object[] args = new Object[] {session(),ec.globalIDForObject(currReason)};
	    		if(ifArchive) {
	    			EOEnterpriseObject archive = EOUtilities.createAndInsertInstance(ec,"MarkArchive");
	    			archive.takeValueForKey(currReason, "objectIdentifier");
	    			archive.takeValueForKey(".","@reason");
	    			archive.takeValueForKey(new Integer(3), "actionType");
	    		}
				ec.deleteObject(currReason);
				ec.saveChanges();
				logger.log(WOLogLevel.EDITING,"Reason deleted",args);
			}
		} catch (Exception e) {
			Object[] args = new Object[] {session(),currReason,e};
			logger.log(WOLogLevel.WARNING,"Error deleting Reason",args);
			session().takeValueForKey(e.getMessage(), "message");
		} finally {
			ec.unlock();
		}
		currReason = null;
		search();
	}
	
	public boolean ticked() {
		if(itemRow == null)
			return false;
		NSMutableArray set = (NSMutableArray)tmpDict.valueForKey("selectedObjects");
		if(set == null)
			return false;
		return set.containsObject(itemRow);
	}
	
	public void setTicked(boolean value) {
		NSMutableArray set = (NSMutableArray)tmpDict.valueForKey("selectedObjects");
		if(value) {
			if(set == null) {
				set = new NSMutableArray(itemRow);
				tmpDict.takeValueForKey(set, "selectedObjects");
			} else if(!set.containsObject(itemRow)) {
					set.addObject(itemRow);
			}
		} else {
			if(set != null)
				set.removeObject(itemRow);
		}
	}
	
	public void reasonsToMoveIn() {
		currObject = null;
		NSMutableArray set = (NSMutableArray)tmpDict.valueForKey("selectedObjects");
		if(set == null || set.count() == 0) {
			tmpDict.takeValueForKey(null, "reasonsToMoveIn");
			return;
		}
		Reason.Props props = Reason.propsFromEvents(set);
		if(props.section != null && !Various.boolForObject(session().valueForKeyPath(
				"sections.hasSections")))
			props.section = null;
		NSArray reasons = Reason.reasons(props);
		if(reasons == null)
			reasons = NSArray.EmptyArray;
		tmpDict.takeValueForKey(reasons, "reasonsToMoveIn");
		tmpDict.takeValueForKey(props, "reasonProps");
		int idx = reasons.indexOfIdenticalObject(currReason);
		tmpDict.takeValueForKey((idx >=0)?new Integer(idx):null, "selectedReason");
	}
	
	public void cancelMove() {
		tmpDict.removeObjectForKey("reasonsToMoveIn");
		tmpDict.removeObjectForKey("reasonProps");
		tmpDict.removeObjectForKey("selectedReason");
	}
	
	public boolean manipulateSelection() {
		Object tmp = tmpDict.valueForKey("allowSelection");
		if(!Various.boolForObject(tmp))
			return false;
		tmp = tmpDict.valueForKey("reasonProps");
		return (tmp == null);
	}
	
	public void moveToReason() {
		NSArray reasonsToMoveIn = (NSArray)tmpDict.removeObjectForKey("reasonsToMoveIn");
		NSMutableArray set = (NSMutableArray)tmpDict.valueForKey("selectedObjects");
		if(set == null || set.count() == 0) {
			return;
		}
		Number idx = (Number)tmpDict.removeObjectForKey("selectedReason");
		Reason.Props props = (Reason.Props)tmpDict.removeObjectForKey("reasonProps");
		boolean newReason = (idx == null || idx.intValue() < 0);
		Reason moveIn = null;
		if(newReason) {
			highlight = props;
			moveIn = props.newReason();
			moveIn.setReason((String)plist.valueForKey("newReason"));
		} else {
			moveIn = (Reason)reasonsToMoveIn.objectAtIndex(idx.intValue());
		}
		Enumeration enu = set.objectEnumerator();
		while (enu.hasMoreElements()) {
			Reason.Event event = (Reason.Event) enu.nextElement();
			event.addObjectToBothSidesOfRelationshipWithKey(moveIn, "reason");
		}
		if(newReason) {
			currReason = moveIn;
			tmpDict.takeValueForKey(null, "archivesCount");
		} else {
			newReason = ifArchive;
			ifArchive = false;
			save();
			ifArchive = newReason;
		}
		set.removeAllObjects();
	}
	
	public Object canCreate() {
		if(!Reason.ENTITY_NAME.equals(currTab.valueForKey("entity")))
			return Boolean.FALSE;
		if(tmpDict.valueForKey("reasonsToMoveIn") != null)
			return Boolean.FALSE;
		return readAccess.valueForKeyPath("create.Reason");
	}
	
	public WOActionResults export() {
		WOComponent exportPage = pageWithName("ReportTable");
		exportPage.takeValueForKey(list, "list");
		exportPage.takeValueForKey(currTab.valueForKey("properties"), "properties");
    	String entityName = (String)currTab.valueForKey("entity");
		exportPage.takeValueForKey('\'' + entityName + "'yyMMdd", "filenameFormatter");
		return exportPage;
	}

	public String title() {
		return (String)plist.valueForKey("title");
	}
	
	public String variationEditor() {
		if (itemRow instanceof Variation) {
			Integer value = (Integer)NSKeyValueCoding.Utility.valueForKey(itemRow, "value");
			if(value.intValue() > 0)
				return "EditVarSub";
			else
				return "EditVariation";
		}
		return null;
	}
}