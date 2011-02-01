package net.rujel.ui;

import com.webobjects.appserver.WOActionResults;
import com.webobjects.appserver.WOApplication;
import com.webobjects.appserver.WOComponent;
import com.webobjects.appserver.WOContext;
import com.webobjects.appserver.WOElement;
import com.webobjects.appserver.WORequest;
import com.webobjects.appserver.WOResponse;

public class RedirectPopup extends WOComponent {
	
	public WOComponent returnPage;
	public String target;

	public RedirectPopup(WOContext context) {
		super(context);
	}
	
    public void appendToResponse(WOResponse aResponse, WOContext aContext) {
    	aResponse.appendContentString(aContext.componentActionURL());
    	if(target != null) {
    		aResponse.appendContentCharacter('\n');
    		aResponse.appendContentString(target);
    	}
    }

    public WOActionResults invokeAction(WORequest aRequest, WOContext aContext) {
    	if(aContext.elementID().equals(aContext.senderID())) {
    		returnPage.ensureAwakeInContext(aContext);
    		return returnPage;
    	}
    	return null;
    }
    
    public WOElement template() {
    	return null;
    }
    
    public static RedirectPopup getRedirect(WOContext ctx, WOComponent returnPage) {
    	return getRedirect(ctx, returnPage,null);
    }
    
    public static RedirectPopup getRedirect(WOContext ctx, WOComponent returnPage, String target) {
    	RedirectPopup redir = (RedirectPopup)WOApplication.application().pageWithName(
    			"RedirectPopup", ctx);
    	redir.returnPage = returnPage;
    	redir.target = target;
    	return redir;
    }
}
