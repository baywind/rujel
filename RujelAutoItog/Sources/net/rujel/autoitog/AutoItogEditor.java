// AutoItogEditor.java: Class file for WO Component 'AutoItogEditor'

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

import java.lang.ref.WeakReference;
import java.text.Format;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.Timer;
import java.util.logging.Logger;

import net.rujel.base.MyUtility;
import net.rujel.base.SettingsBase;
import net.rujel.criterial.BorderSet;
import net.rujel.eduresults.ItogContainer;
import net.rujel.eduresults.ItogMark;
import net.rujel.eduresults.ItogPreset;
import net.rujel.interfaces.EOPeriod;
import net.rujel.interfaces.EduCourse;
import net.rujel.interfaces.EduCycle;
import net.rujel.interfaces.PerPersonLink;
import net.rujel.reusables.NamedFlags;
import net.rujel.reusables.SettingsReader;
import net.rujel.reusables.WOLogLevel;

import com.webobjects.appserver.*;
import com.webobjects.eoaccess.EOUtilities;
import com.webobjects.eocontrol.EOEditingContext;
import com.webobjects.eocontrol.EOEnterpriseObject;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSDictionary;
import com.webobjects.foundation.NSLocking;
import com.webobjects.foundation.NSMutableArray;
import com.webobjects.foundation.NSTimestamp;

// Generated by the WOLips Templateengine Plug-in at Sep 11, 2009 11:25:34 AM
public class AutoItogEditor extends com.webobjects.appserver.WOComponent {
	
	public static final Logger logger = Logger.getLogger("rujel.autoitog");
	
	public WOComponent returnPage;
	public ItogContainer itog;
	public AutoItog autoItog;
	public String listName;
	
	public NSArray bsets;
	public NSArray calculators;
	public Object item;
	public NSDictionary calc;
	public String fireDate;
	public String fireTime;
	public BorderSet borderSet;
	public NamedFlags namedFlags;
	public boolean recalculate;
	public boolean deleteItogs;
	
	public Boolean cantChange;
	public Boolean canDeleteItogs;
	
    public AutoItogEditor(WOContext context) {
        super(context);
        calculators = (NSArray)context.session().valueForKeyPath(
						"strings.RujelAutoItog_AutoItog.calculators");
    }
    
    public void setItog(ItogContainer itogContainer) {
    	itog = itogContainer;
    	bsets = EOUtilities.objectsForEntityNamed(itog.editingContext(), BorderSet.ENTITY_NAME);
    	Integer presetGroup = ItogPreset.getPresetGroup(listName, itog.eduYear(), itog.itogType());
    	NSArray presetList = ItogPreset.listPresetGroup(itog.editingContext(), presetGroup);
    	presetList = (NSArray)presetList.valueForKey(ItogPreset.MARK_KEY);
    	Enumeration enu = bsets.objectEnumerator();
    	NSMutableArray result = new NSMutableArray();
    	bset:
    	while (enu.hasMoreElements()) {
			BorderSet set = (BorderSet) enu.nextElement();
			Enumeration benu = set.borders().objectEnumerator();
			while (benu.hasMoreElements()) {
				EOEnterpriseObject border = (EOEnterpriseObject) benu.nextElement();
				if(!presetList.containsObject(border.valueForKey("title")))
					continue bset;
			}
			result.addObject(set);
		}
    	bsets = result;
    }

    public void setAutoItog(AutoItog ai) {
    	autoItog = ai;
//     	String pattern = SettingsReader.stringForKeyPath("ui.shortDateFormat","MM-dd");
    	Format format = MyUtility.dateFormat();
    	NSTimestamp dt = (ai == null)?AutoItog.defaultFireDateTime():ai.fireDate();
    	fireDate = format.format(dt);
    	format = new SimpleDateFormat("HH:mm");
    	if(ai != null)
    		dt = ai.fireTime();
    	fireTime = format.format(dt);
       	if(ai == null) {
    		namedFlags = new NamedFlags(2,AutoItog.flagNames);
    		return;
    	}
       	listName = ai.listName();
    	String pattern = autoItog.calculatorName();
    	if(pattern != null) { // resolve className to readable title
    		for (int i = 0; i < calculators.count(); i++) {
    			calc = (NSDictionary)calculators.objectAtIndex(i);
    			if(pattern.equals(calc.valueForKey("className"))) {
    				break;
    			}
    			calc = null;
    		}
    	}
    	borderSet = ai.borderSet();
    	namedFlags = ai.namedFlags();
    }

    public void appendToResponse(WOResponse aResponse, WOContext aContext) {
    	recalculate = (autoItog == null);
    	if(recalculate) {
    		cantChange = (Boolean)session().valueForKeyPath("readAccess._create.AutoItog");
    		canDeleteItogs = Boolean.FALSE;
    	} else {
    		NamedFlags flags = (NamedFlags)session().valueForKeyPath("readAccess.FLAGS.autoItog");
    		cantChange = (Boolean)flags.valueForKey("_edit");
    		canDeleteItogs = Boolean.valueOf(flags.getFlag(4));
    	}
    	super.appendToResponse(aResponse, aContext);
    }

    public boolean disableManual() {
    	return (calc == null || cantChange.booleanValue());
    }

    public WOActionResults save() {
    	EOEditingContext ec = itog.editingContext();
    	ec.lock();
    	try {
    		if(autoItog == null) {
    			autoItog = (AutoItog)EOUtilities.createAndInsertInstance(
    					ec,AutoItog.ENTITY_NAME);
    			autoItog.setItogContainer(itog);
    			autoItog.setListName(listName);
    		}
    		autoItog.setNamedFlags(namedFlags);
    		autoItog.setBorderSet(borderSet);
    		autoItog.setCalculatorName((String)valueForKeyPath("calc.className"));
//    		String pattern = SettingsReader.stringForKeyPath("ui.shortDateFormat","MM-dd");
    		Format format = MyUtility.dateFormat();
    		boolean changeDate = (autoItog.fireDate() == null ||
    				!fireDate.equals(format.format(autoItog.fireDate())));
    		if(changeDate) {
    			try {
    				Date dt = (Date)format.parseObject(fireDate);
    				dt = MyUtility.dateToEduYear(dt, itog.eduYear());
    				autoItog.setFireDate(new NSTimestamp(dt));
    			} catch (ParseException e) {
       				logger.log(WOLogLevel.FINE,"Error parsing fireDate " + fireDate,
    						new Object[] {session(),e});
    			}
    		}
    		format = new SimpleDateFormat("HH:mm");
    		if(autoItog.fireTime() == null ||
    				!fireDate.equals(format.format(autoItog.fireTime()))) {
    			try {
    				Date dt = (Date)format.parseObject(fireTime);
    				NSTimestamp prev = autoItog.fireDateTime();
    				autoItog.setFireTime(new NSTimestamp(dt));
    				NSTimestamp upd = AutoItog.combineDateAndTime(prev, dt);
    				changeDate = changeDate ||
						(EOPeriod.Utility.compareDates(upd, null) == 0 &&
								(prev.getTime() < System.currentTimeMillis() || upd.before(prev)));
    			} catch (ParseException e) {
    				logger.log(WOLogLevel.FINE,"Error parsing fireTime " + fireTime,
    						new Object[] {session(),e});
    			}
    		}
    		ec.saveChanges();
   			logger.log(WOLogLevel.COREDATA_EDITING, "Saved AutoItog", 
					new Object[] {session(),autoItog});
    		if(recalculate || deleteItogs || changeDate) {
    			Thread t = new Thread(new Updater(
    					autoItog, listName, recalculate, deleteItogs,session()),"AutoItogUpdate");
				t.setPriority(Thread.MIN_PRIORITY + 1);
				t.start();
				StringBuilder message = new StringBuilder();
				message.append(session().valueForKeyPath(
					"strings.RujelAutoItog_AutoItog.ui.updateStarted"));
				message.append(' ').append(itog.title());
				session().takeValueForKey(message.toString(), "message");
     		}
    	} catch (Exception e) {
    		logger.log(WOLogLevel.WARNING, "Error saving AutoItog", 
    				new Object[] {session(),autoItog,e});
    		session().takeValueForKey(e.getMessage(), "message");
    	} finally {
    		ec.unlock();
    	}
    	returnPage.ensureAwakeInContext(context());
    	return returnPage;
    }

    public WOActionResults delete() {
    	EOEditingContext ec = itog.editingContext();
    	ec.lock();
    	try {
    		ec.deleteObject(autoItog);
       		logger.log(WOLogLevel.COREDATA_EDITING,
       				"Deleted AutoItog from itog and listName " + listName, 
    				new Object[] {session(),itog});
    	} catch (Exception e) {
    		logger.log(WOLogLevel.WARNING, "Error deleting AutoItog", 
    				new Object[] {session(),autoItog,e});
    		session().takeValueForKey(e.getMessage(), "message");
    	} finally {
    		ec.unlock();
    	}
    	returnPage.ensureAwakeInContext(context());
    	return returnPage;
    }
    
    protected static class Updater implements Runnable{
    	protected AutoItog ai;
    	protected EOEditingContext ec;
    	protected String listName;
    	protected boolean recalc;
    	protected WeakReference sesRef;
    	protected String userName;
    	protected String wosid;
    	protected boolean delItog;
    	
    	public Updater(AutoItog autoItog, String listName, 
    			boolean recalculate, boolean deleteItogs, WOSession session) {
    		ec = new EOEditingContext(autoItog.editingContext().rootObjectStore());
    		ec.lock();
    		try {
    			ai = (AutoItog)EOUtilities.localInstanceOfObject(ec, autoItog);
        		this.listName = listName;
        		recalc = recalculate;
        		delItog = deleteItogs;
        		sesRef = new WeakReference(session);
        		userName = (String)session.valueForKeyPath("user.present");
        		if(userName == null)
        			userName = "Global recalc";
        		wosid = session.sessionID();
    		} catch (Exception e) {
				logger.log(WOLogLevel.WARNING,"Failed to initialize AutoItogUpdate",
						new Object [] {session,ai,e});
				if(session != null) {
					StringBuilder message = new StringBuilder();
					message.append(session.valueForKeyPath(
							"strings.RujelAutoItog_AutoItog.ui.updateError"));
					message.append(' ').append(ai.itogContainer().title());
					session.takeValueForKey(message.toString(), "message");
				}
    		} finally {
    			ec.unlock();
    		}
    	}
    	
    	public void run() {
    		ec.lock();
    		try {
    			ItogContainer itog = ai.itogContainer();
    			SettingsBase base = SettingsBase.baseForKey(ItogMark.ENTITY_NAME, ec, false);
    			NSArray courses = base.coursesForSetting(listName, null,itog.eduYear());
    			if(courses == null || courses.count() == 0) {
    				WOSession ses = (sesRef == null)?null:(WOSession)sesRef.get();
    				logger.log(WOLogLevel.FINER,"No Prognoses to update",
    						new Object[] {ses,ai});
    				return;
    			}
    			Enumeration enu = courses.objectEnumerator();
    			Calculator calculator = ai.calculator();
    			boolean ifArchive = (recalc && calculator != null &&
    					SettingsReader.boolForKeyPath("markarchive.Prognosis", 
						SettingsReader.boolForKeyPath("markarchive.archiveAll", false))
						&& ai.namedFlags().flagForKey("manual"));
    			boolean itogArchive = delItog &&
    					SettingsReader.boolForKeyPath("markarchive.ItogMark", 
						SettingsReader.boolForKeyPath("markarchive.archiveAll", false));
    			EduCycle cycle = null;
    			NSArray itogs = null;
    			String delReason = (delItog)?(String)WOApplication.application().valueForKeyPath(
								"strings.RujelAutoItog_AutoItog.ui.deleteItogs"):null;
    			int delCount = 0;
    			while (enu.hasMoreElements()) {
    				EduCourse course = (EduCourse) enu.nextElement();
    				if(delItog && course.cycle() != cycle) {
    					cycle = course.cycle();
    					itogs = ItogMark.getItogMarks(course.cycle(),itog,null,ec);
    				}
    				NSArray prognoses = null;
					CourseTimeout cto = CourseTimeout.
							getTimeoutForCourseAndPeriod(course, itog);
    				if(recalc && calculator != null) {
    					PerPersonLink prppl = calculator.calculatePrognoses(course, ai);
    					if(prppl == null || prppl.count() == 0)
    						continue;
    					prognoses = prppl.allValues();
    				} else {
    					prognoses = Prognosis.prognosesArrayForCourseAndPeriod(course, ai);
    				}
    				for (int i = 0; i < prognoses.count(); i++) {
						Prognosis prognosis = (Prognosis)prognoses.objectAtIndex(i);
						if(delItog) {
							ItogMark itogMark = prognosis.relatedItog(itogs);
							if(itogMark != null && !itogMark.readFlags().flagForKey("manual")) {
								if(itogArchive) {
									EOEnterpriseObject archive = EOUtilities.createAndInsertInstance(
											ec,"MarkArchive");
									archive.takeValueForKey(itogMark, "objectIdentifier");
									archive.takeValueForKey(".", "@mark");
									archive.takeValueForKey(delReason,"reason");
									archive.takeValueForKey(new Integer(3), "actionType");
									archive.takeValueForKey(wosid, "wosid");
									archive.takeValueForKey(userName, "user");
								}
								ec.deleteObject(itogMark);
								prognosis._relatedItog = NullValue;
								delCount++;
							}
						}
						prognosis.updateFireDate(cto);
						if(ifArchive) {
							NSDictionary snapshot = ec.committedSnapshotForObject(prognosis);
							if(snapshot == null || !snapshot
									.valueForKey("mark").equals(prognosis.mark())) {
								EOEnterpriseObject archive = EOUtilities.createAndInsertInstance(
										ec,"MarkArchive");
								archive.takeValueForKey(prognosis, "object");
								archive.takeValueForKey(ai.calculatorName(), "reason");
								archive.takeValueForKey(new Integer((ec.globalIDForObject(
										prognosis).isTemporary())?1:2), "actionType");
								archive.takeValueForKey(wosid, "wosid");
								archive.takeValueForKey(userName, "user");
							}
						}
					}
    				ec.saveChanges();
    			} // courses.objectEnumerator()
    			WOSession ses = (sesRef == null)?null:(WOSession)sesRef.get();
				if(ses != null) {
					StringBuilder message = new StringBuilder();
					message.append(ses.valueForKeyPath(
							"strings.RujelAutoItog_AutoItog.ui.updateFinished"));
					message.append(' ').append(ai.itogContainer().title());
					ses.takeValueForKey(message.toString(), "message");
					if(delCount > 0) {
						message.delete(0, message.length());
						message.append(ses.valueForKeyPath(
							"strings.RujelAutoItog_AutoItog.ui.itogsDeleted"));
						message.append(' ').append(delCount);
						ses.takeValueForKey(message.toString(), "message");
					}
					ses = null;
				}
				logger.log(WOLogLevel.INFO,"Prognoses update complete",
						new Object[] {ses,ai});
				if(delCount > 0) {
					logger.log(WOLogLevel.MASS_EDITING,"Deleted related ItogMarks: " + delCount,
							new Object[] {ses,ai.itogContainer()});	
				}
				if(!(recalc || delItog)) {
					NSTimestamp fire = ai.fireDateTime();
					if(EOPeriod.Utility.compareDates(fire, null) == 0) {
						if(fire.getTime() - System.currentTimeMillis() > NSLocking.OneMinute) {
							Timer timer = (Timer)WOApplication.application().valueForKey("timer");
							if(timer != null)
								timer.schedule(new AutoItogModule.AutoItogAutomator(ai,false),fire);
							logger.log(WOLogLevel.FINE,"AutoItog scheduled");
						} else {
							AutoItogModule.automateItog(ai);
							AutoItogModule.automateTimedOutPrognoses(ai);
							ses = (sesRef == null)?null:(WOSession)sesRef.get();
							if(ses != null) {
								ses.takeValueForKey(ses.valueForKeyPath(
								"strings.RujelAutoItog_AutoItog.ui.prognosesFired"), "message");
							}
						}
					}
				}
    		} catch (Exception e) {
    			WOSession ses = (sesRef == null)?null:(WOSession)sesRef.get();
				logger.log(WOLogLevel.WARNING,"Error updating prognoses",
						new Object [] {ses,ai,e});
				ec.revert();
				if(ses != null) {
					StringBuilder message = new StringBuilder();
					message.append(ses.valueForKeyPath(
							"strings.RujelAutoItog_AutoItog.ui.updateError"));
					message.append(' ').append(ai.itogContainer().title());
					ses.takeValueForKey(message.toString(), "message");
				}
			} finally {
				ec.unlock();
			}
    	}
    }
}