/**
 * ImportServiceSoap.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package ru.mos.dnevnik;

public interface ImportServiceSoap extends java.rmi.Remote {
    public long insertTimeTable(java.lang.String schoolGuid, java.lang.String name, ru.mos.dnevnik.DayTimeTable[] dayTimeTableCollection) throws java.rmi.RemoteException;
    public long insertReportingPeriodGroup(java.lang.String schoolGuid, java.lang.String name, ru.mos.dnevnik.EduReportingPeriodType type, int studyYear) throws java.rmi.RemoteException;
    public long insertReportingPeriod(long periodGroupID, java.lang.String name, org.apache.axis.types.UnsignedByte number, java.util.Calendar dateStart, java.util.Calendar dateFinish) throws java.rmi.RemoteException;
    public long insertGroup(java.lang.String guid, java.lang.String schoolGuid, java.lang.String name, org.apache.axis.types.UnsignedByte parallel, int studyYear, long periodGroupID, java.lang.String description, long tableID) throws java.rmi.RemoteException;
    public long insertSubGroup(java.lang.String guid, java.lang.String name, java.lang.String parentGroupGuid, java.lang.String description) throws java.rmi.RemoteException;
    public ru.mos.dnevnik.InsertLessonResult insertLesson(java.lang.String groupGuid, long subjectID, java.lang.String teacherGuid, java.util.Calendar date, org.apache.axis.types.UnsignedByte number) throws java.rmi.RemoteException;
    public long insertWork(long lessonID, ru.mos.dnevnik.EduWorkType workType, ru.mos.dnevnik.EduMarkType markType, org.apache.axis.types.UnsignedByte markCount, java.lang.String title, java.lang.String description) throws java.rmi.RemoteException;
    public long insertMark(long workID, java.lang.String personGuid, java.math.BigDecimal value, ru.mos.dnevnik.EduMarkType markType, org.apache.axis.types.UnsignedByte number, ru.mos.dnevnik.MarkBonusType bonus, java.lang.String description) throws java.rmi.RemoteException;
    public long insertGroupMembership(java.lang.String personGuid, java.lang.String groupGuid) throws java.rmi.RemoteException;
    public long insertPeriodMark(java.lang.String groupGuid, long subjectID, java.lang.String studentGuid, ru.mos.dnevnik.EduMarkType markType, ru.mos.dnevnik.EduReportingPeriodType periodType, org.apache.axis.types.UnsignedByte periodNumber, java.math.BigDecimal value, ru.mos.dnevnik.MarkBonusType bonus, java.lang.String description) throws java.rmi.RemoteException;
    public long insertFinalMark(java.lang.String groupGuid, long subjectID, java.lang.String studentGuid, ru.mos.dnevnik.EduMarkType markType, ru.mos.dnevnik.FinalMarkType finalMarkType, java.math.BigDecimal value, ru.mos.dnevnik.MarkBonusType bonus, java.lang.String description) throws java.rmi.RemoteException;
    public long insertSubject(java.lang.String schoolGuid, java.lang.String name, java.lang.String nameFull, ru.mos.dnevnik.KnowledgeArea knowledgeArea) throws java.rmi.RemoteException;
    public boolean updateTimeTable(long ID, java.lang.String name, ru.mos.dnevnik.DayTimeTable[] dayTimeTableCollection) throws java.rmi.RemoteException;
    public boolean updateReportingPeriodGroup(long ID, java.lang.String name) throws java.rmi.RemoteException;
    public boolean updateReportingPeriod(long ID, java.util.Calendar dateStart, java.util.Calendar dateFinish) throws java.rmi.RemoteException;
    public boolean updateGroup(java.lang.String guid, java.lang.String schoolGuid, java.lang.String name, org.apache.axis.types.UnsignedByte parallel, int studyYear, long periodGroupID, java.lang.String description, long tableID) throws java.rmi.RemoteException;
    public boolean updateLesson(long ID, long subjectID, java.lang.String teacherGuid, java.util.Calendar date, org.apache.axis.types.UnsignedByte number) throws java.rmi.RemoteException;
    public boolean updateWork(long ID, ru.mos.dnevnik.EduWorkType workType, ru.mos.dnevnik.EduMarkType markType, org.apache.axis.types.UnsignedByte markCount, java.lang.String title, java.lang.String description) throws java.rmi.RemoteException;
    public boolean updateMarkByID(long ID, java.math.BigDecimal value, ru.mos.dnevnik.EduMarkType markType, ru.mos.dnevnik.MarkBonusType bonus, java.lang.String description) throws java.rmi.RemoteException;
    public boolean updateMark(long workID, java.lang.String personGuid, org.apache.axis.types.UnsignedByte number, java.math.BigDecimal value, ru.mos.dnevnik.EduMarkType markType, ru.mos.dnevnik.MarkBonusType bonus, java.lang.String description) throws java.rmi.RemoteException;
    public boolean updatePeriodMark(java.lang.String groupGuid, long subjectID, java.lang.String studentGuid, ru.mos.dnevnik.EduMarkType markType, ru.mos.dnevnik.EduReportingPeriodType periodType, org.apache.axis.types.UnsignedByte periodNumber, java.math.BigDecimal value, ru.mos.dnevnik.MarkBonusType bonus, java.lang.String description) throws java.rmi.RemoteException;
    public boolean updateFinalMark(java.lang.String groupGuid, long subjectID, java.lang.String studentGuid, ru.mos.dnevnik.EduMarkType markType, ru.mos.dnevnik.FinalMarkType finalMarkType, java.math.BigDecimal value, ru.mos.dnevnik.MarkBonusType bonus, java.lang.String description) throws java.rmi.RemoteException;
    public boolean updateSubject(long ID, java.lang.String name, java.lang.String nameFull, ru.mos.dnevnik.KnowledgeArea knowledgeArea) throws java.rmi.RemoteException;
    public boolean updateLessonLogEntry(long lessonID, java.lang.String personGuid, ru.mos.dnevnik.EduLessonLogEntryStatus status) throws java.rmi.RemoteException;
    public boolean deleteTimeTable(long ID) throws java.rmi.RemoteException;
    public boolean deleteReportingPeriodGroup(long ID) throws java.rmi.RemoteException;
    public boolean deleteGroup(java.lang.String guid) throws java.rmi.RemoteException;
    public boolean deleteLesson(long ID) throws java.rmi.RemoteException;
    public boolean deleteWork(long ID) throws java.rmi.RemoteException;
    public boolean deleteMark(long workID, java.lang.String personGuid, org.apache.axis.types.UnsignedByte number) throws java.rmi.RemoteException;
    public boolean deleteMarkByID(long ID) throws java.rmi.RemoteException;
    public boolean deleteGroupMembership(java.lang.String personGuid, java.lang.String groupGuid) throws java.rmi.RemoteException;
    public boolean deleteSubject(long ID) throws java.rmi.RemoteException;
    public boolean deleteLessonLogEntry(long lessonID, java.lang.String personGuid) throws java.rmi.RemoteException;
    public ru.mos.dnevnik.ReportingPeriodGroup[] getReportingPeriodGroupCollection(java.lang.String schoolGuid, int studyYear) throws java.rmi.RemoteException;
    public ru.mos.dnevnik.Subject[] getSubjectCollection(java.lang.String schoolGuid) throws java.rmi.RemoteException;
}
