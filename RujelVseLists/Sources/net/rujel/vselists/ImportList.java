package net.rujel.vselists;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.util.Enumeration;
import java.util.regex.Pattern;

import com.webobjects.appserver.WOContext;
import com.webobjects.foundation.*;

import net.rujel.interfaces.Person;
import net.rujel.reusables.Counter;
import net.rujel.reusables.WOLogLevel;
import net.rujel.ui.LessonList;

public class ImportList extends LessonList {
    public ImportList(WOContext context) {
        super(context);
        Object file = context.request().formValueForKey("file");
        if(file instanceof NSData)
        	data = (NSData)file;
    }
    
    public NSData data;
    public VseEduGroup targetGroup;
    public String separator = null;
    protected NSMutableArray list;
    public NSMutableArray titles;
    public Object item;
    public Object item2;
    public int index;
    public Boolean readFine;
    
    public NSArray list() {
    	if(list != null)
    		return list;
    	return analyse(parse());
    }
    
    public NSArray parse() {
    	try {
    		NSArray charsets = (NSArray)session().valueForKeyPath(
    				"strings.RujelVseLists_VseStrings.charsets");
			Enumeration cenu = charsets.objectEnumerator();
			CharsetDecoder decoder = Charset.forName((String)cenu.nextElement()).newDecoder();
			decoder.onMalformedInput(CodingErrorAction.REPORT)
				.onUnmappableCharacter(CodingErrorAction.REPORT);
			BufferedReader in = new BufferedReader(new InputStreamReader(
					data.stream(),decoder),1024);

//    		BufferedReader reader = new BufferedReader(new InputStreamReader(data.stream()));
    		NSMutableArray[] lists = null;
    		String[] spLib = new String[] {":" , ";" , "\\." , "," , "\t" , " "};
    		int[][] counts = null;
        	NSMutableArray result = new NSMutableArray();
        	String line = null;
    		while (true) {
				try {
					line = in.readLine();
				} catch (java.nio.charset.CharacterCodingException e) {
					in.close();
					result.removeAllObjects();
					if(cenu.hasMoreElements()) {
						String charset = (String)cenu.nextElement();
						decoder = Charset.forName(charset).newDecoder();
						decoder.onMalformedInput(CodingErrorAction.REPORT)
							.onUnmappableCharacter(CodingErrorAction.REPORT);
						in = new BufferedReader(
								new InputStreamReader(data.stream(),decoder),1024);
						continue;
					} else {
			    		ListsEditor.logger.log(WOLogLevel.INFO,"Unsupported character set",
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
							char cX = split[j].charAt(split[j].length() -1);
							if((c0 == cX && (c0 == '"' || cX == '\'')) ||
									(Character.isMirrored(c0) && Character.isMirrored(cX)))
								split[j] = split[j].substring(1,split[j].length() -1).trim();
						}
						lists[i].addObject(split);
						if(L > 5)
							L = 0;
						counts[i][L]++;
					}
				} else {
					result.addObject(line.split(separator));
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
    		ListsEditor.logger.log(WOLogLevel.WARNING,"Error importing",
    				new Object[] {session(),e});
			session().takeValueForKey(e.getMessage(), "message");
			return null;
		}
    }    	

    @SuppressWarnings("deprecation")
	public NSArray analyse(NSArray source) {
    	if(source == null || source.count() == 0) {
    		list = null;
    		titles = null;
    		return null;
    	}
    	Enumeration enu = source.objectEnumerator();
    	titles = new NSMutableArray();
    	list = new NSMutableArray();
    	NSArray assist = (NSArray)session().valueForKeyPath(
    			"strings.RujelVseLists_VseStrings.importAssistance");
    	while (enu.hasMoreElements()) {
    		String[] arr = (String[]) enu.nextElement();
    		NSMutableDictionary dict = new NSMutableDictionary(arr,"array");
    		while (titles.count() < arr.length) {
    			titles.addObject(new NSMutableDictionary());
    		}
    		for (int i = 0; i < arr.length; i++) {
    			if(arr[i].length() == 0)
    				continue;
    			Enumeration aEnu = assist.objectEnumerator();
    			while (aEnu.hasMoreElements()) {
    				NSDictionary rx = (NSDictionary) aEnu.nextElement();
    				Pattern pattern = (Pattern)rx.valueForKey("pattern");
    				if(pattern == null) {
    					pattern = Pattern.compile((String)rx.valueForKey("regex"),
    							Pattern.CASE_INSENSITIVE + Pattern.UNICODE_CASE);
    					rx.takeValueForKey(pattern, "pattern");
    				}
    				if(pattern.matcher(arr[i]).matches()) {
    					String field = (String)rx.valueForKey("field");
    					//							if(dict.valueForKey(field) != null)
    					//								continue;
    					if(dict.valueForKey("sex") == null)
    						dict.takeValueForKey(rx.valueForKey("sex"), "sex");
    					dict.takeValueForKey(arr[i], field);
    					NSMutableDictionary ctd = (NSMutableDictionary)titles.objectAtIndex(i);
    					Counter cnt = (Counter)ctd.valueForKey(field);
    					if(cnt == null)
    						ctd.takeValueForKey(new Counter(1), field);
    					else
    						cnt.raise();
    					break;
    				} // apply from pattern
    			} // importAssistance enumeration
    		} // arr enumeration
    		list.addObject(dict);
    	} // list enumeration
    	enu = titles.objectEnumerator();
    	NSMutableDictionary bestTitles = new NSMutableDictionary();
    	for (int i = 0; i < titles.count(); i++) {  // choose best titles
    		NSMutableDictionary ctd = (NSMutableDictionary) titles.objectAtIndex(i);
    		enu = ctd.keyEnumerator();
    		int max = 0;
    		String best = "???";
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
    		NSMutableDictionary exist = (NSMutableDictionary)bestTitles.valueForKey(best);
    		if(exist != null)
    			exist.takeValueForKey("???", "best");
    		if(max > 0)
    			bestTitles.takeValueForKey(ctd, best);
    	} // titles enumeration
    	enu = list.objectEnumerator();
    	NSTimestampFormatter formatter = null;
    	while (enu.hasMoreElements()) {
    		NSMutableDictionary dict = (NSMutableDictionary) enu.nextElement();
    		String[] arr = (String[])dict.valueForKey("array");
    		int matches = 0;
    		for (int i = 0; i < arr.length; i++) {
    			NSMutableDictionary ctd = (NSMutableDictionary)titles.objectAtIndex(i);
    			String field = (String)ctd.valueForKey("best");
    			if(field == null || field.equals("???"))
    				continue;
    			boolean ok = arr[i].equals(dict.valueForKey(field));
    			if(!ok) { // test with patterns
    				dict.takeValueForKey(arr[i], field);
    				if(arr[i].length() == 0)
    					continue;
    				Enumeration aEnu = assist.objectEnumerator();
    				while (aEnu.hasMoreElements()) {
    					NSDictionary rx = (NSDictionary) aEnu.nextElement();
    					if(!field.equals(rx.valueForKey("field")))
    						continue;
    					Pattern pattern = (Pattern)rx.valueForKey("pattern");
    					if(pattern != null && pattern.matcher(arr[i]).matches()) {
    						ok = true;
    						break;
    					}
    				}
    			} // test with patterns
    			if(ok) {
    				matches++;
    				if("date".equals(field)) { // parceDate
    					if(formatter == null) { //choose formatter 
    						NSArray variants = (NSArray)session().valueForKeyPath(
    	    					"strings.RujelVseLists_VseStrings.dateFormats");
    						Enumeration fenu = variants.objectEnumerator();
    						while (fenu.hasMoreElements()) {
								String pattern = (String) fenu.nextElement();
								try {
									formatter = new NSTimestampFormatter(pattern);
									NSTimestamp date = (NSTimestamp)formatter.parseObject(arr[i]);
									if(date == null) {
										formatter = null;
									} else {
										dict.takeValueForKey(date, "birthDate");
										break;
									}
								} catch (Exception e) {
									formatter = null;
								}
							}
    					} else {
							try {
	    						dict.takeValueForKey(formatter.parseObject(arr[i]), "birthDate");
							} catch (Exception e) {
								;
							}
    					}
    				}
    			}
    		}
    		dict.takeValueForKey(new Integer(matches), "matches");
    		if(matches < 2)
    			dict.takeValueForKey(Boolean.TRUE, "skip");

    	} // list enumeration
    	if(titles == null || titles.count() == 0) {
    		readFine = Boolean.FALSE;
    	} else {
    		enu = titles.objectEnumerator();
    		int needed = 0;
    		while (enu.hasMoreElements()) {
				NSMutableDictionary tDict = (NSMutableDictionary) enu.nextElement();
				String field = (String)tDict.valueForKey("field");
				if(field == null)
					continue;
				if(field.equals("lastName") || field.equals("firstName"))
					needed++;
			}
    		readFine = Boolean.valueOf(needed >= 2);
    	}
    	return list;
    }
    
    public void compareWithGroup() {
    	NSArray grList = targetGroup.lists();
    	Enumeration enu = list.objectEnumerator();
    }
    
    private boolean findEntry(NSMutableDictionary dict, NSArray lists) {
    	String last = (String)dict.valueForKey("lastName");
    	String first = (String)dict.valueForKey("firstName");
    	Enumeration enu = lists.objectEnumerator();
    	while (enu.hasMoreElements()) {
    		VseList vseList = (VseList) enu.nextElement();
    		Person person = vseList.student().person();
			if(compareValue(person, "lastName", last) &&
					compareValue(person, "firstName", first)) {
				if(!(compareValue(person, "secondName", dict.valueForKey("secondName")) &&
						compareValue(person, "birthDate", dict.valueForKey("birthDate"))))
					dict.takeValueForKey(Boolean.TRUE, "update");
				dict.takeValueForKey(vseList, "existing");
				return true;
			}
		}
    	
    	return false;
    }
    
    private boolean compareValue (NSKeyValueCodingAdditions dict,String key, Object value) {
    	Object check = dict.valueForKeyPath(key);
    	if(value == null || value.equals(""))
    		return (check == null || check.equals(""));
    	return value.equals(check);
    }
    
    public Object value() {
    	if(item instanceof NSMutableDictionary) {
    		Object[] array = (Object[])((NSMutableDictionary)item).valueForKey("array");
    		if(array == null)
    			return null;
    		if(index < 0 || index >= array.length)
    			return null;
    		return array[index];
    	}
    	return null;
    }
}