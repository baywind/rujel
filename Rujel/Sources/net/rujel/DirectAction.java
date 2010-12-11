// DirectAction.java

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

package net.rujel;

import java.util.logging.Logger;

import net.rujel.auth.LoginProcessor;
import net.rujel.auth.UserPresentation;
import net.rujel.reusables.SettingsReader;
import net.rujel.reusables.WOLogLevel;

import com.webobjects.appserver.*;

public class DirectAction extends WODirectAction {
/*	protected static CoreApplication appl() {
		return ((CoreApplication)WOApplication.application());
	}*/

    public DirectAction(WORequest aRequest) {
        super(aRequest);
    }

    public WOActionResults defaultAction() {
/*		String url = context().directActionURLForActionNamed("login", null);
		WORedirect result = new WORedirect(context());
		result.setUrl(url);
		*/
        //return pageWithName("Main");
		WOComponent result;
		WOSession ses = WOApplication.application().restoreSessionWithID(request().sessionID(),context());
		if (ses != null && ses.valueForKey("user") != null)//(context().hasSession() || ses != null)
			result = pageWithName("SrcMark");
		else
			result = LoginProcessor.secureRedirect("login",context(),false);
		return result;
    }
	
	public WOActionResults successAction() {
		WOApplication.application().takeValueForKey(request(), "request");
		return defaultAction();
	}

	public WOActionResults loginAction() {
		//WOComponent nextPage = appl().loginHandler().loginComponent(context());
		return LoginProcessor.loginAction(context());
		//return pageWithName("LoginDialog");
	}
	
	public WOActionResults guestAction() {
		if(SettingsReader.boolForKeyPath("auth.noGuest", false))
			return LoginProcessor.secureRedirect("login",context(),false);
		context().session().takeValueForKey(new UserPresentation.Guest(), "user");
		return LoginProcessor.secureRedirect("success",context(),false);
	}
	
	public WOActionResults refuseAction() {
		WOComponent result = pageWithName("MessagePage");
		result.takeValueForKey("noAccessTitle", "plistTitle");
		result.takeValueForKey("accessDenied", "plistMessage");
		result.takeValueForKey("login", "redirectAction");
		return result;
		/*WOResponse resp = WOApplication.application().createResponseInContext(context());
		WOResourceManager rm = WOApplication.application().resourceManager();
		java.io.InputStream in = rm.inputStreamForResourceNamed("AccessDenied.html",null,null);
		try {
			resp.setContentStream(in,4096,in.available());
		} catch (java.io.IOException ioex) {
			throw new NSForwardException (ioex);
		}
		return resp;*/
	}
	
	public WOActionResults logoutAction() {
		WOSession ses = existingSession();
		if(ses != null)
			ses.terminate();
		String url = SettingsReader.stringForKeyPath("ui.logoutScreen", null);
		if(url == null)
			return LoginProcessor.secureRedirect("login",context(),false);
		WORedirect result = new WORedirect(context());
		result.setUrl(url);
		return result; 
	}
	
/*	public WOActionResults checkAction() {
		WOComponent nextPage = LoginProcessor.processLogin(context());
		return nextPage;
	} */
	
	public WOActionResults reportAction() {
		WOResponse response = WOApplication.application().createResponseInContext(context());
		WOSession ses = existingSession();
		String report = request().stringFormValueForKey("report");
		if(report != null && report.length() > 0) 
			Logger.getLogger("rujel").log(WOLogLevel.INFO,"Error report: \n" + report,ses);
		if(ses == null) {
			response.appendContentString(context().directActionURLForActionNamed("logout", null));
		} else {
			response.appendContentString(context().directActionURLForActionNamed("resume", null));
		}
		return response;
	}
	
	public WOActionResults resumeAction() {
		WOSession ses = existingSession();
		if(ses == null)
			return logoutAction();
		WOComponent last = (WOComponent)ses.valueForKey("pullComponent");
		ses.takeValueForKey(Boolean.FALSE,"prolong");
		return last;
	}
}
