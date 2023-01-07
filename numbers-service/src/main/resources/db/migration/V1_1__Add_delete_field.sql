ALTER TABLE NUMBER ADD deleted TIMESTAMP WITH TIME ZONE NULL;
ALTER TABLE NUMBER DROP CONSTRAINT number_phonenumber_key;
CREATE UNIQUE INDEX uidx_phonenumber_deleted_partial ON NUMBER (phonenumber) WHERE deleted IS NULL;