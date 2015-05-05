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

import org.xml.sax.SAXException;

import net.rujel.rest.Agregator.ParseError;
import net.rujel.reusables.Various;
import net.rujel.reusables.xml.EasyGenerationContentHandlerProxy;

import com.webobjects.eocontrol.EOAndQualifier;
import com.webobjects.eocontrol.EOEditingContext;
import com.webobjects.eocontrol.EOEnterpriseObject;
import com.webobjects.eocontrol.EOFetchSpecification;
import com.webobjects.eocontrol.EOKeyValueQualifier;
import com.webobjects.eocontrol.EOOrQualifier;
import com.webobjects.eocontrol.EOQualifier;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSDictionary;
import com.webobjects.foundation.NSKeyValueCoding;
import com.webobjects.foundation.NSMutableArray;
import com.webobjects.foundation.NSMutableDictionary;
import com.webobjects.foundation.NSRange;
import com.webobjects.foundation.NSSelector;

public abstract class AgrEntity {
	
	protected EOEditingContext ec;
	
	public static AgrEntity forName(String entName, EOEditingContext ec) {
		if(entName == null)
			return null;
		AgrEntity result = null;
		if(entName.equalsIgnoreCase("itogMark"))
			result = new AgrItogMark();
		if(entName.equalsIgnoreCase("course"))
			result = new AgrEduCourse();
		if(entName.equalsIgnoreCase("prognos"))
			result = new AgrPrognosis();
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
						int end = value.lastIndexOf('}');
						if(end < i)
							throw new Agregator.ParseError("Closing bracket not found",end, value);
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
		if(snv != null && snv.length > 0) {
			if(snv[0] == EOQualifier.QualifierOperatorEqual) {
				if(snv.length > 2) {
					NSMutableArray or = new NSMutableArray();
					for (int i = 1; i < snv.length; i++) {
						or.addObject(new EOKeyValueQualifier(attrib, 
								(NSSelector)snv[0], new Integer((String)snv[i])));
					}
					quals.addObject(new EOOrQualifier(or));
				} else {
					quals.addObject(new EOKeyValueQualifier(attrib, 
							(NSSelector)snv[0], new Integer((String)snv[1])));
				}
			} else {
				quals.addObject(new EOKeyValueQualifier(attrib, 
						(NSSelector)snv[0], new Integer((String)snv[1])));
				if(snv.length >= 4) {
					quals.addObject(new EOKeyValueQualifier(attrib, 
							(NSSelector)snv[2], new Integer((String)snv[3])));
				}
			}
/*			
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
			} */
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
	
	public static EOQualifier  getStringQual(String attrib, String value) 
	throws ParseError {
		if(value == null || value.length() == 0 ||value.equals("*"))
			return null;
		String trim = value.trim();
		if((trim.charAt(0) == '\'' && trim.charAt(trim.length() -1) == '\'') ||
				(trim.charAt(0) == '\"' && trim.charAt(trim.length() -1) == '\"')) {
			return new EOKeyValueQualifier(attrib, EOQualifier.QualifierOperatorEqual,
					 trim.substring(1, trim.length() -1));
		}
		array:
		if(trim.charAt(0) == '{' && trim.charAt(trim.length() -1) == '}') {
			int idx = 1;
			while (Character.isWhitespace(trim.charAt(idx))) {
				idx++;
			}
			if(idx >= trim.length() -1)
				break array;
			NSMutableArray values = new NSMutableArray();
			char quot = trim.charAt(idx);
			do {
				int next = trim.indexOf(quot, idx + 1);
				if(next < idx)
					throw new ParseError("Cant find closing quote " + attrib, idx, trim);
				values.addObject(trim.substring(idx+1, next));
				idx = trim.indexOf(quot, next + 2);
			} while (idx > 0);
			return Various.getEOInQualifier(attrib, values);
		}
		return new EOKeyValueQualifier(attrib, EOQualifier.QualifierOperatorEqual,trim);
	}
	
	protected static class RowsEnum implements Enumeration {
		private String[] itrAttr;
		protected NSArray[] itrValues;
		private int[] itrIdx;
		protected NSMutableDictionary iterDict;
		protected NSMutableArray quals;
		private int base;
		protected AgrEntity ent;
		private Enumeration recent;
		private Wrapper ongoing;
		
		public RowsEnum(AgrEntity entity,NSArray baseQuals,String itrAttr,NSArray itrValue) {
			this(entity,baseQuals,new String[]{itrAttr},new NSArray[] {itrValue});
		}
		
		public RowsEnum(AgrEntity entity,NSArray baseQuals,String[] itrAttr,NSArray[] itrValues) {
			ent = entity;
			quals = baseQuals.mutableClone();
			base = quals.count();
			if(itrAttr != null) {
				int len = itrAttr.length;
				this.itrAttr = new String[len];
				this.itrValues = new NSArray[len];
				for (int i = 0; i < len; i++) {
					this.itrAttr[i] = itrAttr[i];
					this.itrValues[i] = itrValues[i].immutableClone();
				}
			}
		}
		
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
			if(nextRecent())
					return true;
			while(nextIteration()) {
				EOQualifier qual = new EOAndQualifier(quals);
				EOFetchSpecification fs = new EOFetchSpecification(ent.entityName(),qual,null);
				fs.setFetchesRawRows(true);
				NSArray found = ent.ec.objectsWithFetchSpecification(fs);
				if(found != null && found.count() > 0) {
					recent = found.objectEnumerator();
					if(nextRecent())
						return true;
				} else {
					recent = NSArray.EmptyArray.objectEnumerator();
				}
			}
			return false;
		}
		
		private boolean nextRecent() {
			if(ongoing != null)
				return true;
			if(recent == null)
				return false;
			while (recent.hasMoreElements()) {
				NSDictionary row = (NSDictionary) recent.nextElement();
				ongoing = ent.new Wrapper(row);
				if(iterDict != null)
					ongoing.iterDict = this.iterDict.immutableClone();
				if(qualifies(ongoing))
					return true;
				ongoing = null;
			}
			return false;
		}
		
		protected void restart() {
			itrIdx = null;
			if(quals.count() > base)
				quals.removeObjectsInRange(new NSRange(base, itrAttr.length));
		}

		protected boolean nextIteration() {
			if(itrAttr == null)
				return (recent == null);
			if(itrIdx == null) {
				iterDict = new NSMutableDictionary();
				for (int i = 0; i < itrAttr.length; i++) {
					if(itrValues[i].count() == 0)
						return false;
					Object value = itrValues[i].objectAtIndex(0);
					quals.addObject(new EOKeyValueQualifier(itrAttr[i],
							EOQualifier.QualifierOperatorEqual, value));
					iterDict.takeValueForKey(value, itrAttr[i]);
				}
				itrIdx = new int[itrAttr.length];
				return true;
			} else if(recent == null) {
				return false;
			}
			int i = 0;
			itrIdx[i]++;
			while (itrIdx[i] >= itrValues[i].count()) {
				itrIdx[i] = 0;
				Object value = itrValues[i].objectAtIndex(0);
				EOQualifier qual = new EOKeyValueQualifier(itrAttr[i],
						EOQualifier.QualifierOperatorEqual, value);
				iterDict.takeValueForKey(value, itrAttr[i]);
				quals.replaceObjectAtIndex(qual, base + i);
				i++;
				if(i >= itrIdx.length) {
					recent = null;
					return false;
				}
				itrIdx[i]++;
			}
			Object value = itrValues[i].objectAtIndex(itrIdx[i]);
			EOQualifier qual = new EOKeyValueQualifier(itrAttr[i],
					EOQualifier.QualifierOperatorEqual, value);
			iterDict.takeValueForKey(value, itrAttr[i]);
			quals.replaceObjectAtIndex(qual, base + i);
			return true;
		}
		
		public NSKeyValueCoding nextElement() {
			if(!hasMoreElements())
				throw new NoSuchElementException();
//			if(!nextRecent())
//				throw new NoSuchElementException();
			Wrapper result = ongoing;
			ongoing = null;
			return result;
		}
		
		protected boolean qualifies(Wrapper obj) {
			return true;
		}

	}
	
	protected class Wrapper implements NSKeyValueCoding {
		protected NSDictionary row;
		protected EOEnterpriseObject obj;
		protected NSMutableDictionary dict = new NSMutableDictionary();
		protected NSDictionary iterDict;
		
		public Wrapper(NSKeyValueCoding row) {
			if(row instanceof EOEnterpriseObject) {
				obj = (EOEnterpriseObject)row;
			} else if(row instanceof NSDictionary) {
				this.row = (NSDictionary)row;
				obj = ec.faultForRawRow((NSDictionary)row, entityName());
			}
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
		
		public void parce(EasyGenerationContentHandlerProxy handler) throws SAXException {
			handler.prepareAttribute("entity", obj.entityName());
			handler.prepareAttribute("id", Integer.toString(obj.hashCode()));
			handler.element("object", null);
		}
	}
}
