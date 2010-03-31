// EMailUtiliser.java

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

package net.rujel.contacts;

import net.rujel.reusables.*;
import net.rujel.base.MyUtility;
//import er.javamail.*;

import com.webobjects.foundation.*;
import com.webobjects.eocontrol.*;
import com.webobjects.eoaccess.EOObjectNotAvailableException;
import com.webobjects.eoaccess.EOUtilities;

import java.lang.reflect.Method;
import java.util.Enumeration;
import java.util.logging.Logger;
import net.rujel.reusables.WOLogLevel;

import javax.mail.Address;
import javax.mail.internet.InternetAddress;

public class EMailUtiliser implements Contact.Utiliser {
	public static final String presenter = "EMailPresenter";
	public static final NSArray flagsKeys = new NSArray (
			new String[] {"subscribe","zip","-4-","-8-","-16-","disabled"});
	/*
	static {
		ERJavaMail.sharedInstance().finishInitialization();
	}
	*/
	protected static Logger logger = Logger.getLogger("rujel.contacts");
	private Contact _contact;
	private NamedFlags _flags;
	private InternetAddress _email;
	
	public EMailUtiliser(Contact contact) throws NoSuchMethodException, SecurityException {
		_contact = contact;
		_flags = new NamedFlags(_contact.flags().intValue(),flagsKeys);
		Method syncMethod = EMailUtiliser.class.getMethod("_syncFlags",Flags.class);
		_flags.setSyncParams(this,syncMethod);
	}
	
	public void _syncFlags(Flags flags) {
		if(_contact.flags() == null || _contact.flags().intValue() != flags.intValue())
			_contact.setFlags(flags.toInteger());
	}
	
	public Contact contact() {
		return _contact;
	}
	public NamedFlags flags() {
		return _flags;
	}
	public String presenter() {
		return presenter;
	}
	
	public InternetAddress email() {
		if(_email == null) {
			String con = contact().contact();
			if(con == null)
				return null;
			try {
				_email = new InternetAddress(con);
			} catch (Exception ex) {
				logger.log(WOLogLevel.WARNING,"Error parsing email address '" + contact().contact() + '\'',new Object[] {contact(),ex});
			}
					}
		return _email;
	}
	
	public String address() {
		if(email() == null)
			return null;
		return email().getAddress();
	}
	
	public void setAddress(String address) {
		if(email() == null) {
			_email = new InternetAddress();
		}
		//if(!address.equals(email().getAddress())) {
			 email().setAddress(address);
			contact().setContact(email().toString());
		//}
	}
	
	public String name() {
		if(email() == null)
			return null;
		return email().getPersonal();
	}
	
	public void setName(String name) {
		if(email() == null) {
			_email = new InternetAddress();
		}
		//if(!name.equals(email().getPersonal())) {
			try {
				email().setPersonal(name,"utf8");
			} catch (Exception ex) {
				logger.log(WOLogLevel.INFO,"Error encoding personal name '" + name + '\'',ex);
			}
			contact().setContact(email().toString());
		//}
	}
	
	public String validateContact(Object aValue) {
		if(aValue instanceof Address)
			return aValue.toString();
		if(!(aValue instanceof String))
			throw new NSValidation.ValidationException("Attribute for Contact of type e-mail should be String or javax.mail.Address",aValue,"contact");
		try {
			javax.mail.Address mailAdress = new javax.mail.internet.InternetAddress((String)aValue,true);
			return mailAdress.toString();
		} catch (javax.mail.internet.AddressException aex) {
			String message = MyUtility.stringForPath("Strings.messages.illegalFormat");
			if(message == null) message = "Illegal format of";
			message = message + " e-mail : '" + aValue + "' - " + aex.getMessage();
			logger.log(WOLogLevel.FINER,"Failed to format address " + aValue,aex);
			throw new NSValidation.ValidationException(message,aValue,"contact");
		}
	}
	
	public String present() {
		if(!flags().getFlag(0) || flags().flagForKey("disabled")) {
			StringBuffer buf = new StringBuffer("<span style=\"color:#aaaaaa;\">");
			buf.append(address());
			buf.append("</span>");
			return buf.toString();
		}
		return  address();
	}
	

	
	public static EOEnterpriseObject conType(EOEditingContext ec) {
		String className = EMailUtiliser.class.getName();
		EOEnterpriseObject result = null;
		try {
			result = EOUtilities.objectMatchingKeyAndValue(ec,"ConType","utiliserClass",className);
		} catch (EOObjectNotAvailableException e) {
			result = EOUtilities.createAndInsertInstance(ec, "ConType");
			result.takeValueForKey(className, "utiliserClass");
			result.takeValueForKey("e-meil", "type");
			logger.log(WOLogLevel.WARNING, "Not found conact type for " + className
					+ ". Creating new one.");
		} catch (EOUtilities.MoreThanOneException e) {
			logger.log(WOLogLevel.WARNING, "found multiple conact types for " + className
					+ ". Selecting first one.");
			NSArray results = EOUtilities.objectsMatchingKeyAndValue(ec,"ConType","utiliserClass",className);
			result = (EOEnterpriseObject)results.objectAtIndex(0);
		}
		return result;
	}
	public static InternetAddress[] toAdressesFromContacts(NSSet contacts, boolean ignoreFlags) {
		if(contacts == null || contacts.count() == 0) return null;
		return toAdressesFromContacts(contacts.objectEnumerator(), ignoreFlags);
	}
	
	public static InternetAddress[] toAdressesFromContacts(NSArray contacts, boolean ignoreFlags) {
		if(contacts == null || contacts.count() == 0) return null;
		return toAdressesFromContacts(contacts.objectEnumerator(), ignoreFlags);
	}

	public static InternetAddress[] toAdressesFromContacts(Enumeration contacts, boolean ignoreFlags) {
		NSMutableArray<InternetAddress> toAdresses = new NSMutableArray<InternetAddress> ();
		String className = EMailUtiliser.class.getName();
		while (contacts.hasMoreElements()) {
			Contact con = (Contact)contacts.nextElement();
			String tmp = (String)con.type().valueForKey("utiliserClass");
			if(tmp == null || !tmp.equals(className))
				continue;
			if(!ignoreFlags) {
				int fl = con.flags().intValue();
				if((fl & 1) == 0 || (fl & 32) != 0)
					continue;
			}
			toAdresses.addObject(((EMailUtiliser)con.getUtiliser()).email());
		}
		InternetAddress[] result = new InternetAddress[toAdresses.count()];
		for (int i = 0; i < result.length; i++) {
			result[i] = (InternetAddress)toAdresses.objectAtIndex(i);
		}
		return result;
	}
	
	public void reset() {
		_email = null;
		_flags.setFlags(_contact.flags().intValue());
	}
}
