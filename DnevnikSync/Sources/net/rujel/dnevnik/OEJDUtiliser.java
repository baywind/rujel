package net.rujel.dnevnik;

import java.lang.reflect.Method;

import com.webobjects.foundation.NSArray;

import net.rujel.contacts.Contact;
import net.rujel.contacts.EMailUtiliser;
import net.rujel.contacts.Contact.Utiliser;
import net.rujel.reusables.Flags;
import net.rujel.reusables.NamedFlags;

public class OEJDUtiliser implements Utiliser {

	public static final String presenter = null;
	public static final NSArray flagsKeys = new NSArray (
			new String[] {"active"});

	
	private Contact _contact;
	private NamedFlags _flags;
	
	public OEJDUtiliser(Contact contact) throws NoSuchMethodException, SecurityException {
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
	
	public String validateContact(Object aValue) {
		if(aValue == null)
			return null;
		return aValue.toString();
	}

	public String present() {
		String cnt = _contact.contact();
		if(cnt == null)
			cnt = "&bull;";
		if(!flags().getFlag(0)) {
			StringBuffer buf = new StringBuffer("<span style=\"color:#aaaaaa;\">");
			buf.append(cnt);
			buf.append("</span>");
			return buf.toString();
		}
		return cnt;
	}

	public void reset() {
		_flags.setFlags(_contact.flags().intValue());
	}

}
