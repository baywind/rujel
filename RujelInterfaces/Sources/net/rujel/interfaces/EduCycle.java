// EduCycle.java

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

import java.lang.reflect.Method;

import com.webobjects.eocontrol.EOEditingContext;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSForwardException;

public interface EduCycle extends com.webobjects.eocontrol.EOEnterpriseObject {
	public static final String className = net.rujel.reusables.SettingsReader.stringForKeyPath("interfaces.EduCycle",null);
	public static final String entityName = className.substring(1 + className.lastIndexOf('.'));

	/** Should implement static method <tt>public static NSArray cyclesForGrade(Integer grade, EOEditingContext ec)</tt>  */
	/** Could implement static method <tt>public static NSArray cyclesForEduGroup(EduGroup group)</tt>  */
	
	public String subject();
	public void setSubject(String newSubject);
	
	public Integer grade();
	public void setGrade(Integer newGrade);
	/*
	public Integer level();
	public void setLevel(Integer newLevel);
	*/
	public Integer subgroups();
	//public void setSubgroups(Integer newCount);
	
	public Integer school();
	
	public static class Lister {
		protected static Method cyclesForGrade;
		public static NSArray cyclesForGrade(Integer grade, EOEditingContext ec) {
			try {
				if(cyclesForGrade == null) {
					Class aClass = Class.forName(className);
					cyclesForGrade = aClass.getMethod("cyclesForGrade",Integer.class,EOEditingContext.class);
				}
				return (NSArray)cyclesForGrade.invoke(null,grade,ec);
			} catch (Exception ex) {
				throw new NSForwardException(ex, "Could not initialise EduCycle listing method");
			}			
		}
		
		protected static Method forEduGroup;
		public static NSArray cyclesForEduGroup(EduGroup group) {
			try {
				if(forEduGroup == null) {
					try {
						Class aClass = Class.forName(className);
						forEduGroup = aClass.getMethod("cyclesForEduGroup",EduGroup.class);
					} catch (NoSuchMethodException e) {
						NSArray result = cyclesForGrade(group.grade(),group.editingContext());
						forEduGroup = cyclesForGrade;
						return result;
					}
				}
				if(forEduGroup == cyclesForGrade)
					return (NSArray)cyclesForGrade.invoke(null,group.grade(),group.editingContext());
				return (NSArray)forEduGroup.invoke(null, group);
			} catch (Exception ex) {
				throw new NSForwardException(ex, "Could not initialise EduCycle listing method");
			}
		}
	}
}