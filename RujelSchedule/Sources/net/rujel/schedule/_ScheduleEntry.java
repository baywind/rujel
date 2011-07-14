// _ScheduleEntry.java

/*
 * Copyright (c) 2008, Gennady & Michael Kushnir
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 * 
 * 	o	Redistributions of source code must retain the above copyright notice, this
 * 		list of conditions and the following disclaimer.
 * 	o	Redistributions in binary form must reproduce the above copyright notice,
 * 		this list of conditions and the following disclaimer in the documentation
 * 		and/or other materials provided with the distribution.
 * 	o	Neither the name of the RUJEL nor the names of its contributors may be used
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

// Created by eogenerator
// $LastChangedRevision: 4733 $ DO NOT EDIT.  Make changes to ScheduleEntry.java instead.

package net.rujel.schedule;

import com.webobjects.eocontrol.*;
import com.webobjects.foundation.*;
import java.math.BigDecimal;

@SuppressWarnings("all")
public abstract class _ScheduleEntry extends EOGenericRecord {
	public static final String ENTITY_NAME = "ScheduleEntry";

	// Attributes
	public static final String FLAGS_KEY = "flags";
	public static final String NUM_KEY = "num";
	public static final String REASON_ID_KEY = "reasonId";
	public static final String VALID_SINCE_KEY = "validSince";
	public static final String VALID_TO_KEY = "validTo";
	public static final String WEEKDAY_NUM_KEY = "weekdayNum";

	// Relationships

  public Integer flags() {
    return (Integer) storedValueForKey(FLAGS_KEY);
  }

  public void setFlags(Integer value) {
    takeStoredValueForKey(value, FLAGS_KEY);
  }

  public Integer num() {
    return (Integer) storedValueForKey(NUM_KEY);
  }

  public void setNum(Integer value) {
    takeStoredValueForKey(value, NUM_KEY);
  }

  public Integer reasonId() {
    return (Integer) storedValueForKey(REASON_ID_KEY);
  }

  public void setReasonId(Integer value) {
    takeStoredValueForKey(value, REASON_ID_KEY);
  }

  public NSTimestamp validSince() {
    return (NSTimestamp) storedValueForKey(VALID_SINCE_KEY);
  }

  public void setValidSince(NSTimestamp value) {
    takeStoredValueForKey(value, VALID_SINCE_KEY);
  }

  public NSTimestamp validTo() {
    return (NSTimestamp) storedValueForKey(VALID_TO_KEY);
  }

  public void setValidTo(NSTimestamp value) {
    takeStoredValueForKey(value, VALID_TO_KEY);
  }

  public Integer weekdayNum() {
    return (Integer) storedValueForKey(WEEKDAY_NUM_KEY);
  }

  public void setWeekdayNum(Integer value) {
    takeStoredValueForKey(value, WEEKDAY_NUM_KEY);
  }

}
