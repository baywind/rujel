// PrimitiveEduCycle.java

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

package net.rujel.base;

import net.rujel.interfaces.*;
import net.rujel.reusables.*;
import net.rujel.auth.UseAccess;
import com.webobjects.foundation.*;
import com.webobjects.eoaccess.EOUtilities;
import com.webobjects.eocontrol.*;

public class PrimitiveEduCycle extends _PrimitiveEduCycle implements EduCycle,UseAccess
{
    public PrimitiveEduCycle() {
        super();
    }
    
	public static void init() {
		EOSortOrdering.ComparisonSupport.setSupportForClass(
				new PrimitiveEduCycle.ComparisonSupport(), PrimitiveEduCycle.class);
	}

	
	protected static Integer school;
	public Integer school() {
		if(school == null)
			school = new Integer(SettingsReader.intForKeyPath("schoolNumber", 0));
		return school;
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
    public static NSArray cyclesForGrade(Integer grade, EOEditingContext ec) {
    	return EOUtilities.objectsMatchingKeyAndValue(ec,"PrimitiveEduCycle","grade",grade);
    }
	
	private transient NamedFlags _access;
	public NamedFlags access() {
		if(_access == null) {
			_access = StaticImplementation.access(this,accessKeys);
		}
		return _access.immutableClone();
	}
	/*
	private transient NSMutableDictionary _schemeCache;
	public NamedFlags schemeAccess(String schemePath) {
		NamedFlags result = null;
		if(_schemeCache == null) {
			_schemeCache = new NSMutableDictionary();
		} else {
			result = (NamedFlags)_schemeCache.objectForKey(schemePath);
		}
		if(result == null) {
			result = StaticImplementation.schemeAccess(this,schemePath);
			if(result == null) {
				result = DegenerateFlags.ALL_FALSE;
			}
			_schemeCache.setObjectForKey(result,schemePath);
		}
		if(result == DegenerateFlags.ALL_FALSE)
			return null;
		else return result;
	}
	*/
	
	public boolean isOwned() {
		return StaticImplementation.isOwned(this);
	}
	/*
	public Integer grade() {
		return new Integer(super.grade().intValue());
	}
	public void setGrade(Integer newGrade) {
		super.setGrade(newGrade);
	}

	public Integer subgroups() {
		return new Integer(super.subgroups().intValue());
	}
	public void setSubgroups(Integer newCount) {
		super.setSubgroups(newCount);
	}*/
	
	public void awakeFromInsertion(EOEditingContext ec){
		super.awakeFromInsertion(ec);
		setSubgroups(new Integer(1));
	}
	
	public void validateForDelete() throws NSValidation.ValidationException {
		try {
			super.validateForDelete();
		} catch (NSValidation.ValidationException vex) {
			String message = MyUtility.stringForPath("messages.cantDelete");
			NSValidation.ValidationException myex = new NSValidation.ValidationException(message);
			NSArray aggregate = new NSArray(new Object[] {myex , vex});
			throw NSValidation.ValidationException.aggregateExceptionWithExceptions(aggregate);
		}
	}
	
	public static class ComparisonSupport extends EOSortOrdering.ComparisonSupport {
				
		public int compareAscending(Object left, Object right)  {
			EduCycle l = (EduCycle)left;
			EduCycle r = (EduCycle)right;
			return super.compareAscending(l.subject(), r.subject());
		}	
		public int compareCaseInsensitiveAscending(Object left, Object right)  {
			EduCycle l = (EduCycle)left;
			EduCycle r = (EduCycle)right;
			return super.compareCaseInsensitiveAscending(l.subject(), r.subject());
		}
		
		public int compareDescending(Object left, Object right)  {
			EduCycle l = (EduCycle)left;
			EduCycle r = (EduCycle)right;
			return super.compareDescending(l.subject(), r.subject());
		}
		public int compareCaseInsensitiveDescending(Object left, Object right)  {
			EduCycle l = (EduCycle)left;
			EduCycle r = (EduCycle)right;
			return super.compareCaseInsensitiveDescending(l.subject(), r.subject());
		}
	}

}
