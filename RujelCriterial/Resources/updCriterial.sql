-- Schema updates for Criterial model --

-- v1 (0.9.4) --

ALTER TABLE CR_WORK ADD INDEX (EDU_COURSE);
ALTER TABLE CR_MARK ADD INDEX (STUDENT_ID);

CREATE TABLE IF NOT EXISTS SCHEMA_VERSION (
  MODEL_NAME varchar(255),
  VERSION_NUMBER smallint unsigned NOT NULL,
  VERSION_TITLE varchar(255),
  INSTALL_DATE timestamp
);

INSERT INTO SCHEMA_VERSION (MODEL_NAME,VERSION_NUMBER,VERSION_TITLE)
  VALUES ('Criterial',1,'0.9.4');

-- v2 (0.9.5) --

INSERT INTO CR_CRITERION (CRIT_SET,CRITER_NUM,CRITER_FLAGS)
  SELECT CS_ID,0,0 FROM CR_CRIT_SET
    WHERE CRITER_FLAGS < 8
  ON DUPLICATE KEY UPDATE DFLT_MAX = DFLT_MAX;

UPDATE CR_CRIT_SET
  SET CRITER_FLAGS = (CRITER_FLAGS -8)
  WHERE CRITER_FLAGS >= 8;

INSERT INTO SCHEMA_VERSION (MODEL_NAME,VERSION_NUMBER,VERSION_TITLE)
  VALUES ('Criterial',2,'0.9.5');
