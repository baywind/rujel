// _Reprimand.java

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
// $LastChangedRevision: 4733 $ DO NOT EDIT.  Make changes to Reprimand.java instead.

package net.rujel.curriculum;

import com.webobjects.eocontrol.*;
import com.webobjects.foundation.*;
import java.math.BigDecimal;

@SuppressWarnings("all")
public abstract class _Reprimand extends EOGenericRecord {
	public static final String ENTITY_NAME = "Reprimand";

	// Attributes
	public static final String AUTHOR_KEY = "author";
	public static final String CONTENT_KEY = "content";
	public static final String RAISED_KEY = "raised";
	public static final String RELIEF_KEY = "relief";
	public static final String STATUS_KEY = "status";

	// Relationships

  public String author() {
    return (String) storedValueForKey(AUTHOR_KEY);
  }

  public void setAuthor(String value) {
    takeStoredValueForKey(value, AUTHOR_KEY);
  }

  public String content() {
    return (String) storedValueForKey(CONTENT_KEY);
  }

  public void setContent(String value) {
    takeStoredValueForKey(value, CONTENT_KEY);
  }

  public NSTimestamp raised() {
    return (NSTimestamp) storedValueForKey(RAISED_KEY);
  }

  public void setRaised(NSTimestamp value) {
    takeStoredValueForKey(value, RAISED_KEY);
  }

  public NSTimestamp relief() {
    return (NSTimestamp) storedValueForKey(RELIEF_KEY);
  }

  public void setRelief(NSTimestamp value) {
    takeStoredValueForKey(value, RELIEF_KEY);
  }

  public Integer status() {
    return (Integer) storedValueForKey(STATUS_KEY);
  }

  public void setStatus(Integer value) {
    takeStoredValueForKey(value, STATUS_KEY);
  }

}
