// CompletePopup.java: Class file for WO Component 'CompletePopup'

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

package net.rujel.complete;

import java.util.Enumeration;
import java.util.logging.Logger;

import net.rujel.interfaces.EduCourse;
import net.rujel.reusables.NamedFlags;
import net.rujel.reusables.Various;
import net.rujel.reusables.WOLogLevel;

import com.webobjects.appserver.WOActionResults;
import com.webobjects.appserver.WOContext;
import com.webobjects.appserver.WOComponent;
import com.webobjects.eoaccess.EOUtilities;
import com.webobjects.eocontrol.EOEditingContext;
import com.webobjects.foundation.*;

public class CompletePopup extends WOComponent {
	public static Logger logger = Logger.getLogger("rujel.complete");

	public CompletePopup(WOContext context) {
        super(context);
    }
    
    public WOComponent returnPage;
    public EduCourse course;
    public NSArray modules;
    protected Completion[] completions;
    public NSKeyValueCoding item;
    
    public void setCourse(EduCourse eduCourse) {
    	course = eduCourse;
    	modules = (NSArray)session().valueForKeyPath("modules.courseComplete");
    	NSMutableArray list = new NSMutableArray();
    	NSMutableArray ids = new NSMutableArray();
    	NSMutableDictionary precedes = null;
    	NSMutableDictionary localisation = new NSMutableDictionary();
    	if(modules != null && modules.count() > 0) {
    		Enumeration enu = modules.objectEnumerator();
    		while (enu.hasMoreElements()) {
				NSKeyValueCoding mod = (NSKeyValueCoding) enu.nextElement();
				if(!Various.boolForObject(mod.valueForKey("manual")))
					continue;
				String id = (String)mod.valueForKey("id");
				ids.addObject(id);
				NSMutableDictionary dict = new NSMutableDictionary(id,"id");
				dict.takeValueForKey(mod.valueForKey("title"), "title");
				localisation.takeValueForKey(mod.valueForKey("title"), id);
				NSArray requires = (NSArray)mod.valueForKey("requires");
				if(requires != null)
					dict.takeValueForKey(new NSSet(requires), "requires");
				list.addObject(dict);
				requires = (NSArray)mod.valueForKey("precedes");
				if(requires != null) {
					if(precedes == null)
						precedes = new NSMutableDictionary(requires.count());
					Enumeration penu = requires.objectEnumerator();
					while (penu.hasMoreElements()) {
						String prId = (String) penu.nextElement();
						NSMutableSet pSet = (NSMutableSet)precedes.valueForKey(prId);
						if(pSet == null)
							precedes.takeValueForKey(new NSMutableSet(id), prId);
						else
							pSet.addObject(id);
					}
				}
			} // collect manual modules
    	}
    	/*ids.addObject("student");
    	NSMutableDictionary dict = new NSMutableDictionary("student","id");
    	dict.takeValueForKey(session().valueForKeyPath(
    			"strings.RujelComplete_Complete.StudentCatalog"), "title");
    	localisation.takeValueForKey(dict.valueForKey("title"), "student");
    	dict.takeValueForKey(Boolean.TRUE, "manual");
    	list.addObject(dict);*/
    	completions = new Completion[ids.count()];
    	NSMutableDictionary dict = new NSMutableDictionary(course,"course");
    	dict.takeValueForKey(NSKeyValueCoding.NullValue, "student");
    	EOEditingContext ec = course.editingContext();
    	modules = EOUtilities.objectsMatchingValues(ec,
    			Completion.ENTITY_NAME, dict);
    	if(modules == null || modules.count() == 0)
    		return; //TODO
    	NamedFlags access = (NamedFlags)session().valueForKeyPath("readAccess.FLAGS.Completion");
    	NSMutableSet set = new NSMutableSet();
    	Enumeration enu = modules.objectEnumerator();
    	while (enu.hasMoreElements()) {
			Completion cpt = (Completion) enu.nextElement();
			String id = cpt.aspect();
			int idx = ids.indexOf(id);
			if(idx < 0)
				continue;
			completions[idx] = cpt;
			dict = (NSMutableDictionary)list.objectAtIndex(idx);
			boolean closed = !cpt.isNotClosed();
			dict.takeValueForKey(Boolean.valueOf(closed), "closed");
			if(closed) {
				dict.takeValueForKey(Boolean.valueOf(!access.flagForKey("edit")), "disabled");
				set.addObject(id);
				dict.takeValueForKey("gerade", "styleClass");
			} else {
				dict.takeValueForKey(Boolean.valueOf(!access.flagForKey("create")), "disabled");
				dict.takeValueForKey("ungerade", "styleClass");
				if(cpt.whoClosed() == null)
					continue;
			}
			dict.takeValueForKey(cpt.present(), "hover");
		} // read database for current state
    	for (int i = 0; i < completions.length; i++) {
			dict = (NSMutableDictionary)list.objectAtIndex(i);
			if(completions[i] == null) {
				logger.log(WOLogLevel.INFO,"Creating missing completion",
						new Object[] {session(),dict});
				completions[i] = (Completion)EOUtilities.createAndInsertInstance(ec,
						Completion.ENTITY_NAME);
				completions[i].setCourse(course);
				completions[i].setAspect((String)dict.valueForKey("id"));
				ec.saveChanges();
			}
			if(Various.boolForObject(dict.valueForKey("disabled")) || !completions[i].isNotClosed()) {
				continue;
			}
			NSSet requires = (NSSet)dict.valueForKey("requires");
			if(precedes != null) {
				NSMutableSet pReqs = (NSMutableSet)precedes.objectForKey(dict.valueForKey("id"));
				if(pReqs != null) {
					if(requires != null)
						pReqs.unionSet(requires);
					requires = pReqs;
					dict.takeValueForKey(requires, "requires");
				}
			}
			if(requires != null && !requires.isSubsetOfSet(set)) {
				dict.takeValueForKey(Boolean.TRUE, "disabled");
				enu = requires.objectEnumerator();
				String title = (String)session().valueForKeyPath(
						"strings.RujelComplete_Complete.requires");
				StringBuilder buf = new StringBuilder(title);
				buf.append(':').append(' ');
				String req = (String)enu.nextElement();
				title = (String)localisation.valueForKey(req);
				if(title == null)
					title = req;
				buf.append(title);
				while (enu.hasMoreElements()) {
					req = (String)enu.nextElement();
					title = (String)localisation.valueForKey(req);
					if(title == null)
						title = req;
					buf.append(", ").append(title);
				}
				dict.takeValueForKey(buf.toString(), "hover");
			} // check requirements
		}
    	modules = list;
    }
    
    public WOActionResults save() {
    	boolean changed = false;
    	for (int i = 0; i < completions.length; i++) {
    		NSMutableDictionary dict = (NSMutableDictionary)modules.objectAtIndex(i);
    		boolean closed = Various.boolForObject(dict.valueForKey("closed"));
    		if(closed && completions[i] == null) {
    			completions[i] = (Completion)EOUtilities.createAndInsertInstance(
    					course.editingContext(), Completion.ENTITY_NAME);
    			completions[i].setCourse(course);
    			completions[i].setAspect((String)dict.valueForKey("id"));
    		} else if(closed != completions[i].isNotClosed()) {
    			continue;
    		}
    		changed = true;
    		completions[i].setCloseDate(closed? new NSTimestamp() : null);
    		completions[i].setWhoClosed((String)session().valueForKeyPath("user.present"));
    	}
    	if(changed) {
    		try {
				course.editingContext().saveChanges();
	    		Executor.Task executor = new Executor.Task();
	    		executor.date = session().valueForKey("eduYear");
	    		executor.setCourse(course);
	    		Executor.exec(executor);
				NSArray addOns = (NSArray)session().objectForKey("notesAddOns");
				if(addOns != null)
					addOns.valueForKey("dropCompletionAgregate");
			} catch (Exception e) {
				course.editingContext().revert();
				logger.log(WOLogLevel.WARNING,"Error saving completions", 
						new Object[] {session(),completions,e});
				session().takeValueForKey(e.getMessage(), "message");
			}
    	}
    	returnPage.ensureAwakeInContext(context());
    	return returnPage;
    }
}