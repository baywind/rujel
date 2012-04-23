-- Schema updates for Curriculum model --

-- v1 (0.9) --

ALTER TABLE CU_VARIATION
  ADD COLUMN LESSON_ID int;

DELETE from EO_PK_TABLE
  where `NAME` = 'CU_VARIATION';

INSERT into EO_PK_TABLE (`NAME`,`PK`)
  SELECT 'CU_VARIATION', MAX(V_ID)
  from CU_VARIATION;

INSERT into CU_VARIATION (V_ID, EDU_COURSE, VAR_DATE, VAR_VALUE, REASON_ID)
SELECT count(v2.V_ID) + pk.PK as num, v1.EDU_COURSE, v1.VAR_DATE, 1, v1.REASON_ID
FROM EO_PK_TABLE pk, CU_VARIATION v1
inner join CU_VARIATION v2 on v1.V_ID >= v2.V_ID and v2.VAR_VALUE > 1
where v1.VAR_VALUE > 1 AND pk.NAME = 'CU_VARIATION'
group by v1.V_ID;

update CU_VARIATION
set VAR_VALUE = VAR_VALUE -1
where VAR_VALUE > 1;
## 4 -> 3

UPDATE EO_PK_TABLE
  set `PK` = (SELECT MAX(V_ID) from CU_VARIATION)
  where `NAME` = 'CU_VARIATION';

INSERT into CU_VARIATION (V_ID, EDU_COURSE, VAR_DATE, VAR_VALUE, REASON_ID)
SELECT count(v2.V_ID) + pk.PK as num, v1.EDU_COURSE, v1.VAR_DATE, 1, v1.REASON_ID
FROM EO_PK_TABLE pk, CU_VARIATION v1
inner join CU_VARIATION v2 on v1.V_ID >= v2.V_ID and v2.VAR_VALUE > 1
where v1.VAR_VALUE > 1 AND pk.NAME = 'CU_VARIATION'
group by v1.V_ID;

update CU_VARIATION
set VAR_VALUE = VAR_VALUE -1
where VAR_VALUE > 1;
## 3 -> 2

UPDATE EO_PK_TABLE
  set `PK` = (SELECT MAX(V_ID) from CU_VARIATION)
  where `NAME` = 'CU_VARIATION';

INSERT into CU_VARIATION (V_ID, EDU_COURSE, VAR_DATE, VAR_VALUE, REASON_ID)
SELECT count(v2.V_ID) + pk.PK as num, v1.EDU_COURSE, v1.VAR_DATE, 1, v1.REASON_ID
FROM EO_PK_TABLE pk, CU_VARIATION v1
inner join CU_VARIATION v2 on v1.V_ID >= v2.V_ID and v2.VAR_VALUE > 1
where v1.VAR_VALUE > 1 AND pk.NAME = 'CU_VARIATION'
group by v1.V_ID;

update CU_VARIATION
set VAR_VALUE = VAR_VALUE -1
where VAR_VALUE > 1;
## 2 -> 1

UPDATE EO_PK_TABLE
  set `PK` = (SELECT MAX(V_ID) from CU_VARIATION)
  where `NAME` = 'CU_VARIATION';

CREATE TABLE CU_TMP
SELECT v.V_ID, l.L_ID, count(l2.L_ID) as L_NUM
FROM CU_VARIATION v
join BASE_LESSON l on l.EDU_COURSE = v.EDU_COURSE AND l.DATE_PERFORMED = v.VAR_DATE 
inner join BASE_LESSON l2 on l2.EDU_COURSE = v.EDU_COURSE AND l2.DATE_PERFORMED = v.VAR_DATE AND l2.L_ID <= l.L_ID
where v.VAR_VALUE = 1
group by v.V_ID, l.L_ID;

CREATE TABLE CU_TMP2
select t1.*, count(t2.L_ID) as V_NUM
from CU_TMP t1
inner join CU_TMP t2 ON t1.L_ID = t2.L_ID AND t2.V_ID <= t1.V_ID
group by t1.L_ID, t1.V_ID;

update CU_VARIATION v
LEFT OUTER JOIN CU_TMP2 t on v.V_ID = t.V_ID AND t.V_NUM = t.L_NUM
set v.LESSON_ID = t.L_ID
where v.VAR_VALUE = 1;

drop table CU_TMP2;
drop table CU_TMP;


CREATE TABLE CU_TMP
SELECT v2.VAR_DATE, r.R_ID, v2.V_ID plus,v1.V_ID as minus,v1.VAR_VALUE, c1.EDU_GROUP, v2.LESSON_ID
FROM CU_VARIATION v1, CU_VARIATION v2, CU_REASON r, BASE_COURSE c1, BASE_COURSE c2
where v1.VAR_VALUE < 0 and v2.VAR_VALUE = 1 and v1.REASON_ID = v2.REASON_ID and v1.VAR_DATE = v2.VAR_DATE  
AND r.R_ID = v2.REASON_ID and r.TEACHER_ID is not null 
AND c1.CR_ID = v1.EDU_COURSE and c2.CR_ID = v2.EDU_COURSE AND c1.EDU_GROUP = c2.EDU_GROUP;

CREATE TABLE CU_TMP1
select t1.plus,t1.minus,t1.LESSON_ID,t1.VAR_VALUE, count(t2.minus) as P_NUM
from CU_TMP t1
inner join CU_TMP t2 ON t1.minus = t2.minus AND t2.plus <= t1.plus
group by t1.minus, t1.plus;

CREATE TABLE CU_TMP2
select t1.plus,t1.minus,t1.LESSON_ID, t1.VAR_VALUE, t1.P_NUM,count(t2.plus) as M_NUM
from CU_TMP1 t1
inner join CU_TMP1 t2 ON t1.minus >= t2.minus AND t2.plus = t1.plus
group by t1.minus, t1.plus;

update CU_VARIATION v, CU_TMP2 t
set v.LESSON_ID = t.LESSON_ID
where v.V_ID = t.minus and t.VAR_VALUE = -1 and t.P_NUM = t.M_NUM;

delete from CU_TMP2
where VAR_VALUE = -1;

drop table CU_TMP2;
drop table CU_TMP1;
drop table CU_TMP;

CREATE TABLE IF NOT EXISTS SCHEMA_VERSION (
  MODEL_NAME varchar(255),
  VERSION_NUMBER smallint unsigned NOT NULL,
  VERSION_TITLE varchar(255),
  INSTALL_DATE timestamp
);

INSERT INTO SCHEMA_VERSION (MODEL_NAME,VERSION_NUMBER,VERSION_TITLE)
  VALUES ('Curriculum',1,'0.9');