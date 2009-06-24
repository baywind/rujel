//ReprimList.java: Class file for WO Component 'ReprimList'

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

package net.rujel.curriculum;

import java.util.logging.Logger;

import net.rujel.base.MyUtility;
import net.rujel.interfaces.EduCourse;
import net.rujel.reusables.Various;
import net.rujel.reusables.WOLogLevel;

import com.webobjects.appserver.*;
import com.webobjects.eoaccess.EOUtilities;
import com.webobjects.eocontrol.EOEditingContext;
import com.webobjects.eocontrol.EOGlobalID;
import com.webobjects.eocontrol.EOKeyGlobalID;
import com.webobjects.eocontrol.EOSortOrdering;
import com.webobjects.foundation.*;

//Generated by the WOLips Templateengine Plug-in at Jun 4, 2009 4:50:03 PM
public class ReprimList extends com.webobjects.appserver.WOComponent {
	protected static final Logger logger = Logger.getLogger("rujel.curriculum");
	protected final String relieve = (String)application().valueForKeyPath(
			"strings.RujelCurriculum_Curriculum.ReprimList.relieve");
	protected final String unrelieve = (String)application().valueForKeyPath(
			"strings.RujelCurriculum_Curriculum.ReprimList.unrelieve");

	public WOComponent returnPage;
	public EduCourse course;
	public NSArray list;
	public Reprimand item;
	public String text;
	public Integer ident;
	protected boolean changed = false;

	public ReprimList(WOContext context) {
		super(context);
	}

	public void setCourse(EduCourse eduCourse) {
		course = eduCourse;
		if(list != null)
			return;
		list = EOUtilities.objectsMatchingKeyAndValue(course.editingContext(),
				Reprimand.ENTITY_NAME, "course", course);
		if(list != null && list.count() > 1) {
			EOSortOrdering so = new EOSortOrdering(Reprimand.RAISED_KEY, 
					EOSortOrdering.CompareDescending);
			list = EOSortOrdering.sortedArrayUsingKeyOrderArray(list, new NSArray(so));
		}
    }

	public String styleClass() {
		if(item.relief() == null)
			return "ungerade";
		return "gerade";
	}
	
/*	public String content() {
		String result = (String)valueForKeyPath("item.content");
		if(result == null)
			return null;
		result = WOMessage.stringByEscapingHTMLString(result);
		result = result.replaceAll("\n", "<br/>");
		return result;
	}*/
    
    public void toggleRelief() {
    	if(item == null)
    		return;
    	String message = null;
    	if(item.relief() == null) {
    		item.setRelief(new NSTimestamp());
    		String usr = (String)session().valueForKeyPath("user.present");
    		if(usr != null && !item.author().contains(usr)) {
    			item.setAuthor(item.author() + " / " + usr);
    		}
    		message = "Setting relief to Reprimand";
    	} else {
    		message = "Unsetting relief on Reprimand was " + item.relief();
    		item.setRelief(null);
    	}
    	try {
    		item.editingContext().saveChanges();
    		logger.log(WOLogLevel.UNOWNED_EDITING,message, new Object[] {session(),item});
    		changed = true;
    	} catch (Exception e) {
    		logger.log(WOLogLevel.WARNING,"Error " + message, 
    				new Object[] {session(),item, e});
    		item.editingContext().revert();
		}
    }
    
    public void save() {
    	EOEditingContext ec = course.editingContext();
		String usr = (String)session().valueForKeyPath("user.present");
		String addInfo = null;
    	if(ident == null) {
    		item = (Reprimand)EOUtilities.createAndInsertInstance(ec, Reprimand.ENTITY_NAME);
    		item.setAuthor(usr);
    		item.addObjectToBothSidesOfRelationshipWithKey(course, "course");
    		changed = true;
    	} else {
    		item = (Reprimand)EOUtilities.objectWithPrimaryKeyValue(
    				ec, Reprimand.ENTITY_NAME, ident);
    		if(System.currentTimeMillis() - item.raised().getTime() > MyUtility.dayMillis)
    			addInfo = MyUtility.dateFormat().format(new NSTimestamp());
    		if(usr != null && !usr.equals(item.author())) {
    			if(addInfo == null) {
    				addInfo = usr;
    			} else {
    				addInfo = usr + " - " + addInfo;
    			}
    		}
    		if(addInfo != null)
    			text = text + " (" + addInfo + ')';
    		addInfo = item.content();
    	}
		item.setContent(text);
		text = (ident == null)?"Creating new Reprimand":
								"Editing Reprimand";
    	try {
    		item.editingContext().saveChanges();
    		logger.log(WOLogLevel.UNOWNED_EDITING,text,(addInfo==null)?
    				new Object[] {session(),item}:new Object[] {session(),item,addInfo});
    	} catch (Exception e) {
    		logger.log(WOLogLevel.WARNING,"Error " + text, new Object[] {
    				session(),(ident == null)?course:item, e});
    		item.editingContext().revert();
		}
    	if(ident == null) {
    		if(!(list instanceof NSMutableArray)) {
    			list = list.mutableClone();
    		}
			((NSMutableArray)list).insertObjectAtIndex(item, 0);
    	}
		item = null;
		text = null;
		ident = null;
    }
    
    public String relievOnClick() {
    	if(Various.boolForObject(session().valueForKeyPath("readAccess._delete.item")))
    		return "closePopup();";
    	StringBuilder buf = new StringBuilder("if(confirmAction('");
    	if(item.relief() == null)
    		buf.append(relieve);
    	else
    		buf.append(unrelieve);
    	buf.append("',event))ajaxPopupAction('");
    	buf.append(context().componentActionURL());
    	buf.append("',document.getElementById('ajaxPopup'));");
    	return buf.toString();
    }
    
    public String editOnClick() {
    	if(Various.boolForObject(session().valueForKeyPath("readAccess._create.item")) || 
    			Various.boolForObject(session().valueForKeyPath("readAccess._edit.item")))
    		return null;
    	if(item == null)
    		return null;
    	EOGlobalID gid = item.editingContext().globalIDForObject(item);
    	if(gid.isTemporary())
    		return null;
    	Object key = ((EOKeyGlobalID)gid).keyValues()[0];
    	StringBuilder buf = new StringBuilder("editReprimand(this,");
       	buf.append(key);
       	buf.append(')');
    	return buf.toString();   	
    }
    
    public String closeOnClick() {
    	if(changed)
    		return (String)session().valueForKey("tryLoad");
    	return "closePopup();";
    }
    
    public WOActionResults closePopup() {
    	returnPage.ensureAwakeInContext(context());
    	return returnPage;
    }
}