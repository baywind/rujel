// Person.java

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

import com.webobjects.eocontrol.EOEnterpriseObject;
import com.webobjects.foundation.*;
import com.webobjects.eocontrol.*;

public interface Person extends EOEnterpriseObject,PersonLink {

	public static final NSArray sorter = new NSArray(new EOSortOrdering[] {
		EOSortOrdering.sortOrderingWithKey("person.lastName", EOSortOrdering.CompareAscending),
		EOSortOrdering.sortOrderingWithKey("person.firstName", EOSortOrdering.CompareAscending),
		EOSortOrdering.sortOrderingWithKey("person.secondName", EOSortOrdering.CompareAscending),
		EOSortOrdering.sortOrderingWithKey("person.birthDate", EOSortOrdering.CompareAscending)
	});
	
	/**
	 *Should return this
	 */
	public Person person();
	
	public String lastName();
	public void setLastName(String newLastName);
	
	public String firstName();
	public void setFirstName(String newFirstName);

	public String secondName();
	public void setSecondName(String newSecondName);
	
	public static final Boolean MALE = Boolean.TRUE;
	public static final Boolean FEMALE = Boolean.FALSE;
	
	public Boolean sex();
	public void setSex(Boolean newSex);
	
	public NSTimestamp birthDate();
	public void setBirthDate(NSTimestamp newDate);
	
	/** should return new String (new char[] {firstName().charAt(0),'.',' ',secondName().charAt(0),'.'}); */
	public String initials();
	
	public static class Utility {
		public static String initials(Person pers) {
			StringBuffer sb = new StringBuffer(5);
			if(pers.firstName() != null)
				sb.append(pers.firstName().charAt(0)).append('.');
			
			if(pers.secondName() == null) return sb.toString();
			
			if(sb.length() > 0) sb.append(' ');
			sb.append(pers.secondName().charAt(0)).append('.');
			return sb.toString();
			/*
			char fn = (pers.firstName() != null)?pers.firstName().charAt(0):'?';
			if(pers.secondName() != null)
				return new String (new char[] {fn,'.',' ',pers.secondName().charAt(0),'.'});
			else
				return new String (new char[] {fn,'.'}); */
		}
		
		public static String composeName(Person pers, int firstNameDisplay,int secondNameDisplay) {
			if(firstNameDisplay < 1 && secondNameDisplay < 1)
				return "";
			
			if(firstNameDisplay == 1 && secondNameDisplay == 1)
				return pers.initials();
			
			StringBuffer buf = new StringBuffer(57);
			
			String first = pers.firstName();
			
			switch (firstNameDisplay) {
			case 0:
				break;
			case 1:
				buf.append((first==null)?'?':first.charAt(0)).append('.');
				break;
			default:
				buf.append(first);
				break;
			}
			/*
			if(firstNameDisplay == 1) {
				if(first != null)
					first = new String(new char[]{first.charAt(0),'.'});
				else
					first = "?.";
			} 
			else {
				if(firstNameDisplay < 1)
					first = null;
			} */
			
			String second = pers.secondName();
			if(secondNameDisplay < 1 || second == null) return buf.toString();
			
			if(buf.length() > 0) buf.append(' ');
			
			if(secondNameDisplay == 1)
				buf.append(second.charAt(0)).append('.');
				//second = new String(new char[]{second.charAt(0),'.'});
			else
				buf.append(second);
			
			//if(first == null) return second;
			
			return buf.toString();//first + " " + second;
		}
		
		public static String fullName(PersonLink person, boolean startWithLastName,int lastNameDisplay,int firstNameDisplay,int secondNameDisplay) {
			if(person == null)
				return null;
			Person pers = (person instanceof Person)?(Person)person:person.person();
			if(lastNameDisplay < 1) return composeName(pers,firstNameDisplay,secondNameDisplay);
			
			String last = pers.lastName();
			if(last == null) last = "???";
			if(lastNameDisplay == 1)
				last = new String(new char[]{last.charAt(0),'.'});
			
			if (firstNameDisplay < 1 && secondNameDisplay < 1) return last;
			
			String compose = composeName(pers,firstNameDisplay,secondNameDisplay);
			
			if(startWithLastName)
				return last + " " + compose;
			else
				return compose + " " + last;
		}
		
		public static EOQualifier personQualifier(String last,String first,String second) {
			NSMutableArray quals = new NSMutableArray();
			if(last != null)
				quals.addObject(new EOKeyValueQualifier("lastName", EOQualifier.QualifierOperatorCaseInsensitiveLike,
 last + "*"));
			if(first != null)
				quals.addObject(new EOKeyValueQualifier("firstName", EOQualifier.QualifierOperatorCaseInsensitiveLike,
 first + "*"));
			if(second != null)
				quals.addObject(new EOKeyValueQualifier("secondName", EOQualifier.QualifierOperatorCaseInsensitiveLike,
 second + "*"));
			return new com.webobjects.eocontrol.EOAndQualifier(quals);
		}
		
		public static String[] splitNames(String fullString) {
			if(fullString == null || fullString.length() == 0) return null;
			String tmp = fullString.replaceAll("\\."," ");
			tmp = tmp.trim();
			if(tmp.length() < 1) return null;
//			while(tmp.contains("  ")) {
//				tmp = tmp.replaceAll(" {2,}"," ");
//			}
			return tmp.split("\\ +");
		}
				
		public static EOQualifier fullNameQualifier(String searchString) {
			String[] names = splitNames(searchString);
			if(names == null) return null;

			NSMutableArray quals = new NSMutableArray();
//			EOQualifier qual;
			switch (names.length) {
			case 1:
				quals.addObject(new EOKeyValueQualifier("lastName", EOQualifier.QualifierOperatorCaseInsensitiveLike,
														names[0] + "*"));
				quals.addObject(new EOKeyValueQualifier("firstName", EOQualifier.QualifierOperatorCaseInsensitiveLike,
														names[0] + "*"));
				return new com.webobjects.eocontrol.EOOrQualifier(quals);
				
			case 2:
				quals.addObject(personQualifier(names[0],names[1],null));
				quals.addObject(personQualifier(names[1],names[0],null));
				quals.addObject(personQualifier(null,names[0],names[1]));
				return new com.webobjects.eocontrol.EOOrQualifier(quals);
				
			case 3:
				quals.addObject(personQualifier(names[0],names[1],names[2]));
				quals.addObject(personQualifier(names[2],names[0],names[1]));
				return new com.webobjects.eocontrol.EOOrQualifier(quals);				

			default:
				return null;
			}
		}
		
		public static NSArray search(EOEditingContext ec,String entity,String last,String first,String second) {
			EOFetchSpecification fspec = new EOFetchSpecification(entity,personQualifier(last,first,second),sorter);
			return ec.objectsWithFetchSpecification(fspec);
		}

		public static NSArray search(EOEditingContext ec,String entity,String searchString) {
			EOQualifier qual = Person.Utility.fullNameQualifier(searchString);
			if(qual == null)
				return null;

			EOFetchSpecification fspec = new EOFetchSpecification(entity,qual,sorter);
			return ec.objectsWithFetchSpecification(fspec);		
		}
	}
}
