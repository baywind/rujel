package net.rujel.ui;

import java.math.BigDecimal;
import java.util.logging.Logger;

import net.rujel.base.QualifiedSetting;
import net.rujel.base.ReadAccess;
import net.rujel.base.Setting;
import net.rujel.base.SettingsBase;
import net.rujel.criterial.CriteriaSet;
import net.rujel.criterial.WorkType;
import net.rujel.reusables.ModulesInitialiser;
import net.rujel.reusables.MutableFlags;
import net.rujel.reusables.WOLogLevel;

import com.webobjects.appserver.WOContext;
import com.webobjects.appserver.WOResponse;
import com.webobjects.appserver.WOActionResults;
import com.webobjects.appserver.WOComponent;
import com.webobjects.eoaccess.EOUtilities;
import com.webobjects.eocontrol.EOEditingContext;
import com.webobjects.eocontrol.EOFetchSpecification;
import com.webobjects.eocontrol.EOSortOrdering;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSDictionary;
import com.webobjects.foundation.NSMutableArray;
import com.webobjects.foundation.NSMutableDictionary;

public class WorkTypeSetup extends WOComponent {
	protected static final Logger logger = Logger.getLogger("rujel.criterial");

    public NSMutableArray types;
    public WorkType typeItem;
    public WorkType currType;
    public NSMutableArray critSets;
    public Object item;
    protected NSMutableDictionary<Integer, Integer> reSort = new NSMutableDictionary<Integer, Integer>();
    public SettingsBase maskSettings;
    public Setting currMask;
    public Object rowspan;
    protected MutableFlags maskFlags = new MutableFlags();

	public WorkTypeSetup(WOContext context) {
        super(context);
    }
    
	public static NSArray types(EOEditingContext ec) {
		EOFetchSpecification fs = new EOFetchSpecification(WorkType.ENTITY_NAME,null,
				ModulesInitialiser.sorter);
		return ec.objectsWithFetchSpecification(fs);
	}
	
	public void appendToResponse(WOResponse aResponse, WOContext aContext) {
		if(types == null) {
			EOEditingContext ec = (EOEditingContext)valueForBinding("ec");
			NSArray tmp = types(ec);
			types = (tmp == null)?new NSMutableArray() : tmp.mutableClone();

			NSArray found = EOUtilities.objectsForEntityNamed(ec, CriteriaSet.ENTITY_NAME);
	        String noneTitle = (String)session().valueForKeyPath(
					"strings.RujelCriterial_Strings.setup.CriteriaSet.none");
	        if(noneTitle == null) noneTitle = "none";
	        NSDictionary none = new NSDictionary(noneTitle,CriteriaSet.SET_NAME_KEY);
	        if(found != null && found.count() > 0) {
	        	critSets = found.mutableClone();
	        	EOSortOrdering.sortArrayUsingKeyOrderArray(critSets, SetupCriteria.sorter);
	        	critSets.insertObjectAtIndex(none, 0);
	        } else {
	        	critSets = new NSMutableArray(none);
	        }
	        maskSettings = SettingsBase.baseForKey("WorkTypeMask", ec, false);
		}
		if(currType != null && currType.editingContext() == null) {
			types.removeObject(currType);
			currType = null;
		}
		if(currMask != null && currMask.editingContext() == null) {
			currMask = null;
			maskFlags.setFlags(0);
		}
		item = null;
		typeItem = null;
		super.appendToResponse(aResponse, aContext);
	}
	
    public String rowColor() {
    	if(typeItem == null)
    		return null;
    	if(typeItem.namedFlags().flagForKey("unused"))
    		return "#999999";
    	if(typeItem.dfltWeight() != null && 
    			typeItem.dfltWeight().compareTo(BigDecimal.ZERO) > 0)
    		return typeItem.colorWeight();
    	else
    		return typeItem.colorNoWeight();
    }
    
    public Integer sortValue() {
    	Integer sort = typeItem.sort();
    	Integer value = reSort.objectForKey(sort);
    	if(value == null)
    		return sort;
    	return value;
    }
    
    public void setSortValue(Integer value) {
    	if(value == null)
    		return;
    	Integer sort = typeItem.sort();
    	if(sort == null) {
    		typeItem.setSort(value);
    		return;
    	}
    	if(value.equals(sort))
    		return;
    	reSort.setObjectForKey(value, sort);
    }
    
    public WOActionResults saveOrCreate() {
		EOEditingContext ec = (EOEditingContext)valueForBinding("ec");
		if(currType == null && currMask == null) {
	    	currType = (WorkType)EOUtilities.createAndInsertInstance(ec,
	    			WorkType.ENTITY_NAME);
	    	types.addObject(currType);
		} else {
			if(reSort.count() > 0) {
				if(maskSettings != null) {
					updateSetting(maskSettings);
					NSArray byCourse = maskSettings.qualifiedSettings();
					if(byCourse != null && byCourse.count() > 0) {
						for (int i = 0; i < byCourse.count(); i++) {
							Setting s = (Setting)byCourse.objectAtIndex(i);
							updateSetting(s);
						}
					}
				}
				for (int i = 0; i < types.count(); i++) {
					WorkType type = (WorkType)types.objectAtIndex(i);
					Integer val = reSort.objectForKey(type.sort());
					if(val != null)
						type.setSort(val);
				}
				reSort.removeAllObjects();
			} //reSort
			if(currMask != null) {
				if(currMask.numericValue() == null || 
						maskFlags.intValue() != currMask.numericValue().intValue())
					currMask.takeValueForKey(maskFlags.toInteger(), SettingsBase.NUMERIC_VALUE_KEY);
			}
			if(!ec.hasChanges()) {
				currType = null;
				currMask = null;
				maskFlags.setFlags(0);
				return null;
			}
			try {
				ec.saveChanges();
				if(currType != null)
					logger.log(WOLogLevel.COREDATA_EDITING,"Changed workType",
							new Object[] {session(),currType});
				if(currMask != null)
					logger.log(WOLogLevel.SETTINGS_EDITING,"Changed workType mask settings",
							new Object[] {session(),currMask});
				currType = null;
				currMask = null;
				maskFlags.setFlags(0);
				EOSortOrdering.sortArrayUsingKeyOrderArray(types, ModulesInitialiser.sorter);
			} catch (Exception e) {
				ec.revert();
				String message = e.getMessage();
				java.util.logging.Level level = WOLogLevel.WARNING;
				if(message.equals(
						"The typeName property of WorkType is not allowed to be null.")) {
					message = (String)session().valueForKeyPath(
							"strings.Strings.messages.nullProhibit");
					message = String.format(message, session().valueForKeyPath(
						"strings.RujelCriterial_Strings.setup.WorkType.workType"));
					level = WOLogLevel.FINE;
				}
				session().takeValueForKey(message, "message");
				logger.log(level,"Error saving workType changes",
						new Object[] {session(),currType,e});
				if(currType.editingContext() == null) {
					types.removeObject(currType);
					currType = null;
				}
			}
		}
		return null;
    }
    
    private boolean updateSetting(Setting s) {
    	if(reSort == null || reSort.count() == 0)
    		return false;
    	Integer before = s.numericValue();
    	if(before == null)
    		before = Integer.valueOf(0);
    	int after = updateFlags(before.intValue());
    	if(after == before.intValue())
    		return false;
    	s.takeValueForKey(new Integer(after), SettingsBase.NUMERIC_VALUE_KEY);
    	return true;
    }
    
    
    private int updateFlags(int mask) {
//    	if(reSort == null || reSort.count() == 0)
//        	return mask;
    	int result = 0;
		for (int i = 0; i < 32; i++) {
			int reg = 1 << i;
//			if(reg > mask)
//				break;
			if((reg&mask) > 0) {
				Integer i2 = reSort.objectForKey(new Integer(i));
				if(i2 != null) {
					reg = 1 << i2.intValue();
				}
				result += reg;
			}
		}
    	return result;
    }
    
    public void create() {
    }

    public void select() {
    	if(currType != null) {
    		EOEditingContext ec = currType.editingContext();
    		if(ec.hasChanges())
    			ec.revert();
    		if(currType.editingContext() == null)
    			types.removeObject(currType);
    	}
    	currType = typeItem;
    }
    
    public boolean cantEdit() {
    	if(typeItem != null)
    		return (typeItem != currType);
    	if(item instanceof Setting) {
    		return (item != currMask);
    	}
    	return true;
    }

    public boolean cantEditFlags() {
    	return (typeItem != currType || currType.namedFlags().flagForKey("system"));
    }

    public boolean cantSelect() {
    	ReadAccess readAccess = (ReadAccess)session().valueForKey("readAccess");
    	Integer section = null;
    	if (currMask instanceof QualifiedSetting)
    		section = (Integer)valueForKeyPath("currMask.section.sectionID");
    	return !readAccess.accessForObject("WorkType", section).flagForKey("edit");
    }
    
    public Boolean cantClick() {
    	if(typeItem == currType || currMask != null)
    		return Boolean.TRUE;
    	return (Boolean)session().valueForKeyPath("readAccess._edit.typeItem");
    }
    
    public String buttonTitle() {
    	if(currType == null && currMask == null)
    		return (String)session().valueForKeyPath(
    				"strings.Reusables_Strings.uiElements.Add");
    	else
    		return (String)session().valueForKeyPath(
					"strings.Reusables_Strings.uiElements.Save");
    }
    
    public Boolean showSaveButton() {
    	if(currType == null)
    		return (Boolean)session().valueForKeyPath("readAccess.create.WorkType");
    	else
    		return Boolean.TRUE;
    }
    
    public Boolean cantDelete() {
    	if(currType != typeItem)
    		return Boolean.TRUE;
    	EOEditingContext ec = currType.editingContext();
    	if(ec.globalIDForObject(currType).isTemporary())
    		return Boolean.FALSE;
    	if(currType.isUsed())
    		return Boolean.TRUE;
    	return (Boolean)session().valueForKeyPath("readAccess._delete.currType");
    }
    
    public void delete() {
    	if(currType == null)
    		return;
    	EOEditingContext ec = currType.editingContext();
    	types.removeObject(currType);
    	if(ec == null) {
    		currType = null;
    		return;
    	}
    	if(ec.globalIDForObject(currType).isTemporary()) {
    		ec.revert();
    	} else {
    		ec.deleteObject(currType);
			try {
				logger.log(WOLogLevel.COREDATA_EDITING,"Deleting workType",
						new Object[] {session(),currType});
				ec.saveChanges();
			} catch (Exception e) {
				ec.revert();
				session().takeValueForKey(e.getMessage(), "message");
				logger.log(WOLogLevel.WARNING,"Error saving workType changes",
						new Object[] {session(),currType,e});
			}
    	}
    	currType = null;
    }
    
    public String rowID() {
    	if(typeItem == currType)
    		return "currType";
    	return null;
    }
    
    public boolean synchronizesVariablesWithBindings() {
        return false;
	}
	
	public boolean isStateless() {
		return false;
	}
	
	public void reset() {
		super.reset();
	}

	public Object selectedCritSet() {
		if(typeItem == null)
			return null;
		CriteriaSet result = typeItem.criteriaSet();
		if(result!= null && result.flags() == null)
			return critSets.objectAtIndex(0);
		return result;
	}

	public void setSelectedCritSet(Object selectedCritSet) {
		if(typeItem == null)
			return;
		if(selectedCritSet == null) {
			typeItem.setCriteriaSet(null);
			typeItem.namedFlags().setFlagForKey(false, "specCriter");
		} else {
//			typeItem.namedFlags().setFlagForKey(true, "specCriter");
			if (selectedCritSet instanceof CriteriaSet)
				typeItem.setCriteriaSet((CriteriaSet)selectedCritSet);
			else
				typeItem.setCriteriaSet(CriteriaSet.getNone(typeItem.editingContext()));
		}
		// TODO
	}
	
	public boolean typeIsActive() {
		if(maskFlags == null || typeItem == null)
			return true;
		return maskFlags.getFlag(typeItem.sort());
	}
	
	public void setTypeIsActive(boolean val) {
		if(currMask == null || typeItem == null)
			return;
		maskFlags.setFlag(typeItem.sort(), val);
	}
	
	public WOActionResults selectMask() {
		if(item instanceof Setting) {
			currMask = (Setting)item;
			if(currMask.numericValue() == null)
				maskFlags.setFlags(0);
			else
				maskFlags.setFlags(currMask.numericValue());
		} else {
			currMask = null;
			maskFlags.setFlags(0);
		}
		currType = null;
		return null;
	}
	
	public WOActionResults addMaskSettings() {
		if(maskSettings == null) {
			EOEditingContext ec = (EOEditingContext)valueForBinding("ec");
			maskSettings = SettingsBase.baseForKey("WorkTypeMask", ec, true);
			maskSettings.setNumericValue(Integer.valueOf(-1));
			maskSettings.setTextValue((String)session().valueForKeyPath(
					"strings.RujelBase_Base.noLimit"));
			try {
				ec.saveChanges();
				logger.log(WOLogLevel.SETTINGS_EDITING,"Initiated workType mask settings",
						new Object[] {session(),currMask});
				currType = null;
				currMask = maskSettings;
				maskFlags.setFlags(currMask.numericValue());
			} catch (Exception e) {
				ec.revert();
				session().takeValueForKey(e.getMessage(), "message");
				logger.log(WOLogLevel.WARNING,"Error initiating workType mask",
						new Object[] {session(),currType,e});
				maskSettings = null;
			}
		}
		return null;
    	/*
    	WOComponent editor = pageWithName("ByCourseEditor");
    	editor.takeValueForKey(context().page(), "returnPage");
    	editor.takeValueForKey(maskSettings, "base");
    	editor.takeValueForKey(this, "resultGetter");
    	editor.takeValueForKey("currMask", "pushToKeyPath");
		editor.takeValueForKeyPath(Integer.valueOf(0), "tmpValues.numericValue");
		editor.takeValueForKeyPath("???", "tmpValues.textValue");
    	maskFlags.setFlags(0);
    	return editor;*/
    }

    public String maskStyle() {
    	if(currType == null)
    		return null;
    	if(item instanceof Setting) {
    		Integer flags = ((Setting)item).numericValue();
    		Integer reg = currType.sort();
    		if(reg == null || reg == null)
    			return null;
    		if(MutableFlags.getFlag(reg, flags))
    			return "text-decoration:underline;";
    	}
    	return null;
    }
}