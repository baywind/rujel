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
import net.rujel.base.ReadAccess;

import com.webobjects.foundation.*;
import com.webobjects.appserver.*;
import com.webobjects.eocontrol.*;
import com.webobjects.eoaccess.EOUtilities;

import java.util.logging.Logger;

public class Session extends WOSession implements MultiECLockManager.Session {
	private UserPresentation user = null;
	protected StringStorage _strings;
	protected Logger logger = Logger.getLogger("rujel");
	protected NSDictionary clientIdentity;
	protected MultiECLockManager ecLockManager;
	public NSMutableDictionary state = new NSMutableDictionary();

	protected NSTimestamp today;// = new NSTimestamp();
	public Session() {
        super();
		//logger.log(WOLogLevel.SESSION,"Session created",this);
        
		_strings = (StringStorage)WOApplication.application().valueForKey("strings");
		ecLockManager = new MultiECLockManager();
   }
	
	protected SessionedEditingContext _defaultEC;
	public SessionedEditingContext defaultEditingContext() {
		EOObjectStore os = EOObjectStoreCoordinator.defaultCoordinator();
		if(SettingsReader.boolForKeyPath("dbConnection.yearTag", false)) {
			os = (EOObjectStore)objectForKey("objectStore");
			if(os == null)
				os = DataBaseConnector.objectStoreForTag(eduYear().toString());
			if(_defaultEC == null || _defaultEC.rootObjectStore() != os) {
//				if(_defaultEC != null)
//					_defaultEC.unlock();
				_defaultEC = new SessionedEditingContext(os,this);
//				_defaultEC.lock();
			}
			ecLockManager.registerEditingContext(_defaultEC);
		} else if (_defaultEC == null) {
			_defaultEC = new SessionedEditingContext(os,this);
//			_defaultEC.lock();
			ecLockManager.registerEditingContext(_defaultEC);
		}
		return _defaultEC;
	}
    
	public MultiECLockManager ecLockManager() {
		return ecLockManager;
	}
	
	protected ReadAccess _readAccess;
	
	public ReadAccess readAccess() {
		if(_readAccess == null)
			setDummyUser(Boolean.FALSE);
		return _readAccess;
	}
/*
	public String formMethod() {
		String ua = context().request().headerForKey("user-agent");
		if(ua == null || ua.contains("MSIE"))
			return "get";
		return "post";
	}
*/	
	public void awake() {
		super.awake();
		logger.logp(WOLogLevel.FINEST,"Session","awake","Session awake",this);
		try {
		if(user != null) {
			NSDictionary curr = Various.clientIdentity(context().request());
			if(curr == null) {
				logger.log(WOLogLevel.WARNING,"Unable to identify client",this);
			} else if(!curr.equals(clientIdentity)) {
				String msg = (clientIdentity == null)?"Registered client":"Client changed (maybe sniffed)";
				logger.log(WOLogLevel.SESSION,msg,new Object[] {this,curr});
				clientIdentity = curr;
			}
		}
		} finally {
		ecLockManager.lock();
		}
//		if(_defaultEC != null)
//			_defaultEC.lock();
	}
	
	public void sleep() {
		if(!isTerminating())
			ecLockManager.unlock();
		super.sleep();
//		if(_defaultEC != null)
//			_defaultEC.unlock();
	}
	
	public void setUser(UserPresentation aUser) {
		StringBuilder buf = new StringBuilder();
		if(user == null) {
			buf.append("Session created for user: ");
			buf.append(aUser);
		} else if (user != aUser) {
			buf.append("Session user changed from ").append(user);
			buf.append(" to ").append(aUser);		
		}
		if(!aUser.toString().equals(aUser.present()))
			buf.append(" (").append(aUser.present()).append(')');
		logger.log(WOLogLevel.SESSION, buf.toString(), this);
		user = aUser;
		_readAccess = new ReadAccess(this);
	}
	
	public void setDummyUser(Boolean acc) {
		setUser(new net.rujel.auth.UserPresentation.DummyUser(acc.booleanValue()));
		readAccess().takeValueForKey(acc, "dummyUser");
	}
	
	public UserPresentation user() {
		if(user == null)
			setDummyUser(Boolean.FALSE);
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
		personGID = (EOGlobalID)user.propertyNamed("personGID");
		if (personGID != null) return personGID;
		
		Object pid = user.propertyNamed("teacherID");
		String className = Teacher.entityName;
		if (pid == null) {
			pid = user.propertyNamed("studentID");
			className = Student.entityName;
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
			personGID = defaultEditingContext().globalIDForObject(personLink);
			if(personLink != null && ((PersonLink)personLink).person() != null)
				persList.addObject(((PersonLink)personLink).person());
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
        String result = message.toString();
        message.append('\n');
        return result;
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
			if(!prolong)
				to = to / Double.parseDouble(System.getProperty("TimeOutProgressiveDivizor","2"));
			logger.log(WOLogLevel.FINE,"Session timeout: " + to,this);
		} else {
			to = Double.parseDouble(System.getProperty("WOSessionTimeOut","3600"));
			if(prolong) {
				to = to * Double.parseDouble(System.getProperty("OnEditTimeOutMultiplier","2"));
			}
		}
		setTimeOut(to);
		to = (double)message.length();
		super.appendToResponse(aResponse,aContext);
		int diff = message.length() - (int)to;
		if (diff > 0 && diff < 10 && message.charAt(message.length() -1) == '\n')
			message.delete(0, message.length());
	}
	
	private NSMutableArray persList = new NSMutableArray();
	public NSMutableArray personList() {
		if(persList.count() == 0) {
			EOGlobalID gid = userPersonGID();
			if(gid != null) {
				EOEnterpriseObject up = defaultEditingContext().faultForGlobalID(
						gid, defaultEditingContext());
				if(up != null)
					persList.addObject(up);
			}
		}
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
			if(_eduYear != null)
				return null;
			String defaultDate = SettingsReader.stringForKeyPath("ui.defaultDate", null);
			if(defaultDate == null) {
				_eduYear = (Integer)WOApplication.application().valueForKey("year");
				today = MyUtility.dayInEduYear(_eduYear.intValue());
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
	
	public void setToday(Object day) {
		if(day == null) {
			day = new NSTimestamp();
		}
		Integer nextYear = (day instanceof Integer)?(Integer)day:
			MyUtility.eduYearForDate((NSTimestamp)day);
		if(!nextYear.equals(_eduYear)) {
			logger.log(WOLogLevel.INFO,"Switching eduYear to " + nextYear);
			if(SettingsReader.boolForKeyPath("dbConnection.yearTag", false)) {
				EOObjectStore os = DataBaseConnector.objectStoreForTag(nextYear.toString());
				if(os == null) {
					String msg = (String)WOApplication.application().valueForKeyPath(
					"strings.Strings.messages.unavailableYearlyDb");
					msg = String.format(msg, MyUtility.presentEduYear(nextYear.intValue()));
					setMessage(msg);
					logger.log(WOLogLevel.INFO,msg);
					return;
				}
				_eduYear = nextYear;
				setObjectForKey(os, "objectStore");
				if(persList != null && persList.count() > 0) {
					persList.removeAllObjects();
				}
				pathStack.removeAllObjects();
//				if(defaultEditingContext().rootObjectStore() != os) {
//				defaultEditingContext().unlock();
//				setDefaultEditingContext(new SessionedEditingContext(os,this));
//				}
			}
		}
		if(day instanceof Integer)
			today = null;
		else
			today = (NSTimestamp)day;
	}
	
	protected Integer _eduYear;
	public Integer eduYear() {
		if(_eduYear == null) {
			today();
			if(_eduYear == null) {
				_eduYear = MyUtility.eduYearForDate(today());
			}
		}
		return _eduYear;
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

	public String ajaxPopupNoPos() {
		String href = context().componentActionURL();
		String result = "ajaxPopupAction('" + href + "');";
		return result;
	}
	
	public String tryLoad() {
		String href = context().componentActionURL();
		String result = "if(tryLoad())window.location = '" + href +"';";
		return result;
	}
	
    public String confirmMessage() {
		String href = context().componentActionURL();
		return "if(confirmAction(null,event) && tryLoad())window.location = '"
			+ href +"'; else return false;";
    }

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
		WOComponent result = (WOComponent)pathStack.removeLastObject();
		if(result == null)
			result = WOApplication.application().pageWithName("SrcMark", context());
		else
			result.ensureAwakeInContext(context());
		return result;
	}

	public void terminate() {
		try {
			ModulesInitialiser.useModules(null, this);
		} catch (Exception e) {
			logger.log(WOLogLevel.WARNING,"Error executing modules when terminating session",
					new Object[] {this, e});
		}
		logger.log(WOLogLevel.SESSION,"Session terminated",this);
		_modules = null;
		_strings = null;
		clientIdentity = null;
		_defaultEC = null;
		ecLockManager.unregisterAll();
		ecLockManager = null;
		super.terminate();
	}
		
	protected ModulesInitialiser _modules;
	public ModulesInitialiser modules() {
		if(_modules == null) {
			_modules = new ModulesInitialiser(this);
		}
		return _modules;
	}
	
	public NSKeyValueCoding strings() {
		return _strings;
	}
}
