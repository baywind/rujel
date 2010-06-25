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

import java.util.Calendar;
import java.util.Date;

import net.rujel.reusables.DelegateManager;
import net.rujel.reusables.SessionedEditingContext;
import net.rujel.reusables.Various;

import com.webobjects.appserver.WOApplication;
import com.webobjects.eoaccess.EOEntity;
import com.webobjects.eoaccess.EOModelGroup;
import com.webobjects.eoaccess.EORelationship;
import com.webobjects.eoaccess.EOUtilities;
import com.webobjects.eocontrol.EOEnterpriseObject;
import com.webobjects.eocontrol.EOSortOrdering;
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
		public static final DelegateManager delegateManager = new DelegateManager();
		
		protected static final NSSelector initials = new NSSelector(
				"initials",new Class[] {Person.class});
		public static String initials(Person pers) {
			Object byDelegate = delegateManager.useDelegates(initials, new Object[] {pers});
			if(byDelegate != null) {
				if(byDelegate == NullValue)
					return null;
				return (String)byDelegate;
			}
			StringBuffer sb = new StringBuffer(5);
			if(pers.firstName() != null)
				sb.append(pers.firstName().charAt(0)).append('.');
			
			if(pers.secondName() == null) return sb.toString();
			
			if(sb.length() > 0) sb.append(' ');
			sb.append(pers.secondName().charAt(0)).append('.');
			return sb.toString();
		}
		
		protected static final NSSelector composeName = new NSSelector(
				"composeName",new Class[] {Person.class,Integer.TYPE,Integer.TYPE});
		public static String composeName(Person pers, int firstNameDisplay,int secondNameDisplay) {
			if(firstNameDisplay < 1 && secondNameDisplay < 1)
				return "";

			Object byDelegate = delegateManager.useDelegates(composeName, 
					new Object[] {pers,firstNameDisplay,secondNameDisplay});
			if(byDelegate != null) {
				if(byDelegate == NullValue)
					return null;
				return (String)byDelegate;
			}
				
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
		
		protected static final NSSelector fullName = new NSSelector(
				"fullName",new Class[] {PersonLink.class,Boolean.TYPE,Integer.TYPE,Integer.TYPE,Integer.TYPE});

		public static String fullName(PersonLink person, boolean startWithLastName,int lastNameDisplay,int firstNameDisplay,int secondNameDisplay) {
			if(person == null)
				return "???";
			Person pers = (person instanceof Person)?(Person)person:person.person();
			if(pers == null)
				return "???";
			Object byDelegate = delegateManager.useDelegates(fullName, 
					new Object[] {person,startWithLastName,lastNameDisplay,firstNameDisplay,secondNameDisplay});
			if(byDelegate != null) {
				if(byDelegate == NullValue)
					return null;
				return (String)byDelegate;
			}
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
		
//		protected static final NSSelector personQualifier = new NSSelector(
//				"personQualifier",new Class[] {String.class,String.class,String.class});
		protected static EOQualifier personQualifier(String last,String first,String second) {
/*			Object byDelegate = delegateManager.useDelegates(personQualifier, 
					new Object[] {last,first,second});
			if(byDelegate != null) {
				if(byDelegate == NullValue)
					return null;
				return (EOQualifier)byDelegate;
			}*/
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
				
//		protected static final NSSelector fullNameQualifier = new NSSelector(
//				"fullNameQualifier",new Class[] {String.class});
		protected static EOQualifier fullNameQualifier(String searchString) {
/*			Object byDelegate = delegateManager.useDelegates(personQualifier, 
					new Object[] {searchString});
			if(byDelegate != null) {
				if(byDelegate == NullValue)
					return null;
				return (EOQualifier)byDelegate;
			} */
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
				return new EOOrQualifier(quals);
				
			case 2:
				quals.addObject(personQualifier(names[0],names[1],null));
				quals.addObject(personQualifier(names[1],names[0],null));
				quals.addObject(personQualifier(null,names[0],names[1]));
				return new EOOrQualifier(quals);
				
			case 3:
				quals.addObject(personQualifier(names[0],names[1],names[2]));
				quals.addObject(personQualifier(names[2],names[0],names[1]));
				return new EOOrQualifier(quals);				

			default:
				return null;
			}
		}
		
//		public static NSArray search(EOEditingContext ec,String entity, String personEntity, String last,String first,String second) {
//			EOFetchSpecification fspec = new EOFetchSpecification(entity,personQualifier(last,first,second),sorter);
//			return ec.objectsWithFetchSpecification(fspec);
//		}

		protected static final NSSelector search = new NSSelector(
				"search",new Class[] {EOEditingContext.class,String.class,String.class});
		public static NSArray search(EOEditingContext ec,String entity, String searchString) {
			Object byDelegate = delegateManager.useDelegates(search, 
					new Object[] {ec,entity,searchString});
			if(byDelegate != null) {
				if(byDelegate == NullValue)
					return null;
				return (NSArray)byDelegate;
			}
			EOQualifier qual = Person.Utility.fullNameQualifier(searchString);
			if(qual == null) {
				NSKeyValueCodingAdditions strings = null;
				if(ec instanceof SessionedEditingContext) {
					strings = (NSKeyValueCodingAdditions)((SessionedEditingContext)
							ec).session().valueForKey("strings");
				} else {
					strings = (NSKeyValueCodingAdditions)WOApplication.application()
									.valueForKey("strings");
				}
				String noMore = (String)strings.valueForKeyPath(
						"RujelBase_Base.notMoreXwords");
				StringBuilder buf = new StringBuilder();
				buf.append(strings.valueForKeyPath("Strings.messages.illegalFormat"));
				buf.append(' ');
				buf.append(strings.valueForKeyPath("Reusables_Strings.dataTypes.ofRequest"));
				buf.append(' ');
				buf.append(String.format(noMore,3));
				throw new IllegalArgumentException(buf.toString());
			}
			return search(ec, qual, entity);
		}
		
		public static NSArray search(EOEditingContext ec, EOQualifier qual, String entity) {
			EOModelGroup mg = EOModelGroup.defaultGroup();
			EOEntity ent = mg.entityNamed(entity);
			EORelationship rel = ent.relationshipNamed("person");
			boolean same = rel.destinationEntity().equals(ent);
			String personEntity = (same)?entity:rel.destinationEntity().name();
			EOFetchSpecification fspec = new EOFetchSpecification(personEntity,qual,
					(same)?sorter:null);
			NSArray list = ec.objectsWithFetchSpecification(fspec);
			if(same || list == null || list.count() == 0)
				return list;
			qual = Various.getEOInQualifier("person", list);
			fspec.setEntityName(entity);
			fspec.setQualifier(qual);
			fspec.setSortOrderings(sorter);
			return ec.objectsWithFetchSpecification(fspec);
		}
		
		protected static final NSSelector create = new NSSelector(
				"create",new Class[] {EOEditingContext.class,String.class,String.class});
		public static PersonLink create(EOEditingContext ec,String entity, String initString) {
			Object byDelegate = delegateManager.useDelegates(create, 
					new Object[] {ec,entity,initString});
			if(byDelegate != null) {
				if(byDelegate == NullValue)
					return null;
				return (PersonLink)byDelegate;
			}
			Person onEdit = (Person)EOUtilities.createAndInsertInstance(ec,entity);
			String[] names = splitNames(initString);
			if(names != null) {
				if(names.length > 0)
					onEdit.setLastName(names[0]);
				if(names.length > 1)
					onEdit.setFirstName(names[1]);
				if(names.length > 2)
					onEdit.setSecondName(names[2]);
			}
			return onEdit;
		}
		
		public static int calculateAge(Date birth, Date day) {
			if(birth == null || day == null)
				return -1;
			Calendar calB = Calendar.getInstance();
			calB.setTime(birth);
			Calendar calD = Calendar.getInstance();
			calD .setTime(day);
			int result = calD.get(Calendar.YEAR) - calB.get(Calendar.YEAR);
			if(calD.get(Calendar.MONTH) < calB.get(Calendar.MONTH)) {
				result--;
			} else if(calD.get(Calendar.MONTH) == calB.get(Calendar.MONTH) &&
					calD.get(Calendar.DAY_OF_MONTH) < calB.get(Calendar.DAY_OF_MONTH)) {
				result--;
			}
			return result;
		}
	}
	
	public static class ComparisonSupport extends EOSortOrdering.ComparisonSupport {

		public int compareAscending(Object left, Object right) {
			try {
				Person leftPerson = (Person)left;
				Person rightPerson = (Person)right;
				int result = compareValues(leftPerson.lastName(), rightPerson.lastName(),
						EOSortOrdering.CompareAscending);
				if(result != NSComparator.OrderedSame)
					return result;
				result = compareValues(leftPerson.firstName(), rightPerson.firstName(),
						EOSortOrdering.CompareAscending);
				if(result != NSComparator.OrderedSame)
					return result;
				result = compareValues(leftPerson.secondName(), rightPerson.secondName(),
						EOSortOrdering.CompareAscending);
				if(result != NSComparator.OrderedSame)
					return result;
				result = compareValues(leftPerson.birthDate(), rightPerson.birthDate(),
						EOSortOrdering.CompareAscending);
				return result;
			} catch (Exception e) {
				return super.compareAscending(left, right);
			}
		}

		public int compareCaseInsensitiveAscending(Object left, Object right) {
			try {
				Person leftPerson = (Person)left;
				Person rightPerson = (Person)right;
				int result = compareValues(leftPerson.lastName(), rightPerson.lastName(),
						EOSortOrdering.CompareCaseInsensitiveAscending);
				if(result != NSComparator.OrderedSame)
					return result;
				result = compareValues(leftPerson.firstName(), rightPerson.firstName(),
						EOSortOrdering.CompareCaseInsensitiveAscending);
				if(result != NSComparator.OrderedSame)
					return result;
				result = compareValues(leftPerson.secondName(), rightPerson.secondName(),
						EOSortOrdering.CompareCaseInsensitiveAscending);
				if(result != NSComparator.OrderedSame)
					return result;
				result = compareValues(leftPerson.birthDate(), rightPerson.birthDate(),
						EOSortOrdering.CompareCaseInsensitiveAscending);
				return result;
			} catch (Exception e) {
				return super.compareCaseInsensitiveAscending(left, right);
			}
		}

		public int compareDescending(Object left, Object right) {
			return compareAscending(right, left);
		}

		public int compareCaseInsensitiveDescending(Object left, Object right) {
			return compareCaseInsensitiveAscending(right, left);
		}
	}
}
