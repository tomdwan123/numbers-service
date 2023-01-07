CREATE TABLE BILLING_RATE_PLAN (
  id             UUID PRIMARY KEY                                    NOT NULL,
  rate_plan_id   VARCHAR(40)                                         NOT NULL,
  country        VARCHAR(2)                                          NOT NULL,
  type           NUMBER_TYPE                                         NOT NULL,
  classification CLASSIFICATION                                      NOT NULL,
  capabilities   SERVICE_TYPE []                                     NOT NULL
);

INSERT INTO BILLING_RATE_PLAN(id, rate_plan_id, country, type, classification, capabilities) VALUES
    ('22460a31-9b07-42f4-834e-403c9216849e', '2c92a00e6d151bc4016d19d7a1865808', 'AU', 'MOBILE', 'BRONZE', '{"SMS"}'),
    ('5d868c4e-a617-4fe5-beb6-b4ebb2a1e59b', '2c92a00e6d151bc4016d19d7a50f5869', 'AU', 'MOBILE', 'GOLD', '{"SMS"}'),
    ('dbb51f18-941e-4c38-ad92-cb17b8dc5484', '2c92a00e6d151bc4016d19d7ab0858fe', 'AU', 'MOBILE', 'SILVER', '{"SMS"}'),
    ('dae93488-3f48-4dc7-b1c7-8339eb1f629b', '2c92a00e6d151bc4016d19d7b4e65987', 'GB', 'MOBILE', 'BRONZE', '{"SMS"}'),
    ('3da0ad9f-c3db-45a4-96b4-9ce78fc4d7be', '2c92a00e6d151bc4016d19d7a254581c', 'GB', 'MOBILE', 'GOLD', '{"SMS"}'),
    ('7edcc7e6-89ff-497f-9c34-49066f42af99', '2c92a00e6d151bc4016d19d7a2ef5835', 'GB', 'MOBILE', 'SILVER', '{"SMS"}'),
    ('4df4df42-8359-4c09-bf85-ee03a624c1b6', '2c92a0fc6d151c44016d19e224077b02', 'US', 'LANDLINE', 'BRONZE', '{"MMS","SMS"}'),
    ('f99ae7e4-0b9e-4be6-8cb8-8d1cf5b833ac', '2c92a0fc6d151c44016d19e224077b02', 'US', 'LANDLINE', 'GOLD', '{"MMS","SMS"}'),
    ('ff26eb77-39fa-4d2e-89fc-deeb6870b97a', '2c92a0fc6d151c44016d19e224077b02', 'US', 'LANDLINE', 'SILVER', '{"MMS","SMS"}'),
    ('9ae616ff-3a4c-4dc9-ad54-88ca7c4d66cc', '2c92a00d6d152eef016d19df035e23b3', 'US', 'LANDLINE', 'BRONZE', '{"SMS"}'),
    ('6696e5f5-1963-4391-8bfc-0423992991ea', '2c92a0076d151c15016d19e471ef13e2', 'US', 'LANDLINE', 'GOLD', '{"SMS"}'),
    ('07f53f62-b004-4790-8090-ef54230af9c7', '2c92a0086d152f08016d19e37ade0e09', 'US', 'LANDLINE', 'SILVER', '{"SMS"}'),
    ('e93742d2-7d1f-43e3-8ee1-258ff36956aa', '2c92a0fc6d151c44016d19e224077b02', 'US', 'MOBILE', 'BRONZE', '{"MMS","SMS"}'),
    ('1cb08ee7-43f3-449f-a1c5-175fc8d2f0c0', '2c92a0fc6d151c44016d19e224077b02', 'US', 'MOBILE', 'GOLD', '{"MMS","SMS"}'),
    ('5fad9e81-c032-47c4-9252-0bf8a5efd74b', '2c92a0fc6d151c44016d19e224077b02', 'US', 'MOBILE', 'SILVER', '{"MMS","SMS"}'),
    ('76812ecd-39a8-4e94-8d49-1b3adaef3c4b', '2c92a00d6d152eef016d19df035e23b3', 'US', 'MOBILE', 'BRONZE', '{"SMS"}'),
    ('34e4657c-3dbf-490f-866d-9516a925194a', '2c92a0076d151c15016d19e471ef13e2', 'US', 'MOBILE', 'GOLD', '{"SMS"}'),
    ('a0fd4afb-70cd-4cff-8613-63ab39b42a07', '2c92a0086d152f08016d19e37ade0e09', 'US', 'MOBILE', 'SILVER', '{"SMS"}'),
    ('03f5df0a-c225-41ec-a6d4-cdff8ba46c3c', '2c92a00d6d152eef016d19df035e23b3', 'US', 'MOBILE', 'BRONZE', '{"TTS"}'),
    ('2aff0153-b4b7-44b8-ae9b-3cd788c06584', '2c92a0076d151c15016d19e471ef13e2', 'US', 'MOBILE', 'GOLD', '{"TTS"}'),
    ('3d3dcacc-4f71-4814-ae35-e00a41ae0c75', '2c92a0086d152f08016d19e37ade0e09', 'US', 'MOBILE', 'SILVER', '{"TTS"}'),
    ('f5c06356-db38-4c4a-ba11-b46a99a9a6b4', '2c92a0fe6d151c52016d19e0b3be0051', 'US', 'TOLL_FREE', 'BRONZE', '{"MMS","SMS"}'),
    ('c94cf897-bc7c-4e8a-95a3-6393a9900dce', '2c92a0fe6d151c52016d19e0b3be0051', 'US', 'TOLL_FREE', 'GOLD', '{"MMS","SMS"}'),
    ('af17a9ed-736a-4211-902d-dae75a74053a', '2c92a0fe6d151c52016d19e0b3be0051', 'US', 'TOLL_FREE', 'SILVER', '{"MMS","SMS"}'),
    ('8b68ec8f-5724-4889-8871-027ac0e42605', '2c92a0fe6d151c52016d19e0b3be0051', 'US', 'TOLL_FREE', 'BRONZE', '{"SMS"}'),
    ('9e6dc11a-51c7-4fbf-8e3b-8aebe1919b62', '2c92a0fe6d151c52016d19e0b3be0051', 'US', 'TOLL_FREE', 'GOLD', '{"SMS"}'),
    ('445d3607-dd77-4287-9cd3-531571637454', '2c92a0fe6d151c52016d19e0b3be0051', 'US', 'TOLL_FREE', 'SILVER', '{"SMS"}'),
    ('929d19cd-a5b1-4c77-b037-684c28f90335', '2c92a0fe6d151c52016d19e0b3be0051', 'US', 'TOLL_FREE', 'BRONZE', '{"TTS"}'),
    ('4c97891c-d593-4df4-a345-cf6cc0fd60f5', '2c92a0fe6d151c52016d19e0b3be0051', 'US', 'TOLL_FREE', 'GOLD', '{"TTS"}'),
    ('4a8af746-6c8f-4260-9667-f0d8554453e2', '2c92a0fe6d151c52016d19e0b3be0051', 'US', 'TOLL_FREE', 'SILVER', '{"TTS"}'),
    ('ef9b29a4-6b63-43f9-8188-25d965291757', '2c92a0fe6d151c52016d19e0b3be0051', 'CA', 'TOLL_FREE', 'BRONZE', '{"MMS","SMS"}'),
    ('7f7d9b7b-1149-4e7b-907f-ea715db14097', '2c92a0fe6d151c52016d19e0b3be0051', 'CA', 'TOLL_FREE', 'SILVER', '{"MMS","SMS"}'),
    ('780422c0-13c0-4712-81d6-595c163c90aa', '2c92a0fe6d151c52016d19e0b3be0051', 'CA', 'TOLL_FREE', 'GOLD', '{"MMS","SMS"}'),
    ('2ba1f463-8192-442f-9b47-109f5f19e22e', '2c92a0fe6d151c52016d19e0b3be0051', 'CA', 'TOLL_FREE', 'BRONZE', '{"SMS"}'),
    ('806e2a0d-195e-4b66-9052-f7f9828f9a00', '2c92a0fe6d151c52016d19e0b3be0051', 'CA', 'TOLL_FREE', 'SILVER', '{"SMS"}'),
    ('acafdbe8-29db-4a5b-b5c1-8ed5bb1f91ab', '2c92a0fe6d151c52016d19e0b3be0051', 'CA', 'TOLL_FREE', 'GOLD', '{"SMS"}')
;