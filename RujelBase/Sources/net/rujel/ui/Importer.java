// Importer.java: Class file for WO Component 'Importer'

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

package net.rujel.ui;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.text.Format;
import java.util.Enumeration;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import net.rujel.reusables.Counter;
import net.rujel.reusables.WOLogLevel;

import com.webobjects.appserver.WOActionResults;
import com.webobjects.appserver.WOComponent;
import com.webobjects.appserver.WOContext;
import com.webobjects.appserver.WOResponse;
import com.webobjects.foundation.*;

public class Importer extends WOComponent {
    public Importer(WOContext context) {
        super(context);
    }
    
    public static final Logger logger = Logger.getLogger("rujel.import");
    
    public WOComponent returnPage;
    public String consumerComponent;
    public String title;
    public Object consumerParams;
    public NSData data;
    public String separator;
    public String charset;
    public NSArray fields;
    public NSMutableArray list;
    public NSMutableArray titles;
    public NSMutableArray availableTitles;
    public Object item;
    public Object item2;
    public int index;
    public Boolean readFine;
    public Boolean interpreted;

    public void appendToResponse(WOResponse aResponse, WOContext aContext) {
    	if(data != null && list == null) {
    		list = parse();
    		if(fields != null) {
    			list = analyse(list);
    	    	availableTitles = new NSMutableArray(UNUSED);
    	    	Enumeration enu = fields.objectEnumerator();
    	    	while (enu.hasMoreElements()) {
    				NSMutableDictionary field = (NSMutableDictionary) enu.nextElement();
    				String name = (String)field.valueForKey("field");
    				if(!availableTitles.containsObject(name))
    					availableTitles.addObject(name);
    			}
    	    	
    		}
    	}
    	if(title == null)
    		title = (String)session().valueForKeyPath("strings.RujelBase_Base.import.Import");
    	super.appendToResponse(aResponse, aContext);
    }
    
    public WOActionResults retry() {
		list = null;
		interpreted = null;
		return null;
    }

    public WOActionResults goManual() {
		readFine = Boolean.FALSE;
		return null;
    }

	public WOActionResults importFile() {
        Object file = context().request().formValueForKey("file");
        if(file instanceof NSData)
        	data = (NSData)file;
		session().takeValueForKey(returnPage,"pushComponent");
		return this;
	}
    
    public NSMutableArray parse() {
    	try {
    		Enumeration cenu = null;
    		if(charset == null) {
    			NSArray charsets = (NSArray)session().valueForKeyPath(
    				"strings.RujelBase_Base.import.charsets");
    			cenu = charsets.objectEnumerator();
    			charset = (String)cenu.nextElement();
    		}
			CharsetDecoder decoder = Charset.forName(charset).newDecoder();
			if(cenu == null) {
				decoder.replaceWith("?");
				decoder.onMalformedInput(CodingErrorAction.REPLACE)
					.onUnmappableCharacter(CodingErrorAction.REPLACE);
			} else {
				decoder.onMalformedInput(CodingErrorAction.REPORT)
					.onUnmappableCharacter(CodingErrorAction.REPORT);
			}
			BufferedReader in = new BufferedReader(new InputStreamReader(
					data.stream(),decoder),1024);

//    		BufferedReader reader = new BufferedReader(new InputStreamReader(data.stream()));
    		NSMutableArray[] lists = null;
    		String[] spLib = new String[] {":" , ";" , "\\." , "," , "\t" , " "};
    		int[][] counts = null;
        	NSMutableArray result = new NSMutableArray();
        	if(titles == null)
        		titles = new NSMutableArray();
        	String line = null;
    		while (true) {
				try {
					line = in.readLine();
				} catch (java.nio.charset.CharacterCodingException e) {
					in.close();
					result.removeAllObjects();
					if(cenu != null && cenu.hasMoreElements()) {
						charset = (String)cenu.nextElement();
						decoder = Charset.forName(charset).newDecoder();
						decoder.onMalformedInput(CodingErrorAction.REPORT)
							.onUnmappableCharacter(CodingErrorAction.REPORT);
						in = new BufferedReader(
								new InputStreamReader(data.stream(),decoder),1024);
						continue;
					} else {
			    		logger.log(WOLogLevel.INFO,"Unsupported character set",
			    				new Object[] {session(),e});
						session().takeValueForKey("Unsupported character set", "message");
						return null;
					}
				}
				if(line == null)
					break;
				if(separator == null) {
					if(lists == null)
						lists = new NSMutableArray[6];
					if(counts == null)
						counts = new int[6][6];
					for (int i = 0; i < spLib.length; i++) {
						if(lists[i] == null)
							lists[i] = new NSMutableArray();
						String[] split = line.split(spLib[i]);
						int L = split.length;
						for (int j = 0; j < L; j++) {
							split[j] = split[j].trim();
							if(split[j].length() == 0) {
								if(L > 5)
									counts[i][0]--;
								else
									counts[i][L]--;
								continue;
							}
							char c0 = split[j].charAt(0);
							if(split[j].length() > 1) {
							char cX = split[j].charAt(split[j].length() -1);
							if((c0 == cX && (c0 == '"' || c0 == '\'')) ||
									(Character.isMirrored(c0) && Character.isMirrored(cX)))
								split[j] = split[j].substring(1,split[j].length() -1).trim();
							}
						}
						lists[i].addObject(split);
						if(L > 5)
							L = 0;
						counts[i][L]++;
					}
				} else {
					String[] split = line.split(separator);
					for (int j = 0; j < split.length; j++) {
						split[j] = split[j].trim();
						if(split[j].length() == 0)
							continue;
						char c0 = split[j].charAt(0);
						if(split[j].length() > 1) {
						char cX = split[j].charAt(split[j].length() -1);
						if((c0 == cX && (c0 == '"' || c0 == '\'')) ||
								(Character.isMirrored(c0) && Character.isMirrored(cX)))
							split[j] = split[j].substring(1,split[j].length() -1).trim();
						}
					}
					result.addObject(split);
		    		while (titles.count() < split.length) {
		    			titles.addObject(new NSMutableDictionary());
		    		}					
				}
			} // while reader can read 
    		if(counts != null) {
    			int bestI = 6;
    			int bestJ = 6;
    			for (int i = 0; i < 6; i++) {
    				int max = counts[i][0];
    				counts[i][0] = 0;
					for (int j = 1; j < 6; j++) {
						if(counts[i][j] > max) {
							max = counts[i][j];
							counts[i][0] = 6-j;
						}
					}
					if(counts[i][0] < bestJ) {
						bestJ = counts[i][0];
						bestI = i;
					}
				}
    			separator = spLib[bestI];
    			return lists[bestI];
    		}
        	return result;
    	} catch (Exception e) {
    		logger.log(WOLogLevel.WARNING,"Error importing",
    				new Object[] {session(),e});
			session().takeValueForKey(e.getMessage(), "message");
			return null;
		}
    }

    public void setSeparator(String set) {
    	if(set == null || !set.equals(separator)) {
    		titles = null;
    		list = null;
    	}
    	separator = set;
    }
    
    protected static final String UNUSED = "-= unused =-";
	public NSMutableArray analyse(NSArray source) {
    	if(source == null || source.count() == 0) {
    		list = null;
    		titles = null;
    		return null;
    	}
    	Enumeration enu = source.objectEnumerator();
    	if(titles == null)
    		titles = new NSMutableArray();
    	list = new NSMutableArray();
    	while (enu.hasMoreElements()) {
    		String[] arr = (String[]) enu.nextElement();
    		NSMutableDictionary dict = new NSMutableDictionary(arr,"array");
    		NSMutableDictionary[] matchD = new NSMutableDictionary[arr.length];
    		dict.takeValueForKey(matchD, "matchD");
    		while (titles.count() < arr.length) {
    			titles.addObject(new NSMutableDictionary());
    		}
    		for (int i = 0; i < arr.length; i++) {
    			if(arr[i].length() == 0)
    				continue;
    			Enumeration aEnu = fields.objectEnumerator();
    			while (aEnu.hasMoreElements()) {
    				NSMutableDictionary rx = (NSMutableDictionary) aEnu.nextElement();
    				Object value = testFormatter(rx, arr[i]);
    				if(value == null && testPattern(rx, arr[i]))
    					value = arr[i];
    				if(value != null) {
    					String field = (String)rx.valueForKey("field");
    					NSMutableDictionary ctd = (NSMutableDictionary)titles.objectAtIndex(i);
    					Counter cnt = (Counter)ctd.valueForKey(field);
    					if(cnt == null)
    						ctd.takeValueForKey(new Counter(1), field);
    					else
    						cnt.raise();
    					matchD[i] = rx;
    					break;
    				} // apply from pattern
    			} // fields enumeration
    		} // arr enumeration
    		list.addObject(dict);
    	} // list enumeration
    	enu = titles.objectEnumerator();
    	NSMutableDictionary bestTitles = new NSMutableDictionary();
    	for (int i = 0; i < titles.count(); i++) {  // choose best titles
    		NSMutableDictionary ctd = (NSMutableDictionary) titles.objectAtIndex(i);
    		enu = ctd.keyEnumerator();
    		int max = 0;
    		String best = (String)ctd.valueForKey("best");//"???";
    		if(best == null) {
    		best = UNUSED;
    		while (enu.hasMoreElements()) { // choose best title
    			String field = (String) enu.nextElement();
    			Counter cnt = (Counter)ctd.valueForKey(field);
    			if(cnt.intValue() > max) {
    				NSMutableDictionary exist = (NSMutableDictionary)
    				bestTitles.valueForKey(field);
    				if(exist != null) {
    					Counter exCnt = (Counter)exist.valueForKey(field);
    					if(exCnt.intValue() >= cnt.intValue())
    						continue;
    				}
    				max = cnt.intValue();
    				best = field;
    			}
    		} // field counters enumeration
    		ctd.takeValueForKey(best, "best");
    		}
    		NSMutableDictionary exist = (NSMutableDictionary)bestTitles.valueForKey(best);
    		if(exist != null)
    			exist.takeValueForKey(UNUSED, "best");
    		if(max > 0)
    			bestTitles.takeValueForKey(ctd, best);
    	} // titles enumeration
    	enu = list.objectEnumerator();
    	// put values into dictionary
    	while (enu.hasMoreElements()) {
    		NSMutableDictionary dict = (NSMutableDictionary) enu.nextElement();
    		String[] arr = (String[])dict.valueForKey("array");
    		NSMutableDictionary[] matchD = (NSMutableDictionary[])dict.valueForKey("matchD");
    		if(matchD == null) {
    			matchD = new NSMutableDictionary[arr.length];
    			dict.takeValueForKey(matchD, "matchD");
    		}
    		int matches = 0;
    		for (int i = 0; i < arr.length; i++) {
    			NSMutableDictionary ctd = (NSMutableDictionary)titles.objectAtIndex(i);
    			String field = (String)ctd.valueForKey("best");
    			if(field == null || field == UNUSED) {
    				matchD[i] = null;
    				continue;
    			}
    			if(arr[i].length() == 0) {
    				matchD[i] = null;
    				continue;
    			}
    			dict.takeValueForKey(arr[i], field);
				Object value = null;
				if(matchD[i] == null || !field.equals(matchD[i].valueForKey("field"))) {
					matchD[i] = null;
					Enumeration aEnu = fields.objectEnumerator();
					while (aEnu.hasMoreElements()) {
						NSMutableDictionary rx = (NSMutableDictionary) aEnu.nextElement();
						if(!field.equals(rx.valueForKey("field")))
							continue;
						value = testFormatter(rx, arr[i]);
						if(value == null) {
							if(rx.valueForKey("dateformat") != null) {
								dict.takeValueForKey(null, field);
								continue;
							}
							if(testPattern(rx, arr[i])) {
								value = arr[i];
							}
						}
						if(value != null) {
							matchD[i] = rx;
							break;
						}
					}
    			} else {
    				if(matchD[i].valueForKey("dateformat") != null)
    					value = testFormatter(matchD[i], arr[i]);
    				else
    					value = arr[i];
    			}
				if(value != null) {
					dict.takeValueForKey(value, field);
					NSDictionary extInfo = (NSDictionary)matchD[i].valueForKey("extInfo");
					if(extInfo != null) {
						Enumeration eienu = extInfo.keyEnumerator();
						while (eienu.hasMoreElements()) {
							String key = (String) eienu.nextElement();
							if(dict.valueForKey(key)==null)
								dict.takeValueForKey(extInfo.valueForKey(key), key);
						}
					}
					Number match = (Number)matchD[i].valueForKey("match");
					if(match == null)
						matches++;
					else
						matches += match.intValue();
				}
    		}
    		dict.takeValueForKey(new Integer(matches), "matches");
    	} // list enumeration
    	if(titles == null || titles.count() == 0) {
    		readFine = Boolean.FALSE;
    	} else {
    		enu = titles.objectEnumerator();
    		int needed = 0;
    		while (enu.hasMoreElements()) {
				NSMutableDictionary tDict = (NSMutableDictionary) enu.nextElement();
				String field = (String)tDict.valueForKey("best");
				if(field == null)
					continue;
				if(field.equals("lastName") || field.equals("firstName"))
					needed++;
			}
    		readFine = Boolean.valueOf(needed >= 2);
    	}
    	return list;
    }
    
    @SuppressWarnings("deprecation")
	private Object testFormatter(NSMutableDictionary dict, String value) {
    	NSArray formatters = (NSArray)dict.valueForKey("formatters");
    	if(formatters == null) {
    		Object inDict = dict.valueForKey("dateformat");
    		if(inDict == null)
    			return null;
    		if(inDict instanceof NSArray) {
    			Enumeration enu = ((NSArray)inDict).objectEnumerator();
    			NSMutableArray prepare = new NSMutableArray();
    			while (enu.hasMoreElements()) {
					String pattern = (String) enu.nextElement();
					try {
						prepare.addObject(new NSTimestampFormatter(pattern));
					} catch (Exception e) {
						logger.log(WOLogLevel.WARNING,"Error making dateFormatter from pattern: "
								+ pattern, new Object[] {session(),e});
					}
				}
    			if(prepare.count() > 0)
    				formatters = prepare.immutableClone();
    		} else if(inDict instanceof String) {
				try {
					formatters = new NSArray(new NSTimestampFormatter((String)inDict));
				} catch (Exception e) {
					logger.log(WOLogLevel.WARNING,"Error making dateFormatter from pattern: "
							+ inDict, new Object[] {session(),e});
				}
    		}
    		if(formatters == null)
    			formatters = NSArray.EmptyArray;
    		dict.takeValueForKey(formatters, "formatters");
    	}
    	if(formatters.count() == 0)
    		return null;
    	Enumeration enu = formatters.objectEnumerator();
    	String noapo = (value.charAt(0) == '\'')?value.substring(1):null;
    	while (enu.hasMoreElements()) {
			Format fmt = (Format) enu.nextElement();
			try {
				Object result = fmt.parseObject(value);
				if(result != null)
					return result;
			} catch (Exception e) {
				// fail
			}
			if(noapo != null) {
				try {
					Object result = fmt.parseObject(noapo);
					if(result != null)
						return result;
				} catch (Exception e2) {
					//epic fail
				}
			}
		}
		return null;
	}
    
    private boolean testPattern(NSMutableDictionary rx, String value) {
		Pattern pattern = (Pattern)rx.valueForKey("pattern");
		if(pattern == null) {
			String regex = (String)rx.valueForKey("regex");
			if(regex == null)
				return false;
			pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE + Pattern.UNICODE_CASE);
			rx.takeValueForKey(pattern, "pattern");
		}
		return pattern.matcher(value).matches();
    }
    
    public Object value() {
    	Object[] array = null;
    	if(item instanceof Object[])
    		array = (Object[])item;
    	else if(item instanceof NSMutableDictionary) {
    		array = (Object[])((NSMutableDictionary)item).valueForKey("array");
    	}
		if(array == null)
			return null;
		if(index < 0 || index >= array.length)
			return null;
		return array[index];
    }
    
    public String valueClass() {
    	if(titles == null || index < 0 || !(item instanceof NSMutableDictionary))
    		return null;
    	NSMutableDictionary[] matchD = (NSMutableDictionary[])(
    			(NSMutableDictionary)item).valueForKey("matchD");
    	if(matchD == null || index >= matchD.length)
    		return null;
    	if(matchD[index] != null)
    		return "green";
    	return null;
    }
}