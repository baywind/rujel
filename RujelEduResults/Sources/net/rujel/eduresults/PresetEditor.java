package net.rujel.eduresults;

import java.math.BigDecimal;
import java.util.logging.Logger;

import net.rujel.base.MyUtility;
import net.rujel.reusables.Various;
import net.rujel.reusables.WOLogLevel;

import com.webobjects.appserver.WOActionResults;
import com.webobjects.appserver.WOContext;
import com.webobjects.appserver.WOComponent;
import com.webobjects.appserver.WOResponse;
import com.webobjects.eoaccess.EOUtilities;
import com.webobjects.eocontrol.EOEditingContext;
import com.webobjects.eocontrol.EOSortOrdering;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSKeyValueCodingAdditions;
import com.webobjects.foundation.NSMutableArray;
import com.webobjects.foundation.NSMutableDictionary;

public class PresetEditor extends WOComponent {
	
	protected static Logger logger = Logger.getLogger("rujel.itog");
	public WOComponent returnPage;
	public NSKeyValueCodingAdditions resultGetter;
	public String resultPath;
	public EOEditingContext ec;
	public Integer presetGroup;
	public String groupName;
	public NSMutableArray presets;
	protected NSMutableArray<ItogPreset> toDelete = new NSMutableArray<ItogPreset>();
	public NSMutableDictionary dict = new NSMutableDictionary();
	public ItogPreset item;
	public Integer index;
	public Boolean readOnly;
	
    public PresetEditor(WOContext context) {
        super(context);
        readOnly = (Boolean)context.session().valueForKeyPath("readAccess._edit.ItogPreset");
    }
    
    public void setEc(EOEditingContext newEc) {
    	ec = new EOEditingContext(newEc);
    	ec.lock();
    }
    
    public void awake() {
    	super.awake();
    	if(ec != null)
    		ec.lock();
    }
    
    public void sleep() {
    	if(ec != null)
    		ec.unlock();
    	super.sleep();
    }
    
	public void appendToResponse(WOResponse aResponse, WOContext aContext) {
		if(presetGroup == null) {
			String pg = context().request().stringFormValueForKey("gr");
			presetGroup = Integer.valueOf(pg);
		}
//		ec.lock();
		if(presets == null && dict.count() == 0) {
			dict.takeValueForKey(Integer.valueOf(0), ItogPreset.STATE_KEY);
			NSArray found = ItogPreset.listPresetGroup(ec, presetGroup, false);
			if(found == null) {
				if(presetGroup.intValue() < 0) {
					presetGroup = Integer.valueOf(-presetGroup);
					dict.takeValueForKey(Boolean.TRUE, "isPercent");
					dict.takeValueForKey("66.67", "goodValue");
					dict.takeValueForKey("50", "acceptableValue");
				} else {
					found = new NSMutableArray();
					generatePresets();
					groupName = (String)session().valueForKeyPath(
							"strings.RujelEduResults_EduResults.properties.ItogPreset.newPreset");
				}
		        readOnly = (Boolean)session().valueForKeyPath("readAccess._create.ItogPreset");
			} else {
				presets = found.mutableClone();
				item = (ItogPreset)found.objectAtIndex(0);
				if(item.mark().charAt(0) == '%') {
					dict.takeValueForKey(Boolean.TRUE, "isPercent");
					groupName = item.mark();
					if(item.state().intValue() == 3) {
						dict.takeValueForKey(item, "good");
						dict.takeValueForKey(percentValue(), "goodValue");
						if(found.count() > 1)
							item = (ItogPreset)found.objectAtIndex(1);
					}
					if(item.state().intValue() == 2) {
						dict.takeValueForKey(item, "acceptable");
						dict.takeValueForKey(percentValue(), "acceptableValue");
					}
				} else {
					groupName = ItogPreset.nameForGroup(found);
				}
				item = null;
			}
		}
		super.appendToResponse(aResponse, aContext);
//		ec.unlock();
	}
	
	protected void generatePresets() {
		presets = new NSMutableArray();
		ItogPreset preset = newPreset();
		preset.setValue(BigDecimal.ONE);
		preset.setState(Integer.valueOf(3));
		preset.setMark(ItogPreset.stateSymbols.objectAtIndex(3));
//		presets.addObject(preset);
		preset = newPreset();
		preset.setValue(BigDecimal.valueOf(0.5));
		preset.setState(Integer.valueOf(2));
		preset.setMark(ItogPreset.stateSymbols.objectAtIndex(2));
//		presets.addObject(preset);
		preset = newPreset();
		preset.setValue(BigDecimal.ZERO);
		preset.setState(Integer.valueOf(1));
		preset.setMark(ItogPreset.stateSymbols.objectAtIndex(1));
//		presets.addObject(preset);
	}

	public WOActionResults update() {
//		ec.lock();
		EOSortOrdering.sortArrayUsingKeyOrderArray(presets, ItogPreset.sorter);
		groupName = ItogPreset.nameForGroup(presets);
//		ec.unlock();
		return null;
	}
	
	protected ItogPreset newPreset() {
		ItogPreset preset;
		if(toDelete.count() > 0) {
			preset = (ItogPreset)toDelete.removeLastObject();
			preset.setState(Integer.valueOf(0));
		} else {
			preset = (ItogPreset)EOUtilities.createAndInsertInstance(ec, ItogPreset.ENTITY_NAME);
			preset.setPresetGroup(presetGroup);
		}
		if(presets != null)
			presets.addObject(preset);
		return preset;
	}
		
	public WOActionResults addPreset() {
//		ec.lock();
		String percent = (String)dict.valueForKey(ItogPreset.VALUE_KEY);
		if(percent == null) {
			dict.takeValueForKey(null, ItogPreset.VALUE_KEY);
		} else { try {
			BigDecimal value = new BigDecimal(percent);
			dict.takeValueForKey(value.movePointLeft(2),ItogPreset.VALUE_KEY);
		} catch (NumberFormatException e) {
			dict.takeValueForKey(null, ItogPreset.VALUE_KEY);
		}}
		newPreset().takeValuesFromDictionary(dict);
		dict.removeAllObjects();
		dict.takeValueForKey(Integer.valueOf(0), ItogPreset.STATE_KEY);
//		EOSortOrdering.sortArrayUsingKeyOrderArray(presets, ItogPreset.sorter);
//		groupName = ItogPreset.nameForGroup(presets);
		update();
//		ec.unlock();
		return null;
	}
	
	public WOActionResults delete() {
//		ec.lock();
		if(index != null && index.intValue() >= 0 && index.intValue() < presets.count())
			toDelete.addObject(presets.removeObjectAtIndex(index.intValue()));
//		EOSortOrdering.sortArrayUsingKeyOrderArray(presets, ItogPreset.sorter);
//		groupName = ItogPreset.nameForGroup(presets);
		update();
//		ec.unlock();
		return null;
	}

	public WOActionResults cancel() {
		ec.revert();
		returnPage.ensureAwakeInContext(context());
		return returnPage;
	}
	
	public WOActionResults save() {
//		ec.lock();
		boolean passed = false;
		boolean isPercent = Various.boolForObject(dict.valueForKey("isPercent"));
		try {
			if(isPercent) {
				if(groupName == null)
					groupName = "%%%";
				else if(groupName.charAt(0) != '%')
					groupName = "%" + groupName;
				ItogPreset[] levels = new ItogPreset[2];
				levels[1] = updateLevel("acceptable",2);
				levels[0] = updateLevel("good",3);
				if(presets == null) {
					presets = new NSMutableArray();
				} else if(presets.count() > 0) {
					toDelete.addObjectsFromArray(presets);
					presets.removeAllObjects();
				}
				if(levels[0] != null)
					presets.addObject(levels[0]);
				if(levels[1] != null)
					presets.addObject(levels[1]);
			} else {
				for (int i = 0; i < presets.count(); i++) {
					item = (ItogPreset)presets.objectAtIndex(i);
					if(item.mark().charAt(0) == '%')
						item.setMark(" " + item.mark());
				}
			}
			while(toDelete.count() > 0) {
				item = (ItogPreset)toDelete.removeLastObject();
				presets.removeIdenticalObject(item);
				ec.deleteObject(item);
			}
			item = null;
			ec.saveChanges();
			passed = true;
			((EOEditingContext)ec.parentObjectStore()).saveChanges();
			logger.log(WOLogLevel.COREDATA_EDITING, "Saved changes to PresetGroup " +
					presetGroup, session());
		} catch (Exception e) {
			logger.log(WOLogLevel.WARNING,"Failed to save changes to PresetGroup " + presetGroup,
					new Object[] {session(),e});
			if(passed)
				((EOEditingContext)ec.parentObjectStore()).revert();
			session().takeValueForKey(e.getMessage(),"message");
//		} finally {
//			ec.unlock();
		}
		if(passed && resultPath != null) {
			update();
			NSMutableDictionary result = new NSMutableDictionary(
					presetGroup,ItogPreset.PRESET_GROUP_KEY);
			if(presets == null || presets.count() == 0) {
				result = null;
			} else {
				result.takeValueForKey(presets,"list");
				result.takeValueForKey(groupName, "fullName");
				if(isPercent) {
					result.takeValueForKey(groupName, "max");
				} else {
					item = (ItogPreset)presets.objectAtIndex(0);
					result.takeValueForKey(item.mark(), "max");
				}
				result.takeValueForKey(Boolean.valueOf(isPercent), "isPercent");
			}
			if(resultGetter == null)
				returnPage.takeValueForKeyPath(result, resultPath);
			else
				resultGetter.takeValueForKeyPath(result, resultPath);
		}
		returnPage.ensureAwakeInContext(context());
		return returnPage;
	}

	protected ItogPreset updateLevel(String key, int state) {
		String percent = (String)dict.valueForKey(key + "Value");
		item = (ItogPreset)dict.valueForKey(key);
		if(percent == null) {
			if(item != null && presets != null) {
				presets.removeIdenticalObject(item);
				toDelete.addObject(item);
			}
			return null;
		}
		if(item == null) {
			item = newPreset();
		}
		if(!groupName.equals(item.mark()))
			item.setMark(groupName);
		if(item.state().intValue() != state)
			item.setState(Integer.valueOf(state));
		try {
			BigDecimal value = new BigDecimal(percent);
			value = value.movePointLeft(2);
			if(item.value() == null || value.compareTo(item.value()) != 0)
				item.setValue(value);
		} catch (Exception e) {
			return null;
		}
		if(presets != null)
			presets.removeIdenticalObject(item);
		return item;
	}

	public String rowName() {
		return "state" + index;
	}

	public String percentValue() {
		if(item == null)
			return null;
		BigDecimal value = item.value();
		if(value == null)
			return null;
		if(value.compareTo(BigDecimal.ONE) == 0)
			return "100";
		if(value.compareTo(BigDecimal.ZERO) == 0)
			return "0";
		return MyUtility.formatDecimal(value.movePointRight(2));
	}

	public void setPercentValue(String percentValue) {
		if(item == null)
			return;
		if(percentValue == null) {
			item.setValue(null);
			return;
		}
		try {
			BigDecimal value = new BigDecimal(percentValue);
			if(item.value() == null || value.compareTo(item.value()) != 0)
				item.setValue(value.movePointLeft(2));
		} catch (NumberFormatException e) {
			// do nothing
		}
	}
}