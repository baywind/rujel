// Contacts.java

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


import net.rujel.reusables.*;
import net.rujel.interfaces.*;
import net.rujel.contacts.*;

import com.webobjects.foundation.*;
import com.webobjects.appserver.*;
import com.webobjects.eocontrol.*;
import com.webobjects.eoaccess.*;

import java.lang.reflect.Field;
import java.util.Enumeration;
import java.util.logging.Logger;
import net.rujel.reusables.WOLogLevel;

public class Contacts extends WOComponent {
	protected static Logger logger = Logger.getLogger("rujel.contacts");

	public EOEditingContext ec;
    public EduGroup currClass;
    public PersonLink currPerson;
	public EOEnterpriseObject currConType;

	public NSArray contypes;
	public PerPersonLink allContacts;
    public NSKeyValueCoding typeItem;
	private NSMutableDictionary contactsByType;
	public Contact selectedContact;

 	protected NamedFlags _access;
	//public static final NSArray accessKeys = new NSArray(new Object[] {
	//	"read","create","edit","delete"});
	
	public NamedFlags access() {
		if (_access == null) return DegenerateFlags.ALL_TRUE;
		return (NamedFlags)_access.immutableClone();
	}

	public Contacts(WOContext context) {
        super(context);
		ec = new SessionedEditingContext(session());
		ec.lock();/*
		UserPresentation user = (UserPresentation)session().valueForKey("user");
		try {
			int acc = user.accessLevel("Contacts");
			if (acc == 0) throw new AccessHandler.UnlistedModuleException("Zero access");
			_access = new NamedFlags(acc,accessKeys);
		} catch (AccessHandler.UnlistedModuleException e) {
				logger.logp(WOLogLevel.CONFIG,this.getClass().getName(),"<init>","Can't get accessLevel",session());
				_access = DegenerateFlags.ALL_TRUE;
		}*/
		_access = (NamedFlags)session().valueForKeyPath("readAccess.FLAGS.Contacts");
		ec.setSharedEditingContext(EOSharedEditingContext.defaultSharedEditingContext());
		ec.unlock();
		contypes = EOUtilities.objectsForEntityNamed(ec,"ConType");
		
    }
	
	public String title() {
		return (String)valueForKeyPath("application.extStrings.RujelContacts_Contacts.title");
	}

    public WOComponent selectClass() {
		currPerson = null;
        allContacts = Contact.getContactsForList(currClass.list(),currConType);
		contactsByType = null;
		selectedContact = null;
		revert();
		return null;
    }

    public WOComponent selectPerson() {
		//if(contactsByType == null)
			contactsByType = new NSMutableDictionary();
		NSArray persContacts = Contact.getContactsForPerson(currPerson.person(),null);
		if(persContacts != null && persContacts.count() > 0) {
			Enumeration enu = persContacts.objectEnumerator();
			while (enu.hasMoreElements()) {
				Contact con = (Contact)enu.nextElement();
				NSMutableArray ofType = (NSMutableArray)contactsByType.objectForKey(con.type());
				if(ofType == null) {
					ofType = new NSMutableArray(con);
					contactsByType.setObjectForKey(ofType,con.type());
				} else {
					ofType.addObject(con);
				}
			}
		}
		selectedContact = null;
		revert();
        return null;
    }
	
	public NSArray listOfType() {
		if(contactsByType == null)
			return null;
		return (NSArray)contactsByType.objectForKey(typeItem);
	}
	
	public void setListOfType(NSArray list) {
		if(contactsByType == null) {
			contactsByType = new NSMutableDictionary(list,typeItem);
		} else {
			contactsByType.setObjectForKey(list,typeItem);
		}
	}
	
	public PerPersonLink listContacts() {
		if(currPerson != null || currClass == null || allContacts == null || contypes == null || contypes.count() == 0)
			return null;
		NSMutableDictionary template = new NSMutableDictionary("DynamicCell","presenter");
		
		Enumeration penu = currClass.list().objectEnumerator();
		NSMutableDictionary result = new NSMutableDictionary();
		while(penu.hasMoreElements()) {
			PersonLink pers = (PersonLink)penu.nextElement();
			
			NSMutableDictionary[] res = new NSMutableDictionary[contypes.count()];
			for (int i = 0; i < res.length; i++) {
				res[i] = template.mutableClone();
			}
			NSArray persContacts = (NSArray)allContacts.forPersonLink(pers);
			if(persContacts != null && persContacts.count() > 0) {
				Enumeration enu = persContacts.objectEnumerator();
				while (enu.hasMoreElements()) {
					Contact con = (Contact)enu.nextElement();
					int idx = contypes.indexOfIdenticalObject(con.type());
					if(idx >= 0) {
						NSMutableDictionary dict = res[idx];
						String str = (String)dict.objectForKey("string");
						String out =con.getUtiliser().present();
						if(str == null) {
							str = out;
						} else {
							str = str + "<br/>\r" + out;
						}
						dict.setObjectForKey(str,"string");
					}
				}
			}
			result.setObjectForKey(new NSArray(res),pers);
		}
		return new PerPersonLink.Dictionary(result);
	}
	
	public String presenter() {
		if(typeItem == null) return null;
		String uName = (String)typeItem.valueForKey("utiliserClass");
		try {
			Class uClass = Class.forName(uName);
			Field pFied = uClass.getField("presenter");
			return (String)pFied.get(null);
		} catch (Exception ex) {
			logger.log(WOLogLevel.WARNING,"Failed to initialise Contact.Utiliser for name " + uName,ex);
			return null;
		}
	}
	
	public void revert() {
		if(ec.hasChanges()) {
			NSArray changed = ec.updatedObjects();
			ec.revert();
			if(changed != null && changed.count() > 0) {
				java.util.Enumeration enu = changed.objectEnumerator();
				while(enu.hasMoreElements()) {
					Contact item = (Contact)enu.nextElement();
					item.getUtiliser().reset();
				}
			}
		}
	}
}
