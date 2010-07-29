package net.rujel.base;

import com.webobjects.appserver.WOApplication;
import com.webobjects.foundation.NSDictionary;

public enum Presentation {
	text,color,image,media,html,inner;
	
	public String localised(NSDictionary dict) {
		String result = (String)dict.valueForKey(toString());
		if(result == null)
			return toString();
		return result;
	}
	
	public String localised() {
		NSDictionary dict = (NSDictionary)WOApplication.application().valueForKeyPath(
				"strings.RujelBase_Base.presentation");
		return localised(dict);
	}
}
