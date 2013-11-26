package net.rujel.criterial;

import java.util.Enumeration;

import com.webobjects.appserver.WOApplication;
import com.webobjects.eoaccess.EOUtilities;
import com.webobjects.eocontrol.EOAndQualifier;
import com.webobjects.eocontrol.EOEditingContext;
import com.webobjects.eocontrol.EOEnterpriseObject;
import com.webobjects.eocontrol.EOFetchSpecification;
import com.webobjects.eocontrol.EOKeyValueQualifier;
import com.webobjects.eocontrol.EOQualifier;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSMutableArray;
import com.webobjects.foundation.NSMutableDictionary;
import com.webobjects.foundation.NSTimestamp;

import net.rujel.base.BaseLesson;
import net.rujel.base.IndexRow;
import net.rujel.base.Indexer;
import net.rujel.base.SettingsBase;
import net.rujel.base.BaseLesson.NoteDelegate;
import net.rujel.interfaces.EOPeriod;
import net.rujel.interfaces.EduLesson;
import net.rujel.interfaces.Student;
import net.rujel.reusables.SessionedEditingContext;
import net.rujel.reusables.SettingsReader;

public class WorkNoteDelegate implements NoteDelegate {

	private Work work;
	private EduLesson lsn;
	private boolean searched;
	private boolean moveMarks;
	private boolean noMarks;
	private boolean moveNotes;
	private NSArray indexRows;
	private Integer courseMax;
	public static Integer specFlags = (Integer)WorkType.specTypes.valueForKey("onLesson");

	
	
	public WorkNoteDelegate(EduLesson lesson) {
		super();
		lsn = lesson;
//		work = findWork(lesson);
		searched = false;
		moveMarks = (SettingsBase.numericSettingForCourse(
				"noLessonMarks", lsn.course(), lsn.editingContext(), 0) == 0);
		moveNotes = (SettingsBase.numericSettingForCourse(
				"lessonWorkNotes", lsn.course(), lsn.editingContext(), 0) != 0);
	}

	public String lessonNoteForStudent(Student student) {
		if(!moveNotes && !moveMarks)
			return null;
		if(work(false) == null)
			return null;
		String note = work.noteForStudent(student);
		if(!moveMarks)
			return note; 
		Mark mark = work.markForStudentAndCriterion(student, new Integer(0));
		if(mark == null)
			return note;
		if(note == null)
			return mark.value().toString();
		return mark.value().toString() + ": " + note;
	}

	public boolean setLessonNoteForStudent(String note, Student student) {
		if(!moveNotes && !moveMarks)
			return false;
		if(note != null)
			note = note.trim();
		Integer num = null;
		Integer max = courseMax;
		if(max == null && work(false) != null) {
			NSArray criterMask = work.criterMask();
			if(criterMask != null && criterMask.count() == 1) {
				EOEnterpriseObject mask = (EOEnterpriseObject)criterMask.objectAtIndex(0);
				Integer cr = (Integer)mask.valueForKey("criterion");
				if(cr.intValue() == 0) {
					max = (Integer)mask.valueForKey("max");
				}
			} else if(criterMask != null && criterMask.count() > 1) {
				noMarks = true;
			}
		}
		int arcLevel = 0;
		boolean useNote = false;
		if(moveMarks && !noMarks) {
			if(indexRows == null) {
				CriteriaSet critSet = (work!=null)?work.critSet():
					CriteriaSet.critSetForCourse(lsn.course());
				if(critSet != null) {
					EOEnterpriseObject criter = critSet.criterionForNum(new Integer(0));
					if(criter != null) {
						if(courseMax == null)
							courseMax = (Integer)criter.valueForKey("dfltMax");
						Indexer indexer = (Indexer)criter.valueForKey("indexer");
						if(indexer != null)
							indexRows = indexer.indexRows();
					} else {
						noMarks = true;
					}
				}
				if(courseMax == null)
					courseMax = SettingsBase.numericSettingForCourse("CriterlessMax",
							lsn.course(), lsn.editingContext());
				if(max == null)
					max = courseMax;
				if(indexRows == null)
					indexRows = NSArray.EmptyArray;
			} // prepare indexRows
			if(indexRows.count() > 0) {
				Enumeration enu = indexRows.objectEnumerator();
				rows:
				while (enu.hasMoreElements()) {
					IndexRow row = (IndexRow) enu.nextElement();
					if(note.startsWith(row.value())) {
						int idx = row.value().length();
						if(idx == note.length()) {
							num = row.idx();
							note = null;
							break rows;
						}
						char c = note.charAt(idx);
						if(c != ':' && !Character.isWhitespace(c))
							continue;
						num = row.idx();
						do {
							idx++;
							if(idx >= note.length()) {
								note = null;
								break rows;
							}
							c = note.charAt(idx);
						} while (Character.isWhitespace(c) || c == ':');
						note = note.substring(idx);
						break;
					}
				}
			} // check indexRows
			else if(!noMarks && note != null && note.length() > 0 
					&& Character.isDigit(note.charAt(0))) {
				int idx = 1;
				while (idx < note.length()) {
					if(!Character.isDigit(note.charAt(idx)))
						break;
					idx++;
				}
				if(idx < note.length()) {
					char c = note.charAt(idx);
					if(Character.isWhitespace(c) || c == ':')
						try {
							num = new Integer(note.substring(0,idx));
						} catch (Exception e) {}
				} else {
					try {
						num = new Integer(note);
					} catch (Exception e) {}
				}
				if(num != null) {
					if(max != null && num.intValue() <= max.intValue()) {
						while (idx < note.length()) {
							if(Character.isLetterOrDigit(note.charAt(idx)))
								break;
							idx++;
						}
						if(idx < note.length())
							note = note.substring(idx);
						else
							note = null;
					} else {
						num = null;
					}
				}
			} // numeric mark
			if(num != null) {
				work(true);
				NSArray criterMask = work.criterMask();
				Mark mark = null;
				if(criterMask == null || criterMask.count() == 0) {
					EOEnterpriseObject mask = EOUtilities.createAndInsertInstance(
							lsn.editingContext(), "CriterMask");
					mask.takeValueForKey(new Integer(0), "criterion");
					work.addObjectToBothSidesOfRelationshipWithKey(mask, Work.CRITER_MASK_KEY);
					mask.takeValueForKey(max, "max");
					criterMask = work.criterMask();
				} else {
					Mark[] marks = work.forPersonLink(student);
					if(marks != null) 
						mark = marks[0];
				}
				if(mark == null) {
					mark = (Mark)EOUtilities.createAndInsertInstance(lsn.editingContext(),"Mark");
					mark.setCriterion(new Integer(0));
					mark.setStudent(student);
					work.addObjectToBothSidesOfRelationshipWithKey(mark,Work.MARKS_KEY);
					arcLevel = 1;
				} else if(!num.equals(mark.value())) {
					arcLevel = 2;
				}
				mark.setValue(num);
			} else if(work != null) { // num == null
				Mark mark = work.markForStudentAndCriterion(student, new Integer(0));
				if(mark != null) {
					work.removeObjectFromBothSidesOfRelationshipWithKey(mark, Work.MARKS_KEY);
					lsn.editingContext().deleteObject(mark);
					work.nullify();
					arcLevel = 3;
				}
			}
			if(arcLevel > 1) {
				NSMutableDictionary dict = new NSMutableDictionary(student,"student");
				dict.takeValueForKey(lsn.course(), "course");
				dict.takeValueForKey(work, "lesson");
				EOEditingContext ec = lsn.editingContext();
				NSMutableArray toSave = (NSMutableArray)ec.userInfoForKey("toSave");
				if(toSave == null) {
					toSave = new NSMutableArray(dict);
					ec.setUserInfoForKey(toSave, "toSave");
				} else {
					toSave.addObject(dict);
				}
			}
			if(work != null || moveNotes) {
				arcLevel = setWorkNote(note, student, arcLevel);
				useNote = true;
			}
		} // if(moveMarks)
		else if(moveNotes) {
			arcLevel = setWorkNote(note, student, arcLevel);
			useNote = true;
		}
		boolean arc = (arcLevel > 0) && (SettingsReader.boolForKeyPath("markarchive.Mark", 
				SettingsReader.boolForKeyPath("markarchive.archiveAll", false)));
		if(arc) {
			NSMutableDictionary ident = new NSMutableDictionary(Mark.ENTITY_NAME,"entityName");
			ident.takeValueForKey(work,"work");
			ident.takeValueForKey(student, "student");
			ident.takeValueForKey(lsn.course(), "eduCourse");
			EOEnterpriseObject _archive = EOUtilities.createAndInsertInstance(
					lsn.editingContext(),"MarkArchive");
			_archive.takeValueForKey(new Integer(arcLevel), "actionType");
			if(num != null || arcLevel > 2)
				_archive.takeValueForKey(num, '@' + work.criterName(new Integer(0)));
			if(note != null || arcLevel > 2)
				_archive.takeValueForKey(note, "@text");
			try {
				String reason = ((SessionedEditingContext)lsn.editingContext()).session().context().
					request().stringFormValueForKey("reasonText");
				if(reason != null)
					_archive.takeValueForKey(reason, "reason");
			} catch (Exception e) {}
			_archive.takeValueForKey(ident, "identifierDictionary");
		}
		if(work != null && !lsn.number().equals(work.number()))
			work.setNumber(lsn.number());
		return useNote;
	}
	
	private int setWorkNote(String note,Student student,int arcLevel) {
		if(work(note != null) == null)
			return arcLevel;
		EOEnterpriseObject nto = BaseLesson.lessonNoteforStudent(work, student);
		String prev = (nto == null)?null:(String)nto.valueForKey("note");
//		work.setNoteForStudent(note, student);
		if(note == null) {
			if(prev != null) {
				work.removeObjectFromBothSidesOfRelationshipWithKey(nto, "notes");
				work.editingContext().deleteObject(nto);
				if(arcLevel == 0)
					return 3;
			}
		} else if(arcLevel < 2) {
			if(nto == null) {
				nto = work._newNote();
				nto.addObjectToBothSidesOfRelationshipWithKey(student, "student");
				nto.takeValueForKey(note, "note");
				return 1;
			}
			else if(!note.equals(prev)) {
				nto.takeValueForKey(note, "note");
				return 2;
			}
		}
		return arcLevel;
	}

	protected Work work(boolean create) {
		if(work != null) {
			if(work.editingContext() != null &&
					specFlags.equals(work.workType().dfltFlags()) &&
					EOPeriod.Utility.compareDates(work.date(), lsn.date()) == 0) {
				return work;
			} else {
				work = null;
				searched = false;
				noMarks = false;
				indexRows = null;
			}
		}
		if(!searched) {
			work = findWork(lsn);
			searched = true;
		}
		if(work != null || !create)
			return work;
		work = (Work)EOUtilities.createAndInsertInstance(
				lsn.editingContext(), Work.ENTITY_NAME);
		work.setWorkType(WorkType.getSpecType(lsn.editingContext(), "onLesson"));
		work.addObjectToBothSidesOfRelationshipWithKey(lsn.course(), "course");
		NSTimestamp date = lsn.date();
		work.setDate(date);
		work.setAnnounce(date);
		work.setTheme((String)WOApplication.application().valueForKeyPath(
				"strings.RujelCriterial_Strings.spesTypes.onLesson.typeName"));
		work.setNumber(lsn.number());
		return work;
	}
	
	public static Work findWork(EduLesson lesson) {
		EOEditingContext ec = lesson.editingContext();
		WorkType type = WorkType.getSpecType(ec, "onLesson");
		EOQualifier[] quals = new EOQualifier[3];
		quals[0] = new EOKeyValueQualifier("course",
				EOQualifier.QualifierOperatorEqual,lesson.course());
		quals[1] = new EOKeyValueQualifier(Work.WORK_TYPE_KEY,
				EOQualifier.QualifierOperatorEqual, type);
		quals[2] = new EOKeyValueQualifier(Work.DATE_KEY,
				EOQualifier.QualifierOperatorEqual,lesson.date());
		quals[1] = new EOAndQualifier(new NSArray(quals));
		EOFetchSpecification fs = new EOFetchSpecification(Work.ENTITY_NAME,
				quals[1],EduLesson.sorter);
		NSArray found = ec.objectsWithFetchSpecification(fs);
		if(found != null && found.count() > 0) {
			NSMutableDictionary byNum = new NSMutableDictionary();
			for (int i = 0; i < found.count(); i++) {
				Work work = (Work)found.objectAtIndex(i);
				if(work.number().equals(lesson.number()))
					return work;
				byNum.setObjectForKey(work, work.number());
			}
			quals[1] = new EOKeyValueQualifier(Work.NUMBER_KEY,
					EOQualifier.QualifierOperatorNotEqual,lesson.number());
			quals[1] = new EOAndQualifier(new NSArray(quals));
			fs = new EOFetchSpecification(lesson.entityName(),quals[1],EduLesson.sorter);
			NSMutableArray works = found.mutableClone();
			found = ec.objectsWithFetchSpecification(fs);
			if(found == null || found.count() == 0)
				return (Work)works.objectAtIndex(0);
			NSMutableArray lessons = new NSMutableArray();
			for (int i = 0; i < found.count(); i++) {
				EduLesson ls = (EduLesson)found.objectAtIndex(i);
				Integer num = ls.number();
				Work work = (Work)byNum.removeObjectForKey(num);
				if(work == null) {
					lessons.addObject(ls);
				} else if(byNum.count() == 0) {
					return null;
				} else {
					works.removeObject(work);
				}
			}
			for (int i = 0; i < works.count(); i++) {
				Work work = (Work)works.objectAtIndex(i);
				if(lessons.count() <= i)
					return work;
				EduLesson ls = (EduLesson)lessons.objectAtIndex(i);
				if(ls.number().compareTo(lesson.number()) > 0)
					return work;
			}
		}
		return null;
	}
}
