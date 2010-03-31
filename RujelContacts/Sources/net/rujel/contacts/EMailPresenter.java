// EMailPresenter.java

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

package net.rujel.contacts;

import net.rujel.interfaces.*;
import net.rujel.reusables.*;

import com.webobjects.foundation.*;
import com.webobjects.appserver.*;
import com.webobjects.eocontrol.*;
import com.webobjects.eoaccess.*;

import java.util.logging.Logger;
import net.rujel.reusables.WOLogLevel;

public class EMailPresenter extends WOComponent {
	protected static Logger logger = Logger.getLogger("rujel.journal");

    /** @TypeInfo Contact */
    public Contact item;

    public EMailPresenter(WOContext context) {
        super(context);
    }
	
	protected EOEditingContext _ec;
	protected EOEditingContext ec() {
		if(_ec == null) {
			PersonLink pers = (PersonLink)valueForBinding("person");
			_ec = ((EOEnterpriseObject)pers).editingContext();
		}
		return _ec;
	}
	
	protected NamedFlags _access;
	public NamedFlags access() {
		if(_access == null) {
			_access = (NamedFlags)valueForBinding("access");
			if(_access == null)
				_access = DegenerateFlags.ALL_TRUE;
		}
		return _access;
	}
	
	public boolean selected() {
		Object sel = valueForBinding("selection");
		return (sel != null && sel == item);
	}
	
	public void select() {
		if(access().flagForKey("edit"))
			setValueForBinding(item,"selection");
	}
	
	public void save() {
		item = (Contact)valueForBinding("selection");
		if(ec().hasChanges()) {
			try {
				ec().saveChanges();
				logger.logp(WOLogLevel.OWNED_EDITING,"EMailPresenter","save","Saved email address",new Object[] {session(),item});
				setValueForBinding(null,"selection");
			} catch (Exception ex) {
				logger.logp(WOLogLevel.FINER,"SubgroupEditor","save","Failed to save email address",new Object[] {session(),item,ex});
				session().takeValueForKey(ex.toString(),"message");
			}
		} else {
			setValueForBinding(null,"selection");
		}
	}
	
	public void undo() {
		//item = (Contact)valueForBinding("selection");
		//NSArray ins = ec().insertedObjects();
		NSMutableArray list = (NSMutableArray)valueForBinding("list");
		/*if(ins != null && ins.count() > 0) {
			list.removeObjectsInArray(ins);
		}*/
		if(ec().hasChanges()) {
			NSArray changed = ec().updatedObjects();
			ec().revert();
			if(changed != null && changed.count() > 0) {
				java.util.Enumeration enu = changed.objectEnumerator();
				while(enu.hasMoreElements()) {
					item = (Contact)enu.nextElement();
					item.getUtiliser().reset();
				}
			}
		}
		PersonLink pers = (PersonLink)valueForBinding("person");
		list.setArray(Contact.getContactsForPerson(pers.person(),EMailUtiliser.conType(ec())));
			
		setValueForBinding(null,"selection");
	}
	
	public void add() {
		if(!access().flagForKey("create"))
			return;
		if(ec().hasChanges()) {
			item = (Contact)valueForBinding("selection");
			if(item != null) {
				try {
					item.validateForSave();
				}catch (Exception ex) {
					session().takeValueForKey(ex.toString(),"message");
					return;
				}
			}
			
		}
		PersonLink pers = (PersonLink)valueForBinding("person");
//		EOEditingContext ec = ((EOEnterpriseObject)pers).editingContext();
		item = (Contact)EOUtilities.createAndInsertInstance(ec(),"Contact");
		item.setPerson(pers.person());
		item.setType(EMailUtiliser.conType(ec()));
		NSMutableArray list = (NSMutableArray)valueForBinding("list");
		if(list == null) {
			list = new NSMutableArray(item);
			setValueForBinding(list,"list");
		} else {
			list.addObject(item);
		}
		select();
	}
	
	public void delete() {
		item = (Contact)valueForBinding("selection");
		if(item == null) return;
		if(!access().flagForKey("delete"))
		return;
		if(ec().insertedObjects().containsObject(item)) {
			undo();
		} else {
			NSMutableArray list = (NSMutableArray)valueForBinding("list");
			list.removeObject(item);
			int id = item.persID().intValue();
			item.setPersID(new Integer(-id));
			try {
				ec().saveChanges();
				logger.logp(WOLogLevel.OWNED_EDITING,"EMailPresenter","save","Removed email address",new Object[] {session(),item});
			} catch (Exception ex) {
				logger.logp(WOLogLevel.FINER,"SubgroupEditor","save","Failed to remove email address",new Object[] {session(),item,ex});
				session().takeValueForKey(ex.toString(),"message");
			}
		}
		setValueForBinding(null,"selection");
	}
	
	public String styleClass() {
		if(item == null) return null;
		if(item.getUtiliser().flags().flagForKey("disabled"))
			return "grey";
		else if(item.getUtiliser().flags().flagForKey("subscribe"))
			return "gerade";
		else 
			return "ungerade";
	}
	
	public boolean isStateless() {
		return true;
	}
	
	public boolean synchronizesVariablesWithBindings() {
        return false;
	}
	
	public void reset() {
		item = null;
		_ec = null;
		_access = null;
	}	
}
