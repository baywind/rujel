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
	protected static final EOSortOrdering so = EOSortOrdering.sortOrderingWithKey("least",EOSortOrdering.CompareAscending);

	public BorderSet() {
        super();
    }

/*
    // If you add instance variables to store property values you
    // should add empty implementions of the Serialization methods
    // to avoid unnecessary overhead (the properties will be
    // serialized for you in the superclass).
    private void writeObject(java.io.ObjectOutputStream out) throws java.io.IOException {
    }

    private void readObject(java.io.ObjectInputStream in) throws java.io.IOException, java.lang.ClassNotFoundException {
    }
*/
	
	public static FractionPresenter fractionPresenterForTitleAndDate(EOEditingContext ec, String title,NSTimestamp date) {
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
		NSArray args = new NSArray (new Object[] {title,date});
		EOQualifier qual = EOQualifier.qualifierWithQualifierFormat("title = %@ AND upto = nil",args);
		EOSortOrdering sord = EOSortOrdering.sortOrderingWithKey("upto",EOSortOrdering.CompareDescending);
		EOFetchSpecification fspec = new EOFetchSpecification("BorderSet",qual,new NSArray(sord));
		NSArray found = ec.objectsWithFetchSpecification(fspec);
		BorderSet result = null;
		if(found != null && found.count() > 0)
			result = (BorderSet)found.objectAtIndex(0);
		if(result == null) {
			qual = EOQualifier.qualifierWithQualifierFormat("title = %@ AND upto > %@",args);
			fspec.setQualifier(qual);
			fspec.setFetchLimit(1);
			found = ec.objectsWithFetchSpecification(fspec);
			if(found == null || found.count() == 0) return PERCENTAGE;
			result = (BorderSet)found.objectAtIndex(0);
		}
		if(result.useClass() !=null && result.useClass().length() > 0) {
			try {
				Class resClass = Class.forName(result.useClass());
				return (FractionPresenter)resClass.getConstructor((Class[])null).newInstance();
			} catch (Exception e) {
				throw new NSForwardException(e,"Error constructing FractionPresenter with title '" + title + "' and class '" + result.useClass() + '\'');
			}
		}
		return result;
	}
	

	private transient NSArray _sortedBorders;
	public NSArray sortedBorders() {
		if(_sortedBorders == null) {
			_sortedBorders = EOSortOrdering.sortedArrayUsingKeyOrderArray(borders(),new NSArray(so)).immutableClone();
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
	
	

	public EOEnterpriseObject borderEOForKey(String key) {
		EOQualifier qual = new EOKeyValueQualifier("title",EOQualifier.QualifierOperatorEqual,key);
		NSArray result = EOQualifier.filteredArrayWithQualifier(borders(),qual);
		if(result == null || result.count() == 0) return null;
		return (EOEnterpriseObject)result.objectAtIndex(0);
	}
	public BigDecimal borderForKey(String key) {
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
		MathContext mc = new MathContext(6);
		BigDecimal fract = new BigDecimal(fraction,mc);
		return presentFraction(fract);
/*		EOEnterpriseObject border = borderForFraction(fraction);
		return (border == null)?null:(String)border.valueForKey("title"); */
	}
	
	public String presentFraction(BigDecimal fraction) {
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
		Enumeration en = sortedBorders().objectEnumerator();
		EOEnterpriseObject result = null;
selection:
		while (en.hasMoreElements()) {
			EOEnterpriseObject border = (EOEnterpriseObject)en.nextElement();
			BigDecimal curr = (BigDecimal)border.valueForKey("least");
			int comparator = fraction.compareTo(curr.movePointLeft(2));
			if(comparator > 0) {
				result = border;//(String)border.valueForKey("title");
			} else {
				if((comparator == 0 && !exclude().booleanValue()) ^ findNext) {
					result = border;//(String)border.valueForKey("title");
				}
				break selection;
			}
		}
		return result;
	}
}
