// SyncGenerator.java

/*
 * Copyright (c) 2008, Gennady & Michael Kushnir
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 * 
 * 	•	Redistributions of source code must retain the above copyright notice, this
 * 		list of conditions and the following disclaimer.
 * 	•	Redistributions in binary form must reproduce the above copyright notice,
 * 		this list of conditions and the following disclaimer in the documentation
 * 		and/or other materials provided with the distribution.
 * 	•	Neither the name of the RUJEL nor the names of its contributors may be used
 * 		to endorse or promote products derived from this software without specific 
 * 		prior written permission.
 * 		
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT
 * SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
 * TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
 * BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY
 * WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package net.rujel.io;

import java.util.Enumeration;

import org.xml.sax.SAXException;

import com.webobjects.eoaccess.EOUtilities;
import com.webobjects.eocontrol.EOEditingContext;
import com.webobjects.eocontrol.EOEnterpriseObject;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSDictionary;
import com.webobjects.foundation.NSKeyValueCoding;
import com.webobjects.foundation.NSMutableArray;
import com.webobjects.foundation.NSMutableDictionary;
import com.webobjects.foundation.NSMutableSet;

import net.rujel.base.SettingsBase;
import net.rujel.reusables.DisplayAny;
import net.rujel.reusables.Various;
import net.rujel.reusables.xml.EasyGenerationContentHandlerProxy;
import net.rujel.reusables.xml.GeneratorModule;

public class SyncGenerator extends GeneratorModule {

	public SyncGenerator(NSDictionary options) {
		super(options);
	}
	
	private NSMutableDictionary preload = new NSMutableDictionary();
	protected NSMutableDictionary sources = new NSMutableDictionary();

	@Override
	public void generateFor(Object object,
			EasyGenerationContentHandlerProxy handler) throws SAXException {
		String tag = handler.recentElement();
		boolean inSync = tag.equals("syncdata");
		if(inSync) {
			String path = handler.recentPath();
			int fin = path.length() - 9;
			int idx = path.lastIndexOf('/',fin);
			tag = path.substring(idx, fin);
		}
		Enumeration enu;
		NSMutableArray loaded = (NSMutableArray)preload.valueForKey(tag);
		if(loaded != null && loaded.count() > 0) {
			enu = loaded.objectEnumerator();
		} else {
			NSMutableSet prepared = (NSMutableSet)sources.valueForKey(tag);
			if(prepared == null)
				prepared = prepareSources(tag);
			if(prepared == null || prepared.count() == 0)
				return;
			enu = prepared.objectEnumerator();
		}
		if(!inSync)
			handler.startElement("syncdata");
		while (enu.hasMoreElements()) {
			NSKeyValueCoding pre = (NSKeyValueCoding) enu.nextElement();
			if(pre instanceof ExtBase) {
				ExtBase sys = (ExtBase)pre;
				if(sys.isLocalBase())
					handler.prepareAttribute("product", "GUID");
				else
					handler.prepareAttribute("product", sys.extSystem().productName());
				handler.prepareAttribute("base", sys.baseID());
				String extID = ((ExtBase)pre).extidForObject((EOEnterpriseObject)object);
				handler.element("extid", extID);
			}
			if(pre instanceof ExtSystem) {
				ExtSystem sys = (ExtSystem)pre;
				handler.prepareAttribute("product", sys.productName());
				handler.prepareAttribute("base", sys.productName());
				String extID = ((ExtSystem)pre).extidForObject((EOEnterpriseObject)object, null);
				handler.element("extid", extID);
			}
			String param = (String)pre.valueForKey("baseID");
			if(param != null) {
				handler.prepareAttribute("product", (String) pre.valueForKey("product"));
				handler.prepareAttribute("base", param);
				param  = (String)((NSDictionary)pre).objectForKey(object);
				if(param == null)
					handler.dropAttributes();
				else
					handler.element("extid", param);
				continue;
			}
			param = (String)pre.valueForKey("paramKey");
			if(param == null)
				continue;
			handler.prepareAttribute("key", param);
			if(Various.boolForObject(pre.valueForKey("PRELOADED"))) {
				param  = (String)((NSDictionary)pre).objectForKey(object);
			} else {
				param = readDict((NSDictionary)pre, object);
			}
			if(param == null)
				handler.dropAttributes();
			else
				handler.element("param", param);
		}
		if(!inSync)
			handler.endElement("syncdata");
	}
	
	protected String readDict(NSDictionary dict, Object obj) {
		String param = (String)dict.valueForKey("settingsBase");
		if(param != null) {
			EOEditingContext ec = (EOEditingContext)settings.valueForKey("ec");
			SettingsBase base = SettingsBase.baseForKey(param, ec, false);
			Object ref = dict.valueForKey("forObject");
			if(ref == null)
				ref = obj;
			else
				ref = DisplayAny.ValueReader.evaluateValue(ref, obj, null);
			EOEnterpriseObject setting = base.forObject(ref);
			if(setting == null)
				return null;
			if(Various.boolForObject(dict.valueForKey("numeric"))) {
				Integer num = (Integer)setting.valueForKey(SettingsBase.NUMERIC_VALUE_KEY);
				return (num == null)? null : num.toString();
			}
			return (String)setting.valueForKey(SettingsBase.TEXT_VALUE_KEY);
		}
		Object result = DisplayAny.ValueReader.evaluateDict(dict, obj, null);
		if(result == null)
			return null;
		return result.toString();
	}

	public void preload(String forTag, NSArray objects) {
		NSMutableSet prepared = (NSMutableSet)sources.valueForKey(forTag);
		if(prepared == null)
			prepared = prepareSources(forTag);
		if(prepared == null || prepared.count() == 0)
			return;
		Enumeration enu = prepared.objectEnumerator();
		NSMutableArray loaded = new NSMutableArray();
		while (enu.hasMoreElements()) {
			NSKeyValueCoding pre = (NSKeyValueCoding) enu.nextElement();
			if(pre instanceof ExtBase) {
				ExtBase sys = (ExtBase)pre;
				NSMutableDictionary dict = sys.dictForObjects(objects);
				if(dict == null || dict.count() == 0)
					continue;
				dict.takeValueForKey(sys.baseID(), "baseID");
				if(sys.isLocalBase())
					dict.takeValueForKey("GUID", "product");
				else
					dict.takeValueForKey(sys.extSystem().productName(), "product");
				loaded.addObject(dict);
				continue;
			}
			if(pre instanceof ExtSystem) {
				ExtSystem sys = (ExtSystem)pre;
				NSMutableDictionary dict = sys.dictForObjects(objects, null);
				if(dict == null || dict.count() == 0)
					continue;
				dict.takeValueForKey(sys.productName(), "product");
				loaded.addObject(dict);
				continue;
			}
			String param = (String)pre.valueForKey("settingsBase");
			if(param == null) {
				loaded.addObject(pre);
				continue;
			}
			NSMutableDictionary dict = new NSMutableDictionary();
			dict.takeValueForKey(pre.valueForKey("paramKey"), "paramKey");
			EOEditingContext ec = (EOEditingContext)settings.valueForKey("ec");
			SettingsBase base = SettingsBase.baseForKey(param, ec, false);
			Object ref = pre.valueForKey("forObject");
			Enumeration oen = objects.objectEnumerator();
			while (oen.hasMoreElements()) {
				Object object = oen.nextElement();
				Object obj = (ref==null) ? object :
					DisplayAny.ValueReader.evaluateValue(ref, object, null);
				EOEnterpriseObject setting = base.forObject(obj);
				if(setting == null)
					continue;
				if(Various.boolForObject(dict.valueForKey("numeric"))) {
					Integer num = (Integer)setting.valueForKey(SettingsBase.NUMERIC_VALUE_KEY);
					if(num != null)
						dict.setObjectForKey(num.toString(), object);
				} else {
					String res = (String)setting.valueForKey(SettingsBase.TEXT_VALUE_KEY);
					if(res != null)
						dict.setObjectForKey(res, object);
				}
			}
			dict.takeValueForKey(Boolean.TRUE, "PRELOADED");
			loaded.addObject(dict);
		}
		preload.takeValueForKey(loaded, forTag);
	}

	public void unload(String forTag) {
		if(forTag == null)
			preload.removeAllObjects();
		else
			preload.removeObjectForKey(forTag);
	}

	public NSMutableSet prepareSources(String forTag) {
		Object plistData = settings.valueForKeyPath("reporter.sync." + forTag);
		NSMutableSet prepared = new NSMutableSet();
		if(plistData == null)
			return prepared;
		interpretSync(plistData,prepared);
		sources.setObjectForKey(prepared, forTag);
		return prepared;
	}
	
	protected void interpretSync(Object plistData, NSMutableSet prepared) {
		if(plistData instanceof NSArray) {
			Enumeration enu = ((NSArray)plistData).objectEnumerator();
			while (enu.hasMoreElements()) {
				Object object = enu.nextElement();
				interpretSync(object,prepared);
			}
			return;
		}
		EOEditingContext ec = (EOEditingContext)settings.valueForKey("ec");
		if(ec == null)
			ec = new EOEditingContext();
		if("GUID".equals(plistData)) {
			prepared.addObject(ExtBase.localBase(ec));
		} else if(plistData instanceof String) {
			if(((String) plistData).charAt(0) == '@') {
				interpretSync(settings.valueForKey(((String)plistData).substring(1)), prepared);
			} else {
				NSArray found = EOUtilities.objectsMatchingKeyAndValue(ec, 
						"ExtBase", "baseID", plistData);
				if(found != null && found.count() > 0)
					prepared.addObject(found.objectAtIndex(0));
			}
		} else if(plistData instanceof NSDictionary) {
			prepared.addObject(plistData);
		}
	}
	
}
