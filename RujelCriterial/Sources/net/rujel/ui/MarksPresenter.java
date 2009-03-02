// MarksPresenter.java: Class file for WO Component 'MarksPresenter'

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

import net.rujel.criterial.*;
import net.rujel.interfaces.EduCourse;
import net.rujel.interfaces.Person;
import net.rujel.reusables.*;
import net.rujel.base.MyUtility;
import com.webobjects.foundation.*;
import com.webobjects.appserver.*;
import com.webobjects.eocontrol.*;
import com.webobjects.eoaccess.*;
import java.math.BigDecimal;
import java.text.Format;
import java.util.logging.Logger;

public class MarksPresenter extends NotePresenter {
	public static final NSArray accessKeys = new NSArray (
			new String[] {"read","create","edit","delete","changeDate"});
	
	protected String entityName() {
		return "Mark";
	}

	public NamedFlags access() {
		if(_access == null && lesson() != null)
			_access = lesson().accessForAttribute("marks",accessKeys);
		if(_access == null)
			_access = DegenerateFlags.ALL_TRUE;
		return _access;
	}
	
    public String critItem;
	
	public void setCritItem(Object item) {
		if(item == null) {
			critItem = null;//"text";
		} else if(item instanceof String) {
			critItem = (String)item;
		} else if(item instanceof EOEnterpriseObject) {
			critItem =  (String)((EOEnterpriseObject)item).valueForKey("title");
		} else {
			critItem =  item.toString();
		}
		_mark = null;
	}
	/*
	public String critItem() {
		if(_critItem == null || _critItem instanceof String) {
			return (String)_critItem;
		} else if(_critItem instanceof EOEnterpriseObject) {
			return (String)((EOEnterpriseObject)_critItem).valueForKey("title");
		} else
			return _critItem.toString();
	}*/
	
    public MarksPresenter(WOContext context) {
        super(context);
    }
	
 	public Work lesson() {
		return (Work)super.lesson();
	}
	
	private Mark _mark;
	public Mark mark() {
		if(student() == null || lesson() == null || critItem == null || critItem.equals("text")) 
			return null;
		if(_mark == null) {
			_mark = lesson().markForStudentAndCriterion(student(),critItem);
		}
		return _mark;
	}
		
	//private NSArray _allCriteria;
	protected NSArray allCriteria() {
		NSArray _allCriteria = null;
		//if(_allCriteria == null) {
			if(lesson() == null) {
				EduCourse course = (EduCourse)valueForBinding("course");
				if(course != null) {
					_allCriteria = (NSArray)CriteriaSet.criteriaForCycle(course.cycle());
				}
			} else {
				_allCriteria = (NSArray)lesson().allCriteria();//.valueForKey("title");
			}
			if(_allCriteria == null) _allCriteria = NSArray.EmptyArray;
		//}
		return _allCriteria;
	}
	
	private NSArray _usedCriteria;
	public NSArray usedCriteria() {
		if(_usedCriteria == null) {
			if(hasBinding("initData")) {
				NSKeyValueCoding data = (NSKeyValueCoding)valueForBinding("initData");
				_usedCriteria = (NSArray)data.valueForKey("criteria");
			} else if(Various.boolForObject(valueForBinding("full"))) {
				_usedCriteria = allCriteria();
			} else {
				if(lesson() != null) {
					_usedCriteria = lesson().usedCriteria();
				} else {
					// TODO: remove this debug
					NSMutableDictionary args = new NSMutableDictionary(parent().name(),"parent");
					args.takeValueForKey(session(), "session");
					args.takeValueForKey(context().elementID(), "elementID");
					args.takeValueForKey(parent().valueForKey("currLesson"), "currLesson");
					args.takeValueForKey(parent().valueForBinding("present"), "present");
					args.takeValueForKey(parent().valueForKey("lessonsListing"), "lessonsListing");
					args.takeValueForKey(Thread.currentThread().getStackTrace(), "stackTrace");
					Logger.getAnonymousLogger().log(WOLogLevel.WARNING,"Lesson is null",args);
				}
			}
			if(_usedCriteria == null) _usedCriteria = NSArray.EmptyArray;
		}
		return _usedCriteria;
	}
	
	protected boolean hasValue() {
		if(lesson() == null ||  student() == null)
			return false;
		if(single()) {
			if(critItem == null)
				return super.hasValue();
			if (mark() == null)
				return false;
			EOEditingContext ec = mark().editingContext();
			if(ec == null)
				return false;
			EOGlobalID gid = mark().editingContext().globalIDForObject(mark());
			return (gid != null && !gid.isTemporary());
		}
		String activeCriterion = activeCriterion();
		if(activeCriterion == null || single()) {
			return true;//(anyMark() != null);
		} else if("text".equals(activeCriterion)) {
			return (noteForStudent() != null);
		}
		return (lesson().markForStudentAndCriterion(student(),activeCriterion) != null);
	}

	/*
	public Student student() {
		return (Student)valueForBinding("student");
	}
	 */
	public String markValue() {
		if(hasBinding("data")) {
			NSKeyValueCoding data = (NSKeyValueCoding)valueForBinding("data");
			Object result = data.valueForKey(critItem);
			return (result==null)?null:result.toString();
		}
		if(student() == null) {
			StringBuffer result = new StringBuffer("<big>").append(critItem).append("</big>");
			if(lesson() != null) {
				EOEnterpriseObject mask = lesson().criterMaskNamed(critItem);
				result.append("<br/><small>(");
				result.append((mask == null)?"?":mask.valueForKey("max"));
				result.append(")</small>");
			}
			return result.toString();
		}
		if(!access().flagForKey("read")) return "#";
        if (mark() == null) {
			if(Various.boolForObject(valueForBinding("full")) 
					&& lesson().usedCriteria().containsObject(critItem))
				return ".";
			else return null;
		}
		return mark().value().toString();
    }
	
    protected NSMutableDictionary identifierDictionary() {
		NSMutableDictionary ident = super.identifierDictionary();
		if(ident == null)
			return null;
		ident.takeValueForKey(lesson(),"work");
		//ident.removeObjectForKey("lesson");
		return ident;
    }
    	
	protected Mark anyMark() {
		Mark obj = mark();
		if(obj == null) {
			NSArray marks = lesson().marksForStudentOrCriterion(student(), (String)null);
			if(marks != null && marks.count() > 0)
				obj = (Mark)marks.objectAtIndex(0);
		}
		return obj;
	}
	
	public WOComponent archivePopup() {
		WOComponent result = pageWithName("ArchivePopup");
		result.takeValueForKey("MarksPresenter", "presenter");
		result.takeValueForKey(usedCriteria(), "keys");
		result.takeValueForKey(context().page(), "returnPage");
		NSMutableDictionary initData = identifierDictionary();
		if(_mark != null)
			result.takeValueForKey(_mark, "object");
		else
			result.takeValueForKey(initData,"identifierDictionary");
		StringBuffer description = new StringBuffer();
		if(lesson().theme() != null)
			description.append(lesson().theme()).append(" : ");
		description.append(Person.Utility.fullName(student(), true, 2, 2, 0));
		result.takeValueForKey(description.toString(), "description");
		NSMutableArray keys = (NSMutableArray)result.valueForKey("keys");
		keys.removeObject("text");
		initData.takeValueForKey(keys, "criteria");
		result.takeValueForKey(initData, "initData");
		return result;
	}
	
	public void setMarkValue(Object newMarkValue) {
        if(mark() == null) {
			if(newMarkValue == null) return;
			_mark = (Mark)EOUtilities.createAndInsertInstance(lesson().editingContext(),"Mark");
			lesson().addObjectToBothSidesOfRelationshipWithKey(_mark,"marks");
			_mark.setStudent(student());
			/*if(_critItem instanceof EOEnterpriseObject) {
				_mark.setCriterion((EOEnterpriseObject)_critItem);
				_mark.setCriterMask(lesson().criterMaskNamed(critItem));
			} else {*/
				_mark.setCriterionName((String)critItem);
			//}
		} else if (newMarkValue == null) {
			lesson().removeObjectFromBothSidesOfRelationshipWithKey(_mark,"marks");
			lesson().editingContext().deleteObject(_mark);
			archiveMarkValue(newMarkValue, critItem);
			_mark = null;
			return;
		}
        int value = 0;
        if(newMarkValue instanceof Number)
        	value = ((Number)newMarkValue).intValue();
        else
        	value = Integer.parseInt(newMarkValue.toString());
        if (mark().value() == null || value != mark().value().intValue()) {
			mark().setValue(new Integer(value));
			archiveMarkValue(newMarkValue, critItem);
		}
		/*if(mark().value() == null || mark().value().intValue() != newMarkValue.intValue()) {
			NSTimestamp today = (NSTimestamp)session().valueForKey("today");
			if(today == null) today = new NSTimestamp();
			mark().setDateSet(today);
		}*/
		
    }
	
	public void takeValuesFromRequest(WORequest aRequest, WOContext aContext) {
		super.takeValuesFromRequest(aRequest, aContext);
		if(_archive != null) {
			Mark[] marks = lesson().forPersonLink(student());
			if(marks != null) {
				for (int i = 0; i < marks.length; i++) {
					Mark mark = marks[i];
					if(mark == null) continue;
					String crit = (String)mark.criterion().valueForKey("title");
					_archive.takeValueForKey(mark.value(), '@' + crit);
				}
			}
			if(noteForStudent() != null)
				_archive.takeValueForKey(noteForStudent(), "@text");
		}
	}
	/*
    public void setNoteForStudent(String newNoteForStudent) {
        lesson().setNoteForStudent(newNoteForStudent,student());
    }
	
    public String noteForStudent() {
		if(student() == null || lesson() == null) return null;
        return lesson().noteForStudent(student());
    }*/
	
	protected FractionPresenter presenter() {
		if(lesson() != null && student() != null) {
			if(BigDecimal.ZERO.compareTo(lesson().weight()) == 0) {
				String key = SettingsReader.stringForKeyPath("edu.presenters.weightless", null);
				if(key != null) {
					NSTimestamp today = (NSTimestamp)session().valueForKey("today");
					if(today==null)today = new NSTimestamp();
					return BorderSet.fractionPresenterForTitleAndDate(
							EOSharedEditingContext.defaultSharedEditingContext(),key,today);
				}
			}
		}
		FractionPresenter result = (FractionPresenter)session().objectForKey("integralPresenter");
		if(result==null) {
			String key = SettingsReader.stringForKeyPath("edu.presenters.workIntegral","%");
			NSTimestamp today = (NSTimestamp)session().valueForKey("today");
			if(today==null)today = new NSTimestamp();
			result = BorderSet.fractionPresenterForTitleAndDate(
					EOSharedEditingContext.defaultSharedEditingContext(),key,today);
			session().setObjectForKey(result,"integralPresenter");
		}
		return result;
	}
	protected FractionPresenter colorPresenter() {
		if(lesson() != null) {
			if(BigDecimal.ZERO.compareTo(lesson().weight()) == 0) {
				String key = SettingsReader.stringForKeyPath("edu.presenters.weightlessColor", null);
				if(key != null) {
					NSTimestamp today = (NSTimestamp)session().valueForKey("today");
					if(today==null)today = new NSTimestamp();
					return BorderSet.fractionPresenterForTitleAndDate(
							EOSharedEditingContext.defaultSharedEditingContext(),key,today);
				}
			}
		}
		FractionPresenter result = (FractionPresenter)session().objectForKey("integralColor");
		if(result==null) {
			String key = SettingsReader.stringForKeyPath("edu.presenters.integralColor","color");
			NSTimestamp today = (NSTimestamp)session().valueForKey("today");
			if(today==null)today = new NSTimestamp();
			result = BorderSet.fractionPresenterForTitleAndDate(
					EOSharedEditingContext.defaultSharedEditingContext(),key,today);
			session().setObjectForKey(result,"integralColor");
		}
		return result;
	}

	protected String activeCriterion() {
		//Boolean single = (Boolean)valueForBinding("single");
		if(single())//(!hasBinding("single") ||Various.boolForObject(valueForBinding("single")))
			return null;
		String activeCriterion = (String)session().objectForKey("activeCriterion");
		if(activeCriterion == null)
			return null;
		FractionPresenter fractionPresenter = (FractionPresenter)session().
				objectForKey("integralPresenter");
		if(activeCriterion.equals(fractionPresenter.title()))
			return null;
		//if(allCriteria() != null &&  allCriteria().containsObject(activeCriterion))
			return activeCriterion;
		//return null;
	}
	
    public String integralTitle() {
		String activeCriterion = activeCriterion();
		if(student() == null) {
			//Boolean single = (Boolean)valueForBinding("single");
			if(single())//(Various.boolForObject(valueForBinding("single")))
				return presenter().title();
			return lessonTitle();
		}
		if(hasBinding("data")) {
			return "?";
		}
		if(!access().flagForKey("read")) return "#";
		if(activeCriterion == null) {
			BigDecimal integral = lesson().integralForStudent(student());
			if(integral != null) {
				return presenter().presentFraction(integral);
			} else {
				Mark mark = lesson().markForStudentWithoutCriterion(student());
				return (mark == null)?null:mark.value().toString();
			}
		} else if("text".equals(activeCriterion)) {
			return shortNoteForStudent();
		}
		setCritItem(activeCriterion);
		return (mark() == null)?null:mark().value().toString();
    }

    public String integralColor() {
    	if(student() == null)
    		return null;
    	if(activeCriterion() != null) return null;
    	if(hasBinding("data"))
    		return null;
    	if(!access().flagForKey("read")) return "#aaaaaa";
    	BigDecimal integral = lesson().integralForStudent(student());
    	if(integral == null) return null;
    	FractionPresenter pres = colorPresenter();
    	if(pres == FractionPresenter.PERCENTAGE)
    		return null;
    	return pres.presentFraction(integral);
    }

    /*
	public boolean isStateless() {
		return true;
	}
	public boolean synchronizesVariablesWithBindings() {
		return false;
	}
     */
    public void reset() {
    	super.reset();
    	_mark = null;
    	_usedCriteria = null;
    	//_allCriteria = null;
		critItem = null;
	}
/*	
	public boolean cantCreate() {
		Boolean deny = (Boolean)valueForBinding("denyCreation");
        return (deny != null && deny.booleanValue() && lesson().integralForStudent(student()) == null);
    }
	*/
	public boolean showNote() {
	//	if(student() == null) return true;
		if(!"text".equals(session().objectForKey("activeCriterion")))
			return false;
		
		if(single())//(Various.boolForObject(valueForBinding("single")))
			return false;
		return true;
	}
	
	public boolean deactivate() {
		if(single() && student() == null)
			return true;
		return super.deactivate();
	}
	
	public boolean deactivateIntegral() {
		if (Various.boolForObject(valueForBinding("readOnly")))
			return true;
		if(student() == null)
			return (single() || !hasBinding("selectAction"));
		if(!enableArchive)
			return !hasBinding("selectAction");
		return false;
	}

	/*
	public boolean deactivateNote() {
		critItem = "text";
		if (Various.boolForObject(valueForBinding("readOnly")))
			return true;
		if(forceArchives && noteForStudent() != null)
			return false;
		return isSelected();
	}
	*/
	
	public String tdStyle() {
		if(student() == null && lesson()!= null)
			return lesson().styleClass();
		return null;
	}
	
	public int len() {
		int len = super.len();
		if(len == 20)
			return 6;
		return len;
	}	
	protected static Format dateFormat = MyUtility.dateFormat();
	
	public void awake() {
		super.awake();
		synchronized (dateFormat) {
			dateFormat = MyUtility.dateFormat();
		}
	}
	
	public String markTitle() {
		if(student() == null) {
			if(lesson() != null && critItem != null) {
				EOEnterpriseObject mask = lesson().criterMaskNamed(critItem);
				if(mask == null)
					return "?";
				return (String)mask.valueForKeyPath("criterion.comment");
			} /*else {
				if(_critItem instanceof EOEnterpriseObject)
					return (String)((EOEnterpriseObject)_critItem).valueForKey("comment");
			}*/
			return null;
		} else {
			if(mark() == null) return null;
			if(!access().flagForKey("read")) return (String)application().valueForKeyPath("strings.Strings.messages.noAccess");
			synchronized (dateFormat) {
				return dateFormat.format(mark().dateSet());
			}
		}
	}
	
	public boolean cantCreateMark() {
		Boolean deny = (Boolean)valueForBinding("denyCreation");
        return (deny != null && deny.booleanValue() && student() != null && lesson() != null && mark() == null);
    }
	
	/*
	public String shortNoteForStudent() {
		if(student() == null) {
			String result = (String)application().valueForKeyPath("strings.RujelCriterial_Strings.text");
			return (result==null)?"text":result;
		}
		return super.shortNoteForStudent();
	}
	
//	public String 
    public boolean display() {
		return (Various.boolForObject(valueForBinding("full")) || !Various.boolForObject(valueForBinding("single")) || super.isSelected());
    }*/
	
	public String noteWidth() {
		if(student() != null && !isSelected()) return null;
		int len = (len() + 1);
		String result = "width:" + len + "ex;";
		return result;
	}

	public String title() {
		if(Various.boolForObject(valueForBinding("full")))
			return null;
		String title = null;
		if("text".equals(activeCriterion()))
			title = fullNoteForStudent();
		if(title == null)
			title = (String)valueForKeyPath("lesson.theme");
		if(title == null)
			return null;
		return WOMessage.stringByEscapingHTMLAttributeValue(title);
	}
	/*
	public String onClick() {
		String key = "checkRun";
		if(enableArchive && student() != null && (single() || hasValue()))
			key = "ajaxPopup";
		return (String)session().valueForKey(key);
    }
	
	
	public String fieldTag() {
		if(archiveOnly())
			return "span";
		else
			return "input";
	}
	
	public String fieldInit() {
		StringBuffer buf = new StringBuffer(15);
		if(archiveOnly()) {
			buf.append("class = \"backfield2\" onclick=\"");
			buf.append(session().valueForKey("ajaxPopup"));
			buf.append("\"");
		} else {
			buf.append("type =\"text\" value=\"");
			buf.append(markValue()).append("\" ");
			buf.append("onchange = \"checkChanges(this);\" onkeypress = \"return isNumberInput(event);\"");
		}
		return buf.toString();
	}*/
}
