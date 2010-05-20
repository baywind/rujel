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
import java.util.logging.Logger;

import net.rujel.base.MyUtility;
import net.rujel.interfaces.EduCourse;
import net.rujel.interfaces.Student;
import net.rujel.reusables.*;

import com.webobjects.appserver.*;
import com.webobjects.eocontrol.EOGlobalID;
import com.webobjects.foundation.*;

public class Executor implements Runnable {
	public static Logger logger = Logger.getLogger("rujel.complete");
	
    public WOContext ctx;
//    public Integer year;
//    public File folder;
    public boolean writeReports = false;
    public File studentsFolder;
    public File coursesFolder;
    public EOGlobalID courseID;
    public EOGlobalID[] studentIDs;

    public Executor() {
		super();
	}
	
	public Executor(NSTimestamp date) {
		super();
		ctx = MyUtility.dummyContext(null);
		WOSession ses = ctx.session();
		ses.takeValueForKey(date, "today");
//		Integer year = MyUtility.eduYearForDate(date);
//		folder = completeFolder(year);
	}
	
	public static void exec(Executor ex) {
		if(ex.ctx == null)
			throw new IllegalStateException("Executor was not properly initialsed");
		MultiECLockManager lm = ((MultiECLockManager.Session)ex.ctx.session())
							.ecLockManager();
//		ex.ctx.session().defaultEditingContext().unlock();
		lm.unlock();
		Thread t = new Thread(ex,"Complete");
		t.setPriority(Thread.MIN_PRIORITY + 1);
		t.start();
	}

	public void run() {
		try {
			MultiECLockManager lm = ((MultiECLockManager.Session)ctx.session()).ecLockManager();
			lm.lock();
//			ctx.session().defaultEditingContext().lock();
			if(studentsFolder != null)
				StudentCatalog.prepareStudents(studentsFolder, ctx);
			if(coursesFolder != null)
				CoursesCatalog.prepareCourses(coursesFolder, ctx);
			lm.unlock();
		} catch (RuntimeException e) {
			logger.log(WOLogLevel.WARNING,"Error in Complete",new Object[] {ctx.session(),e});
		} finally {
//			ctx.session().defaultEditingContext().unlock();
			ctx.session().terminate();
		}
	}
	
	public void setCourse(EduCourse course) {
			courseID = (course == null)?null: course.editingContext().globalIDForObject(course);
	}
	
	public void setStudents(NSArray students) {
		if(students == null || students.count() == 0) {
			studentIDs = null;
			return;
		}
		studentIDs = new EOGlobalID[students.count()];
		for (int i = 0; i < studentIDs.length; i++) {
			Student student = (Student)students.objectAtIndex(i);
			studentIDs[i] = student.editingContext().globalIDForObject(student);
		}
	}

    public static File completeFolder(Object year, String type) {
    	String completeDir = SettingsReader.stringForKeyPath("edu."+ type + "CompleteDir", null);
    	if(completeDir == null) {
    		completeDir = SettingsReader.stringForKeyPath("edu.completeDir", null);
    	} else {
    		type = null;
    	}
    	completeDir = Various.convertFilePath(completeDir);
    	if(completeDir == null)
    		return null;
    	try {
    		String name = year.toString();
    		if(type != null)
    			name = name + type;
			File folder = new File(completeDir,name);
			if(!folder.exists())
				folder.mkdirs();
//			createIndex(folder, MyUtility.presentEduYear(year), src);
//    		copyResource(folder,"scripts.js");
//    		copyResource(folder,"styles.css");
    		return folder;
		} catch (Exception e) {
			logger.log(WOLogLevel.WARNING,"Could not get completeFolder for year " + year,e);
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
    
    public static void prepareFolder(File folder, WOContext ctx, String listName) {
		Integer year = (Integer) ctx.session().valueForKey("eduYear");
		if(!folder.exists())
			folder.mkdirs();
		Executor.createIndex(folder, MyUtility.presentEduYear(year), listName);
		Executor.copyResource(folder,"scripts.js");
		Executor.copyResource(folder,"styles.css");
    }
    
    protected static void writeFile(File folder, String filename, WOComponent page,boolean overwrite)  {
		File file = new File(folder,filename);
		writeFile(file, page, overwrite);
    }
	protected static void writeFile(File file, WOComponent page,boolean overwrite)  {
    	try {
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
    		logger.log(WOLogLevel.WARNING,"Error writing file " + file.getAbsolutePath(),e);
    	}
    }
}
