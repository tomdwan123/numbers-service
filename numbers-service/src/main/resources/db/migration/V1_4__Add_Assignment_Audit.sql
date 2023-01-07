CREATE TABLE ASSIGNMENT_HISTORY (
  id               UUID                                    NOT NULL,
  numberId         UUID                                            ,
  vendorId         TEXT                                            ,
  accountId        TEXT                                            ,
  created          TIMESTAMP WITH TIME ZONE                        ,
  callbackUrl      TEXT                                            ,
  externalMetadata HSTORE                                          ,
  rev              INT4 NOT NULL REFERENCES REVINFO (rev)          ,
  revtype          SMALLINT                                        ,
  PRIMARY KEY (id, rev)
);
