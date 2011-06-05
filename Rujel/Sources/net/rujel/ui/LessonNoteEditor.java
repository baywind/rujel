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
import net.rujel.reusables.Tabs.GenericTab;

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
	public DateAgregate dateAgregate;

	private EOEditingContext ec;

	public Object item;

	public NSArray baseTabs;
	public NSArray allTabs;

	protected NSArray presentTabs;

	public LessonNoteEditor(WOContext context) {
		super(context);
		ec = new SessionedEditingContext(session());
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
//		student = null;
		currPerPersonLink = (PerPersonLink)EOUtilities.localInstanceOfObject(ec,lesson);
		session().setObjectForKey(lesson.date(), "recentDate");
	}
	
	public void setCurrPerPersonLink(PerPersonLink lesson) {
//		student = null;
		if(lesson instanceof EduLesson)
			session().setObjectForKey(((EduLesson)lesson).date(), "recentDate");
		if (ec.hasChanges()) {
			ec.revert();
			updateLessonList();
		}
		currPerPersonLink = lesson;
		if(student != null)
			selector = student;
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
		if (ec.hasChanges()) ec.revert();
		if(selector instanceof Student) {
			student = (Student)selector;
			if(currLesson() != null) {
				String ent = currLesson().entityName();
				// TODO: review this
				if(!ent.equals(present.valueForKey("entityName"))) {
					Enumeration enu = presentTabs.objectEnumerator();
					while (enu.hasMoreElements()) {
						NSKeyValueCoding pr = (NSKeyValueCoding) enu.nextElement();
						if(ent.equals(pr.valueForKey("entityName"))) {
							present = pr;
							refresh();
							break;
						}
					}
				}
			}
		} else {
			student = null;
		}
		regime = NORMAL;
		//refresh();
	}

	public static final NSArray accessKeys = new NSArray(
			new Object[] {"open","rightSide","leftSide","byDate","oneLesson"});

	protected NamedFlags _accessInterface;
	public NamedFlags accessInterface() {
		if(_accessInterface == null) {
			_accessInterface = (NamedFlags)session().valueForKeyPath(
					"readAccess.FLAGS.LessonNoteEditor");
			_accessInterface.setKeys(accessKeys);
		}
		return _accessInterface;
	}

	//protected NSMutableSet accessSchemes = new NSMutableSet();

	public String title() {
		StringBuilder buf = new StringBuilder(50);
		buf.append(course.eduGroup().name()).append(" : ");
		buf.append(course.cycle().subject()).append(" - ");
		if(course.teacher() != null)
			buf.append(Person.Utility.fullName(course.teacher(),true,2,1,1));
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

		Integer section = (Integer)session().valueForKeyPath("state.section.idx");
		if(section != null && !section.equals(course.valueForKeyPath("cycle.section"))) {
			section = (Integer)course.valueForKeyPath("cycle.section");
			if(section != null) {
				NSArray sects = (NSArray)application().valueForKeyPath("strings.sections.list");
				Enumeration enu = sects.objectEnumerator();
				while (enu.hasMoreElements()) {
					NSDictionary sect = (NSDictionary) enu.nextElement();
					if(section.equals(sect.valueForKey("idx"))) {
						session().takeValueForKeyPath(sect, "state.section");
						break;
					}
				}
			}
		}
		
		if(accessInterface().flagForKey("rightSide") && !accessInterface().flagForKey("leftSide"))
			regime = LONGLIST;
		else if(!accessInterface().flagForKey("rightSide") 
				&& accessInterface().flagForKey("leftSide"))
			regime = BIGTABLE;
		else
			regime = NORMAL;

		present = (NSKeyValueCoding)session().valueForKeyPath(
				"strings.Strings.LessonNoteEditor.consolidatedTab");
		NSMutableArray tabs = (NSMutableArray)session().valueForKeyPath("modules.presentTabs");
		if(tabs != null && tabs.count() > 0) {
			tabs.insertObjectAtIndex(present, 0);
			presentTabs = tabs.immutableClone();
		} else {
			presentTabs = new NSArray(present);
		}
		
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
		if(_currTab instanceof CustomTab.Tab)
			_currTab = (GenericTab) ((CustomTab.Tab) _currTab).params.valueForKey("parentTab");
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
			NSKeyValueCoding present, EOQualifier qualifier) {
		String entityName = (present == null)?null:(String)present.valueForKey("entityName");
		if(entityName == null)
			entityName = EduLesson.entityName;
		/*String courseAttribute = (String)valueForKeyPath("present.courseAttribute");
		if(courseAttribute == null)
			courseAttribute = "course";*/
		EOQualifier qual = new EOKeyValueQualifier("course",
				EOQualifier.QualifierOperatorEqual,course);
		EOQualifier extraQualifier = (present == null)?null:
			(EOQualifier)present.valueForKey("extraQualifier");
		if(extraQualifier != null || qualifier != null) {
			EOQualifier[] quals = new EOQualifier[] {qual,extraQualifier,qualifier};
			qual = new EOAndQualifier(new NSArray(quals));
		}
		EOFetchSpecification fs = new EOFetchSpecification(entityName,qual,EduLesson.sorter);
		NSArray prefetchPaths = (NSArray)present.valueForKey("prefetchPaths");
		if(prefetchPaths != null)
			fs.setPrefetchingRelationshipKeyPaths(prefetchPaths);
		return course.editingContext().objectsWithFetchSpecification(fs);
	}

	public void updateLessonList() {
		EOQualifier qualifier = null;
		if(currTab() != null) {
			qualifier = currTab().qualifier();
		}
		if(valueForKeyPath("currLesson.editingContext") != null) {
			ec.refaultObject(currLesson());
		}
		lessonsList = lessonListForCourseAndPresent(course, present, qualifier);
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
		save(student == null);
		//return this;
	}
	
	public WOActionResults saveComment() {
		try {
			ec.saveChanges();
			logger.log(WOLogLevel.EDITING,
					"Course comment modified",new Object[] {session(),course});
		} catch (Exception ex) {
			logger.log(WOLogLevel.WARNING,"Error saving comment",
					new Object[] {session(),currPerPersonLink,ex});
			session().takeValueForKey(ex.getMessage(),"message");
			ec.revert();
		}
		return null;
	}

	protected void save(boolean reset) {
		if(reset) {
			student = null;
			selector = currPerPersonLink;
		}
		boolean newLesson = (currPerPersonLink == null);
		if(ec.hasChanges()) {
			NSMutableSet changes = new NSMutableSet();
			NSArray objects = ec.insertedObjects();
			if(objects != null && objects.count() > 0) {
				changes.addObjectsFromArray((NSArray)objects.valueForKey("entityName"));
				Enumeration enu = objects.objectEnumerator();
				while (enu.hasMoreElements()) {
					EOEnterpriseObject obj = (EOEnterpriseObject) enu.nextElement();
					if(obj instanceof EduLesson) {
						currPerPersonLink = (EduLesson)obj;
						newLesson = true;
						break;
					}
				}
			} else {
				newLesson = false;
			}
			if(currPerPersonLink instanceof EOEnterpriseObject) {
				String entityName = ((EOEnterpriseObject)currPerPersonLink).entityName();
				String presentEntity = (String)valueForKeyPath("present.entityName");
				if(entityName.equals((presentEntity == null)?EduLesson.entityName:presentEntity))
					selector = currPerPersonLink;
			}
			objects = ec.updatedObjects();
			if(objects != null && objects.count() > 0)
				changes.addObjectsFromArray((NSArray)objects.valueForKey("entityName"));
			objects = ec.deletedObjects();
			if(objects != null && objects.count() > 0)
				changes.addObjectsFromArray((NSArray)objects.valueForKey("entityName"));
			try {
				if(currPerPersonLink != null)
					((EOEnterpriseObject)currPerPersonLink).validateForSave();
				if(Various.boolForObject(session().valueForKeyPath("readAccess.save.currLesson"))) {
					if(newLesson && currLesson() != null) {
						MyUtility.setNumberToNewLesson(currLesson());
					}
					ec.saveChanges();
					WOLogLevel level = WOLogLevel.EDITING;
					if(newLesson) {
						logger.log(level,"Created new lesson. " + changes,
								new Object[] {session(),currPerPersonLink});
						/*
							NSNotificationCenter.defaultCenter().postNotification(
									"Own created object",session().valueForKey(
									"user"),new NSDictionary(currPerPersonLink,"EO"));*/
					} else if(changes.count() > 0) {
						logger.log(level,"Lesson changed: " + changes,
								new Object[] {session(),currPerPersonLink});
					}
					if(currPerPersonLink != null) {
						NSMutableDictionary dict = new NSMutableDictionary(
								currPerPersonLink,"object");
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
				} else { // check access to edited data
					String message = (String)session().valueForKeyPath("readAccess.message");
					if(message != null)
						session().takeValueForKey(message, "message");
					logger.log(WOLogLevel.FINER,"Saving forbidden for lesson",
							new Object[] {session(),currPerPersonLink,message});
					ec.revert();
					if(newLesson) {
						currPerPersonLink = null;
						refresh();
					}
				}
				//session().takeValueForKey(Boolean.FALSE,"prolong");
			} catch (NSValidation.ValidationException vex) {
				logger.log(WOLogLevel.FINE,"Failed to save lesson",
						new Object[] {session(),currPerPersonLink,vex.toString()});
				session().takeValueForKey(vex.getMessage(),"message");
				reset = false;
			/*	if(!newLesson) {
					ec.revert();
					ec.refaultObject(currLesson());
				}*/
			} catch (Exception ex) {
				logger.log(WOLogLevel.WARNING,"Failed to save lesson",
						new Object[] {session(),currPerPersonLink,ex});
				session().takeValueForKey(ex.getMessage(),"message");
				ec.revert();
				if(currPerPersonLink instanceof EOEnterpriseObject &&
						((EOEnterpriseObject)currPerPersonLink).editingContext() != ec) {
					currPerPersonLink = null;
				}
			}
		}// ec.hasChanges
		if(reset)
			currPerPersonLink = null;
		else if(student != null)
			selector = student;
	}

	public void delete() {
		if(currPerPersonLink == selector)
			selector = null;
		EOQualifier qual = new EOKeyValueQualifier(
				"course",EOQualifier.QualifierOperatorEqual,course);
		EOFetchSpecification fs = new EOFetchSpecification(
				currLesson().entityName(),qual,EduLesson.sorter);
		NSArray allLessons = ec.objectsWithFetchSpecification(fs);
		WOLogLevel level = WOLogLevel.EDITING;
		if(currLesson().notes().count() > 0)
			level = WOLogLevel.MASS_EDITING;
		ec.lock();
		try {
			if(ec.insertedObjects().contains(currLesson())) { //just inserted
				level = WOLogLevel.EDITING;
				logger.log(level,"Undo lesson creation: ",new Object[] {session(),currLesson()});
				ec.revert();
				NSMutableArray tmp = lessonsList.mutableClone();
				tmp.removeObject(currLesson());
				lessonsList = tmp.immutableClone();
				currPerPersonLink = null;
			} else { //delete from middle
				int idx = allLessons.indexOf(currLesson());
				if(idx >= allLessons.count())
					throw new RuntimeException("Something wrong when deleting lesson");
				currLesson().validateForDelete();
				Number number = currLesson().number();
				int num = (number == null)?0:number.intValue();
				if(number == null) {
					EduLesson lesson = (EduLesson)allLessons.objectAtIndex(0);
					number = lesson.number();
					if(number != null)
						num = number.intValue() + idx;
				}
				if(number == null) {
					EduLesson lesson = (EduLesson)allLessons.lastObject();
					number = lesson.number();
					if(number != null)
						num = number.intValue() + idx - allLessons.count() + 1;
				}
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
			ec.revert();
		} catch (Exception ex) {
			logger.log(WOLogLevel.WARNING,"Deletion failed: ",
					new Object[] {session(),currLesson(),ex});
			String message = (String)application().valueForKeyPath(
					"strings.Strings.messages.error") + " : " + application().valueForKeyPath(
							"strings.Strings.messages.cantDelete") + " : " + ex;
			session().takeValueForKey(message,"message");
			ec.revert();
		} finally {
			ec.unlock();
		}
	}

	public EduLesson addLesson() {
		save(false);
		if(session().valueForKey("message") != null)
			return null;

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
		return currLesson();
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
				tmpEc = new EOEditingContext(ec.rootObjectStore());
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
				tmpEc = new EOEditingContext(ec.rootObjectStore());
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
	
	public Boolean cantAddTab() {
		Boolean splitCreate = splitCreate();
		if(splitCreate == null)
			return Boolean.TRUE;
		if(!(_currTab instanceof BaseTab.Tab) && lessonsList.indexOf(currPerPersonLink) <= 0)
			return Boolean.TRUE;
		if(splitCreate.booleanValue())
			return (Boolean)session().valueForKeyPath("readAccess._create.BaseTab");
		else
			return (Boolean)session().valueForKeyPath("readAccess._delete.BaseTab");
	}
	
	protected Boolean splitCreate() {
		if(lessonsList == null || currLesson() == null || currLesson().number().intValue() <= 1)
			return null;
		int idx = lessonsList.indexOf(currPerPersonLink);
		if(_currTab instanceof BaseTab.Tab) {
			if(idx < 0)
				return null;
			return Boolean.valueOf(idx > 0);
		}
		if(idx <=0 )
			return null;
		return Boolean.TRUE;
//		BaseTab tab = BaseTab.tabForLesson(currLesson(), false);
//		return Boolean.valueOf(tab == null || 
//				!tab.firstLessonNumber().equals(currLesson().number()));
	}
	
	public String splitSign() {
		Boolean splitCreate = splitCreate();
		if(splitCreate == null)
			return null;
		if(splitCreate.booleanValue())
			return "+";
		else
			return "-";
	}
	
	public String splitTitle() {
		Boolean splitCreate = splitCreate();
		if(splitCreate == null)
			return null;
		if(splitCreate.booleanValue())
			return (String)application().valueForKeyPath(
					"strings.Strings.LessonNoteEditor.addTab");
		else
			return (String)application().valueForKeyPath(
					"strings.Strings.LessonNoteEditor.removeTab");
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
			tmpEc = new EOEditingContext(ec.rootObjectStore());
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
		else
			student = null;
		if(ec.hasChanges())
			ec.revert();
		refresh();

//		student = null;
		selector = student;
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
//		if(hideLeft() && currPerPersonLink != null) return false;
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
//		updateLessonList();
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
//		updateLessonList();
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
			return (String)application().valueForKeyPath(key + "left");
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
    	NotesPage.resetAddons(session());
		return resultPage;
	}
	
	public WOActionResults chooseEduGroup() {
		WOComponent resultPage = srcMark();
		resultPage.takeValueForKey(course.eduGroup(), "currClass");
		return resultPage;
	}
	
	public WOActionResults chooseTeacher() {
		WOComponent resultPage = srcMark();
 		resultPage.takeValueForKey(course.teacher(), "currTeacher");
		return resultPage;
	}
	
	public String customTabClass() {
		if(_currTab instanceof CustomTab.Tab)
			return "selection";
		return "grey";
	}
	
	public WOActionResults customTab() {
		WOComponent result = CustomTab.getPopup(this, null);
		result.takeValueForKey(ec, "ec");
		result.takeValueForKey(present, "present");
		return result;
	}
	
	public Boolean hideCustomTab() {
		if(present == null || present.valueForKey("params") == null)
			return Boolean.TRUE;
		return Boolean.FALSE;
	}
	
	public String groupHover() {
		try {
			return (String)valueForKeyPath("course.eduGroup.hover");
		} catch (NSKeyValueCoding.UnknownKeyException e) {
			return null;
		}
	}

}
