package net.rujel.ui;

import java.util.logging.Logger;

import net.rujel.base.IndexRow;
import net.rujel.base.Indexer;
import net.rujel.reusables.WOLogLevel;

import com.webobjects.appserver.WOActionResults;
import com.webobjects.appserver.WOContext;
import com.webobjects.appserver.WOComponent;
import com.webobjects.appserver.WOElement;

public class EditIndex extends WOComponent {
	
	public static final Logger logger = Logger.getLogger("rujel.base");
	
    public EditIndex(WOContext context) {
        super(context);
    }
    
    protected Indexer _ind;
    public IndexRow rowItem;
    public Integer newIdx;
    
    public Indexer indexer() {
    	if(_ind == null) {
    		_ind = (Indexer)valueForBinding("indexer");
    	}
    	return _ind;
    }
    
    public WOElement template() {
    	_ind = (Indexer)valueForBinding("indexer");
    	if(_ind == null)
    		return null;
    	return super.template();
    }

    public WOActionResults delete() {
    	if(rowItem == null)
    		return null;
    	Integer idx = rowItem.idx();
    	String value = rowItem.value();
    	rowItem.setComment(null);
    	indexer().removeObjectFromBothSidesOfRelationshipWithKey(rowItem, Indexer.INDEX_ROWS_KEY);
    	indexer().editingContext().deleteObject(rowItem);
    	try {
    		indexer().editingContext().saveChanges();
    		logger.log(WOLogLevel.UNOWNED_EDITING,"Deleted indexRow " + 
    				idx + ": " + value, new Object[] {session(), indexer()});
		} catch (Exception e) {
			logger.log(WOLogLevel.INFO, "Error deleting indexRow " + idx,
					new Object[] {session(),rowItem,e});
			indexer().editingContext().revert();
			session().takeValueForKey(e.getMessage(), "message");
		}
    	return null;
    }
    
    public boolean isStateless() {
		return true;
	}
	public boolean synchronizesVariablesWithBindings() {
        return false;
	}
	
	public void reset() {
		_ind = null;
		newIdx = null;
		rowItem = null;
		super.reset();
	}

}