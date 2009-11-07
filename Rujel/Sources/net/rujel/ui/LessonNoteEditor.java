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
	public EduCourse course;
	private PerPersonLink currPerPersonLink;

	public EduLesson currLesson() {
		if(currPerPersonLink instanceof EduLesson)
			return (EduLesson)currPerPersonLink;
		return null;
	}


	/** @TypeInfo PerPersonLink */
	public NSArray lessonsList;

	public Tabs.GenericTab _currTab;

	public Object selector;
	public Student student;

	private EOEditingContext ec;

	public Object item;

	public NSArray baseTabs;
	public NSArray allTabs;

	protected NSArray presentTabs;

	public LessonNoteEditor(WOContext context) {
		super(context);
		ec = new SessionedEditingContext(session());
		ec.lock();
		ec.unlock();
		session().savePageInPermanentCache(this);
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
			return "LessonList";
			//return SettingsReader.stringForKeyPath("ui.presenter.lessonList","LessonList");
		String result = (String)present.valueForKey("listPresenter");
		if(result == null)
			return "LessonList";
			//return SettingsReader.stringForKeyPath("ui.presenter.lessonList","LessonList");
		return result;
	}
	public PerPersonLink currPerPersonLink() {
		return currPerPersonLink;
	}

	public void setCurrLesson(EduLesson lesson) {
		student = null;
		currPerPersonLink = (PerPersonLink)EOUtilities.localInstanceOfObject(ec,lesson);
		session().setObjectForKey(lesson.date(), "recentDate");
	}
	
	public void setCurrPerPersonLink(PerPersonLink lesson) {
		student = null;
		if(lesson instanceof EduLesson)
			session().setObjectForKey(((EduLesson)lesson).date(), "recentDate");
		if (ec.hasChanges()) {
			ec.revert();
			updateLessonList();
		}
		currPerPersonLink = lesson;
		if(currLesson() == null)
			return;
		logger.log(WOLogLevel.READING,"Open lesson",new Object[] {session(),lesson});
		String accKey = (context().page() == this)?"currPerPersonLink":currLesson().entityName();
		if(Various.boolForObject(session().valueForKeyPath("readAccess.edit." + accKey))) {
			session().takeValueForKey(Boolean.TRUE,"prolong");
		}
	}

	public void selectStudent() {
		if(!accessInterface().flagForKey("oneLesson")) {
			session().takeValueForKey(application().valueForKeyPath(
					"strings.Strings.messages.noAccess"),"message");
			return;
		}
		if(valueForKeyPath("currLesson.editingContext") != null) {
			ec.refaultObject(currLesson());
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

	public static final NSArray accessKeys = new NSArray(
			new Object[] {"open","rightSide","leftSide","byDate","oneLesson"});

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
		if(course.teacher() != null)
			buf.append(Person.Utility.fullName(course.teacher().person(),true,2,1,1));
		else
			buf.append(application().valueForKeyPath("strings.RujelBase_Base.vacant"));
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
//		notesAddOns = null;
//		activeNotesAddOns = null;
	}

	public void refresh() {
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
		if(entityName == null)
			entityName = EduLesson.entityName;
		baseTabs = BaseTab.tabsForCourse(course, entityName);
		if(_currTab == null || _currTab instanceof BaseTab.Tab) {
			if(_currTab != null && (baseTabs == null || !baseTabs.containsObject(_currTab)))
				_currTab = null;
			if(baseTabs != null && baseTabs.count() > 0) {
				if(currPerPersonLink != null){
					for (int i = baseTabs.count() -1; i >= 0; i--) {
						BaseTab.Tab t = (BaseTab.Tab)baseTabs.objectAtIndex(i);
						if(currLesson() != null && t.qualifier().
								evaluateWithObject(currPerPersonLink)) {
							_currTab = t;
							break;
						}
					}
				}
				if(_currTab == null)
					_currTab = (BaseTab.Tab)baseTabs.lastObject();
			}
		}
		session().setObjectForKey(course, "courseForlessons");
		allTabs = (NSArray)session().valueForKeyPath("modules.lessonTabs");
		if(_currTab == null && allTabs != null && allTabs.count() > 0) {
			NSArray tablist = (NSArray)allTabs.objectAtIndex(0);
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
		session().takeValueForKey(Boolean.FALSE,"prolong");
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
		NSArray prefetchPaths = (NSArray)present.valueForKey("prefetchPaths");
		if(prefetchPaths != null)
			fs.setPrefetchingRelationshipKeyPaths(prefetchPaths);
		return course.editingContext().objectsWithFetchSpecification(fs);
	}

	public void updateLessonList() {
		NSMutableArray qualifiers = null;
		if(currTab() != null) {
			qualifiers = new NSMutableArray(currTab().qualifier());
		}
		if(valueForKeyPath("currLesson.editingContext") != null) {
			ec.refaultObject(currLesson());
		}

		lessonsList = lessonListForCourseAndPresent(course, present, qualifiers);
		if(lessonsList != null && lessonsList.count() > 0) {
			EduLesson lesson = (EduLesson)lessonsList.lastObject();
			session().setObjectForKey(lesson.date(), "recentDate");
		} else {
			session().setObjectForKey(session().valueForKey("today"), "recentDate");
		}
	}
	
	public void saveNoreset() {
		save(false);
		//return this;
	}
	
	public void save() {
		save(true);
		//return this;
	}

	protected void save(boolean reset) {
		if(currPerPersonLink instanceof EOEnterpriseObject) {
			String entityName = ((EOEnterpriseObject)currPerPersonLink).entityName();
			String presentEntity = (String)valueForKeyPath("present.entityName");
			if(entityName.equals((presentEntity == null)?EduLesson.entityName:presentEntity))
				selector = currPerPersonLink;
		}
		student = null;
		ec.lock();
		try {
			if(ec.hasChanges()) {
				boolean newLesson = (currPerPersonLink != null && 
						ec.insertedObjects().containsObject((EOEnterpriseObject)currPerPersonLink));
				if(currPerPersonLink != null)
					((EOEnterpriseObject)currPerPersonLink).validateForSave();
				if(newLesson) {
					MyUtility.setNumberToNewLesson(currLesson());
				}
				NSMutableSet changes = new NSMutableSet();
				changes.addObjectsFromArray((NSArray)ec.updatedObjects().valueForKey("entityName"));
				changes.addObjectsFromArray((NSArray)ec.insertedObjects().valueForKey("entityName"));
				changes.addObjectsFromArray((NSArray)ec.deletedObjects().valueForKey("entityName"));
				ec.saveChanges();
				WOLogLevel level = WOLogLevel.UNOWNED_EDITING;
				if(currPerPersonLink != null) {
					if(newLesson) {
						logger.log(level,"Created new lesson. " + changes,
								new Object[] {session(),currPerPersonLink});
						NSNotificationCenter.defaultCenter().postNotification(
								net.rujel.auth.AccessHandler.ownNotificationName,session().valueForKey(
										"user"),new NSDictionary(currPerPersonLink,"EO"));
					} else {
						if(currPerPersonLink instanceof UseAccess && 
								((UseAccess)currPerPersonLink).isOwned())
							level = WOLogLevel.OWNED_EDITING;
						if(changes.count() > 0)
							logger.log(level,"Lesson changed: " + changes,
									new Object[] {session(),currPerPersonLink});
					}
					if(currPerPersonLink != null) {
						NSMutableDictionary dict = new NSMutableDictionary(currPerPersonLink,"object");
						if(currPerPersonLink instanceof EduLesson)
							dict.takeValueForKey(currPerPersonLink, "lesson");
						String entityName = currLesson().entityName();
						dict.takeValueForKey(entityName, "entityName");
						session().setObjectForKey(dict, "objectSaved");
						session().valueForKeyPath("modules.objectSaved");
						session().removeObjectForKey("objectSaved");
					}
					if(reset) {
						if(_currTab != null && currLesson() != null
								&& !_currTab.qualifier().evaluateWithObject(currLesson()))
							_currTab = null;
						refresh();
					}
				} else {
//					if(course instanceof UseAccess && ((UseAccess)course).isOwned())
//						level = WOLogLevel.OWNED_EDITING;
					logger.log(level,"Course comment modified",new Object[] {session(),course});
				}
			}// ec.hasChanges
			//session().takeValueForKey(Boolean.FALSE,"prolong");
			if(reset)
				currPerPersonLink = null;
		} catch (NSValidation.ValidationException vex) {
			logger.log(WOLogLevel.FINER,"Failed to save lesson",
					new Object[] {session(),currPerPersonLink,vex});
			session().takeValueForKey(vex.getMessage(),"message");
		} catch (Exception ex) {
			logger.log(WOLogLevel.WARNING,"Failed to save lesson",
					new Object[] {session(),currPerPersonLink,ex});
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
//		if(currLesson() instanceof UseAccess && ((UseAccess)currLesson()).isOwned())
//			level = WOLogLevel.OWNED_EDITING;
		ec.lock();
		try {
			if(ec.insertedObjects().contains(currLesson())) { //just inserted
				level = WOLogLevel.OWNED_EDITING;
				logger.log(level,"Undo lesson creation: ",new Object[] {session(),currLesson()});
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

				NSMutableDictionary dict = new NSMutableDictionary(course,"course");
				if(currLesson() != null) {
					dict.takeValueForKey(currLesson().entityName(), "entityName");
					dict.takeValueForKey(currLesson().date(), "date");
					dict.takeValueForKey(EOUtilities.
							primaryKeyForObject(ec, currLesson()), "pKey");
				} else {
					String entityName = (String)valueForKeyPath("present.entityName");
					if(entityName == null)
						entityName = EduLesson.entityName;
					dict.takeValueForKey(entityName, "entityName");
				}
				
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
				logger.log(level,"Deleting lesson ",new Object[] {session(),currLesson()});
				ec.saveChanges();
				session().setObjectForKey(dict, "objectSaved");
				session().valueForKeyPath("modules.objectSaved");
				session().removeObjectForKey("objectSaved");

				currPerPersonLink = null;
				updateLessonList();
			}
		} catch (NSValidation.ValidationException vex) {
			logger.log(level,"Deletion failed: ",new Object[] {session(),currLesson(),vex});
			session().takeValueForKey(vex.toString(),"message");
		} catch (Exception ex) {
			logger.log(WOLogLevel.WARNING,"Deletion failed: ",new Object[] {session(),currLesson(),ex});
			String message = (String)application().valueForKeyPath(
					"strings.Strings.messages.error") + " : " + application().valueForKeyPath(
							"strings.Strings.messages.cantDelete") + " : " + ex;
			session().takeValueForKey(message,"message");
		} finally {
			ec.unlock();
		}
	}

	public void addLesson() {
		save(false);
		if(session().valueForKey("message") != null)
			return;

		ec.lock();
		try {
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

	protected static void makeDateFromNum(EduLesson les) {
		NSArray args = new NSArray(new Object[] {les.course(),les.number()});
		EOQualifier qual = EOQualifier.qualifierWithQualifierFormat(
				"course = %@ AND number < %@ AND title = nil",args);
		NSArray sort = new NSArray(new EOSortOrdering ("number",EOSortOrdering.CompareDescending));
		EOFetchSpecification fs = new EOFetchSpecification(les.entityName(),qual,sort);
		fs.setFetchLimit(1);
		NSArray found = les.editingContext().objectsWithFetchSpecification(fs);
		if(found == null || found.count() == 0) {
			qual = EOQualifier.qualifierWithQualifierFormat(
					"course = %@ AND number > %@ AND title = nil",args);
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
				tmpEc = new EOEditingContext(ec.parentObjectStore());
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
				tmpEc = new EOEditingContext(ec.parentObjectStore());
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
	
	public boolean canAddTab() {
		if(currLesson() == null)
			return false;
		if(Various.boolForObject(session().valueForKeyPath("readAccess._edit.currLesson")))
			return false;
		int idx = lessonsList.indexOf(currPerPersonLink);
		return (idx > 0);
	}
	
	public String splitTitle() {
		if(lessonsList == null || currPerPersonLink == null)
			return null;
		int idx = lessonsList.indexOf(currPerPersonLink);
		if(idx > 0)
			return (String)application().valueForKeyPath("strings.Strings.LessonNoteEditor.addTab");
		if(idx == 0)
			return (String)application().valueForKeyPath("strings.Strings.LessonNoteEditor.removeTab");
		return null;
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
			tmpEc = new EOEditingContext(ec.parentObjectStore());
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
		try {
			tmpEc.saveChanges();
		} catch (RuntimeException e) {
			logger.log(WOLogLevel.WARNING,"Error splitting tab", new Object[]
			                            {session(),lesson,e});
			session().takeValueForKey(e.getMessage(), "message");
			tmpEc.revert();
		}
		_currTab = null;
		refresh();
	}

	public Tabs.GenericTab currTab() {
		return _currTab;
	}

	public void setCurrTab (Tabs.GenericTab tab) {
		if(tab != null) {
			_currTab = tab;
		}
		if (ec.hasChanges()) {
			ec.revert();
			refresh();
		} else {
			updateLessonList();
		}
		currPerPersonLink = null;
		//lessonsList = _currTab.lessonsInTab();
	}

	public WOComponent editSubgroup() {
//		String pageName = SettingsReader.stringForKeyPath(
				//"ui.subRegime.editCourseSubgroup","SubgroupEditor");
		WOComponent nextPage = pageWithName("SubgroupEditor");
		nextPage.takeValueForKey(course,"course");
		session().takeValueForKey(this,"pushComponent");
		session().takeValueForKey(Boolean.TRUE,"prolong");
		logger.logp(WOLogLevel.FINER,"LessonNoteEditor",
				"editSubgroup","Going to edit subgroup for course",new Object[] {session(),course});
		return nextPage;
	}

	//Regimes
	public static final int NORMAL = 1;
	public static final int LONGLIST = 0;
	public static final int BIGTABLE = 2;

	public int regime = NORMAL;
	//   public NSDictionary present;

	public NSArray presentTabs() {
		//		if(!(accessInterface().flagForKey("byDate") 
		//				&& accessInterface().flagForKey("oneLesson")))
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
		if(ec.hasChanges())
			ec.revert();
		refresh();

		student = null;
		selector = null;
		currPerPersonLink = null;
	}


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
		if(!(accessInterface().flagForKey("rightSide") && accessInterface().flagForKey("leftSide")))
			return false;
		return true;
	}

	public void moveLeft() {
		if(!accessInterface().flagForKey("rightSide")) {
			session().takeValueForKey(application().valueForKeyPath(
					"strings.Strings.messages.noAccess"),"message");
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
			session().takeValueForKey(application().valueForKeyPath(
					"strings.Strings.messages.noAccess"),"message");
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
	
	public String goRightTitle() {
		String key = "strings.Strings.LessonNoteEditor.";
		if(regime == LONGLIST)
			return (String)application().valueForKeyPath(key + "normal");
		else
			return (String)application().valueForKeyPath(key + "right");
	}

	public String goLeftTitle() {
		String key = "strings.Strings.LessonNoteEditor.";
		if(regime == BIGTABLE) {
			return (String)application().valueForKeyPath(key + "normal");
		} else {
			String result = (String)application().valueForKeyPath(key + "left");
			if(present != null) {
				result = result.substring(0,result.indexOf(' ') + 1);
				result = result + present.valueForKey("title");
			}
			return result;
		}
	}
	
	protected WOComponent srcMark () {
	   	NSMutableArray pathStack = (NSMutableArray)session().valueForKey("pathStack");
		WOComponent resultPage = null;
		if(pathStack != null && pathStack.count() > 0) {
			resultPage = (WOComponent)pathStack.lastObject();
			if(resultPage instanceof SrcMark) {
				pathStack.removeLastObject();
			} else if(pathStack.count() > 1) {
				resultPage = (WOComponent)pathStack.objectAtIndex(0);
				if(resultPage instanceof SrcMark) {
					pathStack.removeAllObjects();
				} else {
					resultPage = null;
				}
			} else {
				resultPage = null;
			}
		}
		if(resultPage == null) {
			resultPage = pageWithName("SrcMark");
		}
		return resultPage;
	}
	
	public WOActionResults chooseEduGroup() {
		WOComponent resultPage = srcMark();
		resultPage.takeValueForKey(course.eduGroup(), "currClass");
    	WOActionResults  result = (WOActionResults)resultPage.valueForKey("selectClass");
		if(result == null)
			result = resultPage;
		return result;
	}
	
	public WOActionResults chooseTeacher() {
		WOComponent resultPage = srcMark();
 		resultPage.takeValueForKey(course.teacher(), "currTeacher");
    	WOActionResults  result = (WOActionResults)resultPage.valueForKey("selectTeacher");
		if(result == null)
			result = resultPage;
		return result;
	}
}
