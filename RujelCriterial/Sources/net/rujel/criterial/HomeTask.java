// HomeTask.java

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

public class HomeTask extends _HomeTask
{
	protected int _hours = 0;
	protected int _minutes = 0;
	
    public HomeTask() {
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
	public int period() {
		long millis = deadline().getTime() - datePublished().getTime();
		return (int)(millis / (1000*3600*24));
	}
	
	public void setTimeRequired(Integer aValue) {
        super.setTimeRequired(aValue);
		iniTime(aValue);
    }
	
	protected void iniTime(Number time) {
		if(time == null || time.intValue()==0) {
			_hours = 0;
			_minutes = 0;
		} else {
			_hours = time.intValue() / 60;
			_minutes = time.intValue() % 60;
		}
	}
	
	public Number hours() {
		iniTime(timeRequired());
		if(_hours == 0) return null;
		else return new Integer(_hours);
	}
	
	public Number minutes() {
		iniTime(timeRequired());
		if(_hours == 0) return timeRequired();
		else return new Integer(_minutes);
	}
	
	public void setHours(Number hours) {
		_hours = (hours == null)?0:hours.intValue();
		if((hours() == null)?_hours != 0:_hours != hours().intValue()) 
			super.setTimeRequired(new Integer(_hours*60 + _minutes));
	}
	
	public void setMinutes(Number minutes) {
		_minutes = (minutes == null)?0:minutes.intValue();
		if((minutes() == null)?_minutes != 0:_minutes != minutes().intValue()) 
			super.setTimeRequired(new Integer(_hours*60 + _minutes));
	}
	
	public double load() {
		return timeRequired().doubleValue()/period();
	}
	
	public void setTask(String aValue) {
		if(aValue == null || aValue.length() == 0) {
			work().removeObjectFromBothSidesOfRelationshipWithKey(this,"homeTasks");
			editingContext().deleteObject(this);
		} else {
			super.setTask(aValue);
		}
	}
}
