// FractionPresenter.java

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

package net.rujel.criterial;


import java.math.*;


public interface FractionPresenter {
	
	public static final FractionPresenter PERCENTAGE = new Percent();
	public static final FractionPresenter NONE = new None();
	
	public String title();
	
	public String presentFraction(double fraction);
	public String presentFraction(BigDecimal fraction);
	
	public BigDecimal borderForKey(String key) throws IllegalArgumentException;
	
	public static class Percent implements FractionPresenter {
		protected int prec = 0;

		public Percent() {
			super();
		}
		
		public Percent(int precision) {
			super();
			prec = precision;
		}
		
		public String title() {
			String result = "%";
			if(prec > 0)
				result = result + prec;
			return result;
		}
		
		public String presentFraction(double fraction) {
			StringBuffer buf = new StringBuffer();
			double proc = fraction*100;
			buf.append((int)proc);
			if(prec > 0) {
				double remain = proc - (int)proc;
				int digs = 10^prec;
				buf.append('.').append((int)remain*digs);
			}
			buf.append('%');
			return buf.toString();
		}
		
		public String presentFraction(BigDecimal fraction) {
			if(fraction == null) return null;
			BigDecimal result = fraction.movePointRight(2);
			result = result.setScale(prec,BigDecimal.ROUND_HALF_UP);
			return result.toString();
		}
	
		public BigDecimal borderForKey(String key) throws IllegalArgumentException {
			try {
				BigDecimal dec = new BigDecimal(key);
				return dec.movePointLeft(2);
			} catch (NumberFormatException nex) {
				throw new IllegalArgumentException("No such key - '" + key +'\'');
			}
		}
		
	}
	
	public static class None implements FractionPresenter {
		protected String string;

		public None() {
			super();
		}
		
		public None(String present) {
			super();
			string = present;
		}
		
		public String title() {
			if(string == null)
				return "none";
			else
				return "none" + string;
		}
		
		public String presentFraction(double fraction) {
			return string;
		}
		public String presentFraction(BigDecimal fraction) {
			return string;
		}
		
		public BigDecimal borderForKey(String key) throws IllegalArgumentException {
			if(string == null) {
				if(key == null)
					return BigDecimal.ZERO;
			} else if(string.equals(key)) {
				return BigDecimal.ZERO;
			}
			throw new IllegalArgumentException("No such key - '" + key +'\'');			
		}

	}
}
