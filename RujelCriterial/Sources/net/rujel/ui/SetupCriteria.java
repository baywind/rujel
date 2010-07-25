package net.rujel.ui;

import java.util.logging.Logger;

import net.rujel.base.Indexer;
import net.rujel.base.SettingsBase;
import net.rujel.criterial.CriteriaSet;
import net.rujel.reusables.SessionedEditingContext;
import net.rujel.reusables.WOLogLevel;

import com.webobjects.appserver.WOActionResults;
import com.webobjects.appserver.WOContext;
import com.webobjects.appserver.WOComponent;
import com.webobjects.eoaccess.EOUtilities;
import com.webobjects.eocontrol.EOEditingContext;
import com.webobjects.eocontrol.EOEnterpriseObject;
import com.webobjects.eocontrol.EOKeyGlobalID;
import com.webobjects.eocontrol.EOSortOrdering;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSDictionary;
import com.webobjects.foundation.NSKeyValueCoding;
import com.webobjects.foundation.NSMutableArray;
import com.webobjects.foundation.NSMutableDictionary;

public class SetupCriteria extends WOComponent {
	
	public static final NSArray sorter = new NSArray(new EOSortOrdering(CriteriaSet.SET_NAME_KEY,
			EOSortOrdering.CompareCaseInsensitiveAscending));
	public static final Logger logger = Logger.getLogger("rujel.criterial");
	
    public SetupCriteria(WOContext context) {
        super(context);
        ec = new SessionedEditingContext(context.session());
        base = SettingsBase.baseForKey(CriteriaSet.ENTITY_NAME, ec, true);
        if(ec.hasChanges()) {
        	try {
				ec.saveChanges();
				logger.log(WOLogLevel.COREDATA_EDITING,"Autogenerated CriteriaSet setting",
						session());
			} catch (Exception e) {
				logger.log(WOLogLevel.WARNING,"Error autogenerating CriteriaSet setting",
						new Object[] {session(),e});
				return;
			}
        }
        NSArray found = EOUtilities.objectsForEntityNamed(ec, CriteriaSet.ENTITY_NAME);
        String noneTitle = (String)session().valueForKeyPath(
				"strings.RujelCriterial_Strings.setup.CriteriaSet.none");
        if(noneTitle == null) noneTitle = "none";
        NSDictionary none = new NSDictionary(noneTitle,CriteriaSet.SET_NAME_KEY);
        if(found != null && found.count() > 0) {
        	sets = found.mutableClone();
        	EOSortOrdering.sortArrayUsingKeyOrderArray(sets, sorter);
        	sets.insertObjectAtIndex(none, 0);
        } else {
        	sets = new NSMutableArray(none);
        }
		currID = base.numericValue();
		if(currID == null || currID.intValue() == 0)
			currSet = none;
		else
			currSet = (CriteriaSet)EOUtilities.objectWithPrimaryKeyValue(ec, 
				CriteriaSet.ENTITY_NAME, currID);
		setCurrSet(currSet);
		indices = Indexer.indexersOfType(ec, "criteria*");
    }
    
    public NSArray indices;
    public EOEditingContext ec;
    public SettingsBase base;
    public NSMutableArray sets;
    public NSKeyValueCoding currSet;
    public Integer currID;
    public Object item;
    public Object item2;
    public NSMutableArray criteria;
    public EOEnterpriseObject criterion;
    public NSMutableDictionary critDict = new NSMutableDictionary();
    public String nameOfCritSet;
    
	public void setNameOfCritSet(String nameOfCritSet) {
		if(nameOfCritSet != null)
			this.nameOfCritSet = nameOfCritSet;
	}

	public WOActionResults selectSet() {
    	setCurrSet((CriteriaSet)item);
    	return null;
    }
    
    public void setCurrSet(NSKeyValueCoding set) {
    	currSet = set;
		nameOfCritSet = (set == null)?null:(String)set.valueForKey(CriteriaSet.SET_NAME_KEY);
    	if(set instanceof CriteriaSet) {
    		EOKeyGlobalID gid = (EOKeyGlobalID)ec.globalIDForObject((CriteriaSet)currSet);
    		currID = (Integer)gid.keyValues()[0];
    		criteria = ((CriteriaSet)set).criteria().mutableClone();
    		if(criteria.count() == 0)
    			return;
    		EOSortOrdering.sortArrayUsingKeyOrderArray(criteria, CriteriaSet.sorter);
    		criterion = (EOEnterpriseObject)criteria.lastObject();
    		String title = (String)criterion.valueForKey("title");
    		criterion = null;
    		critDict.removeAllObjects();
    		if(title.length() == 1) {
    			char t = title.charAt(0);
    			t++;
    			title = String.valueOf(t);
    			critDict.takeValueForKey(title, "title");
    		}
    	} else {
    		currID = null;
    		criteria = null;
    	}
    }
    
//    private static final NSArray keys = new NSArray( new String[] {
//    		"title","dfltMax","dfltWeight","comment"});
    public WOActionResults selectCriter() {
    	criterion = (EOEnterpriseObject)item;
    	return null;
    }
    
    public WOActionResults saveCriter() {
    	boolean create = (criterion == null);
    	if(create) {
    		criterion = ((CriteriaSet)currSet).addCriterion();
    		criterion.takeValuesFromDictionary(critDict);
    	}
    	try {
			ec.saveChanges();
			logger.log(WOLogLevel.COREDATA_EDITING,"Criterion saved",
					new Object[] {session(),criterion});
			if(create) {
				criteria.addObject(criterion);
	    		String title = (String)criterion.valueForKey("title");
	    		critDict.removeAllObjects();
	    		if(title.length() == 1) {
	    			char t = title.charAt(0);
	    			t++;
	    			title = String.valueOf(t);
	    			critDict.takeValueForKey(title, "title");
	    		}
			}
		} catch (Exception e) {
			logger.log(WOLogLevel.WARNING,"Error saving criterion",
					new Object[] {session(),criterion,e});
			session().takeValueForKey(e.getMessage(), "message");
			ec.revert();
		}
		criterion = null;
    	return null;
    }
    
    public Boolean isSelected() {
    	if(criterion != item)
    		return Boolean.FALSE;
    	return (Boolean)session().valueForKeyPath("readAccess.edit.CriteriaSet");
    }
    
    public WOActionResults deleteCriter() {
    	if(criterion == null)
    		return null;
    	ec.deleteObject(criterion);
    	try {
			logger.log(WOLogLevel.COREDATA_EDITING,"Deleting criterion",
					new Object[] {session(),criterion});
    		ec.saveChanges();
    		criteria.removeObject(criterion);
    		criterion = null;
		} catch (Exception e) {
			logger.log(WOLogLevel.WARNING,"Error deleting criterion",
					new Object[] {session(),criterion,e});
			session().takeValueForKey(e.getMessage(), "message");
			ec.revert();
    	}
		return null;
    }
    
    public Boolean canAddCriter() {
    	if(criterion != null)
    		return Boolean.FALSE;
    	return (Boolean)session().valueForKeyPath("readAccess.edit.CriteriaSet");
    }
    
    public String styleCritSave() {
		if(criterion == null)
			return "visibility:hidden;";
		return null;
	}
    
    public WOActionResults saveCritSet() {
    	if(nameOfCritSet == null)
    		return null;
    	boolean create = nameOfCritSet.startsWith("$new$");
    	if(create) {
    		nameOfCritSet = nameOfCritSet.substring(5);
    		currSet = EOUtilities.createAndInsertInstance(ec, CriteriaSet.ENTITY_NAME);
    	}
		currSet.takeValueForKey(nameOfCritSet, CriteriaSet.SET_NAME_KEY);
		try {
			NSArray usage = base.allForSetting(SettingsBase.NUMERIC_VALUE_KEY, currID, null);
			if(usage != null && usage.count() > 0) {
				EOEnterpriseObject bc = (EOEnterpriseObject)usage.objectAtIndex(0);
				if(!nameOfCritSet.equals(bc.valueForKey(SettingsBase.TEXT_VALUE_KEY)))
					usage.takeValueForKey(nameOfCritSet, SettingsBase.TEXT_VALUE_KEY);
			}
			ec.saveChanges();
			if(create) {
				setCurrSet(currSet);
				sets.addObject(currSet);
			}
			logger.log(WOLogLevel.UNOWNED_EDITING,"CriteriaSet " + ((create)?"created":"renamed"),
					new Object[] {session(),currSet});
		} catch (Exception e) {
			if(create)
				currSet = null;
			logger.log(WOLogLevel.INFO,"Error saving CriteriaSet",
					new Object[] {session(),currSet,e});
			session().takeValueForKey(e.getMessage(), "message");
			currSet.takeValueForKey(currSet.valueForKey("flags"), "flags");
			ec.revert();
		}
    	return null;
    }

	public WOActionResults deleteCritSet() {
		if(currSet instanceof CriteriaSet) {
			try {
				NSArray usage = base.allForSetting(SettingsBase.NUMERIC_VALUE_KEY, currID, null);
				if(currID.equals(base.numericValue())) {
					usage.takeValueForKey(null, SettingsBase.NUMERIC_VALUE_KEY);
			        String noneTitle = (String)session().valueForKeyPath(
						"strings.RujelCriterial_Strings.setup.CriteriaSet.none");
			        usage.takeValueForKey(noneTitle, SettingsBase.TEXT_VALUE_KEY);
				} else if(usage.count() > 0) {
					for (int i = 0; i < usage.count(); i++) {
						ec.deleteObject((EOEnterpriseObject)usage.objectAtIndex(i));
					}
				}
				ec.deleteObject((CriteriaSet)currSet);
				ec.saveChanges();
				logger.log(WOLogLevel.COREDATA_EDITING,"Deleted CriteriaSet " 
						+ nameOfCritSet,session());
				sets.removeObject(currSet);
				setCurrSet((NSKeyValueCoding)sets.objectAtIndex(0));
			} catch (Exception e) {
				ec.revert();
				logger.log(WOLogLevel.COREDATA_EDITING,"Error deleting CriteriaSet " 
						+ nameOfCritSet,new Object[] {session(),currSet,e});
				session().takeValueForKey(e.getMessage(), "message");
			}
		}
		return null;
	}

    public boolean synchronizesVariablesWithBindings() {
        return false;
	}

}