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
//import net.rujel.markarchive.MarkArchive;
import net.rujel.reusables.*;
import net.rujel.base.MyUtility;
import com.webobjects.foundation.*;
import com.webobjects.appserver.*;
import com.webobjects.eocontrol.*;
import com.webobjects.eoaccess.*;
import java.math.BigDecimal;
import java.text.Format;

public class MarksPresenter extends NotePresenter {
	public static final NSArray accessKeys = new NSArray (new String[] {"read","create","edit","delete","changeDate"});

	private boolean enableArchive = false;
	
	private NamedFlags _access;
	public NamedFlags access() {
		if(_access == null)
			_access = lesson().accessForAttribute("marks",accessKeys);
		if(_access == null)
			_access = DegenerateFlags.ALL_TRUE;
		return _access;
	}
	
    protected Object _critItem;
	
	public void setCritItem(Object item) {
		_critItem = item;
		_mark = null;
	}
	
	public String critItem() {
		if(_critItem == null || _critItem instanceof String) {
			return (String)_critItem;
		} else if(_critItem instanceof EOEnterpriseObject) {
			return (String)((EOEnterpriseObject)_critItem).valueForKey("title");
		} else
			return _critItem.toString();
	}
	
    public MarksPresenter(WOContext context) {
        super(context);
    }
	
 	public Work lesson() {
		return (Work)valueForBinding("lesson");
	}
	
	private Mark _mark;
	public Mark mark() {
		if(student() == null) return null;
		if(_mark == null) {
			_mark = lesson().markForStudentAndCriterion(student(),critItem());
		}
		return _mark;
	}
	
	public boolean single() {
		return (Various.boolForObject(valueForBinding("full")) || Various.boolForObject(valueForBinding("single")));
	}
	
	private NSArray _allCriteria;
	public NSArray allCriteria() {
		if(_allCriteria == null) {
			if(lesson() == null) {
				EduCourse course = (EduCourse)valueForBinding("course");
				if(course != null) {
					_allCriteria = (NSArray)CriteriaSet.criteriaForCycle(course.cycle());//.valueForKey("title");
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
			if(Various.boolForObject(valueForBinding("full"))) {
				_usedCriteria = allCriteria();
			} else {
				_usedCriteria = lesson().usedCriteria();
			}
			if(_usedCriteria == null) _usedCriteria = NSArray.EmptyArray;
		}
		return _usedCriteria;
	}
	
	public boolean isSelected() {
		if(student() == null) return false;
		if((mark() != null)? !access().flagForKey("edit") : !access().flagForKey("create")) return false;
		else return super.isSelected();
	}
	/*
	public Student student() {
		return (Student)valueForBinding("student");
	}
	 */
	public String markValue() {
		if(student() == null) {
			StringBuffer result = new StringBuffer("<big>").append(critItem()).append("</big>");
			if(lesson() != null) {
				EOEnterpriseObject mask = lesson().criterMaskNamed(critItem());
				result.append("<br/><small>(");
				result.append((mask == null)?"?":mask.valueForKey("max"));
				result.append(")</small>");
			}
			return result.toString();
		}
		if(!access().flagForKey("read")) return "#";
        if (mark() == null) {
			if(Various.boolForObject(valueForBinding("full")) && lesson().usedCriteria().containsObject(critItem()))
				return ".";
			else return null;
		}
		return mark().value().toString();
    }
	
	protected EOEnterpriseObject _archive;
	protected void archiveMarkValue(Object value, String name) {
		if(!enableArchive) return;
		if(_archive == null) {
			EOEditingContext ec = lesson().editingContext();
			_archive = EOUtilities.createAndInsertInstance(ec,"MarkArchive");
			/*NSDictionary key = EOUtilities.primaryKeyForObject(ec, _mark);
			if(key != null && key.count() > 0) {*/
				_archive.takeValueForKey(_mark, "objectIdentifier");//setObjectIdentifier(_mark);
			/*} else {
				key = EOUtilities.destinationKeyForSourceObject(ec, _mark, "student");
				NSMutableDictionary fullKey = key.mutableClone();
				key = EOUtilities.destinationKeyForSourceObject(ec, _mark, "work");
				fullKey.addEntriesFromDictionary(key);
				_archive.setIdentifierFromDictionary(_mark.entityName(), fullKey);
			}*/
		}
		_archive.takeValueForKey(value, '@' + name);
	}
	
	public void setMarkValue(Integer newMarkValue) {
        if(mark() == null) {
			if(newMarkValue == null) return;
			_mark = (Mark)EOUtilities.createAndInsertInstance(lesson().editingContext(),"Mark");
			lesson().addObjectToBothSidesOfRelationshipWithKey(_mark,"marks");
			_mark.setStudent(student());
			if(_critItem instanceof EOEnterpriseObject) {
				_mark.setCriterion((EOEnterpriseObject)_critItem);
				_mark.setCriterMask(lesson().criterMaskNamed(critItem()));
			} else {
				_mark.setCriterionName((String)_critItem);
			}
		} else if (newMarkValue == null) {
			lesson().removeObjectFromBothSidesOfRelationshipWithKey(_mark,"marks");
			lesson().editingContext().deleteObject(_mark);
			archiveMarkValue(newMarkValue, critItem());
			_mark = null;
			return;
		}
        if (mark().value() == null || newMarkValue.intValue() != mark().value().intValue()) {
			mark().setValue(newMarkValue);
			archiveMarkValue(newMarkValue, critItem());
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
			for (int i = 0; i < marks.length; i++) {
				Mark mark = marks[i];
				if(mark == null) continue;
				String critItem = (String)mark.criterion().valueForKey("title");
				_archive.takeValueForKey(mark.value(), '@' + critItem);
			}
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
			if(BigDecimal.ZERO.equals(lesson().weight())) {
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
			if(BigDecimal.ZERO.equals(lesson().weight())) {
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
		FractionPresenter fractionPresenter = (FractionPresenter)session().
				objectForKey("integralPresenter");
		if(activeCriterion == null || activeCriterion.equals(fractionPresenter.title()))
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
		
		if(!access().flagForKey("read")) return "#";
		if(activeCriterion == null) {
			BigDecimal integral = lesson().integralForStudent(student());
			if(integral != null) {
				return presenter().presentFraction(integral);
			} else {
				Mark mark = lesson().markForStudentWithoutCriterion(student());
				return (mark == null)?null:mark.value().toString();
			}
		}
		
		setCritItem(activeCriterion);
		return (mark() == null)?null:mark().value().toString();
    }

    public String integralColor() {
    	if(student() == null)
    		return null;
    	if(activeCriterion() != null) return null;
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
    	_allCriteria = null;
		_access = null;
		_archive = null;
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
	
	public String tdStyle() {
		if(single() || integralColor() != null) {//Various.boolForObject(valueForBinding("single")) 
			return null;
		}
		if(student() == null) {
			if(super.isSelected())
				return "selection";
			else {
				return lesson().styleClass();
			}
		}
		return (String)valueForBinding("defaultStyle");
	}
	
	protected int len() {
		Number maxlen = (Number)valueForBinding("maxlen");
		return (maxlen == null)?6:maxlen.intValue();
	}
	
	protected static Format dateFormat = MyUtility.dateFormat();
	
	public void awake() {
		super.awake();
		synchronized (dateFormat) {
			dateFormat = MyUtility.dateFormat();
		}
		enableArchive = SettingsReader.boolForKeyPath("markarchive.Mark", false);
	}
	
	public String markTitle() {
		if(student() == null) {
			if(_critItem instanceof String && lesson() != null) {
				EOEnterpriseObject mask = lesson().criterMaskNamed((String)_critItem);
				if(mask == null)
					return "?";
				return (String)mask.valueForKeyPath("criterion.comment");
			} else {
				if(_critItem instanceof EOEnterpriseObject)
					return (String)((EOEnterpriseObject)_critItem).valueForKey("comment");
			}
			return null;
		} else {
			if(mark() == null) return null;
			if(!access().flagForKey("read")) return (String)application().valueForKeyPath("strings.messages.noAccess");
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
			String result = (String)application().valueForKeyPath("extStrings.RujelCriterial-Strings.text");
			return (result==null)?"text":result;
		}
		return super.shortNoteForStudent();
	}*/
	
//	public String 
    public boolean display() {
		return (Various.boolForObject(valueForBinding("full")) || !Various.boolForObject(valueForBinding("single")) || super.isSelected());
    }
	
	public String noteWidth() {
		if(student() != null) return null;
		int len = (len() * 2) /3;
		return "width:" + len + "em;";
	}

	public String title() {
		if(Various.boolForObject(valueForBinding("full")))
			return null;
		String title = (String)valueForKeyPath("lesson.theme");
		if(title == null)
			return null;
		return WOMessage.stringByEscapingHTMLAttributeValue(title);
	}
}
