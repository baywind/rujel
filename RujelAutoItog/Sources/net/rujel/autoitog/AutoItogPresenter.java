package net.rujel.autoitog;

import java.text.SimpleDateFormat;

import net.rujel.eduresults.ItogContainer;
import net.rujel.reusables.ExtDynamicElement;
import net.rujel.reusables.SettingsReader;
import net.rujel.reusables.Various;

import com.webobjects.appserver.*;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSDictionary;

// Generated by the WOLips Templateengine Plug-in at Sep 11, 2009 11:25:13 AM
public class AutoItogPresenter extends ExtDynamicElement {

	public AutoItogPresenter(String name, NSDictionary associations, WOElement template) {
		super(name, associations, template);
	}

	public void appendToResponse(WOResponse aResponse, WOContext aContext) {
		ItogContainer itog = (ItogContainer)valueForBinding("value",aContext);
		if(itog == null) {
			aResponse.appendContentString((String)aContext.session().valueForKeyPath(
					"strings.RujelAutoItog_AutoItog.properties.AutoItog.this"));
			return;
		}
		String listName = (String)valueForBinding("listName", aContext);
		AutoItog autoItog = AutoItog.forListName(listName, itog);
		aResponse.appendContentString("<span");
		if(autoItog != null || Various.boolForObject(aContext.session().valueForKeyPath(
				"readAccess.create.AutoItog"))) {
			aResponse.appendContentString(" onclick = \"ajaxPopupAction('");
			aResponse.appendContentString(aContext.componentActionURL());
			aResponse.appendContentString(
				"',event);\" onmouseover = \"dim(this);\" onmouseout = \"unDim(this)\"");
		}
		if(autoItog == null) {
			aResponse.appendContentString(" style = \"font-style:italic;\"");
			aResponse.appendContentCharacter('>');
			aResponse.appendContentString((String)aContext.session().valueForKeyPath(
					"strings.RujelAutoItog_AutoItog.none"));
//			return;
		} else {
			boolean disabled = (autoItog.namedFlags().flagForKey("inactive"));
			if(disabled)
				aResponse.appendContentString(" class = \"dimtext\"");
			aResponse.appendContentCharacter('>');
			String pattern = SettingsReader.stringForKeyPath("ui.shortDateFormat","MMM-dd");
			SimpleDateFormat format = new SimpleDateFormat(pattern);
			aResponse.appendContentString(format.format(autoItog.fireDate()));
			aResponse.appendContentCharacter(' ');
			format = new SimpleDateFormat("HH:mm");
			aResponse.appendContentString(format.format(autoItog.fireTime()));
			aResponse.appendContentCharacter(':');
			aResponse.appendContentCharacter(' ');
			pattern = autoItog.calculatorName();
			if(pattern != null) { // resolve className to readable title
				NSArray calculators = (NSArray)aContext.session().valueForKeyPath(
					"strings.RujelAutoItog_AutoItog.calculators");
				for (int i = 0; i < calculators.count(); i++) {
					NSDictionary calc = (NSDictionary)calculators.objectAtIndex(i);
					if(pattern.equals(calc.valueForKey("className"))) {
						aResponse.appendContentString((String)calc.valueForKey("title"));
						break;
					}
				}
			}
			if(autoItog.namedFlags().flagForKey("manual")) {
				if(pattern != null)
					aResponse.appendContentCharacter('/');
				aResponse.appendContentString((String)aContext.session().valueForKeyPath(
					"strings.RujelAutoItog_AutoItog.manual"));
			}
		} // autoItog != null
		aResponse.appendContentString("</span>");
	}

	protected WOActionResults action(WOContext aContext) {
		ItogContainer itog = (ItogContainer)valueForBinding("value",aContext);
		if(itog == null)
			return null;
		WOComponent popup = WOApplication.application().pageWithName(
				"AutoItogEditor", aContext);
		popup.takeValueForKey(aContext.page(), "returnPage");
		popup.takeValueForKey(itog, "itog");
		String listName = (String)valueForBinding("listName", aContext);
		popup.takeValueForKey(listName, "listName");
		AutoItog autoItog = AutoItog.forListName(listName, itog);
//		if(autoItog != null) {
			popup.takeValueForKey(autoItog, "autoItog");
//		}
		return popup;
	}
}