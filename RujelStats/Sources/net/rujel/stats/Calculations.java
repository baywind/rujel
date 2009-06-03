// Calculations.java

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

package net.rujel.stats;

import java.lang.reflect.Method;

import com.webobjects.appserver.WOApplication;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSDictionary;
import com.webobjects.foundation.NSMutableArray;
import com.webobjects.foundation.NSMutableDictionary;

public class Calculations {
	
	public static final String omitKey = (String)WOApplication.application().valueForKeyPath(
			"strings.RujelStats_Stats.omitKey");
	protected static final NSDictionary formulas = (NSDictionary)WOApplication.application().
			valueForKeyPath("strings.RujelStats_Stats.formulas");

	
	protected static int count(String key, NSDictionary dict) {
		if(dict == null)
			return 0;
//		Object value = dict.valueForKey("dict");
//		if(value instanceof NSDictionary)
//			dict = (NSDictionary)value;
		Object value = dict.objectForKey(key);
		if(value instanceof Number)
			return ((Number)value).intValue();
		return 0;
	}
	
	protected static final String[] func = new String[]
	                                    {"average","uspevaemost","znaniy","sou"};
	protected static NSArray _allFormulas;
	
	public static NSArray allFormulas() {
		if(_allFormulas != null)
			return _allFormulas;
		NSMutableArray result = new NSMutableArray();
		NSArray paramValues = new NSArray(".");
		for (int i = 0; i < func.length; i++) {
			Object tmp = formulas.valueForKey(func[i]);
			NSMutableDictionary col = null; 
			if(tmp instanceof NSDictionary)
				col = ((NSDictionary)tmp).mutableClone();
			else if (tmp instanceof String)
				col = new NSMutableDictionary(tmp,"title");
			else
				col = new NSMutableDictionary();
			try {
				Method method = Calculations.class.getMethod(func[i], NSDictionary.class);
				tmp = new NSDictionary(new Object[] {func[i],method,paramValues},
						new Object[] {"methodName","parsedMethod","paramValues"});
				col.setObjectForKey(tmp, "value");
			} catch (Exception e) {
//				StatsModule.logger
			}
			col.setObjectForKey(new Integer(20 + i), "sort");
			result.addObject(col);
		}
		return result;
	}
	
	public static Number uspevaemost(NSDictionary dict) {
		if(dict == null)
			return null;
		NSDictionary vals = (NSDictionary)dict.objectForKey("dict");
		if(vals == null)
			vals = dict;
		int total = count("total",dict) - count(omitKey,vals);
		if(total == 0)
			return null;
		int chisl = count("5", vals) + count("4", vals) + count("3", vals);
		return new Double((double)100*chisl/total);
	}
	
	public static Number znaniy(NSDictionary dict) {
		if(dict == null)
			return null;
		NSDictionary vals = (NSDictionary)dict.objectForKey("dict");
		if(vals == null)
			vals = dict;
		int total = count("total",dict) - count(omitKey,vals);
		if(total == 0)
			return null;
		int chisl = count("5",vals) + count("4", vals);
		return new  Double((double)100*chisl/total);
	}

	public static Number average(NSDictionary dict) {
		if(dict == null)
			return null;
		NSDictionary vals = (NSDictionary)dict.objectForKey("dict");
		if(vals == null)
			vals = dict;
		int total = count("total",dict) - count(omitKey,vals);
		if(total == 0)
			return null;
		int chisl = count("5",vals)*5;
		chisl += count("4",vals)*4;
		chisl += count("3",vals)*3;
		chisl += count("2",vals)*2;
		return new Double((double)chisl/total);
	}

	public static Number usvoen(NSDictionary dict) {
		if(dict == null)
			return null;
		return new Integer(0);
	}

	public static Number sou(NSDictionary dict) {
		if(dict == null)
			return null;
		NSDictionary vals = (NSDictionary)dict.objectForKey("dict");
		if(vals == null)
			vals = dict;
		int total = count("total",dict) - count(omitKey,vals);
		if(total == 0)
			return null;
		int chisl = count("5",vals)*100;
		chisl += count("4",vals)*64;
		chisl += count("3",vals)*36;
//		chisl += count("2",vals)*16;
//		chisl += count("н/а",vals)*7;
		return new Double((double)chisl/total);
	}

}
