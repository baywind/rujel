package net.rujel.eduresults;

import java.util.Enumeration;
import java.util.logging.Logger;

import net.rujel.base.MyUtility;
import net.rujel.base.ReadAccess;
import net.rujel.base.SettingsBase;
import net.rujel.eduplan.ListSettings;
import net.rujel.reusables.AdaptingComparator;
import net.rujel.reusables.ModulesInitialiser;
import net.rujel.reusables.NamedFlags;
import net.rujel.reusables.PlistReader;
import net.rujel.reusables.Various;
import net.rujel.reusables.WOLogLevel;

import com.webobjects.appserver.*;
import com.webobjects.eoaccess.EOUtilities;
import com.webobjects.eocontrol.EOAndQualifier;
import com.webobjects.eocontrol.EOEditingContext;
import com.webobjects.eocontrol.EOEnterpriseObject;
import com.webobjects.eocontrol.EOFetchSpecification;
import com.webobjects.eocontrol.EOKeyValueQualifier;
import com.webobjects.eocontrol.EOOrQualifier;
import com.webobjects.eocontrol.EOQualifier;
import com.webobjects.eocontrol.EOSortOrdering;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSDictionary;
import com.webobjects.foundation.NSMutableArray;
import com.webobjects.foundation.NSComparator.ComparisonException;
import com.webobjects.foundation.NSMutableDictionary;

// Generated by the WOLips Templateengine Plug-in at Sep 3, 2009 12:37:59 PM
public class SetupItogs extends com.webobjects.appserver.WOComponent {
	protected static Logger logger = Logger.getLogger("rujel.itog");

 	public EOEditingContext ec;
	public String listName;
	public NSArray extraLists;
	public NSArray allTypes;
	public NSMutableArray typesList;
//	public NSMutableArray activeTypes = new NSMutableArray();
	public Object item;
	public NSDictionary currType;
	public NSArray itogsList;
	public NSArray extensions;
	public Object extItem;
	public NSMutableArray allPresets;
	public NamedFlags access;
	public NamedFlags typeAccess;
//	public NSMutableDictionary currPreset;
	protected static NSArray sorter = new NSArray(
			EOSortOrdering.sortOrderingWithKey("type.sort",EOSortOrdering.CompareAscending));

	public SetupItogs(WOContext context) {
        super(context);
//		setEc((EOEditingContext)context.page().valueForKey("ec"));
//		context().page().takeValueForKey(this, "toReset");
    }

	public void appendToResponse(WOResponse aResponse, WOContext aContext) {
		if(Various.boolForObject(valueForBinding("shouldReset"))) {
			currType = null;
			itogsList = null;
//			activeTypes.removeAllObjects();
			setEc((EOEditingContext)context().page().valueForKey("ec"));
			setValueForBinding(Boolean.FALSE, "shouldReset");
		}
		super.appendToResponse(aResponse, aContext);
		extraLists = null;
	}
	
	public NSArray extensions() {
		if(currType == null || Various.boolForObject(currType.valueForKey("active")))
			return extensions;
		return null;
	}

	public void setEc(EOEditingContext newEc) {
		extensions = (NSArray)session().valueForKeyPath("modules.itogExtensions");
		ec = newEc;
		ec.lock();
		try {
			EOFetchSpecification fs = new EOFetchSpecification(ItogType.ENTITY_NAME,
					null, ModulesInitialiser.sorter);
			allTypes = ec.objectsWithFetchSpecification(fs);
			if(allTypes != null && allTypes.count() > 0)  {
				typesList = new NSMutableArray();
				Enumeration enu = allTypes.objectEnumerator();
				while (enu.hasMoreElements()) {
					typesList.addObject(new NSMutableDictionary(enu.nextElement(),"type"));
				}
			}
			SettingsBase base = SettingsBase.baseForKey(ItogMark.ENTITY_NAME, ec, true);
			setListName(base.textValue());
			if(listName == null) {
				setListName((String)application().valueForKeyPath(
					"strings.RujelEduPlan_EduPlan.SetupPeriods.defaultListName"));
				base.setTextValue(listName);
				ec.saveChanges();
				logger.log(WOLogLevel.SETTINGS_EDITING,
						"Created default ItogMark ListName setting: " + listName,
						new Object[] {session(), base});
			} else {
				EOQualifier[] quals = new EOQualifier[2];
				quals[0] = new EOKeyValueQualifier("eduYear", EOQualifier.QualifierOperatorEqual,
						session().valueForKey("eduYear"));
				quals[1] = new EOKeyValueQualifier("eduYear",
						EOQualifier.QualifierOperatorEqual, new Integer(0));
				quals[1] = new EOOrQualifier(new NSArray(quals));
				quals[0] = new EOKeyValueQualifier("listName",
						EOQualifier.QualifierOperatorNotEqual,listName);
				quals[0] = new EOAndQualifier(new NSArray(quals));
				fs = new EOFetchSpecification("ItogTypeList",quals[0],null);
				extraLists = ec.objectsWithFetchSpecification(fs);
			}
			access = ListSettings.listAccess(base, listName, "ItogTypeList", session());
			ReadAccess readAccess = (ReadAccess)session().valueForKey("readAccess");
			typeAccess = readAccess.cachedAccessForObject(ItogType.ENTITY_NAME, (Integer)null);
			updatePresets();
		} catch (Exception e) {
			logger.log(WOLogLevel.WARNING,
					"Error creating default ItogMark ListName setting",
					new Object[] {session(), e});
		} finally {
			ec.unlock();
		}
	}

	protected void updatePresets() {
		allPresets = ItogPreset.allPresets(ec);
		NSMutableDictionary dict = new NSMutableDictionary(Integer.valueOf(0), "presetGroup");
		dict.takeValueForKey(session().valueForKeyPath(
				"strings.RujelEduResults_EduResults.properties.ItogPreset.noPreset"),
				"fullName");
		allPresets.insertObjectAtIndex(dict, 0);
		if(Various.boolForObject(session().valueForKeyPath("readAccess.create.ItogPreset"))) {
			dict = (NSMutableDictionary)allPresets.lastObject();
			Integer presetGroup = (Integer)dict.valueForKey(ItogPreset.PRESET_GROUP_KEY);
			presetGroup = Integer.valueOf(presetGroup.intValue() +1);
			dict = new NSMutableDictionary(presetGroup,ItogPreset.PRESET_GROUP_KEY);
			dict.takeValueForKey(session().valueForKeyPath(
					"strings.RujelEduResults_EduResults.properties.ItogPreset.newPreset"), 
					"fullName");
			allPresets.addObject(dict);
			presetGroup = Integer.valueOf(-presetGroup.intValue());
			dict = new NSMutableDictionary(presetGroup,ItogPreset.PRESET_GROUP_KEY);
			dict.takeValueForKey(session().valueForKeyPath(
			"strings.RujelEduResults_EduResults.properties.ItogPreset.newPercent"), 
			"fullName");
			dict.takeValueForKey(Boolean.TRUE, "isPercent");
			allPresets.addObject(dict);
		}
	}
	
	public void setListName(String name) {
		listName = name;
		if(listName != null) {
			Integer eduYear = (Integer)session().valueForKey("eduYear");
			NSArray used = ItogType.getTypeList(listName, eduYear, ec);
			Enumeration enu = typesList.objectEnumerator();
			boolean hasActive = false;
			while (enu.hasMoreElements()) {
				NSMutableDictionary dict = (NSMutableDictionary) enu.nextElement();
				ItogType type = (ItogType)dict.valueForKey("type");
				EOEnterpriseObject tl = null;
				if(used != null && used.count() > 0) {
					for (int i = 0; i < used.count(); i++) {
						EOEnterpriseObject tmp = (EOEnterpriseObject)used.objectAtIndex(i);
						if(tmp.valueForKey("itogType") == type) {
							tl = tmp;
							break;
						}
					}
				}
				hasActive = (hasActive || tl != null);
				setTypeListOnDict(dict, tl);
			}
			/*
			if(used == null || used.count() == 0) {
				activeTypes.removeAllObjects();
				itogsList = null;
			} else {
				activeTypes.setArray((NSArray)used.valueForKey("itogType")); 
				allItogs();
			}
			*/
			if(hasActive)
				allItogs();
			else
				itogsList = null;
		}
		currType = null;
	}

	protected void setTypeListOnDict(NSMutableDictionary dict,
			EOEnterpriseObject tl) {
		dict.takeValueForKey(tl, "ItogTypeList");
		dict.removeObjectForKey("preset");
		if(tl == null) {
			dict.takeValueForKey(Boolean.FALSE, "active");
			dict.removeObjectForKey(ItogPreset.PRESET_GROUP_KEY);
		} else {
			dict.takeValueForKey(Boolean.TRUE, "active");
			Integer preset = (Integer)tl.valueForKey(ItogPreset.PRESET_GROUP_KEY);
			if(preset.intValue() > 0) {
				NSArray presetGroup = ItogPreset.listPresetGroup(ec, preset, false);
				if(presetGroup == null) {
					preset = Integer.valueOf(0);
					tl.takeValueForKey(preset, ItogPreset.PRESET_GROUP_KEY);
				} else {
					ItogPreset p = (ItogPreset)presetGroup.objectAtIndex(0);
					dict.takeValueForKey(p.mark(), "preset");
				}
			}
			dict.takeValueForKey((preset.intValue() > 0)?preset:null, ItogPreset.PRESET_GROUP_KEY);
		}
	}
	
	
	
	protected void allItogs() {
//		EOSortOrdering.sortArrayUsingKeyOrderArray(activeTypes,ModulesInitialiser.sorter);
		NSMutableArray allItogs = new NSMutableArray();
		Enumeration enu = typesList.objectEnumerator();
		while (enu.hasMoreElements()) {
			NSDictionary dict = (NSDictionary) enu.nextElement();
			if(dict.valueForKey("ItogTypeList") == null)
				continue;
			ItogType type = (ItogType) dict.valueForKey("type");
			Integer eduYear = (Integer)session().valueForKey("eduYear");
			allItogs.addObjectsFromArray(type.itogsInYear(eduYear));
		}
		try {
			allItogs.sortUsingComparator(new AdaptingComparator(ItogContainer.class));
		} catch (ComparisonException e) {
			Logger.getLogger("rujel.itog").log(WOLogLevel.WARNING,"Error sorting itogs",
					new Object[] {session(),e});
		}
		itogsList = allItogs;
	}
	/*
	public boolean active() {
		return activeTypes.containsObject(item);
	}
	
	public void setActive(boolean set) {
		if(set) {
			if(!active())
				activeTypes.addObject(item);
		} else {
			activeTypes.removeObject(item);
		}
	}
	*/
	public WOActionResults saveList() {
		ec.lock();
		Integer eduYear = (Integer)session().valueForKey("eduYear");
		try {
			NSMutableArray toDelete = new NSMutableArray();
			NSMutableArray toAdd = new NSMutableArray();
			Enumeration enu = typesList.objectEnumerator();
			while (enu.hasMoreElements()) {
				NSMutableDictionary dict = (NSMutableDictionary)enu.nextElement();
				EOEnterpriseObject tl = (EOEnterpriseObject) dict.valueForKey("ItogTypeList");
				if(tl != null) {
					Integer year = (Integer)tl.valueForKey("eduYear");
					if(year == null || year.intValue() == 0) { // add year tags
						tl.takeValueForKey(new Integer(eduYear -1), "eduYear");
						tl = null;
					}
				}
				if(Various.boolForObject(dict.valueForKey("active"))) {
					if(tl != null)
						continue;
					toAdd.addObject(dict);
				} else {
					if(tl == null)
						continue;
					setTypeListOnDict(dict, null);
					toDelete.addObject(tl);
				}
			}
			if(toAdd.count() > 0) {
				enu = toAdd.objectEnumerator();
				while (enu.hasMoreElements()) {
					NSMutableDictionary dict = (NSMutableDictionary)enu.nextElement();
					ItogType it = (ItogType) dict.valueForKey("type");
					EOEnterpriseObject tl = (EOEnterpriseObject)toDelete.removeLastObject();
					if(tl == null) {
						tl = EOUtilities.createAndInsertInstance(ec, "ItogTypeList");
						tl.takeValueForKey(listName, "listName");
						tl.takeValueForKey(eduYear, "eduYear");
						tl.takeValueForKey(new Integer(1), "presetGroup");
					}
					tl.addObjectToBothSidesOfRelationshipWithKey(it, "itogType");
					setTypeListOnDict(dict, tl);
				}
			}
			if(toDelete.count() > 0) {
				enu = toDelete.objectEnumerator();
				while (enu.hasMoreElements()) {
					EOEnterpriseObject tl = (EOEnterpriseObject) enu.nextElement();
					ec.deleteObject(tl);
				}
			}
			ec.saveChanges();
			EOSortOrdering.sortArrayUsingKeyOrderArray(typesList,sorter);
			if(currType == null)
				allItogs();
		} catch (Exception e) {
			logger.log(WOLogLevel.WARNING,"Error saving changes in list " + listName,e);
			session().takeValueForKey(e.getMessage(), "message");
			ec.revert();
			setListName(listName);
		} finally {
			ec.unlock();
		}
		return null;
	}
	
	public WOActionResults selectType() {
//		ItogType type = (ItogType)valueForKeyPath("item.type");
		if(currType != item) {
			setCurrType(item);
		} else {
			currType = null;
			allItogs();
		}
		return null;
	}
	
	public void setCurrType(Object value) {
		ItogType type;
		if(value instanceof NSDictionary) {
			currType = (NSDictionary)value;
			type = (ItogType)currType.valueForKey("type");
		} else if(value instanceof ItogType) {
			type = (ItogType)value;
			currType = null;
		} else {
			type = null;
			currType = null;
		}
		ec.lock();
		try {
			if(type == null) {
				EOFetchSpecification fs = new EOFetchSpecification(ItogType.ENTITY_NAME,
						null, ModulesInitialiser.sorter);
				allTypes = ec.objectsWithFetchSpecification(fs);
				setListName(listName);
			} else if(allTypes.containsObject(type)) {
				EOQualifier[] quals = new EOQualifier[2];
				quals[0] = new EOKeyValueQualifier(ItogContainer.ITOG_TYPE_KEY,
						EOQualifier.QualifierOperatorEqual,type);
				quals[1] = new EOKeyValueQualifier(ItogContainer.EDU_YEAR_KEY,
						EOQualifier.QualifierOperatorEqual,
						session().valueForKey("eduYear"));
				quals[0] = new EOAndQualifier(new NSArray(quals));
				EOFetchSpecification fs = new EOFetchSpecification(
						ItogContainer.ENTITY_NAME, quals[0], MyUtility.numSorter);
				itogsList = ec.objectsWithFetchSpecification(fs);
				if(currType == null) {
					for (int i = 0; i < typesList.count(); i++) {
						NSDictionary tmp = (NSDictionary)typesList.objectAtIndex(i);
						if(tmp.valueForKey("type") == type) {
							currType = tmp;
							break;
						}
					}
				}
			} else {
				allTypes = allTypes.arrayByAddingObject(type);
				NSMutableDictionary dict = new NSMutableDictionary(type,"type");
				itogsList = NSArray.EmptyArray;
				EOEnterpriseObject tl = EOUtilities.createAndInsertInstance(ec,
						"ItogTypeList");
				tl.takeValueForKey(listName, "listName");
				tl.takeValueForKey(session().valueForKey("eduYear"), "eduYear");
				tl.addObjectToBothSidesOfRelationshipWithKey(type, "itogType");
				tl.takeValueForKey(new Integer(1), "presetGroup");
				setTypeListOnDict(dict, tl);
				typesList.addObject(dict);
//				activeTypes.addObject(type);
				ec.saveChanges();
			}
		} catch (Exception e) {
			logger.log(WOLogLevel.WARNING, "Error adding ItogType",
					new Object[] {session(),type,e});
		} finally {
			ec.unlock();
		}
//		itogName = currType.name();
//		itogTitle = currType.title();
//		itogCount = currType.inYearCount();
	}
	
	public WOActionResults addType() {
		WOComponent selector = pageWithName("SelectorPopup");
		selector.takeValueForKey(context().page(), "returnPage");
		selector.takeValueForKey("currType", "resultPath");
		selector.takeValueForKey(this, "resultGetter");
		NSDictionary dict = (NSDictionary)application().valueForKeyPath(
				"strings.RujelEduResults_EduResults.addType");
		dict = PlistReader.cloneDictionary(dict, true);
		dict.takeValueForKeyPath(ec, "presenterBindings.ec");
		dict.takeValueForKeyPath(valueForKeyPath("allTypes.@max.sort"),
				"presenterBindings.maxSort");
		selector.takeValueForKey(dict, "dict");
		return selector;
	}
	
	public String styleClass() {
		if(item instanceof NSDictionary) {
//			ItogType type = (ItogType)valueForKeyPath("item.type");
			if(item == currType)
				return "selection";
			else
				return "ungerade";
		} else if(item instanceof ItogContainer) {
			return "gerade";
		} else
			return null;
	}
	
	public WOActionResults generateItogs() {
		ec.lock();
		Integer eduYear = (Integer)session().valueForKey("eduYear");
		ItogType itogType = (ItogType)currType.valueForKey("type");
		try {
			itogType.generateItogsInYear(eduYear);
			ec.saveChanges();
			itogsList = itogType.itogsInYear(eduYear);
			logger.log(WOLogLevel.COREDATA_EDITING, "Generated itogs for type in year "
					+ eduYear, new Object[] {session(),itogType});
		} catch (Exception e) {
			logger.log(WOLogLevel.WARNING,"Error generating itogs for type in year "
					+ eduYear, new Object[] {session(),itogType,e});
			session().takeValueForKey(e.getMessage(), "message");
			ec.revert();
		} finally {
			ec.unlock();
		}
		return null;
	}
	
	public WOActionResults addItogToType() {
		ec.lock();
		Integer eduYear = (Integer)session().valueForKey("eduYear");
		ItogType itogType = (ItogType)currType.valueForKey("type");
		try {
			Integer num = new Integer(1);
			itogsList = itogType.itogsInYear(eduYear);
			if(itogsList != null && itogsList.count() > 0) {
				ItogContainer ic = (ItogContainer)itogsList.lastObject();
				num = new Integer(ic.num() + 1);
			}
			ItogContainer ic = (ItogContainer)EOUtilities.createAndInsertInstance(ec,
					ItogContainer.ENTITY_NAME);
			ic.addObjectToBothSidesOfRelationshipWithKey(itogType,ItogContainer.ITOG_TYPE_KEY);
			ic.setNum(num);
			ic.setEduYear(eduYear);
			ec.saveChanges();
			if(itogsList == null)
				itogsList = new NSArray(ic);
			else
				itogsList = itogsList.arrayByAddingObject(ic);
			logger.log(WOLogLevel.COREDATA_EDITING, "Added itog container",
					new Object[] {session(),ic});
		} catch (Exception e) {
			logger.log(WOLogLevel.WARNING,"Error adding itog to type in year " + eduYear,
					new Object[] {session(),itogType,e});
			session().takeValueForKey(e.getMessage(), "message");
			ec.revert();
		} finally {
			ec.unlock();
		}
		return null;
	}
	
	public WOActionResults prepareItog() {
		if(currType == null)
			return null;
		ItogType itogType = (ItogType)currType.valueForKey("type");
		if(itogType.inYearCount().intValue() > 0)
			return generateItogs();
		else
			return addItogToType();
	}
	
	public String prepareTitle() {
		if(currType == null)
			return null;
		ItogType itogType = (ItogType)currType.valueForKey("type");
		if(itogType.inYearCount().intValue() > 0)
			return (String)session().valueForKeyPath(
					"strings.RujelEduResults_EduResults.generateItogs");
		else
			return (String)session().valueForKeyPath(
					"strings.RujelEduResults_EduResults.addItog");
	}
	
	public Boolean cantPrepare() {
		if(currType == null)// || currType.valueForKey("ItogTypeList") == null)
			return Boolean.TRUE;
		ItogType itogType = (ItogType)currType.valueForKey("type");
		if(itogsList != null && itogType.inYearCount().intValue() > 0
				&& itogsList.count() >= itogType.inYearCount().intValue())
			return Boolean.TRUE;
		return (Boolean)session().valueForKeyPath("readAccess._create.ItogContainer");
	}
	
	public String typeId () {
		if(item == currType)
			return "currType";
		return null;
	}
	
	public Boolean cantDeleteContainer() {
		if(item == null)
			return null;
		Boolean acc = (Boolean)session().valueForKeyPath("readAccess._delete.item");
		if(acc != null && acc.booleanValue())
			return acc;
		EOQualifier qual = new EOKeyValueQualifier(ItogMark.CONTAINER_KEY,
				EOQualifier.QualifierOperatorEqual,item);
		EOFetchSpecification fs = new EOFetchSpecification(ItogMark.ENTITY_NAME,qual,null);
		fs.setFetchLimit(1);
		NSArray found = ec.objectsWithFetchSpecification(fs);
		return Boolean.valueOf(found != null && found.count() > 0);
	}
	
	public WOActionResults deleteContainer() {
		ItogContainer itog = (ItogContainer)item;
		session().setObjectForKey(itog, "deleteItogContainer");
		NSArray mods = (NSArray)session().valueForKeyPath("modules.deleteItogContainer");
		session().removeObjectForKey("deleteItogContainer");
		if(mods != null && mods.count() > 0)
			return null;
		StringBuilder desc = new StringBuilder();
		desc.append(itog.name()).append(' ').append('(');
		desc.append(MyUtility.presentEduYear(itog.eduYear())).append(')');
		ItogType type = itog.itogType();
		try {
			ec.deleteObject(itog);
			ec.saveChanges();
			if(!(itogsList instanceof NSMutableArray)) {
				itogsList = itogsList.mutableClone();
			}
			((NSMutableArray)itogsList).removeIdenticalObject(itog);
			logger.log(WOLogLevel.COREDATA_EDITING, "Deleted itog container "
					+ desc, new Object[] {session(),type});
		} catch (Exception e) {
			logger.log(WOLogLevel.WARNING,"Error deleting itog container "
					+ desc, new Object[] {session(),type,e});
			session().takeValueForKey(e.getMessage(), "message");
			ec.revert();
		}
		return null;
	}

	
	public boolean synchronizesVariablesWithBindings() {
        return false;
	}

	public NSDictionary currPreset() {
		Integer presetGroup = (Integer)currType.valueForKeyPath("ItogTypeList.presetGroup");
		if(presetGroup == null) {
			presetGroup = (Integer)currType.valueForKey(ItogPreset.PRESET_GROUP_KEY);
			return null;
		}
		for (int i = 0; i < allPresets.count(); i++) {
			NSMutableDictionary dict = (NSMutableDictionary)allPresets.objectAtIndex(i);
			if(presetGroup.equals(dict.valueForKey("presetGroup"))) {
				return dict;
			}
		}
		return null;
	}

	public void setCurrPreset(NSDictionary currPreset) {
		if(currPreset == null) {
			currPreset = (NSDictionary)allPresets.objectAtIndex(0);
		}
		Integer pg = (Integer)currPreset.valueForKey(ItogPreset.PRESET_GROUP_KEY);
		if(!pg.equals(currType.valueForKey(ItogPreset.PRESET_GROUP_KEY))) {
			currType.takeValueForKey(pg, ItogPreset.PRESET_GROUP_KEY);
			EOEnterpriseObject itl = (EOEnterpriseObject)currType.valueForKey("ItogTypeList");
			if(itl == null)
				return;
			itl.takeValueForKey(pg, ItogPreset.PRESET_GROUP_KEY);
		}
		currType.takeValueForKey(currPreset.valueForKey("max"), "preset");
		if(allPresets.indexOfIdenticalObject(currPreset) <= 0)
			updatePresets();
	}
	
	public WOActionResults editPreset() {
		WOComponent nextPage = pageWithName("PresetEditor");
		nextPage.takeValueForKey(context().page(), "returnPage");
		nextPage.takeValueForKey(ec, "ec");

		nextPage.takeValueForKey("currPreset", "resultPath");
		nextPage.takeValueForKey(this, "resultGetter");
//		Integer presetGroup = (Integer)currType.valueForKeyPath("ItogTypeList.presetGroup");
//		nextPage.takeValueForKey(presetGroup, "presetGroup");
		return nextPage;
	}
}