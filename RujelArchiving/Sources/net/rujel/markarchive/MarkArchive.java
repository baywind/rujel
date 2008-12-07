// MarkArchive.java

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

package net.rujel.markarchive;

import net.rujel.reusables.*;

import com.webobjects.foundation.*;
import com.webobjects.eocontrol.*;
import com.webobjects.eoaccess.*;
import com.webobjects.appserver.WOSession;
import java.util.Enumeration;
import java.util.logging.Logger;

public class MarkArchive extends _MarkArchive
{
	public static final String REASON_KEY = "?";
	
    public MarkArchive() {
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
	
	public void awakeFromInsertion(EOEditingContext ec) {
		super.awakeFromInsertion(ec);
		Number zero = new Integer(0);
		setKey1(zero);
		setKey2(zero);
		setKey3(zero);
		setTimestamp(new NSTimestamp());
		if(ec instanceof SessionedEditingContext) {
			WOSession ses = ((SessionedEditingContext)ec).session();
			setWosid(ses.sessionID());
			String usr = (String)ses.valueForKeyPath("user.present");
			setUser(usr);
		}
	}

	public void setObjectIdentifier(EOEnterpriseObject eo) {
		NSDictionary pKey = objectIdentifierDict(eo);
		if(pKey == null) {
			editingContext().deleteObject(this);
			Logger.getLogger("rujel.markarchive").log(WOLogLevel.INFO,
					"Could not register object for archiving");
			return;
		}
		EOEnterpriseObject usedEntity = getUsedEntity(eo.entityName(), pKey,eo.editingContext());
		setUsedEntity(usedEntity);
		setIdentifierFromDictionary(usedEntity, pKey);
	}

	public static NSDictionary objectIdentifierDict(EOEnterpriseObject eo) {
		EOEditingContext ec = eo.editingContext();
		/*
		if(ec == null)
			ec = eo.editingContext();
		else {
			if(eo.editingContext() != ec)
				eo = EOUtilities.localInstanceOfObject(ec,eo);
		}*/
		NSDictionary pKey = EOUtilities.primaryKeyForObject(ec,eo);
		if(pKey != null && pKey.count() > 0)
			return pKey;
		EOEntity ent = EOUtilities.entityForObject(ec, eo);
		NSMutableArray keys = new NSMutableArray();
		try {
			EOEnterpriseObject usedEntity = EOUtilities.objectMatchingKeyAndValue(ec,"UsedEntity","usedEntity",eo.entityName());
			String key = (String)usedEntity.valueForKey("key1");
			if(key != null) keys.addObject(key);
			key = (String)usedEntity.valueForKey("key2");
			if(key != null) keys.addObject(key);
			key = (String)usedEntity.valueForKey("key3");
			if(key != null) keys.addObject(key);
		} catch (com.webobjects.eoaccess.EOObjectNotAvailableException e) {
			return null;
		}
		NSMutableDictionary keyDict = new NSMutableDictionary();

		NSArray rels = ent.relationships();
		Enumeration enu = rels.objectEnumerator();
		while (enu.hasMoreElements() && keys.count() > 0) {
			EORelationship rel = (EORelationship)enu.nextElement();
			if(rel.isToMany()) continue;
			Enumeration joins = rel.joins().objectEnumerator();
			while(joins.hasMoreElements()) {
				EOJoin join = (EOJoin)joins.nextElement();
				String sa = join.sourceAttribute().name();
				if(keys.containsObject(sa)) {
					EOEnterpriseObject dest = (EOEnterpriseObject)eo.valueForKey(rel.name());
					pKey = EOUtilities.primaryKeyForObject(ec, dest);
					if(pKey != null) {
						String dk = join.destinationAttribute().name();
						keyDict.takeValueForKey(pKey.valueForKey(dk), sa);
						keys.removeObject(sa);
					}
				}
			}
		}
		if(keys.count() > 0) {
			Object[] args = new Object[] {eo,keyDict,keys};
			Logger.getLogger("rujel.archiving").log(WOLogLevel.WARNING,"Could not resolve required attributes for archiving eo",args);
			//editingContext().deleteObject(this);
			return null;
		}
		return keyDict;
	}
	/*
	protected EOEnterpriseObject getUsedEntity (String entityName, NSDictionary identifierDict) {
		EOEditingContext ec = editingContext();
		return getUsedEntity(entityName, identifierDict, ec);
	}
	
	protected static EOEnterpriseObject getUsedEntity(EOEnterpriseObject eo) {
		EOEditingContext ec = eo.editingContext();
		NSDictionary pKey = EOUtilities.primaryKeyForObject(ec, eo);
		return getUsedEntity(eo.entityName(), pKey, ec);
	}*/
	
	protected static EOEnterpriseObject getUsedEntity (String entityName, NSDictionary identifierDict, EOEditingContext ec) {
		EOEnterpriseObject usedEntity = null;
		try {
			usedEntity = EOUtilities.objectMatchingKeyAndValue(ec,"UsedEntity","usedEntity",entityName);
		} catch (com.webobjects.eoaccess.EOObjectNotAvailableException ex) {
			if(identifierDict == null || identifierDict.count() == 0)
				return null;
			usedEntity = EOUtilities.createAndInsertInstance(ec, "UsedEntity");
			usedEntity.takeValueForKey(entityName, "usedEntity");
			Enumeration keys = identifierDict.keyEnumerator();
			if(keys.hasMoreElements())
				usedEntity.takeValueForKey(keys.nextElement(),"key1");
			else
				throw new IllegalArgumentException("Could not generate 'usedEntity' for archiving: no keys found");
			if(keys.hasMoreElements())
				usedEntity.takeValueForKey(keys.nextElement(),"key2");
			if(keys.hasMoreElements())
				usedEntity.takeValueForKey(keys.nextElement(),"key3");
			if(keys.hasMoreElements())
				throw new IllegalArgumentException("Could not generate 'usedEntity' for archiving: more than 3 keys found");
		}
		return usedEntity;
	}
	
	protected void deleteInsertedDuplicates(EOEnterpriseObject usedEntity, NSDictionary identifierDict) {
		EOEditingContext ec = editingContext();
		if(ec instanceof SessionedEditingContext && 
				(((SessionedEditingContext)ec).failuresCount() == 0)) {
			return;
		}
		EOQualifier qual = archiveQualifier(usedEntity, identifierDict);
		Object[] inserted = ec.insertedObjects().objects();
		for (int i = 0; i < inserted.length; i++) {
			if(inserted[i] != this && inserted[i] instanceof MarkArchive
					&& qual.evaluateWithObject((EOEnterpriseObject)inserted[i])) {
				ec.deleteObject((EOEnterpriseObject)inserted[i]);
			}
		}
	}
	
	public void setUsedEntityName(String entityName) {
		EOEnterpriseObject ent = getUsedEntity(entityName, null, editingContext());
		setUsedEntity(ent);
	}
	
	public void setIdentifier(Object key1, Object key2, Object key3) {
			setKey1(keyForObject(key1));
			setKey1(keyForObject(key2));
			setKey1(keyForObject(key3));
	}
	
	protected static Number namedKeyInDict(String key, NSDictionary identifierDict) {
		Object value = identifierDict.valueForKey(key);
		if(value == null && key.endsWith("ID")) {
			key = key.substring(0, key.length() -2);
			value = identifierDict.valueForKey(key);
		}
		return keyForObject(value);
	}
	
	protected static Number keyForObject(Object obj) {
		if(obj instanceof Number) {
			return (Number)obj;
		} else if(obj instanceof EOEnterpriseObject) {
			EOEditingContext ec = ((EOEnterpriseObject)obj).editingContext();
			NSDictionary pKey = EOUtilities.primaryKeyForObject(ec, (EOEnterpriseObject)obj);
			return (Number) pKey.allValues().objectAtIndex(0);
		}
		return new Integer(0);
	}
	
	public void setIdentifierDictionary(NSDictionary identifierDict) {
		if(usedEntity() == null) {
			String entityName = (String)identifierDict.valueForKey("entityName");
			setUsedEntityName(entityName);
		}
		setIdentifierFromDictionary(usedEntity(),identifierDict);
	}
	
	public void setIdentifierFromDictionary (String entityName, NSDictionary identifierDict) {
		EOEnterpriseObject usedEntity = usedEntity();
		if(usedEntity == null || !entityName.equals(usedEntity.valueForKey("usedEntity"))) {
			usedEntity = getUsedEntity(entityName, identifierDict, editingContext());
			//usedEntity = EOUtilities.objectMatchingKeyAndValue(editingContext(),"UsedEntity","usedEntity",entityName);		
		}
		setIdentifierFromDictionary(usedEntity, identifierDict);
	}
	
	public void setIdentifierFromDictionary (EOEnterpriseObject usedEntity, NSDictionary identifierDict) {
		deleteInsertedDuplicates(usedEntity, identifierDict);
		
		//Number zero = new Integer(0);
		String key = (String)usedEntity.valueForKey("key1");
		if(key != null) {
			setKey1(namedKeyInDict(key, identifierDict));
		}
		key = (String)usedEntity.valueForKey("key2");
		if(key != null) {
			setKey2(namedKeyInDict(key, identifierDict));
		}
		key = (String)usedEntity.valueForKey("key3");
		if(key != null) {
			setKey3(namedKeyInDict(key, identifierDict));
		}
	}
	
	public static NSArray archivesForObject(EOEnterpriseObject eo) {
		EOEditingContext ec = eo.editingContext();
		NSDictionary pKey = objectIdentifierDict(eo);
		if(pKey == null)
			return null;
		//EOUtilities.primaryKeyForObject(ec,eo);
		EOEnterpriseObject usedEntity = getUsedEntity(eo.entityName(), pKey, ec);
		//EOUtilities.objectMatchingKeyAndValue(ec,"UsedEntity","usedEntity",eo.entityName());
		EOQualifier qual = archiveQualifier(usedEntity,pKey);
		EOSortOrdering so = EOSortOrdering.sortOrderingWithKey("timestamp", EOSortOrdering.CompareAscending);
		EOFetchSpecification fs = new EOFetchSpecification("MarkArchive",qual,new NSArray(so));
		return ec.objectsWithFetchSpecification(fs);
	}
	/*
	public static EOQualifier archiveQualifier(EOEnterpriseObject eo) {
		EOEditingContext ec = eo.editingContext();
		NSDictionary pKey = objectIdentifierDict(eo);//EOUtilities.primaryKeyForObject(ec, eo);
		EOEnterpriseObject usedEntity = getUsedEntity(eo.entityName(), pKey, ec);
		return archiveQualifier(usedEntity,pKey);
	}*/
	
	public static EOQualifier archiveQualifier(String entityName, NSDictionary identifierDict, EOEditingContext ec) {
		EOEnterpriseObject usedEntity = getUsedEntity(entityName, identifierDict, ec);
		return archiveQualifier(usedEntity, identifierDict);
	}
	
	public static EOQualifier archiveQualifier(EOEnterpriseObject usedEntity, NSDictionary pKey) {
		EOQualifier qual = new EOKeyValueQualifier("usedEntity",EOQualifier.QualifierOperatorEqual, usedEntity);
		NSMutableArray quals = new NSMutableArray(qual);

		Number value = null;
		String key = (String)usedEntity.valueForKey("key1");
		if(key != null) {
			value = namedKeyInDict(key, pKey);
			qual = new EOKeyValueQualifier("key1",EOQualifier.QualifierOperatorEqual,value);
			quals.addObject(qual);
		}
		key = (String)usedEntity.valueForKey("key2");
		if(key != null) {
			value = namedKeyInDict(key, pKey);
			qual = new EOKeyValueQualifier("key2",EOQualifier.QualifierOperatorEqual,value);
			quals.addObject(qual);
		}
		key = (String)usedEntity.valueForKey("key3");
		if(key != null) {
			value = namedKeyInDict(key, pKey);
			qual = new EOKeyValueQualifier("key3",EOQualifier.QualifierOperatorEqual,value);
			quals.addObject(qual);
		}
		qual = new EOAndQualifier(quals);
		return qual;
	}
	/*
	public static String prepareToArchive(EOEnterpriseObject eo) {
		String result = null;
		EOEnterpriseObject usedEntity = EOUtilities.objectMatchingKeyAndValue(eo.editingContext(),"UsedEntity","usedEntity",eo.entityName());
		if(usedEntity != null) {
			String key = (String)usedEntity.valueForKey("toArchive");
			if(key != null)
				result = eo.valueForKey(key).toString();
		}
		if(result == null) {
			NSArray keys = eo.attributeKeys();
			result = eo.valuesForKeys(keys).toString();
		}
		return result;
	}*/
	
	public void setObject(EOEnterpriseObject eo) {
		setObjectIdentifier(eo);
		String key = (String)usedEntity().valueForKey("toArchive");
		if(key != null) {
			takeArchiveValueForKey(eo.valueForKey(key).toString(),key);
		} else {
			Enumeration keys = eo.attributeKeys().objectEnumerator();
			while (keys.hasMoreElements()) {
				key = (String) keys.nextElement();
				Object value = eo.valueForKey(key);
				if(value == null) continue;
				takeArchiveValueForKey(value, key);
			}
			//setArchiveDict(eo.valuesForKeys(keys));
		}
	}
	
	protected NSMutableDictionary dict;
	
	public void setArchiveDict (NSDictionary newDict) {
		dict = newDict.mutableClone();
		setData(NSPropertyListSerialization.stringFromPropertyList(dict));
	}
	
	public NSDictionary getArchiveDictionary() {
		if(dict==null) {
			String data = data();
			if(data == null)
				return null;
				//dict = new NSMutableDictionary();
			else
				dict = NSPropertyListSerialization.dictionaryForString(data).mutableClone();
		}
		return dict.immutableClone();
	}
	
	public String reason() {
		return getArchiveValueForKey(REASON_KEY);
	}
	
	public void setReason(String reason) {
		takeArchiveValueForKey(reason, REASON_KEY);
	}
	
	public void takeValueForKey(Object value, String key) {
		if(key.charAt(0) == '@')
			takeArchiveValueForKey(value, key.substring(1));
		else
			super.takeValueForKey(value, key);
	}
	
	public void takeArchiveValueForKey(Object value, String key) {
		if(value == null) {
			value = ".";
		}
		if(dict == null) {
			String data = data();
			if(data == null)
				dict = new NSMutableDictionary(value,key);
			else {
				dict = NSPropertyListSerialization.dictionaryForString(data).mutableClone();
				dict.setObjectForKey(value, key);
			}
		} else
			dict.setObjectForKey(value, key);
		setData(NSPropertyListSerialization.stringFromPropertyList(dict));
	}
	
	public Object valueForKey(String key) {
		if(key.charAt(0) == '@')
			return getArchiveValueForKey(key.substring(1));
		else
			return super.valueForKey(key);
	}
	
	public String getArchiveValueForKey(String key) {
		if(dict==null) {
			String data = data();
			if(data == null)
				return null;
				//dict = new NSMutableDictionary();
			else
				dict = NSPropertyListSerialization.dictionaryForString(data).mutableClone();
		}
		return (String)dict.valueForKey(key);
	}
	
	public void validateForSave() {
		if(dict != null) {
			setData(NSPropertyListSerialization.stringFromPropertyList(dict));
		}
		super.validateForSave();
	}
	
	public void turnIntoFault(EOFaultHandler handler) {
		dict = null;
		super.turnIntoFault(handler);
	}
}
