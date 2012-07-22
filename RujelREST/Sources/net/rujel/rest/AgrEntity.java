// AgrEntity.java

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

package net.rujel.rest;

import java.math.BigDecimal;
import java.util.Enumeration;
import java.util.NoSuchElementException;

import net.rujel.rest.Agregator.ParseError;

import com.webobjects.eocontrol.EOAndQualifier;
import com.webobjects.eocontrol.EOEditingContext;
import com.webobjects.eocontrol.EOEnterpriseObject;
import com.webobjects.eocontrol.EOFetchSpecification;
import com.webobjects.eocontrol.EOKeyValueQualifier;
import com.webobjects.eocontrol.EOQualifier;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSDictionary;
import com.webobjects.foundation.NSKeyValueCoding;
import com.webobjects.foundation.NSMutableArray;
import com.webobjects.foundation.NSMutableDictionary;
import com.webobjects.foundation.NSSelector;

public abstract class AgrEntity {
	
	protected EOEditingContext ec;
	
	public static AgrEntity forName(String entName, EOEditingContext ec) {
		if(entName == null)
			return null;
		AgrEntity result = null;
		if(entName.equalsIgnoreCase("itogMark"))
			result = new AgrItogMark();
		if(result == null)
			throw new IllegalArgumentException("Unknown entity name '" + entName + '\'');
		result.ec = ec;
		return result;
	}

	public abstract String entityName();
	public abstract NSArray attributes();
	public abstract Enumeration getObjectsEnumeration(NSDictionary params) throws ParseError;
	public abstract Object getValue(EOEnterpriseObject obj, String attribute);
	
	public static Object[] selectorAndValue(String value) throws ParseError {
		if(value == null || value.equals("*"))
			return null;
		Object[] result = new Object[2];
		result[0] = EOQualifier.QualifierOperatorEqual;
		result[1] = value;
		if(!Character.isDigit(value.charAt(0))) {
			for (int i = 0; i < value.length(); i++) {
				char c = value.charAt(i);
				if(c == '.' || Character.isDigit(c)) {
					result[1] = value.substring(i);
					String op = value.substring(0,i);
					if("!".equals(op) || "!=".equals(op))
						result[0] = EOQualifier.QualifierOperatorNotEqual;
					else if(">".equals(op)) 
						result[0] = EOQualifier.QualifierOperatorGreaterThan;
					else if(">=".equals(op)) 
						result[0] = EOQualifier.QualifierOperatorGreaterThanOrEqualTo;
					else if("<".equals(op)) 
						result[0] = EOQualifier.QualifierOperatorLessThan;
					else if("<=".equals(op)) 
						result[0] = EOQualifier.QualifierOperatorLessThanOrEqualTo;
					else if("[".equals(op) || "(".equals(op)) {
						result = new Object[4];
						if(value.charAt(0) == '[')
							result[0] = EOQualifier.QualifierOperatorGreaterThanOrEqualTo;
						else
							result[0] = EOQualifier.QualifierOperatorGreaterThan;
						if(value.charAt(value.length() -1) == ']')
							result[2] = EOQualifier.QualifierOperatorLessThanOrEqualTo;
						else
							result[2] = EOQualifier.QualifierOperatorLessThan;
						int split = Math.max(value.indexOf(','),value.indexOf(';'));
						result[1] = value.substring(i,split);
						result[3] = value.substring(split +1, value.length() -1);
					} else if("{".equals(op)) {
						if(value.charAt(value.length() -1) != '}')
							throw new Agregator.ParseError("Closing bracket not found",
									value.length() -1, value);
						String[] values = value.substring(i, value.length() -1).split(",");
						result = new Object[values.length +1];
						result[0] = EOQualifier.QualifierOperatorEqual;
						for (int j = 0; j < values.length; j++) {
							result[j+1] = values[j];
						}
					} else 
						throw new Agregator.ParseError("Unknown operator '" + op + '\'',i,value);
					break;
				}
			}
		}
		return result;
	}
	
	public static void addIntToQuals(NSMutableArray quals, String attrib, String value)
															throws ParseError {
		Object[] snv = selectorAndValue(value);
		if(snv != null) {
			quals.addObject(new EOKeyValueQualifier(attrib, 
					(NSSelector)snv[0], new Integer((String)snv[1])));
			if(snv.length > 2) {
				if(snv[0] == EOQualifier.QualifierOperatorEqual) {
					for (int i = 2; i < snv.length; i++) {
						quals.addObject(new EOKeyValueQualifier(attrib, 
								(NSSelector)snv[0], new Integer((String)snv[i])));
					}
				} else {
					quals.addObject(new EOKeyValueQualifier(attrib, 
							(NSSelector)snv[2], new Integer((String)snv[3])));
				}
			}
		}
	}
	
	private static BigDecimal decimal(Object val) {
		String txt = (String)val;
		if(txt.equalsIgnoreCase("null") || txt.equalsIgnoreCase("nil"))
			return null;
		if(txt.charAt(0) == '.') {
			StringBuilder buf = new StringBuilder(txt.length() +1);
			buf.append('0').append(txt);
			txt = buf.toString();
		}
		return new BigDecimal(txt);
	}
	
	public static void addDecToQuals(NSMutableArray quals, String attrib, String value) 
															throws ParseError {
		Object[] snv = selectorAndValue(value);
		if(snv != null) {
			quals.addObject(new EOKeyValueQualifier(attrib, 
					(NSSelector)snv[0], decimal(snv[1])));
			if(snv.length > 2) {
				if(snv[0] == EOQualifier.QualifierOperatorEqual) {
					for (int i = 2; i < snv.length; i++) {
						quals.addObject(new EOKeyValueQualifier(attrib, 
								(NSSelector)snv[0], decimal(snv[i])));
					}
				} else {
					quals.addObject(new EOKeyValueQualifier(attrib, 
							(NSSelector)snv[2], decimal(snv[3])));
				}
			}
		}
	}
	
	protected static class RowsEnum implements Enumeration {
		private String[] itrAttr;
		private NSArray[] itrValues;
		private int[] itrIdx;
		private NSMutableArray quals;
		private int base;
		private AgrEntity ent;
		private Enumeration recent;
		
		public RowsEnum(AgrEntity entity, NSArray baseQuals, NSDictionary iterate) {
			ent = entity;
			quals = baseQuals.mutableClone();
			base = quals.count();
			if(iterate == null || iterate.count() == 0)
				return;
			itrAttr = new String[iterate.count()];
			itrValues = new NSArray[iterate.count()];
//			itrIdx = new int[iterate.count()];
			Enumeration enu = iterate.keyEnumerator();
			int i = 0;
			while (enu.hasMoreElements()) {
				itrAttr[i] = (String) enu.nextElement();
				itrValues[i] = (NSArray)iterate.valueForKey(itrAttr[i]);
//				itrIdx[i] = 0;
				i++;
			}
		}

		public boolean hasMoreElements() {
			if(recent != null && recent.hasMoreElements())
					return true;
			while(nextIteration()) {
				EOQualifier qual = new EOAndQualifier(quals);
				EOFetchSpecification fs = new EOFetchSpecification(ent.entityName(),qual,null);
				fs.setFetchesRawRows(true);
				NSArray found = ent.ec.objectsWithFetchSpecification(fs);
				if(found != null && found.count() > 0) {
					recent = found.objectEnumerator();
					return true;
				}
			}
			return false;
		}

		private boolean nextIteration() {
			if(itrAttr == null)
				return (recent == null);
			if(itrIdx == null) {
				for (int i = 0; i < itrAttr.length; i++) {
					quals.addObject(new EOKeyValueQualifier(itrAttr[i],
							EOQualifier.QualifierOperatorEqual, itrValues[i].objectAtIndex(0)));
				}
				itrIdx = new int[itrAttr.length];
				return true;
			}
			int i = 0;
			itrIdx[i]++;
			while (itrIdx[i] >= itrValues[i].count()) {
				itrIdx[i] = 0;
				EOQualifier qual = new EOKeyValueQualifier(itrAttr[i],
						EOQualifier.QualifierOperatorEqual, itrValues[i].objectAtIndex(0));
				quals.replaceObjectAtIndex(qual, base + i);
				i++;
				if(i >= itrIdx.length)
					return false;
				itrIdx[i]++;
			}
			EOQualifier qual = new EOKeyValueQualifier(itrAttr[i],
					EOQualifier.QualifierOperatorEqual, itrValues[i].objectAtIndex(itrIdx[i]));
			quals.replaceObjectAtIndex(qual, base + i);
			return true;
		}
		
		public NSKeyValueCoding nextElement() {
			if(!hasMoreElements())
				throw new NoSuchElementException();
			NSDictionary row = (NSDictionary) recent.nextElement();
			return ent.new Wrapper(ent.ec.faultForRawRow(row, ent.entityName()));
		}
		
	}
	
	public class Wrapper implements NSKeyValueCoding {
		protected EOEnterpriseObject obj;
		protected NSMutableDictionary dict = new NSMutableDictionary();
		
		public Wrapper(EOEnterpriseObject row) {
			obj = row;
		}
		
		public Object valueForKey(String key) {
			Object value = dict.valueForKey(key);
			if(value != null)
				return (value == NullValue)?null:value;
			value = getValue(obj,key);
			dict.takeValueForKey((value==null)?NullValue:value, key);
			return value;
		}
		
		public int hashCode() {
			return obj.hashCode();
		}
		public boolean equals(Object other) {
			if(other instanceof Wrapper) {
				EOEnterpriseObject obj2 = ((Wrapper)other).obj;
				return obj.equals(obj2);
			}
			return false;
		}
		
		public void takeValueForKey(Object arg0, String arg1) {
			throw new UnsupportedOperationException();
		}
		
		public String toString() {
			return obj.entityName() + '#' + obj.hashCode();
		}
	}
}
