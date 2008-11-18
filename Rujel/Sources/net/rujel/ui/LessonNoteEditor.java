//LessonNoteEditor.java: Class file for WO Component 'LessonNoteEditor'

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
import net.rujel.base.BaseTab;
import net.rujel.base.MyUtility;

import net.rujel.reusables.*;
import net.rujel.auth.*;
//import net.rujel.base.BaseLesson;

import com.webobjects.foundation.*;
import com.webobjects.appserver.*;
import com.webobjects.eocontrol.*;
import com.webobjects.eoaccess.EOUtilities;
import java.util.Enumeration;
import java.util.logging.Logger;
import net.rujel.reusables.WOLogLevel;

public class LessonNoteEditor extends WOComponent {
	protected static Logger logger = Logger.getLogger("rujel.journal");
//	protected static final SettingsReader prefs = new PrefsReader(LessonNoteEditor.class);
	//protected static final String listPresenter = java.util.prefs.Preferences.systemNodeForPackage(LessonNoteEditor.class).node("presenter").get("lessonList","LessonList");
	public EduCourse course;
	private PerPersonLink currPerPersonLink;

	public EduLesson currLesson() {
		if(currPerPersonLink instanceof EduLesson)
			return (EduLesson)currPerPersonLink;
		return null;
	}

	/*	protected NSDictionary lProps;

	public NSDictionary currLessonProperties() {
		if(lProps != null && currPerPersonLink instanceof EduLesson)
			return (NSDictionary)lProps.objectForKey(currPerPersonLink);
		return null;
	}*/

	//    public EduLesson lessonItem;
	//    public Student studentItem;

	/** @TypeInfo PerPersonLink */
	public NSArray lessonsList;
	/*() {
        return EOSortOrdering.sortedArrayUsingKeyOrderArray(course.lessons(),EduLesson.sorter);
    } */

	public Tabs.GenericTab _currTab;

	public Object selector;
	public Student student;

	//	public NamedFlags inheritedAccess;
	//	protected NamedFlags courseAccess;

	private EOEditingContext ec;

	/** @TypeInfo Student */
	//    public NSArray studentsList;

	/** @TypeInfo BaseTab */
	public NSArray tablist;

	public NSArray baseTabs;
	public NSArray allTabs;

	protected NSArray presentTabs;

	public LessonNoteEditor(WOContext context) {
		super(context);
		ec = new SessionedEditingContext(session());
		ec.lock();
		ec.setSharedEditingContext(EOSharedEditingContext.defaultSharedEditingContext());
		ec.unlock();
	}

	public boolean showTabs() {
		return ((allTabs != null && allTabs.count() > 0) || 
				(baseTabs != null && baseTabs.count() > 0));
	}

	public EOEditingContext ec() {
		return ec;
	}
	/*
		public SettingsReader prefs() {
			return prefs;
		}*/

	public String listPresenter() {
		if(present == null)
			return SettingsReader.stringForKeyPath("ui.presenter.lessonList","LessonList");
		String result = (String)present.valueForKey("listPresenter");
		if(result == null)
			return SettingsReader.stringForKeyPath("ui.presenter.lessonList","LessonList");
		return result;
	}
	public PerPersonLink currPerPersonLink() {
		return currPerPersonLink;
	}

	public void setCurrLesson(EduLesson lesson) {
		currPerPersonLink = (PerPersonLink)EOUtilities.localInstanceOfObject(ec,lesson);
		student = null;
	}
	public void setCurrPerPersonLink(PerPersonLink lesson) {
		student = null;
		currPerPersonLink = lesson;
		logger.logp(WOLogLevel.READING,"LessonNoteEditor","setCurrLesson","Open lesson",new Object[] {session(),lesson});
		if (ec.hasChanges()) {
			ec.revert();
			updateLessonList();
		}
		/*	if(regime == LONGLIST) {
				lessonsListForTable = new NSArray(currLesson);
			} */
		/*try {
				String presKey = SettingsReader.stringForKeyPath("edu.presenters.workIntegral","SubgroupEditor");
				Object presKey = prefs.valueForKey("integralPresenterKey");
				if(presKey != null && lesson instanceof NSKeyValueCoding)
					((NSKeyValueCoding)lesson).takeValueForKey(presKey,"integralPresenterKey");
			} catch (Exception ex) {
				logger.logp(WOLogLevel.READING,"LessonNoteEditor","setCurrLesson","Error setting integralPresenterKey to lesson",new Object[] {session(),lesson,ex});
			}*/
		if(Various.boolForObject(session().valueForKeyPath("readAccess.edit.currPerPersonLink"))) {
			session().takeValueForKey(Boolean.TRUE,"prolong");
		}
		/*if(SettingsReader.boolForKeyPath("ui.LessonNoteEditor.autoSwitchSingle", true))
			setSingle(true);*/
	}

	public void selectStudent() {
		if(!accessInterface().flagForKey("oneLesson")) {
			session().takeValueForKey(application().valueForKeyPath("strings.messages.noAccess"),"message");
			return;
		}
		currPerPersonLink = null;
		if (ec.hasChanges()) ec.revert();
		/*Object tmp = selector;
		setSingle(true);
		selector = tmp;*/
		if(selector instanceof Student)
			student = (Student)selector;
		regime = NORMAL;
		//refresh();
	}
	//	public AccessSchemeAssistant courseAssistant;
	//	public AccessSchemeAssistant lessonsAssistant;

	public static final NSArray courseAccessKeys = new NSArray (new String[] {
			"read","create","edit","delete","openCourses","createNewEduPlanCourses","editSubgroups"});
	protected NamedFlags _access;
	public NamedFlags access() {
		if (_access == null) {
			_access = (NamedFlags)session().valueForKeyPath("readAccess.FLAGS.course");
			_access.setKeys(courseAccessKeys);
		}
		return _access;
	}

	public static final NSArray accessKeys = new NSArray(new Object[] {"open","rightSide","leftSide","byDate","oneLesson"});

	protected NamedFlags _accessInterface;
	public NamedFlags accessInterface() {
		if(_accessInterface == null) {
			UserPresentation user = (UserPresentation)session().valueForKey("user");
			if(user != null) {
				try {
					int lvl = user.accessLevel("LessonNoteEditor");
					_accessInterface = new ImmutableNamedFlags(lvl,accessKeys);
				}  catch (AccessHandler.UnlistedModuleException e) {
					_accessInterface = DegenerateFlags.ALL_TRUE;
				}
			}
			if(_accessInterface == null)
				_accessInterface = DegenerateFlags.ALL_TRUE;
		}
		return _accessInterface;
	}

	//protected NSMutableSet accessSchemes = new NSMutableSet();

	public String title() {
		StringBuilder buf = new StringBuilder(50);
		buf.append(course.eduGroup().name()).append(" : ");
		buf.append(course.cycle().subject()).append(" - ");
		buf.append(Person.Utility.fullName(course.teacher().person(),true,2,1,1));
		return buf.toString();
	}

	public void setCourse(EduCourse aCourse) {
		if (aCourse == null) {
			course = null;
			return;
		}
		if(ec == aCourse.editingContext())
			course = aCourse;
		else
			course = (EduCourse)EOUtilities.localInstanceOfObject(ec,aCourse);
		//refresh();
		logger.log(WOLogLevel.READING,"Open course",new Object[] {session(),course});
		/*
			 Object presKey = prefs.valueForKey("integralPresenterKey");
			 if(presKey != null) {
				 try {
					 NSArray lessons = course.lessons();
					 lessons.takeValueForKey(presKey,"integralPresenterKey");
				 } catch (Exception ex) {
					 logger.logp(WOLogLevel.READING,"LessonNoteEditor","setCourse","Error setting integralPresenterKey to lessons of course",new Object[] {session(),ex});
				 }
			 }////////
			 courseAssistant = new AccessSchemeAssistant(((Session)session()).user());
			 courseAssistant.addScheme((EOEnterpriseObject)course.cycle(),"courses");
			 _access = courseAssistant.accessToObject(course);
			 lessonsAssistant = courseAssistant.prolong("lessons",course);*/
		//_access = ((UseAccess)course).access();

		if(accessInterface().flagForKey("rightSide") && !accessInterface().flagForKey("leftSide"))
			regime = LONGLIST;
		else if(!accessInterface().flagForKey("rightSide") && accessInterface().flagForKey("leftSide"))
			regime = BIGTABLE;
		else
			regime = NORMAL;

		presentTabs = (NSArray)session().valueForKeyPath("modules.presentTabs");
		if(presentTabs != null && presentTabs.count() > 0)
			present = (NSKeyValueCoding)presentTabs.objectAtIndex(0);

		refresh();
		notesAddOns = null;
		activeNotesAddOns = null;
	}

	protected void refresh() {
		//		if (ec.hasChanges()) ec.revert();
		/*if(present != null) {
			lessonsList = (NSArray)present.valueForKey("list");
			if(lessonsList != null) {
				//tablist = null;
				_currTab = null;
				return;
			}
		}*/
		//tablist = ((BaseCourse)course).sortedTabs();
		String entityName = (present == null)?null:(String)present.valueForKey("entityName");
		baseTabs = BaseTab.tabsForCourse(course, entityName);
		if(_currTab == null || _currTab instanceof BaseTab.Tab) {
			_currTab = null;
			if(entityName == null)
				entityName = EduLesson.entityName;
			if(baseTabs != null && baseTabs.count() > 0) {
				for (int i = baseTabs.count() -1; i >= 0; i--) {
					BaseTab.Tab t = (BaseTab.Tab)baseTabs.objectAtIndex(i);
					if(currLesson() != null && t.qualifier().evaluateWithObject(currPerPersonLink)) {
						_currTab = t;
						break;
					}
				}
				if(_currTab == null)
					_currTab = (BaseTab.Tab)baseTabs.lastObject();
			}
		}
		session().setObjectForKey(course, "courseForlessons");
		allTabs = (NSArray)session().valueForKeyPath("modules.lessonTabs");
		if(_currTab == null && allTabs != null) {
			tablist = (NSArray)allTabs.objectAtIndex(0);
			Enumeration enu = tablist.objectEnumerator();
			while (enu.hasMoreElements()) {
				Tabs.GenericTab tab = (Tabs.GenericTab) enu.nextElement();
				if((currLesson()==null)?
						tab.defaultCurrent()
						:tab.qualifier().evaluateWithObject(currPerPersonLink)) {
					_currTab = tab;
					break;
				} else if(currLesson() != null && tab.defaultCurrent()) {
					_currTab = tab;
				}
			}
		}
		session().removeObjectForKey("courseForlessons");

		updateLessonList();
	}

	//	public NSArray lessonsListForTable;
	protected static NSArray lessonListForCourseAndPresent(EduCourse course,
			NSKeyValueCoding present, NSMutableArray qualifiers) {
		String entityName = (present == null)?null:(String)present.valueForKey("entityName");
		if(entityName == null)
			entityName = EduLesson.entityName;
		/*String courseAttribute = (String)valueForKeyPath("present.courseAttribute");
		if(courseAttribute == null)
			courseAttribute = "course";*/
		EOQualifier qual = new EOKeyValueQualifier
		("course",EOQualifier.QualifierOperatorEqual,course);
		EOQualifier extraQualifier = (present == null)?null:
			(EOQualifier)present.valueForKey("extraQualifier");
		if(extraQualifier != null) {
			if(qualifiers == null)
				qualifiers = new NSMutableArray(extraQualifier);
			else
				qualifiers.addObject(extraQualifier);
		}
		if(qualifiers != null) {
			qualifiers.addObject(qual);
			qual = new EOAndQualifier(qualifiers);
		}
		EOFetchSpecification fs = new EOFetchSpecification(entityName,qual,EduLesson.sorter);
		return course.editingContext().objectsWithFetchSpecification(fs);
	}

	public void updateLessonList() {
		NSMutableArray qualifiers = null;
		if(currTab() != null) {
			qualifiers = new NSMutableArray(currTab().qualifier());
		}

		lessonsList = lessonListForCourseAndPresent(course, present, qualifiers);
	}
	/*(NSArray)present.valueForKey("list");
				if(lessonsList != null) {
					//tablist = null;
					_currTab = null;
					return;
				}*/
	/*if(tablist != null && tablist.count() > 0) {
				NSMutableArray result = _currTab.lessonsInTab().mutableClone();
				if(regime != NORMAL) {
					int tabIdx = tablist.indexOf(_currTab);
					for (int i = 1; i <= 2 && ((tabIdx + i) < tablist.count()); i++) {
						result.addObjectsFromArray(((BaseTab)tablist.objectAtIndex(tabIdx + i)).lessonsInTab());
					}
				}
				lessonsList =  result.immutableClone();
			} else {
				lessonsList =  course.sortedLessons();
				//lessonsList = EOSortOrdering.sortedArrayUsingKeyOrderArray(course.lessons(),EduLesson.sorter);
			}
		if(student != null) {
			lessonsListForTable = NSArray.EmptyArray;
			return;
			}
			if(lessonsList != null && lessonsList.count() > 0) {
				if(regime == LONGLIST) {
					if(currLesson != null) {
						lessonsListForTable = new NSArray(currLesson);
						return;
					}
					lessonsListForTable = NSArray.EmptyArray;
				} else {
					lessonsListForTable = lessonsList;
				}
			}
	}*/

	/*		studentsList = course.groupList();//eduGroup().list();
		NSArray subgroup = course.subgroup();
		if(subgroup != null && subgroup.count() > 0) {
			NSMutableArray tmp = subgroup.mutableClone();
			Enumeration enumerator = subgroup.objectEnumerator();
			while (enumerator.hasMoreElements()) {
				Object anObject = enumerator.nextElement();
				if(!studentsList.containsObject(anObject))
					tmp.removeObject(anObject);
			}
			studentsList = EOSortOrdering.sortedArrayUsingKeyOrderArray(tmp,Person.sorter);
		}
		tablist = LessonTab.splitArray(lessonsList);
		currTab = (LessonTab)tablist.lastObject(); 
}

public String studentStyle() {
	Boolean sex = (Boolean)valueForKeyPath("studentItem.person.sex");
	if(sex == null) return "grey";
	if (sex.booleanValue())
		return "male";
	else
		return "female";
}

public void setNoteForStudent(String newNoteForStudent) {
	lessonItem.setNoteForStudent(newNoteForStudent,studentItem);
}

public String noteForStudent() {
	if(studentItem == null) return null;
	return lessonItem.noteForStudent(studentItem);
}

public String shortNoteForStudent() {
	String theNote = noteForStudent();
	if (theNote == null || theNote.length() <= 3)
		return theNote;
	String url = application().resourceManager().urlForResourceNamed("text.png",null,null,context().request());
	return "<img src=\"" + url + "\" alt=\"" + theNote + "\" height=\"16\" width=\"16\">";
}

public String fullNoteForStudent() {
	if(studentItem == null)
		return lessonItem.theme();
	String theNote = noteForStudent();
	if (theNote == null || theNote.length() <= 3)
		return null;
	return theNote;
} 

public Object currLesson() {
	return currLesson;
}*/

	public void save() {
		selector = currPerPersonLink;
		student = null;
		ec.lock();
		try {
			if(ec.hasChanges()) {
				boolean newLesson = (currPerPersonLink != null && 
						ec.insertedObjects().containsObject((EOEnterpriseObject)currPerPersonLink));
				if(currPerPersonLink != null)
					((EOEnterpriseObject)currPerPersonLink).validateForSave();
				if(newLesson) {
					MyUtility.setNumberToNewLesson(currLesson(),course);
				}
				NSMutableSet changes = new NSMutableSet();
				changes.addObjectsFromArray((NSArray)ec.updatedObjects().valueForKey("entityName"));
				changes.addObjectsFromArray((NSArray)ec.insertedObjects().valueForKey("entityName"));
				changes.addObjectsFromArray((NSArray)ec.deletedObjects().valueForKey("entityName"));
				//int changes = ec.updatedObjects().count() + ec.insertedObjects().count() + ec.deletedObjects().count();
				ec.saveChanges();
				WOLogLevel level = WOLogLevel.UNOWNED_EDITING;
				if(currPerPersonLink != null) {
					if(newLesson) {
						logger.logp(level,"LessonNoteEditor","save","Created new lesson. " + changes,new Object[] {session(),currPerPersonLink});
						NSNotificationCenter.defaultCenter().postNotification(net.rujel.auth.AccessHandler.ownNotificationName,session().valueForKey("user"),new NSDictionary(currPerPersonLink,"EO"));
					} else {
						if(currPerPersonLink instanceof UseAccess && ((UseAccess)currPerPersonLink).isOwned())
							level = WOLogLevel.OWNED_EDITING;
						/*
							 NSMutableDictionary checksum2 = currLesson.snapshot().mutableClone();
							 Object notes1 = checksum1.removeObjectForKey("notes");
							 Object notes2 = checksum2.removeObjectForKey("notes");
							 if(!checksum1.equals(checksum2)) { */
						if(changes.count() > 0)
							logger.logp(level,"LessonNoteEditor","save","Lesson changed: " + changes,new Object[] {session(),currPerPersonLink});
						/*	changes--;
							 }
							if(changes > 0)//(((notes1==null)^(notes2==null)) || (notes1!=null && !notes1.equals(notes2)))
								logger.logp(level,"LessonNoteEditor","save","Notes changed in lesson (" + changes + ')',new Object[] {session(),currLesson});*/
					}
					if(notesAddOns != null)
						notesAddOns.takeValueForKey(currPerPersonLink, "saveLesson");
					else
						session().valueForKeyPath("modules.saveLesson");
					session().removeObjectForKey("lessonProperies");
					refresh();
				} else {
					if(course instanceof UseAccess && ((UseAccess)course).isOwned())
						level = WOLogLevel.OWNED_EDITING;
					logger.logp(level,"LessonNoteEditor","save","Course comment modified",new Object[] {session(),course});
				}
			}// ec.hasChanges
			session().takeValueForKey(Boolean.FALSE,"prolong");
			currPerPersonLink = null;
			/*if(SettingsReader.boolForKeyPath("ui.LessonNoteEditor.autoSwitchSingle", true))
				setSingle(false); */
			//lessonsList = EOSortOrdering.sortedArrayUsingKeyOrderArray(course.lessons(),EduLesson.sorter);
		} catch (NSValidation.ValidationException vex) {
			logger.logp(WOLogLevel.FINER,"LessonNoteEditor","save","Failed to save lesson",new Object[] {session(),currPerPersonLink,vex});
			session().takeValueForKey(vex.getMessage(),"message");
		} catch (Exception ex) {
			logger.logp(WOLogLevel.WARNING,"LessonNoteEditor","save","Failed to save lesson",new Object[] {session(),currPerPersonLink,ex});
			session().takeValueForKey(ex.getMessage(),"message");
		} finally {
			ec.unlock();
		}
	}

	public void delete() {
		selector = null;
		EOQualifier qual = new EOKeyValueQualifier(
				"course",EOQualifier.QualifierOperatorEqual,course);
		EOFetchSpecification fs = new EOFetchSpecification(
				currLesson().entityName(),qual,EduLesson.sorter);
		NSArray allLessons = ec.objectsWithFetchSpecification(fs);
//EOUtilities.objectsMatchingKeyAndValue(ec,currLesson().entityName(),"course", course);
		WOLogLevel level = WOLogLevel.UNOWNED_EDITING;
		if(currLesson().notes().count() > 0)
			level = WOLogLevel.MASS_EDITING;
		if(currLesson() instanceof UseAccess && ((UseAccess)currLesson()).isOwned())
			level = WOLogLevel.OWNED_EDITING;
		ec.lock();
		try {
			if(ec.insertedObjects().contains(currLesson())) { //just inserted
				level = WOLogLevel.OWNED_EDITING;
				logger.logp(level,"LessonNoteEditor","delete","Undo lesson creation: ",new Object[] {session(),currLesson()});
				ec.revert();
				NSMutableArray tmp = lessonsList.mutableClone();
				/*				if(tmp.removeObjectAtIndex(idx) != currLesson())
						throw new RuntimeException("Something wrong when deleting lesson"); */
				tmp.removeObject(currLesson());
				lessonsList = tmp.immutableClone();
				currPerPersonLink = null;
			} else { //delete from middle
				int idx = allLessons.indexOf(currLesson());
				if(idx >= allLessons.count())
					throw new RuntimeException("Something wrong when deleting lesson");
				currLesson().validateForDelete();
				int num = currLesson().number().intValue();
				ec.deleteObject(currLesson());
				/*
					BaseTab tmp = _currTab;
					if(tablist != null && tablist.count() > 0) {
						num = currTab().length();
						if(num <=1) {
							ec.deleteObject(_currTab);
						} else {
							_currTab.setLessonsCount(num - 1);
						}
						num = tablist.indexOf(_currTab);
						if (num < (tablist.count() - 1)) { // lower tab starters						
							num ++;
							_currTab = (BaseTab)tablist.objectAtIndex(num);
							BaseTab tab = _currTab;
							tab.setFirstLessonNumber(tab.startIndex() - 1);
							num ++;
							while (num < tablist.count()) {
								tab = (BaseTab)tablist.objectAtIndex(num);
								tab.setFirstLessonNumber(tab.startIndex() - 1);
								num ++;
							}
						} else {
							_currTab = null;
						}
					}*/
				if(idx < (allLessons.count() - 1)) { //lower numbers of following lessons
					idx++;
					while(idx < allLessons.count()) {
						((EduLesson)allLessons.objectAtIndex(idx)).setNumber(new Integer(num));
						idx++;
						num++;
					}
				} // end number lowering
				//		logger.logp(level,"LessonNoteEditor","delete","Deletion failed: ",new Object[] {session(),currLesson(),vex});
				logger.logp(level,"LessonNoteEditor","delete","Deleting lesson ",new Object[] {session(),currLesson()});
				ec.saveChanges();
				currPerPersonLink = null;
				/*if(tablist != null && tablist.count() > 0) {
						tablist = ((BaseCourse)course).sortedTabs();
						if(tmp.editingContext() != null)
							_currTab = tmp;
						else
							currTab();
					}*/
				updateLessonList();
				//refresh();
				//lessonsList = EOSortOrdering.sortedArrayUsingKeyOrderArray(course.lessons(),EduLesson.sorter);
				/*		} else {
						session().takeValueForKey(application().valueForKeyPath("strings.messages.noAccess"),"message");
					logger.logp(WOLogLevel.OWNED_EDITING,"LessonNoteEditor","delete","Denied to delete lesson",new Object[] {session(),currLesson()});
					} */
			}
		} catch (NSValidation.ValidationException vex) {
			logger.logp(level,"LessonNoteEditor","delete","Deletion failed: ",new Object[] {session(),currLesson(),vex});
			session().takeValueForKey(vex.toString(),"message");
		} catch (Exception ex) {
			logger.logp(WOLogLevel.WARNING,"LessonNoteEditor","delete","Deletion failed: ",new Object[] {session(),currLesson(),ex});
			String message = (String)application().valueForKeyPath("strings.messages.error") + " : " + application().valueForKeyPath("strings.messages.cantDelete") + " : " + ex;
			session().takeValueForKey(message,"message");
		} finally {
			ec.unlock();
		}
	}
	/*
		 public void selectLesson() {
			 currLesson = lessonItem;
			 selector = currLesson;
			 _access = accessSchemeAssistant.accessToObject(currLesson);
			 if (ec.hasChanges()) {
				 ec.revert();
				 refresh();
			 }
			 logger.log(WOLogLevel.READING,"Open lesson",new Object[] {session(),currLesson});
			 if(regime == LONGLIST) {
				 lessonsListForTable = new NSArray(currLesson);
			 }
		 } */

	public void addLesson() {
		save();
		if(session().valueForKey("message") != null)
			return;

		ec.lock();
		try {
			//	if(lessonsAssistant.accessToObject(null).flagForKey("create")) {
			/*NSArray tabs = ((BaseCourse)course).sortedTabs();
				if (tabs != null && tabs.count() > 0) {
					_currTab = (BaseTab)tabs.lastObject();				
				}*/
			//			currTab();
			String entityName = EduLesson.entityName;
			//String courseAttribute = "course";
			if(present != null) {
				entityName = (String)present.valueForKey("entityName");
				if(entityName == null)
					entityName = EduLesson.entityName;
				/*courseAttribute = (String)present.valueForKey("courseAttribute");
				if(courseAttribute == null)
					courseAttribute = "course";*/
			}
			currPerPersonLink = (EduLesson)EOUtilities.createAndInsertInstance(ec,entityName);
			Integer max = (Integer)valueForKeyPath("lessonsList.@max.number");
			currLesson().setNumber(new Integer((max==null)?1:max.intValue() + 1));
			currLesson().setDate((NSTimestamp)session().valueForKey("today"));
			currLesson().addObjectToBothSidesOfRelationshipWithKey(course,"course");
			//	if(tablist != null)/* _currTab = */((BaseTab)tablist.lastObject()).setLessonsCount(_currTab.length() + 1);
			//	if(_currTab != null) _currTab.setLessonsCount(_currTab.length() + 1);
			//			refresh();
			selector = null;
			/*	} else {
					logger.logp(WOLogLevel.OWNED_EDITING,"LessonNoteEditor","delete","Denied to create lesson",session());
				session().takeValueForKey(valueForKeyPath("application.strings.messages.noAccess"),"message");
				} */
			//		lessonsList = EOSortOrdering.sortedArrayUsingKeyOrderArray(course.lessons(),EduLesson.sorter);
			//updateLessonList();
			selector = currPerPersonLink;
			NSMutableArray list = lessonsList.mutableClone();
			list.addObject(currPerPersonLink);
			EOSortOrdering.sortArrayUsingKeyOrderArray(list, EduLesson.sorter);
			lessonsList = list.immutableClone();
			//lessonsList = _currTab.lessonsInTab();
			session().takeValueForKey(Boolean.TRUE,"prolong");

		} finally {
			ec.unlock();
		}
	}
	/*
		 public String tdStyle() {
			 if(lessonItem == currLesson)
				 return "selection";
			 else
				 return studentStyle();
		 }

		 public String onClick() {
			 if(currLesson == null || lessonItem != currLesson) {
				 String href = context().componentActionURL();
				 return "checkRun('" + href + "');";
			 }
			 else
				 return null;
		 }

		public WOActionResults back() {
			WOComponent nextPage = (WOComponent)session().objectForKey("SrcCourseComponent");
			if(nextPage == null)
				nextPage = pageWithName("SrcMark");
			else
				nextPage.ensureAwakeInContext(context());
			return nextPage;
		}

		 public String tabStyle() {
			 if(currTab == item) return "selection";
			 else return "grey";
		 }

		 public void selectTab 
		 public String inTabStyle() {
			 if (currTab == null || currTab.lessonsInTab().containsObject(lessonItem))
				 return "selection";
			 return null;
		 }

		 public String lessonTitle() {
			 if(lessonItem == null) return null;
			 String result = lessonItem.title();
			 if(result != null)
				 return result;
			 return dateFormatter.format(lessonItem.date());
		 }

		 public void setLessonTitle(String aValue) {
			 String newTitle = null;
			 if(aValue != null) {
				 NSTimestamp aDate = (NSTimestamp)dateFormatter.parseObject(aValue, new java.text.ParsePosition(0));
				 if(aDate == null) {
					 if(aValue.length() > 10)
						 newTitle = aValue.substring(0,10);
					 else
						 newTitle = aValue;
					 makeDateFromNum(currLesson);
				 } else {
					 currLesson.setDate(aDate);
				 }
			 }
			 currLesson.setTitle(newTitle);
		 }
	 */
	protected static void makeDateFromNum(EduLesson les) {
		NSArray args = new NSArray(new Object[] {les.course(),les.number()});
		EOQualifier qual = EOQualifier.qualifierWithQualifierFormat("course = %@ AND number < %@ AND title = nil",args);
		NSArray sort = new NSArray(new EOSortOrdering ("number",EOSortOrdering.CompareDescending));
		EOFetchSpecification fs = new EOFetchSpecification(les.entityName(),qual,sort);
		fs.setFetchLimit(1);
		NSArray found = les.editingContext().objectsWithFetchSpecification(fs);
		if(found == null || found.count() == 0) {
			qual = EOQualifier.qualifierWithQualifierFormat("course = %@ AND number > %@ AND title = nil",args);
			fs.setQualifier(qual);
			sort = new NSArray(new EOSortOrdering ("number",EOSortOrdering.CompareAscending));
			fs.setSortOrderings(sort);
			found = les.editingContext().objectsWithFetchSpecification(fs);
			if(found == null || found.count() == 0) return;
		}
		EduLesson ajacent = (EduLesson)found.objectAtIndex(0);
		les.setDate(ajacent.date());
	}

	public void up() {
		int idx = lessonsList.indexOf(currLesson());
		if(idx < 0) 
			throw new RuntimeException("Something wrong when moving lesson up");
		if(idx == 0 && (_currTab == null || (_currTab instanceof BaseTab.Tab))) {
			// move to previous tab
			BaseTab tab = null;
			EOEditingContext tmpEc = ec;
			if(ec.hasChanges()) {
				if(ec.insertedObjects().contains(currLesson()))
					return;
				tmpEc = new EOEditingContext();
			}
			int tabIdx = (baseTabs == null)?-1:baseTabs.indexOf(_currTab);
			if(_currTab == null || tabIdx == 0) {
				if(lessonsList.count() <= 1)
					return;
				EduLesson lesson = (EduLesson)lessonsList.objectAtIndex(1);
				if(ec != tmpEc)
					lesson = (EduLesson)EOUtilities.localInstanceOfObject(tmpEc, lesson);
				tab = BaseTab.tabForLesson(lesson, true);
			} else {
				EduLesson lesson = currLesson();
				if(ec != tmpEc)
					lesson = (EduLesson)EOUtilities.localInstanceOfObject(tmpEc, lesson);
				tab = BaseTab.tabForLesson(lesson, false);
				if(tab == null)
					return;
				if(lessonsList.count() > 1) {
					Integer num = lesson.number();
					num = new Integer(num.intValue() + 1);
					tab.setFirstLessonNumber(num);
				} else {
					tmpEc.deleteObject(tab);
				}
			}
			tmpEc.saveChanges();
			_currTab = null;
			refresh();
			return;
		}
		if(idx == 0)
			return;
		NSMutableArray tmp = lessonsList.mutableClone();
		EduLesson prev = (EduLesson)tmp.replaceObjectAtIndex(currLesson(),idx - 1);
		Integer oldNum = currLesson().number();
		currLesson().setNumber(prev.number());
		prev.setNumber(oldNum);
		if(tmp.replaceObjectAtIndex(prev,idx) != currLesson())
			throw new RuntimeException("Something wrong when moving lesson up");
		lessonsList = tmp.immutableClone();
		if(currLesson().title()!=null)
			makeDateFromNum(currLesson());
	}

	public void down() {
		int idx = lessonsList.indexOf(currLesson());
		if(idx < 0 || idx >= lessonsList.count())
			throw new RuntimeException("Something wrong when moving lesson down");
		if(idx == lessonsList.count() - 1 &&
				(_currTab == null || (_currTab instanceof BaseTab.Tab))) {
			// move to next tab
			BaseTab tab = null;
			EOEditingContext tmpEc = ec;
			if(ec.hasChanges()) {
				tmpEc = new EOEditingContext();
				if(ec.insertedObjects().contains(currLesson()))
					return;
			}
			int tabIdx = (baseTabs==null)?-1:baseTabs.indexOf(_currTab);
			if(_currTab == null || tabIdx == baseTabs.count() -1) { //creating new tab
				if(lessonsList.count() <= 1)
					return;
				EduLesson lesson = currLesson();
				if(ec != tmpEc)
					lesson = (EduLesson)EOUtilities.localInstanceOfObject(tmpEc, lesson);
				tab = BaseTab.tabForLesson(lesson, true);
			} else { // move to next tab
				EduCourse crs  = course;
				if(ec != tmpEc)
					crs = (EduCourse)EOUtilities.localInstanceOfObject(tmpEc, crs);
				NSArray tabs = BaseTab.baseTabsForCourse(crs, currLesson().entityName());
				Enumeration enu = tabs.objectEnumerator();
				while (enu.hasMoreElements()) {
					tab = (BaseTab) enu.nextElement();
					if(tab.firstLessonNumber().intValue() > currLesson().number().intValue())
						break;
				}
				if(idx == 0)
					tmpEc.deleteObject(tab);
				else
					tab.setFirstLessonNumber(currLesson().number());
			}
			tmpEc.saveChanges();
			_currTab = null;
			refresh();
			return;
		} //just lower lesson inside current tab
		if(idx == lessonsList.count() - 1)
			return;
		NSMutableArray tmp = lessonsList.mutableClone();
		EduLesson next = (EduLesson)tmp.replaceObjectAtIndex(currLesson(),idx + 1);
		Integer oldNum = currLesson().number();
		currLesson().setNumber(next.number());
		next.setNumber(oldNum);
		if(tmp.replaceObjectAtIndex(next,idx) != currLesson())
			throw new RuntimeException("Something wrong when moving lesson down");
		lessonsList = tmp.immutableClone();
		if(currLesson().title()!=null)
			makeDateFromNum(currLesson());
	} 

	public void splitTab() {
		if(baseTabs != null && !(_currTab instanceof BaseTab.Tab)) {
			_currTab = null;
			refresh();
			return;
		}
		int idx = lessonsList.indexOf(currPerPersonLink);
		BaseTab tab = null;
		EOEditingContext tmpEc = ec;
		EduLesson lesson = currLesson();
		if(tmpEc.hasChanges()) {
			tmpEc = new EOEditingContext();
			if(ec.insertedObjects().contains(lesson))
				return;
			lesson = (EduLesson)EOUtilities.localInstanceOfObject(tmpEc, lesson);
		}
		if(idx == 0)  {
			if(baseTabs == null || _currTab == baseTabs.objectAtIndex(0))
				return;
			tab = BaseTab.tabForLesson(lesson, false);
			if(tab == null)
				return;
			tmpEc.deleteObject(tab);
		} else {
			tab = BaseTab.tabForLesson(lesson, true);
		}
		tmpEc.saveChanges();
		_currTab = null;
		refresh();
	}

	public Tabs.GenericTab currTab() {/*
			if (tablist == null) {
				return _currTab;
			}
			if ((_currTab == null || _currTab.editingContext() == null) && tablist != null)
				_currTab = (BaseTab)tablist.lastObject();

			if(currLesson() != null && !_currTab.containsLesson(currLesson())) {
				//			tablist = ((BaseCourse)course).sortedTabs();
				int lesNum = currLesson().number().intValue();
				int currNum = tablist.indexOfObject(_currTab);
				if(lesNum < _currTab.startIndex()) {
					while (currNum >= 0 && lesNum < _currTab.startIndex()) {
						currNum--;
						_currTab = (BaseTab)tablist.objectAtIndex(currNum);
					}
				} else {
					while (currNum < (tablist.count() - 1) && !_currTab.lessonsInTab().containsObject(currPerPersonLink)) {
						currNum++;
						_currTab = (BaseTab)tablist.objectAtIndex(currNum);
					}
				}
			}*/
		return _currTab;
	}

	public void setCurrTab (Tabs.GenericTab tab) {
		currPerPersonLink = null;
		if(tab != null) {
			_currTab = tab;
		}
		if (ec.hasChanges()) {
			ec.revert();
			refresh();
		} else {
			updateLessonList();
		}
		//lessonsList = _currTab.lessonsInTab();
	}

	/*
		 public NSArray unmentionedStudents() {
			 NSMutableSet unmentionedSet = new NSMutableSet();
			 Enumeration lessEnum = lessonsList.objectEnumerator();
			 EduLesson les;
			 Enumeration noteEnum;
			 EOEnterpriseObject note;
			 Object stu;
			 while (lessEnum.hasMoreElements()) {
				 les = (EduLesson)lessEnum.nextElement();
				 noteEnum = les.notes().objectEnumerator();
				 while (noteEnum.hasMoreElements()) {
					 note = (EOEnterpriseObject)noteEnum.nextElement();
					 stu = note.storedValueForKey("student");
					 if(!studentsList.containsObject(stu))
						 unmentionedSet.addObject(stu);
				 }
			 }
			 if(unmentionedSet.count() < 1)
				 return null;
			 NSArray tmp = unmentionedSet.allObjects();
			 if(tmp.count() == 1)
				 return tmp;

			 return EOSortOrdering.sortedArrayUsingKeyOrderArray(tmp,Person.sorter);
		 } 

		 public String dateFieldID() {
			 if(selector == null)
				 return "focus";
			 else
				 return null;
		 }

		 public String themeFieldID() {
			 if(selector != null && selector.equals(currLesson))
				 return "focus";
			 else
				 return null;
		 }*/

	public WOComponent editSubgroup() {
		String pageName = SettingsReader.stringForKeyPath("ui.subRegime.editCourseSubgroup","SubgroupEditor");
		WOComponent nextPage = pageWithName(pageName);
		nextPage.takeValueForKey(course,"course");
		session().takeValueForKey(this,"pushComponent");
		session().takeValueForKey(Boolean.TRUE,"prolong");
		logger.logp(WOLogLevel.FINER,"LessonNoteEditor","editSubgroup","Going to edit subgroup for course",new Object[] {session(),course});
		return nextPage;
	}

	//Regimes
	public static final int NORMAL = 1;
	public static final int LONGLIST = 0;
	public static final int BIGTABLE = 2;

	public int regime = NORMAL;
	//   public NSDictionary present;

	public NSArray presentTabs() {
		//		if(!(accessInterface().flagForKey("byDate") && accessInterface().flagForKey("oneLesson")))
		//			return null;
		/*if(presentTabs == null) {
			presentTabs = (NSArray)session().valueForKeyPath("modules.presentTabs");
			if(presentTabs != null && presentTabs.count() > 0)
				present = (NSKeyValueCoding)presentTabs.objectAtIndex(0);
		}*/
		return presentTabs;
	}

	public NSKeyValueCoding present;/*() {
			if(presentTabs() != null) {
				Enumeration en = presentTabs().objectEnumerator();
				while (en.hasMoreElements()) {
					NSDictionary pres = (NSDictionary)en.nextElement();
					if(Various.boolForObject(pres.valueForKey("single")) == single)
						return pres;
				}
			}
		return null;
		}
	 */
	public void setPresent(NSKeyValueCoding pres) {
		if(pres != null)
			present = pres;
		student = null;
		selector = null;
		currPerPersonLink = null;
		if(ec.hasChanges())
			ec.revert();

		refresh();
		/*if(pres == null) return;
				single = Various.boolForObject(pres.valueForKey("single"));
				if(single) {
					regime = NORMAL;
					refresh();
				}
				else student = null; */
	}
	/*
	public void setSingle(boolean val) {
		if(present != null && single() != val) {
			String toggle = (String)present.valueForKey("toggleSingle");
			if(toggle != null) {
				Enumeration en = presentTabs().objectEnumerator();
				while (en.hasMoreElements()) {
					NSKeyValueCoding cur = (NSKeyValueCoding)en.nextElement();
					//boolean curSingle = Various.boolForObject(cur.valueForKey("single"));
					String mode = (String)cur.valueForKey("mode");
					if(toggle.equals(mode) && 
							val == Various.boolForObject(cur.valueForKey("single"))) {
						setPresent(cur);
						return;
					}
				}
			}
		}
	}*/

	public boolean single() {
		if(student != null)
			return true;
		if(currPerPersonLink == null)
			return false;
		if(lessonsList == null || !lessonsList.containsObject(currPerPersonLink))
			return true;
		if (present==null)
			return false;
		return Various.boolForObject(present.valueForKey("single"));
	}
	/*
	public void setSingle(Object v) {
		;
	}

			 public String normalButtonStyle() {
				 if(regime == NORMAL)
					 return "border-style:inset;outline:black solid 1px;";
				 else
					 return null;
			 }
			 public String longlistButtonStyle() {
				 if(regime == LONGLIST)
					 return "border-style:inset;outline:black solid 1px;";
				 else
					 return null;
			 }
			 public String bigtableButtonStyle() {
				 if(regime == BIGTABLE)
					 return "border-style:inset;outline:black solid 1px;";
				 else
					 return null;
			 }
			 public void goNormal() {
				 student = null;
				 selector = null;
				 regime = NORMAL;
				 refresh();
			 }
			 public void goLonglist() {
				 student = null;
				 //		selector = null;
				 regime = LONGLIST;
				 if(tablist != null) {
					 int idx = tablist.indexOf(_currTab);
					 while(idx > 0 && idx > tablist.count() - 3) {
						 idx--;
					 }
					 _currTab = (BaseTab)tablist.objectAtIndex(idx);
				 }
				 updateLessonList();
			 }
			 public void goBigtable() {
				 student = null;
				 selector = null;
				 regime = BIGTABLE;
				 if(tablist != null) {
					 int idx = tablist.indexOf(_currTab);
					 while(idx > 0 && idx > tablist.count() - 3) {
						 idx--;
					 }
					 _currTab = (BaseTab)tablist.objectAtIndex(idx);
				 }
				 updateLessonList();
			 }*/
	/*
			 public String rowClass() {
				 if(lessonItem == currLesson) return "selection";
				 if(lessonItem.title() != null) return "gerade";
				 else return "ungerade";
			 }

			 public boolean canEdit() {
				 if(lessonItem == currLesson) {
					 NamedFlags acc = (NamedFlags)currLesson.valueForKey("access");
					 if(acc == null) acc = DegenerateFlags.ALL_FALSE;
					 return ((acc.flagForKey("editLessonDescription") || access().flagForKey("editLessonDescription")) ||
							 (ec.insertedObjects().contains(currLesson) && (access().flagForKey("createLesson") || acc.flagForKey("createLesson"))));
				 } else return false;
			 } */

	public String tdWidth() {
		if(regime != NORMAL) 
			return null;
		if(student == null)
			return "50%";
		else
			return "20%";
	}

	public boolean hideLeft() {
		if(regime == LONGLIST || !accessInterface().flagForKey("leftSide")) return true;
		return false;
	}

	public boolean showRight() {
		if(regime == BIGTABLE || !accessInterface().flagForKey("rightSide")) return false;
		return true;
	}

	public boolean showSeparator() {
		if(hideLeft() && currPerPersonLink != null) return false;
		if(!(accessInterface().flagForKey("rightSide") && accessInterface().flagForKey("leftSide"))) return false;
		return true;
	}

	public void moveLeft() {
		if(!accessInterface().flagForKey("rightSide")) {
			session().takeValueForKey(application().valueForKeyPath("strings.messages.noAccess"),"message");
			return;
		}
		student = null;
		selector = null;
		if(regime == BIGTABLE) {
			regime = NORMAL; 
//			tablist = ((BaseCourse)course).sortedTabs();
//			currTab();
		} else if (regime == NORMAL) {
			regime = LONGLIST;
			/*if(tablist != null) {
						int idx = tablist.indexOf(_currTab);
						while(idx > 0 && idx > tablist.count() - 3) {
							idx--;
						}
						_currTab = (BaseTab)tablist.objectAtIndex(idx);
					}*/
		}
		updateLessonList();
	}

	public void moveRight() {
		if(!accessInterface().flagForKey("leftSide")) {
			session().takeValueForKey(application().valueForKeyPath("strings.messages.noAccess"),"message");
			return;
		}
		student = null;
		selector = null;
		if(regime == LONGLIST)  {
			regime = NORMAL; 
			//tablist = ((BaseCourse)course).sortedTabs();
			//currTab();
		} else if (regime == NORMAL)  {
			regime = BIGTABLE;
			/*if(tablist != null) {
						int idx = tablist.indexOf(_currTab);
						while(idx > 0 && idx > tablist.count() - 3) {
							idx--;
						}
						_currTab = (BaseTab)tablist.objectAtIndex(idx);
					}*/
		}
		updateLessonList();
	}

	/** @TypeInfo com.webobjects.foundation.NSKeyValueCoding */
	public NSArray notesAddOns;

	/** @TypeInfo java.lang.String */
	public NSMutableArray activeNotesAddOns; 
	/*
			public NSArray notesAddOns () {
				if(notesAddOns == null) {
					notesAddOns = (NSArray)session().valueForKeyPath("modules.notesAddOns");
					if(notesAddOns == null)
						notesAddOns = NSArray.EmptyArray;
				}
				return notesAddOns;
			}
			public NSMutableArray activeNotesAddOns() {
				if(activeNotesAddOns == null) {
					activeNotesAddOns = new NSMutableArray();
					if(notesAddOns() != null && notesAddOns().count() > 0) {
						Enumeration en  = notesAddOns().objectEnumerator();
						while (en.hasMoreElements()) {
							NSKeyValueCoding curr = (NSKeyValueCoding)en.nextElement();
							if(Various.boolForObject(curr.valueForKey("defaultOn"))) {
								activeNotesAddOns.addObject(curr);
							}
						}
					}
				}
				return activeNotesAddOns;
			}*/
}
