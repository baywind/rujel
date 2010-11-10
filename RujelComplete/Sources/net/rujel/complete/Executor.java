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
import net.rujel.interfaces.Person;
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
	protected static transient NSMutableDictionary progress = new NSMutableDictionary();
	
	protected static Thread thread;

	public static class Task {
		public boolean writeCourses = false;
		public boolean writeStudents = false;
		public EOGlobalID courseID;
		public EOGlobalID[] studentIDs;
		public Object date;
		public Integer year;

		public void setCourse(EduCourse course) {
			if(course != null) {
				courseID = course.editingContext().globalIDForObject(course);
				year = course.eduYear();
			} else {
				courseID = null;
			}
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
	private WOContext ctx;
	protected Task task;

    protected Executor() {
		super();
//		ctx = MyUtility.dummyContext(null);
	}
	
	public static void exec(Task ex) {
		queue.offer(ex);
		if(thread == null || !thread.isAlive()) {
			thread = new Thread(new Executor(),"CompletionExecutor");
			thread.setPriority(Thread.MIN_PRIORITY + 1);
			thread.start();
			if(progress.count() == 0)
				progress.takeValueForKey("!", "running");
		}
	}

	public static synchronized NSMutableDictionary progress() {
		return progress;
	}
	
	public void run() {
		ctx = MyUtility.dummyContext(null);
		WOSession ses = ctx.session();
		while (queue.size() > 0) {
			try {
				task = queue.poll();
				if(task == null) {
					break;
				}
				progress().takeValueForKey(task, "task");
				ses.takeValueForKey(task.date, "today");
//				MultiECLockManager lm = ((MultiECLockManager.Session)ses).ecLockManager();
//				lm.lock();
				if(task.courseID != null) { // Completion closing
					EOEditingContext ec = ses.defaultEditingContext();
					EduCourse course = (EduCourse)ec.faultForGlobalID(task.courseID, ec);
					if(course != null) {
						StringBuilder buf = new StringBuilder();
//						buf.append(ses.valueForKeyPath(
//								"strings.RujelComplete_Complete.closeTitle"));
//						buf.append(' ');
						buf.append(ses.valueForKeyPath(
								"strings.RujelInterfaces_Names.EduCourse.this"));
						buf.append(' ').append(':').append(' ');
						buf.append(course.eduGroup().name()).append(' ');
						buf.append(course.subjectWithComment()).append(' ');
						buf.append(Person.Utility.fullName(course.teacher(), true, 2, 1, 1));
						progress().takeValueForKey(buf.toString(), "running");
						
					}
					if(task.studentIDs != null) {
						writeStudents(course, ec);
					}
					writeCourse(course);
					progress().removeAllObjects();
				} else { //forced closing
					if(task.writeStudents) {
						FileWriterUtil folder = completeFolder(task.year, STUDENTS, true, true,false);
						{
							StringBuilder buf = new StringBuilder();
							buf.append(ses.valueForKeyPath("strings.RujelComplete_Complete.forced"));
							buf.append(' ').append(':').append(' ');
							buf.append(ses.valueForKeyPath(
									"strings.RujelComplete_Complete.StudentCatalog"));
							progress().takeValueForKey(buf.toString(), "running");
						}
						folder.ctx = ctx;
						StudentCatalog.prepareStudents(folder);
						folder.close();
						progress().removeAllObjects();
					}
					if(task.writeCourses) {
						FileWriterUtil folder = completeFolder(task.year, COURSES, true, true, false);
						{
							StringBuilder buf = new StringBuilder();
							buf.append(ses.valueForKeyPath("strings.RujelComplete_Complete.forced"));
							buf.append(' ').append(':').append(' ');
							buf.append(ses.valueForKeyPath(
									"strings.RujelComplete_Complete.CourseCatalog"));
							progress().takeValueForKey(buf.toString(), "running");
						}
						folder.ctx = ctx;
						CoursesCatalog.prepareCourses(folder, null, true);
						folder.close();
						progress().removeAllObjects();
					}
				}
//				lm.unlock();
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
		EduGroup gr = course.eduGroup();
		NSArray courses = null;
		if(gr != null) {
			courses = EOUtilities.objectsWithQualifierFormat(ec,
					EduCourse.entityName, "eduGroup = %@ AND eduYear = %d",
					new NSArray(new Object[] {gr, task.year}));
			NSArray found = Completion.findCompletions(courses, 
					NSKeyValueCoding.NullValue, "student", Boolean.FALSE, ec);
			if(found != null && found.count() > 0) {
				Enumeration enu = found.objectEnumerator();
				while (enu.hasMoreElements()) {
					Completion cpt = (Completion) enu.nextElement();
					NSArray aud = (NSArray)cpt.valueForKeyPath("course.audience");
					if(aud == null || aud.count() == 0) {
						return;
					}
				}
			}
		}
		FileWriterUtil folder = completeFolder(task.year, STUDENTS, false, false, true);
		folder.ctx = ctx;
		FolderCatalog catalog = new FolderCatalog(folder.getBase(), ctx.session());
		NSMutableArray reports = (NSMutableArray)ctx.session().valueForKeyPath(
				"modules.studentReporter");
		reports.insertObjectAtIndex(WOApplication.application().valueForKeyPath(
				"strings.Strings.Overview.defaultReporter"),0);
//		File groupDir = new File(folder,grDir);
		NSMutableDictionary grDict = null;
		if(gr != null) {
			String grDir = StudentCatalog.groupDirName(gr,false);
			grDict = (NSMutableDictionary)catalog.valueForKey(grDir);
			if(grDict == null)
				grDict = new NSMutableDictionary();
			folder.enterDir(grDir, true);
		}
		boolean updateList = false;
		for (int i = 0; i < task.studentIDs.length; i++) {
			Student student = (Student)ec.faultForGlobalID(task.studentIDs[i], ec);
			if(gr == null || !gr.list().containsObject(student)) {
				gr = student.recentMainEduGroup();
				if(grDict != null) {
					folder.leaveDir();
					if(grDict.count() > 0) {
						catalog.takeValueForKey(grDict, folder.currDir().getName());
						updateList = true;
					}
				}
				String grDir = StudentCatalog.groupDirName(gr,false);
				courses = EOUtilities.objectsWithQualifierFormat(ec,
						EduCourse.entityName, "eduGroup = %@ AND eduYear = %d",
						new NSArray(new Object[] {gr, task.year}));
				grDict = (NSMutableDictionary)catalog.valueForKey(grDir);
				if(grDict == null)
					grDict = new NSMutableDictionary();
				folder.enterDir(grDir, true);
			}
			String key = ((EOKeyGlobalID)task.studentIDs[i]).keyValues()[0].toString();
//			File stDir = new File(groupDir,key);
			if(Completion.studentIsReady(student, gr, task.year)) {
				StudentCatalog.completeStudent(gr, student, reports,
						courses, folder, true);
				grDict.takeValueForKey(Boolean.TRUE, key);
			} else {
				File stDir = new File(folder.currDir(),key);
				if(stDir.exists()){
					grDict.takeValueForKey(Boolean.FALSE, key);
					File[] files = stDir.listFiles();
					for (int j = 0; j < files.length; j++) {
						files[j].delete();
					}
					stDir.delete();
				}
			}
		}
		if(grDict != null) {
			if(grDict.count() > 0) {
				catalog.takeValueForKey(grDict, folder.currDir().getName());
				updateList = true;
			}
			folder.leaveDir();
		}
		if(updateList) {
			File file = new File(folder.getBase(),"index.html");
			if(!file.exists())
				prepareFolder(folder,"list.html");
			NSArray groups = EduGroup.Lister.listGroups(
					(NSTimestamp)ctx.session().valueForKey("today"), ec);
			WOComponent page = WOApplication.application().pageWithName("StudentCatalog", ctx);
			page.takeValueForKey(ec, "ec");
			page.takeValueForKey(catalog, "catalog");
			page.takeValueForKey(groups, "eduGroups");
			folder.writeFile("list.html", page);
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
		FileWriterUtil folder = Executor.completeFolder(task.year, COURSES,false,false,true);
		folder.ctx = ctx;
		File file = new File(folder.getBase(),"index.html");
		if(!file.exists())
			prepareFolder(folder, "eduGroup.html");
		FolderCatalog catalog = new FolderCatalog(folder.getBase(), ctx.session());
		String crID = ((EOKeyGlobalID)task.courseID).keyValues()[0].toString();
		catalog.takeValueForKey(ready, crID);
		CoursesCatalog.prepareCourses(folder, catalog, false);
//		file = new File(folder,crID);
		CoursePage.printCourseReports(course, folder, crID, null, ready);
		catalog.writeCatalog();
	}

    public static FileWriterUtil completeFolder(Integer year, String type, 
    		boolean date, boolean zip, boolean overwrite) {
    	String completeDir = SettingsReader.stringForKeyPath("edu."+ type + "CompleteDir", null);
    	if(completeDir == null) {
    		completeDir = SettingsReader.stringForKeyPath("edu.completeDir", null);
    	} else {
    		type = null;
    	}
    	completeDir = Various.convertFilePath(completeDir);
//    	if(year == null)
//    		return new File(completeDir);
    	if(completeDir == null)
    		return null;
		StringBuilder buf = new StringBuilder(year.toString());
		buf.append('-').append(Integer.toString(year.intValue() +1).substring(2));
    	try {
    		if(type != null)
    			buf.append(type);
    		if(date) {
    			buf.append('_');
    			Calendar cal = Calendar.getInstance();
    			buf.append(cal.get(Calendar.YEAR));
    			int idx = cal.get(Calendar.MONTH) +1;
    			if(idx < 10)
    				buf.append('0');
    			buf.append(idx);
    			idx = cal.get(Calendar.DAY_OF_MONTH);
    			if(idx < 10)
    				buf.append('0');
    			buf.append(idx);
    		}
    		String name = buf.toString();
    		if(zip)
    			buf.append(".zip");
			File folder = new File(completeDir,buf.toString());
			FileWriterUtil result = new FileWriterUtil(folder, zip, overwrite);
			if(zip)
				result.enterDir(name, true);
			return result;
		} catch (Exception e) {
			logger.log(WOLogLevel.WARNING,"Could not get completeFolder " + buf,e);
		}
    	return null;
    }
    
    protected static void createIndex(FileWriterUtil folder, String title,String list) {
////    	File file = new File(folder,"index.html");
//    	if(file.exists())
//    		return file;
    	try {
			InputStream str = WOApplication.application().resourceManager().
					inputStreamForResourceNamed("index.html", "RujelComplete", null);
			BufferedReader reader = new BufferedReader(new InputStreamReader(str,"utf8"));
//			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
//					new FileOutputStream(file),"utf8"));
			NSMutableData content = new NSMutableData();
			while (reader.ready()) {
				String line = reader.readLine();
				if(line == null)
					break;
				line = line.replace("$title", title);
				line = line.replace("$list", list);
//				writer.write(line);
//				writer.newLine();
				content.appendBytes(line.getBytes("utf8"));
			}
			reader.close();
			folder.writeData("index.html", content);
//			writer.close();
		} catch (IOException e) {
			logger.log(WOLogLevel.WARNING,"Error preparing index file",e);
		}
//    	return file;
    }
    
    protected static void copyResource(FileWriterUtil folder, String fileName) {
//    	File file = new File(folder, fileName);
//    	if(file.exists())
//    		return file;
//    	try {
			byte[] fileBites = WOApplication.application().resourceManager().
			bytesForResourceNamed(fileName, "RujelComplete", null);
			folder.writeData(fileName, new NSData(fileBites));
//			FileOutputStream fos = new FileOutputStream(file);
//			fos.write(fileBites);
//			fos.close();
//		} catch (IOException e) {
//			logger.log(WOLogLevel.WARNING,"Could not copy resource file: " + fileName,e);
//		}
//    	return file;
    }
    
    public static void prepareFolder(FileWriterUtil folder, String listName) {
		Integer year = (Integer) folder.ctx.session().valueForKey("eduYear");
		Executor.createIndex(folder, MyUtility.presentEduYear(year), listName);
		copyResource(folder,"scripts.js");
		copyResource(folder,"styles.css");
    }
}
