// Agregator.java

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

import net.rujel.reusables.Counter;

import com.webobjects.eocontrol.EOAndQualifier;
import com.webobjects.eocontrol.EOKeyValueQualifier;
import com.webobjects.eocontrol.EOOrQualifier;
import com.webobjects.eocontrol.EOQualifier;
import com.webobjects.foundation.NSKeyValueCoding;
import com.webobjects.foundation.NSMutableArray;
import com.webobjects.foundation.NSSelector;

public class Agregator {

	public static final int COUNT = 0;
	public static final int SUM = 1;
	public static final int AVG = 2;
	
	private int act = COUNT;
	private EOQualifier qual;
	private String attrib;
	
	private int count = 0;
	private int sum = 0;
	private BigDecimal dsum;
	
	public Agregator(int action, String attribute,EOQualifier qualifier) {
		if(action < 0 || action > 2)
			throw new IllegalArgumentException("Unknown action code");
		act = action;
		qual = qualifier;
		attrib = attribute;
	}
	
	private static char skipWhitespace(String source, Counter pos) {
		for (int i = pos.intValue(); i < source.length(); i++) {
			char c = source.charAt(i);
			if(!Character.isWhitespace(c)) {
				pos.setValue(i);
				return c;
			}
		}
		pos.setValue(source.length());
		return ' ';
	}
	
	private static String readAttribute(String source, Counter pos) {
		skipWhitespace(source, pos);
		for (int i = pos.intValue(); i < source.length(); i++) {
			char c = source.charAt(i);
			if(!Character.isLetterOrDigit(c) && c != '_') {
				if(i == pos.intValue())
					throw new IllegalArgumentException("Attribute name expected");
				String result = source.substring(pos.intValue(), i);
				pos.setValue(i);
				return result;
			}
		}
		throw new IllegalArgumentException("Unexpected end of line");
	}
	
	public static int actForName(String act) {
		if(act.equalsIgnoreCase("count")  || act.equalsIgnoreCase("cnt"))
			return COUNT;
		if(act.equalsIgnoreCase("sum"))
			return SUM;
		if(act.equalsIgnoreCase("avg"))
			return AVG;
		throw new IllegalArgumentException("Unknown action name '" + act + '\'');
	}
	
	private static NSSelector readSelector(String source, Counter pos) {
		char c = skipWhitespace(source, pos);
		int start = pos.intValue();
		if(c == ':') {
			pos.raise();
			return null;
		}
		if(c != '=' && c != '>' && c != '<' && c != '!')
			throw new IllegalArgumentException("Qualifier string expected");
		if(source.charAt(pos.raise()) == '=')
			pos.raise();
		String sel = source.substring(start,pos.intValue());
		return EOQualifier.operatorSelectorForString(sel);
	}
	
	private static EOQualifier readBrackets(String source, String attribute, Counter pos) {
		char c = skipWhitespace(source, pos);
		NSSelector sel = null;
		NSMutableArray values = null;
		if(c == '(') {
			sel = EOQualifier.QualifierOperatorGreaterThan;
		} else if(c == '[') {
			sel = EOQualifier.QualifierOperatorGreaterThanOrEqualTo;
		} else if(c == '{') {
			values = new NSMutableArray();
		} else {
			throw new IllegalArgumentException("Opening bracket expected after ':'");
		}
		pos.raise();
		NSMutableArray quals = new NSMutableArray(2);
		if(values == null) {
			Object value = readValue(source, pos);
			quals.addObject(new EOKeyValueQualifier(attribute,sel,value));
			if(skipWhitespace(source, pos) != ',')
				throw new IllegalArgumentException("Comma expected in brackets");
			pos.raise();
			value = readValue(source, pos);
			c = skipWhitespace(source, pos);
			if(c == ')')
				sel = EOQualifier.QualifierOperatorLessThan;
			else if(c == ']')
				sel = EOQualifier.QualifierOperatorLessThanOrEqualTo;
			else
				throw new IllegalArgumentException("Closing bracket expected");
			pos.raise();
			quals.addObject(new EOKeyValueQualifier(attribute,sel,value));
			return new EOAndQualifier(quals);
		} else {
			sel = EOQualifier.QualifierOperatorEqual;
			while (true) {
				Object value = readValue(source, pos);
				quals.addObject(new EOKeyValueQualifier(attribute,sel,value));
				c = skipWhitespace(source, pos);
				if(c == ',') {
					pos.raise();
				} else if (c == '}') {
					pos.raise();
					break;
				} else {
					throw new IllegalArgumentException("Closing bracket or comma expected.");
				}
			}
			return new EOOrQualifier(quals);
		}
	}

	
	private static Object readValue(String source, Counter pos) {
		if(skipWhitespace(source, pos) == '\'')
			return readQuote(source, pos);
		if(source.regionMatches(true, pos.intValue(), "null", 0, 4)) {
			pos.add(4);
			return null;
		}
		if(source.regionMatches(true, pos.intValue(), "nil", 0, 3)) {
			pos.add(3);
			return null;
		}
		boolean decimal = false;
		String result = null;
		for (int i = pos.intValue(); i < source.length(); i++) {
			char c = source.charAt(i);
			if(!Character.isDigit(c)) {
				if(c == '.') {
					if(decimal)
						throw new IllegalArgumentException("Wrong decimal format");
					else
						decimal = true;
				} else {
					result = source.substring(pos.intValue(), i);
					pos.setValue(i);
					break;
				}
			}
		}
		if(result == null)
			result = source.substring(pos.intValue());
		if(result.length() == 0) {
			throw new IllegalArgumentException("Unquoted nondigit value");
		}
		if(decimal)
			return new BigDecimal(result);
		else
			return new Integer(result);
	}

	private static String readQuote(String source, Counter pos) {
		if(source.charAt(pos.intValue()) == '\'')
			pos.raise();
		for (int i = pos.intValue(); i < source.length(); i++) {
			char c = source.charAt(i);
			if(c == '\'') {
				String result = source.substring(pos.intValue(), i);
				pos.setValue(i +1);
				return result;
			}
		}
		throw new IllegalArgumentException("Unclosed quotation");
	}
	
	public static EOQualifier parceQualifier(String source, Counter pos) {
		NSMutableArray quals = new NSMutableArray();
		boolean and = true;
		read:
		while (pos.intValue() < source.length()) {
			char c = skipWhitespace(source, pos);
			if(c == ' ') {
				break read;
			} else if(c == '*') {
				return null;
			} else if(c == ')') {
				pos.raise();
				break read;
			} else if(c == '(') {
				pos.raise();
				quals.addObject(parceQualifier(source, pos));
			} else {
				String attribute = readAttribute(source, pos);
				NSSelector sel = readSelector(source, pos);
				if(sel == null) {
					EOQualifier qual = readBrackets(source, attribute, pos);
					quals.addObject(qual);
				} else {
					Object value = readValue(source, pos);
					quals.addObject(new EOKeyValueQualifier(attribute, sel, value));
				}
			}
			c = skipWhitespace(source, pos);
			if(c == ' ') {
				break read;
			} else if(c == ')') {
				pos.raise();
				break read;
			} else if(c == '|') {
				if(and && quals.count() > 1)
					throw new IllegalArgumentException("Unexpected '&' and '|' mix.");
				else
					and = false;
			} else if(c == '&') {
				if(!and)
					throw new IllegalArgumentException("Unexpected '&' and '|' mix.");
			} else {
				throw new IllegalArgumentException("Expected '&' or '|'");
			}
			pos.raise();
		}
		if(quals.count() == 0)
			return null;
		if(quals.count() == 1)
			return (EOQualifier)quals.objectAtIndex(0);
		if(and)
			return new EOAndQualifier(quals);
		else
			return new EOOrQualifier(quals);
	}
	
	public boolean scan(NSKeyValueCoding row) {
		if(qual != null && !qual.evaluateWithObject(row))
			return false;
		if(attrib != null) {
			Object value = row.valueForKey(attrib);
			if(value == null)
				return false;
			if(value instanceof Agregator)
				value = ((Agregator)value).getResult();
			if(value instanceof Integer) {
				sum += ((Integer)value).intValue();
			} else {
				BigDecimal toAdd;
				if(value instanceof BigDecimal)
					toAdd = (BigDecimal)value;
				else
					toAdd = new BigDecimal(value.toString());
				if(dsum == null)
					dsum = toAdd;
				else
					dsum = dsum.add(toAdd);
			}
		}
		count++;
		return true;
	}
	
	public static Agregator parceAgregator(String source) {
		if(source.equalsIgnoreCase("count"))
			return new Agregator(COUNT, null, null);
		Counter pos = new Counter();
		String txt = readAttribute(source, pos);
		int action = actForName(txt);
		if(skipWhitespace(source, pos) != '(')
			throw new IllegalArgumentException("Opening bracket expected");
		int idx = pos.raise();
		String attribute = null;
		try {
			attribute = readAttribute(source, pos);
			if(skipWhitespace(source, pos) == ',') {
				pos.raise();
			} else {
				attribute = null;
				pos.setValue(idx);
			}
		} catch (Exception e) {
			pos.setValue(idx);
		}
		EOQualifier qualifier = parceQualifier(source, pos);
		return new Agregator(action, attribute, qualifier);
	}

	
	public Number getResult() {
		if(act == COUNT)
			return new Integer(count);
		if(dsum !=null && sum > 0) {
			dsum = dsum.add(new BigDecimal(sum));
			sum = 0;
		}
		if(act == SUM) {
			if(dsum == null)
				return new Integer(SUM);
			else
				return dsum;
		}
		if(act == AVG) {
			if(dsum == null) {
				BigDecimal dSum = new BigDecimal(sum);
				return dSum.divide(new BigDecimal(count), 2, BigDecimal.ROUND_HALF_UP);
			} else {
				return dsum.divide(new BigDecimal(count),
						dsum.scale() +1, BigDecimal.ROUND_HALF_UP);
			}
		}
		return null;
	}
	
	public String toString() {
		Number result = getResult();
		if(result == null)
			return "NaN";
		return result.toString();
	}
	
	public int getCount() {
		return count;
	}
	
	public String getAttribute() {
		return attrib;
	}
	
	public Number getSum() {
		if(dsum !=null && sum > 0) {
			dsum = dsum.add(new BigDecimal(sum));
			sum = 0;
		}
		if(dsum == null)
			return new Integer(sum);
		else
			return dsum;
	}
	
	public int setType(String type) {
		act = actForName(type);
		return act;
	}
	
	public String getType() {
		switch (act) {
		case COUNT:
			return "count";
		case SUM:
			return "sum";
		case AVG:
			return "avg";
		default:
			return "unknown";
		}
	}
}
