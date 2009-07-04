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
  `COURSE` mediumint,
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


