package net.rujel.dnevnik;

import net.rujel.io.ExtBase;
import net.rujel.io.SyncMatch;

import com.webobjects.appserver.WOContext;
import com.webobjects.appserver.WOComponent;
import com.webobjects.eocontrol.EOEditingContext;
import com.webobjects.eocontrol.EOEnterpriseObject;
import com.webobjects.eocontrol.EOKeyGlobalID;

public class GUIDpresenter extends WOComponent {
    public GUIDpresenter(WOContext context) {
        super(context);
    }
    
    public EOEnterpriseObject student;
    
    public String guid() {
    	EOEditingContext ec = student.editingContext();
		EOKeyGlobalID gid = (EOKeyGlobalID)ec.globalIDForObject(student);
		Integer id = (Integer)gid.keyValues()[0];
    	SyncMatch match = SyncMatch.getMatch(null, ExtBase.localBase(ec), student.entityName(),id);
    	if(match == null || match.extID() == null)
    		return null;
    	return match.extID().toUpperCase();
    }
}