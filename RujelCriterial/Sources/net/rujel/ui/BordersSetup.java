package net.rujel.ui;

import java.math.BigDecimal;

import net.rujel.base.Setting;
import net.rujel.base.SettingsBase;
import net.rujel.criterial.BorderSet;
import net.rujel.criterial.FractionPresenter;
import net.rujel.reusables.WOLogLevel;

import com.webobjects.appserver.WOActionResults;
import com.webobjects.appserver.WOContext;
import com.webobjects.appserver.WOComponent;
import com.webobjects.eoaccess.EOUtilities;
import com.webobjects.eocontrol.EOEditingContext;
import com.webobjects.eocontrol.EOEnterpriseObject;
import com.webobjects.eocontrol.EOGlobalID;
import com.webobjects.eocontrol.EOKeyGlobalID;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSKeyValueCoding;

public class BordersSetup extends WOComponent {
	
	public NSArray borders;
	public Object item;
	public Setting setting;
	public Object item3;
	public BorderSet currentBS;
	public Boolean disabled;
	
    public BordersSetup(WOContext context) {
        super(context);
        disabled = (Boolean)context.session().valueForKeyPath("readAccess._edit.BorderSet");
    }
    
    public NSArray borders() {
    	if(borders == null) {
            EOEditingContext ec = (EOEditingContext)valueForBinding("ec");
            borders = EOUtilities.objectsForEntityNamed(ec, BorderSet.ENTITY_NAME);
    	}
    	return borders;
    }
    
    public String styleClass() {
    	if(item == currentBS)
    		return "selection";
    	if (item instanceof BorderSet) {
			BorderSet bs = (BorderSet) item;
			if(bs.valueType().intValue() > 0)
				return "gerade";
			else
				return "ungerade";
		}
    	return null;
    }
    
    public WOActionResults selectBS() {
        EOEditingContext ec = (EOEditingContext)valueForBinding("ec");
        if(ec.hasChanges())
        	ec.revert();
    	if(currentBS == item)
    		currentBS = null;
    	else
    		currentBS = (BorderSet)item;
    	return null;
    }
    
    public WOActionResults saveBS() {
        EOEditingContext ec = (EOEditingContext)valueForBinding("ec");
        String next = context().request().stringFormValueForKey("nextBorder");
    	if(next != null) {
    		try {
    			BigDecimal nextBorder = new BigDecimal(next);
    			EOEnterpriseObject border = EOUtilities.createAndInsertInstance(ec, "Border");
    			border.takeValueForKey(nextBorder, "least");
    			next = context().request().stringFormValueForKey("nextTitle");
    			border.takeValueForKey(next, "title");
    			currentBS.addObjectToBothSidesOfRelationshipWithKey(border, BorderSet.BORDERS_KEY);
    		} catch (Exception e) {
    			SetupCriteria.logger.log(WOLogLevel.FINE,"Failed to add new border"
    					,new Object[] {session(),currentBS,e});
    		}
    	}
    	if(ec.hasChanges()) {
    		try {
    			ec.saveChanges();
    			SetupCriteria.logger.log(WOLogLevel.COREDATA_EDITING,"Changed BorderSet "
    					+ currentBS.title(), new Object[] {session(), currentBS});
    			if(currentBS != null) {
    				currentBS.flush();
    				if(!borders.contains(currentBS))
    					borders = borders.arrayByAddingObject(currentBS);
    			}
    		} catch (Exception e) {
    			session().takeValueForKey(e.getMessage(), "message");
    			SetupCriteria.logger.log(WOLogLevel.WARNING,"Error saving changes to BorderSet"
    					, new Object[] {session(),currentBS,e});
    			ec.revert();
    			if(currentBS.editingContext() == null)
    				currentBS = null;
    		}

    	}
    	return null;
    }
    
    public WOActionResults addBSet() {
        EOEditingContext ec = (EOEditingContext)valueForBinding("ec");
        if(ec.hasChanges())
        	ec.revert();
        if(currentBS == null || currentBS.editingContext() != null)
        	currentBS = (BorderSet)EOUtilities.createAndInsertInstance(ec, BorderSet.ENTITY_NAME);
        else
        	currentBS = null;
    	return null;
    }
    
    public String newBSetStyle() {
    	if(currentBS == null)
    		return "orange";
        EOEditingContext ec = (EOEditingContext)valueForBinding("ec");
        EOGlobalID gid = ec.globalIDForObject(currentBS);
        return (gid.isTemporary())?"selection":"orange";
    }

	public WOActionResults deleteBorder() {
		if(item instanceof EOEnterpriseObject) {
	        EOEditingContext ec = (EOEditingContext)valueForBinding("ec");
	        ec.deleteObject((EOEnterpriseObject) item);
	   		try {
    			ec.saveChanges();
    			SetupCriteria.logger.log(WOLogLevel.COREDATA_EDITING,"Changed BorderSet "
    					+ currentBS.title(), new Object[] {session(), currentBS});
    			if(currentBS != null && !borders.contains(currentBS))
    				borders = borders.arrayByAddingObject(currentBS);
    		} catch (Exception e) {
    			session().takeValueForKey(e.getMessage(), "message");
    			SetupCriteria.logger.log(WOLogLevel.WARNING,"Error saving changes to BorderSet"
    					, new Object[] {session(),currentBS,e});
    			ec.revert();
    			if(currentBS.editingContext() == null)
    				currentBS = null;
    		}
		}
		return null;
	}
	
	public BorderSet selection() {
		if(setting == null)
			return null;
		EOEditingContext ec = (EOEditingContext)valueForBinding("ec");
		if(setting.numericValue() != null) {
			try {
				return (BorderSet)EOUtilities.objectWithPrimaryKeyValue(ec, 
						SettingsBase.ENTITY_NAME, setting.numericValue());
			} catch (Exception e) {
				;
			}
		}
		String txt = setting.textValue();
		if(txt == null) {
			txt = (String)((NSKeyValueCoding)item).valueForKey("defaultValue");
			if(txt == null)
				return null;
		}
		FractionPresenter result = BorderSet.fractionPresenterForTitle(ec, txt);
		if(result instanceof BorderSet)
			return (BorderSet)result;
		return null;
	}
	
	public void setSelection(BorderSet set) {
		if(set == null) {
			setting.takeValueForKey(null, SettingsBase.NUMERIC_VALUE_KEY);
		} else {
			EOGlobalID gid = set.editingContext().globalIDForObject(set);
			Object id = ((EOKeyGlobalID)gid).keyValues()[0];
			setting.takeValueForKey(id, SettingsBase.NUMERIC_VALUE_KEY);
			setting.takeValueForKey(set.title(), SettingsBase.TEXT_VALUE_KEY);
		}
	}

	public String txtValue() {
		if(setting == null)
			return null;
		if(setting.numericValue() == null) {
			String txt = setting.textValue();
			if(txt != null)
				return txt;
			return (String)((NSKeyValueCoding)item).valueForKey("defaultValue");
		}
		return null;
	}
	
	public void setTxtValue(String txt) {
		if(setting != null && setting.numericValue() == null)
			setting.takeValueForKey(txt, SettingsBase.TEXT_VALUE_KEY);
	}
	
	public String txtStyle() {
		if(setting == null || setting.numericValue() == null)
			return null;
		return "display:none;";
	}
	
    public boolean synchronizesVariablesWithBindings() {
        return false;
	}
}