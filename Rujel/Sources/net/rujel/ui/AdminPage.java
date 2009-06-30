// AdminPage.java: Class file for WO Component 'AdminPage'

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

package net.rujel.ui;

import net.rujel.reusables.Various;

import com.webobjects.appserver.*;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSKeyValueCodingAdditions;
import com.webobjects.foundation.NSMutableArray;

// Generated by the WOLips Templateengine Plug-in at Jun 17, 2009 2:24:57 PM
public class AdminPage extends com.webobjects.appserver.WOComponent {
	
	public NSArray modules;
	public NSKeyValueCodingAdditions currModule;
	
	
    public AdminPage(WOContext context) {
        super(context);
        modules = (NSArray)session().valueForKeyPath("modules.adminModules");
        if(Various.boolForObject(session().valueForKeyPath("readAccess.read.Maintenance"))) {
        	Object maintance = application().valueForKeyPath(
        			"strings.Strings.AdminPage.maintenance"); 
        	if(modules == null)
        		modules = new NSArray(maintance);
        	else if (modules instanceof NSMutableArray)
        		((NSMutableArray)modules).addObject(maintance);
        	else
        		modules = modules.arrayByAddingObject(maintance);
        }
        if(modules != null && modules.count() > 0) {
        	currModule = (NSKeyValueCodingAdditions)modules.objectAtIndex(0);
        	
        }
    }
}