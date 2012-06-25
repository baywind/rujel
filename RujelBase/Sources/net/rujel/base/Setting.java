package net.rujel.base;

import com.webobjects.eocontrol.EOEnterpriseObject;

public interface Setting extends EOEnterpriseObject {
	public Integer numericValue();
	public String textValue();
}
