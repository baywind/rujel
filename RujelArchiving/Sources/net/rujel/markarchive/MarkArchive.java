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

import java.lang.ref.WeakReference;
import java.util.Enumeration;
import java.util.logging.Logger;

public class MarkArchive extends _MarkArchive
{
	public static final String REASON_KEY = "?";
	protected static final Logger logger = Logger.getLogger("rujel.markarchive");
	
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

    private void readObject(java.io.ObjectInputStream in) 
    throws java.io.IOException, java.lang.ClassNotFoundException {
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
			logger.log(WOLogLevel.FINER, "Archiving delayed for new object",eo);
			if(dict == null)
				dict = new NSMutableDictionary();
			waiterForEc(eo.editingContext()).registerArchive(dict, eo);
			return;
		}
		EOEnterpriseObject usedEntity = getUsedEntity(eo.entityName(), eo.editingContext());
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
		pKey = CompoundPKeyGenerator.compoundKey(eo);
		if(pKey != null && pKey.count() > 0)
			return pKey;
		return null;
		/*EOEntity ent = EOUtilities.entityForObject(ec, eo);
		NSMutableArray pKeys = new NSMutableArray();
		EOEnterpriseObject usedEntity = getUsedEntity(eo.entityName(), ec);
		for (int i = 0; i < keys.length; i++) {
			String key = (String)usedEntity.valueForKey(keys[i]);
			if(key != null) pKeys.addObject(key);
		}

		NSMutableDictionary keyDict = new NSMutableDictionary();
		NSArray rels = ent.relationships();
		Enumeration enu = rels.objectEnumerator();
		while (enu.hasMoreElements() && pKeys.count() > 0) {
			EORelationship rel = (EORelationship)enu.nextElement();
			if(rel.isToMany()) continue;
			Enumeration joins = rel.joins().objectEnumerator();
			while(joins.hasMoreElements()) {
				EOJoin join = (EOJoin)joins.nextElement();
				String sa = join.sourceAttribute().name();
				if(pKeys.containsObject(sa)) {
					EOEnterpriseObject dest = (EOEnterpriseObject)eo.valueForKey(rel.name());
					pKey = EOUtilities.primaryKeyForObject(ec, dest);
					if(pKey != null) {
						String dk = join.destinationAttribute().name();
						keyDict.takeValueForKey(pKey.valueForKey(dk), sa);
						pKeys.removeObject(sa);
					}
				}
			}
		}
		if(pKeys.count() > 0) {
			Object[] args = new Object[] {eo,keyDict,pKeys};
			logger.log(WOLogLevel.WARNING,
					"Could not resolve required attributes for archiving eo",args);
			//editingContext().deleteObject(this);
			return null;
		}
		return keyDict;*/
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

	protected static NSMutableDictionary<String, EOGlobalID> entities 
										= new NSMutableDictionary<String, EOGlobalID>();
	protected static EOEnterpriseObject getUsedEntity (String entityName, EOEditingContext ec) {
		EOGlobalID entGID = entities.objectForKey(entityName);
		EOEnterpriseObject usedEntity = null;
		if(entGID != null)
			usedEntity = ec.faultForGlobalID(entGID,ec);
		if(usedEntity != null && !entGID.isTemporary())
			return usedEntity;
		NSArray found = EOUtilities.objectsMatchingKeyAndValue(ec,
				"UsedEntity","usedEntity",entityName);
		if(found == null || found.count() == 0) {
			if(usedEntity != null)
				return usedEntity;
			EOEntity entity = EOModelGroup.defaultGroup().entityNamed(entityName);
			NSArray pKeys = entity.primaryKeyAttributeNames();
			if(pKeys == null || pKeys.count() == 0 || pKeys.count() > 3)
				throw new IllegalArgumentException(
						"Could not generate 'usedEntity' for archiving: illegal entityKeys");					
			usedEntity = EOUtilities.createAndInsertInstance(ec, "UsedEntity");
			logger.log(WOLogLevel.COREDATA_EDITING,
				"Registering new archivable entity :" + entityName,pKeys);
			usedEntity.takeValueForKey(entityName, "usedEntity");
			Enumeration enu = pKeys.objectEnumerator();
			if(enu.hasMoreElements())
				usedEntity.takeValueForKey(enu.nextElement(),"key1");
			else
				throw new IllegalArgumentException(
						"Could not generate 'usedEntity' for archiving: no keys found");
			if(enu.hasMoreElements())
				usedEntity.takeValueForKey(enu.nextElement(),"key2");
			if(enu.hasMoreElements())
				usedEntity.takeValueForKey(enu.nextElement(),"key3");
		} else {
			if(found.count() > 1)
				logger.log(WOLogLevel.WARNING,
						"Found several descriptions for entity named:" + entityName);
			usedEntity = (EOEnterpriseObject)found.objectAtIndex(0);
		}
		entGID = ec.globalIDForObject(usedEntity);
		entities.setObjectForKey(entGID, entityName);
		return usedEntity;
	}

	protected void deleteInsertedDuplicates(
			EOEnterpriseObject usedEntity, NSDictionary identifierDict) {
		EOEditingContext ec = editingContext();
		if(ec instanceof SessionedEditingContext && 
				(((SessionedEditingContext)ec).failuresCount() == 0)) {
			return;
		}
		EOQualifier qual = archiveQualifier(usedEntity, identifierDict);
		if(qual == null)
			return;
		Object[] inserted = ec.insertedObjects().objects();
		for (int i = 0; i < inserted.length; i++) {
			if(inserted[i] != this && inserted[i] instanceof MarkArchive
					&& qual.evaluateWithObject((EOEnterpriseObject)inserted[i])) {
				ec.deleteObject((EOEnterpriseObject)inserted[i]);
			}
		}
	}
	
	public void setUsedEntityName(String entityName) {
		EOEnterpriseObject ent = getUsedEntity(entityName, editingContext());
		setUsedEntity(ent);
	}	
/*
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
	*/

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
			usedEntity = getUsedEntity(entityName, editingContext());
		}
		setIdentifierFromDictionary(usedEntity, identifierDict);
	}
	
	protected static final String[] keys = new String[] {"key1","key2","key3"};
	public void setIdentifierFromDictionary (EOEnterpriseObject usedEntity,
											 NSDictionary identifierDict) {
		deleteInsertedDuplicates(usedEntity, identifierDict);
		
		Number zero = new Integer(0);
		for (int i = 0; i < keys.length; i++) {
			String key = (String)usedEntity.valueForKey(keys[i]);
			if(key == null)
				continue;
			Object value = identifierDict.valueForKey(key);
			if(value == null && key.endsWith("ID")) {
				key = key.substring(0, key.length() -2);
				value = identifierDict.valueForKey(key);
			}
			if(value == null) {
				takeValueForKey(zero, keys[i]);
				logger.log(WOLogLevel.FINE,"Archiving " + 
						usedEntity.valueForKey("usedEntity") + ": null value for key " + key,
						new Object[] {identifierDict,usedEntity});
				if(usedEntity.editingContext().globalIDForObject(usedEntity).isTemporary()) {
					usedEntity.takeStoredValueForKey(null, keys[i]);
					logger.log(WOLogLevel.COREDATA_EDITING,
							"Removing key '" + key + "' from entity description '" + 
							usedEntity.valueForKey("usedEntity") + '\'',usedEntity);
				}
			} else if(value instanceof Number) {
				takeValueForKey(value, keys[i]);
			} else if(value instanceof EOEnterpriseObject) {
				EOEditingContext ec = ((EOEnterpriseObject)value).editingContext();
				EOGlobalID gid = ec.globalIDForObject((EOEnterpriseObject)value);
				//NSDictionary pKey = EOUtilities.primaryKeyForObject(ec, (EOEnterpriseObject)value);
				if(gid instanceof EOKeyGlobalID) {
					takeValueForKey(((EOKeyGlobalID)gid).keyValues()[0], keys[i]);
				} else {
					NSSelector selector = new NSSelector("notifyOnPKinit",
							new Class[] {NSNotification.class});
					NSNotificationCenter.defaultCenter().addObserver(
							this, selector, EOGlobalID.GlobalIDChangedNotification, null);
					takeValueForKey(zero, keys[i]);
					if(awaitedKeys == null)
						awaitedKeys = new NSMutableDictionary(keys[i],gid);
					else
						awaitedKeys.setObjectForKey(keys[i],gid);
				}
			} else {
				takeValueForKey(zero, keys[i]);
				logger.log(WOLogLevel.WARNING, 
						"Illegal datatype to archive in key" + key,identifierDict);
			}
		}
	}
	
	protected NSMutableDictionary awaitedKeys;
	
	public void notifyOnPKinit(NSNotification notification) {
		NSDictionary userInfo = notification.userInfo();
		Enumeration enu = awaitedKeys.keyEnumerator();
		while (enu.hasMoreElements()) {
			EOGlobalID tmpGid = (EOGlobalID) enu.nextElement();
			EOKeyGlobalID keyGid = (EOKeyGlobalID)userInfo.objectForKey(tmpGid);
			if(keyGid == null)
				continue;
			String key = (String)awaitedKeys.objectForKey(tmpGid);
			Object value = keyGid.keyValues()[0];
			takeValueForKey(value, key);
		}
		NSNotificationCenter.defaultCenter().removeObserver(this);
	}
	
	public static NSArray archivesForObject(EOEnterpriseObject eo) {
		EOEditingContext ec = eo.editingContext();
		NSDictionary pKey = objectIdentifierDict(eo);
		if(pKey == null)
			return null;
		//EOUtilities.primaryKeyForObject(ec,eo);
		EOEnterpriseObject usedEntity = getUsedEntity(eo.entityName(), ec);
		//EOUtilities.objectMatchingKeyAndValue(ec,"UsedEntity","usedEntity",eo.entityName());
		EOQualifier qual = archiveQualifier(usedEntity,pKey);
		if(qual == null)
			return null;
		EOSortOrdering so = EOSortOrdering.sortOrderingWithKey(
				"timestamp", EOSortOrdering.CompareAscending);
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
	
	public static EOQualifier archiveQualifier(String entityName, 
			NSDictionary identifierDict, EOEditingContext ec) {
		EOEnterpriseObject usedEntity = getUsedEntity(entityName, ec);
		return archiveQualifier(usedEntity, identifierDict);
	}
	
	public static EOQualifier archiveQualifier(EOEnterpriseObject usedEntity, NSDictionary pKey) {
		EOQualifier qual = new EOKeyValueQualifier("usedEntity",
				EOQualifier.QualifierOperatorEqual, usedEntity);
		NSMutableArray quals = new NSMutableArray(qual);

		for (int i = 0; i < keys.length; i++) {
			String key = (String)usedEntity.valueForKey(keys[i]);
			if(key == null)
				continue;
			Object value = pKey.valueForKey(key);
			if(value == null && key.endsWith("ID")) {
				String tmpKey = key.substring(0, key.length() -2);
				value = pKey.valueForKey(tmpKey);
			}
			if(value == null) {
				logger.log(WOLogLevel.FINE,"Key '" + key + "' not available for " +
						usedEntity.valueForKey("usedEntity"), pKey);
				continue;
			}
			if(value instanceof EOEnterpriseObject) {
				EOEditingContext ec = ((EOEnterpriseObject)value).editingContext();
				EOGlobalID gid = ec.globalIDForObject((EOEnterpriseObject)value);
				//NSDictionary pKey = EOUtilities.primaryKeyForObject(ec, (EOEnterpriseObject)value);
				if(gid instanceof EOKeyGlobalID) {
					value = ((EOKeyGlobalID)gid).keyValues()[0];
				} else {
					Object[] args = new Object[] {"?",gid,pKey};
					if(ec instanceof SessionedEditingContext)
						args[0] = ((SessionedEditingContext)ec).session();
					logger.log(WOLogLevel.WARNING, "Uninitialised values in dict",args);
					//throw new IllegalArgumentException("Uninitialised values in dict");
					return null;
				}
			}
			qual = new EOKeyValueQualifier(keys[i],EOQualifier.QualifierOperatorEqual,value);
			quals.addObject(qual);
		}
		qual = new EOAndQualifier(quals);
		return qual;
	}
	/*
	public static String prepareToArchive(EOEnterpriseObject eo) {
		String result = null;
		EOEnterpriseObject usedEntity = EOUtilities.objectMatchingKeyAndValue(
		eo.editingContext(),"UsedEntity","usedEntity",eo.entityName());
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
			Enumeration enu = eo.attributeKeys().objectEnumerator();
			while (enu.hasMoreElements()) {
				key = (String) enu.nextElement();
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
		setData(NSPropertyListSerialization.stringFromPropertyList(dict,false));
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
		setData(NSPropertyListSerialization.stringFromPropertyList(dict,false));
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
		Object value = dict.valueForKey(key);
		return (value==null)?null:value.toString();
	}
	
	public int archivesCount() {
		NSArray all = new NSArray(keys);
		all = all.arrayByAddingObject("usedEntity");
		NSDictionary keysDict = valuesForKeys(all);
		all = EOUtilities.objectsMatchingValues(editingContext(), "MarkArchive", keysDict);
		if(all == null)
			return 0;
		return all.count();
	}
	/*
	public void validateForSave() {
		if(dict != null) {
			setData(NSPropertyListSerialization.stringFromPropertyList(dict,false));
		}
		super.validateForSave();
	}*/
	
	public void turnIntoFault(EOFaultHandler handler) {
		dict = null;
		super.turnIntoFault(handler);
	}
	
	protected static NSMutableSet waiters = new NSMutableSet();
	
	protected static Waiter waiterForEc(EOEditingContext ec) {
		Enumeration enu = waiters.objectEnumerator();
		Waiter result = null;
		NSMutableSet empty = null;
		while (enu.hasMoreElements()) {
			Waiter w = (Waiter) enu.nextElement();
			EOEditingContext wec = w.editingContext();
			if(wec == ec) {
				result = w;
				continue;
			}
			if(wec == null) {
				if(empty == null)
					empty = new NSMutableSet(w);
				else
					empty.addObject(w);
			}
		}
		if(empty != null)
			waiters.subtractSet(empty);
		if(result == null)
			result = new Waiter(ec);
		return result;
	}
	
	public static class Waiter {
		public static NSSelector selector = new NSSelector("fire",
				new Class[] {NSNotification.class});
		
		protected WeakReference<EOEditingContext> ecRef;
		protected NSMutableSet<NSMutableDictionary> dicts = new NSMutableSet<NSMutableDictionary>();
		
		public Waiter(EOEditingContext editingContext) {
			ecRef = new WeakReference(editingContext);
			NSNotificationCenter.defaultCenter().addObserver(this, selector,
					EOEditingContext.EditingContextDidSaveChangesNotification, editingContext);
			waiters.addObject(this);
		}
		
		public EOEditingContext editingContext() {
			return ecRef.get();
		}
		
		public void registerArchive(NSMutableDictionary dict, EOEnterpriseObject eo) {
			WeakReference<EOEnterpriseObject> eoRef = new WeakReference(eo);
			dict.setObjectForKey(eoRef, "eoRef");
			dicts.addObject(dict);
		}
		
		public void fire(NSNotification notification) {
			EOEditingContext ec = editingContext();
			if(ec == null) {
				WOSession ses = null;
				if(ec instanceof SessionedEditingContext)
					ses = ((SessionedEditingContext)ec).session();
				Logger.getLogger("rujel.markarchive").log(WOLogLevel.WARNING,
						"Failed to save archives: editingContext garbage collected",ses);
				return;
			}
			NSMutableSet left = null;
			Enumeration<NSMutableDictionary> enu = dicts.objectEnumerator();
			while (enu.hasMoreElements()) {
				NSMutableDictionary dict = (NSMutableDictionary) enu.nextElement();
				WeakReference<EOEnterpriseObject> eoRef = (WeakReference<EOEnterpriseObject>)
									dict.removeObjectForKey("eoRef");
				if(eoRef == null) continue;
				EOEnterpriseObject eo = eoRef.get();
				if(eo == null) continue;
				NSDictionary pKey = objectIdentifierDict(eo);
				if(pKey != null) {				
					MarkArchive arch = (MarkArchive)EOUtilities.createAndInsertInstance(
							ec, "MarkArchive");
					EOEnterpriseObject usedEntity = getUsedEntity(eo.entityName(), eo.editingContext());
					arch.setUsedEntity(usedEntity);
					arch.setIdentifierFromDictionary(usedEntity, pKey);
					arch.setArchiveDict(dict);
				} else {
					if(left == null)
						left = new NSMutableSet(dict);
					else
						left.addObject(dict);					
				}
			} // dicts.objectEnumerator()
			try {
				NSNotificationCenter.defaultCenter().removeObserver(this);
				ec.saveChanges();
				dicts = left;
			} catch (RuntimeException e) {
				Object args[] = new Object[] {e};
				if(ec instanceof SessionedEditingContext)
					args = new Object[] {e,((SessionedEditingContext)ec).session()};
				Logger.getLogger("rujel.markarchive").log(WOLogLevel.WARNING,
						"Failed to save archives",args);
				if(ec.hasChanges())
					ec.revert();
			}
			//NSMutableSet waiters = MarkArchive.waiters;
			if(dicts == null || dicts.count() == 0)
				waiters.removeObject(this);
			else
				NSNotificationCenter.defaultCenter().addObserver(this, selector,
						EOEditingContext.EditingContextDidSaveChangesNotification, ec);
		}
		
		public boolean equals(Object obj) {
			if (obj instanceof Waiter) {
				Waiter w = (Waiter) obj;
				return (ecRef == w.ecRef);
			}
			return false;
		}
	}
 }
