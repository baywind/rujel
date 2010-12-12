// TableLoginHandler.java

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

package net.rujel.user;

import java.lang.reflect.Constructor;
import java.security.MessageDigest;
import java.util.Enumeration;
import java.util.logging.Logger;

import net.rujel.auth.LoginHandler;
import net.rujel.auth.LoginProcessor;
import net.rujel.auth.UserPresentation;
import net.rujel.reusables.SettingsReader;
import net.rujel.reusables.WOLogLevel;

import com.webobjects.eoaccess.EOUtilities;
import com.webobjects.eocontrol.EOEditingContext;
import com.webobjects.eocontrol.EOEnterpriseObject;
import com.webobjects.eocontrol.EOFetchSpecification;
import com.webobjects.foundation.NSArray;

public class TableLoginHandler implements LoginHandler {
	protected static Logger logger = Logger.getLogger("rujel.user");
	protected LoginHandler parentHandler;
	protected static Boolean noUsers;
	public static final String HASH_PREFIX = "pwhash: ";
	
	public TableLoginHandler() {
		super();
		String lhClassName = net.rujel.reusables.SettingsReader.stringForKeyPath(
				"auth.parentLoginHandler",null);
		if(lhClassName != null) {
			try {
				Class lhClass = Class.forName(lhClassName);
				Constructor lhConstuctor = lhClass.getConstructor();
				parentHandler = (LoginHandler)lhConstuctor.newInstance();
				logger.log(WOLogLevel.CONFIG,
						"Registered parent LoginHandler for TableLoginHandler: " + lhClassName);
			} catch (Exception ex) {
				logger.log(WOLogLevel.WARNING,"Error instantiating parent login hadler",ex);
			}
		}
	}

	public static void flush() {
		noUsers = null;
	}
	
	protected boolean noUsers() {
		if(noUsers == null) {
			if(SettingsReader.stringForKeyPath("auth.parentLoginHandler", null) == null) {
				EOEditingContext ec = new EOEditingContext();
				try {
					ec.lock();
					EOFetchSpecification fs = new EOFetchSpecification(
							AutUser.ENTITY_NAME,null,null);
					fs.setFetchLimit(1);
					NSArray users = ec.objectsWithFetchSpecification(fs);
					noUsers = Boolean.valueOf(users == null || users.count() == 0);
				} catch (Exception e) {
					noUsers = Boolean.FALSE;
				} finally {
					ec.unlock();
				}
			} else {
				noUsers = Boolean.FALSE;
			}
		}
		return noUsers.booleanValue();
	}
	
	public String[] args() {
		if(noUsers())
			return new String[0];
		return new String[] {"username", "password"};
	}
	
	public String identityArg() {
		if(noUsers())
			return null;
		return "username";
	}

	public UserPresentation authenticate (Object [] args) 
			throws AuthenticationFailedException, IllegalArgumentException {
		if(noUsers()) {
			logger.log(WOLogLevel.WARNING,
					"No users found. Allowing anonymous user with full access");
//			noUsers = null;
			return new UserPresentation.DummyUser(true);
		}
		String user;
		String password;
		try {
			user = (String) args[0];
			password = (String) args[1];
		} catch (Exception exc) {
			throw new IllegalArgumentException(
					"Only two String argumens supported: username and password.",exc);
		}
		EOEditingContext ec = new EOEditingContext();
		ec.lock();
		try {
			AutUser au = (AutUser)EOUtilities.objectMatchingKeyAndValue(ec,
					AutUser.ENTITY_NAME, AutUser.USER_NAME_KEY, user);
			if(au.credential() != null) { 
				if(au.credential().startsWith(HASH_PREFIX)) { 
					// compare credential with password digest
					String digestString = getPasswordDigest(password);
					if(digestString != null && digestString.equals(au.credential()))
						return new TableUser(au, null);
					throw new AuthenticationFailedException(CREDENTIAL);
				} else if(parentHandler != null) {
					args[0] = au.credential();
					try { 
						UserPresentation pUser = parentHandler.authenticate(args);
						if(!SettingsReader.boolForKeyPath("auth.readFromParent", false))
							pUser = null;
						return new TableUser(au, pUser);
					} catch (AuthenticationFailedException ex) {
						if(ex.getReason() == IDENTITY)
							ex = new AuthenticationFailedException(ERROR,
									"Could not find user in parentLoginHandler", ex);
						throw ex;
					}
				} else {
					throw new AuthenticationFailedException(ERROR,
							"Could not get parent authorisation method");
				}
			}
			return new TableUser(au, null);
		} catch (EOUtilities.MoreThanOneException e) {
			throw new AuthenticationFailedException(ERROR, 
					"Multiple users with the same name", e);
		} catch (com.webobjects.eoaccess.EOObjectNotAvailableException e) {
			if(parentHandler != null) {
				UserPresentation pUser = parentHandler.authenticate(args);
				if(SettingsReader.boolForKeyPath("auth.autoAddUsers", false)){
					try {
						AutUser au = (AutUser)EOUtilities.createAndInsertInstance(ec,
								AutUser.ENTITY_NAME);
						au.setUserName(user);
						au.setCredential(pUser.toString());
						// add groups
						NSArray groups = EOUtilities.objectsForEntityNamed(ec, "UserGroup");
						if(groups == null || groups.count() < 0) {
							
						} else {
							Enumeration enu = groups.objectEnumerator();
							while (enu.hasMoreElements()) {
								EOEnterpriseObject gr = (EOEnterpriseObject) enu.nextElement();
								String ext = (String)gr.valueForKey("externalEquivalent");
								if(ext == null)
									continue;
								if(pUser.isInGroup(ext)) {
									au.addObjectToBothSidesOfRelationshipWithKey(gr,
											AutUser.GROUPS_KEY);
								}
							}
						}
						ec.saveChanges();
						if(!SettingsReader.boolForKeyPath("auth.readFromParent", false))
							pUser = null;
						return new TableUser(au, pUser);
					} catch (Exception ex) {
						throw new AuthenticationFailedException(ERROR,
								"Error autocreating AutUser: " + user, ex);
					}
				} else {
					return pUser;
				}
			} else {
				throw new AuthenticationFailedException(IDENTITY, "Unknown user", e);
			}
		} catch (AuthenticationFailedException e) {
			throw e;
		} catch (Exception e) {
			throw new AuthenticationFailedException(ERROR, "Error authenticating", e);
		} finally {
			ec.unlock();
		}
	}
	
	public static String getPasswordDigest(String password) {
		if(password == null) return null;
		String algorithm = SettingsReader.stringForKeyPath(
				"auth.passwordDigestAlgorithm", "MD5");
		if(algorithm == null || algorithm.length() == 0 || algorithm.equalsIgnoreCase("none"))
			return password;
		try {
			MessageDigest md = MessageDigest.getInstance(algorithm);
			byte[] digest = md.digest(password.getBytes());
			StringBuilder buf = new StringBuilder(digest.length * 2 + 8);
			buf.append(HASH_PREFIX);
			return LoginProcessor.bytesToString(digest, buf);
		} catch (Exception e) {
			throw new IllegalStateException("Error digesting password", e);
		}
	}
}
