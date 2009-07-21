SET NAMES utf8;

create database RujelStatic default character set utf8;

create table RujelStatic.BASE_COURSE select * from BaseJournal.BASE_COURSE;
alter table RujelStatic.BASE_COURSE ADD PRIMARY KEY (`CR_ID`);

create table RujelStatic.PRIMITIVE_EDU_CYCLE select * from BaseJournal.PRIMITIVE_EDU_CYCLE;
alter table RujelStatic.PRIMITIVE_EDU_CYCLE ADD PRIMARY KEY (`C_ID`);

create table RujelStatic.ENT_INDEX select * from BaseJournal.ENT_INDEX;
alter table RujelStatic.ENT_INDEX ADD PRIMARY KEY (`E_ID`);

CREATE TABLE  RujelStatic.SETTINGS_BASE (
  `S_ID` mediumint NOT NULL,
  `KEY` varchar(28) NOT NULL,
  `TEXT_VALUE` varchar(255),
  `NUM_VALUE` int,
  PRIMARY KEY (`S_ID`)
);

CREATE TABLE  RujelStatic.SETTING_BY_COURSE (
  `SC_ID` mediumint NOT NULL,
  `SETTINGS` mediumint NOT NULL,
  `EDU_YEAR` smallint,
  `COURSE` smallint,
  `CYCLE` mediumint,
  `GRADE` smallint,
  `EDU_GROUP` mediumint,
  `TEACHER` mediumint,
  `TEXT_VALUE` varchar(255),
  `NUM_VALUE` int,
  PRIMARY KEY (`SC_ID`)
);

create database RujelYear2007 default character set utf8;
create database RujelYear2008 default character set utf8;

create table RujelYear2007.COURSE_AUDIENCE select * from BaseJournal.COURSE_AUDIENCE
where COURSE in (select CR_ID from BaseJournal.BASE_COURSE where EDU_YEAR = 2007);
alter table RujelYear2007.COURSE_AUDIENCE ADD PRIMARY KEY (`COURSE`,`STUDENT`);

create table RujelYear2008.COURSE_AUDIENCE select * from BaseJournal.COURSE_AUDIENCE
where COURSE in (select CR_ID from BaseJournal.BASE_COURSE where EDU_YEAR = 2008);
alter table RujelYear2008.COURSE_AUDIENCE ADD PRIMARY KEY (`COURSE`,`STUDENT`);

create table RujelYear2007.BASE_TAB select * from BaseJournal.BASE_TAB
where COURSE in (select CR_ID from BaseJournal.BASE_COURSE where EDU_YEAR = 2007);
alter table RujelYear2007.BASE_TAB ADD PRIMARY KEY (`TAB_ID`);

create table RujelYear2008.BASE_TAB select * from BaseJournal.BASE_TAB
where COURSE in (select CR_ID from BaseJournal.BASE_COURSE where EDU_YEAR = 2008);
alter table RujelYear2008.BASE_TAB ADD PRIMARY KEY (`TAB_ID`);

create table RujelYear2007.BASE_LESSON select * from BaseJournal.BASE_LESSON
where COURSE in (select CR_ID from BaseJournal.BASE_COURSE where EDU_YEAR = 2007);
alter table RujelYear2007.BASE_LESSON ADD PRIMARY KEY (`L_ID`);

create table RujelYear2008.BASE_LESSON select * from BaseJournal.BASE_LESSON
where COURSE in (select CR_ID from BaseJournal.BASE_COURSE where EDU_YEAR = 2008);
alter table RujelYear2008.BASE_LESSON ADD PRIMARY KEY (`L_ID`);

create table RujelYear2007.BASE_NOTE select * from BaseJournal.BASE_NOTE
where LESSON in (select L_ID from RujelYear2007.BASE_LESSON);
alter table RujelYear2007.BASE_NOTE ADD PRIMARY KEY (`LESSON`,`STUDENT`);

create table RujelYear2008.BASE_NOTE select * from BaseJournal.BASE_NOTE
where LESSON in (select L_ID from RujelYear2008.BASE_LESSON);
alter table RujelYear2008.BASE_NOTE ADD PRIMARY KEY (`LESSON`,`STUDENT`);

create table RujelYear2007.WORK select * from Criterial.WORK
where COURSE in (select CR_ID from BaseJournal.BASE_COURSE where EDU_YEAR = 2007);
alter table RujelYear2007.WORK ADD PRIMARY KEY (`W_ID`);

create table RujelYear2008.WORK select * from Criterial.WORK
where COURSE in (select CR_ID from BaseJournal.BASE_COURSE where EDU_YEAR = 2008);
alter table RujelYear2008.WORK ADD PRIMARY KEY (`W_ID`);

create table RujelYear2007.TEXT_STORE select T_ID,ENTITY,STORED_TEXT from BaseJournal.TEXT_STORE
  where T_ID in (select TASK FROM RujelYear2007.`WORK` where TASK is not null);
alter table RujelYear2007.TEXT_STORE ADD PRIMARY KEY (`T_ID`);

create table RujelYear2008.TEXT_STORE select T_ID,ENTITY,STORED_TEXT from BaseJournal.TEXT_STORE
  where T_ID in (select TASK FROM RujelYear2008.`WORK` where TASK is not null);
alter table RujelYear2008.TEXT_STORE ADD PRIMARY KEY (`T_ID`);

create table RujelYear2007.CRITER_MASK select * from Criterial.CRITER_MASK
  where WORK in (select W_ID FROM RujelYear2007.`WORK`);
alter table RujelYear2007.CRITER_MASK ADD PRIMARY KEY (`WORK`,`CRITERION`);

create table RujelYear2008.CRITER_MASK select * from Criterial.CRITER_MASK
  where WORK in (select W_ID FROM RujelYear2008.`WORK`);
alter table RujelYear2008.CRITER_MASK ADD PRIMARY KEY (`WORK`,`CRITERION`);

create table RujelYear2007.MARK select * from Criterial.MARK
  where WORK in (select W_ID FROM RujelYear2007.`WORK`);
alter table RujelYear2007.MARK ADD PRIMARY KEY (`WORK`,`CRITER`,`STUDENT`);

create table RujelYear2008.MARK select * from Criterial.MARK
  where WORK in (select W_ID FROM RujelYear2008.`WORK`);
alter table RujelYear2008.MARK ADD PRIMARY KEY (`WORK`,`CRITER`,`STUDENT`);

create table RujelYear2007.WORK_NOTE select * from Criterial.WORK_NOTE
  where WORK in (select W_ID FROM RujelYear2007.`WORK`);
alter table RujelYear2007.WORK_NOTE ADD PRIMARY KEY (`WORK`,`STUDENT`);

create table RujelYear2008.WORK_NOTE select * from Criterial.WORK_NOTE
  where WORK in (select W_ID FROM RujelYear2008.`WORK`);
alter table RujelYear2008.WORK_NOTE ADD PRIMARY KEY (`WORK`,`STUDENT`);

insert into RujelStatic.SETTINGS_BASE (`S_ID`,`KEY`,`NUM_VALUE`) values (1,'CriteriaSet',1);

create table RujelStatic.BORDER select * from Criterial.BORDER;
alter table RujelStatic.BORDER ADD PRIMARY KEY (`B_ID`);

create table RujelStatic.BORDER_SET select * from Criterial.BORDER_SET;
alter table RujelStatic.BORDER_SET ADD PRIMARY KEY (`BS_ID`);

create table RujelStatic.CRIT_SET select * from Criterial.CRIT_SET;
alter table RujelStatic.CRIT_SET ADD PRIMARY KEY (`CS_ID`);

create table RujelStatic.CRITERION select * from Criterial.CRITERION;
alter table RujelStatic.CRITERION ADD PRIMARY KEY (`CR_ID`);

create table RujelStatic.INDEXER select * from Criterial.INDEXER;
alter table RujelStatic.INDEXER
  ADD COLUMN `DEFAULT_VALUE` VARCHAR(255),
  ADD PRIMARY KEY (`IND_ID`);

create table RujelStatic.INDEX_ROW select * from Criterial.INDEX_ROW;
alter table RujelStatic.INDEX_ROW ADD PRIMARY KEY (`IND`,`IDX`);

create table RujelStatic.ITOG_TYPE select ID_TYPE as T_ID, 
  TITLE, NAME, IN_YEAR_COUNT, ARCHIVE_SINCE as SORT from EduResults.PER_TYPE;
alter table RujelStatic.ITOG_TYPE ADD PRIMARY KEY (`T_ID`);
update RujelStatic.ITOG_TYPE set SORT = (10 - IN_YEAR_COUNT);

create table RujelStatic.ITOG select ID_PER as I_ID, PER_TYPE as TYPE, NUM, EDU_YEAR
  from EduResults.EDU_PERIOD where EDU_YEAR in (2007,2008);
alter table RujelStatic.ITOG ADD PRIMARY KEY (`I_ID`);

create table RujelStatic.ITOG_MARK select PERIOD as CONTAINER, EDU_CYCLE as CYCLE, STUDENT,
  MARK, VALUE, FLAGS from EduResults.ITOG_MARK;
alter table RujelStatic.ITOG_MARK ADD PRIMARY KEY (`CONTAINER`,`CYCLE`,`STUDENT`);

create table RujelStatic.ITOG_COMMENT (
  CONTAINER smallint NOT NULL,
  CYCLE mediumint NOT NULL,
  STUDENT int NOT NULL,
  COMMENT text NOT NULL,
  PRIMARY KEY (`CONTAINER`,`CYCLE`,`STUDENT`)
);

insert into RujelStatic.SETTINGS_BASE (`S_ID`,`KEY`,`TEXT_VALUE`) 
	values (2,'ItogType','Базовый');

insert into RujelStatic.SETTING_BY_COURSE (SC_ID, SETTINGS, COURSE, EDU_GROUP, EDU_YEAR, TEXT_VALUE)
select PKEY - 2 , 2, EDU_COURSE, EDU_GROUP, EDU_YEAR, 'Семестры' from EduResults.PERTYPE_USAGE where PERTYPE_ID = 2;

update RujelStatic.SETTING_BY_COURSE S,  RujelStatic.BASE_COURSE C
set S.EDU_YEAR = C.EDU_YEAR where S.COURSE = C.CR_ID;

create table RujelStatic.ITOG_TYPE_LIST (
  TL_ID mediumint NOT NULL,
  LIST_NAME varchar(28) NOT NULL,
  ITOG_TYPE smallint NOT NULL,
  PRIMARY KEY (`TL_ID`)
);

insert into RujelStatic.ITOG_TYPE_LIST values (1,'Базовый',3), (2,'Базовый',1), (3,'Семестры',2);

update MarkArchive.USED_ENTITY set KEY3 = 'containerID' where ENTITY_NAME = 'ItogMark';
update Stats.DESCRIPTION set GROUPING2 = 'ItogContainer' where ENT_NAME = 'ItogMark';

create table RujelStatic.SUBJ_AREA select CG_ID as A_ID, NAME, NUM from EduPlan.SUBJ_AREA;
alter table RujelStatic.SUBJ_AREA ADD PRIMARY KEY (`A_ID`);

create table RujelStatic.SUBJECT select S_ID, AREA_ID as AREA, NUM, SUBJECT, FULL_NAME,
SUBGROUPS, NORMAL_GROUP from EduPlan.SUBJECT;
alter table RujelStatic.SUBJECT ADD PRIMARY KEY (`S_ID`);

create table RujelStatic.PLAN_CYCLE select * from EduPlan.CYCLE;
alter table RujelStatic.PLAN_CYCLE ADD PRIMARY KEY (`C_ID`);
update RujelStatic.PLAN_CYCLE set HOURS = HOURS * 34;

CREATE TABLE  RujelYear2007.EDU_PERIOD (
  P_ID smallint NOT NULL,
  EDU_YEAR smallint NOT NULL,
  BEGIN date NOT NULL,
  END date NOT NULL,
  TITLE varchar(9) NOT NULL,
  NAME varchar(28),
  PRIMARY KEY (`P_ID`)
);

insert into RujelYear2007.EDU_PERIOD
select P.ID_PER, EDU_YEAR, BEGIN, END, CONCAT(ELT(NUM,'I','II','III'),' ',T.TITLE),
  CONCAT(ELT(NUM,'I','II','III'),' ',T.NAME)
  from EduResults.EDU_PERIOD P, EduResults.PER_TYPE T
  where T.ID_TYPE = P.PER_TYPE AND P.EDU_YEAR = 2007 AND PER_TYPE > 1;

CREATE TABLE  RujelYear2008.EDU_PERIOD (
  P_ID smallint NOT NULL,
  EDU_YEAR smallint NOT NULL,
  BEGIN date NOT NULL,
  END date NOT NULL,
  TITLE varchar(9) NOT NULL,
  NAME varchar(28),
  PRIMARY KEY (`P_ID`)
);

insert into RujelYear2008.EDU_PERIOD
select P.ID_PER, EDU_YEAR, BEGIN, END, CONCAT(ELT(NUM,'I','II','III'),' ',T.TITLE),
  CONCAT(ELT(NUM,'I','II','III'),' ',T.NAME)
  from EduResults.EDU_PERIOD P, EduResults.PER_TYPE T
  where T.ID_TYPE = P.PER_TYPE AND P.EDU_YEAR = 2008 AND PER_TYPE > 1;


create table RujelStatic.HOLIDAY_TYPE (
  HT_ID smallint NOT NULL,
  NAME varchar(28) NOT NULL,
  BEGIN_MONTH tinyint NOT NULL,
  BEGIN_DAY tinyint NOT NULL,
  END_MONTH tinyint NOT NULL,
  END_DAY tinyint NOT NULL,
  PRIMARY KEY (`HT_ID`)
);

create table RujelYear2007.PLAN_DETAIL (
  COURSE smallint NOT NULL,
  PERIOD smallint NOT NULL,
  HOURS smallint NOT NULL,
  WEEKLY smallint NOT NULL,
  PRIMARY KEY (`COURSE`,`PERIOD`)
);

create table RujelYear2008.PLAN_DETAIL (
  COURSE smallint NOT NULL,
  PERIOD smallint NOT NULL,
  HOURS smallint NOT NULL,
  WEEKLY smallint NOT NULL,
  PRIMARY KEY (`COURSE`,`PERIOD`)
);

create table RujelYear2007.PERIOD_LIST (
  PL_ID smallint NOT NULL,
  PERIOD smallint NOT NULL,
  LIST_NAME varchar(28) NOT NULL,
  PRIMARY KEY (`L_ID`)
);

create table RujelYear2008.PERIOD_LIST (
  PL_ID smallint NOT NULL,
  PERIOD smallint NOT NULL,
  LIST_NAME varchar(28) NOT NULL,
  PRIMARY KEY (`L_ID`)
);

insert into RujelStatic.SETTINGS_BASE (`S_ID`,`KEY`,`TEXT_VALUE`) 
	values (3,'EduPeriod','Базовый');

insert into RujelYear2007.PERIOD_LIST (PL_ID,PERIOD,LIST_NAME)
select ID_PER, ID_PER,'Базовый' FROM EduResults.EDU_PERIOD where PER_TYPE = 3 AND EDU_YEAR = 2007;

insert into RujelYear2008.PERIOD_LIST (PL_ID,PERIOD,LIST_NAME)
select ID_PER, ID_PER,'Базовый' FROM EduResults.EDU_PERIOD where PER_TYPE = 3 AND EDU_YEAR = 2008;

insert into RujelStatic.SETTING_BY_COURSE (SC_ID, SETTINGS, COURSE, EDU_GROUP, EDU_YEAR, TEXT_VALUE)
select SC_ID + 8 , 3, COURSE, EDU_GROUP, EDU_YEAR, 'Семестры' from RujelStatic.SETTING_BY_COURSE where SETTINGS = 2;

insert into RujelYear2007.PERIOD_LIST (PL_ID,PERIOD,LIST_NAME)
select ID_PER, ID_PER,'Семестры' FROM EduResults.EDU_PERIOD where PER_TYPE = 2 AND EDU_YEAR = 2007;

create table RujelYear2007.HOLIDAY (
  H_ID smallint NOT NULL,
  TYPE smallint NOT NULL,
  BEGIN date NOT NULL,
  END date NOT NULL,
  LIST_NAME varchar(28),
  PRIMARY KEY (`H_ID`)
);

create table RujelYear2008.HOLIDAY (
  H_ID smallint NOT NULL,
  TYPE smallint NOT NULL,
  BEGIN date NOT NULL,
  END date NOT NULL,
  LIST_NAME varchar(28),
  PRIMARY KEY (`H_ID`)
);

CREATE TABLE  `RujelYear2007`.`REASON` (
  `R_ID` mediumint(9) NOT NULL,
  `BEGIN` date NOT NULL DEFAULT '2000-09-01',
  `END` date DEFAULT NULL,
  `REASON` varchar(255) NOT NULL,
  `TEACHER` mediumint(9) DEFAULT NULL,
  `EDU_GROUP` mediumint(9) DEFAULT NULL,
  `VERIFICATION` varchar(255) DEFAULT NULL,
  `SCHOOL` smallint(6) NOT NULL DEFAULT '0',
  `FLAGS` tinyint(4) NOT NULL,
  PRIMARY KEY (`R_ID`)
);

create table RujelYear2008.REASON select * from Curriculum.REASON where FLAGS != 1;
alter table RujelYear2008.REASON ADD PRIMARY KEY (`R_ID`);

CREATE TABLE  `RujelYear2007`.`SUBSTITUTE` (
  `LESSON` int NOT NULL,
  `TEACHER` mediumint NOT NULL,
  `REASON` mediumint NOT NULL,
  `LESSON_DATE` date NOT NULL,
  `FROM_LESSON` int NOT NULL,
  `FACTOR` decimal(4,2) NOT NULL,
  PRIMARY KEY (`LESSON`,`TEACHER`)
);

create table RujelYear2008.SUBSTITUTE select * from Curriculum.SUBSTITUTE
where REASON in (select R_ID from RujelYear2008.REASON);
alter table RujelYear2008.SUBSTITUTE
  ADD COLUMN `FROM_LESSON` int,
  ADD PRIMARY KEY (`LESSON`,`TEACHER`);

CREATE TABLE  RujelYear2007.`VARIATION` (
  `V_ID` int(11) NOT NULL,
  `COURSE` smallint(6) DEFAULT NULL,
  `V_DATE` date NOT NULL,
  `VALUE` tinyint(4) NOT NULL,
  `REASON` mediumint(9) NOT NULL,
  PRIMARY KEY (`V_ID`)
);

create table RujelYear2008.VARIATION select * from Curriculum.VARIATION
where REASON in (select R_ID from RujelYear2008.REASON);
alter table RujelYear2008.VARIATION
  MODIFY COLUMN `COURSE` smallint,
  ADD PRIMARY KEY (`V_ID`);

CREATE TABLE  `RujelYear2007`.`REPRIMAND` (
  `R_ID` mediumint(9) NOT NULL,
  `COURSE` smallint(6) NOT NULL,
  `RAISED` datetime NOT NULL,
  `RELIEF` datetime DEFAULT NULL,
  `CONTENT` varchar(255) NOT NULL,
  `AUTHOR` varchar(255) NOT NULL,
  `STATUS` tinyint(4) NOT NULL,
  PRIMARY KEY (`R_ID`)
);

create table RujelYear2008.REPRIMAND select * from Curriculum.REPRIMAND;
alter table RujelYear2008.REPRIMAND ADD PRIMARY KEY (`R_ID`);

insert into RujelStatic.HOLIDAY_TYPE (HT_ID, NAME, BEGIN_MONTH, BEGIN_DAY, END_MONTH, END_DAY)
  select R_ID, REASON, MONTH(BEGIN), DAYOFMONTH(BEGIN), MONTH(END), DAYOFMONTH(END)
from Curriculum.reason where FLAGS = 1 and BEGIN != '2008-09-01';
update RujelStatic.HOLIDAY_TYPE set BEGIN_MONTH = BEGIN_MONTH + if(BEGIN_MONTH < 7, 12, 0),
   END_MONTH = END_MONTH + if(END_MONTH < 7, 12, 0);

insert into RujelYear2008.HOLIDAY (H_ID, TYPE, BEGIN, END)
  select R_ID, R_ID, BEGIN, END from Curriculum.reason where FLAGS = 1 and BEGIN != '2008-09-01';
