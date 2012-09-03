// Contact.java

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

import net.rujel.reusables.*;
import net.rujel.base.EntityIndex;
import net.rujel.interfaces.*;
import com.webobjects.foundation.*;
import com.webobjects.eocontrol.*;
import com.webobjects.eoaccess.EOUtilities;
//import com.webobjects.eoaccess.EOObjectNotAvailableException;
import java.util.Enumeration;
import java.util.logging.Logger;
import java.lang.reflect.Constructor;

public class Contact extends _Contact {
	//protected static Logger logger = Logger.getLogger("rujel.contacts");
	
    public Contact() {
        super();
    }

/*
    // If you add instance variables to store property values you
    // should add empty implementions of the Serialization methods
    // to avoid unnecessary overhead (the properties will be
    // serialized for you in the superclass).
    private void writeObject(java.io.ObjectOutputStream out) throws java.io.IOException {
    }

    private void readObject(java.io.ObjectInputStream in) throws java.io.IOException, java.lang.ClassNotFoundException {
    }
*/
	public void turnIntoFault(EOFaultHandler handler) {
		_pers = null;
		_utiliser = null;
		super.turnIntoFault(handler);
	}
	
	public void awakeFromInsertion(EOEditingContext ec) {
		super.awakeFromInsertion(ec);
		setFlags(Integer.valueOf(0));
	}
	
	public String validateContact(Object aValue) {
		return getUtiliser().validateContact(aValue);
	}
	
	private transient PersonLink _pers;
	
	public PersonLink person() {
		if(_pers == null) {
			String entityName = personEntity().entName();
			//(String)valueForKeyPath("personEntity.entName");
			if(entityName == null || persID() == null) return null;
			_pers = (PersonLink)EOUtilities.objectWithPrimaryKeyValue(
					editingContext(),entityName,persID());
		}
		return _pers;
	}

	public void setPerson(PersonLink person) {
		_pers = null;
		if(person == null) {
			setPersonEntity(null);
			setPersID(null);
			return;
		}
		EOEnterpriseObject eo = (EOEnterpriseObject)person;
		if(eo.editingContext() != editingContext()) {
			person = (PersonLink)EOUtilities.localInstanceOfObject(editingContext(),eo);
		}
		
		//String entityName = person.entityName();
		/*EOEnterpriseObject pEnt;
		try {
			pEnt = EOUtilities.objectMatchingKeyAndValue(editingContext(),"PersonEntity","personEntityName",entityName);
		} catch (EOObjectNotAvailableException nax) {
			pEnt = EOUtilities.createAndInsertInstance(editingContext(),"PersonEntity");
			pEnt.takeValueForKey(entityName,"personEntityName");
			logger.log(WOLogLevel.EDITING,"Adding new personEntity '" + entityName + "' to list",pEnt);
		}*/
		EOEnterpriseObject pEnt = EntityIndex.indexForObject(eo,true);
		/*if(pEnt == null) {
			pEnt = EOUtilities.createAndInsertInstance(editingContext(),"PersonEntity");
			pEnt.takeValueForKey(entityName,"personEntityName");
			logger.log(WOLogLevel.EDITING,"Adding new personEntity '" + entityName + "' to list for person",person);
		}*/
		setPersonEntity(pEnt);
		setPersID(idForPerson(person));
		_pers = person;
	}
	/*
	protected static EOEnterpriseObject entityForPerson(Person person) {
		String entityName = person.entityName();
		EOEditingContext ec = person.editingContext();
		EOEnterpriseObject pEnt;
		try {
			pEnt = EOUtilities.objectMatchingKeyAndValue(ec,"PersonEntity","personEntityName",entityName);
		} catch (EOObjectNotAvailableException nax) {
			return null;
		}
		return pEnt;
	}*/
	
	protected static Integer idForPerson(PersonLink person) {
		EOEditingContext ec = ((EOEnterpriseObject)person).editingContext();
		NSDictionary pKey = EOUtilities.primaryKeyForObject(ec,(EOEnterpriseObject)person);
		if(pKey == null || pKey.count() != 1)
			throw new IllegalArgumentException("Person entity should not have compound primary key");
		return (Integer)pKey.allValues().objectAtIndex(0);
	}
	
	public static EOQualifier qualifierForPerson(PersonLink person) {
		EOEnterpriseObject ent = EntityIndex.indexForObject((EOEnterpriseObject)person,false);
		if(ent == null) return null;
		EOQualifier[] qual = new EOQualifier[2];
		qual[0] = new EOKeyValueQualifier(
				PERSON_ENTITY_KEY,EOQualifier.QualifierOperatorEqual,ent);
		qual[1] = new EOKeyValueQualifier(
				PERS_ID_KEY,EOQualifier.QualifierOperatorEqual,idForPerson(person));
		return new EOAndQualifier(new NSArray(qual));
	}
	
	public static NSArray getContactsForPerson(PersonLink person, EOEnterpriseObject type) {
		EOEnterpriseObject ent = EntityIndex.indexForObject((EOEnterpriseObject)person,false);
		if(ent == null) return null;
		NSMutableDictionary dict = new NSMutableDictionary();
		dict.setObjectForKey(ent,PERSON_ENTITY_KEY);
		dict.setObjectForKey(idForPerson(person),PERS_ID_KEY);
		if(type != null) {
			dict.setObjectForKey(type,TYPE_KEY);
		}
		return EOUtilities.objectsMatchingValues(
				((EOEnterpriseObject)person).editingContext(),ENTITY_NAME,dict);
		
		/*
		EOQualifier qual = qualifierForPerson(person);
		if(qual == null) return null;
		EOFetchSpecification fs = new EOFetchSpecification("Contact",qual,null);
		return person.editingContext().objectsWithFetchSpecification(fs);*/
	}
	
	public static PerPersonLink getContactsForList(NSArray list,
			EOEnterpriseObject type, Boolean descend) {
		if(list == null || list.count() == 0)
			return null;
		NSMutableDictionary result = new NSMutableDictionary();
		//EOEditingContext ec = ((EOEnterpriseObject)list.objectAtIndex(0)).editingContext();
		Enumeration enu = list.objectEnumerator();
		NSMutableDictionary ents = new NSMutableDictionary();
		EOQualifier [] quals = new EOQualifier[4];
		quals[1] = new EOKeyValueQualifier(FLAGS_KEY, 
				EOQualifier.QualifierOperatorLessThan, new Integer(32));
		if(type != null) {
			quals[0] = new EOKeyValueQualifier(TYPE_KEY,
					EOQualifier.QualifierOperatorEqual, type);
		}
		while (enu.hasMoreElements()) {
			PersonLink pl = (PersonLink)enu.nextElement();
			EOEnterpriseObject person = (descend == null || descend.booleanValue())?pl.person()
					:(EOEnterpriseObject)pl;
			Object pEnt = ents.objectForKey(person.entityName());
			if(pEnt == null) {
				pEnt = EntityIndex.indexForObject(person,false);
				if(pEnt == null) pEnt = NullValue;
				ents.setObjectForKey(pEnt,person.entityName());
			}
			Object plEnt = null;
			if(descend == null) {
				plEnt = ents.objectForKey(((EOEnterpriseObject)pl).entityName());
				if(plEnt == null) {
					plEnt = EntityIndex.indexForObject(((EOEnterpriseObject)pl),false);
					if(plEnt == null)
						plEnt = NullValue;
					ents.setObjectForKey(pEnt,person.entityName());
				}
			}
			if(pEnt == NullValue || plEnt == NullValue)
				continue;
			if(pl == person || plEnt == null) {
				quals[2] = new EOKeyValueQualifier(PERSON_ENTITY_KEY, 
						EOQualifier.QualifierOperatorEqual, pEnt); 
				quals[3] = new EOKeyValueQualifier(PERS_ID_KEY, 
						EOQualifier.QualifierOperatorEqual, idForPerson((PersonLink)person));
			} else if(descend == null) {
				NSArray args = new NSArray(new Object[] {
						pEnt,idForPerson((PersonLink)person),plEnt,idForPerson(pl)});
				quals[2] = EOQualifier.qualifierWithQualifierFormat(
		"(personEntity = %@ AND persID = %@) OR (personEntity = %@ AND persID = %@)",args);
				quals[3] = null;
			}
			quals[3] = new EOAndQualifier(new NSArray(quals));
			EOFetchSpecification fs = new EOFetchSpecification(ENTITY_NAME,quals[3],null);
			NSArray contacts = person.editingContext().objectsWithFetchSpecification(fs);
			if(contacts.count() > 0)
				result.setObjectForKey(contacts,person);
		}
		if(result.count() == 0) return null;
		return new PerPersonLink.Dictionary(result) {
			public Object forPersonLink(PersonLink pers) {
				return super.forPersonLink(pers.person());
			}
		};
		//PersDictionary(result);
	}
	/*
	protected static class PersDictionary extends PerPersonLink.Dictionary {
		public PersDictionary(NSDictionary dict) {
			super(dict);
		}
		
		public Object forPersonLink(PersonLink pers) {
			return super.forPersonLink(pers.person());
		}
	}*/
	
	public static interface Utiliser {
		public Contact contact();
		public NamedFlags flags();
		public String presenter();
		public String validateContact(Object aValue);
		public String present();
		public void reset();
	}
	
	protected transient Utiliser _utiliser;
	public Utiliser getUtiliser() {
		if(_utiliser == null) {
			String uName = (String)type().valueForKey("utiliserClass");
			if(uName == null) {
				return null;
			}
			try {
				Class uClass = Class.forName(uName);
				Constructor uConstructor = uClass.getConstructor(Contact.class);
				_utiliser = (Utiliser)uConstructor.newInstance(this);
			} catch (Exception ex) {
				Logger.getLogger("rujel.contacts").log(WOLogLevel.WARNING,
						"Failed to initialise Contact.Utiliser for name " + uName,
						new Object[] {this,ex});
			}
		}
		return _utiliser;
	}
}
