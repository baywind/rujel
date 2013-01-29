package net.rujel.markarchive;

import net.rujel.reusables.SessionedEditingContext;

import com.webobjects.appserver.WOContext;
import com.webobjects.appserver.WOComponent;
import com.webobjects.eoaccess.EOUtilities;
import com.webobjects.eocontrol.EOAndQualifier;
import com.webobjects.eocontrol.EOEditingContext;
import com.webobjects.eocontrol.EOEnterpriseObject;
import com.webobjects.eocontrol.EOFetchSpecification;
import com.webobjects.eocontrol.EOKeyValueQualifier;
import com.webobjects.eocontrol.EOQualifier;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSDictionary;
import com.webobjects.foundation.NSLocking;
import com.webobjects.foundation.NSMutableArray;
import com.webobjects.foundation.NSTimestamp;

public class ReadArchives extends WOComponent {
	
	public NSArray records;
	public Object item;
	public NSMutableArray entities;
	public NSDictionary currEntity;
	
	public EOEditingContext ec;
	public NSTimestamp since;
	public NSTimestamp to;
	public Integer level;
	
    public ReadArchives(WOContext context) {
        super(context);
		ec = new SessionedEditingContext(session());
		to = (NSTimestamp)session().valueForKey("today");
		since = to.timestampByAddingGregorianUnits(0, 0, -7, 0, 0, 0);
		if(System.currentTimeMillis() - to.getTime() < NSLocking.OneDay)
			to = null;
		level = new Integer(2);
		select();
		entities = (NSMutableArray)session().valueForKeyPath("modules.archiveType");
		if(entities == null || entities.count() == 0)
			return;
		NSArray used = EOUtilities.objectsForEntityNamed(ec, "UsedEntity");
		if(used == null || used.count() == 0)
			return;
		for (int j = 0; j < entities.count(); j++) {
			NSDictionary ent = (NSDictionary) entities.objectAtIndex(j);
			String name = (String)ent.valueForKey("entityName");
			if(name == null)
				continue;
			for (int i = 0; i < used.count(); i++) {
				EOEnterpriseObject ue = (EOEnterpriseObject)used.objectAtIndex(i);
				if(name.equals(ue.valueForKey("usedEntity"))) {
					ent = ent.mutableClone();
					ent.takeValueForKey(ue, "usedEntity");
					entities.replaceObjectAtIndex(ent, j);
					break;
				}
			}
		}
		
    }
    
    public void select() {
		EOQualifier qual[] = new EOQualifier[4];
		qual[0] = new EOKeyValueQualifier(MarkArchive.ACTION_TYPE_KEY, 
				EOQualifier.QualifierOperatorGreaterThanOrEqualTo, level);
		qual[1] = new EOKeyValueQualifier(MarkArchive.TIMESTAMP_KEY, 
				EOQualifier.QualifierOperatorGreaterThanOrEqualTo, since);
		if(to != null) {
			qual[2] = new EOKeyValueQualifier(MarkArchive.TIMESTAMP_KEY, 
					EOQualifier.QualifierOperatorLessThanOrEqualTo, to);
		}
		if(currEntity != null && currEntity.valueForKey("usedEntity") != null) {
			qual[3]  = new EOKeyValueQualifier(MarkArchive.USED_ENTITY_KEY, 
					EOQualifier.QualifierOperatorEqual, currEntity.valueForKey("usedEntity"));
		}
		qual[0] = new EOAndQualifier(new NSArray(qual));
		EOFetchSpecification fs = new EOFetchSpecification(MarkArchive.ENTITY_NAME,qual[0],
				MarkArchive.backSorter);
		records = ec.objectsWithFetchSpecification(fs);
    }
    
    public String rowClass() {
    	if (item instanceof MarkArchive) {
			MarkArchive ma = (MarkArchive) item;
			switch (ma.actionType()) {
			case 1:
				return "green";
			case 2:
				return "gerade";
			case 3:
				return "female";
			default:
				return "grey";
			}
		}
    	return null;
    }
}