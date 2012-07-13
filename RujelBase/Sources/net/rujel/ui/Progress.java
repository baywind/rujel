package net.rujel.ui;

import net.rujel.reusables.WOLogFormatter;

import com.webobjects.appserver.WOActionResults;
import com.webobjects.appserver.WOComponent;
import com.webobjects.appserver.WOContext;
import com.webobjects.appserver.WOResponse;
import com.webobjects.foundation.NSMutableArray;

public class Progress extends WOComponent {
    public Progress(WOContext context) {
        super(context);
    }
    
    public String title;
    public WOComponent returnPage;
    public String resultPath;
    public State state;
    public String progress;;
    public String stage;
    public String error;
	public String messages;
    
    public void appendToResponse(WOResponse aResponse, WOContext aContext) {
    	synchronized (state) {
    		progress = "height:20px;width:" + (int)(500 * state.progress()) + "px;";
    		StringBuilder buf = new StringBuilder();
    		State st = state;
    		if(st.name != null)
    			buf.append(st.name).append(' ');
    		buf.append('(').append(st.current).append('/').append(st.total).append(')');
    		while (st.sub != null) {
				st = st.sub;
				buf.append(" : ");
	    		if(st.name != null)
	    			buf.append(st.name).append(' ');
	    		buf.append('(').append(st.current).append('/').append(st.total).append(')');
			}
    		stage = buf.toString();
    		if(state.messages != null) {
    			messages = state.messages.componentsJoinedByString("<br/>");
    		}
		}
    	super.appendToResponse(aResponse, aContext);
    }
    
    public WOActionResults refresh() {
    	synchronized (state) {
			if(state.result instanceof Exception) {
				error = WOLogFormatter.formatTrowableHTML((Exception)state.result);
			} else if(state.current >= state.total) {
				returnPage.ensureAwakeInContext(context());
				if(resultPath != null)
					returnPage.takeValueForKeyPath(state.result, resultPath);
				return RedirectPopup.getRedirect(context(), returnPage);
			}
		}
    	return this;
    }
    
    public static class State {
    	public int total = 1;
    	public int current = 0;
    	public String name;
    	public State sub;
    	private State parent;
    	public Object result;
    	public NSMutableArray messages;
    	
    	public State createSub() {
    		sub = new State();
    		sub.parent = this;
    		return sub;
    	}
    	public State getParent() {
    		return parent;
    	}
    	public State end() {
    		parent.current++;
    		parent.sub = null;
    		return parent;
    	}
    	
    	public double progress() {
    		double progress = ((double)current)/total;
    		if(sub != null)
    			progress += sub.progress()/total;
    		return progress;
    	}
    	
    	public void addMessage(String message) {
    		if(messages == null)
    			messages = new NSMutableArray(message);
    		else
    			messages.addObject(message);
    	}
    }
}