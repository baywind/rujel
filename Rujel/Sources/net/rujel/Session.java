// Session.java

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

import net.rujel.auth.*;
import net.rujel.reusables.*;
import net.rujel.interfaces.*;
import net.rujel.base.MyUtility;

import com.webobjects.foundation.*;
import com.webobjects.appserver.*;
import com.webobjects.eocontrol.*;
import com.webobjects.eoaccess.EOUtilities;

import java.util.logging.Logger;

public class Session extends WOSession {
	private UserPresentation user = null;
	protected Logger logger = Logger.getLogger("rujel");
	protected NSDictionary clientIdentity;

	protected NSTimestamp today;// = new NSTimestamp();
	public Session() {
        super();
		//logger.log(WOLogLevel.SESSION,"Session created",this);
        
		setDefaultEditingContext(new SessionedEditingContext(this));
   }
	
	protected SessionedEditingContext _defaultEC;
	public SessionedEditingContext defaultEditingContext() {
		EOObjectStore os = EOObjectStoreCoordinator.defaultCoordinator();
		if(SettingsReader.boolForKeyPath("dbConnection.yearTag", false)) {
			os = (EOObjectStore)objectForKey("objectStore");
			if(os == null)
				os = DataBaseConnector.objectStoreForTag(eduYear().toString());
			if(_defaultEC == null || _defaultEC.parentObjectStore() != os) {
				if(_defaultEC != null)
					_defaultEC.unlock();
				_defaultEC = new SessionedEditingContext(os,this);
				_defaultEC.lock();
			}
		} else if (_defaultEC == null) {
			_defaultEC = new SessionedEditingContext(os,this);
			_defaultEC.lock();
		}
		return _defaultEC;
	}
    
	protected ReadAccess _readAccess;
	
	public ReadAccess readAccess() {
		return _readAccess;
	}
	
	public static final String[] CLIENT_IDENTITY_KEYS = new String[]
	{"x-webobjects-remote-addr", "remote_addr","remote_host","user-agent"};
	
	public static NSDictionary clientIdentity(WORequest request) {
		NSMutableDictionary result = new NSMutableDictionary();
		Object value = null;
		for (int i = 0; i < CLIENT_IDENTITY_KEYS.length; i++) {
			value = request.headerForKey((String)CLIENT_IDENTITY_KEYS[i]);
			if(value != null && (result.count() == 0 || !result.containsValue(value)))
				result.setObjectForKey(value,CLIENT_IDENTITY_KEYS[i]);
		}
		return result;
	}
	
	public void awake() {
		super.awake();
		logger.logp(WOLogLevel.FINEST,"Session","awake","Session awake",this);
		if(user != null) {
			NSDictionary curr = clientIdentity(context().request());
			if(curr == null) {
				logger.log(WOLogLevel.WARNING,"Unable to identify client",this);
			} else if(!curr.equals(clientIdentity)) {
				String msg = (clientIdentity == null)?"Registered client":"Client changed (maybe sniffed)";
				logger.log(WOLogLevel.SESSION,msg,new Object[] {this,curr});
				clientIdentity = curr;
			}
		}
		if(_defaultEC != null)
			_defaultEC.lock();
	}
	
	public void sleep() {
		if(_defaultEC != null)
			_defaultEC.unlock();
	}
	
	public void setUser(UserPresentation aUser) {
		if(user == null) {
			logger.log(WOLogLevel.SESSION,"Session created for user: " + aUser,new Object[] {this});
		} else if (user != aUser) {
			logger.log(WOLogLevel.SESSION,"Session user changed from " + user + " to " + aUser ,new Object[] {this});
		}
		user = aUser;
		_readAccess = new ReadAccess(this);
	}
	
	public void setDummyUser(Boolean acc) {
		setUser(new net.rujel.auth.UserPresentation.DummyUser(acc.booleanValue()));
	}
	
	public UserPresentation user() {
		return user;
	}
	
	public boolean allowedToViewEvents() {
		return true;
	}
	
	public boolean allowedToViewStatistics() {
		return true;
	}
	
	protected Integer _school;
	public Integer school() {
		if(_school == null) {
			_school = new Integer(SettingsReader.intForKeyPath("schoolNumber", 0));
		}
		return _school;
	}
	
//	public boolean isStudent = false;
	private EOGlobalID personGID;
    protected StringBuffer message = new StringBuffer();
	public EOGlobalID userPersonGID() {
		if (personGID != null) return personGID;
		//UserPresentation user = (UserPresentation)session().valueForKey("user");
		//Person result = null;
		Object pid = user.propertyNamed("teacherID");//.toString();
		String className = Teacher.entityName;
		if (pid == null) {
			pid = user.propertyNamed("studentID");//.toString();
			className = Student.entityName;
//			isStudent = true;
		}
		
		if (pid == null) return null;
		
		Object pKey = null;
		if(pid instanceof Number)
			pKey = pid;
		else {
			try {
				pKey = Integer.valueOf(pid.toString());
			} catch (NumberFormatException ex) {
				pKey = pid;
			}
		}
		
		try {
			EOEnterpriseObject personLink = EOUtilities.objectWithPrimaryKeyValue(
					defaultEditingContext(),className, pKey);
			//person = pLink.person();
			personGID = defaultEditingContext().globalIDForObject(personLink);
			if(personLink != null) persList.addObject(((PersonLink)personLink).person());
			logger.log(WOLogLevel.CONFIG, "Person resolved for user: "+ user + " = " + 
					Person.Utility.fullName((PersonLink)personLink,true,2,2,2),this); 
		} catch (Exception ex) {
			return null;
		}
		return personGID;
	}
	
    public String message() {
    	if(message == null || message.length() == 0)
    		return null;
        return message.toString();
    }
    
    public synchronized void setMessage(String newMessage) {
    	if(newMessage == null) {
    		if(message != null)
    			message.delete(0,message.length());
    	} else {
    		if(message == null) {
    			message = new StringBuffer(newMessage);
    		} else {
    			if(message.length() > 0)
    				message.append("<p>").append(newMessage).append("</p>\n");
    			else
    				message.append(newMessage);
    		}
    	}
    }

	public boolean prolong;
	
	public void appendToResponse(WOResponse aResponse, WOContext aContext) {
		double to = timeOut();
		if(WOApplication.application().isRefusingNewSessions()) {
			to = to / Double.parseDouble(System.getProperty("TimeOutProgressiveDivizor","2"));
		} else {
			to = Double.parseDouble(System.getProperty("WOSessionTimeOut","3600"));
			if(prolong) {
				to = to * Double.parseDouble(System.getProperty("OnEditTimeOutMultiplier","2"));
			}
		}
		setTimeOut(to);
		super.appendToResponse(aResponse,aContext);
	}
	
	private NSMutableArray persList = new NSMutableArray();
	public NSMutableArray personList() {
		return persList;
	}
	public NSArray sortedPersList(NSArray sorter){
		return EOSortOrdering.sortedArrayUsingKeyOrderArray(persList,sorter);
	}
	public NSArray sortedPersList() {
		return sortedPersList(Person.sorter);
	}
	
	public void sortPersonList() {
		EOSortOrdering.sortArrayUsingKeyOrderArray(persList,Person.sorter);
	}
	public void clearPersonList() {
		persList.removeAllObjects();
		if (personGID != null) {
			PersonLink personLink = (PersonLink)defaultEditingContext().objectForGlobalID(personGID);
			persList.addObject(personLink.person());
		}
	}
	
	public NSTimestamp today() {
		if(today == null) {
			String defaultDate = SettingsReader.stringForKeyPath("ui.defaultDate", null);
			if(defaultDate == null) {
				today = new NSTimestamp();
			} else {
				try {
					today = (NSTimestamp)MyUtility.dateFormat().parseObject(defaultDate);
				} catch (Exception e) {
					logger.log(WOLogLevel.WARNING, "Failed parsing default date " + 
							defaultDate + ". Using today.",e);
					today = new NSTimestamp();
				}
			}
		}
		return today;
	}
	
	public void setToday(NSTimestamp day) {
		if(day == null) {
//			Calendar cal = Calendar.getInstance();
//			cal.set(2008,8,15,0,30);
//			today = new NSTimestamp(cal.getTimeInMillis());
			today = new NSTimestamp();
		} else {
			today = day;
		}
		if(SettingsReader.boolForKeyPath("dbConnection.yearTag", false)) {
			EOObjectStore os = DataBaseConnector.objectStoreForTag(eduYear().toString());
			setObjectForKey(os, "objectStore");
			if(persList != null && persList.count() > 0) {
				persList.removeAllObjects();
			}
//			if(defaultEditingContext().rootObjectStore() != os) {
//				defaultEditingContext().unlock();
//				setDefaultEditingContext(new SessionedEditingContext(os,this));
//			}
		}
	}
	
	public Integer eduYear() {
		return MyUtility.eduYearForDate(today());
	}
	
	public String checkRun() {
		String href = context().componentActionURL();
		String result = "return checkRun('" + href + "');";
		//actions.setObjectForKey(item,context().elementID());
		return result;
	}
	
	public String ajaxPopup() {
		String href = context().componentActionURL();
		String result = "ajaxPopupAction('" + href + "',event);";
		return result;
	}
	
	public String tryLoad() {
		String href = context().componentActionURL();
		String result = "if(tryLoad())window.location = '" + href +"';";
		return result;
	}
	
    public String confirmMessage() {
/*		WOApplication app = WOApplication.application();
		String formatter = (String)app.valueForKeyPath("strings.Strings.messages.areYouShure");
		Object action = app.valueForKeyPath("strings.Reusables_Strings.uiElements.Delete");
		String msg = "Are you shure?";
		if(formatter != null && action != null) {
			try {
				msg = String.format(formatter,action);
			} catch (Exception ex) {
				; // oops
			}
		}
		return "return (tryLoad(false) && confirm('" + msg + "'));";
*/	
		String href = context().componentActionURL();
		return "if(confirmAction(this.value,event) && tryLoad())window.location = '" + href +"';";
    }
	public static final NSArray defaultAccessKeys = new NSArray(new Object[] {
		"read","create","edit","delete"});
/*
	public NamedFlags access(Object module,NSArray keys) {
		if(keys == null) keys = defaultAccessKeys;
		return new NamedFlags(accessLevel(module),UseAccess.accessKeys);
	}
	
	public int accessLevel(Object module) {
		try {
			return user.accessLevel(module);
		} catch (AccessHandler.UnlistedModuleException e) {
			logger.log(WOLogLevel.CONFIG,"Can't get accessLevel",new Object[] {this,module,e.getMessage()});
			return -1;
		}
	}
	
	public NSKeyValueCoding accessLevel() {
		return new NSKeyValueCoding() {
			public void takeValueForKey(Object value,String key) {
				throw new UnsupportedOperationException("This is read-only value");
			}
			
			public Object valueForKey(String key) {
				return access(key,null);
			}
		};
	}
	*/
	public NSMutableArray pathStack = new NSMutableArray();
	
	public void setPushComponent (WOComponent nextComponent) {
		int idx = pathStack.indexOfIdenticalObject(nextComponent);
		if(idx == NSArray.NotFound) {
			try {
				Object title = nextComponent.valueForKey("title");
				NSArray titles = (NSArray)pathStack.valueForKey("title");
				idx = titles.indexOfObject(title);
				if(idx == NSArray.NotFound) {
					pathStack.addObject(nextComponent);
					return;
				}
			} catch (Exception ex) {
				pathStack.addObject(nextComponent);
				return;
			}
		}
		if(idx < pathStack.count() -1) {
			NSRange left = new NSRange(idx + 1,pathStack.count() - idx - 1);
			pathStack.removeObjectsInRange(left);
		}
	}
	
	public WOComponent pullComponent() {
		WOComponent result = (WOComponent)pathStack.lastObject();
		pathStack.removeObjectAtIndex(pathStack.count() - 1);
		result.ensureAwakeInContext(context());
		return result;
	}
    /*
	public NSArray pathStack() {
		return pathStack.immutableClone();
	}
	
	public void */
	public void terminate() {
		logger.log(WOLogLevel.SESSION,"Session terminated",this);
		super.terminate();
	}
		
	protected ModulesInitialiser _modules;
	public ModulesInitialiser modules() {
		if(_modules == null) {
			_modules = new ModulesInitialiser(this);
		}
		return _modules;
	}
}
