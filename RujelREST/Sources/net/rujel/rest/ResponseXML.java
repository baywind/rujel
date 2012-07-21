package net.rujel.rest;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Enumeration;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import com.webobjects.foundation.NSDictionary;

import net.rujel.reusables.xml.AbstractObjectReader;
import net.rujel.reusables.xml.TransormationErrorListener;

public class ResponseXML extends AbstractObjectReader{

	public static byte[] generate(ReportSource input) throws TransformerException {
		TransformerFactory factory = TransformerFactory.newInstance();
		factory.setErrorListener(new TransormationErrorListener(null));
		Transformer transformer = factory.newTransformer();
		transformer.setOutputProperty("indent", "yes");
		transformer.setOutputProperty("{http://xml.apache.org/xalan}indent-amount", "4");
		XMLReader reader = new ResponseXML();
        Source src = new SAXSource(reader,input);
    	ByteArrayOutputStream out = new ByteArrayOutputStream();
    	Result res = new StreamResult(out);
      	transformer.transform(src, res);
    	return out.toByteArray();
	}
	
	@Override
	public void parse(InputSource input) throws IOException, SAXException {
        if (input instanceof ReportSource) {
            parse((ReportSource)input);
        } else {
            throw new SAXException("Unsupported InputSource specified. "
                    + "Must be a ReportSource");
        }
	}

	public void parse(ReportSource in) throws IOException, SAXException {
	      if (handler == null) {
	            throw new IllegalStateException("ContentHandler not set");
	        }
	        handler.startDocument();
	        handler.prepareAttribute("entity", in.entity);
	        handler.startElement("response");
	        if(in.attributes != null && in.attributes.count() > 0) {
	        	Enumeration enu = in.attributes.keyEnumerator();
	        	while (enu.hasMoreElements()) {
					String att = (String) enu.nextElement();
					handler.prepareAttribute("attribute", att);
					handler.element("param", in.attributes.valueForKey(att).toString());
				}
	        }
	        if(in.rows != null && in.rows.count() > 0) {
	        	Enumeration enu = in.rows.objectEnumerator();
	        	while (enu.hasMoreElements()) {
					NSDictionary row = (NSDictionary) enu.nextElement();
					if(in.level != null)
						handler.prepareAttribute("stage", in.level.toString());
					handler.startElement("grouping");
					if(in.groupings != null) {
						for (int i = 0; i < in.groupings.length; i++) {
							String key = in.groupings[i];
							Object value = row.valueForKey(key);
							if(value == null)
								continue;
							handler.prepareAttribute("name", key);
							handler.element("attribute", value.toString());
						}
					}
					if(in.agregates != null) {
						for (int i = 0; i < in.agregates.length; i++) {
							String key = in.agregates[i];
							Agregator value = (Agregator)row.valueForKey(key);
							if(value == null)
								continue;
							handler.prepareAttribute("name", key);
							handler.prepareAttribute("type",value.getType());
							if(value.getAttribute() != null)
								handler.prepareAttribute("attribute",value.getAttribute());
							handler.element("agregate", value.toString());
						}
					}
					handler.endElement("grouping");
				}
	        }
	        handler.endElement("response");
	        handler.endDocument();
	}
}
