// ItogPopup.java

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

package net.rujel.eduresults;


import net.rujel.reusables.*;
import net.rujel.interfaces.*;
//import net.rujel.eduresults.*;

import com.webobjects.foundation.*;
import com.webobjects.appserver.*;
import com.webobjects.eocontrol.*;
import com.webobjects.eoaccess.*;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.Enumeration;
import java.util.logging.Logger;
import net.rujel.reusables.WOLogLevel;
import net.rujel.ui.AddOnPresenter;

public class ItogPopup extends WOComponent {
	protected static Logger logger = Logger.getLogger("rujel.itog");
	public ItogMark itog;

    public ItogContainer itogContainer;
	//public EduCycle cycle;
    public Student student;
//    public String mark;
    public NSArray presets;
    public NSMutableDictionary dict = new NSMutableDictionary();
	public AddOnPresenter.AddOn addOn;
	public WOComponent returnPage;
	public String changeReason;
	public final boolean ifArchive = SettingsReader.boolForKeyPath("markarchive.ItogMark", 
			SettingsReader.boolForKeyPath("markarchive.archiveAll", false));
	public EOEnterpriseObject commentEO;
	public NSDictionary comments;

	public ItogPopup(WOContext context) {
		super(context);       
	}
	
	public void setItog(ItogMark newItog) {
		itog = newItog;
		if(itog == null) {
			dict.takeValueForKey(null, ItogMark.MARK_KEY);
			dict.takeValueForKey(Integer.valueOf(0), ItogMark.STATE_KEY);
			return;
		}
		if(itogContainer == null)
			itogContainer = itog.container();
		String mark = itog.mark();
		dict.takeValueForKey(mark, ItogMark.MARK_KEY);
		dict.takeValueForKey(itog.state(), ItogMark.STATE_KEY);
		if(presets == null && !readOnly()) {
			Integer presetGroup = ItogPreset.getPresetGroup(itogContainer, course());
			if(presetGroup != null && presetGroup.intValue() > 0) {
				presets = ItogPreset.listPresetGroup(itog.editingContext(), presetGroup,true);
			}
		}
		if(presets != null && presets.count() > 0)
			dict.takeValueForKey(ItogPreset.presetForMark(mark, presets), "preset");
		else
			dict.takeValueForKey(Boolean.TRUE, "noPresets");
	}
	
	public void setItogContainer(ItogContainer value) {
		itogContainer = value;
		if(itog == null) {
			dict.takeValueForKey(null, ItogMark.MARK_KEY);
			dict.takeValueForKey(Integer.valueOf(0), ItogMark.STATE_KEY);
		}
		if(presets == null && addOn != null && !readOnly()) {
			Integer presetGroup = ItogPreset.getPresetGroup(itogContainer, addOn.course());
			if(presetGroup != null && presetGroup.intValue() > 0) {
				presets = ItogPreset.listPresetGroup(itogContainer.editingContext(), presetGroup, true);
				if(presets != null && presets.count() > 0)
					dict.takeValueForKey(presets.objectAtIndex(0), "preset");
				else
					dict.takeValueForKey(Boolean.TRUE, "noPresets");
			}
		}
	}

	public EduCourse course() {
		if(addOn == null)
				return (itog==null)? null: itog.assumeCourse();
		return addOn.course();
	}

	/*
	public String mark() {
		if(itog != null) {
			mark = itog.mark();
		} else {
			mark = null;
		}
		return mark;
	}*/

	public NSArray flaglist() {
		if(dict.valueForKey(ItogMark.MARK_KEY) == null) return null;
		return ItogMark.flagKeys;
	}

	public static final NSDictionary flagNames = (NSDictionary)WOApplication.application().valueForKeyPath("strings.RujelEduResults_EduResults.properties.ItogMark.flags");
	public Object item;

	public boolean flagStatus () {
		return itog.readFlags().flagForKey(item);
	}
	public void setFlagStatus(boolean status) {
		itog.readFlags().setFlagForKey(status,item);
	}
	
	public Boolean flagDisabled() {
		if(item == null)
			return null;
		if(item.equals("flagged")) {
			return (Boolean)access().valueForKey("_edit");
		} else {
			return Boolean.TRUE;
		}
	}

	public String flagName() {
		return (String)flagNames.valueForKey(item.toString());
	}

	protected NamedFlags _access;
	public NamedFlags access() {
		if(_access == null)
			_access = (NamedFlags)session().valueForKeyPath("readAccess.FLAGS." +
					((itog==null)?ItogMark.ENTITY_NAME:"itog"));
		return _access;
	}

	public WOComponent save() {
		returnPage.ensureAwakeInContext(context());
		EduCourse eduCourse = course();
		EOEditingContext ec = (eduCourse==null)?itog.editingContext():eduCourse.editingContext();
		if(commentEO != null || (comments != null && comments.count() != 0)) {
			ec.lock();
			try {
				if(commentEO == null)
					commentEO = ItogMark.getItogComment(eduCourse.cycle(),
							itogContainer, student, true);
				String comment = (String)comments.valueForKey(ItogMark.MANUAL);
				comments = ItogMark.setCommentForKey(commentEO, comment, ItogMark.MANUAL);
				if(comments.count() == 0)
					commentEO = null;
				if(ec.hasChanges())
					ec.saveChanges();
				if(itog != null) {
					itog.takeValueForKey(commentEO, "commentEO");
				}
			} catch (Exception e) {
				logger.log(WOLogLevel.WARNING,"Error saving ItogComment",
						new Object[] {session(),commentEO});
			} finally {
				ec.unlock();
			}
			
		}
		ItogPreset preset = (ItogPreset)dict.valueForKey("preset");
		String mark = (preset == null)?(String)dict.valueForKey(ItogMark.MARK_KEY):preset.mark();
		if(itog == null && mark == null)
			return returnPage;
		if(mark == null)
			return delete();

		boolean newItog = (itog == null);
		if(!access().flagForKey((newItog)?"create":"edit")) {
			session().takeValueForKey(valueForKeyPath("application.strings.Strings.messages.noAccess"),"message");
			return returnPage;
		}
		boolean same = (!newItog && mark.equals(itog.mark()));
		if(newItog || !same || itog.readFlags().flagForKey("constituents")) {
			ec.lock();
			EOEnterpriseObject prognosis = null;
			try {
				Class prClass = Class.forName("net.rujel.autoitog.Prognosis");
				Method meth = prClass.getMethod("getPrognosis", Student.class,EduCourse.class, ItogContainer.class, Boolean.TYPE);
				prognosis = (EOEnterpriseObject)meth.invoke(null, student,eduCourse,itogContainer,Boolean.FALSE);
			} catch (Exception e) {
				// cant get corresponding prognosis
			}
			try {
				/*boolean enableArchive = SettingsReader.boolForKeyPath("markarchive.enable", false);
			if(mark == null) {
				if(enableArchive) {
					EOEnterpriseObject archive = EOUtilities.createAndInsertInstance(ec,"MarkArchive");
					archive.takeValueForKey(itog, "objectIdentifier");
					archive.takeValueForKey(".", '@' + "mark");
				}
				ec.deleteObject(itog);
			} else {*/
				if(newItog) {
					itog = (ItogMark)EOUtilities.createAndInsertInstance(ec,"ItogMark");
					itog.addObjectToBothSidesOfRelationshipWithKey(itogContainer,
							ItogMark.CONTAINER_KEY);
					itog.addObjectToBothSidesOfRelationshipWithKey(student,"student");
					itog.addObjectToBothSidesOfRelationshipWithKey(eduCourse.cycle(),"cycle");
					if(addOn != null)
						addOn.agregate = null;
				}
				if(!same) {
					if(!newItog)
						itog.readFlags().setFlagForKey(true,"changed");
					itog.setMark(mark);
					if(preset == null) {
						itog.takeValueForKey(dict.valueForKey(ItogMark.STATE_KEY),
								ItogMark.STATE_KEY);
					} else {
						itog.setState(preset.state());
						itog.setValue(preset.value());
					}
					itog.readFlags().setFlagForKey(true,"manual");
				}
				itog.readFlags().setFlagForKey(false,"constituents");
				if(prognosis != null) {
					boolean flag = true;
					BigDecimal value = (BigDecimal)prognosis.valueForKey("value");
					if(preset == null || preset.mark().equals(prognosis.valueForKey("mark")))
						itog.setValue(value);
					if(value != null) {
						String fromValue = (String)prognosis.valueForKey("markFromValue");
						if(fromValue != null)
							flag = !mark.equals(fromValue);
					}
					itog.readFlags().setFlagForKey(flag,"forced");
					flag = !Various.boolForObject(prognosis.valueForKey("isComplete"));
					itog.readFlags().setFlagForKey(flag,"incomplete");
//				} else {
//					itog.readFlags().setFlagForKey(false,"forced");
//					itog.readFlags().setFlagForKey(false,"incomplete");
//					itog.setValue(null);
				}
				if(ifArchive) {
					EOEnterpriseObject archive = EOUtilities.createAndInsertInstance(ec,"MarkArchive");
					archive.takeValueForKey(itog, "object");
					if(changeReason == null && same)
						archive.takeValueForKey(flagNames.valueForKey("constituents"),"reason");
					else
						archive.takeValueForKey(changeReason, "reason");
					if(!same && !itog.readFlags().flagForKey("changed")) {
						Integer count = (Integer)archive.valueForKey("archivesCount");
						itog.readFlags().setFlagForKey(count.intValue() > 0,"changed");
					}
					archive.takeValueForKey(new Integer((newItog)?1:2), "actionType");
				}
				//}

				ec.saveChanges();
				String message = (newItog)?"New Itog created":"Itog is changed";
				logger.log(WOLogLevel.EDITING,message,new Object[] {session(),itog});
				if (!same) {
					ModuleInit.prepareStats(eduCourse, itogContainer,ItogMark.MARK_KEY,true);
					ModuleInit.prepareStats(eduCourse, itogContainer,"stateKey",true);
				}
			} catch (Exception ex) {
				logger.log(WOLogLevel.WARNING,"Failed to save itog",
						new Object[] {session(),itog,ex});
				session().takeValueForKey(ex.getMessage(),"message");
			} finally {
				ec.unlock();
			}
		} else if(ec.hasChanges()) {
			ec.lock();
			try {
				ec.saveChanges();
			} catch (Exception e) {
				logger.log(WOLogLevel.WARNING, "Failed to save",
						new Object[] {session(),itog,e});
				session().takeValueForKey(e.getMessage(),"message");
			} finally {
				ec.unlock();
			}
		}
		return returnPage;
	}
	
	public WOComponent delete() {
		returnPage.ensureAwakeInContext(context());
		if(itog == null)
			return returnPage;
		if(!access().flagForKey("delete")) {
			session().takeValueForKey(valueForKeyPath("application.strings.Strings.messages.noAccess"),"message");
			return returnPage;
		}
		EOEditingContext ec = student.editingContext();
		ec.lock();
		try {
			NSDictionary pKey = EOUtilities.primaryKeyForObject(ec,itog);
			if(ifArchive) {
				EOEnterpriseObject archive = EOUtilities.createAndInsertInstance(ec,"MarkArchive");
				archive.takeValueForKey(itog, "objectIdentifier");
				archive.takeValueForKey(".", "@mark");
				archive.takeValueForKey(changeReason, "reason");
				archive.takeValueForKey(new Integer(3), "actionType");
			}
			EduCourse course = course();
			ec.deleteObject(itog);
			ec.saveChanges();
			logger.logp(WOLogLevel.EDITING,getClass().getName(),"delete","Itog is deleted",new Object[] {session(),pKey});
			ModuleInit.prepareStats(course, itogContainer,ItogMark.MARK_KEY,true);
			ModuleInit.prepareStats(course, itogContainer,"stateKey",true);
		} catch (Exception ex) {
			logger.logp(WOLogLevel.WARNING,getClass().getName(),"delete","Failed to delete itog",new Object[] {session(),itog,ex});
			session().takeValueForKey(ex.getMessage(),"message");
			ec.revert();
		} finally {
			ec.unlock();
		}
		if(addOn != null)
			addOn.agregate = null;
		return returnPage;
	}

	private transient Boolean readOnly;

	public boolean readOnly() {
		if(readOnly != null) return readOnly.booleanValue();
		if(access() == null || access().flagForKey("edit")) {
			readOnly = Boolean.FALSE;
			return false;
		} 
		if(access().flagForKey("create") && itog==null) {
			readOnly = Boolean.FALSE;
			return false;
		}
		readOnly = Boolean.TRUE;
		return true;
	}
	
	public NSMutableDictionary identifierDictionary() {
		EduCourse eduCourse = course();
		if(student == null || itogContainer == null || eduCourse == null)
    		return null;
		NSMutableDictionary ident = new NSMutableDictionary("ItogMark","entityName");
		ident.takeValueForKey(itogContainer,"container");
		ident.takeValueForKey(student, "student");
		ident.takeValueForKey(eduCourse.cycle(),"eduCycle");
		ident.takeValueForKey(eduCourse.editingContext(), "editingContext");
		return ident;
    }

	public String onkeypress() {
		if(!ifArchive)
			return null;
		return "showObj('itogChangeReason');form.changeReason.onkeypress();fitWindow();";
	}
	
	public NSDictionary comments() {
		if(comments == null) {
			EduCycle cycle = null;
			if(itog != null) {
				cycle = itog.cycle();
			} else {
				cycle = course().cycle();
			}
			commentEO = ItogMark.getItogComment(cycle, itogContainer, student, false);
			if(commentEO == null) {
				comments = new NSMutableDictionary();
			} else {
				comments = ItogMark.commentsDict(commentEO);
			}
		}
		return comments;
	}
	
	public String otherComments() {
		if(comments().count() == 0)
			return null;
		StringBuilder buf = new StringBuilder();
		Enumeration enu = comments().keyEnumerator();
		while (enu.hasMoreElements()) {
			String key = (String) enu.nextElement();
			if(key.equals("alias") || key.equals(ItogMark.MANUAL) ||
					key.equals(ItogMark.CONTAINER_KEY) || key.equals("subject"))
			continue;
			String comment = (String)comments().valueForKey(key);
			buf.append("<strong>").append(key);
			buf.append(":</strong> ").append(comment);
			buf.append("<br/>\n");
		}
		return buf.toString();
	}
/*
	public String manualMarkStyle() {
		if(dict.valueForKey("preset") == null)
			return null;
		else
			return "display:none;";
	}*/

}
