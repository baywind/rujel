package net.rujel.criterial;

import org.xml.sax.SAXException;

import com.webobjects.foundation.NSDictionary;

import net.rujel.reusables.xml.EasyGenerationContentHandlerProxy;
import net.rujel.reusables.xml.GeneratorModule;

public class CriterialXML extends GeneratorModule {

	public CriterialXML(NSDictionary options) {
		super(options);
	}
	
	public Integer sort() {
		return new Integer(20);
	}
	
	public void generateFor(Object object,
			EasyGenerationContentHandlerProxy handler) throws SAXException {
		

	}

}
