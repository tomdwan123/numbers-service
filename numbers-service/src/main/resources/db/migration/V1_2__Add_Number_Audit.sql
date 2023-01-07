CREATE TABLE REVINFO (
    rev           SERIAL    PRIMARY KEY      NOT NULL,
    revtstmp      INT8
);

CREATE TABLE NUMBER_HISTORY (
  id             UUID                                        NOT NULL,
  phoneNumber    TEXT                                                ,
  providerId     UUID                                                ,
  country        VARCHAR(2)                                          ,
  type           NUMBER_TYPE                                         ,
  classification CLASSIFICATION                                      ,
  capabilities   SERVICE_TYPE []                                     ,
  created        TIMESTAMP WITH TIME ZONE                            ,
  updated        TIMESTAMP WITH TIME ZONE                            ,
  availableAfter TIMESTAMP WITH TIME ZONE                            ,
  rev            INT4 NOT NULL REFERENCES REVINFO (rev)              ,
  revtype        SMALLINT                                            ,
  PRIMARY KEY (id, rev)
);

CREATE SEQUENCE hibernate_sequence INCREMENT 1 MINVALUE 1
  START 1
  CACHE 1;
