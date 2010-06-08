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
import java.util.Calendar;
import java.util.Enumeration;
import java.util.logging.Logger;
import java.util.concurrent.ConcurrentLinkedQueue;

import net.rujel.base.MyUtility;
import net.rujel.interfaces.EduCourse;
import net.rujel.interfaces.EduGroup;
import net.rujel.interfaces.Student;
import net.rujel.reusables.*;

import com.webobjects.appserver.*;
import com.webobjects.eoaccess.EOUtilities;
import com.webobjects.eocontrol.EOEditingContext;
import com.webobjects.eocontrol.EOGlobalID;
import com.webobjects.eocontrol.EOKeyGlobalID;
import com.webobjects.foundation.*;

public class Executor implements Runnable {
	public static final Logger logger = Logger.getLogger("rujel.complete");
	public static final String COURSES = "courses";
	public static final String STUDENTS = "students";

	protected static transient ConcurrentLinkedQueue<Task> queue = 
		new ConcurrentLinkedQueue<Task>();
	protected static Thread thread;
		
	public static class Task {
		public boolean writeReports = false;
		public File studentsFolder;
		public File coursesFolder;
		public EOGlobalID courseID;
		public EOGlobalID[] studentIDs;
		public Object date;

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
	}
	public WOContext ctx;
	protected Task task;

    protected Executor() {
		super();
//		ctx = MyUtility.dummyContext(null);
	}
	
	public static void exec(Task ex) {
//		if(ex.ctx == null)
//			throw new IllegalStateException("Executor was not properly initialsed");
//		MultiECLockManager lm = ((MultiECLockManager.Session)ex.ctx.session())
//							.ecLockManager();
//		ex.ctx.session().defaultEditingContext().unlock();
//		lm.unlock();
		queue.offer(ex);
		if(thread == null || !thread.isAlive()) {
			thread = new Thread(new Executor());
			thread.setPriority(Thread.MIN_PRIORITY + 1);
			thread.start();
		}
	}

	public void run() {
		ctx = MyUtility.dummyContext(null);
		WOSession ses = ctx.session();
		while (queue.size() > 0) {
			try {
				task = queue.poll();
				if(task == null)
					break;
				ses.takeValueForKey(task.date, "today");
				MultiECLockManager lm = ((MultiECLockManager.Session)ses).ecLockManager();
				lm.lock();
				if(task.courseID != null) { // Completion closing
					EOEditingContext ec = ses.defaultEditingContext();
					EduCourse course = (EduCourse)ec.faultForGlobalID(task.courseID, ec);
					if(task.studentIDs != null) {
						writeStudents(course, ec);
					}
					//				if(studentIDs == null) {
					writeCourse(course);
					//				}
				} else if(task.writeReports) { //forced closing
					if(task.studentsFolder != null)
						StudentCatalog.prepareStudents(task.studentsFolder, ctx);
					if(task.coursesFolder != null)
						CoursesCatalog.prepareCourses(task.coursesFolder, ctx, null, true);
				}
				lm.unlock();
			} catch (RuntimeException e) {
				logger.log(WOLogLevel.WARNING,"Error in Completion process"
						,new Object[] {ses,e});
			}
		}
		ses.terminate();
		thread = null;
	}
	
	public static class FolderCatalog  implements NSKeyValueCoding{
		protected File plist;
		protected NSMutableDictionary catalog;
		protected WOSession session;
		
		public FolderCatalog(File folder, WOSession ses) {
			plist = new File(folder,"catalog.plist");
			if(plist.exists()) {
				try {
					FileInputStream fis = new FileInputStream(plist);
					NSData data = new NSData(fis,(int)plist.length());
					catalog = (NSMutableDictionary)NSPropertyListSerialization.
						propertyListFromData(data,"utf8");
					fis.close();
				} catch (Exception e) {
					logger.log(WOLogLevel.WARNING,"Error reading catalog.plist",
							new Object[] {ses,e});
				}
			}
			if(catalog == null)
				catalog = new NSMutableDictionary();
			session = ses;
		}
		
		public void writeCatalog() {
			try {
				NSData data = NSPropertyListSerialization.dataFromPropertyList(
						catalog, "utf8");
				FileOutputStream fos = new FileOutputStream(plist);
				data.writeToStream(fos);
				fos.close();
			} catch (Exception e) {
				logger.log(WOLogLevel.WARNING,"Error writing catalog.plist",
						new Object[] {session,e});
			}
		}

		public void takeValueForKey(Object value, String key) {
			catalog.takeValueForKey(value, key);
		}

		public Object valueForKey(String key) {
			return catalog.valueForKey(key);
		}
	}
	
	protected void writeStudents(EduCourse course, EOEditingContext ec) {
		Integer year = course.eduYear();
		EduGroup gr = course.eduGroup();
		NSArray courses = EOUtilities.objectsWithQualifierFormat(ec,
				EduCourse.entityName, "eduGroup = %@ AND eduYear = %d",
				new NSArray(new Object[] {gr, year}));
		NSArray found = Completion.findCompletions(courses, 
				NSKeyValueCoding.NullValue, "student", Boolean.FALSE, ec);
		if(found != null && found.count() > 0) {
			Enumeration enu = found.objectEnumerator();
			while (enu.hasMoreElements()) {
				Completion cpt = (Completion) enu.nextElement();
				NSArray aud = (NSArray)cpt.valueForKeyPath("course.audience");
				if(aud == null || aud.count() == 0) {
/*					Object subj = cpt.valueForKeyPath("course.subjectWithComment");
//					subj.toString();
					found = Completion.findCompletions(course,null,"student",Boolean.FALSE, ec);
					if(found == null || found.count() == 0)
						studentIDs = null;*/
					return;
				}
			}
		}
		File folder = completeFolder(year, STUDENTS, false);
		FolderCatalog catalog = new FolderCatalog(folder, ctx.session());
		String grDir = StudentCatalog.groupDirName(gr);
		NSMutableDictionary grDict = (NSMutableDictionary)catalog.valueForKey(grDir);
		if(grDict == null) {
			grDict = new NSMutableDictionary();
		}
		NSMutableArray reports = (NSMutableArray)ctx.session().valueForKeyPath(
				"modules.studentReporter");
		reports.insertObjectAtIndex(WOApplication.application().valueForKeyPath(
				"strings.Strings.Overview.defaultReporter"),0);
		File groupDir = new File(folder,grDir);
		for (int i = 0; i < task.studentIDs.length; i++) {
			Student student = (Student)ec.faultForGlobalID(task.studentIDs[i], ec);
			String key = ((EOKeyGlobalID)task.studentIDs[i]).keyValues()[0].toString();
			File stDir = new File(groupDir,key);
			if(Completion.studentIsReady(student, null, year)) {
				StudentCatalog.completeStudent(gr, student, reports,
						courses, stDir, ctx, true);
				grDict.takeValueForKey(Boolean.TRUE, key);
			} else if(stDir.exists()){
				grDict.takeValueForKey(Boolean.FALSE, key);
				File[] files = stDir.listFiles();
				for (int j = 0; j < files.length; j++) {
					files[j].delete();
				}
				stDir.delete();
			}
		}
		if(grDict.count() > 0) {
			catalog.takeValueForKey(grDict, grDir);
			File file = new File(folder,"index.html");
			if(!file.exists())
				prepareFolder(folder, ctx, "list.html");
			NSArray groups = EduGroup.Lister.listGroups(
					(NSTimestamp)ctx.session().valueForKey("today"), ec);
			WOComponent page = WOApplication.application().pageWithName("StudentCatalog", ctx);
			page.takeValueForKey(ec, "ec");
			page.takeValueForKey(catalog, "catalog");
			page.takeValueForKey(groups, "eduGroups");
			Executor.writeFile(folder, "list.html", page,true);
			catalog.writeCatalog();
		}
/*		found = Completion.findCompletions(course,null,"student",Boolean.FALSE, ec);
		if(found == null || found.count() == 0)
			studentIDs = null;*/
	}
	
	protected void writeCourse(EduCourse course) {
		NSArray reports = (NSArray)ctx.session().valueForKeyPath("modules.courseComplete");
		if(reports == null || reports.count() == 0)
			return;
		NSDictionary ready = CoursePage.readyModules(course, reports);
		File folder = Executor.completeFolder(course.eduYear(), "courses",false);
		File file = new File(folder,"index.html");
		if(!file.exists())
			prepareFolder(folder, ctx, "eduGroup.html");
		FolderCatalog catalog = new FolderCatalog(folder, ctx.session());
		String crID = ((EOKeyGlobalID)task.courseID).keyValues()[0].toString();
		catalog.takeValueForKey(ready, crID);
		CoursesCatalog.prepareCourses(folder, ctx, catalog, false);
		file = new File(folder,crID);
		CoursePage.printCourseReports(course, file, ctx, null, ready);
		catalog.writeCatalog();
	}

    public static File completeFolder(Object year, String type, boolean date) {
    	String completeDir = SettingsReader.stringForKeyPath("edu."+ type + "CompleteDir", null);
    	if(completeDir == null) {
    		completeDir = SettingsReader.stringForKeyPath("edu.completeDir", null);
    	} else {
    		type = null;
    	}
    	completeDir = Various.convertFilePath(completeDir);
    	if(year == null)
    		return new File(completeDir);
    	if(completeDir == null)
    		return null;
    	try {
    		String name = year.toString();
    		if(type != null)
    			name = name + type;
    		if(date) {
    			StringBuilder buf = new StringBuilder(name);
    			buf.append('_');
    			Calendar cal = Calendar.getInstance();
    			buf.append(cal.get(Calendar.YEAR));
    			buf.append(cal.get(Calendar.MONTH));
    			buf.append(cal.get(Calendar.DAY_OF_MONTH));
    			name = buf.toString();
    		}
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
    
    protected static void writeFile(File folder, String filename,
    		WOComponent page,boolean overwrite)  {
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
