package net.rujel.ui;

import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Calendar;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import net.rujel.reusables.SettingsReader;
import net.rujel.reusables.WOLogLevel;

import java.io.File;

import com.apress.practicalwo.practicalutilities.WORequestAdditions;
import com.webobjects.appserver.WOActionResults;
import com.webobjects.appserver.WOContext;
import com.webobjects.appserver.WOComponent;
import com.webobjects.appserver.WOResponse;
import com.webobjects.foundation.NSData;
import com.webobjects.foundation.NSMutableData;
import com.webobjects.foundation.NSPathUtilities;

public class BugReport extends WOComponent {
    public BugReport(WOContext context) {
        super(context);
    }
    
	protected static final Logger logger = Logger.getLogger("rujel");
    
	public WOActionResults getEnironment() {
		NSData enironment = enironment();
		if(enironment == null) {
			session().takeValueForKey("error", "message");
			return null;
		}
		WOResponse response = application().createResponseInContext(context());
		response.setContent(enironment);
		response.setHeader("application/octet-stream","Content-Type");
		StringBuilder buf = new StringBuilder("attachment; filename=\"");
		buf.append(SettingsReader.stringForKeyPath("supportCode", "unregistered"));
		buf.append(".zip\"");
		response.setHeader(buf.toString(),"Content-Disposition");
		return response;
	}
	
    public NSData enironment() {
    	final NSMutableData result = new NSMutableData();
    	OutputStream out = new OutputStream() {
			public void write(int arg0) throws IOException {
				result.appendByte((byte)arg0);
			}
			
			public void write(byte[] b) throws IOException {
				result.appendBytes(b);
			}
		};
		Calendar cal = Calendar.getInstance();
		StringBuilder buf = new StringBuilder(40);
		buf.append(cal.get(Calendar.YEAR)).append(cal.get(Calendar.MONTH));
		buf.append(cal.get(Calendar.DATE)).append('/');
		try {
			ZipOutputStream zipStream = new ZipOutputStream(out);
			zipStream.putNextEntry(new ZipEntry(buf.toString()));
			
			buf.append("info.txt");
			try {
				zipStream.putNextEntry(new ZipEntry(buf.toString()));
				OutputStreamWriter writer = new OutputStreamWriter(zipStream, "utf8");
				writer.write("code: ");
				writer.write(SettingsReader.stringForKeyPath("supportCode", "#"));
				writer.write("\rschool: ");
				writer.write(SettingsReader.stringForKeyPath("schoolName", "???"));
				writer.write("\rversion: ");
				writer.write(System.getProperty("RujelVersion","???"));
				writer.write("\rrevision: ");
				writer.write(System.getProperty("RujelRevision","???"));
				writer.write("\rhost: ");
				writer.write(WORequestAdditions.hostName(context().request()));
				writer.write("\rurl: ");
				writer.write(context().request().applicationURLPrefix());
				writer.flush();
			} finally {
				buf.delete(9, buf.length());
			}

			buf.append("System.properties");
			zipStream.putNextEntry(new ZipEntry(buf.toString()));
			System.getProperties().store(zipStream, null);
			buf.delete(9, buf.length());

			buf.append("logs/");
			zipStream.putNextEntry(new ZipEntry(buf.toString()));
			String logPath = System.getProperty("WOOutputPath");
			if(logPath != null) { // System.out log
				buf.append(NSPathUtilities.lastPathComponent(logPath));
				try {
					zipStream.putNextEntry(new ZipEntry(buf.toString()));
					FileInputStream in = new FileInputStream(logPath);
					copyStream(in, zipStream, 4096);
				} finally {
					buf.delete(14, buf.length());
				}
			}
			
			logPath = LogManager.getLogManager().getProperty(
					"java.util.logging.FileHandler.pattern");
			if(logPath != null) {  // Logger logs
				logPath = logPath.replaceAll("%h", System.getProperty("user.home"));
				File logDir = new File(NSPathUtilities.stringByDeletingLastPathComponent(logPath));
				File[] logs = logDir.listFiles(
						new LogFilenameFiler(NSPathUtilities.lastPathComponent(logPath)));
				if(logs == null || logs.length == 0)
					return null;
				for (int i = 0; i < logs.length; i++) {
					buf.append(logs[i].getName());
					try {
						zipStream.putNextEntry(new ZipEntry(buf.toString()));
						FileInputStream in = new FileInputStream(logs[i]);
						copyStream(in, zipStream, 4096);
					} finally {
						buf.delete(14, buf.length());
					}
				}
			}
			zipStream.closeEntry();
			zipStream.close();
	    	return result;
		} catch (Exception e) {
			logger.log(WOLogLevel.WARNING,"Error constructing environment",
					new Object[] {session(),e});
			return null;
		}
    }

    public static void copyStream(InputStream in, OutputStream out, int bufSize)
    				throws IOException {
    	byte[] buf = new byte[bufSize];
		while (in.available() > 0) {
			int len = in.read(buf);
			out.write(buf, 0, len);
		}
		in.close();
    }
    
    public static class LogFilenameFiler implements FilenameFilter {
    	protected char[] pattern;
    	
    	public LogFilenameFiler(String pattern) {
    		super();
    		this.pattern = pattern.toCharArray();
    	}
    	
		public boolean accept(File dir, String name) {
			int j = 0;
			for (int i = 0; i < pattern.length; i++) {
				char test = name.charAt(j);
				if(pattern[i] != '%') {
					if(test != pattern[i])
						return false;
				} else {
					i++;
					if(pattern[i] == '%') {
						if(test != '%')
							return false;
					} else if (pattern[i] == 'g' || pattern[i] == 'u') {
						if(test < '0' || test > '9')
							return false;
						test = name.charAt(j +1);
						while(test >= '0' && test <= '9') {
							j++;
							test = name.charAt(j +1);
						}
					} else {
						return false;
					}
				}
				j++;
				if(name.length() < j)
					return false;
			}
			return name.length() == j;
		}
    }
    
    public boolean synchronizesVariablesWithBindings() {
        return false;
	}
	
	public boolean isStateless() {
		return true;
	}
	
	public void reset() {
		super.reset();
	}

}