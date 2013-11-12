package net.rujel.stats;

import java.lang.reflect.Method;
import java.util.Enumeration;

import net.rujel.interfaces.EduCourse;
import net.rujel.reusables.DisplayAny;
import net.rujel.reusables.Various;
import net.rujel.reusables.WOLogLevel;

import com.webobjects.appserver.*;
import com.webobjects.eoaccess.EOUtilities;
import com.webobjects.eocontrol.EOEditingContext;
import com.webobjects.eocontrol.EOEnterpriseObject;
import com.webobjects.foundation.*;

// Generated by the WOLips Templateengine Plug-in at Jun 1, 2009 1:13:34 PM
public class StatsPlugin extends com.webobjects.appserver.WOComponent {
	
	public EduCourse course;
	public NSMutableArray rows;
	public Object item;
	public int cols =1;
	public Integer index;
	
    public StatsPlugin(WOContext context) {
        super(context);
    }
    
//	public void appendToResponse(WOResponse aResponse, WOContext aContext) {
    public void setCourse(EduCourse aCourse) {
    	course = aCourse;
    	item = null;
    	refresh();
    }
    
    public void refresh() {
		NSKeyValueCodingAdditions readAccess = (NSKeyValueCodingAdditions)
			session().valueForKey("readAccess");
		session().setObjectForKey(course, "statCourseReport");
		NSArray reports = (NSArray)session().valueForKeyPath("modules.statCourseReport");
		session().removeObjectForKey("statCourseReport");
		if(reports == null || reports.count() == 0)
			return;
		EOEditingContext ec = course.editingContext();
		Enumeration enu = reports.objectEnumerator();
		rows = new NSMutableArray();
		NSArray currKeys = null;
		cols = 0;
		Object currDesc = null;
		NSArray formulas = null;
		StringBuilder buf = new StringBuilder();
		while (enu.hasMoreElements()) {
			NSDictionary cfg = (NSDictionary) enu.nextElement();
			String entName = (String)cfg.valueForKey("entName");
			if(Various.boolForObject(readAccess.valueForKeyPath("_read." + entName)))
				continue;
			String statField = (String)cfg.valueForKey("statField");
			EOEnterpriseObject param1 = (EOEnterpriseObject)cfg.valueForKey("param1");
			EOEnterpriseObject param2 =(EOEnterpriseObject) cfg.valueForKey("param2");
			if(param2 == null && param1 != null) {
				param2 = course;
			} else if (param2 != null) {
				param2 = EOUtilities.localInstanceOfObject(ec, param2);
			}
			if(param1 == null) {
				param1 = course;
			} else {
				param1 = EOUtilities.localInstanceOfObject(ec, param1);
			}
			boolean create = (Boolean)readAccess.valueForKeyPath("create.Stats");
			NSDictionary dict = null;
			Grouping grouping = Description.getGrouping(entName, 
					statField, param1, param2,create);
			Object desc = null;
			if(grouping == valueForKeyPath("item.grouping")
					|| ec.globalIDForObject(grouping).isTemporary()) {
				Method ifEmpty = (Method)cfg.valueForKey("ifEmpty");
				if(ifEmpty != null)
					dict = StatsModule._execIfEmpty(ifEmpty, grouping, create, 
							param1, param2, ec, entName, statField);
				desc = Description.getDescription(entName, statField, 
						entForParam(param1), entForParam(param2), ec, create);
				if(desc == null) {
					desc = entName + "_" + statField;
				} else if (((Description)desc).description() == null) {
					NSKeyValueCoding.Utility.takeValueForKey(desc, cfg.valueForKey(
							Description.DESCRIPTION_KEY), Description.DESCRIPTION_KEY);
				}
				if(ec.hasChanges()) {
					try {
						ec.saveChanges();
					} catch (Exception e) {
						StatsModule.logger.log(WOLogLevel.WARNING,
								"Error autocreating stats.Description",
								new Object[] {session(),cfg,e});
						ec.revert();
					}
				}					
			} else {
				dict = grouping.dict();
				desc = grouping.description();
			}
			if(!desc.equals(currDesc)) {
				if(rows.count() > 0)
					rows.addObject(new NSDictionary(Boolean.TRUE,"noRecalc"));
				currDesc = desc;
				NSArray keys = (NSArray)cfg.valueForKey("keys");
				if(keys == null && (desc instanceof Description))
					keys = (NSArray)NSKeyValueCodingAdditions.Utility.
										valueForKeyPath(desc, "borderSet.sortedTitles");
				if(keys == null)
					keys = new NSArray("");
				if(!keys.contains(""))
					keys = keys.arrayByAddingObject("");
				currKeys = keys;
				
				NSMutableDictionary rowDict = new NSMutableDictionary(Boolean.TRUE,"titleRow");
				if(parent() == null)
					rowDict.takeValueForKey("grey","styleClass");
				//rowDict.takeValueForKey("font-weight:bold;", "style");
				buf.append("<th style=\"white-space:nowrap;border-left-style:none;text-align:left;\">");
				if(desc instanceof Description) {
					buf.append(((Description)desc).description());
				} else {
					Object title = cfg.valueForKey(Description.DESCRIPTION_KEY);
					buf.append((title!=null)?title:entName);
				}
				buf.append("</th>");
				rowDict.takeValueForKey(buf.toString(),"title");
				buf.delete(0, buf.length());
				buf.append("<th>");
				buf.append(application().valueForKeyPath(
						"strings.RujelStats_Stats.total"));
				buf.append("</th>");
				rowDict.takeValueForKey(buf.toString(),"total");

				Object tmp = cfg.valueForKey("addCalculations");
				if(Various.boolForObject(tmp)) {
					formulas = Calculations.allFormulas();
				} else {
					formulas = null;
				}
				tmp = cfg.valueForKey("formula");
				if(tmp != null) {
					if(formulas == null) formulas = new NSArray(tmp);
					else formulas = formulas.arrayByAddingObject(tmp);
				}
				tmp = cfg.valueForKey("formulas");
				if(tmp != null) {
					if(formulas == null) formulas = (NSArray)tmp;
					else formulas = formulas.arrayByAddingObjectsFromArray((NSArray)tmp);
				}

				if(formulas != null)
					currKeys = currKeys.arrayByAddingObjectsFromArray(formulas);
				if(currKeys.count() > cols)
					cols = currKeys.count() +2;
				rowDict.takeValueForKey(currKeys.objects(),"values");
				rowDict.takeValueForKey(Boolean.TRUE, "isTitle");
				rowDict.takeValueForKey(readAccess.valueForKeyPath("_edit.Stats"),"noRecalc");
				rows.addObject(rowDict);
			} // titleRow
			NSMutableDictionary rowDict = new NSMutableDictionary();
			if(parent() == null)
				rowDict.takeValueForKey("gerade","styleClass");
			buf.delete(0, buf.length());
			buf.append("<td style = \"white-space:nowrap;border-left-style:none;text-align:left;font-weight:bold;\">" );
			buf.append(cfg.valueForKey("title"));
			buf.append("</td>");
			rowDict.takeValueForKey(buf.toString(), "title");
			buf.delete(0, buf.length());
			buf.append("<td>");
			if(grouping != null) {
				buf.append(grouping.total());
			}
			buf.append("</td>");
			rowDict.takeValueForKey(buf.toString(), "total");
			buf.delete(0, buf.length());
			Object[] row = new Object[currKeys.count()];
			Enumeration kEnu = dict.keyEnumerator();
			while (kEnu.hasMoreElements()) {
				Object key = kEnu.nextElement();
				if(key.equals("keys") && (dict.objectForKey(key) instanceof NSArray))
					continue;
				int idx = currKeys.indexOf(key);
				if(idx >= 0) {
//					rowDict.setObjectForKey(dict.objectForKey(key), key);
					row[idx] = dict.objectForKey(key);
					continue;
				}
				if(key.equals("total")) {
					Number total = (Number)dict.objectForKey("total");
					if(grouping == null) {
						buf.append("<td>").append(total).append("</td>");
						rowDict.takeValueForKey(buf.toString(), "total");
						buf.delete(0, buf.length());
						continue;
					} else if(total.equals(grouping.total())) {
						continue;
					}
				}
				StringBuffer others = (StringBuffer)rowDict.objectForKey("others");
					//row[keys.count() + 2];
				if(others == null) {
					others = new StringBuffer();
//					row[keys.count() + 1] = others;
					rowDict.setObjectForKey(others, "others");
					if(cols < row.length + 3)
						cols = row.length + 3;
				} else {
					others.append(" ; ");
				}
				others.append('\'').append(key).append('\'');
				others.append(':').append(dict.objectForKey(key));
			} // process keys
			if(formulas != null && formulas.count() > 0) {
				int idx = row.length - formulas.count();
				kEnu = formulas.objectEnumerator();
				NSNumberFormatter format = new NSNumberFormatter();
				while (kEnu.hasMoreElements()) {
					NSDictionary fla = (NSDictionary) kEnu.nextElement();
					Object val = DisplayAny.ValueReader.evaluateValue(
							fla.valueForKey("value"), dict, this);
					if(val != null) {
						String pattern = (String)fla.valueForKeyPath(
						"presenterBindings.numberformat");
						if(pattern != null)
							format.setPattern(pattern);
						else
							format.setPattern(NSNumberFormatter.DefaultPattern);
						row[idx] = format.format(val);
					}
					idx++;
				}
			}
			rowDict.takeValueForKey(row, "values");
//			rowDict.takeValueForKey(new Integer(row.length), "valuesCount");
			if(parent() == null && Various.boolForObject(
					readAccess.valueForKeyPath("edit.Stats"))) {
//				rowDict.takeValueForKey(cfg.valueForKey("ifEmpty"), "ifEmpty");
				rowDict.takeValueForKey(grouping, "grouping");
//				rowDict.takeValueForKey(param1, "param1");
//				rowDict.takeValueForKey(param2, "param2");
//				rowDict.takeValueForKey(entName, "entName");
//				rowDict.takeValueForKey(statField, "statField");
			} else {
				rowDict.takeValueForKey(Boolean.TRUE,"noRecalc");
			}
			rows.addObject(rowDict);
		} // process reports
	}
        
    public WOActionResults recalculate() {
    	refresh();
/*    	if(!(item instanceof NSDictionary))
    		return null;
    	
    	Method ifEmpty = (Method)((NSDictionary)item).valueForKey("ifEmpty");
    	Grouping grouping = (Grouping)((NSDictionary)item).valueForKey("grouping");
    	EOEditingContext ec = grouping.editingContext();
    	EOEnterpriseObject param1 = (EOEnterpriseObject)((NSDictionary)item).valueForKey("param1");
    	EOEnterpriseObject param2 = (EOEnterpriseObject)((NSDictionary)item).valueForKey("param2");
    	String entName = (String)((NSDictionary)item).valueForKey("entName");
    	String statField = (String)((NSDictionary)item).valueForKey("statField");
    	NSDictionary dict = StatsModule._execIfEmpty(ifEmpty, grouping, false, 
				param1, param2, ec, entName, statField);
    	if(dict != null) {
    		grouping.setDict(dict);
    		try {
				ec.saveChanges();
				StatsModule.logger.log(WOLogLevel.FINE,"Stats recalculated",
						new Object[] {session(),grouping});
			} catch (Exception e) {
				ec.revert();
				StatsModule.logger.log(WOLogLevel.WARNING,"Errror recalculating Stats",
						new Object[] {session(),grouping,e});
			}
    	}
*/    	return this;
    }
    
	private static String entForParam(Object param) {
		if(param == null)
			return EduCourse.entityName;
		if(param instanceof EOEnterpriseObject)
			return ((EOEnterpriseObject)param).entityName();
		return param.toString();
	}
	
    public boolean titleRow() {
    	return Various.boolForObject(((NSDictionary)item).valueForKey("titleRow"));
    }
    
    public int count() {
//    	if(titleRow()) {
//    		return ((NSArray)item).count();
//    	} else 
    	if(item instanceof NSDictionary) {
			Object[] row = (Object[])((NSDictionary)item).valueForKey("values");
			if(row != null)
				return row.length;
		}
    	return 0;
    }
    
    public String lastCell() {
    	int num = count();
    	if(item != NSDictionary.EmptyDictionary)
    		num += 2;
    	boolean titleRow = titleRow();
    	if(num >= cols)
    		return null;
    	StringBuilder buf = new StringBuilder(12);
    		buf.append((titleRow)?"<th":"<td");
    	if(cols > num +1)
    		buf.append(" align = \"left\" colspan = \"").append(cols - num).append('"');
    	if(item == NSDictionary.EmptyDictionary)
    		buf.append(" style = \"height:1ex;border-left-style:none;\"");
    	buf.append('>');
    	if(titleRow) {
    		buf.append(application().valueForKeyPath("strings.RujelStats_Stats.others"));
    	} else {
    		Object val = ((NSDictionary)item).valueForKey("others");
    		if(val != null)
    			buf.append(val);
    	}
    	buf.append((titleRow)?"</th>":"</td>");
    	return buf.toString();
    }
	
	public String value() {
		if(item == null || index == null)
			return null;
		int idx = index.intValue();
		if(item instanceof NSDictionary) {
			Object[] row = (Object[])((NSDictionary)item).valueForKey("values");
			if(idx >= row.length)
				return null;
			if(row[idx] == null)
				return "<td></td>";
			if(!titleRow())
				return "<td>" + row[idx].toString() + "</td>";
			String val = null;
			String title = null;
			boolean quote = false;
			if(row[idx] instanceof NSDictionary) {
				NSDictionary dict = (NSDictionary)row[idx];
				title = (String)dict.valueForKey("hover");
				val = (String)dict.valueForKey("short");
				if(val == null) {
					val = (String)dict.valueForKey("title");
				} else if (title == null) {
					title = (String)dict.valueForKey("title");
				}
				quote = (val.length() < 3);
			} else {
				val = row[idx].toString();
				quote = (val.length() < 3);
				val = WOMessage.stringByEscapingHTMLString(val.toString());
			}
			StringBuilder result = new StringBuilder("<th style = \"");
			if(quote) {
				result.append("min-width:1.6em;\"");
			} else {
				result.append("white-space:nowrap;\"");
			}
			if(val.equals("")) {
				title = (String)application().valueForKeyPath("strings.RujelStats_Stats.none");
				val = "&oslash;";
				quote = false;
			} else if(row[idx] instanceof NSDictionary) {
				quote = false;
			}
			if(title != null)
				result.append(" title = \"").append(title).append('"');
			result.append('>');
			if(quote) result.append('\'');
			result.append(val);
			if(quote) result.append('\'');
			result.append("</th>");
			return result.toString();
		}
		return null;
	}
	
	public Boolean notPopup() {
		return (parent() != null);
	}
}