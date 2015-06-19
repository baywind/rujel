// PrognosisPopup.java: Class file for WO Component 'PrognosisPopup'

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

package net.rujel.autoitog;

import java.math.BigDecimal;
import java.util.Enumeration;
import java.util.logging.Logger;

import net.rujel.base.MyUtility;
import net.rujel.eduresults.ItogMark;
import net.rujel.eduresults.ItogPreset;
import net.rujel.interfaces.*;
import net.rujel.reusables.*;
import net.rujel.ui.AddOnPresenter.AddOn;
import net.rujel.ui.RedirectPopup;

import com.webobjects.appserver.*;
import com.webobjects.eoaccess.EOUtilities;
import com.webobjects.eocontrol.EOEditingContext;
import com.webobjects.eocontrol.EOEnterpriseObject;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSMutableDictionary;

// Generated by the WOLips Templateengine Plug-in at May 18, 2008 10:06:02 PM
public class PrognosisPopup extends com.webobjects.appserver.WOComponent {
    public PrognosisPopup(WOContext context) {
        super(context);
    }
    
    public Prognosis prognosis;
    public PrognosesAddOn addOn;
    public EduCourse course;
    public Student student;
    public AutoItog eduPeriod;
    public WOComponent returnPage;
    
    public String mark;
    public NamedFlags flags;
    public NSMutableDictionary dict = new NSMutableDictionary();
    
    public String bonusPercent;
    public String bonusText;
    public boolean hasBonus;
    public boolean noEditBonusText;
 	public boolean ifArchive;
 	public String changeReason;
    
    protected boolean calculation;
    public boolean noCancel = false;
    public boolean cantChange;
    public boolean cantEdit;
    
  
    public void appendToResponse(WOResponse aResponse, WOContext aContext) {
    	if(addOn == null)
    		addOn = new PrognosesAddOn(session());
    	addOn.setCourse(course);
    	addOn.setStudent(student);
    	addOn.setPeriodItem(eduPeriod);
    	calculation = (eduPeriod.calculator() != null);
    	flags = new NamedFlags(Prognosis.flagNames);
       	ifArchive = (eduPeriod.namedFlags().flagForKey("manual") &&
       			SettingsReader.boolForKeyPath("markarchive.Prognosis", 
       					SettingsReader.boolForKeyPath("markarchive.archiveAll", false)));
    	if(prognosis == null)  {
    		Calculator calc = eduPeriod.calculator();
    		if(calc != null) {
    			EOEditingContext ec = course.editingContext();
    			ec.lock();
    			prognosis = calc.calculateForStudent(student, course, eduPeriod,
    					eduPeriod.relatedForCourse(course));
    			if(prognosis != null) {
    				prognosis.updateFireDate(addOn.courseTimeout());
    				noCancel = true;
    				/*if(ifArchive) {
    					EOEnterpriseObject archive = EOUtilities.createAndInsertInstance(ec,"MarkArchive");
    					archive.takeValueForKey(prognosis, "object");
    					String calcName = addOn.usage().calculatorName();
						//calcName = calcName.substring(calcName.lastIndexOf('.') +1);
						archive.takeValueForKey(calcName, "reason");
    				}
    				try {
    					ec.saveChanges();
    					//addOn.setPrognosis(prognosis);
    					noCancel = true;
    				} catch (Exception e) {
    					Logger.getLogger("rujel.autoitog").log(WOLogLevel.WARNING,
    							"Error creating single prognosis");
    					ec.revert();
    				}*/
    			}
    			ec.unlock();
    		}
    	} // prognosis == null
		hasBonus = false;
		bonusPercent = null;
		if(prognosis != null) {
			flags.setFlags(prognosis.flags().intValue());
			mark= prognosis.mark();
			dict.takeValueForKey(prognosis.state(), ItogPreset.STATE_KEY);
			addOn.setPrognosis(prognosis);
			if(eduPeriod.calculator() != null) {
				Bonus bonus = prognosis.bonus();
				if(bonus != null)
					bonusText = bonus.reason();
				BigDecimal bonusValue = (bonus == null)?Bonus.calculateBonus(prognosis,null,false)
						:bonus.calculateValue(prognosis, false);
				hasBonus = (bonus != null && bonus.value() != null &&
						bonus.value().compareTo(bonusValue) >= 0);
				NamedFlags accessBonus = (NamedFlags)session().valueForKeyPath("readAccess.FLAGS.Bonus");
				if(!((bonus == null)?accessBonus.flagForKey("create"):accessBonus.flagForKey("read")))
					bonusValue = null;
				bonusPercent = (bonusValue == null)?null:fractionToPercent(bonusValue);
				//String param = (hasBonus)?"Bonus":"BonusText";
				noEditBonusText = !accessBonus.flagForKey(
						(bonus != null && bonus.submitted())?"edit":"create");
			}
		}
    	cantChange = !access().flagForKey((prognosis == null)?"create":"edit");
    	cantEdit = cantChange || !eduPeriod.namedFlags().flagForKey("manual");
    	if(!cantEdit) {
			Integer presetGroup = ItogPreset.getPresetGroup(eduPeriod.itogContainer(), course);
			if(presetGroup != null && presetGroup.intValue() > 0) {
				NSArray presets = ItogPreset.listPresetGroup(
						eduPeriod.editingContext(), presetGroup, true);
				if(presets != null && presets.count() > 0) {
					dict.takeValueForKey(presets, "presets");
					if(mark == null)
						dict.takeValueForKey(presets.objectAtIndex(0), "preset");
					else
						dict.takeValueForKey(ItogPreset.presetForMark(mark, presets), "preset");
				} else {
					dict.takeValueForKey(Boolean.TRUE, "noPresets");
				}
			}
    	}
     	super.appendToResponse(aResponse, aContext);
    }
    
    public boolean showPercent() {
    	if(prognosis == null || !calculation)
    		return false;
    	return true;
    }

    public NamedFlags access() {
    	return addOn.access();
    }
    
    public WOActionResults save() {
    	if(prognosis != null && prognosis.editingContext() == null)
    		prognosis = null;
		ItogPreset preset = (ItogPreset)dict.valueForKey("preset");
		if(preset != null)
			mark = preset.mark();
    	if(prognosis !=null || mark != null) {
    		EOEditingContext ec = course.editingContext();
	    	Logger logger = Logger.getLogger("rujel.autoitog");
	    	int actionType = 164;
    		if(mark == null) {
    			if(calculation) {
    				prognosis = eduPeriod.calculator().calculateForStudent(student, course, 
    						eduPeriod, eduPeriod.relatedForCourse(course));
    				changeReason = eduPeriod.calculatorName();
    				if(prognosis != null) {
    					mark = prognosis.mark();
    					dict.takeValueForKey(prognosis.state(), ItogPreset.STATE_KEY);
    				} else {
    					ec.revert();
    				}
    			} else {
    				if(ec.globalIDForObject(prognosis).isTemporary()) {
    					ec.revert();
    				} else {
    					ec.deleteObject(prognosis);
    					actionType = 3;
    				}
    				prognosis = null;
    			}
    		}
    		if(mark != null) {
    			if(prognosis == null) {
    				prognosis = (Prognosis)EOUtilities.createAndInsertInstance(ec, "Prognosis");
       				prognosis.setStudent(student);
       				prognosis.setCourse(course);
       				prognosis.setAutoItog(eduPeriod);
       				actionType = 1;
     			} else {
     				actionType = 2;
     			}
    			if(!cantEdit) {
    				if(!mark.equals(prognosis.mark())) {
//    					String oldMark = prognosis.mark();
    					if(preset != null) {
//    						BigDecimal oldValue = prognosis.value();
//    						if(oldValue == null || BigDecimal.ZERO.compareTo(oldValue) == 0) {
    							prognosis.setValue(preset.value());
//    						}
    					}
    					prognosis.setMark(mark);
    				}
    				Integer state = (preset == null)?
    						(Integer)dict.valueForKey(ItogPreset.STATE_KEY): preset.state();
    				if(state != null && !state.equals(prognosis.state()))
    					prognosis.setState(state);
    			}
   				prognosis.updateFireDate();
	    		Bonus bonus = prognosis.bonus();
    	    	Object[] args = new Object[] {session(),bonus,prognosis};
    	    	if(hasBonus) {
    	    		if(bonus == null) {
    	    			bonus = (Bonus)EOUtilities.createAndInsertInstance(ec, Bonus.ENTITY_NAME);
//    	    			bonus.initBonus(prognosis, true);
    	    			args[1] = bonus;
    	    			logger.log(WOLogLevel.EDITING,"Adding bonus to prognosis",args);
    	    		} else if(!bonus.submitted()) {
    	    			bonus.calculateValue(prognosis, true);
    	    			logger.log(WOLogLevel.EDITING,"Submitting bonus for prognosis",args);
    	    		}
    	    	} else {
    	    		if(bonus != null) {
    	    			bonus.zeroBonus();
    	    			prognosis.updateMarkFromValue();
    	    			logger.log(WOLogLevel.EDITING,"Unsubmitting bonus from prognosis",args);
    	    		}
	    			flags.setFlagForKey(false, "keepBonus");
    			}
    	    	if(bonusText != null) {
    	    		if(bonus == null) {
    	    			bonus = (Bonus)EOUtilities.createAndInsertInstance(ec, Bonus.ENTITY_NAME);
//    	    			bonus.initBonus(prognosis, false);
    	    			prognosis.addObjectToBothSidesOfRelationshipWithKey(bonus, 
    	    					Prognosis.BONUS_KEY);
    	    			Bonus.calculateBonus(prognosis, bonus, false);
    	    			args[1] = bonus;
    	    			logger.log(WOLogLevel.EDITING,"Requesting bonus for prognosis",args);
    	    		}
    	    		bonus.setReason(bonusText);
    	    	} else if(bonus != null) {
    	    		if(Various.boolForObject(session().valueForKeyPath("readAccess._delete.Bonus"))) {
    	    			Object message = application().valueForKeyPath(
    					"strings.RujelAutoItog_AutoItog.ui.cantDeleteBonus");
    	    			session().takeValueForKey(message, "message");
    	    		} else {
    	    			prognosis.removeObjectFromBothSidesOfRelationshipWithKey(bonus, "bonus");
    	    			ec.deleteObject(bonus);
    	    			logger.log(WOLogLevel.EDITING,"Removing bonus from prognosis",args);  
    	    		}
    	    	}
    	    	prognosis.setNamedFlags(flags);
    		} //  if !(mark == null && !calculation)
    		try {
    			if(ec.hasChanges()) {
    				ec.saveChanges();
    				if(ifArchive) {
    					EOEnterpriseObject archive = EOUtilities.createAndInsertInstance(ec,"MarkArchive");
    					if(prognosis != null) {
    						archive.takeValueForKey(prognosis, "object");
    						if(prognosis.bonus() != null) {
    							archive.takeValueForKey(bonusText,"@bonusText");
    							archive.takeValueForKey(prognosis.bonus().value(),"@bonusValue");
    						}
    					} else {
    						archive.takeValueForKey(identifierDictionary(), "identifierDictionary");
    						archive.takeValueForKey(null,"@mark");
    					}
    					archive.takeValueForKey(changeReason, "reason");
    					archive.takeValueForKey(new Integer(actionType), "actionType");	
        				ec.saveChanges();
    				}
					PrognosesAddOn.feedStats(course, eduPeriod.itogContainer(), null);
    			}
    			addOn.setPrognosis(prognosis);
    		} catch (Exception e) {
    			logger.log(WOLogLevel.WARNING,"Error saving prognosis",e);
    			session().takeValueForKey(e.getMessage(), "message");
				ec.revert();
			}
    	}
    	returnPage.ensureAwakeInContext(context());
    	return returnPage;
    }
    
    public WOActionResults delete() {
    	mark = null;
    	calculation = false;
    	dict.removeObjectForKey("preset");
    	return save();
    }
    
    public static String fractionToPercent(BigDecimal decimal) {
    	if(decimal == null || decimal.compareTo(BigDecimal.ZERO) == 0)
    		return "0";
    	/*decimal = decimal.movePointRight(2).stripTrailingZeros();
    	if(decimal.scale() < 0)
    		decimal = decimal.setScale(0); */
    	return MyUtility.formatDecimal(decimal.movePointRight(2));
    }
    
    public String completePercent() {
    	return fractionToPercent(prognosis.complete());
    }
    public String valuePercent() {
    	return fractionToPercent(prognosis.value());
    }
        
    public String bonusTitle() {
    	String key = "requestBonus";
    	if(hasBonus) {
    		key = "addedBonus";
    	} else if (prognosis.bonus() != null) {
			key = "requestedBonus";
		}
    	String result = (String)application().valueForKeyPath(
    			"strings.RujelAutoItog_AutoItog.ui." + key);
    	if(result == null)
    		result = key;
    	return result;
    }
    
 	public NSMutableDictionary identifierDictionary() {
		if(student == null || eduPeriod == null || course == null)
    		return null;
		NSMutableDictionary ident = new NSMutableDictionary("Prognosis","entityName");
		ident.takeValueForKey(eduPeriod.itogContainer(),"itog");
		ident.takeValueForKey(student, "student");
		ident.takeValueForKey(course,"course");
		ident.takeValueForKey(course.editingContext(), "editingContext");
		return ident;
    }
 	
	public String onkeypress() {
		if(!ifArchive || mark == null)
			return null;
return "hideObj('performPrognos');showObj('prognosChangeReason');form.changeReason.onkeypress();";
	}

	public EOEnterpriseObject item;
    public NSMutableDictionary archDict;
    
    public void setItem(EOEnterpriseObject newItem) {
    	item = newItem;
    	if(item == null)
    		return;
    	if(archDict == null)
    		archDict = new NSMutableDictionary();
    	else
    		archDict.removeAllObjects();
    	String flagsString = (String)item.valueForKey("@flags");
    	NamedFlags archFlags = (flagsString == null)? DegenerateFlags.ALL_FALSE:
    		new NamedFlags(Integer.parseInt(flagsString),Prognosis.flagNames);
    	archDict.takeValueForKey(archFlags, "flags");
    	if(archFlags.flagForKey("disable"))
    		archDict.takeValueForKey("grey", "styleClass");
    	if(archFlags.flagForKey("keep"))
    		archDict.takeValueForKey("font-weight:bold;","style");

    	String  bonusReason = (String)item.valueForKey("@bonusText");
    	if(bonusReason != null) {
        	StringBuffer title = new StringBuffer((String)item.valueForKey("@value"));
    		BigDecimal bonusValue = new BigDecimal((String)item.valueForKey("@bonusValue"));
    		StringBuffer bonus = new StringBuffer("<span style = \"color:#ff0000;\">+");
    		if(bonusValue.compareTo(BigDecimal.ZERO) > 0) {
    			if(archFlags.flagForKey("keepBonus"))
    				bonus.append("<strong>!</strong>");
    			else
    				bonus.append('!');
    			title.append(" + ").append(bonusValue);
    		} else {
    			bonus.append('?');
    		}
    		bonus.append("</span>");
    		archDict.takeValueForKey(bonus.toString(),"bonusString");
    		title.append(" (").append(WOMessage.stringByEscapingHTMLAttributeValue(bonusReason));
    		title.append(')');
    		archDict.takeValueForKey(title.toString(),"title");
    	} else {
    		archDict.takeValueForKey(item.valueForKey("@value"),"title");
    	}
    }
    
    public WOActionResults perform() {
    	ItogMark itog = prognosis.convertToItogMark(null, false, null);
    	if(itog == null) {
			session().takeValueForKey(session().valueForKeyPath(
					"strings.RujelAutoItog_AutoItog.ui.disabledPrognosis"), "message");
			return RedirectPopup.getRedirect(context(), returnPage);
    	}
    	Logger logger = Logger.getLogger("rujel.autoitog");
    	EOEditingContext ec = prognosis.editingContext();
    	try {
    		if(SettingsReader.boolForKeyPath("markarchive.ItogMark", 
    				SettingsReader.boolForKeyPath("markarchive.archiveAll", false))) {
				EOEnterpriseObject archive = EOUtilities.createAndInsertInstance(ec,"MarkArchive");
				archive.takeValueForKey(itog, "object");
				int actionType = (ec.globalIDForObject(itog).isTemporary())? 1 : 2;
				archive.takeValueForKey(new Integer(actionType), "actionType");
    		}
			ec.saveChanges();
			logger.log(WOLogLevel.EDITING,"Forced prognosis execution",itog);
//			ModuleInit.prepareStats(course, prognosis.itogContainer(), true);
		} catch (Exception e) {
			logger.log(WOLogLevel.WARNING,"Error forcing prognosis execution",
					new Object[] {session(),prognosis,e});
			ec.revert();
			session().takeValueForKey(e.getMessage(), "message");
			return RedirectPopup.getRedirect(context(), returnPage);
		}
        WOComponent nextPage = pageWithName("ItogPopup");
		nextPage.takeValueForKey(itog,"itog");
		nextPage.takeValueForKey(prognosis.student(),"student");
		nextPage.takeValueForKey(prognosis.itogContainer(),"itogContainer");
		nextPage.takeValueForKey(returnPage,"returnPage");
		NSArray addOns = (NSArray)session().objectForKey("notesAddOns");
		if(addOns != null) {
			Enumeration enu = addOns.objectEnumerator();
			while (enu.hasMoreElements()) {
				AddOn itogAddOn = (AddOn) enu.nextElement();
				if("itogs".equals(itogAddOn.valueForKey("id"))) {
					nextPage.takeValueForKey(itogAddOn,"addOn");
					itogAddOn.agregate = null;
					break;
				}
			}
		}
        return nextPage;
    }
    
    public String performOnClick() {
		String href = context().componentActionURL();
		String result = "refreshRequired=true;ajaxPopupAction('"+href+"');hideObj('ajaxPopup')";
		return result;
    }
    
    public Boolean cantPerform() {
    	if(prognosis == null)
    		return Boolean.TRUE;
    	ItogMark itog = ItogMark.getItogMark(course.cycle(),prognosis.itogContainer(),
				student,prognosis.editingContext());
    	if(itog != null)
    		return Boolean.TRUE;
    	if(access().flagForKey("edit") && 
//    			EOPeriod.Utility.compareDates(eduPeriod.fireDate(), null) < 0 &&
    			eduPeriod.fireDateTime().getTime() < System.currentTimeMillis()) {
    		return Boolean.FALSE;
    	}
    	return (Boolean)session().valueForKeyPath("readAccess._create.ItogMark");
    }

	public String manualMarkStyle() {
		if(dict.valueForKey("preset") == null)
			return null;
		else
			return "display:none;";
	}

}