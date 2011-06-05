// NotePresenter.java: Class file for WO Component 'NotePresenter'

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

//import net.rujel.base.BaseLesson;
import net.rujel.base.BaseLesson;
import net.rujel.interfaces.*;
import net.rujel.reusables.*;

import com.webobjects.eoaccess.EOUtilities;
import com.webobjects.eocontrol.EOEditingContext;
import com.webobjects.eocontrol.EOEnterpriseObject;
import com.webobjects.foundation.*;
import com.webobjects.appserver.*;

import java.util.GregorianCalendar;

public class NotePresenter extends WOComponent {
	protected NamedFlags _access;

	protected boolean enableArchive = false;
	protected boolean forceArchives = false;
	protected String entityName() {
		return "BaseNote";
	}
	
	public NamedFlags access() {
		if(_access == null) {
//			_access = (NamedFlags)valueForBinding("access");
//			if(_access != null)
//				return _access;
//			NSMutableDictionary presenterCache = (NSMutableDictionary)valueForBinding("presenterCache");
//			if(presenterCache != null)
//				_access = (NamedFlags)presenterCache.valueForKey("noteAccess");
//			if(_access == null) {
				_access = (NamedFlags)session().valueForKeyPath("readAccess.FLAGS.BaseNote");
//				if(presenterCache != null)
//					presenterCache.takeValueForKey(_access, "noteAccess");
//			}
			if(_access == null)
				_access = DegenerateFlags.ALL_TRUE;
		}	
		return _access;
	}
	
	public boolean noAccess() {
		return (hasValue())? !access().flagForKey("edit")
				: !access().flagForKey("create");
	}
	
    public NotePresenter(WOContext context) {
        super(context);
    }
	
    protected EduLesson _lesson;
	public EduLesson lesson() {
		if(_lesson == null)
			_lesson = (EduLesson)valueForBinding("lesson");
		if(_lesson == null && hasBinding("initData")) {
			NSKeyValueCoding data = (NSKeyValueCoding)valueForBinding("initData");
			_lesson = (EduLesson)data.valueForKey("lesson");
		}
		return _lesson;
	}
	
	protected boolean hasValue() {
		if (noteForStudent() == null)
			return false;
		EOEnterpriseObject note = BaseLesson.lessonNoteforStudent(lesson(), student());
		return (note != null &&
				!note.editingContext().globalIDForObject(note).isTemporary());
	}

	public boolean single() {
		return (Various.boolForObject(valueForBinding("full"))
				|| Various.boolForObject(valueForBinding("single"))
				|| hasBinding("initData"));
	}
	
	public boolean isSelected() {
		if(student() == null)
			return false;
		if (Various.boolForObject(valueForBinding("readOnly")))
			return false;
		if(hasBinding("archive"))
			return true;
		if (!Various.boolForObject(valueForBinding("isSelected")))
			return false;
		if(!forceArchives)
			return true;
		return !hasValue();
	}
	
	protected Student _student;
	public Student student() {
		if(_student == null)
			_student = (Student)valueForBinding("student");
		if(_student == null && hasBinding("initData") && 
				(hasBinding("data") || hasBinding("archive"))) {			
			NSKeyValueCoding data = (NSKeyValueCoding)valueForBinding("initData");
			_student = (Student)data.valueForKey("student");
		}
		return _student;
	}
	
	public String tdStyle() {
		if(Various.boolForObject(valueForBinding("isSelected")))
			return "selection";
		return null;
    }
	
    public void setNoteForStudent(String note) {
    	archiveMarkValue(note, "text");
       lesson().setNoteForStudent(note,student());
        _noteForStudent = note;
    }

    protected String _noteForStudent;
    public String noteForStudent() {
    	if(_noteForStudent == null) {
    		if(hasBinding("data")) {
    			NSKeyValueCoding data = (NSKeyValueCoding)valueForBinding("data");
    			_noteForStudent = (String)data.valueForKey("text");
    		} else {
    			if(student() == null || lesson() == null)
    				return null;
    			_noteForStudent = lesson().noteForStudent(student());
    		}
    	}
    	return _noteForStudent;
    }

    public int len() {
    	Number maxlen = (Number)valueForBinding("maxlen");
    	if (maxlen == null && hasBinding("initData")) {
    		NSKeyValueCoding data = (NSKeyValueCoding)valueForBinding("initData");
    		maxlen = (Number)data.valueForKey("maxlen");
    	}
    	if (maxlen == null)
    		maxlen = new Integer((single())?20:3);
    	return maxlen.intValue();
    }
	/*
	public String align() {
		if(len() > 10) return "left";
		else return "center";
	}
	*/
	public String shortNoteForStudent() {
		if(student() == null) {
			String result = (String)application().valueForKeyPath("strings.Reusables_Strings.dataTypes.text");
			if(result == null)
				result = "text";
			if(len() < 5)
				result = "<small>" + result + "</small>";
			return result;
		}
		String theNote = noteForStudent();
		if (theNote == null)
			return null;
		if(!access().flagForKey("read")) return "#";
		if(theNote.length() <= len())
			return theNote;
		String url = application().resourceManager().urlForResourceNamed("text.png","RujelBase",null,context().request());
		return "<img src=\"" + url + "\" alt=\"txt\" height=\"16\" width=\"16\">";
	}
	
	public String fullNoteForStudent() {
		if(student() == null)
			return (lesson() == null)?null:lesson().theme();
		String theNote = noteForStudent();
		if (theNote == null)
			return null;
		if(!access().flagForKey("read")) return (String)application().valueForKeyPath("strings.Strings.messages.noAccess");
		if(theNote.length() <= len())
			return null;
		return WOMessage.stringByEscapingHTMLAttributeValue(theNote);
	}
	
	protected EOEnterpriseObject _archive;
	protected void archiveMarkValue(Object value, String name) {
		if(!enableArchive) return;
		if(_archive == null)
			_archive = (EOEnterpriseObject)valueForBinding("archive");
		if(_archive == null) {
			EOEditingContext ec = lesson().editingContext();
			if(ec == null)
				return;
			_archive = EOUtilities.createAndInsertInstance(ec,"MarkArchive");
			_archive.takeValueForKey(identifierDictionary(), "identifierDictionary");
			if(hasBinding("archive"))
				setValueForBinding(_archive, "archive");
		}
		_archive.takeValueForKey(value, '@' + name);
	}

	protected NSMutableDictionary identifierDictionary() {
    	if(student() == null || lesson() == null)
    		return null;
		NSMutableDictionary ident = new NSMutableDictionary(entityName(),"entityName");
		ident.takeValueForKey(lesson(),"lesson");
		ident.takeValueForKey(student(), "student");
		ident.takeValueForKey(lesson().course(), "eduCourse");
		ident.takeValueForKey(lesson().editingContext(), "editingContext");
		if(lesson().forPersonLink(student()) == null)
			ident.takeValueForKey(Boolean.TRUE, "isEmpty");
		return ident;
    }

	public WOComponent archivePopup() {
		WOComponent result = pageWithName("ArchivePopup");
		result.takeValueForKey("NotePresenter", "presenter");
		result.takeValueForKey(context().page(), "returnPage");
		//result.takeValueForKey(BaseLesson.lessonNoteforStudent(lesson(), student()), "object");
		NSDictionary initData = identifierDictionary();
		initData.takeValueForKey(new Integer(12), "maxlen");
		result.takeValueForKey(initData,"identifierDictionary");
		StringBuilder description = new StringBuilder();
		if(lesson().theme() != null)
			description.append(lesson().theme()).append(" : ");
		description.append(Person.Utility.fullName(student(), true, 2, 2, 0));
		result.takeValueForKey(description.toString(), "description");
		return result;
	}

	public WOActionResults selectAction() {
		if(enableArchive && student() != null)
			return archivePopup();
		return (WOActionResults)valueForBinding("selectAction");
	}
	
	public boolean deactivate() {
		if (Various.boolForObject(valueForBinding("readOnly")))
			return true;
		if(forceArchives && hasValue())
			return false;
		return (isSelected() || !hasBinding("selectAction"));
    }
	
	public String onClick() {
		String key = "checkRun";
		if(enableArchive && student() != null)
			key = "ajaxPopup";
		return (String)session().valueForKey(key);
    }
	
    public boolean cantCreate() {
		Boolean deny = (Boolean)valueForBinding("denyCreation");
        return (deny != null && deny.booleanValue() && noteForStudent() == null);
    }
	public String lessonTitle() {
		return titleForLesson(lesson());
	}
	
	public static String titleForLesson(NSKeyValueCoding lesson) {
		if(lesson==null)return "@";
        String result = (String)lesson.valueForKey("title");
		if(result != null)
			return result;
		GregorianCalendar cal = new GregorianCalendar();
		cal.setTime((NSTimestamp)lesson.valueForKey("date"));
		int day = cal.get(GregorianCalendar.DAY_OF_MONTH);
		int month = cal.get(GregorianCalendar.MONTH);
		NSArray months = (NSArray)WOApplication.application().valueForKeyPath("strings.Reusables_Strings.presets.monthShort");
		return String.format("<small>%1$s</small><br/><b>%2$d</b>",months.objectAtIndex(month),day);
	}

	public String style() {
		//boolean single = Various.boolForObject(valueForBinding("single"));
		StringBuilder buf = new StringBuilder(30);
		//int len = (len() * 2) / 3;
		buf.append("width:").append(len() +1).append("ex;text-align:");
		if(noteForStudent() != null) {
			if(single() && noteForStudent().length() >= len())
				buf.append("left;overflow:hidden;");
			else
				buf.append("center;");
			buf.append("white-space:nowrap;");
		} else {
			buf.append("center;");
		}
		return buf.toString();
	}

    public String td() {
        if(student() == null) return "th";
		return "td";
    }

	public boolean isStateless() {
		return true;
	}
	public boolean synchronizesVariablesWithBindings() {
        return false;
	}
	
	public void reset() {
		super.reset();
		_access = null;
		_archive = null;
		_lesson = null;
		_noteForStudent = null;
		_student = null;
	}
	
	public void awake() {
		super.awake();
		enableArchive = (SettingsReader.boolForKeyPath("markarchive." + entityName(), false));
		forceArchives = (enableArchive && !hasBinding("initData")
				&& SettingsReader.boolForKeyPath("markarchive.forceArchives", false));
	}
}
