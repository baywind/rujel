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
import net.rujel.base.Indexer;
import net.rujel.base.SettingsBase;

import com.webobjects.foundation.*;
import com.webobjects.appserver.*;
import com.webobjects.eocontrol.*;
import com.webobjects.eoaccess.*;

import java.math.BigDecimal;
import java.util.logging.Logger;

public class MarksPresenter extends NotePresenter {
	
	protected String entityName() {
		return "Mark";
	}

	public NamedFlags access() {
		if(_access == null) {
//			_access = (NamedFlags)valueForBinding("access");
//			if(_access != null)
//				return _access;
//			NSMutableDictionary presenterCache = (NSMutableDictionary)valueForBinding(
//					"presenterCache");
//			if(presenterCache != null)
//				_access = (NamedFlags)presenterCache.valueForKey("markAccess");
//			if(_access == null) {
				if(_access == null && lesson() != null)
					_access = (NamedFlags)session().valueForKeyPath("readAccess.FLAGS.Mark");
				if(_access == null)
					_access = DegenerateFlags.ALL_TRUE;
//				if(presenterCache != null)
//					presenterCache.takeValueForKey(_access, "markAccess");
//			}
		}
		return _access;
	}
	
    public Object critItem;
	
	protected Integer critItem() {
		if(critItem == null)
			return null;
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
			} else {
				if(lesson() != null) {
					_usedCriteria = lesson().usedCriteria();
				} else if(Various.boolForObject(valueForBinding("full"))) {
					_usedCriteria = allCriteria();
					return _usedCriteria;
				} else {
					_usedCriteria = NSArray.EmptyArray;
					Logger.getAnonymousLogger().log(WOLogLevel.WARNING,"Lesson is null",
							new Object[] {session(),context().elementID(),
							new NullPointerException()});
				}
				if(Various.boolForObject(valueForBinding("full"))) {
					if(_usedCriteria.count() != 1 ||
							((Integer)_usedCriteria.objectAtIndex(0)).intValue() != 0)
					_usedCriteria = allCriteria();
				}
			}
			if(_usedCriteria == null) _usedCriteria = NSArray.EmptyArray;
		}
		return _usedCriteria;
	}
	
	public String colspan() {
		if(!Various.boolForObject(valueForBinding("full")))
			return null;
		if(usedCriteria().count() > 1)
			return null;
		NSArray all = allCriteria();
		if(all.count() > 1)
			return Integer.toString(all.count());
		return null;
	}
	
	protected boolean hasValue() {
		if(lesson() == null ||  student() == null)
			return false;
		if(single()) {
			if(critItem == null)
				return super.hasValue();
			if (mark() == null || mark().value() == null)
				return false;
			EOEditingContext ec = mark().editingContext();
			if(ec == null)
				return false;
			EOGlobalID gid = ec.globalIDForObject(mark());
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
				CriteriaSet cs = lesson().critSet();
				String max = null;
				if(cs != null) {
					Indexer idx = cs.indexerForCriter(critItem());
					if(idx != null) {
						Integer mIndex = idx.maxIndex();
						if(mIndex != null) {
							max = idx.valueForIndex(mIndex.intValue(), null);
							if(max.length() > 3 && 
									!(max.charAt(0)=='&' && max.length() < 10))
								return result.toString();
						}
					}
				}
				if(max == null) {
					EOEnterpriseObject mask = lesson().getCriterMask(critItem());
					if(mask != null) {
						Integer maxInt = (Integer)mask.valueForKey("max");
						if(maxInt != null)
						max = maxInt.toString();
					}
				}
				if(max != null) {
					result.append("<br/><small>(");
					result.append(max);
					result.append(")</small>");
				}
			}
			return result.toString();
		}
		if(!access().flagForKey("read")) return "#";
        if (mark() == null) {
			if(Various.boolForObject(valueForBinding("full")) 
					&& lesson().usedCriteria().containsObject(critItem) && !isSelected())
				return ".";
			else return null;
		}
		return mark().present();
    }
	
	public String onkeypress() {
		CriteriaSet set = lesson().critSet();
		if(set != null && set.indexerForCriter(critItem()) != null) {
			return null;
		}
		return "return isNumberInput(event);";
	}
	
    protected NSMutableDictionary identifierDictionary() {
		NSMutableDictionary ident = super.identifierDictionary();
		if(ident == null)
			return null;
		ident.takeValueForKey(lesson(),"work");
		if(Various.boolForObject(ident.valueForKey("isEmpty")) && 
				lesson().forPersonLink(student()) != null)
			ident.removeObjectForKey("isEmpty");
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
			if(_archive != null)
				_archive.takeValueForKey(new Integer(3), "actionType");
			_mark = null;
			return;
		}
        boolean archive = (mark().value() == null);
        boolean notNew = !archive;
        if(newMarkValue instanceof Number) {
        	int value = ((Number)newMarkValue).intValue();
        	archive = (archive || value != mark().value().intValue());
        	if(archive)
        		mark().setValue(new Integer(value));
        } else {
        	Boolean tmp = mark().setPresent(newMarkValue.toString());
        	if(tmp == null) {
        		if(hasBinding("archive")) {
        			String message = (String)session().valueForKeyPath(
							"strings.RujelCriterial_Strings.messages.illegalMark");
					message = String.format(message, lesson().criterName(critItem()));
        			session().takeValueForKey(message, "message");
//        			lesson().editingContext().revert();
        		}
    			return;
        	} else {
        		archive = tmp.booleanValue();
        	}
        }
        if(archive) {
			archiveMarkValue(newMarkValue, lesson().criterName(critItem()));
			if(notNew && shouldUpdateArchive())
				_archive.takeValueForKey(new Integer(2), "actionType");
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
					_archive.takeValueForKey(mark.present(), '@' + crit);
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
			return new Integer(0);
		Integer activeCriterion = (Integer)session().objectForKey("activeCriterion");
		if(activeCriterion == null)
			return new Integer(0);
		return activeCriterion;
	}
	
	public Boolean hideIntegralCell() {
		if(valueForBinding("initData") != null)
			return Boolean.TRUE;
		if(lesson() == null)
			return Boolean.FALSE;
		if(Various.boolForObject(valueForBinding("full")))
			return Boolean.FALSE;
		Boolean hide = (Boolean)session().objectForKey("hideMarkless");
		if(hide != null && !hide.booleanValue())
			return hide;
		if(activeCriterion().intValue() < 0) {
			if(hide != null && 
					(lesson().notes() == null || lesson().notes().count() == 0))
				return hide;
			return Boolean.FALSE;
		} else {
			return new Boolean(lesson().criterMask() == null 
					|| lesson().criterMask().count() == 0);
		}
	}
	
    public String integralTitle() {
		if(lesson() == null) {
			return "?";
		}
		if(student() == null) {
			if(single())
				return lesson().integralPresenter().title();
			return lessonTitle();
		}
		if(!access().flagForKey("read")) return "&otimes;";
		if(single())
			return lesson().integralForStudent(student(),lesson().integralPresenter());
		Integer activeCriterion = activeCriterion();
		if(activeCriterion.intValue() < 0)
			return shortNoteForStudent();
		if(lesson().usedCriteria().count() == 0)
			return null;
		Mark mark = lesson().markForStudentAndCriterion(student(),activeCriterion);
		if(mark != null)
			return mark.present();
		if(activeCriterion.intValue() == 0) {
			String result = lesson().integralForStudent(student(),lesson().integralPresenter());
			if(result == null) {
				String note = shortNoteForStudent();
				if(note != null) {
					if(note.charAt(0) != '<')
						note = "<em class = \"dimtext\">" + note + "</em>";
					result = note;
				} else if(lesson().isCompulsory() && lesson().hasWeight())
					result = ".";
			}
			return result;
		}
		if(lesson().usedCriteria().contains(activeCriterion) && 
				(lesson().forPersonLink(student()) != null ||
				(lesson().isCompulsory() && lesson().hasWeight())))
			return ".";
		return null;
    }

    public String integralColor() {
    	if(student() == null)
    		return (String)valueForKeyPath("lesson.color");
    	if(activeCriterion().intValue() != 0) return null;
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

    public void reset() {
    	super.reset();
    	_mark = null;
    	_usedCriteria = null;
//    	_allCriteria = null;
		critItem = null;
	}

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
	
	public int len() {
		int len = super.len();
		if(len == 20)
			return 6;
		return len;
	}	
/*	protected static Format dateFormat = MyUtility.dateFormat();
	
	public void awake() {
		super.awake();
		synchronized (dateFormat) {
			dateFormat = MyUtility.dateFormat();
		}
	}*/
	
	public String markTitle() {
		if(student() == null) {
			if(lesson() != null && critItem != null) {
				if(critItem instanceof Integer) {
					CriteriaSet critSet = lesson().critSet();
					if(critSet == null)
						return null;
					EOEnterpriseObject criterion = critSet.criterionForNum(
							(Integer)critItem);
					if(criterion == null)
						return "?";
					return (String)criterion.valueForKey("comment");
				} else {
					return (String)NSKeyValueCoding.Utility.valueForKey(
							critItem, "comment");
				}
			} /*else {
				if(_critItem instanceof EOEnterpriseObject)
					return (String)((EOEnterpriseObject)_critItem).valueForKey(
					"comment");
			}*/
			return null;
		} else {
			if(hasBinding("data")) return null;
			if(mark() == null) return null;
			if(!access().flagForKey("read"))
				return (String)application().valueForKeyPath(
						"strings.Strings.messages.noAccess");
			return mark().hover();
		}
	}
	
	public boolean cantCreateMark() {
		if(student() == null || lesson() == null)
			return false;
		if(Various.boolForObject(valueForBinding("full")) 
				&& !lesson().usedCriteria().containsObject(critItem))
			return true;
        return (Various.boolForObject(valueForBinding("denyCreation")) && 
        		student() != null && lesson() != null && mark() == null);
    }
	
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
		} else //if(activeCriterion().intValue() != 0) {
			if(activeCriterion().intValue() < 0) {
				title = fullNoteForStudent();
			} else {
				if(lesson().usedCriteria().count() == 0)
					return null;
				Integer activeCriterion = activeCriterion();
				Mark mark = lesson().markForStudentAndCriterion(student(),activeCriterion);
				if(mark != null) {
					return mark.hover();
				}
				if(activeCriterion.intValue() == 0) {
					BigDecimal integral = (lesson().usedCriteria().contains(activeCriterion))?
							null:lesson().integralForStudent(student());
					if(integral != null)
						return integral.toPlainString();
					else if (!single())
						title = fullNoteForStudent();
				}
			}
/*		} else {
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
		}*/
		if(title == null)
			return null;
		return WOMessage.stringByEscapingHTMLAttributeValue(title);
	}
	
	public String extractNoteLink() {
		String note = noteForStudent();
		if(note == null)
			return null;
		int idx = note.indexOf("://");
		if(idx < 0)
			return null;
		int pre = note.lastIndexOf(' ', idx);
		if(pre < 0)
			pre = 0;
		int post = note.indexOf(' ', idx);
		if(post > 0)
			return note.substring(pre, post);
		else if(pre > 0)
			return note.substring(pre);
		else
			return note;
	}
	
	public String shortNoteForStudent() {
		if(student() == null)
			return (String)session().valueForKeyPath(
					"strings.Reusables_Strings.dataTypes.text");
		String note = noteForStudent();
		if(note == null)
			return null;
		int idx = note.indexOf("://");		
		if(len() > 250) {
			if(idx < 0)
				return note;
			StringBuilder buf = new StringBuilder();
			int pre = note.lastIndexOf(' ', idx);
			if(pre < 0)
				pre = 0;
			else
				buf.append(note.substring(0,pre +1));
			int post = note.indexOf(' ', idx);
			String link = note;
			if(post > 0)
				link = note.substring(pre, post);
			else if(pre > 0)
				link = note.substring(pre);
			buf.append(" <a href = \"");
			buf.append(link).append("\" target = \"_blank\">");
			buf.append(link).append("</a> ");
			if(post > 0)
				buf.append(note.substring(post));
			return buf.toString();
		}
		if(idx > 0) {
			String url = application().resourceManager().urlForResourceNamed(
					"link.png","RujelBase",null,context().request());
			return "<img src= \"" + url +
					"\" alt= \"link\" height= \"16\" width= \"16\">";
		}
		return super.shortNoteForStudent();
	}
	
	public String onClick() {
		Integer activeCriterion = activeCriterion();
		if(activeCriterion != null && activeCriterion.intValue() < 0) {
			String note = extractNoteLink();
			if(note != null) {
				StringBuilder buf = new StringBuilder("window.open('");
				buf.append(note).append("','_blank');");
				return buf.toString();
			}
		}
		return super.onClick();
    }

	public String clickNote() {
		String note = extractNoteLink();
		if(note != null) {
			StringBuilder buf = new StringBuilder("window.open('");
			buf.append(note).append("','_blank');");
			return buf.toString();
		}
		return (String)session().valueForKey("ajaxPopup");
	}
}
