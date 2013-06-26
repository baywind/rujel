package net.rujel.ui;

import java.math.BigDecimal;
import java.util.logging.Logger;

import net.rujel.criterial.WorkType;
import net.rujel.reusables.ModulesInitialiser;
import net.rujel.reusables.WOLogLevel;

import com.webobjects.appserver.WOContext;
import com.webobjects.appserver.WOComponent;
import com.webobjects.eoaccess.EOUtilities;
import com.webobjects.eocontrol.EOEditingContext;
import com.webobjects.eocontrol.EOFetchSpecification;
import com.webobjects.eocontrol.EOSortOrdering;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSMutableArray;

public class WorkTypeSetup extends WOComponent {
	protected static final Logger logger = Logger.getLogger("rujel.criterial");

    public NSMutableArray types;
    public WorkType typeItem;
    public WorkType currType;

	public WorkTypeSetup(WOContext context) {
        super(context);
    }
    
	public static NSArray types(EOEditingContext ec) {
		EOFetchSpecification fs = new EOFetchSpecification(WorkType.ENTITY_NAME,null,
				ModulesInitialiser.sorter);
		return ec.objectsWithFetchSpecification(fs);
	}
	
	public NSArray types() {
		if(types == null) {
			EOEditingContext ec = (EOEditingContext)valueForBinding("ec");
			NSArray tmp = types(ec);
			types = (tmp == null)?new NSMutableArray() : tmp.mutableClone();
		}
		if(currType != null && currType.editingContext() == null) {
			types.removeObject(currType);
			currType = null;
		}
		return types;
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
    
    public void saveOrCreate() {
		EOEditingContext ec = (EOEditingContext)valueForBinding("ec");
		if(currType == null) {
	    	currType = (WorkType)EOUtilities.createAndInsertInstance(ec,
	    			WorkType.ENTITY_NAME);
	    	types.addObject(currType);
		} else {
			try {
				ec.saveChanges();
				logger.log(WOLogLevel.COREDATA_EDITING,"Changed workType",
						new Object[] {session(),currType});
				currType = null;
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
    	return (typeItem != currType);
    }

    public boolean cantEditFlags() {
    	return (typeItem != currType || currType.namedFlags().flagForKey("system"));
    }

    
    public Boolean cantClick() {
    	if(typeItem == currType)
    		return Boolean.TRUE;
    	return (Boolean)session().valueForKeyPath("readAccess._edit.typeItem");
    }
    
    public String buttonTitle() {
    	if(currType == null)
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
}