package net.rujel.complete;

import java.util.logging.Logger;

import net.rujel.base.SettingsBase;
import net.rujel.interfaces.EduGroup;
import net.rujel.interfaces.Student;
import net.rujel.reusables.NamedFlags;
import net.rujel.reusables.Various;
import net.rujel.reusables.WOLogLevel;

import com.webobjects.appserver.WOActionResults;
import com.webobjects.appserver.WOContext;
import com.webobjects.appserver.WOComponent;
import com.webobjects.eoaccess.EOUtilities;
import com.webobjects.eocontrol.EOEditingContext;
import com.webobjects.eocontrol.EOFetchSpecification;
import com.webobjects.eocontrol.EOQualifier;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSDictionary;
import com.webobjects.foundation.NSMutableDictionary;

public class DecisionInspector extends WOComponent {
	
	public WOComponent returnPage;
	public Student student;
	public EduGroup eduGroup;
	public PedDecision pedDecision;
	public String title;
	public String dfltDecision;
	public String decision;
	public int state = 0;
	public NSMutableDictionary disable;
	
    public DecisionInspector(WOContext context) {
        super(context);
        disable = new NSMutableDictionary(5);
    }

    public void setStudent(Student set) {
    	student = set;
    	if(set == null) {
    		pedDecision = null;
    		return;
    	}
    	NamedFlags access = (NamedFlags)session().valueForKeyPath("readAccess.FLAGS.PedDecision");
    	NSArray found = EOUtilities.objectsMatchingKeyAndValue(student.editingContext(),
    			PedDecision.ENTITY_NAME, "student", student);
    	if(found == null || found.count() == 0) {
    		if(access.flagForKey("create")) {
    			if(!access.flagForKey("edit"))
        			disable.takeValueForKey(Boolean.TRUE, "custom");
    		} else {
    			disable.takeValueForKey(Boolean.TRUE, "none");
    			disable.takeValueForKey(Boolean.TRUE, "custom");
    			disable.takeValueForKey(Boolean.TRUE, "save");
    			disable.takeValueForKey(Boolean.TRUE, "default");
    		}
			disable.takeValueForKey(Boolean.TRUE, "text");
    		return;
    	}
		if(!access.flagForKey("delete"))
			disable.takeValueForKey(Boolean.TRUE, "none");
		if(!access.flagForKey("edit")) {
			disable.takeValueForKey(Boolean.TRUE, "custom");
			disable.takeValueForKey(Boolean.TRUE, "text");
			disable.takeValueForKey(Boolean.TRUE, "save");
			disable.takeValueForKey(Boolean.TRUE, "default");
		}
    	pedDecision = (PedDecision)found.objectAtIndex(0);
    	decision = pedDecision.specDecision();
    	if(decision == null) {
    		state = 1;
			disable.takeValueForKey(Boolean.TRUE, "text");
    	} else {
    		state = 2;
    	}
    }

    public void setEduGroup(EduGroup group) {
    	eduGroup = group;
    	EOEditingContext ec = group.editingContext();
		NSDictionary crs = SettingsBase.courseDict(group);
		title = SettingsBase.stringSettingForCourse("pedsovetTitle",crs, ec);
		if(title == null)
			title = (String)session().valueForKeyPath(
					"strings.RujelComplete_Complete.pedsovetTitle");
		 dfltDecision = SettingsBase.stringSettingForCourse("pedsovetDecision",crs, ec);
		 if(dfltDecision != null && group.grade() != null) {
			 int next = group.grade().intValue();
			 next++;
			 dfltDecision = String.format(dfltDecision, new Integer(next));
		 }
    }
    
    public WOActionResults save() {
    	EOEditingContext ec = eduGroup.editingContext();
    	if(state == 0) {
    		if(pedDecision != null)
    			ec.deleteObject(pedDecision);
    	} else {
    		if(pedDecision == null) {
    			pedDecision = (PedDecision) EOUtilities.createAndInsertInstance(
    					ec, PedDecision.ENTITY_NAME);
    			pedDecision.setStudent(student);
    			pedDecision.setFlags(new Integer(PedDecision.MANUAL_FLAG));
    		}
    		if(state == 2) {
    			decision = context().request().stringFormValueForKey("customDecision");
    			if(decision == null)
    				decision = " ";
    			if(!decision.equals(pedDecision.specDecision()))
    				pedDecision.setSpecDecision(decision);
    		} else if(pedDecision.specDecision() != null) {
    			pedDecision.setSpecDecision(null);
    		}
    	}
    	if(ec.hasChanges()) {
        	Logger logger = Logger.getLogger("rujel.complete");
    		try {
				ec.saveChanges();
				if(state == 0)
					logger.log(WOLogLevel.EDITING, "Pedsovet Decision deleted for student",
							new Object[] {session(),student});
				else
					logger.log(WOLogLevel.EDITING, "Manual changes to Pedsovet Decision saved",
							new Object[] {session(),pedDecision, student});
			} catch (Exception e) {
				if(state == 0)
					logger.log(WOLogLevel.WARNING, "Error deleting Pedsovet Decision for student",
							new Object[] {session(),student, e});
				else
					logger.log(WOLogLevel.WARNING, "Error saving changes to Pedsovet Decision",
							new Object[] {session(),pedDecision, student, e});
			}
    	}
    	returnPage.ensureAwakeInContext(context());
    	return returnPage;
    }
    
	public static NSMutableDictionary dictForGroup(EduGroup gr) {
		if(gr == null)
			return null;
		EOEditingContext ec = gr.editingContext();
		NSMutableDictionary result = new NSMutableDictionary();
		NSDictionary crs = SettingsBase.courseDict(gr);
		result.takeValueForKey(
				SettingsBase.stringSettingForCourse("pedsovetTitle",crs, ec), "title");
		EOQualifier qual = Various.getEOInQualifier("student", gr.list());
		EOFetchSpecification fs = new EOFetchSpecification(PedDecision.ENTITY_NAME,qual,null);
		NSArray found = ec.objectsWithFetchSpecification(fs);
		String dflt = SettingsBase.stringSettingForCourse("pedsovetDecision",crs, ec);
		if(dflt != null && gr.grade() != null) {
			int next = gr.grade().intValue();
			next++;
			dflt = String.format(dflt, new Integer(next));
		}
		for (int i = 0; i < found.count(); i++) {
			PedDecision dec = (PedDecision)found.objectAtIndex(i);
			String decision = dec.specDecision();
			NSMutableDictionary dict = new NSMutableDictionary(2);
			if(decision == null) {
				decision = dflt;
				if(dec.flags().intValue() == PedDecision.MANUAL_FLAG)
					dict.takeValueForKey("text-decoration:underline;", "style");
			} else {
				dict.takeValueForKey("background-color:#ffff66;", "style");
			}
			dict.takeValueForKey(decision,"decision");
			
			result.setObjectForKey(dict, dec.student());
		}
		return result;
	}

}