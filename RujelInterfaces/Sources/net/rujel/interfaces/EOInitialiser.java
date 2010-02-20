// EOInitialiser.java

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

package net.rujel.interfaces;

import com.webobjects.foundation.*;
import com.webobjects.eoaccess.*;
import java.util.logging.Logger;
import java.util.logging.Level;

public class EOInitialiser {
	protected static Logger logger = Logger.getLogger("EOInitialiser");
//	public static final String ENTITY = "UsedEOEntity";
//	public static final String PKEY = "PrimaryKeyAttribute";
//	public static final String TOMANY = "isToMany";
	
//	public static final SettingsReader intefacesPrefs = SettingsReader.settingsForPath("interfaces",true);
	protected static final EOModelGroup mg = EOModelGroup.defaultGroup();
	
	
	public static final EORelationship initialiseRelationship (String sourceEntityName, 
															   String relationshipName,
															   boolean toMany,
															   String sourceAttributeName,
															   String targetInterface) {
		EOEntity sEntity = mg.entityNamed(sourceEntityName);
		if(sEntity == null)
			throw new IllegalStateException ("Could not retrieve source entity");
		EOAttribute sAttr = sEntity.attributeNamed(sourceAttributeName);
		if(sAttr == null)
			throw new IllegalStateException ("Could not retrieve source attribute");
		return initialiseRelationship(sEntity,relationshipName,toMany,sAttr,targetInterface);
	}
	
	public static final EORelationship initialiseRelationship (EOEntity sourceEntity, 
															   String relationshipName,
															   boolean toMany,
															   EOAttribute sourceAttribute,
															   String targetInterface) {
		if(sourceAttribute == null) {
			throw new IllegalArgumentException ("Source attribute is required");
		}
		EORelationship relationship = sourceEntity.relationshipNamed(relationshipName);
		
		EOEntity tEntity = mg.entityNamed(targetInterface);
		try {
			Class tIf = Class.forName("net.rujel.interfaces." + targetInterface);
			String tEntityName = (String)tIf.getDeclaredField("entityName").get(null);
			tEntity = mg.entityNamed(tEntityName);
		} catch (Exception e) {
			if (relationship == null)
				throw new IllegalStateException("Target Entity was not properly described");
			else
				return relationship;
		}
		/*
		String tEntityName = intefacesPrefs.get(targetInterface,null);
		if(tEntityName == null) { 
			if (tEntity == null) {
				if (relationship == null)
					throw new IllegalStateException("Target Entity was not properly described");
				else
					return relationship;
			} else {
				tEntityName = targetInterface;
			}
		} else {
			int dot = tEntityName.lastIndexOf('.');
			if(dot > 0) {
				tEntityName = tEntityName.substring(dot + 1);
			}
			if(relationship != null) {
				if (relationship.destinationEntity().name().equals(tEntityName))
					return relationship;
				else
					sourceEntity.removeRelationship(relationship);
			}
			tEntity = mg.entityNamed(tEntityName);
		}
		*/
		if(tEntity == null) {
			throw new IllegalStateException ("Could not retrieve target entity");
		} else {
			if(relationship != null) {
				if (relationship.destinationEntity().equals(tEntity))
					return relationship;
				else
					sourceEntity.removeRelationship(relationship);
			}
		}
		NSArray tPkey = tEntity.primaryKeyAttributes();
		if (tPkey.count() != 1)
			throw new IllegalStateException ("Target entity should have exactly one primaryKey attribute");
		EOAttribute tAttr = (EOAttribute)tPkey.lastObject();
		EOJoin join = new EOJoin(sourceAttribute,tAttr);
		relationship = new EORelationship();
		relationship.setName(relationshipName);
		sourceEntity.addRelationship(relationship);
		relationship.addJoin(join);
		relationship.setToMany(toMany);
		relationship.setJoinSemantic(EORelationship.InnerJoin);
		relationship.setIsMandatory(!sourceAttribute.allowsNull());
		logger.logp(Level.CONFIG,"EOInitialiser","initialiseRelationship","Connected '" 
				+ sourceEntity.name() + "' to '" + tEntity.name() +
				"' with relationship named '" + relationshipName + '\'');
		return relationship;
	}
	
	public static String relatedEOforAttribute(String sEntityName, String relationshipName) {
		EOEntity sEntity = mg.entityNamed(sEntityName);
		EORelationship rel = sEntity.relationshipNamed(relationshipName);
		EOEntity tEntity = rel.destinationEntity();
		return tEntity.name();
	}
	
	public static void initAll() {
		//String[] interfaces = null;
//		String className;
		Class aClass;
		java.lang.reflect.Method method;
		/*try {
			interfaces = intefacesPrefs.keys();
		} catch (BackingStoreException bex) {
			throw new NSForwardException(bex, "Could not read preferences.");
		}*/
//		java.util.Enumeration enu = intefacesPrefs.keyEnumerator();
		String[] ifClasses = new String[] {
				EduCourse.className, EduCycle.className, EduLesson.className,
				EduGroup.className, Student.className, Teacher.className
		};
		for (int i = 0; i < ifClasses.length; i++) {
//		while (enu.hasMoreElements()) {
//			String intrfc = (String)enu.nextElement();
//			className = intefacesPrefs.get(intrfc,intrfc);
			try {
				aClass = Class.forName(ifClasses[i]);
				method = aClass.getMethod("init");
				method.invoke(null,(Object[])null);
			} catch (java.lang.NoSuchMethodException nex) {
				
			} catch (java.lang.reflect.InvocationTargetException tex) {
				throw new NSForwardException(tex.getCause(),"Initialisation error");
			} catch (Exception ex) {
				throw new NSForwardException(ex,"Initialisation error");
			}
		}
		}
	}