// Executor.java

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

package net.rujel.complete;

import java.io.*;
import java.util.Enumeration;
import java.util.logging.Logger;

import net.rujel.base.MyUtility;
import net.rujel.interfaces.*;
import net.rujel.reusables.*;

import com.webobjects.appserver.*;
import com.webobjects.eoaccess.EOUtilities;
import com.webobjects.eocontrol.EOEditingContext;
import com.webobjects.eocontrol.EOKeyGlobalID;
import com.webobjects.foundation.*;

public class Executor implements Runnable {
	public static Logger logger = Logger.getLogger("rujel.complete");
	
    public WOContext ctx;
    public Integer year;
    public File folder;
    public boolean writeReports = true;

    public Executor() {
		super();
	}
	
	public Executor(Integer eduYear) {
		super();
		year = eduYear;
		folder = completeFolder(year);
		ctx = MyUtility.dummyContext(null);
	}
	
	public static void exec(Executor ex) {
		Thread t = new Thread(ex,"EMailBroadcast");
		t.setPriority(Thread.MIN_PRIORITY + 1);
		t.start();
	}

	public void run() {
		prepareStructure();
	}

    public static File completeFolder(Integer year) {
    	String completeDir = SettingsReader.stringForKeyPath("edu.completeDir", null);
    	completeDir = Various.convertFilePath(completeDir);
    	if(completeDir == null)
    		return null;
    	try {
			File dir = new File(completeDir);
			if(!dir.exists())
				dir.mkdirs();
			File folder = new File(dir,year.toString());
			if(!folder.exists())
				folder.mkdir();
    		File file = new File(folder,"index.html");
    		if(!file.exists()) {
    			InputStream str = WOApplication.application().resourceManager().
    					inputStreamForResourceNamed("index.html", "RujelComplete", null);
    			BufferedReader reader = new BufferedReader(new InputStreamReader(str,"utf8"));
    			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
    					new FileOutputStream(file),"utf8"));
    			String eduYear = MyUtility.presentEduYear(year.intValue());
    			while (reader.ready()) {
					String line = reader.readLine();
					if(line == null)
						break;
					line = line.replace("$eduYear", eduYear);
					writer.write(line);
					writer.newLine();
				}
    			reader.close();
    			writer.close();
    		}
    		file = new File(folder,"scripts.js");
    		if(!file.exists()) {
    			byte[] fileBites = WOApplication.application().resourceManager().
    					bytesForResourceNamed("scripts.js", "RujelComplete", null);
    			FileOutputStream fos = new FileOutputStream(file);
    			fos.write(fileBites);
    			fos.close();
    		}
    		file = new File(folder,"styles.css");
    		if(!file.exists()) {
    			byte[] fileBites = WOApplication.application().resourceManager().
    					bytesForResourceNamed("styles.css", "RujelComplete", null);
    			FileOutputStream fos = new FileOutputStream(file);
    			fos.write(fileBites);
    			fos.close();
    		}
    		return folder;
		} catch (Exception e) {
			logger.log(WOLogLevel.WARNING,"Could not get copleteFolder for year " + year,e);
		}
    	return null;
    }
    
    protected static void writeFile(File folder, CharSequence filename, WOComponent page)  {
    	try {
    		File file = new File(folder,filename.toString());
    		NSData content = page.generateResponse().content();
    		FileOutputStream fos = new FileOutputStream(file);
    		content.writeToStream(fos);
    		fos.close();
    	} catch (Exception e) {
    		logger.log(WOLogLevel.WARNING,"Error writing file " + folder.getAbsolutePath() +
    				'/' + filename,e);
    	}
    }
        
    public void prepareStructure() {
		WOSession ses = ctx.session(); 
		EOEditingContext ec = new SessionedEditingContext(ses);
    	try {
    		prepareStudents(ec, ses);
    		ses.terminate();
    	} catch (Exception e) {
    		logger.log(WOLogLevel.WARNING,"Error preparing close year structure for year "
    				+ year,new Object[] {ses,e});
    		throw new NSForwardException(e);
		}
    }
    
    protected void prepareStudents(EOEditingContext ec, WOSession ses) {
   		File subFolder = new File(folder,"students");
		if(!subFolder.exists())
			subFolder.mkdirs();
		NSArray groups = EduGroup.Lister.listGroups(
				(NSTimestamp)ses.valueForKey("today"), ec);
		WOComponent page = WOApplication.application().pageWithName("StudentCatalog", ctx);
		page.takeValueForKey(ec, "ec");
		page.takeValueForKey(groups, "eduGroups");
		
		writeFile(subFolder, "list.html", page);
		Enumeration grenu = groups.objectEnumerator();
		NSMutableArray reports = (NSMutableArray)ses.valueForKeyPath(
				"modules.studentReporter");
		NSDictionary reporter = (NSDictionary)WOApplication.application().valueForKeyPath(
				"strings.Strings.Overview.defaultReporter");
		reports.insertObjectAtIndex(reporter,0);
		StringBuilder filename = new StringBuilder(12);
		while (grenu.hasMoreElements()) {
			EduGroup gr = (EduGroup) grenu.nextElement();
			EOKeyGlobalID gid = (EOKeyGlobalID)ec.globalIDForObject(gr);
			filename.append(gr.grade()).append('_');
			filename.append(gid.keyValues()[0]);
			File grDir = new File(subFolder,filename.toString());
			filename.delete(0, filename.length());
			Enumeration stenu = gr.list().objectEnumerator();
			NSArray args = new NSArray(new Object[] {year, gr });
			NSArray existingCourses = EOUtilities.objectsWithQualifierFormat(ec,
					EduCourse.entityName,"eduYear = %d AND eduGroup = %@",args);
			while (stenu.hasMoreElements()) {
				Student student = (Student) stenu.nextElement();
				gid = (EOKeyGlobalID)ec.globalIDForObject(student);
				File stDir = new File(grDir,gid.keyValues()[0].toString());
				if(stDir.exists())
					continue;
				stDir.mkdirs();
				page = WOApplication.application().pageWithName("StudentPage", ctx);
	    		page.takeValueForKey(student, "student");
	    		page.takeValueForKey(gr, "eduGroup");
	    		page.takeValueForKey(reports, "reports");
	    		writeFile(stDir, "index.html", page);
	    		if(!writeReports)
	    			continue;
				Enumeration repEnu = reports.objectEnumerator();
				while (repEnu.hasMoreElements()) {
					reporter = (NSDictionary) repEnu.nextElement();
					page = WOApplication.application().pageWithName("PrintReport",ctx);
					page.takeValueForKey(reporter,"reporter");
					page.takeValueForKey(existingCourses,"courses");
					page.takeValueForKey(new NSArray(student),"students");
					filename.append(reporter.valueForKey("id")).append(".html");
		    		writeFile(stDir, filename, page);
					filename.delete(0, filename.length());
				}
			}
		}
	}
}
