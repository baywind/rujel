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

import net.rujel.interfaces.*;
import net.rujel.reusables.*;

import com.webobjects.foundation.*;
import com.webobjects.appserver.*;
import java.util.GregorianCalendar;

public class NotePresenter extends WOComponent {
	protected NamedFlags _access;

	protected boolean enableArchive = false;
	
	public NamedFlags access() {
		if(_access == null) {
			_access = (NamedFlags)session().valueForKeyPath("readAccess.FLAGS.BaseNote");
			/*
			if(lesson() instanceof UseAccessScheme) {
				_access = ((UseAccessScheme)lesson()).accessForAttribute("notes",null);
			} else {
				UserPresentation user = (UserPresentation)session().valueForKey("user");
				if(user != null) {
					try {
						int lvl = user.accessLevel("BaseNote");
						_access = new ImmutableNamedFlags(lvl,UseAccess.accessKeys);
					}  catch (AccessHandler.UnlistedModuleException e) {
						_access = DegenerateFlags.ALL_TRUE;
					}
				}
			}*/
		}	
		if(_access == null)
			_access = DegenerateFlags.ALL_TRUE;
		return _access;
	}
	
    public NotePresenter(WOContext context) {
        super(context);
    }
	
	public EduLesson lesson() {
		EduLesson result = (EduLesson)valueForBinding("lesson");
		if(result == null && hasBinding("initData")) {
			NSKeyValueCoding data = (NSKeyValueCoding)valueForBinding("initData");
			result = (EduLesson)data.valueForKey("lesson");
		}
		return result;
	}
	
	public boolean isSelected() {
		if((noteForStudent() != null)? !access().flagForKey("edit") : !access().flagForKey("create")) return false;
		Boolean is = (Boolean)valueForBinding("isSelected");
		return (is != null && is.booleanValue());
	}
	
	public Student student() {
		Student result = (Student)valueForBinding("student");
		if(result == null && hasBinding("initData") && 
				(hasBinding("data") || Various.boolForObject(valueForBinding("isSelected")))) {			
			NSKeyValueCoding data = (NSKeyValueCoding)valueForBinding("initData");
			result = (Student)data.valueForKey("student");
		}
		return result;
	}
	
	public String tdStyle() {
		if(isSelected())
			return "selection";
		else
			return (String)valueForBinding("defaultStyle");
    }
	
    public void setNoteForStudent(String newNoteForStudent) {
        lesson().setNoteForStudent(newNoteForStudent,student());
    }

    protected String _noteForStudent;
    public String noteForStudent() {
    	if(_noteForStudent == null) {
     		if(student() == null || lesson() == null)
    			return null;
    		_noteForStudent = lesson().noteForStudent(student());
    	}
    	return _noteForStudent;
    }

    protected int len() {
    	Number maxlen = (Number)valueForBinding("maxlen");
		if (maxlen != null)
			return maxlen.intValue();
		if(Various.boolForObject(valueForBinding("single")))
			return 30;
		return 3;
	}
	/*
	public String align() {
		if(len() > 10) return "left";
		else return "center";
	}
	*/
	public String shortNoteForStudent() {
		String theNote = noteForStudent();
		if (theNote == null)
			return null;
		if(!access().flagForKey("read")) return "#";
		if(theNote.length() <= len())
			return theNote;
		String url = application().resourceManager().urlForResourceNamed("text.png","RujelBase",null,context().request());
		return "<img src=\"" + url + "\" alt=\"" + theNote + "\" height=\"16\" width=\"16\">";
	}
	
	public String fullNoteForStudent() {
		if(student() == null)
			return (lesson() == null)?null:lesson().theme();
		String theNote = noteForStudent();
		if (theNote == null)
			return null;
		if(!access().flagForKey("read")) return (String)application().valueForKeyPath("strings.messages.noAccess");
		if(theNote.length() <= len())
			return null;
		return theNote;
	}
	
	public boolean deactivate() {
		return (isSelected() || !hasBinding("selectAction"));
    }
	
	public String onClick() {
		if(deactivate())
			return null;
		return (String)session().valueForKey((enableArchive)?"ajaxPopup":"checkRun");
    }
    public boolean cantCreate() {
		Boolean deny = (Boolean)valueForBinding("denyCreation");
        return (deny != null && deny.booleanValue() && noteForStudent() == null);
    }
	public String lessonTitle() {
		return titleForLesson(lesson());
	}
	
	public static String titleForLesson(EduLesson lesson) {
		if(lesson==null)return "@";
        String result = lesson.title();
		if(result != null)
			return result;
		GregorianCalendar cal = new GregorianCalendar();
		cal.setTime(lesson.date());
		int day = cal.get(GregorianCalendar.DAY_OF_MONTH);
		int month = cal.get(GregorianCalendar.MONTH);
		NSArray months = (NSArray)WOApplication.application().valueForKeyPath("strings.presets.monthShort");
		return String.format("<small>%1$s</small><br/><b>%2$d</b>",months.objectAtIndex(month),day);
	}

	public String style() {
		boolean single = Various.boolForObject(valueForBinding("single"));
		StringBuffer buf = new StringBuffer(30);
		int len = (len() * 2) / 3;
		buf.append("width:").append(len).append("em;text-align:");
		if(noteForStudent() != null) {
			if(single && noteForStudent().length() >= len())
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
		_noteForStudent = null;
	}
	
	public void awake() {
		super.awake();
		enableArchive = SettingsReader.boolForKeyPath("markarchive.BaseNote", false);
	}
}
