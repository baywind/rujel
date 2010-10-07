// SubgroupEditor.java: Class file for WO Component 'SubgroupEditor'

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

package net.rujel.ui;

import net.rujel.interfaces.*;
import net.rujel.base.*;

import com.webobjects.foundation.*;
import com.webobjects.appserver.*;
import com.webobjects.eocontrol.*;

import java.util.Enumeration;
import java.util.logging.Logger;

import net.rujel.reusables.Export;
import net.rujel.reusables.NamedFlags;
import net.rujel.reusables.WOLogLevel;

public class SubgroupEditor extends WOComponent {
	protected static Logger logger = Logger.getLogger("rujel.journal");
    public BaseCourse course;
    public Student studentItem;
	private NSMutableSet subgroup;
	public boolean cantEdit;
	public EduGroup currGroup;
	public NSArray groups;
	public EduGroup groupItem;
	public NSMutableDictionary byGroup;
	
    public SubgroupEditor(WOContext context) {
        super(context);
    }
	
	public void setCourse(BaseCourse aCourse) {
		if (aCourse == null) {
			course = null;
			return;
		}
		course = aCourse;
		subgroup = new NSMutableSet(course.groupList());
		currGroup = course.eduGroup();
		if(course.namedFlags().flagForKey("mixedGroup")) {
			if(subgroup.count() > 0)
				currGroup = null;
			prepareByGroup();
		}			
		StringBuilder keyPath = new StringBuilder("readAccess._");
		if(course.audience() != null && course.audience().count() > 0)
			keyPath.append("edit");
		else
			keyPath.append("create");
		keyPath.append(".CourseAudience");
		cantEdit = (Boolean)session().valueForKeyPath(keyPath.toString());
	}
	
	protected void prepareByGroup() {
		byGroup = new NSMutableDictionary();
		groups = EduGroup.Lister.listGroups((NSTimestamp)session().valueForKey("today"),
				course.editingContext());
		groups = EOQualifier.filteredArrayWithQualifier(groups, new EOKeyValueQualifier(
				"grade",EOQualifier.QualifierOperatorEqual,course.cycle().grade()));
		NSArray audience = course.audience();
		if(audience == null || audience.count() == 0)
			return;
		NSMutableArray left = ((NSArray)audience.valueForKey("student")).mutableClone();
		Enumeration enu = groups.objectEnumerator();
		while (enu.hasMoreElements()) {
			EduGroup gr = (EduGroup) enu.nextElement();
			NSMutableArray list = new NSMutableArray();
			Enumeration grlist = gr.list().objectEnumerator();
			while (grlist.hasMoreElements()) {
				Student stu = (Student) grlist.nextElement();
				if(left.removeObject(stu))
					list.addObject(stu);
			}
			byGroup.setObjectForKey(list, gr);
		}
		NSMutableArray dangling = null;
		while(left.count() > 0) {
			Student student = (Student)left.objectAtIndex(0);
			EduGroup gr = student.recentMainEduGroup();
			if(gr == null) {
				if(dangling == null)
					dangling = new NSMutableArray();
				dangling.addObject(student);
			}
			if(left.count() == 1) {
				byGroup.setObjectForKey(new NSMutableArray(student), gr);
				left.removeAllObjects();
				break;
			}
			NSMutableArray list = new NSMutableArray();
			Enumeration grlist = gr.list().objectEnumerator();
			while (grlist.hasMoreElements()) {
				Student stu = (Student) grlist.nextElement();
				if(left.removeObject(stu))
					list.addObject(stu);
			}
			byGroup.setObjectForKey(list, gr);			
		}
		if(dangling != null) {
			NSDictionary dict = new NSDictionary("???","name");
			byGroup.setObjectForKey(dangling,dict);
		}
		if(byGroup.count() > groups.count())
			groups = EOSortOrdering.sortedArrayUsingKeyOrderArray(
					byGroup.allKeys(), EduGroup.Lister.sorter());
	}
	
	public NSArray studentsList() {
		if(groupItem == null) {
			if(groups == null)
				return currGroup.list();
			if(currGroup != null && !groups.contains(currGroup))
				return currGroup.list();
			return null;
		} else if(groupItem == currGroup) {
			return currGroup.list();
		} else {
			return (NSArray)byGroup.objectForKey(groupItem);
		}
	}
	
    public boolean isInSubgroup() {
		return subgroup.containsObject(studentItem);
    }
	
    public void setIsInSubgroup(boolean newIsInSubgroup) {
        if(newIsInSubgroup)
			subgroup.addObject(studentItem);
		else
			subgroup.removeObject(studentItem);
    }
	
    protected boolean autoUnmix() {
		int dfltFlags = SettingsBase.numericSettingForCourse("defaultCourseFlags",
				course, course.editingContext(), 0);
		if(dfltFlags != 0) {
			NamedFlags flags = new NamedFlags(dfltFlags,BaseCourse.flagNames);
			if(flags.flagForKey("mixedGroup"))
				return false;
		}
		NSArray list = course.eduGroup().list();
		Enumeration enu = subgroup.objectEnumerator();
		while (enu.hasMoreElements()) {
			Student student = (Student) enu.nextElement();
			if(!list.containsObject(student))
				return false;
		}
		course.namedFlags().setFlagForKey(false, "mixedGroup");
		byGroup = null;
		groups = null;
		currGroup = course.eduGroup();
		return true;
    }
    
	public WOActionResults save() {
//		NSArray ls = course.groupList();
//		if(ls != null && subgroup.equals(new NSMutableSet(ls)) && !course.editingContext().hasChanges()) 
//			return (WOComponent)session().valueForKey("pullComponent");
		
		WOActionResults nextPage = null;
		EOEditingContext ec = course.editingContext();
//		if(ec.hasChanges()) {
			ec.lock();
			course.setSubgroup(subgroup.allObjects());
			WOLogLevel level = WOLogLevel.EDITING;
			try {
				ec.saveChanges();
//				if(course instanceof UseAccess && ((UseAccess)course).isOwned())
//					level = WOLogLevel.OWNED_EDITING;
				logger.logp(level,"SubgroupEditor","save","Subgroup changes saved",
						new Object[] {session(),course});
				session().takeValueForKey(Boolean.TRUE,"prolong");
				if(groups == null) {
					nextPage = (WOComponent)session().valueForKey("pullComponent");
				} else if(autoUnmix()) {
					ec.saveChanges();
				} else {
					NSMutableArray list = new NSMutableArray();
					Enumeration grlist = currGroup.list().objectEnumerator();
					while (grlist.hasMoreElements()) {
						Student stu = (Student) grlist.nextElement();
						if(subgroup.containsObject(stu))
							list.addObject(stu);
					}
					byGroup.setObjectForKey(list, currGroup);
					currGroup = null;
					if(byGroup.count() > groups.count())
						groups = EOSortOrdering.sortedArrayUsingKeyOrderArray(
								byGroup.allKeys(), EduGroup.Lister.sorter());
				}
			} catch (Exception ex) {
				logger.logp(level,"SubgroupEditor","save","Failed to save changes in subgroup",
						new Object[] {session(),course,ex});
				session().takeValueForKey(ex.toString(),"message");
			}
			ec.unlock();
			
//		}
		return nextPage;
	}
	
	public String studentStyle() {
		Boolean sex = (Boolean)valueForKeyPath("studentItem.person.sex");
		if(!isInSubgroup()) return "grey";
		if(sex == null) return "gerade"; 
		if (sex.booleanValue())
			return "male";
		else
			return "female";
	}
	
	
    public String onClick() {
		StringBuilder sb = new StringBuilder(50).append("modifyRowClass(this,'");
		Boolean sex = (Boolean)valueForKeyPath("studentItem.person.sex");
		if(sex != null) {
			if(isInSubgroup()) {
				if (sex.booleanValue())
					sb.append("male");
				else
					sb.append("female");
			} else {
				sb.append("found");
				if (sex.booleanValue())
					sb.append("Male");
				else
					sb.append("Female");
			}
		} else {
			if(!isInSubgroup())
				sb.append("un");
			sb.append("gerade");
		}
		sb.append("','grey");
		sb.append("');");
		return sb.toString();
    }
	
    public String title() {
        return (String)valueForKeyPath(
        		"application.strings.RujelInterfaces_Names.EduCourse.subgroup");
    }
    public Number total() {
        return subgroup.count();
    }
	
    public void setTotal(Number newTotal) {
        if(newTotal.intValue() != subgroup.count())
			logger.logp(WOLogLevel.INFO,"SubgroupEditor","setTotal",
					"Incorrect subgroup.count calculation",new Object[] {session(),course});
    }

	private int idx = 0;
	public int idx() {
		return idx + 1;
	}
	public void setIdx(Number nextIdx) {
		idx = (nextIdx == null)?0:nextIdx.intValue();
	}
	
	public void toggleMixed() {
		boolean mixed = (groups == null);
		course.namedFlags().setFlagForKey(mixed,"mixedGroup");
		if(mixed) {
			if(course.audience() == null || course.audience().count() == 0)
				course.setSubgroup(currGroup.list());
			prepareByGroup();
		} else {
			byGroup = null;
			groups = null;
			currGroup = course.eduGroup();
		}
	}
	
	public String toggleTitle() {
		String txt = (groups == null)?"mixed":"notMixed";
		txt = (String)session().valueForKeyPath("strings.Strings.SubgroupEditor." + txt);
		return txt;
	}
	
	public WOActionResults selectGroup() {
		currGroup = groupItem;
		return null;
	}

	public String cellID () {
		if(groupItem == null || currGroup == groupItem)
			return "currGroup";
		return null;
	}

	public String groupCellClass() {
		if(groupItem == null)
			return "selectionBorder grey";
		StringBuilder buf = new StringBuilder(25);
		if(currGroup == groupItem)
			buf.append("selectionBorder ");
		if(!groupItem.grade().equals(course.cycle().grade()))
			buf.append("un");
		buf.append("gerade");
		return buf.toString();
	}
	
	public String borderClass() {
		if(currGroup == groupItem)
			return "selection";
		else
			return "grey";
	}
	
	public String rowspan() {
		NSArray list = studentsList();
		if(list == null)
			return null;
		int count = list.count();
		if(count == 0)
			return null;
		return String.valueOf(count +2);
	}
	
	public boolean otherGroup() {
		if(groupItem instanceof EduGroup)
			return false;
		if(groupItem instanceof NSDictionary)
			return true;
		return (groups != null && currGroup != null && !groups.contains(currGroup));
	}
	
	public WOActionResults chooseGroup() {
		WOComponent nextPage = pageWithName("SelectorPopup");
		nextPage.takeValueForKey(this, "returnPage");
		nextPage.takeValueForKey("currGroup", "resultPath");
		nextPage.takeValueForKey(course.eduGroup(), "value");
		nextPage.takeValueForKey(session().valueForKeyPath(
				"strings.Strings.SubgroupEditor.popup"), "dict");
		return nextPage;
	}
	
	public WOActionResults export() {
		NSArray list = course.groupList();
		if(list == null || list.count() == 0) {
			return null;
		}
		Export export = new Export(context(), "grouplist");
		Enumeration enu = list.objectEnumerator();
		int num = 1;
		while (enu.hasMoreElements()) {
			Student student = (Student) enu.nextElement();
			export.beginRow();
			export.addValue(Integer.toString(num));
			num++;
			export.addValue(Person.Utility.fullName(student, true, 2, 0, 0));
			Person person = student.person();
			if(person != null) {
				export.addValue(person.firstName());
				export.addValue(person.secondName());
				if(person.birthDate() != null)
					export.addValue(MyUtility.dateFormat().format(person.birthDate()));
				else
					export.addValue(null);
			}
		}
		return export;
	}
}
