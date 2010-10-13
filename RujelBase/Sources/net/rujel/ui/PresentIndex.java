package net.rujel.ui;

import com.webobjects.appserver.WOApplication;
import com.webobjects.appserver.WOContext;
import com.webobjects.appserver.WOElement;
import com.webobjects.appserver.WOResponse;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSDictionary;

import net.rujel.base.IndexRow;
import net.rujel.base.Indexer;
import net.rujel.reusables.ExtDynamicElement;
import net.rujel.reusables.Various;

public class PresentIndex extends ExtDynamicElement {
	
	public static final NSArray mediaTypes = new NSArray(
			new String[] {"text","color","image","media"});

    public PresentIndex(String name, NSDictionary associations,
			WOElement template) {
		super(name, associations, template);

		checkRequired(associations, "indexer");
		checkRequired(associations, "value");
	}
    
	public void appendToResponse(WOResponse aResponse, WOContext aContext) {
		Indexer indexer = (Indexer)valueForBinding("indexer", aContext);
		Number value = (Number)valueForBinding("value", aContext);
		IndexRow row = null;
		if(value != null && indexer != null) {
			row = indexer.rowForIndex(value.intValue(), null);
		}
		
		String present = null;
		if(row != null)
			present = row.value();
		else if(indexer != null)
			present = indexer.defaultValue();
		else if(value != null)
			present = value.toString();
		int type = 0;
		if (indexer != null) {
			String indexType =indexer.indexType();
			int dot = indexType.indexOf(':');
			if(dot > 0) {
				indexType = indexType.substring(dot +1);
				type = mediaTypes.indexOfObject(indexType);
			}
		}
		if(indexer != null && present != null && type < 2) {
			String format = indexer.formatString();
			if(format != null) {
				present = String.format(format, value, present, (row==null)?null:row.comment());
			}
		}
		boolean canEdit = Various.boolForObject(valueForBinding("canEdit", aContext));
		if(canEdit) {
			aResponse.appendContentString("<input type = \"text\" name=\"");
			String name = (String)valueForBinding("name", aContext);
			if(name == null)
				name = aContext.elementID();
			aResponse.appendContentHTMLAttributeValue(name);
			aResponse.appendContentCharacter('"');
			if(Various.boolForObject(valueForBinding("disabled", aContext)))
				aResponse.appendContentString(" disabled = \"disabled\"");
			if(present != null) {
				aResponse.appendContentString(" value = \"");
				if(type == 0)
					aResponse.appendContentHTMLAttributeValue(present);
				else
					aResponse.appendContentString(value.toString());
				aResponse.appendContentCharacter('"');
			}
			aResponse.appendContentString(" style = \"text-align:center;");
			Object tmp = valueForBinding("height", aContext);
			if(tmp != null) {
				aResponse.appendContentString("height:");
				aResponse.appendContentHTMLAttributeValue(tmp.toString());
				aResponse.appendContentCharacter(';');
			}
			tmp = valueForBinding("width", aContext);
			if(tmp != null) {
				aResponse.appendContentString("width:");
				aResponse.appendContentHTMLAttributeValue(tmp.toString());
				aResponse.appendContentCharacter(';');
			}
			if(present != null) {
				if(type == 1) {
					aResponse.appendContentString("background-color:");
					aResponse.appendContentHTMLAttributeValue(present);
					aResponse.appendContentCharacter(';');
				} else if(type == 2) {
					aResponse.appendContentString("background-image:url('");
					if(aContext.doesGenerateCompleteURLs())
						aResponse.appendContentHTMLAttributeValue(
								(String)WOApplication.application().valueForKey("serverUrl"));
					String common = indexer.commonString();
					if(common != null) {
						aResponse.appendContentHTMLAttributeValue(common);
						if(common.charAt(common.length() -1) != '/')
							aResponse.appendContentCharacter('/');
					}
					aResponse.appendContentHTMLAttributeValue(present);
					aResponse.appendContentString(
							"');background-position:center;background-repeat:no-repeat;");
				}
			}
			tmp = valueForBinding("style", aContext);
			if(tmp != null) {
				aResponse.appendContentHTMLAttributeValue(tmp.toString());
			}
			aResponse.appendContentCharacter('"');
			aResponse.appendContentCharacter('/');
			aResponse.appendContentCharacter('>');			
		} // canEdit
		else {
			if(present == null) {
				if(value != null)
					aResponse.appendContentHTMLString(value.toString());
				return;
			}
			if(type == 0) { //text
				boolean span = (row != null && row.comment() != null);
				if(span) {
					aResponse.appendContentString("<span title=\"");
					aResponse.appendContentHTMLAttributeValue(row.comment());
					aResponse.appendContentCharacter('"');
					aResponse.appendContentCharacter('>');			
				}
				aResponse.appendContentHTMLString(present);
				if(span) {
					aResponse.appendContentString("</span>");
				}
			} else if (type == 1) { // color
				aResponse.appendContentString("<div style = \"");
				Object tmp = valueForBinding("height", aContext);
				if(tmp != null) {
					aResponse.appendContentString("height:");
					aResponse.appendContentHTMLAttributeValue(tmp.toString());
					aResponse.appendContentCharacter(';');
				}
				tmp = valueForBinding("width", aContext);
				if(tmp != null) {
					aResponse.appendContentString("width:");
					aResponse.appendContentHTMLAttributeValue(tmp.toString());
					aResponse.appendContentCharacter(';');
				}
				aResponse.appendContentString("background-color:");
				aResponse.appendContentHTMLAttributeValue(present);
				aResponse.appendContentCharacter(';');
				tmp = valueForBinding("style", aContext);
				if(tmp != null) {
					aResponse.appendContentHTMLAttributeValue(tmp.toString());
				}
				if(row.comment() != null) {
					aResponse.appendContentString("\" title = \"");
					aResponse.appendContentHTMLAttributeValue(row.comment());
				}
				aResponse.appendContentCharacter('"');
				aResponse.appendContentCharacter('>');
				super.appendToResponse(aResponse, aContext);
				aResponse.appendContentString("</div>");
			} else if (type == 3) { // image
				aResponse.appendContentString("<img src = \"");
				if(aContext.doesGenerateCompleteURLs())
					aResponse.appendContentHTMLAttributeValue(
							(String)WOApplication.application().valueForKey("serverUrl"));
				String common = indexer.commonString();
				if(common != null) {
					aResponse.appendContentHTMLAttributeValue(common);
					if(common.charAt(common.length() -1) != '/')
						aResponse.appendContentCharacter('/');
				}
				aResponse.appendContentHTMLAttributeValue(present);
				aResponse.appendContentString(" alt =\"");
				aResponse.appendContentHTMLAttributeValue((value==null)?".":value.toString());
				if(row.comment() != null) {
					aResponse.appendContentString("\" title = \"");
					aResponse.appendContentHTMLAttributeValue(row.comment());
				}
				Object tmp = valueForBinding("height", aContext);
				if(tmp != null) {
					aResponse.appendContentString("\" height = \"");
					aResponse.appendContentHTMLAttributeValue(tmp.toString());
				}
				tmp = valueForBinding("width", aContext);
				if(tmp != null) {
					aResponse.appendContentString("\" width = \"");
					aResponse.appendContentHTMLAttributeValue(tmp.toString());
				}
				aResponse.appendContentCharacter('"');
				aResponse.appendContentCharacter('/');
				aResponse.appendContentCharacter('>');
			}
		}
	}
}
