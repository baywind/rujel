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
//    public Integer year;
    public File folder;
    public boolean writeReports = false;

    public Executor() {
		super();
	}
	
	public Executor(NSTimestamp date) {
		super();
		ctx = MyUtility.dummyContext(null);
		WOSession ses = ctx.session();
		ses.takeValueForKey(date, "today");
		Integer year = MyUtility.eduYearForDate(date);
		folder = completeFolder(year);
	}
	
	public static void exec(Executor ex) {
		if(ex.ctx == null || ex.folder == null)
			throw new IllegalStateException("Executor was not properly initialsed");
		Thread t = new Thread(ex,"EMailBroadcast");
		t.setPriority(Thread.MIN_PRIORITY + 1);
		t.start();
	}

	public void run() {
		try {
			prepareStudents();
		} catch (RuntimeException e) {
			logger.log(WOLogLevel.WARNING,"Error in Complete",new Object[] {ctx.session(),e});
		} finally {
			ctx.session().terminate();
		}
	}

    public static File completeFolder(Integer year) {
    	String completeDir = SettingsReader.stringForKeyPath("edu.completeDir", null);
    	completeDir = Various.convertFilePath(completeDir);
    	if(completeDir == null)
    		return null;
    	try {
			File folder = new File(completeDir,year.toString());
			if(!folder.exists())
				folder.mkdirs();
//			createIndex(folder, MyUtility.presentEduYear(year), src);
//    		copyResource(folder,"scripts.js");
//    		copyResource(folder,"styles.css");
    		return folder;
		} catch (Exception e) {
			logger.log(WOLogLevel.WARNING,"Could not get copleteFolder for year " + year,e);
		}
    	return null;
    }
    
    protected static File createIndex(File folder, String title,String list) {
    	File file = new File(folder,"index.html");
    	if(file.exists())
    		return file;
    	try {
			InputStream str = WOApplication.application().resourceManager().
					inputStreamForResourceNamed("index.html", "RujelComplete", null);
			BufferedReader reader = new BufferedReader(new InputStreamReader(str,"utf8"));
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
					new FileOutputStream(file),"utf8"));
			while (reader.ready()) {
				String line = reader.readLine();
				if(line == null)
					break;
				line = line.replace("$title", title);
				line = line.replace("$list", list);
				writer.write(line);
				writer.newLine();
			}
			reader.close();
			writer.close();
		} catch (IOException e) {
			logger.log(WOLogLevel.WARNING,"Error preparing index file",e);
		}
    	return file;
    }
    
    protected static File copyResource(File folder, String fileName) {
    	File file = new File(folder, fileName);
    	if(file.exists())
    		return file;
    	try {
			byte[] fileBites = WOApplication.application().resourceManager().
			bytesForResourceNamed(fileName, "RujelComplete", null);
			FileOutputStream fos = new FileOutputStream(file);
			fos.write(fileBites);
			fos.close();
		} catch (IOException e) {
			logger.log(WOLogLevel.WARNING,"Could not copy resource file: " + fileName,e);
		}
    	return file;
    }
    
    protected static void writeFile(File folder, String filename, WOComponent page,boolean overwrite)  {
    	try {
    		File file = new File(folder,filename);
    		if(file.exists()) {
    			if(overwrite)
    				file.delete();
    			else
    				return;
    		}
    		FileOutputStream fos = new FileOutputStream(file);
    		NSData content = page.generateResponse().content();
    		content.writeToStream(fos);
    		fos.close();
    	} catch (Exception e) {
    		logger.log(WOLogLevel.WARNING,"Error writing file " + folder.getAbsolutePath() +
    				'/' + filename,e);
    	}
    }

    public void prepareStudents() {
    	EOEditingContext ec = new SessionedEditingContext(ctx.session());
   		File subFolder = new File(folder,"students");
		if(!subFolder.exists())
			subFolder.mkdirs();
		Integer year = (Integer) ctx.session().valueForKey("eduYear");
		createIndex(subFolder, MyUtility.presentEduYear(year), "list.html");
		copyResource(subFolder,"scripts.js");
		copyResource(subFolder,"styles.css");
		
		NSArray groups = EduGroup.Lister.listGroups(
				(NSTimestamp)ctx.session().valueForKey("today"), ec);
		WOComponent page = WOApplication.application().pageWithName("StudentCatalog", ctx);
		page.takeValueForKey(ec, "ec");
		page.takeValueForKey(groups, "eduGroups");
		
		writeFile(subFolder, "list.html", page,false);
		Enumeration grenu = groups.objectEnumerator();
		NSMutableArray reports = (NSMutableArray)ctx.session().valueForKeyPath(
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
				if(!stDir.exists())
					stDir.mkdirs();
				page = WOApplication.application().pageWithName("StudentPage", ctx);
	    		page.takeValueForKey(student, "student");
	    		page.takeValueForKey(gr, "eduGroup");
	    		page.takeValueForKey(reports, "reports");
	    		writeFile(stDir, "index.html", page,false);
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
		    		writeFile(stDir, filename.toString(), page,false);
					filename.delete(0, filename.length());
				}
			}
		}
	}
}
