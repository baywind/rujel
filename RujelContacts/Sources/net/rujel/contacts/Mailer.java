// Mailer.java

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

package net.rujel.contacts;

import java.io.*;
import java.text.FieldPosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.logging.Logger;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.mail.*;
import javax.mail.internet.*;

import com.sun.mail.smtp.SMTPTransport;
import com.webobjects.appserver.*;
import com.webobjects.foundation.*;

import net.rujel.reusables.*;

public class Mailer {
	
	protected static final Logger logger = Logger.getLogger("rujel.mail");

	protected SettingsReader settings = SettingsReader.settingsForPath("mail", false);
	protected boolean dontSend = settings.getBoolean("dontSend", false);
	protected boolean writeToFile = settings.getBoolean("writeToFile",false);
	private String prot = "smtp";
	private String mailhost = settings.get("smtpServerURL", null);
	private String user = settings.get("smtpUser", null);

	
	protected Session mailSession;
	
	public Mailer() {
		if(!dontSend) {
			Properties props = System.getProperties();
			//Properties props = new Properties();
			if(settings.getBoolean("secure", false))
				prot = "smtps";
			if(mailhost != null)
			props.put("mail." + prot + ".host", mailhost);
		    if (user != null)
			props.put("mail." + prot + ".auth", "true");
		    
		    mailSession = Session.getInstance(props, null);
		    mailSession.setDebug(settings.getBoolean("debug", false));
		    logger.finer("Constructed mailer");
		}
	}
	
	protected void finalize() throws Throwable {
		super.finalize();
	}

	protected MimeMessage constructMessage (InternetAddress[] to) throws MessagingException {
		MimeMessage msg = new MimeMessage(mailSession);
		String adr = settings.get("mailFrom", null);
		if (adr != null)
			msg.setFrom(new InternetAddress(adr));
		else
			msg.setFrom();

		msg.setRecipients(Message.RecipientType.TO, to);
		adr = settings.get("replyTo", null);
		if(adr != null)
			msg.setReplyTo(InternetAddress.parse(adr, false));
			
		adr = settings.get("cc", null);
		if(adr != null)
			msg.setRecipients(Message.RecipientType.CC, InternetAddress.parse(adr, false));
		adr = settings.get("bcc", null);
		if(adr != null)
			msg.setRecipients(Message.RecipientType.BCC, InternetAddress.parse(adr, false));
		msg.setHeader("X-Mailer", "RUJEL");
		msg.setSentDate(new Date());			
		return msg;
	}

	protected void sendMessage(Message msg) throws MessagingException {
		SMTPTransport t =
			(SMTPTransport)mailSession.getTransport(prot);
		try {
			if (user != null)
				t.connect(mailhost, user, settings.get("smtpPassword", null));
			else
				t.connect();
			t.sendMessage(msg, msg.getAllRecipients());
		} finally {
			if (settings.getBoolean("debug", false))
				logger.log(WOLogLevel.FINE,"SMTP responded:",t.getLastServerResponse());
			t.close();
		}
	}

	public void sendTextMessage(String subject, String text, InternetAddress[] to)
																	throws MessagingException{
		if(!dontSend) {
			//try {
				MimeMessage msg = constructMessage(to);
				msg.setSubject(subject,"UTF-8");
				msg.setText(text,"UTF-8");
				//msg.setDataHandler(new DataHandler(text, "text/plain; charset=\"UTF-8\""));
				//msg.setHeader("Content-Language","ru");
				sendMessage(msg);
				logger.log(WOLogLevel.FINER,"Message was sent",subject);
			/*} catch (MessagingException e) {
				logger.log(WOLogLevel.WARNING,"Error sending message: " + subject,e);
			}*/
		}
		if(writeToFile) {
			NSData message = new NSData(text,"utf8");
			writeToFile(subject + ".txt", message);
		}
	}
	
	public void sendPage(String subject, String text, WOActionResults page ,InternetAddress[] to)
																			throws MessagingException{
		NSData content = page.generateResponse().content();
		StringBuffer name = new StringBuffer(20);
		
		WOSession ses = null;
		if(page instanceof WOComponent) {
			WOComponent cpage = (WOComponent)page;
			if(cpage.context().hasSession())
				ses = cpage.session();
			//name.append(cpage.name());
		}
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
		sdf.format(new Date(), name,new FieldPosition(SimpleDateFormat.YEAR_FIELD));
		name.append(".html");
		if(!dontSend) {
			//try {
				MimeMessage msg = constructMessage(to);
				msg.setSubject(subject,"UTF-8");

				MimeBodyPart mbp1 = new MimeBodyPart();
				if(text == null)
					text = defaultMessageText();
				mbp1.setText(text,"UTF-8");
				//mbp1.setDataHandler(new DataHandler(text, "text/plain; charset=\"UTF-8\""));

				MimeBodyPart mbp2 = new MimeBodyPart();
				DataSource ds = new NSDataSource(name.toString(),content);
			    mbp2.setDataHandler(new DataHandler(ds));
			    mbp2.setFileName(name.toString());
				mbp2.setHeader("Content-Type", "text/html; name=\"" + name + '"');
			    Multipart mp = new MimeMultipart();
			    mp.addBodyPart(mbp1);
			    mp.addBodyPart(mbp2);

			    // add the Multipart to the message
			    msg.setContent(mp);

			    sendMessage(msg);
				Object[] args = new Object[] {ses,subject};
				logger.log(WOLogLevel.FINER,"Message was sent",args);
			/*} catch (MessagingException e) {
				Object[] args = new Object[] {ses,subject,e};
				logger.log(WOLogLevel.WARNING,"Error sending message",args);
			}*/
		}
		if(writeToFile) {
			writeToFile(subject + ".html", content);
		}
	}

	protected String _defaultMessage;
	public String defaultMessageText() {
		if(_defaultMessage == null) {
			_defaultMessage = "";
			/*(String)WOApplication.application().valueForKeyPath(
			"strings.RujelContacts_Contacts.defaultMessage");*/
			String filePath = settings.get("messageFilePath", null);
			if(filePath != null) {
				try {
					InputStream strm = new FileInputStream(filePath);
					InputStreamReader reader = new InputStreamReader(strm,"utf8");
					int size = strm.available();
					char[] cbuf = new char[size];
					size = reader.read(cbuf, 0, size);
					_defaultMessage = new String(cbuf);
				} catch (IOException e) {
					logger.log(WOLogLevel.WARNING,"Error reading default message from file " + filePath,e);
				}
			}
		}
		return _defaultMessage;
	}

	protected static boolean writeToFile(String filename, NSData message) {
		String mailDir = SettingsReader.stringForKeyPath("mail.writeFileDir", null);
		if(mailDir != null) {
			try {
				File messageFile = new File(mailDir,filename);
				FileOutputStream fos = new FileOutputStream(messageFile);
				message.writeToStream(fos);
				fos.close();
				return true;
			} catch (Exception ex) {
				logger.log(WOLogLevel.WARNING,"Failed to write result for " + filename,new Object[] {ex});
				return false;
			}
		} else {
			logger.log(WOLogLevel.FINE,"Mail written to file: " + filename);
			return false;
		}
	}

	protected static class NSDataSource implements DataSource {
		private NSData data;
		private String name;
		private String contentType = "text/html";
		
		public NSDataSource(String aName, NSData content) {
			name = aName;
			data = content;
		}
		
		public java.lang.String getName() {
			return name;
		}
		
		public java.io.InputStream getInputStream() throws IOException {
			return data.stream();
		}

        public java.io.OutputStream getOutputStream() throws IOException {
        	throw new UnsupportedOperationException("Output not supported");
        }
        
        public java.lang.String getContentType() {
        	return contentType;
        }
        
        public void setContentType(String newContentType) {
        	contentType = newContentType;
        }
	}
}
