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
import net.rujel.base.SettingsBase;

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
		if(_access == null) {
			NSMutableDictionary presenterCache = (NSMutableDictionary)valueForBinding("presenterCache");
			if(presenterCache != null)
				_access = (NamedFlags)presenterCache.valueForKey("markAccess");
			if(_access == null) {
				if(_access == null && lesson() != null)
					_access = lesson().accessForAttribute("marks",accessKeys);
				if(_access == null)
					_access = DegenerateFlags.ALL_TRUE;
				if(presenterCache != null)
					presenterCache.takeValueForKey(_access, "markAccess");
			}
		}
		return _access;
	}
	
    public Object critItem;
	
/*	public void setCritItem(Object item) {
		if(item == null) {
			critItem = null;//"text";
		} else if(item instanceof Integer) {
			critItem = (Integer)item;
		} else if(item instanceof EOEnterpriseObject) {
			critItem =  (Integer)((EOEnterpriseObject)item).valueForKey("criterion");
		} else {
			critItem = null;
		}
		_mark = null;
	}*/
	
	protected Integer critItem() {
		if(critItem instanceof Integer)
			return (Integer)critItem;
		return (Integer)NSKeyValueCoding.Utility.valueForKey(critItem, "criterion");
	}
	
    public MarksPresenter(WOContext context) {
        super(context);
    }
	
 	public Work lesson() {
		return (Work)super.lesson();
	}
	
	private Mark _mark;
	public Mark mark() {
		if(student() == null || lesson() == null || critItem() == null) 
			return null;
		if(_mark == null || !_mark.criterion().equals(critItem())) {
			_mark = lesson().markForStudentAndCriterion(student(),critItem());
		}
		return _mark;
	}
		
//	private NSArray _allCriteria;
	protected NSArray allCriteria() {
		NSArray _allCriteria = null;
		if(_allCriteria == null) {
			if(lesson() == null) {
				EduCourse course = (EduCourse)valueForBinding("course");
				if(course != null) {
					_allCriteria = CriteriaSet.criteriaForCourse(course);
//						Work.allCriteria(CriteriaSet.maxCriterionForCourse(course));
				}
			} else {
				_allCriteria = (NSArray)lesson().allCriteria();//.valueForKey("title");
			}
			if(_allCriteria == null) _allCriteria = NSArray.EmptyArray;
		}
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
					args.takeValueForKey(context().senderID(), "senderID");
					args.takeValueForKey(context().request().requestHandlerPath(), "requestHandlerPath");
					args.takeValueForKey(student(), "student");
					args.takeValueForKey(valueForKeyPath("context.page.currLesson"), "currLesson");
					args.takeValueForKey(valueForKeyPath("context.page.course"), "currCourse");
					args.takeValueForKey(parent().valueForBinding("present"), "present");
					args.takeValueForKey(parent().valueForBinding("single"), "single");
					args.takeValueForKey(parent().valueForKey("lessonsListing"), "lessonsListing");
					args.takeValueForKey(
							new NSArray(Thread.currentThread().getStackTrace()), "stackTrace");
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
		Integer activeCriterion = activeCriterion();
		if(activeCriterion == null || single()) {
			return true;//(anyMark() != null);
		} else if(activeCriterion.intValue() < 0) {
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
			String key = (lesson() == null)?CriteriaSet.titleForCriterion(critItem().intValue()):
				lesson().criterName(critItem());
			Object result = data.valueForKey(key);
			return (result==null)?null:result.toString();
		}
		if(student() == null) {
			StringBuffer result = new StringBuffer("<big>");
			if(critItem instanceof NSKeyValueCoding)
				result.append(NSKeyValueCoding.Utility.valueForKey(critItem, "title"));
			else if(lesson() != null) 
				result.append(lesson().criterName(critItem()));
			else
				result.append(CriteriaSet.titleForCriterion(critItem().intValue()));
			result.append("</big>");
			if(lesson() != null) {
				EOEnterpriseObject mask = lesson().getCriterMask(critItem());
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
	
	public WOComponent archivePopup() {
		WOComponent result = pageWithName("ArchivePopup");
		result.takeValueForKey("MarksPresenter", "presenter");
		result.takeValueForKey(context().page(), "returnPage");
		NSMutableDictionary initData = identifierDictionary();
		if(_mark != null)
			result.takeValueForKey(_mark, "object");
		else
			result.takeValueForKey(initData,"identifierDictionary");
		result.takeValueForKey(usedCriteria(), "keys");
		StringBuffer description = new StringBuffer();
		if(lesson() != null && lesson().theme() != null)
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
			_mark.setStudent(student());
			_mark.setCriterion(critItem());
			lesson().addObjectToBothSidesOfRelationshipWithKey(_mark,"marks");
		} else if (newMarkValue == null) {
			lesson().removeObjectFromBothSidesOfRelationshipWithKey(_mark,"marks");
			lesson().editingContext().deleteObject(_mark);
			archiveMarkValue(newMarkValue, lesson().criterName(critItem()));
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
			archiveMarkValue(newMarkValue, lesson().criterName(critItem()));
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
					String crit = lesson().criterName(mark.criterion());
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

	protected FractionPresenter colorPresenter() {
		if(lesson() == null)
			return FractionPresenter.NONE;
		FractionPresenter result = null;
		String key = "integralColor";
		if(BigDecimal.ZERO.compareTo(lesson().weight()) == 0)
			key = "weightlessColor";
		NSMutableDictionary presenterCache = (NSMutableDictionary)valueForBinding("presenterCache");
		if(presenterCache != null) {
			result = (FractionPresenter)presenterCache.valueForKey(key);
			if(result != null)
				return result;
		}
		EOEditingContext ec = lesson().editingContext(); 
		String key2 = "presenters." + key;
		EOEnterpriseObject setting = SettingsBase.settingForCourse(key2,
				lesson().course(), ec);
		if(setting != null) {
			Integer pKey = (Integer)setting.valueForKey(SettingsBase.NUMERIC_VALUE_KEY);
			key2 = (String)setting.valueForKeyPath(SettingsBase.TEXT_VALUE_KEY);
			if (pKey != null) {
				result = (BorderSet)EOUtilities.objectWithPrimaryKeyValue(
						ec, BorderSet.ENTITY_NAME, pKey);
			} else if(key2 != null) {
				result = BorderSet.fractionPresenterForTitle(ec, key2);
			} else {
				result = BorderSet.fractionPresenterForTitle(ec, "color");
			}
		} else {
			result = BorderSet.fractionPresenterForTitle(ec, "color");
		}
		if(presenterCache != null)
			presenterCache.takeValueForKey(result, key);
		return result;
	}

	protected Integer activeCriterion() {
		//Boolean single = (Boolean)valueForBinding("single");
		if(single())//(!hasBinding("single") ||Various.boolForObject(valueForBinding("single")))
			return null;
		Integer activeCriterion = (Integer)session().objectForKey("activeCriterion");
		if(activeCriterion == null)
			return null;
		return activeCriterion;
	}
	
    public String integralTitle() {
		Integer activeCriterion = activeCriterion();
		if(lesson() == null) {
			return "?";
		}
		if(student() == null) {
			if(single())
				return lesson().integralPresenter().title();
			return lessonTitle();
		}
		if(!access().flagForKey("read")) return "#";
		if(activeCriterion == null) {
			if(lesson().noCriteria()) {
				Mark mark = lesson().markForStudentAndCriterion(student(),new Integer(0));
				return (mark == null)?null:mark.value().toString();
			}
			return lesson().integralForStudent(student(),lesson().integralPresenter());
		} else if(activeCriterion.intValue() < 0) {
			return shortNoteForStudent();
		}
		critItem = activeCriterion;
		return (mark() == null)?null:mark().value().toString();
    }

    public String integralColor() {
    	if(student() == null)
    		return (String)valueForKeyPath("lesson.color");
    	if(activeCriterion() != null) return null;
    	if(hasBinding("data"))
    		return null;
    	if(!access().flagForKey("read")) return "#aaaaaa";
    	BigDecimal integral = lesson().integralForStudent(student());
    	if(integral == null) return null;
    	FractionPresenter pres = colorPresenter();
    	if(pres == null || pres == FractionPresenter.PERCENTAGE)
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
//    	_allCriteria = null;
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
	
	public String tdStyle() {
		if(student() == null && lesson()!= null)
			return lesson().styleClass();
		return null;
	}
	*/
	
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
				if(critItem instanceof Integer) {
					CriteriaSet critSet = lesson().critSet();
					if(critSet == null)
						return null;
					EOEnterpriseObject criterion = critSet.criterionForNum((Integer)critItem);
					if(criterion == null)
						return "?";
					return (String)criterion.valueForKey("comment");
				} else {
					return (String)NSKeyValueCoding.Utility.valueForKey(critItem, "comment");
				}
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
//		if(Various.boolForObject(valueForBinding("full")))
//			return null;
		String title = null;
		if(student() == null){ 
			title = (String)valueForKeyPath("lesson.theme");
		} else if(activeCriterion() != null) {
			if(activeCriterion().intValue() < 0) {
				title = fullNoteForStudent();
			} else {
				critItem = activeCriterion();
				return markTitle();
			}
		} else {
			Mark[] marks = lesson().forPersonLink(student());
			if (marks != null) {
				NSTimestamp date = null;
				for (int i = 0; i < marks.length; i++) {
					if (marks[i] != null
							&& (date == null || date
									.compare(marks[i].dateSet()) < 0))
						date = marks[i].dateSet();
				}
				if (date != null)
					synchronized (dateFormat) {
						return dateFormat.format(date);
					}
			}
		}
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
