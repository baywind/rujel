package net.rujel.contacts;

import com.webobjects.appserver.WOContext;
import com.webobjects.appserver.WOComponent;

public class MailRequest extends WOComponent {
    public MailRequest(WOContext context) {
        super(context);
    }
    
    public String code;
}