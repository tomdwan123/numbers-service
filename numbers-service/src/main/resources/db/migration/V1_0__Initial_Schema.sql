CREATE TYPE SERVICE_TYPE AS ENUM (
  'SMS',
  'MMS',
  'TTS'
);

CREATE TYPE NUMBER_TYPE AS ENUM (
  'MOBILE',
  'LANDLINE',
  'TOLL_FREE',
  'SHORT_CODE'
);

CREATE TYPE CLASSIFICATION AS ENUM (
  'BRONZE',
  'SILVER',
  'GOLD'
);

CREATE TABLE NUMBER (
  id             UUID PRIMARY KEY                                    NOT NULL,
  phoneNumber    TEXT UNIQUE                                         NOT NULL,
  providerId     UUID                                                NOT NULL,
  country        VARCHAR(2)                                          NOT NULL,
  type           NUMBER_TYPE                                         NOT NULL,
  classification CLASSIFICATION                                      NOT NULL,
  capabilities   SERVICE_TYPE []                                     NOT NULL,
  created        TIMESTAMP WITH TIME ZONE                            NOT NULL,
  updated        TIMESTAMP WITH TIME ZONE                            NOT NULL,
  availableAfter TIMESTAMP WITH TIME ZONE                            NULL
);

CREATE EXTENSION HSTORE;

CREATE TABLE ASSIGNMENT (
  id               UUID PRIMARY KEY                NOT NULL,
  numberId         UUID REFERENCES NUMBER (id)     NOT NULL,
  vendorId         TEXT                            NOT NULL,
  accountId        TEXT                            NOT NULL,
  created          TIMESTAMP WITH TIME ZONE        NOT NULL,
  deleted          TIMESTAMP WITH TIME ZONE        NULL,
  callbackUrl      TEXT                            NULL,
  externalMetadata HSTORE                          NULL
);
