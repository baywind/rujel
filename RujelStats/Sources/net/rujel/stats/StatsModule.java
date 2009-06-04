// DisplayAny.java

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
import java.util.Enumeration;
import java.util.logging.Logger;

import net.rujel.interfaces.EduCourse;
import net.rujel.reusables.Various;
import net.rujel.reusables.WOLogLevel;

import com.webobjects.appserver.WOApplication;
import com.webobjects.appserver.WOContext;
import com.webobjects.eoaccess.EOUtilities;
import com.webobjects.eocontrol.EOEditingContext;
import com.webobjects.eocontrol.EOEnterpriseObject;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSComparator;
import com.webobjects.foundation.NSDictionary;
import com.webobjects.foundation.NSKeyValueCodingAdditions;
import com.webobjects.foundation.NSMutableArray;
import com.webobjects.foundation.NSMutableDictionary;
import com.webobjects.foundation.NSComparator.ComparisonException;

public class StatsModule {
	
	protected static Logger logger = Logger.getLogger("rujel.stats");

	public static Object init(Object obj, WOContext ctx) {
		if(obj == null || obj.equals("init")) {
			;
		} else if("coursesReport".equals(obj)) {
			return coursesReport(ctx);
		} else if("journalPlugins".equals(obj)) {
			return journalPlugins(ctx);
		}
		return null;
	}
	
	public static Object coursesReport(WOContext ctx) {
		NSKeyValueCodingAdditions readAccess = (NSKeyValueCodingAdditions)ctx.
							session().valueForKey("readAccess");
		if(Various.boolForObject(readAccess.valueForKeyPath("_read.Stats")))
			return null;
		NSArray reports = (NSArray)ctx.session().valueForKeyPath("modules.statCourseReport");
		if(reports == null || reports.count() == 0)
			return null;
		Enumeration enu = reports.objectEnumerator();
		NSMutableArray result = new NSMutableArray();
		int rNum = 0;
		boolean create = Various.boolForObject(readAccess.valueForKeyPath("create.Stats"));
		while (enu.hasMoreElements()) {
			rNum++;
			NSDictionary cfg = (NSDictionary) enu.nextElement();
			String entName = (String)cfg.valueForKey("entName");
			if(Various.boolForObject(readAccess.valueForKeyPath("_read." + entName)))
				continue;
			String statField = (String)cfg.valueForKey("statField");
			Object param1 = cfg.valueForKey("param1");
			Object param2 = cfg.valueForKey("param2");
			if(param2 == null && param1 != null) {
				param2 = ".";
			}
			if(param1 == null) {
				param1 = ".";
			}
			NSMutableDictionary reportDict = new NSMutableDictionary("stats" + rNum,"id");
			String title = (String)cfg.valueForKey("title");
			NSArray keys = (NSArray)cfg.valueForKey("keys");
//			if(title == null || keys == null) {
			EOEditingContext ec = (EOEditingContext)ctx.page().valueForKey("ec");
			Description desc = Description.getDescription(entName, statField, 
					entForParam(param1), entForParam(param2), ec, create);
			String description = (String)cfg.valueForKey(Description.DESCRIPTION_KEY);
			if(description == null)
				description = entName;
			if(desc != null) {
				if(desc.description() != null) {
					description = desc.description();
				} else {
					desc.setDescription(description);
					try {
						ec.saveChanges();
					} catch (Exception e) {
						ec.revert();
					}
				}
				if(keys == null)
					keys = (NSArray)desc.valueForKeyPath("borderSet.sortedTitles");
			}
			if(title == null)
				title = description;
			else 
				title = description + " - " + title;
//			}
			reportDict.takeValueForKey(title, "title");
			reportDict.takeValueForKey(Integer.toString(50 +rNum),"sort");
//			reportDict.takeValueForKey(new NSArray(entName),"checkAccess");

			NSMutableArray subParams = new NSMutableArray();

			NSMutableDictionary keyDict = new NSMutableDictionary(".total","value");
			keyDict.takeValueForKey(WOApplication.application().valueForKeyPath(
				"strings.RujelStats_Stats.total"),"title");
			keyDict.takeValueForKey(new Integer(-1), "sort");
//			keyDict.takeValueForKey("width:1.6em;","titleStyle");
			keyDict.takeValueForKey(Boolean.TRUE, "active");
			subParams.addObject(keyDict);
			int i = 0;
			if(keys!= null && keys.count() > 0) {
				if(!keys.containsObject(""))
					keys = keys.arrayByAddingObject("");
				Enumeration kenu = keys.objectEnumerator();
				while (kenu.hasMoreElements()) {
					String key = (String) kenu.nextElement();
					keyDict = new NSMutableDictionary(".key" + i,"value");
					if(key.equals("")) {
						keyDict.takeValueForKey("&oslash;", "short");
						keyDict.takeValueForKey(WOApplication.application().valueForKeyPath(
							"strings.RujelStats_Stats.none"),"title");
					} else {
						keyDict.takeValueForKey('\'' + key + '\'', "title");
						keyDict.takeValueForKey(Boolean.TRUE, "active");
					}
					keyDict.takeValueForKey("width:1.6em;white-space:nowrap;","titleStyle");
					keyDict.takeValueForKey(new Integer(i), "sort");
					subParams.addObject(keyDict);
					i++;
				}
			} // add preset keys to subParams
			keyDict = new NSMutableDictionary(".others","value");
			keyDict.takeValueForKey(WOApplication.application().valueForKeyPath(
				"strings.RujelStats_Stats.others"),"title");
			keyDict.takeValueForKey(new Integer(i), "sort");
//			keyDict.takeValueForKey("width:1.6em;","titleStyle");
			subParams.addObject(keyDict);
			Object tmp = cfg.valueForKey("addCalculations");
			if(Various.boolForObject(tmp)) {
				subParams.addObjectsFromArray(Calculations.allFormulas());
			}
			tmp = cfg.valueForKey("formula");
			if(tmp != null) {
				subParams.addObject(tmp);
			}
			tmp = cfg.valueForKey("formulas");
			if(tmp != null) {
				subParams.addObjectsFromArray((NSArray)tmp);
			}
			reportDict.setObjectForKey(subParams, "subParams");
			
			try {
				Object[] params = new Object[] {entName, statField, param1, param2, keys,
						cfg.valueForKey("ifEmpty"),create};
				NSMutableDictionary valueDict = new NSMutableDictionary("reportDict","methodName");
				Method method = StatsModule.class.getMethod("reportDict", String.class, String.class, 
EOEnterpriseObject.class, EOEnterpriseObject.class,NSArray.class,Method.class,Boolean.TYPE);
				valueDict.setObjectForKey(method, "parsedMethod");
				valueDict.setObjectForKey(new NSArray(params), "paramValues");
				reportDict.setObjectForKey(valueDict, "value");
			} catch (Exception e) {
				e.printStackTrace();
			}
			result.addObject(reportDict);
		}
		return result;
	}

	private static String entForParam(Object param) {
		if(param == null)
			return null;
		if(param instanceof EOEnterpriseObject)
			return ((EOEnterpriseObject)param).entityName();
		if(param.equals("."))
			return EduCourse.entityName;
		return param.toString();
	}
	
	public static NSDictionary reportDict(String entName, String statField, 
			EOEnterpriseObject param1, EOEnterpriseObject param2, NSArray keys, 
			Method ifEmpty, boolean create) {
		EOEditingContext ec = null;
		if(param1 != null) {
			ec = param1.editingContext();
			if(param2 != null)
				param2 = EOUtilities.localInstanceOfObject(ec, param2);
		} else if(param2 != null) {
			ec = param2.editingContext();
		} else {
			ec = new EOEditingContext();
		}
		Grouping grouping = Description.getGrouping(entName, statField, param1, param2, create);
		NSDictionary dict = null;
		if(ifEmpty != null && 
				(grouping == null || ec.globalIDForObject(grouping).isTemporary())) {
/*			Class[] paramClasses = ifEmpty.getParameterTypes();
			Object[] params = new Object[paramClasses.length];
			boolean[] unused = new boolean[] {true,true,true};
			for (int i = 0; i < paramClasses.length; i++) {
				if(paramClasses[i].isInstance(param1) && unused[1]) {
					params[i] = param1;
					unused[1] = false;
				} else if(paramClasses[i].isInstance(param2) && unused[2]) {
					params[i] = param2;
					unused[2] = false;
				} else if(paramClasses[i].isInstance(ec) && unused[0]) {
					params[i] = ec;
					unused[0] = false;
				}
			}
			try {
				Object result = ifEmpty.invoke(null, params);
				if(result instanceof NSDictionary) {
					dict = (NSDictionary) result;
					if(grouping != null)
						grouping.setDict(dict);
				}
				if(result instanceof NSArray) {
					if(grouping != null) {
						dict = grouping.description().calculate((NSArray)result);
						grouping.setDict(dict);
					} else {
						String ent1 = (param1 == null)?null:param1.entityName();
						String ent2 = (param2 == null)?null:param2.entityName();
						Description desc = Description.getDescription
									(entName, statField, ent1, ent2, ec, create);
						if(desc == null)
							dict = Description.calculate((NSArray)result, statField, null);
						else
							dict = desc.calculate((NSArray)result);
					}
				}
			} catch (Exception e) {
				logger.log(WOLogLevel.WARNING,"Error getting stat values for " + entName,
						new Object[] {e,param1,param2,ifEmpty});
			}
			if(ec.hasChanges()) {
				if(create) {
					try {
						ec.saveChanges();
					} catch (Exception e) {
						logger.log(WOLogLevel.WARNING,"Error savig calculated stats",
								new Object[] {e,param1,param2});
					}
				} else {
					ec.revert();
				}
			} // executing ifEmpty
*/
			dict = _execIfEmpty(ifEmpty, grouping, create, param1, param2, ec, entName, statField);	
		} else if (grouping != null) {
			dict = grouping.dict();
		}
		if(dict == null)
			return null;
		boolean checkTotal = (grouping == null);
		Number total = (Number)dict.valueForKey("total");
		NSArray allKeys = null;
		if(grouping != null) {
			allKeys = grouping.keys();
			total = grouping.total();
		} else {
			if(dict.valueForKey("keys") instanceof NSArray) {
				allKeys = (NSArray)dict.valueForKey("keys");
				checkTotal = false;
			} else {
				allKeys = dict.allKeys();
				try {
					allKeys = allKeys.sortedArrayUsingComparator
							(NSComparator.AscendingCaseInsensitiveStringComparator);
				} catch (ComparisonException e) {
					e.printStackTrace();
				}
			}
		}
		if(allKeys == null) {
			if(grouping != null && grouping.total() != null)
				return new NSDictionary(grouping.total(),"total");
			else
				return null;
		}
		NSMutableArray others = allKeys.mutableClone();

		int checkSum = 0;
		NSMutableDictionary result = new NSMutableDictionary(dict,"dict");
		if (keys != null && keys.count() > 0) {
			others.removeObjectsInArray(keys);
			for (int i = 0; i < keys.count(); i++) {
				Number value = (Number) dict.objectForKey(keys.objectAtIndex(i));
				if(value != null)
					checkSum += value.intValue();
				result.takeValueForKey(value, "key" + i);
			}
		}
		if(checkTotal) {
			others.removeObject("total");
		}
		if(others.count() > 0) {
			StringBuffer buf = new StringBuffer();
			Enumeration enu = others.objectEnumerator();
			while (enu.hasMoreElements()) {
				String key = (String) enu.nextElement();
				buf.append('\'').append(key).append('\'').append(':');
				Number value = (Number)dict.objectForKey(key);
				checkSum += value.intValue();
				buf.append(value);
				if(enu.hasMoreElements())
					buf.append(" ; ");
			}
			if(checkTotal && total != null && total.intValue() < checkSum) {
				buf.append(" ; 'total':").append(total);
				checkSum += total.intValue();
				total = null;
			}
			result.takeValueForKey(buf.toString(), "others");
		} else if(checkTotal && total != null && total.intValue() < checkSum) {
			result.takeValueForKey(" ; 'total':" + total, "others");
			checkSum += total.intValue();
			total = null;
		}
		if(total == null)
			total = new Integer(checkSum);
		result.takeValueForKey(total, "total");
		return result;
	}
	
	public static Object journalPlugins(WOContext ctx) {
		if(Various.boolForObject(ctx.session().valueForKeyPath("readAccess._read.Stats")))
			return null;
		return WOApplication.application().valueForKeyPath("strings.RujelStats_Stats.dashboard");
	}
	
	public static NSDictionary _execIfEmpty(Method ifEmpty,Grouping grouping, boolean create,
			EOEnterpriseObject param1, EOEnterpriseObject param2, 
			EOEditingContext ec, String entName, String statField) {
		Class[] paramClasses = ifEmpty.getParameterTypes();
		Object[] params = new Object[paramClasses.length];
		boolean[] unused = new boolean[] {true,true,true};
		for (int i = 0; i < paramClasses.length; i++) {
			if(paramClasses[i].isInstance(param1) && unused[1]) {
				params[i] = param1;
				unused[1] = false;
			} else if(paramClasses[i].isInstance(param2) && unused[2]) {
				params[i] = param2;
				unused[2] = false;
			} else if(paramClasses[i].isInstance(ec) && unused[0]) {
				params[i] = ec;
				unused[0] = false;
			}
		}
		NSDictionary dict = null;
		try {
			Object result = ifEmpty.invoke(null, params);
			if(result instanceof NSDictionary) {
				dict = (NSDictionary) result;
				if(grouping != null)
					grouping.setDict(dict);
			}
			if(result instanceof NSArray) {
				if(grouping != null) {
					dict = grouping.description().calculate((NSArray)result);
					grouping.setDict(dict);
				} else {
					String ent1 = (param1 == null)?null:param1.entityName();
					String ent2 = (param2 == null)?null:param2.entityName();
					Description desc = Description.getDescription
								(entName, statField, ent1, ent2, ec, create);
					if(desc == null)
						dict = Description.calculate((NSArray)result, statField, null);
					else
						dict = desc.calculate((NSArray)result);
				}
			}
		} catch (Exception e) {
			logger.log(WOLogLevel.WARNING,"Error getting stat values for " + entName,
					new Object[] {e,param1,param2,ifEmpty});
		}
		if(ec.hasChanges()) {
			if(create) {
				try {
					ec.saveChanges();
				} catch (Exception e) {
					logger.log(WOLogLevel.WARNING,"Error savig calculated stats",
							new Object[] {e,param1,param2});
				}
			} else {
				ec.revert();
			}
		}
		return dict;
	}
}
