// BorderSet.java

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

package net.rujel.criterial;


import com.webobjects.foundation.*;
import com.webobjects.eocontrol.*;
import com.webobjects.eoaccess.EOUtilities;
import java.math.*;
import java.util.Enumeration;

public class BorderSet extends _BorderSet implements FractionPresenter
{
	protected static final EOSortOrdering so = EOSortOrdering.sortOrderingWithKey(
			"least",EOSortOrdering.CompareAscending);
	
	public BorderSet() {
        super();
    }
	
    public void awakeFromInsertion(EOEditingContext ec) {
    	setValueType(new Integer(0));
    	setExclude(Boolean.FALSE);
    }

    public static FractionPresenter fractionPresenterForTitle(EOEditingContext ec, String title) {
		if(title.charAt(0)=='%') {
			if(title.length()==1) return PERCENTAGE;
			int precision = Integer.parseInt(title.substring(1));
			return new Percent(precision);
		}
		if(title.startsWith("none")) {
			if(title.length() == 4)
				return NONE;
			return new None(title.substring(4));
		}
		NSArray found =  EOUtilities.objectsMatchingKeyAndValue(ec, ENTITY_NAME, TITLE_KEY, title);
		if(found == null || found.count() == 0)
			return PERCENTAGE;
		BorderSet result = (BorderSet)found.objectAtIndex(0);
		return result;//.getPresenter();
	}
	
    private FractionPresenter _presenter;
    public FractionPresenter getPresenter() {
    	if(_presenter != null) return _presenter;
    	String useClass = useClass();
    	if(useClass == null || useClass.length() == 0) {
    		_presenter = this;
    		return this;
    	}
		try {
			Class resClass = Class.forName(useClass);
			_presenter = (FractionPresenter)resClass.getConstructor((Class[])null).newInstance();
		} catch (Exception e) {
			throw new NSForwardException(e,"Error constructing FractionPresenter with title '" 
					+ title() + "' and class '" + useClass + '\'');
		}
		return _presenter;
    }
    /*
    public void setPresenter(FractionPresenter presenter) {
    	_presenter = presenter;
    	super.setUseClass(presenter.getClass().getCanonicalName());
    }*/
    
    public void setUseClass(String useClass) {
    	_presenter = null;
    	super.setUseClass(useClass);
    }

	public void turnIntoFault(EOFaultHandler handler) {
		super.turnIntoFault(handler);
		flush();
	}

	public void flush() {
		_sortedBorders = null;
		_presenter = null;
	}
    
	private transient NSArray _sortedBorders;
	public NSArray sortedBorders() {
		if(_sortedBorders == null) {
			if(borders() == null)
				return null;
			_sortedBorders = EOSortOrdering.sortedArrayUsingKeyOrderArray(
					borders(),new NSArray(so)).immutableClone();
		}
		return _sortedBorders;
	}
		
	public NSArray sortedTitles() {
		NSMutableArray result = new NSMutableArray(zeroValue());
		result.addObjectsFromArray((NSArray)sortedBorders().valueForKey("title"));
		return result;
	}
	
	public void setBorders(NSArray aValue) {
        _sortedBorders = null;
		super.setBorders(aValue);
    }
	
    public void addToBorders(EOEnterpriseObject object) {
		_sortedBorders = null;
		super.addToBorders(object);
    }
	
    public void removeFromBorders(EOEnterpriseObject object) {
		_sortedBorders = null;
		super.removeFromBorders(object);
    }

	public EOEnterpriseObject borderEOForKey(String key) {
		EOQualifier qual = new EOKeyValueQualifier("title",EOQualifier.QualifierOperatorEqual,key);
		NSArray result = EOQualifier.filteredArrayWithQualifier(borders(),qual);
		if(result == null || result.count() == 0) return null;
		return (EOEnterpriseObject)result.objectAtIndex(0);
	}
	public BigDecimal borderForKey(String key) {
		if(_presenter != this) return getPresenter().borderForKey(key);
		EOEnterpriseObject border = borderEOForKey(key);
		if(border == null)
			throw new IllegalArgumentException("No such key - '" + key +'\'');
		return (BigDecimal) border.valueForKey("least");
	}
	
	public void setBorderForKey(BigDecimal value, String key) {
		EOEnterpriseObject border = borderEOForKey(key);
		if(border == null) {
			border = EOUtilities.createAndInsertInstance(editingContext(),"Border");
			addObjectToBothSidesOfRelationshipWithKey(border,"borders");
			border.takeValueForKey(key,"title");
		}
		border.takeValueForKey(value,"least");
	}
	public String presentFraction(double fraction) {
		if(_presenter != this) return getPresenter().presentFraction(fraction);
		MathContext mc = new MathContext(6);
		BigDecimal fract = new BigDecimal(fraction,mc);
		return presentFraction(fract);
/*		EOEnterpriseObject border = borderForFraction(fraction);
		return (border == null)?null:(String)border.valueForKey("title"); */
	}
	
	public String presentFraction(BigDecimal fraction) {
		if(_presenter != this) return getPresenter().presentFraction(fraction);
		EOEnterpriseObject border = borderForFraction(fraction);
		String title = (border == null)?zeroValue():
			(String)border.valueForKey("title");
		if(formatString() != null)
			title = String.format(formatString(), title);
		return title;
	}
	
	public String presentFraction(Number fraction) {
		if(fraction == null)
			return null;
		if(fraction instanceof BigDecimal)
			return presentFraction((BigDecimal)fraction);
		else
			return presentFraction(fraction.doubleValue());
	}
	
	public EOEnterpriseObject borderForFraction(double fraction) {
		MathContext mc = new MathContext(6);
		BigDecimal fract = new BigDecimal(fraction,mc);
		return borderForFraction(fract);
		/*
		Enumeration en = sortedBorders().objectEnumerator();
		EOEnterpriseObject result = null;
selection:
			while (en.hasMoreElements()) {
				EOEnterpriseObject border = (EOEnterpriseObject)en.nextElement();
				BigDecimal curr = (BigDecimal)border.valueForKey("least");
				int comparator = fract.compareTo(curr);
				if(comparator > 0) {
					result = border;//(String)border.valueForKey("title");
				} else {
					if(comparator == 0 && !exclude().booleanValue()) {
						result = border;// (String)border.valueForKey("title");
					}
					break selection;
				}
			}
		return result;*/
	}
	
	public EOEnterpriseObject borderForFraction(BigDecimal fraction) {
		return borderForFraction(fraction, false);
	}

	public EOEnterpriseObject borderForFraction(BigDecimal fraction, boolean findNext) {
		if(fraction == null) return null;
		fraction = fraction.movePointRight(2);
		Enumeration en = sortedBorders().objectEnumerator();
		EOEnterpriseObject result = null;
selection:
		while (en.hasMoreElements()) {
			EOEnterpriseObject border = (EOEnterpriseObject)en.nextElement();
			BigDecimal curr = (BigDecimal)border.valueForKey("least");
			int comparator = fraction.compareTo(curr);
			if(comparator > 0) {
				result = border;//(String)border.valueForKey("title");
			} else if(comparator == 0 && !exclude().booleanValue()) {
				result = border;
				if(!findNext)
					break selection;
			} else {
				if(findNext)
					result = border;
				break selection;
			}
		}
		return result;
	}
}
